#!/usr/bin/env bash
# Contrôles de sortie de la Phase 0 du lot 1 (PI-GIMAC-01 §0.9), à travers la passerelle.
set -uo pipefail
ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CURL="docker run --rm --network gimac-des_default curlimages/curl:8.22.0 -s"
echecs=0
verifier() { # libellé, commande (vrai = OK) ; une seconde tentative absorbe un aléa réseau passager
  if eval "$2" >/dev/null 2>&1 || { sleep 3; eval "$2" >/dev/null 2>&1; }; then echo "OK     $1"; else echo "ÉCHEC  $1"; echecs=$((echecs + 1)); fi
}
code() { $CURL -o /dev/null -w '%{http_code}' "$@"; }
# Charge utile d'un jeton JWT (base64url sans remplissage).
charge() { python3 -c 'import sys,base64; p=sys.argv[1].split(".")[1]; p+="="*(-len(p)%4); print(base64.urlsafe_b64decode(p).decode())' "$1"; }

verifier "homologation : refus sans jeton (401)" "[ \$(code http://gateway:8080/bff/des/homologation/liaisons) = 401 ]"
source "$ICI/scripts/token.sh" test-chef-des || exit 1
CHEF="$TOKEN"
verifier "homologation : liste des liaisons avec jeton (200)" "[ \$(code -H 'Authorization: Bearer $CHEF' 'http://gateway:8080/bff/des/homologation/liaisons?page=0&size=1') = 200 ]"
verifier "certification : registre avec jeton (200)" "[ \$(code -H 'Authorization: Bearer $CHEF' 'http://gateway:8080/bff/des/certification/registre?page=0&size=1') = 200 ]"
source "$ICI/scripts/token.sh" test-correspondant-a || exit 1
CORR="$TOKEN"
verifier "OPA : un correspondant ne publie pas de référentiel (403)" "[ \$(code -X POST -H 'Authorization: Bearer $CORR' http://gateway:8080/bff/des/homologation/referentiels/x/publication) = 403 ]"
verifier "jeton du correspondant : claim institution_id" "charge '$CORR' | grep -q '\"institution_id\":\"INST-0042\"'"
verifier "jeton du chef du DES : permission HOM_REFERENTIEL_MIGRATE" "charge '$CHEF' | grep -q HOM_REFERENTIEL_MIGRATE"
set -a; . "$ICI/.env"; set +a
SERVICE=$($CURL -X POST http://keycloak:9080/realms/gimac/protocol/openid-connect/token -d grant_type=client_credentials \
  -d client_id=certification-service -d "client_secret=$GIMAC_SECRET_CERTIFICATION_SERVICE" -d scope=openid | python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token",""))')
verifier "jeton de service de MS-CERTIFICATION : INTERNAL_HOMOLOGATION_READ" "charge '$SERVICE' | grep -q INTERNAL_HOMOLOGATION_READ"
SUJETS=$(docker run --rm --network gimac-des_default apache/kafka:4.3.1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list 2>/dev/null)
verifier "sujets Kafka du lot et files de rebut" "echo \"$SUJETS\" | grep -qx homologation.liaison && echo \"$SUJETS\" | grep -qx certification.quitus.dlq"
echo "== Bilan Phase 0 : $echecs échec(s)"
exit "$echecs"
