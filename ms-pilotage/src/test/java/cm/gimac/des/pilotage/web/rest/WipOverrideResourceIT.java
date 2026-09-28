package cm.gimac.des.pilotage.web.rest;

import static cm.gimac.des.pilotage.domain.WipOverrideAsserts.*;
import static cm.gimac.des.pilotage.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.pilotage.IntegrationTest;
import cm.gimac.des.pilotage.domain.BoardColumn;
import cm.gimac.des.pilotage.domain.Task;
import cm.gimac.des.pilotage.domain.WipOverride;
import cm.gimac.des.pilotage.repository.WipOverrideRepository;
import cm.gimac.des.pilotage.service.WipOverrideService;
import cm.gimac.des.pilotage.service.dto.WipOverrideDTO;
import cm.gimac.des.pilotage.service.mapper.WipOverrideMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Integration tests for the {@link WipOverrideResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class WipOverrideResourceIT {

    private static final UUID DEFAULT_REQUESTER_ID = UUID.randomUUID();
    private static final UUID UPDATED_REQUESTER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROVER_ID = UUID.randomUUID();

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_GRANTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_GRANTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_EXPIRES_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRES_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_EXPIRY_NOTIFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRY_NOTIFIED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/wip-overrides";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private WipOverrideRepository wipOverrideRepository;

    @Mock
    private WipOverrideRepository wipOverrideRepositoryMock;

    @Autowired
    private WipOverrideMapper wipOverrideMapper;

    @Mock
    private WipOverrideService wipOverrideServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restWipOverrideMockMvc;

    private WipOverride wipOverride;

    private WipOverride insertedWipOverride;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static WipOverride createEntity(EntityManager em) {
        WipOverride wipOverride = new WipOverride()
            .requesterId(DEFAULT_REQUESTER_ID)
            .approverId(DEFAULT_APPROVER_ID)
            .reason(DEFAULT_REASON)
            .grantedAt(DEFAULT_GRANTED_AT)
            .expiresAt(DEFAULT_EXPIRES_AT)
            .expiryNotifiedAt(DEFAULT_EXPIRY_NOTIFIED_AT);
        // Add required entity
        BoardColumn boardColumn;
        if (TestUtil.findAll(em, BoardColumn.class).isEmpty()) {
            boardColumn = BoardColumnResourceIT.createEntity(em);
            em.persist(boardColumn);
            em.flush();
        } else {
            boardColumn = TestUtil.findAll(em, BoardColumn.class).get(0);
        }
        wipOverride.setBoardColumn(boardColumn);
        // Add required entity
        Task task;
        if (TestUtil.findAll(em, Task.class).isEmpty()) {
            task = TaskResourceIT.createEntity(em);
            em.persist(task);
            em.flush();
        } else {
            task = TestUtil.findAll(em, Task.class).get(0);
        }
        wipOverride.setTask(task);
        return wipOverride;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static WipOverride createUpdatedEntity(EntityManager em) {
        WipOverride updatedWipOverride = new WipOverride()
            .requesterId(UPDATED_REQUESTER_ID)
            .approverId(UPDATED_APPROVER_ID)
            .reason(UPDATED_REASON)
            .grantedAt(UPDATED_GRANTED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .expiryNotifiedAt(UPDATED_EXPIRY_NOTIFIED_AT);
        // Add required entity
        BoardColumn boardColumn;
        if (TestUtil.findAll(em, BoardColumn.class).isEmpty()) {
            boardColumn = BoardColumnResourceIT.createUpdatedEntity(em);
            em.persist(boardColumn);
            em.flush();
        } else {
            boardColumn = TestUtil.findAll(em, BoardColumn.class).get(0);
        }
        updatedWipOverride.setBoardColumn(boardColumn);
        // Add required entity
        Task task;
        if (TestUtil.findAll(em, Task.class).isEmpty()) {
            task = TaskResourceIT.createUpdatedEntity(em);
            em.persist(task);
            em.flush();
        } else {
            task = TestUtil.findAll(em, Task.class).get(0);
        }
        updatedWipOverride.setTask(task);
        return updatedWipOverride;
    }

    @BeforeEach
    void initTest() {
        wipOverride = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedWipOverride != null) {
            wipOverrideRepository.delete(insertedWipOverride);
            insertedWipOverride = null;
        }
    }

    @Test
    @Transactional
    void createWipOverride() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the WipOverride
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);
        var returnedWipOverrideDTO = om.readValue(
            restWipOverrideMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(wipOverrideDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            WipOverrideDTO.class
        );

        // Validate the WipOverride in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedWipOverride = wipOverrideMapper.toEntity(returnedWipOverrideDTO);
        assertWipOverrideUpdatableFieldsEquals(returnedWipOverride, getPersistedWipOverride(returnedWipOverride));

        insertedWipOverride = returnedWipOverride;
    }

    @Test
    @Transactional
    void createWipOverrideWithExistingId() throws Exception {
        // Create the WipOverride with an existing ID
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restWipOverrideMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the WipOverride in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkRequesterIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        wipOverride.setRequesterId(null);

        // Create the WipOverride, which fails.
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        restWipOverrideMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkApproverIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        wipOverride.setApproverId(null);

        // Create the WipOverride, which fails.
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        restWipOverrideMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReasonIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        wipOverride.setReason(null);

        // Create the WipOverride, which fails.
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        restWipOverrideMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkGrantedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        wipOverride.setGrantedAt(null);

        // Create the WipOverride, which fails.
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        restWipOverrideMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkExpiresAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        wipOverride.setExpiresAt(null);

        // Create the WipOverride, which fails.
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        restWipOverrideMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllWipOverrides() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList
        restWipOverrideMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(wipOverride.getId().toString())))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].grantedAt").value(hasItem(DEFAULT_GRANTED_AT.toString())))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].expiryNotifiedAt").value(hasItem(DEFAULT_EXPIRY_NOTIFIED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllWipOverridesWithEagerRelationshipsIsEnabled() throws Exception {
        when(wipOverrideServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restWipOverrideMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(wipOverrideServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllWipOverridesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(wipOverrideServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restWipOverrideMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(wipOverrideRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getWipOverride() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get the wipOverride
        restWipOverrideMockMvc
            .perform(get(ENTITY_API_URL_ID, wipOverride.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(wipOverride.getId().toString()))
            .andExpect(jsonPath("$.requesterId").value(DEFAULT_REQUESTER_ID.toString()))
            .andExpect(jsonPath("$.approverId").value(DEFAULT_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.grantedAt").value(DEFAULT_GRANTED_AT.toString()))
            .andExpect(jsonPath("$.expiresAt").value(DEFAULT_EXPIRES_AT.toString()))
            .andExpect(jsonPath("$.expiryNotifiedAt").value(DEFAULT_EXPIRY_NOTIFIED_AT.toString()));
    }

    @Test
    @Transactional
    void getWipOverridesByIdFiltering() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        UUID id = wipOverride.getId();

        defaultWipOverrideFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllWipOverridesByRequesterIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where requesterId equals to
        defaultWipOverrideFiltering("requesterId.equals=" + DEFAULT_REQUESTER_ID, "requesterId.equals=" + UPDATED_REQUESTER_ID);
    }

    @Test
    @Transactional
    void getAllWipOverridesByRequesterIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where requesterId in
        defaultWipOverrideFiltering(
            "requesterId.in=" + DEFAULT_REQUESTER_ID + "," + UPDATED_REQUESTER_ID,
            "requesterId.in=" + UPDATED_REQUESTER_ID
        );
    }

    @Test
    @Transactional
    void getAllWipOverridesByRequesterIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where requesterId is not null
        defaultWipOverrideFiltering("requesterId.specified=true", "requesterId.specified=false");
    }

    @Test
    @Transactional
    void getAllWipOverridesByApproverIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where approverId equals to
        defaultWipOverrideFiltering("approverId.equals=" + DEFAULT_APPROVER_ID, "approverId.equals=" + UPDATED_APPROVER_ID);
    }

    @Test
    @Transactional
    void getAllWipOverridesByApproverIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where approverId in
        defaultWipOverrideFiltering(
            "approverId.in=" + DEFAULT_APPROVER_ID + "," + UPDATED_APPROVER_ID,
            "approverId.in=" + UPDATED_APPROVER_ID
        );
    }

    @Test
    @Transactional
    void getAllWipOverridesByApproverIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where approverId is not null
        defaultWipOverrideFiltering("approverId.specified=true", "approverId.specified=false");
    }

    @Test
    @Transactional
    void getAllWipOverridesByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where reason equals to
        defaultWipOverrideFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllWipOverridesByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where reason in
        defaultWipOverrideFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllWipOverridesByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where reason is not null
        defaultWipOverrideFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllWipOverridesByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where reason contains
        defaultWipOverrideFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllWipOverridesByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where reason does not contain
        defaultWipOverrideFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllWipOverridesByGrantedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where grantedAt equals to
        defaultWipOverrideFiltering("grantedAt.equals=" + DEFAULT_GRANTED_AT, "grantedAt.equals=" + UPDATED_GRANTED_AT);
    }

    @Test
    @Transactional
    void getAllWipOverridesByGrantedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where grantedAt in
        defaultWipOverrideFiltering("grantedAt.in=" + DEFAULT_GRANTED_AT + "," + UPDATED_GRANTED_AT, "grantedAt.in=" + UPDATED_GRANTED_AT);
    }

    @Test
    @Transactional
    void getAllWipOverridesByGrantedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where grantedAt is not null
        defaultWipOverrideFiltering("grantedAt.specified=true", "grantedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllWipOverridesByExpiresAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where expiresAt equals to
        defaultWipOverrideFiltering("expiresAt.equals=" + DEFAULT_EXPIRES_AT, "expiresAt.equals=" + UPDATED_EXPIRES_AT);
    }

    @Test
    @Transactional
    void getAllWipOverridesByExpiresAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where expiresAt in
        defaultWipOverrideFiltering("expiresAt.in=" + DEFAULT_EXPIRES_AT + "," + UPDATED_EXPIRES_AT, "expiresAt.in=" + UPDATED_EXPIRES_AT);
    }

    @Test
    @Transactional
    void getAllWipOverridesByExpiresAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where expiresAt is not null
        defaultWipOverrideFiltering("expiresAt.specified=true", "expiresAt.specified=false");
    }

    @Test
    @Transactional
    void getAllWipOverridesByExpiryNotifiedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where expiryNotifiedAt equals to
        defaultWipOverrideFiltering(
            "expiryNotifiedAt.equals=" + DEFAULT_EXPIRY_NOTIFIED_AT,
            "expiryNotifiedAt.equals=" + UPDATED_EXPIRY_NOTIFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllWipOverridesByExpiryNotifiedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where expiryNotifiedAt in
        defaultWipOverrideFiltering(
            "expiryNotifiedAt.in=" + DEFAULT_EXPIRY_NOTIFIED_AT + "," + UPDATED_EXPIRY_NOTIFIED_AT,
            "expiryNotifiedAt.in=" + UPDATED_EXPIRY_NOTIFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllWipOverridesByExpiryNotifiedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        // Get all the wipOverrideList where expiryNotifiedAt is not null
        defaultWipOverrideFiltering("expiryNotifiedAt.specified=true", "expiryNotifiedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllWipOverridesByBoardColumnIsEqualToSomething() throws Exception {
        BoardColumn boardColumn;
        if (TestUtil.findAll(em, BoardColumn.class).isEmpty()) {
            wipOverrideRepository.saveAndFlush(wipOverride);
            boardColumn = BoardColumnResourceIT.createEntity(em);
        } else {
            boardColumn = TestUtil.findAll(em, BoardColumn.class).get(0);
        }
        em.persist(boardColumn);
        em.flush();
        wipOverride.setBoardColumn(boardColumn);
        wipOverrideRepository.saveAndFlush(wipOverride);
        UUID boardColumnId = boardColumn.getId();
        // Get all the wipOverrideList where boardColumn equals to boardColumnId
        defaultWipOverrideShouldBeFound("boardColumnId.equals=" + boardColumnId);

        // Get all the wipOverrideList where boardColumn equals to UUID.randomUUID()
        defaultWipOverrideShouldNotBeFound("boardColumnId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllWipOverridesByTaskIsEqualToSomething() throws Exception {
        Task task;
        if (TestUtil.findAll(em, Task.class).isEmpty()) {
            wipOverrideRepository.saveAndFlush(wipOverride);
            task = TaskResourceIT.createEntity(em);
        } else {
            task = TestUtil.findAll(em, Task.class).get(0);
        }
        em.persist(task);
        em.flush();
        wipOverride.setTask(task);
        wipOverrideRepository.saveAndFlush(wipOverride);
        UUID taskId = task.getId();
        // Get all the wipOverrideList where task equals to taskId
        defaultWipOverrideShouldBeFound("taskId.equals=" + taskId);

        // Get all the wipOverrideList where task equals to UUID.randomUUID()
        defaultWipOverrideShouldNotBeFound("taskId.equals=" + UUID.randomUUID());
    }

    private void defaultWipOverrideFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultWipOverrideShouldBeFound(shouldBeFound);
        defaultWipOverrideShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultWipOverrideShouldBeFound(String filter) throws Exception {
        restWipOverrideMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(wipOverride.getId().toString())))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].grantedAt").value(hasItem(DEFAULT_GRANTED_AT.toString())))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].expiryNotifiedAt").value(hasItem(DEFAULT_EXPIRY_NOTIFIED_AT.toString())));

        // Check, that the count call also returns 1
        restWipOverrideMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultWipOverrideShouldNotBeFound(String filter) throws Exception {
        restWipOverrideMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restWipOverrideMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingWipOverride() throws Exception {
        // Get the wipOverride
        restWipOverrideMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingWipOverride() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the wipOverride
        WipOverride updatedWipOverride = wipOverrideRepository.findById(wipOverride.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedWipOverride are not directly saved in db
        em.detach(updatedWipOverride);
        updatedWipOverride
            .requesterId(UPDATED_REQUESTER_ID)
            .approverId(UPDATED_APPROVER_ID)
            .reason(UPDATED_REASON)
            .grantedAt(UPDATED_GRANTED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .expiryNotifiedAt(UPDATED_EXPIRY_NOTIFIED_AT);
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(updatedWipOverride);

        restWipOverrideMockMvc
            .perform(
                put(ENTITY_API_URL_ID, wipOverrideDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isOk());

        // Validate the WipOverride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedWipOverrideToMatchAllProperties(updatedWipOverride);
    }

    @Test
    @Transactional
    void putNonExistingWipOverride() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        wipOverride.setId(UUID.randomUUID());

        // Create the WipOverride
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restWipOverrideMockMvc
            .perform(
                put(ENTITY_API_URL_ID, wipOverrideDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the WipOverride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchWipOverride() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        wipOverride.setId(UUID.randomUUID());

        // Create the WipOverride
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWipOverrideMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the WipOverride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamWipOverride() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        wipOverride.setId(UUID.randomUUID());

        // Create the WipOverride
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWipOverrideMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(wipOverrideDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the WipOverride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateWipOverrideWithPatch() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the wipOverride using partial update
        WipOverride partialUpdatedWipOverride = new WipOverride();
        partialUpdatedWipOverride.setId(wipOverride.getId());

        partialUpdatedWipOverride.reason(UPDATED_REASON).expiryNotifiedAt(UPDATED_EXPIRY_NOTIFIED_AT);

        restWipOverrideMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedWipOverride.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedWipOverride))
            )
            .andExpect(status().isOk());

        // Validate the WipOverride in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertWipOverrideUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedWipOverride, wipOverride),
            getPersistedWipOverride(wipOverride)
        );
    }

    @Test
    @Transactional
    void fullUpdateWipOverrideWithPatch() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the wipOverride using partial update
        WipOverride partialUpdatedWipOverride = new WipOverride();
        partialUpdatedWipOverride.setId(wipOverride.getId());

        partialUpdatedWipOverride
            .requesterId(UPDATED_REQUESTER_ID)
            .approverId(UPDATED_APPROVER_ID)
            .reason(UPDATED_REASON)
            .grantedAt(UPDATED_GRANTED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .expiryNotifiedAt(UPDATED_EXPIRY_NOTIFIED_AT);

        restWipOverrideMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedWipOverride.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedWipOverride))
            )
            .andExpect(status().isOk());

        // Validate the WipOverride in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertWipOverrideUpdatableFieldsEquals(partialUpdatedWipOverride, getPersistedWipOverride(partialUpdatedWipOverride));
    }

    @Test
    @Transactional
    void patchNonExistingWipOverride() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        wipOverride.setId(UUID.randomUUID());

        // Create the WipOverride
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restWipOverrideMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, wipOverrideDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the WipOverride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchWipOverride() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        wipOverride.setId(UUID.randomUUID());

        // Create the WipOverride
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWipOverrideMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the WipOverride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamWipOverride() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        wipOverride.setId(UUID.randomUUID());

        // Create the WipOverride
        WipOverrideDTO wipOverrideDTO = wipOverrideMapper.toDto(wipOverride);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restWipOverrideMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(wipOverrideDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the WipOverride in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteWipOverride() throws Exception {
        // Initialize the database
        insertedWipOverride = wipOverrideRepository.saveAndFlush(wipOverride);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the wipOverride
        restWipOverrideMockMvc
            .perform(delete(ENTITY_API_URL_ID, wipOverride.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return wipOverrideRepository.count();
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

    protected WipOverride getPersistedWipOverride(WipOverride wipOverride) {
        return wipOverrideRepository.findById(wipOverride.getId()).orElseThrow();
    }

    protected void assertPersistedWipOverrideToMatchAllProperties(WipOverride expectedWipOverride) {
        assertWipOverrideAllPropertiesEquals(expectedWipOverride, getPersistedWipOverride(expectedWipOverride));
    }

    protected void assertPersistedWipOverrideToMatchUpdatableProperties(WipOverride expectedWipOverride) {
        assertWipOverrideAllUpdatablePropertiesEquals(expectedWipOverride, getPersistedWipOverride(expectedWipOverride));
    }
}
