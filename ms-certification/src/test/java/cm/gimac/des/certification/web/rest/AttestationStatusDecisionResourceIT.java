package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.AttestationStatusDecisionAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.Attestation;
import cm.gimac.des.certification.domain.AttestationStatusDecision;
import cm.gimac.des.certification.domain.enumeration.TypeDemandeStatut;
import cm.gimac.des.certification.repository.AttestationStatusDecisionRepository;
import cm.gimac.des.certification.service.dto.AttestationStatusDecisionDTO;
import cm.gimac.des.certification.service.mapper.AttestationStatusDecisionMapper;
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
 * Integration tests for the {@link AttestationStatusDecisionResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.certification.security.AuthoritiesConstants.ADMIN)
class AttestationStatusDecisionResourceIT {

    private static final TypeDemandeStatut DEFAULT_TYPE_DEMANDE = TypeDemandeStatut.REVOCATION;
    private static final TypeDemandeStatut UPDATED_TYPE_DEMANDE = TypeDemandeStatut.SUSPENSION;

    private static final String DEFAULT_MOTIF = "AAAAAAAAAA";
    private static final String UPDATED_MOTIF = "BBBBBBBBBB";

    private static final String DEFAULT_PIECE_REF = "AAAAAAAAAA";
    private static final String UPDATED_PIECE_REF = "BBBBBBBBBB";

    private static final UUID DEFAULT_DEMANDEUR_ID = UUID.randomUUID();
    private static final UUID UPDATED_DEMANDEUR_ID = UUID.randomUUID();

    private static final UUID DEFAULT_APPROBATEUR_ID = UUID.randomUUID();
    private static final UUID UPDATED_APPROBATEUR_ID = UUID.randomUUID();

    private static final Instant DEFAULT_DATE_DEMANDE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_DEMANDE = Instant.ofEpochMilli(1702411306964L);

    private static final Instant DEFAULT_DATE_APPROBATION = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_APPROBATION = Instant.ofEpochMilli(1702411306964L);

    private static final String DEFAULT_ETAT = "AAAAAAAAAA";
    private static final String UPDATED_ETAT = "BBBBBBBBBB";

    private static final String DEFAULT_AVIS_DOCUMENT_REF = "AAAAAAAAAA";
    private static final String UPDATED_AVIS_DOCUMENT_REF = "BBBBBBBBBB";

    private static final String DEFAULT_CONDITION_LEVEE = "AAAAAAAAAA";
    private static final String UPDATED_CONDITION_LEVEE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/attestation-status-decisions";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AttestationStatusDecisionRepository attestationStatusDecisionRepository;

    @Autowired
    private AttestationStatusDecisionMapper attestationStatusDecisionMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAttestationStatusDecisionMockMvc;

    private AttestationStatusDecision attestationStatusDecision;

    private AttestationStatusDecision insertedAttestationStatusDecision;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AttestationStatusDecision createEntity(EntityManager em) {
        AttestationStatusDecision attestationStatusDecision = new AttestationStatusDecision()
            .typeDemande(DEFAULT_TYPE_DEMANDE)
            .motif(DEFAULT_MOTIF)
            .pieceRef(DEFAULT_PIECE_REF)
            .demandeurId(DEFAULT_DEMANDEUR_ID)
            .approbateurId(DEFAULT_APPROBATEUR_ID)
            .dateDemande(DEFAULT_DATE_DEMANDE)
            .dateApprobation(DEFAULT_DATE_APPROBATION)
            .etat(DEFAULT_ETAT)
            .avisDocumentRef(DEFAULT_AVIS_DOCUMENT_REF)
            .conditionLevee(DEFAULT_CONDITION_LEVEE);
        // Add required entity
        Attestation attestation;
        if (TestUtil.findAll(em, Attestation.class).isEmpty()) {
            attestation = AttestationResourceIT.createEntity(em);
            em.persist(attestation);
            em.flush();
        } else {
            attestation = TestUtil.findAll(em, Attestation.class).getFirst();
        }
        attestationStatusDecision.setAttestation(attestation);
        return attestationStatusDecision;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AttestationStatusDecision createUpdatedEntity(EntityManager em) {
        AttestationStatusDecision updatedAttestationStatusDecision = new AttestationStatusDecision()
            .typeDemande(UPDATED_TYPE_DEMANDE)
            .motif(UPDATED_MOTIF)
            .pieceRef(UPDATED_PIECE_REF)
            .demandeurId(UPDATED_DEMANDEUR_ID)
            .approbateurId(UPDATED_APPROBATEUR_ID)
            .dateDemande(UPDATED_DATE_DEMANDE)
            .dateApprobation(UPDATED_DATE_APPROBATION)
            .etat(UPDATED_ETAT)
            .avisDocumentRef(UPDATED_AVIS_DOCUMENT_REF)
            .conditionLevee(UPDATED_CONDITION_LEVEE);
        // Add required entity
        Attestation attestation;
        if (TestUtil.findAll(em, Attestation.class).isEmpty()) {
            attestation = AttestationResourceIT.createUpdatedEntity(em);
            em.persist(attestation);
            em.flush();
        } else {
            attestation = TestUtil.findAll(em, Attestation.class).getFirst();
        }
        updatedAttestationStatusDecision.setAttestation(attestation);
        return updatedAttestationStatusDecision;
    }

    @BeforeEach
    void initTest() {
        attestationStatusDecision = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAttestationStatusDecision != null) {
            attestationStatusDecisionRepository.delete(insertedAttestationStatusDecision);
            insertedAttestationStatusDecision = null;
        }
    }

