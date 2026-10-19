#!/usr/bin/env bash
# Déploiement de la plateforme sur un serveur Docker (démonstration, 10.20.36.214) : images, démarrage
# par gimac-dev.sh avec la surcouche docker-compose.serveur.yml, puis amorçage des comptes nominatifs.
#
#   infra/ci/deployer-serveur.sh [hote-public]
#       construction des images sur place depuis une copie du dépôt (.github/workflows/deploiement-serveur.yml) ;
#   infra/ci/deployer-serveur.sh --images <dossier> [hote-public]
#       sans code source : depuis le paquet de infra/ci/paquet-serveur.sh, images listées dans
#       <dossier>/images-*.txt (« nom référence ») puis étiquetées sous les noms des fichiers Compose.
#       Référence déjà présente dans le Docker du serveur (livraison depuis un poste, infra/ci/livrer-serveur.sh),
#       sinon tirée du registre (« image@sha256:… » publiée par la CI, deploiement-serveur-images.yml).
#   hote-public : défaut GIMAC_HOTE_PUBLIC du .env, sinon hostname -I.
#
# Prérequis : Docker et Compose v2 accessibles à l'utilisateur ; en construction sur place, Java 21 et
# Maven sont installés au besoin dans ~/.local/opt, sans droits d'administration. Le .env (secrets) et
# infra/keycloak/local/comptes.json restent sur le serveur, jamais dans le dépôt (BNF-07).
set -euo pipefail

LISTES_IMAGES=""
if [ "${1:-}" = --images ]; then
  LISTES_IMAGES="${2:?--images <dossier des listes images-*.txt>}"
  shift 2
fi

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_DIR="$RACINE/infra/docker-compose"
ENV="$COMPOSE_DIR/.env"
OUTILS="$HOME/.local/opt"
JDK_URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
MAVEN_VERSION="3.9.11"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
export GIMAC_COMPOSE_SUPPLEMENT="$COMPOSE_DIR/docker-compose.serveur.yml"
# Exploitation du serveur (D-111) : redémarrage automatique de chaque service après un redémarrage de
# l'hôte et journaux de conteneur bornés, par la surcouche que génère gimac-dev.sh.
export GIMAC_EXPLOITATION=1

etape() { echo; echo "== $*"; }

# État du serveur hors de la copie de travail, qu'un exécuteur peut recloner : secrets du .env
# (liés aux volumes, dont la base de Keycloak), comptes nominatifs et mots de passe provisoires.
ETAT_SERVEUR="$HOME/.config/gimac-des"
lier_etat() {
  local cible="$1" nom="$2"
  mkdir -p "$ETAT_SERVEUR" "$(dirname "$cible")"
  chmod 700 "$ETAT_SERVEUR"
  if [ -f "$cible" ] && [ ! -L "$cible" ] && [ ! -e "$ETAT_SERVEUR/$nom" ]; then
    mv "$cible" "$ETAT_SERVEUR/$nom"
  elif [ "$nom" = env ] && [ -f "$cible" ] && [ ! -L "$cible" ]; then
    # .env redevenu un fichier ordinaire (avant --follow-symlinks, sed -i rompait le lien) : les variables
    # qu'il est seul à porter rejoignent l'état, au lieu d'être régénérées avec une autre valeur.
    local ligne
    while IFS= read -r ligne; do
      [[ "$ligne" =~ ^([A-Z0-9_]+)= ]] || continue
      grep -q "^${BASH_REMATCH[1]}=" "$ETAT_SERVEUR/$nom" || printf '%s\n' "$ligne" >> "$ETAT_SERVEUR/$nom"
    done < "$cible"
  fi
  [ -e "$ETAT_SERVEUR/$nom" ] || { [ "$nom" = comptes.json ] && return 0; touch "$ETAT_SERVEUR/$nom"; chmod 600 "$ETAT_SERVEUR/$nom"; }
  ln -sfn "$ETAT_SERVEUR/$nom" "$cible"
}
lier_etat "$ENV" env
lier_etat "$RACINE/infra/keycloak/local/comptes.json" comptes.json
lier_etat "$RACINE/infra/keycloak/local/mots-de-passe-provisoires.txt" mots-de-passe-provisoires.txt

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
  definir GIMAC_KEYCLOAK_DB_PASSWORD "$(openssl rand -hex 20)"
}

