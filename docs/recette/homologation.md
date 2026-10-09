# Recette du lot HOM : référentiels et homologation

Validateur : recette exhaustive GIMAC « Moabi », passage du 08/10/2026.
Environnement : applications construites en services simulés, application interne http://localhost:6100, portail
http://localhost:6101 (ports 6000 à 6002 refusés par Chromium, ERR_UNSAFE_PORT).
Navigateur : Chromium sans tête (Playwright), contexte `fr-FR`, 1440 x 900 sauf mention, un seul navigateur ouvert à la fois,
piloté pas à pas (un geste, une capture, lecture de la capture). Démonstration continue (`gm.demoContinue = 1`) pour enchaîner
les acteurs sur une même liaison ; changement d'acteur par `gm.profilDemo` et rechargement.
Captures : `docs/recette/captures/homologation/` (138 fichiers, numérotés dans l'ordre du parcours).
Console du navigateur et réseau surveillés sur tout le parcours : aucune erreur JavaScript, aucune réponse HTTP en erreur.
Scripts de recette (hors dépôt) : scratchpad `recette/homologation/` (pilote interactif `pilote.mjs`, `pas.sh`, `01-acces.mjs`,
`02-vue-filtree.mjs`, `03-themes.mjs`).

## 1. Périmètre

Profils :

| Profil | Compte de démonstration | Rôle dans la recette |
|---|---|---|
| Ingénieur d'homologation (ROLE_INGENIEUR_HOMOLOGATION) | Joël Ekotto Mballa | référentiels, modélisateur, types de projet, fonctions, administration de tous les domaines (REF_METIER_ADMIN) |
| Chargé d'homologation | n'existe pas comme profil : le domaine Keycloak et D-89 confondent « chargé d'homologation » et ingénieur d'homologation | sans objet |
| Conducteur de projet (ROLE_CONDUCTEUR_PROJET) | Aïcha Mahamat Nour | dossier de liaison, gouvernance, renvoi, validations, procès-verbal, signature ordonnée |
| Normes et Agréments (ROLE_SNA) | Rachel Biyong Mengue | essais, constat de couverture, première signature ordonnée, refus RACI |
| Pôle réseau (ROLE_POLE_RESEAU) | Idriss Mahamat Saleh | vue filtrée, opération à prendre (fonction à affecter) |
| Pôle système (ROLE_POLE_SYSTEME) | Chantal Ngo Bassong | vue filtrée, refus RACI |
| Production monétique (ROLE_PRODUCTION_MONETIQUE) | Serge Ondoua Essomba | dossier et fiche de changement, bascule en production |
| Chef de service (ROLE_RESPONSABLE_SERVICE) | Gaston Ndzana Ebode | vue de son unité, édition du référentiel sans publication |
| Acteurs d'appui | chef du DES (lancement, signature du PV), chargé de la DDRM (cadrage, réunion de lancement), RSSI (approbation), correspondant (portail) | nécessaires au parcours complet |

Écrans : tableau de bord (Mon travail), Référentiels d'opérations (arbre, modélisateur canevas et liste, contrôle, comparaison,
étapes, export, modale d'opération en six onglets), Types de projet et fonctions, Processus de l'unité (catalogue tous
domaines, modélisateur, propriétés, contrôle, publication, nouvelle version, retrait, suppression d'un brouillon), Lancer une
certification, Mes actions de certification (À faire, À suivre, Liaisons suivies), dossier de liaison (Périmètre, Étapes,
Opérations, Décision, Gouvernance), tiroir d'opération, Tâches (Mes tâches, À prendre), centre de notifications, Mon équipe,
portail (Ce que le GIMAC attend de vous, procès-verbal).

Flux de bout en bout réellement joués :

1. Ingénieur : dérivation d'un brouillon de « Agrégateur de paiement v1 », ajout d'une étape (Recette interne du switch),
   création de l'opération A9 (R pôle réseau, V conducteur, C pôle système, I chef du DES, E participant, deux livrables par
   code, deux signatures ordonnées portant L1, geste « Déposer un livrable », vue « Coordonnées techniques », prédécesseur A2),
   contrôle, publication (v2, la v1 est retirée), puis brouillon sans l'opération A4 (déclencheur pv-established) publié (v3).
2. Ingénieur : type de projet vierge (« Émetteur de monnaie électronique », onze étapes, aucune opération), type copié
   (« Agrégateur de paiement étendu », copie de la v3 avec étapes, A9 et signatures), désactivation motivée ; nouvelle fonction
   « Cellule des clés » (profil ROLE_POLE_SYSTEME, permission HOM_FUNCTION_SYSTEM), désactivation motivée.
3. Ingénieur (REF_METIER_ADMIN) : processus « Revue annuelle des normes » créé dans une unité qui n'est pas la sienne, étape
   confiée par profil (information de couverture), champ de formulaire, contrôle, publication, nouvelle version, suppression du
   brouillon, retrait de la version publiée.
4. Parcours de certification complet sur LIA-2026-066 (Établissement de paiement A, type Agrégateur) : lancement (chef du DES),
   preuve A1 (chargé DDRM), renvoi motivé puis validation (conducteur), essais A2 (SNA), validation, constat de couverture A3
   (SNA), procès-verbal établi (conducteur), signé GIMAC (chef du DES) puis participant (portail), dossier et fiche de
   changement (production), approbation (RSSI), bascule A8 (production), validation (conducteur) : liaison « En production »,
   quatre jalons atteints, institution INST-0042 « Actif ».
5. Fonction à affecter (LIA-2026-041, Pôle réseau) : réunion de lancement déposée, opération 9 « À prendre » pour le pôle
   réseau, puis affectation de la fonction par le conducteur dans la Gouvernance.
6. Signatures ordonnées à l'exécution (LIA-2026-036, opération 11) : le conducteur attend, SNA signe en premier, puis le
   conducteur ; l'opération se clôt.
7. RACI : dépôt par un acteur non désigné et validation par un acteur qui n'est pas le validateur.

Non joués : relier deux cartes en tirant un arc sur le canevas (couvert par le scénario automatisé
`referentiels-modelisateur.spec.ts`), simulation de migration (réservée à HOM_REFERENTIEL_MIGRATE, aucun profil du lot).

## 2. Tableau des contrôles

| N° | Écran ou flux | Profil | Geste | Résultat attendu | Résultat observé | Capture | Verdict |
|---|---|---|---|---|---|---|---|
| 1 | Barre latérale | Ingénieur | Lecture | Accueil, Mon activité (sans Mon équipe), Institutions, Mes actions, Liaisons, Référentiels ; ni Lancer une certification ni Mes projets | Conforme | 001 | Conforme |
| 2 | Barre latérale | Conducteur | Lecture | Idem plus « Mes projets » | Conforme | 002 | Conforme |
| 3 | Barre latérale | SNA, pôle réseau, pôle système, production | Lecture | Idem ingénieur | Conforme | 003 à 006 | Conforme |
| 4 | Barre latérale | Chef de service | Lecture | Mon équipe et Référentiels ; ni Institutions, ni Mes actions, ni Liaisons | Conforme | 007 | Conforme |
| 5 | Adresses directes interdites | 7 profils | /des/certification/lancer, /des/administration/comptes, /des/administration/profils, /des/plateforme/supervision | « Accès refusé », aucune donnée | Conforme | journal 01-acces | Conforme |
| 6 | Adresses directes interdites | Chef de service | /des/certification/mes-actions, /des/parcours, /des/institutions | Accès refusé | Conforme | journal 01-acces | Conforme |
| 7 | Mon équipe par adresse | 6 profils non responsables | /des/equipe | Accès refusé | Conforme | journal 01-acces | Conforme |
| 8 | Tableau de bord, Mon travail | Conducteur | Lecture | Compteurs (projets, PV à établir, retards, échéances), tâches avec lien | Conforme | 002, 102 | Conforme |
| 9 | Référentiels, arbre | Ingénieur | Ouvrir | Versions par type, libellé du type | « AGREGATEUR » affiché par son code | 008 | Anomalie (HOM-09) |
| 10 | Version publiée | Ingénieur | Ouvrir Agrégateur v1 | Lecture seule, Dériver, Contrôler | Conforme ; la version publiée porte 3 anomalies bloquantes | 009, 011 | Anomalie (HOM-01) |
| 11 | Canevas | Ingénieur | Vue d'ensemble | Cartes lisibles | Cartes de quelques pixels, intitulés de couloirs qui se chevauchent | 010 | Anomalie (HOM-18) |
| 12 | Dériver un brouillon | Ingénieur | Confirmer | Brouillon modifiable, pilote | Conforme | 012, 013 | Conforme |
| 13 | Étapes (modale) | Ingénieur | Lire, ajouter « Recette interne du switch », monter | Étapes ordonnées, protégées sans retrait, étape vide retirable | Conforme, annonce « Étape … ajoutée » | 014 à 016 | Conforme |
| 14 | Modale d'opération, Général | Ingénieur | Code, libellé, étape ajoutée, segment | Champs et étape de la version | Conforme | 019 | Conforme |
| 15 | Modale d'opération | Ingénieur | Saisir puis Échap | Confirmation d'abandon | Fermeture immédiate, saisie perdue | 017, 018 | Anomalie (HOM-08) |
| 16 | Rôles RACI | Ingénieur | R, V, C, I, E, tâche aux consultés | Sélecteurs alimentés par le catalogue | Conforme | 020 | Conforme |
| 17 | Livrables par code | Ingénieur | Ajouter L1 et L2 (nature, déposant) | Codes L1, L2 attribués | Conforme | 021 | Conforme |
| 18 | Signatures ordonnées | Ingénieur | Deux signatures, ordres 1 et 2, livrables portés | L1 et L2 proposés | Liste vide « Aucune institution ne correspond » ; création refusée « Une signature porte au moins un livrable » | 022, 023, 026 | Anomalie (HOM-06, HOM-07) |
| 19 | Signatures ordonnées (contournement) | Ingénieur | Retaper le code, choisir L1 | L1 portée, opération créée | Conforme après contournement | 027 | Conforme |
| 20 | Attribut vue, geste, jalon | Ingénieur | Onglet Exécution | Listes servies | Conforme | 024 | Conforme |
| 21 | Chronologie | Ingénieur | Prédécesseur A2 | Arc créé | Conforme | 025 | Conforme |
| 22 | Contrôle | Ingénieur | Contrôler | Anomalies listées, cliquables | Conforme (3 « Validateur absent » et « Attributs manquants ») | 028 | Conforme |
| 23 | Publication avec anomalies | Ingénieur | Publier la version | Refus HOM_REFERENTIAL_INCOMPLETE ou GRAPH_INVALID | Publiée (v2), v1 retirée | 029 à 031 | Anomalie (HOM-01) |
| 24 | Retrait de la version précédente | Ingénieur | Publication | Ancienne version « retirée » | Conforme | 033 | Conforme |
| 25 | Refus GESTE_CERTIFICATION_ABSENT | Ingénieur | Supprimer A4 (pv-established) puis contrôler et publier | Anomalie GESTE_CERTIFICATION_ABSENT, publication refusée | Aucune anomalie de ce type, publication acceptée (v3) | 032, 033 | Anomalie (HOM-02) |
| 26 | Types de projet, liste | Ingénieur | Lecture | Modèle, état, référentiel, proposable, liaisons | Conforme | 034 | Conforme |
| 27 | Nouveau type, contrôles | Ingénieur | Créer à vide | Code et libellé exigés | Conforme | 035 | Conforme |
| 28 | Nouveau type vierge | Ingénieur | Créer MONNAIE_ELECTRONIQUE | Brouillon v1, onze étapes, aucune opération | Conforme ; type affiché par son code et non déplié dans l'arbre | 036, 037 | Conforme (HOM-09) |
| 29 | Nouveau type copié | Ingénieur | Copier Agrégateur v3 | Étapes, opérations, rôles, livrables, signatures copiés | Conforme (étape ajoutée, A9 et ses deux signatures repris) | 038 à 040 | Conforme |
| 30 | Désactiver un type | Ingénieur | Modifier, Actif décoché, motif | Motif exigé, « Désactivé », non proposé | Conforme | 044, 045 | Conforme |
| 31 | Catalogue des fonctions | Ingénieur | Lecture | Profils, permissions (libellés), nature, état, origine | Conforme ; étiquettes de permissions débordantes | 041 | Anomalie (HOM-12) |
| 32 | Nouvelle fonction | Ingénieur | Profil invalide puis valide, permission HOM_FUNCTION_SYSTEM | Erreur de forme puis création | Conforme | 042, 043 | Conforme |
| 33 | Désactiver une fonction | Ingénieur | Sans motif puis avec motif | Motif exigé, « Désactivé » | Conforme | journal fn2 | Conforme |
| 34 | Vue filtrée | SNA | CI v3 | Bandeau « opérations qui vous concernent », voisines en fantômes, lecture seule | Conforme (5 opérations, 10 lignes) | 060, 061 | Conforme |
| 35 | Vue filtrée | Pôle réseau, pôle système | CI v3 | Idem | Conforme (3 opérations chacun) ; « Hors périmètre » débordant | 063 à 066 | Conforme (HOM-12) |
| 36 | Vue filtrée | Production, conducteur | CI v3 | Idem | Conforme (16 et 43 opérations ; le conducteur est V de toutes) | 067 à 070 | Conforme |
| 37 | Vue complète, éditeur sans publication | Chef de service | CI v3 | Créer un brouillon, Dériver, Contrôle ; pas de Publier | Conforme | 071, 072 | Conforme |
| 38 | Types et fonctions en lecture | SNA, pôles, production, conducteur | Ouvrir | Bandeau « Consultation seulement », aucun bouton d'écriture | Conforme | 062 | Conforme |
| 39 | Types et fonctions en écriture | Chef de service | Ouvrir | Nouveau type, Modifier, Nouvelle fonction | Conforme (HOM_REFERENTIEL_EDIT) | 073 | Conforme |
| 40 | Processus, tous domaines | Ingénieur | Ouvrir | Bandeau « Administration de tous les domaines », toutes les unités | Conforme | 046 | Conforme |
| 41 | Processus, création dans une autre unité | Ingénieur | Service Normes et Agréments | Brouillon créé | Conforme | 047, 048 | Conforme |
| 42 | Information de couverture | Ingénieur | Étape par profil Pôle système | « Aucun porteur dans l'unité ; 1 ailleurs » | Conforme ; codes techniques dans l'avertissement du contrôle | 049, 051 | Anomalie (HOM-15) |
| 43 | Champs du formulaire d'étape | Ingénieur | Ajouter un champ | Code, libellé, type, obligatoire | Conforme | 050 | Conforme |
| 44 | Contrôle et publication du processus | Ingénieur | Contrôler, Publier | Publiable, version publiée, auteur | Conforme | 051 à 053 | Conforme |
| 45 | Nouvelle version, suppression du brouillon | Ingénieur | Nouvelle version, Supprimer (case de confirmation) | v2 créée, supprimée, retour au catalogue | Conforme | 054, 055, 059 | Conforme |
| 46 | Retrait | Ingénieur | Retirer avec motif | « Retirée », motif consigné | Conforme | 056 à 058 | Conforme |
| 47 | Processus, vue d'unité | Chef de service | Ouvrir | Sans bandeau « tous domaines » | Conforme | 123 | Conforme |
| 48 | Lancer une certification | Chef du DES (appui) | Institution, type configuré, conducteur | Liaison ouverte au cadrage | Conforme (LIA-2026-066) ; type affiché par son code | 074, 075 | Conforme (HOM-09) |
| 49 | Mes actions, trois onglets | Conducteur | À faire, À suivre, Liaisons suivies | Lignes, rôle, actions, jalons | Conforme | 076 à 078 | Conforme |
| 50 | Dossier, onglet Étapes | Conducteur | Lecture | Étapes de la version, états, échéances | Échéance du 4 oct. dépassée affichée « Dans les délais » | 079 | Anomalie (HOM-10) |
| 51 | Dossier, onglet Gouvernance | Conducteur | Lecture | Intervenants, qualités, Changer | Conforme ; noms génériques | 080 | Anomalie (HOM-14) |
| 52 | Dossier, onglet Opérations | Conducteur | Lecture | Par étape, états, compteurs | Conforme ; « En retard 0 » faux | 081 | Anomalie (HOM-10) |
| 53 | Dossier, onglet Décision | Conducteur | Lecture | Verrous, circuit, texte sans rang | « (rang 17) » sur un type configuré | 082 | Anomalie (HOM-11) |
| 54 | Gouvernance, fonction à affecter | Conducteur | Affecter Pôle réseau (LIA-2026-041) | Personne choisie, annonce | Conforme | 093 à 095 | Conforme |
| 55 | Gouvernance | Pôle réseau | Lecture | Aucun bouton Affecter | Conforme | journal p15 | Conforme |
| 56 | Tâche à prendre par fonction | Pôle réseau | Tâches > À prendre | Tâche HOM_FUNCTION_NETWORK « Prendre » | Absente ; l'opération apparaît « À prendre » seulement dans Mes actions | 083 à 085, 090, 091 | Anomalie (HOM-04) |
| 57 | Prise qui affecte la personne | Pôle réseau | Ouvrir l'opération 9 et agir | Bouton Prendre, fonction affectée | Pas de Prendre ; dépôt direct ; fonction restée « À affecter » | 092, 086 | Anomalie (HOM-04) |
| 58 | Dépôt d'un livrable par code | Chargé DDRM (appui) | Opération 7, sans fichier puis avec | Erreur puis dépôt ; validation et signature VISA_CONDUITE attendues | Erreur conforme ; opération close dès le dépôt, sans validation ni signature | 087, 088 | Anomalie (HOM-05) |
| 59 | RACI imposé, dépôt | Pôle système | Déposer la preuve de l'opération 4 de LIA-2026-044 (R : DDRM) | Bouton absent, refus | Bouton offert, dépôt accepté | 096, 098 | Anomalie (HOM-03) |
| 60 | RACI imposé, validation | SNA | Valider l'opération 4 (V : conducteur) | Bouton absent, refus | Bouton offert, validation signée acceptée | 099 à 101 | Anomalie (HOM-03) |
| 61 | Notification vers l'opération | Conducteur | Cliquer « Action attendue » | Tiroir de l'opération | Conforme (LIA-2026-036, opération 11) | 103, 104 | Conforme |
| 62 | Notification de validation attendue | Conducteur | Après dépôt de A1 | Notification « validation attendue » | Aucune | 103 | Anomalie (HOM-16) |
| 63 | Lien d'une tâche de Mon travail | Conducteur | Cliquer A1 | Tiroir A1 | Conforme | journal p21 | Conforme |
| 64 | Renvoyer avec motif | Conducteur | Sans motif puis motif | Bouton désactivé puis renvoi, motif affiché | Conforme | 105, 106 | Conforme |
| 65 | Valider (clé de sécurité) | Conducteur | A1, A2 | Opération close, successeur ouvert | Conforme | 107 | Conforme |
| 66 | Essais et couverture | SNA | Déposer A2, constater A3 | À valider puis close | Conforme | 108, 109 | Conforme |
| 67 | Procès-verbal | Conducteur | Établir | « Établi, à signer » ; pas de signature GIMAC pour lui | Conforme | 110 | Conforme |
| 68 | Signature GIMAC | Chef du DES (appui) | Carte visée, signer | « Signé par le GIMAC » | Conforme | 111 | Conforme |
| 69 | Signature participant | Correspondant | Portail, Signer | Procès-verbal signé | Conforme | 112, 113 | Conforme |
| 70 | Dossier et fiche de changement | Production | Constituer, référencer CHG-2026-420 | Gestes acceptés | Conforme | 114, 115 | Conforme |
| 71 | Approbation sécurité | RSSI (appui) | Relire la fiche | « Approuvée par la sécurité » | Conforme | journal p26 | Conforme |
| 72 | Bascule et mise en production | Production, conducteur | Déposer A8, valider | Liaison « En production », 8 sur 8, jalons, institution active | Conforme ; dates des jalons « 29 sept. » | 116, 117 | Conforme (HOM-10) |
| 73 | Signatures ordonnées à l'exécution | Conducteur | Ouvrir l'opération 11 avant SNA | Pas de Signer (ordre 2 « À venir ») | Conforme | 118 | Conforme |
| 74 | Signatures ordonnées | SNA puis conducteur | Signer dans l'ordre | Signée 1, puis 2, opération close | Conforme | 119 à 121 | Conforme |
| 75 | Vue d'unité | Chef de service | Mon équipe, Tâches À prendre de l'unité, tableau de bord | Collaborateurs du service, tâches de l'unité | Conforme | 122 | Conforme |
| 76 | Thème sombre | Ingénieur, conducteur, pôle réseau | 5 écrans | Contraste, lisibilité | Conforme | 124 à 128 | Conforme |
| 77 | Mobile 390 px | Ingénieur, conducteur, pôle réseau | 5 écrans | Pas de défilement horizontal, actions accessibles | Pas de défilement de page ; pied de la modale d'opération qui se chevauche | 129 à 133 | Anomalie (HOM-13) |
| 78 | en-US | 3 profils | 5 écrans | Aucune clé brute | Conforme (seules les données servies restent en français) | 134 à 138 | Conforme |
| 79 | Composition des profils simulés | tous | Comparer à generer-realm-gimac.py | Identique | Ingénieur sans HOM_REPLAY_ADMIN côté simulé | sans | Anomalie (HOM-19) |

Bilan : 79 contrôles, 58 conformes, 21 relevant une anomalie (19 anomalies distinctes : 0 bloquante, 6 majeures, 13 mineures).

## 3. Matrice des accès du lot

V : visible dans la barre latérale et ouvert ; R : refusé proprement (« Accès refusé », aucune donnée) ; O : ouvert par
l'adresse sans entrée propre. Toutes les cases sont conformes au catalogue des fonctionnalités et aux permissions des profils.

| Entrée ou route | Ingénieur | Conducteur | SNA | Pôle réseau | Pôle système | Production | Chef de service |
|---|---|---|---|---|---|---|---|
| Référentiels d'opérations (/des/referentiels/operations) | V | V | V | V | V | V | V |
| Types de projet et fonctions (/des/referentiels/types-projet) | O (écriture) | O (lecture) | O (lecture) | O (lecture) | O (lecture) | O (lecture) | O (écriture) |
| Mes actions de certification | V | V | V | V | V | V | R |
| Liaisons et parcours (/des/parcours) | V | V | V | V | V | V | R |
| Mes projets (/des/parcours?mesProjets=1) | O (Liaisons) | V | O (Liaisons) | O (Liaisons) | O (Liaisons) | O (Liaisons) | R |
| Lancer une certification | R | R | R | R | R | R | R |
| Institutions membres | V | V | V | V | V | V | R |
| Processus de l'unité | V (tous domaines) | V | V | V | V | V | V (son unité) |
| Tâches | V | V | V | V | V | V | V |
| Mon équipe | R | R | R | R | R | R | V |
| Administration (comptes, profils) | R | R | R | R | R | R | R |
| Plateforme (supervision) | R | R | R | R | R | R | R |

Droits dans les écrans permis : Créer un brouillon, Dériver, onglet Contrôle (ingénieur, chef de service) ; Publier (ingénieur
seulement) ; Nouveau type, Nouvelle fonction, Modifier (ingénieur, chef de service) ; Affecter une fonction (conducteur) ;
Signer le procès-verbal pour le GIMAC absent chez le conducteur. Écart : les boutons Déposer et Valider du tiroir sont offerts à
des acteurs non désignés (HOM-03).

## 4. Anomalies

### HOM-01 (majeure) Publication d'un graphe invalide acceptée, version de démonstration publiée avec anomalies
Étapes : ingénieur, Référentiels, Agrégateur v1, Dériver un brouillon, Contrôler (3 « Validateur absent » sur A3, A5, A6 et
« Attributs manquants »), Publier la version, confirmer. Attendu : refus 409 `HOM_REFERENTIAL_INCOMPLETE` ou
`HOM_REFERENTIAL_GRAPH_INVALID` comme `ReferentialLifecycleService.publier` ; la confirmation ne signale pas non plus les
anomalies. Observé : version 2 publiée, v1 retirée. La version de démonstration « Agrégateur de paiement v1 » est elle-même
publiée avec ces trois anomalies : un brouillon qui en dérive ne serait jamais publiable sur le vrai service sans donner un
validateur à des opérations à déclenchement externe.
Captures : 011, 028 à 031.
Cause : `frontend/packages/lot1/src/services/simule/referentiels.ts`, `publierReferentiel` ne refuse que les cycles ;
`versionAgregateur()` (même fichier) laisse `validateur: null` sur A3, A5 et A6.

### HOM-02 (majeure) Refus GESTE_CERTIFICATION_ABSENT absent
Étapes : ingénieur, brouillon dérivé de la v2, supprimer A4 « Établir le procès-verbal » (déclencheur
`certification.pv-established.v1`) alors que A5 porte le jalon CERTIFIEE, Contrôler, Publier. Attendu : anomalie
`GESTE_CERTIFICATION_ABSENT` (codes CERTIFIEE puis le déclencheur manquant) et publication refusée. Observé : seules les
anomalies de validateur, publication acceptée (v3).
Captures : 032, 033.
Cause : `controlerGraphe` (services/simule/referentiels.ts) n'a pas ce contrôle ; le type `TypeAnomalie`
(`modele/homologation.ts`) ne connaît pas `GESTE_CERTIFICATION_ABSENT` et `i18n/ecrans/modelisateur.ts` n'a pas de libellé
`modelisateur.anomalie.GESTE_CERTIFICATION_ABSENT` : servie par la passerelle, l'anomalie s'afficherait par son code brut.

### HOM-03 (majeure) RACI non imposé dans le tiroir d'opération
Étapes : pôle système, /des/parcours/LIA-2026-044?onglet=operations&operation=LIA-2026-044:4 (R : chargé DDRM), joindre un
PDF, Déposer la preuve : accepté (« À valider »). SNA, même opération, Valider l'opération, clé de sécurité : accepté
(opération close), alors que le validateur est le conducteur. Même constat sur A1 de LIA-2026-066 (boutons offerts à des non
désignés). Attendu (D-93 §2) : actions calculées pour la personne, boutons absents, refus du service.
Captures : 096, 098, 099 à 101.
Cause : `services/simule/parcours.ts` (`deposerPreuve`, `deposerLivrable`, `valider`) ne contrôle pas l'acteur ;
`actionsPermises` de l'opération ne dépend pas de la personne.

### HOM-04 (majeure) Tâche à prendre par fonction absente, prise sans effet
Étapes : LIA-2026-041 (Pôle réseau « à affecter »), réunion de lancement close, pôle réseau : Mes actions montre l'opération 9
« À prendre » ; Tâches > À prendre ne montre aucune tâche `HOM_FUNCTION_NETWORK` ; le tiroir n'a pas de bouton « Prendre »
mais offre le dépôt ; après le dépôt la fonction reste « À affecter » (Gouvernance) et l'opération close a pour responsable
« À affecter (Pôle réseau) ». Attendu (effet-reel-referentiels.md §8.8) : tâche à prendre de MS-PILOTAGE, « Prendre » qui affecte
la fonction à la personne.
Captures : 083 à 086, 090 à 092.
Cause : le parcours simulé n'offre pas de tâche au pilotage simulé (`pont.offrir` n'est appelé que pour les transitions de
statut, `services/simule/index.ts`) et aucune prise n'est câblée.

