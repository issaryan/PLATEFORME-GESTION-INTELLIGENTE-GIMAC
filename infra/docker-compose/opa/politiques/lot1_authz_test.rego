package gimac.authz_test

import data.gimac.authz
import rego.v1

test_refus_par_defaut if not authz.allow with input as {}

test_publication_avec_permission if authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/referentiels/v1/publication",
	"permissions": ["HOM_REFERENTIEL_PUBLISH"], "authentifie": true, "acr": "2",
}

test_publication_sans_permission if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/referentiels/v1/publication",
	"permissions": ["HOM_REFERENTIEL_READ"], "authentifie": true, "acr": "2",
}

test_portail_sans_institution_refuse if not authz.allow with input as {
	"method": "GET", "path": "/bff/portail/homologation/liaisons",
	"permissions": ["HOM_PORTAL_READ"], "institution_id": "", "authentifie": true, "acr": "2",
}

test_portail_avec_institution if authz.allow with input as {
	"method": "GET", "path": "/bff/portail/homologation/liaisons",
	"permissions": ["HOM_PORTAL_READ"], "institution_id": "INST-0042", "authentifie": true, "acr": "2",
}

test_lecture_des_authentifiee if authz.allow with input as {"method": "GET", "path": "/bff/des/homologation/liaisons", "permissions": [], "authentifie": true, "acr": "2"}

test_lecture_des_anonyme_refusee if not authz.allow with input as {"method": "GET", "path": "/bff/des/homologation/liaisons", "permissions": [], "authentifie": false}

# D-61 : second facteur exigé sur toutes les routes /bff.
test_permission_sans_second_facteur_refusee if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/referentiels/v1/publication",
	"permissions": ["HOM_REFERENTIEL_PUBLISH"], "authentifie": true, "acr": "1",
}

test_portail_sans_second_facteur_refuse if not authz.allow with input as {
	"method": "GET", "path": "/bff/portail/homologation/liaisons",
	"permissions": ["HOM_PORTAL_READ"], "institution_id": "INST-0042", "authentifie": true,
}

test_lecture_des_sans_acr_refusee if not authz.allow with input as {"method": "GET", "path": "/bff/des/pilotage/projets", "permissions": [], "authentifie": true, "acr": ""}

# Registre des institutions (D-57) : chaque route et sa permission.
routes_participant := [
	["GET", "/bff/des/participant/institutions", "PAR_INSTITUTION_READ"],
	["GET", "/bff/des/participant/institutions/indicateurs", "PAR_INSTITUTION_READ"],
	["GET", "/bff/des/participant/institutions/groupes", "PAR_INSTITUTION_READ"],
	["GET", "/bff/des/participant/institutions/i1/prerequis", "PAR_INSTITUTION_READ"],
	["POST", "/bff/des/participant/institutions", "PAR_INSTITUTION_CREATE"],
	["PUT", "/bff/des/participant/institutions/i1", "PAR_INSTITUTION_EDIT"],
	["PUT", "/bff/des/participant/institutions/i1/conditions-adhesion", "PAR_INSTITUTION_EDIT"],
	["POST", "/bff/des/participant/institutions/i1/pieces/STATUTS/depot", "PAR_ADHESION_INSTRUCT"],
	["POST", "/bff/des/participant/institutions/i1/pieces/STATUTS/validation", "PAR_ADHESION_INSTRUCT"],
	["POST", "/bff/des/participant/institutions/i1/pieces/STATUTS/rejet", "PAR_ADHESION_INSTRUCT"],
	["POST", "/bff/des/participant/institutions/i1/transitions", "PAR_TRANSITION_REQUEST"],
	["POST", "/bff/des/participant/institutions/i1/transitions/t1/approbation", "PAR_TRANSITION_APPROVE"],
	["POST", "/bff/des/participant/institutions/i1/transitions/t1/rejet", "PAR_TRANSITION_APPROVE"],
]

test_participant_avec_permission if {
	every r in routes_participant {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "2"}
	}
}

test_participant_sans_permission if {
	every r in routes_participant {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "2"}
	}
}

test_participant_sans_second_facteur if {
	every r in routes_participant {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "1"}
	}
}

test_chef_ddrm_ne_cree_pas_sans_permission if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/participant/institutions/i1/transitions/t1/approbation",
	"permissions": ["PAR_TRANSITION_REQUEST"], "authentifie": true, "acr": "2",
}

# D-62 : console d'administration, « Mon compte », routes internes, dépôt de pièces.
test_console_comptes_avec_permission if authz.allow with input as {"method": "GET", "path": "/bff/console/iam/comptes", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}

test_console_journal_exige_sa_permission if not authz.allow with input as {"method": "GET", "path": "/bff/console/iam/comptes/journal", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}

test_console_suppression_exige_sa_permission if not authz.allow with input as {"method": "DELETE", "path": "/bff/console/iam/comptes/c1", "permissions": ["IAM_ACCOUNT_EDIT"], "authentifie": true, "acr": "2"}

test_console_suppression_avec_permission if authz.allow with input as {"method": "DELETE", "path": "/bff/console/iam/comptes/c1", "permissions": ["IAM_ACCOUNT_DELETE"], "authentifie": true, "acr": "2"}

test_console_profils_exige_roles if not authz.allow with input as {"method": "PUT", "path": "/bff/console/iam/comptes/c1/profils", "permissions": ["IAM_ACCOUNT_EDIT"], "authentifie": true, "acr": "2"}

test_console_sans_permission_refusee if not authz.allow with input as {"method": "GET", "path": "/bff/console/iam/comptes", "permissions": [], "authentifie": true, "acr": "2"}

# Administration dans le département (D-69) : mêmes permissions que dans la console ; les courriels restent à la console.
test_departement_comptes_avec_permission if authz.allow with input as {"method": "GET", "path": "/bff/des/iam/comptes", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}

test_departement_comptes_sans_permission_refusee if not authz.allow with input as {"method": "GET", "path": "/bff/des/iam/comptes", "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "2"}

test_departement_suppression_exige_sa_permission if not authz.allow with input as {"method": "DELETE", "path": "/bff/des/iam/comptes/c1", "permissions": ["IAM_ACCOUNT_EDIT"], "authentifie": true, "acr": "2"}

test_departement_politique_et_profils if {
	authz.allow with input as {"method": "PUT", "path": "/bff/des/iam/configuration/politique-securite", "permissions": ["PLATFORM_SECURITY_POLICY_EDIT"], "authentifie": true, "acr": "2"}
	authz.allow with input as {"method": "GET", "path": "/bff/des/iam/configuration/profils", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "PUT", "path": "/bff/des/iam/configuration/profils/ROLE_SNA/permissions", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "GET", "path": "/bff/des/iam/comptes", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "1"}
}

test_console_non_listee_refusee if not authz.allow with input as {"method": "GET", "path": "/bff/console/iam/autre", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}

test_console_politique_sans_second_facteur_refusee if not authz.allow with input as {
	"method": "PUT", "path": "/bff/console/iam/configuration/politique-securite", "permissions": ["PLATFORM_SECURITY_POLICY_EDIT"],
	"authentifie": true, "acr": "1",
}

test_mon_compte_depuis_le_portail if authz.allow with input as {
	"method": "POST", "path": "/bff/portail/iam/moi/premiere-configuration", "permissions": [], "institution_id": "",
	"authentifie": true, "acr": "2",
}

test_mon_compte_sans_second_facteur_refuse if not authz.allow with input as {"method": "GET", "path": "/bff/des/iam/moi", "permissions": [], "authentifie": true, "acr": "1"}

test_defi_reauthentification if authz.allow with input as {"method": "POST", "path": "/bff/console/iam/reauthentification/defi", "permissions": [], "authentifie": true, "acr": "2"}

test_route_interne_jamais_relayee if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/iam/interne/reauthentifications/verification",
	"permissions": ["INTERNAL_IAM_READ"], "authentifie": true, "acr": "2",
}

test_depot_des_avec_permission if authz.allow with input as {"method": "POST", "path": "/bff/des/document/documents", "permissions": ["PAR_ADHESION_INSTRUCT"], "authentifie": true, "acr": "2"}

test_depot_des_sans_second_facteur if not authz.allow with input as {"method": "POST", "path": "/bff/des/document/documents", "permissions": ["PAR_ADHESION_INSTRUCT"], "authentifie": true, "acr": "1"}

test_depot_portail_exige_institution if not authz.allow with input as {
	"method": "POST", "path": "/bff/portail/document/documents", "permissions": ["HOM_PORTAL_SUBMIT"], "institution_id": "",
	"authentifie": true, "acr": "2",
}

test_depot_portail if authz.allow with input as {
	"method": "POST", "path": "/bff/portail/document/documents", "permissions": ["HOM_PORTAL_SUBMIT"], "institution_id": "INST-0042",
	"authentifie": true, "acr": "2",
}

# Étape 3 : ouverture de la liaison et désignation du conducteur de projet (D-58).
test_ouverture_liaison_chef_de_departement if authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/liaisons", "permissions": ["HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE"],
	"authentifie": true, "acr": "2",
}

test_designation_conducteur_exige_sa_permission if not authz.allow with input as {
	"method": "PUT", "path": "/bff/des/homologation/liaisons/l1/chef-de-projet", "permissions": ["HOM_LIAISON_OPEN"],
	"authentifie": true, "acr": "2",
}

test_designation_conducteur if authz.allow with input as {
	"method": "PUT", "path": "/bff/des/homologation/liaisons/l1/chef-de-projet", "permissions": ["HOM_PROJECT_DESIGNATE"],
	"authentifie": true, "acr": "2",
}

