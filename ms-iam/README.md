<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-IAM : plateforme DES du GIMAC

Identités, rôles à cinq niveaux (D-31), délégations (élévation 60 min et suppléance 72 h, D-20, D-36), authentification forte FIDO2, revue trimestrielle des accès.

| Élément | Valeur |
|---|---|
| Domaine d'analyse (CA) | D1 |
| Épopée du carnet (CP v2.0) | IAM |
| Port | 8081 |
| Paquet Java | `cm.gimac.des.iam` |
| Entités du JDL (10) | Collaborator, Role, Permission, RoleGrant, Delegation, UserSession, Assignment, Availability, AccessReview, AccessReviewDecision |
| Règles de gestion (REC-GIMAC-01 §5) | RG-01, RG-02, RG-57, RG-65, RG-66, RG-67, RG-74 |
| Modèle | `ms-iam.jdl` |

## État

Squelette JHipster 9.4.0 (CRUD généré réservé à l'administration technique, `ROLE_ADMIN`) et, en architecture hexagonale (`domain`, `application`, `infrastructure`, `web.rest.lot1`), l'administration complète des comptes et de l'authentification depuis la plateforme (D-62) et la réauthentification forte FIDO2 (IAM-07). Keycloak reste le moteur d'authentification ; nul n'ouvre sa console.

### Administration des comptes et de la plateforme (D-62)

- **Annuaire** (port `application/port/out/Annuaire`) : adaptateur réel `infrastructure/idp/KeycloakAnnuaire` (API d'administration, compte de service `iam-service` du domaine `gimac`, rôles `manage-users`, `view-users`, `query-users`, `query-groups`, `manage-realm`, `view-realm`), choisi par `gimac.adapters.annuaire=real` (profil prod) ; adaptateur en mémoire `AnnuaireEnMemoire` en développement et en test (comptes de démonstration `super.admin`, `rssi`). Panne de Keycloak : erreur 503 `IAM_IDP_UNAVAILABLE`, jamais de valeur par défaut.
- **Règles** (`domain/service/ReglesAdministration`, `PolitiqueMotDePasse`, services purs) : identifiant `^[a-z][a-z0-9._-]{2,49}$`, courriel unique, profils connus seulement, profils privilégiés (`ROLE_SUPER_ADMIN`, `ROLE_RSSI`) attribués par le seul détenteur de `IAM_ROLE_PRIVILEGED_GRANT`, nul ne s'administre soi-même, le dernier super administrateur ne peut être retiré, motif obligatoire des actes, planchers de la politique de sécurité (12 caractères au moins, OTP 6 ou 8 chiffres sur 30 à 60 s, sessions 5 à 120 min et 1 à 24 h, protection contre la force brute obligatoire, 3 à 10 échecs).
- **Journal d'administration** (`journal_administration`, insertion seule, déclencheur PostgreSQL) et événements d'audit par l'outbox, dans la transaction de l'acte.
- **Premier accès** : étapes restantes calculées (`UPDATE_PASSWORD`, `CONFIGURE_TOTP`, `VERIFY_EMAIL`, `UPDATE_PROFILE`, `FICHE`) ; la fiche (téléphone, langue fr/en/es, fonction, charte acceptée) termine le premier accès côté plateforme ; les actions exigées sont conduites par les pages de Keycloak au thème GIMAC.
- **Réauthentification forte FIDO2** (`ReauthentificationService`, webauthn4j) : enrôlement des clés depuis « Mon compte », défi à usage unique lié au sujet et à la finalité (2 min), vérification pour le service qui porte la décision (compteur de signatures mis à jour, assertion jamais rejouable, résultat publié vers l'audit). Actes d'administration sensibles : finalité `ADMIN_SENSITIVE` (suppression, profils, politique, composition d'un profil), 428 sans assertion, 403 `IAM_REAUTH_INVALID` si elle est refusée.

### Routes (`/api/v1`)

| Route | Exposée sous | Permission |
|---|---|---|
| `GET /comptes`, `/comptes/indicateurs`, `/comptes/{id}`, `/comptes/{id}/sessions` | `/bff/console/iam` | `IAM_ACCOUNT_READ` |
| `GET /comptes/journal` | `/bff/console/iam` | `IAM_JOURNAL_READ` |
| `POST /comptes` | `/bff/console/iam` | `IAM_ACCOUNT_CREATE` |
| `PUT /comptes/{id}` | `/bff/console/iam` | `IAM_ACCOUNT_EDIT` |
| `PUT /comptes/{id}/profils` (FIDO2) | `/bff/console/iam` | `IAM_ACCOUNT_ROLES` |
| `POST /comptes/{id}/activation`, `/desactivation` | `/bff/console/iam` | `IAM_ACCOUNT_STATUS` |
| `DELETE /comptes/{id}?motif=` (FIDO2) | `/bff/console/iam` | `IAM_ACCOUNT_DELETE` |
| `POST /comptes/{id}/actions-requises`, `/second-facteur/reinitialisation`, `/mot-de-passe-provisoire`, `/sessions/fermeture` | `/bff/console/iam` | `IAM_ACCOUNT_SECURITY` |
| `GET` / `PUT` (FIDO2) `/configuration/politique-securite` | `/bff/console/iam` | `PLATFORM_SECURITY_POLICY_READ` / `_EDIT` |
| `GET` / `PUT /configuration/messagerie`, `POST /configuration/messagerie/test` | `/bff/console/iam` | `PLATFORM_MESSAGING_EDIT` |
| `GET /configuration/profils`, `/configuration/permissions` | `/bff/console/iam` | `IAM_ACCOUNT_READ` |
| `PUT /configuration/profils/{code}/permissions` (FIDO2) | `/bff/console/iam` | `IAM_PROFILE_EDIT` |
| `/moi` (GET, PUT), `/moi/premiere-configuration`, `/moi/actions`, `/moi/sessions`, `/moi/cles-securite` | `/bff/{des,portail,console}/iam` | tout utilisateur à deux facteurs |
| `POST /reauthentification/defi` | `/bff/{des,portail,console}/iam` | tout utilisateur à deux facteurs |
| `POST /interne/reauthentifications/verification` | jamais relayée | jeton de service `INTERNAL_IAM_READ` |
| `GET /interne/collaborateurs/{id}`, `GET /interne/collaborateurs?profil=` | jamais relayée | jeton de service `INTERNAL_IAM_READ` (annuaire IAM-04 : profils, rattachement, activité ; conducteurs de projet, D-58) |

Erreurs : ProblemDetail (`code`, `message`, `details`, `correlationId`). Sécurité (D-61) : `/api/v1/**` exige un jeton à deux facteurs (`acr` 2 ou 3) ; `/api/v1/interne/**` un jeton de service ; le CRUD généré `/api/**` le rôle `ROLE_ADMIN`.

### Événements (outbox, CloudEvents)

`iam.account-administered.v1` (sujet `iam.account`), `iam.configuration-changed.v1` (`iam.configuration`), `iam.reauthentication-verified.v1` (`iam.reauthentication`).

## Tests : définition de fini (D-61)

| Type | Tests |
|---|---|
| Unitaires | `ReglesAdministrationTest`, `PolitiqueMotDePasseTest`, `AdministrationComptesServiceTest`, `MonCompteEtConfigurationTest`, `JournalActesTest`, `ReauthentificationServiceTest` (authentificateur FIDO2 émulé, webauthn4j-test : enrôlement, assertion, non-rejeu, sujet et finalité liés, expiration) |
| Sécurité | `IamRoutesSecurityTest` (toutes les routes : 401, 403 sans second facteur, 403 sans la permission, accès avec elle, 428 et 403 de la réauthentification, route interne), `TechnicalStructureTest` |
| Intégration | `ParcoursAdministrationIT` (PostgreSQL et Kafka par Testcontainers, clé FIDO2 émulée de bout en bout), `KeycloakAnnuaireIT` (vrai Keycloak 26.7.4 et domaine `gimac` généré : attributs du profil utilisateur, profils, politique, messagerie, erreurs), CRUD généré |
| Recette | `RecetteGherkinIT` : `administration-comptes.feature`, `premier-acces.feature` |

Seuils JaCoCo (80 %) : à `test`, chaque classe de la tranche en lignes et en méthodes, l'ensemble en branches ; à `verify`, tout le service en lignes et en méthodes hors MapStruct. `KeycloakAnnuaireIT` démarre Keycloak par Testcontainers (`quay.io/keycloak/keycloak:26.7.4-0`) ou utilise un Keycloak déjà lancé : `GIMAC_KEYCLOAK_IT_URL`, `GIMAC_KEYCLOAK_IT_SECRET` (secret du client `iam-service`).

## Lancer

- Service seul, en développement : `./mvnw` (dépendances : `docker compose -f src/main/docker/services.yml up -d`).
- Avec toute la plateforme : `infra/docker-compose/gimac-dev.sh demarrer` à la racine du dépôt.
- Image de conteneur (Jib, sans Dockerfile, D-48) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Code repris du projet afriland (D-52)

Copié et adapté (paquet `cm.gimac.des.iam`) ; chaque bean est inactif tant que sa propriété ne vaut pas `true`.

| Classe | Origine dans afriland | Propriété d'activation |
|---|---|---|
| `config/DecodeurJwtInterne` | `audit-service/src/main/java/com/afriland/audit/config/DecodeurJwtInterne.java` | sans bean à activer |
| `config/KafkaErrorHandlingConfiguration` | `audit-service/src/main/java/com/afriland/audit/config/KafkaErrorHandlingConfiguration.java` | `gimac.kafka.rebut.actif` |
| `infrastructure/logging/PiiMaskingLogConverter` | `notifications-service/src/main/java/com/afriland/notifications/infrastructure/logging/PiiMaskingLogConverter.java` | sans bean à activer |
| `adapter/outbound/port/KeycloakAdminPort` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/KeycloakAdminPort.java` | sans bean à activer |
| `adapter/outbound/port/GroupeAgenceKc` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/GroupeAgenceKc.java` | sans bean à activer |
| `adapter/outbound/port/IdentityProviderInfo` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/IdentityProviderInfo.java` | sans bean à activer |
| `adapter/outbound/port/ProfilKeycloak` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/ProfilKeycloak.java` | sans bean à activer |
| `adapter/outbound/port/SessionInfo` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/SessionInfo.java` | sans bean à activer |
| `adapter/outbound/port/UtilisateurRealm` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/port/UtilisateurRealm.java` | sans bean à activer |
| `adapter/outbound/keycloak/KeycloakAdminAdapter` | `utilisateurs-service/src/main/java/com/afriland/users/adapter/outbound/keycloak/KeycloakAdminAdapter.java` | `gimac.iam.keycloak-admin.actif` |
| `config/KeycloakConfig` | `utilisateurs-service/src/main/java/com/afriland/users/config/KeycloakConfig.java` | `gimac.iam.keycloak-admin.actif` |
| `config/KeycloakProperties` | `utilisateurs-service/src/main/java/com/afriland/users/config/KeycloakProperties.java` | sans bean à activer |
| `config/KeycloakClientRegistrationConfig` | `utilisateurs-service/src/main/java/com/afriland/users/config/KeycloakClientRegistrationConfig.java` | `gimac.iam.keycloak-admin.actif` |
| `adapter/outbound/securite/ServiceTokenProvider` | `administration-service/src/main/java/com/afriland/admin/adapter/outbound/cognitive/ServiceTokenProvider.java` | `gimac.securite.jeton-service.actif` |

Écarts comblés (D-53) : `ValidateurLiaisonCertificat` et option du décodeur exigeant la liaison au certificat (RFC 8705) ; masquage des journaux limité aux numéros vérifiant Luhn, en 6+4 (RG-52).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | c1b7131 | Génération JHipster 9.4.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 3 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 11 classes reprises d'afriland : GroupeAgenceKc, IdentityProviderInfo, KeycloakAdminAdapter, KeycloakAdminPort, KeycloakClientRegistrationConfig, KeycloakConfig, KeycloakProperties, ProfilKeycloak, ServiceTokenProvider, SessionInfo, UtilisateurRealm (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 101 tests unitaires et 375 tests d'intégration réussis, image Jib construite ; `KeycloakProperties` déplacée dans l'adaptateur (test d'architecture) ; `List.of` (contrôle de modernité) (D-54). |
| 2026-09-29 | — | Permissions métier (HOM_*, CER_*, INTERNAL_*) acceptées comme autorités, en plus de ROLE_* (D-55). |
| 2026-09-29 | — | Administration des comptes et de l'authentification depuis la plateforme (D-62) : annuaire Keycloak, premier accès, « Mon compte », politique de sécurité, messagerie, profils, journal ; réauthentification forte FIDO2 (IAM-07). 170 tests unitaires et de sécurité, 396 d'intégration et de recette réussis ; seuils JaCoCo atteints. |
<!-- GIMAC:FIN -->

# iam

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

To build the final jar and optimize the iam application for production, run:

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
