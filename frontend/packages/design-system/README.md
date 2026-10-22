# @gimac/design-system

Système de conception de la plateforme Moabi du GIMAC (DS-GIMAC-01 v1.0), porté en React et TypeScript strict à partir du système de conception publié. Référence unique du frontend : un écart est une demande de changement, pas une initiative.

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
| Jeton `gm-color-action-subtle` (sombre) | Texte d'action et badge « information » à 4,48:1 sur ce fond, sous le seuil de 4,5:1 (audit axe du thème sombre, D-62) | `#2A4467` devient `#263E5F` : 4,9:1, écart de teinte imperceptible |
| Jeton `gm-grey-600` (texte d'aide, clair) | Aide atténuée (`.gm-field-help`) à 4,47:1 sur le fond de page `gm-grey-100` (en-tête de fiche, « non renseigné »), relevé par l'audit axe de l'application du département (D-63) | `#6B7280` devient `#667080` : 4,63:1 sur la page, 5,01:1 sur la surface |
| DataTable | Un clic sur un bouton ou un lien d'une ligne ouvrait aussi le tiroir de la ligne | Les contrôles de la ligne (bouton, lien, champ) agissent seuls ; le reste de la ligne l'ouvre |
| ResourceList | « Plus d'actions » ne faisait rien ; une action lancée depuis le tiroir le laissait ouvert sous sa confirmation | Le bouton ouvre le tiroir de la ligne, qui porte toutes les actions permises ; lancer une action referme le tiroir |

Écarts du système publié conservés à l'identique et signalés par les tests (à trancher au référentiel) : bordure de champ en clair à 1,9:1 au lieu de 3:1 ; étiquette de `FilterBar` (`.gm-filter-label`, texte atténué sur `gm-color-surface-sunken`) à 4,39:1 au lieu de 4,5:1, relevée par l'audit axe de bout en bout de la console (D-62) et seule exclusion de cet audit.

## Composites de la console et de « Mon compte » (D-62)

Assemblages de composants existants, sans nouvelle feuille de style ni valeur en dur, testés (`test/composites-d62.test.tsx`, 80 % bloquants) et présentés au catalogue vivant :

| Composant | Assemblage | Emploi |
|---|---|---|
| `OneTimeSecret` | Banner, puce `gm-chip`, Button | Mot de passe provisoire affiché une seule fois, copie annoncée |
| `PermissionPicker` | un CheckboxGroup par domaine | Composition d'un profil (HOM, CER, PAR, IAM, PLATFORM) |
| `LiveStatGroup` | StatCard, StatusBadge, zone `aria-live` | Indicateurs alimentés par un flux temps réel, état du flux annoncé |

Extensions rétrocompatibles : `TextField` `type="password"` (secret en écriture seule), `Checkbox` et `CheckboxGroup` `error` (erreur annoncée et reliée par `aria-describedby`), `CheckboxGroup` `disabled` et « Tout sélectionner » calculé sur les seules options du groupe.

## Composites de la tranche DDRM et du cadrage (D-63)

Promus depuis les écrans de l'application du département, où ils étaient composés à la main (grilles, listes et rangées de boutons en styles en ligne) : assemblages de composants et de classes existants, sans nouvelle feuille de style ni valeur en dur, testés (`test/composites-d63.test.tsx`, 80 % bloquants) et présentés au catalogue vivant.

| Composant | Assemblage | Emploi |
|---|---|---|
| `PageHeader` | classes `gm-resource-head`, `gm-heading-display`, `gm-type-micro` | En-tête d'écran : surtitre, titre, ligne de contexte (profils en Tag), actions |
| `PageSection` | région nommée, titre `gm-type-h3`, actions | Section d'un tableau de bord par acteur |
| `StatGrid` | StatCard dans un groupe nommé | Tuiles d'indicateurs (liaisons en cadrage, mes projets…) |
| `LinkList` | Link, StatusBadge, liste sans puces | « Mes dossiers », « Mes projets », liaisons d'une institution, raccourcis |
| `ActionBar` | rangée `gm-row` alignée | Pied de carte ou de formulaire |
| `ToastRegion` | emplacement fixe des Toast | Retours de l'exécuteur d'actions du lot 1 |

Extensions rétrocompatibles : `ResourceForm` `intro` (texte sous le titre : effet de la soumission), `ResourceDetail` `summary` (résumé sous l'en-tête : phase, périmètre, intervenants d'une liaison).

## Commandes

```
npm run jetons -w @gimac/design-system   # régénère tokens.css depuis tokens.json
npm run lint -w @gimac/design-system     # TypeScript strict, aucune valeur en dur, jetons à jour
npm run test -w @gimac/design-system     # contrastes WCAG depuis les jetons, rendu, ARIA et clavier, entrées du catalogue
npm run couverture -w @gimac/design-system   # 80 % bloquants sur tout le paquet (D-61)
npm run test -w @gimac/app-catalogue     # chaque aperçu rendu et audité par axe, clair et sombre
```

Le catalogue vivant (`apps/catalogue`) affiche chaque composant, un à un, en clair et en sombre.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 6eca1ce | Création : jetons, styles, polices, pictogrammes et portage React des 87 composants du système de conception publié |
| 2026-09-29 | — | ThemeProvider, reset.css, contrôle des valeurs en dur, tests (contrastes, composants, axe) ; corrections de DiffView, GanttView, DecisionToTaskRow et de l'accessibilité |
| 2026-09-29 | — | Internationalisation FR, EN, ES de tous les composants (`LangueProvider`, français par défaut, rendu inchangé) ; 18 extensions de contrat rétrocompatibles pour le lot 1 (quatre quitus, états parcourus, vérification d'empreinte, motif et zone complémentaire des dialogues, `onFile`, `onMenu`, onglets contrôlés, `onRowOpen`, `onQuery`, sélection dans DagView, anneau compact…) ; feuille adaptative sous 640 px (portail mobile, tableaux en cartes) |
| 2026-09-29 | — | Composant `Footer` (pied de page GIMAC, §6.3) : groupement, version, classification, documentation, identifiant de corrélation, FR/EN/ES ; seuil de couverture de 80 % sur les nouveaux composants, cliquet sur le catalogue existant (D-61) |
| 2026-09-29 | — | D-62 : composites `OneTimeSecret`, `PermissionPicker`, `LiveStatGroup` (catalogue vivant, tests, audit axe) ; extensions `TextField` password, erreur de `Checkbox` et `CheckboxGroup`, `CheckboxGroup` désactivé ; libellés FR, EN, ES |
| 2026-09-29 | — | D-63 : composites `PageHeader`, `PageSection`, `StatGrid`, `LinkList`, `ActionBar`, `ToastRegion` promus depuis l'application du département ; `ResourceForm` `intro`, `ResourceDetail` `summary` ; corrections de DataTable (clic sur un contrôle de ligne), de ResourceList (« Plus d'actions », tiroir refermé à l'action) et du jeton `gm-grey-600` (4,63:1 sur le fond de page) ; tests du CRUD, des interactions et rendu de chaque entrée du catalogue : couverture portée à plus de 80 % sur tout le paquet, seuil bloquant (D-61) |
| 2026-10-06 | — | Graphiques de métriques (lot 1 de la supervision en graphiques) : Sparkline, tuile de mesure (StatCard `serie`), SerieTemporelle (axe du temps, trous, réticule et infobulle souris et clavier, « Autres », vue Tableau, export branché), BarresHorizontales ; jetons `gm-serie-1` à `4` et `gm-serie-autre` validés pour la vision des couleurs en clair et en sombre ; tests, couverture et catalogue |
| 2026-10-06 | — | Grille de graphiques `gm-graphiques-grille` ; légende des graphiques sans coupure entre la clé et son libellé |
| 2026-10-19 | D-112 | `gm-chip` sans coupure de texte ; DagView : nœud de 150 px, titre sur deux lignes au plus et lisible au survol ; GanttView : pourcentage d'une barre écrit seulement si elle peut le contenir |
| 2026-10-19 | D-113 | Nouveau composant AgendaGrid (vues jour, semaine, mois) au catalogue ; KanbanBoard : glisser-déposer, Alt + flèche, ouverture, ajout par colonne, pagination des colonnes ; TicketCard `reference` |
| 2026-10-19 | D-114 | WallBoard : `label` de la pastille (état réel), nom et grand chiffre sans débordement ; GanttView : colonne « Tenue » seulement si un jalon a une probabilité ; onglets sur une ligne, défilants ; barres de défilement accordées au thème (`color-scheme`) |
| 2026-10-20 | D-118 | Footer : `plateforme` (désignation du produit hébergé) et `classification` facultatives ; une chaîne vide retire la classification. Libellés inchangés. |
