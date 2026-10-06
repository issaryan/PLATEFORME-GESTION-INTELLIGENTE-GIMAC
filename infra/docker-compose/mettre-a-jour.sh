#!/usr/bin/env bash
# Mise à jour d'un poste de développement en une commande : tire main, reconstruit ce qui a changé et
# redémarre la plateforme sans perdre les comptes (Keycloak persistant, docker-compose.keycloak-persistant.yml).
#
#   ./mettre-a-jour.sh            tire, reconstruit les images périmées, redémarre
#   ./mettre-a-jour.sh --tout     reconstruit les 14 services et les trois couches de présentation
#   ./mettre-a-jour.sh --sans-pull  sans git pull (code local, par exemple non encore poussé)
#
# Une image de service est périmée quand un commit postérieur à sa construction touche ms-<service>/src/main
# ou son pom.xml ; les couches de présentation, quand un commit touche frontend/. Les modifications non
# commitées ne sont pas détectées : --tout les prend. Même logique que infra/ci/deployer-serveur.sh pour le
# domaine Keycloak et les configurations montées, avec l'état du poste dans ~/.local/state/gimac-des.
set -euo pipefail

ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RACINE="$(cd "$ICI/../.." && pwd)"
SERVICES=(gateway iam audit document notification participant homologation
          certification incident monitoring rapprochement pilotage reporting collaboration)
FRONTENDS=(frontend-departement frontend-portail frontend-console)
ETAT="$HOME/.local/state/gimac-des"
DOMAINE="$RACINE/infra/keycloak/gimac-realm.json"
EMPREINTE_DOMAINE="$ETAT/gimac-realm.sha256"

tout=false; pull=true
for a in "$@"; do
  case "$a" in
    --tout) tout=true ;;
    --sans-pull) pull=false ;;
    *) sed -n '2,10p' "$0"; exit 2 ;;
  esac
done

