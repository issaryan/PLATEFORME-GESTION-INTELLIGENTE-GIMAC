package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.ReferenceCatalog;
import cm.gimac.des.homologation.repository.ReferenceCatalogRepository;
import cm.gimac.des.homologation.service.dto.ReferenceCatalogDTO;
import cm.gimac.des.homologation.service.mapper.ReferenceCatalogMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ReferenceCatalog}.
 */
@Service
@Transactional
public class ReferenceCatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceCatalogService.class);

    private final ReferenceCatalogRepository referenceCatalogRepository;

    private final ReferenceCatalogMapper referenceCatalogMapper;

    public ReferenceCatalogService(ReferenceCatalogRepository referenceCatalogRepository, ReferenceCatalogMapper referenceCatalogMapper) {
        this.referenceCatalogRepository = referenceCatalogRepository;
        this.referenceCatalogMapper = referenceCatalogMapper;
    }

    /**
     * Save a referenceCatalog.
     *
     * @param referenceCatalogDTO the entity to save.
     * @return the persisted entity.
     */
    public ReferenceCatalogDTO save(ReferenceCatalogDTO referenceCatalogDTO) {
        LOG.debug("Request to save ReferenceCatalog : {}", referenceCatalogDTO);
        ReferenceCatalog referenceCatalog = referenceCatalogMapper.toEntity(referenceCatalogDTO);
        referenceCatalog = referenceCatalogRepository.save(referenceCatalog);
        return referenceCatalogMapper.toDto(referenceCatalog);
    }

    /**
     * Update a referenceCatalog.
     *
     * @param referenceCatalogDTO the entity to save.
     * @return the persisted entity.
     */
    public ReferenceCatalogDTO update(ReferenceCatalogDTO referenceCatalogDTO) {
        LOG.debug("Request to update ReferenceCatalog : {}", referenceCatalogDTO);
        ReferenceCatalog referenceCatalog = referenceCatalogMapper.toEntity(referenceCatalogDTO);
        referenceCatalog = referenceCatalogRepository.save(referenceCatalog);
        return referenceCatalogMapper.toDto(referenceCatalog);
    }

    /**
     * Partially update a referenceCatalog.
     *
     * @param referenceCatalogDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ReferenceCatalogDTO> partialUpdate(ReferenceCatalogDTO referenceCatalogDTO) {
        LOG.debug("Request to partially update ReferenceCatalog : {}", referenceCatalogDTO);

        return referenceCatalogRepository
            .findById(referenceCatalogDTO.getId())
            .map(existingReferenceCatalog -> {
                referenceCatalogMapper.partialUpdate(existingReferenceCatalog, referenceCatalogDTO);

                return existingReferenceCatalog;
            })
            .map(referenceCatalogRepository::save)
            .map(referenceCatalogMapper::toDto);
    }

    /**
     * Get all the referenceCatalogs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ReferenceCatalogDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all ReferenceCatalogs");
        return referenceCatalogRepository.findAll(pageable).map(referenceCatalogMapper::toDto);
    }

    /**
     * Get one referenceCatalog by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ReferenceCatalogDTO> findOne(UUID id) {
        LOG.debug("Request to get ReferenceCatalog : {}", id);
        return referenceCatalogRepository.findById(id).map(referenceCatalogMapper::toDto);
    }

    /**
     * Delete the referenceCatalog by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ReferenceCatalog : {}", id);
        referenceCatalogRepository.deleteById(id);
    }
}
