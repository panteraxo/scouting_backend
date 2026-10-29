package com.scouting.scouting.prospecting.entity;

import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Evaluacion privada e inmutable de una prospeccion. Cada POST crea una fila
 * nueva; no existe PUT, PATCH ni DELETE.
 */
@Entity
@Table(
        name = "prospect_evaluations",
        indexes = {
                @Index(name = "idx_prospect_evaluations_prospect_date",
                        columnList = "prospect_id, evaluated_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProspectEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospect_id", nullable = false)
    private Prospect prospect;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDate evaluatedAt;

    @Column(name = "technical_rating", nullable = false)
    private Integer technicalRating;

    @Column(name = "tactical_rating", nullable = false)
    private Integer tacticalRating;

    @Column(name = "physical_rating", nullable = false)
    private Integer physicalRating;

    @Column(name = "mental_rating", nullable = false)
    private Integer mentalRating;

    @Column(name = "overall_rating")
    private Integer overallRating;

    @Column(name = "strengths", length = 2000)
    private String strengths;

    @Column(name = "improvements", length = 2000)
    private String improvements;

    @Column(name = "recommendation", length = 2000)
    private String recommendation;

    @Column(name = "private_notes", length = 2000)
    private String privateNotes;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
