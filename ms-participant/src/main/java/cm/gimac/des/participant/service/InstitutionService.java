package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import cm.gimac.des.participant.service.mapper.InstitutionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.Institution}.
 */
@Service
@Transactional
public class InstitutionService {

    private static final Logger LOG = LoggerFactory.getLogger(InstitutionService.class);

    private final InstitutionRepository institutionRepository;

    private final InstitutionMapper institutionMapper;

    public InstitutionService(InstitutionRepository institutionRepository, InstitutionMapper institutionMapper) {
        this.institutionRepository = institutionRepository;
        this.institutionMapper = institutionMapper;
    }

    /**
     * Save a institution.
     *
     * @param institutionDTO the entity to save.
     * @return the persisted entity.
     */
    public InstitutionDTO save(InstitutionDTO institutionDTO) {
        LOG.debug("Request to save Institution : {}", institutionDTO);
        Institution institution = institutionMapper.toEntity(institutionDTO);
        institution = institutionRepository.save(institution);
        return institutionMapper.toDto(institution);
    }

    /**
     * Update a institution.
     *
     * @param institutionDTO the entity to save.
     * @return the persisted entity.
     */
    public InstitutionDTO update(InstitutionDTO institutionDTO) {
        LOG.debug("Request to update Institution : {}", institutionDTO);
        Institution institution = institutionMapper.toEntity(institutionDTO);
        institution = institutionRepository.save(institution);
        return institutionMapper.toDto(institution);
    }

    /**
     * Partially update a institution.
     *
     * @param institutionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<InstitutionDTO> partialUpdate(InstitutionDTO institutionDTO) {
        LOG.debug("Request to partially update Institution : {}", institutionDTO);

        return institutionRepository
            .findById(institutionDTO.getId())
            .map(existingInstitution -> {
                institutionMapper.partialUpdate(existingInstitution, institutionDTO);

                return existingInstitution;
            })
            .map(institutionRepository::save)
            .map(institutionMapper::toDto);
    }

    /**
     * Get all the institutions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<InstitutionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return institutionRepository.findAllWithEagerRelationships(pageable).map(institutionMapper::toDto);
    }

    /**
     * Get one institution by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<InstitutionDTO> findOne(UUID id) {
        LOG.debug("Request to get Institution : {}", id);
        return institutionRepository.findOneWithEagerRelationships(id).map(institutionMapper::toDto);
    }

    /**
     * Delete the institution by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Institution : {}", id);
        institutionRepository.deleteById(id);
    }
}
