import { BadgeStatut } from './BadgeStatut.jsx';

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

export default function TicketList({ tickets, surFermer, ticketEnTraitement }) {
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
            <td>
              {ticket.statut === 'OUVERT' || ticket.statut === 'EN_COURS' ? (
                <button
                  type="button"
                  className="bouton bouton-fermer"
                  onClick={() => surFermer(ticket)}
                  disabled={ticketEnTraitement === ticket.id}
                >
                  {ticketEnTraitement === ticket.id ? '…' : 'Fermer'}
                </button>
              ) : (
                <span className="aucune-action">—</span>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
