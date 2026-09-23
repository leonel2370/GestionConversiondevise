package com.k48.leonel.conversiondevises.exception;

/**
 * Levee quand le montant fourni n est pas exploitable (negatif, non numerique,
 * depassant les bornes raisonnables).
 */
public class MontantInvalideException extends RuntimeException {

    public MontantInvalideException(String message) {
        super(message);
    }
}
