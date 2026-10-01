#!/usr/bin/env bash
# Déploiement de la plateforme sur un serveur Docker (démonstration, 10.20.36.214) : construction
# des images sur place, démarrage par gimac-dev.sh avec la surcouche docker-compose.serveur.yml,
# puis amorçage des comptes nominatifs. Exécuté par .github/workflows/deploiement-serveur.yml
# (exécuteur auto-hébergé du serveur) à chaque poussée sur main, ou à la main sur le serveur.
#
#   infra/ci/deployer-serveur.sh [hote-public]     (défaut : GIMAC_HOTE_PUBLIC du .env, sinon hostname -I)
#
# Prérequis : Docker et Compose v2 accessibles à l'utilisateur ; Java 21 et Maven sont installés
# au besoin dans ~/.local/opt, sans droits d'administration. Le .env (secrets) et
# infra/keycloak/local/comptes.json restent sur le serveur, jamais dans le dépôt (BNF-07).
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_DIR="$RACINE/infra/docker-compose"
ENV="$COMPOSE_DIR/.env"
OUTILS="$HOME/.local/opt"
JDK_URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
MAVEN_VERSION="3.9.11"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
export GIMAC_COMPOSE_SUPPLEMENT="$COMPOSE_DIR/docker-compose.serveur.yml"

etape() { echo; echo "== $*"; }

# Un seul déploiement à la fois sur l'hôte.
exec 9>"${TMPDIR:-/tmp}/gimac-deploiement.verrou"
flock 9

outils() {
  mkdir -p "$OUTILS"
  if [ ! -x "$OUTILS/jdk-21/bin/java" ]; then
    etape "Installation de Java 21 (Temurin) dans $OUTILS/jdk-21"
    rm -rf "$OUTILS/jdk-21" && mkdir -p "$OUTILS/jdk-21"
    curl -fsSL "$JDK_URL" | tar -xz -C "$OUTILS/jdk-21" --strip-components=1
  fi
  if [ ! -x "$OUTILS/maven/bin/mvn" ]; then
    etape "Installation de Maven $MAVEN_VERSION dans $OUTILS/maven"
    rm -rf "$OUTILS/maven" && mkdir -p "$OUTILS/maven"
    curl -fsSL "$MAVEN_URL" | tar -xz -C "$OUTILS/maven" --strip-components=1
  fi
  export JAVA_HOME="$OUTILS/jdk-21"
  export PATH="$JAVA_HOME/bin:$OUTILS/maven/bin:$PATH"
}

# Variable du .env fixée seulement si absente : une valeur choisie sur le serveur est conservée.
definir() { grep -q "^$1=" "$ENV" || echo "$1=$2" >> "$ENV"; }

configurer_env() {
  umask 077
  [ -f "$ENV" ] || echo "# Secrets et adresses du serveur, générés par deployer-serveur.sh. Ne pas versionner." > "$ENV"
  local hote="${1:-}"
  [ -n "$hote" ] || hote="$(sed -n 's/^GIMAC_HOTE_PUBLIC=//p' "$ENV")"
  [ -n "$hote" ] || hote="$(hostname -I | awk '{print $1}')"
  definir GIMAC_HOTE_PUBLIC "$hote"
  definir GIMAC_PORT_FRONTEND_DEPARTEMENT 4200
  definir GIMAC_PORT_FRONTEND_PORTAIL 4201
  definir GIMAC_PORT_FRONTEND_CONSOLE 4202
  definir GIMAC_URL_DES "http://$hote:4200"
  definir GIMAC_URL_PORTAIL "http://$hote:4201"
  definir GIMAC_URL_CONSOLE "http://$hote:4202"
  definir GIMAC_WEBAUTHN_RP_ID "$hote"
  definir GIMAC_WEBAUTHN_ORIGINES "http://$hote:4200,http://$hote:4201,http://$hote:4202"
  # Service en http sous une adresse IP : un témoin Secure n'y serait jamais renvoyé.
  definir GIMAC_SESSION_TEMOIN_SECURISE false
}

compose() {
  docker compose --project-name gimac-des -f "$COMPOSE_DIR/docker-compose.yml" \
    -f "$COMPOSE_DIR/docker-compose.gimac.yml" -f "$GIMAC_COMPOSE_SUPPLEMENT" --env-file "$ENV" "$@"
}

outils
configurer_env "${1:-}"

etape "Images des 14 microservices et extension Keycloak"
"$COMPOSE_DIR/gimac-dev.sh" images

etape "Images des trois couches de présentation"
compose build frontend-departement frontend-portail frontend-console

etape "Démarrage"
"$COMPOSE_DIR/gimac-dev.sh" demarrer

if [ -f "$RACINE/infra/keycloak/local/comptes.json" ]; then
  etape "Comptes nominatifs (les comptes existants sont conservés)"
  python3 "$RACINE/infra/keycloak/creer-comptes-personnels.py"
fi

docker image prune -f >/dev/null

hote="$(sed -n 's/^GIMAC_HOTE_PUBLIC=//p' "$ENV")"
etape "Plateforme déployée"
echo "  Application du département : http://$hote:4200"
echo "  Portail des participants    : http://$hote:4201"
echo "  Console d'administration    : http://$hote:4202"
