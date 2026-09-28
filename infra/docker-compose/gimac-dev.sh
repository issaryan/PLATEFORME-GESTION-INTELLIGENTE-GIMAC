#!/usr/bin/env bash
# Environnement local de la plateforme DES du GIMAC en une commande (SOC-13, D-45).
#
#   ./gimac-dev.sh verifier                 contrôle les prérequis
#   ./gimac-dev.sh images                   construit les images des 14 services (Jib, JHipster)
#   ./gimac-dev.sh demarrer [--socle]       démarre tout ; --socle : sans les 14 applications
#   ./gimac-dev.sh arreter                  arrête les conteneurs
#   ./gimac-dev.sh nettoyer                 arrête et supprime conteneurs et volumes
#   ./gimac-dev.sh etat                     état des conteneurs
set -euo pipefail

ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RACINE="$(cd "$ICI/../.." && pwd)"
SERVICES=(gateway iam audit document notification participant homologation
          certification incident monitoring rapprochement pilotage reporting collaboration)
compose() { docker compose --project-name gimac -f "$ICI/docker-compose.yml" -f "$ICI/docker-compose.gimac.yml" "$@"; }
echec() { echo "ERREUR : $*" >&2; exit 1; }

verifier() {
  command -v docker >/dev/null || echec "Docker est absent (prérequis : Docker Engine 24 ou plus)."
  docker info >/dev/null 2>&1 || echec "Le démon Docker ne répond pas ou l'utilisateur n'a pas accès au socket."
  docker compose version >/dev/null 2>&1 || echec "Le greffon Docker Compose v2 est absent."
  command -v openssl >/dev/null || echec "openssl est absent (nécessaire pour générer les secrets locaux)."
  local memoire_ko; memoire_ko=$(awk '/MemTotal/ {print $2}' /proc/meminfo 2>/dev/null || echo 0)
  [ "$memoire_ko" -ge 12000000 ] || echo "ATTENTION : moins de 12 Go de mémoire ; préférez « demarrer --socle »." >&2
  echo "Prérequis satisfaits."
}

# Secrets locaux générés aléatoirement, jamais versionnés (BNF-07, D-44).
preparer_env() {
  local fichier="$ICI/.env"
  [ -f "$fichier" ] && return
  umask 077
  {
    echo "# Secrets locaux de développement, générés par gimac-dev.sh. Ne pas versionner."
    for variable in GIMAC_KEYCLOAK_ADMIN_PASSWORD GIMAC_GRAFANA_ADMIN_PASSWORD \
                    GIMAC_MINIO_ROOT_PASSWORD GIMAC_OPENBAO_TOKEN; do
      echo "$variable=$(openssl rand -hex 24)"
    done
  } > "$fichier"
  echo "Fichier .env créé avec des secrets aléatoires."
}

images_manquantes() {
  local s; for s in "${SERVICES[@]}"; do
    docker image inspect "$s" >/dev/null 2>&1 || echo "$s"
  done
}

images() {
  local s; for s in "${SERVICES[@]}"; do
    echo "== Construction de l'image $s (Jib)"
    (cd "$RACINE/ms-$s" && ./mvnw -ntp -Pprod verify jib:dockerBuild -DskipTests)
  done
}

demarrer() {
  verifier
  preparer_env
  local cibles=()
  if [ "${1:-}" = "--socle" ]; then
    mapfile -t cibles < <(compose config --services | grep -vxF -f <(printf '%s\n' "${SERVICES[@]}"))
  else
    local manquantes; manquantes=$(images_manquantes)
    [ -z "$manquantes" ] || echec "images absentes : $(echo $manquantes). Lancez « ./gimac-dev.sh images » ou « demarrer --socle »."
  fi
  # Aucun composant partiellement démarré n'est laissé en place en cas d'échec.
  if ! compose up -d --wait "${cibles[@]}"; then
    echo "Échec du démarrage : arrêt des conteneurs lancés." >&2
    compose down
    exit 1
  fi
  compose ps
}

case "${1:-}" in
  verifier) verifier ;;
  images) images ;;
  demarrer) shift; demarrer "$@" ;;
  arreter) compose stop ;;
  nettoyer) compose down --volumes --remove-orphans ;;
  etat) compose ps ;;
  *) sed -n '2,10p' "$0"; exit 2 ;;
esac
