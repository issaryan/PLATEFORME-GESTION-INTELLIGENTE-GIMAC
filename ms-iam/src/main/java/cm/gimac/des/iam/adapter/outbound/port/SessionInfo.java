package cm.gimac.des.iam.adapter.outbound.port;

import java.util.Map;

/**
 * Session Keycloak active d'un utilisateur (self-service « Mon profil » → onglet Sessions).
 *
 * @param id         identifiant de session Keycloak (pour révocation ciblée)
 * @param ipAddress  adresse IP d'origine de la session
 * @param start      horodatage epoch-ms d'ouverture de session
 * @param lastAccess horodatage epoch-ms du dernier accès
 * @param clients    applications (clientId) actives dans cette session
 * @param current    true si c'est la session courante de l'appelant
 */
public record SessionInfo(
    String id,
    String ipAddress,
    long start,
    long lastAccess,
    Map<String, String> clients,
    boolean current
) {}
