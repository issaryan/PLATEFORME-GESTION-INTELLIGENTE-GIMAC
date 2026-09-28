package cm.gimac.des.notification.service.mapper;

import cm.gimac.des.notification.domain.AcknowledgementPolicy;
import cm.gimac.des.notification.service.dto.AcknowledgementPolicyDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AcknowledgementPolicy} and its DTO {@link AcknowledgementPolicyDTO}.
 */
@Mapper(componentModel = "spring")
public interface AcknowledgementPolicyMapper extends EntityMapper<AcknowledgementPolicyDTO, AcknowledgementPolicy> {}
