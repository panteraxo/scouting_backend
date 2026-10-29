package com.scouting.scouting.player.dto;

public record PlayerAccountCandidateResponse(
        Long userId,
        String firstName,
        String lastName
) {
}
