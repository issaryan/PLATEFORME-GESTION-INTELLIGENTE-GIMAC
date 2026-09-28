package cm.gimac.des.incident.service.mapper;

import cm.gimac.des.incident.domain.QualificationMatrix;
import cm.gimac.des.incident.service.dto.QualificationMatrixDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link QualificationMatrix} and its DTO {@link QualificationMatrixDTO}.
 */
@Mapper(componentModel = "spring")
public interface QualificationMatrixMapper extends EntityMapper<QualificationMatrixDTO, QualificationMatrix> {}
