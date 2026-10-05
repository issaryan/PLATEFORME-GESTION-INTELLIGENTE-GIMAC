# Mon activité : agenda, activités du jour, tâches, rapports, équipe (D-76)

Fonctionnalités communes à **tout membre interne du GIMAC**, quel que soit son profil : tenir son agenda,
ses activités du jour, ses tâches, et rendre compte (rapport quotidien ou hebdomadaire) à son responsable
(n+1). Portées par **MS-PILOTAGE** (CC p.231-232 : organisation du travail ; CP PIL-04, PIL-05, PIL-08),
étendues au GIMAC entier (REC D-73) et à l'agenda et au rapport individuel, absents des cahiers (extension
ratifiée par D-76). Le rattachement organisationnel (département, service, responsable) est porté par
**MS-IAM** (CA p.83 « Affectation » ; CP IAM-08).

Une seule application interne (couche `des`, http://…:4200) : un tableau de bord, une barre latérale dont
les entrées s'affichent selon les permissions du jeton. Le portail des participants (4201) et la console
d'administration de la plateforme (4202) restent à part.

## 1. Organisation (MS-IAM)

### 1.1 Unités organisationnelles

Catalogue `unite_organisationnelle` de MS-IAM (Liquibase), modifiable par l'administration :

| Champ | Type | Règle |
|---|---|---|
| `code` | texte, `^[A-Z][A-Z0-9_]{1,31}$` | identifiant stable, unique |
| `libelle` | texte 1..120 | |
| `type` | `DIRECTION` \| `DEPARTEMENT` \| `SERVICE` \| `POLE` \| `CELLULE` | |
| `parent` | code ou null | sans cycle ; une `DIRECTION` n'a pas de parent |
| `active` | booléen | une unité inactive n'est plus proposée, ses rattachements restent lisibles |

Jeu initial (CR p.13-14, CCT p.19-21 ; complétable sans évolution logicielle) :
`DG` Direction Générale (DIRECTION) ; `DES` Département Exploitation et Supervision (DEPARTEMENT, parent DG) ;
`SNA` Service Normes et Agréments (SERVICE, DES) ; `PROD_MONETIQUE` Service Production monétique (SERVICE, DES) ;
`PROD_INFO` Service Production informatique (SERVICE, DES) ; `POLE_RESEAU` Pôle réseau (POLE, PROD_INFO) ;
`POLE_SYSTEME` Pôle système (POLE, PROD_INFO) ; `DDRM` Département Développement du Réseau et du Marché
(DEPARTEMENT, DG) ; `SSI` Sécurité des systèmes d'information (SERVICE, DG) ; `CONFORMITE` Conformité
(SERVICE, DG).

### 1.2 Rattachement d'un compte

Attributs de profil Keycloak (ADMIN_SEUL, comme `fonction`) : `unite` (code d'unité, la plus fine : pôle,
service ou département) et `responsable_id` (identifiant du compte n+1). Règles :
- le responsable est un compte **interne actif** (sans institution), distinct du titulaire ;
- aucun cycle (le titulaire ne peut être dans la chaîne de ses propres responsables) : refus 409
  `ORG_CYCLE` ; unité inconnue ou inactive : 422 `ORG_UNITE_INCONNUE` ; responsable invalide : 422
  `ORG_RESPONSABLE_INVALIDE` ;
- un compte d'institution (correspondant) n'a ni unité ni responsable (422 `ORG_COMPTE_EXTERNE`).

### 1.3 Routes MS-IAM

| Méthode et chemin (passerelle) | Service | Permission |
|---|---|---|
| `GET /bff/{des,console}/iam/organisation/unites` | `GET /api/v1/organisation/unites` | `PIL_ACTIVITY_OWN` ou `IAM_ACCOUNT_READ` |
| `POST /bff/{des,console}/iam/organisation/unites` | `POST /api/v1/organisation/unites` | `IAM_ACCOUNT_EDIT` |
| `PUT /bff/{des,console}/iam/organisation/unites/{code}` | `PUT /api/v1/organisation/unites/{code}` | `IAM_ACCOUNT_EDIT` |
| `PUT /bff/{des,console}/iam/comptes/{id}/rattachement` | `PUT /api/v1/comptes/{id}/rattachement` | `IAM_ACCOUNT_EDIT` |
| `GET /bff/des/iam/organisation/moi` | `GET /api/v1/organisation/moi` | `PIL_ACTIVITY_OWN` |
| hors passerelle (jeton de service `INTERNAL_IAM_READ`) | `GET /api/v1/interne/organisation/{id}` | |
| hors passerelle (jeton de service `INTERNAL_IAM_READ`) | `GET /api/v1/interne/organisation/{id}/equipe` | |

Représentations :

```jsonc
// Unite
{ "code": "POLE_RESEAU", "libelle": "Pôle réseau", "type": "POLE", "parent": "PROD_INFO", "active": true }
// PUT …/comptes/{id}/rattachement (corps) ; réponse : la fiche du compte (Compte) complétée
{ "unite": "POLE_RESEAU", "responsableId": "uuid-ou-null" }
// Rattachement (GET organisation/moi et interne/organisation/{id})
{ "id": "…", "nomAffiche": "…", "unite": { …Unite } | null, "chemin": [ …Unite de la plus haute à la plus fine ],
  "responsable": { "id": "…", "nomAffiche": "…", "fonction": "…" } | null,
  "chaine": ["id n+1", "id n+2", …],   // responsables successifs, 10 au plus
  "equipeDirecte": 3 }                  // nombre de comptes dont il est le n+1
// interne/organisation/{id}/equipe : collaborateurs dont {id} est dans la chaîne (récursif, 500 au plus)
[ { "id": "…", "nomAffiche": "…", "fonction": "…", "unite": "POLE_RESEAU", "responsableId": "…", "niveau": 1, "actif": true } ]
```

La fiche `Compte` (administration et « Mon compte ») gagne `unite` (code ou null) et `responsableId`. Le
journal d'administration consigne `COMPTE_RATTACHEMENT_MODIFIE` (avant, après).

## 2. MS-PILOTAGE : routes

Passerelle : `/bff/des/pilotage/**` réécrit en `/api/v1/**` (`OpaAuthorize`, corps ≤ 1 Mo). Toutes les
routes exigent un compte interne (OPA refuse un jeton portant une institution) et `PIL_ACTIVITY_OWN`, sauf
mention. Le sujet du jeton (`sub`) est l'auteur ; aucune donnée d'un autre compte n'est lisible hors des
règles d'équipe (§3). Dates : ISO 8601 ; jours `AAAA-MM-JJ` ; semaines ISO `AAAA-Www` ; fuseau
`Africa/Douala`.

### 2.1 Agenda

| Méthode | Chemin | Objet |
|---|---|---|
| GET | `/agenda?du=&au=[&collaborateur=]` | événements de l'intervalle (62 jours au plus) ; `collaborateur` : un membre de l'équipe (§3) |
| POST | `/agenda` | créer (201) |
| PUT | `/agenda/{id}` | modifier (propriétaire seul) |
| DELETE | `/agenda/{id}` | supprimer (propriétaire seul, 204) |
| POST | `/agenda/{id}/reponse` | `{ "reponse": "ACCEPTE" \| "DECLINE" }` (participant invité) |

```jsonc
{ "id": "…", "titre": "Revue hebdomadaire", "description": "…", "lieu": "Salle 2",
  "debut": "2026-10-06T09:00:00+01:00", "fin": "2026-10-06T10:00:00+01:00", "journeeEntiere": false,
  "categorie": "REUNION" | "ACTIVITE" | "DEPLACEMENT" | "ABSENCE" | "FORMATION" | "AUTRE",
  "proprietaire": { "id": "…", "nomAffiche": "…" },
  "participants": [ { "id": "…", "nomAffiche": "…", "reponse": "EN_ATTENTE" | "ACCEPTE" | "DECLINE" } ],
  "moi": "PROPRIETAIRE" | "PARTICIPANT" | "EQUIPE" }
```
Règles : `fin > debut` (journée entière : `debut` et `fin` à minuit, fin exclusive) ; 50 participants au plus,
comptes internes actifs ; un participant voit l'événement dans son agenda ; `EQUIPE` : lecture seule.

### 2.2 Activités du jour

| Méthode | Chemin | Objet |
|---|---|---|
| GET | `/activites?du=&au=[&collaborateur=]` | activités de l'intervalle (31 jours au plus), triées par jour puis ordre |
| POST | `/activites` | créer (201) |
| PUT | `/activites/{id}` | modifier (auteur seul) |
| POST | `/activites/{id}/statut` | `{ "statut": … }` |
| DELETE | `/activites/{id}` | supprimer (auteur seul, 204) |

```jsonc
{ "id": "…", "jour": "2026-10-06", "titre": "Préparer le dossier de la liaison L-12", "description": "…",
  "statut": "A_FAIRE" | "EN_COURS" | "FAITE" | "REPORTEE" | "ANNULEE",
  "dureeMinutes": 90,            // temps passé, facultatif, 0..1440
  "ordre": 3, "tacheId": "…" | null, "reporteeAu": "2026-10-07" | null,
  "auteur": { "id": "…", "nomAffiche": "…" }, "modifieeLe": "…" }
```
Reporter une activité (`REPORTEE` avec `reporteeAu`) en crée une copie `A_FAIRE` au jour indiqué.

### 2.3 Tâches (CP PIL-04, PIL-08 ; CC TaskState, OriginKind)

| Méthode | Chemin | Objet |
|---|---|---|
| GET | `/taches?vue=moi\|creees\|equipe[&etat=][&page=&taille=]` | `moi` : dont je suis responsable ; `creees` : que j'ai confiées ; `equipe` : de mon équipe (`PIL_TEAM_READ`) |
| GET | `/taches/{id}` | détail et historique |
| POST | `/taches` | créer : pour soi, ou pour un membre de son équipe (`PIL_TASK_ASSIGN`) |
| PUT | `/taches/{id}` | modifier intitulé, description, échéance, priorité (créateur ou responsable) ; réaffecter (créateur, `PIL_TASK_ASSIGN`) |
| POST | `/taches/{id}/transition` | `{ "etat": …, "motif": "…" }` |
| hors passerelle (jeton de service `INTERNAL_PILOTAGE_TASK_CREATE`) | `POST /api/v1/interne/taches` | tâche créée par un autre service (origine OPERATION, REFERENTIEL…), idempotente par `origine.reference` + `responsableId` |

```jsonc
{ "id": "…", "titre": "…", "description": "…", "echeance": "2026-10-10" | null,
  "priorite": "BASSE" | "NORMALE" | "HAUTE" | "URGENTE",
  "etat": "OUVERTE" | "EN_COURS" | "BLOQUEE" | "TERMINEE" | "ANNULEE",
  "responsable": { "id", "nomAffiche" }, "createur": { "id", "nomAffiche" },
  "origine": { "type": "INTERNE" | "OPERATION" | "REFERENTIEL" | "DECISION" | "TICKET" | "FICHE_ACTION" | "RISQUE" | "INDICATEUR",
               "reference": "…" | null, "service": "homologation" | null, "lien": "/des/parcours/…" | null },
  "enRetard": false, "creeeLe": "…", "termineeLe": null,
  "historique": [ { "quand": "…", "acteur": { "id", "nomAffiche" }, "action": "CREEE" | "MODIFIEE" | "REAFFECTEE" | "TRANSITION", "detail": "…" } ] }
```
Transitions (CC p.238, p.245) : OUVERTE→EN_COURS|BLOQUEE|ANNULEE ; EN_COURS→BLOQUEE|TERMINEE|ANNULEE ;
BLOQUEE→EN_COURS|ANNULEE ; TERMINEE→EN_COURS (réouverture, motif obligatoire). `BLOQUEE` et `ANNULEE`
exigent un motif. Le responsable fait les transitions ; le créateur peut annuler. Hors transition permise :
409 `PIL_TRANSITION_INTERDITE`. Une tâche a un **responsable unique** (RG-20) ; il doit être interne et actif.

### 2.4 Rapports quotidiens et hebdomadaires

| Méthode | Chemin | Objet |
|---|---|---|
| GET | `/rapports?vue=moi\|recus[&type=][&etat=][&page=&taille=]` | `recus` : rapports soumis à moi comme destinataire, ou de mon équipe (`PIL_TEAM_READ`) |
| GET | `/rapports/{id}` | détail |
| POST | `/rapports/preremplissage` | `{ "type", "periode" }` → `{ "contenu": "…markdown…" }`, rien n'est enregistré |
| POST | `/rapports` | créer un brouillon (201) ; un seul rapport par (auteur, type, période) : sinon 409 `PIL_RAPPORT_EXISTANT` |
| PUT | `/rapports/{id}` | modifier le brouillon |
| POST | `/rapports/{id}/soumission` | soumettre au destinataire (n+1 au moment de la soumission) |
| POST | `/rapports/{id}/visa` | `{ "commentaire": "…" }` (destinataire seul, ou chaîne hiérarchique avec `PIL_TEAM_READ`) |
| GET | `/rapports/{id}/document` | flux du document joint (auteur, destinataire, chaîne) |

```jsonc
{ "id": "…", "type": "QUOTIDIEN" | "HEBDOMADAIRE", "periode": "2026-10-06" | "2026-W41",
  "mode": "PREREMPLI" | "REDIGE" | "DOCUMENT",
  "contenu": "…markdown, 20 000 caractères au plus…" | null,
  "document": { "ticket": "…", "nom": "rapport.pdf", "typeMime": "application/pdf", "taille": 123456 } | null,
  "etat": "BROUILLON" | "SOUMIS" | "VISE",
  "auteur": { "id", "nomAffiche" }, "destinataire": { "id", "nomAffiche" } | null,
  "soumisLe": null, "viseLe": null, "visa": { "par": { "id", "nomAffiche" }, "commentaire": "…" } | null,
  "indicateurs": { "activitesFaites": 4, "activitesPrevues": 6, "minutes": 380, "tachesTerminees": 2 } }
```
Modes : **PREREMPLI** (contenu proposé par le service à partir des activités et tâches de la période, puis
librement modifié), **REDIGE** (texte libre), **DOCUMENT** (fichier déposé dans MS-DOCUMENT par la voie des
dépôts fragmentés, `docs/lot1/depots-fragmentes.md`, finalité `RAPPORT_ACTIVITE`, référence = identifiant
du rapport ; le ticket est vérifié auprès de MS-DOCUMENT avec le jeton du client `pilotage-service`). Un mode
DOCUMENT peut porter aussi un texte d'accompagnement. Soumission sans destinataire (aucun n+1) : 422
`PIL_SANS_RESPONSABLE`. Un rapport SOUMIS n'est plus modifiable ; VISE est final. Période future : 422
`PIL_PERIODE_FUTURE`.

Contenu prérempli (quotidien) : titre, « Réalisé » (activités FAITE avec durée), « En cours », « Reporté »,
« Tâches terminées », « Prévu demain » (activités du jour ouvré suivant) ; hebdomadaire : les mêmes rubriques
regroupées par jour, plus le total des heures.

### 2.5 Équipe

| Méthode | Chemin | Permission |
|---|---|---|
| GET | `/equipe?jour=` | `PIL_TEAM_READ` |

```jsonc
[ { "collaborateur": { "id", "nomAffiche", "fonction", "unite", "niveau" },
    "activites": { "prevues": 5, "faites": 3, "enCours": 1 },
    "rapportDuJour": "ABSENT" | "BROUILLON" | "SOUMIS" | "VISE",
    "tachesOuvertes": 4, "tachesEnRetard": 1,
    "absent": false } ]   // « absent » : événement ABSENCE couvrant le jour
```

### 2.6 Synthèse du tableau de bord

`GET /moi/synthese?jour=` (`PIL_ACTIVITY_OWN`) : `{ "activites": {prevues, faites, enCours}, "evenements":
[ …5 prochains événements… ], "taches": { ouvertes, enRetard, echeanceProche }, "rapportDuJour": "ABSENT" |
… , "rapportsARecevoir": 2 }` (rapportsARecevoir : soumis à moi, non visés).

## 3. Règles d'équipe

L'**équipe** d'un compte est l'ensemble des comptes dont il figure dans la chaîne des responsables (MS-IAM,
`interne/organisation/{id}/equipe`, mis en cache 60 s dans MS-PILOTAGE). Avec `PIL_TEAM_READ`, on lit
l'agenda (catégorie, titre, horaires), les activités, les tâches et les rapports soumis de son équipe ; on
vise les rapports reçus. `PIL_TASK_ASSIGN` permet de confier une tâche à un membre de son équipe. Un
accès hors équipe : 403 `PIL_HORS_EQUIPE`.

