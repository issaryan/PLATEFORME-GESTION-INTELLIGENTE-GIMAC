#!/usr/bin/env bash
# Validation statique de toute l'infrastructure (D-61) : scripts, Compose, politiques OPA,
# Kubernetes, supervision, Dockerfiles, domaine Keycloak et YAML.
# Aucun conteneur n'est démarré : chaque contrôle s'exécute en quelques secondes.
#
#   infra/ci/valider-infra.sh            # tous les contrôles
#   GIMAC_CI_STRICT=1 infra/ci/valider-infra.sh   # un outil absent devient un échec (CI)
#
# Outils : shellcheck, docker compose, opa, kustomize, kubeconform, promtool, amtool,
# hadolint, python3, yamllint, actionlint. Installation : infra/ci/installer-outils.sh.
set -euo pipefail

# shellcheck source=infra/ci/commun.sh
source "$(dirname "${BASH_SOURCE[0]}")/commun.sh"
cd "$RACINE_DEPOT"

INFRA=infra
COMPOSE="$INFRA/docker-compose"
# Déclarations Kubernetes produites par « jhipster kubernetes » (D-48) : jamais modifiées à
# la main, elles sont validées à travers le rendu Kustomize de kubernetes-gimac.
GENERE_K8S="$INFRA/kubernetes"
# Version de Kubernetes ciblée par la validation des schémas.
VERSION_K8S="${GIMAC_VERSION_K8S:-1.33.0}"

# ---------------------------------------------------------------- 1. Scripts shell
valider_scripts() {
  section "Scripts shell (shellcheck)"
  exiger_outil shellcheck "shellcheck des scripts de infra" || { fin_section; return 0; }
  local scripts=()
  mapfile -t scripts < <(find "$INFRA" -name '*.sh' -not -path '*/node_modules/*' -not -path "$GENERE_K8S/*" | sort)
  info "${#scripts[@]} scripts (hors $GENERE_K8S, généré par JHipster)"
  # Seuil « warning » : les notes de style ne bloquent pas, les défauts réels si.
  controler "shellcheck (${#scripts[@]} scripts de infra)" shellcheck -x -S warning -f gcc "${scripts[@]}"
  local s
  for s in "${scripts[@]}"; do
    bash -n "$s" 2>/dev/null || sh -n "$s" || echec "syntaxe du script $s"
  done
  fin_section
}

# ---------------------------------------------------------------- 2. Docker Compose
# Les variables obligatoires (${VAR:?…}) sont des secrets du .env local, jamais versionné
# (BNF-07) : on leur donne une valeur factice le temps du contrôle.
valider_compose() {
  section "Docker Compose (docker compose config)"
  if ! outil_present docker || ! docker compose version >/dev/null 2>&1; then
    ignore "docker compose config : Docker Compose v2 introuvable"; fin_section; return 0
  fi
  local variables=() v
  mapfile -t variables < <(grep -ohE '\$\{[A-Z0-9_]+(:\?|\?|:-|-)?' "$COMPOSE"/docker-compose*.yml \
    | sed -E 's/^\$\{//; s/[:?-]+$//' | sort -u)
  local env_factice="$DOSSIER_TRAVAIL/compose.env"
  : > "$env_factice"
  for v in "${variables[@]}"; do
    case "$v" in
      GIMAC_PORT_*) ;;                              # les ports gardent leur valeur par défaut
      *) echo "$v=valeur-factice-ci" >> "$env_factice" ;;
    esac
  done
  info "${#variables[@]} variables substituées (valeurs factices pour les secrets)"
  controler "docker-compose.yml seul" \
    docker compose --project-directory "$COMPOSE" --env-file "$env_factice" -f "$COMPOSE/docker-compose.yml" config -q
  controler "docker-compose.yml + docker-compose.gimac.yml (fusion de gimac-dev.sh)" \
    docker compose --project-directory "$COMPOSE" --env-file "$env_factice" \
      -f "$COMPOSE/docker-compose.yml" -f "$COMPOSE/docker-compose.gimac.yml" config -q
  # Surcouche du serveur de démonstration (deployer-serveur.sh) : Keycloak persistant, mandataire du
  # moteur Docker de l'exploitation depuis la console (D-78).
  controler "docker-compose.yml + docker-compose.gimac.yml + docker-compose.serveur.yml (deployer-serveur.sh)" \
    docker compose --project-name gimac-des --project-directory "$COMPOSE" --env-file "$env_factice" \
      -f "$COMPOSE/docker-compose.yml" -f "$COMPOSE/docker-compose.gimac.yml" -f "$COMPOSE/docker-compose.serveur.yml" config -q
  fin_section
}

