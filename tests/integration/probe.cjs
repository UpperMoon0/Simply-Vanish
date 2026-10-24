// Requires minecraft-protocol in NODE_PATH. Run ONLY against a dedicated, approved
// loopback fixture. No production host/authentication configuration is accepted.
const mc = require('minecraft-protocol');
const crypto = require('node:crypto');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const delay = ms => new Promise(r => setTimeout(r, ms));
const port = Number(process.env.VANISH_TEST_PORT || 25575);
const clients = [];
function uuid(name) {
  const hash = crypto.createHash('md5').update('OfflinePlayer:' + name).digest();
  hash[6] = (hash[6] & 15) | 48; hash[8] = (hash[8] & 63) | 128;
  return hash.toString('hex').replace(/^(.{8})(.{4})(.{4})(.{4})(.{12})$/, '$1-$2-$3-$4-$5');
}
const hiddenId = uuid('VanishFixture');
function connect(name) {
  return new Promise((resolve, reject) => {
    const client = mc.createClient({host:'127.0.0.1', port, username:name, version:'1.20.1', auth:'offline'});
    client.observed = []; clients.push(client);
    client.on('packet', (data, meta) => {
      if (['player_info','player_remove','named_entity_spawn','system_chat','login','tab_complete','set_passengers'].includes(meta.name))
        client.observed.push({name:meta.name, data});
      if (meta.name === 'position') client.write('teleport_confirm', {teleportId:data.teleportId});
    });
    client.once('login', () => resolve(client));
    client.once('error', reject);
    client.once('kick_disconnect', data => reject(new Error(JSON.stringify(data))));
    setTimeout(() => reject(new Error('Login timeout: '+name)), 30000).unref();
  });
}
function noHidden(client) {
  for (const event of client.observed) {
    if (event.name === 'player_info') assert.ok(event.data.data.every(e=>e.uuid!==hiddenId), 'Hidden player-info leaked');
    if (event.name === 'player_remove') assert.ok(!event.data.players.includes(hiddenId), 'Hidden removal leaked');
    if (event.name === 'named_entity_spawn') assert.notEqual(event.data.playerUUID, hiddenId, 'Hidden spawn leaked');
    if (event.name === 'system_chat') assert.ok(!JSON.stringify(event.data).includes('VanishFixture'), 'Hidden public message leaked');
  }
}
function ping() { return new Promise((resolve,reject)=>mc.ping({host:'127.0.0.1',port,version:'1.20.1'},(e,r)=>e?reject(e):resolve(r))); }
(async()=>{
  const first=await connect('VisibleFixtureA'); await delay(1200);
  const hidden=await connect('VanishFixture'); await delay(1500);
  const second=await connect('VisibleFixtureB'); await delay(1500);
  noHidden(first);noHidden(second);
  const login=hidden.observed.find(e=>e.name==='login'); assert.equal(login.data.gameMode,3,'Hidden not spectator at initial login');
  // A visible join must remain public: proves packet filtering is not blanket suppression.
  assert.ok(first.observed.some(e=>e.name==='player_info'&&e.data.data.some(p=>p.uuid===uuid('VisibleFixtureB'))));
  await delay(5500); const status=await ping();
  assert.equal(status.players.online,2); assert.ok((status.players.sample||[]).every(p=>p.id!==hiddenId));
  first.write('chat_command',{command:'list',timestamp:BigInt(Date.now()),salt:0n,argumentSignatures:[],messageCount:0,acknowledged:Buffer.alloc(3)});
  first.write('tab_complete',{transactionId:12,text:'/msg Van'});
  await delay(1000); noHidden(first);
  const completion=first.observed.find(e=>e.name==='tab_complete'&&e.data.transactionId===12);
  assert.ok(completion,'Missing tab completion response');assert.ok(!JSON.stringify(completion).includes('VanishFixture'));
  hidden.end(); await delay(1500); noHidden(first); noHidden(second);
  const report={passed:true,hiddenId,modernStatus:status.players,visiblePacketCounts:clients.slice(0,3).map(c=>c.observed.length),checks:['plain vanilla protocol clients accepted without mod','hidden spectator on initial login','first and later visible player info/entity/join filtered','visible join preserved','public modern count/sample','player list and suggestions','hidden disconnect']};
  fs.writeFileSync(process.env.VANISH_TEST_REPORT||'/tmp/vanish-probe-report.json',JSON.stringify(report,null,2));
  console.log(JSON.stringify(report)); clients.forEach(c=>c.end());
})().catch(e=>{console.error(e);clients.forEach(c=>c.end());process.exitCode=1;});
