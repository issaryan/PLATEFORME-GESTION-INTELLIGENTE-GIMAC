<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-PILOTAGE : plateforme DES du GIMAC

Pilotage : tableaux Kanban, projets et jalons, risques, fiches d'action, comités et décisions converties en tâches (D-27).

| Élément | Valeur |
|---|---|
| Domaine d'analyse (CA) | D5 |
| Épopée du carnet (CP v2.0) | PIL |
| Port | 8091 |
| Paquet Java | `cm.gimac.des.pilotage` |
| Entités du JDL (17) | Board, BoardColumn, Task, AcceptanceCriterion, WipOverride, Project, Milestone, MilestoneApproval, Risk, RiskAssessment, ActionSheet, Meeting, MeetingAttendance, Decision, CollaboratorProjection, AbsenceProjection, ProcessedEvent |
| Règles de gestion (REC-GIMAC-01 §5) | RG-20, RG-46, RG-47, RG-49, RG-50, RG-51 |
| Modèle | `ms-pilotage.jdl` |

## État

Squelette généré en JHipster 8.11.0 ; régénération en 9.4.0 prévue lors du passage au gabarit hexagonal (D-43). Code JHipster brut (domaine JPA, DTO MapStruct, services, ressources REST, changelogs Liquibase) conservé tel quel ; les tableaux Kanban, projets, risques et comités du CC restent à écrire (SOC-01, SOC-05).

**Mon activité (D-76, `docs/lot1/mon-activite.md` §2, §3, §5)**, en architecture hexagonale (`application`, `domain/activite`, `domain/service`, `infrastructure`, `web/rest/activite`) :

- Routes `/api/v1/agenda`, `/activites`, `/taches`, `/rapports` (préremplissage, soumission, visa, document), `/equipe`, `/moi/synthese` : jeton à deux facteurs (D-61), puis `PIL_ACTIVITY_OWN` (`/equipe` : `PIL_TEAM_READ`) ; règles d'équipe (403 `PIL_HORS_EQUIPE`) et confiance de tâches (`PIL_TASK_ASSIGN`) contrôlées par les cas d'utilisation. `POST /api/v1/interne/taches` : jeton de service `INTERNAL_PILOTAGE_TASK_CREATE`, idempotent par origine et responsable. `POST /api/v1/interne/taches/cloture` et `/reouverture` (D-77) : même jeton ; corps `{origine: {type?, reference}, responsableId?}` ; les tâches de l'origine créées par un service (jamais une tâche `INTERNE`), restreintes au responsable s'il est donné, sont terminées depuis tout état ouvert (événement `pilotage.task-completed.v1`) ou rouvertes `EN_COURS` si elles étaient terminées ; 200 avec les tâches visées, idempotent.
- Tables `evenement_agenda`, `evenement_participant`, `activite`, `tache`, `tache_historique` (ajout seul, garanti par un déclencheur), `rapport`, `outbox_event` (changelog `20261005090000_pil_mon_activite.xml`). Fuseau `Africa/Douala`, semaines ISO.
- Organisation lue sur MS-IAM (`/api/v1/interne/organisation/{id}`, `/equipe`, `/api/v1/interne/collaborateurs/{id}`, cache de 60 s), documents des rapports vérifiés et relayés en flux depuis MS-DOCUMENT (finalité `RAPPORT_ACTIVITE`), avec le jeton du client `pilotage-service` ; adaptateurs `gimac.adaptateurs.organisation` et `gimac.adaptateurs.documents` : `real` (profil `prod`) ou `stub`.
- Événements `pilotage.task-created.v1`, `pilotage.task-assigned.v1`, `pilotage.task-completed.v1`, `pilotage.report-submitted.v1`, `pilotage.report-visaed.v1` par l'outbox (CloudEvents 1.0, sujets `pilotage.task` et `pilotage.report`), sans contenu de rapport.
- Tests : unitaires du domaine (transitions, périodes, rapports, préremplissage), des adaptateurs, et d'intégration REST (`web/rest/activite/*IT`, PostgreSQL et Kafka par Testcontainers).

**Tâches confiées et pouvoirs des chefs (D-81, `docs/lot1/pouvoirs-hierarchiques.md` §6)** :

