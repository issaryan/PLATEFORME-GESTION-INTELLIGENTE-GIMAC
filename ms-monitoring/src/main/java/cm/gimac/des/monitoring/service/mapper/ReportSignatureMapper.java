package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.ReportSignature;
import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.service.dto.ReportSignatureDTO;
import cm.gimac.des.monitoring.service.dto.ShiftReportDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReportSignature} and its DTO {@link ReportSignatureDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReportSignatureMapper extends EntityMapper<ReportSignatureDTO, ReportSignature> {
    @Mapping(target = "report", source = "report", qualifiedByName = "shiftReportReference")
    ReportSignatureDTO toDto(ReportSignature s);

    @Named("shiftReportReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    ShiftReportDTO toDtoShiftReportReference(ShiftReport shiftReport);
}
