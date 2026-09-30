package cm.gimac.des.certification.web.rest;

import static cm.gimac.des.certification.domain.TimestampTokenAsserts.*;
import static cm.gimac.des.certification.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cm.gimac.des.certification.IntegrationTest;
import cm.gimac.des.certification.domain.TimestampToken;
import cm.gimac.des.certification.repository.TimestampTokenRepository;
import cm.gimac.des.certification.service.dto.TimestampTokenDTO;
import cm.gimac.des.certification.service.mapper.TimestampTokenMapper;
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
 * Integration tests for the {@link TimestampTokenResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = cm.gimac.des.certification.security.AuthoritiesConstants.ADMIN)
class TimestampTokenResourceIT {

    private static final String DEFAULT_OBJET_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_OBJET_TYPE = "BBBBBBBBBB";

    private static final UUID DEFAULT_OBJET_ID = UUID.randomUUID();
    private static final UUID UPDATED_OBJET_ID = UUID.randomUUID();

    private static final String DEFAULT_EMPREINTE = "AAAAAAAAAA";
    private static final String UPDATED_EMPREINTE = "BBBBBBBBBB";

    private static final String DEFAULT_NONCE = "AAAAAAAAAA";
    private static final String UPDATED_NONCE = "BBBBBBBBBB";

    private static final String DEFAULT_JETON = "AAAAAAAAAA";
    private static final String UPDATED_JETON = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_JETON = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_JETON = Instant.ofEpochMilli(1702411306964L);

    private static final Boolean DEFAULT_VERIFIE = false;
    private static final Boolean UPDATED_VERIFIE = true;

    private static final String DEFAULT_AUTORITE_CHAINE = "AAAAAAAAAA";
    private static final String UPDATED_AUTORITE_CHAINE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/timestamp-tokens";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TimestampTokenRepository timestampTokenRepository;

    @Autowired
    private TimestampTokenMapper timestampTokenMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTimestampTokenMockMvc;

    private TimestampToken timestampToken;

    private TimestampToken insertedTimestampToken;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TimestampToken createEntity() {
        return new TimestampToken()
            .objetType(DEFAULT_OBJET_TYPE)
            .objetId(DEFAULT_OBJET_ID)
            .empreinte(DEFAULT_EMPREINTE)
            .nonce(DEFAULT_NONCE)
            .jeton(DEFAULT_JETON)
            .dateJeton(DEFAULT_DATE_JETON)
            .verifie(DEFAULT_VERIFIE)
            .autoriteChaine(DEFAULT_AUTORITE_CHAINE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static TimestampToken createUpdatedEntity() {
        return new TimestampToken()
            .objetType(UPDATED_OBJET_TYPE)
            .objetId(UPDATED_OBJET_ID)
            .empreinte(UPDATED_EMPREINTE)
            .nonce(UPDATED_NONCE)
            .jeton(UPDATED_JETON)
            .dateJeton(UPDATED_DATE_JETON)
            .verifie(UPDATED_VERIFIE)
            .autoriteChaine(UPDATED_AUTORITE_CHAINE);
    }

    @BeforeEach
    void initTest() {
        timestampToken = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedTimestampToken != null) {
            timestampTokenRepository.delete(insertedTimestampToken);
            insertedTimestampToken = null;
        }
    }

    @Test
    @Transactional
    void createTimestampToken() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the TimestampToken
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);
        var returnedTimestampTokenDTO = om.readValue(
            restTimestampTokenMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(timestampTokenDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            TimestampTokenDTO.class
        );

        // Validate the TimestampToken in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedTimestampToken = timestampTokenMapper.toEntity(returnedTimestampTokenDTO);
        assertTimestampTokenUpdatableFieldsEquals(returnedTimestampToken, getPersistedTimestampToken(returnedTimestampToken));

        insertedTimestampToken = returnedTimestampToken;
    }

