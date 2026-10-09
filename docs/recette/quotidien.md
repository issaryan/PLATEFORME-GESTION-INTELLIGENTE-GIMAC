# Recette « travail quotidien et processus d’unité » (lot QUO)

Application interne de démonstration (services simulés) http://localhost:6100, locale fr-FR, Chromium 1440 × 900 (et 390 × 844 pour le mobile), le 08/10/2026. Scripts Playwright pilotés pas à pas (un contexte par profil, au plus deux navigateurs), chaque geste important suivi d’une capture relue à l’écran. Aucune erreur JavaScript ni réponse réseau en erreur n’a été relevée pendant l’ensemble des parcours (console et réseau surveillés).

## 1. Périmètre

**Profils** : collaboratrice ROLE_COLLABORATEUR (Nadège Mvondo Assiga, pôle réseau, n+1 Gaston Ndzana Ebode) ; responsable de service ROLE_RESPONSABLE_SERVICE (Gaston Ndzana Ebode, chef du service Production informatique) ; chefs d’unité ROLE_CHEF_DES (Paul Mbarga Atangana) et ROLE_CHEF_DDRM (Bertrand Nguema Obiang) ; pôle réseau ROLE_POLE_RESEAU (Idriss Mahamat Saleh) ; collaboratrice d’un autre service ROLE_SNA (Rachel Biyong Mengue) ; sommet de l’organisation ROLE_DG (Jean-Baptiste Ella Mintsa) ; service RH de démonstration (Thierry Bekolo Ngoa, Solange Ateba Mvondo : comptes de l’organisation sans profil de démonstration, voir QUO-01).

**Écrans** : barre latérale et routes interdites ; Ma journée ; Agenda ; Tâches (Mes tâches, À prendre, Confiées, Équipe) ; Rapports (Mes rapports, Reçus) ; Mon équipe (Activité du jour, Organisation, Mes demandes) ; Processus de l’unité (catalogue, modélisateur, Lancement et formulaire, contrôle, publication, versions, retrait, instances) ; Lancer une demande ; Mes demandes ; Messagerie ; cloche, page et préférences des notifications ; recherche globale Ctrl+K ; Mon compte (photo, appareils, historique) ; thèmes clair et sombre ; 390 px ; contrôle en en-US.

**Flux de bout en bout** : journée et rapport du collaborateur (trois modes, « Sera remis à », choix du destinataire, refus) ; visa par le destinataire ; dépôt au sommet (DG) ; tâches confiées à une personne, à plusieurs, à une unité, prises, attribuées, relancées, closes, rouvertes ; modélisation complète d’un processus puis publication, nouvelle version, retrait ; demande de changement d’adresse IP lancée par le SNA et menée jusqu’à la clôture par le pôle réseau et le chef du service ; demande de congé lancée par la collaboratrice et menée jusqu’à la clôture (avec réaffectation des étapes RH par le DG) ; désignation par profil hors de l’unité (un porteur : affectation directe ; plusieurs : à prendre) ; messagerie (réponse, mention, accusés, pièces et aperçus, groupe, élargissement).

Captures : `docs/recette/captures/quotidien/NNN-<profil>-<ecran>-<clair|sombre>.png` (numéros non contigus : les captures de mise au point ont été retirées).

## 2. Tableau des contrôles

