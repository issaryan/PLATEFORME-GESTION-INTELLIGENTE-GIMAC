# Politique d'accès du lot 1 (PI-GIMAC-01 §0.6), évaluée par la passerelle (filtre OpaAuthorize)
# puis doublée par @PreAuthorize dans chaque service. Refus par défaut (RG-24).
# Écart au plan : sa première règle allow acceptait aussi le portail sans institution ;
# ici, un chemin du portail n'est autorisé que si le jeton porte institution_id (RG-59).
# D-61 : toute route /bff exige un jeton issu d'une authentification à deux facteurs (acr 2 ou 3).
# D-65 : seule exception, la session restreinte du premier accès (acr 1, règle premier_acces), limitée
# à l'assistant de configuration du compte sur MS-IAM ; une session restreinte n'ouvre rien d'autre.
package gimac.authz

import rego.v1

default allow := false

regles := [
	# Types de projet et catalogue des fonctions (D-91, docs/lot1/types-projet.md §1 et §2) : lecture par
	# HOM_REFERENTIEL_READ, création et modification par HOM_REFERENTIEL_EDIT ; aucune suppression.
	{"m": "GET", "re": `^/bff/des/homologation/types-projet(/[^/]+)?$`, "p": "HOM_REFERENTIEL_READ"},
	{"m": "POST", "re": `^/bff/des/homologation/types-projet$`, "p": "HOM_REFERENTIEL_EDIT"},
	{"m": "PUT", "re": `^/bff/des/homologation/types-projet/[^/]+$`, "p": "HOM_REFERENTIEL_EDIT"},
	{"m": "GET", "re": `^/bff/des/homologation/referentiels/fonctions(/[^/]+)?$`, "p": "HOM_REFERENTIEL_READ"},
	{"m": "POST", "re": `^/bff/des/homologation/referentiels/fonctions$`, "p": "HOM_REFERENTIEL_EDIT"},
	{"m": "PUT", "re": `^/bff/des/homologation/referentiels/fonctions/[^/]+$`, "p": "HOM_REFERENTIEL_EDIT"},
	# Suppression d'un brouillon du référentiel (D-92) ; une version publiée ne se supprime pas (MS-HOMOLOGATION).
	{"m": "DELETE", "re": `^/bff/des/homologation/referentiels/[^/]+$`, "p": "HOM_REFERENTIEL_EDIT"},
	# Signature d'une opération d'une liaison (D-91 §3) : tout membre interne qui lit les liaisons ; MS-HOMOLOGATION
	# vérifie le signataire attendu (fonction du catalogue), l'ordre et l'assertion FIDO2 (SIGNATURE_OPERATION).
	{"m": "POST", "re": `^/bff/des/homologation/liaisons/[^/]+/operations/[^/]+/signatures/[^/]+$`, "p": "HOM_LIAISON_READ"},
	{"m": "POST", "re": `^/bff/des/homologation/referentiels/[^/]+/publication$`, "p": "HOM_REFERENTIEL_PUBLISH"},
	# Modélisateur de processus (D-77, docs/lot1/modelisateur-processus.md §4) : édition des brouillons,
	# migration des liaisons ; la lecture reste au @PreAuthorize (HOM_REFERENTIEL_READ).
	{"m": "POST", "re": `^/bff/des/homologation/referentiels/[^/]+/migrations(/simulation)?$`, "p": "HOM_REFERENTIEL_MIGRATE"},
	{"m": "POST", "re": `^/bff/des/homologation/referentiels(/[^/]+/(operations|controle))?$`, "p": "HOM_REFERENTIEL_EDIT"},
	{"m": "PUT", "re": `^/bff/des/homologation/referentiels/[^/]+/(operations/[^/]+|positions|arcs|pilote)$`, "p": "HOM_REFERENTIEL_EDIT"},
	{"m": "DELETE", "re": `^/bff/des/homologation/referentiels/[^/]+/operations/[^/]+$`, "p": "HOM_REFERENTIEL_EDIT"},
	# Étapes configurables d'un brouillon (D-89, docs/lot1/referentiel-droits-etapes.md §2) : ajout, renommage,
	# ordre et retrait ; la lecture filtrée du référentiel (graphe, liste, export) reste au @PreAuthorize.
	{"m": "POST", "re": `^/bff/des/homologation/referentiels/[^/]+/etapes(/[^/]+|/ordre)?$`, "p": "HOM_REFERENTIEL_EDIT"},
	{"m": "PUT", "re": `^/bff/des/homologation/referentiels/[^/]+/etapes(/[^/]+|/ordre)?$`, "p": "HOM_REFERENTIEL_EDIT"},
	{"m": "DELETE", "re": `^/bff/des/homologation/referentiels/[^/]+/etapes(/[^/]+|/ordre)?$`, "p": "HOM_REFERENTIEL_EDIT"},
	{"m": "POST", "re": `^/bff/des/homologation/liaisons$`, "p": "HOM_LIAISON_OPEN"},
	# Cadrage (étape 3, D-58) : désignation du conducteur de projet par un chef de département.
	{"m": "PUT", "re": `^/bff/des/homologation/liaisons/[^/]+/chef-de-projet$`, "p": "HOM_PROJECT_DESIGNATE"},
	{"m": "POST", "re": `^/bff/des/homologation/operations/[^/]+/validation$`, "p": "HOM_OPERATION_VALIDATE"},
	{"m": "POST", "re": `^/bff/des/homologation/operations/[^/]+/reouverture$`, "p": "HOM_OPERATION_REOPEN"},
	# Parcours, étapes 4 à 10 (docs/lot1/parcours-backend.md §1) : gestes à permission unique. Les gestes
	# ouverts à plusieurs permissions (livrable, intervenants, services supplémentaires) restent au @PreAuthorize.
	{"m": "POST", "re": `^/bff/des/homologation/parcours/operations/[^/]+/validation$`, "p": "HOM_OPERATION_VALIDATE"},
	# Renvoi au responsable par le validateur (D-93 §2) ; le service vérifie le validateur désigné. L'affectation d'une
	# fonction (PUT liaisons/{id}/fonctions/{fonction}, HOM_STEP_MANAGE ou HOM_OPERATION_ASSIGN) et GET moi/fonctions
	# (tout compte) restent au @PreAuthorize.
	{"m": "POST", "re": `^/bff/des/homologation/parcours/operations/[^/]+/renvoi$`, "p": "HOM_OPERATION_VALIDATE"},
	# D-109 (docs/lot1/referentiel-preuves-signatures.md) : chronologie réelle de la liaison et pièce d'une opération
	# (empreinte revérifiée par MS-HOMOLOGATION) en lecture ; un livrable « sans objet » motivé (HOM_STEP_MANAGE ou
	# HOM_EVIDENCE_SUBMIT, comme le dépôt) reste au @PreAuthorize, le droit de dépôt étant décidé par le parcours.
	{"m": "GET", "re": `^/bff/des/homologation/liaisons/[^/]+/chronologie$`, "p": "HOM_LIAISON_READ"},
	{"m": "GET", "re": `^/bff/des/homologation/parcours/operations/[^/]+/pieces/[^/]+$`, "p": "HOM_LIAISON_READ"},
	# D-98 : la preuve, comme le livrable, est ouverte à HOM_STEP_MANAGE ou HOM_EVIDENCE_SUBMIT (@PreAuthorize du service).
	{"m": "PUT", "re": `^/bff/des/homologation/liaisons/[^/]+/ressources$`, "p": "HOM_STEP_MANAGE"},
	{"m": "POST", "re": `^/bff/des/homologation/(liaisons/[^/]+/blocages|blocages/[^/]+/levee)$`, "p": "HOM_STEP_MANAGE"},
	# Remise de la fiche de suivi : conducteur (HOM_STEP_MANAGE) ou Normes et Agréments (CER_DOSSIER_VERIFY), au @PreAuthorize.
	{"m": "POST", "re": `^/bff/des/homologation/liaisons/[^/]+/(couverture|fiche-suivi)/constat$`, "p": "CER_DOSSIER_VERIFY"},
	{"m": "GET", "re": `^/bff/des/homologation/tableaux-de-bord/(acteur|pilotage)$`, "p": "HOM_DASHBOARD_READ"},
	{"m": "POST", "re": `^/bff/des/certification/circuits/[^/]+/quitus/technique$`, "p": "CER_QUITUS_TECHNIQUE_SIGN"},
	{"m": "POST", "re": `^/bff/des/certification/circuits/[^/]+/quitus/securite$`, "p": "CER_QUITUS_SECURITE_SIGN"},
	{"m": "POST", "re": `^/bff/des/certification/circuits/[^/]+/quitus/conformite$`, "p": "CER_QUITUS_CONFORMITE_SIGN"},
	{"m": "POST", "re": `^/bff/des/certification/circuits/[^/]+/quitus/direction-generale$`, "p": "CER_QUITUS_DG_SIGN"},
	# Verrous de la décision (D-56) : procès-verbal et approbation de la sécurité (MS-CERTIFICATION).
	{"m": "POST", "re": `^/bff/des/certification/liaisons/[^/]+/proces-verbal$`, "p": "CER_PV_ESTABLISH"},
	{"m": "POST", "re": `^/bff/des/certification/liaisons/[^/]+/proces-verbal/signature-gimac$`, "p": "CER_QUITUS_TECHNIQUE_SIGN"},
	{"m": "POST", "re": `^/bff/des/certification/liaisons/[^/]+/approbation-securite/dossier$`, "p": "CER_CHANGE_RECORD_LINK"},
	{"m": "PUT", "re": `^/bff/des/certification/liaisons/[^/]+/approbation-securite/fiche$`, "p": "CER_CHANGE_RECORD_LINK"},
	{"m": "POST", "re": `^/bff/des/certification/liaisons/[^/]+/approbation-securite/(attestations/[^/]+/verification|archivage)$`, "p": "CER_SECURITY_VERIFY"},
	{"m": "POST", "re": `^/bff/des/certification/liaisons/[^/]+/approbation-securite/visas/CONFORMITE$`, "p": "CER_QUITUS_CONFORMITE_SIGN"},
	{"m": "POST", "re": `^/bff/des/certification/liaisons/[^/]+/approbation-securite/visas/DIRECTION_GENERALE$`, "p": "CER_QUITUS_DG_SIGN"},
	# Portail (qualité Externe) : gestes du participant sur sa liaison, cloisonnés par l'institution du jeton.
	{"m": "PUT", "re": `^/bff/portail/homologation/liaisons/[^/]+/(coordonnees-techniques|fiche-suivi)$`, "p": "HOM_PORTAL_SUBMIT"},
	{"m": "POST", "re": `^/bff/portail/homologation/operations/[^/]+/livrable$`, "p": "HOM_PORTAL_SUBMIT"},
	{"m": "POST", "re": `^/bff/portail/certification/liaisons/[^/]+/proces-verbal/signature$`, "p": "CER_PORTAL_SIGN"},
	{"m": "POST", "re": `^/bff/portail/certification/liaisons/[^/]+/attestations/[^/]+$`, "p": "HOM_PORTAL_SUBMIT"},
	{"m": "GET", "re": `^/bff/portail/homologation/`, "p": "HOM_PORTAL_READ"},
	{"m": "POST", "re": `^/bff/portail/homologation/operations/[^/]+/preuves$`, "p": "HOM_PORTAL_SUBMIT"},
	{"m": "GET", "re": `^/bff/portail/certification/`, "p": "CER_PORTAL_READ"},
	# Registre des institutions, étapes 1 et 2 du parcours (D-57).
	{"m": "GET", "re": `^/bff/des/participant/institutions(/.*)?$`, "p": "PAR_INSTITUTION_READ"},
	{"m": "POST", "re": `^/bff/des/participant/institutions$`, "p": "PAR_INSTITUTION_CREATE"},
	{"m": "PUT", "re": `^/bff/des/participant/institutions/[^/]+(/conditions-adhesion)?$`, "p": "PAR_INSTITUTION_EDIT"},
	{"m": "POST", "re": `^/bff/des/participant/institutions/[^/]+/pieces/[^/]+/(depot|validation|rejet)$`, "p": "PAR_ADHESION_INSTRUCT"},
	{"m": "POST", "re": `^/bff/des/participant/institutions/[^/]+/transitions$`, "p": "PAR_TRANSITION_REQUEST"},
	{"m": "POST", "re": `^/bff/des/participant/institutions/[^/]+/transitions/[^/]+/(approbation|rejet)$`, "p": "PAR_TRANSITION_APPROVE"},
	# Pièces déposées par le correspondant depuis le portail (MS-DOCUMENT).
	{"m": "POST", "re": `^/bff/portail/document/documents$`, "p": "HOM_PORTAL_SUBMIT"},
	# Sandbox GIMAC, administration déléguée des comptes d'une institution (D-115, docs/lot1/sandbox-existant-adapte.md §4
	# US-EXI-03) : administrateur de l'institution, institution exigée comme pour toute route du portail ; MS-IAM borne
	# chaque acte à l'institution du jeton.
	{"m": "GET", "re": `^/bff/portail/sbx-habilitations/(roles|comptes(/[^/]+)?)$`, "p": "SBX_HAB_ADMINISTRATION"},
	{"m": "POST", "re": `^/bff/portail/sbx-habilitations/comptes$`, "p": "SBX_HAB_ADMINISTRATION"},
	{"m": "PUT", "re": `^/bff/portail/sbx-habilitations/comptes/[^/]+/(roles|etat)$`, "p": "SBX_HAB_ADMINISTRATION"},
	# Dépôts fragmentés depuis le portail (D-74, docs/lot1/depots-fragmentes.md §3) : ouverture, parties,
	# état, fin et abandon ; institution exigée comme pour toute route du portail.
	{"m": "POST", "re": `^/bff/portail/document/depots$`, "p": "HOM_PORTAL_SUBMIT"},
	{"m": "PUT", "re": `^/bff/portail/document/depots/[^/]+/parties/[^/]+$`, "p": "HOM_PORTAL_SUBMIT"},
	{"m": "GET", "re": `^/bff/portail/document/depots/[^/]+$`, "p": "HOM_PORTAL_SUBMIT"},
	{"m": "POST", "re": `^/bff/portail/document/depots/[^/]+/fin$`, "p": "HOM_PORTAL_SUBMIT"},
	{"m": "DELETE", "re": `^/bff/portail/document/depots/[^/]+$`, "p": "HOM_PORTAL_SUBMIT"},
	# Administration (D-62, D-69) : comptes, journal, politique de sécurité et profils depuis la console et
	# depuis le département (RSSI, super administrateur) ; serveur de courriels depuis la console seulement.
	# La première règle correspondante s'applique : les chemins les plus précis viennent d'abord.
	{"m": "GET", "re": `^/bff/(des|console)/iam/comptes/journal$`, "p": "IAM_JOURNAL_READ"},
	{"m": "GET", "re": `^/bff/(des|console)/iam/comptes(/.*)?$`, "p": "IAM_ACCOUNT_READ"},
	{"m": "POST", "re": `^/bff/(des|console)/iam/comptes$`, "p": "IAM_ACCOUNT_CREATE"},
	{"m": "PUT", "re": `^/bff/(des|console)/iam/comptes/[^/]+/profils$`, "p": "IAM_ACCOUNT_ROLES"},
	# Rattachement d'un compte à une unité et à son responsable (D-76, docs/lot1/mon-activite.md §1.3).
	{"m": "PUT", "re": `^/bff/(des|console)/iam/comptes/[^/]+/rattachement$`, "p": "IAM_ACCOUNT_EDIT"},
	{"m": "PUT", "re": `^/bff/(des|console)/iam/comptes/[^/]+$`, "p": "IAM_ACCOUNT_EDIT"},
	{"m": "POST", "re": `^/bff/(des|console)/iam/comptes/[^/]+/(activation|desactivation)$`, "p": "IAM_ACCOUNT_STATUS"},
	{"m": "DELETE", "re": `^/bff/(des|console)/iam/comptes/[^/]+$`, "p": "IAM_ACCOUNT_DELETE"},
	{"m": "POST", "re": `^/bff/(des|console)/iam/comptes/[^/]+/(actions-requises|second-facteur/reinitialisation|mot-de-passe-provisoire|sessions/fermeture)$`, "p": "IAM_ACCOUNT_SECURITY"},
	{"m": "GET", "re": `^/bff/(des|console)/iam/configuration/politique-securite$`, "p": "PLATFORM_SECURITY_POLICY_READ"},
	{"m": "PUT", "re": `^/bff/(des|console)/iam/configuration/politique-securite$`, "p": "PLATFORM_SECURITY_POLICY_EDIT"},
	{"m": "GET", "re": `^/bff/console/iam/configuration/messagerie$`, "p": "PLATFORM_MESSAGING_EDIT"},
	{"m": "PUT", "re": `^/bff/console/iam/configuration/messagerie$`, "p": "PLATFORM_MESSAGING_EDIT"},
	{"m": "POST", "re": `^/bff/console/iam/configuration/messagerie/test$`, "p": "PLATFORM_MESSAGING_EDIT"},
	{"m": "GET", "re": `^/bff/(des|console)/iam/configuration/(profils|permissions)$`, "p": "IAM_ACCOUNT_READ"},
	{"m": "PUT", "re": `^/bff/(des|console)/iam/configuration/profils/[^/]+/permissions$`, "p": "IAM_PROFILE_EDIT"},
	# Profils créés par l'administration (D-81 §5).
	{"m": "POST", "re": `^/bff/(des|console)/iam/configuration/profils$`, "p": "IAM_PROFILE_EDIT"},
	{"m": "DELETE", "re": `^/bff/(des|console)/iam/configuration/profils/[^/]+$`, "p": "IAM_PROFILE_EDIT"},
	# Libellé et description d'un profil (D-86 §2) ; MS-IAM vérifie le profil privilégié et la réauthentification.
	{"m": "PUT", "re": `^/bff/(des|console)/iam/configuration/profils/[^/]+$`, "p": "IAM_PROFILE_EDIT"},
	# Catalogue des fonctionnalités (D-94, docs/lot1/ecrans-par-configuration.md §6) : modification par
	# l'administration des profils ; MS-IAM exige le motif et la réauthentification. Lecture : règle
	# « catalogue_fonctionnalites » plus bas (tout compte interne).
	{"m": "PUT", "re": `^/bff/(des|console)/iam/configuration/fonctionnalites/[^/]+$`, "p": "IAM_PROFILE_EDIT"},
	# Matrice des pouvoirs et demandes à l'administration (D-81 §3, §4) ; MS-IAM vérifie la portée, l'acte
	# exécuté à l'acceptation et la réauthentification.
	{"m": "GET", "re": `^/bff/(des|console)/iam/pouvoirs/matrice$`, "p": "IAM_ACCOUNT_READ"},
	{"m": "PUT", "re": `^/bff/(des|console)/iam/pouvoirs/matrice/[^/]+$`, "p": "IAM_PROFILE_EDIT"},
	{"m": "GET", "re": `^/bff/des/iam/pouvoirs/moi$`, "p": "PIL_ACTIVITY_OWN"},
	# Rattachement d'un subordonné par son chef (action RATTACHEMENT_MODIFIER, D-81 §3) ; MS-IAM vérifie la
	# portée, l'unité et le n+1 proposés.
	{"m": "PUT", "re": `^/bff/des/iam/pouvoirs/subordonnes/[^/]+/rattachement$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "POST", "re": `^/bff/des/iam/demandes(/[^/]+/annulation)?$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "GET", "re": `^/bff/(des|console)/iam/demandes(/[^/]+)?$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "POST", "re": `^/bff/(des|console)/iam/demandes/[^/]+/acceptation$`, "p": "IAM_ACCOUNT_CREATE"},
	{"m": "POST", "re": `^/bff/(des|console)/iam/demandes/[^/]+/refus$`, "p": "IAM_ACCOUNT_READ"},
	# Messagerie instantanée de travail (D-64, docs/lot1/messagerie.md §1) : tout membre interne du
	# GIMAC, depuis le DES et depuis la console (RSSI, super administrateur).
	{"m": "GET", "re": `^/bff/(des|console)/collaboration/messagerie/(annuaire|non-lus|conversations(/[^/]+(/messages(/[^/]+/lectures)?)?)?|pieces/[^/]+)$`, "p": "COL_MESSAGE_READ"},
	{"m": "POST", "re": `^/bff/(des|console)/collaboration/messagerie/conversations/[^/]+/lu$`, "p": "COL_MESSAGE_READ"},
	{"m": "POST", "re": `^/bff/(des|console)/collaboration/messagerie/conversations(/[^/]+/messages)?$`, "p": "COL_MESSAGE_SEND"},
	# Groupes de discussion (D-68) : nom, membres ajoutés, retrait ou départ ; l'administration du groupe
	# est vérifiée par MS-COLLABORATION.
	{"m": "PATCH", "re": `^/bff/(des|console)/collaboration/messagerie/conversations/[^/]+$`, "p": "COL_MESSAGE_SEND"},
	{"m": "POST", "re": `^/bff/(des|console)/collaboration/messagerie/conversations/[^/]+/membres$`, "p": "COL_MESSAGE_SEND"},
	{"m": "DELETE", "re": `^/bff/(des|console)/collaboration/messagerie/conversations/[^/]+/membres/[^/]+$`, "p": "COL_MESSAGE_SEND"},
	# « En train d'écrire » (D-72) : signal éphémère ; l'appartenance est vérifiée par MS-COLLABORATION.
	{"m": "POST", "re": `^/bff/(des|console)/collaboration/messagerie/conversations/[^/]+/saisie$`, "p": "COL_MESSAGE_SEND"},
	# Organisation (D-76 §1.3) : catalogue des unités (édition par l'administration des comptes) et
	# rattachement du titulaire ; lecture du catalogue : tout membre interne (département), l'administration
	# des comptes (console).
	# Organigramme (D-102, docs/lot1/organigramme.md) : affectation de personnes à une unité depuis la console
	# (IAM_ACCOUNT_EDIT ; depuis le département, règle « affectation_unite » plus bas : administration ou chef, MS-IAM
	# vérifie le périmètre de RATTACHEMENT_MODIFIER), retrait d'une personne (IAM_ACCOUNT_EDIT), organigramme en PDF
	# (mêmes lecteurs que le catalogue).
	{"m": "POST", "re": `^/bff/console/iam/organisation/unites/[^/]+/affectations$`, "p": "IAM_ACCOUNT_EDIT"},
	{"m": "DELETE", "re": `^/bff/(des|console)/iam/organisation/unites/[^/]+/affectations/[^/]+$`, "p": "IAM_ACCOUNT_EDIT"},
	{"m": "GET", "re": `^/bff/des/iam/organisation/organigramme\.pdf$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "GET", "re": `^/bff/console/iam/organisation/organigramme\.pdf$`, "p": "IAM_ACCOUNT_READ"},
	{"m": "POST", "re": `^/bff/(des|console)/iam/organisation/unites$`, "p": "IAM_ACCOUNT_EDIT"},
	{"m": "PUT", "re": `^/bff/(des|console)/iam/organisation/unites/[^/]+$`, "p": "IAM_ACCOUNT_EDIT"},
	{"m": "GET", "re": `^/bff/des/iam/organisation/(moi|unites)$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "GET", "re": `^/bff/console/iam/organisation/unites$`, "p": "IAM_ACCOUNT_READ"},
	# Notifications de la plateforme (D-80, docs/lot1/notifications.md §3) : les siennes seulement (MS-NOTIFICATION).
	# Portail (D-93 §11.4) : mêmes routes pour un compte d'institution (règle allow du portail : institution exigée).
	{"m": "GET", "re": `^/bff/(des|console|portail)/notification/(notifications(/compteur)?|preferences)$`, "p": "NOT_INBOX_READ"},
	{"m": "POST", "re": `^/bff/(des|console|portail)/notification/notifications(/[^/]+)?/lecture$`, "p": "NOT_INBOX_READ"},
	{"m": "PUT", "re": `^/bff/(des|console|portail)/notification/preferences$`, "p": "NOT_INBOX_READ"},
	# Mon activité (D-76 §2) : agenda, activités, tâches, rapports de chacun ; équipe des responsables.
	# Les droits fins (propriétaire, équipe, destinataire) sont vérifiés par MS-PILOTAGE.
	{"m": "GET", "re": `^/bff/des/pilotage/equipe$`, "p": "PIL_TEAM_READ"},
	{"m": "GET", "re": `^/bff/des/pilotage/(agenda|activites|taches(/groupes)?(/[^/]+)?|rapports(/[^/]+(/document|/destinataires)?)?|moi/synthese)$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "POST", "re": `^/bff/des/pilotage/(agenda(/[^/]+/reponse)?|activites(/[^/]+/statut)?|taches(/[^/]+/(transition|prise|attribution|relance))?|rapports(/preremplissage|/[^/]+/(soumission|visa))?)$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "PUT", "re": `^/bff/des/pilotage/(agenda|activites|taches|rapports)/[^/]+$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "DELETE", "re": `^/bff/des/pilotage/(agenda|activites)/[^/]+$`, "p": "PIL_ACTIVITY_OWN"},
	# Processus d'unité (D-87, docs/lot1/processus-unite.md §5.1) : définitions et instances, tout membre interne ;
	# droits fins (unité, pouvoirs D-81/D-86, intervenant, signataire, validateur) vérifiés par MS-PILOTAGE.
	# Candidats d'un intervenant (désignation hors de l'unité) : /processus/candidats et …/etapes/{code}/candidats.
	# D-98 : « processus que je peux lancer » (/processus/lancables) et « mes demandes » (/processus/demandes), même règle.
	# QUO-04 : pièce du formulaire de lancement ou livrable d'une instance (/processus/instances/{id}/documents/{doc}).
	# D-103 : public d'un brouillon ou d'une version publiée (PUT /processus/{id}/public), même permission que la
	# modification d'une définition ; qui définit les processus de l'unité est vérifié par MS-PILOTAGE.
	{"m": "GET", "re": `^/bff/des/pilotage/processus(/[^/]+|/instances/[^/]+(/etapes/[^/]+/candidats|/documents/[^/]+)?)?$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "POST", "re": `^/bff/des/pilotage/processus(/[^/]+/(controle|publication|nouvelle-version|retrait|instances))?$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "POST", "re": `^/bff/des/pilotage/processus/instances/[^/]+/(suspension|reprise|annulation)$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "POST", "re": `^/bff/des/pilotage/processus/instances/[^/]+/etapes/[^/]+/(livrables|signature|fin|validation|reaffectation)$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "PUT", "re": `^/bff/des/pilotage/processus/([^/]+|[^/]+/public|instances/[^/]+/etapes/[^/]+/champs)$`, "p": "PIL_ACTIVITY_OWN"},
	{"m": "DELETE", "re": `^/bff/des/pilotage/processus/[^/]+$`, "p": "PIL_ACTIVITY_OWN"},
	# Aperçu des documents bureautiques (D-83, docs/lot1/apercu-bureautique.md §2) : conversion en PDF par
	# MS-DOCUMENT, tout membre interne (qui a déjà le contenu à afficher) ; jamais un compte d'institution.
	{"m": "POST", "re": `^/bff/(des|console)/document/apercus$`, "p": "PIL_ACTIVITY_OWN"},
	# Reprise de l'existant (D-107, docs/exploitation/reprise-des-donnees.md) : super administrateur, depuis la console
	# (route dédiée) ou le département (route générale du service) ; les services revérifient REF_REPRISE_IMPORT.
	{"m": "POST", "re": `^/bff/(des|console)/participant/reprise/institutions$`, "p": "REF_REPRISE_IMPORT"},
	{"m": "POST", "re": `^/bff/(des|console)/homologation/reprise/liaisons$`, "p": "REF_REPRISE_IMPORT"},
	# Supervision des liaisons, astreintes et relève de quart (D-114, docs/lot1/astreintes-monitoring.md §8) : lecture par
	# MON_SUPERVISION_READ ; acquittement, main courante, signatures, visa, grille et échanges par leur permission ; la
	# contresignature (opérateur ou chef de la vacation entrante) et les réponses aux échanges sont revérifiées par
	# MS-MONITORING (@PreAuthorize, membre de la vacation, titulaire de la plage).
	{"m": "GET", "re": `^/bff/des/monitoring/(liaisons(/[^/]+)?|alertes(/[^/]+)?|politiques-alerte|vacations/en-cours|vacations/[^/]+(/main-courante(/verification)?|/rapport)?|rapports/[^/]+|grilles(/publiee|/[^/]+(/echanges)?)?)$`, "p": "MON_SUPERVISION_READ"},
	{"m": "POST", "re": `^/bff/des/monitoring/alertes/[^/]+/acquittement$`, "p": "MON_ALERT_ACK"},
	{"m": "POST", "re": `^/bff/des/monitoring/(vacations/[^/]+/(main-courante|rapport)|rapports/[^/]+/signature)$`, "p": "MON_SHIFT_OPERATOR"},
	{"m": "POST", "re": `^/bff/des/monitoring/rapports/[^/]+/contresignature$`, "p": "MON_SUPERVISION_READ"},
	{"m": "POST", "re": `^/bff/des/monitoring/rapports/[^/]+/visa$`, "p": "MON_SHIFT_LEAD"},
	{"m": "POST", "re": `^/bff/des/monitoring/(grilles/generation|echanges/[^/]+/(approbation|rejet))$`, "p": "MON_ROSTER_MANAGE"},
	{"m": "POST", "re": `^/bff/des/monitoring/grilles/[^/]+/publication$`, "p": "MON_ROSTER_PUBLISH"},
	{"m": "POST", "re": `^/bff/des/monitoring/(grilles/[^/]+/echanges|echanges/[^/]+/(accord|refus))$`, "p": "MON_SUPERVISION_READ"},
	# Recentrage D-116 (docs/lot1/astreintes-monitoring.md §10) : rapports rattachés au programme, rapport quotidien de 8 h,
	# activité de l'équipe et contraintes de génération ; MS-MONITORING borne la lecture (auteur, destinataires, chefs).
	{"m": "GET", "re": `^/bff/des/monitoring/(rapports-monitoring(/quotidiens|/[^/]+(/fichier)?)?|activite|grilles/contraintes)$`, "p": "MON_SUPERVISION_READ"},
	{"m": "POST", "re": `^/bff/des/monitoring/rapports-monitoring(/[^/]+/envoi)?$`, "p": "MON_SUPERVISION_READ"},
	{"m": "PUT", "re": `^/bff/des/monitoring/grilles/contraintes$`, "p": "MON_ROSTER_MANAGE"},
	# Programme configurable (D-117, docs/lot1/astreintes-monitoring.md §11) : modèle des créneaux lu par tous les
	# lecteurs du module (route GET /grilles/{id} ci-dessus), modifié par la gestion du programme ; téléchargement.
	{"m": "PUT", "re": `^/bff/des/monitoring/grilles/creneaux$`, "p": "MON_ROSTER_MANAGE"},
	{"m": "GET", "re": `^/bff/des/monitoring/grilles/[^/]+/export$`, "p": "MON_SUPERVISION_READ"},
]

