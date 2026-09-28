package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.service.dto.QuitusDTO;
import cm.gimac.des.certification.service.mapper.QuitusMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.Quitus}.
 */
@Service
@Transactional
public class QuitusService {

    private static final Logger LOG = LoggerFactory.getLogger(QuitusService.class);

    private final QuitusRepository quitusRepository;

    private final QuitusMapper quitusMapper;

    public QuitusService(QuitusRepository quitusRepository, QuitusMapper quitusMapper) {
        this.quitusRepository = quitusRepository;
        this.quitusMapper = quitusMapper;
    }

    /**
     * Save a quitus.
     *
     * @param quitusDTO the entity to save.
     * @return the persisted entity.
     */
    public QuitusDTO save(QuitusDTO quitusDTO) {
        LOG.debug("Request to save Quitus : {}", quitusDTO);
        Quitus quitus = quitusMapper.toEntity(quitusDTO);
        quitus = quitusRepository.save(quitus);
        return quitusMapper.toDto(quitus);
    }

    /**
     * Update a quitus.
     *
     * @param quitusDTO the entity to save.
     * @return the persisted entity.
     */
    public QuitusDTO update(QuitusDTO quitusDTO) {
        LOG.debug("Request to update Quitus : {}", quitusDTO);
        Quitus quitus = quitusMapper.toEntity(quitusDTO);
        quitus = quitusRepository.save(quitus);
        return quitusMapper.toDto(quitus);
    }

    /**
     * Partially update a quitus.
     *
     * @param quitusDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<QuitusDTO> partialUpdate(QuitusDTO quitusDTO) {
        LOG.debug("Request to partially update Quitus : {}", quitusDTO);

        return quitusRepository
            .findById(quitusDTO.getId())
            .map(existingQuitus -> {
                quitusMapper.partialUpdate(existingQuitus, quitusDTO);

                return existingQuitus;
            })
            .map(quitusRepository::save)
            .map(quitusMapper::toDto);
    }

    /**
     * Get all the quituses with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<QuitusDTO> findAllWithEagerRelationships(Pageable pageable) {
        return quitusRepository.findAllWithEagerRelationships(pageable).map(quitusMapper::toDto);
    }

    /**
     * Get one quitus by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<QuitusDTO> findOne(UUID id) {
        LOG.debug("Request to get Quitus : {}", id);
        return quitusRepository.findOneWithEagerRelationships(id).map(quitusMapper::toDto);
    }

    /**
     * Delete the quitus by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Quitus : {}", id);
        quitusRepository.deleteById(id);
    }
}
