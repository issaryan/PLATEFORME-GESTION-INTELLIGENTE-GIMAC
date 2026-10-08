# Lancer un processus, agir à chaque étape, suivre son avancement (D-98)

Demande de KOAGNE ISSA RYAN (08/10/2026) : une fois un processus publié (mis en production), les acteurs qui
y interviennent doivent pouvoir le lancer et le suivre ; on ne voit aujourd'hui nulle part où lancer un
processus d'unité ni le parcours de certification. Chaque notification d'étape doit mener à une action
réellement faisable (interface et serveur), et les autres personnes concernées doivent voir l'état de cette
action. Il existe des processus internes à un service et des processus qu'une personne lance depuis un autre
service vers un service destinataire (demande de congé vers les RH, demande de changement d'adresse IP au pôle
réseau), en fournissant au départ ce que le processus exige.

## 1. Qui peut lancer un processus (MS-PILOTAGE, données de la définition)

Chaque définition publiée porte ses **initiateurs**, en données (aucun nom de profil codé) :

- `MEMBRES_UNITE` (défaut, comportement actuel) : les membres de l'unité propriétaire ;
- `TOUT_LE_GIMAC` : tout compte interne actif (demande de congé, demande d'intervention) ;
- `UNITES` : les membres d'unités désignées ;
- `PROFILS` / `PERMISSIONS` : les porteurs de profils ou de permissions désignés.

Plusieurs règles se cumulent. Le service propriétaire (unité destinataire) reste celui qui exécute les étapes ;
l'initiateur peut appartenir à un autre service.

## 2. Ce que l'initiateur fournit au départ

La définition porte un **formulaire de lancement** : champs typés (texte, nombre, date, période, liste, case,
pièce jointe par la voie des dépôts fragmentés) avec obligation et aide, plus un objet libre. Les valeurs sont
contrôlées au lancement (422 avec `fieldErrors` lus par l'interface) et visibles des intervenants à chaque étape.

## 3. Où lancer, où suivre (application interne, barre latérale pilotée par le catalogue D-94)

- **Lancer une demande** : catalogue des processus que je peux lancer, groupés par service destinataire, avec
  recherche ; ouvre le formulaire de lancement, puis l'instance créée.
- **Mes demandes** : les instances que j'ai lancées, avec leur étape en cours, la personne qui la tient, l'état
  de chaque étape et l'historique.
- **Processus de l'unité** (existant) : les définitions (modélisateur) et les instances de mes unités.
- **Mes tâches** (existant) : chaque étape qui m'est confiée ou que je peux prendre.

## 4. Agir à une étape

La notification d'une étape mène directement à l'écran de l'étape, qui présente seulement les actions
permises à cette personne (calculées par le serveur) : prendre, remplir les champs, déposer les livrables,
signer (FIDO2), valider, renvoyer avec motif, terminer, réaffecter (chef ou lanceur). Chaque action est un appel
réel au serveur ; son résultat met à jour l'état visible par l'initiateur, les intervenants et le chef de
l'unité, en temps réel, et notifie la personne suivante.

## 5. Parcours de certification (MS-PARTICIPANT, MS-HOMOLOGATION, MS-CERTIFICATION)

Le flux principal doit se lancer et se suivre de la même façon, du départ à la fin :

- **Lancer une certification** : point d'entrée unique et visible pour qui en a le droit (candidature et
  adhésion d'une institution, puis ouverture de la liaison et choix du type de projet, conducteur désigné) ;
- **Mes actions de certification** : chaque opération dont je suis responsable, validateur, signataire ou que
  je peux prendre, avec un lien direct vers l'écran de l'opération et ses actions réelles (déposer, valider,
  renvoyer, signer, gestes de certification) ;
- **Suivi** : l'état de chaque opération et jalon, visible par le conducteur, les intervenants, les chefs et,
  pour ce qui le concerne, le participant au portail.

## 6. Preuve

Tests d'intégration serveur (initiateurs, formulaire, lancement inter-services, actions d'étape) et parcours
E2E : un collaborateur d'un service lance une demande vers un autre service, l'intervenant reçoit la
notification, agit, l'initiateur voit l'avancement ; un acteur de la certification lance et fait avancer un
projet jusqu'à la fin.

## 7. Mise en œuvre des processus d'unité (08/10/2026)

Précisions apportées par la réalisation des §1 à §4 et §6 (MS-PILOTAGE, MS-NOTIFICATION, interface), sans changer leur
sens. Le contrat des processus d'unité (docs/lot1/processus-unite.md §5 et §6) reste valable ; seuls les ajouts sont
décrits ici.

### 7.1 Initiateurs en données (MS-PILOTAGE)

- La définition porte `initiateurs` (JSONB `processus.initiateurs`) :

```jsonc
"initiateurs": {
  "regles": ["MEMBRES_UNITE" | "TOUT_LE_GIMAC" | "UNITES" | "PROFILS" | "PERMISSIONS"],   // cumulables, l'une suffit ; défaut [MEMBRES_UNITE]
  "unites": ["SNA", …],             // règle UNITES : membres de ces unités, sous-unités comprises
  "profils": ["ROLE_…", …],         // règle PROFILS : porteurs actifs de l'un des profils
  "permissions": ["CODE", …]        // règle PERMISSIONS : l'une des permissions du jeton de l'appelant
}
```

- Lancer = `REF_METIER_ADMIN` ou `PROCESSUS_LANCER` couvrant l'unité (pouvoirs de pilotage, inchangés), sinon l'une des
  règles : `MEMBRES_UNITE` (membre de l'unité propriétaire ou d'une sous-unité), `TOUT_LE_GIMAC` (compte interne actif),
  `UNITES`, `PROFILS`, `PERMISSIONS`. Aucun nom de profil dans le code : les profils et permissions sont des données de la
  définition. Refus : 403 `PIL_PROCESSUS_NON_INITIATEUR`, message citant les règles, `details` = `{ regles, unites, profils,
  permissions }`.
- Saisie (`POST` et `PUT /processus/{id}`) : `initiateurs` facultatif ; absent d'une création, la règle par défaut ; absent
  d'une modification, celui de la définition est gardé (anciens clients). Formes : 400 `PIL_REQUETE_INVALIDE`
  (`details.champ` : `initiateurs.regles[i]`, `initiateurs.unites[i]`, `initiateurs.profils[i]` au format `ROLE_…`,
  `initiateurs.permissions[i]` au format `^[A-Z][A-Z0-9_]{1,79}$`), 20 éléments au plus par liste. Une nouvelle version
  reprend les initiateurs et le formulaire.
- Contrôle de publication, anomalies bloquantes (`etape` nulle) : `INITIATEURS_INCOMPLETS` (règle `UNITES`, `PROFILS` ou
  `PERMISSIONS` sans paramètre, `element` = `initiateurs.unites|profils|permissions`), `UNITE_INCONNUE` et `PROFIL_INCONNU`
  (`element` = `initiateurs.unites|profils`).
- Une version publiée se lit (`GET /processus/{id}`) de qui peut la lancer, même hors de l'unité (formulaire, désignations
  au lancement) ; `droits.lancer` suit la même règle dans le catalogue et la définition.
