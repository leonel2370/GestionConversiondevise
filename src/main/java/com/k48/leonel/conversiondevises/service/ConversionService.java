package com.k48.leonel.conversiondevises.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.k48.leonel.conversiondevises.dto.ApiTauxChangeReponse;
import com.k48.leonel.conversiondevises.dto.ConversionReponse;
import com.k48.leonel.conversiondevises.dto.ConversionRequete;
import com.k48.leonel.conversiondevises.dto.DevisesReponse;
import com.k48.leonel.conversiondevises.dto.TauxReponse;
import com.k48.leonel.conversiondevises.exception.ApiExterneException;
import com.k48.leonel.conversiondevises.exception.DeviseInvalideException;
import com.k48.leonel.conversiondevises.exception.MontantInvalideException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service coeur de l API : recupere les taux de change depuis l API externe
 * (Exchangerate-API v6), les met en cache, puis realise les conversions.
 * <p>
 * Tous les echanges avec l API externe sont centralises ici afin de traduire
 * les erreurs reseau / HTTP en exceptions metier homogenes.
 */
@Service
public class ConversionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionService.class);

    /** Devises ISO 4217 non echangeables (metaux, codes reserves, hors ligne). */
    private static final Set<String> DEVISES_NON_ECHANGEABLES = Set.of(
            "BOV", "CHE", "CHW", "CLF", "CLP", "COU", "CUC", "DBG", "MCO",
            "MXV", "SLL", "STN", "USN", "USS", "UYI", "UYW", "VED",
            "XBA", "XBB", "XBC", "XBD", "XDR", "XPD", "XPT", "XTS", "XXX");

    private static final BigDecimal MONTANT_MAXIMAL = new BigDecimal("999999999999999");
    private static final Duration DELAI_APPEL = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final String cleApi;
    private final Duration dureeCache;

    /** Cache des taux par devise de base : { "USD" -> { "EUR": 0.91, ... } }. */
    private final Map<String, TauxEnCache> cacheParDevise = new ConcurrentHashMap<>();

    public ConversionService(WebClient webClientTauxChange,
                             @Value("${exchange.api.api-key}") String cleApi,
                             @Value("${exchange.api.cache-taux-minutes:60}") long cacheTauxMinutes) {
        this.webClient = webClientTauxChange;
        this.cleApi = cleApi;
        this.dureeCache = Duration.ofMinutes(cacheTauxMinutes);
    }

    /**
     * Convertit un montant de la devise source vers la devise cible en
     * utilisant le taux de change recupere dynamiquement.
     */
    public ConversionReponse convertir(ConversionRequete requete) {
        ConversionRequete demande = requete.normalisee();
        validerMontant(demande.montant());
        BigDecimal taux = tauxDeChange(demande.deviseSource(), demande.deviseCible());
        BigDecimal montantConverti = demande.montant()
                .multiply(taux)
                .setScale(2, RoundingMode.HALF_UP);
        return new ConversionReponse(
                demande.deviseSource(),
                demande.deviseCible(),
                demande.montant(),
                taux,
                montantConverti,
                Instant.now());
    }

    /** Renvoie le taux de change courant entre deux devises. */
    public TauxReponse obtenirTaux(String deviseSource, String deviseCible) {
        String source = deviseSource.trim().toUpperCase();
        String cible = deviseCible.trim().toUpperCase();
        BigDecimal taux = tauxDeChange(source, cible);
        return new TauxReponse(source, cible, taux, Instant.now().toString());
    }

    /** Liste les devises supportees par l API de taux de change. */
    public DevisesReponse listerDevises() {
        Map<String, String> codes = recupererCodesDevises();
        List<String> devises = codes.keySet().stream()
                .filter(code -> !DEVISES_NON_ECHANGEABLES.contains(code))
                .sorted()
                .toList();
        return new DevisesReponse(devises.size(), devises);
    }

    // =====================================================================
    // Interactions avec l API externe
    // =====================================================================

    /** Cherche le taux en cache, le recharge depuis l API externe si besoin. */
    private BigDecimal tauxDeChange(String deviseSource, String deviseCible) {
        validerDevise(deviseSource);
        validerDevise(deviseCible);

        TauxEnCache entree = cacheParDevise.get(deviseSource);
        if (entree == null || entree.estPerimee(dureeCache)) {
            entree = chargerTaux(deviseSource);
            cacheParDevise.put(deviseSource, entree);
        }

        BigDecimal taux = entree.tauxParDevise().get(deviseCible);
        if (taux == null) {
            throw new DeviseInvalideException(deviseCible,
                    "non trouvee parmi les devises de conversion de " + deviseSource);
        }
        return taux;
    }

    /** Appel GET {base}/{cle}/latest/{deviseBase} et extraction des taux. */
    private TauxEnCache chargerTaux(String deviseBase) {
        JsonNode racine = appelerApi("/latest/" + deviseBase);
        if (racine == null) {
            throw new DeviseInvalideException(deviseBase, "inconnue de l API de taux de change");
        }
        String resultat = racine.path("result").asText("");
        if ("error".equalsIgnoreCase(resultat)) {
            String type = racine.path("error-type").asText("");
            if ("unsupported-code".equals(type)) {
                throw new DeviseInvalideException(deviseBase, "non supportee par l API de taux de change");
            }
            throw new ApiExterneException("L API de taux de change a refuse la requete : %s"
                    .formatted(type.isEmpty() ? "erreur inconnue" : type));
        }

        JsonNode noeudTaux = racine.path("conversion_rates");
        if (!noeudTaux.isObject() || noeudTaux.isEmpty()) {
            throw new ApiExterneException("Reponse inattendue de l API de taux de change : taux absents");
        }
        Map<String, BigDecimal> tauxParDevise = new HashMap<>();
        noeudTaux.fieldNames().forEachRemaining(code -> {
            JsonNode valeur = noeudTaux.get(code);
            if (valeur != null && valeur.isNumber()) {
                tauxParDevise.put(code, valeur.decimalValue());
            }
        });
        return new TauxEnCache(tauxParDevise, Instant.now());
    }

    /** Appel GET {base}/{cle}/codes et extraction de la liste des devises. */
    private Map<String, String> recupererCodesDevises() {
        JsonNode racine = appelerApi("/codes");
        if (racine == null || "error".equalsIgnoreCase(racine.path("result").asText(""))) {
            throw new ApiExterneException("Impossible de recuperer la liste des devises supportees");
        }
        JsonNode noeudCodes = racine.path("supported_codes");
        if (!noeudCodes.isArray() || noeudCodes.isEmpty()) {
            throw new ApiExterneException("Reponse inattendue de l API de taux de change : liste des devises absente");
        }
        Map<String, String> codes = new LinkedHashMap<>();
        noeudCodes.forEach(entree -> {
            if (entree.isArray() && entree.size() >= 2) {
                codes.put(entree.get(0).asText(), entree.get(1).asText());
            }
        });
        return codes;
    }

    /**
     * Execute un appel GET vers l API externe et renvoie le corps JSON.
     * Renvoie {@code null} si l API repond 404 (ressource/devise inconnue) ;
     * leve une {@link ApiExterneException} pour tout autre incident reseau ou HTTP.
     */
    private JsonNode appelerApi(String chemin) {
        try {
            JsonNode reponse = webClient.get()
                    .uri("/" + cleApi + chemin)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(DELAI_APPEL);
            if (reponse == null) {
                throw new ApiExterneException("Reponse vide de l API de taux de change");
            }
            return reponse;
        } catch (WebClientResponseException.NotFound exception) {
            return null;
        } catch (WebClientRequestException exception) {
            throw new ApiExterneException("Impossible de joindre l API de taux de change", exception);
        } catch (WebClientResponseException exception) {
            LOGGER.error("Appel externe en echec : statut={}, location={}, corps={}",
                    exception.getStatusCode(), exception.getHeaders().getLocation(),
                    tronquer(exception.getResponseBodyAsString()));
            throw new ApiExterneException(
                    "L API de taux de change a renvoye une erreur %d".formatted(exception.getStatusCode().value()),
                    exception);
        } catch (IllegalStateException exception) {
            throw new ApiExterneException("Delai depasse lors de l appel a l API de taux de change", exception);
        }
    }

    // =====================================================================
    // Validations internes
    // =====================================================================

    private void validerDevise(String devise) {
        if (DEVISES_NON_ECHANGEABLES.contains(devise)) {
            throw new DeviseInvalideException(devise, "devise non echangeable");
        }
    }

    private void validerMontant(BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) {
            throw new MontantInvalideException("Le montant doit etre strictement positif");
        }
        if (montant.compareTo(MONTANT_MAXIMAL) > 0) {
            throw new MontantInvalideException("Le montant depasse la limite autorisee");
        }
    }

    private String tronquer(String texte) {
        if (texte == null) {
            return "";
        }
        return texte.length() <= 200 ? texte : texte.substring(0, 200) + "...";
    }

    /**
     * Taux d une devise de base, horodate pour la mise en cache.
     * {@link ApiTauxChangeReponse} sert de reference au format de l API externe.
     */
    private record TauxEnCache(Map<String, BigDecimal> tauxParDevise, Instant dateCache) {

        boolean estPerimee(Duration dureeCache) {
            return dateCache.plus(dureeCache).isBefore(Instant.now());
        }
    }
}
