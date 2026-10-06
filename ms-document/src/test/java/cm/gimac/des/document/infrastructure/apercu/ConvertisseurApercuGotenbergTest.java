package cm.gimac.des.document.infrastructure.apercu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.application.port.out.ConvertisseurApercu.ConversionRefusee;
import cm.gimac.des.document.application.port.out.ConvertisseurApercu.MoteurIndisponible;
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
 * Adaptateur Gotenberg contre un faux moteur HTTP local (serveur HTTP du JDK) : requête multipart attendue
 * par POST /forms/libreoffice/convert, PDF rendu, et traduction des réponses d'erreur, du délai dépassé et
 * de la connexion impossible.
 */
class ConvertisseurApercuGotenbergTest {

    static final byte[] PDF = "%PDF-1.7 converti".getBytes(StandardCharsets.US_ASCII);

    HttpServer serveur;
    final AtomicReference<String> methode = new AtomicReference<>();
    final AtomicReference<String> chemin = new AtomicReference<>();
    final AtomicReference<String> typeContenu = new AtomicReference<>();
    final AtomicReference<String> corps = new AtomicReference<>();
    volatile int statut = 200;
    volatile byte[] reponse = PDF;
    volatile long attenteMs = 0;

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
        methode.set(e.getRequestMethod());
        chemin.set(e.getRequestURI().getPath());
        typeContenu.set(e.getRequestHeaders().getFirst("Content-Type"));
        corps.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
        if (attenteMs > 0) {
            try {
                Thread.sleep(attenteMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        e.sendResponseHeaders(statut, reponse.length);
        e.getResponseBody().write(reponse);
        e.close();
    }

    ConvertisseurApercuGotenberg adaptateur(String url, Duration delai) {
        return new ConvertisseurApercuGotenberg(new ApercuProprietes(url, delai, Duration.ofSeconds(2), null, null, null, null));
    }

    ConvertisseurApercuGotenberg adaptateur() {
        return adaptateur("http://127.0.0.1:" + serveur.getAddress().getPort(), Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("POST multipart sur /forms/libreoffice/convert, partie files nommée document.<extension>, PDF rendu")
    void conversion() {
        byte[] source = { 'P', 'K', 3, 4, 'x', 'y' };
        assertThat(adaptateur().convertirEnPdf("pptx", source)).isEqualTo(PDF);
        assertThat(methode.get()).isEqualTo("POST");
        assertThat(chemin.get()).isEqualTo("/forms/libreoffice/convert");
        assertThat(typeContenu.get()).startsWith("multipart/form-data");
        assertThat(corps.get())
            .contains("Content-Disposition: form-data; name=\"files\"; filename=\"document.pptx\"")
            .contains("PK\u0003\u0004xy");
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 422, 500 })
    @DisplayName("Fichier protégé par mot de passe (400) ou que LibreOffice ne peut ouvrir (500) : conversion refusée")
    void refus(int code) {
        statut = code;
        reponse = "The document is password-protected".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> adaptateur().convertirEnPdf("docx", new byte[] { 1 })).isInstanceOf(ConversionRefusee.class);
    }

    @ParameterizedTest
    @ValueSource(ints = { 429, 502, 503, 504, 404 })
    @DisplayName("File d'attente pleine, délai du moteur dépassé, route absente : moteur indisponible")
    void indisponible(int code) {
        statut = code;
        reponse = "indisponible".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> adaptateur().convertirEnPdf("docx", new byte[] { 1 })).isInstanceOf(MoteurIndisponible.class);
    }

    @Test
    @DisplayName("Délai de réponse dépassé : moteur indisponible")
    void delai() {
        attenteMs = 1500;
        ConvertisseurApercuGotenberg a = adaptateur("http://127.0.0.1:" + serveur.getAddress().getPort(), Duration.ofMillis(300));
        assertThatThrownBy(() -> a.convertirEnPdf("docx", new byte[] { 1 })).isInstanceOf(MoteurIndisponible.class);
    }

    @Test
    @DisplayName("Connexion impossible : moteur indisponible")
    void injoignable() throws IOException {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        ConvertisseurApercuGotenberg a = adaptateur("http://127.0.0.1:" + port, Duration.ofSeconds(1));
        assertThatThrownBy(() -> a.convertirEnPdf("docx", new byte[] { 1 })).isInstanceOf(MoteurIndisponible.class);
    }
}
