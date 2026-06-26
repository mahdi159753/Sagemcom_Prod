package com.alibou.security.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Permission {

    // ── ADMIN ──────────────────────────────────────────────────────────────
    ADMIN_READ("admin:read"),
    ADMIN_CREATE("admin:create"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_DELETE("admin:delete"),

    // ── RESPONSABLE PRODUCTION ─────────────────────────────────────────────
    RESPONSABLE_READ("responsable:read"),
    RESPONSABLE_CREATE("responsable:create"),
    RESPONSABLE_UPDATE("responsable:update"),
    RESPONSABLE_DELETE("responsable:delete"),

    // ── INGENIEUR QUALITE ──────────────────────────────────────────────────
    INGENIEUR_READ("ingenieur:read"),
    INGENIEUR_CREATE("ingenieur:create"),
    INGENIEUR_UPDATE("ingenieur:update"),
    INGENIEUR_DELETE("ingenieur:delete"),

    // ── PREPARATEUR ───────────────────────────────────────────────────────
    PREPARATEUR_READ("preparateur:read"),
    PREPARATEUR_CREATE("preparateur:create"),
    PREPARATEUR_UPDATE("preparateur:update");

    @Getter
    private final String permission;
}
