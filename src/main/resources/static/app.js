'use strict';
const $=id=>document.getElementById(id);
let graph,pointById=new Map(),segmentById=new Map(),map,networkLayer,routeLayer,markerLayer,pick=null;
function el(tag,text){const e=document.createElement(tag);if(text!==undefined)e.textContent=text;return e}
function time(s){if(!s)return 'não disponível';return new Date(/[Zz]|[+]\d\d:\d\d$/.test(s)?s:s+'Z').toLocaleString('pt-BR',{timeZone:'America/Sao_Paulo',dateStyle:'short',timeStyle:'short'})}
async function api(path,body,method='GET'){
 const headers={'Content-Type':'application/json'};if(method!=='GET'&&$('key').value)headers['X-Admin-Key']=$('key').value;
 const r=await fetch('/api'+path,{method,headers,body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(20000)});
 if(r.status===204)return null;const data=await r.json();if(!r.ok)throw Error(data.message||'Serviço temporariamente indisponível');return data;
}
function active(o){return o.status!=='CLOSED'&&new Date(o.validUntil+'Z')>new Date()}
function mark(){markerLayer.clearLayers();for(const [id,letter] of [['from','A'],['to','B']]){const p=pointById.get(+$(id).value);if(p)L.marker([p.latitude,p.longitude],{icon:L.divIcon({className:'point-label',html:letter,iconSize:[29,29],iconAnchor:[14,14]})}).bindTooltip(p.name).addTo(markerLayer)}}
function clearRoute(){routeLayer?.clearLayers();$('result').replaceChildren();mark()}
function initializeMap(){
 map=L.map('map',{scrollWheelZoom:true}).setView(graph.source.center,15);networkLayer=L.layerGroup().addTo(map);routeLayer=L.layerGroup().addTo(map);markerLayer=L.layerGroup().addTo(map);
 const tiles=L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'}).addTo(map);
 tiles.on('tileerror',()=>{$('map-status').textContent='Fundo cartográfico indisponível. A malha cadastrada continua visível.'});
 map.on('click',event=>{
  if(!pick)return;let best=null,distance=Infinity;for(const p of graph.points){const d=map.distance(event.latlng,[p.latitude,p.longitude]);if(d<distance){best=p;distance=d}}
  if(distance>150){$('selection').textContent='Escolha um ponto a até 150 m da malha destacada.';return}
  $(pick).value=best.id;pick=null;$('selection').textContent='Ponto selecionado na malha cadastrada.';clearRoute();
 });
}
function draw(){
 networkLayer.clearLayers();routeLayer.clearLayers();const blocked=new Set(graph.occurrences.filter(o=>active(o)&&(o.type==='FLOOD'||o.severity==='HIGH')).map(o=>o.segmentId));
 for(const s of graph.segments){const a=pointById.get(s.fromId),b=pointById.get(s.toId);if(!a||!b)continue;
  const popup=el('div');popup.append(el('b',s.name),el('p',s.meters+' m · acessibilidade: '+(s.accessibility==='yes'?'cadastrada':s.accessibility==='no'?'não':'não informada')));
  const choose=el('button','Selecionar trecho no painel');choose.onclick=()=>{$('segment').value=s.id;document.querySelector('details').open=true;$('segment').focus()};popup.append(choose);
  if(s.accessRestriction)popup.append(el('p','Acesso restrito no OpenStreetMap; excluído do cálculo de passagem.'));
  L.polyline([[a.latitude,a.longitude],[b.latitude,b.longitude]],{color:s.blocked||s.accessRestriction||blocked.has(s.id)?'#bd4838':'#688895',weight:s.blocked||s.accessRestriction||blocked.has(s.id)?6:3,opacity:.8}).bindTooltip(el('span',s.name)).bindPopup(popup).addTo(networkLayer);
 }mark();
}
async function loadGraph(){
 graph=await api('/graph');if(graph.simulated)throw Error('Este perfil usa uma rede de testes. Ative a malha real para usar esta interface.');
 pointById=new Map(graph.points.map(p=>[p.id,p]));segmentById=new Map(graph.segments.map(s=>[s.id,s]));if(!graph.points.length)throw Error('Malha cartográfica indisponível.');
 const sorted=[...graph.points].sort((a,b)=>a.name.localeCompare(b.name,'pt-BR'));
 for(const id of ['from','to']){const old=$(id).value;$(id).replaceChildren();for(const p of sorted){const o=el('option',p.name);o.value=p.id;$(id).append(o)}
  if(pointById.has(+old))$(id).value=old;else {const order=[...graph.points].sort((a,b)=>a.longitude-b.longitude);$(id).value=order[Math.floor(order.length*(id==='from'?.25:.75))].id}}
 const previous=$('segment').value;$('segment').replaceChildren();for(const s of [...graph.segments].sort((a,b)=>a.name.localeCompare(b.name,'pt-BR'))){const o=el('option',s.name+' · '+s.meters+' m · '+s.id);o.value=s.id;$('segment').append(o)}if(segmentById.has(+previous))$('segment').value=previous;
 if(!map)initializeMap();draw();$('network-count').textContent=graph.points.length+' pontos · '+graph.segments.length+' trechos';
 $('updated').textContent='Base OSM: '+time(graph.source.osmTimestamp||graph.source.retrievedAt)+' · Condições consultadas: '+time(graph.generatedAt);
 $('alerts').replaceChildren();$('occurrences').replaceChildren();const open=graph.occurrences.filter(active);
 if(!open.length)$('alerts').append(el('p','Nenhuma ocorrência vigente cadastrada nesta malha. As condições das ruas não foram verificadas automaticamente.'));
 for(const o of open){const card=el('div');card.className='card';card.append(el('b',segmentById.get(o.segmentId)?.name||'Trecho cadastrado'),el('p',o.description),el('small','Fonte: '+o.source+' · '+(o.status==='CONFIRMED'?'confirmada por operador':'relatada')+' · observada em '+time(o.observedAt)+' · válida até '+time(o.validUntil)));$('alerts').append(card);
  const row=el('div',o.description);row.className='card';const btn=el('button','Encerrar ocorrência');btn.onclick=async()=>{try{await api('/occurrences/'+o.id,{...o,status:'CLOSED'},'PUT');await loadGraph();$('result').replaceChildren(el('p','Condições alteradas. Calcule o trajeto novamente.'))}catch(e){$('adminResult').textContent=e.message}};row.append(btn);$('occurrences').append(row);
 }
}
async function loadWeather(){
 try{const r=await fetch('/api/weather',{signal:AbortSignal.timeout(15000)});const w=await r.json();$('weather-current').replaceChildren();$('weather-hours').replaceChildren();
  if(!r.ok||!w.data){$('weather-status').textContent='Previsão temporariamente indisponível.';$('weather-source').textContent='Open-Meteo não respondeu com dados atuais. Nenhum valor estimado foi inventado.';return}
  const c=w.data.current;$('weather-status').textContent=w.status==='STALE'?'Última consulta válida — atualização indisponível.':'Dados meteorológicos disponíveis';
  $('weather-current').append(el('strong',Number(c.temperature_2m).toLocaleString('pt-BR')+' °C'),el('p',c.precipitation+' mm · precipitação estimada'));
  const h=w.data.hourly;const now=Date.now();let count=0;
  for(let i=0;i<h.time.length&&count<6;i++){const date=new Date(h.time[i]+'Z');if(date.getTime()<now)continue;const card=el('div');card.className='hour';card.append(el('span',date.toLocaleTimeString('pt-BR',{timeZone:'America/Sao_Paulo',hour:'2-digit',minute:'2-digit'})));const bar=el('div');bar.className='bar';const fill=el('i');const probability=h.precipitation_probability[i];fill.style.height=(probability==null?0:Math.max(2,probability*.4))+'px';bar.append(fill);card.append(bar,el('b',probability==null?'—':probability+'%'),el('span',h.precipitation[i]==null?'—':h.precipitation[i]+' mm'));$('weather-hours').append(card);count++}
  $('weather-source').textContent='Open-Meteo · estimativas de modelo, não sensores locais · referência: '+time(c.time)+' · consulta: '+time(w.fetchedAt)+'. Próximas horas: probabilidade e volume de precipitação. Chuva prevista não confirma alagamento.';
 }catch(e){$('weather-status').textContent='Não foi possível consultar a previsão.';$('weather-current').replaceChildren();$('weather-hours').replaceChildren();$('weather-source').textContent='O cálculo na malha cadastrada continua disponível; a condição meteorológica atual é desconhecida.'}
}
$('route').onclick=async()=>{try{$('error').textContent='';$('route').disabled=true;const r=await api('/routes',{fromId:+$('from').value,toId:+$('to').value,accessible:$('accessible').checked,night:$('night').checked},'POST');routeLayer.clearLayers();$('result').replaceChildren();$('result').append(el('h2',r.status==='FOUND'?(r.meters>=1000?(r.meters/1000).toFixed(2).replace('.',',')+' km':r.meters+' m'):'Sem trajeto com estes critérios'));
 if(r.status==='FOUND'){const list=el('ol');let lastName='';for(const id of r.segments){const s=segmentById.get(id);if(s&&s.name!==lastName){list.append(el('li',s.name));lastName=s.name}}$('result').append(list);const coords=r.points.map(id=>pointById.get(id)).filter(Boolean).map(p=>[p.latitude,p.longitude]);if(coords.length>1){const line=L.polyline(coords,{color:'#087d62',weight:7,opacity:.95}).addTo(routeLayer);map.fitBounds(line.getBounds(),{padding:[35,35],maxZoom:17})}}
 r.warnings.forEach(w=>{const p=el('p',w);p.className='warning';$('result').append(p)});
 }catch(e){$('error').textContent=e.message}finally{$('route').disabled=false}};
