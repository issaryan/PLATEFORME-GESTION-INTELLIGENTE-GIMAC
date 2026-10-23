package cm.gimac.des.homologation.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HOM-06 : ordre des changements de statut d'une institution (version du registre, à défaut date d'effet). */
class StatutInstitutionAppliqueTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");
    static final UUID I = UUID.randomUUID();

    @Test
    @DisplayName("Statuts gelants : suspendu et retiré seulement")
    void gelants() {
        assertThat(StatutInstitutionApplique.gelant("SUSPENDU")).isTrue();
        assertThat(StatutInstitutionApplique.gelant("RETIRE")).isTrue();
        assertThat(StatutInstitutionApplique.gelant("ACTIF")).isFalse();
        assertThat(StatutInstitutionApplique.gelant("EN_HOMOLOGATION")).isFalse();
    }

    @Test
    @DisplayName("Versions connues : seule une version supérieure est appliquée")
    void parVersion() {
        StatutInstitutionApplique s = new StatutInstitutionApplique(I, "SUSPENDU", 5L, T, T);
        assertThat(s.depasse(6L, T.minusSeconds(60))).isTrue();
        assertThat(s.depasse(5L, T.plusSeconds(60))).isFalse();
        assertThat(s.depasse(4L, T.plusSeconds(60))).isFalse();
    }

    @Test
    @DisplayName("Sans version (événement antérieur à D-120) : date d'effet strictement postérieure")
    void parDate() {
        StatutInstitutionApplique s = new StatutInstitutionApplique(I, "SUSPENDU", null, T, T);
        assertThat(s.depasse(null, T.plusSeconds(1))).isTrue();
        assertThat(s.depasse(null, T)).isFalse();
        assertThat(s.depasse(7L, T.minusSeconds(1))).isFalse();
        assertThat(new StatutInstitutionApplique(I, "ACTIF", 3L, T, T).depasse(null, T.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("Ni version ni date comparables : le plus récemment reçu prévaut")
    void sansOrdre() {
        assertThat(new StatutInstitutionApplique(I, "SUSPENDU", null, null, T).depasse(null, T)).isTrue();
        assertThat(new StatutInstitutionApplique(I, "SUSPENDU", 2L, T, T).depasse(null, null)).isTrue();
    }

    @Test
    @DisplayName("Retenir : statut, version et date remplacés, jamais effacés par un événement sans version")
    void retenir() {
        StatutInstitutionApplique s = new StatutInstitutionApplique(I, "SUSPENDU", 5L, T, T);
        s.retenir("ACTIF", null, null, T.plusSeconds(10));
        assertThat(s.getStatut()).isEqualTo("ACTIF");
        assertThat(s.getVersionStatut()).isEqualTo(5L);
        assertThat(s.getDateEffet()).isEqualTo(T);
        assertThat(s.getAppliqueLe()).isEqualTo(T.plusSeconds(10));
        s.retenir("RETIRE", 6L, T.plusSeconds(5), T.plusSeconds(20));
        assertThat(s.getVersionStatut()).isEqualTo(6L);
        assertThat(s.getDateEffet()).isEqualTo(T.plusSeconds(5));
        assertThat(s.getInstitutionId()).isEqualTo(I);
        assertThatThrownBy(() -> s.retenir(null, 7L, T, T)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StatutInstitutionApplique(null, "ACTIF", null, null, T)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new StatutInstitutionApplique(I, null, null, null, T)).isInstanceOf(NullPointerException.class);
        assertThat(new StatutInstitutionApplique().getStatut()).isNull();
    }
}