# D-64 : messagerie instantanée de travail (docs/lot1/messagerie.md §1), tout membre interne (DES et console).
routes_messagerie := [
	["GET", "/bff/des/collaboration/messagerie/annuaire", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/conversations", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/conversations/c1", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/conversations/c1/messages", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/non-lus", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/pieces/p1", "COL_MESSAGE_READ"],
	["POST", "/bff/des/collaboration/messagerie/conversations/c1/lu", "COL_MESSAGE_READ"],
	["POST", "/bff/des/collaboration/messagerie/conversations/c1/saisie", "COL_MESSAGE_SEND"],
	["POST", "/bff/des/collaboration/messagerie/conversations", "COL_MESSAGE_SEND"],
	["POST", "/bff/des/collaboration/messagerie/conversations/c1/messages", "COL_MESSAGE_SEND"],
	["PATCH", "/bff/des/collaboration/messagerie/conversations/c1", "COL_MESSAGE_SEND"],
	["POST", "/bff/des/collaboration/messagerie/conversations/c1/membres", "COL_MESSAGE_SEND"],
	["DELETE", "/bff/des/collaboration/messagerie/conversations/c1/membres/u1", "COL_MESSAGE_SEND"],
]

test_messagerie_console_avec_permission if {
	every r in routes_messagerie {
		authz.allow with input as {
			"method": r[0], "path": replace(r[1], "/bff/des/", "/bff/console/"), "permissions": [r[2]],
			"authentifie": true, "acr": "2",
		}
	}
}

test_messagerie_console_sans_permission if {
	every r in routes_messagerie {
		not authz.allow with input as {
			"method": r[0], "path": replace(r[1], "/bff/des/", "/bff/console/"), "permissions": ["IAM_ACCOUNT_READ"],
			"authentifie": true, "acr": "2",
		}
	}
}

test_messagerie_compte_externe_refuse if {
	every r in routes_messagerie {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": [r[2]], "institution_id": "INST-0042",
			"authentifie": true, "acr": "2",
		}
	}
}

test_messagerie_compte_interne_institution_vide if authz.allow with input as {
	"method": "GET", "path": "/bff/des/collaboration/messagerie/non-lus", "permissions": ["COL_MESSAGE_READ"],
	"institution_id": "", "authentifie": true, "acr": "2",
}

test_messagerie_session_restreinte_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/console/collaboration/messagerie/non-lus", "permissions": ["COL_MESSAGE_READ"],
	"authentifie": true, "acr": "2", "restreinte": true,
}

test_messagerie_avec_permission if {
	every r in routes_messagerie {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "2"}
	}
}

test_messagerie_sans_permission if {
	every r in routes_messagerie {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "2"}
	}
}

test_messagerie_sans_second_facteur if {
	every r in routes_messagerie {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "1"}
	}
}

test_messagerie_lecture_ne_permet_pas_envoi if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/collaboration/messagerie/conversations/c1/messages",
	"permissions": ["COL_MESSAGE_READ"], "authentifie": true, "acr": "2",
}

test_messagerie_chemin_non_liste_refuse if not authz.allow with input as {
	"method": "DELETE", "path": "/bff/des/collaboration/messagerie/conversations/c1",
	"permissions": ["COL_MESSAGE_READ", "COL_MESSAGE_SEND"], "authentifie": true, "acr": "2",
}

test_messagerie_portail_refusee if {
	every r in routes_messagerie {
		not authz.allow with input as {
			"method": r[0], "path": replace(r[1], "/bff/des/", "/bff/portail/"),
			"permissions": ["COL_MESSAGE_READ", "COL_MESSAGE_SEND", "HOM_PORTAL_READ"],
			"institution_id": "INST-0042", "authentifie": true, "acr": "2",
		}
	}
}

test_collaboration_portail_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/portail/collaboration",
	"permissions": ["COL_MESSAGE_READ"], "institution_id": "INST-0042", "authentifie": true, "acr": "3",
}

test_messagerie_racine_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/collaboration/messagerie",
	"permissions": ["COL_MESSAGE_READ"], "authentifie": true, "acr": "2",
}

test_collaboration_hors_messagerie_laissee_au_service if authz.allow with input as {
	"method": "GET", "path": "/bff/des/collaboration/espaces",
	"permissions": [], "authentifie": true, "acr": "2",
}

# Verrous de la décision et gestes du portail (D-56).
test_pv_etabli_par_le_conducteur if authz.allow with input as {
	"method": "POST", "path": "/bff/des/certification/liaisons/L1/proces-verbal", "permissions": ["CER_PV_ESTABLISH"], "acr": "2", "authentifie": true,
}

test_pv_non_etabli_sans_permission if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/certification/liaisons/L1/proces-verbal", "permissions": ["CER_DOSSIER_READ"], "acr": "2", "authentifie": true,
}

test_signature_gimac_du_chef_des if authz.allow with input as {
	"method": "POST", "path": "/bff/des/certification/liaisons/L1/proces-verbal/signature-gimac", "permissions": ["CER_QUITUS_TECHNIQUE_SIGN"], "acr": "2", "authentifie": true,
}

test_verification_des_attestations_reservee_au_rssi if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/certification/liaisons/L1/approbation-securite/attestations/PCI_DSS/verification", "permissions": ["CER_CHANGE_RECORD_LINK"], "acr": "2", "authentifie": true,
}

test_archivage_par_le_rssi if authz.allow with input as {
	"method": "POST", "path": "/bff/des/certification/liaisons/L1/approbation-securite/archivage", "permissions": ["CER_SECURITY_VERIFY"], "acr": "3", "authentifie": true,
}

test_visa_dg_refuse_a_la_conformite if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/certification/liaisons/L1/approbation-securite/visas/DIRECTION_GENERALE", "permissions": ["CER_QUITUS_CONFORMITE_SIGN"], "acr": "2", "authentifie": true,
}

test_signature_du_participant if authz.allow with input as {
	"method": "POST", "path": "/bff/portail/certification/liaisons/L1/proces-verbal/signature", "permissions": ["CER_PORTAL_READ", "CER_PORTAL_SIGN"],
	"institution_id": "INST-0042", "acr": "2", "authentifie": true,
}

test_signature_du_participant_refusee_sans_permission if not authz.allow with input as {
	"method": "POST", "path": "/bff/portail/certification/liaisons/L1/proces-verbal/signature", "permissions": ["CER_PORTAL_READ"],
	"institution_id": "INST-0042", "acr": "2", "authentifie": true,
}

test_attestation_et_coordonnees_du_participant if {
	authz.allow with input as {
		"method": "POST", "path": "/bff/portail/certification/liaisons/L1/attestations/PCI_DSS", "permissions": ["HOM_PORTAL_SUBMIT"],
		"institution_id": "INST-0042", "acr": "2", "authentifie": true,
	}
	authz.allow with input as {
		"method": "PUT", "path": "/bff/portail/homologation/liaisons/L1/coordonnees-techniques", "permissions": ["HOM_PORTAL_SUBMIT"],
		"institution_id": "INST-0042", "acr": "2", "authentifie": true,
	}
}

test_livrable_du_participant_sans_institution_refuse if not authz.allow with input as {
	"method": "POST", "path": "/bff/portail/homologation/operations/O1/livrable", "permissions": ["HOM_PORTAL_SUBMIT"],
	"institution_id": "", "acr": "2", "authentifie": true,
}

# Parcours, étapes 4 à 10 : chaque geste à permission unique exige sa permission ; route interne refusée.
routes_parcours := [
	["POST", "/bff/des/homologation/parcours/operations/o1/validation", "HOM_OPERATION_VALIDATE"],
	["POST", "/bff/des/homologation/parcours/operations/o1/renvoi", "HOM_OPERATION_VALIDATE"],
	["PUT", "/bff/des/homologation/liaisons/l1/ressources", "HOM_STEP_MANAGE"],
	["POST", "/bff/des/homologation/liaisons/l1/blocages", "HOM_STEP_MANAGE"],
	["POST", "/bff/des/homologation/blocages/b1/levee", "HOM_STEP_MANAGE"],
	["POST", "/bff/des/homologation/liaisons/l1/couverture/constat", "CER_DOSSIER_VERIFY"],
	["POST", "/bff/des/homologation/liaisons/l1/fiche-suivi/constat", "CER_DOSSIER_VERIFY"],
	["GET", "/bff/des/homologation/tableaux-de-bord/acteur", "HOM_DASHBOARD_READ"],
	["GET", "/bff/des/homologation/tableaux-de-bord/pilotage", "HOM_DASHBOARD_READ"],
]

test_parcours_avec_permission if {
	every r in routes_parcours {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "2"}
	}
}

test_parcours_sans_permission if {
	every r in routes_parcours {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "2"}
	}
}

# D-109 : chronologie et pièce d'une opération en lecture (HOM_LIAISON_READ) ; refusées sans elle ou sans second facteur.
routes_preuves := ["/bff/des/homologation/liaisons/l1/chronologie", "/bff/des/homologation/parcours/operations/o1/pieces/d1"]

test_preuves_lecture if {
	every chemin in routes_preuves {
		authz.allow with input as {"method": "GET", "path": chemin, "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "2"}
		not authz.allow with input as {"method": "GET", "path": chemin, "permissions": ["HOM_DASHBOARD_READ"], "authentifie": true, "acr": "2"}
		not authz.allow with input as {"method": "GET", "path": chemin, "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "1"}
	}
}

test_preuves_sans_objet_laisse_au_service if authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/parcours/operations/o1/livrables/L1/sans-objet", "permissions": ["HOM_EVIDENCE_SUBMIT"],
	"authentifie": true, "acr": "2",
}

test_parcours_livrable_laisse_au_service if authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/parcours/operations/o1/livrable", "permissions": ["HOM_STEP_MANAGE"],
	"authentifie": true, "acr": "2",
}

