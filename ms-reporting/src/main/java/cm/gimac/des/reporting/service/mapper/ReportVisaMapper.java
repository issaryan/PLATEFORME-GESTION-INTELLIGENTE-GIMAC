package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.PeriodicReport;
import cm.gimac.des.reporting.domain.ReportVisa;
import cm.gimac.des.reporting.service.dto.PeriodicReportDTO;
import cm.gimac.des.reporting.service.dto.ReportVisaDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ReportVisa} and its DTO {@link ReportVisaDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReportVisaMapper extends EntityMapper<ReportVisaDTO, ReportVisa> {
    @Mapping(target = "report", source = "report", qualifiedByName = "periodicReportId")
    ReportVisaDTO toDto(ReportVisa s);

    @Named("periodicReportId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PeriodicReportDTO toDtoPeriodicReportId(PeriodicReport periodicReport);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
