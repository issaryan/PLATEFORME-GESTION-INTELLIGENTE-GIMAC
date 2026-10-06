# Couches de présentation (frontend)

Espace de travail npm des trois couches de présentation de la plateforme (D-46, D-51). Aucune logique métier ni donnée persistante dans le navigateur : les données viennent de MS-GATEWAY selon le modèle BFF, et aucun jeton n'est exposé au navigateur (RG-58).

| Dossier | Paquet | Couche | Zone | Port de dev |
|---|---|---|---|---|
| `apps/departement` | `@gimac/app-departement` | Application du département | Applicative | 4200 |
| `apps/portail` | `@gimac/app-portail` | Portail des participants | DMZ | 4201 |
| `apps/console` | `@gimac/app-console` | Console d'administration et d'audit | Administration hors bande | 4202 |
| `packages/theme` | `@gimac/theme` | Jetons du thème (CC 6.2) | — | — |
| `packages/design-system` | `@gimac/design-system` | Système de conception DS-GIMAC-01 (jetons, styles, 88 composants React, dont `AvatarCropper`) | — | — |
| `packages/socle` | `@gimac/socle` | Socle d'interface F0 : client passerelle (CSRF, appels de fond), canal poussé, FIDO2, i18n, enveloppe, connexion sur la plateforme et mot de passe oublié (D-65), session et fermeture après inactivité | — | — |
| `packages/lot1` | `@gimac/lot1` | Lot 1 et certification : ports, services, hooks, tableau de bord par acteur (« Mon travail », pilotage), registre des institutions (DDRM), parcours de la liaison (étapes 3 à 10, dossier numérique, verrous, mise en service, portail), messagerie de travail (D-64), écrans DES-08 à DES-12 et POR-03 | — | — |
| `packages/compte` | `@gimac/compte` | « Mon compte » et premier accès (D-62, D-65), communs aux trois couches : port `/bff/<couche>/iam/moi`, services HTTP et simulé, garde de premier accès, assistant (mot de passe, code TOTP par code QR, fiche, charte, connexion), page Mon compte en modales : photo de profil, fiche, mot de passe, code TOTP, clés, appareils connectés, historique des connexions | — | — |
| `packages/administration` | `@gimac/administration` | Administration de la plateforme (D-62, D-69) : comptes, journal, profils et politique de sécurité, servis dans la console (`/console`) et dans le département (`/des/administration`, `/bff/des/iam`) ; supervision, configuration et courriels de la plateforme pour la console seulement | — | — |
| `e2e` | — | Scénarios de bout en bout Playwright par acteur, couverture istanbul avec seuil de 80 % (hors espaces de travail npm) | — | — |
| `apps/catalogue` | `@gimac/app-catalogue` | Catalogue vivant, hors production | Poste de développement | 4203 |

Pile : React 19.2, TypeScript 6, Vite 8, oxlint, générés par `create-vite` (gabarit `react-ts`).

## Commandes

