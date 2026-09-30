# Connexion et configuration des comptes sur la plateforme (D-65) : contrat

L'utilisateur ne voit jamais une page de Keycloak. Il se connecte, fait son premier accès, change son
mot de passe, configure son code TOTP et réinitialise un mot de passe oublié dans l'application de sa
couche (département, portail, console). Keycloak reste le moteur : il vérifie mot de passe et code,
applique la protection contre la force brute et émet les jetons, que seule la passerelle détient (BFF).
Ce document fait foi pour MS-GATEWAY, MS-IAM, le domaine Keycloak `gimac` et le frontend. Il complète
`docs/lot1/messagerie.md` pour la forme des erreurs (`application/problem+json`, champ `code`).

## 1. Parcours

```
Identifiant + mot de passe ──► POST /api/connexion
   ├─ 401 CNX_IDENTIFIANTS_INVALIDES          (message générique, rien sur la cause)
   ├─ 200 { etat: CODE_REQUIS }               compte configuré : l'écran demande le code TOTP
   │     └─ Identifiant + mot de passe + code ──► POST /api/connexion ──► 200 { etat: CONNECTE, session }
   └─ 200 { etat: PREMIER_ACCES, etapes }     session restreinte (acr 1) : assistant de premier accès
         mot de passe définitif, code TOTP (code QR), fiche, charte, via /bff/{couche}/iam/moi/**
         puis, avec le nouveau mot de passe et un code : POST /api/connexion ──► CONNECTE
Mot de passe oublié ──► POST /api/connexion/mot-de-passe-oublie ──► courriel de la plateforme
Lien du courriel (/connexion/nouveau-mot-de-passe?jeton=…) ──► POST /api/connexion/nouveau-mot-de-passe
```

Le navigateur garde le mot de passe en mémoire le temps de la saisie du code (jamais dans un stockage),
et l'efface dès la réponse `CONNECTE` ou l'abandon.

## 2. Passerelle (MS-GATEWAY) : `/api/connexion/**`, public, protégé contre la CSRF (témoin `XSRF-TOKEN`)

| Méthode et chemin | Corps | Réponses |
|---|---|---|
| POST `/api/connexion` | `{ "identifiant", "motDePasse", "code"?: "123456", "couche": "des" \| "portail" \| "console" }` | 200 `{ "etat": "CONNECTE", "session": SessionVue }` ; 200 `{ "etat": "CODE_REQUIS" }` ; 200 `{ "etat": "PREMIER_ACCES", "etapes": ["MOT_DE_PASSE", "SECOND_FACTEUR", "FICHE", "CHARTE"] }` ; 401 `CNX_IDENTIFIANTS_INVALIDES` ; 403 `CNX_COUCHE_REFUSEE` (correspondant sur le département ou la console, compte interne sur le portail) ; 403 `CNX_COMPTE_INACTIF` ; 429 `CNX_TROP_DE_TENTATIVES` |
| POST `/api/connexion/mot-de-passe-oublie` | `{ "identifiant" }` (identifiant ou adresse électronique) | 202 toujours (on ne révèle pas l'existence d'un compte) |
| POST `/api/connexion/nouveau-mot-de-passe` | `{ "jeton", "motDePasse" }` | 204 ; 400 `CNX_JETON_INVALIDE` (inconnu, expiré ou déjà utilisé) ; 400 `IAM_PASSWORD_POLICY` (politique de sécurité, détail dans `violations`) |
| GET `/api/session` | — | inchangé (`SessionVue`) ; ajoute `"restreinte": true` pendant le premier accès |
| POST `/api/logout` | — | inchangé : ferme la session Keycloak (révocation du jeton de rafraîchissement) et la session de la passerelle |

Déroulé de `POST /api/connexion` :
1. Limitation : 5 tentatives par minute et par adresse IP et par identifiant (429 au-delà), en plus de
   la protection contre la force brute de Keycloak.
2. Octroi direct `grant_type=password` sur le client **`gimac-connexion`** (flux mot de passe seul).
   Échec : 401 `CNX_IDENTIFIANTS_INVALIDES` (compte désactivé : 403 `CNX_COMPTE_INACTIF` si Keycloak le dit).
3. Couche : un jeton portant `institution_id` n'ouvre que le portail ; sans `institution_id`, que le
   département et la console (403 `CNX_COUCHE_REFUSEE`).
4. État du compte : `GET /api/v1/interne/comptes/{sub}/etat-connexion` sur MS-IAM (jeton de service du
   client `gateway-service`, permission `INTERNAL_IAM_READ`) → `{ "actif", "secondFacteur", "actionsRequises": [...] }`.
5. Si `actionsRequises` n'est pas vide ou si `secondFacteur` est faux : session **restreinte** avec le
   jeton de `gimac-connexion` (`acr` 1) et réponse `PREMIER_ACCES` (étapes = actions requises, plus
   `SECOND_FACTEUR` sans code configuré).
