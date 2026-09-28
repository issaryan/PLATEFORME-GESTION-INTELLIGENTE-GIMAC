package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.CertificationAttestation;
import cm.gimac.des.certification.repository.CertificationAttestationRepository;
import cm.gimac.des.certification.service.dto.CertificationAttestationDTO;
import cm.gimac.des.certification.service.mapper.CertificationAttestationMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.CertificationAttestation}.
 */
@Service
@Transactional
public class CertificationAttestationService {

    private static final Logger LOG = LoggerFactory.getLogger(CertificationAttestationService.class);

    private final CertificationAttestationRepository certificationAttestationRepository;

    private final CertificationAttestationMapper certificationAttestationMapper;

    public CertificationAttestationService(
        CertificationAttestationRepository certificationAttestationRepository,
        CertificationAttestationMapper certificationAttestationMapper
    ) {
        this.certificationAttestationRepository = certificationAttestationRepository;
        this.certificationAttestationMapper = certificationAttestationMapper;
    }

    /**
     * Save a certificationAttestation.
     *
     * @param certificationAttestationDTO the entity to save.
     * @return the persisted entity.
     */
    public CertificationAttestationDTO save(CertificationAttestationDTO certificationAttestationDTO) {
        LOG.debug("Request to save CertificationAttestation : {}", certificationAttestationDTO);
        CertificationAttestation certificationAttestation = certificationAttestationMapper.toEntity(certificationAttestationDTO);
        certificationAttestation = certificationAttestationRepository.save(certificationAttestation);
        return certificationAttestationMapper.toDto(certificationAttestation);
    }

    /**
     * Update a certificationAttestation.
     *
     * @param certificationAttestationDTO the entity to save.
     * @return the persisted entity.
     */
    public CertificationAttestationDTO update(CertificationAttestationDTO certificationAttestationDTO) {
        LOG.debug("Request to update CertificationAttestation : {}", certificationAttestationDTO);
        CertificationAttestation certificationAttestation = certificationAttestationMapper.toEntity(certificationAttestationDTO);
        certificationAttestation = certificationAttestationRepository.save(certificationAttestation);
        return certificationAttestationMapper.toDto(certificationAttestation);
    }

    /**
     * Partially update a certificationAttestation.
     *
     * @param certificationAttestationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CertificationAttestationDTO> partialUpdate(CertificationAttestationDTO certificationAttestationDTO) {
        LOG.debug("Request to partially update CertificationAttestation : {}", certificationAttestationDTO);

        return certificationAttestationRepository
            .findById(certificationAttestationDTO.getId())
            .map(existingCertificationAttestation -> {
                certificationAttestationMapper.partialUpdate(existingCertificationAttestation, certificationAttestationDTO);

                return existingCertificationAttestation;
            })
            .map(certificationAttestationRepository::save)
            .map(certificationAttestationMapper::toDto);
    }

    /**
     * Get all the certificationAttestations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CertificationAttestationDTO> findAll() {
        LOG.debug("Request to get all CertificationAttestations");
        return certificationAttestationRepository
            .findAll()
            .stream()
            .map(certificationAttestationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the certificationAttestations where CommissioningFile is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CertificationAttestationDTO> findAllWhereCommissioningFileIsNull() {
        LOG.debug("Request to get all certificationAttestations where CommissioningFile is null");
        return StreamSupport.stream(certificationAttestationRepository.findAll().spliterator(), false)
            .filter(certificationAttestation -> certificationAttestation.getCommissioningFile() == null)
            .map(certificationAttestationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one certificationAttestation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CertificationAttestationDTO> findOne(UUID id) {
        LOG.debug("Request to get CertificationAttestation : {}", id);
        return certificationAttestationRepository.findById(id).map(certificationAttestationMapper::toDto);
    }

    /**
     * Delete the certificationAttestation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CertificationAttestation : {}", id);
        certificationAttestationRepository.deleteById(id);
    }
}
