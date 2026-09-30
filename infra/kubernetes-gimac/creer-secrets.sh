#!/usr/bin/env bash
# Crée dans l'espace de noms gimac-des les secrets retirés des déclarations générées.
# Valeurs aléatoires, jamais écrites sur disque ; un secret existant n'est pas modifié.
# À partir d'INT, ces secrets sont délivrés par OpenBao (SOC-06, D-44).
set -euo pipefail
NS=gimac-des
kubectl get namespace "$NS" >/dev/null 2>&1 || kubectl create namespace "$NS"
creer() { local nom=$1; shift
  if kubectl -n "$NS" get secret "$nom" >/dev/null 2>&1; then echo "existant : $nom"; return; fi
  kubectl -n "$NS" create secret generic "$nom" "$@" >/dev/null && echo "créé : $nom"; }
for base in gateway-postgresql iam-postgresql audit-postgresql document-postgresql notification-postgresql participant-postgresql homologation-postgresql certification-postgresql incident-postgresql monitoring-postgresql rapprochement-postgresql pilotage-postgresql reporting-postgresql collaboration-postgresql keycloak-postgresql; do
  creer "$base" --from-literal=postgresql-password="$(openssl rand -hex 24)"
done
creer jhipster-grafana-credentials --from-literal=username=gimac-admin --from-literal=password="$(openssl rand -hex 24)"
# Domaine « gimac » (D-62) : administrateur d'amorçage de Keycloak et secrets des clients de service.
creer keycloak-admin --from-literal=password="$(openssl rand -hex 24)"
creer gimac-clients-keycloak \
  --from-literal=mot-de-passe-test="$(openssl rand -hex 24)" \
  --from-literal=tester-cli="$(openssl rand -hex 24)" \
  --from-literal=homologation-service="$(openssl rand -hex 24)" \
  --from-literal=certification-service="$(openssl rand -hex 24)" \
  --from-literal=participant-service="$(openssl rand -hex 24)" \
  --from-literal=iam-service="$(openssl rand -hex 24)" \
  --from-literal=gateway-supervision="$(openssl rand -hex 24)" \
  --from-literal=collaboration-service="$(openssl rand -hex 24)" \
  --from-literal=internal="$(openssl rand -hex 24)"
# Messagerie de travail (D-64) : clé AES-256-GCM du chiffrement au repos (32 octets en base 64).
creer gimac-messagerie --from-literal=cle-chiffrement="$(openssl rand -base64 32)"
# Dépôt objet WORM de MS-DOCUMENT (D-50) : point d'accès et clés délivrés par l'exploitant de Ceph RGW.
if [ -n "${GIMAC_S3_POINT_ACCES:-}" ] && [ -n "${GIMAC_S3_CLE_ACCES:-}" ] && [ -n "${GIMAC_S3_CLE_SECRETE:-}" ]; then
  creer gimac-stockage-objet --from-literal=point-acces="$GIMAC_S3_POINT_ACCES" \
    --from-literal=cle-acces="$GIMAC_S3_CLE_ACCES" --from-literal=cle-secrete="$GIMAC_S3_CLE_SECRETE"
else
  echo "à fournir : GIMAC_S3_POINT_ACCES, GIMAC_S3_CLE_ACCES et GIMAC_S3_CLE_SECRETE (secret gimac-stockage-objet, MS-DOCUMENT)" >&2
fi