# ---------------------------------------------------------------- 3. Politiques OPA
valider_opa() {
  section "Politiques OPA (opa check, fmt, test)"
  exiger_outil opa "politiques OPA" || { fin_section; return 0; }
  local politiques="$COMPOSE/opa/politiques"
  controler "opa check --strict ($politiques)" opa check --strict "$politiques"
  controler "opa fmt : politiques au format canonique" opa fmt --list --fail "$politiques"
  if compgen -G "$politiques/*_test.rego" >/dev/null; then
    controler "opa test (tests unitaires des politiques)" opa test --verbose=false "$politiques"
    # Couverture des politiques par leurs tests : même seuil que le code (D-61, 80 %).
    local couverture
    if couverture="$(opa test --coverage --threshold 80 --format=json "$politiques")"; then
      reussi "opa test --coverage ≥ 80 % ($(python3 -c 'import json,sys; print(round(json.load(sys.stdin)["coverage"], 1))' <<<"$couverture") %)"
    else
      echec "opa test --coverage : couverture des politiques inférieure à 80 %"
    fi
  else
    ignore "opa test : aucun fichier *_test.rego"
  fi
  fin_section
}

# ---------------------------------------------------------------- 4. Kubernetes
valider_kubernetes() {
  section "Kubernetes (kustomize build, kubeconform)"
  # Copies des politiques OPA, d'Alertmanager et du script des sujets Kafka (kustomize ne lit pas hors de
  # son dossier) : elles doivent rester identiques à leurs sources.
  controler "copies partagées de kubernetes-gimac conformes à leurs sources" \
    bash "$INFRA/kubernetes-gimac/synchroniser-partages.sh" --verifier
  exiger_outil kustomize "rendu Kustomize de kubernetes-gimac" || { fin_section; return 0; }
  local rendu="$DOSSIER_TRAVAIL/kubernetes-gimac.yaml"
  if kustomize build "$INFRA/kubernetes-gimac" > "$rendu"; then
    reussi "kustomize build infra/kubernetes-gimac ($(grep -c '^kind:' "$rendu") objets)"
  else
    echec "kustomize build infra/kubernetes-gimac"; fin_section; return 0
  fi
  # Aucun secret dans le rendu (BNF-07, D-44) : ils sont créés au déploiement.
  if grep -qE '^kind: Secret$' "$rendu"; then
    echec "le rendu Kustomize contient un Secret (retrait-secrets-generes.yaml incomplet)"
  else
    reussi "aucun Secret dans le rendu Kustomize (BNF-07)"
  fi
  # Images : « latest » interdit ; une image sans étiquette (services, fixée au déploiement) est signalée.
  local images sans_etiquette
  images="$(sed -nE 's/^[[:space:]]+image: *//p' "$rendu" | tr -d '"' | sort -u)"
  if grep -q ':latest$' <<<"$images"; then
    grep ':latest$' <<<"$images" | sed 's/^/    /'
    echec "images Kubernetes en « latest »"
  else
    reussi "aucune image Kubernetes en « latest »"
  fi
  sans_etiquette="$(grep -vE '@sha256:|:[^/]+$' <<<"$images" || true)"
  if [ -n "$sans_etiquette" ]; then
    info "Images sans étiquette (fixée au déploiement par kustomize edit set image) :"
    indenter <<<"$sans_etiquette"
  fi
  exiger_outil kubeconform "validation des schémas Kubernetes" || { fin_section; return 0; }
  # Schémas des CRD (Kyverno, Prometheus Operator) : catalogue datreeio ; une ressource
  # sans schéma connu est signalée mais ne bloque pas.
  controler "kubeconform (schémas Kubernetes $VERSION_K8S et CRD connues)" \
    kubeconform -strict -summary -ignore-missing-schemas \
      -kubernetes-version "$VERSION_K8S" \
      -schema-location default \
      -schema-location 'https://raw.githubusercontent.com/datreeio/CRDs-catalog/main/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json' \
      "$rendu"
  fin_section
}

# ---------------------------------------------------------------- 5. Supervision
valider_supervision() {
  section "Supervision (promtool, amtool)"
  if exiger_outil promtool "configuration Prometheus"; then
    controler "promtool check config prometheus.yml" promtool check config "$COMPOSE/prometheus-conf/prometheus.yml"
    controler "promtool check rules alert_rules.yml" promtool check rules "$COMPOSE/prometheus-conf/alert_rules.yml"
  fi
  if exiger_outil amtool "configuration Alertmanager"; then
    controler "amtool check-config alertmanager-conf/config.yml" amtool check-config "$COMPOSE/alertmanager-conf/config.yml"
  fi
  fin_section
}

