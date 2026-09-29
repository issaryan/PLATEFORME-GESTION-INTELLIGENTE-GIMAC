package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.service.dto.AdhesionPieceDTO;
import cm.gimac.des.participant.service.mapper.AdhesionPieceMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.AdhesionPiece}.
 */
@Service
@Transactional
public class AdhesionPieceService {

    private static final Logger LOG = LoggerFactory.getLogger(AdhesionPieceService.class);

    private final AdhesionPieceRepository adhesionPieceRepository;

    private final AdhesionPieceMapper adhesionPieceMapper;

    public AdhesionPieceService(AdhesionPieceRepository adhesionPieceRepository, AdhesionPieceMapper adhesionPieceMapper) {
        this.adhesionPieceRepository = adhesionPieceRepository;
        this.adhesionPieceMapper = adhesionPieceMapper;
    }

    /**
     * Save a adhesionPiece.
     *
     * @param adhesionPieceDTO the entity to save.
     * @return the persisted entity.
     */
    public AdhesionPieceDTO save(AdhesionPieceDTO adhesionPieceDTO) {
        LOG.debug("Request to save AdhesionPiece : {}", adhesionPieceDTO);
        AdhesionPiece adhesionPiece = adhesionPieceMapper.toEntity(adhesionPieceDTO);
        adhesionPiece = adhesionPieceRepository.save(adhesionPiece);
        return adhesionPieceMapper.toDto(adhesionPiece);
    }

    /**
     * Update a adhesionPiece.
     *
     * @param adhesionPieceDTO the entity to save.
     * @return the persisted entity.
     */
    public AdhesionPieceDTO update(AdhesionPieceDTO adhesionPieceDTO) {
        LOG.debug("Request to update AdhesionPiece : {}", adhesionPieceDTO);
        AdhesionPiece adhesionPiece = adhesionPieceMapper.toEntity(adhesionPieceDTO);
        adhesionPiece = adhesionPieceRepository.save(adhesionPiece);
        return adhesionPieceMapper.toDto(adhesionPiece);
    }

    /**
     * Partially update a adhesionPiece.
     *
     * @param adhesionPieceDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AdhesionPieceDTO> partialUpdate(AdhesionPieceDTO adhesionPieceDTO) {
        LOG.debug("Request to partially update AdhesionPiece : {}", adhesionPieceDTO);

        return adhesionPieceRepository
            .findById(adhesionPieceDTO.getId())
            .map(existingAdhesionPiece -> {
                adhesionPieceMapper.partialUpdate(existingAdhesionPiece, adhesionPieceDTO);

                return existingAdhesionPiece;
            })
            .map(adhesionPieceRepository::save)
            .map(adhesionPieceMapper::toDto);
    }

    /**
     * Get all the adhesionPieces.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AdhesionPieceDTO> findAll() {
        LOG.debug("Request to get all AdhesionPieces");
        return adhesionPieceRepository.findAll().stream().map(adhesionPieceMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the adhesionPieces with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<AdhesionPieceDTO> findAllWithEagerRelationships() {
        return adhesionPieceRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(adhesionPieceMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one adhesionPiece by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AdhesionPieceDTO> findOne(UUID id) {
        LOG.debug("Request to get AdhesionPiece : {}", id);
        return adhesionPieceRepository.findOneWithEagerRelationships(id).map(adhesionPieceMapper::toDto);
    }

    /**
     * Delete the adhesionPiece by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AdhesionPiece : {}", id);
        adhesionPieceRepository.deleteById(id);
    }
}
