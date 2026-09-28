package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.CommissioningFileAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CertificationAttestation;
import cm.gimac.des.certification.domain.ChangeRecord;
import cm.gimac.des.certification.domain.CommissioningFile;
import cm.gimac.des.certification.domain.EvidenceItem;
import cm.gimac.des.certification.domain.enumeration.FileState;
import cm.gimac.des.certification.repository.CommissioningFileRepository;
import cm.gimac.des.certification.service.CommissioningFileService;
import cm.gimac.des.certification.service.mapper.CommissioningFileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link CommissioningFileResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CommissioningFileResourceIT {

    private static final String DEFAULT_FILE_CODE = "DMS-1493-8431";
    private static final String UPDATED_FILE_CODE = "DMS-5533-3534167";

    private static final UUID DEFAULT_LIAISON_ID = UUID.randomUUID();
    private static final UUID UPDATED_LIAISON_ID = UUID.randomUUID();

    private static final UUID DEFAULT_INSTITUTION_ID = UUID.randomUUID();
    private static final UUID UPDATED_INSTITUTION_ID = UUID.randomUUID();

    private static final UUID DEFAULT_PROJECT_LEAD_ID = UUID.randomUUID();
    private static final UUID UPDATED_PROJECT_LEAD_ID = UUID.randomUUID();

    private static final FileState DEFAULT_STATE = FileState.OUVERT;
    private static final FileState UPDATED_STATE = FileState.QUITUS_EN_COURS;

    private static final String DEFAULT_BASIS_DIGEST = "afe45f0dce5aa256c50d7f633d4f5962f4af2bf73929e5ad511d5affb33e0742";
    private static final String UPDATED_BASIS_DIGEST = "da333cb0cb5555026d046b262d2079df2a07e677ddb27c9118eaae0e60f025ae";

    private static final byte[] DEFAULT_BASIS_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_BASIS_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_BASIS_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_BASIS_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/png";

    private static final Instant DEFAULT_BASIS_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_BASIS_SEALED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_OPENED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_OPENED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_CORRECTION_REASON = "AAAAAAAAAA";
    private static final String UPDATED_CORRECTION_REASON = "BBBBBBBBBB";

    private static final String DEFAULT_MANIFEST_DIGEST = "2409012ad263a17e7d1233e9518970efdae050795b39c9e72b9900d77ad01385";
    private static final String UPDATED_MANIFEST_DIGEST = "4201e904ae5fc2cbdc13604098d62f3f372822c6ce3f2e8e9272c6129e193c67";

    private static final byte[] DEFAULT_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/png";

    private static final Instant DEFAULT_SEALED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SEALED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_GO_LIVE_AUTHORIZED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_GO_LIVE_AUTHORIZED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/commissioning-files";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CommissioningFileRepository commissioningFileRepository;

    @Mock
    private CommissioningFileRepository commissioningFileRepositoryMock;

    @Autowired
    private CommissioningFileMapper commissioningFileMapper;

    @Mock
    private CommissioningFileService commissioningFileServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCommissioningFileMockMvc;

    private CommissioningFile commissioningFile;

    private CommissioningFile insertedCommissioningFile;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CommissioningFile createEntity() {
        return new CommissioningFile()
            .fileCode(DEFAULT_FILE_CODE)
            .liaisonId(DEFAULT_LIAISON_ID)
            .institutionId(DEFAULT_INSTITUTION_ID)
            .projectLeadId(DEFAULT_PROJECT_LEAD_ID)
            .state(DEFAULT_STATE)
            .basisDigest(DEFAULT_BASIS_DIGEST)
            .basisTimestampToken(DEFAULT_BASIS_TIMESTAMP_TOKEN)
            .basisTimestampTokenContentType(DEFAULT_BASIS_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .basisSealedAt(DEFAULT_BASIS_SEALED_AT)
            .openedAt(DEFAULT_OPENED_AT)
            .correctionReason(DEFAULT_CORRECTION_REASON)
            .manifestDigest(DEFAULT_MANIFEST_DIGEST)
            .timestampToken(DEFAULT_TIMESTAMP_TOKEN)
            .timestampTokenContentType(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .sealedAt(DEFAULT_SEALED_AT)
            .goLiveAuthorizedAt(DEFAULT_GO_LIVE_AUTHORIZED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CommissioningFile createUpdatedEntity() {
        return new CommissioningFile()
            .fileCode(UPDATED_FILE_CODE)
            .liaisonId(UPDATED_LIAISON_ID)
            .institutionId(UPDATED_INSTITUTION_ID)
            .projectLeadId(UPDATED_PROJECT_LEAD_ID)
            .state(UPDATED_STATE)
            .basisDigest(UPDATED_BASIS_DIGEST)
            .basisTimestampToken(UPDATED_BASIS_TIMESTAMP_TOKEN)
            .basisTimestampTokenContentType(UPDATED_BASIS_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .basisSealedAt(UPDATED_BASIS_SEALED_AT)
            .openedAt(UPDATED_OPENED_AT)
            .correctionReason(UPDATED_CORRECTION_REASON)
            .manifestDigest(UPDATED_MANIFEST_DIGEST)
            .timestampToken(UPDATED_TIMESTAMP_TOKEN)
            .timestampTokenContentType(UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .sealedAt(UPDATED_SEALED_AT)
            .goLiveAuthorizedAt(UPDATED_GO_LIVE_AUTHORIZED_AT);
    }

    @BeforeEach
    void initTest() {
        commissioningFile = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCommissioningFile != null) {
            commissioningFileRepository.delete(insertedCommissioningFile);
            insertedCommissioningFile = null;
        }
    }

    @Test
    @Transactional
    void getAllCommissioningFiles() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList
        restCommissioningFileMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(commissioningFile.getId().toString())))
            .andExpect(jsonPath("$.[*].fileCode").value(hasItem(DEFAULT_FILE_CODE)))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].projectLeadId").value(hasItem(DEFAULT_PROJECT_LEAD_ID.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].basisDigest").value(hasItem(DEFAULT_BASIS_DIGEST)))
            .andExpect(jsonPath("$.[*].basisTimestampTokenContentType").value(hasItem(DEFAULT_BASIS_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(
                jsonPath("$.[*].basisTimestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_BASIS_TIMESTAMP_TOKEN)))
            )
            .andExpect(jsonPath("$.[*].basisSealedAt").value(hasItem(DEFAULT_BASIS_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].openedAt").value(hasItem(DEFAULT_OPENED_AT.toString())))
            .andExpect(jsonPath("$.[*].correctionReason").value(hasItem(DEFAULT_CORRECTION_REASON)))
            .andExpect(jsonPath("$.[*].manifestDigest").value(hasItem(DEFAULT_MANIFEST_DIGEST)))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].goLiveAuthorizedAt").value(hasItem(DEFAULT_GO_LIVE_AUTHORIZED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCommissioningFilesWithEagerRelationshipsIsEnabled() throws Exception {
        when(commissioningFileServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCommissioningFileMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(commissioningFileServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCommissioningFilesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(commissioningFileServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCommissioningFileMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(commissioningFileRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCommissioningFile() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get the commissioningFile
        restCommissioningFileMockMvc
            .perform(get(ENTITY_API_URL_ID, commissioningFile.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(commissioningFile.getId().toString()))
            .andExpect(jsonPath("$.fileCode").value(DEFAULT_FILE_CODE))
            .andExpect(jsonPath("$.liaisonId").value(DEFAULT_LIAISON_ID.toString()))
            .andExpect(jsonPath("$.institutionId").value(DEFAULT_INSTITUTION_ID.toString()))
            .andExpect(jsonPath("$.projectLeadId").value(DEFAULT_PROJECT_LEAD_ID.toString()))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.basisDigest").value(DEFAULT_BASIS_DIGEST))
            .andExpect(jsonPath("$.basisTimestampTokenContentType").value(DEFAULT_BASIS_TIMESTAMP_TOKEN_CONTENT_TYPE))
            .andExpect(jsonPath("$.basisTimestampToken").value(Base64.getEncoder().encodeToString(DEFAULT_BASIS_TIMESTAMP_TOKEN)))
            .andExpect(jsonPath("$.basisSealedAt").value(DEFAULT_BASIS_SEALED_AT.toString()))
            .andExpect(jsonPath("$.openedAt").value(DEFAULT_OPENED_AT.toString()))
            .andExpect(jsonPath("$.correctionReason").value(DEFAULT_CORRECTION_REASON))
            .andExpect(jsonPath("$.manifestDigest").value(DEFAULT_MANIFEST_DIGEST))
            .andExpect(jsonPath("$.timestampTokenContentType").value(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE))
            .andExpect(jsonPath("$.timestampToken").value(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN)))
            .andExpect(jsonPath("$.sealedAt").value(DEFAULT_SEALED_AT.toString()))
            .andExpect(jsonPath("$.goLiveAuthorizedAt").value(DEFAULT_GO_LIVE_AUTHORIZED_AT.toString()));
    }

    @Test
    @Transactional
    void getCommissioningFilesByIdFiltering() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        UUID id = commissioningFile.getId();

        defaultCommissioningFileFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByFileCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where fileCode equals to
        defaultCommissioningFileFiltering("fileCode.equals=" + DEFAULT_FILE_CODE, "fileCode.equals=" + UPDATED_FILE_CODE);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByFileCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where fileCode in
        defaultCommissioningFileFiltering("fileCode.in=" + DEFAULT_FILE_CODE + "," + UPDATED_FILE_CODE, "fileCode.in=" + UPDATED_FILE_CODE);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByFileCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where fileCode is not null
        defaultCommissioningFileFiltering("fileCode.specified=true", "fileCode.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByFileCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where fileCode contains
        defaultCommissioningFileFiltering("fileCode.contains=" + DEFAULT_FILE_CODE, "fileCode.contains=" + UPDATED_FILE_CODE);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByFileCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where fileCode does not contain
        defaultCommissioningFileFiltering("fileCode.doesNotContain=" + UPDATED_FILE_CODE, "fileCode.doesNotContain=" + DEFAULT_FILE_CODE);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByLiaisonIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where liaisonId equals to
        defaultCommissioningFileFiltering("liaisonId.equals=" + DEFAULT_LIAISON_ID, "liaisonId.equals=" + UPDATED_LIAISON_ID);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByLiaisonIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where liaisonId in
        defaultCommissioningFileFiltering(
            "liaisonId.in=" + DEFAULT_LIAISON_ID + "," + UPDATED_LIAISON_ID,
            "liaisonId.in=" + UPDATED_LIAISON_ID
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByLiaisonIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where liaisonId is not null
        defaultCommissioningFileFiltering("liaisonId.specified=true", "liaisonId.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByInstitutionIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where institutionId equals to
        defaultCommissioningFileFiltering(
            "institutionId.equals=" + DEFAULT_INSTITUTION_ID,
            "institutionId.equals=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByInstitutionIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where institutionId in
        defaultCommissioningFileFiltering(
            "institutionId.in=" + DEFAULT_INSTITUTION_ID + "," + UPDATED_INSTITUTION_ID,
            "institutionId.in=" + UPDATED_INSTITUTION_ID
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByInstitutionIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where institutionId is not null
        defaultCommissioningFileFiltering("institutionId.specified=true", "institutionId.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByProjectLeadIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where projectLeadId equals to
        defaultCommissioningFileFiltering(
            "projectLeadId.equals=" + DEFAULT_PROJECT_LEAD_ID,
            "projectLeadId.equals=" + UPDATED_PROJECT_LEAD_ID
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByProjectLeadIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where projectLeadId in
        defaultCommissioningFileFiltering(
            "projectLeadId.in=" + DEFAULT_PROJECT_LEAD_ID + "," + UPDATED_PROJECT_LEAD_ID,
            "projectLeadId.in=" + UPDATED_PROJECT_LEAD_ID
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByProjectLeadIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where projectLeadId is not null
        defaultCommissioningFileFiltering("projectLeadId.specified=true", "projectLeadId.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where state equals to
        defaultCommissioningFileFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where state in
        defaultCommissioningFileFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where state is not null
        defaultCommissioningFileFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByBasisDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where basisDigest equals to
        defaultCommissioningFileFiltering("basisDigest.equals=" + DEFAULT_BASIS_DIGEST, "basisDigest.equals=" + UPDATED_BASIS_DIGEST);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByBasisDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where basisDigest in
        defaultCommissioningFileFiltering(
            "basisDigest.in=" + DEFAULT_BASIS_DIGEST + "," + UPDATED_BASIS_DIGEST,
            "basisDigest.in=" + UPDATED_BASIS_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByBasisDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where basisDigest is not null
        defaultCommissioningFileFiltering("basisDigest.specified=true", "basisDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByBasisDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where basisDigest contains
        defaultCommissioningFileFiltering("basisDigest.contains=" + DEFAULT_BASIS_DIGEST, "basisDigest.contains=" + UPDATED_BASIS_DIGEST);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByBasisDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where basisDigest does not contain
        defaultCommissioningFileFiltering(
            "basisDigest.doesNotContain=" + UPDATED_BASIS_DIGEST,
            "basisDigest.doesNotContain=" + DEFAULT_BASIS_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByBasisSealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where basisSealedAt equals to
        defaultCommissioningFileFiltering(
            "basisSealedAt.equals=" + DEFAULT_BASIS_SEALED_AT,
            "basisSealedAt.equals=" + UPDATED_BASIS_SEALED_AT
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByBasisSealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where basisSealedAt in
        defaultCommissioningFileFiltering(
            "basisSealedAt.in=" + DEFAULT_BASIS_SEALED_AT + "," + UPDATED_BASIS_SEALED_AT,
            "basisSealedAt.in=" + UPDATED_BASIS_SEALED_AT
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByBasisSealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where basisSealedAt is not null
        defaultCommissioningFileFiltering("basisSealedAt.specified=true", "basisSealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByOpenedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where openedAt equals to
        defaultCommissioningFileFiltering("openedAt.equals=" + DEFAULT_OPENED_AT, "openedAt.equals=" + UPDATED_OPENED_AT);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByOpenedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where openedAt in
        defaultCommissioningFileFiltering("openedAt.in=" + DEFAULT_OPENED_AT + "," + UPDATED_OPENED_AT, "openedAt.in=" + UPDATED_OPENED_AT);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByOpenedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where openedAt is not null
        defaultCommissioningFileFiltering("openedAt.specified=true", "openedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByCorrectionReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where correctionReason equals to
        defaultCommissioningFileFiltering(
            "correctionReason.equals=" + DEFAULT_CORRECTION_REASON,
            "correctionReason.equals=" + UPDATED_CORRECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByCorrectionReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where correctionReason in
        defaultCommissioningFileFiltering(
            "correctionReason.in=" + DEFAULT_CORRECTION_REASON + "," + UPDATED_CORRECTION_REASON,
            "correctionReason.in=" + UPDATED_CORRECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByCorrectionReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where correctionReason is not null
        defaultCommissioningFileFiltering("correctionReason.specified=true", "correctionReason.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByCorrectionReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where correctionReason contains
        defaultCommissioningFileFiltering(
            "correctionReason.contains=" + DEFAULT_CORRECTION_REASON,
            "correctionReason.contains=" + UPDATED_CORRECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByCorrectionReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where correctionReason does not contain
        defaultCommissioningFileFiltering(
            "correctionReason.doesNotContain=" + UPDATED_CORRECTION_REASON,
            "correctionReason.doesNotContain=" + DEFAULT_CORRECTION_REASON
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByManifestDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where manifestDigest equals to
        defaultCommissioningFileFiltering(
            "manifestDigest.equals=" + DEFAULT_MANIFEST_DIGEST,
            "manifestDigest.equals=" + UPDATED_MANIFEST_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByManifestDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where manifestDigest in
        defaultCommissioningFileFiltering(
            "manifestDigest.in=" + DEFAULT_MANIFEST_DIGEST + "," + UPDATED_MANIFEST_DIGEST,
            "manifestDigest.in=" + UPDATED_MANIFEST_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByManifestDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where manifestDigest is not null
        defaultCommissioningFileFiltering("manifestDigest.specified=true", "manifestDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByManifestDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where manifestDigest contains
        defaultCommissioningFileFiltering(
            "manifestDigest.contains=" + DEFAULT_MANIFEST_DIGEST,
            "manifestDigest.contains=" + UPDATED_MANIFEST_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByManifestDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where manifestDigest does not contain
        defaultCommissioningFileFiltering(
            "manifestDigest.doesNotContain=" + UPDATED_MANIFEST_DIGEST,
            "manifestDigest.doesNotContain=" + DEFAULT_MANIFEST_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesBySealedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where sealedAt equals to
        defaultCommissioningFileFiltering("sealedAt.equals=" + DEFAULT_SEALED_AT, "sealedAt.equals=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesBySealedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where sealedAt in
        defaultCommissioningFileFiltering("sealedAt.in=" + DEFAULT_SEALED_AT + "," + UPDATED_SEALED_AT, "sealedAt.in=" + UPDATED_SEALED_AT);
    }

    @Test
    @Transactional
    void getAllCommissioningFilesBySealedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where sealedAt is not null
        defaultCommissioningFileFiltering("sealedAt.specified=true", "sealedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByGoLiveAuthorizedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where goLiveAuthorizedAt equals to
        defaultCommissioningFileFiltering(
            "goLiveAuthorizedAt.equals=" + DEFAULT_GO_LIVE_AUTHORIZED_AT,
            "goLiveAuthorizedAt.equals=" + UPDATED_GO_LIVE_AUTHORIZED_AT
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByGoLiveAuthorizedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where goLiveAuthorizedAt in
        defaultCommissioningFileFiltering(
            "goLiveAuthorizedAt.in=" + DEFAULT_GO_LIVE_AUTHORIZED_AT + "," + UPDATED_GO_LIVE_AUTHORIZED_AT,
            "goLiveAuthorizedAt.in=" + UPDATED_GO_LIVE_AUTHORIZED_AT
        );
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByGoLiveAuthorizedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCommissioningFile = commissioningFileRepository.saveAndFlush(commissioningFile);

        // Get all the commissioningFileList where goLiveAuthorizedAt is not null
        defaultCommissioningFileFiltering("goLiveAuthorizedAt.specified=true", "goLiveAuthorizedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByChangeRecordIsEqualToSomething() throws Exception {
        ChangeRecord changeRecord;
        if (TestUtil.findAll(em, ChangeRecord.class).isEmpty()) {
            commissioningFileRepository.saveAndFlush(commissioningFile);
            changeRecord = ChangeRecordResourceIT.createEntity();
        } else {
            changeRecord = TestUtil.findAll(em, ChangeRecord.class).get(0);
        }
        em.persist(changeRecord);
        em.flush();
        commissioningFile.setChangeRecord(changeRecord);
        commissioningFileRepository.saveAndFlush(commissioningFile);
        UUID changeRecordId = changeRecord.getId();
        // Get all the commissioningFileList where changeRecord equals to changeRecordId
        defaultCommissioningFileShouldBeFound("changeRecordId.equals=" + changeRecordId);

        // Get all the commissioningFileList where changeRecord equals to UUID.randomUUID()
        defaultCommissioningFileShouldNotBeFound("changeRecordId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByAttestationIsEqualToSomething() throws Exception {
        CertificationAttestation attestation;
        if (TestUtil.findAll(em, CertificationAttestation.class).isEmpty()) {
            commissioningFileRepository.saveAndFlush(commissioningFile);
            attestation = CertificationAttestationResourceIT.createEntity();
        } else {
            attestation = TestUtil.findAll(em, CertificationAttestation.class).get(0);
        }
        em.persist(attestation);
        em.flush();
        commissioningFile.setAttestation(attestation);
        commissioningFileRepository.saveAndFlush(commissioningFile);
        UUID attestationId = attestation.getId();
        // Get all the commissioningFileList where attestation equals to attestationId
        defaultCommissioningFileShouldBeFound("attestationId.equals=" + attestationId);

        // Get all the commissioningFileList where attestation equals to UUID.randomUUID()
        defaultCommissioningFileShouldNotBeFound("attestationId.equals=" + UUID.randomUUID());
    }

    @Test
    @Transactional
    void getAllCommissioningFilesByCommissioningReportIsEqualToSomething() throws Exception {
        EvidenceItem commissioningReport;
        if (TestUtil.findAll(em, EvidenceItem.class).isEmpty()) {
            commissioningFileRepository.saveAndFlush(commissioningFile);
            commissioningReport = EvidenceItemResourceIT.createEntity(em);
        } else {
            commissioningReport = TestUtil.findAll(em, EvidenceItem.class).get(0);
        }
        em.persist(commissioningReport);
        em.flush();
        commissioningFile.setCommissioningReport(commissioningReport);
        commissioningFileRepository.saveAndFlush(commissioningFile);
        UUID commissioningReportId = commissioningReport.getId();
        // Get all the commissioningFileList where commissioningReport equals to commissioningReportId
        defaultCommissioningFileShouldBeFound("commissioningReportId.equals=" + commissioningReportId);

        // Get all the commissioningFileList where commissioningReport equals to UUID.randomUUID()
        defaultCommissioningFileShouldNotBeFound("commissioningReportId.equals=" + UUID.randomUUID());
    }

    private void defaultCommissioningFileFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCommissioningFileShouldBeFound(shouldBeFound);
        defaultCommissioningFileShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCommissioningFileShouldBeFound(String filter) throws Exception {
        restCommissioningFileMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(commissioningFile.getId().toString())))
            .andExpect(jsonPath("$.[*].fileCode").value(hasItem(DEFAULT_FILE_CODE)))
            .andExpect(jsonPath("$.[*].liaisonId").value(hasItem(DEFAULT_LIAISON_ID.toString())))
            .andExpect(jsonPath("$.[*].institutionId").value(hasItem(DEFAULT_INSTITUTION_ID.toString())))
            .andExpect(jsonPath("$.[*].projectLeadId").value(hasItem(DEFAULT_PROJECT_LEAD_ID.toString())))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].basisDigest").value(hasItem(DEFAULT_BASIS_DIGEST)))
            .andExpect(jsonPath("$.[*].basisTimestampTokenContentType").value(hasItem(DEFAULT_BASIS_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(
                jsonPath("$.[*].basisTimestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_BASIS_TIMESTAMP_TOKEN)))
            )
            .andExpect(jsonPath("$.[*].basisSealedAt").value(hasItem(DEFAULT_BASIS_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].openedAt").value(hasItem(DEFAULT_OPENED_AT.toString())))
            .andExpect(jsonPath("$.[*].correctionReason").value(hasItem(DEFAULT_CORRECTION_REASON)))
            .andExpect(jsonPath("$.[*].manifestDigest").value(hasItem(DEFAULT_MANIFEST_DIGEST)))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].sealedAt").value(hasItem(DEFAULT_SEALED_AT.toString())))
            .andExpect(jsonPath("$.[*].goLiveAuthorizedAt").value(hasItem(DEFAULT_GO_LIVE_AUTHORIZED_AT.toString())));

        // Check, that the count call also returns 1
        restCommissioningFileMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCommissioningFileShouldNotBeFound(String filter) throws Exception {
        restCommissioningFileMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCommissioningFileMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCommissioningFile() throws Exception {
        // Get the commissioningFile
        restCommissioningFileMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return commissioningFileRepository.count();
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

    protected CommissioningFile getPersistedCommissioningFile(CommissioningFile commissioningFile) {
        return commissioningFileRepository.findById(commissioningFile.getId()).orElseThrow();
    }

    protected void assertPersistedCommissioningFileToMatchAllProperties(CommissioningFile expectedCommissioningFile) {
        assertCommissioningFileAllPropertiesEquals(expectedCommissioningFile, getPersistedCommissioningFile(expectedCommissioningFile));
    }

    protected void assertPersistedCommissioningFileToMatchUpdatableProperties(CommissioningFile expectedCommissioningFile) {
        assertCommissioningFileAllUpdatablePropertiesEquals(
            expectedCommissioningFile,
            getPersistedCommissioningFile(expectedCommissioningFile)
        );
    }
}
