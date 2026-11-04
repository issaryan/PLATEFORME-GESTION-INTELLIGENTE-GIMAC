<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-CERTIFICATION : plateforme DES du GIMAC

Certification et mise en service. Décision de mise en production selon le modèle hybride (REC-GIMAC-01 D-56) : verrou 1, procès-verbal de certification établi à 100 % de couverture et signé par le GIMAC et le participant ; verrou 2, approbation de la sécurité (dossier et fiche de changement lue sans écriture, attestations vérifiées puis archivées) ; visas optionnels de la conformité et de la Direction Générale. Signatures personnelles FIDO2 puis cachet horodaté (D-35). Le circuit des quatre quitus du plan est remplacé ; dossier de mise en service, attestation PDF/A-3, registre et révocation (CER-08 à CER-15) restent au modèle généré.

| Élément | Valeur |
|---|---|
| Lot | Lot 1, module de gestion de la certification (PI-GIMAC-01 v1.0, `docs/lot1`) |
| Récits | CER-01 à CER-07 réécrits selon D-56 (verrous 1 et 2, visas) ; CER-08 à CER-15 |
| Port | 8087 |
| Paquet Java | `cm.gimac.des.certification` |
| Modèle | `ms-certification.jdl` (JDL du plan, §0.2) ; JHipster 9.4.0 ; Cucumber |
| Entités générées (11) | CertificationCircuit, Quitus, SignedDecision, GoLiveDossier, CompletenessCondition, ChangeRecordSnapshot, GoLiveReport, TimestampToken, Attestation, CertificationRegisterEntry, AttestationStatusDecision |
| Entités des verrous (5, `domain/decision`) | ProcesVerbalCertification, SignatureVerrou (immuable), ApprobationSecurite, AttestationSecurite, VisaSecurite ; jetons d'horodatage des cachets dans `timestamp_token` (objet `SIGNATURE`) ; changelog `20261006000001_lot1_verrous_decision.xml` |
| Règles de gestion (REC-GIMAC-01 §5) | RG-34, RG-73 |
| Correspondances avec le plan | `docs/lot1/correspondance.md` |

## Architecture (PI §0.8)

- `domain/` : entités JPA générées, `domain/service` (services de domaine purs), `domain/event` (charges utiles).
- `application/` : ports entrants et sortants, cas d'utilisation transactionnels.
- `infrastructure/` : outbox et relais CloudEvents, consommateur idempotent, adaptateurs réels et de substitution (`gimac.adapters.*`), jeton de service, contexte de corrélation.
- `web/rest/` : ressources générées (`/api/<entités>`) et ressources du lot (`/api/v1/...`, exposées par la passerelle sous `/bff/des/certification` et `/bff/portail/certification`).
- Test d'architecture : couches `application` et `infrastructure`, services de domaine sans Spring.

## Verrous de la décision (D-56 ; `docs/lot1/parcours-backend.md`)

Spécification exécutable : `frontend/packages/lot1/src/services/simule/parcours.ts` (objets `decision` et `portailParcours`) ; chemins et corps : `services/http/parcours.ts`. Toutes les routes exigent un jeton à deux facteurs (`acr` 2 ou 3, D-61) ; le portail exige en plus `institution_id` et répond 404 `HOM_LIAISON_NOT_FOUND` pour une liaison d'une autre institution, sans nom interne (fonctions `CONDUCTEUR`, `CHEF_DES`, `PRODUCTION_MONETIQUE`, `RSSI`, visas absents). Erreurs en `application/problem+json` avec `code`.

