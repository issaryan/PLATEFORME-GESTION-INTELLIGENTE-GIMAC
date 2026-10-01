package cm.gimac.des.certification.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Lecture réelle de la fiche de changement (défaut trouvé sur la pile réelle : le substitut, actif en production,
 * lisait « approuvée » toute référence). Contrat du simulacre infra/docker-compose/simulacres/changements.
 */
class FicheChangementRestAdapterTest {

    RestClient.Builder builder;
    MockRestServiceServer serveur;
    FicheChangementRestAdapter adaptateur;

    @BeforeEach
    void init() {
        builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).build();
        adaptateur = new FicheChangementRestAdapter(builder, "http://changements");
    }

    static void refus(Runnable appel, int status, String code) {
        assertThatThrownBy(appel::run).isInstanceOfSatisfying(CertificationException.class, e -> {
            assertThat(e.status()).isEqualTo(status);
            assertThat(e.code()).isEqualTo(code);
        });
    }

    @Test
    @DisplayName("Fiche approuvée par le comité : lue APPROUVEE ; inconnue : 422 ; dispositif injoignable : 503")
    void lecture() {
        // Attentes déclarées d'avance (MockRestServiceServer les refuse après la première requête), dans l'ordre des appels.
        serveur
            .expect(requestTo("http://changements/api/v1/changements/CHG-2026-0412"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"reference\":\"CHG-2026-0412\",\"etat\":\"APPROUVE\",\"comite\":\"CAB-2026-38\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo("http://changements/api/v1/changements/CHG-2026-9999")).andRespond(withResourceNotFound());
        serveur.expect(requestTo("http://changements/api/v1/changements/CHG-2026-0001")).andRespond(withServerError());
        serveur.expect(requestTo("http://changements/api/v1/changements/CHG-2026-0002")).andRespond(withSuccess("{\"reference\":\"CHG-2026-0002\"}", MediaType.APPLICATION_JSON));

        assertThat(adaptateur.lire("CHG-2026-0412")).isEqualTo(EtatFicheChangement.APPROUVEE);
        refus(() -> adaptateur.lire("CHG-2026-9999"), 422, "CER_CHANGE_RECORD_UNKNOWN");
        refus(() -> adaptateur.lire("CHG-2026-0001"), 503, "CER_CHANGE_RECORD_UNAVAILABLE");
        refus(() -> adaptateur.lire("CHG-2026-0002"), 503, "CER_CHANGE_RECORD_UNAVAILABLE");
        serveur.verify();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({ "APPROUVE, APPROUVEE", "approuvée, APPROUVEE", "REFUSE, REFUSEE", "REJETE, REFUSEE", "EN_EXAMEN, EN_EXAMEN", "OUVERT, OUVERTE" })
    void etats(String brut, EtatFicheChangement attendu) {
        assertThat(FicheChangementRestAdapter.etat(brut)).isEqualTo(attendu);
    }

    @Test
    void etatInconnu() {
        refus(() -> FicheChangementRestAdapter.etat("ARCHIVE"), 503, "CER_CHANGE_RECORD_UNAVAILABLE");
    }
}
