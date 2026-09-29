<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-GATEWAY : plateforme DES du GIMAC

Passerelle d'accès et frontière d'entrée : point d'entrée unique des trois couches, authentification déléguée, jeton interne lié au certificat (RFC 8705), routes déclarées, limitation de débit, masquage des numéros de carte, BFF.

| Élément | Valeur |
|---|---|
| Domaine d'analyse (CA) | D7 |
| Épopée du carnet (CP v2.0) | GW |
| Port | 8080 |
| Paquet Java | `cm.gimac.des.gateway` |
| Entités du JDL (2) | PresentationLayer, Route |
| Règles de gestion (REC-GIMAC-01 §5) | RG-56, RG-58, RG-60, RG-61 |
| Modèle | `ms-gateway.jdl` |

## État

Squelette généré en JHipster 9.4.0, version cible. Code JHipster brut : domaine JPA, DTO MapStruct, services, ressources REST, changelogs Liquibase, messages fr et en. L'architecture hexagonale, l'outbox, l'inbox et les algorithmes du CC restent à écrire (SOC-01, SOC-05).

## Lancer

- Service seul, en développement : `./mvnw` (dépendances : `docker compose -f src/main/docker/services.yml up -d`).
- Avec toute la plateforme : `infra/docker-compose/gimac-dev.sh demarrer` à la racine du dépôt.
- Image de conteneur (Jib, sans Dockerfile, D-48) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Code repris du projet afriland (D-52)

Copié et adapté (paquet `cm.gimac.des.gateway`) ; chaque bean est inactif tant que sa propriété ne vaut pas `true`.

| Classe | Origine dans afriland | Propriété d'activation |
|---|---|---|
| `security/PhantomTokenAuthenticationFilter` | `api-gateway/src/main/java/com/afriland/gateway/security/PhantomTokenAuthenticationFilter.java` | `gimac.securite.jeton-fantome.actif` |
| `security/EmetteurJwtInterne` | `api-gateway/src/main/java/com/afriland/gateway/security/EmetteurJwtInterne.java` | `gimac.securite.jeton-fantome.actif` |
| `security/KeycloakIntrospectionClient` | `api-gateway/src/main/java/com/afriland/gateway/security/KeycloakIntrospectionClient.java` | `gimac.securite.jeton-fantome.actif` |
| `security/RsaKeyConfig` | `api-gateway/src/main/java/com/afriland/gateway/security/RsaKeyConfig.java` | `gimac.securite.jeton-fantome.actif` |
| `security/RsaKeyLoader` | `api-gateway/src/main/java/com/afriland/gateway/security/RsaKeyLoader.java` | sans bean à activer |
| `config/GatewayMtlsCustomizer` | `api-gateway/src/main/java/com/afriland/gateway/config/GatewayMtlsCustomizer.java` | `gimac.securite.mtls.actif` |
| `ratelimit/TokenBucketRateLimiter` | `api-gateway/src/main/java/com/afriland/gateway/ratelimit/TokenBucketRateLimiter.java` | `gimac.passerelle.limitation-debit.actif` |
| `audit/GatewayAuditEventPublisher` | `api-gateway/src/main/java/com/afriland/gateway/audit/GatewayAuditEventPublisher.java` | `gimac.passerelle.journal-decisions.actif` |
| `config/ApplicationProperties` | `api-gateway/src/main/java/com/afriland/gateway/config/ApplicationProperties.java` | sans bean à activer |
| `infrastructure/logging/PiiMaskingLogConverter` | `notifications-service/src/main/java/com/afriland/notifications/infrastructure/logging/PiiMaskingLogConverter.java` | sans bean à activer |

Écarts comblés (D-53) : jeton interne lié au certificat mTLS de la passerelle (`cnf.x5t#S256`, `CertificatMtlsPasserelle`, `EmpreinteCertificat`) ; point JWKS repris (`web/rest/JwksController`) ; mTLS TLS 1.3 seul et mots de passe du magasin sans valeur par défaut ; route propre à afriland retirée et chemins exemptés de jeton ancrés (`CHEMINS_TECHNIQUES`) ; masquage des journaux limité aux numéros vérifiant Luhn, en 6+4. Tests : `LiaisonCertificatPasserelleTest`.

