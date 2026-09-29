package cm.gimac.des.certification.service.mapper;

import cm.gimac.des.certification.domain.CertificationRegisterEntry;
import cm.gimac.des.certification.service.dto.CertificationRegisterEntryDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CertificationRegisterEntry} and its DTO {@link CertificationRegisterEntryDTO}.
 */
@Mapper(componentModel = "spring")
public interface CertificationRegisterEntryMapper extends EntityMapper<CertificationRegisterEntryDTO, CertificationRegisterEntry> {}
