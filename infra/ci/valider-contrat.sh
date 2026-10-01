#!/usr/bin/env bash
# Contrat des routes entre les couches de présentation et le backend (docs/lot1/correspondance.md) :
# chaque appel HTTP du frontend (http.get|post|put|delete|blob, fetch, EventSource) doit trouver une route
# de la passerelle (application.yml de MS-GATEWAY, réécriture /bff/{couche}/<service>/** vers
# /api/v1[/portail]/**), une règle OPA qui ne le refuse pas par construction (lot1_authz.rego) et un
# contrôleur du service cible (@GetMapping, @PostMapping…) de même méthode et de même chemin. La permission
# exigée par OPA doit figurer dans le @PreAuthorize du contrôleur. Les écarts connus des écrans antérieurs au
# parcours sont listés, motif à l'appui, dans infra/ci/contrat/ecarts-connus.txt.
#
#   infra/ci/valider-contrat.sh               # contrôle bloquant
#   infra/ci/valider-contrat.sh --markdown    # tableau route par route, pour docs/lot1/correspondance.md
set -euo pipefail

# shellcheck source=infra/ci/commun.sh
source "$(dirname "${BASH_SOURCE[0]}")/commun.sh"

if [ "${1:-}" = "--markdown" ]; then
  exec node "$RACINE_DEPOT/infra/ci/contrat/routes.mjs" --markdown
fi

section "Contrat des routes frontend, passerelle, OPA et contrôleurs"
if ! outil_present node; then
  ignore "contrat des routes : Node.js introuvable"
elif [ ! -d "$RACINE_DEPOT/frontend/node_modules/typescript" ] || [ ! -d "$RACINE_DEPOT/frontend/node_modules/js-yaml" ]; then
  ignore "contrat des routes : dépendances du frontend absentes (npm ci dans frontend/)"
else
  controler "chaque route appelée par le frontend a sa contrepartie" node "$RACINE_DEPOT/infra/ci/contrat/routes.mjs"
fi
fin_section

bilan "Contrat des routes"
