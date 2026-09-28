package cm.gimac.des.audit.domain;

import static cm.gimac.des.audit.domain.MerkleNodeTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MerkleNodeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MerkleNode.class);
        MerkleNode merkleNode1 = getMerkleNodeSample1();
        MerkleNode merkleNode2 = new MerkleNode();
        assertThat(merkleNode1).isNotEqualTo(merkleNode2);

        merkleNode2.setId(merkleNode1.getId());
        assertThat(merkleNode1).isEqualTo(merkleNode2);

        merkleNode2 = getMerkleNodeSample2();
        assertThat(merkleNode1).isNotEqualTo(merkleNode2);
    }
}
