package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.AttestationAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.Attestation;
import cm.gimac.des.certification.domain.GoLiveDossier;
import cm.gimac.des.certification.domain.enumeration.EtatAttestation;
import cm.gimac.des.certification.repository.AttestationRepository;
import cm.gimac.des.certification.service.dto.AttestationDTO;
import cm.gimac.des.certification.service.mapper.AttestationMapper;
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
 * Integration tests for the {@link AttestationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.certification.security.AuthoritiesConstants.ADMIN)
class AttestationResourceIT {

    private static final String DEFAULT_NUMERO = "AAAAAAAAAA";
    private static final String UPDATED_NUMERO = "BBBBBBBBBB";

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final String DEFAULT_TYPE_PROJET = "AAAAAAAAAA";
    private static final String UPDATED_TYPE_PROJET = "BBBBBBBBBB";

    private static final String DEFAULT_SERVICES_CERTIFIES = "[\"A\"]"; // colonne jsonb : JSON valide requis
    private static final String UPDATED_SERVICES_CERTIFIES = "[\"B\"]";

    private static final Instant DEFAULT_DATE_EMISSION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_EMISSION = Instant.ofEpochMilli(1702411306964L);

    private static final String DEFAULT_SOCLE_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_SOCLE_EMPREINTE = "BBBBBBBBBB";

    private static final String DEFAULT_DOCUMENT_REF = "AAAAAAAAAA";
    private static final String UPDATED_DOCUMENT_REF = "BBBBBBBBBB";

    private static final String DEFAULT_EMPREINTE_DOCUMENT = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE_DOCUMENT = "BBBBBBBBBB";

    private static final EtatAttestation DEFAULT_ETAT = EtatAttestation.EN_VIGUEUR;
    private static final EtatAttestation UPDATED_ETAT = EtatAttestation.SUSPENDUE;

    private static final String DEFAULT_REFERENCE_AUDIT = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE_AUDIT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/attestations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AttestationRepository attestationRepository;

    @Autowired
    private AttestationMapper attestationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAttestationMockMvc;

    private Attestation attestation;

    private Attestation insertedAttestation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Attestation createEntity(EntityManager em) {
        Attestation attestation = new Attestation()
            .numero(DEFAULT_NUMERO)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .typeProjet(DEFAULT_TYPE_PROJET)
            .servicesCertifies(DEFAULT_SERVICES_CERTIFIES)
            .dateEmission(DEFAULT_DATE_EMISSION)
            .socleEmpreinte(DEFAULT_SOCLE_EMPREINTE)
            .documentRef(DEFAULT_DOCUMENT_REF)
            .empreinteDocument(DEFAULT_EMPREINTE_DOCUMENT)
            .etat(DEFAULT_ETAT)
            .referenceAudit(DEFAULT_REFERENCE_AUDIT);
        // Add required entity
        GoLiveDossier goLiveDossier;
        if (TestUtil.findAll(em, GoLiveDossier.class).isEmpty()) {
            goLiveDossier = GoLiveDossierResourceIT.createEntity(em);
            em.persist(goLiveDossier);
            em.flush();
        } else {
            goLiveDossier = TestUtil.findAll(em, GoLiveDossier.class).getFirst();
        }
        attestation.setDossier(goLiveDossier);
        return attestation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Attestation createUpdatedEntity(EntityManager em) {
        Attestation updatedAttestation = new Attestation()
            .numero(UPDATED_NUMERO)
            .institutionId(UPDATED_INSTITUTION_ID)
            .typeProjet(UPDATED_TYPE_PROJET)
            .servicesCertifies(UPDATED_SERVICES_CERTIFIES)
            .dateEmission(UPDATED_DATE_EMISSION)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinteDocument(UPDATED_EMPREINTE_DOCUMENT)
            .etat(UPDATED_ETAT)
            .referenceAudit(UPDATED_REFERENCE_AUDIT);
        // Add required entity
        GoLiveDossier goLiveDossier;
        if (TestUtil.findAll(em, GoLiveDossier.class).isEmpty()) {
            goLiveDossier = GoLiveDossierResourceIT.createUpdatedEntity(em);
            em.persist(goLiveDossier);
            em.flush();
        } else {
            goLiveDossier = TestUtil.findAll(em, GoLiveDossier.class).getFirst();
        }
        updatedAttestation.setDossier(goLiveDossier);
        return updatedAttestation;
    }

    @BeforeEach
    void initTest() {
        attestation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAttestation != null) {
            attestationRepository.delete(insertedAttestation);
            insertedAttestation = null;
        }
    }

