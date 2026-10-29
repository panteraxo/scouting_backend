package com.scouting.scouting.organization.entity;

import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.Instant;

/**
 * Asociacion unica entre una cuenta y una academia o club. Un usuario tiene como
 * maximo una fila. Profesor, Director de academia y Jugador se asocian a una
 * academia; Responsable de club se asocia a un club. ADMIN y SCOUT no tienen
 * fila. El cargo del Responsable de club se guarda en {@link #position} y solo
 * puede existir en una asociacion de club.
 */
@Entity
@Table(
        name = "user_organizations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_organizations_user", columnNames = "user_id")
        }
)
@Check(constraints = "(academy_id is not null and club_id is null and position is null) "
        + "or (academy_id is null and club_id is not null)")
@Getter
@Setter
@NoArgsConstructor
public class UserOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id")
    private Academy academy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    /**
     * Cargo funcional del Responsable de club. Es opcional, pero solo puede
     * informarse en una asociacion con club; nunca en una academia.
     */
    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        validateScope();
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        validateScope();
    }

    private void validateScope() {
        if (user == null || user.getRole() == null) {
            throw new IllegalStateException("UserOrganization requiere un usuario con rol.");
        }
        boolean hasAcademy = academy != null;
        boolean hasClub = club != null;
        if (hasAcademy == hasClub) {
            throw new IllegalStateException("UserOrganization debe tener exactamente una academia o un club.");
        }

        Role role = user.getRole();
        switch (role) {
            case ACADEMY_DIRECTOR, PROFESSOR, PLAYER -> {
                if (!hasAcademy) {
                    throw new IllegalStateException("El rol " + role + " requiere una academia.");
                }
                if (position != null) {
                    throw new IllegalStateException("El cargo solo aplica a un Responsable de club.");
                }
            }
            case CLUB_MANAGER -> {
                if (!hasClub) {
                    throw new IllegalStateException("El rol " + role + " requiere un club.");
                }
            }
            case ADMIN, SCOUT -> throw new IllegalStateException(
                    "El rol " + role + " no debe tener una organizacion asignada.");
        }
    }
}
