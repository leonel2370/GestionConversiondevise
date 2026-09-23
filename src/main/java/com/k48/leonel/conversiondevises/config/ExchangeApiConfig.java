package com.k48.leonel.conversiondevises.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Configuration de l acces a l API externe de taux de change.
 * <p>
 * Les valeurs sensibles (cle API) proviennent du fichier .env ou de
 * variables d environnement, jamais du code source.
 */
@Configuration
public class ExchangeApiConfig {

    @Bean
    public WebClient webClientTauxChange(
            @Value("${exchange.api.base-url}") String baseUrl) {

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(
                        httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                        reponse -> reponse.createException().flatMap(Mono::error))
                .build();
    }
}
