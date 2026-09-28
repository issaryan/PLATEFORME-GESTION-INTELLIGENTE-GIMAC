package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.RiskAssessment;
import cm.gimac.des.pilotage.repository.RiskAssessmentRepository;
import cm.gimac.des.pilotage.service.dto.RiskAssessmentDTO;
import cm.gimac.des.pilotage.service.mapper.RiskAssessmentMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.RiskAssessment}.
 */
@Service
@Transactional
public class RiskAssessmentService {

    private static final Logger LOG = LoggerFactory.getLogger(RiskAssessmentService.class);

    private final RiskAssessmentRepository riskAssessmentRepository;

    private final RiskAssessmentMapper riskAssessmentMapper;

    public RiskAssessmentService(RiskAssessmentRepository riskAssessmentRepository, RiskAssessmentMapper riskAssessmentMapper) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskAssessmentMapper = riskAssessmentMapper;
    }

    /**
     * Save a riskAssessment.
     *
     * @param riskAssessmentDTO the entity to save.
     * @return the persisted entity.
     */
    public RiskAssessmentDTO save(RiskAssessmentDTO riskAssessmentDTO) {
        LOG.debug("Request to save RiskAssessment : {}", riskAssessmentDTO);
        RiskAssessment riskAssessment = riskAssessmentMapper.toEntity(riskAssessmentDTO);
        riskAssessment = riskAssessmentRepository.save(riskAssessment);
        return riskAssessmentMapper.toDto(riskAssessment);
    }

    /**
     * Update a riskAssessment.
     *
     * @param riskAssessmentDTO the entity to save.
     * @return the persisted entity.
     */
    public RiskAssessmentDTO update(RiskAssessmentDTO riskAssessmentDTO) {
        LOG.debug("Request to update RiskAssessment : {}", riskAssessmentDTO);
        RiskAssessment riskAssessment = riskAssessmentMapper.toEntity(riskAssessmentDTO);
        riskAssessment = riskAssessmentRepository.save(riskAssessment);
        return riskAssessmentMapper.toDto(riskAssessment);
    }

    /**
     * Partially update a riskAssessment.
     *
     * @param riskAssessmentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RiskAssessmentDTO> partialUpdate(RiskAssessmentDTO riskAssessmentDTO) {
        LOG.debug("Request to partially update RiskAssessment : {}", riskAssessmentDTO);

        return riskAssessmentRepository
            .findById(riskAssessmentDTO.getId())
            .map(existingRiskAssessment -> {
                riskAssessmentMapper.partialUpdate(existingRiskAssessment, riskAssessmentDTO);

                return existingRiskAssessment;
            })
            .map(riskAssessmentRepository::save)
            .map(riskAssessmentMapper::toDto);
    }

    /**
     * Get all the riskAssessments with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RiskAssessmentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return riskAssessmentRepository.findAllWithEagerRelationships(pageable).map(riskAssessmentMapper::toDto);
    }

    /**
     * Get one riskAssessment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RiskAssessmentDTO> findOne(UUID id) {
        LOG.debug("Request to get RiskAssessment : {}", id);
        return riskAssessmentRepository.findOneWithEagerRelationships(id).map(riskAssessmentMapper::toDto);
    }

    /**
     * Delete the riskAssessment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RiskAssessment : {}", id);
        riskAssessmentRepository.deleteById(id);
    }
}
