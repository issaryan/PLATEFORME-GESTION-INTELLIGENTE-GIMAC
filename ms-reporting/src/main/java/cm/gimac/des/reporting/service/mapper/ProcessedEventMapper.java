package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.ProcessedEvent;
import cm.gimac.des.reporting.service.dto.ProcessedEventDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ProcessedEvent} and its DTO {@link ProcessedEventDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProcessedEventMapper extends EntityMapper<ProcessedEventDTO, ProcessedEvent> {}
