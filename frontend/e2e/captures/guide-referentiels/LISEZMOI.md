# Guide visuel : mettre à jour les référentiels métier

Captures numérotées, une par étape, chacune en thème clair (`NN-sujet-clair.png`) et en thème sombre
(`NN-sujet-sombre.png`), fenêtre 1440 × 900 (pleine page quand l'écran défile). Le contour orange signale
l'élément à regarder ; il n'existe pas dans l'application.

Produites par `e2e/specs/guide-referentiels.spec.ts` sur les services simulés de l'application du département.
Régénération, depuis `frontend/` (constructions instrumentées à jour, Chromium préinstallé) :

```
npm run e2e:construire
GIMAC_GUIDE=1 npx playwright test --config e2e/playwright.config.ts guide-referentiels
```

Le guide est ignoré sans `GIMAC_GUIDE=1` (donc hors de `npm run e2e` et de `npm run e2e:couverture`).

## Partie A : processus d'une cellule ou d'un service (D-87)

Acteur : Gaston Ndzana Ebode, chef du service Production informatique (profil de démonstration
`ROLE_RESPONSABLE_SERVICE`). Il porte `PROCESSUS_DEFINIR` sur les unités qu'il dirige, dont le pôle réseau et le
pôle système ; le pôle réseau n'a pas de chef propre dans l'organisation de démonstration.

- 01 : l'utilisateur ouvre le menu « Processus de l'unité » et choisit l'unité « Pôle réseau » dans le filtre ; il remarque que le catalogue est groupé par unité, avec l'état (Brouillon, Publiée) et la version de chaque processus.
- 02 : il clique sur « Nouveau processus » et remplit code, unité, libellé, catégorie, délai cible et description ; le code (majuscules) ne pourra plus changer ensuite.
- 03 : après « Créer le brouillon », le modélisateur s'ouvre vide (état Brouillon, v1) ; tout commence par « Ajouter une étape ».
- 04 : dans la modale « Nouvelle étape », onglet Général, il saisit le code, le libellé et l'action à effectuer ; l'action est le texte que l'intervenant lira dans sa tâche.
- 05 : onglet « Intervenant et validateur », désignation « Un porteur du profil » avec le profil « Pôle réseau » ; la tâche sera à prendre par les membres de l'unité qui portent ce profil.
- 06 : même onglet en « Une personne nommée » (Idriss Mahamat Saleh) ; le profil de remplacement devient obligatoire et sert si la personne quitte le GIMAC.
- 07 : il coche « L'étape est validée par un validateur » et choisit « Le chef de l'unité », unité où chercher « Service Production informatique » ; c'est ce chef qui validera l'étape.
- 08 : onglet Livrables, « Ajouter un livrable » : code, libellé, case « obligatoire » cochée, type de pièce PDF.
- 09 : onglet Signatures, « Ajouter une signature » : signataire chef d'unité, et la signature porte sur le livrable coché (« Livrables signés »).
- 10 : onglet Champs, « Ajouter un champ » de type Date, obligatoire ; la mention en bas rappelle qu'à l'étape initiale les champs se remplissent au lancement.
- 11 : onglet Chronologie, il coche l'étape précédente (prédécesseur) et fixe le délai de l'étape ; l'échéance de la tâche part de l'activation de l'étape.
- 12 : après deux étapes de plus, il relie la dernière en tirant à la souris de la sortie (carré à droite) d'une carte vers l'entrée (carré à gauche) de la suivante ; le graphe montre quatre étapes reliées.
- 13 : il clique sur « Contrôler » ; la liste « Contrôle de publication » donne chaque anomalie avec sa gravité (ici deux « Levable ») et les cartes en cause sont encadrées en rouge.
- 14 : un clic sur une anomalie ouvre l'étape au bon onglet ; il corrige (ici l'unité où chercher le validateur) puis enregistre.
- 15 : nouveau « Contrôler » : « Aucune anomalie : le processus peut être publié. » et plus aucune carte en rouge.
- 16 : il clique sur « Publier » ; la confirmation rappelle que la version publiée devient lançable et que la précédente sera retirée.
- 17 : de retour au catalogue, le processus apparaît en v1, état Publiée, avec sa date de publication et le bouton « Lancer ».
- 18 : pour le modifier plus tard, il ouvre la version publiée et clique sur « Nouvelle version » ; un brouillon v2 s'ouvre (sous-titre « v2 », sélecteur « Version »), la v1 reste en service.
- 19 : dans la v2, il ouvre une étape (« Modifier » sur la carte) et lui ajoute une signature, ici sur la configuration appliquée.
- 20 : il publie la v2 ; comme le contrôle relève un avertissement levable (le pôle n'a pas de chef), la modale « Lever les avertissements » exige un motif, consigné dans la chronologie et le registre d'audit.
- 21 : le catalogue montre la v2 Publiée et la v1 Retirée (conservée, consultable, plus lançable).
- 22 : il clique sur « Lancer » : intitulé de l'instance et désignation de la personne pour l'étape « désignée au lancement » (les porteurs du profil de remplacement sont proposés d'abord).
- 23 : le suivi de l'instance s'ouvre : graphe coloré par état (première étape Active), légende des états, panneau de l'étape active et historique.
- 24 : vue d'un autre service, le chef de la DDRM : son catalogue ne contient aucun processus du pôle réseau ; il a son propre bouton « Nouveau processus ».
- 25 : après création, son brouillon apparaît sous sa propre unité, « Département Développement du Réseau et du Marché · mon unité » : chaque cellule ou service a ses propres processus.
- 26 : un membre du pôle système ne voit ni les processus du pôle réseau ni ceux de la DDRM, et n'a pas de bouton « Nouveau processus » (il ne dirige aucune unité).

## Partie B : référentiel d'homologation (D-77, D-89)

Acteur : Joël Ekotto Mballa, ingénieur d'homologation (`ROLE_INGENIEUR_HOMOLOGATION`), sur le type de projet
« Paiement mobile » ; puis Idriss Mahamat Saleh, du pôle réseau, lecteur non éditeur (`ROLE_POLE_RESEAU`).

- 30 : l'utilisateur ouvre le menu « Référentiels d'opérations » ; l'arbre « Versions par type de projet » liste les versions de chaque type avec leur état (brouillon, publiée, retirée).
- 31 : il choisit « Paiement mobile, Version 2 » (publiée) ; la mention « Version figée : lecture seule » s'affiche et la seule façon de la modifier est « Dériver un brouillon ».
- 32 : il clique sur « Dériver un brouillon » ; la confirmation rappelle l'objet (« Paiement mobile, version 2 »).
- 33 : le brouillon s'ouvre dans l'onglet Modélisateur : la barre d'outils gagne « Ajouter une opération », « Disposition automatique », « Publier la version » et le pilote du brouillon.
- 34 : le bouton « Étapes » ouvre la liste ordonnée des étapes (couloirs du canevas) avec couleur, nombre d'opérations et étiquette « Protégée » le cas échéant.
- 35 : « Ajouter une étape » : code, libellé, étape canonique de rattachement (pour la progression des liaisons) et couleur, puis « Enregistrer l'étape ».
- 36 : avec la flèche « Monter », il place « Recette interne » juste avant « Certification » ; l'ordre de la liste est celui des couloirs.
- 37 : une étape protégée (Certification, Approbation de la sécurité, Mise en production) n'a pas de bouton de retrait et son étape canonique est figée (champ grisé, explication sous le champ) ; seule une étape vide et non protégée porte la croix de retrait.
- 38 : « Ajouter une opération », onglet Général : code, libellé, description, étape (la nouvelle « Recette interne » est proposée) et segment ; ici le segment choisi est volontairement erroné.
- 39 : onglet Rôles : responsable (R), validateur (V, distinct du responsable), consultés (C), informés (I) et externes (E).
- 40 : onglet « Livrables et preuves » : « Ajouter un livrable ou une preuve », libellé, nature, déposant, types de pièces, obligatoire.
- 41 : onglet Exécution : geste d'exécution, plateforme, grain de preuve (jalon, particularité et cases spéciales au besoin).
- 42 : onglet Chronologie : prédécesseurs (ici l'opération 16) et délai ; début et fin au plus tôt sont calculés après l'enregistrement.
- 43 : après « Créer l'opération » et « Disposition automatique », il relie R1 à 17 en tirant de la sortie de R1 vers l'entrée de 17 ; l'arc apparaît et les deux cartes signalent déjà une anomalie.
- 44 : le bouton « Liste » donne la même version en tableau accessible au clavier (code, libellé, étape, segment, R, V, prédécesseurs) ; la nouvelle ligne R1 est en bas.
- 45 : « Contrôler » ouvre « Anomalies du graphe » : « Segment incohérent · 17, R1 » ; un clic sur l'anomalie centre l'opération.
- 46 : il corrige le segment de R1 (« Modifier », onglet Général) ; le contrôle affiche « Aucune anomalie : le graphe est publiable. ».
- 47 : « Comparer » ouvre l'onglet « Comparaison avec la version active » (présentation Unifié) : la ligne 17 modifiée (~) et l'opération R1 ajoutée (+).
- 48 : « Publier la version » : la confirmation indique l'impact (liaisons en cours qui restent sur la version active jusqu'à une migration décidée).
- 49 : après publication, l'arbre montre « Version 3 » publiée et « Version 2 » retirée ; la version est désormais en lecture seule (le titre garde le libellé saisi à la dérivation).
- 50 : « Exporter (CSV) » télécharge le fichier des opérations de la version ; le navigateur enregistre le fichier, l'application n'affiche pas de message de confirmation.
- 51 : vue d'un non-éditeur (pôle réseau) sur la même famille de versions : bandeau « Vous voyez les opérations qui vous concernent », aucun bouton d'édition, 10 opérations au lieu de 49.
- 52 : en zoomant sur le canevas, les voisines hors périmètre apparaissent en fantômes grisés, en pointillés, étiquetées « Hors périmètre » : code et libellé seulement, non ouvrables.
- 53 : en vue Liste, ces voisines portent la mention « Hors périmètre » et n'affichent ni rôles ni délais.

## Étapes de la demande sans écran dédié

- Publication d'un processus (A) : la modale de publication n'a pas de motif quand le contrôle est vert ; le motif n'est demandé que pour lever des avertissements (capture 20).
- Suppression d'une étape protégée (B) : l'interface ne propose pas de bouton de retrait pour ces étapes ; il n'y a donc pas de message de refus à montrer (capture 37 à la place).
- Export CSV (B) : aucune confirmation à l'écran en dehors du téléchargement (capture 50).
