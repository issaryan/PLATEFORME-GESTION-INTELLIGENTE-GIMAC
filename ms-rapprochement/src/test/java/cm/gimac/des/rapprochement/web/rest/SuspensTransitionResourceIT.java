package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.SuspensTransitionAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.domain.SuspensTransition;
import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import cm.gimac.des.rapprochement.domain.enumeration.SuspensState;
import cm.gimac.des.rapprochement.repository.SuspensTransitionRepository;
import cm.gimac.des.rapprochement.service.SuspensTransitionService;
import cm.gimac.des.rapprochement.service.mapper.SuspensTransitionMapper;
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
 * Integration tests for the {@link SuspensTransitionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class SuspensTransitionResourceIT {

    private static final SuspensState DEFAULT_FROM_STATE = SuspensState.OUVERT;
    private static final SuspensState UPDATED_FROM_STATE = SuspensState.AFFECTE;

    private static final SuspensState DEFAULT_TO_STATE = SuspensState.OUVERT;
    private static final SuspensState UPDATED_TO_STATE = SuspensState.AFFECTE;

    private static final UUID DEFAULT_ACTOR_ID = UUID.randomUUID();
    private static final UUID UPDATED_ACTOR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_OCCURRED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OCCURRED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_JUSTIFICATION = "AAAAAAAAAA";
    private static final String UPDATED_JUSTIFICATION = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/suspens-transitions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private SuspensTransitionRepository suspensTransitionRepository;

    @Mock
    private SuspensTransitionRepository suspensTransitionRepositoryMock;

    @Autowired
    private SuspensTransitionMapper suspensTransitionMapper;

    @Mock
    private SuspensTransitionService suspensTransitionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restSuspensTransitionMockMvc;

    private SuspensTransition suspensTransition;

    private SuspensTransition insertedSuspensTransition;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SuspensTransition createEntity(EntityManager em) {
        SuspensTransition suspensTransition = new SuspensTransition()
            .fromState(DEFAULT_FROM_STATE)
            .toState(DEFAULT_TO_STATE)
            .actorId(DEFAULT_ACTOR_ID)
            .occurredAt(DEFAULT_OCCURRED_AT)
            .justification(DEFAULT_JUSTIFICATION);
        // Add required entity
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspens = SuspensResourceIT.createEntity(em);
            em.persist(suspens);
            em.flush();
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        suspensTransition.setSuspens(suspens);
        return suspensTransition;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SuspensTransition createUpdatedEntity(EntityManager em) {
        SuspensTransition updatedSuspensTransition = new SuspensTransition()
            .fromState(UPDATED_FROM_STATE)
            .toState(UPDATED_TO_STATE)
            .actorId(UPDATED_ACTOR_ID)
            .occurredAt(UPDATED_OCCURRED_AT)
            .justification(UPDATED_JUSTIFICATION);
        // Add required entity
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspens = SuspensResourceIT.createUpdatedEntity(em);
            em.persist(suspens);
            em.flush();
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        updatedSuspensTransition.setSuspens(suspens);
        return updatedSuspensTransition;
    }

    @BeforeEach
    void initTest() {
        suspensTransition = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedSuspensTransition != null) {
            suspensTransitionRepository.delete(insertedSuspensTransition);
            insertedSuspensTransition = null;
        }
    }

    @Test
    @Transactional
    void getAllSuspensTransitions() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList
        restSuspensTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(suspensTransition.getId().toString())))
            .andExpect(jsonPath("$.[*].fromState").value(hasItem(DEFAULT_FROM_STATE.toString())))
            .andExpect(jsonPath("$.[*].toState").value(hasItem(DEFAULT_TO_STATE.toString())))
            .andExpect(jsonPath("$.[*].actorId").value(hasItem(DEFAULT_ACTOR_ID.toString())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].justification").value(hasItem(DEFAULT_JUSTIFICATION)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSuspensTransitionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(suspensTransitionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSuspensTransitionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(suspensTransitionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllSuspensTransitionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(suspensTransitionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restSuspensTransitionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(suspensTransitionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getSuspensTransition() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get the suspensTransition
        restSuspensTransitionMockMvc
            .perform(get(ENTITY_API_URL_ID, suspensTransition.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(suspensTransition.getId().toString()))
            .andExpect(jsonPath("$.fromState").value(DEFAULT_FROM_STATE.toString()))
            .andExpect(jsonPath("$.toState").value(DEFAULT_TO_STATE.toString()))
            .andExpect(jsonPath("$.actorId").value(DEFAULT_ACTOR_ID.toString()))
            .andExpect(jsonPath("$.occurredAt").value(DEFAULT_OCCURRED_AT.toString()))
            .andExpect(jsonPath("$.justification").value(DEFAULT_JUSTIFICATION));
    }

    @Test
    @Transactional
    void getSuspensTransitionsByIdFiltering() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        UUID id = suspensTransition.getId();

        defaultSuspensTransitionFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByFromStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where fromState equals to
        defaultSuspensTransitionFiltering("fromState.equals=" + DEFAULT_FROM_STATE, "fromState.equals=" + UPDATED_FROM_STATE);
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByFromStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where fromState in
        defaultSuspensTransitionFiltering(
            "fromState.in=" + DEFAULT_FROM_STATE + "," + UPDATED_FROM_STATE,
            "fromState.in=" + UPDATED_FROM_STATE
        );
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByFromStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where fromState is not null
        defaultSuspensTransitionFiltering("fromState.specified=true", "fromState.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByToStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where toState equals to
        defaultSuspensTransitionFiltering("toState.equals=" + DEFAULT_TO_STATE, "toState.equals=" + UPDATED_TO_STATE);
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByToStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where toState in
        defaultSuspensTransitionFiltering("toState.in=" + DEFAULT_TO_STATE + "," + UPDATED_TO_STATE, "toState.in=" + UPDATED_TO_STATE);
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByToStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where toState is not null
        defaultSuspensTransitionFiltering("toState.specified=true", "toState.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByActorIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where actorId equals to
        defaultSuspensTransitionFiltering("actorId.equals=" + DEFAULT_ACTOR_ID, "actorId.equals=" + UPDATED_ACTOR_ID);
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByActorIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where actorId in
        defaultSuspensTransitionFiltering("actorId.in=" + DEFAULT_ACTOR_ID + "," + UPDATED_ACTOR_ID, "actorId.in=" + UPDATED_ACTOR_ID);
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByActorIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where actorId is not null
        defaultSuspensTransitionFiltering("actorId.specified=true", "actorId.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByOccurredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where occurredAt equals to
        defaultSuspensTransitionFiltering("occurredAt.equals=" + DEFAULT_OCCURRED_AT, "occurredAt.equals=" + UPDATED_OCCURRED_AT);
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByOccurredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where occurredAt in
        defaultSuspensTransitionFiltering(
            "occurredAt.in=" + DEFAULT_OCCURRED_AT + "," + UPDATED_OCCURRED_AT,
            "occurredAt.in=" + UPDATED_OCCURRED_AT
        );
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByOccurredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where occurredAt is not null
        defaultSuspensTransitionFiltering("occurredAt.specified=true", "occurredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByJustificationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where justification equals to
        defaultSuspensTransitionFiltering("justification.equals=" + DEFAULT_JUSTIFICATION, "justification.equals=" + UPDATED_JUSTIFICATION);
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByJustificationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where justification in
        defaultSuspensTransitionFiltering(
            "justification.in=" + DEFAULT_JUSTIFICATION + "," + UPDATED_JUSTIFICATION,
            "justification.in=" + UPDATED_JUSTIFICATION
        );
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByJustificationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where justification is not null
        defaultSuspensTransitionFiltering("justification.specified=true", "justification.specified=false");
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByJustificationContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where justification contains
        defaultSuspensTransitionFiltering(
            "justification.contains=" + DEFAULT_JUSTIFICATION,
            "justification.contains=" + UPDATED_JUSTIFICATION
        );
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsByJustificationNotContainsSomething() throws Exception {
        // Initialize the database
        insertedSuspensTransition = suspensTransitionRepository.saveAndFlush(suspensTransition);

        // Get all the suspensTransitionList where justification does not contain
        defaultSuspensTransitionFiltering(
            "justification.doesNotContain=" + UPDATED_JUSTIFICATION,
            "justification.doesNotContain=" + DEFAULT_JUSTIFICATION
        );
    }

    @Test
    @Transactional
    void getAllSuspensTransitionsBySuspensIsEqualToSomething() throws Exception {
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspensTransitionRepository.saveAndFlush(suspensTransition);
            suspens = SuspensResourceIT.createEntity(em);
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        em.persist(suspens);
        em.flush();
        suspensTransition.setSuspens(suspens);
        suspensTransitionRepository.saveAndFlush(suspensTransition);
        UUID suspensId = suspens.getId();
        // Get all the suspensTransitionList where suspens equals to suspensId
        defaultSuspensTransitionShouldBeFound("suspensId.equals=" + suspensId);

        // Get all the suspensTransitionList where suspens equals to UUID.randomUUID()
        defaultSuspensTransitionShouldNotBeFound("suspensId.equals=" + UUID.randomUUID());
    }

    private void defaultSuspensTransitionFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultSuspensTransitionShouldBeFound(shouldBeFound);
        defaultSuspensTransitionShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultSuspensTransitionShouldBeFound(String filter) throws Exception {
        restSuspensTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(suspensTransition.getId().toString())))
            .andExpect(jsonPath("$.[*].fromState").value(hasItem(DEFAULT_FROM_STATE.toString())))
            .andExpect(jsonPath("$.[*].toState").value(hasItem(DEFAULT_TO_STATE.toString())))
            .andExpect(jsonPath("$.[*].actorId").value(hasItem(DEFAULT_ACTOR_ID.toString())))
            .andExpect(jsonPath("$.[*].occurredAt").value(hasItem(DEFAULT_OCCURRED_AT.toString())))
            .andExpect(jsonPath("$.[*].justification").value(hasItem(DEFAULT_JUSTIFICATION)));

        // Check, that the count call also returns 1
        restSuspensTransitionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultSuspensTransitionShouldNotBeFound(String filter) throws Exception {
        restSuspensTransitionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restSuspensTransitionMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingSuspensTransition() throws Exception {
        // Get the suspensTransition
        restSuspensTransitionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return suspensTransitionRepository.count();
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

    protected SuspensTransition getPersistedSuspensTransition(SuspensTransition suspensTransition) {
        return suspensTransitionRepository.findById(suspensTransition.getId()).orElseThrow();
    }

    protected void assertPersistedSuspensTransitionToMatchAllProperties(SuspensTransition expectedSuspensTransition) {
        assertSuspensTransitionAllPropertiesEquals(expectedSuspensTransition, getPersistedSuspensTransition(expectedSuspensTransition));
    }

    protected void assertPersistedSuspensTransitionToMatchUpdatableProperties(SuspensTransition expectedSuspensTransition) {
        assertSuspensTransitionAllUpdatablePropertiesEquals(
            expectedSuspensTransition,
            getPersistedSuspensTransition(expectedSuspensTransition)
        );
    }
}
