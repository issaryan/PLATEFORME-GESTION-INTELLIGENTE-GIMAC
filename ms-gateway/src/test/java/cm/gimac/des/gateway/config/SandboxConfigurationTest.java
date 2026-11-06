package cm.gimac.des.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.infrastructure.connexion.LimiteurTentativesMemoire;
import cm.gimac.des.gateway.infrastructure.connexion.LimiteurTentativesValkey;
import cm.gimac.des.gateway.infrastructure.sandbox.ConsommateurPerimetresKafka;
import cm.gimac.des.gateway.infrastructure.sandbox.JournalDecisionsSandboxAudit;
import cm.gimac.des.gateway.infrastructure.sandbox.ProjectionPerimetresMemoire;
import cm.gimac.des.gateway.infrastructure.sandbox.ProjectionPerimetresR2dbc;
import cm.gimac.des.gateway.ratelimit.TokenBucketRateLimiter;
import cm.gimac.des.gateway.security.EmetteurJwtInterne;
import cm.gimac.des.gateway.security.RsaKeyLoader;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

/** Assemblage du cloisonnement des routes de la sandbox (D-120, US-EXI-07). */
class SandboxConfigurationTest {

    final SandboxConfiguration config = new SandboxConfiguration();

    @SuppressWarnings("unchecked")
    static <T> ObjectProvider<T> fournisseur(T valeur) {
        ObjectProvider<T> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(valeur);
        when(p.getIfAvailable(any(Supplier.class))).thenAnswer(i -> valeur != null ? valeur : ((Supplier<T>) i.getArgument(0)).get());
        return p;
    }

    @Test
    @DisplayName("Projection en base si la base existe, en mémoire sinon ; journal des décisions")
    void projectionEtJournal() {
        Clock h = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        assertThat(config.projectionPerimetres(fournisseur(mock(DatabaseClient.class)), fournisseur(h))).isInstanceOf(ProjectionPerimetresR2dbc.class);
        assertThat(config.projectionPerimetres(fournisseur(null), fournisseur(null))).isInstanceOf(ProjectionPerimetresMemoire.class);
        assertThat(config.journalDecisionsSandbox(fournisseur(null))).isInstanceOf(JournalDecisionsSandboxAudit.class);
        assertThat(config.introspectionSandbox(new ConnexionProperties(), fournisseur(null), fournisseur(null)))
            .isInstanceOf(cm.gimac.des.gateway.infrastructure.sandbox.IamIntrospectionSandbox.class);
    }

    @Test
    @DisplayName("Quotas par couche : mémoire sans Valkey, Valkey avec la mémoire en secours ; valeurs par défaut des propriétés")
    void quotas() {
        SandboxProperties p = new SandboxProperties();
        assertThat(p.getQuotas()).containsEntry("portail", 120).containsEntry("console", 300).containsEntry("des", 120);
        Map<String, LimiteurTentatives> memoire = SandboxConfiguration.quotas(Map.of("portail", 1), null, Clock.systemUTC());
        assertThat(memoire.get("portail")).isInstanceOf(LimiteurTentativesMemoire.class);
        assertThat(memoire.get("portail").consommer("k").block()).isTrue();
        assertThat(memoire.get("portail").consommer("k").block()).isFalse();
        TokenBucketRateLimiter valkey = mock(TokenBucketRateLimiter.class);
        when(valkey.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(Mono.just(List.of(1L, 0L)));
        assertThat(SandboxConfiguration.quotas(Map.of("console", 5), valkey, Clock.systemUTC()).get("console")).isInstanceOf(LimiteurTentativesValkey.class);
        p.setQuotas(Map.of("portail", 2));
        assertThat(p.getQuotas()).containsOnlyKeys("portail");
    }

    @Test
    @DisplayName("Cas d'utilisation : avec ou sans émetteur du jeton interne de la passerelle")
    void cloisonnement() throws Exception {
        SandboxProperties p = new SandboxProperties();
        EmetteurJwtInterne emetteur = new EmetteurJwtInterne(RsaKeyLoader.genererEphemere(2048), fournisseur(null));
        cm.gimac.des.gateway.domain.sandbox.port.IntrospectionSandbox introspection = (j, i, c) -> Mono.just(
            new cm.gimac.des.gateway.domain.sandbox.port.IntrospectionSandbox.Portee(true, "u", "99001", List.of("SBX_TRA_CONSULTATION"), "COMPTE")
        );
        CloisonnementSandbox avec = config.cloisonnementSandbox(p, introspection, e -> {}, fournisseur(null), fournisseur(emetteur), fournisseur(null), "");
        CloisonnementSandbox sans = config.cloisonnementSandbox(p, introspection, e -> {}, fournisseur(null), fournisseur(null), fournisseur(null), "");
        CloisonnementSandbox.Demande d = new CloisonnementSandbox.Demande(
            "u",
            "99001",
            null,
            new cm.gimac.des.gateway.domain.sandbox.RouteSandbox("portail", "trace"),
            "GET",
            "/bff/portail/sbx-trace/x",
            List.of("SBX_TRA_CONSULTATION"),
            "2",
            "",
            "original"
        );
        // Valeur explicite vraie avec émetteur, valeur absente (nulle) sans émetteur : démarrage admis.
        assertThat(config.cloisonnementSandbox(p, introspection, e -> {}, fournisseur(null), fournisseur(emetteur), fournisseur(null), " true ")).isNotNull();
        assertThat(config.cloisonnementSandbox(p, introspection, e -> {}, fournisseur(null), fournisseur(null), fournisseur(null), null)).isNotNull();
        // Émetteur présent, sans valeur explicite : jeton réduit obligatoire et émis.
        assertThat(avec.evaluer(d).block().jeton()).isPresent();
        // Sans émetteur (déploiement Docker actuel) : jeton original relayé, réponse 200, aucun jeton réduit.
        assertThat(sans.evaluer(d).block().jeton()).isEmpty();
        assertThat(sans.evaluer(d).block().statut()).isEqualTo(200);
    }

    @Test
    @DisplayName("Consommateur des périmètres : courtiers et propriétés supplémentaires ; propriétés lues et modifiables")
    void consommateur() {
        SandboxProperties p = new SandboxProperties();
        SandboxProperties.Perimetres pp = p.getPerimetres();
        assertThat(pp.isActif()).isFalse();
        assertThat(pp.getSujet()).isEqualTo("sbx-espace.scope-changed.v1");
        assertThat(pp.getBootstrapServers()).isEqualTo("localhost:9092");
        pp.setActif(true);
        pp.setSujet("s");
        pp.setBootstrapServers("127.0.0.1:1");
        pp.setKafka(Map.of("client.id", "c"));
        assertThat(pp.getKafka()).containsEntry("client.id", "c");
        assertThat(pp.isActif()).isTrue();
        ConsommateurPerimetresKafka c = config.consommateurPerimetresKafka(p, new ProjectionPerimetresMemoire());
        assertThat(c.isRunning()).isFalse();
    }
}
