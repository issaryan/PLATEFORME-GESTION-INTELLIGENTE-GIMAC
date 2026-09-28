package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.DocumentTypePolicy;
import cm.gimac.des.document.repository.DocumentTypePolicyRepository;
import cm.gimac.des.document.service.dto.DocumentTypePolicyDTO;
import cm.gimac.des.document.service.mapper.DocumentTypePolicyMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.document.domain.DocumentTypePolicy}.
 */
@Service
@Transactional
public class DocumentTypePolicyService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentTypePolicyService.class);

    private final DocumentTypePolicyRepository documentTypePolicyRepository;

    private final DocumentTypePolicyMapper documentTypePolicyMapper;

    public DocumentTypePolicyService(
        DocumentTypePolicyRepository documentTypePolicyRepository,
        DocumentTypePolicyMapper documentTypePolicyMapper
    ) {
        this.documentTypePolicyRepository = documentTypePolicyRepository;
        this.documentTypePolicyMapper = documentTypePolicyMapper;
    }

    /**
     * Save a documentTypePolicy.
     *
     * @param documentTypePolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public DocumentTypePolicyDTO save(DocumentTypePolicyDTO documentTypePolicyDTO) {
        LOG.debug("Request to save DocumentTypePolicy : {}", documentTypePolicyDTO);
        DocumentTypePolicy documentTypePolicy = documentTypePolicyMapper.toEntity(documentTypePolicyDTO);
        documentTypePolicy = documentTypePolicyRepository.save(documentTypePolicy);
        return documentTypePolicyMapper.toDto(documentTypePolicy);
    }

    /**
     * Update a documentTypePolicy.
     *
     * @param documentTypePolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public DocumentTypePolicyDTO update(DocumentTypePolicyDTO documentTypePolicyDTO) {
        LOG.debug("Request to update DocumentTypePolicy : {}", documentTypePolicyDTO);
        DocumentTypePolicy documentTypePolicy = documentTypePolicyMapper.toEntity(documentTypePolicyDTO);
        documentTypePolicy = documentTypePolicyRepository.save(documentTypePolicy);
        return documentTypePolicyMapper.toDto(documentTypePolicy);
    }

    /**
     * Partially update a documentTypePolicy.
     *
     * @param documentTypePolicyDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DocumentTypePolicyDTO> partialUpdate(DocumentTypePolicyDTO documentTypePolicyDTO) {
        LOG.debug("Request to partially update DocumentTypePolicy : {}", documentTypePolicyDTO);

        return documentTypePolicyRepository
            .findById(documentTypePolicyDTO.getId())
            .map(existingDocumentTypePolicy -> {
                documentTypePolicyMapper.partialUpdate(existingDocumentTypePolicy, documentTypePolicyDTO);

                return existingDocumentTypePolicy;
            })
            .map(documentTypePolicyRepository::save)
            .map(documentTypePolicyMapper::toDto);
    }

    /**
     * Get one documentTypePolicy by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DocumentTypePolicyDTO> findOne(UUID id) {
        LOG.debug("Request to get DocumentTypePolicy : {}", id);
        return documentTypePolicyRepository.findById(id).map(documentTypePolicyMapper::toDto);
    }

    /**
     * Delete the documentTypePolicy by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete DocumentTypePolicy : {}", id);
        documentTypePolicyRepository.deleteById(id);
    }
}
