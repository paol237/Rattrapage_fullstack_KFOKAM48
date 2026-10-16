import { useState } from 'react';
import Bouton from './Bouton.jsx';

/**
 * Formulaire de creation d'un ticket.
 * Deux champs : titre (obligatoire) et description (facultative).
 * Pas de limite de longueur cote client : le refus de plus de 100 caracteres
 * doit venir du serveur (RG-01), affiche a cote du formulaire.
 */
export default function TicketForm({ onSubmit, enCours, erreur }) {
  const [titre, setTitre] = useState('');
  const [description, setDescription] = useState('');

  const soumissionValide = titre.trim().length > 0 && !enCours;

  // Le conteneur gere ses propres erreurs : onSubmit ne rejette jamais,
  // les champs ne sont nettoyes qu'en cas de succes.
  async function gererSoumission(evenement) {
    evenement.preventDefault();
    if (!soumissionValide) return;
    const succes = await onSubmit({ titre: titre.trim(), description: description.trim() });
    if (succes) {
      setTitre('');
      setDescription('');
    }
  }

  return (
    <section className="card" id="formulaire-nouveau-ticket">
      <h2>Nouveau ticket</h2>
      <form className="formulaire" onSubmit={gererSoumission}>
        <label htmlFor="champ-titre">
          Titre <span className="obligatoire">*</span>
          <input
            id="champ-titre"
            type="text"
            value={titre}
            onChange={(e) => setTitre(e.target.value)}
            placeholder="Ex. : Serveur de production inaccessible"
          />
        </label>
        <label>
          Description (facultative)
          <textarea
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Detail de la demande…"
          />
        </label>
        <Bouton
          type="submit"
          disabled={!soumissionValide}
          chargement={enCours}
          title={titre.trim().length === 0 ? 'Le titre est obligatoire' : undefined}
        >
          {enCours ? 'Enregistrement…' : 'Créer le ticket'}
        </Bouton>
        {erreur && <p className="message-erreur formulaire-erreur" role="alert">{erreur}</p>}
      </form>
    </section>
  );
}