# D-98 : la preuve est laissée au @PreAuthorize du service (HOM_STEP_MANAGE ou HOM_EVIDENCE_SUBMIT), comme le livrable.
test_parcours_preuve_laissee_au_service if {
	every p in ["HOM_STEP_MANAGE", "HOM_EVIDENCE_SUBMIT"] {
		authz.allow with input as {
			"method": "POST", "path": "/bff/des/homologation/parcours/operations/o1/preuves", "permissions": [p],
			"authentifie": true, "acr": "2",
		}
	}
}

# D-98 : « Mes actions de certification » ouvertes à toute permission d'un acteur du parcours, refusées sans, sans second
# facteur ou pour un compte d'institution.
test_mes_operations_acteurs_du_parcours if {
	every p in ["HOM_LIAISON_READ", "HOM_FUNCTION_DDRM", "HOM_PROJECT_CONDUCT", "CER_SECURITY_VERIFY", "CER_QUITUS_DG_SIGN", "CER_CHANGE_RECORD_LINK"] {
		authz.allow with input as {
			"method": "GET", "path": "/bff/des/homologation/parcours/mes-operations", "permissions": [p],
			"authentifie": true, "acr": "2",
		}
	}
}

test_mes_operations_refusees if {
	not authz.allow with input as {
		"method": "GET", "path": "/bff/des/homologation/parcours/mes-operations", "permissions": ["HOM_LIAISON_LIST", "PIL_ACTIVITY_OWN"],
		"authentifie": true, "acr": "2",
	}
	not authz.allow with input as {
		"method": "GET", "path": "/bff/des/homologation/parcours/mes-operations", "permissions": ["HOM_LIAISON_READ"],
		"authentifie": true, "acr": "1",
	}
	not authz.allow with input as {
		"method": "GET", "path": "/bff/des/homologation/parcours/mes-operations", "permissions": ["HOM_LIAISON_READ"],
		"institution_id": "INST-0042", "authentifie": true, "acr": "2",
	}
}

# D-93 et D-94 : affectation d'une fonction et fonctions de l'appelant laissées au @PreAuthorize du service.
test_affectation_fonction_et_mes_fonctions_laissees_au_service if {
	authz.allow with input as {
		"method": "PUT", "path": "/bff/des/homologation/liaisons/l1/fonctions/SNA", "permissions": ["HOM_OPERATION_ASSIGN"],
		"authentifie": true, "acr": "2",
	}
	authz.allow with input as {"method": "GET", "path": "/bff/des/homologation/moi/fonctions", "permissions": [], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "GET", "path": "/bff/des/homologation/moi/fonctions", "permissions": [], "authentifie": true, "acr": "1"}
}

# Remise de la fiche de suivi : conducteur ou Normes et Agréments, décidé par le @PreAuthorize du service.
test_parcours_remise_fiche_laissee_au_service if authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/liaisons/l1/fiche-suivi/remise", "permissions": ["CER_DOSSIER_VERIFY"],
	"authentifie": true, "acr": "2",
}

# Dépôt d'une pièce depuis le DES : livrable d'étape (HOM_STEP_MANAGE) et dossier de changement (CER_CHANGE_RECORD_LINK).
test_depot_document_livrable_et_dossier_changement if {
	every p in ["HOM_STEP_MANAGE", "CER_CHANGE_RECORD_LINK"] {
		authz.allow with input as {"method": "POST", "path": "/bff/des/document/documents", "permissions": [p], "authentifie": true, "acr": "2"}
	}
}

test_contexte_certification_interne_refuse if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/homologation/interne/liaisons/l1/contexte-certification",
	"permissions": ["INTERNAL_HOMOLOGATION_READ"], "authentifie": true, "acr": "2",
}

# D-65 : session restreinte du premier accès (acr 1), règle premier_acces.
chemins_premier_acces := [
	["GET", "/bff/des/iam/moi"],
	["POST", "/bff/des/iam/moi/premiere-configuration"],
	["POST", "/bff/portail/iam/moi/mot-de-passe"],
	["POST", "/bff/console/iam/moi/second-facteur/totp/enrolement"],
	["POST", "/bff/console/iam/moi/second-facteur/totp"],
	["GET", "/bff/portail/iam/moi/second-facteur"],
]

test_premier_acces_chemins_admis if {
	every r in chemins_premier_acces {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [], "authentifie": true, "acr": "1", "restreinte": true}
	}
}

test_premier_acces_exige_session_restreinte if {
	every r in chemins_premier_acces {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": [], "authentifie": true, "acr": "1"}
	}
}

test_premier_acces_exige_authentification if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/moi", "permissions": [], "authentifie": false, "acr": "1", "restreinte": true,
}

test_premier_acces_exige_acr_1 if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/moi", "permissions": [], "authentifie": true, "acr": "", "restreinte": true,
}

test_premier_acces_methodes_limitees if {
	every m in ["PUT", "DELETE", "PATCH"] {
		not authz.allow with input as {"method": m, "path": "/bff/des/iam/moi/mot-de-passe", "permissions": [], "authentifie": true, "acr": "1", "restreinte": true}
	}
}

# Hors de l'assistant, une session restreinte n'ouvre rien, même avec la permission ou acr 2.
chemins_hors_premier_acces := [
	["GET", "/bff/des/iam/moi/sessions"],
	["POST", "/bff/des/iam/moi/fido2"],
	["POST", "/bff/des/iam/reauthentification/defi"],
	["GET", "/bff/des/homologation/liaisons"],
	["GET", "/bff/console/iam/comptes"],
	["GET", "/bff/des/iam/interne/comptes/c1/etat-connexion"],
	["POST", "/bff/des/iam/moi-autre"],
]

test_session_restreinte_hors_assistant_refusee if {
	every r in chemins_hors_premier_acces {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["IAM_ACCOUNT_READ", "HOM_LIAISON_READ"],
			"authentifie": true, "acr": "1", "restreinte": true,
		}
	}
}

test_session_restreinte_acr_2_refusee_hors_assistant if not authz.allow with input as {
	"method": "GET", "path": "/bff/console/iam/comptes", "permissions": ["IAM_ACCOUNT_READ"],
	"authentifie": true, "acr": "2", "restreinte": true,
}

test_session_restreinte_depot_refuse if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/document/documents", "permissions": ["DOC_DEPOSIT"],
	"authentifie": true, "acr": "2", "restreinte": true,
}

# Session complète : inchangée (acr 2 ou 3 exigé), restreinte fausse ou absente.
test_session_complete_mon_compte if authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/moi/sessions", "permissions": [], "authentifie": true, "acr": "2", "restreinte": false,
}

test_session_complete_acr_1_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/moi", "permissions": [], "authentifie": true, "acr": "1", "restreinte": false,
}

# « Mon compte » (docs/lot1/mon-compte.md §1) : photo du titulaire depuis chaque couche, acr 2 exigé.
test_photo_du_titulaire_chaque_couche if {
	every r in [["PUT", "/bff/des/iam/moi/avatar"], ["DELETE", "/bff/portail/iam/moi/avatar"], ["GET", "/bff/console/iam/moi/avatar"], ["GET", "/bff/portail/iam/moi/avatar"]] {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [], "institution_id": "", "authentifie": true, "acr": "2"}
	}
}

test_photo_du_titulaire_sans_second_facteur_refusee if {
	every acr in ["1", "", "0"] {
		not authz.allow with input as {"method": "PUT", "path": "/bff/des/iam/moi/avatar", "permissions": [], "authentifie": true, "acr": acr}
	}
}

test_photo_du_titulaire_hors_assistant_en_session_restreinte if {
	every m in ["GET", "PUT", "DELETE"] {
		not authz.allow with input as {"method": m, "path": "/bff/des/iam/moi/avatar", "permissions": [], "authentifie": true, "acr": "1", "restreinte": true}
	}
}

test_photo_d_un_collegue_des_et_console if {
	every c in ["des", "console"] {
		authz.allow with input as {"method": "GET", "path": sprintf("/bff/%s/iam/avatars/u-123", [c]), "permissions": [], "authentifie": true, "acr": "2"}
	}
}

test_photo_d_un_collegue_acr_3 if authz.allow with input as {"method": "GET", "path": "/bff/des/iam/avatars/u-123", "permissions": [], "authentifie": true, "acr": "3"}

test_photo_d_un_collegue_refusee_au_portail if not authz.allow with input as {
	"method": "GET", "path": "/bff/portail/iam/avatars/u-123", "permissions": ["HOM_PORTAL_READ"], "institution_id": "INST-0042",
	"authentifie": true, "acr": "2",
}

test_photo_d_un_collegue_sans_second_facteur_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/avatars/u-123", "permissions": [], "authentifie": true, "acr": "1",
}

test_photo_d_un_collegue_sans_authentification_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/console/iam/avatars/u-123", "permissions": [], "authentifie": false, "acr": "2",
}

test_photo_d_un_collegue_session_restreinte_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/avatars/u-123", "permissions": [], "authentifie": true, "acr": "2", "restreinte": true,
}

test_photo_d_un_collegue_lecture_seule if {
	every r in [["PUT", "/bff/des/iam/avatars/u-123"], ["DELETE", "/bff/console/iam/avatars/u-123"], ["GET", "/bff/des/iam/avatars"], ["GET", "/bff/des/iam/avatars/u-1/autre"]] {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": [], "authentifie": true, "acr": "2"}
	}
}

# D-68 : seules les formes prévues de gestion d'un groupe passent (pas de suppression de conversation).
test_messagerie_groupe_formes_non_prevues_refusees if {
	every r in [
		["DELETE", "/bff/des/collaboration/messagerie/conversations/c1"],
		["PATCH", "/bff/des/collaboration/messagerie/conversations/c1/membres/u1"],
		["PUT", "/bff/des/collaboration/messagerie/conversations/c1"],
	] {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["COL_MESSAGE_SEND", "COL_MESSAGE_READ"],
			"authentifie": true, "acr": "2", "institution_id": "",
		}
	}
}

