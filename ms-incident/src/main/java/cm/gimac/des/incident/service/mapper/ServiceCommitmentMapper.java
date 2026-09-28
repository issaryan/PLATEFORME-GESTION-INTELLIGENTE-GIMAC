package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.ServiceCommitment;
import cm.gimac.des.incident.service.dto.ServiceCommitmentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ServiceCommitment} and its DTO {@link ServiceCommitmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface ServiceCommitmentMapper extends EntityMapper<ServiceCommitmentDTO, ServiceCommitment> {}
