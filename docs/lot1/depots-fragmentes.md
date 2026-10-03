# Dépôts fragmentés (claim check) et repartage par ticket (D-74)

Contrat entre le frontend, la passerelle, OPA, MS-DOCUMENT et MS-COLLABORATION pour déposer des fichiers
de plusieurs gigaoctets avec progression visible, puis les repartager sans copie.

## 1. Principe

- **Claim check** : le fichier ne voyage jamais dans le corps d'un message métier. L'émetteur le dépose
  d'abord dans MS-DOCUMENT, morceau par morceau, et reçoit un **ticket** : l'identifiant du document
  (`documentId`). Le message (ou tout autre acte métier) ne porte que ce ticket.
- **Morceaux de 16 Mio** envoyés l'un après l'autre : progression exacte côté émetteur, reprise après une
  coupure (les morceaux reçus sont connus du serveur), aucun fichier entier en mémoire, ni dans le
  navigateur ni dans les services.
- **Vérification serveur** : à la fin de l'envoi, MS-DOCUMENT relit l'envoi assemblé une seule fois et
  calcule son empreinte SHA-256 (jamais celle annoncée par le client), contrôle sa taille et, pour les
  fichiers d'au plus 20 Mo dont le texte s'extrait, l'absence de numéro de carte (BNF-16).
- **Déduplication** : si un document de même empreinte et de même taille existe déjà, le nouveau
  document (nouvelle ligne : sa finalité, sa référence, son déposant) pointe vers **le même objet** du
  dépôt WORM ; rien n'est réécrit. Le calcul étant fait par le serveur sur les octets reçus, un client ne
  peut pas obtenir un document qu'il ne possède pas en annonçant une empreinte.
- **Repartage** : transférer une pièce, c'est partager son ticket. La nouvelle pièce jointe référence le
  même `documentId`, donc le même emplacement en stockage. Aucune copie, aucun nouvel envoi.

## 2. Stockage (Ceph RGW, protocole S3)

| Compartiment | Verrouillage | Rôle |
|---|---|---|
| `gimac-depots` (nouveau, `gimac.stockage.s3.compartiment-depots`) | aucun | Envois en cours : téléversement multipart S3, puis objet assemblé le temps de la vérification. Règle de cycle de vie : objets expirés après 2 jours, téléversements incomplets abandonnés après 1 jour. |
| `gimac-documents` | COMPLIANCE (WORM) | Documents vérifiés, comme aujourd'hui. |

