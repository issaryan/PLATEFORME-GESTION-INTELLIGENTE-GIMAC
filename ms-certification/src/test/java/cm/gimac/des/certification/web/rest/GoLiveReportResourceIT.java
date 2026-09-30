package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.GoLiveReportAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.domain.GoLiveReport;
import cm.gimac.des.certification.repository.GoLiveReportRepository;
import cm.gimac.des.certification.service.dto.GoLiveReportDTO;
import cm.gimac.des.certification.service.mapper.GoLiveReportMapper;
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
 * Integration tests for the {@link GoLiveReportResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.certification.security.AuthoritiesConstants.ADMIN)
class GoLiveReportResourceIT {

    private static final String DEFAULT_DOCUMENT_REF = "AAAAAAAAAA";
    private static final String UPDATED_DOCUMENT_REF = "BBBBBBBBBB";

    private static final String DEFAULT_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE = "BBBBBBBBBB";

    private static final Instant DEFAULT_HEURE_BASCULE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_HEURE_BASCULE = Instant.ofEpochMilli(1702411306964L);

    private static final Boolean DEFAULT_DANS_FENETRE = false;
    private static final Boolean UPDATED_DANS_FENETRE = true;

    private static final UUID DEFAULT_DEPOSANT_ID = UUID.randomUUID();
    private static final UUID UPDATED_DEPOSANT_ID = UUID.randomUUID();

    private static final Instant DEFAULT_DATE_DEPOT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_DEPOT = Instant.ofEpochMilli(1702411306964L);

    private static final Boolean DEFAULT_REGULARISE = false;
    private static final Boolean UPDATED_REGULARISE = true;

    private static final String ENTITY_API_URL = "/api/go-live-reports";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GoLiveReportRepository goLiveReportRepository;

    @Autowired
    private GoLiveReportMapper goLiveReportMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGoLiveReportMockMvc;

    private GoLiveReport goLiveReport;

    private GoLiveReport insertedGoLiveReport;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GoLiveReport createEntity(EntityManager em) {
        GoLiveReport goLiveReport = new GoLiveReport()
            .documentRef(DEFAULT_DOCUMENT_REF)
            .empreinte(DEFAULT_EMPREINTE)
            .heureBascule(DEFAULT_HEURE_BASCULE)
            .dansFenetre(DEFAULT_DANS_FENETRE)
            .deposantId(DEFAULT_DEPOSANT_ID)
            .dateDepot(DEFAULT_DATE_DEPOT)
            .regularise(DEFAULT_REGULARISE);
        // Add required entity
        GoLiveDossier goLiveDossier;
        if (TestUtil.findAll(em, GoLiveDossier.class).isEmpty()) {
            goLiveDossier = GoLiveDossierResourceIT.createEntity(em);
            em.persist(goLiveDossier);
            em.flush();
        } else {
            goLiveDossier = TestUtil.findAll(em, GoLiveDossier.class).getFirst();
        }
        goLiveReport.setDossier(goLiveDossier);
        return goLiveReport;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GoLiveReport createUpdatedEntity(EntityManager em) {
        GoLiveReport updatedGoLiveReport = new GoLiveReport()
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinte(UPDATED_EMPREINTE)
            .heureBascule(UPDATED_HEURE_BASCULE)
            .dansFenetre(UPDATED_DANS_FENETRE)
            .deposantId(UPDATED_DEPOSANT_ID)
            .dateDepot(UPDATED_DATE_DEPOT)
            .regularise(UPDATED_REGULARISE);
        // Add required entity
        GoLiveDossier goLiveDossier;
        if (TestUtil.findAll(em, GoLiveDossier.class).isEmpty()) {
            goLiveDossier = GoLiveDossierResourceIT.createUpdatedEntity(em);
            em.persist(goLiveDossier);
            em.flush();
        } else {
            goLiveDossier = TestUtil.findAll(em, GoLiveDossier.class).getFirst();
        }
        updatedGoLiveReport.setDossier(goLiveDossier);
        return updatedGoLiveReport;
    }

    @BeforeEach
    void initTest() {
        goLiveReport = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedGoLiveReport != null) {
            goLiveReportRepository.delete(insertedGoLiveReport);
            insertedGoLiveReport = null;
        }
    }

