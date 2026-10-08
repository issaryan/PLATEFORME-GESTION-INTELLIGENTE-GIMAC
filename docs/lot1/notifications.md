# Notifications de la plateforme : cloche, fenêtre surgissante, notification du système (D-80)

Demande de KOAGNE ISSA RYAN : chaque utilisateur voit ses nouveaux messages de la messagerie instantanée
sous forme de fenêtre surgissante et dans une cloche de notifications, **par le service de notifications**
(MS-NOTIFICATION, CC : notifications multicanal, préférences, consolidation ; CP épopée NOT). Arbitrage :
notifications remises tant qu'un onglet de la plateforme est ouvert (pas de Web Push navigateur fermé).

## 1. Chaîne

1. MS-COLLABORATION publie déjà `collaboration.message-sent.v1` (sujet `collaboration.message`, sans contenu,
   avec `destinataires`). MS-PILOTAGE publie `pilotage.task-assigned.v1` et `pilotage.report-submitted.v1`
   (D-76).
2. **MS-NOTIFICATION** consomme ces événements (consommateur idempotent, table des événements traités),
   crée une notification **par destinataire** (jamais pour l'auteur ou l'acteur), selon les préférences du
   destinataire, puis publie `notification.inapp-created.v1` (outbox, sujet `notification.inapp`, CloudEvents
   1.0, `destinataires: [id]`, sans contenu de message).
3. La **passerelle** relaie `notification.inapp` sur le canal temps réel (`/bff/{des,console}/live`, sujet
   `notification`) au seul destinataire, comme les événements de la messagerie.
4. L'interface relit le compteur et la liste, affiche la fenêtre surgissante et, onglet en arrière-plan,
   la notification du système (API Notification du navigateur, contexte sécurisé seulement).

