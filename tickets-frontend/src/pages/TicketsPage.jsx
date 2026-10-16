import { useCallback, useEffect, useState } from 'react';
import { creerTicket, listerTickets, demarrerTicket, fermerTicket, supprimerTicket, ErreurApi } from '../services/ticketApi.js';
import TicketForm from '../components/TicketForm.jsx';
import TicketList from '../components/TicketList.jsx';
import FloatingActions from '../components/FloatingActions.jsx';
import Bouton from '../components/Bouton.jsx';
import ControleTheme from '../components/ControleTheme.jsx';
import { STATUTS } from '../components/BadgeStatut.jsx';

const TAILLE_PAGE = 5;

export default function TicketsPage() {
  const [reponseListe, setReponseListe] = useState(null);
  const [chargement, setChargement] = useState(true);
  const [erreurChargement, setErreurChargement] = useState(null);
  const [statutFiltre, setStatutFiltre] = useState('');
  const [pageCourante, setPageCourante] = useState(0);
  const [tri, setTri] = useState('desc');
  const [erreurFormulaire, setErreurFormulaire] = useState(null);
  const [creationEnCours, setCreationEnCours] = useState(false);
  const [ticketEnTraitement, setTicketEnTraitement] = useState(null);
  const [erreurAction, setErreurAction] = useState(null);
  const [erreurSuppression, setErreurSuppression] = useState(null);

  const charger = useCallback(async ({ statut = statutFiltre, page = pageCourante, triCourant = tri } = {}) => {
    setChargement(true);
    setErreurChargement(null);
    try {
      const reponse = await listerTickets({ statut: statut || undefined, page, taille: TAILLE_PAGE, tri: triCourant });
      setReponseListe(reponse);
      setPageCourante(reponse.page);
    } catch {
      setErreurChargement('Impossible de joindre le serveur. Vérifiez que le backend est démarré puis réessayez.');
    } finally {
      setChargement(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    charger();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function changerFiltre(statut) {
    setStatutFiltre(statut);
    setPageCourante(0);
    await charger({ statut, page: 0 });
  }

  async function changerTri(nouveauTri) {
    setTri(nouveauTri);
    await charger({ triCourant: nouveauTri });
  }

  async function changerPage(nouvellePage) {
    await charger({ page: nouvellePage });
  }

  async function gererCreation(entree) {
    setCreationEnCours(true);
    setErreurFormulaire(null);
    try {
      await creerTicket(entree);
      await charger({ page: 0 });
      setPageCourante(0);
      return true;
    } catch (erreur) {
      setErreurFormulaire(erreur instanceof ErreurApi ? erreur.message : 'La création a échoué.');
      return false;
    } finally {
      setCreationEnCours(false);
    }
  }

  async function gererDemarrer(ticket) {
    const confirme = window.confirm(
      `Passer le ticket « ${ticket.titre} » en EN_COURS ?`
    );
    if (!confirme) return;
    setTicketEnTraitement(ticket.id);
    setErreurAction(null);
    try {
      await demarrerTicket(ticket.id);
      await charger();
    } catch (erreur) {
      setErreurAction(
        `Ticket « ${ticket.titre} » : ${erreur instanceof ErreurApi ? erreur.message : 'le démarrage a échoué.'}`
      );
    } finally {
      setTicketEnTraitement(null);
    }
  }

  async function gererFermeture(ticket) {
    const confirme = window.confirm(
      `Voulez-vous vraiment fermer le ticket « ${ticket.titre} » ? Cette action est définitive.`
    );
    if (!confirme) return;
    setTicketEnTraitement(ticket.id);
    setErreurAction(null);
    try {
      await fermerTicket(ticket.id);
      await charger();
    } catch (erreur) {
      setErreurAction(
        `Ticket « ${ticket.titre} » : ${erreur instanceof ErreurApi ? erreur.message : 'la fermeture a échoué.'}`
      );
    } finally {
      setTicketEnTraitement(null);
    }
  }

  async function gererSuppression(ticket) {
    const confirme = window.confirm(
      `Voulez-vous vraiment supprimer définitivement le ticket « ${ticket.titre} » ?`
    );
    if (!confirme) return;
    setTicketEnTraitement(ticket.id);
    setErreurAction(null);
    setErreurSuppression(null);
    try {
      await supprimerTicket(ticket.id);
      const resteSurCettePage = reponseListe && reponseListe.contenu.length > 1;
      await charger(resteSurCettePage ? {} : { page: Math.max(pageCourante - 1, 0) });
    } catch (erreur) {
      setErreurSuppression(
        `Ticket « ${ticket.titre} » : ${erreur instanceof ErreurApi ? erreur.message : 'la suppression a échoué.'}`
      );
    } finally {
      setTicketEnTraitement(null);
    }
  }

  const donnees = reponseListe || { contenu: [], totalElements: 0, totalPages: 0, ticketsOuverts: 0 };

  return (
    <main className="page">
      <header className="entete">
        <h1>Gestion des tickets</h1>
        <ControleTheme />
      </header>

      <p className="compteur">
        <strong>{donnees.ticketsOuverts}</strong> ticket{donnees.ticketsOuverts > 1 ? 's' : ''} ouvert
        {donnees.ticketsOuverts > 1 ? 's' : ''} sur {donnees.totalElements} au total
      </p>

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
        <button
          type="button"
          className="puce puce-tri"
          onClick={() => changerTri(tri === 'desc' ? 'asc' : 'desc')}
          title="Tri par date de création"
        >
          {tri === 'desc' ? '↓ Plus récents' : '↑ Plus anciens'}
        </button>
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
          <Bouton onClick={() => charger()}>Réessayer</Bouton>
        </div>
      )}

      {!chargement && !erreurChargement && (
        <>
          {erreurAction && <p className="message-erreur" role="alert">{erreurAction}</p>}
          {erreurSuppression && <p className="message-erreur" role="alert">{erreurSuppression}</p>}

          <section className="card card-tableau">
            <TicketList
              tickets={donnees.contenu}
              surDemarrer={gererDemarrer}
              surFermer={gererFermeture}
              surSupprimer={gererSuppression}
              ticketEnTraitement={ticketEnTraitement}
            />
          </section>

          {donnees.totalPages > 1 && (
            <nav className="pagination" aria-label="Pagination">
              <button
                type="button"
                className="puce"
                disabled={pageCourante === 0}
                onClick={() => changerPage(pageCourante - 1)}
              >
                ← Précédent
              </button>
              <span className="page-info">Page {pageCourante + 1} / {donnees.totalPages}</span>
              <button
                type="button"
                className="puce"
                disabled={pageCourante >= donnees.totalPages - 1}
                onClick={() => changerPage(pageCourante + 1)}
              >
                Suivant →
              </button>
            </nav>
          )}
        </>
      )}

      <TicketForm
        onSubmit={gererCreation}
        enCours={creationEnCours}
        erreur={erreurFormulaire}
      />

      <FloatingActions />
    </main>
  );
}
