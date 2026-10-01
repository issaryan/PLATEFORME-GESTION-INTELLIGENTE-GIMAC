# Image Keycloak de la plateforme DES (D-62, D-65) : thème GIMAC, extension « gimac-totp » (code TOTP
# configuré sur la plateforme, docs/lot1/connexion.md §4) et domaine « gimac » importé au démarrage
# (secrets substitués depuis l'environnement).
# Construite depuis infra/keycloak : docker build -t registre.gimac.local/des/keycloak:26.7.4-gimac .

# Étape 1 : extension gimac-totp (Java 21, dépendances Keycloak alignées sur l'image ci-dessous),
# tests unitaires compris.
FROM maven:3.9.11-eclipse-temurin-21 AS extension
WORKDIR /construction
COPY extensions/gimac-totp/pom.xml pom.xml
RUN mvn -B -ntp -q dependency:go-offline
COPY extensions/gimac-totp/src src
RUN mvn -B -ntp -q package

# Étape 2 : Keycloak ; l'extension est prise en compte à la construction automatique du serveur
# (« start ») avec les options de base de données du déploiement.
FROM quay.io/keycloak/keycloak:26.7.4-0
COPY --from=extension --chown=keycloak:keycloak /construction/target/gimac-totp.jar /opt/keycloak/providers/gimac-totp.jar
COPY --chown=keycloak:keycloak themes/gimac /opt/keycloak/themes/gimac
# DOMAINE : gimac-realm.json (développement, recette : comptes test-*) ; gimac-realm-production.json pour la
# production (aucun compte de test, seuls les comptes de service) : --build-arg DOMAINE=gimac-realm-production.json.
ARG DOMAINE=gimac-realm.json
COPY --chown=keycloak:keycloak ${DOMAINE} /opt/keycloak/data/import/gimac-realm.json
