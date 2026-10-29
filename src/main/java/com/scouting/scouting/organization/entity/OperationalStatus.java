package com.scouting.scouting.organization.entity;

/**
 * Vigencia operativa de una academia o club. Es independiente de su estado de
 * verificacion. Una organizacion inactiva conserva sus relaciones y cuentas,
 * pero queda en solo lectura para sus usuarios asignados.
 */
public enum OperationalStatus {
    ACTIVE,
    INACTIVE
}
