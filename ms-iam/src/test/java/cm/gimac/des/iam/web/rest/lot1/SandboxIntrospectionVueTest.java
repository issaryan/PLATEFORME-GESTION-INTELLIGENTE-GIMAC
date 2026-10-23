package cm.gimac.des.iam.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.domain.sandbox.Introspection;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Réponse d'introspection au format de RFC 7662 (D-119), sans Docker. */
class SandboxIntrospectionVueTest {

    @Test
    @DisplayName("Refus : active seul ; admis : portée projetée, échéance en secondes ; le jeton n'apparaît jamais dans une trace")
    void vue() {
        assertThat(SandboxIntrospectionResource.vue(Introspection.refusee("s", "PERIMETRE_SUSPENDU"))).containsOnlyKeys("active").containsEntry("active", false);
        var admis = SandboxIntrospectionResource.vue(
            new Introspection(true, "s", "99001", Set.of("SBX_CER_EXECUTION"), "MANDAT", "d1", Instant.ofEpochSecond(1000), Set.of(), null)
        );
        assertThat(admis).containsEntry("active", true).containsEntry("institution_id", "99001").containsEntry("exp", 1000L).containsEntry("delegation_id", "d1");
        assertThat(SandboxIntrospectionResource.vue(new Introspection(true, "s", null, Set.of(), "COMPTE", null, null, Set.of(), null))).containsEntry("exp", null);
        assertThat(new SandboxIntrospectionResource.IntrospectionRequest("secret", "99001", "SBX-TRACE").toString()).doesNotContain("secret");
    }
}
