package cm.gimac.des.document.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.domain.service.EmpreinteSha256;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Flux vérifié au fil de la lecture : empreinte comparée avant les derniers octets, jamais au-delà de la taille inscrite. */
class FluxVerifieTest {

    static final byte[] OCTETS = "Ordre du jour".getBytes(StandardCharsets.US_ASCII);
    final List<String> alertes = new ArrayList<>();

    FluxVerifie flux(byte[] source, String sha, long taille) {
        return new FluxVerifie(new ByteArrayInputStream(source), sha, taille, alertes::add);
    }

    @Test
    @DisplayName("Contenu conforme : lu octet par octet ou par blocs, fin de flux à la taille inscrite, octets en trop jamais lus")
    void conforme() throws IOException {
        byte[] avecSurplus = (new String(OCTETS, StandardCharsets.US_ASCII) + "EN TROP").getBytes(StandardCharsets.US_ASCII);
        try (FluxVerifie f = flux(avecSurplus, EmpreinteSha256.de(OCTETS), OCTETS.length)) {
            assertThat(f.read()).isEqualTo('O');
            assertThat(f.available()).isEqualTo(OCTETS.length - 1);
            assertThat(f.read(new byte[4], 0, 0)).isZero();
            byte[] reste = f.readAllBytes();
            assertThat(reste).hasSize(OCTETS.length - 1);
            assertThat(f.read()).isEqualTo(-1);
            assertThat(f.available()).isZero();
        }
        assertThat(alertes).isEmpty();
    }

    @Test
    @DisplayName("Saut : les octets sautés entrent dans l'empreinte ; mark/reset refusés")
    void saut() throws IOException {
        try (FluxVerifie f = flux(OCTETS, EmpreinteSha256.de(OCTETS), OCTETS.length)) {
            assertThat(f.markSupported()).isFalse();
            f.mark(10);
            assertThat(f.skip(5)).isEqualTo(5);
            assertThatThrownBy(f::reset).isInstanceOf(IOException.class);
            assertThat(f.skip(100)).isEqualTo(OCTETS.length - 5);
        }
        assertThat(alertes).isEmpty();
    }

    @Test
    @DisplayName("Empreinte différente : IOException au dernier bloc, alerte journalisée une fois")
    void empreinteDifferente() {
        FluxVerifie f = flux(OCTETS, "0".repeat(64), OCTETS.length);
        assertThatThrownBy(f::readAllBytes).isInstanceOf(FluxVerifie.ContenuAltere.class);
        assertThat(alertes).hasSize(1);
    }

    @Test
    @DisplayName("Flux plus court que la taille inscrite : IOException")
    void tronque() {
        FluxVerifie f = flux(OCTETS, EmpreinteSha256.de(OCTETS), OCTETS.length + 5L);
        assertThatThrownBy(f::readAllBytes).isInstanceOf(IOException.class).hasMessageContaining("plus court");
        assertThat(alertes).hasSize(1);
    }
}
