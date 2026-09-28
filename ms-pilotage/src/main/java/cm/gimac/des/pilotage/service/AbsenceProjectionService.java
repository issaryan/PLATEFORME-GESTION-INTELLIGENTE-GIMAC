package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.AbsenceProjection;
import cm.gimac.des.pilotage.repository.AbsenceProjectionRepository;
import cm.gimac.des.pilotage.service.dto.AbsenceProjectionDTO;
import cm.gimac.des.pilotage.service.mapper.AbsenceProjectionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.AbsenceProjection}.
 */
@Service
@Transactional
public class AbsenceProjectionService {

    private static final Logger LOG = LoggerFactory.getLogger(AbsenceProjectionService.class);

    private final AbsenceProjectionRepository absenceProjectionRepository;

    private final AbsenceProjectionMapper absenceProjectionMapper;

    public AbsenceProjectionService(
        AbsenceProjectionRepository absenceProjectionRepository,
        AbsenceProjectionMapper absenceProjectionMapper
    ) {
        this.absenceProjectionRepository = absenceProjectionRepository;
        this.absenceProjectionMapper = absenceProjectionMapper;
    }

    /**
     * Save a absenceProjection.
     *
     * @param absenceProjectionDTO the entity to save.
     * @return the persisted entity.
     */
    public AbsenceProjectionDTO save(AbsenceProjectionDTO absenceProjectionDTO) {
        LOG.debug("Request to save AbsenceProjection : {}", absenceProjectionDTO);
        AbsenceProjection absenceProjection = absenceProjectionMapper.toEntity(absenceProjectionDTO);
        absenceProjection = absenceProjectionRepository.save(absenceProjection);
        return absenceProjectionMapper.toDto(absenceProjection);
    }

    /**
     * Update a absenceProjection.
     *
     * @param absenceProjectionDTO the entity to save.
     * @return the persisted entity.
     */
    public AbsenceProjectionDTO update(AbsenceProjectionDTO absenceProjectionDTO) {
        LOG.debug("Request to update AbsenceProjection : {}", absenceProjectionDTO);
        AbsenceProjection absenceProjection = absenceProjectionMapper.toEntity(absenceProjectionDTO);
        absenceProjection = absenceProjectionRepository.save(absenceProjection);
        return absenceProjectionMapper.toDto(absenceProjection);
    }

    /**
     * Partially update a absenceProjection.
     *
     * @param absenceProjectionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AbsenceProjectionDTO> partialUpdate(AbsenceProjectionDTO absenceProjectionDTO) {
        LOG.debug("Request to partially update AbsenceProjection : {}", absenceProjectionDTO);

        return absenceProjectionRepository
            .findById(absenceProjectionDTO.getId())
            .map(existingAbsenceProjection -> {
                absenceProjectionMapper.partialUpdate(existingAbsenceProjection, absenceProjectionDTO);

                return existingAbsenceProjection;
            })
            .map(absenceProjectionRepository::save)
            .map(absenceProjectionMapper::toDto);
    }

    /**
     * Get one absenceProjection by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AbsenceProjectionDTO> findOne(UUID id) {
        LOG.debug("Request to get AbsenceProjection : {}", id);
        return absenceProjectionRepository.findById(id).map(absenceProjectionMapper::toDto);
    }

    /**
     * Delete the absenceProjection by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AbsenceProjection : {}", id);
        absenceProjectionRepository.deleteById(id);
    }
}
