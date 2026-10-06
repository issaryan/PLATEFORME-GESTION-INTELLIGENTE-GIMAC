# Pouvoirs des chefs sur leurs subordonnés : matrice des pouvoirs, demandes à l'administration, tâches confiées (D-81)

Demande de KOAGNE ISSA RYAN : améliorer les tâches (un chef confie une tâche à ses subordonnés, au pôle
réseau, au pôle système, etc.), permettre de créer des rôles et des subordonnés selon une matrice de
pouvoirs ; tous les chefs agissent sur leurs subordonnés selon leurs rôles et leurs droits. Arbitrages du
demandeur : **un chef ne crée pas de compte lui-même**, il adresse une demande à l'administration ; **seule
l'administration crée les rôles** ; une tâche se confie **à une personne, à plusieurs ou à une unité**.
Complète D-62 (administration des comptes), D-76 (organisation, Mon activité) et D-80 (notifications).

## 1. Notions

- **Subordonnés directs** : comptes dont je suis le responsable (n+1). **Équipe** : comptes dont je suis
  dans la chaîne des responsables (D-76 §3). **Unités dirigées** : unités dont je suis le **responsable
  d'unité** (nouveau champ `responsableId` de l'unité, §2), et leurs sous-unités ; **membres d'unité** :
  comptes rattachés à l'une d'elles.
- **Portée** d'un pouvoir : `AUCUNE` < `DIRECTS` < `EQUIPE` < `UNITES` (`UNITES` = équipe ∪ membres des unités
  dirigées). Un compte qui détient plusieurs profils a, pour chaque action, la portée la plus large.

## 2. Responsable d'une unité (MS-IAM)

`Unite` gagne `responsableId` (compte interne actif, ou null) ; modifiable par l'administration
(`PUT /organisation/unites/{code}`, `IAM_ACCOUNT_EDIT`). `Rattachement` (D-76 §1.3) gagne `unitesDirigees`
(codes, sous-unités comprises). Interne : `GET /api/v1/interne/organisation/{id}/perimetre?portee=` →
`{ portee, comptes: [ { id, nomAffiche, fonction, unite, responsableId, niveau, actif } ], unites: [codes] }`
(jeton de service `INTERNAL_IAM_READ`).

## 3. Matrice des pouvoirs (MS-IAM, réglée par l'administration)

Pour chaque **profil** (rôle composite du domaine) et chaque **action**, une portée. Actions :

| Action | Effet | Portée par défaut des responsables* |
|---|---|---|
| `TACHE_CONFIER` | confier une tâche à un subordonné ou à une unité dirigée, la réaffecter | `UNITES` |
| `TACHE_SUIVRE` | voir les tâches des subordonnés et les relancer | `UNITES` |
| `ACTIVITE_LIRE` | lire agenda, activités du jour | `EQUIPE` |
| `RAPPORT_VISER` | lire et viser les rapports | `EQUIPE` |
| `RATTACHEMENT_MODIFIER` | changer l'unité (dans ses unités dirigées) ou le n+1 (dans son équipe) d'un subordonné | `DIRECTS` |
| `COMPTE_DEMANDER` | demander à l'administration la création d'un compte de subordonné | `UNITES` |
| `PROFIL_DEMANDER` | demander l'ajout ou le retrait d'un profil pour un subordonné | `EQUIPE` |
| `STATUT_DEMANDER` | demander la désactivation ou la réactivation d'un subordonné | `EQUIPE` |

\* Responsables : chef du DES, chef de la DDRM, DG, RSSI, conformité, responsable de service. Les autres
profils : `AUCUNE` partout. La matrice porte aussi, par profil, la liste des **profils demandables** (ceux
qu'un chef peut proposer pour un subordonné ; jamais un profil privilégié : super administrateur, RSSI,
administrateur système).

Routes (passerelle `/bff/{des,console}/iam/...` → `/api/v1/...`) :

