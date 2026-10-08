# Types de projet, fonctions et signatures configurables (D-91)

Demande de KOAGNE ISSA RYAN : l'ingénieur d'homologation doit pouvoir créer un nouveau type de projet et lui
affecter les étapes, les personnes ou rôles de chaque étape, les livrables attendus, les signatures, etc.
Aujourd'hui les types de projet (`TypeProjet` : carte interbancaire, paiement mobile, délégataire) et les
fonctions des rôles R, V, C, I, E (`Fonction`) sont des énumérations figées, et une opération ne porte pas de
signatures. Complète D-77 (modélisateur, moteur unique) et D-89 (droits, étapes configurables).

## 1. Types de projet (MS-HOMOLOGATION, données au lieu d'une énumération)

```jsonc
{ "code": "^[A-Z][A-Z0-9_]{2,39}$", "libelle": "1..120", "description": "0..1000",
  "modele": "CARTE_INTERBANCAIRE" | "PAIEMENT_MOBILE" | "DELEGATAIRE" | "GENERIQUE",
  "actif": true, "referentielActif": { "id", "version" } | null, "creeLe", "creePar", "modifieLe", "modifiePar" }
```
- Les trois types actuels deviennent les trois premiers enregistrements (mêmes codes) ; tout ce qui en dépend
  continue de fonctionner (liaisons, référentiels, attestations, catalogue de services, plateformes, certification,
  participants, interface).
- `modele` porte les comportements jusqu'ici codés par type (attestations exigées, catalogue des services,
  plateforme d'exécution par défaut, projection du parcours) : un nouveau type choisit le modèle dont il hérite,
  ou `GENERIQUE` (aucune attestation imposée, plateforme laissée à chaque opération, services sans catalogue).
- Création (`HOM_REFERENTIEL_EDIT`) : le type naît avec un **brouillon v1** de son référentiel, soit **vierge**
  (les 11 étapes canoniques, aucune opération), soit **copié** de la version active d'un type existant
  (étapes, opérations, rôles, livrables, signatures). Il devient proposable aux liaisons quand une version de son
  référentiel est publiée et qu'il est `actif`.
- Modification du libellé, de la description, de l'état actif (`HOM_REFERENTIEL_EDIT`, motif) ; le code et le modèle
  sont figés après la première publication. Aucune suppression d'un type qui a une liaison ; désactivation seulement.
- Routes (passerelle `/bff/des/homologation/types-projet/**` → `/api/v1/types-projet/**`) : `GET` (liste, `actif=`),
  `GET /{code}`, `POST` (`{ code, libelle, description, modele, depart: { mode: VIERGE | COPIE, typeSource? } }`),
  `PUT /{code}` (`{ libelle, description, actif, motif }`). Lecture : `HOM_REFERENTIEL_READ`.
- Événements : `homologation.project-type-created.v1`, `homologation.project-type-updated.v1` (audit).

## 2. Fonctions des rôles (catalogue configurable)

Les fonctions utilisées pour R, V, C, I, E et les déposants deviennent un catalogue : `{ code, libelle, profils:
[codes de profils IAM], externe: bool, actif }`. Les 16 fonctions actuelles sont reprises à l'identique. Un
éditeur du référentiel crée une fonction et la relie à un ou plusieurs profils (y compris les profils créés par
l'administration, D-81/D-86) ; la recherche des intervenants, la lecture filtrée de D-89 et les tâches suivent ce
catalogue. Routes `/api/v1/referentiels/fonctions` (`GET`, `POST`, `PUT /{code}`), écriture `HOM_REFERENTIEL_EDIT`.

## 3. Signatures d'une opération

Une opération du référentiel gagne `signatures: [ { code, libelle, signataire: Fonction, ordre, porte:
[codes de livrables de l'opération ou d'opérations antérieures] } ]`. Contrôles de publication : codes uniques,
signataire présent dans le catalogue, livrables portés existants et antérieurs. À l'exécution d'une liaison,
l'opération ne se clôt qu'après les signatures dans l'ordre : assertion FIDO2 (finalité `SIGNATURE_OPERATION`,
vérifiée par MS-IAM comme D-87), empreinte SHA-256 des livrables portés, scellement au registre d'audit, tâche
et notification au signataire attendu. Les opérations existantes n'ont aucune signature (comportement inchangé).

