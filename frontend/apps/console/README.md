# Console d'administration et d'audit

Couche de présentation `@gimac/app-console` (D-46, CC 6.3.1, D-61, D-62).

| Élément | Valeur |
|---|---|
| Public | super administrateur, RSSI (et, pour leur espace personnel, tout utilisateur de la console) |
| Zone réseau | zone d'administration hors bande (D-17) |
| Authentification | Keycloak par la passerelle, **deux facteurs exigés** ; réauthentification FIDO2 (`ADMIN_SENSITIVE`) pour les actes sensibles |
| Port de développement | 4202 (relais `/api`, `/bff`, `/oauth2`, `/login`, `/logout` vers la passerelle, comme l'application du département) |
| Image | `frontend-console` (`docker build --build-arg APPLICATION=console` dans `frontend/`) |

Aucun administrateur n'ouvre la console Keycloak (D-62) : comptes, profils, politique de sécurité, messagerie et supervision se configurent ici.

## Écrans

| Route | Permission | Contenu |
|---|---|---|
| `/console/tableau-de-bord` | - | Super administrateur (`PLATFORM_SUPERVISION_READ` et `IAM_ROLE_PRIVILEGED_GRANT`) : état de la plateforme en temps réel (flux SSE `GET /api/plateforme/flux`, événement `plateforme`, reconnexion à délai croissant, état du flux annoncé), services (latence, version, CPU, tas, disque), serveurs et conteneurs, alertes par sévérité, indicateurs des comptes. RSSI (`IAM_ACCOUNT_READ` sans `IAM_ROLE_PRIVILEGED_GRANT`) : posture de sécurité des comptes, derniers actes, politique, supervision si la session l'ouvre |
| `/console/comptes` | `IAM_ACCOUNT_READ` | Liste paginée côté serveur, recherche, filtres profil et état (`ACTIF`, `INACTIF`, `PREMIER_ACCES`, `SANS_2FA`) |
| `/console/comptes/nouveau` | `IAM_ACCOUNT_CREATE` | Création (aucune inscription libre) : identité, profils (privilégiés seulement avec `IAM_ROLE_PRIVILEGED_GRANT`), invitation et/ou mot de passe provisoire affiché une seule fois (`OneTimeSecret`) |
| `/console/comptes/:id` | `IAM_ACCOUNT_READ` | Fiche, sécurité, sessions, journal ; actes selon la permission : fiche (`IAM_ACCOUNT_EDIT`), profils (`IAM_ACCOUNT_ROLES`, FIDO2 et motif), activation (`IAM_ACCOUNT_STATUS`, motif), actions requises, second facteur, mot de passe provisoire, sessions (`IAM_ACCOUNT_SECURITY`, motif), suppression (`IAM_ACCOUNT_DELETE`, confirmation irréversible, FIDO2 et motif). Un administrateur n'agit pas sur son propre compte |
| `/console/journal` | `IAM_JOURNAL_READ` | Journal d'administration paginé, filtré par compte |
| `/console/profils` | `IAM_ACCOUNT_READ` (`IAM_PROFILE_EDIT` pour modifier) | Profils, titulaires, composition par domaine (HOM, CER, PAR, IAM, PLATFORM ; INTERNAL exclu) avec `PermissionPicker`, FIDO2 et motif ; profil privilégié modifiable avec `IAM_ROLE_PRIVILEGED_GRANT` seulement |
| `/console/politique-securite` | `PLATFORM_SECURITY_POLICY_READ` (`_EDIT` pour appliquer) | 16 champs, planchers de MS-IAM en aide, FIDO2 et motif, erreurs `IAM_POLICY_INVALID` rattachées à chaque champ (`details.champs`) |
| `/console/messagerie` | `PLATFORM_MESSAGING_EDIT` | Serveur SMTP, mot de passe en écriture seule (« défini », vide conserve l'existant), motif, courriel de test |
| `/console/plateforme/configuration` | `PLATFORM_CONFIG_READ` | Routes de la passerelle, profils actifs et propriétés masquées de chaque service |
| `/console/plateforme/journalisation` | `PLATFORM_CONFIG_READ` (`PLATFORM_CONFIG_EDIT` pour modifier) | Niveaux des loggers d'un service, filtre par préfixe, changement à chaud avec motif (jamais pour le RSSI) |
| `/console/mon-compte`, `/console/deconnexion` | - | Espace personnel commun (`@gimac/compte`) |

Toutes les pages sont dans l'enveloppe commune (navigation élaguée selon les permissions, pied de page GIMAC) ; le premier accès (`GardePremierAcces`) passe avant tout écran.

## Architecture

Même découpage que `@gimac/lot1` : `src/services/ports.ts` (`ServiceComptes`, `ServiceConfiguration`, `ServicePlateforme`, `ServiceReauthentification`), `services/http` (`/bff/console/iam/comptes`, `/bff/console/iam/configuration`, `/api/plateforme`, flux `ouvrirFlux`), `services/simule` (données fictives, comptes de démonstration `super.admin` et `rssi`, mêmes refus que MS-IAM), `hooks`, `pages`, `composants` (`useActes` : ConfirmDialog motivé ou SignDialog avec réauthentification, 428, `IAM_REAUTH_INVALID`, `IAM_NO_SECURITY_KEY` avec lien vers « Mon compte »), `i18n` (FR, EN, ES). Composition exclusive du design system.

Services réels avec `VITE_SERVICES=http` (défaut de l'image) ; sinon simulés. Profil de démonstration : `VITE_PROFIL_DEMO` (`ROLE_SUPER_ADMIN` par défaut, `ROLE_RSSI`) ou clé `gm.profilDemo` du stockage local ; `gm.premierAccesDemo=1` pour démontrer le premier accès (services simulés seulement).

```
npm run dev:console                        # depuis frontend/
npm run test -w @gimac/app-console         # 62 tests
npm run couverture -w @gimac/app-console   # 80 % bloquants (D-61)
npm run e2e:couverture                     # depuis frontend/ : scénarios Playwright et seuil de 80 %
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | b20a049 | Génération `create-vite`, page d'accueil provisoire, thème, relais /api vers la passerelle |
| 2026-09-28 | - | Remplacement du README du gabarit Vite par ce README |
| 2026-09-29 | - | Console réelle (D-62) : connexion, garde de session à deux facteurs, premier accès, tableaux de bord du super administrateur (supervision temps réel par SSE) et du RSSI, comptes, journal, politique de sécurité, messagerie, profils et permissions, configuration et journalisation de la plateforme, Mon compte ; services HTTP et simulés ; FR/EN/ES ; 62 tests, couverture 98,6 % des instructions, 89,5 % des branches ; scénarios de bout en bout |
