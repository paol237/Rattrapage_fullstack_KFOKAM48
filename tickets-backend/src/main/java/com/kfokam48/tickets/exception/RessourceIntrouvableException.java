package com.kfokam48.tickets.exception;

/**
 * Signale qu'un identifiant connu n'existe pas en base. Provoque une 404.
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
