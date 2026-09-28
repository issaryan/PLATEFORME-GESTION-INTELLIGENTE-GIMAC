package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeduplicationEntryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DeduplicationEntryDTO.class);
        DeduplicationEntryDTO deduplicationEntryDTO1 = new DeduplicationEntryDTO();
        deduplicationEntryDTO1.setId(UUID.randomUUID());
        DeduplicationEntryDTO deduplicationEntryDTO2 = new DeduplicationEntryDTO();
        assertThat(deduplicationEntryDTO1).isNotEqualTo(deduplicationEntryDTO2);
        deduplicationEntryDTO2.setId(deduplicationEntryDTO1.getId());
        assertThat(deduplicationEntryDTO1).isEqualTo(deduplicationEntryDTO2);
        deduplicationEntryDTO2.setId(UUID.randomUUID());
        assertThat(deduplicationEntryDTO1).isNotEqualTo(deduplicationEntryDTO2);
        deduplicationEntryDTO1.setId(null);
        assertThat(deduplicationEntryDTO1).isNotEqualTo(deduplicationEntryDTO2);
    }
}
