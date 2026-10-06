package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.in.ApercuBureautiqueUseCase.Demande;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu;
import cm.gimac.des.document.domain.DocumentException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/**
 * Aperçu bureautique (D-83, apercu-bureautique.md §2) : contrôles, cache par empreinte, limite de débit par
 * compte, et traduction des erreurs du moteur en codes du contrat.
 */
class ApercuBureautiqueServiceTest {

    static final byte[] PDF = "%PDF-1.7 apercu".getBytes(StandardCharsets.US_ASCII);

    static byte[] docx(String texte) {
        byte[] t = texte.getBytes(StandardCharsets.UTF_8);
        byte[] c = Arrays.copyOf(new byte[] { 'P', 'K', 3, 4 }, 4 + t.length);
        System.arraycopy(t, 0, c, 4, t.length);
        return c;
    }

    /** Horloge réglable. */
    static final class Horloge extends Clock {

        Instant instant = Instant.parse("2026-10-06T08:00:00Z");

        void avancer(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    /** Convertisseur simulé : appels comptés, comportement réglable. */
    static final class ConvertisseurSimule implements ConvertisseurApercu {

        final List<String> appels = new ArrayList<>();
        BiFunction<String, byte[], byte[]> reponse = (ext, contenu) -> PDF;

        @Override
        public byte[] convertirEnPdf(String extension, byte[] contenu) {
            appels.add(extension);
            return reponse.apply(extension, contenu);
        }
    }

    Horloge horloge;
    ConvertisseurSimule moteur;
    ApercuBureautiqueService service;

    @BeforeEach
    void init() {
        horloge = new Horloge();
        moteur = new ConvertisseurSimule();
        service = new ApercuBureautiqueService(moteur, ApercuProprietes.parDefaut(), horloge);
    }

    static String code(Throwable t) {
        return ((DocumentException) t).code();
    }

    static int statut(Throwable t) {
        return ((DocumentException) t).status();
    }

    @Test
    @DisplayName("Conversion : PDF du moteur, extension seule transmise")
    void conversion() {
        assertThat(service.apercu(new Demande("Rapport mensuel.DOCX", docx("a")), "u1")).isEqualTo(PDF);
        assertThat(moteur.appels).containsExactly("docx");
        assertThat(service.tailleMax()).isEqualTo(20L * 1024 * 1024);
    }

    @Test
    @DisplayName("Cache par empreinte SHA-256 : un même fichier n'est converti qu'une fois, quel que soit le compte ou le nom")
    void cache() {
        byte[] f = docx("même contenu");
        service.apercu(new Demande("a.docx", f), "u1");
        service.apercu(new Demande("autre nom.docx", f.clone()), "u2");
        assertThat(moteur.appels).hasSize(1);
        // Même contenu sous une autre extension : autre filtre d'import, nouvelle conversion.
        service.apercu(new Demande("a.odt", f), "u1");
        assertThat(moteur.appels).hasSize(2);
        // Contenu différent : nouvelle conversion.
        service.apercu(new Demande("a.docx", docx("autre")), "u1");
        assertThat(moteur.appels).hasSize(3);
    }

    @Test
    @DisplayName("Cache : entrée expirée après 24 h")
    void expirationCache() {
        byte[] f = docx("x");
        service.apercu(new Demande("a.docx", f), "u1");
        horloge.avancer(Duration.ofHours(23));
        service.apercu(new Demande("a.docx", f), "u1");
        assertThat(moteur.appels).hasSize(1);
        horloge.avancer(Duration.ofHours(2));
        service.apercu(new Demande("a.docx", f), "u1");
        assertThat(moteur.appels).hasSize(2);
    }

    @Test
    @DisplayName("Cache borné en taille : au-delà de la taille totale, les PDF sont évincés")
    void tailleCache() {
        ApercuProprietes p = new ApercuProprietes(null, null, null, null, null, DataSize.ofBytes(PDF.length), null);
        ApercuBureautiqueService petit = new ApercuBureautiqueService(moteur, p, horloge);
        moteur.reponse = (ext, contenu) -> new byte[PDF.length * 2];
        petit.apercu(new Demande("a.docx", docx("1")), "u1");
        petit.apercu(new Demande("a.docx", docx("1")), "u1");
        assertThat(moteur.appels).hasSize(2);
    }

    @Test
    @DisplayName("Limite : 30 conversions par minute et par compte, 429 DOC_APERCU_LIMITE au-delà ; les aperçus en cache ne comptent pas")
    void limite() {
        for (int i = 0; i < 30; i++) {
            service.apercu(new Demande("a.docx", docx("f" + i)), "u1");
        }
        // Aperçu déjà converti : servi par le cache malgré la limite atteinte.
        assertThat(service.apercu(new Demande("a.docx", docx("f0")), "u1")).isEqualTo(PDF);
        assertThatThrownBy(() -> service.apercu(new Demande("a.docx", docx("f30")), "u1")).satisfies(e -> {
            assertThat(code(e)).isEqualTo("DOC_APERCU_LIMITE");
            assertThat(statut(e)).isEqualTo(429);
            assertThat(((DocumentException) e).details()).containsEntry("limiteParMinute", 30).containsKey("reessayerDans");
        });
        // Un autre compte n'est pas concerné.
        service.apercu(new Demande("a.docx", docx("f30")), "u2");
        assertThat(moteur.appels).hasSize(31);
        // Fenêtre glissante : une minute après, de nouveau admis.
        horloge.avancer(Duration.ofSeconds(61));
        service.apercu(new Demande("a.docx", docx("f31")), "u1");
        assertThat(moteur.appels).hasSize(32);
    }

    @Test
    @DisplayName("413 DOC_APERCU_TROP_VOLUMINEUX au-delà de 20 Mo, sans appel au moteur")
    void tropVolumineux() {
        byte[] gros = docx("x");
        gros = Arrays.copyOf(gros, 20 * 1024 * 1024 + 1);
        byte[] f = gros;
        assertThatThrownBy(() -> service.apercu(new Demande("a.docx", f), "u1")).satisfies(e -> {
            assertThat(code(e)).isEqualTo("DOC_APERCU_TROP_VOLUMINEUX");
            assertThat(statut(e)).isEqualTo(413);
        });
        // 20 Mo exactement : admis.
        assertThat(service.apercu(new Demande("a.docx", Arrays.copyOf(gros, 20 * 1024 * 1024)), "u1")).isEqualTo(PDF);
        assertThat(moteur.appels).hasSize(1);
    }

    @Test
    @DisplayName("415 DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE : extension hors liste, nom absent, signature incohérente, contenu vide")
    void formatNonPrisEnCharge() {
        for (Demande d : List.of(
            new Demande("a.pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)),
            new Demande(null, docx("x")),
            new Demande("a.docm", docx("x")),
            new Demande("a.docx", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)),
            new Demande("a.doc", docx("x")),
            new Demande("a.rtf", docx("x")),
            new Demande("a.docx", new byte[0]),
            new Demande("a.docx", null)
        )) {
            assertThatThrownBy(() -> service.apercu(d, "u1")).as(d.nomFichier()).satisfies(e -> {
                assertThat(code(e)).isEqualTo("DOC_APERCU_FORMAT_NON_PRIS_EN_CHARGE");
                assertThat(statut(e)).isEqualTo(415);
            });
        }
        assertThat(moteur.appels).isEmpty();
    }

    @Test
    @DisplayName("Moteur : refus du fichier en 422 DOC_APERCU_ILLISIBLE, indisponibilité en 503 DOC_APERCU_INDISPONIBLE ; rien en cache")
    void erreursMoteur() {
        moteur.reponse = (ext, c) -> {
            throw new ConvertisseurApercu.ConversionRefusee("mot de passe");
        };
        assertThatThrownBy(() -> service.apercu(new Demande("a.docx", docx("p")), "u1")).satisfies(e -> {
            assertThat(code(e)).isEqualTo("DOC_APERCU_ILLISIBLE");
            assertThat(statut(e)).isEqualTo(422);
        });
        moteur.reponse = (ext, c) -> {
            throw new ConvertisseurApercu.MoteurIndisponible("délai", null);
        };
        assertThatThrownBy(() -> service.apercu(new Demande("a.docx", docx("p")), "u1")).satisfies(e -> {
            assertThat(code(e)).isEqualTo("DOC_APERCU_INDISPONIBLE");
            assertThat(statut(e)).isEqualTo(503);
        });
        moteur.reponse = (ext, c) -> PDF;
        assertThat(service.apercu(new Demande("a.docx", docx("p")), "u1")).isEqualTo(PDF);
        assertThat(moteur.appels).hasSize(3);
    }
}
