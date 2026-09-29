package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.EvaluationBaseAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.EvaluationBase;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.repository.EvaluationBaseRepository;
import cm.gimac.des.homologation.service.dto.EvaluationBaseDTO;
import cm.gimac.des.homologation.service.mapper.EvaluationBaseMapper;
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
 * Integration tests for the {@link EvaluationBaseResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.homologation.security.AuthoritiesConstants.ADMIN)
class EvaluationBaseResourceIT {

    private static final String DEFAULT_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE = "BBBBBBBBBB";

    private static final Integer DEFAULT_CYCLE = 1;
    private static final Integer UPDATED_CYCLE = 2;

    private static final Instant DEFAULT_DATE_CALCUL = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_CALCUL = Instant.ofEpochMilli(1701958935371L);

    private static final String DEFAULT_COMPOSITION = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_COMPOSITION = "[\"B\"]";

    private static final String ENTITY_API_URL = "/api/evaluation-bases";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EvaluationBaseRepository evaluationBaseRepository;

    @Autowired
    private EvaluationBaseMapper evaluationBaseMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEvaluationBaseMockMvc;

    private EvaluationBase evaluationBase;

    private EvaluationBase insertedEvaluationBase;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EvaluationBase createEntity(EntityManager em) {
        EvaluationBase evaluationBase = new EvaluationBase()
            .empreinte(DEFAULT_EMPREINTE)
            .cycle(DEFAULT_CYCLE)
            .dateCalcul(DEFAULT_DATE_CALCUL)
            .composition(DEFAULT_COMPOSITION);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        evaluationBase.setLiaison(liaison);
        return evaluationBase;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EvaluationBase createUpdatedEntity(EntityManager em) {
        EvaluationBase updatedEvaluationBase = new EvaluationBase()
            .empreinte(UPDATED_EMPREINTE)
            .cycle(UPDATED_CYCLE)
            .dateCalcul(UPDATED_DATE_CALCUL)
            .composition(UPDATED_COMPOSITION);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        updatedEvaluationBase.setLiaison(liaison);
        return updatedEvaluationBase;
    }

    @BeforeEach
    void initTest() {
        evaluationBase = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEvaluationBase != null) {
            evaluationBaseRepository.delete(insertedEvaluationBase);
            insertedEvaluationBase = null;
        }
    }

