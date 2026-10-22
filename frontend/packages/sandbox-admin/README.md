# @gimac/sandbox-admin : espace sandbox de l'application du département

Administration de la Sandbox GIMAC depuis la plateforme (D-118 points 2 et 3, D-121, D-122). Contrat :
`docs/lot1/sandbox-existant-adapte.md` §7.

## Entrées

L'espace est servi sous `/des/sandbox/*` par `<RoutesSandbox garde={G} />`. Chaque écran est une entrée du registre
`ENTREES_SANDBOX` (`src/entrees/`), dans son propre fichier : clé de la fonctionnalité au catalogue de MS-IAM (groupe
`sandbox`), chemin, permission qui la garde et routes. Une entrée de plus (« Supervision de la sandbox », SBX-TRACE)
ajoute son fichier, une ligne au registre, sa fonctionnalité au catalogue et son chemin dans `ECRANS_DES` (lot 1),
sans toucher aux autres entrées.

| Entrée | Chemin | Permission | Service |
|---|---|---|---|
| Espaces des institutions | `/des/sandbox/espaces`, `/des/sandbox/espaces/:code` | `SBX_HAB_CONSULTATION` (lecture), `SBX_HAB_ADMINISTRATION` (actes) | SBX-ESPACE, `/bff/des/sbx-espace/**` réécrit en `/api/v1/console/**` |

Le composant `EspaceInstitution` (fiche, motifs, suspension, rétablissement, fermeture, ouverture si absent) sert aussi
l'onglet « Sandbox » du dossier de liaison.

## Services

- HTTP : `servicesSandboxHttp()` ; toute écriture porte l'en-tête `Idempotency-Key`, tiré à l'ouverture du dialogue et
  gardé pour ses nouvelles tentatives.
- Simulé (`VITE_SERVICES=simule`) : `servicesSandboxSimules()`, au contrat exact de SBX-ESPACE (branche
  `sbx-espace-hexagone`) : 404 indiscernable hors droit, 422 sans second facteur, clé d'idempotence (400 absente, 422
  invalide, résultat d'origine rendu à la clé rejouée), statuts admis `CANDIDAT`, `EN_HOMOLOGATION`, `ACTIF`,
  `A_REGULARISER`. Jeu fictif : institutions 99001 à 99005.
- Comptes de démonstration : `super.admin` (lecture et actes), chef du DES et RSSI (lecture).

## Tests

`npx vitest run --coverage` : seuil de 98 % par fichier (instructions, branches, fonctions, lignes).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-10-22 | - | Création (D-122) : registre des entrées, « Espaces des institutions » sur SBX-ESPACE (HTTP et simulé), fiche réutilisable |
