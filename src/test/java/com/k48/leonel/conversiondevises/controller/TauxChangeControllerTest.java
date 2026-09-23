package com.k48.leonel.conversiondevises.controller;

import com.k48.leonel.conversiondevises.dto.DevisesReponse;
import com.k48.leonel.conversiondevises.dto.TauxReponse;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests du TauxChangeController avec la pile MVC complete (validation incluse).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TauxChangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversionService conversionService;

    @Test
    @DisplayName("GET /api/v1/taux renvoie le taux courant")
    void tauxNominal() throws Exception {
        when(conversionService.obtenirTaux("USD", "EUR"))
                .thenReturn(new TauxReponse("USD", "EUR", new BigDecimal("0.9123"), "2026-09-23T10:00:00Z"));

        mockMvc.perform(get("/api/v1/taux")
                        .param("source", "USD")
                        .param("cible", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devise_source").value("USD"))
                .andExpect(jsonPath("$.devise_cible").value("EUR"))
                .andExpect(jsonPath("$.taux").value(0.9123));
    }

    @Test
    @DisplayName("GET /api/v1/devises renvoie la liste des devises supportees")
    void devisesNominal() throws Exception {
        when(conversionService.listerDevises())
                .thenReturn(new DevisesReponse(3, List.of("EUR", "USD", "XAF")));

        mockMvc.perform(get("/api/v1/devises").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(3))
                .andExpect(jsonPath("$.devises.length()").value(3));
    }

    @Test
    @DisplayName("un code de devise mal forme renvoie 400 via la validation")
    void deviseMalFormeeRenvoie400() throws Exception {
        mockMvc.perform(get("/api/v1/taux")
                        .param("source", "EURO")
                        .param("cible", "EUR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("un montant non numerique renvoie 400")
    void montantNonNumeriqueRenvoie400() throws Exception {
        mockMvc.perform(get("/api/v1/conversion")
                        .param("source", "USD")
                        .param("cible", "EUR")
                        .param("montant", "abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
