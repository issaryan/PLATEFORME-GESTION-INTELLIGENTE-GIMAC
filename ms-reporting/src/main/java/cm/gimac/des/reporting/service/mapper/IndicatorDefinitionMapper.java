package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.CockpitView;
import cm.gimac.des.reporting.domain.IndicatorDefinition;
import cm.gimac.des.reporting.service.dto.CockpitViewDTO;
import cm.gimac.des.reporting.service.dto.IndicatorDefinitionDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link IndicatorDefinition} and its DTO {@link IndicatorDefinitionDTO}.
 */
@Mapper(componentModel = "spring")
public interface IndicatorDefinitionMapper extends EntityMapper<IndicatorDefinitionDTO, IndicatorDefinition> {
    @Mapping(target = "cockpits", source = "cockpits", qualifiedByName = "cockpitViewCodeSet")
    IndicatorDefinitionDTO toDto(IndicatorDefinition s);

    @Mapping(target = "cockpits", ignore = true)
    @Mapping(target = "removeCockpits", ignore = true)
    IndicatorDefinition toEntity(IndicatorDefinitionDTO indicatorDefinitionDTO);

    @Named("cockpitViewCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    CockpitViewDTO toDtoCockpitViewCode(CockpitView cockpitView);

    @Named("cockpitViewCodeSet")
    default Set<CockpitViewDTO> toDtoCockpitViewCodeSet(Set<CockpitView> cockpitView) {
        return cockpitView.stream().map(this::toDtoCockpitViewCode).collect(Collectors.toSet());
    }
}
