package com.k48.leonel.conversiondevises.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Requete de conversion : devise source, devise cible et montant.
 */
@Schema(description = "Parametres d une conversion de devise")
public record ConversionRequete(

        @Schema(description = "Code ISO 4217 de la devise source", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La devise source est obligatoire")
        @Size(min = 3, max = 3, message = "La devise source doit contenir exactement 3 lettres")
        @Pattern(regexp = "[A-Za-z]{3}", message = "La devise source doit etre un code de 3 lettres")
        String deviseSource,

        @Schema(description = "Code ISO 4217 de la devise cible", example = "EUR", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La devise cible est obligatoire")
        @Size(min = 3, max = 3, message = "La devise cible doit contenir exactement 3 lettres")
        @Pattern(regexp = "[A-Za-z]{3}", message = "La devise cible doit etre un code de 3 lettres")
        String deviseCible,

        @Schema(description = "Montant a convertir (strictement positif)", example = "100.50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit etre strictement positif")
        BigDecimal montant) {

    /** Normalise les devises en majuscules (ex : "usd" -> "USD"). */
    public ConversionRequete normalisee() {
        return new ConversionRequete(
                deviseSource.trim().toUpperCase(),
                deviseCible.trim().toUpperCase(),
                montant);
    }
}
