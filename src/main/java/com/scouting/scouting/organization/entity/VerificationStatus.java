package com.scouting.scouting.organization.entity;

/**
 * Estado de verificacion de una organizacion. Solo ADMIN puede cambiarlo.
 *
 * <p>Transiciones permitidas:</p>
 * <ul>
 *   <li>{@link #PENDING_VERIFICATION} -&gt; {@link #VERIFIED} o {@link #REJECTED}</li>
 *   <li>{@link #REJECTED} -&gt; {@link #PENDING_VERIFICATION} (reabrir)</li>
 *   <li>{@link #VERIFIED} -&gt; {@link #PENDING_VERIFICATION} (revocar)</li>
 * </ul>
 */
public enum VerificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED
}
