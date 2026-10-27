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
| Supervision de la sandbox | `/des/sandbox/supervision` | `SBX_SUP_CONSULTATION` | SBX-TRACE, quatre lectures exactes sous `/bff/des/sbx-trace` |

Le composant `EspaceInstitution` (fiche, motifs, suspension, rétablissement, fermeture, ouverture si absent) sert aussi
l'onglet « Sandbox » du dossier de liaison.

## Services

- HTTP : `servicesSandboxHttp()` ; toute écriture porte l'en-tête `Idempotency-Key`, tiré à l'ouverture du dialogue et
  gardé pour ses nouvelles tentatives. La supervision appelle `etat-sante/detail`, `elements-supervises`, `alertes` et
  `disponibilites`, avec pagination numérotée pour les trois listes.
- Simulé (`VITE_SERVICES=simule`) : `servicesSandboxSimules()`, au contrat exact de SBX-ESPACE et de ces quatre lectures
  de SBX-TRACE (permission `SBX_SUP_CONSULTATION`, compte GIMAC, pages bornées). Jeu fictif : institutions 99001 à 99005,
  santé dégradée, 23 éléments supervisés, alertes et indicateurs mensuels.
- Comptes de démonstration : `super.admin` (lecture et actes), chef du DES et RSSI (lecture).

## Tests

`npx vitest run --coverage` : seuil de 98 % par fichier (instructions, branches, fonctions, lignes). Preuve du 27/10/2026 :
46 tests réussis ; 100 % des instructions, fonctions et lignes, 99,55 % des branches.

## Recette

`docs/recette/sandbox.md` référence DEMO-SBX-12 : les espaces et les résultats de campagne disposent de 30 captures
clair/sombre dans `docs/recette/captures/exi/homologation/`. Les captures ont été produites sur services simulés et
ne prouvent pas un raccordement réel à SBX-ESPACE ou SBX-LECTURE. L'écran D-121 est validé par les 46 tests ci-dessus,
sans nouvelle capture et sans démarrage du Compose complet.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-10-22 | - | Création (D-122) : registre des entrées, « Espaces des institutions » sur SBX-ESPACE (HTTP et simulé), fiche réutilisable |
| 2026-10-27 | - | D-121 : écran « Supervision de la sandbox », HTTP/simulation, FR/EN/ES, trois paginations, 46 tests et couverture au-dessus de 98 % |
| 2026-10-27 | - | DEMO-SBX-12 : 30 captures des espaces et résultats référencées ; absence de nouvelles captures D-121 et de preuve Compose/serveurs réels explicitée |
