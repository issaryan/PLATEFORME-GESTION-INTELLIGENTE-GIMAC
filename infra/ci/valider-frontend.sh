#!/usr/bin/env bash
# Validation des couches de présentation (D-46, D-51, D-61) : installation reproductible,
# contrôle des types et lint, tests unitaires Vitest avec seuils de couverture (≥ 80 %,
# bloquants), construction des applications et, sur demande, tests de bout en bout
# Playwright avec couverture nyc (≥ 80 %, bloquante).
#
#   infra/ci/valider-frontend.sh          # installation, lint, couverture, construction
#   infra/ci/valider-frontend.sh --e2e    # idem, puis tests de bout en bout
#
# Variables :
#   GIMAC_E2E=1               équivalent de --e2e
#   GIMAC_NPM_CI=1|0          force (1) ou saute (0) « npm ci » ; par défaut, lancé en CI
#                             ou si node_modules est absent (npm ci supprime node_modules)
#   GIMAC_CHROMIUM            navigateur Chromium des tests de bout en bout ; par défaut
#                             /opt/pw-browsers/chromium, sinon celui installé par
#                             « npx playwright install chromium »
set -euo pipefail

# shellcheck source=infra/ci/commun.sh
source "$(dirname "${BASH_SOURCE[0]}")/commun.sh"
FRONTEND="$RACINE_DEPOT/frontend"
cd "$FRONTEND"

e2e="${GIMAC_E2E:-0}"
for argument in "$@"; do
  case "$argument" in
    --e2e) e2e=1 ;;
    *) echo "Argument inconnu : $argument" >&2; exit 2 ;;
  esac
done

section "Prérequis"
if ! outil_present node || ! outil_present npm; then
  echec "Node.js introuvable (version ≥ 22 requise)"; fin_section; bilan "Validation du frontend"; exit 1
fi
version_node="$(node -p 'process.versions.node')"
info "Node.js $version_node, npm $(npm -v)"
if [ "${version_node%%.*}" -lt 22 ]; then
  echec "Node.js $version_node : version ≥ 22 requise (engines de frontend/package.json)"
fi
fin_section

section "Installation (npm ci)"
installer="${GIMAC_NPM_CI:-}"
if [ -z "$installer" ]; then
  if [ -n "${CI:-}" ] || [ ! -d node_modules ]; then installer=1; else installer=0; fi
fi
if [ "$installer" = 1 ]; then
  controler "npm ci (verrou package-lock.json respecté)" npm ci --no-audit --no-fund
else
  info "node_modules présent : npm ci sauté (GIMAC_NPM_CI=1 pour le forcer)"
fi
fin_section

section "Types et lint"
controler "npm run lint (tsc, oxlint, contrôle des jetons de conception)" npm run lint
fin_section

section "Tests unitaires et couverture (Vitest, seuils ≥ 80 %)"
controler "npm run couverture" npm run couverture
fin_section

section "Construction des applications"
controler "npm run build" npm run build
fin_section

if [ "$e2e" = 1 ]; then
  section "Tests de bout en bout et couverture (Playwright, nyc ≥ 80 %)"
  if [ -z "${GIMAC_CHROMIUM:-}" ] && [ ! -x /opt/pw-browsers/chromium ]; then
    GIMAC_CHROMIUM="$(node -e "console.log(require('playwright-core').chromium.executablePath())" 2>/dev/null || true)"
  fi
  if [ -n "${GIMAC_CHROMIUM:-}" ] && [ ! -x "$GIMAC_CHROMIUM" ]; then
    ignore "tests de bout en bout : Chromium introuvable ($GIMAC_CHROMIUM) ; lancer « npx playwright install --with-deps chromium »"
  else
    [ -z "${GIMAC_CHROMIUM:-}" ] || export GIMAC_CHROMIUM
    info "Chromium : ${GIMAC_CHROMIUM:-/opt/pw-browsers/chromium}"
    controler "npm run e2e:couverture" npm run e2e:couverture
  fi
  fin_section
else
  info "Tests de bout en bout non demandés (--e2e ou GIMAC_E2E=1)"
fi

bilan "Validation du frontend"
