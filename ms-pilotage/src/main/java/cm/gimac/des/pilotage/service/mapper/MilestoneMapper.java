package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Milestone;
import cm.gimac.des.pilotage.domain.Project;
import cm.gimac.des.pilotage.service.dto.MilestoneDTO;
import cm.gimac.des.pilotage.service.dto.ProjectDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Milestone} and its DTO {@link MilestoneDTO}.
 */
@Mapper(componentModel = "spring")
public interface MilestoneMapper extends EntityMapper<MilestoneDTO, Milestone> {
    @Mapping(target = "project", source = "project", qualifiedByName = "projectTitle")
    @Mapping(target = "predecessors", source = "predecessors", qualifiedByName = "milestoneLabelSet")
    @Mapping(target = "successors", source = "successors", qualifiedByName = "milestoneLabelSet")
    MilestoneDTO toDto(Milestone s);

    @Mapping(target = "removePredecessors", ignore = true)
    @Mapping(target = "successors", ignore = true)
    @Mapping(target = "removeSuccessors", ignore = true)
    Milestone toEntity(MilestoneDTO milestoneDTO);

    @Named("projectTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    ProjectDTO toDtoProjectTitle(Project project);

    @Named("milestoneLabel")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "label", source = "label")
    MilestoneDTO toDtoMilestoneLabel(Milestone milestone);

    @Named("milestoneLabelSet")
    default Set<MilestoneDTO> toDtoMilestoneLabelSet(Set<Milestone> milestone) {
        return milestone.stream().map(this::toDtoMilestoneLabel).collect(Collectors.toSet());
    }
}
