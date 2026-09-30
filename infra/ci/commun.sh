# shellcheck shell=bash
# Fonctions communes aux scripts de validation de la chaîne d'intégration continue (D-61).
# Chargé par « source » depuis les scripts de infra/ci ; jamais exécuté seul.
#
# Chaque contrôle est enregistré dans l'un de trois états :
#   RÉUSSI  le contrôle s'est exécuté sans erreur ;
#   IGNORÉ  l'outil nécessaire est absent du poste (en CI, il est toujours installé :
#           GIMAC_CI_STRICT=1 transforme alors une absence en échec) ;
#   ÉCHEC   le contrôle a détecté un défaut.
# Le script appelant termine par « bilan », qui affiche le récapitulatif et rend 1 en cas d'échec.

RACINE_DEPOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export RACINE_DEPOT

# Coloration seulement sur un terminal (les journaux de CI restent lisibles).
if [ -t 1 ]; then
  C_VERT=$'\e[32m'; C_ROUGE=$'\e[31m'; C_JAUNE=$'\e[33m'; C_GRAS=$'\e[1m'; C_RAZ=$'\e[0m'
else
  C_VERT=''; C_ROUGE=''; C_JAUNE=''; C_GRAS=''; C_RAZ=''
fi

RESULTATS_REUSSIS=()
RESULTATS_IGNORES=()
RESULTATS_ECHECS=()

# Section repliable dans GitHub Actions, simple titre ailleurs.
section() {
  if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::group::$*"; else echo; echo "${C_GRAS}=== $* ===${C_RAZ}"; fi
}
fin_section() {
  if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::endgroup::"; fi
}

info()   { echo "  $*"; }
# indenter [préfixe] : décale chaque ligne de l'entrée standard sous le contrôle en cours.
indenter() { local l; while IFS= read -r l; do echo "    ${1:-}$l"; done; }
reussi() { RESULTATS_REUSSIS+=("$1"); echo "${C_VERT}  [RÉUSSI]${C_RAZ} $1"; }
echec()  {
  RESULTATS_ECHECS+=("$1"); echo "${C_ROUGE}  [ÉCHEC]${C_RAZ} $1"
  if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::error title=Validation GIMAC::$1"; fi
}
ignore() {
  if [ "${GIMAC_CI_STRICT:-0}" = 1 ]; then
    echec "$1 (outil absent alors que GIMAC_CI_STRICT=1)"
  else
    RESULTATS_IGNORES+=("$1"); echo "${C_JAUNE}  [IGNORÉ]${C_RAZ} $1"
    if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::warning title=Validation GIMAC::$1"; fi
  fi
}

# outil_present <commande> : vrai si la commande est disponible.
outil_present() { command -v "$1" >/dev/null 2>&1; }

# controler <libellé> <commande…> : exécute la commande et enregistre le résultat,
# sans interrompre le script (tous les contrôles sont menés, puis le bilan tranche).
controler() {
  local libelle="$1"; shift
  if "$@"; then reussi "$libelle"; else echec "$libelle"; fi
}

# essayer <libellé> <commande…> : comme controler, mais rend le statut de la commande,
# pour enchaîner une étape sur la réussite de la précédente (« if essayer … ; then »).
essayer() {
  local libelle="$1"; shift
  if "$@"; then reussi "$libelle"; return 0; fi
  echec "$libelle"; return 1
}

# exiger_outil <commande> <libellé> : vrai si présent, sinon enregistre le contrôle comme ignoré.
exiger_outil() {
  if outil_present "$1"; then return 0; fi
  ignore "$2 : « $1 » introuvable"
  return 1
}

# Dossier de travail temporaire, supprimé à la sortie du script.
DOSSIER_TRAVAIL="$(mktemp -d "${TMPDIR:-/tmp}/gimac-ci.XXXXXX")"
trap 'rm -rf "$DOSSIER_TRAVAIL"' EXIT

bilan() {
  local titre="$1"
  echo
  echo "${C_GRAS}=== Bilan : $titre ===${C_RAZ}"
  echo "  Réussis : ${#RESULTATS_REUSSIS[@]}   Ignorés : ${#RESULTATS_IGNORES[@]}   Échecs : ${#RESULTATS_ECHECS[@]}"
  local r
  for r in "${RESULTATS_IGNORES[@]}"; do echo "${C_JAUNE}  - ignoré : $r${C_RAZ}"; done
  for r in "${RESULTATS_ECHECS[@]}"; do echo "${C_ROUGE}  - échec  : $r${C_RAZ}"; done
  if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
      echo "### $titre"
      echo
      echo "| État | Contrôle |"
      echo "|---|---|"
      for r in "${RESULTATS_REUSSIS[@]}"; do echo "| réussi | $r |"; done
      for r in "${RESULTATS_IGNORES[@]}"; do echo "| ignoré | $r |"; done
      for r in "${RESULTATS_ECHECS[@]}"; do echo "| **échec** | $r |"; done
      echo
    } >> "$GITHUB_STEP_SUMMARY"
  fi
  [ "${#RESULTATS_ECHECS[@]}" -eq 0 ]
}

# Liste des microservices (dossiers ms-* munis d'un pom.xml), triée.
lister_services() {
  local d
  for d in "$RACINE_DEPOT"/ms-*/; do
    [ -f "$d/pom.xml" ] && basename "$d"
  done
}
