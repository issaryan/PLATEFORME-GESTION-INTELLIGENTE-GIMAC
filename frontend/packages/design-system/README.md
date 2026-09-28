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

Le thème sombre s'active par `data-theme="dark"` sur la racine (ou sur un conteneur). Les composants ne consomment que des jetons `--gm-*`.

## Commandes

```
npm run jetons -w @gimac/design-system   # régénère tokens.css depuis tokens.json
npm run lint -w @gimac/design-system     # TypeScript strict et jetons à jour
```

Le catalogue vivant (`apps/catalogue`) affiche chaque composant, un à un, en clair et en sombre.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | — | Création : jetons, styles, polices, pictogrammes et portage React des composants du système de conception publié |
