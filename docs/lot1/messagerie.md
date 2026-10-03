# Messagerie instantanée de travail (D-64, D-68) : contrat de service

Premier incrément de MS-COLLABORATION : messages directs et de groupe entre acteurs internes du GIMAC,
étiquettes de projet (liaisons), pièces jointes, annuaire, temps réel ; groupes de discussion nommés,
administrés par leur créateur (D-68). Ce document fait foi pour le
backend (MS-COLLABORATION, MS-IAM, MS-DOCUMENT, MS-GATEWAY) et pour le frontend (`@gimac/lot1`).

## 1. Routes exposées

Chemins publics (passerelle) : `/bff/des/collaboration/messagerie/**` (application du département) et
`/bff/console/collaboration/messagerie/**` (console : RSSI, super administrateur), réécrits en
`/api/v1/messagerie/**` sur MS-COLLABORATION (port 8093). Jeton utilisateur de niveau 2 (`acr` 2 ou 3) exigé
sur tout `/api/v1/**`. Tout membre interne y a accès quel que soit son profil (D-64, 01/10/2026) ; OPA refuse
tout jeton portant une institution et tout chemin `/bff/portail/collaboration/**`.

| Méthode | Chemin (service) | Permission | Objet |
|---|---|---|---|
| GET | `/api/v1/messagerie/annuaire?recherche=&page=0&taille=20` | `COL_MESSAGE_READ` | Personnes joignables : comptes internes actifs (MS-IAM), l'appelant exclu |
| GET | `/api/v1/messagerie/conversations?page=0&taille=20&recherche=&liaisonId=` | `COL_MESSAGE_READ` | Conversations dont l'appelant est membre, la plus récente d'abord |
| POST | `/api/v1/messagerie/conversations` (multipart) | `COL_MESSAGE_SEND` | Nouvelle conversation et premier message |
| GET | `/api/v1/messagerie/conversations/{id}` | `COL_MESSAGE_READ` | Une conversation (membre seulement, sinon 404) |
| GET | `/api/v1/messagerie/conversations/{id}/messages?avant={messageId}&taille=50` | `COL_MESSAGE_READ` | Messages, du plus ancien au plus récent, pagination vers le passé |
| PATCH | `/api/v1/messagerie/conversations/{id}` (JSON `{ "titre": "…" }`) | `COL_MESSAGE_SEND` | Renomme un groupe (administrateur) ; renvoie la conversation |
| POST | `/api/v1/messagerie/conversations/{id}/membres` (JSON `{ "membres": ["<id IAM>", …] }`) | `COL_MESSAGE_SEND` | Ajoute des membres à un groupe (administrateur) ; renvoie la conversation |
| DELETE | `/api/v1/messagerie/conversations/{id}/membres/{membreId}` | `COL_MESSAGE_SEND` | Retire un membre (administrateur) ou quitte le groupe (`membreId` = l'appelant) ; `204` |
| POST | `/api/v1/messagerie/conversations/{id}/messages` (multipart) | `COL_MESSAGE_SEND` | Nouveau message |
| POST | `/api/v1/messagerie/conversations/{id}/lu` | `COL_MESSAGE_READ` | Marque lus tous les messages jusqu'au dernier |
| POST | `/api/v1/messagerie/conversations/{id}/saisie` | `COL_MESSAGE_SEND` | « En train d'écrire » (D-72) : signal éphémère aux autres membres, 204 ; 404 hors de la conversation |
| GET | `/api/v1/messagerie/non-lus` | `COL_MESSAGE_READ` | `{ "total": n }` |
| GET | `/api/v1/messagerie/pieces/{pieceId}` | `COL_MESSAGE_READ` | Contenu d'une pièce jointe (membre seulement), relayé en flux quelle que soit sa taille (D-74) : `Content-Disposition: attachment`, `Content-Length`, `Repr-Digest` |

Refus : 401 sans jeton ; 403 sans second facteur ou sans la permission ; 404 pour une conversation dont
l'appelant n'est pas membre (on ne révèle pas son existence).

### Corps multipart (création et envoi)

- `message` (partie JSON, `application/json`) :
  - création : `{ "destinataires": ["<id IAM>", …], "titre": "…" | null, "liaisonId": "<uuid>" | null, "contenu": "…", "etiquettes": [{ "liaisonId": "<uuid>", "reference": "LIA-2026-027" }], "groupe": false, "documents": ["<uuid>"], "transferts": ["<uuid>"] }`
  - envoi : `{ "contenu": "…", "etiquettes": [ … ], "documents": [ … ], "transferts": [ … ] }`
  - `documents` et `transferts` sont facultatifs (D-74, voir ci-dessous).
- `fichiers` (0 à 5 parties) : petites pièces jointes envoyées avec le message. Une requête sans aucune
  partie `fichiers` est admise (message ne portant que du texte, des tickets ou des transferts).

Règles :
- `contenu` : 1 à 4 000 caractères (espaces retirés aux bords) ; au moins un contenu **ou** une pièce jointe.
- `destinataires` : 1 à 50 comptes internes actifs de l'annuaire, distincts de l'expéditeur ; deux personnes
  sans titre ni liaison forment une conversation **directe** : la conversation directe existante est réutilisée.
- `etiquettes` : 0 à 10, `reference` au format `^LIA-\d{4}-\d{3,6}$`.
- Pièces jointes : **5 au plus par message, toutes sources confondues** (`fichiers`, `documents`,
  `transferts` ; un identifiant répété compte une fois), `COL_TOO_MANY_ATTACHMENTS`.
- `fichiers` : 20 Mo chacun (`COL_ATTACHMENT_TOO_LARGE`) ; PDF, PNG, JPEG, texte, CSV, Word, Excel,
  PowerPoint, ZIP (`COL_ATTACHMENT_TYPE`).
- **RG-52** : toute suite de 13 à 19 chiffres (espaces ou tirets admis) passant le contrôle de Luhn est
  masquée en `6 premiers + * + 4 derniers` avant l'enregistrement.
- Un message est **immuable** une fois envoyé.

### Gros fichiers et repartage (D-74, `docs/lot1/depots-fragmentes.md` §5)

- `documents` : **tickets** (identifiants de documents) obtenus par l'expéditeur au terme d'un dépôt
  fragmenté de finalité `MESSAGERIE` dans MS-DOCUMENT. Chaque ticket est vérifié par
  `GET /api/v1/interne/documents/{id}` : document existant, `finalite` = `MESSAGERIE`, `deposant` =
  l'expéditeur, empreinte SHA-256 présente. Sinon 400 `COL_DOCUMENT_INVALID`, dont les détails ne portent
  que le ticket fourni (rien n'est révélé du document d'un autre). La pièce reprend le nom, la taille, le
  type et l'empreinte des métadonnées ; rien n'est envoyé à nouveau.
- Les tickets n'ont pas de limite de taille propre à la messagerie (celle du dépôt fragmenté s'applique,
  20 Go par défaut). Formats admis, contrôlés sur l'extension du nom **et** sur le type des métadonnées
  (`application/octet-stream` admis seulement pour une extension de la liste ; type rendu canonique),
  sinon `COL_ATTACHMENT_TYPE` :

  | Famille | Extensions | Types |
  |---|---|---|
  | Pièces jointes ordinaires | `pdf`, `png`, `jpg`, `jpeg`, `txt`, `csv`, `doc`, `docx`, `xls`, `xlsx`, `ppt`, `pptx`, `zip` | ceux des `fichiers` |
  | Vidéo | `mp4`, `mov`, `webm`, `mkv`, `avi` | `video/mp4`, `video/quicktime`, `video/webm`, `video/x-matroska`, `video/x-msvideo` |
  | Audio | `mp3`, `wav`, `m4a` | `audio/mpeg`, `audio/wav` (ou `audio/x-wav`), `audio/mp4` |
  | Archives | `zip`, `7z`, `tar`, `gz` | `application/zip`, `application/x-7z-compressed`, `application/x-tar`, `application/gzip` |

  Le frontend contrôle l'extension avant d'ouvrir le dépôt, pour ne pas envoyer plusieurs gigaoctets
  refusés à l'envoi du message.
