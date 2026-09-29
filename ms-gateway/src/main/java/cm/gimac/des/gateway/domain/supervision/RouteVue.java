package cm.gimac.des.gateway.domain.supervision;

import java.util.List;

/** Route effective de la passerelle : identifiant, prédicats, destination, filtres et ordre. */
public record RouteVue(String id, String uri, String predicats, List<String> filtres, int ordre) {
    public RouteVue {
        filtres = List.copyOf(filtres);
    }
}
