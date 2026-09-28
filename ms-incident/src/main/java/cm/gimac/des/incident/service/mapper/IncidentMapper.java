package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.Incident;
import cm.gimac.des.incident.domain.QualificationMatrix;
import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.service.dto.IncidentDTO;
import cm.gimac.des.incident.service.dto.QualificationMatrixDTO;
import cm.gimac.des.incident.service.dto.TicketDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Incident} and its DTO {@link IncidentDTO}.
 */
@Mapper(componentModel = "spring")
public interface IncidentMapper extends EntityMapper<IncidentDTO, Incident> {
    @Mapping(target = "ticket", source = "ticket", qualifiedByName = "ticketTicketNumber")
    @Mapping(target = "matrix", source = "matrix", qualifiedByName = "qualificationMatrixMatrixVersion")
    IncidentDTO toDto(Incident s);

    @Named("ticketTicketNumber")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    TicketDTO toDtoTicketTicketNumber(Ticket ticket);

    @Named("qualificationMatrixMatrixVersion")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "matrixVersion", source = "matrixVersion")
    QualificationMatrixDTO toDtoQualificationMatrixMatrixVersion(QualificationMatrix qualificationMatrix);
}
