package cm.gimac.des.audit.service.mapper;

import cm.gimac.des.audit.domain.MerkleNode;
import cm.gimac.des.audit.service.dto.MerkleNodeDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MerkleNode} and its DTO {@link MerkleNodeDTO}.
 */
@Mapper(componentModel = "spring")
public interface MerkleNodeMapper extends EntityMapper<MerkleNodeDTO, MerkleNode> {}
