package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ReplayCursor;
import cm.gimac.des.homologation.service.dto.ReplayCursorDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReplayCursor} and its DTO {@link ReplayCursorDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReplayCursorMapper extends EntityMapper<ReplayCursorDTO, ReplayCursor> {}
