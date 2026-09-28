package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.DeduplicationEntry;
import cm.gimac.des.certification.service.dto.DeduplicationEntryDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DeduplicationEntry} and its DTO {@link DeduplicationEntryDTO}.
 */
@Mapper(componentModel = "spring")
public interface DeduplicationEntryMapper extends EntityMapper<DeduplicationEntryDTO, DeduplicationEntry> {}
