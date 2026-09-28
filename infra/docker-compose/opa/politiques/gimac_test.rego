package gimac.autorisation_test

import data.gimac.autorisation
import rego.v1

test_refus_par_defaut if not autorisation.autorise with input as {}

test_niveau_suffisant if autorisation.autorise with input as {
	"utilisateur": {"niveau": "APPROBATION"},
	"route": {"niveau_requis": "EXECUTION"},
}

test_niveau_insuffisant if not autorisation.autorise with input as {
	"utilisateur": {"niveau": "CONSULTATION"},
	"route": {"niveau_requis": "APPROBATION"},
}

test_correspondant_autre_institution if not autorisation.autorise with input as {
	"utilisateur": {"niveau": "EXECUTION", "institution": "INST-0042"},
	"route": {"niveau_requis": "CONSULTATION"},
	"ressource": {"institution": "INST-0043"},
}