### HOM-05 (majeure) Opération à livrable close dès le dépôt, sans validation ni signature configurée
Étapes : chargé DDRM, LIA-2026-041, opération 7 « Réunion de lancement » (V : conducteur ; signature VISA_CONDUITE du
conducteur au référentiel), Déposer le livrable. Observé : « Faite » immédiatement ; même chose pour l'opération 9. Attendu :
validation par le V et signature ordonnée avant la clôture (types-projet.md §3).
Captures : 087, 088, 092.
Cause : `services/simule/parcours.ts`, `deposerLivrable` appelle `terminer` sans passer par `EN_VALIDATION` ; les signatures
du référentiel ne sont pas reprises sur les opérations des liaisons (seul le scénario `enSignature` de LIA-2026-036 en porte).

### HOM-06 (majeure) « Livrables portés » ne propose pas les livrables saisis dans la modale
Étapes : Ajouter une opération, onglet Livrables (L1, L2), onglet Signatures, Ajouter une signature, champ « Livrables
portés » : liste vide ; Créer l'opération est refusé (« Une signature porte au moins un livrable »). Contournement : retaper
le code de l'opération, la liste se recalcule.
Captures : 022, 026, 027.
Cause : `pages/des/modelisateur/ModaleOperation.tsx`, `useMemo` des options : dépendances `[c, p.noeuds, o.code, t]` sans
`o.livrables`.

