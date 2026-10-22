<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-GATEWAY : plateforme DES du GIMAC

Passerelle d'accès et frontière d'entrée : point d'entrée unique des trois couches, connexion sur la plateforme (octroi direct, D-65), jeton interne lié au certificat (RFC 8705), routes déclarées, limitation de débit, masquage des numéros de carte, BFF.

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

## Routes des couches de présentation (PI-GIMAC-01 §0.6)

Déclarées dans `src/main/resources/config/application.yml` (`spring.cloud.gateway.server.webflux.routes`) et vérifiées par `RoutesPresentationTest`. Chaque route passe d'abord le filtre `OpaAuthorize` (politique `gimac.authz`, `infra/docker-compose/opa/politiques/lot1_authz.rego`) sur le chemin d'origine, puis est réécrite vers l'API v1 du service ; `TokenRelay` est un filtre par défaut. Toute route `/bff/**` exige une session (401 sinon) et un second facteur (`acr` 2 ou 3, D-61). Les routes JHipster `/services/{service}/**` sont retirées (elles relayaient le CRUD généré des services, hors OPA, à tout jeton portant `ROLE_ADMIN`) et `/services/**` est refusé à tous (`denyAll`) : `RoutesPresentationTest` vérifie que toute route est une route `/bff` filtrée d'abord par `OpaAuthorize`, `CheminsServicesSecuriteTest` le refus de `/services/**` même avec `ROLE_ADMIN`.

| Chemin public | Service | Chemin réécrit | Particularités |
|---|---|---|---|
| `/bff/des/homologation/**` | homologation | `/api/v1/**` | |
| `/bff/portail/homologation/**` | homologation | `/api/v1/portail/**` | `institution_id` exigé (RG-59) |
| `/bff/des/certification/**` | certification | `/api/v1/**` | |
| `/bff/portail/certification/**` | certification | `/api/v1/portail/**` | `institution_id` exigé |
| `/bff/des/participant/**` | participant | `/api/v1/**` | |
| `/bff/portail/participant/**` | participant | `/api/v1/portail/**` | `institution_id` exigé |
| `/bff/{des,portail,console}/iam/moi/**`, `/iam/reauthentification/defi` | iam | `/api/v1/**` | tout utilisateur à deux facteurs (D-62) ; filtre `FermetureAutresSessions`, corps borné à 3 Mo (photo) |
| `/bff/{des,console}/iam/avatars/**` | iam | `/api/v1/avatars/**` | photos des collègues, `GET` seulement (mon-compte.md §1) ; aucune route depuis le portail |
| `/bff/console/iam/comptes/**`, `/bff/console/iam/configuration/**` | iam | `/api/v1/**` | console seulement |
| `/bff/{des,portail}/document/documents` | document | `/api/v1/documents` | dépôt d'une pièce |
| `/bff/{des,console,portail}/document/depots/**` | document | `/api/v1/depots/**` | dépôts fragmentés (D-74, `docs/lot1/depots-fragmentes.md` §3) : ouverture, parties en octets bruts, état, fin, abandon ; OPA : l'une des permissions de dépôt ou `COL_MESSAGE_SEND` (DES, console), `HOM_PORTAL_SUBMIT` et institution (portail), autres chemins et méthodes refusés ; corps borné à 17 Mio (`RequestSize`, une partie de 16 Mio), 413 au-delà |
| `/bff/{des,console}/collaboration/messagerie/**` | collaboration | `/api/v1/messagerie/**` | messagerie (D-64) : tout membre interne, DES et console (jeton portant une institution refusé par OPA) ; corps multipart relayés en flux, taille annoncée bornée à 101 Mo (`RequestSize`, 5 pièces de 20 Mo, partie JSON et délimiteurs), 413 au-delà |

Messagerie, règles OPA (`docs/lot1/messagerie.md` §1) : `COL_MESSAGE_READ` pour `GET annuaire`, `conversations`, `conversations/{id}`, `conversations/{id}/messages`, `non-lus`, `pieces/{id}` et `POST conversations/{id}/lu` ; `COL_MESSAGE_SEND` pour `POST conversations` et `POST conversations/{id}/messages` ; tout autre chemin ou méthode sous `/bff/{des,console}/collaboration/messagerie` est refusé, de même que tout jeton portant une institution ; `/bff/portail/collaboration/**` est refusé en entier (aucune route ne le relaie). Aucune limite de taille de corps n'est posée ailleurs dans la passerelle (pas de `max-in-memory-size` sur les routes relayées) ; un mandataire placé devant la passerelle doit admettre au moins 101 Mo.

## Canal poussé des couches (contrat de messagerie §5)

`GET /bff/{des|portail|console}/live?sujets=homologation,certification,collaboration,notification&depuis=<id>` : flux `text/event-stream` servi par la passerelle elle-même (hors OPA), ouvert par `EventSource` avec le cookie de session (`frontend/packages/socle/src/live/live.ts`). Chaque événement porte une ligne `id:` et `data: {"id","type","subject","topic"}` ; aucune donnée métier n'est relayée : le frontend invalide ses requêtes du sujet et relit l'état par l'API, sous ses propres droits.

