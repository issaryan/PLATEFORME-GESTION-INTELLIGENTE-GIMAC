# Messagerie : accusés de lecture (D-88)

Demande de KOAGNE ISSA RYAN : dans le chat, l'utilisateur doit voir si son interlocuteur a lu le message qui
lui a été envoyé. MS-COLLABORATION conserve déjà, par membre, la position de lecture (`POST
/conversations/{id}/lu`, D-64) ; D-88 l'expose et la diffuse.

## 1. Représentation

Chaque message **dont je suis l'auteur** porte un état de lecture (absent pour les messages des autres et les
messages d'événement) :
```jsonc
"lecture": { "etat": "ENVOYE" | "LU_PAR_CERTAINS" | "LU", "lus": 2, "destinataires": 3 }
```
`ENVOYE` : aucun autre membre ne l'a lu ; `LU` : tous les membres présents à l'envoi (hors auteur) l'ont lu ;
`LU_PAR_CERTAINS` : groupe, au moins un mais pas tous. Un membre a lu un message quand sa position de lecture
est postérieure ou égale à l'envoi. Un membre parti du groupe ne compte plus.
`GET /conversations/{id}/messages/{messageId}/lectures` (auteur seulement, sinon 404) →
`[ { membre: { id, nom }, luLe: "ISO-8601" | null } ]` (détail « Lu par » d'un message de groupe).
Conversation (liste) : `dernierMessage.lecture` pour mon dernier message.

## 2. Temps réel

`POST /lu` publie `collaboration.conversation-read.v1` (sujet `collaboration.message`, clé = conversation) :
`{ conversationId, lecteurId, luJusquA: "ISO-8601", destinataires: [membres sauf le lecteur] }`, sans contenu,
seulement si la position avance. La passerelle le relaie sur le canal temps réel (sujet `messagerie`, type
`lecture`) aux destinataires, comme les autres événements de la messagerie ; l'interface met à jour les
coches sans relire tout le fil. MS-NOTIFICATION peut s'en servir pour marquer lues les notifications de la
conversation (D-80 §1).

## 3. Interface

Sous chacun de mes messages : une coche grise (envoyé), deux coches grises (lu par certains, groupe), deux
coches de couleur d'accent (lu par tous), avec texte accessible (« Lu », « Lu par 2 sur 3 »). Survol ou clic
sur un message de groupe : liste « Lu par » avec l'heure. Respect de la vie privée : pas d'option de
désactivation au lot 1 (messagerie professionnelle interne).

## 4. Mise en œuvre

**MS-COLLABORATION.**
- Calcul de l'état (`Lecture.de`, domaine) en mémoire, sans requête par message : les membres de la conversation,
  avec leur position de lecture (`conversation_membre.dernier_lu_le`, posée par `POST /lu` et par l'envoi d'un
  message), sont déjà chargés avec la conversation pour le fil comme pour la liste. Destinataires d'un message :
  membres **actuels**, hors auteur, entrés au plus tard à l'envoi (`ajoute_le <= envoye_le`). L'appartenance n'est
  pas historisée (un départ supprime la ligne du membre) : un membre parti ne compte plus, un membre ajouté (ou
  réintégré) après l'envoi ne compte pas. Plus aucun destinataire (tous partis) : `ENVOYE`, `0` sur `0`.
  `lecture` est absent (champ omis) pour les messages des autres et les événements de groupe ; présent aussi dans
  la réponse `201` de l'envoi d'un message.
- Heure de lecture : chaque avancée de la position est gardée dans `conversation_lecture` (`conversation_id`,
  `membre_id`, `lu_jusqu_a`, `lu_le`, en ajout seulement, changelog `20261006120000_col_accuses_lecture.xml`) ;
  `luLe` d'un message = première avancée qui l'a couvert, lue en une requête groupée par membre. Position atteinte
  avant D-88 : lu, heure inconnue (`luLe: null`).
- `GET /api/v1/messagerie/conversations/{id}/messages/{messageId}/lectures` (`COL_MESSAGE_READ`) →
  `[ { "membre": { "id", "nom" }, "lu": true|false, "luLe": "ISO-8601"|null } ]` : destinataires présents à
  l'envoi et encore membres, lecteurs d'abord (par heure, inconnue en dernier), puis les autres dans l'ordre
  d'entrée. `lu` (ajout au contrat §1) distingue « lu, heure inconnue » de « non lu ». 404
  `COL_MESSAGE_NOT_FOUND` indistinct : non-membre, message inconnu, d'une autre conversation, d'un autre auteur
  ou événement de groupe.
- `POST /lu` : position avancée seulement vers un instant postérieur ; alors ligne `conversation_lecture` et
  outbox `collaboration.conversation-read.v1` (agrégat `conversation`, clé = conversation, sujet Kafka
  `collaboration.message` par exception explicite de `TopicResolver`), donnée
  `{ conversationId, lecteurId, luJusquA, destinataires }` (membres sauf le lecteur). Un second `POST /lu` sans
  nouveau message ne publie rien. L'envoi d'un message avance la position de son auteur (ligne
  `conversation_lecture`) sans accusé : `collaboration.message-sent.v1` fait déjà relire le fil.

**Passerelle.** Le sujet `collaboration.message` est déjà consommé : l'accusé est remis, comme les autres
événements de la messagerie, aux seuls `destinataires` connectés du DES et de la console (jamais le portail),
avec identifiant de reprise. Le « sujet `messagerie`, type `lecture` » du §2 correspond sur le canal à
`topic: "collaboration"`, `type: "collaboration.conversation-read.v1"`. L'enveloppe remise est
`{ id, type, subject: conversationId, topic: "collaboration", acteur: lecteurId, luJusquA }` ; `luJusquA`
n'existe que pour ce type (omis ailleurs), aucune autre donnée de `data` n'est relayée.

**Interface.** Sur `collaboration.conversation-read.v1` reçu pour la conversation ouverte (`subject`) : le
lecteur (`acteur`) a lu tous les messages envoyés jusqu'à `luJusquA` inclus ; mettre à jour les coches de mes
messages concernés sans relire le fil : en conversation directe, `envoyeLe <= luJusquA` passe à `LU` ; en groupe,
l'événement ne dit pas si ce lecteur était déjà compté : relire seulement la page affichée du fil et le détail
« Lu par » s'il est ouvert. Ne compter aucun non-lu pour ce type. Relecture de secours (5 s) inchangée.
