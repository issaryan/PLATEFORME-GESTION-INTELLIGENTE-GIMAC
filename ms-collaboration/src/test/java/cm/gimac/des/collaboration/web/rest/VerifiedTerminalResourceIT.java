package cm.gimac.des.collaboration.web.rest;

import static cm.gimac.des.collaboration.domain.VerifiedTerminalAsserts.*;
import static cm.gimac.des.collaboration.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.collaboration.IntegrationTest;
import cm.gimac.des.collaboration.domain.VerifiedTerminal;
import cm.gimac.des.collaboration.domain.enumeration.VerificationState;
import cm.gimac.des.collaboration.repository.VerifiedTerminalRepository;
import cm.gimac.des.collaboration.service.dto.VerifiedTerminalDTO;
import cm.gimac.des.collaboration.service.mapper.VerifiedTerminalMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link VerifiedTerminalResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class VerifiedTerminalResourceIT {

    private static final UUID DEFAULT_USER_ID = UUID.randomUUID();
    private static final UUID UPDATED_USER_ID = UUID.randomUUID();

    private static final String DEFAULT_TERMINAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_TERMINAL_ID = "BBBBBBBBBB";

    private static final String DEFAULT_IDENTITY_KEY = "d5f47926e77b9b7b8d51db6536e8a96bf26780d7789a5d1fd2f04d096b8d930a";
    private static final String UPDATED_IDENTITY_KEY = "21bc64b81f8904a64cd67ef016d66b60ce528a8056010feed9ea62a777b29cfb";

    private static final String DEFAULT_AGREEMENT_KEY = "b0eb20ee921fa3c4d77f7052ca37ff8426c8124ee3aee023d0875ef695c4757d";
    private static final String UPDATED_AGREEMENT_KEY = "352e7fd6a7d6473aef02b43b17c8f0b9653807839c9ae193c10a4b257efa5c73";

    private static final String DEFAULT_SIGNED_PRE_KEY = "24f1fb479d2eff986634382659e58e5b191380c7096e03663789a7df4b8fb1d4";
    private static final String UPDATED_SIGNED_PRE_KEY = "d747d6dffb23388f2edb4af2f1055df6d06636bf80356d1b37c1317d5c095e7a";

    private static final String DEFAULT_SIGNED_PRE_KEY_SIGNATURE =
        "7db36ee3c29a1f4cf958c2db4b66f7ad6ed2410ed792d2e6991fba90fede3c04d75dde527af5326320a990406e953f79f7f324a69f5097a6d8a7c4d19390b4d3";
    private static final String UPDATED_SIGNED_PRE_KEY_SIGNATURE =
        "939b4dcc5c90948b36151b8d825daa7ec2c51fe6e94e848ce9751b2ec36b6b1e7bd3db2ac94772f4682101aad205662552d80309aec5e66eaeb80459f04961b5";

    private static final Integer DEFAULT_ONE_TIME_PRE_KEYS = 0;
    private static final Integer UPDATED_ONE_TIME_PRE_KEYS = 1;
    private static final Integer SMALLER_ONE_TIME_PRE_KEYS = 0 - 1;

    private static final String DEFAULT_CROSS_SIGNATURE =
        "d291973a5007e9c806b7c294c29b0713ae9b2b8f34783d03e3b271befa55ddb4211448cd78292443fa7e77544969693863aea3826b338ca182e71df599380488";
    private static final String UPDATED_CROSS_SIGNATURE =
        "cfadc74be320db61adbbf2faa95508098f153e73f306f31b6261ae69622e5fa7c3043778c4053c4dd9049fffa908ff6de6912fe079e739120c296a5f7a33d850";

    private static final VerificationState DEFAULT_STATE = VerificationState.NON_VERIFIE;
    private static final VerificationState UPDATED_STATE = VerificationState.VERIFIE;

    private static final Boolean DEFAULT_ARCHIVE_TERMINAL = false;
    private static final Boolean UPDATED_ARCHIVE_TERMINAL = true;

    private static final Instant DEFAULT_REGISTERED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REGISTERED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_VERIFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_VERIFIED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_REVOKED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REVOKED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/verified-terminals";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private VerifiedTerminalRepository verifiedTerminalRepository;

    @Autowired
    private VerifiedTerminalMapper verifiedTerminalMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restVerifiedTerminalMockMvc;

    private VerifiedTerminal verifiedTerminal;

    private VerifiedTerminal insertedVerifiedTerminal;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static VerifiedTerminal createEntity() {
        return new VerifiedTerminal()
            .userId(DEFAULT_USER_ID)
            .terminalId(DEFAULT_TERMINAL_ID)
            .identityKey(DEFAULT_IDENTITY_KEY)
            .agreementKey(DEFAULT_AGREEMENT_KEY)
            .signedPreKey(DEFAULT_SIGNED_PRE_KEY)
            .signedPreKeySignature(DEFAULT_SIGNED_PRE_KEY_SIGNATURE)
            .oneTimePreKeys(DEFAULT_ONE_TIME_PRE_KEYS)
            .crossSignature(DEFAULT_CROSS_SIGNATURE)
            .state(DEFAULT_STATE)
            .archiveTerminal(DEFAULT_ARCHIVE_TERMINAL)
            .registeredAt(DEFAULT_REGISTERED_AT)
            .verifiedAt(DEFAULT_VERIFIED_AT)
            .revokedAt(DEFAULT_REVOKED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static VerifiedTerminal createUpdatedEntity() {
        return new VerifiedTerminal()
            .userId(UPDATED_USER_ID)
            .terminalId(UPDATED_TERMINAL_ID)
            .identityKey(UPDATED_IDENTITY_KEY)
            .agreementKey(UPDATED_AGREEMENT_KEY)
            .signedPreKey(UPDATED_SIGNED_PRE_KEY)
            .signedPreKeySignature(UPDATED_SIGNED_PRE_KEY_SIGNATURE)
            .oneTimePreKeys(UPDATED_ONE_TIME_PRE_KEYS)
            .crossSignature(UPDATED_CROSS_SIGNATURE)
            .state(UPDATED_STATE)
            .archiveTerminal(UPDATED_ARCHIVE_TERMINAL)
            .registeredAt(UPDATED_REGISTERED_AT)
            .verifiedAt(UPDATED_VERIFIED_AT)
            .revokedAt(UPDATED_REVOKED_AT);
    }

    @BeforeEach
    void initTest() {
        verifiedTerminal = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedVerifiedTerminal != null) {
            verifiedTerminalRepository.delete(insertedVerifiedTerminal);
            insertedVerifiedTerminal = null;
        }
    }

    @Test
    @Transactional
    void createVerifiedTerminal() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the VerifiedTerminal
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);
        var returnedVerifiedTerminalDTO = om.readValue(
            restVerifiedTerminalMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(verifiedTerminalDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            VerifiedTerminalDTO.class
        );

        // Validate the VerifiedTerminal in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedVerifiedTerminal = verifiedTerminalMapper.toEntity(returnedVerifiedTerminalDTO);
        assertVerifiedTerminalUpdatableFieldsEquals(returnedVerifiedTerminal, getPersistedVerifiedTerminal(returnedVerifiedTerminal));

        insertedVerifiedTerminal = returnedVerifiedTerminal;
    }

    @Test
    @Transactional
    void createVerifiedTerminalWithExistingId() throws Exception {
        // Create the VerifiedTerminal with an existing ID
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the VerifiedTerminal in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUserIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setUserId(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTerminalIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setTerminalId(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIdentityKeyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setIdentityKey(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkAgreementKeyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setAgreementKey(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSignedPreKeyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setSignedPreKey(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkSignedPreKeySignatureIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setSignedPreKeySignature(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkOneTimePreKeysIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setOneTimePreKeys(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setState(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkArchiveTerminalIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setArchiveTerminal(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRegisteredAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        verifiedTerminal.setRegisteredAt(null);

        // Create the VerifiedTerminal, which fails.
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminals() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList
        restVerifiedTerminalMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(verifiedTerminal.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].terminalId").value(hasItem(DEFAULT_TERMINAL_ID)))
            .andExpect(jsonPath("$.[*].identityKey").value(hasItem(DEFAULT_IDENTITY_KEY)))
            .andExpect(jsonPath("$.[*].agreementKey").value(hasItem(DEFAULT_AGREEMENT_KEY)))
            .andExpect(jsonPath("$.[*].signedPreKey").value(hasItem(DEFAULT_SIGNED_PRE_KEY)))
            .andExpect(jsonPath("$.[*].signedPreKeySignature").value(hasItem(DEFAULT_SIGNED_PRE_KEY_SIGNATURE)))
            .andExpect(jsonPath("$.[*].oneTimePreKeys").value(hasItem(DEFAULT_ONE_TIME_PRE_KEYS)))
            .andExpect(jsonPath("$.[*].crossSignature").value(hasItem(DEFAULT_CROSS_SIGNATURE)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].archiveTerminal").value(hasItem(DEFAULT_ARCHIVE_TERMINAL)))
            .andExpect(jsonPath("$.[*].registeredAt").value(hasItem(DEFAULT_REGISTERED_AT.toString())))
            .andExpect(jsonPath("$.[*].verifiedAt").value(hasItem(DEFAULT_VERIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].revokedAt").value(hasItem(DEFAULT_REVOKED_AT.toString())));
    }

    @Test
    @Transactional
    void getVerifiedTerminal() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get the verifiedTerminal
        restVerifiedTerminalMockMvc
            .perform(get(ENTITY_API_URL_ID, verifiedTerminal.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(verifiedTerminal.getId().toString()))
            .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID.toString()))
            .andExpect(jsonPath("$.terminalId").value(DEFAULT_TERMINAL_ID))
            .andExpect(jsonPath("$.identityKey").value(DEFAULT_IDENTITY_KEY))
            .andExpect(jsonPath("$.agreementKey").value(DEFAULT_AGREEMENT_KEY))
            .andExpect(jsonPath("$.signedPreKey").value(DEFAULT_SIGNED_PRE_KEY))
            .andExpect(jsonPath("$.signedPreKeySignature").value(DEFAULT_SIGNED_PRE_KEY_SIGNATURE))
            .andExpect(jsonPath("$.oneTimePreKeys").value(DEFAULT_ONE_TIME_PRE_KEYS))
            .andExpect(jsonPath("$.crossSignature").value(DEFAULT_CROSS_SIGNATURE))
            .andExpect(jsonPath("$.state").value(DEFAULT_STATE.toString()))
            .andExpect(jsonPath("$.archiveTerminal").value(DEFAULT_ARCHIVE_TERMINAL))
            .andExpect(jsonPath("$.registeredAt").value(DEFAULT_REGISTERED_AT.toString()))
            .andExpect(jsonPath("$.verifiedAt").value(DEFAULT_VERIFIED_AT.toString()))
            .andExpect(jsonPath("$.revokedAt").value(DEFAULT_REVOKED_AT.toString()));
    }

    @Test
    @Transactional
    void getVerifiedTerminalsByIdFiltering() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        UUID id = verifiedTerminal.getId();

        defaultVerifiedTerminalFiltering("id.equals=" + id, "id.notEquals=" + id);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByUserIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where userId equals to
        defaultVerifiedTerminalFiltering("userId.equals=" + DEFAULT_USER_ID, "userId.equals=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByUserIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where userId in
        defaultVerifiedTerminalFiltering("userId.in=" + DEFAULT_USER_ID + "," + UPDATED_USER_ID, "userId.in=" + UPDATED_USER_ID);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByUserIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where userId is not null
        defaultVerifiedTerminalFiltering("userId.specified=true", "userId.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByTerminalIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where terminalId equals to
        defaultVerifiedTerminalFiltering("terminalId.equals=" + DEFAULT_TERMINAL_ID, "terminalId.equals=" + UPDATED_TERMINAL_ID);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByTerminalIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where terminalId in
        defaultVerifiedTerminalFiltering(
            "terminalId.in=" + DEFAULT_TERMINAL_ID + "," + UPDATED_TERMINAL_ID,
            "terminalId.in=" + UPDATED_TERMINAL_ID
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByTerminalIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where terminalId is not null
        defaultVerifiedTerminalFiltering("terminalId.specified=true", "terminalId.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByTerminalIdContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where terminalId contains
        defaultVerifiedTerminalFiltering("terminalId.contains=" + DEFAULT_TERMINAL_ID, "terminalId.contains=" + UPDATED_TERMINAL_ID);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByTerminalIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where terminalId does not contain
        defaultVerifiedTerminalFiltering(
            "terminalId.doesNotContain=" + UPDATED_TERMINAL_ID,
            "terminalId.doesNotContain=" + DEFAULT_TERMINAL_ID
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByIdentityKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where identityKey equals to
        defaultVerifiedTerminalFiltering("identityKey.equals=" + DEFAULT_IDENTITY_KEY, "identityKey.equals=" + UPDATED_IDENTITY_KEY);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByIdentityKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where identityKey in
        defaultVerifiedTerminalFiltering(
            "identityKey.in=" + DEFAULT_IDENTITY_KEY + "," + UPDATED_IDENTITY_KEY,
            "identityKey.in=" + UPDATED_IDENTITY_KEY
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByIdentityKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where identityKey is not null
        defaultVerifiedTerminalFiltering("identityKey.specified=true", "identityKey.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByIdentityKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where identityKey contains
        defaultVerifiedTerminalFiltering("identityKey.contains=" + DEFAULT_IDENTITY_KEY, "identityKey.contains=" + UPDATED_IDENTITY_KEY);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByIdentityKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where identityKey does not contain
        defaultVerifiedTerminalFiltering(
            "identityKey.doesNotContain=" + UPDATED_IDENTITY_KEY,
            "identityKey.doesNotContain=" + DEFAULT_IDENTITY_KEY
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByAgreementKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where agreementKey equals to
        defaultVerifiedTerminalFiltering("agreementKey.equals=" + DEFAULT_AGREEMENT_KEY, "agreementKey.equals=" + UPDATED_AGREEMENT_KEY);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByAgreementKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where agreementKey in
        defaultVerifiedTerminalFiltering(
            "agreementKey.in=" + DEFAULT_AGREEMENT_KEY + "," + UPDATED_AGREEMENT_KEY,
            "agreementKey.in=" + UPDATED_AGREEMENT_KEY
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByAgreementKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where agreementKey is not null
        defaultVerifiedTerminalFiltering("agreementKey.specified=true", "agreementKey.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByAgreementKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where agreementKey contains
        defaultVerifiedTerminalFiltering(
            "agreementKey.contains=" + DEFAULT_AGREEMENT_KEY,
            "agreementKey.contains=" + UPDATED_AGREEMENT_KEY
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByAgreementKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where agreementKey does not contain
        defaultVerifiedTerminalFiltering(
            "agreementKey.doesNotContain=" + UPDATED_AGREEMENT_KEY,
            "agreementKey.doesNotContain=" + DEFAULT_AGREEMENT_KEY
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeyIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKey equals to
        defaultVerifiedTerminalFiltering("signedPreKey.equals=" + DEFAULT_SIGNED_PRE_KEY, "signedPreKey.equals=" + UPDATED_SIGNED_PRE_KEY);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeyIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKey in
        defaultVerifiedTerminalFiltering(
            "signedPreKey.in=" + DEFAULT_SIGNED_PRE_KEY + "," + UPDATED_SIGNED_PRE_KEY,
            "signedPreKey.in=" + UPDATED_SIGNED_PRE_KEY
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeyIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKey is not null
        defaultVerifiedTerminalFiltering("signedPreKey.specified=true", "signedPreKey.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeyContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKey contains
        defaultVerifiedTerminalFiltering(
            "signedPreKey.contains=" + DEFAULT_SIGNED_PRE_KEY,
            "signedPreKey.contains=" + UPDATED_SIGNED_PRE_KEY
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeyNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKey does not contain
        defaultVerifiedTerminalFiltering(
            "signedPreKey.doesNotContain=" + UPDATED_SIGNED_PRE_KEY,
            "signedPreKey.doesNotContain=" + DEFAULT_SIGNED_PRE_KEY
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeySignatureIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKeySignature equals to
        defaultVerifiedTerminalFiltering(
            "signedPreKeySignature.equals=" + DEFAULT_SIGNED_PRE_KEY_SIGNATURE,
            "signedPreKeySignature.equals=" + UPDATED_SIGNED_PRE_KEY_SIGNATURE
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeySignatureIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKeySignature in
        defaultVerifiedTerminalFiltering(
            "signedPreKeySignature.in=" + DEFAULT_SIGNED_PRE_KEY_SIGNATURE + "," + UPDATED_SIGNED_PRE_KEY_SIGNATURE,
            "signedPreKeySignature.in=" + UPDATED_SIGNED_PRE_KEY_SIGNATURE
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeySignatureIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKeySignature is not null
        defaultVerifiedTerminalFiltering("signedPreKeySignature.specified=true", "signedPreKeySignature.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeySignatureContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKeySignature contains
        defaultVerifiedTerminalFiltering(
            "signedPreKeySignature.contains=" + DEFAULT_SIGNED_PRE_KEY_SIGNATURE,
            "signedPreKeySignature.contains=" + UPDATED_SIGNED_PRE_KEY_SIGNATURE
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsBySignedPreKeySignatureNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where signedPreKeySignature does not contain
        defaultVerifiedTerminalFiltering(
            "signedPreKeySignature.doesNotContain=" + UPDATED_SIGNED_PRE_KEY_SIGNATURE,
            "signedPreKeySignature.doesNotContain=" + DEFAULT_SIGNED_PRE_KEY_SIGNATURE
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByOneTimePreKeysIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where oneTimePreKeys equals to
        defaultVerifiedTerminalFiltering(
            "oneTimePreKeys.equals=" + DEFAULT_ONE_TIME_PRE_KEYS,
            "oneTimePreKeys.equals=" + UPDATED_ONE_TIME_PRE_KEYS
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByOneTimePreKeysIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where oneTimePreKeys in
        defaultVerifiedTerminalFiltering(
            "oneTimePreKeys.in=" + DEFAULT_ONE_TIME_PRE_KEYS + "," + UPDATED_ONE_TIME_PRE_KEYS,
            "oneTimePreKeys.in=" + UPDATED_ONE_TIME_PRE_KEYS
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByOneTimePreKeysIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where oneTimePreKeys is not null
        defaultVerifiedTerminalFiltering("oneTimePreKeys.specified=true", "oneTimePreKeys.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByOneTimePreKeysIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where oneTimePreKeys is greater than or equal to
        defaultVerifiedTerminalFiltering(
            "oneTimePreKeys.greaterThanOrEqual=" + DEFAULT_ONE_TIME_PRE_KEYS,
            "oneTimePreKeys.greaterThanOrEqual=" + UPDATED_ONE_TIME_PRE_KEYS
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByOneTimePreKeysIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where oneTimePreKeys is less than or equal to
        defaultVerifiedTerminalFiltering(
            "oneTimePreKeys.lessThanOrEqual=" + DEFAULT_ONE_TIME_PRE_KEYS,
            "oneTimePreKeys.lessThanOrEqual=" + SMALLER_ONE_TIME_PRE_KEYS
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByOneTimePreKeysIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where oneTimePreKeys is less than
        defaultVerifiedTerminalFiltering(
            "oneTimePreKeys.lessThan=" + UPDATED_ONE_TIME_PRE_KEYS,
            "oneTimePreKeys.lessThan=" + DEFAULT_ONE_TIME_PRE_KEYS
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByOneTimePreKeysIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where oneTimePreKeys is greater than
        defaultVerifiedTerminalFiltering(
            "oneTimePreKeys.greaterThan=" + SMALLER_ONE_TIME_PRE_KEYS,
            "oneTimePreKeys.greaterThan=" + DEFAULT_ONE_TIME_PRE_KEYS
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByCrossSignatureIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where crossSignature equals to
        defaultVerifiedTerminalFiltering(
            "crossSignature.equals=" + DEFAULT_CROSS_SIGNATURE,
            "crossSignature.equals=" + UPDATED_CROSS_SIGNATURE
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByCrossSignatureIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where crossSignature in
        defaultVerifiedTerminalFiltering(
            "crossSignature.in=" + DEFAULT_CROSS_SIGNATURE + "," + UPDATED_CROSS_SIGNATURE,
            "crossSignature.in=" + UPDATED_CROSS_SIGNATURE
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByCrossSignatureIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where crossSignature is not null
        defaultVerifiedTerminalFiltering("crossSignature.specified=true", "crossSignature.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByCrossSignatureContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where crossSignature contains
        defaultVerifiedTerminalFiltering(
            "crossSignature.contains=" + DEFAULT_CROSS_SIGNATURE,
            "crossSignature.contains=" + UPDATED_CROSS_SIGNATURE
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByCrossSignatureNotContainsSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where crossSignature does not contain
        defaultVerifiedTerminalFiltering(
            "crossSignature.doesNotContain=" + UPDATED_CROSS_SIGNATURE,
            "crossSignature.doesNotContain=" + DEFAULT_CROSS_SIGNATURE
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByStateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where state equals to
        defaultVerifiedTerminalFiltering("state.equals=" + DEFAULT_STATE, "state.equals=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByStateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where state in
        defaultVerifiedTerminalFiltering("state.in=" + DEFAULT_STATE + "," + UPDATED_STATE, "state.in=" + UPDATED_STATE);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByStateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where state is not null
        defaultVerifiedTerminalFiltering("state.specified=true", "state.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByArchiveTerminalIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where archiveTerminal equals to
        defaultVerifiedTerminalFiltering(
            "archiveTerminal.equals=" + DEFAULT_ARCHIVE_TERMINAL,
            "archiveTerminal.equals=" + UPDATED_ARCHIVE_TERMINAL
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByArchiveTerminalIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where archiveTerminal in
        defaultVerifiedTerminalFiltering(
            "archiveTerminal.in=" + DEFAULT_ARCHIVE_TERMINAL + "," + UPDATED_ARCHIVE_TERMINAL,
            "archiveTerminal.in=" + UPDATED_ARCHIVE_TERMINAL
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByArchiveTerminalIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where archiveTerminal is not null
        defaultVerifiedTerminalFiltering("archiveTerminal.specified=true", "archiveTerminal.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByRegisteredAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where registeredAt equals to
        defaultVerifiedTerminalFiltering("registeredAt.equals=" + DEFAULT_REGISTERED_AT, "registeredAt.equals=" + UPDATED_REGISTERED_AT);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByRegisteredAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where registeredAt in
        defaultVerifiedTerminalFiltering(
            "registeredAt.in=" + DEFAULT_REGISTERED_AT + "," + UPDATED_REGISTERED_AT,
            "registeredAt.in=" + UPDATED_REGISTERED_AT
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByRegisteredAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where registeredAt is not null
        defaultVerifiedTerminalFiltering("registeredAt.specified=true", "registeredAt.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByVerifiedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where verifiedAt equals to
        defaultVerifiedTerminalFiltering("verifiedAt.equals=" + DEFAULT_VERIFIED_AT, "verifiedAt.equals=" + UPDATED_VERIFIED_AT);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByVerifiedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where verifiedAt in
        defaultVerifiedTerminalFiltering(
            "verifiedAt.in=" + DEFAULT_VERIFIED_AT + "," + UPDATED_VERIFIED_AT,
            "verifiedAt.in=" + UPDATED_VERIFIED_AT
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByVerifiedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where verifiedAt is not null
        defaultVerifiedTerminalFiltering("verifiedAt.specified=true", "verifiedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByRevokedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where revokedAt equals to
        defaultVerifiedTerminalFiltering("revokedAt.equals=" + DEFAULT_REVOKED_AT, "revokedAt.equals=" + UPDATED_REVOKED_AT);
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByRevokedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where revokedAt in
        defaultVerifiedTerminalFiltering(
            "revokedAt.in=" + DEFAULT_REVOKED_AT + "," + UPDATED_REVOKED_AT,
            "revokedAt.in=" + UPDATED_REVOKED_AT
        );
    }

    @Test
    @Transactional
    void getAllVerifiedTerminalsByRevokedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        // Get all the verifiedTerminalList where revokedAt is not null
        defaultVerifiedTerminalFiltering("revokedAt.specified=true", "revokedAt.specified=false");
    }

    private void defaultVerifiedTerminalFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultVerifiedTerminalShouldBeFound(shouldBeFound);
        defaultVerifiedTerminalShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultVerifiedTerminalShouldBeFound(String filter) throws Exception {
        restVerifiedTerminalMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(verifiedTerminal.getId().toString())))
            .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID.toString())))
            .andExpect(jsonPath("$.[*].terminalId").value(hasItem(DEFAULT_TERMINAL_ID)))
            .andExpect(jsonPath("$.[*].identityKey").value(hasItem(DEFAULT_IDENTITY_KEY)))
            .andExpect(jsonPath("$.[*].agreementKey").value(hasItem(DEFAULT_AGREEMENT_KEY)))
            .andExpect(jsonPath("$.[*].signedPreKey").value(hasItem(DEFAULT_SIGNED_PRE_KEY)))
            .andExpect(jsonPath("$.[*].signedPreKeySignature").value(hasItem(DEFAULT_SIGNED_PRE_KEY_SIGNATURE)))
            .andExpect(jsonPath("$.[*].oneTimePreKeys").value(hasItem(DEFAULT_ONE_TIME_PRE_KEYS)))
            .andExpect(jsonPath("$.[*].crossSignature").value(hasItem(DEFAULT_CROSS_SIGNATURE)))
            .andExpect(jsonPath("$.[*].state").value(hasItem(DEFAULT_STATE.toString())))
            .andExpect(jsonPath("$.[*].archiveTerminal").value(hasItem(DEFAULT_ARCHIVE_TERMINAL)))
            .andExpect(jsonPath("$.[*].registeredAt").value(hasItem(DEFAULT_REGISTERED_AT.toString())))
            .andExpect(jsonPath("$.[*].verifiedAt").value(hasItem(DEFAULT_VERIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].revokedAt").value(hasItem(DEFAULT_REVOKED_AT.toString())));

        // Check, that the count call also returns 1
        restVerifiedTerminalMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultVerifiedTerminalShouldNotBeFound(String filter) throws Exception {
        restVerifiedTerminalMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restVerifiedTerminalMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingVerifiedTerminal() throws Exception {
        // Get the verifiedTerminal
        restVerifiedTerminalMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingVerifiedTerminal() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the verifiedTerminal
        VerifiedTerminal updatedVerifiedTerminal = verifiedTerminalRepository.findById(verifiedTerminal.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedVerifiedTerminal are not directly saved in db
        em.detach(updatedVerifiedTerminal);
        updatedVerifiedTerminal
            .userId(UPDATED_USER_ID)
            .terminalId(UPDATED_TERMINAL_ID)
            .identityKey(UPDATED_IDENTITY_KEY)
            .agreementKey(UPDATED_AGREEMENT_KEY)
            .signedPreKey(UPDATED_SIGNED_PRE_KEY)
            .signedPreKeySignature(UPDATED_SIGNED_PRE_KEY_SIGNATURE)
            .oneTimePreKeys(UPDATED_ONE_TIME_PRE_KEYS)
            .crossSignature(UPDATED_CROSS_SIGNATURE)
            .state(UPDATED_STATE)
            .archiveTerminal(UPDATED_ARCHIVE_TERMINAL)
            .registeredAt(UPDATED_REGISTERED_AT)
            .verifiedAt(UPDATED_VERIFIED_AT)
            .revokedAt(UPDATED_REVOKED_AT);
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(updatedVerifiedTerminal);

        restVerifiedTerminalMockMvc
            .perform(
                put(ENTITY_API_URL_ID, verifiedTerminalDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isOk());

        // Validate the VerifiedTerminal in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedVerifiedTerminalToMatchAllProperties(updatedVerifiedTerminal);
    }

    @Test
    @Transactional
    void putNonExistingVerifiedTerminal() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        verifiedTerminal.setId(UUID.randomUUID());

        // Create the VerifiedTerminal
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restVerifiedTerminalMockMvc
            .perform(
                put(ENTITY_API_URL_ID, verifiedTerminalDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the VerifiedTerminal in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchVerifiedTerminal() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        verifiedTerminal.setId(UUID.randomUUID());

        // Create the VerifiedTerminal
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVerifiedTerminalMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the VerifiedTerminal in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamVerifiedTerminal() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        verifiedTerminal.setId(UUID.randomUUID());

        // Create the VerifiedTerminal
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVerifiedTerminalMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the VerifiedTerminal in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateVerifiedTerminalWithPatch() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the verifiedTerminal using partial update
        VerifiedTerminal partialUpdatedVerifiedTerminal = new VerifiedTerminal();
        partialUpdatedVerifiedTerminal.setId(verifiedTerminal.getId());

        partialUpdatedVerifiedTerminal
            .terminalId(UPDATED_TERMINAL_ID)
            .identityKey(UPDATED_IDENTITY_KEY)
            .agreementKey(UPDATED_AGREEMENT_KEY)
            .signedPreKey(UPDATED_SIGNED_PRE_KEY)
            .oneTimePreKeys(UPDATED_ONE_TIME_PRE_KEYS)
            .crossSignature(UPDATED_CROSS_SIGNATURE)
            .state(UPDATED_STATE)
            .verifiedAt(UPDATED_VERIFIED_AT);

        restVerifiedTerminalMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedVerifiedTerminal.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedVerifiedTerminal))
            )
            .andExpect(status().isOk());

        // Validate the VerifiedTerminal in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertVerifiedTerminalUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedVerifiedTerminal, verifiedTerminal),
            getPersistedVerifiedTerminal(verifiedTerminal)
        );
    }

    @Test
    @Transactional
    void fullUpdateVerifiedTerminalWithPatch() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the verifiedTerminal using partial update
        VerifiedTerminal partialUpdatedVerifiedTerminal = new VerifiedTerminal();
        partialUpdatedVerifiedTerminal.setId(verifiedTerminal.getId());

        partialUpdatedVerifiedTerminal
            .userId(UPDATED_USER_ID)
            .terminalId(UPDATED_TERMINAL_ID)
            .identityKey(UPDATED_IDENTITY_KEY)
            .agreementKey(UPDATED_AGREEMENT_KEY)
            .signedPreKey(UPDATED_SIGNED_PRE_KEY)
            .signedPreKeySignature(UPDATED_SIGNED_PRE_KEY_SIGNATURE)
            .oneTimePreKeys(UPDATED_ONE_TIME_PRE_KEYS)
            .crossSignature(UPDATED_CROSS_SIGNATURE)
            .state(UPDATED_STATE)
            .archiveTerminal(UPDATED_ARCHIVE_TERMINAL)
            .registeredAt(UPDATED_REGISTERED_AT)
            .verifiedAt(UPDATED_VERIFIED_AT)
            .revokedAt(UPDATED_REVOKED_AT);

        restVerifiedTerminalMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedVerifiedTerminal.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedVerifiedTerminal))
            )
            .andExpect(status().isOk());

        // Validate the VerifiedTerminal in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertVerifiedTerminalUpdatableFieldsEquals(
            partialUpdatedVerifiedTerminal,
            getPersistedVerifiedTerminal(partialUpdatedVerifiedTerminal)
        );
    }

    @Test
    @Transactional
    void patchNonExistingVerifiedTerminal() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        verifiedTerminal.setId(UUID.randomUUID());

        // Create the VerifiedTerminal
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restVerifiedTerminalMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, verifiedTerminalDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the VerifiedTerminal in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchVerifiedTerminal() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        verifiedTerminal.setId(UUID.randomUUID());

        // Create the VerifiedTerminal
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVerifiedTerminalMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the VerifiedTerminal in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamVerifiedTerminal() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        verifiedTerminal.setId(UUID.randomUUID());

        // Create the VerifiedTerminal
        VerifiedTerminalDTO verifiedTerminalDTO = verifiedTerminalMapper.toDto(verifiedTerminal);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restVerifiedTerminalMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(verifiedTerminalDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the VerifiedTerminal in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteVerifiedTerminal() throws Exception {
        // Initialize the database
        insertedVerifiedTerminal = verifiedTerminalRepository.saveAndFlush(verifiedTerminal);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the verifiedTerminal
        restVerifiedTerminalMockMvc
            .perform(delete(ENTITY_API_URL_ID, verifiedTerminal.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return verifiedTerminalRepository.count();
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

    protected VerifiedTerminal getPersistedVerifiedTerminal(VerifiedTerminal verifiedTerminal) {
        return verifiedTerminalRepository.findById(verifiedTerminal.getId()).orElseThrow();
    }

    protected void assertPersistedVerifiedTerminalToMatchAllProperties(VerifiedTerminal expectedVerifiedTerminal) {
        assertVerifiedTerminalAllPropertiesEquals(expectedVerifiedTerminal, getPersistedVerifiedTerminal(expectedVerifiedTerminal));
    }

    protected void assertPersistedVerifiedTerminalToMatchUpdatableProperties(VerifiedTerminal expectedVerifiedTerminal) {
        assertVerifiedTerminalAllUpdatablePropertiesEquals(
            expectedVerifiedTerminal,
            getPersistedVerifiedTerminal(expectedVerifiedTerminal)
        );
    }
}