# Dépôts fragmentés (D-74, docs/lot1/depots-fragmentes.md §3) : routes du contrat depuis les trois couches.
routes_depots_fragmentes(couche) := [
	["POST", sprintf("/bff/%s/document/depots", [couche])],
	["PUT", sprintf("/bff/%s/document/depots/d1/parties/1", [couche])],
	["PUT", sprintf("/bff/%s/document/depots/d1/parties/256", [couche])],
	["GET", sprintf("/bff/%s/document/depots/d1", [couche])],
	["POST", sprintf("/bff/%s/document/depots/d1/fin", [couche])],
	["DELETE", sprintf("/bff/%s/document/depots/d1", [couche])],
]

test_depots_fragmentes_des_et_console_avec_permission if {
	every couche in ["des", "console"] {
		every r in routes_depots_fragmentes(couche) {
			every p in ["PAR_ADHESION_INSTRUCT", "HOM_EVIDENCE_SUBMIT", "HOM_STEP_MANAGE", "CER_CHANGE_RECORD_LINK", "DOC_DEPOSIT", "COL_MESSAGE_SEND"] {
				authz.allow with input as {"method": r[0], "path": r[1], "permissions": [p], "authentifie": true, "acr": "2"}
			}
		}
	}
}

test_depots_fragmentes_acr_3 if authz.allow with input as {
	"method": "PUT", "path": "/bff/console/document/depots/d1/parties/3", "permissions": ["COL_MESSAGE_SEND"],
	"authentifie": true, "acr": "3",
}

test_depots_fragmentes_sans_permission_refuses if {
	every couche in ["des", "console"] {
		every r in routes_depots_fragmentes(couche) {
			not authz.allow with input as {
				"method": r[0], "path": r[1], "permissions": ["COL_MESSAGE_READ", "HOM_PORTAL_READ", "INTERNAL_DOCUMENT_READ"],
				"authentifie": true, "acr": "2",
			}
		}
	}
}

test_depots_fragmentes_sans_second_facteur_refuses if {
	every couche in ["des", "console", "portail"] {
		every r in routes_depots_fragmentes(couche) {
			not authz.allow with input as {
				"method": r[0], "path": r[1], "permissions": ["DOC_DEPOSIT", "COL_MESSAGE_SEND", "HOM_PORTAL_SUBMIT"],
				"institution_id": "INST-0042", "authentifie": true, "acr": "1",
			}
		}
	}
}

test_depots_fragmentes_session_restreinte_refuses if {
	every r in routes_depots_fragmentes("des") {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["DOC_DEPOSIT"], "authentifie": true, "acr": "2", "restreinte": true,
		}
	}
}

test_depots_fragmentes_portail if {
	every r in routes_depots_fragmentes("portail") {
		authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["HOM_PORTAL_SUBMIT"], "institution_id": "INST-0042",
			"authentifie": true, "acr": "2",
		}
	}
}

test_depots_fragmentes_portail_exige_institution_et_permission if {
	every r in routes_depots_fragmentes("portail") {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["HOM_PORTAL_SUBMIT"], "institution_id": "",
			"authentifie": true, "acr": "2",
		}
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["DOC_DEPOSIT", "COL_MESSAGE_SEND", "HOM_PORTAL_READ"],
			"institution_id": "INST-0042", "authentifie": true, "acr": "2",
		}
	}
}

# Pas de repli sur @PreAuthorize : autres méthodes et chemins voisins refusés, même avec toutes les permissions.
test_depots_fragmentes_formes_non_prevues_refusees if {
	every couche in ["des", "console", "portail"] {
		every r in [
			["GET", sprintf("/bff/%s/document/depots", [couche])],
			["PUT", sprintf("/bff/%s/document/depots/d1", [couche])],
			["PATCH", sprintf("/bff/%s/document/depots/d1", [couche])],
			["POST", sprintf("/bff/%s/document/depots/d1/parties/1", [couche])],
			["PUT", sprintf("/bff/%s/document/depots/d1/parties/1/x", [couche])],
			["GET", sprintf("/bff/%s/document/depots/d1/parties", [couche])],
			["DELETE", sprintf("/bff/%s/document/depots/d1/fin", [couche])],
			["GET", sprintf("/bff/%s/document/depots/d1/autre/chose", [couche])],
		] {
			not authz.allow with input as {
				"method": r[0], "path": r[1],
				"permissions": ["DOC_DEPOSIT", "COL_MESSAGE_SEND", "HOM_PORTAL_SUBMIT", "PAR_ADHESION_INSTRUCT"],
				"institution_id": "INST-0042", "authentifie": true, "acr": "2",
			}
		}
	}
}

# D-76 : Mon activité (docs/lot1/mon-activite.md §2), tout membre interne ; équipe des responsables.
routes_pilotage := [
	["GET", "/bff/des/pilotage/agenda", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/agenda", "PIL_ACTIVITY_OWN"],
	["PUT", "/bff/des/pilotage/agenda/e1", "PIL_ACTIVITY_OWN"],
	["DELETE", "/bff/des/pilotage/agenda/e1", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/agenda/e1/reponse", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/activites", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/activites", "PIL_ACTIVITY_OWN"],
	["PUT", "/bff/des/pilotage/activites/a1", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/activites/a1/statut", "PIL_ACTIVITY_OWN"],
	["DELETE", "/bff/des/pilotage/activites/a1", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/taches", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/taches/t1", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/taches", "PIL_ACTIVITY_OWN"],
	["PUT", "/bff/des/pilotage/taches/t1", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/taches/t1/transition", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/rapports", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/rapports/r1", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/rapports/r1/document", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/rapports/r1/destinataires", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/rapports", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/rapports/preremplissage", "PIL_ACTIVITY_OWN"],
	["PUT", "/bff/des/pilotage/rapports/r1", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/rapports/r1/soumission", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/rapports/r1/visa", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/moi/synthese", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/pilotage/equipe", "PIL_TEAM_READ"],
	["GET", "/bff/des/iam/organisation/moi", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/iam/organisation/unites", "IAM_ACCOUNT_EDIT"],
	["PUT", "/bff/console/iam/organisation/unites/DES", "IAM_ACCOUNT_EDIT"],
	["PUT", "/bff/des/iam/comptes/c1/rattachement", "IAM_ACCOUNT_EDIT"],
]

test_pilotage_avec_permission if {
	every r in routes_pilotage {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "2"}
	}
}

test_pilotage_sans_permission if {
	every r in routes_pilotage {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["COL_MESSAGE_READ"], "authentifie": true, "acr": "2"}
	}
}

# Pilotage et organisation : jamais pour un compte d'institution (le rattachement relève de l'administration).
routes_reservees_internes := [r | some r in routes_pilotage; not contains(r[1], "/comptes/")]

test_pilotage_compte_externe_refuse if {
	every r in routes_reservees_internes {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": [r[2]], "institution_id": "INST-0042",
			"authentifie": true, "acr": "2",
		}
	}
}

test_pilotage_equipe_exige_sa_permission if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/pilotage/equipe", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2",
}

# Chemin ou méthode hors contrat : refusé, sans repli sur @PreAuthorize.
test_pilotage_chemin_non_liste_refuse if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/pilotage/boards", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2",
}

test_pilotage_suppression_tache_refusee if not authz.allow with input as {
	"method": "DELETE", "path": "/bff/des/pilotage/taches/t1", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2",
}

test_pilotage_sans_second_facteur_refuse if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/pilotage/agenda", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "1",
}

test_pilotage_interne_jamais_relaye if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/pilotage/interne/taches", "permissions": ["PIL_ACTIVITY_OWN", "INTERNAL_PILOTAGE_TASK_CREATE"],
	"authentifie": true, "acr": "2",
}

# D-87 : processus d'unité (docs/lot1/processus-unite.md §5.1), toutes les méthodes et tous les chemins du contrat.
routes_processus := [
	["GET", "/bff/des/pilotage/processus"],
	["POST", "/bff/des/pilotage/processus"],
	["GET", "/bff/des/pilotage/processus/p1"],
	["PUT", "/bff/des/pilotage/processus/p1"],
	# D-103 : public d'un brouillon ou d'une version publiée.
	["PUT", "/bff/des/pilotage/processus/p1/public"],
	["DELETE", "/bff/des/pilotage/processus/p1"],
	["POST", "/bff/des/pilotage/processus/p1/controle"],
	["POST", "/bff/des/pilotage/processus/p1/publication"],
	["POST", "/bff/des/pilotage/processus/p1/nouvelle-version"],
	["POST", "/bff/des/pilotage/processus/p1/retrait"],
	["POST", "/bff/des/pilotage/processus/p1/instances"],
	["GET", "/bff/des/pilotage/processus/instances"],
	["GET", "/bff/des/pilotage/processus/instances/i1"],
	["GET", "/bff/des/pilotage/processus/candidats"],
	# D-98 : processus que je peux lancer, mes demandes.
	["GET", "/bff/des/pilotage/processus/lancables"],
	["GET", "/bff/des/pilotage/processus/demandes"],
	["GET", "/bff/des/pilotage/processus/instances/i1/etapes/ANALYSE/candidats"],
	# QUO-04 : pièce jointe ou livrable d'une instance.
	["GET", "/bff/des/pilotage/processus/instances/i1/documents/d1"],
	["POST", "/bff/des/pilotage/processus/instances/i1/etapes/ANALYSE/livrables"],
	["PUT", "/bff/des/pilotage/processus/instances/i1/etapes/ANALYSE/champs"],
	["POST", "/bff/des/pilotage/processus/instances/i1/etapes/RECETTE/signature"],
	["POST", "/bff/des/pilotage/processus/instances/i1/etapes/ANALYSE/fin"],
	["POST", "/bff/des/pilotage/processus/instances/i1/etapes/ANALYSE/validation"],
	["POST", "/bff/des/pilotage/processus/instances/i1/etapes/ANALYSE/reaffectation"],
	["POST", "/bff/des/pilotage/processus/instances/i1/suspension"],
	["POST", "/bff/des/pilotage/processus/instances/i1/reprise"],
	["POST", "/bff/des/pilotage/processus/instances/i1/annulation"],
]

