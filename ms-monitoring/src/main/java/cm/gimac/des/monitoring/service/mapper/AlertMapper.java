package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.service.dto.AlertDTO;
import cm.gimac.des.monitoring.service.dto.SupervisedLinkDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Alert} and its DTO {@link AlertDTO}.
 */
@Mapper(componentModel = "spring")
public interface AlertMapper extends EntityMapper<AlertDTO, Alert> {
    @Mapping(target = "link", source = "link", qualifiedByName = "supervisedLinkCode")
    @Mapping(target = "correlationRoot", source = "correlationRoot", qualifiedByName = "alertReference")
    AlertDTO toDto(Alert s);

    @Named("supervisedLinkCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    SupervisedLinkDTO toDtoSupervisedLinkCode(SupervisedLink supervisedLink);

    @Named("alertReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    AlertDTO toDtoAlertReference(Alert alert);
}