# Publication (D-75, infra/serveur/publication.env) : en mode mixte ou https, les adresses publiques,
# les origines des clés de sécurité et le témoin de session suivent les noms HTTPS du GIMAC. En mode
# http, le .env reste tel quel (adresse IP et ports).
# --follow-symlinks : le .env est un lien vers l'état du serveur, que sed -i remplacerait par une copie.
fixer() {
  if grep -q "^$1=" "$ENV"; then sed -i --follow-symlinks "s|^$1=.*|$1=$2|" "$ENV"; else echo "$1=$2" >> "$ENV"; fi
}
appliquer_publication() {
  local pub="$RACINE/infra/serveur/publication.env" mode domaine port suffixe hote interne portail console
  [ -f "$pub" ] || return 0
  mode="$(sed -n 's/^GIMAC_PUBLICATION=//p' "$pub")"
  case "$mode" in
    http) return 0 ;;
    mixte | https) ;;
    *) echo "ERREUR : GIMAC_PUBLICATION doit valoir http, mixte ou https ($mode)" >&2; exit 1 ;;
  esac
  domaine="$(sed -n 's/^GIMAC_DOMAINE=//p' "$pub")"
  port="$(sed -n 's/^GIMAC_PORT_HTTPS=//p' "$pub")"
  suffixe=""; [ "${port:-443}" = 443 ] || suffixe=":$port"
  interne="https://$(sed -n 's/^GIMAC_NOM_INTERNE=//p' "$pub").$domaine$suffixe"
  portail="https://$(sed -n 's/^GIMAC_NOM_PORTAIL=//p' "$pub").$domaine$suffixe"
  console="https://$(sed -n 's/^GIMAC_NOM_CONSOLE=//p' "$pub").$domaine$suffixe"
  hote="$(sed -n 's/^GIMAC_HOTE_PUBLIC=//p' "$ENV")"
  etape "Publication $mode : $interne, $portail, $console"
  fixer GIMAC_URL_DES "$interne"
  fixer GIMAC_URL_PORTAIL "$portail"
  fixer GIMAC_URL_CONSOLE "$console"
  # Clés de sécurité liées au domaine du GIMAC : valables sur les trois noms.
  fixer GIMAC_WEBAUTHN_RP_ID "$domaine"
  if [ "$mode" = mixte ]; then
    fixer GIMAC_WEBAUTHN_ORIGINES "$interne,$portail,$console,http://$hote:4200,http://$hote:4201,http://$hote:4202"
    # L'accès par l'adresse IP en http reste possible : témoin non Secure pendant la transition.
    fixer GIMAC_SESSION_TEMOIN_SECURISE false
  else
    fixer GIMAC_WEBAUTHN_ORIGINES "$interne,$portail,$console"
    fixer GIMAC_SESSION_TEMOIN_SECURISE true
  fi
}

compose() {
  local fichiers=(-f "$COMPOSE_DIR/docker-compose.yml" -f "$COMPOSE_DIR/docker-compose.gimac.yml" -f "$GIMAC_COMPOSE_SUPPLEMENT")
  [ -f "$COMPOSE_DIR/docker-compose.exploitation.yml" ] && fichiers+=(-f "$COMPOSE_DIR/docker-compose.exploitation.yml")
  docker compose --project-name gimac-des "${fichiers[@]}" --env-file "$ENV" "$@"
}

# Keycloak est persistant (docker-compose.serveur.yml) : --import-realm ignore un domaine existant.
# Quand gimac-realm.json change, le domaine en service est exporté avec ses comptes (kc.sh export,
# mots de passe hachés et codes TOTP compris), fusionné avec la version du dépôt, puis réimporté.
DOMAINE="$RACINE/infra/keycloak/gimac-realm.json"
# Les fichiers d'échange contiennent des empreintes de mots de passe : supprimés en sortie.
ECHANGE=""
trap 'rm -rf "${ECHANGE:-/nonexistent}"' EXIT
EMPREINTE="$HOME/.local/state/gimac-des/gimac-realm.sha256"
# Domaine du dépôt déployé la fois précédente : base de la fusion à trois voies des rôles (fusionner-domaine.py).
DOMAINE_PRECEDENT="$HOME/.local/state/gimac-des/gimac-realm.precedent.json"

