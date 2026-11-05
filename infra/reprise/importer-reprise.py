#!/usr/bin/env python3
"""Reprise de l'existant (D-107) : import des institutions et des projets de certification.

Lit un fichier JSON de reprise (format : docs/exploitation/reprise-des-donnees.md §9, exemples dans
infra/reprise/exemples/) et l'envoie par lots aux routes internes de reprise, avec le jeton du compte de service
reprise-service (permission REF_REPRISE_IMPORT) :

- institutions : POST <participant>/api/v1/interne/reprise/institutions (MS-PARTICIPANT) ;
- liaisons     : POST <homologation>/api/v1/interne/reprise/liaisons (MS-HOMOLOGATION) ;
- correspondants : POST <participant>/api/v1/interne/reprise/correspondants (MS-PARTICIPANT, D-132), une fois les
  institutions et les comptes repris ;
- clotures     : POST <homologation>/api/v1/interne/reprise/liaisons/clotures (MS-HOMOLOGATION, D-137), clôture sans
  suite de liaisons reprises sans projet réel ; motif du fichier obligatoire ; une liaison close est rapportée « mise
  à jour ».

Par défaut, tout est joué en SIMULATION : les services contrôlent chaque ligne jusqu'à l'écriture en base puis
annulent ; rien n'est écrit. L'option --reel écrit. Rapport ligne à ligne (créée, mise à jour, inchangée, refusée
avec son code et son motif), puis totaux.

    python3 infra/reprise/importer-reprise.py institutions institutions.json
    python3 infra/reprise/importer-reprise.py liaisons liaisons.json --reel --rapport rapport-liaisons.json

Jeton : variable GIMAC_JETON_REPRISE (jeton déjà obtenu), sinon octroi client_credentials du client
reprise-service auprès de Keycloak avec le secret de la variable GIMAC_SECRET_REPRISE_SERVICE (jamais sur la ligne
de commande, jamais affiché ni écrit). Adresses : --participant, --homologation, --jeton-url, ou les variables
GIMAC_REPRISE_PARTICIPANT_URL, GIMAC_REPRISE_HOMOLOGATION_URL, GIMAC_REPRISE_JETON_URL.

Code de sortie : 0 si aucune ligne refusée, 1 si au moins une ligne est refusée, 2 en cas d'erreur d'usage,
de fichier, de jeton ou d'appel.
"""
import argparse
import json
import os
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request

ROUTES = {
    "institutions": ("participant", "/api/v1/interne/reprise/institutions", "codeMembre"),
    "liaisons": ("homologation", "/api/v1/interne/reprise/liaisons", "referenceOrigine"),
    "correspondants": ("participant", "/api/v1/interne/reprise/correspondants", "codeMembre"),
    "clotures": ("homologation", "/api/v1/interne/reprise/liaisons/clotures", "referenceOrigine"),
}
# Clé du tableau des lignes dans le corps de la requête, quand elle diffère de la nature (D-137).
CLES_LOT = {"clotures": "liaisons"}
ADRESSES = {
    "participant": ("GIMAC_REPRISE_PARTICIPANT_URL", "http://localhost:8085"),
    "homologation": ("GIMAC_REPRISE_HOMOLOGATION_URL", "http://localhost:8086"),
    "jeton": ("GIMAC_REPRISE_JETON_URL", "http://localhost:9080/realms/gimac/protocol/openid-connect/token"),
}
# Bornes des services (RepriseInstitutionsUseCase.LIGNES_MAX, RepriseLiaisonsUseCase.LIGNES_MAX).
LOT_MAX = {"institutions": 2000, "liaisons": 500, "correspondants": 2000, "clotures": 500}
LIBELLES = {"CREEE": "créée", "MISE_A_JOUR": "mise à jour", "INCHANGEE": "inchangée", "REFUSEE": "REFUSÉE"}
# Résultats au masculin de la reprise des correspondants (RepriseCorrespondantsUseCase.Resultat), rapportés aux mêmes libellés.
MASCULINS = {"CREE": "CREEE", "MIS_A_JOUR": "MISE_A_JOUR", "INCHANGE": "INCHANGEE", "REFUSE": "REFUSEE", "CLOSE": "MISE_A_JOUR"}
DELAI = 300


class ErreurReprise(Exception):
    """Erreur d'usage, de fichier, de jeton ou d'appel : rien n'est supposé écrit au-delà des lots déjà rapportés."""


