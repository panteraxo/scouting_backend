package com.scouting.scouting.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Secuencia persistida para generar codigos visibles sin exponer IDs internos.
 * La fila se bloquea de forma pesimista durante la asignacion.
 */
@Entity
@Table(name = "organization_code_sequences")
@Getter
@Setter
@NoArgsConstructor
public class OrganizationCodeSequence {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false, length = 20)
    private OrganizationType type;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    public OrganizationCodeSequence(OrganizationType type, long nextValue) {
        this.type = type;
        this.nextValue = nextValue;
    }
}