actualiser_domaine() {
  local nouvelle
  nouvelle="$(sha256sum "$DOMAINE" | cut -d' ' -f1)"
  docker volume inspect gimac-des_keycloak_donnees >/dev/null 2>&1 || return 0   # premier import
  [ "$(cat "$EMPREINTE" 2>/dev/null)" != "$nouvelle" ] || return 0
  etape "Domaine gimac modifié : réimport en conservant les comptes"
  ECHANGE="$(mktemp -d)"
  mkdir -p "$ECHANGE/import"
  compose stop keycloak
  compose up -d --wait keycloak-postgresql
  # Volume présent mais domaine jamais importé (premier démarrage interrompu) : rien à conserver,
  # l'import du démarrage (--import-realm) crée le domaine.
  if [ -z "$(compose exec -T keycloak-postgresql psql -U keycloak -d keycloak -tAc \
      "select 1 from realm where name = 'gimac'" 2>/dev/null)" ]; then
    echo "  Domaine gimac absent de la base : import au démarrage"
    return 0
  fi
  compose run --rm --no-deps --user "$(id -u):0" -v "$ECHANGE:/echange" keycloak \
    export --dir /echange --realm gimac --users realm_file
  local base=()
  [ -s "$DOMAINE_PRECEDENT" ] && base=("$DOMAINE_PRECEDENT")
  # Comptes de test du générateur écartés : supprimés du serveur, ils n'y reviennent pas (13/10/2026).
  GIMAC_SANS_COMPTES_TEST=1 python3 "$RACINE/infra/ci/fusionner-domaine.py" "$DOMAINE" "$ECHANGE/gimac-realm.json" "${base[@]}" > "$ECHANGE/import/gimac-realm.json"
  compose run --rm --no-deps --user "$(id -u):0" -v "$ECHANGE/import:/echange" keycloak \
    import --dir /echange --override true
}

# Configurations montées en lecture seule (politiques OPA, simulacres, observabilité) : lues au démarrage
# de leur conteneur, que « compose up » ne recrée pas quand seul le contenu du dossier change. Chaque
# service dont la configuration a changé depuis le déploiement précédent est redémarré, puis attendu sain.
CONFIGURATIONS_MONTEES=(
  "opa:opa/politiques"
  "simulateur-certification:simulacres/simulateur"
  "gestion-changements:simulacres/changements"
  "autorite-horodatage:simulacres/horodatage"
  "grafana:observabilite/grafana/provisioning"
  "otel-collector:observabilite/otel-collector.yaml"
  "tempo:observabilite/tempo.yaml"
)
EMPREINTES_MONTEES="$HOME/.local/state/gimac-des/configurations"

empreinte_montee() {
  (cd "$COMPOSE_DIR" && find "$1" -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum | cut -d' ' -f1)
}

actualiser_configurations_montees() {
  local entree service chemin nouvelle fichier
  local relances=() empreintes=()
  mkdir -p "$EMPREINTES_MONTEES"
  for entree in "${CONFIGURATIONS_MONTEES[@]}"; do
    service="${entree%%:*}"; chemin="${entree#*:}"
    [ -e "$COMPOSE_DIR/$chemin" ] || continue
    compose ps --status running -q "$service" 2>/dev/null | grep -q . || continue
    nouvelle="$(empreinte_montee "$chemin")"
    fichier="$EMPREINTES_MONTEES/$service.sha256"
    [ "$(cat "$fichier" 2>/dev/null)" != "$nouvelle" ] || continue
    relances+=("$service"); empreintes+=("$fichier:$nouvelle")
  done
  [ ${#relances[@]} -gt 0 ] || return 0
  etape "Configurations montées modifiées : redémarrage de ${relances[*]}"
  compose restart "${relances[@]}"
  compose up -d --wait "${relances[@]}"
  for entree in "${empreintes[@]}"; do echo "${entree#*:}" > "${entree%%:*}"; done
}

# Images livrées ou publiées (--images) : toutes présentes dans les listes avant de toucher au serveur, tirées
# du registre si absentes du serveur, puis étiquetées sous les noms des fichiers Compose (gateway, iam… ; frontend-<application>
# depuis frontend-<application>-serveur, construites avec les adresses de publication.env). L'extension
# gimac-totp, montée par le Compose, est extraite de l'image keycloak.
recuperer_images() {
  local listes nom reference manquants=() services
  listes="$(cat "$LISTES_IMAGES"/images-*.txt 2>/dev/null || true)"
  reference_de() { awk -v n="$1" '$1 == n && $2 != "" { print $2; exit }' <<< "$listes"; }
  mapfile -t services < <(sed -n '/^SERVICES=(/,/)/p' "$COMPOSE_DIR/gimac-dev.sh" | tr -d '()' | sed 's/^SERVICES=//' | tr -s ' ' '\n' | grep .)
  [ "${#services[@]}" -eq 14 ] || { echo "ERREUR : liste des microservices illisible dans gimac-dev.sh" >&2; exit 1; }
  for nom in "${services[@]}" frontend-departement-serveur frontend-portail-serveur frontend-console-serveur keycloak; do
    [ -n "$(reference_de "$nom")" ] || manquants+=("$nom")
  done
  [ "${#manquants[@]}" -eq 0 ] || { echo "ERREUR : images absentes des listes : ${manquants[*]}" >&2; exit 1; }
  for nom in "${services[@]}" frontend-departement-serveur frontend-portail-serveur frontend-console-serveur keycloak; do
    reference="$(reference_de "$nom")"
    docker image inspect "$reference" >/dev/null 2>&1 || docker pull -q "$reference" >/dev/null
    case "$nom" in
      keycloak)
        local conteneur cible="$RACINE/infra/keycloak/extensions/gimac-totp/target"
        mkdir -p "$cible"
        conteneur="$(docker create "$reference")"
        docker cp "$conteneur:/opt/keycloak/providers/gimac-totp.jar" "$cible/gimac-totp.jar" >/dev/null
        docker rm "$conteneur" >/dev/null ;;
      *-serveur) docker tag "$reference" "${nom%-serveur}" ;;
      *) docker tag "$reference" "$nom" ;;
    esac
    echo "  $nom : $reference"
  done
}

