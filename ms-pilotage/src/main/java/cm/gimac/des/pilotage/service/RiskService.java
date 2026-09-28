package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.Risk;
import cm.gimac.des.pilotage.repository.RiskRepository;
import cm.gimac.des.pilotage.service.dto.RiskDTO;
import cm.gimac.des.pilotage.service.mapper.RiskMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.Risk}.
 */
@Service
@Transactional
public class RiskService {

    private static final Logger LOG = LoggerFactory.getLogger(RiskService.class);

    private final RiskRepository riskRepository;

    private final RiskMapper riskMapper;

    public RiskService(RiskRepository riskRepository, RiskMapper riskMapper) {
        this.riskRepository = riskRepository;
        this.riskMapper = riskMapper;
    }

    /**
     * Save a risk.
     *
     * @param riskDTO the entity to save.
     * @return the persisted entity.
     */
    public RiskDTO save(RiskDTO riskDTO) {
        LOG.debug("Request to save Risk : {}", riskDTO);
        Risk risk = riskMapper.toEntity(riskDTO);
        risk = riskRepository.save(risk);
        return riskMapper.toDto(risk);
    }

    /**
     * Update a risk.
     *
     * @param riskDTO the entity to save.
     * @return the persisted entity.
     */
    public RiskDTO update(RiskDTO riskDTO) {
        LOG.debug("Request to update Risk : {}", riskDTO);
        Risk risk = riskMapper.toEntity(riskDTO);
        risk = riskRepository.save(risk);
        return riskMapper.toDto(risk);
    }

    /**
     * Partially update a risk.
     *
     * @param riskDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RiskDTO> partialUpdate(RiskDTO riskDTO) {
        LOG.debug("Request to partially update Risk : {}", riskDTO);

        return riskRepository
            .findById(riskDTO.getId())
            .map(existingRisk -> {
                riskMapper.partialUpdate(existingRisk, riskDTO);

                return existingRisk;
            })
            .map(riskRepository::save)
            .map(riskMapper::toDto);
    }

    /**
     * Get all the risks with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RiskDTO> findAllWithEagerRelationships(Pageable pageable) {
        return riskRepository.findAllWithEagerRelationships(pageable).map(riskMapper::toDto);
    }

    /**
     * Get one risk by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RiskDTO> findOne(UUID id) {
        LOG.debug("Request to get Risk : {}", id);
        return riskRepository.findOneWithEagerRelationships(id).map(riskMapper::toDto);
    }

    /**
     * Delete the risk by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Risk : {}", id);
        riskRepository.deleteById(id);
    }
}
