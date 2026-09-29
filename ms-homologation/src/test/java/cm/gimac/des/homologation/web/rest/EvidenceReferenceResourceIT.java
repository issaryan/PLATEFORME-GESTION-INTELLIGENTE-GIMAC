package cm.gimac.des.homologation.web.rest;

import static cm.gimac.des.homologation.domain.EvidenceReferenceAsserts.*;
import static cm.gimac.des.homologation.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.homologation.IntegrationTest;
import cm.gimac.des.homologation.domain.EvidenceReference;
import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.repository.EvidenceReferenceRepository;
import cm.gimac.des.homologation.service.dto.EvidenceReferenceDTO;
import cm.gimac.des.homologation.service.mapper.EvidenceReferenceMapper;
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
 * Integration tests for the {@link EvidenceReferenceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class EvidenceReferenceResourceIT {

    private static final String DEFAULT_DOCUMENT_REF = "AAAAAAAAAA";
    private static final String UPDATED_DOCUMENT_REF = "BBBBBBBBBB";

    private static final String DEFAULT_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE = "BBBBBBBBBB";

    private static final Integer DEFAULT_VERSION = 1;
    private static final Integer UPDATED_VERSION = 2;

    private static final String DEFAULT_TYPE_PIECE = "AAAAAAAAAA";
    private static final String UPDATED_TYPE_PIECE = "BBBBBBBBBB";

    private static final UUID DEFAULT_DEPOSANT_ID = UUID.randomUUID();
    private static final UUID UPDATED_DEPOSANT_ID = UUID.randomUUID();

    private static final Instant DEFAULT_DATE_DEPOT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_DEPOT = Instant.ofEpochMilli(1701958935371L);

    private static final Boolean DEFAULT_SCELLE = false;
    private static final Boolean UPDATED_SCELLE = true;

    private static final Boolean DEFAULT_VALIDE = false;
    private static final Boolean UPDATED_VALIDE = true;

    private static final String ENTITY_API_URL = "/api/evidence-references";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private EvidenceReferenceRepository evidenceReferenceRepository;

    @Autowired
    private EvidenceReferenceMapper evidenceReferenceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restEvidenceReferenceMockMvc;

    private EvidenceReference evidenceReference;

    private EvidenceReference insertedEvidenceReference;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EvidenceReference createEntity(EntityManager em) {
        EvidenceReference evidenceReference = new EvidenceReference()
            .documentRef(DEFAULT_DOCUMENT_REF)
            .empreinte(DEFAULT_EMPREINTE)
            .version(DEFAULT_VERSION)
            .typePiece(DEFAULT_TYPE_PIECE)
            .deposantId(DEFAULT_DEPOSANT_ID)
            .dateDepot(DEFAULT_DATE_DEPOT)
            .scelle(DEFAULT_SCELLE)
            .valide(DEFAULT_VALIDE);
        // Add required entity
        OperationInstance operationInstance;
        if (TestUtil.findAll(em, OperationInstance.class).isEmpty()) {
            operationInstance = OperationInstanceResourceIT.createEntity(em);
            em.persist(operationInstance);
            em.flush();
        } else {
            operationInstance = TestUtil.findAll(em, OperationInstance.class).getFirst();
        }
        evidenceReference.setOperation(operationInstance);
        return evidenceReference;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static EvidenceReference createUpdatedEntity(EntityManager em) {
        EvidenceReference updatedEvidenceReference = new EvidenceReference()
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinte(UPDATED_EMPREINTE)
            .version(UPDATED_VERSION)
            .typePiece(UPDATED_TYPE_PIECE)
            .deposantId(UPDATED_DEPOSANT_ID)
            .dateDepot(UPDATED_DATE_DEPOT)
            .scelle(UPDATED_SCELLE)
            .valide(UPDATED_VALIDE);
        // Add required entity
        OperationInstance operationInstance;
        if (TestUtil.findAll(em, OperationInstance.class).isEmpty()) {
            operationInstance = OperationInstanceResourceIT.createUpdatedEntity(em);
            em.persist(operationInstance);
            em.flush();
        } else {
            operationInstance = TestUtil.findAll(em, OperationInstance.class).getFirst();
        }
        updatedEvidenceReference.setOperation(operationInstance);
        return updatedEvidenceReference;
    }

    @BeforeEach
    void initTest() {
        evidenceReference = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedEvidenceReference != null) {
            evidenceReferenceRepository.delete(insertedEvidenceReference);
            insertedEvidenceReference = null;
        }
    }

    @Test
    @Transactional
    void createEvidenceReference() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the EvidenceReference
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);
        var returnedEvidenceReferenceDTO = om.readValue(
            restEvidenceReferenceMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(evidenceReferenceDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            EvidenceReferenceDTO.class
        );

        // Validate the EvidenceReference in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedEvidenceReference = evidenceReferenceMapper.toEntity(returnedEvidenceReferenceDTO);
        assertEvidenceReferenceUpdatableFieldsEquals(returnedEvidenceReference, getPersistedEvidenceReference(returnedEvidenceReference));

        insertedEvidenceReference = returnedEvidenceReference;
    }

    @Test
    @Transactional
    void createEvidenceReferenceWithExistingId() throws Exception {
        // Create the EvidenceReference with an existing ID
        insertedEvidenceReference = evidenceReferenceRepository.saveAndFlush(evidenceReference);
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restEvidenceReferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceReference in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDocumentRefIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidenceReference.setDocumentRef(null);

        // Create the EvidenceReference, which fails.
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        restEvidenceReferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEmpreinteIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidenceReference.setEmpreinte(null);

        // Create the EvidenceReference, which fails.
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        restEvidenceReferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVersionIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidenceReference.setVersion(null);

        // Create the EvidenceReference, which fails.
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        restEvidenceReferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTypePieceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidenceReference.setTypePiece(null);

        // Create the EvidenceReference, which fails.
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        restEvidenceReferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDeposantIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidenceReference.setDeposantId(null);

        // Create the EvidenceReference, which fails.
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        restEvidenceReferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateDepotIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidenceReference.setDateDepot(null);

        // Create the EvidenceReference, which fails.
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        restEvidenceReferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkScelleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        evidenceReference.setScelle(null);

        // Create the EvidenceReference, which fails.
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        restEvidenceReferenceMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllEvidenceReferences() throws Exception {
        // Initialize the database
        insertedEvidenceReference = evidenceReferenceRepository.saveAndFlush(evidenceReference);

        // Get all the evidenceReferenceList
        restEvidenceReferenceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(evidenceReference.getId().toString())))
            .andExpect(jsonPath("$.[*].documentRef").value(hasItem(DEFAULT_DOCUMENT_REF)))
            .andExpect(jsonPath("$.[*].empreinte").value(hasItem(DEFAULT_EMPREINTE)))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)))
            .andExpect(jsonPath("$.[*].typePiece").value(hasItem(DEFAULT_TYPE_PIECE)))
            .andExpect(jsonPath("$.[*].deposantId").value(hasItem(DEFAULT_DEPOSANT_ID.toString())))
            .andExpect(jsonPath("$.[*].dateDepot").value(hasItem(DEFAULT_DATE_DEPOT.toString())))
            .andExpect(jsonPath("$.[*].scelle").value(hasItem(DEFAULT_SCELLE)))
            .andExpect(jsonPath("$.[*].valide").value(hasItem(DEFAULT_VALIDE)));
    }

    @Test
    @Transactional
    void getEvidenceReference() throws Exception {
        // Initialize the database
        insertedEvidenceReference = evidenceReferenceRepository.saveAndFlush(evidenceReference);

        // Get the evidenceReference
        restEvidenceReferenceMockMvc
            .perform(get(ENTITY_API_URL_ID, evidenceReference.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(evidenceReference.getId().toString()))
            .andExpect(jsonPath("$.documentRef").value(DEFAULT_DOCUMENT_REF))
            .andExpect(jsonPath("$.empreinte").value(DEFAULT_EMPREINTE))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION))
            .andExpect(jsonPath("$.typePiece").value(DEFAULT_TYPE_PIECE))
            .andExpect(jsonPath("$.deposantId").value(DEFAULT_DEPOSANT_ID.toString()))
            .andExpect(jsonPath("$.dateDepot").value(DEFAULT_DATE_DEPOT.toString()))
            .andExpect(jsonPath("$.scelle").value(DEFAULT_SCELLE))
            .andExpect(jsonPath("$.valide").value(DEFAULT_VALIDE));
    }

    @Test
    @Transactional
    void getNonExistingEvidenceReference() throws Exception {
        // Get the evidenceReference
        restEvidenceReferenceMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingEvidenceReference() throws Exception {
        // Initialize the database
        insertedEvidenceReference = evidenceReferenceRepository.saveAndFlush(evidenceReference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidenceReference
        EvidenceReference updatedEvidenceReference = evidenceReferenceRepository.findById(evidenceReference.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedEvidenceReference are not directly saved in db
        em.detach(updatedEvidenceReference);
        updatedEvidenceReference
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinte(UPDATED_EMPREINTE)
            .version(UPDATED_VERSION)
            .typePiece(UPDATED_TYPE_PIECE)
            .deposantId(UPDATED_DEPOSANT_ID)
            .dateDepot(UPDATED_DATE_DEPOT)
            .scelle(UPDATED_SCELLE)
            .valide(UPDATED_VALIDE);
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(updatedEvidenceReference);

        restEvidenceReferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, evidenceReferenceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isOk());

        // Validate the EvidenceReference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedEvidenceReferenceToMatchAllProperties(updatedEvidenceReference);
    }

    @Test
    @Transactional
    void putNonExistingEvidenceReference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceReference.setId(UUID.randomUUID());

        // Create the EvidenceReference
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEvidenceReferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, evidenceReferenceDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceReference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchEvidenceReference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceReference.setId(UUID.randomUUID());

        // Create the EvidenceReference
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceReferenceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceReference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamEvidenceReference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceReference.setId(UUID.randomUUID());

        // Create the EvidenceReference
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceReferenceMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the EvidenceReference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateEvidenceReferenceWithPatch() throws Exception {
        // Initialize the database
        insertedEvidenceReference = evidenceReferenceRepository.saveAndFlush(evidenceReference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidenceReference using partial update
        EvidenceReference partialUpdatedEvidenceReference = new EvidenceReference();
        partialUpdatedEvidenceReference.setId(evidenceReference.getId());

        partialUpdatedEvidenceReference
            .empreinte(UPDATED_EMPREINTE)
            .deposantId(UPDATED_DEPOSANT_ID)
            .dateDepot(UPDATED_DATE_DEPOT)
            .scelle(UPDATED_SCELLE);

        restEvidenceReferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvidenceReference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvidenceReference))
            )
            .andExpect(status().isOk());

        // Validate the EvidenceReference in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEvidenceReferenceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedEvidenceReference, evidenceReference),
            getPersistedEvidenceReference(evidenceReference)
        );
    }

    @Test
    @Transactional
    void fullUpdateEvidenceReferenceWithPatch() throws Exception {
        // Initialize the database
        insertedEvidenceReference = evidenceReferenceRepository.saveAndFlush(evidenceReference);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the evidenceReference using partial update
        EvidenceReference partialUpdatedEvidenceReference = new EvidenceReference();
        partialUpdatedEvidenceReference.setId(evidenceReference.getId());

        partialUpdatedEvidenceReference
            .documentRef(UPDATED_DOCUMENT_REF)
            .empreinte(UPDATED_EMPREINTE)
            .version(UPDATED_VERSION)
            .typePiece(UPDATED_TYPE_PIECE)
            .deposantId(UPDATED_DEPOSANT_ID)
            .dateDepot(UPDATED_DATE_DEPOT)
            .scelle(UPDATED_SCELLE)
            .valide(UPDATED_VALIDE);

        restEvidenceReferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedEvidenceReference.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedEvidenceReference))
            )
            .andExpect(status().isOk());

        // Validate the EvidenceReference in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertEvidenceReferenceUpdatableFieldsEquals(
            partialUpdatedEvidenceReference,
            getPersistedEvidenceReference(partialUpdatedEvidenceReference)
        );
    }

    @Test
    @Transactional
    void patchNonExistingEvidenceReference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceReference.setId(UUID.randomUUID());

        // Create the EvidenceReference
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEvidenceReferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, evidenceReferenceDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceReference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchEvidenceReference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceReference.setId(UUID.randomUUID());

        // Create the EvidenceReference
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceReferenceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the EvidenceReference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamEvidenceReference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        evidenceReference.setId(UUID.randomUUID());

        // Create the EvidenceReference
        EvidenceReferenceDTO evidenceReferenceDTO = evidenceReferenceMapper.toDto(evidenceReference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restEvidenceReferenceMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(evidenceReferenceDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the EvidenceReference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteEvidenceReference() throws Exception {
        // Initialize the database
        insertedEvidenceReference = evidenceReferenceRepository.saveAndFlush(evidenceReference);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the evidenceReference
        restEvidenceReferenceMockMvc
            .perform(delete(ENTITY_API_URL_ID, evidenceReference.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return evidenceReferenceRepository.count();
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

    protected EvidenceReference getPersistedEvidenceReference(EvidenceReference evidenceReference) {
        return evidenceReferenceRepository.findById(evidenceReference.getId()).orElseThrow();
    }

    protected void assertPersistedEvidenceReferenceToMatchAllProperties(EvidenceReference expectedEvidenceReference) {
        assertEvidenceReferenceAllPropertiesEquals(expectedEvidenceReference, getPersistedEvidenceReference(expectedEvidenceReference));
    }

    protected void assertPersistedEvidenceReferenceToMatchUpdatableProperties(EvidenceReference expectedEvidenceReference) {
        assertEvidenceReferenceAllUpdatablePropertiesEquals(
            expectedEvidenceReference,
            getPersistedEvidenceReference(expectedEvidenceReference)
        );
    }
}
