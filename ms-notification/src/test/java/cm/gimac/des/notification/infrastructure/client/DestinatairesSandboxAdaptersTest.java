package cm.gimac.des.notification.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.notification.domain.exception.NotificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Destinataires de la sandbox (US-EXI-09) : MS-IAM réel (cache, 404, panne) et substitution. */
class DestinatairesSandboxAdaptersTest {

    static final String IAM = "http://iam.test";

    @Test
    @DisplayName("Institution : comptes actifs de l'institution et leurs profils, cache ; porteurs d'une permission ; 404 vide ; panne 503")
    void reel() {
        RestClient.Builder builder = RestClient.builder().baseUrl(IAM);
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/collaborateurs?institution=99001"))
            .andRespond(
                withSuccess(
                    "[{\"id\":\"a\",\"institutionId\":\"99001\",\"actif\":true,\"profils\":[\"ROLE_SBX_ADMIN_INSTITUTION\"]}," +
                    "{\"id\":\"b\",\"institutionId\":\"99001\",\"actif\":false,\"profils\":[]},{\"id\":\"c\",\"institutionId\":\"99002\",\"actif\":true}]",
                    MediaType.APPLICATION_JSON
                )
            );
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/collaborateurs?permission=SBX_HAB_ADMINISTRATION"))
            .andRespond(withSuccess("[{\"id\":\"g\",\"actif\":true,\"profils\":[\"ADMIN\"]}]", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(IAM + "/api/v1/interne/collaborateurs?institution=99003")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(IAM + "/api/v1/interne/collaborateurs?institution=99004")).andRespond(withServerError());
        IamDestinatairesSandboxAdapter a = new IamDestinatairesSandboxAdapter(builder.build(), 60);
        assertThat(a.comptesInstitution(" 99001 ")).containsOnlyKeys("a").containsEntry("a", Set.of("ROLE_SBX_ADMIN_INSTITUTION"));
        assertThat(a.comptesInstitution("99001")).containsOnlyKeys("a");
        assertThat(a.porteurs("SBX_HAB_ADMINISTRATION")).containsExactly("g");
        assertThat(a.comptesInstitution("99003")).isEmpty();
        assertThat(a.comptesInstitution(null)).isEmpty();
        assertThat(a.comptesInstitution(" ")).isEmpty();
        assertThatThrownBy(() -> a.comptesInstitution("99004")).isInstanceOf(NotificationException.class).extracting("code").isEqualTo("NOT_ANNUAIRE_INDISPONIBLE");
        serveur.verify();
    }

    @Test
    @DisplayName("Construction par Spring : URL de MS-IAM et jeton de service")
    void construction() throws NoSuchMethodException {
        assertThat(new IamDestinatairesSandboxAdapter(RestClient.builder(), org.mockito.Mockito.mock(ServiceTokenInterceptor.class), IAM, 60)).isNotNull();
        assertThat(
            IamDestinatairesSandboxAdapter.class.getConstructor(RestClient.Builder.class, ServiceTokenInterceptor.class, String.class, long.class).getAnnotation(org.springframework.beans.factory.annotation.Autowired.class)
        ).isNotNull();
    }

    @Test
    @DisplayName("Réponse en page ou vide, compte sans identifiant ignoré")
    void profils() throws Exception {
        ObjectMapper json = new ObjectMapper();
        assertThat(IamDestinatairesSandboxAdapter.profils(json.readTree("{\"contenu\":[{\"id\":\"x\",\"actif\":true},{\"id\":\"\",\"actif\":true}]}"), null)).containsOnlyKeys("x");
        assertThat(IamDestinatairesSandboxAdapter.profils(json.readTree("{\"total\":0}"), null)).isEmpty();
        assertThat(IamDestinatairesSandboxAdapter.profils(null, "99001")).isEmpty();
    }

    @Test
    @DisplayName("Substitution : institution 99001 et porteurs de démonstration, panne simulée, réinitialisation")
    void substitution() {
        DestinatairesSandboxStubAdapter s = new DestinatairesSandboxStubAdapter();
        assertThat(s.comptesInstitution("99001")).containsKeys(DestinatairesSandboxStubAdapter.ADMIN_99001, DestinatairesSandboxStubAdapter.CHEF_99001);
        assertThat(s.comptesInstitution(null)).isEmpty();
        assertThat(s.comptesInstitution("00000")).isEmpty();
        assertThat(s.porteurs("SBX_LEC_ADMINISTRATION")).contains(DestinatairesSandboxStubAdapter.RSSI);
        assertThat(s.porteurs("AUTRE")).isEmpty();
        s.rendreIndisponible(true);
        assertThatThrownBy(() -> s.porteurs("SBX_HAB_ADMINISTRATION")).isInstanceOf(NotificationException.class);
        assertThatThrownBy(() -> s.comptesInstitution("99001")).isInstanceOf(NotificationException.class);
        s.reinitialiser();
        assertThat(s.porteurs("SBX_HAB_ADMINISTRATION")).containsExactly(DestinatairesSandboxStubAdapter.ADMIN_GIMAC);
    }
}
