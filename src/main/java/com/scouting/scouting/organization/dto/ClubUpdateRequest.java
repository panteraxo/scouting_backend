package com.scouting.scouting.organization.dto;

import com.scouting.scouting.validation.HttpUrl;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos de reemplazo completo de club. No incluye campos de estado ni pais. */
public record ClubUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres.")
        String name,

        @NotBlank(message = "El tipo es obligatorio.")
        @Size(max = 100, message = "El tipo no debe superar 100 caracteres.")
        String type,

        @NotBlank(message = "El RUC es obligatorio.")
        @Pattern(regexp = "^\\d{11}$", message = "El RUC debe tener 11 digitos.")
        String ruc,

        @NotNull(message = "El anio de fundacion es obligatorio.")
        @Min(value = 1800, message = "El anio de fundacion no puede ser anterior a 1800.")
        @Max(value = 2100, message = "El anio de fundacion no es valido.")
        Integer foundationYear,

        @NotBlank(message = "El logo es obligatorio.")
        @Size(max = 500, message = "El logo no debe superar 500 caracteres.")
        @HttpUrl(message = "El logo debe ser una URL http o https valida con host.")
        String logoUrl,

        @NotBlank(message = "La descripcion es obligatoria.")
        @Size(max = 2000, message = "La descripcion no debe superar 2000 caracteres.")
        String description,

        @NotBlank(message = "El UBIGEO es obligatorio.")
        String ubigeoCode,

        @NotBlank(message = "La direccion es obligatoria.")
        @Size(max = 300, message = "La direccion no debe superar 300 caracteres.")
        String address,

        @NotBlank(message = "El correo institucional es obligatorio.")
        @Email(message = "El correo institucional no es valido.")
        @Size(max = 150, message = "El correo institucional no debe superar 150 caracteres.")
        String institutionalEmail,

        @NotBlank(message = "El telefono institucional es obligatorio.")
        @Size(max = 30, message = "El telefono institucional no debe superar 30 caracteres.")
        String institutionalPhone,

        @Size(max = 500, message = "El sitio web no debe superar 500 caracteres.")
        @HttpUrl(message = "El sitio web debe ser una URL http o https valida con host.")
        String websiteUrl,

        @Size(max = 500, message = "Instagram no debe superar 500 caracteres.")
        @HttpUrl(message = "Instagram debe ser una URL http o https valida con host.")
        String instagramUrl,

        @Size(max = 500, message = "Facebook no debe superar 500 caracteres.")
        @HttpUrl(message = "Facebook debe ser una URL http o https valida con host.")
        String facebookUrl,

        @Size(max = 500, message = "YouTube no debe superar 500 caracteres.")
        @HttpUrl(message = "YouTube debe ser una URL http o https valida con host.")
        String youtubeUrl,

        @Size(max = 500, message = "TikTok no debe superar 500 caracteres.")
        @HttpUrl(message = "TikTok debe ser una URL http o https valida con host.")
        String tiktokUrl
) {
}