for(const id of ['from','to','accessible','night'])$(id).onchange=()=>clearRoute();
for(const id of ['from','to'])$('pick-'+id).onclick=()=>{pick=id;$('selection').textContent='Clique no mapa para marcar '+(id==='from'?'a origem.':'o destino.');map.getContainer().focus()};
$('create').onclick=async()=>{try{if(!$('observed').checked)throw Error('Confirme a verificação da ocorrência antes de publicar.');if($('desc').value.trim().length<10)throw Error('Descreva a ocorrência e sua referência.');const now=new Date(),until=new Date(now.getTime()+3600000);await api('/occurrences',{segmentId:+$('segment').value,type:$('type').value,severity:$('severity').value,status:'REPORTED',source:'OPERATOR',description:$('desc').value,observedAt:now.toISOString().slice(0,-1),validUntil:until.toISOString().slice(0,-1)},'POST');$('adminResult').textContent='Ocorrência registrada por 1 hora. Calcule o trajeto novamente.';$('observed').checked=false;await loadGraph();$('result').replaceChildren()}catch(e){$('adminResult').textContent=e.message}};
$('refresh').onclick=async()=>{$('refresh').disabled=true;try{await Promise.all([loadGraph(),loadWeather()]);$('result').replaceChildren()}catch(e){$('error').textContent=e.message}finally{$('refresh').disabled=false}};
loadGraph().catch(e=>$('error').textContent=e.message);loadWeather();
