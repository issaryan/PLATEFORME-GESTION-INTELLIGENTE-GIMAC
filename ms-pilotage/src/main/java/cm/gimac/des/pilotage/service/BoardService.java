package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.Board;
import cm.gimac.des.pilotage.repository.BoardRepository;
import cm.gimac.des.pilotage.service.dto.BoardDTO;
import cm.gimac.des.pilotage.service.mapper.BoardMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.Board}.
 */
@Service
@Transactional
public class BoardService {

    private static final Logger LOG = LoggerFactory.getLogger(BoardService.class);

    private final BoardRepository boardRepository;

    private final BoardMapper boardMapper;

    public BoardService(BoardRepository boardRepository, BoardMapper boardMapper) {
        this.boardRepository = boardRepository;
        this.boardMapper = boardMapper;
    }

    /**
     * Save a board.
     *
     * @param boardDTO the entity to save.
     * @return the persisted entity.
     */
    public BoardDTO save(BoardDTO boardDTO) {
        LOG.debug("Request to save Board : {}", boardDTO);
        Board board = boardMapper.toEntity(boardDTO);
        board = boardRepository.save(board);
        return boardMapper.toDto(board);
    }

    /**
     * Update a board.
     *
     * @param boardDTO the entity to save.
     * @return the persisted entity.
     */
    public BoardDTO update(BoardDTO boardDTO) {
        LOG.debug("Request to update Board : {}", boardDTO);
        Board board = boardMapper.toEntity(boardDTO);
        board = boardRepository.save(board);
        return boardMapper.toDto(board);
    }

    /**
     * Partially update a board.
     *
     * @param boardDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<BoardDTO> partialUpdate(BoardDTO boardDTO) {
        LOG.debug("Request to partially update Board : {}", boardDTO);

        return boardRepository
            .findById(boardDTO.getId())
            .map(existingBoard -> {
                boardMapper.partialUpdate(existingBoard, boardDTO);

                return existingBoard;
            })
            .map(boardRepository::save)
            .map(boardMapper::toDto);
    }

    /**
     * Get all the boards.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<BoardDTO> findAll() {
        LOG.debug("Request to get all Boards");
        return boardRepository.findAll().stream().map(boardMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one board by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<BoardDTO> findOne(UUID id) {
        LOG.debug("Request to get Board : {}", id);
        return boardRepository.findById(id).map(boardMapper::toDto);
    }

    /**
     * Delete the board by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Board : {}", id);
        boardRepository.deleteById(id);
    }
}
