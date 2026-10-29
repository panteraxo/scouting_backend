package com.scouting.scouting.ubigeo.service;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.ubigeo.dto.UbigeoOptionResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Catalogo UBIGEO local basado en la version 2025 del INEI. No realiza llamadas
 * de red en tiempo de ejecucion. La fuente y la licencia se documentan en
 * {@code src/main/resources/ubigeo/README.md}.
 */
@Service
public class UbigeoCatalog {

    private static final String CATALOG_RESOURCE = "ubigeo/ubigeo-inei-2025.csv";
    private static final int EXPECTED_COLUMNS = 6;

    private final Map<String, String> departments;
    private final Map<String, String> provinces;
    private final Map<String, UbigeoDistrict> districts;

    public UbigeoCatalog() {
        LoadedCatalog loaded = loadCatalog();
        this.departments = Collections.unmodifiableMap(loaded.departments());
        this.provinces = Collections.unmodifiableMap(loaded.provinces());
        this.districts = Collections.unmodifiableMap(loaded.districts());
    }

    public List<UbigeoOptionResponse> departments() {
        return departments.entrySet().stream()
                .map(entry -> new UbigeoOptionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<UbigeoOptionResponse> provinces(String departmentCode) {
        requireCode(departmentCode, 2, "INVALID_DEPARTMENT_CODE",
                "El codigo de departamento debe tener 2 digitos.");
        if (!departments.containsKey(departmentCode)) {
            throw ApiException.notFound("UBIGEO_NOT_FOUND", "Departamento no encontrado.");
        }
        return provinces.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(departmentCode))
                .map(entry -> new UbigeoOptionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<UbigeoOptionResponse> districts(String provinceCode) {
        requireCode(provinceCode, 4, "INVALID_PROVINCE_CODE",
                "El codigo de provincia debe tener 4 digitos.");
        if (!provinces.containsKey(provinceCode)) {
            throw ApiException.notFound("UBIGEO_NOT_FOUND", "Provincia no encontrada.");
        }
        return districts.values().stream()
                .filter(district -> district.provinceCode().equals(provinceCode))
                .map(district -> new UbigeoOptionResponse(district.districtCode(), district.districtName()))
                .toList();
    }

    public Optional<UbigeoDistrict> findDistrict(String districtCode) {
        if (districtCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(districts.get(districtCode));
    }

    /** Valida que el codigo corresponda a un distrito del catalogo vigente. */
    public UbigeoDistrict requireDistrict(String districtCode) {
        requireCode(districtCode, 6, "INVALID_UBIGEO",
                "El codigo UBIGEO debe tener 6 digitos.");
        UbigeoDistrict district = districts.get(districtCode);
        if (district == null) {
            throw ApiException.badRequest("INVALID_UBIGEO",
                    "El codigo UBIGEO no existe en el catalogo INEI 2025.");
        }
        return district;
    }

    /**
     * Valida un filtro de ubicacion: 2 digitos (departamento), 4 (provincia) o
     * 6 (distrito). Devuelve null cuando el filtro esta vacio.
     */
    public String normalizeUbigeoFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.matches("\\d{2}|\\d{4}|\\d{6}")) {
            throw ApiException.badRequest("INVALID_UBIGEO_FILTER",
                    "El filtro UBIGEO debe tener 2, 4 o 6 digitos.");
        }
        return normalized;
    }

    private void requireCode(String value, int length, String errorCode, String message) {
        if (value == null || value.length() != length || !value.chars().allMatch(Character::isDigit)) {
            throw ApiException.badRequest(errorCode, message);
        }
    }

    private LoadedCatalog loadCatalog() {
        ClassPathResource resource = new ClassPathResource(CATALOG_RESOURCE);
        Map<String, String> loadedDepartments = new TreeMap<>();
        Map<String, String> loadedProvinces = new TreeMap<>();
        Map<String, UbigeoDistrict> loadedDistricts = new TreeMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split(";", -1);
                if (columns.length != EXPECTED_COLUMNS) {
                    throw new IllegalStateException("Fila UBIGEO invalida: " + line);
                }
                String departmentCode = columns[0].trim();
                String departmentName = columns[1].trim();
                String provinceCode = columns[2].trim();
                String provinceName = columns[3].trim();
                String districtCode = columns[4].trim();
                String districtName = columns[5].trim();

                if (departmentCode.length() != 2 || provinceCode.length() != 4 || districtCode.length() != 6
                        || !districtCode.startsWith(provinceCode)
                        || !provinceCode.startsWith(departmentCode)) {
                    throw new IllegalStateException("Jerarquia UBIGEO invalida: " + line);
                }

                loadedDepartments.putIfAbsent(departmentCode, departmentName);
                loadedProvinces.putIfAbsent(provinceCode, provinceName);
                loadedDistricts.put(districtCode, new UbigeoDistrict(
                        departmentCode, departmentName, provinceCode, provinceName, districtCode, districtName));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar el catalogo UBIGEO local.", ex);
        }

        if (loadedDepartments.isEmpty() || loadedProvinces.isEmpty() || loadedDistricts.isEmpty()) {
            throw new IllegalStateException("El catalogo UBIGEO local esta vacio.");
        }

        return new LoadedCatalog(
                new LinkedHashMap<>(loadedDepartments),
                new LinkedHashMap<>(loadedProvinces),
                new LinkedHashMap<>(loadedDistricts));
    }

    private record LoadedCatalog(
            Map<String, String> departments,
            Map<String, String> provinces,
            Map<String, UbigeoDistrict> districts
    ) {
    }

    public record UbigeoDistrict(
            String departmentCode,
            String departmentName,
            String provinceCode,
            String provinceName,
            String districtCode,
            String districtName
    ) {
    }
}