6. Sinon, sans `code` : `CODE_REQUIS`, aucune session. Avec `code` : octroi direct sur **`web_app`**
   (flux mot de passe et code obligatoire, `acr` 2) ; échec : 401 `CNX_IDENTIFIANTS_INVALIDES`.
7. Session : nouvel identifiant de session (contre la fixation), jetons d'accès, d'identité et de
   rafraîchissement enregistrés côté serveur comme pour une connexion OIDC (le relais `TokenRelay` et le
   rafraîchissement fonctionnent à l'identique), témoin HttpOnly, `Secure`, `SameSite=Lax`.
8. Journal d'audit : `gateway.login-succeeded.v1` / `gateway.login-failed.v1` (identifiant masqué,
   adresse IP, couche, cause technique), jamais le mot de passe ni le code.

Session restreinte : la passerelle et OPA n'y relaient que `GET|POST /bff/{couche}/iam/moi`,
`/bff/{couche}/iam/moi/premiere-configuration`, `/bff/{couche}/iam/moi/mot-de-passe` et
`/bff/{couche}/iam/moi/second-facteur/**` (règle `premier_acces` de `lot1_authz.rego`, `acr` 1 admis) ;
tout autre chemin répond 403. La redirection vers Keycloak (`/oauth2/authorization/**`,
`/login/oauth2/code/**`) est retirée : 404.

## 3. MS-IAM

### 3.1 Interne (jeton de service)
- `GET /api/v1/interne/comptes/{id}/etat-connexion` (`INTERNAL_IAM_READ`) → `{ "actif": bool,
  "secondFacteur": bool, "actionsRequises": ["MOT_DE_PASSE" | "SECOND_FACTEUR" | "FICHE" | "CHARTE" | "VERIFIER_COURRIEL"] }`.
- `POST /api/v1/interne/connexion/mot-de-passe-oublie` `{ "identifiant", "couche" }` → 202 ; envoie le
  courriel de réinitialisation si le compte existe et est actif.
- `POST /api/v1/interne/connexion/nouveau-mot-de-passe` `{ "jeton", "motDePasse" }` → 204 ou 400
  (`CNX_JETON_INVALIDE`, `IAM_PASSWORD_POLICY`).

### 3.2 Utilisateur (`/api/v1/moi/**`, `acr` 1 admis sur les seuls chemins ci-dessous pendant le premier accès)
- `GET /api/v1/moi` : inchangé, `etapesRestantes` suit les actions requises de la plateforme.
- `POST /api/v1/moi/mot-de-passe` `{ "actuel", "nouveau" }` → 204 ; 400 `IAM_PASSWORD_INVALID` (actuel
  faux, vérifié par octroi direct sur `gimac-connexion`), `IAM_PASSWORD_POLICY`, `IAM_PASSWORD_REUSED`.
  Retire l'action `MOT_DE_PASSE`.
- `POST /api/v1/moi/second-facteur/totp/enrolement` → `{ "enrolementId", "secret" (base32), "uri"
  (otpauth://totp/GIMAC%20DES:{identifiant}?secret=…&issuer=GIMAC%20DES&digits=6&period=30&algorithm=SHA1) }` ;
  secret gardé 10 min côté MS-IAM (chiffré), jamais journalisé.
- `POST /api/v1/moi/second-facteur/totp` `{ "enrolementId", "code", "libelle"? }` → 204 ; 400
  `IAM_TOTP_CODE_INVALID`, `IAM_TOTP_ENROLMENT_EXPIRED`. Enregistre le code dans Keycloak (extension
  `gimac-totp`), remplace un code existant, retire l'action `SECOND_FACTEUR`. Hors premier accès :
  réauthentification FIDO2 exigée si une clé est enrôlée (finalité `ACCOUNT_SENSITIVE`).
- `POST /api/v1/moi/premiere-configuration` : inchangé (fiche et charte), retire `FICHE` et `CHARTE`.

### 3.3 Administration (inchangée dans ses routes, changée dans ses effets)
- « Exiger des actions » écrit l'attribut `gimac_actions_requises`, jamais les actions requises de Keycloak.
- « Mot de passe provisoire » : mot de passe non temporaire pour Keycloak, plus l'action `MOT_DE_PASSE`.
- « Réinitialiser le second facteur » : suppression des identifiants OTP dans Keycloak, plus l'action `SECOND_FACTEUR`.
- Création et « invitation » : courriel de la plateforme (lien `/connexion/nouveau-mot-de-passe?jeton=`,
  24 h) au lieu de `execute-actions-email`.
- Jetons de lien : 32 octets aléatoires, base64url, conservés hachés (SHA-256) avec finalité, compte,
  échéance et date d'usage ; usage unique ; réinitialisation 30 min.