test_processus_avec_permission if {
	every r in routes_processus {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2"}
	}
}

test_processus_sans_permission if {
	every r in routes_processus {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["PIL_TEAM_READ", "TACHE_CONFIER", "IAM_PROFILE_EDIT", "COL_MESSAGE_READ"],
			"authentifie": true, "acr": "2",
		}
	}
}

test_processus_compte_externe_refuse if {
	every r in routes_processus {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["PIL_ACTIVITY_OWN"], "institution_id": "INST-0042",
			"authentifie": true, "acr": "2",
		}
	}
}

test_processus_sans_second_facteur_refuse if {
	every r in routes_processus {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "1"}
	}
}

test_processus_session_restreinte_refusee if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/pilotage/processus", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2",
	"restreinte": true,
}

# Méthode ou chemin hors contrat : refusé, sans repli sur @PreAuthorize ; ni console ni portail.
test_processus_hors_contrat_refuse if {
	every r in [
		["PATCH", "/bff/des/pilotage/processus/p1"],
		["DELETE", "/bff/des/pilotage/processus/instances/i1"],
		["PUT", "/bff/des/pilotage/processus/instances/i1/etapes/E/signature"],
		["POST", "/bff/des/pilotage/processus/p1/public"],
		["DELETE", "/bff/des/pilotage/processus/p1/public"],
		["PUT", "/bff/des/pilotage/processus/p1/public/x"],
		["PUT", "/bff/des/pilotage/processus/p1/autre"],
		["POST", "/bff/des/pilotage/processus/p1/autre"],
		["POST", "/bff/des/pilotage/processus/instances/i1/etapes/E/autre"],
		["GET", "/bff/des/pilotage/processus/instances/i1/etapes/E"],
		["GET", "/bff/des/pilotage/processus/p1/publication/x"],
		["POST", "/bff/des/pilotage/processus/instances/i1/cloture"],
		["GET", "/bff/console/pilotage/processus"],
		["GET", "/bff/portail/pilotage/processus"],
		["POST", "/bff/des/pilotage/processus/interne/x"],
	] {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2"}
	}
}

test_unites_lecture_membre_interne if authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/organisation/unites", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2",
}

test_unites_lecture_administration_console if authz.allow with input as {
	"method": "GET", "path": "/bff/console/iam/organisation/unites", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2",
}

test_unites_lecture_sans_permission if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/organisation/unites", "permissions": ["COL_MESSAGE_READ"], "authentifie": true, "acr": "2",
}

test_unites_lecture_compte_externe if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/organisation/unites", "permissions": ["PIL_ACTIVITY_OWN"], "institution_id": "INST-0042",
	"authentifie": true, "acr": "2",
}

test_depot_rapport_activite if authz.allow with input as {
	"method": "POST", "path": "/bff/des/document/depots", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2",
}

# D-77 : modélisateur de processus, écriture des brouillons et migration des liaisons.
routes_modelisateur := [
	["POST", "/bff/des/homologation/referentiels", "HOM_REFERENTIEL_EDIT"],
	["POST", "/bff/des/homologation/referentiels/v1/operations", "HOM_REFERENTIEL_EDIT"],
	["POST", "/bff/des/homologation/referentiels/v1/controle", "HOM_REFERENTIEL_EDIT"],
	["PUT", "/bff/des/homologation/referentiels/v1/operations/C1", "HOM_REFERENTIEL_EDIT"],
	["PUT", "/bff/des/homologation/referentiels/v1/positions", "HOM_REFERENTIEL_EDIT"],
	["PUT", "/bff/des/homologation/referentiels/v1/arcs", "HOM_REFERENTIEL_EDIT"],
	["PUT", "/bff/des/homologation/referentiels/v1/pilote", "HOM_REFERENTIEL_EDIT"],
	["DELETE", "/bff/des/homologation/referentiels/v1/operations/C1", "HOM_REFERENTIEL_EDIT"],
	["POST", "/bff/des/homologation/referentiels/v1/migrations", "HOM_REFERENTIEL_MIGRATE"],
	["POST", "/bff/des/homologation/referentiels/v1/migrations/simulation", "HOM_REFERENTIEL_MIGRATE"],
	["POST", "/bff/des/homologation/referentiels/v1/publication", "HOM_REFERENTIEL_PUBLISH"],
]

test_modelisateur_avec_permission if {
	every r in routes_modelisateur {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "2"}
	}
}

test_modelisateur_sans_permission if {
	every r in routes_modelisateur {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["HOM_REFERENTIEL_READ"], "authentifie": true, "acr": "2"}
	}
}

test_modelisateur_lecture_graphe if authz.allow with input as {
	"method": "GET", "path": "/bff/des/homologation/referentiels/v1/graphe", "permissions": ["HOM_REFERENTIEL_READ"], "authentifie": true, "acr": "2",
}

# D-89 : étapes configurables d'un brouillon (ajout, renommage, ordre, retrait) ; lecture filtrée par le service.
routes_etapes_referentiel := [
	["POST", "/bff/des/homologation/referentiels/v1/etapes"],
	["PUT", "/bff/des/homologation/referentiels/v1/etapes/REVUE_JURIDIQUE"],
	["PUT", "/bff/des/homologation/referentiels/v1/etapes/ordre"],
	["DELETE", "/bff/des/homologation/referentiels/v1/etapes/REVUE_JURIDIQUE"],
]

test_etapes_referentiel_editeur if {
	every r in routes_etapes_referentiel {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["HOM_REFERENTIEL_EDIT"], "authentifie": true, "acr": "2"}
	}
}

test_etapes_referentiel_lecteur_refuse if {
	every r in routes_etapes_referentiel {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL"], "authentifie": true, "acr": "2"}
	}
}

test_etapes_referentiel_un_facteur_refuse if {
	not authz.allow with input as {
		"method": "POST", "path": "/bff/des/homologation/referentiels/v1/etapes", "permissions": ["HOM_REFERENTIEL_EDIT"], "authentifie": true, "acr": "1",
	}
}

test_etapes_referentiel_chemin_imbrique_non_couvert if {
	not authz.required_permission with input as {"method": "DELETE", "path": "/bff/des/homologation/referentiels/v1/etapes/A/B"}
}

test_referentiel_lecture_filtree if {
	every chemin in ["/bff/des/homologation/referentiels/v1/etapes", "/bff/des/homologation/referentiels/v1/export", "/bff/des/homologation/referentiels/catalogue"] {
		authz.allow with input as {"method": "GET", "path": chemin, "permissions": ["HOM_REFERENTIEL_READ"], "authentifie": true, "acr": "2"}
	}
}

# D-91 : types de projet et catalogue des fonctions (lecture HOM_REFERENTIEL_READ, écriture HOM_REFERENTIEL_EDIT).
routes_types_fonctions_lecture := [
	"/bff/des/homologation/types-projet",
	"/bff/des/homologation/types-projet/MONNAIE_ELECTRONIQUE",
	"/bff/des/homologation/referentiels/fonctions",
	"/bff/des/homologation/referentiels/fonctions/JURISTE",
]

routes_types_fonctions_ecriture := [
	["POST", "/bff/des/homologation/types-projet"],
	["PUT", "/bff/des/homologation/types-projet/MONNAIE_ELECTRONIQUE"],
	["POST", "/bff/des/homologation/referentiels/fonctions"],
	["PUT", "/bff/des/homologation/referentiels/fonctions/JURISTE"],
]

test_types_fonctions_lecture if {
	every chemin in routes_types_fonctions_lecture {
		authz.required_permission == "HOM_REFERENTIEL_READ" with input as {"method": "GET", "path": chemin}
		authz.allow with input as {"method": "GET", "path": chemin, "permissions": ["HOM_REFERENTIEL_READ"], "authentifie": true, "acr": "2"}
		not authz.allow with input as {"method": "GET", "path": chemin, "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "2"}
	}
}

test_types_fonctions_ecriture_editeur if {
	every r in routes_types_fonctions_ecriture {
		authz.required_permission == "HOM_REFERENTIEL_EDIT" with input as {"method": r[0], "path": r[1]}
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["HOM_REFERENTIEL_EDIT"], "authentifie": true, "acr": "2"}
	}
}

test_types_fonctions_ecriture_lecteur_refuse if {
	every r in routes_types_fonctions_ecriture {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL"], "authentifie": true, "acr": "2"}
	}
}

test_types_fonctions_un_facteur_refuse if {
	not authz.allow with input as {"method": "POST", "path": "/bff/des/homologation/types-projet", "permissions": ["HOM_REFERENTIEL_EDIT"], "authentifie": true, "acr": "1"}
}

test_types_projet_chemin_imbrique_non_couvert if {
	not authz.required_permission with input as {"method": "PUT", "path": "/bff/des/homologation/types-projet/A/B"}
}

# D-91 §3 : signature d'une opération d'une liaison (membre interne ; signataire vérifié par MS-HOMOLOGATION).
signature_operation := "/bff/des/homologation/liaisons/l1/operations/27/signatures/VISA_DG"

