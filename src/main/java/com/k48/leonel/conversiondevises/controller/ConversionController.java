package com.k48.leonel.conversiondevises.controller;

import com.k48.leonel.conversiondevises.dto.ConversionRequete;
import com.k48.leonel.conversiondevises.dto.ConversionReponse;
import com.k48.leonel.conversiondevises.service.ConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Endpoints de conversion de devises, documentes pour Swagger UI.
 */
@RestController
@RequestMapping(path = "/api/v1/conversion", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Conversion", description = "Conversion de sommes d argent d une devise a une autre")
public class ConversionController {

    private final ConversionService conversionService;

    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping
    @Operation(
            summary = "Convertir un montant d une devise vers une autre",
            description = "Recupere le taux de change courant depuis l API externe, "
                    + "puis convertit le montant fourni dans la devise cible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversion realisee"),
            @ApiResponse(responseCode = "400", description = "Parametres manquants, invalides ou devise inconnue"),
            @ApiResponse(responseCode = "502", description = "API de taux de change injoignable ou en erreur")
    })
    public ResponseEntity<ConversionReponse> convertir(
            @Parameter(description = "Code ISO 4217 de la devise source", example = "USD", required = true)
            @RequestParam @NotBlank(message = "La devise source est obligatoire")
            @Size(min = 3, max = 3, message = "La devise source doit contenir exactement 3 lettres")
            @Pattern(regexp = "[A-Za-z]{3}", message = "La devise source doit etre un code de 3 lettres")
            String source,

            @Parameter(description = "Code ISO 4217 de la devise cible", example = "EUR", required = true)
            @RequestParam @NotBlank(message = "La devise cible est obligatoire")
            @Size(min = 3, max = 3, message = "La devise cible doit contenir exactement 3 lettres")
            @Pattern(regexp = "[A-Za-z]{3}", message = "La devise cible doit etre un code de 3 lettres")
            String cible,

            @Parameter(description = "Montant a convertir (strictement positif)", example = "100.50", required = true)
            @RequestParam @NotNull(message = "Le montant est obligatoire")
            @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit etre strictement positif")
            BigDecimal montant) {

        ConversionRequete requete = new ConversionRequete(source, cible, montant);
        return ResponseEntity.ok(conversionService.convertir(requete));
    }
}
