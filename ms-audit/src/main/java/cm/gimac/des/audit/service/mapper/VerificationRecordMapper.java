package cm.gimac.des.audit.service.mapper;

import cm.gimac.des.audit.domain.InclusionProof;
import cm.gimac.des.audit.domain.VerificationRecord;
import cm.gimac.des.audit.service.dto.InclusionProofDTO;
import cm.gimac.des.audit.service.dto.VerificationRecordDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link VerificationRecord} and its DTO {@link VerificationRecordDTO}.
 */
@Mapper(componentModel = "spring")
public interface VerificationRecordMapper extends EntityMapper<VerificationRecordDTO, VerificationRecord> {
    @Mapping(target = "proof", source = "proof", qualifiedByName = "inclusionProofId")
    VerificationRecordDTO toDto(VerificationRecord s);

    @Named("inclusionProofId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    InclusionProofDTO toDtoInclusionProofId(InclusionProof inclusionProof);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
