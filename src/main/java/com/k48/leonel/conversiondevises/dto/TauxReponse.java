package com.k48.leonel.conversiondevises.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Taux de change d une devise source vers une devise cible.
 */
@Schema(description = "Taux de change entre deux devises")
public record TauxReponse(

        @Schema(description = "Devise source (code ISO 4217)", example = "USD")
        String deviseSource,

        @Schema(description = "Devise cible (code ISO 4217)", example = "EUR")
        String deviseCible,

        @Schema(description = "Taux de change applique", example = "0.9123")
        BigDecimal taux,

        @Schema(description = "Instant d observation du taux")
        String date) {
}