# Routes internes des services (jeton de service) : jamais relayées depuis une couche de présentation.
interne if regex.match(`^/bff/[^/]+/[^/]+/interne(/|$)`, input.path)

# Messagerie (D-64) : seuls les chemins listés dans « regles » sont relayés ; tout autre chemin ou
# méthode sous /bff/{des,console}/collaboration/messagerie est refusé (pas de repli sur @PreAuthorize).
messagerie if regex.match(`^/bff/(des|console)/collaboration/messagerie(/|$)`, input.path)

# Les correspondants des institutions (jeton portant une institution) n'ont jamais accès à la messagerie.
compte_externe if input.institution_id != ""

messagerie_externe if {
	messagerie
	compte_externe
}

# Mon activité et organisation (D-76) : comme la messagerie, seuls les chemins listés dans « regles »
# sont relayés, et jamais pour un compte d'institution.
pilotage if regex.match(`^/bff/des/pilotage(/|$)`, input.path)

organisation if regex.match(`^/bff/(des|console)/iam/(organisation|pouvoirs|demandes)(/|$)`, input.path)

# Supervision et astreintes (D-114) : comme Mon activité, seuls les chemins listés dans « regles » sont relayés, et
# jamais pour un compte d'institution.
monitoring if regex.match(`^/bff/des/monitoring(/|$)`, input.path)

