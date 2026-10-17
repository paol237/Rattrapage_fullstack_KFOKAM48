import { BadgeStatut } from './BadgeStatut.jsx';
import Bouton from './Bouton.jsx';

function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function TicketList({ tickets, surDemarrer, surFermer, surSupprimer, ticketEnTraitement }) {
  if (tickets.length === 0) {
    return <p className="liste-vide">Aucun ticket</p>;
  }

  return (
    <table className="tableau">
      <thead>
        <tr>
          <th>Titre</th>
          <th>Statut</th>
          <th>Date de création</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        {tickets.map((ticket) => (
          <tr key={ticket.id}>
            <td>{ticket.titre}</td>
            <td><BadgeStatut statut={ticket.statut} /></td>
            <td>{formatDate(ticket.dateCreation)}</td>
            <td className="cellule-actions">
              {ticket.statut === 'OUVERT' && (
                <Bouton
                  variante="ambre"
                  taille="petit"
                  onClick={() => surDemarrer(ticket)}
                  disabled={ticketEnTraitement === ticket.id}
                  chargement={ticketEnTraitement === ticket.id}
                  title="Passer ce ticket en EN_COURS"
                >
                  {ticketEnTraitement === ticket.id ? '…' : 'Démarrer'}
                </Bouton>
              )}
              {ticket.statut === 'OUVERT' || ticket.statut === 'EN_COURS' ? (
                <Bouton
                  variante="danger"
                  taille="petit"
                  onClick={() => surFermer(ticket)}
                  disabled={ticketEnTraitement === ticket.id}
                  chargement={ticketEnTraitement === ticket.id}
                >
                  {ticketEnTraitement === ticket.id ? '…' : 'Fermer'}
                </Bouton>
              ) : (
                <Bouton
                  variante="neutre"
                  taille="petit"
                  onClick={() => surSupprimer(ticket)}
                  disabled={ticketEnTraitement === ticket.id}
                  chargement={ticketEnTraitement === ticket.id}
                  title="Supprimer ce ticket fermé (définif)"
                >
                  {ticketEnTraitement === ticket.id ? '…' : 'Supprimer'}
                </Bouton>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
