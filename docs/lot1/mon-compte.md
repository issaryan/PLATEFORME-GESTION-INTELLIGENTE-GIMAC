# « Mon compte » : photo de profil, mot de passe, appareils connectés (contrat)

Complète D-62 (administration depuis la plateforme) et D-65 (connexion et sessions sur la plateforme).
Fait foi pour MS-IAM, MS-GATEWAY, MS-COLLABORATION et le frontend (`@gimac/compte`, enveloppe, messagerie,
console). Erreurs en `application/problem+json` avec `code`.

## 1. Photo de profil (MS-IAM)

| Méthode et chemin (service) | Accès | Effet |
|---|---|---|
| PUT `/api/v1/moi/avatar` (multipart `image`) | titulaire, acr 2 | Remplace la photo ; 200 `{ "version": "<empreinte courte>" }` |
| DELETE `/api/v1/moi/avatar` | titulaire, acr 2 | Retire la photo (retour aux initiales) ; 204 |
| GET `/api/v1/avatars/{compteId}?v=<version>` | tout compte interne authentifié (et le titulaire) | Image `image/webp` (ou `image/png`), `Cache-Control: private, max-age=86400, immutable` ; 404 sans photo |

- Formats acceptés : PNG, JPEG, WebP (type réel vérifié par les octets, pas par l'extension) ; 2 Mo au plus ;
  dimensions entre 64 et 4 096 px. Refus : `IAM_AVATAR_TYPE`, `IAM_AVATAR_TOO_LARGE`, `IAM_AVATAR_DIMENSIONS`,
  `IAM_AVATAR_UNREADABLE`.
- Traitement : recadrage carré centré, redimensionnement à 256 × 256, réencodage (les métadonnées, dont
  EXIF et la position GPS, sont supprimées par le réencodage) ; conservation en base de MS-IAM (table
  `avatar_compte` : compte, image, type, version, mise à jour) ; jamais l'image d'origine.
- `GET /api/v1/moi` et l'annuaire (`/api/v1/interne/collaborateurs`, puis l'annuaire de la messagerie)
  ajoutent `"avatarVersion": "…" | null`. Le portail n'expose que l'avatar du titulaire.
- Journal d'administration et audit : `iam.avatar-changed.v1` (sans l'image).
- Passerelle : `/bff/{couche}/iam/moi/avatar` et `/bff/{des,console}/iam/avatars/**` (OPA : authentifié,
  acr 2 ; le portail n'a pas accès aux avatars des autres).

## 2. Mot de passe

Déjà en place (D-65) : `POST /api/v1/moi/mot-de-passe { actuel, nouveau }`. Ajout : champ facultatif
`"fermerAutresSessions": true` → la passerelle ferme les autres sessions du compte (§3) et MS-IAM ferme
les sessions Keycloak correspondantes. Courriel d'information au titulaire après tout changement.

## 3. Appareils connectés et historique des connexions (MS-GATEWAY, porteur des sessions)

| Méthode et chemin | Effet |
|---|---|
| GET `/api/session/appareils` | Sessions ouvertes du compte : `[{ "id", "courante": bool, "appareil": { "navigateur", "systeme", "type": "ORDINATEUR"\|"MOBILE"\|"TABLETTE"\|"INCONNU" }, "adresseIp" (masquée au dernier octet hors session courante), "couche", "ouverteLe", "derniereActivite", "expireA" }]` |
| DELETE `/api/session/appareils/{id}` | Ferme une autre session (révocation du jeton de rafraîchissement) ; 204 ; 409 `CNX_SESSION_COURANTE` pour la session courante (utiliser la déconnexion) |
| DELETE `/api/session/appareils` | Ferme toutes les autres sessions ; 204 |
| GET `/api/session/historique?page=0&taille=20` | Connexions du compte, les plus récentes d'abord : `{ "contenu": [{ "le", "resultat": "REUSSIE"\|"ECHEC_MOT_DE_PASSE"\|"ECHEC_CODE"\|"BLOQUEE", "appareil", "adresseIp", "couche" }], "total" }` (90 jours) |

- `id` est un identifiant opaque propre à la session (jamais l'identifiant de session du témoin).
- Appareil déduit de l'en-tête `User-Agent` à la connexion (analyse locale, sans service tiers).
- Historique persistant (base de la passerelle) alimenté à chaque tentative de connexion ; rétention 90 jours.
- Nouvel appareil (navigateur et système jamais vus pour ce compte) : courriel d'alerte au titulaire par
  MS-IAM (`POST /api/v1/interne/comptes/{id}/alerte-connexion`, jeton de service de la passerelle).

## 4. Interface (« Mon compte », enveloppe, messagerie, console)

- Photo : choix d'un fichier, aperçu recadré carré avant l'envoi, retrait ; avatar affiché dans le menu
  utilisateur de la barre du haut, les conversations et fils de la messagerie, le choix des destinataires,
  la liste et la fiche des comptes de la console ; initiales à défaut.
- Sécurité : « Changer mon mot de passe » (modale, case « fermer mes autres sessions »), « Reconfigurer
  mon code TOTP », « Appareils connectés » (session courante marquée, fermer une session ou toutes les
  autres, en modale de confirmation), « Historique des connexions » (paginé).
- Toutes ces actions en modale CRUD standard ; captures en clair et en sombre.
