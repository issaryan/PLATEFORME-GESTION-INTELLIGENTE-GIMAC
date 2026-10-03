package cm.gimac.des.collaboration.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.ContenuDocument;
import cm.gimac.des.collaboration.application.port.out.DocumentStoragePort.DocumentDepose;
import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.messagerie.Collaborateur;
import cm.gimac.des.collaboration.domain.messagerie.FichierJoint;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Adaptateurs de la messagerie : annuaire de MS-IAM et dépôt de MS-DOCUMENT, réels et de substitution. */
class AdaptateursMessagerieTest {

    static final String ID = "8d1c1a4e-0000-4000-8000-000000000001";

    static ServiceTokenInterceptor jeton() {
        OAuth2AuthorizedClientManager m = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration r = ClientRegistration.withRegistrationId("service-token")
            .clientId("collaboration-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://idp/token")
            .build();
        when(m.authorize(any())).thenReturn(
            new OAuth2AuthorizedClient(r, "service", new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton", Instant.now(), Instant.now().plusSeconds(60)))
        );
        return new ServiceTokenInterceptor(m);
    }

    static void erreur(ThrowingCallable appel, String code, int statut) {
        assertThatThrownBy(appel).isInstanceOfSatisfying(CollaborationException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    static FichierJoint fichier(String contenu) {
        byte[] octets = contenu.getBytes(StandardCharsets.UTF_8);
        return new FichierJoint("pv de recette.pdf", "application/pdf", octets.length, () -> new ByteArrayInputStream(octets));
    }

    @Test
    @DisplayName("Annuaire réel : recherche paginée et fiche avec le jeton de service ; 404 : absent ; panne : 503")
    void annuaireReel() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        IamCollaboratorAdapter a = new IamCollaboratorAdapter(b, jeton(), "http://iam");
        String json =
            "{\"id\":\"" + ID + "\",\"nomAffiche\":\"Aminata Ngono\",\"profils\":[\"ROLE_CONDUCTEUR_PROJET\"],\"institutionId\":null,\"actif\":true,\"fonction\":\"Conductrice\"}";
        String recherche = "http://iam/api/v1/interne/collaborateurs?recherche=ngo&page=1&taille=10";
        String fiche = "http://iam/api/v1/interne/collaborateurs/" + ID;
        serveur
            .expect(requestTo(recherche))
            .andExpect(header("Authorization", "Bearer jeton"))
            .andRespond(withSuccess("{\"contenu\":[" + json + "],\"total\":11,\"page\":1,\"taille\":10}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(recherche)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(recherche)).andRespond(withSuccess("{\"contenu\":[{\"id\":\"x\",\"nomAffiche\":\"X\"}]}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(recherche)).andRespond(withServerError());
        serveur.expect(requestTo(fiche)).andExpect(header("Authorization", "Bearer jeton")).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(fiche)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(fiche)).andRespond(withServerError());

        var p = a.rechercher("ngo", 1, 10);
        assertThat(p.total()).isEqualTo(11);
        assertThat(p.contenu()).singleElement().satisfies(c -> {
            assertThat(c.nom()).isEqualTo("Aminata Ngono");
            assertThat(c.fonction()).isEqualTo("Conductrice");
            assertThat(c.joignable()).isTrue();
        });
        assertThat(a.rechercher("ngo", 1, 10).contenu()).isEmpty();
        var partielle = a.rechercher("ngo", 1, 10);
        assertThat(partielle.total()).isEqualTo(1);
        assertThat(partielle.page()).isEqualTo(1);
        assertThat(partielle.taille()).isEqualTo(10);
        assertThat(partielle.contenu().get(0).joignable()).isFalse();
        erreur(() -> a.rechercher("ngo", 1, 10), "COL_DIRECTORY_UNAVAILABLE", 503);
        assertThat(a.trouver(ID)).map(Collaborateur::profils).contains(Set.of("ROLE_CONDUCTEUR_PROJET"));
        assertThat(a.trouver(ID)).isEmpty();
        erreur(() -> a.trouver(ID), "COL_DIRECTORY_UNAVAILABLE", 503);
        serveur.verify();
    }

    @Test
    @DisplayName("Annuaire de substitution : internes actifs, filtre par nom ou fonction, pagination")
    void annuaireSubstitution() {
        CollaboratorStubAdapter a = new CollaboratorStubAdapter();
        var tous = a.rechercher(null, 0, 50);
        assertThat(tous.contenu()).allMatch(Collaborateur::joignable).extracting(Collaborateur::id).doesNotContain(CollaboratorStubAdapter.CORRESPONDANT, CollaboratorStubAdapter.INACTIF);
        assertThat(tous.total()).isEqualTo(5);
        assertThat(a.rechercher("sécurité", 0, 10).contenu()).extracting(Collaborateur::id).containsExactly(CollaboratorStubAdapter.RSSI);
        assertThat(a.rechercher("NGONO", 0, 10).contenu()).hasSize(1);
        assertThat(a.rechercher("", 2, 2).contenu()).hasSize(1);
        assertThat(a.rechercher("", 9, 2).contenu()).isEmpty();
        assertThat(a.trouver(CollaboratorStubAdapter.CORRESPONDANT)).get().extracting(Collaborateur::joignable).isEqualTo(false);
        assertThat(a.trouver("inconnu")).isEmpty();
        a.enregistrer(new Collaborateur("x", "Xavier", null, Set.of(), null, true));
        assertThat(a.rechercher("xav", 0, 10).contenu()).hasSize(1);
    }

    @Test
    @DisplayName("Dépôt réel : multipart fichier, finalité MESSAGERIE, référence et déposant ; refus 4xx ; panne 503")
    void depotReel() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        DocumentRestAdapter a = new DocumentRestAdapter(b, jeton(), "http://document");
        UUID message = UUID.randomUUID();
        UUID doc = UUID.randomUUID();
        String url = "http://document/api/v1/interne/documents";
        serveur
            .expect(requestTo(url))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer jeton"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
            .andExpect(content().string(org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("name=\"fichier\"; filename="),
                org.hamcrest.Matchers.containsString("contenu du PV"),
                org.hamcrest.Matchers.containsString("MESSAGERIE"),
                org.hamcrest.Matchers.containsString(message.toString()),
                org.hamcrest.Matchers.containsString("name=\"deposantId\"")
            )))
            .andRespond(withSuccess("{\"id\":\"" + doc + "\",\"sha256\":\"" + "a".repeat(64) + "\",\"nomFichier\":\"pv.pdf\",\"taille\":13,\"typeMime\":\"application/pdf\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{\"id\":\"" + doc + "\",\"sha256\":\"" + "b".repeat(64) + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        serveur.expect(requestTo(url)).andRespond(withServerError());

        DocumentDepose d = a.deposer(fichier("contenu du PV"), message, "moi");
        assertThat(d.id()).isEqualTo(doc);
        assertThat(d.sha256()).hasSize(64);
        assertThat(d.nomFichier()).isEqualTo("pv.pdf");
        DocumentDepose partiel = a.deposer(fichier("contenu du PV"), message, "moi");
        assertThat(partiel.nomFichier()).isEqualTo("pv de recette.pdf");
        assertThat(partiel.taille()).isEqualTo(13);
        assertThat(partiel.typeMime()).isEqualTo("application/pdf");
        erreur(() -> a.deposer(fichier("x"), message, "moi"), "COL_DOCUMENT_UNAVAILABLE", 503);
        erreur(() -> a.deposer(fichier("4111111111111111"), message, "moi"), "COL_ATTACHMENT_REJECTED", 400);
        erreur(() -> a.deposer(fichier("x"), message, "moi"), "COL_DOCUMENT_UNAVAILABLE", 503);
        FichierJoint illisible = new FichierJoint("a.pdf", "application/pdf", 1, () -> {
            throw new IOException("disque");
        });
        erreur(() -> a.deposer(illisible, message, "moi"), "COL_DOCUMENT_UNAVAILABLE", 503);
        serveur.verify();
    }

    @Test
    @DisplayName("Lecture réelle : flux du document ; 404 : pièce introuvable ; panne : 503")
    void lectureReelle() throws Exception {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        DocumentRestAdapter a = new DocumentRestAdapter(b, jeton(), "http://document");
        UUID doc = UUID.randomUUID();
        String url = "http://document/api/v1/interne/documents/" + doc + "/contenu";
        serveur.expect(requestTo(url)).andExpect(header("Authorization", "Bearer jeton")).andRespond(withSuccess("octets", MediaType.APPLICATION_OCTET_STREAM));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        ContenuDocument c = a.lire(doc);
        try (InputStream in = c.flux()) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("octets");
        }
        erreur(() -> a.lire(doc), "COL_ATTACHMENT_NOT_FOUND", 404);
        erreur(() -> a.lire(doc), "COL_DOCUMENT_UNAVAILABLE", 503);
        serveur.verify();
    }

    @Test
    @DisplayName("D-74 : métadonnées réelles d'un ticket avec le jeton de service ; 404 : vide ; panne ou réponse incomplète : 503")
    void metadonneesReelles() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        DocumentRestAdapter a = new DocumentRestAdapter(b, jeton(), "http://document");
        UUID doc = UUID.randomUUID();
        String url = "http://document/api/v1/interne/documents/" + doc;
        String json =
            "{\"id\":\"" + doc + "\",\"nomFichier\":\"atelier.mp4\",\"taille\":4294967296,\"typeMime\":\"video/mp4\",\"sha256\":\"" + "c".repeat(64) +
            "\",\"finalite\":\"MESSAGERIE\",\"reference\":null,\"deposant\":\"moi\",\"institutionId\":null,\"deposeLe\":\"2026-10-01T08:00:00Z\"}";
        serveur
            .expect(requestTo(url))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer jeton"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.FORBIDDEN));
        serveur.expect(requestTo(url)).andRespond(withSuccess("{\"nomFichier\":\"x\"}", MediaType.APPLICATION_JSON));

        assertThat(a.metadonnees(doc)).hasValueSatisfying(m -> {
            assertThat(m.id()).isEqualTo(doc);
            assertThat(m.nomFichier()).isEqualTo("atelier.mp4");
            assertThat(m.taille()).isEqualTo(4294967296L);
            assertThat(m.typeMime()).isEqualTo("video/mp4");
            assertThat(m.sha256()).isEqualTo("c".repeat(64));
            assertThat(m.finalite()).isEqualTo("MESSAGERIE");
            assertThat(m.deposant()).isEqualTo("moi");
        });
        assertThat(a.metadonnees(doc)).isEmpty();
        erreur(() -> a.metadonnees(doc), "COL_DOCUMENT_UNAVAILABLE", 503);
        erreur(() -> a.metadonnees(doc), "COL_DOCUMENT_UNAVAILABLE", 503);
        erreur(() -> a.metadonnees(doc), "COL_DOCUMENT_UNAVAILABLE", 503);
        serveur.verify();
    }

    @Test
    @DisplayName("D-74 : métadonnées de substitution, cohérentes avec ses dépôts (direct et fragmenté simulé) ; inconnu : vide")
    void metadonneesSubstitution() throws Exception {
        DocumentStubAdapter a = new DocumentStubAdapter();
        UUID message = UUID.randomUUID();
        DocumentDepose d = a.deposer(fichier("abc"), message, "moi");
        assertThat(a.metadonnees(d.id())).hasValueSatisfying(m -> {
            assertThat(m.nomFichier()).isEqualTo("pv de recette.pdf");
            assertThat(m.taille()).isEqualTo(3);
            assertThat(m.sha256()).isEqualTo(d.sha256());
            assertThat(m.finalite()).isEqualTo("MESSAGERIE");
            assertThat(m.reference()).isEqualTo(message.toString());
            assertThat(m.deposant()).isEqualTo("moi");
        });
        UUID ticket = a.deposerFragmente("atelier.mp4", "video/mp4", new byte[] { 'a', 'b', 'c' }, "MESSAGERIE", "moi");
        assertThat(a.metadonnees(ticket)).hasValueSatisfying(m -> {
            assertThat(m.id()).isEqualTo(ticket);
            assertThat(m.typeMime()).isEqualTo("video/mp4");
            assertThat(m.sha256()).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        });
        try (InputStream in = a.lire(ticket).flux()) {
            assertThat(in.readAllBytes()).containsExactly('a', 'b', 'c');
        }
        assertThat(a.metadonnees(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("Dépôt de substitution : empreinte SHA-256, métadonnées du dépôt, relecture ; inconnu : 404")
    void depotSubstitution() throws Exception {
        DocumentStubAdapter a = new DocumentStubAdapter();
        UUID message = UUID.randomUUID();
        DocumentDepose d = a.deposer(fichier("abc"), message, "moi");
        assertThat(d.sha256()).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(d.taille()).isEqualTo(3);
        assertThat(a.depose(d.id()).finalite()).isEqualTo("MESSAGERIE");
        assertThat(a.depose(d.id()).reference()).isEqualTo(message.toString());
        assertThat(a.depose(d.id()).deposantId()).isEqualTo("moi");
        try (InputStream in = a.lire(d.id()).flux()) {
            assertThat(in.readAllBytes()).containsExactly('a', 'b', 'c');
        }
        UUID inconnu = UUID.randomUUID();
        erreur(() -> a.lire(inconnu), "COL_ATTACHMENT_NOT_FOUND", 404);
        FichierJoint illisible = new FichierJoint("a.pdf", "application/pdf", 1, () -> {
            throw new IOException("disque");
        });
        assertThatThrownBy(() -> a.deposer(illisible, message, "moi")).isInstanceOf(java.io.UncheckedIOException.class);
    }

    @Test
    @DisplayName("mon-compte.md §1 : versions des photos par lots de 200 avec le jeton de service ; version relayée par l'annuaire ; panne : 503")
    void versionsAvatarReel() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        IamCollaboratorAdapter a = new IamCollaboratorAdapter(b, jeton(), "http://iam");
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 201; i++) {
            ids.add("u" + i);
        }
        serveur
            .expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate("http://iam/api/v1/interne/collaborateurs/avatars?ids={a}", "u0"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        serveur
            .expect(org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam("ids", ids.subList(0, 200).toArray(String[]::new)))
            .andExpect(header("Authorization", "Bearer jeton"))
            .andRespond(withSuccess("{\"u1\":\"0123456789abcdef\"}", MediaType.APPLICATION_JSON));
        serveur.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam("ids", "u200")).andRespond(withSuccess("{\"u200\":\"fedcba9876543210\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo("http://iam/api/v1/interne/collaborateurs/avatars?ids=u0")).andRespond(withServerError());
        serveur
            .expect(requestTo("http://iam/api/v1/interne/collaborateurs/" + ID))
            .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"nomAffiche\":\"A\",\"actif\":true,\"avatarVersion\":\"0123456789abcdef\"}", MediaType.APPLICATION_JSON));

        assertThat(a.versionsAvatar(java.util.Arrays.asList("u0", null, " ", "u0"))).isEmpty();
        assertThat(a.versionsAvatar(ids)).containsEntry("u1", "0123456789abcdef").containsEntry("u200", "fedcba9876543210").hasSize(2);
        erreur(() -> a.versionsAvatar(java.util.List.of("u0")), "COL_DIRECTORY_UNAVAILABLE", 503);
        assertThat(a.versionsAvatar(java.util.List.of())).isEmpty();
        assertThat(a.versionsAvatar(null)).isEmpty();
        assertThat(a.trouver(ID)).map(Collaborateur::avatarVersion).contains("0123456789abcdef");
        serveur.verify();
    }

    @Test
    @DisplayName("mon-compte.md §1 : annuaire de substitution, versions des photos des comptes connus seulement")
    void versionsAvatarSubstitution() {
        CollaboratorStubAdapter a = new CollaboratorStubAdapter();
        a.enregistrer(new Collaborateur(CollaboratorStubAdapter.RSSI, "Clarisse Bilong", "RSSI", Set.of("ROLE_RSSI"), null, true, "0123456789abcdef"));
        assertThat(a.versionsAvatar(java.util.Arrays.asList(CollaboratorStubAdapter.RSSI, CollaboratorStubAdapter.DDRM, "inconnu", null))).containsExactly(
            java.util.Map.entry(CollaboratorStubAdapter.RSSI, "0123456789abcdef")
        );
        assertThat(a.versionsAvatar(null)).isEmpty();
        assertThat(a.trouver(CollaboratorStubAdapter.DDRM)).map(Collaborateur::avatarVersion).isEmpty();
    }
}