test_signature_operation if {
	authz.required_permission == "HOM_LIAISON_READ" with input as {"method": "POST", "path": signature_operation}
	authz.allow with input as {"method": "POST", "path": signature_operation, "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "2"}
}

test_signature_operation_refusee if {
	not authz.allow with input as {"method": "POST", "path": signature_operation, "permissions": ["HOM_PORTAL_SUBMIT"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "POST", "path": signature_operation, "permissions": ["HOM_LIAISON_READ"], "authentifie": true, "acr": "1"}
}

# D-92 : suppression d'un brouillon du référentiel.
test_suppression_brouillon if {
	authz.allow with input as {"method": "DELETE", "path": "/bff/des/homologation/referentiels/v1", "permissions": ["HOM_REFERENTIEL_EDIT"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "DELETE", "path": "/bff/des/homologation/referentiels/v1", "permissions": ["HOM_REFERENTIEL_READ"], "authentifie": true, "acr": "2"}
}

# D-92 : REF_METIER_ADMIN vaut chaque permission du référentiel, des étapes, des types de projet et des fonctions.
test_ref_metier_admin_referentiels if {
	routes := array.concat(
		array.concat(routes_modelisateur, [[r[0], r[1], "HOM_REFERENTIEL_EDIT"] | some r in routes_etapes_referentiel]),
		[[r[0], r[1], "HOM_REFERENTIEL_EDIT"] | some r in routes_types_fonctions_ecriture],
	)
	every r in routes {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["REF_METIER_ADMIN"], "authentifie": true, "acr": "2"}
	}
	every chemin in routes_types_fonctions_lecture {
		authz.allow with input as {"method": "GET", "path": chemin, "permissions": ["REF_METIER_ADMIN"], "authentifie": true, "acr": "2"}
	}
	authz.allow with input as {"method": "DELETE", "path": "/bff/des/homologation/referentiels/v1", "permissions": ["REF_METIER_ADMIN"], "authentifie": true, "acr": "2"}
}

test_ref_metier_admin_hors_referentiels if {
	not authz.allow with input as {"method": "POST", "path": "/bff/des/homologation/liaisons", "permissions": ["REF_METIER_ADMIN"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "POST", "path": "/bff/console/iam/comptes", "permissions": ["REF_METIER_ADMIN"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "POST", "path": "/bff/des/homologation/types-projet", "permissions": ["REF_METIER_ADMIN"], "authentifie": true, "acr": "1"}
}

# D-80 : notifications de la plateforme, comptes internes seulement.
routes_notifications := [
	["GET", "/bff/des/notification/notifications"],
	["GET", "/bff/console/notification/notifications/compteur"],
	["POST", "/bff/des/notification/notifications/n1/lecture"],
	["POST", "/bff/console/notification/notifications/lecture"],
	["GET", "/bff/des/notification/preferences"],
	["PUT", "/bff/des/notification/preferences"],
]

test_notifications_avec_permission if {
	every r in routes_notifications {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["NOT_INBOX_READ"], "authentifie": true, "acr": "2"}
	}
}

test_notifications_sans_permission if {
	every r in routes_notifications {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["COL_MESSAGE_READ"], "authentifie": true, "acr": "2"}
	}
}

test_notifications_compte_externe_refuse if {
	every r in routes_notifications {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["NOT_INBOX_READ"], "institution_id": "INST-0042",
			"authentifie": true, "acr": "2",
		}
	}
}

test_notifications_chemin_non_liste_refuse if not authz.allow with input as {
	"method": "DELETE", "path": "/bff/des/notification/notifications/n1", "permissions": ["NOT_INBOX_READ"], "authentifie": true, "acr": "2",
}

# D-93 §11.4 : notifications du portail, comptes d'institution à deux facteurs portant NOT_INBOX_READ.
routes_notifications_portail := [
	["GET", "/bff/portail/notification/notifications"],
	["GET", "/bff/portail/notification/notifications/compteur"],
	["GET", "/bff/portail/notification/preferences"],
	["POST", "/bff/portail/notification/notifications/n1/lecture"],
	["POST", "/bff/portail/notification/notifications/lecture"],
	["PUT", "/bff/portail/notification/preferences"],
]

correspondant(r, permissions, acr) := {
	"method": r[0], "path": r[1], "permissions": permissions, "institution_id": "INST-0042",
	"authentifie": true, "acr": acr,
}

test_notifications_portail_correspondant if {
	every r in routes_notifications_portail {
		authz.allow with input as correspondant(r, ["NOT_INBOX_READ", "HOM_PORTAL_READ"], "2")
	}
}

test_notifications_portail_refus if {
	every r in routes_notifications_portail {
		not authz.allow with input as correspondant(r, ["HOM_PORTAL_READ"], "2")
		not authz.allow with input as correspondant(r, ["NOT_INBOX_READ"], "1")
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["NOT_INBOX_READ"], "authentifie": true, "acr": "2"}
	}
}

test_notifications_portail_chemin_non_liste_refuse if {
	not authz.allow with input as correspondant(["DELETE", "/bff/portail/notification/notifications/n1"], ["NOT_INBOX_READ"], "2")
	not authz.allow with input as correspondant(["GET", "/bff/portail/notification/notifications/n1"], ["NOT_INBOX_READ"], "2")
	not authz.allow with input as correspondant(["PUT", "/bff/portail/notification/notifications/compteur"], ["NOT_INBOX_READ"], "2")
}

# D-81 : pouvoirs des chefs, demandes à l'administration, profils créés, tâches confiées.
routes_pouvoirs := [
	["GET", "/bff/des/iam/pouvoirs/matrice", "IAM_ACCOUNT_READ"],
	["PUT", "/bff/console/iam/pouvoirs/matrice/ROLE_CHEF_DES", "IAM_PROFILE_EDIT"],
	["GET", "/bff/des/iam/pouvoirs/moi", "PIL_ACTIVITY_OWN"],
	["PUT", "/bff/des/iam/pouvoirs/subordonnes/u1/rattachement", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/iam/demandes", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/des/iam/demandes", "PIL_ACTIVITY_OWN"],
	["GET", "/bff/console/iam/demandes/d1", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/iam/demandes/d1/annulation", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/iam/demandes/d1/acceptation", "IAM_ACCOUNT_CREATE"],
	["POST", "/bff/console/iam/demandes/d1/refus", "IAM_ACCOUNT_READ"],
	["POST", "/bff/des/iam/configuration/profils", "IAM_PROFILE_EDIT"],
	["DELETE", "/bff/console/iam/configuration/profils/ROLE_OPERATEUR_RESEAU", "IAM_PROFILE_EDIT"],
	["GET", "/bff/des/pilotage/taches/groupes/g1", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/taches/t1/prise", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/taches/t1/attribution", "PIL_ACTIVITY_OWN"],
	["POST", "/bff/des/pilotage/taches/t1/relance", "PIL_ACTIVITY_OWN"],
]

test_pouvoirs_avec_permission if {
	every r in routes_pouvoirs {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "2"}
	}
}

test_pouvoirs_sans_permission if {
	every r in routes_pouvoirs {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["COL_MESSAGE_READ"], "authentifie": true, "acr": "2"}
	}
}

routes_pouvoirs_reservees := [r | some r in routes_pouvoirs; not contains(r[1], "/configuration/")]

test_pouvoirs_compte_externe_refuse if {
	every r in routes_pouvoirs_reservees {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": [r[2]], "institution_id": "INST-0042",
			"authentifie": true, "acr": "2",
		}
	}
}

# Rattachement d'un subordonné : PUT sur le département seulement, avec PIL_ACTIVITY_OWN ; la route
# d'administration du rattachement reste réservée à IAM_ACCOUNT_EDIT.
# D-86 : libellé et description d'un profil (PUT), sans ouvrir la recomposition ni la création à IAM_ACCOUNT_READ.
routes_profils_organisation := [
	["PUT", "/bff/console/iam/configuration/profils/ROLE_POLE_RESEAU"],
	["PUT", "/bff/des/iam/configuration/profils/ROLE_OPERATEUR_RESEAU"],
]

test_profil_presentation_avec_permission if {
	every r in routes_profils_organisation {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["IAM_PROFILE_EDIT"], "authentifie": true, "acr": "2"}
	}
}

test_profil_presentation_sans_permission if {
	every r in routes_profils_organisation {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}
	}
}

