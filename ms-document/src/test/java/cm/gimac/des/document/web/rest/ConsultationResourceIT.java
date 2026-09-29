package cm.gimac.des.document.web.rest;

import static cm.gimac.des.document.domain.ConsultationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.document.IntegrationTest;
import cm.gimac.des.document.domain.Consultation;
import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.domain.enumeration.ConsultationAction;
import cm.gimac.des.document.domain.enumeration.ConsultationChannel;
import cm.gimac.des.document.repository.ConsultationRepository;
import cm.gimac.des.document.service.ConsultationService;
import cm.gimac.des.document.service.mapper.ConsultationMapper;
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
import cm.gimac.des.document.security.AuthoritiesConstants;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ConsultationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class ConsultationResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final ConsultationAction DEFAULT_ACTION = ConsultationAction.LECTURE;
    private static final ConsultationAction UPDATED_ACTION = ConsultationAction.TELECHARGEMENT;

    private static final ConsultationChannel DEFAULT_CHANNEL = ConsultationChannel.PORTAIL_DMZ;
    private static final ConsultationChannel UPDATED_CHANNEL = ConsultationChannel.DEPARTEMENT;

    private static final Instant DEFAULT_CONSULTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CONSULTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final UUID DEFAULT_CORRELATION_ID = UUID.randomUUID();
    private static final UUID UPDATED_CORRELATION_ID = UUID.randomUUID();

    private static final Boolean DEFAULT_GRANTED = false;
    private static final Boolean UPDATED_GRANTED = true;

    private static final String DEFAULT_REFUSAL_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REFUSAL_REASON = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/consultations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Mock
    private ConsultationRepository consultationRepositoryMock;

    @Autowired
    private ConsultationMapper consultationMapper;

    @Mock
    private ConsultationService consultationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restConsultationMockMvc;

    private Consultation consultation;

    private Consultation insertedConsultation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Consultation createEntity(EntityManager em) {
        Consultation consultation = new Consultation()
            .userId(DEFAULT_USER_ID)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .action(DEFAULT_ACTION)
            .channel(DEFAULT_CHANNEL)
            .consultedAt(DEFAULT_CONSULTED_AT)
            .correlationId(DEFAULT_CORRELATION_ID)
            .granted(DEFAULT_GRANTED)
            .refusalReason(DEFAULT_REFUSAL_REASON);
        // Add required entity
        Document document;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            document = DocumentResourceIT.createEntity(em);
            em.persist(document);
            em.flush();
        } else {
            document = TestUtil.findAll(em, Document.class).get(0);
        }
        consultation.setDocument(document);
        return consultation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Consultation createUpdatedEntity(EntityManager em) {
        Consultation updatedConsultation = new Consultation()
            .userId(UPDATED_USER_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .action(UPDATED_ACTION)
            .channel(UPDATED_CHANNEL)
            .consultedAt(UPDATED_CONSULTED_AT)
            .correlationId(UPDATED_CORRELATION_ID)
            .granted(UPDATED_GRANTED)
            .refusalReason(UPDATED_REFUSAL_REASON);
        // Add required entity
        Document document;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            document = DocumentResourceIT.createUpdatedEntity(em);
            em.persist(document);
            em.flush();
        } else {
            document = TestUtil.findAll(em, Document.class).get(0);
        }
        updatedConsultation.setDocument(document);
        return updatedConsultation;
    }

    @BeforeEach
    void initTest() {
        consultation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedConsultation != null) {
            consultationRepository.delete(insertedConsultation);
            insertedConsultation = null;
        }
    }

    @Test
    @Transactional
    void getAllConsultations() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList
        restConsultationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(consultation.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].channel").value(hasItem(DEFAULT_CHANNEL.toString())))
            .andExpect(jsonPath("$.[*].consultedAt").value(hasItem(DEFAULT_CONSULTED_AT.toString())))
            .andExpect(jsonPath("$.[*].correlationId").value(hasItem(DEFAULT_CORRELATION_ID.toString())))
            .andExpect(jsonPath("$.[*].granted").value(hasItem(DEFAULT_GRANTED)))
            .andExpect(jsonPath("$.[*].refusalReason").value(hasItem(DEFAULT_REFUSAL_REASON)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConsultationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(consultationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConsultationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(consultationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllConsultationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(consultationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restConsultationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(consultationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getConsultation() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get the consultation
        restConsultationMockMvc
            .perform(get(ENTITY_API_URL_ID, consultation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(consultation.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.action").value(DEFAULT_ACTION.toString()))
            .andExpect(jsonPath("$.channel").value(DEFAULT_CHANNEL.toString()))
            .andExpect(jsonPath("$.consultedAt").value(DEFAULT_CONSULTED_AT.toString()))
            .andExpect(jsonPath("$.correlationId").value(DEFAULT_CORRELATION_ID.toString()))
            .andExpect(jsonPath("$.granted").value(DEFAULT_GRANTED))
            .andExpect(jsonPath("$.refusalReason").value(DEFAULT_REFUSAL_REASON));
    }

    @Test
    @Transactional
    void getConsultationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        UUID id = consultation.getId();

        defaultConsultationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllConsultationsByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where userId equals to
        defaultConsultationFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllConsultationsByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where userId in
        defaultConsultationFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllConsultationsByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where userId is not null
        defaultConsultationFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllConsultationsByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where institutionId equals to
        defaultConsultationFiltering("institutionId.equals=" + DEFAULT_INSTITUTION_ID, "institutionId.equals=" + UPDATED_INSTITUTION_ID);
    }

    @Test
    @Transactional
    void getAllConsultationsByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where institutionId in
        defaultConsultationFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllConsultationsByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where institutionId is not null
        defaultConsultationFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllConsultationsByActionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where action equals to
        defaultConsultationFiltering("action.equals=" + DEFAULT_ACTION, "action.equals=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    void getAllConsultationsByActionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where action in
        defaultConsultationFiltering("action.in=" + DEFAULT_ACTION + "," + UPDATED_ACTION, "action.in=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    void getAllConsultationsByActionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where action is not null
        defaultConsultationFiltering("action.specified=true", "action.specified=false");
    }

    @Test
    @Transactional
    void getAllConsultationsByChannelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where channel equals to
        defaultConsultationFiltering("channel.equals=" + DEFAULT_CHANNEL, "channel.equals=" + UPDATED_CHANNEL);
    }

    @Test
    @Transactional
    void getAllConsultationsByChannelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where channel in
        defaultConsultationFiltering("channel.in=" + DEFAULT_CHANNEL + "," + UPDATED_CHANNEL, "channel.in=" + UPDATED_CHANNEL);
    }

    @Test
    @Transactional
    void getAllConsultationsByChannelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where channel is not null
        defaultConsultationFiltering("channel.specified=true", "channel.specified=false");
    }

    @Test
    @Transactional
    void getAllConsultationsByConsultedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where consultedAt equals to
        defaultConsultationFiltering("consultedAt.equals=" + DEFAULT_CONSULTED_AT, "consultedAt.equals=" + UPDATED_CONSULTED_AT);
    }

    @Test
    @Transactional
    void getAllConsultationsByConsultedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where consultedAt in
        defaultConsultationFiltering(
            "consultedAt.in=" + DEFAULT_CONSULTED_AT + "," + UPDATED_CONSULTED_AT,
            "consultedAt.in=" + UPDATED_CONSULTED_AT
        );
    }

    @Test
    @Transactional
    void getAllConsultationsByConsultedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where consultedAt is not null
        defaultConsultationFiltering("consultedAt.specified=true", "consultedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllConsultationsByCorrelationIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where correlationId equals to
        defaultConsultationFiltering("correlationId.equals=" + DEFAULT_CORRELATION_ID, "correlationId.equals=" + UPDATED_CORRELATION_ID);
    }

    @Test
    @Transactional
    void getAllConsultationsByCorrelationIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where correlationId in
        defaultConsultationFiltering(
            "correlationId.in=" + DEFAULT_CORRELATION_ID + "," + UPDATED_CORRELATION_ID,
            "correlationId.in=" + UPDATED_CORRELATION_ID
        );
    }

    @Test
    @Transactional
    void getAllConsultationsByCorrelationIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where correlationId is not null
        defaultConsultationFiltering("correlationId.specified=true", "correlationId.specified=false");
    }

    @Test
    @Transactional
    void getAllConsultationsByGrantedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where granted equals to
        defaultConsultationFiltering("granted.equals=" + DEFAULT_GRANTED, "granted.equals=" + UPDATED_GRANTED);
    }

    @Test
    @Transactional
    void getAllConsultationsByGrantedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where granted in
        defaultConsultationFiltering("granted.in=" + DEFAULT_GRANTED + "," + UPDATED_GRANTED, "granted.in=" + UPDATED_GRANTED);
    }

    @Test
    @Transactional
    void getAllConsultationsByGrantedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where granted is not null
        defaultConsultationFiltering("granted.specified=true", "granted.specified=false");
    }

    @Test
    @Transactional
    void getAllConsultationsByRefusalReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where refusalReason equals to
        defaultConsultationFiltering("refusalReason.equals=" + DEFAULT_REFUSAL_REASON, "refusalReason.equals=" + UPDATED_REFUSAL_REASON);
    }

    @Test
    @Transactional
    void getAllConsultationsByRefusalReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where refusalReason in
        defaultConsultationFiltering(
            "refusalReason.in=" + DEFAULT_REFUSAL_REASON + "," + UPDATED_REFUSAL_REASON,
            "refusalReason.in=" + UPDATED_REFUSAL_REASON
        );
    }

    @Test
    @Transactional
    void getAllConsultationsByRefusalReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where refusalReason is not null
        defaultConsultationFiltering("refusalReason.specified=true", "refusalReason.specified=false");
    }

    @Test
    @Transactional
    void getAllConsultationsByRefusalReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where refusalReason contains
        defaultConsultationFiltering(
            "refusalReason.contains=" + DEFAULT_REFUSAL_REASON,
            "refusalReason.contains=" + UPDATED_REFUSAL_REASON
        );
    }

    @Test
    @Transactional
    void getAllConsultationsByRefusalReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedConsultation = consultationRepository.saveAndFlush(consultation);

        // Get all the consultationList where refusalReason does not contain
        defaultConsultationFiltering(
            "refusalReason.doesNotContain=" + UPDATED_REFUSAL_REASON,
            "refusalReason.doesNotContain=" + DEFAULT_REFUSAL_REASON
        );
    }

    @Test
    @Transactional
    void getAllConsultationsByDocumentIsEqualToSomething() throws Exception {
        Document document;
        if (TestUtil.findAll(em, Document.class).isEmpty()) {
            consultationRepository.saveAndFlush(consultation);
            document = DocumentResourceIT.createEntity(em);
        } else {
            document = TestUtil.findAll(em, Document.class).get(0);
        }
        em.persist(document);
        em.flush();
        consultation.setDocument(document);
        consultationRepository.saveAndFlush(consultation);
        UUID documentId = document.getId();
        // Get all the consultationList where document equals to documentId
        defaultConsultationShouldBeFound("documentId.equals=" + documentId);

        // Get all the consultationList where document equals to UUID.randomUUID()
        defaultConsultationShouldNotBeFound("documentId.equals=" + UUID.randomUUID());
    }

    private void defaultConsultationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultConsultationShouldBeFound(shouldBeFound);
        defaultConsultationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultConsultationShouldBeFound(String filter) throws Exception {
        restConsultationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(consultation.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].channel").value(hasItem(DEFAULT_CHANNEL.toString())))
            .andExpect(jsonPath("$.[*].consultedAt").value(hasItem(DEFAULT_CONSULTED_AT.toString())))
            .andExpect(jsonPath("$.[*].correlationId").value(hasItem(DEFAULT_CORRELATION_ID.toString())))
            .andExpect(jsonPath("$.[*].granted").value(hasItem(DEFAULT_GRANTED)))
            .andExpect(jsonPath("$.[*].refusalReason").value(hasItem(DEFAULT_REFUSAL_REASON)));

        // Check, that the count call also returns 1
        restConsultationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultConsultationShouldNotBeFound(String filter) throws Exception {
        restConsultationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restConsultationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingConsultation() throws Exception {
        // Get the consultation
        restConsultationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return consultationRepository.count();
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

    protected Consultation getPersistedConsultation(Consultation consultation) {
        return consultationRepository.findById(consultation.getId()).orElseThrow();
    }

    protected void assertPersistedConsultationToMatchAllProperties(Consultation expectedConsultation) {
        assertConsultationAllPropertiesEquals(expectedConsultation, getPersistedConsultation(expectedConsultation));
    }

    protected void assertPersistedConsultationToMatchUpdatableProperties(Consultation expectedConsultation) {
        assertConsultationAllUpdatablePropertiesEquals(expectedConsultation, getPersistedConsultation(expectedConsultation));
    }
}
