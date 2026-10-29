package com.scouting.scouting.player.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;

/** Metrica posicional de una entrada de historial. */
@Entity
@Table(
        name = "player_history_metrics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_player_history_metrics_entry_code",
                        columnNames = {"history_entry_id", "metric_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PlayerHistoryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "history_entry_id", nullable = false)
    private PlayerHistoryEntry historyEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_group", nullable = false, length = 20)
    private PositionGroup metricGroup;

    @Column(name = "metric_code", nullable = false, length = 40)
    private String metricCode;

    @Column(name = "numeric_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal numericValue;
}
