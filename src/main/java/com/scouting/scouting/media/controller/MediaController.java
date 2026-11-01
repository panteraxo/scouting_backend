package com.scouting.scouting.media.controller;

import com.scouting.scouting.media.dto.MediaCompleteResponse;
import com.scouting.scouting.media.dto.MediaUploadRequest;
import com.scouting.scouting.media.dto.MediaUploadResponse;
import com.scouting.scouting.media.service.MediaService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Subida directa a R2 y reproduccion autenticada de videos privados. */
@RestController
@RequestMapping("/api/v1/media")
@Validated
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/uploads")
    @PreAuthorize("hasAnyRole('ADMIN','ACADEMY_DIRECTOR','PROFESSOR','SCOUT','CLUB_MANAGER')")
    public ResponseEntity<MediaUploadResponse> createUpload(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @Valid @RequestBody MediaUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(mediaService.createUpload(actor, request));
    }

    @PostMapping("/{mediaId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaCompleteResponse> complete(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable String mediaId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mediaService.complete(actor, mediaId));
    }

    @GetMapping("/{mediaId}/play")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> play(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable String mediaId) {
        return mediaService.play(actor, mediaId);
    }
}
