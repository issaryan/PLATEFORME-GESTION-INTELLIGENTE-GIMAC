# Référentiel des opérations : visibilité, droits d'édition, étapes configurables (D-89)

Demande de KOAGNE ISSA RYAN : une personne ne doit voir que les opérations du référentiel qui la concernent ;
le référentiel est modifiable par le chargé d'homologation (ingénieur d'homologation) et le chef du service ou
du département, ainsi que par le super administrateur ; un ingénieur d'homologation ne peut aujourd'hui ni
créer ni modifier d'étapes : les étapes sont une liste figée (`EtapeCode`). Complète D-77.

## 1. Droits

- **Éditer** (créer un brouillon, dériver, modifier, contrôler, publier) : `HOM_REFERENTIEL_EDIT`, porté par
  `ROLE_INGENIEUR_HOMOLOGATION`, `ROLE_CHEF_DES`, `ROLE_CHEF_DDRM`, `ROLE_RESPONSABLE_SERVICE` (chef de service)
  et `ROLE_SUPER_ADMIN` ; `HOM_REFERENTIEL_PUBLISH` : ingénieur d'homologation, chefs de département, super
  administrateur. Tout éditeur peut modifier un brouillon (le « pilote » de D-77 devient le responsable
  affiché et notifié, plus un verrou) ; les écritures concurrentes restent protégées par version (409).
- **Lire tout le référentiel** : les éditeurs et `HOM_REFERENTIEL_READ_ALL` (nouvelle permission : DG,
  conformité, RSSI, chefs de département).
- **Lire ce qui me concerne** : tout autre porteur de `HOM_REFERENTIEL_READ` ne reçoit que les opérations
  dont une de ses fonctions est R, V, C, I ou E, ou déposant d'un livrable (fonctions déduites de ses profils,
  catalogue D-77), plus leurs prédécesseurs et successeurs directs en **fantômes** (code et libellé seulement)
  pour garder la chronologie lisible. Le filtrage est fait par MS-HOMOLOGATION (graphe, liste, export), jamais
  par l'interface seule.

## 2. Étapes configurables

Les étapes deviennent des données de la version du référentiel : `{ code (^[A-Z][A-Z0-9_]{1,39}$), libelle
(1..120), ordre, couleur (jeton du design system), canonique: EtapeCode | null }`. La version 1 reprend les 11
étapes actuelles, chacune liée à son étape canonique. Dans un brouillon, un éditeur ajoute, renomme,
réordonne, recolore une étape, et supprime une étape **vide** ; une étape liée à une étape canonique porteuse
d'un jalon ou du verrou 2 ne se supprime pas (409 `HOM_ETAPE_CANONIQUE`). Une opération référence le code
d'étape de sa version. La progression d'une liaison (étape courante, statuts) passe par l'étape canonique :
une étape ajoutée sans canonique est rattachée, pour la progression, à l'étape canonique de l'étape qui la
précède. Routes (`HOM_REFERENTIEL_EDIT`, brouillon seulement) : `POST /referentiels/{id}/etapes`,
`PUT /referentiels/{id}/etapes/{code}`, `DELETE /referentiels/{id}/etapes/{code}`,
`PUT /referentiels/{id}/etapes/ordre` ; le graphe et le catalogue exposent les étapes de la version.

## 3. Interface

Modélisateur : les couloirs suivent les étapes de la version ; bouton « Étapes » (modale : liste ordonnable,
ajout, renommage, couleur, suppression d'une étape vide). Liste et graphe en lecture filtrée pour les
personnes concernées (mention « Vous voyez les opérations qui vous concernent »). Le menu du référentiel
reste visible de tous les porteurs de `HOM_REFERENTIEL_READ`.

Réalisé (06/10/2026) : couloirs = `graphe.etapes` triées par `ordre`, teintés par leur `jeton` (variable CSS), une
étape vide gardant son couloir ; bouton « Étapes » (modale : liste ordonnable par « Monter » et « Descendre »,
`PUT …/etapes/ordre` ; ajout `POST …/etapes` ; libellé, couleur et canonique `PUT …/etapes/{code}` ; retrait confirmé
d'une étape `supprimable` ; couleurs de `catalogue.couleurs`, canoniques de `etapesCanoniques`, refus `HOM_ETAPE_*`
expliqués) ; sélecteur d'étape de la modale d'opération lu dans `catalogue?version=<id>` (`etape` est une chaîne) ;
nœuds `fantome: true` grisés, ni déplacés ni reliés ni ouverts ; bandeau « Vous voyez les opérations qui vous
concernent » si `filtre === "CONCERNE"` (graphe) ou `X-Gimac-Filtre: CONCERNE` (onglet Opérations, avec la section
« Opérations voisines ») ; bouton « Exporter (CSV) » (`GET …/export`). L'édition dépend seulement de `MODIFIER` et de
`HOM_REFERENTIEL_EDIT` (le pilote est affiché et notifié). Catalogues simulés, matrice des profils et catalogue des
fonctionnalités D-90 portent `HOM_REFERENTIEL_READ_ALL` et les droits du §4 ; le super administrateur édite le
référentiel dans l'application interne. Poignées du canevas décoratives (sans nom), comme pour les processus D-87.

## 4. Mise en œuvre (MS-HOMOLOGATION, OPA, Keycloak, 06/10/2026)

- **Blocage constaté** : le service ne réservait aucune écriture au pilote ni au créateur, mais il annonçait un
  brouillon par `actionsPermises = ["EDITER", "CONTROLER", "PUBLIER"]` alors que l'interface (modélisateur,
  descripteur DES-12) attend `MODIFIER` : le modélisateur restait en lecture seule pour tous. Un brouillon
  annonce désormais `["MODIFIER", "EDITER", "CONTROLER", "PUBLIER"]` (état seulement ; l'interface croise avec les
  permissions). Les pilotes possibles (`gimac.homologation.referentiel.profils-pilote`) sont les cinq profils
  éditeurs ; le pilote reste affiché et destinataire de la tâche, sans verrou.
