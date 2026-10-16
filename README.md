# Rattrapage — Module Ticket (KFOKAM48 — Batch 2 — Phase 3)

Suivi des demandes des utilisateurs d'un service de support : création, liste (filtrable par statut) et fermeture de tickets.

## Stack

- **Base de données** : PostgreSQL 16 dans un conteneur Docker (`tickets-db`, port hôte **5433**)
- **Backend** : Spring Boot 4 (Java 21, Maven) — Web, JPA, Validation, springdoc-openapi (Swagger UI)
- **Frontend** : React 19 + Vite

## Démarrage

```bash
# 1. PostgreSQL dans Docker (port hôte 5433, les autres bases locales ne sont pas touchées)
docker run -d --name tickets-db \
  -e POSTGRES_DB=ticketsdb -e POSTGRES_USER=tickets -e POSTGRES_PASSWORD=tickets123 \
  -p 5433:5432 postgres:16-alpine

# 2. Backend — http://localhost:8080 — Swagger : http://localhost:8080/swagger-ui.html
cd tickets-backend
./mvnw spring-boot:run

# 3. Frontend — http://localhost:5173
cd tickets-frontend
npm install
npm run dev
```

## API

| Verbe  | Chemin                      | Rôle             | Succès | Erreurs      |
|--------|-----------------------------|------------------|--------|--------------|
| POST   | `/api/tickets`              | Créer un ticket  | 201    | 400, 409     |
| GET    | `/api/tickets?statut=...`   | Lister (filtre)  | 200    | —            |
| PATCH  | `/api/tickets/{id}/fermer`  | Fermer un ticket | 200    | 404, 409     |

Le client n'envoie que `titre` et `description`. `statut` (OUVERT à la création) et `dateCreation` sont déterminés par le serveur.

### Règles de gestion

| Réf.  | Règle                                                                                                    |
|-------|----------------------------------------------------------------------------------------------------------|
| RG-01 | Titre obligatoire, 100 caractères max → 400 avec le message exact                                          |
| RG-02 | Titre unique (insensible à la casse) → 409, la réponse nomme la règle                                      |
| RG-03 | Fermeture interdite sur un ticket FERME → 409, la réponse nomme la règle ; identifiant inconnu → 404        |

### Exemples

```bash
# Création
curl -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"titre":"Serveur de prod HS","description":"Impossible de joindre le serveur"}'

# Refus RG-02 -> 409 avec la référence de la règle dans le corps
# {"regle":"RG-02","message":"RG-02 : un ticket avec le titre ... existe deja."}

# Refus RG-03 -> 409  |  id inconnu -> 404
curl -X PATCH http://localhost:8080/api/tickets/1/fermer
curl -X PATCH http://localhost:8080/api/tickets/999/fermer
```

## Architecture

**Backend** (`com.kfokam48.tickets`) : `entity` / `repository` / `service` (toute la logique métier) / `controller` (léger, sans logique métier) / `dto` (`TicketInputDto`, `TicketOutputDto` — l'entité ne sort jamais du service) / `exception` (`GlobalExceptionHandler` : 400, 404, 409).

**Frontend** (`tickets-frontend/src`) : `services/ticketApi.js` (seul endroit où `fetch` est appelé), `pages/TicketsPage.jsx` (conteneur : état et appels), `components/TicketList.jsx`, `components/TicketForm.jsx`, `components/BadgeStatut.jsx`. Interface en français, quatre états gérés : chargement, données, liste vide (« Aucun ticket »), erreur avec bouton « Réessayer ».

## Retrouver un ticket en base (partie A.4)

```bash
docker exec -it tickets-db psql -U tickets -d ticketsdb \
  -c "SELECT id, titre, statut, date_creation FROM tickets ORDER BY id;"
```

## Épreuve D — Git / Pull Request

1. Renommer la branche avec votre prenom-nom :
   ```bash
   git branch -m rattrapage/prenom-nom rattrapage/votre-prenom-nom
   ```
2. Ajouter le dépôt distant puis pousser :
   ```bash
   git remote add origin <URL_DU_DEPOT>
   git push -u origin rattrapage/votre-prenom-nom
   ```
3. Ouvrir la Pull Request vers `main` avec :
   - une capture de la page Swagger (trois endpoints visibles) ;
   - une capture d'un refus 409 affiché dans l'écran (ex. second clic sur « Fermer » après rechargement de la liste).

## Captures à préparer pour la PR

- **Swagger** : `http://localhost:8080/swagger-ui.html` — vérifier que les trois endpoints et leurs codes (201/400/409, 200, 200/404/409) apparaissent.
- **409 dans l'écran** : créer un ticket, le fermer, puis tenter de fermer un autre ticket déjà FERME : le message `RG-03 : le ticket X est deja FERME...` doit s'afficher en rouge sous/au-dessus du tableau.