- `transferts` : identifiants de pièces jointes existantes à repartager. L'expéditeur doit être membre
  de la conversation d'origine de chaque pièce ; sinon 404 `COL_ATTACHMENT_NOT_FOUND` (pièce inconnue ou
  hors de portée, indistinctement). La nouvelle pièce référence le **même document** (même objet du
  dépôt WORM) avec le même nom, la même taille, le même type et la même empreinte : ni copie, ni nouveau
  dépôt.
- Tous les tickets et transferts sont vérifiés avant toute écriture : en cas de refus, ni conversation
  ni message n'est créé. L'événement `collaboration.message-sent.v1` est inchangé.

### Groupes de discussion (D-68)

- Création avec `"groupe": true` : `titre` obligatoire (1 à 200 caractères, `COL_GROUP_TITLE_REQUIRED`),
  premier message **facultatif** ; un groupe est toujours une nouvelle conversation de type `GROUPE`, même
  avec un seul destinataire. Le créateur en est l'**administrateur**.
- Seul l'administrateur renomme le groupe, ajoute ou retire des membres (`COL_GROUP_ADMIN_REQUIRED`, 403) ;
  tout membre peut le quitter. Les membres déjà présents sont ignorés à l'ajout ; 51 membres au plus,
  créateur compris (`COL_TOO_MANY_RECIPIENTS`). Une conversation directe ne se renomme pas et ne change pas
  de membres (`COL_NOT_A_GROUP`, 400) ; un membre absent du groupe répond `COL_MEMBER_NOT_FOUND` (404).
