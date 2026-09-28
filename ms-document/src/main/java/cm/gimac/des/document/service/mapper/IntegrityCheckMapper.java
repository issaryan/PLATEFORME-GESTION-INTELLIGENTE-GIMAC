package cm.gimac.des.document.service.mapper;

import cm.gimac.des.document.domain.IntegrityCheck;
import cm.gimac.des.document.service.dto.IntegrityCheckDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link IntegrityCheck} and its DTO {@link IntegrityCheckDTO}.
 */
@Mapper(componentModel = "spring")
public interface IntegrityCheckMapper extends EntityMapper<IntegrityCheckDTO, IntegrityCheck> {}
