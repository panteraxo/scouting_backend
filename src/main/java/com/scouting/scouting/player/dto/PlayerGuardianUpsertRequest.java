package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.user.entity.DocumentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerGuardianUpsertRequest(

        @NotBlank(message = "Los nombres del tutor son obligatorios.")
        @Size(max = 100, message = "Los nombres del tutor no deben superar 100 caracteres.")
        String firstName,

        @NotBlank(message = "Los apellidos del tutor son obligatorios.")
        @Size(max = 100, message = "Los apellidos del tutor no deben superar 100 caracteres.")
        String lastName,

        @NotNull(message = "El tipo de documento del tutor es obligatorio.")
        DocumentType documentType,

        @NotBlank(message = "El numero de documento del tutor es obligatorio.")
        @Pattern(regexp = "^[A-Za-z0-9-]{4,30}$", message = "El numero de documento del tutor no es valido.")
        String documentNumber,

        @NotBlank(message = "El telefono del tutor es obligatorio.")
        @Pattern(regexp = "^[0-9+()\\-\\s]{6,20}$", message = "El telefono del tutor no es valido.")
        String phone,

        @NotBlank(message = "La direccion del tutor es obligatoria.")
        @Size(max = 300, message = "La direccion del tutor no debe superar 300 caracteres.")
        String address,

        @NotBlank(message = "El parentesco del tutor es obligatorio.")
        @Size(max = 80, message = "El parentesco no debe superar 80 caracteres.")
        String relationship,

        @Min(value = 0, message = "La version del tutor no es valida.")
        Long version
) {
}
