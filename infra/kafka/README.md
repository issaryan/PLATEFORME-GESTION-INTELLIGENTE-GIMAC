# Sujets Kafka

`creer-sujets-lot1.sh` crée les sujets d'agrégat du lot 1 (6 partitions, rétention 30 jours) et la file de rebut `.dlq` de chaque sujet consommé (PI-GIMAC-01 §0.5). Au démarrage de l'environnement Docker Compose, le conteneur `kafka-sujets-init` l'exécute automatiquement (`REPLICATION=1`, prérequis des services). À la main : `REPLICATION=1 infra/kafka/creer-sujets-lot1.sh` (plateforme démarrée). Un sujet existant n'est pas modifié : un sujet auto-créé auparavant avec 1 partition le reste (le supprimer, ou `kafka-topics.sh --alter --partitions 6`).

## Journal des mises à jour

| Date | Commit | Modification |
|---|---|---|
| 2026-09-29 | — | Script des sujets du lot 1 (D-55) |
| 2026-10-01 | — | Exécution automatique par `kafka-sujets-init` ; sujets existants lus une fois et ignorés |
| 2026-10-23 | D-122, D-124 | Sujet `sbx-lecture.campaign-published.v1` et sa file `.dlq` (fin de campagne de SBX-LECTURE, US-EXI-15) ; les autres sujets de la sandbox, nommés d'après le type de l'événement, sont créés par la sandbox |