def lire_fichier(chemin, nature):
    """Lignes du fichier : objet {"<nature>": [...], "motif": "..."} ou tableau seul ; motif facultatif."""
    try:
        donnees = json.loads(pathlib.Path(chemin).read_text(encoding="utf-8"))
    except FileNotFoundError:
        raise ErreurReprise(f"fichier introuvable : {chemin}")
    except (json.JSONDecodeError, UnicodeDecodeError) as e:
        raise ErreurReprise(f"fichier JSON illisible : {chemin} ({e})")
    motif = None
    if isinstance(donnees, dict):
        motif = donnees.get("motif")
        lignes = donnees.get(nature)
    else:
        lignes = donnees
    if not isinstance(lignes, list) or not lignes:
        raise ErreurReprise(f"aucune ligne « {nature} » dans {chemin}")
    if not all(isinstance(l, dict) for l in lignes):
        raise ErreurReprise(f"chaque ligne de {chemin} doit être un objet JSON")
    return lignes, motif


def lots(lignes, taille):
    for debut in range(0, len(lignes), taille):
        yield debut, lignes[debut:debut + taille]


def adresse(nom, valeur):
    variable, defaut = ADRESSES[nom]
    return (valeur or os.environ.get(variable) or defaut).rstrip("/")


def appeler(url, corps, entetes, ouvrir=urllib.request.urlopen):
    """POST et réponse JSON ; une erreur HTTP rend son code et son corps (ProblemDetail des services)."""
    requete = urllib.request.Request(url, data=corps, headers=entetes, method="POST")
    try:
        with ouvrir(requete, timeout=DELAI) as r:
            return json.loads(r.read().decode("utf-8") or "{}")
    except urllib.error.HTTPError as e:
        texte = e.read().decode("utf-8", errors="replace")
        try:
            detail = json.loads(texte)
            message = f"{detail.get('code') or detail.get('title') or ''} {detail.get('detail') or detail.get('message') or ''}".strip()
        except json.JSONDecodeError:
            message = texte[:200]
        raise ErreurReprise(f"HTTP {e.code} sur {url} : {message}")
    except urllib.error.URLError as e:
        raise ErreurReprise(f"service injoignable : {url} ({e.reason})")


def jeton(url_jeton, client, environ=os.environ, ouvrir=urllib.request.urlopen):
    """Jeton du compte de service : fourni (GIMAC_JETON_REPRISE) ou obtenu par client_credentials."""
    fourni = environ.get("GIMAC_JETON_REPRISE")
    if fourni:
        return fourni.strip()
    secret = environ.get("GIMAC_SECRET_REPRISE_SERVICE")
    if not secret:
        raise ErreurReprise("ni GIMAC_JETON_REPRISE ni GIMAC_SECRET_REPRISE_SERVICE dans l'environnement")
    corps = urllib.parse.urlencode({"grant_type": "client_credentials", "client_id": client, "client_secret": secret}).encode()
    reponse = appeler(url_jeton, corps, {"Content-Type": "application/x-www-form-urlencoded"}, ouvrir)
    if not reponse.get("access_token"):
        raise ErreurReprise("réponse de Keycloak sans jeton d'accès")
    return reponse["access_token"]


def importer(nature, lignes, motif, simulation, url_service, jeton_acces, taille, ouvrir=urllib.request.urlopen):
    """Envoie les lots et rend les lignes du rapport, renumérotées dans le fichier (rang à partir de 1)."""
    _, chemin, _ = ROUTES[nature]
    rapport = []
    for debut, lot in lots(lignes, taille):
        corps = {"simulation": simulation, CLES_LOT.get(nature, nature): lot}
        if motif and nature in ("institutions", "correspondants", "clotures"):
            corps["motif"] = motif
        reponse = appeler(
            url_service + chemin,
            json.dumps(corps, ensure_ascii=False).encode("utf-8"),
            {"Content-Type": "application/json", "Accept": "application/json", "Authorization": "Bearer " + jeton_acces},
            ouvrir,
        )
        if bool(reponse.get("simulation")) != simulation:
            raise ErreurReprise("le service n'a pas respecté le mode demandé (simulation ou réel) : arrêt")
        for ligne in reponse.get("lignes", []):
            ligne = dict(ligne)
            ligne["rang"] = debut + int(ligne.get("rang", 0)) + 1
            rapport.append(ligne)
    return rapport


