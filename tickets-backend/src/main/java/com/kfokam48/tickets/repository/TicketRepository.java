package com.kfokam48.tickets.repository;

import com.kfokam48.tickets.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByTitreIgnoreCase(String titre);

    Page<Ticket> findByStatut(Ticket.Statut statut, Pageable pageable);

    long countByStatut(Ticket.Statut statut);
}
