package com.k48.leonel.conversiondevises.controller;

import com.k48.leonel.conversiondevises.dto.ConversionReponse;
import com.k48.leonel.conversiondevises.exception.ApiExterneException;
import com.k48.leonel.conversiondevises.exception.DeviseInvalideException;
import com.k48.leonel.conversiondevises.service.ConversionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests du ConversionController avec la pile MVC complete (validation incluse).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversionService conversionService;

    @Test
    @DisplayName("GET /api/v1/conversion renvoie la conversion complete")
    void conversionNominal() throws Exception {
        when(conversionService.convertir(any()))
                .thenReturn(new ConversionReponse(
                        "USD", "EUR", new BigDecimal("100.50"),
                        new BigDecimal("0.9123"), new BigDecimal("91.69"),
                        Instant.parse("2026-09-23T10:00:00Z")));

        mockMvc.perform(get("/api/v1/conversion")
                        .param("source", "USD")
                        .param("cible", "EUR")
                        .param("montant", "100.50")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devise_source").value("USD"))
                .andExpect(jsonPath("$.devise_cible").value("EUR"))
                .andExpect(jsonPath("$.montant").value(100.50))
                .andExpect(jsonPath("$.taux").value(0.9123))
                .andExpect(jsonPath("$.montant_converti").value(91.69));
    }

    @Test
    @DisplayName("GET /api/v1/conversion sans parametre renvoie 400")
    void parametresManquantsRenvoient400() throws Exception {
        mockMvc.perform(get("/api/v1/conversion").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("une devise inconnue renvoie un probleme 400 normalise")
    void deviseInconnueRenvoie400() throws Exception {
        when(conversionService.convertir(any()))
                .thenThrow(new DeviseInvalideException("ZZZ", "inconnue"));

        mockMvc.perform(get("/api/v1/conversion")
                        .param("source", "USD")
                        .param("cible", "ZZZ")
                        .param("montant", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Devise invalide"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ZZZ")));
    }

    @Test
    @DisplayName("une panne de l API externe renvoie un probleme 502 normalise")
    void panneApiExterneRenvoie502() throws Exception {
        when(conversionService.convertir(any()))
                .thenThrow(new ApiExterneException(
                        "L API de taux de change a renvoye une erreur 500"));

        mockMvc.perform(get("/api/v1/conversion")
                        .param("source", "USD")
                        .param("cible", "EUR")
                        .param("montant", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Erreur de l API de taux de change"));
    }
}
