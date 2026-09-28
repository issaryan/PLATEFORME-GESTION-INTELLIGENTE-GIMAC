package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.BoardColumn;
import cm.gimac.des.pilotage.repository.BoardColumnRepository;
import cm.gimac.des.pilotage.service.dto.BoardColumnDTO;
import cm.gimac.des.pilotage.service.mapper.BoardColumnMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.BoardColumn}.
 */
@Service
@Transactional
public class BoardColumnService {

    private static final Logger LOG = LoggerFactory.getLogger(BoardColumnService.class);

    private final BoardColumnRepository boardColumnRepository;

    private final BoardColumnMapper boardColumnMapper;

    public BoardColumnService(BoardColumnRepository boardColumnRepository, BoardColumnMapper boardColumnMapper) {
        this.boardColumnRepository = boardColumnRepository;
        this.boardColumnMapper = boardColumnMapper;
    }

    /**
     * Save a boardColumn.
     *
     * @param boardColumnDTO the entity to save.
     * @return the persisted entity.
     */
    public BoardColumnDTO save(BoardColumnDTO boardColumnDTO) {
        LOG.debug("Request to save BoardColumn : {}", boardColumnDTO);
        BoardColumn boardColumn = boardColumnMapper.toEntity(boardColumnDTO);
        boardColumn = boardColumnRepository.save(boardColumn);
        return boardColumnMapper.toDto(boardColumn);
    }

    /**
     * Update a boardColumn.
     *
     * @param boardColumnDTO the entity to save.
     * @return the persisted entity.
     */
    public BoardColumnDTO update(BoardColumnDTO boardColumnDTO) {
        LOG.debug("Request to update BoardColumn : {}", boardColumnDTO);
        BoardColumn boardColumn = boardColumnMapper.toEntity(boardColumnDTO);
        boardColumn = boardColumnRepository.save(boardColumn);
        return boardColumnMapper.toDto(boardColumn);
    }

    /**
     * Partially update a boardColumn.
     *
     * @param boardColumnDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<BoardColumnDTO> partialUpdate(BoardColumnDTO boardColumnDTO) {
        LOG.debug("Request to partially update BoardColumn : {}", boardColumnDTO);

        return boardColumnRepository
            .findById(boardColumnDTO.getId())
            .map(existingBoardColumn -> {
                boardColumnMapper.partialUpdate(existingBoardColumn, boardColumnDTO);

                return existingBoardColumn;
            })
            .map(boardColumnRepository::save)
            .map(boardColumnMapper::toDto);
    }

    /**
     * Get all the boardColumns.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<BoardColumnDTO> findAll() {
        LOG.debug("Request to get all BoardColumns");
        return boardColumnRepository.findAll().stream().map(boardColumnMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the boardColumns with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<BoardColumnDTO> findAllWithEagerRelationships(Pageable pageable) {
        return boardColumnRepository.findAllWithEagerRelationships(pageable).map(boardColumnMapper::toDto);
    }

    /**
     * Get one boardColumn by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<BoardColumnDTO> findOne(UUID id) {
        LOG.debug("Request to get BoardColumn : {}", id);
        return boardColumnRepository.findOneWithEagerRelationships(id).map(boardColumnMapper::toDto);
    }

    /**
     * Delete the boardColumn by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete BoardColumn : {}", id);
        boardColumnRepository.deleteById(id);
    }
}