| N° | Écran ou flux | Profil | Geste | Résultat attendu | Résultat observé | Capture | Verdict |
|---|---|---|---|---|---|---|---|
| ACC-collab-nav | Barre latérale | ROLE_COLLABORATEUR | Ouvrir le tableau de bord, relever la barre latérale | Entrées du catalogue permises par le profil | Entrées du catalogue exactement présentes (plus « Mon compte » et « Se déconnecter » du groupe Mon espace, communs à tous) | 002 | Conforme |
| ACC-collab-_des_equipe | /des/equipe | ROLE_COLLABORATEUR | Adresse directe /des/equipe | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-collab-_des_institutions | /des/institutions | ROLE_COLLABORATEUR | Adresse directe /des/institutions | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-collab-_des_liaisons | /des/liaisons | ROLE_COLLABORATEUR | Adresse directe /des/liaisons | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-collab-_des_referentiels_operations | /des/referentiels/operations | ROLE_COLLABORATEUR | Adresse directe /des/referentiels/operations | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-collab-_des_certification_lancer | /des/certification/lancer | ROLE_COLLABORATEUR | Adresse directe /des/certification/lancer | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-collab-_des_administration_comptes | /des/administration/comptes | ROLE_COLLABORATEUR | Adresse directe /des/administration/comptes | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | 003 | Conforme |
| ACC-collab-_des_administration_matrice-pouvoirs | /des/administration/matrice-pouvoirs | ROLE_COLLABORATEUR | Adresse directe /des/administration/matrice-pouvoirs | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-collab-_des_administration_demandes | /des/administration/demandes | ROLE_COLLABORATEUR | Adresse directe /des/administration/demandes | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-collab-_des_plateforme_supervision | /des/plateforme/supervision | ROLE_COLLABORATEUR | Adresse directe /des/plateforme/supervision | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-resp-nav | Barre latérale | ROLE_RESPONSABLE_SERVICE | Ouvrir le tableau de bord, relever la barre latérale | Entrées du catalogue permises par le profil | Entrées du catalogue exactement présentes (plus « Mon compte » et « Se déconnecter » du groupe Mon espace, communs à tous) | 005 | Conforme |
| ACC-resp-_des_institutions | /des/institutions | ROLE_RESPONSABLE_SERVICE | Adresse directe /des/institutions | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-resp-_des_liaisons | /des/liaisons | ROLE_RESPONSABLE_SERVICE | Adresse directe /des/liaisons | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-resp-_des_administration_comptes | /des/administration/comptes | ROLE_RESPONSABLE_SERVICE | Adresse directe /des/administration/comptes | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-resp-_des_administration_matrice-pouvoirs | /des/administration/matrice-pouvoirs | ROLE_RESPONSABLE_SERVICE | Adresse directe /des/administration/matrice-pouvoirs | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-resp-_des_plateforme_configuration | /des/plateforme/configuration | ROLE_RESPONSABLE_SERVICE | Adresse directe /des/plateforme/configuration | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-chefdes-nav | Barre latérale | ROLE_CHEF_DES | Ouvrir le tableau de bord, relever la barre latérale | Entrées du catalogue permises par le profil | Entrées du catalogue exactement présentes (plus « Mon compte » et « Se déconnecter » du groupe Mon espace, communs à tous) | 007 | Conforme |
| ACC-chefdes-_des_administration_comptes | /des/administration/comptes | ROLE_CHEF_DES | Adresse directe /des/administration/comptes | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-chefdes-_des_administration_matrice-pouvoirs | /des/administration/matrice-pouvoirs | ROLE_CHEF_DES | Adresse directe /des/administration/matrice-pouvoirs | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-chefdes-_des_administration_demandes | /des/administration/demandes | ROLE_CHEF_DES | Adresse directe /des/administration/demandes | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-chefddrm-nav | Barre latérale | ROLE_CHEF_DDRM | Ouvrir le tableau de bord, relever la barre latérale | Entrées du catalogue permises par le profil | Entrées du catalogue exactement présentes (plus « Mon compte » et « Se déconnecter » du groupe Mon espace, communs à tous) | - | Conforme |
| ACC-chefddrm-_des_administration_matrice-pouvoirs | /des/administration/matrice-pouvoirs | ROLE_CHEF_DDRM | Adresse directe /des/administration/matrice-pouvoirs | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-reseau-nav | Barre latérale | ROLE_POLE_RESEAU | Ouvrir le tableau de bord, relever la barre latérale | Entrées du catalogue permises par le profil | Entrées du catalogue exactement présentes (plus « Mon compte » et « Se déconnecter » du groupe Mon espace, communs à tous) | 010 | Conforme |
| ACC-reseau-_des_equipe | /des/equipe | ROLE_POLE_RESEAU | Adresse directe /des/equipe | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-reseau-_des_equipe_demandes | /des/equipe/demandes | ROLE_POLE_RESEAU | Adresse directe /des/equipe/demandes | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-reseau-_des_administration_comptes | /des/administration/comptes | ROLE_POLE_RESEAU | Adresse directe /des/administration/comptes | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-reseau-_des_certification_lancer | /des/certification/lancer | ROLE_POLE_RESEAU | Adresse directe /des/certification/lancer | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-dg-nav | Barre latérale | ROLE_DG | Ouvrir le tableau de bord, relever la barre latérale | Entrées du catalogue permises par le profil | Entrées du catalogue exactement présentes (plus « Mon compte » et « Se déconnecter » du groupe Mon espace, communs à tous) | 012 | Conforme |
| ACC-dg-_des_administration_comptes | /des/administration/comptes | ROLE_DG | Adresse directe /des/administration/comptes | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-dg-_des_administration_matrice-pouvoirs | /des/administration/matrice-pouvoirs | ROLE_DG | Adresse directe /des/administration/matrice-pouvoirs | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-dg-_des_plateforme_supervision | /des/plateforme/supervision | ROLE_DG | Adresse directe /des/plateforme/supervision | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| ACC-sna-nav | Barre latérale | ROLE_SNA | Ouvrir le tableau de bord, relever la barre latérale | Entrées du catalogue permises par le profil | Entrées du catalogue exactement présentes (plus « Mon compte » et « Se déconnecter » du groupe Mon espace, communs à tous) | - | Conforme |
| ACC-sna-_des_equipe | /des/equipe | ROLE_SNA | Adresse directe /des/equipe | Refus propre ou redirection, sans données | Écran « Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée affichée | - | Conforme |
| QUO-J01 | Ma journée | ROLE_COLLABORATEUR | Ouvrir « Ma journée » depuis la barre latérale | Titre « Ma journée », liste des activités du jour | Conforme à l’attendu | 020 | Conforme |
| QUO-J02 | Ma journée | ROLE_COLLABORATEUR | Ajouter une activité vide puis remplie | Refus « Indiquez l’intitulé », puis toast « Activité ajoutée. » | Conforme à l’attendu | 021 | Conforme |
| QUO-J03 | Ma journée | ROLE_COLLABORATEUR | Changer le statut d’une activité, modifier, reporter, supprimer | Toasts de confirmation successifs | Conforme à l’attendu | 022 | Conforme |
| QUO-A01 | Agenda | ROLE_COLLABORATEUR | Accepter l’invitation en attente | Toast « Invitation acceptée. », fiche « Accepté » | Conforme à l’attendu | 023, 024 | Conforme |
| QUO-A02 | Agenda | ROLE_COLLABORATEUR | Créer un événement (fin avant début refusée), avec un participant | Refus « La fin doit suivre le début. », puis « Événement créé ; les participants sont invités. » | Conforme à l’attendu | 025 | Conforme |
| QUO-A03 | Agenda | ROLE_COLLABORATEUR | Modifier l’événement (journée entière) puis le supprimer | Toasts « Événement modifié. » et « Événement supprimé. » | Conforme à l’attendu | 026 | Conforme |
| QUO-T01 | Tâches | ROLE_COLLABORATEUR | Créer une tâche personnelle | Toast « Tâche créée. » | Tâche créée ; aucun choix pour confier à d’autres (droit réservé aux responsables) | 027, 028 | Conforme |
| QUO-R06 | Rapports | ROLE_COLLABORATEUR | Rouvrir un rapport soumis : plus de modification | Pas de bouton « Modifier le brouillon » ni « Viser » pour l’auteur | Conforme à l’attendu | 035 | Conforme |
| QUO-E00 | Mon équipe | ROLE_COLLABORATEUR | Adresse directe /des/equipe | Accès refusé | Conforme à l’attendu | - | Conforme |
| QUO-T02 | Tâches | ROLE_COLLABORATEUR | Commencer, terminer, rouvrir sans puis avec motif | États « En cours », « Terminée », motif exigé pour rouvrir | Réouverture sans motif bloquée (dialogue reste ouvert), puis acceptée avec motif | 041, 042 | Conforme |
| QUO-R01 | Rapports | ROLE_COLLABORATEUR | Nouveau rapport quotidien en mode Préremplir : « Sera remis à » | Destinataire annoncé (n+1), texte prérempli à partir des activités | Mention : « Le rapport soumis sera remis à Gaston Ndzana Ebode, votre responsable. » | 043, 044 | Conforme |
| QUO-R02 | Rapports | ROLE_COLLABORATEUR | Ouvrir le brouillon : « Sera remis à », choisir un autre destinataire de la chaîne | « Sera remis à : Gaston Ndzana Ebode (votre n+1) », liste « Remettre à » ; choix « Paul Mbarga Atangana (choisi par vous) » | Options « Remettre à » : Gaston Ndzana Ebode · votre n+1 / Paul Mbarga Atangana · chef d’une unité au-dessus de la vôtre / Jean-Baptiste Ella Mintsa · chef d’une unité au-dessus de la vôtre ; remis au chef du DES | 045 | Conforme |
| QUO-R03 | Rapports | ROLE_COLLABORATEUR | Second rapport quotidien pour le même jour | Refus expliqué PIL_RAPPORT_EXISTANT | Message « Vous avez déjà un rapport pour cette période : ouvrez-le depuis la liste. » | 046 | Conforme |
| QUO-R04 | Rapports | ROLE_COLLABORATEUR | Rapport hebdomadaire mode Rédiger vide, puis mode Déposer sans document | Refus « Le rapport est vide… » et « Choisissez le document du rapport. » | Conforme à l’attendu | 047 | Conforme |
| QUO-R05 | Rapports | ROLE_COLLABORATEUR | Rapport hebdomadaire par dépôt d’un PDF, aperçu, soumission | Progression, « Brouillon enregistré. », aperçu, « Rapport remis à Gaston Ndzana Ebode (votre n+1). » | Conforme à l’attendu | 048, 049 | Conforme |
| QUO-S-ma-journee | ma-journee | ROLE_COLLABORATEUR | Ouvrir /des/activite en thème sombre | Thème sombre appliqué, texte lisible | fond rgb(24, 26, 30) | 050 | Conforme |
| QUO-S-agenda | agenda | ROLE_COLLABORATEUR | Ouvrir /des/agenda en thème sombre | Thème sombre appliqué, texte lisible | fond rgb(24, 26, 30) | 051 | Conforme |
| QUO-S-taches | taches | ROLE_COLLABORATEUR | Ouvrir /des/taches en thème sombre | Thème sombre appliqué, texte lisible | fond rgb(24, 26, 30) | 052 | Conforme |
| QUO-S-rapports | rapports | ROLE_COLLABORATEUR | Ouvrir /des/rapports en thème sombre | Thème sombre appliqué, texte lisible | fond rgb(24, 26, 30) | 053 | Conforme |
| QUO-S-rapport | Rapports | ROLE_COLLABORATEUR | Nouveau rapport en sombre | Modale lisible | Conforme à l’attendu | 054 | Conforme |
| QUO-M-ma-journee | ma-journee | ROLE_COLLABORATEUR | Ouvrir /des/activite à 390 px | Pas de défilement horizontal, contenu lisible | largeur 390 px | 055 | Conforme |
| QUO-M-agenda | agenda | ROLE_COLLABORATEUR | Ouvrir /des/agenda à 390 px | Pas de défilement horizontal, contenu lisible | largeur 390 px | 056 | Conforme |
| QUO-M-taches | taches | ROLE_COLLABORATEUR | Ouvrir /des/taches à 390 px | Pas de défilement horizontal, contenu lisible | largeur 390 px | 057 | Conforme |
| QUO-M-lancer-demande | lancer-demande | ROLE_COLLABORATEUR | Ouvrir /des/demandes/lancer à 390 px | Pas de défilement horizontal, contenu lisible | largeur 390 px | 058 | Conforme |
| QUO-E01 | Tableau de bord | ROLE_RESPONSABLE_SERVICE | Lire « Ma journée » du tableau de bord | Compteur « Rapports à viser » | Conforme à l’attendu | 060 | Conforme |
| QUO-E02 | Mon équipe | ROLE_RESPONSABLE_SERVICE | Ouvrir « Mon équipe » | Tableau des membres : activités, rapport, tâches, présence ; Chantal absente, 1 en retard | 3 membres listés | 061 | Conforme |
| QUO-E03 | Mon équipe | ROLE_RESPONSABLE_SERVICE | Ouvrir la journée d’Idriss | Lecture seule : pas de « Ajouter une activité » | Conforme à l’attendu | 062 | Conforme |
| QUO-E04 | Mon équipe | ROLE_RESPONSABLE_SERVICE | Ouvrir l’agenda de Chantal et un événement | Agenda restreint : « catégorie, titre et horaires seulement » | Conforme à l’attendu | 063 | Conforme |
| QUO-E05 | Rapports reçus | ROLE_RESPONSABLE_SERVICE | Ouvrir le rapport déposé d’Idriss, aperçu, viser avec commentaire | Aperçu du document, toast « Rapport visé. » | Conforme à l’attendu | 064, 065, 066 | Conforme |
| QUO-E06 | Rapports | ROLE_RESPONSABLE_SERVICE | Onglets « Reçus » et « Mes rapports » ; son propre rapport | Son rapport sera remis à Paul Mbarga Atangana (n+1) | « Le rapport soumis sera remis à Paul Mbarga Atangana, votre responsable. » (capture 068) | 067, 068 | Conforme |
| QUO-E07 | Tâches | ROLE_RESPONSABLE_SERVICE | Confier une tâche à une personne, la modifier, l’annuler (motif) | « Tâche confiée. », « Tâche modifiée. », « Annulée » | Choix proposés : Une personne / Plusieurs personnes / Une unité | 070, 071 | Conforme |
| QUO-E08 | Tâches | ROLE_RESPONSABLE_SERVICE | Confier une tâche à plusieurs personnes, suivi du groupe, relancer deux fois | « Tâche confiée à 2 personnes. », relance puis refus « moins de 24 heures » | Conforme à l’attendu | 072, 073 | Conforme |
| QUO-E09 | Tâches | ROLE_RESPONSABLE_SERVICE | Confier une tâche à une unité (Pôle réseau), la voir « À prendre », l’attribuer | « Tâche confiée à l’unité « Pôle réseau ». », puis « Tâche attribuée à … » | Unités proposées : Choisir une unité / Service Production informatique / Pôle réseau / Pôle système ; personnes : Choisir un membre / Idriss Mahamat Saleh / Nadège Mvondo Assiga | 074 | Conforme |
| QUO-E10 | Mon équipe | ROLE_RESPONSABLE_SERVICE | Onglet Organisation : gestes permis sur un subordonné | Gestes selon la matrice des pouvoirs par défaut (RATTACHEMENT_MODIFIER : DIRECTS) | Subordonné direct : Confier une tâche, Journée, Agenda, Rapports, Modifier le rattachement, Demander un profil, Demander la désactivation (portée DIRECTS respectée) | 075 | Conforme |
| QUO-E11 | Mon équipe | ROLE_RESPONSABLE_SERVICE | Demander un compte à l’administration, suivre « Mes demandes » | Toast « Demande adressée à l’administration. », demande listée | Unités proposées : Choisir une unité / Service Production informatique / Pôle réseau / Pôle système | 077, 078 | Conforme |
| QUO-ES-mon-equipe | mon-equipe | ROLE_RESPONSABLE_SERVICE | Ouvrir /des/equipe en sombre | Lisible en sombre | Conforme à l’attendu | 079 | Conforme |
| QUO-ES-rapports-recus | rapports-recus | ROLE_RESPONSABLE_SERVICE | Ouvrir /des/rapports en sombre | Lisible en sombre | Conforme à l’attendu | 080 | Conforme |
| QUO-ES-taches | taches | ROLE_RESPONSABLE_SERVICE | Ouvrir /des/taches en sombre | Lisible en sombre | Conforme à l’attendu | 081 | Conforme |
| QUO-P01 | Processus (catalogue) | ROLE_CHEF_DES | Ouvrir le catalogue | Processus groupés par unité, actions selon l’état | Conforme à l’attendu | 085 | Conforme |
| QUO-P02 | Nouveau processus | ROLE_CHEF_DES | Créer sans rien remplir, puis code invalide | Erreurs de saisie expliquées | Libellé manquant signalé ; code saisi « badge » mis en majuscules (« BADGE ») ; unité proposée d’office : DES | 086 | Conforme |
| QUO-P03 | Nouveau processus | ROLE_CHEF_DES | Créer le brouillon DEMANDE_BADGE pour le Service Normes et Agréments | Modélisateur ouvert sur le brouillon, unité SNA | Conforme à l’attendu | 087 | Conforme |
| QUO-P04 | Modélisateur : étape | ROLE_CHEF_DES | Étape DEMANDE (le lanceur), un champ « Motif » obligatoire | Étape ajoutée sur le canevas | Désignation par défaut de la première étape : LANCEUR | 088 | Conforme |
| QUO-P05 | Modélisateur : intervenant par profil | ROLE_CHEF_DES | Étape ANALYSE : porteur du profil Pôle réseau (absent du SNA), validateur chef d’unité, livrable PDF | Couverture : « Aucun porteur dans l’unité ; 1 ailleurs dans le GIMAC » | Couverture : Un porteur du profil / Aucun porteur dans l’unité ; 1 ailleurs dans le GIMAC (Pôle réseau) : l’étape leur sera offerte (une seule personne : affectation automatique). / Tâche à prendre par les porteurs du profil dans l’unité ; s’il n’y en a pas, par ceux du reste du GIMAC ; un seul porteur reçoit l’étape directement. | 089, 090 | Conforme |
| QUO-P06 | Modélisateur : personne nommée | ROLE_CHEF_DES | Étape EXECUTION : personne nommée, profil de remplacement | Liste des personnes groupée « Dans l’unité » / « Ailleurs dans le GIMAC » | Groupes : Dans l’unité (1), Autres personnes de l’annuaire (12) ; 14 options | 091 | Conforme |
| QUO-P07 | Modélisateur : désigné au lancement, signature | ROLE_CHEF_DES | Étape REMISE : désigné au lancement (remplacement SNA), signature du chef d’unité | Signature ajoutée, étape enregistrée | Conforme à l’attendu | 092, 093, 094 | Conforme |
| QUO-P08 | Modélisateur : canevas et liste | ROLE_CHEF_DES | Voir le canevas puis la liste | 4 étapes reliées | Arcs tracés à la souris : true, true, true ; liste : 4 étapes ; DEMANDE ←  ; ANALYSE ← DEMANDE ; EXECUTION ← ANALYSE ; REMISE ←  | 095, 096 | Conforme |
| QUO-P09 | Lancement et formulaire | ROLE_CHEF_DES | Régler les initiateurs (Tout le GIMAC) et ajouter un champ « Période » au formulaire | « Lancement et formulaire enregistrés. » | Règle « Unités désignées » sans unité refusée (« Choisissez au moins une unité. »), puis enregistrement | 097, 098 | Conforme |
| QUO-P10 | Contrôle | ROLE_CHEF_DES | Contrôler le brouillon | Anomalies listées par gravité, information de couverture hors unité | Contrôle terminé : des anomalies empêchent la publication. / Avertissement / Profil porté hors de l’unité / Levable / Bloquante / Levable / 1 anomalie / 2 anomalies | 099 | Conforme |
| QUO-P10b | Modélisateur : code de livrable | ROLE_CHEF_DES | Codes de livrable proposés d’office (LIVRABLE_1) dans deux étapes | Codes proposés d’office uniques dans le processus (pas d’anomalie créée par l’outil) | Anomalie bloquante « Code de livrable en double : LIVRABLE_1 » provoquée par les codes proposés d’office ; corrigée en renommant (BON_REMISE) | 099, 100 | Anomalie QUO-06 |
| QUO-P11 | Publication | ROLE_CHEF_DES | Publier (levée motivée des avertissements si demandée) | Version publiée, lecture seule | Avertissements levés avec motif, version publiée en lecture seule | 101, 102 | Conforme |
| QUO-P12 | Nouvelle version | ROLE_CHEF_DES | Créer une nouvelle version, puis supprimer ce brouillon | Brouillon v2 ouvert, puis supprimé | Nouvelle version v2 (brouillon) créée ; suppression soumise à une case « Je confirme : cette action est irréversible » ; brouillon supprimé puis retour au catalogue (après environ 2 s) | 103, 104 | Conforme |
| QUO-P13 | Retrait | ROLE_CHEF_DES | Retirer la version publiée (motif) | « Version retirée. », état Retirée, plus de « Lancer » | Ligne du catalogue : Demande de badge d’accès DEMANDE_BADGE · v1 Sécurité Retirée 4 08/10/2026 19:03 Ouvrir | 106, 107 | Conforme |
| QUO-P14 | Modélisateur (processus publié) | ROLE_CHEF_DES | Ouvrir « Ouverture d’un flux réseau » publiée en sombre | Consultation seule, lisible en sombre | Conforme à l’attendu | 108, 109 | Conforme |
| QUO-D01 | Lancer une demande | ROLE_SNA | Ouvrir « Lancer une demande » | Processus ouverts, groupés par service destinataire ; le processus interne du pôle n’est pas proposé | Conforme à l’attendu | 110 | Conforme |
| QUO-D02 | Nouvelle demande (IP) | ROLE_SNA | Envoyer le formulaire vide, puis rempli | « Champ obligatoire. » puis instance ouverte (INS-…) | Instance INS-0011 (/des/demandes/INS-0011) | 111, 112, 113 | Conforme |
| QUO-D03 | Mes demandes | ROLE_SNA | Ouvrir « Mes demandes » | Étape en cours tenue par Idriss Mahamat Saleh, 0 sur 4 | Conforme à l’attendu | 114 | Conforme |
| QUO-D04 | Notifications | ROLE_POLE_RESEAU | Ouvrir la cloche après le lancement | Notification de la nouvelle étape « Analyse technique » reçue | Tâche reçue (« Mes étapes », « Tâches ») mais aucune notification d’étape dans la cloche : 3 notifications de démonstration seulement | 115, 116 | Anomalie QUO-05 |
| QUO-D05 | Ma journée : Mes étapes | ROLE_POLE_RESEAU | Lire « Mes étapes de processus » | L’étape « Analyse technique » de la demande y figure | Étapes listées : Poste d’accueil du rez-de-chaussée : Analyse technique ; Ouverture du port 443 pour la supervision : Analyse de sécurité du flux | 117 | Conforme |
| QUO-D06 | Tâches → étape | ROLE_POLE_RESEAU | Ouvrir la tâche, puis l’étape par le lien « Ouvrir » | Étape ouverte, seules ses actions : Terminer, Enregistrer les champs ; pas de pilotage | URL /des/processus/instances/INS-0011?etape=ANALYSE ; aucune action de pilotage | 118, 119 | Conforme |
| QUO-D07 | Étape Analyse technique | ROLE_POLE_RESEAU | Terminer sans le champ obligatoire | Refus expliqué (champ obligatoire) | Message : Des champs obligatoires manquent : | 120 | Conforme |
| QUO-D08 | Étape Analyse technique | ROLE_POLE_RESEAU | Renseigner « Adresse disponible », enregistrer, terminer | Étape terminée, quitte « Mes étapes » | Conforme à l’attendu | 121 | Conforme |
| QUO-D09 | Instance (tiers) | ROLE_COLLABORATEUR | Ouvrir l’instance par l’adresse directe (non intervenante) | Aucune action d’étape ni de pilotage | Écran : « Poste d’accueil du rez-de-chaussée » ; aucune action | 122 | Conforme |
| QUO-D10 | Mes demandes | ROLE_SNA | Suivre l’avancement | Analyse terminée, validation tenue par Gaston Ndzana Ebode | Conforme à l’attendu | 123 | Conforme |
| QUO-D11 | Notifications | ROLE_RESPONSABLE_SERVICE | Ouvrir la cloche | Notification de l’étape de validation | Même constat pour l’étape de validation : aucune notification PROCESSUS | 124, 125 | Anomalie QUO-05 |
| QUO-D12 | Étape Validation | ROLE_RESPONSABLE_SERVICE | Ouvrir l’étape depuis « Mes étapes », décider « Accordée », terminer | Étape terminée | Actions de pilotage visibles pour le chef du service : Suspendre, Annuler l’instance, Réaffecter l’étape | 126, 127 | Conforme |
| QUO-D13 | Étape Mise en œuvre et clôture | ROLE_POLE_RESEAU | Renseigner l’adresse attribuée, terminer ; puis clôture | Instance close | Conforme à l’attendu | 128 | Conforme |
| QUO-D14 | Mes demandes | ROLE_SNA | Constater la clôture (thème sombre) | « Demande close le … », 4 sur 4 | Conforme à l’attendu | 129, 130 | Conforme |
| QUO-D15 | Notifications | ROLE_SNA | Cloche de l’initiatrice après la clôture | Notification de clôture de sa demande | Aucune notification de clôture : la cloche montre les 3 mêmes notifications de démonstration que pour les autres profils (capture 131) | 131 | Anomalie QUO-05 |
| QUO-C01 | Nouvelle demande (congé) | ROLE_COLLABORATEUR | Période inversée, pièce d’un type non admis | « La fin précède le début. », « Type de fichier non admis » | Messages : La fin précède le début. ; Type de fichier non admis | 135 | Conforme |
| QUO-C02 | Nouvelle demande (congé) | ROLE_COLLABORATEUR | Corriger, joindre un justificatif PDF, envoyer | Instance ouverte ; Mes demandes : étape « Vérification des droits » tenue par le service RH | INS-0013 ; Étape en cours : Vérification des droits à congé Active · tenue par Thierry Bekolo Ngoa · échéance 10/10/2026  Vérification des droits à congé Active Accord du supérieur hiérarchique En attente Enregistrement du congé En attente Clôture du dossier En attente L | 136, 137, 138 | Conforme |
| QUO-C04-resp | Étape RH « Vérification des droits » | ROLE_RESPONSABLE_SERVICE | Ouvrir l’étape RH de la demande de congé | Une personne du service RH de démonstration peut agir ; les autres ne voient pas d’action | « L’instance n’a pas pu être lue. Instance introuvable. » avec « Réessayer » : le n+1 (futur intervenant de l’accord) ne lit pas la demande et le refus n’est pas expliqué | 140 | Anomalie QUO-12 |
| QUO-C04-dg | Étape RH « Vérification des droits » | ROLE_DG | Ouvrir l’étape RH de la demande de congé | Une personne du service RH de démonstration peut agir ; les autres ne voient pas d’action | h1 « Congé annuel de décembre » ; exécutant : Thierry Bekolo Ngoa ; actions : Suspendre, Annuler l’instance, Réaffecter l’étape | 141 | Conforme |
| QUO-C04-collab | Étape RH « Vérification des droits » | ROLE_COLLABORATEUR | Ouvrir l’étape RH de la demande de congé | Une personne du service RH de démonstration peut agir ; les autres ne voient pas d’action | Exécutant : Thierry Bekolo Ngoa (RH, sans compte de démonstration) ; aucune action pour la demandeuse | 142 | Conforme |
| QUO-C05 | Réaffectation (DG) | ROLE_DG | Réaffecter l’étape RH, si permis, au collaborateur | Étape réaffectée | Personnes proposées : Choisir une personne | 143 | Conforme |
| QUO-C12 | Demande de congé : service RH | service RH de démonstration | Chercher un profil de démonstration du service RH pour mener les étapes RH | Un compte RH de démonstration reçoit la tâche et agit | Aucun : u-rh (Thierry Bekolo Ngoa) et u-rh-chef (Solange Ateba Mvondo) n’ont pas de profil de démonstration ; les 3 étapes RH n’ont pu être menées qu’après réaffectation par le DG | 141, 142, 143 | Anomalie QUO-01 |
| QUO-C06 | Pièce justificative | ROLE_COLLABORATEUR | Ouvrir la pièce justificative depuis l’instance | Lien ou bouton d’aperçu ou de téléchargement | « note.pdf · 676 o » affiché en texte simple, sans lien ni aperçu | 137, 145 | Anomalie QUO-04 |
| QUO-C07 | Réaffectation | ROLE_DG | Réaffecter l’étape RH « Vérification des droits » | Personnes du service RH proposées ; pas le demandeur | Liste : la demandeuse Nadège Mvondo Assiga proposée en premier et choisie d’office (« Seul porteur du profil Collaborateur : choisi automatiquement », faux : Thierry Bekolo Ngoa porte aussi ce profil), puis tout l’annuaire ; réaffectation acceptée | 143, 146, 147 | Anomalie QUO-02 |
| QUO-C08 | Étape RH réaffectée | ROLE_COLLABORATEUR | Mes étapes, puis renseigner le solde et terminer | Étape terminée ; accord du supérieur activé | Conforme à l’attendu | 148 | Conforme |
| QUO-C09 | Accord du supérieur (n+1) | ROLE_RESPONSABLE_SERVICE | Mes étapes, ouvrir l’étape, décider « Accordé » | Étape tenue par le n+1 du demandeur, terminée | Mes étapes : Congé de fin d’année : Accord du supérieur hiérarchique Active 10/10/2026 Flux SFTP vers la Banque de Libreville : Recette du flux À signer 10/10/2026 | 149, 150 | Conforme |
| QUO-C10 | Enregistrement du congé | ROLE_COLLABORATEUR | Déposer la décision (PDF) et terminer | Livrable déposé, étape terminée | Conforme à l’attendu | 151 | Conforme |
| QUO-C11 | Clôture | ROLE_COLLABORATEUR | Terminer la clôture puis lire « Mes demandes » | Demande close, 4 sur 4 | Conforme à l’attendu | 152 | Conforme |
| QUO-G01 | Désignation par profil hors unité (plusieurs porteurs) | ROLE_CHEF_DES | Étape RECENSEMENT : profil Collaborateur, absent du SNA | Couverture « Aucun porteur dans l’unité ; 2 ailleurs dans le GIMAC », tâche à prendre | Aucun porteur dans l’unité ; 2 ailleurs dans le GIMAC (Pôle réseau, Service des ressources humaines) : l’étape leur sera offerte (une seule personne : affectation automatique). | 158 | Conforme |
| QUO-G02 | Désigné au lancement : « Dans l’unité » / « Ailleurs dans le GIMAC » | ROLE_CHEF_DES | Étape CONTROLE désignée au lancement, remplacement Pôle réseau | Étape enregistrée | Conforme à l’attendu | 159 | Conforme |
| QUO-G03 | Lancement (désignation) | ROLE_CHEF_DES | Lancer : liste de la désignation au lancement | Désignation au lancement : porteurs du profil de remplacement proposés d’abord ; un seul porteur : choisi d’office | Aucun porteur Pôle réseau au SNA, un seul ailleurs : « Seul porteur du profil Pôle réseau : choisi automatiquement. » (Idriss Mahamat Saleh) ; instance lancée | 159, 160 | Conforme |
| QUO-G04 | Tâche à prendre (plusieurs porteurs) | ROLE_COLLABORATEUR | Onglet « À prendre », prendre l’étape | Étape « à prendre » proposée, puis prise : « Étape prise : vous en êtes l’exécutant. » | Tâche à prendre visible mais l’étape s’ouvre sur « L’instance n’a pas pu être lue. Instance introuvable. » ; fiche : boutons  / Fermer / Prendre la tâche ; liens Ouvrir | 162, 163, 164 | Anomalie QUO-03 |
| QUO-G05 | Tâche à prendre (plusieurs porteurs) | ROLE_COLLABORATEUR | Prendre la tâche depuis sa fiche, puis ouvrir l’étape | Tâche prise, étape ouverte avec « Terminer l’étape » | Message : Tâche prise : vous en êtes le responsable. | 166, 167 | Conforme |
| QUO-M01 | Messagerie | ROLE_COLLABORATEUR | Ouvrir la messagerie | Liste des conversations, non-lus | 3 conversations | 170 | Conforme |
| QUO-M02 | Messagerie : présence | ROLE_COLLABORATEUR | Ouvrir la conversation avec Aïcha (en ligne) | Pastille « En ligne » dans la liste et le fil | Conforme à l’attendu | 171 | Conforme |
| QUO-M03 | Messagerie : accusés de lecture | ROLE_COLLABORATEUR | Envoyer un message, puis lecture par Aïcha (simulée) | Coche « Envoyé » puis « Lu » | Conforme à l’attendu | 172 | Conforme |
| QUO-M04 | Messagerie : pièces jointes et aperçus | ROLE_COLLABORATEUR | Joindre PDF, docx, pptx ; ouvrir chaque aperçu | Aperçus rendus (page 1 sur 1), téléchargement possible | note.pdf : page rendue ; compte-rendu.docx : page rendue ; point-avancement.pptx : page rendue | 173, 174, 175 | Conforme |
| QUO-M05 | Messagerie : répondre et mentionner | ROLE_COLLABORATEUR | Répondre à un message du groupe en mentionnant avec @ | Citation dans la bulle, mention mise en évidence | Membres proposés : Rachel Biyong Mengue, Aïcha Mahamat Nour, Hervé Kamga Tchoupo | 176, 177 | Conforme |
| QUO-M06 | Messagerie : lien de liaison | ROLE_COLLABORATEUR | Suivre le lien « Ouvrir la liaison LIA-2026-030 » du groupe | Profil sans droit d’homologation : refus propre | Accès refusé (le jeu de démonstration place la collaboratrice dans un groupe d’homologation) | 178 | Conforme |
| QUO-M07 | Messagerie : groupe | ROLE_COLLABORATEUR | Créer un groupe vide (refus), puis avec deux personnes ; renommer | Refus expliqués puis groupe créé | Conforme à l’attendu | 179, 180 | Conforme |
| QUO-M08 | Messagerie : élargir | ROLE_COLLABORATEUR | Bouton « Élargir » | Barre latérale réduite aux pictogrammes | Barre latérale : 72 px | 181 | Conforme |
| QUO-M09 | Messagerie : message reçu, mention | ROLE_COLLABORATEUR | Réception simulée d’un message qui me mentionne (hors messagerie) | Fenêtre surgissante et cloche | Réception simulée (gmDemoNotifications) : cloche et pastille mises à jour mais aucune fenêtre surgissante en 4 essais manuels ; le scénario automatisé notifications.spec.ts rejoué sur le port 6100 passe (capture 182) | 182 | Anomalie QUO-11 |
| QUO-M10 | Messagerie : en train d’écrire | ROLE_COLLABORATEUR | Observer l’indicateur « … écrit… » | Indicateur affiché quand un correspondant écrit | Non démontrable : la démonstration n’émet aucun signal collaboration.message-typing.v1 (socle/src/live/signaux.ts n’est alimenté que par le flux du serveur) | (aucune) | Anomalie QUO-10 |
| QUO-M11 | Messagerie (sombre) | ROLE_COLLABORATEUR | Conversation de groupe en thème sombre | Lisible | Conforme à l’attendu | 184 | Conforme |
| QUO-N01 | Notifications (cloche) | ROLE_COLLABORATEUR | Ouvrir la cloche puis « Voir tout » | Panneau des notifications, non-lues, « Tout marquer comme lu », « Voir tout » | Éléments : Tâche : Tâche confiée par Paul Mbarga Atangana, non lue / Rapport : Rapport de Joël Ekotto Mballa / Homologation : Action attendue : Atelier comptabilisation / Message : Rachel Biyong Mengue, non lue / Message : Aïcha Mahamat Nour, non lue | 190, 192 | Conforme |
| QUO-N02 | Préférences de notification | ROLE_COLLABORATEUR | Ouvrir les préférences (Mon compte), basculer un interrupteur | « Préférences de notification enregistrées. » ; interrupteurs adaptés au profil | interrupteurs sans objet pour une collaboratrice sans homologation ni équipe : Rapports qui me sont soumis ; Homologation : opérations des liaisons à mener, valider ou signer | 191 | Anomalie QUO-08 |
| QUO-K01 | Recherche globale Ctrl+K | ROLE_COLLABORATEUR | Ctrl+K, « liaisons », « Essais », « zzzz » | Seuls les écrans permis sont proposés ; objets ; « Aucun résultat » | « liaisons » : aucun ; « Essais » : Essais de LIA-2026-030 LIA-2026-030 | 193, 194 | Conforme |
| QUO-K02 | Recherche globale Ctrl+K | ROLE_COLLABORATEUR | Chercher « Nadège »/« biyong » (personne) et ouvrir | Résultats limités aux objets accessibles | « biyong » ne propose que la conversation « Essais de LIA-2026-030 » (aucune fiche de compte : l’annuaire des comptes relève de l’administration) ; aucune fuite | 195 | Conforme |
| QUO-K03 | Mon compte | ROLE_COLLABORATEUR | Ouvrir « Mon compte » : rattachement, photo, appareils, historique | Cartes présentes ; rattachement Pôle réseau, n+1 Gaston | Conforme à l’attendu | 197 | Conforme |
| QUO-K04 | Mon compte : photo | ROLE_COLLABORATEUR | Déposer un faux PNG (refus), puis un vrai PNG | Refus expliqué, puis photo dans le menu utilisateur | Refus : Choisissez une image, puis cadrez-la : seul le carré retenu est envoyé, redimensionné à 256 × 256 px et réencodé (les métadonnées, dont la position GPS, sont supprimées). / Image * | 198, 199 | Conforme |
| QUO-K05 | Mon compte : appareils et historique | ROLE_COLLABORATEUR | Fermer une autre session (confirmation), parcourir l’historique | Session fermée ; historique paginé | Conforme à l’attendu | 200 | Conforme |
| QUO-R10 | Rapport au sommet | ROLE_DG | Rédiger puis soumettre un rapport quotidien (personne au-dessus) | « Personne n’est au-dessus de vous… », rapport « Déposé » | Annonce dans « Nouveau rapport » :  | 201, 202 | Conforme |
| QUO-R11 | Mon équipe (sommet) | ROLE_DG | Ouvrir « Mon équipe » | Équipe de toute l’organisation (chaîne hiérarchique) | 10 lignes | 203 | Conforme |
| QUO-H01 | Chef DDRM : équipe et processus | ROLE_CHEF_DDRM | Ouvrir « Mon équipe » et « Processus de l’unité » | Équipe DDRM ; seuls les processus de ses unités | Équipe : Fatimé Abakar Hissein / Chargée de la DDRM · Département Développement du Réseau et du Marché ; processus : Aucun processus | 204, 205 | Conforme |
| QUO-H02 | Chef DDRM : pouvoirs | ROLE_CHEF_DDRM | Onglet Organisation de « Mon équipe », gestes sur un subordonné | Gestes de la matrice des pouvoirs | Gestes : Confier une tâche / Modifier le rattachement / Demander un profil / Demander la désactivation | 206 | Conforme |
| QUO-H03 | Chef DES : organisation (sombre) | ROLE_CHEF_DES | Onglet Organisation, subordonné indirect | Pas de « Modifier le rattachement » (portée DIRECTS) | Gestes : Confier une tâche / Demander un profil / Demander la désactivation | 207 | Conforme |
| QUO-I-ma-journee | ma-journee | ROLE_RESPONSABLE_SERVICE | Ouvrir /des/activite en en-US | Aucune clé de traduction brute, aucun texte français résiduel | Textes français résiduels (souvent données) : Point de situation avec l’équipe / Planifier la maintenance des serveurs de recette / Suite des travaux de la veille. / Classer les comptes rendus de la semaine / Répondre aux courriels en attente | 208 | Conforme |
| QUO-I-mon-equipe | mon-equipe | ROLE_RESPONSABLE_SERVICE | Ouvrir /des/equipe en en-US | Aucune clé de traduction brute, aucun texte français résiduel | Textes français résiduels (souvent données) : Ingénieur réseau · Pôle réseau / Ingénieure système · Pôle système / Nadège Mvondo Assiga / Technicienne réseau · Pôle réseau | 209 | Conforme |
| QUO-I-lancer-demande | lancer-demande | ROLE_RESPONSABLE_SERVICE | Ouvrir /des/demandes/lancer en en-US | Aucune clé de traduction brute, aucun texte français résiduel | Textes français résiduels (souvent données) : Pôle réseau / Demande adressée au pôle réseau pour changer l’adresse IP d’un poste ou d’un équipement : analyse, validation, mise en œuvre et clôture. / Réseau / Ouverture d’un flux réseau / Demande, analyse de sécurité, mise en œuvre et recette d’un flux entre la plateforme et un participant. | 210 | Conforme |

