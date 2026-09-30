#!/usr/bin/env bash
# Déploiement continu en recette (D-45, D-48) : applique infra/kubernetes-gimac en remplaçant
# les images registre.gimac.local/des/* par les images publiées et signées par la CI,
# référencées par empreinte (jamais par étiquette mobile).
#
#   infra/ci/deployer-recette.sh <liste-images> [--simulation]
#
# <liste-images> : lignes « nom image@sha256:… » produites par construire-images.sh
#                  (GIMAC_LISTE_IMAGES) ; un nom absent garde l'image des déclarations.
# --simulation   : rendu et validation côté serveur (kubectl apply --dry-run=server) sans
#                  modification ; sans accès à une grappe, seul le rendu est produit.
#
# Accès à la grappe : KUBECONFIG (fichier) ; les secrets sont créés au préalable par
# infra/kubernetes-gimac/creer-secrets.sh (aucun secret dans le dépôt, BNF-07).
set -euo pipefail

# shellcheck source=infra/ci/commun.sh
source "$(dirname "${BASH_SOURCE[0]}")/commun.sh"
cd "$RACINE_DEPOT"

liste="${1:?usage : deployer-recette.sh <liste-images> [--simulation]}"
simulation=0
[ "${2:-}" = "--simulation" ] && simulation=1
REGISTRE_DECLARATIONS="${GIMAC_REGISTRE_DECLARATIONS:-registre.gimac.local/des}"

# Surcouche Kustomize temporaire : la base reste intacte, seules les images changent.
surcouche="$DOSSIER_TRAVAIL/recette"
mkdir -p "$surcouche"
base="$(python3 -c 'import os,sys; print(os.path.relpath(sys.argv[1], sys.argv[2]))' "$RACINE_DEPOT/infra/kubernetes-gimac" "$surcouche")"
{
  echo "apiVersion: kustomize.config.k8s.io/v1beta1"
  echo "kind: Kustomization"
  echo "resources:"
  echo "  - $base"
  echo "images:"
  while read -r nom reference; do
    [ -n "$nom" ] || continue
    case "$reference" in
      *@sha256:*) ;;
      *) echo "Référence sans empreinte ignorée : $nom $reference" >&2; continue ;;
    esac
    echo "  - name: $REGISTRE_DECLARATIONS/$nom"
    echo "    newName: ${reference%@*}"
    echo "    digest: ${reference#*@}"
  done < "$liste"
} > "$surcouche/kustomization.yaml"

section "Rendu de la surcouche de recette"
cat "$surcouche/kustomization.yaml"
rendu="$DOSSIER_TRAVAIL/recette.yaml"
if outil_present kustomize; then rendre=(kustomize build); else rendre=(kubectl kustomize); fi
if ! essayer "rendu Kustomize de la surcouche de recette" "${rendre[@]}" "$surcouche" -o "$rendu"; then
  fin_section; bilan "Déploiement en recette"; exit 1
fi
info "Images déployées :"
sed -nE 's/^[[:space:]]+image: *//p' "$rendu" | sort -u | sed 's/^/    /'
fin_section

section "Application à la grappe"
if [ "$simulation" = 1 ]; then
  if kubectl version --request-timeout=10s >/dev/null 2>&1; then
    controler "kubectl apply -k --dry-run=server" kubectl apply --dry-run=server -k "$surcouche"
  else
    ignore "grappe injoignable : simulation limitée au rendu"
  fi
else
  controler "kubectl apply -k (recette)" kubectl apply -k "$surcouche"
  # Attente du déploiement de chaque service (échec si un pod ne devient pas prêt).
  while read -r deploiement; do
    controler "déploiement prêt : $deploiement" \
      kubectl -n gimac-des rollout status "$deploiement" --timeout=10m
  done < <(kubectl -n gimac-des get deployments -o name)
fi
fin_section

bilan "Déploiement en recette"
