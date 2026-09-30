package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.CertificationRegisterEntryAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CertificationRegisterEntry;
import cm.gimac.des.certification.domain.enumeration.EtatAttestation;
import cm.gimac.des.certification.repository.CertificationRegisterEntryRepository;
import cm.gimac.des.certification.service.dto.CertificationRegisterEntryDTO;
import cm.gimac.des.certification.service.mapper.CertificationRegisterEntryMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
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
 * Integration tests for the {@link CertificationRegisterEntryResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.certification.security.AuthoritiesConstants.ADMIN)
class CertificationRegisterEntryResourceIT {

    private static final String DEFAULT_NUMERO_ATTESTATION = "AAAAAAAAAA";
    private static final String UPDATED_NUMERO_ATTESTATION = "BBBBBBBBBB";

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_TYPE_PROJET = "AAAAAAAAAA";
    private static final String UPDATED_TYPE_PROJET = "BBBBBBBBBB";

    private static final String DEFAULT_SERVICES = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_SERVICES = "[\"B\"]";

    private static final Instant DEFAULT_DATE_EMISSION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_EMISSION = Instant.ofEpochMilli(1702411306964L);

    private static final LocalDate DEFAULT_ECHEANCE_RENOUVELLEMENT = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_ECHEANCE_RENOUVELLEMENT = LocalDate.parse("2023-12-12");

    private static final EtatAttestation DEFAULT_ETAT = EtatAttestation.EN_VIGUEUR;
    private static final EtatAttestation UPDATED_ETAT = EtatAttestation.SUSPENDUE;

    private static final UUID DEFAULT_PRECEDENTE_ID = UUID.randomUUID();
    private static final UUID UPDATED_PRECEDENTE_ID = UUID.randomUUID();

    private static final String DEFAULT_DERNIER_SEUIL_NOTIFIE = "AAAAAAAAAA";
    private static final String UPDATED_DERNIER_SEUIL_NOTIFIE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/certification-register-entries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CertificationRegisterEntryRepository certificationRegisterEntryRepository;

    @Autowired
    private CertificationRegisterEntryMapper certificationRegisterEntryMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCertificationRegisterEntryMockMvc;

    private CertificationRegisterEntry certificationRegisterEntry;

    private CertificationRegisterEntry insertedCertificationRegisterEntry;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CertificationRegisterEntry createEntity() {
        return new CertificationRegisterEntry()
            .numeroAttestation(DEFAULT_NUMERO_ATTESTATION)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .typeProjet(DEFAULT_TYPE_PROJET)
            .services(DEFAULT_SERVICES)
            .dateEmission(DEFAULT_DATE_EMISSION)
            .echeanceRenouvellement(DEFAULT_ECHEANCE_RENOUVELLEMENT)
            .etat(DEFAULT_ETAT)
            .precedenteId(DEFAULT_PRECEDENTE_ID)
            .dernierSeuilNotifie(DEFAULT_DERNIER_SEUIL_NOTIFIE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CertificationRegisterEntry createUpdatedEntity() {
        return new CertificationRegisterEntry()
            .numeroAttestation(UPDATED_NUMERO_ATTESTATION)
            .institutionId(UPDATED_INSTITUTION_ID)
            .typeProjet(UPDATED_TYPE_PROJET)
            .services(UPDATED_SERVICES)
            .dateEmission(UPDATED_DATE_EMISSION)
            .echeanceRenouvellement(UPDATED_ECHEANCE_RENOUVELLEMENT)
            .etat(UPDATED_ETAT)
            .precedenteId(UPDATED_PRECEDENTE_ID)
            .dernierSeuilNotifie(UPDATED_DERNIER_SEUIL_NOTIFIE);
    }

    @BeforeEach
    void initTest() {
        certificationRegisterEntry = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCertificationRegisterEntry != null) {
            certificationRegisterEntryRepository.delete(insertedCertificationRegisterEntry);
            insertedCertificationRegisterEntry = null;
        }
    }

    @Test
    @Transactional
    void createCertificationRegisterEntry() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the CertificationRegisterEntry
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);
        var returnedCertificationRegisterEntryDTO = om.readValue(
            restCertificationRegisterEntryMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CertificationRegisterEntryDTO.class
        );

        // Validate the CertificationRegisterEntry in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCertificationRegisterEntry = certificationRegisterEntryMapper.toEntity(returnedCertificationRegisterEntryDTO);
        assertCertificationRegisterEntryUpdatableFieldsEquals(
            returnedCertificationRegisterEntry,
            getPersistedCertificationRegisterEntry(returnedCertificationRegisterEntry)
        );

