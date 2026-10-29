package com.scouting.scouting.recommendation.entity;

import com.scouting.scouting.prospecting.entity.ProspectEvaluation;
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

/** Evaluacion seleccionada y congelada en una recomendacion. */
@Entity
@Table(
        name = "recommendation_evaluations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recommendation_evaluation",
                        columnNames = {"recommendation_id", "evaluation_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RecommendationEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private ProspectEvaluation evaluation;
}
