#!/usr/bin/env bash
# Environnement local de la plateforme Moabi du GIMAC en une commande (SOC-13, D-45).
#
#   ./gimac-dev.sh verifier                 contrôle les prérequis
#   ./gimac-dev.sh preparer                 complète le .env des secrets locaux manquants
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
# GIMAC_COMPOSE_SUPPLEMENT : surcouche facultative d'un hôte (ex. docker-compose.serveur.yml), prise de
# l'environnement ou, à défaut, du .env du poste (ex. docker-compose.keycloak-persistant.yml) ; un chemin
# relatif s'entend depuis ce dossier.
if [ -z "${GIMAC_COMPOSE_SUPPLEMENT:-}" ] && [ -f "$ICI/.env" ]; then
  GIMAC_COMPOSE_SUPPLEMENT="$(sed -n 's/^GIMAC_COMPOSE_SUPPLEMENT=//p' "$ICI/.env" | tail -1)"
fi
case "${GIMAC_COMPOSE_SUPPLEMENT:-}" in ""|/*) ;; *) GIMAC_COMPOSE_SUPPLEMENT="$ICI/$GIMAC_COMPOSE_SUPPLEMENT" ;; esac
compose() { docker compose --project-name gimac-des -f "$ICI/docker-compose.yml" -f "$ICI/docker-compose.gimac.yml" \
  ${GIMAC_COMPOSE_SUPPLEMENT:+-f "$GIMAC_COMPOSE_SUPPLEMENT"} "$@"; }
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

# Ports de l'hôte publiés par les services visés ; un port pris est signalé
# avec la variable de .env qui permet de le déplacer.
verifier_ports() {
  local occupes actifs
  # Les ports tenus par les conteneurs déjà démarrés de ce projet ne sont pas des conflits.
  actifs=$(compose ps --services --status running 2>/dev/null | tr '\n' ' ')
  occupes=$(compose config --format json "$@" | GIMAC_ACTIFS="$actifs" python3 -c '
import json, os, subprocess, sys
config = json.load(sys.stdin)
actifs = set(os.environ.get("GIMAC_ACTIFS", "").split())
cibles = sys.argv[1:] or list(config["services"])
for nom in cibles:
    if nom in actifs:
        continue
    for port in config["services"][nom].get("ports", []):
        hote = port.get("published")
        if hote and subprocess.run(["ss", "-ltnH", f"sport = :{hote}"], capture_output=True, text=True).stdout.strip():
            print(f"  {hote} (service {nom})")
' "$@")
  [ -z "$occupes" ] || echec "ports déjà utilisés sur ce poste :
$occupes
Choisissez d'autres ports dans .env (variables GIMAC_PORT_*, voir docker-compose.gimac.yml)."
}

# Secrets locaux générés aléatoirement, jamais versionnés (BNF-07, D-44).
# Une variable absente d'un .env existant y est ajoutée ; les valeurs présentes sont conservées.
preparer_env() {
  local fichier="$ICI/.env" variable
  umask 077
  [ -f "$fichier" ] || echo "# Secrets locaux de développement, générés par gimac-dev.sh. Ne pas versionner." > "$fichier"
  for variable in GIMAC_KEYCLOAK_ADMIN_PASSWORD GIMAC_GRAFANA_ADMIN_PASSWORD GIMAC_OPENBAO_TOKEN \
                  GIMAC_S3_CLE_ACCES GIMAC_S3_CLE_SECRETE GIMAC_MOT_DE_PASSE_TEST GIMAC_SECRET_TESTER_CLI \
                  GIMAC_SECRET_HOMOLOGATION_SERVICE GIMAC_SECRET_CERTIFICATION_SERVICE GIMAC_SECRET_PARTICIPANT_SERVICE \
                  GIMAC_SECRET_IAM_SERVICE GIMAC_SECRET_GATEWAY_SUPERVISION GIMAC_SECRET_INTERNAL GIMAC_KEYCLOAK_DB_PASSWORD \
                  GIMAC_SECRET_COLLABORATION_SERVICE GIMAC_SECRET_CONNEXION GIMAC_SECRET_WEB_APP \
                  GIMAC_SECRET_GATEWAY_SERVICE GIMAC_SECRET_PILOTAGE_SERVICE GIMAC_SECRET_NOTIFICATION_SERVICE \
                  GIMAC_SECRET_REPRISE_SERVICE; do
    grep -q "^$variable=" "$fichier" || { echo "$variable=$(openssl rand -hex 20)" >> "$fichier"; echo "Secret ajouté à .env : $variable"; }
  done
  # Clé AES-256-GCM du chiffrement au repos de la messagerie (D-64) : 32 octets en base 64.
  grep -q "^GIMAC_MESSAGERIE_CLE=" "$fichier" \
    || { echo "GIMAC_MESSAGERIE_CLE=$(openssl rand -base64 32)" >> "$fichier"; echo "Secret ajouté à .env : GIMAC_MESSAGERIE_CLE"; }
  # Clé AES-256-GCM des secrets de MS-IAM (enrôlement TOTP, mot de passe SMTP, D-65) : 32 octets en base 64.
  grep -q "^GIMAC_IAM_CLE_CHIFFREMENT=" "$fichier" \
    || { echo "GIMAC_IAM_CLE_CHIFFREMENT=$(openssl rand -base64 32)" >> "$fichier"; echo "Secret ajouté à .env : GIMAC_IAM_CLE_CHIFFREMENT"; }
}

# Avant l'arrêt sur échec, conserve les journaux des conteneurs en mauvaise santé ou arrêtés.
conserver_journaux() {
  local dossier nom
  dossier="$ICI/.journaux-echec/$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$dossier"
  for nom in $(docker ps -a --filter label=com.docker.compose.project=gimac-des --filter health=unhealthy --format '{{.Names}}') \
             $(docker ps -a --filter label=com.docker.compose.project=gimac-des --filter status=exited --format '{{.Names}}'); do
    docker logs --tail 300 "$nom" > "$dossier/$nom.log" 2>&1
  done
  echo "Journaux conservés dans $dossier" >&2
  # Exécuteur GitHub Actions (déploiement du serveur de démonstration) : fin des journaux dans la sortie du
  # travail, pour un diagnostic sans accès au serveur ; valeurs de type secret masquées.
  if [ "${GITHUB_ACTIONS:-}" = true ]; then
    for nom in "$dossier"/*.log; do
      [ -e "$nom" ] || continue
      echo "::group::$(basename "$nom" .log) (60 dernières lignes)"
      tail -n 60 "$nom" | sed -E 's/((password|passwd|secret|token|credential)[A-Za-z_]*[=:][[:space:]]*)[^[:space:],}]+/\1******/Ig'
      echo "::endgroup::"
    done
  fi
}

