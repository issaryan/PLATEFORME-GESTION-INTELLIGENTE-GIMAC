# syntax=docker/dockerfile:1
# Image d'une couche de présentation (D-46, D-51) : construction Vite puis service
# des fichiers statiques par nginx sans privilège (port 8080, utilisateur non root).
#   docker build --build-arg APPLICATION=departement -t frontend-departement .
# APPLICATION : departement | portail | console
# SERVICES : http (défaut, passerelle MS-GATEWAY) ; simule pour une image de démonstration, jamais en production
#   (services et comptes fictifs). Toute autre valeur est refusée.
# URL_CONSOLE : adresse de la console d'administration, cible des liens « Administration » de
#   l'application du département (D-62) ; http://localhost:4202 par défaut.
# URL_DEPARTEMENT : adresse de l'application du département, cible du retour depuis la console (D-69) ;
#   http://localhost:4200 par défaut.
# URL_DES, URL_PORTAIL : adresses de l'application interne et du portail, cibles de l'aiguillage des comptes
#   entre les trois applications (D-79) ; URL_DES vaut URL_DEPARTEMENT à défaut, URL_PORTAIL
#   http://localhost:4201 par défaut. Les trois applications reçoivent les trois adresses.
# Écrit à la main : aucun générateur (JHipster, docker init) ne couvre ce frontend séparé.

# 1. Manifestes de l'espace de travail npm : chaque package.json (racine, packages/*, apps/*, e2e),
#    à sa place, sans le code. npm ci n'est rejoué que si un manifeste ou le verrou change, et tout
#    nouvel espace de travail est pris en compte sans modifier ce fichier.
FROM node:22.23.3-alpine AS manifestes
# Un échec de find ou de tar dans le tube interrompt la construction (hadolint DL4006).
SHELL ["/bin/ash", "-eo", "pipefail", "-c"]
WORKDIR /src
COPY . .
RUN mkdir /manifestes \
 && find . -name package.json -not -path '*/node_modules/*' | tar -cf - -T - | tar -xf - -C /manifestes \
 && cp package-lock.json /manifestes/

# 2. Dépendances puis construction de la couche demandée.
FROM node:22.23.3-alpine AS construction
WORKDIR /src
COPY --from=manifestes /manifestes/ ./
# Derrière un relais d'entreprise : --build-arg HTTPS_PROXY=… et, si le relais réécrit le TLS,
# --secret id=ca,src=<autorité de certification> (jamais copiée dans l'image).
RUN --mount=type=secret,id=ca,required=false \
    if [ -f /run/secrets/ca ]; then export NODE_EXTRA_CA_CERTS=/run/secrets/ca; fi; \
    npm ci --no-audit --no-fund
COPY packages packages
COPY apps apps
COPY e2e e2e
ARG APPLICATION
ARG SERVICES=http
ARG VERSION=0.1.0
ARG URL_CONSOLE=http://localhost:4202
ARG URL_DEPARTEMENT=http://localhost:4200
ARG URL_DES=$URL_DEPARTEMENT
ARG URL_PORTAIL=http://localhost:4201
ENV VITE_SERVICES=$SERVICES \
    VITE_VERSION=$VERSION \
    VITE_URL_CONSOLE=$URL_CONSOLE \
    VITE_URL_DEPARTEMENT=$URL_DEPARTEMENT \
    VITE_URL_DES=$URL_DES \
    VITE_URL_PORTAIL=$URL_PORTAIL
RUN test -n "$APPLICATION" \
 && case "$SERVICES" in http|simule) ;; *) echo "SERVICES : http ou simule" >&2; exit 1 ;; esac \
 && npm run build -w "@gimac/app-$APPLICATION"

# 3. Service des fichiers statiques.
FROM nginxinc/nginx-unprivileged:1.31.2-alpine
ARG APPLICATION
LABEL org.opencontainers.image.title="gimac-frontend-$APPLICATION" \
      org.opencontainers.image.vendor="GIMAC - Département Exploitation et Supervision"
# Passerelle MS-GATEWAY vers laquelle /api, /bff et /logout sont relayés (BFF, RG-58),
# et résolveur DNS (127.0.0.11 sous Docker ; service DNS de la grappe sous Kubernetes).
ENV GIMAC_PASSERELLE=http://gateway:8080 \
    GIMAC_RESOLVEUR=127.0.0.11
COPY nginx/gimac.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/securite.conf /etc/nginx/gimac-securite.conf
COPY --from=construction /src/apps/$APPLICATION/dist /usr/share/nginx/html
EXPOSE 8080
