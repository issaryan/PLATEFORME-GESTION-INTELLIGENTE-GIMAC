package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ContractualScopeAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.service.dto.ContractualScopeDTO;
import cm.gimac.des.homologation.service.mapper.ContractualScopeMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link ContractualScopeResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.homologation.security.AuthoritiesConstants.ADMIN)
class ContractualScopeResourceIT {

    private static final Instant DEFAULT_DATE_DERIVATION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_DERIVATION = Instant.ofEpochMilli(1701958935371L);

    private static final String DEFAULT_SOURCE_EVENT_ID = "AAAAAAAAAA";
    private static final String UPDATED_SOURCE_EVENT_ID = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/contractual-scopes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ContractualScopeRepository contractualScopeRepository;

    @Autowired
    private ContractualScopeMapper contractualScopeMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restContractualScopeMockMvc;

    private ContractualScope contractualScope;

    private ContractualScope insertedContractualScope;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ContractualScope createEntity(EntityManager em) {
        ContractualScope contractualScope = new ContractualScope()
            .dateDerivation(DEFAULT_DATE_DERIVATION)
            .sourceEventId(DEFAULT_SOURCE_EVENT_ID);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        contractualScope.setLiaison(liaison);
        return contractualScope;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ContractualScope createUpdatedEntity(EntityManager em) {
        ContractualScope updatedContractualScope = new ContractualScope()
            .dateDerivation(UPDATED_DATE_DERIVATION)
            .sourceEventId(UPDATED_SOURCE_EVENT_ID);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        updatedContractualScope.setLiaison(liaison);
        return updatedContractualScope;
    }

    @BeforeEach
    void initTest() {
        contractualScope = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedContractualScope != null) {
            contractualScopeRepository.delete(insertedContractualScope);
            insertedContractualScope = null;
        }
    }