    @Test
    @Transactional
    void createTimestampTokenWithExistingId() throws Exception {
        // Create the TimestampToken with an existing ID
        insertedTimestampToken = timestampTokenRepository.saveAndFlush(timestampToken);
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restTimestampTokenMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TimestampToken in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkObjetTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        timestampToken.setObjetType(null);

        // Create the TimestampToken, which fails.
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        restTimestampTokenMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkObjetIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        timestampToken.setObjetId(null);

        // Create the TimestampToken, which fails.
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        restTimestampTokenMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEmpreinteIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        timestampToken.setEmpreinte(null);

        // Create the TimestampToken, which fails.
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        restTimestampTokenMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkNonceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        timestampToken.setNonce(null);

        // Create the TimestampToken, which fails.
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        restTimestampTokenMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateJetonIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        timestampToken.setDateJeton(null);

        // Create the TimestampToken, which fails.
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        restTimestampTokenMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkVerifieIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        timestampToken.setVerifie(null);

        // Create the TimestampToken, which fails.
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        restTimestampTokenMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllTimestampTokens() throws Exception {
        // Initialize the database
        insertedTimestampToken = timestampTokenRepository.saveAndFlush(timestampToken);

        // Get all the timestampTokenList
        restTimestampTokenMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(timestampToken.getId().toString())))
            .andExpect(jsonPath("$.[*].objetType").value(hasItem(DEFAULT_OBJET_TYPE)))
            .andExpect(jsonPath("$.[*].objetId").value(hasItem(DEFAULT_OBJET_ID.toString())))
            .andExpect(jsonPath("$.[*].empreinte").value(hasItem(DEFAULT_EMPREINTE)))
            .andExpect(jsonPath("$.[*].nonce").value(hasItem(DEFAULT_NONCE)))
            .andExpect(jsonPath("$.[*].jeton").value(hasItem(DEFAULT_JETON)))
            .andExpect(jsonPath("$.[*].dateJeton").value(hasItem(DEFAULT_DATE_JETON.toString())))
            .andExpect(jsonPath("$.[*].verifie").value(hasItem(DEFAULT_VERIFIE)))
            .andExpect(jsonPath("$.[*].autoriteChaine").value(hasItem(DEFAULT_AUTORITE_CHAINE)));
    }

    @Test
    @Transactional
    void getTimestampToken() throws Exception {
        // Initialize the database
        insertedTimestampToken = timestampTokenRepository.saveAndFlush(timestampToken);

        // Get the timestampToken
        restTimestampTokenMockMvc
            .perform(get(ENTITY_API_URL_ID, timestampToken.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(timestampToken.getId().toString()))
            .andExpect(jsonPath("$.objetType").value(DEFAULT_OBJET_TYPE))
            .andExpect(jsonPath("$.objetId").value(DEFAULT_OBJET_ID.toString()))
            .andExpect(jsonPath("$.empreinte").value(DEFAULT_EMPREINTE))
            .andExpect(jsonPath("$.nonce").value(DEFAULT_NONCE))
            .andExpect(jsonPath("$.jeton").value(DEFAULT_JETON))
            .andExpect(jsonPath("$.dateJeton").value(DEFAULT_DATE_JETON.toString()))
            .andExpect(jsonPath("$.verifie").value(DEFAULT_VERIFIE))
            .andExpect(jsonPath("$.autoriteChaine").value(DEFAULT_AUTORITE_CHAINE));
    }

    @Test
    @Transactional
    void getNonExistingTimestampToken() throws Exception {
        // Get the timestampToken
        restTimestampTokenMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingTimestampToken() throws Exception {
        // Initialize the database
        insertedTimestampToken = timestampTokenRepository.saveAndFlush(timestampToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the timestampToken
        TimestampToken updatedTimestampToken = timestampTokenRepository.findById(timestampToken.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedTimestampToken are not directly saved in db
        em.detach(updatedTimestampToken);
        updatedTimestampToken
            .objetType(UPDATED_OBJET_TYPE)
            .objetId(UPDATED_OBJET_ID)
            .empreinte(UPDATED_EMPREINTE)
            .nonce(UPDATED_NONCE)
            .jeton(UPDATED_JETON)
            .dateJeton(UPDATED_DATE_JETON)
            .verifie(UPDATED_VERIFIE)
            .autoriteChaine(UPDATED_AUTORITE_CHAINE);
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(updatedTimestampToken);

        restTimestampTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, timestampTokenDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isOk());

        // Validate the TimestampToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedTimestampTokenToMatchAllProperties(updatedTimestampToken);
    }

    @Test
    @Transactional
    void putNonExistingTimestampToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        timestampToken.setId(UUID.randomUUID());

        // Create the TimestampToken
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTimestampTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, timestampTokenDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TimestampToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchTimestampToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        timestampToken.setId(UUID.randomUUID());

        // Create the TimestampToken
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTimestampTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TimestampToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamTimestampToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        timestampToken.setId(UUID.randomUUID());

        // Create the TimestampToken
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTimestampTokenMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the TimestampToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateTimestampTokenWithPatch() throws Exception {
        // Initialize the database
        insertedTimestampToken = timestampTokenRepository.saveAndFlush(timestampToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the timestampToken using partial update
        TimestampToken partialUpdatedTimestampToken = new TimestampToken();
        partialUpdatedTimestampToken.setId(timestampToken.getId());

        partialUpdatedTimestampToken
            .objetType(UPDATED_OBJET_TYPE)
            .objetId(UPDATED_OBJET_ID)
            .empreinte(UPDATED_EMPREINTE)
            .nonce(UPDATED_NONCE)
            .jeton(UPDATED_JETON)
            .dateJeton(UPDATED_DATE_JETON)
            .verifie(UPDATED_VERIFIE)
            .autoriteChaine(UPDATED_AUTORITE_CHAINE);

        restTimestampTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTimestampToken.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTimestampToken))
            )
            .andExpect(status().isOk());

        // Validate the TimestampToken in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTimestampTokenUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedTimestampToken, timestampToken),
            getPersistedTimestampToken(timestampToken)
        );
    }

    @Test
    @Transactional
    void fullUpdateTimestampTokenWithPatch() throws Exception {
        // Initialize the database
        insertedTimestampToken = timestampTokenRepository.saveAndFlush(timestampToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the timestampToken using partial update
        TimestampToken partialUpdatedTimestampToken = new TimestampToken();
        partialUpdatedTimestampToken.setId(timestampToken.getId());

        partialUpdatedTimestampToken
            .objetType(UPDATED_OBJET_TYPE)
            .objetId(UPDATED_OBJET_ID)
            .empreinte(UPDATED_EMPREINTE)
            .nonce(UPDATED_NONCE)
            .jeton(UPDATED_JETON)
            .dateJeton(UPDATED_DATE_JETON)
            .verifie(UPDATED_VERIFIE)
            .autoriteChaine(UPDATED_AUTORITE_CHAINE);

        restTimestampTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedTimestampToken.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedTimestampToken))
            )
            .andExpect(status().isOk());

        // Validate the TimestampToken in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertTimestampTokenUpdatableFieldsEquals(partialUpdatedTimestampToken, getPersistedTimestampToken(partialUpdatedTimestampToken));
    }

    @Test
    @Transactional
    void patchNonExistingTimestampToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        timestampToken.setId(UUID.randomUUID());

        // Create the TimestampToken
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTimestampTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, timestampTokenDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TimestampToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchTimestampToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        timestampToken.setId(UUID.randomUUID());

        // Create the TimestampToken
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTimestampTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the TimestampToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamTimestampToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        timestampToken.setId(UUID.randomUUID());

        // Create the TimestampToken
        TimestampTokenDTO timestampTokenDTO = timestampTokenMapper.toDto(timestampToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restTimestampTokenMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(timestampTokenDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the TimestampToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteTimestampToken() throws Exception {
        // Initialize the database
        insertedTimestampToken = timestampTokenRepository.saveAndFlush(timestampToken);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the timestampToken
        restTimestampTokenMockMvc
            .perform(delete(ENTITY_API_URL_ID, timestampToken.getId().toString()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return timestampTokenRepository.count();
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

    protected TimestampToken getPersistedTimestampToken(TimestampToken timestampToken) {
        return timestampTokenRepository.findById(timestampToken.getId()).orElseThrow();
    }

    protected void assertPersistedTimestampTokenToMatchAllProperties(TimestampToken expectedTimestampToken) {
        assertTimestampTokenAllPropertiesEquals(expectedTimestampToken, getPersistedTimestampToken(expectedTimestampToken));
    }

    protected void assertPersistedTimestampTokenToMatchUpdatableProperties(TimestampToken expectedTimestampToken) {
        assertTimestampTokenAllUpdatablePropertiesEquals(expectedTimestampToken, getPersistedTimestampToken(expectedTimestampToken));
    }
}
