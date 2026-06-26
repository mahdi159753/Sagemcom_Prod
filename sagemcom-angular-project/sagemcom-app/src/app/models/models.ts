export type Role = 'ADMIN' | 'RESPONSABLE_PRODUCTION' | 'INGENIEUR_QUALITE' | 'PREPARATEUR';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  access_token:  string;
  refresh_token: string;
  id?:           number;
  email?:        string;
  firstname?:    string;
  lastname?:     string;
  matricule?:    string;
  poste?:        string;
  role?:         Role;          // ← backend now returns this
}
// ── Production Line ──────────────────────────────────────────────
export type LigneStatut = 'EN_PRODUCTION' | 'ARRETEE' | 'CHANGEMENT_SERIE';

export interface ProductionLine {
  id?: number;
  code: string;
  nom: string;
  chantier?: string;
  produitId?: number;
  produitNom?: string;
  produitReference?: string;
  produitCategory?: string;
  statut: LigneStatut;
  shiftActuel: string;
  responsable: string;
  cadenceReelle: number;
  cadenceObjectif: number;
  efficiency?: number;
  trg: number;
  trs: number;
  fpy: number;
  dernierArret?: string;
  causeArret?: string;
  createdAt?: string;
  updatedAt?: string;
}

// ── Downtime Event ───────────────────────────────────────────────
export interface DowntimeStatsResponse {
  today: string;
  todayChange: string;
  todayIsUp: boolean;
  thisWeek: string;
  weekChange: string;
  weekIsUp: boolean;
  thisMonth: string;
  monthChange: string;
  monthIsUp: boolean;
  avgPerDay: string;
  avgChange: string;
  avgIsUp: boolean;
}

export interface Downtime {
  id?: number;
  ligne: string;
  produit: string;
  type: string;
  description: string;
  startTime?: string | null;
  endTime?: string | null;
  operateur: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  statut?: 'ONGOING' | 'RESOLVED';
  
  // Computed fields on frontend
  duration?: string;
  dateStr?: string;
}

// ── Produit ──────────────────────────────────────────────────────
export interface Produit {
  id?: number;
  name: string;
  reference: string;
  category: string;
  status: string;
  targetRate: number;
  cycleTime: number;
  totalProduced?: number;
  instructionPdfPath?: string;
  createdAt?: string;
  updatedAt?: string;
}

// ── Instruction Sheet ────────────────────────────────────────────
export interface InstructionSheet {
  id: number;
  product: string;
  reference: string;
  version: string;
  status: 'active' | 'draft' | 'archived';
  updated: string;
}

// ── KPI Metric ───────────────────────────────────────────────────
export interface KpiMetric {
  title: string;
  value: number;
  target: number;
  trend: number;
  unit: string;
  status: 'excellent' | 'good' | 'warning' | 'critical';
}

// ── Daily Production Row ─────────────────────────────────────────
export interface DailyProductionRow {
  name: string;
  objectif: number;
  vals: (number | string | null)[];
  global: number | null;
  pct?: boolean;
  lowerBetter?: boolean;
}

// ── TRG History ──────────────────────────────────────────────────
export interface TrgHistoryRow {
  date: string;
  shift: string;
  produced: number;
  target: number;
  trg: number;
  trs: number;
  downtime: string;
}

// ── Alert ────────────────────────────────────────────────────────
export interface Alert {
  title: string;
  description: string;
  severity: 'critical' | 'warning';
  time: string;
}

export interface PosteTravail {
  id: string;
  code: string;
  libelle: string;
  ligne: string;
  type: 'Assemblage' | 'Test' | 'Controle Qualite' | 'Emballage';
  ficheInstruction: string;
  ficheVersion: string;
  operateur: string;
  statut: 'Actif' | 'Inactif';
  trg: number;
  trs: number;
  fpy: number;
}

export interface NonConformite {
  id: string;
  reference: string;
  ligne: string;
  poste: string;
  produit: string;
  description: string;
  gravite: 'Critique' | 'Majeur' | 'Mineur';
  statut: 'Ouverte' | 'En Traitement' | 'Cloturee';
  dateDetection: string;
  operateur: string;
  actionCorrective?: string;
}
export interface User {
  id: number;
  nom: string;
  prenom: string;
  firstname?: string;
  lastname?: string;
  matricule?: string;
  email: string;
  role: Role;
  ligne?: string;
  poste?: string;
  statut: 'Actif' | 'Inactif';
  dateCreation: string;
  dernierAcces?: string;
  lastActive?: string;
}

export interface UserFormData {
  nom: string;
  prenom: string;
  email: string;
  password?: string;   // only used on creation (optional on edit)
  role: Role;
  ligne: string;
  poste: string;
  statut: 'Actif' | 'Inactif';
}
export interface NcStats {
  ouvertes: number;
  enTraitement: number;
  cloturees: number;
  critiques: number;
}

export interface NonConformity {
  id?: number;
  reference: string;
  origine: 'USINE' | 'CLIENT';
  localisation: string;
  gravite: 'MINEUR' | 'MAJEUR' | 'CRITIQUE';
  statut: 'OUVERTE' | 'EN_TRAITEMENT' | 'A_VALIDER' | 'CLOTUREE';
  description: string;
  actionCorrective: string;
  procedureType: string;
  lotConcerne: string;
  valideeParClient: boolean;
  assigneeEmail?: string;
  reportPdfPath?: string;
  dateDeclaration?: string;
  dateCloture?: string;
}

// ── FI Guide Models ──────────────────────────────────────────────
export interface FiDocument {
  id?: number;
  produit?: Produit;
  version: string;
  fileName: string;
  filePath?: string;
  uploadedBy?: User;
  uploadedAt?: string;
  active: boolean;
  extractedImageUrls?: string;
}

export interface InstructionStep {
  id?: number;
  stepNumber: string;
  sequenceOrder: number;
  operation: string;
  description: string;
  warningText?: string;
  imageUrls?: string;
}

export interface ControlSession {
  id?: number;
  fiDocument?: FiDocument;
  produit?: Produit;
  operator?: User;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  startedAt?: string;
  completedAt?: string;
  conforme?: boolean;
}

export interface ControlStepValidation {
  id?: number;
  controlSessionId?: number;
  instructionStep?: InstructionStep;
  status: 'CONFORME' | 'NON_CONFORME';
  comment?: string;
  validatedAt?: string;
}

export interface SessionReportStep {
  stepId: number;
  stepNumber: string;
  operation: string;
  description: string;
  warningText?: string;
  imageUrls?: string;
  validated: boolean;
  status: 'CONFORME' | 'NON_CONFORME' | 'PENDING';
  comment: string;
  validatedAt?: string;
}

export interface SessionReport {
  sessionId: number;
  produitName: string;
  produitRef: string;
  fiVersion: string;
  fiFileName: string;
  operatorName: string;
  operatorEmail: string;
  operatorMatricule?: string;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  conforme: boolean;
  startedAt: string;
  completedAt?: string;
  steps: SessionReportStep[];
}

