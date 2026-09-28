package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Project;
import cm.gimac.des.pilotage.service.dto.ProjectDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Project} and its DTO {@link ProjectDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProjectMapper extends EntityMapper<ProjectDTO, Project> {
    @Mapping(target = "dependencies", source = "dependencies", qualifiedByName = "projectTitleSet")
    @Mapping(target = "dependents", source = "dependents", qualifiedByName = "projectTitleSet")
    ProjectDTO toDto(Project s);

    @Mapping(target = "removeDependencies", ignore = true)
    @Mapping(target = "dependents", ignore = true)
    @Mapping(target = "removeDependents", ignore = true)
    Project toEntity(ProjectDTO projectDTO);

    @Named("projectTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    ProjectDTO toDtoProjectTitle(Project project);

    @Named("projectTitleSet")
    default Set<ProjectDTO> toDtoProjectTitleSet(Set<Project> project) {
        return project.stream().map(this::toDtoProjectTitle).collect(Collectors.toSet());
    }
}
