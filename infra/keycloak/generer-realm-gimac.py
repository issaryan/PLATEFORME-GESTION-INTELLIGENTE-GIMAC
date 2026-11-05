#!/usr/bin/env python3
"""Domaine Keycloak « gimac » du lot 1 (PI-GIMAC-01, convention transversale et §0.6, §0.7).

Dérivé du realm de développement généré par JHipster (infra/docker-compose/realm-config),
il y ajoute les permissions du lot, un rôle composite par profil, les utilisateurs de test,
le claim institution_id, le client tester-cli et les clients de service. Aucun secret n'est
écrit : mots de passe et secrets sont des variables ${...} résolues par Keycloak à l'import,
à partir du .env local (BNF-07, D-44).

    python3 infra/keycloak/generer-realm-gimac.py
"""
import copy
import json
import pathlib
import uuid

RACINE = pathlib.Path(__file__).resolve().parents[2]
SOURCE = RACINE / "infra/docker-compose/realm-config/jhipster-realm.json"
CIBLE = RACINE / "infra/keycloak/gimac-realm.json"
# Domaine de production : identique, sans aucun compte de test (seuls les comptes de service des clients restent) ;
# les comptes réels sont créés depuis la console (D-62, docs/exploitation/reprise-des-donnees.md).
CIBLE_PRODUCTION = RACINE / "infra/keycloak/gimac-realm-production.json"
# Catalogue des permissions et des profils livrés, lu dans le domaine généré comme MS-IAM le lit dans Keycloak
# (KeycloakAnnuaire : permissions, profils composites ROLE_*, libellé, mission, caractère externe) : source unique
# des services simulés et des profils de démonstration du frontend (recette ADM-01, ADM-09), jamais recopié à la main.
CIBLE_FRONTEND = RACINE / "frontend/packages/lot1/src/domaine-gimac.json"

PERMISSIONS = {
    "HOM": ["PROJECT_DESIGNATE", "REFERENTIEL_READ", "REFERENTIEL_EDIT", "REFERENTIEL_PUBLISH", "REFERENTIEL_MIGRATE", "LIAISON_READ", "LIAISON_LIST",
            # Lecture de tout le référentiel des opérations, hors éditeurs (D-89, docs/lot1/referentiel-droits-etapes.md §1).
            "REFERENTIEL_READ_ALL",
            "LIAISON_OPEN", "LIAISON_UNFREEZE", "OPERATION_ASSIGN", "EVIDENCE_SUBMIT", "OPERATION_VALIDATE", "OPERATION_REOPEN",
            "GRID_EDIT", "REPLAY_ADMIN", "SCENARIO_QUALIFY", "DASHBOARD_READ", "DEADLINE_CONFIG", "EVIDENCE_FILE_EXPORT",
            "PORTAL_READ", "PORTAL_SUBMIT",
            # Parcours de la liaison, étapes 4 à 10 (D-56 à D-59) : conduite des étapes.
            "STEP_MANAGE",
            # Conduite d'un projet d'homologation (D-94 §3) : rend un compte désignable comme conducteur de liaison.
            "PROJECT_CONDUCT",
            # Fonctions opérationnelles du catalogue D-91 sans permission propre (D-97) : leurs porteurs sont les
            # candidats d'une fonction à affecter (tâche à prendre), au lieu d'une tâche au conducteur.
            "FUNCTION_DDRM", "FUNCTION_PRODUCTION", "FUNCTION_NETWORK", "FUNCTION_SYSTEM", "FUNCTION_OPERATIONS"],
    "CER": ["CIRCUIT_READ", "QUITUS_TECHNIQUE_SIGN", "QUITUS_SECURITE_SIGN", "QUITUS_CONFORMITE_SIGN", "QUITUS_DG_SIGN",
            "DOSSIER_READ", "DOSSIER_VERIFY", "CHANGE_RECORD_LINK", "GOLIVE_REPORT_SUBMIT", "ATTESTATION_ISSUE", "REGISTER_READ",
            "REGISTER_EXPORT", "ATTESTATION_STATUS_REQUEST", "ATTESTATION_STATUS_APPROVE", "PORTAL_READ",
            # Verrous de la décision (D-56) : procès-verbal établi par le conducteur, vérification de la sécurité, signature du participant.
            "PV_ESTABLISH", "SECURITY_VERIFY", "PORTAL_SIGN"],
    # Registre des institutions et étapes 1 et 2 du parcours (D-57).
    "PAR": ["INSTITUTION_READ", "INSTITUTION_CREATE", "INSTITUTION_EDIT", "ADHESION_INSTRUCT", "TRANSITION_REQUEST",
            "TRANSITION_APPROVE"],
    # Administration des comptes et de la plateforme depuis la console (D-62).
    "IAM": ["ACCOUNT_READ", "ACCOUNT_CREATE", "ACCOUNT_EDIT", "ACCOUNT_ROLES", "ACCOUNT_STATUS", "ACCOUNT_DELETE",
            "ACCOUNT_SECURITY", "JOURNAL_READ", "PROFILE_EDIT", "ROLE_PRIVILEGED_GRANT"],
    "PLATFORM": ["SUPERVISION_READ", "CONFIG_READ", "CONFIG_EDIT", "SECURITY_POLICY_READ", "SECURITY_POLICY_EDIT",
                 "MESSAGING_EDIT",
                 # Exploitation de l'infrastructure depuis la console (D-78) : lecture, gestes Docker et Kubernetes.
                 "OPS_READ", "OPS_EXECUTE"],
    # Messagerie instantanée de travail entre acteurs internes (D-64, docs/lot1/messagerie.md).
    "COL": ["MESSAGE_READ", "MESSAGE_SEND"],
    # Mon activité (D-76, docs/lot1/mon-activite.md) : agenda, activités, tâches et rapports de chacun ;
    # équipe (lecture, visa des rapports) et tâches confiées aux membres de son équipe.
    "PIL": ["ACTIVITY_OWN", "TEAM_READ", "TASK_ASSIGN"],
    # Notifications de la plateforme (D-80, docs/lot1/notifications.md) : cloche et préférences de chacun.
    "NOT": ["INBOX_READ"],
    # Supervision des liaisons, astreintes et relève de quart (D-114, docs/lot1/astreintes-monitoring.md §2) : lecture,
    # acquittement, qualifications (opérateur en salle, astreinte de niveau 2, chef de quart), grille (génération,
    # publication signée du chef du DES).
    "MON": ["SUPERVISION_READ", "ALERT_ACK", "SHIFT_OPERATOR", "ONCALL_L2", "SHIFT_LEAD", "ROSTER_MANAGE", "ROSTER_PUBLISH"],
    # Administration des référentiels métier de tous les domaines (D-92, docs/lot1/referentiels-metier-admin.md) :
    # non privilégiée au sens d'IAM (ni IAM_ ni PLATFORM_).
    "REF": ["METIER_ADMIN",
            # Reprise de l'existant (D-107, docs/exploitation/reprise-des-donnees.md) : import des institutions et des
            # liaisons par les routes /api/v1/interne/reprise/** ; super administrateur et compte de service reprise-service.
            "REPRISE_IMPORT"],
    "INTERNAL": ["HOMOLOGATION_READ", "CERTIFICATION_READ", "PARTICIPANT_READ", "DOCUMENT_READ", "DOCUMENT_DEPOSIT", "IAM_READ",
                 # Tâches créées par un autre service dans MS-PILOTAGE (D-76 §2.3, D-77).
                 "PILOTAGE_TASK_CREATE",
                 # Titre d'une conversation pour une notification (D-80 §2).
                 "COLLABORATION_READ",
                 # Mise en page PDF d'un document composé par un service (D-102, organigramme de MS-IAM).
                 "DOCUMENT_RENDER",
                 # Réception des alertes des sondes par MS-MONITORING (D-114 §4.2), jeton de service de la source.
                 "MONITORING_ALERT_INGEST",
                 # Sandbox GIMAC (D-119, docs/lot1/sandbox-existant-adapte.md §6) : étapes des sagas de SBX-ESPACE dans MS-IAM
                 # (suspension de périmètre, protection, délégations) et introspection des jetons pour la sandbox.
                 "IAM_SBX_SAGA", "IAM_SBX_INTROSPECT",
                 # Republication des états des liaisons pour la sandbox (D-122), jouée par le compte de service d'exploitation.
                 "HOMOLOGATION_REPUBLISH",
                 # Sandbox GIMAC, US-EXI-05 (D-123) : appels synchrones entre services de la sandbox (CC-SBX-GIMAC-02, tableau
                 # 5.3), une permission par opération appelée ; lecture de SBX-LECTURE par la plateforme du DES ; Mifos.
                 "SBX_RACCORDEMENT_IDENTIFIANTS", "SBX_RACCORDEMENT_LIAISONS", "SBX_RACCORDEMENT_RATTACHABILITE",
                 "SBX_DONNEES_ETATS", "SBX_DONNEES_MOUVEMENTS", "SBX_MOBILE_COUPONS", "SBX_TRACE_REFERENCES",
                 "SBX_CAMPAGNE_REFERENCES_TRACE", "SBX_SANTE", "SBX_MIFOS_API", "SBX_LECTURE_READ"],
}
# Sandbox GIMAC (D-115, docs/lot1/sandbox-existant-adapte.md §2, US-EXI-02) : SBX_<MODULE>_<FONCTION>, douze modules et cinq
# fonctions (CA-SBX-GIMAC-02 UC-INS-04), soit soixante permissions contrôlées côté serveur par les services de la sandbox.
SBX_MODULES = ["HAB", "RAC", "REF", "SCN", "DON", "SIM", "MOB", "ISO", "CAR", "CER", "TRA", "LEC"]
SBX_FONCTIONS = ["ADMINISTRATION", "SIMULATION", "EXECUTION", "VALIDATION", "CONSULTATION"]
PERMISSIONS["SBX"] = [f"{m}_{f}" for m in SBX_MODULES for f in SBX_FONCTIONS]
# Treizième module (D-121, D-119) : supervision de la sandbox depuis la plateforme, consultation seule, attribuée par
# profil depuis la console ; aucun profil livré ne la reçoit d'office. SBX-TRACE l'accepte sur ses routes de supervision.
PERMISSIONS["SBX"].append("SUP_CONSULTATION")
TOUTES = [f"{d}_{p}" for d, ps in PERMISSIONS.items() for p in ps]

LECTURE_DES = ["HOM_REFERENTIEL_READ", "HOM_LIAISON_READ", "HOM_LIAISON_LIST", "HOM_DASHBOARD_READ", "CER_CIRCUIT_READ",
               "CER_DOSSIER_READ", "CER_REGISTER_READ", "PAR_INSTITUTION_READ"]