Total : **145 contrôles**, **133 conformes**, **12 en anomalie** (rattachés aux anomalies de la section 4, qui en compte d’autres relevées à la lecture des captures).

## 3. Matrice des accès du lot

V : entrée visible dans la barre latérale ; R : adresse directe refusée (« Accès refusé : Votre profil ne donne pas accès à cet écran. », aucune donnée) ; - : entrée absente (attendu d’après le catalogue `fonctionnalites-initiales.json` et la matrice PROFILS). Toutes les cases sont conformes à l’attendu ; « Mon compte » et « Se déconnecter » (groupe Mon espace) sont communs à tous.

| Entrée (route) | Collab. | Resp. service | Chef DES | Chef DDRM | Pôle réseau | DG | SNA |
|---|---|---|---|---|---|---|---|
| Tableau de bord (`/des/tableau-de-bord`) | V | V | V | V | V | V | V |
| Messagerie (`/des/messagerie`) | V | V | V | V | V | V | V |
| Notifications (`/des/notifications`) | V | V | V | V | V | V | V |
| Ma journée (`/des/activite`) | V | V | V | V | V | V | V |
| Agenda (`/des/agenda`) | V | V | V | V | V | V | V |
| Tâches (`/des/taches`) | V | V | V | V | V | V | V |
| Rapports (`/des/rapports`) | V | V | V | V | V | V | V |
| Mon équipe (`/des/equipe`) | R | V | V | V | R | V | R |
| Processus de l’unité (`/des/processus`) | V | V | V | V | V | V | V |
| Lancer une demande (`/des/demandes/lancer`) | V | V | V | V | V | V | V |
| Mes demandes (`/des/demandes`) | V | V | V | V | V | V | V |
| Institutions membres (`/des/institutions`) | R | R | V | V | V | V | V |
| Lancer une certification (`/des/certification/lancer`) | R | - | V | V | R | - | - |
| Mes actions de certification (`/des/certification/mes-actions`) | - | - | V | V | V | V | V |
| Liaisons et parcours (`/des/parcours`) | R | R | V | V | V | V | V |
| Référentiels d’opérations (`/des/referentiels/operations`) | R | V | V | V | V | V | V |
| Adresse directe `/des/administration/comptes` | R | R | R | non testé | R | R | non testé |
| Adresse directe `/des/administration/matrice-pouvoirs` | R | R | R | R | non testé | R | non testé |
| Adresse directe `/des/administration/demandes` | R | non testé | R | non testé | non testé | non testé | non testé |
| Adresse directe `/des/plateforme/supervision` | R | non testé | non testé | non testé | non testé | R | non testé |
| Adresse directe `/des/plateforme/configuration` | non testé | R | non testé | non testé | non testé | non testé | non testé |
| Adresse directe `/des/equipe/demandes` | non testé | non testé | non testé | non testé | R | non testé | non testé |

