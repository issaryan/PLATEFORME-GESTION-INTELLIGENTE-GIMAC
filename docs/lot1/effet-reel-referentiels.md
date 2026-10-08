# Effet réel des référentiels sur les acteurs et les projets (D-93)

Demande de KOAGNE ISSA RYAN : les homologations et référentiels métier doivent avoir un effet réel dans le
fonctionnement de la plateforme, pour les acteurs concernés, sur les projets concernés. Un audit du code (07/10/2026)
établit que le moteur de MS-HOMOLOGATION suit bien la version publiée (graphe, gestes, jalons, déclencheurs,
échéances, R et V, tâches MS-PILOTAGE à R puis V) et que les processus d'unité (D-87) fonctionnent en grande partie,
mais que le travail n'atteint pas encore les bonnes personnes et que plusieurs écrans restent codés en dur.

## 1. Qui reçoit le travail (MS-HOMOLOGATION)

- **Plus d'attribution arbitraire** : une fonction non affectée sur une liaison n'est plus résolue au premier
  titulaire de l'annuaire. Elle reste « à affecter » ; l'opération ouverte crée une tâche **à prendre** par les
  porteurs des profils de la fonction (mécanisme D-81, filtrée par profil comme D-87) et une tâche au conducteur
  « Affecter <fonction> ». Dès l'affectation, la tâche va à la personne désignée.
- **Départ d'une personne** : consommation de `iam.account-administered.v1` (DESACTIVATION, SUPPRESSION) : sur les
  liaisons ouvertes, ses rôles repassent « à affecter », tâche au conducteur ; un refus de tâche par MS-PILOTAGE
  (`PIL_RESPONSABLE_INVALIDE`) remonte au conducteur au lieu d'être perdu.
- **C, I, E et externes** : événements `homologation.operation-activated.v1`, `homologation.operation-validation-requested.v1`,
  `homologation.liaison-milestone-reached.v1` (outbox) ; MS-NOTIFICATION notifie les C et I internes et les comptes
  correspondants de l'institution pour ses opérations (R ou E externes) ; tâche facultative aux C (réglable par
  opération : `consultesRecoiventTache`).
- Les fonctions externes BANQUE_PARTENAIRE et PERSONNALISATEUR sont résolues vers des comptes réels si une
  institution liée est déclarée sur la liaison, sinon l'opération reste à la main du conducteur (plus de marqueur fictif).

## 2. Le RACI s'impose à l'exécution

- Dépôt de livrable ou de preuve : réservé au R désigné, au déposant du livrable, ou au conducteur de la liaison
  (ou à `HOM_STEP_MANAGE` en supervision, journalisé comme tel).
- Validation : réservée au V désigné ; nouvelle action **Renvoyer** avec motif (l'opération revient à faire, tâche
  rouverte à R).
- Les actions proposées par le dossier (`actions`) sont calculées pour la personne, pas seulement pour la fonction.

## 3. Livrables du référentiel appliqués

Dépôt par code de livrable (plusieurs par opération), types de pièces vérifiés avec MS-DOCUMENT, déposant contrôlé,
clôture impossible tant qu'un livrable obligatoire manque (modèle D-87). Signatures D-91 en sus.

## 4. Certification et participant alignés

- MS-CERTIFICATION refuse un geste (PV, approbation de sécurité, attestations) si l'opération correspondante de la
  liaison n'est pas ouverte ou si l'acteur n'est ni la personne désignée ni le conducteur ; une version de référentiel
  sans opération portant le geste attendu est signalée au contrôle de publication au lieu de bloquer plus tard.
- Le jalon EN_PRODUCTION publié fait passer l'institution à ACTIF dans MS-PARTICIPANT.

## 5. Écrans pilotés par l'opération, pas par des rangs

L'interface du dossier, du tableau de bord et du portail lit `geste`, `declencheur`, `particularite`, le libellé,
les livrables (libellé, déposant, obligatoire, types) et l'étape configurée (D-89) de chaque opération ; plus aucun
rang en dur (6, 12, 16, 17, 18 à 26, 32 à 35, C1) ni libellé `parcours.op.<rang>`. Le serveur expose `libelle` sur
les tâches et les vraies étapes de la version. Tableaux de bord « mes tâches » par personne désignée. Le simulateur
de démonstration suit les mêmes règles.

## 6. Processus d'unité (compléments)

Signatures scellées au registre d'audit (MS-AUDIT consomme `pilotage.process-step-signed.v1`) ; tâches (et non
seulement notifications) au validateur et aux signataires attendus ; réaction au changement de chef d'unité, au
retrait de profil et au changement d'unité d'un intervenant en cours d'étape.

## 7. Preuve

Tests d'intégration de chaîne (homologation, MS-PILOTAGE, notification, certification, participant) et une recette
de bout en bout sur la pile réelle (docker compose, Playwright avec authentificateur WebAuthn virtuel) : créer un
type de projet, publier son référentiel, ouvrir une liaison, vérifier que la tâche arrive à la personne désignée,
déposer, signer, valider (et renvoyer), voir l'opération suivante arriver à l'acteur suivant et le statut évoluer ;
même chose pour un processus d'unité.

## 8. Mise en œuvre (MS-HOMOLOGATION, OPA, Keycloak, 07/10/2026)

### 8.1 Qui reçoit le travail

- **Plus d'attribution arbitraire** : `ParcoursInstanciation.designation` ne lit plus l'annuaire. Une fonction est
  tenue par la ressource affectée, le conducteur, le correspondant (PARTICIPANT), une institution liée, sinon elle est
  « à affecter » (`Personne.aAffecter`, identifiant `A_AFFECTER:<FONCTION>`). `Intervenants.titulaire` est supprimé.
- **Fonctions externes** : `parcours_liaison.institutions_liees` (`{ "<FONCTION>": { "id", "nom" } }`). La banque
  partenaire déclarée à l'ouverture y est inscrite (nom lu au registre) ; une fonction externe se lie ensuite par la
  route d'affectation. Une institution liée est désignée `EXT-<institutionId>` (institution réelle). Plus aucun
  marqueur `EXT-BANQUE`, `EXT-PERSONNALISATEUR` ni `EXT-<CODE>` : la reprise Liquibase les remplace par « à affecter »
  (la banque partenaire de la liaison devient l'institution liée). Sans institution liée, la tâche de R va au conducteur.
- **Affectation à tout moment** : `PUT /api/v1/liaisons/{id}/fonctions/{fonction}` (passerelle
  `/bff/des/homologation/liaisons/{id}/fonctions/{fonction}`), `HOM_STEP_MANAGE` ou `HOM_OPERATION_ASSIGN`, corps
  `{ "personneId": "uuid" (fonction interne, profil de la fonction exigé), "institutionId": "uuid" (fonction externe),
  "motif": "0..500" }` → 204. Remplace la fonction à affecter et l'ancien titulaire sur les opérations non faites.
  Erreurs : 422 `HOM_FONCTION_NON_AFFECTABLE` (PARTICIPANT, CONDUCTEUR, code inconnu), `HOM_INSTITUTION_REQUISE`,
  `HOM_PERSON_UNKNOWN`, `HOM_PERSON_NOT_ELIGIBLE`.
- **Tâche à prendre** : MS-PILOTAGE n'accepte pas encore, sur `POST /api/v1/interne/taches`, une tâche sans responsable
  confiée à un profil (`TacheInterneRequete` : `responsableId` obligatoire, ni `profilRequis` ni `uniteCible` ;
  `Tache.pourUniteEtProfil` n'est servie qu'aux processus d'unité). En attendant :
  - le conducteur reçoit « Affecter <libellé de la fonction> (<réf.>) », priorité HAUTE, référence
    `<réf.>/AFFECTER/<FONCTION>`, lien `/des/parcours/<liaison>?onglet=etapes&fonction=<FONCTION>`, tant qu'une
    fonction interne à affecter est R d'une opération engagée, V d'une opération en validation ou C d'une opération
    dont les consultés reçoivent une tâche ; terminée dès l'affectation, rouverte si la fonction redevient à affecter ;
  - les porteurs de la fonction voient l'opération dans « mes tâches » (`aPrendre: true`), peuvent déposer (R à
    affecter) ou valider (V à affecter).
  - **À ajouter côté MS-PILOTAGE** : `TacheInterneRequete` gagne `profilsRequis: [codes ROLE_…]` (1 à 20) et
    `uniteCible` facultative ; avec `profilsRequis` et sans `responsableId`, la tâche naît `A_PRENDRE`, visible et
    prenable par tout compte interne actif portant l'un des profils (dans l'unité si elle est donnée, sinon dans toute
    l'organisation) ; idempotence par `origine.reference` seule pour ces tâches ; `/cloture` et `/reouverture` sans
    `responsableId` visent aussi la tâche à prendre ; à la prise, événement `pilotage.task-taken.v1` (`tacheId`,
    `origine.reference`, `responsableId`) que MS-HOMOLOGATION consommera pour désigner la personne sur la fonction.
    MS-HOMOLOGATION enverra alors `profilsRequis` = profils de la fonction (catalogue D-91) à la place de la tâche
    « Affecter » au conducteur.
  - **Mise à jour du 08/10/2026** : MS-HOMOLOGATION offre désormais la fonction par permission
    (`permissionsRequises`, aucun code de profil), voir §8.8.
- **Départ d'une personne** : consommateur `ComptesIamConsumer` (groupe `homologation-comptes`, sujet `iam.account`,
  `gimac.consommateurs.comptes.sujet`), type `iam.account-administered.v1`, actions `DESACTIVATION` et `SUPPRESSION`,
  idempotent (`processed_event`). Sur chaque liaison non close où le compte tient une qualité d'une opération non
  faite, une ressource ou la conduite : qualités à affecter, ressource retirée ; conducteur parti : conducteur à
  désigner (`conducteurADesigner`). Les tâches suivent : celle du partant est terminée, le conducteur reçoit
  « Affecter <fonction> ». File de rebut `iam.account.dlq` ajoutée à `gimac.kafka.rebut.topics-surveilles`.
