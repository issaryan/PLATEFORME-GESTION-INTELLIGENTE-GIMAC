<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-DOCUMENT : plateforme DES du GIMAC

Gestion documentaire probante : dépôt WORM (Ceph RGW, D-50), empreinte SHA-256, conservation légale, journal de consultation.

| Élément | Valeur |
|---|---|
| Domaine d'analyse (CA) | D1 |
| Épopée du carnet (CP v2.0) | DOC |
| Port | 8083 |
| Paquet Java | `cm.gimac.des.document` |
| Entités du JDL (7) | DocumentTypePolicy, DocumentLineage, Document, Consultation, PurgeRequest, IntegrityCheck, IntegrityMismatch |
| Règles de gestion (REC-GIMAC-01 §5) | RG-04, RG-05, RG-06, RG-16 |
| Modèle | `ms-document.jdl` |

## État

Squelette généré en JHipster 8.11.0 ; régénération en 9.4.0 prévue lors du passage au gabarit hexagonal (D-43). Code JHipster brut : domaine JPA, DTO MapStruct, services, ressources REST, changelogs Liquibase, messages fr et en. L'outbox, l'inbox et les algorithmes 6.17 à 6.20 du CC restent à écrire (SOC-01, SOC-05).

Écrit à la main, en arborescence hexagonale (`application`, `domain/service`, `infrastructure`), pour la tranche DDRM déjà livrée (D-60) : le **dépôt direct** d'un fichier et la **lecture de son empreinte** par les autres services ; pour la messagerie de MS-COLLABORATION (D-64) : le **dépôt par un service pour le compte d'un utilisateur** et la **lecture du contenu** d'un document (pièces jointes).

### Dépôt direct (tranche DDRM)

