package cm.gimac.des.gateway.web.rest;

import static cm.gimac.des.gateway.domain.RouteAsserts.*;
import static cm.gimac.des.gateway.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import cm.gimac.des.gateway.IntegrationTest;
import cm.gimac.des.gateway.domain.PresentationLayer;
import cm.gimac.des.gateway.domain.Route;
import cm.gimac.des.gateway.repository.RouteRepository;
import cm.gimac.des.gateway.service.RouteService;
import cm.gimac.des.gateway.service.dto.RouteDTO;
import cm.gimac.des.gateway.service.mapper.RouteMapper;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link RouteResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class RouteResourceIT {

    private static final String DEFAULT_PATH = "AAAAAAAAAA";
    private static final String UPDATED_PATH = "BBBBBBBBBB";

    private static final String DEFAULT_TARGET_SERVICE = "AAAAAAAAAA";
    private static final String UPDATED_TARGET_SERVICE = "BBBBBBBBBB";

    private static final String DEFAULT_REQUIRED_PERMISSIONS = "AAAAAAAAAA";
    private static final String UPDATED_REQUIRED_PERMISSIONS = "BBBBBBBBBB";

    private static final Integer DEFAULT_RATE_LIMIT_PER_MINUTE = 1;
    private static final Integer UPDATED_RATE_LIMIT_PER_MINUTE = 2;

    private static final Boolean DEFAULT_INSPECT_BODY = false;
    private static final Boolean UPDATED_INSPECT_BODY = true;

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/routes";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RouteRepository routeRepository;

    @Mock
    private RouteRepository routeRepositoryMock;

    @Autowired
    private RouteMapper routeMapper;

    @Mock
    private RouteService routeServiceMock;

    @Autowired
    private R2dbcEntityTemplate template;

    @Autowired
    private WebTestClient webTestClient;

    private Route route;

    private Route insertedRoute;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Route createEntity(R2dbcEntityTemplate template) {
        Route route = new Route()
            .path(DEFAULT_PATH)
            .targetService(DEFAULT_TARGET_SERVICE)
            .requiredPermissions(DEFAULT_REQUIRED_PERMISSIONS)
            .rateLimitPerMinute(DEFAULT_RATE_LIMIT_PER_MINUTE)
            .inspectBody(DEFAULT_INSPECT_BODY)
            .active(DEFAULT_ACTIVE);
        // Add required entity
        PresentationLayer presentationLayer;
        presentationLayer = template.insert(PresentationLayerResourceIT.createEntity()).block();
        route.setLayer(presentationLayer);
        return route;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Route createUpdatedEntity(R2dbcEntityTemplate template) {
        Route updatedRoute = new Route()
            .path(UPDATED_PATH)
            .targetService(UPDATED_TARGET_SERVICE)
            .requiredPermissions(UPDATED_REQUIRED_PERMISSIONS)
            .rateLimitPerMinute(UPDATED_RATE_LIMIT_PER_MINUTE)
            .inspectBody(UPDATED_INSPECT_BODY)
            .active(UPDATED_ACTIVE);
        // Add required entity
        PresentationLayer presentationLayer;
        presentationLayer = template.insert(PresentationLayerResourceIT.createUpdatedEntity()).block();
        updatedRoute.setLayer(presentationLayer);
        return updatedRoute;
    }

    public static void deleteEntities(R2dbcEntityTemplate template) {
        try {
            template.delete(Route.class).all().block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
        PresentationLayerResourceIT.deleteEntities(template);
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        route = createEntity(template);
    }

    @AfterEach
    void cleanup() {
        if (insertedRoute != null) {
            routeRepository.delete(insertedRoute).block();
            insertedRoute = null;
        }
        deleteEntities(template);
    }

    @Test
    void createRoute() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);
        var returnedRouteDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(RouteDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Route in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoute = routeMapper.toEntity(returnedRouteDTO);
        assertRouteUpdatableFieldsEquals(returnedRoute, getPersistedRoute(returnedRoute));

        insertedRoute = returnedRoute;
    }

    @Test
    void createRouteWithExistingId() throws Exception {
        // Create the Route with an existing ID
        route.setId(1L);
        RouteDTO routeDTO = routeMapper.toDto(route);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkPathIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        route.setPath(null);

        // Create the Route, which fails.
        RouteDTO routeDTO = routeMapper.toDto(route);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkTargetServiceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        route.setTargetService(null);

        // Create the Route, which fails.
        RouteDTO routeDTO = routeMapper.toDto(route);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkRequiredPermissionsIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        route.setRequiredPermissions(null);

        // Create the Route, which fails.
        RouteDTO routeDTO = routeMapper.toDto(route);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkInspectBodyIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        route.setInspectBody(null);

        // Create the Route, which fails.
        RouteDTO routeDTO = routeMapper.toDto(route);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        route.setActive(null);

        // Create the Route, which fails.
        RouteDTO routeDTO = routeMapper.toDto(route);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllRoutes() {
        // Initialize the database
        insertedRoute = routeRepository.save(route).block();

        // Get all the routeList
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
            .value(hasItem(route.getId().intValue()))
            .jsonPath("$.[*].path")
            .value(hasItem(DEFAULT_PATH))
            .jsonPath("$.[*].targetService")
            .value(hasItem(DEFAULT_TARGET_SERVICE))
            .jsonPath("$.[*].requiredPermissions")
            .value(hasItem(DEFAULT_REQUIRED_PERMISSIONS))
            .jsonPath("$.[*].rateLimitPerMinute")
            .value(hasItem(DEFAULT_RATE_LIMIT_PER_MINUTE))
            .jsonPath("$.[*].inspectBody")
            .value(hasItem(DEFAULT_INSPECT_BODY))
            .jsonPath("$.[*].active")
            .value(hasItem(DEFAULT_ACTIVE));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoutesWithEagerRelationshipsIsEnabled() {
        when(routeServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?eagerload=true")
            .exchange()
            .expectStatus()
            .isOk();

        verify(routeServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoutesWithEagerRelationshipsIsNotEnabled() {
        when(routeServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?eagerload=false")
            .exchange()
            .expectStatus()
            .isOk();
        verify(routeRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getRoute() {
        // Initialize the database
        insertedRoute = routeRepository.save(route).block();

        // Get the route
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, route.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(route.getId().intValue()))
            .jsonPath("$.path")
            .value(is(DEFAULT_PATH))
            .jsonPath("$.targetService")
            .value(is(DEFAULT_TARGET_SERVICE))
            .jsonPath("$.requiredPermissions")
            .value(is(DEFAULT_REQUIRED_PERMISSIONS))
            .jsonPath("$.rateLimitPerMinute")
            .value(is(DEFAULT_RATE_LIMIT_PER_MINUTE))
            .jsonPath("$.inspectBody")
            .value(is(DEFAULT_INSPECT_BODY))
            .jsonPath("$.active")
            .value(is(DEFAULT_ACTIVE));
    }

    @Test
    void getNonExistingRoute() {
        // Get the route
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingRoute() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.save(route).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the route
        Route updatedRoute = routeRepository.findById(route.getId()).block();
        updatedRoute
            .path(UPDATED_PATH)
            .targetService(UPDATED_TARGET_SERVICE)
            .requiredPermissions(UPDATED_REQUIRED_PERMISSIONS)
            .rateLimitPerMinute(UPDATED_RATE_LIMIT_PER_MINUTE)
            .inspectBody(UPDATED_INSPECT_BODY)
            .active(UPDATED_ACTIVE);
        RouteDTO routeDTO = routeMapper.toDto(updatedRoute);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, routeDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRouteToMatchAllProperties(updatedRoute);
    }

    @Test
    void putNonExistingRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, routeDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateRouteWithPatch() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.save(route).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the route using partial update
        Route partialUpdatedRoute = new Route();
        partialUpdatedRoute.setId(route.getId());

        partialUpdatedRoute.path(UPDATED_PATH).requiredPermissions(UPDATED_REQUIRED_PERMISSIONS);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedRoute.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedRoute))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Route in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRouteUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedRoute, route), getPersistedRoute(route));
    }

    @Test
    void fullUpdateRouteWithPatch() throws Exception {
        // Initialize the database
        insertedRoute = routeRepository.save(route).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the route using partial update
        Route partialUpdatedRoute = new Route();
        partialUpdatedRoute.setId(route.getId());

        partialUpdatedRoute
            .path(UPDATED_PATH)
            .targetService(UPDATED_TARGET_SERVICE)
            .requiredPermissions(UPDATED_REQUIRED_PERMISSIONS)
            .rateLimitPerMinute(UPDATED_RATE_LIMIT_PER_MINUTE)
            .inspectBody(UPDATED_INSPECT_BODY)
            .active(UPDATED_ACTIVE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedRoute.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedRoute))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Route in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRouteUpdatableFieldsEquals(partialUpdatedRoute, getPersistedRoute(partialUpdatedRoute));
    }

    @Test
    void patchNonExistingRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, routeDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamRoute() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        route.setId(longCount.incrementAndGet());

        // Create the Route
        RouteDTO routeDTO = routeMapper.toDto(route);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(routeDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Route in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteRoute() {
        // Initialize the database
        insertedRoute = routeRepository.save(route).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the route
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, route.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return routeRepository.count().block();
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

    protected Route getPersistedRoute(Route route) {
        return routeRepository.findById(route.getId()).block();
    }

    protected void assertPersistedRouteToMatchAllProperties(Route expectedRoute) {
        // Test fails because reactive api returns an empty object instead of null
        // assertRouteAllPropertiesEquals(expectedRoute, getPersistedRoute(expectedRoute));
        assertRouteUpdatableFieldsEquals(expectedRoute, getPersistedRoute(expectedRoute));
    }

    protected void assertPersistedRouteToMatchUpdatableProperties(Route expectedRoute) {
        // Test fails because reactive api returns an empty object instead of null
        // assertRouteAllUpdatablePropertiesEquals(expectedRoute, getPersistedRoute(expectedRoute));
        assertRouteUpdatableFieldsEquals(expectedRoute, getPersistedRoute(expectedRoute));
    }
}
