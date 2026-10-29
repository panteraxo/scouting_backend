package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.validation.HttpUrl;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta conjunta de la cuenta PLAYER y su perfil deportivo.
 *
 * <p>La identidad replica las validaciones de {@code AdminUserCreateRequest}. La
 * academia solo se toma del request cuando el actor es ADMIN; para
 * ACADEMY_DIRECTOR y PROFESSOR se deriva de la sesion y el valor enviado se
 * ignora. El perfil replica {@code PlayerCreateRequest} sin {@code userId}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerAccountCreateRequest(

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

        @Positive(message = "La academia no es valida.")
        Long academyId,

        @NotNull(message = "La fecha de nacimiento es obligatoria.")
        LocalDate birthDate,

        @NotBlank(message = "El UBIGEO de residencia es obligatorio.")
        @Pattern(regexp = "^\\d{6}$", message = "El UBIGEO de residencia debe tener 6 digitos.")
        String residenceUbigeoCode,

        @Min(value = 80, message = "La talla debe ser al menos 80 cm.")
        @Max(value = 250, message = "La talla no debe superar 250 cm.")
        Integer heightCm,

        @DecimalMin(value = "20.00", message = "El peso debe ser al menos 20 kg.")
        @DecimalMax(value = "200.00", message = "El peso no debe superar 200 kg.")
        @Digits(integer = 3, fraction = 2, message = "El peso no es valido.")
        BigDecimal weightKg,

        @NotNull(message = "La posicion es obligatoria.")
        PlayerPosition position,

        PlayerPosition bestPosition,

        @NotNull(message = "El pie dominante es obligatorio.")
        DominantFoot dominantFoot,

        @Size(max = 2000, message = "El resumen no debe superar 2000 caracteres.")
        String summary,

        @HttpUrl
        @Size(max = 500, message = "La foto no debe superar 500 caracteres.")
        String photoUrl
) {
}
