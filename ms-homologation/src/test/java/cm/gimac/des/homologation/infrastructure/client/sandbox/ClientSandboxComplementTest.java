package cm.gimac.des.homologation.infrastructure.client.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.HorsContrat;
import cm.gimac.des.homologation.application.usecase.sandbox.ReleveSandbox;
import cm.gimac.des.homologation.infrastructure.client.ServiceTokenInterceptor;
import cm.gimac.des.homologation.infrastructure.messaging.Json;
import cm.gimac.des.homologation.infrastructure.scheduling.ReleveSandboxScheduler;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;

/** Cas de bord du client de SBX-LECTURE, du substitut, du planificateur et de la lecture des enveloppes (US-EXI-14, US-EXI-15). */
class ClientSandboxComplementTest {

    static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Réponses hors contrat : vide, null JSON, élément vide ; Repr-Digest trop court")
    void horsContrat() {
        assertThatThrownBy(() -> TraducteurLectureSandbox.campagnes(b(""))).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> TraducteurLectureSandbox.campagnes(b("null"))).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> TraducteurLectureSandbox.campagnes(b("{\"campagnes\":[{\"identifiantCampagne\":\" \",\"etat\":\"EN_COURS\"}]}"))).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> TraducteurLectureSandbox.campagnes(b("{\"campagnes\":[{\"identifiantCampagne\":7,\"etat\":\"EN_COURS\"}]}"))).isInstanceOf(HorsContrat.class);
        assertThatThrownBy(() -> TraducteurLectureSandbox.empreinteRepr("sha-256=::")).isInstanceOf(HorsContrat.class).hasMessageContaining("absente");
        assertThatThrownBy(() -> TraducteurLectureSandbox.empreinteRepr("sha-256=:q83vEjRWeJA=")).isInstanceOf(HorsContrat.class).hasMessageContaining("absente");
    }

    @Test
    @DisplayName("Adaptateur réel construit par Spring (passerelle interne, jeton de service) ; condensé d'un algorithme absent")
    void construction() throws NoSuchMethodException {
        assertThat(new LectureSandboxRestAdapter(RestClient.builder(), new ServiceTokenInterceptor(mock(OAuth2AuthorizedClientManager.class)), "http://sbx-lecture:8111")).isNotNull();
        assertThat(
            LectureSandboxRestAdapter.class.getConstructor(RestClient.Builder.class, ServiceTokenInterceptor.class, String.class).getAnnotation(org.springframework.beans.factory.annotation.Autowired.class)
        ).isNotNull();
        assertThatThrownBy(() -> LectureSandboxStubAdapter.condense(b("x"), "INCONNU")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Planificateur : relève périodique de toutes les liaisons")
    void planificateur() {
        ReleveSandbox r = mock(ReleveSandbox.class);
        new ReleveSandboxScheduler(r).relever();
        verify(r).releverTout();
    }

    @Test
    @DisplayName("Planificateur : relève en échec consignée puis reprise ; une Error n'est jamais avalée")
    void planificateurEnEchec() {
        ReleveSandbox r = mock(ReleveSandbox.class);
        when(r.releverTout()).thenThrow(new IllegalStateException("SBX-LECTURE injoignable")).thenReturn(3).thenThrow(new OutOfMemoryError("tas"));
        ReleveSandboxScheduler s = new ReleveSandboxScheduler(r);
        assertThatCode(s::relever).doesNotThrowAnyException();
        assertThatCode(s::relever).doesNotThrowAnyException();
        assertThatThrownBy(s::relever).isInstanceOf(OutOfMemoryError.class);
        verify(r, times(3)).releverTout();
    }

    @Test
    @DisplayName("Lecture d'un champ textuel d'enveloppe : absent, nul, objet, vide, nombre, texte")
    void texte() {
        var n = Json.tree("{\"a\":null,\"b\":{},\"c\":\" \",\"d\":7,\"e\":\"x\"}");
        assertThat(Json.texte(n, "z")).isNull();
        assertThat(Json.texte(n, "a")).isNull();
        assertThat(Json.texte(n, "b")).isNull();
        assertThat(Json.texte(n, "c")).isNull();
        assertThat(Json.texte(n, "d")).isEqualTo("7");
        assertThat(Json.texte(n, "e")).isEqualTo("x");
        assertThat(Json.texte(null, "e")).isNull();
    }
}
