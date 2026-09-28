package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.CockpitView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface CockpitViewRepositoryWithBagRelationships {
    Optional<CockpitView> fetchBagRelationships(Optional<CockpitView> cockpitView);

    List<CockpitView> fetchBagRelationships(List<CockpitView> cockpitViews);

    Page<CockpitView> fetchBagRelationships(Page<CockpitView> cockpitViews);
}
