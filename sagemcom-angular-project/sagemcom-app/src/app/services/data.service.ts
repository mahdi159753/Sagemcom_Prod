import { Injectable } from '@angular/core';
import {
  ProductionLine, Produit,
  InstructionSheet, KpiMetric, DailyProductionRow, TrgHistoryRow, Alert,
  NonConformite,
  PosteTravail
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class DataService {

  getProductionLines(): any[] {
    return [
      { line:'Line 1', product:'Gateway X300-FR', status:'production', statusLabel:'Production', trg:82, performance:'good', unitsPerHour:180, target:200, efficiency:90, operator:'Marie Durant', startTime:'08:00' },
      { line:'Line 2', product:'Gateway Fiber',   status:'stopped',    statusLabel:'Stopped',    trg:0,  performance:'critical', unitsPerHour:0,   target:190, efficiency:0,  operator:'Pierre Martin', startTime:'08:00' },
      { line:'Line 3', product:'Gateway Pro',     status:'production', statusLabel:'Production', trg:75, performance:'normal', unitsPerHour:165, target:180, efficiency:92, operator:'Sophie Bernard', startTime:'08:00' },
      { line:'Line 4', product:'Gateway X500',    status:'changeover', statusLabel:'Changeover', trg:45, performance:'low',    unitsPerHour:95,  target:210, efficiency:45, operator:'Luc Petit', startTime:'14:00' },
    ];
  }



  getProducts(): Produit[] {
    return [
      { id:1, name:'Gateway X300-FR',   reference:'GW-X300-FR',   category:'Fiber Gateway',      status:'active',      targetRate:180, cycleTime:32, totalProduced:15420 },
      { id:2, name:'Gateway Fiber',     reference:'GW-FIBER',     category:'Fiber Gateway',      status:'active',      targetRate:190, cycleTime:30, totalProduced:18200 },
      { id:3, name:'Gateway Pro',       reference:'GW-PRO',       category:'Premium Gateway',    status:'active',      targetRate:165, cycleTime:35, totalProduced:12800 },
      { id:4, name:'Gateway X500',      reference:'GW-X500',      category:'Enterprise Gateway', status:'active',      targetRate:210, cycleTime:28, totalProduced:9500  },
      { id:5, name:'Gateway Mini',      reference:'GW-MINI',      category:'Entry Gateway',      status:'development', targetRate:250, cycleTime:25, totalProduced:0     },
      { id:6, name:'Gateway Plus',      reference:'GW-PLUS',      category:'Standard Gateway',   status:'active',      targetRate:200, cycleTime:30, totalProduced:21300 },
    ];
  }

  getInstructionSheets(): InstructionSheet[] {
    return [
      { id:1,  product:'Gateway X300-FR',   reference:'GW-X300-FR-v2.1',  version:'v2.1', status:'active',   updated:'Feb 15, 2026' },
      { id:2,  product:'Gateway Fiber',     reference:'GW-FIBER-v1.8',    version:'v1.8', status:'active',   updated:'Feb 12, 2026' },
      { id:3,  product:'Gateway Pro',       reference:'GW-PRO-v3.2',      version:'v3.2', status:'active',   updated:'Feb 10, 2026' },
      { id:4,  product:'Gateway X500',      reference:'GW-X500-v2.5',     version:'v2.5', status:'active',   updated:'Feb 8, 2026'  },
      { id:5,  product:'Gateway Mini',      reference:'GW-MINI-v1.3',     version:'v1.3', status:'draft',    updated:'Feb 5, 2026'  },
      { id:6,  product:'Gateway Plus',      reference:'GW-PLUS-v2.0',     version:'v2.0', status:'active',   updated:'Feb 3, 2026'  },
      { id:7,  product:'Gateway X200-FR',   reference:'GW-X200-FR-v1.9',  version:'v1.9', status:'archived', updated:'Jan 28, 2026' },
      { id:8,  product:'Gateway Ultra',     reference:'GW-ULTRA-v3.0',    version:'v3.0', status:'active',   updated:'Jan 25, 2026' },
      { id:9,  product:'Gateway Lite',      reference:'GW-LITE-v1.5',     version:'v1.5', status:'draft',    updated:'Jan 20, 2026' },
      { id:10, product:'Gateway Enterprise',reference:'GW-ENT-v4.1',      version:'v4.1', status:'active',   updated:'Jan 15, 2026' },
      { id:11, product:'Gateway X100',      reference:'GW-X100-v2.3',     version:'v2.3', status:'archived', updated:'Jan 10, 2026' },
      { id:12, product:'Gateway Max',       reference:'GW-MAX-v3.5',      version:'v3.5', status:'active',   updated:'Jan 5, 2026'  },
    ];
  }

  getKpiMetrics(): KpiMetric[] {
    return [
      { title:'Overall Equipment Effectiveness (OEE)', value:72,    target:85,    trend:-2,   unit:'%',     status:'warning'   },
      { title:'First Pass Yield',                       value:96,    target:95,    trend:1,    unit:'%',     status:'good'      },
      { title:'On-Time Delivery',                       value:94,    target:98,    trend:-1,   unit:'%',     status:'warning'   },
      { title:'Cycle Time',                             value:32,    target:30,    trend:0,    unit:'sec',   status:'warning'   },
      { title:'Scrap Rate',                             value:2.1,   target:3,     trend:-0.3, unit:'%',     status:'good'      },
      { title:'Production Volume',                      value:21450, target:20000, trend:7,    unit:'units', status:'excellent' },
    ];
  }

  getDailyProductionRows(): DailyProductionRow[] {
    return [
      { name:'Quantites Produites',        objectif:2000, vals:[3011,3749,null,null,null,null,null,null], global:6752 },
      { name:'TRG (%)',                    objectif:85,   vals:[83,82,null,null,null,null,null,null],     global:82,   pct:true },
      { name:'Objectif Planning (%)',      objectif:100,  vals:[100,100,null,null,null,null,null,null],   global:100,  pct:true },
      { name:'Efficience / Ecart DMH',     objectif:0,    vals:[1461,843,null,null,null,null,null,null],  global:968,  lowerBetter:true },
      { name:'Depassement Charte',         objectif:0,    vals:[null,null,null,null,null,null,null,null], global:null, lowerBetter:true },
      { name:'FOR (%)',                    objectif:95,   vals:[2.95,1,null,null,null,null,null,null],    global:null, pct:true },
      { name:'AQL',                        objectif:100,  vals:['-','-',null,null,null,null,null,null],   global:null },
      { name:'AQC',                        objectif:100,  vals:['-','-',null,null,null,null,null,null],   global:null },
      { name:'Tx Arrachement (%)',         objectif:2,    vals:[1.75,'-',null,null,null,null,null,null],  global:null, lowerBetter:true },
      { name:'Mur Qualite',               objectif:0,    vals:[45,23,null,null,null,null,null,null],     global:null, lowerBetter:true },
      { name:'QRQC / Tx Cosmetique (%)',  objectif:0.5,  vals:[0.33,0.35,null,null,null,null,null,null], global:null, pct:true, lowerBetter:true },
      { name:'FPY Vision / FCT2 (%)',     objectif:95,   vals:[null,95,null,null,null,null,null,null],   global:null, pct:true },
      { name:'FPY NFT/BWR/SYNCHRO (%)',   objectif:95,   vals:[null,null,null,null,null,null,null,null], global:null },
      { name:'FPY Bouton / NFT (%)',      objectif:95,   vals:[95,null,null,null,null,null,null,null],   global:null, pct:true },
      { name:'FPY Telechargement (%)',    objectif:95,   vals:[96,98,null,null,null,null,null,null],     global:null, pct:true },
      { name:'Encours Depannage',         objectif:0,    vals:[null,null,null,null,null,null,null,null], global:null, lowerBetter:true },
      { name:'Accident de Travail',       objectif:0,    vals:[null,null,null,null,null,null,null,null], global:null, lowerBetter:true },
      { name:'AQF',                       objectif:100,  vals:[null,null,null,null,null,null,null,null], global:null },
    ];
  }

  getTrgHistory(): TrgHistoryRow[] {
    return [
      { date:'Feb 18, 2026', shift:'Morning',   produced:850, target:1000, trg:78, trs:75, downtime:'45 min' },
      { date:'Feb 17, 2026', shift:'Afternoon', produced:920, target:1000, trg:85, trs:82, downtime:'20 min' },
      { date:'Feb 17, 2026', shift:'Morning',   produced:880, target:1000, trg:82, trs:79, downtime:'35 min' },
      { date:'Feb 16, 2026', shift:'Afternoon', produced:950, target:1000, trg:88, trs:85, downtime:'15 min' },
      { date:'Feb 16, 2026', shift:'Morning',   produced:900, target:1000, trg:84, trs:81, downtime:'25 min' },
      { date:'Feb 15, 2026', shift:'Afternoon', produced:870, target:1000, trg:80, trs:77, downtime:'40 min' },
      { date:'Feb 15, 2026', shift:'Morning',   produced:820, target:1000, trg:76, trs:73, downtime:'50 min' },
      { date:'Feb 14, 2026', shift:'Afternoon', produced:940, target:1000, trg:87, trs:84, downtime:'18 min' },
      { date:'Feb 14, 2026', shift:'Morning',   produced:890, target:1000, trg:83, trs:80, downtime:'30 min' },
      { date:'Feb 13, 2026', shift:'Afternoon', produced:910, target:1000, trg:85, trs:82, downtime:'22 min' },
    ];
  }

  getAlerts(): Alert[] {
    return [
      { title:'Line 2 – Machine Failure', description:'Critical: Robot arm malfunction', severity:'critical', time:'2 hours ago' },
      { title:'Line 4 – Low Performance', description:'TRG dropped below 50%',          severity:'warning',  time:'45 minutes ago' },
    ];
  }

  getTrgChartData() {
    return {
      labels: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'],
      datasets: [
        { label:'Line 1', data:[85,88,82,90,86,84,87], borderColor:'#2E75B6', backgroundColor:'rgba(46,117,182,.1)', tension:.3, fill:false, pointRadius:3 },
        { label:'Line 2', data:[78,80,75,82,0,65,70],  borderColor:'#DC3545', backgroundColor:'rgba(220,53,69,.1)',  tension:.3, fill:false, pointRadius:3 },
        { label:'Line 3', data:[82,79,85,88,80,82,85], borderColor:'#28A745', backgroundColor:'rgba(40,167,69,.1)', tension:.3, fill:false, pointRadius:3 },
        { label:'Line 4', data:[75,78,80,82,70,68,75], borderColor:'#FFC107', backgroundColor:'rgba(255,193,7,.1)', tension:.3, fill:false, pointRadius:3 },
      ]
    };
  }

  getDowntimePieData() {
    return {
      labels: ['Machine Failure','Maintenance','Changeover','Material Shortage','Other'],
      datasets: [{
        data: [35,25,20,15,5],
        backgroundColor: ['#DC3545','#FF8C42','#FFC107','#2E75B6','#9CA3AF'],
        borderWidth: 2,
        borderColor: '#fff'
      }]
    };
  }

  getKpiPerformanceData() {
    return {
      labels: ['Jan','Feb','Mar','Apr','May','Jun'],
      datasets: [
        { label:'Target',            data:[20000,20000,20000,20000,20000,20000], backgroundColor:'#E2E8F0' },
        { label:'Actual Production', data:[18500,19200,20100,19800,21000,20500], backgroundColor:'#2E75B6' },
      ]
    };
  }

  getKpiQualityData() {
    return {
      labels: ['Jan','Feb','Mar','Apr','May','Jun'],
      datasets: [{
        label: 'Quality Rate (%)',
        data: [96,97,98,97,98,96],
        borderColor: '#28A745',
        backgroundColor: 'rgba(40,167,69,.1)',
        tension:.4, fill:true, pointRadius:4, borderWidth:3
      }]
    };
  }

  getAvailabilityMiniData() {
    return { labels: ['','','','','','',''], datasets: [{ data:[83,85,84,86,85,87,85], borderColor:'#2E75B6', backgroundColor:'rgba(46,117,182,.15)', tension:.4, fill:true, pointRadius:0, borderWidth:2 }] };
  }
  getPerformanceMiniData() {
    return { labels: ['','','','','','',''], datasets: [{ data:[93,92,91,93,92,91,92], borderColor:'#DC3545', backgroundColor:'rgba(220,53,69,.15)', tension:.4, fill:true, pointRadius:0, borderWidth:2 }] };
  }
  getQualityMiniData() {
    return { labels: ['','','','','','',''], datasets: [{ data:[97,98,98,97,99,98,98], borderColor:'#28A745', backgroundColor:'rgba(40,167,69,.15)', tension:.4, fill:true, pointRadius:0, borderWidth:2 }] };
  }

  getPostesTravail(): PosteTravail[] {
    return [
      { id:'1', code:'P001-ASM', libelle:'Assemblage Carte Mere',   ligne:'Ligne 1', type:'Assemblage',      ficheInstruction:'FI-ASM-001', ficheVersion:'v2.1 PROD', operateur:'Marie Durant',   statut:'Actif',   trg:85, trs:78, fpy:96 },
      { id:'2', code:'P002-TST', libelle:'Test Vision FCT2',        ligne:'Ligne 1', type:'Test',             ficheInstruction:'FI-TST-012', ficheVersion:'v1.5 PROD', operateur:'Pierre Martin',  statut:'Actif',   trg:88, trs:82, fpy:94 },
      { id:'3', code:'P003-QC',  libelle:'Controle Qualite Final',  ligne:'Ligne 1', type:'Controle Qualite', ficheInstruction:'FI-QC-005',  ficheVersion:'v3.0 TEST', operateur:'Sophie Bernard', statut:'Actif',   trg:90, trs:85, fpy:98 },
      { id:'4', code:'P004-EMB', libelle:'Emballage Final',         ligne:'Ligne 1', type:'Emballage',        ficheInstruction:'FI-EMB-002', ficheVersion:'v1.2 PROD', operateur:'Luc Petit',      statut:'Actif',   trg:92, trs:88, fpy:99 },
      { id:'5', code:'P005-ASM', libelle:'Assemblage Boitier',      ligne:'Ligne 2', type:'Assemblage',       ficheInstruction:'FI-ASM-003', ficheVersion:'v1.8 PROD', operateur:'Anne Dubois',    statut:'Actif',   trg:82, trs:75, fpy:95 },
      { id:'6', code:'P006-TST', libelle:'Test Telechargement',     ligne:'Ligne 2', type:'Test',             ficheInstruction:'FI-TST-018', ficheVersion:'v2.3 TEST', operateur:'',               statut:'Inactif', trg:0,  trs:0,  fpy:0  },
    ];
  }

  getNonConformites(): NonConformite[] {
    return [
      { id:'1', reference:'NC-2026-001', ligne:'Ligne 1', poste:'P001-ASM', produit:'Gateway X300-FR', description:'Composant mal soude sur carte mere - Court-circuit detecte',  gravite:'Critique', statut:'En Traitement', dateDetection:'2026-03-06 08:30', operateur:'Marie Durant' },
      { id:'2', reference:'NC-2026-002', ligne:'Ligne 2', poste:'P005-ASM', produit:'Gateway Fiber',   description:'Rayure superficielle sur boitier - Aspect cosmetique',         gravite:'Mineur',   statut:'Cloturee',      dateDetection:'2026-03-05 14:20', operateur:'Pierre Martin',  actionCorrective:'Ajustement de la pression de la pince. Formation operateur effectuee.' },
      { id:'3', reference:'NC-2026-003', ligne:'Ligne 1', poste:'P002-TST', produit:'Gateway X300-FR', description:'Echec test Vision FCT2 - LED defectueuse non detectee',        gravite:'Majeur',   statut:'Ouverte',       dateDetection:'2026-03-06 10:15', operateur:'Sophie Bernard' },
      { id:'4', reference:'NC-2026-004', ligne:'Ligne 3', poste:'P010-QC',  produit:'Gateway Pro',     description:'Etiquette code-barres mal positionnee',                       gravite:'Mineur',   statut:'Cloturee',      dateDetection:'2026-03-04 16:45', operateur:'Luc Petit',      actionCorrective:'Calibration de l applicateur d etiquettes effectuee.' },
    ];
  }

}
