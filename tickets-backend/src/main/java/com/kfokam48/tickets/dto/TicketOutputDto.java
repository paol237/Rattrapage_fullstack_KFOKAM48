package com.kfokam48.tickets.dto;

import com.kfokam48.tickets.entity.Ticket;

import java.time.OffsetDateTime;

/**
 * Representation d'un ticket renvoyee au client.
 * L'entite Ticket ne sort jamais du service.
 */
public record TicketOutputDto(
        Long id,
        String titre,
        String description,
        Ticket.Statut statut,
        OffsetDateTime dateCreation
) {
    public static TicketOutputDto from(Ticket ticket) {
        return new TicketOutputDto(
                ticket.getId(),
                ticket.getTitre(),
                ticket.getDescription(),
                ticket.getStatut(),
                ticket.getDateCreation()
        );
    }
}
