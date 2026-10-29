package com.scouting.scouting.player.entity;

import com.scouting.scouting.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entrada historica deportiva. Es append-only: las correcciones se auditan en
 * {@link PlayerHistoryRevision} y conservan el autor original.
 */
@Entity
@Table(name = "player_history_entries")
@Getter
@Setter
@NoArgsConstructor
public class PlayerHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_profile_id", nullable = false)
    private PlayerProfile playerProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "competition", length = 150)
    private String competition;

    @Column(name = "opponent", length = 150)
    private String opponent;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_category", length = 20)
    private AgeCategory ageCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "competitive_level", length = 30)
    private CompetitiveLevel competitiveLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_played", length = 30)
    private PlayerPosition positionPlayed;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation", length = 20)
    private Participation participation;

    @Column(name = "minutes_played")
    private Integer minutesPlayed;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_result", length = 10)
    private TeamResult teamResult;

    @Column(name = "team_goals")
    private Integer teamGoals;

    @Column(name = "opponent_goals")
    private Integer opponentGoals;

    @Column(name = "player_goals")
    private Integer playerGoals;

    @Column(name = "assists")
    private Integer assists;

    @Column(name = "yellow_cards")
    private Integer yellowCards;

    @Column(name = "red_cards")
    private Integer redCards;

    @Column(name = "observations", length = 2000)
    private String observations;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "video_timestamp_seconds")
    private Integer videoTimestampSeconds;

    @Column(name = "video_description", length = 500)
    private String videoDescription;

    @Column(name = "technical_rating")
    private Integer technicalRating;

    @Column(name = "tactical_rating")
    private Integer tacticalRating;

    @Column(name = "physical_rating")
    private Integer physicalRating;

    @Column(name = "mental_rating")
    private Integer mentalRating;

    @Column(name = "overall_rating")
    private Integer overallRating;

    @Column(name = "strengths", length = 2000)
    private String strengths;

    @Column(name = "improvements", length = 2000)
    private String improvements;

    @Column(name = "recommendations", length = 2000)
    private String recommendations;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "last_modified_by_user_id", nullable = false)
    private User lastModifiedBy;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @OneToMany(mappedBy = "historyEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerHistoryMetric> metrics = new ArrayList<>();

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
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
