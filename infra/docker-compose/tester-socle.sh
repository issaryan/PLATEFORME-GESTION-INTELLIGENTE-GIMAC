#!/usr/bin/env bash
# Vérification de bout en bout du socle démarré par « gimac-dev.sh demarrer ».
# Chaque contrôle affiche OK ou ÉCHEC ; le code de sortie est le nombre d'échecs.
set -uo pipefail

ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
compose() { docker compose --project-name gimac-des -f "$ICI/docker-compose.yml" -f "$ICI/docker-compose.gimac.yml" "$@"; }
set -a; . "$ICI/.env"; set +a
RESEAU=gimac-des_default
CURL="docker run --rm --network $RESEAU curlimages/curl:8.22.0 -s"
AWS="docker run --rm --network $RESEAU -e AWS_ACCESS_KEY_ID=$GIMAC_S3_CLE_ACCES -e AWS_SECRET_ACCESS_KEY=$GIMAC_S3_CLE_SECRETE -e AWS_DEFAULT_REGION=us-east-1 amazon/aws-cli:2.37.5 --endpoint-url http://stockage-objet:7480"
SERVICES=(gateway iam audit document notification participant homologation certification incident monitoring rapprochement pilotage reporting collaboration)
echecs=0
verifier() { # libellé, commande (vrai = OK) ; une seconde tentative absorbe un aléa réseau passager
  if eval "$2" >/dev/null 2>&1 || { sleep 3; eval "$2" >/dev/null 2>&1; }; then echo "OK     $1"; else echo "ÉCHEC  $1"; echecs=$((echecs + 1)); fi
}

echo "== Conteneurs"
for s in "${SERVICES[@]}"; do
  verifier "service $s sain" "[ \"\$(docker inspect -f '{{.State.Health.Status}}' gimac-des-$s-1)\" = healthy ]"
done
for s in keycloak stockage-objet messagerie simulateur-certification gestion-changements autorite-horodatage frontend-departement frontend-portail frontend-console; do
  verifier "$s sain" "[ \"\$(docker inspect -f '{{.State.Health.Status}}' gimac-des-$s-1)\" = healthy ]"
done
for s in kafka registre-schemas openbao opa prometheus grafana loki tempo otel-collector; do
  verifier "$s actif" "[ \"\$(docker inspect -f '{{.State.Running}}' gimac-des-$s-1)\" = true ]"
done
verifier "compartiments WORM initialisés" "compose logs stockage-objet-init | grep -q 'Compartiments WORM prêts'"

