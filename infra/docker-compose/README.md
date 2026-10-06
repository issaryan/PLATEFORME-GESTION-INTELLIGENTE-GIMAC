# Environnement local (Docker Compose)

Environnement de développement de la plateforme en une commande (SOC-13, D-45, D-48). Voir aussi `README-DOCKER-COMPOSE.md`, produit par JHipster.

## Fichiers

| Fichier | Rôle |
|---|---|
| `.yo-rc.json` | Réponses du générateur `jhipster docker-compose` |
| `docker-compose.yml` | **Généré** : 14 services, PostgreSQL et cache par service, Kafka, Keycloak, Prometheus, Alertmanager, Grafana. Ne pas éditer : régénérer |
| `docker-compose.gimac.yml` | Complément du projet, fusionné au lancement |
| `gimac-dev.sh` | Commande unique : prérequis, secrets, images, démarrage, arrêt ; journaux conservés dans `.journaux-echec/` en cas d'échec |
| `scripts/token.sh` | Jeton d'un utilisateur de test du domaine gimac (à sourcer) |
| `scripts/verifier-phase0.sh` | Contrôles de sortie de la Phase 0 du lot 1 |
| `tester-socle.sh` | Vérification de bout en bout du socle démarré (48 contrôles) ; code de sortie = nombre d'échecs |
| `stockage-objet/` | Ceph RGW mono-nœud et création des compartiments WORM (D-50) |
| `opa/politiques/` | Politique d'accès de développement et ses tests |
| `simulacres/` | Simulacres WireMock du simulateur, de la gestion des changements et de l'autorité d'horodatage |
| `observabilite/` | Collecteur OpenTelemetry, Tempo, sources de données Grafana |
| `.env` | Secrets et ports locaux, créés par `gimac-dev.sh`, **jamais versionnés** |

## Complément du projet

