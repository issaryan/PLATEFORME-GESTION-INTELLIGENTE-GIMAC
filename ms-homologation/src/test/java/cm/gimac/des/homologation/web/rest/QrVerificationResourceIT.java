package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.QrVerificationAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.Operation;
import cm.gimac.des.homologation.domain.QrVerification;
import cm.gimac.des.homologation.domain.enumeration.QrForm;
import cm.gimac.des.homologation.domain.enumeration.ScenarioOutcome;
import cm.gimac.des.homologation.repository.QrVerificationRepository;
import cm.gimac.des.homologation.service.QrVerificationService;
import cm.gimac.des.homologation.service.dto.QrVerificationDTO;
import cm.gimac.des.homologation.service.mapper.QrVerificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link QrVerificationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class QrVerificationResourceIT {

    private static final QrForm DEFAULT_FORM = QrForm.STATIQUE;
    private static final QrForm UPDATED_FORM = QrForm.DYNAMIQUE;

    private static final ScenarioOutcome DEFAULT_OUTCOME = ScenarioOutcome.SUCCES;
    private static final ScenarioOutcome UPDATED_OUTCOME = ScenarioOutcome.ECHEC;

    private static final LocalDate DEFAULT_VERIFIED_ON = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VERIFIED_ON = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate SMALLER_VERIFIED_ON = LocalDate.ofEpochDay(-1L);

    private static final UUID DEFAULT_VERIFIER_ID = UUID.randomUUID();
    private static final UUID UPDATED_VERIFIER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_EVIDENCE_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID UPDATED_EVIDENCE_DOCUMENT_ID = UUID.randomUUID();

    private static final String DEFAULT_EVIDENCE_DIGEST = "9ba38d7528fffeba712e7acb76eab06423d8cf2a16bfb07f56965074330f1d30";
    private static final String UPDATED_EVIDENCE_DIGEST = "999f993e7e3b4471d8bcb9696738821eec878ada69032a6ac700d91c22beb3ce";

    private static final Instant DEFAULT_EVIDENCE_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EVIDENCE_SEALED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/qr-verifications";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private QrVerificationRepository qrVerificationRepository;

    @Mock
    private QrVerificationRepository qrVerificationRepositoryMock;

    @Autowired
    private QrVerificationMapper qrVerificationMapper;

    @Mock
    private QrVerificationService qrVerificationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restQrVerificationMockMvc;

    private QrVerification qrVerification;

    private QrVerification insertedQrVerification;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static QrVerification createEntity(EntityManager em) {
        QrVerification qrVerification = new QrVerification()
            .form(DEFAULT_FORM)
            .outcome(DEFAULT_OUTCOME)
            .verifiedOn(DEFAULT_VERIFIED_ON)
            .verifierId(DEFAULT_VERIFIER_ID)
            .evidenceDocumentId(DEFAULT_EVIDENCE_DOCUMENT_ID)
            .evidenceDigest(DEFAULT_EVIDENCE_DIGEST)
            .evidenceSealedAt(DEFAULT_EVIDENCE_SEALED_AT);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        qrVerification.setLiaison(liaison);
        // Add required entity
        Operation operation;
        if (TestUtil.findAll(em, Operation.class).isEmpty()) {
            operation = OperationResourceIT.createEntity(em);
            em.persist(operation);
            em.flush();
        } else {
            operation = TestUtil.findAll(em, Operation.class).get(0);
        }
        qrVerification.setOperation(operation);
        return qrVerification;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static QrVerification createUpdatedEntity(EntityManager em) {
        QrVerification updatedQrVerification = new QrVerification()
            .form(UPDATED_FORM)
            .outcome(UPDATED_OUTCOME)
            .verifiedOn(UPDATED_VERIFIED_ON)
            .verifierId(UPDATED_VERIFIER_ID)
            .evidenceDocumentId(UPDATED_EVIDENCE_DOCUMENT_ID)
            .evidenceDigest(UPDATED_EVIDENCE_DIGEST)
            .evidenceSealedAt(UPDATED_EVIDENCE_SEALED_AT);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        updatedQrVerification.setLiaison(liaison);
        // Add required entity
        Operation operation;
        if (TestUtil.findAll(em, Operation.class).isEmpty()) {
            operation = OperationResourceIT.createUpdatedEntity(em);
            em.persist(operation);
            em.flush();
        } else {
            operation = TestUtil.findAll(em, Operation.class).get(0);
        }
        updatedQrVerification.setOperation(operation);
        return updatedQrVerification;
    }

    @BeforeEach
    void initTest() {
        qrVerification = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedQrVerification != null) {
            qrVerificationRepository.delete(insertedQrVerification);
            insertedQrVerification = null;
        }
    }

    @Test
    @Transactional
    void createQrVerification() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the QrVerification
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);
        var returnedQrVerificationDTO = om.readValue(
            restQrVerificationMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(qrVerificationDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            QrVerificationDTO.class
        );

        // Validate the QrVerification in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedQrVerification = qrVerificationMapper.toEntity(returnedQrVerificationDTO);
        assertQrVerificationUpdatableFieldsEquals(returnedQrVerification, getPersistedQrVerification(returnedQrVerification));

        insertedQrVerification = returnedQrVerification;
    }

    @Test
    @Transactional
    void createQrVerificationWithExistingId() throws Exception {
        // Create the QrVerification with an existing ID
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restQrVerificationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QrVerification in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkFormIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qrVerification.setForm(null);

        // Create the QrVerification, which fails.
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        restQrVerificationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOutcomeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qrVerification.setOutcome(null);

        // Create the QrVerification, which fails.
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        restQrVerificationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVerifiedOnIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qrVerification.setVerifiedOn(null);

        // Create the QrVerification, which fails.
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        restQrVerificationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVerifierIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qrVerification.setVerifierId(null);

        // Create the QrVerification, which fails.
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        restQrVerificationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEvidenceDocumentIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qrVerification.setEvidenceDocumentId(null);

        // Create the QrVerification, which fails.
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        restQrVerificationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEvidenceDigestIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        qrVerification.setEvidenceDigest(null);

        // Create the QrVerification, which fails.
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        restQrVerificationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllQrVerifications() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList
        restQrVerificationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(qrVerification.getId().toString())))
            .andExpect(jsonPath("$.[*].form").value(hasItem(DEFAULT_FORM.toString())))
            .andExpect(jsonPath("$.[*].outcome").value(hasItem(DEFAULT_OUTCOME.toString())))
            .andExpect(jsonPath("$.[*].verifiedOn").value(hasItem(DEFAULT_VERIFIED_ON.toString())))
            .andExpect(jsonPath("$.[*].verifierId").value(hasItem(DEFAULT_VERIFIER_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDocumentId").value(hasItem(DEFAULT_EVIDENCE_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDigest").value(hasItem(DEFAULT_EVIDENCE_DIGEST)))
            .andExpect(jsonPath("$.[*].evidenceSealedAt").value(hasItem(DEFAULT_EVIDENCE_SEALED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllQrVerificationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(qrVerificationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restQrVerificationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(qrVerificationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllQrVerificationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(qrVerificationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restQrVerificationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(qrVerificationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getQrVerification() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get the qrVerification
        restQrVerificationMockMvc
            .perform(get(ENTITY_API_URL_ID, qrVerification.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(qrVerification.getId().toString()))
            .andExpect(jsonPath("$.form").value(DEFAULT_FORM.toString()))
            .andExpect(jsonPath("$.outcome").value(DEFAULT_OUTCOME.toString()))
            .andExpect(jsonPath("$.verifiedOn").value(DEFAULT_VERIFIED_ON.toString()))
            .andExpect(jsonPath("$.verifierId").value(DEFAULT_VERIFIER_ID.toString()))
            .andExpect(jsonPath("$.evidenceDocumentId").value(DEFAULT_EVIDENCE_DOCUMENT_ID.toString()))
            .andExpect(jsonPath("$.evidenceDigest").value(DEFAULT_EVIDENCE_DIGEST))
            .andExpect(jsonPath("$.evidenceSealedAt").value(DEFAULT_EVIDENCE_SEALED_AT.toString()));
    }

    @Test
    @Transactional
    void getQrVerificationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        UUID id = qrVerification.getId();

        defaultQrVerificationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByFormIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where form equals to
        defaultQrVerificationFiltering("form.equals=" + DEFAULT_FORM, "form.equals=" + UPDATED_FORM);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByFormIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where form in
        defaultQrVerificationFiltering("form.in=" + DEFAULT_FORM + "," + UPDATED_FORM, "form.in=" + UPDATED_FORM);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByFormIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where form is not null
        defaultQrVerificationFiltering("form.specified=true", "form.specified=false");
    }

    @Test
    @Transactional
    void getAllQrVerificationsByOutcomeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where outcome equals to
        defaultQrVerificationFiltering("outcome.equals=" + DEFAULT_OUTCOME, "outcome.equals=" + UPDATED_OUTCOME);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByOutcomeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where outcome in
        defaultQrVerificationFiltering("outcome.in=" + DEFAULT_OUTCOME + "," + UPDATED_OUTCOME, "outcome.in=" + UPDATED_OUTCOME);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByOutcomeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where outcome is not null
        defaultQrVerificationFiltering("outcome.specified=true", "outcome.specified=false");
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifiedOnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifiedOn equals to
        defaultQrVerificationFiltering("verifiedOn.equals=" + DEFAULT_VERIFIED_ON, "verifiedOn.equals=" + UPDATED_VERIFIED_ON);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifiedOnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifiedOn in
        defaultQrVerificationFiltering(
            "verifiedOn.in=" + DEFAULT_VERIFIED_ON + "," + UPDATED_VERIFIED_ON,
            "verifiedOn.in=" + UPDATED_VERIFIED_ON
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifiedOnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifiedOn is not null
        defaultQrVerificationFiltering("verifiedOn.specified=true", "verifiedOn.specified=false");
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifiedOnIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifiedOn is greater than or equal to
        defaultQrVerificationFiltering(
            "verifiedOn.greaterThanOrEqual=" + DEFAULT_VERIFIED_ON,
            "verifiedOn.greaterThanOrEqual=" + UPDATED_VERIFIED_ON
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifiedOnIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifiedOn is less than or equal to
        defaultQrVerificationFiltering(
            "verifiedOn.lessThanOrEqual=" + DEFAULT_VERIFIED_ON,
            "verifiedOn.lessThanOrEqual=" + SMALLER_VERIFIED_ON
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifiedOnIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifiedOn is less than
        defaultQrVerificationFiltering("verifiedOn.lessThan=" + UPDATED_VERIFIED_ON, "verifiedOn.lessThan=" + DEFAULT_VERIFIED_ON);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifiedOnIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifiedOn is greater than
        defaultQrVerificationFiltering("verifiedOn.greaterThan=" + SMALLER_VERIFIED_ON, "verifiedOn.greaterThan=" + DEFAULT_VERIFIED_ON);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifierIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifierId equals to
        defaultQrVerificationFiltering("verifierId.equals=" + DEFAULT_VERIFIER_ID, "verifierId.equals=" + UPDATED_VERIFIER_ID);
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifierIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifierId in
        defaultQrVerificationFiltering(
            "verifierId.in=" + DEFAULT_VERIFIER_ID + "," + UPDATED_VERIFIER_ID,
            "verifierId.in=" + UPDATED_VERIFIER_ID
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByVerifierIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where verifierId is not null
        defaultQrVerificationFiltering("verifierId.specified=true", "verifierId.specified=false");
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceDocumentIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceDocumentId equals to
        defaultQrVerificationFiltering(
            "evidenceDocumentId.equals=" + DEFAULT_EVIDENCE_DOCUMENT_ID,
            "evidenceDocumentId.equals=" + UPDATED_EVIDENCE_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceDocumentIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceDocumentId in
        defaultQrVerificationFiltering(
            "evidenceDocumentId.in=" + DEFAULT_EVIDENCE_DOCUMENT_ID + "," + UPDATED_EVIDENCE_DOCUMENT_ID,
            "evidenceDocumentId.in=" + UPDATED_EVIDENCE_DOCUMENT_ID
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceDocumentIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceDocumentId is not null
        defaultQrVerificationFiltering("evidenceDocumentId.specified=true", "evidenceDocumentId.specified=false");
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceDigest equals to
        defaultQrVerificationFiltering(
            "evidenceDigest.equals=" + DEFAULT_EVIDENCE_DIGEST,
            "evidenceDigest.equals=" + UPDATED_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceDigest in
        defaultQrVerificationFiltering(
            "evidenceDigest.in=" + DEFAULT_EVIDENCE_DIGEST + "," + UPDATED_EVIDENCE_DIGEST,
            "evidenceDigest.in=" + UPDATED_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceDigest is not null
        defaultQrVerificationFiltering("evidenceDigest.specified=true", "evidenceDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceDigest contains
        defaultQrVerificationFiltering(
            "evidenceDigest.contains=" + DEFAULT_EVIDENCE_DIGEST,
            "evidenceDigest.contains=" + UPDATED_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceDigest does not contain
        defaultQrVerificationFiltering(
            "evidenceDigest.doesNotContain=" + UPDATED_EVIDENCE_DIGEST,
            "evidenceDigest.doesNotContain=" + DEFAULT_EVIDENCE_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceSealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceSealedAt equals to
        defaultQrVerificationFiltering(
            "evidenceSealedAt.equals=" + DEFAULT_EVIDENCE_SEALED_AT,
            "evidenceSealedAt.equals=" + UPDATED_EVIDENCE_SEALED_AT
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceSealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceSealedAt in
        defaultQrVerificationFiltering(
            "evidenceSealedAt.in=" + DEFAULT_EVIDENCE_SEALED_AT + "," + UPDATED_EVIDENCE_SEALED_AT,
            "evidenceSealedAt.in=" + UPDATED_EVIDENCE_SEALED_AT
        );
    }

    @Test
    @Transactional
    void getAllQrVerificationsByEvidenceSealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        // Get all the qrVerificationList where evidenceSealedAt is not null
        defaultQrVerificationFiltering("evidenceSealedAt.specified=true", "evidenceSealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllQrVerificationsByLiaisonIsEqualToSomething() throws Exception {
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            qrVerificationRepository.saveAndFlush(qrVerification);
            liaison = LiaisonResourceIT.createEntity(em);
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        em.persist(liaison);
        em.flush();
        qrVerification.setLiaison(liaison);
        qrVerificationRepository.saveAndFlush(qrVerification);
        UUID liaisonId = liaison.getId();
        // Get all the qrVerificationList where liaison equals to liaisonId
        defaultQrVerificationShouldBeFound("liaisonId.equals=" + liaisonId);

        // Get all the qrVerificationList where liaison equals to UUID.randomUUID()
        defaultQrVerificationShouldNotBeFound("liaisonId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllQrVerificationsByOperationIsEqualToSomething() throws Exception {
        Operation operation;
        if (TestUtil.findAll(em, Operation.class).isEmpty()) {
            qrVerificationRepository.saveAndFlush(qrVerification);
            operation = OperationResourceIT.createEntity(em);
        } else {
            operation = TestUtil.findAll(em, Operation.class).get(0);
        }
        em.persist(operation);
        em.flush();
        qrVerification.setOperation(operation);
        qrVerificationRepository.saveAndFlush(qrVerification);
        UUID operationId = operation.getId();
        // Get all the qrVerificationList where operation equals to operationId
        defaultQrVerificationShouldBeFound("operationId.equals=" + operationId);

        // Get all the qrVerificationList where operation equals to UUID.randomUUID()
        defaultQrVerificationShouldNotBeFound("operationId.equals=" + UUID.randomUUID());
    }

    private void defaultQrVerificationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultQrVerificationShouldBeFound(shouldBeFound);
        defaultQrVerificationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultQrVerificationShouldBeFound(String filter) throws Exception {
        restQrVerificationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(qrVerification.getId().toString())))
            .andExpect(jsonPath("$.[*].form").value(hasItem(DEFAULT_FORM.toString())))
            .andExpect(jsonPath("$.[*].outcome").value(hasItem(DEFAULT_OUTCOME.toString())))
            .andExpect(jsonPath("$.[*].verifiedOn").value(hasItem(DEFAULT_VERIFIED_ON.toString())))
            .andExpect(jsonPath("$.[*].verifierId").value(hasItem(DEFAULT_VERIFIER_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDocumentId").value(hasItem(DEFAULT_EVIDENCE_DOCUMENT_ID.toString())))
            .andExpect(jsonPath("$.[*].evidenceDigest").value(hasItem(DEFAULT_EVIDENCE_DIGEST)))
            .andExpect(jsonPath("$.[*].evidenceSealedAt").value(hasItem(DEFAULT_EVIDENCE_SEALED_AT.toString())));

        // Check, that the count call also returns 1
        restQrVerificationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultQrVerificationShouldNotBeFound(String filter) throws Exception {
        restQrVerificationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restQrVerificationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingQrVerification() throws Exception {
        // Get the qrVerification
        restQrVerificationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingQrVerification() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the qrVerification
        QrVerification updatedQrVerification = qrVerificationRepository.findById(qrVerification.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedQrVerification are not directly saved in db
        em.detach(updatedQrVerification);
        updatedQrVerification
            .form(UPDATED_FORM)
            .outcome(UPDATED_OUTCOME)
            .verifiedOn(UPDATED_VERIFIED_ON)
            .verifierId(UPDATED_VERIFIER_ID)
            .evidenceDocumentId(UPDATED_EVIDENCE_DOCUMENT_ID)
            .evidenceDigest(UPDATED_EVIDENCE_DIGEST)
            .evidenceSealedAt(UPDATED_EVIDENCE_SEALED_AT);
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(updatedQrVerification);

        restQrVerificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, qrVerificationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isOk());

        // Validate the QrVerification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedQrVerificationToMatchAllProperties(updatedQrVerification);
    }

    @Test
    @Transactional
    void putNonExistingQrVerification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qrVerification.setId(UUID.randomUUID());

        // Create the QrVerification
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restQrVerificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, qrVerificationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QrVerification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchQrVerification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qrVerification.setId(UUID.randomUUID());

        // Create the QrVerification
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQrVerificationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QrVerification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamQrVerification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qrVerification.setId(UUID.randomUUID());

        // Create the QrVerification
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQrVerificationMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the QrVerification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateQrVerificationWithPatch() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the qrVerification using partial update
        QrVerification partialUpdatedQrVerification = new QrVerification();
        partialUpdatedQrVerification.setId(qrVerification.getId());

        partialUpdatedQrVerification
            .outcome(UPDATED_OUTCOME)
            .verifiedOn(UPDATED_VERIFIED_ON)
            .evidenceDocumentId(UPDATED_EVIDENCE_DOCUMENT_ID);

        restQrVerificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedQrVerification.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedQrVerification))
            )
            .andExpect(status().isOk());

        // Validate the QrVerification in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertQrVerificationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedQrVerification, qrVerification),
            getPersistedQrVerification(qrVerification)
        );
    }

    @Test
    @Transactional
    void fullUpdateQrVerificationWithPatch() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the qrVerification using partial update
        QrVerification partialUpdatedQrVerification = new QrVerification();
        partialUpdatedQrVerification.setId(qrVerification.getId());

        partialUpdatedQrVerification
            .form(UPDATED_FORM)
            .outcome(UPDATED_OUTCOME)
            .verifiedOn(UPDATED_VERIFIED_ON)
            .verifierId(UPDATED_VERIFIER_ID)
            .evidenceDocumentId(UPDATED_EVIDENCE_DOCUMENT_ID)
            .evidenceDigest(UPDATED_EVIDENCE_DIGEST)
            .evidenceSealedAt(UPDATED_EVIDENCE_SEALED_AT);

        restQrVerificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedQrVerification.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedQrVerification))
            )
            .andExpect(status().isOk());

        // Validate the QrVerification in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertQrVerificationUpdatableFieldsEquals(partialUpdatedQrVerification, getPersistedQrVerification(partialUpdatedQrVerification));
    }

    @Test
    @Transactional
    void patchNonExistingQrVerification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qrVerification.setId(UUID.randomUUID());

        // Create the QrVerification
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restQrVerificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, qrVerificationDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QrVerification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchQrVerification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qrVerification.setId(UUID.randomUUID());

        // Create the QrVerification
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQrVerificationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the QrVerification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamQrVerification() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        qrVerification.setId(UUID.randomUUID());

        // Create the QrVerification
        QrVerificationDTO qrVerificationDTO = qrVerificationMapper.toDto(qrVerification);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restQrVerificationMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(qrVerificationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the QrVerification in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteQrVerification() throws Exception {
        // Initialize the database
        insertedQrVerification = qrVerificationRepository.saveAndFlush(qrVerification);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the qrVerification
        restQrVerificationMockMvc
            .perform(delete(ENTITY_API_URL_ID, qrVerification.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return qrVerificationRepository.count();
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

    protected QrVerification getPersistedQrVerification(QrVerification qrVerification) {
        return qrVerificationRepository.findById(qrVerification.getId()).orElseThrow();
    }

    protected void assertPersistedQrVerificationToMatchAllProperties(QrVerification expectedQrVerification) {
        assertQrVerificationAllPropertiesEquals(expectedQrVerification, getPersistedQrVerification(expectedQrVerification));
    }

    protected void assertPersistedQrVerificationToMatchUpdatableProperties(QrVerification expectedQrVerification) {
        assertQrVerificationAllUpdatablePropertiesEquals(expectedQrVerification, getPersistedQrVerification(expectedQrVerification));
    }
}
