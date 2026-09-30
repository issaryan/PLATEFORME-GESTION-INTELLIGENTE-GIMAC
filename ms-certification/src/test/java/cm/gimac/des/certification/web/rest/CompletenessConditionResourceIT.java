package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.CompletenessConditionAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CompletenessCondition;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.domain.enumeration.ConditionCode;
import cm.gimac.des.certification.repository.CompletenessConditionRepository;
import cm.gimac.des.certification.service.dto.CompletenessConditionDTO;
import cm.gimac.des.certification.service.mapper.CompletenessConditionMapper;
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
 * Integration tests for the {@link CompletenessConditionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.certification.security.AuthoritiesConstants.ADMIN)
class CompletenessConditionResourceIT {

    private static final ConditionCode DEFAULT_CODE = ConditionCode.COUVERTURE_CERTIFICATION;
    private static final ConditionCode UPDATED_CODE = ConditionCode.COUVERTURE_MISE_EN_SERVICE;

    private static final Boolean DEFAULT_SATISFAITE = false;
    private static final Boolean UPDATED_SATISFAITE = true;

    private static final String DEFAULT_SOURCE = "AAAAAAAAAA";
    private static final String UPDATED_SOURCE = "BBBBBBBBBB";

    private static final String DEFAULT_DETAIL = "AAAAAAAAAA";
    private static final String UPDATED_DETAIL = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_VERIFICATION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_VERIFICATION = Instant.ofEpochMilli(1702411306964L);

    private static final String ENTITY_API_URL = "/api/completeness-conditions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CompletenessConditionRepository completenessConditionRepository;

    @Autowired
    private CompletenessConditionMapper completenessConditionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCompletenessConditionMockMvc;

    private CompletenessCondition completenessCondition;

    private CompletenessCondition insertedCompletenessCondition;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CompletenessCondition createEntity(EntityManager em) {
        CompletenessCondition completenessCondition = new CompletenessCondition()
            .code(DEFAULT_CODE)
            .satisfaite(DEFAULT_SATISFAITE)
            .source(DEFAULT_SOURCE)
            .detail(DEFAULT_DETAIL)
            .dateVerification(DEFAULT_DATE_VERIFICATION);
        // Add required entity
        GoLiveDossier goLiveDossier;
        if (TestUtil.findAll(em, GoLiveDossier.class).isEmpty()) {
            goLiveDossier = GoLiveDossierResourceIT.createEntity(em);
            em.persist(goLiveDossier);
            em.flush();
        } else {
            goLiveDossier = TestUtil.findAll(em, GoLiveDossier.class).getFirst();
        }
        completenessCondition.setDossier(goLiveDossier);
        return completenessCondition;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CompletenessCondition createUpdatedEntity(EntityManager em) {
        CompletenessCondition updatedCompletenessCondition = new CompletenessCondition()
            .code(UPDATED_CODE)
            .satisfaite(UPDATED_SATISFAITE)
            .source(UPDATED_SOURCE)
            .detail(UPDATED_DETAIL)
            .dateVerification(UPDATED_DATE_VERIFICATION);
        // Add required entity
        GoLiveDossier goLiveDossier;
        if (TestUtil.findAll(em, GoLiveDossier.class).isEmpty()) {
            goLiveDossier = GoLiveDossierResourceIT.createUpdatedEntity(em);
            em.persist(goLiveDossier);
            em.flush();
        } else {
            goLiveDossier = TestUtil.findAll(em, GoLiveDossier.class).getFirst();
        }
        updatedCompletenessCondition.setDossier(goLiveDossier);
        return updatedCompletenessCondition;
    }

    @BeforeEach
    void initTest() {
        completenessCondition = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCompletenessCondition != null) {
            completenessConditionRepository.delete(insertedCompletenessCondition);
            insertedCompletenessCondition = null;
        }
    }

