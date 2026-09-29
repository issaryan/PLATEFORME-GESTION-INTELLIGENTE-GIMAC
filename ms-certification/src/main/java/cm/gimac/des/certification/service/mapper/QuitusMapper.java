package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.service.dto.CertificationCircuitDTO;
import cm.gimac.des.certification.service.dto.QuitusDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Quitus} and its DTO {@link QuitusDTO}.
 */
@Mapper(componentModel = "spring")
public interface QuitusMapper extends EntityMapper<QuitusDTO, Quitus> {
    @Mapping(target = "circuit", source = "circuit", qualifiedByName = "certificationCircuitId")
    QuitusDTO toDto(Quitus s);

    @Named("certificationCircuitId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    CertificationCircuitDTO toDtoCertificationCircuitId(CertificationCircuit certificationCircuit);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
