/**
 * Sessions de la passerelle ouvertes par la connexion sur la plateforme (D-65) : ouverture après
 * l'octroi direct, session restreinte du premier accès, fermeture après 30 minutes d'inactivité
 * ou 10 heures de durée (docs/lot1/connexion.md §6), révocation des jetons chez Keycloak.
 */
package cm.gimac.des.gateway.security.session;
