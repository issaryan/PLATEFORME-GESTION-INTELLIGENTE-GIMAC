package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.SuspensAsserts.*;
import static cm.gimac.des.rapprochement.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.Reconciliation;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.domain.enumeration.DiscrepancyType;
import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import cm.gimac.des.rapprochement.repository.SuspensRepository;
import cm.gimac.des.rapprochement.service.SuspensService;
import cm.gimac.des.rapprochement.service.mapper.SuspensMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * Integration tests for the {@link SuspensResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class SuspensResourceIT {

    private static final String DEFAULT_REFERENCE = "SUS-0714-8202-906549";
    private static final String UPDATED_REFERENCE = "SUS-2177-6140-668";

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final DiscrepancyType DEFAULT_TYPE = DiscrepancyType.ABSENT_COMMUTATEUR;
    private static final DiscrepancyType UPDATED_TYPE = DiscrepancyType.ABSENT_PARTICIPANT;

    private static final String DEFAULT_PIVOT_TRANSACTION_REF = "AAAAAAAAAA";
    private static final String UPDATED_PIVOT_TRANSACTION_REF = "BBBBBBBBBB";

    private static final String DEFAULT_PIVOT_AUTHORIZATION_REF = "AAAAAAAAAA";
    private static final String UPDATED_PIVOT_AUTHORIZATION_REF = "BBBBBBBBBB";

    private static final Instant DEFAULT_PIVOT_OPERATION_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PIVOT_OPERATION_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final BigDecimal DEFAULT_PIVOT_AMOUNT = new BigDecimal(1);
    private static final BigDecimal UPDATED_PIVOT_AMOUNT = new BigDecimal(2);
    private static final BigDecimal SMALLER_PIVOT_AMOUNT = new BigDecimal(1 - 1);

    private static final String DEFAULT_PIVOT_PARTICIPANT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_PIVOT_PARTICIPANT_CODE = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_AMOUNT_SWITCH = new BigDecimal(1);
    private static final BigDecimal UPDATED_AMOUNT_SWITCH = new BigDecimal(2);
    private static final BigDecimal SMALLER_AMOUNT_SWITCH = new BigDecimal(1 - 1);

    private static final BigDecimal DEFAULT_AMOUNT_PARTICIPANT = new BigDecimal(1);
    private static final BigDecimal UPDATED_AMOUNT_PARTICIPANT = new BigDecimal(2);
    private static final BigDecimal SMALLER_AMOUNT_PARTICIPANT = new BigDecimal(1 - 1);

    private static final SuspensState DEFAULT_STATE = SuspensState.OUVERT;
    private static final SuspensState UPDATED_STATE = SuspensState.AFFECTE;

    private static final UUID DEFAULT_ASSIGNEE_ID = UUID.randomUUID();
    private static final UUID UPDATED_ASSIGNEE_ID = UUID.randomUUID();

    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_DUE_DATE = LocalDate.ofEpochDay(-1L);

    private static final Instant DEFAULT_OPENED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OPENED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_DISPUTE_TICKET_ID = UUID.randomUUID();
    private static final UUID UPDATED_DISPUTE_TICKET_ID = UUID.randomUUID();

    private static final Instant DEFAULT_CLOSED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CLOSED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/suspens";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SuspensRepository suspensRepository;

    @Mock
    private SuspensRepository suspensRepositoryMock;

    @Autowired
    private SuspensMapper suspensMapper;

    @Mock
    private SuspensService suspensServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSuspensMockMvc;

    private Suspens suspens;

    private Suspens insertedSuspens;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Suspens createEntity(EntityManager em) {
        Suspens suspens = new Suspens()
            .reference(DEFAULT_REFERENCE)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .type(DEFAULT_TYPE)
            .pivotTransactionRef(DEFAULT_PIVOT_TRANSACTION_REF)
            .pivotAuthorizationRef(DEFAULT_PIVOT_AUTHORIZATION_REF)
            .pivotOperationAt(DEFAULT_PIVOT_OPERATION_AT)
            .pivotAmount(DEFAULT_PIVOT_AMOUNT)
            .pivotParticipantCode(DEFAULT_PIVOT_PARTICIPANT_CODE)
            .amountSwitch(DEFAULT_AMOUNT_SWITCH)
            .amountParticipant(DEFAULT_AMOUNT_PARTICIPANT)
            .state(DEFAULT_STATE)
            .assigneeId(DEFAULT_ASSIGNEE_ID)
            .dueDate(DEFAULT_DUE_DATE)
            .openedAt(DEFAULT_OPENED_AT)
            .disputeTicketId(DEFAULT_DISPUTE_TICKET_ID)
            .closedAt(DEFAULT_CLOSED_AT);
        // Add required entity
        Reconciliation reconciliation;
        if (TestUtil.findAll(em, Reconciliation.class).isEmpty()) {
            reconciliation = ReconciliationResourceIT.createEntity();
            em.persist(reconciliation);
            em.flush();
        } else {
            reconciliation = TestUtil.findAll(em, Reconciliation.class).get(0);
        }
        suspens.setReconciliation(reconciliation);
        return suspens;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Suspens createUpdatedEntity(EntityManager em) {
        Suspens updatedSuspens = new Suspens()
            .reference(UPDATED_REFERENCE)
            .institutionId(UPDATED_INSTITUTION_ID)
            .type(UPDATED_TYPE)
            .pivotTransactionRef(UPDATED_PIVOT_TRANSACTION_REF)
            .pivotAuthorizationRef(UPDATED_PIVOT_AUTHORIZATION_REF)
            .pivotOperationAt(UPDATED_PIVOT_OPERATION_AT)
            .pivotAmount(UPDATED_PIVOT_AMOUNT)
            .pivotParticipantCode(UPDATED_PIVOT_PARTICIPANT_CODE)
            .amountSwitch(UPDATED_AMOUNT_SWITCH)
            .amountParticipant(UPDATED_AMOUNT_PARTICIPANT)
            .state(UPDATED_STATE)
            .assigneeId(UPDATED_ASSIGNEE_ID)
            .dueDate(UPDATED_DUE_DATE)
            .openedAt(UPDATED_OPENED_AT)
            .disputeTicketId(UPDATED_DISPUTE_TICKET_ID)
            .closedAt(UPDATED_CLOSED_AT);
        // Add required entity
        Reconciliation reconciliation;
        if (TestUtil.findAll(em, Reconciliation.class).isEmpty()) {
            reconciliation = ReconciliationResourceIT.createUpdatedEntity();
            em.persist(reconciliation);
            em.flush();
        } else {
            reconciliation = TestUtil.findAll(em, Reconciliation.class).get(0);
        }
        updatedSuspens.setReconciliation(reconciliation);
        return updatedSuspens;
    }

    @BeforeEach
    void initTest() {
        suspens = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSuspens != null) {
            suspensRepository.delete(insertedSuspens);
            insertedSuspens = null;
        }
    }

    @Test
    @Transactional
    void getAllSuspens() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList
        restSuspensMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(suspens.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].pivotTransactionRef").value(hasItem(DEFAULT_PIVOT_TRANSACTION_REF)))
            .andExpect(jsonPath("$.[*].pivotAuthorizationRef").value(hasItem(DEFAULT_PIVOT_AUTHORIZATION_REF)))
            .andExpect(jsonPath("$.[*].pivotOperationAt").value(hasItem(DEFAULT_PIVOT_OPERATION_AT.toString())))
            .andExpect(jsonPath("$.[*].pivotAmount").value(hasItem(sameNumber(DEFAULT_PIVOT_AMOUNT))))
            .andExpect(jsonPath("$.[*].pivotParticipantCode").value(hasItem(DEFAULT_PIVOT_PARTICIPANT_CODE)))
            .andExpect(jsonPath("$.[*].amountSwitch").value(hasItem(sameNumber(DEFAULT_AMOUNT_SWITCH))))
            .andExpect(jsonPath("$.[*].amountParticipant").value(hasItem(sameNumber(DEFAULT_AMOUNT_PARTICIPANT))))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].assigneeId").value(hasItem(DEFAULT_ASSIGNEE_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].openedAt").value(hasItem(DEFAULT_OPENED_AT.toString())))
            .andExpect(jsonPath("$.[*].disputeTicketId").value(hasItem(DEFAULT_DISPUTE_TICKET_ID.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSuspensWithEagerRelationshipsIsEnabled() throws Exception {
        when(suspensServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSuspensMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(suspensServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSuspensWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(suspensServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSuspensMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(suspensRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSuspens() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get the suspens
        restSuspensMockMvc
            .perform(get(ENTITY_API_URL_ID, suspens.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(suspens.getId().toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.pivotTransactionRef").value(DEFAULT_PIVOT_TRANSACTION_REF))
            .andExpect(jsonPath("$.pivotAuthorizationRef").value(DEFAULT_PIVOT_AUTHORIZATION_REF))
            .andExpect(jsonPath("$.pivotOperationAt").value(DEFAULT_PIVOT_OPERATION_AT.toString()))
            .andExpect(jsonPath("$.pivotAmount").value(sameNumber(DEFAULT_PIVOT_AMOUNT)))
            .andExpect(jsonPath("$.pivotParticipantCode").value(DEFAULT_PIVOT_PARTICIPANT_CODE))
            .andExpect(jsonPath("$.amountSwitch").value(sameNumber(DEFAULT_AMOUNT_SWITCH)))
            .andExpect(jsonPath("$.amountParticipant").value(sameNumber(DEFAULT_AMOUNT_PARTICIPANT)))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.assigneeId").value(DEFAULT_ASSIGNEE_ID.toString()))
            .andExpect(jsonPath("$.dueDate").value(DEFAULT_DUE_DATE.toString()))
            .andExpect(jsonPath("$.openedAt").value(DEFAULT_OPENED_AT.toString()))
            .andExpect(jsonPath("$.disputeTicketId").value(DEFAULT_DISPUTE_TICKET_ID.toString()))
            .andExpect(jsonPath("$.closedAt").value(DEFAULT_CLOSED_AT.toString()));
    }

    @Test
    @Transactional
    void getSuspensByIdFiltering() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        UUID id = suspens.getId();

        defaultSuspensFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllSuspensByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where reference equals to
        defaultSuspensFiltering("reference.equals=" + DEFAULT_REFERENCE, "reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllSuspensByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where reference in
        defaultSuspensFiltering("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE, "reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllSuspensByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where reference is not null
        defaultSuspensFiltering("reference.specified=true", "reference.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByReferenceContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where reference contains
        defaultSuspensFiltering("reference.contains=" + DEFAULT_REFERENCE, "reference.contains=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    void getAllSuspensByReferenceNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where reference does not contain
        defaultSuspensFiltering("reference.doesNotContain=" + UPDATED_REFERENCE, "reference.doesNotContain=" + DEFAULT_REFERENCE);
    }

    @Test
    @Transactional
    void getAllSuspensByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where institutionId equals to
        defaultSuspensFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllSuspensByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where institutionId in
        defaultSuspensFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllSuspensByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where institutionId is not null
        defaultSuspensFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where type equals to
        defaultSuspensFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllSuspensByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where type in
        defaultSuspensFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllSuspensByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where type is not null
        defaultSuspensFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByPivotTransactionRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotTransactionRef equals to
        defaultSuspensFiltering(
            "pivotTransactionRef.equals=" + DEFAULT_PIVOT_TRANSACTION_REF,
            "pivotTransactionRef.equals=" + UPDATED_PIVOT_TRANSACTION_REF
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotTransactionRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotTransactionRef in
        defaultSuspensFiltering(
            "pivotTransactionRef.in=" + DEFAULT_PIVOT_TRANSACTION_REF + "," + UPDATED_PIVOT_TRANSACTION_REF,
            "pivotTransactionRef.in=" + UPDATED_PIVOT_TRANSACTION_REF
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotTransactionRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotTransactionRef is not null
        defaultSuspensFiltering("pivotTransactionRef.specified=true", "pivotTransactionRef.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByPivotTransactionRefContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotTransactionRef contains
        defaultSuspensFiltering(
            "pivotTransactionRef.contains=" + DEFAULT_PIVOT_TRANSACTION_REF,
            "pivotTransactionRef.contains=" + UPDATED_PIVOT_TRANSACTION_REF
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotTransactionRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotTransactionRef does not contain
        defaultSuspensFiltering(
            "pivotTransactionRef.doesNotContain=" + UPDATED_PIVOT_TRANSACTION_REF,
            "pivotTransactionRef.doesNotContain=" + DEFAULT_PIVOT_TRANSACTION_REF
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAuthorizationRefIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAuthorizationRef equals to
        defaultSuspensFiltering(
            "pivotAuthorizationRef.equals=" + DEFAULT_PIVOT_AUTHORIZATION_REF,
            "pivotAuthorizationRef.equals=" + UPDATED_PIVOT_AUTHORIZATION_REF
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAuthorizationRefIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAuthorizationRef in
        defaultSuspensFiltering(
            "pivotAuthorizationRef.in=" + DEFAULT_PIVOT_AUTHORIZATION_REF + "," + UPDATED_PIVOT_AUTHORIZATION_REF,
            "pivotAuthorizationRef.in=" + UPDATED_PIVOT_AUTHORIZATION_REF
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAuthorizationRefIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAuthorizationRef is not null
        defaultSuspensFiltering("pivotAuthorizationRef.specified=true", "pivotAuthorizationRef.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAuthorizationRefContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAuthorizationRef contains
        defaultSuspensFiltering(
            "pivotAuthorizationRef.contains=" + DEFAULT_PIVOT_AUTHORIZATION_REF,
            "pivotAuthorizationRef.contains=" + UPDATED_PIVOT_AUTHORIZATION_REF
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAuthorizationRefNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAuthorizationRef does not contain
        defaultSuspensFiltering(
            "pivotAuthorizationRef.doesNotContain=" + UPDATED_PIVOT_AUTHORIZATION_REF,
            "pivotAuthorizationRef.doesNotContain=" + DEFAULT_PIVOT_AUTHORIZATION_REF
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotOperationAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotOperationAt equals to
        defaultSuspensFiltering(
            "pivotOperationAt.equals=" + DEFAULT_PIVOT_OPERATION_AT,
            "pivotOperationAt.equals=" + UPDATED_PIVOT_OPERATION_AT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotOperationAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotOperationAt in
        defaultSuspensFiltering(
            "pivotOperationAt.in=" + DEFAULT_PIVOT_OPERATION_AT + "," + UPDATED_PIVOT_OPERATION_AT,
            "pivotOperationAt.in=" + UPDATED_PIVOT_OPERATION_AT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotOperationAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotOperationAt is not null
        defaultSuspensFiltering("pivotOperationAt.specified=true", "pivotOperationAt.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAmount equals to
        defaultSuspensFiltering("pivotAmount.equals=" + DEFAULT_PIVOT_AMOUNT, "pivotAmount.equals=" + UPDATED_PIVOT_AMOUNT);
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAmountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAmount in
        defaultSuspensFiltering(
            "pivotAmount.in=" + DEFAULT_PIVOT_AMOUNT + "," + UPDATED_PIVOT_AMOUNT,
            "pivotAmount.in=" + UPDATED_PIVOT_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAmount is not null
        defaultSuspensFiltering("pivotAmount.specified=true", "pivotAmount.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAmountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAmount is greater than or equal to
        defaultSuspensFiltering(
            "pivotAmount.greaterThanOrEqual=" + DEFAULT_PIVOT_AMOUNT,
            "pivotAmount.greaterThanOrEqual=" + UPDATED_PIVOT_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAmountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAmount is less than or equal to
        defaultSuspensFiltering(
            "pivotAmount.lessThanOrEqual=" + DEFAULT_PIVOT_AMOUNT,
            "pivotAmount.lessThanOrEqual=" + SMALLER_PIVOT_AMOUNT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAmountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAmount is less than
        defaultSuspensFiltering("pivotAmount.lessThan=" + UPDATED_PIVOT_AMOUNT, "pivotAmount.lessThan=" + DEFAULT_PIVOT_AMOUNT);
    }

    @Test
    @Transactional
    void getAllSuspensByPivotAmountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotAmount is greater than
        defaultSuspensFiltering("pivotAmount.greaterThan=" + SMALLER_PIVOT_AMOUNT, "pivotAmount.greaterThan=" + DEFAULT_PIVOT_AMOUNT);
    }

    @Test
    @Transactional
    void getAllSuspensByPivotParticipantCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotParticipantCode equals to
        defaultSuspensFiltering(
            "pivotParticipantCode.equals=" + DEFAULT_PIVOT_PARTICIPANT_CODE,
            "pivotParticipantCode.equals=" + UPDATED_PIVOT_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotParticipantCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotParticipantCode in
        defaultSuspensFiltering(
            "pivotParticipantCode.in=" + DEFAULT_PIVOT_PARTICIPANT_CODE + "," + UPDATED_PIVOT_PARTICIPANT_CODE,
            "pivotParticipantCode.in=" + UPDATED_PIVOT_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotParticipantCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotParticipantCode is not null
        defaultSuspensFiltering("pivotParticipantCode.specified=true", "pivotParticipantCode.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByPivotParticipantCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotParticipantCode contains
        defaultSuspensFiltering(
            "pivotParticipantCode.contains=" + DEFAULT_PIVOT_PARTICIPANT_CODE,
            "pivotParticipantCode.contains=" + UPDATED_PIVOT_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllSuspensByPivotParticipantCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where pivotParticipantCode does not contain
        defaultSuspensFiltering(
            "pivotParticipantCode.doesNotContain=" + UPDATED_PIVOT_PARTICIPANT_CODE,
            "pivotParticipantCode.doesNotContain=" + DEFAULT_PIVOT_PARTICIPANT_CODE
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountSwitchIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountSwitch equals to
        defaultSuspensFiltering("amountSwitch.equals=" + DEFAULT_AMOUNT_SWITCH, "amountSwitch.equals=" + UPDATED_AMOUNT_SWITCH);
    }

    @Test
    @Transactional
    void getAllSuspensByAmountSwitchIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountSwitch in
        defaultSuspensFiltering(
            "amountSwitch.in=" + DEFAULT_AMOUNT_SWITCH + "," + UPDATED_AMOUNT_SWITCH,
            "amountSwitch.in=" + UPDATED_AMOUNT_SWITCH
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountSwitchIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountSwitch is not null
        defaultSuspensFiltering("amountSwitch.specified=true", "amountSwitch.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByAmountSwitchIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountSwitch is greater than or equal to
        defaultSuspensFiltering(
            "amountSwitch.greaterThanOrEqual=" + DEFAULT_AMOUNT_SWITCH,
            "amountSwitch.greaterThanOrEqual=" + UPDATED_AMOUNT_SWITCH
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountSwitchIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountSwitch is less than or equal to
        defaultSuspensFiltering(
            "amountSwitch.lessThanOrEqual=" + DEFAULT_AMOUNT_SWITCH,
            "amountSwitch.lessThanOrEqual=" + SMALLER_AMOUNT_SWITCH
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountSwitchIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountSwitch is less than
        defaultSuspensFiltering("amountSwitch.lessThan=" + UPDATED_AMOUNT_SWITCH, "amountSwitch.lessThan=" + DEFAULT_AMOUNT_SWITCH);
    }

    @Test
    @Transactional
    void getAllSuspensByAmountSwitchIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountSwitch is greater than
        defaultSuspensFiltering("amountSwitch.greaterThan=" + SMALLER_AMOUNT_SWITCH, "amountSwitch.greaterThan=" + DEFAULT_AMOUNT_SWITCH);
    }

    @Test
    @Transactional
    void getAllSuspensByAmountParticipantIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountParticipant equals to
        defaultSuspensFiltering(
            "amountParticipant.equals=" + DEFAULT_AMOUNT_PARTICIPANT,
            "amountParticipant.equals=" + UPDATED_AMOUNT_PARTICIPANT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountParticipantIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountParticipant in
        defaultSuspensFiltering(
            "amountParticipant.in=" + DEFAULT_AMOUNT_PARTICIPANT + "," + UPDATED_AMOUNT_PARTICIPANT,
            "amountParticipant.in=" + UPDATED_AMOUNT_PARTICIPANT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountParticipantIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountParticipant is not null
        defaultSuspensFiltering("amountParticipant.specified=true", "amountParticipant.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByAmountParticipantIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountParticipant is greater than or equal to
        defaultSuspensFiltering(
            "amountParticipant.greaterThanOrEqual=" + DEFAULT_AMOUNT_PARTICIPANT,
            "amountParticipant.greaterThanOrEqual=" + UPDATED_AMOUNT_PARTICIPANT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountParticipantIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountParticipant is less than or equal to
        defaultSuspensFiltering(
            "amountParticipant.lessThanOrEqual=" + DEFAULT_AMOUNT_PARTICIPANT,
            "amountParticipant.lessThanOrEqual=" + SMALLER_AMOUNT_PARTICIPANT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountParticipantIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountParticipant is less than
        defaultSuspensFiltering(
            "amountParticipant.lessThan=" + UPDATED_AMOUNT_PARTICIPANT,
            "amountParticipant.lessThan=" + DEFAULT_AMOUNT_PARTICIPANT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByAmountParticipantIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where amountParticipant is greater than
        defaultSuspensFiltering(
            "amountParticipant.greaterThan=" + SMALLER_AMOUNT_PARTICIPANT,
            "amountParticipant.greaterThan=" + DEFAULT_AMOUNT_PARTICIPANT
        );
    }

    @Test
    @Transactional
    void getAllSuspensByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where state equals to
        defaultSuspensFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllSuspensByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where state in
        defaultSuspensFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllSuspensByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where state is not null
        defaultSuspensFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByAssigneeIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where assigneeId equals to
        defaultSuspensFiltering("assigneeId.equals=" + DEFAULT_ASSIGNEE_ID, "assigneeId.equals=" + UPDATED_ASSIGNEE_ID);
    }

    @Test
    @Transactional
    void getAllSuspensByAssigneeIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where assigneeId in
        defaultSuspensFiltering("assigneeId.in=" + DEFAULT_ASSIGNEE_ID + "," + UPDATED_ASSIGNEE_ID, "assigneeId.in=" + UPDATED_ASSIGNEE_ID);
    }

    @Test
    @Transactional
    void getAllSuspensByAssigneeIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where assigneeId is not null
        defaultSuspensFiltering("assigneeId.specified=true", "assigneeId.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByDueDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where dueDate equals to
        defaultSuspensFiltering("dueDate.equals=" + DEFAULT_DUE_DATE, "dueDate.equals=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllSuspensByDueDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where dueDate in
        defaultSuspensFiltering("dueDate.in=" + DEFAULT_DUE_DATE + "," + UPDATED_DUE_DATE, "dueDate.in=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllSuspensByDueDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where dueDate is not null
        defaultSuspensFiltering("dueDate.specified=true", "dueDate.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByDueDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where dueDate is greater than or equal to
        defaultSuspensFiltering("dueDate.greaterThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.greaterThanOrEqual=" + UPDATED_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllSuspensByDueDateIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where dueDate is less than or equal to
        defaultSuspensFiltering("dueDate.lessThanOrEqual=" + DEFAULT_DUE_DATE, "dueDate.lessThanOrEqual=" + SMALLER_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllSuspensByDueDateIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where dueDate is less than
        defaultSuspensFiltering("dueDate.lessThan=" + UPDATED_DUE_DATE, "dueDate.lessThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllSuspensByDueDateIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where dueDate is greater than
        defaultSuspensFiltering("dueDate.greaterThan=" + SMALLER_DUE_DATE, "dueDate.greaterThan=" + DEFAULT_DUE_DATE);
    }

    @Test
    @Transactional
    void getAllSuspensByOpenedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where openedAt equals to
        defaultSuspensFiltering("openedAt.equals=" + DEFAULT_OPENED_AT, "openedAt.equals=" + UPDATED_OPENED_AT);
    }

    @Test
    @Transactional
    void getAllSuspensByOpenedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where openedAt in
        defaultSuspensFiltering("openedAt.in=" + DEFAULT_OPENED_AT + "," + UPDATED_OPENED_AT, "openedAt.in=" + UPDATED_OPENED_AT);
    }

    @Test
    @Transactional
    void getAllSuspensByOpenedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where openedAt is not null
        defaultSuspensFiltering("openedAt.specified=true", "openedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByDisputeTicketIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where disputeTicketId equals to
        defaultSuspensFiltering(
            "disputeTicketId.equals=" + DEFAULT_DISPUTE_TICKET_ID,
            "disputeTicketId.equals=" + UPDATED_DISPUTE_TICKET_ID
        );
    }

    @Test
    @Transactional
    void getAllSuspensByDisputeTicketIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where disputeTicketId in
        defaultSuspensFiltering(
            "disputeTicketId.in=" + DEFAULT_DISPUTE_TICKET_ID + "," + UPDATED_DISPUTE_TICKET_ID,
            "disputeTicketId.in=" + UPDATED_DISPUTE_TICKET_ID
        );
    }

    @Test
    @Transactional
    void getAllSuspensByDisputeTicketIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where disputeTicketId is not null
        defaultSuspensFiltering("disputeTicketId.specified=true", "disputeTicketId.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByClosedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where closedAt equals to
        defaultSuspensFiltering("closedAt.equals=" + DEFAULT_CLOSED_AT, "closedAt.equals=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllSuspensByClosedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where closedAt in
        defaultSuspensFiltering("closedAt.in=" + DEFAULT_CLOSED_AT + "," + UPDATED_CLOSED_AT, "closedAt.in=" + UPDATED_CLOSED_AT);
    }

    @Test
    @Transactional
    void getAllSuspensByClosedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspens = suspensRepository.saveAndFlush(suspens);

        // Get all the suspensList where closedAt is not null
        defaultSuspensFiltering("closedAt.specified=true", "closedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensByReconciliationIsEqualToSomething() throws Exception {
        Reconciliation reconciliation;
        if (TestUtil.findAll(em, Reconciliation.class).isEmpty()) {
            suspensRepository.saveAndFlush(suspens);
            reconciliation = ReconciliationResourceIT.createEntity();
        } else {
            reconciliation = TestUtil.findAll(em, Reconciliation.class).get(0);
        }
        em.persist(reconciliation);
        em.flush();
        suspens.setReconciliation(reconciliation);
        suspensRepository.saveAndFlush(suspens);
        UUID reconciliationId = reconciliation.getId();
        // Get all the suspensList where reconciliation equals to reconciliationId
        defaultSuspensShouldBeFound("reconciliationId.equals=" + reconciliationId);

        // Get all the suspensList where reconciliation equals to UUID.randomUUID()
        defaultSuspensShouldNotBeFound("reconciliationId.equals=" + UUID.randomUUID());
    }

    private void defaultSuspensFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultSuspensShouldBeFound(shouldBeFound);
        defaultSuspensShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultSuspensShouldBeFound(String filter) throws Exception {
        restSuspensMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(suspens.getId().toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].pivotTransactionRef").value(hasItem(DEFAULT_PIVOT_TRANSACTION_REF)))
            .andExpect(jsonPath("$.[*].pivotAuthorizationRef").value(hasItem(DEFAULT_PIVOT_AUTHORIZATION_REF)))
            .andExpect(jsonPath("$.[*].pivotOperationAt").value(hasItem(DEFAULT_PIVOT_OPERATION_AT.toString())))
            .andExpect(jsonPath("$.[*].pivotAmount").value(hasItem(sameNumber(DEFAULT_PIVOT_AMOUNT))))
            .andExpect(jsonPath("$.[*].pivotParticipantCode").value(hasItem(DEFAULT_PIVOT_PARTICIPANT_CODE)))
            .andExpect(jsonPath("$.[*].amountSwitch").value(hasItem(sameNumber(DEFAULT_AMOUNT_SWITCH))))
            .andExpect(jsonPath("$.[*].amountParticipant").value(hasItem(sameNumber(DEFAULT_AMOUNT_PARTICIPANT))))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].assigneeId").value(hasItem(DEFAULT_ASSIGNEE_ID.toString())))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].openedAt").value(hasItem(DEFAULT_OPENED_AT.toString())))
            .andExpect(jsonPath("$.[*].disputeTicketId").value(hasItem(DEFAULT_DISPUTE_TICKET_ID.toString())))
            .andExpect(jsonPath("$.[*].closedAt").value(hasItem(DEFAULT_CLOSED_AT.toString())));

        // Check, that the count call also returns 1
        restSuspensMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultSuspensShouldNotBeFound(String filter) throws Exception {
        restSuspensMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restSuspensMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingSuspens() throws Exception {
        // Get the suspens
        restSuspensMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return suspensRepository.count();
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

    protected Suspens getPersistedSuspens(Suspens suspens) {
        return suspensRepository.findById(suspens.getId()).orElseThrow();
    }

    protected void assertPersistedSuspensToMatchAllProperties(Suspens expectedSuspens) {
        assertSuspensAllPropertiesEquals(expectedSuspens, getPersistedSuspens(expectedSuspens));
    }

    protected void assertPersistedSuspensToMatchUpdatableProperties(Suspens expectedSuspens) {
        assertSuspensAllUpdatablePropertiesEquals(expectedSuspens, getPersistedSuspens(expectedSuspens));
    }
}
