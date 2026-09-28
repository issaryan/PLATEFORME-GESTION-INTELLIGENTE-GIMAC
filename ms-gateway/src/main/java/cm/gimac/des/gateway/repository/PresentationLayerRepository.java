package cm.gimac.des.gateway.repository;

import cm.gimac.des.gateway.domain.PresentationLayer;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data R2DBC repository for the PresentationLayer entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PresentationLayerRepository extends R2dbcRepository<PresentationLayer, Long> {}