- Quand l'administrateur quitte le groupe, le membre le plus ancien le devient.
- Chaque changement est consigné au fil par un **message d'événement** (sans contenu) : `GROUPE_CREE`,
  `GROUPE_RENOMME`, `MEMBRES_AJOUTES`, `MEMBRE_RETIRE`, `MEMBRE_PARTI`, avec ses cibles (nouveau nom,
  noms des personnes ajoutées ou retirées). Un membre retiré ne voit plus la conversation (404).
- Une conversation rattachée à une liaison, même à deux, reste de type `GROUPE` sans nom propre, comme
  avant D-68 ; seuls les groupes nommés s'administrent depuis l'interface.

Statuts : création `201` pour une nouvelle conversation, `200` quand la conversation directe existante
est réutilisée ; envoi `201` ; `/lu` répond `204`. La création accepte aussi `liaisonReference`
(facultatif) pour la liaison rattachée.

Codes d'erreur (`application/problem+json`, champ `code`) : `COL_RECIPIENT_REQUIRED`, `COL_RECIPIENT_UNKNOWN`,
`COL_RECIPIENT_SELF`, `COL_TOO_MANY_RECIPIENTS`, `COL_MESSAGE_EMPTY`, `COL_MESSAGE_TOO_LONG`,
`COL_TAG_INVALID`, `COL_TOO_MANY_TAGS`, `COL_ATTACHMENT_TOO_LARGE`, `COL_ATTACHMENT_TYPE`,
`COL_TOO_MANY_ATTACHMENTS`, `COL_CONVERSATION_NOT_FOUND`, `COL_ATTACHMENT_NOT_FOUND`,
`COL_MESSAGE_NOT_FOUND`, `COL_DOCUMENT_INVALID` (400, ticket inconnu, d'un autre déposant ou d'une autre finalité), `COL_GROUP_TITLE_REQUIRED`, `COL_NOT_A_GROUP` (400), `COL_GROUP_ADMIN_REQUIRED` (403),
`COL_MEMBER_NOT_FOUND` (404), `COL_ATTACHMENT_REJECTED` (400, refus de MS-DOCUMENT, par exemple un numéro de
carte en clair), `COL_DIRECTORY_UNAVAILABLE` et `COL_DOCUMENT_UNAVAILABLE` (503), `COL_USER_TOKEN_REQUIRED`.

## 2. Représentations

```jsonc
// Personne de l'annuaire
{ "id": "…", "nom": "Paul Mbarga Atangana", "fonction": "Chef du DES", "profils": ["ROLE_CHEF_DES"] }

// Conversation (liste et détail)
{
  "id": "uuid", "type": "DIRECTE" | "GROUPE", "titre": "…" | null,
  "liaison": { "liaisonId": "uuid", "reference": "LIA-2026-027" } | null,
  "membres": [ { "id": "…", "nom": "…", "fonction": "…", "administrateur": false } ],
  "dernierMessage": { "id": "uuid", "auteurNom": "…", "extrait": "120 premiers caractères", "envoyeLe": "ISO-8601", "piecesJointes": 1,
                      "evenement": { "type": "MEMBRES_AJOUTES", "cibles": ["Rachel Biyong Mengue"] } | null } | null,
  "nonLus": 3, "misAJourLe": "ISO-8601", "creeeLe": "ISO-8601"
}

// Message
{
  "id": "uuid", "conversationId": "uuid",
  "auteur": { "id": "…", "nom": "…", "fonction": "…" }, "moi": true,
  "contenu": "…", "envoyeLe": "ISO-8601",
  "evenement": { "type": "GROUPE_CREE" | "GROUPE_RENOMME" | "MEMBRES_AJOUTES" | "MEMBRE_RETIRE" | "MEMBRE_PARTI", "cibles": ["…"] } | null,
  "etiquettes": [ { "liaisonId": "uuid", "reference": "LIA-2026-027" } ],
  "piecesJointes": [ { "id": "uuid", "nom": "rapport.pdf", "taille": 12345, "typeMime": "application/pdf", "empreinte": "sha256 hex" } ]
}
```

Pages : `{ "contenu": [...], "total": n, "page": p, "taille": t }` (annuaire et conversations) ;
messages : `{ "contenu": [...], "plusAnciens": true|false }`.

## 3. Dépendances entre services (jeton de service `client_credentials`, client `collaboration-service`)