### HOM-07 (mineure) Texte de liste vide hors sujet
« Aucune institution ne correspond. La création n'est pas possible ici. » dans « Livrables portés ». Capture 022.
Cause : libellé par défaut `searchSelect.aucun` de `frontend/packages/design-system/src/i18n/libelles.ts`, non surchargé par
`ChoixMultiple` (ModaleOperation.tsx).

### HOM-08 (mineure) Modale d'opération fermée sans confirmation d'abandon
Saisir code et libellé puis Échap (ou Annuler) : la modale se ferme, la saisie est perdue, sans la confirmation que donne
`CrudDialog` (`dirty`). Captures 017, 018. Cause : `ModaleOperation.tsx`, `onClose={fermer}` direct sur `Dialog`.

### HOM-09 (mineure) Type de projet affiché par son code
Arbre des versions (« AGREGATEUR », « MONNAIE_ELECTRONIQUE », nœud du type créé replié) et Identification du dossier de liaison
(« Type de projet AGREGATEUR »). Captures 008, 037, 075. Cause : `adaptateurs/referentiel.ts` `versArbre` traduit
`typeProjet.<code>` au lieu du libellé servi des types (`useOptionsTypesProjet`).

### HOM-10 (mineure) Dates du parcours simulé figées au 29/09/2026
Échéances dépassées (4 oct., 30 sept.) affichées « Dans les délais » ou « Échéance proche », « En retard 0 », clôtures et
jalons « le 29 sept. 2026 », démarrage au 29 sept. pour une liaison ouverte le 8 oct. Captures 079, 081, 087, 117.
Cause : `services/simule/parcours.ts:24`, `const AUJOURDHUI = new Date('2026-09-29T09:00:00Z')`.