| Règle | Comportement |
|---|---|
| Session | 401 sans session ; 403 sans second facteur (`acr` 2 ou 3) ; 404 pour une couche inconnue |
| Sujets | `sujets` absent : tous ceux de la couche ; inconnus ignorés. DES et console : homologation, certification, collaboration, presence, notification ; portail : homologation, certification |
| Portail | `institution_id` exigé (403 sinon) ; seuls les événements dont l'enveloppe porte `institutionid` égal à celui de l'utilisateur sont remis |
| Collaboration | `collaboration.message-sent.v1` n'est remis qu'aux utilisateurs dont le `sub` (identifiant IAM) figure dans `data.destinataires` ; jamais au portail |
| Notification (D-80) | sujet `notification` (Kafka `notification.inapp`, `notification.inapp-created.v1` de MS-NOTIFICATION) : remis au seul compte de `data.destinataires`, au DES et à la console, jamais au portail ; `acteur` lu dans `data.acteurId` |
| Reprise | tampon en mémoire des 500 derniers événements (5 min) ; `Last-Event-ID`, sinon `depuis` : événements postérieurs rejoués ; identifiant inconnu ou expiré : tout le tampon (un doublon ne fait qu'invalider une requête) |
| Maintien | commentaire `:ouvert` à l'ouverture (l'état « connecté » s'affiche aussitôt), `:battement` toutes les 25 s ; en-tête `X-Accel-Buffering: no` |
| Débit | tampon de 256 événements par connexion, les plus anciens abandonnés si le navigateur ne suit pas ; les autres connexions ne sont pas freinées |
| Durée | connexion close après 30 min : le navigateur se reconnecte avec son dernier identifiant et ses droits sont réévalués |

Source : sujets Kafka du lot 1 (`homologation.{referential,liaison,operation,campaign,coverage,notification}`, `certification.{circuit,quitus,dossier,attestation,notification}`, `collaboration.message`, `notification.inapp`), enveloppes CloudEvents 1.0 en mode structuré ; sujet fonctionnel = premier segment du sujet Kafka. Chaque instance a son propre groupe de consommateurs (`gateway-live-<aléa>`), positionné à la fin des sujets, sans validation de décalage : c'est une diffusion de notifications, pas un traitement. Courtier injoignable : le consommateur réessaie en arrière-plan sans bloquer le démarrage, le canal reste servi.

| Couche | Paquet | Contenu |
|---|---|---|
| Domaine | `domain/live` | `EvenementLive`, `VueEvenement` (données remises), `Couche`, `Abonne` (règles de remise), `TamponRejeu` |
| Port sortant | `domain/live/port/SourceEvenements` | Flux chaud des événements |
| Adaptateurs | `infrastructure/live` | `KafkaSourceEvenements` (Spring Kafka, cycle de vie du contexte), `LectureCloudEvent` (enveloppe → événement) |
| Application | `service/live/CanalDirectService` | Reprise et direct sans trou ni doublon, filtrage, maintien, débit et durée par connexion |
| REST | `web/rest/live/CanalDirectResource` | `GET /bff/{couche}/live` |
| Assemblage | `config/LiveConfiguration`, `config/LiveProperties`, `security/RevendicationsSession` | Beans, propriétés `gimac.live`, claims `sub` et `institution_id` |

Propriétés (`gimac.live`) : `actif` (vrai ; faux dans les tests : source vide), `bootstrap-servers` (`spring.kafka.bootstrap-servers`, soit `SPRING_KAFKA_BOOTSTRAP_SERVERS`, à défaut `spring.cloud.stream.kafka.binder.brokers`, puis `localhost:9092`), `sujets`, `kafka.*` (propriétés supplémentaires du consommateur, sécurité du courtier), `taille-tampon` (500), `duree-tampon` (5m), `battement` (25s), `duree-max-connexion` (30m), `tampon-connexion` (256).

Tests : `AbonneTest`, `TamponRejeuTest`, `LectureCloudEventTest`, `KafkaSourceEvenementsTest`, `CanalDirectServiceTest`, `LiveConfigurationTest`, `RevendicationsSessionTest` (unitaires) ; `CanalDirectResourceTest` (WebFlux et sécurité sur la vraie chaîne : 401, 403, 404, destinataire, autre utilisateur, portail, reprise) ; `KafkaSourceEvenementsIT` (intégration, courtier Kafka Testcontainers). JaCoCo (`seuil-canal-pousse`, phase `test`) bloque la construction sous 80 % en lignes et méthodes par classe et en branches sur le module.

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

## Exploitation de l'infrastructure depuis la console (D-78)

Contrat : `docs/exploitation/operations-infrastructure.md`. Gestes d'exploitation (redémarrer, arrêter, démarrer, mettre à l'échelle) et lecture des charges de travail et de leurs journaux, servis par un contrôleur propre de la passerelle (aucun relais vers un service).