Accès aux instances de processus (`/des/processus/instances/<id>`, `/des/demandes/<id>`, garde PIL_ACTIVITY_OWN puis contrôle de lecture du service) : initiateur, intervenant courant, membres de l’unité propriétaire et chefs couvrant l’unité lisent l’instance ; les actions affichées sont celles du lecteur (exécutant : champs, dépôt, « Terminer l’étape » ; pilote : « Suspendre », « Annuler l’instance », « Réaffecter l’étape »). Écarts : candidats d’une étape « à prendre » et futur n+1 refusés par « Instance introuvable » (QUO-03, QUO-12).

## 4. Anomalies

Synthèse : **0 bloquante, 5 majeures, 15 mineures**.

### QUO-01 (majeure) : aucun compte de démonstration du service RH, la demande de congé ne peut pas être traitée par les RH
- Étapes : en collaboratrice, « Lancer une demande » › « Demande de congé », envoyer ; « Mes demandes » : étape « Vérification des droits à congé » tenue par Thierry Bekolo Ngoa ; aucun profil de démonstration ne correspond à ce compte (ni à Solange Ateba Mvondo, cheffe du service) ; le collaborateur, le responsable et le DG n’ont aucune action d’exécutant (le DG peut seulement piloter et réaffecter).
- Constat : le congé n’a pu être mené à la clôture qu’en faisant réaffecter les trois étapes RH par le DG à la demandeuse elle-même (captures 141, 142, 143, 146 à 152).
- Cause probable : `frontend/packages/lot1/src/services/simule/pilotage.ts` (ORGANISATION_DEMO et PROFILS_ORGANISATION_DEMO : u-rh et u-rh-chef) alors que `frontend/packages/lot1/src/profilsDemo.ts` ne crée un profil de démonstration que pour les comptes de l’ANNUAIRE de la messagerie (`services/simule/messagerie.ts`), un par rôle : le gestionnaire RH (ROLE_COLLABORATEUR) est masqué par Nadège Mvondo Assiga.

