package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.service.dto.TicketDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Ticket} and its DTO {@link TicketDTO}.
 */
@Mapper(componentModel = "spring")
public interface TicketMapper extends EntityMapper<TicketDTO, Ticket> {
    @Mapping(target = "relatedTicket", source = "relatedTicket", qualifiedByName = "ticketTicketNumber")
    TicketDTO toDto(Ticket s);

    @Named("ticketTicketNumber")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    TicketDTO toDtoTicketTicketNumber(Ticket ticket);
}