```
npm install                   # à la racine de frontend/
npm run dev:departement       # ou dev:portail, dev:console ; services réels, /api et /bff relayés vers localhost:8080
VITE_SERVICES=simule npm run dev:departement   # démonstration sur les services simulés (jamais en production)
npm run build                 # les trois couches
npm run lint
npm test                      # tests de tous les paquets
npm run couverture            # tests avec seuils de couverture (D-61)
npm run e2e                   # constructions instrumentées puis scénarios Playwright
npm run e2e:couverture        # idem, puis nyc : 80 % bloquants (instructions, branches, fonctions, lignes)
GIMAC_E2E_PORT=5400 GIMAC_E2E_SORTIE=-moi npm run e2e   # ports et dossiers dédiés (exécutions parallèles)
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
| `specs/connexion.spec.ts` | Connexion sur la plateforme (D-65) : succès avec code et retour à la page demandée, refus par code d'erreur (identifiants, compte désactivé, couche refusée, trop de tentatives), code refusé puis accepté sur la passerelle simulée (corps du contrat, jeton anti-CSRF), mot de passe oublié, nouveau mot de passe (jeton invalide, violations de la politique, succès), premier accès depuis l'écran de connexion avec code TOTP, avertissement à 28 minutes et fermeture à 30 minutes sous horloge simulée, 401 `CNX_SESSION_EXPIREE` |
| `specs/premier-acces.spec.ts` | Premier accès (mot de passe, code TOTP, fiche, charte) puis Mon compte en modales dans la console, l'application du département et le portail, déconnexion |
| `specs/departement-charge-ddrm.spec.ts` | Chargé de la DDRM : prise de contact, fiche, « Mes dossiers », dépôt, validation et rejet motivé des pièces, conditions d'adhésion, proposition d'adhésion, refus d'une seconde proposition |
| `specs/departement-chef-ddrm.spec.ts` | Chef de la DDRM : approbation signée et rejet motivé des propositions, ouverture d'une liaison au cadrage depuis le registre, désignation motivée du conducteur, bascule automatique de l'institution en homologation, doublon refusé |
| `specs/departement-chef-des.spec.ts` | Chef du DES : liaisons en cadrage, désignation depuis la liste, « Mes projets », recherche, pilotage, ouverture renvoyée au registre, refus d'un type sans référentiel publié, onglets d'une liaison, attribution d'une opération, export des preuves, dégel motivé, dossier de mise en service |
| `specs/departement-conducteur.spec.ts` | Conducteur de projet : « Mes projets » au tableau de bord et activé d'emblée dans la liste, fiche en lecture sans désignation |
| `specs/departement-lecture.spec.ts` | Normes et agréments, ingénieur d'homologation : tableaux de bord en lecture seule, ni ouverture ni désignation |
| `specs/departement-passerelle.spec.ts` | Application du département construite avec `VITE_SERVICES=http`, passerelle simulée (`passerelle-des.ts`) : approbation signée par `navigator.credentials` (`X-Reauth-Assertion`, finalité `STATUS_APPROVE`), `POST /liaisons` refusé (`HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE`) puis accepté, `PUT /liaisons/{id}/chef-de-projet` motivé, 403 `HOM_PROJECT_DESIGNATION_FORBIDDEN`, liaison sans périmètre ni conducteur, liste indisponible, canal poussé |
| `specs/coquille.spec.ts` | Coquille fixe : page sans défilement, zone de contenu seule à défiler, navigation immobile et à défilement propre, retour en haut au changement de page, page courte, tiroir sur mobile, portail et console |
| `specs/modales.spec.ts` | Modale CRUD standard : fiche d'un compte (abandon confirmé), création sur la liste, suppression et action sensible, politique et messagerie, blocage du dossier de liaison, fiche d'une institution |
| `specs/administration.spec.ts` | Gestion des utilisateurs en tête du tableau de bord (super administrateur et RSSI), indicateurs vers la liste filtrée, onglets de la fiche d'un compte, liens « Administration » du RSSI vers la console |
| `specs/mon-compte.spec.ts` | Mon compte (docs/lot1/mon-compte.md), en clair et en sombre : photo refusée (type réel) puis choisie, recadrée au clavier, enregistrée, affichée dans le menu utilisateur et la messagerie (conversations, fil, destinataires), retirée ; appareils connectés (session courante marquée, fermeture d'une session puis des autres en modale) ; historique paginé ; audits axe ; photos de la liste et de la fiche des comptes de la console ; routes du contrat sur la passerelle simulée (PUT multipart `/bff/console/iam/moi/avatar`, `/api/session/appareils`, `/api/session/historique`, `fermerAutresSessions`) |
| `specs/accessibilite.spec.ts` | Audit axe (WCAG 2.2 AA) en clair et en sombre : console, Mon compte et ses modales, premier accès, écrans de connexion, avertissement d'inactivité, écrans de l'application du département (tableau de bord, registre, fiche, ouverture d'une liaison, liaisons, désignation), dialogues |

`npm run e2e:construire` produit des constructions instrumentées par `vite-plugin-istanbul` (`GIMAC_E2E=1`, `e2e/instrumentation.mts`) : `dist-e2e` (services simulés) pour les trois couches et `dist-e2e-http` pour la console et l'application du département ; `vite preview` les sert (ports 5200, 5201, 5202, 5210, 5212). Chaque scénario dépose `window.__coverage__` dans `e2e/.nyc_output` (aussi à chaque déchargement de page) ; `nyc report` (`e2e/.nycrc.json`) échoue sous 80 %. Périmètre mesuré : la console, `@gimac/administration`, `@gimac/compte`, la réauthentification et la déconnexion du socle, les points d'entrée des trois couches et l'application du département (`apps/departement/src`, écrans, compositions, descripteurs et exécuteur d'actions des étapes 1 à 3 dans `@gimac/lot1` ; les services HTTP et simulés relèvent des tests de contrat Vitest). Le profil de démonstration de chaque scénario est posé par `gm.profilDemo` (fixture `demo`). Chromium préinstallé : `/opt/pw-browsers/chromium` (ou `GIMAC_CHROMIUM`), jamais `playwright install`.

Chaque scénario est connecté d'avance (fixture `connecte`, session de démonstration de l'onglet) ; les scénarios de connexion posent `test.use({ connecte: false })`. `GIMAC_E2E_PORT` déplace les six ports (base, base+1, base+2, base+10 à base+12), `GIMAC_E2E_SORTIE` suffixe les dossiers de construction et de résultats, `GIMAC_E2E_NYC` change le dossier de recueil de la couverture.

Image derrière un relais d'entreprise : `docker build --build-arg HTTPS_PROXY=… --secret id=ca,src=<autorité> --build-arg APPLICATION=console .` ; `SERVICES` vaut `http` par défaut (services réels de la passerelle).

La variable `GIMAC_PASSERELLE` change l'adresse de la passerelle en développement.

Exécutions parallèles sur un même poste : `GIMAC_E2E_PORT` (base des six ports de prévisualisation, 5200 par défaut), `GIMAC_E2E_SORTIE` (suffixe des dossiers `dist-e2e*` et `e2e/resultats*`), `GIMAC_E2E_NYC` (dossier de recueil de la couverture) ; `GIMAC_CAPTURES=<dossier>` enregistre les captures des scénarios de la coquille, des modales et de l'administration.

Preuves de l'aiguillage et des tableaux de bord par permissions (D-79) : `e2e/specs/roles-redirections.spec.ts` connecte
chaque profil interne et le correspondant sur chacune des trois applications, vérifie l'application d'arrivée, la barre
latérale et les widgets du tableau de bord, et enregistre toujours une capture plein écran en clair et en sombre du
tableau de bord d'arrivée : `e2e/captures/roles/<profil>-<application de connexion>-<clair|sombre>.png` (profil sans
`ROLE_`, en minuscules : `admin-systeme-console-sombre.png`). `e2e/specs/infrastructure.spec.ts` enregistre l'écran
« Infrastructure » (D-78) de l'administrateur système et du RSSI (lecture seule) dans `e2e/captures/infrastructure/`.
Avec `GIMAC_CAPTURES`, ces captures vont dans `$GIMAC_CAPTURES/roles` et `$GIMAC_CAPTURES/infrastructure`.

## Coquille, modales et administration (30/09/2026)

- **Coquille fixe** (`Enveloppe` du socle, classes `gm-shell-*` de `11-enveloppe.css`) : la page ne défile pas ; barre supérieure fixe ; navigation latérale sur toute la hauteur disponible, à défilement propre, jamais étirée par le contenu, sans nom de couche sous le logo ; zone de contenu seule à défiler, pied de page en bas de cette zone ; au clic d'une entrée, seule la zone de contenu change et revient en haut ; tiroir sous 1 280 px. Aucune hauteur calculée en JavaScript.
- **Modale CRUD standard** `CrudDialog` (système de conception, entrée au catalogue) : contextes `create`, `edit`, `delete`, `sensitive`, `confirm`, `detail` ; tailles `sm`, `md`, `lg` ; sous-titre de contexte (l'objet concerné) ; refus du service en ligne ; pied Annuler / action principale avec chargement ; ni Échap ni voile pendant l'envoi ; abandon confirmé d'un formulaire modifié. `ConfirmDialog`, `SignDialog` et `ResourceForm` (`presentation="dialog"`) en dérivent. Toute création, modification, suppression ou action sensible de la console et du lot 1 s'y ouvre (compte, profils, politique, messagerie, journalisation, institutions, pièces, cadrage, conducteur, attribution, blocage, livrable, fiche et dossier de changement, service supplémentaire, procès-verbal DES-11).
- **Administration visible** : section « Gestion des utilisateurs » en tête du tableau de bord de la console (super administrateur et RSSI), indicateurs cliquables vers `/console/comptes?etat=…`, création en modale ; fiche d'un compte en onglets Fiche, Sécurité, Sessions, Journal (`?onglet=`). Dans l'application du département, le RSSI (profil aligné sur `generer-realm-gimac.py`) a le groupe « Administration » de la navigation et la section « Administration et sécurité des comptes », liens vers la console : `VITE_URL_CONSOLE` (argument `URL_CONSOLE` du Dockerfile, `http://localhost:4202` par défaut).
- **Coquille et recherche globale** (D-70) : barre latérale sur toute la hauteur, barre supérieure et pied de page dans la colonne de contenu ; recherche globale (`RechercheGlobale` du socle sur la `CommandPalette` interactive du système de conception) ouverte par « Rechercher », Ctrl+K ou « / » : écrans permis, puis liaisons, institutions et conversations (`useSourcesRecherche`, `@gimac/lot1`) et comptes (`useSourceComptes`, `@gimac/administration`).
- **QrCode** promu au système de conception (dépendance `uqr`, tests et catalogue), employé par `@gimac/compte` pour la configuration du TOTP.
- **Portail** : une tâche de livrable mène toujours à `/portail/liaisons/:id/livrables/:op`, depuis le tableau de bord et le dossier (pièces « à déposer »).

