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

# D-64 : messagerie instantanée de travail (docs/lot1/messagerie.md §1), DES seulement.
routes_messagerie := [
	["GET", "/bff/des/collaboration/messagerie/annuaire", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/conversations", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/conversations/c1", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/conversations/c1/messages", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/non-lus", "COL_MESSAGE_READ"],
	["GET", "/bff/des/collaboration/messagerie/pieces/p1", "COL_MESSAGE_READ"],
	["POST", "/bff/des/collaboration/messagerie/conversations/c1/lu", "COL_MESSAGE_READ"],
	["POST", "/bff/des/collaboration/messagerie/conversations", "COL_MESSAGE_SEND"],
	["POST", "/bff/des/collaboration/messagerie/conversations/c1/messages", "COL_MESSAGE_SEND"],
]

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
	["POST", "/bff/des/homologation/parcours/operations/o1/preuves", "HOM_EVIDENCE_SUBMIT"],
	["PUT", "/bff/des/homologation/liaisons/l1/ressources", "HOM_STEP_MANAGE"],
	["POST", "/bff/des/homologation/liaisons/l1/blocages", "HOM_STEP_MANAGE"],
	["POST", "/bff/des/homologation/blocages/b1/levee", "HOM_STEP_MANAGE"],
	["POST", "/bff/des/homologation/liaisons/l1/fiche-suivi/remise", "HOM_STEP_MANAGE"],
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

test_parcours_livrable_laisse_au_service if authz.allow with input as {
	"method": "POST", "path": "/bff/des/homologation/parcours/operations/o1/livrable", "permissions": ["HOM_STEP_MANAGE"],
	"authentifie": true, "acr": "2",
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
