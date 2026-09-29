package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.CertificationRegisterEntry;
import cm.gimac.des.certification.repository.CertificationRegisterEntryRepository;
import cm.gimac.des.certification.service.CertificationRegisterEntryService;
import cm.gimac.des.certification.service.dto.CertificationRegisterEntryDTO;
import cm.gimac.des.certification.service.mapper.CertificationRegisterEntryMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.CertificationRegisterEntry}.
 */
@Service
@Transactional
public class CertificationRegisterEntryServiceImpl implements CertificationRegisterEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificationRegisterEntryServiceImpl.class);

    private final CertificationRegisterEntryRepository certificationRegisterEntryRepository;

    private final CertificationRegisterEntryMapper certificationRegisterEntryMapper;

    public CertificationRegisterEntryServiceImpl(
        CertificationRegisterEntryRepository certificationRegisterEntryRepository,
        CertificationRegisterEntryMapper certificationRegisterEntryMapper
    ) {
        this.certificationRegisterEntryRepository = certificationRegisterEntryRepository;
        this.certificationRegisterEntryMapper = certificationRegisterEntryMapper;
    }

    @Override
    public CertificationRegisterEntryDTO save(CertificationRegisterEntryDTO certificationRegisterEntryDTO) {
        LOG.debug("Request to save CertificationRegisterEntry : {}", certificationRegisterEntryDTO);
        CertificationRegisterEntry certificationRegisterEntry = certificationRegisterEntryMapper.toEntity(certificationRegisterEntryDTO);
        certificationRegisterEntry = certificationRegisterEntryRepository.save(certificationRegisterEntry);
        return certificationRegisterEntryMapper.toDto(certificationRegisterEntry);
    }

    @Override
    public CertificationRegisterEntryDTO update(CertificationRegisterEntryDTO certificationRegisterEntryDTO) {
        LOG.debug("Request to update CertificationRegisterEntry : {}", certificationRegisterEntryDTO);
        CertificationRegisterEntry certificationRegisterEntry = certificationRegisterEntryMapper.toEntity(certificationRegisterEntryDTO);
        certificationRegisterEntry = certificationRegisterEntryRepository.save(certificationRegisterEntry);
        return certificationRegisterEntryMapper.toDto(certificationRegisterEntry);
    }

    @Override
    public Optional<CertificationRegisterEntryDTO> partialUpdate(CertificationRegisterEntryDTO certificationRegisterEntryDTO) {
        LOG.debug("Request to partially update CertificationRegisterEntry : {}", certificationRegisterEntryDTO);

        return certificationRegisterEntryRepository
            .findById(certificationRegisterEntryDTO.getId())
            .map(existingCertificationRegisterEntry -> {
                certificationRegisterEntryMapper.partialUpdate(existingCertificationRegisterEntry, certificationRegisterEntryDTO);

                return existingCertificationRegisterEntry;
            })
            .map(certificationRegisterEntryRepository::save)
            .map(certificationRegisterEntryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificationRegisterEntryDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all CertificationRegisterEntries");
        return certificationRegisterEntryRepository.findAll(pageable).map(certificationRegisterEntryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CertificationRegisterEntryDTO> findOne(UUID id) {
        LOG.debug("Request to get CertificationRegisterEntry : {}", id);
        return certificationRegisterEntryRepository.findById(id).map(certificationRegisterEntryMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete CertificationRegisterEntry : {}", id);
        certificationRegisterEntryRepository.deleteById(id);
    }
}
