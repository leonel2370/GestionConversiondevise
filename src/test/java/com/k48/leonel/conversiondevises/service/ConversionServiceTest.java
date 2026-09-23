package com.k48.leonel.conversiondevises.service;

import com.k48.leonel.conversiondevises.dto.ConversionReponse;
import com.k48.leonel.conversiondevises.dto.ConversionRequete;
import com.k48.leonel.conversiondevises.dto.TauxReponse;
import com.k48.leonel.conversiondevises.exception.ApiExterneException;
import com.k48.leonel.conversiondevises.exception.DeviseInvalideException;
import com.k48.leonel.conversiondevises.exception.MontantInvalideException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests du ConversionService avec un serveur HTTP simulant l API externe.
 */
class ConversionServiceTest {

    private MockWebServer serveurApi;
    private ConversionService service;

    @BeforeEach
    void demarrerServeur() throws IOException {
        serveurApi = new MockWebServer();
        serveurApi.start();
        String baseUrl = serveurApi.url("/v6").toString();
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        service = new ConversionService(webClient, "cle-de-test", 60L);
    }

    @AfterEach
    void arreterServeur() throws IOException {
        serveurApi.shutdown();
    }

    private void enregistrerReponseTauxOk() {
        serveurApi.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "result": "success",
                          "base_code": "USD",
                          "conversion_rates": {
                            "USD": 1.0, "EUR": 0.9123, "XAF": 598.42, "GBP": 0.7841
                          }
                        }
                        """));
    }

    @Test
    @DisplayName("convertir calcule le montant converti avec le taux externe")
    void convertirCalculeLeMontant() {
        enregistrerReponseTauxOk();

        ConversionReponse reponse = service.convertir(new ConversionRequete("usd", "eur", new BigDecimal("100.50")));

        assertThat(reponse.deviseSource()).isEqualTo("USD");
        assertThat(reponse.deviseCible()).isEqualTo("EUR");
        assertThat(reponse.montant()).isEqualByComparingTo("100.50");
        assertThat(reponse.taux()).isEqualByComparingTo("0.9123");
        assertThat(reponse.montantConverti()).isEqualByComparingTo("91.69");
    }

    @Test
    @DisplayName("convertir reutilise le taux en cache lors d un second appel")
    void cacheReutiliseLeTaux() {
        enregistrerReponseTauxOk();
        service.convertir(new ConversionRequete("USD", "XAF", BigDecimal.ONE));

        ConversionReponse reponse = service.convertir(new ConversionRequete("USD", "XAF", new BigDecimal("2")));

        assertThat(reponse.taux()).isEqualByComparingTo("598.42");
        assertThat(serveurApi.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("obtenirTaux renvoie le taux courant entre deux devises")
    void obtenirTauxRenvoieLeTaux() {
        enregistrerReponseTauxOk();

        TauxReponse reponse = service.obtenirTaux("EUR", "GBP");

        assertThat(reponse.deviseSource()).isEqualTo("EUR");
        assertThat(reponse.deviseCible()).isEqualTo("GBP");
        assertThat(reponse.taux()).isEqualByComparingTo("0.7841");
    }

    @Test
    @DisplayName("une devise absente des taux declenche DeviseInvalideException")
    void deviseCibleInconnueLeveException() {
        enregistrerReponseTauxOk();

        assertThatThrownBy(() -> service.convertir(new ConversionRequete("USD", "ZZZ", BigDecimal.ONE)))
                .isInstanceOf(DeviseInvalideException.class)
                .hasMessageContaining("ZZZ");
    }

    @Test
    @DisplayName("un 404 de l API externe signifie devise source inconnue")
    void erreur404SignifieDeviseInconnue() {
        serveurApi.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> service.convertir(new ConversionRequete("XXX", "EUR", BigDecimal.ONE)))
                .isInstanceOf(DeviseInvalideException.class)
                .hasMessageContaining("XXX");
    }

    @Test
    @DisplayName("un 500 de l API externe declenche ApiExterneException")
    void erreurServeurLeveApiExterneException() {
        serveurApi.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> service.obtenirTaux("USD", "EUR"))
                .isInstanceOf(ApiExterneException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("une erreur metier de l API externe declenche ApiExterneException")
    void erreurMetierExterneLeveApiExterneException() {
        serveurApi.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"result\": \"error\", \"error-type\": \"quota-exceeded\"}"));

        assertThatThrownBy(() -> service.obtenirTaux("USD", "EUR"))
                .isInstanceOf(ApiExterneException.class)
                .hasMessageContaining("quota-exceeded");
    }

    @Test
    @DisplayName("l appel externe cible le chemin /{cle}/latest/{devise}")
    void cheminAppelExterneCorrect() throws InterruptedException {
        enregistrerReponseTauxOk();

        service.obtenirTaux("USD", "EUR");

        RecordedRequest requete = serveurApi.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/v6/cle-de-test/latest/USD");
    }

    @Test
    @DisplayName("un montant negatif ou nul est refuse")
    void montantInvalideLeveException() {
        assertThatThrownBy(() -> service.convertir(new ConversionRequete("USD", "EUR", BigDecimal.ZERO)))
                .isInstanceOf(MontantInvalideException.class);

        assertThatThrownBy(() -> service.convertir(new ConversionRequete("USD", "EUR", new BigDecimal("-5"))))
                .isInstanceOf(MontantInvalideException.class);
    }
}
