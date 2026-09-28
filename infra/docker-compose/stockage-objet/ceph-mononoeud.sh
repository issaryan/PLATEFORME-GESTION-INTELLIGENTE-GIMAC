#!/bin/bash
# Ceph mono-nœud de DÉVELOPPEMENT (D-50) : moniteur, gestionnaire, un OSD BlueStore
# sur fichier et la passerelle S3 RGW, dans un seul conteneur de l'image officielle.
# Réplication 1 et authentification interne désactivée : jamais hors DEV.
# INT, QUAL et PROD utilisent une grappe Ceph déployée par la Production Informatique.
set -euo pipefail

DONNEES=/var/lib/ceph
CONF=/etc/ceph/ceph.conf
: "${GIMAC_S3_CLE_ACCES:?}" "${GIMAC_S3_CLE_SECRETE:?}"

if [ ! -f "$DONNEES/.amorce" ]; then
  echo "Amorçage du Ceph mono-nœud..."
  FSID=$(uuidgen)
  mkdir -p /etc/ceph "$DONNEES"/{mon/ceph-a,mgr/ceph-a,osd/ceph-0,radosgw/ceph-rgw.gimac}
  cat > "$CONF" <<CONF
[global]
fsid = $FSID
mon initial members = a
mon host = 127.0.0.1
auth cluster required = none
auth service required = none
auth client required = none
osd pool default size = 1
osd pool default min size = 1
osd crush chooseleaf type = 0
mon allow pool size one = true
mon warn on pool no redundancy = false
mon allow pool delete = false
osd objectstore = bluestore
bluestore block create = true
bluestore block size = 10737418240
osd memory target = 939524096
osd class update on start = false
log to stderr = false
[client.rgw.gimac]
rgw frontends = beast port=7480
rgw data = $DONNEES/radosgw/ceph-rgw.gimac
CONF
  cp "$CONF" "$DONNEES/ceph.conf"
  monmaptool --create --add a 127.0.0.1 --fsid "$FSID" /tmp/monmap
  ceph-mon --mkfs -i a --monmap /tmp/monmap
  touch "$DONNEES/.amorce-mon"
fi
cp "$DONNEES/ceph.conf" "$CONF"

ceph-mon -i a
ceph-mgr -i a
if [ ! -f "$DONNEES/.amorce" ]; then
  OSD_UUID=$(uuidgen)
  OSD=$(ceph osd create "$OSD_UUID")
  ceph-osd -i "$OSD" --mkfs --osd-uuid "$OSD_UUID"
fi
ceph-osd -i 0
until ceph osd stat 2>/dev/null | grep -q "1 up"; do sleep 2; done
radosgw -n client.rgw.gimac
until curl -fsS http://127.0.0.1:7480 >/dev/null 2>&1; do sleep 2; done

if [ ! -f "$DONNEES/.amorce" ]; then
  radosgw-admin user create --uid=gimac --display-name="Plateforme DES (DEV)" \
    --access-key="$GIMAC_S3_CLE_ACCES" --secret="$GIMAC_S3_CLE_SECRETE" >/dev/null
  touch "$DONNEES/.amorce"
fi
echo "Passerelle S3 prête sur le port 7480."
# Le conteneur vit tant que la passerelle vit.
exec tail --pid="$(pgrep -o radosgw)" -f /dev/null
