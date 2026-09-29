package gimac.authz_test

import data.gimac.authz
import rego.v1

test_refus_par_defaut if not authz.allow with input as {}

test_publication_avec_permission if authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/referentiels/v1/publication",
	"permissions": ["HOM_REFERENTIEL_PUBLISH"], "authentifie": true,
}

test_publication_sans_permission if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/referentiels/v1/publication",
	"permissions": ["HOM_REFERENTIEL_READ"], "authentifie": true,
}

test_portail_sans_institution_refuse if not authz.allow with input as {
	"method": "GET", "path": "/bff/portail/homologation/liaisons",
	"permissions": ["HOM_PORTAL_READ"], "institution_id": "", "authentifie": true,
}

test_portail_avec_institution if authz.allow with input as {
	"method": "GET", "path": "/bff/portail/homologation/liaisons",
	"permissions": ["HOM_PORTAL_READ"], "institution_id": "INST-0042", "authentifie": true,
}

test_lecture_des_authentifiee if authz.allow with input as {
	"method": "GET", "path": "/bff/des/homologation/liaisons", "permissions": [], "authentifie": true,
}

test_lecture_des_anonyme_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/homologation/liaisons", "permissions": [], "authentifie": false,
}