## Mon compte : photo de profil, appareils connectés, historique (01/10/2026)

Contrat : `docs/lot1/mon-compte.md`.

- **Photo de profil** (`@gimac/compte`, carte « Photo de profil ») : choix d'une image dans la modale CRUD standard ; contrôles avant l'envoi aux mêmes codes que MS-IAM (type réel par les octets : PNG, JPEG, WebP, `IAM_AVATAR_TYPE` ; 2 Mo au plus, `IAM_AVATAR_TOO_LARGE` ; 64 à 4 096 px, `IAM_AVATAR_DIMENSIONS` ; fichier illisible, `IAM_AVATAR_UNREADABLE`), messages explicatifs en FR, EN, ES ; recadrage carré `AvatarCropper` (système de conception : glisser, flèches, Maj, + et −, 0 ; curseur de zoom ; aperçus à 96, 40 et 24 px) ; image recadrée calculée dans le navigateur depuis le fichier d'origine, orientation EXIF appliquée (`recadrerImage` : `createImageBitmap(fichier, { imageOrientation: 'from-image' })`, repli sur une image décodée ; 512 px, WebP sinon PNG ; MS-IAM ne lit pas l'orientation : une photo de téléphone n'arrive jamais tournée, scénario JPEG `Orientation = 6`) puis `PUT /bff/<couche>/iam/moi/avatar` (multipart `image`) ; retrait confirmé (`DELETE`). Lecture de l'image choisie en adresse `data:` (politique de sécurité du contenu : `img-src 'self' data: blob:`, `blob:` servant à l'aperçu des pièces jointes de la messagerie, D-68).
- **Avatar** (système de conception) : cercle (`gm-radius-avatar`, seule forme ronde), `src` avec repli sur les initiales si l'image manque ou échoue, tailles `xs` à `xl`. Adresse `urlAvatar(couche, id, version)` du socle : `/bff/<couche>/iam/avatars/{id}?v=<version>` (portail : `/bff/portail/iam/moi/avatar?v=`, seule photo qui lui est ouverte, §5), version issue de `/moi`, de l'annuaire et des comptes (`avatarVersion`). Employé dans le menu utilisateur de la barre du haut (nouveau menu ancré : avatar, nom, fonction, Mon compte, Se déconnecter), la liste et le fil des conversations, le choix des destinataires (`Option.avatar`, `Tag.avatar`), la liste et la fiche des comptes de la console. Services simulés : registre en mémoire de la page (`avatarsSimules`, photos de démonstration en SVG, dépôts de la démonstration), jamais employé avec les services réels.
- **Sécurité** : « Changer mon mot de passe » avec la case « Fermer mes autres sessions » (`fermerAutresSessions`) ; « Reconfigurer mon code TOTP » inchangé ; **Appareils connectés** (`GET /api/session/appareils` : appareil, navigateur, système, IP masquée, couche, ouverture, dernière activité ; « Cet appareil » ; `DELETE /api/session/appareils/{id}` et `DELETE /api/session/appareils`, confirmations en modale, `CNX_SESSION_COURANTE` et `CNX_APPAREIL_INCONNU` expliqués) remplace l'ancienne carte « Sessions ouvertes » ; **Historique des connexions** (`GET /api/session/historique?page=&taille=`, tableau paginé, résultat en `StatusBadge`, adresses masquées, `CNX_HISTORIQUE_INDISPONIBLE` expliqué).
- **Fond du fil de la messagerie** (D-66) : motif « trame » de §5.6 (classe `gm-trame-fil`, masque `assets/motif/trame-masque.svg`, jeton `gm-color-motif-fil`, 6 % en clair et 7 % en sombre) derrière les messages et l'état vide seulement ; bulles, ligne d'auteur, en-tête, saisie et liste sur fond plein ; retiré sous `prefers-reduced-transparency` et en couleurs forcées.

## Services réels ou simulés

Les services réels (HTTP, par la passerelle) sont le défaut : `VITE_SERVICES` absent ou `http`. Les services simulés
(données fictives, comptes de démonstration) ne sont retenus qu'avec `VITE_SERVICES=simule` : scénarios de bout en bout
(`e2e/construire.mjs`), démonstrations, image construite avec `--build-arg SERVICES=simule`. La constante est remplacée
à la construction ; `sideEffects` (`false` pour `@gimac/lot1`, feuilles de style seulement pour les applications) laisse
Rollup élaguer le simulateur, si bien qu'un paquet construit pour la production ne contient aucune donnée fictive.
`valider-frontend.sh` le vérifie sur les trois `dist`. Les tests Vitest des applications posent `VITE_SERVICES=simule`
(`vitest.config.ts`).

## Image

`Dockerfile` commun, argument `APPLICATION` (et `SERVICES`, `http` par défaut) : les manifestes `package.json` de tous les espaces de travail (racine, `packages/*`, `apps/*`, `e2e`) sont d'abord extraits à leur place pour `npm ci` (tout nouvel espace de travail est pris en compte sans modifier le fichier), puis construction avec Node 22 et service par `nginx-unprivileged` sur le port 8080, sous un utilisateur non root, en lecture seule. `nginx/gimac.conf.template` et `nginx/securite.conf` fixent la politique de sécurité du contenu, le cache et le relais vers la passerelle (`/api`, `/bff`, `/logout` ; plus de `/oauth2` ni de `/login` depuis D-65) ; les flux SSE (`/api/plateforme/flux`, `/bff/<couche>/live`) passent sans mise en tampon (HTTP/1.1, `proxy_buffering off`, délai de lecture de 24 h). `/sante` sert de sonde. Chaque couche est servie à la racine de son port ou de son hôte (4200, 4201, 4202 sous Docker Compose ; `departement`, `portail`, `console.des.gimac.local` sous Kubernetes) et route elle-même `/des`, `/portail` ou `/console`.

## Connexion, premier accès et sessions (D-65)

Contrat : `docs/lot1/connexion.md`. L'utilisateur ne voit jamais une page du fournisseur d'identité : la passerelle soumet les secrets à Keycloak (octroi direct, serveur à serveur) et pose le témoin de session HttpOnly.

- **Écran de connexion** (`PageConnexion`, socle), route `/connexion/*` de chaque couche, mosaïque TRA-01 conservée : identifiant et mot de passe (`POST /api/connexion`), puis code à six chiffres (`CODE_REQUIS`) ; lien « Mot de passe oublié ? » (`/connexion/mot-de-passe-oublie`, réponse identique que le compte existe ou non) et `/connexion/nouveau-mot-de-passe?jeton=` (rappel de la politique, violations de `IAM_PASSWORD_POLICY`). Messages par code (`CNX_IDENTIFIANTS_INVALIDES`, `CNX_COUCHE_REFUSEE`, `CNX_COMPTE_INACTIF`, `CNX_TROP_DE_TENTATIVES`, `CNX_JETON_INVALIDE`, `CNX_SESSION_EXPIREE`) en français, anglais et espagnol, identifiant de corrélation affiché. Retour à la page demandée (`?retour=`, chemins internes seulement). Le mot de passe ne vit que dans l'état du formulaire le temps du code, jamais dans un stockage, et il est effacé dès la réponse ou l'abandon.
- **Premier accès** (`GardePremierAcces`, `@gimac/compte`) : dans la session restreinte (`restreinte: true`, `acr` 1), l'assistant enchaîne mot de passe définitif (`POST /moi/mot-de-passe`), code TOTP (`POST /moi/second-facteur/totp/enrolement` puis `/totp` ; code QR calculé dans le navigateur par la bibliothèque embarquée `uqr`, sans service tiers ; secret affichable sur demande), fiche et charte (`POST /moi/premiere-configuration`), puis la connexion avec le nouveau mot de passe et un code, qui ouvre la session complète.
- **Mon compte** : toute modification (fiche, mot de passe, code TOTP, clés de sécurité, sessions) s'ouvre dans la modale CRUD standard du système de conception (`CrudDialog`), par l'adaptateur unique `packages/compte/src/composants/ModaleSaisie.tsx` ; une saisie modifiée demande confirmation avant l'abandon. Hors premier accès, la reconfiguration du code TOTP demande la clé de sécurité si MS-IAM l'exige (428, finalité `ACCOUNT_SENSITIVE`).
- **Sessions** (`SessionActive`, `SurveillanceInactivite`) : minuterie d'inactivité remise à zéro par la souris, le clavier, le défilement et le toucher, partagée entre onglets (`BroadcastChannel` « gimac-session ») ; durée lue sur `expireA` de `/api/session` (30 minutes à défaut, plafond non négociable) ; avertissement deux minutes avant l'échéance avec compte à rebours et « Rester connecté » (`POST /api/session/prolongation`) ; à l'échéance, déconnexion et retour à `/connexion?motif=inactivite` (« Session fermée après 30 minutes d'inactivité »). Une activité locale prolonge la session serveur au plus toutes les 5 minutes. Tout 401 hors route publique ramène à la connexion (`surNonAutorise`, motif `expiree` pour `CNX_SESSION_EXPIREE`). Les appels de fond (compteur de messages non lus) portent `X-Gimac-Fond: 1` et ne prolongent pas la session ; les flux SSE (`EventSource`) ne peuvent porter d'en-tête : la passerelle les reconnaît à leur chemin (`/bff/{couche}/live`, `/api/plateforme/flux`).
- **Services simulés** : `connexionSimulee` (socle) reproduit les états et les codes d'erreur sur des comptes de démonstration par profil (`apps/*/src/connexionDemo.ts`) et ouvre la session de démonstration de l'onglet (`sessionStorage`, clé `gm.sessionDemo` : identifiant, profil, premier accès ; aucun secret). Démonstration :