- Valkey à la place de Redis (D-25) et mots de passe d'administration issus du `.env` (D-44).
- Ceph RGW (port 7480) avec compartiments `gimac-preuves`, `gimac-documents`, `gimac-archives`, `gimac-rapports`, verrouillage d'objets activé ; compartiment `gimac-depots` des dépôts fragmentés en cours (D-74), sans verrouillage, objets expirés après 2 jours et téléversements incomplets abandonnés après 1 jour.
- Registre des schémas Apicurio (8880), OpenBao (8200), OPA (8181).
- Simulacres : simulateur (8190, lecture seule, écritures refusées), gestion des changements (8191), horodatage (8192), messagerie Mailpit (1025, 8025).
- OpenTelemetry (4317, 4318), Loki (3100), Tempo (3200).
- État des serveurs (D-61) : node-exporter (hôte, sans port publié) et, avec `COMPOSE_PROFILES=conteneurs`, cAdvisor (conteneurs) ; jobs `node-exporter` et `cadvisor` (découverte DNS, aucune cible sans le profil) ajoutés à `prometheus-conf/prometheus.yml`, à reporter après une régénération. La passerelle les restitue sur `/api/plateforme/serveurs`.
- Secrets des clients de service du domaine `gimac`, créés dans `.env` par `gimac-dev.sh` : `GIMAC_SECRET_HOMOLOGATION_SERVICE`, `GIMAC_SECRET_CERTIFICATION_SERVICE`, `GIMAC_SECRET_PARTICIPANT_SERVICE`, `GIMAC_SECRET_IAM_SERVICE` (MS-IAM administre le domaine, D-62), `GIMAC_SECRET_GATEWAY_SUPERVISION` (supervision de la passerelle), `GIMAC_SECRET_CONNEXION` (client `gimac-connexion`), `GIMAC_SECRET_WEB_APP` (client `web_app`) et `GIMAC_SECRET_GATEWAY_SERVICE` (client `gateway-service`) de la connexion sur la plateforme (D-65), transmis à Keycloak et à la passerelle ; `GIMAC_SECRET_PILOTAGE_SERVICE` (client `pilotage-service` de MS-PILOTAGE, Mon activité, D-76), transmis à Keycloak et à MS-PILOTAGE. `GIMAC_SECRET_NOTIFICATION_SERVICE` (client `notification-service` de MS-NOTIFICATION, notifications de la plateforme, D-80), transmis à Keycloak et à MS-NOTIFICATION. Un `.env` existant est complété des variables manquantes.
- Keycloak monte le thème GIMAC (`infra/keycloak/themes/gimac`) : pages de connexion et courriels d'invitation, reçus dans Mailpit (http://localhost:8025).
- Couches de présentation : département (4200), portail (4201), console (4202).
- Kafka mono-nœud : réplication des sujets internes à 1 (`KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` et sujets de transactions et de partage), absente du Compose généré. Sans elle, `__consumer_offsets` n'est jamais créé (facteur 3 par défaut sur un seul courtier) et aucun consommateur abonné ne reçoit de message (canal poussé de la passerelle muet). Garde-fou : `CourtierPlateformeIT` (ms-gateway) démarre un courtier avec les variables de ces deux fichiers.
- `kafka-sujets-init` (image `apache/kafka`, outils absents de l'image native) : exécute `infra/kafka/creer-sujets-lot1.sh` avec `REPLICATION=1` (6 partitions, rétention 30 jours, files `.dlq`) dès que le courtier répond ; les 14 services en dépendent (`service_completed_successfully`), si bien que les sujets ne sont plus auto-créés à 1 partition par le premier producteur ou consommateur. Idempotent : un sujet existant n'est pas modifié.

## Commandes

```
./gimac-dev.sh verifier                 # prérequis et ports libres
./gimac-dev.sh demarrer --socle         # sans les 14 services
./gimac-dev.sh images                   # construit les images Jib des 14 services
./gimac-dev.sh demarrer                 # tout
./gimac-dev.sh etat | arreter | nettoyer
./tester-socle.sh                       # vérification de bout en bout
```

Un port déjà pris sur le poste est signalé avec la variable `GIMAC_PORT_*` à placer dans `.env`. En cas d'échec, tout ce qui a démarré est arrêté. Le projet Compose s'appelle `gimac-des`.

## Mettre à jour un poste

```
infra/docker-compose/mettre-a-jour.sh              # git pull, images périmées, domaine Keycloak, redémarrage
infra/docker-compose/mettre-a-jour.sh --tout       # reconstruit les 14 services et les trois couches
infra/docker-compose/mettre-a-jour.sh --sans-pull  # code local non poussé
```

Le script tire `main`, reconstruit les seules images dont le code a changé depuis leur construction (commit postérieur sur `ms-<service>/src/main`, `pom.xml` ou `frontend/`), applique une nouvelle version de `gimac-realm.json` en conservant les comptes (Keycloak persistant), démarre la plateforme, redémarre les services dont la configuration montée a changé (OPA, simulacres, observabilité), puis vérifie les trois couches. Les modifications non commitées ne sont pas détectées : `--tout`. Il remplace la chaîne CI/CD tant qu'elle est indisponible.

## Keycloak persistant sur un poste

Par défaut, Keycloak garde ses données dans son conteneur (dev-file) : comptes, mots de passe et codes TOTP enrôlés disparaissent quand il est recréé. Pour les conserver, comme sur le serveur de démonstration, ajouter au `.env` du poste :

```
GIMAC_COMPOSE_SUPPLEMENT=docker-compose.keycloak-persistant.yml
```

`gimac-dev.sh` lit alors cette surcouche (base PostgreSQL `keycloak-postgresql`, volume `keycloak_donnees`) ; `GIMAC_KEYCLOAK_DB_PASSWORD` est généré dans le `.env`. Le domaine `gimac` est importé au premier démarrage ; une version ultérieure de `gimac-realm.json` n'est plus appliquée d'elle-même (`--import-realm` ignore un domaine existant) : l'appliquer en conservant les comptes par export (`kc.sh export --users realm_file`), fusion (`infra/ci/fusionner-domaine.py`) puis import (`kc.sh import --override true`), comme `actualiser_domaine` de `infra/ci/deployer-serveur.sh`.

## Régénérer la base

```
cd infra/docker-compose && jhipster docker-compose --no-insight
git checkout -- ../../ms-*/.yo-rc.json   # annule la réécriture de version par le générateur
```

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 9f2250a | Génération JHipster, complément, `gimac-dev.sh` |
| 2026-09-28 | 932d192 | Ports réglables, contrôle des ports, projet `gimac-des`, santé de Keycloak |
| 2026-09-28 | 7dcca58 | Ceph RGW remplace MinIO ; clés S3 dans le `.env` |
| 2026-09-28 | b20a049 | Trois couches de présentation |
| 2026-09-28 | — | Ajout de ce README |
| 2026-09-29 | — | Services dépendants de Kafka, délai de santé élargi, Valkey de la passerelle, amorçage Ceph robuste sous charge, conteneurs d'initialisation hors attente de santé, journaux conservés sur échec, `tester-socle.sh` : 48/48 (D-54) |
| 2026-09-29 | — | Lot 1 : domaine Keycloak gimac importé, émetteur gimac pour tous les services, jeton de service des deux services du lot, caches Redis retirés (régénération), `scripts/token.sh` et `scripts/verifier-phase0.sh`, politique OPA du lot (D-55) |
| 2026-09-29 | — | `scripts/token.sh` : jeton de niveau 2 par autorisation par appareil (double facteur, D-61), niveau 1 sur demande ; politique OPA : second facteur et routes du registre des institutions (18 tests, couverture 100 %) |
| 2026-09-29 | — | Supervision de la plateforme (D-61) : node-exporter, cAdvisor (profil `conteneurs`), collecte Prometheus correspondante, jeton de service de la passerelle (`GIMAC_SECRET_GATEWAY_SUPERVISION`, facultatif) |
| 2026-09-29 | — | D-62 : MS-IAM en annuaire réel (client `iam-service`), MS-PARTICIPANT avec son client de service, secrets des clients passés à Keycloak, thème GIMAC monté ; secret de supervision désormais obligatoire ; `docker compose config` vérifié |
| 2026-09-29 | — | `gimac-dev.sh images` : `-Djacoco.skip=true`, sans quoi le seuil de couverture de `verify` (D-61), privé de mesures par `-DskipTests`, fait échouer la construction des images |
| 2026-09-29 | — | Régénération JHipster 9.4.0 après celle de MS-PARTICIPANT : son entrée prend `SPRING_DATASOURCE_USERNAME` (la configuration `prod` de 9.4 n'en porte plus ; sans elle le pilote se connectait sous l'utilisateur système `ubuntu` et le service restait malsain), la sonde de santé et les images de 9.4 ; complément Prometheus (node-exporter, cAdvisor) reporté |
| 2026-09-30 | — | Connexion sur la plateforme (D-65) : secrets `GIMAC_SECRET_CONNEXION`, `GIMAC_SECRET_WEB_APP`, `GIMAC_SECRET_GATEWAY_SERVICE` générés dans `.env` et passés à Keycloak et à la passerelle (clients `gimac-connexion`, `web_app`, `gateway-service`) ; journal d'audit de la connexion activé (Kafka) ; témoin de session `Secure` réglable (`GIMAC_SESSION_TEMOIN_SECURISE`) ; `tester-socle.sh` : 401 sans session et 404 sur `/oauth2/authorization/oidc` ; politique OPA : règle `premier_acces` (session restreinte, `acr` 1) |
| 2026-10-01 | — | Kafka : réplication des sujets internes à 1 (coordinateur de groupe indisponible auparavant : aucun consommateur ne recevait, temps réel de la messagerie muet) ; conteneur `kafka-sujets-init` créant les sujets du lot 1 au démarrage, prérequis des 14 services ; scénario de la messagerie : 27/27 |
| 2026-10-01 | — | Serveur de démonstration : surcouche `docker-compose.serveur.yml` (couches de présentation publiées sur le réseau, adresses publiques et témoin de session non `Secure` par `.env`), ajoutée par `GIMAC_COMPOSE_SUPPLEMENT` dans `gimac-dev.sh` ; déploiement par `infra/ci/deployer-serveur.sh` |
| 2026-10-01 | — | Serveur de démonstration : Keycloak persistant (PostgreSQL `keycloak-postgresql`, volume `keycloak_donnees`) ; une nouvelle version de `gimac-realm.json` est réimportée par `deployer-serveur.sh` en conservant les comptes (export `kc.sh`, `infra/ci/fusionner-domaine.py`) |
| 2026-10-01 | — | `gimac-dev.sh images` construit aussi les trois couches de présentation : le Compose gardait sinon des images antérieures à D-65 (redirection vers `/oauth2/authorization/oidc`, 404) |
| 2026-10-02 | — | `gimac-dev.sh images` : secrets du `.env` préparés avant la construction des couches de présentation (sur un poste neuf, le Compose refusait d'interpoler les secrets absents) |
| 2026-10-02 | — | `tester-socle.sh` : 51/51. Secret du client `internal` lu dans `.env` (`GIMAC_SECRET_INTERNAL`) ; CRUD de MS-IAM et MS-PARTICIPANT contrôlés selon D-62 (403 sans `ROLE_ADMIN`, 200 avec le compte de service `gateway-supervision`, portée `openid` exigée par l'appel `userinfo` de la passerelle) |
| 2026-10-02 | — | Surcouche `docker-compose.keycloak-persistant.yml` : Keycloak persistant sur un poste (comptes, mots de passe et TOTP conservés) ; `GIMAC_COMPOSE_SUPPLEMENT` lu dans le `.env` par `gimac-dev.sh`, `GIMAC_KEYCLOAK_DB_PASSWORD` généré |
| 2026-10-05 | — | `mettre-a-jour.sh` : mise à jour d'un poste en une commande (pull, images périmées seulement, domaine Keycloak réimporté en conservant les comptes, configurations montées, état final) |
| 2026-10-05 | - | Mon activité (D-76) : `pilotage` reçoit l'émetteur gimac, son client de service `pilotage-service` (`GIMAC_SECRET_PILOTAGE_SERVICE`, généré dans `.env` et passé à Keycloak) et les adresses de MS-IAM et MS-DOCUMENT ; sujets Kafka `pilotage.task` et `pilotage.report` |
| 2026-10-05 | - | Notifications de la plateforme (D-80) : `notification` reçoit l'émetteur gimac, son client de service `notification-service` (`GIMAC_SECRET_NOTIFICATION_SERVICE`, généré dans `.env` et passé à Keycloak) et les adresses de MS-IAM et MS-COLLABORATION ; sujet Kafka `notification.inapp`, files `.dlq` de `collaboration.message`, `pilotage.task` et `pilotage.report` |
| 2026-10-06 | — | `gimac-dev.sh preparer` : complète le `.env` des secrets manquants ; appelé par `mettre-a-jour.sh` avant toute construction (nouveaux clients de service des commits tirés) |
| 2026-10-06 | — | Prometheus : rétention explicite de 15 jours (complément), au-delà de la plus longue période des graphiques de la supervision (7 jours) |
