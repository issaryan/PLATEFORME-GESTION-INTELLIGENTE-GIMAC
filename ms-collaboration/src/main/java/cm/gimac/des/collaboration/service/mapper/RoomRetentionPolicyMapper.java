package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.RoomRetentionPolicy;
import cm.gimac.des.collaboration.service.dto.RoomRetentionPolicyDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RoomRetentionPolicy} and its DTO {@link RoomRetentionPolicyDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomRetentionPolicyMapper extends EntityMapper<RoomRetentionPolicyDTO, RoomRetentionPolicy> {}