- **MS-IAM** `GET /api/v1/interne/collaborateurs?recherche=&page=&taille=` (`INTERNAL_IAM_READ`) : comptes
  internes actifs (sans `institution_id`), filtrés par nom, courriel ou fonction ; `GET /api/v1/interne/collaborateurs/{id}`.
- **MS-DOCUMENT** `POST /api/v1/interne/documents` (multipart `fichier`, `finalite=MESSAGERIE`, `reference`,
  `deposantId`) (`INTERNAL_DOCUMENT_DEPOSIT`) → `{ id, sha256, nomFichier, taille, typeMime }` ; dépôt WORM,
  refus d'un fichier portant un numéro de carte en clair (BNF-16) ;
  `GET /api/v1/interne/documents/{id}/contenu` (`INTERNAL_DOCUMENT_READ`) : flux du document, servi et
  relayé en flux (`Content-Length`, `Repr-Digest`), sans tableau d'octets en mémoire (D-74) ;
  `GET /api/v1/interne/documents/{id}` (même autorité) : métadonnées `{ id, nomFichier, taille, typeMime,
  sha256, finalite, reference, deposant, institutionId, deposeLe }`, 404 si inconnu, pour vérifier les
  tickets (`docs/lot1/depots-fragmentes.md` §4).

## 4. Événements (outbox, CloudEvents 1.0)

Sujet Kafka `collaboration.message`, type `collaboration.message-sent.v1`, clé = identifiant de conversation.
Donnée : `{ "conversationId", "messageId", "auteurId", "destinataires": ["<id>", …], "envoyeLe" }` ;
**aucun contenu de message** dans l'événement. Les événements de groupe (D-68) passent par le même type ;
leurs `destinataires` comprennent aussi la personne retirée, pour que sa liste se mette à jour.

## 4 bis. Interface (frontend, D-68)

- **Annuaire** : le nouveau message et le nouveau groupe cherchent les personnes par nom, fonction ou
  service (recherche différée de 250 ms, photo et fonction affichées) ; un message direct vise une
  personne, un groupe plusieurs.
- **Groupe** : « Nouveau groupe » (nom et membres) ; « Membres et réglages » dans le fil (renommer, ajouter,
  retirer avec confirmation, quitter) ; les autres membres voient la liste en lecture seule.
- **Pièces jointes** : un clic ouvre une fenêtre d'aperçu (PDF dans un cadre, image, texte et CSV en clair ;
  autres formats : invitation à télécharger) avec un bouton « Télécharger ». L'en-tête CSP de nginx
  autorise à cet effet `frame-src 'self' blob:` et `img-src 'self' data: blob:`.

## 5. Temps réel

`GET /bff/{couche}/live?sujets=collaboration,homologation,certification&depuis=<id>` (passerelle, SSE,
cookie de session). Chaque événement : `{ "id", "type", "subject", "topic", "acteur" }`. Les événements
`collaboration.message-sent.v1` ne sont remis qu'aux utilisateurs dont l'identifiant figure parmi les
`destinataires` ; le frontend invalide alors ses requêtes du sujet `collaboration` (liste, fil, non-lus).
À défaut d'événement (relais coupé), la liste et le fil ouverts sont relus toutes les 5 s quand la page est
visible, en appels de fond (`X-Gimac-Fond: 1`).

Signaux éphémères (D-72), ni enregistrés ni rejoués (pas de ligne `id:`) :

| Événement | Origine | Remis à | `subject` / `acteur` |
|---|---|---|---|
| `collaboration.message-typing.v1` | `POST …/conversations/{id}/saisie` (MS-COLLABORATION, hors outbox) | autres membres de la conversation | conversation / compte qui écrit |
| `presence.account-online.v1` | passerelle, première connexion du compte au canal (DES, console) | tout abonné interne sauf le compte | compte / compte |
| `presence.account-offline.v1` | passerelle, 10 s après la fermeture de sa dernière connexion | tout abonné interne sauf le compte | compte / compte |

État initial de la présence : `GET /bff/{des|console}/presence` → `{ "enLigne": [ids], "vusLe": { id: instant } }`
(passerelle, session à deux facteurs ; 404 sur le portail). Un signal « écrit… » au plus toutes les 3 s côté
navigateur, toutes les 2 s par membre côté service ; affiché 6 s ou jusqu'au message de son auteur.

## 6. Permissions (domaine Keycloak `gimac`)

`COL_MESSAGE_READ`, `COL_MESSAGE_SEND` : tous les profils internes (DDRM, chefs, conducteur, SNA,
production monétique, pôles, RSSI, conformité, DG, ingénieur, super administrateur). Jamais le
correspondant d'un participant (`ROLE_CORRESPONDANT`).
