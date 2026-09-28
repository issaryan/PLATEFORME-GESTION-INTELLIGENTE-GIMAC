package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.Incident;
import cm.gimac.des.incident.repository.IncidentRepository;
import cm.gimac.des.incident.service.dto.IncidentDTO;
import cm.gimac.des.incident.service.mapper.IncidentMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.Incident}.
 */
@Service
@Transactional
public class IncidentService {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;

    private final IncidentMapper incidentMapper;

    public IncidentService(IncidentRepository incidentRepository, IncidentMapper incidentMapper) {
        this.incidentRepository = incidentRepository;
        this.incidentMapper = incidentMapper;
    }

    /**
     * Save a incident.
     *
     * @param incidentDTO the entity to save.
     * @return the persisted entity.
     */
    public IncidentDTO save(IncidentDTO incidentDTO) {
        LOG.debug("Request to save Incident : {}", incidentDTO);
        Incident incident = incidentMapper.toEntity(incidentDTO);
        incident = incidentRepository.save(incident);
        return incidentMapper.toDto(incident);
    }

    /**
     * Update a incident.
     *
     * @param incidentDTO the entity to save.
     * @return the persisted entity.
     */
    public IncidentDTO update(IncidentDTO incidentDTO) {
        LOG.debug("Request to update Incident : {}", incidentDTO);
        Incident incident = incidentMapper.toEntity(incidentDTO);
        incident = incidentRepository.save(incident);
        return incidentMapper.toDto(incident);
    }

    /**
     * Partially update a incident.
     *
     * @param incidentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<IncidentDTO> partialUpdate(IncidentDTO incidentDTO) {
        LOG.debug("Request to partially update Incident : {}", incidentDTO);

        return incidentRepository
            .findById(incidentDTO.getId())
            .map(existingIncident -> {
                incidentMapper.partialUpdate(existingIncident, incidentDTO);

                return existingIncident;
            })
            .map(incidentRepository::save)
            .map(incidentMapper::toDto);
    }

    /**
     * Get all the incidents with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<IncidentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return incidentRepository.findAllWithEagerRelationships(pageable).map(incidentMapper::toDto);
    }

    /**
     * Get one incident by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<IncidentDTO> findOne(UUID id) {
        LOG.debug("Request to get Incident : {}", id);
        return incidentRepository.findOneWithEagerRelationships(id).map(incidentMapper::toDto);
    }

    /**
     * Delete the incident by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Incident : {}", id);
        incidentRepository.deleteById(id);
    }
}
