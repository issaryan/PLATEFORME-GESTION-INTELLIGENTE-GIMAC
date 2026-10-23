package cm.gimac.des.homologation.infrastructure.client.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.HorsContrat;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Indisponible;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.PageExecutions;
import cm.gimac.des.homologation.contrat.SchemaContrat;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat de lecture v1 de SBX-LECTURE (L04, US-EXI-14) : les réponses du substitut respectent les schémas publiés
 * (contrats/sandbox/sbx-lecture.v1.*), et l'adaptateur réel, qui relit ces mêmes réponses par HTTP, rend exactement ce
 * que rend le substitut. Le port ne comporte que des lectures.
 */
class ContratLectureSandboxTest {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final UUID LIAISON = UUID.fromString("0192f0aa-0000-7000-8000-00000000a001");
    static final String C = "0192f0aa-0000-7000-8000-0000000c0001";

    LectureSandboxStubAdapter stub;
    MockRestServiceServer serveur;
    LectureSandboxRestAdapter reel;

    @BeforeEach
    void init() {
        stub = new LectureSandboxStubAdapter();
        stub
            .alimenter(LIAISON, C, "99001", "MOB", List.of("RETRAIT", "TRANSFERT"))
            .executer("EXE-000001", "SCN-MOB-001", "PASSED", "favorable", "2026-10-13T16:52:07Z")
            .executer("EXE-000002", "SCN-MOB-002", "FAILED", "defavorable", "2026-10-13T16:53:07Z")
            .taux("RETRAIT", "REUSSITE", 1, 1, "100.00", "2026-10-13T17:00:00Z")
            .taux(null, "AVANCEMENT", 2, 2, "100.00", "2026-10-13T17:00:00Z")
            .etat("TERMINEE", 1)
            .piece("BILAN", 1, "{\"bilan\":1}");
        stub.alimenter(LIAISON, "0192f0aa-0000-7000-8000-0000000c0002", "99001", "CAR", List.of("RETRAIT")).signaler("COLLISION_CODE_PARTICIPANT");
        RestClient.Builder b = RestClient.builder().baseUrl("http://sbx-lecture");
        serveur = MockRestServiceServer.bindTo(b).build();
        reel = new LectureSandboxRestAdapter(b.build());
    }

    static SchemaContrat schema(String nom) {
        return SchemaContrat.charger("sandbox/sbx-lecture.v1." + nom + ".schema.json");
    }

