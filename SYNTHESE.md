# Synthèse de l'épreuve — Module Ticket (KFOKAM48 — Batch 2 — Phase 3)

Rattrapage des séances 1, 2 et 3 — Full Stack. Cette synthèse récapitule tout ce qui a été livré et vérifié.
La documentation d'utilisation détaillée se trouve dans le `README.md` principal.

---

## A. Environnement (tourne ✔)

| Élément | Détail |
|---|---|
| PostgreSQL | Conteneur Docker `tickets-db` (image `postgres:16-alpine`), port hôte **5433** car le 5432 de la machine est occupé par un PostgreSQL natif |
| Identifiants base | base `ticketsdb`, utilisateur `tickets`, mot de passe `tickets123` |
| Backend | Spring Boot 4 (Java 21, Maven) démarré sur **http://localhost:8080** — logs sans erreur |
| Frontend | React 19 + Vite démarré sur **http://localhost:5173** |
| Swagger UI | **http://localhost:8080/swagger-ui.html** |

## B. Backend (`tickets-backend/`)

### Architecture en couches

```
com.kfokam48.tickets
├── entity/Ticket.java              # id, titre (≤100, unique), description, statut, dateCreation
├── repository/TicketRepository.java
├── service/TicketService.java      # TOUTE la logique métier (RG-01 → RG-03)
├── controller/TicketController.java# aminci : aucune logique métier
├── dto/TicketInputDto.java         # le client n'envoie que titre + description
├── dto/TicketOutputDto.java        # l'entité ne sort jamais du service
└── exception/                      # GlobalExceptionHandler → 400 / 404 / 409
```

### Endpoints

| Verbe | Chemin | Succès | Erreurs |
|---|---|---|---|
| POST | `/api/tickets` | 201 | 400 (RG-01), 409 (RG-02) |
| GET | `/api/tickets?statut=OUVERT\|EN_COURS\|FERME` | 200 | — |
| PATCH | `/api/tickets/{id}/fermer` | 200 | 404 (id inconnu), 409 (RG-03) |

### Endpoints vérifiés par tests réels (curl)

| Test | Résultat |
|---|---|
| Création valide | **201** avec le JSON complet (id, statut OUVERT, dateCreation serveur) |
| Titre vide / espaces | **400** `RG-01 — Le titre est obligatoire.` |
| Titre de 110 caractères | **400** `RG-01 — Le titre ne doit pas depasser 100 caracteres.` |
| Titre déjà utilisé | **409** `RG-02 : un ticket avec le titre "..." existe deja.` |
| Fermeture d'un ticket OUVERT | **200**, statut passe à FERME |
| Deuxième fermeture | **409** `RG-03 : le ticket 1 est deja FERME et ne peut plus changer d'etat.` |
| Fermeture id 999 | **404** `Le ticket 999 n'existe pas.` |
| Filtre `?statut=OUVERT` | **200**, liste filtrée |

## C. Frontend (`tickets-frontend/`)

- **Service API dédié** : `src/services/ticketApi.js` — seul endroit du code où `fetch` est appelé ; il extrait le message du serveur des réponses 400/409.
- **Composants découpés** :
  - `pages/TicketsPage.jsx` — le conteneur : état global et appels API uniquement ;
  - `components/TicketList.jsx` — le tableau : titre, statut (badge coloré), date de création, action ;
  - `components/TicketForm.jsx` — deux champs (titre, description facultative), bouton inactif tant que le titre est vide, **aucune limite de longueur côté client** ;
  - `components/BadgeStatut.jsx` — badge de couleur par statut (bonus).
- **Les quatre états** :
  1. *Chargement* : spinner animé visible pendant l'appel ;
  2. *Données* : tableau rempli ;
  3. *Liste vide* : message explicite « Aucun ticket » ;
  4. *Erreur* : « Impossible de joindre le serveur… » + bouton **Réessayer** (testable en arrêtant le backend puis en rechargeant).
- **Traitement des refus** : les messages 400 et 409 **du serveur** s'affichent en rouge, à côté du formulaire. Aucun `alert()`, aucun message générique.
- **Fermeture** : bouton par ligne, visible uniquement si OUVERT ou EN_COURS, `window.confirm` avant l'appel, mise à jour de la liste après succès, message du serveur en cas de 409.
- Interface **100 % en français**, aucune donnée codée en dur : tout vient de l'API.

## D. Retrouver les tickets en base (partie A.4)

```bash
docker exec -it tickets-db psql -U tickets -d ticketsdb \
  -c "SELECT id, titre, statut, date_creation FROM tickets ORDER BY id;"
```

Résultat lors de la vérification : **2 tickets** en base, dont 1 créé via l'API (Swagger/curl), le second inséré en EN_COURS pour la démo du formateur.

## E. Git / Pull Request (branche poussée ✔)

- **Dépôt distant** : `git@github.com:paol237/Rattrapage_fullstack_KFOKAM48.git`
- **Branche** : `rattrapage/paol-djoumbouossie` (upstream configuré, poussée)
- **3 commits explicites** :
  1. `Initialise le projet avec la documentation de l'epreuve ticket`
  2. `Ajoute le backend Ticket : API, regles de gestion et Swagger`
  3. `Ajoute l'ecran de gestion des tickets en React`

**Titre PR suggéré** : `Ajoute le module Ticket : API Spring Boot + écran React`

**Description PR à copier** :

```markdown
## Contenu
- Backend Spring Boot : entité/repository/service/contrôleur, DTO en entrée et sortie,
  validation, gestion d'erreurs 400/404/409, Swagger
- Frontend React : service API dédié, liste filtrable, formulaire, fermeture confirmée,
  4 états (chargement, données, vide, erreur)

## Captures d'écran
1. Page Swagger montrant les 3 endpoints avec leurs codes → http://localhost:8080/swagger-ui.html
2. Refus 409 affiché dans l'écran (tentative de fermeture d'un ticket déjà FERME)

## Test manuel
- POST sans titre / titre > 100 car. → 400
- POST titre dupliqué → 409 (RG-02)
- PATCH sur ticket FERME → 409 (RG-03) ; id inconnu → 404
- Arrêter le backend et recharger l'écran → état d'erreur + bouton Réessayer
```

## F. Démarrage rapide (pour le jour J)

```bash
# 1. Base (si le conteneur est arrêté)
docker start tickets-db

# 2. Backend
cd tickets-backend && ./mvnw spring-boot:run

# 3. Frontend
cd tickets-frontend && npm run dev
```

⚠️ **Avant le passage devant le formateur** : backend démarré, base contenant au moins un ticket, et les **2 captures d'écran** exigées (Swagger + refus 409 dans l'écran) glissées dans la description de la PR.
