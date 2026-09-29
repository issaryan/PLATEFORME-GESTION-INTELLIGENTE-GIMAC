package cm.gimac.des.iam.domain.event;

import java.util.List;

/** iam.configuration-changed.v1 : politique de sécurité, messagerie ou profil modifiés depuis la console. */
public record ConfigurationModifieePayload(String objet, String acteurId, String motif, List<String> champs) {
    public static final String TYPE = "iam.configuration-changed.v1";
}
