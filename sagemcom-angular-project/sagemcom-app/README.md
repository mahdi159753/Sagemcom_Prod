# Sagemcom Production Manager – Angular App

Conversion complète du template Figma en application Angular 17 avec composants standalone.

---

## 🚀 Installation & Démarrage

### Prérequis
- Node.js >= 18
- Angular CLI >= 17

### 1. Installer Angular CLI (si pas encore fait)
```bash
npm install -g @angular/cli
```

### 2. Installer les dépendances
```bash
cd sagemcom-app
npm install
```

### 3. Lancer le serveur de développement
```bash
ng serve
```
Ouvrir : **http://localhost:4200**

---

## 📁 Structure du Projet

```
src/
├── app/
│   ├── app.component.ts          # Root component
│   ├── app.config.ts             # App configuration (providers)
│   ├── app.routes.ts             # Routing (lazy loading)
│   │
│   ├── components/
│   │   └── layout/               # Shell : sidebar + topbar + router-outlet
│   │       ├── layout.component.ts
│   │       ├── layout.component.html
│   │       └── layout.component.scss
│   │
│   ├── models/
│   │   └── models.ts             # Toutes les interfaces TypeScript
│   │
│   ├── services/
│   │   └── data.service.ts       # Service centralisé (mock data + chart configs)
│   │
│   └── pages/
│       ├── dashboard/            # Vue d'ensemble + KPI cards + charts
│       ├── production-lines/     # 4 lignes de production
│       ├── daily-production/     # Tableau indicateur chantier (INTEG / CMS2)
│       ├── trg-monitoring/       # Jauge TRG + historique
│       ├── downtimes/            # Suivi des arrêts
│       ├── instruction-sheets/   # Fiches d'instruction (cards grid)
│       ├── kpi-dashboard/        # KPI métriques + graphiques
│       ├── products/             # Catalogue produits Gateway
│       └── settings/             # Configuration + sécurité
│
├── environments/
│   └── environment.ts
├── styles.scss                   # Styles globaux + variables CSS
└── index.html
```

---

## 🧩 Pages & Composants

| Page                | Route                  | Description                                   |
|---------------------|------------------------|-----------------------------------------------|
| Dashboard           | `/dashboard`           | KPI cards, lignes, alertes, timeline, charts  |
| Production Lines    | `/production-lines`    | Statut temps réel de chaque ligne             |
| Daily Production    | `/daily-production`    | Tableau INTEG/CMS2 avec rouge/vert auto       |
| TRG/TRS Monitoring  | `/trg-monitoring`      | Jauge circulaire, mini-charts, historique     |
| Downtimes           | `/downtimes`           | Arrêts filtrables par statut                  |
| Instructions        | `/instructions`        | Fiches d'instruction avec recherche/filtre    |
| KPI Dashboard       | `/kpi`                 | 6 KPI metrics + bar chart + line chart        |
| Products            | `/products`            | Catalogue Gateway avec recherche              |
| Settings            | `/settings`            | Config site, sécurité, notifications, backup  |

---

## 🛠 Technologies

| Technologie    | Version  | Rôle                          |
|----------------|----------|-------------------------------|
| Angular        | 17       | Framework frontend             |
| TypeScript     | 5.2      | Typage statique                |
| Chart.js       | 4.4      | Graphiques (TRG, KPI, Pie)    |
| SCSS           | –        | Styles + variables CSS         |
| RxJS           | 7.8      | Programmation réactive         |

---

## 🔌 Connexion Backend Spring Boot

Remplacer les données mock dans `data.service.ts` par des appels HTTP réels :

```typescript
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

// Exemple :
getProductionLines() {
  return this.http.get<ProductionLine[]>(`${environment.apiUrl}/production-lines`);
}
```

Ajouter `provideHttpClient()` dans `app.config.ts`.

---

## 🎨 Design System

Les variables CSS globales sont dans `src/styles.scss` :

```scss
:root {
  --primary:  #2E75B6;   // Bleu Sagemcom
  --success:  #28A745;   // Vert (objectif atteint)
  --danger:   #DC3545;   // Rouge (alerte / sous objectif)
  --warning:  #FFC107;   // Orange
  --sidebar:  #1E293B;   // Fond sidebar
  --bg:       #F5F7FA;   // Fond général
}
```
