package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.MeasureWindow;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.service.dto.MeasureWindowDTO;
import cm.gimac.des.monitoring.service.dto.SupervisedLinkDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link MeasureWindow} and its DTO {@link MeasureWindowDTO}.
 */
@Mapper(componentModel = "spring")
public interface MeasureWindowMapper extends EntityMapper<MeasureWindowDTO, MeasureWindow> {
    @Mapping(target = "link", source = "link", qualifiedByName = "supervisedLinkCode")
    MeasureWindowDTO toDto(MeasureWindow s);

    @Named("supervisedLinkCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    SupervisedLinkDTO toDtoSupervisedLinkCode(SupervisedLink supervisedLink);
}
