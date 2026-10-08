package cm.gimac.des.notification.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comptes d'une institution lus chez MS-IAM (D-93 §11) : filtre défensif sur l'institution et l'état actif. */
class IamComptesInstitutionAdapterTest {

    final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("Tableau : comptes actifs de l'institution demandée seulement, sans doublon")
    void tableau() throws Exception {
        var r = json.readTree(
            "[{\"id\":\"a\",\"institutionId\":\"b-1\",\"actif\":true},{\"id\":\"b\",\"institutionId\":\"b-1\",\"actif\":false}," +
            "{\"id\":\"c\",\"institutionId\":\"b-2\",\"actif\":true},{\"id\":\"a\",\"institutionId\":\"b-1\",\"actif\":true},{\"id\":\"d\",\"actif\":true}]"
        );
        assertThat(IamComptesInstitutionAdapter.retenus(r, "b-1")).containsExactly("a");
    }

    @Test
    @DisplayName("Page de l'annuaire interne (paramètre ignoré par MS-IAM) : aucun compte interne retenu")
    void annuaireInterne() throws Exception {
        var r = json.readTree("{\"contenu\":[{\"id\":\"x\",\"institutionId\":null,\"actif\":true}],\"total\":1}");
        assertThat(IamComptesInstitutionAdapter.retenus(r, "b-1")).isEmpty();
        assertThat(IamComptesInstitutionAdapter.retenus(null, "b-1")).isEmpty();
    }
}
