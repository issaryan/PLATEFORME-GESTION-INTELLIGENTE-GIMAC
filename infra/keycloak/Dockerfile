# Image Keycloak de la plateforme DES (D-62) : thème GIMAC des pages de connexion et des
# courriels, domaine « gimac » importé au démarrage (secrets substitués depuis l'environnement).
# Construite depuis infra/keycloak : docker build -t registre.gimac.local/des/keycloak:26.7.4-gimac .
FROM quay.io/keycloak/keycloak:26.7.4-0
COPY --chown=keycloak:keycloak themes/gimac /opt/keycloak/themes/gimac
COPY --chown=keycloak:keycloak gimac-realm.json /opt/keycloak/data/import/gimac-realm.json
