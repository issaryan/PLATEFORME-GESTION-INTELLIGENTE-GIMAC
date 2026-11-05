#!/usr/bin/env python3
"""Tests de infra/reprise/importer-reprise.py (D-107) : lecture des fichiers, jeton, lots, mode, rapport, erreurs.

    python3 -m unittest -q infra/reprise/test_importer_reprise.py

Aucun service réel : les appels HTTP passent par un ouvreur simulé ; données fictives (infra/reprise/exemples).
"""
import importlib.util
import io
import json
import pathlib
import tempfile
import unittest
import urllib.error

ICI = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("importer_reprise", ICI / "importer-reprise.py")
R = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(R)


class Reponse:
    def __init__(self, corps):
        self.corps = corps

    def read(self):
        return self.corps

    def __enter__(self):
        return self

    def __exit__(self, *a):
        return False


class Ouvreur:
    """Ouvreur simulé : jeton Keycloak, puis réponses des services rangées par chemin ; garde chaque requête."""

    def __init__(self, reponses=None, simulation=None):
        self.requetes = []
        self.reponses = reponses or {}
        self.simulation = simulation

    def __call__(self, requete, timeout=None):
        self.requetes.append(requete)
        url = requete.full_url
        if url.endswith("/token"):
            return Reponse(json.dumps({"access_token": "jeton-obtenu"}).encode())
        corps = json.loads(requete.data.decode())
        reponse = self.reponses.get(url.split("/api/")[0] + "/api/" + url.split("/api/")[1])
        if isinstance(reponse, Exception):
            raise reponse
        if callable(reponse):
            return Reponse(json.dumps(reponse(corps)).encode())
        cle = next(k for k in ("institutions", "liaisons", "correspondants") if k in corps)
        simulation = corps["simulation"] if self.simulation is None else self.simulation
        lignes = [{"rang": i, "resultat": "CREEE", "statut": "ACTIF", "id": f"id-{i}"} for i in range(len(corps[cle]))]
        return Reponse(json.dumps({"simulation": simulation, "lignes": lignes}).encode())


def fichier(dossier, contenu, nom="lot.json"):
    chemin = pathlib.Path(dossier) / nom
    chemin.write_text(contenu if isinstance(contenu, str) else json.dumps(contenu), encoding="utf-8")
    return str(chemin)


class LectureTest(unittest.TestCase):
    def test_exemples_fictifs_lisibles(self):
        lignes, motif = R.lire_fichier(ICI / "exemples/institutions.json", "institutions")
        self.assertEqual(4, len(lignes))
        self.assertIn("Reprise", motif)
        self.assertIn("MFSA", [l.get("groupe") for l in lignes])
        liaisons, motif = R.lire_fichier(ICI / "exemples/liaisons.json", "liaisons")
        self.assertIsNone(motif)
        self.assertEqual({"EN_PRODUCTION", None}, {l.get("avancement") for l in liaisons})

    def test_tableau_seul_et_erreurs(self):
        with tempfile.TemporaryDirectory() as d:
            self.assertEqual(([{"a": 1}], None), R.lire_fichier(fichier(d, [{"a": 1}]), "liaisons"))
            for contenu in ("{", {"institutions": []}, {"autre": [1]}, [1, 2]):
                with self.assertRaises(R.ErreurReprise):
                    R.lire_fichier(fichier(d, contenu), "institutions")
            with self.assertRaises(R.ErreurReprise):
                R.lire_fichier(pathlib.Path(d) / "absent.json", "institutions")

    def test_lots_et_adresses(self):
        self.assertEqual([(0, [1, 2]), (2, [3])], list(R.lots([1, 2, 3], 2)))
        self.assertEqual("http://p", R.adresse("participant", "http://p/"))
        self.assertTrue(R.adresse("homologation", None).startswith("http"))


class JetonTest(unittest.TestCase):
    def test_jeton_fourni_ou_obtenu_sans_secret_dans_la_requete_affichee(self):
        self.assertEqual("abc", R.jeton("http://k/token", "reprise-service", {"GIMAC_JETON_REPRISE": " abc "}))
        o = Ouvreur()
        self.assertEqual("jeton-obtenu", R.jeton("http://k/token", "reprise-service", {"GIMAC_SECRET_REPRISE_SERVICE": "s3cr3t"}, o))
        corps = o.requetes[0].data.decode()
        self.assertIn("grant_type=client_credentials", corps)
        self.assertIn("client_id=reprise-service", corps)

    def test_jeton_absent_ou_refuse(self):
        with self.assertRaises(R.ErreurReprise):
            R.jeton("http://k/token", "c", {})

        def sans_jeton(requete, timeout=None):
            return Reponse(b"{}")

        with self.assertRaises(R.ErreurReprise):
            R.jeton("http://k/token", "c", {"GIMAC_SECRET_REPRISE_SERVICE": "x"}, sans_jeton)


