package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.Escalation;
import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.service.dto.EscalationDTO;
import cm.gimac.des.incident.service.dto.TicketDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Escalation} and its DTO {@link EscalationDTO}.
 */
@Mapper(componentModel = "spring")
public interface EscalationMapper extends EntityMapper<EscalationDTO, Escalation> {
    @Mapping(target = "ticket", source = "ticket", qualifiedByName = "ticketTicketNumber")
    EscalationDTO toDto(Escalation s);

    @Named("ticketTicketNumber")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    TicketDTO toDtoTicketTicketNumber(Ticket ticket);
}
