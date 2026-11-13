"""Tests du générateur du domaine Keycloak « gimac » : profils selon le CR-CERT-GIMAC-01 v1.7 (missions, ouverture
des liaisons par la DDRM, agent de la sécurité des SI) et cohérence des fichiers générés avec le générateur.

    python3 -m unittest -q infra/keycloak/test_generer_realm_gimac.py
"""
import importlib.util
import json
import pathlib
import unittest

ICI = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("generer_realm_gimac", ICI / "generer-realm-gimac.py")
G = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(G)


class ProfilsCrV17(unittest.TestCase):
    def test_conducteur_de_projet_ouvre_le_parcours(self):
        conducteur = G.PROFILS["ROLE_CONDUCTEUR_PROJET"]
        for permission in ("PAR_INSTITUTION_CREATE", "HOM_LIAISON_OPEN", "HOM_PERIMETRE_EDIT", "HOM_PROJECT_CONDUCT"):
            self.assertIn(permission, conducteur)
        # Ni désignation d'un autre conducteur, ni décision d'adhésion, ni périmètre des projets d'autrui.
        for permission in ("HOM_PROJECT_DESIGNATE", "PAR_TRANSITION_APPROVE", "PAR_TRANSITION_REQUEST", "HOM_PERIMETRE_EDIT_TOUS"):
            self.assertNotIn(permission, conducteur)

    def test_ouverture_des_liaisons_reservee_a_la_ddrm(self):
        # D-146 : le conducteur de projet crée aussi le projet qu'il conduit, sans désigner un autre conducteur.
        self.assertEqual([n for n, ps in G.PROFILS.items() if "HOM_LIAISON_OPEN" in ps], ["ROLE_CHEF_DDRM", "ROLE_CONDUCTEUR_PROJET"])
        # Les deux chefs désignent le conducteur (rang 6) ; le chef du DES garde la certification.
        for chef in ("ROLE_CHEF_DES", "ROLE_CHEF_DDRM"):
            self.assertIn("HOM_PROJECT_DESIGNATE", G.PROFILS[chef])
        self.assertIn("CER_QUITUS_TECHNIQUE_SIGN", G.PROFILS["ROLE_CHEF_DES"])

    def test_agent_ssi(self):
        ps = G.PROFILS["ROLE_AGENT_SSI"]
        for p in ("CER_SECURITY_VERIFY", "HOM_EVIDENCE_SUBMIT", "HOM_LIAISON_READ", "COL_MESSAGE_SEND", "PIL_ACTIVITY_OWN"):
            self.assertIn(p, ps)
        self.assertEqual([p for p in ps if p.startswith(("IAM_", "PLATFORM_", "CER_QUITUS_", "PIL_TEAM"))], [])
        self.assertNotIn("ROLE_AGENT_SSI", G.RESPONSABLES)
        self.assertNotIn("ROLE_AGENT_SSI", G.PROFILS_EXTERNES)
        self.assertIn(("test-agent-ssi", "Agent", "Sécurité des SI", "ROLE_AGENT_SSI", None), G.UTILISATEURS)
        self.assertEqual(G.PRESENTATIONS["ROLE_AGENT_SSI"][0], "Agent de la sécurité des SI")

    def test_perimetre_de_lecture_approuve_par_le_rssi(self):
        # CC-SBX-GIMAC-02 §D.8 et UC-LEC-10 : le périmètre du compte de service est approuvé par le RSSI
        # (SBX_LEC_VALIDATION), profil interne ; aucun profil externe ne porte de validation (RG-05).
        self.assertEqual([n for n, ps in G.PROFILS.items() if "SBX_LEC_VALIDATION" in ps], ["ROLE_RSSI"])
        self.assertNotIn("ROLE_RSSI", G.PROFILS_EXTERNES)
        for cible in (G.CIBLE, G.CIBLE_PRODUCTION):
            realm = json.loads(cible.read_text(encoding="utf-8"))
            rssi = next(r for r in realm["roles"]["realm"] if r["name"] == "ROLE_RSSI")
            self.assertIn("SBX_LEC_VALIDATION", rssi["composites"]["realm"])

    def test_supervision_de_la_sandbox_au_des(self):
        # D-135 : SBX_SUP_CONSULTATION au chef du DES, à l'administrateur système et au super administrateur seuls ;
        # aucun profil externe ni rôle d'institution de la sandbox.
        porteurs = sorted(n for n, ps in G.PROFILS.items() if "SBX_SUP_CONSULTATION" in ps)
        self.assertEqual(porteurs, ["ROLE_ADMIN_SYSTEME", "ROLE_CHEF_DES", "ROLE_SUPER_ADMIN"])
        self.assertFalse(set(porteurs) & G.PROFILS_EXTERNES)
        for cible in (G.CIBLE, G.CIBLE_PRODUCTION):
            realm = json.loads(cible.read_text(encoding="utf-8"))
            composites = {r["name"]: r.get("composites", {}).get("realm", []) for r in realm["roles"]["realm"]}
            self.assertEqual(sorted(n for n, c in composites.items() if "SBX_SUP_CONSULTATION" in c), porteurs)

    def test_jeton_de_homologation_service_lie_au_certificat_en_production(self):
        # RFC 8705, CC-SBX-GIMAC-02 §D.8 : cnf.x5t#S256 exigé par SBX-LECTURE ; jamais hors production.
        def lies(cible):
            realm = json.loads(cible.read_text(encoding="utf-8"))
            return [c["clientId"] for c in realm["clients"] if (c.get("attributes") or {}).get("tls.client.certificate.bound.access.tokens") == "true"]
        self.assertEqual(lies(G.CIBLE_PRODUCTION), ["homologation-service"])
        self.assertEqual(lies(G.CIBLE), [])
        clients = [{"clientId": "homologation-service"}, {"clientId": "autre", "attributes": {"x": "1"}}]
        self.assertEqual(G.lier_au_certificat(clients)[1], clients[1])
        self.assertNotIn("attributes", clients[0])

    def test_developpeur_execute_les_cas_affectes(self):
        # CA-SBX-GIMAC-02 UC-CER-04 : développeur et testeur sont les acteurs principaux de l'exécution d'un cas.
        for profil in ("ROLE_SBX_DEVELOPPEUR", "ROLE_SBX_TESTEUR"):
            self.assertIn("SBX_CER_EXECUTION", G.PROFILS[profil])
            self.assertIn("SBX_CER_CONSULTATION", G.PROFILS[profil])

    def test_documentation_du_referentiel_au_portail(self):
        # CC-SBX-GIMAC-02 §6 : /bff/portail/sbx-referentiel/** en lecture sous SBX_REF_CONSULTATION.
        porteurs = sorted(n for n in G.SANDBOX_INSTITUTIONS if "SBX_REF_CONSULTATION" in G.PROFILS[n])
        self.assertEqual(porteurs, ["ROLE_SBX_CHEF_PROJET", "ROLE_SBX_DEVELOPPEUR", "ROLE_SBX_LECTEUR", "ROLE_SBX_PARTENAIRE", "ROLE_SBX_TESTEUR"])

    def test_iam_lit_la_correspondance_du_registre(self):
        # D-134 : MS-IAM lit la correspondance identifiant au registre / code participant dans MS-PARTICIPANT.
        realm = json.loads(G.CIBLE.read_text(encoding="utf-8"))
        iam = next(u for u in realm["users"] if u.get("serviceAccountClientId") == "iam-service")
        self.assertIn("INTERNAL_PARTICIPANT_READ", iam["realmRoles"])

    def test_sbx_donnees_lit_la_liste_des_membres(self):
        # D-140 : SBX-DONNEES lit la liste paginée des institutions de MS-PARTICIPANT.
        realm = json.loads(G.CIBLE.read_text(encoding="utf-8"))
        donnees = next(u for u in realm["users"] if u.get("serviceAccountClientId") == "sbx-donnees")
        self.assertIn("INTERNAL_PARTICIPANT_READ", donnees["realmRoles"])

    def test_missions_selon_le_cr(self):
        m = {k: v[1] for k, v in G.PRESENTATIONS.items()}
        self.assertIn("Coordonnateur fonctionnel", m["ROLE_CHEF_DDRM"])
        self.assertIn("Coordonnateur technique", m["ROLE_CHEF_DES"])
        self.assertNotIn("ouvre les liaisons", m["ROLE_CHEF_DES"])
        self.assertIn("conduit les essais service par service", m["ROLE_SNA"])
        self.assertIn("émet la demande de changement", m["ROLE_PRODUCTION_MONETIQUE"])
        self.assertIn("lien d'essai", m["ROLE_POLE_RESEAU"])
        self.assertIn("répertoires de dépôt", m["ROLE_POLE_SYSTEME"])
        self.assertIn("Ouvre le parcours de certification", m["ROLE_CONDUCTEUR_PROJET"])
        for libelle, mission in G.PRESENTATIONS.values():
            self.assertTrue(1 <= len(libelle) <= 80 and len(mission) <= 500)
            self.assertNotIn("\u2014", libelle + mission)


