import { useEffect, useState } from 'react';

const SEUIL_HAUT_FIXE = 300;
const DUREE_DEFILEMENT = 350;

/**
 * Seuil adaptatif : 300 px, ou la moitie du defilement maximal lorsque la
 * page est courte (sinon le bouton ne apparaitrait jamais).
 */
function seuilDefilement() {
  const defilementMax = document.documentElement.scrollHeight - window.innerHeight;
  if (defilementMax <= 0) return Number.POSITIVE_INFINITY;
  return Math.min(SEUIL_HAUT_FIXE, defilementMax / 2);
}

function comportementDoux() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
}

/**
 * Boutons flottants :
 * - « Haut de page » : n'apparait qu'apres un vrai defilement (400 px),
 *   inutilisable au clavier lorsqu'il est masque.
 * - « Nouveau ticket » : amene au formulaire et place le focus sur le champ titre.
 * Les deux ont une action reelle, aucun bouton decoratif.
 */
export default function FloatingActions() {
  const [defile, setDefile] = useState(false);

  useEffect(() => {
    function gererDefilement() {
      // Hysteresis : le bouton reste visible jusqu'au haut reel, pour ne pas
      // perdre le focus en plein defilement doux (cela l'interromprait).
      setDefile((etat) => {
        const y = window.scrollY;
        if (y > seuilDefilement()) return true;
        if (y < 1) return false; /* cache seulement une fois en haut reel */
        return etat;
      });
    }
    gererDefilement();
    window.addEventListener('scroll', gererDefilement, { passive: true });
    return () => window.removeEventListener('scroll', gererDefilement);
  }, []);

  function versFormulaire() {
    const section = document.getElementById('formulaire-nouveau-ticket');
    if (!section) return;
    section.scrollIntoView({ behavior: comportementDoux(), block: 'center' });
    const champ = document.getElementById('champ-titre');
    if (champ) {
      window.setTimeout(() => champ.focus({ preventScroll: true }), DUREE_DEFILEMENT);
    }
  }

  function versHaut() {
    window.scrollTo({ top: 0, behavior: comportementDoux() });
  }

  return (
    <div className="actions-flottantes">
      <button
        type="button"
        className={'bouton-flottant bouton-haut' + (defile ? ' est-visible' : '')}
        onClick={versHaut}
        aria-label="Retour en haut de la page"
        title="Retour en haut"
        aria-hidden={!defile}
        tabIndex={defile ? 0 : -1}
      >
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
          <path
            d="M10 15V5M10 5L5 10M10 5l5 5"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      <button
        type="button"
        className="bouton-flottant bouton-nouveau"
        onClick={versFormulaire}
        aria-label="Créer un nouveau ticket"
        title="Nouveau ticket"
      >
        <svg width="22" height="22" viewBox="0 0 22 22" fill="none" aria-hidden="true">
          <path d="M11 4v14M4 11h14" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
        </svg>
      </button>
    </div>
  );
}
