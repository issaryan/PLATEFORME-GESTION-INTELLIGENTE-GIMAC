# @gimac/design-system

Système de conception de la plateforme DES du GIMAC (DS-GIMAC-01 v1.0), porté en React et TypeScript strict à partir du système de conception publié. Référence unique du frontend : un écart est une demande de changement, pas une initiative.

## Contenu

| Dossier | Contenu |
|---|---|
| `src/jetons/` | `tokens.json` (source unique), `tokens.css` généré (primitives, couche sémantique, mode sombre sous `data-theme="dark"`, espacements, formes, mouvements, tailles, classes `.gm-type-*`), `fonts.css` (Ubuntu, Inter, JetBrains Mono auto-hébergées) |
| `src/styles/` | Feuilles des composants, reprises à l'identique et dans le même ordre de cascade que l'original ; `index.css` les assemble |
| `src/assets/` | Logos (couleur, bleu, blanc) et motif de trame |
| `src/icones/` | Pictogrammes Lucide (trait 1,5 px) et composant `Icon` |
| `src/actions/`, `saisie/`, `affichage/`, `navigation/`, `conteneurs/`, `donnees/`, `enveloppe/`, `domaine/`, `crud/` | Un dossier par composant (`<Nom>/<Nom>.tsx`), rangés par famille (chapitre 7) ; le CRUD universel lit un descripteur de ressource (chapitre 8) |

## Emploi

```tsx
import '@gimac/design-system/styles.css'
import { Button, StatusBadge } from '@gimac/design-system'
```

Le thème sombre s'active par `data-theme="dark"` sur la racine (ou sur un conteneur), ou par le fournisseur `ThemeProvider` qui pose `data-theme` et `data-density` depuis les préférences du profil (`useTheme()` pour les changer). Les couches importent aussi `@gimac/design-system/reset.css`. Les composants ne consomment que des jetons `--gm-*`.

## Fidélité et écarts

Chaque composant a été comparé au pixel près, en clair et en sombre, au rendu du système publié : écart nul, hors animations (spinner, squelette) et anticrénelage de quelques pixels. Les écarts suivants sont volontaires, car ils corrigent des défauts de l'original :

| Composant | Défaut de l'original | Correction |
|---|---|---|
| DiffView | Les lignes longues débordaient du cadre | Défilement horizontal dans le composant |
| GanttView | Échelle fixe de 14 px par jour : la frise sortait de la carte | Échelle adaptative de 14 px au plus, 6 px au moins |
| DecisionToTaskRow | Colonne d'état de largeur nulle : le badge sortait de la ligne | Colonne à la largeur du badge |
| DateField, FileDropzone, SlaClock, DecisionToTaskRow | Rôles ARIA invalides ou barres de progression sans nom (axe) | Rôles et noms accessibles, rendu inchangé |
| DeviceVerificationCard | L'empreinte passait par la prop réservée `key` | Prop `fingerprint` |
| Drawer | `useState` après un retour anticipé | Ordre des hooks corrigé |

Écarts du système publié conservés à l'identique et signalés par les tests (à trancher au référentiel) : bordure de champ en clair à 1,9:1 au lieu de 3:1 ; badge « information » en sombre à 4,48:1 au lieu de 4,5:1.

## Commandes

```
npm run jetons -w @gimac/design-system   # régénère tokens.css depuis tokens.json
npm run lint -w @gimac/design-system     # TypeScript strict, aucune valeur en dur, jetons à jour
npm run test -w @gimac/design-system     # contrastes WCAG depuis les jetons, rendu, ARIA et clavier
npm run test -w @gimac/app-catalogue     # chaque aperçu rendu et audité par axe, clair et sombre
```

Le catalogue vivant (`apps/catalogue`) affiche chaque composant, un à un, en clair et en sombre.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 6eca1ce | Création : jetons, styles, polices, pictogrammes et portage React des 87 composants du système de conception publié |
| 2026-09-29 | — | ThemeProvider, reset.css, contrôle des valeurs en dur, tests (contrastes, composants, axe) ; corrections de DiffView, GanttView, DecisionToTaskRow et de l'accessibilité |
| 2026-09-29 | — | Internationalisation FR, EN, ES de tous les composants (`LangueProvider`, français par défaut, rendu inchangé) ; 18 extensions de contrat rétrocompatibles pour le lot 1 (quatre quitus, états parcourus, vérification d'empreinte, motif et zone complémentaire des dialogues, `onFile`, `onMenu`, onglets contrôlés, `onRowOpen`, `onQuery`, sélection dans DagView, anneau compact…) ; feuille adaptative sous 640 px (portail mobile, tableaux en cartes) |
