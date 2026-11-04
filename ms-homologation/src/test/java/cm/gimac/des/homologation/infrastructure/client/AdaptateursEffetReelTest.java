package cm.gimac.des.homologation.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Adaptateurs de D-93 et D-94 : porteurs d'une permission (MS-IAM), type d'une pièce (MS-DOCUMENT), substituts. */
class AdaptateursEffetReelTest {

    static final UUID ID = UUID.randomUUID();

    @Test
    @DisplayName("Annuaire réel par permission : liste ; réponse nulle : vide ; objet, 4xx ou 5xx : indisponible (aucun repli)")
    void parPermission() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        IamCollaboratorAdapter a = new IamCollaboratorAdapter(b, AdaptateursCadrageTest.jeton(), "http://iam");
        String url = "http://iam/api/v1/interne/collaborateurs?permission=HOM_PROJECT_CONDUCT";
        String json = "{\"id\":\"" + ID + "\",\"nomAffiche\":\"Aminata Ngono\",\"profils\":[\"ROLE_X\"],\"institutionId\":null,\"actif\":true}";
        serveur.expect(requestTo(url)).andRespond(withSuccess("[" + json + "]", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{\"contenu\":[],\"total\":0,\"page\":0,\"taille\":20}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        serveur.expect(requestTo(url)).andRespond(withSuccess("null", MediaType.APPLICATION_JSON));
        assertThat(a.parPermission("HOM_PROJECT_CONDUCT")).extracting(CollaboratorView::nomAffiche).containsExactly("Aminata Ngono");
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> a.parPermission("HOM_PROJECT_CONDUCT")).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_IAM_UNAVAILABLE"));
        }
        assertThat(a.parPermission("HOM_PROJECT_CONDUCT")).isEmpty();
        serveur.verify();
    }

    @Test
    @DisplayName("Registre documentaire réel : type MIME inscrit ; 404 : inconnu ; 5xx : indisponible")
    void typeMime() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        DocumentRestAdapter a = new DocumentRestAdapter(b, AdaptateursCadrageTest.jeton(), "http://document", "homologation-service");
        String url = "http://document/api/v1/interne/documents/" + ID;
        serveur.expect(requestTo(url)).andRespond(withSuccess("{\"id\":\"" + ID + "\",\"nomFichier\":\"cr.pdf\",\"typeMime\":\"application/pdf\",\"sha256\":\"a\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("null", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        assertThat(a.typeMime(ID)).isEqualTo("application/pdf");
        assertThat(a.typeMime(ID)).isNull();
        assertThat(a.typeMime(ID)).isNull();
        assertThatThrownBy(() -> a.typeMime(ID)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_DOCUMENT_UNAVAILABLE"));
        serveur.verify();
    }

    @Test
    @DisplayName("D-109 : contenu d'une pièce et son nom ; inconnue (404, réponse vide) : absente ; 4xx ou 5xx : indisponible")
    void lire() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        DocumentRestAdapter a = new DocumentRestAdapter(b, AdaptateursCadrageTest.jeton(), "http://document", "homologation-service");
        String meta = "http://document/api/v1/interne/documents/" + ID;
        String contenu = meta + "/contenu";
        String json = "{\"id\":\"" + ID + "\",\"nomFichier\":\"ping.png\",\"typeMime\":\"image/png\",\"sha256\":\"a\"}";
        serveur.expect(requestTo(meta)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(contenu)).andRespond(withSuccess("png".getBytes(), MediaType.IMAGE_PNG));
        serveur.expect(requestTo(meta)).andRespond(withSuccess("null", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(contenu)).andRespond(withSuccess("x".getBytes(), MediaType.IMAGE_PNG));
        serveur.expect(requestTo(meta)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(meta)).andRespond(withStatus(HttpStatus.FORBIDDEN));
        serveur.expect(requestTo(meta)).andRespond(withServerError());
        assertThat(a.lire(ID)).hasValueSatisfying(c -> {
            assertThat(c.nomFichier()).isEqualTo("ping.png");
            assertThat(c.typeMime()).isEqualTo("image/png");
            assertThat(new String(c.octets())).isEqualTo("png");
        });
        assertThat(a.lire(ID)).isEmpty();
        assertThat(a.lire(ID)).isEmpty();
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> a.lire(ID)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_DOCUMENT_UNAVAILABLE"));
        }
        serveur.verify();
    }

    @Test
    @DisplayName("Substituts : composition des profils en permissions modifiable ; type d'une pièce déposée ou PDF par défaut")
    void substituts() {
        CollaboratorStubAdapter c = new CollaboratorStubAdapter();
        assertThat(c.parPermission("HOM_PROJECT_CONDUCT")).extracting(CollaboratorView::nomAffiche).containsExactly("Aminata Ngono", "Paul Essomba");
        c.enregistrer(new CollaboratorView(ID, "Profil créé", Set.of("ROLE_CREE"), null, true));
        assertThat(c.parPermission("HOM_PROJECT_CONDUCT")).hasSize(2);
        c.composer("ROLE_CREE", Set.of("HOM_PROJECT_CONDUCT"));
        assertThat(c.parPermission("HOM_PROJECT_CONDUCT")).hasSize(3);
        c.reinitialiserCompositions();
        assertThat(c.parPermission("HOM_PROJECT_CONDUCT")).hasSize(2);
        DocumentStubAdapter d = new DocumentStubAdapter();
        assertThat(d.typeMime(ID)).isEqualTo("application/pdf");
        var depot = d.deposer("x".getBytes(), "x.png", "image/png", "LIVRABLE", "r", null);
        assertThat(d.typeMime(depot.id())).isEqualTo("image/png");
        assertThat(d.typeMime(d.deposer("y".getBytes(), "y", null, "LIVRABLE", "r", null).id())).isEqualTo("application/pdf");
    }
}
