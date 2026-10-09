#!/usr/bin/env bash
# Livraison du serveur de démonstration (10.20.36.214) depuis un poste, sans code source sur le serveur
# ni GitHub Actions : images construites sur le poste, envoyées par SSH avec le paquet de déploiement
# (infra/ci/paquet-serveur.sh), puis démarrées par deployer-serveur.sh --images sur le serveur.
# Voir docs/exploitation/livraison-serveur.md.
#
#   infra/ci/livrer-serveur.sh [--tests] [--serveur utilisateur@hote]
#
#   --tests    valide d'abord le backend et le frontend (infra/ci/valider-backend.sh, valider-frontend.sh) ;
#              la livraison n'a lieu que si tout passe
#   --serveur  cible SSH (défaut : GIMAC_SERVEUR, sinon 10.20.36.214) ; accès par clé, sans mot de passe
#
# Livre le commit courant (HEAD) ; refuse une copie de travail modifiée, sauf GIMAC_LIVRER_MODIFIE=1.
# Seules les images absentes du serveur traversent le réseau (comparaison par identifiant d'image).
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVEUR="${GIMAC_SERVEUR:-10.20.36.214}"
TESTS=0
while [ $# -gt 0 ]; do
  case "$1" in
    --tests) TESTS=1 ;;
    --serveur) SERVEUR="${2:?--serveur utilisateur@hote}"; shift ;;
    *) sed -n '2,15p' "$0"; exit 2 ;;
  esac
  shift
done

etape() { echo; echo "== $*"; }
ssh_serveur() { ssh -o BatchMode=yes -o ConnectTimeout=15 "$SERVEUR" "$@"; }
cd "$RACINE"

etape "Prérequis"
ssh_serveur 'docker info >/dev/null' || { echo "ERREUR : $SERVEUR injoignable par SSH ou Docker inaccessible" >&2; exit 1; }
if [ -n "$(git status --porcelain --untracked-files=no)" ] && [ "${GIMAC_LIVRER_MODIFIE:-0}" != 1 ]; then
  echo "ERREUR : copie de travail modifiée ; commiter d'abord (ou GIMAC_LIVRER_MODIFIE=1)" >&2; exit 1
fi
VERSION="$(git rev-parse --short=12 HEAD)"
echo "Commit livré : $(git log -1 --format='%h %s')"
echo "Serveur      : $SERVEUR"

if [ "$TESTS" = 1 ]; then
  etape "Validation du backend et du frontend"
  infra/ci/valider-backend.sh
  infra/ci/valider-frontend.sh
fi

etape "Construction des images sur le poste"
# Préfixe propre aux livraisons : les images du développement local (gateway, frontend-…) ne sont pas touchées.
export GIMAC_REGISTRE=gimac-livraison GIMAC_ETIQUETTE="$VERSION"
mapfile -t services < <(source infra/ci/commun.sh && lister_services)
infra/ci/construire-images.sh "${services[@]}" keycloak frontend-serveur

TRAVAIL="$(mktemp -d)"
trap 'rm -rf "$TRAVAIL"' EXIT
LISTE="$TRAVAIL/images-livraison.txt"
for s in "${services[@]}"; do echo "${s#ms-} $GIMAC_REGISTRE/${s#ms-}:$VERSION"; done > "$LISTE"
for nom in keycloak frontend-departement-serveur frontend-portail-serveur frontend-console-serveur; do
  echo "$nom $GIMAC_REGISTRE/$nom:$VERSION" >> "$LISTE"
done
while read -r nom reference; do
  docker image inspect "$reference" >/dev/null 2>&1 || { echo "ERREUR : image $reference non construite ($nom)" >&2; exit 1; }
done < "$LISTE"

etape "Paquet de déploiement"
infra/ci/paquet-serveur.sh "$TRAVAIL/paquet-serveur.tar.gz"

etape "Envoi des images absentes du serveur"
presentes="$(ssh_serveur "docker images -q --no-trunc | sort -u")"
a_envoyer=()
while read -r nom reference; do
  identifiant="$(docker image inspect -f '{{.Id}}' "$reference")"
  if grep -qxF "$identifiant" <<< "$presentes"; then
    echo "  $nom : déjà sur le serveur"
  else
    a_envoyer+=("$reference"); echo "  $nom : à envoyer"
  fi
done < "$LISTE"
if [ "${#a_envoyer[@]}" -gt 0 ]; then
  docker save "${a_envoyer[@]}" | gzip -1 | ssh_serveur 'gunzip | docker load -q'
fi
# Image déjà présente sous une autre étiquette (service inchangé, construction Jib reproductible) : étiquetée.
while read -r nom reference; do
  echo "docker tag $(docker image inspect -f '{{.Id}}' "$reference") $reference"
done < "$LISTE" | ssh_serveur bash -s

etape "Installation du paquet et démarrage sur le serveur"
DISTANT="/tmp/gimac-livraison-$VERSION"
ssh_serveur "rm -rf '$DISTANT' && mkdir -p '$DISTANT/images' '$DISTANT/extrait'"
scp -q -o BatchMode=yes "$TRAVAIL/paquet-serveur.tar.gz" "$SERVEUR:$DISTANT/"
scp -q -o BatchMode=yes "$LISTE" "$SERVEUR:$DISTANT/images/"
# shellcheck disable=SC2087  # variables développées sur le poste à dessein (chemins de cette livraison)
ssh_serveur bash -s <<DISTANT
set -euo pipefail
tar -xzf '$DISTANT/paquet-serveur.tar.gz' -C '$DISTANT/extrait'
mkdir -p "\$HOME/gimac-des"
# L'état propre au serveur n'est pas dans le paquet : .env et comptes (liens vers ~/.config/gimac-des),
# journaux d'échec, extension gimac-totp extraite de l'image keycloak.
rsync -a --delete \
  --exclude /infra/docker-compose/.env --exclude /infra/docker-compose/.journaux-echec/ \
  --exclude /infra/keycloak/local/ --exclude /infra/keycloak/extensions/ \
  '$DISTANT/extrait/' "\$HOME/gimac-des/"
"\$HOME/gimac-des/infra/ci/deployer-serveur.sh" --images '$DISTANT/images'
# Images des livraisons précédentes : retirées si plus aucun conteneur ne les utilise.
docker images --format '{{.Repository}}:{{.Tag}}' | grep '^gimac-livraison/' | grep -v ':$VERSION\$' \
  | xargs -r docker rmi >/dev/null 2>&1 || true
rm -rf '$DISTANT'
DISTANT

etape "Contrôle"
# shellcheck disable=SC2016  # commandes évaluées sur le serveur
ssh_serveur 'for p in 4200 4201 4202; do printf "  http://127.0.0.1:%s -> %s\n" $p "$(curl -s -o /dev/null -w "%{http_code}" --max-time 15 http://127.0.0.1:$p/)"; done
malsains="$(docker ps --filter label=com.docker.compose.project=gimac-des --filter health=unhealthy --format "{{.Names}}")"
[ -z "$malsains" ] || { echo "Conteneurs malsains : $malsains"; exit 1; }
echo "  Conteneurs sains"'
echo
echo "Livraison $VERSION terminée sur $SERVEUR."
