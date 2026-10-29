package com.scouting.scouting.recommendation.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.recommendation.dto.RecommendationCreateRequest;
import com.scouting.scouting.recommendation.dto.RecommendationDetailResponse;
import com.scouting.scouting.recommendation.dto.RecommendationSummaryResponse;
import com.scouting.scouting.recommendation.service.RecommendationService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prospects/{prospectId}/recommendations")
@PreAuthorize("hasRole('SCOUT')")
@Validated
public class ProspectRecommendationController {

    private final RecommendationService recommendationService;

    public ProspectRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<RecommendationSummaryResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long prospectId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(recommendationService.listForProspect(actor, prospectId, page, size));
    }

    @PostMapping
    public ResponseEntity<RecommendationDetailResponse> create(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long prospectId,
            @Valid @RequestBody RecommendationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(recommendationService.create(actor, prospectId, request));
    }
}
