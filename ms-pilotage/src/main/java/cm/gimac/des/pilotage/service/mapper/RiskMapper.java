package cm.gimac.des.pilotage.service.mapper;

import cm.gimac.des.pilotage.domain.Project;
import cm.gimac.des.pilotage.domain.Risk;
import cm.gimac.des.pilotage.service.dto.ProjectDTO;
import cm.gimac.des.pilotage.service.dto.RiskDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Risk} and its DTO {@link RiskDTO}.
 */
@Mapper(componentModel = "spring")
public interface RiskMapper extends EntityMapper<RiskDTO, Risk> {
    @Mapping(target = "project", source = "project", qualifiedByName = "projectTitle")
    RiskDTO toDto(Risk s);

    @Named("projectTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    ProjectDTO toDtoProjectTitle(Project project);
}
