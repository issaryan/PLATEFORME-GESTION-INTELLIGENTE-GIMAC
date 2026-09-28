package cm.gimac.des.gateway.service;

import cm.gimac.des.gateway.repository.PresentationLayerRepository;
import cm.gimac.des.gateway.service.dto.PresentationLayerDTO;
import cm.gimac.des.gateway.service.mapper.PresentationLayerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link cm.gimac.des.gateway.domain.PresentationLayer}.
 */
@Service
@Transactional
public class PresentationLayerService {

    private static final Logger LOG = LoggerFactory.getLogger(PresentationLayerService.class);

    private final PresentationLayerRepository presentationLayerRepository;

    private final PresentationLayerMapper presentationLayerMapper;

    public PresentationLayerService(
        PresentationLayerRepository presentationLayerRepository,
        PresentationLayerMapper presentationLayerMapper
    ) {
        this.presentationLayerRepository = presentationLayerRepository;
        this.presentationLayerMapper = presentationLayerMapper;
    }

    /**
     * Save a presentationLayer.
     *
     * @param presentationLayerDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<PresentationLayerDTO> save(PresentationLayerDTO presentationLayerDTO) {
        LOG.debug("Request to save PresentationLayer : {}", presentationLayerDTO);
        return presentationLayerRepository.save(presentationLayerMapper.toEntity(presentationLayerDTO)).map(presentationLayerMapper::toDto);
    }

    /**
     * Update a presentationLayer.
     *
     * @param presentationLayerDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<PresentationLayerDTO> update(PresentationLayerDTO presentationLayerDTO) {
        LOG.debug("Request to update PresentationLayer : {}", presentationLayerDTO);
        return presentationLayerRepository.save(presentationLayerMapper.toEntity(presentationLayerDTO)).map(presentationLayerMapper::toDto);
    }

    /**
     * Partially update a presentationLayer.
     *
     * @param presentationLayerDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Mono<PresentationLayerDTO> partialUpdate(PresentationLayerDTO presentationLayerDTO) {
        LOG.debug("Request to partially update PresentationLayer : {}", presentationLayerDTO);

        return presentationLayerRepository
            .findById(presentationLayerDTO.getId())
            .map(existingPresentationLayer -> {
                presentationLayerMapper.partialUpdate(existingPresentationLayer, presentationLayerDTO);

                return existingPresentationLayer;
            })
            .flatMap(presentationLayerRepository::save)
            .map(presentationLayerMapper::toDto);
    }

    /**
     * Get all the presentationLayers.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Flux<PresentationLayerDTO> findAll() {
        LOG.debug("Request to get all PresentationLayers");
        return presentationLayerRepository.findAll().map(presentationLayerMapper::toDto);
    }

    /**
     * Returns the number of presentationLayers available.
     * @return the number of entities in the database.
     *
     */
    public Mono<Long> countAll() {
        return presentationLayerRepository.count();
    }

    /**
     * Get one presentationLayer by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Mono<PresentationLayerDTO> findOne(Long id) {
        LOG.debug("Request to get PresentationLayer : {}", id);
        return presentationLayerRepository.findById(id).map(presentationLayerMapper::toDto);
    }

    /**
     * Delete the presentationLayer by id.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete PresentationLayer : {}", id);
        return presentationLayerRepository.deleteById(id);
    }
}