- **Refus de MS-PILOTAGE** : `demande_tache_pilotage.escalade_vers` (le conducteur, pour les tâches R, V, C et
  signatures). Une création refusée (4xx, dont `PIL_RESPONSABLE_INVALIDE`) devient « Réaffecter : <titre> », priorité
  HAUTE, référence `<référence d'origine>/REFUS`, même lien, pour le conducteur.
- **Consultés** : attribut d'opération `consultesRecoiventTache` (modèle, modélisateur, exécution). Vrai : tâche
  « Avis attendu : <libellé> (<réf.>) », référence `<réf.>/<code>/C`, à chaque consulté interne désigné tant que
  l'opération est engagée ou en validation.

### 8.2 Événements (outbox, clé = liaison, extension CloudEvents `institutionid` = participant)

`homologation.operation-activated.v1` et `homologation.operation-validation-requested.v1`, sujet
`homologation.operation` :

```jsonc
{ "liaisonId": "uuid", "reference": "LIA-2026-001", "participantId": "uuid", "institutionNom": "…",
  "typeProjet": "CARTE_INTERBANCAIRE", "etatLiaison": "OUVERTE", "statut": "CADREE",
  "operationId": "uuid", "code": "3", "libelle": "…", "etape": "CADRAGE" /* code de l'étape de la version */,
  "vue": "AUCUNE", "geste": "DEPOSER_PREUVE", "jalon": null, "echeance": "2026-10-09",
  "motif": "OUVERTURE" | "RENVOI" | "REPRISE" /* activée seulement */,
  "responsable": Destinataire, "validateur": Destinataire,
  "destinataires": [Destinataire] /* consultés (C) et informés (I) internes */,
  "externe": true /* R ou E tenu par une fonction externe */,
  "institutionId": "uuid" /* institution de R, sinon la première de E ; absent pour une opération interne */,
  "institutionsConcernees": ["uuid"], "motifRenvoi": "…" /* motif RENVOI */,
  "lien": "/des/parcours/<liaison>?onglet=operations&operation=<id>", "le": "instant",
  "declencheur": "certification.pv-signed.v1" /* événement externe qui clôt l'opération, absent sinon (ajout du 08/10/2026) */,
  "signatures": [{ "code": "VISA", "libelle": "…", "signataire": "CHEF_DES" /* fonction D-91 */, "ordre": 1 }] /* signatures configurées, [] sinon */ }
// Destinataire (champs nuls omis)
{ "qualite": "R|V|C|I|E", "fonction": "DDRM", "libelleFonction": "…", "compteId": "uuid" /* compte interne désigné */,
  "aAffecter": true, "profils": ["ROLE_CHARGE_DDRM"] /* si à affecter */, "institutionId": "uuid" /* fonction tenue par une institution */ }
```

`homologation.operation-closed.v1`, sujet `homologation.operation` (ajout du 08/10/2026, §10.5) : à chaque opération
devenue faite (geste, validation, dernière signature, clôture forcée par un événement de MS-CERTIFICATION ou un constat)
ou sans objet (instanciation, migration), pour que la projection de MS-CERTIFICATION suive toutes les clôtures :

```jsonc
{ "liaisonId", "reference", "participantId", "operationId", "code", "libelle", "etat": "FAITE|SANS_OBJET",
  "declencheur" /* absent sinon */, "jalon" /* absent sinon */, "lien", "le" }
```

`homologation.liaison-milestone-reached.v1`, sujet `homologation.liaison` (jalon atteint : l'opération qui le porte
est faite) :

```jsonc
{ "liaisonId", "reference", "institutionId", "institutionNom", "typeProjet", "modeleTypeProjet",
  "jalon": "APTE_CERTIFICATION|CERTIFIEE|APPROUVEE_SECURITE|EN_PRODUCTION", "libelleJalon",
  "statut" /* statut du CR qui en résulte */, "etatLiaison", "operationId", "code", "libelle",
  "versionId", "versionReferentiel", "conducteurId", "destinataires": [Destinataire], "lien": "/des/parcours/<liaison>", "le" }
```

Consommateurs attendus : MS-NOTIFICATION (C et I internes, comptes de l'institution pour une opération externe, profils
d'une fonction à affecter), MS-PARTICIPANT (EN_PRODUCTION → ACTIF), MS-CERTIFICATION (contrôle d'ouverture des
gestes), MS-AUDIT (journal). Aucun sujet nouveau (`infra/kafka/creer-sujets-lot1.sh` : commentaire seulement).

### 8.3 RACI imposé

- Dépôt (`POST /parcours/operations/{id}/livrable`, `/preuves`, portail `/operations/{id}/livrable`) : R désigné ;
  R à affecter : porteur de sa fonction ; déposant du livrable visé (désigné, sinon porteur de la fonction
  déposante) ; conducteur de la liaison ; sinon `HOM_STEP_MANAGE` en supervision (dépôt marqué `supervision: true`,
  journalisé). Refus 403 `HOM_DEPOT_NON_AUTORISE`.
- Validation : V désigné ; V à affecter : porteur de sa fonction ; jamais R (409 `HOM_VALIDATOR_IS_RESPONSIBLE`) ;
  refus 403 `HOM_VALIDATION_NON_VALIDATEUR`, contrôlé avant la réauthentification.
- **Renvoyer** : `POST /api/v1/parcours/operations/{id}/renvoi`, `HOM_OPERATION_VALIDATE` (OPA et @PreAuthorize),
  corps `{ "motif": "1..500" }` → 204 ; même règle que la validation, sans réauthentification. L'opération revient
  `A_FAIRE` (`BLOQUEE` si un blocage est actif), preuve et dépôts gardés, renvoi inscrit (`renvois: [{ motif, parId,
  parNom, le }]`), tâche de R rouverte, tâche de V terminée, événement `operation-activated` (motif RENVOI). Erreurs :
  409 `HOM_OPERATION_NOT_IN_VALIDATION`, 422 `HOM_MOTIF_REQUIS`.
- `actionsPermises` de l'opération calculées pour la personne : DEPOSER_* si le droit de dépôt est acquis, `VALIDER`
  et `RENVOYER` pour le validateur admis, `SIGNER` (D-91), `ATTRIBUER` (permission).

### 8.4 Livrables appliqués

- Dépôt par code : `livrable` dans le corps (`LivrableRequest`, `PreuveRequest`) ou le paramètre `livrable` du
  multipart du portail ; absent : l'unique livrable de l'opération. 422 `HOM_LIVRABLE_REQUIS` (`details.attendus`),
  `HOM_LIVRABLE_INCONNU`. Type de la pièce lu chez MS-DOCUMENT (`GET /api/v1/interne/documents/{id}`, `typeMime`) quand
  le livrable déclare des types : 422 `HOM_LIVRABLE_TYPE_REFUSE` (`details.admis`).
- `parcours_operation.depots` : `[{ livrable, documentRef, nom, empreinte, typeMime, deposeLe, deposePar, deposeParId,
  supervision?, repris? }]` ; un nouveau dépôt du même livrable remplace le précédent. L'opération reste `EN_COURS`
  tant qu'un livrable obligatoire manque, se clôt (livrable) ou passe en validation (preuve) au dernier ; validation
  refusée 409 `HOM_LIVRABLE_MANQUANT` (`details.manquants`). Une opération sans livrable déclaré garde la pièce unique.
- Reprise : la pièce d'une opération qui déclare des livrables devient le dépôt de son premier livrable (`repris: true`).

### 8.5 Vues servies

- Opération (`GET /liaisons/{id}/parcours/operations`) : `etapeCode` (étape de la version), `vue`,
  `livrablesAttendus: [{ code, libelle, nature, deposant, libelleDeposant, typesPieces, obligatoire, depot?,
  peutDeposer }]`, `renvois`.
