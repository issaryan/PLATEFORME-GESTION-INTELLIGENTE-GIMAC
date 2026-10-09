#!/usr/bin/env bash
# Paquet de déploiement du serveur de démonstration (10.20.36.214) : tout ce qu'il faut pour démarrer la
# plateforme à partir des images publiées, et rien du code des applications. Le serveur ne reçoit que ce
# paquet et les images (infra/ci/deployer-serveur.sh --images) : ni sources Java ou React, ni pom.xml.
#
#   infra/ci/paquet-serveur.sh <archive.tar.gz>
#
# Contenu, fichiers suivis par git aux chemins du dépôt (les fichiers Compose et les scripts s'y retrouvent
# à l'identique) :
#   infra/docker-compose   fichiers Compose et configurations montées
#   infra/keycloak         domaines, thème, scripts d'amorçage des comptes (sans l'extension gimac-totp,
#                          tirée de l'image keycloak au déploiement, ni les comptes locaux)
#   infra/kafka, infra/serveur, et de infra/ci : deployer-serveur.sh, fusionner-domaine.py
# Contrôle : chaque fichier ou dossier monté par les fichiers Compose existe dans le paquet.
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARCHIVE="${1:?usage : paquet-serveur.sh <archive.tar.gz>}"
case "$ARCHIVE" in /*) ;; *) ARCHIVE="$PWD/$ARCHIVE" ;; esac

TRAVAIL="$(mktemp -d)"
trap 'rm -rf "$TRAVAIL"' EXIT
cd "$RACINE"
# Fichiers suivis par git seulement : ni .env, ni journaux, ni comptes locaux, ni état d'outils du poste.
git ls-files -z -- infra/docker-compose infra/keycloak ':!infra/keycloak/extensions' infra/kafka infra/serveur \
    infra/ci/deployer-serveur.sh infra/ci/fusionner-domaine.py \
  | tar --null -T - -cf - | tar -xf - -C "$TRAVAIL"
git rev-parse HEAD > "$TRAVAIL/VERSION" 2>/dev/null || echo local > "$TRAVAIL/VERSION"

# Montages relatifs des fichiers Compose (« - ./x:/y » ou « - ../x:/y ») ; l'extension gimac-totp est
# déposée au déploiement depuis l'image keycloak.
manquants=0
while read -r source; do
  [ "$source" = ../keycloak/extensions/gimac-totp/target/gimac-totp.jar ] && continue
  if [ ! -e "$TRAVAIL/infra/docker-compose/$source" ]; then
    echo "ERREUR : montage absent du paquet : infra/docker-compose/$source" >&2
    manquants=1
  fi
done < <(sed -nE 's/^[[:space:]]+- (\.\.?\/[^:]+):.*/\1/p' infra/docker-compose/*.yml | sort -u)
[ "$manquants" = 0 ] || exit 1

# Aucun code d'application : ni pom.xml, ni sources Java, TypeScript ou JavaScript.
code="$(cd "$TRAVAIL" && find . \( -name pom.xml -o -name '*.java' -o -name '*.ts' -o -name '*.tsx' -o -name package.json \) -print | head -5)"
[ -z "$code" ] || { echo "ERREUR : code d'application dans le paquet :" >&2; echo "$code" >&2; exit 1; }

tar -czf "$ARCHIVE" -C "$TRAVAIL" .
echo "Paquet du serveur : $ARCHIVE ($(du -h "$ARCHIVE" | cut -f1), version $(cat "$TRAVAIL/VERSION"))"
