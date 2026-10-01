package cm.gimac.des.gateway.domain.connexion;

import java.util.Locale;

/**
 * Identifiant masqué pour le journal d'audit (contrat D-65 §2, point 8) : deux premiers caractères
 * et dernier caractère de la partie locale, domaine d'une adresse électronique conservé.
 * « jean.dupont » devient « je***t », « jean@gimac.org » devient « je***n@gimac.org ».
 */
public final class MasqueIdentifiant {

    private MasqueIdentifiant() {}

    public static String masquer(String identifiant) {
        if (identifiant == null || identifiant.isBlank()) {
            return "";
        }
        String id = identifiant.trim().toLowerCase(Locale.ROOT);
        int arobase = id.indexOf('@');
        String local = arobase > 0 ? id.substring(0, arobase) : id;
        String domaine = arobase > 0 ? id.substring(arobase) : "";
        String masque = local.length() <= 3 ? "***" : local.substring(0, 2) + "***" + local.charAt(local.length() - 1);
        return masque + domaine;
    }
}