### QUO-02 (majeure) : la réaffectation d’une étape RH propose d’office la demandeuse elle-même
- Étapes : congé lancé par Nadège Mvondo Assiga ; en DG, ouvrir l’étape ANALYSE (`/des/processus/instances/<id>?etape=ANALYSE`) › « Réaffecter l’étape ».
- Constat : « Nouvel exécutant » préréglé sur la demandeuse avec l’aide « Seul porteur du profil Collaborateur : choisi automatiquement. », ce qui est faux (Thierry Bekolo Ngoa, au service RH, porte aussi ce profil), puis tout l’annuaire ; la réaffectation est acceptée et la demandeuse vérifie ses propres droits à congé puis enregistre sa propre décision (captures 143, 147, 151). Aucune séparation des fonctions (demandeur et intervenant).
- Cause probable : liste des candidats de la réaffectation (`services/simule/processus.ts`, recherche des porteurs) qui exclut l’exécutant courant, ne se limite pas à l’unité de l’étape (RH) et n’écarte pas l’initiateur.

### QUO-03 (majeure) : une tâche « à prendre » d’étape de processus s’ouvre sur « Instance introuvable »
- Étapes : en chef du DES, publier un processus du SNA dont l’étape est confiée au profil Collaborateur (aucun porteur au SNA, deux ailleurs) et le lancer ; en collaboratrice, « Tâches » › « À prendre » › la tâche › lien « Ouvrir ».
- Constat : « L’instance n’a pas pu être lue. Instance introuvable. » ; le bouton « Prendre l’étape » de l’instance est donc inaccessible ; seule la fiche de la tâche (« Prendre la tâche ») permet la prise, après quoi l’étape s’ouvre normalement (captures 162 à 167).
- Cause probable : contrôle de lecture des instances du service simulé (`services/simule/processus.ts`) qui admet l’initiateur, les intervenants désignés et les membres de l’unité, mais pas les candidats d’une étape à prendre hors de l’unité.

