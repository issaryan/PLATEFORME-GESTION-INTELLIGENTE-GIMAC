package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CommissioningFile;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.service.dto.CommissioningFileDTO;
import cm.gimac.des.certification.service.dto.QuitusDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Quitus} and its DTO {@link QuitusDTO}.
 */
@Mapper(componentModel = "spring")
public interface QuitusMapper extends EntityMapper<QuitusDTO, Quitus> {
    @Mapping(target = "file", source = "file", qualifiedByName = "commissioningFileFileCode")
    QuitusDTO toDto(Quitus s);

    @Named("commissioningFileFileCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fileCode", source = "fileCode")
    CommissioningFileDTO toDtoCommissioningFileFileCode(CommissioningFile commissioningFile);
}
