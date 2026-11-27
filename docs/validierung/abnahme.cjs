// Vom Projektstamm mit gestarteter Prod-Compose-Umgebung ausführen.
const WebSocket = require('../../webshop-ui/node_modules/ws');
const assert = require('node:assert/strict');
const root = 'http://localhost:8080';
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function api(path, method, body, token, expected) {
  const res = await fetch(root + '/api' + path, {method, headers: {'Content-Type':'application/json', ...(token && {Authorization:'Bearer '+token})}, body: JSON.stringify(body)});
  assert.ok([expected].flat().includes(res.status), `${method} ${path}: HTTP ${res.status}, erwartet ${expected}`);
  return res.status === 204 ? null : res.json();
}
async function account(label) {
  const username = `ac-${Date.now()}-${label}@example.com`;
  const password = 'Demo-Validation123';
  const user = await api('/users/auth/register','POST',{username,email:username,password,firstName:'Abnahme',lastName:label},null,201);
  assert.equal(user.role,'CUSTOMER');
  await api('/users/auth/login','POST',{username,password:'wrong'},null,401);
  const login = await api('/users/auth/login','POST',{username,password},null,200);
  assert.ok(login.accessToken && login.expiresIn > 0);
  return login.accessToken;
}
function subscribe(token) {
  return new Promise((resolve,reject) => {
    const ws = new WebSocket('ws://localhost:8080/ws/orders');
    const events = [];
    const timer = setTimeout(()=>reject(Error('WebSocket-Anmeldung abgelaufen')),10000);
    ws.on('error',reject);
    ws.on('open',()=>ws.send(JSON.stringify({token})));
    ws.on('message',raw=>{ const event=JSON.parse(raw); if(event.type==='AUTHENTICATED'){ clearTimeout(timer); resolve({ws,events}); } else events.push(event); });
  });
}
async function run() {
  const a=await account('A'), b=await account('B');
  console.log('Registrierung und Login beider Konten erfolgreich');
  const owner=await subscribe(a), other=await subscribe(b);
  const products=await api('/products','GET',undefined,null,200);
  const product=products.find(p=>p.active && p.stockQuantity>0);
  assert.ok(product);
  const start=Date.now();
  const order=await api('/orders','POST',{items:[{productId:product.id,quantity:1}]},a,201);
  assert.equal(order.status,'CREATED'); assert.equal(order.totalPrice,product.price);
  const current=await api('/products/'+product.id,'GET',undefined,null,200);
  assert.equal(current.stockQuantity,product.stockQuantity-1);
  for(const method of ['GET','DELETE']) await api('/orders/'+order.id,method,undefined,b,[403,404]);
  while(!owner.events.some(e=>e.orderId===order.id && e.status==='DELIVERED')) { assert.ok(Date.now()-start<20000,'Lieferupdate fehlt'); await pause(100); }
  console.log('Bestellung',order.id,'bis DELIVERED inkl. HTTP/JMS/WS',Date.now()-start,'ms');
  await pause(500);
  assert.ok(!other.events.some(e=>e.orderId===order.id));
  assert.ok((await api('/orders','GET',undefined,a,200)).some(o=>o.id===order.id && o.status==='DELIVERED'));
  owner.ws.close(); other.ws.close();
  for(const token of [a,b]) { await api('/users/me','DELETE',undefined,token,204); await api('/users/me','GET',undefined,token,401); }
  assert.equal((await fetch(root)).status,200); console.log('REST, Besitzprüfung, WebSocket-Isolation, gespeicherter Status und Kontolöschung erfolgreich');
}
run().catch(error=>{console.error(error.message);process.exit(1);});
