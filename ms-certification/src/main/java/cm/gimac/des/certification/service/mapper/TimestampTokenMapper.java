package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.TimestampToken;
import cm.gimac.des.certification.service.dto.TimestampTokenDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link TimestampToken} and its DTO {@link TimestampTokenDTO}.
 */
@Mapper(componentModel = "spring")
public interface TimestampTokenMapper extends EntityMapper<TimestampTokenDTO, TimestampToken> {}
