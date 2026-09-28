package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.EscalationAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.Escalation;
import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.domain.enumeration.EscalationScope;
import cm.gimac.des.incident.repository.EscalationRepository;
import cm.gimac.des.incident.service.EscalationService;
import cm.gimac.des.incident.service.mapper.EscalationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Duration;
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
 * Integration tests for the {@link EscalationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class EscalationResourceIT {

    private static final EscalationScope DEFAULT_SCOPE = EscalationScope.INTERNE;
    private static final EscalationScope UPDATED_SCOPE = EscalationScope.EXTERNE;

    private static final Integer DEFAULT_LEVEL = 1;
    private static final Integer UPDATED_LEVEL = 2;
    private static final Integer SMALLER_LEVEL = 1 - 1;

    private static final UUID DEFAULT_TARGET_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_TARGET_USER_ID = UUID.randomUUID();

    private static final UUID DEFAULT_TARGET_CONTACT_ID = UUID.randomUUID();
    private static final UUID UPDATED_TARGET_CONTACT_ID = UUID.randomUUID();

    private static final Instant DEFAULT_TRIGGERED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_TRIGGERED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final Duration DEFAULT_IMPUTABLE_DELAY = Duration.ofHours(6);
    private static final Duration UPDATED_IMPUTABLE_DELAY = Duration.ofHours(12);
    private static final Duration SMALLER_IMPUTABLE_DELAY = Duration.ofHours(5);

    private static final String ENTITY_API_URL = "/api/escalations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EscalationRepository escalationRepository;

    @Mock
    private EscalationRepository escalationRepositoryMock;

    @Autowired
    private EscalationMapper escalationMapper;

    @Mock
    private EscalationService escalationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEscalationMockMvc;

    private Escalation escalation;

    private Escalation insertedEscalation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Escalation createEntity(EntityManager em) {
        Escalation escalation = new Escalation()
            .scope(DEFAULT_SCOPE)
            .level(DEFAULT_LEVEL)
            .targetUserId(DEFAULT_TARGET_USER_ID)
            .targetContactId(DEFAULT_TARGET_CONTACT_ID)
            .triggeredAt(DEFAULT_TRIGGERED_AT)
            .reason(DEFAULT_REASON)
            .imputableDelay(DEFAULT_IMPUTABLE_DELAY);
        // Add required entity
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            ticket = TicketResourceIT.createEntity();
            em.persist(ticket);
            em.flush();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        escalation.setTicket(ticket);
        return escalation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Escalation createUpdatedEntity(EntityManager em) {
        Escalation updatedEscalation = new Escalation()
            .scope(UPDATED_SCOPE)
            .level(UPDATED_LEVEL)
            .targetUserId(UPDATED_TARGET_USER_ID)
            .targetContactId(UPDATED_TARGET_CONTACT_ID)
            .triggeredAt(UPDATED_TRIGGERED_AT)
            .reason(UPDATED_REASON)
            .imputableDelay(UPDATED_IMPUTABLE_DELAY);
        // Add required entity
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            ticket = TicketResourceIT.createUpdatedEntity();
            em.persist(ticket);
            em.flush();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        updatedEscalation.setTicket(ticket);
        return updatedEscalation;
    }

    @BeforeEach
    void initTest() {
        escalation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEscalation != null) {
            escalationRepository.delete(insertedEscalation);
            insertedEscalation = null;
        }
    }

    @Test
    @Transactional
    void getAllEscalations() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(escalation.getId().toString())))
            .andExpect(jsonPath("$.[*].scope").value(hasItem(DEFAULT_SCOPE.toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL)))
            .andExpect(jsonPath("$.[*].targetUserId").value(hasItem(DEFAULT_TARGET_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].targetContactId").value(hasItem(DEFAULT_TARGET_CONTACT_ID.toString())))
            .andExpect(jsonPath("$.[*].triggeredAt").value(hasItem(DEFAULT_TRIGGERED_AT.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].imputableDelay").value(hasItem(DEFAULT_IMPUTABLE_DELAY.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEscalationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(escalationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEscalationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(escalationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEscalationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(escalationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEscalationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(escalationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getEscalation() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get the escalation
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL_ID, escalation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(escalation.getId().toString()))
            .andExpect(jsonPath("$.scope").value(DEFAULT_SCOPE.toString()))
            .andExpect(jsonPath("$.level").value(DEFAULT_LEVEL))
            .andExpect(jsonPath("$.targetUserId").value(DEFAULT_TARGET_USER_ID.toString()))
            .andExpect(jsonPath("$.targetContactId").value(DEFAULT_TARGET_CONTACT_ID.toString()))
            .andExpect(jsonPath("$.triggeredAt").value(DEFAULT_TRIGGERED_AT.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.imputableDelay").value(DEFAULT_IMPUTABLE_DELAY.toString()));
    }

    @Test
    @Transactional
    void getEscalationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        UUID id = escalation.getId();

        defaultEscalationFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllEscalationsByScopeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where scope equals to
        defaultEscalationFiltering("scope.equals=" + DEFAULT_SCOPE, "scope.equals=" + UPDATED_SCOPE);
    }

    @Test
    @Transactional
    void getAllEscalationsByScopeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where scope in
        defaultEscalationFiltering("scope.in=" + DEFAULT_SCOPE + "," + UPDATED_SCOPE, "scope.in=" + UPDATED_SCOPE);
    }

    @Test
    @Transactional
    void getAllEscalationsByScopeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where scope is not null
        defaultEscalationFiltering("scope.specified=true", "scope.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level equals to
        defaultEscalationFiltering("level.equals=" + DEFAULT_LEVEL, "level.equals=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level in
        defaultEscalationFiltering("level.in=" + DEFAULT_LEVEL + "," + UPDATED_LEVEL, "level.in=" + UPDATED_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is not null
        defaultEscalationFiltering("level.specified=true", "level.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is greater than or equal to
        defaultEscalationFiltering("level.greaterThanOrEqual=" + DEFAULT_LEVEL, "level.greaterThanOrEqual=" + (DEFAULT_LEVEL + 1));
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is less than or equal to
        defaultEscalationFiltering("level.lessThanOrEqual=" + DEFAULT_LEVEL, "level.lessThanOrEqual=" + SMALLER_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is less than
        defaultEscalationFiltering("level.lessThan=" + (DEFAULT_LEVEL + 1), "level.lessThan=" + DEFAULT_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByLevelIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where level is greater than
        defaultEscalationFiltering("level.greaterThan=" + SMALLER_LEVEL, "level.greaterThan=" + DEFAULT_LEVEL);
    }

    @Test
    @Transactional
    void getAllEscalationsByTargetUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where targetUserId equals to
        defaultEscalationFiltering("targetUserId.equals=" + DEFAULT_TARGET_USER_ID, "targetUserId.equals=" + UPDATED_TARGET_USER_ID);
    }

    @Test
    @Transactional
    void getAllEscalationsByTargetUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where targetUserId in
        defaultEscalationFiltering(
            "targetUserId.in=" + DEFAULT_TARGET_USER_ID + "," + UPDATED_TARGET_USER_ID,
            "targetUserId.in=" + UPDATED_TARGET_USER_ID
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByTargetUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where targetUserId is not null
        defaultEscalationFiltering("targetUserId.specified=true", "targetUserId.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByTargetContactIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where targetContactId equals to
        defaultEscalationFiltering(
            "targetContactId.equals=" + DEFAULT_TARGET_CONTACT_ID,
            "targetContactId.equals=" + UPDATED_TARGET_CONTACT_ID
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByTargetContactIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where targetContactId in
        defaultEscalationFiltering(
            "targetContactId.in=" + DEFAULT_TARGET_CONTACT_ID + "," + UPDATED_TARGET_CONTACT_ID,
            "targetContactId.in=" + UPDATED_TARGET_CONTACT_ID
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByTargetContactIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where targetContactId is not null
        defaultEscalationFiltering("targetContactId.specified=true", "targetContactId.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByTriggeredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where triggeredAt equals to
        defaultEscalationFiltering("triggeredAt.equals=" + DEFAULT_TRIGGERED_AT, "triggeredAt.equals=" + UPDATED_TRIGGERED_AT);
    }

    @Test
    @Transactional
    void getAllEscalationsByTriggeredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where triggeredAt in
        defaultEscalationFiltering(
            "triggeredAt.in=" + DEFAULT_TRIGGERED_AT + "," + UPDATED_TRIGGERED_AT,
            "triggeredAt.in=" + UPDATED_TRIGGERED_AT
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByTriggeredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where triggeredAt is not null
        defaultEscalationFiltering("triggeredAt.specified=true", "triggeredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason equals to
        defaultEscalationFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason in
        defaultEscalationFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason is not null
        defaultEscalationFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason contains
        defaultEscalationFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllEscalationsByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where reason does not contain
        defaultEscalationFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllEscalationsByImputableDelayIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where imputableDelay equals to
        defaultEscalationFiltering("imputableDelay.equals=" + DEFAULT_IMPUTABLE_DELAY, "imputableDelay.equals=" + UPDATED_IMPUTABLE_DELAY);
    }

    @Test
    @Transactional
    void getAllEscalationsByImputableDelayIsInShouldWork() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where imputableDelay in
        defaultEscalationFiltering(
            "imputableDelay.in=" + DEFAULT_IMPUTABLE_DELAY + "," + UPDATED_IMPUTABLE_DELAY,
            "imputableDelay.in=" + UPDATED_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByImputableDelayIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where imputableDelay is not null
        defaultEscalationFiltering("imputableDelay.specified=true", "imputableDelay.specified=false");
    }

    @Test
    @Transactional
    void getAllEscalationsByImputableDelayIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where imputableDelay is greater than or equal to
        defaultEscalationFiltering(
            "imputableDelay.greaterThanOrEqual=" + DEFAULT_IMPUTABLE_DELAY,
            "imputableDelay.greaterThanOrEqual=" + UPDATED_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByImputableDelayIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where imputableDelay is less than or equal to
        defaultEscalationFiltering(
            "imputableDelay.lessThanOrEqual=" + DEFAULT_IMPUTABLE_DELAY,
            "imputableDelay.lessThanOrEqual=" + SMALLER_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByImputableDelayIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where imputableDelay is less than
        defaultEscalationFiltering(
            "imputableDelay.lessThan=" + UPDATED_IMPUTABLE_DELAY,
            "imputableDelay.lessThan=" + DEFAULT_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByImputableDelayIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedEscalation = escalationRepository.saveAndFlush(escalation);

        // Get all the escalationList where imputableDelay is greater than
        defaultEscalationFiltering(
            "imputableDelay.greaterThan=" + SMALLER_IMPUTABLE_DELAY,
            "imputableDelay.greaterThan=" + DEFAULT_IMPUTABLE_DELAY
        );
    }

    @Test
    @Transactional
    void getAllEscalationsByTicketIsEqualToSomething() throws Exception {
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            escalationRepository.saveAndFlush(escalation);
            ticket = TicketResourceIT.createEntity();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        em.persist(ticket);
        em.flush();
        escalation.setTicket(ticket);
        escalationRepository.saveAndFlush(escalation);
        UUID ticketId = ticket.getId();
        // Get all the escalationList where ticket equals to ticketId
        defaultEscalationShouldBeFound("ticketId.equals=" + ticketId);

        // Get all the escalationList where ticket equals to UUID.randomUUID()
        defaultEscalationShouldNotBeFound("ticketId.equals=" + UUID.randomUUID());
    }

    private void defaultEscalationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultEscalationShouldBeFound(shouldBeFound);
        defaultEscalationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultEscalationShouldBeFound(String filter) throws Exception {
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(escalation.getId().toString())))
            .andExpect(jsonPath("$.[*].scope").value(hasItem(DEFAULT_SCOPE.toString())))
            .andExpect(jsonPath("$.[*].level").value(hasItem(DEFAULT_LEVEL)))
            .andExpect(jsonPath("$.[*].targetUserId").value(hasItem(DEFAULT_TARGET_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].targetContactId").value(hasItem(DEFAULT_TARGET_CONTACT_ID.toString())))
            .andExpect(jsonPath("$.[*].triggeredAt").value(hasItem(DEFAULT_TRIGGERED_AT.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].imputableDelay").value(hasItem(DEFAULT_IMPUTABLE_DELAY.toString())));

        // Check, that the count call also returns 1
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultEscalationShouldNotBeFound(String filter) throws Exception {
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restEscalationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingEscalation() throws Exception {
        // Get the escalation
        restEscalationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return escalationRepository.count();
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

    protected Escalation getPersistedEscalation(Escalation escalation) {
        return escalationRepository.findById(escalation.getId()).orElseThrow();
    }

    protected void assertPersistedEscalationToMatchAllProperties(Escalation expectedEscalation) {
        assertEscalationAllPropertiesEquals(expectedEscalation, getPersistedEscalation(expectedEscalation));
    }

    protected void assertPersistedEscalationToMatchUpdatableProperties(Escalation expectedEscalation) {
        assertEscalationAllUpdatablePropertiesEquals(expectedEscalation, getPersistedEscalation(expectedEscalation));
    }
}
