# syntax=docker/dockerfile:1
# Image d'une couche de présentation (D-46, D-51) : construction Vite puis service
# des fichiers statiques par nginx sans privilège (port 8080, utilisateur non root).
#   docker build --build-arg APPLICATION=departement -t frontend-departement .
# APPLICATION : departement | portail | console
# Écrit à la main : aucun générateur (JHipster, docker init) ne couvre ce frontend séparé.

FROM node:22.23.3-alpine AS construction
WORKDIR /src
COPY package.json package-lock.json ./
COPY packages/theme/package.json packages/theme/
COPY apps/departement/package.json apps/departement/
COPY apps/portail/package.json apps/portail/
COPY apps/console/package.json apps/console/
RUN npm ci --no-audit --no-fund
COPY packages packages
COPY apps apps
ARG APPLICATION
RUN test -n "$APPLICATION" && npm run build -w "@gimac/app-$APPLICATION"

FROM nginxinc/nginx-unprivileged:1.31.2-alpine
ARG APPLICATION
LABEL org.opencontainers.image.title="gimac-frontend-$APPLICATION" \
      org.opencontainers.image.vendor="GIMAC - Département Exploitation et Supervision"
# Passerelle MS-GATEWAY vers laquelle /api, /oauth2 et /login sont relayés (BFF, RG-58),
# et résolveur DNS (127.0.0.11 sous Docker ; service DNS de la grappe sous Kubernetes).
ENV GIMAC_PASSERELLE=http://gateway:8080 \
    GIMAC_RESOLVEUR=127.0.0.11
COPY nginx/gimac.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/securite.conf /etc/nginx/gimac-securite.conf
COPY --from=construction /src/apps/$APPLICATION/dist /usr/share/nginx/html
EXPOSE 8080
