package com.kfokam48.tickets.controller;

import com.kfokam48.tickets.dto.TicketInputDto;
import com.kfokam48.tickets.dto.TicketListeDto;
import com.kfokam48.tickets.dto.TicketOutputDto;
import com.kfokam48.tickets.entity.Ticket;
import com.kfokam48.tickets.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Suivi des demandes des utilisateurs du service de support")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(summary = "Creer un ticket", description = "Le client n'envoie que titre et description. "
            + "RG-01 : titre obligatoire, 100 caracteres max ; RG-02 : titre unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket cree"),
            @ApiResponse(responseCode = "400", description = "RG-01 : titre absent ou de plus de 100 caracteres"),
            @ApiResponse(responseCode = "409", description = "RG-02 : un ticket avec ce titre existe deja")
    })
    public ResponseEntity<TicketOutputDto> creer(@Valid @RequestBody TicketInputDto input) {
        TicketOutputDto cree = ticketService.creer(input);
        return ResponseEntity
                .created(URI.create("/api/tickets/" + cree.id()))
                .body(cree);
    }

    @GetMapping
    @Operation(summary = "Lister les tickets",
            description = "Liste paginee et triee par date de creation. Filtre par statut : OUVERT, EN_COURS ou FERME. "
                    + "tri=asc pour les plus anciens d'abord (desc par defaut : plus recents d'abord). "
                    + "ticketsOuverts porte sur tous les tickets, filtre ou non.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de tickets (eventuellement vide) avec compteur de tickets ouverts")
    })
    public TicketListeDto lister(
            @Parameter(description = "Statut voulu. Absent = tous les tickets.")
            @RequestParam(required = false) Ticket.Statut statut,
            @Parameter(description = "Numero de page, a partir de 0.")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page.")
            @RequestParam(defaultValue = "5") int taille,
            @Parameter(description = "Tri par date de creation : desc (defaut) ou asc.")
            @RequestParam(defaultValue = "desc") String tri) {
        return ticketService.lister(statut, page, taille, tri);
    }

    @PatchMapping("/{id}/fermer")
    @Operation(summary = "Fermer un ticket", description = "RG-03 : impossible si le ticket est deja FERME.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket ferme"),
            @ApiResponse(responseCode = "404", description = "Identifiant inconnu"),
            @ApiResponse(responseCode = "409", description = "RG-03 : le ticket est deja FERME")
    })
    public TicketOutputDto fermer(@PathVariable Long id) {
        return ticketService.fermer(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un ticket", description = "RG-04 : seul un ticket FERME peut etre supprime (action definitive).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ticket supprime"),
            @ApiResponse(responseCode = "404", description = "Identifiant inconnu"),
            @ApiResponse(responseCode = "409", description = "RG-04 : le ticket n'est pas FERME, il ne peut pas etre supprime")
    })
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        ticketService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