# « Mes actions de certification » (D-98, docs/lot1/lancement-processus.md §8) : GET des opérations de l'appelant,
# ouvert à toute permission d'un acteur du parcours de certification (même liste que le @PreAuthorize du service et
# que la fonctionnalité « mesActionsCertification » du catalogue), jamais à un compte d'institution.
mes_operations if {
	input.method == "GET"
	input.path == "/bff/des/homologation/parcours/mes-operations"
}

permissions_mes_operations := {
	"HOM_LIAISON_READ", "HOM_DASHBOARD_READ", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_STEP_MANAGE",
	"HOM_OPERATION_ASSIGN", "HOM_PROJECT_CONDUCT", "HOM_FUNCTION_DDRM", "HOM_FUNCTION_PRODUCTION", "HOM_FUNCTION_NETWORK",
	"HOM_FUNCTION_SYSTEM", "HOM_FUNCTION_OPERATIONS", "CER_PV_ESTABLISH", "CER_QUITUS_TECHNIQUE_SIGN", "CER_DOSSIER_VERIFY",
	"CER_SECURITY_VERIFY", "CER_CHANGE_RECORD_LINK", "CER_QUITUS_CONFORMITE_SIGN", "CER_QUITUS_DG_SIGN",
}

boite_notifications if regex.match(`^/bff/(des|console|portail)/notification(/|$)`, input.path)

