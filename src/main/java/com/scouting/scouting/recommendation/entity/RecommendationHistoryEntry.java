package com.scouting.scouting.recommendation.entity;

import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Historial seleccionado y congelado en una recomendacion. */
@Entity
@Table(
        name = "recommendation_history_entries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recommendation_history_entry",
                        columnNames = {"recommendation_id", "history_entry_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RecommendationHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "history_entry_id", nullable = false)
    private PlayerHistoryEntry historyEntry;
}
