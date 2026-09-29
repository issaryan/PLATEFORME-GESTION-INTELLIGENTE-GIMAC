package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.AdhesionPieceTestSamples.*;
import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AdhesionPieceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AdhesionPiece.class);
        AdhesionPiece adhesionPiece1 = getAdhesionPieceSample1();
        AdhesionPiece adhesionPiece2 = new AdhesionPiece();
        assertThat(adhesionPiece1).isNotEqualTo(adhesionPiece2);

        adhesionPiece2.setId(adhesionPiece1.getId());
        assertThat(adhesionPiece1).isEqualTo(adhesionPiece2);

        adhesionPiece2 = getAdhesionPieceSample2();
        assertThat(adhesionPiece1).isNotEqualTo(adhesionPiece2);
    }

    @Test
    void institutionTest() {
        AdhesionPiece adhesionPiece = getAdhesionPieceRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        adhesionPiece.setInstitution(institutionBack);
        assertThat(adhesionPiece.getInstitution()).isEqualTo(institutionBack);

        adhesionPiece.institution(null);
        assertThat(adhesionPiece.getInstitution()).isNull();
    }
}
