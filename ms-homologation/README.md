<!-- GIMAC:DEBUT : section du projet, tenue à jour à chaque modification ; la documentation JHipster suit. -->
# MS-HOMOLOGATION : plateforme DES du GIMAC

Suivi de l'homologation des participants : référentiel d'opérations versionné, liaisons, instanciation des opérations par tri topologique, règle des quatre éléments, reprise en lecture seule du simulateur, couvertures, aptitude à la certification.

| Élément | Valeur |
|---|---|
| Lot | Lot 1, module de gestion de la certification (PI-GIMAC-01 v1.0, `docs/lot1`) |
| Récits | HOM-01 à HOM-24 |
| Port | 8086 |
| Paquet Java | `cm.gimac.des.homologation` |
| Modèle | `ms-homologation.jdl` (JDL du plan, §0.2) ; JHipster 9.4.0 ; Cucumber |
| Entités (18) | ReferentialVersion, ReferenceOperation, Liaison, ContractualScope, ScopeItem, OperationInstance, Assignment, EvidenceReference, ClosureRecord, CampaignResult, UnmatchedScenario, Coverage, EvaluationBase, EvidenceFileExport, ReplayCursor, RejectedRecord, ReferentialMigration, DeadlineNotification |
| Règles de gestion (REC-GIMAC-01 §5) | RG-26 à RG-33, RG-72 |
| Correspondances avec le plan | `docs/lot1/correspondance.md` |

## Architecture (PI §0.8)

- `domain/` : entités JPA générées, `domain/service` (services de domaine purs), `domain/event` (charges utiles).
- `application/` : ports entrants et sortants, cas d'utilisation transactionnels.
- `infrastructure/` : outbox et relais CloudEvents, consommateur idempotent, adaptateurs réels et de substitution (`gimac.adapters.*`), jeton de service, contexte de corrélation.
- `web/rest/` : ressources générées (`/api/<entités>`) et ressources du lot (`/api/v1/...`, exposées par la passerelle sous `/bff/des/homologation` et `/bff/portail/homologation`).
- Test d'architecture : couches `application` et `infrastructure`, services de domaine sans Spring.

## Cadrage : étape 3 du parcours (D-57, D-58)

La liaison s'ouvre au cadrage sur une institution **adhérente** (ou déjà en homologation, pour une nouvelle liaison sans repasser par l'adhésion, rang 35). Un **chef de département** (chef du DES ou de la DDRM : `HOM_LIAISON_OPEN` et `HOM_PROJECT_DESIGNATE`) l'ouvre en désignant le **conducteur de projet** parmi les détenteurs du rôle `CONDUCTEUR_PROJET`, actifs et internes au GIMAC ; la banque partenaire éventuelle doit être une banque. La liaison est liée à la version publiée en vigueur du référentiel ; son **périmètre contractualisé** est dérivé des services souscrits (HOM-05), le service `TRANSVERSE` toujours inclus. L'événement `homologation.liaison-opened.v1` (sujet `homologation.liaison`) fait passer l'adhérent à `EN_HOMOLOGATION` dans MS-PARTICIPANT ; la redésignation publie `homologation.liaison-lead-designated.v1`. Depuis D-104, une institution peut conduire plusieurs liaisons actives, y compris du même type de projet (lots successifs, nouveaux services, périmètres différents) : l'index `uk_liaison_active` est retiré ; un intitulé libre (`intituleProjet`) les distingue et une référence d'origine facultative (`referenceOrigine`, reprise d'un outil antérieur) reste unique (409 `HOM_LIAISON_REFERENCE_ORIGINE_DUPLICATE`).

| Méthode et chemin (`/api/v1`, sous `/bff/des/homologation`) | Permission | Objet |
|---|---|---|
| `GET /liaisons?etat=&typeProjet=&institutionId=&recherche=&mesProjets=&page=&taille=` | `HOM_LIAISON_LIST` ou `HOM_LIAISON_READ` | Liste `{contenu, total}` ; `mesProjets` : liaisons confiées au conducteur connecté |
| `GET /liaisons/{id}` | `HOM_LIAISON_READ` | Fiche : phase, périmètre, intervenants, actions permises |
| `GET /liaisons/conducteurs` | `HOM_PROJECT_DESIGNATE` ou `HOM_LIAISON_OPEN` | Conducteurs désignables |
| `POST /liaisons` | `HOM_LIAISON_OPEN` et `HOM_PROJECT_DESIGNATE` | Ouverture (201, 403, 409, 503) |
| `PUT /liaisons/{id}/chef-de-projet` | `HOM_PROJECT_DESIGNATE` | Redésignation motivée |