reserve_interne_externe if {
	boite_notifications
	compte_externe
}

reserve_interne_externe if {
	pilotage
	compte_externe
}

reserve_interne_externe if {
	organisation
	compte_externe
}

reserve_interne_externe if {
	monitoring
	compte_externe
}

# Aperçu bureautique (D-83) : seule la route listée dans « regles » est relayée, et jamais pour un compte
# d'institution ; le portail n'y a pas accès (aucune règle).
apercus if regex.match(`^/bff/(des|console|portail)/document/apercus(/|$)`, input.path)

reserve_interne_externe if {
	apercus
	compte_externe
}

# La collaboration est réservée aux acteurs internes : aucun chemin du portail n'est relayé.
collaboration_portail if regex.match(`^/bff/portail/collaboration(/|$)`, input.path)

# « Mon compte » et défi de réauthentification (D-62) : tout utilisateur à deux facteurs, depuis
# chacune des trois couches ; MS-IAM n'y expose que le compte du titulaire du jeton.
libre_service if regex.match(`^/bff/(des|portail|console)/iam/(moi(/.*)?|reauthentification/defi)$`, input.path)

# Photos de profil des collègues (docs/lot1/mon-compte.md §1) : DES et console, lecture seule, tout
# utilisateur à deux facteurs ; le portail n'y a pas accès (seulement à la sienne, sous /iam/moi/avatar).
avatars if regex.match(`^/bff/(des|console)/iam/avatars(/|$)`, input.path)

