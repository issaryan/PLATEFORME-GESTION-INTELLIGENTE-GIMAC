package cm.gimac.des.rapprochement.web.rest;

import static cm.gimac.des.rapprochement.domain.AgingNoticeAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.rapprochement.IntegrationTest;
import cm.gimac.des.rapprochement.domain.AgingNotice;
import cm.gimac.des.rapprochement.domain.AgingRule;
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.repository.AgingNoticeRepository;
import cm.gimac.des.rapprochement.service.AgingNoticeService;
import cm.gimac.des.rapprochement.service.mapper.AgingNoticeMapper;
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
 * Integration tests for the {@link AgingNoticeResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class AgingNoticeResourceIT {

    private static final Instant DEFAULT_APPLIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_APPLIED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_AGE_IN_DAYS = 0;
    private static final Integer UPDATED_AGE_IN_DAYS = 1;

    private static final String ENTITY_API_URL = "/api/aging-notices";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AgingNoticeRepository agingNoticeRepository;

    @Mock
    private AgingNoticeRepository agingNoticeRepositoryMock;

    @Autowired
    private AgingNoticeMapper agingNoticeMapper;

    @Mock
    private AgingNoticeService agingNoticeServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAgingNoticeMockMvc;

    private AgingNotice agingNotice;

    private AgingNotice insertedAgingNotice;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AgingNotice createEntity(EntityManager em) {
        AgingNotice agingNotice = new AgingNotice().appliedAt(DEFAULT_APPLIED_AT).ageInDays(DEFAULT_AGE_IN_DAYS);
        // Add required entity
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspens = SuspensResourceIT.createEntity(em);
            em.persist(suspens);
            em.flush();
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        agingNotice.setSuspens(suspens);
        // Add required entity
        AgingRule agingRule;
        if (TestUtil.findAll(em, AgingRule.class).isEmpty()) {
            agingRule = AgingRuleResourceIT.createEntity();
            em.persist(agingRule);
            em.flush();
        } else {
            agingRule = TestUtil.findAll(em, AgingRule.class).get(0);
        }
        agingNotice.setRule(agingRule);
        return agingNotice;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AgingNotice createUpdatedEntity(EntityManager em) {
        AgingNotice updatedAgingNotice = new AgingNotice().appliedAt(UPDATED_APPLIED_AT).ageInDays(UPDATED_AGE_IN_DAYS);
        // Add required entity
        Suspens suspens;
        if (TestUtil.findAll(em, Suspens.class).isEmpty()) {
            suspens = SuspensResourceIT.createUpdatedEntity(em);
            em.persist(suspens);
            em.flush();
        } else {
            suspens = TestUtil.findAll(em, Suspens.class).get(0);
        }
        updatedAgingNotice.setSuspens(suspens);
        // Add required entity
        AgingRule agingRule;
        if (TestUtil.findAll(em, AgingRule.class).isEmpty()) {
            agingRule = AgingRuleResourceIT.createUpdatedEntity();
            em.persist(agingRule);
            em.flush();
        } else {
            agingRule = TestUtil.findAll(em, AgingRule.class).get(0);
        }
        updatedAgingNotice.setRule(agingRule);
        return updatedAgingNotice;
    }

    @BeforeEach
    void initTest() {
        agingNotice = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAgingNotice != null) {
            agingNoticeRepository.delete(insertedAgingNotice);
            insertedAgingNotice = null;
        }
    }

    @Test
    @Transactional
    void getAllAgingNotices() throws Exception {
        // Initialize the database
        insertedAgingNotice = agingNoticeRepository.saveAndFlush(agingNotice);

        // Get all the agingNoticeList
        restAgingNoticeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(agingNotice.getId().toString())))
            .andExpect(jsonPath("$.[*].appliedAt").value(hasItem(DEFAULT_APPLIED_AT.toString())))
            .andExpect(jsonPath("$.[*].ageInDays").value(hasItem(DEFAULT_AGE_IN_DAYS)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAgingNoticesWithEagerRelationshipsIsEnabled() throws Exception {
        when(agingNoticeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAgingNoticeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(agingNoticeServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAgingNoticesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(agingNoticeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAgingNoticeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(agingNoticeRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAgingNotice() throws Exception {
        // Initialize the database
        insertedAgingNotice = agingNoticeRepository.saveAndFlush(agingNotice);

        // Get the agingNotice
        restAgingNoticeMockMvc
            .perform(get(ENTITY_API_URL_ID, agingNotice.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(agingNotice.getId().toString()))
            .andExpect(jsonPath("$.appliedAt").value(DEFAULT_APPLIED_AT.toString()))
            .andExpect(jsonPath("$.ageInDays").value(DEFAULT_AGE_IN_DAYS));
    }

    @Test
    @Transactional
    void getNonExistingAgingNotice() throws Exception {
        // Get the agingNotice
        restAgingNoticeMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return agingNoticeRepository.count();
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

    protected AgingNotice getPersistedAgingNotice(AgingNotice agingNotice) {
        return agingNoticeRepository.findById(agingNotice.getId()).orElseThrow();
    }

    protected void assertPersistedAgingNoticeToMatchAllProperties(AgingNotice expectedAgingNotice) {
        assertAgingNoticeAllPropertiesEquals(expectedAgingNotice, getPersistedAgingNotice(expectedAgingNotice));
    }

    protected void assertPersistedAgingNoticeToMatchUpdatableProperties(AgingNotice expectedAgingNotice) {
        assertAgingNoticeAllUpdatablePropertiesEquals(expectedAgingNotice, getPersistedAgingNotice(expectedAgingNotice));
    }
}
