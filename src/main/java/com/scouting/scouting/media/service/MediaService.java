package com.scouting.scouting.media.service;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.media.dto.MediaCompleteResponse;
import com.scouting.scouting.media.dto.MediaUploadRequest;
import com.scouting.scouting.media.dto.MediaUploadResponse;
import com.scouting.scouting.media.entity.MediaAsset;
import com.scouting.scouting.media.entity.MediaKind;
import com.scouting.scouting.media.entity.MediaPurpose;
import com.scouting.scouting.media.entity.MediaStatus;
import com.scouting.scouting.media.repository.MediaAssetRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Orquesta el ciclo de vida de los archivos en R2. */
@Service
public class MediaService {

    static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    static final long MAX_VIDEO_BYTES = 200L * 1024 * 1024;

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm");

    private final MediaAssetRepository mediaAssetRepository;
    private final UserRepository userRepository;
    private final MediaStorage mediaStorage;

    public MediaService(MediaAssetRepository mediaAssetRepository,
                        UserRepository userRepository,
                        MediaStorage mediaStorage) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.userRepository = userRepository;
        this.mediaStorage = mediaStorage;
    }

    @Transactional
    public MediaUploadResponse createUpload(AppUserPrincipal actor, MediaUploadRequest request) {
        MediaKind kind = request.kind();
        MediaPurpose purpose = request.purpose();
        if (actor == null) {
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas.");
        }
        if (actor.role() == com.scouting.scouting.user.entity.Role.CLUB_MANAGER
                && purpose != MediaPurpose.ORGANIZATION_LOGO) {
            throw ApiException.forbidden("MEDIA_UPLOAD_FORBIDDEN",
                    "El responsable de club solo puede subir el logo de su organizacion.");
        }
        if (purpose.kind() != kind) {
            throw ApiException.badRequest("INVALID_MEDIA_PURPOSE", "El uso no corresponde al tipo de archivo.");
        }
        validateContentType(kind, request.contentType());
        validateSize(kind, request.sizeBytes());

        mediaStorage.requireConfigured();
        User owner = userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Credenciales invalidas."));

        String objectKey = buildObjectKey(purpose, request.contentType());
        MediaStorage.Bucket bucket = kind == MediaKind.IMAGE
                ? MediaStorage.Bucket.PUBLIC
                : MediaStorage.Bucket.PRIVATE;
        String uploadUrl = mediaStorage.presignPut(bucket, objectKey, request.contentType());

        MediaAsset asset = new MediaAsset();
        asset.setKind(kind);
        asset.setPurpose(purpose);
        asset.setStatus(MediaStatus.PENDING);
        asset.setBucket(mediaStorage.bucketFor(bucket));
        asset.setObjectKey(objectKey);
        asset.setPublicUrl(kind == MediaKind.IMAGE ? mediaStorage.buildPublicUrl(objectKey) : null);
        asset.setContentType(request.contentType());
        asset.setSizeBytes(request.sizeBytes());
        asset.setUploadedBy(owner);
        MediaAsset saved = mediaAssetRepository.saveAndFlush(asset);

        return new MediaUploadResponse(
                saved.getId(),
                uploadUrl,
                "PUT",
                Map.of("Content-Type", request.contentType()),
                saved.getPublicUrl(),
                playbackPath(saved));
    }

    @Transactional
    public MediaCompleteResponse complete(AppUserPrincipal actor, String mediaId) {
        MediaAsset asset = find(mediaId);
        requireOwner(actor, asset);
        if (asset.getStatus() == MediaStatus.READY) {
            return toCompleteResponse(asset);
        }

        MediaStorage.ObjectMetadata metadata = mediaStorage.head(
                asset.getKind() == MediaKind.IMAGE ? MediaStorage.Bucket.PUBLIC : MediaStorage.Bucket.PRIVATE,
                asset.getObjectKey());
        validateSize(asset.getKind(), metadata.sizeBytes());
        String contentType = metadata.contentType() == null || metadata.contentType().isBlank()
                ? asset.getContentType()
                : metadata.contentType();
        validateContentType(asset.getKind(), contentType);

        asset.setContentType(contentType);
        asset.setSizeBytes(metadata.sizeBytes());
        asset.setStatus(MediaStatus.READY);
        asset.setConfirmedAt(Instant.now());
        MediaAsset saved = mediaAssetRepository.saveAndFlush(asset);
        return toCompleteResponse(saved);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Void> play(AppUserPrincipal actor, String mediaId) {
        MediaAsset asset = find(mediaId);
        if (asset.getStatus() != MediaStatus.READY) {
            throw ApiException.notFound("MEDIA_NOT_READY", "El archivo aun no esta disponible.");
        }
        String target;
        if (asset.getKind() == MediaKind.IMAGE) {
            if (asset.getPublicUrl() == null || asset.getPublicUrl().isBlank()) {
                throw ApiException.notFound("MEDIA_NOT_FOUND", "El archivo no esta disponible.");
            }
            target = asset.getPublicUrl();
        } else {
            target = mediaStorage.presignGet(MediaStorage.Bucket.PRIVATE, asset.getObjectKey());
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", target)
                .header("Cache-Control", "no-store")
                .build();
    }

    private MediaCompleteResponse toCompleteResponse(MediaAsset asset) {
        return new MediaCompleteResponse(
                asset.getId(),
                asset.getKind(),
                asset.getPublicUrl(),
                playbackPath(asset),
                asset.getContentType(),
                asset.getSizeBytes());
    }

    private String playbackPath(MediaAsset asset) {
        return asset.getKind() == MediaKind.VIDEO
                ? "/api/v1/media/" + asset.getId() + "/play"
                : null;
    }

    private MediaAsset find(String mediaId) {
        return mediaAssetRepository.findById(mediaId)
                .orElseThrow(() -> ApiException.notFound("MEDIA_NOT_FOUND", "Archivo no encontrado."));
    }

    private void requireOwner(AppUserPrincipal actor, MediaAsset asset) {
        if (asset.getUploadedBy() == null || !asset.getUploadedBy().getId().equals(actor.id())) {
            throw ApiException.forbidden("MEDIA_NOT_OWNED", "No puedes confirmar este archivo.");
        }
    }

    private void validateSize(MediaKind kind, Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw ApiException.badRequest("INVALID_MEDIA_SIZE", "El tamano del archivo no es valido.");
        }
        long max = kind == MediaKind.IMAGE ? MAX_IMAGE_BYTES : MAX_VIDEO_BYTES;
        if (sizeBytes > max) {
            throw ApiException.badRequest("MEDIA_TOO_LARGE",
                    kind == MediaKind.IMAGE
                            ? "La imagen no debe superar 5 MB."
                            : "El video no debe superar 200 MB.");
        }
    }

    private void validateContentType(MediaKind kind, String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        Set<String> allowed = kind == MediaKind.IMAGE ? IMAGE_TYPES : VIDEO_TYPES;
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("INVALID_MEDIA_TYPE",
                    kind == MediaKind.IMAGE
                            ? "Solo se aceptan imagenes JPEG, PNG o WebP."
                            : "Solo se aceptan videos MP4 o WebM.");
        }
    }

    private String buildObjectKey(MediaPurpose purpose, String contentType) {
        String extension = extension(contentType);
        String folder = purpose.kind() == MediaKind.IMAGE ? "images" : "videos";
        return folder + "/" + purpose.name().toLowerCase(Locale.ROOT) + "/"
                + UUID.randomUUID() + "." + extension;
    }

    private String extension(String contentType) {
        return switch (contentType.trim().toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            default -> "bin";
        };
    }
}
