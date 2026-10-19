package com.kfokam48.tickets.service;

import com.kfokam48.tickets.dto.TicketInputDto;
import com.kfokam48.tickets.dto.TicketListeDto;
import com.kfokam48.tickets.dto.TicketOutputDto;
import com.kfokam48.tickets.entity.Ticket;
import com.kfokam48.tickets.exception.RegleMetierException;
import com.kfokam48.tickets.exception.RessourceIntrouvableException;
import com.kfokam48.tickets.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de la couche metier sur la base H2 de test (voir
 * src/test/resources/application.properties) : independants du
 * conteneur PostgreSQL et de tout serveur HTTP.
 */
@SpringBootTest
class TicketServiceTest {

    @Autowired
    private TicketService service;

    @Autowired
    private TicketRepository repository;

    private int sequence = 0;

    @AfterEach
    void purger() {
        repository.deleteAll();
    }

    // --- Construction d'un titre unique par test pour ISOLER les donnees ---

    private TicketInputDto entree(String base) {
        return new TicketInputDto(base + " #" + ++sequence, "description de test");
    }

    private TicketOutputDto creer(String base) {
        return service.creer(entree(base));
    }

    /** Insere un ticket avec une date de creation imposee (champ sans setter volontairement). */
    private Ticket semer(String titre, Ticket.Statut statut, OffsetDateTime date) {
        Ticket ticket = new Ticket();
        ticket.setTitre(titre);
        ticket.setDescription(null);
        ticket.setStatut(statut);
        ReflectionTestUtils.setField(ticket, "dateCreation", date);
        return repository.save(ticket);
    }

    // ------------------------------------------------------------------
    // Creation (RG-01 / RG-02)
    // ------------------------------------------------------------------

