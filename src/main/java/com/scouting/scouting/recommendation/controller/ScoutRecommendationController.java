package com.scouting.scouting.recommendation.controller;

import com.scouting.scouting.recommendation.dto.RecommendationDetailResponse;
import com.scouting.scouting.recommendation.service.RecommendationService;
import com.scouting.scouting.security.AppUserPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@PreAuthorize("hasRole('SCOUT')")
public class ScoutRecommendationController {

    private final RecommendationService recommendationService;

    public ScoutRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationDetailResponse> get(@AuthenticationPrincipal AppUserPrincipal actor,
                                                            @PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(recommendationService.getForScout(actor, id));
    }
}
