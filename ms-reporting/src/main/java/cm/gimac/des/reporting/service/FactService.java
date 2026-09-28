package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.Fact;
import cm.gimac.des.reporting.repository.FactRepository;
import cm.gimac.des.reporting.service.dto.FactDTO;
import cm.gimac.des.reporting.service.mapper.FactMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.Fact}.
 */
@Service
@Transactional
public class FactService {

    private static final Logger LOG = LoggerFactory.getLogger(FactService.class);

    private final FactRepository factRepository;

    private final FactMapper factMapper;

    public FactService(FactRepository factRepository, FactMapper factMapper) {
        this.factRepository = factRepository;
        this.factMapper = factMapper;
    }

    /**
     * Save a fact.
     *
     * @param factDTO the entity to save.
     * @return the persisted entity.
     */
    public FactDTO save(FactDTO factDTO) {
        LOG.debug("Request to save Fact : {}", factDTO);
        Fact fact = factMapper.toEntity(factDTO);
        fact = factRepository.save(fact);
        return factMapper.toDto(fact);
    }

    /**
     * Update a fact.
     *
     * @param factDTO the entity to save.
     * @return the persisted entity.
     */
    public FactDTO update(FactDTO factDTO) {
        LOG.debug("Request to update Fact : {}", factDTO);
        Fact fact = factMapper.toEntity(factDTO);
        fact = factRepository.save(fact);
        return factMapper.toDto(fact);
    }

    /**
     * Partially update a fact.
     *
     * @param factDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FactDTO> partialUpdate(FactDTO factDTO) {
        LOG.debug("Request to partially update Fact : {}", factDTO);

        return factRepository
            .findById(factDTO.getId())
            .map(existingFact -> {
                factMapper.partialUpdate(existingFact, factDTO);

                return existingFact;
            })
            .map(factRepository::save)
            .map(factMapper::toDto);
    }

    /**
     * Get all the facts with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<FactDTO> findAllWithEagerRelationships(Pageable pageable) {
        return factRepository.findAllWithEagerRelationships(pageable).map(factMapper::toDto);
    }

    /**
     * Get one fact by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FactDTO> findOne(UUID id) {
        LOG.debug("Request to get Fact : {}", id);
        return factRepository.findOneWithEagerRelationships(id).map(factMapper::toDto);
    }

    /**
     * Delete the fact by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Fact : {}", id);
        factRepository.deleteById(id);
    }
}
