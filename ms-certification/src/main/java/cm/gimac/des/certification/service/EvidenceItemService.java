package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.EvidenceItem;
import cm.gimac.des.certification.repository.EvidenceItemRepository;
import cm.gimac.des.certification.service.dto.EvidenceItemDTO;
import cm.gimac.des.certification.service.mapper.EvidenceItemMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.EvidenceItem}.
 */
@Service
@Transactional
public class EvidenceItemService {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceItemService.class);

    private final EvidenceItemRepository evidenceItemRepository;

    private final EvidenceItemMapper evidenceItemMapper;

    public EvidenceItemService(EvidenceItemRepository evidenceItemRepository, EvidenceItemMapper evidenceItemMapper) {
        this.evidenceItemRepository = evidenceItemRepository;
        this.evidenceItemMapper = evidenceItemMapper;
    }

    /**
     * Save a evidenceItem.
     *
     * @param evidenceItemDTO the entity to save.
     * @return the persisted entity.
     */
    public EvidenceItemDTO save(EvidenceItemDTO evidenceItemDTO) {
        LOG.debug("Request to save EvidenceItem : {}", evidenceItemDTO);
        EvidenceItem evidenceItem = evidenceItemMapper.toEntity(evidenceItemDTO);
        evidenceItem = evidenceItemRepository.save(evidenceItem);
        return evidenceItemMapper.toDto(evidenceItem);
    }

    /**
     * Update a evidenceItem.
     *
     * @param evidenceItemDTO the entity to save.
     * @return the persisted entity.
     */
    public EvidenceItemDTO update(EvidenceItemDTO evidenceItemDTO) {
        LOG.debug("Request to update EvidenceItem : {}", evidenceItemDTO);
        EvidenceItem evidenceItem = evidenceItemMapper.toEntity(evidenceItemDTO);
        evidenceItem = evidenceItemRepository.save(evidenceItem);
        return evidenceItemMapper.toDto(evidenceItem);
    }

    /**
     * Partially update a evidenceItem.
     *
     * @param evidenceItemDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<EvidenceItemDTO> partialUpdate(EvidenceItemDTO evidenceItemDTO) {
        LOG.debug("Request to partially update EvidenceItem : {}", evidenceItemDTO);

        return evidenceItemRepository
            .findById(evidenceItemDTO.getId())
            .map(existingEvidenceItem -> {
                evidenceItemMapper.partialUpdate(existingEvidenceItem, evidenceItemDTO);

                return existingEvidenceItem;
            })
            .map(evidenceItemRepository::save)
            .map(evidenceItemMapper::toDto);
    }

    /**
     * Get all the evidenceItems with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<EvidenceItemDTO> findAllWithEagerRelationships(Pageable pageable) {
        return evidenceItemRepository.findAllWithEagerRelationships(pageable).map(evidenceItemMapper::toDto);
    }

    /**
     *  Get all the evidenceItems where CommissioningFile is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<EvidenceItemDTO> findAllWhereCommissioningFileIsNull() {
        LOG.debug("Request to get all evidenceItems where CommissioningFile is null");
        return StreamSupport.stream(evidenceItemRepository.findAll().spliterator(), false)
            .filter(evidenceItem -> evidenceItem.getCommissioningFile() == null)
            .map(evidenceItemMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one evidenceItem by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EvidenceItemDTO> findOne(UUID id) {
        LOG.debug("Request to get EvidenceItem : {}", id);
        return evidenceItemRepository.findOneWithEagerRelationships(id).map(evidenceItemMapper::toDto);
    }

    /**
     * Delete the evidenceItem by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete EvidenceItem : {}", id);
        evidenceItemRepository.deleteById(id);
    }
}