# Profils par fonction (D-58). Chaque acteur n'a que les actions de sa fonction ; le rôle
# CONDUCTEUR_PROJET habilite à conduire un projet, le projet lui étant confié par désignation.
PROFILS = {
    "ROLE_INGENIEUR_HOMOLOGATION": LECTURE_DES + ["HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_PUBLISH",
        "HOM_OPERATION_ASSIGN", "HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "HOM_OPERATION_REOPEN", "HOM_GRID_EDIT",
        "HOM_REPLAY_ADMIN", "HOM_SCENARIO_QUALIFY", "HOM_EVIDENCE_FILE_EXPORT", "CER_DOSSIER_VERIFY", "CER_GOLIVE_REPORT_SUBMIT",
        "CER_ATTESTATION_ISSUE"],
    # Chefs de département (CR v1.7 §3.6) : tous deux désignent le conducteur de projet (rang 6) ; seul le chef de la DDRM,
    # coordonnateur fonctionnel et porte d'entrée, ouvre la liaison (§4.2.1) ; le chef du DES, coordonnateur technique,
    # affecte les ressources techniques et pilote la certification.
    "ROLE_CHEF_DES": LECTURE_DES + ["HOM_PROJECT_DESIGNATE", "HOM_REFERENTIEL_MIGRATE", "HOM_LIAISON_UNFREEZE", "HOM_OPERATION_VALIDATE",
        "HOM_DEADLINE_CONFIG", "HOM_EVIDENCE_FILE_EXPORT", "CER_QUITUS_TECHNIQUE_SIGN", "CER_ATTESTATION_ISSUE",
        "CER_REGISTER_EXPORT", "CER_ATTESTATION_STATUS_REQUEST", "HOM_STEP_MANAGE"],
    # Super administrateur et RSSI administrent la plateforme depuis la console (D-62) ; seul le
    # super administrateur attribue un profil privilégié et règle la journalisation des services.
    # Sans ROLE_ADMIN : ce rôle technique ouvre le CRUD généré (/api/**) et les points /management des
    # services JHipster, hors OPA et hors règles métier ; seul le compte de service gateway-supervision le
    # porte (lecture des /management par la passerelle). La console passe par /api/plateforme/** (PLATFORM_*).
    "ROLE_SUPER_ADMIN": LECTURE_DES + [f"IAM_{p}" for p in PERMISSIONS["IAM"]]
        + [f"PLATFORM_{p}" for p in PERMISSIONS["PLATFORM"]],
    "ROLE_RSSI": LECTURE_DES + ["CER_QUITUS_SECURITE_SIGN", "CER_SECURITY_VERIFY", "HOM_EVIDENCE_SUBMIT"] + [f"IAM_{p}" for p in PERMISSIONS["IAM"] if p != "ROLE_PRIVILEGED_GRANT"]
        + ["PLATFORM_SUPERVISION_READ", "PLATFORM_CONFIG_READ", "PLATFORM_SECURITY_POLICY_READ", "PLATFORM_SECURITY_POLICY_EDIT",
           "PLATFORM_MESSAGING_EDIT", "PLATFORM_OPS_READ"],
    # Agent de la sécurité des SI, sous le RSSI (CR v1.7 §3.3, §4.5) : vérifie les attestations et archive le dossier,
    # dépose les preuves de la cérémonie de clés et de la saisie des clés carte ; ni administration des comptes ni de la
    # plateforme, ni quitus (séparation des tâches avec le RSSI).
    "ROLE_AGENT_SSI": LECTURE_DES + ["CER_SECURITY_VERIFY", "HOM_EVIDENCE_SUBMIT"],
    "ROLE_CONFORMITE": LECTURE_DES + ["CER_QUITUS_CONFORMITE_SIGN", "CER_CHANGE_RECORD_LINK"],
    "ROLE_DG": LECTURE_DES + ["CER_QUITUS_DG_SIGN", "CER_ATTESTATION_STATUS_APPROVE"],
    # Notifications du portail (D-93 §11.4) : boîte et préférences du correspondant.
    "ROLE_CORRESPONDANT": ["HOM_PORTAL_READ", "HOM_PORTAL_SUBMIT", "CER_PORTAL_READ", "CER_PORTAL_SIGN", "NOT_INBOX_READ"],
    # DDRM : le chargé instruit et propose, le chef prononce (maker-checker, algorithme 6.25).
    "ROLE_CHARGE_DDRM": LECTURE_DES + ["PAR_INSTITUTION_CREATE", "PAR_INSTITUTION_EDIT", "PAR_ADHESION_INSTRUCT",
        "PAR_TRANSITION_REQUEST", "HOM_STEP_MANAGE", "HOM_FUNCTION_DDRM"],
    "ROLE_CHEF_DDRM": LECTURE_DES + ["HOM_LIAISON_OPEN", "HOM_PROJECT_DESIGNATE", "PAR_INSTITUTION_CREATE", "PAR_INSTITUTION_EDIT", "PAR_ADHESION_INSTRUCT",
        "PAR_TRANSITION_REQUEST", "PAR_TRANSITION_APPROVE", "HOM_STEP_MANAGE"],
    # Service Normes et Agréments, production monétique, pôles réseau et système (CR-CERT §3.6).
    "ROLE_SNA": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT", "HOM_OPERATION_VALIDATE", "CER_DOSSIER_VERIFY"],
    # Exploitation et supervision (catalogue D-91) : tenue par la production monétique, sans profil propre (D-58).
    "ROLE_PRODUCTION_MONETIQUE": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT", "CER_CHANGE_RECORD_LINK", "CER_GOLIVE_REPORT_SUBMIT",
        "HOM_FUNCTION_PRODUCTION", "HOM_FUNCTION_OPERATIONS"],
    "ROLE_POLE_RESEAU": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT", "HOM_FUNCTION_NETWORK"],
    "ROLE_POLE_SYSTEME": LECTURE_DES + ["HOM_EVIDENCE_SUBMIT", "HOM_FUNCTION_SYSTEM"],
    # D-76 : tout employé du GIMAC sans fonction métier dans la plateforme, et responsable d'une unité
    # (service, pôle, cellule) ; « Mon activité » et la messagerie leur sont ajoutées ci-dessous.
    "ROLE_COLLABORATEUR": [],
    # Administrateur système (D-78) : exploitation de la plateforme depuis la console (supervision, charges
    # de travail Docker et Kubernetes) ; n'administre pas les comptes.
    "ROLE_ADMIN_SYSTEME": ["PLATFORM_SUPERVISION_READ", "PLATFORM_CONFIG_READ", "PLATFORM_OPS_READ", "PLATFORM_OPS_EXECUTE"],
    "ROLE_RESPONSABLE_SERVICE": [],
    "ROLE_CONDUCTEUR_PROJET": LECTURE_DES + ["HOM_OPERATION_ASSIGN", "HOM_EVIDENCE_SUBMIT", "HOM_EVIDENCE_FILE_EXPORT",
        "HOM_OPERATION_VALIDATE", "HOM_STEP_MANAGE", "CER_PV_ESTABLISH", "HOM_PROJECT_CONDUCT"],
}
# Libellé (attribut gimac_libelle du rôle) et mission (description du rôle) de chaque profil livré (D-86,
# docs/lot1/profils-organisation.md §2) : modifiables ensuite depuis la console, repris partout où un profil
# est affiché. Libellés repris de l'interface (frontend/packages/lot1/src/i18n/ecrans/par.ts).
PRESENTATIONS = {
    "ROLE_INGENIEUR_HOMOLOGATION": ("Ingénieur d'homologation",
        "Conduit les opérations d'homologation des liaisons (référentiel, preuves, validation) et prépare la certification."),
    "ROLE_CHEF_DES": ("Chef du DES",
        "Coordonnateur technique : dirige le Département Exploitation et Supervision, affecte les ressources techniques des projets, désigne les conducteurs de projet et coordonne la certification."),
    "ROLE_SUPER_ADMIN": ("Super administrateur",
        "Administre la plateforme, les comptes et les profils ; seul habilité à attribuer un profil privilégié."),
    "ROLE_RSSI": ("Sécurité des SI",
        "Responsable de la sécurité des systèmes d'information : saisie par la demande de changement, vérifie les attestations et archive le dossier, approuve le changement, conduit la cérémonie de clés et saisit les clés carte ; administre les comptes."),
    "ROLE_AGENT_SSI": ("Agent de la sécurité des SI",
        "Agent de la sécurité des systèmes d'information, sous le RSSI : vérifie les attestations de conformité et archive le dossier, dépose les preuves de la cérémonie de clés et de la saisie des clés carte."),
    "ROLE_CONFORMITE": ("Conformité",
        "Contrôle la conformité des dossiers de certification, rattache les changements et signe le quitus conformité."),
    "ROLE_DG": ("Direction générale",
        "Direction générale du GIMAC : signe le quitus final et approuve les changements de statut des attestations."),
    "ROLE_CORRESPONDANT": ("Correspondant du participant",
        "Représente une institution membre sur le portail : suit l'homologation de sa liaison, dépose les livrables et signe."),
    "ROLE_CHARGE_DDRM": ("Chargé de la DDRM",
        "Instruit les dossiers des institutions membres (adhésion, transitions) et propose les décisions au chef de la DDRM."),
    "ROLE_CHEF_DDRM": ("Chef de la DDRM",
        "Coordonnateur fonctionnel : dirige la DDRM, porte d'entrée des projets ; prononce les décisions sur les institutions membres, ouvre les liaisons, pilote le cadrage, le périmètre et la coordination fonctionnelle, et désigne les conducteurs de projet."),
    "ROLE_SNA": ("Normes et Agréments",
        "Service Normes et Agréments : conduit les essais service par service, constate leur aboutissement et porte la certification (intégration, tests et certification)."),
    "ROLE_PRODUCTION_MONETIQUE": ("Production monétique",
        "Après la certification, paramètre le participant en production, émet la demande de changement et exécute la configuration ; conduit toute la branche mobile, adresses et certificats compris."),
    "ROLE_POLE_RESEAU": ("Pôle réseau",
        "Production informatique, pôle réseau : établit la matrice des flux, contrôle l'atteignabilité des ports et établit le lien d'essai, où s'achève son intervention ; ouvre le port des guichets automatiques des projets délégataires."),
    "ROLE_POLE_SYSTEME": ("Pôle système",
        "Production informatique, pôle système : au changement de production, crée les répertoires de dépôt et met à jour les scripts qui y déposent les fichiers."),
    "ROLE_COLLABORATEUR": ("Collaborateur",
        "Employé du GIMAC sans fonction métier dans la plateforme : messagerie, Mon activité et notifications."),
    "ROLE_ADMIN_SYSTEME": ("Administrateur système",
        "Exploite l'infrastructure de la plateforme (supervision, opérations Docker et Kubernetes) sans administrer les comptes."),
    "ROLE_RESPONSABLE_SERVICE": ("Responsable de service",
        "Dirige un service, un pôle ou une cellule : confie et suit les tâches de son équipe, lit ses activités et vise ses rapports."),
    "ROLE_CONDUCTEUR_PROJET": ("Conducteur de projet",
        "Désigné par projet, assure la coordination opérationnelle au jour le jour du projet de liaison qui lui est confié : affecte les opérations, valide les preuves et établit le procès-verbal de certification."),
}
assert set(PRESENTATIONS) == set(PROFILS), "libellé et mission de chaque profil livré"
assert all(1 <= len(l) <= 80 and len(d) <= 500 and "\u2014" not in l + d for l, d in PRESENTATIONS.values())
# Messagerie de travail (D-64) : tous les profils internes du GIMAC, jamais le correspondant d'un
# participant (profil externe, porteur d'une institution).
PROFILS_EXTERNES = {"ROLE_CORRESPONDANT"}
MESSAGERIE = ["COL_MESSAGE_READ", "COL_MESSAGE_SEND"]
# Mon activité (D-76 §4) : tout profil interne ; équipe et tâches confiées pour les responsables.
MON_ACTIVITE = ["PIL_ACTIVITY_OWN", "NOT_INBOX_READ"]
RESPONSABLES = {"ROLE_CHEF_DES", "ROLE_CHEF_DDRM", "ROLE_DG", "ROLE_RSSI", "ROLE_CONFORMITE", "ROLE_RESPONSABLE_SERVICE"}
# Référentiel des opérations (D-89, docs/lot1/referentiel-droits-etapes.md §1) : éditeurs (ingénieur d'homologation,
# chefs de département, chef de service, super administrateur), publieurs, lecture complète (DG, conformité, RSSI,
# chefs de département) ; tout autre lecteur ne reçoit que les opérations qui le concernent (MS-HOMOLOGATION).
REFERENTIEL_EDITEURS = {"ROLE_INGENIEUR_HOMOLOGATION", "ROLE_CHEF_DES", "ROLE_CHEF_DDRM", "ROLE_RESPONSABLE_SERVICE", "ROLE_SUPER_ADMIN"}
REFERENTIEL_PUBLIEURS = {"ROLE_INGENIEUR_HOMOLOGATION", "ROLE_CHEF_DES", "ROLE_CHEF_DDRM", "ROLE_SUPER_ADMIN"}
REFERENTIEL_LECTURE_COMPLETE = {"ROLE_DG", "ROLE_CONFORMITE", "ROLE_RSSI", "ROLE_CHEF_DES", "ROLE_CHEF_DDRM"}
# Ouverture d'une liaison (CR v1.7 §4.2.1 : « Celui-ci ouvre le projet ») : la DDRM seule, par son chef.
assert [n for n, ps in PROFILS.items() if "HOM_LIAISON_OPEN" in ps] == ["ROLE_CHEF_DDRM"]
# Agent de la sécurité des SI : ni administration des comptes ou de la plateforme, ni quitus.
assert not [p for p in PROFILS["ROLE_AGENT_SSI"] if p.startswith(("IAM_", "PLATFORM_", "CER_QUITUS_"))]
# Sandbox GIMAC (D-115 §2, CA-SBX-GIMAC-02 UC-INS-04) : valeurs de départ, configurables ensuite depuis la console (D-86,
# écran des profils), sans seconde administration des droits. Les fonctions du GIMAC s'ajoutent aux profils existants
# (identité unique) ; les six rôles de portée institution sont des profils externes ROLE_SBX_* du portail. Le prestataire
# de maintenance passe par l'accès temporaire par délégation (US-EXI-24), pas par un profil. La validation des verdicts
# (SBX_*_VALIDATION) n'est jamais donnée à un profil externe, et MS-IAM refuse de l'y ajouter (RG-05, ReglesSandbox).
def _sbx(*couples):
    """Permissions SBX : couples (modules, fonctions), « * » pour tous les modules."""
    l = []
    for modules, fonctions in couples:
        for m in (SBX_MODULES if modules == "*" else modules.split()):
            for f in fonctions.split():
                if f"SBX_{m}_{f}" not in l:
                    l.append(f"SBX_{m}_{f}")
    return l
