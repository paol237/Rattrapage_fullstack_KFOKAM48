import { useEffect, useRef } from 'react';

/**
 * Arriere-plan decoratif : reseau de neurones artistique (Canvas 2D natif).
 *
 * - Particules lumineuses (neurones) qui derivent lentement.
 * - Liaisons fines entre particules proches, l'opacite depend de la distance.
 * - Impulsions lumineuses qui circulent le long de certaines liaisons.
 * - Legere reaction des neurones proches du curseur (aucun pointeur capture :
 *   le canvas est pointer-events: none).
 *
 * Performance et accessibilite :
 * - requestAnimationFrame, delta plafonne, Densite de pixels plafonnee a 2,
 *   nombre de particules adapte a la taille de l'ecran.
 * - Animation en pause quand l'onglet est masque ; tout est nettoie au demontage.
 * - prefers-reduced-motion : un seul cadre statique (sans impulsions ni souris),
 *   redessine lors des redimensionnements ; reagit au changement en direct.
 * - Couleurs lues dans les variables CSS (--reseau-*) : suivent le theme clair/sombre.
 */

const LIAISON_SEUIL = 140; // distance max pour relier deux neurones
const NB_MAX = 64;
const IMPULSIONS_MAX = 3;
const RAYON_CURSEUR = 130;

/** Convertit une couleur CSS (#hex, #hex court ou rgb()) en canaux decimaux. */
function canaux(couleur) {
  if (couleur.startsWith('#')) {
    const hex = couleur.slice(1);
    const complet = hex.length === 3 ? hex.split('').map((c) => c + c).join('') : hex;
    return [0, 2, 4].map((i) => parseInt(complet.slice(i, i + 2), 16) || 0);
  }
  const valeurs = couleur.match(/[\d.]+/g) || [];
  return [Number(valeurs[0]) || 0, Number(valeurs[1]) || 0, Number(valeurs[2]) || 0];
}

