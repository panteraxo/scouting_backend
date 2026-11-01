package com.scouting.scouting.media.entity;

/**
 * Uso funcional del medio. Determina el bucket y las validaciones aplicables.
 */
public enum MediaPurpose {
    PLAYER_PHOTO(MediaKind.IMAGE),
    ORGANIZATION_LOGO(MediaKind.IMAGE),
    PLAYER_HISTORY_VIDEO(MediaKind.VIDEO),
    PROSPECT_EVALUATION_VIDEO(MediaKind.VIDEO);

    private final MediaKind kind;

    MediaPurpose(MediaKind kind) {
        this.kind = kind;
    }

    public MediaKind kind() {
        return kind;
    }
}
