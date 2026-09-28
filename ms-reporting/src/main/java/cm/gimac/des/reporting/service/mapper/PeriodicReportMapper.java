package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.PeriodicReport;
import cm.gimac.des.reporting.service.dto.PeriodicReportDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PeriodicReport} and its DTO {@link PeriodicReportDTO}.
 */
@Mapper(componentModel = "spring")
public interface PeriodicReportMapper extends EntityMapper<PeriodicReportDTO, PeriodicReport> {}