    @Test
    void creer_un_ticket_le_met_ouvert_avec_une_date_de_creation_serveur() {
        TicketOutputDto cree = service.creer(new TicketInputDto("Impression bloquee", "HP LaserJet"));

        assertThat(cree.id()).isNotNull();
        assertThat(cree.statut()).isEqualTo(Ticket.Statut.OUVERT);
        assertThat(cree.titre()).isEqualTo("Impression bloquee");
        assertThat(cree.description()).isEqualTo("HP LaserJet");
        assertThat(cree.dateCreation()).isNotNull();
        assertThat(cree.dateCreation().truncatedTo(ChronoUnit.DAYS))
                .isEqualTo(OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
    }

    @Test
    void creer_avec_un_titre_deja_pris_rejette_RG02_meme_en_casse_different() {
        service.creer(new TicketInputDto("Formation CRM", null));

        assertThatThrownBy(() -> service.creer(new TicketInputDto("formation crm", null)))
                .isInstanceOf(RegleMetierException.class)
                .satisfies(e -> assertThat(((RegleMetierException) e).getReference()).isEqualTo("RG-02"));
    }

    // ------------------------------------------------------------------
    // Changements de statut (RG-03 / RG-05)
    // ------------------------------------------------------------------

    @Test
    void demarrer_un_ticket_ouvert_le_passe_en_cours() {
        Long id = creer("Imprimante vague").id();

        TicketOutputDto demarre = service.demarrer(id);

        assertThat(demarre.statut()).isEqualTo(Ticket.Statut.EN_COURS);
    }

    @Test
    void demarrer_un_ticket_deja_en_cours_viole_RG05() {
        Long id = creer("Imprimante vague").id();
        service.demarrer(id);

        assertThatThrownBy(() -> service.demarrer(id))
                .isInstanceOf(RegleMetierException.class)
                .satisfies(e -> assertThat(((RegleMetierException) e).getReference()).isEqualTo("RG-05"));
    }

    @Test
    void demarrer_un_ticket_ferme_viole_RG05() {
        Long id = creer("Imprimante vague").id();
        service.fermer(id);

        assertThatThrownBy(() -> service.demarrer(id))
                .isInstanceOf(RegleMetierException.class)
                .satisfies(e -> assertThat(((RegleMetierException) e).getReference()).isEqualTo("RG-05"));
    }

    @Test
    void fermer_un_ticket_ouvert_le_passe_en_ferme() {
        Long id = creer("Panne reseau").id();

        TicketOutputDto ferme = service.fermer(id);

        assertThat(ferme.statut()).isEqualTo(Ticket.Statut.FERME);
    }

    @Test
    void fermer_un_ticket_en_cours_est_autorise() {
        Long id = creer("Panne reseau").id();
        service.demarrer(id);

        assertThat(service.fermer(id).statut()).isEqualTo(Ticket.Statut.FERME);
    }

    @Test
    void fermer_un_ticket_deja_ferme_viole_RG03() {
        Long id = creer("Panne reseau").id();
        service.fermer(id);

        assertThatThrownBy(() -> service.fermer(id))
                .isInstanceOf(RegleMetierException.class)
                .satisfies(e -> assertThat(((RegleMetierException) e).getReference()).isEqualTo("RG-03"));
    }

    @Test
    void demarrer_et_fermer_un_id_inconnu_levent_ressource_introuvable() {
        assertThatThrownBy(() -> service.demarrer(9_999_999L))
                .isInstanceOf(RessourceIntrouvableException.class);
        assertThatThrownBy(() -> service.fermer(9_999_999L))
                .isInstanceOf(RessourceIntrouvableException.class);
        assertThatThrownBy(() -> service.supprimer(9_999_999L))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    // ------------------------------------------------------------------
    // Suppression (RG-04)
    // ------------------------------------------------------------------

    @Test
    void supprimer_un_ticket_ferme_reussit_definitivement() {
        Long id = creer("Ecran casse").id();
        service.fermer(id);

        service.supprimer(id);

        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void supprimer_un_ticket_ouvert_ou_en_cours_viole_RG04() {
        Long ouvert = creer("Ecran casse").id();

        assertThatThrownBy(() -> service.supprimer(ouvert))
                .isInstanceOf(RegleMetierException.class)
                .satisfies(e -> assertThat(((RegleMetierException) e).getReference()).isEqualTo("RG-04"));

        // et un EN_COURS ne peut pas etre supprime non plus
        Long enCours = creer("Ecran casse").id();
        service.demarrer(enCours);
        assertThatThrownBy(() -> service.supprimer(enCours))
                .isInstanceOf(RegleMetierException.class)
                .satisfies(e -> assertThat(((RegleMetierException) e).getReference()).isEqualTo("RG-04"));
    }

    // ------------------------------------------------------------------
    // Liste : pagination, tri, filtre, compteur
    // ------------------------------------------------------------------

    @Test
    void lister_pagine_et_compte_les_totalElements_totalPages_et_tickets_ouverts_globalement() {
        // 7 : 4 OUVERT + 2 FERME + 1 EN_COURS -> compteur = 4, filtre ou non
        for (int i = 1; i <= 4; i++) {
            semer("Ticket ouvert " + i, Ticket.Statut.OUVERT, OffsetDateTime.now().minusMinutes(i));
        }
        semer("Ticket clos 1", Ticket.Statut.FERME, OffsetDateTime.now().minusHours(2));
        semer("Ticket clos 2", Ticket.Statut.FERME, OffsetDateTime.now().minusHours(3));
        semer("Ticket advance", Ticket.Statut.EN_COURS, OffsetDateTime.now().minusHours(4));

        TicketListeDto page0 = service.lister(null, 0, 3, "desc");

        assertThat(page0.totalElements()).isEqualTo(7);
        assertThat(page0.totalPages()).isEqualTo(3);
        assertThat(page0.page()).isZero();
        assertThat(page0.taille()).isEqualTo(3);
        assertThat(page0.contenu()).hasSize(3);
        assertThat(page0.ticketsOuverts()).isEqualTo(4);

        TicketListeDto page2 = service.lister(null, 2, 3, "desc");
        assertThat(page2.contenu()).hasSize(1);
        assertThat(page2.totalPages()).isEqualTo(3);

        // marge basse : page trop grande -> contenu vide, metadata coherentes
        TicketListeDto horsPage = service.lister(null, 9, 3, "desc");
        assertThat(horsPage.contenu()).isEmpty();
        assertThat(horsPage.totalElements()).isEqualTo(7);
    }

    @Test
    void lister_filtre_par_statut_et_le_compteur_reste_global() {
        semer("O1", Ticket.Statut.OUVERT, OffsetDateTime.now().minusMinutes(5));
        semer("O2", Ticket.Statut.OUVERT, OffsetDateTime.now().minusMinutes(4));
        semer("C1", Ticket.Statut.FERME, OffsetDateTime.now().minusMinutes(3));
        semer("E1", Ticket.Statut.EN_COURS, OffsetDateTime.now().minusMinutes(2));

        TicketListeDto filtre = service.lister(Ticket.Statut.FERME, 0, 5, "desc");

        assertThat(filtre.totalElements()).isEqualTo(1);
        assertThat(filtre.contenu().get(0).titre()).isEqualTo("C1");
        // le compteur de tickets ouverts porte sur TOUS les tickets, filtre ou non
        assertThat(filtre.ticketsOuverts()).isEqualTo(2);
    }

    @Test
    void lister_tri_asc_et_desc_par_date_de_creation() {
        semer("T-ancien", Ticket.Statut.OUVERT, OffsetDateTime.now().minusHours(3));
        semer("T-median", Ticket.Statut.OUVERT, OffsetDateTime.now().minusHours(2));
        semer("T-recent", Ticket.Statut.OUVERT, OffsetDateTime.now().minusHours(1));

        // desc (defaut) : plus recents d'abord
        TicketListeDto desc = service.lister(null, 0, 5, "desc");
        assertThat(desc.contenu().stream().map(TicketOutputDto::titre))
                .containsExactly("T-recent", "T-median", "T-ancien");
        assertThat(desc.tri()).isEqualTo("desc");

        // asc : plus anciens d'abord
        TicketListeDto asc = service.lister(null, 0, 5, "asc");
        assertThat(asc.contenu().stream().map(TicketOutputDto::titre))
                .containsExactly("T-ancien", "T-median", "T-recent");
        assertThat(asc.tri()).isEqualTo("asc");
    }

    @Test
    void lister_avec_un_tri_inconnu_retombe_sur_desc_et_une_page_negative_vaut_0() {
        semer("Unique-test", Ticket.Statut.OUVERT, OffsetDateTime.now());

        TicketListeDto triInconnu = service.lister(null, 0, 5, "bidule");
        assertThat(triInconnu.tri()).isEqualTo("desc");

        TicketListeDto pageNegative = service.lister(null, -2, 5, "desc");
        assertThat(pageNegative.page()).isEqualTo(0);
        assertThat(pageNegative.contenu()).hasSize(1);
    }
}
