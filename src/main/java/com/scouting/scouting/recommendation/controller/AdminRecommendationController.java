package com.scouting.scouting.recommendation.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.recommendation.dto.RecommendationAdminResponse;
import com.scouting.scouting.recommendation.service.RecommendationService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/recommendations")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminRecommendationController {

    private final RecommendationService recommendationService;

    public AdminRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<RecommendationAdminResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(recommendationService.listAdmin(actor, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendationAdminResponse> get(@AuthenticationPrincipal AppUserPrincipal actor,
                                                           @PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(recommendationService.getAdmin(actor, id));
    }
}