| Méthode | Chemin | Permission |
|---|---|---|
| GET | `/pouvoirs/matrice` | `IAM_ACCOUNT_READ` |
| PUT | `/pouvoirs/matrice/{profil}` (`{ actions: { ACTION: PORTEE }, profilsDemandables: [..] }`) | `IAM_PROFILE_EDIT` + réauthentification FIDO2 |
| GET | `/pouvoirs/moi` → `{ actions: { ACTION: PORTEE }, profilsDemandables, subordonnes: [..] , unitesDirigees: [..] }` | `PIL_ACTIVITY_OWN` |
| PUT | `/pouvoirs/subordonnes/{id}/rattachement` (`{ unite, responsableId, motif? }`) → fiche du compte, comme `PUT /comptes/{id}/rattachement` | `PIL_ACTIVITY_OWN` (+ matrice `RATTACHEMENT_MODIFIER`, couche `des` seulement) |
| hors passerelle | `GET /api/v1/interne/pouvoirs/{id}` (même forme, jeton `INTERNAL_IAM_READ`) | |

Journal d'administration : `MATRICE_POUVOIRS_MODIFIEE` (avant, après).

## 4. Demandes à l'administration (MS-IAM)

Un chef (selon la matrice) adresse une **demande** ; l'administration (super administrateur, RSSI : permission
d'exécution correspondante) l'**accepte** (l'acte est alors exécuté par MS-IAM avec les règles habituelles :
invitation, journal, profils privilégiés) ou la **refuse** avec un motif.

```jsonc
// Demande
{ "id": "…", "type": "CREATION_COMPTE" | "PROFILS" | "STATUT",
  "demandeur": { "id", "nomAffiche" }, "cible": { "id", "nomAffiche" } | null,   // null pour une création
  "contenu": {
    // CREATION_COMPTE : { identifiant, prenom, nom, email, telephone?, fonction, unite, responsableId, profils: [..] }
    // PROFILS : { ajouter: [..], retirer: [..] }       STATUT : { actif: false|true }
  },
  "motif": "…",                                    // justification du demandeur, 5..500
  "etat": "EN_ATTENTE" | "ACCEPTEE" | "REFUSEE" | "ANNULEE",
  "decision": { "par": { "id", "nomAffiche" }, "motif": "…", "le": "…" } | null,
  "creeeLe": "…" }
```

