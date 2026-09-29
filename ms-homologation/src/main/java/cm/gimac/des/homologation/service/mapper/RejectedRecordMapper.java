package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.RejectedRecord;
import cm.gimac.des.homologation.service.dto.RejectedRecordDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RejectedRecord} and its DTO {@link RejectedRecordDTO}.
 */
@Mapper(componentModel = "spring")
public interface RejectedRecordMapper extends EntityMapper<RejectedRecordDTO, RejectedRecord> {}