- Dossier : `etapesVersion: [{ code, libelle, ordre, couleur, jeton, canonique, etat, actionsFaites, actionsTotal,
  responsable, validateur, debut, closeLe, echeance, sla, livrables }]` (étapes réelles de la version),
  `intervenants: [{ fonction, libelle, qualites, personne, aAffecter }]` (lus du RACI) ; `responsableFonctionnel` et
  `responsableTechnique` (DDRM et SNA figés) ne sont plus renseignés. Livrables d'étape : `{ libelle, operation, code,
  obligatoire, fourni }` ; la clé `parcours.op.<rang>` disparaît (`libelleKey` ne reste que pour les étapes 1 et 2).
- Tâche (`TableauActeur.taches`) : `libelle`, `aPrendre`, `signature` ; les tâches sont celles de la personne.
- Attribut `vue` de l'opération (`AUCUNE`, `AFFECTATION`, `COORDONNEES`, `ESSAIS`, `DECISION`, `ATTESTATIONS`,
  `MISE_EN_SERVICE`, `CEREMONIE`) : saisi au modélisateur (`vue`, conservé si absent), servi par `OperationVue`, listé
  par le catalogue (`vuesOperation`) ; absent : déduit du geste, de la particularité puis du déclencheur. Reprise des
  versions existantes par les rangs : 6 AFFECTATION, 12 COORDONNEES, 15 à 17 ESSAIS, 18 à 20 et 23 à 26 DECISION,
  21 et 22 ATTESTATIONS, 32 à 35 MISE_EN_SERVICE, C1 CEREMONIE.

### 8.6 Consignes pour l'interface

Lire `vue` (et non le rang) pour ouvrir l'écran spécialisé ; afficher `etapesVersion` (libellé, `jeton` de couleur) au
lieu des onze étapes canoniques ; afficher `intervenants` ; proposer le dépôt par livrable (`livrablesAttendus`,
`peutDeposer`, types admis) et envoyer `livrable` ; bouton « Renvoyer » (motif) quand `RENVOYER` est permis ;
afficher `renvois` ; « Affecter » une fonction par `PUT /liaisons/{id}/fonctions/{fonction}` (personne ou institution) ;
« mes tâches » : `libelle`, badge « à prendre ». Le simulateur de démonstration suit ces formes.

### 8.7 Tests

`EffetReelParcoursTest`, `EffetReelApplicationTest`, `AdaptateursEffetReelTest`, `ComptesIamConsumerTest`,
`RelaisTachesPilotageTest.remontee` (unitaires) ; `EffetReelReferentielsIT`, `TachesParcoursIT.renvoiEtDepart`
(intégration). Tests adaptés : `TachesParcoursIT.cycle`, `ParcoursConduiteIT`, `ParcoursInstanciationTest`,
`IntervenantsTest` (premier titulaire) ; `ParcoursCommandesTest`, `ParcoursITSupport.valider`,
`ParcoursStepDefs` (validateur désigné imposé) ; `CadrageLiaisonIT` (événements d'opération dans l'outbox) ;
`TableauDeBordCalculateurTest`, `ParcoursProjectionTest` (compteurs en données, tâches par personne, livrables) ;
`LiaisonOpeningPolicyTest`, `LiaisonQueriesTest`, `ReferentialModelisateurServiceTest` (permission au lieu du profil) ;
`SuiviTachesParcoursTest`, `SignaturesParcoursTest` (création avec remontée) ; `ParcoursSamples` (plus de marqueur
fictif).

### 8.8 Tâches à prendre (08/10/2026)

- **Permissions d'une fonction** (catalogue D-91, données) : `referentiel_fonction.permissions` (JSON, 0 à 20 codes
  `^[A-Z][A-Z0-9_]{1,79}$`), servi par `GET /referentiels/fonctions` (`FonctionVue.permissions`), saisi par `POST` et
  `PUT /referentiels/fonctions/{code}` (champ `permissions`, absent : gardé ; 422 `HOM_FONCTION_PERMISSION_INVALIDE`),
  porté par `homologation.function-created.v1` et `function-updated.v1` (`permissions`, changement `permissions`). Ce
  sont les permissions dont les porteurs peuvent tenir la fonction. Valeurs de départ (changelog
  `20261012000100_taches_a_prendre.xml`) : la permission que seuls les profils de la fonction portent dans le domaine
  livré : `CHEF_DDRM` `PAR_TRANSITION_APPROVE`, `CHEF_DES` `CER_QUITUS_TECHNIQUE_SIGN`, `CHEFS_DEPARTEMENT` ces deux-là,
  `CONDUCTEUR` `HOM_PROJECT_CONDUCT`, `SNA` `CER_DOSSIER_VERIFY`, `RSSI` `CER_SECURITY_VERIFY`, `CONFORMITE`
  `CER_QUITUS_CONFORMITE_SIGN`, `DG` `CER_QUITUS_DG_SIGN`. `DDRM`, `PRODUCTION_MONETIQUE`, `POLE_RESEAU`, `POLE_SYSTEME`
  et `EXPLOITATION` n'avaient aucune permission propre dans le domaine livré : chacune reçoit désormais une permission
  dédiée (D-97, ci-dessous).
- **Une permission par fonction opérationnelle (D-97)** : sur le modèle de `HOM_PROJECT_CONDUCT`, le domaine livré
  (`infra/keycloak/generer-realm-gimac.py`, `PERMISSIONS["HOM"]`, libellés dans `LIBELLES_PERMISSIONS`) déclare cinq
  permissions non privilégiées (ni `IAM_*` ni `PLATFORM_*`), données aux profils que le catalogue des fonctions associe
  à chaque fonction (valeurs de départ de `CatalogueFonctions.REPRISES`) :

  | Fonction | Permission | Libellé | Profils livrés |
  |---|---|---|---|
  | `DDRM` | `HOM_FUNCTION_DDRM` | Tenir la fonction DDRM dans un projet d'homologation | `ROLE_CHARGE_DDRM` |
  | `PRODUCTION_MONETIQUE` | `HOM_FUNCTION_PRODUCTION` | Tenir la fonction Production monétique dans un projet d'homologation | `ROLE_PRODUCTION_MONETIQUE` |
  | `POLE_RESEAU` | `HOM_FUNCTION_NETWORK` | Tenir la fonction Pôle réseau dans un projet d'homologation | `ROLE_POLE_RESEAU` |
  | `POLE_SYSTEME` | `HOM_FUNCTION_SYSTEM` | Tenir la fonction Pôle système dans un projet d'homologation | `ROLE_POLE_SYSTEME` |
  | `EXPLOITATION` | `HOM_FUNCTION_OPERATIONS` | Tenir la fonction Exploitation et Supervision dans un projet d'homologation | `ROLE_PRODUCTION_MONETIQUE` |

  `EXPLOITATION` n'a pas de profil propre (D-58) : le catalogue la confie à la production monétique, dont le profil
  porte donc deux de ces permissions. Une assertion du générateur vérifie que chaque permission n'est portée que par ces
  profils. Valeurs de départ du catalogue : `CatalogueFonctions.REPRISES` et reprise Liquibase
  `20261013000100_permissions_fonctions.xml`, qui ne pose la permission que si la fonction est encore sans permission
  (`permissions = '[]'`) : une valeur saisie dans la plateforme n'est pas remplacée. Domaine déjà en service : la fusion
  à trois voies (`infra/ci/fusionner-domaine.py`) crée les cinq rôles et les ajoute aux profils livrés, recompositions
  faites depuis la console conservées (vérifié sur le domaine de production de la version précédente, profil recomposé
  et profil créé conservés). MS-IAM les compose dans un profil sans octroi privilégié (`MonCompteEtConfigurationTest`,
  `KeycloakAnnuaireIT.permissionsDesFonctions`) ; elles figurent dans l'annuaire de substitution de MS-IAM, dans
  `frontend/packages/lot1/src/permissions.ts` (`PERM.fonction*`, `PROFILS`) et dans les libellés
  `droits.permission.HOM_FUNCTION_*` (FR, EN, ES). Droits D-90 : `HOM_PROJECT_CONDUCT` et ces cinq permissions sont des
  droits de nature `MODIFIER` de la fonctionnalité `liaisons` (module `homologation`) du catalogue des fonctionnalités
  de MS-IAM (`fonctionnalites-initiales.json`, copie de démonstration du frontend identique), libellés
  `droits.permission.<code>` : elles se cochent dans la composition d'un profil (matrice D-90) et l'écart
  `ECARTS_SERVEUR` est levé (`droits.test.ts`, `ProfilsOrganisation.test.tsx`). Un serveur déjà en service les reçoit
  au démarrage, la fonctionnalité `liaisons` étant mise à jour tant qu'elle n'a jamais été modifiée depuis la
  plateforme (sinon conservée, `FonctionnalitesServiceTest.fichierLivre`). Effet : la tâche d'une de ces fonctions à affecter est offerte aux
  porteurs de sa permission au lieu de retomber au conducteur (`TachesAPrendreIT.valeursLivrees`).
- **Offre** : une fonction interne à affecter (mêmes conditions qu'au §8.1 : R d'une opération engagée, V en
  validation, C sollicité) dont le catalogue donne au moins une permission et dont MS-IAM connaît au moins un porteur
  interne actif (`GET /api/v1/interne/collaborateurs?permission=`) devient une tâche à prendre de MS-PILOTAGE
  (`POST /api/v1/interne/taches` sans `responsableId`, `permissionsRequises` = permissions de la fonction, aucun code de
  profil) : titre « Prendre la fonction <libellé> (<réf.>) », priorité HAUTE, référence `<réf.>/AFFECTER/<FONCTION>`,
  lien `/des/parcours/<liaison>?onglet=etapes&fonction=<FONCTION>`. Elle remplace la tâche « Affecter <fonction> » du
  conducteur. Refusée par MS-PILOTAGE (4xx) : remontée au conducteur (« Réaffecter : … », §8.1).
- **Tâche au conducteur** gardée seulement si la fonction n'a aucune permission au catalogue, si MS-IAM ne lui connaît
  aucun porteur (liste vide), ou si MS-IAM est indisponible. Choix pour l'indisponibilité : tâche au conducteur plutôt
  qu'une erreur 503, car l'offre est décidée avant la validation de la transaction du geste (dépôt, validation, départ
  d'un compte, consommateur Kafka) : un 503 ferait échouer un geste métier qui ne dépend pas de MS-IAM, alors que la
  tâche au conducteur garde le travail visible ; l'incident est consigné (niveau WARN).
- **Fin et reprise** : fonction affectée (prise, route `PUT /liaisons/{id}/fonctions/{fonction}`, ressources) : la
  tâche à prendre est terminée (`/cloture` avec `aPrendre: true`) et l'éventuelle tâche du conducteur aussi ; fonction
  de nouveau à affecter (départ de la personne) : la tâche est remise à prendre (`/reouverture` avec `aPrendre: true` :
  sans responsable, offerte de nouveau aux porteurs).
- **Prise** : consommateur `TachesPilotageConsumer`, groupe `homologation-taches`, sujet `pilotage.task`
  (`gimac.consommateurs.taches.sujet`, file de rebut `pilotage.task.dlq` ajoutée à
  `gimac.kafka.rebut.topics-surveilles`), type `pilotage.task-taken.v1`, idempotent (`processed_event`). Retenu si
  `origine.service = homologation`, référence `<réf.>/AFFECTER/<FONCTION>` et lien `/des/parcours/<liaison>…` ; la
  personne `responsableId` (prise ou attribution par le chef d'unité) est affectée à la fonction sur la liaison, même
  effet que `PUT /liaisons/{id}/fonctions/{fonction}` (`PrisesFonctions`, journal applicatif : fonction, liaison, mode,
  acteur, personne) ; sa tâche de R (ou de V, de C) suit, la tâche à prendre est terminée. Ignoré et consigné : liaison
  inconnue ou référence étrangère, fonction plus à affecter (déjà pourvue), personne inconnue ou non éligible. MS-IAM
  indisponible : l'événement est rejoué (rebut après trois essais).
- **Éligibilité** (route d'affectation et prise) : profil de la fonction, ou l'une des permissions de la fonction
  (lue dans MS-IAM). 422 `HOM_PERSON_NOT_ELIGIBLE` sinon.
- **Repli supprimé** : `gimac.homologation.permissions-repli` (liste de profils) et son code sont retirés. Les porteurs
  d'une permission sont lus dans MS-IAM seulement (`PorteursPermission`, `CollaboratorDirectoryPort.parPermission` rend
  une liste) ; MS-IAM injoignable, en refus ou réponse qui n'est pas un tableau : 503 `HOM_IAM_UNAVAILABLE` (pilote d'un
  référentiel, liste des conducteurs, ouverture d'une liaison), sans valeur supposée.
- **Contrat MS-CERTIFICATION** (§10.5) : `declencheur` et `signatures` ajoutés aux événements d'opération,
  `operation-closed.v1` (§8.2) ; `certification.pv-signed.v1` est lu comme complet (procès-verbal `SIGNE`, plus de
  reconnaissance du participant par sa fonction) et les signatures configurées (D-91) des opérations de déclencheur
  `pv-signed` dont le code figure dans `signatures[].signature` sont tenues pour recueillies (signataire, date,
  empreinte du procès-verbal, `origine: certification.pv-signed.v1`, sans nouvel `operation-signed.v1` : MS-CERTIFICATION
  les a scellées) : l'opération se clôt sans passer `EN_SIGNATURE` ; une signature configurée absente de l'événement
  reste attendue. Publication d'une version (et contrôle à blanc, graphe du modélisateur) : anomalie
  `GESTE_CERTIFICATION_ABSENT` (409 `HOM_REFERENTIAL_GRAPH_INVALID`, `codes` = jalon puis déclencheurs manquants) quand
  un jalon est porté sans les déclencheurs attendus, lus en configuration
  (`gimac.homologation.referentiel.gestes-certification.toujours.<JALON>` : `CERTIFIEE` pv-established et pv-signed,
  `APPROUVEE_SECURITE` security-approved ; `.avec-attestations.<JALON>` si le modèle du type exige des attestations :
  `APPROUVEE_SECURITE` change-record-constituted et attestations-archived). La reprise Liquibase du référentiel n'y est
  pas soumise.
- **Interface** : catalogue des fonctions, champ « Permissions » ; « Mes tâches » : tâche à prendre de MS-PILOTAGE
  (`permissionsRequises`, bouton « Prendre »), la fonction est affectée à la prise.
- **Tests** : `TachesAPrendreIT` (offre par permission, prise par l'événement, idempotence, prise sans objet, départ et
  remise à prendre, éligibilité par permission, sans porteur : conducteur), `TachesPilotageConsumerTest`,
  `PrisesFonctionsTest` (référence, permissions du catalogue), `ContratCertificationTest` (déclencheur et signatures,
  `operation-closed`, signatures du procès-verbal reprises, `GESTE_CERTIFICATION_ABSENT`, configuration),
  `EffetReelApplicationTest.porteurs` et `AdaptateursEffetReelTest.parPermission` (sans repli, 503).

## 9. Mise en œuvre MS-PILOTAGE (08/10/2026)

### 9.1 Tâches à prendre offertes par un autre service (réponse au §8.1)

- **Création** : `POST /api/v1/interne/taches` (hors passerelle, jeton de service `INTERNAL_PILOTAGE_TASK_CREATE`),
  `TacheInterneRequete` gagne `profilsRequis` et `uniteCible`, noms et forme attendus par le §8.1 :

```jsonc
{ "titre": "1..200", "description": "0..4000", "echeance": "AAAA-MM-JJ", "priorite": "BASSE|NORMALE|HAUTE|URGENTE",
  "createurId": "…" /* facultatif, défaut : le client du jeton */,
  "origine": { "type": "OPERATION|REFERENTIEL|…", "reference": "LIA-2026-001/AFFECTER/DDRM", "service": "homologation", "lien": "/des/…" },
  "profilsRequis": ["ROLE_…", "…"] /* 1 à 20 codes de profil (données de MS-IAM), distincts */,
  "permissionsRequises": ["CER_DOSSIER_VERIFY", "…"] /* 1 à 20 permissions, distinctes (ajout du 08/10/2026) */,
  "uniteCible": "DDRM" /* facultative : sinon toute l'organisation */ }
