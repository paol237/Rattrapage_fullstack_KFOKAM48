import { useEffect, useState } from 'react';

const CLE_STOCKAGE = 'theme';
const MODES = ['clair', 'sombre', 'systeme'];
const LIBELLES = { clair: 'Clair', sombre: 'Sombre', systeme: 'Système' };

function lireModeInitial() {
  try {
    const valeur = localStorage.getItem(CLE_STOCKAGE);
    return MODES.includes(valeur) ? valeur : 'systeme';
  } catch {
    return 'systeme';
  }
}

function estSombre(mode) {
  if (mode === 'sombre') return true;
  if (mode === 'clair') return false;
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

function appliquerSurDocument(mode) {
  document.documentElement.classList.toggle('theme-sombre', estSombre(mode));
  document.documentElement.dataset.theme = mode;
}

function Icone({ mode }) {
  if (mode === 'clair') {
    return (
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
        <circle cx="10" cy="10" r="4" stroke="currentColor" strokeWidth="2" />
        <path d="M10 1.5v2M10 16.5v2M1.5 10h2M16.5 10h2M3.7 3.7l1.4 1.4M14.9 14.9l1.4 1.4M16.3 3.7l-1.4 1.4M5.1 14.9l-1.4 1.4"
          stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
    );
  }
  if (mode === 'sombre') {
    return (
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
        <path d="M17 11.5A7.5 7.5 0 0 1 8.5 3a7.5 7.5 0 1 0 8.5 8.5z"
          stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
      </svg>
    );
  }
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <rect x="2.5" y="3.5" width="15" height="10" rx="1.5" stroke="currentColor" strokeWidth="2" />
      <path d="M7 17h6M10 13.5V17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

/**
 * Control de theme clair / sombre / systeme.
 * - Trois modes en boucle, persistes dans localStorage.
 * - En mode systeme, suit les changements de prefers-color-scheme.
 * - Le script inline de index.html applique le theme avant le premier
 *   rendu (pas de flash au chargement).
 */
export default function ControleTheme() {
  const [mode, setMode] = useState(lireModeInitial);

  useEffect(() => {
    appliquerSurDocument(mode);
    try {
      localStorage.setItem(CLE_STOCKAGE, mode);
    } catch {
      /* stockage indisponible : le theme reste valable pour la session */
    }
    if (mode !== 'systeme') return undefined;
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const gererChangement = () => appliquerSurDocument('systeme');
    media.addEventListener('change', gererChangement);
    return () => media.removeEventListener('change', gererChangement);
  }, [mode]);

  function changerMode() {
    setMode((courant) => MODES[(MODES.indexOf(courant) + 1) % MODES.length]);
  }

  const libelle = `Thème : ${LIBELLES[mode]} — activer pour changer`;

  return (
    <button
      type="button"
      className="controle-theme"
      onClick={changerMode}
      aria-label={libelle}
      title={libelle}
      data-mode={mode}
    >
      <Icone mode={mode} />
      <span className="controle-theme-libelle">{LIBELLES[mode]}</span>
    </button>
  );
}