Règles : la cible (ou, pour une création, l'unité et le n+1 proposés) doit être dans la portée de l'action
du demandeur (`COMPTE_DEMANDER`, `PROFIL_DEMANDER`, `STATUT_DEMANDER`), sinon 403 `ORG_HORS_PERIMETRE` ; les
profils proposés doivent être dans ses `profilsDemandables`, sinon 422 `ORG_PROFIL_NON_DEMANDABLE` ; une seule
demande `EN_ATTENTE` par cible et par type (409 `ORG_DEMANDE_EXISTANTE`) ; le demandeur peut annuler la
sienne tant qu'elle est en attente. Accepter exige la permission de l'acte (`IAM_ACCOUNT_CREATE`,
`IAM_ACCOUNT_ROLES`, `IAM_ACCOUNT_STATUS`) et la réauthentification FIDO2 comme l'acte direct ; un profil
privilégié reste soumis à `IAM_ROLE_PRIVILEGED_GRANT`.

| Méthode | Chemin | Permission |
|---|---|---|
| POST | `/demandes` | `PIL_ACTIVITY_OWN` (+ matrice) |
| GET | `/demandes?vue=miennes\|a-traiter&etat=&page=&taille=` | `PIL_ACTIVITY_OWN` (`a-traiter` : `IAM_ACCOUNT_READ`) |
| GET | `/demandes/{id}` | demandeur ou administration |
| POST | `/demandes/{id}/annulation` | demandeur |
| POST | `/demandes/{id}/acceptation` | permission de l'acte + FIDO2 |
| POST | `/demandes/{id}/refus` (`{ motif }`) | `IAM_ACCOUNT_READ` |

Événements (outbox) : `iam.request-submitted.v1` (destinataires : comptes portant `IAM_ACCOUNT_CREATE` ou
`IAM_ACCOUNT_ROLES`), `iam.request-decided.v1` (destinataire : le demandeur). MS-NOTIFICATION les consomme
(type `DEMANDE`, lien `/des/administration/demandes?demande=<id>` pour l'administration,
`/des/equipe/demandes?demande=<id>` pour le demandeur).

## 5. Rôles (profils) créés par l'administration (MS-IAM)

| Méthode | Chemin | Permission |
|---|---|---|
| POST | `/configuration/profils` (`{ code: "ROLE_…", description, permissions: [..] }`) | `IAM_PROFILE_EDIT` + FIDO2 |
| DELETE | `/configuration/profils/{code}` (profil créé, sans titulaire) | `IAM_PROFILE_EDIT` + FIDO2 |

Code `^ROLE_[A-Z][A-Z0-9_]{2,40}$`, unique ; un profil portant une permission `IAM_*`, `PLATFORM_*` ou
`INTERNAL_*` est **privilégié** (création et attribution réservées à `IAM_ROLE_PRIVILEGED_GRANT`) ;
`INTERNAL_*` interdites à un profil de personne (422). Les profils du domaine livrés ne sont pas supprimables
(409 `IAM_PROFIL_SYSTEME`). Un profil créé est aussitôt attribuable (liste des profils attribuables =
configuration ∪ profils créés), et apparaît dans la matrice des pouvoirs avec la portée `AUCUNE` partout.
Tout profil interne créé reçoit la messagerie, Mon activité et les notifications (comme les profils livrés).

## 6. Tâches confiées (MS-PILOTAGE, extension de D-76 §2.3)

- **À plusieurs personnes** : `POST /taches` avec `responsables: [id, …]` (2 à 50) crée une tâche par personne,
  liées par un `groupeId` ; `GET /taches/groupes/{groupeId}` → `{ groupeId, titre, createur, taches: [ … ],
  avancement: { total, terminees, enRetard } }`.
- **À une unité** : `POST /taches` avec `uniteCible: "POLE_RESEAU"` (et sans responsable) crée une tâche **à
  prendre** (`etat` `A_PRENDRE`, `responsable` null) visible de tous les membres de l'unité et de ses
  sous-unités ; `POST /taches/{id}/prise` : un membre la prend (devient responsable, `OUVERTE`) ;
  `POST /taches/{id}/attribution` (`{ responsableId }`) : le responsable de l'unité ou le créateur la confie
  à un membre. Vue `GET /taches?vue=unite` : tâches à prendre de mes unités.
- **Portée** : confier, réaffecter, attribuer exigent `TACHE_CONFIER` couvrant la personne ou l'unité visée
  (MS-PILOTAGE lit les pouvoirs par `GET /api/v1/interne/pouvoirs/{id}`, cache 60 s) ; sinon 403
  `PIL_HORS_EQUIPE`. Vue `equipe` et relance : `TACHE_SUIVRE`. `POST /taches/{id}/relance` (créateur ou
  `TACHE_SUIVRE`) : notification au responsable, au plus une par 24 h (429 `PIL_RELANCE_RECENTE`).
- Lecture d'agenda, d'activités et visa des rapports : `ACTIVITE_LIRE`, `RAPPORT_VISER` remplacent la règle
  « chaîne + PIL_TEAM_READ » de D-76 §3 (même résultat avec la matrice par défaut).
- Événements : `pilotage.task-assigned.v1` (déjà) ; `pilotage.task-offered.v1` (tâche à prendre, destinataires
  = membres de l'unité) ; `pilotage.task-reminded.v1` (relance). MS-NOTIFICATION les consomme (type `TACHE`).

## 7. Interface (couche `des`)

- **Mon équipe** : arbre de mes subordonnés et unités dirigées ; sur chaque personne, les actions que ma
  matrice ouvre (confier une tâche, voir agenda/activités/rapports, modifier le rattachement, demander un
  profil, une désactivation) ; bouton « Demander un compte » (formulaire en modale) ; onglet « Mes
  demandes » (état, motif de décision, annulation).
- **Tâches** : création en modale avec le choix « une personne / plusieurs personnes / une unité » (listes
  limitées à ma portée), onglet « À prendre » (unité), suivi groupé, relance.
- **Administration** : « Demandes à traiter » (accepter avec la clé de sécurité, refuser avec motif),
  « Matrice des pouvoirs » (tableau profils × actions, portée par cellule, profils demandables), création et
  suppression de profils dans « Profils et permissions », responsable d'une unité dans « Organisation ».

## 8. Mise en œuvre (MS-IAM, MS-NOTIFICATION)

Précisions apportées par la réalisation, sans changer le sens des §2 à §5.

- **Responsable d'une unité** : `PUT /organisation/unites/{code}` : `responsableId` absent du corps, responsable conservé ;
  `null`, retiré ; identifiant, compte interne actif (422 `ORG_RESPONSABLE_INVALIDE`). Aussi accepté par `POST`.
- **Périmètre** (`interne/organisation/{id}/perimetre`, `subordonnes` de `pouvoirs/moi`) : `niveau` 1 pour un direct,
  2 et plus dans la chaîne, **0 pour un membre d'une unité dirigée hors de la chaîne** ; le compte lui-même n'y figure
  jamais ; 500 comptes au plus. `unites` : unités dirigées (sous-unités comprises) pour `UNITES`, vide sinon.
  `subordonnes` = périmètre de la portée la plus large des actions du compte. Un compte inactif ou d'institution n'a
  aucun pouvoir.
- **Matrice** : `GET /pouvoirs/matrice` → `{ actions: [ACTION…], portees: [PORTEE…], profils: [ { profil, privilegie,
  cree, actions: { ACTION: PORTEE }, profilsDemandables: [..] } ] }`, une ligne par profil attribuable. `PUT` :
  action absente = `AUCUNE`, `motif` facultatif ; réponse : la ligne. Profil demandable inconnu : 400
  `IAM_PROFILE_UNKNOWN` ; privilégié : 422 `ORG_PROFIL_NON_DEMANDABLE`. Profils demandables par défaut : profils non
  privilégiés du domaine de chaque responsable (liste dans le README de MS-IAM).
- **Demandes** : corps de `POST /demandes` : `{ type, cibleId (absent pour une création), contenu, motif }`. Contenu ou
  motif invalide, ou demande sans effet sur le compte visé : 400 `ORG_DEMANDE_INVALIDE`. Création : le n+1 proposé est
  le demandeur ou un compte de son périmètre ; l'unité proposée est la sienne, celle d'un compte de son périmètre ou
  une unité dirigée (portée `UNITES`) ; identifiant ou courriel pris : 409 `IAM_ACCOUNT_CONFLICT`. Unicité d'une
  création : par identifiant proposé. Acceptation : corps facultatif `{ motif }` ; permission de l'acte manquante,
  annulation par un autre que le demandeur, vue `a-traiter` sans `IAM_ACCOUNT_READ` : 403 `ORG_DEMANDE_INTERDITE` ;
  décider sa propre demande : 403 `IAM_SELF_ADMINISTRATION` ; demande déjà décidée ou annulée : 409
  `ORG_DEMANDE_TRAITEE` ; demande inconnue ou non visible : 404 `ORG_DEMANDE_INTROUVABLE`. **Une création acceptée
  renseigne `cible` avec le compte créé** (null tant qu'elle est en attente). `decision` reste null pour une demande
  annulée. Liste : `{ contenu, total, page, taille }`, 25 par défaut, 100 au plus.
- **Événements** (sujet `iam.request`, clé = demande) : `iam.request-submitted.v1` `{ demandeId, type, etat,
  demandeurId, cibleId, destinataires, le }` ; `iam.request-decided.v1` `{ demandeId, type, etat, demandeurId,
  cibleId, acteurId, destinataires: [demandeurId], le }`. Aucun événement pour une annulation.
- **Rattachement d'un subordonné** (`PUT /pouvoirs/subordonnes/{id}/rattachement`, action `RATTACHEMENT_MODIFIER`) :
  corps de la route d'administration `PUT /comptes/{id}/rattachement`, `{ unite, responsableId }` (null, absent ou
  vide : la valeur est retirée), plus `motif` facultatif (1000 au plus) ; réponse 200 : la fiche du compte, même
  forme que la route d'administration (`id`, `identifiant`, `nomAffiche`, …, `avatarVersion`, `unite`,
  `responsableId`). Règles : cible dans le périmètre de la portée `RATTACHEMENT_MODIFIER` de l'appelant ; nouvelle
  unité = unité actuelle de la cible ou une unité dirigée par l'appelant (sous-unités comprises) ; nouveau n+1 =
  n+1 actuel de la cible, l'appelant ou un compte de ce même périmètre ; sinon 403 `ORG_HORS_PERIMETRE` (aussi pour
  une portée `AUCUNE`). Soi-même comme cible : 403 `IAM_SELF_ADMINISTRATION`. Ensuite les règles habituelles du
  rattachement (422 `ORG_UNITE_INCONNUE`, `ORG_RESPONSABLE_INVALIDE`, `ORG_COMPTE_EXTERNE`, 409 `ORG_CYCLE`).
  Journal `COMPTE_RATTACHEMENT_MODIFIE` (acteur : le chef, motif, détails `avant`, `après` et
  `pouvoir=RATTACHEMENT_MODIFIER portee=…`) et événement `iam.account-administered.v1`, comme la route
  d'administration. Passerelle : route `iam-pouvoirs-demandes` ; OPA : `PUT ^/bff/des/iam/pouvoirs/subordonnes/[^/]+/rattachement$`,
  `PIL_ACTIVITY_OWN`, comptes internes seulement.
