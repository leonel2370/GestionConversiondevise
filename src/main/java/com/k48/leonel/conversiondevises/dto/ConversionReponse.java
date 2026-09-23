package com.k48.leonel.conversiondevises.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resultat d une conversion de devise.
 */
@Schema(description = "Resultat d une conversion de devise")
public record ConversionReponse(

        @Schema(description = "Devise source (code ISO 4217)", example = "USD")
        @JsonProperty("devise_source")
        String deviseSource,

        @Schema(description = "Devise cible (code ISO 4217)", example = "EUR")
        @JsonProperty("devise_cible")
        String deviseCible,

        @Schema(description = "Montant fourni en entree", example = "100.50")
        @JsonProperty("montant")
        BigDecimal montant,

        @Schema(description = "Taux de change applique (1 unite source = taux unites cible)", example = "0.9123")
        @JsonProperty("taux")
        BigDecimal taux,

        @Schema(description = "Montant converti dans la devise cible", example = "91.69")
        @JsonProperty("montant_converti")
        BigDecimal montantConverti,

        @Schema(description = "Instant de la conversion")
        @JsonProperty("date")
        Instant date) {
}
