#!/bin/sh
# Compartiments WORM de développement (SOC-10, RG-04, RG-16, D-11, D-50).
# Verrouillage d'objets activé à la création (il ne peut pas l'être après).
# En DEV la rétention par défaut est en mode GOVERNANCE pour 1 jour, afin que le
# poste reste nettoyable. INT, QUAL et PROD utilisent COMPLIANCE pour 10 ans.
set -eu
s3() { aws --endpoint-url http://stockage-objet:7480 s3api "$@"; }
for compartiment in gimac-preuves gimac-documents gimac-archives gimac-rapports; do
  if ! s3 head-bucket --bucket "$compartiment" >/dev/null 2>&1; then
    s3 create-bucket --bucket "$compartiment" --object-lock-enabled-for-bucket >/dev/null
  fi
  s3 put-object-lock-configuration --bucket "$compartiment" --object-lock-configuration \
    '{"ObjectLockEnabled":"Enabled","Rule":{"DefaultRetention":{"Mode":"GOVERNANCE","Days":1}}}'
done
echo "Compartiments WORM prêts."

# Dépôts fragmentés en cours (D-74, docs/lot1/depots-fragmentes.md §2) : sans verrouillage d'objets
# (rien n'y est conservé), objets expirés après 2 jours, téléversements incomplets abandonnés après
# 1 jour. Compartiment surchargeable par GIMAC_S3_COMPARTIMENT_DEPOTS.
depots="${GIMAC_S3_COMPARTIMENT_DEPOTS:-gimac-depots}"
if ! s3 head-bucket --bucket "$depots" >/dev/null 2>&1; then
  s3 create-bucket --bucket "$depots" >/dev/null
fi
# Le cycle de vie n'est qu'un nettoyage (MS-DOCUMENT abandonne et supprime lui-même ses envois) : une
# passerelle S3 qui le refuse ne doit pas bloquer le démarrage de la plateforme.
if ! s3 put-bucket-lifecycle-configuration --bucket "$depots" --lifecycle-configuration \
  '{"Rules":[{"ID":"objets-expires","Status":"Enabled","Filter":{"Prefix":""},"Expiration":{"Days":2}},{"ID":"envois-incomplets","Status":"Enabled","Filter":{"Prefix":""},"AbortIncompleteMultipartUpload":{"DaysAfterInitiation":1}}]}'; then
  echo "Avertissement : règle de cycle de vie refusée sur $depots ; nettoyage par MS-DOCUMENT seulement." >&2
fi
echo "Compartiment des dépôts en cours $depots prêt."
