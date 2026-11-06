package cm.gimac.des.document.infrastructure.apercu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.out.ConvertisseurApercu.ConversionRefusee;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.MoteurIndisponible;
import cm.gimac.des.document.application.port.out.ConvertisseurHtml.Papier;
import cm.gimac.des.document.application.usecase.ApercuProprietes;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Impression HTML par Gotenberg (Chromium, D-102) contre un faux moteur HTTP local : requête multipart attendue
 * par POST /forms/chromium/convert/html (index.html, footer.html, papier et marges en pouces, fonds), PDF rendu,
 * traduction des refus et des pannes.
 */
class ConvertisseurHtmlGotenbergTest {

    static final byte[] PDF = "%PDF-1.7 imprime".getBytes(StandardCharsets.US_ASCII);
    static final Papier A3_PAYSAGE = new Papier(420, 297, 12, 16, 12);

    HttpServer serveur;
    final AtomicReference<String> chemin = new AtomicReference<>();
    final AtomicReference<String> corps = new AtomicReference<>();
    volatile int statut = 200;
    volatile byte[] reponse = PDF;

    @BeforeEach
    void demarrer() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", this::repondre);
        serveur.start();
    }

    @AfterEach
    void arreter() {
        serveur.stop(0);
    }

    void repondre(HttpExchange e) throws IOException {
        chemin.set(e.getRequestMethod() + " " + e.getRequestURI().getPath());
        corps.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        e.sendResponseHeaders(statut, reponse.length);
        e.getResponseBody().write(reponse);
        e.close();
    }

    ConvertisseurHtmlGotenberg adaptateur(String url) {
        return new ConvertisseurHtmlGotenberg(new ApercuProprietes(url, Duration.ofSeconds(5), Duration.ofSeconds(2), null, null, null, null));
    }

    ConvertisseurHtmlGotenberg adaptateur() {
        return adaptateur("http://127.0.0.1:" + serveur.getAddress().getPort());
    }

    @Test
    @DisplayName("Métadonnées du PDF (D-139) : champ metadata en JSON échappé ; absentes : aucun champ metadata")
    void metadonnees() {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("Title", "Programme \"W46\" \\ é");
        m.put("Keywords", "a\nb");
        assertThat(adaptateur().imprimer("<html></html>", null, A3_PAYSAGE, m)).isEqualTo(PDF);
        assertThat(corps.get()).contains("name=\"metadata\"").contains("{\"Title\":\"Programme \\\"W46\\\" \\\\ é\",\"Keywords\":\"a\\u000ab\"}");
        adaptateur().imprimer("<html></html>", null, A3_PAYSAGE);
        assertThat(corps.get()).doesNotContain("name=\"metadata\"");
        assertThat(ConvertisseurHtmlGotenberg.json(java.util.Map.of())).isEqualTo("{}");
        java.util.Map<String, String> nul = new java.util.HashMap<>();
        nul.put("Subject", null);
        assertThat(ConvertisseurHtmlGotenberg.json(nul)).isEqualTo("{\"Subject\":\"\"}");
    }

    @Test
    @DisplayName("POST /forms/chromium/convert/html : index.html, footer.html, papier A3 paysage en pouces, marges, fonds imprimés")
    void impression() {
        assertThat(adaptateur().imprimer("<html>Organigramme é</html>", "<p>pied</p>", A3_PAYSAGE)).isEqualTo(PDF);
        assertThat(chemin.get()).isEqualTo("POST /forms/chromium/convert/html");
        assertThat(corps.get())
            .contains("name=\"files\"; filename=\"index.html\"")
            .contains("<html>Organigramme é</html>")
            .contains("name=\"files\"; filename=\"footer.html\"")
            .contains("name=\"paperWidth\"")
            .contains("16.5354")
            .contains("11.6929")
            .contains("0.4724")
            .contains("0.6299")
            .contains("name=\"printBackground\"");
        adaptateur().imprimer("<html></html>", null, A3_PAYSAGE);
        assertThat(corps.get()).doesNotContain("footer.html");
        assertThat(ConvertisseurHtmlGotenberg.pouces(25.4)).isEqualTo("1.0000");
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 422 })
    @DisplayName("Document refusé par le moteur")
    void refus(int code) {
        statut = code;
        assertThatThrownBy(() -> adaptateur().imprimer("<html></html>", null, A3_PAYSAGE)).isInstanceOf(ConversionRefusee.class);
    }

    @ParameterizedTest
    @ValueSource(ints = { 404, 429, 500, 503 })
    @DisplayName("Route désactivée, file pleine, panne : moteur indisponible")
    void indisponible(int code) {
        statut = code;
        assertThatThrownBy(() -> adaptateur().imprimer("<html></html>", null, A3_PAYSAGE)).isInstanceOf(MoteurIndisponible.class);
    }

    @Test
    @DisplayName("Connexion impossible : moteur indisponible")
    void injoignable() throws IOException {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        assertThatThrownBy(() -> adaptateur("http://127.0.0.1:" + port).imprimer("<html></html>", null, A3_PAYSAGE)).isInstanceOf(MoteurIndisponible.class);
    }
}
