package com.scouting.scouting.media.dto;

import com.scouting.scouting.media.entity.MediaKind;

public record MediaCompleteResponse(
        String mediaId,
        MediaKind kind,
        String publicUrl,
        String playbackPath,
        String contentType,
        Long sizeBytes
) {
}
