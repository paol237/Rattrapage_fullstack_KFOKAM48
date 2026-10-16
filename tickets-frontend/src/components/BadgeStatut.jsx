const STATUTS = ['OUVERT', 'EN_COURS', 'FERME'];

/** Badge de couleur par statut (bonus du sujet). */
export function BadgeStatut({ statut }) {
  const couleurs = {
    OUVERT: 'badge badge-ouvert',
    EN_COURS: 'badge badge-en-cours',
    FERME: 'badge badge-ferme',
  };
  return <span className={couleurs[statut] || 'badge'}>{statut}</span>;
}

export { STATUTS };
