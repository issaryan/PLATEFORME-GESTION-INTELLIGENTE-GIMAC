package cm.gimac.des.iam.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.application.port.out.sandbox.RegistreInstitutions.RegistreIndisponible;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Correspondance du registre lue dans MS-PARTICIPANT (D-134), contre une fausse route interne (serveur HTTP du JDK). */
class RegistreInstitutionsParticipantTest {

    static final UUID ID = UUID.fromString("6f1c2a3e-4444-4a6b-9c1d-000000000042");

    HttpServer serveur;
    final Map<String, Object[]> reponses = new ConcurrentHashMap<>();
    String base;

    @BeforeEach
    void demarrer() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", this::repondre);
        serveur.start();
        base = "http://127.0.0.1:" + serveur.getAddress().getPort();
    }

    @AfterEach
    void arreter() {
        serveur.stop(0);
    }

    void repondre(HttpExchange e) throws IOException {
        Object[] r = reponses.getOrDefault(e.getRequestURI().getPath(), new Object[] { 404, "" });
        byte[] b = ((String) r[1]).getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json");
        e.sendResponseHeaders((int) r[0], b.length == 0 ? -1 : b.length);
        if (b.length > 0) {
            e.getResponseBody().write(b);
        }
        e.close();
    }

    RegistreInstitutionsParticipant registre() {
        return new RegistreInstitutionsParticipant(RestClient.builder().baseUrl(base).build());
    }

    @Test
    void correspondanceDansLesDeuxSens() {
        reponses.put("/api/v1/interne/institutions/" + ID + "/correspondance", new Object[] { 200, "{\"institutionId\":\"" + ID + "\",\"codeParticipant\":\"99002\"}" });
        reponses.put("/api/v1/interne/institutions/code/99002/correspondance", new Object[] { 200, "{\"institutionId\":\"" + ID + "\",\"codeParticipant\":\"99002\"}" });
        assertThat(registre().codeParticipant(ID)).contains("99002");
        assertThat(registre().identifiant("99002")).contains(ID);
        // Institution inconnue (404) ou sans code : vide.
        assertThat(registre().codeParticipant(UUID.randomUUID())).isEmpty();
        assertThat(registre().identifiant("99999")).isEmpty();
        reponses.put("/api/v1/interne/institutions/" + ID + "/correspondance", new Object[] { 200, "{\"institutionId\":\"" + ID + "\",\"codeParticipant\":null}" });
        assertThat(registre().codeParticipant(ID)).isEmpty();
    }

    @Test
    void registreEnErreurOuInjoignable() {
        reponses.put("/api/v1/interne/institutions/" + ID + "/correspondance", new Object[] { 403, "" });
        assertThatThrownBy(() -> registre().codeParticipant(ID)).isInstanceOf(RegistreIndisponible.class).hasMessageContaining("403");
        reponses.put("/api/v1/interne/institutions/" + ID + "/correspondance", new Object[] { 503, "" });
        assertThatThrownBy(() -> registre().codeParticipant(ID)).isInstanceOf(RegistreIndisponible.class);
        serveur.stop(0);
        assertThatThrownBy(() -> registre().codeParticipant(ID)).isInstanceOf(RegistreIndisponible.class).hasMessageContaining("injoignable");
        assertThat(new RegistreInstitutionsParticipant(RestClient.builder(), new ServiceTokenInterceptor(null), base)).isNotNull();
        assertThat(new RegistreInstitutionsSansRegistre().codeParticipant(ID)).isEmpty();
        assertThat(new RegistreInstitutionsSansRegistre().identifiant("99002")).isEmpty();
    }
}