# Espace disque : Docker vit sur / (70 Go sur la 214), dont la saturation a arrêté le serveur le
# 10/10/2026. Refus de déployer sous le seuil GIMAC_DISQUE_MIN_GO (10 Go par défaut) ; le ménage des
# images et du cache de construction se fait en fin de déploiement, quand chaque image utile a son
# conteneur (au début, une plateforme dont les conteneurs ont disparu verrait tout retiré).
verifier_espace() {
  local libre seuil="${GIMAC_DISQUE_MIN_GO:-10}"
  libre="$(df -P --block-size=1G "$(docker info -f '{{.DockerRootDir}}')" | awk 'NR == 2 { print $4 }')"
  echo "  Espace libre pour Docker : ${libre} Go (seuil ${seuil} Go)"
  if [ "$libre" -lt "$seuil" ]; then
    echo "ERREUR : moins de ${seuil} Go libres pour Docker ; libérer de la place (docker system df) avant de déployer" >&2
    exit 1
  fi
}

etape "Espace disque"
# Journaux de déploiement de plus d'un jour (D-111) ; le journal en cours, récent, est conservé.
find "$HOME" -maxdepth 1 -type f -name '*.log' -mtime +0 -delete 2>/dev/null || true
verifier_espace
configurer_env "${1:-}"
appliquer_publication

if [ -n "$LISTES_IMAGES" ]; then
  etape "Images livrées ou publiées (aucune construction sur le serveur)"
  recuperer_images
  actualiser_domaine
else
  outils
  etape "Images des 14 microservices et extension Keycloak"
  "$COMPOSE_DIR/gimac-dev.sh" images
  actualiser_domaine

  etape "Images des trois couches de présentation"
  # Une image après l'autre : trois constructions Vite simultanées épuisent la mémoire du serveur et font
  # tomber le démon de construction (« failed to receive status … EOF »). Les couches communes (npm ci)
  # sont en cache dès la première image.
  for couche in frontend-departement frontend-portail frontend-console; do
    compose build "$couche"
  done
fi

etape "Démarrage"
"$COMPOSE_DIR/gimac-dev.sh" demarrer
actualiser_configurations_montees
mkdir -p "$(dirname "$EMPREINTE")"
sha256sum "$DOMAINE" | cut -d' ' -f1 > "$EMPREINTE"
cp "$DOMAINE" "$DOMAINE_PRECEDENT"

# Secrets des clients de service : alignés sur le .env (un client ajouté au domaine réimporté gardait un
# secret différent de celui du service, invalid_client_credentials). Idempotent, aucun secret affiché.
etape "Secrets des clients de service"
python3 "$RACINE/infra/keycloak/aligner-secrets-clients.py"

if [ -f "$RACINE/infra/keycloak/local/comptes.json" ]; then
  etape "Comptes nominatifs (les comptes existants sont conservés)"
  python3 "$RACINE/infra/keycloak/creer-comptes-personnels.py"
fi

# Ménage, plateforme démarrée : images sans conteneur et cache de construction de plus de 3 jours.
# Les volumes ne sont jamais touchés ici (bases, Keycloak, stockage).
etape "Ménage des images et du cache de construction"
docker image prune -af --filter until=72h >/dev/null || true
docker builder prune -f --filter until=72h >/dev/null || true
df -h "$(docker info -f '{{.DockerRootDir}}')" | awk 'NR == 2 { print "  Espace libre pour Docker : " $4 }'

hote="$(sed -n 's/^GIMAC_HOTE_PUBLIC=//p' "$ENV")"
etape "Plateforme déployée"
echo "  Application du département : http://$hote:4200"
echo "  Portail des participants    : http://$hote:4201"
echo "  Console d'administration    : http://$hote:4202"
