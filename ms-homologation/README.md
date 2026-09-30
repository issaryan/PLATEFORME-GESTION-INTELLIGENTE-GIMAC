<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-HOMOLOGATION : plateforme DES du GIMAC

Suivi de l'homologation des participants : référentiel d'opérations versionné, liaisons, instanciation des opérations par tri topologique, règle des quatre éléments, reprise en lecture seule du simulateur, couvertures, aptitude à la certification.

| Élément | Valeur |
|---|---|
| Lot | Lot 1, module de gestion de la certification (PI-GIMAC-01 v1.0, `docs/lot1`) |
| Récits | HOM-01 à HOM-24 |
| Port | 8086 |
| Paquet Java | `cm.gimac.des.homologation` |
| Modèle | `ms-homologation.jdl` (JDL du plan, §0.2) ; JHipster 9.4.0 ; Cucumber |
| Entités (18) | ReferentialVersion, ReferenceOperation, Liaison, ContractualScope, ScopeItem, OperationInstance, Assignment, EvidenceReference, ClosureRecord, CampaignResult, UnmatchedScenario, Coverage, EvaluationBase, EvidenceFileExport, ReplayCursor, RejectedRecord, ReferentialMigration, DeadlineNotification |
| Règles de gestion (REC-GIMAC-01 §5) | RG-26 à RG-33, RG-72 |
| Correspondances avec le plan | `docs/lot1/correspondance.md` |

## Architecture (PI §0.8)

- `domain/` : entités JPA générées, `domain/service` (services de domaine purs), `domain/event` (charges utiles).
- `application/` : ports entrants et sortants, cas d'utilisation transactionnels.
- `infrastructure/` : outbox et relais CloudEvents, consommateur idempotent, adaptateurs réels et de substitution (`gimac.adapters.*`), jeton de service, contexte de corrélation.
- `web/rest/` : ressources générées (`/api/<entités>`) et ressources du lot (`/api/v1/...`, exposées par la passerelle sous `/bff/des/homologation` et `/bff/portail/homologation`).
- Test d'architecture : couches `application` et `infrastructure`, services de domaine sans Spring.

## Cadrage : étape 3 du parcours (D-57, D-58)

La liaison s'ouvre au cadrage sur une institution **adhérente** (ou déjà en homologation, pour une nouvelle liaison sans repasser par l'adhésion, rang 35). Un **chef de département** (chef du DES ou de la DDRM : `HOM_LIAISON_OPEN` et `HOM_PROJECT_DESIGNATE`) l'ouvre en désignant le **conducteur de projet** parmi les détenteurs du rôle `CONDUCTEUR_PROJET`, actifs et internes au GIMAC ; la banque partenaire éventuelle doit être une banque. La liaison est liée à la version publiée en vigueur du référentiel ; son **périmètre contractualisé** est dérivé des services souscrits (HOM-05), le service `TRANSVERSE` toujours inclus. L'événement `homologation.liaison-opened.v1` (sujet `homologation.liaison`) fait passer l'adhérent à `EN_HOMOLOGATION` dans MS-PARTICIPANT ; la redésignation publie `homologation.liaison-lead-designated.v1`. Une seule liaison active par institution et type de projet (index partiel `uk_liaison_active`).

| Méthode et chemin (`/api/v1`, sous `/bff/des/homologation`) | Permission | Objet |
|---|---|---|
| `GET /liaisons?etat=&typeProjet=&institutionId=&recherche=&mesProjets=&page=&taille=` | `HOM_LIAISON_LIST` ou `HOM_LIAISON_READ` | Liste `{contenu, total}` ; `mesProjets` : liaisons confiées au conducteur connecté |
| `GET /liaisons/{id}` | `HOM_LIAISON_READ` | Fiche : phase, périmètre, intervenants, actions permises |
| `GET /liaisons/conducteurs` | `HOM_PROJECT_DESIGNATE` ou `HOM_LIAISON_OPEN` | Conducteurs désignables |
| `POST /liaisons` | `HOM_LIAISON_OPEN` et `HOM_PROJECT_DESIGNATE` | Ouverture (201, 403, 409, 503) |
| `PUT /liaisons/{id}/chef-de-projet` | `HOM_PROJECT_DESIGNATE` | Redésignation motivée |

