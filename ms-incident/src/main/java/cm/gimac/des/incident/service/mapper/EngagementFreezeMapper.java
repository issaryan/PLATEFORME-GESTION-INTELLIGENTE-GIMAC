package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.EngagementFreeze;
import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.service.dto.EngagementFreezeDTO;
import cm.gimac.des.incident.service.dto.TicketDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link EngagementFreeze} and its DTO {@link EngagementFreezeDTO}.
 */
@Mapper(componentModel = "spring")
public interface EngagementFreezeMapper extends EntityMapper<EngagementFreezeDTO, EngagementFreeze> {
    @Mapping(target = "ticket", source = "ticket", qualifiedByName = "ticketTicketNumber")
    EngagementFreezeDTO toDto(EngagementFreeze s);

    @Named("ticketTicketNumber")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    TicketDTO toDtoTicketTicketNumber(Ticket ticket);
}
