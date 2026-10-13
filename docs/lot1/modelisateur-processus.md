# Modélisateur de processus et moteur unique des référentiels (D-77)

Demande de KOAGNE ISSA RYAN : définir de manière exhaustive et entièrement graphique (fenêtres modales)
les opérations d'un référentiel (étapes, rôle de chacune, livrables et preuves avec le rôle qui les
fournit, chronologie, prédécesseurs et successeurs), « comme un BPMN mais en mieux ». Arbitrage : **un seul
moteur**. Le référentiel publié pilote l'exécution, certification comprise ; l'annexe A du CR-CERT-GIMAC-01
v1.4 (aujourd'hui un CSV lu en dur) devient la **version 1 publiée** de chaque type de projet. La création
d'une liaison (opération de certification) ou d'un référentiel crée une tâche (MS-PILOTAGE, D-76) pour la
personne qui doit agir, et lui permet d'affecter les rôles aux membres du GIMAC.

## 1. Modèle (MS-HOMOLOGATION, `reference_operation` étendue)

Une opération du référentiel (nœud du graphe) :

| Champ | Type | Règle |
|---|---|---|
| `code` | texte 1..40, `^[A-Z0-9][A-Z0-9_-]*$` | unique dans la version ; la version 1 reprend les rangs du CR (`3`…`35`, `C1`…, `M1`…, `D1`…) |
| `libelle` | texte 1..300 | en base (fin des clés `parcours.op.<rang>` côté interface ; la version 1 reprend les libellés français actuels) |
| `description` | texte ≤ 4000 | actions attendues |
| `etape` | `EtapeCode` (LANCEMENT … SUIVI, APPROBATION_SECURITE) | remplace `phase` (conservée en lecture) |
| `segment` | AMONT \| BRANCHE \| AVAL | (`Segment` existant, valeurs renommées dans la vue) |
| `ordre` | entier | ordre d'affichage dans l'étape |
| `responsable` | `Fonction` | qualité R, une seule (RG-20) |
| `validateur` | `Fonction` | qualité V, ≠ responsable |
| `consultes`, `informes`, `externes` | listes de `Fonction` | qualités C, I, E |
| `predecesseurs` | liste de codes | arcs du DAG (les successeurs s'en déduisent) |
| `delaiJours` | entier 0..365 | durée ; échéance au plus tôt calculée sur le DAG |
| `grain` | PHASE \| OPERATION | |
| `plateforme` | `PlateformeExecution` | |
| `cle`, `secret`, `oubliSansSymptome`, `pivot` | booléens | `secret` interdit toute saisie de valeur (code de contrôle seulement, D-59) |
| `valeurReference` | texte ≤ 120 \| null | libellé de la valeur à relever |
| `livrables` | liste de `Livrable` | voir ci-dessous |
| `geste` | `Geste` | geste d'exécution, remplace les rangs codés en dur |
| `jalon` | `Jalon` \| null | statut de la liaison atteint à la clôture |
| `declencheur` | sujet d'événement \| null | opération close par un événement d'un autre service |
| `position` | `{x, y}` | placement dans le modélisateur |

`Livrable` : `{ code, libelle, nature: LIVRABLE | PREUVE, deposant: Fonction, typesPieces: ["application/pdf", …],
obligatoire: bool }` (le déposant peut être une fonction externe : le participant dépose depuis le portail).

`Geste` : `DEPOSER_LIVRABLE`, `DEPOSER_PREUVE`, `CONSTATER_COUVERTURE`, `AFFECTER_RESSOURCES`,
`COORDONNEES_TECHNIQUES`, `FICHE_SUIVI`, `EVENEMENT_EXTERNE` (avec `declencheur`), `AUCUN`.
`Jalon` : `APTE_CERTIFICATION`, `CERTIFIEE`, `APPROUVEE_SECURITE`, `EN_PRODUCTION`.

Version 1 (fidèle au CSV) : les gestes, jalons et déclencheurs reproduisent exactement les rangs codés
aujourd'hui (`Parcours`, `ParcoursProjection`, `EvenementsCertification`, simulateur) : 6 →
AFFECTER_RESSOURCES ; 12 → COORDONNEES_TECHNIQUES ; 17 → CONSTATER_COUVERTURE et APTE_CERTIFICATION ;
18 → `certification.pv-established.v1` ; 19 → `certification.pv-signed.v1`, CERTIFIEE ; 20 →
`certification.change-record-constituted.v1` ; 21, 22 → `certification.attestations-archived.v1` ; 23 à 25
→ `certification.security-approved.v1`, APPROUVEE_SECURITE ; 26 → `certification.change-record-referenced.v1`
(et règle actuelle) ; 31 → EN_PRODUCTION ; 32, 33 → FICHE_SUIVI ; grain PHASE ou rangs 7 à 11 →
DEPOSER_LIVRABLE ; sinon DEPOSER_PREUVE. Porteurs : R = premier porteur, C = autres internes, E = externes,
V = CONDUCTEUR_PROJET (ou CHEF_DES si R est le conducteur), I = CHEF_DDRM (règle `raciInitial` actuelle).
Le rang 27 reçoit explicitement pour prédécesseur la fin de chaque branche (fin du raccord implicite
`finBranche`). Livrables : un livrable par opération de grain PHASE ou `pivot`, déposant = R.

## 2. Contrôles de publication

En plus des contrôles existants (complétude, codes, prédécesseurs connus, segments, cycle : Kahn) :
`responsable` et `validateur` présents et distincts ; au moins une opération sans prédécesseur ; chaque
`EVENEMENT_EXTERNE` a un `declencheur` parmi les sujets connus ; chaque `Jalon` au plus une fois, dans
l'ordre APTE_CERTIFICATION < CERTIFIEE < APPROUVEE_SECURITE < EN_PRODUCTION (un jalon ne peut précéder
un jalon antérieur dans le DAG) ; `secret` ⇒ pas de `valeurReference` ; livrables à codes uniques.
Anomalies nouvelles : `RESPONSABLE_ABSENT`, `VALIDATEUR_EGAL_RESPONSABLE`, `DECLENCHEUR_INCONNU`,
`JALON_DUPLIQUE`, `JALON_DESORDONNE`, `SANS_ENTREE`, `LIVRABLE_DUPLIQUE`.

## 3. Moteur unique

- `Liaison` → version PUBLIEE de son type (déjà) ; **l'instanciation lit les opérations de cette version**
  (plus le CSV). `parcours_liaison.version_cr` devient `version_id` + `numero` ; `parcours_operation` garde
  `rang` = `code` et gagne `version_id`.
- Gestes, jalons, déclencheurs et livrables sont lus sur l'opération ; plus aucun rang codé en dur dans
  `Parcours`, `ParcoursProjection`, `EvenementsCertification` (ni dans le simulateur de l'interface).
- Échéances : au plus tôt sur le DAG (`max(échéances des prédécesseurs) + delaiJours`, depuis la date de
  démarrage), jours calendaires comme aujourd'hui.
- **Reprise des données** (Liquibase, une fois) : création, par type de projet, de la version fidèle au
  CSV CR-1.4, publiée (numéro suivant, les versions publiées antérieures sont retirées) ; toutes les liaisons
  du type y sont rattachées et leurs `parcours_operation` reçoivent `version_id` (les codes sont les rangs :
  aucun état d'exécution ne change). Le CSV reste dans le dépôt comme source de cette reprise seulement.
- **Migration** d'une liaison vers une nouvelle version (`HOM_REFERENTIEL_MIGRATE`) : applique le
  différentiel à `parcours_operation` (opérations ajoutées : VERROUILLEE puis éligibilité recalculée ;
  retirées : supprimées si non commencées, sinon conservées en SANS_OBJET ; modifiées : RACI, livrables,
  délai et prédécesseurs mis à jour si l'opération n'est pas FAITE).

## 4. Routes (MS-HOMOLOGATION, passerelle `/bff/des/homologation/referentiels/**`)

Existantes conservées (création de brouillon, dérivation, contrôle, publication, graphe, migration).
Nouvelles ou étendues :

| Méthode | Chemin | Permission |
|---|---|---|
| PUT | `/referentiels/{id}/operations/{code}` (tous les champs §1) | `HOM_REFERENTIEL_EDIT` |
| POST | `/referentiels/{id}/operations` (création, code fourni) | `HOM_REFERENTIEL_EDIT` |
| DELETE | `/referentiels/{id}/operations/{code}` (retire aussi le code des prédécesseurs des autres) | `HOM_REFERENTIEL_EDIT` |
| PUT | `/referentiels/{id}/positions` (`[{code, x, y}]`) | `HOM_REFERENTIEL_EDIT` |
| PUT | `/referentiels/{id}/arcs` (`{de, vers, present: bool}`) | `HOM_REFERENTIEL_EDIT` |
| GET | `/referentiels/{id}/graphe` (étendu : nœuds complets, positions, anomalies, chemin critique) | `HOM_REFERENTIEL_READ` |
| GET | `/referentiels/catalogue` (fonctions avec libellés et profils, gestes, jalons, déclencheurs, étapes, plateformes) | `HOM_REFERENTIEL_READ` |
| PUT | `/referentiels/{id}/pilote` (`{ compteId }`) | `HOM_REFERENTIEL_EDIT` |

Toute écriture sur une version non BROUILLON : 409 `HOM_REFERENTIAL_FROZEN` (existant).

## 5. Tâches (MS-PILOTAGE, `POST /api/v1/interne/taches`, jeton `homologation-service`)

- **Brouillon créé ou dérivé** : le créateur désigne un **pilote** (compte portant `HOM_REFERENTIEL_EDIT`,
  par défaut lui-même) ; tâche au pilote « Compléter et publier le référentiel <type> v<n> », origine
  `REFERENTIEL`, lien `/des/referentiels/operations?version=<id>`. Publiée : tâche terminée.
- **Liaison ouverte** (cadrage, D-58) : tâche au conducteur désigné « Affecter les intervenants de la
  liaison <réf.> » (geste AFFECTER_RESSOURCES : il associe à chaque fonction du référentiel les membres du
  GIMAC qui la tiennent), origine `OPERATION`, lien vers le dossier.
- **Opération devenue éligible** (A_FAIRE) : tâche au membre affecté à la qualité R (s'il est désigné),
  « <libellé de l'opération> (<réf.>) », échéance = échéance de l'opération ; **validation demandée** (EN_VALIDATION) :
  tâche au membre V. Opération FAITE, SANS_OBJET ou réouverte : la tâche correspondante est terminée ou
  rouverte par `POST /api/v1/interne/taches/cloture` et `/reouverture` (`{ origine: {type, reference}, responsableId }`,
  à ajouter dans MS-PILOTAGE, même jeton de service).
- Idempotence par (origine.reference, responsableId) ; échec de MS-PILOTAGE : réessai par l'outbox, jamais
  de blocage du geste métier.

## 6. Interface (couche `des`, écran DES-12 « Référentiels »)

Onglet **Modélisateur** (brouillon : édition ; publiée ou retirée : lecture) sur `@xyflow/react` (MIT) :
couloirs par étape (colonnes) et segment ; nœud = opération (code, libellé, R et V, nombre de livrables,
délai, jalon ou déclencheur en pastille, anomalies en rouge) ; tirer d'un nœud à l'autre crée un
prédécesseur, supprimer un arc le retire ; double-clic ou « Modifier » ouvre la **modale de l'opération**
à onglets (Général ; Rôles R, V, C, I, E ; Livrables et preuves ; Exécution : geste, jalon, déclencheur,
plateforme, grain, clé, secret, valeur de référence ; Chronologie : prédécesseurs, successeurs calculés,
délai, échéance au plus tôt). Barre d'outils : ajouter une opération, disposition automatique (couches de
Kahn), contrôler (anomalies surlignées et listées), comparer, publier (impact), pilote du brouillon.
Mini-carte, zoom, vue liste accessible (clavier, lecteur d'écran) équivalente au canevas. Thèmes clair et
sombre par les jetons du design system.

## 7. Délai cible et délai maximal (D-106, 13/10/2026)

Remarque de KOAGNE ISSA RYAN : chaque opération qui porte un délai doit avoir un délai cible (objectif) et un délai
maximal (au-delà, dépassement).

- **Modèle** : `delaiJours` (colonne `reference_operation.delai_jours`, inchangée) est le **délai cible** ; nouveau champ
  `delaiMaxJours` (`reference_operation.delai_max_jours`, 0..365, `NULL` = égal au délai cible). Les valeurs existantes
  sont reprises comme délai cible, sans autre changement (changelog `20261015000400_delai_maximal.xml`, contrainte
  `ck_reference_operation_delai_max`). Les étapes configurables (`referential_etape`) ne portent pas de délai.
- **Saisie** : `PUT` et `POST …/operations` acceptent `delaiMaxJours` ; inférieur au délai cible (3 jours par défaut) :
  422 `HOM_OPERATION_DELAI_MAX_INVALIDE` (`details.champ = delaiMaxJours`). Recopié par la dérivation, comparé par la
  migration d'une liaison, exporté en dernière colonne du CSV (`delaiMaxJours`).
- **Graphe** : `finAuPlusTot` reste calculé sur les délais cibles (chemin critique inchangé) ; `finAuPlusTotMax` donne la
  fin au plus tôt sur les délais maximaux.
- **Échéances d'une liaison** : `parcours_operation` recopie `delai_max_jours` et gagne `echeance_cible`. `planifier()`
  calcule deux passes sur le DAG : `echeanceCible` (délais cibles) et `echeance` (délais maximaux), qui reste la date du
  dépassement, de la tâche relayée à MS-PILOTAGE, des événements et du taux de respect des délais. Reprise : l'échéance
  cible des opérations existantes est leur échéance. Date cible de mise en production : échéance cible.
- **SLA** : `DEPASSE` après l'échéance maximale ; `ECHEANCE_PROCHE` à cinq jours ou moins de l'échéance cible (ou
  au-delà de la cible sans dépasser le maximum) ; sinon `DANS_LES_DELAIS`. Sans délai maximal distinct, la règle
  antérieure est inchangée. Vue d'une opération : `echeanceCible`, `delaiJours`, `delaiMaxJours`.
- **Interface** : modale de l'opération, onglet Chronologie : « Délai cible » et « Délai maximal » (vide = égal à la
  cible, contrôle maximum >= cible), « Fin au plus tard (délais maximaux) » ; cartes du canevas et vue liste « 3 à 5 j ».
  Services simulés alignés (refus 422, fin au plus tôt maximale). FR, EN, ES.
- **Tests** : `ParcoursTest.delaiCibleEtMaximal`, `ReferentialModelisateurTest` (422 et délai effectif),
  `ReferentielDroitsEtapesIT` (CSV), interface `ModaleOperation.test.tsx`, `services/simule/delais-d104.test.ts`.