# Catalogue des fonctionnalités (D-94) : lu par l'application interne et la console pour construire la barre
# latérale, les gardes et les tableaux de bord ; tout compte interne à deux facteurs, jamais un compte
# d'institution ni le portail.
catalogue_fonctionnalites if regex.match(`^/bff/(des|console)/iam/configuration/fonctionnalites(/|$)`, input.path)

lecture_catalogue_fonctionnalites if {
	input.method == "GET"
	regex.match(`^/bff/(des|console)/iam/configuration/fonctionnalites$`, input.path)
}

avatar_collegue if {
	input.method == "GET"
	regex.match(`^/bff/(des|console)/iam/avatars/[^/]+$`, input.path)
}

# Dépôt d'une pièce depuis le DES : l'une des permissions d'instruction ou de soumission.
permissions_depot_document := {"PAR_ADHESION_INSTRUCT", "HOM_EVIDENCE_SUBMIT", "HOM_STEP_MANAGE", "CER_CHANGE_RECORD_LINK", "DOC_DEPOSIT"}

depot_document_des if {
	input.method == "POST"
	input.path == "/bff/des/document/documents"
}

# Dépôts fragmentés (D-74, docs/lot1/depots-fragmentes.md §3) : seuls les chemins et méthodes du contrat
# sont relayés ; tout autre chemin ou méthode sous …/document/depots est refusé (pas de repli sur
# @PreAuthorize). Portail : règles de « regles » (HOM_PORTAL_SUBMIT et institution).
depots_fragmentes if regex.match(`^/bff/(des|console|portail)/document/depots(/|$)`, input.path)

