package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.ClosedOperationSnapshot;
import cm.gimac.des.certification.service.dto.ClosedOperationSnapshotDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ClosedOperationSnapshot} and its DTO {@link ClosedOperationSnapshotDTO}.
 */
@Mapper(componentModel = "spring")
public interface ClosedOperationSnapshotMapper extends EntityMapper<ClosedOperationSnapshotDTO, ClosedOperationSnapshot> {}
