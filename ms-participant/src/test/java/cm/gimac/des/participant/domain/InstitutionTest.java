package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class InstitutionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Institution.class);
        Institution institution1 = getInstitutionSample1();
        Institution institution2 = new Institution();
        assertThat(institution1).isNotEqualTo(institution2);

        institution2.setId(institution1.getId());
        assertThat(institution1).isEqualTo(institution2);

        institution2 = getInstitutionSample2();
        assertThat(institution1).isNotEqualTo(institution2);
    }

    @Test
    void settlementBankTest() {
        Institution institution = getInstitutionRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        institution.setSettlementBank(institutionBack);
        assertThat(institution.getSettlementBank()).isEqualTo(institutionBack);

        institution.settlementBank(null);
        assertThat(institution.getSettlementBank()).isNull();
    }

    @Test
    void sponsorBankTest() {
        Institution institution = getInstitutionRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        institution.setSponsorBank(institutionBack);
        assertThat(institution.getSponsorBank()).isEqualTo(institutionBack);

        institution.sponsorBank(null);
        assertThat(institution.getSponsorBank()).isNull();
    }
}
