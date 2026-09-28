package cm.gimac.des.audit.service.mapper;

import cm.gimac.des.audit.domain.ConsistencyProof;
import cm.gimac.des.audit.service.dto.ConsistencyProofDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ConsistencyProof} and its DTO {@link ConsistencyProofDTO}.
 */
@Mapper(componentModel = "spring")
public interface ConsistencyProofMapper extends EntityMapper<ConsistencyProofDTO, ConsistencyProof> {}
