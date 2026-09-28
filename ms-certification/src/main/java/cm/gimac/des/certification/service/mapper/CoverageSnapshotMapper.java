package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CoverageSnapshot;
import cm.gimac.des.certification.service.dto.CoverageSnapshotDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CoverageSnapshot} and its DTO {@link CoverageSnapshotDTO}.
 */
@Mapper(componentModel = "spring")
public interface CoverageSnapshotMapper extends EntityMapper<CoverageSnapshotDTO, CoverageSnapshot> {}
