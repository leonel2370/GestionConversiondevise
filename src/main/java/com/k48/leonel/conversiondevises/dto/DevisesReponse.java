package com.k48.leonel.conversiondevises.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Liste des devises supportees par l API de taux de change.
 */
@Schema(description = "Devises supportees par l API de taux de change")
public record DevisesReponse(

        @Schema(description = "Nombre de devises disponibles", example = "42")
        int nombre,

        @Schema(description = "Codes ISO 4217 des devises", example = "[\"USD\", \"EUR\", \"XAF\"]")
        List<String> devises) {
}
