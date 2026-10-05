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
{ "id": "…", "type": "MESSAGE" | "TACHE" | "RAPPORT",
  "titre": "Carine Ebongue Ndjock",              // expéditeur, ou titre de la tâche, ou « Rapport de … »
  "sousTitre": "Groupe Cadrage L-12" | null,     // conversation de groupe ; pour une tâche : échéance
  "lien": "/des/messagerie?conversation=c-1",    // chemin relatif dans la couche
  "reference": { "service": "collaboration", "type": "CONVERSATION", "id": "c-1" },
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
| GET | `/preferences` | `{ "systeme": true, "son": false, "types": { "MESSAGE": true, "TACHE": true, "RAPPORT": true } }` |
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

## 5. Permissions et clients

`NOT_INBOX_READ` : tous les profils internes. Client de service `notification-service` :
`INTERNAL_IAM_READ`, `INTERNAL_COLLABORATION_READ` (nouvelle permission de service, route interne de
MS-COLLABORATION ci-dessus).

## 6. Mise en œuvre serveur (précisions, sans changement du contrat)

- **Événements consommés** (groupe `notification-inapp`, table `processed_event`) : `collaboration.message-sent.v1`
  (sujet `collaboration.message`) ; `pilotage.task-assigned.v1` (sujet `pilotage.task`) ; `pilotage.report-submitted.v1`
  (sujet `pilotage.report`). Les autres types de ces sujets sont ignorés. Un événement de groupe de la messagerie
  (champ `evenement` non nul, messagerie.md §4) ne notifie pas.
- **Destinataires** : message, `destinataires` sauf `auteurId` ; tâche, `responsableId` sauf s'il vaut `acteurId` ;
  rapport, `destinataireId` sauf s'il vaut l'auteur ou l'acteur.
- **Titres** : message, nom de l'expéditeur (du dernier, si regroupé) ; tâche, `titre` de la charge s'il y figure,
  sinon « Tâche confiée par {acteur} », sous-titre « Échéance : {echeance} » si la charge la porte (la charge v1 de
  MS-PILOTAGE ne porte ni titre ni échéance) ; rapport, « Rapport de {auteur} », sous-titre « Rapport quotidien
  AAAA-MM-JJ » ou « Rapport hebdomadaire AAAA-Www ». Annuaire ou messagerie injoignables : la notification est créée
  avec un titre neutre (« Nouveau message ») ou sans sous-titre, plutôt que perdue.
- **Liens** : `/des/messagerie?conversation={id}`, `/des/taches?tache={id}`, `/des/rapports?vue=recus&rapport={id}`
  (la console remplace le préfixe `/des` par le sien).
- **Liste** : `etat` absent vaut `toutes` ; `page` à partir de 0 ; `taille` 20 par défaut, de 1 à 100 ; tri par
  `misAJourLe` décroissant. Erreurs en `application/problem+json` avec `code` : `NOT_REQUETE_INVALIDE` (400),
  `NOT_NOTIFICATION_INTROUVABLE` (404, notification inconnue ou d'un autre compte).
- **`POST /notifications/lecture`** : corps facultatif ; `reference` exige `service`, `type` et `id`.
- **`PUT /preferences`** : un champ absent garde sa valeur ; type inconnu dans `types` : 400.
- **Événement publié** `notification.inapp-created.v1` (sujet `notification.inapp`, clé et `subject` = identifiant de la
  notification), à la création **et à chaque regroupement** : `data = { notificationId, type, destinataires: [id],
  referenceService, referenceType, referenceId, acteurId, nombre, le }`, sans titre ni contenu.
- **Purge** : chaque nuit (3 h 17 UTC, `gimac.notification.purge.cron`), notifications dont `misAJourLe` a plus de
  90 jours.
