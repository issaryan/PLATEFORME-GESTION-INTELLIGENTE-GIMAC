# Portail des participants

Couche de présentation `@gimac/app-portail` (D-46, CC 6.3.1).

| Élément | Valeur |
|---|---|
| Public | correspondants des institutions membres, cloisonnés par institution (RG-59, D-21) |
| Zone réseau | DMZ |
| Authentification | fournisseur d'identité, facteur fort ; adapté jusqu'à 360 px de large |
| Port de développement | 4201 |
| Image | `frontend-portail` (`docker build --build-arg APPLICATION=portail` dans `frontend/`) |

Généré par `create-vite` (gabarit `react-ts`). La page de démonstration de Vite est remplacée par une page d'accueil provisoire en français, sur les jetons de `@gimac/theme`, en attendant l'enveloppe commune (IHM-01).

```
npm run dev:portail      # depuis frontend/
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | b20a049 | Génération `create-vite`, page d'accueil provisoire, thème, relais /api vers la passerelle |
| 2026-09-28 | - | Remplacement du README du gabarit Vite par ce README |
| 2026-09-29 | - | Routes du lot 1, enveloppe commune, connexion TRA-01, services simulés par défaut (`VITE_SERVICES=http` pour la passerelle), i18n FR, EN, ES, relais `/bff` |
| 2026-09-29 | - | D-61, D-62 : session lue sur `/api/session` avec second facteur exigé (services réels), premier accès avant tout écran, « Mon compte » (`/portail/mon-compte`) et déconnexion dans l'espace personnel (`@gimac/compte`, `/bff/portail/iam/moi`) |
