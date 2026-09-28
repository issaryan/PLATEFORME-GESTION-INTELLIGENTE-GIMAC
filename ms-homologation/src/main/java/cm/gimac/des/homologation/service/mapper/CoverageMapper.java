package cm.gimac.des.homologation.service.mapper;

import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.service.dto.CoverageDTO;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Coverage} and its DTO {@link CoverageDTO}.
 */
@Mapper(componentModel = "spring")
public interface CoverageMapper extends EntityMapper<CoverageDTO, Coverage> {
    @Mapping(target = "liaison", source = "liaison", qualifiedByName = "liaisonLiaisonCode")
    CoverageDTO toDto(Coverage s);

    @Named("liaisonLiaisonCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "liaisonCode", source = "liaisonCode")
    LiaisonDTO toDtoLiaisonLiaisonCode(Liaison liaison);
}
