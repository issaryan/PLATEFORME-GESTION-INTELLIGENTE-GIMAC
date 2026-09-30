#!/usr/bin/env bash
# Tests de sécurité statiques (D-61, BNF-07) : secrets en clair (gitleaks) dans l'arborescence
# versionnée et dans l'historique, puis, si trivy est installé, vulnérabilités connues des
# dépendances (Maven, npm) et des images de base.
#
#   infra/ci/valider-securite.sh
#
# Variables :
#   GIMAC_PLAGE_COMMITS  commits examinés par gitleaks en plus de l'arborescence (ex.
#                        « origin/main..HEAD » ; en CI : ceux de la demande de fusion ou du push)
#   GIMAC_TRIVY=0        saute l'analyse trivy (la CI la mène par aquasecurity/trivy-action)
set -euo pipefail

# shellcheck source=infra/ci/commun.sh
source "$(dirname "${BASH_SOURCE[0]}")/commun.sh"
cd "$RACINE_DEPOT"
CONFIG_GITLEAKS="$RACINE_DEPOT/infra/ci/gitleaks.toml"

section "Secrets en clair (gitleaks)"
if exiger_outil gitleaks "détection de secrets"; then
  # Arborescence : fichiers versionnés ou nouveaux non ignorés (ni node_modules, ni dist, ni
  # target), copiés à part pour que les chemins signalés soient ceux du dépôt.
  arbre="$DOSSIER_TRAVAIL/arbre"
  mkdir -p "$arbre"
  git ls-files -z -co --exclude-standard | grep -zv '/$' | xargs -0 -r cp --parents -t "$arbre" 2>/dev/null || true
  # shellcheck disable=SC2016 # variables développées par le bash -c, pas ici
  controler "gitleaks : fichiers du dépôt" \
    bash -c 'cd "$1" && gitleaks dir --no-banner --redact --verbose -c "$2" .' _ "$arbre" "$CONFIG_GITLEAKS"
  # Commits nouveaux : un secret ajouté puis retiré dans la même série reste détecté.
  # L'historique antérieur n'est pas réexaminé (il ne peut plus être corrigé).
  if [ -n "${GIMAC_PLAGE_COMMITS:-}" ]; then
    controler "gitleaks : commits $GIMAC_PLAGE_COMMITS" \
      gitleaks git --no-banner --redact --verbose -c "$CONFIG_GITLEAKS" --log-opts="$GIMAC_PLAGE_COMMITS" .
  else
    info "Commits : GIMAC_PLAGE_COMMITS non défini (ex. « origin/main..HEAD »), historique non examiné"
  fi
fi
fin_section

if [ "${GIMAC_TRIVY:-1}" != 0 ]; then
  section "Vulnérabilités des dépendances (trivy)"
  if exiger_outil trivy "analyse de vulnérabilités"; then
    controler "trivy fs : aucune vulnérabilité HIGH ou CRITICAL corrigeable" \
      trivy fs --quiet --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 \
        --skip-dirs '**/node_modules' --skip-dirs '**/target' .
  fi
  fin_section
fi

bilan "Tests de sécurité"
