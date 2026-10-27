package cm.gimac.des.gateway.infrastructure.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.IntrospectionSandbox.Portee;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox;
import cm.gimac.des.gateway.test.util.ServeurHttpLocal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Intégration HTTP avec le contrat MS-IAM : jeton de service, portée projetée et refus par défaut. */
class IamIntrospectionSandboxIT {

    static final String CHEMIN = "/api/v1/interne/sandbox/introspection";

    @Test
    @DisplayName("Compte propre, mandat actif et prestataire : réponses RFC 7662 projetées, jeton original transmis sans journal")
    void porteesActives() throws Exception {
        try (ServeurHttpLocal iam = new ServeurHttpLocal().json(
            CHEMIN,
            200,
            "{\"active\":true,\"sub\":\"u-1\",\"institution_id\":\"99002\",\"permissions\":[\"SBX_CER_CONSULTATION\"],\"type\":\"MANDAT\"}"
        )) {
            IamIntrospectionSandbox client = client(iam, () -> Mono.just("service-gateway"));
            Portee p = client.introspecter("original-secret", "99002", "SBX-CAMPAGNE").block();
            assertThat(p.active()).isTrue();
            assertThat(p.sujet()).isEqualTo("u-1");
            assertThat(p.institution()).isEqualTo("99002");
            assertThat(p.permissions()).containsExactly("SBX_CER_CONSULTATION");
            assertThat(p.type()).isEqualTo("MANDAT");
            ServeurHttpLocal.Recue recue = iam.recues().get(0);
            assertThat(recue.methode()).isEqualTo("POST");
            assertThat(recue.chemin()).isEqualTo(CHEMIN);
            assertThat(recue.autorisation()).isEqualTo("Bearer service-gateway");
            assertThat(recue.typeContenu()).startsWith("application/json");
            assertThat(recue.corps()).contains("\"jeton\":\"original-secret\"", "\"institution\":\"99002\"", "\"composant\":\"SBX-CAMPAGNE\"");
            assertThat(new IamIntrospectionSandbox.Requete("original-secret", "99002", "SBX-CAMPAGNE").toString()).doesNotContain("original-secret");
        }
    }

    @Test
    @DisplayName("Mandat échu et suspension : active=false devient le même 404 audité, sans cache")
    void refusActifFaux() throws Exception {
        try (ServeurHttpLocal iam = new ServeurHttpLocal().json(CHEMIN, 200, "{\"active\":false}")) {
            IamIntrospectionSandbox client = client(iam, () -> Mono.just("service-gateway"));
            List<JournalDecisionsSandbox.Entree> journal = new ArrayList<>();
            CloisonnementSandbox service = new CloisonnementSandbox(client, journal::add, Map.of(), j -> Optional.of("reduit"));
            CloisonnementSandbox.Demande demande = new CloisonnementSandbox.Demande(
                "u-1", "99010", "99002", new RouteSandbox("portail", "campagne"), "GET", "/bff/portail/sbx-campagne/campagnes", List.of(), "2", "", "original-secret"
            );
            CloisonnementSandbox.Resultat r = service.evaluer(demande).block();
            assertThat(r.statut()).isEqualTo(404);
            assertThat(r.decision().motif()).isEqualTo(Motif.INTROSPECTION_REFUSEE);
            assertThat(journal).singleElement().satisfies(e -> assertThat(e.statut()).isEqualTo(404));
            assertThat(iam.recues()).hasSize(1);
        }
    }

    @Test
    @DisplayName("Erreur HTTP, délai ou jeton de service absent : refus 404 IAM_INDISPONIBLE")
    void indisponibilite() throws Exception {
        try (ServeurHttpLocal iam = new ServeurHttpLocal().json(CHEMIN, 503, "{}")) {
            CloisonnementSandbox service = new CloisonnementSandbox(client(iam, () -> Mono.just("service-gateway")), e -> {}, Map.of(), null);
            assertThat(service.evaluer(demande()).block().decision().motif()).isEqualTo(Motif.IAM_INDISPONIBLE);
            CloisonnementSandbox sansService = new CloisonnementSandbox(client(iam, Mono::empty), e -> {}, Map.of(), null);
            assertThat(sansService.evaluer(demande()).block().decision().motif()).isEqualTo(Motif.IAM_INDISPONIBLE);
            assertThat(client(iam, () -> Mono.just("service-gateway")).introspecter(" ", null, "SBX-CAMPAGNE").block().active()).isFalse();
        }
        try (ServeurHttpLocal lent = new ServeurHttpLocal().repondre(CHEMIN, 200, "application/json", "{\"active\":true}", 500)) {
            CloisonnementSandbox service = new CloisonnementSandbox(
                new IamIntrospectionSandbox(WebClient.builder().baseUrl(lent.url()).build(), () -> Mono.just("service-gateway"), Duration.ofMillis(30)),
                e -> {},
                Map.of(),
                null
            );
            assertThat(service.evaluer(demande()).block().decision().motif()).isEqualTo(Motif.IAM_INDISPONIBLE);
        }
    }

    static IamIntrospectionSandbox client(ServeurHttpLocal iam, java.util.function.Supplier<Mono<String>> service) {
        return new IamIntrospectionSandbox(WebClient.builder().baseUrl(iam.url()).build(), service, Duration.ofSeconds(5));
    }

    static CloisonnementSandbox.Demande demande() {
        return new CloisonnementSandbox.Demande(
            "u-1", "99001", null, new RouteSandbox("portail", "campagne"), "GET", "/bff/portail/sbx-campagne/campagnes", List.of(), "2", "", "original-secret"
        );
    }
}
