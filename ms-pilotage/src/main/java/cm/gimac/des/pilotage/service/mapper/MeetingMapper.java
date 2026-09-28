package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Meeting;
import cm.gimac.des.pilotage.service.dto.MeetingDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Meeting} and its DTO {@link MeetingDTO}.
 */
@Mapper(componentModel = "spring")
public interface MeetingMapper extends EntityMapper<MeetingDTO, Meeting> {}
