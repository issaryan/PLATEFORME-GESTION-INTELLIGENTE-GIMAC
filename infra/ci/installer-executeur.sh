#!/usr/bin/env bash
# Exécuteur GitHub Actions auto-hébergé de la CI (étiquette gimac-ci) sur un poste Linux x86_64 avec
# systemd : utilisateur système dédié, exécuteur vérifié par son empreinte SHA-256, service systemd,
# dépendances système de Playwright et entretien hebdomadaire. Voir docs/exploitation/executeur-ci.md.
#
#   sudo infra/ci/installer-executeur.sh [numéro]       # instance 1 par défaut : /opt/actions-runner/gimac-ci-1
#
# Le jeton d'inscription (GitHub : Settings > Actions > Runners > New self-hosted runner, valable une heure)
# est demandé sur le terminal, sans écho ; il n'est écrit ni dans un fichier ni dans un journal.
# Idempotent : une instance déjà inscrite n'est ni retéléchargée ni réinscrite.
set -euo pipefail

DEPOT_URL="https://github.com/issaryan/PLATEFORME-GESTION-INTELLIGENTE-GIMAC"
UTILISATEUR=gimac-ci
ACCUEIL=/var/lib/gimac-ci
INSTANCE="${1:-1}"
[[ "$INSTANCE" =~ ^[1-9]$ ]] || { echo "Numéro d'instance attendu : 1 à 9" >&2; exit 2; }
DOSSIER="/opt/actions-runner/gimac-ci-$INSTANCE"
NOM="gimac-ci-$(hostname -s)-$INSTANCE"
RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

etape() { echo; echo "== $*"; }
[ "$(id -u)" = 0 ] || { echo "À lancer avec sudo." >&2; exit 1; }
[ "$(uname -m)" = x86_64 ] || { echo "Poste x86_64 attendu." >&2; exit 1; }
command -v systemctl >/dev/null || { echo "systemd attendu." >&2; exit 1; }
getent group docker >/dev/null || { echo "Groupe docker absent : installer Docker Engine d'abord." >&2; exit 1; }

etape "Utilisateur $UTILISATEUR (système, sans droits d'administration, groupe docker)"
if ! id "$UTILISATEUR" >/dev/null 2>&1; then
  useradd --system --create-home --home-dir "$ACCUEIL" --shell /bin/bash "$UTILISATEUR"
fi
usermod -aG docker "$UTILISATEUR"
chmod 750 "$ACCUEIL"
id "$UTILISATEUR"

etape "Exécuteur GitHub Actions dans $DOSSIER"
if [ ! -x "$DOSSIER/config.sh" ]; then
  publication="$(curl -fsSL https://api.github.com/repos/actions/runner/releases/latest)"
  version="$(python3 -c 'import json, sys; print(json.load(sys.stdin)["tag_name"].lstrip("v"))' <<<"$publication")"
  attendue="$(python3 -c 'import json, re, sys; m = re.search(r"BEGIN SHA linux-x64 -->([0-9a-f]{64})", json.load(sys.stdin)["body"]); print(m.group(1) if m else "")' <<<"$publication")"
  [ -n "$attendue" ] || { echo "Empreinte SHA-256 publiée introuvable pour la version $version" >&2; exit 1; }
  archive="$(mktemp)"
  trap 'rm -f "$archive"' EXIT
  curl -fsSL -o "$archive" "https://github.com/actions/runner/releases/download/v$version/actions-runner-linux-x64-$version.tar.gz"
  echo "$attendue  $archive" | sha256sum -c --quiet || { echo "Empreinte SHA-256 incorrecte : archive rejetée" >&2; exit 1; }
  echo "Version $version, empreinte SHA-256 vérifiée"
  mkdir -p "$DOSSIER"
  tar -xzf "$archive" -C "$DOSSIER"
  chown -R "$UTILISATEUR:$UTILISATEUR" "$DOSSIER"
  "$DOSSIER/bin/installdependencies.sh"
fi
chmod 750 "$DOSSIER"
# Ports des aperçus Playwright propres à l'instance (5200 reste au poste) : deux instances ou un
# développeur et la CI ne se gênent pas (frontend/e2e/playwright.config.ts, GIMAC_E2E_PORT).
echo "GIMAC_E2E_PORT=$((5300 + 100 * INSTANCE))" > "$DOSSIER/.env"
chown "$UTILISATEUR:$UTILISATEUR" "$DOSSIER/.env"

etape "Dépendances système de Playwright (Chromium)"
version_playwright="$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1]))["packages"]["node_modules/@playwright/test"]["version"])' "$RACINE/frontend/package-lock.json")"
if command -v npx >/dev/null; then
  npx -y "playwright@$version_playwright" install-deps chromium
else
  echo "npx absent : dépendances de Playwright à installer plus tard (sudo npx playwright install-deps chromium)" >&2
fi

etape "Inscription ($NOM, étiquette gimac-ci)"
if [ -f "$DOSSIER/.runner" ]; then
  echo "Déjà inscrit"
else
  read -rsp "Jeton d'inscription (non affiché) : " jeton </dev/tty; echo
  [ -n "$jeton" ] || { echo "Jeton vide" >&2; exit 1; }
  (cd "$DOSSIER" && sudo -u "$UTILISATEUR" ./config.sh --unattended --url "$DEPOT_URL" --token "$jeton" \
    --name "$NOM" --labels gimac-ci --work _work --replace)
  unset jeton
fi

etape "Service systemd"
cd "$DOSSIER"
# svc.sh install écrit .service (nom de l'unité) : présent, le service existe déjà.
[ -f .service ] || ./svc.sh install "$UTILISATEUR"
./svc.sh start
./svc.sh status | sed -n '1,4p'
cd - >/dev/null

etape "Entretien hebdomadaire (images et constructions Docker de plus de 7 jours, dossiers _work inactifs)"
cat > /etc/systemd/system/gimac-ci-entretien.service <<'UNITE'
[Unit]
Description=Entretien des exécuteurs gimac-ci (Docker et dossiers de travail)
After=docker.service

[Service]
Type=oneshot
User=gimac-ci
# Objets inutilisés depuis plus de 7 jours ; images de conteneurs existants et volumes jamais touchés.
ExecStart=/usr/bin/docker system prune -af --filter until=168h
# Copies de dépôts inactives depuis 7 jours ; outils (_tool, _actions) et état de l'exécuteur conservés.
ExecStart=/usr/bin/find /opt/actions-runner -mindepth 3 -maxdepth 3 -path '*/_work/*' -type d -not -name '_*' -mtime +7 -prune -exec rm -rf {} +
UNITE
cat > /etc/systemd/system/gimac-ci-entretien.timer <<'UNITE'
[Unit]
Description=Entretien hebdomadaire des exécuteurs gimac-ci

[Timer]
OnCalendar=Sun 03:00
Persistent=true

[Install]
WantedBy=timers.target
UNITE
systemctl daemon-reload
systemctl enable --now gimac-ci-entretien.timer
systemctl list-timers gimac-ci-entretien.timer --no-pager | sed -n '1,2p'

echo
echo "Exécuteur $NOM installé : vérifier dans GitHub (Settings > Actions > Runners) qu'il est « Idle »."