export default function ReseauNeurones() {
  const refCanvas = useRef(null);

  useEffect(() => {
    const canvas = refCanvas.current;
    if (!canvas) return undefined;
    const ctx = canvas.getContext('2d');
    if (!ctx) return undefined;

    // Couleurs issues du design system : suivent le theme clair/sombre.
    const styles = getComputedStyle(document.documentElement);
    const couleurNeurone = styles.getPropertyValue('--reseau-neurone').trim() || '#0b5cad';
    const couleurLiaison = styles.getPropertyValue('--reseau-liaison').trim() || couleurNeurone;
    const couleurImpulsion = styles.getPropertyValue('--reseau-impulsion').trim() || '#d97706';
    const [rNeurone, gNeurone, bNeurone] = canaux(couleurNeurone);
    const [rLiaison, gLiaison, bLiaison] = canaux(couleurLiaison);
    const [rImpulsion, gImpulsion, bImpulsion] = canaux(couleurImpulsion);

    const mediaReduit = window.matchMedia('(prefers-reduced-motion: reduce)');
    let largeur = 0;
    let hauteur = 0;
    let neurones = [];
    let impulsions = [];
    let liaisonsActives = []; // { a, b } pour le tirage des impulsions
    let curseur = { x: -9999, y: -9999 };
    let tempsAnimation = 0;
    let tempsCadrePrecedent = 0;
    let temporisation = null;
    let identifiantImage = 0;
    let statique = mediaReduit.matches;

    const colorer = (r, g, b, alpha) => `rgba(${r}, ${g}, ${b}, ${alpha})`;

    function creerNeurones() {
      const nb = Math.max(20, Math.min(NB_MAX, Math.round((largeur * hauteur) / 26000)));
      neurones = Array.from({ length: nb }, () => ({
        x: Math.random() * largeur,
        y: Math.random() * hauteur,
        vx: (Math.random() - 0.5) * 9, // px/s : derive tres lente
        vy: (Math.random() - 0.5) * 9,
        taille: 1.2 + Math.random() * 1.2,
        phase: Math.random() * Math.PI * 2, // scintillement personnel
      }));
      impulsions = [];
    }

    function redimensionner() {
      const densite = Math.min(window.devicePixelRatio || 1, 2);
      largeur = window.innerWidth;
      hauteur = window.innerHeight;
      canvas.width = Math.round(largeur * densite);
      canvas.height = Math.round(hauteur * densite);
      ctx.setTransform(densite, 0, 0, densite, 0, 0);
      creerNeurones();
      dessiner();
    }

    function dessiner() {
      ctx.clearRect(0, 0, largeur, hauteur);
      liaisonsActives = [];

      // Liaisons : opacite dependante de la distance, structure legerement vivante
      // (une pulsation lente par paire cree des zones plus ou moins connectees).
      for (let i = 0; i < neurones.length; i++) {
        const a = neurones[i];
        for (let j = i + 1; j < neurones.length; j++) {
          const b = neurones[j];
          const dx = a.x - b.x;
          const dy = a.y - b.y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist >= LIAISON_SEUIL) continue;
          const force = 1 - dist / LIAISON_SEUIL;
          const souffle = 0.55 + 0.45 * Math.sin(tempsAnimation * 0.6 + i * 1.7 + j * 0.9);
          const alpha = force * 0.28 * souffle;
          ctx.strokeStyle = colorer(rLiaison, gLiaison, bLiaison, alpha.toFixed(3));
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(a.x, a.y);
          ctx.lineTo(b.x, b.y);
          ctx.stroke();
          liaisonsActives.push({ a, b, force });
        }
      }

      // Impulsions lumineuses circulant sur quelques liaisons.
      for (let i = impulsions.length - 1; i >= 0; i--) {
        const p = impulsions[i];
        if (!p.a || !p.b || p.avancement >= 1) {
          impulsions.splice(i, 1);
          continue;
        }
        const x = p.a.x + (p.b.x - p.a.x) * p.avancement;
        const y = p.a.y + (p.b.y - p.a.y) * p.avancement;
        const fondu = Math.sin(p.avancement * Math.PI); // apparition et disparition douces
        ctx.fillStyle = colorer(rImpulsion, gImpulsion, bImpulsion, (0.75 * fondu).toFixed(3));
        ctx.beginPath();
        ctx.arc(x, y, 2.2, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = colorer(rImpulsion, gImpulsion, bImpulsion, (0.25 * fondu).toFixed(3));
        ctx.beginPath();
        ctx.arc(x, y, 6, 0, Math.PI * 2);
        ctx.fill();
      }

      // Neurones : point net + halo doux, brillance pulsant tres lentement.
      for (const n of neurones) {
        const scintille = 0.55 + 0.45 * Math.sin(tempsAnimation * 1.2 + n.phase);
        let eclat = 0.35 + 0.4 * scintille;
        // Reaction discrete au curseur : simple eclaircissement de proximite.
        const distCurseur = Math.hypot(n.x - curseur.x, n.y - curseur.y);
        if (distCurseur < RAYON_CURSEUR) {
          eclat += (1 - distCurseur / RAYON_CURSEUR) * 0.45;
        }
        ctx.fillStyle = colorer(rNeurone, gNeurone, bNeurone, (Math.min(0.8, eclat) * 0.35).toFixed(3));
        ctx.beginPath();
        ctx.arc(n.x, n.y, n.taille * 3, 0, Math.PI * 2); // halo
        ctx.fill();
        ctx.fillStyle = colorer(rNeurone, gNeurone, bNeurone, Math.min(0.8, eclat).toFixed(3));
        ctx.beginPath();
        ctx.arc(n.x, n.y, n.taille, 0, Math.PI * 2); // point net
        ctx.fill();
      }
    }

    function avancerNeurones(delta) {
      const marge = 24;
      for (const n of neurones) {
        n.x += n.vx * delta;
        n.y += n.vy * delta;
        if (n.x < -marge) n.x = largeur + marge;
        if (n.x > largeur + marge) n.x = -marge;
        if (n.y < -marge) n.y = hauteur + marge;
        if (n.y > hauteur + marge) n.y = -marge;
      }
    }

    function tirerImpulsion() {
      if (impulsions.length >= IMPULSIONS_MAX || liaisonsActives.length === 0) return;
      const { a, b } = liaisonsActives[(Math.random() * liaisonsActives.length) | 0];
      impulsions.push({ a, b, avancement: 0, vitesse: 0.25 + Math.random() * 0.2 });
    }

    function boucle(temps) {
      const delta = Math.min((temps - tempsCadrePrecedent) / 1000, 0.1); // delta plafonne
      tempsCadrePrecedent = temps;
      tempsAnimation += delta;
      avancerNeurones(delta);
      tirerImpulsion();
      for (const p of impulsions) p.avancement += p.vitesse * delta;
      dessiner();
      identifiantImage = requestAnimationFrame(boucle);
    }

    function demarrer() {
      if (identifiantImage) return;
      tempsCadrePrecedent = performance.now();
      identifiantImage = requestAnimationFrame(boucle);
    }

    function arreter() {
      if (!identifiantImage) return;
      cancelAnimationFrame(identifiantImage);
      identifiantImage = 0;
    }

    // --- Evenements, tous nettoyes au demontage ---

    const surMouvement = (e) => {
      curseur.x = e.clientX;
      curseur.y = e.clientY;
    };
    const surQuitter = () => {
      curseur.x = -9999;
      curseur.y = -9999;
    };
    const surVisibilite = () => (document.hidden ? arreter() : demarrer());
    const surRedimensionnement = () => {
      clearTimeout(temporisation);
      temporisation = setTimeout(redimensionner, 150); // debonce : evite les recalculs en rafale
    };
    function mettreAJourMode(e) {
      statique = e.matches;
      arreter();
      redimensionner(); // cadre statique ou neuf
      if (!statique) demarrer();
    }

    if (statique) {
      redimensionner(); // un seul cadre, pas de boucle, pas de suivi du curseur
    } else {
      redimensionner();
      demarrer();
      window.addEventListener('pointermove', surMouvement, { passive: true });
      window.addEventListener('pointerleave', surQuitter);
      document.addEventListener('visibilitychange', surVisibilite);
    }
    window.addEventListener('resize', surRedimensionnement);
    mediaReduit.addEventListener('change', mettreAJourMode);

    return () => {
      arreter();
      clearTimeout(temporisation);
      window.removeEventListener('pointermove', surMouvement);
      window.removeEventListener('pointerleave', surQuitter);
      document.removeEventListener('visibilitychange', surVisibilite);
      window.removeEventListener('resize', surRedimensionnement);
      mediaReduit.removeEventListener('change', mettreAJourMode);
    };
  }, []);

  return <canvas ref={refCanvas} className="reseau-neurones" aria-hidden="true" />;
}