## 4. Interface (couche `des`, Référentiels d'opérations)

- Liste des **types de projet** : nouveau type (modale : code, libellé, description, modèle, départ vierge ou
  copie), modification, activation ; chaque type ouvre ses versions de référentiel.
- Modélisateur : onglet **Signatures** dans la modale d'opération ; sélecteurs de rôles et de déposants alimentés
  par le catalogue des fonctions, avec « Nouvelle fonction » (modale : code, libellé, profils, externe).
- Cadrage d'une liaison : le choix du type de projet propose les types actifs dont le référentiel est publié.
- Exécution : panneau de l'opération avec les signatures attendues et le bouton « Signer » (clé de sécurité,
  connexion sécurisée requise).

## 5. Mise en œuvre (MS-HOMOLOGATION, MS-CERTIFICATION, OPA, Keycloak, 06/10/2026)

### 5.1 Types de projet en données

- **Stockage** : table `type_projet` (clé `code`, `^[A-Z][A-Z0-9_]{2,39}$` ; `libelle` 1..120 ; `description` 0..1000 ;
  `modele` ∈ `CARTE_INTERBANCAIRE`, `PAIEMENT_MOBILE`, `DELEGATAIRE`, `GENERIQUE` ; `actif` ; traçabilité ; `version`),
  changelog `20261010000100_types_projet_fonctions_signatures.xml`. Les trois types historiques sont repris (mêmes codes,
  modèle de même nom) dans tous les contextes. Les colonnes `liaison.type_projet` et `referential_version.type_projet`
  restent du texte (`varchar(40)`), désormais clés étrangères vers `type_projet(code)` : aucune valeur ne change.
- **Code Java** : `TypeProjet` (même paquet) n'est plus une énumération mais la valeur d'un code (constantes
  `CARTE_INTERBANCAIRE`, `PAIEMENT_MOBILE`, `DELEGATAIRE` ; `values()` rend ces trois types repris), sérialisée par son
  code : le JSON et la base gardent les mêmes valeurs. Entité `DefinitionTypeProjet`, liée à la liaison par la même
  colonne (`Liaison.modeleTypeProjet()`).
- **Modèle** (`ModeleTypeProjet`) : porte ce qui était codé par type. Attestations exigées (PCI DSS pour carte et
  délégataire ; ISO 27001 et GSMA pour le mobile ; aucune pour `GENERIQUE` ; COBAC ajoutée pour une fintech, quel que
  soit le modèle), catalogue des services proposé au cadrage sans service souscrit (carte, mobile ; aucun pour
  `GENERIQUE` : périmètre `TRANSVERSE` plus les services souscrits), plateforme des essais de l'annexe A (rangs 15 et 16 :
  simulateur pour le mobile, sinon laissée à l'opération), avancement repris du simulateur dans le périmètre (mobile).
  Les anciens appels (`TypeAttestation.exigees`, `CatalogueServices.parType`, `ConversionAnnexeA`,
  `ParcoursProjection.perimetre`, `Parcours.attestationsAFournir`, `OpenLiaisonService.perimetre`) passent par le modèle.
- **Ouverture d'une liaison** : type inconnu 404 `HOM_TYPE_PROJET_INTROUVABLE` ; type désactivé 409
  `HOM_TYPE_PROJET_INACTIF` ; sans version publiée 409 `HOM_REFERENTIAL_NONE_PUBLISHED` (existant).
  `homologation.liaison-opened.v1` gagne `modeleTypeProjet` (champs existants inchangés).
