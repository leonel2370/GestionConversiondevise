package com.k48.leonel.conversiondevises.exception;

/**
 * Levee quand une devise demandee est inconnue de l API de taux de change
 * (code ISO invalide, ex : "XX" ou "EURO").
 */
public class DeviseInvalideException extends RuntimeException {

    public DeviseInvalideException(String message) {
        super(message);
    }

    public DeviseInvalideException(String devise, String cause) {
        super(String.format("Devise invalide : %s (%s)", devise, cause));
    }
}
