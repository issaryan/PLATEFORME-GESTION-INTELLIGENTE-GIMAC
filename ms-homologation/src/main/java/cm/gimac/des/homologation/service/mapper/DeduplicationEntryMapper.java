package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.DeduplicationEntry;
import cm.gimac.des.homologation.service.dto.DeduplicationEntryDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DeduplicationEntry} and its DTO {@link DeduplicationEntryDTO}.
 */
@Mapper(componentModel = "spring")
public interface DeduplicationEntryMapper extends EntityMapper<DeduplicationEntryDTO, DeduplicationEntry> {}