- **MS-CERTIFICATION** ne possède pas la table : il garde le type en texte (colonnes portées à `varchar(40)`) et reçoit le
  modèle par la route interne déjà utilisée, `GET /api/v1/interne/liaisons/{id}/contexte-certification` (jeton
  `certification-service`, `INTERNAL_HOMOLOGATION_READ`), qui gagne `modeleTypeProjet` et `attestationsExigees`.
  Choix : aucune nouvelle route ni nouvelle permission, le contexte étant déjà lu à l'établissement du procès-verbal ; le
  procès-verbal fige le modèle (`proces_verbal_certification.modele_type_projet`, reprise : modèle = code pour les trois
  types repris) et l'approbation de la sécurité en déduit les attestations (`ApprobationPolicy.modele` : modèle reçu,
  sinon type repris de même code, sinon `GENERIQUE` ; sans type, la règle historique de la carte). La route
  `/api/v1/interne/types-projet/{code}` n'est donc pas créée.
- **MS-PARTICIPANT** reçoit déjà le type en texte (`homologation.liaison-opened.v1`, `data.typeProjet`) et ne s'en sert
  que pour le motif de la transition : aucun changement.

### 5.2 Routes des types de projet (`/api/v1/types-projet`, passerelle `/bff/des/homologation/types-projet/**`)

Lecture `HOM_REFERENTIEL_READ`, écriture `HOM_REFERENTIEL_EDIT` ; `REF_METIER_ADMIN` (D-92) vaut les deux.

```jsonc
// GET /types-projet?actif=true|false   → [TypeProjetVue] (types repris d'abord, puis par code)
// GET /types-projet/{code}             → TypeProjetVue
{ "code": "AGREGATEUR", "libelle": "Agrégateur de paiement", "description": null, "modele": "GENERIQUE",
  "libelleModele": "Générique", "actif": true, "referentielActif": { "id": "uuid", "version": 1 } | null,
  "brouillonId": "uuid" | null,          // brouillon le plus récent
  "fige": true,                          // une version a été publiée : code et modèle figés
  "proposable": true,                    // actif et référentiel publié : proposé au cadrage
  "nombreLiaisons": 1, "creeLe": "…", "creePar": "uuid", "modifieLe": "…" | null, "modifiePar": "uuid" | null }
// POST /types-projet → 201, Location /api/v1/types-projet/{code}, corps TypeProjetVue
{ "code": "AGREGATEUR", "libelle": "…", "description": "…", "modele": "GENERIQUE",
  "depart": { "mode": "VIERGE" | "COPIE", "typeSource": "CARTE_INTERBANCAIRE" } }
// PUT /types-projet/{code} → TypeProjetVue ; champs absents gardés
{ "libelle": "…", "description": "…", "modele": "…", "actif": false, "motif": "1..500 (exigé)" }
```

- VIERGE : brouillon v1 « Référentiel <libellé> (version 1) » avec les onze étapes canoniques, sans opération ; COPIE :
  étapes, opérations, rôles, livrables et signatures de la version publiée du type source (`versionSourceId` vide : autre
  type). Le créateur est pilote du brouillon et reçoit la tâche « Compléter et publier le référentiel <libellé> v1 ».
- Le modèle se change par `PUT` tant qu'aucune version n'a été publiée (sinon 409 `HOM_TYPE_PROJET_FIGE`) ; le code ne
  change jamais. Pas de suppression (aucune route `DELETE`) : désactivation.
- Erreurs : 422 `HOM_TYPE_PROJET_CODE_INVALIDE`, `HOM_TYPE_PROJET_LIBELLE_INVALIDE`, `HOM_TYPE_PROJET_DESCRIPTION_INVALIDE`,
  `HOM_TYPE_PROJET_MODELE_INVALIDE`, `HOM_TYPE_PROJET_DEPART_INVALIDE` (mode absent, copie sans `typeSource`),
  `HOM_MOTIF_REQUIS` ; 404 `HOM_TYPE_PROJET_INTROUVABLE` (type ou type source) ; 409 `HOM_TYPE_PROJET_CODE_PRIS`,
  `HOM_TYPE_PROJET_SOURCE_NON_PUBLIE`, `HOM_TYPE_PROJET_FIGE`. Un modèle inconnu dans le JSON : 400.
