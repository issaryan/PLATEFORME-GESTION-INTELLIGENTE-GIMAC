package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.document.application.usecase.DepotPieceSandboxService;
import cm.gimac.des.document.application.usecase.DepotPieceSandboxService.Resultat;
import cm.gimac.des.document.application.usecase.TypesPiecesSandboxService;
import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.domain.sandbox.PieceSandbox;
import cm.gimac.des.document.domain.sandbox.TypePieceSandbox;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Routes des pièces de la sandbox (US-EXI-08) : statuts du dépôt, catalogue, quarantaines, sans contexte Spring. */
class PiecesSandboxResourceTest {

    final DepotPieceSandboxService depot = mock(DepotPieceSandboxService.class);
    final TypesPiecesSandboxService types = mock(TypesPiecesSandboxService.class);
    final cm.gimac.des.document.application.usecase.RechercheParEmpreinteService empreintes = mock(cm.gimac.des.document.application.usecase.RechercheParEmpreinteService.class);
    final PiecesSandboxResource resource = new PiecesSandboxResource(depot, types, empreintes);

    static JwtAuthenticationToken service(String client) {
        Jwt j = Jwt.withTokenValue("t").header("alg", "none").subject("s").claim("azp", client).claim("preferred_username", "service-account-" + client).issuedAt(Instant.EPOCH).build();
        return new JwtAuthenticationToken(j, List.of(), "service-account-" + client);
    }

    static Resultat resultat(PieceSandbox.Etat etat, boolean deja) {
        return new Resultat(UUID.randomUUID(), etat, "a".repeat(64), null, deja);
    }

    @Test
    @DisplayName("Dépôt : 201 pièce scellée, 200 déjà conservée, 202 quarantaine ; service lu dans le jeton")
    void depot() {
        MockMultipartFile f = new MockMultipartFile("fichier", "b.pdf", "application/pdf", new byte[] { 1 });
        when(depot.deposer(any(), eq("sbx-campagne"))).thenReturn(resultat(PieceSandbox.Etat.SCELLEE, false), resultat(PieceSandbox.Etat.SCELLEE, true), resultat(PieceSandbox.Etat.QUARANTAINE, false));
        assertThat(resource.deposer(f, "T", "e", "99001", "r", service("sbx-campagne")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resource.deposer(f, "T", "e", "99001", "r", service("sbx-campagne")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resource.deposer(null, "T", "e", "99001", "r", service("sbx-campagne")).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThatThrownBy(() -> resource.deposer(f, "T", "e", "99001", "r", new TestingAuthenticationToken("u", "p"))).isInstanceOf(DocumentException.class);
    }

    @Test
    @DisplayName("Catalogue, modification de la rétention par l'auteur du jeton, quarantaines paginées")
    void console() {
        TypePieceSandbox t = new TypePieceSandbox("T", Map.of("fr", "T"), Period.ofYears(10), true, Set.of("sbx-trace", "sbx-campagne"), TypePieceSandbox.ReglePan.QUARANTAINE, 5, true, 2);
        when(types.types()).thenReturn(List.of(t));
        when(types.modifierRetention("T", "P10Y", "m", 1, "rssi")).thenReturn(t);
        when(types.quarantaines(0, 20)).thenReturn(new TypesPiecesSandboxService.PageQuarantaines(List.of(), 0, 0, 20));
        PiecesSandboxResource.TypeVue v = resource.types().get(0);
        assertThat(v.retention()).isEqualTo("P10Y");
        assertThat(v.servicesAutorises()).containsExactly("sbx-campagne", "sbx-trace");
        assertThat(v.reglePan()).isEqualTo("QUARANTAINE");
        assertThat(resource.modifierRetention("T", new PiecesSandboxResource.Retention("P10Y", "m", 1), new TestingAuthenticationToken("rssi", "p")).version()).isEqualTo(2);
        assertThat(resource.quarantaines(0, 20).taille()).isEqualTo(20);
        var piece = new cm.gimac.des.document.application.usecase.RechercheParEmpreinteService.Piece(UUID.randomUUID(), "b.pdf", "application/pdf", "a".repeat(64));
        when(empreintes.parEmpreinte("a".repeat(64))).thenReturn(piece);
        assertThat(resource.parEmpreinte("a".repeat(64))).isSameAs(piece);
    }
}
