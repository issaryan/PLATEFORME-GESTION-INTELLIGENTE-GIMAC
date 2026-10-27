package cm.gimac.des.iam.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import cm.gimac.des.iam.domain.AccessReviewDecision;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReexamenAccesTest {

    @Test
    void periodeEtListesCanoniques() {
        assertThat(ReexamenAcces.periode(Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC))).isEqualTo("2026-T4");
        assertThat(ReexamenAcces.periode(Clock.fixed(Instant.parse("2027-01-01T00:00:00Z"), ZoneOffset.UTC))).isEqualTo("2027-T1");
        String canonique = ReexamenAcces.canonique(Set.of("SBX_CER_EXECUTION", "NOT_INBOX_READ"));
        assertThat(canonique).isEqualTo("NOT_INBOX_READ\nSBX_CER_EXECUTION\n");
        assertThat(ReexamenAcces.liste(canonique)).containsExactly("NOT_INBOX_READ", "SBX_CER_EXECUTION");
        assertThat(ReexamenAcces.liste(null)).isEmpty();
        assertThat(ReexamenAcces.liste("")).isEmpty();
    }

    @Test
    void empreinteStableEtSensibleAuxDecisions() {
        AccessReviewDecision d = new AccessReviewDecision()
            .collaborator(new Collaborator().federatedId("c1"))
            .profilesSnapshot("ROLE_SBX_TESTEUR\n")
            .permissionsSnapshot("SBX_CER_EXECUTION\n")
            .decision(ReviewDecision.MAINTENU)
            .comment(null)
            .decidedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .decidedAt(Instant.parse("2026-10-27T12:00:00Z"));
        String empreinte = ReexamenAcces.empreinte("2026-T4", List.of(d, d));
        assertThat(empreinte).matches("^[0-9a-f]{64}$").isEqualTo(ReexamenAcces.empreinte("2026-T4", List.of(d, d)));
        assertThat(ReexamenAcces.empreinte("2026-T3", List.of(d))).isNotEqualTo(empreinte);
        d.decision(ReviewDecision.REVOQUE);
        assertThat(ReexamenAcces.empreinte("2026-T4", List.of(d))).isNotEqualTo(empreinte);
    }

    @Test
    void constructeurEtAlgorithmeIndisponible() throws Exception {
        var constructeur = ReexamenAcces.class.getDeclaredConstructor();
        constructeur.setAccessible(true);
        assertThat(constructeur.newInstance()).isNotNull();
        try (var digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new NoSuchAlgorithmException());
            assertThatThrownBy(() -> ReexamenAcces.empreinte("2026-T4", List.of())).isInstanceOf(IllegalStateException.class);
        }
    }
}
