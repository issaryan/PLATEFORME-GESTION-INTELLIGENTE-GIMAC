# Sandbox GIMAC : adaptations de la plateforme du DES (épopée EXI, D-115)

La Sandbox GIMAC (dépôt SandBox-Gimac, documents TDR, CA, CC et CP-SBX-GIMAC-02) ne reconstruit aucune capacité de la
plateforme du DES. Elle se raccorde à ses composants par leurs seuls points d'entrée prévus. L'épopée EXI du carnet de
la sandbox (25 récits, 109 points) regroupe ces adaptations, réalisées dans ce dépôt.

## 1. Principes

- **Aucun second registre, aucune seconde administration des comptes ou des droits, aucune seconde supervision** : la
  sandbox utilise MS-PARTICIPANT, MS-IAM et MS-MONITORING tels qu'ils sont.
- **La sandbox n'écrit jamais dans MS-HOMOLOGATION ni dans MS-CERTIFICATION** : la plateforme du DES lit la sandbox
  (contrat de lecture, SBX-LECTURE) et décide seule de la certification.
- **Identité unique** : une personne a un seul compte, partagé entre la plateforme et la sandbox.
- **Connexion** : la console et le portail de la sandbox sont des applications distinctes, mais elles appartiennent aux
  couches console et portail de la passerelle. Leurs appels passent sous `/bff/console/sbx-*` et `/bff/portail/sbx-*`,
  avec la même session et le même écran de connexion que la plateforme (D-65, aucune page de Keycloak). Aucun nouveau
  client Keycloak n'est créé pour elles. Quand elles seront publiées, leurs origines s'ajouteront aux origines admises
  des clés de sécurité (`GIMAC_WEBAUTHN_ORIGINES`, réauthentification FIDO2).
- **Second facteur** : le carnet parle de la revendication `amr` ; la plateforme porte le niveau d'assurance dans `acr`
  (D-61). `acr = 2` vaut second facteur, et toute route `/api/v1/**` l'exige déjà.
- **Couverture** : les classes ajoutées pour EXI servent les membres de la sandbox ; elles sont tenues à 98 % de
  couverture, sans relever le seuil global du service qui les porte.
- **Récits dont l'acteur est un service de la sandbox encore absent** (saga MATRICE de SBX-ESPACE, événements
  `sbx-*`) : ils sont couverts par des tests de contrat qui rejouent l'appel ou l'enveloppe d'événement. La démonstration
  de bout en bout est refaite à l'arrivée du service ; elle est notée dans le tableau du §4.

## 2. Identité et matrice des permissions (US-EXI-01, US-EXI-02)

### 2.1 Permissions

`SBX_<MODULE>_<FONCTION>` : douze modules et cinq fonctions (CA-SBX-GIMAC-02 UC-INS-04), soit soixante permissions,
déclarées dans `infra/keycloak/generer-realm-gimac.py` (source unique du domaine Keycloak, du domaine de production et
du catalogue du frontend).

| Module | Objet | Module | Objet |
|---|---|---|---|
| HAB | Habilitations | MOB | Interface mobile |
| RAC | Raccordements | ISO | Interface ISO 20022 |
| REF | Référentiels | CAR | Interface carte |
| SCN | Scénarios | CER | Campagnes de certification |
| DON | Données de test | TRA | Traces |
| SIM | Simulation | LEC | Contrat de lecture |

Fonctions : ADMINISTRATION, SIMULATION, EXECUTION (de campagne), VALIDATION (de verdict), CONSULTATION.

Ajout de D-119, en complément de D-121 : un treizième module, `SUP` (supervision de la sandbox depuis la plateforme),
avec la seule permission `SBX_SUP_CONSULTATION`, soit soixante et une permissions `SBX_*`. Elle s'attribue par profil
depuis la console ; aucun profil livré ne la reçoit d'office (vérifié par le générateur). SBX-TRACE l'accepte sur ses
quatre routes de lecture de supervision. MS-IAM refuse toute autre fonction du module SUP (`ReglesSandbox`), et le
catalogue des droits porte la fonctionnalité `sandboxSup`.

### 2.2 Valeurs de départ

Valeurs configurables ensuite depuis la console (écran des profils, D-86). Les fonctions du GIMAC s'ajoutent aux profils
existants ; les rôles de portée institution sont six profils externes `ROLE_SBX_*` du portail. Le prestataire de
maintenance passe par un accès temporaire par délégation (US-EXI-24), pas par un profil.

| Rôle de la sandbox (UC-INS-04) | Profil | Permissions SBX de départ |
|---|---|---|
| Administrateur GIMAC | ROLE_SUPER_ADMIN | ADMINISTRATION et CONSULTATION de tous les modules |
| Responsable de produit (chef du DES) | ROLE_CHEF_DES | CONSULTATION de tous les modules |
| Ingénieur certificateur | ROLE_SNA | CER : EXECUTION, VALIDATION, CONSULTATION ; SIM, MOB, ISO, CAR : SIMULATION, CONSULTATION ; RAC, REF, SCN, DON, TRA, LEC : CONSULTATION |
| RSSI | ROLE_RSSI | RAC, DON, TRA, LEC : ADMINISTRATION ; LEC : VALIDATION (approbation du périmètre de lecture, CC-SBX-GIMAC-02 §D.8) ; CONSULTATION de tous les modules |
| Production monétique | ROLE_PRODUCTION_MONETIQUE | CAR, DON, TRA : ADMINISTRATION ; CAR, DON : SIMULATION ; RAC, REF, TRA, CAR, DON, SIM, CER, LEC : CONSULTATION |
| Exploitant | ROLE_ADMIN_SYSTEME | TRA : ADMINISTRATION, CONSULTATION ; RAC, SIM, CER, LEC : CONSULTATION |
| Administrateur de l'institution | ROLE_SBX_ADMIN_INSTITUTION (externe) | HAB : ADMINISTRATION, CONSULTATION ; RAC, CER, TRA : CONSULTATION |
| Chef de projet participant | ROLE_SBX_CHEF_PROJET (externe) | CER : EXECUTION, CONSULTATION ; SIM, MOB, ISO, CAR : SIMULATION, CONSULTATION ; RAC, DON, SCN, TRA, REF : CONSULTATION |
| Développeur du participant | ROLE_SBX_DEVELOPPEUR (externe) | RAC : ADMINISTRATION, CONSULTATION ; CER : EXECUTION, CONSULTATION (cas affectés, CA-SBX-GIMAC-02 UC-CER-04) ; DON, SIM, MOB, ISO, CAR : SIMULATION, CONSULTATION ; SCN, TRA, REF : CONSULTATION |
| Testeur du participant | ROLE_SBX_TESTEUR (externe) | CER, SIM, MOB, ISO, CAR : EXECUTION, CONSULTATION ; TRA, REF : CONSULTATION |
| Lecteur du participant | ROLE_SBX_LECTEUR (externe) | CER, RAC, REF, SIM, MOB, ISO, CAR : CONSULTATION |
| Partenaire technique mandaté | ROLE_SBX_PARTENAIRE (externe) | comme le développeur, plus CER : EXECUTION, CONSULTATION |

Les profils externes reçoivent aussi `NOT_INBOX_READ` (cloche du portail), comme le correspondant.

### 2.3 Règles

- **RG-05, validation réservée au GIMAC** : `SBX_*_VALIDATION` n'est jamais portée par un profil externe. MS-IAM refuse
  (422 `IAM_SBX_VALIDATION_RESERVEE`, `details.permissions`) la création d'un profil externe qui la porte, la
  recomposition d'un profil externe qui l'ajoute, et le passage en externe d'un profil qui la porte
  (`ReglesSandbox`). Le générateur du domaine le vérifie aussi pour les valeurs de départ.
- **Second facteur pour ADMINISTRATION et VALIDATION** : toute route de la plateforme exige déjà `acr = 2`.
- **Sans redéploiement** : une permission ajoutée à un profil prend effet à la connexion suivante (jeton Keycloak),
  la modification est journalisée avec son auteur (journal CONFIGURATION de MS-IAM).
- **Liste filtrée** : `GET /api/v1/configuration/permissions?prefixe=SBX` (IAM_ACCOUNT_READ) rend les soixante
  permissions de la sandbox.
- **Écran** : le catalogue des droits (D-90) porte un module « Sandbox GIMAC » avec une fonctionnalité par module et un
  droit par fonction ; l'écran « Matrice » et l'écran des profils de la console les affichent sans code propre.

### 2.4 Connexion (US-EXI-01)

| Critère | Réalisation |
|---|---|
| Utilisateur interne fédéré, jeton portant ses rôles de la sandbox | Les permissions SBX sont des rôles du domaine, composés dans les profils : le jeton les porte |
| Correspondant d'une institution, jeton portant `institution_id` et ses seuls rôles de portée institution | Attribut `institution_id` des comptes d'institution, profils `ROLE_SBX_*` externes |
| Aucune inscription libre | `registrationAllowed: false` dans le domaine |
| Second facteur exigé | `acr = 2` sur toute route `/api/v1/**` (D-61) |

## 3. Suite de l'épopée

Ordre de réalisation, composant par composant, serveur et écrans ensemble.

