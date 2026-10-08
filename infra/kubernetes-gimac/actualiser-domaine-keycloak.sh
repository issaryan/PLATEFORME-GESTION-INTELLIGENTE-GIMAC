#!/usr/bin/env bash
# Mise à jour du domaine Keycloak « gimac » dans la grappe en conservant l'administration faite dans la
# plateforme : comptes (mots de passe hachés, codes TOTP), profils créés, recompositions, politique de mot de
# passe, OTP, sessions et force brute modifiées dans la console (fusion à trois voies, infra/ci/fusionner-domaine.py).
# Équivalent de actualiser_domaine de infra/ci/deployer-serveur.sh et de infra/docker-compose/mettre-a-jour.sh.
#
#   ./actualiser-domaine-keycloak.sh     après « kubectl apply -k . » d'une image Keycloak dont le domaine a changé
#
# « start --import-realm » ignore un domaine existant : sans ce script, une nouvelle version de gimac-realm.json
# n'est jamais appliquée. Keycloak est arrêté le temps du Job (aucune écriture concurrente, caches vidés au
# redémarrage), puis remis au nombre de répliques d'avant. Domaine de l'image identique à celui importé la fois
# précédente : le Job ne fait rien. Les fichiers d'échange (empreintes de mots de passe) restent dans un volume
# en mémoire du pod du Job, supprimé en fin de script ; rien n'est copié sur le poste.
set -euo pipefail

ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NS=gimac-des
JOB=keycloak-domaine
DELAI="${GIMAC_DELAI_DOMAINE:-20m}"

k() { kubectl -n "$NS" "$@"; }

image="$(k get statefulset keycloak -o jsonpath='{.spec.template.spec.containers[0].image}')"
repliques="$(k get statefulset keycloak -o jsonpath='{.spec.replicas}')"
k get pvc keycloak-domaine-etat >/dev/null
k get configmap keycloak-fusion-domaine >/dev/null
echo "Image Keycloak : $image ; $repliques répliques"

relancer() {
  echo "Keycloak remis à $repliques répliques"
  k scale statefulset keycloak --replicas="$repliques"
  k rollout status statefulset keycloak --timeout="$DELAI"
}

echo "Arrêt de Keycloak"
k scale statefulset keycloak --replicas=0
k wait --for=delete pod -l app=keycloak --timeout="$DELAI" 2>/dev/null || true
trap 'k delete job "$JOB" --ignore-not-found --wait=false >/dev/null; relancer' EXIT

k delete job "$JOB" --ignore-not-found --wait=true
kubectl set image --local -f "$ICI/keycloak-domaine-job.yaml" -o yaml \
  exporter="$image" importer="$image" | k apply -f -

if k wait --for=condition=complete "job/$JOB" --timeout="$DELAI"; then
  k logs "job/$JOB" --all-containers --prefix
  echo "Domaine gimac à jour"
else
  # Aucun import n'a lieu si l'export ou la fusion échoue : le domaine en service est intact.
  k logs "job/$JOB" --all-containers --prefix || true
  echo "ERREUR : mise à jour du domaine gimac en échec (journaux ci-dessus)" >&2
  exit 1
fi