Consolidation (entité `ConsolidationWindow` du JDL) : pour une même conversation, une seule notification non
lue par destinataire ; un nouveau message l'incrémente (`nombre`) et la remonte en tête, sans en créer une
autre. Une notification de message est marquée lue quand le destinataire ouvre la conversation
(`collaboration.conversation-read.v1` si MS-COLLABORATION le publie, sinon par l'interface, §3).

## 2. Notification

```jsonc
{ "id": "…", "type": "MESSAGE" | "TACHE" | "RAPPORT" | "DEMANDE" | "MENTION" | "REPONSE" | "PROCESSUS",
  "titre": "Carine Ebongue Ndjock",              // expéditeur, ou titre de la tâche, ou « Rapport de … »
  "sousTitre": "Groupe Cadrage L-12" | null,     // conversation de groupe ; pour une tâche : échéance
  "lien": "/des/messagerie?conversation=c-1",    // chemin relatif dans la couche
  "reference": { "service": "collaboration", "type": "CONVERSATION", "id": "c-1" },
  "conversationId": "c-1" | null,                // D-82 : conversation (MESSAGE, MENTION, REPONSE), sinon null
  "acteur": { "id": "…", "nomAffiche": "…" } | null,
  "nombre": 3,                                   // messages regroupés
  "creeLe": "…", "misAJourLe": "…", "lueLe": null }
```
Aucun contenu de message n'est stocké ni transmis par MS-NOTIFICATION : l'aperçu affiché dans la fenêtre
surgissante est lu par l'interface auprès de MS-COLLABORATION (fil de la conversation, membres seulement).
Noms : annuaire interne de MS-IAM (jeton du client `notification-service`, `INTERNAL_IAM_READ`), mis en
cache. Titre d'une conversation de groupe : `GET /api/v1/interne/conversations/{id}` de MS-COLLABORATION
(`{ id, type, nom, membres }`, jeton `INTERNAL_COLLABORATION_READ`) ; conversation directe : `sousTitre` null.
Conservation : 90 jours, puis purge.

## 3. Routes (passerelle `/bff/{des,console}/notification/**` → MS-NOTIFICATION `/api/v1/**`)

Compte interne seulement (OPA refuse un jeton portant une institution), permission `NOT_INBOX_READ`.

| Méthode | Chemin | Objet |
|---|---|---|
| GET | `/notifications?etat=non-lues\|toutes&page=&taille=` | les miennes, récentes d'abord, `{contenu,total,page,taille}` |
| GET | `/notifications/compteur` | `{ "nonLues": 4 }` |
| POST | `/notifications/{id}/lecture` | marquer lue (204) |
| POST | `/notifications/lecture` | `{ "type"?, "reference"? }` : toutes, ou celles d'une conversation (204) |
| GET | `/preferences` | `{ "systeme": true, "son": false, "types": { "MESSAGE": true, "TACHE": true, "RAPPORT": true, "DEMANDE": true, "MENTION": true, "REPONSE": true, "PROCESSUS": true } }` |
| PUT | `/preferences` | mêmes champs (200) |

Un type désactivé ne crée plus de notification (la messagerie reste lisible). Accès à la notification d'un
autre : 404.

## 4. Interface (application interne et console)

- **Cloche** de la barre du haut : pastille du nombre de non-lues (99+), panneau des notifications récentes
  (icône du type, titre, sous-titre, aperçu pour un message, depuis, nombre regroupé), clic = ouvrir le lien
  et marquer lue, « Tout marquer comme lu », « Voir tout » (page `/{couche}/notifications`).
- **Fenêtre surgissante** (toast du design system, 6 s, empilées par 3 au plus) à l'arrivée d'une
  notification, sauf pour la conversation actuellement ouverte : titre, aperçu du dernier message, bouton
  « Ouvrir ». Ouvrir une conversation marque ses notifications lues (`POST /notifications/lecture`).
- **Notification du système** : si l'onglet est caché, la préférence `systeme` active et la permission du
  navigateur accordée (demandée par un bouton, jamais à l'ouverture) ; clic = retour à l'onglet et au lien.
  En http par l'adresse IP, l'option est indiquée comme indisponible (HTTPS exigé).
- **Son** discret si `son` est actif.
- **Préférences** dans « Mon compte » (section Notifications).
- **Mentions et réponses (D-82)** : pictogrammes « @ » (`MENTION`) et « répondre » (`REPONSE`) dans la cloche, la
  page et la fenêtre surgissante ; aperçu tiré du message lui-même (dernière page du fil de `conversationId`). Pas
  de fenêtre surgissante pour la conversation ouverte : la comparaison porte sur `conversationId` (à défaut, la
  référence d'une notification de message). Le lien ouvre la conversation sur le message, surligné ; la console
  substitue son préfixe comme pour un message. Interrupteurs « Mentions » et « Réponses » dans Mon compte, actifs
  si le service ne renvoie pas encore ces types.

## 5. Permissions et clients

`NOT_INBOX_READ` : tous les profils internes. Client de service `notification-service` :
`INTERNAL_IAM_READ`, `INTERNAL_COLLABORATION_READ` (nouvelle permission de service, route interne de
MS-COLLABORATION ci-dessus).

## 6. Mise en œuvre serveur (précisions, sans changement du contrat)

- **Événements consommés** (groupe `notification-inapp`, table `processed_event`) : `collaboration.message-sent.v1`
  (sujet `collaboration.message`) ; `pilotage.task-assigned.v1` (sujet `pilotage.task`) ; `pilotage.report-submitted.v1`
  (sujet `pilotage.report`) ; processus d'unité (sujet `pilotage.process`, §8). Les autres types de ces sujets sont ignorés. Un événement de groupe de la messagerie
  (champ `evenement` non nul, messagerie.md §4) ne notifie pas.
- **Destinataires** : message, `destinataires` sauf `auteurId` ; tâche, `responsableId` sauf s'il vaut `acteurId` ;
  rapport, `destinataireId` sauf s'il vaut l'auteur ou l'acteur.
- **Titres** : message, nom de l'expéditeur (du dernier, si regroupé) ; tâche, `titre` de la charge s'il y figure,
  sinon « Tâche confiée par {acteur} », sous-titre « Échéance : {echeance} » si la charge la porte (la charge v1 de
  MS-PILOTAGE ne porte ni titre ni échéance) ; rapport, « Rapport de {auteur} », sous-titre « Rapport quotidien
  AAAA-MM-JJ » ou « Rapport hebdomadaire AAAA-Www ». Annuaire ou messagerie injoignables : la notification est créée
  avec un titre neutre (« Nouveau message ») ou sans sous-titre, plutôt que perdue.
- **Liens** : `/des/messagerie?conversation={id}`, `/des/taches?tache={id}`, `/des/rapports?vue=recus&rapport={id}`,
  mention et réponse `/des/messagerie?conversation={id}&message={messageId}` (la console remplace le préfixe `/des`
  par le sien).
- **Liste** : `etat` absent vaut `toutes` ; `page` à partir de 0 ; `taille` 20 par défaut, de 1 à 100 ; tri par
  `misAJourLe` décroissant. Erreurs en `application/problem+json` avec `code` : `NOT_REQUETE_INVALIDE` (400),
  `NOT_NOTIFICATION_INTROUVABLE` (404, notification inconnue ou d'un autre compte).
- **`POST /notifications/lecture`** : corps facultatif ; `reference` exige `service`, `type` et `id`. Avec la référence
  d'une conversation (`collaboration`, `CONVERSATION`, id), sont marquées lues ses notifications de message **et ses
  mentions et réponses** (D-82, colonne `conversation_id`) ; un `type` fourni filtre encore (l'interface n'en envoie
  pas : ouvrir la conversation lit tout).
- **`PUT /preferences`** : un champ absent garde sa valeur ; type inconnu dans `types` : 400.
- **Événement publié** `notification.inapp-created.v1` (sujet `notification.inapp`, clé et `subject` = identifiant de la
  notification), à la création **et à chaque regroupement** : `data = { notificationId, type, destinataires: [id],
  referenceService, referenceType, referenceId, acteurId, nombre, le }`, sans titre ni contenu.
- **Purge** : chaque nuit (3 h 17 UTC, `gimac.notification.purge.cron`), notifications dont `misAJourLe` a plus de
  90 jours.

## 7. Mentions et réponses (D-82, docs/lot1/messagerie.md §4)

- **Source** : `collaboration.message-sent.v1` porte, facultatifs, `reponseAAuteurId` (auteur du message cité, ou
  null) et `mentionnes` (identifiants, sans l'auteur). Absents (événements antérieurs) : null et liste vide, seul le
  message notifie.
- **Types** `MENTION` (titre « {auteur} vous a mentionné ») et `REPONSE` (titre « {auteur} a répondu à votre
  message ») ; libellés neutres « Vous avez été mentionné » et « Réponse à votre message » si l'annuaire est
  injoignable. Sous-titre : nom du groupe, null pour une conversation directe (les deux types). **Jamais regroupées** :
  une notification par message (`nombre` = 1). Référence `{ "service": "collaboration", "type": "MESSAGE", "id":
  messageId }`, `conversationId` renseigné, lien `/des/messagerie?conversation={id}&message={messageId}`, `acteur` =
  auteur du message.
- **Règles** : seulement si `evenement` est null (vrai message) et si `messageId` est présent ; jamais pour l'auteur ;
  seuls les `destinataires` du message (membres) sont retenus ; un compte à la fois mentionné et auteur cité reçoit
  **une seule** notification, `MENTION` (si `MENTION` est désactivé chez lui, il ne reçoit rien de plus : pas de repli
  sur `REPONSE`). Ces notifications s'ajoutent à la notification `MESSAGE` habituelle (regroupée), soumise à sa propre
  préférence.
- **Préférences** `types.MENTION` et `types.REPONSE`, actives par défaut (colonnes `type_mention`, `type_reponse`) ;
  un type désactivé ne crée rien.
- **Événement publié** `notification.inapp-created.v1` comme les autres types (`type` `MENTION` ou `REPONSE`,
  `referenceType` `MESSAGE`, `referenceId` = messageId, `nombre` 1).
- **Lecture** : ouvrir la conversation (`POST /notifications/lecture` avec sa référence) les marque lues ; un clic sur
  la notification la marque lue (`POST /notifications/{id}/lecture`).

## 8. Processus d'unité (D-87, docs/lot1/processus-unite.md §5.5)

- **Source** : sujet `pilotage.process` (ajouté à `gimac.consommateurs.notification.sujets` et aux sujets surveillés
  pour la file de rebut), six types porteurs de `destinataires` et de `lien` : `pilotage.process-instance-started.v1`,
  `process-step-activated.v1`, `process-step-submitted.v1`, `process-step-signed.v1`, `process-step-reassigned.v1`,
  `process-instance-closed.v1`. `pilotage.process-published.v1` (sans destinataire) est ignoré.
- **Type** `PROCESSUS`, **jamais regroupé** : une notification par événement et par destinataire (`nombre` = 1).
  Référence `{ "service": "pilotage", "type": "PROCESSUS", "id": instanceId }` (`POST /notifications/lecture` avec
  cette référence marque lues celles de l'instance). Consommation idempotente par l'identifiant CloudEvents
  (`processed_event`, groupe `notification-inapp`).
- **Destinataires** : `destinataires` de la charge, calculés par MS-PILOTAGE, sans doublon, **jamais l'acteur** :
  `lanceurId` (instance lancée), `acteurId` (étape soumise, réaffectée, instance close), `signataireId` (étape
  signée) ; une étape activée n'a pas d'acteur. Préférence `types.PROCESSUS`, active par défaut (colonne
  `type_processus`, changelog `20261007090000_not_processus.xml`, contrainte `ck_notification_utilisateur_type`
  étendue).
- **Titres** (`{etape}` : `libelleEtape`, à défaut le code `etape` ; `{instance}` : `intitule`, à défaut `code`) :

| Événement | Titre | Sous-titre |
|---|---|---|
| instance lancée | « Processus lancé : {instance} » | « {code}, lancé par {lanceur} » |
| étape activée (exécutant nommé) | « Étape à réaliser : {etape} » | « {instance}, échéance {echeance} » |
| étape activée (`executantId` nul, à prendre) | « Étape à prendre : {etape} » | idem |
| étape activée par un renvoi (`renvoi`) | « Étape renvoyée : {etape} » | idem |
| étape soumise, `attente` SIGNATURE / VALIDATION | « Signature attendue : {etape} » / « Validation attendue : {etape} » | « {instance}, soumise par {acteur} » |
| étape signée, `etatEtape` A_SIGNER / A_VALIDER / TERMINEE | « Signature attendue : {etape} » / « Validation attendue : {etape} » / « Étape signée : {etape} » | « {instance}, signée par {signataire} » |
| étape réaffectée, `cause` REAFFECTATION | « Étape réaffectée : {etape} » | « {instance}, réaffectée par {acteur} » |
| étape réaffectée, `cause` DEPART | « Étape à reprendre : {etape} » | « {instance}, après le départ de {ancien} » (« après un départ » si le nom est inconnu) |
| instance close, `etat` CLOSE / ANNULEE | « Processus terminé : {instance} » / « Processus annulé : {instance} » | « {code} » / « {code}, annulé par {acteur} » |

  Sans nom disponible (annuaire injoignable), la mention de l'acteur est omise ; sans intitulé, le code du processus
  nomme l'instance et n'est pas répété en sous-titre. Les charges des étapes portent `libelleEtape`
  (le code n'est affiché qu'à défaut). Aucun motif, contenu de document ni valeur de champ n'est repris.
- **Lien** : celui de l'événement, **rendu tel quel** (`/des/processus/instances/{id}`, `?etape={CODE}` pour une étape ;
  la console remplace le préfixe `/des` comme pour les autres types) s'il commence par `/des/processus/`, sans espace,
  caractère de contrôle ni `..`, 500 caractères au plus ; sinon `/des/processus/instances/{instanceId}`.
- **Événement publié** `notification.inapp-created.v1` comme les autres types (`type` `PROCESSUS`, `referenceService`
  `pilotage`, `referenceType` `PROCESSUS`, `referenceId` = instanceId, `nombre` 1).

## 9. Parcours d'homologation (D-93, docs/lot1/effet-reel-referentiels.md §11)

- **Source** : sujets `homologation.operation` et `homologation.liaison` (groupe `notification-homologation`), types
  `homologation.operation-activated.v1`, `homologation.operation-validation-requested.v1`,
  `homologation.liaison-milestone-reached.v1`.
- **Type** `HOMOLOGATION`, jamais regroupé, préférence `types.HOMOLOGATION` (active par défaut, colonne
  `type_homologation`). Référence `{ "service": "homologation", "type": "OPERATION" | "LIAISON", "id": … }`.
- **Destinataires et titres** selon la qualité RACI (action attendue, avis demandé, information, escalade), comptes de
  l'institution sur le portail, fonctions à affecter jamais nommées : voir docs/lot1/effet-reel-referentiels.md §11.1.
- **Liens** : `/des/parcours/<liaison>?onglet=operations&operation=<id>`, `/des/parcours/<liaison>` ; portail
  `/portail/liaisons/<liaison>` et `/portail/liaisons/<liaison>/livrables/<operation>`.