def formater(nature, rapport, simulation):
    """Rapport lisible : une ligne par entrée, avertissements et champs refusés en retrait, puis totaux."""
    cle = ROUTES[nature][2]
    mode = "SIMULATION (rien n'est écrit)" if simulation else "IMPORT RÉEL"
    sortie = [f"Reprise des {nature} : {mode}"]
    totaux = {}
    for l in rapport:
        resultat = MASCULINS.get(l.get("resultat"), l.get("resultat", "REFUSEE"))
        totaux[resultat] = totaux.get(resultat, 0) + 1
        ident = str(l.get(cle) or "?")
        if resultat == "REFUSEE":
            sortie.append(f"  [{l['rang']:>4}] {ident:<24} {LIBELLES[resultat]:<12} {l.get('code')} : {l.get('motif')}")
            champs = (l.get("details") or {}).get("champs") or {}
            for champ, message in champs.items():
                sortie.append(f"           - {champ} : {message}")
        else:
            infos = []
            if nature == "liaisons":
                infos += [f"{k}={l[k]}" for k in ("reference", "statut", "etapeCourante") if l.get(k)]
                if l.get("operationsModifiees"):
                    infos.append(f"opérations={l['operationsModifiees']}")
            else:
                infos += [f"{k}={l[k]}" for k in ("statut",) if l.get(k)]
                if nature == "correspondants" and l.get("compteId"):
                    infos.append(f"compte={l['compteId']}")
                if l.get("changements") and resultat == "MISE_A_JOUR":
                    infos.append("champs=" + ",".join(l["changements"]))
            if l.get("id") or l.get("liaisonId"):
                infos.append(f"id={l.get('id') or l.get('liaisonId')}")
            sortie.append(f"  [{l['rang']:>4}] {ident:<24} {LIBELLES.get(resultat, resultat):<12} {' '.join(infos)}".rstrip())
        for a in l.get("avertissements") or []:
            sortie.append(f"           ! {a}")
    sortie.append("Totaux : " + ", ".join(f"{LIBELLES[k]} {totaux.get(k, 0)}" for k in LIBELLES))
    return "\n".join(sortie), totaux.get("REFUSEE", 0)


def analyser(arguments):
    p = argparse.ArgumentParser(description="Reprise de l'existant (D-107, D-132) : institutions, correspondants et projets de certification.")
    p.add_argument("nature", choices=sorted(ROUTES), help="institutions ou correspondants (MS-PARTICIPANT), liaisons ou clotures (MS-HOMOLOGATION)")
    p.add_argument("fichier", help="fichier JSON de reprise")
    p.add_argument("--reel", action="store_true", help="écrire réellement (sans cette option : simulation)")
    p.add_argument("--participant", help="adresse de MS-PARTICIPANT")
    p.add_argument("--homologation", help="adresse de MS-HOMOLOGATION")
    p.add_argument("--jeton-url", help="point d'émission des jetons de Keycloak (domaine gimac)")
    p.add_argument("--client", default="reprise-service", help="client de service (par défaut reprise-service)")
    p.add_argument("--lot", type=int, help="lignes par appel (par défaut : maximum du service)")
    p.add_argument("--rapport", help="écrit aussi le rapport brut (JSON) dans ce fichier")
    a = p.parse_args(arguments)
    if a.lot is not None and not 1 <= a.lot <= LOT_MAX[a.nature]:
        p.error(f"--lot entre 1 et {LOT_MAX[a.nature]}")
    return a


def main(arguments=None, environ=os.environ, ouvrir=urllib.request.urlopen, sortie=sys.stdout):
    a = analyser(sys.argv[1:] if arguments is None else arguments)
    simulation = not a.reel
    try:
        lignes, motif = lire_fichier(a.fichier, a.nature)
        service = ROUTES[a.nature][0]
        url_service = adresse(service, getattr(a, service))
        acces = jeton(adresse("jeton", a.jeton_url), a.client, environ, ouvrir)
        rapport = importer(a.nature, lignes, motif, simulation, url_service, acces, a.lot or LOT_MAX[a.nature], ouvrir)
    except ErreurReprise as e:
        print(f"ERREUR : {e}", file=sys.stderr)
        return 2
    texte, refusees = formater(a.nature, rapport, simulation)
    print(texte, file=sortie)
    if a.rapport:
        pathlib.Path(a.rapport).write_text(json.dumps({"simulation": simulation, "lignes": rapport}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 1 if refusees else 0


if __name__ == "__main__":
    sys.exit(main())