# DES et console : l'une des permissions du dépôt direct, COL_MESSAGE_SEND (pièces jointes de la
# messagerie) ou PIL_ACTIVITY_OWN (rapports d'activité déposés, D-76 ; livrables des processus d'unité,
# finalité PROCESSUS_UNITE, D-87) ; MS-DOCUMENT exige ensuite la permission propre à la finalité.
permissions_depot_fragmente := permissions_depot_document | {"COL_MESSAGE_SEND", "PIL_ACTIVITY_OWN"}

route_depot_fragmente if {
	input.method == "POST"
	regex.match(`^/bff/(des|console)/document/depots(/[^/]+/fin)?$`, input.path)
}

route_depot_fragmente if {
	input.method == "PUT"
	regex.match(`^/bff/(des|console)/document/depots/[^/]+/parties/[^/]+$`, input.path)
}

route_depot_fragmente if {
	input.method in {"GET", "DELETE"}
	regex.match(`^/bff/(des|console)/document/depots/[^/]+$`, input.path)
}

# Affectation de personnes à une unité depuis le département (D-102, docs/lot1/organigramme.md §2) :
# administration des comptes (IAM_ACCOUNT_EDIT) ou chef (PIL_ACTIVITY_OWN, périmètre vérifié par MS-IAM) ;
# jamais un compte d'institution.
affectation_unite if {
	input.method == "POST"
	regex.match(`^/bff/des/iam/organisation/unites/[^/]+/affectations$`, input.path)
}