# Sur la console (pas de repli vers @PreAuthorize), seuls les chemins listés sont ouverts.
test_profil_presentation_chemins_voisins if {
	authz.allow with input as {"method": "PUT", "path": "/bff/console/iam/configuration/profils/ROLE_SNA/permissions", "permissions": ["IAM_PROFILE_EDIT"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "PUT", "path": "/bff/console/iam/configuration/profils/ROLE_SNA/autre", "permissions": ["IAM_PROFILE_EDIT"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "PUT", "path": "/bff/console/iam/configuration/profils", "permissions": ["IAM_PROFILE_EDIT"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "PATCH", "path": "/bff/console/iam/configuration/profils/ROLE_SNA", "permissions": ["IAM_PROFILE_EDIT"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "PUT", "path": "/bff/portail/iam/configuration/profils/ROLE_SNA", "permissions": ["IAM_PROFILE_EDIT"], "institution_id": "INST-0042", "authentifie": true, "acr": "2"}
}

# Sans double facteur, refusé même avec la permission.
test_profil_presentation_sans_double_facteur if not authz.allow with input as {
	"method": "PUT", "path": "/bff/console/iam/configuration/profils/ROLE_SNA", "permissions": ["IAM_PROFILE_EDIT"], "authentifie": true, "acr": "1",
}

test_rattachement_subordonne_avec_permission if authz.allow with input as {
	"method": "PUT", "path": "/bff/des/iam/pouvoirs/subordonnes/u1/rattachement", "permissions": ["PIL_ACTIVITY_OWN"],
	"authentifie": true, "acr": "2",
}

test_rattachement_subordonne_sans_permission if not authz.allow with input as {
	"method": "PUT", "path": "/bff/des/iam/pouvoirs/subordonnes/u1/rattachement", "permissions": ["IAM_ACCOUNT_EDIT"],
	"authentifie": true, "acr": "2",
}

test_rattachement_subordonne_autres_formes_refusees if {
	not authz.allow with input as {
		"method": "POST", "path": "/bff/des/iam/pouvoirs/subordonnes/u1/rattachement", "permissions": ["PIL_ACTIVITY_OWN"],
		"authentifie": true, "acr": "2",
	}
	not authz.allow with input as {
		"method": "PUT", "path": "/bff/console/iam/pouvoirs/subordonnes/u1/rattachement", "permissions": ["PIL_ACTIVITY_OWN"],
		"authentifie": true, "acr": "2",
	}
	not authz.allow with input as {
		"method": "PUT", "path": "/bff/des/iam/pouvoirs/subordonnes/u1/x/rattachement", "permissions": ["PIL_ACTIVITY_OWN"],
		"authentifie": true, "acr": "2",
	}
	not authz.allow with input as {
		"method": "PUT", "path": "/bff/des/iam/comptes/u1/rattachement", "permissions": ["PIL_ACTIVITY_OWN"],
		"authentifie": true, "acr": "2",
	}
}

test_pouvoirs_interne_jamais_relaye if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/interne/pouvoirs/u1", "permissions": ["PIL_ACTIVITY_OWN", "INTERNAL_IAM_READ"],
	"authentifie": true, "acr": "2",
}

# Aperçu des documents bureautiques (D-83, docs/lot1/apercu-bureautique.md §2) : POST seulement, département et
# console, PIL_ACTIVITY_OWN, comptes internes seulement.
test_apercus_des_et_console if {
	every couche in ["des", "console"] {
		authz.allow with input as {
			"method": "POST", "path": sprintf("/bff/%s/document/apercus", [couche]), "permissions": ["PIL_ACTIVITY_OWN"],
			"authentifie": true, "acr": "2",
		}
	}
}

test_apercus_acr_3 if authz.allow with input as {
	"method": "POST", "path": "/bff/console/document/apercus", "permissions": ["PIL_ACTIVITY_OWN"],
	"institution_id": "", "authentifie": true, "acr": "3",
}

test_apercus_sans_permission_refuse if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/document/apercus",
	"permissions": ["DOC_DEPOSIT", "COL_MESSAGE_SEND", "COL_MESSAGE_READ"], "authentifie": true, "acr": "2",
}

test_apercus_sans_second_facteur_refuse if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/document/apercus", "permissions": ["PIL_ACTIVITY_OWN"],
	"authentifie": true, "acr": "1",
}

test_apercus_compte_externe_refuse if {
	every couche in ["des", "console"] {
		not authz.allow with input as {
			"method": "POST", "path": sprintf("/bff/%s/document/apercus", [couche]), "permissions": ["PIL_ACTIVITY_OWN"],
			"institution_id": "INST-0042", "authentifie": true, "acr": "2",
		}
	}
}

test_apercus_portail_refuse if not authz.allow with input as {
	"method": "POST", "path": "/bff/portail/document/apercus", "permissions": ["PIL_ACTIVITY_OWN", "HOM_PORTAL_SUBMIT"],
	"institution_id": "INST-0042", "authentifie": true, "acr": "2",
}

test_apercus_autres_methodes_et_chemins_refuses if {
	every r in [
		["GET", "/bff/des/document/apercus"],
		["PUT", "/bff/des/document/apercus"],
		["DELETE", "/bff/console/document/apercus"],
		["POST", "/bff/des/document/apercus/a1"],
		["GET", "/bff/des/document/apercus/a1"],
	] {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": ["PIL_ACTIVITY_OWN", "DOC_DEPOSIT"],
			"authentifie": true, "acr": "2",
		}
	}
}

test_apercus_session_restreinte_refuse if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/document/apercus", "permissions": ["PIL_ACTIVITY_OWN"],
	"authentifie": true, "acr": "2", "restreinte": true,
}

test_acceptation_demande_sans_creation_refusee if {
	not authz.allow with input as {"method": "POST", "path": "/bff/des/iam/demandes/d1/acceptation", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}
}

# D-88 : détail « Lu par » d'un de mes messages.
test_lectures_message_avec_permission if {
	authz.allow with input as {"method": "GET", "path": "/bff/des/collaboration/messagerie/conversations/c1/messages/m1/lectures", "permissions": ["COL_MESSAGE_READ"], "authentifie": true, "acr": "2"}
}

test_lectures_message_sans_permission if {
	not authz.allow with input as {"method": "GET", "path": "/bff/des/collaboration/messagerie/conversations/c1/messages/m1/lectures", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2"}
}

# Catalogue des fonctionnalités (D-94) : lecture par tout compte interne, modification par IAM_PROFILE_EDIT.
test_catalogue_fonctionnalites_lu_par_tout_compte_interne if {
	every c in ["des", "console"] {
		authz.allow with input as {"method": "GET", "path": sprintf("/bff/%s/iam/configuration/fonctionnalites", [c]), "permissions": [], "authentifie": true, "acr": "2"}
	}
}

test_catalogue_fonctionnalites_refuse_a_un_compte_d_institution if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/iam/configuration/fonctionnalites", "permissions": ["HOM_PORTAL_READ"], "institution_id": "INST-0042",
	"authentifie": true, "acr": "2",
}

test_catalogue_fonctionnalites_refuse_au_portail if not authz.allow with input as {
	"method": "GET", "path": "/bff/portail/iam/configuration/fonctionnalites", "permissions": ["HOM_PORTAL_READ"], "institution_id": "INST-0042",
	"authentifie": true, "acr": "2",
}

test_catalogue_fonctionnalites_refuse_sans_second_facteur if not authz.allow with input as {
	"method": "GET", "path": "/bff/console/iam/configuration/fonctionnalites", "permissions": [], "authentifie": true, "acr": "1",
}

test_modification_d_une_fonctionnalite_avec_iam_profile_edit if {
	every c in ["des", "console"] {
		authz.allow with input as {"method": "PUT", "path": sprintf("/bff/%s/iam/configuration/fonctionnalites/liaisons", [c]), "permissions": ["IAM_PROFILE_EDIT"], "authentifie": true, "acr": "2"}
	}
}

test_modification_d_une_fonctionnalite_refusee_sans_iam_profile_edit if not authz.allow with input as {
	"method": "PUT", "path": "/bff/des/iam/configuration/fonctionnalites/liaisons", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2",
}

# Routes JHipster /services/{service}/** (CRUD généré et points /management des services) : aucune règle ne
# les couvre, OPA les refuse donc par défaut, même à un jeton d'administrateur portant toutes les permissions
# des règles, ROLE_ADMIN et ROLE_SUPER_ADMIN, à deux facteurs, avec ou sans institution. La passerelle ne les
# relaie plus et les refuse (denyAll) : ce refus est une barrière de plus.
toutes_permissions := {r.p | some r in authz.regles} | {
	"ROLE_ADMIN", "ROLE_SUPER_ADMIN", "IAM_ACCOUNT_MANAGE", "IAM_ROLE_PRIVILEGED_GRANT",
	"PLATFORM_CONFIG_EDIT", "PAR_INSTITUTION_EDIT", "HOM_PORTAL_SUBMIT", "COL_MESSAGE_SEND",
}

test_services_refuses_meme_administrateur if {
	every chemin in [
		"/services/iam/api/collaborators",
		"/services/iam/api/collaborators/1",
		"/services/iam/api/admin/users",
		"/services/participant/api/institutions",
		"/services/homologation/api/liaisons",
		"/services/document/api/documents",
		"/services/collaboration/api/conversations",
		"/services/certification/management/env",
		"/services/pilotage/management/loggers/ROOT",
		"/services/gateway/api/admin/users",
	] {
		every methode in ["GET", "POST", "PUT", "PATCH", "DELETE"] {
			every institution in ["", "INST-0042"] {
				not authz.allow with input as {
					"method": methode, "path": chemin, "permissions": toutes_permissions,
					"institution_id": institution, "authentifie": true, "acr": "2",
				}
			}
		}
	}
}

test_services_toute_permission_isolee_refusee if {
	every p in toutes_permissions {
		not authz.allow with input as {
			"method": "GET", "path": "/services/iam/api/collaborators", "permissions": [p],
			"authentifie": true, "acr": "3",
		}
	}
}

# ------------------------------------------------------------------ D-102 : organigramme (affectations, PDF)

affecter(chemin, permissions, institution) := {
	"method": "POST", "path": chemin, "permissions": permissions, "institution_id": institution, "authentifie": true, "acr": "2",
}

test_affectation_des_administration_ou_chef if {
	authz.allow with input as affecter("/bff/des/iam/organisation/unites/SNA/affectations", ["IAM_ACCOUNT_EDIT"], "")
	authz.allow with input as affecter("/bff/des/iam/organisation/unites/SNA/affectations", ["PIL_ACTIVITY_OWN"], "")
}

test_affectation_des_refusee if {
	not authz.allow with input as affecter("/bff/des/iam/organisation/unites/SNA/affectations", ["IAM_ACCOUNT_READ"], "")
	not authz.allow with input as affecter("/bff/des/iam/organisation/unites/SNA/affectations", ["PIL_ACTIVITY_OWN"], "INST-0042")
	not authz.allow with input as object.union(affecter("/bff/des/iam/organisation/unites/SNA/affectations", ["IAM_ACCOUNT_EDIT"], ""), {"acr": "1"})
	not authz.allow with input as affecter("/bff/des/iam/organisation/unites/SNA/affectations/u-1", ["PIL_ACTIVITY_OWN"], "")
	not authz.allow with input as affecter("/bff/portail/iam/organisation/unites/SNA/affectations", ["IAM_ACCOUNT_EDIT"], "INST-0042")
}

test_affectation_console_administration_seulement if {
	authz.allow with input as affecter("/bff/console/iam/organisation/unites/SNA/affectations", ["IAM_ACCOUNT_EDIT"], "")
	not authz.allow with input as affecter("/bff/console/iam/organisation/unites/SNA/affectations", ["PIL_ACTIVITY_OWN"], "")
}

