package cm.gimac.des.participant.service.mapper;

import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.service.dto.AdhesionPieceDTO;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AdhesionPiece} and its DTO {@link AdhesionPieceDTO}.
 */
@Mapper(componentModel = "spring")
public interface AdhesionPieceMapper extends EntityMapper<AdhesionPieceDTO, AdhesionPiece> {
    @Mapping(target = "institution", source = "institution", qualifiedByName = "institutionName")
    AdhesionPieceDTO toDto(AdhesionPiece s);

    @Named("institutionName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    InstitutionDTO toDtoInstitutionName(Institution institution);
}