### QUO-04 (majeure) : la pièce justificative du formulaire de lancement n’est pas consultable
- Étapes : lancer une demande de congé avec une pièce justificative PDF ; ouvrir l’instance (initiatrice, intervenant RH ou DG).
- Constat : « Pièce justificative : note.pdf · 676 o » en texte simple, sans lien, aperçu ni téléchargement (captures 137, 145) ; l’intervenant RH chargé de vérifier les droits ne peut pas lire le justificatif. Le livrable déposé à une étape s’affiche de même sous forme de texte (capture 151).
- Cause probable : rendu des valeurs de lancement de type PIECE_JOINTE (zone `valeurs-lancement` de la page d’instance, `pages/des/processus/`).

### QUO-05 (majeure) : aucune notification d’étape ni de clôture de demande en services simulés
- Étapes : lancer la demande d’adresse IP (SNA) ; ouvrir la cloche d’Idriss Mahamat Saleh, puis celle de Gaston Ndzana Ebode à l’étape de validation, puis celle de l’initiatrice après la clôture.
- Constat : la tâche arrive bien (« Mes étapes », « Tâches »), mais la cloche ne montre que les 3 notifications de démonstration, identiques pour tous (captures 115, 116, 124, 125, 131) ; la règle « chaque notification d’étape mène à une action » (docs/lot1/lancement-processus.md) n’est pas démontrable.
- Cause probable : `frontend/packages/lot1/src/services/notifications.ts` (notificationsDemo) n’est branché que sur la messagerie et les décisions ; le service simulé des processus n’émet aucune notification PROCESSUS ni DEMANDE_AVANCEE.

