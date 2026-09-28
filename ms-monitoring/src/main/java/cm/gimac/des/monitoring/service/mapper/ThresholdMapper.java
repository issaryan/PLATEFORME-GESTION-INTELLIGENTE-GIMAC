package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.domain.Threshold;
import cm.gimac.des.monitoring.service.dto.SupervisedLinkDTO;
import cm.gimac.des.monitoring.service.dto.ThresholdDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Threshold} and its DTO {@link ThresholdDTO}.
 */
@Mapper(componentModel = "spring")
public interface ThresholdMapper extends EntityMapper<ThresholdDTO, Threshold> {
    @Mapping(target = "link", source = "link", qualifiedByName = "supervisedLinkCode")
    ThresholdDTO toDto(Threshold s);

    @Named("supervisedLinkCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    SupervisedLinkDTO toDtoSupervisedLinkCode(SupervisedLink supervisedLink);
}
