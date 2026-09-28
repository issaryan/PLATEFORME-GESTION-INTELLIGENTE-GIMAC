package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.ImportedStatement;
import cm.gimac.des.reporting.repository.ImportedStatementRepository;
import cm.gimac.des.reporting.service.dto.ImportedStatementDTO;
import cm.gimac.des.reporting.service.mapper.ImportedStatementMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.ImportedStatement}.
 */
@Service
@Transactional
public class ImportedStatementService {

    private static final Logger LOG = LoggerFactory.getLogger(ImportedStatementService.class);

    private final ImportedStatementRepository importedStatementRepository;

    private final ImportedStatementMapper importedStatementMapper;

    public ImportedStatementService(
        ImportedStatementRepository importedStatementRepository,
        ImportedStatementMapper importedStatementMapper
    ) {
        this.importedStatementRepository = importedStatementRepository;
        this.importedStatementMapper = importedStatementMapper;
    }

    /**
     * Save a importedStatement.
     *
     * @param importedStatementDTO the entity to save.
     * @return the persisted entity.
     */
    public ImportedStatementDTO save(ImportedStatementDTO importedStatementDTO) {
        LOG.debug("Request to save ImportedStatement : {}", importedStatementDTO);
        ImportedStatement importedStatement = importedStatementMapper.toEntity(importedStatementDTO);
        importedStatement = importedStatementRepository.save(importedStatement);
        return importedStatementMapper.toDto(importedStatement);
    }

    /**
     * Update a importedStatement.
     *
     * @param importedStatementDTO the entity to save.
     * @return the persisted entity.
     */
    public ImportedStatementDTO update(ImportedStatementDTO importedStatementDTO) {
        LOG.debug("Request to update ImportedStatement : {}", importedStatementDTO);
        ImportedStatement importedStatement = importedStatementMapper.toEntity(importedStatementDTO);
        importedStatement = importedStatementRepository.save(importedStatement);
        return importedStatementMapper.toDto(importedStatement);
    }

    /**
     * Partially update a importedStatement.
     *
     * @param importedStatementDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ImportedStatementDTO> partialUpdate(ImportedStatementDTO importedStatementDTO) {
        LOG.debug("Request to partially update ImportedStatement : {}", importedStatementDTO);

        return importedStatementRepository
            .findById(importedStatementDTO.getId())
            .map(existingImportedStatement -> {
                importedStatementMapper.partialUpdate(existingImportedStatement, importedStatementDTO);

                return existingImportedStatement;
            })
            .map(importedStatementRepository::save)
            .map(importedStatementMapper::toDto);
    }

    /**
     * Get all the importedStatements with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ImportedStatementDTO> findAllWithEagerRelationships(Pageable pageable) {
        return importedStatementRepository.findAllWithEagerRelationships(pageable).map(importedStatementMapper::toDto);
    }

    /**
     *  Get all the importedStatements where ImportedStatement is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ImportedStatementDTO> findAllWhereImportedStatementIsNull() {
        LOG.debug("Request to get all importedStatements where ImportedStatement is null");
        return StreamSupport.stream(importedStatementRepository.findAll().spliterator(), false)
            .filter(importedStatement -> importedStatement.getImportedStatement() == null)
            .map(importedStatementMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one importedStatement by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ImportedStatementDTO> findOne(UUID id) {
        LOG.debug("Request to get ImportedStatement : {}", id);
        return importedStatementRepository.findOneWithEagerRelationships(id).map(importedStatementMapper::toDto);
    }

    /**
     * Delete the importedStatement by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ImportedStatement : {}", id);
        importedStatementRepository.deleteById(id);
    }
}
