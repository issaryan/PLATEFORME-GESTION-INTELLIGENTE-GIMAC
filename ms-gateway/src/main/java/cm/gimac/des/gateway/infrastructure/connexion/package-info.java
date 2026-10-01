/**
 * Connexion sur la plateforme (D-65) : adaptateurs des ports sortants vers Keycloak (octroi direct,
 * validation des jetons par le JWKS, révocation), MS-IAM (jeton de service gateway-service),
 * limitation des tentatives (Valkey ou mémoire) et journal d'audit (Kafka).
 */
package cm.gimac.des.gateway.infrastructure.connexion;
