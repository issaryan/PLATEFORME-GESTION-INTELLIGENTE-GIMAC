#!/usr/bin/env bash
# Clé RSA persistante du jeton interne de la passerelle (D-134, RsaKeyConfig) : PEM PKCS#8 de 2048 bits, générée une
# seule fois hors du dépôt, jamais régénérée si elle existe (les services de la sandbox gardent ses clés publiques
# jusqu'au prochain rafraîchissement du JWKS). Usage : generer-cle-passerelle.sh <chemin du fichier>.
set -euo pipefail
cible="${1:?chemin du fichier de clé attendu}"
if [ -s "$cible" ]; then
  echo "Clé de signature de la passerelle déjà présente : $cible"
  exit 0
fi
command -v openssl >/dev/null || { echo "openssl est requis pour générer la clé de signature de la passerelle" >&2; exit 1; }
mkdir -p "$(dirname "$cible")"
umask 077
temporaire="$(mktemp "$(dirname "$cible")/.cle-XXXXXX")"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$temporaire" 2>/dev/null
# Lisible par l'utilisateur du conteneur (montage du seul fichier, en lecture seule) ; le dossier parent reste privé.
chmod 644 "$temporaire"
mv "$temporaire" "$cible"
echo "Clé de signature de la passerelle générée : $cible"
