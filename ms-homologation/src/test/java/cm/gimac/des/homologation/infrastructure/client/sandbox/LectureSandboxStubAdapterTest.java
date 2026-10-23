package cm.gimac.des.homologation.infrastructure.client.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.HorsContrat;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.Indisponible;
import cm.gimac.des.homologation.application.port.out.LectureSandboxPort.PageExecutions;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SBX-LECTURE de substitution : pagination par position (annexe D.5), jetons, pannes, signalements, pièces. */
class LectureSandboxStubAdapterTest {

    static final UUID L = UUID.randomUUID();

    static LectureSandboxStubAdapter avec(int executions) {
        LectureSandboxStubAdapter s = new LectureSandboxStubAdapter();
        var c = s.alimenter(L, "C1", "99001", "MOB", List.of("RETRAIT"));
        for (int i = 1; i <= executions; i++) {
            c.executer("EXE-" + i, "SCN-" + i, "PASSED", "favorable", "2026-10-13T16:52:07Z");
        }
        return s;
    }

    @Test
    @DisplayName("Pages par position : pleine et close, dernière non close, page vide non close quand le lecteur est à jour")
    void pagination() {
        LectureSandboxStubAdapter s = avec(3);
        PageExecutions p1 = s.executions("C1", null, 4);
        assertThat(p1.enregistrements()).hasSize(4);
        assertThat(p1.close()).isTrue();
        PageExecutions p2 = s.executions("C1", p1.positionSuivante(), 4);
        assertThat(p2.enregistrements()).hasSize(2);
        assertThat(p2.close()).isFalse();
        PageExecutions p3 = s.executions("C1", p2.positionSuivante(), 4);
        assertThat(p3.enregistrements()).isEmpty();
        assertThat(p3.close()).isFalse();
        assertThat(p3.positionSuivante()).isEqualTo(p2.positionSuivante());
        // Une page close relue depuis la même position avec la même taille est identique.
        assertThat(s.executions("C1", null, 4)).isEqualTo(p1);
        // Taille bornée à la taille maximale publiée.
        assertThat(s.executions("C1", null, 0).enregistrements()).hasSize(1);
    }

    @Test
    @DisplayName("Jeton altéré ou d'une autre campagne : 400 sans page")
    void jetons() {
        LectureSandboxStubAdapter s = avec(1);
        s.alimenter(L, "C2", "99001", "CAR", List.of());
        String autre = LectureSandboxStubAdapter.jeton("C2", 1);
        assertThatThrownBy(() -> s.executions("C1", autre, 10)).isInstanceOf(HorsContrat.class).hasMessageContaining("400");
        assertThatThrownBy(() -> s.executions("C1", "%%%", 10)).isInstanceOf(HorsContrat.class);
        String malForme = Base64.getUrlEncoder().encodeToString("v2|C1|1".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> s.executions("C1", malForme, 10)).isInstanceOf(HorsContrat.class);
        String court = Base64.getUrlEncoder().encodeToString("v1|C1".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> s.executions("C1", court, 10)).isInstanceOf(HorsContrat.class);
    }

    @Test
    @DisplayName("Pannes : 503 ou 429 avec délai, après un nombre d'appels servis, puis retour")
    void pannes() {
        LectureSandboxStubAdapter s = avec(1);
        s.indisponibleApres(1, 1, 429, Duration.ofSeconds(9));
        assertThat(s.campagnes(L)).hasSize(1);
        assertThatThrownBy(() -> s.campagne("C1")).isInstanceOfSatisfying(Indisponible.class, e -> {
            assertThat(e.delai()).isEqualTo(Duration.ofSeconds(9));
            assertThat(e.getMessage()).contains("429");
        });
        assertThat(s.campagne("C1").identifiantCampagne()).isEqualTo("C1");
        s.indisponible(2, 503, Duration.ofSeconds(30));
        s.vider();
        assertThat(s.campagnes(L)).isEmpty();
    }

    @Test
    @DisplayName("Campagne signalée (collision) : remplacée par un signalement en liste, 409 en accès direct ; inconnue : 404")
    void signalementsEtAbsences() {
        LectureSandboxStubAdapter s = avec(0);
        s.alimenter(L, "C9", "99002", "CAR", List.of()).signaler("COLLISION");
        assertThat(s.campagnes(L)).anySatisfy(c -> {
            assertThat(c.identifiantCampagne()).isEqualTo("C9");
            assertThat(c.etat()).isEqualTo("SIGNALEE");
            assertThat(c.signalement()).isEqualTo("COLLISION");
        });
        assertThat(s.campagnes(UUID.randomUUID())).isEmpty();
        assertThatThrownBy(() -> s.campagne("C9")).isInstanceOf(HorsContrat.class).hasMessageContaining("409");
        assertThatThrownBy(() -> s.campagne("C0")).isInstanceOf(HorsContrat.class).hasMessageContaining("404");
    }

    @Test
    @DisplayName("Pièces : empreinte des octets, version inconnue 404, empreinte annoncée falsifiable ; état et bilan")
    void pieces() {
        LectureSandboxStubAdapter s = avec(0);
        var c = s.alimenter(L, "C3", "99001", "MOB", List.of()).piece("BILAN", 2, "contenu").etat("TERMINEE", 2).etat("TERMINEE", null);
        String hex = LectureSandboxStubAdapter.hex(LectureSandboxStubAdapter.sha256("contenu".getBytes(StandardCharsets.UTF_8)));
        assertThat(s.pieces("C3")).singleElement().satisfies(p -> {
            assertThat(p.empreinte()).isEqualTo(hex);
            assertThat(p.taille()).isEqualTo(7);
            assertThat(p.nom()).isEqualTo("bilan-v2.json");
        });
        assertThat(s.empreinte("C3", "BILAN", 2)).isEqualTo(hex);
        assertThat(new String(s.telecharger("C3", "BILAN", 2), StandardCharsets.UTF_8)).isEqualTo("contenu");
        assertThatThrownBy(() -> s.telecharger("C3", "BILAN", 3)).isInstanceOf(HorsContrat.class);
        c.falsifierEmpreinte("00".repeat(32));
        assertThat(s.empreinte("C3", "BILAN", 2)).isEqualTo("00".repeat(32));
        assertThat(s.campagne("C3").versionBilan()).isEqualTo(2);
        assertThat(s.campagne("C3").etat()).isEqualTo("TERMINEE");
    }

    @Test
    @DisplayName("Rectification : un enregistrement de plus pour la même exécution, à une position supérieure")
    void rectification() {
        LectureSandboxStubAdapter s = new LectureSandboxStubAdapter();
        s.alimenter(L, "C4", "99001", "MOB", List.of())
            .executer("EXE-1", "SCN-1", "FAILED", "defavorable", "2026-10-13T16:52:07Z")
            .enregistrer("EXE-1", "SCN-1", "CAS-SCN-1", "PASSED", "favorable", "RECTIFICATION", "2026-10-14T08:00:00Z");
        var e = s.executions("C4", null, 10).enregistrements();
        assertThat(e).extracting(x -> x.nature()).containsExactly("CREATION", "VALIDATION", "RECTIFICATION");
        assertThat(e.get(2).verdictNatif()).isEqualTo("PASSED");
    }
}
