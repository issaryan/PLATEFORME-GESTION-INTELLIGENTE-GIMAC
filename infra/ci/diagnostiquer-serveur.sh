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

# Serveur injoignable de l'extérieur (« Connection refused » sur tous les ports) : services arrêtés ou
# pare-feu ? L'exécuteur, qui sort vers GitHub, peut encore répondre quand plus rien n'entre.
etape "Accès réseau : démarrage, services, ports en écoute, pare-feu"
echo "Démarré le : $(uptime -s)"
for s in sshd nginx docker firewalld; do printf '  %-10s %s\n' "$s" "$(systemctl is-active "$s" 2>&1)"; done
ss -ltnH | awk '{print $4}' | grep -E ':(22|4200|4201|4202|8463)$' | sort -u | sed 's/^/  en écoute : /'
firewall-cmd --list-all 2>&1 | sed 's/^/  /' || true

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

# Aperçu des documents bureautiques (D-83) : moteur de conversion sur le réseau interne « conversion », sans
# port publié ; santé vue de l'intérieur du conteneur, puis depuis MS-DOCUMENT, son seul client.
etape "Moteur de conversion des aperçus (conversion-documents) : état et santé"
compose ps -a --format 'table {{.Service}}\t{{.Image}}\t{{.State}}\t{{.Status}}' conversion-documents 2>&1
echo "-- santé vue du conteneur"
compose exec -T conversion-documents curl -sS -m 10 http://127.0.0.1:3000/health 2>&1 | head -5
echo
echo "-- santé vue de MS-DOCUMENT"
compose exec -T document curl -sS -m 10 -o /dev/null -w 'statut=%{http_code} duree=%{time_total}s\n' http://conversion-documents:3000/health 2>&1 | head -3
echo "-- aperçus des 24 dernières heures (résultats)"
compose logs --no-color --since 24h document 2>&1 | grep -o 'Aperçu bureautique : .*resultat=[A-Z_]*' | sed -E 's/.*resultat=//' | sort | uniq -c

etape "Réponses de nginx (port 4200) sans session"
hote="$(sed -n 's/^GIMAC_HOTE_PUBLIC=//p' "$ENV" 2>/dev/null)"
for chemin in /api/session /api/auth-info /bff/des/collaboration/messagerie/non-lus; do
  echo "-- GET $chemin"
  curl -sS -m 15 -o /dev/null -w 'statut=%{http_code} redirection=%{redirect_url} duree=%{time_total}s\n' "http://localhost:4200$chemin" 2>&1
  [ -n "$hote" ] && curl -sS -m 15 -o /dev/null -w "  via $hote : statut=%{http_code} duree=%{time_total}s\n" "http://$hote:4200$chemin" 2>&1
done
echo "-- POST /api/connexion (corps vide, attendu 400 ou 401)"
curl -sS -m 15 -o /dev/null -w 'statut=%{http_code} duree=%{time_total}s\n' -X POST -H 'Content-Type: application/json' -d '{}' "http://localhost:4200/api/connexion" 2>&1

etape "Sonde d'intermittence : 200 appels successifs à /api/auth-info (port 4200)"
echecs=0; lents=0
for i in $(seq 1 200); do
  r="$(curl -sS -m 10 -o /dev/null -w '%{http_code} %{time_total}' "http://localhost:4200/api/auth-info" 2>&1 | tail -1)"
  code="${r%% *}"; t="${r##* }"
  [ "$code" = "200" ] || { echecs=$((echecs+1)); echo "  appel $i : $r"; }
  awk -v t="$t" 'BEGIN{exit !(t+0>1)}' && lents=$((lents+1))
done
echo "échecs=$echecs/200, réponses de plus d'une seconde=$lents"

