# Messagerie instantanée de travail (D-64) : contrat de service

Premier incrément de MS-COLLABORATION : messages directs et de groupe entre acteurs internes du GIMAC,
étiquettes de projet (liaisons), pièces jointes, annuaire, temps réel. Ce document fait foi pour le
backend (MS-COLLABORATION, MS-IAM, MS-DOCUMENT, MS-GATEWAY) et pour le frontend (`@gimac/lot1`).

## 1. Routes exposées

Chemin public (passerelle) : `/bff/des/collaboration/messagerie/**` réécrit en `/api/v1/messagerie/**`
sur MS-COLLABORATION (port 8093). Jeton utilisateur de niveau 2 (`acr` 2 ou 3) exigé sur tout `/api/v1/**`.

| Méthode | Chemin (service) | Permission | Objet |
|---|---|---|---|
| GET | `/api/v1/messagerie/annuaire?recherche=&page=0&taille=20` | `COL_MESSAGE_READ` | Personnes joignables : comptes internes actifs (MS-IAM), l'appelant exclu |
| GET | `/api/v1/messagerie/conversations?page=0&taille=20&recherche=&liaisonId=` | `COL_MESSAGE_READ` | Conversations dont l'appelant est membre, la plus récente d'abord |
| POST | `/api/v1/messagerie/conversations` (multipart) | `COL_MESSAGE_SEND` | Nouvelle conversation et premier message |
| GET | `/api/v1/messagerie/conversations/{id}` | `COL_MESSAGE_READ` | Une conversation (membre seulement, sinon 404) |
| GET | `/api/v1/messagerie/conversations/{id}/messages?avant={messageId}&taille=50` | `COL_MESSAGE_READ` | Messages, du plus ancien au plus récent, pagination vers le passé |
| POST | `/api/v1/messagerie/conversations/{id}/messages` (multipart) | `COL_MESSAGE_SEND` | Nouveau message |
| POST | `/api/v1/messagerie/conversations/{id}/lu` | `COL_MESSAGE_READ` | Marque lus tous les messages jusqu'au dernier |
| GET | `/api/v1/messagerie/non-lus` | `COL_MESSAGE_READ` | `{ "total": n }` |
| GET | `/api/v1/messagerie/pieces/{pieceId}` | `COL_MESSAGE_READ` | Contenu d'une pièce jointe (membre seulement), `Content-Disposition: attachment` |

Refus : 401 sans jeton ; 403 sans second facteur ou sans la permission ; 404 pour une conversation dont
l'appelant n'est pas membre (on ne révèle pas son existence).

### Corps multipart (création et envoi)

- `message` (partie JSON, `application/json`) :
  - création : `{ "destinataires": ["<id IAM>", …], "titre": "…" | null, "liaisonId": "<uuid>" | null, "contenu": "…", "etiquettes": [{ "liaisonId": "<uuid>", "reference": "LIA-2026-027" }] }`
  - envoi : `{ "contenu": "…", "etiquettes": [ … ] }`
- `fichiers` (0 à 5 parties) : pièces jointes.

Règles :
- `contenu` : 1 à 4 000 caractères (espaces retirés aux bords) ; au moins un contenu **ou** une pièce jointe.
- `destinataires` : 1 à 50 comptes internes actifs de l'annuaire, distincts de l'expéditeur ; deux personnes
  sans titre ni liaison forment une conversation **directe** : la conversation directe existante est réutilisée.
- `etiquettes` : 0 à 10, `reference` au format `^LIA-\d{4}-\d{3,6}$`.
- Pièces jointes : 5 au plus, 20 Mo chacune ; PDF, PNG, JPEG, texte, CSV, Word, Excel, PowerPoint, ZIP.
- **RG-52** : toute suite de 13 à 19 chiffres (espaces ou tirets admis) passant le contrôle de Luhn est
  masquée en `6 premiers + * + 4 derniers` avant l'enregistrement.
- Un message est **immuable** une fois envoyé.

Statuts : création `201` pour une nouvelle conversation, `200` quand la conversation directe existante
est réutilisée ; envoi `201` ; `/lu` répond `204`. La création accepte aussi `liaisonReference`
(facultatif) pour la liaison rattachée.

Codes d'erreur (`application/problem+json`, champ `code`) : `COL_RECIPIENT_REQUIRED`, `COL_RECIPIENT_UNKNOWN`,
`COL_RECIPIENT_SELF`, `COL_TOO_MANY_RECIPIENTS`, `COL_MESSAGE_EMPTY`, `COL_MESSAGE_TOO_LONG`,
`COL_TAG_INVALID`, `COL_TOO_MANY_TAGS`, `COL_ATTACHMENT_TOO_LARGE`, `COL_ATTACHMENT_TYPE`,
`COL_TOO_MANY_ATTACHMENTS`, `COL_CONVERSATION_NOT_FOUND`, `COL_ATTACHMENT_NOT_FOUND`,
`COL_MESSAGE_NOT_FOUND`, `COL_ATTACHMENT_REJECTED` (400, refus de MS-DOCUMENT, par exemple un numéro de
carte en clair), `COL_DIRECTORY_UNAVAILABLE` et `COL_DOCUMENT_UNAVAILABLE` (503), `COL_USER_TOKEN_REQUIRED`.

## 2. Représentations

```jsonc
// Personne de l'annuaire
{ "id": "…", "nom": "Paul Mbarga Atangana", "fonction": "Chef du DES", "profils": ["ROLE_CHEF_DES"] }

// Conversation (liste et détail)
{
  "id": "uuid", "type": "DIRECTE" | "GROUPE", "titre": "…" | null,
  "liaison": { "liaisonId": "uuid", "reference": "LIA-2026-027" } | null,
  "membres": [ { "id": "…", "nom": "…", "fonction": "…" } ],
  "dernierMessage": { "id": "uuid", "auteurNom": "…", "extrait": "120 premiers caractères", "envoyeLe": "ISO-8601", "piecesJointes": 1 } | null,
  "nonLus": 3, "misAJourLe": "ISO-8601", "creeeLe": "ISO-8601"
}

// Message
{
  "id": "uuid", "conversationId": "uuid",
  "auteur": { "id": "…", "nom": "…", "fonction": "…" }, "moi": true,
  "contenu": "…", "envoyeLe": "ISO-8601",
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
  `GET /api/v1/interne/documents/{id}/contenu` (`INTERNAL_DOCUMENT_READ`) : flux du document.

## 4. Événements (outbox, CloudEvents 1.0)

Sujet Kafka `collaboration.message`, type `collaboration.message-sent.v1`, clé = identifiant de conversation.
Donnée : `{ "conversationId", "messageId", "auteurId", "destinataires": ["<id>", …], "envoyeLe" }` ;
**aucun contenu de message** dans l'événement.

## 5. Temps réel

`GET /bff/{couche}/live?sujets=collaboration,homologation,certification&depuis=<id>` (passerelle, SSE,
cookie de session). Chaque événement : `{ "id", "type", "subject", "topic" }`. Les événements
`collaboration.message-sent.v1` ne sont remis qu'aux utilisateurs dont l'identifiant figure parmi les
`destinataires` ; le frontend invalide alors ses requêtes du sujet `collaboration` (liste, fil, non-lus).

## 6. Permissions (domaine Keycloak `gimac`)

`COL_MESSAGE_READ`, `COL_MESSAGE_SEND` : tous les profils internes (DDRM, chefs, conducteur, SNA,
production monétique, pôles, RSSI, conformité, DG, ingénieur, super administrateur). Jamais le
correspondant d'un participant (`ROLE_CORRESPONDANT`).