    @Test
    @Transactional
    void createAttestationStatusDecision() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AttestationStatusDecision
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);
        var returnedAttestationStatusDecisionDTO = om.readValue(
            restAttestationStatusDecisionMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AttestationStatusDecisionDTO.class
        );

        // Validate the AttestationStatusDecision in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAttestationStatusDecision = attestationStatusDecisionMapper.toEntity(returnedAttestationStatusDecisionDTO);
        assertAttestationStatusDecisionUpdatableFieldsEquals(
            returnedAttestationStatusDecision,
            getPersistedAttestationStatusDecision(returnedAttestationStatusDecision)
        );

        insertedAttestationStatusDecision = returnedAttestationStatusDecision;
    }

    @Test
    @Transactional
    void createAttestationStatusDecisionWithExistingId() throws Exception {
        // Create the AttestationStatusDecision with an existing ID
        insertedAttestationStatusDecision = attestationStatusDecisionRepository.saveAndFlush(attestationStatusDecision);
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAttestationStatusDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AttestationStatusDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeDemandeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        attestationStatusDecision.setTypeDemande(null);

        // Create the AttestationStatusDecision, which fails.
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        restAttestationStatusDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkMotifIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        attestationStatusDecision.setMotif(null);

        // Create the AttestationStatusDecision, which fails.
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        restAttestationStatusDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDemandeurIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        attestationStatusDecision.setDemandeurId(null);

        // Create the AttestationStatusDecision, which fails.
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        restAttestationStatusDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateDemandeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        attestationStatusDecision.setDateDemande(null);

        // Create the AttestationStatusDecision, which fails.
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        restAttestationStatusDecisionMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAttestationStatusDecisions() throws Exception {
        // Initialize the database
        insertedAttestationStatusDecision = attestationStatusDecisionRepository.saveAndFlush(attestationStatusDecision);

        // Get all the attestationStatusDecisionList
        restAttestationStatusDecisionMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(attestationStatusDecision.getId().toString())))
            .andExpect(jsonPath("$.[*].typeDemande").value(hasItem(DEFAULT_TYPE_DEMANDE.toString())))
            .andExpect(jsonPath("$.[*].motif").value(hasItem(DEFAULT_MOTIF)))
            .andExpect(jsonPath("$.[*].pieceRef").value(hasItem(DEFAULT_PIECE_REF)))
            .andExpect(jsonPath("$.[*].demandeurId").value(hasItem(DEFAULT_DEMANDEUR_ID.toString())))
            .andExpect(jsonPath("$.[*].approbateurId").value(hasItem(DEFAULT_APPROBATEUR_ID.toString())))
            .andExpect(jsonPath("$.[*].dateDemande").value(hasItem(DEFAULT_DATE_DEMANDE.toString())))
            .andExpect(jsonPath("$.[*].dateApprobation").value(hasItem(DEFAULT_DATE_APPROBATION.toString())))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT)))
            .andExpect(jsonPath("$.[*].avisDocumentRef").value(hasItem(DEFAULT_AVIS_DOCUMENT_REF)))
            .andExpect(jsonPath("$.[*].conditionLevee").value(hasItem(DEFAULT_CONDITION_LEVEE)));
    }

    @Test
    @Transactional
    void getAttestationStatusDecision() throws Exception {
        // Initialize the database
        insertedAttestationStatusDecision = attestationStatusDecisionRepository.saveAndFlush(attestationStatusDecision);

        // Get the attestationStatusDecision
        restAttestationStatusDecisionMockMvc
            .perform(get(ENTITY_API_URL_ID, attestationStatusDecision.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(attestationStatusDecision.getId().toString()))
            .andExpect(jsonPath("$.typeDemande").value(DEFAULT_TYPE_DEMANDE.toString()))
            .andExpect(jsonPath("$.motif").value(DEFAULT_MOTIF))
            .andExpect(jsonPath("$.pieceRef").value(DEFAULT_PIECE_REF))
            .andExpect(jsonPath("$.demandeurId").value(DEFAULT_DEMANDEUR_ID.toString()))
            .andExpect(jsonPath("$.approbateurId").value(DEFAULT_APPROBATEUR_ID.toString()))
            .andExpect(jsonPath("$.dateDemande").value(DEFAULT_DATE_DEMANDE.toString()))
            .andExpect(jsonPath("$.dateApprobation").value(DEFAULT_DATE_APPROBATION.toString()))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT))
            .andExpect(jsonPath("$.avisDocumentRef").value(DEFAULT_AVIS_DOCUMENT_REF))
            .andExpect(jsonPath("$.conditionLevee").value(DEFAULT_CONDITION_LEVEE));
    }

    @Test
    @Transactional
    void getNonExistingAttestationStatusDecision() throws Exception {
        // Get the attestationStatusDecision
        restAttestationStatusDecisionMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAttestationStatusDecision() throws Exception {
        // Initialize the database
        insertedAttestationStatusDecision = attestationStatusDecisionRepository.saveAndFlush(attestationStatusDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the attestationStatusDecision
        AttestationStatusDecision updatedAttestationStatusDecision = attestationStatusDecisionRepository
            .findById(attestationStatusDecision.getId())
            .orElseThrow();
        // Disconnect from session so that the updates on updatedAttestationStatusDecision are not directly saved in db
        em.detach(updatedAttestationStatusDecision);
        updatedAttestationStatusDecision
            .typeDemande(UPDATED_TYPE_DEMANDE)
            .motif(UPDATED_MOTIF)
            .pieceRef(UPDATED_PIECE_REF)
            .demandeurId(UPDATED_DEMANDEUR_ID)
            .approbateurId(UPDATED_APPROBATEUR_ID)
            .dateDemande(UPDATED_DATE_DEMANDE)
            .dateApprobation(UPDATED_DATE_APPROBATION)
            .etat(UPDATED_ETAT)
            .avisDocumentRef(UPDATED_AVIS_DOCUMENT_REF)
            .conditionLevee(UPDATED_CONDITION_LEVEE);
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(updatedAttestationStatusDecision);

        restAttestationStatusDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, attestationStatusDecisionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isOk());

        // Validate the AttestationStatusDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAttestationStatusDecisionToMatchAllProperties(updatedAttestationStatusDecision);
    }

    @Test
    @Transactional
    void putNonExistingAttestationStatusDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestationStatusDecision.setId(UUID.randomUUID());

        // Create the AttestationStatusDecision
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAttestationStatusDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, attestationStatusDecisionDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AttestationStatusDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAttestationStatusDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestationStatusDecision.setId(UUID.randomUUID());

        // Create the AttestationStatusDecision
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAttestationStatusDecisionMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AttestationStatusDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAttestationStatusDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestationStatusDecision.setId(UUID.randomUUID());

        // Create the AttestationStatusDecision
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAttestationStatusDecisionMockMvc
            .perform(
                put(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AttestationStatusDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAttestationStatusDecisionWithPatch() throws Exception {
        // Initialize the database
        insertedAttestationStatusDecision = attestationStatusDecisionRepository.saveAndFlush(attestationStatusDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the attestationStatusDecision using partial update
        AttestationStatusDecision partialUpdatedAttestationStatusDecision = new AttestationStatusDecision();
        partialUpdatedAttestationStatusDecision.setId(attestationStatusDecision.getId());

        partialUpdatedAttestationStatusDecision.dateDemande(UPDATED_DATE_DEMANDE).conditionLevee(UPDATED_CONDITION_LEVEE);

        restAttestationStatusDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAttestationStatusDecision.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAttestationStatusDecision))
            )
            .andExpect(status().isOk());

        // Validate the AttestationStatusDecision in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAttestationStatusDecisionUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAttestationStatusDecision, attestationStatusDecision),
            getPersistedAttestationStatusDecision(attestationStatusDecision)
        );
    }

    @Test
    @Transactional
    void fullUpdateAttestationStatusDecisionWithPatch() throws Exception {
        // Initialize the database
        insertedAttestationStatusDecision = attestationStatusDecisionRepository.saveAndFlush(attestationStatusDecision);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the attestationStatusDecision using partial update
        AttestationStatusDecision partialUpdatedAttestationStatusDecision = new AttestationStatusDecision();
        partialUpdatedAttestationStatusDecision.setId(attestationStatusDecision.getId());

        partialUpdatedAttestationStatusDecision
            .typeDemande(UPDATED_TYPE_DEMANDE)
            .motif(UPDATED_MOTIF)
            .pieceRef(UPDATED_PIECE_REF)
            .demandeurId(UPDATED_DEMANDEUR_ID)
            .approbateurId(UPDATED_APPROBATEUR_ID)
            .dateDemande(UPDATED_DATE_DEMANDE)
            .dateApprobation(UPDATED_DATE_APPROBATION)
            .etat(UPDATED_ETAT)
            .avisDocumentRef(UPDATED_AVIS_DOCUMENT_REF)
            .conditionLevee(UPDATED_CONDITION_LEVEE);

        restAttestationStatusDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAttestationStatusDecision.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAttestationStatusDecision))
            )
            .andExpect(status().isOk());

        // Validate the AttestationStatusDecision in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAttestationStatusDecisionUpdatableFieldsEquals(
            partialUpdatedAttestationStatusDecision,
            getPersistedAttestationStatusDecision(partialUpdatedAttestationStatusDecision)
        );
    }

    @Test
    @Transactional
    void patchNonExistingAttestationStatusDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestationStatusDecision.setId(UUID.randomUUID());

        // Create the AttestationStatusDecision
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAttestationStatusDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, attestationStatusDecisionDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AttestationStatusDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAttestationStatusDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestationStatusDecision.setId(UUID.randomUUID());

        // Create the AttestationStatusDecision
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAttestationStatusDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AttestationStatusDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAttestationStatusDecision() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        attestationStatusDecision.setId(UUID.randomUUID());

        // Create the AttestationStatusDecision
        AttestationStatusDecisionDTO attestationStatusDecisionDTO = attestationStatusDecisionMapper.toDto(attestationStatusDecision);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAttestationStatusDecisionMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(attestationStatusDecisionDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AttestationStatusDecision in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAttestationStatusDecision() throws Exception {
        // Initialize the database
        insertedAttestationStatusDecision = attestationStatusDecisionRepository.saveAndFlush(attestationStatusDecision);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the attestationStatusDecision
        restAttestationStatusDecisionMockMvc
            .perform(
                delete(ENTITY_API_URL_ID, attestationStatusDecision.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return attestationStatusDecisionRepository.count();
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

    protected AttestationStatusDecision getPersistedAttestationStatusDecision(AttestationStatusDecision attestationStatusDecision) {
        return attestationStatusDecisionRepository.findById(attestationStatusDecision.getId()).orElseThrow();
    }

    protected void assertPersistedAttestationStatusDecisionToMatchAllProperties(
        AttestationStatusDecision expectedAttestationStatusDecision
    ) {
        assertAttestationStatusDecisionAllPropertiesEquals(
            expectedAttestationStatusDecision,
            getPersistedAttestationStatusDecision(expectedAttestationStatusDecision)
        );
    }

    protected void assertPersistedAttestationStatusDecisionToMatchUpdatableProperties(
        AttestationStatusDecision expectedAttestationStatusDecision
    ) {
        assertAttestationStatusDecisionAllUpdatablePropertiesEquals(
            expectedAttestationStatusDecision,
            getPersistedAttestationStatusDecision(expectedAttestationStatusDecision)
        );
    }
}