    @Test
    @Transactional
    void createAttestation() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Attestation
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);
        var returnedAttestationDTO = om.readValue(
            restAttestationMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(attestationDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AttestationDTO.class
        );

        // Validate the Attestation in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAttestation = attestationMapper.toEntity(returnedAttestationDTO);
        assertAttestationUpdatableFieldsEquals(returnedAttestation, getPersistedAttestation(returnedAttestation));

        insertedAttestation = returnedAttestation;
    }

    @Test
    @Transactional
    void createAttestationWithExistingId() throws Exception {
        // Create the Attestation with an existing ID
        insertedAttestation = attestationRepository.saveAndFlush(attestation);
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAttestationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(attestationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Attestation in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNumeroIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        attestation.setNumero(null);

        // Create the Attestation, which fails.
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);

        restAttestationMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(attestationDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAttestations() throws Exception {
        // Initialize the database
        insertedAttestation = attestationRepository.saveAndFlush(attestation);

        // Get all the attestationList
        restAttestationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(attestation.getId().toString())))
            .andExpect(jsonPath("$.[*].numero").value(hasItem(DEFAULT_NUMERO)))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].typeProjet").value(hasItem(DEFAULT_TYPE_PROJET)))
            .andExpect(jsonPath("$.[*].servicesCertifies").value(hasItem(DEFAULT_SERVICES_CERTIFIES)))
            .andExpect(jsonPath("$.[*].dateEmission").value(hasItem(DEFAULT_DATE_EMISSION.toString())))
            .andExpect(jsonPath("$.[*].socleEmpreinte").value(hasItem(DEFAULT_SOCLE_EMPREINTE)))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF)))
            .andExpect(jsonPath("$.[*].empreinteDocument").value(hasItem(DEFAULT_EMPREINTE_DOCUMENT)))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].referenceAudit").value(hasItem(DEFAULT_REFERENCE_AUDIT)));
    }

    @Test
    @Transactional
    void getAttestation() throws Exception {
        // Initialize the database
        insertedAttestation = attestationRepository.saveAndFlush(attestation);

        // Get the attestation
        restAttestationMockMvc
            .perform(get(ENTITY_API_URL_ID, attestation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(attestation.getId().toString()))
            .andExpect(jsonPath("$.numero").value(DEFAULT_NUMERO))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.typeProjet").value(DEFAULT_TYPE_PROJET))
            .andExpect(jsonPath("$.servicesCertifies").value(DEFAULT_SERVICES_CERTIFIES))
            .andExpect(jsonPath("$.dateEmission").value(DEFAULT_DATE_EMISSION.toString()))
            .andExpect(jsonPath("$.socleEmpreinte").value(DEFAULT_SOCLE_EMPREINTE))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF))
            .andExpect(jsonPath("$.empreinteDocument").value(DEFAULT_EMPREINTE_DOCUMENT))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.referenceAudit").value(DEFAULT_REFERENCE_AUDIT));
    }

    @Test
    @Transactional
    void getNonExistingAttestation() throws Exception {
        // Get the attestation
        restAttestationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAttestation() throws Exception {
        // Initialize the database
        insertedAttestation = attestationRepository.saveAndFlush(attestation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the attestation
        Attestation updatedAttestation = attestationRepository.findById(attestation.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAttestation are not directly saved in db
        em.detach(updatedAttestation);
        updatedAttestation
            .numero(UPDATED_NUMERO)
            .institutionId(UPDATED_INSTITUTION_ID)
            .typeProjet(UPDATED_TYPE_PROJET)
            .servicesCertifies(UPDATED_SERVICES_CERTIFIES)
            .dateEmission(UPDATED_DATE_EMISSION)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinteDocument(UPDATED_EMPREINTE_DOCUMENT)
            .etat(UPDATED_ETAT)
            .referenceAudit(UPDATED_REFERENCE_AUDIT);
        AttestationDTO attestationDTO = attestationMapper.toDto(updatedAttestation);

        restAttestationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, attestationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationDTO))
            )
            .andExpect(status().isOk());

        // Validate the Attestation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAttestationToMatchAllProperties(updatedAttestation);
    }

    @Test
    @Transactional
    void putNonExistingAttestation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestation.setId(UUID.randomUUID());

        // Create the Attestation
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAttestationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, attestationDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Attestation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAttestation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestation.setId(UUID.randomUUID());

        // Create the Attestation
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAttestationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Attestation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAttestation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestation.setId(UUID.randomUUID());

        // Create the Attestation
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAttestationMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(attestationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Attestation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAttestationWithPatch() throws Exception {
        // Initialize the database
        insertedAttestation = attestationRepository.saveAndFlush(attestation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the attestation using partial update
        Attestation partialUpdatedAttestation = new Attestation();
        partialUpdatedAttestation.setId(attestation.getId());

        partialUpdatedAttestation
            .institutionId(UPDATED_INSTITUTION_ID)
            .typeProjet(UPDATED_TYPE_PROJET)
            .servicesCertifies(UPDATED_SERVICES_CERTIFIES)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .empreinteDocument(UPDATED_EMPREINTE_DOCUMENT);

        restAttestationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAttestation.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAttestation))
            )
            .andExpect(status().isOk());

        // Validate the Attestation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAttestationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAttestation, attestation),
            getPersistedAttestation(attestation)
        );
    }

    @Test
    @Transactional
    void fullUpdateAttestationWithPatch() throws Exception {
        // Initialize the database
        insertedAttestation = attestationRepository.saveAndFlush(attestation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the attestation using partial update
        Attestation partialUpdatedAttestation = new Attestation();
        partialUpdatedAttestation.setId(attestation.getId());

        partialUpdatedAttestation
            .numero(UPDATED_NUMERO)
            .institutionId(UPDATED_INSTITUTION_ID)
            .typeProjet(UPDATED_TYPE_PROJET)
            .servicesCertifies(UPDATED_SERVICES_CERTIFIES)
            .dateEmission(UPDATED_DATE_EMISSION)
            .socleEmpreinte(UPDATED_SOCLE_EMPREINTE)
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinteDocument(UPDATED_EMPREINTE_DOCUMENT)
            .etat(UPDATED_ETAT)
            .referenceAudit(UPDATED_REFERENCE_AUDIT);

        restAttestationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAttestation.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAttestation))
            )
            .andExpect(status().isOk());

        // Validate the Attestation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAttestationUpdatableFieldsEquals(partialUpdatedAttestation, getPersistedAttestation(partialUpdatedAttestation));
    }

    @Test
    @Transactional
    void patchNonExistingAttestation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestation.setId(UUID.randomUUID());

        // Create the Attestation
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAttestationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, attestationDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(attestationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Attestation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAttestation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestation.setId(UUID.randomUUID());

        // Create the Attestation
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAttestationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(attestationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Attestation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAttestation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestation.setId(UUID.randomUUID());

        // Create the Attestation
        AttestationDTO attestationDTO = attestationMapper.toDto(attestation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAttestationMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(attestationDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Attestation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAttestation() throws Exception {
        // Initialize the database
        insertedAttestation = attestationRepository.saveAndFlush(attestation);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the attestation
        restAttestationMockMvc
            .perform(delete(ENTITY_API_URL_ID, attestation.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return attestationRepository.count();
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

    protected Attestation getPersistedAttestation(Attestation attestation) {
        return attestationRepository.findById(attestation.getId()).orElseThrow();
    }

    protected void assertPersistedAttestationToMatchAllProperties(Attestation expectedAttestation) {
        assertAttestationAllPropertiesEquals(expectedAttestation, getPersistedAttestation(expectedAttestation));
    }

    protected void assertPersistedAttestationToMatchUpdatableProperties(Attestation expectedAttestation) {
        assertAttestationAllUpdatablePropertiesEquals(expectedAttestation, getPersistedAttestation(expectedAttestation));
    }
}
