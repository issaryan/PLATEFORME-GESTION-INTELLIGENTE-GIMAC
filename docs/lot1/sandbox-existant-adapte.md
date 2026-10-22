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
| US-EXI-16 | Fait (D-122) | Consommation par SBX-CAMPAGNE et SBX-LECTURE à leur arrivée (projection de référence en test de contrat) |

## 6. MS-HOMOLOGATION et MS-CERTIFICATION (lot homologation, D-122)

### 6.1 États des liaisons publiés pour la sandbox (US-EXI-16)

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
- **À faire hors de ce lot** : droits de lecture du sujet `homologation.liaison` pour les comptes de service `sbx-campagne` et `sbx-lecture` ; le gel d'une liaison sur changement de statut de l'institution (HOM-06) n'existe pas encore.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-10-19 | - | Création : principes, identité et matrice des permissions (US-EXI-01, US-EXI-02), ordre de la suite |
| 2026-10-20 | - | §4 : administration déléguée (US-EXI-03), route du portail et règles |
| 2026-10-22 | - | D-121 : supervision de la sandbox hors de MS-MONITORING ; EXI-11, 12, 13, 18, 19 et 22 écartés ; écran dans l'espace sandbox du DES sur SBX-TRACE |
| 2026-10-22 | - | §6.1 : états des liaisons publiés pour la sandbox (US-EXI-16, D-122) |