## Supervision de la plateforme (D-61)

Module hexagonal qui donne au super administrateur, en temps réel, l'état des services et des serveurs, les alertes, la configuration effective et les niveaux de journalisation de toute la plateforme.

| Couche | Paquet | Contenu |
|---|---|---|
| Domaine | `domain/supervision` | Modèle (`EtatService`, `EtatServeurs`, `Alerte`…), analyse du format Prometheus (`FormatPrometheus`), masquage des secrets (`MasqueSecrets`), sélection des propriétés (`ProprietesExploitation`) |
| Ports sortants | `domain/supervision/port` | `SondeService` (/management/** des services), `SourceServeurs` (Prometheus), `SourceAlertes` (Alertmanager), `SourceRoutes` (routes de la passerelle) |
| Adaptateurs | `infrastructure/supervision` | WebClient (`ActuatorWebClientAdapter`, `PrometheusWebClientAdapter`, `AlertmanagerWebClientAdapter`), `RoutesPasserelleAdapter` (RouteLocator), `JetonServiceSupervision` (client_credentials) |
| Application | `service/supervision/SupervisionPlateformeService` | Appels parallèles, délai par appel, injoignable sans bloquer les autres, agrégation, flux périodique |
| REST | `web/rest/plateforme/PlateformeResource` | Routes ci-dessous |
| Assemblage | `config/SupervisionConfiguration`, `config/SupervisionProperties` | Beans et propriétés `gimac.supervision` |

| Route | Permission | Contenu |
|---|---|---|
| `GET /api/plateforme/services` | `PLATFORM_SUPERVISION_READ` | Passerelle et chaque service de `gimac.services` : état (UP, DOWN, OUT_OF_SERVICE, INCONNU, INJOIGNABLE), composants, latence, version, métriques (durée, CPU, tas, disque, charge, threads) ; Keycloak ; synthèse |
| `GET /api/plateforme/serveurs` | `PLATFORM_SUPERVISION_READ` | Cibles de Prometheus ; hôtes (node-exporter) et conteneurs (cAdvisor) par PromQL |
| `GET /api/plateforme/alertes` | `PLATFORM_SUPERVISION_READ` | Alertes actives d'Alertmanager |
| `GET /api/plateforme/flux` | `PLATFORM_SUPERVISION_READ` | `text/event-stream`, événement `plateforme` toutes les `intervalle-flux` : instantané des services et nombre d'alertes (-1 si Alertmanager est injoignable) |
| `GET /api/plateforme/configuration` | `PLATFORM_CONFIG_READ` | Routes effectives ; pour chaque service, profils actifs, `gimac.*` et propriétés d'exploitation, secrets masqués (`******`) |
| `GET /api/plateforme/journalisation/{service}?filtre=` | `PLATFORM_CONFIG_READ` | Niveaux de journalisation |
| `POST /api/plateforme/journalisation/{service}` | `PLATFORM_CONFIG_EDIT` | `{ logger, niveau, motif }` : niveau changé à chaud, motif obligatoire (5 à 500 caractères), changement journalisé |

Toutes les routes exigent en plus une session à deux facteurs (`acr` ∈ `gimac.securite.double-facteur.niveaux-acr`, 2 et 3 par défaut) : 401 sans session, 403 sans second facteur ou sans la permission.

Propriétés (`gimac.supervision`) : `passerelle-url`, `delai` (2 s), `intervalle-flux` (5 s), `prometheus-url`, `alertmanager-url`, `keycloak-sante-url` ; adresses Docker dans `application-prod.yml`. Jeton de service : enregistrement OAuth2 `supervision` (client Keycloak `gateway-supervision`, `client_credentials`), variables `GIMAC_SUPERVISION_CLIENT_ID` et `GIMAC_SUPERVISION_CLIENT_SECRET`. Sans jeton, la supervision fonctionne en mode dégradé : santé sans le détail des composants, configuration et journalisation des services refusées par ceux-ci. Le compte de service du client doit porter `ROLE_ADMIN` (points `/management/env` et `/management/loggers` des services JHipster).

Tests : `FormatPrometheusTest`, `MasqueSecretsTest`, `SupervisionPlateformeServiceTest`, `AdaptateursSupervisionTest`, `SupervisionConfigurationTest`, `DoubleFacteurAuthorizationManagerTest` (unitaires) ; `PlateformeResourceSecurityTest` (sécurité, 30 cas, vraie chaîne de sécurité) ; `SupervisionPlateformeHttpTest` (intégration, serveurs HTTP locaux simulant services, Keycloak, Prometheus et Alertmanager). JaCoCo (`seuil-supervision-plateforme`, phase `test`) bloque la construction sous 80 % en lignes et méthodes par classe et en branches sur le module.

Kubernetes (non livré) : installer node-exporter en DaemonSet et la collecte cAdvisor du kubelet (kube-prometheus-stack), un Alertmanager, puis ouvrir dans `infra/kubernetes-gimac/politiques-reseau.yaml` la sortie de la passerelle vers Prometheus (9090), Alertmanager (9093) et le port de gestion de Keycloak (9990), et l'entrée correspondante ; adresses à fixer par `GIMAC_SUPERVISION_PROMETHEUS_URL`, `GIMAC_SUPERVISION_ALERTMANAGER_URL`, `GIMAC_SUPERVISION_KEYCLOAK_SANTE_URL`.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | ed4c1d5 | Génération JHipster 9.4.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 10 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés : liaison du jeton au certificat, JWKS, TLS 1.3, chemins exemptés ancrés, masquage Luhn 6+4 (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 40 tests unitaires et 74 tests d'intégration réussis, image Jib construite ; 13 routes déclarées vers `gimac.services.<service>` (la résolution `lb://` générée ne fonctionne pas sans découverte) ; Valkey pour la sonde Redis ; `List.of` (contrôle de modernité) (D-54). |
| 2026-09-29 | — | Lot 1 : routes `/bff/des` et `/bff/portail` vers l'homologation et la certification, filtre `OpaAuthorize`, 401 sans jeton sur `/bff/**`, permissions acceptées comme autorités (D-55). |
| 2026-09-29 | — | Routes MS-IAM (D-62) : `/bff/{des,portail,console}/iam/moi/**` et `/iam/reauthentification/defi` pour tout utilisateur à deux facteurs, `/bff/console/iam/comptes/**` et `/configuration/**` pour la console ; dépôt de pièces `/bff/{des,portail}/document/documents` (MS-DOCUMENT) ; routes `/interne` jamais relayées ; `RoutesPresentationTest` (routage et réécriture lus dans `application.yml`) ; 144 tests réussis. |
| 2026-09-29 | — | Routes `/bff/des/participant` et `/bff/portail/participant` ; `acr` transmis à OPA, second facteur exigé sur `/bff/**` (D-61) ; `GET /api/session` (matrice des rôles et niveau d'assurance pour l'interface) ; certificat public de test `client-test.pem` versionné (il manquait, `*.pem` étant ignoré) ; 50 tests unitaires réussis. |
| 2026-09-29 | — | Supervision de la plateforme (D-61) : `/api/plateforme/{services,serveurs,alertes,configuration,journalisation,flux}`, module hexagonal, jeton de service `supervision` (client `gateway-supervision`), session à deux facteurs exigée sur `/api/plateforme/**`, permissions `PLATFORM_SUPERVISION_READ`, `PLATFORM_CONFIG_READ`, `PLATFORM_CONFIG_EDIT` ; 128 tests unitaires réussis, couverture du module 100 % en lignes, 99,5 % en méthodes, 96 % en branches (seuil JaCoCo de 80 %). |
| 2026-09-29 | — | Deux violations modernizer de la supervision corrigées (`Optional.orElseThrow`, corps de `/loggers` en `HashMap` car le niveau nul est admis) ; exception `!src/**/out/` au `.gitignore` |
<!-- GIMAC:FIN -->

# gateway

This application was generated using JHipster 9.4.0, you can find documentation and help at [https://www.jhipster.tech/documentation-archive/v9.4.0](https://www.jhipster.tech/documentation-archive/v9.4.0).

This is a "gateway" application intended to be part of a microservice architecture, please refer to the [Doing microservices with JHipster][] page of the documentation for more information.

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

To build the final jar and optimize the gateway application for production, run:

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