- **Droits** (Keycloak, `generer-realm-gimac.py`) : `HOM_REFERENTIEL_READ_ALL` ajoutée (79 permissions) ;
  `HOM_REFERENTIEL_READ` et `HOM_REFERENTIEL_EDIT` pour l'ingénieur d'homologation, les chefs du DES et de la
  DDRM, le responsable de service et le super administrateur ; `HOM_REFERENTIEL_PUBLISH` pour l'ingénieur, les
  deux chefs de département et le super administrateur ; `HOM_REFERENTIEL_READ_ALL` pour la DG, la conformité,
  le RSSI et les deux chefs de département.
- **Visibilité** (`VisibiliteReferentiel`) : lecture complète pour `HOM_REFERENTIEL_READ_ALL`, `_EDIT`,
  `_PUBLISH` ou `_MIGRATE` ; sinon fonctions du jeton (`Fonction.desProfils`, plus `CHEFS_DEPARTEMENT` pour un
  chef de département) comparées aux qualités effectives R, V, C, I, E et aux déposants des livrables. Filtrés :
  `GET /referentiels/{id}/graphe` (champ `filtre`, nœuds fantômes `fantome: true` réduits à `code` et `libelle`,
  arcs touchant une opération visible, anomalies, manquants et chemin critique réduits aux opérations visibles),
  `GET /referentiels/{id}/operations` (fantômes compris, en-tête `X-Gimac-Filtre: CONCERNE | COMPLET`),
  `GET /referentiels` et `GET /referentiels/{id}` (`nombreOperations`, `ordreTopologique`, `filtre`) et le nouvel
  export `GET /referentiels/{id}/export` (CSV `;`, même filtre, colonne `fantome`).
- **Étapes** : table `referential_etape` (changelog `20261009000100_referentiel_etapes.xml`, reprise des onze
  étapes dans chaque version existante) ; `reference_operation.etape` porte le code d'une étape de la version
  (422 `HOM_ETAPE_INCONNUE` sinon). Couleurs : `NEUTRE`, `INFO`, `SUCCES`, `ALERTE`, `CRITIQUE`, `ACTION`,
  `ACCENT` (variables `--gm-color-*`, exposées par le catalogue). Étapes canoniques protégées : `CERTIFICATION`,
  `APPROBATION_SECURITE`, `MISE_EN_PRODUCTION` (ni retrait ni changement de canonique : 409 `HOM_ETAPE_CANONIQUE`).
  Étape non vide : 409 `HOM_ETAPE_NON_VIDE` ; code invalide : 422 `HOM_ETAPE_CODE_INVALIDE` ; doublon : 409
  `HOM_ETAPE_CODE_DUPLIQUE` ; ordre qui n'est pas une permutation : 422 `HOM_ETAPE_ORDRE_INVALIDE` ; introuvable :
  404 `HOM_ETAPE_NOT_FOUND`. Progression : `etapeEffective()` passe par la canonique de l'étape, sinon celle de
  l'étape précédente ; une étape sans canonique placée avant le cadrage est rattachée au cadrage (les étapes 1 et
  2 ne sont pas instanciées). La dérivation copie les étapes. Lecture : `GET /referentiels/{id}/etapes`, champ
  `etapes` du graphe, `GET /referentiels/catalogue?version={id}` (`etapes` de la version, `etapesVersion`,
  `etapesCanoniques`, `couleurs`).
- **OPA** : `POST|PUT|DELETE ^/bff/des/homologation/referentiels/[^/]+/etapes(/[^/]+|/ordre)?$` exigent
  `HOM_REFERENTIEL_EDIT` ; la route de la passerelle `/bff/des/homologation/**` couvre déjà ces chemins.
- **Tests** : `EtapesConfigurablesTest` (domaine), `ReferentielDroitsEtapesIT` (ingénieur non créateur, étapes,
  lecture filtrée), règles OPA `test_etapes_referentiel_*`.
