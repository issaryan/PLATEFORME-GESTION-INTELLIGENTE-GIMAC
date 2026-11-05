package cm.gimac.des.participant.web.rest.lot1;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.application.port.out.RegistreLecture.Correspondant;
import cm.gimac.des.participant.application.usecase.RegistreSandboxQueries.Fiche;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat de la fiche lue par la sandbox (US-EXI-21, §6.6) : la charge d'exemple contrats/fiche-sandbox.exemple.json
 * est celle que SBX-RACCORDEMENT lit dans son propre test de contrat (même fichier dans les deux dépôts).
 */
class ContratFicheSandboxTest {

    @Test
    @DisplayName("La fiche sérialisée est exactement la charge d'exemple partagée avec la sandbox, services souscrits compris")
    void ficheConformeALaChargeDExemple() throws Exception {
        JsonMapper json = JsonMapper.builder().build();
        Fiche fiche = new Fiche(
            "99001",
            "Banque fictive 99001",
            "ACTIF",
            4,
            List.of(
                new Correspondant(
                    UUID.fromString("6f1c2a3e-1111-4a6b-9c1d-000000000001"),
                    UUID.fromString("6f1c2a3e-2222-4a6b-9c1d-000000000002"),
                    "Awa Ndiaye",
                    "MONETIQUE"
                )
            ),
            List.of("CRT.ATM", "ISO.ACC", "W2W")
        );
        try (InputStream exemple = getClass().getResourceAsStream("/contrats/fiche-sandbox.exemple.json")) {
            assertThat(json.readTree(json.writeValueAsString(fiche))).isEqualTo(json.readTree(exemple));
        }
    }
}
