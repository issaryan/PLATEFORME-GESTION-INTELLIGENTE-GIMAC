package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Milestone;
import cm.gimac.des.pilotage.domain.MilestoneApproval;
import cm.gimac.des.pilotage.service.dto.MilestoneApprovalDTO;
import cm.gimac.des.pilotage.service.dto.MilestoneDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MilestoneApproval} and its DTO {@link MilestoneApprovalDTO}.
 */
@Mapper(componentModel = "spring")
public interface MilestoneApprovalMapper extends EntityMapper<MilestoneApprovalDTO, MilestoneApproval> {
    @Mapping(target = "milestone", source = "milestone", qualifiedByName = "milestoneLabel")
    MilestoneApprovalDTO toDto(MilestoneApproval s);

    @Named("milestoneLabel")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "label", source = "label")
    MilestoneDTO toDtoMilestoneLabel(Milestone milestone);
}
