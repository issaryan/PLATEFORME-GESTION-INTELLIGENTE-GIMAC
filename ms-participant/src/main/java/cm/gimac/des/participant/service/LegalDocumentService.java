package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.LegalDocument;
import cm.gimac.des.participant.repository.LegalDocumentRepository;
import cm.gimac.des.participant.service.dto.LegalDocumentDTO;
import cm.gimac.des.participant.service.mapper.LegalDocumentMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.LegalDocument}.
 */
@Service
@Transactional
public class LegalDocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(LegalDocumentService.class);

    private final LegalDocumentRepository legalDocumentRepository;

    private final LegalDocumentMapper legalDocumentMapper;

    public LegalDocumentService(LegalDocumentRepository legalDocumentRepository, LegalDocumentMapper legalDocumentMapper) {
        this.legalDocumentRepository = legalDocumentRepository;
        this.legalDocumentMapper = legalDocumentMapper;
    }

    /**
     * Save a legalDocument.
     *
     * @param legalDocumentDTO the entity to save.
     * @return the persisted entity.
     */
    public LegalDocumentDTO save(LegalDocumentDTO legalDocumentDTO) {
        LOG.debug("Request to save LegalDocument : {}", legalDocumentDTO);
        LegalDocument legalDocument = legalDocumentMapper.toEntity(legalDocumentDTO);
        legalDocument = legalDocumentRepository.save(legalDocument);
        return legalDocumentMapper.toDto(legalDocument);
    }

    /**
     * Update a legalDocument.
     *
     * @param legalDocumentDTO the entity to save.
     * @return the persisted entity.
     */
    public LegalDocumentDTO update(LegalDocumentDTO legalDocumentDTO) {
        LOG.debug("Request to update LegalDocument : {}", legalDocumentDTO);
        LegalDocument legalDocument = legalDocumentMapper.toEntity(legalDocumentDTO);
        legalDocument = legalDocumentRepository.save(legalDocument);
        return legalDocumentMapper.toDto(legalDocument);
    }

    /**
     * Partially update a legalDocument.
     *
     * @param legalDocumentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<LegalDocumentDTO> partialUpdate(LegalDocumentDTO legalDocumentDTO) {
        LOG.debug("Request to partially update LegalDocument : {}", legalDocumentDTO);

        return legalDocumentRepository
            .findById(legalDocumentDTO.getId())
            .map(existingLegalDocument -> {
                legalDocumentMapper.partialUpdate(existingLegalDocument, legalDocumentDTO);

                return existingLegalDocument;
            })
            .map(legalDocumentRepository::save)
            .map(legalDocumentMapper::toDto);
    }

    /**
     * Get all the legalDocuments with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<LegalDocumentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return legalDocumentRepository.findAllWithEagerRelationships(pageable).map(legalDocumentMapper::toDto);
    }

    /**
     *  Get all the legalDocuments where LegalDocument is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<LegalDocumentDTO> findAllWhereLegalDocumentIsNull() {
        LOG.debug("Request to get all legalDocuments where LegalDocument is null");
        return StreamSupport.stream(legalDocumentRepository.findAll().spliterator(), false)
            .filter(legalDocument -> legalDocument.getLegalDocument() == null)
            .map(legalDocumentMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one legalDocument by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<LegalDocumentDTO> findOne(UUID id) {
        LOG.debug("Request to get LegalDocument : {}", id);
        return legalDocumentRepository.findOneWithEagerRelationships(id).map(legalDocumentMapper::toDto);
    }

    /**
     * Delete the legalDocument by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete LegalDocument : {}", id);
        legalDocumentRepository.deleteById(id);
    }
}