    @Test
    @Transactional
    void createEvaluationBase() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the EvaluationBase
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);
        var returnedEvaluationBaseDTO = om.readValue(
            restEvaluationBaseMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(evaluationBaseDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            EvaluationBaseDTO.class
        );

        // Validate the EvaluationBase in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedEvaluationBase = evaluationBaseMapper.toEntity(returnedEvaluationBaseDTO);
        assertEvaluationBaseUpdatableFieldsEquals(returnedEvaluationBase, getPersistedEvaluationBase(returnedEvaluationBase));

        insertedEvaluationBase = returnedEvaluationBase;
    }

    @Test
    @Transactional
    void createEvaluationBaseWithExistingId() throws Exception {
        // Create the EvaluationBase with an existing ID
        insertedEvaluationBase = evaluationBaseRepository.saveAndFlush(evaluationBase);
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restEvaluationBaseMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvaluationBase in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkEmpreinteIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evaluationBase.setEmpreinte(null);

        // Create the EvaluationBase, which fails.
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        restEvaluationBaseMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCycleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evaluationBase.setCycle(null);

        // Create the EvaluationBase, which fails.
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        restEvaluationBaseMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateCalculIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evaluationBase.setDateCalcul(null);

        // Create the EvaluationBase, which fails.
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        restEvaluationBaseMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllEvaluationBases() throws Exception {
        // Initialize the database
        insertedEvaluationBase = evaluationBaseRepository.saveAndFlush(evaluationBase);

        // Get all the evaluationBaseList
        restEvaluationBaseMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(evaluationBase.getId().toString())))
            .andExpect(jsonPath("$.[*].empreinte").value(hasItem(DEFAULT_EMPREINTE)))
            .andExpect(jsonPath("$.[*].cycle").value(hasItem(DEFAULT_CYCLE)))
            .andExpect(jsonPath("$.[*].dateCalcul").value(hasItem(DEFAULT_DATE_CALCUL.toString())))
            .andExpect(jsonPath("$.[*].composition").value(hasItem(DEFAULT_COMPOSITION)));
    }

    @Test
    @Transactional
    void getEvaluationBase() throws Exception {
        // Initialize the database
        insertedEvaluationBase = evaluationBaseRepository.saveAndFlush(evaluationBase);

        // Get the evaluationBase
        restEvaluationBaseMockMvc
            .perform(get(ENTITY_API_URL_ID, evaluationBase.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(evaluationBase.getId().toString()))
            .andExpect(jsonPath("$.empreinte").value(DEFAULT_EMPREINTE))
            .andExpect(jsonPath("$.cycle").value(DEFAULT_CYCLE))
            .andExpect(jsonPath("$.dateCalcul").value(DEFAULT_DATE_CALCUL.toString()))
            .andExpect(jsonPath("$.composition").value(DEFAULT_COMPOSITION));
    }

    @Test
    @Transactional
    void getNonExistingEvaluationBase() throws Exception {
        // Get the evaluationBase
        restEvaluationBaseMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingEvaluationBase() throws Exception {
        // Initialize the database
        insertedEvaluationBase = evaluationBaseRepository.saveAndFlush(evaluationBase);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evaluationBase
        EvaluationBase updatedEvaluationBase = evaluationBaseRepository.findById(evaluationBase.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedEvaluationBase are not directly saved in db
        em.detach(updatedEvaluationBase);
        updatedEvaluationBase
            .empreinte(UPDATED_EMPREINTE)
            .cycle(UPDATED_CYCLE)
            .dateCalcul(UPDATED_DATE_CALCUL)
            .composition(UPDATED_COMPOSITION);
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(updatedEvaluationBase);

        restEvaluationBaseMockMvc
            .perform(
                put(ENTITY_API_URL_ID, evaluationBaseDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isOk());

        // Validate the EvaluationBase in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedEvaluationBaseToMatchAllProperties(updatedEvaluationBase);
    }

    @Test
    @Transactional
    void putNonExistingEvaluationBase() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evaluationBase.setId(UUID.randomUUID());

        // Create the EvaluationBase
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEvaluationBaseMockMvc
            .perform(
                put(ENTITY_API_URL_ID, evaluationBaseDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvaluationBase in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchEvaluationBase() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evaluationBase.setId(UUID.randomUUID());

        // Create the EvaluationBase
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvaluationBaseMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvaluationBase in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamEvaluationBase() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evaluationBase.setId(UUID.randomUUID());

        // Create the EvaluationBase
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvaluationBaseMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the EvaluationBase in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateEvaluationBaseWithPatch() throws Exception {
        // Initialize the database
        insertedEvaluationBase = evaluationBaseRepository.saveAndFlush(evaluationBase);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evaluationBase using partial update
        EvaluationBase partialUpdatedEvaluationBase = new EvaluationBase();
        partialUpdatedEvaluationBase.setId(evaluationBase.getId());

        partialUpdatedEvaluationBase.cycle(UPDATED_CYCLE).dateCalcul(UPDATED_DATE_CALCUL);

        restEvaluationBaseMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvaluationBase.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvaluationBase))
            )
            .andExpect(status().isOk());

        // Validate the EvaluationBase in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEvaluationBaseUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedEvaluationBase, evaluationBase),
            getPersistedEvaluationBase(evaluationBase)
        );
    }

    @Test
    @Transactional
    void fullUpdateEvaluationBaseWithPatch() throws Exception {
        // Initialize the database
        insertedEvaluationBase = evaluationBaseRepository.saveAndFlush(evaluationBase);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evaluationBase using partial update
        EvaluationBase partialUpdatedEvaluationBase = new EvaluationBase();
        partialUpdatedEvaluationBase.setId(evaluationBase.getId());

        partialUpdatedEvaluationBase
            .empreinte(UPDATED_EMPREINTE)
            .cycle(UPDATED_CYCLE)
            .dateCalcul(UPDATED_DATE_CALCUL)
            .composition(UPDATED_COMPOSITION);

        restEvaluationBaseMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvaluationBase.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvaluationBase))
            )
            .andExpect(status().isOk());

        // Validate the EvaluationBase in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEvaluationBaseUpdatableFieldsEquals(partialUpdatedEvaluationBase, getPersistedEvaluationBase(partialUpdatedEvaluationBase));
    }

    @Test
    @Transactional
    void patchNonExistingEvaluationBase() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evaluationBase.setId(UUID.randomUUID());

        // Create the EvaluationBase
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEvaluationBaseMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, evaluationBaseDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvaluationBase in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchEvaluationBase() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evaluationBase.setId(UUID.randomUUID());

        // Create the EvaluationBase
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvaluationBaseMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvaluationBase in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamEvaluationBase() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evaluationBase.setId(UUID.randomUUID());

        // Create the EvaluationBase
        EvaluationBaseDTO evaluationBaseDTO = evaluationBaseMapper.toDto(evaluationBase);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvaluationBaseMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evaluationBaseDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the EvaluationBase in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteEvaluationBase() throws Exception {
        // Initialize the database
        insertedEvaluationBase = evaluationBaseRepository.saveAndFlush(evaluationBase);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the evaluationBase
        restEvaluationBaseMockMvc
            .perform(delete(ENTITY_API_URL_ID, evaluationBase.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return evaluationBaseRepository.count();
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

    protected EvaluationBase getPersistedEvaluationBase(EvaluationBase evaluationBase) {
        return evaluationBaseRepository.findById(evaluationBase.getId()).orElseThrow();
    }

    protected void assertPersistedEvaluationBaseToMatchAllProperties(EvaluationBase expectedEvaluationBase) {
        assertEvaluationBaseAllPropertiesEquals(expectedEvaluationBase, getPersistedEvaluationBase(expectedEvaluationBase));
    }

    protected void assertPersistedEvaluationBaseToMatchUpdatableProperties(EvaluationBase expectedEvaluationBase) {
        assertEvaluationBaseAllUpdatablePropertiesEquals(expectedEvaluationBase, getPersistedEvaluationBase(expectedEvaluationBase));
    }
}