class AppelTest(unittest.TestCase):
    def test_erreurs_http_et_reseau(self):
        def refus(corps):
            def ouvrir(requete, timeout=None):
                raise urllib.error.HTTPError(requete.full_url, 403, "Interdit", {}, io.BytesIO(corps))

            return ouvrir

        with self.assertRaisesRegex(R.ErreurReprise, "HTTP 403.*HOM_X refus"):
            R.appeler("http://s/api", b"{}", {}, refus(b'{"code":"HOM_X","detail":"refus"}'))
        with self.assertRaisesRegex(R.ErreurReprise, "HTTP 403.*page"):
            R.appeler("http://s/api", b"{}", {}, refus(b"page"))

        def injoignable(requete, timeout=None):
            raise urllib.error.URLError("refusée")

        with self.assertRaisesRegex(R.ErreurReprise, "injoignable"):
            R.appeler("http://s/api", b"{}", {}, injoignable)


class ImportTest(unittest.TestCase):
    def test_lots_renumerotes_mode_et_jeton(self):
        o = Ouvreur()
        lignes = [{"codeMembre": f"BQ-{i}"} for i in range(5)]
        rapport = R.importer("institutions", lignes, "Motif", True, "http://p", "jeton", 2, o)
        self.assertEqual([1, 2, 3, 4, 5], [l["rang"] for l in rapport])
        self.assertEqual(3, len(o.requetes))
        premier = json.loads(o.requetes[0].data.decode())
        self.assertTrue(premier["simulation"])
        self.assertEqual("Motif", premier["motif"])
        self.assertEqual("Bearer jeton", o.requetes[0].get_header("Authorization"))
        self.assertEqual("http://p/api/v1/interne/reprise/institutions", o.requetes[0].full_url)
        R.importer("liaisons", [{"referenceOrigine": "R"}], "Motif", False, "http://h", "jeton", 10, o)
        dernier = json.loads(o.requetes[-1].data.decode())
        self.assertNotIn("motif", dernier)
        self.assertFalse(dernier["simulation"])

    def test_mode_non_respecte_arrete(self):
        with self.assertRaises(R.ErreurReprise):
            R.importer("institutions", [{"codeMembre": "B"}], None, True, "http://p", "j", 10, Ouvreur(simulation=False))