INTERFACES_SBX = "SIM MOB ISO CAR"
SANDBOX_GIMAC = {
    # Administrateur GIMAC de la sandbox.
    "ROLE_SUPER_ADMIN": _sbx(("*", "ADMINISTRATION CONSULTATION")),
    # Responsable de produit : lecture de toute la sandbox.
    "ROLE_CHEF_DES": _sbx(("*", "CONSULTATION")),
    # Ingénieur certificateur : seul à valider les verdicts.
    "ROLE_SNA": _sbx(("CER", "EXECUTION VALIDATION CONSULTATION"), (INTERFACES_SBX, "SIMULATION CONSULTATION"),
                     ("RAC REF SCN DON TRA LEC", "CONSULTATION")),
    # Certificats, clés et plages de BIN de test, périmètre du compte de service du DES, conservation ; approbation
    # du périmètre de lecture et du rattachement du certificat client (CC-SBX-GIMAC-02 §D.8, UC-LEC-10).
    "ROLE_RSSI": _sbx(("RAC DON TRA LEC", "ADMINISTRATION"), ("LEC", "VALIDATION"), ("*", "CONSULTATION")),
    # Plages de BIN, profils de cartes, clés de test et paramètres ISO 8583 ; exploitation (catalogue D-91).
    "ROLE_PRODUCTION_MONETIQUE": _sbx(("CAR DON TRA", "ADMINISTRATION"), ("CAR DON", "SIMULATION"), ("RAC REF TRA CAR DON SIM CER LEC", "CONSULTATION")),
    # Exploitant de la plateforme.
    "ROLE_ADMIN_SYSTEME": _sbx(("TRA", "ADMINISTRATION CONSULTATION"), ("RAC SIM CER LEC", "CONSULTATION")),
}
SANDBOX_INSTITUTIONS = {
    "ROLE_SBX_ADMIN_INSTITUTION": _sbx(("HAB", "ADMINISTRATION CONSULTATION"), ("RAC CER TRA", "CONSULTATION")),
    "ROLE_SBX_CHEF_PROJET": _sbx(("CER", "EXECUTION CONSULTATION"), (INTERFACES_SBX, "SIMULATION CONSULTATION"),
                                 ("RAC DON SCN TRA", "CONSULTATION")),
    # Le développeur exécute les cas qui lui sont affectés (CA-SBX-GIMAC-02 UC-CER-04, acteur principal).
    "ROLE_SBX_DEVELOPPEUR": _sbx(("RAC", "ADMINISTRATION CONSULTATION"), ("CER", "EXECUTION CONSULTATION"),
                                 ("DON " + INTERFACES_SBX, "SIMULATION CONSULTATION"), ("SCN TRA", "CONSULTATION")),
    "ROLE_SBX_TESTEUR": _sbx(("CER " + INTERFACES_SBX, "EXECUTION CONSULTATION"), ("TRA", "CONSULTATION")),
    "ROLE_SBX_LECTEUR": _sbx(("CER RAC " + INTERFACES_SBX, "CONSULTATION")),
    "ROLE_SBX_PARTENAIRE": _sbx(("RAC", "ADMINISTRATION CONSULTATION"), ("DON " + INTERFACES_SBX, "SIMULATION CONSULTATION"),
                                ("CER", "EXECUTION CONSULTATION"), ("SCN TRA", "CONSULTATION")),
}
PRESENTATIONS_SANDBOX = {
    "ROLE_SBX_ADMIN_INSTITUTION": ("Sandbox : administrateur de l'institution",
        "Crée, suspend et rétablit les comptes de ses collaborateurs dans la sandbox, accorde les mandats des partenaires techniques et désigne le chef de projet."),
    "ROLE_SBX_CHEF_PROJET": ("Sandbox : chef de projet participant",
        "Pilote la certification de son institution dans la sandbox : demande les campagnes, affecte les cas, suit l'avancement et les bilans."),
    "ROLE_SBX_DEVELOPPEUR": ("Sandbox : développeur du participant",
        "Raccorde le système de son institution à la sandbox, conduit ses essais libres et consulte ses chemins d'exécution et ses traces."),
    "ROLE_SBX_TESTEUR": ("Sandbox : testeur du participant",
        "Exécute dans la sandbox les cas qui lui sont affectés dans une campagne et consigne ses observations."),
    "ROLE_SBX_LECTEUR": ("Sandbox : lecteur du participant",
        "Consulte dans la sandbox, sans rien modifier, les campagnes, les résultats et les bilans de son institution."),
    "ROLE_SBX_PARTENAIRE": ("Sandbox : partenaire technique mandaté",
        "Intégrateur ou éditeur mandaté par une institution, pour une durée définie et révocable, dans la limite de son mandat."),
}
assert set(PRESENTATIONS_SANDBOX) == set(SANDBOX_INSTITUTIONS)
assert all(1 <= len(l) <= 80 and len(d) <= 500 and "\u2014" not in l + d for l, d in PRESENTATIONS_SANDBOX.values())
for _profil, _ajouts in SANDBOX_GIMAC.items():
    PROFILS[_profil] = PROFILS[_profil] + [x for x in _ajouts if x not in PROFILS[_profil]]
