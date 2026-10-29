package com.scouting.scouting.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Inicio de sesion por correo. */
public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no es valido.")
        @Size(max = 150, message = "El correo no debe superar 150 caracteres.")
        String email,

        @NotBlank(message = "La contrasena es obligatoria.")
        @Size(max = 72, message = "La contrasena no debe superar 72 caracteres.")
        String password
) {
}
