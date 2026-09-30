package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.CertificationCircuitAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CertificationCircuit;
import cm.gimac.des.certification.domain.enumeration.EtatCircuit;
import cm.gimac.des.certification.repository.CertificationCircuitRepository;
import cm.gimac.des.certification.service.dto.CertificationCircuitDTO;
import cm.gimac.des.certification.service.mapper.CertificationCircuitMapper;
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
 * Integration tests for the {@link CertificationCircuitResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.certification.security.AuthoritiesConstants.ADMIN)
class CertificationCircuitResourceIT {

    private static final UUID DEFAULT_LIAISON_ID = UUID.randomUUID();
    private static final UUID UPDATED_LIAISON_ID = UUID.randomUUID();

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final Integer DEFAULT_CYCLE = 1;
    private static final Integer UPDATED_CYCLE = 2;

    private static final String DEFAULT_SOCLE_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_SOCLE_EMPREINTE = "BBBBBBBBBB";

    private static final EtatCircuit DEFAULT_ETAT = EtatCircuit.OUVERT;
    private static final EtatCircuit UPDATED_ETAT = EtatCircuit.INTERROMPU;

    private static final Instant DEFAULT_DATE_OUVERTURE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_OUVERTURE = Instant.ofEpochMilli(1702411306964L);

    private static final Instant DEFAULT_DATE_FIN = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_FIN = Instant.ofEpochMilli(1702411306964L);

    private static final String ENTITY_API_URL = "/api/certification-circuits";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CertificationCircuitRepository certificationCircuitRepository;

    @Autowired
    private CertificationCircuitMapper certificationCircuitMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCertificationCircuitMockMvc;

    private CertificationCircuit certificationCircuit;

    private CertificationCircuit insertedCertificationCircuit;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CertificationCircuit createEntity() {
        return new CertificationCircuit()
            .liaisonId(DEFAULT_LIAISON_ID)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .cycle(DEFAULT_CYCLE)
            .socleEmpreinte(DEFAULT_SOCLE_EMPREINTE)
            .etat(DEFAULT_ETAT)
            .dateOuverture(DEFAULT_DATE_OUVERTURE)
            .dateFin(DEFAULT_DATE_FIN);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CertificationCircuit createUpdatedEntity() {
        return new CertificationCircuit()
            .liaisonId(UPDATED_LIAISON_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .cycle(UPDATED_CYCLE)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .etat(UPDATED_ETAT)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .dateFin(UPDATED_DATE_FIN);
    }

    @BeforeEach
    void initTest() {
        certificationCircuit = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCertificationCircuit != null) {
            certificationCircuitRepository.delete(insertedCertificationCircuit);
            insertedCertificationCircuit = null;
        }
    }

    @Test
    @Transactional
    void createCertificationCircuit() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the CertificationCircuit
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);
        var returnedCertificationCircuitDTO = om.readValue(
            restCertificationCircuitMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(certificationCircuitDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CertificationCircuitDTO.class
        );

        // Validate the CertificationCircuit in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCertificationCircuit = certificationCircuitMapper.toEntity(returnedCertificationCircuitDTO);
        assertCertificationCircuitUpdatableFieldsEquals(
            returnedCertificationCircuit,
            getPersistedCertificationCircuit(returnedCertificationCircuit)
        );

        insertedCertificationCircuit = returnedCertificationCircuit;
    }

