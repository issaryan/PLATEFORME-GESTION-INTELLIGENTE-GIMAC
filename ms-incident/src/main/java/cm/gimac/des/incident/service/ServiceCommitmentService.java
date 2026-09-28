package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.ServiceCommitment;
import cm.gimac.des.incident.repository.ServiceCommitmentRepository;
import cm.gimac.des.incident.service.dto.ServiceCommitmentDTO;
import cm.gimac.des.incident.service.mapper.ServiceCommitmentMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.ServiceCommitment}.
 */
@Service
@Transactional
public class ServiceCommitmentService {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceCommitmentService.class);

    private final ServiceCommitmentRepository serviceCommitmentRepository;

    private final ServiceCommitmentMapper serviceCommitmentMapper;

    public ServiceCommitmentService(
        ServiceCommitmentRepository serviceCommitmentRepository,
        ServiceCommitmentMapper serviceCommitmentMapper
    ) {
        this.serviceCommitmentRepository = serviceCommitmentRepository;
        this.serviceCommitmentMapper = serviceCommitmentMapper;
    }

    /**
     * Save a serviceCommitment.
     *
     * @param serviceCommitmentDTO the entity to save.
     * @return the persisted entity.
     */
    public ServiceCommitmentDTO save(ServiceCommitmentDTO serviceCommitmentDTO) {
        LOG.debug("Request to save ServiceCommitment : {}", serviceCommitmentDTO);
        ServiceCommitment serviceCommitment = serviceCommitmentMapper.toEntity(serviceCommitmentDTO);
        serviceCommitment = serviceCommitmentRepository.save(serviceCommitment);
        return serviceCommitmentMapper.toDto(serviceCommitment);
    }

    /**
     * Update a serviceCommitment.
     *
     * @param serviceCommitmentDTO the entity to save.
     * @return the persisted entity.
     */
    public ServiceCommitmentDTO update(ServiceCommitmentDTO serviceCommitmentDTO) {
        LOG.debug("Request to update ServiceCommitment : {}", serviceCommitmentDTO);
        ServiceCommitment serviceCommitment = serviceCommitmentMapper.toEntity(serviceCommitmentDTO);
        serviceCommitment = serviceCommitmentRepository.save(serviceCommitment);
        return serviceCommitmentMapper.toDto(serviceCommitment);
    }

    /**
     * Partially update a serviceCommitment.
     *
     * @param serviceCommitmentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ServiceCommitmentDTO> partialUpdate(ServiceCommitmentDTO serviceCommitmentDTO) {
        LOG.debug("Request to partially update ServiceCommitment : {}", serviceCommitmentDTO);

        return serviceCommitmentRepository
            .findById(serviceCommitmentDTO.getId())
            .map(existingServiceCommitment -> {
                serviceCommitmentMapper.partialUpdate(existingServiceCommitment, serviceCommitmentDTO);

                return existingServiceCommitment;
            })
            .map(serviceCommitmentRepository::save)
            .map(serviceCommitmentMapper::toDto);
    }

    /**
     * Get all the serviceCommitments.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ServiceCommitmentDTO> findAll() {
        LOG.debug("Request to get all ServiceCommitments");
        return serviceCommitmentRepository
            .findAll()
            .stream()
            .map(serviceCommitmentMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one serviceCommitment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ServiceCommitmentDTO> findOne(UUID id) {
        LOG.debug("Request to get ServiceCommitment : {}", id);
        return serviceCommitmentRepository.findById(id).map(serviceCommitmentMapper::toDto);
    }

    /**
     * Delete the serviceCommitment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ServiceCommitment : {}", id);
        serviceCommitmentRepository.deleteById(id);
    }
}
