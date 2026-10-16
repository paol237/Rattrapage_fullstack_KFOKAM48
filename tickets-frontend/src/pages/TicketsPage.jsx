import { useCallback, useEffect, useState } from 'react';
import { creerTicket, listerTickets, fermerTicket, ErreurApi } from '../services/ticketApi.js';
import TicketForm from '../components/TicketForm.jsx';
import TicketList from '../components/TicketList.jsx';
import { STATUTS } from '../components/BadgeStatut.jsx';

export default function TicketsPage() {
  const [tickets, setTickets] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreurChargement, setErreurChargement] = useState(null);
  const [statutFiltre, setStatutFiltre] = useState('');
  const [erreurFormulaire, setErreurFormulaire] = useState(null);
  const [creationEnCours, setCreationEnCours] = useState(false);
  const [ticketEnTraitement, setTicketEnTraitement] = useState(null);
  const [erreurFermeture, setErreurFermeture] = useState(null);

  const charger = useCallback(async (statut = statutFiltre) => {
    setChargement(true);
    setErreurChargement(null);
    try {
      const liste = await listerTickets(statut || undefined);
      setTickets(liste);
    } catch {
      setErreurChargement('Impossible de joindre le serveur. Vérifiez que le backend est démarré puis réessayez.');
    } finally {
      setChargement(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    charger();
  }, [charger]);

  async function changerFiltre(statut) {
    setStatutFiltre(statut);
    await charger(statut);
  }

  async function gererCreation(entree) {
    setCreationEnCours(true);
    setErreurFormulaire(null);
    try {
      await creerTicket(entree);
      await changerFiltre(statutFiltre);
      return true;
    } catch (erreur) {
      setErreurFormulaire(erreur instanceof ErreurApi ? erreur.message : 'La création a échoué.');
      return false;
    } finally {
      setCreationEnCours(false);
    }
  }

  async function gererFermeture(ticket) {
    const confirme = window.confirm(
      `Voulez-vous vraiment fermer le ticket « ${ticket.titre} » ? Cette action est définitive.`
    );
    if (!confirme) return;
    setTicketEnTraitement(ticket.id);
    setErreurFermeture(null);
    try {
      await fermerTicket(ticket.id);
      await charger(statutFiltre);
    } catch (erreur) {
      setErreurFermeture(
        `Ticket « ${ticket.titre} » : ${erreur instanceof ErreurApi ? erreur.message : 'la fermeture a échoué.'}`
      );
    } finally {
      setTicketEnTraitement(null);
    }
  }

  return (
    <main className="page">
      <h1>Gestion des tickets</h1>

      <div className="filtres" role="group" aria-label="Filtre par statut">
        <button
          type="button"
          className={'puce' + (statutFiltre === '' ? ' puce-active' : '')}
          onClick={() => changerFiltre('')}
        >
          Tous
        </button>
        {STATUTS.map((statut) => (
          <button
            key={statut}
            type="button"
            className={'puce' + (statutFiltre === statut ? ' puce-active' : '')}
            onClick={() => changerFiltre(statut)}
          >
            {statut}
          </button>
        ))}
      </div>

      {chargement && (
        <div className="etat etat-chargement" aria-live="polite">
          <span className="indicateur" aria-hidden="true" />
          Chargement des tickets…
        </div>
      )}

      {!chargement && erreurChargement && (
        <div className="etat etat-erreur" role="alert">
          <p>{erreurChargement}</p>
          <button type="button" className="bouton" onClick={() => charger(statutFiltre)}>
            Réessayer
          </button>
        </div>
      )}

      {!chargement && !erreurChargement && (
        <>
          {erreurFermeture && <p className="message-erreur" role="alert">{erreurFermeture}</p>}
          <section className="card">
            <TicketList
              tickets={tickets}
              surFermer={gererFermeture}
              ticketEnTraitement={ticketEnTraitement}
            />
          </section>
        </>
      )}

      <TicketForm
        onSubmit={gererCreation}
        enCours={creationEnCours}
        erreur={erreurFormulaire}
      />
    </main>
  );
}
