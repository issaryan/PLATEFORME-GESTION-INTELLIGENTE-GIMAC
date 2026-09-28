package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.CockpitView;
import cm.gimac.des.reporting.domain.IndicatorDefinition;
import cm.gimac.des.reporting.service.dto.CockpitViewDTO;
import cm.gimac.des.reporting.service.dto.IndicatorDefinitionDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CockpitView} and its DTO {@link CockpitViewDTO}.
 */
@Mapper(componentModel = "spring")
public interface CockpitViewMapper extends EntityMapper<CockpitViewDTO, CockpitView> {
    @Mapping(target = "indicators", source = "indicators", qualifiedByName = "indicatorDefinitionCodeSet")
    CockpitViewDTO toDto(CockpitView s);

    @Mapping(target = "removeIndicators", ignore = true)
    CockpitView toEntity(CockpitViewDTO cockpitViewDTO);

    @Named("indicatorDefinitionCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    IndicatorDefinitionDTO toDtoIndicatorDefinitionCode(IndicatorDefinition indicatorDefinition);

    @Named("indicatorDefinitionCodeSet")
    default Set<IndicatorDefinitionDTO> toDtoIndicatorDefinitionCodeSet(Set<IndicatorDefinition> indicatorDefinition) {
        return indicatorDefinition.stream().map(this::toDtoIndicatorDefinitionCode).collect(Collectors.toSet());
    }
}
