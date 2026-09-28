package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.domain.CrisisMember;
import cm.gimac.des.incident.service.dto.CrisisCellDTO;
import cm.gimac.des.incident.service.dto.CrisisMemberDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CrisisMember} and its DTO {@link CrisisMemberDTO}.
 */
@Mapper(componentModel = "spring")
public interface CrisisMemberMapper extends EntityMapper<CrisisMemberDTO, CrisisMember> {
    @Mapping(target = "cell", source = "cell", qualifiedByName = "crisisCellReference")
    CrisisMemberDTO toDto(CrisisMember s);

    @Named("crisisCellReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    CrisisCellDTO toDtoCrisisCellReference(CrisisCell crisisCell);
}
