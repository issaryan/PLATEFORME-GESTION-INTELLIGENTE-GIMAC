package cm.gimac.des.gateway.service.mapper;

import cm.gimac.des.gateway.domain.PresentationLayer;
import cm.gimac.des.gateway.service.dto.PresentationLayerDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PresentationLayer} and its DTO {@link PresentationLayerDTO}.
 */
@Mapper(componentModel = "spring")
public interface PresentationLayerMapper extends EntityMapper<PresentationLayerDTO, PresentationLayer> {}
