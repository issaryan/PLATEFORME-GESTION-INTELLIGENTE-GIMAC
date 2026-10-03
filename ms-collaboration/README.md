<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-COLLABORATION : plateforme DES du GIMAC

Collaboration entre les acteurs du GIMAC. Premier incrément livré (D-64) : la **messagerie instantanée de travail** entre acteurs internes, messages directs ou de groupe, étiquettes de liaison, pièces jointes, non-lus et temps réel. Les salons chiffrés de bout en bout, la visioconférence et l'archive scellée de crise (D-28, modèle `ms-collaboration.jdl`) viendront avec la vague 3.

| Élément | Valeur |
|---|---|
| Domaine d'analyse (CA) | D6 |
| Épopée du carnet (CP v2.0) | COL |
| Décisions | D-64 (messagerie de travail), D-61 (définition de fini), D-55 (socle du lot 1), D-43 (JHipster 9.4.0) |
| Contrat d'API | [`docs/lot1/messagerie.md`](../docs/lot1/messagerie.md) |
| Port | 8093 |
| Paquet Java | `cm.gimac.des.collaboration` |
| Socle | JHipster 9.4.0, Spring Boot 4.1, Java 21, Jackson 3 ; sans cache ; Cucumber |
| Entités générées (12) | RoomRetentionPolicy, Room, RoomMember, VerifiedTerminal, MessageMetadata, MessageReceipt, Conference, ConferenceParticipant, RecordingConsent, RecordingSegment, MediaKeyEpoch, RoomArchive (CRUD réservé à `ROLE_ADMIN`, V3) |
| Tables écrites à la main | `conversation`, `conversation_membre`, `message`, `message_etiquette`, `piece_jointe` (D-64) ; `outbox_event`, `processed_event` (D-55) |
| Règles de gestion | RG-52 (masquage des PAN), RG-53 (conservation), RG-15 et RG-35 (idempotence) ; RG-22 à RG-55 pour la V3 |

## Messagerie de travail (D-64)

