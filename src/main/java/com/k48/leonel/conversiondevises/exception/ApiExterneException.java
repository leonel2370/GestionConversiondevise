package com.k48.leonel.conversiondevises.exception;

/**
 * Levee quand l API externe de taux de change est injoignable, renvoie une
 * erreur ou une reponse inattendue (indisponibilite, cle API refusee, etc.).
 */
public class ApiExterneException extends RuntimeException {

    public ApiExterneException(String message) {
        super(message);
    }

    public ApiExterneException(String message, Throwable cause) {
        super(message, cause);
    }
}
