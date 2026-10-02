#!/usr/bin/env bash
# Ajoute un compte nominatif au serveur de démonstration et le crée dans Keycloak (D-62), sans accès
# au serveur : lancé par .github/workflows/compte-nominatif.yml sur l'exécuteur gimac-214.
#
#   IDENTIFIANT=prenom.nom EMAIL=… PRENOM=… NOM=… PROFIL=ROLE_… [FONCTION=…] infra/ci/compte-nominatif.sh
#
# La liste des comptes reste sur le serveur (~/.config/gimac-des/comptes.json, BNF-07) ; le mot de
# passe provisoire est écrit dans le résumé de l'exécution GitHub Actions (ou sur la sortie standard).
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ETAT_SERVEUR="$HOME/.config/gimac-des"
COMPTES="$ETAT_SERVEUR/comptes.json"
LIEN="$RACINE/infra/keycloak/local/comptes.json"
MOTS_DE_PASSE="$RACINE/infra/keycloak/local/mots-de-passe-provisoires.txt"

[ -e "$RACINE/infra/docker-compose/.env" ] || { echo "ERREUR : .env du serveur absent (déployer d'abord)." >&2; exit 1; }

# Un seul accès à l'état du serveur à la fois (déploiement compris).
exec 9>"${TMPDIR:-/tmp}/gimac-deploiement.verrou"
flock 9

mkdir -p "$ETAT_SERVEUR" "$(dirname "$LIEN")"
chmod 700 "$ETAT_SERVEUR"
[ -e "$COMPTES" ] || { echo "[]" > "$COMPTES"; chmod 600 "$COMPTES"; }
ln -sfn "$COMPTES" "$LIEN"

# Contrôle des saisies et ajout à la liste (valeurs lues dans l'environnement, jamais interpolées).
python3 - "$COMPTES" <<'EOF'
import json, os, re, sys
chemin = sys.argv[1]
c = {k: os.environ.get(v, "").strip() for k, v in
     [("identifiant", "IDENTIFIANT"), ("email", "EMAIL"), ("prenom", "PRENOM"), ("nom", "NOM"), ("profil", "PROFIL"), ("fonction", "FONCTION")]}
c["identifiant"] = c["identifiant"].lower()
erreurs = []
if not re.fullmatch(r"[a-z][a-z0-9._-]{2,49}", c["identifiant"]): erreurs.append("identifiant invalide")
if not re.fullmatch(r"[^@\s]+@[^@\s]+\.[^@\s]+", c["email"]): erreurs.append("courriel invalide")
if not c["prenom"] or not c["nom"]: erreurs.append("prénom et nom obligatoires")
if not re.fullmatch(r"ROLE_[A-Z_]+", c["profil"]): erreurs.append("profil invalide")
if erreurs: sys.exit("ERREUR : " + ", ".join(erreurs))
comptes = json.load(open(chemin))
if any(x["identifiant"] == c["identifiant"] for x in comptes):
    print(f"déjà dans la liste : {c['identifiant']}")
else:
    comptes.append({**c, "fonction": c["fonction"] or None, "institution_id": None})
    with open(chemin, "w") as f:
        json.dump(comptes, f, ensure_ascii=False, indent=2)
    print(f"ajouté à la liste : {c['identifiant']} ({c['profil']})")
EOF

avant=0; [ ! -f "$MOTS_DE_PASSE" ] || avant="$(wc -l < "$MOTS_DE_PASSE")"
python3 "$RACINE/infra/keycloak/creer-comptes-personnels.py"

# Mot de passe provisoire du compte, seulement s'il vient d'être créé (lignes ajoutées par ce passage).
identifiant="$(echo "${IDENTIFIANT:?}" | tr '[:upper:]' '[:lower:]' | xargs)"
ligne="$(tail -n +"$((avant + 1))" "$MOTS_DE_PASSE" 2>/dev/null | grep -P "^\Q${identifiant}\E\t" | tail -n 1 || true)"
if [ -z "$ligne" ]; then
  resume="Compte \`$identifiant\` : déjà présent dans Keycloak, laissé tel quel (aucun nouveau mot de passe)."
else
  mdp="$(printf '%s' "$ligne" | cut -f4)"
  resume="$(printf '%s\n' \
    "### Compte \`$identifiant\` créé" \
    "" \
    "| Identifiant | Profil | Mot de passe provisoire |" \
    "|---|---|---|" \
    "| \`$identifiant\` | \`$(printf '%s' "$ligne" | cut -f3)\` | \`$mdp\` |" \
    "" \
    "Connexion : application du département. Au premier accès : nouveau mot de passe, code TOTP, fiche et charte." \
    "Supprimer cette exécution une fois le mot de passe transmis à son titulaire.")"
fi
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then echo "$resume" >> "$GITHUB_STEP_SUMMARY"; else echo "$resume"; fi
echo "Compte traité : $identifiant (mot de passe provisoire dans le résumé de l'exécution)."
