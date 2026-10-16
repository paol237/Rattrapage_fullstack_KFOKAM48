package com.kfokam48.tickets.exception;

/**
 * Signale la violation d'une regle de gestion (RG-02, RG-03).
 * Provoque une reponse 409 avec un message qui nomme la regle.
 */
public class RegleMetierException extends RuntimeException {

    private final String reference;

    public RegleMetierException(String reference, String message) {
        super(message);
        this.reference = reference;
    }

    public String getReference() {
        return reference;
    }
}
