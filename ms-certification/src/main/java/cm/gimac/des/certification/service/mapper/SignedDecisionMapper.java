package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.SignedDecision;
import cm.gimac.des.certification.service.dto.QuitusDTO;
import cm.gimac.des.certification.service.dto.SignedDecisionDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SignedDecision} and its DTO {@link SignedDecisionDTO}.
 */
@Mapper(componentModel = "spring")
public interface SignedDecisionMapper extends EntityMapper<SignedDecisionDTO, SignedDecision> {
    @Mapping(target = "quitus", source = "quitus", qualifiedByName = "quitusId")
    SignedDecisionDTO toDto(SignedDecision s);

    @Named("quitusId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    QuitusDTO toDtoQuitusId(Quitus quitus);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
