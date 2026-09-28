package cm.gimac.des.participant.web.rest;

import static cm.gimac.des.participant.domain.StatusTransitionAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.participant.IntegrationTest;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import cm.gimac.des.participant.service.StatusTransitionService;
import cm.gimac.des.participant.service.mapper.StatusTransitionMapper;
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
 * Integration tests for the {@link StatusTransitionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class StatusTransitionResourceIT {

    private static final InstitutionStatus DEFAULT_FROM_STATUS = InstitutionStatus.CANDIDAT;
    private static final InstitutionStatus UPDATED_FROM_STATUS = InstitutionStatus.EN_HOMOLOGATION;

    private static final InstitutionStatus DEFAULT_TO_STATUS = InstitutionStatus.CANDIDAT;
    private static final InstitutionStatus UPDATED_TO_STATUS = InstitutionStatus.EN_HOMOLOGATION;

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final UUID DEFAULT_REQUESTER_ID = UUID.randomUUID();
    private static final UUID UPDATED_REQUESTER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_REQUESTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REQUESTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_AUTOMATIC_TRANSITION = false;
    private static final Boolean UPDATED_AUTOMATIC_TRANSITION = true;

    private static final Integer DEFAULT_REQUIRED_APPROVALS = 1;
    private static final Integer UPDATED_REQUIRED_APPROVALS = 2;
    private static final Integer SMALLER_REQUIRED_APPROVALS = 1 - 1;

    private static final UUID DEFAULT_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROVER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_SECOND_APPROVER_ID = UUID.randomUUID();
    private static final UUID UPDATED_SECOND_APPROVER_ID = UUID.randomUUID();

    private static final Instant DEFAULT_SECOND_APPROVED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SECOND_APPROVED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_DECIDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DECIDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final TransitionState DEFAULT_STATE = TransitionState.PROPOSEE;
    private static final TransitionState UPDATED_STATE = TransitionState.APPROUVEE;

    private static final String DEFAULT_REJECTION_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REJECTION_REASON = "BBBBBBBBBB";

    private static final UUID DEFAULT_EVIDENCE_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_EVIDENCE_DOCUMENT_ID = UUID.randomUUID();

    private static final String ENTITY_API_URL = "/api/status-transitions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private StatusTransitionRepository statusTransitionRepository;

    @Mock
    private StatusTransitionRepository statusTransitionRepositoryMock;

    @Autowired
    private StatusTransitionMapper statusTransitionMapper;

    @Mock
    private StatusTransitionService statusTransitionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStatusTransitionMockMvc;

    private StatusTransition statusTransition;

    private StatusTransition insertedStatusTransition;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StatusTransition createEntity(EntityManager em) {
        StatusTransition statusTransition = new StatusTransition()
            .fromStatus(DEFAULT_FROM_STATUS)
            .toStatus(DEFAULT_TO_STATUS)
            .reason(DEFAULT_REASON)
            .requesterId(DEFAULT_REQUESTER_ID)
            .requestedAt(DEFAULT_REQUESTED_AT)
            .automaticTransition(DEFAULT_AUTOMATIC_TRANSITION)
            .requiredApprovals(DEFAULT_REQUIRED_APPROVALS)
            .approverId(DEFAULT_APPROVER_ID)
            .secondApproverId(DEFAULT_SECOND_APPROVER_ID)
            .secondApprovedAt(DEFAULT_SECOND_APPROVED_AT)
            .decidedAt(DEFAULT_DECIDED_AT)
            .state(DEFAULT_STATE)
            .rejectionReason(DEFAULT_REJECTION_REASON)
            .evidenceDocumentId(DEFAULT_EVIDENCE_DOCUMENT_ID);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        statusTransition.setInstitution(institution);
        return statusTransition;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StatusTransition createUpdatedEntity(EntityManager em) {
        StatusTransition updatedStatusTransition = new StatusTransition()
            .fromStatus(UPDATED_FROM_STATUS)
            .toStatus(UPDATED_TO_STATUS)
            .reason(UPDATED_REASON)
            .requesterId(UPDATED_REQUESTER_ID)
            .requestedAt(UPDATED_REQUESTED_AT)
            .automaticTransition(UPDATED_AUTOMATIC_TRANSITION)
            .requiredApprovals(UPDATED_REQUIRED_APPROVALS)
            .approverId(UPDATED_APPROVER_ID)
            .secondApproverId(UPDATED_SECOND_APPROVER_ID)
            .secondApprovedAt(UPDATED_SECOND_APPROVED_AT)
            .decidedAt(UPDATED_DECIDED_AT)
            .state(UPDATED_STATE)
            .rejectionReason(UPDATED_REJECTION_REASON)
            .evidenceDocumentId(UPDATED_EVIDENCE_DOCUMENT_ID);
        // Add required entity
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            institution = InstitutionResourceIT.createUpdatedEntity();
            em.persist(institution);
            em.flush();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        updatedStatusTransition.setInstitution(institution);
        return updatedStatusTransition;
    }

    @BeforeEach
    void initTest() {
        statusTransition = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedStatusTransition != null) {
            statusTransitionRepository.delete(insertedStatusTransition);
            insertedStatusTransition = null;
        }
    }

    @Test
    @Transactional
    void getAllStatusTransitions() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList
        restStatusTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(statusTransition.getId().toString())))
            .andExpect(jsonPath("$.[*].fromStatus").value(hasItem(DEFAULT_FROM_STATUS.toString())))
            .andExpect(jsonPath("$.[*].toStatus").value(hasItem(DEFAULT_TO_STATUS.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].automaticTransition").value(hasItem(DEFAULT_AUTOMATIC_TRANSITION)))
            .andExpect(jsonPath("$.[*].requiredApprovals").value(hasItem(DEFAULT_REQUIRED_APPROVALS)))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].secondApproverId").value(hasItem(DEFAULT_SECOND_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].secondApprovedAt").value(hasItem(DEFAULT_SECOND_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].decidedAt").value(hasItem(DEFAULT_DECIDED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].rejectionReason").value(hasItem(DEFAULT_REJECTION_REASON)))
            .andExpect(jsonPath("$.[*].evidenceDocumentId").value(hasItem(DEFAULT_EVIDENCE_DOCUMENT_ID.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllStatusTransitionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(statusTransitionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restStatusTransitionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(statusTransitionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllStatusTransitionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(statusTransitionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restStatusTransitionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(statusTransitionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getStatusTransition() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get the statusTransition
        restStatusTransitionMockMvc
            .perform(get(ENTITY_API_URL_ID, statusTransition.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(statusTransition.getId().toString()))
            .andExpect(jsonPath("$.fromStatus").value(DEFAULT_FROM_STATUS.toString()))
            .andExpect(jsonPath("$.toStatus").value(DEFAULT_TO_STATUS.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.requesterId").value(DEFAULT_REQUESTER_ID.toString()))
            .andExpect(jsonPath("$.requestedAt").value(DEFAULT_REQUESTED_AT.toString()))
            .andExpect(jsonPath("$.automaticTransition").value(DEFAULT_AUTOMATIC_TRANSITION))
            .andExpect(jsonPath("$.requiredApprovals").value(DEFAULT_REQUIRED_APPROVALS))
            .andExpect(jsonPath("$.approverId").value(DEFAULT_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.secondApproverId").value(DEFAULT_SECOND_APPROVER_ID.toString()))
            .andExpect(jsonPath("$.secondApprovedAt").value(DEFAULT_SECOND_APPROVED_AT.toString()))
            .andExpect(jsonPath("$.decidedAt").value(DEFAULT_DECIDED_AT.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.rejectionReason").value(DEFAULT_REJECTION_REASON))
            .andExpect(jsonPath("$.evidenceDocumentId").value(DEFAULT_EVIDENCE_DOCUMENT_ID.toString()));
    }

    @Test
    @Transactional
    void getStatusTransitionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        UUID id = statusTransition.getId();

        defaultStatusTransitionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByFromStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where fromStatus equals to
        defaultStatusTransitionFiltering("fromStatus.equals=" + DEFAULT_FROM_STATUS, "fromStatus.equals=" + UPDATED_FROM_STATUS);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByFromStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where fromStatus in
        defaultStatusTransitionFiltering(
            "fromStatus.in=" + DEFAULT_FROM_STATUS + "," + UPDATED_FROM_STATUS,
            "fromStatus.in=" + UPDATED_FROM_STATUS
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByFromStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where fromStatus is not null
        defaultStatusTransitionFiltering("fromStatus.specified=true", "fromStatus.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByToStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where toStatus equals to
        defaultStatusTransitionFiltering("toStatus.equals=" + DEFAULT_TO_STATUS, "toStatus.equals=" + UPDATED_TO_STATUS);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByToStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where toStatus in
        defaultStatusTransitionFiltering("toStatus.in=" + DEFAULT_TO_STATUS + "," + UPDATED_TO_STATUS, "toStatus.in=" + UPDATED_TO_STATUS);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByToStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where toStatus is not null
        defaultStatusTransitionFiltering("toStatus.specified=true", "toStatus.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where reason equals to
        defaultStatusTransitionFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where reason in
        defaultStatusTransitionFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where reason is not null
        defaultStatusTransitionFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where reason contains
        defaultStatusTransitionFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where reason does not contain
        defaultStatusTransitionFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequesterIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requesterId equals to
        defaultStatusTransitionFiltering("requesterId.equals=" + DEFAULT_REQUESTER_ID, "requesterId.equals=" + UPDATED_REQUESTER_ID);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequesterIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requesterId in
        defaultStatusTransitionFiltering(
            "requesterId.in=" + DEFAULT_REQUESTER_ID + "," + UPDATED_REQUESTER_ID,
            "requesterId.in=" + UPDATED_REQUESTER_ID
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequesterIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requesterId is not null
        defaultStatusTransitionFiltering("requesterId.specified=true", "requesterId.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequestedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requestedAt equals to
        defaultStatusTransitionFiltering("requestedAt.equals=" + DEFAULT_REQUESTED_AT, "requestedAt.equals=" + UPDATED_REQUESTED_AT);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequestedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requestedAt in
        defaultStatusTransitionFiltering(
            "requestedAt.in=" + DEFAULT_REQUESTED_AT + "," + UPDATED_REQUESTED_AT,
            "requestedAt.in=" + UPDATED_REQUESTED_AT
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequestedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requestedAt is not null
        defaultStatusTransitionFiltering("requestedAt.specified=true", "requestedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByAutomaticTransitionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where automaticTransition equals to
        defaultStatusTransitionFiltering(
            "automaticTransition.equals=" + DEFAULT_AUTOMATIC_TRANSITION,
            "automaticTransition.equals=" + UPDATED_AUTOMATIC_TRANSITION
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByAutomaticTransitionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where automaticTransition in
        defaultStatusTransitionFiltering(
            "automaticTransition.in=" + DEFAULT_AUTOMATIC_TRANSITION + "," + UPDATED_AUTOMATIC_TRANSITION,
            "automaticTransition.in=" + UPDATED_AUTOMATIC_TRANSITION
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByAutomaticTransitionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where automaticTransition is not null
        defaultStatusTransitionFiltering("automaticTransition.specified=true", "automaticTransition.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequiredApprovalsIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requiredApprovals equals to
        defaultStatusTransitionFiltering(
            "requiredApprovals.equals=" + DEFAULT_REQUIRED_APPROVALS,
            "requiredApprovals.equals=" + UPDATED_REQUIRED_APPROVALS
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequiredApprovalsIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requiredApprovals in
        defaultStatusTransitionFiltering(
            "requiredApprovals.in=" + DEFAULT_REQUIRED_APPROVALS + "," + UPDATED_REQUIRED_APPROVALS,
            "requiredApprovals.in=" + UPDATED_REQUIRED_APPROVALS
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequiredApprovalsIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requiredApprovals is not null
        defaultStatusTransitionFiltering("requiredApprovals.specified=true", "requiredApprovals.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequiredApprovalsIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requiredApprovals is greater than or equal to
        defaultStatusTransitionFiltering(
            "requiredApprovals.greaterThanOrEqual=" + DEFAULT_REQUIRED_APPROVALS,
            "requiredApprovals.greaterThanOrEqual=" + (DEFAULT_REQUIRED_APPROVALS + 1)
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequiredApprovalsIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requiredApprovals is less than or equal to
        defaultStatusTransitionFiltering(
            "requiredApprovals.lessThanOrEqual=" + DEFAULT_REQUIRED_APPROVALS,
            "requiredApprovals.lessThanOrEqual=" + SMALLER_REQUIRED_APPROVALS
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequiredApprovalsIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requiredApprovals is less than
        defaultStatusTransitionFiltering(
            "requiredApprovals.lessThan=" + (DEFAULT_REQUIRED_APPROVALS + 1),
            "requiredApprovals.lessThan=" + DEFAULT_REQUIRED_APPROVALS
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRequiredApprovalsIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where requiredApprovals is greater than
        defaultStatusTransitionFiltering(
            "requiredApprovals.greaterThan=" + SMALLER_REQUIRED_APPROVALS,
            "requiredApprovals.greaterThan=" + DEFAULT_REQUIRED_APPROVALS
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByApproverIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where approverId equals to
        defaultStatusTransitionFiltering("approverId.equals=" + DEFAULT_APPROVER_ID, "approverId.equals=" + UPDATED_APPROVER_ID);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByApproverIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where approverId in
        defaultStatusTransitionFiltering(
            "approverId.in=" + DEFAULT_APPROVER_ID + "," + UPDATED_APPROVER_ID,
            "approverId.in=" + UPDATED_APPROVER_ID
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByApproverIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where approverId is not null
        defaultStatusTransitionFiltering("approverId.specified=true", "approverId.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsBySecondApproverIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where secondApproverId equals to
        defaultStatusTransitionFiltering(
            "secondApproverId.equals=" + DEFAULT_SECOND_APPROVER_ID,
            "secondApproverId.equals=" + UPDATED_SECOND_APPROVER_ID
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsBySecondApproverIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where secondApproverId in
        defaultStatusTransitionFiltering(
            "secondApproverId.in=" + DEFAULT_SECOND_APPROVER_ID + "," + UPDATED_SECOND_APPROVER_ID,
            "secondApproverId.in=" + UPDATED_SECOND_APPROVER_ID
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsBySecondApproverIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where secondApproverId is not null
        defaultStatusTransitionFiltering("secondApproverId.specified=true", "secondApproverId.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsBySecondApprovedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where secondApprovedAt equals to
        defaultStatusTransitionFiltering(
            "secondApprovedAt.equals=" + DEFAULT_SECOND_APPROVED_AT,
            "secondApprovedAt.equals=" + UPDATED_SECOND_APPROVED_AT
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsBySecondApprovedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where secondApprovedAt in
        defaultStatusTransitionFiltering(
            "secondApprovedAt.in=" + DEFAULT_SECOND_APPROVED_AT + "," + UPDATED_SECOND_APPROVED_AT,
            "secondApprovedAt.in=" + UPDATED_SECOND_APPROVED_AT
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsBySecondApprovedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where secondApprovedAt is not null
        defaultStatusTransitionFiltering("secondApprovedAt.specified=true", "secondApprovedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByDecidedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where decidedAt equals to
        defaultStatusTransitionFiltering("decidedAt.equals=" + DEFAULT_DECIDED_AT, "decidedAt.equals=" + UPDATED_DECIDED_AT);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByDecidedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where decidedAt in
        defaultStatusTransitionFiltering(
            "decidedAt.in=" + DEFAULT_DECIDED_AT + "," + UPDATED_DECIDED_AT,
            "decidedAt.in=" + UPDATED_DECIDED_AT
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByDecidedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where decidedAt is not null
        defaultStatusTransitionFiltering("decidedAt.specified=true", "decidedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where state equals to
        defaultStatusTransitionFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where state in
        defaultStatusTransitionFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where state is not null
        defaultStatusTransitionFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRejectionReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where rejectionReason equals to
        defaultStatusTransitionFiltering(
            "rejectionReason.equals=" + DEFAULT_REJECTION_REASON,
            "rejectionReason.equals=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRejectionReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where rejectionReason in
        defaultStatusTransitionFiltering(
            "rejectionReason.in=" + DEFAULT_REJECTION_REASON + "," + UPDATED_REJECTION_REASON,
            "rejectionReason.in=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRejectionReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where rejectionReason is not null
        defaultStatusTransitionFiltering("rejectionReason.specified=true", "rejectionReason.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRejectionReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where rejectionReason contains
        defaultStatusTransitionFiltering(
            "rejectionReason.contains=" + DEFAULT_REJECTION_REASON,
            "rejectionReason.contains=" + UPDATED_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByRejectionReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where rejectionReason does not contain
        defaultStatusTransitionFiltering(
            "rejectionReason.doesNotContain=" + UPDATED_REJECTION_REASON,
            "rejectionReason.doesNotContain=" + DEFAULT_REJECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByEvidenceDocumentIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where evidenceDocumentId equals to
        defaultStatusTransitionFiltering(
            "evidenceDocumentId.equals=" + DEFAULT_EVIDENCE_DOCUMENT_ID,
            "evidenceDocumentId.equals=" + UPDATED_EVIDENCE_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByEvidenceDocumentIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where evidenceDocumentId in
        defaultStatusTransitionFiltering(
            "evidenceDocumentId.in=" + DEFAULT_EVIDENCE_DOCUMENT_ID + "," + UPDATED_EVIDENCE_DOCUMENT_ID,
            "evidenceDocumentId.in=" + UPDATED_EVIDENCE_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByEvidenceDocumentIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedStatusTransition = statusTransitionRepository.saveAndFlush(statusTransition);

        // Get all the statusTransitionList where evidenceDocumentId is not null
        defaultStatusTransitionFiltering("evidenceDocumentId.specified=true", "evidenceDocumentId.specified=false");
    }

    @Test
    @Transactional
    void getAllStatusTransitionsByInstitutionIsEqualToSomething() throws Exception {
        Institution institution;
        if (TestUtil.findAll(em, Institution.class).isEmpty()) {
            statusTransitionRepository.saveAndFlush(statusTransition);
            institution = InstitutionResourceIT.createEntity();
        } else {
            institution = TestUtil.findAll(em, Institution.class).get(0);
        }
        em.persist(institution);
        em.flush();
        statusTransition.setInstitution(institution);
        statusTransitionRepository.saveAndFlush(statusTransition);
        UUID institutionId = institution.getId();
        // Get all the statusTransitionList where institution equals to institutionId
        defaultStatusTransitionShouldBeFound("institutionId.equals=" + institutionId);

        // Get all the statusTransitionList where institution equals to UUID.randomUUID()
        defaultStatusTransitionShouldNotBeFound("institutionId.equals=" + UUID.randomUUID());
    }

    private void defaultStatusTransitionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultStatusTransitionShouldBeFound(shouldBeFound);
        defaultStatusTransitionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultStatusTransitionShouldBeFound(String filter) throws Exception {
        restStatusTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(statusTransition.getId().toString())))
            .andExpect(jsonPath("$.[*].fromStatus").value(hasItem(DEFAULT_FROM_STATUS.toString())))
            .andExpect(jsonPath("$.[*].toStatus").value(hasItem(DEFAULT_TO_STATUS.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].requesterId").value(hasItem(DEFAULT_REQUESTER_ID.toString())))
            .andExpect(jsonPath("$.[*].requestedAt").value(hasItem(DEFAULT_REQUESTED_AT.toString())))
            .andExpect(jsonPath("$.[*].automaticTransition").value(hasItem(DEFAULT_AUTOMATIC_TRANSITION)))
            .andExpect(jsonPath("$.[*].requiredApprovals").value(hasItem(DEFAULT_REQUIRED_APPROVALS)))
            .andExpect(jsonPath("$.[*].approverId").value(hasItem(DEFAULT_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].secondApproverId").value(hasItem(DEFAULT_SECOND_APPROVER_ID.toString())))
            .andExpect(jsonPath("$.[*].secondApprovedAt").value(hasItem(DEFAULT_SECOND_APPROVED_AT.toString())))
            .andExpect(jsonPath("$.[*].decidedAt").value(hasItem(DEFAULT_DECIDED_AT.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].rejectionReason").value(hasItem(DEFAULT_REJECTION_REASON)))
            .andExpect(jsonPath("$.[*].evidenceDocumentId").value(hasItem(DEFAULT_EVIDENCE_DOCUMENT_ID.toString())));

        // Check, that the count call also returns 1
        restStatusTransitionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultStatusTransitionShouldNotBeFound(String filter) throws Exception {
        restStatusTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restStatusTransitionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingStatusTransition() throws Exception {
        // Get the statusTransition
        restStatusTransitionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return statusTransitionRepository.count();
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

    protected StatusTransition getPersistedStatusTransition(StatusTransition statusTransition) {
        return statusTransitionRepository.findById(statusTransition.getId()).orElseThrow();
    }

    protected void assertPersistedStatusTransitionToMatchAllProperties(StatusTransition expectedStatusTransition) {
        assertStatusTransitionAllPropertiesEquals(expectedStatusTransition, getPersistedStatusTransition(expectedStatusTransition));
    }

    protected void assertPersistedStatusTransitionToMatchUpdatableProperties(StatusTransition expectedStatusTransition) {
        assertStatusTransitionAllUpdatablePropertiesEquals(
            expectedStatusTransition,
            getPersistedStatusTransition(expectedStatusTransition)
        );
    }
}
