package com.kfokam48.tickets.repository;

import com.kfokam48.tickets.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByTitreIgnoreCase(String titre);

    List<Ticket> findByStatutOrderByIdAsc(Ticket.Statut statut);

    List<Ticket> findAllByOrderByIdAsc();
}
