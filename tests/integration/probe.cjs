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
      if (['player_info','player_remove','named_entity_spawn','system_chat','login','tab_complete','set_passengers','respawn','death_combat_event'].includes(meta.name))
        client.observed.push({name:meta.name, data});
      if (meta.name === 'death_combat_event') client.write('client_command',{actionId:'perform_respawn'});
      if (meta.name === 'position') client.write('teleport_confirm', {teleportId:data.teleportId});
    });
    client.once('login', () => {
      client.write('settings',{locale:'en_US',viewDistance:3,chatFlags:0,chatColors:true,skinParts:127,mainHand:1,enableTextFiltering:false,enableServerListing:true});
      resolve(client);
    });
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
async function legacyPing(payload) {
  const net = require('node:net');
  return new Promise((resolve,reject)=>{
    const socket = net.connect(port,'127.0.0.1',()=>socket.write(payload));
    const chunks=[];socket.on('data',b=>chunks.push(b));socket.on('error',reject);
    socket.on('end',()=>{const b=Buffer.concat(chunks);assert.equal(b[0],255);const content=b.subarray(3);content.swap16();resolve(content.toString('utf16le'));});
    socket.setTimeout(5000,()=>{socket.destroy();reject(new Error('Legacy ping timeout'));});
  });
}
async function fullQuery() {
  const dgram=require('node:dgram');const socket=dgram.createSocket('udp4');
  const session=Buffer.from([1,2,3,4]);
  const request=(type,payload=Buffer.alloc(0))=>new Promise((resolve,reject)=>{
    const timeout=setTimeout(()=>reject(new Error('Query timeout')),5000);
    socket.once('message',b=>{clearTimeout(timeout);resolve(b);});
    socket.send(Buffer.concat([Buffer.from([254,253,type]),session,payload]),25576,'127.0.0.1');
  });
  try {
    const challenge=await request(9);const token=Buffer.alloc(8);token.writeInt32BE(Number(challenge.subarray(5,-1).toString()),0);
    return await request(0,token);
  } finally {socket.close();}
}
(async()=>{
  const first=await connect('VisibleFixtureA'); await delay(1200);
  const hidden=await connect('VanishFixture'); await delay(1500);
  const second=await connect('VisibleFixtureB'); await delay(1500);
  noHidden(first);noHidden(second);
  const login=hidden.observed.find(e=>e.name==='login'); assert.equal(login.data.gameMode,3,'Hidden not spectator at initial login');
  // A visible join must remain public: proves packet filtering is not blanket suppression.
  assert.ok(first.observed.some(e=>e.name==='player_info'&&e.data.data.some(p=>p.uuid===uuid('VisibleFixtureB'))));
  assert.ok(first.observed.some(e=>e.name==='named_entity_spawn'&&e.data.playerUUID===uuid('VisibleFixtureB')),'Visible tracking regression or vacuous tracking assertion');
  await delay(32000); const status=await ping();
  assert.equal(status.players.online,2); assert.ok((status.players.sample||[]).every(p=>p.id!==hiddenId));
  if (process.env.VANISH_TEST_COMMAND_SCENARIOS === '1') {
    assert.ok(hidden.observed.some(e=>e.name==='death_combat_event'),'Hidden death scenario missing');
    assert.ok(hidden.observed.filter(e=>e.name==='respawn').length>=2,'Dimension and death respawn scenario missing');
    assert.ok(hidden.observed.filter(e=>e.name==='respawn').every(e=>e.data.gamemode===3),'Observer mode lost on dimension/respawn');
  }
  if (process.env.VANISH_TEST_REFRESH_FIXTURE === '1') {
    assert.ok(first.observed.filter(e=>e.name==='player_info'&&e.data.data.some(p=>p.uuid===uuid('VisibleFixtureB'))).length>=2,'Mixed refresh bundle did not preserve visible entries');
  }
  const legacyOld=await legacyPing(Buffer.from([254]));
  assert.ok(legacyOld.endsWith('§2§10'),legacyOld);
  const legacyNew=await legacyPing(Buffer.from([254,1]));
  assert.ok(legacyNew.endsWith('\0'+'2'+'\0'+'10'),JSON.stringify(legacyNew));
  const query=await fullQuery();
  assert.ok(!query.includes(Buffer.from('VanishFixture')));
  assert.ok(query.includes(Buffer.from('numplayers\0'+'2'+'\0')));
  assert.ok(query.includes(Buffer.from('VisibleFixtureA')) && query.includes(Buffer.from('VisibleFixtureB')));
  first.write('chat_command' ,{command:'list',timestamp:BigInt(Date.now()),salt:0n,argumentSignatures:[],messageCount:0,acknowledged:Buffer.alloc(3)});
  first.write('tab_complete',{transactionId:12,text:'/msg Van'});
  await delay(1000); noHidden(first);
  const completion=first.observed.find(e=>e.name==='tab_complete'&&e.data.transactionId===12);
  assert.ok(completion,'Missing tab completion response');assert.ok(!JSON.stringify(completion).includes('VanishFixture'));
  hidden.end(); await delay(1500); noHidden(first); noHidden(second);
  const report={passed:true,hiddenId,modernStatus:status.players,visiblePacketCounts:clients.slice(0,3).map(c=>c.observed.length),checks:['plain vanilla protocol clients accepted without mod','hidden spectator on initial login','first and later visible player info/entity/join filtered','visible join preserved','public modern count/sample','legacy ping 1.3 and 1.4+ counts','GS4 full query count/names','player list and suggestions','hidden disconnect',...(process.env.VANISH_TEST_REFRESH_FIXTURE==='1'?['delayed remove/init refresh and mixed bundle filtering']:[]),...(process.env.VANISH_TEST_COMMAND_SCENARIOS==='1'?['hidden advancement','dimension transition','death and respawn spectator preservation']:[])]};
  fs.writeFileSync(process.env.VANISH_TEST_REPORT||'/tmp/vanish-probe-report.json',JSON.stringify(report,null,2));
  console.log(JSON.stringify(report)); clients.forEach(c=>c.end()); await delay(250); process.exit(0);
})().catch(e=>{console.error(e);clients.forEach(c=>c.end());process.exitCode=1;setTimeout(()=>process.exit(1),250);});