- Reprise Liquibase (`20261008130000_pil_lancement_processus.xml`) : colonnes `processus.initiateurs` (défaut et valeur des
  lignes existantes `{"regles":["MEMBRES_UNITE"],…}`), `processus.formulaire` (`{"champs":[]}`) et
  `processus_instance.formulaire` (`{}`) ; les définitions existantes gardent le comportement antérieur.

### 7.2 Formulaire de lancement (MS-PILOTAGE)

```jsonc
"formulaire": { "champs": [ {
  "code": "^[A-Z0-9][A-Z0-9_-]{0,39}$", "libelle": "1..200",
  "type": "TEXTE" | "TEXTE_LONG" | "NOMBRE" | "DATE" | "PERIODE" | "LISTE" | "CASE" | "PIECE_JOINTE",
  "choix": ["…"],                 // LISTE seulement, 50 au plus
  "obligatoire": false, "aide": "0..500",
  "typesPieces": ["application/pdf", "image/*"]   // PIECE_JOINTE seulement ; vide : tout type
} ] }                              // 30 champs au plus
```

- Contrôle de publication : `CHAMP_CODE_DUPLIQUE` et `CHAMP_CHOIX_ABSENT` (`etape` nulle, `element` = `formulaire.CODE`).
- Lancement : `POST /processus/{id}/instances` `{ intitule, champs, designations, formulaire: { CODE: valeur } }`.
  L'intitulé est l'objet de la demande. Valeurs : texte (500 caractères au plus), texte long (4000), nombre (texte ou
  nombre JSON, virgule admise), date `AAAA-MM-JJ`, période `{ debut, fin }` (fin au plus tôt le jour du début), valeur de la
  liste, case `true|false` (obligatoire : cochée), pièce jointe `{ documentId }` ou l'identifiant seul.
