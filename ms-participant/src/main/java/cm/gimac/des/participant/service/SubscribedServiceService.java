package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.SubscribedService;
import cm.gimac.des.participant.repository.SubscribedServiceRepository;
import cm.gimac.des.participant.service.dto.SubscribedServiceDTO;
import cm.gimac.des.participant.service.mapper.SubscribedServiceMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.SubscribedService}.
 */
@Service
@Transactional
public class SubscribedServiceService {

    private static final Logger LOG = LoggerFactory.getLogger(SubscribedServiceService.class);

    private final SubscribedServiceRepository subscribedServiceRepository;

    private final SubscribedServiceMapper subscribedServiceMapper;

    public SubscribedServiceService(
        SubscribedServiceRepository subscribedServiceRepository,
        SubscribedServiceMapper subscribedServiceMapper
    ) {
        this.subscribedServiceRepository = subscribedServiceRepository;
        this.subscribedServiceMapper = subscribedServiceMapper;
    }

    /**
     * Save a subscribedService.
     *
     * @param subscribedServiceDTO the entity to save.
     * @return the persisted entity.
     */
    public SubscribedServiceDTO save(SubscribedServiceDTO subscribedServiceDTO) {
        LOG.debug("Request to save SubscribedService : {}", subscribedServiceDTO);
        SubscribedService subscribedService = subscribedServiceMapper.toEntity(subscribedServiceDTO);
        subscribedService = subscribedServiceRepository.save(subscribedService);
        return subscribedServiceMapper.toDto(subscribedService);
    }

    /**
     * Update a subscribedService.
     *
     * @param subscribedServiceDTO the entity to save.
     * @return the persisted entity.
     */
    public SubscribedServiceDTO update(SubscribedServiceDTO subscribedServiceDTO) {
        LOG.debug("Request to update SubscribedService : {}", subscribedServiceDTO);
        SubscribedService subscribedService = subscribedServiceMapper.toEntity(subscribedServiceDTO);
        subscribedService = subscribedServiceRepository.save(subscribedService);
        return subscribedServiceMapper.toDto(subscribedService);
    }

    /**
     * Partially update a subscribedService.
     *
     * @param subscribedServiceDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<SubscribedServiceDTO> partialUpdate(SubscribedServiceDTO subscribedServiceDTO) {
        LOG.debug("Request to partially update SubscribedService : {}", subscribedServiceDTO);

        return subscribedServiceRepository
            .findById(subscribedServiceDTO.getId())
            .map(existingSubscribedService -> {
                subscribedServiceMapper.partialUpdate(existingSubscribedService, subscribedServiceDTO);

                return existingSubscribedService;
            })
            .map(subscribedServiceRepository::save)
            .map(subscribedServiceMapper::toDto);
    }

    /**
     * Get all the subscribedServices with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<SubscribedServiceDTO> findAllWithEagerRelationships(Pageable pageable) {
        return subscribedServiceRepository.findAllWithEagerRelationships(pageable).map(subscribedServiceMapper::toDto);
    }

    /**
     * Get one subscribedService by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<SubscribedServiceDTO> findOne(UUID id) {
        LOG.debug("Request to get SubscribedService : {}", id);
        return subscribedServiceRepository.findOneWithEagerRelationships(id).map(subscribedServiceMapper::toDto);
    }

    /**
     * Delete the subscribedService by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete SubscribedService : {}", id);
        subscribedServiceRepository.deleteById(id);
    }
}
