#!/usr/bin/env bash
# Enchaîne sur le poste toutes les validations de la chaîne d'intégration (D-61), dans
# l'ordre de la CI : infrastructure, sécurité, frontend, contrat des routes (frontend, passerelle, OPA,
# contrôleurs), backend. Chaque étape est menée même
# si une précédente échoue ; le code de sortie est non nul si l'une d'elles a échoué.
#
#   infra/ci/valider-tout.sh              # sans tests de bout en bout
#   GIMAC_E2E=1 infra/ci/valider-tout.sh  # avec tests de bout en bout Playwright
set -uo pipefail

ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echecs=()
for etape in valider-infra valider-securite valider-frontend valider-contrat valider-backend; do
  "$ICI/$etape.sh" || echecs+=("$etape")
done

echo
if [ "${#echecs[@]}" -eq 0 ]; then
  echo "Toutes les validations ont réussi."
else
  echo "Validations en échec : ${echecs[*]}"
  exit 1
fi
