# Couches de présentation (frontend)

Espace de travail npm des trois couches de présentation de la plateforme (D-46, D-51). Aucune logique métier ni donnée persistante dans le navigateur : les données viennent de MS-GATEWAY selon le modèle BFF, et aucun jeton n'est exposé au navigateur (RG-58).

| Dossier | Paquet | Couche | Zone | Port de dev |
|---|---|---|---|---|
| `apps/departement` | `@gimac/app-departement` | Application du département | Applicative | 4200 |
| `apps/portail` | `@gimac/app-portail` | Portail des participants | DMZ | 4201 |
| `apps/console` | `@gimac/app-console` | Console d'administration et d'audit | Administration hors bande | 4202 |
| `packages/theme` | `@gimac/theme` | Jetons du thème (CC 6.2) | — | — |

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
