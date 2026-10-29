# Processus d'unité : opérations propres à chaque service, intervenants, livrables, signatures (D-87)

Demande de KOAGNE ISSA RYAN : le référentiel des opérations est trop orienté certification ; chaque service
doit pouvoir créer ses propres opérations, définir qui intervient à chaque étape, l'action à effectuer, les
livrables à déposer, les signatures numériques exigées ; « un vrai système de gestion d'une unité », les
unités étant elles aussi dynamiques. Arbitrages : **deux moteurs** (la certification garde le moteur D-77 de
MS-HOMOLOGATION ; les processus d'unité ont le leur dans MS-PILOTAGE) ; un intervenant se désigne **par
profil, par personne nommée ou par désignation au lancement** ; une personne nommée ou désignée peut quitter
le GIMAC : chaque intervenant porte donc un **profil de remplacement** qui sert à choisir une nouvelle personne.

## 1. Définition (MS-PILOTAGE)

```jsonc
// Processus (une version) ; versions successives comme D-77 : BROUILLON -> PUBLIEE -> RETIREE
{ "id", "code": "^[A-Z][A-Z0-9_-]{1,39}$", "version": 1, "etat": "BROUILLON" | "PUBLIEE" | "RETIREE",
  "unite": "POLE_RESEAU",                 // unité propriétaire (dynamique, MS-IAM)
  "libelle": "1..200", "description": "0..4000", "categorie": "0..80",
  "delaiCibleJours": 0..365 | null,
  "etapes": [ Etape ], "publieeLe", "publieePar", "creeLe", "creePar" }

// Etape (nœud du graphe)
{ "code": "^[A-Z0-9][A-Z0-9_-]{0,39}$", "libelle": "1..200",
  "action": "0..4000",                    // ce qu'il faut faire
  "intervenant": Intervenant,             // qui exécute
  "validateur": Intervenant | null,       // qui valide (distinct de l'intervenant si personne nommée)
  "predecesseurs": ["CODE", …], "delaiJours": 0..365,
  "livrables": [ { "code", "libelle", "typesPieces": ["application/pdf", …], "obligatoire": true, "modele": "documentId" | null } ],
  "signatures": [ { "code", "libelle", "signataire": Intervenant, "porte": ["CODE_LIVRABLE", …], "ordre": 1 } ],
  "champs": [ { "code", "libelle", "type": "TEXTE" | "NOMBRE" | "DATE" | "CHOIX", "choix": [..], "obligatoire" } ],
  "position": { "x", "y" } }

// Intervenant
{ "mode": "PROFIL" | "PERSONNE" | "AU_LANCEMENT" | "CHEF_UNITE" | "LANCEUR" | "N_PLUS_1_LANCEUR",
  "profil": "ROLE_…" | null,              // PROFIL : un membre de l'unité portant ce profil
  "personneId": "…" | null,               // PERSONNE
  "unite": "CODE" | null,                 // unité où chercher (défaut : unité propriétaire, sous-unités comprises)
  "remplacement": "ROLE_…" | null }       // obligatoire pour PERSONNE et AU_LANCEMENT
```

Contrôles de publication : graphe sans cycle (Kahn), au moins une étape sans prédécesseur, codes uniques,
prédécesseurs connus ; intervenant complet (profil existant ; personne interne active ; `remplacement`
présent pour PERSONNE et AU_LANCEMENT) ; au moins une personne active couvre chaque profil exigé dans l'unité
(sinon avertissement bloquant, levable par motif) ; livrables et signatures à codes uniques ; une signature
ne porte que sur des livrables de son étape ou d'étapes antérieures.

Droits : définir et publier les processus d'une unité = `PROCESSUS_DEFINIR` couvrant l'unité (D-86 §4) ou
`IAM_PROFILE_EDIT` (administration) ; lire = membres de l'unité, ceux qu'elle couvre, et l'administration.

## 2. Exécution

- **Lancer** une version publiée : membre de l'unité ou `PROCESSUS_LANCER` couvrant l'unité. Le lanceur
  donne un intitulé, les valeurs des champs de lancement, et désigne une personne pour chaque intervenant
  `AU_LANCEMENT` (proposées : membres de l'unité portant le profil de remplacement, ou toute personne de sa
  portée). L'instance garde la version utilisée.
- Une étape devient **active** quand tous ses prédécesseurs sont terminés. Elle crée une **tâche**
  (MS-PILOTAGE, origine `PROCESSUS`, échéance = délai de l'étape) : PERSONNE, AU_LANCEMENT, CHEF_UNITE,
  LANCEUR, N_PLUS_1_LANCEUR → tâche à la personne ; PROFIL → tâche **à prendre** (D-81) par les membres de
  l'unité portant le profil.
- **Terminer une étape** : livrables obligatoires déposés (dépôts MS-DOCUMENT, empreinte SHA-256 gardée),
  champs obligatoires remplis, puis **signatures** dans l'ordre : chaque signataire signe l'empreinte des
  livrables portés par sa signature (assertion FIDO2, finalité `SIGNATURE_PROCESSUS`, comme les actes
  sensibles ; le registre d'audit scelle la signature) ; puis validation par le validateur s'il existe
  (valider, ou renvoyer avec motif : l'étape redevient active). Étape terminée → successeurs activés ;
  toutes terminées → instance **close**.
- **Départ ou désactivation** d'une personne (événement `iam.account-administered.v1`, action
  `DESACTIVATION` ou `SUPPRESSION`, ou contrôle à l'activation) : ses étapes actives et ses signatures en attente passent à un membre de l'unité portant le
  profil de remplacement (tâche à prendre), le chef de l'unité est notifié ; même règle pour une personne
  nommée dans la définition (avertissement sur la définition : « personne partie, remplacement par profil »).
- Le chef de l'unité (ou `TACHE_CONFIER`) peut **réaffecter** une étape active, **suspendre** et **annuler**
  une instance (motif). Tout est historisé (qui, quoi, quand).

## 3. Routes (passerelle `/bff/des/pilotage/processus/**` → MS-PILOTAGE `/api/v1/processus/**`)

| Méthode | Chemin | Objet |
|---|---|---|
| GET | `/processus?unite=&etat=&q=` | définitions visibles |
| POST | `/processus` | nouveau processus (brouillon v1) |
| GET, PUT, DELETE | `/processus/{id}` | lire, modifier (brouillon), supprimer (brouillon) |
| POST | `/processus/{id}/controle` | anomalies |
| POST | `/processus/{id}/publication` | publier (motif) |
| POST | `/processus/{id}/nouvelle-version` | brouillon v+1 depuis une version publiée |
| POST | `/processus/{id}/retrait` | retirer (motif) |
| PUT | `/processus/{id}/public` | public d'un brouillon ou d'une version publiée, sans nouvelle version (D-103, `docs/lot1/lancement-processus.md` §11) |
| GET | `/processus/instances?vue=miennes\|unite\|lancees&etat=` | instances |
| POST | `/processus/{id}/instances` | lancer (`{ intitule, champs, designations: { CODE_ETAPE: personneId } }`) |
| GET | `/instances/{id}` | instance (étapes, états, livrables, signatures, historique) |
| POST | `/instances/{id}/etapes/{code}/livrables` | rattacher un dépôt (`{ livrable, documentId }`) |
| PUT | `/instances/{id}/etapes/{code}/champs` | valeurs des champs |
| POST | `/instances/{id}/etapes/{code}/signature` | signer (`{ signature }` + `X-Reauth-Assertion`) |
| POST | `/instances/{id}/etapes/{code}/fin` | terminer l'étape |
| POST | `/instances/{id}/etapes/{code}/validation` | `{ decision: VALIDER \| RENVOYER, motif }` |
| POST | `/instances/{id}/etapes/{code}/reaffectation` | `{ personneId, motif }` |
| POST | `/instances/{id}/suspension`, `/reprise`, `/annulation` | `{ motif }` |

Permission d'entrée `PIL_ACTIVITY_OWN` (OPA) ; la portée est vérifiée par MS-PILOTAGE (pouvoirs D-81/D-86).
Événements (outbox) : `pilotage.process-published.v1`, `pilotage.process-instance-started.v1`,
`pilotage.process-step-activated.v1`, `pilotage.process-step-signed.v1`, `pilotage.process-instance-closed.v1` ;
MS-NOTIFICATION notifie (type `PROCESSUS`) l'intervenant d'une étape activée, le signataire attendu, le
validateur, le lanceur à la clôture.

Limite connue : une signature FIDO2 exige une connexion sécurisée (HTTPS et nom de domaine) ; en http par
l'adresse IP, les étapes à signature ne peuvent pas être terminées.

## 4. Interface (couche `des`, menu « Processus de l'unité »)

- **Catalogue** par unité (mes unités en premier), filtres, état, version.
- **Modélisateur** : le canevas `@xyflow/react` de D-77, rendu générique (composant partagé) : nœud = étape
  (intervenant, livrables, signatures, délai), arcs = prédécesseurs, **modale d'étape** à onglets (Général ;
  Intervenant et validateur ; Livrables ; Signatures ; Champs ; Chronologie), contrôle, publication,
  versions, vue liste accessible, clair et sombre.
- **Lancement** en modale (désignations, champs).
- **Suivi d'une instance** : graphe coloré par état, panneau de l'étape active (action, livrables à déposer,
  champs, signatures attendues avec bouton « Signer », validation), historique ; « Mes étapes » dans Mon
  activité et les tâches.

**Réalisation de l'interface (frontend).**
- Menu « Processus de l'unité » (`/des/processus`, groupe Mon activité, `PIL_ACTIVITY_OWN`) ; onglets Catalogue
  (groupé par unité, les miennes d'abord ; filtres unité, état, recherche) et Instances (vues `miennes`, `unite`,
  `lancees`, filtre d'état). « Nouveau processus » n'apparaît qu'avec `PROCESSUS_DEFINIR` (portée `UNITES` sur une
  unité dirigée) ; les boutons d'une définition suivent `droits` et ceux d'une étape suivent `moi`.
- Modélisateur (`/des/processus/{id}`) : le canevas `@xyflow/react` est extrait en composant générique
  (`CanevasGraphe`), partagé avec le modélisateur des référentiels (D-77, inchangé pour l'utilisateur). Chaque
  geste (ajout, déplacement, liaison, modale) enregistre la définition entière (`PUT`). Modale d'étape à six
  onglets ; une erreur de forme ouvre l'onglet concerné ; profil de remplacement obligatoire pour `PERSONNE` et
  `AU_LANCEMENT`. Contrôle : anomalies listées par gravité, un clic ouvre l'étape et la surligne. Publication :
  confirmation, puis sur 422 `PIL_PROCESSUS_AVERTISSEMENTS` modale « Lever les avertissements » avec motif
  (`{ motif, leverAvertissements: true }`). Nouvelle version, choix de version, retrait motivé, suppression d'un
  brouillon. Vue liste équivalente au clavier.
- Lancement en modale : intitulé, champs de départ typés (nombre, date JJ/MM/AAAA envoyée en AAAA-MM-JJ, choix),
  une désignation par clé `CODE_ETAPE`, `CODE_ETAPE.validateur`, `CODE_ETAPE.signature.CODE` (porteurs du profil
  de remplacement proposés d'abord).
- Suivi (`/des/processus/instances/{id}`) : graphe en lecture colorée par état et légende, panneau de l'étape
  (prise de la tâche par `POST /pilotage/taches/{id}/prise`, livrables déposés par le dépôt fragmenté de
  MS-DOCUMENT, finalité `PROCESSUS_UNITE`, référence = l'instance, champs, signatures dans l'ordre avec
  l'empreinte SHA-256 calculée sur les livrables portés, fin, validation ou renvoi motivé, réaffectation), actions
  de l'instance (suspendre, reprendre, annuler) et historique. Signature : défi de finalité `SIGNATURE_PROCESSUS`,
  assertion dans `X-Reauth-Assertion`. Hors contexte sécurisé (plateforme ouverte en http par adresse IP), la clé
  de sécurité est indisponible : le panneau l'explique et désactive « Signer ».
- « Mes étapes » : carte de « Ma journée » (étapes en cours de la vue `miennes`) ; dans les tâches, origine
  `PROCESSUS`, ligne « Profil requis » pour une tâche à prendre, transitions Terminée et Annulée masquées (la tâche
  suit l'étape).
- Notifications de type `PROCESSUS` (et `DEMANDE`) : icône, libellé et préférence dans Mon compte, lien suivi tel
  que servi.
- Services simulés : démonstration « Ouverture d'un flux réseau » du pôle réseau (quatre étapes, une instance
  à signer par le chef d'unité, une étape à prendre), état commun à toutes les personnes de l'organisation
  simulée.

## 5. Mise en œuvre (MS-PILOTAGE)

Précisions apportées par la réalisation, sans changer le sens des §1 à §3.

### 5.1 Routes et formes

Toutes les routes vivent sous `/api/v1/processus` (passerelle `/bff/des/pilotage/processus/**`) :
définitions `/api/v1/processus[/{id}[/controle|/publication|/nouvelle-version|/retrait|/instances]]`, instances
`/api/v1/processus/instances[/{id}/…]`. `@PreAuthorize("hasAuthority('PIL_ACTIVITY_OWN')")` partout ; les droits fins
sont vérifiés par le cas d'utilisation (403 `PIL_PROCESSUS_INTERDIT`).

- `GET /processus?unite=&etat=&q=` : tableau de résumés `{ id, code, version, etat, unite, libelle, description,
  categorie, delaiCibleJours, nombreEtapes, publieeLe, modifieLe, modifiePar, droits }`, une ligne par version visible
  (brouillons : seulement pour qui peut définir). Administration (`IAM_PROFILE_EDIT`) : toutes les unités.
- `POST /processus` (201, `Location`), `PUT /processus/{id}` : `{ code, unite, libelle, description, categorie,
  delaiCibleJours, etapes: [Etape] }` au format du §1 (`code` et `unite` immuables ensuite). Un brouillon peut être
  incomplet ; les formats sont contrôlés (400 `PIL_REQUETE_INVALIDE`, `details.champ` = chemin, par exemple
  `etapes[1].intervenant.mode`). Bornes : 100 étapes, 30 livrables, signatures ou champs par étape, 20 types de pièces,
  50 choix, ordre de signature 1..99, délais 0..365. Seuls les attributs du mode sont gardés dans un intervenant.
  `obligatoire` d'un livrable vaut `true` par défaut, celui d'un champ `false` ; `ordre` vaut 1 par défaut.
- Définition rendue (`GET /processus/{id}` et réponses d'écriture) :

```jsonc
{ "id", "code", "version", "etat", "unite", "libelle", "description", "categorie", "delaiCibleJours",
  "etapes": [ Etape ],                                    // exactement le §1
  "creeLe", "creePar": { "id", "nomAffiche" }, "modifieLe", "modifiePar",
  "publieeLe", "publieePar", "motifPublication", "avertissementsLeves": false,
  "retireeLe", "retireePar", "motifRetrait",
  "droits": { "modifier", "publier", "lancer", "retirer" },
  "versions": [ { "id", "version", "etat" } ],           // du même code, la plus récente d'abord
  "avertissements": [ Anomalie ],                         // version publiée : PERSONNE_PARTIE
  "personnes": { "<personneId>": "<nomAffiche>" },        // personnes nommées dans les intervenants
  "historique": [ { "quand", "acteur", "action", "etape", "detail" } ] }
// Anomalie
{ "code", "gravite": "BLOQUANTE" | "LEVABLE" | "AVERTISSEMENT", "etape", "element", "message" }
```

- `POST /processus/{id}/controle` → `{ publiable, anomalies: [Anomalie] }`. Codes : `AUCUNE_ETAPE`, `CODE_DUPLIQUE`,
  `PREDECESSEUR_INCONNU`, `AUCUNE_ETAPE_INITIALE`, `CYCLE` (Kahn ; `element` : étapes du cycle), `INTERVENANT_ABSENT`,
  `INTERVENANT_INCOMPLET`, `REMPLACEMENT_ABSENT`, `VALIDATEUR_IDENTIQUE`, `LIVRABLE_CODE_DUPLIQUE` (unique dans le
  processus), `SIGNATURE_CODE_DUPLIQUE`, `CHAMP_CODE_DUPLIQUE`, `CHAMP_CHOIX_ABSENT`, `SIGNATURE_SANS_LIVRABLE`,
  `SIGNATURE_PORTEE_INVALIDE`, `UNITE_INCONNUE`, `PROFIL_INCONNU`, `PERSONNE_INCONNUE`, `PERSONNE_INACTIVE` (bloquants) ;
  `COUVERTURE_PROFIL` (aucune personne active ne porte le profil, ou le profil de remplacement, dans l'unité) et
  `CHEF_ABSENT` (intervenant CHEF_UNITE, unité sans responsable) : levables ; `PERSONNE_PARTIE` : avertissement d'une
  version publiée. `element` : `intervenant`, `validateur`, `signature.CODE`, suivi de `.remplacement` le cas échéant.
- `POST /processus/{id}/publication` `{ motif?, leverAvertissements? }` : anomalie bloquante → 422
  `PIL_PROCESSUS_ANOMALIES` (`details.anomalies`) ; anomalie levable sans `leverAvertissements: true` → 422
  `PIL_PROCESSUS_AVERTISSEMENTS` ; levée sans motif → 400 (`motif`, 1..1000). La version publiée précédente du même
  code est retirée (« Remplacée par la version N »). Une seule version BROUILLON et une seule PUBLIEE par code (index
  uniques).
- `POST /processus/{id}/nouvelle-version` (201) depuis une version publiée ou retirée : brouillon `max(version)+1` ;
  un brouillon existe déjà → 409 `PIL_PROCESSUS_BROUILLON_EXISTANT`. `POST …/retrait` `{ motif }` (obligatoire).
  `DELETE` : brouillon seulement (204). Hors brouillon, modifier ou supprimer → 409 `PIL_PROCESSUS_ETAT`.
- `PUT /processus/{id}/public` (D-103) : seul le public se règle sur une version publiée, sans nouvelle version ;
  contrat dans `docs/lot1/lancement-processus.md` §11.
- `POST /processus/{id}/instances` (201) : `{ intitule, champs: { CODE_CHAMP: valeur }, designations: { cle:
  personneId } }`. `champs` : valeurs des champs des étapes initiales (sans prédécesseur), par code. Clés des
  désignations : `CODE_ETAPE` (exécutant), `CODE_ETAPE.validateur`, `CODE_ETAPE.signature.CODE`. Personne désignée
  admise : compte interne actif, et lanceur lui-même, ou porteur du profil de remplacement dans l'unité, ou couvert
  par la portée `TACHE_CONFIER` du lanceur.
- `GET /processus/instances?vue=miennes|unite|lancees&etat=&page=&taille=` → `{ contenu, total, page, taille }`
  (20 par défaut, 100 au plus) de résumés `{ id, processus: { id, code, version, libelle }, unite, intitule, etat,
  lanceur, lanceeLe, closeLe, avancement: { total, terminees }, etapesEnCours: [ { code, libelle, etat, echeance } ] }`.
  `miennes` : je suis ou ai été exécutant (responsable de la tâche), validateur nommé ou signataire (attendu ou
  ayant signé) ; `unite` : instances des unités de mon chemin et de mes unités dirigées (administration : toutes).
- Instance rendue (`GET /processus/instances/{id}` et toutes les réponses d'actes) :

```jsonc
{ "id", "processus": { "id", "code", "version", "libelle" }, "unite", "intitule",
  "etat": "EN_COURS" | "SUSPENDUE" | "CLOSE" | "ANNULEE",
  "lanceur": { "id", "nomAffiche" }, "lanceeLe", "closeLe", "motif",
  "designations": { "MISE_EN_OEUVRE": { "id", "nomAffiche" } },
  "avancement": { "total", "terminees" },
  "etapes": [ { "code", "libelle", "action", "predecesseurs", "position", "delaiJours",
      "etat": "EN_ATTENTE" | "ACTIVE" | "A_SIGNER" | "A_VALIDER" | "TERMINEE" | "ANNULEE",
      "activeeLe", "echeance", "soumiseLe", "termineeLe", "enRetard", "renvois", "tacheId",
      "executant": { "id", "nomAffiche" } | null, "aPrendre": { "unite", "profil" } | null,
      "validateur": { "personne": { "id", "nomAffiche" } | null, "profil", "unite" } | null,
      "livrables": [ { "code", "libelle", "obligatoire", "typesPieces", "modele",
                       "depot": { "documentId", "nomFichier", "typeMime", "taille", "sha256", "deposePar", "deposeLe" } | null } ],
      "champs": [ { "code", "libelle", "type", "choix", "obligatoire", "valeur" } ],   // NOMBRE : nombre JSON
      "signatures": [ { "code", "libelle", "ordre", "porte", "attendu": { "personne", "profil", "unite" },
                        "etat": "ATTENDUE" | "SIGNEE", "signePar", "signeLe", "empreinte",
                        "documents": [ { "livrable", "documentId", "sha256" } ] } ],
      "moi": { "executer", "prendre", "signer": ["CODE"], "valider", "reaffecter" } } ],
  "historique": [ { "quand", "acteur", "action", "etape", "detail" } ],
  "droits": { "suspendre", "reprendre", "annuler", "reaffecter" } }
```

- Corps des actes : livrables `{ livrable, documentId }` ; champs `PUT … { valeurs: { CODE: valeur | null } }` (TEXTE
  4000 au plus, NOMBRE décimal, DATE `AAAA-MM-JJ`, CHOIX parmi `choix`) ; signature `{ signature: "CODE" }` + en-tête
  `X-Reauth-Assertion` ; validation `{ decision: VALIDER | RENVOYER, motif }` (motif obligatoire pour RENVOYER) ;
  réaffectation `{ personneId, motif }` ; suspension et annulation `{ motif }` (obligatoire), reprise `{ motif? }`.

### 5.2 Droits

Définir, publier, versions, retrait : `REF_METIER_ADMIN`, `PROCESSUS_DEFINIR` de portée `TOUTES`, ou de portée `UNITES`
sur une unité dirigée qui comprend l'unité (`IAM_PROFILE_EDIT` n'y suffit plus depuis D-94). Lire : administration,
membre de l'unité ou d'une sous-unité (chemin MS-IAM), responsable d'une unité qui la comprend, porteurs de
`PROCESSUS_DEFINIR` ou `PROCESSUS_LANCER` la couvrant ; une instance est aussi lisible de ses intervenants ; une version
publiée l'est aussi de son public, réduite à elle seule (D-106). Lancer : le public du processus seul (règles
d'initiateurs, D-100) ; règle de synthèse : `docs/lot1/lancement-processus.md` §12.
Réaffecter, suspendre, reprendre, annuler : chef d'une unité qui comprend l'unité (unité dirigée) ou `TACHE_CONFIER`
la couvrant. Agir sur une étape : l'exécutant (responsable de sa tâche) ; signer : le signataire attendu (personne, ou
membre actif de l'unité portant le profil) ; valider : le validateur, jamais l'exécutant de l'étape. Les actions
`PROCESSUS_DEFINIR` et `PROCESSUS_LANCER` absentes de la réponse de MS-IAM valent `AUCUNE`.

### 5.3 Exécution

- Une étape activée crée une tâche d'origine `PROCESSUS` (référence `<instance>/<ETAPE>`, service `pilotage`, lien
  `/des/processus/instances/<id>?etape=<ETAPE>`, créateur : le lanceur, échéance : jour + `delaiJours`). Personne
  résolue : tâche OUVERTE à cette personne. PROFIL (ou remplacement) : tâche À PRENDRE (`uniteCible`, nouveau champ
  `profilRequis` rendu par les tâches) : seuls les porteurs du profil la voient dans `GET /taches?vue=unite` et la
  prennent (`POST /taches/{id}/prise`, sinon 403 `PIL_PROFIL_REQUIS`) ; l'attribution par le chef reste possible.
  L'exécutant d'une étape est le responsable de sa tâche (aucune double saisie) ; la prise est historisée dans
  l'instance (`ETAPE_PRISE`).
- La tâche suit l'étape : terminée quand l'étape est terminée (`pilotage.task-completed.v1`), annulée avec
  l'instance ; la terminer ou l'annuler à la main, ou la réaffecter par `PUT /taches/{id}`, est refusé (409
  `PIL_TACHE_PROCESSUS`). EN_COURS et BLOQUEE restent libres. Le renvoi par le validateur garde la tâche ouverte.
- Résolution : PERSONNE, AU_LANCEMENT, CHEF_UNITE (responsable de l'unité de l'intervenant, à défaut l'unité
  propriétaire), LANCEUR, N_PLUS_1_LANCEUR (n+1 au lancement) → la personne si elle est interne et active ; sinon
  tâche à prendre par le profil de remplacement (à défaut, par tout membre de l'unité), historique `DEPART` et
  événement `pilotage.process-step-reassigned.v1` (cause `DEPART`) au chef de l'unité. Validateur et signataires
  sont résolus à l'activation de l'étape.
- Livrable : document déposé par l'exécutant dans MS-DOCUMENT, finalité `PROCESSUS_UNITE`, référence = identifiant de
  l'instance, vérifié par `GET /api/v1/interne/documents/{id}` (jeton `pilotage-service`, `INTERNAL_DOCUMENT_READ`) ;
  l'empreinte SHA-256 conservée est celle de MS-DOCUMENT. Sinon 422 `PIL_PROCESSUS_DOCUMENT_INVALIDE` ; type hors
  `typesPieces` (`type/*` admis) : 422 `PIL_PROCESSUS_TYPE_PIECE`. Un nouveau dépôt remplace le précédent.
- Fin (`…/fin`, étape ACTIVE) : livrables obligatoires (422 `PIL_PROCESSUS_LIVRABLES_MANQUANTS`) et champs
  obligatoires (422 `PIL_PROCESSUS_CHAMPS_MANQUANTS`), puis A_SIGNER, A_VALIDER ou TERMINEE.
- Signature : par ordre croissant (409 `PIL_PROCESSUS_SIGNATURE_ORDRE` ; même ordre : indifférent). Sans en-tête :
  428 `PIL_PROCESSUS_REAUTH_REQUISE` ; MS-IAM vérifie l'assertion (`POST /api/v1/interne/reauthentifications/verification`
  `{ assertion, sujet, finalite: "SIGNATURE_PROCESSUS" }`, jeton `pilotage-service`, comme la passerelle) ; refus : 403
  `PIL_PROCESSUS_REAUTH_REFUSEE` ; MS-IAM injoignable : 503 `PIL_REAUTH_INDISPONIBLE`. Empreinte signée : SHA-256
  (hexadécimal) des lignes `CODE_LIVRABLE:documentId:sha256\n` des livrables portés déposés, dans l'ordre de `porte`.
- Renvoi : étape ACTIVE, signatures remises en attente, `renvois` + 1. Toutes étapes terminées : instance CLOSE.
- Suspension : tout acte sur les étapes rend 409 `PIL_PROCESSUS_INSTANCE_ETAT` ; les tâches ne changent pas.
- Départ : consommateur Kafka (groupe `pilotage-processus-comptes`, sujet `iam.account`, type
  `iam.account-administered.v1`, `data.action` DESACTIVATION ou SUPPRESSION, `data.compteId`), idempotent par
  l'identifiant CloudEvents (table `processed_event`). Tâches d'étapes ouvertes du compte : remises à prendre par le
  profil de remplacement de l'intervenant (`pilotage.task-offered.v1`) ; validations et signatures attendues de la
  personne : reprises par le profil de remplacement du validateur ou du signataire ; historique `DEPART`.
- Stockage : tables `processus` (graphe en JSONB), `processus_instance`, `processus_etape_instance` (dépôts, valeurs,
  signatures, validateur en JSONB), `processus_historique` (ajout seul, déclencheur) ; `tache.profil_requis`
  (changelog `20261007090000_pil_processus_unite.xml`).
- Démonstration : avec `gimac.demonstration.processus=true` (profil dev), l'organisation simulée gagne le pôle réseau
  (chef Brice Onana, ingénieurs portant `ROLE_POLE_RESEAU`) et le processus publié `OUVERTURE_FLUX_RESEAU` v1
  « Ouverture d'un flux réseau » : DEMANDE (LANCEUR, champs SOURCE, DESTINATION, PORT, PROTOCOLE, DATE_SOUHAITEE),
  ANALYSE (PROFIL `ROLE_POLE_RESEAU`, validée par CHEF_UNITE, note d'analyse), MISE_EN_OEUVRE (AU_LANCEMENT,
  remplacement `ROLE_POLE_RESEAU`, configuration), RECETTE (PROFIL, procès-verbal signé par CHEF_UNITE, signature
  portant PV_RECETTE et NOTE_ANALYSE).

### 5.4 Codes d'erreur (problem+json, champ `code`)

400 `PIL_REQUETE_INVALIDE` (`details.champ`) ; 403 `PIL_PROCESSUS_INTERDIT`, `PIL_PROCESSUS_NON_INTERVENANT`,
`PIL_PROCESSUS_NON_SIGNATAIRE`, `PIL_PROCESSUS_NON_VALIDATEUR`, `PIL_PROCESSUS_REAUTH_REFUSEE`, `PIL_PROFIL_REQUIS` ;
404 `PIL_PROCESSUS_INTROUVABLE`, `PIL_PROCESSUS_INSTANCE_INTROUVABLE`, `PIL_PROCESSUS_ETAPE_INCONNUE`,
`PIL_PROCESSUS_LIVRABLE_INCONNU`, `PIL_PROCESSUS_SIGNATURE_INCONNUE` ; 409 `PIL_PROCESSUS_CODE_PRIS`,
`PIL_PROCESSUS_ETAT`, `PIL_PROCESSUS_BROUILLON_EXISTANT`, `PIL_PROCESSUS_NON_PUBLIE`, `PIL_PROCESSUS_INSTANCE_ETAT`,
`PIL_PROCESSUS_ETAPE_ETAT` (`details.etat`, `details.attendu`), `PIL_PROCESSUS_SIGNATURE_ORDRE`,
`PIL_PROCESSUS_DEJA_SIGNEE`, `PIL_PROCESSUS_DEJA_AFFECTEE`, `PIL_PROCESSUS_CONCURRENCE`, `PIL_TACHE_PROCESSUS` ; 422
`PIL_PROCESSUS_UNITE_INCONNUE`, `PIL_PROCESSUS_ANOMALIES`, `PIL_PROCESSUS_AVERTISSEMENTS` (`details.anomalies`),
`PIL_PROCESSUS_DESIGNATION_MANQUANTE` (`details.cles`), `PIL_PROCESSUS_DESIGNATION_INVALIDE`,
`PIL_PROCESSUS_PERSONNE_INVALIDE`, `PIL_PROCESSUS_DOCUMENT_INVALIDE`, `PIL_PROCESSUS_TYPE_PIECE`,
`PIL_PROCESSUS_CHAMP_INVALIDE`, `PIL_PROCESSUS_LIVRABLES_MANQUANTS`, `PIL_PROCESSUS_CHAMPS_MANQUANTS` ; 428
`PIL_PROCESSUS_REAUTH_REQUISE` ; 503 `PIL_ORGANISATION_INDISPONIBLE`, `PIL_DOCUMENT_INDISPONIBLE`,
`PIL_REAUTH_INDISPONIBLE`.

### 5.5 Événements (outbox, CloudEvents, sujet `pilotage.process`)

Clé : la version publiée pour `process-published`, l'instance pour les autres. `lien` : `/des/processus/instances/<id>`
(`?etape=<CODE>` pour une étape). Jamais de contenu de document ni de valeur de champ.

| Type | Charge | Destinataires |
|---|---|---|
| `pilotage.process-published.v1` | `processusId, code, version, unite, libelle, acteurId, motif, avertissementsLeves, versionRetiree, le` | aucun |
| `pilotage.process-instance-started.v1` | `instanceId, processusId, code, version, unite, intitule, lanceurId, destinataires, lien, le` | chef de l'unité (sauf lanceur) |
| `pilotage.process-step-activated.v1` | `instanceId, processusId, code, intitule, etape, libelleEtape, tacheId, executantId, profil, unite, echeance, renvoi, motif, destinataires, lien, le` | exécutant, ou porteurs actifs du profil (à défaut le chef) ; renvoi : l'exécutant |
| `pilotage.process-step-submitted.v1` (ajout) | `instanceId, code, intitule, etape, libelleEtape, attente: SIGNATURE \| VALIDATION, signatures, acteurId, destinataires, lien, le` | signataires attendus ou validateur |
| `pilotage.process-step-signed.v1` | `instanceId, code, intitule, etape, libelleEtape, signature, signataireId, empreinte, documents: [{livrable, documentId, sha256}], etatEtape, destinataires, lien, le` | signataires suivants, validateur, ou exécutant |
| `pilotage.process-step-reassigned.v1` (ajout) | `instanceId, code, intitule, etape, libelleEtape, cause: REAFFECTATION \| DEPART, roles, ancienId, executantId, profil, unite, chefId, acteurId, motif, destinataires, lien, le` | nouvel exécutant ; départ : porteurs du profil de remplacement et chef |
| `pilotage.process-instance-closed.v1` | `instanceId, processusId, code, intitule, etat: CLOSE \| ANNULEE, lanceurId, acteurId, motif, destinataires, lien, le` | lanceur (sauf acteur) |

`libelleEtape` (libellé de l'étape dans la version exécutée) accompagne `etape` dans toutes les charges propres à une
étape (`process-step-*`) ; MS-NOTIFICATION l'utilise pour titrer, à défaut le code `etape`.

Les tâches liées émettent aussi `pilotage.task-created.v1`, `task-assigned`, `task-offered`, `task-completed`.

### 5.6 Dépendances

- **MS-IAM** (jeton de service) : l'unité est lue d'abord par `GET /api/v1/interne/organisation/unites/{code}`
  (`{ code, libelle, type, parent, active, responsableId }`, 404 `ORG_UNITE_INTROUVABLE` : unité inconnue ; libellé,
  état et chef de l'unité, même sans membre ; mise en cache 60 s), puis ses membres par
  `GET /api/v1/interne/organisation/unites/{code}/membres?sousUnites=true&profil=` (tableau
  `{ id, nomAffiche, fonction, unite, responsableId, actif, profils }`) : une unité connue sans membre rend ainsi son
  chef et une liste vide ; toute autre erreur de MS-IAM : 503 `PIL_ORGANISATION_INDISPONIBLE` ; un profil inconnu rend une liste vide (signalé
  `COUVERTURE_PROFIL`, pas `PROFIL_INCONNU`) ; profils d'un compte par `GET /api/v1/interne/collaborateurs/{id}` ;
  `PROCESSUS_DEFINIR` et `PROCESSUS_LANCER` dans `/interne/pouvoirs/{id}` ; défi FIDO2 de finalité
  `SIGNATURE_PROCESSUS` accepté.
- **MS-DOCUMENT** : finalité de dépôt fragmenté `PROCESSUS_UNITE` ouverte à `PIL_ACTIVITY_OWN` (référence =
  identifiant de l'instance).
- **MS-NOTIFICATION** : type `PROCESSUS` pour les événements du §5.5 portant des destinataires (lien rendu tel quel).
- **Passerelle et OPA** : `/bff/des/pilotage/processus/**` vers `/api/v1/processus/**`, `PIL_ACTIVITY_OWN`, comptes
  internes ; en-tête `X-Reauth-Assertion` transmis ; méthodes GET, POST, PUT, DELETE.
- **Configuration** : `gimac.adaptateurs.reauthentification` (`stub` par défaut, `real` en production),
  `gimac.consommateurs.comptes.sujet` (`iam.account`), `gimac.demonstration.processus`.


## 6. Désignation hors de l'unité (08/10/2026)

Signalement : affecter une étape à un profil que personne ne porte dans l'unité désignée échouait toujours
(« aucune personne active ne porte le profil … dans l'unité … »), alors que des porteurs existaient ailleurs dans
le GIMAC. Cause : MS-PILOTAGE ne cherchait les porteurs que dans l'unité de l'intervenant (contrôle de publication,
résolution à l'activation, désignation au lancement, réaffectation). Le moteur cherche désormais partout.

**Résolution d'un intervenant par profil** (mode `PROFIL`, ou profil de remplacement d'une personne nommée, désignée,
chef, lanceur ou n+1 absents ou partis) :

1. porteurs actifs du profil dans l'unité de l'intervenant (sous-unités comprises) ;
2. à défaut, porteurs actifs dans tout le GIMAC (`GET /api/v1/interne/collaborateurs?profil=` de MS-IAM, unité de
   rattachement lue dans la réponse ; un compte d'une unité inactive est écarté, un compte sans rattachement est
   gardé) ;
3. **un seul porteur** (dans l'unité ou ailleurs) : l'étape lui est confiée d'office (tâche `OUVERTE`) ;
   **plusieurs** : tâche à prendre, dans l'unité (`uniteCible` = l'unité) ou, s'ils sont hors de l'unité, sans
   unité visée (`uniteCible` nulle, `profilRequis` = le profil) : chacun la voit dans « Mes tâches » et la prend ;
   le chef de l'unité (ou `TACHE_CONFIER`) et le **lanceur** peuvent aussi choisir la personne (réaffectation) ;
4. **aucun porteur nulle part** : le lancement n'échoue pas ; l'étape est confiée au chef de l'unité, à défaut au
   lanceur, pour désigner une personne (historique : « Aucune personne active ne porte le profil … dans le GIMAC :
   étape confiée au chef de l'unité … pour désigner une personne »).

Les validateurs et signataires suivent la même règle ; leur tâche d'attente hors de l'unité est à prendre sans unité
visée. L'historique de l'instance dit chaque fois ce qui a été fait (« confiée à …, seul porteur actif du profil …,
hors de l'unité … », « à prendre par les 3 porteurs du profil …, hors de l'unité … (SNA : 2, DDRM : 1) »). La revue
d'organisation (D-93 §9.3) garde le titulaire qui porte le profil ailleurs tant que personne ne le porte dans l'unité.

**Contrôle de publication** : `COUVERTURE_PROFIL` (levable) seulement si personne ne porte le profil dans tout le
GIMAC ; si des porteurs existent hors de l'unité, nouvelle anomalie `COUVERTURE_HORS_UNITE`, gravité `AVERTISSEMENT`
(information, ne bloque pas la publication et n'exige pas de motif), message avec le nombre de porteurs et leurs
unités. Une version publiée rend ces informations dans `avertissements` (avec `PERSONNE_PARTIE`).

**Désignation au lancement et réaffectation** : sont admises, en plus des règles du §5.1 (lanceur lui-même, membre de
l'unité, personne de la portée `TACHE_CONFIER`), toutes les personnes actives qui portent le profil requis, même hors
de l'unité : profil de remplacement pour une désignation au lancement ; pour une réaffectation, profil de la tâche à
prendre, à défaut celui de l'intervenant `PROFIL`, à défaut son profil de remplacement. Personne inactive, ou sans le
profil et hors de ces règles : refus inchangé (422 `PIL_PROCESSUS_DESIGNATION_INVALIDE`,
`PIL_PROCESSUS_PERSONNE_INVALIDE`). Le **lanceur** peut réaffecter une étape de son instance tant qu'elle est à prendre
ou qu'elle lui est revenue faute de porteur (`moi.reaffecter`).

**Routes des candidats** (passerelle `/bff/des/pilotage/processus/**`, `PIL_ACTIVITY_OWN`, OPA :
`GET …/processus/candidats` et `GET …/processus/instances/{id}/etapes/{code}/candidats`) :

| Méthode | Chemin | Objet |
|---|---|---|
| GET | `/processus/candidats?unite=&profil=` | candidats d'un intervenant (modélisateur, lancement) ; unité obligatoire (422 `PIL_PROCESSUS_UNITE_INCONNUE`), profil facultatif (400 s'il est mal formé ; sans profil : membres actifs de l'unité) |
| GET | `/processus/instances/{id}/etapes/{code}/candidats` | candidats à l'exécution d'une étape en cours ; réservé à qui peut la réaffecter (403 `PIL_PROCESSUS_INTERDIT`) |

```jsonc
{ "unite": "POLE_RESEAU", "profil": "ROLE_…" | null,
  "dansUnite": [ { "id", "nomAffiche", "fonction", "unite" } ],   // triés par nom
  "ailleurs":  [ { "id", "nomAffiche", "fonction", "unite" } ] }  // porteurs hors de l'unité, avec leur unité
```

Instance : `aPrendre.unite` est nul pour une étape offerte hors de l'unité. Stockage : contrainte `ck_tache_responsable`
élargie (tâche à prendre sans unité mais avec `profil_requis`, changelog `20261008110000_pil_etapes_hors_unite.xml`).

**Interface** : le sélecteur de personne (personne nommée dans le modélisateur, désignation au lancement, réaffectation)
propose les candidats groupés « Dans l'unité » puis « Ailleurs dans le GIMAC » (avec leur unité), puis les autres
personnes de l'annuaire ; il se remplit d'office quand il n'y a qu'un candidat et l'annonce (« Seul porteur du
profil … : choisi automatiquement ») ; sans porteur, il le dit (« le chef de l'unité désignera »). Pour un intervenant
`PROFIL`, le modélisateur annonce la couverture (porteurs dans l'unité, ailleurs avec leurs unités, ou personne).
L'anomalie `COUVERTURE_HORS_UNITE` (« Profil porté hors de l'unité ») s'affiche comme information, en clair et en
sombre. Le service simulé suit la même règle (démonstration : le pôle réseau n'ayant qu'un porteur de
`ROLE_POLE_RESEAU`, l'étape d'analyse lui est confiée d'office).

Tests : `ResolutionIntervenantsTest` (hors de l'unité, seul porteur), `ProcessusDesignationIT` (publication,
seul porteur ailleurs, plusieurs porteurs ailleurs avec prise et choix du chef ou du lanceur, aucun porteur,
candidats, revue d'organisation), tests du service simulé et des écrans, E2E `processus-unite` et
`departement-passerelle-processus`.

## 7. Délai cible et délai maximal (D-106, 13/10/2026)

- **Processus** : `delaiCibleJours` (inchangé) et nouveau `delaiMaxJours` (`processus.delai_max_jours`, 0..365, au moins
  le délai cible, `NULL` = égal au délai cible ; 400 `PIL_REQUETE_INVALIDE`, `details.champ = delaiMaxJours`).
- **Étape** (JSONB `etapes`, sans migration) : `delaiJours` devient le délai cible ; nouveau `delaiMaxJours` facultatif
  (400, `details.champ = etapes[i].delaiMaxJours`). Une étape antérieure se lit avec un maximum égal à sa cible.
- **Échéances** : à l'activation, la tâche et l'étape portent l'échéance maximale (jour + délai maximal) ; l'étape garde
  aussi `echeance_cible` (jour + délai cible, colonne `processus_etape_instance.echeance_cible`, reprise : l'échéance).
  Vue de l'étape (instance et « Mes demandes ») : `enRetard` (échéance maximale dépassée), `aLApproche` (échéance cible
  atteinte, maximum non dépassé), `echeanceCible`, `delaiMaxJours`. Vue de l'instance : `delais` `{ cibleJours, maxJours,
  echeanceCible, echeanceMax, enRetard, aLApproche }` comptés depuis le lancement, null sans délai.
- **Interface** : modale d'étape (Chronologie : « Délai cible de l'étape », « Délai maximal de l'étape »), propriétés du
  processus (« Délai cible », « Délai maximal »), cartes et liste « 3 à 5 j », pastilles « en retard » et « échéance
  proche » dans l'instance et « Mes demandes », délais de la demande dans son en-tête, « Lancer une demande » (délai
  maximal). Changelog `20261013090000_pil_delai_maximal.xml`.
- **Tests** : `DelaisProcessusTest`, `EtapeInstanceTest.delaiCibleEtMaximal`, `ProcessusDelaisIT`, interface
  `Processus.test.tsx`, `services/simule/delais-d104.test.ts`.

## 8. Demande de changement et export PDF (28/10/2026)

Le modèle `DEMANDE_CHANGEMENT` est provisionné publié par `DemandeChangementConfiguration`, sauf si un processus de
même code existe déjà (`gimac.processus.demande-changement=false` désactive ce provisionnement). Son unité est `SSI`
et son public `TOUT_LE_GIMAC`. Il ne s'agit pas d'un circuit de certification.

Le formulaire de lancement contient douze champs obligatoires : unité du demandeur, élément de configuration,
identifiant EC, priorité, déclencheur, contact du demandeur, raison détaillée, EC impactés, description de l'impact,
contact du responsable de mise en œuvre, plan de test et plan de retour. Les étapes sont chaînées ainsi :

| Étape | Intervenant | Sortie exigée |
|---|---|---|
| `IDENTIFICATION` | lanceur | vérification de l'identification |
| `RAISON` | lanceur | vérification de la raison |
| `IMPACT` | lanceur | vérification de l'impact |
| `IMPLEMENTATION` | responsable désigné au lancement | livrable obligatoire `DOSSIER_CHANGEMENT`, PDF seulement |
| `SSI` | profil `ROLE_AGENT_SSI` | dates de déploiement et de revue, évaluation, décision et description ; signature `DECISION_SSI` portant sur `DOSSIER_CHANGEMENT` |

Chaque processus peut définir `referenceModele`. Pour ce modèle, `CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}` est résolu au
lancement avec la date locale et une séquence PostgreSQL ; la référence est persistée sur l'instance
et exposée dans ses résumés et sa vue détaillée. Les instances sans modèle conservent leur comportement antérieur.

`GET /api/v1/processus/instances/{id}/pdf` (passerelle
`/bff/des/pilotage/processus/instances/{id}/pdf`) rend un PDF A4 nommé `<reference>.pdf`, en `application/pdf` avec
`Cache-Control: no-store`. La lecture suit exactement les droits de l'instance. Le document reprend les données figées
du formulaire, les champs des étapes et les signatures (signataire, date, empreinte), jamais l'assertion FIDO2.
MS-PILOTAGE échappe les valeurs dans le HTML transmis à `POST /api/v1/interne/rendus/pdf` de MS-DOCUMENT ; le client
`pilotage-service` utilise `INTERNAL_DOCUMENT_RENDER`. `gimac.adaptateurs.rendu-pdf` vaut `real` en production et
`stub` sinon ; une réponse non PDF ou indisponible produit `PIL_PDF_INDISPONIBLE`.

Validations locales : tests unitaires de la référence, du modèle et du rendu, scénario `ProcessusChangementIT`, seuils
JaCoCo à 98 % sur le nouveau code ; tests Vitest ciblés et seuils de 98 % limités aux nouveaux fichiers frontend ;
scénario Playwright complet en clair et sombre avec huit captures. Le rendu réel de MS-DOCUMENT, une assertion FIDO2
réelle et le Compose complet n'ont pas été exercés : ces preuves ne constituent ni une recette de production ni un
raccordement à des serveurs GIMAC.