    @Test
    @Transactional
    void createContractualScope() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ContractualScope
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);
        var returnedContractualScopeDTO = om.readValue(
            restContractualScopeMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(contractualScopeDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ContractualScopeDTO.class
        );

        // Validate the ContractualScope in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedContractualScope = contractualScopeMapper.toEntity(returnedContractualScopeDTO);
        assertContractualScopeUpdatableFieldsEquals(returnedContractualScope, getPersistedContractualScope(returnedContractualScope));

        insertedContractualScope = returnedContractualScope;
    }

    @Test
    @Transactional
    void createContractualScopeWithExistingId() throws Exception {
        // Create the ContractualScope with an existing ID
        insertedContractualScope = contractualScopeRepository.saveAndFlush(contractualScope);
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restContractualScopeMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ContractualScope in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDateDerivationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        contractualScope.setDateDerivation(null);

        // Create the ContractualScope, which fails.
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);

        restContractualScopeMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllContractualScopes() throws Exception {
        // Initialize the database
        insertedContractualScope = contractualScopeRepository.saveAndFlush(contractualScope);

        // Get all the contractualScopeList
        restContractualScopeMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(contractualScope.getId().toString())))
            .andExpect(jsonPath("$.[*].dateDerivation").value(hasItem(DEFAULT_DATE_DERIVATION.toString())))
            .andExpect(jsonPath("$.[*].sourceEventId").value(hasItem(DEFAULT_SOURCE_EVENT_ID)));
    }

    @Test
    @Transactional
    void getContractualScope() throws Exception {
        // Initialize the database
        insertedContractualScope = contractualScopeRepository.saveAndFlush(contractualScope);

        // Get the contractualScope
        restContractualScopeMockMvc
            .perform(get(ENTITY_API_URL_ID, contractualScope.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(contractualScope.getId().toString()))
            .andExpect(jsonPath("$.dateDerivation").value(DEFAULT_DATE_DERIVATION.toString()))
            .andExpect(jsonPath("$.sourceEventId").value(DEFAULT_SOURCE_EVENT_ID));
    }

    @Test
    @Transactional
    void getNonExistingContractualScope() throws Exception {
        // Get the contractualScope
        restContractualScopeMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingContractualScope() throws Exception {
        // Initialize the database
        insertedContractualScope = contractualScopeRepository.saveAndFlush(contractualScope);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the contractualScope
        ContractualScope updatedContractualScope = contractualScopeRepository.findById(contractualScope.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedContractualScope are not directly saved in db
        em.detach(updatedContractualScope);
        updatedContractualScope.dateDerivation(UPDATED_DATE_DERIVATION).sourceEventId(UPDATED_SOURCE_EVENT_ID);
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(updatedContractualScope);

        restContractualScopeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, contractualScopeDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isOk());

        // Validate the ContractualScope in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedContractualScopeToMatchAllProperties(updatedContractualScope);
    }

    @Test
    @Transactional
    void putNonExistingContractualScope() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contractualScope.setId(UUID.randomUUID());

        // Create the ContractualScope
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restContractualScopeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, contractualScopeDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ContractualScope in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchContractualScope() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contractualScope.setId(UUID.randomUUID());

        // Create the ContractualScope
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restContractualScopeMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ContractualScope in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamContractualScope() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contractualScope.setId(UUID.randomUUID());

        // Create the ContractualScope
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restContractualScopeMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ContractualScope in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateContractualScopeWithPatch() throws Exception {
        // Initialize the database
        insertedContractualScope = contractualScopeRepository.saveAndFlush(contractualScope);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the contractualScope using partial update
        ContractualScope partialUpdatedContractualScope = new ContractualScope();
        partialUpdatedContractualScope.setId(contractualScope.getId());

        partialUpdatedContractualScope.sourceEventId(UPDATED_SOURCE_EVENT_ID);

        restContractualScopeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedContractualScope.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedContractualScope))
            )
            .andExpect(status().isOk());

        // Validate the ContractualScope in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertContractualScopeUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedContractualScope, contractualScope),
            getPersistedContractualScope(contractualScope)
        );
    }

    @Test
    @Transactional
    void fullUpdateContractualScopeWithPatch() throws Exception {
        // Initialize the database
        insertedContractualScope = contractualScopeRepository.saveAndFlush(contractualScope);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the contractualScope using partial update
        ContractualScope partialUpdatedContractualScope = new ContractualScope();
        partialUpdatedContractualScope.setId(contractualScope.getId());

        partialUpdatedContractualScope.dateDerivation(UPDATED_DATE_DERIVATION).sourceEventId(UPDATED_SOURCE_EVENT_ID);

        restContractualScopeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedContractualScope.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedContractualScope))
            )
            .andExpect(status().isOk());

        // Validate the ContractualScope in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertContractualScopeUpdatableFieldsEquals(
            partialUpdatedContractualScope,
            getPersistedContractualScope(partialUpdatedContractualScope)
        );
    }

    @Test
    @Transactional
    void patchNonExistingContractualScope() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contractualScope.setId(UUID.randomUUID());

        // Create the ContractualScope
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restContractualScopeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, contractualScopeDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ContractualScope in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchContractualScope() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contractualScope.setId(UUID.randomUUID());

        // Create the ContractualScope
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restContractualScopeMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ContractualScope in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamContractualScope() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contractualScope.setId(UUID.randomUUID());

        // Create the ContractualScope
        ContractualScopeDTO contractualScopeDTO = contractualScopeMapper.toDto(contractualScope);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restContractualScopeMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(contractualScopeDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ContractualScope in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteContractualScope() throws Exception {
        // Initialize the database
        insertedContractualScope = contractualScopeRepository.saveAndFlush(contractualScope);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the contractualScope
        restContractualScopeMockMvc
            .perform(delete(ENTITY_API_URL_ID, contractualScope.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return contractualScopeRepository.count();
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

    protected ContractualScope getPersistedContractualScope(ContractualScope contractualScope) {
        return contractualScopeRepository.findById(contractualScope.getId()).orElseThrow();
    }

    protected void assertPersistedContractualScopeToMatchAllProperties(ContractualScope expectedContractualScope) {
        assertContractualScopeAllPropertiesEquals(expectedContractualScope, getPersistedContractualScope(expectedContractualScope));
    }

    protected void assertPersistedContractualScopeToMatchUpdatableProperties(ContractualScope expectedContractualScope) {
        assertContractualScopeAllUpdatablePropertiesEquals(
            expectedContractualScope,
            getPersistedContractualScope(expectedContractualScope)
        );
    }
}
