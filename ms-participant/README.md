<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-PARTICIPANT : plateforme DES du GIMAC

Référentiel des institutions membres et **étapes 1 et 2 du parcours de certification** (prise de contact, adhésion : CR-CERT §4.2.1, D-57) : dossier d'adhésion pièce par pièce, cycle de vie en maker-checker, conventions, services souscrits, correspondants, pièces légales et échéances, annuaire d'escalade.

| Élément | Valeur |
|---|---|
| Domaine d'analyse (CA) | D2 |
| Épopée du carnet (CP v2.0) | PAR |
| Port | 8085 |
| Paquet Java | `cm.gimac.des.participant` |
| Entités du JDL (10) | Institution, AdhesionPiece, Convention, SubscribedService, Correspondent, LegalDocument, Deadline, Interaction, EscalationContact, StatusTransition |
| Règles de gestion (REC-GIMAC-01 §5) | RG-10 à RG-14, RG-68, RG-69, RG-70 |
| Modèle | `ms-participant.jdl` ; JHipster 9.4.0 ; Cucumber |

## État

Régénéré en JHipster 9.4.0 (D-43) avec le socle de la Phase 0 repris de MS-HOMOLOGATION : outbox transactionnelle et relais CloudEvents, consommateur idempotent, rebut `.dlq`, jeton de service, masquage des journaux, arborescence hexagonale (`application`, `domain/service`, `infrastructure`). Première tranche verticale livrée : **la DDRM** (D-60).

> La régénération a changé les identifiants des changelogs Liquibase : **la base de développement de MS-PARTICIPANT doit être recréée** (`docker compose -f src/main/docker/services.yml down -v`).

### Parcours de la DDRM (étapes 1 et 2)

1. **Prise de contact** (rang 1) : le chargé de la DDRM enregistre le candidat (`CANDIDAT`) ; le dossier d'adhésion est ouvert avec les pièces paramétrées dans `gimac.participant.adhesion.pieces` (liste initiale à confirmer par la DDRM).
2. **Dossier d'adhésion** (rang 2) : chaque pièce est déposée (référence MS-DOCUMENT et empreinte SHA-256 vérifiée, jamais le contenu), puis validée ou rejetée avec motif ; les conditions d'adhésion (code participant, mode direct ou indirect, compte de règlement ou de cantonnement, banque marraine) sont arrêtées.
3. **Prononcé** : le chargé propose l'adhésion ; les prérequis sont évalués (pièces obligatoires validées, RG-11, RG-12, banque marraine membre direct actif) ; le chef de la DDRM approuve après réauthentification forte (en-tête `X-Reauth-Assertion`), en personne distincte du demandeur (algorithme 6.25). L'institution passe à `ADHERENT`.
4. **Clôture sans suite** (`CLOS_SANS_SUITE`), par le même circuit, tant qu'aucune liaison n'est ouverte.

`EN_HOMOLOGATION` n'est jamais proposé : il sera atteint à l'ouverture de la première liaison, au cadrage (étape 3, tranche suivante).

### Routes (`/api/v1`, exposées sous `/bff/des/participant`)

| Méthode et chemin | Permission | Objet |
|---|---|---|
| `GET /institutions?statut=&q=&mesDossiers=&page=&taille=` | `PAR_INSTITUTION_READ` | Liste paginée `{contenu, total}` |
| `GET /institutions/indicateurs` | `PAR_INSTITUTION_READ` | Compteurs du tableau de bord de la DDRM |
| `GET /institutions/{id}` | `PAR_INSTITUTION_READ` | Dossier numérique : fiche, adhésion, pièces, transitions, prérequis, transitions permises |
| `GET /institutions/{id}/prerequis?etat=` | `PAR_INSTITUTION_READ` | Prérequis d'un état d'arrivée (PAR-03) |
| `POST /institutions` | `PAR_INSTITUTION_CREATE` | Prise de contact (PAR-01) |
| `PUT /institutions/{id}` | `PAR_INSTITUTION_EDIT` | Données descriptives |
| `PUT /institutions/{id}/conditions-adhesion` | `PAR_INSTITUTION_EDIT` | Conditions de l'adhésion |
| `POST /institutions/{id}/pieces/{code}/depot` | `PAR_ADHESION_INSTRUCT` | Dépôt d'une pièce |
| `POST /institutions/{id}/pieces/{code}/validation` | `PAR_ADHESION_INSTRUCT` | Validation d'une pièce |
| `POST /institutions/{id}/pieces/{code}/rejet` | `PAR_ADHESION_INSTRUCT` | Rejet motivé |
| `POST /institutions/{id}/transitions` | `PAR_TRANSITION_REQUEST` | Proposition de transition (PAR-02) |
| `POST /institutions/{id}/transitions/{t}/approbation` | `PAR_TRANSITION_APPROVE` | Approbation, réauthentification forte |
| `POST /institutions/{id}/transitions/{t}/rejet` | `PAR_TRANSITION_APPROVE` | Rejet motivé |

