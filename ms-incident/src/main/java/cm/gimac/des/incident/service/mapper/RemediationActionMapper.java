package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.CrisisDecision;
import cm.gimac.des.incident.domain.Incident;
import cm.gimac.des.incident.domain.RemediationAction;
import cm.gimac.des.incident.service.dto.CrisisDecisionDTO;
import cm.gimac.des.incident.service.dto.IncidentDTO;
import cm.gimac.des.incident.service.dto.RemediationActionDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RemediationAction} and its DTO {@link RemediationActionDTO}.
 */
@Mapper(componentModel = "spring")
public interface RemediationActionMapper extends EntityMapper<RemediationActionDTO, RemediationAction> {
    @Mapping(target = "incident", source = "incident", qualifiedByName = "incidentId")
    @Mapping(target = "crisisDecision", source = "crisisDecision", qualifiedByName = "crisisDecisionId")
    RemediationActionDTO toDto(RemediationAction s);

    @Named("incidentId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    IncidentDTO toDtoIncidentId(Incident incident);

    @Named("crisisDecisionId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    CrisisDecisionDTO toDtoCrisisDecisionId(CrisisDecision crisisDecision);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