- **Destinataires** choisis dans l'annuaire de MS-IAM : comptes internes actifs (sans institution), 1 à 50, jamais l'expéditeur ; aucune adresse libre. Deux personnes sans titre ni liaison forment une conversation **directe**, réutilisée ensuite (clé d'unicité `cle_directe`) ; sinon la conversation est **de groupe**, avec un titre et une liaison de rattachement facultatifs.
- **Membres** : identifiant IAM, nom et fonction figés à l'entrée ; la marque de lecture (dernier message lu) donne les **non-lus** par conversation et au total.
- **Messages** : 1 à 4 000 caractères, ou au moins une pièce jointe ; **immuables** (entité `@Immutable` et déclencheur PostgreSQL refusant toute mise à jour ; une correction passe par un nouveau message).
- **RG-52** : toute suite de 13 à 19 chiffres (espaces ou tirets admis) valide au contrôle de Luhn est masquée en 6 premiers chiffres, astérisques, 4 derniers, **avant** l'enregistrement (`MasquagePan`).
- **Chiffrement au repos** : AES-256-GCM, IV aléatoire par message, identifiant du message en donnée associée ; clé de 32 octets hors base (`GIMAC_MESSAGERIE_CLE`) ; sans clé valide, le service ne démarre pas.
- **Étiquettes** : 0 à 10 liaisons citées (`LIA-AAAA-NNN`), qui ouvrent le dossier `/des/parcours/{liaisonId}` (droits vérifiés à l'ouverture par MS-HOMOLOGATION).
- **Pièces jointes** : 5 au plus, 20 Mo chacune ; PDF, PNG, JPEG, texte, CSV, Word, Excel, PowerPoint, ZIP (extension et type contrôlés, type rendu canonique). Déposées dans MS-DOCUMENT (dépôt WORM, finalité `MESSAGERIE`, référence = message) ; nom, taille, type et empreinte SHA-256 conservés. Téléchargement réservé aux membres, en `Content-Disposition: attachment`, avec l'empreinte en `Repr-Digest` (RFC 9530) ; chaque lecture est journalisée.
- **Gros fichiers et repartage (D-74, `docs/lot1/depots-fragmentes.md` §5)** : la partie `message` accepte `documents` (tickets de documents déposés par l'expéditeur par envoi fragmenté, finalité `MESSAGERIE`, vérifiés auprès de MS-DOCUMENT, sinon `COL_DOCUMENT_INVALID`) et `transferts` (pièces existantes repartagées, l'expéditeur devant être membre de leur conversation, sinon 404 `COL_ATTACHMENT_NOT_FOUND`). La pièce créée référence le document existant : ni envoi ni copie. 5 pièces au plus toutes sources confondues ; les tickets n'ont pas la limite de 20 Mo et admettent aussi vidéo, audio et archives (7z, TAR, GZ). Le téléchargement est relayé en flux quelle que soit la taille.
- **Événement** `collaboration.message-sent.v1` (sujet `collaboration.message`, clé = conversation, outbox transactionnelle, CloudEvents 1.0) : `conversationId`, `messageId`, `auteurId`, `destinataires`, `envoyeLe` ; **jamais le contenu**. La passerelle le pousse aux seuls destinataires connectés (`/bff/{couche}/live`).

- **Photo de profil** (docs/lot1/mon-compte.md §1) : `avatarVersion` (nulle sans photo) dans l'annuaire (relayé de MS-IAM), dans les membres des conversations et dans l'auteur de chaque message ; versions lues à chaque réponse par lot (`GET /api/v1/interne/collaborateurs/avatars?ids=` de MS-IAM, port `CollaboratorDirectoryPort.versionsAvatar`) car une photo change après l'entrée du membre ; annuaire injoignable : versions absentes (initiales), la lecture n'échoue pas. L'image elle-même est servie par MS-IAM (`/bff/des/iam/avatars/{id}?v=`).

**Écart assumé et borné (D-64).** Le chiffrement de bout en bout (RG-22, D-28, BF-24) et l'archivage scellé par terminal déclaré restent exigés pour les salons de crise et les salons liés à un ticket (V3). La messagerie de travail n'en porte pas : le serveur voit le clair, pour appliquer RG-52, et le protège par TLS en transit et AES-256-GCM au repos. Le RSSI confirme cet écart avant la mise en production.

### API (`/api/v1/messagerie`, sous `/bff/{des,console}/collaboration/messagerie`)

| Méthode et chemin | Permission | Réponse |
|---|---|---|
| `GET /annuaire?recherche=&page=0&taille=20` | `COL_MESSAGE_READ` | Page `{contenu, total, page, taille}` de personnes joignables, l'appelant exclu |
| `GET /conversations?page=0&taille=20&recherche=&liaisonId=` | `COL_MESSAGE_READ` | Conversations de l'appelant, la plus récente d'abord ; recherche sur le titre, la référence de liaison et le nom des membres |
| `POST /conversations` (multipart `message` + `fichiers`) | `COL_MESSAGE_SEND` | 201 et `Location` (nouvelle conversation) ou 200 (conversation directe réutilisée) |
| `GET /conversations/{id}` | `COL_MESSAGE_READ` | Conversation (membre seulement, 404 sinon) |
| `GET /conversations/{id}/messages?avant=&taille=50` | `COL_MESSAGE_READ` | `{contenu, plusAnciens}`, du plus ancien au plus récent, pagination vers le passé |
| `POST /conversations/{id}/messages` (multipart) | `COL_MESSAGE_SEND` | 201, message créé |
| `POST /conversations/{id}/lu` | `COL_MESSAGE_READ` | 204 |
| `GET /non-lus` | `COL_MESSAGE_READ` | `{ "total": n }` |
| `GET /pieces/{pieceId}` | `COL_MESSAGE_READ` | Contenu de la pièce (membre seulement, 404 sinon) |

Refus : 401 sans jeton ; 403 sans second facteur (`acr` 2 ou 3) ou sans la permission ; 404 hors appartenance. Erreurs en `application/problem+json` avec `code` : ceux du contrat (`COL_RECIPIENT_REQUIRED`, `COL_RECIPIENT_UNKNOWN`, `COL_RECIPIENT_SELF`, `COL_TOO_MANY_RECIPIENTS`, `COL_MESSAGE_EMPTY`, `COL_MESSAGE_TOO_LONG`, `COL_TAG_INVALID`, `COL_TOO_MANY_TAGS`, `COL_ATTACHMENT_TOO_LARGE`, `COL_ATTACHMENT_TYPE`, `COL_TOO_MANY_ATTACHMENTS`, `COL_CONVERSATION_NOT_FOUND`, `COL_ATTACHMENT_NOT_FOUND`, `COL_DOCUMENT_INVALID`), plus `COL_MESSAGE_NOT_FOUND` (curseur `avant` inconnu), `COL_ATTACHMENT_REJECTED` (400, refus du dépôt par MS-DOCUMENT, par exemple BNF-16), `COL_DIRECTORY_UNAVAILABLE` et `COL_DOCUMENT_UNAVAILABLE` (503, tiers injoignable), `COL_USER_TOKEN_REQUIRED` (403).

### Dépendances (jeton de service `client_credentials`, client `collaboration-service`)

| Service | Route | Permission | État |
|---|---|---|---|
| MS-IAM | `GET /api/v1/interne/collaborateurs?recherche=&page=&taille=`, `GET /api/v1/interne/collaborateurs/{id}` | `INTERNAL_IAM_READ` | Livrée avec cet incrément |
| MS-IAM | `GET /api/v1/interne/collaborateurs/avatars?ids=` (versions des photos de profil, 200 par appel) | `INTERNAL_IAM_READ` | Livrée (mon-compte.md §1) |
| MS-DOCUMENT | `POST /api/v1/interne/documents` (multipart `fichier`, `finalite`, `reference`, `deposantId`) | `INTERNAL_DOCUMENT_DEPOSIT` | Client écrit d'après le contrat ; route à livrer dans MS-DOCUMENT |
| MS-DOCUMENT | `GET /api/v1/interne/documents/{id}/contenu` | `INTERNAL_DOCUMENT_READ` | Idem ; servi en flux (D-74) |
| MS-DOCUMENT | `GET /api/v1/interne/documents/{id}` (métadonnées d'un ticket : nom, taille, type, empreinte, finalité, déposant) | `INTERNAL_DOCUMENT_READ` | Client écrit d'après `docs/lot1/depots-fragmentes.md` §4 (D-74) |

Adaptateurs choisis un port à la fois (`real` | `stub`) : réels en profil `prod`, de substitution en développement et en test (annuaire de cinq acteurs internes, un correspondant et un compte désactivé ; dépôt en mémoire avec empreinte SHA-256, dont les métadonnées sont celles de ses propres dépôts ; `DocumentStubAdapter.deposerFragmente` simule un dépôt fragmenté pour les tests).

## Architecture (PI §0.8, D-55)

- `domain/messagerie` : entités de la messagerie ; `domain/service` : `MasquagePan`, `ReglesMessagerie` (services purs, sans Spring) ; `domain/event` : `MessageEnvoyePayload` ; `domain/exception` : `CollaborationException`.
- `application/port/in` : `EnvoyerMessageUseCase`, `ConsulterMessagerieUseCase` ; `application/port/out` : `CollaboratorDirectoryPort`, `DocumentStoragePort`, `ChiffrementPort` ; `application/usecase` : `MessagerieService`, `MessagerieQueries`.
- `infrastructure/client` : `IamCollaboratorAdapter`, `CollaboratorStubAdapter`, `DocumentRestAdapter`, `DocumentStubAdapter`, `ServiceTokenInterceptor` ; `infrastructure/crypto` : `AesGcmChiffrement` ; `infrastructure/messaging` : outbox, relais CloudEvents, consommateur idempotent ; `infrastructure/security` : `StrongAuthenticationAuthorizationManager`, `CorrelationContext`.
- `web/rest/messagerie` : `MessagerieResource` ; `web/rest/errors/CollaborationExceptionHandler` (ProblemDetail).
- Sécurité (`config/SecurityConfiguration`) : `/api/v1/interne/**` au jeton de service ; `/api/v1/**` au jeton à deux facteurs puis à la permission (`@PreAuthorize`) ; le CRUD généré `/api/**` à `ROLE_ADMIN`.
- Liquibase : `20260930100000_lot1_post_generation.xml` (outbox, déduplication) et `20260930100100_col_messagerie.xml` (tables, index, déclencheurs d'immuabilité), écrits à la main.

## Configuration

| Propriété | Variable d'environnement | Rôle |
|---|---|---|
| `gimac.messagerie.cle-chiffrement` | `GIMAC_MESSAGERIE_CLE` | Clé AES-256-GCM, 32 octets en base 64 (`openssl rand -base64 32`), générée dans `.env` par `gimac-dev.sh`, Secret `gimac-messagerie` en Kubernetes, OpenBao à partir d'INT |
| `spring.security.oauth2.client.registration.service-token.*` | `GIMAC_SERVICE_CLIENT_ID`, `GIMAC_SERVICE_CLIENT_SECRET` | Client `collaboration-service` (secret `GIMAC_SECRET_COLLABORATION_SERVICE`) |
| `spring.security.oauth2.client.registration.oidc.client-secret` | `GIMAC_SECRET_INTERNAL` | Client `internal` généré par JHipster (profil `secret-samples`, domaine `gimac`) |
| `gimac.adaptateurs.annuaire`, `gimac.adaptateurs.documents` | `GIMAC_ADAPTATEURS_ANNUAIRE`, `GIMAC_ADAPTATEURS_DOCUMENTS` | `real` ou `stub` (`stub` par défaut, `real` en `prod`) |
| `gimac.services.iam.url`, `gimac.services.document.url` | `GIMAC_SERVICES_IAM_URL`, `GIMAC_SERVICES_DOCUMENT_URL` | Adresses de MS-IAM et MS-DOCUMENT |
| `spring.servlet.multipart.max-file-size`, `max-request-size` | — | 21 Mo et 106 Mo : bornes de la requête ; la règle des 20 Mo est appliquée par le domaine |

## Tests : définition de fini (D-61)

- **Unitaires** : `MasquagePanTest`, `ReglesMessagerieTest` (règles, pièces, étiquettes, conversation directe, non-lus), `MessagerieServiceTest` et `MessagerieQueriesTest` (Mockito), `AesGcmChiffrementTest`, `AdaptateursMessagerieTest` (`MockRestServiceServer` pour MS-IAM et MS-DOCUMENT, adaptateurs de substitution), `StrongAuthenticationAuthorizationManagerTest`, `TechnicalStructureTest` (ArchUnit).
- **Sécurité** : `MessagerieResourceSecurityTest`, chaque route : 401 sans jeton, 403 sans second facteur, 403 sans la permission, accès avec elle ; formes des réponses et erreurs.
- **Intégration** : `MessagerieIT` sur PostgreSQL et Kafka (Testcontainers) : conversation directe réutilisée, événement relayé sans contenu, chiffré en base, RG-52, immuabilité, pagination du fil, non-lus, groupe et étiquettes, pièce jointe, non-membre 404, règles.
- **Recette** : `lot1/messagerie.feature` dans `RecetteGherkinIT` (envoyer un message direct, étiqueter une liaison, joindre une pièce, lire et compter les non-lus, refus pour un non-membre, masquage d'un PAN).
- Seuils JaCoCo à 80 % : à `test`, classes de la messagerie en lignes et en méthodes, l'ensemble en branches ; à `verify`, tout le service en lignes et en méthodes, hors MapStruct.
- Lancer : `./mvnw -ntp -B verify` (Docker requis).

## Lancer

- Avec la plateforme : `infra/docker-compose/gimac-dev.sh demarrer` à la racine du dépôt (Keycloak `gimac`, sujets `infra/kafka/creer-sujets-lot1.sh`).
- Service seul, en développement : `GIMAC_MESSAGERIE_CLE=$(openssl rand -base64 32) ./mvnw`.
- Image de conteneur (Jib, sans Dockerfile, D-48) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Code repris du projet afriland (D-52)

Copié et adapté (paquet `cm.gimac.des.collaboration`), aligné sur MS-HOMOLOGATION à la régénération.

| Classe | Origine dans afriland | Propriété d'activation |
|---|---|---|
| `config/DecodeurJwtInterne` | `audit-service/src/main/java/com/afriland/audit/config/DecodeurJwtInterne.java` | sans bean à activer |
| `config/KafkaErrorHandlingConfiguration` | `audit-service/src/main/java/com/afriland/audit/config/KafkaErrorHandlingConfiguration.java` | `gimac.kafka.rebut.actif` (files `.dlq`, rejeux 1 s, 5 s, 25 s) |
| `infrastructure/logging/PiiMaskingLogConverter` | `notifications-service/src/main/java/com/afriland/notifications/infrastructure/logging/PiiMaskingLogConverter.java` | sans bean à activer (`%maskedMsg`) |
| `adapter/outbound/securite/ServiceTokenProvider` | `administration-service/.../ServiceTokenProvider.java` | remplacé par `infrastructure/client/ServiceTokenInterceptor` et `config/ServiceTokenConfiguration` (même rôle, convention de MS-HOMOLOGATION) |

Écarts comblés (D-53) : `ValidateurLiaisonCertificat` et option du décodeur exigeant la liaison au certificat (RFC 8705) ; masquage des journaux limité aux numéros vérifiant Luhn, en 6+4 (RG-52).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 4bade16 | Génération JHipster 8.11.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 3 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 1 classes reprises d'afriland : ServiceTokenProvider (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 103 tests unitaires et 431 tests d'intégration réussis, image Jib construite (D-54). |
| 2026-09-29 | — | `.gitignore` : exception `!src/**/out/`, pour que les ports sortants `application/port/out` soient versionnés |
| 2026-09-30 | — | Régénération en JHipster 9.4.0 (D-43, D-55 : Spring Boot 4.1, Java 21, sans cache, Cucumber ; `ProcessedEvent` remplacée par la déduplication du consommateur idempotent) ; socle du lot 1 repris de MS-HOMOLOGATION (outbox, relais, rebut `.dlq`, jeton de service, second facteur, ArchUnit, JaCoCo). Messagerie de travail (D-64) : conversations directes et de groupe, RG-52, chiffrement AES-256-GCM au repos, étiquettes, pièces jointes via MS-DOCUMENT, non-lus, événement `collaboration.message-sent.v1`. 184 tests unitaires et de sécurité, 442 d'intégration et de recette réussis ; couverture fusionnée du service 88,8 % en lignes et 86,4 % en méthodes (hors MapStruct), code de la messagerie 96,8 % en lignes et 93,6 % en branches. |
| 2026-10-01 | — | Photo de profil (docs/lot1/mon-compte.md §1) : `avatarVersion` dans l'annuaire, les membres des conversations et l'auteur des messages (versions lues par lot sur MS-IAM, annuaire injoignable sans échec). `./mvnw -ntp -B verify` : 188 tests unitaires et de sécurité, 443 d'intégration et de recette réussis ; couverture fusionnée hors MapStruct 88,9 % en lignes, 86,6 % en méthodes. |
<!-- GIMAC:FIN -->

# collaboration

This application was generated using JHipster 9.4.0, you can find documentation and help at [https://www.jhipster.tech/documentation-archive/v9.4.0](https://www.jhipster.tech/documentation-archive/v9.4.0).

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

```bash
./mvnw
```

For further instructions on how to develop with JHipster, have a look at [Using JHipster in development][].

### OAuth 2.0 / OpenID Connect

Congratulations! You've selected an excellent way to secure your JHipster application. If you're not sure what OAuth and OpenID Connect (OIDC) are, please see [What the Heck is OAuth?](https://developer.okta.com/blog/2017/06/21/what-the-heck-is-oauth)

To log in to your app, you'll need to have [Keycloak](https://keycloak.org) up and running. The JHipster Team has created a Docker container for you that has the default users and roles. Start Keycloak using the following command.

```bash
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

If you'd like to use Okta instead of Keycloak, it's pretty quick.

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

Create a `ROLE_ADMIN` and `ROLE_USER` group and add users into them. Modify e2e tests to use this account when running integration tests.

Navigate to **API** > **Authorization Servers**, click the **Authorization Servers** tab and edit the default one. Click the **Claims** tab and **Add Claim**. Name it "groups", and include it in the ID Token. Set the value type to "Groups" and set the filter to be a Regex of `.*`.

After making these changes, you should be good to go! If you have any issues, please post them to [Stack Overflow](https://stackoverflow.com/questions/tagged/jhipster). Make sure to tag your question with "jhipster" and "okta".

### Auth0

If you'd like to use [Auth0](https://auth0.com/) instead of Keycloak, follow the configuration steps below:

- Create a free developer account at [Sign Up - Auth0](https://auth0.com/signup). After successful sign-up, your account will be associated with a unique domain like `dev-xxx.us.auth0.com`
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

To build the final jar and optimize the collaboration application for production, run:

```bash
./mvnw -Pprod clean verify
```

To ensure everything worked, run:

```bash
java -jar target/*.jar
```

Refer to [Using JHipster in production][] for more details.

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

```bash
./mvnw -Pprod,war clean verify
```

### JHipster Control Center

JHipster Control Center can help you manage and control your application(s). You can start a local control center server (accessible on http://localhost:7419) with:

```bash
docker compose -f src/main/docker/jhipster-control-center.yml up
```

## Testing

### Spring Boot tests

To launch your application's tests, run:

```bash
./mvnw verify
```

## Others

### Code quality using Sonar

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```bash
docker compose -f src/main/docker/sonar.yml up -d
```

Note: we have turned off forced authentication redirect for UI in [src/main/docker/sonar.yml](src/main/docker/sonar.yml) for out of the box experience while trying out SonarQube, for real use cases turn it back on.

You can run a Sonar analysis with using the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the maven plugin.

Then, run a Sonar analysis:

```bash
./mvnw -Pprod clean verify sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

If you need to re-run the Sonar phase, please be sure to specify at least the `initialize` phase since Sonar properties are loaded from the sonar-project.properties file.

```bash
./mvnw initialize sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

Additionally, Instead of passing `sonar.password` and `sonar.login` as CLI arguments, these parameters can be configured from [sonar-project.properties](sonar-project.properties) as shown below:

```bash
sonar.login=admin
sonar.password=admin
```

For more information, refer to the [Code quality page][].

### Docker Compose support

JHipster generates a number of Docker Compose configuration files in the [src/main/docker/](src/main/docker/) folder to launch required third party services.

For example, to start required services in Docker containers, run:

```bash
docker compose -f src/main/docker/services.yml up -d
```

To stop and remove the containers, run:

```bash
docker compose -f src/main/docker/services.yml down
```

[Spring Docker Compose Integration](https://docs.spring.io/spring-boot/reference/features/dev-services.html) is enabled by default. It's possible to disable it in `application.yml`:

```yaml
spring:
  ...
  docker:
    compose:
      enabled: false
```

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a Docker image of your app by running:

```bash
npm run java:docker
```

Or build an arm64 Docker image when using an arm64 processor OS, i.e., Apple Silicon chips (M*), running:

```bash
npm run java:docker:arm64
```

Then run:

```bash
docker compose -f src/main/docker/app.yml up -d
```

For more information refer to [Docker and Docker-Compose](https://www.jhipster.tech/documentation-archive/v9.4.0/docker-compose/), this page also contains information on the Docker Compose sub-generator (`jhipster docker-compose`), which is able to generate Docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration](https://www.jhipster.tech/documentation-archive/v9.4.0/setting-up-ci/) page for more information.

## References

- [JHipster Homepage and latest documentation](https://www.jhipster.tech/)
- [JHipster 9.4.0 archive](https://www.jhipster.tech/documentation-archive/v9.4.0)
- [Doing microservices with JHipster](https://www.jhipster.tech/documentation-archive/v9.4.0/microservices-architecture/)
- [Using JHipster in development](https://www.jhipster.tech/documentation-archive/v9.4.0/development/)
- [Using Docker and Docker-Compose](https://www.jhipster.tech/documentation-archive/v9.4.0/docker-compose)
- [Using JHipster in production](https://www.jhipster.tech/documentation-archive/v9.4.0/production/)
- [Running tests page](https://www.jhipster.tech/documentation-archive/v9.4.0/running-tests/)
- [Code quality page](https://www.jhipster.tech/documentation-archive/v9.4.0/code-quality/)
- [Setting up Continuous Integration](https://www.jhipster.tech/documentation-archive/v9.4.0/setting-up-ci/)
- [Node.js](https://nodejs.org/)
- [NPM](https://www.npmjs.com/)
