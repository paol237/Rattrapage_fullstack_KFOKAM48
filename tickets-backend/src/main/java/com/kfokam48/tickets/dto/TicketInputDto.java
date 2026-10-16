package com.kfokam48.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ce que le client est autorise a envoyer : uniquement titre et description.
 * Tout le reste (id, statut, dateCreation) est determine par le serveur.
 */
public record TicketInputDto(
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 100, message = "Le titre ne doit pas depasser 100 caracteres.")
        String titre,

        String description
) {
}
