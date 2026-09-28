package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.AcknowledgementPolicy;
import cm.gimac.des.notification.repository.AcknowledgementPolicyRepository;
import cm.gimac.des.notification.service.dto.AcknowledgementPolicyDTO;
import cm.gimac.des.notification.service.mapper.AcknowledgementPolicyMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.notification.domain.AcknowledgementPolicy}.
 */
@Service
@Transactional
public class AcknowledgementPolicyService {

    private static final Logger LOG = LoggerFactory.getLogger(AcknowledgementPolicyService.class);

    private final AcknowledgementPolicyRepository acknowledgementPolicyRepository;

    private final AcknowledgementPolicyMapper acknowledgementPolicyMapper;

    public AcknowledgementPolicyService(
        AcknowledgementPolicyRepository acknowledgementPolicyRepository,
        AcknowledgementPolicyMapper acknowledgementPolicyMapper
    ) {
        this.acknowledgementPolicyRepository = acknowledgementPolicyRepository;
        this.acknowledgementPolicyMapper = acknowledgementPolicyMapper;
    }

    /**
     * Save a acknowledgementPolicy.
     *
     * @param acknowledgementPolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public AcknowledgementPolicyDTO save(AcknowledgementPolicyDTO acknowledgementPolicyDTO) {
        LOG.debug("Request to save AcknowledgementPolicy : {}", acknowledgementPolicyDTO);
        AcknowledgementPolicy acknowledgementPolicy = acknowledgementPolicyMapper.toEntity(acknowledgementPolicyDTO);
        acknowledgementPolicy = acknowledgementPolicyRepository.save(acknowledgementPolicy);
        return acknowledgementPolicyMapper.toDto(acknowledgementPolicy);
    }

    /**
     * Update a acknowledgementPolicy.
     *
     * @param acknowledgementPolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public AcknowledgementPolicyDTO update(AcknowledgementPolicyDTO acknowledgementPolicyDTO) {
        LOG.debug("Request to update AcknowledgementPolicy : {}", acknowledgementPolicyDTO);
        AcknowledgementPolicy acknowledgementPolicy = acknowledgementPolicyMapper.toEntity(acknowledgementPolicyDTO);
        acknowledgementPolicy = acknowledgementPolicyRepository.save(acknowledgementPolicy);
        return acknowledgementPolicyMapper.toDto(acknowledgementPolicy);
    }

    /**
     * Partially update a acknowledgementPolicy.
     *
     * @param acknowledgementPolicyDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AcknowledgementPolicyDTO> partialUpdate(AcknowledgementPolicyDTO acknowledgementPolicyDTO) {
        LOG.debug("Request to partially update AcknowledgementPolicy : {}", acknowledgementPolicyDTO);

        return acknowledgementPolicyRepository
            .findById(acknowledgementPolicyDTO.getId())
            .map(existingAcknowledgementPolicy -> {
                acknowledgementPolicyMapper.partialUpdate(existingAcknowledgementPolicy, acknowledgementPolicyDTO);

                return existingAcknowledgementPolicy;
            })
            .map(acknowledgementPolicyRepository::save)
            .map(acknowledgementPolicyMapper::toDto);
    }

    /**
     * Get all the acknowledgementPolicies.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AcknowledgementPolicyDTO> findAll() {
        LOG.debug("Request to get all AcknowledgementPolicies");
        return acknowledgementPolicyRepository
            .findAll()
            .stream()
            .map(acknowledgementPolicyMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one acknowledgementPolicy by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AcknowledgementPolicyDTO> findOne(UUID id) {
        LOG.debug("Request to get AcknowledgementPolicy : {}", id);
        return acknowledgementPolicyRepository.findById(id).map(acknowledgementPolicyMapper::toDto);
    }

    /**
     * Delete the acknowledgementPolicy by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AcknowledgementPolicy : {}", id);
        acknowledgementPolicyRepository.deleteById(id);
    }
}
