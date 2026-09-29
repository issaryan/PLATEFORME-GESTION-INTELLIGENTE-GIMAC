# Politique d'accès du lot 1 (PI-GIMAC-01 §0.6), évaluée par la passerelle (filtre OpaAuthorize)
# puis doublée par @PreAuthorize dans chaque service. Refus par défaut (RG-24).
# Écart au plan : sa première règle allow acceptait aussi le portail sans institution ;
# ici, un chemin du portail n'est autorisé que si le jeton porte institution_id (RG-59).
# D-61 : toute route /bff exige un jeton issu d'une authentification à deux facteurs (acr 2 ou 3).
package gimac.authz

import rego.v1

default allow := false

regles := [
	{"m": "POST", "re": `^/bff/des/homologation/referentiels/[^/]+/publication$`, "p": "HOM_REFERENTIEL_PUBLISH"},
	{"m": "POST", "re": `^/bff/des/homologation/liaisons$`, "p": "HOM_LIAISON_OPEN"},
	{"m": "POST", "re": `^/bff/des/homologation/operations/[^/]+/validation$`, "p": "HOM_OPERATION_VALIDATE"},
	{"m": "POST", "re": `^/bff/des/homologation/operations/[^/]+/reouverture$`, "p": "HOM_OPERATION_REOPEN"},
	{"m": "POST", "re": `^/bff/des/certification/circuits/[^/]+/quitus/technique$`, "p": "CER_QUITUS_TECHNIQUE_SIGN"},
	{"m": "POST", "re": `^/bff/des/certification/circuits/[^/]+/quitus/securite$`, "p": "CER_QUITUS_SECURITE_SIGN"},
	{"m": "POST", "re": `^/bff/des/certification/circuits/[^/]+/quitus/conformite$`, "p": "CER_QUITUS_CONFORMITE_SIGN"},
	{"m": "POST", "re": `^/bff/des/certification/circuits/[^/]+/quitus/direction-generale$`, "p": "CER_QUITUS_DG_SIGN"},
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
]

# Routes internes des services (jeton de service) : jamais relayées depuis une couche de présentation.
interne if regex.match(`^/bff/[^/]+/[^/]+/interne(/|$)`, input.path)

# « Mon compte » et défi de réauthentification (D-62) : tout utilisateur à deux facteurs, depuis
# chacune des trois couches ; MS-IAM n'y expose que le compte du titulaire du jeton.
libre_service if regex.match(`^/bff/(des|portail|console)/iam/(moi(/.*)?|reauthentification/defi)$`, input.path)

# Dépôt d'une pièce depuis le DES : l'une des permissions d'instruction ou de soumission.
permissions_depot_document := {"PAR_ADHESION_INSTRUCT", "HOM_EVIDENCE_SUBMIT", "DOC_DEPOSIT"}

depot_document_des if {
	input.method == "POST"
	input.path == "/bff/des/document/documents"
}

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
	not interne
	not portail
	double_facteur
	required_permission in input.permissions
}

# Chemins du portail : permission et institution dans le jeton.
allow if {
	not interne
	portail
	double_facteur
	required_permission in input.permissions
	input.institution_id != ""
}

# Chemins du lot non listés : l'accès est laissé au contrôle @PreAuthorize du service,
# à condition d'être authentifié (liste d'autorisations explicites du plan, extrait).
allow if {
	not interne
	not required_permission
	not portail
	startswith(input.path, "/bff/des/")
	input.authentifie == true
	double_facteur
}

allow if {
	not interne
	libre_service
	input.authentifie == true
	double_facteur
}

allow if {
	depot_document_des
	double_facteur
	some p in input.permissions
	p in permissions_depot_document
}