    @Test
    @Transactional
    void createGoLiveReport() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the GoLiveReport
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);
        var returnedGoLiveReportDTO = om.readValue(
            restGoLiveReportMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveReportDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GoLiveReportDTO.class
        );

        // Validate the GoLiveReport in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGoLiveReport = goLiveReportMapper.toEntity(returnedGoLiveReportDTO);
        assertGoLiveReportUpdatableFieldsEquals(returnedGoLiveReport, getPersistedGoLiveReport(returnedGoLiveReport));

        insertedGoLiveReport = returnedGoLiveReport;
    }

    @Test
    @Transactional
    void createGoLiveReportWithExistingId() throws Exception {
        // Create the GoLiveReport with an existing ID
        insertedGoLiveReport = goLiveReportRepository.saveAndFlush(goLiveReport);
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGoLiveReportMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveReport in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDocumentRefIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        goLiveReport.setDocumentRef(null);

        // Create the GoLiveReport, which fails.
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        restGoLiveReportMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEmpreinteIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        goLiveReport.setEmpreinte(null);

        // Create the GoLiveReport, which fails.
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        restGoLiveReportMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkHeureBasculeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        goLiveReport.setHeureBascule(null);

        // Create the GoLiveReport, which fails.
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        restGoLiveReportMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDansFenetreIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        goLiveReport.setDansFenetre(null);

        // Create the GoLiveReport, which fails.
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        restGoLiveReportMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllGoLiveReports() throws Exception {
        // Initialize the database
        insertedGoLiveReport = goLiveReportRepository.saveAndFlush(goLiveReport);

        // Get all the goLiveReportList
        restGoLiveReportMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(goLiveReport.getId().toString())))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF)))
            .andExpect(jsonPath("$.[*].empreinte").value(hasItem(DEFAULT_EMPREINTE)))
            .andExpect(jsonPath("$.[*].heureBascule").value(hasItem(DEFAULT_HEURE_BASCULE.toString())))
            .andExpect(jsonPath("$.[*].dansFenetre").value(hasItem(DEFAULT_DANS_FENETRE)))
            .andExpect(jsonPath("$.[*].deposantId").value(hasItem(DEFAULT_DEPOSANT_ID.toString())))
            .andExpect(jsonPath("$.[*].dateDepot").value(hasItem(DEFAULT_DATE_DEPOT.toString())))
            .andExpect(jsonPath("$.[*].regularise").value(hasItem(DEFAULT_REGULARISE)));
    }

    @Test
    @Transactional
    void getGoLiveReport() throws Exception {
        // Initialize the database
        insertedGoLiveReport = goLiveReportRepository.saveAndFlush(goLiveReport);

        // Get the goLiveReport
        restGoLiveReportMockMvc
            .perform(get(ENTITY_API_URL_ID, goLiveReport.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(goLiveReport.getId().toString()))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF))
            .andExpect(jsonPath("$.empreinte").value(DEFAULT_EMPREINTE))
            .andExpect(jsonPath("$.heureBascule").value(DEFAULT_HEURE_BASCULE.toString()))
            .andExpect(jsonPath("$.dansFenetre").value(DEFAULT_DANS_FENETRE))
            .andExpect(jsonPath("$.deposantId").value(DEFAULT_DEPOSANT_ID.toString()))
            .andExpect(jsonPath("$.dateDepot").value(DEFAULT_DATE_DEPOT.toString()))
            .andExpect(jsonPath("$.regularise").value(DEFAULT_REGULARISE));
    }

    @Test
    @Transactional
    void getNonExistingGoLiveReport() throws Exception {
        // Get the goLiveReport
        restGoLiveReportMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGoLiveReport() throws Exception {
        // Initialize the database
        insertedGoLiveReport = goLiveReportRepository.saveAndFlush(goLiveReport);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the goLiveReport
        GoLiveReport updatedGoLiveReport = goLiveReportRepository.findById(goLiveReport.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGoLiveReport are not directly saved in db
        em.detach(updatedGoLiveReport);
        updatedGoLiveReport
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinte(UPDATED_EMPREINTE)
            .heureBascule(UPDATED_HEURE_BASCULE)
            .dansFenetre(UPDATED_DANS_FENETRE)
            .deposantId(UPDATED_DEPOSANT_ID)
            .dateDepot(UPDATED_DATE_DEPOT)
            .regularise(UPDATED_REGULARISE);
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(updatedGoLiveReport);

        restGoLiveReportMockMvc
            .perform(
                put(ENTITY_API_URL_ID, goLiveReportDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isOk());

        // Validate the GoLiveReport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGoLiveReportToMatchAllProperties(updatedGoLiveReport);
    }

    @Test
    @Transactional
    void putNonExistingGoLiveReport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveReport.setId(UUID.randomUUID());

        // Create the GoLiveReport
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGoLiveReportMockMvc
            .perform(
                put(ENTITY_API_URL_ID, goLiveReportDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveReport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchGoLiveReport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveReport.setId(UUID.randomUUID());

        // Create the GoLiveReport
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGoLiveReportMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveReport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGoLiveReport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveReport.setId(UUID.randomUUID());

        // Create the GoLiveReport
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGoLiveReportMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the GoLiveReport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateGoLiveReportWithPatch() throws Exception {
        // Initialize the database
        insertedGoLiveReport = goLiveReportRepository.saveAndFlush(goLiveReport);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the goLiveReport using partial update
        GoLiveReport partialUpdatedGoLiveReport = new GoLiveReport();
        partialUpdatedGoLiveReport.setId(goLiveReport.getId());

        partialUpdatedGoLiveReport.empreinte(UPDATED_EMPREINTE).regularise(UPDATED_REGULARISE);

        restGoLiveReportMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGoLiveReport.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGoLiveReport))
            )
            .andExpect(status().isOk());

        // Validate the GoLiveReport in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGoLiveReportUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedGoLiveReport, goLiveReport),
            getPersistedGoLiveReport(goLiveReport)
        );
    }

    @Test
    @Transactional
    void fullUpdateGoLiveReportWithPatch() throws Exception {
        // Initialize the database
        insertedGoLiveReport = goLiveReportRepository.saveAndFlush(goLiveReport);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the goLiveReport using partial update
        GoLiveReport partialUpdatedGoLiveReport = new GoLiveReport();
        partialUpdatedGoLiveReport.setId(goLiveReport.getId());

        partialUpdatedGoLiveReport
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinte(UPDATED_EMPREINTE)
            .heureBascule(UPDATED_HEURE_BASCULE)
            .dansFenetre(UPDATED_DANS_FENETRE)
            .deposantId(UPDATED_DEPOSANT_ID)
            .dateDepot(UPDATED_DATE_DEPOT)
            .regularise(UPDATED_REGULARISE);

        restGoLiveReportMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGoLiveReport.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGoLiveReport))
            )
            .andExpect(status().isOk());

        // Validate the GoLiveReport in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGoLiveReportUpdatableFieldsEquals(partialUpdatedGoLiveReport, getPersistedGoLiveReport(partialUpdatedGoLiveReport));
    }

    @Test
    @Transactional
    void patchNonExistingGoLiveReport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveReport.setId(UUID.randomUUID());

        // Create the GoLiveReport
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGoLiveReportMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, goLiveReportDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveReport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGoLiveReport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveReport.setId(UUID.randomUUID());

        // Create the GoLiveReport
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGoLiveReportMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveReport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGoLiveReport() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveReport.setId(UUID.randomUUID());

        // Create the GoLiveReport
        GoLiveReportDTO goLiveReportDTO = goLiveReportMapper.toDto(goLiveReport);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGoLiveReportMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(goLiveReportDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the GoLiveReport in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteGoLiveReport() throws Exception {
        // Initialize the database
        insertedGoLiveReport = goLiveReportRepository.saveAndFlush(goLiveReport);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the goLiveReport
        restGoLiveReportMockMvc
            .perform(delete(ENTITY_API_URL_ID, goLiveReport.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return goLiveReportRepository.count();
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

    protected GoLiveReport getPersistedGoLiveReport(GoLiveReport goLiveReport) {
        return goLiveReportRepository.findById(goLiveReport.getId()).orElseThrow();
    }

    protected void assertPersistedGoLiveReportToMatchAllProperties(GoLiveReport expectedGoLiveReport) {
        assertGoLiveReportAllPropertiesEquals(expectedGoLiveReport, getPersistedGoLiveReport(expectedGoLiveReport));
    }

    protected void assertPersistedGoLiveReportToMatchUpdatableProperties(GoLiveReport expectedGoLiveReport) {
        assertGoLiveReportAllUpdatablePropertiesEquals(expectedGoLiveReport, getPersistedGoLiveReport(expectedGoLiveReport));
    }
}
