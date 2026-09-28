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