- Pièce jointe : dépôt fragmenté de MS-DOCUMENT (D-74), finalité `PROCESSUS_UNITE`, **référence = identifiant de la
  définition lancée**, déposée par l'initiateur ; vérifiée par `GET /api/v1/interne/documents/{id}` (déposant, finalité,
  référence absente ou égale, type admis, empreinte SHA-256) ; l'instance garde `{ documentId, nomFichier, typeMime, taille,
  sha256 }`.
- Refus : 422 `PIL_PROCESSUS_FORMULAIRE_INVALIDE`, toutes les erreurs ensemble, en `details.fieldErrors` et en `fieldErrors`
  du problème (forme de l'ExceptionTranslator, reprise par le client HTTP dans `details.champs`) :
  `[ { objectName: "lancement", field: "formulaire.CODE", message, code } ]`, codes `OBLIGATOIRE`, `INCONNU`, `FORMAT`,
  `TROP_LONG`, `HORS_LISTE`, `PERIODE`, `PIECE_INVALIDE`, `TYPE_PIECE`. Aucune instance n'est créée.
- Instance rendue : `formulaire: [ { code, libelle, type, obligatoire, aide, choix, typesPieces, valeur } ]` (NOMBRE : nombre
  JSON ; PERIODE : objet ; CASE : booléen ; PIECE_JOINTE : objet ; absente : null), lisible de toute personne qui lit
  l'instance (initiateur, intervenants, chef de l'unité).
- Champs des étapes initiales : l'interface ne les présente au lancement que pour une étape initiale exécutée par le
  lanceur (`LANCEUR`) ; ceux d'une étape initiale confiée au service destinataire restent à son exécutant.

### 7.3 Routes « Lancer une demande » et « Mes demandes »

Passerelle `/bff/des/pilotage/processus/**` (inchangée), `PIL_ACTIVITY_OWN` (OPA : la règle `GET` des processus couvre déjà
ces chemins, cas de test ajoutés), `@PreAuthorize(PIL_ACTIVITY_OWN)`.

| Méthode | Chemin | Objet |
|---|---|---|
| GET | `/processus/lancables?q=` | versions publiées que l'appelant peut lancer, groupées par unité propriétaire : `{ groupes: [ { unite, processus: [Résumé] } ] }` ; `q` : code, libellé ou catégorie |
| GET | `/processus/demandes?etat=&q=&page=&taille=` | instances lancées par l'appelant, les plus récentes d'abord (20 par défaut, 100 au plus) ; `q` : objet, libellé ou code du processus |

- Résumé d'une version (catalogue, lançables) : ajout de `initiateurs` et `nombreChampsFormulaire`.
- Ligne de « Mes demandes » :

```jsonc
{ "id", "processus": { "id", "code", "version", "libelle" }, "unite", "intitule", "etat", "lanceeLe", "closeLe", "motif",
  "avancement": { "total", "terminees" },
  "etapes": [ { "code", "libelle", "etat", "executant": { "id", "nomAffiche" } | null,
                "aPrendre": { "unite", "profil" } | null, "activeeLe", "echeance", "termineeLe", "enRetard" } ],
  "derniereEvolution": { "quand", "acteur", "action", "etape", "detail" } | null }
```

- L'initiateur lit son instance (`GET /processus/instances/{id}`) comme avant (participant) ; les actions d'étape restent
  calculées par le serveur (`moi`, `droits`) : l'initiateur n'en a aucune sur les étapes du service destinataire, sauf la
  réaffectation d'une étape à prendre ou qui lui revient faute de porteur (processus-unite.md §6).

### 7.4 Notifications de l'initiateur (MS-NOTIFICATION)

- Constat : seule la clôture (`process-instance-closed`) atteignait l'initiateur ; l'avancement de sa demande ne lui
  parvenait pas.
- MS-PILOTAGE ajoute `lanceurId` à `pilotage.process-step-activated.v1` (ajout compatible).
- MS-NOTIFICATION consomme ce champ : à chaque étape activée (hors renvoi), si l'initiateur n'est pas lui-même destinataire
  (il n'agit pas sur l'étape), une notification `PROCESSUS` lui est créée, nature `DEMANDE_AVANCEE` : titre « Votre demande
  avance : <libellé de l'étape> », sous-titre « <objet>, en attente de prise en charge » (étape à prendre) ou « <objet>,
  étape en cours, échéance <date> », lien de l'événement (instance, étape). Même identifiant d'événement, consommation
  idempotente unique ; un événement antérieur sans `lanceurId` ne notifie que ses destinataires.

### 7.5 Interface (application interne)

- Catalogue des fonctionnalités (D-94, les deux copies, mêmes clés) : `lancerDemande` (« Lancer une demande »,
  `/des/demandes/lancer`, icône `send`, ordre 92) et `mesDemandes` (« Mes demandes », `/des/demandes`, icône `inbox`,
  ordre 94), module `processus`, groupe `monActivite`, `unDe: [PIL_ACTIVITY_OWN]`. Routes : `/des/demandes`,
  `/des/demandes/lancer`, `/des/demandes/:id` (suivi de l'instance, lien profond gardé par `PIL_ACTIVITY_OWN`, §7.8).
- « Lancer une demande » : cartes par service destinataire (mes unités d'abord), recherche ; « Lancer » ouvre la fenêtre
  « Nouvelle demande » : objet, formulaire typé (période en deux dates, liste, case, pièce jointe déposée avant l'envoi et
  non redéposée après un refus), désignations éventuelles ; les erreurs du service s'affichent sous chaque champ.
- « Mes demandes » : par demande, étape en cours et qui la tient (« tenue par … » ou « à prendre : profil (unité) »),
  frise des étapes colorée par état (chaque étape ouvre le suivi sur elle), dernière évolution ; filtre d'état, recherche,
  pages de 10 ; relue toutes les 10 s.
- Suivi d'une instance : bloc « Informations fournies au lancement » ; instance en cours ou suspendue relue toutes les 10 s
  (initiateur, intervenants et chef voient l'état mis à jour) ; l'initiateur revient à « Mes demandes ». La tâche et la
  notification mènent à l'écran de l'étape (`?etape=`), qui présente les seules actions permises par le serveur
  (prendre, remplir, déposer, signer, valider, renvoyer, terminer, réaffecter), chacune étant un appel réel.
- Modélisateur : bouton « Lancement et formulaire » (lecture seule hors brouillon) : règles cumulables (unités et profils
  par cases, permissions par codes), champs du formulaire (libellé, code tiré du libellé, type, obligation, aide, valeurs
  d'une liste, types de pièces, ordre) ; résumé « Qui peut lancer » et nombre de champs dans la fiche.
- Clair et sombre : jetons `--gm-*` seulement (`processus.css`).

### 7.6 Démonstration

- MS-PILOTAGE (profil dev, `gimac.demonstration.processus=true`) : l'organisation simulée gagne le service des ressources
  humaines `RH` (cheffe Solange Ateba Mvondo, gestionnaire Thierry Bekolo Ngoa portant `ROLE_COLLABORATEUR`) ; deux versions
  publiées, initiateurs `TOUT_LE_GIMAC` :
  - `DEMANDE_CONGE` « Demande de congé » (RH) : formulaire période (obligatoire), motif (texte long, obligatoire), pièce
    justificative facultative (PDF ou image) ; étapes ANALYSE « Vérification des droits à congé » (gestionnaire RH, solde de
    jours), VALIDATION « Accord du supérieur hiérarchique » (n+1 du lanceur, décision), EXECUTION « Enregistrement du
    congé » (décision de congé facultative), CLOTURE « Clôture du dossier » ;
  - `CHANGEMENT_ADRESSE_IP` « Demande de changement d'adresse IP » (pôle réseau) : formulaire équipement, adresse actuelle,
    adresse demandée, justification (tous obligatoires) ; étapes ANALYSE « Analyse technique » (profil réseau, adresse
    disponible), VALIDATION « Validation du chef de pôle » (chef d'unité, décision), EXECUTION « Mise en œuvre du
    changement » (adresse attribuée), CLOTURE « Vérification et clôture ».
- Services simulés de l'interface : mêmes définitions et même unité `RH` ; une demande de congé de l'ingénieur
  d'homologation en attente de l'accord de son supérieur. Dans une session de démonstration ouverte, l'état des processus
  est gardé dans l'onglet (`sessionStorage`) : changer de profil de démonstration (rechargement) garde les demandes, et les
  tâches des étapes en cours renaissent dans « Mon activité ».

### 7.7 Tests

- MS-PILOTAGE : `AccesProcessusTest.initiateurs`, `FormulaireLancementTest`, `DemonstrationProcessusTest` ;
  `ProcessusLancementIT` (définition et reprise Liquibase, contrôle, lancement par un membre d'un autre service, refus
  explicite, formulaire et pièce jointe, processus lançables, « Mes demandes » jusqu'à la clôture, `lanceurId` des
  événements, règles cumulées) ; `ProcessusExecutionIT` adapté au refus `PIL_PROCESSUS_NON_INITIATEUR`.
- MS-NOTIFICATION : `EvenementsNotificationsConsumerTest.avancementDemande`, `ReglesNotificationsTest.demandeAvancee`.
- OPA : chemins `lancables` et `demandes` dans `routes_processus`.
- Interface : `services/simule/demandes.test.ts`, `pages/des/processus/Demandes.test.tsx` ; E2E `lancement-processus`
  (clair et sombre, captures `frontend/e2e/captures/d98/`).

### 7.8 Liens profonds et « Mes étapes » (compléments du 08/10/2026)

- **Liens des notifications et des tâches** : MS-PILOTAGE envoie `/des/processus/instances/<id>` (instance) et
  `/des/processus/instances/<id>?etape=<CODE>` (étape) ; MS-NOTIFICATION les rend tels quels (préfixe `/des/processus/`,
  y compris pour `DEMANDE_AVANCEE`) et l'application interne les suit par son routeur. Ces routes, ainsi que
  `/des/demandes/:id`, ne dépendent plus d'une fonctionnalité de la barre latérale : elles sont gardées par la permission
  d'entrée `PIL_ACTIVITY_OWN` (garde `GardePermission`), si bien que l'intervenant, l'initiateur ou le chef ouvre
  l'instance ou l'étape qui le concerne même si « Processus de l'unité » lui est masquée ; MS-PILOTAGE contrôle la
  lecture (unité, intervenant, initiateur). Sur l'écran, le retour « Instances » n'apparaît que si « Processus de
  l'unité » est ouverte, et « Mes demandes » que si cette fonctionnalité l'est.
- **Actions de l'appelant dans les résumés** : chaque étape en cours d'un résumé d'instance
  (`GET /processus/instances?vue=…`) porte `moi` (`{ executer, prendre, signer, valider, reaffecter }`, même calcul que
  l'instance) et `attendMoi` (vrai si l'étape attend la décision de l'appelant : l'exécuter, la prendre, la signer ou la
  valider ; la réaffectation, geste de pilotage, n'en fait pas partie).
- **« Mes étapes »** (Ma journée) ne présente que les étapes dont `attendMoi` est vrai (un service antérieur sans ce champ :
  toutes) : une étape soumise qui attend le validateur ou un signataire quitte la liste de son exécutant et apparaît dans
  celle du validateur ou du signataire.
- Tests : `ProcessusLancementIT` (`attendMoi` et `moi` du chef, de l'ingénieure qui a soumis, d'un porteur qui peut
  prendre), `Demandes.test.tsx` (instance ouverte par lien profond avec « Processus de l'unité » masquée, refus sans
  `PIL_ACTIVITY_OWN`, filtre de « Mes étapes »), E2E `processus-unite` (l'étape soumise quitte « Mes étapes », liste
  chargée attendue).

## 8. Mise en œuvre du parcours de certification (08/10/2026)

### 8.1 Audit du flux, du départ à la mise en production

Parcours suivi acteur par acteur, de la candidature (ou d'une institution adhérente) jusqu'à la liaison `EN_PRODUCTION`
et à l'institution `ACTIF`. Trous constatés et suite donnée :

| Étape (acteur) | Constat | Suite |
|---|---|---|
| Point d'entrée | Aucune entrée de barre latérale ; l'ouverture d'une liaison se trouvait dans trois écrans (fiche de l'institution, liste des liaisons, tableau de bord des chefs). | « Lancer une certification » (§8.2). |
| Ouverture depuis la fiche de l'institution (chef de département) | Menait à l'ancien écran DES-08 (`/des/liaisons/:id?ouverte=1`), pas au dossier de la liaison où se suivent les opérations. | Même ouverture que le parcours (`POST /liaisons`, conducteurs habilités), puis `/des/parcours/:id?nouvelle=1`. |
| Ouverture depuis la liste des liaisons | Seuls les adhérents étaient proposés, alors que MS-HOMOLOGATION admet une institution déjà en homologation (nouvelle liaison). | Adhérents et institutions en homologation proposés. |
| Adhésion prononcée (approbateur, `PAR_TRANSITION_APPROVE`) | `participant.transition-requested.v1` n'est consommé par personne : aucune tâche ni notification ; seul le compteur « propositions en attente » de la DDRM la signale. | Le lancement affiche la proposition en attente et mène l'approbateur à la proposition ; tâche à prendre et notification de l'approbateur (§8.8). |
| Preuves du cadrage (chargé de la DDRM, R des rangs 3 à 5) | Le profil livré porte `HOM_STEP_MANAGE` mais pas `HOM_EVIDENCE_SUBMIT` : la route `/parcours/operations/{id}/preuves` (OPA et `@PreAuthorize`) et le bouton refusaient le responsable désigné. | Preuve ouverte à `HOM_STEP_MANAGE` ou `HOM_EVIDENCE_SUBMIT`, comme le livrable ; le droit de dépôt (RACI, D-93 §2) décide toujours. |
| Fonction à affecter (porteur de la fonction, conducteur) | La tâche « Prendre la fonction … » ou « Affecter … » mène à `?onglet=etapes&fonction=<FONCTION>` ; l'écran ignorait `fonction` et l'affectation se fait dans la gouvernance. | Le dossier ouvre la gouvernance, signale la fonction demandée et propose « Affecter » ; une fonction déjà affectée l'annonce. |
| Mes opérations (tout acteur) | Le tableau d'acteur exige `HOM_DASHBOARD_READ` et une fonction portée (403 `HOM_FUNCTION_NOT_HELD`), ne rend que les tâches et rien de ce que l'on suit ni des jalons. | `GET /parcours/mes-operations` et « Mes actions de certification » (§8.3). |
| Liens de tâche et de notification, application interne | Lien de l'événement et de la tâche = `/des/parcours/<liaison>?onglet=<onglet du geste>&operation=<id>` : l'onglet s'ouvre et l'opération dans son tiroir (onglet Opérations) ; un geste de certification ouvre l'onglet Décision et ses cartes (procès-verbal, approbation). Conforme. | Vérifié (tests unitaires et E2E). |
| Liens de notification du portail (participant) | Seul un geste `DEPOSER_*` menait à son écran ; la signature du procès-verbal, les coordonnées techniques, les attestations et la fiche de suivi menaient à la vue générale de la liaison. | `ReglesHomologation.lienPortailOperation` suit le geste du participant de MS-HOMOLOGATION (§8.4). |
| Démonstration simulée | La liaison n'atteignait jamais `EN_PRODUCTION` (jalons non appliqués), l'institution ne changeait pas de statut, un type créé dans la plateforme n'était pas instancié depuis sa version, les gestes de certification étaient repérés par rang, et le responsable gardait sa tâche pendant la validation. | Simulateur aligné (§8.5). |

### 8.2 « Lancer une certification » (couche `des`)

- Catalogue des fonctionnalités (MS-IAM `config/fonctionnalites-initiales.json` et copie de démonstration du frontend) :
  `lancerCertification`, module et groupe `homologation`, ordre 104, icône `flag`, libellé `nav.lancerCertification`,
  `permissions.unDe` : `PAR_INSTITUTION_CREATE`, `PAR_ADHESION_INSTRUCT`, `HOM_LIAISON_OPEN` ; aucun droit propre (ses
  permissions sont déjà des droits d'autres fonctionnalités, D-90). Un serveur déjà en service l'ajoute au démarrage.
- Écran `/des/certification/lancer` (`LancerCertificationPage`, garde `lancerCertification`), en quatre étapes :
  1. **Institution** : choix au registre (candidats, adhérents, institutions en homologation) ou enregistrement d'une
     nouvelle (formulaire de prise de contact, `PAR_INSTITUTION_CREATE`) ; l'institution est portée par `?institution=`
     et retrouvée dans l'onglet (stockage de session, commodité) au retour de sa fiche ;
  2. **Adhésion** : prérequis servis par MS-PARTICIPANT, proposition en attente, liens vers la fiche pour instruire
     (`?onglet=pieces`), proposer et prononcer (`?onglet=transitions`), selon les permissions ;
  3. **Ouverture de la liaison** (adhérent ou en homologation, `HOM_LIAISON_OPEN`) : type de projet proposable servi par
     MS-HOMOLOGATION (D-91 : actif, référentiel publié), conducteur habilité (`GET /liaisons/conducteurs`), banque
     partenaire facultative ; `POST /liaisons` (refus restitué avec l'identifiant de corrélation) ; liaisons déjà ouvertes
     de l'institution listées ;
  4. **Dossier de la liaison** : `/des/parcours/:id?nouvelle=1`.
  Sans `HOM_LIAISON_OPEN`, l'écran l'explique sans nommer de profil.

### 8.3 « Mes actions de certification » et `GET /parcours/mes-operations`

- MS-HOMOLOGATION, `GET /api/v1/parcours/mes-operations` (passerelle `/bff/des/homologation/parcours/mes-operations`, route
  existante `des-homologation`) : `@PreAuthorize` et règle OPA `mes_operations` exigent l'une de `HOM_LIAISON_READ`,
  `HOM_DASHBOARD_READ`, `HOM_EVIDENCE_SUBMIT`, `HOM_OPERATION_VALIDATE`, `HOM_STEP_MANAGE`, `HOM_OPERATION_ASSIGN`,
  `HOM_PROJECT_CONDUCT`, `HOM_FUNCTION_*` (cinq), `CER_PV_ESTABLISH`, `CER_QUITUS_TECHNIQUE_SIGN`, `CER_DOSSIER_VERIFY`,
  `CER_SECURITY_VERIFY`, `CER_CHANGE_RECORD_LINK`, `CER_QUITUS_CONFORMITE_SIGN`, `CER_QUITUS_DG_SIGN` ; jamais un compte
  d'institution (OPA). Aucune fonction n'est exigée : le service rend ce qui concerne le sujet du jeton.

  ```jsonc
  { "aAgir": [MonOperation], "aSuivre": [MonOperation], "liaisons": [LiaisonSuivie] }
  // MonOperation (champs nuls omis)
  { "operationId", "liaisonId", "liaisonReference", "institutionNom", "statutLiaison", "code", "libelle", "etape", "etapeCode",
    "etat", "qualite": "R|V|C" /* absente : suivie */, "aPrendre": true, "signature": "<code>" /* signature attendue */,
    "geste", "jalon", "declencheur", "vue", "responsable": Personne, "validateur": Personne, "echeance", "sla",
    "lien": "/des/parcours/<liaison>?onglet=<onglet du geste>&operation=<id>", "actionsPermises": ["DEPOSER_PREUVE", "VALIDER", "RENVOYER", "SIGNER", …] }
  // LiaisonSuivie
  { "liaisonId", "reference", "institutionNom", "typeProjet", "statut", "etapeCourante", "conducteur": true,
    "operationsFaites", "operationsTotal", "jalons": [{ "jalon", "atteint", "atteintLe", "operationId" }], "lien": "/des/parcours/<liaison>" }
  ```

  `aAgir` : opérations en cours que la personne doit faire, même règle que « mes tâches » (D-93 §5 :
  `TableauDeBordCalculateur.part`) : R désigné, R à affecter dont elle porte la fonction (`aPrendre`), V désigné en
  validation (V à affecter : `aPrendre`), consulté sollicité, signataire attendu, conducteur pour une fonction externe sans
  institution liée ; `actionsPermises` calculées pour elle (`actionsAvecSignature`). `aSuivre` : autres opérations
  engagées des liaisons qu'elle conduit ou sur lesquelles elle intervient (`Parcours.implique`). `liaisons` : ces liaisons
  (liaisons closes sans suite exclues), statut, avancement et jalons (atteint : l'opération qui le porte est faite).
- Catalogue : `mesActionsCertification`, module et groupe `homologation`, ordre 106, icône `check-check`, libellé
  `nav.mesActionsCertification`, `unDe` = la même liste de permissions ; aucun droit propre.
- Écran `/des/certification/mes-actions` (`MesActionsCertificationPage`) : compteurs (à faire, à prendre, à valider, à
  signer, liaisons suivies), onglets « À faire » (opération et lien direct, liaison, rôle, état, actions possibles et geste
  de certification, échéance et délai), « À suivre » (responsable et validateur, fonction à affecter signalée) et
  « Liaisons suivies » (statut, étape en cours, avancement, jalons atteints ou attendus). Lien « Lancer une certification »
  si le catalogue l'ouvre. Rafraîchi par le canal poussé (sujets `homologation` et `certification` : clé `parcours`) et à
  chaque geste.
- Chaque ligne mène à l'écran de l'opération et à ses actions réelles (appels du port `ServiceParcours` ou
  `ServiceDecision`, passerelle, service) : déposer (preuve ou livrable), valider (clé de sécurité), renvoyer (motif),
  signer, constater la couverture, établir et signer le procès-verbal, constituer le dossier, référencer et relire la
  fiche de changement.

### 8.4 Liens de tâche et de notification

- Application interne : inchangés (`ParcoursProjection.lienOperation`, repris par les tâches de MS-PILOTAGE et le `lien`
  des événements ; MS-NOTIFICATION le garde s'il est sûr). Le dossier ouvre l'onglet donné et le tiroir de `operation` ;
  `?onglet=etapes&fonction=<FONCTION>` ouvre désormais la gouvernance avec la fonction demandée (§8.1).
- Portail (MS-NOTIFICATION, `ReglesHomologation.lienPortailOperation`) : `FaitsOperation` gagne `declencheur` (lu dans
  `homologation.operation-activated.v1` et `operation-validation-requested.v1`). Lien : geste `COORDONNEES_TECHNIQUES` →
  `/portail/liaisons/<liaison>/coordonnees` ; déclencheur `certification.pv-signed.v1` → `…/proces-verbal` ;
  `certification.attestations-archived.v1` → `…/attestations` ; geste `FICHE_SUIVI` → `…/fiche-suivi` ; action attendue
  sur un geste `DEPOSER_*` → `…/livrables/<operation>` (inchangé) ; sinon la liaison. Ce sont les écrans du geste du
  participant servis par MS-HOMOLOGATION (`gesteParticipant`).
- État visible : conducteur, intervenants et chefs (dossier de la liaison, « Mes actions de certification », liste et
  pilotage) ; participant au portail (suivi de son projet, tâches et liens ci-dessus), sans donnée interne (D-97).

### 8.5 Démonstration simulée alignée

- Type créé dans la plateforme : « Agrégateur de paiement » (`AGREGATEUR`, modèle `GENERIQUE`), version 1 publiée
  (`REF-AG-1`), huit opérations qui parcourent tout le flux en données (geste, jalon, déclencheur, vue) : A1 périmètre
  (DDRM, V conducteur), A2 essais (SNA, V conducteur), A3 constat de couverture (`APTE_CERTIFICATION`), A4 procès-verbal
  établi (conducteur, `pv-established`), A5 procès-verbal signé (chef du DES et participant au portail, `pv-signed`,
  `CERTIFIEE`), A6 dossier de changement (production monétique), A7 approbation (production monétique, V sécurité,
  `security-approved`, `APPROUVEE_SECURITE`), A8 bascule (production monétique, V conducteur, `EN_PRODUCTION`).
- Le simulateur du parcours instancie une liaison d'un type créé depuis sa version publiée, repère gestes et
  déclencheurs sur l'opération (plus de rang), applique les jalons (statut de la liaison ; `EN_PRODUCTION` : institution
  `EN_HOMOLOGATION` → `ACTIF`), fait passer l'institution adhérente en homologation à l'ouverture, retire la tâche du
  responsable pendant la validation, et sert `mesOperations`.
- Démonstration continue : avec `gm.demoContinue = 1` (stockage local), l'état du parcours et du registre simulés est
  gardé d'un chargement à l'autre (`gm.demo.parcours`, `gm.demo.participant`) : on change de profil de démonstration
  sans perdre la liaison. Sans ce réglage, chaque chargement repart du jeu initial.

### 8.6 Tests

- MS-HOMOLOGATION : `TableauDeBordCalculateurTest.mesOperations` (à faire, à prendre, à valider, à suivre, jalons,
  personne étrangère, sans identifiant), `TableauxDeBordResourceSecurityTest` (matrice 401, 403, chacune des 19
  permissions, forme), `ParcoursResourceSecurityTest` (preuve : `HOM_STEP_MANAGE` ou `HOM_EVIDENCE_SUBMIT`),
  `TableauxDeBordIT.mesOperations` (PostgreSQL : le chargé de la DDRM livré prend et dépose la preuve, le conducteur la
  reçoit à valider avec `VALIDER` et `RENVOYER`, liaison suivie et jalons, personne étrangère). `./mvnw -o verify` :
  BUILD SUCCESS (386 tests unitaires, 401 d'intégration).
- MS-NOTIFICATION : `ReglesHomologationTest.liensPortailParGeste`. `./mvnw -o verify` : BUILD SUCCESS.
- MS-IAM : fichier livré du catalogue (`FonctionnalitesServiceTest.fichierLivre` : forme, permissions connues du domaine).
- OPA : `test_parcours_preuve_laissee_au_service`, `test_mes_operations_acteurs_du_parcours`,
  `test_mes_operations_refusees` ; copie Kubernetes resynchronisée. Contrat des routes : sans écart bloquant.
- Interface : `pages/des/certification/Certification.test.tsx` (« Mes actions » de l'ingénieur Normes et Agréments et du
  conducteur, lancement par le chef du DES jusqu'au dossier, chargé de la DDRM sur un candidat, démonstration continue de
  bout en bout : chaque acteur trouve son opération et agit jusqu'à `EN_PRODUCTION` et `ACTIF`) ; `cadrage.test.tsx`
  et `hooks/parcours.test.tsx` adaptés (le dossier de la liaison s'ouvre ; l'ouverture relit aussi le registre). Suite
  unitaire de `@gimac/lot1` : 735 tests verts.
- E2E `certification-bout-en-bout` : le chef du DES lance la certification ; le chargé de la DDRM, le conducteur (renvoi
  motivé puis validations), Normes et Agréments, le chef du DES, le participant au portail, la production monétique et la
  sécurité agissent chacun depuis « Mes actions de certification » ; liaison `EN_PRODUCTION`, jalons atteints, institution
  `ACTIF`. Captures clair et sombre dans `frontend/e2e/captures/d98/` (`lancer-certification`, `dossier-ouvert`,
  `mes-actions-conducteur`, `portail-signature-attendue`, `liaisons-suivies-en-production`, `portail-en-production`).
  Specs du parcours rejouées (`parcours-*`, `portail-parcours`, `departement-*`, `configuration-d91-d94`, `accessibilite`) :
  vertes ; `departement-chef-ddrm`, `departement-chef-des` et `departement-passerelle` adaptées à l'ouverture qui mène au
  dossier de la liaison.

### 8.7 Reste à faire

- Adhésion : tâche et notification de l'approbateur pour `participant.transition-requested.v1` : faites (§8.8).
- Onglet Décision : carte du geste visé par `?operation=` mise en évidence : fait (§8.9).
- Annuaire de démonstration : les institutions simulées de MS-PARTICIPANT et de MS-HOMOLOGATION ne portent pas toujours
  les mêmes identifiants (données de démonstration seulement).

### 8.8 Adhésion à prononcer : tâche et notification de l'approbateur (compléments du 08/10/2026)

- **Choix** : un consommateur dans MS-PILOTAGE plutôt qu'un client HTTP dans MS-PARTICIPANT. Les événements de
  transition partent déjà par l'outbox de MS-PARTICIPANT ; MS-PILOTAGE crée la tâche par le même cas d'utilisation que
  sa route interne `POST /api/v1/interne/taches` (`TachesUseCase.creerPourService`, offre par `permissionsRequises`,
  docs/lot1/effet-reel-referentiels.md §9.1), sans jeton de service, sans table de relais ni adaptateur de plus ; Kafka
  rejoue une indisponibilité de MS-IAM (lecture des porteurs de la permission).
- **MS-PARTICIPANT** (sujet `participant.transition`, clé = institution, donc dans l'ordre pour une institution) :
  - `participant.transition-requested.v1` gagne `institutionNom` et `lien` (`/des/institutions/<id>?onglet=transitions`,
    onglet où l'on prononce ou refuse la proposition) ; ajouts compatibles ;
  - nouvel événement `participant.transition-approved.v1`
    `{ institutionId, transitionId, de, vers, approbateurId, approbationsRequises, approbationsRecues, definitive }`,
    publié à chaque approbation : `definitive` vrai quand la transition est prononcée (avant
    `participant.status-changed.v1`), faux après la première de deux approbations (levée de suspension, RG-14) ;
  - `participant.transition-rejected.v1` inchangé.
- **MS-PILOTAGE**, consommateur `TransitionsParticipantConsumer` (groupe `pilotage-transitions-participant`, sujet
  `gimac.consommateurs.transitions.sujet`, défaut `participant.transition`), idempotent par l'identifiant CloudEvents
  (`processed_event`) ; message illisible, d'un autre type ou sans institution ni transition ignoré ; refus définitif
  (4xx) journalisé et marqué traité ; indisponibilité (503) remontée pour être rejouée :
  - proposition : tâche à prendre (`A_PRENDRE`, sans responsable) d'origine `DECISION`, service `participant`, référence
    `<institution>/TRANSITION/<transition>` (une seule tâche par proposition), lien de l'événement (à défaut l'onglet des
    transitions), `permissionsRequises: ["PAR_TRANSITION_APPROVE"]` (porteurs lus dans MS-IAM, aucun profil cité),
    priorité `HAUTE`, créateur = auteur de la proposition ; titre « Prononcer l'adhésion : <institution> » (vers
    `ADHERENT`) ou « Statuer sur la transition <de> vers <vers> : <institution> », description avec le motif ;
  - approbation définitive ou refus : tâche terminée par le service (`/cloture` sur l'offre) ; première de deux
    approbations : tâche remise à prendre (`aPrendre`) si elle avait été prise, pour qu'un second approbateur la voie ;
  - la tâche apparaît dans « Mes tâches » (onglet « À prendre ») des porteurs, avec « Décision à prononcer » ; l'auteur
    de la proposition ne la reçoit pas en notification (créateur exclu) mais, s'il porte la permission, la voit à prendre
    (MS-PARTICIPANT refuse qu'il la prononce : `PAR_SEPARATION_OF_DUTIES`).
- **Notification** : `pilotage.task-offered.v1` gagne `lien` (facultatif), renseigné pour une offre d'origine
  `DECISION` (l'acte se fait sur l'écran d'origine, sans prise préalable) ; MS-NOTIFICATION
  (`ReglesNotifications.lienTacheOfferte`) le rend tel quel s'il est un chemin sûr sous `/des/` (500 caractères au plus,
  sans espace, caractère de contrôle ni « .. »), sinon `/des/taches?vue=unite` comme avant. Les offres de
  MS-HOMOLOGATION (fonction à prendre, origine `OPERATION`) gardent le lien des tâches à prendre.
- **« Mes actions de certification »** : la route `GET /parcours/mes-operations` ne s'y prête pas. Elle est servie par
  MS-HOMOLOGATION et décrit des opérations de liaison (`operationId`, `liaisonId`, RACI, gestes de l'opération) ; la
  proposition d'adhésion est un objet de MS-PARTICIPANT, antérieur à toute liaison, que MS-HOMOLOGATION ne connaît pas
  (il ne reçoit que `participant.status-changed.v1`). L'y faire paraître demanderait une projection des propositions
  dans MS-HOMOLOGATION et un second modèle de ligne. L'approbateur la trouve dans « Mes tâches » et par sa notification ;
  « Lancer une certification » (étape Adhésion, §8.2) montre aussi la proposition en attente et mène à la fiche.
- **Démonstration simulée** : MS-PARTICIPANT simulé publie la proposition, l'approbation et le refus
  (`pont.surTransition`) ; MS-PILOTAGE simulé crée et clôt l'offre (`pont.offrir`, `pont.terminerOrigine`) ; la
  notification de démonstration (`services.decisions`) n'est adressée qu'à un porteur de la permission, autre que
  l'auteur ; en démonstration continue, les propositions en attente gardées sont offertes à chaque chargement.
- **Tests** : MS-PARTICIPANT `InstitutionLifecycleServiceTest` (charge de la proposition, approbation définitive et
  première approbation) et `ParcoursDdrmIT` (outbox et sujet `participant.transition`) ; MS-PILOTAGE
  `TransitionsParticipantConsumerTest`, `ApprobationsTransitionsServiceTest` (titre, référence, lien, permission, clôture,
  remise à prendre, lien de notification) et `ApprobationTransitionKafkaIT` (Kafka réel : tâche à prendre des porteurs,
  « Mes tâches », lien de `task-offered`, rejeu sans doublon, prise puis remise à prendre après la première approbation,
  clôture au prononcé et au refus, message incomplet ignoré) ; MS-NOTIFICATION `ReglesNotificationsTest`,
  `BoiteNotificationsServiceTest.tacheOfferteAvecLien`, `EvenementsNotificationsConsumerTest` et
  `EvenementsNotificationsKafkaIT` (lien profond conservé) ; interface `approbation-transition.test.ts` (simulation, y
  compris démonstration continue), `TachesPage.test.tsx` (« Décision à prononcer », lien d'origine) ; E2E
  `certification-bout-en-bout` (scénario « l'adhésion proposée est offerte aux approbateurs ») : le chargé de la DDRM
  propose, le chef de la DDRM trouve la tâche à prendre, ouvre la notification qui mène à l'onglet des propositions,
  prononce avec sa clé, la tâche disparaît ; captures `adhesion-a-prononcer` (clair et sombre).

### 8.9 Onglet Décision : carte visée par `?operation=` (compléments du 08/10/2026)

- Le lien d'une tâche ou d'une notification vers un geste de certification (`?onglet=decision&operation=<id>`) met
  désormais en évidence la carte où il se fait, comme l'onglet des opérations ouvre l'opération et la gouvernance signale
  la fonction demandée : la carte défile dans la vue (`scrollIntoView`), porte un liseré de focus et un fond passager
  (`.gm-decision-cible`, jetons du thème, clair et sombre, sans animation si l'utilisateur réduit les mouvements),
  `aria-current="step"` et le libellé « Opération visée : <opération> ». Les autres cartes restent affichées, chacune avec
  ses seuls gestes permis.
- Carte lue sur l'opération (`carteDecision`, jamais sur le rang) : procès-verbal pour un déclencheur `*.pv-*`, les jalons
  `APTE_CERTIFICATION` et `CERTIFIEE` ou le geste `CONSTATER_COUVERTURE` ; approbation de la sécurité pour
  `*.change-record-*`, `*.attestations-archived`, `*.security-approved`, le jalon `APPROUVEE_SECURITE` ou la vue
  `ATTESTATIONS` ; autre opération de vue `DECISION` : procès-verbal ; opération inconnue : aucune carte. Les opérations
  ne sont lues que si `operation` est présent.
- Tests : `regles.test.ts` (`carteDecision`), `OngletDecision.test.tsx` (signature du procès-verbal et approbation
  visées, défilement, opération inconnue) ; E2E `certification-bout-en-bout` (le chef du DES arrive sur la carte du
  procès-verbal visée et dans la vue, la production monétique sur celle de l'approbation) ; captures
  `decision-carte-visee` (clair et sombre).


### 8.10 Validation avant livraison (08/10/2026)

- `./mvnw -o verify`, BUILD SUCCESS : MS-PILOTAGE 257 tests unitaires et 796 d'intégration, MS-HOMOLOGATION 386 et 401,
  MS-NOTIFICATION 153 et 355, MS-IAM 326 et 430, MS-PARTICIPANT 289 et 639 (MS-GATEWAY inchangé).
- OPA : 174 tests sur 174 ; contrat des routes : aucun écart bloquant (46 écarts connus) ; realm Keycloak régénéré sans
  changement ; copies Kubernetes conformes ; `valider-infra.sh` : 19 réussis, 4 ignorés faute d'outil local (promtool,
  amtool, hadolint, actionlint), aucun échec.
- Interface : compilation TypeScript de tous les paquets et applications ; tests unitaires : `@gimac/lot1` 740,
  administration 130, compte 44, système de conception 544, socle 170, applications catalogue 210, console 8,
  département 38 (le portail n'a pas de test unitaire).
- E2E Playwright complet (par lots de six fichiers, trois agents) : 299 réussis, 0 échec ; les 8 scénarios du guide
  `guide-referentiels` sont ignorés hors `GIMAC_GUIDE=1` (guide visuel, pas une recette) et passent tous les 8 avec
  `GIMAC_GUIDE=1`. Couverture nyc fusionnée : instructions 92,23 %, branches 80,31 %, fonctions 91,82 %, lignes 95,43 %.
- Scénario console de `accessibilite` (dépassement de 60 s sous charge parallèle) : douze écrans audités par
  rechargement complet, chacun réanalysant la construction instrumentée et réinjectant axe-core (37 s seul, plus de 60 s
  à quatre agents). Les écrans s'enchaînent désormais par la navigation de l'application (`ecran`, historique du
  routeur) et axe-core n'est injecté qu'une fois par document : 22 à 24 s à quatre agents ; même traitement pour le
  scénario du département.
