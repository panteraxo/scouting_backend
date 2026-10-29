package com.scouting.scouting.recommendation.entity;

import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.prospecting.entity.Prospect;
import com.scouting.scouting.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Recomendacion inmutable de un ojeador a un club. La carta, el snapshot y la
 * seleccion no se editan despues del envio.
 */
@Entity
@Table(
        name = "recommendations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recommendations_prospect_club",
                        columnNames = {"prospect_id", "club_id"})
        },
        indexes = {
                @Index(name = "idx_recommendations_club_sent", columnList = "club_id, sent_at"),
                @Index(name = "idx_recommendations_scout_sent", columnList = "scout_id, sent_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospect_id", nullable = false)
    private Prospect prospect;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scout_id", nullable = false)
    private User scout;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecommendationStatus status;

    @Column(name = "message", nullable = false, length = 4000)
    private String message;

    @Lob
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_type", nullable = false, length = 30)
    private RecommendationAuthorizationType authorizationType;

    @Column(name = "authorized_at", nullable = false)
    private Instant authorizedAt;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.sentAt == null) {
            this.sentAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
