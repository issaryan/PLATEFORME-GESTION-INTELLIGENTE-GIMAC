package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.RegularizationAttestation;
import cm.gimac.des.rapprochement.repository.RegularizationAttestationRepository;
import cm.gimac.des.rapprochement.service.dto.RegularizationAttestationDTO;
import cm.gimac.des.rapprochement.service.mapper.RegularizationAttestationMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.RegularizationAttestation}.
 */
@Service
@Transactional
public class RegularizationAttestationService {

    private static final Logger LOG = LoggerFactory.getLogger(RegularizationAttestationService.class);

    private final RegularizationAttestationRepository regularizationAttestationRepository;

    private final RegularizationAttestationMapper regularizationAttestationMapper;

    public RegularizationAttestationService(
        RegularizationAttestationRepository regularizationAttestationRepository,
        RegularizationAttestationMapper regularizationAttestationMapper
    ) {
        this.regularizationAttestationRepository = regularizationAttestationRepository;
        this.regularizationAttestationMapper = regularizationAttestationMapper;
    }

    /**
     * Save a regularizationAttestation.
     *
     * @param regularizationAttestationDTO the entity to save.
     * @return the persisted entity.
     */
    public RegularizationAttestationDTO save(RegularizationAttestationDTO regularizationAttestationDTO) {
        LOG.debug("Request to save RegularizationAttestation : {}", regularizationAttestationDTO);
        RegularizationAttestation regularizationAttestation = regularizationAttestationMapper.toEntity(regularizationAttestationDTO);
        regularizationAttestation = regularizationAttestationRepository.save(regularizationAttestation);
        return regularizationAttestationMapper.toDto(regularizationAttestation);
    }

    /**
     * Update a regularizationAttestation.
     *
     * @param regularizationAttestationDTO the entity to save.
     * @return the persisted entity.
     */
    public RegularizationAttestationDTO update(RegularizationAttestationDTO regularizationAttestationDTO) {
        LOG.debug("Request to update RegularizationAttestation : {}", regularizationAttestationDTO);
        RegularizationAttestation regularizationAttestation = regularizationAttestationMapper.toEntity(regularizationAttestationDTO);
        regularizationAttestation = regularizationAttestationRepository.save(regularizationAttestation);
        return regularizationAttestationMapper.toDto(regularizationAttestation);
    }

    /**
     * Partially update a regularizationAttestation.
     *
     * @param regularizationAttestationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RegularizationAttestationDTO> partialUpdate(RegularizationAttestationDTO regularizationAttestationDTO) {
        LOG.debug("Request to partially update RegularizationAttestation : {}", regularizationAttestationDTO);

        return regularizationAttestationRepository
            .findById(regularizationAttestationDTO.getId())
            .map(existingRegularizationAttestation -> {
                regularizationAttestationMapper.partialUpdate(existingRegularizationAttestation, regularizationAttestationDTO);

                return existingRegularizationAttestation;
            })
            .map(regularizationAttestationRepository::save)
            .map(regularizationAttestationMapper::toDto);
    }

    /**
     * Get all the regularizationAttestations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RegularizationAttestationDTO> findAll() {
        LOG.debug("Request to get all RegularizationAttestations");
        return regularizationAttestationRepository
            .findAll()
            .stream()
            .map(regularizationAttestationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the regularizationAttestations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RegularizationAttestationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return regularizationAttestationRepository.findAllWithEagerRelationships(pageable).map(regularizationAttestationMapper::toDto);
    }

    /**
     * Get one regularizationAttestation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RegularizationAttestationDTO> findOne(UUID id) {
        LOG.debug("Request to get RegularizationAttestation : {}", id);
        return regularizationAttestationRepository.findOneWithEagerRelationships(id).map(regularizationAttestationMapper::toDto);
    }

    /**
     * Delete the regularizationAttestation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RegularizationAttestation : {}", id);
        regularizationAttestationRepository.deleteById(id);
    }
}