- **Profils créés** : corps `{ code, description, permissions, motif? }` → 201, `{ code, description, permissions,
  privilegie, titulaires, cree: true }` ; `DELETE …?motif=` → 204. Refus : code invalide 400
  `IAM_PROFIL_CODE_INVALIDE`, code pris 409 `IAM_PROFIL_EXISTANT`, `INTERNAL_*` 422 `IAM_PROFIL_PERMISSION_INTERNE`,
  privilégié sans `IAM_ROLE_PRIVILEGED_GRANT` 403 `IAM_PRIVILEGED_PROFILE`, titulaires 409 `IAM_PROFIL_TITULAIRES`.
  La marque « créé » est tenue par MS-IAM (table `profil_cree`), le rôle Keycloak portant en plus l'attribut
  `gimac_profil_cree`. `GET /configuration/profils` ajoute `cree` à chaque profil.
- **MS-NOTIFICATION** : type `DEMANDE` (préférence `types.DEMANDE`, active par défaut) ; `TACHE` pour
  `pilotage.task-offered.v1` (lien `/des/taches?vue=unite`) et `pilotage.task-reminded.v1` (lien
  `/des/taches?tache=<id>`).
- **Passerelle (OPA)** : l'acceptation exige `IAM_ACCOUNT_CREATE` (porté par le super administrateur et le RSSI,
  qui portent aussi `IAM_ACCOUNT_ROLES` et `IAM_ACCOUNT_STATUS`) ; MS-IAM vérifie ensuite la permission propre à
  l'acte. Le refus exige `IAM_ACCOUNT_READ`.
- **Compléments D-86** (docs/lot1/profils-organisation.md §5) : chaque ligne de la matrice porte aussi `libelle` (libellé
  du profil, le code à défaut) et `actions` compte `PROCESSUS_DEFINIR` et `PROCESSUS_LANCER` (défaut `UNITES` pour les
  six responsables) ; `pouvoirs/moi` et `interne/pouvoirs/{id}` portent `libellesProfils` (`{ code: libellé }` des
  profils demandables) ; un profil créé accepte `libelle` (1 à 80) et une description de 500 caractères au plus, et sa
  réponse suit la forme complète d'un profil (`libelle`, `modifieLe`, `modifiePar`).
