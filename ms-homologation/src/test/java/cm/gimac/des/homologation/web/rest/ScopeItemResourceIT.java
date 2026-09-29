package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.ScopeItemAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.domain.ScopeItem;
import cm.gimac.des.homologation.domain.enumeration.EtatScopeItem;
import cm.gimac.des.homologation.repository.ScopeItemRepository;
import cm.gimac.des.homologation.service.dto.ScopeItemDTO;
import cm.gimac.des.homologation.service.mapper.ScopeItemMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link ScopeItemResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ScopeItemResourceIT {

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_SERVICE_LIBELLE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_LIBELLE = "BBBBBBBBBB";

    private static final EtatScopeItem DEFAULT_ETAT = EtatScopeItem.A_ENGAGER;
    private static final EtatScopeItem UPDATED_ETAT = EtatScopeItem.EN_COURS;

    private static final String ENTITY_API_URL = "/api/scope-items";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ScopeItemRepository scopeItemRepository;

    @Autowired
    private ScopeItemMapper scopeItemMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restScopeItemMockMvc;

    private ScopeItem scopeItem;

    private ScopeItem insertedScopeItem;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ScopeItem createEntity(EntityManager em) {
        ScopeItem scopeItem = new ScopeItem().serviceCode(DEFAULT_SERVICE_CODE).serviceLibelle(DEFAULT_SERVICE_LIBELLE).etat(DEFAULT_ETAT);
        // Add required entity
        ContractualScope contractualScope;
        if (TestUtil.findAll(em, ContractualScope.class).isEmpty()) {
            contractualScope = ContractualScopeResourceIT.createEntity(em);
            em.persist(contractualScope);
            em.flush();
        } else {
            contractualScope = TestUtil.findAll(em, ContractualScope.class).getFirst();
        }
        scopeItem.setScope(contractualScope);
        return scopeItem;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ScopeItem createUpdatedEntity(EntityManager em) {
        ScopeItem updatedScopeItem = new ScopeItem()
            .serviceCode(UPDATED_SERVICE_CODE)
            .serviceLibelle(UPDATED_SERVICE_LIBELLE)
            .etat(UPDATED_ETAT);
        // Add required entity
        ContractualScope contractualScope;
        if (TestUtil.findAll(em, ContractualScope.class).isEmpty()) {
            contractualScope = ContractualScopeResourceIT.createUpdatedEntity(em);
            em.persist(contractualScope);
            em.flush();
        } else {
            contractualScope = TestUtil.findAll(em, ContractualScope.class).getFirst();
        }
        updatedScopeItem.setScope(contractualScope);
        return updatedScopeItem;
    }

    @BeforeEach
    void initTest() {
        scopeItem = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedScopeItem != null) {
            scopeItemRepository.delete(insertedScopeItem);
            insertedScopeItem = null;
        }
    }

    @Test
    @Transactional
    void createScopeItem() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the ScopeItem
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);
        var returnedScopeItemDTO = om.readValue(
            restScopeItemMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(scopeItemDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ScopeItemDTO.class
        );

        // Validate the ScopeItem in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedScopeItem = scopeItemMapper.toEntity(returnedScopeItemDTO);
        assertScopeItemUpdatableFieldsEquals(returnedScopeItem, getPersistedScopeItem(returnedScopeItem));

        insertedScopeItem = returnedScopeItem;
    }

    @Test
    @Transactional
    void createScopeItemWithExistingId() throws Exception {
        // Create the ScopeItem with an existing ID
        insertedScopeItem = scopeItemRepository.saveAndFlush(scopeItem);
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restScopeItemMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(scopeItemDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ScopeItem in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkServiceCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        scopeItem.setServiceCode(null);

        // Create the ScopeItem, which fails.
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        restScopeItemMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(scopeItemDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        scopeItem.setEtat(null);

        // Create the ScopeItem, which fails.
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        restScopeItemMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(scopeItemDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllScopeItems() throws Exception {
        // Initialize the database
        insertedScopeItem = scopeItemRepository.saveAndFlush(scopeItem);

        // Get all the scopeItemList
        restScopeItemMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(scopeItem.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].serviceLibelle").value(hasItem(DEFAULT_SERVICE_LIBELLE)))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())));
    }

    @Test
    @Transactional
    void getScopeItem() throws Exception {
        // Initialize the database
        insertedScopeItem = scopeItemRepository.saveAndFlush(scopeItem);

        // Get the scopeItem
        restScopeItemMockMvc
            .perform(get(ENTITY_API_URL_ID, scopeItem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(scopeItem.getId().toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.serviceLibelle").value(DEFAULT_SERVICE_LIBELLE))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingScopeItem() throws Exception {
        // Get the scopeItem
        restScopeItemMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingScopeItem() throws Exception {
        // Initialize the database
        insertedScopeItem = scopeItemRepository.saveAndFlush(scopeItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the scopeItem
        ScopeItem updatedScopeItem = scopeItemRepository.findById(scopeItem.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedScopeItem are not directly saved in db
        em.detach(updatedScopeItem);
        updatedScopeItem.serviceCode(UPDATED_SERVICE_CODE).serviceLibelle(UPDATED_SERVICE_LIBELLE).etat(UPDATED_ETAT);
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(updatedScopeItem);

        restScopeItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, scopeItemDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(scopeItemDTO))
            )
            .andExpect(status().isOk());

        // Validate the ScopeItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedScopeItemToMatchAllProperties(updatedScopeItem);
    }

    @Test
    @Transactional
    void putNonExistingScopeItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        scopeItem.setId(UUID.randomUUID());

        // Create the ScopeItem
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restScopeItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, scopeItemDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(scopeItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ScopeItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchScopeItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        scopeItem.setId(UUID.randomUUID());

        // Create the ScopeItem
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restScopeItemMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(scopeItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ScopeItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamScopeItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        scopeItem.setId(UUID.randomUUID());

        // Create the ScopeItem
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restScopeItemMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(scopeItemDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ScopeItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateScopeItemWithPatch() throws Exception {
        // Initialize the database
        insertedScopeItem = scopeItemRepository.saveAndFlush(scopeItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the scopeItem using partial update
        ScopeItem partialUpdatedScopeItem = new ScopeItem();
        partialUpdatedScopeItem.setId(scopeItem.getId());

        partialUpdatedScopeItem.serviceCode(UPDATED_SERVICE_CODE).serviceLibelle(UPDATED_SERVICE_LIBELLE);

        restScopeItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedScopeItem.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedScopeItem))
            )
            .andExpect(status().isOk());

        // Validate the ScopeItem in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertScopeItemUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedScopeItem, scopeItem),
            getPersistedScopeItem(scopeItem)
        );
    }

    @Test
    @Transactional
    void fullUpdateScopeItemWithPatch() throws Exception {
        // Initialize the database
        insertedScopeItem = scopeItemRepository.saveAndFlush(scopeItem);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the scopeItem using partial update
        ScopeItem partialUpdatedScopeItem = new ScopeItem();
        partialUpdatedScopeItem.setId(scopeItem.getId());

        partialUpdatedScopeItem.serviceCode(UPDATED_SERVICE_CODE).serviceLibelle(UPDATED_SERVICE_LIBELLE).etat(UPDATED_ETAT);

        restScopeItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedScopeItem.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedScopeItem))
            )
            .andExpect(status().isOk());

        // Validate the ScopeItem in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertScopeItemUpdatableFieldsEquals(partialUpdatedScopeItem, getPersistedScopeItem(partialUpdatedScopeItem));
    }

    @Test
    @Transactional
    void patchNonExistingScopeItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        scopeItem.setId(UUID.randomUUID());

        // Create the ScopeItem
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restScopeItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, scopeItemDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(scopeItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ScopeItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchScopeItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        scopeItem.setId(UUID.randomUUID());

        // Create the ScopeItem
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restScopeItemMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(scopeItemDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ScopeItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamScopeItem() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        scopeItem.setId(UUID.randomUUID());

        // Create the ScopeItem
        ScopeItemDTO scopeItemDTO = scopeItemMapper.toDto(scopeItem);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restScopeItemMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(scopeItemDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ScopeItem in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteScopeItem() throws Exception {
        // Initialize the database
        insertedScopeItem = scopeItemRepository.saveAndFlush(scopeItem);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the scopeItem
        restScopeItemMockMvc
            .perform(delete(ENTITY_API_URL_ID, scopeItem.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return scopeItemRepository.count();
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

    protected ScopeItem getPersistedScopeItem(ScopeItem scopeItem) {
        return scopeItemRepository.findById(scopeItem.getId()).orElseThrow();
    }

    protected void assertPersistedScopeItemToMatchAllProperties(ScopeItem expectedScopeItem) {
        assertScopeItemAllPropertiesEquals(expectedScopeItem, getPersistedScopeItem(expectedScopeItem));
    }

    protected void assertPersistedScopeItemToMatchUpdatableProperties(ScopeItem expectedScopeItem) {
        assertScopeItemAllUpdatablePropertiesEquals(expectedScopeItem, getPersistedScopeItem(expectedScopeItem));
    }
}