| Route (`/api/v1`, passerelle `/bff/des/certification`) | Permission | Effet, refus |
|---|---|---|
| `GET /liaisons/{id}/proces-verbal` | `CER_DOSSIER_READ` | `ProcesVerbalCertification` ; actions `ETABLIR`, `ETABLIR_BLOQUE`, `SIGNER_GIMAC` filtrées par permission |
| `POST /liaisons/{id}/proces-verbal` | `CER_PV_ESTABLISH` | Rang 18 ; `CER_COVERAGE_INCOMPLETE`, `CER_PV_NOT_READY`, `HOM_LIAISON_NOT_FOUND` |
| `POST /liaisons/{id}/proces-verbal/signature-gimac` (`X-Reauth-Assertion`) | `CER_QUITUS_TECHNIQUE_SIGN` | Rang 19 ; `IAM_REAUTH_REQUIRED` (401), `CER_REAUTH_INVALID` (403), `CER_PV_NOT_SIGNABLE` |
| `GET /liaisons/{id}/approbation-securite` | `CER_DOSSIER_READ` | `ApprobationSecurite` ; actions `CONSTITUER_DOSSIER`, `VERIFIER_ATTESTATION`, `ARCHIVER`, `REFERENCER_FICHE`, `RELIRE_FICHE`, `SIGNER_VISA_CONFORMITE`, `SIGNER_VISA_DG` |
| `POST …/approbation-securite/dossier` `{documentId, empreinte, nom}` | `CER_CHANGE_RECORD_LINK` | Rang 20, empreinte revérifiée par MS-DOCUMENT ; `CER_PV_NOT_SIGNED`, `CER_DOCUMENT_INVALID`, `CER_CHANGE_DOSSIER_ALREADY_CONSTITUTED` |
| `PUT …/approbation-securite/fiche` `{reference}` | `CER_CHANGE_RECORD_LINK` | Rang 26, fiche lue aussitôt ; `CER_CHANGE_RECORD_INVALID` |
| `POST …/approbation-securite/fiche/lecture` | `CER_CHANGE_RECORD_LINK` ou `CER_SECURITY_VERIFY` | Lecture, jamais refaite ; approbation si dossier, archivage, fiche approuvée et visas actifs favorables ; `CER_CHANGE_RECORD_MISSING` |
| `POST …/approbation-securite/attestations/{type}/verification` `{conforme, motif}` | `CER_SECURITY_VERIFY` | Rang 23 ; `CER_ATTESTATION_MISSING`, `CER_ATTESTATION_NOT_REQUIRED`, `CER_REJECTION_REASON_REQUIRED`, `CER_ATTESTATIONS_ARCHIVED` |
| `POST …/approbation-securite/archivage` | `CER_SECURITY_VERIFY` | `CER_ATTESTATIONS_INCOMPLETE` |
| `POST …/approbation-securite/visas/{CONFORMITE,DIRECTION_GENERALE}` `{favorable, motif}` (`X-Reauth-Assertion`) | `CER_QUITUS_CONFORMITE_SIGN`, `CER_QUITUS_DG_SIGN` | `CER_VISA_NOT_REQUIRED`, `CER_VISA_ALREADY_SIGNED`, `CER_REJECTION_REASON_REQUIRED` ; avis défavorable : `REFUSEE` |
| `GET /portail/liaisons/{id}/proces-verbal` | `CER_PORTAL_READ` | Action `SIGNER_PARTICIPANT` |
| `POST /portail/liaisons/{id}/proces-verbal/signature` (`X-Reauth-Assertion`) | `CER_PORTAL_SIGN` | Rang 19, participant |
| `GET /portail/liaisons/{id}/approbation-securite` | `CER_PORTAL_READ` | Action `DEPOSER_ATTESTATION` |
| `POST /portail/liaisons/{id}/attestations/{type}` (multipart `fichier`, PDF) | `HOM_PORTAL_SUBMIT` | Dépôt dans MS-DOCUMENT pour le compte du correspondant ; `CER_PV_NOT_SIGNED`, `CER_ATTESTATION_NOT_REQUIRED`, `CER_ATTESTATION_FORMAT`, `CER_ATTESTATION_ALREADY_VERIFIED` |

Attestations exigées : `PCI_DSS` (carte interbancaire, délégataire), `ISO_27001` et `GSMA` (paiement mobile), `COBAC` en plus pour une fintech. Visas : `gimac.certification.visas.{conformite,direction-generale}` (types de projet, vides par défaut). Actes concurrents : 409 `CER_CONCURRENT_UPDATE`.

Événements (outbox, CloudEvents 1.0, extension `institutionid` de la liaison) :

| Type | Sujet |
|---|---|
| `certification.pv-established.v1` `{liaisonId, etabliPar, etabliParId, empreinte}` ; `certification.pv-signed.v1` `{liaisonId, signatures:[{fonction, signataire, signeLe}], empreinte}` ; complément `certification.pv-signature-recorded.v1` `{liaisonId, fonction, signeLe, etat}` | `certification.proces-verbal` |
| `certification.change-record-constituted.v1`, `certification.change-record-referenced.v1`, `certification.attestations-archived.v1`, `certification.security-approved.v1` ; compléments `certification.attestation-submitted.v1`, `certification.attestation-verified.v1`, `certification.visa-signed.v1` | `certification.approbation` |

Consommé : `homologation.coverage-established.v1` `{liaisonId, taux, servicesCouverts}` (sujets `homologation.liaison` et `homologation.coverage`, groupe `certification-couverture-etablie`, idempotent) : le procès-verbal devient établissable.

