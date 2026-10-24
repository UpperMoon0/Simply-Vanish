const mc=require('minecraft-protocol');const assert=require('node:assert/strict');
const expected=Number(process.env.EXPECT_MODE);
const client=mc.createClient({host:'127.0.0.1',port:25575,username:'VanishFixture',version:'1.20.1',auth:'offline'});
client.on('error',e=>{console.error(e);process.exit(1);});
client.once('login',data=>{assert.equal(data.gameMode,expected);console.log(JSON.stringify({mode:data.gameMode,expected,passed:true}));setTimeout(()=>{client.end();setTimeout(()=>process.exit(0),250);},300);});
setTimeout(()=>{throw new Error('mode login timeout');},30000).unref();
