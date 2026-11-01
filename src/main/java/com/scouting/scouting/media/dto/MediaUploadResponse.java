package com.scouting.scouting.media.dto;

import java.util.Map;

public record MediaUploadResponse(
        String mediaId,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        String publicUrl,
        String playbackPath
) {
}
