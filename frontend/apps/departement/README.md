# Application du département

Couche de présentation `@gimac/app-departement` (D-46, CC 6.3.1).

| Élément | Valeur |
|---|---|
| Public | collaborateurs du DES et des directions |
| Zone réseau | zone applicative |
| Authentification | Keycloak par la passerelle, **deux facteurs exigés** (mot de passe puis TOTP, `acr` 2, D-61) ; réauthentification FIDO2 pour les décisions |
| Port de développement | 4200 |
| Image | `frontend-departement` (`docker build --build-arg APPLICATION=departement` dans `frontend/`) |

Généré par `create-vite` (gabarit `react-ts`).

Avec les services réels (`VITE_SERVICES=http`), la session est lue sur `/api/session` : sans second facteur, aucun écran n'est servi ; chaque acteur arrive sur **son tableau de bord** (`/des/tableau-de-bord`) et ne voit que les écrans et actions de sa matrice des rôles. En mode simulé, le profil de démonstration est choisi par `VITE_PROFIL_DEMO` (`ROLE_CHEF_DDRM` par défaut, `ROLE_CHARGE_DDRM`, `ROLE_INGENIEUR_HOMOLOGATION`). Pied de page GIMAC sur toutes les pages.

```
npm run dev:departement      # depuis frontend/
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | b20a049 | Génération `create-vite`, page d'accueil provisoire, thème, relais /api vers la passerelle |
| 2026-09-28 | — | Remplacement du README du gabarit Vite par ce README |
| 2026-09-29 | — | Routes du lot 1, enveloppe commune, connexion TRA-01, services simulés par défaut (`VITE_SERVICES=http` pour la passerelle), i18n FR, EN, ES, relais `/bff` |
| 2026-09-29 | — | Garde de session à double facteur, matrice des rôles depuis `/api/session`, tableau de bord par acteur, registre des institutions, pied de page GIMAC (D-58, D-61) |