Vérification en une passe : l'objet assemblé de `gimac-depots` est lu en flux ; chaque tranche de 16 Mio
est écrite comme partie d'un téléversement multipart vers `gimac-documents` (avec `Content-MD5`, exigé par
le verrouillage d'objets), pendant que le SHA-256 est calculé. En fin de lecture : doublon trouvé →
`AbortMultipartUpload` (aucun objet créé, donc rien n'est retenu) ; sinon `CompleteMultipartUpload`.
L'objet de `gimac-depots` est ensuite supprimé. Clé WORM inchangée : `finalite/aaaa/mm/jj/documentId`.

La contrainte d'unicité de `depot_document.cle_objet` devient un index simple : plusieurs documents peuvent
partager un objet.

## 3. API de MS-DOCUMENT (jeton utilisateur, via la passerelle)

Chemins publics : `/bff/{des|console|portail}/document/depots/**`, réécrits en `/api/v1/depots/**`.
Session à deux facteurs. Corps d'une requête : 17 Mio au plus (`RequestSize` de la passerelle).

| Méthode | Chemin | Corps | Réponse |
|---|---|---|---|
| POST | `/api/v1/depots` | `{ "nomFichier", "typeMime", "taille", "finalite", "reference" }` | 201 `Depot` |
| PUT | `/api/v1/depots/{id}/parties/{numero}` | octets bruts (`application/octet-stream`), numéro à partir de 1 | 204 |
| GET | `/api/v1/depots/{id}` | | 200 `Depot` |
| POST | `/api/v1/depots/{id}/fin` | | 202 `Depot` (`EN_VERIFICATION`) |
| DELETE | `/api/v1/depots/{id}` | | 204 (abandon) |

`Depot` :

```json
{
  "id": "uuid du dépôt",
  "etat": "OUVERT | EN_VERIFICATION | DISPONIBLE | REJETE | ABANDONNE",
  "nomFichier": "atelier.mp4", "typeMime": "video/mp4", "taille": 4294967296,
  "taillePartie": 16777216, "nombreParties": 256, "partiesRecues": [1, 2, 3],
  "documentId": "uuid du document (ticket), seulement DISPONIBLE",
  "sha256": "64 hex, seulement DISPONIBLE",
  "dedoublonne": true,
  "motif": "code d'erreur, seulement REJETE",
  "expireLe": "instant ISO 8601"
}
```

Règles :

- `taille` : de 1 octet à `gimac.document.depot.taille-max-fragmente` (20 Go par défaut) ; sinon 413
  `DOC_FILE_TOO_LARGE`. `taillePartie` : 16 Mio (configurable, au moins 5 Mio, minimum S3).
- Permission selon la finalité : `MESSAGERIE` exige `COL_MESSAGE_SEND` ; les autres finalités exigent l'une
  des permissions du dépôt direct (`PAR_ADHESION_INSTRUCT`, `HOM_EVIDENCE_SUBMIT`, `HOM_STEP_MANAGE`,
  `CER_CHANGE_RECORD_LINK`, `HOM_PORTAL_SUBMIT`, `DOC_DEPOSIT`). Sinon 403.
- Seul le compte qui a ouvert le dépôt le lit, l'alimente, le termine ou l'abandonne (404 sinon, sans
  révéler l'existence du dépôt).
- Partie : taille exacte attendue (`taillePartie`, ou le reste pour la dernière), sinon 400
  `DOC_PART_SIZE`. Renvoyer une partie la remplace (reprise). Dépôt non `OUVERT` : 409 `DOC_UPLOAD_CLOSED`.
- Fin : toutes les parties reçues, sinon 409 `DOC_UPLOAD_INCOMPLETE` avec `details.manquantes`.
- Dépôt `OUVERT` sans activité depuis 24 h : `ABANDONNE`, téléversement S3 abandonné.
- Vérification : tâche de fond qui reprend les dépôts `EN_VERIFICATION` (y compris après un redémarrage).
  Échec : `REJETE` avec `motif` (`DOC_PAN_DETECTED`, `DOC_SIZE_MISMATCH`). Dépôt objet indisponible :
  nouvelle tentative plus tard, le dépôt reste `EN_VERIFICATION`.

## 4. API interne de MS-DOCUMENT (jeton de service)

- `GET /api/v1/interne/documents/{id}` → `{ "id", "nomFichier", "taille", "typeMime", "sha256",
  "finalite", "reference", "deposant", "institutionId", "deposeLe" }` (404 sinon). Même autorité que
  les autres lectures internes.
- `GET /api/v1/interne/documents/{id}/contenu` : servi **en flux** (plus de tableau d'octets en mémoire),
  `Content-Length`, `Repr-Digest`.

## 5. Messagerie (MS-COLLABORATION)

Le message (création de conversation ou envoi) accepte, en plus des parties `fichiers` (petits fichiers,
inchangé), dans sa partie JSON `message` :

- `documents` : tickets de documents déposés par l'émetteur via un dépôt fragmenté de finalité
  `MESSAGERIE`. Vérifiés auprès de MS-DOCUMENT (`deposant` = émetteur, finalité `MESSAGERIE`) ; sinon 400
  `COL_DOCUMENT_INVALID`.
- `transferts` : identifiants de pièces jointes existantes à repartager. L'émetteur doit être membre de la
  conversation d'origine de chaque pièce ; sinon 404 `COL_ATTACHMENT_NOT_FOUND`. La nouvelle pièce
  référence le même `documentId` (même objet).

Au plus 5 pièces par message, toutes sources confondues. Le téléchargement d'une pièce est relayé en
flux, quelle que soit sa taille.

## 6. Frontend

- `televerser(fichier, { couche, finalite, reference }, { surProgression, signal })` (socle) : ouvre le
  dépôt, envoie les parties une à une (`XMLHttpRequest`, progression octet par octet), reprend les parties
  manquantes après une erreur (trois essais par partie), termine, puis suit la vérification jusqu'à
  `DISPONIBLE` ; rend `{ documentId, sha256 }`. Abandon : `DELETE`.
- Messagerie : chaque fichier joint part dès qu'il est choisi, avec sa barre de progression (« Envoi
  45 % », « Vérification… », « Prêt ») et un bouton d'annulation ; « Envoyer » attend que tous soient
  prêts. Bouton « Transférer » sur chaque pièce du fil : choix d'une conversation, envoi du ticket.
- Pièces de plus de 50 Mo : pas d'aperçu ; téléchargement par lien direct (flux du navigateur), sans
  charger le fichier en mémoire.

## 7. Précisions de mise en œuvre

- Codes d'erreur ajoutés : 400 `DOC_SIZE_INVALID` (taille absente ou nulle), 400 `DOC_PART_NUMBER_INVALID`
  (numéro hors de 1 à `nombreParties`), 403 `DOC_PURPOSE_FORBIDDEN` (permission absente pour la finalité),
  404 `DOC_UPLOAD_NOT_FOUND` (dépôt inconnu ou d'un autre compte). Motifs de rejet ajoutés :
  `DOC_PDF_UNREADABLE`, `DOC_CONTENT_MISSING`.
- La vérification est une tâche planifiée (toutes les 5 s) qui réserve un dépôt pour une heure ; reprise
  après un arrêt : l'écriture WORM interrompue est abandonnée et recommencée, avec le même ticket.
- Rétention : MS-DOCUMENT lit au démarrage la rétention par défaut du compartiment WORM et la pose
  explicitement à l'ouverture de chaque écriture multipart (certaines passerelles S3 ne l'appliquent pas
  d'elles-mêmes aux objets assemblés). À contrôler sur Ceph RGW à la recette du socle.
- Un objet écrit par morceaux ne porte pas la métadonnée `x-amz-meta-sha256` (l'empreinte n'est connue
  qu'en fin d'écriture) : l'empreinte de référence est celle de `depot_document`.
- `GET /api/v1/interne/documents/{id}/contenu` vérifie l'empreinte pendant la lecture : un contenu altéré
  interrompt le flux avant ses derniers octets.
- Kubernetes : le compartiment `gimac-depots` est créé par l'exploitant (variable `GIMAC_S3_COMPARTIMENT_DEPOTS`).