Adaptateurs (`gimac.adapters.*`, `real` en production) : `homologation-reader` (contexte de certification, `GET /api/v1/interne/liaisons/{id}/contexte-certification`), `reauthentication` (MS-IAM, finalité `QUITUS_SIGN`), `document-deposit` (MS-DOCUMENT : empreinte et dépôt interne). `key-vault-signing` (cachet HMAC et horodatage locaux) et `change-record-reader` (fiche lue approuvée) n'ont qu'un substitut tant que le coffre, l'autorité d'horodatage et l'interface du dispositif de changement ne sont pas livrés.

## US-EXI-25 : socle d'évaluation et caducité (D-125)

Le circuit existant `CertificationCircuit`/`Quitus` consomme `homologation.campaign-result-imported.v1` de manière idempotente, sans module parallèle ni appel entrant de la Sandbox. Une même empreinte est sans effet ; une nouvelle empreinte ouvre le cycle suivant, interrompt le circuit remplacé et passe ses seuls quitus `SIGNE_FAVORABLE` à `CADUC`, avec motif et publication de `certification.quitus-lapsed.v1`. Une signature sur un circuit remplacé ou une empreinte différente est refusée en 409 `CER_SOCLE_OUTDATED`. Les événements historiques sans `empreinteBilan` restent admis sans effet. Contrat : `docs/lot1/sandbox-existant-adapte.md` §12.

Validation du 27/10/2026 avec `TESTCONTAINERS_RYUK_DISABLED=true ./mvnw clean verify` : 219 tests unitaires et 236 tests d'intégration réussis ; contrôles JaCoCo globaux et EXI à 98 % respectés. `BilanSandboxIT.circuitComplet` couvre notamment doublon, changement de socle, caducité et refus de signature. Aucun serveur GIMAC réel ni Compose complet n'a été utilisé pour cette preuve.

## Lancer

- Avec la plateforme : `infra/docker-compose/gimac-dev.sh demarrer`, puis `infra/docker-compose/scripts/verifier-phase0.sh`.
- Image (Jib) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | a7b7881 | Génération JHipster 8.11.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 3 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 2 classes reprises d'afriland : ServiceTokenProvider, TransitionInterditeException (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 88 tests unitaires et 275 tests d'intégration réussis, image Jib construite (D-54). |
| 2026-09-29 | — | Lot 1, Phase 0 : régénération depuis le JDL du plan en JHipster 9.4.0, corrections Liquibase (jsonb, unicités, index, immuabilité), messagerie (outbox, relais, consommateur idempotent, rebut `.dlq`), jeton de service, sécurité du portail, arborescence hexagonale (D-55). |
| 2026-09-29 | — | Ports sortants `application/port/out` versionnés : la règle `out/` du `.gitignore` généré par JHipster les ignorait, le dépôt distant ne compilait pas ; exception `!src/**/out/` ajoutée |
| 2026-09-30 | — | Verrous de la décision (D-56) : procès-verbal de certification (établissement à 100 %, signatures GIMAC et participant FIDO2 puis cachet horodaté, immuables), approbation de la sécurité (dossier et fiche de changement, attestations vérifiées avec rejet motivé puis archivées), visas optionnels, portail cloisonné ; contexte de MS-HOMOLOGATION, consommation de `homologation.coverage-established.v1`, 10 événements `certification.*` ; routes métier et portail à deux facteurs (D-61), CRUD généré réservé à l'administration ; seuils JaCoCo D-61 ; 191 tests unitaires et de sécurité, 233 tests d'intégration dont 8 scénarios Gherkin des verrous (`RecetteGherkinIT`), `verify` réussi. |
| 2026-10-27 | D-125 | US-EXI-25 : consommation idempotente du socle, cycle suivant au changement, quitus antérieurs caducs et refus `CER_SOCLE_OUTDATED` ; `clean verify` : 219 tests unitaires et 236 d'intégration, seuils globaux et EXI 98 % |
| 2026-11-04 | D-48 | Contrôle du socle (CER-06, US-EXI-25) sorti du service généré : `ControleSocleDecisions` (`@Primary`) le pose devant `SignedDecisionServiceImpl`, rétabli comme sur `main` ; requêtes du circuit courant et des quitus d'un circuit dans `infrastructure/persistence` (`CircuitsLiaisonRepository`, `QuitusCircuitRepository`), dépôts générés rétablis |
<!-- GIMAC:FIN -->

# certification

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

To build the final jar and optimize the certification application for production, run:

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
