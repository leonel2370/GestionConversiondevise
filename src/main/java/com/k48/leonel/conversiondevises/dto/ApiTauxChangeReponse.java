package com.k48.leonel.conversiondevises.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Corps JSON renvoye par l API Exchangerate-API (v6).
 * <p>
 * Exemple : { "result": "success", "base_code": "USD",
 * "conversion_rates": { "EUR": 0.9123, ... } }
 */
public record ApiTauxChangeReponse(
        @JsonProperty("result") String resultat,
        @JsonProperty("base_code") String deviseBase,
        @JsonProperty("conversion_rates") BigDecimal taux) {
}
