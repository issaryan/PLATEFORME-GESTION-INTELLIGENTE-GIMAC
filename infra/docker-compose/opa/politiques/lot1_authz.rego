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
	{"m": "POST", "re": `^/bff/des/homologation/referentiels/[^/]+/publication$`, "p": "HOM_REFERENTIEL_PUBLISH"},
	{"m": "POST", "re": `^/bff/des/homologation/liaisons$`, "p": "HOM_LIAISON_OPEN"},
	# Cadrage (étape 3, D-58) : désignation du conducteur de projet par un chef de département.
	{"m": "PUT", "re": `^/bff/des/homologation/liaisons/[^/]+/chef-de-projet$`, "p": "HOM_PROJECT_DESIGNATE"},
	{"m": "POST", "re": `^/bff/des/homologation/operations/[^/]+/validation$`, "p": "HOM_OPERATION_VALIDATE"},
	{"m": "POST", "re": `^/bff/des/homologation/operations/[^/]+/reouverture$`, "p": "HOM_OPERATION_REOPEN"},
	# Parcours, étapes 4 à 10 (docs/lot1/parcours-backend.md §1) : gestes à permission unique. Les gestes
	# ouverts à plusieurs permissions (livrable, intervenants, services supplémentaires) restent au @PreAuthorize.
	{"m": "POST", "re": `^/bff/des/homologation/parcours/operations/[^/]+/validation$`, "p": "HOM_OPERATION_VALIDATE"},
	{"m": "POST", "re": `^/bff/des/homologation/parcours/operations/[^/]+/preuves$`, "p": "HOM_EVIDENCE_SUBMIT"},
	{"m": "PUT", "re": `^/bff/des/homologation/liaisons/[^/]+/ressources$`, "p": "HOM_STEP_MANAGE"},
	{"m": "POST", "re": `^/bff/des/homologation/(liaisons/[^/]+/blocages|blocages/[^/]+/levee|liaisons/[^/]+/fiche-suivi/remise)$`, "p": "HOM_STEP_MANAGE"},
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
	# Console d'administration (D-62) : comptes, journal, politique de sécurité, messagerie, profils.
	# La première règle correspondante s'applique : les chemins les plus précis viennent d'abord.
	{"m": "GET", "re": `^/bff/console/iam/comptes/journal$`, "p": "IAM_JOURNAL_READ"},
	{"m": "GET", "re": `^/bff/console/iam/comptes(/.*)?$`, "p": "IAM_ACCOUNT_READ"},
	{"m": "POST", "re": `^/bff/console/iam/comptes$`, "p": "IAM_ACCOUNT_CREATE"},
	{"m": "PUT", "re": `^/bff/console/iam/comptes/[^/]+/profils$`, "p": "IAM_ACCOUNT_ROLES"},
	{"m": "PUT", "re": `^/bff/console/iam/comptes/[^/]+$`, "p": "IAM_ACCOUNT_EDIT"},
	{"m": "POST", "re": `^/bff/console/iam/comptes/[^/]+/(activation|desactivation)$`, "p": "IAM_ACCOUNT_STATUS"},
	{"m": "DELETE", "re": `^/bff/console/iam/comptes/[^/]+$`, "p": "IAM_ACCOUNT_DELETE"},
	{"m": "POST", "re": `^/bff/console/iam/comptes/[^/]+/(actions-requises|second-facteur/reinitialisation|mot-de-passe-provisoire|sessions/fermeture)$`, "p": "IAM_ACCOUNT_SECURITY"},
	{"m": "GET", "re": `^/bff/console/iam/configuration/politique-securite$`, "p": "PLATFORM_SECURITY_POLICY_READ"},
	{"m": "PUT", "re": `^/bff/console/iam/configuration/politique-securite$`, "p": "PLATFORM_SECURITY_POLICY_EDIT"},
	{"m": "GET", "re": `^/bff/console/iam/configuration/messagerie$`, "p": "PLATFORM_MESSAGING_EDIT"},
	{"m": "PUT", "re": `^/bff/console/iam/configuration/messagerie$`, "p": "PLATFORM_MESSAGING_EDIT"},
	{"m": "POST", "re": `^/bff/console/iam/configuration/messagerie/test$`, "p": "PLATFORM_MESSAGING_EDIT"},
	{"m": "GET", "re": `^/bff/console/iam/configuration/(profils|permissions)$`, "p": "IAM_ACCOUNT_READ"},
	{"m": "PUT", "re": `^/bff/console/iam/configuration/profils/[^/]+/permissions$`, "p": "IAM_PROFILE_EDIT"},
	# Messagerie instantanée de travail (D-64, docs/lot1/messagerie.md §1) : DES seulement.
	{"m": "GET", "re": `^/bff/des/collaboration/messagerie/(annuaire|non-lus|conversations(/[^/]+(/messages)?)?|pieces/[^/]+)$`, "p": "COL_MESSAGE_READ"},
	{"m": "POST", "re": `^/bff/des/collaboration/messagerie/conversations/[^/]+/lu$`, "p": "COL_MESSAGE_READ"},
	{"m": "POST", "re": `^/bff/des/collaboration/messagerie/conversations(/[^/]+/messages)?$`, "p": "COL_MESSAGE_SEND"},
]

# Routes internes des services (jeton de service) : jamais relayées depuis une couche de présentation.
interne if regex.match(`^/bff/[^/]+/[^/]+/interne(/|$)`, input.path)

# Messagerie (D-64) : seuls les chemins listés dans « regles » sont relayés ; tout autre chemin ou
# méthode sous /bff/des/collaboration/messagerie est refusé (pas de repli sur @PreAuthorize).
messagerie if regex.match(`^/bff/des/collaboration/messagerie(/|$)`, input.path)

# La collaboration est réservée aux acteurs internes : aucun chemin du portail n'est relayé.
collaboration_portail if regex.match(`^/bff/portail/collaboration(/|$)`, input.path)

# « Mon compte » et défi de réauthentification (D-62) : tout utilisateur à deux facteurs, depuis
# chacune des trois couches ; MS-IAM n'y expose que le compte du titulaire du jeton.
libre_service if regex.match(`^/bff/(des|portail|console)/iam/(moi(/.*)?|reauthentification/defi)$`, input.path)

# Photos de profil des collègues (docs/lot1/mon-compte.md §1) : DES et console, lecture seule, tout
# utilisateur à deux facteurs ; le portail n'y a pas accès (seulement à la sienne, sous /iam/moi/avatar).
avatars if regex.match(`^/bff/(des|console)/iam/avatars(/|$)`, input.path)

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

# Permission exigée par la première règle correspondant à la méthode et au chemin.
correspondances := [i |
	some i, regle in regles
	regle.m == input.method
	regex.match(regle.re, input.path)
]

required_permission := regles[min(correspondances)].p if count(correspondances) > 0

portail if startswith(input.path, "/bff/portail/")

# Chemins du DES et de la console : la permission suffit.
allow if {
	not restreinte
	not interne
	not portail
	double_facteur
	required_permission in input.permissions
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
	not avatars
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
	avatar_collegue
	input.authentifie == true
	double_facteur
}
