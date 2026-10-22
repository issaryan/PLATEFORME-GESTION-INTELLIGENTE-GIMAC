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

### 2.2 Valeurs de départ

Valeurs configurables ensuite depuis la console (écran des profils, D-86). Les fonctions du GIMAC s'ajoutent aux profils
existants ; les rôles de portée institution sont six profils externes `ROLE_SBX_*` du portail. Le prestataire de
maintenance passe par un accès temporaire par délégation (US-EXI-24), pas par un profil.

| Rôle de la sandbox (UC-INS-04) | Profil | Permissions SBX de départ |
|---|---|---|
| Administrateur GIMAC | ROLE_SUPER_ADMIN | ADMINISTRATION et CONSULTATION de tous les modules |
| Responsable de produit (chef du DES) | ROLE_CHEF_DES | CONSULTATION de tous les modules |
| Ingénieur certificateur | ROLE_SNA | CER : EXECUTION, VALIDATION, CONSULTATION ; SIM, MOB, ISO, CAR : SIMULATION, CONSULTATION ; RAC, REF, SCN, DON, TRA, LEC : CONSULTATION |
| RSSI | ROLE_RSSI | RAC, DON, TRA, LEC : ADMINISTRATION ; CONSULTATION de tous les modules |
| Production monétique | ROLE_PRODUCTION_MONETIQUE | CAR, DON, TRA : ADMINISTRATION ; CAR, DON : SIMULATION ; RAC, REF, TRA, CAR, DON, SIM, CER, LEC : CONSULTATION |
| Exploitant | ROLE_ADMIN_SYSTEME | TRA : ADMINISTRATION, CONSULTATION ; RAC, SIM, CER, LEC : CONSULTATION |
| Administrateur de l'institution | ROLE_SBX_ADMIN_INSTITUTION (externe) | HAB : ADMINISTRATION, CONSULTATION ; RAC, CER, TRA : CONSULTATION |
| Chef de projet participant | ROLE_SBX_CHEF_PROJET (externe) | CER : EXECUTION, CONSULTATION ; SIM, MOB, ISO, CAR : SIMULATION, CONSULTATION ; RAC, DON, SCN, TRA : CONSULTATION |
| Développeur du participant | ROLE_SBX_DEVELOPPEUR (externe) | RAC : ADMINISTRATION, CONSULTATION ; DON, SIM, MOB, ISO, CAR : SIMULATION, CONSULTATION ; SCN, TRA : CONSULTATION |
| Testeur du participant | ROLE_SBX_TESTEUR (externe) | CER, SIM, MOB, ISO, CAR : EXECUTION, CONSULTATION ; TRA : CONSULTATION |
| Lecteur du participant | ROLE_SBX_LECTEUR (externe) | CER, RAC, SIM, MOB, ISO, CAR : CONSULTATION |
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
| 1 | MS-IAM et Keycloak | EXI-01, 02, 03 (faits), 04, 05, 06, 23, 24 |
| 2 | Passerelle et OPA | EXI-07 |
| 3 | MS-PARTICIPANT | EXI-20, 21 |
| 4 | MS-DOCUMENT, MS-NOTIFICATION, MS-AUDIT | EXI-08, 09, 10 |
| 5 | Supervision de la sandbox (D-121) | EXI-11, 12, 13, 18, 19, 22 écartés : aucune adaptation de MS-MONITORING, MS-INCIDENT ni MS-REPORTING ; écran « Supervision de la sandbox » de l'espace sandbox du DES, sur SBX-TRACE, permission dédiée |
| 6 | MS-HOMOLOGATION et MS-CERTIFICATION | EXI-14, 15, 16, 25 |
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
- **À faire hors de ce lot** : droits de lecture du sujet `homologation.liaison` pour les comptes de service `sbx-campagne` et `sbx-lecture` (lot IAM). Le gel sur changement de statut de l'institution (HOM-06) est réalisé au §7.3.

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
- **Besoin du lot IAM** : la permission `INTERNAL_HOMOLOGATION_REPUBLISH` sur le compte de service d'exploitation qui
  joue la republication.

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
| 2026-10-22 | - | §7.3 : gel des liaisons sur changement de statut de l'institution (HOM-06) et republication des états (D-122) |
