import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../services/data.service';
import { DailyProductionService, DailyProductionDTO, DailyProductionIndicator, OeePrediction } from '../../services/daily-production.service';
import { AuthService } from '../../services/auth.service';
import { DailyProductionRow } from '../../models/models';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-daily-production',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './daily-production.component.html',
  styleUrls: ['./daily-production.component.scss']
})
export class DailyProductionComponent implements OnInit {
  chantier = 'INTEG';
  selectedDate = new Date().toISOString().split('T')[0];
  rows: DailyProductionRow[] = [];
  chantiers = ['INTEG', 'CMS2', 'CMS1', 'ASSEMBLY'];
  lines = [1,2,3,4,5,6,7,8];
  
  // AI Predictions
  oeePredictions: OeePrediction[] = [];
  isPredicting = false;

  constructor(
    private data: DataService, 
    private dpService: DailyProductionService, 
    public authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.initEmptyRows();
    this.fetchData();
  }

  initEmptyRows() {
    // Get the indicator structure from data service, but empty out the values.
    const templateRows = this.data.getDailyProductionRows();
    templateRows.forEach(r => {
      r.vals = [null, null, null, null, null, null, null, null];
      r.global = null;
    });
    this.rows = templateRows;
  }

  fetchData() {
    this.dpService.getIndicators(this.selectedDate, this.chantier).subscribe({
      next: (res) => {
        this.initEmptyRows(); // reset
        
        // Populate specific cells
        res.forEach(ind => {
          const row = this.rows.find(r => r.name === ind.indicatorName);
          if (row) {
            if (ind.ligne >= 1 && ind.ligne <= 8) {
               row.vals[ind.ligne - 1] = ind.stringValue;
            } else if (ind.ligne === 0) {
               row.objectif = parseFloat(ind.stringValue);
            }
          }
        });

        // Recalculate globals
        this.recalcGlobals();
        
        // Fetch AI Predictions
        this.fetchAiPredictions();
      },
      error: (e) => console.error(e)
    });
  }

  fetchAiPredictions() {
    this.isPredicting = true;
    this.dpService.getOeePredictions(this.chantier).subscribe({
      next: (preds) => {
        this.oeePredictions = preds;
        this.isPredicting = false;
      },
      error: (e) => {
        console.error('Error fetching AI predictions', e);
        this.isPredicting = false;
      }
    });
  }

  recalcGlobals() {
    this.rows.forEach(r => {
      let sum = 0;
      let count = 0;
      r.vals.forEach(v => {
        if (v !== null && v !== '') {
          const n = parseFloat(String(v).replace(',', '.'));
          if (!isNaN(n)) {
            sum += n;
            count++;
          }
        }
      });

      if (count === 0) {
        r.global = null;
      } else {
        if (r.pct || r.name.includes('%')) {
          r.global = Number((sum / count).toFixed(2));
        } else {
          r.global = Number(sum.toFixed(2));
        }
      }
    });
  }

  onCellChange(rowIdx: number, colIdx: number, event: any) {
    this.recalcGlobals();
  }

  trackByIndex(index: number, obj: any): any {
    return index;
  }

  async clearTable() {
    const confirmed = await this.notificationService.confirmAction(
      'Vider le tableau ?', 
      'Les données restent sauvegardées en base de données.'
    );
    if (confirmed) {
      this.rows.forEach(r => {
        r.vals = [null, null, null, null, null, null, null, null];
        r.global = null;
      });
    }
  }

  downloadCSV() {
    let csvContent = 'data:text/csv;charset=utf-8,';
    csvContent += 'Indicateurs;Objectif;Ligne 1;Ligne 2;Ligne 3;Ligne 4;Ligne 5;Ligne 6;Ligne 7;Ligne 8;Global\n';
    
    this.rows.forEach(r => {
      const rowArr = [
        r.name,
        r.objectif,
        ...(r.vals.map(v => v !== null ? v : '')),
        r.global !== null ? r.global : ''
      ];
      csvContent += rowArr.join(';') + '\n';
    });

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `daily_production_${this.chantier}_${this.selectedDate}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  displayVal(val: number | string | null): string {
    if (val === null) return '';
    return String(val);
  }

  getCellClass(val: number | string | null, obj: number, lowerBetter = false): string {
    if (val === null || val === '' || val === '-') return '';
    const n = parseFloat(String(val).replace(',','.'));
    const o = parseFloat(String(obj).replace(',','.'));
    if (isNaN(n) || isNaN(o)) return '';
    
    if (lowerBetter) return n <= o ? 'cell-green' : 'cell-red';
    return n >= o ? 'cell-green' : 'cell-red';
  }

  async deleteDayData() {
    if (!this.authService.isAdmin()) return;
    const confirmed = await this.notificationService.confirmDelete(
      'Supprimer toutes les données ?', 
      'Êtes-vous sûr de vouloir supprimer TOUTES les données de cette date pour ce chantier ? (Irréversible)'
    );
    if (confirmed) {
      this.dpService.deleteByDateAndChantier(this.selectedDate, this.chantier).subscribe({
        next: () => {
          this.notificationService.showSuccess('Succès', 'Données supprimées avec succès.');
          this.fetchData(); // reload
        },
        error: (e) => {
          console.error(e);
          this.notificationService.showError('Erreur', 'Impossible de supprimer les données.');
        }
      });
    }
  }

  async deleteRowData(idx: number) {
    if (!this.authService.isAdmin()) return;
    const rowName = this.rows[idx].name;
    const confirmed = await this.notificationService.confirmDelete(
      'Supprimer la ligne ?', 
      'Supprimer les données de l\'indicateur ' + rowName + ' pour cette date ?'
    );
    if (confirmed) {
      this.dpService.deleteIndicator(this.selectedDate, this.chantier, rowName).subscribe({
        next: () => {
          this.rows[idx].vals = [null, null, null, null, null, null, null, null];
          this.rows[idx].objectif = undefined as any;
          this.recalcGlobals();
          this.notificationService.showSuccess('Succès', 'Ligne supprimée.');
        },
        error: (e) => {
          console.error(e);
          this.notificationService.showError('Erreur', 'Impossible de supprimer cette ligne.');
        }
      });
    }
  }

  onSave() {
    const batch: DailyProductionDTO[] = [];

    this.rows.forEach(r => {
      // Save objectif
      if (r.objectif !== undefined && r.objectif !== null) {
        batch.push({
          date: this.selectedDate,
          chantier: this.chantier,
          ligne: 0,
          indicatorName: r.name,
          value: String(r.objectif)
        });
      }
      
      // Save line values
      r.vals.forEach((v, idx) => {
        if (v !== null && v !== '') {
          batch.push({
            date: this.selectedDate,
            chantier: this.chantier,
            ligne: idx + 1,
            indicatorName: r.name,
            value: String(v)
          });
        }
      });
    });

    if (batch.length === 0) {
      this.notificationService.showInfo('Aucune modification', 'Aucune donnée à sauvegarder.');
      return;
    }

    this.dpService.saveBatch(batch).subscribe({
      next: () => {
        // Flash success
        const btn = document.querySelector('.btn-primary') as HTMLElement;
        if (btn) {
          const originalText = btn.innerHTML;
          btn.innerHTML = '✔ Enregistré !';
          btn.style.backgroundColor = '#28a745';
          setTimeout(() => {
            btn.innerHTML = originalText;
            btn.style.backgroundColor = '';
          }, 2000);
        }
      },
      error: (e) => {
        console.error("Error saving batch", e);
        this.notificationService.showError("Erreur", "Erreur lors de la sauvegarde.");
      }
    });
  }
}
