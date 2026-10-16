package com.kfokam48.tickets.service;

import com.kfokam48.tickets.dto.TicketInputDto;
import com.kfokam48.tickets.dto.TicketOutputDto;
import com.kfokam48.tickets.entity.Ticket;
import com.kfokam48.tickets.exception.RegleMetierException;
import com.kfokam48.tickets.exception.RessourceIntrouvableException;
import com.kfokam48.tickets.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * Liste de tous les tickets, ou seulement ceux d'un statut donne (filtre par statut).
     */
    @Transactional(readOnly = true)
    public List<TicketOutputDto> lister(Ticket.Statut statut) {
        List<Ticket> tickets = (statut == null)
                ? ticketRepository.findAllByOrderByIdAsc()
                : ticketRepository.findByStatutOrderByIdAsc(statut);
        return tickets.stream().map(TicketOutputDto::from).toList();
    }

    /**
     * RG-03 : un ticket ne peut etre ferme que s'il est OUVERT ou EN_COURS.
     * Un ticket FERME ne change plus d'etat -> RG-03 -> 409.
     */
    public TicketOutputDto fermer(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Le ticket " + id + " n'existe pas."
                ));
        if (ticket.getStatut() == Ticket.Statut.FERME) {
            throw new RegleMetierException(
                    "RG-03",
                    "RG-03 : le ticket " + id + " est deja FERME et ne peut plus changer d'etat."
            );
        }
        ticket.setStatut(Ticket.Statut.FERME);
        return TicketOutputDto.from(ticketRepository.save(ticket));
    }
}
