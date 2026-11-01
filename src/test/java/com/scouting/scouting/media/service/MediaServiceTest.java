package com.scouting.scouting.media.service;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.media.dto.MediaUploadRequest;
import com.scouting.scouting.media.dto.MediaUploadResponse;
import com.scouting.scouting.media.entity.MediaAsset;
import com.scouting.scouting.media.entity.MediaKind;
import com.scouting.scouting.media.entity.MediaPurpose;
import com.scouting.scouting.media.entity.MediaStatus;
import com.scouting.scouting.media.repository.MediaAssetRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MediaServiceTest {

    private static final Long OWNER_ID = 7L;

    private MediaAssetRepository repository;
    private UserRepository userRepository;
    private MediaStorage storage;
    private MediaService service;

    @BeforeEach
    void setUp() {
        repository = mock(MediaAssetRepository.class);
        userRepository = mock(UserRepository.class);
        storage = mock(MediaStorage.class);
        service = new MediaService(repository, userRepository, storage);
    }

    @Test
    void imageUploadCreatesPendingAssetInPublicBucket() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        when(storage.presignPut(any(), anyString(), anyString())).thenReturn("https://r2.example/upload");
        when(storage.bucketFor(any(MediaStorage.Bucket.class))).thenReturn("scouting-public");
        when(storage.buildPublicUrl(anyString())).thenReturn("https://media.example.com/images/a.jpg");
        when(repository.saveAndFlush(any(MediaAsset.class))).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(0);
            asset.setId("media-1");
            return asset;
        });

        MediaUploadResponse response = service.createUpload(principal(),
                new MediaUploadRequest(MediaKind.IMAGE, MediaPurpose.PLAYER_PHOTO, "image/jpeg", 1024L));

        assertEquals("media-1", response.mediaId());
        assertEquals("https://r2.example/upload", response.uploadUrl());
        assertEquals("PUT", response.method());
        assertEquals("https://media.example.com/images/a.jpg", response.publicUrl());
        assertNull(response.playbackPath());
        verify(repository).saveAndFlush(any(MediaAsset.class));
    }

    @Test
    void videoUploadReturnsStablePlaybackPath() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        when(storage.presignPut(any(), anyString(), anyString())).thenReturn("https://r2.example/upload");
        when(storage.bucketFor(any(MediaStorage.Bucket.class))).thenReturn("scouting-private");
        when(repository.saveAndFlush(any(MediaAsset.class))).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(0);
            asset.setId("media-9");
            return asset;
        });

        MediaUploadResponse response = service.createUpload(principal(),
                new MediaUploadRequest(MediaKind.VIDEO, MediaPurpose.PLAYER_HISTORY_VIDEO, "video/mp4", 2048L));

        assertNull(response.publicUrl());
        assertEquals("/api/v1/media/media-9/play", response.playbackPath());
    }

    @Test
    void imageAboveLimitIsRejectedBeforeStorage() {
        ApiException ex = assertThrows(ApiException.class, () -> service.createUpload(principal(),
                new MediaUploadRequest(MediaKind.IMAGE, MediaPurpose.PLAYER_PHOTO,
                        "image/jpeg", MediaService.MAX_IMAGE_BYTES + 1)));

        assertEquals("MEDIA_TOO_LARGE", ex.getCode());
        verifyNoInteractions(storage, repository, userRepository);
    }

    @Test
    void videoAboveLimitIsRejected() {
        ApiException ex = assertThrows(ApiException.class, () -> service.createUpload(principal(),
                new MediaUploadRequest(MediaKind.VIDEO, MediaPurpose.PLAYER_HISTORY_VIDEO,
                        "video/mp4", MediaService.MAX_VIDEO_BYTES + 1)));

        assertEquals("MEDIA_TOO_LARGE", ex.getCode());
        verifyNoInteractions(storage);
    }

    @Test
    void purposeMustMatchKind() {
        ApiException ex = assertThrows(ApiException.class, () -> service.createUpload(principal(),
                new MediaUploadRequest(MediaKind.VIDEO, MediaPurpose.PLAYER_PHOTO,
                        "video/mp4", 1024L)));

        assertEquals("INVALID_MEDIA_PURPOSE", ex.getCode());
        verifyNoInteractions(storage);
    }

    @Test
    void unsupportedContentTypeIsRejected() {
        ApiException ex = assertThrows(ApiException.class, () -> service.createUpload(principal(),
                new MediaUploadRequest(MediaKind.IMAGE, MediaPurpose.PLAYER_PHOTO,
                        "image/gif", 1024L)));

        assertEquals("INVALID_MEDIA_TYPE", ex.getCode());
        verifyNoInteractions(storage);
    }

    @Test
    void completeMarksOwnedPendingObjectReady() {
        MediaAsset asset = pendingVideo();
        when(repository.findById("media-1")).thenReturn(Optional.of(asset));
        when(storage.head(any(), anyString()))
                .thenReturn(new MediaStorage.ObjectMetadata(2048L, "video/mp4"));
        when(repository.saveAndFlush(asset)).thenReturn(asset);

        service.complete(principal(), "media-1");

        assertEquals(MediaStatus.READY, asset.getStatus());
        assertEquals(2048L, asset.getSizeBytes());
        verify(repository).saveAndFlush(asset);
    }

    @Test
    void completeRejectsAnotherUsersAsset() {
        MediaAsset asset = pendingVideo();
        User other = owner();
        other.setId(99L);
        asset.setUploadedBy(other);
        when(repository.findById("media-1")).thenReturn(Optional.of(asset));

        ApiException ex = assertThrows(ApiException.class, () -> service.complete(principal(), "media-1"));

        assertEquals("MEDIA_NOT_OWNED", ex.getCode());
        verify(storage, never()).head(any(), anyString());
    }

    @Test
    void playVideoRedirectsToPresignedUrl() {
        MediaAsset asset = pendingVideo();
        asset.setStatus(MediaStatus.READY);
        when(repository.findById("media-1")).thenReturn(Optional.of(asset));
        when(storage.presignGet(any(), anyString())).thenReturn("https://r2.example/play");

        var response = service.play(principal(), "media-1");

        assertEquals(302, response.getStatusCode().value());
        assertEquals("https://r2.example/play", response.getHeaders().getLocation().toString());
    }

    private AppUserPrincipal principal() {
        return new AppUserPrincipal(OWNER_ID, "scout@example.com", "hash",
                Role.SCOUT, UserStatus.ACTIVE, false, null);
    }

    private User owner() {
        User user = new User();
        user.setId(OWNER_ID);
        user.setEmail("scout@example.com");
        user.setRole(Role.SCOUT);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private MediaAsset pendingVideo() {
        MediaAsset asset = new MediaAsset();
        asset.setId("media-1");
        asset.setKind(MediaKind.VIDEO);
        asset.setPurpose(MediaPurpose.PLAYER_HISTORY_VIDEO);
        asset.setBucket("scouting-private");
        asset.setObjectKey("videos/player_history_video/a.mp4");
        asset.setContentType("video/mp4");
        asset.setSizeBytes(2048L);
        asset.setStatus(MediaStatus.PENDING);
        asset.setUploadedBy(owner());
        return asset;
    }
}
