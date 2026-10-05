package cm.gimac.des.homologation.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort.Designation;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort.Origine;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort.Resultat;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort.Tache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Tâches de MS-PILOTAGE (D-77, §5) : adaptateur réel (jeton de service) et de substitution. */
class AdaptateursPilotageTest {

    static final Origine ORIGINE = new Origine("OPERATION", "LIA-2026-001/17/R", "homologation", "/des/parcours/x");
    static final Tache TACHE = new Tache("Constat (LIA-2026-001)", "R", "2026-10-10", "NORMALE", "00000000-0000-4000-8000-000000000001", null, ORIGINE);
    static final Designation DESIGNATION = new Designation(ORIGINE, TACHE.responsableId());

    @Test
    @DisplayName("Réel : création, clôture, réouverture ; refus 4xx définitif ; 401, 429, 5xx et panne réessayés")
    void reel() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        PilotageTachesRestAdapter a = new PilotageTachesRestAdapter(b, AdaptateursCadrageTest.jeton(), "http://pilotage");
        serveur
            .expect(requestTo("http://pilotage/api/v1/interne/taches"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer jeton"))
            .andExpect(content().json("{\"titre\":\"Constat (LIA-2026-001)\",\"origine\":{\"type\":\"OPERATION\",\"reference\":\"LIA-2026-001/17/R\"}}"))
            .andRespond(withStatus(HttpStatus.CREATED));
        serveur.expect(requestTo("http://pilotage/api/v1/interne/taches/cloture")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo("http://pilotage/api/v1/interne/taches/reouverture")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo("http://pilotage/api/v1/interne/taches")).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        serveur.expect(requestTo("http://pilotage/api/v1/interne/taches")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        serveur.expect(requestTo("http://pilotage/api/v1/interne/taches")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        serveur.expect(requestTo("http://pilotage/api/v1/interne/taches/cloture")).andRespond(withServerError());
        assertThat(a.creer(TACHE)).isEqualTo(Resultat.FAIT);
        assertThat(a.cloturer(DESIGNATION)).isEqualTo(Resultat.FAIT);
        assertThat(a.rouvrir(DESIGNATION)).isEqualTo(Resultat.FAIT);
        assertThat(a.creer(TACHE)).isEqualTo(Resultat.REFUSE);
        assertThat(a.creer(TACHE)).isEqualTo(Resultat.INDISPONIBLE);
        assertThat(a.creer(TACHE)).isEqualTo(Resultat.INDISPONIBLE);
        assertThat(a.cloturer(DESIGNATION)).isEqualTo(Resultat.INDISPONIBLE);
        serveur.verify();
    }

    @Test
    @DisplayName("Substitution : idempotence par origine et responsable, clôture, réouverture, indisponibilité simulée")
    void substitution() {
        PilotageTachesStubAdapter s = new PilotageTachesStubAdapter();
        assertThat(s.creer(TACHE)).isEqualTo(Resultat.FAIT);
        assertThat(s.creer(TACHE)).isEqualTo(Resultat.FAIT);
        assertThat(s.taches()).hasSize(1);
        assertThat(s.cloturer(DESIGNATION)).isEqualTo(Resultat.FAIT);
        assertThat(s.taches().values()).singleElement().extracting(PilotageTachesStubAdapter.TacheTenue::etat).isEqualTo("TERMINEE");
        assertThat(s.rouvrir(DESIGNATION)).isEqualTo(Resultat.FAIT);
        assertThat(s.taches().values()).singleElement().extracting(PilotageTachesStubAdapter.TacheTenue::etat).isEqualTo("EN_COURS");
        assertThat(s.journal()).containsExactly(
            "CREER LIA-2026-001/17/R|" + TACHE.responsableId(),
            "CREER LIA-2026-001/17/R|" + TACHE.responsableId(),
            "CLOTURER LIA-2026-001/17/R|" + TACHE.responsableId(),
            "ROUVRIR LIA-2026-001/17/R|" + TACHE.responsableId()
        );
        s.indisponible(true);
        assertThat(s.creer(TACHE)).isEqualTo(Resultat.INDISPONIBLE);
        assertThat(s.cloturer(DESIGNATION)).isEqualTo(Resultat.INDISPONIBLE);
        s.indisponible(false);
        s.vider();
        assertThat(s.taches()).isEmpty();
        assertThat(s.journal()).isEmpty();
        PilotageTachesPort p = s;
        assertThat(p.rouvrir(DESIGNATION)).isEqualTo(Resultat.FAIT);
    }
}