# ---------------------------------------------------------------- 6. Dockerfiles
valider_dockerfiles() {
  section "Dockerfiles (hadolint)"
  exiger_outil hadolint "hadolint des Dockerfiles" || { fin_section; return 0; }
  local fichiers=() f
  mapfile -t fichiers < <(find infra frontend ms-* -name 'Dockerfile*' -not -path '*/node_modules/*' -not -path '*/target/*' 2>/dev/null | sort)
  for f in "${fichiers[@]}"; do
    # Seuil « warning » : DL3018 (versions apk) et consorts restent des informations.
    controler "hadolint $f" hadolint --no-color --failure-threshold warning "$f"
  done
  fin_section
}

# ---------------------------------------------------------------- 7. Domaine Keycloak
# gimac-realm.json est produit par generer-realm-gimac.py (D-62) : on le régénère dans une
# copie de l'arborescence et on le compare au fichier versionné.
valider_keycloak() {
  section "Domaine Keycloak gimac (régénération et comparaison)"
  exiger_outil python3 "régénération du domaine Keycloak" || { fin_section; return 0; }
  local copie="$DOSSIER_TRAVAIL/realm"
  mkdir -p "$copie/infra/keycloak" "$copie/infra/docker-compose/realm-config"
  cp "$INFRA/keycloak/generer-realm-gimac.py" "$copie/infra/keycloak/"
  cp "$COMPOSE/realm-config/jhipster-realm.json" "$copie/infra/docker-compose/realm-config/"
  if python3 "$copie/infra/keycloak/generer-realm-gimac.py" >/dev/null; then
    if diff -q "$INFRA/keycloak/gimac-realm.json" "$copie/infra/keycloak/gimac-realm.json" >/dev/null \
      && diff -q "$INFRA/keycloak/gimac-realm-production.json" "$copie/infra/keycloak/gimac-realm-production.json" >/dev/null; then
      reussi "gimac-realm.json et gimac-realm-production.json (sans compte de test) identiques à la sortie de generer-realm-gimac.py"
    else
      diff -u "$INFRA/keycloak/gimac-realm.json" "$copie/infra/keycloak/gimac-realm.json" | head -40 || true
      echec "gimac-realm.json désynchronisé : lancer « python3 infra/keycloak/generer-realm-gimac.py » et versionner le résultat"
    fi
  else
    echec "generer-realm-gimac.py a échoué"
  fi
  # Aucun secret en clair pour un client confidentiel (BNF-07, D-44) : seules des variables
  # ${…} résolues à l'import sont admises. Les clients publics n'utilisent pas leur secret.
  local secrets_en_clair
  secrets_en_clair="$(python3 - "$INFRA/keycloak/gimac-realm.json" <<'PY'
import json, sys
domaine = json.load(open(sys.argv[1], encoding="utf-8"))
for c in domaine.get("clients", []):
    s = c.get("secret")
    if s and not s.startswith("${") and not c.get("publicClient") and not c.get("bearerOnly"):
        compte = " (compte de service actif)" if c.get("serviceAccountsEnabled") else ""
        print(f"{c['clientId']}{compte}")
PY
)"
  if [ -n "$secrets_en_clair" ]; then
    indenter "client confidentiel à secret en clair : " <<<"$secrets_en_clair"
    echec "gimac-realm.json : secrets de clients confidentiels en clair (à remplacer par des variables \${…})"
  else
    reussi "gimac-realm.json : secrets des clients confidentiels sous forme de variables \${…}"
  fi
  fin_section
}

# ---------------------------------------------------------------- 8. JSON et YAML
valider_json_yaml() {
  section "JSON et YAML (syntaxe, yamllint)"
  local json=() f ko=0
  mapfile -t json < <(find infra -name '*.json' -not -path '*/node_modules/*' | sort)
  for f in "${json[@]}"; do
    python3 -m json.tool "$f" >/dev/null 2>&1 || { echo "    JSON invalide : $f"; ko=1; }
  done
  if [ "$ko" = 0 ]; then reussi "JSON valides (${#json[@]} fichiers)"; else echec "JSON invalides"; fi
  exiger_outil yamllint "yamllint de infra" || { fin_section; return 0; }
  local cibles=(infra)
  [ -d .github ] && cibles+=(.github)
  controler "yamllint (${cibles[*]})" yamllint -c "$INFRA/ci/yamllint.yml" -f parsable "${cibles[@]}"
  fin_section
}

# ---------------------------------------------------------------- 9. Chaîne CI elle-même
valider_workflows() {
  section "Définition de la chaîne (actionlint)"
  if [ ! -d .github/workflows ]; then info "aucun workflow"; fin_section; return 0; fi
  exiger_outil actionlint "actionlint des workflows GitHub Actions" || { fin_section; return 0; }
  controler "actionlint (.github/workflows)" actionlint
  fin_section
}

valider_scripts
valider_compose
valider_opa
valider_kubernetes
valider_supervision
valider_dockerfiles
valider_keycloak
valider_json_yaml
valider_workflows
bilan "Validation de l'infrastructure"
