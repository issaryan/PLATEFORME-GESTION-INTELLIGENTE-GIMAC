package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.Dimension;
import cm.gimac.des.reporting.service.dto.DimensionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Dimension} and its DTO {@link DimensionDTO}.
 */
@Mapper(componentModel = "spring")
public interface DimensionMapper extends EntityMapper<DimensionDTO, Dimension> {}
