package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.StatementLine;
import cm.gimac.des.rapprochement.repository.StatementLineRepository;
import cm.gimac.des.rapprochement.service.dto.StatementLineDTO;
import cm.gimac.des.rapprochement.service.mapper.StatementLineMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.StatementLine}.
 */
@Service
@Transactional
public class StatementLineService {

    private static final Logger LOG = LoggerFactory.getLogger(StatementLineService.class);

    private final StatementLineRepository statementLineRepository;

    private final StatementLineMapper statementLineMapper;

    public StatementLineService(StatementLineRepository statementLineRepository, StatementLineMapper statementLineMapper) {
        this.statementLineRepository = statementLineRepository;
        this.statementLineMapper = statementLineMapper;
    }

    /**
     * Save a statementLine.
     *
     * @param statementLineDTO the entity to save.
     * @return the persisted entity.
     */
    public StatementLineDTO save(StatementLineDTO statementLineDTO) {
        LOG.debug("Request to save StatementLine : {}", statementLineDTO);
        StatementLine statementLine = statementLineMapper.toEntity(statementLineDTO);
        statementLine = statementLineRepository.save(statementLine);
        return statementLineMapper.toDto(statementLine);
    }

    /**
     * Update a statementLine.
     *
     * @param statementLineDTO the entity to save.
     * @return the persisted entity.
     */
    public StatementLineDTO update(StatementLineDTO statementLineDTO) {
        LOG.debug("Request to update StatementLine : {}", statementLineDTO);
        StatementLine statementLine = statementLineMapper.toEntity(statementLineDTO);
        statementLine = statementLineRepository.save(statementLine);
        return statementLineMapper.toDto(statementLine);
    }

    /**
     * Partially update a statementLine.
     *
     * @param statementLineDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<StatementLineDTO> partialUpdate(StatementLineDTO statementLineDTO) {
        LOG.debug("Request to partially update StatementLine : {}", statementLineDTO);

        return statementLineRepository
            .findById(statementLineDTO.getId())
            .map(existingStatementLine -> {
                statementLineMapper.partialUpdate(existingStatementLine, statementLineDTO);

                return existingStatementLine;
            })
            .map(statementLineRepository::save)
            .map(statementLineMapper::toDto);
    }

    /**
     * Get all the statementLines with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<StatementLineDTO> findAllWithEagerRelationships(Pageable pageable) {
        return statementLineRepository.findAllWithEagerRelationships(pageable).map(statementLineMapper::toDto);
    }

    /**
     * Get one statementLine by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<StatementLineDTO> findOne(UUID id) {
        LOG.debug("Request to get StatementLine : {}", id);
        return statementLineRepository.findOneWithEagerRelationships(id).map(statementLineMapper::toDto);
    }

    /**
     * Delete the statementLine by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete StatementLine : {}", id);
        statementLineRepository.deleteById(id);
    }
}
