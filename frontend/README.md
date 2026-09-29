# Couches de présentation (frontend)

Espace de travail npm des trois couches de présentation de la plateforme (D-46, D-51). Aucune logique métier ni donnée persistante dans le navigateur : les données viennent de MS-GATEWAY selon le modèle BFF, et aucun jeton n'est exposé au navigateur (RG-58).

| Dossier | Paquet | Couche | Zone | Port de dev |
|---|---|---|---|---|
| `apps/departement` | `@gimac/app-departement` | Application du département | Applicative | 4200 |
| `apps/portail` | `@gimac/app-portail` | Portail des participants | DMZ | 4201 |
| `apps/console` | `@gimac/app-console` | Console d'administration et d'audit | Administration hors bande | 4202 |
| `packages/theme` | `@gimac/theme` | Jetons du thème (CC 6.2) | — | — |
| `packages/design-system` | `@gimac/design-system` | Système de conception DS-GIMAC-01 (jetons, styles, 87 composants React) | — | — |
| `packages/socle` | `@gimac/socle` | Socle d'interface F0 : client passerelle, canal poussé, FIDO2, i18n, enveloppe, connexion | — | — |
| `packages/lot1` | `@gimac/lot1` | Lot 1 et certification : ports, services, hooks, tableau de bord par acteur, registre des institutions (DDRM), cadrage des liaisons (étape 3), écrans DES-08 à DES-12 et POR-03 | — | — |
| `packages/compte` | `@gimac/compte` | « Mon compte » et premier accès (D-62), communs aux trois couches : port `/bff/<couche>/iam/moi`, services HTTP et simulé, garde de premier accès, assistant, page Mon compte | — | — |
| `e2e` | — | Scénarios de bout en bout Playwright par acteur, couverture istanbul avec seuil de 80 % (hors espaces de travail npm) | — | — |
| `apps/catalogue` | `@gimac/app-catalogue` | Catalogue vivant, hors production | Poste de développement | 4203 |

Pile : React 19.2, TypeScript 6, Vite 8, oxlint, générés par `create-vite` (gabarit `react-ts`).

## Commandes

```
npm install                   # à la racine de frontend/
npm run dev:departement       # ou dev:portail, dev:console ; /api relayé vers localhost:8080
npm run build                 # les trois couches
npm run lint
npm test                      # tests de tous les paquets
npm run couverture            # tests avec seuils de couverture (D-61)
npm run e2e                   # constructions instrumentées puis scénarios Playwright
npm run e2e:couverture        # idem, puis nyc : 80 % bloquants (instructions, branches, fonctions, lignes)
docker build --build-arg APPLICATION=portail -t frontend-portail .
```

## Tests de bout en bout (D-61, D-62)

`e2e/` : scénarios Playwright rédigés en français sur le modèle Gherkin (« Fonctionnalité », « Scénario », étapes « Étant donné », « Quand », « Alors »), par acteur :

| Fichier | Scénarios |
|---|---|
| `specs/console-super-admin.spec.ts` | Supervision temps réel, création et administration complète d'un compte (profils signés, statut, actions requises, second facteur, mot de passe provisoire, sessions, suppression), filtres, politique, messagerie, profils, configuration et journalisation, journal, Mon compte, bascule de langue |
| `specs/console-rssi.spec.ts` | Posture de sécurité, aucun profil privilégié proposé ni modifiable, dernier super administrateur protégé, journalisation en lecture seule |
| `specs/console-passerelle*.spec.ts` | Console construite avec `VITE_SERVICES=http`, passerelle simulée par Playwright (`page.route`) : flux SSE et reconnexion, réauthentification FIDO2 par `navigator.credentials` simulé (428 puis succès, annulation, `IAM_NO_SECURITY_KEY`), enrôlement d'une clé, chaque acte d'administration sur sa route, session absente, sans second facteur ou indisponible, déconnexion |
| `specs/console-refus.spec.ts` | Refus de MS-IAM rendus au bon endroit, états dégradés de la plateforme, politique en consultation seule |
| `specs/premier-acces.spec.ts` | Premier accès puis Mon compte dans la console, l'application du département et le portail, déconnexion |
| `specs/departement-charge-ddrm.spec.ts` | Chargé de la DDRM : prise de contact, fiche, « Mes dossiers », dépôt, validation et rejet motivé des pièces, conditions d'adhésion, proposition d'adhésion, refus d'une seconde proposition |
| `specs/departement-chef-ddrm.spec.ts` | Chef de la DDRM : approbation signée et rejet motivé des propositions, ouverture d'une liaison au cadrage depuis le registre, désignation motivée du conducteur, bascule automatique de l'institution en homologation, doublon refusé |
| `specs/departement-chef-des.spec.ts` | Chef du DES : liaisons en cadrage, désignation depuis la liste, « Mes projets », recherche, pilotage, ouverture renvoyée au registre, refus d'un type sans référentiel publié, onglets d'une liaison, attribution d'une opération, export des preuves, dégel motivé, dossier de mise en service |
| `specs/departement-conducteur.spec.ts` | Conducteur de projet : « Mes projets » au tableau de bord et activé d'emblée dans la liste, fiche en lecture sans désignation |
| `specs/departement-lecture.spec.ts` | Normes et agréments, ingénieur d'homologation : tableaux de bord en lecture seule, ni ouverture ni désignation |
| `specs/departement-passerelle.spec.ts` | Application du département construite avec `VITE_SERVICES=http`, passerelle simulée (`passerelle-des.ts`) : approbation signée par `navigator.credentials` (`X-Reauth-Assertion`, finalité `STATUS_APPROVE`), `POST /liaisons` refusé (`HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE`) puis accepté, `PUT /liaisons/{id}/chef-de-projet` motivé, 403 `HOM_PROJECT_DESIGNATION_FORBIDDEN`, liaison sans périmètre ni conducteur, liste indisponible, canal poussé |
| `specs/accessibilite.spec.ts` | Audit axe (WCAG 2.2 AA) en clair et en sombre : console, Mon compte, premier accès, écrans de l'application du département (tableau de bord, registre, fiche, ouverture d'une liaison, liaisons, désignation), dialogues |