| Couche | Paquet | Contenu |
|---|---|---|
| Domaine | `domain/exploitation` | Modèle (`Charge`, `Operation`, `JournauxCharge`…), `ChargesProtegees` (motifs à `*`), `MasqueJournaux` (JWT, Bearer, mots de passe et secrets comme `infra/ci/diagnostiquer-serveur.sh`, carte Luhn 13 à 19 chiffres en 6+4), `ExploitationException` (codes du contrat) |
| Ports sortants | `domain/exploitation/port` | `Orchestrateur`, `JournalOperations`, `VerificateurReauthentification`, `PublicationOperations` |
| Adaptateurs | `infrastructure/exploitation` | `DockerOrchestrateur` (API du moteur par le mandataire, `FluxMultiplexeDocker`), `KubernetesOrchestrateur` (API du cluster, `CompteServiceKubernetes`), `OrchestrateurNonConfigure`, `JournalOperationsR2dbc` (table `operation_infrastructure`) et `JournalOperationsMemoire`, `IamVerificateurReauthentification`, `PublicationOperationsAudit` |
| Application | `service/exploitation/ExploitationInfrastructureService` | Règles, verrou par charge, geste en tâche de fond, journal, audit, diffusion |
| REST | `web/rest/plateforme/InfrastructureResource` | Routes ci-dessous ; opérations poussées sur `GET /api/plateforme/flux` |
| Assemblage | `config/ExploitationConfiguration`, `config/ExploitationProperties` | Choix de l'orchestrateur, propriétés `gimac.orchestrateur` |

| Route | Permission | Contenu |
|---|---|---|
| `GET /api/plateforme/infrastructure/charges` | `PLATFORM_OPS_READ` | Charges du périmètre (état, santé, répliques, redémarrages, image, démarrage, protection, gestes permis) ; en-tête `X-Orchestrateur` (`DOCKER`, `KUBERNETES`, `AUCUN` : liste vide, exploitation non configurée) |
| `GET /api/plateforme/infrastructure/charges/{nom}/journaux?lignes=200&depuis=PT10M` | `PLATFORM_OPS_READ` | 1 à 500 lignes, `PT1S` à `PT30M`, masquées ; `tronque` si des lignes plus anciennes existent ou si une ligne dépasse 4 000 caractères |
| `GET /api/plateforme/infrastructure/operations?page=0&taille=20` | `PLATFORM_OPS_READ` | Historique, des plus récentes aux plus anciennes (`contenu`, `total`, `page`, `taille`) |
| `POST /api/plateforme/infrastructure/charges/{nom}/redemarrage` | `PLATFORM_OPS_EXECUTE` + FIDO2 | 202, opération `EN_COURS` |
| `POST /api/plateforme/infrastructure/charges/{nom}/arret` | `PLATFORM_OPS_EXECUTE` + FIDO2 | 202 ; charge protégée : 409 `OPS_CHARGE_PROTEGEE` |
| `POST /api/plateforme/infrastructure/charges/{nom}/demarrage` | `PLATFORM_OPS_EXECUTE` + FIDO2 | 202 |
| `PUT /api/plateforme/infrastructure/charges/{nom}/repliques` | `PLATFORM_OPS_EXECUTE` + FIDO2 | `{ motif, repliques }` (0 à 5) ; Kubernetes seulement (Docker : 422 `OPS_NON_PRIS_EN_CHARGE`) ; zéro réplique d'une charge protégée : 409 |

Session à deux facteurs exigée comme sur tout `/api/plateforme/**`. Contrôles d'un geste, dans l'ordre : orchestrateur configuré (422 `OPS_NON_CONFIGURE`), prise en charge, motif de 5 à 500 caractères (400 `OPS_MOTIF_INVALIDE`), répliques (400 `OPS_REPLIQUES_INVALIDES`), présence de l'assertion (401 `REAUTH_REQUIRED`), charge connue (404 `OPS_CHARGE_INCONNUE`), protection (409), une opération à la fois par charge (409 `OPS_OPERATION_EN_COURS`), geste permis dans l'état actuel (409 `OPS_GESTE_NON_PERMIS`), puis vérification de l'assertion par MS-IAM (refus : 401 `REAUTH_REQUIRED` ; MS-IAM injoignable : 503 `OPS_REAUTH_INDISPONIBLE`). Réponses d'erreur `application/problem+json` portant `code`.

FIDO2 : même mécanisme que les décisions sensibles des services (IAM-07, D-62). La console demande un défi (`POST /bff/console/iam/reauthentification/defi`, `purpose` = `ADMIN_SENSITIVE`) puis envoie l'assertion dans `X-Reauth-Assertion` ; la passerelle la fait vérifier et consommer par MS-IAM (`POST /api/v1/interne/reauthentifications/verification`, jeton de service `gateway-service`, `INTERNAL_IAM_READ`) pour le sujet de la session (`sub`) et la finalité `gimac.orchestrateur.finalite-reauthentification`.

Traçabilité : table `operation_infrastructure` (Liquibase `20261005100000`), événement `platform.infrastructure-operation.v1` à la demande puis à l'issue (mécanisme d'audit de la passerelle, sujet `gateway.security.events`, si `gimac.passerelle.journal-decisions.actif`), journal applicatif `gimac.audit.exploitation`, événement SSE `operation-infrastructure` sur `/api/plateforme/flux` pour qui porte `PLATFORM_OPS_READ`.

