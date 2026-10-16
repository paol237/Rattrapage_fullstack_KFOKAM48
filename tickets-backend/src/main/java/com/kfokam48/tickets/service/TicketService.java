package com.kfokam48.tickets.service;

import com.kfokam48.tickets.dto.TicketInputDto;
import com.kfokam48.tickets.dto.TicketListeDto;
import com.kfokam48.tickets.dto.TicketOutputDto;
import com.kfokam48.tickets.entity.Ticket;
import com.kfokam48.tickets.exception.RegleMetierException;
import com.kfokam48.tickets.exception.RessourceIntrouvableException;
import com.kfokam48.tickets.repository.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * RG-01 : le titre est obligatoire et <= 100 caracteres (valide par le DTO en amont).
     * Un ticket est cree avec le statut OUVERT ; la dateCreation est generee par le serveur.
     * RG-02 : deux tickets ne peuvent pas avoir le meme titre.
     */
    public TicketOutputDto creer(TicketInputDto input) {
        if (ticketRepository.existsByTitreIgnoreCase(input.titre().trim())) {
            throw new RegleMetierException(
                    "RG-02",
                    "RG-02 : un ticket avec le titre \"" + input.titre().trim() + "\" existe deja."
            );
        }
        Ticket ticket = new Ticket();
        ticket.setTitre(input.titre().trim());
        ticket.setDescription(input.description());
        ticket.setStatut(Ticket.Statut.OUVERT);
        return TicketOutputDto.from(ticketRepository.save(ticket));
    }

    /**
     * Liste paginee, filtrable par statut, triee par date de creation
     * (asc = plus anciens d'abord, desc par defaut = plus recents d'abord).
     * Le compteur de tickets ouverts porte sur tous les tickets, filtre ou non.
     */
    @Transactional(readOnly = true)
    public TicketListeDto lister(Ticket.Statut statut, int page, int taille, String tri) {
        boolean ascendant = "asc".equalsIgnoreCase(tri);
        Sort sort = ascendant
                ? Sort.by("dateCreation").ascending()
                : Sort.by("dateCreation").descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), taille, sort);
        Page<Ticket> resultat = (statut == null)
                ? ticketRepository.findAll(pageable)
                : ticketRepository.findByStatut(statut, pageable);
        return new TicketListeDto(
                resultat.getContent().stream().map(TicketOutputDto::from).toList(),
                resultat.getTotalElements(),
                resultat.getNumber(),
                resultat.getSize(),
                resultat.getTotalPages(),
                ascendant ? "asc" : "desc",
                ticketRepository.countByStatut(Ticket.Statut.OUVERT)
        );
    }

    /**
     * RG-03 : un ticket ne peut etre ferme que s'il est OUVERT ou EN_COURS.
     * Un ticket FERME ne change plus d'etat -> RG-03 -> 409.
     */
    public TicketOutputDto fermer(Long id) {
        Ticket ticket = ticketTrouve(id);
        if (ticket.getStatut() == Ticket.Statut.FERME) {
            throw new RegleMetierException(
                    "RG-03",
                    "RG-03 : le ticket " + id + " est deja FERME et ne peut plus changer d'etat."
            );
        }
        ticket.setStatut(Ticket.Statut.FERME);
        return TicketOutputDto.from(ticketRepository.save(ticket));
    }

    /**
     * Suppression reservee aux tickets FERME -> 409 RG-04 sinon, 404 si l'id est inconnu.
     */
    public void supprimer(Long id) {
        Ticket ticket = ticketTrouve(id);
        if (ticket.getStatut() != Ticket.Statut.FERME) {
            throw new RegleMetierException(
                    "RG-04",
                    "RG-04 : seul un ticket FERME peut etre supprime. Fermez d'abord le ticket " + id + "."
            );
        }
        ticketRepository.delete(ticket);
    }

    private Ticket ticketTrouve(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Le ticket " + id + " n'existe pas."
                ));
    }
}