PROFILS.update({n: ps + ["NOT_INBOX_READ"] for n, ps in SANDBOX_INSTITUTIONS.items()})
PRESENTATIONS.update(PRESENTATIONS_SANDBOX)
PROFILS_EXTERNES = PROFILS_EXTERNES | set(SANDBOX_INSTITUTIONS)
# RG-05 : la validation des verdicts reste au GIMAC, à l'ingénieur certificateur (Normes et Agréments) seul.
assert not [n for n in PROFILS_EXTERNES if any(p.startswith("SBX_") and p.endswith("_VALIDATION") for p in PROFILS[n])]
assert [n for n, ps in PROFILS.items() if "SBX_CER_VALIDATION" in ps] == ["ROLE_SNA"]
# Périmètre de lecture du compte de service du DES : proposé par l'administrateur GIMAC, approuvé par le RSSI seul.
assert [n for n, ps in PROFILS.items() if "SBX_LEC_VALIDATION" in ps] == ["ROLE_RSSI"]
# Exemples de US-EXI-02 : un testeur exécute, n'administre pas ; le lecteur n'a pas les traces.
assert "SBX_MOB_EXECUTION" in PROFILS["ROLE_SBX_TESTEUR"] and "SBX_MOB_ADMINISTRATION" not in PROFILS["ROLE_SBX_TESTEUR"]
assert "SBX_TRA_CONSULTATION" not in PROFILS["ROLE_SBX_LECTEUR"]
# Administration des référentiels métier (D-92) : super administrateur et ingénieur d'homologation seulement ; le
# super administrateur porte aussi tous les droits du référentiel des opérations (lecture complète, migration).
REFERENTIELS_METIER_ADMIN = {"ROLE_SUPER_ADMIN", "ROLE_INGENIEUR_HOMOLOGATION"}
for _profil, _ajouts in (
    *((p, ["HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_EDIT"]) for p in sorted(REFERENTIEL_EDITEURS)),
    *((p, ["REF_METIER_ADMIN"]) for p in sorted(REFERENTIELS_METIER_ADMIN)),
    ("ROLE_SUPER_ADMIN", ["HOM_REFERENTIEL_READ_ALL", "HOM_REFERENTIEL_MIGRATE", "REF_REPRISE_IMPORT"]),
    *((p, ["HOM_REFERENTIEL_PUBLISH"]) for p in sorted(REFERENTIEL_PUBLIEURS)),
    *((p, ["HOM_REFERENTIEL_READ_ALL"]) for p in sorted(REFERENTIEL_LECTURE_COMPLETE)),
):
    PROFILS[_profil] = PROFILS[_profil] + [x for x in _ajouts if x not in PROFILS[_profil]]
for _profil in PROFILS:
    if _profil not in PROFILS_EXTERNES:
        PROFILS[_profil] = PROFILS[_profil] + MESSAGERIE + MON_ACTIVITE
        if _profil in RESPONSABLES:
            PROFILS[_profil] = PROFILS[_profil] + ["PIL_TEAM_READ", "PIL_TASK_ASSIGN"]
# Supervision et astreintes (D-114, docs/lot1/astreintes-monitoring.md §2) : valeurs de départ, configurables ensuite
# (D-86). La production monétique tient l'exploitation et la supervision (catalogue D-91) ; les pôles et l'administrateur
# système tiennent les vacations ; le chef du DES génère, publie la grille et reçoit le dernier palier d'escalade.
SUPERVISION = {
    "ROLE_CHEF_DES": ["MON_SUPERVISION_READ", "MON_ALERT_ACK", "MON_ONCALL_L2", "MON_SHIFT_LEAD", "MON_ROSTER_MANAGE", "MON_ROSTER_PUBLISH"],
    "ROLE_PRODUCTION_MONETIQUE": ["MON_SUPERVISION_READ", "MON_ALERT_ACK", "MON_SHIFT_OPERATOR", "MON_ONCALL_L2", "MON_SHIFT_LEAD"],
    "ROLE_POLE_RESEAU": ["MON_SUPERVISION_READ", "MON_ALERT_ACK", "MON_SHIFT_OPERATOR", "MON_ONCALL_L2"],
    "ROLE_POLE_SYSTEME": ["MON_SUPERVISION_READ", "MON_ALERT_ACK", "MON_SHIFT_OPERATOR", "MON_ONCALL_L2"],
    "ROLE_ADMIN_SYSTEME": ["MON_SUPERVISION_READ", "MON_ALERT_ACK", "MON_SHIFT_OPERATOR"],
    "ROLE_SUPER_ADMIN": ["MON_SUPERVISION_READ"],
    "ROLE_RSSI": ["MON_SUPERVISION_READ"],
}
for _profil, _ajouts in SUPERVISION.items():
    PROFILS[_profil] = PROFILS[_profil] + [x for x in _ajouts if x not in PROFILS[_profil]]
assert [n for n, ps in PROFILS.items() if "MON_ROSTER_PUBLISH" in ps] == ["ROLE_CHEF_DES"]
# Libellés propres de certaines permissions (description du rôle Keycloak), à défaut « Permission du lot 1 ».
LIBELLES_PERMISSIONS = {"REF_METIER_ADMIN": "Administration des référentiels métier, tous domaines",
                        "REF_REPRISE_IMPORT": "Reprise de l'existant : import des institutions et des liaisons (simulation ou réel)",
                        "HOM_PROJECT_CONDUCT": "Conduite des projets d'homologation (désignable comme conducteur de liaison)",
                        "HOM_FUNCTION_DDRM": "Tenir la fonction DDRM dans un projet d'homologation",
                        "HOM_FUNCTION_PRODUCTION": "Tenir la fonction Production monétique dans un projet d'homologation",
                        "HOM_FUNCTION_NETWORK": "Tenir la fonction Pôle réseau dans un projet d'homologation",
                        "HOM_FUNCTION_SYSTEM": "Tenir la fonction Pôle système dans un projet d'homologation",
                        "HOM_FUNCTION_OPERATIONS": "Tenir la fonction Exploitation et Supervision dans un projet d'homologation",
                        "MON_SUPERVISION_READ": "Supervision : liaisons, alertes, vacations, grille d'astreinte",
                        "MON_ALERT_ACK": "Acquitter une alerte (membre de la vacation en cours)",
                        "MON_SHIFT_OPERATOR": "Opérateur de supervision en salle (niveau 1) : main courante et relève",
                        "MON_ONCALL_L2": "Astreinte joignable (niveau 2)",
                        "MON_SHIFT_LEAD": "Habilitation de chef de quart : visa de la relève",
                        "MON_ROSTER_MANAGE": "Générer la grille d'astreinte et approuver les échanges",
                        "MON_ROSTER_PUBLISH": "Publier la grille d'astreinte (signature du chef du DES)",
                        "INTERNAL_MONITORING_ALERT_INGEST": "Réception des alertes des sondes par MS-MONITORING",
                        "INTERNAL_IAM_SBX_SAGA": "Sandbox : étapes des sagas de SBX-ESPACE dans MS-IAM (périmètres, protections, délégations)",
                        "INTERNAL_IAM_SBX_INTROSPECT": "Sandbox : introspection des jetons par MS-IAM (jeton interne réduit)",
                        "INTERNAL_HOMOLOGATION_REPUBLISH": "Sandbox : republication des états des liaisons par MS-HOMOLOGATION",
                        "INTERNAL_SBX_RACCORDEMENT_IDENTIFIANTS": "Sandbox, appel de SBX-RACCORDEMENT : vérification d'un identifiant d'accès de test",
                        "INTERNAL_SBX_RACCORDEMENT_LIAISONS": "Sandbox, appel de SBX-RACCORDEMENT : disponibilité des liaisons de test",
                        "INTERNAL_SBX_RACCORDEMENT_RATTACHABILITE": "Sandbox, appel de SBX-RACCORDEMENT : rattachabilité d'un participant",
                        "INTERNAL_SBX_DONNEES_ETATS": "Sandbox, appel de SBX-DONNEES : restauration de l'état initial et relevé des soldes",
                        "INTERNAL_SBX_DONNEES_MOUVEMENTS": "Sandbox, appel de SBX-DONNEES : mouvements, comptes, cartes, PIN et clés de travail",
                        "INTERNAL_SBX_MOBILE_COUPONS": "Sandbox, appel de SBX-MOBILE : coupons de retrait sans carte",
                        "INTERNAL_SBX_TRACE_REFERENCES": "Sandbox, appel de SBX-TRACE : référence de trace d'un essai",
                        "INTERNAL_SBX_CAMPAGNE_REFERENCES_TRACE": "Sandbox, appel de SBX-CAMPAGNE : références de trace protégées",
                        "INTERNAL_SBX_SANTE": "Sandbox : lecture des points de santé des onze services par SBX-TRACE",
                        "INTERNAL_SBX_MIFOS_API": "Sandbox : interfaces publiques du cœur bancaire simulé Mifos",
                        "INTERNAL_SBX_LECTURE_READ": "Sandbox : lecture du contrat SBX-LECTURE par la plateforme du DES"}
_MODULES_SBX = {"HAB": "habilitations", "RAC": "raccordements", "REF": "référentiels", "SCN": "scénarios", "DON": "données de test",
                "SIM": "simulation", "MOB": "interface mobile", "ISO": "interface ISO 20022", "CAR": "interface carte",
                "CER": "campagnes", "TRA": "traces", "LEC": "contrat de lecture"}
_FONCTIONS_SBX = {"ADMINISTRATION": "administration", "SIMULATION": "simulation", "EXECUTION": "exécution de campagne",
                  "VALIDATION": "validation de verdict", "CONSULTATION": "consultation"}
LIBELLES_PERMISSIONS.update({f"SBX_{m}_{f}": f"Sandbox, {_MODULES_SBX[m]} : {_FONCTIONS_SBX[f]}" for m in SBX_MODULES for f in SBX_FONCTIONS})
LIBELLES_PERMISSIONS["SBX_SUP_CONSULTATION"] = "Sandbox, supervision depuis la plateforme : consultation"
assert len(PERMISSIONS["SBX"]) == 61 and not [n for n, ps in PROFILS.items() if "SBX_SUP_CONSULTATION" in ps], "SBX_SUP_CONSULTATION"

# D-94 §3 : le conducteur de projet est le porteur de HOM_PROJECT_CONDUCT, donnée par défaut au profil conducteur seul.
assert [n for n, ps in PROFILS.items() if "HOM_PROJECT_CONDUCT" in ps] == ["ROLE_CONDUCTEUR_PROJET"]
# D-97 : permission de chaque fonction opérationnelle, donnée aux profils que le catalogue des fonctions (D-91,
# valeurs de départ de MS-HOMOLOGATION) associe à la fonction ; EXPLOITATION est tenue par la production monétique.
FONCTIONS_OPERATIONNELLES = {"HOM_FUNCTION_DDRM": ["ROLE_CHARGE_DDRM"], "HOM_FUNCTION_PRODUCTION": ["ROLE_PRODUCTION_MONETIQUE"],
                             "HOM_FUNCTION_NETWORK": ["ROLE_POLE_RESEAU"], "HOM_FUNCTION_SYSTEM": ["ROLE_POLE_SYSTEME"],
                             "HOM_FUNCTION_OPERATIONS": ["ROLE_PRODUCTION_MONETIQUE"]}
for _permission, _profils in FONCTIONS_OPERATIONNELLES.items():
    assert [n for n, ps in PROFILS.items() if _permission in ps] == _profils, _permission
