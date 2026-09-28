package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.AlertSource;
import cm.gimac.des.monitoring.service.dto.AlertSourceDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AlertSource} and its DTO {@link AlertSourceDTO}.
 */
@Mapper(componentModel = "spring")
public interface AlertSourceMapper extends EntityMapper<AlertSourceDTO, AlertSource> {}
