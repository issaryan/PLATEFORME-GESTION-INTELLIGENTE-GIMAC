package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReferenceCatalogDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferenceCatalogDTO.class);
        ReferenceCatalogDTO referenceCatalogDTO1 = new ReferenceCatalogDTO();
        referenceCatalogDTO1.setId(UUID.randomUUID());
        ReferenceCatalogDTO referenceCatalogDTO2 = new ReferenceCatalogDTO();
        assertThat(referenceCatalogDTO1).isNotEqualTo(referenceCatalogDTO2);
        referenceCatalogDTO2.setId(referenceCatalogDTO1.getId());
        assertThat(referenceCatalogDTO1).isEqualTo(referenceCatalogDTO2);
        referenceCatalogDTO2.setId(UUID.randomUUID());
        assertThat(referenceCatalogDTO1).isNotEqualTo(referenceCatalogDTO2);
        referenceCatalogDTO1.setId(null);
        assertThat(referenceCatalogDTO1).isNotEqualTo(referenceCatalogDTO2);
    }
}
