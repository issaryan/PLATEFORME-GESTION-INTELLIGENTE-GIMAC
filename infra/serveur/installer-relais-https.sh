#!/usr/bin/env bash
# Installe le relais HTTPS de Moabi dans le nginx de l'hôte 10.20.36.214 (D-75,
# docs/exploitation/publication-https.md). À exécuter sur le serveur par un administrateur (sudo) :
#
#   infra/serveur/installer-relais-https.sh <certificat.pem> <cle.pem> [--appliquer]
#
# Sans --appliquer, la configuration produite est seulement affichée. Avec --appliquer, elle est écrite
# dans /etc/nginx/conf.d/moabi.conf, contrôlée (nginx -t), puis nginx est rechargé ; en cas d'échec du
# contrôle, l'ancienne version est remise en place. Le certificat et la clé restent où ils sont : seuls
# leurs chemins sont écrits dans la configuration (ceux du certificat *.gimac-afr.org de default.conf).
set -euo pipefail

ICI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PUB="$ICI/publication.env"
MODELE="$ICI/nginx/relais-moabi.conf.modele"
CIBLE=/etc/nginx/conf.d/moabi.conf

[ $# -ge 2 ] || { sed -n '2,10p' "$0"; exit 2; }
certificat="$1"; cle="$2"; appliquer="${3:-}"
lire() { sed -n "s/^$1=//p" "$PUB"; }
domaine="$(lire GIMAC_DOMAINE)"
port="$(lire GIMAC_PORT_HTTPS)"; port="${port:-443}"

bloc() {
  local nom="$1" interface="$2"
  cat <<BLOC
server {
    listen $port ssl;
    http2 on;
    server_name $nom.$domaine;
    server_tokens off;

    ssl_certificate     $certificat;
    ssl_certificate_key $cle;
    ssl_protocols       TLSv1.2 TLSv1.3;

    client_max_body_size 110m;

    location / {
        proxy_pass http://127.0.0.1:$interface;
        proxy_http_version 1.1;
        proxy_set_header Host \$http_host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection \$moabi_connexion;
        proxy_buffering off;
        proxy_request_buffering off;
        proxy_read_timeout 1h;
        proxy_send_timeout 1h;
    }
}
BLOC
}

blocs="$(bloc "$(lire GIMAC_NOM_INTERNE)" 4200; echo; bloc "$(lire GIMAC_NOM_PORTAIL)" 4201; echo; bloc "$(lire GIMAC_NOM_CONSOLE)" 4202)"
conf="$(sed -e "s/@DOMAINE@/$domaine/g" -e '/^@BLOCS@$/d' "$MODELE")"$'\n'"$blocs"

if [ "$appliquer" != "--appliquer" ]; then
  printf '%s\n' "$conf"
  echo "# (aperçu : relancer avec --appliquer pour installer dans $CIBLE)" >&2
  exit 0
fi

[ -r "$certificat" ] && [ -r "$cle" ] || sudo test -r "$certificat" -a -r "$cle" || { echo "Certificat ou clé illisible." >&2; exit 1; }
sauvegarde=""
if [ -f "$CIBLE" ]; then sauvegarde="$(mktemp)"; sudo cp "$CIBLE" "$sauvegarde"; fi
printf '%s\n' "$conf" | sudo tee "$CIBLE" >/dev/null
if sudo nginx -t; then
  sudo systemctl reload nginx 2>/dev/null || sudo nginx -s reload
  echo "Relais HTTPS installé : $CIBLE"
else
  echo "Contrôle nginx en échec : configuration précédente remise en place." >&2
  if [ -n "$sauvegarde" ]; then sudo cp "$sauvegarde" "$CIBLE"; else sudo rm -f "$CIBLE"; fi
  exit 1
fi
