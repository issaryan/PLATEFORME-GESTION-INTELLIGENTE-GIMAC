package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.ProcessedEvent;
import cm.gimac.des.pilotage.service.dto.ProcessedEventDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ProcessedEvent} and its DTO {@link ProcessedEventDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProcessedEventMapper extends EntityMapper<ProcessedEventDTO, ProcessedEvent> {}
