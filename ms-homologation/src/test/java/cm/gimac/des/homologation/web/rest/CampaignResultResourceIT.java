package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.CampaignResultAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.CampaignResult;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.Verdict;
import cm.gimac.des.homologation.repository.CampaignResultRepository;
import cm.gimac.des.homologation.service.dto.CampaignResultDTO;
import cm.gimac.des.homologation.service.mapper.CampaignResultMapper;
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
 * Integration tests for the {@link CampaignResultResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CampaignResultResourceIT {

    private static final String DEFAULT_CAMPAGNE_ID = "AAAAAAAAAA";
    private static final String UPDATED_CAMPAGNE_ID = "BBBBBBBBBB";

    private static final String DEFAULT_EXECUTION_ID = "AAAAAAAAAA";
    private static final String UPDATED_EXECUTION_ID = "BBBBBBBBBB";

    private static final String DEFAULT_ID_SCENARIO = "AAAAAAAAAA";
    private static final String UPDATED_ID_SCENARIO = "BBBBBBBBBB";

    private static final Verdict DEFAULT_VERDICT = Verdict.FAVORABLE;
    private static final Verdict UPDATED_VERDICT = Verdict.DEFAVORABLE;

    private static final Instant DEFAULT_DATE_EXECUTION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_EXECUTION = Instant.ofEpochMilli(1701958935371L);

    private static final String DEFAULT_VERSION_SCENARIO = "AAAAAAAAAA";
    private static final String UPDATED_VERSION_SCENARIO = "BBBBBBBBBB";

    private static final String DEFAULT_REFERENCE_TRACE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE_TRACE = "BBBBBBBBBB";

    private static final String DEFAULT_EMPREINTE_REPONSE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE_REPONSE = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_REPRISE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_REPRISE = Instant.ofEpochMilli(1701958935371L);

    private static final String ENTITY_API_URL = "/api/campaign-results";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CampaignResultRepository campaignResultRepository;

    @Autowired
    private CampaignResultMapper campaignResultMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCampaignResultMockMvc;

    private CampaignResult campaignResult;

    private CampaignResult insertedCampaignResult;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CampaignResult createEntity(EntityManager em) {
        CampaignResult campaignResult = new CampaignResult()
            .campagneId(DEFAULT_CAMPAGNE_ID)
            .executionId(DEFAULT_EXECUTION_ID)
            .idScenario(DEFAULT_ID_SCENARIO)
            .verdict(DEFAULT_VERDICT)
            .dateExecution(DEFAULT_DATE_EXECUTION)
            .versionScenario(DEFAULT_VERSION_SCENARIO)
            .referenceTrace(DEFAULT_REFERENCE_TRACE)
            .empreinteReponse(DEFAULT_EMPREINTE_REPONSE)
            .dateReprise(DEFAULT_DATE_REPRISE);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        campaignResult.setLiaison(liaison);
        return campaignResult;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CampaignResult createUpdatedEntity(EntityManager em) {
        CampaignResult updatedCampaignResult = new CampaignResult()
            .campagneId(UPDATED_CAMPAGNE_ID)
            .executionId(UPDATED_EXECUTION_ID)
            .idScenario(UPDATED_ID_SCENARIO)
            .verdict(UPDATED_VERDICT)
            .dateExecution(UPDATED_DATE_EXECUTION)
            .versionScenario(UPDATED_VERSION_SCENARIO)
            .referenceTrace(UPDATED_REFERENCE_TRACE)
            .empreinteReponse(UPDATED_EMPREINTE_REPONSE)
            .dateReprise(UPDATED_DATE_REPRISE);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        updatedCampaignResult.setLiaison(liaison);
        return updatedCampaignResult;
    }

    @BeforeEach
    void initTest() {
        campaignResult = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCampaignResult != null) {
            campaignResultRepository.delete(insertedCampaignResult);
            insertedCampaignResult = null;
        }
    }

    @Test
    @Transactional
    void createCampaignResult() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the CampaignResult
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);
        var returnedCampaignResultDTO = om.readValue(
            restCampaignResultMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(campaignResultDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CampaignResultDTO.class
        );

        // Validate the CampaignResult in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCampaignResult = campaignResultMapper.toEntity(returnedCampaignResultDTO);
        assertCampaignResultUpdatableFieldsEquals(returnedCampaignResult, getPersistedCampaignResult(returnedCampaignResult));

        insertedCampaignResult = returnedCampaignResult;
    }

    @Test
    @Transactional
    void createCampaignResultWithExistingId() throws Exception {
        // Create the CampaignResult with an existing ID
        insertedCampaignResult = campaignResultRepository.saveAndFlush(campaignResult);
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCampaignResultMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CampaignResult in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkCampagneIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaignResult.setCampagneId(null);

        // Create the CampaignResult, which fails.
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        restCampaignResultMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkExecutionIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaignResult.setExecutionId(null);

        // Create the CampaignResult, which fails.
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        restCampaignResultMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIdScenarioIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaignResult.setIdScenario(null);

        // Create the CampaignResult, which fails.
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        restCampaignResultMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVerdictIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaignResult.setVerdict(null);

        // Create the CampaignResult, which fails.
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        restCampaignResultMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateExecutionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaignResult.setDateExecution(null);

        // Create the CampaignResult, which fails.
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        restCampaignResultMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEmpreinteReponseIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaignResult.setEmpreinteReponse(null);

        // Create the CampaignResult, which fails.
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        restCampaignResultMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateRepriseIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaignResult.setDateReprise(null);

        // Create the CampaignResult, which fails.
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        restCampaignResultMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCampaignResults() throws Exception {
        // Initialize the database
        insertedCampaignResult = campaignResultRepository.saveAndFlush(campaignResult);

        // Get all the campaignResultList
        restCampaignResultMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(campaignResult.getId().toString())))
            .andExpect(jsonPath("$.[*].campagneId").value(hasItem(DEFAULT_CAMPAGNE_ID)))
            .andExpect(jsonPath("$.[*].executionId").value(hasItem(DEFAULT_EXECUTION_ID)))
            .andExpect(jsonPath("$.[*].idScenario").value(hasItem(DEFAULT_ID_SCENARIO)))
            .andExpect(jsonPath("$.[*].verdict").value(hasItem(DEFAULT_VERDICT.toString())))
            .andExpect(jsonPath("$.[*].dateExecution").value(hasItem(DEFAULT_DATE_EXECUTION.toString())))
            .andExpect(jsonPath("$.[*].versionScenario").value(hasItem(DEFAULT_VERSION_SCENARIO)))
            .andExpect(jsonPath("$.[*].referenceTrace").value(hasItem(DEFAULT_REFERENCE_TRACE)))
            .andExpect(jsonPath("$.[*].empreinteReponse").value(hasItem(DEFAULT_EMPREINTE_REPONSE)))
            .andExpect(jsonPath("$.[*].dateReprise").value(hasItem(DEFAULT_DATE_REPRISE.toString())));
    }

    @Test
    @Transactional
    void getCampaignResult() throws Exception {
        // Initialize the database
        insertedCampaignResult = campaignResultRepository.saveAndFlush(campaignResult);

        // Get the campaignResult
        restCampaignResultMockMvc
            .perform(get(ENTITY_API_URL_ID, campaignResult.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(campaignResult.getId().toString()))
            .andExpect(jsonPath("$.campagneId").value(DEFAULT_CAMPAGNE_ID))
            .andExpect(jsonPath("$.executionId").value(DEFAULT_EXECUTION_ID))
            .andExpect(jsonPath("$.idScenario").value(DEFAULT_ID_SCENARIO))
            .andExpect(jsonPath("$.verdict").value(DEFAULT_VERDICT.toString()))
            .andExpect(jsonPath("$.dateExecution").value(DEFAULT_DATE_EXECUTION.toString()))
            .andExpect(jsonPath("$.versionScenario").value(DEFAULT_VERSION_SCENARIO))
            .andExpect(jsonPath("$.referenceTrace").value(DEFAULT_REFERENCE_TRACE))
            .andExpect(jsonPath("$.empreinteReponse").value(DEFAULT_EMPREINTE_REPONSE))
            .andExpect(jsonPath("$.dateReprise").value(DEFAULT_DATE_REPRISE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCampaignResult() throws Exception {
        // Get the campaignResult
        restCampaignResultMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCampaignResult() throws Exception {
        // Initialize the database
        insertedCampaignResult = campaignResultRepository.saveAndFlush(campaignResult);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaignResult
        CampaignResult updatedCampaignResult = campaignResultRepository.findById(campaignResult.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCampaignResult are not directly saved in db
        em.detach(updatedCampaignResult);
        updatedCampaignResult
            .campagneId(UPDATED_CAMPAGNE_ID)
            .executionId(UPDATED_EXECUTION_ID)
            .idScenario(UPDATED_ID_SCENARIO)
            .verdict(UPDATED_VERDICT)
            .dateExecution(UPDATED_DATE_EXECUTION)
            .versionScenario(UPDATED_VERSION_SCENARIO)
            .referenceTrace(UPDATED_REFERENCE_TRACE)
            .empreinteReponse(UPDATED_EMPREINTE_REPONSE)
            .dateReprise(UPDATED_DATE_REPRISE);
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(updatedCampaignResult);

        restCampaignResultMockMvc
            .perform(
                put(ENTITY_API_URL_ID, campaignResultDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isOk());

        // Validate the CampaignResult in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCampaignResultToMatchAllProperties(updatedCampaignResult);
    }

    @Test
    @Transactional
    void putNonExistingCampaignResult() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaignResult.setId(UUID.randomUUID());

        // Create the CampaignResult
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCampaignResultMockMvc
            .perform(
                put(ENTITY_API_URL_ID, campaignResultDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CampaignResult in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCampaignResult() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaignResult.setId(UUID.randomUUID());

        // Create the CampaignResult
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCampaignResultMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CampaignResult in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCampaignResult() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaignResult.setId(UUID.randomUUID());

        // Create the CampaignResult
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCampaignResultMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CampaignResult in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCampaignResultWithPatch() throws Exception {
        // Initialize the database
        insertedCampaignResult = campaignResultRepository.saveAndFlush(campaignResult);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaignResult using partial update
        CampaignResult partialUpdatedCampaignResult = new CampaignResult();
        partialUpdatedCampaignResult.setId(campaignResult.getId());

        partialUpdatedCampaignResult
            .campagneId(UPDATED_CAMPAGNE_ID)
            .idScenario(UPDATED_ID_SCENARIO)
            .dateExecution(UPDATED_DATE_EXECUTION)
            .versionScenario(UPDATED_VERSION_SCENARIO)
            .dateReprise(UPDATED_DATE_REPRISE);

        restCampaignResultMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCampaignResult.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCampaignResult))
            )
            .andExpect(status().isOk());

        // Validate the CampaignResult in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCampaignResultUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCampaignResult, campaignResult),
            getPersistedCampaignResult(campaignResult)
        );
    }

    @Test
    @Transactional
    void fullUpdateCampaignResultWithPatch() throws Exception {
        // Initialize the database
        insertedCampaignResult = campaignResultRepository.saveAndFlush(campaignResult);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaignResult using partial update
        CampaignResult partialUpdatedCampaignResult = new CampaignResult();
        partialUpdatedCampaignResult.setId(campaignResult.getId());

        partialUpdatedCampaignResult
            .campagneId(UPDATED_CAMPAGNE_ID)
            .executionId(UPDATED_EXECUTION_ID)
            .idScenario(UPDATED_ID_SCENARIO)
            .verdict(UPDATED_VERDICT)
            .dateExecution(UPDATED_DATE_EXECUTION)
            .versionScenario(UPDATED_VERSION_SCENARIO)
            .referenceTrace(UPDATED_REFERENCE_TRACE)
            .empreinteReponse(UPDATED_EMPREINTE_REPONSE)
            .dateReprise(UPDATED_DATE_REPRISE);

        restCampaignResultMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCampaignResult.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCampaignResult))
            )
            .andExpect(status().isOk());

        // Validate the CampaignResult in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCampaignResultUpdatableFieldsEquals(partialUpdatedCampaignResult, getPersistedCampaignResult(partialUpdatedCampaignResult));
    }

    @Test
    @Transactional
    void patchNonExistingCampaignResult() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaignResult.setId(UUID.randomUUID());

        // Create the CampaignResult
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCampaignResultMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, campaignResultDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CampaignResult in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCampaignResult() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaignResult.setId(UUID.randomUUID());

        // Create the CampaignResult
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCampaignResultMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CampaignResult in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCampaignResult() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaignResult.setId(UUID.randomUUID());

        // Create the CampaignResult
        CampaignResultDTO campaignResultDTO = campaignResultMapper.toDto(campaignResult);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCampaignResultMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(campaignResultDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CampaignResult in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCampaignResult() throws Exception {
        // Initialize the database
        insertedCampaignResult = campaignResultRepository.saveAndFlush(campaignResult);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the campaignResult
        restCampaignResultMockMvc
            .perform(delete(ENTITY_API_URL_ID, campaignResult.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return campaignResultRepository.count();
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

    protected CampaignResult getPersistedCampaignResult(CampaignResult campaignResult) {
        return campaignResultRepository.findById(campaignResult.getId()).orElseThrow();
    }

    protected void assertPersistedCampaignResultToMatchAllProperties(CampaignResult expectedCampaignResult) {
        assertCampaignResultAllPropertiesEquals(expectedCampaignResult, getPersistedCampaignResult(expectedCampaignResult));
    }

    protected void assertPersistedCampaignResultToMatchUpdatableProperties(CampaignResult expectedCampaignResult) {
        assertCampaignResultAllUpdatablePropertiesEquals(expectedCampaignResult, getPersistedCampaignResult(expectedCampaignResult));
    }
}
