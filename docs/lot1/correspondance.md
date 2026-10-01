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

## Parcours de la liaison, étapes 3 à 10 : contrat attendu par l'interface (D-56 à D-59)

L'interface consomme déjà les routes réelles de l'étape 3 (`LiaisonsCadrageResource`). Pour les étapes 4 à 10, elle appelle les routes ci-dessous, servies aujourd'hui par l'implémentation simulée (`frontend/packages/lot1/src/services/simule/parcours.ts`) ; la session backend les livre sous ces chemins, ou le port HTTP (`services/http/parcours.ts`) est ajusté.

### Permissions nouvelles

| Permission | Profils | Objet |
|---|---|---|
| `HOM_STEP_MANAGE` | chargé et chef DDRM, chef DES, conducteur de projet | Conduite des étapes 4 à 10 : livrables de lancement et d'ateliers, blocages datés, affectation des ressources |
| `CER_PV_ESTABLISH` | conducteur de projet | Établissement du procès-verbal de certification (rang 18) |
| `CER_SECURITY_VERIFY` | RSSI | Vérification et archivage des attestations de sécurité (rang 23) |
| `CER_PORTAL_SIGN` | correspondant du participant | Signature du procès-verbal par le participant (rang 19) |

Le constat de la couverture (rang 17) relève de `CER_DOSSIER_VERIFY` (SNA). Le conducteur de projet reçoit `HOM_OPERATION_VALIDATE` : il est le validateur (V) de la plupart des opérations ; c'est au service d'interdire que le validateur soit aussi le responsable (`HOM_VALIDATOR_IS_RESPONSIBLE`) et de retirer `VALIDER` des actions permises dans ce cas.

Preuves : une opération `cle` (C1, D3, D12, D27) ne porte que des codes de contrôle ; la cérémonie C1 porte sa date de tenue, les trois porteurs de composantes et leurs trois codes de contrôle (`ceremonie`) ; une opération `secret` (29, M3, M7, M9, D2) ne porte qu'une trace, hors valeurs.

Réutilisées : `CER_QUITUS_TECHNIQUE_SIGN` devient la signature GIMAC du procès-verbal (chef du DES) ; `CER_QUITUS_CONFORMITE_SIGN` et `CER_QUITUS_DG_SIGN` portent les visas optionnels ; `CER_CHANGE_RECORD_LINK` la fiche de changement. Matrice complète : `frontend/packages/lot1/src/permissions.ts`.

### Champs attendus sur `LiaisonVue`