assert all(p in TOUTES for p in LIBELLES_PERMISSIONS)
assert all("REF_METIER_ADMIN" in ps for n, ps in PROFILS.items() if n in REFERENTIELS_METIER_ADMIN)
# D-107 : la reprise de l'existant n'est ouverte qu'au super administrateur (et au compte de service reprise-service).
assert [n for n, ps in PROFILS.items() if "REF_REPRISE_IMPORT" in ps] == ["ROLE_SUPER_ADMIN"]
assert [n for n, ps in PROFILS.items() if "REF_METIER_ADMIN" in ps] == [n for n in PROFILS if n in REFERENTIELS_METIER_ADMIN]
assert {"HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL", "HOM_REFERENTIEL_EDIT", "HOM_REFERENTIEL_PUBLISH",
        "HOM_REFERENTIEL_MIGRATE"} <= set(PROFILS["ROLE_SUPER_ADMIN"])
# Institutions fictives du jeu de données de référence (D-30).
UTILISATEURS = [
    ("test-ingenieur-homologation", "Ingénieur", "Homologation", "ROLE_INGENIEUR_HOMOLOGATION", None),
    ("test-chef-des", "Chef", "DES", "ROLE_CHEF_DES", None),
    ("test-super-admin", "Super", "Administrateur", "ROLE_SUPER_ADMIN", None),
    ("test-rssi", "Responsable", "Sécurité", "ROLE_RSSI", None),
    ("test-agent-ssi", "Agent", "Sécurité des SI", "ROLE_AGENT_SSI", None),
    ("test-conformite", "Responsable", "Conformité", "ROLE_CONFORMITE", None),
    ("test-dg", "Direction", "Générale", "ROLE_DG", None),
    ("test-correspondant-a", "Correspondant", "Établissement de paiement A", "ROLE_CORRESPONDANT", "INST-0042"),
    ("test-correspondant-b", "Correspondant", "Banque B", "ROLE_CORRESPONDANT", "INST-0043"),
    ("test-charge-ddrm", "Chargé", "DDRM", "ROLE_CHARGE_DDRM", None),
    ("test-chef-ddrm", "Chef", "DDRM", "ROLE_CHEF_DDRM", None),
    ("test-sna", "Agent", "Normes et Agréments", "ROLE_SNA", None),
    ("test-production-monetique", "Agent", "Production monétique", "ROLE_PRODUCTION_MONETIQUE", None),
    ("test-pole-reseau", "Agent", "Pôle réseau", "ROLE_POLE_RESEAU", None),
    ("test-pole-systeme", "Agent", "Pôle système", "ROLE_POLE_SYSTEME", None),
    ("test-conducteur-projet", "Conducteur", "Projet", "ROLE_CONDUCTEUR_PROJET", None),
    ("test-collaborateur", "Agent", "GIMAC", "ROLE_COLLABORATEUR", None),
    ("test-responsable-service", "Responsable", "Service", "ROLE_RESPONSABLE_SERVICE", None),
    ("test-admin-systeme", "Administrateur", "Système", "ROLE_ADMIN_SYSTEME", None),
]
SERVICES = {
    # Livrables du portail déposés pour le correspondant dans MS-DOCUMENT (étapes 4 à 10).
    "homologation-service": ["INTERNAL_PARTICIPANT_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_DEPOSIT", "INTERNAL_IAM_READ",
                             "INTERNAL_PILOTAGE_TASK_CREATE"],
    # Attestations du participant déposées pour son compte dans MS-DOCUMENT (verrou 2).
    "certification-service": ["INTERNAL_HOMOLOGATION_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_DEPOSIT", "INTERNAL_IAM_READ"],
    "participant-service": ["INTERNAL_DOCUMENT_READ", "INTERNAL_IAM_READ"],
    # Messagerie (D-64) : annuaire des comptes internes (MS-IAM), dépôt et lecture des pièces jointes (MS-DOCUMENT).
    "collaboration-service": ["INTERNAL_IAM_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_DEPOSIT"],
    # Mon activité (D-76) : rattachements et équipes (MS-IAM), documents des rapports (MS-DOCUMENT). Processus d'unité
    # (D-87) : unités et membres, pouvoirs, vérification des assertions FIDO2 SIGNATURE_PROCESSUS (MS-IAM, routes
    # /api/v1/interne/** sous INTERNAL_IAM_READ), livrables PROCESSUS_UNITE (GET /api/v1/interne/documents/{id}).
    "pilotage-service": ["INTERNAL_IAM_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_RENDER"],
    # Supervision et astreintes (D-114) : opérateurs qualifiés, absences validées et vérification des assertions FIDO2
    # (MS-IAM), dépôt des rapports de quart scellés (MS-DOCUMENT) ; programme en PDF (D-117) : mise en page par MS-DOCUMENT.
    "monitoring-service": ["INTERNAL_IAM_READ", "INTERNAL_DOCUMENT_READ", "INTERNAL_DOCUMENT_DEPOSIT", "INTERNAL_DOCUMENT_RENDER"],
    # Notifications (D-80) : noms des comptes (MS-IAM) et titre des conversations (MS-COLLABORATION).
    "notification-service": ["INTERNAL_IAM_READ", "INTERNAL_COLLABORATION_READ"],
    # MS-IAM administre le realm par l'API d'administration (D-62) : rôles de realm-management ci-dessous. Organigramme en
    # PDF (D-102) : mise en page par MS-DOCUMENT (POST /api/v1/interne/rendus/pdf).
    "iam-service": ["INTERNAL_DOCUMENT_RENDER"],
    # Connexion sur la plateforme (D-65) : la passerelle lit l'état de connexion du compte sur MS-IAM.
    "gateway-service": ["INTERNAL_IAM_READ",
                        # Introspection des jetons pour la sandbox (D-119) : décision de MS-IAM pour le jeton interne réduit.
                        "INTERNAL_IAM_SBX_INTROSPECT"],
    # Sandbox GIMAC (D-119) : SBX-ESPACE conduit ses sagas dans MS-IAM (INTERNAL_IAM_SBX_SAGA seul, sans INTERNAL_IAM_READ)
    # et lit la fiche d'une institution dans MS-PARTICIPANT (US-EXI-21).
    "sbx-espace": ["INTERNAL_IAM_SBX_SAGA", "INTERNAL_PARTICIPANT_READ"],
    # Republication des états des liaisons pour la sandbox (D-122, docs/lot1/sandbox-existant-adapte.md §7.3), jouée à la
    # mise en service de SBX-CAMPAGNE et de SBX-LECTURE ; aucune autre permission.
    "exploitation-service": ["INTERNAL_HOMOLOGATION_REPUBLISH"],
    # Reprise de l'existant (D-107) : outil infra/reprise/importer-reprise.py, jeton client_credentials, routes
    # /api/v1/interne/reprise/** de MS-PARTICIPANT et MS-HOMOLOGATION ; aucune autre permission.
    "reprise-service": ["REF_REPRISE_IMPORT"],
    # Supervision de la passerelle : lecture des points /management des services (ROLE_ADMIN).
    "gateway-supervision": ["ROLE_ADMIN"],
}
# US-EXI-05 (D-123, docs/lot1/sandbox-existant-adapte.md §9) : un compte de service par service de la sandbox, plus le compte
# dédié au cœur bancaire simulé Mifos, et des autorisations d'appel limitées au tableau des appels synchrones du
# CC-SBX-GIMAC-02 (tableau 5.3) : (appelant, permission portée, objet). Chaque appelé n'accepte que la permission de
# l'opération appelée ; aucun autre appel n'est autorisé (matrice vérifiée ci-dessous et par le test du générateur).
APPELS_SBX = [
    ("sbx-espace", "INTERNAL_PARTICIPANT_READ", "MS-PARTICIPANT : fiche d'une institution"),
    ("sbx-espace", "INTERNAL_IAM_SBX_SAGA", "MS-IAM : comptes, rôles, délégations (saga)"),
    ("sbx-raccordement", "INTERNAL_PARTICIPANT_READ", "MS-PARTICIPANT : code participant et nomenclature des services"),
    ("sbx-referentiel", "INTERNAL_PARTICIPANT_READ", "MS-PARTICIPANT : code participant et nomenclature des services"),
    ("sbx-mobile", "INTERNAL_SBX_RACCORDEMENT_IDENTIFIANTS", "SBX-RACCORDEMENT : vérification d'un identifiant d'accès de test"),
    ("sbx-iso20022", "INTERNAL_SBX_RACCORDEMENT_IDENTIFIANTS", "SBX-RACCORDEMENT : vérification d'un identifiant d'accès de test"),
    ("sbx-simulation", "INTERNAL_SBX_TRACE_REFERENCES", "SBX-TRACE : référence de trace au démarrage de l'essai"),
    ("sbx-simulation", "INTERNAL_SBX_DONNEES_ETATS", "SBX-DONNEES : restauration de l'état initial, relevé des soldes"),
    ("sbx-simulation", "INTERNAL_SBX_RACCORDEMENT_LIAISONS", "SBX-RACCORDEMENT : disponibilité des liaisons de test"),
    ("sbx-mobile", "INTERNAL_SBX_DONNEES_MOUVEMENTS", "SBX-DONNEES : mouvements, comptes, cartes, PIN, clés de travail"),
    ("sbx-iso20022", "INTERNAL_SBX_DONNEES_MOUVEMENTS", "SBX-DONNEES : mouvements, comptes, cartes, PIN, clés de travail"),
    ("sbx-carte", "INTERNAL_SBX_DONNEES_MOUVEMENTS", "SBX-DONNEES : mouvements, comptes, cartes, PIN, clés de travail"),
    ("sbx-carte", "INTERNAL_SBX_MOBILE_COUPONS", "SBX-MOBILE : coupon de retrait sans carte"),
    ("sbx-campagne", "INTERNAL_SBX_RACCORDEMENT_RATTACHABILITE", "SBX-RACCORDEMENT : rattachabilité d'un participant"),
    ("sbx-donnees", "INTERNAL_SBX_RACCORDEMENT_RATTACHABILITE", "SBX-RACCORDEMENT : rattachabilité d'un participant"),
    ("sbx-trace", "INTERNAL_SBX_CAMPAGNE_REFERENCES_TRACE", "SBX-CAMPAGNE : références de trace protégées avant purge"),
    ("sbx-campagne", "INTERNAL_DOCUMENT_DEPOSIT", "MS-DOCUMENT : dépôt du bilan (US-EXI-08)"),
    ("sbx-trace", "INTERNAL_DOCUMENT_DEPOSIT", "MS-DOCUMENT : dépôt des traces exportées (US-EXI-08)"),
    ("sbx-iso20022", "INTERNAL_DOCUMENT_DEPOSIT", "MS-DOCUMENT : dépôt du rapport d'écarts (US-EXI-08)"),
    ("sbx-mifos", "INTERNAL_SBX_MIFOS_API", "Mifos : comptes, soldes et écritures par ses interfaces publiques (tenu par SBX-DONNEES)"),
    ("sbx-trace", "INTERNAL_SBX_SANTE", "Les onze services : points de santé"),
    ("homologation-service", "INTERNAL_SBX_LECTURE_READ", "SBX-LECTURE : campagnes, exécutions, taux et pièces (plateforme du DES)"),
]
SERVICES_SBX = ["sbx-espace", "sbx-raccordement", "sbx-referentiel", "sbx-donnees", "sbx-simulation", "sbx-mobile", "sbx-iso20022",
                "sbx-carte", "sbx-campagne", "sbx-trace", "sbx-lecture", "sbx-mifos"]
