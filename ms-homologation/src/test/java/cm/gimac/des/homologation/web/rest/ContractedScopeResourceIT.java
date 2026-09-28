package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ContractedScopeAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ContractedScope;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.ScopeProgress;
import cm.gimac.des.homologation.repository.ContractedScopeRepository;
import cm.gimac.des.homologation.service.ContractedScopeService;
import cm.gimac.des.homologation.service.mapper.ContractedScopeMapper;
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
 * Integration tests for the {@link ContractedScopeResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ContractedScopeResourceIT {

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final Boolean DEFAULT_INCLUDED = false;
    private static final Boolean UPDATED_INCLUDED = true;

    private static final ScopeProgress DEFAULT_PROGRESS = ScopeProgress.NON_DEMARRE;
    private static final ScopeProgress UPDATED_PROGRESS = ScopeProgress.EN_ESSAI;

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/contracted-scopes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ContractedScopeRepository contractedScopeRepository;

    @Mock
    private ContractedScopeRepository contractedScopeRepositoryMock;

    @Autowired
    private ContractedScopeMapper contractedScopeMapper;

    @Mock
    private ContractedScopeService contractedScopeServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restContractedScopeMockMvc;

    private ContractedScope contractedScope;

    private ContractedScope insertedContractedScope;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ContractedScope createEntity(EntityManager em) {
        ContractedScope contractedScope = new ContractedScope()
            .serviceCode(DEFAULT_SERVICE_CODE)
            .included(DEFAULT_INCLUDED)
            .progress(DEFAULT_PROGRESS)
            .updatedAt(DEFAULT_UPDATED_AT);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        contractedScope.setLiaison(liaison);
        return contractedScope;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ContractedScope createUpdatedEntity(EntityManager em) {
        ContractedScope updatedContractedScope = new ContractedScope()
            .serviceCode(UPDATED_SERVICE_CODE)
            .included(UPDATED_INCLUDED)
            .progress(UPDATED_PROGRESS)
            .updatedAt(UPDATED_UPDATED_AT);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        updatedContractedScope.setLiaison(liaison);
        return updatedContractedScope;
    }

    @BeforeEach
    void initTest() {
        contractedScope = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedContractedScope != null) {
            contractedScopeRepository.delete(insertedContractedScope);
            insertedContractedScope = null;
        }
    }

    @Test
    @Transactional
    void getAllContractedScopes() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList
        restContractedScopeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(contractedScope.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].included").value(hasItem(DEFAULT_INCLUDED)))
            .andExpect(jsonPath("$.[*].progress").value(hasItem(DEFAULT_PROGRESS.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllContractedScopesWithEagerRelationshipsIsEnabled() throws Exception {
        when(contractedScopeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restContractedScopeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(contractedScopeServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllContractedScopesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(contractedScopeServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restContractedScopeMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(contractedScopeRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getContractedScope() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get the contractedScope
        restContractedScopeMockMvc
            .perform(get(ENTITY_API_URL_ID, contractedScope.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(contractedScope.getId().toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.included").value(DEFAULT_INCLUDED))
            .andExpect(jsonPath("$.progress").value(DEFAULT_PROGRESS.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getContractedScopesByIdFiltering() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        UUID id = contractedScope.getId();

        defaultContractedScopeFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllContractedScopesByServiceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where serviceCode equals to
        defaultContractedScopeFiltering("serviceCode.equals=" + DEFAULT_SERVICE_CODE, "serviceCode.equals=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllContractedScopesByServiceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where serviceCode in
        defaultContractedScopeFiltering(
            "serviceCode.in=" + DEFAULT_SERVICE_CODE + "," + UPDATED_SERVICE_CODE,
            "serviceCode.in=" + UPDATED_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllContractedScopesByServiceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where serviceCode is not null
        defaultContractedScopeFiltering("serviceCode.specified=true", "serviceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllContractedScopesByServiceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where serviceCode contains
        defaultContractedScopeFiltering("serviceCode.contains=" + DEFAULT_SERVICE_CODE, "serviceCode.contains=" + UPDATED_SERVICE_CODE);
    }

    @Test
    @Transactional
    void getAllContractedScopesByServiceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where serviceCode does not contain
        defaultContractedScopeFiltering(
            "serviceCode.doesNotContain=" + UPDATED_SERVICE_CODE,
            "serviceCode.doesNotContain=" + DEFAULT_SERVICE_CODE
        );
    }

    @Test
    @Transactional
    void getAllContractedScopesByIncludedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where included equals to
        defaultContractedScopeFiltering("included.equals=" + DEFAULT_INCLUDED, "included.equals=" + UPDATED_INCLUDED);
    }

    @Test
    @Transactional
    void getAllContractedScopesByIncludedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where included in
        defaultContractedScopeFiltering("included.in=" + DEFAULT_INCLUDED + "," + UPDATED_INCLUDED, "included.in=" + UPDATED_INCLUDED);
    }

    @Test
    @Transactional
    void getAllContractedScopesByIncludedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where included is not null
        defaultContractedScopeFiltering("included.specified=true", "included.specified=false");
    }

    @Test
    @Transactional
    void getAllContractedScopesByProgressIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where progress equals to
        defaultContractedScopeFiltering("progress.equals=" + DEFAULT_PROGRESS, "progress.equals=" + UPDATED_PROGRESS);
    }

    @Test
    @Transactional
    void getAllContractedScopesByProgressIsInShouldWork() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where progress in
        defaultContractedScopeFiltering("progress.in=" + DEFAULT_PROGRESS + "," + UPDATED_PROGRESS, "progress.in=" + UPDATED_PROGRESS);
    }

    @Test
    @Transactional
    void getAllContractedScopesByProgressIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where progress is not null
        defaultContractedScopeFiltering("progress.specified=true", "progress.specified=false");
    }

    @Test
    @Transactional
    void getAllContractedScopesByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where updatedAt equals to
        defaultContractedScopeFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllContractedScopesByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where updatedAt in
        defaultContractedScopeFiltering(
            "updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT,
            "updatedAt.in=" + UPDATED_UPDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllContractedScopesByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedContractedScope = contractedScopeRepository.saveAndFlush(contractedScope);

        // Get all the contractedScopeList where updatedAt is not null
        defaultContractedScopeFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllContractedScopesByLiaisonIsEqualToSomething() throws Exception {
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            contractedScopeRepository.saveAndFlush(contractedScope);
            liaison = LiaisonResourceIT.createEntity(em);
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).get(0);
        }
        em.persist(liaison);
        em.flush();
        contractedScope.setLiaison(liaison);
        contractedScopeRepository.saveAndFlush(contractedScope);
        UUID liaisonId = liaison.getId();
        // Get all the contractedScopeList where liaison equals to liaisonId
        defaultContractedScopeShouldBeFound("liaisonId.equals=" + liaisonId);

        // Get all the contractedScopeList where liaison equals to UUID.randomUUID()
        defaultContractedScopeShouldNotBeFound("liaisonId.equals=" + UUID.randomUUID());
    }

    private void defaultContractedScopeFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultContractedScopeShouldBeFound(shouldBeFound);
        defaultContractedScopeShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultContractedScopeShouldBeFound(String filter) throws Exception {
        restContractedScopeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(contractedScope.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].included").value(hasItem(DEFAULT_INCLUDED)))
            .andExpect(jsonPath("$.[*].progress").value(hasItem(DEFAULT_PROGRESS.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restContractedScopeMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultContractedScopeShouldNotBeFound(String filter) throws Exception {
        restContractedScopeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restContractedScopeMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingContractedScope() throws Exception {
        // Get the contractedScope
        restContractedScopeMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return contractedScopeRepository.count();
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

    protected ContractedScope getPersistedContractedScope(ContractedScope contractedScope) {
        return contractedScopeRepository.findById(contractedScope.getId()).orElseThrow();
    }

    protected void assertPersistedContractedScopeToMatchAllProperties(ContractedScope expectedContractedScope) {
        assertContractedScopeAllPropertiesEquals(expectedContractedScope, getPersistedContractedScope(expectedContractedScope));
    }

    protected void assertPersistedContractedScopeToMatchUpdatableProperties(ContractedScope expectedContractedScope) {
        assertContractedScopeAllUpdatablePropertiesEquals(expectedContractedScope, getPersistedContractedScope(expectedContractedScope));
    }
}