    @Test
    @Transactional
    void createCertificationCircuitWithExistingId() throws Exception {
        // Create the CertificationCircuit with an existing ID
        insertedCertificationCircuit = certificationCircuitRepository.saveAndFlush(certificationCircuit);
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCertificationCircuitMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationCircuit in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkLiaisonIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        certificationCircuit.setLiaisonId(null);

        // Create the CertificationCircuit, which fails.
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        restCertificationCircuitMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCycleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        certificationCircuit.setCycle(null);

        // Create the CertificationCircuit, which fails.
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        restCertificationCircuitMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSocleEmpreinteIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        certificationCircuit.setSocleEmpreinte(null);

        // Create the CertificationCircuit, which fails.
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        restCertificationCircuitMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        certificationCircuit.setEtat(null);

        // Create the CertificationCircuit, which fails.
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        restCertificationCircuitMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCertificationCircuits() throws Exception {
        // Initialize the database
        insertedCertificationCircuit = certificationCircuitRepository.saveAndFlush(certificationCircuit);

        // Get all the certificationCircuitList
        restCertificationCircuitMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(certificationCircuit.getId().toString())))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].cycle").value(hasItem(DEFAULT_CYCLE)))
            .andExpect(jsonPath("$.[*].socleEmpreinte").value(hasItem(DEFAULT_SOCLE_EMPREINTE)))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].dateOuverture").value(hasItem(DEFAULT_DATE_OUVERTURE.toString())))
            .andExpect(jsonPath("$.[*].dateFin").value(hasItem(DEFAULT_DATE_FIN.toString())));
    }

    @Test
    @Transactional
    void getCertificationCircuit() throws Exception {
        // Initialize the database
        insertedCertificationCircuit = certificationCircuitRepository.saveAndFlush(certificationCircuit);

        // Get the certificationCircuit
        restCertificationCircuitMockMvc
            .perform(get(ENTITY_API_URL_ID, certificationCircuit.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(certificationCircuit.getId().toString()))
            .andExpect(jsonPath("$.liaisonId").value(DEFAULT_LIAISON_ID.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.cycle").value(DEFAULT_CYCLE))
            .andExpect(jsonPath("$.socleEmpreinte").value(DEFAULT_SOCLE_EMPREINTE))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.dateOuverture").value(DEFAULT_DATE_OUVERTURE.toString()))
            .andExpect(jsonPath("$.dateFin").value(DEFAULT_DATE_FIN.toString()));
    }

    @Test
    @Transactional
    void getNonExistingCertificationCircuit() throws Exception {
        // Get the certificationCircuit
        restCertificationCircuitMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCertificationCircuit() throws Exception {
        // Initialize the database
        insertedCertificationCircuit = certificationCircuitRepository.saveAndFlush(certificationCircuit);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the certificationCircuit
        CertificationCircuit updatedCertificationCircuit = certificationCircuitRepository
            .findById(certificationCircuit.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedCertificationCircuit are not directly saved in db
        em.detach(updatedCertificationCircuit);
        updatedCertificationCircuit
            .liaisonId(UPDATED_LIAISON_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .cycle(UPDATED_CYCLE)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .etat(UPDATED_ETAT)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .dateFin(UPDATED_DATE_FIN);
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(updatedCertificationCircuit);

        restCertificationCircuitMockMvc
            .perform(
                put(ENTITY_API_URL_ID, certificationCircuitDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isOk());

        // Validate the CertificationCircuit in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCertificationCircuitToMatchAllProperties(updatedCertificationCircuit);
    }

    @Test
    @Transactional
    void putNonExistingCertificationCircuit() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationCircuit.setId(UUID.randomUUID());

        // Create the CertificationCircuit
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCertificationCircuitMockMvc
            .perform(
                put(ENTITY_API_URL_ID, certificationCircuitDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationCircuit in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCertificationCircuit() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationCircuit.setId(UUID.randomUUID());

        // Create the CertificationCircuit
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCertificationCircuitMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationCircuit in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCertificationCircuit() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationCircuit.setId(UUID.randomUUID());

        // Create the CertificationCircuit
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCertificationCircuitMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CertificationCircuit in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCertificationCircuitWithPatch() throws Exception {
        // Initialize the database
        insertedCertificationCircuit = certificationCircuitRepository.saveAndFlush(certificationCircuit);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the certificationCircuit using partial update
        CertificationCircuit partialUpdatedCertificationCircuit = new CertificationCircuit();
        partialUpdatedCertificationCircuit.setId(certificationCircuit.getId());

        partialUpdatedCertificationCircuit.liaisonId(UPDATED_LIAISON_ID).etat(UPDATED_ETAT).dateOuverture(UPDATED_DATE_OUVERTURE);

        restCertificationCircuitMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCertificationCircuit.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCertificationCircuit))
            )
            .andExpect(status().isOk());

        // Validate the CertificationCircuit in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCertificationCircuitUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCertificationCircuit, certificationCircuit),
            getPersistedCertificationCircuit(certificationCircuit)
        );
    }

    @Test
    @Transactional
    void fullUpdateCertificationCircuitWithPatch() throws Exception {
        // Initialize the database
        insertedCertificationCircuit = certificationCircuitRepository.saveAndFlush(certificationCircuit);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the certificationCircuit using partial update
        CertificationCircuit partialUpdatedCertificationCircuit = new CertificationCircuit();
        partialUpdatedCertificationCircuit.setId(certificationCircuit.getId());

        partialUpdatedCertificationCircuit
            .liaisonId(UPDATED_LIAISON_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .cycle(UPDATED_CYCLE)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .etat(UPDATED_ETAT)
            .dateOuverture(UPDATED_DATE_OUVERTURE)
            .dateFin(UPDATED_DATE_FIN);

        restCertificationCircuitMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCertificationCircuit.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCertificationCircuit))
            )
            .andExpect(status().isOk());

        // Validate the CertificationCircuit in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCertificationCircuitUpdatableFieldsEquals(
            partialUpdatedCertificationCircuit,
            getPersistedCertificationCircuit(partialUpdatedCertificationCircuit)
        );
    }

    @Test
    @Transactional
    void patchNonExistingCertificationCircuit() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationCircuit.setId(UUID.randomUUID());

        // Create the CertificationCircuit
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCertificationCircuitMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, certificationCircuitDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationCircuit in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCertificationCircuit() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationCircuit.setId(UUID.randomUUID());

        // Create the CertificationCircuit
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCertificationCircuitMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationCircuit in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCertificationCircuit() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationCircuit.setId(UUID.randomUUID());

        // Create the CertificationCircuit
        CertificationCircuitDTO certificationCircuitDTO = certificationCircuitMapper.toDto(certificationCircuit);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCertificationCircuitMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(certificationCircuitDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CertificationCircuit in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCertificationCircuit() throws Exception {
        // Initialize the database
        insertedCertificationCircuit = certificationCircuitRepository.saveAndFlush(certificationCircuit);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the certificationCircuit
        restCertificationCircuitMockMvc
            .perform(delete(ENTITY_API_URL_ID, certificationCircuit.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return certificationCircuitRepository.count();
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

    protected CertificationCircuit getPersistedCertificationCircuit(CertificationCircuit certificationCircuit) {
        return certificationCircuitRepository.findById(certificationCircuit.getId()).orElseThrow();
    }

    protected void assertPersistedCertificationCircuitToMatchAllProperties(CertificationCircuit expectedCertificationCircuit) {
        assertCertificationCircuitAllPropertiesEquals(
            expectedCertificationCircuit,
            getPersistedCertificationCircuit(expectedCertificationCircuit)
        );
    }

    protected void assertPersistedCertificationCircuitToMatchUpdatableProperties(CertificationCircuit expectedCertificationCircuit) {
        assertCertificationCircuitAllUpdatablePropertiesEquals(
            expectedCertificationCircuit,
            getPersistedCertificationCircuit(expectedCertificationCircuit)
        );
    }
}
