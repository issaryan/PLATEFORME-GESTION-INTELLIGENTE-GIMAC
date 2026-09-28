package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.ChangeRecord;
import cm.gimac.des.certification.service.dto.ChangeRecordDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ChangeRecord} and its DTO {@link ChangeRecordDTO}.
 */
@Mapper(componentModel = "spring")
public interface ChangeRecordMapper extends EntityMapper<ChangeRecordDTO, ChangeRecord> {}
