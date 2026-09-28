package cm.gimac.des.collaboration.service.mapper;

import cm.gimac.des.collaboration.domain.ProcessedEvent;
import cm.gimac.des.collaboration.service.dto.ProcessedEventDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ProcessedEvent} and its DTO {@link ProcessedEventDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProcessedEventMapper extends EntityMapper<ProcessedEventDTO, ProcessedEvent> {}
