#!/usr/bin/env bash
# Sujets Kafka du lot 1 (PI-GIMAC-01 §0.5) : sujets d'agrégat (6 partitions, rétention
# 30 jours) et file de rebut .dlq de chaque sujet consommé. collaboration.message porte
# collaboration.message-sent.v1 (D-64), clé = conversation, sans contenu de message.
#   KAFKA_BOOTSTRAP  courtier (défaut : kafka:9092, réseau Docker gimac-des_default)
#   REPLICATION      facteur de réplication (défaut 3 ; 1 sur le poste de développement)
#   KAFKA_TOPICS     commande kafka-topics.sh ; par défaut, l'image officielle Apache Kafka
#                    lancée sur le réseau du projet (l'image native du socle n'a pas les outils)
set -euo pipefail
KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:9092}"
REPLICATION="${REPLICATION:-3}"
MIN_ISR=$(( REPLICATION > 1 ? 2 : 1 ))
KAFKA_TOPICS="${KAFKA_TOPICS:-docker run --rm --network gimac-des_default apache/kafka:4.3.1 /opt/kafka/bin/kafka-topics.sh}"

creer() { # sujet partitions [options...]
  local sujet=$1 partitions=$2; shift 2
  $KAFKA_TOPICS --bootstrap-server "$KAFKA_BOOTSTRAP" --create --if-not-exists --topic "$sujet" \
    --partitions "$partitions" --replication-factor "$REPLICATION" "$@" >/dev/null
  echo "sujet prêt : $sujet"
}

for t in homologation.referential homologation.liaison homologation.operation homologation.campaign homologation.coverage \
         homologation.notification certification.circuit certification.quitus certification.dossier certification.attestation \
         certification.notification \
         participant.institution participant.adhesion participant.transition \
         collaboration.message; do
  creer "$t" 6 --config retention.ms=2592000000 --config min.insync.replicas="$MIN_ISR"
done
for t in participant.subscription participant.status document.document iam.user iam.role homologation.liaison \
         homologation.operation homologation.coverage certification.quitus certification.dossier certification.attestation; do
  creer "$t.dlq" 3
done
