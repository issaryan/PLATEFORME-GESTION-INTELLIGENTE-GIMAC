package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.service.dto.ContractualScopeDTO;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ContractualScope} and its DTO {@link ContractualScopeDTO}.
 */
@Mapper(componentModel = "spring")
public interface ContractualScopeMapper extends EntityMapper<ContractualScopeDTO, ContractualScope> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonId")
    ContractualScopeDTO toDto(ContractualScope s);

    @Named("liaisonId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    LiaisonDTO toDtoLiaisonId(Liaison liaison);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