for _client in SERVICES_SBX:
    SERVICES.setdefault(_client, [])
for _client, _permission, _ in APPELS_SBX:
    if _permission not in SERVICES[_client]:
        SERVICES[_client].append(_permission)
ROLES_CLIENT_SERVICES = {
    "iam-service": {"realm-management": ["manage-users", "view-users", "query-users", "query-groups", "manage-realm",
                                         "view-realm"]},
}

# Profil utilisateur déclaratif (Keycloak 26) : les attributs de la plateforme sont déclarés, modifiables
# par l'administration seulement ; l'utilisateur les renseigne par « Mon compte » (MS-IAM), jamais par Keycloak.
ADMIN_SEUL = {"view": ["admin"], "edit": ["admin"]}
PROFIL_UTILISATEUR = {
    "unmanagedAttributePolicy": "ADMIN_EDIT",
    "attributes": [
        {"name": "username", "displayName": "${username}", "permissions": {"view": ["admin", "user"], "edit": ["admin"]},
         "validations": {"length": {"min": 3, "max": 255}, "username-prohibited-characters": {}, "up-username-not-idn-homograph": {}}},
        {"name": "email", "displayName": "${email}", "required": {"roles": ["user"]},
         "permissions": {"view": ["admin", "user"], "edit": ["admin"]}, "validations": {"email": {}, "length": {"max": 255}}},
        {"name": "firstName", "displayName": "${firstName}", "required": {"roles": ["user"]},
         "permissions": {"view": ["admin", "user"], "edit": ["admin", "user"]},
         "validations": {"length": {"max": 255}, "person-name-prohibited-characters": {}}},
        {"name": "lastName", "displayName": "${lastName}", "required": {"roles": ["user"]},
         "permissions": {"view": ["admin", "user"], "edit": ["admin", "user"]},
         "validations": {"length": {"max": 255}, "person-name-prohibited-characters": {}}},
        {"name": "institution_id", "displayName": "Institution", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "phone_number", "displayName": "Téléphone", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "fonction", "displayName": "Fonction", "permissions": ADMIN_SEUL, "multivalued": False},
        # Rattachement organisationnel (D-76, docs/lot1/mon-activite.md §1.2) : code de l'unité la plus fine
        # (catalogue unite_organisationnelle de MS-IAM) et identifiant du compte responsable (n+1).
        {"name": "unite", "displayName": "Unité organisationnelle", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "responsable_id", "displayName": "Responsable (n+1)", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "profil_complet", "displayName": "Premier accès terminé", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "langKey", "displayName": "Langue", "permissions": ADMIN_SEUL, "multivalued": False},
        {"name": "login", "displayName": "Identifiant", "permissions": ADMIN_SEUL, "multivalued": False},
        # Actions exigées tenues par la plateforme (D-65), jamais par les actions requises de Keycloak,
        # qui bloqueraient l'octroi direct : MOT_DE_PASSE, SECOND_FACTEUR, FICHE, CHARTE, VERIFIER_COURRIEL.
        {"name": "gimac_actions_requises", "displayName": "Actions exigées par la plateforme", "permissions": ADMIN_SEUL,
         "multivalued": True},
    ],
    "groups": [],
}

# Double facteur (D-61) : niveaux d'assurance du flux navigateur. Niveau 1 : mot de passe ;
# niveau 2 : mot de passe puis code à usage unique (TOTP). Keycloak pose le niveau atteint dans
# le claim acr (« 1 » ou « 2 ») ; les clients des couches de présentation exigent le niveau 2.
FLUX_NAVIGATEUR = "navigateur-gimac"
# D-65 : web_app ne passe plus par le navigateur (octroi direct à deux facteurs, acr posé par mappeur) ;
# seul tester-cli (contrôles cURL des développeurs, autorisation par appareil) garde le flux navigateur.
CLIENTS_NIVEAU_2 = ["tester-cli"]

# Connexion sur la plateforme (D-65, docs/lot1/connexion.md §4) : la passerelle soumet identifiant,
# mot de passe et code à Keycloak par l'octroi direct, serveur à serveur ; aucune page de Keycloak.
FLUX_MOT_DE_PASSE = "direct-gimac-mot-de-passe"
FLUX_DEUX_FACTEURS = "direct-gimac-deux-facteurs"
CLIENT_CONNEXION = "gimac-connexion"
# Actions exigées des comptes de test : second facteur à configurer sur la plateforme au premier accès.
ACTIONS_TEST = ["SECOND_FACTEUR"]
# Sessions (connexion.md §6) : 30 min d'inactivité, 10 h au plus, jeton d'accès de 5 min.
SESSIONS = {"ssoSessionIdleTimeout": 1800, "ssoSessionMaxLifespan": 36000, "accessTokenLifespan": 300}
# Protection contre la force brute (D-62, planchers) : elle s'applique aussi à l'octroi direct.
FORCE_BRUTE = {"bruteForceProtected": True, "permanentLockout": False, "failureFactor": 5, "waitIncrementSeconds": 900,
               "maxFailureWaitSeconds": 900, "maxDeltaTimeSeconds": 43200, "quickLoginCheckMilliSeconds": 1000,
               "minimumQuickLoginWaitSeconds": 60}
# Politique OTP du domaine, lue par MS-IAM et par l'extension gimac-totp pour vérifier le premier code.
# Politique des mots de passe au plancher de la plateforme (D-62), écrite dans la syntaxe de Keycloak
# comme MS-IAM l'écrit (PolitiqueMotDePasse.versChaine) ; la console la durcit ensuite.
POLITIQUE_MOTS_DE_PASSE = "length(12) and notUsername(undefined)"
POLITIQUE_OTP = {"otpPolicyType": "totp", "otpPolicyAlgorithm": "HmacSHA1", "otpPolicyDigits": 6, "otpPolicyPeriod": 30,
                 "otpPolicyLookAheadWindow": 1, "otpPolicyInitialCounter": 0, "otpPolicyCodeReusable": False}


def execution(authenticator, requirement, priority, config=None):
    e = {"authenticator": authenticator, "authenticatorFlow": False, "requirement": requirement, "priority": priority,
         "userSetupAllowed": False}
    if config:
        e["authenticatorConfig"] = config
    return e


def sous_flux(alias, requirement, priority):
    return {"flowAlias": alias, "authenticatorFlow": True, "requirement": requirement, "priority": priority,
            "userSetupAllowed": False}


def flux(alias, description, executions, top_level=False):
    return {"alias": alias, "description": description, "providerId": "basic-flow", "topLevel": top_level,
            "builtIn": False, "authenticationExecutions": executions}


def flux_double_facteur():
    """Flux navigateur à niveaux d'assurance (Keycloak « step-up authentication »)."""
    f = FLUX_NAVIGATEUR
    flux_ = [
        flux(f, "Navigateur GIMAC : cookie ou formulaires à niveaux d'assurance (D-61)", [
            execution("auth-cookie", "ALTERNATIVE", 10),
            sous_flux(f + " formulaires", "ALTERNATIVE", 20),
        ], top_level=True),
        flux(f + " formulaires", "Niveaux d'assurance 1 et 2", [
            sous_flux(f + " niveau 1", "CONDITIONAL", 10),
            sous_flux(f + " niveau 2", "CONDITIONAL", 20),
        ]),
        flux(f + " niveau 1", "Niveau 1 : identifiant et mot de passe", [
            execution("conditional-level-of-authentication", "REQUIRED", 10, "gimac-niveau-1"),
            execution("auth-username-password-form", "REQUIRED", 20),
        ]),
        flux(f + " niveau 2", "Niveau 2 : code à usage unique", [
            execution("conditional-level-of-authentication", "REQUIRED", 10, "gimac-niveau-2"),
            execution("auth-otp-form", "REQUIRED", 20),
        ]),
    ]
    configs = [
        {"alias": "gimac-niveau-1", "config": {"loa-condition-level": "1", "loa-max-age": "36000"}},
        {"alias": "gimac-niveau-2", "config": {"loa-condition-level": "2", "loa-max-age": "36000"}},
    ]
    return flux_, configs


def flux_octroi_direct():
    """Deux flux d'octroi direct (D-65) : mot de passe seul (acr 1), mot de passe et code obligatoire (acr 2)."""
    base = [execution("direct-grant-validate-username", "REQUIRED", 10),
            execution("direct-grant-validate-password", "REQUIRED", 20)]
    mot_de_passe = flux(FLUX_MOT_DE_PASSE, "Octroi direct GIMAC : identifiant et mot de passe (premier accès, acr 1)",
                        copy.deepcopy(base), top_level=True)
    deux_facteurs = flux(FLUX_DEUX_FACTEURS, "Octroi direct GIMAC : identifiant, mot de passe et code TOTP obligatoire (acr 2)",
                         copy.deepcopy(base) + [execution("direct-grant-validate-otp", "REQUIRED", 30)], top_level=True)
    # Identifiants propres (nouveaux flux) : les clients s'y lient par identifiant, que Keycloak remappe à l'import.
    mot_de_passe["id"] = uid("flow:" + FLUX_MOT_DE_PASSE)
    deux_facteurs["id"] = uid("flow:" + FLUX_DEUX_FACTEURS)
    return [mot_de_passe, deux_facteurs]


def mappeur_acr(niveau):
    """Niveau d'assurance posé en dur : l'octroi direct ne le calcule pas (acr 1 ou 2 selon le client)."""
    return {"name": "acr-gimac", "protocol": "openid-connect", "protocolMapper": "oidc-hardcoded-claim-mapper",
            "consentRequired": False,
            "config": {"claim.name": "acr", "claim.value": niveau, "jsonType.label": "String", "id.token.claim": "true",
                       "access.token.claim": "true", "userinfo.token.claim": "true", "introspection.token.claim": "true"}}


