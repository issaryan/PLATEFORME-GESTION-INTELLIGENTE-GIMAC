/**
 * Supervision de la plateforme (D-61) : modèle et règles du module qui donne au super
 * administrateur, en temps réel, l'état des services et des serveurs, les alertes, la
 * configuration effective et les niveaux de journalisation. Aucune dépendance technique :
 * les sources (actionneurs Spring, Prometheus, Alertmanager, routes) sont des ports.
 */
package cm.gimac.des.gateway.domain.supervision;
