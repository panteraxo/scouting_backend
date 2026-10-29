package com.scouting.scouting.user.entity;

/**
 * Tipos de documento de identidad aceptados en el registro de personas.
 *
 * <p>Se mantiene como enumeracion cerrada para no aceptar valores libres desde el
 * navegador. Si el PDF de alcance define otros tipos deben agregarse aqui de forma explicita.</p>
 */
public enum DocumentType {
    DNI,
    CE,
    PASSPORT
}