permissions_affectation_unite := {"IAM_ACCOUNT_EDIT", "PIL_ACTIVITY_OWN"}

# Session restreinte du premier accès (D-65, docs/lot1/connexion.md §2), transmise par la passerelle.
restreinte if input.restreinte == true

# Assistant de premier accès : GET|POST /bff/{couche}/iam/moi, …/premiere-configuration,
# …/mot-de-passe et …/second-facteur/**.
chemin_premier_acces if regex.match(
	`^/bff/(des|portail|console)/iam/moi(/premiere-configuration|/mot-de-passe|/second-facteur(/.*)?)?$`,
	input.path,
)

premier_acces if {
	restreinte
	input.authentifie == true
	input.acr == "1"
	input.method in {"GET", "POST"}
	chemin_premier_acces
}

allow if premier_acces

# Second facteur (D-61) : niveaux d'assurance acceptés.
niveaux_double_facteur := {"2", "3"}

double_facteur if input.acr in niveaux_double_facteur

# Administration des référentiels métier, tous domaines (D-92, docs/lot1/referentiels-metier-admin.md) :
# REF_METIER_ADMIN vaut chacune des permissions du référentiel des opérations, des étapes, des types de projet
# et des fonctions.
permissions_equivalentes := {
	"HOM_REFERENTIEL_READ": {"REF_METIER_ADMIN"},
	"HOM_REFERENTIEL_EDIT": {"REF_METIER_ADMIN"},
	"HOM_REFERENTIEL_PUBLISH": {"REF_METIER_ADMIN"},
	"HOM_REFERENTIEL_MIGRATE": {"REF_METIER_ADMIN"},
}

# La permission exigée, ou une permission qui la vaut.
permission_accordee if required_permission in input.permissions

permission_accordee if {
	some p in input.permissions
	p in permissions_equivalentes[required_permission]
}

# Permission exigée par la première règle correspondant à la méthode et au chemin.
correspondances := [i |
	some i, regle in regles
	regle.m == input.method
	regex.match(regle.re, input.path)
]

required_permission := regles[min(correspondances)].p if count(correspondances) > 0

portail if startswith(input.path, "/bff/portail/")

