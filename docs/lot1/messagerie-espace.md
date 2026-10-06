# Messagerie : largeur du fil de discussion (D-84)

Demande de KOAGNE ISSA RYAN : pouvoir élargir le chat par rapport à la barre latérale.

- **Séparateur déplaçable** entre la liste des conversations et le fil : glisser à la souris ou au doigt,
  flèches gauche et droite au clavier (pas de 16 px, `role="separator"`, `aria-orientation="vertical"`,
  `aria-valuenow` en pixels), double clic pour revenir à la largeur par défaut. Liste de 240 à 480 px.
- **Replier la liste des conversations** (bouton dans l'en-tête du fil) : le fil prend toute la largeur ; un
  bouton la rouvre.
- **Replier la barre latérale de l'application** depuis la messagerie (bouton « Élargir ») : la barre passe en
  mode icônes, et revient à son état précédent en quittant la messagerie.
- Préférences gardées par navigateur (`localStorage`, lecture et écriture protégées). Sous 900 px, l'écran
  garde son comportement actuel (une colonne à la fois).

## Mise en œuvre (frontend, 06/10/2026)

- Grille `gm-messagerie-espace` : colonnes `var(--gm-messagerie-liste) 6px 1fr`, la variable posée par React sur
  l'élément (propriété CSSOM, admise par `style-src 'self'`, comme les autres styles de l'application) ; aucune
  feuille de style injectée. Pas de 16 px au clavier, Origine et Fin pour les bornes, double clic pour 320 px ;
  glissement par les événements de pointeur (souris et doigt, `touch-action: none`).
- Bouton de repli dans l'en-tête du fil (et des panneaux « Nouveau message », « Nouveau groupe ») ; la liste
  n'est repliée que si un fil ou un panneau occupe l'écran.
- « Élargir » (en-tête de la page) : le socle offre `useBarreLaterale()` (`reduite`, `reduire`, `active`) fourni par
  l'enveloppe ; la navigation latérale (`SideNav reduite`) passe en mode icônes (72 px, libellés lus par les
  technologies d'assistance et en infobulle, pastilles en coin). La messagerie mémorise l'état de la barre à son
  ouverture et le rétablit en se fermant. Sans barre latérale fixe (moins de 1 280 px, navigation en tiroir), le
  bouton n'apparaît pas.
- Préférences : `gm.messagerie.largeurListe`, `gm.messagerie.listeRepliee`, `gm.messagerie.elargie` (lecture et
  écriture sous `try`). Sous 961 px (point de rupture existant de la messagerie, une colonne à la fois), ni
  séparateur ni repli : comportement inchangé.
