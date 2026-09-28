package cm.gimac.des.audit.service.mapper;

import static cm.gimac.des.audit.domain.MerkleNodeAsserts.*;
import static cm.gimac.des.audit.domain.MerkleNodeTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MerkleNodeMapperTest {

    private MerkleNodeMapper merkleNodeMapper;

    @BeforeEach
    void setUp() {
        merkleNodeMapper = new MerkleNodeMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMerkleNodeSample1();
        var actual = merkleNodeMapper.toEntity(merkleNodeMapper.toDto(expected));
        assertMerkleNodeAllPropertiesEquals(expected, actual);
    }
}
