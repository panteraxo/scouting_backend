package com.scouting.scouting.organization.service;

import com.scouting.scouting.organization.entity.OrganizationCodeSequence;
import com.scouting.scouting.organization.entity.OrganizationType;
import com.scouting.scouting.organization.repository.OrganizationCodeSequenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;

/**
 * Asigna codigos visibles secuenciales. La fila de secuencia se bloquea de
 * forma pesimista dentro de una transaccion independiente. Si dos instancias
 * crean la fila inicial a la vez, la que pierde reintenta y lee la fila ya
 * confirmada.
 */
@Service
public class OrganizationCodeAllocator {

    private static final int MAX_ATTEMPTS = 3;

    private final OrganizationCodeSequenceRepository sequenceRepository;
    private final TransactionTemplate transactionTemplate;

    public OrganizationCodeAllocator(OrganizationCodeSequenceRepository sequenceRepository,
                                     PlatformTransactionManager transactionManager) {
        this.sequenceRepository = sequenceRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public synchronized String nextCode(OrganizationType type) {
        DataIntegrityViolationException lastConflict = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return transactionTemplate.execute(status -> allocate(type));
            } catch (DataIntegrityViolationException ex) {
                // Otra instancia creo la fila inicial; se reintenta leyendo la fila ganadora.
                lastConflict = ex;
            }
        }
        throw lastConflict == null
                ? new IllegalStateException("No se pudo asignar el codigo de organizacion.")
                : lastConflict;
    }

    private String allocate(OrganizationType type) {
        OrganizationCodeSequence sequence = sequenceRepository.findForUpdate(type)
                .orElseGet(() -> sequenceRepository.saveAndFlush(new OrganizationCodeSequence(type, 1L)));

        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);

        String prefix = switch (type) {
            case ACADEMY -> "ACA";
            case CLUB -> "CLU";
        };
        return String.format(Locale.ROOT, "%s-%06d", prefix, value);
    }
}