Ports sortants : `ParticipantDirectoryPort` (interface interne `/api/v1/interne/institutions/{id}` de MS-PARTICIPANT) et `CollaboratorDirectoryPort` (`/api/v1/interne/collaborateurs` de MS-IAM), jeton de service ; adaptateurs réels en profil prod, de substitution en développement et en test (`gimac.adapters.participant-reader`, `collaborator-reader`). Un annuaire injoignable donne 503, jamais un statut supposé.

Sécurité (D-61) : `/api/v1/**` exige un jeton à deux facteurs (`acr` 2 ou 3) ; le CRUD généré `/api/**` est réservé à `ROLE_ADMIN`.

## Parcours de la liaison : étapes 4 à 10 (D-56 à D-59)

Part de MS-HOMOLOGATION fixée par `docs/lot1/parcours-backend.md` ; les règles, refus, compteurs et liens sont ceux du serveur de substitution de l'interface (`frontend/packages/lot1/src/services/simule/parcours.ts`), les formes celles de `modele/parcours.ts`.

- **Référentiel de l'annexe A** (CR-CERT-GIMAC-01 v1.4, D-59) chargé par Liquibase (`config/liquibase/data/parcours_referentiel_cr.csv`, table `parcours_referentiel_operation`) : 96 opérations (rangs 1 à 35, C1 à C10, M1 à M17, D1 à D34) avec étape, segment, porteurs, types, plateforme, grain, `cle`, `secret`, `valeurReference`, `oubliSansSymptome`, `pivot`, prédécesseurs et délai. Il est **instancié à l'ouverture** de chaque liaison (rangs 3 et suivants, seule branche du type ; le rang 27 suit la dernière opération de la branche ; rangs 15 et 16 sur le simulateur pour le mobile) ; une liaison ouverte avant cette tranche l'est à la première lecture. Référence lisible `LIA-AAAA-NNN`.
- **Agrégat** `domain/parcours/Parcours` (sans Spring) : graphe des prédécesseurs (`VERROUILLEE`, `A_FAIRE`, `EN_VALIDATION`, `FAITE`, `BLOQUEE`), qualités RACI + E (le validateur n'est jamais le responsable), échéances et SLA (proche à 5 jours), étape courante et statut du CR (`CADREE` … `EN_PRODUCTION`) projetés sur l'automate technique de la liaison (`OUVERTE` … `CLOSE`), couvertures de certification et de mise en service par service, blocages datés, fiche de suivi, projection des verrous de MS-CERTIFICATION. Projections et tableaux de bord : `domain/service/ParcoursProjection`, `TableauDeBordCalculateur`.
- **Preuves** : une pièce déposée par l'interface dans MS-DOCUMENT est rattachée par `documentId` et `empreinte`, **revérifiées** auprès de MS-DOCUMENT (`HOM_DOCUMENT_UNKNOWN`, `HOM_DOCUMENT_HASH_MISMATCH`) ; une clé (C1, D3, D12, D27) ne porte que des codes de contrôle (4 à 8 hexadécimaux), C1 sa cérémonie ; un secret (29, M3, M7, M9, D2) une trace ; toute propriété hors contrat est refusée (`HOM_EVIDENCE_FIELD_REFUSED`). Validation après réauthentification forte (`X-Reauth-Assertion` vérifiée par MS-IAM, 428 `IAM_REAUTH_REQUIRED`, 403 `HOM_REAUTH_INVALID`).
- **Couverture** : un service est certifié à la fin des essais (rang 16) ; si le simulateur a été repris (table `coverage`, nature `CERTIFICATION`, HOM-12), seul un service mesuré à 100 % l'est. Le constat (rang 17) publie `homologation.coverage-established.v1`.

| Méthode et chemin (`/api/v1`) | Permission | Objet |
|---|---|---|
| `GET /liaisons/{id}/dossier` | `HOM_LIAISON_READ` | Dossier numérique : périmètre, deux couvertures, onze étapes à six éléments, blocages, actions permises |
| `GET /liaisons/{id}/parcours/operations?etape=&fonction=` | `HOM_LIAISON_READ` | Opérations instanciées, RACI + E, plateforme, grain, SLA, preuve, actions permises |
| `PUT /liaisons/{id}/ressources` | `HOM_STEP_MANAGE` | Affectation des ressources (rang 6) |
| `POST /parcours/operations/{id}/livrable` | `HOM_STEP_MANAGE` ou `HOM_EVIDENCE_SUBMIT` | Livrable (clôt l'opération) |
| `POST /parcours/operations/{id}/preuves` | `HOM_EVIDENCE_SUBMIT` | Preuve (valeur de référence, code de contrôle, cérémonie ; jamais de secret) |
| `POST /parcours/operations/{id}/validation` | `HOM_OPERATION_VALIDATE` + FIDO2 | Validation par V, jamais par R |
| `PUT /parcours/operations/{id}/intervenants` | `HOM_OPERATION_ASSIGN` ou `HOM_STEP_MANAGE` | Qualités R, C, V, I, E |
| `POST /liaisons/{id}/blocages` · `POST /blocages/{id}/levee` | `HOM_STEP_MANAGE` | Blocage daté, levée |
| `POST /liaisons/{id}/couverture/constat` | `CER_DOSSIER_VERIFY` | Constat de la couverture (rang 17) |
| `POST /liaisons/{id}/services-supplementaires` | `HOM_LIAISON_OPEN` ou `HOM_STEP_MANAGE` | Nouvelle liaison au cadrage pour un service supplémentaire (rang 35), hors unicité, conducteur à désigner |
| `GET /liaisons/{id}/fiche-suivi` · `POST …/remise` · `POST …/constat` | `HOM_LIAISON_READ` · `HOM_STEP_MANAGE` · `CER_DOSSIER_VERIFY` | Fiche de suivi des services, constat de mise en service (rangs 32, 33) |
| `GET /tableaux-de-bord/acteur?fonctions=` · `GET /tableaux-de-bord/pilotage` | `HOM_DASHBOARD_READ` | Tableau de bord par fonction (seules les fonctions des profils `ROLE_*` du jeton) ; pilotage : 4 compteurs, répartition, délais, 6 questions de l'annexe B |
| `GET /intervenants?fonction=` | `HOM_LIAISON_READ`, `HOM_OPERATION_ASSIGN` ou `HOM_STEP_MANAGE` | Personnes d'une fonction (annuaire de MS-IAM) |
| `GET /portail/liaisons` · `GET /portail/liaisons/{id}/dossier` · `GET /portail/taches` · `GET /portail/liaisons/{id}/fiche-suivi` | `HOM_PORTAL_READ` | Portail (`/bff/portail/homologation`) : institution du jeton, projection sans nom interne, tâches avec leur geste et leur écran |
| `PUT /portail/liaisons/{id}/coordonnees-techniques` · `POST /portail/operations/{id}/livrable` (multipart) · `PUT /portail/liaisons/{id}/fiche-suivi` | `HOM_PORTAL_SUBMIT` | Coordonnées d'essai (rang 12, hôte et port contrôlés), livrable déposé dans MS-DOCUMENT pour le compte du participant, fiche renseignée (rang 32) |
| `GET /interne/liaisons/{id}/contexte-certification` | `INTERNAL_HOMOLOGATION_READ` (jeton de service) | Contexte servi à MS-CERTIFICATION (§2) |

La liste `GET /liaisons` sert en outre `reference`, `statut`, `etapeCourante`, `bloquee`, `prochaineEcheance`, `dateCibleProduction`, `liaisonOrigineId` et les couvertures réelles ; elle se filtre par `statut`, `etape` (liste), `bloquees` et `sansConducteur`. Le portail exige l'institution et le second facteur ; une liaison d'une autre institution est introuvable (404).

Événements : publié `homologation.coverage-established.v1` (sujet `homologation.liaison`, extension `institutionid`). Consommés (groupe `homologation-certification`, sujets `certification.proces-verbal` et `certification.approbation`, idempotents, rebut `.dlq`) : `certification.pv-established.v1` (rang 18), `pv-signed.v1` (rang 19, `CERTIFIEE`), `change-record-constituted.v1` (rang 20), `change-record-referenced.v1` (rang 26 s'il est ouvert), `attestations-archived.v1` (rangs 21, 22), `security-approved.v1` (rangs 23 à 25, `APPROUVEE_SECURITE`) ; tolérés pour les compteurs : `attestation-deposited.v1`, `attestation-verified.v1`, `visas-required.v1`, `visa-signed.v1`.

Ports sortants ajoutés : `DocumentRegistryPort` (MS-DOCUMENT, `/api/v1/interne/documents{,/{id}/empreinte}`, `gimac.adapters.document-deposit`) et `StrongReauthPort` (MS-IAM, `/api/v1/interne/reauthentifications/verification`, `gimac.adapters.reauthentication`), adaptateurs réels et de substitution.

## Tests : définition de fini (D-61)

Unitaires (`LiaisonOpeningPolicyTest`, `OpenLiaisonServiceTest`, `LiaisonQueriesTest`, `AdaptateursCadrageTest`, `StrongAuthenticationAuthorizationManagerTest` ; parcours : `ParcoursTest`, `ParcoursProjectionTest`, `TableauDeBordCalculateurTest`, `ParcoursCommandesTest`, `ParcoursLecturesTest`, `ParcoursInstanciationTest`, `ParcoursDepotTest`, `IntervenantsTest`, `EvenementsCertificationTest`, `EvenementsCertificationConsumerTest`, `AdaptateursParcoursTest`), sécurité (`LiaisonsCadrageResourceSecurityTest`, `ParcoursResourceSecurityTest`, `TableauxDeBordResourceSecurityTest`, `PortailParcoursResourceSecurityTest`, `ContexteCertificationResourceSecurityTest`), intégration (`CadrageLiaisonIT`, `ParcoursConduiteIT`, `ParcoursVerrousIT`, `ParcoursPortailIT`, `TableauxDeBordIT` sur PostgreSQL et Kafka), recette (`cadrage.feature`, `parcours.feature` dans `RecetteGherkinIT`). Seuils JaCoCo à 80 % : à `test`, classes du cadrage en lignes et méthodes, l'ensemble en branches ; à `verify`, tout le service en lignes et en fonctions hors MapStruct. Les scénarios du lot effacent leurs données après chaque test (les tests générés comptent les lignes).

## Lancer

- Avec la plateforme : `infra/docker-compose/gimac-dev.sh demarrer`, puis `infra/docker-compose/scripts/verifier-phase0.sh`.
- Image (Jib) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 76e838e | Génération JHipster 8.11.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 3 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 2 classes reprises d'afriland : ServiceTokenProvider, TransitionInterditeException (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 103 tests unitaires et 386 tests d'intégration réussis, image Jib construite (D-54). |
| 2026-09-29 | — | Étape 3, cadrage (D-57, D-58) : ouverture d'une liaison sur un adhérent par un chef de département, conducteur de projet désigné, périmètre dérivé des services souscrits, événements ; second facteur exigé sur `/api/v1/**`, CRUD généré réservé à l'administration ; garde d'immuabilité du CRUD des opérations appliquée à la version réelle ; seuils JaCoCo. 165 tests unitaires et de sécurité, 365 d'intégration et de recette réussis. |
| 2026-09-29 | — | Lot 1, Phase 0 : régénération depuis le JDL du plan en JHipster 9.4.0, corrections Liquibase (jsonb, unicités, index, immuabilité), messagerie (outbox, relais, consommateur idempotent, rebut `.dlq`), jeton de service, sécurité du portail, arborescence hexagonale (D-55). |
| 2026-09-29 | — | Ports sortants `application/port/out` versionnés : la règle `out/` du `.gitignore` généré par JHipster les ignorait, le dépôt distant ne compilait pas ; exception `!src/**/out/` ajoutée et ports `ParticipantDirectoryPort`, `CollaboratorDirectoryPort` reconstitués d'après leurs adaptateurs |
| 2026-09-30 | — | Parcours de la liaison, étapes 4 à 10 (D-56 à D-59) : référentiel de l'annexe A chargé et instancié à l'ouverture, dossier numérique, opérations, ressources, livrables et preuves revérifiés auprès de MS-DOCUMENT, validation FIDO2, qualités, blocages, constat de la couverture (`homologation.coverage-established.v1`), service supplémentaire, fiche de suivi, tableaux de bord par fonction et pilotage, portail cloisonné, contexte interne pour MS-CERTIFICATION, consommation idempotente des événements `certification.*`. |
<!-- GIMAC:FIN -->

# homologation

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

To build the final jar and optimize the homologation application for production, run:

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
