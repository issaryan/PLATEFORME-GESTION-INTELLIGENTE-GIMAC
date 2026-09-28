package cm.gimac.des.iam.adapter.outbound.port;

/**
 * Projection d'un GROUPE Keycloak représentant une agence.
 * ────────────────────────────────────────────────────────────────────────────
 * Keycloak est l'autorité du provisionnement des agences : chaque agence est un
 * groupe realm porteur d'un attribut {@code tenant_id}. Les groupes techniques
 * sans {@code tenant_id} (ex. {@code Admins}, {@code Users}) ne sont PAS des agences.
 *
 * @param idGroupe  identifiant du groupe Keycloak (clé de synchronisation stable)
 * @param nom       nom du groupe (ex. {@code AG-YDE-01})
 * @param tenantId  valeur de l'attribut {@code tenant_id} du groupe (clé métier de l'agence)
 */
public record GroupeAgenceKc(String idGroupe, String nom, String tenantId) {}
