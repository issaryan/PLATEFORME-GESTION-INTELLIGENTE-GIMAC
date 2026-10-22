#!/usr/bin/env bash
# Crée dans l'espace de noms gimac-des les secrets retirés des déclarations générées.
# Crée aussi la ConfigMap gimac-dispositifs-externes (adresses des dispositifs extérieurs à la grappe).
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
  --from-literal=pilotage-service="$(openssl rand -hex 24)" \
  --from-literal=monitoring-service="$(openssl rand -hex 24)" \
  --from-literal=notification-service="$(openssl rand -hex 24)" \
  --from-literal=reprise-service="$(openssl rand -hex 24)" \
  --from-literal=internal="$(openssl rand -hex 24)" \
  --from-literal=connexion="$(openssl rand -hex 24)" \
  --from-literal=web-app="$(openssl rand -hex 24)" \
  --from-literal=gateway-service="$(openssl rand -hex 24)"
# Clé ajoutée après la première création du secret (D-107, client reprise-service) : complétée sans toucher aux autres.
if ! kubectl -n "$NS" get secret gimac-clients-keycloak -o jsonpath='{.data.reprise-service}' 2>/dev/null | grep -q .; then
  kubectl -n "$NS" patch secret gimac-clients-keycloak --type merge \
    -p "{\"stringData\":{\"reprise-service\":\"$(openssl rand -hex 24)\"}}" >/dev/null && echo "complété : gimac-clients-keycloak (reprise-service)"
fi
# Clients de la Sandbox GIMAC (D-119), ajoutés après la première création du secret : complétés sans toucher aux autres.
for cle in sbx-espace; do
  if ! kubectl -n "$NS" get secret gimac-clients-keycloak -o jsonpath="{.data.$cle}" 2>/dev/null | grep -q .; then
    kubectl -n "$NS" patch secret gimac-clients-keycloak --type merge \
      -p "{\"stringData\":{\"$cle\":\"$(openssl rand -hex 24)\"}}" >/dev/null && echo "complété : gimac-clients-keycloak ($cle)"
  fi
done
# Messagerie de travail (D-64) : clé AES-256-GCM du chiffrement au repos (32 octets en base 64).
creer gimac-messagerie --from-literal=cle-chiffrement="$(openssl rand -base64 32)"
# Clé AES-256-GCM des secrets de MS-IAM (enrôlement TOTP, mot de passe SMTP, D-65).
creer gimac-iam --from-literal=cle-chiffrement="$(openssl rand -base64 32)"
# Dépôt objet WORM de MS-DOCUMENT (D-50) : point d'accès et clés délivrés par l'exploitant de Ceph RGW.
if [ -n "${GIMAC_S3_POINT_ACCES:-}" ] && [ -n "${GIMAC_S3_CLE_ACCES:-}" ] && [ -n "${GIMAC_S3_CLE_SECRETE:-}" ]; then
  creer gimac-stockage-objet --from-literal=point-acces="$GIMAC_S3_POINT_ACCES" \
    --from-literal=cle-acces="$GIMAC_S3_CLE_ACCES" --from-literal=cle-secrete="$GIMAC_S3_CLE_SECRETE"
else
  echo "à fournir : GIMAC_S3_POINT_ACCES, GIMAC_S3_CLE_ACCES et GIMAC_S3_CLE_SECRETE (secret gimac-stockage-objet, MS-DOCUMENT)" >&2
fi
# Dispositifs extérieurs (non secrets) : adresse du dispositif réel de gestion des changements, lue par
# MS-CERTIFICATION (GIMAC_GESTION_CHANGEMENTS_URL). Aucun simulacre en Kubernetes : sans cette ConfigMap,
# le pod de MS-CERTIFICATION ne démarre pas. Une ConfigMap existante n'est pas modifiée.
if kubectl -n "$NS" get configmap gimac-dispositifs-externes >/dev/null 2>&1; then
  echo "existant : gimac-dispositifs-externes"
elif [ -n "${GIMAC_GESTION_CHANGEMENTS_URL:-}" ]; then
  kubectl -n "$NS" create configmap gimac-dispositifs-externes \
    --from-literal=gestion-changements-url="$GIMAC_GESTION_CHANGEMENTS_URL" >/dev/null && echo "créé : gimac-dispositifs-externes"
else
  echo "à fournir : GIMAC_GESTION_CHANGEMENTS_URL (ConfigMap gimac-dispositifs-externes, MS-CERTIFICATION)" >&2
fi
