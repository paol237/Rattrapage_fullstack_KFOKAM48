package com.kfokam48.tickets.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transformation uniforme des erreurs en reponses JSON :
 * 400 pour la validation (RG-01), 404 pour un identifiant inconnu,
 * 409 pour une regle de gestion violee (RG-02, RG-03) — avec un message qui nomme la regle.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validationEchouee(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Le titre est obligatoire.");
        return ResponseEntity.badRequest().body(corps(HttpStatus.BAD_REQUEST, "RG-01", message));
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<Map<String, Object>> introuvable(RessourceIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(corps(HttpStatus.NOT_FOUND, null, ex.getMessage()));
    }

    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<Map<String, Object>> regleViolee(RegleMetierException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(corps(HttpStatus.CONFLICT, ex.getReference(), ex.getMessage()));
    }

    private Map<String, Object> corps(HttpStatus status, String reference, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("horodatage", OffsetDateTime.now());
        body.put("statut", status.value());
        body.put("erreur", status.getReasonPhrase());
        if (reference != null) {
            body.put("regle", reference);
        }
        body.put("message", message);
        return body;
    }
}
