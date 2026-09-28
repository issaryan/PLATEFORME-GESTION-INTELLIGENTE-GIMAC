#!/bin/sh
# Compartiments WORM de développement (SOC-10, RG-04, RG-16, D-11).
# En DEV le verrouillage est en mode GOUVERNANCE pour 1 jour, afin que le poste
# reste nettoyable. INT, QUAL et PROD utilisent le mode CONFORMITÉ pour 10 ans.
set -eu
mc alias set gimac http://minio:9000 gimac-admin "$GIMAC_MINIO_ROOT_PASSWORD" >/dev/null
for compartiment in gimac-preuves gimac-documents gimac-archives gimac-rapports; do
  if ! mc ls "gimac/$compartiment" >/dev/null 2>&1; then
    mc mb --with-lock "gimac/$compartiment"
  fi
  mc retention set --default GOVERNANCE 1d "gimac/$compartiment"
done
echo "Compartiments WORM prêts."