class FichiersGeneres(unittest.TestCase):
    """Les fichiers versionnés sont ceux que produit le générateur (jamais édités à la main)."""

    def test_realm_et_catalogue_frontend(self):
        realm = json.loads(G.CIBLE.read_text(encoding="utf-8"))
        roles = {r["name"]: r for r in realm["roles"]["realm"]}
        self.assertEqual(roles["ROLE_AGENT_SSI"]["description"], G.PRESENTATIONS["ROLE_AGENT_SSI"][1])
        self.assertEqual(set(roles["ROLE_AGENT_SSI"]["composites"]["realm"]) - {"ROLE_USER"}, set(G.PROFILS["ROLE_AGENT_SSI"]))
        self.assertNotIn("HOM_LIAISON_OPEN", roles["ROLE_CHEF_DES"]["composites"]["realm"])
        front = json.loads(G.CIBLE_FRONTEND.read_text(encoding="utf-8"))
        self.assertEqual([p["code"] for p in front["profils"]], list(G.PROFILS))
        prod = json.loads(G.CIBLE_PRODUCTION.read_text(encoding="utf-8"))
        self.assertIn("ROLE_AGENT_SSI", {r["name"] for r in prod["roles"]["realm"]})
        self.assertFalse([u for u in prod["users"] if u["username"].startswith("test-")])


if __name__ == "__main__":
    unittest.main()
