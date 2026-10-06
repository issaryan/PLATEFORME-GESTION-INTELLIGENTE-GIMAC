#!/usr/bin/env bash
# Sujets Kafka du lot 1 (PI-GIMAC-01 §0.5) : sujets d'agrégat (6 partitions, rétention
# 30 jours) et file de rebut .dlq de chaque sujet consommé. collaboration.message porte
# collaboration.message-sent.v1 (D-64), clé = conversation, sans contenu de message.
# certification.proces-verbal et certification.approbation portent les verrous de la décision (D-56),
# consommés par MS-HOMOLOGATION (docs/lot1/parcours-backend.md §3). pilotage.task et pilotage.report portent
# les événements de Mon activité (D-76, docs/lot1/mon-activite.md §5), clé = tâche ou rapport, sans contenu.
# notification.inapp porte notification.inapp-created.v1 (D-80, docs/lot1/notifications.md), clé = notification,
# relayé par la passerelle au seul destinataire. iam.request porte iam.request-submitted.v1 et iam.request-decided.v1
# (demandes à l'administration, D-81, docs/lot1/pouvoirs-hierarchiques.md §4), clé = demande, sans contenu.
# collaboration.message, pilotage.task, pilotage.report et iam.request sont consommés par MS-NOTIFICATION.
# iam.account porte iam.account-administered.v1 (activation, désactivation… : D-87, remplacement des intervenants partis) ;
# pilotage.process porte les événements des processus d'unité (D-87), consommés par MS-NOTIFICATION.
#   KAFKA_BOOTSTRAP  courtier (défaut : kafka:9092, réseau Docker gimac-des_default)
#   REPLICATION      facteur de réplication (défaut 3 ; 1 sur le poste de développement)
#   KAFKA_TOPICS     commande kafka-topics.sh ; par défaut, l'image officielle Apache Kafka
#                    lancée sur le réseau du projet (l'image native du socle n'a pas les outils)
# Exécuté au démarrage de la pile par le conteneur kafka-sujets-init (infra/docker-compose).
set -euo pipefail
KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:9092}"
REPLICATION="${REPLICATION:-3}"
MIN_ISR=$(( REPLICATION > 1 ? 2 : 1 ))
KAFKA_TOPICS="${KAFKA_TOPICS:-docker run --rm --network gimac-des_default apache/kafka:4.3.1 /opt/kafka/bin/kafka-topics.sh}"

# Sujets existants lus une fois : un sujet présent n'est ni recréé ni modifié (une JVM de moins par sujet).
EXISTANTS=$($KAFKA_TOPICS --bootstrap-server "$KAFKA_BOOTSTRAP" --list)

creer() { # sujet partitions [options...]
  local sujet=$1 partitions=$2; shift 2
  if grep -qxF "$sujet" <<< "$EXISTANTS"; then echo "sujet existant : $sujet"; return; fi
  $KAFKA_TOPICS --bootstrap-server "$KAFKA_BOOTSTRAP" --create --if-not-exists --topic "$sujet" \
    --partitions "$partitions" --replication-factor "$REPLICATION" "$@" >/dev/null
  echo "sujet prêt : $sujet"
}

for t in homologation.referential homologation.liaison homologation.operation homologation.campaign homologation.coverage \
         homologation.notification certification.circuit certification.quitus certification.dossier certification.attestation \
         certification.notification certification.proces-verbal certification.approbation \
         participant.institution participant.adhesion participant.transition \
         collaboration.message pilotage.task pilotage.report pilotage.process notification.inapp iam.request iam.account; do
  creer "$t" 6 --config retention.ms=2592000000 --config min.insync.replicas="$MIN_ISR"
done
for t in participant.subscription participant.status document.document iam.user iam.role homologation.liaison \
         homologation.operation homologation.coverage certification.quitus certification.dossier certification.attestation \
         certification.proces-verbal certification.approbation collaboration.message pilotage.task pilotage.report pilotage.process \
         iam.request iam.account; do
  creer "$t.dlq" 3
done