- Courriels envoyés par MS-IAM avec les paramètres de messagerie de la console (D-62), en fr/en/es selon
  la langue du compte ; lien construit sur l'adresse publique de la couche (`gimac.iam.liens.{des,portail,console}`).

## 4. Keycloak (domaine `gimac`, `infra/keycloak`)
- Flux `direct-gimac-mot-de-passe` : `direct-grant-validate-username`, `direct-grant-validate-password`.
- Flux `direct-gimac-deux-facteurs` : les deux mêmes, puis `direct-grant-validate-otp` **REQUIRED**.
- Client `gimac-connexion` : confidentiel, octroi direct seulement (flux mot de passe), secret
  `${GIMAC_SECRET_CONNEXION}`, jeton court (5 min), mappeur `acr` = « 1 », sans portée `acr` par défaut.
- Client `web_app` : confidentiel, octroi direct seulement (flux deux facteurs), `standardFlowEnabled`
  faux, mappeur `acr` = « 2 », sans portée `acr` par défaut.
- Client `gateway-service` : compte de service, `INTERNAL_IAM_READ`.
- Aucune action requise Keycloak (`CONFIGURE_TOTP`, `UPDATE_PASSWORD`…) posée sur les comptes.
- Extension `gimac-totp` (`infra/keycloak/extensions/gimac-totp`, SPI `RealmResourceProvider`, intégrée
  à l'image `Dockerfile`) : `POST /realms/gimac/gimac-totp/utilisateurs/{id}/totp` `{ "secret", "code",
  "libelle" }` vérifie le code puis crée l'identifiant OTP ; `DELETE …/totp` supprime les codes ;
  `GET …/totp` → `{ "configure": bool }`. Appel réservé au compte de service `iam-service` (rôle
  `manage-users` de `realm-management`) ; 401/403 sinon.
- Protection contre la force brute conservée (elle s'applique à l'octroi direct).

## 5. Frontend
- `PageConnexion` (socle) : formulaire identifiant et mot de passe, puis code à six chiffres ; lien
  « Mot de passe oublié ? » ; pages `/connexion/mot-de-passe-oublie` et `/connexion/nouveau-mot-de-passe`.
  Messages d'erreur par code (fr, en, es). Plus aucun bouton de redirection vers le fournisseur d'identité.
- Assistant de premier accès (`@gimac/compte`, `GardePremierAcces`) : étapes `MOT_DE_PASSE` (nouveau mot
  de passe avec rappel de la politique), `SECOND_FACTEUR` (code QR de l'URI `otpauth`, secret affichable,
  saisie d'un code de vérification), `FICHE`, `CHARTE` ; à la fin, reconnexion automatique avec le
  nouveau mot de passe et un code.
- « Mon compte » : changer de mot de passe, reconfigurer le code TOTP, dans des fenêtres modales.
- Services simulés : mêmes états (`CODE_REQUIS`, `PREMIER_ACCES`, `CONNECTE`) et mêmes codes d'erreur,
  comptes de démonstration par profil.

## 6. Sessions : fermeture après 30 minutes d'inactivité
- **Passerelle** : session serveur à inactivité maximale de 30 min (`gimac.session.inactivite-max: PT30M`) ;
  toute requête de l'utilisateur la prolonge, sauf les appels de fond marqués `X-Gimac-Fond: 1` (canal
  poussé `/bff/{couche}/live`, sondages des compteurs), qui ne comptent pas comme activité. Durée absolue
  maximale : 10 h (`gimac.session.duree-max: PT10H`). À l'échéance : jeton de rafraîchissement révoqué
  chez Keycloak, session supprimée, réponse 401 `CNX_SESSION_EXPIREE`.
- **Keycloak** : `ssoSessionIdleTimeout` 1 800 s, `ssoSessionMaxLifespan` 36 000 s, jeton d'accès 300 s
  (filet si la passerelle ne révoque pas).
- **`GET /api/session`** ajoute `"expireA"` (ISO-8601, fin d'inactivité prévue) ; **`POST /api/session/prolongation`** → 204
  (activité explicite, bouton « Rester connecté »).
- **Frontend** (socle) : minuterie d'inactivité (souris, clavier, défilement, toucher) ; à 28 min, fenêtre
  modale « Votre session va expirer » avec compte à rebours et « Rester connecté » ; à 30 min,
  déconnexion et retour à l'écran de connexion avec le message « Session fermée après 30 minutes
  d'inactivité ». Plusieurs onglets partagent l'activité (`BroadcastChannel`). Tout 401
  `CNX_SESSION_EXPIREE` ramène à la connexion.
- Valeur modifiable depuis la politique de sécurité de la console (entre 5 et 30 min, plancher non négociable : 30 min au plus).
