package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.TransitionAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.domain.Transition;
import cm.gimac.des.incident.domain.enumeration.TicketState;
import cm.gimac.des.incident.domain.enumeration.TicketState;
import cm.gimac.des.incident.repository.TransitionRepository;
import cm.gimac.des.incident.service.TransitionService;
import cm.gimac.des.incident.service.mapper.TransitionMapper;
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
 * Integration tests for the {@link TransitionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class TransitionResourceIT {

    private static final Long DEFAULT_CHAIN_INDEX = 1L;
    private static final Long UPDATED_CHAIN_INDEX = 2L;
    private static final Long SMALLER_CHAIN_INDEX = 1L - 1L;

    private static final TicketState DEFAULT_FROM_STATE = TicketState.OUVERT;
    private static final TicketState UPDATED_FROM_STATE = TicketState.QUALIFIE;

    private static final TicketState DEFAULT_TO_STATE = TicketState.OUVERT;
    private static final TicketState UPDATED_TO_STATE = TicketState.QUALIFIE;

    private static final UUID DEFAULT_ACTOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_ACTOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_OCCURRED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OCCURRED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_JUSTIFICATION = "AAAAAAAAAA";
    private static final String UPDATED_JUSTIFICATION = "BBBBBBBBBB";

    private static final String DEFAULT_PREVIOUS_HASH = "4cdfcd57d985a97f0e64d69a941141a2c7b1103f57101768c0eff76a0716fb1c";
    private static final String UPDATED_PREVIOUS_HASH = "00b6732dcd6e2e50b6638d36184409be20db8ab676e3ea6752cb0fe0745a733d";

    private static final String DEFAULT_HASH = "1fc8b501b13e29a33c947b68801e13974b9dc43c1f6cd3af16e7032ae124a4de";
    private static final String UPDATED_HASH = "54af8622b8405c1945997830ed505b2457ba043516692d074a5f6db23157345c";

    private static final String ENTITY_API_URL = "/api/transitions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TransitionRepository transitionRepository;

    @Mock
    private TransitionRepository transitionRepositoryMock;

    @Autowired
    private TransitionMapper transitionMapper;

    @Mock
    private TransitionService transitionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTransitionMockMvc;

    private Transition transition;

    private Transition insertedTransition;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Transition createEntity(EntityManager em) {
        Transition transition = new Transition()
            .chainIndex(DEFAULT_CHAIN_INDEX)
            .fromState(DEFAULT_FROM_STATE)
            .toState(DEFAULT_TO_STATE)
            .actorId(DEFAULT_ACTOR_ID)
            .occurredAt(DEFAULT_OCCURRED_AT)
            .justification(DEFAULT_JUSTIFICATION)
            .previousHash(DEFAULT_PREVIOUS_HASH)
            .hash(DEFAULT_HASH);
        // Add required entity
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            ticket = TicketResourceIT.createEntity();
            em.persist(ticket);
            em.flush();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        transition.setTicket(ticket);
        return transition;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Transition createUpdatedEntity(EntityManager em) {
        Transition updatedTransition = new Transition()
            .chainIndex(UPDATED_CHAIN_INDEX)
            .fromState(UPDATED_FROM_STATE)
            .toState(UPDATED_TO_STATE)
            .actorId(UPDATED_ACTOR_ID)
            .occurredAt(UPDATED_OCCURRED_AT)
            .justification(UPDATED_JUSTIFICATION)
            .previousHash(UPDATED_PREVIOUS_HASH)
            .hash(UPDATED_HASH);
        // Add required entity
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            ticket = TicketResourceIT.createUpdatedEntity();
            em.persist(ticket);
            em.flush();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        updatedTransition.setTicket(ticket);
        return updatedTransition;
    }

    @BeforeEach
    void initTest() {
        transition = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedTransition != null) {
            transitionRepository.delete(insertedTransition);
            insertedTransition = null;
        }
    }

    @Test
    @Transactional
    void getAllTransitions() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList
        restTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(transition.getId().toString())))
            .andExpect(jsonPath("$.[*].chainIndex").value(hasItem(DEFAULT_CHAIN_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].fromState").value(hasItem(DEFAULT_FROM_STATE.toString())))
            .andExpect(jsonPath("$.[*].toState").value(hasItem(DEFAULT_TO_STATE.toString())))
            .andExpect(jsonPath("$.[*].actorId").value(hasItem(DEFAULT_ACTOR_ID.toString())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].justification").value(hasItem(DEFAULT_JUSTIFICATION)))
            .andExpect(jsonPath("$.[*].previousHash").value(hasItem(DEFAULT_PREVIOUS_HASH)))
            .andExpect(jsonPath("$.[*].hash").value(hasItem(DEFAULT_HASH)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTransitionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(transitionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTransitionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(transitionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllTransitionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(transitionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restTransitionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(transitionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getTransition() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get the transition
        restTransitionMockMvc
            .perform(get(ENTITY_API_URL_ID, transition.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(transition.getId().toString()))
            .andExpect(jsonPath("$.chainIndex").value(DEFAULT_CHAIN_INDEX.intValue()))
            .andExpect(jsonPath("$.fromState").value(DEFAULT_FROM_STATE.toString()))
            .andExpect(jsonPath("$.toState").value(DEFAULT_TO_STATE.toString()))
            .andExpect(jsonPath("$.actorId").value(DEFAULT_ACTOR_ID.toString()))
            .andExpect(jsonPath("$.occurredAt").value(DEFAULT_OCCURRED_AT.toString()))
            .andExpect(jsonPath("$.justification").value(DEFAULT_JUSTIFICATION))
            .andExpect(jsonPath("$.previousHash").value(DEFAULT_PREVIOUS_HASH))
            .andExpect(jsonPath("$.hash").value(DEFAULT_HASH));
    }

    @Test
    @Transactional
    void getTransitionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        UUID id = transition.getId();

        defaultTransitionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllTransitionsByChainIndexIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where chainIndex equals to
        defaultTransitionFiltering("chainIndex.equals=" + DEFAULT_CHAIN_INDEX, "chainIndex.equals=" + UPDATED_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllTransitionsByChainIndexIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where chainIndex in
        defaultTransitionFiltering(
            "chainIndex.in=" + DEFAULT_CHAIN_INDEX + "," + UPDATED_CHAIN_INDEX,
            "chainIndex.in=" + UPDATED_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllTransitionsByChainIndexIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where chainIndex is not null
        defaultTransitionFiltering("chainIndex.specified=true", "chainIndex.specified=false");
    }

    @Test
    @Transactional
    void getAllTransitionsByChainIndexIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where chainIndex is greater than or equal to
        defaultTransitionFiltering(
            "chainIndex.greaterThanOrEqual=" + DEFAULT_CHAIN_INDEX,
            "chainIndex.greaterThanOrEqual=" + UPDATED_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllTransitionsByChainIndexIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where chainIndex is less than or equal to
        defaultTransitionFiltering(
            "chainIndex.lessThanOrEqual=" + DEFAULT_CHAIN_INDEX,
            "chainIndex.lessThanOrEqual=" + SMALLER_CHAIN_INDEX
        );
    }

    @Test
    @Transactional
    void getAllTransitionsByChainIndexIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where chainIndex is less than
        defaultTransitionFiltering("chainIndex.lessThan=" + UPDATED_CHAIN_INDEX, "chainIndex.lessThan=" + DEFAULT_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllTransitionsByChainIndexIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where chainIndex is greater than
        defaultTransitionFiltering("chainIndex.greaterThan=" + SMALLER_CHAIN_INDEX, "chainIndex.greaterThan=" + DEFAULT_CHAIN_INDEX);
    }

    @Test
    @Transactional
    void getAllTransitionsByFromStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where fromState equals to
        defaultTransitionFiltering("fromState.equals=" + DEFAULT_FROM_STATE, "fromState.equals=" + UPDATED_FROM_STATE);
    }

    @Test
    @Transactional
    void getAllTransitionsByFromStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where fromState in
        defaultTransitionFiltering("fromState.in=" + DEFAULT_FROM_STATE + "," + UPDATED_FROM_STATE, "fromState.in=" + UPDATED_FROM_STATE);
    }

    @Test
    @Transactional
    void getAllTransitionsByFromStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where fromState is not null
        defaultTransitionFiltering("fromState.specified=true", "fromState.specified=false");
    }

    @Test
    @Transactional
    void getAllTransitionsByToStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where toState equals to
        defaultTransitionFiltering("toState.equals=" + DEFAULT_TO_STATE, "toState.equals=" + UPDATED_TO_STATE);
    }

    @Test
    @Transactional
    void getAllTransitionsByToStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where toState in
        defaultTransitionFiltering("toState.in=" + DEFAULT_TO_STATE + "," + UPDATED_TO_STATE, "toState.in=" + UPDATED_TO_STATE);
    }

    @Test
    @Transactional
    void getAllTransitionsByToStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where toState is not null
        defaultTransitionFiltering("toState.specified=true", "toState.specified=false");
    }

    @Test
    @Transactional
    void getAllTransitionsByActorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where actorId equals to
        defaultTransitionFiltering("actorId.equals=" + DEFAULT_ACTOR_ID, "actorId.equals=" + UPDATED_ACTOR_ID);
    }

    @Test
    @Transactional
    void getAllTransitionsByActorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where actorId in
        defaultTransitionFiltering("actorId.in=" + DEFAULT_ACTOR_ID + "," + UPDATED_ACTOR_ID, "actorId.in=" + UPDATED_ACTOR_ID);
    }

    @Test
    @Transactional
    void getAllTransitionsByActorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where actorId is not null
        defaultTransitionFiltering("actorId.specified=true", "actorId.specified=false");
    }

    @Test
    @Transactional
    void getAllTransitionsByOccurredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where occurredAt equals to
        defaultTransitionFiltering("occurredAt.equals=" + DEFAULT_OCCURRED_AT, "occurredAt.equals=" + UPDATED_OCCURRED_AT);
    }

    @Test
    @Transactional
    void getAllTransitionsByOccurredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where occurredAt in
        defaultTransitionFiltering(
            "occurredAt.in=" + DEFAULT_OCCURRED_AT + "," + UPDATED_OCCURRED_AT,
            "occurredAt.in=" + UPDATED_OCCURRED_AT
        );
    }

    @Test
    @Transactional
    void getAllTransitionsByOccurredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where occurredAt is not null
        defaultTransitionFiltering("occurredAt.specified=true", "occurredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllTransitionsByJustificationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where justification equals to
        defaultTransitionFiltering("justification.equals=" + DEFAULT_JUSTIFICATION, "justification.equals=" + UPDATED_JUSTIFICATION);
    }

    @Test
    @Transactional
    void getAllTransitionsByJustificationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where justification in
        defaultTransitionFiltering(
            "justification.in=" + DEFAULT_JUSTIFICATION + "," + UPDATED_JUSTIFICATION,
            "justification.in=" + UPDATED_JUSTIFICATION
        );
    }

    @Test
    @Transactional
    void getAllTransitionsByJustificationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where justification is not null
        defaultTransitionFiltering("justification.specified=true", "justification.specified=false");
    }

    @Test
    @Transactional
    void getAllTransitionsByJustificationContainsSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where justification contains
        defaultTransitionFiltering("justification.contains=" + DEFAULT_JUSTIFICATION, "justification.contains=" + UPDATED_JUSTIFICATION);
    }

    @Test
    @Transactional
    void getAllTransitionsByJustificationNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where justification does not contain
        defaultTransitionFiltering(
            "justification.doesNotContain=" + UPDATED_JUSTIFICATION,
            "justification.doesNotContain=" + DEFAULT_JUSTIFICATION
        );
    }

    @Test
    @Transactional
    void getAllTransitionsByPreviousHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where previousHash equals to
        defaultTransitionFiltering("previousHash.equals=" + DEFAULT_PREVIOUS_HASH, "previousHash.equals=" + UPDATED_PREVIOUS_HASH);
    }

    @Test
    @Transactional
    void getAllTransitionsByPreviousHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where previousHash in
        defaultTransitionFiltering(
            "previousHash.in=" + DEFAULT_PREVIOUS_HASH + "," + UPDATED_PREVIOUS_HASH,
            "previousHash.in=" + UPDATED_PREVIOUS_HASH
        );
    }

    @Test
    @Transactional
    void getAllTransitionsByPreviousHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where previousHash is not null
        defaultTransitionFiltering("previousHash.specified=true", "previousHash.specified=false");
    }

    @Test
    @Transactional
    void getAllTransitionsByPreviousHashContainsSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where previousHash contains
        defaultTransitionFiltering("previousHash.contains=" + DEFAULT_PREVIOUS_HASH, "previousHash.contains=" + UPDATED_PREVIOUS_HASH);
    }

    @Test
    @Transactional
    void getAllTransitionsByPreviousHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where previousHash does not contain
        defaultTransitionFiltering(
            "previousHash.doesNotContain=" + UPDATED_PREVIOUS_HASH,
            "previousHash.doesNotContain=" + DEFAULT_PREVIOUS_HASH
        );
    }

    @Test
    @Transactional
    void getAllTransitionsByHashIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where hash equals to
        defaultTransitionFiltering("hash.equals=" + DEFAULT_HASH, "hash.equals=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllTransitionsByHashIsInShouldWork() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where hash in
        defaultTransitionFiltering("hash.in=" + DEFAULT_HASH + "," + UPDATED_HASH, "hash.in=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllTransitionsByHashIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where hash is not null
        defaultTransitionFiltering("hash.specified=true", "hash.specified=false");
    }

    @Test
    @Transactional
    void getAllTransitionsByHashContainsSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where hash contains
        defaultTransitionFiltering("hash.contains=" + DEFAULT_HASH, "hash.contains=" + UPDATED_HASH);
    }

    @Test
    @Transactional
    void getAllTransitionsByHashNotContainsSomething() throws Exception {
        // Initialize the database
        insertedTransition = transitionRepository.saveAndFlush(transition);

        // Get all the transitionList where hash does not contain
        defaultTransitionFiltering("hash.doesNotContain=" + UPDATED_HASH, "hash.doesNotContain=" + DEFAULT_HASH);
    }

    @Test
    @Transactional
    void getAllTransitionsByTicketIsEqualToSomething() throws Exception {
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            transitionRepository.saveAndFlush(transition);
            ticket = TicketResourceIT.createEntity();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        em.persist(ticket);
        em.flush();
        transition.setTicket(ticket);
        transitionRepository.saveAndFlush(transition);
        UUID ticketId = ticket.getId();
        // Get all the transitionList where ticket equals to ticketId
        defaultTransitionShouldBeFound("ticketId.equals=" + ticketId);

        // Get all the transitionList where ticket equals to UUID.randomUUID()
        defaultTransitionShouldNotBeFound("ticketId.equals=" + UUID.randomUUID());
    }

    private void defaultTransitionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultTransitionShouldBeFound(shouldBeFound);
        defaultTransitionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultTransitionShouldBeFound(String filter) throws Exception {
        restTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(transition.getId().toString())))
            .andExpect(jsonPath("$.[*].chainIndex").value(hasItem(DEFAULT_CHAIN_INDEX.intValue())))
            .andExpect(jsonPath("$.[*].fromState").value(hasItem(DEFAULT_FROM_STATE.toString())))
            .andExpect(jsonPath("$.[*].toState").value(hasItem(DEFAULT_TO_STATE.toString())))
            .andExpect(jsonPath("$.[*].actorId").value(hasItem(DEFAULT_ACTOR_ID.toString())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].justification").value(hasItem(DEFAULT_JUSTIFICATION)))
            .andExpect(jsonPath("$.[*].previousHash").value(hasItem(DEFAULT_PREVIOUS_HASH)))
            .andExpect(jsonPath("$.[*].hash").value(hasItem(DEFAULT_HASH)));

        // Check, that the count call also returns 1
        restTransitionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultTransitionShouldNotBeFound(String filter) throws Exception {
        restTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restTransitionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingTransition() throws Exception {
        // Get the transition
        restTransitionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return transitionRepository.count();
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

    protected Transition getPersistedTransition(Transition transition) {
        return transitionRepository.findById(transition.getId()).orElseThrow();
    }

    protected void assertPersistedTransitionToMatchAllProperties(Transition expectedTransition) {
        assertTransitionAllPropertiesEquals(expectedTransition, getPersistedTransition(expectedTransition));
    }

    protected void assertPersistedTransitionToMatchUpdatableProperties(Transition expectedTransition) {
        assertTransitionAllUpdatablePropertiesEquals(expectedTransition, getPersistedTransition(expectedTransition));
    }
}
