package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CommissioningFile;
import cm.gimac.des.certification.domain.EvidenceItem;
import cm.gimac.des.certification.service.dto.CommissioningFileDTO;
import cm.gimac.des.certification.service.dto.EvidenceItemDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link EvidenceItem} and its DTO {@link EvidenceItemDTO}.
 */
@Mapper(componentModel = "spring")
public interface EvidenceItemMapper extends EntityMapper<EvidenceItemDTO, EvidenceItem> {
    @Mapping(target = "file", source = "file", qualifiedByName = "commissioningFileFileCode")
    EvidenceItemDTO toDto(EvidenceItem s);

    @Named("commissioningFileFileCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fileCode", source = "fileCode")
    CommissioningFileDTO toDtoCommissioningFileFileCode(CommissioningFile commissioningFile);
}