Ports sortants : `ParticipantDirectoryPort` (interface interne `/api/v1/interne/institutions/{id}` de MS-PARTICIPANT) et `CollaboratorDirectoryPort` (`/api/v1/interne/collaborateurs` de MS-IAM), jeton de service ; adaptateurs réels en profil prod, de substitution en développement et en test (`gimac.adapters.participant-reader`, `collaborator-reader`). Un annuaire injoignable donne 503, jamais un statut supposé.

Sécurité (D-61) : `/api/v1/**` exige un jeton à deux facteurs (`acr` 2 ou 3) ; le CRUD généré `/api/**` est réservé à `ROLE_ADMIN`.

## Parcours de la liaison : étapes 4 à 10 (D-56 à D-59)

Part de MS-HOMOLOGATION fixée par `docs/lot1/parcours-backend.md` ; les formes sont celles de `frontend/packages/lot1/src/modele/parcours.ts`. Depuis D-77 (`docs/lot1/modelisateur-processus.md`), **le référentiel publié fait foi** : un seul moteur, qui lit la version de la liaison.

- **Référentiel publié** : à l'ouverture, la liaison est rattachée à la version publiée de son type et ses opérations (hors étapes 1 et 2, tenues par MS-PARTICIPANT) sont **instanciées depuis cette version**, modèle recopié dans `parcours_operation` (libellé, geste, jalon, déclencheur, particularité, livrables, délai, responsable de référence, `version_id`) ; `parcours_liaison` porte `version_id` et `version_numero`. Échéances au plus tôt sur le graphe depuis le démarrage. Une liaison ouverte avant cette tranche l'est à la première lecture. Référence lisible `LIA-AAAA-NNN`.
- **Version 1** : l'annexe A du CR-CERT-GIMAC-01 v1.4 (`config/liquibase/data/parcours_referentiel_cr.csv`, libellés `libelles_operations_cr.csv`) n'est plus lue par le moteur ; la reprise `RepriseReferentielCr` (Liquibase, une fois, hors contexte `test`) l'a convertie par `ConversionAnnexeA` en version publiée de chaque type : carte 43 opérations (tronc 33, branche C1 à C10), paiement mobile 49 (tronc 32 sans le rang 5, branche M1 à M17), délégataire 67 (tronc 33, branche D1 à D34) ; R le premier porteur, C les autres internes, E les externes, V le conducteur (le chef du DES pour le conducteur), I le chef de la DDRM ; rang 27 rattaché explicitement à la fin de la branche ; gestes, jalons, déclencheurs et particularités des rangs autrefois codés en dur ; un livrable par opération de grain PHASE ou pivot.
- **Agrégat** `domain/parcours/Parcours` (sans Spring) : graphe des prédécesseurs (`VERROUILLEE`, `A_FAIRE`, `EN_VALIDATION`, `FAITE`, `BLOQUEE`), qualités RACI + E (le validateur n'est jamais le responsable), échéances et SLA (proche à 5 jours), étape courante et statut du CR (`CADREE` … `EN_PRODUCTION`) projetés sur l'automate technique de la liaison (`OUVERTE` … `CLOSE`), couvertures de certification et de mise en service par service, blocages datés, fiche de suivi, projection des verrous de MS-CERTIFICATION. Projections et tableaux de bord : `domain/service/ParcoursProjection`, `TableauDeBordCalculateur`.
- **Preuves** : une pièce déposée par l'interface dans MS-DOCUMENT est rattachée par `documentId` et `empreinte`, **revérifiées** auprès de MS-DOCUMENT (`HOM_DOCUMENT_UNKNOWN`, `HOM_DOCUMENT_HASH_MISMATCH`) ; une clé (C1, D3, D12, D27 en version 1) ne porte que des codes de contrôle (4 à 8 hexadécimaux), une opération de particularité `CEREMONIE_CLES` (C1) sa cérémonie ; un secret (29, M3, M7, M9, D2) une trace ; toute propriété hors contrat est refusée (`HOM_EVIDENCE_FIELD_REFUSED`). Validation après réauthentification forte (`X-Reauth-Assertion` vérifiée par MS-IAM, 428 `IAM_REAUTH_REQUIRED`, 403 `HOM_REAUTH_INVALID`).
- **Couverture** : un service est certifié à la fin des essais (particularité `FIN_ESSAIS`, rang 16 en version 1) ; si le simulateur a été repris (table `coverage`, nature `CERTIFICATION`, HOM-12), seul un service mesuré à 100 % l'est. Le constat (geste `CONSTATER_COUVERTURE`, rang 17) publie `homologation.coverage-established.v1`.

| Méthode et chemin (`/api/v1`) | Permission | Objet |
|---|---|---|
| `GET /liaisons/{id}/dossier` | `HOM_LIAISON_READ` | Dossier numérique : périmètre, deux couvertures, onze étapes à six éléments, blocages, actions permises |
| `GET /liaisons/{id}/parcours/operations?etape=&fonction=` | `HOM_LIAISON_READ` | Opérations instanciées, RACI + E, plateforme, grain, SLA, preuve, actions permises |
| `PUT /liaisons/{id}/ressources` | `HOM_STEP_MANAGE` | Affectation des ressources (rang 6) |
| `POST /parcours/operations/{id}/livrable` | `HOM_STEP_MANAGE` ou `HOM_EVIDENCE_SUBMIT` | Livrable (clôt l'opération) |
| `POST /parcours/operations/{id}/preuves` | `HOM_EVIDENCE_SUBMIT` | Preuve (valeur de référence, code de contrôle, cérémonie ; jamais de secret) |
| `POST /parcours/operations/{id}/validation` | `HOM_OPERATION_VALIDATE` + FIDO2 | Validation par V, jamais par R |
| `PUT /parcours/operations/{id}/intervenants` | `HOM_OPERATION_ASSIGN` ou `HOM_STEP_MANAGE` | Qualités R, C, V, I, E |
| `POST /liaisons/{id}/blocages` · `POST /blocages/{id}/levee` | `HOM_STEP_MANAGE` | Blocage daté, levée |
| `POST /liaisons/{id}/couverture/constat` | `CER_DOSSIER_VERIFY` | Constat de la couverture (rang 17) |
| `POST /liaisons/{id}/services-supplementaires` | `HOM_LIAISON_OPEN` ou `HOM_STEP_MANAGE` | Nouvelle liaison au cadrage pour un service supplémentaire (rang 35), hors unicité, conducteur à désigner |
| `GET /liaisons/{id}/fiche-suivi` · `POST …/remise` · `POST …/constat` | `HOM_LIAISON_READ` · `HOM_STEP_MANAGE` · `CER_DOSSIER_VERIFY` | Fiche de suivi des services, constat de mise en service (rangs 32, 33) |
| `GET /tableaux-de-bord/acteur?fonctions=` · `GET /tableaux-de-bord/pilotage` | `HOM_DASHBOARD_READ` | Tableau de bord par fonction (seules les fonctions des profils `ROLE_*` du jeton) ; pilotage : 4 compteurs, répartition, délais, 6 questions de l'annexe B |
| `GET /intervenants?fonction=` | `HOM_LIAISON_READ`, `HOM_OPERATION_ASSIGN` ou `HOM_STEP_MANAGE` | Personnes d'une fonction (annuaire de MS-IAM) |
| `GET /portail/liaisons` · `GET /portail/liaisons/{id}/dossier` · `GET /portail/taches` · `GET /portail/liaisons/{id}/fiche-suivi` | `HOM_PORTAL_READ` | Portail (`/bff/portail/homologation`) : institution du jeton, projection sans nom interne, tâches avec leur geste et leur écran |
| `PUT /portail/liaisons/{id}/coordonnees-techniques` · `POST /portail/operations/{id}/livrable` (multipart) · `PUT /portail/liaisons/{id}/fiche-suivi` | `HOM_PORTAL_SUBMIT` | Coordonnées d'essai (rang 12, hôte et port contrôlés), livrable déposé dans MS-DOCUMENT pour le compte du participant, fiche renseignée (rang 32) |
| `GET /interne/liaisons/{id}/contexte-certification` | `INTERNAL_HOMOLOGATION_READ` (jeton de service) | Contexte servi à MS-CERTIFICATION (§2) |

La liste `GET /liaisons` sert en outre `reference`, `statut`, `etapeCourante`, `bloquee`, `prochaineEcheance`, `dateCibleProduction`, `liaisonOrigineId` et les couvertures réelles ; elle se filtre par `statut`, `etape` (liste), `bloquees` et `sansConducteur`. Le portail exige l'institution et le second facteur ; une liaison d'une autre institution est introuvable (404).

Événements : publié `homologation.coverage-established.v1` (sujet `homologation.liaison`, extension `institutionid`). Consommés (groupe `homologation-certification`, sujets `certification.proces-verbal` et `certification.approbation`, idempotents, rebut `.dlq`) : chaque sujet clôt les opérations dont il est le **déclencheur** (en version 1 : `pv-established.v1` rang 18, `pv-signed.v1` rang 19 et jalon `CERTIFIEE`, `change-record-constituted.v1` rang 20, `change-record-referenced.v1` rang 26 s'il est ouvert, `attestations-archived.v1` rangs 21 et 22, `security-approved.v1` rangs 23 à 25 et jalon `APPROUVEE_SECURITE`) ; tolérés pour les compteurs : `attestation-deposited.v1`, `attestation-verified.v1`, `visas-required.v1`, `visa-signed.v1`.

Ports sortants ajoutés : `DocumentRegistryPort` (MS-DOCUMENT, `/api/v1/interne/documents{,/{id}/empreinte}`, `gimac.adapters.document-deposit`) et `StrongReauthPort` (MS-IAM, `/api/v1/interne/reauthentifications/verification`, `gimac.adapters.reauthentication`), adaptateurs réels et de substitution.

## Modélisateur de processus et tâches (D-77)

Contrat : `docs/lot1/modelisateur-processus.md`. Une opération du référentiel porte, en plus des attributs de couverture (HOM-01, dérivés du modèle quand l'étape est saisie) : `description`, `etape`, `ordre`, `responsable`, `validateur`, `consultes`, `informes`, `externes`, `delaiJours` (0 à 365), `grain`, `plateforme`, `cle`, `secret`, `oubliSansSymptome`, `pivot`, `valeurReference` (libellé, jamais avec `secret`), `livrables` (jsonb), `geste`, `jalon`, `declencheur`, `position` (jsonb) et `particularite` (extension : `ENTREE_ESSAIS`, `FIN_ESSAIS`, `CEREMONIE_CLES`, `SERVICE_SUPPLEMENTAIRE`). Les vues servent le segment renommé `AMONT`, `BRANCHE`, `AVAL` (les deux formes sont admises en saisie). Contrôles de publication ajoutés : `RESPONSABLE_ABSENT`, `VALIDATEUR_ABSENT`, `VALIDATEUR_EGAL_RESPONSABLE`, `DECLENCHEUR_INCONNU`, `JALON_DUPLIQUE`, `JALON_DESORDONNE`, `SANS_ENTREE`, `LIVRABLE_DUPLIQUE`, `SECRET_AVEC_VALEUR`.

| Méthode et chemin (`/api/v1/referentiels`) | Permission | Objet |
|---|---|---|
| `POST /` (`piloteId` facultatif) | `HOM_REFERENTIEL_EDIT` | Brouillon ou dérivation ; le créateur pilote par défaut, tâche au pilote |
| `POST /{id}/operations` (code dans le corps) · `PUT /{id}/operations/{code}` · `DELETE /{id}/operations/{code}` | `HOM_REFERENTIEL_EDIT` | Création (409 `HOM_OPERATION_CODE_DUPLICATE`), écriture complète, retrait (le code quitte les prédécesseurs des autres) |
| `PUT /{id}/positions` · `PUT /{id}/arcs` | `HOM_REFERENTIEL_EDIT` | Placement `[{code, x, y}]` ; arc `{de, vers, present}` (409 `HOM_REFERENTIAL_CYCLE` s'il fermerait un cycle) |
| `PUT /{id}/pilote` | `HOM_REFERENTIEL_EDIT` | Pilote `{compteId}` : le demandeur ou un compte actif d'un profil `gimac.homologation.referentiel.profils-pilote` (422 `HOM_PILOTE_NOT_ELIGIBLE`) |
| `GET /{id}/graphe` | `HOM_REFERENTIEL_READ` | Graphe étendu : opérations complètes, couches, début et fin au plus tôt, chemin critique, anomalies par nœud, attributs manquants |
| `GET /catalogue` | `HOM_REFERENTIEL_READ` | Fonctions (libellés, profils), gestes, jalons, déclencheurs, étapes, segments, plateformes, grains, natures, particularités, types de pièces, pilotes possibles |

**D-89** (`docs/lot1/referentiel-droits-etapes.md`) : tout éditeur modifie un brouillon (le pilote est affiché et notifié, sans verrou ; un brouillon annonce `MODIFIER` dans `actionsPermises`). Étapes configurables par version (`referential_etape`) : `GET /{id}/etapes` (`HOM_REFERENTIEL_READ`), `POST /{id}/etapes`, `PUT /{id}/etapes/{code}`, `PUT /{id}/etapes/ordre`, `DELETE /{id}/etapes/{code}` (`HOM_REFERENTIEL_EDIT`, brouillon seulement ; 409 `HOM_ETAPE_CANONIQUE`, `HOM_ETAPE_NON_VIDE`). Lecture filtrée (`VisibiliteReferentiel`) du graphe, des listes et de l'export `GET /{id}/export` (CSV) : lecteur sans `HOM_REFERENTIEL_READ_ALL` ni droit d'édition, ses opérations et leurs voisines en fantômes (`filtre`, `fantome`, en-tête `X-Gimac-Filtre`).

Toute écriture sur une version publiée ou retirée : 409 `HOM_REFERENTIAL_FROZEN`. La **migration** d'une liaison (`HOM_REFERENTIEL_MIGRATE`) applique le différentiel à son parcours. Les **tâches de MS-PILOTAGE** (pilote du brouillon, conducteur à l'ouverture et à la désignation, R à l'éligibilité, V à la validation demandée, clôture et réouverture) sont écrites dans `demande_tache_pilotage` dans la transaction du geste et relayées par `RelaisTachesPilotage` avec réessai croissant (port `PilotageTachesPort`, `gimac.adapters.pilotage-tasks` : `real` en profil `prod`, jeton `homologation-service` ; `gimac.services.pilotage.url`).

## Sandbox GIMAC (épopée EXI, D-122)

Contrat : `docs/lot1/sandbox-existant-adapte.md` §6. **US-EXI-16** : `homologation.liaison-opened.v1` porte en plus `codeParticipant` et `versionEtat` (et part aussi à l'ouverture d'un service supplémentaire) ; `homologation.liaison-status-changed.v1` (sujet `homologation.liaison`) part à l'ouverture puis à chaque passage à `OUVERTE`, `GELEE` ou `CLOSE` vu par la sandbox, avec une version d'état strictement croissante (`application/usecase/sandbox/PublicationEtatsLiaison`, table `liaison_sandbox`, changeset `20261022000100`). Schémas : `src/main/resources/contrats/sandbox/`. Tests : `PublicationEtatsLiaisonTest`, `EtatLiaisonSandboxTest`, `ContratEvenementsLiaisonSandboxTest` (schémas, sujet, projection de référence de SBX-CAMPAGNE, revue d'absence d'écriture de la sandbox), `EtatsLiaisonSandboxIT` (Kafka). **HOM-06 et republication** : `StatutsInstitutionConsumer` (sujet `participant.status`) et `GelLiaisonsInstitution` gèlent les liaisons d'une institution `SUSPENDU` ou `RETIRE` et les rétablissent à son retour (table `institution_statut_applique`, changeset `20261022000200`, ordre par version de statut puis date d'effet) ; `POST /api/v1/interne/sandbox/liaisons/republication` (`INTERNAL_HOMOLOGATION_REPUBLISH`) republie l'état courant de chaque liaison (`RepublicationEtatsLiaison`). Tests : `StatutInstitutionAppliqueTest`, `GelLiaisonsInstitutionTest`, `StatutsInstitutionConsumerTest`, `RepublicationEtatsLiaisonTest`, `GelLiaisonsSandboxIT`. Couverture : exécution JaCoCo `seuil-sandbox-exi` à 98 % (lignes, branches, méthodes) sur les classes de la sandbox.

### US-EXI-25 : socle du bilan repris (D-125)

La relève enregistre dans `EvaluationBase` l'empreinte SHA-256 d'un bilan dont les lectures GET, HEAD et les octets concordent, avec sa source, sa campagne et sa version. Elle propage `empreinteBilan`, champ facultatif de `homologation.campaign-result-imported.v1` : un événement historique sans empreinte reste accepté sans ouvrir de cycle. Une pièce absente ou divergente ne devient jamais un socle ; aucune écriture vers la Sandbox n'est ajoutée. Le détail du contrat reste dans `docs/lot1/sandbox-existant-adapte.md` §12.

Validation du 27/10/2026 avec `TESTCONTAINERS_RYUK_DISABLED=true ./mvnw clean verify` : 536 tests unitaires et 421 tests d'intégration réussis ; contrôles JaCoCo globaux et EXI à 98 % respectés. Les preuves couvrent Testcontainers et les contrats avec SBX-LECTURE simulé, pas un SBX-LECTURE GIMAC réel ni un Compose complet.

## Tests : définition de fini (D-61)

Unitaires (`LiaisonOpeningPolicyTest`, `OpenLiaisonServiceTest`, `LiaisonQueriesTest`, `AdaptateursCadrageTest`, `StrongAuthenticationAuthorizationManagerTest` ; parcours : `ParcoursTest`, `ParcoursProjectionTest`, `TableauDeBordCalculateurTest`, `ParcoursCommandesTest`, `ParcoursLecturesTest`, `ParcoursInstanciationTest`, `ParcoursDepotTest`, `IntervenantsTest`, `EvenementsCertificationTest`, `EvenementsCertificationConsumerTest`, `AdaptateursParcoursTest` ; D-77 : `ConversionAnnexeATest` (fidélité de la version 1 à l'ancien chemin du CSV, type par type), `ModeleParcoursTest`, `ReferentialModelisateurTest`, `ReferentialModelisateurServiceTest`, `SuiviTachesParcoursTest`, `RelaisTachesPilotageTest`, `AdaptateursPilotageTest`), sécurité (`LiaisonsCadrageResourceSecurityTest`, `ParcoursResourceSecurityTest`, `TableauxDeBordResourceSecurityTest`, `PortailParcoursResourceSecurityTest`, `ContexteCertificationResourceSecurityTest`), intégration (`CadrageLiaisonIT`, `ParcoursConduiteIT`, `ParcoursVerrousIT`, `ParcoursPortailIT`, `TableauxDeBordIT`, `ModelisateurIT`, `MoteurUniqueIT` (reprise et migration), `TachesParcoursIT` sur PostgreSQL et Kafka), recette (`cadrage.feature`, `parcours.feature` dans `RecetteGherkinIT`). Seuils JaCoCo à 80 % : à `test`, classes du cadrage en lignes et méthodes, l'ensemble en branches ; à `verify`, tout le service en lignes et en fonctions hors MapStruct. Les scénarios du lot effacent leurs données après chaque test (les tests générés comptent les lignes).

## Lancer

- Avec la plateforme : `infra/docker-compose/gimac-dev.sh demarrer`, puis `infra/docker-compose/scripts/verifier-phase0.sh`.
- Image (Jib) : `./mvnw -ntp -Pprod verify jib:dockerBuild`.

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-28 | 76e838e | Génération JHipster 8.11.0 du squelette depuis le JDL ; compilation Maven vérifiée. |
| 2026-09-28 | 9f2250a, c0b91ee | Intégration à `infra/docker-compose` et `infra/kubernetes` générés par JHipster ; cache Valkey (D-25). |
| 2026-09-28 | — | Ajout de cette section. |
| 2026-09-28 | — | Reprise de 3 classes du projet afriland (D-52) ; compilation vérifiée. |
| 2026-09-28 | — | Écarts comblés (liaison au certificat, masquage Luhn 6+4) et 2 classes reprises d'afriland : ServiceTokenProvider, TransitionInterditeException (D-53). |
| 2026-09-29 | — | Masquage des journaux activé (logback, `%maskedMsg`) ; 103 tests unitaires et 386 tests d'intégration réussis, image Jib construite (D-54). |
| 2026-09-29 | — | Étape 3, cadrage (D-57, D-58) : ouverture d'une liaison sur un adhérent par un chef de département, conducteur de projet désigné, périmètre dérivé des services souscrits, événements ; second facteur exigé sur `/api/v1/**`, CRUD généré réservé à l'administration ; garde d'immuabilité du CRUD des opérations appliquée à la version réelle ; seuils JaCoCo. 165 tests unitaires et de sécurité, 365 d'intégration et de recette réussis. |
| 2026-09-29 | — | Lot 1, Phase 0 : régénération depuis le JDL du plan en JHipster 9.4.0, corrections Liquibase (jsonb, unicités, index, immuabilité), messagerie (outbox, relais, consommateur idempotent, rebut `.dlq`), jeton de service, sécurité du portail, arborescence hexagonale (D-55). |
| 2026-09-29 | — | Ports sortants `application/port/out` versionnés : la règle `out/` du `.gitignore` généré par JHipster les ignorait, le dépôt distant ne compilait pas ; exception `!src/**/out/` ajoutée et ports `ParticipantDirectoryPort`, `CollaboratorDirectoryPort` reconstitués d'après leurs adaptateurs |
| 2026-09-30 | — | Parcours de la liaison, étapes 4 à 10 (D-56 à D-59) : référentiel de l'annexe A chargé et instancié à l'ouverture, dossier numérique, opérations, ressources, livrables et preuves revérifiés auprès de MS-DOCUMENT, validation FIDO2, qualités, blocages, constat de la couverture (`homologation.coverage-established.v1`), service supplémentaire, fiche de suivi, tableaux de bord par fonction et pilotage, portail cloisonné, contexte interne pour MS-CERTIFICATION, consommation idempotente des événements `certification.*`. |
| 2026-10-08 | à venir | Modélisateur de processus et moteur unique (D-77) : modèle complet de l'opération (colonnes typées, livrables et position en jsonb), contrôles de publication du modèle, routes du modélisateur (opérations, positions, arcs, graphe étendu avec chemin critique, catalogue, pilote), instanciation depuis la version de la liaison sans aucun rang codé en dur, échéances au plus tôt sur le graphe, migration différentielle, reprise en version 1 publiée par type (`RepriseReferentielCr`), tâches de MS-PILOTAGE relayées avec réessai. |
| 2026-10-06 | à venir | Référentiel des opérations (D-89) : droits d'édition élargis (brouillon modifiable par tout éditeur, `MODIFIER` annoncé), lecture filtrée par fonction avec fantômes (graphe, liste, fiche, export CSV), étapes configurables par version (table, reprise des onze étapes, routes, étape canonique pour la progression, copie à la dérivation). |
| 2026-10-13 | D-104 | index `uk_liaison_active` retiré (plusieurs liaisons actives par institution et type) ; colonnes `intitule_projet` et `reference_origine` (unique si renseignée, 409 `HOM_LIAISON_REFERENCE_ORIGINE_DUPLICATE`), recherche étendue, champs servis au DES et au portail (sans la référence d'origine) |
| 2026-10-13 | - | D-106 : étape `ADHESION` affichée « Procédure administrative » (changeset `20261015000300`) ; délai maximal des opérations (`reference_operation.delai_max_jours`, 422 `HOM_OPERATION_DELAI_MAX_INVALIDE`), échéances cible et maximale des opérations de liaison (`parcours_operation.echeance_cible`), SLA (dépassé au maximum, proche à la cible), `finAuPlusTotMax` du graphe, colonne `delaiMaxJours` de l'export (changeset `20261015000400`) |
| 2026-10-13 | D-107 | D-107 : `LiaisonOpeningPolicy` admet ACTIF ; `POST /api/v1/interne/reprise/liaisons` et `/api/v1/reprise/liaisons` (`REF_REPRISE_IMPORT`) : import idempotent par référence d'origine, avancement à qualifier, détaillé ou en production, preuve « reprise », projection recalculée par le moteur, simulation par défaut ; changeset `20261016000100` (`origine_etat`, `reprise_le`) |
| 2026-10-22 | - | D-122, US-EXI-16 : états des liaisons publiés pour la Sandbox GIMAC (`liaison-status-changed.v1`, code participant, version d'état), règle JaCoCo à 98 % |
| 2026-10-22 | - | D-122, HOM-06 : gel et rétablissement des liaisons sur changement de statut de l'institution ; route interne de republication des états pour la sandbox |
| 2026-10-23 | - | D-122, US-EXI-14, US-EXI-15, US-EXI-17 : client du contrat de SBX-LECTURE, relève périodique, consommateur `sbx-lecture.campaign-published.v1` (groupe `homologation-fin-campagne-sandbox`), résultats de la sandbox d'une liaison ; règle JaCoCo à 98 % étendue |
| 2026-10-27 | D-125 | US-EXI-25 : empreinte du bilan dans `EvaluationBase`, propagation rétrocompatible par `homologation.campaign-result-imported.v1` ; `clean verify` : 536 tests unitaires et 421 d'intégration, seuils globaux et EXI 98 % |

### Mise a jour du 02/11/2026 : Fiabilisation de la releve de la sandbox et depot documentaire

- Depot des pieces de preuve dans MS-DOCUMENT : correction de DocumentRestAdapter en renseignant deposantId avec service-account-homologation-service lorsque deposantId est nul, levant le rejet DOC_DEPOSITOR_INVALID (HTTP 422).
- Securisation du scheduler : encadrement de releve.releverTout() dans ReleveSandboxScheduler par try/catch avec trace detaillee pour eviter le blocage de l executeur de planification en cas d exception imprevue.
- Suppression du composant KafkaProducer parasite pour assainir les journaux et eviter les erreurs KafkaSendFailureException en boucle.
- Desactivation de Spring Docker Compose en profil prod (spring.docker.compose.enabled: false).

<!-- GIMAC:FIN -->

# homologation

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

To build the final jar and optimize the homologation application for production, run:

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
| 2026-10-01 | — | `GET /api/v1/referentiels` : paramètre `type` facultatif ; sans lui, toutes les versions (écran DES-12, qui les regroupe par type, recevait un 400) ; test `listeAvecEtSansType` |
| 2026-10-13 | D-105 | Annexe A du CR v1.7 : fichiers `parcours_referentiel_cr_v17.csv` et `libelles_operations_cr_v17.csv` (les fichiers v1.4, chargés par des changesets appliqués, restent figés), `ConversionAnnexeA.Edition` ; changelog `20261015000500_annexe_a_cr_v17.xml` : libellés des étapes 3 et 4, `ROLE_AGENT_SSI` dans la fonction RSSI, reprise idempotente `RepriseReferentielCrV17` (version v1.7 publiée par type, liaisons non closes rattachées sans changement d'exécution, R des opérations à faire passé à la nouvelle fonction) ; jalon `APPROUVEE_SECURITE` sur le rang 24 ; ouverture d'une liaison par le chef de la DDRM seul (domaine Keycloak) |
| 2026-10-15 | D-110 | `AvancementParcours` : avancement global pondéré par étape, servi dans `LiaisonResume` et le dossier (`avancement`) ; essais au taux mesuré (`ESSAIS_GLOBAL`, table `coverage`) ; reprise : champ `essais` `{ reussis, attribues, mesureLe }` ; tests `AvancementParcoursTest`, reprise et validation |
| 2026-10-15 | D-110 | Vue de liste `LiaisonVue` : `avancement` servi au DES et au portail ; conducteur à désigner rendu sans nom (le compte de reprise n'apparaît plus comme conducteur) ; test `LiaisonVueAvancementTest` |
| 2026-10-15 | D-109 | Référentiel `preuves_signatures_cr_v17.json`, reprise `RepriseReferentielPreuvesCrV17` (changeset `20261017000100`), livrables exigés à la clôture (`HOM_LIVRABLE_MANQUANT`), sans objet motivé, opérations reprises dispensées, chronologie et pièces des opérations |
| 2026-10-19 | D-112 | Vue d'une opération : `predecesseurs` (tous les rangs prédécesseurs) ; reprise `20261019000100_etapes_brouillons_d59.xml` : étapes des opérations des brouillons D-59 tirées de `phase` |
| 2026-11-01 | US-EXI-14 | Constructeur Spring de `LectureSandboxRestAdapter` rendu explicite malgré le second constructeur de test ; régression couverte par `ClientSandboxComplementTest` (4 essais), image reconstruite et démarrage réel sain dans Docker. |
| 2026-11-05 | D-133 | Périmètre du projet de certification : gestionnaire de la liaison (son créateur), `PUT /api/v1/liaisons/{id}/perimetre` réservé au gestionnaire (`HOM_PERIMETRE_EDIT`), au super administrateur et au chargé de l'implémentation des projets de la SNA (`HOM_PERIMETRE_EDIT_TOUS`) ; services souscrits seuls, motif obligatoire, projet clos refusé ; journal `perimetre_modification` (auteur, permission, avant, après, motif) et `homologation.liaison-scope-changed.v1` ; un test par refus (`PerimetreLiaisonIT`). |
