# Politique d'accès du lot 1 (PI-GIMAC-01 §0.6), évaluée par la passerelle (filtre OpaAuthorize)
# puis doublée par @PreAuthorize dans chaque service. Refus par défaut (RG-24).
# Écart au plan : sa première règle allow acceptait aussi le portail sans institution ;
# ici, un chemin du portail n'est autorisé que si le jeton porte institution_id (RG-59).
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
]

# Permission exigée par la première règle correspondant à la méthode et au chemin.
required_permission := p if {
	some regle in regles
	regle.m == input.method
	regex.match(regle.re, input.path)
	p := regle.p
}

portail if startswith(input.path, "/bff/portail/")

# Chemins du DES : la permission suffit.
allow if {
	not portail
	required_permission in input.permissions
}

# Chemins du portail : permission et institution dans le jeton.
allow if {
	portail
	required_permission in input.permissions
	input.institution_id != ""
}

# Chemins du lot non listés : l'accès est laissé au contrôle @PreAuthorize du service,
# à condition d'être authentifié (liste d'autorisations explicites du plan, extrait).
allow if {
	not required_permission
	not portail
	startswith(input.path, "/bff/des/")
	input.authentifie == true
}
