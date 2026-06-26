package com.alibou.security.ligneproduction;

public enum LigneStatut {
    EN_PRODUCTION,        // Line running normally
    ARRETEE,              // Stopped (breakdown, maintenance, missing components)
    CHANGEMENT_SERIE      // Changeover between two products
}