echo "== Sécurité et routage"
JETON=$($CURL -X POST http://keycloak:9080/realms/gimac/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=internal -d "client_secret=${GIMAC_SECRET_INTERNAL}" -d scope=openid | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
verifier "jeton Keycloak obtenu (client_credentials, portée openid pour userinfo)" "[ -n \"$JETON\" ]"
# D-65 : sans session ni jeton, la passerelle répond 401 (plus de redirection vers Keycloak) ;
# la délégation par redirection est retirée (404) ; aucune donnée n'est servie.
verifier "API refusée sans session (401)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' http://gateway:8080/bff/des/iam/moi)\" = 401 ]"
verifier "redirection vers Keycloak retirée (404)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' http://gateway:8080/oauth2/authorization/oidc)\" = 404 ]"
# Le CRUD généré des services n'est plus relayé par la passerelle (routes JHipster /services/** retirées,
# /services/** refusé à tous) : il ne reste joignable que sur le réseau interne, par l'adresse du service.
for cible in incident:8088:tickets audit:8082:audit-events; do
  IFS=: read -r s port r <<<"$cible"
  verifier "API $s servie sur le réseau interne avec jeton (GET /api/$r = 200)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $JETON' http://$s:$port/api/$r)\" = 200 ]"
done
# D-62 : dans MS-IAM et MS-PARTICIPANT, le CRUD généré (/api/**) est réservé à ROLE_ADMIN, que seul porte le
# compte de service gateway-supervision (lecture des points /management par la passerelle) ; aucun profil
# (pas même ROLE_SUPER_ADMIN) ne le porte. Refus attendu pour un jeton sans ROLE_ADMIN, accès avec lui.
JETON_ADMIN=$($CURL -X POST http://keycloak:9080/realms/gimac/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=gateway-supervision -d "client_secret=${GIMAC_SECRET_GATEWAY_SUPERVISION}" -d scope=openid | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
for cible in iam:8081:collaborators participant:8085:institutions; do
  IFS=: read -r s port r <<<"$cible"
  verifier "CRUD $s refusé sans ROLE_ADMIN (GET /api/$r = 403)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $JETON' http://$s:$port/api/$r)\" = 403 ]"
  verifier "CRUD $s servi avec ROLE_ADMIN sur le réseau interne (GET /api/$r = 200)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $JETON_ADMIN' http://$s:$port/api/$r)\" = 200 ]"
done
# Contournement d'OPA fermé : la passerelle refuse /services/** même au jeton portant ROLE_ADMIN.
for cible in iam:collaborators participant:institutions incident:tickets audit:audit-events; do
  s=${cible%%:*}; r=${cible##*:}
  verifier "passerelle : /services/$s/api/$r refusé même avec ROLE_ADMIN (403)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $JETON_ADMIN' http://gateway:8080/services/$s/api/$r)\" = 403 ]"
done
verifier "OPA refuse par défaut (gimac.authz)" "$CURL -X POST http://opa:8181/v1/data/gimac/authz/allow -d '{\"input\":{}}' | grep -q '\"result\":false'"
verifier "OPA refuse /services/** à un administrateur (gimac.authz)" "$CURL -X POST http://opa:8181/v1/data/gimac/authz/allow -d '{\"input\":{\"method\":\"GET\",\"path\":\"/services/iam/api/collaborators\",\"permissions\":[\"ROLE_ADMIN\",\"IAM_ACCOUNT_MANAGE\"],\"authentifie\":true,\"acr\":\"2\"}}' | grep -q '\"result\":false'"

echo "== Stockage WORM (Ceph RGW)"
CLE="verification/$(date +%s).txt"
verifier "dépôt d'une preuve" "echo preuve | $AWS s3 cp - s3://gimac-preuves/$CLE"
VERSION=$($AWS s3api list-object-versions --bucket gimac-preuves --prefix "$CLE" --query 'Versions[0].VersionId' --output text 2>/dev/null)
verifier "suppression d'une version verrouillée refusée" "! $AWS s3api delete-object --bucket gimac-preuves --key $CLE --version-id $VERSION"

echo "== Simulacres et présentation"
verifier "simulateur en lecture seule (GET 200)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' http://simulateur-certification:8080/api/v1/liaisons/LIA-1/campagnes)\" = 200 ]"
verifier "simulateur refuse l'écriture (405)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' -X POST http://simulateur-certification:8080/api/v1/campagnes/X/resultats)\" = 405 ]"
for f in departement portail console; do
  # En-têtes seuls (-o /dev/null) : avec pipefail, grep -q couperait curl en pleine écriture du corps.
  verifier "couche $f servie avec CSP" "$CURL -o /dev/null -D - http://frontend-$f:8080/ | grep -qi content-security-policy"
done
CIBLES_ACTIVES=$($CURL 'http://prometheus:9090/api/v1/query?query=count(up==1)' | python3 -c 'import json,sys; r=json.load(sys.stdin)["data"]["result"]; print(r[0]["value"][1] if r else 0)')
verifier "Prometheus collecte les 14 services ($CIBLES_ACTIVES cibles actives)" "[ ${CIBLES_ACTIVES:-0} -ge 14 ]"

echo "== Bilan : $echecs échec(s)"
exit "$echecs"
