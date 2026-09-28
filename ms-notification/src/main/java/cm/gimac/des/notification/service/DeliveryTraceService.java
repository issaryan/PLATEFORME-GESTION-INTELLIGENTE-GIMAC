package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.DeliveryTrace;
import cm.gimac.des.notification.repository.DeliveryTraceRepository;
import cm.gimac.des.notification.service.dto.DeliveryTraceDTO;
import cm.gimac.des.notification.service.mapper.DeliveryTraceMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.notification.domain.DeliveryTrace}.
 */
@Service
@Transactional
public class DeliveryTraceService {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryTraceService.class);

    private final DeliveryTraceRepository deliveryTraceRepository;

    private final DeliveryTraceMapper deliveryTraceMapper;

    public DeliveryTraceService(DeliveryTraceRepository deliveryTraceRepository, DeliveryTraceMapper deliveryTraceMapper) {
        this.deliveryTraceRepository = deliveryTraceRepository;
        this.deliveryTraceMapper = deliveryTraceMapper;
    }

    /**
     * Save a deliveryTrace.
     *
     * @param deliveryTraceDTO the entity to save.
     * @return the persisted entity.
     */
    public DeliveryTraceDTO save(DeliveryTraceDTO deliveryTraceDTO) {
        LOG.debug("Request to save DeliveryTrace : {}", deliveryTraceDTO);
        DeliveryTrace deliveryTrace = deliveryTraceMapper.toEntity(deliveryTraceDTO);
        deliveryTrace = deliveryTraceRepository.save(deliveryTrace);
        return deliveryTraceMapper.toDto(deliveryTrace);
    }

    /**
     * Update a deliveryTrace.
     *
     * @param deliveryTraceDTO the entity to save.
     * @return the persisted entity.
     */
    public DeliveryTraceDTO update(DeliveryTraceDTO deliveryTraceDTO) {
        LOG.debug("Request to update DeliveryTrace : {}", deliveryTraceDTO);
        DeliveryTrace deliveryTrace = deliveryTraceMapper.toEntity(deliveryTraceDTO);
        deliveryTrace = deliveryTraceRepository.save(deliveryTrace);
        return deliveryTraceMapper.toDto(deliveryTrace);
    }

    /**
     * Partially update a deliveryTrace.
     *
     * @param deliveryTraceDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DeliveryTraceDTO> partialUpdate(DeliveryTraceDTO deliveryTraceDTO) {
        LOG.debug("Request to partially update DeliveryTrace : {}", deliveryTraceDTO);

        return deliveryTraceRepository
            .findById(deliveryTraceDTO.getId())
            .map(existingDeliveryTrace -> {
                deliveryTraceMapper.partialUpdate(existingDeliveryTrace, deliveryTraceDTO);

                return existingDeliveryTrace;
            })
            .map(deliveryTraceRepository::save)
            .map(deliveryTraceMapper::toDto);
    }

    /**
     * Get one deliveryTrace by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DeliveryTraceDTO> findOne(UUID id) {
        LOG.debug("Request to get DeliveryTrace : {}", id);
        return deliveryTraceRepository.findById(id).map(deliveryTraceMapper::toDto);
    }

    /**
     * Delete the deliveryTrace by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete DeliveryTrace : {}", id);
        deliveryTraceRepository.deleteById(id);
    }
}