for f in frontend-departement frontend-portail frontend-console; do
  etape "Accès nginx $f (24 h) : statuts des appels /api et /bff"
  compose logs --no-color --since 24h "$f" 2>&1 | grep -E '"(GET|POST|PUT|PATCH|DELETE) /(api|bff)' > /tmp/acces-diag.txt || true
  echo "requêtes=$(wc -l < /tmp/acces-diag.txt)"
  awk '{for(i=1;i<=NF;i++) if($i ~ /^"(GET|POST|PUT|PATCH|DELETE)$/){print $(i+3); break}}' /tmp/acces-diag.txt | sort | uniq -c | sort -rn
  echo "-- statuts 0, 444, 499, 5xx (30 derniers)"
  awk '{for(i=1;i<=NF;i++) if($i ~ /^"(GET|POST|PUT|PATCH|DELETE)$/){ if($(i+3) ~ /^(0|444|499|5[0-9][0-9])$/) print; break}}' /tmp/acces-diag.txt | sed -E 's/\?[^ ]*/?…/' | tail -30 | masquer
  rm -f /tmp/acces-diag.txt
  echo "-- erreurs nginx"
  compose logs --no-color --since 24h "$f" 2>&1 | grep -E '\[(error|crit|alert|emerg)\]' | sed -E 's/\?[^ ,]*/?…/' | tail -20 | masquer
done

etape "Passerelle (24 h) : connexions coupées, délais dépassés"
compose logs --no-color --since 24h gateway 2>&1 | grep -Ei 'reset by peer|prematurely|PrematureClose|timed? ?out|Connection refused|pool|AbortedException' | grep -v 'Not updating' | tail -30 | masquer

etape "Hôte : connexions TCP vers les ports 4200 à 4202"
ss -tan state established '( sport = :4200 or sport = :4201 or sport = :4202 )' 2>/dev/null | wc -l
ss -s 2>/dev/null | head -5

etape "Erreurs récentes de nginx (département)"
compose logs --no-color --since 6h frontend-departement 2>&1 | grep -Ei 'error|upstream|refused|reset|timed out| 50[0-9] ' | tail -40 | masquer

for s in gateway iam document keycloak pilotage notification homologation collaboration conversion-documents; do
  etape "Erreurs récentes : $s (6 dernières heures)"
  compose logs --no-color --since 6h "$s" 2>&1 | grep -E 'ERROR|WARN|Exception|OutOfMemory|Killed|refused|Caused by' | grep -Ev 'Not updating high watermark|HHH90006001|ConversionRuleAction' | tail -40 | masquer
done

etape "Clients de service refusés par Keycloak (dernière heure)"
compose logs --no-color --since 1h keycloak 2>&1 | grep -o 'type="CLIENT_LOGIN_ERROR".*clientId="[^"]*".*error="[^"]*"' | sed -E 's/.*clientId="([^"]*)".*error="([^"]*)".*/\1 \2/' | sort | uniq -c

etape "Jeton de chaque client de service (code HTTP seulement, aucun secret affiché)"
# Le secret est lu dans le .env et transmis par l'environnement (docker exec -e NOM, sans valeur sur la
# ligne de commande) ; seul le code de la réponse de Keycloak est affiché.
for client in pilotage-service:GIMAC_SECRET_PILOTAGE_SERVICE notification-service:GIMAC_SECRET_NOTIFICATION_SERVICE \
              collaboration-service:GIMAC_SECRET_COLLABORATION_SERVICE homologation-service:GIMAC_SECRET_HOMOLOGATION_SERVICE \
              reprise-service:GIMAC_SECRET_REPRISE_SERVICE; do
  CLIENT_ID="${client%%:*}"
  CLIENT_SECRET="$(sed -n "s/^${client#*:}=//p" "$ENV" 2>/dev/null)"
  export CLIENT_ID CLIENT_SECRET
  code="$(compose exec -T -e CLIENT_ID -e CLIENT_SECRET frontend-departement sh -c \
    'wget -q -S -O /dev/null --post-data "grant_type=client_credentials&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET" http://keycloak:9080/realms/gimac/protocol/openid-connect/token 2>&1 | grep -o "HTTP/[0-9.]* [0-9]*" | tail -1' 2>&1)"
  echo "$CLIENT_ID : ${code:-aucune réponse} (secret présent dans .env : $([ -n "$CLIENT_SECRET" ] && echo oui || echo non))"
  unset CLIENT_SECRET
done

etape "Migrations Liquibase (homologation, pilotage, notification)"
for s in homologation pilotage notification; do
  echo "-- $s"; compose logs --no-color "$s" 2>&1 | grep -E "Liquibase|ChangeSet .* ran|d77-reprise|Started [A-Za-z]+App" | tail -6 | masquer
done

etape "Dernières lignes de la passerelle"
compose logs --no-color --tail 40 gateway 2>&1 | masquer
