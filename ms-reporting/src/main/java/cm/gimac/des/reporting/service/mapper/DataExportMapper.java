package cm.gimac.des.reporting.service.mapper;

import cm.gimac.des.reporting.domain.DataExport;
import cm.gimac.des.reporting.service.dto.DataExportDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DataExport} and its DTO {@link DataExportDTO}.
 */
@Mapper(componentModel = "spring")
public interface DataExportMapper extends EntityMapper<DataExportDTO, DataExport> {}
