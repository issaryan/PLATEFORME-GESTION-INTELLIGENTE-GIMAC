package cm.gimac.des.pilotage.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.domain.processus.ReferenceProcessus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrat entre MS-PILOTAGE et MS-CERTIFICATION : une référence de demande de changement produite par le modèle
 * DEMANDE_CHANGEMENT est une référence de fiche de changement admise au verrou 2 (format de la fiche, CHG-AAAA-N). Le
 * motif est lu dans la source même d'ApprobationPolicy, sans copie qui pourrait diverger.
 */
class ReferenceFicheChangementContratTest {

    static final Path POLITIQUE = Path.of("../ms-certification/src/main/java/cm/gimac/des/certification/domain/service/ApprobationPolicy.java");

    static Pattern referenceFiche() throws IOException {
        Matcher m = Pattern.compile("REFERENCE_FICHE = Pattern\\.compile\\(\"(.+?)\"\\);").matcher(Files.readString(POLITIQUE));
        assertThat(m.find()).as("motif REFERENCE_FICHE d'ApprobationPolicy").isTrue();
        return Pattern.compile(m.group(1).replace("\\\\", "\\"));
    }

    @Test
    @DisplayName("Référence CHG-AAAA-N de MS-PILOTAGE acceptée par ApprobationPolicy de MS-CERTIFICATION")
    void referenceAdmiseAuVerrou2() throws IOException {
        Pattern fiche = referenceFiche();
        for (long sequence : new long[] { 1, 412, 999_999 }) {
            String reference = ReferenceProcessus.suivante(DemandeChangementConfiguration.REFERENCE, DemandeChangementConfiguration.CODE, LocalDate.parse("2026-11-04"), sequence);
            assertThat(reference).matches(fiche);
        }
        assertThat(ReferenceProcessus.suivante(DemandeChangementConfiguration.REFERENCE, DemandeChangementConfiguration.CODE, LocalDate.parse("2026-11-04"), 412)).isEqualTo("CHG-2026-412");
        // L'ancien gabarit daté de la recette du 28/10 n'était pas admis.
        assertThat("CHG-2026-10-28-000001").doesNotMatch(fiche);
    }
}
