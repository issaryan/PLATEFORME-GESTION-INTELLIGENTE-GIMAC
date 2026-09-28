package cm.gimac.des.audit.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MerkleNodeDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MerkleNodeDTO.class);
        MerkleNodeDTO merkleNodeDTO1 = new MerkleNodeDTO();
        merkleNodeDTO1.setId(UUID.randomUUID());
        MerkleNodeDTO merkleNodeDTO2 = new MerkleNodeDTO();
        assertThat(merkleNodeDTO1).isNotEqualTo(merkleNodeDTO2);
        merkleNodeDTO2.setId(merkleNodeDTO1.getId());
        assertThat(merkleNodeDTO1).isEqualTo(merkleNodeDTO2);
        merkleNodeDTO2.setId(UUID.randomUUID());
        assertThat(merkleNodeDTO1).isNotEqualTo(merkleNodeDTO2);
        merkleNodeDTO1.setId(null);
        assertThat(merkleNodeDTO1).isNotEqualTo(merkleNodeDTO2);
    }
}
