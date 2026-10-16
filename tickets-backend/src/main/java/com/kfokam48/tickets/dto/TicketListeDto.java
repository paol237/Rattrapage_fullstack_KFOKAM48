package com.kfokam48.tickets.dto;

import java.util.List;

/**
 * Reponse structuree de la liste : contenu pagine, metadonnees de pagination,
 * tri applique et compteur de tickets ouverts (tous statuts confondus).
 * L'entite Ticket ne sort jamais du service.
 */
public record TicketListeDto(
        List<TicketOutputDto> contenu,
        long totalElements,
        int page,
        int taille,
        int totalPages,
        String tri,
        long ticketsOuverts
) {
}
