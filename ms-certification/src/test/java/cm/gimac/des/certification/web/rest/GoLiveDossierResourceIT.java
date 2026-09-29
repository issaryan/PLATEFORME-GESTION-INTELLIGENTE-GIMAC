package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.GoLiveDossierAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.domain.enumeration.EtatDossier;
import cm.gimac.des.certification.domain.enumeration.EtatScellement;
import cm.gimac.des.certification.repository.GoLiveDossierRepository;
import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
import cm.gimac.des.certification.service.mapper.GoLiveDossierMapper;
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
 * Integration tests for the {@link GoLiveDossierResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class GoLiveDossierResourceIT {

    private static final UUID DEFAULT_LIAISON_ID = UUID.randomUUID();
    private static final UUID UPDATED_LIAISON_ID = UUID.randomUUID();

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final EtatDossier DEFAULT_ETAT = EtatDossier.OUVERT;
    private static final EtatDossier UPDATED_ETAT = EtatDossier.AUTORISE;

    private static final Instant DEFAULT_DATE_OUVERTURE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_OUVERTURE = Instant.ofEpochMilli(1702411306964L);

    private static final Instant DEFAULT_DATE_AUTORISATION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_AUTORISATION = Instant.ofEpochMilli(1702411306964L);

    private static final Instant DEFAULT_DATE_MISE_EN_SERVICE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_MISE_EN_SERVICE = Instant.ofEpochMilli(1702411306964L);

    private static final Instant DEFAULT_DATE_COMPLETUDE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_COMPLETUDE = Instant.ofEpochMilli(1702411306964L);

    private static final EtatScellement DEFAULT_ETAT_SCELLEMENT = EtatScellement.EN_ATTENTE;
    private static final EtatScellement UPDATED_ETAT_SCELLEMENT = EtatScellement.SCELLE;

    private static final String DEFAULT_REFERENCE_FICHE_CHANGEMENT = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE_FICHE_CHANGEMENT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/go-live-dossiers";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GoLiveDossierRepository goLiveDossierRepository;

    @Autowired
    private GoLiveDossierMapper goLiveDossierMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGoLiveDossierMockMvc;

    private GoLiveDossier goLiveDossier;

    private GoLiveDossier insertedGoLiveDossier;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GoLiveDossier createEntity(EntityManager em) {
        GoLiveDossier goLiveDossier = new GoLiveDossier()
            .liaisonId(DEFAULT_LIAISON_ID)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .etat(DEFAULT_ETAT)
            .dateOuverture(DEFAULT_DATE_OUVERTURE)
            .dateAutorisation(DEFAULT_DATE_AUTORISATION)
            .dateMiseEnService(DEFAULT_DATE_MISE_EN_SERVICE)
            .dateCompletude(DEFAULT_DATE_COMPLETUDE)
            .etatScellement(DEFAULT_ETAT_SCELLEMENT)
            .referenceFicheChangement(DEFAULT_REFERENCE_FICHE_CHANGEMENT);
        // Add required entity
        CertificationCircuit certificationCircuit;
        if (TestUtil.findAll(em, CertificationCircuit.class).isEmpty()) {
            certificationCircuit = CertificationCircuitResourceIT.createEntity();
            em.persist(certificationCircuit);
            em.flush();
        } else {
            certificationCircuit = TestUtil.findAll(em, CertificationCircuit.class).getFirst();
        }
        goLiveDossier.setCircuit(certificationCircuit);
        return goLiveDossier;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GoLiveDossier createUpdatedEntity(EntityManager em) {
        GoLiveDossier updatedGoLiveDossier = new GoLiveDossier()
            .liaisonId(UPDATED_LIAISON_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .etat(UPDATED_ETAT)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .dateAutorisation(UPDATED_DATE_AUTORISATION)
            .dateMiseEnService(UPDATED_DATE_MISE_EN_SERVICE)
            .dateCompletude(UPDATED_DATE_COMPLETUDE)
            .etatScellement(UPDATED_ETAT_SCELLEMENT)
            .referenceFicheChangement(UPDATED_REFERENCE_FICHE_CHANGEMENT);
        // Add required entity
        CertificationCircuit certificationCircuit;
        if (TestUtil.findAll(em, CertificationCircuit.class).isEmpty()) {
            certificationCircuit = CertificationCircuitResourceIT.createUpdatedEntity();
            em.persist(certificationCircuit);
            em.flush();
        } else {
            certificationCircuit = TestUtil.findAll(em, CertificationCircuit.class).getFirst();
        }
        updatedGoLiveDossier.setCircuit(certificationCircuit);
        return updatedGoLiveDossier;
    }

    @BeforeEach
    void initTest() {
        goLiveDossier = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedGoLiveDossier != null) {
            goLiveDossierRepository.delete(insertedGoLiveDossier);
            insertedGoLiveDossier = null;
        }
    }

    @Test
    @Transactional
    void createGoLiveDossier() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the GoLiveDossier
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);
        var returnedGoLiveDossierDTO = om.readValue(
            restGoLiveDossierMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(goLiveDossierDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GoLiveDossierDTO.class
        );

        // Validate the GoLiveDossier in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGoLiveDossier = goLiveDossierMapper.toEntity(returnedGoLiveDossierDTO);
        assertGoLiveDossierUpdatableFieldsEquals(returnedGoLiveDossier, getPersistedGoLiveDossier(returnedGoLiveDossier));

        insertedGoLiveDossier = returnedGoLiveDossier;
    }

    @Test
    @Transactional
    void createGoLiveDossierWithExistingId() throws Exception {
        // Create the GoLiveDossier with an existing ID
        insertedGoLiveDossier = goLiveDossierRepository.saveAndFlush(goLiveDossier);
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGoLiveDossierMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveDossier in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkLiaisonIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        goLiveDossier.setLiaisonId(null);

        // Create the GoLiveDossier, which fails.
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        restGoLiveDossierMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        goLiveDossier.setEtat(null);

        // Create the GoLiveDossier, which fails.
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        restGoLiveDossierMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllGoLiveDossiers() throws Exception {
        // Initialize the database
        insertedGoLiveDossier = goLiveDossierRepository.saveAndFlush(goLiveDossier);

        // Get all the goLiveDossierList
        restGoLiveDossierMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(goLiveDossier.getId().toString())))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].dateOuverture").value(hasItem(DEFAULT_DATE_OUVERTURE.toString())))
            .andExpect(jsonPath("$.[*].dateAutorisation").value(hasItem(DEFAULT_DATE_AUTORISATION.toString())))
            .andExpect(jsonPath("$.[*].dateMiseEnService").value(hasItem(DEFAULT_DATE_MISE_EN_SERVICE.toString())))
            .andExpect(jsonPath("$.[*].dateCompletude").value(hasItem(DEFAULT_DATE_COMPLETUDE.toString())))
            .andExpect(jsonPath("$.[*].etatScellement").value(hasItem(DEFAULT_ETAT_SCELLEMENT.toString())))
            .andExpect(jsonPath("$.[*].referenceFicheChangement").value(hasItem(DEFAULT_REFERENCE_FICHE_CHANGEMENT)));
    }

    @Test
    @Transactional
    void getGoLiveDossier() throws Exception {
        // Initialize the database
        insertedGoLiveDossier = goLiveDossierRepository.saveAndFlush(goLiveDossier);

        // Get the goLiveDossier
        restGoLiveDossierMockMvc
            .perform(get(ENTITY_API_URL_ID, goLiveDossier.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(goLiveDossier.getId().toString()))
            .andExpect(jsonPath("$.liaisonId").value(DEFAULT_LIAISON_ID.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.dateOuverture").value(DEFAULT_DATE_OUVERTURE.toString()))
            .andExpect(jsonPath("$.dateAutorisation").value(DEFAULT_DATE_AUTORISATION.toString()))
            .andExpect(jsonPath("$.dateMiseEnService").value(DEFAULT_DATE_MISE_EN_SERVICE.toString()))
            .andExpect(jsonPath("$.dateCompletude").value(DEFAULT_DATE_COMPLETUDE.toString()))
            .andExpect(jsonPath("$.etatScellement").value(DEFAULT_ETAT_SCELLEMENT.toString()))
            .andExpect(jsonPath("$.referenceFicheChangement").value(DEFAULT_REFERENCE_FICHE_CHANGEMENT));
    }

    @Test
    @Transactional
    void getNonExistingGoLiveDossier() throws Exception {
        // Get the goLiveDossier
        restGoLiveDossierMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGoLiveDossier() throws Exception {
        // Initialize the database
        insertedGoLiveDossier = goLiveDossierRepository.saveAndFlush(goLiveDossier);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the goLiveDossier
        GoLiveDossier updatedGoLiveDossier = goLiveDossierRepository.findById(goLiveDossier.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGoLiveDossier are not directly saved in db
        em.detach(updatedGoLiveDossier);
        updatedGoLiveDossier
            .liaisonId(UPDATED_LIAISON_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .etat(UPDATED_ETAT)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .dateAutorisation(UPDATED_DATE_AUTORISATION)
            .dateMiseEnService(UPDATED_DATE_MISE_EN_SERVICE)
            .dateCompletude(UPDATED_DATE_COMPLETUDE)
            .etatScellement(UPDATED_ETAT_SCELLEMENT)
            .referenceFicheChangement(UPDATED_REFERENCE_FICHE_CHANGEMENT);
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(updatedGoLiveDossier);

        restGoLiveDossierMockMvc
            .perform(
                put(ENTITY_API_URL_ID, goLiveDossierDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isOk());

        // Validate the GoLiveDossier in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGoLiveDossierToMatchAllProperties(updatedGoLiveDossier);
    }

    @Test
    @Transactional
    void putNonExistingGoLiveDossier() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveDossier.setId(UUID.randomUUID());

        // Create the GoLiveDossier
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGoLiveDossierMockMvc
            .perform(
                put(ENTITY_API_URL_ID, goLiveDossierDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveDossier in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchGoLiveDossier() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveDossier.setId(UUID.randomUUID());

        // Create the GoLiveDossier
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGoLiveDossierMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveDossier in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGoLiveDossier() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveDossier.setId(UUID.randomUUID());

        // Create the GoLiveDossier
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGoLiveDossierMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the GoLiveDossier in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateGoLiveDossierWithPatch() throws Exception {
        // Initialize the database
        insertedGoLiveDossier = goLiveDossierRepository.saveAndFlush(goLiveDossier);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the goLiveDossier using partial update
        GoLiveDossier partialUpdatedGoLiveDossier = new GoLiveDossier();
        partialUpdatedGoLiveDossier.setId(goLiveDossier.getId());

        partialUpdatedGoLiveDossier
            .liaisonId(UPDATED_LIAISON_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .etat(UPDATED_ETAT)
            .dateMiseEnService(UPDATED_DATE_MISE_EN_SERVICE)
            .dateCompletude(UPDATED_DATE_COMPLETUDE)
            .etatScellement(UPDATED_ETAT_SCELLEMENT)
            .referenceFicheChangement(UPDATED_REFERENCE_FICHE_CHANGEMENT);

        restGoLiveDossierMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGoLiveDossier.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGoLiveDossier))
            )
            .andExpect(status().isOk());

        // Validate the GoLiveDossier in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGoLiveDossierUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedGoLiveDossier, goLiveDossier),
            getPersistedGoLiveDossier(goLiveDossier)
        );
    }

    @Test
    @Transactional
    void fullUpdateGoLiveDossierWithPatch() throws Exception {
        // Initialize the database
        insertedGoLiveDossier = goLiveDossierRepository.saveAndFlush(goLiveDossier);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the goLiveDossier using partial update
        GoLiveDossier partialUpdatedGoLiveDossier = new GoLiveDossier();
        partialUpdatedGoLiveDossier.setId(goLiveDossier.getId());

        partialUpdatedGoLiveDossier
            .liaisonId(UPDATED_LIAISON_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .etat(UPDATED_ETAT)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .dateAutorisation(UPDATED_DATE_AUTORISATION)
            .dateMiseEnService(UPDATED_DATE_MISE_EN_SERVICE)
            .dateCompletude(UPDATED_DATE_COMPLETUDE)
            .etatScellement(UPDATED_ETAT_SCELLEMENT)
            .referenceFicheChangement(UPDATED_REFERENCE_FICHE_CHANGEMENT);

        restGoLiveDossierMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGoLiveDossier.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGoLiveDossier))
            )
            .andExpect(status().isOk());

        // Validate the GoLiveDossier in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGoLiveDossierUpdatableFieldsEquals(partialUpdatedGoLiveDossier, getPersistedGoLiveDossier(partialUpdatedGoLiveDossier));
    }

    @Test
    @Transactional
    void patchNonExistingGoLiveDossier() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveDossier.setId(UUID.randomUUID());

        // Create the GoLiveDossier
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGoLiveDossierMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, goLiveDossierDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveDossier in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGoLiveDossier() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveDossier.setId(UUID.randomUUID());

        // Create the GoLiveDossier
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGoLiveDossierMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GoLiveDossier in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGoLiveDossier() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        goLiveDossier.setId(UUID.randomUUID());

        // Create the GoLiveDossier
        GoLiveDossierDTO goLiveDossierDTO = goLiveDossierMapper.toDto(goLiveDossier);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGoLiveDossierMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(goLiveDossierDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the GoLiveDossier in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteGoLiveDossier() throws Exception {
        // Initialize the database
        insertedGoLiveDossier = goLiveDossierRepository.saveAndFlush(goLiveDossier);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the goLiveDossier
        restGoLiveDossierMockMvc
            .perform(delete(ENTITY_API_URL_ID, goLiveDossier.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return goLiveDossierRepository.count();
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

    protected GoLiveDossier getPersistedGoLiveDossier(GoLiveDossier goLiveDossier) {
        return goLiveDossierRepository.findById(goLiveDossier.getId()).orElseThrow();
    }

    protected void assertPersistedGoLiveDossierToMatchAllProperties(GoLiveDossier expectedGoLiveDossier) {
        assertGoLiveDossierAllPropertiesEquals(expectedGoLiveDossier, getPersistedGoLiveDossier(expectedGoLiveDossier));
    }

    protected void assertPersistedGoLiveDossierToMatchUpdatableProperties(GoLiveDossier expectedGoLiveDossier) {
        assertGoLiveDossierAllUpdatablePropertiesEquals(expectedGoLiveDossier, getPersistedGoLiveDossier(expectedGoLiveDossier));
    }
}