class RapportTest(unittest.TestCase):
    def test_rapport_lisible(self):
        rapport = [
            {"rang": 1, "codeMembre": "BQ-1", "resultat": "CREEE", "statut": "ACTIF", "id": "u1", "avertissements": ["Pays hors CEMAC"]},
            {"rang": 2, "codeMembre": "BQ-2", "resultat": "MISE_A_JOUR", "statut": "ACTIF", "changements": ["nom", "statut"], "id": "u2"},
            {"rang": 3, "codeMembre": "BQ-3", "resultat": "INCHANGEE", "id": "u3"},
            {"rang": 4, "codeMembre": None, "resultat": "REFUSEE", "code": "PAR_REPRISE_LIGNE_INVALIDE", "motif": "Ligne invalide", "details": {"champs": {"pays": "Champ obligatoire"}}},
        ]
        texte, refusees = R.formater("institutions", rapport, True)
        self.assertEqual(1, refusees)
        self.assertIn("SIMULATION (rien n'est écrit)", texte)
        self.assertIn("! Pays hors CEMAC", texte)
        self.assertIn("champs=nom,statut", texte)
        self.assertIn("REFUSÉE", texte)
        self.assertIn("- pays : Champ obligatoire", texte)
        self.assertIn("Totaux : créée 1, mise à jour 1, inchangée 1, REFUSÉE 1", texte)
        self.assertNotIn("—", texte)
        liaisons = [{"rang": 1, "referenceOrigine": "R-1", "resultat": "CREEE", "reference": "LIA-2026-001", "statut": "EN_PRODUCTION", "etapeCourante": "SUIVI", "operationsModifiees": 40, "liaisonId": "l1"}]
        texte, refusees = R.formater("liaisons", liaisons, False)
        self.assertEqual(0, refusees)
        self.assertIn("IMPORT RÉEL", texte)
        self.assertIn("reference=LIA-2026-001 statut=EN_PRODUCTION etapeCourante=SUIVI opérations=40 id=l1", texte)

    def test_rapport_des_correspondants(self):
        rapport = [
            {"rang": 1, "codeMembre": "BQ-1", "compteId": "c1", "resultat": "CREE", "id": "r1"},
            {"rang": 2, "codeMembre": "BQ-1", "compteId": "c2", "resultat": "MIS_A_JOUR", "changements": ["actif"], "id": "r2"},
            {"rang": 3, "codeMembre": "BQ-1", "compteId": "c3", "resultat": "INCHANGE", "id": "r3"},
            {"rang": 4, "codeMembre": "BQ-9", "compteId": "c4", "resultat": "REFUSE", "code": "PAR_REPRISE_INSTITUTION_INCONNUE", "motif": "Inconnue"},
        ]
        texte, refusees = R.formater("correspondants", rapport, True)
        self.assertEqual(1, refusees)
        self.assertIn("compte=c1 id=r1", texte)
        self.assertIn("compte=c2 champs=actif id=r2", texte)
        self.assertIn("PAR_REPRISE_INSTITUTION_INCONNUE : Inconnue", texte)
        self.assertIn("Totaux : créée 1, mise à jour 1, inchangée 1, REFUSÉE 1", texte)

    def test_route_des_clotures(self):
        o = Ouvreur()
        R.importer("clotures", [{"referenceOrigine": "REPRISE-1"}], "Liaison par défaut", True, "http://h", "j", 10, o)
        corps = json.loads(o.requetes[-1].data)
        self.assertTrue(o.requetes[-1].full_url.endswith("/api/v1/interne/reprise/liaisons/clotures"))
        self.assertEqual((corps["motif"], corps["liaisons"]), ("Liaison par défaut", [{"referenceOrigine": "REPRISE-1"}]))
        texte, refusees = R.formater("clotures", [{"rang": 1, "referenceOrigine": "REPRISE-1", "resultat": "CLOSE", "id": "l1"}], True)
        self.assertEqual(0, refusees)
        self.assertIn("mise à jour 1", texte)

    def test_exemple_et_route_des_correspondants(self):
        lignes, motif = R.lire_fichier(ICI / "exemples/correspondants.json", "correspondants")
        self.assertEqual(1, len(lignes))
        o = Ouvreur()
        R.importer("correspondants", lignes, motif, True, "http://p", "j", 10, o)
        requete = o.requetes[-1]
        self.assertTrue(requete.full_url.endswith("/api/v1/interne/reprise/correspondants"))
        self.assertEqual(motif, json.loads(requete.data)["motif"])


class PrincipalTest(unittest.TestCase):
    def test_simulation_par_defaut_reel_sur_option_et_rapport_ecrit(self):
        with tempfile.TemporaryDirectory() as d:
            lot = fichier(d, {"institutions": [{"codeMembre": "BQ-1"}]})
            o = Ouvreur()
            sortie = io.StringIO()
            env = {"GIMAC_SECRET_REPRISE_SERVICE": "s3cr3t"}
            self.assertEqual(0, R.main(["institutions", lot, "--participant", "http://p"], env, o, sortie))
            self.assertTrue(json.loads(o.requetes[1].data.decode())["simulation"])
            self.assertNotIn("s3cr3t", sortie.getvalue())
            brut = pathlib.Path(d) / "rapport.json"
            self.assertEqual(0, R.main(["institutions", lot, "--reel", "--rapport", str(brut), "--lot", "1"], env, o, io.StringIO()))
            self.assertFalse(json.loads(o.requetes[-1].data.decode())["simulation"])
            self.assertFalse(json.loads(brut.read_text())["simulation"])

    def test_refus_et_erreurs(self):
        with tempfile.TemporaryDirectory() as d:
            lot = fichier(d, {"liaisons": [{"referenceOrigine": "R-1"}]})
            refus = {"http://h/api/v1/interne/reprise/liaisons": lambda c: {"simulation": True, "lignes": [{"rang": 0, "resultat": "REFUSEE", "code": "HOM_X", "motif": "non"}]}}
            env = {"GIMAC_JETON_REPRISE": "j"}
            self.assertEqual(1, R.main(["liaisons", lot, "--homologation", "http://h"], env, Ouvreur(refus), io.StringIO()))
            panne = {"http://h/api/v1/interne/reprise/liaisons": urllib.error.URLError("refusée")}
            self.assertEqual(2, R.main(["liaisons", lot, "--homologation", "http://h"], env, Ouvreur(panne), io.StringIO()))
            self.assertEqual(2, R.main(["liaisons", lot], {}, Ouvreur(), io.StringIO()))
            with self.assertRaises(SystemExit):
                R.main(["liaisons", lot, "--lot", "501"], env, Ouvreur(), io.StringIO())


if __name__ == "__main__":
    unittest.main()
