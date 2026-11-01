package com.scouting.scouting.media.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaStorageTest {

    @Test
    void normalizesPublicBaseUrlAndBuildsObjectUrl() {
        MediaStorage storage = new MediaStorage(
                "https://account.r2.cloudflarestorage.com",
                "auto",
                "scouting-public",
                "scouting-private",
                "media.example.com/",
                "public-key",
                "public-secret",
                "private-key",
                "private-secret",
                900);

        assertTrue(storage.isConfigured());
        assertEquals("https://media.example.com/images/player_photo/a.jpg",
                storage.buildPublicUrl("images/player_photo/a.jpg"));
    }

    @Test
    void missingCredentialsAreReportedAsNotConfigured() {
        MediaStorage storage = new MediaStorage(
                "", "auto", "public", "private", "media.example.com",
                "", "", "", "", 900);

        assertFalse(storage.isConfigured());
    }
}
