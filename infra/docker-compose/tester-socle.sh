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
verifier() { # libellé, commande (vrai = OK)
  if eval "$2" >/dev/null 2>&1; then echo "OK     $1"; else echo "ÉCHEC  $1"; echecs=$((echecs + 1)); fi
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
JETON=$($CURL -X POST http://keycloak:9080/realms/jhipster/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=internal -d client_secret=internal -d scope=openid | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
verifier "jeton Keycloak obtenu (client_credentials, portée openid pour userinfo)" "[ -n \"$JETON\" ]"
# Sans jeton, la passerelle BFF redirige vers la connexion Keycloak (302) ; aucune donnée n'est servie.
verifier "API refusée sans jeton (302 vers Keycloak)" "$CURL -o /dev/null -w '%{http_code} %{redirect_url}' http://gateway:8080/services/iam/api/collaborators | grep -q '^302 .*oauth2/authorization'"
for cible in iam:collaborators participant:institutions incident:tickets audit:audit-events; do
  s=${cible%%:*}; r=${cible##*:}
  verifier "API $s atteinte via la passerelle avec jeton (GET /api/$r = 200)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $JETON' http://gateway:8080/services/$s/api/$r)\" = 200 ]"
done
verifier "OPA refuse par défaut" "$CURL -X POST http://opa:8181/v1/data/gimac/autorisation/autorise -d '{\"input\":{}}' | grep -q '\"result\":false'"

echo "== Stockage WORM (Ceph RGW)"
CLE="verification/$(date +%s).txt"
verifier "dépôt d'une preuve" "echo preuve | $AWS s3 cp - s3://gimac-preuves/$CLE"
VERSION=$($AWS s3api list-object-versions --bucket gimac-preuves --prefix "$CLE" --query 'Versions[0].VersionId' --output text 2>/dev/null)
verifier "suppression d'une version verrouillée refusée" "! $AWS s3api delete-object --bucket gimac-preuves --key $CLE --version-id $VERSION"

echo "== Simulacres et présentation"
verifier "simulateur en lecture seule (GET 200)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' http://simulateur-certification:8080/api/v1/liaisons/LIA-1/campagnes)\" = 200 ]"
verifier "simulateur refuse l'écriture (405)" "[ \"\$($CURL -o /dev/null -w '%{http_code}' -X POST http://simulateur-certification:8080/api/v1/campagnes/X/resultats)\" = 405 ]"
for f in departement portail console; do
  verifier "couche $f servie avec CSP" "$CURL -D - http://frontend-$f:8080/ | grep -qi content-security-policy"
done
CIBLES_ACTIVES=$($CURL 'http://prometheus:9090/api/v1/query?query=count(up==1)' | python3 -c 'import json,sys; r=json.load(sys.stdin)["data"]["result"]; print(r[0]["value"][1] if r else 0)')
verifier "Prometheus collecte les 14 services ($CIBLES_ACTIVES cibles actives)" "[ ${CIBLES_ACTIVES:-0} -ge 14 ]"

echo "== Bilan : $echecs échec(s)"
exit "$echecs"
