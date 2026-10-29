package com.scouting.scouting.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contrasena del usuario autenticado. La identidad se obtiene del
 * contexto de seguridad, no del request.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "La contrasena actual es obligatoria.")
        @Size(max = 72, message = "La contrasena actual no debe superar 72 caracteres.")
        String currentPassword,

        @NotBlank(message = "La nueva contrasena es obligatoria.")
        @Size(min = 8, max = 72, message = "La nueva contrasena debe tener entre 8 y 72 caracteres.")
        String newPassword
) {
}