- Matrice des pouvoirs lue sur MS-IAM avec le jeton du client `pilotage-service` : `GET /api/v1/interne/pouvoirs/{id}` (`actions` par portée `AUCUNE` < `DIRECTS` < `EQUIPE` < `UNITES`, `profilsDemandables`, `subordonnes`, `unitesDirigees`) et `GET /api/v1/interne/organisation/{id}/perimetre?portee=` (`comptes`, `unites`), cache de 60 s ; le chemin des unités du rattachement (`chemin`) dit si un compte est membre d'une unité ou d'une sous-unité. L'adaptateur simulé (`OrganisationStubAdapter`) porte les unités du jeu initial, leurs responsables et la matrice par défaut des responsables ; les tests la changent compte par compte.
- `POST /api/v1/taches` : `responsableId` (une personne), `responsables` (2 à 50, une tâche par personne liées par `groupeId` ; réponse 201 : le groupe) ou `uniteCible` (tâche `A_PRENDRE`, sans responsable, visible des membres de l'unité et des sous-unités et de ceux qui la dirigent) ; `GET /api/v1/taches/groupes/{groupeId}` (tâches visibles et avancement `{ total, terminees, enRetard }`, annulées hors total) ; `POST /api/v1/taches/{id}/prise` (membre de l'unité), `/attribution` (`{ responsableId }`, responsable de l'unité ou créateur), `/relance` (créateur ou `TACHE_SUIVRE`, une par 24 h, sinon 429 `PIL_RELANCE_RECENTE`) ; vue `GET /api/v1/taches?vue=unite` (à prendre par défaut, `etat=` pour les autres).
- Contrôles : confier, réaffecter, attribuer : `PIL_TASK_ASSIGN` et `TACHE_CONFIER` couvrant la personne (ou, pour une unité, portée `UNITES` et unité dirigée) ; vue `equipe`, lecture des tâches d'un subordonné, relance : `PIL_TEAM_READ` et `TACHE_SUIVRE` ; agenda, activités et `/equipe` : `PIL_TEAM_READ` et `ACTIVITE_LIRE` ; rapports reçus, lecture et visa : `PIL_TEAM_READ` et `RAPPORT_VISER` (le destinataire vise toujours). La permission du jeton reste une condition nécessaire : le profil ouvre la fonction, la matrice en règle la portée ; hors portée : 403 `PIL_HORS_EQUIPE`.
- Changelog `20261006090000_pil_taches_confiees.xml` : `responsable_id` facultatif, colonnes `groupe_id`, `unite_cible`, `relancee_le`, contrainte `ck_tache_responsable` (sans responsable : à prendre, ou annulée avant d'avoir été prise).
- Événements `pilotage.task-offered.v1` (`{ tacheId, titre, uniteCible, destinataires, acteurId, le }`, membres actifs de l'unité et des sous-unités dans la portée du créateur), `pilotage.task-reminded.v1` (`{ tacheId, titre, responsableId, acteurId, le }`), et `pilotage.task-assigned.v1` (avec `titre` et `echeance`) pour chaque tâche d'un groupe, chaque prise et chaque attribution ; `pilotage.task-created.v1` gagne `groupeId` et `uniteCible`.

**Processus d'unité (D-87, `docs/lot1/processus-unite.md`, mise en œuvre §5)** : définitions versionnées
(BROUILLON, PUBLIEE, RETIREE) d'une unité, graphe d'étapes (intervenants PROFIL, PERSONNE, AU_LANCEMENT, CHEF_UNITE,
LANCEUR, N_PLUS_1_LANCEUR avec profil de remplacement), contrôle de publication (Kahn…), instances : tâches d'origine
`PROCESSUS` (à prendre réservées au profil), livrables MS-DOCUMENT (SHA-256), champs, signatures FIDO2 vérifiées par
MS-IAM (`SIGNATURE_PROCESSUS`), validation et renvoi, réaffectation, suspension, annulation ; départ d'un compte
(`iam.account-administered.v1`, sujet `iam.account`) ; routes `/api/v1/processus/**`, événements `pilotage.process-*`.
Démonstration (profil dev) : `OUVERTURE_FLUX_RESEAU` du pôle réseau.

**Tâche de l'approbateur d'une transition (D-98, `docs/lot1/lancement-processus.md` §8.8)** : consommateur
`TransitionsParticipantConsumer` (groupe `pilotage-transitions-participant`, sujet `participant.transition`,
`gimac.consommateurs.transitions.sujet`) : `participant.transition-requested.v1` crée une tâche à prendre d'origine
`DECISION` par les porteurs de `PAR_TRANSITION_APPROVE`, notifiée vers l'écran de la proposition ;
`participant.transition-approved.v1` la termine (définitive) ou la remet à prendre (première de deux approbations) ;
`participant.transition-rejected.v1` la termine.

## Lancer

- Service seul, en développement : `./mvnw` (dépendances : `docker compose -f src/main/docker/services.yml up -d`).
- Avec toute la plateforme : `infra/docker-compose/gimac-dev.sh demarrer` à la racine du dépôt.
- Image de conteneur (Jib, sans Dockerfile, D-48) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Code repris du projet afriland (D-52)

Copié et adapté (paquet `cm.gimac.des.pilotage`) ; chaque bean est inactif tant que sa propriété ne vaut pas `true`.

| Classe | Origine dans afriland | Propriété d'activation |
|---|---|---|
| `config/DecodeurJwtInterne` | `audit-service/src/main/java/com/afriland/audit/config/DecodeurJwtInterne.java` | sans bean à activer |
| `config/KafkaErrorHandlingConfiguration` | `audit-service/src/main/java/com/afriland/audit/config/KafkaErrorHandlingConfiguration.java` | `gimac.kafka.rebut.actif` |
| `infrastructure/logging/PiiMaskingLogConverter` | `notifications-service/src/main/java/com/afriland/notifications/infrastructure/logging/PiiMaskingLogConverter.java` | sans bean à activer |
| `adapter/outbound/securite/ServiceTokenProvider` | `administration-service/src/main/java/com/afriland/admin/adapter/outbound/cognitive/ServiceTokenProvider.java` | `gimac.securite.jeton-service.actif` |
| `domain/exception/TransitionInterditeException` | `dossiers-service/src/main/java/com/afriland/dossiers/domain/exception/TransitionInterditeException.java` | sans bean à activer |

Écarts comblés (D-53) : `ValidateurLiaisonCertificat` et option du décodeur exigeant la liaison au certificat (RFC 8705) ; masquage des journaux limité aux numéros vérifiant Luhn, en 6+4 (RG-52).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | ae9f579 | Génération JHipster 8.11.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 3 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 2 classes reprises d'afriland : ServiceTokenProvider, TransitionInterditeException (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 151 tests unitaires et 735 tests d'intégration réussis, image Jib construite ; `@DecimalMax("0")` généré pour `max(0.9)` rétabli à `0.9` sur le coefficient de maîtrise (Risk, RiskAssessment) ; tests ajustés (D-54). |
| 2026-09-29 | — | `.gitignore` : exception `!src/**/out/`, pour que les ports sortants `application/port/out` soient versionnés |
| 2026-10-05 | - | Mon activité (D-76) : agenda, activités du jour, tâches, rapports, équipe et synthèse ; jeton de service `pilotage-service` vers MS-IAM et MS-DOCUMENT ; outbox des événements `pilotage.*` ; permissions métier (`PIL_*`, `INTERNAL_*`) conservées dans les autorités du jeton |
| 2026-10-06 | - | Tâches confiées (D-81 §6) : à plusieurs personnes (groupe), à une unité (à prendre, prise, attribution), relance ; contrôles par la matrice des pouvoirs de MS-IAM (`TACHE_CONFIER`, `TACHE_SUIVRE`, `ACTIVITE_LIRE`, `RAPPORT_VISER`) ; événements `pilotage.task-offered.v1` et `pilotage.task-reminded.v1` |
| 2026-10-07 | - | Processus d'unité (D-87) : définitions, contrôle, publication, versions ; moteur d'exécution, tâches liées, signatures FIDO2, départ d'un compte ; changelog `20261007090000_pil_processus_unite.xml` |
<!-- GIMAC:FIN -->

# pilotage

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

To build the final jar and optimize the pilotage application for production, run:

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