| Ordre | Composant | Récits |
|---|---|---|
| 1 | MS-IAM et Keycloak | EXI-01, 02, 03, 04, 05, 06, 23, 24 (faits ; réexamen des accès d'EXI-23 au §8.5) |
| 2 | Passerelle et OPA | EXI-07 |
| 3 | MS-PARTICIPANT | EXI-20, 21 |
| 4 | MS-DOCUMENT, MS-NOTIFICATION, MS-AUDIT | EXI-08, 09, 10 (faits) |
| 5 | Supervision de la sandbox (D-121) | EXI-11, 12, 13, 18, 19, 22 écartés : aucune adaptation de MS-MONITORING, MS-INCIDENT ni MS-REPORTING ; écran « Supervision de la sandbox » de l'espace sandbox du DES, sur SBX-TRACE, permission dédiée |
| 6 | MS-HOMOLOGATION et MS-CERTIFICATION | EXI-14, 15, 16 (faits), 25 (en attente d'une décision, §12) |
| 7 | Écrans du DES | EXI-17 |

## 4. Administration déléguée (US-EXI-03)

L'administrateur de l'institution (`SBX_HAB_ADMINISTRATION`, profil `ROLE_SBX_ADMIN_INSTITUTION`) gère les comptes de
ses collaborateurs par les écritures existantes de MS-IAM, dans les seules bornes de son institution (`institution_id`
du jeton).

| Méthode | Route MS-IAM (`/api/v1/administration-deleguee`) | Portail (`/bff/portail/sbx-habilitations`) | Objet |
|---|---|---|---|
| GET | `/roles` | `/roles` | Rôles délégables |
| GET | `/comptes?page=&taille=` | `/comptes` | Comptes de l'institution, suspendus compris, 100 par page au plus |
| GET | `/comptes/{id}` | `/comptes/{id}` | Un compte |
| POST | `/comptes` | `/comptes` | Création `{ identifiant, prenom, nom, email, telephone?, fonction?, roles }`, invitation par courriel ; 201, ou 200 pour une étape rejouée |
| PUT | `/comptes/{id}/roles` | `/comptes/{id}/roles` | `{ roles, motif }` : rôles de la sandbox remplacés, autres profils gardés |
| PUT | `/comptes/{id}/etat` | `/comptes/{id}/etat` | `{ actif, motif }` : suspension ou rétablissement, sans suppression |

Règles :

- **Rôles délégables** : les profils externes dont la composition porte une permission de la sandbox (aucun nom de
  profil, D-94). Autre profil : 403 `IAM_SBX_ROLE_HORS_PORTEE` ; création sans rôle : 400 `IAM_SBX_ROLE_REQUIS`.
- **Périmètre** : un compte d'une autre institution reçoit la même réponse qu'un compte inexistant (404
  `IAM_ACCOUNT_NOT_FOUND`) ; jeton sans institution : 403 `IAM_SBX_SANS_INSTITUTION`.
- **Jamais sur soi-même** : 403 `IAM_SELF_ADMINISTRATION`, tentative journalisée (`TENTATIVE_AUTO_ATTRIBUTION`,
  `TENTATIVE_AUTO_SUSPENSION`, `TENTATIVE_AUTO_RETABLISSEMENT`).
- **RG-15, dernier administrateur** : retirer le rôle d'administration ou suspendre le dernier administrateur actif de
  l'institution : 409 `IAM_SBX_DERNIER_ADMINISTRATEUR`.
- **Étapes de saga** : en-tête `Idempotency-Key` ; une étape déjà appliquée rend son résultat d'origine sans rien refaire
  (table `etape_idempotente`).
- **Journal et événements** : chaque acte passe par l'administration des comptes de MS-IAM (journal d'administration,
  `iam.account-administered.v1`, équivalent sur la plateforme de `iam.user-changed.v1` du carnet).

## 5. État des récits

| Récit | État | Démonstration de bout en bout à refaire |
|---|---|---|
| US-EXI-01 | Fait (D-115) | Connexion à la console et au portail de la sandbox, quand leurs écrans seront servis |
| US-EXI-02 | Fait (D-115) | Saga MATRICE de SBX-ESPACE, à son arrivée |
| US-EXI-03 | Fait (D-115) | Sagas de SBX-ESPACE et écran des collaborateurs du portail de la sandbox |
| US-EXI-07 | Fait (D-120, §6) | Appel réel de chaque service de la sandbox par sa route ; mandat de partenaire avec le jeton réduit actif |
| US-EXI-16 | Fait (D-122, §7) | Consommation par SBX-CAMPAGNE et SBX-LECTURE à leur arrivée (projection de référence en test de contrat) |
| US-EXI-20 | Fait (D-120, §6.5) | Événement périmé écarté par SBX-ESPACE, à son raccordement au bus |
| US-EXI-21 | Fait (D-120, §6.6) | Ouverture d’un espace par SBX-ESPACE sur le registre réel, comptes de service sbx-* dans le domaine Keycloak |
| US-EXI-04 | Fait (D-119, §8.3) | Saga MANDAT de SBX-ESPACE ; jeton délégué présenté par la passerelle à l'introspection de MS-IAM |
| US-EXI-06 | Fait (D-119, §8.2) | Sagas SUSPENSION et RETABLISSEMENT de SBX-ESPACE ; refus immédiat sur le jeton réduit quand la passerelle interrogera l'introspection de MS-IAM |
| US-EXI-14 | Fait (D-122, §7.4) | Relève de SBX-LECTURE réel (adaptateur `real`), reprise d'une campagne complète de 99001 |
| US-EXI-15 | Fait (D-122, §7.5) | Publication de `sbx-lecture.campaign-published.v1` par SBX-LECTURE sur le sujet réservé |
| US-EXI-17 | Fait (D-122, §7.6) | Onglet sur les résultats d'une campagne close reprise de SBX-LECTURE réel |
| US-EXI-24 | Fait (D-123, §8.4) | Introspection par la passerelle (jeton réduit) et par SBX-TRACE pour le composant appelé |
| US-EXI-05 | Fait (D-123, §9) | Jetons réduits entre services de la sandbox ; rattachement au certificat client au déploiement |
| US-EXI-23 | Fait (D-124, §8.5) | Campagne trimestrielle de réexamen réelle depuis la console ; raccordement OPA des nouvelles écritures BFF par le lot passerelle |
| US-EXI-10 | Fait (D-124, §10) | Publication des événements à effet par les onze services, enveloppes conformes au §10 |
| US-EXI-09 | Fait (D-124, §11) | Demandes `sbx-*.notification-requested.v1` des onze services ; canal courriel (§11, écart) |
| US-EXI-25 | Fait (D-125, §12) | Socle d'évaluation et caducité dans le circuit `CertificationCircuit`/`Quitus` existant ; événements historiques acceptés |

## 6. Passerelle, registre et services transverses (lot D-120)

Routes de la sandbox dans MS-GATEWAY (US-EXI-07) aux §6.1 à §6.4, registre de MS-PARTICIPANT (US-EXI-20, US-EXI-21) ensuite.

### 6.1 Routes de la sandbox dans MS-GATEWAY (US-EXI-07)

Onze services de la sandbox, chacun dans les couches console et portail, plus une seule route du département. Chaque
route commence par `OpaAuthorize`, puis `CloisonnementSandbox`, corps de 1 Mo au plus.

| Chemin reçu | Service | Chemin relayé |
|---|---|---|
| `/bff/console/sbx-<service>/(.*)` | `sbx-<service>` | `/api/v1/console/$1` |
| `/bff/portail/sbx-<service>/(.*)` | `sbx-<service>` | `/api/v1/portail/$1` |
| `/bff/des/sbx-espace/(.*)` | `sbx-espace` | `/api/v1/console/$1` (D-118 : l'application du DES emploie l'API console) |

Services et adresses (`gimac.services.sbx-*`, `localhost` en développement, nom du service en production) : espace
8101, raccordement 8102, referentiel 8103, donnees 8104, simulation 8105, mobile 8106, iso20022 8107, carte 8108,
campagne 8109, trace 8110, lecture 8111. Aucune autre adresse : une route de la sandbox ne peut viser qu'une propriété
`gimac.services.sbx-*` (GW-21, `RoutesSandboxTest`).

Écart au CP (critère 3 de US-EXI-07, « aucune route de conduite dans l'application du département ») : D-118 ouvre
`/bff/des/sbx-espace/**` pour l'administration des espaces. La revue de configuration (`RoutesSandboxTest`, règle OPA
`sbx_chemin`) refuse toute autre route `/bff/des/sbx-*`.

### 6.2 Autorisation (OPA, bloc D-120 de `lot1_authz.rego`)

| Service | Module | Lecture (GET, HEAD, OPTIONS) | Écriture (POST, PUT, PATCH, DELETE) |
|---|---|---|---|
| sbx-espace | HAB | `SBX_HAB_CONSULTATION` | `SBX_HAB_ADMINISTRATION` |
| sbx-raccordement, referentiel, donnees, simulation, mobile, iso20022, carte, campagne, trace, lecture | RAC, REF, DON, SIM, MOB, ISO, CAR, CER, TRA, LEC | `SBX_<MODULE>_CONSULTATION` | l'une de `SBX_<MODULE>_ADMINISTRATION`, `_SIMULATION`, `_EXECUTION`, `_VALIDATION` ; le service vérifie la fonction exacte du geste |

- Portail : jeton portant `institution_id`. Console et département : comptes du GIMAC seulement (sans institution).
- Second facteur (`acr` 2 ou 3), session restreinte refusée, chemins `/interne` jamais relayés.
- La règle de repli du département (chemin non listé, compte authentifié) n'ouvre jamais `/bff/des/sbx-*`.
- La mention `SBX_ESP_*` de D-118 est une coquille pour `SBX_HAB_*` (module des habilitations) ; le lot homologation
  la corrige au REC.

### 6.3 Cloisonnement étendu aux mandats

Le filtre `CloisonnementSandbox` (après OPA et après le relais du jeton de session) décide au nom de quelle institution
l'appel est relayé :

| Situation | Décision | Statut |
|---|---|---|
| Compte du GIMAC (aucune institution) | admis, aucune institution (`COMPTE_GIMAC`) | relayé |
| Compte d'institution sans en-tête, ou en-tête égal à son institution | institution du jeton (`INSTITUTION_PROPRE`) | relayé |
| En-tête `X-Institution-Mandante: 99002`, mandat actif (fin postérieure à l'instant) | institution mandante (`MANDAT_ACTIF`) | relayé |
| Mandat absent ou échu | refus (`MANDAT_ABSENT`, `MANDAT_ECHU`) | 404 `GW_SBX_INTROUVABLE` |
| Mandat actif mais aucun jeton réduit ne peut être émis | refus par défaut (`JETON_REDUIT_INDISPONIBLE`) | 404 `GW_SBX_INTROUVABLE` |
| Quota dépassé | refus (`QUOTA_DEPASSE`) | 429 `GW_SBX_QUOTA_DEPASSE`, `Retry-After: 60` |

- **Mandats** : la passerelle tient une projection des périmètres de SBX-ESPACE (table `sbx_perimetre` de sa base,
  consommateur du sujet `sbx-espace.scope-changed.v1`, groupe `gateway-sbx-perimetres`). Champs lus dans `data` :
  `idCompte`, `institutionPropre`, `mandantes` (`{code: fin ISO 8601}`), `version`. Un instantané de version inférieure
  ou égale est sans effet ; une enveloppe non conforme est écartée.
- **Jeton interne réduit** (RFC 8693) : quand l'émetteur de jetons internes de la passerelle est actif
  (`gimac.securite.jeton-fantome.actif`), l'en-tête `Authorization` relayé est remplacé par un jeton RS256 de cinq
  minutes, issuer `gimac-gateway`, lié au certificat mTLS quand il existe (`cnf`, RFC 8705). Revendications : `sub`,
  `institution_id` (institution effective), `institution_propre` (si mandat), `roles` (permissions `SBX_*` seulement),
  `acr`, `aud` = `sbx-<service>`, `jti`. Les services de la sandbox valident ce jeton par `/api/internal/jwks` de la
  passerelle. Émetteur inactif (poste de développement) : le jeton de session est relayé tel quel et un mandat est
  refusé (aucune institution mandante ne peut être portée).
- **Activation** : `gimac.securite.jeton-fantome.actif` (variable `GIMAC_SECURITE_JETONFANTOME_ACTIF`) reste fausse par
  défaut dans le dépôt. Elle est mise à vrai dans le `.env` de la recette locale seulement ; elle active aussi la clé
  RS256 de la passerelle (`RsaKeyConfig`) et son JWKS. Sans elle, les mandats de partenaire sont refusés.
- L'en-tête `X-Institution-Mandante` n'est jamais relayé au service.
- **Quotas** (`gimac.sandbox.quotas`, requêtes par minute) : portail 120, console 300, département 120, par couche,
  titulaire (institution effective, ou compte du GIMAC) et service ; seau Valkey partagé si la limitation de débit est
  active, mémoire sinon.
- **Journal** : chaque décision publie `gateway.sandbox-decision.v1` sur `gateway.security.events` (scellé par
  MS-AUDIT) quand le journal des décisions est actif, et toujours au journal applicatif `gimac.audit.sandbox`. Détails :
  `source=sandbox`, couche, service, méthode, chemin, institutions du jeton, demandée et effective, décision, motif ;
  jamais de jeton.
- Problèmes RFC 9457 : titre en fr, en ou es selon `Accept-Language`, sans révéler la ressource ni le mandat.

### 6.4 Tests

| Critère | Test |
|---|---|
| 1. Route du portail cloisonnée, décision journalisée | `CloisonnementSandboxTest.routePortailCloisonnee`, `CloisonnementSandboxGatewayFilterFactoryTest.routePortailCloisonnee`, `AdaptateursSandboxTest.journal`, OPA `test_sbx_fonctions_ecriture` |
| 2. Partenaire mandaté | `CloisonnementSandboxTest.partenaireMandate`, `CloisonnementSandboxGatewayFilterFactoryTest.partenaireMandate`, `AdaptateursSandboxTest.jetonReduit` |
| 3. Aucune route de conduite dans le département | `RoutesSandboxTest.routeDeConduiteDansLeDepartement`, OPA `test_sbx_aucune_conduite_dans_le_departement` |
| 4. Institution hors mandat | `CloisonnementSandboxTest.institutionHorsMandat`, `CloisonnementSandboxGatewayFilterFactoryTest.institutionHorsMandat`, `ProjectionPerimetresR2dbcIT.refusMandatEchu` |
| Sécurité : matrice des routes | `RoutesSandboxTest.matriceDesRoutes`, OPA `test_sbx_matrice_portail_console` |
| GW-21 | `RoutesSandboxTest.aucuneRouteVersLaProduction` |

### 6.5 Version de statut dans participant.status-changed.v1 (US-EXI-20)

- **Champ ajouté par extension** : `versionStatutRegistre` (entier, 1 au premier changement de statut publié), en
  dernier champ des données de `participant.status-changed.v1`. Les champs existants sont inchangés ; les lecteurs
  (MS-HOMOLOGATION, HOM-06) le tiennent pour optionnel : les événements publiés avant D-120 ne le portent pas.
- **Incrément strict par institution** dans la transaction du changement de statut (approbation d'une transition,
  transition automatique de la liaison, reprise) : table `institution_version_statut` (une ligne par institution),
  ordre unique `insert … on conflict … do update set version = version + 1 returning version`, ligne verrouillée
  jusqu'à la validation. Une transaction annulée ne consomme aucune version. La version n'est jamais recalculée à la
  redélivrance : l'outbox renvoie la charge déjà écrite.
- **Contrat lisible par machine** : `ms-participant/src/main/resources/config/contrats/participant.status-changed.v1.schema.json`
  (JSON Schema 2020-12).
- **Clé de partition** : l'identifiant de l'institution (clé de l'outbox, sujet `participant.status`). Écart au CP, qui
  demande le code participant : la clé est en bijection avec lui, mais une candidate n'a pas encore de code, et
  changer de clé désordonnerait les événements en vol d'une même institution. L'ordre par institution est garanti.
- **Lecteurs (SBX-ESPACE, SBX-RACCORDEMENT)** : appliquent un événement si sa version est supérieure à celle qu'ils ont
  retenue, l'acquittent sans effet sinon (critères 2 et 3 du récit, réalisés dans SBX-ESPACE :
  `EspaceInstitution`, `version <= versionStatutRegistre`).

| Critère | Test |
|---|---|
| 1. Suspension ordonnée (version 6 vers 7) | `VersionStatutRegistreTest.suspensionOrdonnee` |
| Contrat : présence et monotonie | `VersionStatutRegistreTest.monotonie`, `VersionStatutRegistreTest.contratLisibleParMachine`, `VersionsStatutRegistreJdbcIT.incrementStrict` |
| 2 et 3. Republication et événement périmé | côté SBX-ESPACE (lecteur) ; la plateforme garantit la version publiée |

### 6.6 Fiche et nomenclature des services de MS-PARTICIPANT (US-EXI-21)

Interface interne de MS-PARTICIPANT (jamais relayée par la passerelle), jeton de service portant
`INTERNAL_PARTICIPANT_READ`, donné par le domaine Keycloak aux comptes de service `sbx-espace`, `sbx-raccordement` et
`sbx-referentiel` (lot IAM). Consultation seule.

| Méthode | Route | Réponse |
|---|---|---|
| GET | `/api/v1/interne/institutions/code/{code}/fiche` | `{ codeParticipant, raisonSociale, statut, versionStatut, correspondants: [{ id, compteId, nom, fonction }], servicesSouscrits: [code] }` ; 404 `PAR_INSTITUTION_NOT_FOUND` si le code est absent |
| GET | `/api/v1/interne/institutions/{id}/correspondance` | `{ institutionId, codeParticipant }` (D-134, lu par MS-IAM) ; 404 `PAR_INSTITUTION_NOT_FOUND` |
| GET | `/api/v1/interne/institutions/code/{code}/correspondance` | `{ institutionId, codeParticipant }` (D-134, sens inverse) ; 404 `PAR_INSTITUTION_NOT_FOUND` |
| GET | `/api/v1/interne/institutions?page=0&taille=50&statut=ACTIF&statut=ADHERENT` | `{ elements: [{ codeParticipant, statut }], total, page, taille }` (D-140, lu par SBX-DONNEES) : institutions dotées d'un code participant, triées par code ; `statut` facultatif et répétable, valeur hors `InstitutionStatus` : 422 `PAR_STATUT_INVALIDE` ; taille de 1 à 100, sinon 422 `PAR_PAGINATION_INVALIDE` |
| GET | `/api/v1/interne/nomenclature/services?page=0&taille=50` | `{ contenu: [{ code, institutions }], total, page, taille }`, codes des abonnements actifs triés ; taille de 1 à 100, sinon 422 `PAR_PAGINATION_INVALIDE` |

- `versionStatut` : version de statut courante (§6.5), 0 tant qu'aucun changement n'a été publié.
- `servicesSouscrits` (ajout du 05/11/2026) : codes des abonnements actifs de l'institution, triés, lus par
  SBX-RACCORDEMENT (« services souscrits lus sur la fiche », CC-SBX-GIMAC-02 algorithme 6.7). La nomenclature ne
  donne, par code, que le nombre d'institutions abonnées (`institutions`, entier). Charge d'exemple partagée avec la
  sandbox : `ms-participant/src/test/resources/contrats/fiche-sandbox.exemple.json`.
- `correspondants` : correspondants actifs seulement ; `compteId` est le compte de la plateforme (MS-IAM) quand il
  existe. Aucun courriel ni téléphone.
- **Port de SBX-ESPACE** (`RegistreParticipantPort`, lecture seule) : `lireFiche(code)` vers la fiche
  (`FicheParticipant(codeParticipant, raisonSociale, statut, versionStatut)`, vide sur 404) ; `estCorrespondant(code,
  id)` vrai si `id` vaut l'`id` ou le `compteId` d'un correspondant de la fiche.
- **Écriture refusée** : un compte de service n'a ni second facteur ni permission métier ; toute route d'écriture lui
  rend 403 sans effet. Chaque refus d'un jeton dont le client (`azp`, sinon `client_id`) commence par `sbx-` est
  journalisé (`participant.sandbox-write-denied`, journal `gimac.audit.sandbox`, méthode et chemin).
- **Registre indisponible** (critère 4) : SBX-ESPACE refuse l'ouverture avec un motif explicite (délai, disjoncteur) ;
  la plateforme n'a rien à servir dans ce cas.

| Critère | Test |
|---|---|
| 1. Lecture de la fiche | `RegistreSandboxQueriesTest.lectureDeLaFiche`, `RegistreSandboxIT.lectureDeLaFiche` |
| 2. Nomenclature des services | `RegistreSandboxQueriesTest.nomenclatureDesServices`, `RegistreSandboxIT.nomenclatureDesServices` |
| 3. Tentative d'écriture | `RegistreSandboxIT.tentativeEcriture`, `JournalRefusSandboxTest.tentativeEcriture` |
| 4. Registre indisponible | côté SBX-ESPACE (`OuvertureEspace`) |

### 6.7 Types de pièces et rétention de la sandbox dans MS-DOCUMENT (US-EXI-08)

**Catalogue** (table `sbx_type_piece`, DOC-03 ; le code du type est aussi la finalité du dépôt dans la chaîne existante) :

| Type | Rétention | Probant | Déposants | Numéro de carte en clair |
|---|---|---|---|---|
| `SBX_BILAN_CERTIFICATION` | P10Y | oui | sbx-campagne | quarantaine |
| `SBX_RAPPORT_ECARTS_ISO20022` | P10Y | oui | sbx-iso20022 | quarantaine |
| `SBX_TRACE_EXPORTEE` | P10Y | oui | sbx-trace | quarantaine |
| `SBX_FICHIER_REMIS_PARTICIPANT` | P5Y | non | sbx-trace, sbx-campagne | refus |

- **Rétention** : période ISO 8601, modifiable depuis la console avec motif et version lue ; dix ans au moins pour un
  type probant (422 `DOC_SBX_RETENTION_INSUFFISANTE`). Elle est inscrite sur la pièce (`conserveeJusquAu`). Le
  verrouillage du dépôt inaltérable reste celui du compartiment : **réglage de déploiement** (rétention par défaut du
  compartiment WORM d'au moins dix ans, la plus longue des rétentions de type), pas un changement du code.
- **Dépôt** : `POST /api/v1/interne/pieces-sandbox` (jeton de service `INTERNAL_DOCUMENT_DEPOSIT`, comptes sbx-campagne,
  sbx-trace, sbx-iso20022), multipart `fichier`, `type`, `empreinte` (SHA-256 hexadécimal), `codeParticipant`,
  `reference`. Ordre des contrôles :
  1. type actif (400 `DOC_SBX_TYPE_INCONNU`) et déposant admis pour ce type (403 `DOC_SBX_DEPOSANT_NON_AUTORISE`) ;
  2. numéro de carte en clair : quarantaine (202, `etat: QUARANTAINE`, ligne inscrite sans contenu, jamais dans le
     dépôt inaltérable) ou refus (422 `DOC_PAN_DETECTED`) selon la règle du type ; alerte `ALERTE_EXPLOITATION` ;
  3. empreinte calculée contre empreinte annoncée : divergence 400 `DOC_SBX_EMPREINTE_DIVERGENTE` (`details.annoncee`,
     `details.calculee`), rien n'est conservé, le service déposant réessaie ; alerte `ALERTE_EXPLOITATION` ;
  4. déduplication par (institution, type, empreinte) : 200, identifiant de la pièce existante, `dejaConservee: true`,
     aucun second dépôt ni nouvel événement ;
  5. dépôt par la chaîne existante (`depot_document`, WORM), pièce `SCELLEE`, `document.document-sealed.v1` dans
     l'outbox, dans la même transaction ; 201 `{ id, etat, sha256, conserveeJusquAu, dejaConservee }`.
- **Événement** `document.document-sealed.v1` (sujet `document.document`, CloudEvents, clé = identifiant du
  document) : `documentId`, `typePiece`, `codeParticipant`, `reference`, `sha256`, `taille`, `nomFichier`,
  `serviceDeposant`, `scelleeLe`, `conserveeJusquAu`. Outbox `sbx_evenement_sortant`, relais toutes les secondes
  (`gimac.document.sandbox.relais.*`), rebut après dix échecs.
- **Pièce déjà conservée** (pour MS-HOMOLOGATION) : `GET /api/v1/interne/documents/empreintes/{sha256}`
  (`INTERNAL_DOCUMENT_READ`) rend `{ id, nomFichier, typeMime, sha256 }` du plus ancien document de cette empreinte ;
  404 si aucun ; 422 `DOC_SHA256_INVALIDE` si l'empreinte n'est pas en hexadécimal minuscule de 64 caractères (index
  `ix_depot_document_sha256_taille`).
- **Console** (route `console-document-sandbox`, `/bff/console/document/sandbox/**` vers `/api/v1/sandbox/**`, comptes
  du GIMAC) : `GET types-pieces` et `GET quarantaines?page=&taille=` (`SBX_TRA_CONSULTATION`),
  `PUT types-pieces/{code}/retention` `{ retention, motif, version }` (`SBX_TRA_ADMINISTRATION`, 409
  `DOC_SBX_VERSION_PERIMEE`).

| Critère | Test |
|---|---|
| 1. Dépôt du bilan figé | `DepotPieceSandboxServiceTest.depotDuBilanFige`, `PiecesSandboxIT.depotDuBilanFige` (réception de l'événement sur Kafka) |
| 2. Pièce déjà conservée | `DepotPieceSandboxServiceTest.pieceDejaConservee`, `PiecesSandboxIT.dedoublonnageDivergenceQuarantaine` |
| 3. Empreinte divergente | `DepotPieceSandboxServiceTest.empreinteDivergente`, `PiecesSandboxIT.dedoublonnageDivergenceQuarantaine` |
| 4. Numéro de carte en clair | `DepotPieceSandboxServiceTest.numeroDeCarteEnClair`, `PiecesSandboxIT.dedoublonnageDivergenceQuarantaine` |

## 7. MS-HOMOLOGATION et MS-CERTIFICATION (lot homologation, D-122)

### 7.1 États des liaisons publiés pour la sandbox (US-EXI-16)

| Événement (sujet `homologation.liaison`, extension `institutionid`) | Quand | Données ajoutées pour la sandbox |
|---|---|---|
| `homologation.liaison-opened.v1` | Ouverture au cadrage, reprise de l'existant (D-107), service supplémentaire (rang 35) | `codeParticipant`, `versionEtat` = 1 |
| `homologation.liaison-status-changed.v1` (nom P14 retenu) | Ouverture, puis chaque changement du statut vu par la sandbox | `liaisonId`, `institutionId`, `codeParticipant`, `typeProjet`, `statut` (`OUVERTE`, `GELEE`, `CLOSE`), `statutPrecedent`, `etatLiaison`, `versionEtat`, `survenuLe` |

- **Statut vu par la sandbox** : tous les états actifs de la liaison (ouverte, en homologation, apte, mise en service autorisée, certifiée) valent `OUVERTE` ; `GELEE` et `CLOSE` interdisent l'ouverture d'une campagne.
- **Version d'état** : strictement croissante par liaison (table `liaison_sandbox`, ligne lue sous verrou) ; un consommateur acquitte sans effet toute version inférieure ou égale à celle qu'il connaît, sans appel en retour.
- **Détection** : toute liaison chargée pour une écriture est observée ; avant la validation de la transaction, le statut est comparé au dernier publié (`PublicationEtatsLiaison`). Une liaison ouverte avant D-122 reçoit sa version 1 à sa première écriture.
- **Code participant** : celui de l'interface interne de MS-PARTICIPANT ; registre injoignable : l'événement part sans code, le code est complété à la publication suivante.
- **Schémas** : `ms-homologation/src/main/resources/contrats/sandbox/*.schema.json` (JSON Schema 2020-12).
- **Aucune écriture de la sandbox** : aucune route d'écriture de MS-HOMOLOGATION n'admet de permission `SBX_*` (test de revue).
- **Droits de lecture du sujet** `homologation.liaison` pour `sbx-campagne` et `sbx-lecture` : le bus de la plateforme n'a pas encore d'ACL Kafka par compte (réglage du déploiement, avec l'authentification SASL des comptes de service du §9). Le gel sur changement de statut de l'institution (HOM-06) est réalisé au §7.3.

### 7.2 Espace sandbox du DES : « Espaces des institutions » (D-118 points 2 et 3)

- **Paquet** `@gimac/sandbox-admin`, monté sous `/des/sandbox/*` dans l'application du DES. Registre d'entrées
  (`ENTREES_SANDBOX`) : chaque écran a son fichier, sa fonctionnalité au catalogue de MS-IAM (groupe `sandbox`), sa
  permission et ses routes ; une entrée de plus (« Supervision de la sandbox », D-121) ne touche à aucune autre.
- **Entrée « Espaces des institutions »** (`sandboxEspaces`, `/des/sandbox/espaces`, `/des/sandbox/espaces/:code`) :
  fonctionnalité réservée aux comptes internes (`PIL_ACTIVITY_OWN` et `SBX_HAB_CONSULTATION`) ; actes pour
  `SBX_HAB_ADMINISTRATION`. La mention `SBX_ESP_*` de D-118 est une coquille : les permissions sont celles du module HAB.
- **Contrat de SBX-ESPACE** (API console, branche `sbx-espace-hexagone`, servie par `/bff/des/sbx-espace/**`, §6.1) :

| Méthode | Route | Effet | Refus |
|---|---|---|---|
| GET | `/espaces?apres=&taille=` | Page `{elements, suivant}` par curseur (code participant), 1 à 100 | 404 `SBX_ESP_INTROUVABLE` hors droit |
| GET | `/espaces/{code}` | `EspaceVM` | 404 |
| POST | `/espaces` `{codeParticipant, correspondantId}` | 201 saga `TERMINEE`, 202 en reprise ou compensation | 404 institution absente ; 409 `SBX_ESP_STATUT_NON_ADMIS`, `SBX_ESP_ESPACE_EXISTANT` ; 422 `SBX_ESP_CORRESPONDANT_INCONNU` |
| POST | `/espaces/{code}/suspension` | Motif `DECISION_GIMAC`, état `SUSPENDU` | 409 `SBX_ESP_ETAT_INCOMPATIBLE` |
| POST | `/espaces/{code}/retablissement` | État `OUVERT`, motifs effacés | 409 `SBX_ESP_ETAT_INCOMPATIBLE`, `SBX_ESP_REGISTRE_OPPOSE` |
| POST | `/espaces/{code}/fermeture` | État `FERME` (institution retirée) | 409 `SBX_ESP_ETAT_INCOMPATIBLE`, `SBX_ESP_NON_RETIRE` |

  Toute écriture : en-tête `Idempotency-Key` (400 `SBX_ESP_CLE_IDEMPOTENCE_ABSENTE`, 422 `…_INVALIDE`), tiré à
  l'ouverture du dialogue ; second facteur (422 `SBX_ESP_SECOND_FACTEUR_EXIGE`) ; problèmes RFC 9457 traduits en fr,
  en, es par leur code.
- **Fiche réutilisable** `EspaceInstitution` : servie aussi par l'onglet « Sandbox » du dossier de liaison, avec
  l'ouverture de l'espace proposée au code de l'institution quand il n'existe pas.
- **Écarts relevés** : (1) les statuts admis de SBX-ESPACE (`CANDIDAT`, `EN_HOMOLOGATION`, `ACTIF`, `A_REGULARISER`)
  excluent `ADHERENT`, alors qu'un adhérent précède l'homologation : à confirmer pour SBX-ESPACE ; (2) aucune route ne
  liste les correspondants d'une institution : le correspondant est saisi par l'identifiant de son compte.
- **Comptes de démonstration** : `super.admin` (lecture et actes), chef du DES et RSSI (lecture). Simulation au contrat
  exact : institutions fictives 99001 à 99005.

### 7.3 Gel des liaisons (HOM-06) et republication des états

- **Gel** : MS-HOMOLOGATION consomme `participant.status-changed.v1` (sujet `participant.status`, groupe
  `homologation-statut-institution`, idempotent par identifiant d'événement). Une institution `SUSPENDU` ou `RETIRE` fait
  passer ses liaisons actives (hors `CLOSE`) à `GELEE` : état d'avant le gel retenu dans `etat_avant_gel`, motif dans
  `motif_etat`. Au retour à un autre statut, chaque liaison gelée retrouve l'état qu'elle avait. Chaque changement part
  par `homologation.liaison-status-changed.v1` avec une version d'état supérieure (§7.1).
- **Ordre** : par `versionStatutRegistre` (D-120, US-EXI-20) quand l'événement et le dernier appliqué en portent une,
  sinon par `dateEffet` (strictement postérieure) ; un événement sans version ni date comparable prévaut. Dernier statut
  appliqué par institution : table `institution_statut_applique`. Un événement périmé est acquitté sans effet.
- **Republication** : `POST /api/v1/interne/sandbox/liaisons/republication` (jeton de service portant
  `INTERNAL_HOMOLOGATION_REPUBLISH`, route interne jamais relayée par la passerelle, test OPA du bloc D-122) republie
  l'état courant de chaque liaison avec sa version d'état (idempotente : les lecteurs à jour l'ignorent) ; une liaison
  jamais publiée, ou dont le statut a changé sans publication, reçoit sa version suivante. Réponse
  `{liaisons, republiees, nouvelles, le}` ; appel journalisé avec son demandeur. À jouer à la mise en service de
  SBX-CAMPAGNE et de SBX-LECTURE.
- **Compte de service d'exploitation** : `exploitation-service`, seul porteur de `INTERNAL_HOMOLOGATION_REPUBLISH` (§9).

### 7.4 Lecture du contrat de SBX-LECTURE (US-EXI-14)

Client du contrat de lecture v1 (livrable L04, CC-SBX-GIMAC-02 annexe D) : port `LectureSandboxPort`, consultation seule
(GET et HEAD), sans aucune méthode d'écriture. Il remplace, pour la sandbox, l'adaptateur de lecture du simulateur
existant (HOM-13).

| Chemin de SBX-LECTURE | Objet | Schéma (`ms-homologation/src/main/resources/contrats/sandbox/`) |
|---|---|---|
| `GET /v1/liaisons/{id}/campagnes` | Campagnes de la liaison ; une campagne en collision de code participant est remplacée par un signalement | `sbx-lecture.v1.campagnes.schema.json` |
| `GET /v1/campagnes/{c}` | Détail : liaison, participant, interface, services, état, taux, version du bilan, version de la table | `sbx-lecture.v1.campagne.schema.json` |
| `GET /v1/campagnes/{c}/executions?depuis=&taille=` | Enregistrements de position strictement supérieure au jeton `depuis`, au plus `taille` ; jeton suivant, page close si pleine | `sbx-lecture.v1.executions.schema.json` |
| `GET /v1/campagnes/{c}/pieces` | Pièces (bilan, résultats détaillés, fichiers remis) avec empreinte SHA-256 et taille annoncées | `sbx-lecture.v1.pieces.schema.json` |
| `HEAD` puis `GET /v1/campagnes/{c}/pieces/{nature}/{version}` | Empreinte annoncée (`Repr-Digest`), puis contenu | |

- **Authentification** : jeton du compte de service de la plateforme (`homologation-service`, permission
  `INTERNAL_SBX_LECTURE_READ`, §9), par la passerelle interne de service, sur liaison mutuellement authentifiée ; le
  rattachement du jeton au certificat client (RFC 8705) est un réglage du déploiement.
- **Relève** (`ReleveSandbox`, périodique toutes les cinq minutes, `gimac.sandbox.releve.periode-ms`) : pour chaque
  liaison non close, les campagnes, le détail et les taux, puis les exécutions depuis la dernière position retenue. Les
  appels à la sandbox sont faits hors transaction ; chaque page est enregistrée avec la position suivante dans une
  même transaction : une indisponibilité laisse la position à la dernière page enregistrée, sans perte ni doublon.
  Pour une exécution, l'enregistrement de plus haute position remplace le précédent (création PENDING, validation,
  rectification). `homologation.campaign-result-imported.v1` (sujet `homologation.campaign`) est publié par l'outbox
  quand une relève apporte un enregistrement ou une pièce.
- **Indisponibilité** : 503 et 429 (avec `Retry-After`), comme un service injoignable, ne donnent aucune donnée ; la
  relève de la liaison est reportée pendant le délai indicatif (cinq minutes à défaut), puis reprend de la position
  retenue. Une réponse hors contrat (403, 404 d'une campagne hors périmètre, forme invalide) écarte la page entière.
- **Pièces** : empreinte de la liste, empreinte du HEAD et empreinte des octets téléchargés doivent concorder
  (`EMPREINTE_DIVERGENTE` sinon : rien n'est déposé) ; une pièce de même empreinte déjà conservée (bilan déposé par
  SBX-CAMPAGNE à la clôture, US-EXI-08, lu par `GET /api/v1/interne/documents/empreintes/{sha256}` de MS-DOCUMENT, ou
  relève antérieure) n'est jamais redéposée (`DEJA_CONSERVEE`) ; sinon dépôt par la chaîne existante, finalité
  `SANDBOX_CAMPAGNE` (`DEPOSEE`).
- **Rapprochement** : un scénario présent dans le référentiel d'opérations de la version de la liaison donne le code
  de l'opération ; un scénario inconnu isole l'exécution parmi les scénarios non rapprochés (`unmatched_scenario`,
  `A_QUALIFIER`). Aucune écriture n'est adressée à la sandbox.
- **Aptitude** : pour une campagne `TERMINEE`, le taux de réussite restitué par service devient la mesure de
  certification du service (table `coverage`, nature `CERTIFICATION`), sans recalcul ; une mesure plus ancienne ne
  remplace jamais une plus récente. Le parcours ne certifie alors un service mesuré qu'à 100 %.
- **Tables** (changeset `20261022000300_exi14_releve_sandbox`) : `sbx_releve_liaison` (dernière réussite, dernier
  échec, reprise après), `sbx_campagne` (en-tête, taux, position retenue), `sbx_execution`, `sbx_piece`.
- **Adaptateurs** : `gimac.adapters.sbx-lecture` vaut `stub` (substitut au contrat exact, aucune donnée tant qu'il
  n'est pas alimenté) jusqu'à la mise en service de SBX-LECTURE, puis `real` (`gimac.services.sbx-lecture.url`).

| Critère | Test |
|---|---|
| 1. Relève des exécutions, plus haute position retenue, événement publié | `ReleveSandboxIT.ca1ReleveDesExecutions` |
| 2. Indisponibilité (503 au milieu de la relève, délai indicatif) | `ReleveSandboxIT.ca2IndisponibiliteDeLaSandbox`, `ContratLectureSandboxTest` |
| 3. Pièce déjà conservée, empreinte divergente | `ReleveSandboxIT.ca3PieceDejaConservee`, `PiecesEtCouverturesSandboxTest` |
| 4. Scénario inconnu isolé, aucune écriture | `ReleveSandboxIT.ca4ScenarioInconnu`, `ReleveSandboxTest` |
| Contrat lisible par machine | `ContratLectureSandboxTest`, `TraducteurLectureSandboxTest`, `LectureSandboxStubAdapterTest` |

### 7.5 Notification de fin de campagne (US-EXI-15)

- **Sujet réservé** `sbx-lecture.campaign-published.v1` (nommé par le type, comme les sujets de la sandbox ; écriture par SBX-LECTURE seul, lecture par le compte de service de la
  plateforme et par MS-AUDIT ; créé par `infra/kafka/creer-sujets-lot1.sh` avec sa file de rebut), consommateur
  `FinCampagneSandboxConsumer` (groupe `homologation-fin-campagne-sandbox`, sujet configurable
  `gimac.consommateurs.fin-campagne-sandbox.sujet`).
- **Enveloppe** : CloudEvents 1.0, type `sbx-lecture.campaign-published.v1`, `data` = `{ identifiantCampagne, motif }`
  (fin de campagne, rectification d'un verdict après clôture, annulation). Une notification sans identifiant ou sans
  campagne est ignorée et journalisée.
- **Effet** : relecture immédiate de la campagne désignée par le contrat de lecture (`ReleveSandbox.releverCampagneNotifiee`),
  dont la liaison est prise dans le détail lu ; cette lecture vaut accusé de lecture pour SBX-LECTURE. Aucune écriture
  vers la sandbox.
- **Idempotence** : par identifiant d'événement (table des événements traités) ; une notification réémise à
  l'identique est acquittée sans second traitement. La relecture est faite hors transaction englobante.
- **Relève périodique maintenue** : elle reste la source de vérité ; une notification désactivée, perdue ou dont la
  relecture échoue (sandbox indisponible : échec noté sur la relève de la liaison) est reprise par elle.

| Critère | Test |
|---|---|
| 1. Relecture déclenchée, accusé constaté | `ReleveSandboxIT.exi15RelectureDeclenchee`, `FinCampagneSandboxConsumerTest.relectureDeclenchee`, `ReleveSandboxTest.relectureNotifiee` |
| 2. Notification réémise sans second traitement | `ReleveSandboxIT.exi15RelectureDeclenchee`, `FinCampagneSandboxConsumerTest.notificationReemise` |
| 3. Notification désactivée ou perdue : relève périodique | `ReleveSandboxIT.exi15ReleveSansNotification`, `ReleveSandboxTest.relectureNotifieeIndisponible` |

### 7.6 Onglet « Résultats de la sandbox » du dossier de liaison (US-EXI-17, DES-09)

- **Route** : `GET /bff/des/homologation/liaisons/{id}/sandbox` et `GET …/sandbox/campagnes/{c}/executions?page=&taille=`
  (MS-HOMOLOGATION, `ResultatsSandboxResource`, `HOM_LIAISON_READ` ; règle OPA explicite, lecture seule). Aucune route
  de l'application du département vers un service de la sandbox.
- **Écran** : onglet `?onglet=sandbox` du dossier `/des/parcours/:id` (`@gimac/lot1`, `OngletSandbox`) : mention
  permanente « Repris de la sandbox, jamais recalculé » ; dernière relève réussie ; campagnes reprises (liste
  paginée, choix d'une campagne) ; pour la campagne choisie, version du bilan et version de la table des verdicts,
  taux par service avec numérateur, dénominateur, version de formule et horodatage, exécutions retenues en pagination
  serveur (verdict natif, verdict normalisé, version de la table, opération rapprochée ou « Non rapprochée »), pièces
  avec leur empreinte copiable et leur conservation ; scénarios non rapprochés. Aucune action vers la sandbox.
- **Sandbox indisponible** : bandeau « Dernière relève en échec » (date et motif) ; les résultats déjà repris restent
  affichés avec la date de la dernière relève réussie.
- FR, EN, ES (`i18n/ecrans/sandbox-resultats.ts`) ; services simulés au contrat exact (`services/simule/sandbox.ts`).
- Captures : `docs/recette/captures/exi/homologation/exi-17/`.

| Critère | Test |
|---|---|
| 1. Verdicts, version de la table, taux en lecture | `OngletSandbox.test.tsx` (critère 1), `ReleveSandboxIT.ca4ScenarioInconnu` (route) |
| 2. Scénarios non rapprochés signalés | `OngletSandbox.test.tsx` (critère 2) |
| 3. Sandbox indisponible, dernière relève réussie | `OngletSandbox.test.tsx` (critère 3) |
| Mention permanente, aucune action d'écriture, accessibilité | `OngletSandbox.test.tsx` (critère 1, audit axe) |

## 8. MS-IAM : périmètres, délégations et comptes de service (lot IAM, D-119)

Côté plateforme du port `IamAdministrationPort` de SBX-ESPACE. Toute écriture est une étape de saga : en-tête
`Idempotency-Key` (identifiant de saga et rang de l'étape) ; une étape reprise avec la même clé rend son résultat
d'origine sans second effet, même quand deux reprises arrivent en même temps (réservation de la clé par insertion sans
conflit dans `etape_idempotente`, la seconde attend la première et lit son résultat).

### 8.1 Routes internes et permissions de service

Jetons de service seulement, sans second facteur ; `INTERNAL_IAM_READ` n'ouvre aucune de ces routes (moindre
privilège, règle placée avant celle de `/api/v1/interne/**`).

| Opération du port | Méthode et route MS-IAM | Permission | Corps | Réponse |
|---|---|---|---|---|
| `ouvrirCompte(…, cle)` (saga OUVERTURE) | `POST /api/v1/interne/sandbox/comptes` | `INTERNAL_IAM_SBX_SAGA` | `{ identifiant, prenom, nom, email, telephone?, fonction?, institutionId, profils }` | 201, ou 200 au rejeu : compte, profils et `permissionsSbx`, `rejouee` ; invitation, aucun mot de passe rendu |
| `attribuerRole(…, cle)` (saga MATRICE) | `PUT /api/v1/interne/sandbox/comptes/{id}/profils-sandbox` | `INTERNAL_IAM_SBX_SAGA` | `{ profils, motif }` | Compte et permissions SBX ; seuls les profils SBX changent, les autres accès sont conservés |
| `publierMatrice(…, cle)` (saga MATRICE, US-INS-07) | `PUT /api/v1/interne/sandbox/matrice` | `INTERNAL_IAM_SBX_SAGA` | `{ version, profils: { CODE: [SBX_*] } }` | `{ version, rejouee }` ; seules les permissions SBX des profils fournis changent, leurs autres permissions sont conservées |
| (lecture du compte) | `GET /api/v1/interne/sandbox/comptes/{id}` | `INTERNAL_IAM_SBX_SAGA` | | même forme |
| `suspendrePerimetre(code, suspendu, cle)` | `PUT /api/v1/interne/sandbox/perimetres/{code}/suspension` | `INTERNAL_IAM_SBX_SAGA` | `{ suspendu, motif? }` | `{ code, suspendu, depuis, sessionsRevoquees, rejouee }` |
| (lecture) | `GET /api/v1/interne/sandbox/perimetres/{code}` | `INTERNAL_IAM_SBX_SAGA` | | même forme |
| `attribuerRole(…, protege, cle)` (attribut seul) | `PUT /api/v1/interne/sandbox/comptes/{id}/protection-dernier-administrateur` | `INTERNAL_IAM_SBX_SAGA` | `{ protege }` | `{ compteId, institutionId, protege, rejouee }` |
| (passerelle, services) | `POST /api/v1/interne/sandbox/introspection` | `INTERNAL_IAM_SBX_INTROSPECT` | `{ jeton, institution? }` | RFC 7662 : `{ active: false }`, ou `{ active, sub, institution_id, institution_propre, permissions, type, delegation_id, composants, exp }` ; `institution_id` et `institution_propre` sont des codes participants traduits par MS-IAM (D-134) |

Clients du domaine Keycloak (générateur) : `sbx-espace` porte `INTERNAL_IAM_SBX_SAGA` (seul porteur, vérifié par le
générateur) et `INTERNAL_PARTICIPANT_READ` ; `gateway-service` reçoit `INTERNAL_IAM_SBX_INTROSPECT`. Secret
`GIMAC_SECRET_SBX_ESPACE` (Compose, `gimac-dev.sh`, Kubernetes, `diagnostiquer-serveur.sh`). Code de périmètre :
`^[A-Za-z0-9_-]{1,64}$`, sinon 400 `IAM_SBX_PERIMETRE_INVALIDE`.

OUVERTURE et MATRICE réutilisent `AdministrationComptesService`, `EtapesIdempotentes` et `ReglesSandbox`. Seuls des
profils externes dont la composition porte une permission SBX sont acceptés (`IAM_SBX_PROFIL_REQUIS` ou
`IAM_SBX_PROFIL_INVALIDE`) ; MATRICE préserve les profils non SBX du compte. La matrice globale US-INS-07 réutilise de
même `ConfigurationAuthentificationService` : toutes les entrées sont validées avant écriture, chaque recomposition est
auditée, les permissions hors SBX sont fusionnées et la validation externe reste refusée. Contrats et reprises :
`ComptesSandboxSagaServiceTest`, `MatriceSandboxSagaServiceTest`, `ComptesSandboxSagaIT`, `SandboxPerimetresRoutesTest`.

### 8.2 Suspension d'institution et dernier administrateur (US-EXI-06)

- **Indicateur superposé** (table `perimetre_suspendu`) : jamais recopié sur les comptes. État effectif d'un compte
  d'institution = compte actif et périmètre non suspendu. Le rétablissement lève l'indicateur : chaque compte retrouve
  son propre état, un compte suspendu avant la suspension le reste.
- **Effet immédiat** : à la pose, les sessions Keycloak de chaque compte de l'institution sont fermées (révocation
  globale, les jetons de ces sessions deviennent inactifs pour Keycloak) ; `GET /api/v1/interne/comptes/{id}/etat-connexion`
  rend `actif: false` (la passerelle refuse la connexion, D-65) ; l'introspection de MS-IAM refuse tout jeton d'un
  compte du périmètre (`PERIMETRE_SUSPENDU`), à l'instant de la requête et sans cache.
- **Traces** : entrée `PERIMETRE_SUSPENDU` ou `PERIMETRE_RETABLI` au journal d'administration (cible : le code), et
  `iam.account-administered.v1` pour chaque compte du périmètre (équivalent de `iam.user-changed.v1`). Pose répétée
  avec une autre clé : sessions de nouveau révoquées, aucune nouvelle trace.
- **Protection du dernier administrateur** (table `protection_administrateur`, posée et levée par SBX-ESPACE) : la
  suspension du compte par l'institution (`PUT /api/v1/administration-deleguee/comptes/{id}/etat`, `actif: false`) est
  refusée, 409 `IAM_SBX_COMPTE_PROTEGE`, `details.motif`, tentative journalisée `TENTATIVE_SUSPENSION_COMPTE_PROTEGE` ;
  la désactivation pour inactivité l'écarte (`DESACTIVATION_INACTIVITE_ECARTEE`). Compte du GIMAC : 422
  `IAM_SBX_COMPTE_HORS_INSTITUTION`.
- **Désactivation des comptes inactifs de la sandbox** (IAM-15, n'existait pas encore dans MS-IAM) : comptes
  d'institution portant un rôle de la sandbox, actifs, sans connexion depuis `gimac.sandbox.inactivite-jours` (90) ;
  passage quotidien à 2 h 30 (`gimac.sandbox.inactivite-planification`), une seule réplique à la fois (verrou
  consultatif PostgreSQL) ; désactivation par l'administration des comptes (journal, événement), acteur
  `systeme-iam`. Dernière connexion : relevée quand la passerelle lit l'état de connexion après le mot de passe (table
  `activite_compte`) ; à défaut, date de création du compte.
- **Console** (`/console/sandbox`, fonctionnalité `consoleSandbox`, `IAM_ACCOUNT_READ`) : onglets « Périmètres
  suspendus » et « Comptes protégés », lecture seule ; routes `GET /bff/console/iam/sandbox/{perimetres,
  perimetres/{code}, protections}` vers `/api/v1/sandbox/**` (route `console-iam-sandbox`, OPA bloc D-119). Compte de
  démonstration : `ROLE_SUPER_ADMIN` (ou `ROLE_RSSI`). Captures : `docs/recette/captures/exi/iam/exi-06/`.
- **À brancher par le lot passerelle** : la décision de MS-IAM est servie par l'introspection (§8.1). Le cache de
  quatre minutes de `KeycloakIntrospectionClient` et la projection `sbx_perimetre` (§6.3) ne voient pas la suspension :
  la passerelle doit interroger l'introspection de MS-IAM (sans cache, ou avec un cache borné à quelques secondes) pour
  que le refus soit immédiat sur le jeton réduit. D'ici là, la suspension prend effet à la fermeture des sessions
  (Keycloak) et à la connexion suivante.

| Critère | Test |
|---|---|
| 1. Introspection refusée, sessions révoquées, état des comptes inchangé | `SandboxPerimetresIT.refusImmediatApresSuspension`, `PerimetresSandboxServiceTest.suspensionDuPerimetre`, `IntrospectionSandboxServiceTest.perimetreSuspendu`, `EtatEffectifSandboxTest.etatEffectif` |
| 2. Rétablissement à l'état antérieur | `SandboxPerimetresIT.retablissement`, `PerimetresSandboxServiceTest.retablissement`, `ReglesPerimetreTest.actifEffectif` |
| 3. Dernier administrateur épargné et suspension refusée avec motif | `SandboxPerimetresIT.compteProtege`, `DesactivationInactifsServiceTest.compteProtegeEcarte`, `AdministrationDelegueeServiceTest.suspensionCompteProtegeRefusee` |
| 4. Reprise idempotente de l'étape pivot | `SandboxPerimetresIT.repriseIdempotente` (trois reprises concurrentes), `PerimetresSandboxServiceTest.repriseIdempotente` |
| Routes et moindre privilège | `SandboxPerimetresRoutesTest`, OPA `test_sbx_console_iam_lecture`, `test_sbx_console_iam_refus` |

Délégations de mandat comprises dans le refus (critère 1) : la suspension révoque aussi les sessions des titulaires d'un
mandat sur le périmètre, et l'introspection refuse tout jeton délégué vers un périmètre suspendu (§8.3 ;
`PerimetresSandboxServiceTest.titulairesDeMandat`, `SandboxDelegationsIT.prolongationEtSuspension`).

### 8.3 Mandats de partenaire (US-EXI-04)

| Opération du port | Méthode et route MS-IAM (`INTERNAL_IAM_SBX_SAGA`) | Corps | Réponse |
|---|---|---|---|
| `accorderDelegation(compte, code, perimetre, debut, fin, cle)` | `POST /api/v1/interne/sandbox/delegations` | `{ compteId, institution, permissions, debut, fin }` (ISO 8601) | 201, ou 200 pour une étape rejouée : `{ delegation, etat, nomCompte, rejouee }` ; `delegation.id` est l'identifiant rendu par le port |
| `revoquerDelegation(id, cle)` | `DELETE /api/v1/interne/sandbox/delegations/{id}?motif=` | | même forme, `etat: REVOQUEE` |
| prolongation (algorithme 6.3, étape 7) | `PUT /api/v1/interne/sandbox/delegations/{id}/fin` | `{ fin }` | même forme |
| lecture | `GET /api/v1/interne/sandbox/delegations/{id}` | | même forme |

- **Droits** : codes `SBX_<MODULE>_<FONCTION>` (le `Permission(module, fonction)` de SBX-ESPACE se transmet ainsi),
  inclus dans les droits accordables par un rôle de portée institution (union des compositions des profils externes à
  permission SBX), jamais `*_VALIDATION` (422 `IAM_SBX_VALIDATION_RESERVEE`) ; hors portée : 403
  `IAM_SBX_PERIMETRE_HORS_PORTEE` ; aucun : 400 `IAM_SBX_PERIMETRE_REQUIS`.
- **Bornes** : début avant la fin (422 `IAM_SBX_PERIODE_INVALIDE`), fin à venir (422 `IAM_SBX_PERIODE_ECHUE`), durée au
  plus `gimac.sandbox.mandat-duree-max` (90 jours ; 422 `IAM_SBX_DUREE_EXCESSIVE`, `details.dureeMax`). Un refus ne
  laisse aucun état : ni délégation, ni clé d'idempotence (la saga compense).
- **Intervenant** : compte actif (422 `IAM_SBX_COMPTE_INACTIF`), extérieur à l'institution mandante (422
  `IAM_SBX_MANDAT_PROPRE_INSTITUTION`) ; périmètre mandant suspendu : 403 `IAM_SBX_PERIMETRE_SUSPENDU`.
- **Jeton délégué** : la passerelle (ou un service) présente le jeton du partenaire à l'introspection avec
  `institution` = code mandant. MS-IAM l'admet sous un mandat actif à l'instant de la requête et rend `institution_id`
  mandante, `permissions` = droits du mandat seulement, `type: MANDAT`, `delegation_id`, `exp` = plus proche de la fin
  du jeton et de la fin du mandat. Hors mandat, mandat programmé, échu (dès la date de fin, sans le planificateur de
  SBX-ESPACE), révoqué, ou périmètre mandant suspendu : `{ active: false }`.
- **Révocation** : délégation close (`REVOQUEE`, date et motif), sessions Keycloak du titulaire fermées, introspection
  refusée dès la validation. Rejeu ou délégation déjà révoquée : sans effet.
- **Prolongation** : délégation active ou programmée (409 `IAM_SBX_DELEGATION_CLOSE` sinon), nouvelle fin après
  l'actuelle (422 `IAM_SBX_PROLONGATION_INVALIDE`), durée totale sous le maximum.
- **Traces** : `MANDAT_ACCORDE`, `DELEGATION_REVOQUEE`, `DELEGATION_PROLONGEE` au journal d'administration, sur le
  compte de l'intervenant, avec `iam.account-administered.v1`. Table `delegation_sandbox` (l'entité `Delegation` de
  JHipster n'est pas reprise).
- **Console** : onglet « Mandats de partenaire » de `/console/sandbox` (`GET /api/v1/sandbox/delegations?type=MANDAT`),
  état effectif à l'instant (programmé, actif, échu, révoqué), lecture seule. Captures :
  `docs/recette/captures/exi/iam/exi-04/`.

| Critère | Test |
|---|---|
| 1. Jeton délégué : institution mandante, seuls droits du mandat, dates | `SandboxDelegationsIT.octroi`, `IntrospectionSandboxServiceTest.jetonDelegue`, `DelegationsSandboxServiceTest.octroi` |
| 2. Échéance malgré un planificateur en retard | `SandboxDelegationsIT.echeanceSansPlanificateur`, `IntrospectionSandboxServiceTest.echeanceSansPlanificateur`, `ReglesDelegationTest.etatEffectif` |
| 3. Révocation anticipée, sessions révoquées | `SandboxDelegationsIT.revocation`, `DelegationsSandboxServiceTest.revocation` |
| 4. Durée excessive, problème normalisé, sans état partiel | `SandboxDelegationsIT.dureeExcessive`, `DelegationsSandboxServiceTest.dureeExcessive`, `ReglesDelegationTest.dureeExcessive` |
| Routes | `SandboxPerimetresRoutesTest.delegations` ; écran : `SandboxIamPage.test.tsx` |

### 8.4 Accès de prestataire de maintenance (US-EXI-24, D-123)

Délégation temporaire de type `PRESTATAIRE` (table `delegation_sandbox`), bornée aux composants techniques désignés
(`SBX-ESPACE` … `SBX-LECTURE`, `MIFOS`), sans institution ni permission sur les données des institutions. Routes de la
console (`/bff/console/iam/sandbox/**` vers `/api/v1/sandbox/**`, `IAM_ACCOUNT_SECURITY`, second facteur ; OPA bloc
D-123) :

| Méthode et route | Corps | Effet |
|---|---|---|
| `POST /delegations/prestataire` | `{ compteId, composants, debut, fin, motif }` | 201, délégation `DEMANDEE` : n'ouvre rien |
| `POST /delegations/{id}/approbation` | | `ACCORDEE`, active de son début à sa fin ; 403 `IAM_SBX_APPROBATION_PROPRE` pour l'auteur de la demande |
| `POST /delegations/{id}/refus` | `{ motif }` | `REFUSEE` |
| `POST /delegations/{id}/revocation` | `{ motif }` | `REVOQUEE`, sessions du prestataire fermées |
| `GET /delegations?type=PRESTATAIRE` | | Liste paginée (`IAM_ACCOUNT_READ`) |

- **Contrôles** : compte actif (422 `IAM_SBX_COMPTE_INACTIF`) et sans institution (422
  `IAM_SBX_PRESTATAIRE_INSTITUTION`) ; au moins un composant connu (400 `IAM_SBX_COMPOSANT_REQUIS`,
  `IAM_SBX_COMPOSANT_INCONNU`) ; durée au plus `gimac.sandbox.prestataire-duree-max` (8 heures ; 422
  `IAM_SBX_DUREE_EXCESSIVE`) ; demande déjà décidée ou échue : 409 `IAM_SBX_DELEGATION_CLOSE` ; un mandat n'est ni
  approuvé ni révoqué par ces routes (404).
- **Introspection** (§8.1) : la requête porte en plus `composant` (composant appelé). Le jeton d'un compte désigné
  dans un accès de prestataire n'est admis que sous un accès actif à l'instant de la requête pour ce composant :
  `{ active, sub, institution_id: null, permissions: [], type: PRESTATAIRE, delegation_id, composants, exp }`, `exp` au
  plus tard à la fin de l'accès. Hors accès, échu (dès la date de fin, sans planificateur), révoqué, refusé ou autre
  composant : `{ active: false }`. Une institution demandée par un prestataire est refusée et journalisée
  (`TENTATIVE_ACCES_PRESTATAIRE`, `iam.account-administered.v1`).
- **Traces** : `ACCES_PRESTATAIRE_DEMANDE`, `ACCES_PRESTATAIRE_ACCORDE`, `ACCES_PRESTATAIRE_REFUSE`,
  `DELEGATION_REVOQUEE` au journal d'administration, avec `iam.account-administered.v1` (registre d'audit).
- **Console** : onglet « Accès de prestataire » de `/console/sandbox` : liste paginée (titulaire, composants et motif,
  période, état effectif), demande (compte, composants, début à l'heure de Douala, durée de une à huit heures, motif),
  approbation, refus et révocation motivés pour les porteurs d'`IAM_ACCOUNT_SECURITY` ; lecture seule sinon. Captures :
  `docs/recette/captures/exi/iam/exi-24/`.

| Critère | Test |
|---|---|
| 1. Demande sur SBX-TRACE approuvée par le RSSI pour quatre heures, octroi journalisé | `SandboxDelegationsIT.accesPrestataire`, `DelegationsSandboxServiceTest.accesPrestataire`, `SandboxIamPage.test.tsx` (scénario 1) |
| 2. Échéance : introspection refusée | `SandboxDelegationsIT.accesPrestataire`, `IntrospectionSandboxServiceTest.echeancePrestataire`, `SandboxIamPage.test.tsx` (scénario 2) |
| 3. Données d'une institution : refus journalisé | `SandboxDelegationsIT.accesPrestataire`, `IntrospectionSandboxServiceTest.prestataireHorsDonnees` |
| Routes, moindre privilège | `SandboxPerimetresRoutesTest.prestataire`, OPA `test_sbx_prestataire_console` |

### 8.5 Verrouillage, déblocage et journal des connexions (US-EXI-23, D-124)

Les comptes de la sandbox sont des comptes du domaine GIMAC : ils héritent tels quels du verrouillage après un nombre
d'échecs paramétré (protection contre la force brute de Keycloak, `forceBruteEchecsMax` et `forceBruteAttenteMinutes`
de la politique de sécurité de MS-IAM, console « Politique de sécurité »), du journal des connexions et des
tentatives (`gateway.login-failed.v1`, événements de connexion du fournisseur d'identité) et de la désactivation des
comptes inactifs (D-119). Ajouts :

| Méthode et route | Permission | Effet |
|---|---|---|
| `GET /bff/console/iam/comptes/{id}/verrouillage` | `IAM_ACCOUNT_READ` | `{ verrouille, echecs, dernierEchec }` (Keycloak : `attack-detection/brute-force/users/{id}`) |
| `POST /bff/console/iam/comptes/{id}/deblocage` | `IAM_ACCOUNT_SECURITY`, second facteur | `{ motif }` : verrouillage levé, compteur remis à zéro, journal `COMPTE_DEBLOQUE` (détail : nombre d'échecs) et `iam.account-administered.v1` ; jamais sur soi-même (403 `IAM_SELF_ADMINISTRATION`) ; motif exigé (400 `IAM_REASON_REQUIRED`) ; compte non verrouillé : sans effet ni trace |
| `POST /bff/portail/sbx-habilitations/comptes/{id}/deblocage` | `SBX_HAB_ADMINISTRATION` (administrateur de l'institution) | Refus 403 `IAM_SBX_DEBLOCAGE_RESERVE` et tentative journalisée (`TENTATIVE_DEBLOCAGE`, détail : institution) ; compte hors de l'institution : 404 sans révéler son existence |

- **Console** : fiche du compte (`/console/comptes/{id}`), bandeau « Compte verrouillé » (nombre d'échecs, dernier
  échec) et action « Débloquer le compte » motivée, pour les porteurs d'`IAM_ACCOUNT_SECURITY`. Captures :
  `docs/recette/captures/exi/iam/exi-23/`.
- **Réexamen périodique (critère 2, IAM-18)** : `ReexamensAccesService` ouvre une campagne `AAAA-Tn` au premier jour
  de chaque trimestre à 3 h (`gimac.iam.reexamen-planification`, désactivation par `gimac.iam.reexamen-actif=false`),
  sous le verrou de tâches partagé et avec unicité de la période. Chaque ligne `AccessReviewDecision` fige profils et
  permissions effectives de tous les comptes, `SBX_*` comprises. Décisions `MAINTENU`, `REDUIT` (sous-ensemble strict,
  sans ajout) et `REVOQUE`, motif obligatoire pour les deux dernières ; réduction et révocation passent par
  `AdministrationComptesService`, donc par ses contrôles, son journal et ses événements. Clôture refusée tant qu'un accès
  est `EN_ATTENTE` ; à clôture, SHA-256 de la période et des lignes canoniques triées, approbateur et date conservés.
  Actes `REEXAMEN_OUVERT`, `REEXAMEN_DECISION_*` et `REEXAMEN_CLOS` au journal d'administration.
- **Routes MS-IAM** : `GET /api/v1/comptes/reexamens` et `GET /api/v1/comptes/reexamens/{id}` (`IAM_ACCOUNT_READ`),
  `POST /api/v1/comptes/reexamens/ouverture` (`IAM_ACCOUNT_CREATE`, reprise de la période courante), décision et clôture
  par `POST /api/v1/comptes/reexamens/{id}/comptes/{compteId}/decision` et `/cloture` (`IAM_ACCOUNT_ROLES`). La console
  ajoute l'onglet « Réexamen des accès » à `/console/sandbox` : instantané, permissions SBX, décisions et empreinte.
  La route BFF existante `/bff/console/iam/comptes/**` relaie ces appels ; les règles OPA des nouvelles écritures restent
  à ajouter par le lot passerelle, hors du présent périmètre.

| Critère | Test |
|---|---|
| 1. Verrouillage après le nombre d'échecs, déblocage par l'administrateur du GIMAC | `AdministrationComptesServiceTest.verrouillageEtDeblocage`, `AdministrationDelegueeIT.verrouillageEtDeblocage`, `ComptePage.test.tsx` |
| 2. Réexamen des accès portant sur les rôles de la sandbox | `ReexamensAccesServiceTest`, `ReexamensAccesIT.campagneComplete`, `ReexamenAccesTest`, `reexamens.test.tsx` |
| 3. Déblocage par l'administrateur de l'institution refusé et journalisé | `AdministrationDelegueeServiceTest.deblocageRefuse`, `AdministrationDelegueeIT.verrouillageEtDeblocage`, OPA `test_sbx_deblocage` |

## 9. Comptes de service de la sandbox (US-EXI-05, D-123)

Un client confidentiel du domaine Keycloak par service de la sandbox (`sbx-espace`, `sbx-raccordement`,
`sbx-referentiel`, `sbx-donnees`, `sbx-simulation`, `sbx-mobile`, `sbx-iso20022`, `sbx-carte`, `sbx-campagne`,
`sbx-trace`, `sbx-lecture`) et un compte dédié au cœur bancaire simulé Mifos (`sbx-mifos`, tenu par SBX-DONNEES pour
ses seuls appels à Mifos). Secrets `GIMAC_SECRET_SBX_<SERVICE>` (Compose, `gimac-dev.sh`, Kubernetes, `creer-secrets.sh`,
`diagnostiquer-serveur.sh`) ; aucun identifiant commun. Les autorisations d'appel sont dérivées du tableau des appels
synchrones du CC-SBX-GIMAC-02 (tableau 5.3), une permission par opération appelée (`APPELS_SBX` du générateur) :

| Appelant | Permission | Appelé, objet |
|---|---|---|
| sbx-espace | `INTERNAL_PARTICIPANT_READ`, `INTERNAL_IAM_SBX_SAGA` | MS-PARTICIPANT (fiche), MS-IAM (sagas) |
| sbx-raccordement, sbx-referentiel | `INTERNAL_PARTICIPANT_READ` | MS-PARTICIPANT : code participant, nomenclature |
| sbx-mobile, sbx-iso20022 | `INTERNAL_SBX_RACCORDEMENT_IDENTIFIANTS` | SBX-RACCORDEMENT : identifiant d'accès de test |
| sbx-simulation | `INTERNAL_SBX_TRACE_REFERENCES`, `INTERNAL_SBX_DONNEES_ETATS`, `INTERNAL_SBX_RACCORDEMENT_LIAISONS` | SBX-TRACE, SBX-DONNEES, SBX-RACCORDEMENT |
| sbx-mobile, sbx-iso20022, sbx-carte | `INTERNAL_SBX_DONNEES_MOUVEMENTS` | SBX-DONNEES : mouvements, comptes, cartes, PIN, clés |
| sbx-carte | `INTERNAL_SBX_MOBILE_COUPONS` | SBX-MOBILE : coupon de retrait sans carte |
| sbx-campagne, sbx-donnees | `INTERNAL_SBX_RACCORDEMENT_RATTACHABILITE` | SBX-RACCORDEMENT : rattachabilité |
| sbx-trace | `INTERNAL_SBX_CAMPAGNE_REFERENCES_TRACE`, `INTERNAL_SBX_SANTE` | SBX-CAMPAGNE (références protégées), points de santé des onze services |
| sbx-campagne, sbx-trace, sbx-iso20022 | `INTERNAL_DOCUMENT_DEPOSIT` | MS-DOCUMENT : pièces de la sandbox (§6.7) |
| sbx-mifos | `INTERNAL_SBX_MIFOS_API` | Mifos : interfaces publiques |
| sbx-lecture | aucune | aucun appel sortant (CC §6.1.11) |
| homologation-service (plateforme du DES) | `INTERNAL_SBX_LECTURE_READ` | SBX-LECTURE : contrat de lecture (§7.4) |
| exploitation-service | `INTERNAL_HOMOLOGATION_REPUBLISH` | MS-HOMOLOGATION : republication des états (§7.3) |

- **Vérifié par le générateur** : chaque compte `sbx-*` porte exactement les permissions du tableau ; aucune
  permission `INTERNAL_SBX_*` hors du tableau ; aucune autorisation de la sandbox vers MS-CERTIFICATION ni
  MS-HOMOLOGATION (US-EXI-25, critère 3) ; `INTERNAL_HOMOLOGATION_REPUBLISH` au seul compte d'exploitation.
- **Refus hors tableau** (critère 3) : chaque service appelé de la sandbox n'accepte que la permission de l'opération
  appelée et journalise la tentative vers MS-AUDIT (côté sandbox) ; côté plateforme, MS-PARTICIPANT journalise déjà
  toute écriture tentée par un client `sbx-*` (§6.6).
- **Réglages du déploiement** (hors code) : rattachement des jetons de compte de service au certificat client de
  l'autorité de certification interne (RFC 8705, attribut `tls.client.certificate.bound.access.tokens` des clients,
  passerelle interne de service en mTLS), rotation des secrets par `creer-secrets.sh` et reprise du domaine (fusion à
  trois voies, sans interruption des autres clients). Le contrôle de l'empreinte du certificat est fait par
  SBX-LECTURE (critère 4).
- **Écart au CP** : `iam.user-changed.v1` « à chaque changement d'état du compte de service de la plateforme » n'est
  pas publié : MS-IAM n'administre pas les comptes de service (domaine Keycloak généré) ; un changement passe par le
  générateur et la reprise du domaine, tracés par la chaîne de livraison.

| Critère | Test |
|---|---|
| 1. Appel autorisé entre services, pour cette seule opération | générateur (`APPELS_SBX`, assertions), `valider-infra.sh` (domaine régénéré identique) ; jeton réduit : côté sandbox |
| 2. Jeton du compte de service de la plateforme, lecture des seules liaisons ouvertes | `homologation-service` porte `INTERNAL_SBX_LECTURE_READ` ; périmètre et empreinte : côté SBX-LECTURE |
| 3. Appel hors du tableau refusé et journalisé | générateur (matrice exacte) ; refus et journal : côté sandbox |
| 4. Jeton sans le certificat lié | réglage du déploiement, contrôle par SBX-LECTURE |

## 10. Événements à effet de la sandbox dans MS-AUDIT (US-EXI-10, D-124)

MS-AUDIT consomme les événements à effet des onze services de la sandbox par abonnement à un motif de sujets
(`gimac.consommateurs.sandbox.motif`, défaut `sbx-[a-z0-9]+\.[a-z0-9-]+(\.v[0-9]+)?` : chaque service publie sur le
sujet nommé d'après le type de l'événement ; les files `.dlq` et `.dlt` ne correspondent pas), groupe `audit-sandbox`,
idempotent par identifiant d'événement.

- **Contrôle de l'enveloppe** (`EnveloppeSandbox`, dans l'ordre) : `specversion` 1.0 (`SPECVERSION_INVALIDE`),
  `id` (`ID_ABSENT`), `type` `sbx-<service>.<action>.v<n>` d'un des onze services (`TYPE_INVALIDE`), `source` dont le
  dernier segment est le service (`/gimac/sandbox/sbx-campagne` ; `SOURCE_INVALIDE`), `time` ISO-8601
  (`TIME_INVALIDE`), `data` présentes (`DATA_ABSENTES`), institution (extension `institutionid` ou
  `data.codeParticipant`) au format d'un code participant (`INSTITUTION_INVALIDE`). Les demandes de notification
  (`*.notification-requested.v1`, §11) ne sont pas des événements à effet : ignorées.
- **Inscription** : registre inaltérable existant (feuille de l'arbre de Merkle, scellement de la période et preuve
  d'inclusion inchangés), domaine `sandbox`, auteur (`data.acteur`, à défaut le service), horodatage de référence
  (`time`), objet (`subject`), institution (colonne `institution_id`, indexée).
- **Rebut** : enveloppe non conforme ou illisible inscrite dans `rebut_sandbox` (identifiant, sujet, motif, reçu le) ;
  au-delà de `gimac.audit.sandbox.seuil-rebut` (10) entrées, l'alerte de retard de consommation est levée (indicateur
  `alerte` de la lecture du rebut, journal `ALERTE_EXPLOITATION`).
- **Console** (`/bff/console/audit/sandbox/**` vers `/api/v1/registre/sandbox`, route `console-audit-sandbox` de la
  passerelle, `SBX_TRA_CONSULTATION`, second facteur) : recherche paginée filtrée par institution, export CSV (RFC 4180,
  UTF-8, fichier `registre-sandbox-<institution>.csv`), rebut paginé. Onglet « Registre d'audit » de
  `/console/sandbox`, visible des seuls porteurs de `SBX_TRA_CONSULTATION`. Captures :
  `docs/recette/captures/exi/audit/exi-10/`.

| Critère | Test |
|---|---|
| 1. Verdict validé inscrit (auteur, horodatage, objet), preuve après scellement | `EnveloppeSandboxTest`, `EvenementsSandboxConsumerTest.inscription`, `RegistreSandboxIT.registreDeLaSandbox` |
| 2. Recherche par source sandbox et institution 99001, export au format ouvert | `RegistreSandboxIT.registreDeLaSandbox`, `RegistreSandboxQueriesTest`, `SandboxIamPage.test.tsx` (registre), OPA `test_sbx_registre_audit` |
| 3. Enveloppe non conforme au rebut avec son motif, alerte au-delà du seuil | `EnveloppeSandboxTest.nonConformes`, `EvenementsSandboxConsumerTest.rebut`, `RegistreSandboxIT` |
| Abonnement par motif, redélivrance sans effet | `RegistreSandboxIT.kafka` |

## 11. Notifications de la sandbox dans MS-NOTIFICATION (US-EXI-09, D-124)

Les services de la sandbox ne notifient personne directement : ils publient `sbx-<service>.notification-requested.v1`
sur le sujet du même nom (CloudEvents 1.0, source `/gimac/sandbox/sbx-<service>`, clé : code participant), données
`{ modele, destinataires, codeParticipant, codeEspace, le }` ; un destinataire est un compte nommé ou un rôle
`ROLE:<rôle>`. Forme de SBX-DONNEES également admise : `gabarit` pour le modèle, `destinataire` unique (nom de rôle :
`RSSI`, `EXPLOITATION`, `PRODUCTION_MONETIQUE`), code participant pris de l'extension `partitionkey`. MS-NOTIFICATION
s'abonne aux onze sujets (`gimac.consommateurs.sandbox.sujets`, groupe
`notification-sandbox`, idempotent par identifiant d'événement, files `.dlt` créées au démarrage) et crée une
notification de type `SANDBOX` par destinataire résolu, jamais regroupée, avec `notification.inapp-created.v1`.

- **Catalogue unique** (ajout du 05/11/2026) : modèles et rôles sont tenus dans
  `ms-notification/src/main/resources/config/notifications-sandbox.json`, chargé par `CatalogueNotificationsSandbox` ;
  le même fichier est copié dans le dépôt de la sandbox (`docs/contrats/notifications-sandbox.json`), où chaque service
  émetteur refuse à l'émission tout modèle absent de sa liste (`ModelesNotification`) et vérifie par un test que ses
  modèles et ses rôles figurent au catalogue. Préfixes : `SBX_CER_` (SBX-CAMPAGNE), `SBX_REF_`, `SBX_TRA_`, `SBX_SIM_` ;
  un compte nommé est un identifiant nu, sans préfixe.
- **Règles** (`ReglesSandbox`, service de domaine) : chaque modèle (`SBX_ESP_*` invitations, suspension,
  rétablissement, mandats et échéances ; `SBX_RAC_*` échéance de certificat, collision ; `SBX_CER_*` ouverture,
  affectation, anomalie, clôture de campagne ; `SBX_LEC_*` et `SBX_TRA_ALERTE` signalements et alertes ; `SBX_DON_*`
  restauration en échec, clés de test, plages BIN, refus hors plage répétés) fixe titre,
  sous-titre et portée : `INSTITUTION` (comptes de l'institution, lien `/portail`), `GIMAC` (comptes du GIMAC, lien
  `/des/sandbox/espaces/{code}`) ou `MIXTE`. Rôles : `ADMINISTRATEUR_INSTITUTION`, `CHEF_DE_PROJET`, `DEVELOPPEUR`,
  `TESTEUR` (profils `ROLE_SBX_*` des comptes de l'institution) ; `ADMINISTRATEUR_GIMAC` (`SBX_HAB_ADMINISTRATION`),
  `RSSI` (`SBX_LEC_ADMINISTRATION`), `CERTIFICATEUR` (`SBX_CER_VALIDATION`), `EXPLOITANT` ou `EXPLOITATION`
  (`SBX_TRA_ADMINISTRATION`), `PRODUCTION_MONETIQUE` (`SBX_DON_ADMINISTRATION`).
- **Cloisonnement** : un compte nommé n'est retenu que s'il appartient à l'institution de la demande (MS-IAM,
  `GET /api/v1/interne/collaborateurs?institution=` puis `?permission=`, cache de 60 s), sauf l'intervenant désigné
  d'un mandat de partenaire (modèles `SBX_ESP_MANDAT_*`, identifiant de compte) ; un rôle n'est résolu que dans
  la portée du modèle. Le type `SANDBOX` est servi au portail (`TypeNotification.PORTAIL`) ; préférence `SANDBOX`
  (active par défaut) dans « Mon compte ».
- **Sans donnée sensible** : gabarits fixes, seul le code participant (format contrôlé, sinon « ? ») y figure ; jamais
  d'identifiant de connexion, de secret, de donnée de test ni de numéro de carte (test des gabarits).
- **Sans règle** : modèle inconnu mis au rebut (`rebut_notification_sandbox`, motif `MODELE_SANS_REGLE`) et alerte
  d'exploitation journalisée (`ALERTE_EXPLOITATION`) ; MS-IAM injoignable : rejeu puis `<sujet>.dlt`.
- **Résolution à blanc** (NOT-05) : `GET /bff/console/notification/sandbox/modeles` (catalogue) et
  `POST /bff/console/notification/sandbox/resolution` `{ modele, destinataires, codeParticipant }` (destinataires,
  titres et liens qui seraient remis, rien n'est créé), `SBX_HAB_ADMINISTRATION` et second facteur (OPA).
- **Écart (critère 1, courriel)** : la remise est faite sur le canal de la plateforme (notification du portail et du
  DES) ; le courriel minimal renvoyant vers la plateforme attend la lecture des adresses chez MS-IAM (l'annuaire
  interne ne sert aucune donnée de contact) et l'activation du relais SMTP (`gimac.notification.courriel.actif`).

| Critère | Test |
|---|---|
| 1. Invitation du premier administrateur résolue, renvoi vers la plateforme sans identifiant ni secret | `ReglesSandboxTest.invitation`, `ReglesSandboxTest.gabaritsSansDonneeSensible`, `DemandesSandboxKafkaIT.parcours` |
| 2. Redélivrance : aucune seconde remise | `DemandesSandboxConsumerTest.doublon`, `DemandesSandboxKafkaIT.parcours` |
| 3. Demande sans règle au rebut avec son motif, alerte | `NotificationsSandboxServiceTest.sansRegle`, `DemandesSandboxKafkaIT.parcours` |
| Portée, cloisonnement par institution, préférences | `ReglesSandboxTest`, `NotificationsSandboxServiceTest` |
| Résolution à blanc | `NotificationsSandboxServiceTest.simulation`, `DemandesSandboxKafkaIT.resolutionABlanc`, OPA `test_sbx_notifications_regles` |

## 12. Bilan repris, socle du circuit des quitus (US-EXI-25) : réalisé (D-125)

Le circuit existant `CertificationCircuit`/`Quitus` est utilisé, sans module de certification parallèle :

- **Socle** : la relève de §7.4 reprend l'empreinte SHA-256 du bilan dont les empreintes de la liste, du HEAD et des
  octets concordent. Elle l'enregistre dans `EvaluationBase` avec la source, la campagne et la version du bilan. Une
  nouvelle empreinte crée le cycle suivant ; la même empreinte ne crée rien. Une pièce divergente ou absente ne devient
  jamais un socle.
- **Contrat rétrocompatible** : `homologation.campaign-result-imported.v1` reçoit le champ facultatif
  `empreinteBilan`. L'ancien constructeur de `CampaignResultImportedPayload` est conservé et produit une empreinte
  nulle ; un événement historique sans ce champ reste accepté sans effet sur le circuit.
- **Consommation idempotente** : `BilanSandboxConsumer` écoute `homologation.campaign`. Un identifiant CloudEvent déjà
  traité est acquitté sans effet ; une nouvelle livraison de la même empreinte conserve également le circuit et ses
  quitus.
- **Caducité (CER-06)** : une empreinte différente ouvre le cycle suivant, interrompt le circuit remplacé et fait
  passer ses seuls quitus `SIGNE_FAVORABLE` à `CADUC`, avec les deux empreintes dans le motif.
  `certification.quitus-lapsed.v1` est publié une fois par circuit remplacé comportant au moins un favorable.
- **Signature** : `SignedDecisionServiceImpl` n'accepte que l'empreinte du circuit actuellement ouvert. Une signature
  sur un circuit remplacé ou une empreinte différente est refusée en 409 `CER_SOCLE_OUTDATED`, avec `socleAJour`.
- **Aucune écriture vers la Sandbox ou Moabi** : le flux part des lectures GET/HEAD de SBX-LECTURE et continue par
  l'outbox de MS-HOMOLOGATION ; aucune méthode d'écriture n'est ajoutée au port de lecture. Le critère 3 reste tenu
  par la matrice du générateur (§9).

| Vérification | Test |
|---|---|
| Empreinte propagée, socle historisé seulement au changement | `SocleEvaluationSandboxReleveTest`, `ReleveSandboxIT.exi25EmpreinteBilanEtSocle` |
| Même empreinte conservée, changement à `CADUC`, doublon sans effet | `SocleEvaluationSandboxTest`, `BilanSandboxConsumerTest`, `BilanSandboxIT.circuitComplet` |
| Ancien contrat sans empreinte, signature sur ancien socle refusée | `SocleEvaluationSandboxReleveTest.constructeurHistorique`, `BilanSandboxConsumerTest`, `BilanSandboxIT.circuitComplet` |

Les classes EXI ajoutées dans les deux services sont soumises aux seuils JaCoCo de 98 % en lignes, branches et
méthodes, sans relever le seuil global des services. Le 27/10/2026, `TESTCONTAINERS_RYUK_DISABLED=true ./mvnw clean
verify` réussit sur les deux services : 536 tests unitaires et 421 tests d'intégration pour MS-HOMOLOGATION, 219 tests
unitaires et 236 tests d'intégration pour MS-CERTIFICATION ; contrôles JaCoCo globaux et EXI 98 % respectés.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-10-19 | - | Création : principes, identité et matrice des permissions (US-EXI-01, US-EXI-02), ordre de la suite |
| 2026-10-20 | - | §4 : administration déléguée (US-EXI-03), route du portail et règles |
| 2026-10-22 | - | D-121 : supervision de la sandbox hors de MS-MONITORING ; EXI-11, 12, 13, 18, 19 et 22 écartés ; écran dans l'espace sandbox du DES sur SBX-TRACE |
| 2026-10-22 | - | §6 : routes de la sandbox dans MS-GATEWAY, OPA, cloisonnement étendu aux mandats, jeton réduit, quotas, journal (US-EXI-07, D-120) |
| 2026-10-22 | - | §7.1 : états des liaisons publiés pour la sandbox (US-EXI-16, D-122) |
| 2026-10-22 | - | §6.5 : version de statut dans participant.status-changed.v1, contrat lisible par machine (US-EXI-20, D-120) ; §6.3 : activation du jeton réduit |
| 2026-10-22 | - | §7.2 : espace sandbox du DES, « Espaces des institutions » sur SBX-ESPACE (D-118 points 2 et 3, D-122) |
| 2026-10-22 | - | §6.6 : fiche et nomenclature des services de MS-PARTICIPANT pour la sandbox, refus journalisé des écritures (US-EXI-21, D-120) |
| 2026-10-22 | - | §7.3 : gel des liaisons sur changement de statut de l'institution (HOM-06) et republication des états (D-122) |
| 2026-10-22 | - | §8.1 et §8.2 : routes internes du lot IAM, suspension d'institution et protection du dernier administrateur (US-EXI-06, D-119) |
| 2026-10-22 | - | §6.7 : types de pièces, rétention, dépôt contrôlé et document.document-sealed.v1 de la sandbox dans MS-DOCUMENT, pièce par empreinte (US-EXI-08, D-120) |
| 2026-10-22 | - | §2.1 : treizième module SUP (`SBX_SUP_CONSULTATION`) ; §8.3 : mandats de partenaire (US-EXI-04, D-119) |
| 2026-10-23 | - | §7.4 et §7.5 : lecture de SBX-LECTURE et notification de fin de campagne par MS-HOMOLOGATION (US-EXI-14, US-EXI-15) ; §7.6 : onglet « Résultats de la sandbox » du dossier de liaison (US-EXI-17) |
| 2026-10-23 | - | §8.4 : accès de prestataire de maintenance (US-EXI-24, D-123) ; §9 : comptes de service de la sandbox et matrice des appels synchrones (US-EXI-05, D-123) |
| 2026-10-23 | - | §8.5 : verrouillage et déblocage des comptes de la sandbox (US-EXI-23) ; §10 : événements à effet dans MS-AUDIT (US-EXI-10) ; §11 : notifications de la sandbox dans MS-NOTIFICATION (US-EXI-09) ; §12 : US-EXI-25 en attente (D-124) |
| 2026-10-27 | - | §8.1 : sagas OUVERTURE et MATRICE ; §8.5 : campagne trimestrielle de réexamen, décisions, scellement, journal et onglet console (US-EXI-23 critère 2) ; raccordement OPA des écritures BFF signalé au lot passerelle |
| 2026-10-27 | - | §12 : empreinte du bilan dans le socle d'évaluation, caducité des quitus et refus des signatures sur ancien socle (US-EXI-25, D-125) |
