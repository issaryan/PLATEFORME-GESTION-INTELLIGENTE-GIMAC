package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.BoardAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Board;
import cm.gimac.des.pilotage.repository.BoardRepository;
import cm.gimac.des.pilotage.service.dto.BoardDTO;
import cm.gimac.des.pilotage.service.mapper.BoardMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link BoardResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class BoardResourceIT {

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Long DEFAULT_VERSION = 0L;
    private static final Long UPDATED_VERSION = 1L;

    private static final String ENTITY_API_URL = "/api/boards";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardMapper boardMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restBoardMockMvc;

    private Board board;

    private Board insertedBoard;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Board createEntity() {
        return new Board().serviceCode(DEFAULT_SERVICE_CODE).name(DEFAULT_NAME).version(DEFAULT_VERSION);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Board createUpdatedEntity() {
        return new Board().serviceCode(UPDATED_SERVICE_CODE).name(UPDATED_NAME).version(UPDATED_VERSION);
    }

    @BeforeEach
    void initTest() {
        board = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedBoard != null) {
            boardRepository.delete(insertedBoard);
            insertedBoard = null;
        }
    }

    @Test
    @Transactional
    void createBoard() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Board
        BoardDTO boardDTO = boardMapper.toDto(board);
        var returnedBoardDTO = om.readValue(
            restBoardMockMvc
                .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            BoardDTO.class
        );

        // Validate the Board in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedBoard = boardMapper.toEntity(returnedBoardDTO);
        assertBoardUpdatableFieldsEquals(returnedBoard, getPersistedBoard(returnedBoard));

        insertedBoard = returnedBoard;
    }

    @Test
    @Transactional
    void createBoardWithExistingId() throws Exception {
        // Create the Board with an existing ID
        insertedBoard = boardRepository.saveAndFlush(board);
        BoardDTO boardDTO = boardMapper.toDto(board);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restBoardMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Board in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkServiceCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        board.setServiceCode(null);

        // Create the Board, which fails.
        BoardDTO boardDTO = boardMapper.toDto(board);

        restBoardMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        board.setName(null);

        // Create the Board, which fails.
        BoardDTO boardDTO = boardMapper.toDto(board);

        restBoardMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVersionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        board.setVersion(null);

        // Create the Board, which fails.
        BoardDTO boardDTO = boardMapper.toDto(board);

        restBoardMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllBoards() throws Exception {
        // Initialize the database
        insertedBoard = boardRepository.saveAndFlush(board);

        // Get all the boardList
        restBoardMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(board.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION.intValue())));
    }

    @Test
    @Transactional
    void getBoard() throws Exception {
        // Initialize the database
        insertedBoard = boardRepository.saveAndFlush(board);

        // Get the board
        restBoardMockMvc
            .perform(get(ENTITY_API_URL_ID, board.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(board.getId().toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION.intValue()));
    }

    @Test
    @Transactional
    void getNonExistingBoard() throws Exception {
        // Get the board
        restBoardMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingBoard() throws Exception {
        // Initialize the database
        insertedBoard = boardRepository.saveAndFlush(board);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the board
        Board updatedBoard = boardRepository.findById(board.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedBoard are not directly saved in db
        em.detach(updatedBoard);
        updatedBoard.serviceCode(UPDATED_SERVICE_CODE).name(UPDATED_NAME).version(UPDATED_VERSION);
        BoardDTO boardDTO = boardMapper.toDto(updatedBoard);

        restBoardMockMvc
            .perform(
                put(ENTITY_API_URL_ID, boardDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(boardDTO))
            )
            .andExpect(status().isOk());

        // Validate the Board in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedBoardToMatchAllProperties(updatedBoard);
    }

    @Test
    @Transactional
    void putNonExistingBoard() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        board.setId(UUID.randomUUID());

        // Create the Board
        BoardDTO boardDTO = boardMapper.toDto(board);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBoardMockMvc
            .perform(
                put(ENTITY_API_URL_ID, boardDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(boardDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Board in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchBoard() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        board.setId(UUID.randomUUID());

        // Create the Board
        BoardDTO boardDTO = boardMapper.toDto(board);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBoardMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(boardDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Board in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamBoard() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        board.setId(UUID.randomUUID());

        // Create the Board
        BoardDTO boardDTO = boardMapper.toDto(board);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBoardMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Board in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateBoardWithPatch() throws Exception {
        // Initialize the database
        insertedBoard = boardRepository.saveAndFlush(board);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the board using partial update
        Board partialUpdatedBoard = new Board();
        partialUpdatedBoard.setId(board.getId());

        partialUpdatedBoard.serviceCode(UPDATED_SERVICE_CODE).name(UPDATED_NAME).version(UPDATED_VERSION);

        restBoardMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedBoard.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedBoard))
            )
            .andExpect(status().isOk());

        // Validate the Board in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBoardUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedBoard, board), getPersistedBoard(board));
    }

    @Test
    @Transactional
    void fullUpdateBoardWithPatch() throws Exception {
        // Initialize the database
        insertedBoard = boardRepository.saveAndFlush(board);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the board using partial update
        Board partialUpdatedBoard = new Board();
        partialUpdatedBoard.setId(board.getId());

        partialUpdatedBoard.serviceCode(UPDATED_SERVICE_CODE).name(UPDATED_NAME).version(UPDATED_VERSION);

        restBoardMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedBoard.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedBoard))
            )
            .andExpect(status().isOk());

        // Validate the Board in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBoardUpdatableFieldsEquals(partialUpdatedBoard, getPersistedBoard(partialUpdatedBoard));
    }

    @Test
    @Transactional
    void patchNonExistingBoard() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        board.setId(UUID.randomUUID());

        // Create the Board
        BoardDTO boardDTO = boardMapper.toDto(board);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBoardMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, boardDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(boardDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Board in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchBoard() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        board.setId(UUID.randomUUID());

        // Create the Board
        BoardDTO boardDTO = boardMapper.toDto(board);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBoardMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(boardDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Board in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamBoard() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        board.setId(UUID.randomUUID());

        // Create the Board
        BoardDTO boardDTO = boardMapper.toDto(board);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBoardMockMvc
            .perform(patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(boardDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Board in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteBoard() throws Exception {
        // Initialize the database
        insertedBoard = boardRepository.saveAndFlush(board);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the board
        restBoardMockMvc
            .perform(delete(ENTITY_API_URL_ID, board.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return boardRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Board getPersistedBoard(Board board) {
        return boardRepository.findById(board.getId()).orElseThrow();
    }

    protected void assertPersistedBoardToMatchAllProperties(Board expectedBoard) {
        assertBoardAllPropertiesEquals(expectedBoard, getPersistedBoard(expectedBoard));
    }

    protected void assertPersistedBoardToMatchUpdatableProperties(Board expectedBoard) {
        assertBoardAllUpdatablePropertiesEquals(expectedBoard, getPersistedBoard(expectedBoard));
    }
}
