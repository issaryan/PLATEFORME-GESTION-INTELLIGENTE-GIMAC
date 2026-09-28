package cm.gimac.des.incident.web.rest;

import static cm.gimac.des.incident.domain.CrisisMemberAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.incident.IntegrationTest;
import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.domain.CrisisMember;
import cm.gimac.des.incident.repository.CrisisMemberRepository;
import cm.gimac.des.incident.service.CrisisMemberService;
import cm.gimac.des.incident.service.mapper.CrisisMemberMapper;
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
 * Integration tests for the {@link CrisisMemberResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CrisisMemberResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final String DEFAULT_ROLE = "AAAAAAAAAA";
    private static final String UPDATED_ROLE = "BBBBBBBBBB";

    private static final Integer DEFAULT_WEIGHT = 0;
    private static final Integer UPDATED_WEIGHT = 1;

    private static final UUID DEFAULT_SUBSTITUTE_FOR = UUID.randomUUID();
    private static final UUID UPDATED_SUBSTITUTE_FOR = UUID.randomUUID();

    private static final Instant DEFAULT_CONVENED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CONVENED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_JOINED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_JOINED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/crisis-members";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CrisisMemberRepository crisisMemberRepository;

    @Mock
    private CrisisMemberRepository crisisMemberRepositoryMock;

    @Autowired
    private CrisisMemberMapper crisisMemberMapper;

    @Mock
    private CrisisMemberService crisisMemberServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCrisisMemberMockMvc;

    private CrisisMember crisisMember;

    private CrisisMember insertedCrisisMember;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CrisisMember createEntity(EntityManager em) {
        CrisisMember crisisMember = new CrisisMember()
            .userId(DEFAULT_USER_ID)
            .role(DEFAULT_ROLE)
            .weight(DEFAULT_WEIGHT)
            .substituteFor(DEFAULT_SUBSTITUTE_FOR)
            .convenedAt(DEFAULT_CONVENED_AT)
            .joinedAt(DEFAULT_JOINED_AT);
        // Add required entity
        CrisisCell crisisCell;
        if (TestUtil.findAll(em, CrisisCell.class).isEmpty()) {
            crisisCell = CrisisCellResourceIT.createEntity(em);
            em.persist(crisisCell);
            em.flush();
        } else {
            crisisCell = TestUtil.findAll(em, CrisisCell.class).get(0);
        }
        crisisMember.setCell(crisisCell);
        return crisisMember;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CrisisMember createUpdatedEntity(EntityManager em) {
        CrisisMember updatedCrisisMember = new CrisisMember()
            .userId(UPDATED_USER_ID)
            .role(UPDATED_ROLE)
            .weight(UPDATED_WEIGHT)
            .substituteFor(UPDATED_SUBSTITUTE_FOR)
            .convenedAt(UPDATED_CONVENED_AT)
            .joinedAt(UPDATED_JOINED_AT);
        // Add required entity
        CrisisCell crisisCell;
        if (TestUtil.findAll(em, CrisisCell.class).isEmpty()) {
            crisisCell = CrisisCellResourceIT.createUpdatedEntity(em);
            em.persist(crisisCell);
            em.flush();
        } else {
            crisisCell = TestUtil.findAll(em, CrisisCell.class).get(0);
        }
        updatedCrisisMember.setCell(crisisCell);
        return updatedCrisisMember;
    }

    @BeforeEach
    void initTest() {
        crisisMember = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCrisisMember != null) {
            crisisMemberRepository.delete(insertedCrisisMember);
            insertedCrisisMember = null;
        }
    }

    @Test
    @Transactional
    void getAllCrisisMembers() throws Exception {
        // Initialize the database
        insertedCrisisMember = crisisMemberRepository.saveAndFlush(crisisMember);

        // Get all the crisisMemberList
        restCrisisMemberMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(crisisMember.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE)))
            .andExpect(jsonPath("$.[*].weight").value(hasItem(DEFAULT_WEIGHT)))
            .andExpect(jsonPath("$.[*].substituteFor").value(hasItem(DEFAULT_SUBSTITUTE_FOR.toString())))
            .andExpect(jsonPath("$.[*].convenedAt").value(hasItem(DEFAULT_CONVENED_AT.toString())))
            .andExpect(jsonPath("$.[*].joinedAt").value(hasItem(DEFAULT_JOINED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCrisisMembersWithEagerRelationshipsIsEnabled() throws Exception {
        when(crisisMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCrisisMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(crisisMemberServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCrisisMembersWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(crisisMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCrisisMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(crisisMemberRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCrisisMember() throws Exception {
        // Initialize the database
        insertedCrisisMember = crisisMemberRepository.saveAndFlush(crisisMember);

        // Get the crisisMember
        restCrisisMemberMockMvc
            .perform(get(ENTITY_API_URL_ID, crisisMember.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(crisisMember.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE))
            .andExpect(jsonPath("$.weight").value(DEFAULT_WEIGHT))
            .andExpect(jsonPath("$.substituteFor").value(DEFAULT_SUBSTITUTE_FOR.toString()))
            .andExpect(jsonPath("$.convenedAt").value(DEFAULT_CONVENED_AT.toString()))
            .andExpect(jsonPath("$.joinedAt").value(DEFAULT_JOINED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCrisisMember() throws Exception {
        // Get the crisisMember
        restCrisisMemberMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return crisisMemberRepository.count();
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

    protected CrisisMember getPersistedCrisisMember(CrisisMember crisisMember) {
        return crisisMemberRepository.findById(crisisMember.getId()).orElseThrow();
    }

    protected void assertPersistedCrisisMemberToMatchAllProperties(CrisisMember expectedCrisisMember) {
        assertCrisisMemberAllPropertiesEquals(expectedCrisisMember, getPersistedCrisisMember(expectedCrisisMember));
    }

    protected void assertPersistedCrisisMemberToMatchUpdatableProperties(CrisisMember expectedCrisisMember) {
        assertCrisisMemberAllUpdatablePropertiesEquals(expectedCrisisMember, getPersistedCrisisMember(expectedCrisisMember));
    }
}
