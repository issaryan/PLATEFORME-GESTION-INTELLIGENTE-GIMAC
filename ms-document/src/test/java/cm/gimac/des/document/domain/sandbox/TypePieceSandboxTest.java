package cm.gimac.des.document.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.domain.sandbox.TypePieceSandbox.ReglePan;
import java.time.Instant;
import java.time.Period;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Type de pièce de la sandbox : déposants admis, fin de conservation, plancher de rétention des preuves. */
class TypePieceSandboxTest {

    static TypePieceSandbox type(Period retention, boolean probante) {
        return new TypePieceSandbox("T", Map.of("fr", "T"), retention, probante, Set.of("sbx-trace"), ReglePan.QUARANTAINE, 10, true, 3);
    }

    @Test
    @DisplayName("Déposant admis, fin de conservation calendaire, nouvelle version à la modification")
    void regles() {
        TypePieceSandbox t = type(Period.ofYears(10), true);
        assertThat(t.autorise("sbx-trace")).isTrue();
        assertThat(t.autorise("sbx-campagne")).isFalse();
        assertThat(t.autorise(null)).isFalse();
        assertThat(t.conserveeJusquAu(Instant.parse("2028-02-29T00:00:00Z"))).isEqualTo(Instant.parse("2038-02-28T00:00:00Z"));
        TypePieceSandbox m = t.avecRetention(Period.ofYears(12));
        assertThat(m.version()).isEqualTo(4);
        assertThat(m.retention()).isEqualTo(Period.ofYears(12));
        assertThatThrownBy(() -> new TypePieceSandbox(null, Map.of(), Period.ofYears(1), false, Set.of(), ReglePan.REFUS, 1, true, 1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rétention : positive ; dix ans au moins pour un type probant (P3653D admis, P119M refusé)")
    void retention() {
        assertThat(TypePieceSandbox.refusRetention(Period.ofYears(10), true)).isNull();
        assertThat(TypePieceSandbox.refusRetention(Period.ofDays(3653), true)).isNull();
        assertThat(TypePieceSandbox.refusRetention(Period.ofMonths(119), true)).isEqualTo("DOC_SBX_RETENTION_INSUFFISANTE");
        assertThat(TypePieceSandbox.refusRetention(Period.ofMonths(1), false)).isNull();
        assertThat(TypePieceSandbox.refusRetention(Period.ZERO, false)).isEqualTo("DOC_SBX_RETENTION_INVALIDE");
        assertThat(TypePieceSandbox.refusRetention(Period.ofYears(-1), false)).isEqualTo("DOC_SBX_RETENTION_INVALIDE");
        assertThat(TypePieceSandbox.refusRetention(null, false)).isEqualTo("DOC_SBX_RETENTION_INVALIDE");
    }
}