        insertedCertificationRegisterEntry = returnedCertificationRegisterEntry;
    }

    @Test
    @Transactional
    void createCertificationRegisterEntryWithExistingId() throws Exception {
        // Create the CertificationRegisterEntry with an existing ID
        insertedCertificationRegisterEntry = certificationRegisterEntryRepository.saveAndFlush(certificationRegisterEntry);
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restCertificationRegisterEntryMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationRegisterEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNumeroAttestationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        certificationRegisterEntry.setNumeroAttestation(null);

        // Create the CertificationRegisterEntry, which fails.
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        restCertificationRegisterEntryMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkInstitutionIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        certificationRegisterEntry.setInstitutionId(null);

        // Create the CertificationRegisterEntry, which fails.
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        restCertificationRegisterEntryMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllCertificationRegisterEntries() throws Exception {
        // Initialize the database
        insertedCertificationRegisterEntry = certificationRegisterEntryRepository.saveAndFlush(certificationRegisterEntry);

        // Get all the certificationRegisterEntryList
        restCertificationRegisterEntryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(certificationRegisterEntry.getId().toString())))
            .andExpect(jsonPath("$.[*].numeroAttestation").value(hasItem(DEFAULT_NUMERO_ATTESTATION)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].typeProjet").value(hasItem(DEFAULT_TYPE_PROJET)))
            .andExpect(jsonPath("$.[*].services").value(hasItem(DEFAULT_SERVICES)))
            .andExpect(jsonPath("$.[*].dateEmission").value(hasItem(DEFAULT_DATE_EMISSION.toString())))
            .andExpect(jsonPath("$.[*].echeanceRenouvellement").value(hasItem(DEFAULT_ECHEANCE_RENOUVELLEMENT.toString())))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].precedenteId").value(hasItem(DEFAULT_PRECEDENTE_ID.toString())))
            .andExpect(jsonPath("$.[*].dernierSeuilNotifie").value(hasItem(DEFAULT_DERNIER_SEUIL_NOTIFIE)));
    }

    @Test
    @Transactional
    void getCertificationRegisterEntry() throws Exception {
        // Initialize the database
        insertedCertificationRegisterEntry = certificationRegisterEntryRepository.saveAndFlush(certificationRegisterEntry);

        // Get the certificationRegisterEntry
        restCertificationRegisterEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, certificationRegisterEntry.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(certificationRegisterEntry.getId().toString()))
            .andExpect(jsonPath("$.numeroAttestation").value(DEFAULT_NUMERO_ATTESTATION))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.typeProjet").value(DEFAULT_TYPE_PROJET))
            .andExpect(jsonPath("$.services").value(DEFAULT_SERVICES))
            .andExpect(jsonPath("$.dateEmission").value(DEFAULT_DATE_EMISSION.toString()))
            .andExpect(jsonPath("$.echeanceRenouvellement").value(DEFAULT_ECHEANCE_RENOUVELLEMENT.toString()))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.precedenteId").value(DEFAULT_PRECEDENTE_ID.toString()))
            .andExpect(jsonPath("$.dernierSeuilNotifie").value(DEFAULT_DERNIER_SEUIL_NOTIFIE));
    }

    @Test
    @Transactional
    void getNonExistingCertificationRegisterEntry() throws Exception {
        // Get the certificationRegisterEntry
        restCertificationRegisterEntryMockMvc
            .perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCertificationRegisterEntry() throws Exception {
        // Initialize the database
        insertedCertificationRegisterEntry = certificationRegisterEntryRepository.saveAndFlush(certificationRegisterEntry);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the certificationRegisterEntry
        CertificationRegisterEntry updatedCertificationRegisterEntry = certificationRegisterEntryRepository
            .findById(certificationRegisterEntry.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedCertificationRegisterEntry are not directly saved in db
        em.detach(updatedCertificationRegisterEntry);
        updatedCertificationRegisterEntry
            .numeroAttestation(UPDATED_NUMERO_ATTESTATION)
            .institutionId(UPDATED_INSTITUTION_ID)
            .typeProjet(UPDATED_TYPE_PROJET)
            .services(UPDATED_SERVICES)
            .dateEmission(UPDATED_DATE_EMISSION)
            .echeanceRenouvellement(UPDATED_ECHEANCE_RENOUVELLEMENT)
            .etat(UPDATED_ETAT)
            .precedenteId(UPDATED_PRECEDENTE_ID)
            .dernierSeuilNotifie(UPDATED_DERNIER_SEUIL_NOTIFIE);
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(
            updatedCertificationRegisterEntry
        );

        restCertificationRegisterEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, certificationRegisterEntryDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isOk());

        // Validate the CertificationRegisterEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCertificationRegisterEntryToMatchAllProperties(updatedCertificationRegisterEntry);
    }

    @Test
    @Transactional
    void putNonExistingCertificationRegisterEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationRegisterEntry.setId(UUID.randomUUID());

        // Create the CertificationRegisterEntry
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCertificationRegisterEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, certificationRegisterEntryDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationRegisterEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchCertificationRegisterEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationRegisterEntry.setId(UUID.randomUUID());

        // Create the CertificationRegisterEntry
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCertificationRegisterEntryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationRegisterEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCertificationRegisterEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationRegisterEntry.setId(UUID.randomUUID());

        // Create the CertificationRegisterEntry
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCertificationRegisterEntryMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CertificationRegisterEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateCertificationRegisterEntryWithPatch() throws Exception {
        // Initialize the database
        insertedCertificationRegisterEntry = certificationRegisterEntryRepository.saveAndFlush(certificationRegisterEntry);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the certificationRegisterEntry using partial update
        CertificationRegisterEntry partialUpdatedCertificationRegisterEntry = new CertificationRegisterEntry();
        partialUpdatedCertificationRegisterEntry.setId(certificationRegisterEntry.getId());

        partialUpdatedCertificationRegisterEntry
            .numeroAttestation(UPDATED_NUMERO_ATTESTATION)
            .institutionId(UPDATED_INSTITUTION_ID)
            .dateEmission(UPDATED_DATE_EMISSION)
            .etat(UPDATED_ETAT)
            .precedenteId(UPDATED_PRECEDENTE_ID)
            .dernierSeuilNotifie(UPDATED_DERNIER_SEUIL_NOTIFIE);

        restCertificationRegisterEntryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCertificationRegisterEntry.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCertificationRegisterEntry))
            )
            .andExpect(status().isOk());

        // Validate the CertificationRegisterEntry in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCertificationRegisterEntryUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCertificationRegisterEntry, certificationRegisterEntry),
            getPersistedCertificationRegisterEntry(certificationRegisterEntry)
        );
    }

    @Test
    @Transactional
    void fullUpdateCertificationRegisterEntryWithPatch() throws Exception {
        // Initialize the database
        insertedCertificationRegisterEntry = certificationRegisterEntryRepository.saveAndFlush(certificationRegisterEntry);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the certificationRegisterEntry using partial update
        CertificationRegisterEntry partialUpdatedCertificationRegisterEntry = new CertificationRegisterEntry();
        partialUpdatedCertificationRegisterEntry.setId(certificationRegisterEntry.getId());

        partialUpdatedCertificationRegisterEntry
            .numeroAttestation(UPDATED_NUMERO_ATTESTATION)
            .institutionId(UPDATED_INSTITUTION_ID)
            .typeProjet(UPDATED_TYPE_PROJET)
            .services(UPDATED_SERVICES)
            .dateEmission(UPDATED_DATE_EMISSION)
            .echeanceRenouvellement(UPDATED_ECHEANCE_RENOUVELLEMENT)
            .etat(UPDATED_ETAT)
            .precedenteId(UPDATED_PRECEDENTE_ID)
            .dernierSeuilNotifie(UPDATED_DERNIER_SEUIL_NOTIFIE);

        restCertificationRegisterEntryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCertificationRegisterEntry.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCertificationRegisterEntry))
            )
            .andExpect(status().isOk());

        // Validate the CertificationRegisterEntry in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCertificationRegisterEntryUpdatableFieldsEquals(
            partialUpdatedCertificationRegisterEntry,
            getPersistedCertificationRegisterEntry(partialUpdatedCertificationRegisterEntry)
        );
    }

    @Test
    @Transactional
    void patchNonExistingCertificationRegisterEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationRegisterEntry.setId(UUID.randomUUID());

        // Create the CertificationRegisterEntry
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCertificationRegisterEntryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, certificationRegisterEntryDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationRegisterEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCertificationRegisterEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationRegisterEntry.setId(UUID.randomUUID());

        // Create the CertificationRegisterEntry
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCertificationRegisterEntryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CertificationRegisterEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCertificationRegisterEntry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        certificationRegisterEntry.setId(UUID.randomUUID());

        // Create the CertificationRegisterEntry
        CertificationRegisterEntryDTO certificationRegisterEntryDTO = certificationRegisterEntryMapper.toDto(certificationRegisterEntry);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCertificationRegisterEntryMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(certificationRegisterEntryDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the CertificationRegisterEntry in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteCertificationRegisterEntry() throws Exception {
        // Initialize the database
        insertedCertificationRegisterEntry = certificationRegisterEntryRepository.saveAndFlush(certificationRegisterEntry);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the certificationRegisterEntry
        restCertificationRegisterEntryMockMvc
            .perform(
                delete(ENTITY_API_URL_ID, certificationRegisterEntry.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return certificationRegisterEntryRepository.count();
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

    protected CertificationRegisterEntry getPersistedCertificationRegisterEntry(CertificationRegisterEntry certificationRegisterEntry) {
        return certificationRegisterEntryRepository.findById(certificationRegisterEntry.getId()).orElseThrow();
    }

    protected void assertPersistedCertificationRegisterEntryToMatchAllProperties(
        CertificationRegisterEntry expectedCertificationRegisterEntry
    ) {
        assertCertificationRegisterEntryAllPropertiesEquals(
            expectedCertificationRegisterEntry,
            getPersistedCertificationRegisterEntry(expectedCertificationRegisterEntry)
        );
    }

    protected void assertPersistedCertificationRegisterEntryToMatchUpdatableProperties(
        CertificationRegisterEntry expectedCertificationRegisterEntry
    ) {
        assertCertificationRegisterEntryAllUpdatablePropertiesEquals(
            expectedCertificationRegisterEntry,
            getPersistedCertificationRegisterEntry(expectedCertificationRegisterEntry)
        );
    }
}
