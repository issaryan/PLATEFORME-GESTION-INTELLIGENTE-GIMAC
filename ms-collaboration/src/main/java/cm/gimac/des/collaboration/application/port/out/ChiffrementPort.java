package cm.gimac.des.collaboration.application.port.out;

import java.util.UUID;

/**
 * Chiffrement au repos du contenu des messages (D-64 : AES-256-GCM, clé hors base). L'identifiant du
 * message est lié au chiffré : un contenu déplacé vers un autre message ne se déchiffre pas.
 */
public interface ChiffrementPort {
    byte[] chiffrer(String clair, UUID messageId);

    String dechiffrer(byte[] chiffre, UUID messageId);
}