Erreurs en ProblemDetail RFC 9457 (`code` PAR_*, `message`, `details`, `correlationId`). Les ressources CRUD générées (`/api/<entité>`) sont réservées à l'administration technique (`ROLE_ADMIN`).

### Sécurité (D-61)

- Toute route `/api/v1/**` exige un jeton issu d'une **authentification à deux facteurs** (claim `acr` parmi `gimac.securite.double-facteur.niveaux-acr`, 2 et 3 par défaut) ; la **matrice des rôles** portée par ce jeton décide ensuite route par route (`@PreAuthorize`).
- `/api/v1/interne/**` : jeton de service (`INTERNAL_PARTICIPANT_READ`) ; `/api/v1/portail/**` : institution obligatoire dans le jeton (RG-59).
- Adaptateurs sortants sélectionnés par propriété (`gimac.adapters.*`, `stub` en dev et test, `real` en prod) : registre documentaire (MS-DOCUMENT) et réauthentification forte (MS-IAM, IAM-07).

### Événements (outbox, D-37)

| Type | Sujet |
|---|---|
| `participant.institution-registered.v1` | `participant.institution` |
| `participant.institution-updated.v1` | `participant.institution` |
| `participant.status-changed.v1` | `participant.status` (lu par MS-HOMOLOGATION, HOM-06) |
| `participant.adhesion-piece-recorded.v1` | `participant.adhesion` |
| `participant.transition-requested.v1`, `participant.transition-rejected.v1` | `participant.transition` |

## Tests : définition de fini (D-61)

Quatre types de tests, conformément aux fiches du carnet de produit, et une couverture d'au moins 80 % :

| Type | Où | Commande |
|---|---|---|
| Unitaires (domaine, cas d'utilisation, adaptateurs, architecture) | `*Test` | `./mvnw test` |
| Sécurité (401, 403 sans second facteur, 403 sans permission, accès accordé, sur chaque route) | `RegistreInstitutionsResourceSecurityTest` | `./mvnw test` |
| Intégration (PostgreSQL, Redis, Kafka par Testcontainers) | `*IT` | `./mvnw verify` |
| Recette (Gherkin du carnet, `src/test/resources/.../cucumber/ddrm`) | `RecetteGherkinIT` | `./mvnw verify` |

JaCoCo bloque la construction sous 80 % : à `test`, chaque classe du lot en lignes et en méthodes, et l'ensemble en branches ; à `verify`, tout le service (unitaires et intégration fusionnés) en lignes et en branches. Seuil : propriété `jacoco.seuil`.

## Lancer

- Service seul, en développement : `./mvnw` (dépendances : `docker compose -f src/main/docker/services.yml up -d`).
- Avec toute la plateforme : `infra/docker-compose/gimac-dev.sh demarrer` à la racine du dépôt.
- Image de conteneur (Jib, sans Dockerfile, D-48) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Code repris du projet afriland (D-52)

| Classe | Origine dans afriland | Propriété d'activation |
|---|---|---|
| `config/DecodeurJwtInterne` | `audit-service/.../config/DecodeurJwtInterne.java` | sans bean à activer |
| `config/KafkaErrorHandlingConfiguration` | `audit-service/.../config/KafkaErrorHandlingConfiguration.java` | `gimac.kafka.rebut.actif` |
| `infrastructure/logging/PiiMaskingLogConverter` | `notifications-service/.../logging/PiiMaskingLogConverter.java` | sans bean à activer |

`ServiceTokenProvider` et `TransitionInterditeException`, repris en 8.11.0, sont remplacés par `ServiceTokenConfiguration` et `ServiceTokenInterceptor` (Phase 0) et par `ParticipantException`.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | e9d11b1 | Génération JHipster 8.11.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 3 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 2 classes reprises d'afriland : ServiceTokenProvider, TransitionInterditeException (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 112 tests unitaires et 555 tests d'intégration réussis, image Jib construite ; test généré `InstitutionResourceIT` corrigé (code participant unique) (D-54). |
| 2026-09-29 | — | Régénération JHipster 9.4.0 avec le socle de la Phase 0 ; tranche DDRM (étapes 1 et 2, D-57) : dossier d'adhésion, maker-checker, routes `/api/v1`, double facteur (D-61) ; 266 tests unitaires et de sécurité réussis, seuils JaCoCo de 80 % atteints ; tests d'intégration et de recette écrits, à exécuter sur le poste de développement. |
<!-- GIMAC:FIN -->


# participant

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

To build the final jar and optimize the participant application for production, run:

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