def client_octroi_direct(c, flux_id, niveau):
    """Client confidentiel à octroi direct seul, lié à son flux, acr en dur, sans portée acr par défaut."""
    c.update({"enabled": True, "publicClient": False, "clientAuthenticatorType": "client-secret",
              "standardFlowEnabled": False, "implicitFlowEnabled": False, "directAccessGrantsEnabled": True,
              "serviceAccountsEnabled": False, "frontchannelLogout": False, "redirectUris": [], "webOrigins": []})
    c["defaultClientScopes"] = [s for s in c.get("defaultClientScopes", []) if s != "acr"]
    c["authenticationFlowBindingOverrides"] = {"direct_grant": flux_id}
    c["protocolMappers"] = [m for m in c.get("protocolMappers", []) if m.get("name") != "acr-gimac"] + [mappeur_acr(niveau)]
    attributs = c.setdefault("attributes", {})
    attributs.pop("minimum.acr.value", None)
    attributs["access.token.lifespan"] = "300"
    return c


def uid(nom):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "gimac:" + nom))


def role(nom, description, composites=None, attributs=None):
    r = {"id": uid("role:" + nom), "name": nom, "description": description, "composite": bool(composites), "clientRole": False}
    if composites:
        r["composites"] = {"realm": composites}
    if attributs:
        r["attributes"] = attributs
    return r


def client_confidentiel(client_id, secret_var, directs=False, service=False, roles_service=None):
    c = {
        "id": uid("client:" + client_id), "clientId": client_id, "enabled": True, "publicClient": False,
        "clientAuthenticatorType": "client-secret", "secret": "${" + secret_var + "}",
        "standardFlowEnabled": False, "directAccessGrantsEnabled": directs, "serviceAccountsEnabled": service,
        "protocol": "openid-connect", "fullScopeAllowed": True,
        "defaultClientScopes": ["web-origins", "acr", "jhipster", "roles", "profile", "email"],
        "optionalClientScopes": ["offline_access"],
    }
    return c


def catalogue_frontend(realm):
    """Permissions et profils du domaine, selon les règles de lecture de MS-IAM (KeycloakAnnuaire) : une permission
    est un rôle de realm en majuscules à souligné, hors ROLE_ ; un profil est un rôle composite ROLE_*."""
    import re
    motif = re.compile(r"^[A-Z][A-Z0-9]*(_[A-Z0-9]+)+$")
    est_permission = lambda n: motif.match(n) and not n.startswith("ROLE_")
    roles = realm["roles"]["realm"]
    profils = [{
        "code": r["name"],
        "libelle": (r.get("attributes") or {}).get("gimac_libelle", [None])[0],
        "description": r.get("description") or "",
        "externe": (r.get("attributes") or {}).get("gimac_externe") == ["true"],
        "permissions": sorted(p for p in r["composites"]["realm"] if est_permission(p)),
    } for r in roles if r["name"].startswith("ROLE_") and r.get("composite")]
    return {
        "source": "Généré par infra/keycloak/generer-realm-gimac.py à partir de infra/keycloak/gimac-realm.json : ne pas modifier à la main.",
        "permissions": sorted(r["name"] for r in roles if est_permission(r["name"])),
        "profils": profils,
    }


