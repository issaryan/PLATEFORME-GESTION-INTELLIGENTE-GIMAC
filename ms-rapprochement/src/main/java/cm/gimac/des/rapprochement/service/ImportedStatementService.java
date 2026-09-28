package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.ImportedStatement;
import cm.gimac.des.rapprochement.repository.ImportedStatementRepository;
import cm.gimac.des.rapprochement.service.dto.ImportedStatementDTO;
import cm.gimac.des.rapprochement.service.mapper.ImportedStatementMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.ImportedStatement}.
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
     * Get one importedStatement by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ImportedStatementDTO> findOne(UUID id) {
        LOG.debug("Request to get ImportedStatement : {}", id);
        return importedStatementRepository.findById(id).map(importedStatementMapper::toDto);
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
