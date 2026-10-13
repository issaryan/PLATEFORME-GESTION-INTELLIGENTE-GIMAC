# Complément Kubernetes du projet (Kustomize)

S'appuie sur `../kubernetes`, généré par JHipster, sans le modifier (D-48, D-49).

## Contenu

| Fichier | Rôle |
|---|---|
| `kustomization.yaml` | Assemblage : base générée, surcharges, étiquettes de segment, corrections de schéma, ports des Services applicatifs alignés sur ceux des conteneurs (8080 à 8093), équivalence avec Docker Compose (courtier Kafka, secret `internal`, passerelle, Keycloak, ServiceMonitor, Prometheus), générateurs des ConfigMap partagées, version de busybox |
| `opa.yaml` | Politiques d'accès (IAM-19, RG-24) : OPA `1.21.0-static` comme Compose, 2 répliques, politiques de la ConfigMap `opa-politiques`, segment `politiques` joint par la passerelle seule, profil `restricted` |
| `alertmanager.yaml` | Alertmanager `v0.34.1` comme Compose, configuration `alertmanager-configuration`, segment `alertes` (Prometheus, passerelle, Grafana), aucune sortie |
| `regles-alertes.yaml` | `PrometheusRule` générée depuis `docker-compose/prometheus-conf/alert_rules.yml` par `synchroniser-partages.sh` |
| `kafka-sujets.yaml` | Job `kafka-sujets-init` : sujets du lot 1 (équivalent du conteneur Compose), réplication 1 |
| `keycloak-domaine.yaml`, `keycloak-domaine-job.yaml`, `actualiser-domaine-keycloak.sh` | Mise à jour du domaine `gimac` sans perte (comptes, profils créés, recompositions, politique de mot de passe, OTP, sessions, force brute) : volume `keycloak-domaine-etat` (base de la fusion), ConfigMap `keycloak-fusion-domaine` (copie de `infra/ci/fusionner-domaine.py`), Job appliqué hors de Kustomize par le script, Keycloak arrêté |
| `partages/` | Copies exactes des fichiers de Compose et de la CI lus par les générateurs (`opa/*.rego`, `alertmanager/config.yml`, `kafka/creer-sujets-lot1.sh`, `keycloak/fusionner-domaine.py`) : Kustomize ne lit rien hors de ce dossier |
| `synchroniser-partages.sh` | Recopie `partages/` et régénère `regles-alertes.yaml` ; `--verifier` échoue si une copie diffère de sa source |
| `retrait-operateur-prometheus.yaml` | Retrait de l'opérateur Prometheus généré (v0.42.1, RBAC `v1beta1` refusé depuis Kubernetes 1.22) |
| `politiques-reseau.yaml` | Refus par défaut et matrice des flux par segment : `dmz`, `socle`, `referentiels`, `exploitation`, `pilotage`, `persistance`, `identite`, `supervision`, `presentation`, `conversion`, `politiques`, `alertes` ; sorties vers les dispositifs extérieurs (S3, SMTP, gestion des changements) par port, à resserrer par `ipBlock` |
| `valkey.yaml` | Cache partagé (D-25, SOC-11) |
| `conversion-documents.yaml` | Moteur de conversion de l'aperçu bureautique (D-83) et de l'organigramme en PDF (D-102, Chromium sans JavaScript, adresses limitées à ses fichiers temporaires) : Gotenberg `8.37.0`, segment `conversion`, joint par MS-DOCUMENT seulement (politiques `conversion-documents`, `sortie-document-conversion`), profil `restricted`, 1,5 Gio |
| `frontend.yaml` | Trois couches de présentation, profil `restricted`, limitées à la passerelle |
| `kyverno-images-signees.yaml` | Vérification des signatures d'images (Audit, puis Enforce avec SOC-02) |
| `retrait-secrets-generes.yaml` | Retrait des Secrets générés |
| `creer-secrets.sh` | Création des secrets au déploiement (puis OpenBao, SOC-06), dont `keycloak-admin` et `gimac-clients-keycloak` (secrets des clients de service), et de la ConfigMap `gimac-dispositifs-externes` (`GIMAC_GESTION_CHANGEMENTS_URL`) |
| `domaine-gimac/*.yaml` | Domaine Keycloak `gimac` (D-61, D-62) : émetteur des jetons des 14 services, clients de service (`iam-service`, `participant-service`, `homologation-service`, `certification-service`, `gateway-supervision`), annuaire réel de MS-IAM par l'entrée, Keycloak à l'image GIMAC (thème et domaine intégrés, administrateur d'amorçage lu dans un Secret) |

## Déployer

```
GIMAC_S3_POINT_ACCES=... GIMAC_S3_CLE_ACCES=... GIMAC_S3_CLE_SECRETE=... \
GIMAC_GESTION_CHANGEMENTS_URL=https://... ./creer-secrets.sh
./synchroniser-partages.sh --verifier
kubectl apply -k .
./actualiser-domaine-keycloak.sh   # seulement si l'image Keycloak porte une nouvelle version du domaine
```

## Mettre à jour le domaine Keycloak sans perte

`start --import-realm` ignore un domaine existant. Quand l'image Keycloak porte une nouvelle version de
`gimac-realm.json`, `./actualiser-domaine-keycloak.sh` : arrête Keycloak (aucune écriture concurrente, caches vidés
au redémarrage) ; lance le Job `keycloak-domaine` (`keycloak-domaine-job.yaml`, même image que le StatefulSet, base
`keycloak-postgresql`) dont les conteneurs **exporter** (`kc.sh export --users realm_file` ; domaine de l'image
identique à la base : rien à faire), **fusionner** (`python3 fusionner-domaine.py`, ConfigMap `keycloak-fusion-domaine`)
et **importer** (`kc.sh import --override true`, secrets `${GIMAC_*}` du Secret `gimac-clients-keycloak`, puis domaine de
l'image gardé comme base dans le volume `keycloak-domaine-etat`) se suivent ; affiche les journaux, supprime le Job et
relance Keycloak au nombre de répliques d'avant. Les fichiers d'échange (empreintes de mots de passe) restent dans un
volume en mémoire du pod. Règles de la fusion : `docs/lot1/ecrans-par-configuration.md` §6.6. Le Job porte le segment
`identite` (sortie vers `keycloak-postgresql` seule) et l'étiquette `app: keycloak-domaine`, hors du Service `keycloak`.

Prérequis : contrôleur d'entrée NGINX dans l'espace de noms `ingress-nginx`, opérateur Kyverno, opérateur Prometheus à jour et ses CRD (`monitoring.coreos.com` : bundle officiel de prometheus-operator ou kube-prometheus-stack ; celui généré par JHipster est retiré), images dans `registre.gimac.local/des`, dont l'image Keycloak construite depuis `infra/keycloak/Dockerfile` (`registre.gimac.local/des/keycloak:26.7.4-gimac`).

## Fichiers partagés avec Docker Compose

Docker Compose est la source des politiques OPA, de la configuration d'Alertmanager, des règles d'alerte et du script des sujets Kafka. Kustomize refuse de lire un fichier hors de ce dossier (restriction de chargement que ni `kubectl apply -k` ni `infra/ci/valider-infra.sh` ne lèvent) : ils sont recopiés sans retouche dans `partages/` (et `regles-alertes.yaml`). Après toute modification d'une source : `./synchroniser-partages.sh`, puis versionner les copies ; `./synchroniser-partages.sh --verifier` avant chaque déploiement (et en CI) échoue sur une copie désynchronisée, orpheline ou absente du générateur `opa-politiques`. Une nouvelle politique `.rego` s'ajoute aussi à ce générateur dans `kustomization.yaml`.

## Équivalence avec Docker Compose

Comparaison avec `docker-compose.yml`, `docker-compose.gimac.yml` et `docker-compose.serveur.yml` (serveur de démonstration), état au 2026-10-06.

| Élément de Compose | Kubernetes | État |
|---|---|---|
| `opa` (OPA `1.21.0-static`, `./opa/politiques`) | `opa.yaml`, ConfigMap `opa-politiques` (copie `partages/opa`), `GIMAC_OPA_URL` de la passerelle, politique réseau `opa` | Corrigé : sans OPA, le filtre `OpaAuthorize` refusait tout (refus par défaut). `--ignore=.*` évite le double chargement des entrées cachées du volume (vérifié avec OPA 1.21.0) |
| `alertmanager` (`v0.34.1`, `alertmanager-conf/config.yml`) | `alertmanager.yaml`, ConfigMap `alertmanager-configuration`, `alerting` de la ressource Prometheus, `GIMAC_SUPERVISION_ALERTMANAGERURL` | Corrigé. Récepteur `webhook.site` de la configuration partagée : exemple, aucune sortie ouverte ; récepteur réel à fixer |
| `prometheus` (`v3.14.0`, job `prometheus` sur `/management/prometheus` toutes les 5 s, `alert_rules.yml`) | Ressource Prometheus `jhipster-prometheus` (`v3.14.0`, `ruleSelector`), 14 ServiceMonitor, `regles-alertes.yaml`, `GIMAC_SUPERVISION_PROMETHEUSURL` | Corrigé : ServiceMonitor générés sur le port `web` et le chemin `/prometheusMetrics` inexistants (désormais `http`, `/management/prometheus`, 5 s) ; RBAC `v1beta1` passé en `v1` ; sortie de Prometheus vers l'API (découverte) et vers Alertmanager ouverte. Données en `emptyDir` (volume sous Compose) |
| Opérateur Prometheus (absent de Compose) | Retiré (`retrait-operateur-prometheus.yaml`) | Prérequis de la grappe : la version générée (v0.42.1, 2020) n'installe pas les CRD et son ClusterRole `v1beta1` est refusé |
| `/management/prometheus` | `permitAll` dans la `SecurityConfiguration` de la passerelle et des 13 services | Accepté dans la grappe (Prometheus seul y est admis par les politiques réseau). Corrigé : l'entrée générée publiait la passerelle sans authentification (`gateway.gimac-des.des.gimac.local`, dont `/management/*`) et chaque service sous `/services/<service>/` sans OPA ; la règle est retirée, comme le serveur Compose qui ne publie que les couches de présentation |
| `keycloak` (`KC_HEALTH_ENABLED`, `KC_HTTP_MANAGEMENT_PORT=9990`, `start-dev`) | Port `management` 9990 (conteneur et Service), `GIMAC_SUPERVISION_KEYCLOAKSANTEURL`, politiques `identite` et `sortie-passerelle-plateforme` | Corrigé. Aussi : options v1 retirées de Keycloak 26 (`KC_PROXY`, `KC_HOSTNAME_URL`, `KC_HOSTNAME_ADMIN_URL`) remplacées par `KC_HOSTNAME`, `KC_HTTP_ENABLED` (sans quoi `start` refuse le HTTP) et `KC_PROXY_HEADERS` ; JGroups (7800, 57800) ouvert entre les deux répliques |
| Import du domaine (`--import-realm`, `gimac-realm.json` et secrets `${GIMAC_SECRET_*}`) | Domaine intégré à l'image GIMAC, realm JHipster par ConfigMap, 14 secrets du Secret `gimac-clients-keycloak` | Équivalent. Base `keycloak-postgresql` comme `docker-compose.serveur.yml` |
| Réimport par fusion à trois voies d'un domaine modifié (`actualiser_domaine` de `deployer-serveur.sh`, `mettre-a-jour.sh`) | `actualiser-domaine-keycloak.sh` et Job `keycloak-domaine` (`kc.sh export`, `fusionner-domaine.py`, `kc.sh import --override true`), base dans le volume `keycloak-domaine-etat` | Équivalent, lancé par l'exploitant après `kubectl apply -k .` (Keycloak arrêté le temps du Job). `--import-realm` seul ignorait toute nouvelle version du domaine |
| Secrets des clients de service (`.env`) | `creer-secrets.sh` (`gimac-clients-keycloak`), puis OpenBao (SOC-06) | Corrigé : `GIMAC_SECRET_INTERNAL` manquait aux 13 services (registration OAuth2 `internal`) |
| `kafka` (réplication 1 des sujets internes) | `jhipster-kafka` | Corrigé : réplication 1 des sujets de transactions et de partage ajoutée ; adresse du courtier (`SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS`, `SPRING_KAFKA_BOOTSTRAP_SERVERS` pour la passerelle) absente : les 14 services visaient `localhost:9092` (les `KAFKA_CONSUMER_*` générées ne sont lues par aucun service). Pas de volume (comme Compose) |
| `kafka-sujets-init` | Job `kafka-sujets-init` (`kafka-sujets.yaml`) | Corrigé. Sans dépendance d'ordre en Kubernetes : un service démarré avant la fin du Job créerait un sujet à 1 partition (en pratique, le Job s'achève avant le démarrage des JVM) |
| Journal d'audit des connexions de la passerelle (`GIMAC_PASSERELLE_JOURNAL_DECISIONS_ACTIF=true`) | `GIMAC_PASSERELLE_JOURNALDECISIONS_ACTIF=true` | Corrigé (inactif auparavant en Kubernetes) |
| `gestion-changements`, `simulateur-certification`, `autorite-horodatage` (WireMock) | Non déployés. `GIMAC_GESTION_CHANGEMENTS_URL` de MS-CERTIFICATION lue dans la ConfigMap `gimac-dispositifs-externes` (`creer-secrets.sh`), obligatoire ; sortie 443 de MS-CERTIFICATION | Choix : le simulacre déclare toute fiche `APPROUVE`, contraire au contrôle du changement en production. Le simulateur et l'horodatage ne sont lus par aucun service |
| `messagerie` (Mailpit) | Non déployé ; sortie SMTP de MS-IAM (25, 465, 587) | Serveur SMTP réel réglé dans la console (D-65) |
| `conversion-documents` (Gotenberg) | `conversion-documents.yaml` | Équivalent |
| `docker-socket-proxy` (exploitation, D-78) | Orchestrateur `kubernetes` : compte de service `gateway`, Role `gateway-exploitation` (`gateway-k8s/gateway-rbac.yml`), sortie 443/6443 | Équivalent par construction. OPA n'est pas une charge protégée (comme Compose) : l'arrêter depuis la console bloque toutes les routes filtrées |
| `cadvisor`, `node-exporter` | Aucun (hostPID, hostPath : interdits par le profil `baseline` de `gimac-des`) | Restant : à fournir par kube-prometheus-stack dans son propre espace de noms (kubelet/cAdvisor, node-exporter) et à faire collecter par `jhipster-prometheus`. Les requêtes de la passerelle (`name`, `container_label_com_docker_compose_project`) suivent les étiquettes de cAdvisor sous Docker : à adapter (`pod`, `container`, `namespace`) dans ms-gateway |
| `otel-collector`, `loki`, `tempo`, sources Loki et Tempo de Grafana | Aucun | Restant, sans effet : aucun service n'émet en OTLP sous Compose (aucune variable `OTEL_*`) |
| `grafana` (provisionnement de la source Prometheus) | `jhipster-grafana` et Job `jhipster-grafana-dashboard` (source `http://jhipster-prometheus:9090`, tableau JHipster) | Équivalent pour Prometheus. Grafana publiée par l'entrée générée (127.0.0.1 sous Compose) : à restreindre |
| Valkey par service et `gateway-valkey` | `valkey.yaml` partagé | Équivalent (D-25) |
| `stockage-objet` et `stockage-objet-init` (Ceph RGW mono-nœud) | Ceph RGW de l'exploitant (Secret `gimac-stockage-objet`), compartiments créés par l'exploitant ; sortie 443/7480 de MS-DOCUMENT | Corrigé (sortie absente) ; RGW hors de ce dossier |
| `openbao` (mode dev) | `creer-secrets.sh` | Prévu (SOC-06) |
| `registre-schemas` (Apicurio) | Aucun | Aucun service ne le lit en production |
| Profils `api-docs`, `secret-samples` | Profil `prod` seul | Voulu en production |
| Keycloak publié par l'entrée (`keycloak.gimac-des.des.gimac.local`) | Émetteur des jetons joint par les services via l'entrée | Restant : derrière le pare-feu sous Compose ; à restreindre (`whitelist-source-range`) à la mise en production |

## Vérifier sans grappe

```
kustomize build . | kubeconform -summary -ignore-missing-schemas
```

Dernier résultat (`infra/ci/valider-infra.sh`, schémas Kubernetes 1.33.0 et catalogue des CRD) : 151 objets, 151 valides, aucun ignoré.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | c0b91ee | Création : segments, politiques réseau, Valkey, Kyverno, retrait des secrets, corrections de schéma |
| 2026-09-28 | b20a049 | Couches de présentation et segment `presentation` |
| 2026-09-28 | — | Ajout de ce README |
| 2026-09-29 | — | Passerelle raccordée au Valkey partagé (sonde Redis, limitation de débit) |
| 2026-09-29 | — | Domaine `gimac` pour tous les services, clients de service et image Keycloak GIMAC ; administrateur de Keycloak par Secret (D-62) ; `kustomize build` vérifié (130 objets) |
| 2026-09-29 | — | MS-DOCUMENT : point d'accès et clés du dépôt WORM par le Secret `gimac-stockage-objet`, fourni par l'exploitant (`GIMAC_S3_*` à l'appel de `creer-secrets.sh`) |
| 2026-10-02 | - | MS-DOCUMENT : compartiment des dépôts fragmentés en cours `GIMAC_S3_COMPARTIMENT_DEPOTS` (`gimac-depots`, sans verrouillage, cycle de vie : objets 2 jours, téléversements incomplets 1 jour), créé par l'exploitant (D-74) |
| 2026-10-06 | - | Aperçu bureautique (D-83) : `conversion-documents.yaml` (Deployment et Service internes), segment `conversion` et politiques réseau ; `GIMAC_APERCU_URL` de MS-DOCUMENT ; entrées des couches : corps de 110 Mo (`proxy-body-size`, 1 Mo par défaut au contrôleur) ; `kustomize build` vérifié (138 objets) |
| 2026-10-06 | - | Services applicatifs : port exposé = port du conteneur (`gateway` 8080, `iam` 8081 ... `collaboration` 8093) au lieu du port 80 généré, pour les adresses `http://<service>:<port>` des `application-prod.yml` (dont `gimac.services.*` de la passerelle) et `GIMAC_PASSERELLE` des couches de présentation ; garde `test` sur le port cible ; l'entrée désigne les ports par nom, inchangée |
| 2026-10-06 | - | Équivalence avec Docker Compose (tableau ci-dessus) : OPA, Alertmanager, règles d'alerte, Job des sujets Kafka, copies `partages/` et `synchroniser-partages.sh` ; ServiceMonitor (`http`, `/management/prometheus`), RBAC `v1`, opérateur Prometheus généré retiré ; adresse du courtier Kafka et `GIMAC_SECRET_INTERNAL` des services ; supervision et journal des connexions de la passerelle ; Keycloak 26 (nom d'hôte v2, HTTP, port de gestion 9990, JGroups) ; règle publique de la passerelle retirée de l'entrée ; gestion des changements par `gimac-dispositifs-externes` ; `kustomize build` et kubeconform : 151 objets valides |
| 2026-10-06 | - | Copie `partages/opa/gimac.rego` et son entrée du générateur `opa-politiques` retirées avec la politique morte de Compose ; `synchroniser-partages.sh --verifier` conforme |
| 2026-10-07 | - | Domaine Keycloak mis à jour sans perte : `actualiser-domaine-keycloak.sh`, Job `keycloak-domaine` (hors Kustomize, validé par kubeconform dans `valider-infra.sh`), volume `keycloak-domaine-etat`, ConfigMap `keycloak-fusion-domaine` (copie `partages/keycloak/fusionner-domaine.py` tenue par `synchroniser-partages.sh`) ; `kustomize build` : 157 objets |
| 2026-10-13 | D-107 | D-107 : clé `reprise-service` du secret `gimac-clients-keycloak` (complétée par `creer-secrets.sh` si le secret existe), transmise à Keycloak et au Job du domaine ; règles OPA de reprise |
