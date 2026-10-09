# Recette du lot DDR : DDRM et ouverture

Validateur : recette exhaustive GIMAC « Moabi », passage du 08/10/2026 (heure de Douala).
Environnement : applications construites en services simulés, application interne http://localhost:6100, portail
http://localhost:6101, console http://localhost:6102 (ports 6000 à 6002 refusés par Chromium, ERR_UNSAFE_PORT).
Navigateur : Chromium sans tête (Playwright), contexte `fr-FR`, fuseau Africa/Douala, 1440 x 900 sauf mention, piloté pas à
pas (un geste, une capture, une lecture de la capture). Démonstration continue (`gm.demoContinue = 1`) pour enchaîner les
acteurs sur un même état ; changement d'acteur par la session de démonstration (`gm.sessionDemo`, `gm.profilDemo`).
Captures : `docs/recette/captures/ddrm/` (123 fichiers, numérotés dans l'ordre du parcours).
Console du navigateur et réseau surveillés sur tout le parcours : aucune erreur JavaScript, aucune réponse HTTP en erreur.

## 1. Périmètre

Profils :

| Profil | Compte de démonstration | Application |
|---|---|---|
| Chargé de la DDRM (ROLE_CHARGE_DDRM) | f.abakar, Fatimé Abakar Hissein | interne |
| Chef de la DDRM (ROLE_CHEF_DDRM) | b.nguema, Bertrand Nguema Obiang | interne |
| Correspondant du participant (ROLE_CORRESPONDANT) | correspondant-a, Établissement de paiement A (INST-0042) | portail |
| Conducteur de projet (ROLE_CONDUCTEUR_PROJET) | acteur d'appui seulement (renvoi et validation des opérations du cadrage) | interne |

Écrans : connexion (mot de passe puis code), tableau de bord, Institutions membres (liste, Mes dossiers, filtres), fiche
d'institution (Détail, Dossier d'adhésion, Conditions d'adhésion, Prérequis, Propositions de statut, Liaisons, tiroirs),
Lancer une certification, dossier de liaison (Périmètre, Étapes, Opérations, Décision, tiroir d'opération, affectation des
ressources, désignation du conducteur), Mes actions de certification, Tâches (Mes tâches, À prendre, Confiées), centre et page
de notifications, Référentiels d'opérations, Mon équipe, portail (tableau de bord, Mes liaisons, suivi d'une liaison,
coordonnées d'essai, notifications).

Flux de bout en bout réellement joués :

1. Le chargé enregistre la prise de contact de « Banque Atlantique du Littoral » (tous les champs), avec les contrôles de saisie
   (obligatoires, BIC, code BEAC, date inexistante, date future, garde d'abandon).
2. Il instruit le dossier : dépôt des quatre pièces, validation, rejet motivé des statuts, redépôt, validation ; conditions
   d'adhésion ; prérequis réunis ; modification de la fiche.
3. Il propose l'adhésion (une proposition prématurée est refusée par le service, une seconde proposition concurrente aussi).
4. Le chef trouve la tâche « Décision à prononcer » (À prendre), la prend, suit la notification et le lien d'origine vers
   l'onglet Propositions de statut ; il refuse la proposition avec motif ; le chargé voit le motif et repropose ; le chef
   prononce l'adhésion avec sa clé de sécurité ; la tâche passe à Terminée.
5. Maker-checker : le chef propose lui-même l'adhésion de Banque de Libreville puis tente de la prononcer.
6. Le chef lance la certification (institution, adhésion constatée, type « Carte interbancaire », conducteur, banque
   partenaire) : liaison LIA-2026-066 ouverte au cadrage.
7. Cadrage (rangs 3 à 5) : preuve du rang 3 (déposée à tort par le chef, renvoyée avec motif par le conducteur, redéposée par la
   chargée responsable, validée), preuves des rangs 4 et 5 par la chargée, validations ; étape Cadrage close (six éléments).
   Lancement : affectation des ressources (rang 6) par le chef, compte rendu de la réunion de lancement (rang 7) par la
   chargée ; Ateliers : compte rendu de l'atelier de cadrage fonctionnel (rang 8).
8. Tâches : création et attribution par le chef (à une personne, à l'unité), vues de la chargée.
9. Portail : ce que voit le correspondant, ses notifications, ses actions (coordonnées d'essai), son cloisonnement.

Limite : aucun compte correspondant n'est rattaché à une institution créée pendant la recette ; le parcours d'adhésion n'a
donc pas de pendant au portail (le portail n'expose d'ailleurs ni l'adhésion ni son état, conformément au catalogue qui ne
lui donne que le tableau de bord et les liaisons).

## 2. Tableau des contrôles

| N° | Écran ou flux | Profil | Geste | Résultat attendu | Résultat observé | Capture | Verdict |
|---|---|---|---|---|---|---|---|
| 1 | Connexion interne | Chargé | Identifiant f.abakar, mot de passe, code à six chiffres | Session ouverte sur le tableau de bord | Conforme, nom et profil affichés | 001, 002, 003 | Conforme |
| 2 | Tableau de bord | Chargé | Lecture | Ma journée, Relations avec les membres, Mon travail, sans pilotage consolidé | Conforme | 003 | Conforme |
| 3 | Institutions membres | Chargé | Ouvrir la liste à 1440 px | Toutes les colonnes visibles | Colonne des actions coupée, défilement horizontal | 004 | Anomalie (DDR-12) |
| 4 | Prise de contact | Chargé | Ouvrir la modale | Neuf champs, aides sous les champs | Conforme | 005 | Conforme |
| 5 | Prise de contact | Chargé | Envoyer vide | Messages « Champ obligatoire » sous Dénomination, Catégorie, Pays et résumé | Messages présents ; résumé dans un ordre différent du formulaire | 006 | Conforme (remarque DDR-16) |
| 6 | Prise de contact | Chargé | BIC « BAL-123 », BEAC « CM-10005-XYZ », date 31/02/2026 | Trois messages sous les champs | Conforme | 007 | Conforme |
| 7 | Prise de contact | Chargé | BEAC de 11 caractères, date 15/12/2026 | Refus longueur et date future | Conforme | 008 | Conforme |
| 8 | Prise de contact | Chargé | BIC « bal cmcx xxx » (10 caractères) | Refus (8 ou 11 attendus) | Conforme | 011 | Conforme |
| 9 | Prise de contact | Chargé | Échap sur une saisie modifiée | Garde « Abandonner la saisie ? » | Conforme | 009 | Conforme |
| 10 | Prise de contact | Chargé | Bouton « Ouvrir le calendrier » | Calendrier affiché | Rien ne s'ouvre, aria-expanded reste false | 010 | Anomalie (DDR-11) |
| 11 | Prise de contact | Chargé | Saisie valide, BIC et BEAC en minuscules avec espaces | Codes normalisés, fiche ouverte | BALTCMCXXXX, 10005AB, fiche INS-0074 | 011, 012 | Conforme |
| 12 | Fiche | Chargé | Lecture du détail | Champs saisis, chargé de la DDRM | Champs exacts ; « Chargé DDRM » au lieu du nom de l'agent | 012 | Anomalie (DDR-16) |
| 13 | Proposer l'adhésion | Chargé | Proposer sans prérequis | Refus du service | Refus affiché, en texte neutre | 014, 015 | Conforme (remarque DDR-17) |
| 14 | Dossier d'adhésion | Chargé | Déposer une pièce sans fichier | Dépôt impossible | Bouton désactivé (« Choisissez un fichier ») | 016 | Conforme |
| 15 | Dossier d'adhésion | Chargé | Déposer les quatre pièces | État Reçue, fichier, date | Conforme ; types admis en MIME bruts | 016, 017 | Conforme (remarque DDR-14) |
| 16 | Dossier d'adhésion | Chargé | Valider trois pièces (confirmation) | État Validée | Conforme | 018, 023 | Conforme |
| 17 | Dossier d'adhésion | Chargé | Rejeter les statuts avec motif court puis motif valable | Motif exigé, état Rejetée | Conforme ; règle des 10 caractères seulement en infobulle | 019, 020, 021 | Conforme (remarque DDR-19) |
| 18 | Tiroir de pièce | Chargé | Onglets Chronologie, Pièces, Audit | Dépôt, rejet, motif consignés | Chronologie vide, Pièces et Audit « Aucun élément » | 022 | Anomalie (DDR-13) |
| 19 | Dossier d'adhésion | Chargé | Redéposer puis valider les statuts | Validée | Conforme | 023 | Conforme |
| 20 | Conditions d'adhésion | Chargé | Envoyer sans code participant ni mode | Refus « Champ obligatoire » | Accepté, mode « Adhésion directe » imposé | 024, 025 | Anomalie (DDR-03) |
| 21 | Conditions d'adhésion | Chargé | Code « bal@01 ! », adhésion indirecte sans marraine ni cantonnement | Refus (motif du code, RG-12) | Accepté, code « BAL@01 ! » enregistré | 026 | Anomalie (DDR-03) |
| 22 | Conditions d'adhésion | Chargé | Code bq-bal-074, directe, compte BEAC-CM-074 | Enregistré en majuscules | Conforme | 027 | Conforme |
| 23 | Conditions d'adhésion | Chargé | Rouvrir la modale | Valeurs déjà saisies reprises | Compte de règlement vide | 027 | Anomalie (DDR-18) |
| 24 | Prérequis | Chargé | Lecture | Cinq prérequis satisfaits | Conforme ; références « DIRECT », « ATTENDUE » brutes | 014, 028 | Conforme (remarque DDR-14) |
| 25 | Modifier la fiche | Chargé | BIC « 12345678 » | Refus sous le champ | Conforme | 029 | Conforme |
| 26 | Modifier la fiche | Chargé | Changer BIC, code BEAC, origine, sigle | Tous enregistrés | Seul le sigle est enregistré ; date de contact absente du formulaire | 030 | Anomalie (DDR-04) |
| 27 | Proposer l'adhésion | Chargé | Motif puis confirmation | Proposition En attente, sans bouton Approuver | Conforme | 031, 032 | Conforme |
| 28 | Proposer la clôture sans suite | Chargé | Seconde proposition | Refus « déjà en attente » | Conforme | 033 | Conforme |
| 29 | Tâches À prendre | Chargé | Ouvrir après sa proposition | Aucune décision offerte au proposant | Conforme | 084 | Conforme |
| 30 | Notification | Chef | Cloche après la proposition | « Prononcer l'adhésion : Banque Atlantique du Littoral » | Conforme (horodatage « il y a 5 min » inexact) | 038 | Conforme (remarque DDR-15) |
| 31 | Tâches À prendre | Chef | Lecture | Tâche « Décision à prononcer », offerte aux porteurs | Conforme ; colonne Unité = « Décision à prononcer » | 040 | Conforme (remarque DDR-16) |
| 32 | Tâche de décision | Chef | Ouvrir puis Prendre la tâche | Responsable = chef, historique « Prise » | Conforme | 041, 042 | Conforme |
| 33 | Tâche de décision | Chef | Lien « Ouvrir » de l'origine | Fiche, onglet Propositions de statut | Conforme | 043 | Conforme |
| 34 | Notification | Chef | Clic sur la notification | Fiche, onglet Propositions de statut | Conforme (?onglet=transitions) | 044 | Conforme |
| 35 | Refus | Chef | Rejeter avec motif | Rejetée, motif visible du chargé, chargé notifié | Rejetée et motif visible ; aucune notification au chargé | 050, 051, 052, 053 | Anomalie (DDR-08) |
| 36 | Prononcé | Chef | Approuver, clé de sécurité | Adhérent, tâche Terminée | Conforme | 054, 055, 056 | Conforme |
| 37 | Maker-checker | Chef | Proposer puis approuver sa propre proposition | Refus (séparation des tâches) | Approbation acceptée, Banque de Libreville Adhérent | 045, 046, 047 | Anomalie (DDR-01) |
| 38 | Démonstration continue | Chef | Créer une seconde institution | Identifiant nouveau | INS-0074 réattribué ; la fiche de Banque Atlantique devient inaccessible | 048, 049 | Anomalie (DDR-02) |
| 39 | Lancer une certification | Chef | Choisir l'institution adhérente | Adhésion constatée, étape 3 ouverte | Conforme ; Banque B et Banque D (actif, suspendu) exclues | 057 | Conforme |
| 40 | Lancer une certification | Chef | Ouvrir sans type ni conducteur | Deux messages | Conforme ; Type de projet sans astérisque | 058 | Conforme (remarque DDR-27) |
| 41 | Lancer une certification | Chef | Carte interbancaire, conducteur 1, Banque B, Ouvrir | Dossier LIA-2026-066 ouvert au cadrage | Conforme ; date de démarrage 29 sept. | 059, 060 | Conforme (remarque DDR-15) |
| 42 | Dossier de liaison | Chef | Onglets Étapes et Opérations | Étapes 1 et 2 closes, Cadrage en cours, 43 opérations | Conforme ; « Close le 9 sept. » ; référentiel « version 4 » | 061, 062 | Conforme (remarques DDR-15, DDR-24) |
| 43 | Tiroir opération 3 | Chef | Déposer la preuve (non responsable, sans HOM_EVIDENCE_SUBMIT) | Bouton absent ou refus | Dépôt accepté et attribué à « Chargé DDRM 1 » | 063, 064 | Anomalie (DDR-05) |
| 44 | Mes actions de certification | Chargé | Lecture | Ses opérations R et l'état | Conforme | 065 | Conforme |
| 45 | Renvoi | Conducteur | Renvoyer avec motif | Opération À faire, motif visible | Conforme | 066, 068 | Conforme |
| 46 | Mes actions de certification | Chargé | Après renvoi | Ligne LIA-2026-066 « Déposer la preuve », notification | Ligne présente ; aucune notification | 067 | Anomalie (DDR-08) |
| 47 | Tiroir opération 3 | Chargé | Lien direct, dépôt de preuve, confirmation | À valider, sans bouton Valider pour elle | Conforme ; empreinte identique au premier fichier | 068, 069 | Conforme (remarque DDR-26) |
| 48 | Validation | Conducteur | Valider l'opération 3 (clé) | Faite | Conforme | 071 | Conforme |
| 49 | Rangs 4 et 5 | Chargé | Mes actions, dépôts | Opérations ouvertes puis À valider | Conforme | 070 | Conforme |
| 50 | Étape Cadrage | Conducteur | Valider 4 et 5 | Cadrage Close, six éléments réunis | Conforme | 071 | Conforme |
| 51 | Mes actions de certification | Chef | Lecture | Ses seules actions | « Signer le procès-verbal » (LIA-2026-027) annoncé, aucun bouton sur l'écran | 072, 073 | Anomalie (DDR-10) |
| 52 | Affectation des ressources | Chef | Rang 6, lien du tiroir | Fenêtre d'affectation | Conforme | 074, 075 | Conforme |
| 53 | Affectation des ressources | Chef | Envoyer sans aucune désignation | Refus (responsables exigés) | Accepté, rang 6 clos | 076 | Anomalie (DDR-06) |
| 54 | Réunion de lancement | Chargé | Compte rendu et commentaire | Opération Faite | Conforme | 077, 078 | Conforme |
| 55 | Ateliers | Chargé | Compte rendu du rang 8 sans fichier puis avec | « Joignez le compte rendu. », puis livrable affiché | Conforme ; formats admis différents des preuves | 079, 080 | Conforme (remarque DDR-14) |
| 56 | Tâches | Chef | Nouvelle tâche confiée à la chargée, puis à l'unité | Listées dans Confiées | Conforme | 081, 082 | Conforme |
| 57 | Tâches | Chargé | Mes tâches, À prendre après changement d'acteur | Tâches du chef visibles | Absentes | 083, 084 | Anomalie (DDR-09) |
| 58 | Tâches | Chargé | Nouvelle tâche | Tâche personnelle seulement (pas de destination) | Conforme | 083 | Conforme |
| 59 | Liens de notification | Chargé | Quatre notifications | Écran exact de chaque objet | Tâche non ouverte, rapport reçu et atelier d'une autre fonction | 085, 086 | Anomalie (DDR-20) |
| 60 | Thème sombre | Chargé | Tableau de bord, fiche, Mes actions, dossier, erreurs | Lisible, contrastes | Conforme | 087 à 091 | Conforme |
| 61 | Mobile 390 px | Chargé | Tableau de bord, fiche, Mes actions | Pas de défilement horizontal de page | Conforme (scrollWidth 390) | 092 à 094 | Conforme |
| 62 | en-US | Chargé | Liste, fiche, Mes actions, Lancer | Pas de clé manquante | Conforme (données en français) | 095 à 097 | Conforme |
| 63 | Lancer une certification | Chargé | Institution adhérente | Pas d'ouverture (HOM_LIAISON_OPEN absente) | Message « transmettez la demande à un responsable » | 098 | Conforme |
| 64 | Lancer une certification | Chargé | Institution candidate | Prérequis et actions d'instruction | Conforme | 099 | Conforme |
| 65 | Fiche adhérente (Banque de Libreville) | Chargé | Boutons de l'en-tête | Pas « Ouvrir une liaison » | Seuls « Modifier la fiche » et « Proposer la clôture sans suite » (relevé du DOM) | 098 | Conforme |
| 66 | Fiche adhérente | Chef | Ouvrir une liaison (cadrage) | Fenêtre d'ouverture | Conforme ; MS-PARTICIPANT cité ; banques partenaires différentes | 122 | Conforme (remarques DDR-16, DDR-25) |
| 67 | Liaison sans conducteur | Chef | Désigner le conducteur | Carte vidée | Conforme | 119, 120, 121 | Conforme |
| 68 | Mon équipe | Chef | Ouvrir | Écran permis (PIL_TEAM_READ) | Conforme | 100 | Conforme |
| 69 | Référentiels | Chargé | Ouvrir | Lecture seule | Conforme ; type « AGREGATEUR » brut | 117 | Conforme (remarque DDR-14) |
| 70 | Filtre Statut | Chargé | Choisir « En homologation » | Liste filtrée | Liste inchangée, retour à « Tous » | 118 | Anomalie (DDR-07) |
| 71 | Mes dossiers, recherche | Chargé | Onglet et recherche « Atlantique » | Dossiers du chargé, une ligne | Conforme | 123 | Conforme |
| 72 | Accès refusés | Chargé | Adresses directes (équipe, comptes, journal, matrice, supervision, configuration) | « Accès refusé » sans données | Conforme | 034 | Conforme |
| 73 | Console | Chargé | Connexion f.abakar sur 6102 | Aiguillage vers l'application interne | Conforme | 035 | Conforme |
| 74 | Portail | Chargé | Connexion f.abakar sur 6101 | Aiguillage vers l'application interne | Conforme | 036 | Conforme |
| 75 | Accès refusés | Chef | Adresses directes (comptes, profils, politique, supervision, infrastructure, courriels) | « Accès refusé » | Conforme | 101 | Conforme |
| 76 | Console | Chef | /console/comptes | Aiguillage, aucune donnée | Conforme | 102 | Conforme |
| 77 | Portail | Correspondant | Connexion correspondant-a, tableau de bord | Ce que le GIMAC attend, ses liaisons | Conforme | 103 | Conforme |
| 78 | Portail | Correspondant | Notifications (cloche et page) | Aucun identifiant interne ni nom d'agent | Conforme | 104, 108 | Conforme |
| 79 | Portail | Correspondant | Lien « Action attendue : Paramétrage d'essai » | Écran de l'action attendue | « Ce livrable ne vous est pas demandé » | 105 | Anomalie (DDR-22) |
| 80 | Portail | Correspondant | Mes liaisons, suivi de LIA-2026-030 | Ses quatre liaisons, sans nom d'agent du GIMAC | Conforme | 106, 107 | Conforme |
| 81 | Portail | Correspondant | Liaison d'une autre institution (LIA-2026-066, LIA-2026-022) | Introuvable | Conforme | 110 | Conforme |
| 82 | Portail | Correspondant | Connexion sur l'application interne | Aiguillage vers le portail | Conforme | 109 | Conforme |
| 83 | Portail | Correspondant | Coordonnées d'essai invalides puis valides | Messages sous les champs, confirmation, valeurs figées | Conforme | 111, 112, 113 | Conforme |
| 84 | Portail sombre et mobile | Correspondant | Tableau de bord, suivi | Lisible, pas de défilement horizontal | Conforme sauf tableau vide en sombre | 114, 115, 116 | Anomalie (DDR-23) |

Bilan : 84 contrôles, 64 conformes (dont 14 avec une remarque reportée en anomalie mineure), 20 en anomalie.

## 3. Matrice des accès du lot

Barre latérale attendue d'après `ms-iam/src/main/resources/config/fonctionnalites-initiales.json` (permissions tous/unDe) et
les profils de `infra/keycloak/generer-realm-gimac.py`. V = visible, A = absente.

| Entrée (application interne) | Chargé attendu | Chargé observé | Chef attendu | Chef observé | Verdict |
|---|---|---|---|---|---|
| Tableau de bord | V | V | V | V | Conforme |
| Messagerie | V | V | V | V | Conforme |
| Notifications | V | V | V | V | Conforme |
| Ma journée, Agenda, Tâches, Rapports | V | V | V | V | Conforme |
| Mon équipe (PIL_TEAM_READ) | A | A | V | V | Conforme |
| Processus de l'unité, Lancer une demande, Mes demandes | V | V | V | V | Conforme |
| Institutions membres | V | V | V | V | Conforme |
| Lancer une certification | V | V | V | V | Conforme |
| Mes actions de certification | V | V | V | V | Conforme |
| Liaisons et parcours | V | V | V | V | Conforme |
| Mes projets (HOM_LIAISON_LIST et CER_PV_ESTABLISH) | A | A | A | A | Conforme |
| Référentiels d'opérations | V | V | V | V | Conforme |
| Administration (comptes, demandes, profils, matrice, organisation, organigramme, journal, politique) | A | A | A | A | Conforme |
| Plateforme (supervision, infrastructure, configuration, journalisation, courriels, console) | A | A | A | A | Conforme |
| Référentiels métier (REF_METIER_ADMIN) | A | A | A | A | Conforme |

| Route ou application | Chargé | Chef | Correspondant | Verdict |
|---|---|---|---|---|
| /des/equipe | refusée (« Accès refusé ») | permise | sans objet | Conforme |
| /des/administration/comptes, journal, profils, matrice-pouvoirs, politique-securite | refusées | refusées | sans objet | Conforme |
| /des/plateforme/supervision, configuration, infrastructure, courriels | refusées | refusées | sans objet | Conforme |
| /des/certification/lancer | permise, ouverture refusée par message | permise, ouverture possible | sans objet | Conforme |
| Approbation d'une proposition (bouton Approuver) | absent | présent, y compris sur sa propre proposition | sans objet | Anomalie (DDR-01) |
| Dépôt de preuve sur une opération dont il n'est pas responsable | non testé hors ateliers (livrables d'étape ouverts à HOM_STEP_MANAGE) | possible (rang 3) | sans objet | Anomalie (DDR-05) |
| Console http://localhost:6102 | aiguillé vers l'application interne | aiguillé | sans objet | Conforme |
| Portail http://localhost:6101 | aiguillé vers l'application interne | non testé | permis | Conforme |
| Application interne http://localhost:6100 | permise | permise | aiguillé vers le portail | Conforme |
| Portail : Tableau de bord, Mes liaisons | sans objet | sans objet | V | Conforme |
| Portail : liaison d'une autre institution | sans objet | sans objet | « Ce parcours est introuvable. » | Conforme |

## 4. Anomalies

### Bloquantes

**DDR-01 (bloquante) : maker-checker non appliqué, le chef prononce sa propre proposition.**
Étapes : chef de la DDRM, fiche de Banque de Libreville, « Proposer l'adhésion » avec motif ; onglet Propositions de statut ;
« Approuver » ; clé de sécurité. Résultat : « Approuver : effectué », Banque de Libreville passe Adhérent. Attendu : refus
PAR_SEPARATION_OF_DUTIES, et pas de bouton Approuver offert au proposant. Captures 045, 046, 047.
Cause probable : `frontend/packages/lot1/src/services/simule/participant.ts`, `proposerTransition` fige `demandeurId: 'sim-charge'`
et `approuverTransition` ne compare pas l'approbateur au demandeur (le service réel le fait :
`ms-participant/.../InstitutionLifecycleService.java`, contrôle « L'approbateur doit être distinct du demandeur ») ; l'écran
Propositions de statut n'utilise pas le demandeur pour masquer l'action.

**DDR-02 (bloquante pour la démonstration continue) : identifiants réattribués après un rechargement.**
Étapes : en démonstration continue, créer une institution (INS-0074), changer d'acteur ou recharger, créer une autre
institution. Résultat : la seconde reçoit aussi INS-0074 ; le lien « Banque Atlantique du Littoral » ouvre « Trésor public
centrafricain » ; la seconde proposition de Banque Atlantique porte aussi TRA-0074. Captures 048, 049.
Cause probable : `participant.ts` ligne 31, compteur `id()` de module remis à zéro à chaque chargement alors que l'état est
relu de `gm.demo.participant` (`services/simule/index.ts`, `persistanceDemo`). Contournement de recette : retrait de
l'entrée en double dans le stockage local pour poursuivre le parcours.

### Majeures

**DDR-03 (majeure) : conditions d'adhésion enregistrées sans aucun contrôle.**
Étapes : fiche d'un candidat, Conditions d'adhésion, « Enregistrer les conditions » sans rien saisir ; puis code « bal@01 ! »,
adhésion indirecte sans banque marraine ni compte de cantonnement. Résultat : fenêtre fermée, « Adhésion directe » imposée
la première fois, « BAL@01 ! » enregistré la seconde. Attendu : code obligatoire au motif `^[A-Z0-9-]+$` (20 caractères au
plus, `ConditionsRequest` de MS-PARTICIPANT), mode obligatoire, compte exigé selon RG-11 et RG-12. Captures 024, 025, 026.
Cause probable : `frontend/packages/lot1/src/pages/des/institutions/formulaires.tsx` (formulaire des conditions : aucun
contrôle, `mode ?? 'DIRECT'`) et `participant.ts` `definirConditions` (aucune règle).

**DDR-04 (majeure) : modification de fiche perdue en partie.**
Étapes : « Modifier la fiche », changer code BIC, code banque BEAC, origine et sigle ; enregistrer ; recharger. Résultat :
seul le sigle change ; BIC, BEAC et origine reprennent leur ancienne valeur sans message. La date de prise de contact n'est
pas modifiable (absente du formulaire) alors que le service l'accepte. Captures 029, 030.
Cause probable : `participant.ts` (modification de fiche : seuls nom, sigle, pays, catégorie et projet sont repris ; le
service réel `appliquerFiche` reprend tous les champs).

**DDR-05 (majeure) : dépôt de preuve par un acteur non habilité et attribué à un autre.**
Étapes : chef de la DDRM, dossier LIA-2026-066, onglet Opérations, rang 3 « Arrêt du périmètre des services » (responsable
Chargé DDRM 1), joindre un fichier, « Déposer la preuve ». Résultat : dépôt accepté, « Déposée par Chargé DDRM 1 ». Attendu :
action absente pour le chef (sans HOM_EVIDENCE_SUBMIT, non responsable) et auteur réel tracé. Captures 063, 064.
Cause probable : `pages/des/parcours/operations/Execution.tsx` `usePermis` (preuve ouverte à HOM_STEP_MANAGE), et
`services/simule/parcours.ts` (actions permises calculées sans l'acteur, auteur du dépôt pris dans le responsable).

**DDR-06 (majeure) : affectation des ressources acceptée sans désignation.**
Étapes : chef, tiroir du rang 6, « Affecter les ressources », laisser toutes les fonctions « Non affecté », valider. Résultat :
fenêtre fermée, rang 6 clos (Lancement 1 sur 2). Attendu : refus, la fenêtre annonce « Les responsables fonctionnel et
technique sont exigés ». Captures 075, 076.

**DDR-07 (majeure) : filtre Statut de la liste des institutions sans effet.**
Étapes : Institutions membres, liste « Statut », choisir « En homologation » (souris ou clavier). Résultat : la liste reste
entière et la liste déroulante revient à « Tous ». Par l'adresse `?statut=CANDIDAT` la liste est filtrée mais la liste
déroulante affiche « Tous ». Capture 118.
Cause probable : `pages/des/InstitutionsPage.tsx` (filtre initial `defaultQuery` et `onQuery`) ou le filtre du composant
`design-system/src/crud/ResourceList/ResourceList.tsx`.

**DDR-08 (majeure) : acteurs non notifiés des décisions et des opérations.**
Étapes : le chef refuse puis prononce la proposition du chargé ; le conducteur renvoie l'opération 3 ; la liaison LIA-2026-066
ouvre les rangs 3 à 7 de la chargée. Résultat : la cloche de la chargée ne change pas (mêmes trois notifications de
démonstration). Attendu : notification du refus et du prononcé au proposant, de l'opération ouverte ou renvoyée à son
responsable (« Chaque opération ouverte parvient à son responsable (tâche et notification) », écran Lancer une
certification). Captures 052, 067.
Cause probable : `frontend/packages/lot1/src/services/notifications.ts` (seules les décisions à prononcer sont émises).

**DDR-09 (majeure) : tâches confiées perdues entre acteurs.**
Étapes : chef, Tâches, « Nouvelle tâche » confiée à Fatimé Abakar Hissein, puis une tâche à l'unité DDRM (visibles dans
Confiées) ; passer à la chargée. Résultat : ni « Mes tâches » ni « À prendre » ne les contiennent ; de même une tâche de
décision prise par le chef disparaît de ses tâches après rechargement. Captures 082, 083, 084.
Cause probable : pilotage simulé hors de la démonstration continue (`services/simule/index.ts`, `persistanceDemo` limitée à
parcours et participant).

**DDR-10 (majeure) : action d'un autre profil annoncée au chef de la DDRM.**
Étapes : chef de la DDRM, Mes actions de certification. Résultat : ligne « Signature du procès-verbal » (LIA-2026-027,
R · Responsable, « Signer le procès-verbal ») ; l'écran ouvert (onglet Décision) n'offre aucun bouton de signature. Attendu :
ligne absente (CER_QUITUS_TECHNIQUE_SIGN est au chef du DES). Captures 072, 073.
Cause probable : `services/simule/parcours.ts` `mesOperations` et `tachesDe` (fonction CHEF_DES rattachée aux deux chefs de
département sans croiser la permission du geste).

### Mineures

**DDR-11 (mineure)** : le bouton « Ouvrir le calendrier » des champs date n'ouvre rien ; le message d'erreur de la date n'est
pas relié au champ (aria-describedby absent). Capture 010. Cause : `design-system/src/saisie/DateField/DateField.tsx` (bouton
sans gestionnaire, `open` jamais piloté).

**DDR-12 (mineure)** : à 1440 px la liste des institutions déborde (tableau 1245 px dans 1110 px) ; boutons « Proposer
l'adhésion » et « Modifier la fiche » coupés. Capture 004.

**DDR-13 (mineure)** : tiroir d'une pièce : Chronologie vide sans état vide, Pièces et Audit « Aucun élément » malgré dépôt,
rejet et redépôt, alors que la confirmation annonce « Consigné dans la chronologie et le registre d'audit » ; empreinte
SHA-256 « 00000000…00000000 ». Capture 022. Cause : `design-system/src/conteneurs/Drawer/Drawer.tsx` (Timeline sans
événements) et tiroir de pièce sans chronologie.

**DDR-14 (mineure)** : codes bruts affichés : « ATTENDUE » et « DIRECT » (Prérequis, colonne Référence), « CANDIDAT vers
ADHERENT » (description de la tâche de décision), « AGREGATEUR » (arbre des référentiels), « application/pdf,image/png,image/jpeg »
(dépôt de pièce) ; formats admis hétérogènes (pièce : MIME ; preuve : « PDF, PNG, JPEG » ; compte rendu : « .pdf,.docx,.odt »).
Captures 014, 016, 041, 079, 117.

**DDR-15 (mineure)** : date de référence des services simulés figée au 29/09/2026 alors que l'horloge affiche le 08/10/2026 :
liaison ouverte le 8 octobre avec « Date de démarrage 29 sept. 2026 », étapes 1 et 2 « Close le 9 sept. 2026 » pour une
adhésion prononcée le 8 octobre, échéances passées (3 oct.) marquées « Échéance proche », preuve « Déposée le 29 sept. »
après un renvoi daté du 8 oct., signature « Horodatée le 28/09/2026 14:05 », blocage « depuis le 20 sept. (9 jours) »,
notification « il y a 5 min » pour une proposition d'une minute, tâche de décision « Créée le 18:50 » pour une proposition de
18:48. Captures 037, 038, 047, 060, 061, 069.

**DDR-16 (mineure)** : libellés : « 1 éléments » dans toutes les paginations ; résumé d'erreurs dans l'ordre Dénomination,
Pays, Catégorie (formulaire : Catégorie avant Pays) ; « Chargé de la DDRM : Chargé DDRM » au lieu du nom de l'agent ;
description de la tâche « depuis l'onglet Transitions » (onglet nommé « Propositions de statut ») ; colonne « Unité » valant
« Décision à prononcer » ; « Responsable : À prendre : » ; « MS-PARTICIPANT » et « MS-HOMOLOGATION » cités dans la fenêtre
d'ouverture de liaison. Captures 006, 012, 040, 041, 122.

**DDR-17 (mineure)** : propositions de statut : pastille « sur proposition » qui déborde de sa cellule ; proposant absent du
tableau et du tiroir ; refus du service (prérequis) affiché en texte neutre, sans mise en forme d'erreur ; « Proposer
l'adhésion » reste actif avec une proposition en attente ou 0 prérequis sur 5. Captures 015, 031, 032, 099.

**DDR-18 (mineure)** : la fenêtre des conditions d'adhésion se rouvre sans le compte de règlement (ni de cantonnement) déjà
enregistré : un nouvel enregistrement l'efface. Cause : `formulaires.tsx`, valeurs initiales limitées au code et au mode.

**DDR-19 (mineure)** : la longueur minimale du motif (10 caractères) n'est indiquée que par l'infobulle du bouton désactivé.
Capture 020.

**DDR-20 (mineure)** : notifications de démonstration identiques pour tous les profils internes : la chargée reçoit « Rapport
de Joël Ekotto Mballa » (rapports reçus, sans PIL_TEAM_READ) et « Action attendue : Atelier comptabilisation » (fonction
Exploitation) ; « Tâche confiée par Paul Mbarga Atangana » ouvre la liste des tâches sans la tâche, absente de la liste.
Captures 085, 086.

**DDR-21 (mineure)** : données de tâches contradictoires : « Mettre à jour la fiche de procédure du service » est à la fois
dans « Mes tâches » du chef (confiée par Jean-Baptiste Ella Mintsa) et dans ses « Confiées » (responsable Fatimé Abakar
Hissein). Captures 039, 082.

**DDR-22 (mineure)** : portail : la notification « Action attendue : Paramétrage d'essai » mène à « Ce livrable ne vous est pas
demandé » ; sous-titre de la page Notifications (« Messages reçus, mentions et réponses, tâches confiées et rapports
soumis ») inadapté au correspondant ; pied de page et écran de connexion « Usage interne, confidentiel » sur le portail
externe ; bouton « Réessayer » sur une liaison introuvable. Captures 105, 108, 110.

**DDR-23 (mineure)** : portail en thème sombre : le tableau vide « Ce qui est attendu de vous » a un fond bleu saturé et une
illustration grise peu lisible. Capture 115.

**DDR-24 (mineure)** : la liaison LIA-2026-066 (Carte interbancaire) cite « Référentiel d'opérations version 4 » alors que
l'arbre des référentiels n'affiche que la version 3 publiée et un brouillon. Captures 060, 117.

**DDR-25 (mineure)** : banques partenaires proposées différentes selon le chemin d'ouverture : Lancer une certification
(Banque de Libreville, Banque B) ; fenêtre de la fiche (Banque Atlantique du Littoral, en homologation, et Banque B). Captures
059, 122.

**DDR-26 (mineure)** : empreinte SHA-256 identique (f50c43ef…95f19cf9) pour deux fichiers de preuve différents ; « Empreinte de ce
qui sera signé » égale à l'identifiant TRA-0074. Captures 046, 069.

**DDR-27 (mineure)** : « Type de projet » obligatoire sans astérisque dans Lancer une certification et la fenêtre d'ouverture.
Capture 058.

**DDR-28 (mineure)** : « Annuler la tâche » est proposé sur une tâche de décision créée par le service participant. Capture
042.

**DDR-29 (mineure)** : « Conformité documentaire : À régulariser » demeure sur la fiche alors que toutes les pièces sont
validées et l'adhésion prononcée. Captures 030, 088.

## 5. Synthèse

84 contrôles, 64 conformes, 20 en anomalie. 29 anomalies : 2 bloquantes (DDR-01, DDR-02), 8 majeures (DDR-03 à DDR-10),
19 mineures (DDR-11 à DDR-29). Les droits de la barre latérale et les refus d'accès (adresses directes, console, portail,
cloisonnement du correspondant) sont conformes ; les écarts portent sur les règles métier des services simulés
(maker-checker, conditions, fiche, RACI des dépôts, affectation), la continuité de la démonstration et les notifications.

## 6. Correction

Registre détaillé : `docs/recette/corrections/ddrm.json` ; captures après : `docs/recette/captures/ddrm/corrections/<ID>-apres.png`
(même geste, même profil, même thème, fr-FR). Les services simulés appliquent les mêmes règles que les services réels.

| Anomalie | Statut | Correction | Fichiers | Test |
|---|---|---|---|---|
| DDR-01 | corrigée | Le service simulé prend l'utilisateur comme proposant et approbateur et refuse 403 PAR_SEPARATION_OF_DUTIES comme MS-PARTICIPANT ; les boutons Approuver et Rejeter ne sont plus offerts au proposant. | `participant.ts`, `index.ts`, `institution.tsx`, `InstitutionPage.tsx` | frontend/packages/lot1/src/services/simule/participant.test.ts (DDR-01 : maker-checker) ; ms-participant InstitutionLifecycleServiceTest.approbateurIdentique (règle serveur déjà présente) |
| DDR-02 | corrigée | Le compteur d'identifiants du registre simulé reprend après le plus grand identifiant relu du stockage ; le serveur attribue des UUID (sans défaut). | `participant.ts` | frontend/packages/lot1/src/services/simule/participant.test.ts (DDR-02) |
| DDR-03 | corrigée | Contrôles du code participant, du mode et des comptes (RG-11, RG-12) dans MS-PARTICIPANT (422 PAR_CONDITIONS_INVALID, un message par champ), dans le service simulé et dans le formulaire, messages sous les champs, sans mode imposé. | `InstitutionLifecycleService.java`, `ParticipantException.java`, `participant.ts`, `saisie.ts`, `formulaires.tsx` | ms-participant InstitutionLifecycleServiceTest.conditionsIncompletes ; frontend/packages/lot1/src/services/simule/participant.test.ts (DDR-03) |
| DDR-04 | corrigée | Le service simulé reprend tous les champs de la fiche comme appliquerFiche (BIC, code BEAC, origine, date de prise de contact) et contrôle l'unicité des codes ; la date de prise de contact est modifiable dans le formulaire. | `participant.ts`, `institution.tsx`, `formulaires.tsx` | frontend/packages/lot1/src/services/simule/participant.test.ts (DDR-04) |
| DDR-05 | corrigée | Le dépôt n'est offert qu'au RACI (responsable, conducteur) dans MS-HOMOLOGATION et le service simulé ; la supervision HOM_STEP_MANAGE reste admise et journalisée sans être offerte ; l'auteur tracé est l'utilisateur réel. | `ParcoursProjection.java`, `parcours.ts`, `index.ts` | ms-homologation ParcoursProjectionTest.supervisionNonOfferte ; frontend/packages/lot1/src/services/simule/ddrm-recette.test.ts (DDR-05) |
| DDR-06 | corrigée | La fenêtre exige une personne pour chaque fonction présentée (message sous chaque fonction) et annonce la règle appliquée ; le service simulé contrôle comme MS-HOMOLOGATION la personne et sa fonction et l'affecte sur les opérations non faites. | `DialogueAffectation.tsx`, `parcours-cadrage.ts`, `parcours.ts` | frontend/packages/lot1/src/services/simule/ddrm-recette.test.ts (DDR-06) |
| DDR-07 | corrigée | La barre de filtres de ResourceList est pilotée : la valeur choisie reste affichée, part dans la requête, et l'adresse ?statut= la présélectionne ; « Tous » retire le filtre. | `ResourceList.tsx` | frontend/packages/lot1/src/pages/des/InstitutionsPage.test.tsx (DDR-07) |
| DDR-08 | corrigée | MS-PARTICIPANT publie le proposant dans transition-approved et transition-rejected ; MS-NOTIFICATION avise le proposant du prononcé et du refus ; le service simulé notifie aussi les opérations ouvertes et renvoyées à leur responsable. | `TransitionApprovedPayload.java`, `TransitionRejectedPayload.java`, `BoiteNotificationsService.java`, `EvenementsNotificationsConsumer.java`, `notifications.ts`, `parcours.ts`, `index.ts` | ms-notification BoiteNotificationsServiceTest.transitionTranchee et EvenementsNotificationsConsumerTest.transitionsTranchees ; frontend/packages/lot1/src/services/notifications.test.ts (DDR-08) |
| DDR-09 | corrigée | Les tâches simulées sont gardées en démonstration continue (gm.demo.pilotage), effets des autres services compris, sans réattribution d'identifiant. | `pilotage.ts`, `index.ts` | frontend/packages/lot1/src/services/simule/ddrm-recette.test.ts (DDR-09) |
| DDR-10 | corrigée | Le service simulé ne confond plus la fonction CHEF_DES avec la fonction partagée CHEFS_DEPARTEMENT : la signature du procès-verbal n'est plus annoncée au chef de la DDRM (le serveur n'avait pas le défaut). | `parcours.ts`, `DialogueAffectation.tsx` | frontend/packages/lot1/src/services/simule/ddrm-recette.test.ts (DDR-10) |
| DDR-11 | corrigée | Le bouton « Ouvrir le calendrier » ouvre un calendrier piloté (mois, jour choisi, changement de mois) ; le message d'erreur est relié au champ par aria-describedby. | `DateField.tsx`, `CalendarPopover.tsx` | Capture après ; tests du système de conception existants (saisie de date) |
| DDR-12 | corrigée | Les actions « Proposer l'adhésion » et « Modifier la fiche » quittent la liste (fiche et formulaire) : le tableau tient dans la largeur à 1440 px. | `institution.tsx` | Capture après (tableau de 1110 px dans 1110 px) |
| DDR-13 | corrigée | Le tiroir d'une pièce montre sa chronologie (dépôt, examen, motif), le fichier déposé et son empreinte ; le service simulé calcule une empreinte propre à chaque pièce. | `InstitutionPage.tsx`, `participant.ts`, `ddrm-recette.ts` | frontend/packages/lot1/src/pages/des/InstitutionPage.test.tsx |
| DDR-14 | corrigée | Références des prérequis nommées (état de la pièce, mode d'adhésion) ; statuts nommés dans le titre et la description de la tâche de décision, au service simulé comme dans MS-PILOTAGE. Le type AGREGATEUR et les formats de fichier lisibles relèvent d'autres lots (déjà nommés). | `InstitutionPage.tsx`, `index.ts`, `ApprobationsTransitionsService.java` | frontend/packages/lot1/src/pages/des/InstitutionPage.test.tsx (DDR-14) ; ms-pilotage ApprobationsTransitionsServiceTest |
| DDR-15 | corrigée | Le jour de référence des services simulés est le jour courant de Douala (même horloge que l'en-tête) ; étapes 1 et 2 datées par le registre des institutions, signature horodatée à l'instant, tâche et notification de décision datées de la proposition. | `parcours.ts`, `participant.ts`, `pilotage.ts`, `index.ts`, `notifications.ts`, `SignatureField.tsx` | frontend/packages/lot1/src/services/simule/ddrm-recette.test.ts (DDR-15) |
| DDR-16 | corrigée | « 1 élément » au singulier ; résumé d'erreurs dans l'ordre des champs ; nom de la chargée ; onglet « Propositions de statut » dans la description ; colonne Unité « Toutes unités » ; « À prendre » sans deux-points ; plus de nom de microservice dans la fenêtre d'ouverture de liaison. | `Pagination.tsx`, `libelles.ts`, `ResourceForm.tsx`, `dialoguesTache.tsx`, `TachesPage.tsx`, `cadrage.ts`, `parcours-cadrage.ts`, `ddrm-recette.ts`, `index.ts`, `ApprobationsTransitionsService.java` | frontend/packages/lot1/src/pages/des/activite/TachesPage.test.tsx (DDR-16) ; frontend/packages/lot1/src/pages/des/TableauDeBordPage.test.tsx |
| DDR-17 | corrigée | Pastille « sur proposition » contenue dans sa cellule ; proposant affiché dans le tableau et le tiroir ; « Proposer l'adhésion » non offert pendant une proposition en attente ou avec des prérequis manquants. La mise en forme d'erreur du refus est due au lot certification (CER-10). | `institution.tsx`, `InstitutionPage.tsx`, `ddrm-recette.ts`, `05-badges.css` | frontend/packages/lot1/src/pages/des/InstitutionPage.test.tsx (DDR-17) |
| DDR-18 | corrigée | La fenêtre des conditions se rouvre sur toutes les valeurs enregistrées (comptes de règlement et de cantonnement, banque marraine). | `formulaires.tsx`, `saisie.ts` | frontend/packages/lot1/src/pages/des/InstitutionPage.test.tsx (DDR-03 et DDR-18) |
| DDR-19 | corrigée | L'aide du champ motif annonce « 10 caractères au moins » dans les trois langues. | `libelles.ts` | Capture après |
| DDR-20 | corrigée | Notifications de démonstration propres au profil : rapport reçu d'un membre de son équipe seulement, action d'homologation aux seuls porteurs de la fonction, tâche confiée réelle de sa liste avec un lien vers elle. | `notifications.ts`, `pilotage.ts`, `index.ts`, `App.tsx`, `App.tsx` | frontend/packages/lot1/src/services/notifications.test.ts (DDR-20) ; frontend/e2e/specs/notifications.spec.ts |
| DDR-21 | corrigée | Le jeu de tâches distingue la fiche du responsable d'équipe (confiée par son propre responsable) de celle qu'il confie à ses membres. | `pilotage.ts` | Capture après |
| DDR-22 | corrigée | Pied de page et écran de connexion du portail en « Diffusion restreinte au participant et au GIMAC » (couche servie). Lien de la notification (CER-12), sous-titre de la page Notifications et bouton Réessayer sur une liaison introuvable (CER-18) corrigés par d'autres lots. | `Footer.tsx`, `Enveloppe.tsx`, `PageConnexion.tsx`, `fr.json`, `en.json`, `es.json`, `libelles.ts` | Capture après |
| DDR-23 | corrigée | La ligne d'état vide n'est ni zébrée ni surlignée au survol, et son illustration est contrastée en thème sombre. | `09-tableau.css`, `DataTable.tsx` | Capture après (thème sombre) |
| DDR-24 | corrigée | La liaison ouverte instancie la version publiée de son type (arbre des référentiels), version 3 pour la carte interbancaire. | `parcours.ts`, `index.ts` | frontend/packages/lot1/src/pages/des/parcours/DossierLiaisonPage.test.tsx (DDR-24) |
| DDR-25 | pas un défaut | Pas un défaut : les deux chemins appliquent la même règle (banques adhérentes, en homologation ou actives, hors l'institution elle-même) ; l'écart observé tient à l'état du registre au moment de chaque geste et à l'institution exclue. | aucun | Sans objet |
| DDR-26 | corrigée | Empreinte SHA-256 calculée sur ce qui est signé (institution, transition, statuts, motif, auteur) ; deux fichiers de preuve différents ont deux empreintes différentes. | `institution.tsx`, `parcours.ts`, `index.ts` | frontend/packages/lot1/src/services/simule/ddrm-recette.test.ts (DDR-26) ; frontend/packages/lot1/src/ressources/institution.test.tsx |
| DDR-27 | corrigée | Le groupe de choix obligatoire porte l'astérisque après sa légende, comme les autres champs. | `RadioGroup.tsx`, `ResourceForm.tsx`, `LancerCertificationPage.tsx` | Capture après |
| DDR-28 | corrigée | Ni annulation ni fin à la main d'une tâche de décision : elle se clôt quand le service prononce ou refuse ; MS-PILOTAGE refuse 409 PIL_TACHE_DECISION. | `dialoguesTache.tsx`, `pilotage.ts`, `TacheService.java` | frontend/packages/lot1/src/pages/des/activite/TachesPage.test.tsx (DDR-28) ; ms-pilotage ApprobationTransitionKafkaIT |
| DDR-29 | corrigée | Au prononcé de l'adhésion, dossier complet, la conformité documentaire passe à CONFORME, comme le service. | `participant.ts` | frontend/packages/lot1/src/services/simule/participant.test.ts (DDR-29) |

Point transmis par le lot certification (CER-17, statut « En homologation » de INST-0042) : non modifié. Passer
l'institution à « Actif » (jalon EN_PRODUCTION de LIA-2025-088) la rendrait inéligible à l'ouverture d'une liaison
(LiaisonOpeningPolicy de MS-HOMOLOGATION, D-57 : adhérent ou en homologation seulement) et fermerait le parcours de
démonstration de ses liaisons en cours ; la question de l'ouverture d'une nouvelle liaison par une institution active reste
à trancher au référentiel fonctionnel.