## 4. Permissions et profils (domaine Keycloak `gimac`)

Nouvelles permissions : `PIL_ACTIVITY_OWN`, `PIL_TEAM_READ`, `PIL_TASK_ASSIGN`,
`INTERNAL_PILOTAGE_TASK_CREATE` (jeton de service). `PIL_ACTIVITY_OWN` est donnée à **tous les profils
internes** (comme la messagerie). `PIL_TEAM_READ` et `PIL_TASK_ASSIGN` : chef du DES, chef DDRM, DG, RSSI,
responsable conformité, responsable de service.

Nouveaux profils :
- `ROLE_COLLABORATEUR` : tout employé du GIMAC sans fonction métier dans la plateforme (messagerie,
  Mon activité, lecture de rien d'autre) ;
- `ROLE_RESPONSABLE_SERVICE` : chef de service, de pôle ou de cellule (Mon activité, équipe, tâches confiées).

Client de service `pilotage-service` : `INTERNAL_IAM_READ`, `INTERNAL_DOCUMENT_READ`.
`homologation-service` reçoit `INTERNAL_PILOTAGE_TASK_CREATE` (D-77).

## 5. Événements (outbox, CloudEvents 1.0, sujets `pilotage.*`)

`pilotage.task-created.v1`, `pilotage.task-assigned.v1`, `pilotage.task-completed.v1` (CC tableau 6.57, CP
p.337) ; `pilotage.report-submitted.v1`, `pilotage.report-visaed.v1` (sujets nouveaux, règle de nommage REC
D-33). Charge utile : identifiants, type, état, acteur ; jamais le contenu d'un rapport.

## 6. Interface (couche `des`)

Barre latérale, groupe « Mon activité » (sous le tableau de bord, `PIL_ACTIVITY_OWN`) : **Ma journée**
(`/des/activite`), **Agenda** (`/des/agenda`), **Tâches** (`/des/taches`), **Rapports** (`/des/rapports`) ;
**Mon équipe** (`/des/equipe`, `PIL_TEAM_READ`). Administration : rattachement (unité, responsable) dans la
fiche d'un compte ; catalogue des unités (`/des/administration/organisation`, `IAM_ACCOUNT_READ`, édition
`IAM_ACCOUNT_EDIT`). Tableau de bord : section « Ma journée » pour tout `PIL_ACTIVITY_OWN` (synthèse §2.6),
avant les sections métier existantes. Toute création et modification se fait dans des fenêtres modales.
