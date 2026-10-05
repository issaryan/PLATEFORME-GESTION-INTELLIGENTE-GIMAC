#!/usr/bin/env bash
# Diagnostic en lecture seule du serveur de démonstration (10.20.36.214), lancé à la demande par
# .github/workflows/diagnostic-serveur.yml quand la plateforme ne répond pas comme attendu : état des
# conteneurs, ressources de l'hôte, santé de la passerelle vue de l'intérieur du réseau Docker, réponses
# de nginx sur /api et /bff, et dernières erreurs des journaux. Rien n'est modifié ni redémarré.
# Aucun secret n'est affiché : ni variables d'environnement, ni .env ; dans les journaux, les jetons
# (JWT) et les valeurs de mots de passe éventuels sont masqués.
set -uo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_DIR="$RACINE/infra/docker-compose"
ENV="$COMPOSE_DIR/.env"
compose() {
  docker compose --project-name gimac-des -f "$COMPOSE_DIR/docker-compose.yml" \
    -f "$COMPOSE_DIR/docker-compose.gimac.yml" -f "$COMPOSE_DIR/docker-compose.serveur.yml" --env-file "$ENV" "$@"
}
etape() { echo; echo "== $*"; }
masquer() { sed -E 's/eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]+/<jeton masqué>/g; s/((mot.?de.?passe|password|secret|motDePasse)[^:=]{0,20}[:=]\s*)[^ ,"]+/\1<masqué>/Ig' | cut -c1-400; }

etape "Hôte : mémoire, disque, charge"
free -m; df -h / /var/lib/docker 2>/dev/null; uptime

etape "Conteneurs (état, santé, redémarrages)"
compose ps -a --format 'table {{.Service}}\t{{.State}}\t{{.Status}}'
for c in $(compose ps -a -q); do
  docker inspect -f '{{.Name}} restarts={{.RestartCount}} oom={{.State.OOMKilled}} exit={{.State.ExitCode}} etat={{.State.Status}}' "$c"
done

etape "Consommation mémoire des conteneurs"
mapfile -t actifs < <(compose ps -q)
[ "${#actifs[@]}" -gt 0 ] && docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}\t{{.CPUPerc}}' "${actifs[@]}" 2>/dev/null

etape "Passerelle vue depuis le conteneur du département"
compose exec -T frontend-departement sh -c 'wget -q -S -O - http://gateway:8080/management/health 2>&1 | head -20' 2>&1 | masquer

etape "Réponses de nginx (port 4200) sans session"
hote="$(sed -n 's/^GIMAC_HOTE_PUBLIC=//p' "$ENV" 2>/dev/null)"
for chemin in /api/session /api/auth-info /bff/des/collaboration/messagerie/non-lus; do
  echo "-- GET $chemin"
  curl -sS -m 15 -o /dev/null -w 'statut=%{http_code} redirection=%{redirect_url} duree=%{time_total}s\n' "http://localhost:4200$chemin" 2>&1
  [ -n "$hote" ] && curl -sS -m 15 -o /dev/null -w "  via $hote : statut=%{http_code} duree=%{time_total}s\n" "http://$hote:4200$chemin" 2>&1
done
echo "-- POST /api/connexion (corps vide, attendu 400 ou 401)"
curl -sS -m 15 -o /dev/null -w 'statut=%{http_code} duree=%{time_total}s\n' -X POST -H 'Content-Type: application/json' -d '{}' "http://localhost:4200/api/connexion" 2>&1

etape "Erreurs récentes de nginx (département)"
compose logs --no-color --since 6h frontend-departement 2>&1 | grep -Ei 'error|upstream|refused|reset|timed out| 50[0-9] ' | tail -40 | masquer

for s in gateway iam document keycloak; do
  etape "Erreurs récentes : $s (6 dernières heures)"
  compose logs --no-color --since 6h "$s" 2>&1 | grep -E 'ERROR|WARN|Exception|OutOfMemory|Killed|refused|Caused by' | tail -40 | masquer
done

etape "Dernières lignes de la passerelle"
compose logs --no-color --tail 40 gateway 2>&1 | masquer
