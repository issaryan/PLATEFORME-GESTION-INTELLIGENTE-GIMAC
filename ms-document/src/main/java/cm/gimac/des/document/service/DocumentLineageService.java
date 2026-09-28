package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.DocumentLineage;
import cm.gimac.des.document.repository.DocumentLineageRepository;
import cm.gimac.des.document.service.dto.DocumentLineageDTO;
import cm.gimac.des.document.service.mapper.DocumentLineageMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.document.domain.DocumentLineage}.
 */
@Service
@Transactional
public class DocumentLineageService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentLineageService.class);

    private final DocumentLineageRepository documentLineageRepository;

    private final DocumentLineageMapper documentLineageMapper;

    public DocumentLineageService(DocumentLineageRepository documentLineageRepository, DocumentLineageMapper documentLineageMapper) {
        this.documentLineageRepository = documentLineageRepository;
        this.documentLineageMapper = documentLineageMapper;
    }

    /**
     * Save a documentLineage.
     *
     * @param documentLineageDTO the entity to save.
     * @return the persisted entity.
     */
    public DocumentLineageDTO save(DocumentLineageDTO documentLineageDTO) {
        LOG.debug("Request to save DocumentLineage : {}", documentLineageDTO);
        DocumentLineage documentLineage = documentLineageMapper.toEntity(documentLineageDTO);
        documentLineage = documentLineageRepository.save(documentLineage);
        return documentLineageMapper.toDto(documentLineage);
    }

    /**
     * Update a documentLineage.
     *
     * @param documentLineageDTO the entity to save.
     * @return the persisted entity.
     */
    public DocumentLineageDTO update(DocumentLineageDTO documentLineageDTO) {
        LOG.debug("Request to update DocumentLineage : {}", documentLineageDTO);
        DocumentLineage documentLineage = documentLineageMapper.toEntity(documentLineageDTO);
        documentLineage = documentLineageRepository.save(documentLineage);
        return documentLineageMapper.toDto(documentLineage);
    }

    /**
     * Partially update a documentLineage.
     *
     * @param documentLineageDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DocumentLineageDTO> partialUpdate(DocumentLineageDTO documentLineageDTO) {
        LOG.debug("Request to partially update DocumentLineage : {}", documentLineageDTO);

        return documentLineageRepository
            .findById(documentLineageDTO.getId())
            .map(existingDocumentLineage -> {
                documentLineageMapper.partialUpdate(existingDocumentLineage, documentLineageDTO);

                return existingDocumentLineage;
            })
            .map(documentLineageRepository::save)
            .map(documentLineageMapper::toDto);
    }

    /**
     * Get one documentLineage by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DocumentLineageDTO> findOne(UUID id) {
        LOG.debug("Request to get DocumentLineage : {}", id);
        return documentLineageRepository.findById(id).map(documentLineageMapper::toDto);
    }

    /**
     * Delete the documentLineage by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete DocumentLineage : {}", id);
        documentLineageRepository.deleteById(id);
    }
}
