package cm.gimac.des.iam.infrastructure.persistence;

import cm.gimac.des.iam.application.port.out.CatalogueUnites;
import cm.gimac.des.iam.domain.organisation.Unite;
import java.util.List;
import org.springframework.stereotype.Component;

/** Adaptateur JPA du catalogue des unités organisationnelles (table unite_organisationnelle). */
@Component
public class CatalogueUnitesJpa implements CatalogueUnites {

    private final UniteOrganisationnelleRepository depot;

    public CatalogueUnitesJpa(UniteOrganisationnelleRepository depot) {
        this.depot = depot;
    }

    @Override
    public List<Unite> toutes() {
        return depot.findAllByOrderByCodeAsc().stream().map(UniteOrganisationnelle::versDomaine).toList();
    }

    @Override
    public Unite enregistrer(Unite unite) {
        UniteOrganisationnelle e = depot.findById(unite.code()).orElseGet(() -> new UniteOrganisationnelle(unite.code()));
        e.appliquer(unite);
        return depot.save(e).versDomaine();
    }
}