| Élément | Valeur |
|---|---|
| Mot de passe de tous les comptes | `Gimac-Demo-2026!` (`MOT_DE_PASSE_DEMO`) |
| Code à six chiffres (connexion, code TOTP) | tout code de six chiffres, par exemple `000000` |
| Console | `super.admin`, `rssi` ; `nouvel.admin` (premier accès) ; `compte.desactive` ; `correspondant-a` (couche refusée) |
| Application du département | un identifiant par profil interne (annuaire simulé, par exemple le chef de la DDRM) ; `nouvel.agent` (premier accès) ; `compte.desactive` ; `correspondant-a` (couche refusée) |
| Portail | `correspondant-a` ; `nouveau.correspondant` (premier accès) ; `compte.desactive` ; `super.admin` (couche refusée) |
| Trop de tentatives | cinq échecs en une minute pour un identifiant |
| Lien de réinitialisation | tout jeton base64url de 16 caractères au moins, une seule fois ; `expire` est refusé |

## Messagerie de travail (D-64)

Page `/des/messagerie` de l'application du département : conversations (la plus récente en tête,
non-lus en pastille, pagination), fil (liaisons citées en liens vers leur dossier, pièces jointes
téléchargeables), nouveau message (destinataires choisis dans l'annuaire de la plateforme, objet,
liaison rattachée, jusqu'à cinq pièces de 20 Mo), « Discuter de cette liaison » depuis le dossier.
Contrat : `docs/lot1/messagerie.md` ; service HTTP `/bff/des/collaboration/messagerie`, service simulé
aux mêmes refus (RG-52 : numéros de carte masqués). Le canal poussé (`/bff/des/live`, sujet
`collaboration`) rafraîchit la liste, le fil et le compteur de non-lus.

La même page sert la console (`/console/messagerie`, `couche="console"`) : tout membre interne, dont le RSSI
et le super administrateur, y a accès (D-64). Service HTTP `/bff/console/collaboration/messagerie`, canal
poussé `/bff/console/live` ; les liaisons citées s'y affichent en texte, sans citation ni lien vers le DES.
La configuration du serveur d'envoi des courriels est sous « Courriels de la plateforme »
(`/console/plateforme/courriels`).

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
| 2026-09-29 | — | Parcours de la liaison, socle (D-56 à D-59) : modèle des étapes 3 à 10, référentiel de l'annexe A du CR v1.4 (rangs 1 à 35, C1 à C10, M1 à M17, D1 à D34), matrice des 15 profils par fonction, ports `ServiceParcours`, `ServiceDecision` et `ServicePortailParcours` (HTTP : routes réelles de l'étape 3 ; simulé : liaisons fictives à chaque étape), hooks, vocabulaire FR, EN, ES ; profil de démonstration par `?profil=ROLE_…` |
| 2026-09-30 | 292c28c | Recalage après l'arrêt des agents : compilation, cloisonnement du tableau du participant, état simulé isolé par instance, clôture des rangs 21 et 22, secrets non cryptographiques en trace seule, onglet du circuit des quatre quitus retiré (D-56) |
| 2026-09-30 | 1a1ce09, 3ccf11f, 1025925 | Enveloppe fixe (barre supérieure et navigation latérale), navigation aux couleurs du GIMAC (logo et bande des États membres, entrée courante en vert), tableaux de bord épurés (« Mon travail », pilotage), pagination visible sur toute liste (DataTable), libellés unifiés |
| 2026-09-30 | d07c74d, 01701e2 | Messagerie de travail (D-64) : `ConversationList`, `MessageThread`, `MessageComposer` au système de conception ; `SearchSelect` et `MultiSelect` interactifs ; page `/des/messagerie`, pastille de non-lus ; canal poussé testé (sujet `collaboration`, parcours rafraîchi) ; douze profils de démonstration dérivés de la matrice des permissions |
| 2026-09-30 | 7d6c942 | Attribution des opérations : annuaire réel au lieu d'une route inexistante |
| 2026-09-30 | — | Coquille fixe (seule la zone de contenu défile), nom de couche retiré sous le logo, modale CRUD standard `CrudDialog` partout, gestion des utilisateurs visible (console et RSSI du département), liens de dépôt des livrables du portail, jetons de la navigation (`gm-inverse-a*`, `gm-cemac-*`), ports et sorties E2E paramétrables |
| 2026-09-30 | — | D-65 : connexion sur la plateforme (identifiant, mot de passe, code ; mot de passe oublié ; messages par code), premier accès avec code TOTP par code QR local et connexion finale, Mon compte en modales CRUD standard, fermeture des sessions après 30 minutes d'inactivité (avertissement, prolongation, onglets), appels de fond `X-Gimac-Fond`, services simulés de connexion ; relais `/oauth2` et `/login` retirés ; scénarios de bout en bout de connexion et ports dédiés |
| 2026-10-01 | — | Mon compte (docs/lot1/mon-compte.md) : photo de profil (choix contrôlé, recadrage `AvatarCropper`, envoi, retrait), `Avatar` avec photo et repli sur les initiales dans le menu utilisateur, la messagerie et la console ; case « Fermer mes autres sessions » ; appareils connectés et historique des connexions (services HTTP et simulés, hooks) ; fond texturé du fil de la messagerie (D-66) ; scénarios `mon-compte.spec.ts` |
| 2026-10-01 | — | Lien avec le backend : services réels par défaut, simulateur seulement avec `VITE_SERVICES=simule` et élagué des paquets de production ; gestes dédiés des verrous et rang 26 dans le simulateur, rang 34 dans l'onglet des opérations, remise de la fiche de suivi ouverte à `HOM_STEP_MANAGE`, référence `LIA-AAAA-NNN` du service, chronologie des référentiels ; contrat des routes vérifié en CI (`infra/ci/valider-contrat.sh`) |
| 2026-10-06 | — | Console, création de compte : erreurs de rattachement affichées sous le champ « Institution », aide précisée (FR, EN, ES), même règle dans le service simulé |
| 2026-10-06 | — | Graphiques de métriques (lot 1 de la supervision en graphiques) : Sparkline, tuile de mesure (StatCard `serie`), SerieTemporelle (axe du temps, trous, réticule et infobulle souris et clavier, « Autres », vue Tableau, export branché), BarresHorizontales ; jetons `gm-serie-1` à `4` et `gm-serie-autre` validés pour la vision des couleurs en clair et en sombre ; tests, couverture et catalogue |
