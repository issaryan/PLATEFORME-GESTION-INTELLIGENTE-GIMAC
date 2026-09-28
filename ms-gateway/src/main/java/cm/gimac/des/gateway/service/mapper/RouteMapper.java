package cm.gimac.des.gateway.service.mapper;

import cm.gimac.des.gateway.domain.PresentationLayer;
import cm.gimac.des.gateway.domain.Route;
import cm.gimac.des.gateway.service.dto.PresentationLayerDTO;
import cm.gimac.des.gateway.service.dto.RouteDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Route} and its DTO {@link RouteDTO}.
 */
@Mapper(componentModel = "spring")
public interface RouteMapper extends EntityMapper<RouteDTO, Route> {
    @Mapping(target = "layer", source = "layer", qualifiedByName = "presentationLayerName")
    RouteDTO toDto(Route s);

    @Named("presentationLayerName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    PresentationLayerDTO toDtoPresentationLayerName(PresentationLayer presentationLayer);
}
