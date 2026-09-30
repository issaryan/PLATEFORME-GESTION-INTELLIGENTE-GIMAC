package cm.gimac.des.homologation.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort.Verification;
import cm.gimac.des.homologation.domain.HomologationException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Adaptateurs du parcours : registre documentaire (MS-DOCUMENT) et réauthentification forte (MS-IAM), réels et de substitution. */
class AdaptateursParcoursTest {

    static final UUID ID = UUID.randomUUID();
    static final String SHA = "c".repeat(64);

    static void code(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(HomologationException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    @Test
    @DisplayName("Registre documentaire réel : empreinte conforme, divergente, pièce inconnue ; panne : indisponible")
    void documentReelVerification() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        DocumentRestAdapter a = new DocumentRestAdapter(b, AdaptateursCadrageTest.jeton(), "http://document");
        String url = "http://document/api/v1/interne/documents/" + ID + "/empreinte";
        serveur.expect(requestTo(url)).andExpect(header("Authorization", "Bearer jeton")).andRespond(withSuccess("{\"sha256\":\"" + SHA.toUpperCase() + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{\"sha256\":\"" + "d".repeat(64) + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.FORBIDDEN));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        assertThat(a.verifier(ID, SHA)).isEqualTo(Verification.CONFORME);
        assertThat(a.verifier(ID, SHA)).isEqualTo(Verification.DIVERGENTE);
        assertThat(a.verifier(ID, SHA)).isEqualTo(Verification.INCONNUE);
        assertThat(a.verifier(ID, SHA)).isEqualTo(Verification.INCONNUE);
        code(() -> a.verifier(ID, SHA), "HOM_DOCUMENT_UNAVAILABLE", 503);
        code(() -> a.verifier(ID, SHA), "HOM_DOCUMENT_UNAVAILABLE", 503);
        serveur.verify();
    }

    @Test
    @DisplayName("Registre documentaire réel : dépôt pour le compte du correspondant ; refus 4xx ; panne")
    void documentReelDepot() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        DocumentRestAdapter a = new DocumentRestAdapter(b, AdaptateursCadrageTest.jeton(), "http://document");
        String url = "http://document/api/v1/interne/documents";
        serveur
            .expect(requestTo(url))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
            .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"sha256\":\"" + SHA + "\",\"nomFichier\":\"cr.pdf\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{\"id\":\"" + ID + "\",\"sha256\":\"" + SHA + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.valueOf(422)));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        byte[] contenu = "pdf".getBytes(StandardCharsets.UTF_8);
        var d = a.deposer(contenu, "cr.pdf", "application/pdf", "LIVRABLE", "op", UUID.randomUUID());
        assertThat(d.id()).isEqualTo(ID);
        assertThat(d.sha256()).isEqualTo(SHA);
        assertThat(a.deposer(contenu, "autre.pdf", "application/pdf", "LIVRABLE", "op", null).nomFichier()).isEqualTo("autre.pdf");
        code(() -> a.deposer(contenu, "x.pdf", null, "LIVRABLE", "op", null), "HOM_DOCUMENT_UNAVAILABLE", 503);
        code(() -> a.deposer(contenu, "x.pdf", null, "LIVRABLE", "op", null), "HOM_DOCUMENT_REFUSED", 422);
        code(() -> a.deposer(contenu, "x.pdf", null, "LIVRABLE", "op", null), "HOM_DOCUMENT_UNAVAILABLE", 503);
        serveur.verify();
    }

    @Test
    @DisplayName("Registre de substitution : pièces déposées retenues, pièce retirée inconnue, empreinte mal formée divergente")
    void documentSubstitution() {
        DocumentStubAdapter a = new DocumentStubAdapter();
        assertThat(a.verifier(ID, SHA)).isEqualTo(Verification.CONFORME);
        assertThat(a.verifier(ID, "zz")).isEqualTo(Verification.DIVERGENTE);
        assertThat(a.verifier(ID, null)).isEqualTo(Verification.DIVERGENTE);
        var d = a.deposer("abc".getBytes(StandardCharsets.UTF_8), "a.pdf", "application/pdf", "LIVRABLE", "r", null);
        assertThat(d.sha256()).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(a.verifier(d.id(), d.sha256().toUpperCase())).isEqualTo(Verification.CONFORME);
        assertThat(a.verifier(d.id(), SHA)).isEqualTo(Verification.DIVERGENTE);
        a.retirer(ID);
        assertThat(a.verifier(ID, SHA)).isEqualTo(Verification.INCONNUE);
    }

    @Test
    @DisplayName("Réauthentification réelle : verdict de MS-IAM ; refus 4xx : faux ; panne : indisponible ; assertion vide sans appel")
    void reauthReelle() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        IamReauthAdapter a = new IamReauthAdapter(b, AdaptateursCadrageTest.jeton(), "http://iam");
        String url = "http://iam/api/v1/interne/reauthentifications/verification";
        serveur.expect(requestTo(url)).andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{\"valide\":true}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{\"valide\":false}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        UUID sujet = UUID.randomUUID();
        assertThat(a.verifier("a", sujet, "F")).isTrue();
        assertThat(a.verifier("a", sujet, "F")).isFalse();
        assertThat(a.verifier("a", sujet, "F")).isFalse();
        assertThat(a.verifier("a", sujet, "F")).isFalse();
        code(() -> a.verifier("a", sujet, "F"), "HOM_IAM_UNAVAILABLE", 503);
        assertThat(a.verifier(" ", sujet, "F")).isFalse();
        assertThat(a.verifier(null, sujet, "F")).isFalse();
        serveur.verify();
    }

    @Test
    @DisplayName("Réauthentification de substitution : toute assertion non vide, sauf le refus simulé")
    void reauthSubstitution() {
        ReauthStubAdapter a = new ReauthStubAdapter();
        assertThat(a.verifier("a", null, "F")).isTrue();
        assertThat(a.verifier(ReauthStubAdapter.ASSERTION_REFUSEE, null, "F")).isFalse();
        assertThat(a.verifier(" ", null, "F")).isFalse();
        assertThat(a.verifier(null, null, "F")).isFalse();
    }
}