### HOM-11 (mineure) Rang codé en dur dans l'onglet Décision
« Le procès-verbal s'établit après le constat de la couverture du périmètre par Normes et Agréments (rang 17). » sur une liaison
du type Agrégateur (opération A3). Contraire à D-93 §5. Capture 082.

### HOM-12 (mineure) Étiquettes débordantes
Les permissions longues du catalogue des fonctions (« Tenir la fonction Production monétique dans un projet d'homologation »)
et l'étiquette « Hors périmètre » des fantômes passent sur deux lignes et débordent de leur cadre sur la ligne suivante.
Captures 041, 062, 063.

### HOM-13 (mineure) Modale d'opération à 390 px
Les boutons du pied (« Supprimer l'opération », « Annuler », « Enregistrer ») se chevauchent et sont coupés. Capture 129.

### HOM-14 (mineure) Identités simulées incohérentes
Pilotes « Ingénieur d'homologation A/B », conducteurs « Conducteur de projet 1/2 », intervenants « Chargé DDRM 1 »,
« Ingénieur pôle réseau » alors que les comptes de démonstration sont Joël Ekotto Mballa, Aïcha Mahamat Nour, Fatimé Abakar
Hissein, Idriss Mahamat Saleh : l'acteur ne se reconnaît pas dans la Gouvernance. Captures 013, 074, 080, 095.

### HOM-15 (mineure) Avertissement de couverture en codes techniques
Contrôle d'un processus : « Aucune personne active ne porte ROLE_POLE_SYSTEME dans l'unité SNA ; 1 porteur ailleurs dans le
GIMAC (POLE_SYSTEME : 1) ». Attendu : libellés (Pôle système, Service Normes et Agréments). Capture 051.

### HOM-16 (mineure) Pas de notification de validation attendue
Après le dépôt de la preuve A1, le conducteur (validateur) ne reçoit aucune notification ; le centre ne contient que les
notifications de démonstration (effet-reel-referentiels.md §11.1). Capture 103.

### HOM-17 (mineure) Fonction « Chefs des deux départements » reliée au seul chef du DES
CHEFS_DEPARTEMENT n'a que ROLE_CHEF_DES pour profil (services simulés et
`ms-homologation/.../20261010000100_types_projet_fonctions_signatures.xml`) ; le libellé annonce les deux départements.
Capture 041.

### HOM-18 (mineure) Canevas illisible à l'ouverture
Vue d'ensemble ajustée sur tous les couloirs : cartes de quelques pixels, intitulés de couloirs qui se chevauchent
(« CERTIFICATION » et « ATELIERS »). Captures 010, 028.

### HOM-19 (mineure) Profil simulé de l'ingénieur d'homologation incomplet
`frontend/packages/lot1/src/permissions.ts` (PROFILS.ROLE_INGENIEUR_HOMOLOGATION) n'a pas `HOM_REPLAY_ADMIN`, présent dans
`infra/keycloak/generer-realm-gimac.py`. Sans écran dans le lot, sans effet visible.

## 5. Correction

Passage de correction du 09/10/2026 (lot HOM). Registre : `docs/recette/corrections/homologation.json` ; captures après :
`docs/recette/captures/homologation/corrections/` (constructions simulées sur le port isolé 6700, profils et gestes de la recette).

| Anomalie | Statut | Correctif |
|---|---|---|
| HOM-01 | corrigée | Le simulateur refuse la publication comme `ReferentialLifecycleService.publier` : `HOM_REFERENTIAL_INCOMPLETE` (attributs du service, dérivés de l'étape), puis `HOM_REFERENTIAL_GRAPH_INVALID` pour toute anomalie (fonctions inconnues, signatures, gestes de certification compris). Le contrôle à blanc sert `anomalies` et `conforme` ; la confirmation de publication annonce le refus. L'Agrégateur v1 donne un validateur à A3, A5, A6 ; le rang 7 de l'annexe déclare le livrable L1 que porte son visa : toutes les versions de démonstration passent le contrôle. |
| HOM-02 | corrigée | Contrôle `GESTE_CERTIFICATION_ABSENT` du simulateur aligné sur `gimac.homologation.referentiel.gestes-certification` (attestations selon le modèle du type), type TS et libellés fr, en, es (avec ceux des anomalies de signatures et de fonctions qui manquaient). Motif en libellés côté MS-HOMOLOGATION et simulateur. |
| HOM-03 | corrigée | Validation et renvoi réservés au validateur désigné (`Parcours.exigerValidateur` : 403 `HOM_VALIDATION_NON_VALIDATEUR`, 409 `HOM_VALIDATOR_IS_RESPONSIBLE`), boutons retirés pour les autres. Dépôt : déjà imposé par DDR-05. Le service appliquait la règle. |
| HOM-04 | corrigée | Fonction interne à affecter (R engagé, V en validation) offerte en tâche à prendre de MS-PILOTAGE simulé (« Prendre la fonction … », permissions de la fonction) ; la prise affecte la personne sur la liaison et termine la tâche, comme `PrisesFonctions`. |
| HOM-05 | corrigée | Signatures configurées au référentiel recopiées sur les opérations des liaisons : le compte rendu de lancement passe En signature et ne se clôt qu'après le visa du conducteur. Une preuve passe par la validation du V ; un livrable se clôt au dernier dépôt obligatoire, règle du service (`effet-reel-referentiels.md` §8.4) gardée. |
| HOM-06, HOM-07, HOM-08 | corrigées | Livrables saisis aussitôt proposés ; texte de liste vide propre au champ (MultiSelect) ; confirmation « Abandonner la saisie ? » avant toute fermeture d'une saisie modifiée. |
| HOM-09 | corrigée | Type de projet nommé par son libellé servi : arbre des versions (type ajouté déplié), identification du dossier, listes de liaisons. |
| HOM-10 | corrigée (lot DDRM) | Horloge de la démonstration au jour courant (DDR-15), rien refait. |
| HOM-11 | corrigée | Aucun rang dans les libellés de la décision et de la mise en service (fr, en, es). |
| HOM-12, HOM-13 | corrigées | Étiquettes qui s'agrandissent, permissions du catalogue passées à la ligne ; pied des modales à la ligne à 390 px. |
| HOM-14 | corrigée | Intervenants, conducteurs et pilotes simulés nommés comme les comptes de démonstration ; tests et scénarios mis à jour. |
| HOM-15 | corrigée | Message de couverture du contrôle d'un processus rendu avec les libellés des profils et des unités (service et simulateur). |
| HOM-16 | corrigée | « Validation attendue » notifiée au validateur au dépôt d'une preuve (MS-NOTIFICATION le faisait déjà). |
| HOM-17 | corrigée | `CHEFS_DEPARTEMENT` reliée aux deux profils de chef : simulateur, `CatalogueFonctions`, changeset `20261014000200_chefs_departement.xml`. |
| HOM-18 | corrigée | Cadrage initial jamais sous un zoom lisible (ouvert sur le début du parcours), intitulés de couloirs coupés dans leur couloir, cartes de l'Agrégateur dans les couloirs de leur étape. |
| HOM-19 | corrigée (lot administration) | Profils simulés lus dans `domaine-gimac.json` ; `HOM_REPLAY_ADMIN` présent. |

Tests ajoutés : `frontend/packages/lot1/src/services/simule/homologation-recette.test.ts` (HOM-01 à HOM-05, HOM-16, HOM-17),
`ms-homologation/.../domain/service/RecetteHomologationTest.java` (HOM-02, HOM-17). Tests mis à jour pour le visa du rang 7, les
noms de démonstration et les libellés sans rang ; scénario `parcours-conducteur.spec.ts` : signature du visa avant la clôture.
