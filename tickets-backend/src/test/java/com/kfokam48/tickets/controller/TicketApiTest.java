package com.kfokam48.tickets.controller;

import com.kfokam48.tickets.repository.TicketRepository;
import com.kfokam48.tickets.service.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// Spring Boot 4 : @AutoConfigureMockMvc demenagee dans le module webmvc-test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat HTTP de l'API (MockMvc, base H2 de test, sans serveur de conteneur) :
 * chaque regle de gestion se traduit par le bon code et un corps JSON qui nomme
 * la regle (RG-01 a RG-05), comme documenté dans le README.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TicketService service;

    @Autowired
    private TicketRepository repository;

    @AfterEach
    void purger() {
        repository.deleteAll();
    }

    private String corps(String titre) {
        return "{\"titre\":\"" + titre + "\",\"description\":\"desc test\"}";
    }

    // ------------------------------------------------------------------
    // Creation : 201 (Location + statut OUVERT), 400 RG-01, 409 RG-02
    // ------------------------------------------------------------------

    @Test
    void post_cree_un_ticket_avec_201_location_et_statut_ouvert() throws Exception {
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Clavier depanne")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/tickets/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titre").value("Clavier depanne"))
                .andExpect(jsonPath("$.statut").value("OUVERT"))
                .andExpect(jsonPath("$.dateCreation").isNotEmpty());
    }

    @Test
    void post_sans_titre_renvoie_400_RG01_avec_le_message_de_validation() throws Exception {
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"sans titre\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.regle").value("RG-01"))
                .andExpect(jsonPath("$.message").value("Le titre est obligatoire."));
    }

    @Test
    void post_avec_un_titre_uniquement_blanc_renvoie_400_RG01() throws Exception {
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.regle").value("RG-01"));
    }

    @Test
    void post_avec_un_titre_de_plus_de_100_caracteres_renvoie_400_RG01() throws Exception {
        String tropLong = "x".repeat(101);
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(tropLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.regle").value("RG-01"))
                .andExpect(jsonPath("$.message").value("Le titre ne doit pas depasser 100 caracteres."));
    }

    @Test
    void post_avec_titre_duplicat_renvoie_409_RG02_meme_si_la_casse_differ() throws Exception {
        mvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps("Ticket duplique")));

        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("ticket duplique")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-02"))
                .andExpect(jsonPath("$.message").value(
                        "RG-02 : un ticket avec le titre \"ticket duplique\" existe deja."));
    }

    // ------------------------------------------------------------------
    // Liste : pagination, filtre, tri, compteur (200)
    // ------------------------------------------------------------------

    @Test
    void get_liste_renvoie_la_page_avec_les_metadata_de_pagination() throws Exception {
        for (int i = 1; i <= 7; i++) {
            mvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corps("Ticket pagination " + i)));
        }

        mvc.perform(get("/api/tickets").param("page", "0").param("taille", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.taille").value(3))
                .andExpect(jsonPath("$.tri").value("desc"));
    }

    @Test
    void get_liste_avec_un_filtre_statut_et_un_tri_asc_sont_honores() throws Exception {
        // donnees posees via le service pour maîtriser exactement les statuts voulus
        final long a = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("F OUVERT A", null)).id();
        long b = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("F OUVERT B", null)).id();
        long f = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("F FERME A", null)).id();
        service.fermer(f);
        long d = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("F EN_COURS A", null)).id();
        service.demarrer(d);

        mvc.perform(get("/api/tickets").param("statut", "OUVERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.ticketsOuverts").value(2))
                .andExpect(jsonPath("$.contenu", hasSize(2)));

        mvc.perform(get("/api/tickets").param("statut", "FERME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenu[0].statut").value("FERME"));

        // tri asc : plus anciens d'abord (creation sequentielle -> dates croissantes)
        mvc.perform(get("/api/tickets").param("tri", "asc").param("taille", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].titre").value("F OUVERT A"))
                .andExpect(jsonPath("$.tri").value("asc"));
    }

    @Test
    void get_liste_avec_un_statut_inconnu_renvoie_400() throws Exception {
        mvc.perform(get("/api/tickets").param("statut", "BIDULE"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Démarrage (RG-05) : 200, 409, 404
    // ------------------------------------------------------------------

    @Test
    void patch_demarrer_sur_ouvert_renvoie_200_en_cours() throws Exception {
        long id = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("Dem article", null)).id();

        mvc.perform(patch("/api/tickets/" + id + "/demarrer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void patch_demarrer_sur_ticket_deja_en_cours_renvoie_409_RG05() throws Exception {
        long id = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("Dem cascade", null)).id();
        service.demarrer(id);

        mvc.perform(patch("/api/tickets/" + id + "/demarrer"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-05"));
    }

    @Test
    void patch_demarrer_sur_ticket_ferme_renvoie_409_RG05() throws Exception {
        long id = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("Dem ferme", null)).id();
        service.fermer(id);

        mvc.perform(patch("/api/tickets/" + id + "/demarrer"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-05"));
    }

    @Test
    void patch_demarrer_sur_id_inconnu_renvoie_404() throws Exception {
        mvc.perform(patch("/api/tickets/999999/demarrer"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statut").value(404))
                .andExpect(jsonPath("$.message").value("Le ticket 999999 n'existe pas."));
    }

    // ------------------------------------------------------------------
    // Fermeture (RG-03) : 200, 409, 404
    // ------------------------------------------------------------------

    @Test
    void patch_fermer_sur_ouvert_renvoie_200_ferme() throws Exception {
        long id = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("Fermer un ticket ok", null)).id();

        mvc.perform(patch("/api/tickets/" + id + "/fermer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("FERME"));
    }

    @Test
    void patch_fermer_sur_deja_ferme_renvoie_409_RG03() throws Exception {
        long id = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("Ferme cascade", null)).id();
        service.fermer(id);

        mvc.perform(patch("/api/tickets/" + id + "/fermer"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-03"))
                .andExpect(jsonPath("$.message").value(
                        "RG-03 : le ticket " + id + " est deja FERME et ne peut plus changer d'etat."));
    }

    @Test
    void patch_fermer_sur_id_inconnu_renvoie_404() throws Exception {
        mvc.perform(patch("/api/tickets/999999/fermer"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Suppression (RG-04) : 204, 409, 404
    // ------------------------------------------------------------------

    @Test
    void delete_dun_ticket_ferme_renvoie_204_puis_le_ticket_disparait() throws Exception {
        long id = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("Delposable", null)).id();
        service.fermer(id);

        mvc.perform(delete("/api/tickets/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/tickets").param("statut", "FERME"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void delete_dun_ticket_non_ferme_renvoie_409_RG04() throws Exception {
        long ouvert = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("Del refus", null)).id();

        mvc.perform(delete("/api/tickets/" + ouvert))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-04"));

        // et un ticket EN_COURS non plus
        long enCours = service.creer(new com.kfokam48.tickets.dto.TicketInputDto("Del refus 2", null)).id();
        service.demarrer(enCours);
        mvc.perform(delete("/api/tickets/" + enCours))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.regle").value("RG-04"));
    }

    @Test
    void delete_dun_id_inconnu_renvoie_404() throws Exception {
        mvc.perform(delete("/api/tickets/999999"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Structure du corps d'erreur (cite les regles, uniforme)
    // ------------------------------------------------------------------

    @Test
    void le_corps_d_erreur_metier_contient_horodatage_statut_erreur_regle_et_message() throws Exception {
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Erreur corps")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Erreur corps")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.horodatage").isNotEmpty())
                .andExpect(jsonPath("$.statut").value(409))
                .andExpect(jsonPath("$.erreur").value(is("Conflict")))
                .andExpect(jsonPath("$.regle").value("RG-02"));

        // le filtre par statut inconnu est refuse avant d'atteindre le service
        mvc.perform(get("/api/tickets").param("statut", "BIDULE"))
                .andExpect(status().isBadRequest());
    }
}