`npm run e2e:construire` produit des constructions instrumentées par `vite-plugin-istanbul` (`GIMAC_E2E=1`, `e2e/instrumentation.mts`) : `dist-e2e` (services simulés) pour les trois couches et `dist-e2e-http` pour la console et l'application du département ; `vite preview` les sert (ports 5200, 5201, 5202, 5210, 5212). Chaque scénario dépose `window.__coverage__` dans `e2e/.nyc_output` (aussi à chaque déchargement de page) ; `nyc report` (`e2e/.nycrc.json`) échoue sous 80 %. Périmètre mesuré : la console, `@gimac/compte`, la réauthentification et la déconnexion du socle, les points d'entrée des trois couches et l'application du département (`apps/departement/src`, écrans, compositions, descripteurs et exécuteur d'actions des étapes 1 à 3 dans `@gimac/lot1` ; les services HTTP et simulés relèvent des tests de contrat Vitest). Le profil de démonstration de chaque scénario est posé par `gm.profilDemo` (fixture `demo`). Chromium préinstallé : `/opt/pw-browsers/chromium` (ou `GIMAC_CHROMIUM`), jamais `playwright install`.

Image derrière un relais d'entreprise : `docker build --build-arg HTTPS_PROXY=… --secret id=ca,src=<autorité> --build-arg APPLICATION=console .` ; `SERVICES` vaut `http` par défaut (services réels de la passerelle).

La variable `GIMAC_PASSERELLE` change l'adresse de la passerelle en développement.

## Image

`Dockerfile` commun, argument `APPLICATION` (et `SERVICES`, `http` par défaut) : les manifestes `package.json` de tous les espaces de travail (racine, `packages/*`, `apps/*`, `e2e`) sont d'abord extraits à leur place pour `npm ci` (tout nouvel espace de travail est pris en compte sans modifier le fichier), puis construction avec Node 22 et service par `nginx-unprivileged` sur le port 8080, sous un utilisateur non root, en lecture seule. `nginx/gimac.conf.template` et `nginx/securite.conf` fixent la politique de sécurité du contenu, le cache et le relais vers la passerelle (`/api`, `/bff`, `/oauth2`, `/login`, `/logout`) ; les flux SSE (`/api/plateforme/flux`, `/bff/<couche>/live`) passent sans mise en tampon (HTTP/1.1, `proxy_buffering off`, délai de lecture de 24 h). `/sante` sert de sonde. Chaque couche est servie à la racine de son port ou de son hôte (4200, 4201, 4202 sous Docker Compose ; `departement`, `portail`, `console.des.gimac.local` sous Kubernetes) et route elle-même `/des`, `/portail` ou `/console`.

## Suite prévue

Enveloppe commune IHM-01, bibliothèque de composants IHM-02, navigation élaguée IHM-03, bilinguisme IHM-10 et accessibilité WCAG 2.2 AA IHM-11, conformément au carnet v2.0.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | b20a049 | Création : trois applications `create-vite`, paquet `@gimac/theme`, Dockerfile, configuration nginx |
| 2026-09-28 | — | Ajout de ce README et des README des applications |
| 2026-09-28 | 6eca1ce | Paquet `@gimac/design-system` (portage React du DS-GIMAC-01, 87 composants) et catalogue vivant `apps/catalogue` (port 4203) |
| 2026-09-29 | — | Système de conception terminé : ThemeProvider, reset.css, contrôle des valeurs en dur, tests (48 unitaires, 174 audits axe), corrections de DiffView, GanttView et DecisionToTaskRow |
| 2026-09-29 | — | Lot 1 : paquets `@gimac/socle` (F0) et `@gimac/lot1` (ports, services HTTP et simulés, hooks, écrans DES-08 à DES-12 et POR-03) ; connexion TRA-01 ; i18n FR, EN, ES ; relais `/bff` dans Vite et nginx |
| 2026-09-29 | — | Tranche DDRM : session à double facteur et matrice des rôles, tableau de bord par acteur, registre des institutions, pied de page GIMAC ; seuils de couverture Vitest (D-61) |
| 2026-09-29 | — | D-62 : console d'administration réelle (supervision temps réel, comptes, journal, politique de sécurité, messagerie, profils, configuration et journalisation), paquet `@gimac/compte` (Mon compte et premier accès dans les trois couches), composites du design system (`OneTimeSecret`, `PermissionPicker`, `LiveStatGroup`), tests de bout en bout Playwright avec seuil de couverture istanbul de 80 % ; Dockerfile (tous les manifestes de l'espace de travail, `SERVICES=http`), relais SSE sans tampon dans nginx et l'Ingress |
| 2026-09-29 | — | Étape 3, cadrage (D-57, D-58) dans `@gimac/lot1` et l'application du département (ouverture depuis la fiche de l'institution, résumé et désignation du conducteur, « Mes projets », tuiles par acteur, profils de démonstration) ; tâche 9 : audit D-63 de la tranche DDRM (composites `PageHeader`, `PageSection`, `StatGrid`, `LinkList`, `ActionBar`, `ToastRegion` promus au système de conception, corrections de DataTable, ResourceList et du jeton `gm-grey-600`), scénarios de bout en bout par acteur de l'application du département (simulée et sur passerelle simulée), audit axe clair et sombre, périmètre de couverture étendu au département ; couverture Vitest de 80 % bloquante sur tous les paquets (D-61) |
