const BASE_URL = 'http://localhost:8080/api/tickets';

/**
 * Service dedie aux appels API vers le backend.
 * Aucun composant n'appelle fetch directement.
 */
export async function listerTickets({ statut, page = 0, taille = 5, tri = 'desc' } = {}) {
  const params = new URLSearchParams({
    page: String(page),
    taille: String(taille),
    tri,
  });
  if (statut) {
    params.set('statut', statut);
  }
  const reponse = await fetch(`${BASE_URL}?${params.toString()}`);
  if (!reponse.ok) {
    throw new ErreurApi(`Le serveur a repondu avec le code ${reponse.status}.`, reponse.status);
  }
  return reponse.json();
}

export async function creerTicket({ titre, description }) {
  const reponse = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ titre, description }),
  });
  const corps = await reponse.json().catch(() => null);
  if (!reponse.ok) {
    throw new ErreurApi(extraireMessage(corps), reponse.status);
  }
  return corps;
}

export async function fermerTicket(id) {
  const reponse = await fetch(`${BASE_URL}/${id}/fermer`, { method: 'PATCH' });
  const corps = await reponse.json().catch(() => null);
  if (!reponse.ok) {
    throw new ErreurApi(extraireMessage(corps), reponse.status);
  }
  return corps;
}

export async function supprimerTicket(id) {
  const reponse = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
  if (!reponse.ok && reponse.status !== 204) {
    const corps = await reponse.json().catch(() => null);
    throw new ErreurApi(extraireMessage(corps), reponse.status);
  }
  return true;
}

export class ErreurApi extends Error {
  constructor(message, statut) {
    super(message);
    this.statut = statut;
  }
}

function extraireMessage(corps) {
  if (corps && typeof corps.message === 'string' && corps.message.length > 0) {
    return corps.message;
  }
  return 'Le serveur a refuse la demande sans message exploitable.';
}