    void servir(String url, byte[] corps) {
        serveur.expect(requestTo(url)).andExpect(method(HttpMethod.GET)).andRespond(withSuccess(corps, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Contrat : réponses du substitut conformes aux schémas publiés (campagnes, campagne, page, pièces)")
    void schemas() {
        assertThat(schema("campagnes").ecarts(JSON.readTree(stub.jsonCampagnes(LIAISON)))).isEmpty();
        assertThat(schema("campagne").ecarts(JSON.readTree(stub.jsonCampagne(C)))).isEmpty();
        assertThat(schema("executions").ecarts(JSON.readTree(stub.jsonExecutions(C, null, 3)))).isEmpty();
        assertThat(schema("pieces").ecarts(JSON.readTree(stub.jsonPieces(C)))).isEmpty();
    }

    @Test
    @DisplayName("Client contre le contrat : l'adaptateur réel rend exactement ce que rend le substitut")
    void clientContreLeContrat() {
        servir("http://sbx-lecture/v1/liaisons/" + LIAISON + "/campagnes", stub.jsonCampagnes(LIAISON));
        servir("http://sbx-lecture/v1/campagnes/" + C, stub.jsonCampagne(C));
        servir("http://sbx-lecture/v1/campagnes/" + C + "/executions?taille=3", stub.jsonExecutions(C, null, 3));
        String suivant = stub.executions(C, null, 3).positionSuivante();
        servir("http://sbx-lecture/v1/campagnes/" + C + "/executions?taille=3&depuis=" + suivant, stub.jsonExecutions(C, suivant, 3));
        servir("http://sbx-lecture/v1/campagnes/" + C + "/pieces", stub.jsonPieces(C));
        String hex = stub.pieces(C).get(0).empreinte();
        HttpHeaders h = new HttpHeaders();
        h.set("Repr-Digest", TraducteurLectureSandbox.reprDigest(hex));
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C + "/pieces/BILAN/1")).andExpect(method(HttpMethod.HEAD)).andRespond(withSuccess().headers(h));
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C + "/pieces/BILAN/1")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"bilan\":1}", MediaType.APPLICATION_JSON));

        assertThat(reel.campagnes(LIAISON)).isEqualTo(stub.campagnes(LIAISON));
        assertThat(reel.campagne(C)).isEqualTo(stub.campagne(C));
        PageExecutions p1 = reel.executions(C, null, 3);
        assertThat(p1).isEqualTo(stub.executions(C, null, 3));
        assertThat(reel.executions(C, suivant, 3)).isEqualTo(stub.executions(C, suivant, 3));
        assertThat(reel.pieces(C)).isEqualTo(stub.pieces(C));
        assertThat(reel.empreinte(C, "BILAN", 1)).isEqualTo(stub.empreinte(C, "BILAN", 1)).isEqualTo(hex);
        assertThat(new String(reel.telecharger(C, "BILAN", 1), StandardCharsets.UTF_8)).isEqualTo("{\"bilan\":1}");
        serveur.verify();

        // Annexe D : création PENDING sans verdict normalisé, puis validation ; page pleine close, dernière non close.
        assertThat(p1.close()).isTrue();
        assertThat(p1.enregistrements().get(0).verdictNatif()).isEqualTo("PENDING");
        assertThat(p1.enregistrements().get(0).verdictNormalise()).isNull();
        assertThat(p1.enregistrements().get(1).verdictNormalise()).isEqualTo("favorable");
        assertThat(stub.executions(C, suivant, 3).close()).isFalse();
    }

    @Test
    @DisplayName("Indisponibilité : 503 et 429 avec Retry-After, sans en-tête ou injoignable, délai par défaut")
    void indisponibilite() {
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.RETRY_AFTER, "120");
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).headers(h));
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C)).andRespond(r -> {
            throw new java.io.IOException("connexion refusée");
        });
        assertThatThrownBy(() -> reel.campagne(C)).isInstanceOfSatisfying(Indisponible.class, e -> assertThat(e.delai()).isEqualTo(Duration.ofSeconds(120)));
        assertThatThrownBy(() -> reel.campagne(C)).isInstanceOfSatisfying(Indisponible.class, e -> assertThat(e.delai()).isEqualTo(LectureSandboxRestAdapter.DELAI_PAR_DEFAUT));
        assertThatThrownBy(() -> reel.campagne(C)).isInstanceOfSatisfying(Indisponible.class, e -> assertThat(e.delai()).isEqualTo(LectureSandboxRestAdapter.DELAI_PAR_DEFAUT));
        HttpHeaders date = new HttpHeaders();
        date.set(HttpHeaders.RETRY_AFTER, "Wed, 21 Oct 2026 07:28:00 GMT");
        assertThat(LectureSandboxRestAdapter.delai(date)).isEqualTo(LectureSandboxRestAdapter.DELAI_PAR_DEFAUT);
        assertThat(LectureSandboxRestAdapter.delai(null)).isEqualTo(LectureSandboxRestAdapter.DELAI_PAR_DEFAUT);
    }

    @Test
    @DisplayName("Hors contrat : 400 (jeton), 404 (hors périmètre), 409 (collision), réponse vide ou illisible")
    void horsContrat() {
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C + "/executions?taille=40&depuis=x")).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C)).andRespond(withStatus(HttpStatus.CONFLICT));
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C + "/pieces")).andRespond(withSuccess());
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C + "/pieces")).andRespond(withSuccess("pas du json", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo("http://sbx-lecture/v1/campagnes/" + C + "/pieces/BILAN/1")).andExpect(method(HttpMethod.HEAD)).andRespond(withSuccess());
        assertThatThrownBy(() -> reel.executions(C, "x", 40)).isInstanceOf(HorsContrat.class).hasMessageContaining("400");
        assertThatThrownBy(() -> reel.campagne(C)).isInstanceOf(HorsContrat.class).hasMessageContaining("404");
        assertThatThrownBy(() -> reel.campagne(C)).isInstanceOf(HorsContrat.class).hasMessageContaining("409");
        assertThatThrownBy(() -> reel.pieces(C)).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> reel.pieces(C)).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> reel.empreinte(C, "BILAN", 1)).isInstanceOf(HorsContrat.class).hasMessageContaining("Repr-Digest");
    }

    @Test
    @DisplayName("Revue : le port de lecture ne comporte que des lectures, aucune écriture vers la sandbox")
    void portEnLectureSeule() {
        Set<String> lectures = Set.of("campagnes", "campagne", "executions", "pieces", "empreinte", "telecharger");
        assertThat(Arrays.stream(LectureSandboxPort.class.getDeclaredMethods()).map(Method::getName)).allMatch(lectures::contains);
    }
}
