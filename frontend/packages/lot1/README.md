# @gimac/lot1

Interface du lot 1 (PI-GIMAC-01) : écrans DES-08 à DES-12 de l'application du département et POR-03 du portail.

## Découplage des composants et des appels

```
pages (compositions du design system) ──> hooks de données ──> ports (interfaces) ──> implémentation
                                                                                      ├─ http    : passerelle /bff (réel)
                                                                                      └─ simule  : serveur fictif en mémoire
```

- **Composants** : uniquement ceux de `@gimac/design-system`, purement visuels.
- **Ports** (`src/services/ports.ts`) : `ServiceHomologation`, `ServiceCertification`, `ServicePortail`, `ServiceReauthentification`. Seul contrat entre les écrans et les données.
- **Implémentations** : `services/http` (chemins de la cartographie des contrats du plan) et `services/simule` (données de développement fictives, latence, refus normalisés). L'application choisit : simulée par défaut, réelle avec `VITE_SERVICES=http`.
- **Hooks** (`src/hooks`) : seuls à appeler les services, par injection (`ServicesProvider`) ; clés de requête `[sujet, ressource, …]` invalidées par le canal poussé.
- **Descripteurs** (`src/ressources`) : déclaratifs, libellés en clés i18n, actions agissant par `ctx.services` ; convertis pour le catalogue par `useDescripteurDS`. `ExecuteurActions` porte confirmation, signature FIDO2, motif et retours.
- **Adaptateurs** (`src/adaptateurs`) : fonctions pures des réponses des services vers les propriétés des composants.
- **i18n** (`src/i18n`) : un catalogue FR, EN, ES par écran.

Aucune règle métier n'est évaluée côté client : transitions, permissions, couvertures et impartialité viennent du service.

```
npm run test -w @gimac/lot1
npm run lint -w @gimac/lot1
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Création : ports, implémentations HTTP et simulée, hooks, descripteurs, écrans du lot 1 |