```

  **Par permission** (D-94, ajout du 08/10/2026) : `permissionsRequises` (codes `^[A-Z][A-Z0-9_]*$`, 80 caractères
  au plus) se donne à la place de `profilsRequis` ou avec lui ; l'un des profils **ou** l'une des permissions suffit
  (union : un service peut passer des profils aux permissions sans trou ; MS-HOMOLOGATION n'envoie que des
  permissions). Les porteurs d'une permission sont lus par `GET /api/v1/interne/collaborateurs?permission=` de MS-IAM
  (comptes internes actifs dont un profil compose la permission), mis en cache comme les autres lectures
  (`gimac.organisation.cache-secondes`, 60 s, vidé à chaque changement d'organisation ou de profils reçu) ; dans une
  unité, ce sont les porteurs membres de l'unité ou de ses sous-unités. Stockage : table
  `tache_permission_requise (tache_id, permission)` (changelog `20261008100000_pil_taches_permissions.xml`).

  Avec `profilsRequis` et sans `responsableId`, la tâche naît `A_PRENDRE`, sans responsable (`offre_profils`), et
  `pilotage.task-created.v1` puis `pilotage.task-offered.v1` partent (destinataires : comptes internes actifs portant
  l'un des profils, dans l'unité et ses sous-unités si elle est donnée, sinon par
  `GET /api/v1/interne/collaborateurs?profil=` de MS-IAM). Idempotence par `origine.reference` seule (index unique
  partiel `ux_tache_offre`) : 201 à la création, 200 avec la tâche existante ensuite, prise ou non. Sans
  `profilsRequis`, le contrat D-77 est inchangé (`responsableId` obligatoire, idempotence par référence et
  responsable).
- **Refus** : 400 `PIL_REQUETE_INVALIDE` (`details.champ` = `profilsRequis`) si `responsableId` et `profilsRequis` sont
  donnés ensemble, si `uniteCible` vient sans `profilsRequis` ni `permissionsRequises`, si la liste est vide, compte plus
  de 20 codes ou un code invalide ; mêmes refus pour `permissionsRequises` (`details.champ` = `permissionsRequises`, y
  compris avec `responsableId`) ; 400 (`uniteCible`) pour un code d'unité mal formé ; 422 `PIL_UNITE_INCONNUE` pour une unité inconnue de
  MS-IAM ; 503 `PIL_ORGANISATION_INDISPONIBLE` si MS-IAM ne répond pas (à réessayer).
- **« Mes tâches »** : `GET /api/v1/taches?vue=moi` (passerelle `/bff/des/pilotage/taches?vue=moi`, inchangée) rend
  les tâches dont l'appelant est responsable et les tâches à prendre qu'il peut prendre : offre dont il porte l'un des
  profils ou l'une des permissions (dans l'une de ses unités si une unité est visée ; MS-IAM injoignable : seules les
  offres par profil sont listées, sans échec) et étape de processus réservée à un profil qu'il porte dans
  l'une de ses unités. La lecture `GET /taches/{id}` est ouverte aux mêmes personnes.
- **Prise** : route existante `POST /api/v1/taches/{id}/prise` (passerelle `/bff/des/pilotage/taches/{id}/prise`,
  `PIL_ACTIVITY_OWN`) : la tâche devient `OUVERTE` à l'appelant. Refus : 403 `PIL_PROFIL_REQUIS` (aucun des profils ni
  aucune des permissions ; même code pour une offre par permission), 503 `PIL_ORGANISATION_INDISPONIBLE` (porteurs
  d'une permission illisibles),
  403 `PIL_ACTION_RESERVEE` (hors de l'unité visée), 409 `PIL_TRANSITION_INTERDITE` (déjà prise, terminée ou annulée,
  prise concurrente), 403 `PIL_HORS_EQUIPE` (tâche ni lisible ni prenable). L'attribution
  (`POST /taches/{id}/attribution`) reste réservée au chef de l'unité visée et au créateur ; la personne doit porter
  l'un des profils ou l'une des permissions (422 `PIL_RESPONSABLE_INVALIDE`, `details.profilsRequis`,
  `details.permissionsRequises`).
- **Événement de prise** : `pilotage.task-taken.v1` (sujet `pilotage.task`, clé = tâche), à chaque prise ou attribution
  d'une tâche à prendre (offre, tâche d'unité D-81, étape de processus) :

```jsonc
{ "tacheId": "uuid", "origine": { "type": "OPERATION", "reference": "LIA-2026-001/AFFECTER/DDRM", "service": "homologation", "lien": "/des/…" },
  "responsableId": "uuid" /* la personne désignée */, "acteurId": "uuid", "mode": "PRISE" | "ATTRIBUTION",
  "profilsRequis": ["ROLE_…"] /* vide hors offre */, "profilRequis": "ROLE_…" | null /* étape de processus */,
  "uniteCible": "DDRM" | null, "titre": "…", "le": "instant",
  "permissionsRequises": ["CER_DOSSIER_VERIFY"] /* vide hors offre par permission (ajout du 08/10/2026) */ }
