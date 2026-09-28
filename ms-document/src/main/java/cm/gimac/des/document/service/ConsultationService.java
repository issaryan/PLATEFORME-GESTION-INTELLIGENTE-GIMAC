package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.Consultation;
import cm.gimac.des.document.repository.ConsultationRepository;
import cm.gimac.des.document.service.dto.ConsultationDTO;
import cm.gimac.des.document.service.mapper.ConsultationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.document.domain.Consultation}.
 */
@Service
@Transactional
public class ConsultationService {

    private static final Logger LOG = LoggerFactory.getLogger(ConsultationService.class);

    private final ConsultationRepository consultationRepository;

    private final ConsultationMapper consultationMapper;

    public ConsultationService(ConsultationRepository consultationRepository, ConsultationMapper consultationMapper) {
        this.consultationRepository = consultationRepository;
        this.consultationMapper = consultationMapper;
    }

    /**
     * Save a consultation.
     *
     * @param consultationDTO the entity to save.
     * @return the persisted entity.
     */
    public ConsultationDTO save(ConsultationDTO consultationDTO) {
        LOG.debug("Request to save Consultation : {}", consultationDTO);
        Consultation consultation = consultationMapper.toEntity(consultationDTO);
        consultation = consultationRepository.save(consultation);
        return consultationMapper.toDto(consultation);
    }

    /**
     * Update a consultation.
     *
     * @param consultationDTO the entity to save.
     * @return the persisted entity.
     */
    public ConsultationDTO update(ConsultationDTO consultationDTO) {
        LOG.debug("Request to update Consultation : {}", consultationDTO);
        Consultation consultation = consultationMapper.toEntity(consultationDTO);
        consultation = consultationRepository.save(consultation);
        return consultationMapper.toDto(consultation);
    }

    /**
     * Partially update a consultation.
     *
     * @param consultationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ConsultationDTO> partialUpdate(ConsultationDTO consultationDTO) {
        LOG.debug("Request to partially update Consultation : {}", consultationDTO);

        return consultationRepository
            .findById(consultationDTO.getId())
            .map(existingConsultation -> {
                consultationMapper.partialUpdate(existingConsultation, consultationDTO);

                return existingConsultation;
            })
            .map(consultationRepository::save)
            .map(consultationMapper::toDto);
    }

    /**
     * Get all the consultations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ConsultationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return consultationRepository.findAllWithEagerRelationships(pageable).map(consultationMapper::toDto);
    }

    /**
     * Get one consultation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ConsultationDTO> findOne(UUID id) {
        LOG.debug("Request to get Consultation : {}", id);
        return consultationRepository.findOneWithEagerRelationships(id).map(consultationMapper::toDto);
    }

    /**
     * Delete the consultation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Consultation : {}", id);
        consultationRepository.deleteById(id);
    }
}
