package com.scouting.scouting.recommendation.entity;

import com.scouting.scouting.organization.entity.Club;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Respuesta append-only de un club. La vigente es la ultima por fecha/id. */
@Entity
@Table(
        name = "recommendation_responses",
        indexes = {
                @Index(name = "idx_recommendation_responses_recommendation_date",
                        columnList = "recommendation_id, responded_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RecommendationResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responded_by_user_id", nullable = false)
    private User respondedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RecommendationResponseStatus status;

    @Column(name = "message", nullable = false, length = 4000)
    private String message;

    @Column(name = "responded_at", nullable = false)
    private Instant respondedAt;

    @PrePersist
    void onCreate() {
        if (this.respondedAt == null) {
            this.respondedAt = Instant.now();
        }
    }
}
