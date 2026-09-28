package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.EngagementFreezeAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.EngagementFreeze;
import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.repository.EngagementFreezeRepository;
import cm.gimac.des.incident.service.EngagementFreezeService;
import cm.gimac.des.incident.service.mapper.EngagementFreezeMapper;
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
 * Integration tests for the {@link EngagementFreezeResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class EngagementFreezeResourceIT {

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final String DEFAULT_THIRD_PARTY = "AAAAAAAAAA";
    private static final String UPDATED_THIRD_PARTY = "BBBBBBBBBB";

    private static final Instant DEFAULT_STARTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_STARTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_ENDED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENDED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_PROOF_REF = "AAAAAAAAAA";
    private static final String UPDATED_PROOF_REF = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/engagement-freezes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EngagementFreezeRepository engagementFreezeRepository;

    @Mock
    private EngagementFreezeRepository engagementFreezeRepositoryMock;

    @Autowired
    private EngagementFreezeMapper engagementFreezeMapper;

    @Mock
    private EngagementFreezeService engagementFreezeServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEngagementFreezeMockMvc;

    private EngagementFreeze engagementFreeze;

    private EngagementFreeze insertedEngagementFreeze;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EngagementFreeze createEntity(EntityManager em) {
        EngagementFreeze engagementFreeze = new EngagementFreeze()
            .reason(DEFAULT_REASON)
            .thirdParty(DEFAULT_THIRD_PARTY)
            .startedAt(DEFAULT_STARTED_AT)
            .endedAt(DEFAULT_ENDED_AT)
            .proofRef(DEFAULT_PROOF_REF);
        // Add required entity
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            ticket = TicketResourceIT.createEntity();
            em.persist(ticket);
            em.flush();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        engagementFreeze.setTicket(ticket);
        return engagementFreeze;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EngagementFreeze createUpdatedEntity(EntityManager em) {
        EngagementFreeze updatedEngagementFreeze = new EngagementFreeze()
            .reason(UPDATED_REASON)
            .thirdParty(UPDATED_THIRD_PARTY)
            .startedAt(UPDATED_STARTED_AT)
            .endedAt(UPDATED_ENDED_AT)
            .proofRef(UPDATED_PROOF_REF);
        // Add required entity
        Ticket ticket;
        if (TestUtil.findAll(em, Ticket.class).isEmpty()) {
            ticket = TicketResourceIT.createUpdatedEntity();
            em.persist(ticket);
            em.flush();
        } else {
            ticket = TestUtil.findAll(em, Ticket.class).get(0);
        }
        updatedEngagementFreeze.setTicket(ticket);
        return updatedEngagementFreeze;
    }

    @BeforeEach
    void initTest() {
        engagementFreeze = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEngagementFreeze != null) {
            engagementFreezeRepository.delete(insertedEngagementFreeze);
            insertedEngagementFreeze = null;
        }
    }

    @Test
    @Transactional
    void getAllEngagementFreezes() throws Exception {
        // Initialize the database
        insertedEngagementFreeze = engagementFreezeRepository.saveAndFlush(engagementFreeze);

        // Get all the engagementFreezeList
        restEngagementFreezeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(engagementFreeze.getId().toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].thirdParty").value(hasItem(DEFAULT_THIRD_PARTY)))
            .andExpect(jsonPath("$.[*].startedAt").value(hasItem(DEFAULT_STARTED_AT.toString())))
            .andExpect(jsonPath("$.[*].endedAt").value(hasItem(DEFAULT_ENDED_AT.toString())))
            .andExpect(jsonPath("$.[*].proofRef").value(hasItem(DEFAULT_PROOF_REF)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEngagementFreezesWithEagerRelationshipsIsEnabled() throws Exception {
        when(engagementFreezeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEngagementFreezeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(engagementFreezeServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllEngagementFreezesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(engagementFreezeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restEngagementFreezeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(engagementFreezeRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getEngagementFreeze() throws Exception {
        // Initialize the database
        insertedEngagementFreeze = engagementFreezeRepository.saveAndFlush(engagementFreeze);

        // Get the engagementFreeze
        restEngagementFreezeMockMvc
            .perform(get(ENTITY_API_URL_ID, engagementFreeze.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(engagementFreeze.getId().toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.thirdParty").value(DEFAULT_THIRD_PARTY))
            .andExpect(jsonPath("$.startedAt").value(DEFAULT_STARTED_AT.toString()))
            .andExpect(jsonPath("$.endedAt").value(DEFAULT_ENDED_AT.toString()))
            .andExpect(jsonPath("$.proofRef").value(DEFAULT_PROOF_REF));
    }

    @Test
    @Transactional
    void getNonExistingEngagementFreeze() throws Exception {
        // Get the engagementFreeze
        restEngagementFreezeMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return engagementFreezeRepository.count();
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

    protected EngagementFreeze getPersistedEngagementFreeze(EngagementFreeze engagementFreeze) {
        return engagementFreezeRepository.findById(engagementFreeze.getId()).orElseThrow();
    }

    protected void assertPersistedEngagementFreezeToMatchAllProperties(EngagementFreeze expectedEngagementFreeze) {
        assertEngagementFreezeAllPropertiesEquals(expectedEngagementFreeze, getPersistedEngagementFreeze(expectedEngagementFreeze));
    }

    protected void assertPersistedEngagementFreezeToMatchUpdatableProperties(EngagementFreeze expectedEngagementFreeze) {
        assertEngagementFreezeAllUpdatablePropertiesEquals(
            expectedEngagementFreeze,
            getPersistedEngagementFreeze(expectedEngagementFreeze)
        );
    }
}
