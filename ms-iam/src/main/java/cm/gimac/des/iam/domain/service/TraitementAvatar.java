package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Photo de profil (docs/lot1/mon-compte.md §1), service pur : type réel reconnu par les octets (PNG,
 * JPEG, WebP ; jamais par l'extension ni le type annoncé), 2 Mo au plus, dimensions entre 64 et
 * 4 096 px lues dans l'en-tête avant tout décodage (pas de bombe de décompression), recadrage carré
 * centré, réduction à 256 × 256 et réencodage en PNG. Le réencodage ne reprend que les pixels : les
 * métadonnées de l'original (EXIF, position GPS, profils, commentaires) ne sont jamais conservées, pas
 * plus que l'image d'origine.
 */
public final class TraitementAvatar {

    /** Taille maximale du fichier reçu : 2 Mo. */
    public static final int TAILLE_MAX = 2 * 1024 * 1024;
    public static final int DIMENSION_MIN = 64;
    public static final int DIMENSION_MAX = 4096;
    /** Côté de l'image conservée. */
    public static final int COTE = 256;
    /** Type de l'image conservée (le contrat admet image/webp ou image/png). */
    public static final String TYPE_CONSERVE = "image/png";

    /** Formats reconnus par leur signature. */
    public enum Format {
        PNG("png"),
        JPEG("jpeg"),
        WEBP("webp");

        private final String lecteur;

        Format(String lecteur) {
            this.lecteur = lecteur;
        }

        public String lecteur() {
            return lecteur;
        }
    }

    /** Image conservée : octets réencodés, type et version (empreinte courte des octets). */
    public record ImageAvatar(byte[] octets, String type, String version) {
        @Override
        public String toString() {
            return "ImageAvatar[type=" + type + ", version=" + version + ", taille=" + (octets == null ? 0 : octets.length) + "]";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ImageAvatar i && java.util.Arrays.equals(octets, i.octets) && java.util.Objects.equals(type, i.type) && java.util.Objects.equals(version, i.version);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(java.util.Arrays.hashCode(octets), type, version);
        }
    }

    private TraitementAvatar() {}

    /** Contrôle et transforme l'image reçue ; IamException IAM_AVATAR_* si elle est refusée. */
    public static ImageAvatar traiter(byte[] octets) {
        if (octets == null || octets.length == 0) {
            throw illisible();
        }
        if (octets.length > TAILLE_MAX) {
            throw tropGrande();
        }
        Format format = detecter(octets);
        if (format == null) {
            throw IamException.invalide("IAM_AVATAR_TYPE", "Format refusé : PNG, JPEG ou WebP", Map.of());
        }
        BufferedImage source = decoder(octets, format);
        byte[] png = encoder(carre(source, COTE));
        return new ImageAvatar(png, TYPE_CONSERVE, version(png));
    }

    /** Format d'après la signature des premiers octets ; nul s'il n'est pas admis. */
    public static Format detecter(byte[] o) {
        if (o == null) {
            return null;
        }
        if (o.length >= 8 && (o[0] & 0xFF) == 0x89 && o[1] == 'P' && o[2] == 'N' && o[3] == 'G' && o[4] == 0x0D && o[5] == 0x0A && o[6] == 0x1A && o[7] == 0x0A) {
            return Format.PNG;
        }
        if (o.length >= 3 && (o[0] & 0xFF) == 0xFF && (o[1] & 0xFF) == 0xD8 && (o[2] & 0xFF) == 0xFF) {
            return Format.JPEG;
        }
        if (o.length >= 12 && o[0] == 'R' && o[1] == 'I' && o[2] == 'F' && o[3] == 'F' && o[8] == 'W' && o[9] == 'E' && o[10] == 'B' && o[11] == 'P') {
            return Format.WEBP;
        }
        return null;
    }

    /** Dimensions lues dans l'en-tête, puis décodage de la seule première image, métadonnées ignorées. */
    static BufferedImage decoder(byte[] octets, Format format) {
        Iterator<ImageReader> lecteurs = ImageIO.getImageReadersByFormatName(format.lecteur());
        if (!lecteurs.hasNext()) {
            throw illisible();
        }
        ImageReader lecteur = lecteurs.next();
        try (ImageInputStream entree = ImageIO.createImageInputStream(new ByteArrayInputStream(octets))) {
            lecteur.setInput(entree, true, true);
            int largeur = lecteur.getWidth(0);
            int hauteur = lecteur.getHeight(0);
            if (largeur < DIMENSION_MIN || hauteur < DIMENSION_MIN || largeur > DIMENSION_MAX || hauteur > DIMENSION_MAX) {
                throw IamException.invalide(
                    "IAM_AVATAR_DIMENSIONS",
                    "Dimensions refusées : entre " + DIMENSION_MIN + " et " + DIMENSION_MAX + " pixels",
                    Map.of("largeur", largeur, "hauteur", hauteur)
                );
            }
            BufferedImage image = lecteur.read(0);
            if (image == null) {
                throw illisible();
            }
            return image;
        } catch (IamException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw illisible();
        } finally {
            lecteur.dispose();
        }
    }

    /**
     * Recadrage carré centré, puis réduction par moitiés successives (bilinéaire) jusqu'au côté voulu :
     * plus fidèle qu'une réduction directe pour une grande image. Transparence conservée.
     */
    static BufferedImage carre(BufferedImage source, int cote) {
        int c = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - c) / 2;
        int y = (source.getHeight() - c) / 2;
        BufferedImage courante = copier(source.getSubimage(x, y, c, c), c);
        int taille = c;
        while (taille / 2 >= cote) {
            taille /= 2;
            courante = copier(courante, taille);
        }
        return taille == cote ? courante : copier(courante, cote);
    }

    private static BufferedImage copier(BufferedImage source, int cote) {
        BufferedImage cible = new BufferedImage(cote, cote, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cible.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, cote, cote, null);
        } finally {
            g.dispose();
        }
        return cible;
    }

    /** PNG sans bloc de métadonnées : seuls les pixels sont écrits. */
    static byte[] encoder(BufferedImage image) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream(64 * 1024);
        try {
            if (!ImageIO.write(image, "png", sortie)) {
                throw illisible();
            }
        } catch (IOException e) {
            throw illisible();
        }
        return sortie.toByteArray();
    }

    /** Empreinte courte (16 caractères hexadécimaux du SHA-256) : change avec l'image, sert au cache. */
    public static String version(byte[] octets) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(octets), 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static IamException tropGrande() {
        return IamException.invalide("IAM_AVATAR_TOO_LARGE", "Image trop volumineuse : 2 Mo au plus", Map.of("tailleMax", TAILLE_MAX));
    }

    static IamException illisible() {
        return IamException.invalide("IAM_AVATAR_UNREADABLE", "Image illisible", Map.of());
    }
}
