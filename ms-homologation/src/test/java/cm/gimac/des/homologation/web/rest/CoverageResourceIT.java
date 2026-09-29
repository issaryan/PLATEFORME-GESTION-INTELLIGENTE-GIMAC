package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.CoverageAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static cm.gimac.des.homologation.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.service.dto.CoverageDTO;
import cm.gimac.des.homologation.service.mapper.CoverageMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
 * Integration tests for the {@link CoverageResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.homologation.security.AuthoritiesConstants.ADMIN)
class CoverageResourceIT {

    private static final String DEFAULT_SERVICE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_SERVICE_CODE = "BBBBBBBBBB";

    private static final NatureCouverture DEFAULT_NATURE = NatureCouverture.CERTIFICATION;
    private static final NatureCouverture UPDATED_NATURE = NatureCouverture.MISE_EN_SERVICE;

    private static final Integer DEFAULT_NUMERATEUR = 1;
    private static final Integer UPDATED_NUMERATEUR = 2;

    private static final Integer DEFAULT_DENOMINATEUR = 1;
    private static final Integer UPDATED_DENOMINATEUR = 2;

    private static final BigDecimal DEFAULT_TAUX = new BigDecimal(1);
    private static final BigDecimal UPDATED_TAUX = new BigDecimal(2);

    private static final Boolean DEFAULT_CALCULABLE = false;
    private static final Boolean UPDATED_CALCULABLE = true;

    private static final String DEFAULT_MOTIF_NON_CALCULABLE = "AAAAAAAAAA";
    private static final String UPDATED_MOTIF_NON_CALCULABLE = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_CALCUL = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_CALCUL = Instant.ofEpochMilli(1701958935371L);

    private static final String ENTITY_API_URL = "/api/coverages";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CoverageRepository coverageRepository;

    @Autowired
    private CoverageMapper coverageMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCoverageMockMvc;

    private Coverage coverage;

    private Coverage insertedCoverage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Coverage createEntity(EntityManager em) {
        Coverage coverage = new Coverage()
            .serviceCode(DEFAULT_SERVICE_CODE)
            .nature(DEFAULT_NATURE)
            .numerateur(DEFAULT_NUMERATEUR)
            .denominateur(DEFAULT_DENOMINATEUR)
            .taux(DEFAULT_TAUX)
            .calculable(DEFAULT_CALCULABLE)
            .motifNonCalculable(DEFAULT_MOTIF_NON_CALCULABLE)
            .dateCalcul(DEFAULT_DATE_CALCUL);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        coverage.setLiaison(liaison);
        return coverage;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Coverage createUpdatedEntity(EntityManager em) {
        Coverage updatedCoverage = new Coverage()
            .serviceCode(UPDATED_SERVICE_CODE)
            .nature(UPDATED_NATURE)
            .numerateur(UPDATED_NUMERATEUR)
            .denominateur(UPDATED_DENOMINATEUR)
            .taux(UPDATED_TAUX)
            .calculable(UPDATED_CALCULABLE)
            .motifNonCalculable(UPDATED_MOTIF_NON_CALCULABLE)
            .dateCalcul(UPDATED_DATE_CALCUL);
        // Add required entity
        Liaison liaison;
        if (TestUtil.findAll(em, Liaison.class).isEmpty()) {
            liaison = LiaisonResourceIT.createUpdatedEntity(em);
            em.persist(liaison);
            em.flush();
        } else {
            liaison = TestUtil.findAll(em, Liaison.class).getFirst();
        }
        updatedCoverage.setLiaison(liaison);
        return updatedCoverage;
    }

    @BeforeEach
    void initTest() {
        coverage = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedCoverage != null) {
            coverageRepository.delete(insertedCoverage);
            insertedCoverage = null;
        }
    }

