package com.scouting.scouting.player.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.validation.HttpUrl;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerCreateRequest(

        @NotNull(message = "La cuenta del jugador es obligatoria.")
        @Positive(message = "La cuenta del jugador no es valida.")
        Long userId,

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
