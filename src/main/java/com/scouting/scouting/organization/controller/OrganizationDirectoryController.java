package com.scouting.scouting.organization.controller;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.organization.dto.AcademyDirectoryResponse;
import com.scouting.scouting.organization.dto.ClubDirectoryResponse;
import com.scouting.scouting.organization.service.OrganizationDirectoryService;
import com.scouting.scouting.security.AppUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Directorio de ojeadores: solo organizaciones activas y verificadas. La vista
 * omite RUC, representantes y datos personales.
 */
@RestController
@RequestMapping("/api/v1/directory")
@PreAuthorize("hasAnyRole('ADMIN','SCOUT')")
@Validated
public class OrganizationDirectoryController {

    private final OrganizationDirectoryService directoryService;

    public OrganizationDirectoryController(OrganizationDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("/academies")
    public PageResponse<AcademyDirectoryResponse> listAcademies(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) @Size(max = 100, message = "La busqueda no debe superar 100 caracteres.")
            String q,
            @RequestParam(required = false) @Size(max = 100, message = "El tipo no debe superar 100 caracteres.")
            String type,
            @RequestParam(required = false) String ubigeo) {
        return directoryService.listAcademies(actor, page, size, sort, q, type, ubigeo);
    }

    @GetMapping("/academies/{id}")
    public AcademyDirectoryResponse getAcademy(@AuthenticationPrincipal AppUserPrincipal actor,
                                               @PathVariable Long id) {
        return directoryService.getAcademy(actor, id);
    }

    @GetMapping("/clubs")
    public PageResponse<ClubDirectoryResponse> listClubs(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano de pagina debe ser al menos 1.")
            @Max(value = 100, message = "El tamano de pagina no debe superar 100.") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(required = false) @Size(max = 100, message = "La busqueda no debe superar 100 caracteres.")
            String q,
            @RequestParam(required = false) @Size(max = 100, message = "El tipo no debe superar 100 caracteres.")
            String type,
            @RequestParam(required = false) String ubigeo) {
        return directoryService.listClubs(actor, page, size, sort, q, type, ubigeo);
    }

    @GetMapping("/clubs/{id}")
    public ClubDirectoryResponse getClub(@AuthenticationPrincipal AppUserPrincipal actor,
                                         @PathVariable Long id) {
        return directoryService.getClub(actor, id);
    }
}
