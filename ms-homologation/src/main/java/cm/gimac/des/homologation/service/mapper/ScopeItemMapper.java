package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.service.dto.ContractualScopeDTO;
import cm.gimac.des.homologation.service.dto.ScopeItemDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ScopeItem} and its DTO {@link ScopeItemDTO}.
 */
@Mapper(componentModel = "spring")
public interface ScopeItemMapper extends EntityMapper<ScopeItemDTO, ScopeItem> {
    @Mapping(target = "scope", source = "scope", qualifiedByName = "contractualScopeId")
    ScopeItemDTO toDto(ScopeItem s);

    @Named("contractualScopeId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ContractualScopeDTO toDtoContractualScopeId(ContractualScope contractualScope);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
