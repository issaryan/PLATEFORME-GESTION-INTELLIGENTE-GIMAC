package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.QualificationMatrix;
import cm.gimac.des.incident.repository.QualificationMatrixRepository;
import cm.gimac.des.incident.service.dto.QualificationMatrixDTO;
import cm.gimac.des.incident.service.mapper.QualificationMatrixMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.QualificationMatrix}.
 */
@Service
@Transactional
public class QualificationMatrixService {

    private static final Logger LOG = LoggerFactory.getLogger(QualificationMatrixService.class);

    private final QualificationMatrixRepository qualificationMatrixRepository;

    private final QualificationMatrixMapper qualificationMatrixMapper;

    public QualificationMatrixService(
        QualificationMatrixRepository qualificationMatrixRepository,
        QualificationMatrixMapper qualificationMatrixMapper
    ) {
        this.qualificationMatrixRepository = qualificationMatrixRepository;
        this.qualificationMatrixMapper = qualificationMatrixMapper;
    }

    /**
     * Save a qualificationMatrix.
     *
     * @param qualificationMatrixDTO the entity to save.
     * @return the persisted entity.
     */
    public QualificationMatrixDTO save(QualificationMatrixDTO qualificationMatrixDTO) {
        LOG.debug("Request to save QualificationMatrix : {}", qualificationMatrixDTO);
        QualificationMatrix qualificationMatrix = qualificationMatrixMapper.toEntity(qualificationMatrixDTO);
        qualificationMatrix = qualificationMatrixRepository.save(qualificationMatrix);
        return qualificationMatrixMapper.toDto(qualificationMatrix);
    }

    /**
     * Update a qualificationMatrix.
     *
     * @param qualificationMatrixDTO the entity to save.
     * @return the persisted entity.
     */
    public QualificationMatrixDTO update(QualificationMatrixDTO qualificationMatrixDTO) {
        LOG.debug("Request to update QualificationMatrix : {}", qualificationMatrixDTO);
        QualificationMatrix qualificationMatrix = qualificationMatrixMapper.toEntity(qualificationMatrixDTO);
        qualificationMatrix = qualificationMatrixRepository.save(qualificationMatrix);
        return qualificationMatrixMapper.toDto(qualificationMatrix);
    }

    /**
     * Partially update a qualificationMatrix.
     *
     * @param qualificationMatrixDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<QualificationMatrixDTO> partialUpdate(QualificationMatrixDTO qualificationMatrixDTO) {
        LOG.debug("Request to partially update QualificationMatrix : {}", qualificationMatrixDTO);

        return qualificationMatrixRepository
            .findById(qualificationMatrixDTO.getId())
            .map(existingQualificationMatrix -> {
                qualificationMatrixMapper.partialUpdate(existingQualificationMatrix, qualificationMatrixDTO);

                return existingQualificationMatrix;
            })
            .map(qualificationMatrixRepository::save)
            .map(qualificationMatrixMapper::toDto);
    }

    /**
     * Get all the qualificationMatrices.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<QualificationMatrixDTO> findAll() {
        LOG.debug("Request to get all QualificationMatrices");
        return qualificationMatrixRepository
            .findAll()
            .stream()
            .map(qualificationMatrixMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one qualificationMatrix by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<QualificationMatrixDTO> findOne(UUID id) {
        LOG.debug("Request to get QualificationMatrix : {}", id);
        return qualificationMatrixRepository.findById(id).map(qualificationMatrixMapper::toDto);
    }

    /**
     * Delete the qualificationMatrix by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete QualificationMatrix : {}", id);
        qualificationMatrixRepository.deleteById(id);
    }
}
