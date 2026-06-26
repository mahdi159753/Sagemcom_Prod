import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../services/data.service';
import { NonConformite, NcStats } from '../../models/models';

@Component({
  selector: 'app-non-conformites',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './non-conformites.component.html',
  styleUrls: ['./non-conformites.component.scss']
})
export class NonConformitesComponent implements OnInit {
  all: NonConformite[] = [];
  filtered: NonConformite[] = [];
  stats!: NcStats;
  showModal = false;

  ligneFilter   = 'all';
  graviteFilter = 'all';
  statutFilter  = 'all';

  // Declare form
  newNC = {
    ligne: '', poste: '', produit: '',
    typeNC: '', description: '', gravite: ''
  };

  constructor(private data: DataService) {}

  ngOnInit() {
    this.all      = this.data.getNonConformites();
    this.filtered = this.all;
    this.calcStats();
  }

  calcStats() {
    this.stats = {
      ouvertes:     this.all.filter(n => n.statut === 'Ouverte').length,
      enTraitement: this.all.filter(n => n.statut === 'En Traitement').length,
      cloturees:    this.all.filter(n => n.statut === 'Cloturee').length,
      critiques:    this.all.filter(n => n.gravite === 'Critique').length,
    };
  }

  applyFilter() {
    this.filtered = this.all.filter(n => {
      const ml = this.ligneFilter   === 'all' || n.ligne   === this.ligneFilter;
      const mg = this.graviteFilter === 'all' || n.gravite === this.graviteFilter;
      const ms = this.statutFilter  === 'all' || n.statut  === this.statutFilter;
      return ml && mg && ms;
    });
  }

  getSeverityColor(g: string): string {
    return { Critique:'#DC3545', Majeur:'#FF8C00', Mineur:'#FFC107' }[g] ?? '#64748B';
  }

  getStatutClass(s: string): string {
    return { Ouverte:'badge-red', 'En Traitement':'badge-yellow', Cloturee:'badge-green' }[s] ?? 'badge-gray';
  }

  cloturer(nc: NonConformite) {
    nc.statut = 'Cloturee';
    nc.actionCorrective = 'Action corrective en cours de saisie.';
    this.calcStats();
    this.applyFilter();
  }

  submitNC() {
    this.showModal = false;
    this.newNC = { ligne:'', poste:'', produit:'', typeNC:'', description:'', gravite:'' };
  }
}