    @Test
    @Transactional
    void createCompletenessCondition() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the CompletenessCondition
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);
        var returnedCompletenessConditionDTO = om.readValue(
            restCompletenessConditionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(completenessConditionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CompletenessConditionDTO.class
        );

        // Validate the CompletenessCondition in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCompletenessCondition = completenessConditionMapper.toEntity(returnedCompletenessConditionDTO);
        assertCompletenessConditionUpdatableFieldsEquals(
            returnedCompletenessCondition,
            getPersistedCompletenessCondition(returnedCompletenessCondition)
        );

        insertedCompletenessCondition = returnedCompletenessCondition;
    }

    @Test
    @Transactional
    void createCompletenessConditionWithExistingId() throws Exception {
        // Create the CompletenessCondition with an existing ID
        insertedCompletenessCondition = completenessConditionRepository.saveAndFlush(completenessCondition);
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCompletenessConditionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CompletenessCondition in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        completenessCondition.setCode(null);

        // Create the CompletenessCondition, which fails.
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        restCompletenessConditionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSatisfaiteIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        completenessCondition.setSatisfaite(null);

        // Create the CompletenessCondition, which fails.
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        restCompletenessConditionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCompletenessConditions() throws Exception {
        // Initialize the database
        insertedCompletenessCondition = completenessConditionRepository.saveAndFlush(completenessCondition);

        // Get all the completenessConditionList
        restCompletenessConditionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(completenessCondition.getId().toString())))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE.toString())))
            .andExpect(jsonPath("$.[*].satisfaite").value(hasItem(DEFAULT_SATISFAITE)))
            .andExpect(jsonPath("$.[*].source").value(hasItem(DEFAULT_SOURCE)))
            .andExpect(jsonPath("$.[*].detail").value(hasItem(DEFAULT_DETAIL)))
            .andExpect(jsonPath("$.[*].dateVerification").value(hasItem(DEFAULT_DATE_VERIFICATION.toString())));
    }

    @Test
    @Transactional
    void getCompletenessCondition() throws Exception {
        // Initialize the database
        insertedCompletenessCondition = completenessConditionRepository.saveAndFlush(completenessCondition);

        // Get the completenessCondition
        restCompletenessConditionMockMvc
            .perform(get(ENTITY_API_URL_ID, completenessCondition.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(completenessCondition.getId().toString()))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE.toString()))
            .andExpect(jsonPath("$.satisfaite").value(DEFAULT_SATISFAITE))
            .andExpect(jsonPath("$.source").value(DEFAULT_SOURCE))
            .andExpect(jsonPath("$.detail").value(DEFAULT_DETAIL))
            .andExpect(jsonPath("$.dateVerification").value(DEFAULT_DATE_VERIFICATION.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCompletenessCondition() throws Exception {
        // Get the completenessCondition
        restCompletenessConditionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCompletenessCondition() throws Exception {
        // Initialize the database
        insertedCompletenessCondition = completenessConditionRepository.saveAndFlush(completenessCondition);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the completenessCondition
        CompletenessCondition updatedCompletenessCondition = completenessConditionRepository
            .findById(completenessCondition.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedCompletenessCondition are not directly saved in db
        em.detach(updatedCompletenessCondition);
        updatedCompletenessCondition
            .code(UPDATED_CODE)
            .satisfaite(UPDATED_SATISFAITE)
            .source(UPDATED_SOURCE)
            .detail(UPDATED_DETAIL)
            .dateVerification(UPDATED_DATE_VERIFICATION);
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(updatedCompletenessCondition);

        restCompletenessConditionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, completenessConditionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isOk());

        // Validate the CompletenessCondition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCompletenessConditionToMatchAllProperties(updatedCompletenessCondition);
    }

    @Test
    @Transactional
    void putNonExistingCompletenessCondition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        completenessCondition.setId(UUID.randomUUID());

        // Create the CompletenessCondition
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCompletenessConditionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, completenessConditionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CompletenessCondition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCompletenessCondition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        completenessCondition.setId(UUID.randomUUID());

        // Create the CompletenessCondition
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCompletenessConditionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CompletenessCondition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCompletenessCondition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        completenessCondition.setId(UUID.randomUUID());

        // Create the CompletenessCondition
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCompletenessConditionMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CompletenessCondition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCompletenessConditionWithPatch() throws Exception {
        // Initialize the database
        insertedCompletenessCondition = completenessConditionRepository.saveAndFlush(completenessCondition);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the completenessCondition using partial update
        CompletenessCondition partialUpdatedCompletenessCondition = new CompletenessCondition();
        partialUpdatedCompletenessCondition.setId(completenessCondition.getId());

        partialUpdatedCompletenessCondition.source(UPDATED_SOURCE).detail(UPDATED_DETAIL).dateVerification(UPDATED_DATE_VERIFICATION);

        restCompletenessConditionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCompletenessCondition.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCompletenessCondition))
            )
            .andExpect(status().isOk());

        // Validate the CompletenessCondition in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCompletenessConditionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCompletenessCondition, completenessCondition),
            getPersistedCompletenessCondition(completenessCondition)
        );
    }

    @Test
    @Transactional
    void fullUpdateCompletenessConditionWithPatch() throws Exception {
        // Initialize the database
        insertedCompletenessCondition = completenessConditionRepository.saveAndFlush(completenessCondition);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the completenessCondition using partial update
        CompletenessCondition partialUpdatedCompletenessCondition = new CompletenessCondition();
        partialUpdatedCompletenessCondition.setId(completenessCondition.getId());

        partialUpdatedCompletenessCondition
            .code(UPDATED_CODE)
            .satisfaite(UPDATED_SATISFAITE)
            .source(UPDATED_SOURCE)
            .detail(UPDATED_DETAIL)
            .dateVerification(UPDATED_DATE_VERIFICATION);

        restCompletenessConditionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCompletenessCondition.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCompletenessCondition))
            )
            .andExpect(status().isOk());

        // Validate the CompletenessCondition in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCompletenessConditionUpdatableFieldsEquals(
            partialUpdatedCompletenessCondition,
            getPersistedCompletenessCondition(partialUpdatedCompletenessCondition)
        );
    }

    @Test
    @Transactional
    void patchNonExistingCompletenessCondition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        completenessCondition.setId(UUID.randomUUID());

        // Create the CompletenessCondition
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCompletenessConditionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, completenessConditionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CompletenessCondition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCompletenessCondition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        completenessCondition.setId(UUID.randomUUID());

        // Create the CompletenessCondition
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCompletenessConditionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CompletenessCondition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCompletenessCondition() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        completenessCondition.setId(UUID.randomUUID());

        // Create the CompletenessCondition
        CompletenessConditionDTO completenessConditionDTO = completenessConditionMapper.toDto(completenessCondition);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCompletenessConditionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(completenessConditionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CompletenessCondition in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCompletenessCondition() throws Exception {
        // Initialize the database
        insertedCompletenessCondition = completenessConditionRepository.saveAndFlush(completenessCondition);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the completenessCondition
        restCompletenessConditionMockMvc
            .perform(delete(ENTITY_API_URL_ID, completenessCondition.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return completenessConditionRepository.count();
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

    protected CompletenessCondition getPersistedCompletenessCondition(CompletenessCondition completenessCondition) {
        return completenessConditionRepository.findById(completenessCondition.getId()).orElseThrow();
    }

    protected void assertPersistedCompletenessConditionToMatchAllProperties(CompletenessCondition expectedCompletenessCondition) {
        assertCompletenessConditionAllPropertiesEquals(
            expectedCompletenessCondition,
            getPersistedCompletenessCondition(expectedCompletenessCondition)
        );
    }

    protected void assertPersistedCompletenessConditionToMatchUpdatableProperties(CompletenessCondition expectedCompletenessCondition) {
        assertCompletenessConditionAllUpdatablePropertiesEquals(
            expectedCompletenessCondition,
            getPersistedCompletenessCondition(expectedCompletenessCondition)
        );
    }
}