images_manquantes() {
  local s; for s in "${SERVICES[@]}"; do
    docker image inspect "$s" >/dev/null 2>&1 || echo "$s"
  done
}

# Keycloak tourne sous son propre utilisateur (uid 1000) : l'extension montée doit être lisible par tous,
# même construite sous un masque restrictif (exécuteur du serveur de démonstration, umask 077).
# Sinon : « Failed to read …/providers/gimac-totp.jar » au démarrage.
lisible_par_keycloak() {
  local cible="$RACINE/infra/keycloak/extensions/gimac-totp/target"
  # Domaines importés et thème, montés depuis la copie de travail.
  chmod a+r "$RACINE/infra/keycloak/gimac-realm.json" "$ICI/realm-config/jhipster-realm.json" "$ICI/realm-config/keycloak-health-check.sh"
  chmod -R a+rX "$RACINE/infra/keycloak/themes/gimac"
  [ -e "$cible/gimac-totp.jar" ] || return 0
  chmod a+rx "$cible" && chmod a+r "$cible/gimac-totp.jar"
}

images() {
  # Extension gimac-totp de Keycloak (D-65), montée par le Compose dans /opt/keycloak/providers.
  echo "== Construction de l'extension Keycloak gimac-totp"
  (cd "$RACINE/infra/keycloak/extensions/gimac-totp" && mvn -q -ntp -B package -DskipTests)
  lisible_par_keycloak
  local s; for s in "${SERVICES[@]}"; do
    echo "== Construction de l'image $s (Jib)"
    # Tests exclus : le seuil JaCoCo de verify (D-61) n'aurait aucune mesure et échouerait ;
    # les tests et leurs seuils s'exécutent à part (./mvnw verify).
    (cd "$RACINE/ms-$s" && ./mvnw -ntp -Pprod verify jib:dockerBuild -DskipTests -Djacoco.skip=true)
  done
  # Sans reconstruction explicite, le Compose garde des couches de présentation périmées.
  # Le Compose n'interpole qu'avec les secrets du .env : générés ici sur un poste neuf.
  echo "== Construction des images des couches de présentation"
  preparer_env
  compose build frontend-departement frontend-portail frontend-console
}

demarrer() {
  verifier
  lisible_par_keycloak
  preparer_env
  local cibles=()
  if [ "${1:-}" = "--socle" ]; then
    mapfile -t cibles < <(compose config --services | grep -vxF -f <(printf '%s\n' "${SERVICES[@]}"))
  else
    local manquantes; manquantes=$(images_manquantes)
    [ -z "$manquantes" ] || echec "images absentes : $(echo $manquantes). Lancez « ./gimac-dev.sh images » ou « demarrer --socle »."
  fi
  verifier_ports "${cibles[@]}"
  # Aucun composant partiellement démarré n'est laissé en place en cas d'échec.
  # Services durables : attente de leur santé. Les conteneurs d'initialisation, qui
  # se terminent volontairement, sont lancés ensuite et jugés sur leur code de sortie.
  [ ${#cibles[@]} -gt 0 ] || mapfile -t cibles < <(compose config --services)
  local durables=() init
  for s in "${cibles[@]}"; do [[ "$s" == *-init ]] || durables+=("$s"); done
  if ! compose up -d --wait "${durables[@]}"; then
    echo "Échec du démarrage : arrêt des conteneurs lancés." >&2
    conserver_journaux
    compose down
    exit 1
  fi
  for init in $(compose config --services | grep -- '-init$'); do
    compose up -d "$init" >/dev/null
    if [ "$(docker wait "$(compose ps -a -q "$init")")" != 0 ]; then
      echo "Échec de l'initialisation $init : arrêt des conteneurs lancés." >&2
      compose down
      exit 1
    fi
  done
  compose ps
}

case "${1:-}" in
  verifier) verifier ;;
  preparer) preparer_env ;;
  images) images ;;
  demarrer) shift; demarrer "$@" ;;
  arreter) compose stop ;;
  nettoyer) compose down --volumes --remove-orphans ;;
  etat) compose ps ;;
  *) sed -n '2,10p' "$0"; exit 2 ;;
esac
