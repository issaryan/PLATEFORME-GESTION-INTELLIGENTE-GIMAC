# @gimac/lot1

Interface du lot 1 (PI-GIMAC-01) et de la certification de bout en bout (D-56 à D-61) : tableau de bord par acteur (DES-01), registre des institutions et étapes 1 et 2 du parcours (DES-05 à DES-07, tranche DDRM), écrans DES-08 à DES-12 de l'application du département et POR-03 du portail.

## Découplage des composants et des appels

```
pages (compositions du design system) ──> hooks de données ──> ports (interfaces) ──> implémentation
                                                                                      ├─ http    : passerelle /bff (réel)
                                                                                      └─ simule  : serveur fictif en mémoire
```

- **Composants** : uniquement ceux de `@gimac/design-system`, purement visuels.
- **Ports** (`src/services/ports.ts`) : `ServiceParticipant` (registre, dossier d'adhésion, maker-checker), `ServiceHomologation`, `ServiceCertification`, `ServicePortail`, `ServiceReauthentification`. Seul contrat entre les écrans et les données.
- **Implémentations** : `services/http` (chemins de la cartographie des contrats du plan) et `services/simule` (données de développement fictives, latence, refus normalisés). L'application choisit : simulée par défaut, réelle avec `VITE_SERVICES=http`.
- **Hooks** (`src/hooks`) : seuls à appeler les services, par injection (`ServicesProvider`) ; clés de requête `[sujet, ressource, …]` invalidées par le canal poussé.
- **Descripteurs** (`src/ressources`) : déclaratifs, libellés en clés i18n, actions agissant par `ctx.services` ; convertis pour le catalogue par `useDescripteurDS`. `ExecuteurActions` porte confirmation, signature FIDO2, motif et retours.
- **Adaptateurs** (`src/adaptateurs`) : fonctions pures des réponses des services vers les propriétés des composants.
- **i18n** (`src/i18n`) : un catalogue FR, EN, ES par écran.

Aucune règle métier n'est évaluée côté client : transitions, permissions, couvertures et impartialité viennent du service.

**Matrice des rôles (D-58, D-61).** Les pages lisent les permissions de la session (`usePermissions` du socle, alimenté par `/api/session` après une connexion à deux facteurs) et les passent aux composants du catalogue : une action non permise est absente, pas grisée. Le tableau de bord (`TableauDeBordPage`) ne compose que les sections que ces permissions ouvrent : propositions à trancher pour le chef de la DDRM, dossiers en cours pour le chargé, pilotage des liaisons pour l'ingénieur d'homologation, raccourcis vers les écrans permis.

**Tranche DDRM.** `InstitutionsPage` (liste paginée côté serveur, filtre de statut, « Mes dossiers »), `InstitutionPage` (fiche, dossier d'adhésion pièce par pièce, conditions d'adhésion, prérequis, propositions de statut avec approbation signée), formulaires de prise de contact, de conditions et de dépôt (MS-DOCUMENT puis rattachement de la référence et de l'empreinte).

```
npm run test -w @gimac/lot1
npm run couverture -w @gimac/lot1   # seuils : 80 % sur les tranches livrées, cliquet ailleurs (D-61)
npm run lint -w @gimac/lot1
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Création : ports, implémentations HTTP et simulée, hooks, descripteurs, écrans du lot 1 |
| 2026-09-29 | — | Tranche DDRM : port `ServiceParticipant` (HTTP et simulé), hooks, descripteurs, tableau de bord par acteur, registre et dossier numérique des institutions, formulaires ; permissions de la session passées aux composants ; 121 tests, couverture de la tranche au-dessus de 80 % (D-61) |
| 2026-09-29 | — | Parcours de la liaison, socle (D-56 à D-59) : modèle des étapes 3 à 10, référentiel de l'annexe A du CR v1.4 (rangs 1 à 35, C1 à C10, M1 à M17, D1 à D34), matrice des 15 profils par fonction, ports `ServiceParcours`, `ServiceDecision` et `ServicePortailParcours` (HTTP : routes réelles de l'étape 3 ; simulé : liaisons fictives à chaque étape), hooks, vocabulaire FR, EN, ES ; profil de démonstration par `?profil=ROLE_…` |
