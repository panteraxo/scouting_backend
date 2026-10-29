package com.scouting.scouting.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.user.entity.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registro publico de ojeador.
 *
 * <p>El request solo puede aportar datos de persona. El rol, el estado y cualquier
 * organizacion se fijan en el servidor; los campos desconocidos del JSON se ignoran
 * y jamas se usan como autoridad.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterScoutRequest(

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

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no es valido.")
        @Size(max = 150, message = "El correo no debe superar 150 caracteres.")
        String email,

        @Size(max = 30, message = "El telefono no debe superar 30 caracteres.")
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,20}$", message = "El telefono no es valido.")
        String phone,

        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$", message = "El nombre de usuario no es valido.")
        String username,

        @NotBlank(message = "La contrasena es obligatoria.")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres.")
        String password
) {
}