- Événements (outbox, sujet `homologation.project`, clé = code, journalisés par l'audit) :
  `homologation.project-type-created.v1` `{ code, libelle, description, modele, actif, mode, typeSource, versionSourceId,
  brouillonId, acteurId, le }` ; `homologation.project-type-updated.v1` `{ code, libelle, description, modele, actif,
  changements: ["libelle"|"description"|"modele"|"actif"], motif, acteurId, le }` (rien n'est émis sans changement).
- Référentiels : `POST /referentiels` (brouillon, dérivation) refuse un type inconnu (404 `HOM_TYPE_PROJET_INTROUVABLE`) ;
  `GET /referentiels` sans `type` liste les versions de tous les types ; le catalogue du modélisateur gagne
  `modelesTypeProjet` et `typesProjet` (`{ code, libelle, actif }`). D-92 : `DELETE /referentiels/{id}` supprime un
  brouillon (opérations et étapes comprises, tâche du pilote terminée) ; version publiée ou retirée : 409
  `HOM_REFERENTIAL_FROZEN`.

### 5.3 Catalogue des fonctions (`/api/v1/referentiels/fonctions`)

- Table `referentiel_fonction` (`code` `^[A-Z][A-Z0-9_]{1,39}$`, `libelle` 1..120, `profils` JSON de codes `ROLE_…`
  (20 au plus, au moins un pour une fonction interne), `externe`, `actif`, traçabilité) ; les seize fonctions reprises
  à l'identique (profils, externe, libellés). `Fonction` devient la valeur d'un code (constantes des seize fonctions,
  même JSON, mêmes colonnes) dont libellé, profils et caractère externe sont lus au catalogue (`CatalogueFonctions`,
  relu au plus tard toutes les 30 s et aussitôt après une écriture de l'instance). Tout ce qui servait l'énumération
  suit le catalogue : qualités R, V, C, I, E, déposants, recherche des intervenants (`GET /intervenants?fonction=`, premier
  profil), affectation des ressources (profil exigé), lecture filtrée D-89 (`Fonction.desProfils(jeton)`), tâches,
  catalogue du modélisateur (`fonctions[]` avec `actif`), tableaux de bord (`fonctions=` inconnue : 422
  `HOM_FUNCTION_UNKNOWN`). Une fonction externe créée est tenue par le tiers (`EXT-<code>`).

```jsonc
// GET /referentiels/fonctions?actif=  → [FonctionVue] (reprises d'abord, puis par code) ; GET /referentiels/fonctions/{code}
{ "code": "JURISTE", "libelle": "Juriste", "profils": ["ROLE_JURISTE"], "externe": false, "actif": true,
  "reprise": false, "creeLe": "…", "creePar": "uuid", "modifieLe": null, "modifiePar": null }
// POST → 201 { "code": "JURISTE", "libelle": "Juriste", "profils": ["ROLE_JURISTE"], "externe": false }
// PUT /{code} → FonctionVue { "libelle", "profils", "externe", "actif", "motif" (exigé) } ; champs absents gardés
```

- Erreurs : 422 `HOM_FONCTION_CODE_INVALIDE`, `HOM_FONCTION_LIBELLE_INVALIDE`, `HOM_FONCTION_PROFIL_INVALIDE`,
  `HOM_FONCTION_PROFIL_REQUIS`, `HOM_MOTIF_REQUIS` ; 404 `HOM_FONCTION_INTROUVABLE` ; 409 `HOM_FONCTION_CODE_PRIS`. Pas de
  suppression. L'existence des profils dans MS-IAM n'est pas vérifiée (format seulement) : un profil créé plus tard par
  l'administration devient effectif dès qu'un compte le porte.
- Événements (sujet `homologation.function`) : `homologation.function-created.v1` et `homologation.function-updated.v1`
  `{ code, libelle, profils, externe, actif, changements, motif, acteurId, le }`.

### 5.4 Signatures d'une opération

- Modèle : `reference_operation.signatures` (JSON) ; saisie `signatures: [{ code, libelle, signataire, ordre, porte }]`
  dans `PUT|POST /referentiels/{id}/operations[/{code}]` (absente : signatures conservées ; `[]` : retirées), rendue dans
  l'opération (`OperationVue.signatures`, graphe, liste), copiée par la dérivation et le départ COPIE, exportée en CSV
  (dernière colonne `signatures` : `CODE:SIGNATAIRE:ordre:porte1,porte2`, séparées par `|`). `porte` : code d'un livrable
  de l'opération (`L1`) ou `CODE_OPERATION/CODE_LIVRABLE` d'une opération antérieure (prédécesseur transitif).
- Saisie (422 `HOM_SIGNATURE_INVALIDE`, `details.champ` p. ex. `signatures[0].code`) : 30 signatures au plus, code
  `^[A-Z][A-Z0-9_]{1,39}$`, libellé 1..200, signataire exigé, ordre 1..99 (absent : 1), 30 livrables portés au plus.
- Publication (anomalies nouvelles, refus 409 `HOM_REFERENTIAL_GRAPH_INVALID`, aussi au contrôle à blanc) :
  `SIGNATURE_DUPLIQUEE`, `SIGNATAIRE_INCONNU` (absent du catalogue ou désactivé), `SIGNATAIRE_EXTERNE` (une signature
  d'opération est donnée par un membre du GIMAC : le portail ne signe pas), `SIGNATURE_SANS_LIVRABLE`,
  `SIGNATURE_PORTEE_INVALIDE` (livrable inconnu ou opération non antérieure), et `FONCTION_INCONNUE` pour toute fonction
  citée (R, V, C, I, E, déposant, signataire) absente du catalogue actif.
- Exécution : `parcours_operation.signatures` (modèle recopié) et `signatures_recueillies`. Une opération dont le geste
  aboutit (livrable, validation, constat, événement) passe à l'état **`EN_SIGNATURE`** (nouvel état, compté parmi les
  opérations en cours) si elle exige des signatures ; elle n'est `FAITE` (particularité, ouverture des suivantes) qu'après
  la dernière. Ordre croissant ; même ordre : indifférent. Une migration qui retire toutes les signatures clôt
  l'opération en attente.
- Route : `POST /api/v1/liaisons/{id}/operations/{code}/signatures/{signature}` (sans corps, en-tête
  `X-Reauth-Assertion`, `HOM_LIAISON_READ`) → 204. Signataire admis : la personne qui tient la fonction sur l'opération
  (R, V, C, I, E), sinon la ressource affectée à la liaison, sinon le conducteur ; sans désignation, tout porteur de la
  fonction (profils du jeton ; un chef de département tient aussi `CHEFS_DEPARTEMENT`). L'état et l'ordre sont contrôlés
  avant la réauthentification, qui est vérifiée et consommée par MS-IAM (`POST /api/v1/interne/reauthentifications/
  verification`, `{ assertion, sujet, finalite: "SIGNATURE_OPERATION" }`, jeton `homologation-service` qui porte déjà
  `INTERNAL_IAM_READ` : aucun changement du realm pour cela). Empreinte : SHA-256 (hexadécimal) des lignes
  `REFERENCE:documentId:sha256\n` des livrables portés déposés (pièce de l'opération porteuse), dans l'ordre de `porte`.
- Erreurs : 428 `IAM_REAUTH_REQUIRED` ; 403 `HOM_USER_TOKEN_REQUIRED`, `HOM_SIGNATURE_NON_SIGNATAIRE`, `HOM_REAUTH_INVALID` ;
  404 `HOM_OPERATION_NOT_FOUND`, `HOM_SIGNATURE_INCONNUE` ; 409 `HOM_SIGNATURE_ETAT` (`details.etat`, `details.attendu`),
  `HOM_SIGNATURE_ORDRE` (`details.attendues`), `HOM_SIGNATURE_DEJA_SIGNEE` ; 503 `HOM_IAM_UNAVAILABLE`.
- Lecture (`GET /liaisons/{id}/parcours/operations`) : chaque opération gagne `signatures: [{ code, libelle, ordre,
  signataire, libelleSignataire, porte, etat: "ATTENDUE" | "SIGNEE", suivante, attendu?: { id, nom, fonction }, signePar?,
  signeParNom?, signeLe?, empreinte? }]` et l'action `SIGNER` quand l'acteur peut donner une signature suivante.
- Scellement : `homologation.operation-signed.v1` (sujet `homologation.operation`, clé = liaison) `{ liaisonId,
  reference, institutionId, operationId, rang, libelle, signature, libelleSignature, ordre, fonction, signataireId,
  signataireNom, empreinte, documents: [{ livrable, documentId, sha256 }], restantes, operationClose, lien, le }`,
  journalisé et scellé par le registre d'audit (jamais de contenu de document).
- Tâches (MS-PILOTAGE, comme les autres tâches d'opération) : pour chaque signature du plus petit ordre restant dont la
  fonction est tenue par un membre interne désigné, tâche « Signer : <libellé> (<réf.>) », origine `OPERATION`,
  référence `<réf.>/<code>/S/<signature>`, terminée par la signature ; la tâche du responsable (R) et celle du
  validateur se terminent au passage en `EN_SIGNATURE`. La notification suit la tâche.
- Lecture filtrée D-89 : un signataire est « concerné » par l'opération.

### 5.5 Droits, OPA, passerelle, Keycloak

- OPA (`lot1_authz.rego`) : `GET ^/bff/des/homologation/(types-projet|referentiels/fonctions)(/[^/]+)?$`
  `HOM_REFERENTIEL_READ` ; `POST` sur la collection et `PUT /{code}` `HOM_REFERENTIEL_EDIT` ; `DELETE
  ^/bff/des/homologation/referentiels/[^/]+$` `HOM_REFERENTIEL_EDIT` ; signature `POST
  ^/bff/des/homologation/liaisons/[^/]+/operations/[^/]+/signatures/[^/]+$` `HOM_LIAISON_READ` (choix : la permission
  d'exécution voisine, `HOM_OPERATION_VALIDATE`, n'est pas portée par toutes les fonctions signataires, p. ex. la DG ou la
  conformité ; le service vérifie le signataire attendu). D-92 : `permissions_equivalentes` fait valoir
  `REF_METIER_ADMIN` pour `HOM_REFERENTIEL_READ`, `_EDIT`, `_PUBLISH`, `_MIGRATE`. Tests `test_types_fonctions_*`,
  `test_signature_operation*`, `test_suppression_brouillon`, `test_ref_metier_admin_*`. La route de la passerelle
  `/bff/des/homologation/**` couvre ces chemins et transmet `X-Reauth-Assertion`.
- MS-HOMOLOGATION : `REF_METIER_ADMIN` s'ajoute à chaque `@PreAuthorize` du référentiel, des étapes, des types et des
  fonctions, et ouvre la lecture complète (D-89).
- Keycloak (`generer-realm-gimac.py`, 80 permissions) : `REF_METIER_ADMIN` (« Administration des référentiels métier,
  tous domaines », non privilégiée) pour `ROLE_SUPER_ADMIN` et `ROLE_INGENIEUR_HOMOLOGATION` ; le super administrateur
  porte aussi `HOM_REFERENTIEL_READ_ALL` et `HOM_REFERENTIEL_MIGRATE` (il avait déjà `_READ`, `_EDIT`, `_PUBLISH`).

### 5.6 Tests

`CatalogueTypesFonctionsTest`, `SignaturesOperationTest`, `ValidateurSignaturesTest`, `SignaturesParcoursTest`
(MS-HOMOLOGATION, unitaires) ; `TypesProjetSignaturesIT` (création VIERGE et COPIE, refus, type GENERIQUE de bout en bout
jusqu'à une liaison, fonction créée puis utilisée, signatures ordonnées avec le vérificateur de substitution, refus hors
ordre, publication refusée sur une signature invalide, suppression d'un brouillon par `REF_METIER_ADMIN`) ;
`PolitiquesDecisionTest.attestationsParModele` et `AdaptateursDecisionTest` (MS-CERTIFICATION).
