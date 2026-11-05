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
| Activité des tests | `/des/sandbox/activite` | `SBX_SUP_CONSULTATION` ; chaîne de certification sous `HOM_LIAISON_READ` | SBX-LECTURE, cinq lectures exactes sous `/bff/des/sbx-lecture/statistiques` ; MS-HOMOLOGATION `/bff/des/homologation/liaisons/{id}` |

Le composant `EspaceInstitution` (fiche, motifs, suspension, rétablissement, fermeture, ouverture si absent) sert aussi
l'onglet « Sandbox » du dossier de liaison.

## Graphes (D-121)

« Activité des tests » montre les tests des participants dans la sandbox. Les agrégats sont calculés en SQL par SBX-LECTURE (`StatistiquesConsoleController`) : l'écran n'agrège rien en masse. Seul le pas du temps est complété par des zéros.

- Bibliothèque : les composants SVG du design-system (`SerieTemporelle`, `BarresHorizontales`, `StatGrid`, `StateMachineBar`, `DataTable`), déjà présents dans le monorepo, sans dépendance nouvelle. Chaque carte de graphe a sa vue tableau.
- Règles dataviz : une seule teinte (`gm-data-1`) pour les grandeurs ; les trois interfaces en petits multiples sur la même échelle, plutôt qu'en couleurs d'identité ; les statuts portent un libellé. Le validateur de palette rejette `gm-data-3` en sombre et `gm-data-7` et `8` dans les deux thèmes (note au journal du design-system), d'où ce choix.
- Filtres portés par l'adresse : `periode` (7, 30, 90, 365 jours ; pas du jour, de la semaine, du mois), `participant`, `interface`.
- Liens : participant vers `/des/sandbox/espaces/{code}`, campagne et dossier vers `/des/parcours/{liaison}?onglet=sandbox` (pièces, bilan et empreintes).
- Codes de rejet : « source indisponible » tant que `exchange-captured.v1` ne porte pas `codeRetour` jusqu'à SBX-TRACE.

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
| 2026-11-04 | - | Couverture rétablie à 100 % (repli de la clé d'idempotence, troisième page des éléments supervisés) : 49 tests |
| 2026-11-05 | - | D-121 : écran « Activité des tests » (/des/sandbox/activite, SBX_SUP_CONSULTATION) : indicateurs, exécutions par interface en petits multiples, verdicts, taux des campagnes, cas défavorables, codes de rejet (source indisponible), chaîne de certification (HOM_LIAISON_READ, mode dégradé), délais ; HTTP et simulé au contrat ; FR/EN/ES |
| 2026-11-05 | D-121, complément | Section « Services de la sandbox » de l'écran de supervision (lecture `/bff/des/sbx-trace/services`, relue toutes les 30 s, mini-courbe des latences), états par module en badges, pagination commune ; FR/EN/ES ; 51 tests, couverture 100 % des instructions et 99,62 % des branches |
