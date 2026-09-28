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
