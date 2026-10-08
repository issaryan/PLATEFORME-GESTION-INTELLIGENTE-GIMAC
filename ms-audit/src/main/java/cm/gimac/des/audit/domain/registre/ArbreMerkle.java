package cm.gimac.des.audit.domain.registre;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Arbre de Merkle du registre d'audit (CC §6.1.3.3, RFC 9162) : empreinte de feuille {@code SHA256(0x00 || canon)},
 * nœud interne {@code SHA256(0x01 || gauche || droite)} (séparation de domaine), racine {@code MTH} récursive avec
 * {@code k} la plus grande puissance de deux strictement inférieure à {@code n}. Le registre ne conserve que les nœuds
 * des sous-arbres complets (niveau {@code l}, position {@code p} : feuilles {@code [p·2^l, (p+1)·2^l)}), d'où toute
 * racine se recalcule. Fonctions pures, empreintes en hexadécimal minuscule.
 */
public final class ArbreMerkle {

    /** Nœud d'un sous-arbre complet : niveau (0 = feuille), position, empreinte. */
    public record Noeud(int niveau, long position, String empreinte) {}

    /** Lecture d'un nœud déjà inscrit (niveau, position) ; absent : null. */
    @FunctionalInterface
    public interface Lecteur {
        String empreinte(int niveau, long position);
    }

    private static final HexFormat HEX = HexFormat.of();

    private ArbreMerkle() {}

    /** Empreinte de feuille d'une forme canonique (UTF-8). */
    public static String feuille(String canon) {
        MessageDigest d = sha256();
        d.update((byte) 0x00);
        d.update(canon.getBytes(StandardCharsets.UTF_8));
        return HEX.formatHex(d.digest());
    }

    /** Empreinte d'un nœud interne. */
    public static String noeud(String gauche, String droite) {
        MessageDigest d = sha256();
        d.update((byte) 0x01);
        d.update(HEX.parseHex(gauche));
        d.update(HEX.parseHex(droite));
        return HEX.formatHex(d.digest());
    }

    /** Racine MTH d'une liste d'empreintes de feuilles (RFC 9162 §2.1.1) ; liste vide : SHA256 de la chaîne vide. */
    public static String racine(List<String> feuilles) {
        if (feuilles.isEmpty()) {
            return HEX.formatHex(sha256().digest());
        }
        return mth(feuilles, 0, feuilles.size());
    }

    private static String mth(List<String> f, int debut, int n) {
        if (n == 1) {
            return f.get(debut);
        }
        int k = plusGrandePuissanceInferieure(n);
        return noeud(mth(f, debut, k), mth(f, debut + k, n - k));
    }

    /**
     * Nœuds à inscrire quand la feuille d'index {@code index} entre dans l'arbre : la feuille, puis chaque sous-arbre
     * complet qu'elle achève (son frère gauche est lu parmi les nœuds déjà inscrits).
     *
     * @throws IllegalStateException si un frère gauche manque (registre incomplet)
     */
    public static List<Noeud> noeudsAInscrire(long index, String feuille, Lecteur lecteur) {
        List<Noeud> r = new ArrayList<>();
        int niveau = 0;
        long position = index;
        String empreinte = feuille;
        r.add(new Noeud(niveau, position, empreinte));
        while ((position & 1L) == 1L) {
            String gauche = lecteur.empreinte(niveau, position - 1);
            if (gauche == null) {
                throw new IllegalStateException("Nœud manquant au niveau " + niveau + ", position " + (position - 1));
            }
            empreinte = noeud(gauche, empreinte);
            niveau++;
            position >>= 1;
            r.add(new Noeud(niveau, position, empreinte));
        }
        return r;
    }

    /** Racine de l'arbre des {@code taille} premières feuilles, recalculée depuis les sous-arbres complets inscrits. */
    public static String racine(long taille, Lecteur lecteur) {
        if (taille <= 0) {
            return HEX.formatHex(sha256().digest());
        }
        return mth(0, taille, lecteur);
    }

    private static String mth(long debut, long n, Lecteur lecteur) {
        if (Long.bitCount(n) == 1 && debut % n == 0) {
            int niveau = Long.numberOfTrailingZeros(n);
            String e = lecteur.empreinte(niveau, debut >> niveau);
            if (e == null) {
                throw new IllegalStateException("Nœud manquant au niveau " + niveau + ", position " + (debut >> niveau));
            }
            return e;
        }
        long k = Long.highestOneBit(n - 1);
        return noeud(mth(debut, k, lecteur), mth(debut + k, n - k, lecteur));
    }

    private static int plusGrandePuissanceInferieure(int n) {
        return Integer.highestOneBit(n - 1);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