Propriétés (`gimac.orchestrateur`) : `type` (`docker`, `kubernetes`, `aucun` par défaut ; un type inconnu arrête le démarrage), `delai-lecture` (10 s), `delai-geste` (90 s), `charges-protegees` (défaut : `gateway`, `frontend-console`, `docker-socket-proxy`, `*-postgresql`, `keycloak*`, `kafka`, `*-kafka`, `stockage-objet*`, `minio*`, `*-redis`, `*-valkey`, `valkey*`, `redis*`), `finalite-reauthentification` (`ADMIN_SENSITIVE`) ; `docker.url` (mandataire, port 2375), `docker.projet` (`gimac-des`), `docker.delai-arret` (10 s) ; `kubernetes.url` (`kubernetes.default.svc` en HTTPS), `kubernetes.espace` (`gimac-des`), `kubernetes.jeton` et `kubernetes.autorite` (fichiers du compte de service monté).

Déploiement : serveur de démonstration, `infra/docker-compose/docker-compose.serveur.yml` (service `docker-socket-proxy`, réseau interne `exploitation`, liste fermée de l'API dans `infra/docker-compose/exploitation/docker-socket-proxy.cfg`, `GIMAC_ORCHESTRATEUR_TYPE=docker`) ; Kubernetes, `infra/kubernetes/gateway-k8s/gateway-rbac.yml` (compte de service `gateway`, `Role` `gateway-exploitation`) et sortie vers l'API du cluster dans `infra/kubernetes-gimac/politiques-reseau.yaml`.

Tests : `ReglesExploitationTest`, `FluxMultiplexeDockerTest`, `DockerOrchestrateurTest`, `KubernetesOrchestrateurTest`, `AdaptateursExploitationTest`, `ExploitationInfrastructureServiceTest`, `ExploitationConfigurationTest` (unitaires, serveurs HTTP locaux simulant le moteur Docker, l'API Kubernetes et MS-IAM) ; `InfrastructureResourceSecurityTest` (sécurité, vraie chaîne de sécurité et vrai service : RSSI en lecture seule, sans FIDO2, protections, flux) ; `JournalOperationsR2dbcIT` (PostgreSQL par Testcontainers). JaCoCo `seuil-exploitation-infrastructure` (80 %).

## Connexion sur la plateforme et sessions (D-65)

Contrat : `docs/lot1/connexion.md` (§2 passerelle, §6 sessions). L'utilisateur ne voit jamais Keycloak : la passerelle (BFF) soumet identifiant, mot de passe et code TOTP à Keycloak par l'octroi direct, de serveur à serveur, et ne renvoie au navigateur que le témoin de session. La délégation par redirection est retirée : ni `oauth2Login` ni `oauth2Client`, `/oauth2/authorization/**` et `/login/oauth2/code/**` répondent 404 ; sans session, toute route protégée répond 401, sans redirection.

| Route | Réponses |
|---|---|
| `POST /api/connexion` `{identifiant, motDePasse, code?, couche}` | 200 `{etat: CODE_REQUIS}` ; 200 `{etat: PREMIER_ACCES, etapes}` ; 200 `{etat: CONNECTE, session}` ; 400 `CNX_REQUETE_INVALIDE` ; 401 `CNX_IDENTIFIANTS_INVALIDES` ; 403 `CNX_COUCHE_REFUSEE`, `CNX_COMPTE_INACTIF` ; 429 `CNX_TROP_DE_TENTATIVES` (`Retry-After: 60`) ; 503 `CNX_SERVICE_INDISPONIBLE` |
| `POST /api/connexion/mot-de-passe-oublie` `{identifiant, couche?}` | 202 toujours (transmis à MS-IAM, sauf au-delà de la limite ou en panne, sans le dire) |
| `POST /api/connexion/nouveau-mot-de-passe` `{jeton, motDePasse}` | 204 ; 400 de MS-IAM relayé tel quel (`CNX_JETON_INVALIDE`, `IAM_PASSWORD_POLICY` et `violations`) ; 400 `CNX_REQUETE_INVALIDE` ; 503 |
| `GET /api/session` | `SessionVue` plus `expireA` (fin d'inactivité prévue, ISO-8601, bornée par la durée maximale) et `restreinte` |
| `POST /api/session/prolongation` | 204 (activité explicite, « Rester connecté ») |
| `POST /api/logout` | 200 `{}` : jeton de rafraîchissement révoqué chez Keycloak, session supprimée ; plus d'adresse `logoutUrl` du fournisseur d'identité |

Erreurs en `application/problem+json` (champ `code`). Routes `/api/connexion/**` publiques, protégées contre la CSRF (témoin `XSRF-TOKEN`).

Déroulé de `POST /api/connexion` (`ServiceConnexion`) : limitation (5 tentatives par minute par adresse IP puis par identifiant, clé = empreinte SHA-256 de l'identifiant) ; octroi direct sur `gimac-connexion` (enregistrement `connexion`) ; validation du jeton d'identité par le JWKS du domaine (signature, émetteur, audience, échéance, sujets concordants) ; couche (`institution_id` n'ouvre que le portail) ; état du compte sur MS-IAM (`GET /api/v1/interne/comptes/{sub}/etat-connexion`, jeton de service `gateway-service` en `client_credentials`, gardé jusqu'à expiration) ; actions requises ou second facteur absent : session restreinte (`acr` 1) et `PREMIER_ACCES` ; sans code : `CODE_REQUIS` sans session ; avec code : octroi direct sur `web_app` (enregistrement `oidc`, `acr` 2). Tout jeton de `gimac-connexion` qui ne porte pas une session restreinte est révoqué aussitôt.

Ouverture de la session (`OuvertureSession`) : ancienne session vidée (jetons révoqués), nouvel identifiant de session, `OAuth2AuthenticationToken` dont l'`OidcUser` est construit sur le jeton d'identité validé (rôles et permissions des jetons d'identité et d'accès), `OAuth2AuthorizedClient` (accès et rafraîchissement) enregistré dans le `ServerOAuth2AuthorizedClientRepository`, contexte de sécurité dans la session. `TokenRelay`, le rafraîchissement (`OAuth2ReactiveRefreshTokensWebFilter`, par le client de la session : `web_app` ou `gimac-connexion`), `/api/session` et `/api/logout` fonctionnent donc comme avec une connexion OIDC. Le principal porte un identifiant propre à la session (claim `gimac_session`) : deux sessions d'un même compte ne partagent jamais leurs jetons. Un rafraîchissement refusé ferme la session (401 `CNX_SESSION_EXPIREE`). Le gestionnaire des clients autorisés est fourni par `ConnexionConfiguration` : celui que Spring Security 7 installe par défaut rechargeait l'utilisateur OIDC (appel userinfo) à chaque rafraîchissement et remplaçait le principal ; ici le principal reste celui de la connexion, un changement de droits s'applique à la connexion suivante.

Session restreinte (premier accès) : `GardeSessionWebFilter` ne sert que `GET|POST /bff/{couche}/iam/moi`, `…/premiere-configuration`, `…/mot-de-passe`, `…/second-facteur/**`, `GET /api/session`, `POST /api/session/prolongation`, `POST /api/logout` et `/api/connexion/**` ; ailleurs 403 `CNX_SESSION_RESTREINTE`. Le filtre `OpaAuthorize` transmet `restreinte` à OPA ; la règle `premier_acces` de `lot1_authz.rego` n'admet `acr` 1 que sur ces chemins, et une session restreinte n'ouvre aucune autre règle.

Sessions (§6) : `GardeSessionWebFilter` (ordre -150, avant Spring Security) ferme la session après `gimac.session.inactivite-max` (PT30M) sans activité ou `gimac.session.duree-max` (PT10H) : jeton de rafraîchissement révoqué, client autorisé oublié, session invalidée, 401 `CNX_SESSION_EXPIREE` (aussi pour un témoin qui désigne encore une session fermée pour échéance). Les requêtes `X-Gimac-Fond: 1` et le canal poussé `/bff/{couche}/live` ne prolongent pas la session. `BalayeurSessions` ferme chaque `gimac.session.balayage` (PT1M) les sessions échues sans attendre de requête. Témoin `SESSION` HttpOnly, `Secure` (`GIMAC_SESSION_TEMOIN_SECURISE`, vrai par défaut), `SameSite=Lax` ; magasin en mémoire gardé 10 h (`server.reactive.session.timeout`).

Journal d'audit : `gateway.login-succeeded.v1` (états `PREMIER_ACCES`, `CONNECTE`) et `gateway.login-failed.v1` (code et cause technique), identifiant masqué (`je***t`), adresse IP, couche, jamais le mot de passe ni le code ; journal applicatif `gimac.audit.connexion` et, si `gimac.passerelle.journal-decisions.actif`, sujet Kafka `gateway.security.events` (`GatewayAuditEventPublisher.publishSecurityEvent`, publié hors du fil Netty, Jackson 3). Limitation : `TokenBucketRateLimiter` sur Valkey si `gimac.passerelle.limitation-debit.actif`, avec repli en mémoire si Valkey ne répond pas ; sinon en mémoire de l'instance.

| Couche | Paquet | Contenu |
|---|---|---|
| Domaine | `domain/connexion` | `EtapesPremierAcces`, `RegleCouche`, `MasqueIdentifiant`, `CauseEchec` (statuts et codes), `ResultatConnexion` ; ports `FournisseurIdentite`, `AnnuaireComptes`, `LimiteurTentatives`, `JournalConnexion` |
| Adaptateurs | `infrastructure/connexion` | `KeycloakFournisseurIdentite`, `IamAnnuaireComptes`, `JetonServicePasserelle`, `LimiteurTentativesMemoire`, `LimiteurTentativesValkey`, `JournalConnexionAudit` |
| Application | `service/connexion/ServiceConnexion` | Déroulé du contrat |
| Sécurité | `security/session` | `OuvertureSession`, `FermetureSession`, `GardeSessionWebFilter`, `BalayeurSessions`, `RegistreSessions`, `SessionGimac`, `ReponseProbleme` |
| REST | `web/rest` | `ConnexionResource`, `SessionResource`, `LogoutResource` |
| Assemblage | `config` | `ConnexionConfiguration`, `ConnexionProperties` (`gimac.connexion` : `iam-url`, `delai` 5 s, `tentatives-par-minute` 5), `SessionProperties` (`gimac.session`) |

Configuration : enregistrements OAuth2 `oidc` (client `web_app`, secret `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_SECRET`), `connexion` (`GIMAC_CONNEXION_CLIENT_ID`, `GIMAC_SECRET_CONNEXION`), `gateway-service` (`GIMAC_SERVICE_CLIENT_ID`, `GIMAC_SERVICE_CLIENT_SECRET`) ; aucun secret dans le dépôt (`infra/docker-compose` les tire du `.env` : `GIMAC_SECRET_WEB_APP`, `GIMAC_SECRET_CONNEXION`, `GIMAC_SECRET_GATEWAY_SERVICE`). En production, `server.forward-headers-strategy: native` : l'adresse du client est lue dans `X-Forwarded-For` posé par le mandataire des couches.

Tests : `ConnexionDomaineTest`, `SessionGimacTest`, `AdaptateursConnexionTest`, `ServiceConnexionTest`, `GatewayAuditEventPublisherTest`, `SessionResourceTest`, `OpaAuthorizeGatewayFilterFactoryTest`, `OAuth2ReactiveRefreshTokensWebFilterTest` (unitaires) ; `ConnexionResourceSecuriteTest` (sécurité, vraie chaîne, Keycloak et MS-IAM simulés : états, codes 400/401/403/429/503, CSRF, fixation de session, session restreinte, inactivité, appels de fond, durée absolue, balayage, déconnexion, redirection retirée, audit) ; `ConnexionIT` (intégration, contexte complet : enregistrements construits depuis la découverte OIDC du Keycloak simulé `KeycloakSimule`, route `/bff/{couche}/iam/moi` avec `OpaAuthorize` et `TokenRelay`, rafraîchissement par le client de la session, témoin `Secure`). JaCoCo (`seuil-connexion-sessions`, phase `test`) bloque la construction sous 80 % en lignes et méthodes par classe et en branches sur le module. Politique OPA : `opa test infra/docker-compose/opa/politiques`.

## « Mon compte » : appareils connectés, historique, photo, mot de passe (docs/lot1/mon-compte.md)

La passerelle, porteuse des sessions, sert les appareils connectés et l'historique des connexions, relaie la photo de profil et applique l'option « fermer mes autres sessions » du changement de mot de passe.

| Route | Réponses |
|---|---|
| `GET /api/session/appareils` | Sessions ouvertes du compte, courante d'abord : `{id, courante, appareil{navigateur, systeme, type}, adresseIp, couche, ouverteLe, derniereActivite, expireA}` ; `id` opaque propre à la session (jamais l'identifiant du témoin) ; adresse masquée au dernier octet hors session courante |
| `DELETE /api/session/appareils/{id}` | 204 (jeton de rafraîchissement révoqué, session supprimée) ; 409 `CNX_SESSION_COURANTE` ; 404 `CNX_APPAREIL_INCONNU` (inconnue ou d'un autre compte) |
| `DELETE /api/session/appareils` | 204 : toutes les autres sessions du compte fermées |
| `GET /api/session/historique?page=0&taille=20` | `{contenu[{le, resultat, appareil, adresseIp (masquée), couche}], total}`, plus récentes d'abord, taille 1 à 100 ; 503 `CNX_HISTORIQUE_INDISPONIBLE` |

Session à deux facteurs exigée (`DoubleFacteurAuthorizationManager`) : 401 sans session, 403 sans second facteur, 403 `CNX_SESSION_RESTREINTE` au premier accès ; fermetures protégées contre la CSRF.

- **Appareil** : en-tête `User-Agent` analysé localement (`domain/appareils/AnalyseurUserAgent` : Edge, Opera, Samsung Internet, Firefox, Chrome, Safari, Internet Explorer ; Windows, Android, iOS, iPadOS, ChromeOS, macOS, Linux ; ORDINATEUR, MOBILE, TABLETTE, INCONNU), décrit dans la session à la connexion (`SessionGimac.decrire`).
- **Historique** : table `historique_connexion` de la base PostgreSQL de la passerelle (Liquibase `20261001100000_historique_connexion.xml`, lue et écrite en R2DBC par `HistoriqueConnexionsR2dbc` ; en mémoire seulement sans base, tests web). Chaque tentative : `REUSSIE` (connexion complète ou premier accès), `ECHEC_MOT_DE_PASSE`, `ECHEC_CODE`, `BLOQUEE` (limitation, compte inactif, couche refusée) ; l'étape `CODE_REQUIS`, une requête mal formée et une panne ne sont pas inscrites. Identifiant jamais en clair (empreinte SHA-256 tronquée, qui retrouve les échecs du mot de passe du compte), jamais de mot de passe ni de code. Rétention : `gimac.appareils.retention` (P90D), purge `gimac.appareils.purge` (PT1H, `PurgeHistorique`). Base en panne : la connexion n'attend pas plus de 2 s et n'échoue pas.
- **Nouvel appareil** : familles du navigateur et du système (sans versions) jamais vues dans une connexion réussie du compte, sauf à sa toute première connexion : `POST /api/v1/interne/comptes/{id}/alerte-connexion` sur MS-IAM (jeton de service `gateway-service`), hors du fil de la réponse.
- **Mot de passe** : filtre `FermetureAutresSessions` de la route « Mon compte » : sur `POST /bff/{couche}/iam/moi/mot-de-passe` réussi portant l'en-tête `X-Gimac-Fermer-Autres-Sessions: 1` de MS-IAM, les autres sessions du compte sont fermées avant la réponse ; l'en-tête n'est jamais transmis au navigateur.
- **Photo** : `/bff/{des,portail,console}/iam/moi/avatar` par la route « Mon compte » (corps annoncé borné à 3 Mo, `RequestSize`) ; `/bff/{des,console}/iam/avatars/**` → `/api/v1/avatars/**` (route `iam-avatars`) ; OPA : `GET` seulement, authentifié, acr 2 ou 3, jamais en session restreinte ; le portail n'a aucune route vers les photos des autres.

| Couche | Paquet | Contenu |
|---|---|---|
| Domaine | `domain/appareils` | `Appareil`, `TypeAppareil`, `AnalyseurUserAgent`, `MasqueAdresseIp`, `ResultatTentative`, `TentativeConnexion` ; ports `HistoriqueConnexions`, `AlerteNouvelAppareil` |
| Adaptateurs | `infrastructure/appareils`, `infrastructure/connexion/IamAnnuaireComptes` | `HistoriqueConnexionsR2dbc`, `HistoriqueConnexionsMemoire` ; alerte vers MS-IAM |
| Application | `service/appareils` | `SuiviConnexions` (historique, description de la session, alerte), `ServiceAppareils`, `PurgeHistorique` |
| REST et filtre | `web/rest/AppareilsResource`, `web/filter/FermetureAutresSessionsGatewayFilterFactory` | Routes ci-dessus |

Tests : `AppareilsDomaineTest`, `SuiviEtServiceAppareilsTest`, `FermetureAutresSessionsGatewayFilterFactoryTest`, `AdaptateursConnexionTest.alerteNouvelAppareil` (unitaires) ; `AppareilsResourceSecuriteTest` (sécurité, vraie chaîne et vraie connexion : 401, 403, session restreinte, CSRF, appareils, 409, 404, fermetures, historique, alerte) ; `HistoriqueConnexionsR2dbcIT` (PostgreSQL Testcontainers) et `ConnexionIT.monCompte` (contexte complet : historique en base, fermeture des autres sessions au changement de mot de passe, photo relayée) ; `RoutesPresentationTest` ; politique OPA (`lot1_authz_test.rego`). JaCoCo `seuil-appareils-connectes` (phase `test`).

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
| 2026-09-30 | — | Messagerie (D-64) : route `/bff/des/collaboration/messagerie/**` vers MS-COLLABORATION (`/api/v1/messagerie/**`), OPA d'abord, corps multipart bornés à 101 Mo ; règles OPA `COL_MESSAGE_READ` et `COL_MESSAGE_SEND`, `/bff/portail/collaboration/**` refusé. Canal poussé `GET /bff/{couche}/live` (SSE, contrat de messagerie §5) : consommateur Kafka par instance, remise filtrée par couche, institution et destinataires, reprise sur 500 événements (5 min), maintien toutes les 25 s ; seuil JaCoCo `seuil-canal-pousse`. `./mvnw verify` : 192 tests unitaires et 76 d'intégration réussis ; couverture du canal 100 % en lignes, 98,7 % en méthodes, 96,3 % en branches. |
| 2026-10-01 | — | Connexion sur la plateforme (D-65) : `POST /api/connexion` (octroi direct sur `gimac-connexion` puis `web_app`, état du compte sur MS-IAM par le jeton de service `gateway-service`, `CODE_REQUIS`, `PREMIER_ACCES`, `CONNECTE`), mot de passe oublié et nouveau mot de passe, limitation 5/min par adresse IP et par identifiant, audit `gateway.login-succeeded.v1` et `gateway.login-failed.v1` ; redirection vers Keycloak retirée (404) ; session restreinte (`acr` 1, OPA `premier_acces`) ; fermeture après 30 min d'inactivité ou 10 h (révocation chez Keycloak, 401 `CNX_SESSION_EXPIREE`, balayage), `expireA`, `restreinte`, `POST /api/session/prolongation` ; publication d'audit passée à Jackson 3 ; échecs intermittents corrigés (BlockHound et verrou du cache de désérialiseurs de Jackson 3, délais de `SupervisionPlateformeHttpTest`, délai de `LiveConfigurationTest.actif`). `./mvnw -ntp -B verify` : 258 tests unitaires et 79 d'intégration réussis ; couverture du module 99,0 % en lignes, 97,9 % en méthodes, 90,8 % en branches. |
| 2026-10-01 | — | « Mon compte » (docs/lot1/mon-compte.md §1 à §3) : appareils connectés (`GET`/`DELETE /api/session/appareils`, 409 `CNX_SESSION_COURANTE`), historique persistant des connexions (`GET /api/session/historique`, table `historique_connexion` en R2DBC, 90 jours), User-Agent analysé localement, alerte « nouvel appareil » vers MS-IAM, filtre `FermetureAutresSessions`, routes des photos (`/bff/{couche}/iam/moi/avatar` bornée à 3 Mo, `/bff/{des,console}/iam/avatars/**`), règles OPA et tests ; seuil JaCoCo `seuil-appareils-connectes`. `./mvnw -ntp -B verify` : 299 tests unitaires et 83 d'intégration réussis ; couverture fusionnée hors MapStruct 81,1 % en lignes, 85,5 % en méthodes ; code nouveau 98,9 % en lignes, 90,2 % en branches ; `opa test` 80/80. |
| 2026-10-01 | — | Messagerie (D-64) ouverte à tout membre interne, depuis le DES et la console : route `/bff/console/collaboration/messagerie/**`, sujet `collaboration` du canal poussé remis à la couche console ; OPA refuse la messagerie à tout jeton portant une institution (87 tests OPA). |
| 2026-10-02 | - | Dépôts fragmentés (D-74) : route `depot-fragmente`, `/bff/{des,console,portail}/document/depots/**` réécrit en `/api/v1/depots/**` (MS-DOCUMENT), `OpaAuthorize` puis `RequestSize` 17 Mio ; `RoutesPresentationTest` (routage depuis les trois couches, taille bornée) ; 325 tests réussis. |
| 2026-10-05 | - | Exploitation de l'infrastructure depuis la console (D-78) : `/api/plateforme/infrastructure/{charges,charges/{nom}/journaux,operations}` (`PLATFORM_OPS_READ`), gestes `redemarrage`, `arret`, `demarrage`, `repliques` (`PLATFORM_OPS_EXECUTE`, motif, FIDO2 vérifié par MS-IAM), orchestrateurs Docker (mandataire `docker-socket-proxy`) et Kubernetes (compte de service `gateway`), charges protégées, journaux masqués, table `operation_infrastructure`, audit `platform.infrastructure-operation.v1`, opérations poussées sur `/api/plateforme/flux`. |
| 2026-10-05 | - | Notifications de la plateforme (D-80) : sujet Kafka `notification.inapp` relayé sur le canal poussé (sujet logique `notification`, DES et console), remis au seul compte de `data.destinataires`, `acteur` lu dans `data.acteurId` ; `sujets=notification` accepté. `./mvnw verify` : 428 tests unitaires et 85 d'intégration réussis (`ConnexionIT`, en dépassement de délai sous la charge de la machine, réussi relancé seul) ; seuils JaCoCo, modernizer et checkstyle respectés. |
| 2026-10-06 | — | Séries temporelles des graphiques de la supervision : `GET /api/plateforme/series` (métrique et période dans des listes fermées, jamais de PromQL libre ; cible validée ; mesures manquantes explicites ; séries triées ; Prometheus injoignable signalé sans erreur), permission PLATFORM_SUPERVISION_READ et second facteur ; tests de domaine, de service, d'adaptateur et de sécurité |
| 2026-10-06 | — | Séries `CHARGE_CPU` et `CHARGE_MEMOIRE` : processeur et mémoire par charge de travail Docker (service Compose), pour les courbes de l'écran Infrastructure |
| 2026-10-06 | — | Exploitation Kubernetes, lecture : pods et événements d'une charge, nœuds du cluster, jobs et cronjobs (`/api/plateforme/infrastructure/charges/{nom}/pods`, `…/evenements`, `/noeuds`, `/taches`), PLATFORM_OPS_READ ; Docker : 422 « Kubernetes seulement » ; tests de l'adaptateur face à un faux cluster, du service et de sécurité |
| 2026-10-06 | — | Geste RETABLIR (Kubernetes) : `POST /api/plateforme/infrastructure/charges/{nom}/retablissement`, PLATFORM_OPS_EXECUTE + FIDO2 ; cible = révision précédente d'image différente (révisions de simple redémarrage sautées), correctif JSON qui remplace le gabarit ; `imagePrecedente` sur la charge ; changeset Liquibase qui élargit la contrainte du journal ; tests |
| 2026-10-06 | - | Contournement d'OPA fermé : routes JHipster `/services/{service}/**` et `/services/gateway/**` retirées (aucun client), `/services/**` en `denyAll`, CORS `/services/*/api/**` retiré ; tests `CheminsServicesSecuriteTest` (401 sans session, 403 avec jeton ou session portant `ROLE_ADMIN` et les permissions d'administration) et `RoutesPresentationTest` (aucune route hors `/bff`, `OpaAuthorize` en premier partout) |
| 2026-10-13 | D-107 | D-107 : routes `console-reprise-participant` et `console-reprise-homologation` (`/bff/console/{participant,homologation}/reprise/**` vers `/api/v1/reprise/**`, 10 Mo, OPA `REF_REPRISE_IMPORT`) |
| 2026-10-19 | D-114 | Route `des-monitoring` (`/bff/des/monitoring/**` vers MS-MONITORING, OPA d'abord, corps de 64 Ko, `X-Reauth-Assertion` relayé) et son test |
| 2026-10-20 | D-115 | Route `portail-sbx-habilitations` (`/bff/portail/sbx-habilitations/**` vers l'administration déléguée de MS-IAM) |
| 2026-10-22 | D-120 | Routes de la sandbox (`console-sbx-*`, `portail-sbx-*`, `des-sbx-espace`, adresses `gimac.services.sbx-*` 8101 à 8111), filtre `CloisonnementSandbox` (mandats, jeton réduit, quotas, `gateway.sandbox-decision.v1`), table `sbx_perimetre`, règle JaCoCo à 98 % |
| 2026-10-22 | D-119 | Route `console-iam-sandbox` (`/bff/console/iam/sandbox/**` vers `/api/v1/sandbox/**` de MS-IAM) |
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
