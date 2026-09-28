package cm.gimac.des.gateway.web.rest;

import static cm.gimac.des.gateway.domain.PresentationLayerAsserts.*;
import static cm.gimac.des.gateway.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import cm.gimac.des.gateway.IntegrationTest;
import cm.gimac.des.gateway.domain.PresentationLayer;
import cm.gimac.des.gateway.domain.enumeration.NetworkZone;
import cm.gimac.des.gateway.repository.PresentationLayerRepository;
import cm.gimac.des.gateway.service.dto.PresentationLayerDTO;
import cm.gimac.des.gateway.service.mapper.PresentationLayerMapper;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link PresentationLayerResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class PresentationLayerResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final NetworkZone DEFAULT_ZONE = NetworkZone.DEPARTEMENT;
    private static final NetworkZone UPDATED_ZONE = NetworkZone.DMZ;

    private static final String DEFAULT_AUTHENTICATION_REQUIREMENT = "AAAAAAAAAA";
    private static final String UPDATED_AUTHENTICATION_REQUIREMENT = "BBBBBBBBBB";

    private static final String DEFAULT_BFF_PREFIX = "/kw3rs";
    private static final String UPDATED_BFF_PREFIX = "/s7pl";

    private static final Integer DEFAULT_DEFAULT_RATE_LIMIT_PER_MINUTE = 1;
    private static final Integer UPDATED_DEFAULT_RATE_LIMIT_PER_MINUTE = 2;

    private static final String ENTITY_API_URL = "/api/presentation-layers";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PresentationLayerRepository presentationLayerRepository;

    @Autowired
    private PresentationLayerMapper presentationLayerMapper;

    @Autowired
    private R2dbcEntityTemplate template;

    @Autowired
    private WebTestClient webTestClient;

    private PresentationLayer presentationLayer;

    private PresentationLayer insertedPresentationLayer;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PresentationLayer createEntity() {
        return new PresentationLayer()
            .name(DEFAULT_NAME)
            .zone(DEFAULT_ZONE)
            .authenticationRequirement(DEFAULT_AUTHENTICATION_REQUIREMENT)
            .bffPrefix(DEFAULT_BFF_PREFIX)
            .defaultRateLimitPerMinute(DEFAULT_DEFAULT_RATE_LIMIT_PER_MINUTE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PresentationLayer createUpdatedEntity() {
        return new PresentationLayer()
            .name(UPDATED_NAME)
            .zone(UPDATED_ZONE)
            .authenticationRequirement(UPDATED_AUTHENTICATION_REQUIREMENT)
            .bffPrefix(UPDATED_BFF_PREFIX)
            .defaultRateLimitPerMinute(UPDATED_DEFAULT_RATE_LIMIT_PER_MINUTE);
    }

    public static void deleteEntities(R2dbcEntityTemplate template) {
        try {
            template.delete(PresentationLayer.class).all().block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        presentationLayer = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedPresentationLayer != null) {
            presentationLayerRepository.delete(insertedPresentationLayer).block();
            insertedPresentationLayer = null;
        }
        deleteEntities(template);
    }

    @Test
    void createPresentationLayer() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the PresentationLayer
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);
        var returnedPresentationLayerDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(PresentationLayerDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the PresentationLayer in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedPresentationLayer = presentationLayerMapper.toEntity(returnedPresentationLayerDTO);
        assertPresentationLayerUpdatableFieldsEquals(returnedPresentationLayer, getPersistedPresentationLayer(returnedPresentationLayer));

        insertedPresentationLayer = returnedPresentationLayer;
    }

    @Test
    void createPresentationLayerWithExistingId() throws Exception {
        // Create the PresentationLayer with an existing ID
        presentationLayer.setId(1L);
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the PresentationLayer in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        presentationLayer.setName(null);

        // Create the PresentationLayer, which fails.
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkZoneIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        presentationLayer.setZone(null);

        // Create the PresentationLayer, which fails.
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkAuthenticationRequirementIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        presentationLayer.setAuthenticationRequirement(null);

        // Create the PresentationLayer, which fails.
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkBffPrefixIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        presentationLayer.setBffPrefix(null);

        // Create the PresentationLayer, which fails.
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkDefaultRateLimitPerMinuteIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        presentationLayer.setDefaultRateLimitPerMinute(null);

        // Create the PresentationLayer, which fails.
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllPresentationLayersAsStream() {
        // Initialize the database
        presentationLayerRepository.save(presentationLayer).block();

        List<PresentationLayer> presentationLayerList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(PresentationLayerDTO.class)
            .getResponseBody()
            .map(presentationLayerMapper::toEntity)
            .filter(presentationLayer::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(presentationLayerList).isNotNull();
        assertThat(presentationLayerList).hasSize(1);
        PresentationLayer testPresentationLayer = presentationLayerList.getFirst();

        // Test fails because reactive api returns an empty object instead of null
        // assertPresentationLayerAllPropertiesEquals(presentationLayer, testPresentationLayer);
        assertPresentationLayerUpdatableFieldsEquals(presentationLayer, testPresentationLayer);
    }

    @Test
    void getAllPresentationLayers() {
        // Initialize the database
        insertedPresentationLayer = presentationLayerRepository.save(presentationLayer).block();

        // Get all the presentationLayerList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(presentationLayer.getId().intValue()))
            .jsonPath("$.[*].name")
            .value(hasItem(DEFAULT_NAME))
            .jsonPath("$.[*].zone")
            .value(hasItem(DEFAULT_ZONE.toString()))
            .jsonPath("$.[*].authenticationRequirement")
            .value(hasItem(DEFAULT_AUTHENTICATION_REQUIREMENT))
            .jsonPath("$.[*].bffPrefix")
            .value(hasItem(DEFAULT_BFF_PREFIX))
            .jsonPath("$.[*].defaultRateLimitPerMinute")
            .value(hasItem(DEFAULT_DEFAULT_RATE_LIMIT_PER_MINUTE));
    }

    @Test
    void getPresentationLayer() {
        // Initialize the database
        insertedPresentationLayer = presentationLayerRepository.save(presentationLayer).block();

        // Get the presentationLayer
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, presentationLayer.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(presentationLayer.getId().intValue()))
            .jsonPath("$.name")
            .value(is(DEFAULT_NAME))
            .jsonPath("$.zone")
            .value(is(DEFAULT_ZONE.toString()))
            .jsonPath("$.authenticationRequirement")
            .value(is(DEFAULT_AUTHENTICATION_REQUIREMENT))
            .jsonPath("$.bffPrefix")
            .value(is(DEFAULT_BFF_PREFIX))
            .jsonPath("$.defaultRateLimitPerMinute")
            .value(is(DEFAULT_DEFAULT_RATE_LIMIT_PER_MINUTE));
    }

    @Test
    void getNonExistingPresentationLayer() {
        // Get the presentationLayer
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingPresentationLayer() throws Exception {
        // Initialize the database
        insertedPresentationLayer = presentationLayerRepository.save(presentationLayer).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the presentationLayer
        PresentationLayer updatedPresentationLayer = presentationLayerRepository.findById(presentationLayer.getId()).block();
        updatedPresentationLayer
            .name(UPDATED_NAME)
            .zone(UPDATED_ZONE)
            .authenticationRequirement(UPDATED_AUTHENTICATION_REQUIREMENT)
            .bffPrefix(UPDATED_BFF_PREFIX)
            .defaultRateLimitPerMinute(UPDATED_DEFAULT_RATE_LIMIT_PER_MINUTE);
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(updatedPresentationLayer);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, presentationLayerDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the PresentationLayer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPresentationLayerToMatchAllProperties(updatedPresentationLayer);
    }

    @Test
    void putNonExistingPresentationLayer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        presentationLayer.setId(longCount.incrementAndGet());

        // Create the PresentationLayer
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, presentationLayerDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the PresentationLayer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchPresentationLayer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        presentationLayer.setId(longCount.incrementAndGet());

        // Create the PresentationLayer
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the PresentationLayer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamPresentationLayer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        presentationLayer.setId(longCount.incrementAndGet());

        // Create the PresentationLayer
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the PresentationLayer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdatePresentationLayerWithPatch() throws Exception {
        // Initialize the database
        insertedPresentationLayer = presentationLayerRepository.save(presentationLayer).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the presentationLayer using partial update
        PresentationLayer partialUpdatedPresentationLayer = new PresentationLayer();
        partialUpdatedPresentationLayer.setId(presentationLayer.getId());

        partialUpdatedPresentationLayer
            .name(UPDATED_NAME)
            .authenticationRequirement(UPDATED_AUTHENTICATION_REQUIREMENT)
            .bffPrefix(UPDATED_BFF_PREFIX)
            .defaultRateLimitPerMinute(UPDATED_DEFAULT_RATE_LIMIT_PER_MINUTE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPresentationLayer.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedPresentationLayer))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the PresentationLayer in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPresentationLayerUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedPresentationLayer, presentationLayer),
            getPersistedPresentationLayer(presentationLayer)
        );
    }

    @Test
    void fullUpdatePresentationLayerWithPatch() throws Exception {
        // Initialize the database
        insertedPresentationLayer = presentationLayerRepository.save(presentationLayer).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the presentationLayer using partial update
        PresentationLayer partialUpdatedPresentationLayer = new PresentationLayer();
        partialUpdatedPresentationLayer.setId(presentationLayer.getId());

        partialUpdatedPresentationLayer
            .name(UPDATED_NAME)
            .zone(UPDATED_ZONE)
            .authenticationRequirement(UPDATED_AUTHENTICATION_REQUIREMENT)
            .bffPrefix(UPDATED_BFF_PREFIX)
            .defaultRateLimitPerMinute(UPDATED_DEFAULT_RATE_LIMIT_PER_MINUTE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPresentationLayer.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedPresentationLayer))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the PresentationLayer in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPresentationLayerUpdatableFieldsEquals(
            partialUpdatedPresentationLayer,
            getPersistedPresentationLayer(partialUpdatedPresentationLayer)
        );
    }

    @Test
    void patchNonExistingPresentationLayer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        presentationLayer.setId(longCount.incrementAndGet());

        // Create the PresentationLayer
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, presentationLayerDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the PresentationLayer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchPresentationLayer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        presentationLayer.setId(longCount.incrementAndGet());

        // Create the PresentationLayer
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the PresentationLayer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamPresentationLayer() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        presentationLayer.setId(longCount.incrementAndGet());

        // Create the PresentationLayer
        PresentationLayerDTO presentationLayerDTO = presentationLayerMapper.toDto(presentationLayer);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(presentationLayerDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the PresentationLayer in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deletePresentationLayer() {
        // Initialize the database
        insertedPresentationLayer = presentationLayerRepository.save(presentationLayer).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the presentationLayer
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, presentationLayer.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return presentationLayerRepository.count().block();
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

    protected PresentationLayer getPersistedPresentationLayer(PresentationLayer presentationLayer) {
        return presentationLayerRepository.findById(presentationLayer.getId()).block();
    }

    protected void assertPersistedPresentationLayerToMatchAllProperties(PresentationLayer expectedPresentationLayer) {
        // Test fails because reactive api returns an empty object instead of null
        // assertPresentationLayerAllPropertiesEquals(expectedPresentationLayer, getPersistedPresentationLayer(expectedPresentationLayer));
        assertPresentationLayerUpdatableFieldsEquals(expectedPresentationLayer, getPersistedPresentationLayer(expectedPresentationLayer));
    }

    protected void assertPersistedPresentationLayerToMatchUpdatableProperties(PresentationLayer expectedPresentationLayer) {
        // Test fails because reactive api returns an empty object instead of null
        // assertPresentationLayerAllUpdatablePropertiesEquals(expectedPresentationLayer, getPersistedPresentationLayer(expectedPresentationLayer));
        assertPresentationLayerUpdatableFieldsEquals(expectedPresentationLayer, getPersistedPresentationLayer(expectedPresentationLayer));
    }
}