### QUO-06 (mineure) : codes de livrable proposés d’office en double, anomalie bloquante créée par l’outil
- Étapes : modélisateur, ajouter un livrable à l’étape ANALYSE puis un à l’étape REMISE sans retoucher les codes ; « Contrôler ».
- Constat : « Bloquante : Code de livrable en double : REMISE · LIVRABLE_1 » ; publication refusée (« Le processus présente des anomalies bloquantes : corrigez-les. ») jusqu’au renommage (captures 099, 100).
- Cause probable : numérotation des codes proposés par étape et non par processus (modale d’étape du modélisateur, `pages/des/modelisateur/`).

### QUO-07 (mineure) : codes techniques montrés à l’utilisateur
- « Des champs obligatoires manquent : ADRESSE_DISPONIBLE » au lieu du libellé « Adresse disponible » (capture 120) ; information de couverture « Aucune personne active ne porte ROLE_POLE_RESEAU dans l’unité SNA ; 1 porteur ailleurs dans le GIMAC (POLE_RESEAU : 1) » (captures 099, 102) ; « Types admis : application/pdf,image/* » dans le formulaire de congé (capture 136) ; type MIME complet sous le titre de l’aperçu d’un docx (capture 174) ; historique « Étape soumise · EXECUTION ».

### QUO-08 (mineure) : préférences de notification sans objet pour le profil
- Mon compte › Notifications d’une collaboratrice : interrupteurs « Rapports qui me sont soumis » et « Homologation : opérations des liaisons à mener, valider ou signer » proposés alors qu’elle n’a ni équipe ni droit d’homologation (capture 191).

### QUO-09 (mineure) : jeu de démonstration identique pour tous les profils
- La collaboratrice reçoit « Rapport de Joël Ekotto Mballa » et « Homologation : Action attendue : Atelier comptabilisation » (capture 190) et figure dans le groupe « Essais de LIA-2026-030 », dont le lien « Ouvrir la liaison » mène à « Accès refusé » (capture 178) ; l’initiatrice SNA a les mêmes notifications (capture 131).
- Cause probable : `services/notifications.ts` (initiales communes) et `services/simule/messagerie.ts` (conversations de démonstration non filtrées par profil).

### QUO-10 (mineure) : indicateur « … écrit… » non démontrable
- Aucun signal `collaboration.message-typing.v1` n’est simulé : l’indicateur de la messagerie (`MessageriePage.tsx`, `socle/src/live/signaux.ts`) ne s’observe qu’avec le serveur.

### QUO-11 (mineure, à confirmer) : fenêtre surgissante non reproduite à la main
- Quatre réceptions simulées (`gmDemoNotifications.recevoirMessage`) sur le tableau de bord, avec et sans « Tout marquer comme lu » : la pastille de la cloche augmente mais aucune fenêtre surgissante n’apparaît ; le scénario automatisé `notifications.spec.ts` rejoué sur le port 6100 passe et montre la fenêtre (capture 182). Écart de conditions non élucidé (probablement l’ordre de chargement de la liste récente dans `socle/src/notifications/Surgissantes.tsx`).

### QUO-12 (mineure) : refus non expliqué pour le n+1 avant son étape
- Le responsable (n+1 de la demandeuse, futur intervenant de « Accord du supérieur hiérarchique ») ouvre la demande de congé : « L’instance n’a pas pu être lue. Instance introuvable. » et « Réessayer » (capture 140), au lieu d’un refus d’accès explicite.

### QUO-13 (mineure) : accord du pluriel
- « 1 faites sur 3 » (Mon équipe, capture 061), « 1 activités faites sur 4 » (rapport du DG, capture 201).

### QUO-14 (mineure) : colonne « Accès » de Mon équipe
- Les liens empilés « Journée », « Agenda », « Rapports » débordent de la ligne et touchent ses bords (captures 061, 079).

### QUO-15 (mineure) : messages contradictoires dans le suivi d’un groupe
- Après une seconde relance, l’erreur « Une relance a déjà été envoyée il y a moins de 24 heures. » et le succès précédent « Relance envoyée à Idriss Mahamat Saleh. » s’affichent ensemble (capture 073).

### QUO-16 (mineure) : vue Liste du modélisateur coupée à 1440 px
- La dernière colonne est tronquée (« Modifi… ») sans défilement visible (capture 096).

### QUO-17 (mineure) : participants affichés deux fois dans « Nouvel événement »
- La personne choisie apparaît en puce au-dessus du champ de recherche et de nouveau dans « 1 personne choisie » (capture 025).

### QUO-18 (mineure) : date au format ISO dans le rapport prérempli
- « Rapport quotidien du 2026-10-08 » dans un texte en français (capture 045).

### QUO-19 (mineure, démonstration) : tâches de processus perdues au rechargement
- Ouvertes directement, « Tâches » de la collaboratrice montre aussi ses tâches de processus terminées (INS-0007/DEMANDE, INS-0003/MISE_EN_OEUVRE, capture 027) ; après un rechargement de page, seules ses 2 tâches personnelles restent (capture 057). Cause probable : restauration de l’état simulé (`services/simule/processus.ts`, sessionStorage gm.demoProcessus.v1) qui ne recrée que les tâches des étapes en cours.

### QUO-20 (mineure) : onglet « Mes demandes » de Mon équipe mis en avant en thème sombre
- En sombre, « Mes demandes » s’affiche en texte clair et gras comme un onglet actif alors que « Activité du jour » est sélectionné (capture 079 ; rendu correct en clair, capture 061).

## 5. Points vérifiés conformes (rappel)

Barre latérale exactement conforme au catalogue pour les 7 profils ; adresses interdites refusées proprement ; Ma journée, agenda (invitation, création avec participant, journée entière, suppression confirmée), tâches (motif exigé pour rouvrir, confier à une personne, à plusieurs, à une unité, attribuer, relancer, annuler), rapports en trois modes avec « Sera remis à », choix dans la chaîne hiérarchique, refus expliqués (rapport existant, vide, sans document), visa commenté, dépôt au sommet ; modélisateur complet (intervenant par profil, personne nommée, désigné au lancement, remplacement, livrables, signatures, champs, arcs au canevas, Lancement et formulaire, contrôle par gravité, levée motivée, publication, nouvelle version, suppression confirmée, retrait) ; demande d’adresse IP menée à la clôture, chaque intervenant ne voyant que ses actions ; « Mes étapes » limitée à ce qui attend la personne ; désignation hors unité (un porteur : affectation directe ; plusieurs : à prendre) ; pouvoirs des chefs selon la matrice (DIRECTS pour le rattachement) et demande de compte à l’administration ; messagerie (présence, accusés Envoyé puis Lu, aperçus PDF, docx et pptx, réponse avec citation, mention @, groupe, élargissement) ; cloche, page et préférences ; Ctrl+K sans fuite (aucune liaison proposée à la collaboratrice) ; Mon compte (photo refusée puis acceptée, sessions, historique) ; thème sombre lisible ; 390 px sans défilement horizontal ; en-US sans clé brute.

## 6. Correction (09/10/2026)

Les 20 anomalies sont corrigées. Registre avant/après : `docs/recette/corrections/quotidien.json` ; captures après :
`docs/recette/captures/quotidien/corrections/QUO-NN-apres.png` (application interne simulée, 1440 × 900, fr-FR).
Règle structurante : **D-99, séparation des fonctions** (`REC-GIMAC-01_Reconciliation.md`, contrat
`docs/lot1/lancement-processus.md` §9).

| N° | Correctif (serveur et démonstration) | Test |
|---|---|---|
| QUO-01 | Comptes de démonstration du service RH tirés de l’organisation simulée : `t.bekolo` (gestionnaire, profil collaborateur) et `s.ateba` (cheffe, profil responsable de service, comme l’organisation simulée de MS-PILOTAGE) ; la session garde le compte choisi et non le premier porteur du profil. Côté serveur, les comptes RH existaient déjà (organisation simulée) : rien à corriger. | `App.test.tsx` (QUO-01), `quotidien.test.ts` |
| QUO-02 | D-99 : l’initiateur n’est ni résolu, ni désigné au lancement, ni proposé, ni réaffecté, et ne prend, n’exécute, ne valide ni ne signe les étapes de sa demande (MS-PILOTAGE : `ResolutionIntervenants`, `ExecutionProcessusService`, contrôle `PriseTacheProcessus` de la prise et de l’attribution, offre non listée à l’initiateur ; démonstration identique). Refus `PIL_PROCESSUS_SEPARATION_FONCTIONS`, même message. Exceptions : intervenant `LANCEUR`, réglage `initiateurs.initiateurIntervenant` (case du modélisateur, non par défaut). Aucun autre porteur : chef de l’unité (D-95), chef initiateur : son responsable. L’aide « Seul porteur … choisi automatiquement » n’est plus donnée quand un porteur est écarté. | `ProcessusSeparationFonctionsIT` (4 scénarios), ITs du moteur adaptés, `quotidien.test.ts`, `Processus.test.tsx` |
| QUO-03 | Le porteur qui peut prendre une étape (ou la valider, la signer, par son profil) lit l’instance, côté serveur et en démonstration ; « Prendre l’étape » est accessible depuis l’instance. Pas de capture : le scénario exige de modéliser un processus confié à un profil porté hors de l’unité. | `ProcessusSeparationFonctionsIT.lecturePorteursEtDocuments`, `quotidien.test.ts` |
| QUO-04 | Route `GET /processus/instances/{id}/documents/{documentId}` (lecture de l’instance exigée, OPA), pièce du formulaire et livrables ouverts en aperçu et téléchargeables ; démonstration alignée (contenu gardé dans la session). | IT ci-dessus, `quotidien.test.ts`, `Processus.test.tsx`, `lot1_authz_test.rego` |
| QUO-05 | Le moteur simulé produit les notifications PROCESSUS de MS-NOTIFICATION (étape à réaliser ou à prendre, validation ou signature attendue, renvoi, réaffectation, « Votre demande avance », processus terminé ou annulé), gardées avec l’état de la session. Côté serveur, déjà émises (événements `pilotage.process-*`). | `quotidien.test.ts`, `notifications.test.ts`, E2E `notifications` |
| QUO-06 | Code de livrable proposé d’office : premier `LIVRABLE_n` libre dans tout le processus. Le serveur ne propose pas de code : rien à corriger. | `Processus.test.tsx` (QUO-06) |
| QUO-07 | Champs et livrables manquants nommés par leur libellé, historique nommant l’étape, types admis et type d’un aperçu en noms courts traduits (PDF, images, Word) ; couverture : profils et unités nommés (correctif HOM-15 du même écran). | `typesLisibles.test.ts`, `Processus.test.tsx` |
| QUO-08 | Préférences proposées selon les permissions (rapports soumis : équipe ; demandes : équipe, proposition de statut ou administration ; homologation : lecture des liaisons). MS-NOTIFICATION n’adresse déjà ces types qu’aux comptes concernés. | `notifications.test.tsx` (QUO-08), E2E `notifications` |
| QUO-09 | Sans lecture des liaisons, les conversations de démonstration ne portent ni liaison ni étiquette ; une référence de liaison écrite dans le texte d’un message n’est un lien, et « Citer une liaison » n’est offert, que pour un lecteur qui lit les liaisons (`MessageriePage.tsx`) ; notifications de démonstration propres au profil (correctif DDR-20). | `quotidien.test.ts` (QUO-09), `MessageriePage.test.tsx` (QUO-09), E2E `messagerie.spec.ts` |
| QUO-10 | Signal « … écrit… » simulé en démonstration (`gmDemoNotifications.ecrire()`). Serveur : signal déjà porté par le canal poussé. | `MessageriePage.test.tsx` (QUO-10) |
| QUO-11 | Défaut confirmé : une notification reçue avant la première lecture de la liste récente ne surgissait jamais ; elle surgit si elle est arrivée depuis l’ouverture de l’écran. | `notifications.test.tsx` (QUO-11) |
| QUO-12 | Refus de lecture expliqué (« Vous ne pouvez pas consulter cette demande », sans « Réessayer ») ; « Demande introuvable » réservé au 404 ; la démonstration renvoie 403 comme le service. | `quotidien.test.ts`, `Processus.test.tsx`, `App.test.tsx` |
| QUO-13 | Accords au singulier et au pluriel (fr, en, es). | lecture de la capture |
| QUO-14 | Liens de la colonne Accès côte à côte, sans débordement. | lecture de la capture |
| QUO-15 | Une nouvelle relance efface le retour précédent. | lecture de la capture |
| QUO-16 | Cellule d’actions des tableaux gardée comme cellule (elle sortait de la grille), ajustée à ses boutons ; en-têtes longs de la vue Liste renvoyés à la ligne. | lecture de la capture |
| QUO-17 | Participants choisis affichés une seule fois. | `AgendaPage.test.tsx` |
| QUO-18 | Préremplissage de la démonstration aligné sur MS-PILOTAGE (déjà au format local) : « Rapport quotidien du vendredi 9 octobre 2026 ». | `pilotage.test.ts` |
| QUO-19 | Les tâches des étapes terminées ou annulées renaissent aussi au rechargement, sans doublon. | `quotidien.test.ts` (QUO-19) |
| QUO-20 | Le survol d’un onglet le souligne sans le confondre avec l’onglet actif (sombre). | lecture de la capture |

Validation : `./mvnw -o verify` de MS-PILOTAGE (800 tests, succès) ; OPA 174/174 ; `node infra/ci/contrat/routes.mjs` sans
écart nouveau ; tests unitaires de `@gimac/lot1`, `@gimac/design-system`, `@gimac/socle`, `@gimac/compte` et de
l’application du département ; E2E `lancement-processus`, `processus-unite` et `notifications` (port 6500, clair et sombre).
