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
| `packages/lot1` | `@gimac/lot1` | Lot 1 : ports, services, hooks, écrans DES-08 à DES-12 et POR-03 | — | — |
| `apps/catalogue` | `@gimac/app-catalogue` | Catalogue vivant, hors production | Poste de développement | 4203 |

Pile : React 19.2, TypeScript 6, Vite 8, oxlint, générés par `create-vite` (gabarit `react-ts`).

## Commandes

```
npm install                   # à la racine de frontend/
npm run dev:departement       # ou dev:portail, dev:console ; /api relayé vers localhost:8080
npm run build                 # les trois couches
npm run lint
docker build --build-arg APPLICATION=portail -t frontend-portail .
```

La variable `GIMAC_PASSERELLE` change l'adresse de la passerelle en développement.

## Image

`Dockerfile` commun, argument `APPLICATION` : construction avec Node 22, puis service par `nginx-unprivileged` sur le port 8080, sous un utilisateur non root, en lecture seule. `nginx/gimac.conf.template` et `nginx/securite.conf` fixent la politique de sécurité du contenu, le cache et le relais vers la passerelle. `/sante` sert de sonde.

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
