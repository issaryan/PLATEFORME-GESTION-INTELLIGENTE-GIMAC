package cm.gimac.des.document.web.rest;

import static cm.gimac.des.document.domain.PurgeRequestAsserts.*;
import static cm.gimac.des.document.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.PurgeRequest;
import cm.gimac.des.document.domain.enumeration.PurgeState;
import cm.gimac.des.document.repository.PurgeRequestRepository;
import cm.gimac.des.document.service.PurgeRequestService;
import cm.gimac.des.document.service.dto.PurgeRequestDTO;
import cm.gimac.des.document.service.mapper.PurgeRequestMapper;
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
 * Integration tests for the {@link PurgeRequestResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class PurgeRequestResourceIT {

    private static final UUID DEFAULT_REQUESTER_ID = UUID.randomUUID();
    private static final UUID UPDATED_REQUESTER_ID = UUID.randomUUID();

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_REQUESTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REQUESTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROVER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_APPROVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_APPROVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final PurgeState DEFAULT_STATE = PurgeState.DEMANDEE;
    private static final PurgeState UPDATED_STATE = PurgeState.APPROUVEE;

    private static final String DEFAULT_REJECTION_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REJECTION_REASON = "BBBBBBBBBB";

    private static final Instant DEFAULT_EXECUTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXECUTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/purge-requests";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PurgeRequestRepository purgeRequestRepository;

    @Mock
    private PurgeRequestRepository purgeRequestRepositoryMock;

    @Autowired
    private PurgeRequestMapper purgeRequestMapper;

    @Mock
    private PurgeRequestService purgeRequestServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPurgeRequestMockMvc;

    private PurgeRequest purgeRequest;

    private PurgeRequest insertedPurgeRequest;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PurgeRequest createEntity(EntityManager em) {
        PurgeRequest purgeRequest = new PurgeRequest()
            .requesterId(DEFAULT_REQUESTER_ID)
            .reason(DEFAULT_REASON)
            .requestedAt(DEFAULT_REQUESTED_AT)
            .approverId(DEFAULT_APPROVER_ID)
            .approvedAt(DEFAULT_APPROVED_AT)
            .state(DEFAULT_STATE)
            .rejectionReason(DEFAULT_REJECTION_REASON)
            .executedAt(DEFAULT_EXECUTED_AT);
        // Add required entity
        Document document;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            document = DocumentResourceIT.createEntity(em);
            em.persist(document);
            em.flush();
        } else {
            document = TestUtil.findAll(em, Document.class).get(0);
        }
        purgeRequest.setDocument(document);
        return purgeRequest;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PurgeRequest createUpdatedEntity(EntityManager em) {
        PurgeRequest updatedPurgeRequest = new PurgeRequest()
            .requesterId(UPDATED_REQUESTER_ID)
            .reason(UPDATED_REASON)
            .requestedAt(UPDATED_REQUESTED_AT)
            .approverId(UPDATED_APPROVER_ID)
            .approvedAt(UPDATED_APPROVED_AT)
            .state(UPDATED_STATE)
            .rejectionReason(UPDATED_REJECTION_REASON)
            .executedAt(UPDATED_EXECUTED_AT);
        // Add required entity
        Document document;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            document = DocumentResourceIT.createUpdatedEntity(em);
            em.persist(document);
            em.flush();
        } else {
            document = TestUtil.findAll(em, Document.class).get(0);
        }
        updatedPurgeRequest.setDocument(document);
        return updatedPurgeRequest;
    }

    @BeforeEach
    void initTest() {
        purgeRequest = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedPurgeRequest != null) {
            purgeRequestRepository.delete(insertedPurgeRequest);
            insertedPurgeRequest = null;
        }
    }

    @Test
    @Transactional
    void createPurgeRequest() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PurgeRequest
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);
        var returnedPurgeRequestDTO = om.readValue(
            restPurgeRequestMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(purgeRequestDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            PurgeRequestDTO.class
        );

        // Validate the PurgeRequest in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPurgeRequest = purgeRequestMapper.toEntity(returnedPurgeRequestDTO);
        assertPurgeRequestUpdatableFieldsEquals(returnedPurgeRequest, getPersistedPurgeRequest(returnedPurgeRequest));

        insertedPurgeRequest = returnedPurgeRequest;
    }

    @Test
    @Transactional
    void createPurgeRequestWithExistingId() throws Exception {
        // Create the PurgeRequest with an existing ID
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restPurgeRequestMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PurgeRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkRequesterIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        purgeRequest.setRequesterId(null);

        // Create the PurgeRequest, which fails.
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        restPurgeRequestMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkReasonIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        purgeRequest.setReason(null);

        // Create the PurgeRequest, which fails.
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        restPurgeRequestMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRequestedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        purgeRequest.setRequestedAt(null);

        // Create the PurgeRequest, which fails.
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        restPurgeRequestMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        purgeRequest.setState(null);

        // Create the PurgeRequest, which fails.
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        restPurgeRequestMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllPurgeRequests() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList
        restPurgeRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(purgeRequest.getId().toString())))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].rejectionReason").value(hasItem(DEFAULT_REJECTION_REASON)))
            .andExpect(jsonPath("$.[*].executedAt").value(hasItem(DEFAULT_EXECUTED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPurgeRequestsWithEagerRelationshipsIsEnabled() throws Exception {
        when(purgeRequestServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPurgeRequestMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(purgeRequestServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPurgeRequestsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(purgeRequestServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restPurgeRequestMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(purgeRequestRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getPurgeRequest() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get the purgeRequest
        restPurgeRequestMockMvc
            .perform(get(ENTITY_API_URL_ID, purgeRequest.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(purgeRequest.getId().toString()))
            .andExpect(jsonPath("$.requesterId").value(DEFAULT_REQUESTER_ID.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.requestedAt").value(DEFAULT_REQUESTED_AT.toString()))
            .andExpect(jsonPath("$.approverId").value(DEFAULT_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.approvedAt").value(DEFAULT_APPROVED_AT.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.rejectionReason").value(DEFAULT_REJECTION_REASON))
            .andExpect(jsonPath("$.executedAt").value(DEFAULT_EXECUTED_AT.toString()));
    }

    @Test
    @Transactional
    void getPurgeRequestsByIdFiltering() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        UUID id = purgeRequest.getId();

        defaultPurgeRequestFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRequesterIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where requesterId equals to
        defaultPurgeRequestFiltering("requesterId.equals=" + DEFAULT_REQUESTER_ID, "requesterId.equals=" + UPDATED_REQUESTER_ID);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRequesterIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where requesterId in
        defaultPurgeRequestFiltering(
            "requesterId.in=" + DEFAULT_REQUESTER_ID + "," + UPDATED_REQUESTER_ID,
            "requesterId.in=" + UPDATED_REQUESTER_ID
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRequesterIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where requesterId is not null
        defaultPurgeRequestFiltering("requesterId.specified=true", "requesterId.specified=false");
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where reason equals to
        defaultPurgeRequestFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where reason in
        defaultPurgeRequestFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where reason is not null
        defaultPurgeRequestFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where reason contains
        defaultPurgeRequestFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where reason does not contain
        defaultPurgeRequestFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRequestedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where requestedAt equals to
        defaultPurgeRequestFiltering("requestedAt.equals=" + DEFAULT_REQUESTED_AT, "requestedAt.equals=" + UPDATED_REQUESTED_AT);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRequestedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where requestedAt in
        defaultPurgeRequestFiltering(
            "requestedAt.in=" + DEFAULT_REQUESTED_AT + "," + UPDATED_REQUESTED_AT,
            "requestedAt.in=" + UPDATED_REQUESTED_AT
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRequestedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where requestedAt is not null
        defaultPurgeRequestFiltering("requestedAt.specified=true", "requestedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByApproverIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where approverId equals to
        defaultPurgeRequestFiltering("approverId.equals=" + DEFAULT_APPROVER_ID, "approverId.equals=" + UPDATED_APPROVER_ID);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByApproverIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where approverId in
        defaultPurgeRequestFiltering(
            "approverId.in=" + DEFAULT_APPROVER_ID + "," + UPDATED_APPROVER_ID,
            "approverId.in=" + UPDATED_APPROVER_ID
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByApproverIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where approverId is not null
        defaultPurgeRequestFiltering("approverId.specified=true", "approverId.specified=false");
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByApprovedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where approvedAt equals to
        defaultPurgeRequestFiltering("approvedAt.equals=" + DEFAULT_APPROVED_AT, "approvedAt.equals=" + UPDATED_APPROVED_AT);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByApprovedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where approvedAt in
        defaultPurgeRequestFiltering(
            "approvedAt.in=" + DEFAULT_APPROVED_AT + "," + UPDATED_APPROVED_AT,
            "approvedAt.in=" + UPDATED_APPROVED_AT
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByApprovedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where approvedAt is not null
        defaultPurgeRequestFiltering("approvedAt.specified=true", "approvedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where state equals to
        defaultPurgeRequestFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where state in
        defaultPurgeRequestFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where state is not null
        defaultPurgeRequestFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRejectionReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where rejectionReason equals to
        defaultPurgeRequestFiltering(
            "rejectionReason.equals=" + DEFAULT_REJECTION_REASON,
            "rejectionReason.equals=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRejectionReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where rejectionReason in
        defaultPurgeRequestFiltering(
            "rejectionReason.in=" + DEFAULT_REJECTION_REASON + "," + UPDATED_REJECTION_REASON,
            "rejectionReason.in=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRejectionReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where rejectionReason is not null
        defaultPurgeRequestFiltering("rejectionReason.specified=true", "rejectionReason.specified=false");
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRejectionReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where rejectionReason contains
        defaultPurgeRequestFiltering(
            "rejectionReason.contains=" + DEFAULT_REJECTION_REASON,
            "rejectionReason.contains=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByRejectionReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where rejectionReason does not contain
        defaultPurgeRequestFiltering(
            "rejectionReason.doesNotContain=" + UPDATED_REJECTION_REASON,
            "rejectionReason.doesNotContain=" + DEFAULT_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByExecutedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where executedAt equals to
        defaultPurgeRequestFiltering("executedAt.equals=" + DEFAULT_EXECUTED_AT, "executedAt.equals=" + UPDATED_EXECUTED_AT);
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByExecutedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where executedAt in
        defaultPurgeRequestFiltering(
            "executedAt.in=" + DEFAULT_EXECUTED_AT + "," + UPDATED_EXECUTED_AT,
            "executedAt.in=" + UPDATED_EXECUTED_AT
        );
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByExecutedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        // Get all the purgeRequestList where executedAt is not null
        defaultPurgeRequestFiltering("executedAt.specified=true", "executedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllPurgeRequestsByDocumentIsEqualToSomething() throws Exception {
        Document document;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            purgeRequestRepository.saveAndFlush(purgeRequest);
            document = DocumentResourceIT.createEntity(em);
        } else {
            document = TestUtil.findAll(em, Document.class).get(0);
        }
        em.persist(document);
        em.flush();
        purgeRequest.setDocument(document);
        purgeRequestRepository.saveAndFlush(purgeRequest);
        UUID documentId = document.getId();
        // Get all the purgeRequestList where document equals to documentId
        defaultPurgeRequestShouldBeFound("documentId.equals=" + documentId);

        // Get all the purgeRequestList where document equals to UUID.randomUUID()
        defaultPurgeRequestShouldNotBeFound("documentId.equals=" + UUID.randomUUID());
    }

    private void defaultPurgeRequestFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultPurgeRequestShouldBeFound(shouldBeFound);
        defaultPurgeRequestShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultPurgeRequestShouldBeFound(String filter) throws Exception {
        restPurgeRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(purgeRequest.getId().toString())))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].approvedAt").value(hasItem(DEFAULT_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].rejectionReason").value(hasItem(DEFAULT_REJECTION_REASON)))
            .andExpect(jsonPath("$.[*].executedAt").value(hasItem(DEFAULT_EXECUTED_AT.toString())));

        // Check, that the count call also returns 1
        restPurgeRequestMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultPurgeRequestShouldNotBeFound(String filter) throws Exception {
        restPurgeRequestMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restPurgeRequestMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingPurgeRequest() throws Exception {
        // Get the purgeRequest
        restPurgeRequestMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingPurgeRequest() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the purgeRequest
        PurgeRequest updatedPurgeRequest = purgeRequestRepository.findById(purgeRequest.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedPurgeRequest are not directly saved in db
        em.detach(updatedPurgeRequest);
        updatedPurgeRequest
            .requesterId(UPDATED_REQUESTER_ID)
            .reason(UPDATED_REASON)
            .requestedAt(UPDATED_REQUESTED_AT)
            .approverId(UPDATED_APPROVER_ID)
            .approvedAt(UPDATED_APPROVED_AT)
            .state(UPDATED_STATE)
            .rejectionReason(UPDATED_REJECTION_REASON)
            .executedAt(UPDATED_EXECUTED_AT);
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(updatedPurgeRequest);

        restPurgeRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, purgeRequestDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isOk());

        // Validate the PurgeRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPurgeRequestToMatchAllProperties(updatedPurgeRequest);
    }

    @Test
    @Transactional
    void putNonExistingPurgeRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        purgeRequest.setId(UUID.randomUUID());

        // Create the PurgeRequest
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPurgeRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, purgeRequestDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PurgeRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchPurgeRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        purgeRequest.setId(UUID.randomUUID());

        // Create the PurgeRequest
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPurgeRequestMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PurgeRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamPurgeRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        purgeRequest.setId(UUID.randomUUID());

        // Create the PurgeRequest
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPurgeRequestMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the PurgeRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdatePurgeRequestWithPatch() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the purgeRequest using partial update
        PurgeRequest partialUpdatedPurgeRequest = new PurgeRequest();
        partialUpdatedPurgeRequest.setId(purgeRequest.getId());

        partialUpdatedPurgeRequest.requesterId(UPDATED_REQUESTER_ID).approvedAt(UPDATED_APPROVED_AT).executedAt(UPDATED_EXECUTED_AT);

        restPurgeRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPurgeRequest.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPurgeRequest))
            )
            .andExpect(status().isOk());

        // Validate the PurgeRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPurgeRequestUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPurgeRequest, purgeRequest),
            getPersistedPurgeRequest(purgeRequest)
        );
    }

    @Test
    @Transactional
    void fullUpdatePurgeRequestWithPatch() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the purgeRequest using partial update
        PurgeRequest partialUpdatedPurgeRequest = new PurgeRequest();
        partialUpdatedPurgeRequest.setId(purgeRequest.getId());

        partialUpdatedPurgeRequest
            .requesterId(UPDATED_REQUESTER_ID)
            .reason(UPDATED_REASON)
            .requestedAt(UPDATED_REQUESTED_AT)
            .approverId(UPDATED_APPROVER_ID)
            .approvedAt(UPDATED_APPROVED_AT)
            .state(UPDATED_STATE)
            .rejectionReason(UPDATED_REJECTION_REASON)
            .executedAt(UPDATED_EXECUTED_AT);

        restPurgeRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedPurgeRequest.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedPurgeRequest))
            )
            .andExpect(status().isOk());

        // Validate the PurgeRequest in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPurgeRequestUpdatableFieldsEquals(partialUpdatedPurgeRequest, getPersistedPurgeRequest(partialUpdatedPurgeRequest));
    }

    @Test
    @Transactional
    void patchNonExistingPurgeRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        purgeRequest.setId(UUID.randomUUID());

        // Create the PurgeRequest
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPurgeRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, purgeRequestDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PurgeRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchPurgeRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        purgeRequest.setId(UUID.randomUUID());

        // Create the PurgeRequest
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPurgeRequestMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the PurgeRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamPurgeRequest() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        purgeRequest.setId(UUID.randomUUID());

        // Create the PurgeRequest
        PurgeRequestDTO purgeRequestDTO = purgeRequestMapper.toDto(purgeRequest);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restPurgeRequestMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(purgeRequestDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the PurgeRequest in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deletePurgeRequest() throws Exception {
        // Initialize the database
        insertedPurgeRequest = purgeRequestRepository.saveAndFlush(purgeRequest);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the purgeRequest
        restPurgeRequestMockMvc
            .perform(delete(ENTITY_API_URL_ID, purgeRequest.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return purgeRequestRepository.count();
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

    protected PurgeRequest getPersistedPurgeRequest(PurgeRequest purgeRequest) {
        return purgeRequestRepository.findById(purgeRequest.getId()).orElseThrow();
    }

    protected void assertPersistedPurgeRequestToMatchAllProperties(PurgeRequest expectedPurgeRequest) {
        assertPurgeRequestAllPropertiesEquals(expectedPurgeRequest, getPersistedPurgeRequest(expectedPurgeRequest));
    }

    protected void assertPersistedPurgeRequestToMatchUpdatableProperties(PurgeRequest expectedPurgeRequest) {
        assertPurgeRequestAllUpdatablePropertiesEquals(expectedPurgeRequest, getPersistedPurgeRequest(expectedPurgeRequest));
    }
}
