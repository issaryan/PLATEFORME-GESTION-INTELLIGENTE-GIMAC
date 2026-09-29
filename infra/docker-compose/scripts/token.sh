#!/usr/bin/env bash
# Jeton d'un utilisateur de test du domaine gimac (PI-GIMAC-01 §0.9), à sourcer :
#   source infra/docker-compose/scripts/token.sh test-chef-des            # niveau 2 (double facteur)
#   NIVEAU=1 source infra/docker-compose/scripts/token.sh test-chef-des   # niveau 1, pour vérifier les refus
#
# D-61 : les routes métier exigent un jeton issu d'une authentification à deux facteurs (acr 2).
# Niveau 2 : autorisation par appareil (RFC 8628) ; le script affiche une adresse et un code,
# l'utilisateur de test s'authentifie dans le navigateur (mot de passe puis code TOTP, enrôlé à la
# première connexion) et le script récupère le jeton. Niveau 1 : mot de passe seul (acr 1).
#
# Le jeton est demandé depuis le réseau Docker du projet, pour que son émetteur
# (http://keycloak:9080/realms/gimac) soit celui qu'attendent la passerelle et les services.
# Mot de passe et secret viennent du .env local (GIMAC_MOT_DE_PASSE_TEST, GIMAC_SECRET_TESTER_CLI).
ICI_TOKEN="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UTILISATEUR="${1:?utilisateur de test requis (test-chef-des, test-charge-ddrm...)}"
NIVEAU="${NIVEAU:-2}"
set -a; . "$ICI_TOKEN/.env"; set +a
KC=http://keycloak:9080/realms/gimac/protocol/openid-connect
curl_kc() { docker run --rm --network gimac-des_default curlimages/curl:8.22.0 -s "$@"; }
champ() { python3 -c "import json,sys; print(json.load(sys.stdin).get('$1',''))" 2>/dev/null; }

if [ "$NIVEAU" = "1" ]; then
  REPONSE=$(curl_kc -X POST "$KC/token" -d grant_type=password -d client_id=tester-cli \
    -d "client_secret=${GIMAC_SECRET_TESTER_CLI}" -d scope=openid -d "username=${UTILISATEUR}" \
    -d "password=${GIMAC_MOT_DE_PASSE_TEST}")
else
  APPAREIL=$(curl_kc -X POST "$KC/auth/device" -d client_id=tester-cli -d "client_secret=${GIMAC_SECRET_TESTER_CLI}" \
    -d scope=openid -d acr_values=2)
  CODE_APPAREIL=$(printf '%s' "$APPAREIL" | champ device_code)
  ADRESSE=$(printf '%s' "$APPAREIL" | champ verification_uri_complete | sed 's#http://keycloak:9080#http://localhost:9080#')
  INTERVALLE=$(printf '%s' "$APPAREIL" | champ interval)
  if [ -z "$CODE_APPAREIL" ]; then
    echo "autorisation par appareil refusée : $APPAREIL" >&2
    return 1 2>/dev/null || exit 1
  fi
  echo "Ouvrir $ADRESSE et se connecter en ${UTILISATEUR} (mot de passe puis code TOTP)." >&2
  REPONSE=""
  for _ in $(seq 1 60); do
    sleep "${INTERVALLE:-5}"
    REPONSE=$(curl_kc -X POST "$KC/token" -d grant_type=urn:ietf:params:oauth:grant-type:device_code \
      -d client_id=tester-cli -d "client_secret=${GIMAC_SECRET_TESTER_CLI}" -d "device_code=${CODE_APPAREIL}")
    [ -n "$(printf '%s' "$REPONSE" | champ access_token)" ] && break
  done
fi
TOKEN=$(printf '%s' "$REPONSE" | champ access_token)
if [ -z "$TOKEN" ]; then
  echo "jeton non obtenu pour ${UTILISATEUR}" >&2
  return 1 2>/dev/null || exit 1
fi
export TOKEN
export GATEWAY_URL="${GATEWAY_URL:-http://gateway:8080}"
