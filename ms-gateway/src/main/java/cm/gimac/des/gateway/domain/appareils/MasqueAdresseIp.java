package cm.gimac.des.gateway.domain.appareils;

/**
 * Masquage d'une adresse IP (docs/lot1/mon-compte.md §3) : dernier octet d'une adresse IPv4
 * (« 41.202.10.* »), dernier groupe d'une adresse IPv6 (« 2001:db8:0:0:0:0:0:* ») ; autre valeur : « * ».
 */
public final class MasqueAdresseIp {

    private MasqueAdresseIp() {}

    public static String masquer(String adresse) {
        if (adresse == null || adresse.isBlank()) {
            return "*";
        }
        String a = adresse.trim();
        int i = a.lastIndexOf(a.indexOf(':') >= 0 ? ':' : '.');
        if (i <= 0 || (a.indexOf(':') < 0 && a.chars().filter(c -> c == '.').count() != 3)) {
            return "*";
        }
        return a.substring(0, i + 1) + "*";
    }
}
