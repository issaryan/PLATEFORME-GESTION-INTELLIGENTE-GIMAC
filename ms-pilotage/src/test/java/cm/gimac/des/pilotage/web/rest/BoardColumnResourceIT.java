package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.BoardColumnAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.Board;
import cm.gimac.des.pilotage.domain.BoardColumn;
import cm.gimac.des.pilotage.repository.BoardColumnRepository;
import cm.gimac.des.pilotage.service.BoardColumnService;
import cm.gimac.des.pilotage.service.dto.BoardColumnDTO;
import cm.gimac.des.pilotage.service.mapper.BoardColumnMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link BoardColumnResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class BoardColumnResourceIT {

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final Integer DEFAULT_POSITION = 0;
    private static final Integer UPDATED_POSITION = 1;

    private static final Integer DEFAULT_WIP_LIMIT = 0;
    private static final Integer UPDATED_WIP_LIMIT = 1;

    private static final Boolean DEFAULT_TERMINAL = false;
    private static final Boolean UPDATED_TERMINAL = true;

    private static final String ENTITY_API_URL = "/api/board-columns";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private BoardColumnRepository boardColumnRepositoryMock;

    @Autowired
    private BoardColumnMapper boardColumnMapper;

    @Mock
    private BoardColumnService boardColumnServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restBoardColumnMockMvc;

    private BoardColumn boardColumn;

    private BoardColumn insertedBoardColumn;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static BoardColumn createEntity(EntityManager em) {
        BoardColumn boardColumn = new BoardColumn()
            .code(DEFAULT_CODE)
            .label(DEFAULT_LABEL)
            .position(DEFAULT_POSITION)
            .wipLimit(DEFAULT_WIP_LIMIT)
            .terminal(DEFAULT_TERMINAL);
        // Add required entity
        Board board;
        if (TestUtil.findAll(em, Board.class).isEmpty()) {
            board = BoardResourceIT.createEntity();
            em.persist(board);
            em.flush();
        } else {
            board = TestUtil.findAll(em, Board.class).get(0);
        }
        boardColumn.setBoard(board);
        return boardColumn;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static BoardColumn createUpdatedEntity(EntityManager em) {
        BoardColumn updatedBoardColumn = new BoardColumn()
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .position(UPDATED_POSITION)
            .wipLimit(UPDATED_WIP_LIMIT)
            .terminal(UPDATED_TERMINAL);
        // Add required entity
        Board board;
        if (TestUtil.findAll(em, Board.class).isEmpty()) {
            board = BoardResourceIT.createUpdatedEntity();
            em.persist(board);
            em.flush();
        } else {
            board = TestUtil.findAll(em, Board.class).get(0);
        }
        updatedBoardColumn.setBoard(board);
        return updatedBoardColumn;
    }

    @BeforeEach
    void initTest() {
        boardColumn = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedBoardColumn != null) {
            boardColumnRepository.delete(insertedBoardColumn);
            insertedBoardColumn = null;
        }
    }

    @Test
    @Transactional
    void createBoardColumn() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the BoardColumn
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);
        var returnedBoardColumnDTO = om.readValue(
            restBoardColumnMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardColumnDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            BoardColumnDTO.class
        );

        // Validate the BoardColumn in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedBoardColumn = boardColumnMapper.toEntity(returnedBoardColumnDTO);
        assertBoardColumnUpdatableFieldsEquals(returnedBoardColumn, getPersistedBoardColumn(returnedBoardColumn));

        insertedBoardColumn = returnedBoardColumn;
    }

    @Test
    @Transactional
    void createBoardColumnWithExistingId() throws Exception {
        // Create the BoardColumn with an existing ID
        insertedBoardColumn = boardColumnRepository.saveAndFlush(boardColumn);
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restBoardColumnMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BoardColumn in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        boardColumn.setCode(null);

        // Create the BoardColumn, which fails.
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        restBoardColumnMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkLabelIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        boardColumn.setLabel(null);

        // Create the BoardColumn, which fails.
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        restBoardColumnMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkPositionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        boardColumn.setPosition(null);

        // Create the BoardColumn, which fails.
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        restBoardColumnMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkWipLimitIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        boardColumn.setWipLimit(null);

        // Create the BoardColumn, which fails.
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        restBoardColumnMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTerminalIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        boardColumn.setTerminal(null);

        // Create the BoardColumn, which fails.
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        restBoardColumnMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllBoardColumns() throws Exception {
        // Initialize the database
        insertedBoardColumn = boardColumnRepository.saveAndFlush(boardColumn);

        // Get all the boardColumnList
        restBoardColumnMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(boardColumn.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL)))
            .andExpect(jsonPath("$.[*].position").value(hasItem(DEFAULT_POSITION)))
            .andExpect(jsonPath("$.[*].wipLimit").value(hasItem(DEFAULT_WIP_LIMIT)))
            .andExpect(jsonPath("$.[*].terminal").value(hasItem(DEFAULT_TERMINAL)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllBoardColumnsWithEagerRelationshipsIsEnabled() throws Exception {
        when(boardColumnServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restBoardColumnMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(boardColumnServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllBoardColumnsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(boardColumnServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restBoardColumnMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(boardColumnRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getBoardColumn() throws Exception {
        // Initialize the database
        insertedBoardColumn = boardColumnRepository.saveAndFlush(boardColumn);

        // Get the boardColumn
        restBoardColumnMockMvc
            .perform(get(ENTITY_API_URL_ID, boardColumn.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(boardColumn.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL))
            .andExpect(jsonPath("$.position").value(DEFAULT_POSITION))
            .andExpect(jsonPath("$.wipLimit").value(DEFAULT_WIP_LIMIT))
            .andExpect(jsonPath("$.terminal").value(DEFAULT_TERMINAL));
    }

    @Test
    @Transactional
    void getNonExistingBoardColumn() throws Exception {
        // Get the boardColumn
        restBoardColumnMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingBoardColumn() throws Exception {
        // Initialize the database
        insertedBoardColumn = boardColumnRepository.saveAndFlush(boardColumn);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the boardColumn
        BoardColumn updatedBoardColumn = boardColumnRepository.findById(boardColumn.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedBoardColumn are not directly saved in db
        em.detach(updatedBoardColumn);
        updatedBoardColumn
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .position(UPDATED_POSITION)
            .wipLimit(UPDATED_WIP_LIMIT)
            .terminal(UPDATED_TERMINAL);
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(updatedBoardColumn);

        restBoardColumnMockMvc
            .perform(
                put(ENTITY_API_URL_ID, boardColumnDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isOk());

        // Validate the BoardColumn in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedBoardColumnToMatchAllProperties(updatedBoardColumn);
    }

    @Test
    @Transactional
    void putNonExistingBoardColumn() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        boardColumn.setId(UUID.randomUUID());

        // Create the BoardColumn
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBoardColumnMockMvc
            .perform(
                put(ENTITY_API_URL_ID, boardColumnDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BoardColumn in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchBoardColumn() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        boardColumn.setId(UUID.randomUUID());

        // Create the BoardColumn
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBoardColumnMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BoardColumn in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamBoardColumn() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        boardColumn.setId(UUID.randomUUID());

        // Create the BoardColumn
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBoardColumnMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(boardColumnDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the BoardColumn in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateBoardColumnWithPatch() throws Exception {
        // Initialize the database
        insertedBoardColumn = boardColumnRepository.saveAndFlush(boardColumn);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the boardColumn using partial update
        BoardColumn partialUpdatedBoardColumn = new BoardColumn();
        partialUpdatedBoardColumn.setId(boardColumn.getId());

        restBoardColumnMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedBoardColumn.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedBoardColumn))
            )
            .andExpect(status().isOk());

        // Validate the BoardColumn in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBoardColumnUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedBoardColumn, boardColumn),
            getPersistedBoardColumn(boardColumn)
        );
    }

    @Test
    @Transactional
    void fullUpdateBoardColumnWithPatch() throws Exception {
        // Initialize the database
        insertedBoardColumn = boardColumnRepository.saveAndFlush(boardColumn);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the boardColumn using partial update
        BoardColumn partialUpdatedBoardColumn = new BoardColumn();
        partialUpdatedBoardColumn.setId(boardColumn.getId());

        partialUpdatedBoardColumn
            .code(UPDATED_CODE)
            .label(UPDATED_LABEL)
            .position(UPDATED_POSITION)
            .wipLimit(UPDATED_WIP_LIMIT)
            .terminal(UPDATED_TERMINAL);

        restBoardColumnMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedBoardColumn.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedBoardColumn))
            )
            .andExpect(status().isOk());

        // Validate the BoardColumn in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBoardColumnUpdatableFieldsEquals(partialUpdatedBoardColumn, getPersistedBoardColumn(partialUpdatedBoardColumn));
    }

    @Test
    @Transactional
    void patchNonExistingBoardColumn() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        boardColumn.setId(UUID.randomUUID());

        // Create the BoardColumn
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBoardColumnMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, boardColumnDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BoardColumn in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchBoardColumn() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        boardColumn.setId(UUID.randomUUID());

        // Create the BoardColumn
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBoardColumnMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BoardColumn in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamBoardColumn() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        boardColumn.setId(UUID.randomUUID());

        // Create the BoardColumn
        BoardColumnDTO boardColumnDTO = boardColumnMapper.toDto(boardColumn);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBoardColumnMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(boardColumnDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the BoardColumn in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteBoardColumn() throws Exception {
        // Initialize the database
        insertedBoardColumn = boardColumnRepository.saveAndFlush(boardColumn);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the boardColumn
        restBoardColumnMockMvc
            .perform(delete(ENTITY_API_URL_ID, boardColumn.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return boardColumnRepository.count();
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

    protected BoardColumn getPersistedBoardColumn(BoardColumn boardColumn) {
        return boardColumnRepository.findById(boardColumn.getId()).orElseThrow();
    }

    protected void assertPersistedBoardColumnToMatchAllProperties(BoardColumn expectedBoardColumn) {
        assertBoardColumnAllPropertiesEquals(expectedBoardColumn, getPersistedBoardColumn(expectedBoardColumn));
    }

    protected void assertPersistedBoardColumnToMatchUpdatableProperties(BoardColumn expectedBoardColumn) {
        assertBoardColumnAllUpdatablePropertiesEquals(expectedBoardColumn, getPersistedBoardColumn(expectedBoardColumn));
    }
}