test_retrait_administration_seulement if {
	every couche in ["des", "console"] {
		authz.allow with input as {"method": "DELETE", "path": sprintf("/bff/%s/iam/organisation/unites/SNA/affectations/u-1", [couche]), "permissions": ["IAM_ACCOUNT_EDIT"], "authentifie": true, "acr": "2"}
		not authz.allow with input as {"method": "DELETE", "path": sprintf("/bff/%s/iam/organisation/unites/SNA/affectations/u-1", [couche]), "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2"}
	}
}

test_organigramme_pdf_lecteurs_du_catalogue if {
	authz.allow with input as {"method": "GET", "path": "/bff/des/iam/organisation/organigramme.pdf", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2"}
	authz.allow with input as {"method": "GET", "path": "/bff/console/iam/organisation/organigramme.pdf", "permissions": ["IAM_ACCOUNT_READ"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "GET", "path": "/bff/console/iam/organisation/organigramme.pdf", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "GET", "path": "/bff/des/iam/organisation/organigramme.pdf", "permissions": ["PIL_ACTIVITY_OWN"], "institution_id": "INST-0042", "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "GET", "path": "/bff/des/iam/organisation/organigrammexpdf", "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "GET", "path": "/bff/portail/iam/organisation/organigramme.pdf", "permissions": ["PIL_ACTIVITY_OWN"], "institution_id": "INST-0042", "authentifie": true, "acr": "2"}
}

# Reprise de l'existant (D-107) : console du super administrateur seulement, second facteur exigé ; les routes
# internes de reprise ne sont jamais relayées.
test_reprise_console_super_administrateur if {
	authz.allow with input as {"method": "POST", "path": "/bff/console/participant/reprise/institutions", "permissions": ["REF_REPRISE_IMPORT"], "authentifie": true, "acr": "2"}
	authz.allow with input as {"method": "POST", "path": "/bff/console/homologation/reprise/liaisons", "permissions": ["REF_REPRISE_IMPORT"], "authentifie": true, "acr": "2"}
}

test_reprise_departement_exige_la_permission if {
	not authz.allow with input as {"method": "POST", "path": "/bff/des/participant/reprise/institutions", "permissions": ["PAR_INSTITUTION_CREATE"], "authentifie": true, "acr": "2"}
	authz.allow with input as {"method": "POST", "path": "/bff/des/homologation/reprise/liaisons", "permissions": ["REF_REPRISE_IMPORT"], "authentifie": true, "acr": "2"}
}

test_reprise_sans_permission_refusee if not authz.allow with input as {"method": "POST", "path": "/bff/console/participant/reprise/institutions", "permissions": ["IAM_ACCOUNT_CREATE"], "authentifie": true, "acr": "2"}

test_reprise_sans_second_facteur_refusee if not authz.allow with input as {"method": "POST", "path": "/bff/console/homologation/reprise/liaisons", "permissions": ["REF_REPRISE_IMPORT"], "authentifie": true, "acr": "1"}

test_reprise_interne_jamais_relayee if {
	not authz.allow with input as {"method": "POST", "path": "/bff/console/participant/interne/reprise/institutions", "permissions": ["REF_REPRISE_IMPORT"], "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "POST", "path": "/bff/des/homologation/interne/reprise/liaisons", "permissions": ["REF_REPRISE_IMPORT"], "authentifie": true, "acr": "2"}
}

# D-114 : supervision des liaisons, astreintes et relève de quart (docs/lot1/astreintes-monitoring.md §8).
routes_monitoring := [
	["GET", "/bff/des/monitoring/liaisons", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/liaisons/l1", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/alertes", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/alertes/a1", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/politiques-alerte", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/vacations/en-cours", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/vacations/v1", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/vacations/v1/main-courante", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/vacations/v1/main-courante/verification", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/vacations/v1/rapport", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/rapports/r1", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/grilles", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/grilles/publiee", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/grilles/g1", "MON_SUPERVISION_READ"],
	["GET", "/bff/des/monitoring/grilles/g1/echanges", "MON_SUPERVISION_READ"],
	["POST", "/bff/des/monitoring/alertes/a1/acquittement", "MON_ALERT_ACK"],
	["POST", "/bff/des/monitoring/vacations/v1/main-courante", "MON_SHIFT_OPERATOR"],
	["POST", "/bff/des/monitoring/vacations/v1/rapport", "MON_SHIFT_OPERATOR"],
	["POST", "/bff/des/monitoring/rapports/r1/signature", "MON_SHIFT_OPERATOR"],
	["POST", "/bff/des/monitoring/rapports/r1/contresignature", "MON_SUPERVISION_READ"],
	["POST", "/bff/des/monitoring/rapports/r1/visa", "MON_SHIFT_LEAD"],
	["POST", "/bff/des/monitoring/grilles/generation", "MON_ROSTER_MANAGE"],
	["POST", "/bff/des/monitoring/grilles/g1/publication", "MON_ROSTER_PUBLISH"],
	["POST", "/bff/des/monitoring/grilles/g1/echanges", "MON_SUPERVISION_READ"],
	["POST", "/bff/des/monitoring/echanges/e1/accord", "MON_SUPERVISION_READ"],
	["POST", "/bff/des/monitoring/echanges/e1/refus", "MON_SUPERVISION_READ"],
	["POST", "/bff/des/monitoring/echanges/e1/approbation", "MON_ROSTER_MANAGE"],
	["POST", "/bff/des/monitoring/echanges/e1/rejet", "MON_ROSTER_MANAGE"],
]

test_monitoring_avec_permission if {
	every r in routes_monitoring {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "2"}
	}
}

test_monitoring_sans_permission if {
	every r in routes_monitoring {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["PIL_ACTIVITY_OWN"], "authentifie": true, "acr": "2"}
	}
}

test_monitoring_compte_externe_refuse if {
	every r in routes_monitoring {
		not authz.allow with input as {
			"method": r[0], "path": r[1], "permissions": [r[2]], "institution_id": "INST-0042",
			"authentifie": true, "acr": "2",
		}
	}
}

test_monitoring_sans_second_facteur_refuse if {
	every r in routes_monitoring {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": [r[2]], "authentifie": true, "acr": "1"}
	}
}

# Les permissions sont distinctes : la lecture n'ouvre ni l'acquittement, ni la grille, ni le visa.
actes_monitoring := [r | some r in routes_monitoring; r[2] != "MON_SUPERVISION_READ"]

test_monitoring_lecture_n_ouvre_pas_les_actes if {
	count(actes_monitoring) == 9
	every r in actes_monitoring {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["MON_SUPERVISION_READ"], "authentifie": true, "acr": "2"}
	}
}

test_monitoring_publication_exige_le_chef_du_des if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/monitoring/grilles/g1/publication", "permissions": ["MON_ROSTER_MANAGE", "MON_SHIFT_LEAD"],
	"authentifie": true, "acr": "2",
}

# Chemin ou méthode hors contrat : refusé, sans repli sur @PreAuthorize ; l'interne n'est jamais relayé.
test_monitoring_chemin_non_liste_refuse if not authz.allow with input as {
	"method": "GET", "path": "/bff/des/monitoring/supervised-links", "permissions": ["MON_SUPERVISION_READ"], "authentifie": true, "acr": "2",
}

test_monitoring_suppression_refusee if not authz.allow with input as {
	"method": "DELETE", "path": "/bff/des/monitoring/alertes/a1", "permissions": ["MON_SUPERVISION_READ", "MON_ALERT_ACK"], "authentifie": true, "acr": "2",
}

test_monitoring_interne_refuse if not authz.allow with input as {
	"method": "POST", "path": "/bff/des/monitoring/interne/alertes", "permissions": ["INTERNAL_MONITORING_ALERT_INGEST", "MON_SUPERVISION_READ"],
	"authentifie": true, "acr": "2",
}

# Sandbox GIMAC, administration déléguée (D-115, US-EXI-03) : administrateur de l'institution, institution et second
# facteur exigés ; aucune autre route ni aucune autre méthode.
routes_sbx_habilitations := [
	["GET", "/bff/portail/sbx-habilitations/roles"],
	["GET", "/bff/portail/sbx-habilitations/comptes"],
	["GET", "/bff/portail/sbx-habilitations/comptes/c1"],
	["POST", "/bff/portail/sbx-habilitations/comptes"],
	["PUT", "/bff/portail/sbx-habilitations/comptes/c1/roles"],
	["PUT", "/bff/portail/sbx-habilitations/comptes/c1/etat"],
]

test_sbx_habilitations_administrateur_institution if {
	every r in routes_sbx_habilitations {
		authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["SBX_HAB_ADMINISTRATION"], "institution_id": "99001", "authentifie": true, "acr": "2"}
	}
}

test_sbx_habilitations_refus if {
	every r in routes_sbx_habilitations {
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["SBX_HAB_ADMINISTRATION"], "institution_id": "", "authentifie": true, "acr": "2"}
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["SBX_HAB_CONSULTATION"], "institution_id": "99001", "authentifie": true, "acr": "2"}
		not authz.allow with input as {"method": r[0], "path": r[1], "permissions": ["SBX_HAB_ADMINISTRATION"], "institution_id": "99001", "authentifie": true, "acr": "1"}
	}
	not authz.allow with input as {"method": "DELETE", "path": "/bff/portail/sbx-habilitations/comptes/c1", "permissions": ["SBX_HAB_ADMINISTRATION"], "institution_id": "99001", "authentifie": true, "acr": "2"}
	not authz.allow with input as {"method": "PUT", "path": "/bff/portail/sbx-habilitations/comptes/c1/mot-de-passe", "permissions": ["SBX_HAB_ADMINISTRATION"], "institution_id": "99001", "authentifie": true, "acr": "2"}
}