# Chemins du DES et de la console : la permission suffit (messagerie : comptes internes seulement).
allow if {
	not restreinte
	not interne
	not portail
	not messagerie_externe
	not reserve_interne_externe
	double_facteur
	permission_accordee
}

# Chemins du portail : permission et institution dans le jeton.
allow if {
	not restreinte
	not interne
	portail
	not collaboration_portail
	double_facteur
	required_permission in input.permissions
	input.institution_id != ""
}

# Chemins du lot non listés : l'accès est laissé au contrôle @PreAuthorize du service,
# à condition d'être authentifié (liste d'autorisations explicites du plan, extrait).
allow if {
	not restreinte
	not interne
	not required_permission
	not portail
	not messagerie
	not pilotage
	not monitoring
	not organisation
	not boite_notifications
	not avatars
	not catalogue_fonctionnalites
	not depots_fragmentes
	not apercus
	not mes_operations

	# D-120 : /bff/des/sbx-espace n'est jamais ouvert par cette règle de repli (règles de la sandbox seulement).
	not sbx_chemin
	startswith(input.path, "/bff/des/")
	input.authentifie == true
	double_facteur
}

allow if {
	not restreinte
	not interne
	libre_service
	input.authentifie == true
	double_facteur
}

allow if {
	not restreinte
	depot_document_des
	double_facteur
	some p in input.permissions
	p in permissions_depot_document
}

allow if {
	not restreinte
	route_depot_fragmente
	double_facteur
	some p in input.permissions
	p in permissions_depot_fragmente
}

allow if {
	not restreinte
	affectation_unite
	not compte_externe
	double_facteur
	some p in input.permissions
	p in permissions_affectation_unite
}

allow if {
	not restreinte
	lecture_catalogue_fonctionnalites
	not compte_externe
	input.authentifie == true
	double_facteur
}

allow if {
	not restreinte
	avatar_collegue
	input.authentifie == true
	double_facteur
}

allow if {
	not restreinte
	mes_operations
	not compte_externe
	input.authentifie == true
	double_facteur
	some p in input.permissions
	p in permissions_mes_operations
}

# ---------------------------------------------------------------------------------------------------------------------
# D-120 (docs/lot1/sandbox-existant-adapte.md §6, US-EXI-07) : routes de la Sandbox GIMAC.
# /bff/console/sbx-<service>/** et /bff/portail/sbx-<service>/** des onze services ; /bff/des/sbx-espace/** seul dans
# le département (D-118). Lecture (GET, HEAD, OPTIONS) : SBX_<MODULE>_CONSULTATION. Écriture : une fonction d'action du
# module (ADMINISTRATION, SIMULATION, EXECUTION, VALIDATION), ADMINISTRATION seule pour sbx-espace
# (SBX_HAB_ADMINISTRATION) ; le service revérifie la fonction exacte du geste. Portail : institution exigée ; console
# et département : comptes du GIMAC seulement. Le cloisonnement par mandat et le quota sont appliqués ensuite par le
# filtre CloisonnementSandbox de la passerelle.
sbx_modules := {
	"espace": "HAB",
	"raccordement": "RAC",
	"referentiel": "REF",
	"donnees": "DON",
	"simulation": "SIM",
	"mobile": "MOB",
	"iso20022": "ISO",
	"carte": "CAR",
	"campagne": "CER",
	"trace": "TRA",
	"lecture": "LEC",
}

# Tout chemin /bff/des/sbx-* (service connu ou non) : jamais ouvert par la règle de repli du département.
sbx_chemin if regex.match(`^/bff/des/sbx-`, input.path)

sbx_route := {"couche": m[1], "service": m[2]} if {
	m := regex.find_all_string_submatch_n(`^/bff/(portail|console|des)/sbx-([a-z0-9]+)(/.*)?$`, input.path, 1)[0]
	sbx_modules[m[2]]
}

sbx_lecture if input.method in {"GET", "HEAD", "OPTIONS"}

sbx_fonctions_ecriture := {"ADMINISTRATION", "SIMULATION", "EXECUTION", "VALIDATION"}

sbx_permissions contains sprintf("SBX_%s_CONSULTATION", [sbx_modules[sbx_route.service]]) if sbx_lecture

sbx_permissions contains sprintf("SBX_%s_%s", [sbx_modules[sbx_route.service], f]) if {
	not sbx_lecture
	sbx_route.service != "espace"
	some f in sbx_fonctions_ecriture
}

sbx_permissions contains "SBX_HAB_ADMINISTRATION" if {
	not sbx_lecture
	sbx_route.service == "espace"
}

sbx_couche_admise if {
	sbx_route.couche == "portail"
	compte_externe
}

sbx_couche_admise if {
	sbx_route.couche == "console"
	not compte_externe
}

sbx_couche_admise if {
	sbx_route.couche == "des"
	sbx_route.service == "espace"
	not compte_externe
}

allow if {
	not restreinte
	not interne
	sbx_route
	sbx_couche_admise
	double_facteur
	some p in input.permissions
	p in sbx_permissions
}

# D-120 (US-EXI-08) : types de pièces de la sandbox et quarantaines de MS-DOCUMENT, depuis la console, comptes du GIMAC
# seulement (un compte d'institution porte SBX_TRA_CONSULTATION pour ses propres traces, pas pour ce catalogue).
sbx_document_lecture if {
	input.method == "GET"
	regex.match(`^/bff/console/document/sandbox/(types-pieces|quarantaines)$`, input.path)
}

sbx_document_ecriture if {
	input.method == "PUT"
	regex.match(`^/bff/console/document/sandbox/types-pieces/[A-Z0-9_]+/retention$`, input.path)
}

allow if {
	not restreinte
	sbx_document_lecture
	not compte_externe
	double_facteur
	"SBX_TRA_CONSULTATION" in input.permissions
}

allow if {
	not restreinte
	sbx_document_ecriture
	not compte_externe
	double_facteur
	"SBX_TRA_ADMINISTRATION" in input.permissions
}
