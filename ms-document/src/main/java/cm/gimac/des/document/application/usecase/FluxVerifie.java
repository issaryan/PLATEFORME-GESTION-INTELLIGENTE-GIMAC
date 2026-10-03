package cm.gimac.des.document.application.usecase;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Flux d'un document vérifié au fil de la lecture : l'empreinte SHA-256 des octets lus est
 * calculée, et comparée à l'empreinte inscrite au moment où la taille inscrite est atteinte,
 * avant que les derniers octets ne soient rendus. En cas d'écart (ou de flux plus court), la
 * lecture échoue (IOException) : la réponse est interrompue et le destinataire ne reçoit jamais le
 * contenu altéré en entier (Content-Length non atteint). Les octets au-delà de la taille inscrite
 * ne sont jamais lus.
 */
class FluxVerifie extends FilterInputStream {

    /** Écart constaté entre le contenu lu et l'empreinte ou la taille inscrites. */
    static final class ContenuAltere extends IOException {

        private static final long serialVersionUID = 1L;

        ContenuAltere(String message) {
            super(message);
        }
    }

    /** Notifié une fois, à la première altération constatée (journal). */
    interface Alerte {
        void signaler(String motif);
    }

    private final MessageDigest sha;
    private final String attendue;
    private final long taille;
    private final Alerte alerte;
    private long lus;
    private boolean termine;

    FluxVerifie(InputStream source, String sha256Attendue, long taille, Alerte alerte) {
        super(source);
        this.attendue = sha256Attendue;
        this.taille = taille;
        this.alerte = alerte;
        try {
            this.sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    @Override
    public int read() throws IOException {
        byte[] un = new byte[1];
        int n = read(un, 0, 1);
        return n < 0 ? -1 : un[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (termine) {
            return -1;
        }
        int n = in.read(b, off, (int) Math.min(len, taille - lus));
        if (n < 0) {
            throw altere("contenu plus court que la taille inscrite (" + lus + " octets sur " + taille + ")");
        }
        sha.update(b, off, n);
        lus += n;
        if (lus == taille) {
            termine = true;
            if (!HexFormat.of().formatHex(sha.digest()).equals(attendue)) {
                throw altere("empreinte du contenu lu différente de l'empreinte inscrite");
            }
        }
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        // Chaque octet doit entrer dans l'empreinte : le saut est une lecture.
        long sautes = 0;
        byte[] tampon = new byte[8192];
        while (sautes < n) {
            int r = read(tampon, 0, (int) Math.min(tampon.length, n - sautes));
            if (r < 0) {
                break;
            }
            sautes += r;
        }
        return sautes;
    }

    @Override
    public int available() throws IOException {
        return termine ? 0 : (int) Math.min(super.available(), taille - lus);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int limite) {
        // Sans effet : la vérification exige une lecture unique, dans l'ordre.
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset non pris en charge");
    }

    private ContenuAltere altere(String motif) {
        termine = true;
        alerte.signaler(motif);
        return new ContenuAltere(motif);
    }
}
