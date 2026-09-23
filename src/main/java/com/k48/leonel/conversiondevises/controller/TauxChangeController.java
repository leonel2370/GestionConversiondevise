package com.k48.leonel.conversiondevises.controller;

import com.k48.leonel.conversiondevises.dto.DevisesReponse;
import com.k48.leonel.conversiondevises.dto.TauxReponse;
import com.k48.leonel.conversiondevises.service.ConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de consultation des devises et des taux de change.
 */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Taux de change", description = "Consultation des devises supportees et des taux courants")
public class TauxChangeController {

    private final ConversionService conversionService;

    public TauxChangeController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/devises")
    @Operation(summary = "Lister les devises supportees",
            description = "Renvoie la liste des codes ISO 4217 disponibles sur l API de taux de change.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des devises"),
            @ApiResponse(responseCode = "502", description = "API de taux de change injoignable ou en erreur")
    })
    public ResponseEntity<DevisesReponse> listerDevises() {
        return ResponseEntity.ok(conversionService.listerDevises());
    }

    @GetMapping("/taux")
    @Operation(summary = "Consulter le taux de change courant",
            description = "Renvoie le taux actuel d une devise source vers une devise cible, sans conversion.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Taux courant"),
            @ApiResponse(responseCode = "400", description = "Devise inconnue ou parametres invalides"),
            @ApiResponse(responseCode = "502", description = "API de taux de change injoignable ou en erreur")
    })
    public ResponseEntity<TauxReponse> obtenirTaux(
            @Parameter(description = "Code ISO 4217 de la devise source", example = "USD", required = true)
            @RequestParam @Size(min = 3, max = 3, message = "La devise source doit contenir exactement 3 lettres")
            @Pattern(regexp = "[A-Za-z]{3}", message = "La devise source doit etre un code de 3 lettres")
            String source,

            @Parameter(description = "Code ISO 4217 de la devise cible", example = "XAF", required = true)
            @RequestParam @Size(min = 3, max = 3, message = "La devise cible doit contenir exactement 3 lettres")
            @Pattern(regexp = "[A-Za-z]{3}", message = "La devise cible doit etre un code de 3 lettres")
            String cible) {

        return ResponseEntity.ok(conversionService.obtenirTaux(source, cible));
    }
}
