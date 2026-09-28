package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.QuitusAsserts.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.CommissioningFile;
import cm.gimac.des.certification.domain.Quitus;
import cm.gimac.des.certification.domain.enumeration.QuitusDecision;
import cm.gimac.des.certification.domain.enumeration.QuitusStatus;
import cm.gimac.des.certification.domain.enumeration.QuitusType;
import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.service.QuitusService;
import cm.gimac.des.certification.service.mapper.QuitusMapper;
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
 * Integration tests for the {@link QuitusResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class QuitusResourceIT {

    private static final QuitusType DEFAULT_TYPE = QuitusType.TECHNIQUE;
    private static final QuitusType UPDATED_TYPE = QuitusType.SECURITE;

    private static final QuitusStatus DEFAULT_STATUS = QuitusStatus.ATTENDU;
    private static final QuitusStatus UPDATED_STATUS = QuitusStatus.SIGNE;

    private static final QuitusDecision DEFAULT_DECISION = QuitusDecision.FAVORABLE;
    private static final QuitusDecision UPDATED_DECISION = QuitusDecision.DEFAVORABLE;

    private static final UUID DEFAULT_SIGNER_ID = UUID.randomUUID();
    private static final UUID UPDATED_SIGNER_ID = UUID.randomUUID();

    private static final String DEFAULT_REASON = "AAAAAAAAAA";
    private static final String UPDATED_REASON = "BBBBBBBBBB";

    private static final String DEFAULT_BASIS_DIGEST = "af25759d0abbbcdd56bc95b8dff42efadbb04fa513df266dd8ae7b56b8ebec88";
    private static final String UPDATED_BASIS_DIGEST = "c963a79858e4b4f2d5073caf3534aa48f4a5c28c126b64b330ba3fa8ce0ad013";

    private static final String DEFAULT_DECISION_DIGEST = "055dcf1e9884a2dd5e9d8dd2c11df09089f7719843a6f4df223e54931e30984c";
    private static final String UPDATED_DECISION_DIGEST = "68a0813bbe4a5cbe12fc982c42091fd9afeb14a0f707f7804ef45341e49edef1";

    private static final String DEFAULT_CREDENTIAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_CREDENTIAL_ID = "BBBBBBBBBB";

    private static final byte[] DEFAULT_AUTHENTICATOR_DATA = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_AUTHENTICATOR_DATA = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_AUTHENTICATOR_DATA_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_AUTHENTICATOR_DATA_CONTENT_TYPE = "image/png";

    private static final String DEFAULT_CLIENT_DATA_JSON = "AAAAAAAAAA";
    private static final String UPDATED_CLIENT_DATA_JSON = "BBBBBBBBBB";

    private static final byte[] DEFAULT_SIGNATURE = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_SIGNATURE = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_SIGNATURE_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_SIGNATURE_CONTENT_TYPE = "image/png";

    private static final Long DEFAULT_SIGN_COUNT = 0L;
    private static final Long UPDATED_SIGN_COUNT = 1L;
    private static final Long SMALLER_SIGN_COUNT = 0L - 1L;

    private static final byte[] DEFAULT_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_TIMESTAMP_TOKEN = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE = "image/png";

    private static final Instant DEFAULT_SIGNED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_SIGNED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_DUE_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DUE_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_INVALIDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_INVALIDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/quituses";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private QuitusRepository quitusRepository;

    @Mock
    private QuitusRepository quitusRepositoryMock;

    @Autowired
    private QuitusMapper quitusMapper;

    @Mock
    private QuitusService quitusServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restQuitusMockMvc;

    private Quitus quitus;

    private Quitus insertedQuitus;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Quitus createEntity(EntityManager em) {
        Quitus quitus = new Quitus()
            .type(DEFAULT_TYPE)
            .status(DEFAULT_STATUS)
            .decision(DEFAULT_DECISION)
            .signerId(DEFAULT_SIGNER_ID)
            .reason(DEFAULT_REASON)
            .basisDigest(DEFAULT_BASIS_DIGEST)
            .decisionDigest(DEFAULT_DECISION_DIGEST)
            .credentialId(DEFAULT_CREDENTIAL_ID)
            .authenticatorData(DEFAULT_AUTHENTICATOR_DATA)
            .authenticatorDataContentType(DEFAULT_AUTHENTICATOR_DATA_CONTENT_TYPE)
            .clientDataJson(DEFAULT_CLIENT_DATA_JSON)
            .signature(DEFAULT_SIGNATURE)
            .signatureContentType(DEFAULT_SIGNATURE_CONTENT_TYPE)
            .signCount(DEFAULT_SIGN_COUNT)
            .timestampToken(DEFAULT_TIMESTAMP_TOKEN)
            .timestampTokenContentType(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .signedAt(DEFAULT_SIGNED_AT)
            .dueAt(DEFAULT_DUE_AT)
            .invalidatedAt(DEFAULT_INVALIDATED_AT);
        // Add required entity
        CommissioningFile commissioningFile;
        if (TestUtil.findAll(em, CommissioningFile.class).isEmpty()) {
            commissioningFile = CommissioningFileResourceIT.createEntity();
            em.persist(commissioningFile);
            em.flush();
        } else {
            commissioningFile = TestUtil.findAll(em, CommissioningFile.class).get(0);
        }
        quitus.setFile(commissioningFile);
        return quitus;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Quitus createUpdatedEntity(EntityManager em) {
        Quitus updatedQuitus = new Quitus()
            .type(UPDATED_TYPE)
            .status(UPDATED_STATUS)
            .decision(UPDATED_DECISION)
            .signerId(UPDATED_SIGNER_ID)
            .reason(UPDATED_REASON)
            .basisDigest(UPDATED_BASIS_DIGEST)
            .decisionDigest(UPDATED_DECISION_DIGEST)
            .credentialId(UPDATED_CREDENTIAL_ID)
            .authenticatorData(UPDATED_AUTHENTICATOR_DATA)
            .authenticatorDataContentType(UPDATED_AUTHENTICATOR_DATA_CONTENT_TYPE)
            .clientDataJson(UPDATED_CLIENT_DATA_JSON)
            .signature(UPDATED_SIGNATURE)
            .signatureContentType(UPDATED_SIGNATURE_CONTENT_TYPE)
            .signCount(UPDATED_SIGN_COUNT)
            .timestampToken(UPDATED_TIMESTAMP_TOKEN)
            .timestampTokenContentType(UPDATED_TIMESTAMP_TOKEN_CONTENT_TYPE)
            .signedAt(UPDATED_SIGNED_AT)
            .dueAt(UPDATED_DUE_AT)
            .invalidatedAt(UPDATED_INVALIDATED_AT);
        // Add required entity
        CommissioningFile commissioningFile;
        if (TestUtil.findAll(em, CommissioningFile.class).isEmpty()) {
            commissioningFile = CommissioningFileResourceIT.createUpdatedEntity();
            em.persist(commissioningFile);
            em.flush();
        } else {
            commissioningFile = TestUtil.findAll(em, CommissioningFile.class).get(0);
        }
        updatedQuitus.setFile(commissioningFile);
        return updatedQuitus;
    }

    @BeforeEach
    void initTest() {
        quitus = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedQuitus != null) {
            quitusRepository.delete(insertedQuitus);
            insertedQuitus = null;
        }
    }

    @Test
    @Transactional
    void getAllQuituses() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList
        restQuitusMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(quitus.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].decision").value(hasItem(DEFAULT_DECISION.toString())))
            .andExpect(jsonPath("$.[*].signerId").value(hasItem(DEFAULT_SIGNER_ID.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].basisDigest").value(hasItem(DEFAULT_BASIS_DIGEST)))
            .andExpect(jsonPath("$.[*].decisionDigest").value(hasItem(DEFAULT_DECISION_DIGEST)))
            .andExpect(jsonPath("$.[*].credentialId").value(hasItem(DEFAULT_CREDENTIAL_ID)))
            .andExpect(jsonPath("$.[*].authenticatorDataContentType").value(hasItem(DEFAULT_AUTHENTICATOR_DATA_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].authenticatorData").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_AUTHENTICATOR_DATA))))
            .andExpect(jsonPath("$.[*].clientDataJson").value(hasItem(DEFAULT_CLIENT_DATA_JSON)))
            .andExpect(jsonPath("$.[*].signatureContentType").value(hasItem(DEFAULT_SIGNATURE_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].signature").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_SIGNATURE))))
            .andExpect(jsonPath("$.[*].signCount").value(hasItem(DEFAULT_SIGN_COUNT.intValue())))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].signedAt").value(hasItem(DEFAULT_SIGNED_AT.toString())))
            .andExpect(jsonPath("$.[*].dueAt").value(hasItem(DEFAULT_DUE_AT.toString())))
            .andExpect(jsonPath("$.[*].invalidatedAt").value(hasItem(DEFAULT_INVALIDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllQuitusesWithEagerRelationshipsIsEnabled() throws Exception {
        when(quitusServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restQuitusMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(quitusServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllQuitusesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(quitusServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restQuitusMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(quitusRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getQuitus() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get the quitus
        restQuitusMockMvc
            .perform(get(ENTITY_API_URL_ID, quitus.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(quitus.getId().toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.decision").value(DEFAULT_DECISION.toString()))
            .andExpect(jsonPath("$.signerId").value(DEFAULT_SIGNER_ID.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON))
            .andExpect(jsonPath("$.basisDigest").value(DEFAULT_BASIS_DIGEST))
            .andExpect(jsonPath("$.decisionDigest").value(DEFAULT_DECISION_DIGEST))
            .andExpect(jsonPath("$.credentialId").value(DEFAULT_CREDENTIAL_ID))
            .andExpect(jsonPath("$.authenticatorDataContentType").value(DEFAULT_AUTHENTICATOR_DATA_CONTENT_TYPE))
            .andExpect(jsonPath("$.authenticatorData").value(Base64.getEncoder().encodeToString(DEFAULT_AUTHENTICATOR_DATA)))
            .andExpect(jsonPath("$.clientDataJson").value(DEFAULT_CLIENT_DATA_JSON))
            .andExpect(jsonPath("$.signatureContentType").value(DEFAULT_SIGNATURE_CONTENT_TYPE))
            .andExpect(jsonPath("$.signature").value(Base64.getEncoder().encodeToString(DEFAULT_SIGNATURE)))
            .andExpect(jsonPath("$.signCount").value(DEFAULT_SIGN_COUNT.intValue()))
            .andExpect(jsonPath("$.timestampTokenContentType").value(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE))
            .andExpect(jsonPath("$.timestampToken").value(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN)))
            .andExpect(jsonPath("$.signedAt").value(DEFAULT_SIGNED_AT.toString()))
            .andExpect(jsonPath("$.dueAt").value(DEFAULT_DUE_AT.toString()))
            .andExpect(jsonPath("$.invalidatedAt").value(DEFAULT_INVALIDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getQuitusesByIdFiltering() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        UUID id = quitus.getId();

        defaultQuitusFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllQuitusesByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where type equals to
        defaultQuitusFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllQuitusesByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where type in
        defaultQuitusFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllQuitusesByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where type is not null
        defaultQuitusFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where status equals to
        defaultQuitusFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllQuitusesByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where status in
        defaultQuitusFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllQuitusesByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where status is not null
        defaultQuitusFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByDecisionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where decision equals to
        defaultQuitusFiltering("decision.equals=" + DEFAULT_DECISION, "decision.equals=" + UPDATED_DECISION);
    }

    @Test
    @Transactional
    void getAllQuitusesByDecisionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where decision in
        defaultQuitusFiltering("decision.in=" + DEFAULT_DECISION + "," + UPDATED_DECISION, "decision.in=" + UPDATED_DECISION);
    }

    @Test
    @Transactional
    void getAllQuitusesByDecisionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where decision is not null
        defaultQuitusFiltering("decision.specified=true", "decision.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesBySignerIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signerId equals to
        defaultQuitusFiltering("signerId.equals=" + DEFAULT_SIGNER_ID, "signerId.equals=" + UPDATED_SIGNER_ID);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignerIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signerId in
        defaultQuitusFiltering("signerId.in=" + DEFAULT_SIGNER_ID + "," + UPDATED_SIGNER_ID, "signerId.in=" + UPDATED_SIGNER_ID);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignerIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signerId is not null
        defaultQuitusFiltering("signerId.specified=true", "signerId.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByReasonIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where reason equals to
        defaultQuitusFiltering("reason.equals=" + DEFAULT_REASON, "reason.equals=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllQuitusesByReasonIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where reason in
        defaultQuitusFiltering("reason.in=" + DEFAULT_REASON + "," + UPDATED_REASON, "reason.in=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllQuitusesByReasonIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where reason is not null
        defaultQuitusFiltering("reason.specified=true", "reason.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByReasonContainsSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where reason contains
        defaultQuitusFiltering("reason.contains=" + DEFAULT_REASON, "reason.contains=" + UPDATED_REASON);
    }

    @Test
    @Transactional
    void getAllQuitusesByReasonNotContainsSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where reason does not contain
        defaultQuitusFiltering("reason.doesNotContain=" + UPDATED_REASON, "reason.doesNotContain=" + DEFAULT_REASON);
    }

    @Test
    @Transactional
    void getAllQuitusesByBasisDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where basisDigest equals to
        defaultQuitusFiltering("basisDigest.equals=" + DEFAULT_BASIS_DIGEST, "basisDigest.equals=" + UPDATED_BASIS_DIGEST);
    }

    @Test
    @Transactional
    void getAllQuitusesByBasisDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where basisDigest in
        defaultQuitusFiltering(
            "basisDigest.in=" + DEFAULT_BASIS_DIGEST + "," + UPDATED_BASIS_DIGEST,
            "basisDigest.in=" + UPDATED_BASIS_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllQuitusesByBasisDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where basisDigest is not null
        defaultQuitusFiltering("basisDigest.specified=true", "basisDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByBasisDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where basisDigest contains
        defaultQuitusFiltering("basisDigest.contains=" + DEFAULT_BASIS_DIGEST, "basisDigest.contains=" + UPDATED_BASIS_DIGEST);
    }

    @Test
    @Transactional
    void getAllQuitusesByBasisDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where basisDigest does not contain
        defaultQuitusFiltering("basisDigest.doesNotContain=" + UPDATED_BASIS_DIGEST, "basisDigest.doesNotContain=" + DEFAULT_BASIS_DIGEST);
    }

    @Test
    @Transactional
    void getAllQuitusesByDecisionDigestIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where decisionDigest equals to
        defaultQuitusFiltering("decisionDigest.equals=" + DEFAULT_DECISION_DIGEST, "decisionDigest.equals=" + UPDATED_DECISION_DIGEST);
    }

    @Test
    @Transactional
    void getAllQuitusesByDecisionDigestIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where decisionDigest in
        defaultQuitusFiltering(
            "decisionDigest.in=" + DEFAULT_DECISION_DIGEST + "," + UPDATED_DECISION_DIGEST,
            "decisionDigest.in=" + UPDATED_DECISION_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllQuitusesByDecisionDigestIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where decisionDigest is not null
        defaultQuitusFiltering("decisionDigest.specified=true", "decisionDigest.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByDecisionDigestContainsSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where decisionDigest contains
        defaultQuitusFiltering("decisionDigest.contains=" + DEFAULT_DECISION_DIGEST, "decisionDigest.contains=" + UPDATED_DECISION_DIGEST);
    }

    @Test
    @Transactional
    void getAllQuitusesByDecisionDigestNotContainsSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where decisionDigest does not contain
        defaultQuitusFiltering(
            "decisionDigest.doesNotContain=" + UPDATED_DECISION_DIGEST,
            "decisionDigest.doesNotContain=" + DEFAULT_DECISION_DIGEST
        );
    }

    @Test
    @Transactional
    void getAllQuitusesByCredentialIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where credentialId equals to
        defaultQuitusFiltering("credentialId.equals=" + DEFAULT_CREDENTIAL_ID, "credentialId.equals=" + UPDATED_CREDENTIAL_ID);
    }

    @Test
    @Transactional
    void getAllQuitusesByCredentialIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where credentialId in
        defaultQuitusFiltering(
            "credentialId.in=" + DEFAULT_CREDENTIAL_ID + "," + UPDATED_CREDENTIAL_ID,
            "credentialId.in=" + UPDATED_CREDENTIAL_ID
        );
    }

    @Test
    @Transactional
    void getAllQuitusesByCredentialIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where credentialId is not null
        defaultQuitusFiltering("credentialId.specified=true", "credentialId.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByCredentialIdContainsSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where credentialId contains
        defaultQuitusFiltering("credentialId.contains=" + DEFAULT_CREDENTIAL_ID, "credentialId.contains=" + UPDATED_CREDENTIAL_ID);
    }

    @Test
    @Transactional
    void getAllQuitusesByCredentialIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where credentialId does not contain
        defaultQuitusFiltering(
            "credentialId.doesNotContain=" + UPDATED_CREDENTIAL_ID,
            "credentialId.doesNotContain=" + DEFAULT_CREDENTIAL_ID
        );
    }

    @Test
    @Transactional
    void getAllQuitusesBySignCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signCount equals to
        defaultQuitusFiltering("signCount.equals=" + DEFAULT_SIGN_COUNT, "signCount.equals=" + UPDATED_SIGN_COUNT);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signCount in
        defaultQuitusFiltering("signCount.in=" + DEFAULT_SIGN_COUNT + "," + UPDATED_SIGN_COUNT, "signCount.in=" + UPDATED_SIGN_COUNT);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signCount is not null
        defaultQuitusFiltering("signCount.specified=true", "signCount.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesBySignCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signCount is greater than or equal to
        defaultQuitusFiltering("signCount.greaterThanOrEqual=" + DEFAULT_SIGN_COUNT, "signCount.greaterThanOrEqual=" + UPDATED_SIGN_COUNT);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signCount is less than or equal to
        defaultQuitusFiltering("signCount.lessThanOrEqual=" + DEFAULT_SIGN_COUNT, "signCount.lessThanOrEqual=" + SMALLER_SIGN_COUNT);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signCount is less than
        defaultQuitusFiltering("signCount.lessThan=" + UPDATED_SIGN_COUNT, "signCount.lessThan=" + DEFAULT_SIGN_COUNT);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signCount is greater than
        defaultQuitusFiltering("signCount.greaterThan=" + SMALLER_SIGN_COUNT, "signCount.greaterThan=" + DEFAULT_SIGN_COUNT);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signedAt equals to
        defaultQuitusFiltering("signedAt.equals=" + DEFAULT_SIGNED_AT, "signedAt.equals=" + UPDATED_SIGNED_AT);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signedAt in
        defaultQuitusFiltering("signedAt.in=" + DEFAULT_SIGNED_AT + "," + UPDATED_SIGNED_AT, "signedAt.in=" + UPDATED_SIGNED_AT);
    }

    @Test
    @Transactional
    void getAllQuitusesBySignedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where signedAt is not null
        defaultQuitusFiltering("signedAt.specified=true", "signedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByDueAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where dueAt equals to
        defaultQuitusFiltering("dueAt.equals=" + DEFAULT_DUE_AT, "dueAt.equals=" + UPDATED_DUE_AT);
    }

    @Test
    @Transactional
    void getAllQuitusesByDueAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where dueAt in
        defaultQuitusFiltering("dueAt.in=" + DEFAULT_DUE_AT + "," + UPDATED_DUE_AT, "dueAt.in=" + UPDATED_DUE_AT);
    }

    @Test
    @Transactional
    void getAllQuitusesByDueAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where dueAt is not null
        defaultQuitusFiltering("dueAt.specified=true", "dueAt.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByInvalidatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where invalidatedAt equals to
        defaultQuitusFiltering("invalidatedAt.equals=" + DEFAULT_INVALIDATED_AT, "invalidatedAt.equals=" + UPDATED_INVALIDATED_AT);
    }

    @Test
    @Transactional
    void getAllQuitusesByInvalidatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where invalidatedAt in
        defaultQuitusFiltering(
            "invalidatedAt.in=" + DEFAULT_INVALIDATED_AT + "," + UPDATED_INVALIDATED_AT,
            "invalidatedAt.in=" + UPDATED_INVALIDATED_AT
        );
    }

    @Test
    @Transactional
    void getAllQuitusesByInvalidatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedQuitus = quitusRepository.saveAndFlush(quitus);

        // Get all the quitusList where invalidatedAt is not null
        defaultQuitusFiltering("invalidatedAt.specified=true", "invalidatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllQuitusesByFileIsEqualToSomething() throws Exception {
        CommissioningFile file;
        if (TestUtil.findAll(em, CommissioningFile.class).isEmpty()) {
            quitusRepository.saveAndFlush(quitus);
            file = CommissioningFileResourceIT.createEntity();
        } else {
            file = TestUtil.findAll(em, CommissioningFile.class).get(0);
        }
        em.persist(file);
        em.flush();
        quitus.setFile(file);
        quitusRepository.saveAndFlush(quitus);
        UUID fileId = file.getId();
        // Get all the quitusList where file equals to fileId
        defaultQuitusShouldBeFound("fileId.equals=" + fileId);

        // Get all the quitusList where file equals to UUID.randomUUID()
        defaultQuitusShouldNotBeFound("fileId.equals=" + UUID.randomUUID());
    }

    private void defaultQuitusFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultQuitusShouldBeFound(shouldBeFound);
        defaultQuitusShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultQuitusShouldBeFound(String filter) throws Exception {
        restQuitusMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(quitus.getId().toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].decision").value(hasItem(DEFAULT_DECISION.toString())))
            .andExpect(jsonPath("$.[*].signerId").value(hasItem(DEFAULT_SIGNER_ID.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON)))
            .andExpect(jsonPath("$.[*].basisDigest").value(hasItem(DEFAULT_BASIS_DIGEST)))
            .andExpect(jsonPath("$.[*].decisionDigest").value(hasItem(DEFAULT_DECISION_DIGEST)))
            .andExpect(jsonPath("$.[*].credentialId").value(hasItem(DEFAULT_CREDENTIAL_ID)))
            .andExpect(jsonPath("$.[*].authenticatorDataContentType").value(hasItem(DEFAULT_AUTHENTICATOR_DATA_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].authenticatorData").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_AUTHENTICATOR_DATA))))
            .andExpect(jsonPath("$.[*].clientDataJson").value(hasItem(DEFAULT_CLIENT_DATA_JSON)))
            .andExpect(jsonPath("$.[*].signatureContentType").value(hasItem(DEFAULT_SIGNATURE_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].signature").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_SIGNATURE))))
            .andExpect(jsonPath("$.[*].signCount").value(hasItem(DEFAULT_SIGN_COUNT.intValue())))
            .andExpect(jsonPath("$.[*].timestampTokenContentType").value(hasItem(DEFAULT_TIMESTAMP_TOKEN_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].timestampToken").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_TIMESTAMP_TOKEN))))
            .andExpect(jsonPath("$.[*].signedAt").value(hasItem(DEFAULT_SIGNED_AT.toString())))
            .andExpect(jsonPath("$.[*].dueAt").value(hasItem(DEFAULT_DUE_AT.toString())))
            .andExpect(jsonPath("$.[*].invalidatedAt").value(hasItem(DEFAULT_INVALIDATED_AT.toString())));

        // Check, that the count call also returns 1
        restQuitusMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultQuitusShouldNotBeFound(String filter) throws Exception {
        restQuitusMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restQuitusMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingQuitus() throws Exception {
        // Get the quitus
        restQuitusMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    protected long getRepositoryCount() {
        return quitusRepository.count();
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

    protected Quitus getPersistedQuitus(Quitus quitus) {
        return quitusRepository.findById(quitus.getId()).orElseThrow();
    }

    protected void assertPersistedQuitusToMatchAllProperties(Quitus expectedQuitus) {
        assertQuitusAllPropertiesEquals(expectedQuitus, getPersistedQuitus(expectedQuitus));
    }

    protected void assertPersistedQuitusToMatchUpdatableProperties(Quitus expectedQuitus) {
        assertQuitusAllUpdatablePropertiesEquals(expectedQuitus, getPersistedQuitus(expectedQuitus));
    }
}