    @Test
    @Transactional
    void createCoverage() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Coverage
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);
        var returnedCoverageDTO = om.readValue(
            restCoverageMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(coverageDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CoverageDTO.class
        );

        // Validate the Coverage in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCoverage = coverageMapper.toEntity(returnedCoverageDTO);
        assertCoverageUpdatableFieldsEquals(returnedCoverage, getPersistedCoverage(returnedCoverage));

        insertedCoverage = returnedCoverage;
    }

    @Test
    @Transactional
    void createCoverageWithExistingId() throws Exception {
        // Create the Coverage with an existing ID
        insertedCoverage = coverageRepository.saveAndFlush(coverage);
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCoverageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(coverageDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Coverage in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkServiceCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        coverage.setServiceCode(null);

        // Create the Coverage, which fails.
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        restCoverageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(coverageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNatureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        coverage.setNature(null);

        // Create the Coverage, which fails.
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        restCoverageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(coverageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCalculableIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        coverage.setCalculable(null);

        // Create the Coverage, which fails.
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        restCoverageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(coverageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateCalculIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        coverage.setDateCalcul(null);

        // Create the Coverage, which fails.
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        restCoverageMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(coverageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCoverages() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get all the coverageList
        restCoverageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(coverage.getId().toString())))
            .andExpect(jsonPath("$.[*].serviceCode").value(hasItem(DEFAULT_SERVICE_CODE)))
            .andExpect(jsonPath("$.[*].nature").value(hasItem(DEFAULT_NATURE.toString())))
            .andExpect(jsonPath("$.[*].numerateur").value(hasItem(DEFAULT_NUMERATEUR)))
            .andExpect(jsonPath("$.[*].denominateur").value(hasItem(DEFAULT_DENOMINATEUR)))
            .andExpect(jsonPath("$.[*].taux").value(hasItem(sameNumber(DEFAULT_TAUX))))
            .andExpect(jsonPath("$.[*].calculable").value(hasItem(DEFAULT_CALCULABLE)))
            .andExpect(jsonPath("$.[*].motifNonCalculable").value(hasItem(DEFAULT_MOTIF_NON_CALCULABLE)))
            .andExpect(jsonPath("$.[*].dateCalcul").value(hasItem(DEFAULT_DATE_CALCUL.toString())));
    }

    @Test
    @Transactional
    void getCoverage() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        // Get the coverage
        restCoverageMockMvc
            .perform(get(ENTITY_API_URL_ID, coverage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(coverage.getId().toString()))
            .andExpect(jsonPath("$.serviceCode").value(DEFAULT_SERVICE_CODE))
            .andExpect(jsonPath("$.nature").value(DEFAULT_NATURE.toString()))
            .andExpect(jsonPath("$.numerateur").value(DEFAULT_NUMERATEUR))
            .andExpect(jsonPath("$.denominateur").value(DEFAULT_DENOMINATEUR))
            .andExpect(jsonPath("$.taux").value(sameNumber(DEFAULT_TAUX)))
            .andExpect(jsonPath("$.calculable").value(DEFAULT_CALCULABLE))
            .andExpect(jsonPath("$.motifNonCalculable").value(DEFAULT_MOTIF_NON_CALCULABLE))
            .andExpect(jsonPath("$.dateCalcul").value(DEFAULT_DATE_CALCUL.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCoverage() throws Exception {
        // Get the coverage
        restCoverageMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCoverage() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the coverage
        Coverage updatedCoverage = coverageRepository.findById(coverage.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCoverage are not directly saved in db
        em.detach(updatedCoverage);
        updatedCoverage
            .serviceCode(UPDATED_SERVICE_CODE)
            .nature(UPDATED_NATURE)
            .numerateur(UPDATED_NUMERATEUR)
            .denominateur(UPDATED_DENOMINATEUR)
            .taux(UPDATED_TAUX)
            .calculable(UPDATED_CALCULABLE)
            .motifNonCalculable(UPDATED_MOTIF_NON_CALCULABLE)
            .dateCalcul(UPDATED_DATE_CALCUL);
        CoverageDTO coverageDTO = coverageMapper.toDto(updatedCoverage);

        restCoverageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, coverageDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(coverageDTO))
            )
            .andExpect(status().isOk());

        // Validate the Coverage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCoverageToMatchAllProperties(updatedCoverage);
    }

    @Test
    @Transactional
    void putNonExistingCoverage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        coverage.setId(UUID.randomUUID());

        // Create the Coverage
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCoverageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, coverageDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(coverageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Coverage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCoverage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        coverage.setId(UUID.randomUUID());

        // Create the Coverage
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCoverageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(coverageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Coverage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCoverage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        coverage.setId(UUID.randomUUID());

        // Create the Coverage
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCoverageMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(coverageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Coverage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCoverageWithPatch() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the coverage using partial update
        Coverage partialUpdatedCoverage = new Coverage();
        partialUpdatedCoverage.setId(coverage.getId());

        partialUpdatedCoverage
            .serviceCode(UPDATED_SERVICE_CODE)
            .nature(UPDATED_NATURE)
            .motifNonCalculable(UPDATED_MOTIF_NON_CALCULABLE)
            .dateCalcul(UPDATED_DATE_CALCUL);

        restCoverageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCoverage.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCoverage))
            )
            .andExpect(status().isOk());

        // Validate the Coverage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCoverageUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedCoverage, coverage), getPersistedCoverage(coverage));
    }

    @Test
    @Transactional
    void fullUpdateCoverageWithPatch() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the coverage using partial update
        Coverage partialUpdatedCoverage = new Coverage();
        partialUpdatedCoverage.setId(coverage.getId());

        partialUpdatedCoverage
            .serviceCode(UPDATED_SERVICE_CODE)
            .nature(UPDATED_NATURE)
            .numerateur(UPDATED_NUMERATEUR)
            .denominateur(UPDATED_DENOMINATEUR)
            .taux(UPDATED_TAUX)
            .calculable(UPDATED_CALCULABLE)
            .motifNonCalculable(UPDATED_MOTIF_NON_CALCULABLE)
            .dateCalcul(UPDATED_DATE_CALCUL);

        restCoverageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCoverage.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCoverage))
            )
            .andExpect(status().isOk());

        // Validate the Coverage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCoverageUpdatableFieldsEquals(partialUpdatedCoverage, getPersistedCoverage(partialUpdatedCoverage));
    }

    @Test
    @Transactional
    void patchNonExistingCoverage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        coverage.setId(UUID.randomUUID());

        // Create the Coverage
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCoverageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, coverageDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(coverageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Coverage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCoverage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        coverage.setId(UUID.randomUUID());

        // Create the Coverage
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCoverageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(coverageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Coverage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCoverage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        coverage.setId(UUID.randomUUID());

        // Create the Coverage
        CoverageDTO coverageDTO = coverageMapper.toDto(coverage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCoverageMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(coverageDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Coverage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCoverage() throws Exception {
        // Initialize the database
        insertedCoverage = coverageRepository.saveAndFlush(coverage);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the coverage
        restCoverageMockMvc
            .perform(delete(ENTITY_API_URL_ID, coverage.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return coverageRepository.count();
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

    protected Coverage getPersistedCoverage(Coverage coverage) {
        return coverageRepository.findById(coverage.getId()).orElseThrow();
    }

    protected void assertPersistedCoverageToMatchAllProperties(Coverage expectedCoverage) {
        assertCoverageAllPropertiesEquals(expectedCoverage, getPersistedCoverage(expectedCoverage));
    }

    protected void assertPersistedCoverageToMatchUpdatableProperties(Coverage expectedCoverage) {
        assertCoverageAllUpdatablePropertiesEquals(expectedCoverage, getPersistedCoverage(expectedCoverage));
    }
}
