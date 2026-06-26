package com.alibou.security.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibou.security.user.Permission.*;

@RequiredArgsConstructor
public enum Role {

  /**
   * ADMIN — full access to everything including user management,
   * lines, postes, fiches, KPI and all settings.
   */
  ADMIN(
          Set.of(
                  ADMIN_READ,    ADMIN_CREATE,    ADMIN_UPDATE,    ADMIN_DELETE,
                  RESPONSABLE_READ, RESPONSABLE_CREATE, RESPONSABLE_UPDATE, RESPONSABLE_DELETE,
                  INGENIEUR_READ,   INGENIEUR_CREATE,   INGENIEUR_UPDATE,   INGENIEUR_DELETE,
                  PREPARATEUR_READ, PREPARATEUR_CREATE, PREPARATEUR_UPDATE
          )
  ),

  /**
   * RESPONSABLE_PRODUCTION — can consult all data, manage production lines,
   * postes, view KPI dashboards, generate reports, consult fiches.
   * Cannot manage users or modify fiches.
   */
  RESPONSABLE_PRODUCTION(
          Set.of(
                  RESPONSABLE_READ,
                  RESPONSABLE_CREATE,
                  RESPONSABLE_UPDATE,
                  RESPONSABLE_DELETE,
                  PREPARATEUR_READ
          )
  ),

  /**
   * INGENIEUR_QUALITE — manages fiches d'instruction with versioning,
   * manages products (add/update/delete), manages non-conformités,
   * generates reports.
   */
  INGENIEUR_QUALITE(
          Set.of(
                  INGENIEUR_READ,
                  INGENIEUR_CREATE,
                  INGENIEUR_UPDATE,
                  INGENIEUR_DELETE,
                  PREPARATEUR_READ
          )
  ),

  /**
   * PREPARATEUR — can only read/saisir fiches d'instruction per poste,
   * saisir indicateurs (KPI), consult production data.
   * Read-only + data entry access.
   */
  PREPARATEUR(
          Set.of(
                  PREPARATEUR_READ,
                  PREPARATEUR_CREATE,
                  PREPARATEUR_UPDATE
          )
  );

  @Getter
  private final Set<Permission> permissions;

  public List<SimpleGrantedAuthority> getAuthorities() {
    var authorities = getPermissions()
            .stream()
            .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
            .collect(Collectors.toList());
    authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
    return authorities;
  }
}
