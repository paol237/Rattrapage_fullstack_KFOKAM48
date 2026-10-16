/**
 * Bouton polymorphe.
 *
 * - `as` : balise rendue — 'button' (defaut), 'a' (lien de navigation),
 *   ou toute autre balise valide.
 * - `variante` : primaire (defaut), danger, ambre, neutre.
 * - `taille` : moyen (defaut), petit.
 * - `chargement` : etat occupe — aria-busy ; sur un bouton natif,
 *   desactivation le temps du traitement.
 * - Les attributs HTML passent tels quels (href, onClick, type, disabled…).
 *
 * La semantique est preservee : le type n'est force que pour les <button>
 * (type="button" par defaut), un <a> garde ses attributs de lien et ne
 * recoit jamais `disabled` (il recoit aria-disabled via `disabled` passe).
 */
export default function Bouton({
  as,
  variante = 'primaire',
  taille = 'moyen',
  chargement = false,
  className = '',
  children,
  ...props
}) {
  const Element = as || 'button';
  const boutonNatif = Element === 'button';

  const classes = ['bouton', `bouton-${variante}`, `bouton-${taille}`]
    .concat(chargement ? 'est-chargement' : [])
    .concat(className)
    .filter(Boolean)
    .join(' ');

  const attributs = { ...props, className: classes };

  if (boutonNatif && attributs.type === undefined) {
    attributs.type = 'button';
  }
  if (chargement) {
    attributs['aria-busy'] = 'true';
    if (boutonNatif) {
      attributs.disabled = true;
    }
  }

  return <Element {...attributs}>{children}</Element>;
}
