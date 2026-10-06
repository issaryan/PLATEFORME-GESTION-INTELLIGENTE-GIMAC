#!/usr/bin/env bash
# Copies des fichiers partagés avec Docker Compose, lues par les générateurs de kustomization.yaml.
#
# Kustomize refuse de lire un fichier hors de son dossier (restriction de chargement, que ni
# « kubectl apply -k » ni infra/ci/valider-infra.sh ne lèvent) : les politiques OPA, la configuration
# d'Alertmanager, les règles d'alerte de Prometheus et le script des sujets Kafka, dont Docker Compose
# est la source, sont donc recopiés dans partages/ (et regles-alertes.yaml), sans aucune retouche.
#
#   ./synchroniser-partages.sh             recopie les sources dans partages/ et regles-alertes.yaml
#   ./synchroniser-partages.sh --verifier  échoue (code 1) si une copie diffère de sa source
#
# À lancer après toute modification d'une source, et avant chaque « kubectl apply -k . ».
set -euo pipefail

ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA="$(cd "$ICI/.." && pwd)"
POLITIQUES="$INFRA/docker-compose/opa/politiques"
MODE="${1:-copier}"
case "$MODE" in copier|--verifier) ;; *) echo "usage : $0 [--verifier]" >&2; exit 2 ;; esac

ecarts=0
# Paires « source copie » ; les politiques OPA sont toutes celles de Compose hors tests (*_test.rego),
# comme le --ignore=*_test.rego du conteneur opa de docker-compose.gimac.yml.
paires=()
for f in "$POLITIQUES"/*.rego; do
  case "$f" in *_test.rego) continue ;; esac
  paires+=("$f" "$ICI/partages/opa/$(basename "$f")")
done
paires+=("$INFRA/docker-compose/alertmanager-conf/config.yml" "$ICI/partages/alertmanager/config.yml")
paires+=("$INFRA/kafka/creer-sujets-lot1.sh" "$ICI/partages/kafka/creer-sujets-lot1.sh")

# Règles d'alerte : la ressource PrometheusRule porte, sous « spec », le contenu de alert_rules.yml.
regles_attendues() {
  cat <<'EOF'
# Généré par synchroniser-partages.sh depuis infra/docker-compose/prometheus-conf/alert_rules.yml :
# ne pas modifier à la main. Sélectionné par la ressource Prometheus jhipster-prometheus (ruleSelector).
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: gimac-alertes
  namespace: gimac-des
  labels:
    team: gimac-des
spec:
EOF
  sed 's/^/  /; s/^  $//' "$INFRA/docker-compose/prometheus-conf/alert_rules.yml"
}

i=0
while [ "$i" -lt "${#paires[@]}" ]; do
  source="${paires[$i]}" copie="${paires[$((i + 1))]}"
  i=$((i + 2))
  if [ "$MODE" = copier ]; then
    mkdir -p "$(dirname "$copie")"
    cp "$source" "$copie"
    echo "copié : ${copie#"$ICI"/}"
  elif ! cmp -s "$source" "$copie"; then
    echo "désynchronisé : ${copie#"$ICI"/} (source ${source#"$INFRA"/})" >&2
    ecarts=1
  fi
done

if [ "$MODE" = copier ]; then
  regles_attendues > "$ICI/regles-alertes.yaml"
  echo "généré : regles-alertes.yaml"
else
  if ! regles_attendues | cmp -s - "$ICI/regles-alertes.yaml"; then
    echo "désynchronisé : regles-alertes.yaml (source docker-compose/prometheus-conf/alert_rules.yml)" >&2
    ecarts=1
  fi
  # Copie sans source (politique supprimée ou renommée dans Compose).
  for copie in "$ICI"/partages/opa/*.rego; do
    [ -e "$POLITIQUES/$(basename "$copie")" ] || { echo "copie orpheline : ${copie#"$ICI"/}" >&2; ecarts=1; }
  done
fi

# Toute politique copiée doit figurer dans le générateur opa-politiques de kustomization.yaml.
for copie in "$ICI"/partages/opa/*.rego; do
  grep -qF "partages/opa/$(basename "$copie")" "$ICI/kustomization.yaml" \
    || { echo "absente de kustomization.yaml (opa-politiques) : partages/opa/$(basename "$copie")" >&2; ecarts=1; }
done

if [ "$ecarts" -ne 0 ]; then
  echo "lancer ./synchroniser-partages.sh puis vérifier kustomization.yaml" >&2
  exit 1
fi
[ "$MODE" = --verifier ] && echo "copies conformes à leurs sources"
exit 0
