package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.CrisisDecisionAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.domain.CrisisDecision;
import cm.gimac.des.incident.repository.CrisisDecisionRepository;
import cm.gimac.des.incident.service.CrisisDecisionService;
import cm.gimac.des.incident.service.mapper.CrisisDecisionMapper;
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
 * Integration tests for the {@link CrisisDecisionResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CrisisDecisionResourceIT {

    private static final String DEFAULT_STATEMENT = "AAAAAAAAAA";
    private static final String UPDATED_STATEMENT = "BBBBBBBBBB";

    private static final UUID DEFAULT_DECIDED_BY = UUID.randomUUID();
    private static final UUID UPDATED_DECIDED_BY = UUID.randomUUID();

    private static final Instant DEFAULT_DECIDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DECIDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_TASK_REF = "AAAAAAAAAA";
    private static final String UPDATED_TASK_REF = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/crisis-decisions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CrisisDecisionRepository crisisDecisionRepository;

    @Mock
    private CrisisDecisionRepository crisisDecisionRepositoryMock;

    @Autowired
    private CrisisDecisionMapper crisisDecisionMapper;

    @Mock
    private CrisisDecisionService crisisDecisionServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCrisisDecisionMockMvc;

    private CrisisDecision crisisDecision;

    private CrisisDecision insertedCrisisDecision;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CrisisDecision createEntity(EntityManager em) {
        CrisisDecision crisisDecision = new CrisisDecision()
            .statement(DEFAULT_STATEMENT)
            .decidedBy(DEFAULT_DECIDED_BY)
            .decidedAt(DEFAULT_DECIDED_AT)
            .taskRef(DEFAULT_TASK_REF);
        // Add required entity
        CrisisCell crisisCell;
        if (TestUtil.findAll(em, CrisisCell.class).isEmpty()) {
            crisisCell = CrisisCellResourceIT.createEntity(em);
            em.persist(crisisCell);
            em.flush();
        } else {
            crisisCell = TestUtil.findAll(em, CrisisCell.class).get(0);
        }
        crisisDecision.setCell(crisisCell);
        return crisisDecision;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CrisisDecision createUpdatedEntity(EntityManager em) {
        CrisisDecision updatedCrisisDecision = new CrisisDecision()
            .statement(UPDATED_STATEMENT)
            .decidedBy(UPDATED_DECIDED_BY)
            .decidedAt(UPDATED_DECIDED_AT)
            .taskRef(UPDATED_TASK_REF);
        // Add required entity
        CrisisCell crisisCell;
        if (TestUtil.findAll(em, CrisisCell.class).isEmpty()) {
            crisisCell = CrisisCellResourceIT.createUpdatedEntity(em);
            em.persist(crisisCell);
            em.flush();
        } else {
            crisisCell = TestUtil.findAll(em, CrisisCell.class).get(0);
        }
        updatedCrisisDecision.setCell(crisisCell);
        return updatedCrisisDecision;
    }

    @BeforeEach
    void initTest() {
        crisisDecision = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCrisisDecision != null) {
            crisisDecisionRepository.delete(insertedCrisisDecision);
            insertedCrisisDecision = null;
        }
    }

    @Test
    @Transactional
    void getAllCrisisDecisions() throws Exception {
        // Initialize the database
        insertedCrisisDecision = crisisDecisionRepository.saveAndFlush(crisisDecision);

        // Get all the crisisDecisionList
        restCrisisDecisionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(crisisDecision.getId().toString())))
            .andExpect(jsonPath("$.[*].statement").value(hasItem(DEFAULT_STATEMENT)))
            .andExpect(jsonPath("$.[*].decidedBy").value(hasItem(DEFAULT_DECIDED_BY.toString())))
            .andExpect(jsonPath("$.[*].decidedAt").value(hasItem(DEFAULT_DECIDED_AT.toString())))
            .andExpect(jsonPath("$.[*].taskRef").value(hasItem(DEFAULT_TASK_REF)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCrisisDecisionsWithEagerRelationshipsIsEnabled() throws Exception {
        when(crisisDecisionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCrisisDecisionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(crisisDecisionServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCrisisDecisionsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(crisisDecisionServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCrisisDecisionMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(crisisDecisionRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCrisisDecision() throws Exception {
        // Initialize the database
        insertedCrisisDecision = crisisDecisionRepository.saveAndFlush(crisisDecision);

        // Get the crisisDecision
        restCrisisDecisionMockMvc
            .perform(get(ENTITY_API_URL_ID, crisisDecision.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(crisisDecision.getId().toString()))
            .andExpect(jsonPath("$.statement").value(DEFAULT_STATEMENT))
            .andExpect(jsonPath("$.decidedBy").value(DEFAULT_DECIDED_BY.toString()))
            .andExpect(jsonPath("$.decidedAt").value(DEFAULT_DECIDED_AT.toString()))
            .andExpect(jsonPath("$.taskRef").value(DEFAULT_TASK_REF));
    }

    @Test
    @Transactional
    void getNonExistingCrisisDecision() throws Exception {
        // Get the crisisDecision
        restCrisisDecisionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return crisisDecisionRepository.count();
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

    protected CrisisDecision getPersistedCrisisDecision(CrisisDecision crisisDecision) {
        return crisisDecisionRepository.findById(crisisDecision.getId()).orElseThrow();
    }

    protected void assertPersistedCrisisDecisionToMatchAllProperties(CrisisDecision expectedCrisisDecision) {
        assertCrisisDecisionAllPropertiesEquals(expectedCrisisDecision, getPersistedCrisisDecision(expectedCrisisDecision));
    }

    protected void assertPersistedCrisisDecisionToMatchUpdatableProperties(CrisisDecision expectedCrisisDecision) {
        assertCrisisDecisionAllUpdatablePropertiesEquals(expectedCrisisDecision, getPersistedCrisisDecision(expectedCrisisDecision));
    }
}
