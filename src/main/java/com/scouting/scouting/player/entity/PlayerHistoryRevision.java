package com.scouting.scouting.player.entity;

import com.scouting.scouting.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Revision inmutable de una entrada historica. */
@Entity
@Table(
        name = "player_history_revisions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_player_history_revisions_entry_number",
                        columnNames = {"history_entry_id", "revision_number"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PlayerHistoryRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "history_entry_id", nullable = false)
    private PlayerHistoryEntry historyEntry;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private HistoryChangeType changeType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Lob
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edited_by_user_id", nullable = false)
    private User editedBy;

    @Column(name = "edited_at", nullable = false)
    private Instant editedAt;
}