1. **Admission** : partie `fichier` présente, non vide, 20 Mo au plus (`gimac.document.depot.taille-max`, doublé par `spring.servlet.multipart.max-file-size`) ; `finalite` en code majuscule (ADHESION...) ; `reference` libre (par exemple `<institutionId>/<code>`), 255 caractères au plus ; nom de fichier réduit à son dernier segment ; type MIME normalisé (un contenu commençant par `%PDF-` est traité en PDF quel que soit le type déclaré).
2. **Contrôle de masquage (BNF-16, RG-52)** : le texte des fichiers texte (text/*, JSON, XML, CSV) et des PDF (PDFBox) est parcouru ; une suite de 13 à 19 chiffres vérifiant Luhn, contiguë ou groupée par quatre, fait refuser le dépôt (422 `DOC_PAN_DETECTED`, avec le nombre d'occurrences, jamais le numéro). Refus plutôt que masquage : réécrire la pièce changerait l'empreinte et altérerait un PDF signé. Un PDF illisible ou chiffré est refusé (422 `DOC_PDF_UNREADABLE`). Les images et documents bureautiques ne sont pas lus (reconnaissance de caractères hors périmètre, chaîne 6.17). La règle de Luhn est commune avec le masquage des journaux (`ControleNumerosCarte`).
3. **Empreinte SHA-256 calculée par le serveur**, en hexadécimal minuscule.
4. **Conservation WORM** (D-50) par le port `StockageObjet` : clé `<finalite>/<aaaa>/<mm>/<jj>/<id>`, empreinte en métadonnée `x-amz-meta-sha256`, `Content-MD5` vérifié par la passerelle. Adaptateur choisi par `gimac.adapters.stockage` : `real` (S3 par le SDK AWS vers Ceph RGW, profil prod ; démarrage refusé si le compartiment n'a pas le verrouillage d'objets, `gimac.stockage.s3.exiger-verrouillage`) ou `stub` (mémoire, dev et test). En Docker Compose, le service reçoit `GIMAC_S3_*` et attend la création des compartiments.
5. **Métadonnées** dans la table `depot_document` (changelog manuel `20260929120000_added_table_depot_document.xml`), en insertion seule : un déclencheur PostgreSQL refuse toute modification et toute suppression (RG-04, RG-16) ; empreinte au format canonique imposée par une contrainte. Table dédiée plutôt que l'entité `Document` du JDL, qui suppose la chaîne complète 6.17 (catalogue des types de pièces, lignée, antivirus) ; ces dépôts y seront repris avec cette chaîne.

### Interface interne de la messagerie (D-64)

MS-COLLABORATION (client `collaboration-service`, jeton `client_credentials`) conserve les pièces jointes des messages dans MS-DOCUMENT et n'en garde que l'identifiant et l'empreinte (contrat : `docs/lot1/messagerie.md` §3).

1. **Dépôt pour le compte d'un utilisateur** (`POST /api/v1/interne/documents`) : même cas d'utilisation que le dépôt direct (`DeposerDocumentUseCase`), donc mêmes contrôles (taille, finalité, référence, numéros de carte BNF-16, empreinte serveur, conservation WORM). Le déposant inscrit est l'utilisateur transmis dans `deposantId` (colonne `deposant`, 100 caractères au plus, 400 `DOC_DEPOSITOR_INVALID` sinon) et le client du service appelant (colonne `service_deposant`, changelog `20260930120000_added_column_depot_document_service_deposant.xml`).
2. **Lecture du contenu** (`GET /api/v1/interne/documents/{id}/contenu`, `LireDocumentUseCase`) : objet lu **en flux** dans le dépôt WORM (D-74 : jamais chargé en mémoire, quelle que soit sa taille). Objet absent ou d'une autre taille que la taille inscrite : 500 `DOC_CONTENT_MISSING` ou `DOC_INTEGRITY_MISMATCH` avant tout envoi, journalisé en erreur. L'empreinte SHA-256 est recalculée au fil de la lecture (`FluxVerifie`) et comparée à celle inscrite avant les derniers octets : un contenu altéré interrompt la réponse (le destinataire n'atteint jamais `Content-Length`) et l'écart est journalisé en erreur. Réponse : octets du document, `Content-Type` du type conservé, `Content-Length`, `Content-Disposition: attachment; filename*=UTF-8''<nom>` (RFC 8187), `Repr-Digest: sha-256=:<empreinte en base64>:` (RFC 9530) et `X-Content-Type-Options: nosniff`.
3. **Métadonnées** (`GET /api/v1/interne/documents/{id}`) : `{ id, nomFichier, taille, typeMime, sha256, finalite, reference, deposant, institutionId, deposeLe }`, pour vérifier un ticket de dépôt fragmenté (déposant, finalité) ; 404 `DOC_NOT_FOUND`.
4. **Journal** : chaque dépôt par un service et chaque lecture restituée sont journalisés (niveau INFO) avec l'identifiant du document, le client du service, la finalité et la taille ; jamais le contenu.

### Dépôts fragmentés (claim check, D-74)

Fichiers de plusieurs gigaoctets déposés par parties, avec reprise ; le client reçoit un **ticket** (l'identifiant du document), que les autres actes (message, transfert) référencent sans copie. Contrat : `docs/lot1/depots-fragmentes.md` §1 à §4.

1. **Ouverture** (`POST /api/v1/depots`, JSON `{ nomFichier, typeMime, taille, finalite, reference }`, `DepotFragmenteService`) : taille de 1 octet à `gimac.document.depot.taille-max-fragmente` (20 Go ; sinon 400 `DOC_SIZE_INVALID`, `DOC_FILE_EMPTY` ou 413 `DOC_FILE_TOO_LARGE`) ; finalité et référence comme au dépôt direct ; permission selon la finalité (`MESSAGERIE` : `COL_MESSAGE_SEND` ; `RAPPORT_ACTIVITE` (D-76) et `PROCESSUS_UNITE` (livrables des processus d'unité, référence = identifiant de l'instance, D-87) : `PIL_ACTIVITY_OWN` ; autres finalités : l'une des permissions du dépôt direct ; sinon 403 `DOC_PURPOSE_FORBIDDEN`). Un téléversement multipart est ouvert dans le compartiment des dépôts en cours (`gimac.stockage.s3.compartiment-depots`, `gimac-depots`, sans verrouillage d'objets ; règles de cycle de vie : objets expirés après 2 jours, téléversements incomplets abandonnés après 1 jour). Parties de `gimac.document.depot.taille-partie` (16 Mio, 5 Mio au moins, 10 000 parties au plus : vérifié au démarrage).
2. **Parties** (`PUT /api/v1/depots/{id}/parties/{numero}`, octets bruts `application/octet-stream`, numéro à partir de 1) : corps lu en flux et borné à la taille attendue (taille d'une partie, le reste pour la dernière ; sinon 400 `DOC_PART_SIZE` avec `details.attendue` et `details.recue`, sans rien envoyer), envoyé au dépôt objet avec son `Content-MD5`, puis inscrit avec son ETag (table `depot_fragmente_partie`). Renvoyer une partie la remplace (reprise). Dépôt non `OUVERT` : 409 `DOC_UPLOAD_CLOSED`. Seul le compte qui a ouvert le dépôt (sujet du jeton) le lit, l'alimente, le termine ou l'abandonne ; pour tout autre : 404 `DOC_UPLOAD_NOT_FOUND`, sans révéler son existence.
3. **Fin** (`POST /api/v1/depots/{id}/fin`, 202) : toutes les parties reçues (sinon 409 `DOC_UPLOAD_INCOMPLETE` avec `details.manquantes`), assemblage (`CompleteMultipartUpload`), état `EN_VERIFICATION`. Une fin renvoyée après une réponse perdue rend l'état courant. Réception d'une partie, fin et abandon verrouillent la ligne du dépôt (`SELECT … FOR UPDATE`) : une partie arrivée après la fin n'est jamais inscrite.
4. **Vérification** (`VerificationDepotService`, tâche planifiée toutes les `intervalle-verification`, 5 s) : l'objet assemblé est lu **une seule fois**, en flux ; chaque tranche de 16 Mio est écrite comme partie d'un téléversement multipart vers le dépôt WORM (`Content-MD5` par partie ; rétention par défaut du compartiment posée explicitement), pendant que le SHA-256 est calculé. Jamais plus d'une tranche en mémoire, sauf pour un fichier d'au plus `taille-max` (20 Mo), gardé pour le contrôle des numéros de carte (BNF-16, même extraction que le dépôt direct ; au-delà, le contrôle n'est pas appliqué et c'est journalisé). Type retenu : PDF si le contenu commence par `%PDF-`. Rejets (`REJETE`, `motif`) : `DOC_SIZE_MISMATCH`, `DOC_PAN_DETECTED`, `DOC_PDF_UNREADABLE`, `DOC_CONTENT_MISSING` (objet assemblé disparu) ; l'écriture WORM est alors abandonnée (aucun objet retenu).
5. **Déduplication** : un document de même empreinte et de même taille existe déjà (`depot_document`, index `ix_depot_document_sha256_taille`) : l'écriture WORM est abandonnée et le nouveau document (sa finalité, sa référence, son déposant) pointe vers le **même objet** (`cle_objet`, `version_objet`) ; `dedoublonne` vaut vrai. L'unicité de `depot_document.cle_objet` devient un index simple (changelog `20261002120000_added_table_depot_fragmente.xml`, nouveau changeset). Sinon l'objet est créé sous `<finalite>/<aaaa>/<mm>/<jj>/<documentId>` ; un objet écrit en flux ne porte pas `x-amz-meta-sha256` (empreinte connue en fin d'écriture seulement), elle est inscrite en base.
6. **Inscription** : document et état `DISPONIBLE` (ticket `documentId`, `sha256`) dans une même transaction, puis l'objet assemblé est supprimé.
7. **Reprise et instances** : un dépôt est pris par verrouillage optimiste (colonne `version`) et réservé pendant `bail-verification` (1 h) ; après un arrêt, il est repris à l'échéance, le téléversement WORM laissé en cours (`envoi_worm`) est abandonné et le même ticket est gardé. Dépôt objet indisponible : le dépôt reste `EN_VERIFICATION` et est repris après `delai-nouvelle-tentative` (1 min).
8. **Abandon** (`DELETE /api/v1/depots/{id}`, 204) : `ABANDONNE`, téléversement abandonné ; renvoyé, sans effet ; dépôt terminé : 409 `DOC_UPLOAD_CLOSED`. Tâche planifiée (`intervalle-expiration`, 10 min) : dépôt `OUVERT` sans activité depuis `inactivite` (24 h) abandonné de même.
9. **État** (`GET /api/v1/depots/{id}`) : `Depot` du contrat ; `documentId` et `sha256` seulement `DISPONIBLE`, `motif` seulement `REJETE`, `expireLe` (fin du délai d'inactivité) seulement `OUVERT`.

### Routes

| Méthode et chemin | Accès | Contrat |
|---|---|---|
| `POST /api/v1/documents` (multipart : `fichier`, `finalite`, `reference`) | jeton à deux facteurs et l'une de `PAR_ADHESION_INSTRUCT`, `HOM_EVIDENCE_SUBMIT`, `HOM_PORTAL_SUBMIT`, `DOC_DEPOSIT` | 201 `{ id, sha256, nomFichier, taille, typeMime }` ; 400 `DOC_FILE_MISSING`, `DOC_FILE_EMPTY`, `DOC_FILE_UNREADABLE`, `DOC_PURPOSE_INVALID`, `DOC_REFERENCE_INVALID` ; 403 `DOC_USER_TOKEN_REQUIRED` ; 413 `DOC_FILE_TOO_LARGE` ; 422 `DOC_PAN_DETECTED`, `DOC_PDF_UNREADABLE` ; 503 `DOC_STORAGE_UNAVAILABLE` |
| `GET /api/v1/interne/documents/{id}/empreinte` | jeton de service, `INTERNAL_DOCUMENT_READ` | 200 `{ sha256 }` ; 404 `DOC_NOT_FOUND` |
| `POST /api/v1/interne/documents` (multipart : `fichier`, `finalite`, `reference`, `deposantId`) | jeton de service, `INTERNAL_DOCUMENT_DEPOSIT` | 201 `{ id, sha256, nomFichier, taille, typeMime }` ; 400 `DOC_FILE_MISSING`, `DOC_FILE_EMPTY`, `DOC_FILE_UNREADABLE`, `DOC_PURPOSE_INVALID`, `DOC_REFERENCE_INVALID`, `DOC_DEPOSITOR_INVALID` ; 413 `DOC_FILE_TOO_LARGE` ; 422 `DOC_PAN_DETECTED`, `DOC_PDF_UNREADABLE` ; 503 `DOC_STORAGE_UNAVAILABLE` |
| `GET /api/v1/interne/documents/{id}/contenu` | jeton de service, `INTERNAL_DOCUMENT_READ` | 200 octets du document en flux (en-têtes ci-dessus) ; 404 `DOC_NOT_FOUND` ; 500 `DOC_CONTENT_MISSING`, `DOC_INTEGRITY_MISMATCH` ; 503 `DOC_STORAGE_UNAVAILABLE` |
| `GET /api/v1/interne/documents/{id}` | jeton de service, `INTERNAL_DOCUMENT_READ` | 200 `{ id, nomFichier, taille, typeMime, sha256, finalite, reference, deposant, institutionId, deposeLe }` ; 404 `DOC_NOT_FOUND` |
| `POST /api/v1/depots` (JSON : `nomFichier`, `typeMime`, `taille`, `finalite`, `reference`) | jeton à deux facteurs et l'une de `PAR_ADHESION_INSTRUCT`, `HOM_EVIDENCE_SUBMIT`, `HOM_STEP_MANAGE`, `CER_CHANGE_RECORD_LINK`, `HOM_PORTAL_SUBMIT`, `DOC_DEPOSIT`, `COL_MESSAGE_SEND` ; permission de la finalité | 201 `Depot` ; 400 `DOC_SIZE_INVALID`, `DOC_FILE_EMPTY`, `DOC_PURPOSE_INVALID`, `DOC_REFERENCE_INVALID` ; 403 `DOC_USER_TOKEN_REQUIRED`, `DOC_PURPOSE_FORBIDDEN` ; 413 `DOC_FILE_TOO_LARGE` ; 503 `DOC_STORAGE_UNAVAILABLE` |
| `PUT /api/v1/depots/{id}/parties/{numero}` (`application/octet-stream`) | idem ; déposant seulement | 204 ; 400 `DOC_PART_SIZE`, `DOC_PART_NUMBER_INVALID`, `DOC_FILE_UNREADABLE` ; 404 `DOC_UPLOAD_NOT_FOUND` ; 409 `DOC_UPLOAD_CLOSED` ; 415 autre type de contenu ; 503 `DOC_STORAGE_UNAVAILABLE` |
| `GET /api/v1/depots/{id}` | idem ; déposant seulement | 200 `Depot` ; 404 `DOC_UPLOAD_NOT_FOUND` |
| `POST /api/v1/depots/{id}/fin` | idem ; déposant seulement | 202 `Depot` (`EN_VERIFICATION`) ; 404 `DOC_UPLOAD_NOT_FOUND` ; 409 `DOC_UPLOAD_INCOMPLETE` (`details.manquantes`), `DOC_UPLOAD_CLOSED` ; 503 `DOC_STORAGE_UNAVAILABLE` |
| `DELETE /api/v1/depots/{id}` | idem ; déposant seulement | 204 ; 404 `DOC_UPLOAD_NOT_FOUND` ; 409 `DOC_UPLOAD_CLOSED` |
| `POST /api/v1/apercus` (octets du fichier, en-tête `X-Nom-Fichier` encodé en URI) | jeton à deux facteurs, `PIL_ACTIVITY_OWN`, compte interne (aucune institution) | 200 `application/pdf`, `Content-Disposition: inline`, `Cache-Control: private, max-age=3600` ; 403 `DOC_USER_TOKEN_REQUIRED`, `DOC_APERCU_RESERVE_INTERNE` ; 413 `DOC_APERCU_TROP_VOLUMINEUX` ; 415 `DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE` ; 422 `DOC_APERCU_ILLISIBLE` ; 429 `DOC_APERCU_LIMITE` ; 503 `DOC_APERCU_INDISPONIBLE` |

Erreurs en ProblemDetail RFC 9457 (`code` DOC_*, `message`, `details`, `correlationId` repris de l'en-tête `X-Correlation-Id` s'il est présent). Exposition par la passerelle : `POST /bff/{des|portail}/document/documents` ; `/bff/{des|console|portail}/document/depots/**` réécrit en `/api/v1/depots/**` (OPA : permissions ci-dessus depuis le DES et la console, `HOM_PORTAL_SUBMIT` et institution depuis le portail ; corps de 17 Mio au plus) ; `POST /bff/{des|console}/document/apercus` réécrit en `/api/v1/apercus` (OPA : `PIL_ACTIVITY_OWN`, comptes internes ; corps de 20 Mo au plus, délai de réponse de 90 s) ; l'interface interne n'est pas exposée.

### Aperçu des documents bureautiques (D-83)

Contrat : `docs/lot1/apercu-bureautique.md`. `ApercuResource` lit le corps (au plus `gimac.apercu.taille-max` + 1 octets, 20 Mo), `ApercuBureautiqueService` contrôle la taille, l'extension et la signature (`SignatureBureautique` : ZIP pour Open XML et OpenDocument, OLE2 pour doc, ppt, xls et pour un Open XML protégé par mot de passe, `{\rtf` pour rtf), sert le PDF du cache (Caffeine, clé : empreinte SHA-256 et extension, 24 h, 500 Mo) ou réserve une conversion (30 par minute et par compte, fenêtre glissante) et appelle le port `ConvertisseurApercu`. Adaptateur `ConvertisseurApercuGotenberg` : `POST {gimac.apercu.url}/forms/libreoffice/convert`, partie `files` nommée `document.<extension>` (le nom d'origine ne quitte pas le service) ; 400, 422 ou 500 du moteur : 422 `DOC_APERCU_ILLISIBLE` ; autre statut, délai (`gimac.apercu.delai`, 65 s) ou connexion impossible : 503 `DOC_APERCU_INDISPONIBLE`. Journal : compte, taille, extension (admise, sinon `?`), durée, résultat.

### Sécurité (D-61)

- `/api/v1/interne/**` : **jeton de service uniquement** et permission interne **route par route** (`ServiceTokenAuthorizationManager`) : `POST /api/v1/interne/documents` exige `INTERNAL_DOCUMENT_DEPOSIT`, `GET …/{id}`, `GET …/{id}/empreinte` et `GET …/{id}/contenu` exigent `INTERNAL_DOCUMENT_READ` ; tout autre chemin interne est refusé. Un jeton de service se reconnaît à son compte de service Keycloak (`preferred_username` = `service-account-<client>`, où `<client>` est `azp`, à défaut `client_id`) ; un jeton utilisateur est refusé (403), même porteur d'une permission interne ; `/api/v1/portail/**` : institution obligatoire dans le jeton (RG-59) ; `/api/v1/**` : jeton issu d'une **authentification à deux facteurs** (claim `acr` parmi `gimac.securite.double-facteur.niveaux-acr`, 2 et 3 par défaut), puis `@PreAuthorize` route par route ; `/api/**` généré par JHipster : `ROLE_ADMIN`.
- `SecurityUtils` accepte comme autorités les rôles `ROLE_*` et les permissions en majuscules (`^[A-Z][A-Z0-9]*(_[A-Z0-9]+)+$`).

## Tests : définition de fini (D-61)

| Type | Où | Commande |
|---|---|---|
| Unitaires (empreinte, contrôle de Luhn, validations, cas d'utilisation, adaptateur mémoire, adaptateur S3 avec client simulé, extraction de texte, décisions d'accès, erreurs ; dépôts fragmentés : `DepotFragmenteServiceTest` et `VerificationDepotServiceTest` sur le substitut multipart et une base simulée à horloge réglable, déduplication, rejets, reprise après une panne ou un arrêt, instance concurrente, expiration ; `StockageObjetMemoireMultipartTest`, `StockageObjetS3MultipartTest`, `FluxVerifieTest`) | `*Test` | `./mvnw test` |
| Sécurité (401 sans jeton, 403 sans second facteur, 403 sans permission, accès avec chacune des quatre permissions ; interface interne : 401 sans jeton, 403 pour un jeton utilisateur, 403 pour un jeton de service sans la permission de la route, accès avec elle, autres chemins internes refusés) | `DocumentResourceSecurityTest`, `DepotFragmenteResourceTest` (dépôts fragmentés : mêmes refus sur chaque route, accès avec chacune des sept permissions, représentations et codes du contrat) (`@WebMvcTest`, vraie chaîne de filtres) | `./mvnw test` |
| Intégration (PostgreSQL, Redis, Kafka et passerelle S3 par Testcontainers) | `DepotDocumentIT`, `DocumentInterneIT` (dépôt interne puis relecture sur compartiment WORM : octets et empreinte identiques, refus BNF-16, taille maximale, document inconnu, jetons refusés), `DepotFragmenteIT` (dépôt fragmenté de bout en bout sur PostgreSQL et passerelle S3 réelle : parties dans le désordre et renvoyées, fin incomplète, vérification, métadonnées et contenu en flux, déduplication, rejet BNF-16, accès), `StockageObjetS3IT`, `StockageObjetS3MultipartIT` (seulement si `GIMAC_S3_TEST_ENDPOINT` désigne une passerelle S3, par exemple Ceph RGW) et les `*IT` générés | `./mvnw verify` |
| Recette (scénario de la DDRM ; le service n'a pas de Cucumber) | `RecetteDepotDocumentIT`, adaptateur S3 réel | `./mvnw verify` |

JaCoCo bloque la construction : à `test`, chaque classe écrite pour le dépôt à 80 % en lignes et en méthodes, l'ensemble à 80 % en branches (`seuil-unitaire-du-lot`) ; à `verify`, tout le service, unitaires et intégration fusionnés, à 80 % en lignes et en fonctions (méthodes), hors implémentations MapStruct produites à la compilation (`seuil-global-du-service`, même règle que MS-PARTICIPANT et MS-IAM, D-61). Mesure du 29/09/2026 : code écrit pour le dépôt couvert à 100 % des lignes et 94,4 % des branches ; tout le service, hors MapStruct, à 86,1 % des lignes et 84,8 % des méthodes.

Les tests de l'adaptateur réel utilisent une passerelle S3 SeaweedFS (image Docker Hub légère) : ils éprouvent le protocole S3 de l'adaptateur ; le mode conformité de Ceph reste vérifié par `tester-socle.sh` (D-50). Testcontainers est porté à 1.21.4 : la version 1.20.6 gérée par Spring Boot 3.4.5 parle l'API Docker 1.32, refusée par Docker Engine 29.

## Lancer

- Service seul, en développement : `./mvnw` (dépendances : `docker compose -f src/main/docker/services.yml up -d`).
- Avec toute la plateforme : `infra/docker-compose/gimac-dev.sh demarrer` à la racine du dépôt.
- Image de conteneur (Jib, sans Dockerfile, D-48) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Code repris du projet afriland (D-52)

Copié et adapté (paquet `cm.gimac.des.document`) ; chaque bean est inactif tant que sa propriété ne vaut pas `true`.

| Classe | Origine dans afriland | Propriété d'activation |
|---|---|---|
| `config/DecodeurJwtInterne` | `audit-service/src/main/java/com/afriland/audit/config/DecodeurJwtInterne.java` | sans bean à activer |
| `config/KafkaErrorHandlingConfiguration` | `audit-service/src/main/java/com/afriland/audit/config/KafkaErrorHandlingConfiguration.java` | `gimac.kafka.rebut.actif` |
| `web/filter/TenantAndCorrelationFilter` | `documents-service/src/main/java/com/afriland/documents/web/filter/TenantAndCorrelationFilter.java` | `gimac.document.correlation.actif` |
| `service/util/DocumentMerger` | `documents-service/src/main/java/com/afriland/documents/service/util/DocumentMerger.java` | `gimac.document.fusion-pdf.actif` |
| `service/util/ByteArrayMultipartFile` | `documents-service/src/main/java/com/afriland/documents/service/util/ByteArrayMultipartFile.java` | sans bean à activer |
| `infrastructure/logging/PiiMaskingLogConverter` | `notifications-service/src/main/java/com/afriland/notifications/infrastructure/logging/PiiMaskingLogConverter.java` | sans bean à activer |
| `adapter/outbound/securite/ServiceTokenProvider` | `administration-service/src/main/java/com/afriland/admin/adapter/outbound/cognitive/ServiceTokenProvider.java` | `gimac.securite.jeton-service.actif` |

Écarts comblés (D-53) : `ValidateurLiaisonCertificat` et option du décodeur exigeant la liaison au certificat (RFC 8705) ; masquage des journaux limité aux numéros vérifiant Luhn, en 6+4 (RG-52).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 6c7eb44 | Génération JHipster 8.11.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 6 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 1 classes reprises d'afriland : ServiceTokenProvider (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 78 tests unitaires et 355 tests d'intégration réussis, image Jib construite ; test généré `DocumentTypePolicyResourceIT` corrigé (valeur de rétention) (D-54). |
| 2026-09-29 | — | Tranche DDRM : `POST /api/v1/documents` (dépôt WORM par le port `StockageObjet`, adaptateurs S3 et mémoire, empreinte SHA-256 serveur, refus des numéros de carte BNF-16, table `depot_document` en insertion seule) et `GET /api/v1/interne/documents/{id}/empreinte` ; sécurité D-61 (double facteur, permissions en majuscules, `/api/**` généré réservé à `ROLE_ADMIN`) ; 182 tests unitaires et 369 tests d'intégration réussis ; seuils JaCoCo (unitaire 80 %, global au niveau mesuré) ; Testcontainers 1.21.4 (Docker 29). |
| 2026-09-29 | — | Ports sortants `application/port/out` versionnés : la règle `out/` du `.gitignore` généré par JHipster les ignorait, le dépôt distant ne compilait pas ; exception `!src/**/out/` ajoutée et ports `StockageObjet`, `ExtracteurTexte` reconstitués d'après leurs adaptateurs |
| 2026-09-30 | — | Messagerie (D-64) : interface interne pour MS-COLLABORATION, `POST /api/v1/interne/documents` (`INTERNAL_DOCUMENT_DEPOSIT`, dépôt pour le compte de `deposantId` par le même cas d'utilisation que le dépôt direct, service déposant inscrit dans la nouvelle colonne `service_deposant`) et `GET /api/v1/interne/documents/{id}/contenu` (`INTERNAL_DOCUMENT_READ`, empreinte revérifiée, `Repr-Digest`, lecture journalisée) ; `/api/v1/interne/**` réservé aux jetons de service avec permission interne route par route, tout autre chemin interne refusé ; 217 tests unitaires et 374 tests d'intégration réussis, seuils JaCoCo tenus (code écrit pour le dépôt : 100 % des lignes, 96,1 % des branches en unitaire). |
| 2026-10-02 | - | Dépôts fragmentés (claim check, D-74) : `POST/GET/DELETE /api/v1/depots`, `PUT …/{id}/parties/{numero}`, `POST …/{id}/fin` ; tables `depot_fragmente` et `depot_fragmente_partie`, unicité de `depot_document.cle_objet` remplacée par un index simple (changelog `20261002120000_added_table_depot_fragmente.xml`) ; port `StockageObjet` étendu (téléversements multipart du compartiment `gimac-depots`, écriture WORM en flux avec Content-MD5 par partie et rétention explicite, lectures en flux), substitut mémoire fidèle aux règles multipart de S3 ; vérification en tâche de fond (empreinte, taille, BNF-16 jusqu'à 20 Mo, déduplication, reprise) ; `GET /api/v1/interne/documents/{id}` (métadonnées) et contenu servi en flux, vérifié au fil de la lecture ; 299 tests unitaires et 383 tests d'intégration réussis (5 ignorés sans `GIMAC_S3_TEST_ENDPOINT`), seuils JaCoCo tenus. |
| 2026-10-20 | D-116 | Finalité de dépôt `RAPPORT_MONITORING` (MON_SUPERVISION_READ) pour les rapports de monitoring et d'astreinte |
| 2026-10-22 | D-120 | US-EXI-08 : `POST /api/v1/interne/pieces-sandbox`, `GET /api/v1/interne/documents/empreintes/{sha256}`, `/api/v1/sandbox/types-pieces` et `/quarantaines`, tables `sbx_type_piece`, `sbx_piece`, outbox `sbx_evenement_sortant`, règle JaCoCo à 98 % |
| 2026-10-29 | — | Supervision : registre `micrometer-registry-prometheus` aligné sur Spring Boot 4 ; `/actuator/prometheus` vérifié en 200 avec CA et certificat client mTLS ; autres endpoints Actuator toujours réservés à `ROLE_ADMIN` |
| 2026-11-02 | — | Profil `prod` : `spring.docker.compose.enabled: false`, aucune tentative de démarrer `postgresql.yml` dans le conteneur ; image reconstruite avec la consultation par empreinte de D-120 (`GET /api/v1/interne/documents/empreintes/{sha256}`) |
| 2026-11-06 | D-139 | Gabarit commun des PDF : champ facultatif `gabarit` de `POST /api/v1/interne/rendus/pdf` (titre, surtitre, sous-titre, référence, classification PUBLIC, INTERNE ou CONFIDENTIEL, langue fr, en, es, date d'édition, filigrane, données clés, style), `GabaritMoabi` (en-tête et pied de l'organigramme, ressources de la charte sous `resources/gabarit/`), métadonnées du PDF (champ `metadata` de Gotenberg) ; requêtes sans gabarit inchangées. |
| 2026-11-06 | D-139, complément | Filigrane réduit sur papier étroit (A4 portrait) ; impression réelle des requêtes `*.json` d'un dossier par `ConvertisseurHtmlGotenbergReelTest` (GIMAC_PDF_ESSAI_DOSSIER), source des exemples du frontend. |
| 2026-11-07 | Exploitation | Binding `kafkaProducer-out-0` du gabarit JHipster configuré avec `ByteArraySerializer`, comme MS-PARTICIPANT : `spring.kafka.producer` (StringSerializer) lui faisait lever `SerializationException` à chaque envoi, soit une `KafkaSendFailureException` par seconde dans le journal depuis le 06/11. |
<!-- GIMAC:FIN -->

# document

This application was generated using JHipster 8.11.0, you can find documentation and help at [https://www.jhipster.tech/documentation-archive/v8.11.0](https://www.jhipster.tech/documentation-archive/v8.11.0).

This is a "microservice" application intended to be part of a microservice architecture, please refer to the [Doing microservices with JHipster][] page of the documentation for more information.

## Project Structure

Node is required for generation and recommended for development. `package.json` is always generated for a better development experience with prettier, commit hooks, scripts and so on.

In the project root, JHipster generates configuration files for tools like git, prettier, eslint, husky, and others that are well known and you can find references in the web.

`/src/*` structure follows default Java structure.

- `.yo-rc.json` - Yeoman configuration file
  JHipster configuration is stored in this file at `generator-jhipster` key. You may find `generator-jhipster-*` for specific blueprints configuration.
- `.yo-resolve` (optional) - Yeoman conflict resolver
  Allows to use a specific action when conflicts are found skipping prompts for files that matches a pattern. Each line should match `[pattern] [action]` with pattern been a [Minimatch](https://github.com/isaacs/minimatch#minimatch) pattern and action been one of skip (default if omitted) or force. Lines starting with `#` are considered comments and are ignored.
- `.jhipster/*.json` - JHipster entity configuration files
- `/src/main/docker` - Docker configurations for the application and services that the application depends on

## Development

To start your application in the dev profile, run:

```
./mvnw
```

For further instructions on how to develop with JHipster, have a look at [Using JHipster in development][].

### OAuth 2.0 / OpenID Connect

Congratulations! You've selected an excellent way to secure your JHipster application. If you're not sure what OAuth and OpenID Connect (OIDC) are, please see [What the Heck is OAuth?](https://developer.okta.com/blog/2017/06/21/what-the-heck-is-oauth)

To log in to your app, you'll need to have [Keycloak](https://keycloak.org) up and running. The JHipster Team has created a Docker container for you that has the default users and roles. Start Keycloak using the following command.

```
docker compose -f src/main/docker/keycloak.yml up
```

The security settings in `src/main/resources/config/application.yml` are configured for this image.

```yaml
spring:
  ...
  security:
    oauth2:
      client:
        provider:
          oidc:
            issuer-uri: http://localhost:9080/realms/jhipster
        registration:
          oidc:
            client-id: web_app
            client-secret: web_app
            scope: openid,profile,email
```

Some of Keycloak configuration is now done in build time and the other part before running the app, here is the [list](https://www.keycloak.org/server/all-config) of all build and configuration options.

Before moving to production, please make sure to follow this [guide](https://www.keycloak.org/server/configuration) for better security and performance.

Also, you should never use `start-dev` nor `KC_DB=dev-file` in production.

When using Kubernetes, importing should be done using init-containers (with a volume when using `db=dev-file`).

### Okta

If you'd like to use Okta instead of Keycloak, it's pretty quick using the [Okta CLI](https://cli.okta.com/). After you've installed it, run:

```shell
okta register
```

Then, in your JHipster app's directory, run `okta apps create` and select **JHipster**. This will set up an Okta app for you, create `ROLE_ADMIN` and `ROLE_USER` groups, create a `.okta.env` file with your Okta settings, and configure a `groups` claim in your ID token.

Run `source .okta.env` and start your app with Maven or Gradle. You should be able to sign in with the credentials you registered with.

If you're on Windows, you should install [WSL](https://docs.microsoft.com/en-us/windows/wsl/install-win10) so the `source` command will work.

If you'd like to configure things manually through the Okta developer console, see the instructions below.

First, you'll need to create a free developer account at <https://developer.okta.com/signup/>. After doing so, you'll get your own Okta domain, that has a name like `https://dev-123456.okta.com`.

Modify `src/main/resources/config/application.yml` to use your Okta settings.

```yaml
spring:
  ...
  security:
    oauth2:
      client:
        provider:
          oidc:
            issuer-uri: https://{yourOktaDomain}/oauth2/default
        registration:
          oidc:
            client-id: {clientId}
            client-secret: {clientSecret}
security:
```

Create an OIDC App in Okta to get a `{clientId}` and `{clientSecret}`. To do this, log in to your Okta Developer account and navigate to **Applications** > **Add Application**. Click **Web** and click the **Next** button. Give the app a name you’ll remember, specify `http://localhost:8080` as a Base URI, and `http://localhost:8080/login/oauth2/code/oidc` as a Login Redirect URI. Click **Done**, then Edit and add `http://localhost:8080` as a Logout redirect URI. Copy and paste the client ID and secret into your `application.yml` file.

Create a `ROLE_ADMIN` and `ROLE_USER` group and add users into them. Modify e2e tests to use this account when running integration tests. You'll need to change credentials in `src/test/javascript/e2e/account/account.spec.ts` and `src/test/javascript/e2e/admin/administration.spec.ts`.

Navigate to **API** > **Authorization Servers**, click the **Authorization Servers** tab and edit the default one. Click the **Claims** tab and **Add Claim**. Name it "groups", and include it in the ID Token. Set the value type to "Groups" and set the filter to be a Regex of `.*`.

After making these changes, you should be good to go! If you have any issues, please post them to [Stack Overflow](https://stackoverflow.com/questions/tagged/jhipster). Make sure to tag your question with "jhipster" and "okta".

### Auth0

If you'd like to use [Auth0](https://auth0.com/) instead of Keycloak, follow the configuration steps below:

- Create a free developer account at <https://auth0.com/signup>. After successful sign-up, your account will be associated with a unique domain like `dev-xxx.us.auth0.com`
- Create a new application of type `Regular Web Applications`. Switch to the `Settings` tab, and configure your application settings like:
  - Allowed Callback URLs: `http://localhost:8080/login/oauth2/code/oidc`
  - Allowed Logout URLs: `http://localhost:8080/`
- Navigate to **User Management** > **Roles** and create new roles named `ROLE_ADMIN`, and `ROLE_USER`.
- Navigate to **User Management** > **Users** and create a new user account. Click on the **Role** tab to assign roles to the newly created user account.
- Navigate to **Auth Pipeline** > **Rules** and create a new Rule. Choose `Empty rule` template. Provide a meaningful name like `JHipster claims` and replace `Script` content with the following and Save.

```javascript
function (user, context, callback) {
  user.preferred_username = user.email;
  const roles = (context.authorization || {}).roles;

  function prepareCustomClaimKey(claim) {
    return `https://www.jhipster.tech/${claim}`;
  }

  const rolesClaim = prepareCustomClaimKey('roles');

  if (context.idToken) {
    context.idToken[rolesClaim] = roles;
  }

  if (context.accessToken) {
    context.accessToken[rolesClaim] = roles;
  }

  callback(null, user, context);
}
```

- In your `JHipster` application, modify `src/main/resources/config/application.yml` to use your Auth0 application settings:

```yaml
spring:
  ...
  security:
    oauth2:
      client:
        provider:
          oidc:
            # make sure to include the ending slash!
            issuer-uri: https://{your-auth0-domain}/
        registration:
          oidc:
            client-id: {clientId}
            client-secret: {clientSecret}
            scope: openid,profile,email
jhipster:
  ...
  security:
    oauth2:
      audience:
        - https://{your-auth0-domain}/api/v2/
```

## Building for production

### Packaging as jar

To build the final jar and optimize the document application for production, run:

```
./mvnw -Pprod clean verify
```

To ensure everything worked, run:

```
java -jar target/*.jar
```

Refer to [Using JHipster in production][] for more details.

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

```
./mvnw -Pprod,war clean verify
```

### JHipster Control Center

JHipster Control Center can help you manage and control your application(s). You can start a local control center server (accessible on http://localhost:7419) with:

```
docker compose -f src/main/docker/jhipster-control-center.yml up
```

## Testing

### Spring Boot tests

To launch your application's tests, run:

```
./mvnw verify
```

## Others

### Code quality using Sonar

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker compose -f src/main/docker/sonar.yml up -d
```

Note: we have turned off forced authentication redirect for UI in [src/main/docker/sonar.yml](src/main/docker/sonar.yml) for out of the box experience while trying out SonarQube, for real use cases turn it back on.

You can run a Sonar analysis with using the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the maven plugin.

Then, run a Sonar analysis:

```
./mvnw -Pprod clean verify sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

If you need to re-run the Sonar phase, please be sure to specify at least the `initialize` phase since Sonar properties are loaded from the sonar-project.properties file.

```
./mvnw initialize sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

Additionally, Instead of passing `sonar.password` and `sonar.login` as CLI arguments, these parameters can be configured from [sonar-project.properties](sonar-project.properties) as shown below:

```
sonar.login=admin
sonar.password=admin
```

For more information, refer to the [Code quality page][].

### Docker Compose support

JHipster generates a number of Docker Compose configuration files in the [src/main/docker/](src/main/docker/) folder to launch required third party services.

For example, to start required services in Docker containers, run:

```
docker compose -f src/main/docker/services.yml up -d
```

To stop and remove the containers, run:

```
docker compose -f src/main/docker/services.yml down
```

[Spring Docker Compose Integration](https://docs.spring.io/spring-boot/reference/features/dev-services.html) is enabled by default. It's possible to disable it in application.yml:

```yaml
spring:
  ...
  docker:
    compose:
      enabled: false
```

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a Docker image of your app by running:

```sh
npm run java:docker
```

Or build a arm64 Docker image when using an arm64 processor os like MacOS with M1 processor family running:

```sh
npm run java:docker:arm64
```

Then run:

```sh
docker compose -f src/main/docker/app.yml up -d
```

For more information refer to [Using Docker and Docker-Compose][], this page also contains information on the Docker Compose sub-generator (`jhipster docker-compose`), which is able to generate Docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration][] page for more information.

[JHipster Homepage and latest documentation]: https://www.jhipster.tech
[JHipster 8.11.0 archive]: https://www.jhipster.tech/documentation-archive/v8.11.0
[Doing microservices with JHipster]: https://www.jhipster.tech/documentation-archive/v8.11.0/microservices-architecture/
[Using JHipster in development]: https://www.jhipster.tech/documentation-archive/v8.11.0/development/
[Using Docker and Docker-Compose]: https://www.jhipster.tech/documentation-archive/v8.11.0/docker-compose
[Using JHipster in production]: https://www.jhipster.tech/documentation-archive/v8.11.0/production/
[Running tests page]: https://www.jhipster.tech/documentation-archive/v8.11.0/running-tests/
[Code quality page]: https://www.jhipster.tech/documentation-archive/v8.11.0/code-quality/
[Setting up Continuous Integration]: https://www.jhipster.tech/documentation-archive/v8.11.0/setting-up-ci/
[Node.js]: https://nodejs.org/
[NPM]: https://www.npmjs.com/
