package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.domain.Transition;
import cm.gimac.des.incident.service.dto.TicketDTO;
import cm.gimac.des.incident.service.dto.TransitionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Transition} and its DTO {@link TransitionDTO}.
 */
@Mapper(componentModel = "spring")
public interface TransitionMapper extends EntityMapper<TransitionDTO, Transition> {
    @Mapping(target = "ticket", source = "ticket", qualifiedByName = "ticketTicketNumber")
    TransitionDTO toDto(Transition s);

    @Named("ticketTicketNumber")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    TicketDTO toDtoTicketTicketNumber(Ticket ticket);
}
