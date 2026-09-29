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

Avec les services réels (`VITE_SERVICES=http`), la session est lue sur `/api/session` : sans second facteur, aucun écran n'est servi ; chaque acteur arrive sur **son tableau de bord** (`/des/tableau-de-bord`) et ne voit que les écrans et actions de sa matrice des rôles. Pied de page GIMAC sur toutes les pages.

En mode simulé, le profil de démonstration (`src/profils.ts`, d'après le domaine « gimac ») est choisi par la préférence locale `gm.profilDemo`, puis par `VITE_PROFIL_DEMO` ; « Mon compte » simulé porte son identité :

| Profil | Démonstration | Particularités (D-57, D-58) |
|---|---|---|
| `ROLE_CHEF_DDRM` (défaut) | Bertrand Nguema Obiang | Tranche les propositions ; ouvre les liaisons au cadrage et désigne leur conducteur (`HOM_LIAISON_OPEN` et `HOM_PROJECT_DESIGNATE`) |
| `ROLE_CHARGE_DDRM` | Fatimé Abakar Hissein | Instruit les candidatures, propose |
| `ROLE_CHEF_DES` | Paul Mbarga Atangana | Ouvre et désigne ; dégel, export des preuves, quitus technique |
| `ROLE_INGENIEUR_HOMOLOGATION` | Joël Ekotto Mballa | N'ouvre plus de liaison ; référentiels, opérations, preuves |
| `ROLE_CONDUCTEUR_PROJET` | Aïcha Mahamat Nour | Lit ; « Mes projets » activé d'emblée |
| `ROLE_SNA` | Rachel Biyong Mengue | Lecture, dépôt et validation de preuves |

```
npm run dev:departement                  # depuis frontend/
npm run test -w @gimac/app-departement    # routes, profils, services réels ou simulés
npm run couverture -w @gimac/app-departement   # 80 % bloquants (D-61)
```

Scénarios de bout en bout par acteur : `e2e/specs/departement-*.spec.ts` (voir `frontend/README.md`).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | b20a049 | Génération `create-vite`, page d'accueil provisoire, thème, relais /api vers la passerelle |
| 2026-09-28 | — | Remplacement du README du gabarit Vite par ce README |
| 2026-09-29 | — | Routes du lot 1, enveloppe commune, connexion TRA-01, services simulés par défaut (`VITE_SERVICES=http` pour la passerelle), i18n FR, EN, ES, relais `/bff` |
| 2026-09-29 | — | Garde de session à double facteur, matrice des rôles depuis `/api/session`, tableau de bord par acteur, registre des institutions, pied de page GIMAC (D-58, D-61) |
| 2026-09-29 | — | D-62 : premier accès (`GardePremierAcces`) avant tout écran, « Mon compte » (`/des/mon-compte`) et déconnexion (`/des/deconnexion`) dans l'espace personnel de l'enveloppe (`@gimac/compte`, `/bff/des/iam/moi`) ; `gm.premierAccesDemo=1` pour la démonstration en services simulés |
| 2026-09-29 | — | Étape 3, cadrage (D-57, D-58) : profils de démonstration alignés sur le domaine « gimac » (`src/profils.ts` ; chefs du DES et de la DDRM ouvrent et désignent, ingénieur sans ouverture, conducteur de projet, SNA), choix par `gm.profilDemo`, « Mon compte » simulé au nom du profil ; tests Vitest (15, 80 % bloquants) et scénarios de bout en bout par acteur, construction instrumentée `dist-e2e-http` (D-61) |