etape() { echo; echo "== $*"; }
# Même surcouche que gimac-dev.sh (GIMAC_COMPOSE_SUPPLEMENT du .env, chemin relatif à ce dossier).
SUPPLEMENT="${GIMAC_COMPOSE_SUPPLEMENT:-$(sed -n 's/^GIMAC_COMPOSE_SUPPLEMENT=//p' "$ICI/.env" 2>/dev/null | tail -1)}"
case "$SUPPLEMENT" in ""|/*) ;; *) SUPPLEMENT="$ICI/$SUPPLEMENT" ;; esac
compose() { docker compose --project-name gimac-des -f "$ICI/docker-compose.yml" -f "$ICI/docker-compose.gimac.yml" \
  ${SUPPLEMENT:+-f "$SUPPLEMENT"} "$@"; }

# Les fichiers d'échange du domaine contiennent des empreintes de mots de passe : supprimés en sortie.
ECHANGE=""
trap 'rm -rf "${ECHANGE:-/nonexistent}"' EXIT

perimee() { # image, chemins… : vrai si l'image manque ou si un commit postérieur touche ces chemins
  local cree
  cree="$(docker image inspect -f '{{.Created}}' "$1" 2>/dev/null)" || return 0
  shift
  [ -n "$(git -C "$RACINE" log -1 --since="$cree" --format=%H -- "$@")" ]
}

if $pull; then
  etape "Mise à jour du dépôt"
  git -C "$RACINE" pull --ff-only
fi
git -C "$RACINE" log --oneline -1

etape "Images"
"$ICI/gimac-dev.sh" verifier >/dev/null
# Secrets ajoutés par les commits tirés (nouveaux clients de service…) : le Compose refuse d'interpoler
# un secret absent, y compris pour construire les couches de présentation.
"$ICI/gimac-dev.sh" preparer
# Extension gimac-totp de Keycloak (D-65), montée par le Compose : quelques secondes, toujours reconstruite.
(cd "$RACINE/infra/keycloak/extensions/gimac-totp" && mvn -q -ntp -B package -DskipTests)
chmod -R a+rX "$RACINE/infra/keycloak/extensions/gimac-totp/target" 2>/dev/null || true
construits=()
for s in "${SERVICES[@]}"; do
  if $tout || perimee "$s:latest" "ms-$s/src/main" "ms-$s/pom.xml"; then
    echo "-- $s (Jib)"
    # Tests exclus : le seuil JaCoCo de verify (D-61) n'aurait aucune mesure ; ils s'exécutent à part.
    (cd "$RACINE/ms-$s" && ./mvnw -B -ntp -q -Pprod verify jib:dockerBuild -DskipTests -Djacoco.skip=true)
    construits+=("$s")
  fi
done
if $tout || perimee "${FRONTENDS[0]}" frontend; then
  echo "-- couches de présentation"
  compose build "${FRONTENDS[@]}"
  construits+=(frontends)
fi
echo "Reconstruit : ${construits[*]:-rien}"

# Keycloak persistant : --import-realm ignore un domaine existant. Quand gimac-realm.json change, le domaine
# en service est exporté avec ses comptes (mots de passe hachés et codes TOTP compris), fusionné avec la
# version du dépôt (infra/ci/fusionner-domaine.py), puis réimporté.
actualiser_domaine() {
  local nouvelle
  nouvelle="$(sha256sum "$DOMAINE" | cut -d' ' -f1)"
  docker volume inspect gimac-des_keycloak_donnees >/dev/null 2>&1 || return 0   # non persistant ou premier import
  [ "$(cat "$EMPREINTE_DOMAINE" 2>/dev/null)" != "$nouvelle" ] || return 0
  etape "Domaine gimac modifié : réimport en conservant les comptes"
  ECHANGE="$(mktemp -d)"
  mkdir -p "$ECHANGE/import"
  compose stop keycloak
  compose up -d --wait keycloak-postgresql
  if [ -n "$(compose exec -T keycloak-postgresql psql -U keycloak -d keycloak -tAc \
      "select 1 from realm where name = 'gimac'" 2>/dev/null)" ]; then
    compose run --rm --no-deps --user "$(id -u):0" -v "$ECHANGE:/echange" keycloak \
      export --dir /echange --realm gimac --users realm_file
    python3 "$RACINE/infra/ci/fusionner-domaine.py" "$DOMAINE" "$ECHANGE/gimac-realm.json" > "$ECHANGE/import/gimac-realm.json"
    compose run --rm --no-deps --user "$(id -u):0" -v "$ECHANGE/import:/echange" keycloak \
      import --dir /echange --override true
  fi
}
actualiser_domaine

etape "Démarrage"
"$ICI/gimac-dev.sh" demarrer >/dev/null
mkdir -p "$ETAT"
sha256sum "$DOMAINE" | cut -d' ' -f1 > "$EMPREINTE_DOMAINE"

# Configurations montées en lecture seule, lues au démarrage de leur conteneur, que « compose up » ne recrée
# pas quand seul le contenu du dossier change : redémarrage des services concernés.
CONFIGURATIONS_MONTEES=(
  "opa:opa/politiques"
  "simulateur-certification:simulacres/simulateur"
  "gestion-changements:simulacres/changements"
  "autorite-horodatage:simulacres/horodatage"
  "grafana:observabilite/grafana/provisioning"
  "otel-collector:observabilite/otel-collector.yaml"
  "tempo:observabilite/tempo.yaml"
)
relances=(); empreintes=()
mkdir -p "$ETAT/configurations"
for entree in "${CONFIGURATIONS_MONTEES[@]}"; do
  service="${entree%%:*}"; chemin="${entree#*:}"
  [ -e "$ICI/$chemin" ] || continue
  nouvelle="$(cd "$ICI" && find "$chemin" -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum | cut -d' ' -f1)"
  fichier="$ETAT/configurations/$service.sha256"
  [ "$(cat "$fichier" 2>/dev/null)" != "$nouvelle" ] || continue
  relances+=("$service"); empreintes+=("$fichier:$nouvelle")
done
if [ ${#relances[@]} -gt 0 ]; then
  etape "Configurations montées modifiées : redémarrage de ${relances[*]}"
  compose restart "${relances[@]}"
  compose up -d --wait "${relances[@]}"
  for e in "${empreintes[@]}"; do echo "${e#*:}" > "${e%%:*}"; done
fi

etape "État"
malsains="$(docker ps --format '{{.Names}} {{.Status}}' | grep gimac-des | grep -E 'unhealthy|starting' || true)"
for couche in DEPARTEMENT:4200 PORTAIL:4201 CONSOLE:4202; do
  port="$(sed -n "s/^GIMAC_PORT_FRONTEND_${couche%%:*}=//p" "$ICI/.env" | tail -1)"
  echo "http://localhost:${port:-${couche#*:}} -> $(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${port:-${couche#*:}}/")"
done
if [ -n "$malsains" ]; then echo "Conteneurs non sains :"; echo "$malsains"; exit 1; fi
echo "Plateforme à jour et saine."
