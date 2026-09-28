package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.ContractedScope;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.service.dto.ContractedScopeDTO;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ContractedScope} and its DTO {@link ContractedScopeDTO}.
 */
@Mapper(componentModel = "spring")
public interface ContractedScopeMapper extends EntityMapper<ContractedScopeDTO, ContractedScope> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonLiaisonCode")
    ContractedScopeDTO toDto(ContractedScope s);

    @Named("liaisonLiaisonCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "liaisonCode", source = "liaisonCode")
    LiaisonDTO toDtoLiaisonLiaisonCode(Liaison liaison);
}
