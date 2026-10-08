package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.DefinitionFonction;
import cm.gimac.des.homologation.domain.HomologationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tâches à prendre (D-94, §8.8) : référence d'une fonction à affecter, permissions d'une fonction du catalogue. */
class PrisesFonctionsTest {

    @Test
    @DisplayName("Fonction d'une référence <réf.>/AFFECTER/<FONCTION> ; toute autre référence : aucune")
    void fonctionDe() {
        assertThat(PrisesFonctions.fonctionDe("LIA-2026-001/AFFECTER/DDRM")).contains("DDRM");
        assertThat(PrisesFonctions.fonctionDe("LIA-2026-001/AFFECTER/ddrm")).isEmpty();
        assertThat(PrisesFonctions.fonctionDe("LIA-2026-001/3/R")).isEmpty();
        assertThat(PrisesFonctions.fonctionDe("/AFFECTER/DDRM")).isEmpty();
        assertThat(PrisesFonctions.fonctionDe(null)).isEmpty();
    }

    @Test
    @DisplayName("Permissions d'une fonction du catalogue : codes distincts, majuscules, 20 au plus ; modifiables avec trace")
    void permissions() {
        DefinitionFonction f = DefinitionFonction.creer("JURISTE", "Juriste", List.of("ROLE_JURISTE"), false, List.of("HOM_A", "HOM_A", " HOM_B "), null, Instant.EPOCH);
        assertThat(f.getPermissions()).containsExactly("HOM_A", "HOM_B");
        assertThat(f.definition().permissions()).containsExactly("HOM_A", "HOM_B");
        assertThat(f.modifier(null, null, null, null, List.of("HOM_C"), null, Instant.EPOCH)).containsExactly("permissions");
        assertThat(f.modifier(null, null, null, null, null, null, Instant.EPOCH)).isEmpty();
        assertThat(f.getPermissions()).containsExactly("HOM_C");
        assertThatThrownBy(() -> f.modifier(null, null, null, null, List.of("hom_minuscule"), null, Instant.EPOCH))
            .isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_FONCTION_PERMISSION_INVALIDE"));
        assertThat(DefinitionFonction.creer("AUTRE", "Autre", List.of("ROLE_X"), false, null, Instant.EPOCH).getPermissions()).isEmpty();
    }
}
