# Politique de développement : refus par défaut (RG-24, confiance nulle).
# Les politiques réelles sont écrites par MS-IAM (IAM-19) et évaluées par MS-GATEWAY.
package gimac.autorisation

import rego.v1

default autorise := false

# Niveaux d'habilitation du CCT, du moins au plus privilégié (D-31).
niveaux := {
	"CONSULTATION": 1,
	"EXECUTION": 2,
	"APPROBATION": 3,
	"ADMINISTRATION_METIER": 4,
	"AUDIT_SECURITE": 5,
}

# Autorise si le niveau de l'utilisateur atteint le niveau requis par la route
# et, pour un correspondant externe, si l'institution demandée est la sienne (RG-59).
autorise if {
	niveaux[input.utilisateur.niveau] >= niveaux[input.route.niveau_requis]
	institution_permise
}

institution_permise if not input.utilisateur.institution

institution_permise if input.utilisateur.institution == input.ressource.institution