```

  `pilotage.task-assigned.v1` part aussi (notification). MS-HOMOLOGATION reconnaît ses offres par
  `origine.service` et `origine.reference`.
- **Clôture et réouverture** : `POST /api/v1/interne/taches/cloture` et `/reouverture` sans `responsableId` visent
  toutes les tâches de la référence, offre comprise. Une offre terminée sans avoir été prise reste sans responsable
  (`pilotage.task-completed.v1`, `responsableId` nul) ; rouverte, elle redevient `A_PRENDRE` (prise, elle reprend
  `EN_COURS` comme avant). **Remise à prendre** (ajout du 08/10/2026) : `CibleInterneRequete.aPrendre: true` vise
  l'offre seule de la référence ; `/reouverture` la remet alors `A_PRENDRE` sans responsable même si elle avait été
  prise (la fonction qu'elle pourvoit est de nouveau vacante), historique `TRANSITION` (« … remise à prendre … »),
  `pilotage.task-offered.v1` de nouveau aux porteurs ; `/cloture` avec `aPrendre` termine l'offre seule.
- **Vue tâche** (`TacheVue`) : nouveaux champs `profilsRequis` (liste triée, vide hors offre), `permissionsRequises`
  (liste triée, vide hors offre par permission) et `aPrendre` (état
  `A_PRENDRE`) ; `responsable` nul tant que la tâche n'est pas prise ; `profilRequis` (étape de processus) inchangé.
- **MS-HOMOLOGATION** (§8.8) : envoie `permissionsRequises` (jamais `profilsRequis`) pour une fonction à affecter et
  consomme `pilotage.task-taken.v1` (groupe `homologation-taches`) ; il remet l'offre à prendre par `aPrendre`.
- **Stockage** : `tache.offre_profils`, table `tache_profil_requis (tache_id, profil)`, contrainte
  `ck_tache_responsable` élargie (changelog `20261008090000_pil_taches_a_prendre.xml`).

### 9.2 Processus d'unité : tâches au validateur et aux signataires (§6)

- Étape en attente de signatures : une tâche d'origine `PROCESSUS` par signature ouverte (ordre minimal), référence
  `<instance>/<ETAPE>/SIGNATURE/<CODE>` ; en attente de validation : une tâche, référence `<instance>/<ETAPE>/VALIDATION`.
  Titre « Signer « <libellé> » : <étape> : <intitulé> » ou « Valider : <étape> : <intitulé> », échéance de l'étape, lien
  `/des/processus/instances/<id>?etape=<ETAPE>`. Signataire ou validateur nommé (personne, désigné au lancement, chef,
  lanceur, n+1) : tâche `OUVERTE` à lui ; par profil : tâche `A_PRENDRE` (unité, `profilRequis`), dans « Mes tâches »
  des porteurs. Événements `pilotage.task-created.v1`, `task-assigned` ou `task-offered`.
- **Prise** d'une tâche de signature ou de validation : la personne devient le signataire ou le validateur attendu
  (elle seule signe ou valide ensuite), historique `ETAPE_PRISE` (« Signature VISA prise par … »).
- Signature apposée, validation ou renvoi décidés : la tâche est terminée au nom de l'acteur (confiée à lui si elle
  était à prendre) ; une nouvelle attente rouvre la même tâche (une tâche par référence). Instance annulée : tâches
  annulées. Comme la tâche d'exécution, elles ne se terminent ni ne s'annulent à la main (409 `PIL_TACHE_PROCESSUS`).

### 9.3 Processus d'unité : réaction aux changements d'organisation (§6)

- **Consommation** (idempotente par l'identifiant CloudEvents, table `processed_event`) :
  - sujet `iam.account`, type `iam.account-administered.v1`, groupe `pilotage-processus-comptes` : actions
    `DESACTIVATION`, `SUPPRESSION` (départ, inchangé), `PROFILS` (cause `PROFIL_RETIRE`) et
    `COMPTE_RATTACHEMENT_MODIFIE` (cause `CHANGEMENT_UNITE`), `data.compteId` ;
  - sujet `iam.configuration` (`gimac.consommateurs.configuration.sujet`), type `iam.configuration-changed.v1`, groupe
    `pilotage-processus-organisation` : `data.objet` = `UNITE_ORGANISATIONNELLE`, unité lue dans `data.champs`
    (`code=<CODE>`) (cause `CHANGEMENT_CHEF`).
  Chaque réception vide les lectures de MS-IAM mises en cache (60 s) avant la revue.
- **Revue d'un rôle** (exécutant d'une étape active, validateur, signataire attendu) selon le mode de désignation :
  - le titulaire garde le rôle s'il est la personne résolue (personne nommée, désignée au lancement, lanceur, n+1,
    chef actuel de l'unité) ou, hors `CHEF_UNITE`, s'il est actif et porte dans l'unité le profil du rôle (`PROFIL`) ou
    son profil de remplacement (autres modes) ;
  - sinon le rôle passe à la personne résolue (nouveau chef, personne nommée de nouveau active), à défaut il repasse à
    prendre par ce profil dans l'unité (étape « à affecter ») ;
  - `CHEF_UNITE` : changement de chef de l'unité, le rôle suit le nouveau chef (à défaut, profil de remplacement) ; un
    rôle à prendre faute de chef est confié au chef nommé ensuite ;
  - une personne nommée ou désignée au lancement garde son rôle si elle change d'unité ou de profils (seul son départ
    la remplace, D-87) ;
  - une étape réaffectée à la main par le chef d'unité (`REAFFECTEE`) garde son exécutant.
  Effets : tâche d'exécution confiée ou remise à prendre, tâches d'attente réalignées, historique `ORGANISATION`
  (« Profil retiré : EXECUTANT : X vers à prendre par … »), événement `pilotage.process-step-reassigned.v1` avec
  `cause` `PROFIL_RETIRE`, `CHANGEMENT_UNITE` ou `CHANGEMENT_CHEF`, `roles`, `ancienId`, `executantId` (nouvelle
  personne ou nul), `profil`, `unite`, `chefId`, `destinataires` (nouvelle personne ou porteurs du profil, et le chef).
  Un changement de parent d'unité n'est pas revu (seul le chef de l'unité modifiée l'est).
- Départ (inchangé) : la tâche d'exécution remise à prendre pendant les signatures ou la validation est terminée au
  nom de l'acteur qui clôt l'étape.

### 9.4 Consignes pour l'interface

- « Mes tâches » : badge « à prendre » si `aPrendre`, ligne « Profils requis » (`profilsRequis`, sinon
  `profilRequis`), bouton « Prendre » (`POST /pilotage/taches/{id}/prise`), messages pour 403 `PIL_PROFIL_REQUIS` et
  409 `PIL_TRANSITION_INTERDITE` (« déjà prise »). Responsable absent : afficher « à prendre », pas une personne.
- Tâches d'attente d'un processus (référence finissant par `/VALIDATION` ou `/SIGNATURE/<CODE>`) : le lien ouvre
  l'instance sur l'étape ; transitions Terminée et Annulée masquées comme pour la tâche d'étape.
- Historique d'instance : libellé pour l'action `ORGANISATION` (« Organisation modifiée ») ; notification de
  `process-step-reassigned` : causes `PROFIL_RETIRE`, `CHANGEMENT_UNITE`, `CHANGEMENT_CHEF`.

### 9.5 Dépendances et limites

- Infra Kafka : le sujet `iam.configuration` (et `iam.configuration.dlq`) n'est pas créé par
  `infra/kafka/creer-sujets-lot1.sh` : à ajouter pour que le changement de chef d'unité soit reçu en production.
- MS-NOTIFICATION : `pilotage.task-taken.v1` n'appelle pas de notification ; causes nouvelles de
  `process-step-reassigned` à libeller.
- Passerelle et OPA : aucune route nouvelle (prise et « Mes tâches » existent ; la route interne est hors passerelle).

### 9.6 Tests

`TachesAPrendreIT` (offre sans unité et dans une unité, idempotence, « Mes tâches », prise, `task-taken`, clôture et
réouverture sans prise, refus ; offre par permission : destinataires, « Mes tâches », lecture et prise refusées sans la
permission, `task-taken` avec `permissionsRequises`, attribution par le chef à un porteur seulement, remise à prendre
`aPrendre`, profils et permissions ensemble, refus), `AdaptateursReelsTest.porteursPermission` (lecture par
permission, cache, 503), `TacheTest.offreParPermission` (prise, remise à prendre), `ProcessusOrganisationIT` (retrait de profil, changement d'unité, tâches de signature
et de validation, prise du signataire, nouveau chef, annulation), `ProcessusExecutionIT` (tâches d'attente du
parcours complet), `ProcessusKafkaIT.changementsOrganisationConsommes` (sujets `iam.account` et `iam.configuration`) ;
unitaires `TacheTest` (offre, réouverture à prendre, tâche d'attente), `EtapeInstanceTest.reaffecterSignature`,
`ComptesIamConsumerTest` (lecture et aiguillage).

## 10. Mise en œuvre MS-CERTIFICATION et MS-PARTICIPANT (08/10/2026)

### 10.1 Source de vérité des opérations : projection des événements (MS-CERTIFICATION)

- **Choix** : MS-HOMOLOGATION ne sert aucune route interne sur l'état des opérations (seul
  `GET /api/v1/interne/liaisons/{id}/contexte-certification` existe, sans opérations ; `GET /liaisons/{id}/parcours/
  operations` exige un jeton utilisateur `HOM_LIAISON_READ`). Les événements de §8.2 existent : MS-CERTIFICATION tient
  donc une projection locale, table `operation_liaison` (`OperationLiaison` : opération, liaison, code, libellé,
  geste, vue, jalon, `declencheur`, `etat`, `intervenants` R, V, C, I, `institutions_concernees`, `signatures`,
  `evenement_le`).
- **Consommateur** `OperationsHomologationConsumer`, groupe `certification-operations`, sujets `homologation.operation`
  et `homologation.liaison`, idempotent (`processed_event`), message illisible en rebut. Effets :
  `operation-activated.v1` → `OUVERTE` (ouverture, renvoi, reprise) ; `operation-validation-requested.v1` →
  `EN_VALIDATION` ; `operation-signed.v1` → `EN_SIGNATURE`, ou `FAITE` si `operationClose` ;
  `operation-closed.v1` (§8.2, ajout du 08/10/2026) → `FAITE` ou `SANS_OBJET` (clôtures forcées comprises ; un état
  inattendu est tenu pour fait et consigné) ; `liaison-milestone-reached.v1` → opération du jalon `FAITE`. Un événement
  dont `le` (sinon `time`) précède le dernier appliqué est ignoré (rejeu, réouverture antérieure à une clôture). Autres
  types ignorés.
- **Déclencheur de l'opération** : lu seulement dans les événements de MS-HOMOLOGATION. `operation-activated.v1` et
  `operation-validation-requested.v1` font foi : `declencheur` absent, l'opération ne porte aucun geste de
  certification (un déclencheur antérieur est effacé). `operation-closed.v1` le complète s'il est publié. La
  correspondance par code (`gimac.certification.operations.declencheurs-par-code`) est retirée, configuration et code.
  Reprise : aucune ; la table `operation_liaison` naît dans le même lot non livré, et une ligne d'un poste de
  développement reçoit le déclencheur publié au prochain événement de son opération.

### 10.2 Gestes contrôlés (D-93 §4)

Un geste exige une opération **ouverte** dont le déclencheur est l'événement que MS-CERTIFICATION émet au terme du
geste (`GesteCertification`, aucun rang) :

| Geste | Route | Déclencheur de l'opération |
| --- | --- | --- |
| `ETABLIR_PV` | `POST /liaisons/{id}/proces-verbal` | `certification.pv-established.v1` |
| `SIGNER_PV` | `POST …/proces-verbal/signature-gimac`, portail `…/proces-verbal/signature` | `certification.pv-signed.v1` |
| `CONSTITUER_DOSSIER` | `POST …/approbation-securite/dossier` | `certification.change-record-constituted.v1` |
| `DEPOSER_ATTESTATION`, `VERIFIER_ATTESTATION`, `ARCHIVER_ATTESTATIONS` | portail `…/attestations/{type}`, `…/verification`, `…/archivage` | `certification.attestations-archived.v1` |
| `REFERENCER_FICHE`, `RELIRE_FICHE`, `SIGNER_VISA` | `PUT …/fiche`, `POST …/fiche/lecture`, `POST …/visas/{nature}` | `certification.security-approved.v1` |

- **Acteur** : personne désignée R ou V de l'opération (`compteId`), sinon conducteur de la liaison (contexte de
  certification, lu seulement si nécessaire) ; R ou V « à affecter » : tout porteur de la permission de la route (comme
  MS-HOMOLOGATION) ; au portail : l'institution qui tient une fonction de l'opération (`institutionsConcernees` ou
  intervenant institutionnel). Les signatures sont contrôlées par le circuit (§10.3).
- **Erreurs** (ProblemDetail, `code`, `details`) : 409 `CER_OPERATION_ABSENTE` (`details.geste`,
  `details.declencheur` : la version ne porte pas le geste), 409 `CER_OPERATION_NON_OUVERTE` (`details.operations:
  [{ code, etat }]`), 409 `CER_OPERATION_INCONNUE` (liaison sans projection, mode REFUSER), 403
  `CER_ACTEUR_NON_DESIGNE` (`details.geste`, `details.operations`). Les règles d'état propres aux verrous restent
  contrôlées avant (p. ex. `CER_PV_NOT_READY`, `CER_PV_NOT_SIGNED`), le cloisonnement du portail aussi (404).
- **Transition** : une liaison dont aucun événement d'opération n'est encore reçu (ouverte avant le déploiement) est
  admise sans contrôle (`gimac.certification.operations.liaison-sans-projection: ADMETTRE`, défaut) ; passer à
  `REFUSER` quand toutes les liaisons en cours ont émis leurs événements. Les clôtures forcées par un événement de
  MS-CERTIFICATION arrivent par `operation-closed.v1` ; la répétition d'un geste reste aussi refusée par l'état du
  verrou (procès-verbal établi, attestations archivées, approbation acquise).
- **Quitus** : `QuitusResource` reste l'administration générée (`ROLE_ADMIN`) ; aucun geste métier de quitus n'est
  servi par le lot 1, rien à contrôler.

### 10.3 Circuit de signature en données (D-94)

- `RoleSignature` est supprimé. Une étape de circuit (`EtapeSignature`) : `code`, `libelle`, `fonction` (catalogue
  D-91), `ordre` (1..99, croissant, même ordre : indifférent), `portail` (signée au portail par l'institution).
- **Procès-verbal** : les `signatures` publiées avec l'opération ouverte qui porte la signature du procès-verbal
  (déclencheur `pv-signed`, `[{ code, libelle, signataire, ordre }]` reprises telles quelles : code, libellé, fonction,
  ordre), complétées des étapes `portail` de la configuration ; `signatures: []` : circuit par défaut (le référentiel D-91
  refuse un signataire externe) ; à défaut, `gimac.certification.circuits.proces-verbal` (livré : `GIMAC` tenue par
  `CHEF_DES`, `PARTICIPANT` au portail, ordre 1 tous deux, comportement antérieur). Le circuit est figé sur le
  procès-verbal à la première signature (`proces_verbal_certification.circuit`).
- **Signataire** : côté DES, la prochaine étape signable dont l'acteur tient la fonction sur l'opération ouverte, sinon
  le conducteur ; une fonction que personne ne tient sur l'opération est laissée au porteur de
  `CER_QUITUS_TECHNIQUE_SIGN`. Refus 403 `CER_SIGNATAIRE_NON_DESIGNE` (`details.signature`, `details.fonction`),
  409 `CER_SIGNATURE_ORDRE` (`details.attendues`), 409 `CER_PV_NOT_SIGNABLE`. État : `SIGNE_GIMAC` quand toutes les
  étapes du DES sont signées, `SIGNE_PARTICIPANT` pour le portail, `SIGNE` à la dernière (verrou 2 ouvert).
- **Visas** : étape `gimac.certification.circuits.visas.<CONFORMITE|DIRECTION_GENERALE>` (fonctions `CONFORMITE`, `DG`),
  même règle de signataire sur l'opération ouverte qui porte `security-approved`. Activation inchangée
  (`gimac.certification.visas.*`).
- **Réauthentification** : finalité `gimac.certification.circuits.finalite`, `SIGNATURE_OPERATION` par défaut (au
  lieu de `QUITUS_SIGN`).
- **Vue** : `ProcesVerbalVue.circuit: [{ code, libelle, fonction, ordre, portail, etat: ATTENDUE|SIGNEE, signature? }]`
  ; `signatureGimac` et `signatureParticipant` gardés (première signature de chaque côté) ; `SIGNER_GIMAC` n'est
  proposé qu'au signataire admis.
- **Événements** : `certification.pv-signature-recorded` et `certification.pv-signed.v1` gagnent `signature` (code de
  l'étape) à côté de `fonction`.
- **Reprise Liquibase** (`20261011000001_d93_d94_operations_circuits.xml`) : table `operation_liaison` ;
  `signature_verrou.role` renommée `etape` (40), `fonction` (40) ; les codes repris (GIMAC, PARTICIPANT, CONFORMITE,
  DIRECTION_GENERALE) sont ceux du circuit par défaut, aucune ligne immuable n'est modifiée ; un procès-verbal déjà en
  signature reçoit le circuit par défaut figé.

### 10.4 Jalon EN_PRODUCTION (MS-PARTICIPANT)

- Consommateur `JalonLiaisonConsumer`, groupe `participant-jalon-liaison`, sujet `homologation.liaison`
  (`gimac.consommateurs.jalons.sujet`), type `homologation.liaison-milestone-reached.v1`, idempotent
  (`processed_event`), message illisible en rebut.
- `jalon = EN_PRODUCTION` : `ManageInstitutionUseCase.mettreEnProduction` ; institution `EN_HOMOLOGATION` → `ACTIF`,
  transition automatique approuvée par le système (`status_transition.automatic_transition`), motif « Jalon
  EN_PRODUCTION de la liaison <réf.> (<type>) », journal `CHANGEMENT_STATUT`, `participant.status-changed.v1` par
  l'outbox. Institution déjà `ACTIF` (seconde liaison) : rien ; `A_REGULARISER`, `SUSPENDU`, autre : signalé dans le
  journal applicatif, jamais appliqué (une réactivation reste une décision humaine). Institution inconnue : 404 métier,
  message en rebut.
- Autres jalons (`APTE_CERTIFICATION`, `CERTIFIEE`, `APPROUVEE_SECURITE`) : aucun effet documenté sur l'institution,
  ignorés sans trace d'idempotence.

### 10.5 Consignes pour les autres composants

- **MS-HOMOLOGATION, événements** : fait (§8.2 et §8.8) : `declencheur` et `signatures` publiés,
  `operation-closed.v1` émis ; MS-CERTIFICATION les consomme (§10.1, §10.3).
- **MS-HOMOLOGATION, contrôle de publication et procès-verbal signé** : faits (§8.8 : anomalie
  `GESTE_CERTIFICATION_ABSENT`, `pv-signed.v1` lu comme complet, signatures D-91 de l'opération porteuse tenues pour
  recueillies d'après `signatures[].signature`).
- **Interface** : demander le défi WebAuthn avec la finalité `SIGNATURE_OPERATION` pour le procès-verbal et les visas
  (`ProcesVerbal.tsx`, `Visas.tsx`, `ProcesVerbalPage.tsx`, type `SignPurpose`) ; afficher `circuit` ; traiter
  `CER_OPERATION_ABSENTE`, `CER_OPERATION_NON_OUVERTE`, `CER_ACTEUR_NON_DESIGNE`, `CER_SIGNATAIRE_NON_DESIGNE`,
  `CER_SIGNATURE_ORDRE`.
- Aucun sujet Kafka nouveau (commentaire de `infra/kafka/creer-sujets-lot1.sh` et de sa copie Kubernetes) ; aucune
  route nouvelle, aucune règle OPA.

### 10.6 Tests

MS-CERTIFICATION : `GestesParOperationTest` (liaison sans projection et mode REFUSER, opération absente, non ouverte,
rejeu, renvoi, clôture par jalon et signature, `operation-closed` faite ou sans objet et ordre, déclencheur publié
ou absent, acteur désigné, conducteur, fonction à affecter, portail, circuit de
l'opération ordonné et figé, visas par circuit, finalité, contrôles de configuration), `OperationsHomologationConsumerTest`
(lecture des charges §8.2 dont `declencheur` et `operation-closed`, doublon, types ignorés, rebut), `PolitiquesDecisionTest` (état et ordre du circuit),
`EntitesDecisionTest` (étape, circuit repris par Liquibase), `ProcesVerbalServiceTest`, `DecisionResourceSecurityTest`
(forme `circuit`) ; intégration `GestesParOperationIT` (PostgreSQL : projection idempotente, clôture publiée, refus 409 et 403,
circuit de l'événement appliqué, `signatures: []` vers le circuit par défaut, opération sans déclencheur, colonnes
`etape` et `circuit`). MS-PARTICIPANT : `JalonLiaisonConsumerTest`,
`InstitutionLifecycleServiceTest.MiseEnProduction` ; intégration `ParcoursDdrmIT.miseEnProduction` (Kafka réel :
jalon livré deux fois, une seule transition, autres jalons sans effet, journal et outbox).

## 11. Mise en œuvre MS-NOTIFICATION et MS-AUDIT (08/10/2026)

### 11.1 Notifications du parcours d'homologation (MS-NOTIFICATION)

- **Consommateur** `EvenementsHomologationConsumer` : groupe `notification-homologation`, sujets
  `homologation.operation` et `homologation.liaison` (`gimac.consommateurs.homologation.sujets`), types
  `homologation.operation-activated.v1`, `homologation.operation-validation-requested.v1`,
  `homologation.liaison-milestone-reached.v1` (les autres types, dont `operation-signed`, sont ignorés). Idempotent par
  identifiant CloudEvents (`processed_event`, propre au groupe) ; échéance lue en `AAAA-MM-JJ` ou en tableau ; erreur
  (enveloppe illisible, MS-IAM injoignable) : trois remises puis `<sujet>.dlt` (les deux sujets ajoutés à
  `gimac.kafka.rebut.topics-surveilles`).
- **Type** `HOMOLOGATION` (contrainte `ck_notification_utilisateur_type` étendue, préférence `types.HOMOLOGATION`, colonne
  `type_homologation` active par défaut, changelog `20261007150000_not_homologation.xml`). Une notification par événement
  et par compte, jamais regroupée, sans acteur. Référence `{ service: "homologation", type: "OPERATION", id: operationId }`
  (opération) ou `{ …, type: "LIAISON", id: liaisonId }` (jalon). `notification.inapp-created.v1` publié comme les autres
  types (`referenceService` `homologation`).
- **Rôles** (`ReglesHomologation`, service de domaine pur), un compte visé à plusieurs titres reçoit la seule
  notification du rôle le plus fort (action, escalade, avis, information) :

| Événement | R | V (A) | C | I | E | Institution |
|---|---|---|---|---|---|---|
| opération ouverte | « Action attendue : {libellé} » (« Opération renvoyée : … » si motif RENVOI) | rien (sa tâche arrive à la validation) | « Avis demandé : … » | « Opération ouverte : … » | « Escalade : … » | R externe : comptes de l'institution, action ; autres `institutionsConcernees` : escalade |
| validation demandée | rien (il vient de déposer) | « Validation attendue : … » | avis | « Opération en validation : … » | escalade | R externe et institutions concernées : « Opération en validation : … » (information) |
| jalon atteint | conducteur (`conducteurId`), C et I : « Jalon atteint : {libelleJalon} » (information) | | | | | institution de la liaison, si le jalon figure dans `gimac.notification.homologation.jalons-portail` (défaut : les quatre jalons) |

  Sous-titre : « {référence} ({institution}), échéance {AAAA-MM-JJ} » (opération) ou « {référence} ({institution}) »
  (jalon), éléments absents omis. Préférence `HOMOLOGATION` de chaque compte respectée (comptes d'institution compris).
- **Liens** (chemins lus dans `frontend/apps`) : couche interne, le `lien` de l'événement s'il commence par
  `/des/parcours/` et est sûr (sans espace, caractère de contrôle ni `..`, 500 caractères au plus), sinon
  `/des/parcours/<liaison>?onglet=operations&operation=<id>` (onglet Opérations du dossier, `DossierLiaisonPage`) ; jalon :
  `/des/parcours/<liaison>`. Portail (`apps/portail`, routes `liaisons/:id` et `liaisons/:id/livrables/:op`) : action
  attendue d'une institution sur un geste `DEPOSER_*` : `/portail/liaisons/<liaison>/livrables/<operation>` ; sinon
  `/portail/liaisons/<liaison>`. Un identifiant hors `[A-Za-z0-9-]{1,80}` n'entre jamais dans un lien.
- **Comptes de l'institution** : port `ComptesInstitutionPort`, adaptateur réel `IamComptesInstitutionAdapter`
  (`gimac.adapters.annuaire=real`) : `GET /api/v1/interne/collaborateurs?institution=<id>` de MS-IAM (jeton
  `notification-service`, `INTERNAL_IAM_READ`), mis en cache 60 s (`gimac.notification.cache-institutions-secondes`).
  Seuls les comptes `actif` dont `institutionId` vaut l'institution demandée sont retenus, réponse en tableau ou en page
  (`contenu`) : tant que MS-IAM ignore ce paramètre (il sert alors l'annuaire interne), personne n'est notifié par
  erreur. 404 : aucun compte ; autre erreur : 503 `NOT_ANNUAIRE_INDISPONIBLE`, l'événement est rejoué puis mis en rebut.
  Adaptateur de substitution `ComptesInstitutionStubAdapter` (une institution de démonstration, deux correspondants).
- **Fonctions à affecter** (`aAffecter: true`, `A_AFFECTER:<FONCTION>`) : aucune notification nominative ni diffusion aux
  porteurs des profils (`profils` de la charge non utilisés) ; la fonction est seulement journalisée (niveau INFO, code de
  la fonction, jamais de nom). Le travail leur parvient par MS-HOMOLOGATION et MS-PILOTAGE : tâche « Affecter
  <fonction> » à prendre (§8.8, `permissionsRequises` de la fonction) : notification `TACHE` « à prendre » de
  `pilotage.task-offered.v1` aux porteurs ; à défaut de porteur ou de permission au catalogue, tâche au conducteur
  (notification `TACHE` de `pilotage.task-assigned.v1`). Dès l'affectation, l'événement suivant (renvoi, validation) vise la personne.
- **Processus d'unité** (§9.4, §9.5) : causes `PROFIL_RETIRE` et `CHANGEMENT_UNITE` de `process-step-reassigned` titrées
  « Étape à reprendre : {étape} », sous-titre « {instance}, après le retrait d'un profil de {ancien} » ou « … après le
  changement d'unité de {ancien} » ; `CHANGEMENT_CHEF` : « Étape réaffectée : {étape} », « {instance}, après un changement
  de chef d'unité ».

### 11.2 Signatures scellées au registre d'audit (MS-AUDIT)

- **Consommateur** `SignaturesConsumer` : groupe `audit-signatures`, sujets `pilotage.process` et `homologation.operation`
  (`gimac.consommateurs.signatures.sujets`), types `pilotage.process-step-signed.v1` (charge `EtapeSigneePayload` de
  MS-PILOTAGE : `instanceId, code, intitule, etape, libelleEtape, signature, signataireId, empreinte, documents, etatEtape,
  destinataires, lien, le`) et `homologation.operation-signed.v1` (`OperationSignedPayload` de MS-HOMOLOGATION, D-91). Les
  autres types sont ignorés. Rebut actif (`gimac.kafka.rebut`, `<sujet>.dlt`) ; configuration Kafka dans le profil `kafka`.
- **Inscription** (`RegistreAuditService`, CC §6.1.3.3), une transaction : déduplication par identifiant CloudEvents
  (`deduplication_entry.event_id`, un identifiant non UUID devient un UUID de nom stable) avant tout calcul ; verrou du
  registre (`pg_advisory_xact_lock`) puis index = dernier + 1 (strictement croissant, sans trou, ordre de réception) ;
  enveloppe reçue mise en forme canonique RFC 8785 (`payload`), feuille `SHA256(0x00 || canon)` (`leaf_hash`) ; acteur =
  `signataireId` (`INCONNU` si absent, l'anomalie est inscrite, jamais écartée), `action_type` `SIGNATURE_PROCESSUS` ou
  `SIGNATURE_OPERATION`, `domain` `pilotage` ou `homologation`, `subject` = type, `occurred_at` = `time` de l'enveloppe
  (sinon `le`), `correlation_id` si c'est un UUID ; entrée de déduplication ; nœuds `merkle_node` de la feuille et des
  sous-arbres complets qu'elle achève (`SHA256(0x01 || gauche || droite)`). Le chaînage est celui de l'arbre (RFC 9162) :
  chaque racine couvre toutes les feuilles précédentes et se recalcule depuis les nœuds (`RegistreAuditService.racine`).
  Contrainte `ux_merkle_node__level_position` (changelog `20261007160000_aud_scellement_signatures.xml`).
- **Domaine** : `ArbreMerkle` (feuille, nœud, racine MTH, nœuds à inscrire, racine depuis les nœuds) et `NatureSignature`,
  purs (règle ArchUnit `registreEstPur`) ; couches `application` et `infrastructure` ajoutées à `TechnicalStructureTest`.

### 11.3 Sujets Kafka

Aucun sujet nouveau. `infra/kafka/creer-sujets-lot1.sh` et sa copie `infra/kubernetes-gimac/partages/kafka/` (Job
`kafka-sujets`) : commentaire des groupes `notification-homologation` et `audit-signatures` seulement.

### 11.4 Reste à faire

- **MS-IAM** : servir `GET /api/v1/interne/collaborateurs?institution=<id>` (comptes de l'institution, champs `id`,
  `institutionId`, `actif`) ; d'ici là, les institutions ne reçoivent aucune notification (sans erreur).
  **Servi** (07/10/2026, `ecrans-par-configuration.md` §6.6), ainsi que le portail : routes `/bff/portail/notification/**`
  ouvertes (OPA, passerelle), `NOT_INBOX_READ` au profil du correspondant, `notification.inapp` relayé au canal du
  portail (destinataire seul).
- **Aucune donnée interne du GIMAC vers le participant** (D-97, PI-GIMAC-01, projections du portail). **Servi**
  (08/10/2026) : la passerelle remet au canal du portail une vue sans `acteur` ni `luJusquA` (`Abonne.vue`), en direct
  et en reprise, et MS-NOTIFICATION répond à un jeton portant `institution_id` sans `acteur` ni `conversationId`
  (`NotificationVue.pourInstitution`) ; `homologation` et `certification` ne relayaient déjà aucun identifiant de membre.
- **Passerelle et OPA** : la boîte de notifications n'est ouverte qu'aux couches `des` et `console` ; ouvrir
  `/bff/portail/notification/**` (route `notification-boite`, règle `boite_notifications` qui refuse aujourd'hui un compte
  d'institution) et relayer `notification.inapp` au canal du portail, pour que les correspondants lisent leurs
  notifications `HOMOLOGATION`.
- **Interface** : pictogramme et interrupteur « Homologation » (type `HOMOLOGATION`, préférence `types.HOMOLOGATION`) dans
  la cloche, la page des notifications et Mon compte ; lecture par référence `homologation`/`OPERATION` ou `LIAISON`.
- **MS-AUDIT** : scellement périodique (racine de période, horodatage RFC 3161, publication), preuves d'inclusion et de
  cohérence, déclencheur base refusant mise à jour et suppression (incompatible avec les tests CRUD générés),
  consommation de tous les sujets à effet : hors de ce lot. Les données factices du contexte `faker` (profil `dev`) ne
  forment pas un arbre valide : vider le registre avant d'y sceller des signatures en développement.

### 11.5 Tests

MS-NOTIFICATION : `ReglesHomologationTest`, `NotificationsHomologationServiceTest`, `EvenementsHomologationConsumerTest`
(idempotence), `IamComptesInstitutionAdapterTest`, `ReglesNotificationsTest` (préférence HOMOLOGATION, causes de
réaffectation) ; intégration `EvenementsHomologationKafkaIT` (Kafka et PostgreSQL réels : trois types, livraison en
double consommée une fois, rôles, comptes de l'institution et liens du portail, fonction à affecter ignorée, préférence
désactivée, `operation-signed` ignoré, publication sur `notification.inapp`). MS-AUDIT : `ArbreMerkleTest` (séparation
de domaine, racine incrémentale de 1 à 33 feuilles), `SignaturesConsumerTest` ; intégration `SignaturesRegistreIT`
(cinq signatures, index 0 à 4, feuille de la forme canonique, racine recalculée, relivraison sans effet ; Kafka réel :
signatures de processus et d'opération, doublon et autre type ignorés, rebut actif). `./mvnw -o verify` : BUILD SUCCESS
dans les deux services.
