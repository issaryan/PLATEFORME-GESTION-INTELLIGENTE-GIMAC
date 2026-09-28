package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.CampaignSyncAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.CampaignSync;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.CampaignSyncRepository;
import cm.gimac.des.homologation.service.CampaignSyncService;
import cm.gimac.des.homologation.service.mapper.CampaignSyncMapper;
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
 * Integration tests for the {@link CampaignSyncResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CampaignSyncResourceIT {

    private static final Instant DEFAULT_CURSOR = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CURSOR = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_POLLED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_POLLED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_SUCCESS_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_SUCCESS_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_CONSECUTIVE_FAILURES = 0;
    private static final Integer UPDATED_CONSECUTIVE_FAILURES = 1;

    private static final Instant DEFAULT_CIRCUIT_OPEN_UNTIL = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CIRCUIT_OPEN_UNTIL = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/campaign-syncs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CampaignSyncRepository campaignSyncRepository;

    @Mock
    private CampaignSyncRepository campaignSyncRepositoryMock;

    @Autowired
    private CampaignSyncMapper campaignSyncMapper;

    @Mock
    private CampaignSyncService campaignSyncServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCampaignSyncMockMvc;

    private CampaignSync campaignSync;

    private CampaignSync insertedCampaignSync;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CampaignSync createEntity(EntityManager em) {
        CampaignSync campaignSync = new CampaignSync()
            .cursor(DEFAULT_CURSOR)
            .lastPolledAt(DEFAULT_LAST_POLLED_AT)
            .lastSuccessAt(DEFAULT_LAST_SUCCESS_AT)
            .consecutiveFailures(DEFAULT_CONSECUTIVE_FAILURES)
            .circuitOpenUntil(DEFAULT_CIRCUIT_OPEN_UNTIL);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        campaignSync.setLiaison(liaison);
        return campaignSync;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CampaignSync createUpdatedEntity(EntityManager em) {
        CampaignSync updatedCampaignSync = new CampaignSync()
            .cursor(UPDATED_CURSOR)
            .lastPolledAt(UPDATED_LAST_POLLED_AT)
            .lastSuccessAt(UPDATED_LAST_SUCCESS_AT)
            .consecutiveFailures(UPDATED_CONSECUTIVE_FAILURES)
            .circuitOpenUntil(UPDATED_CIRCUIT_OPEN_UNTIL);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        updatedCampaignSync.setLiaison(liaison);
        return updatedCampaignSync;
    }

    @BeforeEach
    void initTest() {
        campaignSync = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCampaignSync != null) {
            campaignSyncRepository.delete(insertedCampaignSync);
            insertedCampaignSync = null;
        }
    }

    @Test
    @Transactional
    void getAllCampaignSyncs() throws Exception {
        // Initialize the database
        insertedCampaignSync = campaignSyncRepository.saveAndFlush(campaignSync);

        // Get all the campaignSyncList
        restCampaignSyncMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(campaignSync.getId().toString())))
            .andExpect(jsonPath("$.[*].cursor").value(hasItem(DEFAULT_CURSOR.toString())))
            .andExpect(jsonPath("$.[*].lastPolledAt").value(hasItem(DEFAULT_LAST_POLLED_AT.toString())))
            .andExpect(jsonPath("$.[*].lastSuccessAt").value(hasItem(DEFAULT_LAST_SUCCESS_AT.toString())))
            .andExpect(jsonPath("$.[*].consecutiveFailures").value(hasItem(DEFAULT_CONSECUTIVE_FAILURES)))
            .andExpect(jsonPath("$.[*].circuitOpenUntil").value(hasItem(DEFAULT_CIRCUIT_OPEN_UNTIL.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCampaignSyncsWithEagerRelationshipsIsEnabled() throws Exception {
        when(campaignSyncServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCampaignSyncMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(campaignSyncServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCampaignSyncsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(campaignSyncServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCampaignSyncMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(campaignSyncRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCampaignSync() throws Exception {
        // Initialize the database
        insertedCampaignSync = campaignSyncRepository.saveAndFlush(campaignSync);

        // Get the campaignSync
        restCampaignSyncMockMvc
            .perform(get(ENTITY_API_URL_ID, campaignSync.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(campaignSync.getId().toString()))
            .andExpect(jsonPath("$.cursor").value(DEFAULT_CURSOR.toString()))
            .andExpect(jsonPath("$.lastPolledAt").value(DEFAULT_LAST_POLLED_AT.toString()))
            .andExpect(jsonPath("$.lastSuccessAt").value(DEFAULT_LAST_SUCCESS_AT.toString()))
            .andExpect(jsonPath("$.consecutiveFailures").value(DEFAULT_CONSECUTIVE_FAILURES))
            .andExpect(jsonPath("$.circuitOpenUntil").value(DEFAULT_CIRCUIT_OPEN_UNTIL.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCampaignSync() throws Exception {
        // Get the campaignSync
        restCampaignSyncMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return campaignSyncRepository.count();
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

    protected CampaignSync getPersistedCampaignSync(CampaignSync campaignSync) {
        return campaignSyncRepository.findById(campaignSync.getId()).orElseThrow();
    }

    protected void assertPersistedCampaignSyncToMatchAllProperties(CampaignSync expectedCampaignSync) {
        assertCampaignSyncAllPropertiesEquals(expectedCampaignSync, getPersistedCampaignSync(expectedCampaignSync));
    }

    protected void assertPersistedCampaignSyncToMatchUpdatableProperties(CampaignSync expectedCampaignSync) {
        assertCampaignSyncAllUpdatablePropertiesEquals(expectedCampaignSync, getPersistedCampaignSync(expectedCampaignSync));
    }
}
