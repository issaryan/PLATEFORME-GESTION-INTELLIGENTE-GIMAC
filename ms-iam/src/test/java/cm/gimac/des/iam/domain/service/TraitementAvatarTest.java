package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.service.TraitementAvatar.Format;
import cm.gimac.des.iam.domain.service.TraitementAvatar.ImageAvatar;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Photo de profil (docs/lot1/mon-compte.md §1) : type par les octets, limites, recadrage, réencodage sans métadonnées. */
public class TraitementAvatarTest {

    public static byte[] ressource(String nom) throws IOException {
        try (InputStream in = TraitementAvatarTest.class.getResourceAsStream("/avatars/" + nom)) {
            return in.readAllBytes();
        }
    }

    public static byte[] png(int largeur, int hauteur) throws IOException {
        BufferedImage im = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < largeur; x++) {
            for (int y = 0; y < hauteur; y++) {
                im.setRGB(x, y, x < largeur / 2 ? 0xC81E1E : 0x1E1EC8);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(im, "png", out);
        return out.toByteArray();
    }

    public static BufferedImage lire(byte[] octets) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(octets));
    }

    static void refus(byte[] octets, String code) {
        assertThatThrownBy(() -> TraitementAvatar.traiter(octets)).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(400);
        });
    }

    public static boolean contient(byte[] octets, String texte) {
        byte[] t = texte.getBytes(StandardCharsets.ISO_8859_1);
        outer:
        for (int i = 0; i + t.length <= octets.length; i++) {
            for (int j = 0; j < t.length; j++) {
                if (octets[i + j] != t[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    @Test
    @DisplayName("Signature : PNG, JPEG et WebP reconnus par les octets ; GIF, PDF, SVG et octets courts refusés")
    void detection() throws IOException {
        assertThat(TraitementAvatar.detecter(png(64, 64))).isEqualTo(Format.PNG);
        assertThat(TraitementAvatar.detecter(ressource("gps.jpg"))).isEqualTo(Format.JPEG);
        assertThat(TraitementAvatar.detecter(ressource("paysage.webp"))).isEqualTo(Format.WEBP);
        assertThat(TraitementAvatar.detecter("GIF89a......".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(TraitementAvatar.detecter("%PDF-1.7".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(TraitementAvatar.detecter("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(TraitementAvatar.detecter("RIFF0000WAVE".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(TraitementAvatar.detecter(new byte[] { (byte) 0xFF, (byte) 0xD8 })).isNull();
        assertThat(TraitementAvatar.detecter(null)).isNull();
        assertThat(Format.WEBP.lecteur()).isEqualTo("webp");
        refus("GIF89a......".getBytes(StandardCharsets.US_ASCII), "IAM_AVATAR_TYPE");
    }

    @Test
    @DisplayName("Paysage WebP : recadrage carré centré, 256 × 256, réencodé en PNG, version stable")
    void webp() throws IOException {
        ImageAvatar a = TraitementAvatar.traiter(ressource("paysage.webp"));
        assertThat(a.type()).isEqualTo("image/png");
        assertThat(TraitementAvatar.detecter(a.octets())).isEqualTo(Format.PNG);
        BufferedImage im = lire(a.octets());
        assertThat(im.getWidth()).isEqualTo(256);
        assertThat(im.getHeight()).isEqualTo(256);
        // Carré centré de 200 px pris dans 300 : la limite rouge/bleu (x = 150) tombe au milieu.
        assertThat((im.getRGB(20, 128) >> 16) & 0xFF).isGreaterThan(150);
        assertThat(im.getRGB(235, 128) & 0xFF).isGreaterThan(150);
        assertThat(a.version()).hasSize(16).matches("[0-9a-f]{16}").isEqualTo(TraitementAvatar.version(a.octets()));
        assertThat(TraitementAvatar.traiter(ressource("paysage.webp")).version()).isEqualTo(a.version());
        assertThat(a.toString()).contains("image/png").doesNotContain("[B@");
    }

    @Test
    @DisplayName("JPEG avec EXIF et position GPS : métadonnées absentes de l'image conservée")
    void exifSupprime() throws IOException {
        byte[] origine = ressource("gps.jpg");
        assertThat(contient(origine, "Exif")).isTrue();
        assertThat(contient(origine, "FabricantSecret")).isTrue();
        ImageAvatar a = TraitementAvatar.traiter(origine);
        assertThat(contient(a.octets(), "Exif")).isFalse();
        assertThat(contient(a.octets(), "FabricantSecret")).isFalse();
        assertThat(contient(a.octets(), "eXIf")).isFalse();
        assertThat(lire(a.octets()).getWidth()).isEqualTo(256);
    }

    @Test
    @DisplayName("PNG portant un commentaire : commentaire supprimé ; portrait recadré ; transparence conservée")
    void commentaireSupprime() throws IOException {
        byte[] origine = ressource("commentaire.png");
        assertThat(contient(origine, "CommentaireSecret")).isTrue();
        ImageAvatar a = TraitementAvatar.traiter(origine);
        assertThat(contient(a.octets(), "CommentaireSecret")).isFalse();
        assertThat(contient(a.octets(), "tEXt")).isFalse();
        BufferedImage im = lire(a.octets());
        assertThat(im.getWidth()).isEqualTo(256);
        assertThat(im.getColorModel().hasAlpha()).isTrue();
    }

    @Test
    @DisplayName("Grande image : réductions successives jusqu'à 256 × 256")
    void grande() throws IOException {
        BufferedImage im = lire(TraitementAvatar.traiter(png(2048, 1500)).octets());
        assertThat(im.getWidth()).isEqualTo(256);
        assertThat(im.getHeight()).isEqualTo(256);
        assertThat(lire(TraitementAvatar.traiter(png(1024, 1024)).octets()).getWidth()).isEqualTo(256);
    }

    @Test
    @DisplayName("Limites : 2 Mo, dimensions entre 64 et 4 096 px, octets illisibles")
    void limites() throws IOException {
        byte[] gros = Arrays.copyOf(png(64, 64), TraitementAvatar.TAILLE_MAX + 1);
        refus(gros, "IAM_AVATAR_TOO_LARGE");
        refus(png(63, 200), "IAM_AVATAR_DIMENSIONS");
        refus(png(200, 63), "IAM_AVATAR_DIMENSIONS");
        refus(png(4097, 64), "IAM_AVATAR_DIMENSIONS");
        refus(png(64, 4097), "IAM_AVATAR_DIMENSIONS");
        assertThat(lire(TraitementAvatar.traiter(png(64, 64)).octets()).getWidth()).isEqualTo(256);
        byte[] tronque = Arrays.copyOf(png(100, 100), 40);
        refus(tronque, "IAM_AVATAR_UNREADABLE");
        byte[] fauxPng = Arrays.copyOf(png(100, 100), 8);
        refus(fauxPng, "IAM_AVATAR_UNREADABLE");
        byte[] fauxJpeg = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1, 2, 3, 4, 5, 6 };
        refus(fauxJpeg, "IAM_AVATAR_UNREADABLE");
        refus(new byte[0], "IAM_AVATAR_UNREADABLE");
        refus(null, "IAM_AVATAR_UNREADABLE");
    }

    @Test
    @DisplayName("Égalité des images par contenu")
    void egalite() {
        ImageAvatar a = new ImageAvatar(new byte[] { 1, 2 }, "image/png", "v");
        assertThat(a).isEqualTo(new ImageAvatar(new byte[] { 1, 2 }, "image/png", "v")).hasSameHashCodeAs(new ImageAvatar(new byte[] { 1, 2 }, "image/png", "v"));
        assertThat(a).isNotEqualTo(new ImageAvatar(new byte[] { 1, 3 }, "image/png", "v")).isNotEqualTo("autre");
        assertThat(new ImageAvatar(null, "image/png", "v").toString()).contains("taille=0");
    }
}
