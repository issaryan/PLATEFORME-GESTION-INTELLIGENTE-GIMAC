#!/usr/bin/env bash
# Jeton d'un utilisateur de test du domaine gimac (PI-GIMAC-01 §0.9), à sourcer :
#   source infra/docker-compose/scripts/token.sh test-chef-des
# Le jeton est demandé depuis le réseau Docker du projet, pour que son émetteur
# (http://keycloak:9080/realms/gimac) soit celui qu'attendent la passerelle et les services.
# Mot de passe et secret viennent du .env local (GIMAC_MOT_DE_PASSE_TEST, GIMAC_SECRET_TESTER_CLI).
ICI_TOKEN="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UTILISATEUR="${1:?utilisateur de test requis (test-chef-des, test-ingenieur-homologation...)}"
set -a; . "$ICI_TOKEN/.env"; set +a
REPONSE=$(docker run --rm --network gimac-des_default curlimages/curl:8.22.0 -s -X POST \
  http://keycloak:9080/realms/gimac/protocol/openid-connect/token \
  -d grant_type=password -d client_id=tester-cli -d "client_secret=${GIMAC_SECRET_TESTER_CLI}" \
  -d scope=openid -d "username=${UTILISATEUR}" -d "password=${GIMAC_MOT_DE_PASSE_TEST}")
TOKEN=$(printf '%s' "$REPONSE" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token",""))' 2>/dev/null)
if [ -z "$TOKEN" ]; then
  echo "jeton non obtenu pour ${UTILISATEUR}" >&2
  return 1 2>/dev/null || exit 1
fi
export TOKEN
export GATEWAY_URL="${GATEWAY_URL:-http://gateway:8080}"
