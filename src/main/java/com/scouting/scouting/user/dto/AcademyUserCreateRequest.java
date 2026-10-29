package com.scouting.scouting.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Creacion de cuentas desde el alcance de una academia (PROFESSOR o
 * ACADEMY_DIRECTOR). La academia se deriva de la sesion; el request no puede
 * enviar academia, club ni estado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AcademyUserCreateRequest(

        @NotBlank(message = "Los nombres son obligatorios.")
        @Size(max = 100, message = "Los nombres no deben superar 100 caracteres.")
        String firstName,

        @NotBlank(message = "Los apellidos son obligatorios.")
        @Size(max = 100, message = "Los apellidos no deben superar 100 caracteres.")
        String lastName,

        @NotNull(message = "El tipo de documento es obligatorio.")
        DocumentType documentType,

        @NotBlank(message = "El numero de documento es obligatorio.")
        @Pattern(regexp = "^[A-Za-z0-9-]{4,30}$", message = "El numero de documento no es valido.")
        String documentNumber,

        @Size(max = 30, message = "El telefono no debe superar 30 caracteres.")
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,20}$", message = "El telefono no es valido.")
        String phone,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no es valido.")
        @Size(max = 150, message = "El correo no debe superar 150 caracteres.")
        String email,

        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$", message = "El nombre de usuario no es valido.")
        String username,

        @NotNull(message = "El rol es obligatorio.")
        Role role
) {
}
