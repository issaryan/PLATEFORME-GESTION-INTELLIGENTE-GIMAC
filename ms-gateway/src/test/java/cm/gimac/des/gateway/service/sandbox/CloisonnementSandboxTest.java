package cm.gimac.des.gateway.service.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import cm.gimac.des.gateway.domain.sandbox.JetonReduit;
import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import cm.gimac.des.gateway.domain.sandbox.RouteSandbox;
import cm.gimac.des.gateway.domain.sandbox.port.EmetteurJetonReduit;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import cm.gimac.des.gateway.infrastructure.sandbox.ProjectionPerimetresMemoire;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox.Demande;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox.Resultat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/** Décision de la passerelle sur les routes de la sandbox (US-EXI-07), horloge figée au 22/10/2026. */
class CloisonnementSandboxTest {

    static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);
    static final RouteSandbox SIMULATION = new RouteSandbox("portail", "simulation");

    final List<JournalDecisionsSandbox.Entree> journal = new ArrayList<>();
    final List<JetonReduit> emis = new ArrayList<>();
    ProjectionPerimetresMemoire projection;
    EmetteurJetonReduit emetteur;
    boolean quotaAdmis;

    @BeforeEach
    void preparer() {
        projection = new ProjectionPerimetresMemoire();
        emetteur = j -> {
            emis.add(j);
            return Optional.of("jeton-reduit-" + j.institution());
        };
        quotaAdmis = true;
    }

    CloisonnementSandbox service(EmetteurJetonReduit e, Map<String, LimiteurTentatives> quotas) {
        return new CloisonnementSandbox(projection, journal::add, quotas, e, HORLOGE);
    }

    CloisonnementSandbox service() {
        return service(emetteur, Map.of("portail", cle -> Mono.just(quotaAdmis)));
    }

    static Demande demande(String institution, String demandee) {
        return new Demande(
            "u-1",
            institution,
            demandee,
            SIMULATION,
            "GET",
            "/bff/portail/sbx-simulation/essais",
            List.of("SBX_SIM_SIMULATION", "NOT_INBOX_READ", "SBX_SIM_CONSULTATION"),
            "2",
            "10.0.0.1"
        );
    }

    @Test
    @DisplayName("Critère 1 : route du portail cloisonnée, jeton interne réduit portant 99001, décision journalisée")
    void routePortailCloisonnee() {
        Resultat r = service().evaluer(demande("99001", null)).block();
        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.decision().motif()).isEqualTo(Motif.INSTITUTION_PROPRE);
        assertThat(r.jeton()).contains("jeton-reduit-99001");
        // Institution choisie égale à celle du jeton : même décision, sans lecture du périmètre.
        assertThat(service().evaluer(demande("99001", "99001")).block().decision().motif()).isEqualTo(Motif.INSTITUTION_PROPRE);
        journal.remove(1);
        emis.remove(1);
        assertThat(emis).singleElement().satisfies(j -> {
            assertThat(j.institution()).isEqualTo("99001");
            assertThat(j.institutionPropre()).isEqualTo("99001");
            assertThat(j.permissions()).containsExactly("SBX_SIM_CONSULTATION", "SBX_SIM_SIMULATION");
            assertThat(j.audience()).isEqualTo("sbx-simulation");
            assertThat(j.acr()).isEqualTo("2");
            assertThat(j.sujet()).isEqualTo("u-1");
        });
        assertThat(journal).singleElement().satisfies(e -> {
            assertThat(e.statut()).isEqualTo(200);
            assertThat(e.decision().institutionEffective()).isEqualTo("99001");
            assertThat(e.route()).isEqualTo(SIMULATION);
            assertThat(e.compte()).isEqualTo("u-1");
        });
    }

    @Test
    @DisplayName("Critère 2 : partenaire mandaté par 99001 et 99002, le jeton réduit porte 99002 tant que le mandat est actif")
    void partenaireMandate() {
        projection.appliquer(new PerimetreMandats("u-1", "99010", Map.of("99001", Instant.parse("2026-11-10T00:00:00Z"), "99002", Instant.parse("2026-11-10T00:00:00Z")), 3)).block();
        Resultat r = service().evaluer(demande("99010", "99002")).block();
        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.decision().motif()).isEqualTo(Motif.MANDAT_ACTIF);
        assertThat(r.jeton()).contains("jeton-reduit-99002");
        assertThat(emis.get(0).institutionPropre()).isEqualTo("99010");
    }

    @Test
    @DisplayName("Critère 4 : mandat échu pour 99002, refus 404 sans révéler la ressource, tentative journalisée")
    void institutionHorsMandat() {
        projection.appliquer(new PerimetreMandats("u-1", "99010", Map.of("99002", Instant.parse("2026-10-01T00:00:00Z")), 4)).block();
        Resultat r = service().evaluer(demande("99010", "99002")).block();
        assertThat(r.statut()).isEqualTo(404);
        assertThat(r.decision().motif()).isEqualTo(Motif.MANDAT_ECHU);
        assertThat(r.jeton()).isEmpty();
        assertThat(emis).isEmpty();
        assertThat(journal).singleElement().satisfies(e -> {
            assertThat(e.statut()).isEqualTo(404);
            assertThat(e.demandee()).isEqualTo("99002");
            assertThat(e.decision().admise()).isFalse();
        });
        // Aucun instantané reçu pour ce compte : mandat absent, même refus.
        assertThat(service().evaluer(new Demande("u-2", "99010", "99002", SIMULATION, "GET", "/x", List.of(), "2", "")).block().decision().motif()).isEqualTo(Motif.MANDAT_ABSENT);
    }

    @Test
    @DisplayName("Mandat actif sans émetteur de jeton réduit : refus par défaut (aucune institution mandante ne peut être relayée)")
    void jetonReduitIndisponible() {
        projection.appliquer(new PerimetreMandats("u-1", "99010", Map.of("99002", Instant.parse("2026-11-10T00:00:00Z")), 1)).block();
        Resultat r = service(null, Map.of()).evaluer(demande("99010", "99002")).block();
        assertThat(r.statut()).isEqualTo(404);
        assertThat(r.decision().motif()).isEqualTo(Motif.JETON_REDUIT_INDISPONIBLE);
        assertThat(r.decision().institutionEffective()).isEqualTo("99010");
        // Institution propre sans émetteur : relayée avec le jeton de session.
        Resultat propre = service(null, Map.of()).evaluer(demande("99001", null)).block();
        assertThat(propre.statut()).isEqualTo(200);
        assertThat(propre.jeton()).isEmpty();
    }

    @Test
    @DisplayName("Quota par couche, institution et route dépassé : 429, décision journalisée")
    void quota() {
        List<String> cles = new ArrayList<>();
        quotaAdmis = false;
        CloisonnementSandbox s = service(emetteur, Map.of("portail", cle -> {
            cles.add(cle);
            return Mono.just(quotaAdmis);
        }));
        Resultat r = s.evaluer(demande("99001", null)).block();
        assertThat(r.statut()).isEqualTo(429);
        assertThat(r.decision().motif()).isEqualTo(Motif.QUOTA_DEPASSE);
        assertThat(r.decision().institutionEffective()).isEqualTo("99001");
        assertThat(cles).containsExactly("sbx:portail:99001:simulation");
        assertThat(journal.get(0).statut()).isEqualTo(429);
    }

    @Test
    @DisplayName("Compte du GIMAC sur la console : admis, quota par compte, jeton réduit sans institution")
    void compteGimac() {
        List<String> cles = new ArrayList<>();
        CloisonnementSandbox s = service(emetteur, Map.of("console", cle -> {
            cles.add(cle);
            return Mono.just(true);
        }));
        Demande d = new Demande("u-gimac", "", null, new RouteSandbox("console", "trace"), "GET", "/bff/console/sbx-trace/x", List.of("SBX_TRA_CONSULTATION"), "2", "");
        Resultat r = s.evaluer(d).block();
        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.decision().motif()).isEqualTo(Motif.COMPTE_GIMAC);
        assertThat(cles).containsExactly("sbx:console:gimac:u-gimac:trace");
        assertThat(emis.get(0).institution()).isEmpty();
        assertThat(emis.get(0).institutionPropre()).isEmpty();
        // Institution du jeton nulle : même traitement.
        Demande nulle = new Demande("u-gimac", null, null, new RouteSandbox("des", "espace"), "GET", "/bff/des/sbx-espace/x", List.of(), "2", "");
        assertThat(s.evaluer(nulle).block().statut()).isEqualTo(200);
    }
}