`statut` (CADREE à CLOSE_SANS_SUITE, D-57), `etapeCourante` (code d'étape du CR), `bloquee`, `prochaineEcheance`, `dateCibleProduction`. En leur absence, l'interface traduit `EtatLiaison` pour l'affichage (`versResume`), sans calcul de règle.

### Routes prévues (préfixe passerelle `/bff/des/homologation`, sauf mention)

| Route | Objet |
|---|---|
| `GET /liaisons/{id}/dossier` | Dossier numérique : périmètre service par service, deux couvertures, étapes à six éléments, blocages, interlocuteurs |
| `GET /liaisons/{id}/parcours/operations?etape=&fonction=` | Opérations du référentiel instanciées, RACI+E, plateforme, grain, SLA, preuves |
| `PUT /liaisons/{id}/ressources` | Affectation des ressources (rang 6) |
| `POST /parcours/operations/{id}/livrable` · `/preuves` · `/validation` (FIDO2) · `PUT …/intervenants` | Livrable, preuve (valeur de référence, code de contrôle de clé, jamais de secret), validation, qualités |
| `POST /liaisons/{id}/blocages` · `POST /blocages/{id}/levee` | Blocages datés |
| `POST /liaisons/{id}/couverture/constat` | Constat de la couverture (rang 17) |
| `POST /liaisons/{id}/services-supplementaires` | Nouvelle liaison pour un service supplémentaire (rang 35) |
| `GET /tableaux-de-bord/acteur?fonctions=` · `GET /tableaux-de-bord/pilotage` · `GET /intervenants?fonction=` | Tableaux de bord par fonction, pilotage (4 compteurs, répartition, délais, 6 questions de l'annexe B) |
| `GET/POST /liaisons/{id}/fiche-suivi` · `…/remise` · `…/constat` | Fiche de suivi des services et constat de mise en service (rangs 32 et 33) |
| `/bff/des/certification/liaisons/{id}/proces-verbal` (+ `/signature-gimac`, FIDO2) | Verrou 1 |
| `/bff/des/certification/liaisons/{id}/approbation-securite` (+ `/dossier`, `/fiche`, `/fiche/lecture`, `/attestations/{type}/verification`, `/archivage`, `/visas/{nature}`) | Verrou 2 |
| `/bff/portail/homologation/liaisons…`, `/bff/portail/homologation/taches`, `/bff/portail/certification/liaisons/{id}/proces-verbal/signature`, `…/attestations/{type}` | Portail du participant, cloisonné à l'institution du jeton |

## Contrat des routes : frontend, passerelle, OPA et services (vérifié le 01/10/2026)

Chaque appel HTTP du frontend (adaptateurs `services/http` des paquets `lot1`, `compte`, `socle` et des applications)
est rapproché, route par route, de la route de la passerelle (`/bff/{couche}/<service>/**` réécrit vers
`/api/v1[/portail]/**`), de la règle OPA (`lot1_authz.rego`) et du contrôleur du service (`@RequestMapping` et
`@PreAuthorize`). Le contrôle est automatique : `infra/ci/valider-contrat.sh` (appelé par `valider-tout.sh` et par la
CI, tâche frontend) exécute `infra/ci/contrat/routes.mjs` et échoue si une route du frontend n'a pas de contrepartie, si
OPA la refuse, si la permission exigée par OPA n'est pas acceptée par le `@PreAuthorize` du contrôleur, ou si un écart
connu est résolu sans être retiré de `infra/ci/contrat/ecarts-connus.txt`. Le tableau ci-dessous est produit par
`node infra/ci/contrat/routes.mjs --markdown`.

### Écarts trouvés sur la pile réelle et corrigés

| Écart | Côté corrigé | Correction |
|---|---|---|
| Aucun référentiel publié sur une base neuve (`HOM_REFERENTIAL_NONE_PUBLISHED`) : impossible d'ouvrir une liaison | MS-HOMOLOGATION | Brouillons de l'annexe A (carte, mobile, délégataire) chargés par Liquibase hors contexte `test`, à publier par DES-12 |
| Routes de DES-12 absentes (fiche d'une version, chronologie, contrôle restitué, simulation et migration HOM-03, liaisons d'une version) | MS-HOMOLOGATION | `ReferentialResource`, `ReferentialMigrationService`, filtres `versionId` et `actives` des liaisons |
| Validation d'opération refusée (`HOM_REAUTH_INVALID`) : finalité FIDO2 `HOM_OPERATION_VALIDATE` au lieu de `OPERATION_VALIDATE` | MS-HOMOLOGATION | Finalité alignée sur le frontend et MS-IAM |
| Périmètre vide quand le registre ne connaît aucun service souscrit | MS-HOMOLOGATION | Catalogue du type de projet, comme le simulateur |
| Verrous de la décision (rangs 6, 18 à 25, 32, 33) clos par un dépôt libre : liaison CERTIFIEE sans procès-verbal | MS-HOMOLOGATION, simulateur | Gestes dédiés : `409 HOM_OPERATION_DEDICATED_GESTURE` |
| Rang 26 rouvert après l'approbation de la sécurité, alors que la fiche est déjà référencée : parcours bloqué | MS-HOMOLOGATION, simulateur | Rang 26 clos par la fiche référencée à l'approbation |
| Fonction « exploitation » sans titulaire | MS-HOMOLOGATION | Tenue par la production monétique |
| Remise de la fiche de suivi : bouton réservé à `CER_DOSSIER_VERIFY`, service et OPA exigeant `HOM_STEP_MANAGE` | frontend, OPA, MS-HOMOLOGATION | Les deux permissions acceptées partout |
| Rang 34 sans geste dans l'interface | frontend, MS-HOMOLOGATION | Prouvé dans l'onglet des opérations |
| Dépôt de pièce refusé au chef du DES et à la conformité (`403`) | MS-DOCUMENT, OPA | `HOM_STEP_MANAGE` et `CER_CHANGE_RECORD_LINK` admis au dépôt |
| Fiche de changement lue sur un bouchon qui approuvait tout, même en production | MS-CERTIFICATION | Adaptateur réel `GET /api/v1/changements/{reference}` (profil `prod`) ; simulacre aligné sur le format de référence |
| Liste des parcours affichant un extrait d'UUID au lieu de `LIA-AAAA-NNN` | frontend | Référence du service |
| Chronologie des référentiels : champs `at`, `actor`, `correlationId` ignorés | frontend | Adaptateur aligné |
| Services simulés par défaut (données fictives servies si `VITE_SERVICES` absent) | frontend | Services réels par défaut ; simulateur seulement avec `VITE_SERVICES=simule`, élagué des paquets construits |
| Domaine Keycloak de production contenant les comptes de test | infra | `gimac-realm-production.json` (comptes de service seulement), `ARG DOMAINE` de l'image |

Les 46 écarts connus restants sont des écrans antérieurs au parcours D-56 à D-59 (DES-08, DES-09, DES-11, POR-03
antérieur, CER-14), encore présents dans le code mais remplacés par le dossier numérique ; leur retrait du frontend
est à décider.

### Tableau route par route

| Méthode | Chemin du frontend | Route de la passerelle | Service : chemin | Permission (OPA, puis @PreAuthorize) | État |
|---|---|---|---|---|---|
| POST | `/api/connexion/mot-de-passe-oublie` | (passerelle) | MS-GATEWAY : `/api/connexion/mot-de-passe-oublie` | session | conforme |
| POST | `/api/connexion/nouveau-mot-de-passe` | (passerelle) | MS-GATEWAY : `/api/connexion/nouveau-mot-de-passe` | session | conforme |
| POST | `/api/connexion` | (passerelle) | MS-GATEWAY : `/api/connexion` | session | conforme |
| POST | `/api/logout` | (passerelle) | MS-GATEWAY : `/api/logout` | session | conforme |
| GET | `/api/plateforme/alertes` | (passerelle) | MS-GATEWAY : `/api/plateforme/alertes` | session | conforme |
| GET | `/api/plateforme/configuration` | (passerelle) | MS-GATEWAY : `/api/plateforme/configuration` | session | conforme |
| GET | `/api/plateforme/flux` | (passerelle) | MS-GATEWAY : `/api/plateforme/flux` | session | conforme |
| GET | `/api/plateforme/journalisation/{…}` | (passerelle) | MS-GATEWAY : `/api/plateforme/journalisation/{service}` | session | conforme |
| POST | `/api/plateforme/journalisation/{…}` | (passerelle) | MS-GATEWAY : `/api/plateforme/journalisation/{service}` | session | conforme |
| GET | `/api/plateforme/serveurs` | (passerelle) | MS-GATEWAY : `/api/plateforme/serveurs` | session | conforme |
| GET | `/api/plateforme/services` | (passerelle) | MS-GATEWAY : `/api/plateforme/services` | session | conforme |
| DELETE | `/api/session/appareils/{…}` | (passerelle) | MS-GATEWAY : `/api/session/appareils/{id}` | session | conforme |
| DELETE | `/api/session/appareils` | (passerelle) | MS-GATEWAY : `/api/session/appareils` | session | conforme |
| GET | `/api/session/appareils` | (passerelle) | MS-GATEWAY : `/api/session/appareils` | session | conforme |
| GET | `/api/session/historique` | (passerelle) | MS-GATEWAY : `/api/session/historique` | session | conforme |
| POST | `/api/session/prolongation` | (passerelle) | MS-GATEWAY : `/api/session/prolongation` | session | conforme |
| GET | `/api/session` | (passerelle) | MS-GATEWAY : `/api/session` | session | conforme |
| GET | `/bff/console/collaboration/messagerie/annuaire` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/annuaire` | COL_MESSAGE_READ | conforme |
| POST | `/bff/console/collaboration/messagerie/conversations/{…}/lu` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations/{id}/lu` | COL_MESSAGE_READ | conforme |
| GET | `/bff/console/collaboration/messagerie/conversations/{…}/messages` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations/{id}/messages` | COL_MESSAGE_READ | conforme |
| POST | `/bff/console/collaboration/messagerie/conversations/{…}/messages` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations/{id}/messages` | COL_MESSAGE_SEND | conforme |
| GET | `/bff/console/collaboration/messagerie/conversations/{…}` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations/{id}` | COL_MESSAGE_READ | conforme |
| GET | `/bff/console/collaboration/messagerie/conversations` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations` | COL_MESSAGE_READ | conforme |
| POST | `/bff/console/collaboration/messagerie/conversations` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations` | COL_MESSAGE_SEND | conforme |
| GET | `/bff/console/collaboration/messagerie/non-lus` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/non-lus` | COL_MESSAGE_READ | conforme |
| GET | `/bff/console/collaboration/messagerie/pieces/{…}` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/pieces/{pieceId}` | COL_MESSAGE_READ | conforme |
| POST | `/bff/console/iam/comptes/{…}/actions-requises` | console-iam | MS-IAM : `/api/v1/comptes/{id}/actions-requises` | IAM_ACCOUNT_SECURITY | conforme |
| POST | `/bff/console/iam/comptes/{…}/activation` | console-iam | MS-IAM : `/api/v1/comptes/{id}/activation` | IAM_ACCOUNT_STATUS | conforme |
| POST | `/bff/console/iam/comptes/{…}/desactivation` | console-iam | MS-IAM : `/api/v1/comptes/{id}/desactivation` | IAM_ACCOUNT_STATUS | conforme |
| POST | `/bff/console/iam/comptes/{…}/mot-de-passe-provisoire` | console-iam | MS-IAM : `/api/v1/comptes/{id}/mot-de-passe-provisoire` | IAM_ACCOUNT_SECURITY | conforme |
| PUT | `/bff/console/iam/comptes/{…}/profils` | console-iam | MS-IAM : `/api/v1/comptes/{id}/profils` | IAM_ACCOUNT_ROLES | conforme |
| POST | `/bff/console/iam/comptes/{…}/second-facteur/reinitialisation` | console-iam | MS-IAM : `/api/v1/comptes/{id}/second-facteur/reinitialisation` | IAM_ACCOUNT_SECURITY | conforme |
| POST | `/bff/console/iam/comptes/{…}/sessions/fermeture` | console-iam | MS-IAM : `/api/v1/comptes/{id}/sessions/fermeture` | IAM_ACCOUNT_SECURITY | conforme |
| GET | `/bff/console/iam/comptes/{…}/sessions` | console-iam | MS-IAM : `/api/v1/comptes/{id}/sessions` | IAM_ACCOUNT_READ | conforme |
| DELETE | `/bff/console/iam/comptes/{…}` | console-iam | MS-IAM : `/api/v1/comptes/{id}` | IAM_ACCOUNT_DELETE | conforme |
| GET | `/bff/console/iam/comptes/{…}` | console-iam | MS-IAM : `/api/v1/comptes/indicateurs` | IAM_ACCOUNT_READ | conforme |
| PUT | `/bff/console/iam/comptes/{…}` | console-iam | MS-IAM : `/api/v1/comptes/{id}` | IAM_ACCOUNT_EDIT | conforme |
| GET | `/bff/console/iam/comptes/indicateurs` | console-iam | MS-IAM : `/api/v1/comptes/indicateurs` | IAM_ACCOUNT_READ | conforme |
| GET | `/bff/console/iam/comptes/journal` | console-iam | MS-IAM : `/api/v1/comptes/journal` | IAM_JOURNAL_READ | conforme |
| GET | `/bff/console/iam/comptes` | console-iam | MS-IAM : `/api/v1/comptes` | IAM_ACCOUNT_READ | conforme |
| POST | `/bff/console/iam/comptes` | console-iam | MS-IAM : `/api/v1/comptes` | IAM_ACCOUNT_CREATE | conforme |
| POST | `/bff/console/iam/configuration/messagerie/test` | console-iam | MS-IAM : `/api/v1/configuration/messagerie/test` | PLATFORM_MESSAGING_EDIT | conforme |
| GET | `/bff/console/iam/configuration/messagerie` | console-iam | MS-IAM : `/api/v1/configuration/messagerie` | PLATFORM_MESSAGING_EDIT | conforme |
| PUT | `/bff/console/iam/configuration/messagerie` | console-iam | MS-IAM : `/api/v1/configuration/messagerie` | PLATFORM_MESSAGING_EDIT | conforme |
| GET | `/bff/console/iam/configuration/permissions` | console-iam | MS-IAM : `/api/v1/configuration/permissions` | IAM_ACCOUNT_READ | conforme |
| GET | `/bff/console/iam/configuration/politique-securite` | console-iam | MS-IAM : `/api/v1/configuration/politique-securite` | PLATFORM_SECURITY_POLICY_READ | conforme |
| PUT | `/bff/console/iam/configuration/politique-securite` | console-iam | MS-IAM : `/api/v1/configuration/politique-securite` | PLATFORM_SECURITY_POLICY_EDIT | conforme |
| PUT | `/bff/console/iam/configuration/profils/{…}/permissions` | console-iam | MS-IAM : `/api/v1/configuration/profils/{code}/permissions` | IAM_PROFILE_EDIT | conforme |
| GET | `/bff/console/iam/configuration/profils` | console-iam | MS-IAM : `/api/v1/configuration/profils` | IAM_ACCOUNT_READ | conforme |
| DELETE | `/bff/console/iam/moi/avatar` | iam-libre-service | MS-IAM : `/api/v1/moi/avatar` | titulaire (2 facteurs) | conforme |
| PUT | `/bff/console/iam/moi/avatar` | iam-libre-service | MS-IAM : `/api/v1/moi/avatar` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/console/iam/moi/cles-securite/{…}` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite/{id}` | titulaire (2 facteurs) | conforme |
| POST | `/bff/console/iam/moi/cles-securite/defi` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite/defi` | titulaire (2 facteurs) | conforme |
| GET | `/bff/console/iam/moi/cles-securite` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite` | titulaire (2 facteurs) | conforme |
| POST | `/bff/console/iam/moi/cles-securite` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite` | titulaire (2 facteurs) | conforme |
| POST | `/bff/console/iam/moi/mot-de-passe` | iam-libre-service | MS-IAM : `/api/v1/moi/mot-de-passe` | titulaire (2 facteurs) | conforme |
| POST | `/bff/console/iam/moi/premiere-configuration` | iam-libre-service | MS-IAM : `/api/v1/moi/premiere-configuration` | titulaire (2 facteurs) | conforme |
| POST | `/bff/console/iam/moi/second-facteur/totp/enrolement` | iam-libre-service | MS-IAM : `/api/v1/moi/second-facteur/totp/enrolement` | titulaire (2 facteurs) | conforme |
| POST | `/bff/console/iam/moi/second-facteur/totp` | iam-libre-service | MS-IAM : `/api/v1/moi/second-facteur/totp` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/console/iam/moi/sessions/{…}` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions/{sessionId}` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/console/iam/moi/sessions` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions` | titulaire (2 facteurs) | conforme |
| GET | `/bff/console/iam/moi/sessions` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions` | titulaire (2 facteurs) | conforme |
| GET | `/bff/console/iam/moi` | iam-libre-service | MS-IAM : `/api/v1/moi` | titulaire (2 facteurs) | conforme |
| PUT | `/bff/console/iam/moi` | iam-libre-service | MS-IAM : `/api/v1/moi` | titulaire (2 facteurs) | conforme |
| POST | `/bff/console/iam/reauthentification/defi` | iam-libre-service | MS-IAM : `/api/v1/reauthentification/defi` | titulaire (2 facteurs) | conforme |
| GET | `/bff/console/live` | (passerelle) | MS-GATEWAY : `/bff/{couche}/live` | — | conforme |
| POST | `/bff/des/certification/attestations/{…}/demandes-de-statut` | des-certification | MS-CERTIFICATION : `/api/v1/attestations/{}/demandes-de-statut` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/attestations/{…}/fiche` | des-certification | MS-CERTIFICATION : `/api/v1/attestations/{}/fiche` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/attestations/{…}` | des-certification | MS-CERTIFICATION : `/api/v1/attestations/{}` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| POST | `/bff/des/certification/circuits/{…}/quitus/{…}` | des-certification | MS-CERTIFICATION : `/api/v1/circuits/{}/quitus/{}` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/circuits/{…}` | des-certification | MS-CERTIFICATION : `/api/v1/circuits/{}` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| POST | `/bff/des/certification/demandes-de-statut/{…}/approbation` | des-certification | MS-CERTIFICATION : `/api/v1/demandes-de-statut/{}/approbation` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| POST | `/bff/des/certification/dossiers/{…}/attestation` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/attestation` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/dossiers/{…}/conditions` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/conditions` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/dossiers/{…}/fiche-de-changement` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/fiche-de-changement` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| PUT | `/bff/des/certification/dossiers/{…}/fiche-de-changement` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/fiche-de-changement` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/dossiers/{…}/historique` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/historique` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/dossiers/{…}/horodatages` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/horodatages` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/dossiers/{…}/proces-verbal` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/proces-verbal` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| POST | `/bff/des/certification/dossiers/{…}/proces-verbal` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/proces-verbal` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| POST | `/bff/des/certification/dossiers/{…}/verification` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}/verification` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/dossiers/{…}` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers/{}` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| GET | `/bff/des/certification/dossiers` | des-certification | MS-CERTIFICATION : `/api/v1/dossiers` | 2 facteurs | écart connu : DES-11 (dossier de mise en service, circuit des quatre quitus) : remplacé par les verrous de D-56 ; CER-08 à CER-15 à livrer |
| POST | `/bff/des/certification/liaisons/{…}/approbation-securite/archivage` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/approbation-securite/archivage` | CER_SECURITY_VERIFY | conforme |
| POST | `/bff/des/certification/liaisons/{…}/approbation-securite/attestations/{…}/verification` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/approbation-securite/attestations/{type}/verification` | CER_SECURITY_VERIFY | conforme |
| POST | `/bff/des/certification/liaisons/{…}/approbation-securite/dossier` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/approbation-securite/dossier` | CER_CHANGE_RECORD_LINK | conforme |
| POST | `/bff/des/certification/liaisons/{…}/approbation-securite/fiche/lecture` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/approbation-securite/fiche/lecture` | CER_CHANGE_RECORD_LINK ou CER_SECURITY_VERIFY | conforme |
| PUT | `/bff/des/certification/liaisons/{…}/approbation-securite/fiche` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/approbation-securite/fiche` | CER_CHANGE_RECORD_LINK | conforme |
| POST | `/bff/des/certification/liaisons/{…}/approbation-securite/visas/{…}` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/approbation-securite/visas/CONFORMITE` | CER_QUITUS_CONFORMITE_SIGN | conforme |
| GET | `/bff/des/certification/liaisons/{…}/approbation-securite` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/approbation-securite` | CER_DOSSIER_READ | conforme |
| POST | `/bff/des/certification/liaisons/{…}/proces-verbal/signature-gimac` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/proces-verbal/signature-gimac` | CER_QUITUS_TECHNIQUE_SIGN | conforme |
| GET | `/bff/des/certification/liaisons/{…}/proces-verbal` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/proces-verbal` | CER_DOSSIER_READ | conforme |
| POST | `/bff/des/certification/liaisons/{…}/proces-verbal` | des-certification | MS-CERTIFICATION : `/api/v1/liaisons/{liaisonId}/proces-verbal` | CER_PV_ESTABLISH | conforme |
| GET | `/bff/des/certification/registre/export` | des-certification | MS-CERTIFICATION : `/api/v1/registre/export` | 2 facteurs | écart connu : CER-14 : export du registre des certifications à livrer |
| GET | `/bff/des/certification/registre` | des-certification | MS-CERTIFICATION : `/api/v1/registre` | CER_REGISTER_READ | conforme |
| GET | `/bff/des/collaboration/messagerie/annuaire` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/annuaire` | COL_MESSAGE_READ | conforme |
| POST | `/bff/des/collaboration/messagerie/conversations/{…}/lu` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations/{id}/lu` | COL_MESSAGE_READ | conforme |
| GET | `/bff/des/collaboration/messagerie/conversations/{…}/messages` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations/{id}/messages` | COL_MESSAGE_READ | conforme |
| POST | `/bff/des/collaboration/messagerie/conversations/{…}/messages` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations/{id}/messages` | COL_MESSAGE_SEND | conforme |
| GET | `/bff/des/collaboration/messagerie/conversations/{…}` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations/{id}` | COL_MESSAGE_READ | conforme |
| GET | `/bff/des/collaboration/messagerie/conversations` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations` | COL_MESSAGE_READ | conforme |
| POST | `/bff/des/collaboration/messagerie/conversations` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/conversations` | COL_MESSAGE_SEND | conforme |
| GET | `/bff/des/collaboration/messagerie/non-lus` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/non-lus` | COL_MESSAGE_READ | conforme |
| GET | `/bff/des/collaboration/messagerie/pieces/{…}` | des-collaboration-messagerie | MS-COLLABORATION : `/api/v1/messagerie/pieces/{pieceId}` | COL_MESSAGE_READ | conforme |
| POST | `/bff/des/document/documents` | depot-document | MS-DOCUMENT : `/api/v1/documents` | PAR_ADHESION_INSTRUCT ou HOM_EVIDENCE_SUBMIT ou DOC_DEPOSIT | conforme |
| POST | `/bff/des/homologation/blocages/{…}/levee` | des-homologation | MS-HOMOLOGATION : `/api/v1/blocages/{id}/levee` | HOM_STEP_MANAGE | conforme |
| GET | `/bff/des/homologation/intervenants` | des-homologation | MS-HOMOLOGATION : `/api/v1/intervenants` | HOM_LIAISON_READ ou HOM_OPERATION_ASSIGN ou HOM_STEP_MANAGE | conforme |
| POST | `/bff/des/homologation/liaisons/{…}/blocages` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/blocages` | HOM_STEP_MANAGE | conforme |
| GET | `/bff/des/homologation/liaisons/{…}/campagnes` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/campagnes` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| PUT | `/bff/des/homologation/liaisons/{…}/chef-de-projet` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/chef-de-projet` | HOM_PROJECT_DESIGNATE | conforme |
| POST | `/bff/des/homologation/liaisons/{…}/couverture/constat` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/couverture/constat` | CER_DOSSIER_VERIFY | conforme |
| GET | `/bff/des/homologation/liaisons/{…}/couvertures` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/couvertures` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| POST | `/bff/des/homologation/liaisons/{…}/degel` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/degel` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/liaisons/{…}/dossier-de-preuves` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/dossier-de-preuves` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| POST | `/bff/des/homologation/liaisons/{…}/dossier-de-preuves` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/dossier-de-preuves` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/liaisons/{…}/dossier` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/dossier` | HOM_LIAISON_READ | conforme |
| POST | `/bff/des/homologation/liaisons/{…}/fiche-suivi/constat` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/fiche-suivi/constat` | CER_DOSSIER_VERIFY | conforme |
| POST | `/bff/des/homologation/liaisons/{…}/fiche-suivi/remise` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/fiche-suivi/remise` | HOM_STEP_MANAGE ou CER_DOSSIER_VERIFY | conforme |
| GET | `/bff/des/homologation/liaisons/{…}/fiche-suivi` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/fiche-suivi` | HOM_LIAISON_READ | conforme |
| GET | `/bff/des/homologation/liaisons/{…}/graphe` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/graphe` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/liaisons/{…}/historique` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/historique` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/liaisons/{…}/operations` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/operations` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/liaisons/{…}/parcours/operations` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/parcours/operations` | HOM_LIAISON_READ | conforme |
| PUT | `/bff/des/homologation/liaisons/{…}/ressources` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/ressources` | HOM_STEP_MANAGE | conforme |
| GET | `/bff/des/homologation/liaisons/{…}/scenarios-inconnus` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{}/scenarios-inconnus` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| POST | `/bff/des/homologation/liaisons/{…}/services-supplementaires` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/{id}/services-supplementaires` | HOM_LIAISON_OPEN ou HOM_STEP_MANAGE | conforme |
| GET | `/bff/des/homologation/liaisons/{…}` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/conducteurs` | HOM_PROJECT_DESIGNATE ou HOM_LIAISON_OPEN | conforme |
| GET | `/bff/des/homologation/liaisons/conducteurs` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons/conducteurs` | HOM_PROJECT_DESIGNATE ou HOM_LIAISON_OPEN | conforme |
| GET | `/bff/des/homologation/liaisons` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons` | HOM_LIAISON_LIST ou HOM_LIAISON_READ | conforme |
| POST | `/bff/des/homologation/liaisons` | des-homologation | MS-HOMOLOGATION : `/api/v1/liaisons` | HOM_LIAISON_OPEN | conforme |
| GET | `/bff/des/homologation/operations/{…}/attributions` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/attributions` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/operations/{…}/elements-de-cloture` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/elements-de-cloture` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/operations/{…}/grille` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/grille` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| PUT | `/bff/des/homologation/operations/{…}/grille` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/grille` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/operations/{…}/preuves` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/preuves` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| POST | `/bff/des/homologation/operations/{…}/preuves` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/preuves` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| POST | `/bff/des/homologation/operations/{…}/reouverture` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/reouverture` | HOM_OPERATION_REOPEN | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| PUT | `/bff/des/homologation/operations/{…}/responsable` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/responsable` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| POST | `/bff/des/homologation/operations/{…}/validation` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}/validation` | HOM_OPERATION_VALIDATE | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/operations/{…}` | des-homologation | MS-HOMOLOGATION : `/api/v1/operations/{}` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| PUT | `/bff/des/homologation/parcours/operations/{…}/intervenants` | des-homologation | MS-HOMOLOGATION : `/api/v1/parcours/operations/{id}/intervenants` | HOM_OPERATION_ASSIGN ou HOM_STEP_MANAGE | conforme |
| POST | `/bff/des/homologation/parcours/operations/{…}/livrable` | des-homologation | MS-HOMOLOGATION : `/api/v1/parcours/operations/{id}/livrable` | HOM_STEP_MANAGE ou HOM_EVIDENCE_SUBMIT | conforme |
| POST | `/bff/des/homologation/parcours/operations/{…}/preuves` | des-homologation | MS-HOMOLOGATION : `/api/v1/parcours/operations/{id}/preuves` | HOM_EVIDENCE_SUBMIT | conforme |
| POST | `/bff/des/homologation/parcours/operations/{…}/validation` | des-homologation | MS-HOMOLOGATION : `/api/v1/parcours/operations/{id}/validation` | HOM_OPERATION_VALIDATE | conforme |
| POST | `/bff/des/homologation/referentiels/{…}/controle` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels/{id}/controle` | HOM_REFERENTIEL_EDIT | conforme |
| GET | `/bff/des/homologation/referentiels/{…}/historique` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels/{id}/historique` | HOM_REFERENTIEL_READ | conforme |
| POST | `/bff/des/homologation/referentiels/{…}/migrations/simulation` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels/{id}/migrations/simulation` | HOM_REFERENTIEL_MIGRATE | conforme |
| POST | `/bff/des/homologation/referentiels/{…}/migrations` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels/{id}/migrations` | HOM_REFERENTIEL_MIGRATE | conforme |
| GET | `/bff/des/homologation/referentiels/{…}/operations` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels/{id}/operations` | HOM_REFERENTIEL_READ | conforme |
| POST | `/bff/des/homologation/referentiels/{…}/publication` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels/{id}/publication` | HOM_REFERENTIEL_PUBLISH | conforme |
| GET | `/bff/des/homologation/referentiels/{…}` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels/{id}` | HOM_REFERENTIEL_READ | conforme |
| GET | `/bff/des/homologation/referentiels` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels` | HOM_REFERENTIEL_READ | conforme |
| POST | `/bff/des/homologation/referentiels` | des-homologation | MS-HOMOLOGATION : `/api/v1/referentiels` | HOM_REFERENTIEL_EDIT | conforme |
| GET | `/bff/des/homologation/reprise/etat` | des-homologation | MS-HOMOLOGATION : `/api/v1/reprise/etat` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| POST | `/bff/des/homologation/scenarios-inconnus/{…}/qualification` | des-homologation | MS-HOMOLOGATION : `/api/v1/scenarios-inconnus/{}/qualification` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/tableau-de-bord/liaisons` | des-homologation | MS-HOMOLOGATION : `/api/v1/tableau-de-bord/liaisons` | 2 facteurs | écart connu : DES-08, DES-09 et clôture d'opération (HOM-07 à HOM-19) : remplacés par le dossier numérique du parcours (D-57) |
| GET | `/bff/des/homologation/tableaux-de-bord/acteur` | des-homologation | MS-HOMOLOGATION : `/api/v1/tableaux-de-bord/acteur` | HOM_DASHBOARD_READ | conforme |
| GET | `/bff/des/homologation/tableaux-de-bord/pilotage` | des-homologation | MS-HOMOLOGATION : `/api/v1/tableaux-de-bord/pilotage` | HOM_DASHBOARD_READ | conforme |
| DELETE | `/bff/des/iam/moi/avatar` | iam-libre-service | MS-IAM : `/api/v1/moi/avatar` | titulaire (2 facteurs) | conforme |
| PUT | `/bff/des/iam/moi/avatar` | iam-libre-service | MS-IAM : `/api/v1/moi/avatar` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/des/iam/moi/cles-securite/{…}` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite/{id}` | titulaire (2 facteurs) | conforme |
| POST | `/bff/des/iam/moi/cles-securite/defi` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite/defi` | titulaire (2 facteurs) | conforme |
| GET | `/bff/des/iam/moi/cles-securite` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite` | titulaire (2 facteurs) | conforme |
| POST | `/bff/des/iam/moi/cles-securite` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite` | titulaire (2 facteurs) | conforme |
| POST | `/bff/des/iam/moi/mot-de-passe` | iam-libre-service | MS-IAM : `/api/v1/moi/mot-de-passe` | titulaire (2 facteurs) | conforme |
| POST | `/bff/des/iam/moi/premiere-configuration` | iam-libre-service | MS-IAM : `/api/v1/moi/premiere-configuration` | titulaire (2 facteurs) | conforme |
| POST | `/bff/des/iam/moi/second-facteur/totp/enrolement` | iam-libre-service | MS-IAM : `/api/v1/moi/second-facteur/totp/enrolement` | titulaire (2 facteurs) | conforme |
| POST | `/bff/des/iam/moi/second-facteur/totp` | iam-libre-service | MS-IAM : `/api/v1/moi/second-facteur/totp` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/des/iam/moi/sessions/{…}` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions/{sessionId}` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/des/iam/moi/sessions` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions` | titulaire (2 facteurs) | conforme |
| GET | `/bff/des/iam/moi/sessions` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions` | titulaire (2 facteurs) | conforme |
| GET | `/bff/des/iam/moi` | iam-libre-service | MS-IAM : `/api/v1/moi` | titulaire (2 facteurs) | conforme |
| PUT | `/bff/des/iam/moi` | iam-libre-service | MS-IAM : `/api/v1/moi` | titulaire (2 facteurs) | conforme |
| POST | `/bff/des/iam/reauthentification/defi` | iam-libre-service | MS-IAM : `/api/v1/reauthentification/defi` | titulaire (2 facteurs) | conforme |
| GET | `/bff/des/live` | (passerelle) | MS-GATEWAY : `/bff/{couche}/live` | — | conforme |
| PUT | `/bff/des/participant/institutions/{…}/conditions-adhesion` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}/conditions-adhesion` | PAR_INSTITUTION_EDIT | conforme |
| POST | `/bff/des/participant/institutions/{…}/pieces/{…}/depot` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}/pieces/{code}/depot` | PAR_ADHESION_INSTRUCT | conforme |
| POST | `/bff/des/participant/institutions/{…}/pieces/{…}/rejet` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}/pieces/{code}/rejet` | PAR_ADHESION_INSTRUCT | conforme |
| POST | `/bff/des/participant/institutions/{…}/pieces/{…}/validation` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}/pieces/{code}/validation` | PAR_ADHESION_INSTRUCT | conforme |
| GET | `/bff/des/participant/institutions/{…}/prerequis` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}/prerequis` | PAR_INSTITUTION_READ | conforme |
| POST | `/bff/des/participant/institutions/{…}/transitions/{…}/approbation` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}/transitions/{transitionId}/approbation` | PAR_TRANSITION_APPROVE | conforme |
| POST | `/bff/des/participant/institutions/{…}/transitions/{…}/rejet` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}/transitions/{transitionId}/rejet` | PAR_TRANSITION_APPROVE | conforme |
| POST | `/bff/des/participant/institutions/{…}/transitions` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}/transitions` | PAR_TRANSITION_REQUEST | conforme |
| GET | `/bff/des/participant/institutions/{…}` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/indicateurs` | PAR_INSTITUTION_READ | conforme |
| PUT | `/bff/des/participant/institutions/{…}` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/{id}` | PAR_INSTITUTION_EDIT | conforme |
| GET | `/bff/des/participant/institutions/indicateurs` | des-participant | MS-PARTICIPANT : `/api/v1/institutions/indicateurs` | PAR_INSTITUTION_READ | conforme |
| GET | `/bff/des/participant/institutions` | des-participant | MS-PARTICIPANT : `/api/v1/institutions` | PAR_INSTITUTION_READ | conforme |
| POST | `/bff/des/participant/institutions` | des-participant | MS-PARTICIPANT : `/api/v1/institutions` | PAR_INSTITUTION_CREATE | conforme |
| GET | `/bff/portail/certification/certifications/{…}/attestation` | portail-certification | MS-CERTIFICATION : `/api/v1/portail/certifications/{}/attestation` | CER_PORTAL_READ | écart connu : POR-03 antérieur : remplacé par le portail du parcours (D-58) |
| GET | `/bff/portail/certification/certifications` | portail-certification | MS-CERTIFICATION : `/api/v1/portail/certifications` | CER_PORTAL_READ | écart connu : POR-03 antérieur : remplacé par le portail du parcours (D-58) |
| GET | `/bff/portail/certification/liaisons/{…}/approbation-securite` | portail-certification | MS-CERTIFICATION : `/api/v1/portail/liaisons/{liaisonId}/approbation-securite` | CER_PORTAL_READ | conforme |
| POST | `/bff/portail/certification/liaisons/{…}/attestations/{…}` | portail-certification | MS-CERTIFICATION : `/api/v1/portail/liaisons/{liaisonId}/attestations/{type}` | HOM_PORTAL_SUBMIT | conforme |
| POST | `/bff/portail/certification/liaisons/{…}/proces-verbal/signature` | portail-certification | MS-CERTIFICATION : `/api/v1/portail/liaisons/{liaisonId}/proces-verbal/signature` | CER_PORTAL_SIGN | conforme |
| GET | `/bff/portail/certification/liaisons/{…}/proces-verbal` | portail-certification | MS-CERTIFICATION : `/api/v1/portail/liaisons/{liaisonId}/proces-verbal` | CER_PORTAL_READ | conforme |
| GET | `/bff/portail/homologation/liaisons/{…}/a-faire/{…}` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/liaisons/{}/a-faire/{}` | HOM_PORTAL_READ | écart connu : POR-03 antérieur : remplacé par le portail du parcours (D-58) |
| GET | `/bff/portail/homologation/liaisons/{…}/a-faire` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/liaisons/{}/a-faire` | HOM_PORTAL_READ | écart connu : POR-03 antérieur : remplacé par le portail du parcours (D-58) |
| PUT | `/bff/portail/homologation/liaisons/{…}/coordonnees-techniques` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/liaisons/{id}/coordonnees-techniques` | HOM_PORTAL_SUBMIT | conforme |
| GET | `/bff/portail/homologation/liaisons/{…}/dossier` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/liaisons/{id}/dossier` | HOM_PORTAL_READ | conforme |
| GET | `/bff/portail/homologation/liaisons/{…}/fiche-suivi` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/liaisons/{id}/fiche-suivi` | HOM_PORTAL_READ | conforme |
| PUT | `/bff/portail/homologation/liaisons/{…}/fiche-suivi` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/liaisons/{id}/fiche-suivi` | HOM_PORTAL_SUBMIT | conforme |
| GET | `/bff/portail/homologation/liaisons/{…}` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/liaisons/{}` | HOM_PORTAL_READ | écart connu : POR-03 antérieur : remplacé par le portail du parcours (D-58) |
| GET | `/bff/portail/homologation/liaisons` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/liaisons` | HOM_PORTAL_READ | conforme |
| POST | `/bff/portail/homologation/operations/{…}/livrable` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/operations/{id}/livrable` | HOM_PORTAL_SUBMIT | conforme |
| POST | `/bff/portail/homologation/operations/{…}/preuves` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/operations/{}/preuves` | HOM_PORTAL_SUBMIT | écart connu : POR-03 antérieur : remplacé par le portail du parcours (D-58) |
| GET | `/bff/portail/homologation/taches` | portail-homologation | MS-HOMOLOGATION : `/api/v1/portail/taches` | HOM_PORTAL_READ | conforme |
| DELETE | `/bff/portail/iam/moi/avatar` | iam-libre-service | MS-IAM : `/api/v1/moi/avatar` | titulaire (2 facteurs) | conforme |
| PUT | `/bff/portail/iam/moi/avatar` | iam-libre-service | MS-IAM : `/api/v1/moi/avatar` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/portail/iam/moi/cles-securite/{…}` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite/{id}` | titulaire (2 facteurs) | conforme |
| POST | `/bff/portail/iam/moi/cles-securite/defi` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite/defi` | titulaire (2 facteurs) | conforme |
| GET | `/bff/portail/iam/moi/cles-securite` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite` | titulaire (2 facteurs) | conforme |
| POST | `/bff/portail/iam/moi/cles-securite` | iam-libre-service | MS-IAM : `/api/v1/moi/cles-securite` | titulaire (2 facteurs) | conforme |
| POST | `/bff/portail/iam/moi/mot-de-passe` | iam-libre-service | MS-IAM : `/api/v1/moi/mot-de-passe` | titulaire (2 facteurs) | conforme |
| POST | `/bff/portail/iam/moi/premiere-configuration` | iam-libre-service | MS-IAM : `/api/v1/moi/premiere-configuration` | titulaire (2 facteurs) | conforme |
| POST | `/bff/portail/iam/moi/second-facteur/totp/enrolement` | iam-libre-service | MS-IAM : `/api/v1/moi/second-facteur/totp/enrolement` | titulaire (2 facteurs) | conforme |
| POST | `/bff/portail/iam/moi/second-facteur/totp` | iam-libre-service | MS-IAM : `/api/v1/moi/second-facteur/totp` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/portail/iam/moi/sessions/{…}` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions/{sessionId}` | titulaire (2 facteurs) | conforme |
| DELETE | `/bff/portail/iam/moi/sessions` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions` | titulaire (2 facteurs) | conforme |
| GET | `/bff/portail/iam/moi/sessions` | iam-libre-service | MS-IAM : `/api/v1/moi/sessions` | titulaire (2 facteurs) | conforme |
| GET | `/bff/portail/iam/moi` | iam-libre-service | MS-IAM : `/api/v1/moi` | titulaire (2 facteurs) | conforme |
| PUT | `/bff/portail/iam/moi` | iam-libre-service | MS-IAM : `/api/v1/moi` | titulaire (2 facteurs) | conforme |
| POST | `/bff/portail/iam/reauthentification/defi` | iam-libre-service | MS-IAM : `/api/v1/reauthentification/defi` | titulaire (2 facteurs) | conforme |
| GET | `/bff/portail/live` | (passerelle) | MS-GATEWAY : `/bff/{couche}/live` | — | conforme |