def main():
    realm = json.loads(SOURCE.read_text(encoding="utf-8"))
    realm = copy.deepcopy(realm)
    realm["realm"] = "gimac"
    realm["id"] = "gimac"
    realm["displayName"] = "Plateforme Moabi du GIMAC (développement)"
    realm["internationalizationEnabled"] = True
    realm["supportedLocales"] = ["fr", "en", "es"]
    realm["defaultLocale"] = "fr"
    # D-62 : pages et courriels du fournisseur d'identité aux couleurs du GIMAC ; aucun compte
    # ouvert par inscription libre (création par le super administrateur ou le RSSI) ; mot de
    # passe oublié réinitialisable par courriel ; identifiant non modifiable par l'utilisateur.
    realm["loginTheme"] = "gimac"
    realm["emailTheme"] = "gimac"
    realm["registrationAllowed"] = False
    # D-65 : le mot de passe oublié se réinitialise sur la plateforme (courriel de MS-IAM), jamais par Keycloak.
    realm["resetPasswordAllowed"] = False
    realm["rememberMe"] = False
    realm["editUsernameAllowed"] = False
    realm["loginWithEmailAllowed"] = True
    # Messagerie de développement (Mailpit du Compose) ; reconfigurable depuis la console.
    realm["smtpServer"] = {"host": "messagerie", "port": "1025", "from": "des@gimac.test",
                           "fromDisplayName": "Plateforme Moabi du GIMAC", "auth": "false", "starttls": "false", "ssl": "false"}
    for r in realm["roles"]["realm"]:
        if r["name"] == "default-roles-jhipster":
            r["name"] = "default-roles-gimac"
    if "defaultRole" in realm:
        realm["defaultRole"]["name"] = "default-roles-gimac"

    realm["roles"]["realm"] += [role(p, LIBELLES_PERMISSIONS.get(p, "Permission du lot 1")) for p in TOUTES]
    realm["roles"]["realm"] += [role(n, PRESENTATIONS[n][1], ["ROLE_USER"] + ps, {"gimac_libelle": [PRESENTATIONS[n][0]], **({"gimac_externe": ["true"]} if n in PROFILS_EXTERNES else {})})
                                for n, ps in PROFILS.items()]

    # Claim institution_id (RG-59), porté par le client scope jhipster déjà servi aux applications.
    for cs in realm["clientScopes"]:
        if cs["name"] == "jhipster":
            # D-65 : la passerelle ouvre la session à partir de l'octroi direct ; les rôles et permissions
            # figurent dans le jeton d'identité comme dans le jeton d'accès (acr et institution_id aussi).
            for m in cs["protocolMappers"]:
                if m["name"] == "roles":
                    m["config"]["id.token.claim"] = "true"
                    m["config"]["access.token.claim"] = "true"
            cs["protocolMappers"].append({
                "id": uid("mapper:institution_id"), "name": "institution_id", "protocol": "openid-connect",
                "protocolMapper": "oidc-usermodel-attribute-mapper", "consentRequired": False,
                "config": {"user.attribute": "institution_id", "claim.name": "institution_id", "jsonType.label": "String",
                           "id.token.claim": "true", "access.token.claim": "true", "userinfo.token.claim": "true"},
            })

    for c in realm["clients"]:
        if c["clientId"] == "internal" and "jhipster" not in c.get("defaultClientScopes", []):
            c["defaultClientScopes"].append("jhipster")
        # Aucun secret hérité du realm JHipster (BNF-07) : le client de service « internal » reçoit une
        # variable résolue à l'import ; les clients publics n'en ont pas l'usage et les clients intégrés
        # (account, broker, realm-management) en reçoivent un de Keycloak.
        if c["clientId"] == "internal":
            c["secret"] = "${GIMAC_SECRET_INTERNAL}"
        else:
            c.pop("secret", None)
    realm["clients"].append(client_confidentiel("tester-cli", "GIMAC_SECRET_TESTER_CLI", directs=True))
    # Jetons de recette à deux facteurs pour les contrôles cURL : autorisation par appareil (RFC 8628),
    # qui passe par le flux navigateur ; le mot de passe direct ne donne que le niveau 1.
    realm["clients"][-1].setdefault("attributes", {})["oauth2.device.authorization.grant.enabled"] = "true"

    flux_, configs = flux_double_facteur()
    realm["authenticationFlows"] = [x for x in realm["authenticationFlows"] if not x["alias"].startswith(FLUX_NAVIGATEUR)] + flux_
    realm["authenticatorConfig"] = [x for x in realm.get("authenticatorConfig", []) if not x["alias"].startswith("gimac-niveau-")] + configs
    realm["browserFlow"] = FLUX_NAVIGATEUR
    for c in realm["clients"]:
        if c["clientId"] in CLIENTS_NIVEAU_2:
            c.setdefault("attributes", {})["minimum.acr.value"] = "2"

    # D-65 : octroi direct. gimac-connexion (mot de passe seul, acr 1, session restreinte du premier
    # accès) et web_app (mot de passe et code obligatoire, acr 2), confidentiels, sans flux navigateur.
    octroi = flux_octroi_direct()
    realm["authenticationFlows"] = [x for x in realm["authenticationFlows"] if not x["alias"].startswith("direct-gimac-")] + octroi
    flux_mdp, flux_2fa = (f["id"] for f in octroi)
    connexion = client_confidentiel(CLIENT_CONNEXION, "GIMAC_SECRET_CONNEXION", directs=True)
    connexion["description"] = "Connexion sur la plateforme, mot de passe seul (acr 1, premier accès) : passerelle et MS-IAM"
    realm["clients"].append(client_octroi_direct(connexion, flux_mdp, "1"))
    for c in realm["clients"]:
        if c["clientId"] == "web_app":
            client_octroi_direct(c, flux_2fa, "2")
            c["secret"] = "${GIMAC_SECRET_WEB_APP}"
            c["description"] = "Connexion sur la plateforme, mot de passe et code TOTP (acr 2) : passerelle"
    for client_id in SERVICES:
        realm["clients"].append(client_confidentiel(client_id, "GIMAC_SECRET_" + client_id.upper().replace("-", "_"), service=True))

    utilisateurs = [u for u in realm["users"] if not u["username"].startswith("service-account-")]
    for login, prenom, nom, profil, institution in UTILISATEURS:
        u = {
            "id": uid("user:" + login), "username": login, "enabled": True, "emailVerified": True,
            "firstName": prenom, "lastName": nom, "email": f"{login}@gimac.test",
            "credentials": [{"type": "password", "value": "${GIMAC_MOT_DE_PASSE_TEST}", "temporary": False}],
            "realmRoles": ["default-roles-gimac", profil], "groups": ["/Users"],
            # Second facteur configuré sur la plateforme au premier accès (D-61, D-65) : action tenue par
            # l'attribut gimac_actions_requises, aucune action requise de Keycloak (elle bloquerait l'octroi direct).
            "attributes": {"langKey": ["fr"], "login": [login], "gimac_actions_requises": list(ACTIONS_TEST)},
        }
        if institution:
            u["attributes"]["institution_id"] = [institution]
        utilisateurs.append(u)
    for client_id, roles in SERVICES.items():
        utilisateurs.append({
            "id": uid("user:service-account-" + client_id), "username": "service-account-" + client_id, "enabled": True,
            "serviceAccountClientId": client_id, "realmRoles": ["default-roles-gimac"] + roles,
        })
    for u in utilisateurs:
        u["realmRoles"] = ["default-roles-gimac" if r == "default-roles-jhipster" else r for r in u.get("realmRoles", [])]
        if u["username"] in ("admin", "user"):
            # Les empreintes du realm généré (mot de passe connu « admin », « user ») sont remplacées.
            u["credentials"] = [{"type": "password", "value": "${GIMAC_MOT_DE_PASSE_TEST}", "temporary": False}]
    for u in utilisateurs:
        u.pop("requiredActions", None)
    realm["users"] = utilisateurs
    realm.update(SESSIONS)
    realm.update(FORCE_BRUTE)
    realm.update(POLITIQUE_OTP)
    realm["passwordPolicy"] = POLITIQUE_MOTS_DE_PASSE
    for u in realm["users"]:
        client_id = u.get("serviceAccountClientId")
        if client_id in ROLES_CLIENT_SERVICES:
            u["clientRoles"] = ROLES_CLIENT_SERVICES[client_id]

    for composant in realm["components"]["org.keycloak.userprofile.UserProfileProvider"]:
        composant["config"] = {"kc.user.profile.config": [json.dumps(PROFIL_UTILISATEUR, ensure_ascii=False)]}

    # Identifiants internes retirés : le realm JHipster est importé dans la même base Keycloak,
    # ses identifiants de rôles, clients et portées y existent déjà. Keycloak les régénère.
    def sans_identifiants(noeud, racine=False):
        if isinstance(noeud, dict):
            return {k: sans_identifiants(v) for k, v in noeud.items() if racine and k == "id" or k not in ("id", "containerId")}
        if isinstance(noeud, list):
            return [sans_identifiants(v) for v in noeud]
        return noeud
    realm = sans_identifiants(realm, racine=True)
    # Les flux d'octroi direct gardent leur identifiant : les clients s'y lient par lui (remappé à l'import).
    for f in realm["authenticationFlows"]:
        if f["alias"] in (FLUX_MOT_DE_PASSE, FLUX_DEUX_FACTEURS):
            f["id"] = uid("flow:" + f["alias"])

    texte = json.dumps(realm, ensure_ascii=False, indent=2)
    assert "default-roles-jhipster" not in texte
    assert all(c.get("secret", "${").startswith("${") for c in realm["clients"]), "secret de client en clair"

    assert all(x in texte for x in ('"browserFlow": "navigateur-gimac"', '"minimum.acr.value": "2"', "PAR_TRANSITION_APPROVE", "ROLE_SUPER_ADMIN",
                                          "manage-realm", "unmanagedAttributePolicy", "COL_MESSAGE_SEND", "collaboration-service",
                                          '"loginTheme": "gimac"'))
    # D-65 : connexion sur la plateforme, jamais de page Keycloak.
    clients = {c["clientId"]: c for c in realm["clients"]}
    flux_ids = {f["alias"]: f.get("id") for f in realm["authenticationFlows"]}
    for client_id, flux_alias, niveau in ((CLIENT_CONNEXION, FLUX_MOT_DE_PASSE, "1"), ("web_app", FLUX_DEUX_FACTEURS, "2")):
        c = clients[client_id]
        assert not c["publicClient"] and c["directAccessGrantsEnabled"] and not c["standardFlowEnabled"], client_id
        assert not c.get("implicitFlowEnabled") and not c.get("serviceAccountsEnabled"), client_id
        assert c["secret"].startswith("${GIMAC_SECRET_"), client_id
        assert c["authenticationFlowBindingOverrides"] == {"direct_grant": flux_ids[flux_alias]}, client_id
        assert "acr" not in c["defaultClientScopes"] and "jhipster" in c["defaultClientScopes"], client_id
        assert "minimum.acr.value" not in c.get("attributes", {}), client_id
        assert [m["config"]["claim.value"] for m in c["protocolMappers"] if m["protocolMapper"] == "oidc-hardcoded-claim-mapper"
                and m["config"]["claim.name"] == "acr"] == [niveau], client_id
    assert clients[CLIENT_CONNEXION]["secret"] == "${GIMAC_SECRET_CONNEXION}"
    assert clients["web_app"]["secret"] == "${GIMAC_SECRET_WEB_APP}" and clients["gateway-service"]["secret"] == "${GIMAC_SECRET_GATEWAY_SERVICE}"
    jhipster = next(cs for cs in realm["clientScopes"] if cs["name"] == "jhipster")
    assert all(m["config"]["id.token.claim"] == m["config"]["access.token.claim"] == "true"
               for m in jhipster["protocolMappers"] if m["name"] in ("roles", "institution_id"))
    assert clients[CLIENT_CONNEXION]["attributes"]["access.token.lifespan"] == "300"
    otp = [e["authenticator"] for f in realm["authenticationFlows"] if f["alias"] == FLUX_DEUX_FACTEURS
           for e in f["authenticationExecutions"] if e["requirement"] == "REQUIRED"]
    assert otp == ["direct-grant-validate-username", "direct-grant-validate-password", "direct-grant-validate-otp"], otp
    assert "direct-grant-validate-otp" not in texte.split(FLUX_MOT_DE_PASSE, 1)[1].split('"topLevel"', 1)[0]
    comptes_service = {u.get("serviceAccountClientId"): u for u in realm["users"]}
    assert clients["gateway-service"]["serviceAccountsEnabled"] and "INTERNAL_IAM_READ" in comptes_service["gateway-service"]["realmRoles"]
    assert "manage-users" in comptes_service["iam-service"]["clientRoles"]["realm-management"]
    # D-119 : étapes de saga de la sandbox réservées à SBX-ESPACE, sans lecture interne générale ; introspection à la passerelle.
    assert [u["serviceAccountClientId"] for u in realm["users"] if "INTERNAL_IAM_SBX_SAGA" in (u.get("realmRoles") or [])] == ["sbx-espace"]
    assert "INTERNAL_IAM_READ" not in comptes_service["sbx-espace"]["realmRoles"]
    assert "INTERNAL_IAM_SBX_INTROSPECT" in comptes_service["gateway-service"]["realmRoles"]
    # US-EXI-05 : un compte de service par service de la sandbox et pour Mifos ; chaque permission INTERNAL_SBX_* et chaque
    # permission de la plateforme d'un compte sbx-* vient du tableau des appels synchrones, et d'aucune autre source.
    assert all(c in comptes_service for c in SERVICES_SBX)
    attendues = {}
    for appelant, permission, _ in APPELS_SBX:
        attendues.setdefault(appelant, set()).add(permission)
    for client_id, u in comptes_service.items():
        portees = {r for r in u.get("realmRoles") or [] if r != "default-roles-gimac"}
        if client_id and client_id.startswith("sbx-"):
            assert portees == attendues.get(client_id, set()), (client_id, portees)
        else:
            assert not {r for r in portees if r.startswith("INTERNAL_SBX_")} - attendues.get(client_id, set()), client_id
    assert comptes_service["sbx-lecture"]["realmRoles"] == ["default-roles-gimac"], "SBX-LECTURE n'appelle aucun service"
    # US-EXI-25 : aucune autorisation d'appel de la sandbox vers MS-CERTIFICATION ni vers MS-HOMOLOGATION.
    assert not any(r.startswith(("INTERNAL_CERTIFICATION_", "INTERNAL_HOMOLOGATION_")) or r.startswith(("CER_", "HOM_"))
                   for c in SERVICES_SBX for r in comptes_service[c]["realmRoles"])
    assert [u["serviceAccountClientId"] for u in realm["users"] if "INTERNAL_HOMOLOGATION_REPUBLISH" in (u.get("realmRoles") or [])] == ["exploitation-service"]
    # ROLE_ADMIN (CRUD généré et /management des services) : jamais dans un profil, porté par le seul compte
    # de service de la supervision.
    assert "ROLE_ADMIN" in comptes_service["gateway-supervision"]["realmRoles"]
    assert not any("ROLE_ADMIN" in (r.get("composites") or {}).get("realm", []) for r in realm["roles"]["realm"]), "ROLE_ADMIN dans un profil"
    assert [u["username"] for u in realm["users"] if "ROLE_ADMIN" in (u.get("realmRoles") or [])] == ["service-account-gateway-supervision"]
    assert not any(u.get("requiredActions") for u in realm["users"]), "action requise Keycloak sur un compte"
    assert all(u["attributes"]["gimac_actions_requises"] == ACTIONS_TEST for u in realm["users"] if u["username"].startswith("test-"))
    assert all(realm[k] == v for k, v in {**SESSIONS, **FORCE_BRUTE, **POLITIQUE_OTP}.items())
    assert realm["passwordPolicy"] == POLITIQUE_MOTS_DE_PASSE
    assert realm["resetPasswordAllowed"] is False and "gimac_actions_requises" in texte
    # D-86 : chaque profil livré porte son libellé et sa mission, plus aucun « Profil du lot 1 ».
    profils_realm = {r["name"]: r for r in realm["roles"]["realm"] if r["name"] in PROFILS}
    assert all(r["attributes"]["gimac_libelle"] == [PRESENTATIONS[n][0]] and r["description"] == PRESENTATIONS[n][1]
               for n, r in profils_realm.items()) and "Profil du lot 1" not in texte
    # D-94 : le caractère externe d'un profil est une donnée (attribut gimac_externe), plus un nom figé dans MS-IAM.
    assert all((r["attributes"].get("gimac_externe") == ["true"]) == (n in PROFILS_EXTERNES) for n, r in profils_realm.items())
    CIBLE.write_text(texte + "\n", encoding="utf-8")
    production = dict(realm, users=[u for u in realm["users"] if u.get("serviceAccountClientId")])
    assert not any(u["username"].startswith("test-") for u in production["users"]) and production["users"], "domaine de production"
    CIBLE_PRODUCTION.write_text(json.dumps(production, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    frontend = catalogue_frontend(realm)
    assert [p["code"] for p in frontend["profils"]] == list(PROFILS) and sorted(TOUTES) == frontend["permissions"], "catalogue du frontend"
    assert all(set(p["permissions"]) == set(PROFILS[p["code"]]) for p in frontend["profils"]), "composition d'un profil du frontend"
    CIBLE_FRONTEND.write_text(json.dumps(frontend, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{CIBLE.relative_to(RACINE)} : {len(TOUTES)} permissions, {len(PROFILS)} profils, {len(UTILISATEURS)} utilisateurs de test")
    print(f"{CIBLE_PRODUCTION.relative_to(RACINE)} : {len(production['users'])} comptes de service, aucun compte de test")
    print(f"{CIBLE_FRONTEND.relative_to(RACINE)} : catalogue des permissions et des profils des services simulés")


if __name__ == "__main__":
    main()
