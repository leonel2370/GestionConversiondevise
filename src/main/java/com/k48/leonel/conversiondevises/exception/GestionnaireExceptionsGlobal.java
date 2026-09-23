package com.k48.leonel.conversiondevises.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.time.Instant;

/**
 * Gestion centralisee des erreurs de l API.
 * Chaque erreur renvoie un corps RFC 7807 (Problem Detail) homogene :
 * { type, title, status, detail, timestamp }.
 */
@RestControllerAdvice
public class GestionnaireExceptionsGlobal {

    @ExceptionHandler(DeviseInvalideException.class)
    public ProblemDetail deviseInvalide(DeviseInvalideException exception) {
        return construire(HttpStatus.BAD_REQUEST, "Devise invalide", exception.getMessage());
    }

    @ExceptionHandler(ApiExterneException.class)
    public ProblemDetail apiExterne(ApiExterneException exception) {
        return construire(HttpStatus.BAD_GATEWAY, "Erreur de l API de taux de change", exception.getMessage());
    }

    @ExceptionHandler(MontantInvalideException.class)
    public ProblemDetail montantInvalide(MontantInvalideException exception) {
        return construire(HttpStatus.BAD_REQUEST, "Montant invalide", exception.getMessage());
    }

    /** Parametre manquant dans la requete (ex : montant absent). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail parametreManquant(MissingServletRequestParameterException exception) {
        return construire(HttpStatus.BAD_REQUEST, "Parametre manquant",
                "Le parametre obligatoire '%s' est absent de la requete.".formatted(exception.getParameterName()));
    }

    /** Echec de validation d un objet de requete (ex : montant negatif). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validationEchouee(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getDefaultMessage())
                .findFirst()
                .orElse("Donnees de la requete invalides");
        return construire(HttpStatus.BAD_REQUEST, "Requete invalide", message);
    }

    /** Echec de validation d un parametre de methode (ex : devise a 4 lettres). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail contrainteViolee(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Parametres de requete invalides");
        return construire(HttpStatus.BAD_REQUEST, "Parametre invalide", message);
    }

    /** Parametre de type errone (ex : montant=abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail typeParametreInvalide(MethodArgumentTypeMismatchException exception) {
        return construire(HttpStatus.BAD_REQUEST, "Parametre invalide",
                "La valeur '%s' du parametre '%s' est invalide."
                        .formatted(String.valueOf(exception.getValue()), exception.getName()));
    }

    /** Erreur HTTP renvoyee par l API externe (webclient). */
    @ExceptionHandler(WebClientResponseException.class)
    public ProblemDetail erreurWebClient(WebClientResponseException exception) {
        return construire(HttpStatus.BAD_GATEWAY, "Erreur de l API de taux de change",
                "L API de taux de change a repondu avec le code %d.".formatted(exception.getStatusCode().value()));
    }

    /** Filet de securite : toute erreur non prevue renvoie un probleme 500 homogene. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail erreurInattendue(Exception exception) {
        return construire(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue s est produite. Reessayez plus tard.");
    }

    private ProblemDetail construire(HttpStatus statut, String titre, String detail) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
        probleme.setTitle(titre);
        probleme.setType(URI.create("about:blank"));
        probleme.setProperty("timestamp", Instant.now().toString());
        return probleme;
    }
}
