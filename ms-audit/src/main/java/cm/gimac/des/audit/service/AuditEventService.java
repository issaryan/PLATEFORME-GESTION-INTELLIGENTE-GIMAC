package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.AuditEvent;
import cm.gimac.des.audit.repository.AuditEventRepository;
import cm.gimac.des.audit.service.dto.AuditEventDTO;
import cm.gimac.des.audit.service.mapper.AuditEventMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.audit.domain.AuditEvent}.
 */
@Service
@Transactional
public class AuditEventService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditEventService.class);

    private final AuditEventRepository auditEventRepository;

    private final AuditEventMapper auditEventMapper;

    public AuditEventService(AuditEventRepository auditEventRepository, AuditEventMapper auditEventMapper) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventMapper = auditEventMapper;
    }

    /**
     * Save a auditEvent.
     *
     * @param auditEventDTO the entity to save.
     * @return the persisted entity.
     */
    public AuditEventDTO save(AuditEventDTO auditEventDTO) {
        LOG.debug("Request to save AuditEvent : {}", auditEventDTO);
        AuditEvent auditEvent = auditEventMapper.toEntity(auditEventDTO);
        auditEvent = auditEventRepository.save(auditEvent);
        return auditEventMapper.toDto(auditEvent);
    }

    /**
     * Update a auditEvent.
     *
     * @param auditEventDTO the entity to save.
     * @return the persisted entity.
     */
    public AuditEventDTO update(AuditEventDTO auditEventDTO) {
        LOG.debug("Request to update AuditEvent : {}", auditEventDTO);
        AuditEvent auditEvent = auditEventMapper.toEntity(auditEventDTO);
        auditEvent = auditEventRepository.save(auditEvent);
        return auditEventMapper.toDto(auditEvent);
    }

    /**
     * Partially update a auditEvent.
     *
     * @param auditEventDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AuditEventDTO> partialUpdate(AuditEventDTO auditEventDTO) {
        LOG.debug("Request to partially update AuditEvent : {}", auditEventDTO);

        return auditEventRepository
            .findById(auditEventDTO.getId())
            .map(existingAuditEvent -> {
                auditEventMapper.partialUpdate(existingAuditEvent, auditEventDTO);

                return existingAuditEvent;
            })
            .map(auditEventRepository::save)
            .map(auditEventMapper::toDto);
    }

    /**
     * Get one auditEvent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AuditEventDTO> findOne(UUID id) {
        LOG.debug("Request to get AuditEvent : {}", id);
        return auditEventRepository.findById(id).map(auditEventMapper::toDto);
    }

    /**
     * Delete the auditEvent by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AuditEvent : {}", id);
        auditEventRepository.deleteById(id);
    }
}
