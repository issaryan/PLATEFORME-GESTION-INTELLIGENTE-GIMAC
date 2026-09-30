#!/usr/bin/env bash
# Construction, et sur demande publication et signature, des images de la plateforme
# (D-45, D-46, SOC-02) : 14 microservices (Jib), 3 couches de présentation (frontend/Dockerfile)
# et Keycloak à l'image GIMAC (infra/keycloak/Dockerfile).
#
#   infra/ci/construire-images.sh                       # tout construire dans le Docker local
#   infra/ci/construire-images.sh frontend keycloak     # cibles choisies
#   infra/ci/construire-images.sh ms-iam ms-gateway
#   infra/ci/construire-images.sh --pousser ms-iam      # publier dans le registre, puis signer
#
# Cibles : frontend (departement, portail, console), keycloak, ms-<service>.
#
# Variables :
#   GIMAC_REGISTRE       préfixe des images (défaut : registre.gimac.local/des, celui des
#                        déclarations Kubernetes) ; en CI : ghcr.io/<propriétaire>/gimac-des
#   GIMAC_ETIQUETTE      étiquette (défaut : empreinte courte du commit)
#   GIMAC_VERSION_FRONTEND  version affichée par les couches de présentation (défaut du Dockerfile)
#   GIMAC_LISTE_IMAGES   fichier où ajouter « nom image@empreinte » des images publiées
#   COSIGN_PRIVATE_KEY / COSIGN_PASSWORD
#                        signature par clé (celle que vérifie la politique Kyverno
#                        kyverno-images-signees.yaml, clé publique dans kyverno/cle-signature-images)
#   GIMAC_SIGNATURE_SANS_CLE=1
#                        signature supplémentaire sans clé (Sigstore, identité OIDC de la CI)
set -euo pipefail

# shellcheck source=infra/ci/commun.sh
source "$(dirname "${BASH_SOURCE[0]}")/commun.sh"
cd "$RACINE_DEPOT"

pousser=0
cibles=()
for argument in "$@"; do
  case "$argument" in
    --pousser) pousser=1 ;;
    frontend|keycloak|ms-*) cibles+=("${argument%/}") ;;
    *) echo "Cible inconnue : $argument (frontend, keycloak, ms-<service>)" >&2; exit 2 ;;
  esac
done
if [ "${#cibles[@]}" -eq 0 ]; then
  cibles=(frontend keycloak)
  mapfile -t -O 2 cibles < <(lister_services)
fi

REGISTRE="${GIMAC_REGISTRE:-registre.gimac.local/des}"
ETIQUETTE="${GIMAC_ETIQUETTE:-$(git rev-parse --short=12 HEAD 2>/dev/null || echo local)}"

if ! outil_present docker; then
  echec "Docker introuvable"; bilan "Images"; exit 1
fi

# Signe l'image publiée (référence par empreinte, jamais par étiquette) et l'inscrit dans la liste.
signer_et_lister() {
  local nom="$1" reference="$2"
  if [ -n "${GIMAC_LISTE_IMAGES:-}" ]; then echo "$nom $reference" >> "$GIMAC_LISTE_IMAGES"; fi
  if [ -n "${COSIGN_PRIVATE_KEY:-}" ]; then
    controler "$nom : signature cosign par clé" cosign sign --yes --key env://COSIGN_PRIVATE_KEY "$reference"
  fi
  if [ "${GIMAC_SIGNATURE_SANS_CLE:-0}" = 1 ]; then
    controler "$nom : signature cosign sans clé (Sigstore)" cosign sign --yes "$reference"
  fi
  if [ -z "${COSIGN_PRIVATE_KEY:-}" ] && [ "${GIMAC_SIGNATURE_SANS_CLE:-0}" != 1 ]; then
    ignore "$nom : image publiée non signée (ni COSIGN_PRIVATE_KEY ni GIMAC_SIGNATURE_SANS_CLE=1)"
  fi
}

# Construit une image par Dockerfile ; la publie et la signe avec --pousser.
image_docker() {
  local nom="$1" contexte="$2"; shift 2
  local image="$REGISTRE/$nom:$ETIQUETTE"
  if ! essayer "$nom : docker build ($image)" \
       docker build --pull --label "org.opencontainers.image.revision=$(git rev-parse HEAD 2>/dev/null || echo local)" \
         -t "$image" "$@" "$contexte"; then
    return 0
  fi
  if [ "$pousser" = 1 ]; then
    if essayer "$nom : docker push" docker push "$image"; then
      local reference
      reference="$(docker inspect --format '{{index .RepoDigests 0}}' "$image")"
      signer_et_lister "$nom" "$reference"
    fi
  fi
}

# Construit l'image d'un microservice par Jib (pom.xml) ; avec --pousser, Jib la publie
# directement dans le registre (identifiants de « docker login ») sans démon Docker.
image_service() {
  local service="$1" nom="${1#ms-}"
  local image="$REGISTRE/$nom:$ETIQUETTE"
  local but=jib:dockerBuild
  [ "$pousser" = 1 ] && but=jib:build
  if essayer "$nom : Jib $but ($image)" \
       "$service/mvnw" -f "$service/pom.xml" -ntp -B -Pprod -DskipTests package "$but" -Djib.to.image="$image"; then
    if [ "$pousser" = 1 ]; then
      signer_et_lister "$nom" "$REGISTRE/$nom@$(cat "$service/target/jib-image.digest")"
    fi
  fi
}

for cible in "${cibles[@]}"; do
  section "Image(s) : $cible"
  case "$cible" in
    frontend)
      for application in departement portail console; do
        arguments=(--build-arg "APPLICATION=$application")
        # Version affichée par l'application (défaut du Dockerfile sinon).
        [ -z "${GIMAC_VERSION_FRONTEND:-}" ] || arguments+=(--build-arg "VERSION=$GIMAC_VERSION_FRONTEND")
        image_docker "frontend-$application" frontend "${arguments[@]}"
      done ;;
    keycloak)
      image_docker keycloak infra/keycloak ;;
    ms-*)
      image_service "$cible" ;;
  esac
  fin_section
done

bilan "Images"
