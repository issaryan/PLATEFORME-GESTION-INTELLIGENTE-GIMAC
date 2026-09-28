package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.Operation;
import cm.gimac.des.homologation.domain.QrVerification;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import cm.gimac.des.homologation.service.dto.OperationDTO;
import cm.gimac.des.homologation.service.dto.QrVerificationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link QrVerification} and its DTO {@link QrVerificationDTO}.
 */
@Mapper(componentModel = "spring")
public interface QrVerificationMapper extends EntityMapper<QrVerificationDTO, QrVerification> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonLiaisonCode")
    @Mapping(target = "operation", source = "operation", qualifiedByName = "operationOperationCode")
    QrVerificationDTO toDto(QrVerification s);

    @Named("liaisonLiaisonCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "liaisonCode", source = "liaisonCode")
    LiaisonDTO toDtoLiaisonLiaisonCode(Liaison liaison);

    @Named("operationOperationCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "operationCode", source = "operationCode")
    OperationDTO toDtoOperationOperationCode(Operation operation);
}
