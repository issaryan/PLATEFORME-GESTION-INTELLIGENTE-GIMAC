# Sujets Kafka

`creer-sujets-lot1.sh` crée les sujets d'agrégat du lot 1 (6 partitions, rétention 30 jours) et la file de rebut `.dlq` de chaque sujet consommé (PI-GIMAC-01 §0.5). En développement : `REPLICATION=1 infra/kafka/creer-sujets-lot1.sh` (plateforme démarrée).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Script des sujets du lot 1 (D-55) |
