# Lot 1 : correspondance entre le plan et le dépôt

Règle du relevé (PI-GIMAC-01 §0.1) : ce qui existe dans le dépôt fait foi. Le plan n'est pas réécrit ; ce tableau fait le lien. Les arbitrages sont consignés dans REC-GIMAC-01 (décision D-55).

## Services, paquets et chemins

| Plan | Dépôt | Motif |
|---|---|---|
| `services/mshomologation`, `services/mscertification` | `ms-homologation`, `ms-certification` à la racine | Organisation du dépôt (D-02) |
| Paquets `org.gimac.des.homologation`, `org.gimac.des.certification` | `cm.gimac.des.homologation`, `cm.gimac.des.certification` | Paquets du socle |
| Ports 8087 et 8088 | 8086 et 8087 | Ports du socle (passerelle, Docker Compose, Kubernetes) |
| `jdl/lot1-homologation.jdl`, `jdl/lot1-certification.jdl` | `ms-homologation/ms-homologation.jdl`, `ms-certification/ms-certification.jdl` | JDL du plan repris à l'identique, en-tête d'application adapté ; il remplace le JDL antérieur (d'après CC-GIMAC-01) |
| Régénération incrémentale | Régénération complète en JHipster 9.4.0 dans un dossier vierge, puis report du code propre au projet | Aucune entité antérieure à conserver (CRUD généré seulement) ; applique D-43 |
| `domain/model/` (entités JPA) | `domain/` (emplacement généré par JHipster) | Le dépôt fait foi |
| `cacheProvider no` | appliqué | Les caches Redis des deux services disparaissent du Docker Compose généré |
| Cucumber (`CucumberIT`) | `testFrameworks [cucumber]` ajouté au JDL | Exigé par le schéma de chaque récit |
| Identifiants `UUID` (chemins, événements, `liaisonId` entre services) | `id UUID` ajouté à chaque entité du JDL | Sans cette mention, JHipster génère des `Long` à séquence |

## Phase 0

| Plan | Dépôt | Motif |
|---|---|---|
| Changelog `20261005000001_lot1_post_generation.xml` | identique, dans chaque service ; comprend aussi `outbox_event` et `processed_event` | §0.3 et §0.4 |
| Bibliothèque `gimac-common-messaging` | absente : classes créées dans `infrastructure/messaging` de chaque service, plus `CorrelationContext`, `ProcessedEvent`, `Json` que le plan appelle sans les donner | §0.4 |
| `OutboxPublisher.publish(type, UUID, ...)` | surchargé : identifiant `String` ou `UUID` | HOM-01 appelle la variante `String` |
| `infra/kafka/creer-sujets-lot1.sh` | identique, via l'image officielle `apache/kafka` (l'image native du socle n'a pas les outils) et `REPLICATION` réglable (1 en DEV) | §0.5 |
| `infra/opa/policies/lot1_homologation.rego` | `infra/docker-compose/opa/politiques/lot1_authz.rego` et ses 7 tests | Corrigé : le portail exige l'institution ; les lectures authentifiées du DES non listées sont admises (sinon `GET /liaisons` serait refusé) |
| `application-lot1.yml`, `uri: lb://mshomologation` | 4 routes dans `application.yml` de la passerelle, `uri: ${gimac.services.<service>}` | `lb://` inopérant sans découverte (D-54) |
| Filtres `RewritePath`, `TokenRelay`, `OpaAuthorize` | `OpaAuthorize` (créé) avant `RewritePath` ; `TokenRelay` est un filtre par défaut | OPA évalue le chemin d'origine |
| Permissions `HOM_*`, `CER_*`, `INTERNAL_*` dans le jeton | `SecurityUtils` accepte les autorités en majuscules à tiret bas, en plus de `ROLE_*` | JHipster ne gardait que `ROLE_*` |
| `/bff/**` sans jeton : 401 | règle `/bff/**` authentifiée et point d'entrée 401 dans la passerelle | La passerelle redirigeait vers la connexion |
| Domaine Keycloak `gimac`, utilisateurs de test, `tester-cli` | `infra/keycloak/gimac-realm.json`, produit par `generer-realm-gimac.py` à partir du realm JHipster | Permissions, 6 profils, 7 utilisateurs, claim `institution_id`, clients de service ; secrets en variables `${...}` |
| `PASSWORD_<UTILISATEUR>` | `GIMAC_MOT_DE_PASSE_TEST` (un mot de passe de test commun, `.env`) | Simplicité ; aucun secret dans le dépôt |
| `scripts/token.sh` | `infra/docker-compose/scripts/token.sh`, jeton demandé depuis le réseau Docker | L'émetteur doit être `http://keycloak:9080/realms/gimac` |
| Clients `mshomologation-service`, `mscertification-service` | `homologation-service`, `certification-service` | Nom de base des services |
| `KafkaErrorHandlingConfiguration` du plan | celle reprise d'afriland (D-52), suffixe `.dlq`, rejeux 1 s, 5 s, 25 s ; partition choisie par le courtier | Le modulo 3 du plan échouerait si la file de rebut comptait moins de partitions |
| `gimac.adapters.document-deposit: real` | `stub` | MS-DOCUMENT n'expose encore que son CRUD généré : le dépôt probant n'est pas livré |
| Contrôle de sortie de la Phase 0 | `infra/docker-compose/scripts/verifier-phase0.sh` | §0.9 |
| `GET /bff/des/homologation/liaisons`, `GET /bff/des/certification/registre` à 200 | `LiaisonLot1Resource` et `RegistreLot1Resource` (lecture paginée sous `/api/v1`) | JHipster ne génère que `/api/<entités>` |
| Délai des tests générés : 15 s | 60 s dans les deux services du lot | Poste de développement chargé par d'autres projets : dépassements de délai sans rapport avec le code |
| `SignedDecision` modifiable par le CRUD généré | `PUT` et `PATCH` refusés (409 `CER_SIGNED_DECISION_IMMUTABLE`) ; tests générés adaptés | Déclencheur d'immuabilité du §0.3 ; D-35 |
| Test d'architecture | couches `application` et `infrastructure` ajoutées, couches générées bornées au paquet racine, services de domaine purs | `..service..` attrapait `domain.service` |

## Décisions de conception rappelées

| Plan | Référentiel appliqué |
|---|---|
| React 18 | React 19.2 (D-51) |
| Quitus « signé par le coffre de clés » | Assertion WebAuthn personnelle, puis cachet de la plateforme horodaté (D-35) |
| Noms d'événements | Tranchés récit par récit selon D-37 ; les écarts sont ajoutés ici au fil des récits |
