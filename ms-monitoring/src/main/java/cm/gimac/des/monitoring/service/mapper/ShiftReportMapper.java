package cm.gimac.des.monitoring.service.mapper;

import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.service.dto.ShiftDTO;
import cm.gimac.des.monitoring.service.dto.ShiftReportDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ShiftReport} and its DTO {@link ShiftReportDTO}.
 */
@Mapper(componentModel = "spring")
public interface ShiftReportMapper extends EntityMapper<ShiftReportDTO, ShiftReport> {
    @Mapping(target = "shift", source = "shift", qualifiedByName = "shiftReference")
    @Mapping(target = "supersedes", source = "supersedes", qualifiedByName = "shiftReportReference")
    ShiftReportDTO toDto(ShiftReport s);

    @Named("shiftReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    ShiftDTO toDtoShiftReference(Shift shift);

    @Named("shiftReportReference")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    ShiftReportDTO toDtoShiftReportReference(ShiftReport shiftReport);
}
