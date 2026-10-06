package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.GererEtapesReferentielUseCase;
import cm.gimac.des.homologation.domain.EtapeReferentiel;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.parcours.CouleurEtape;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.repository.EtapeReferentielRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Étapes configurables (D-89, §2) : la version est verrouillée en écriture (écritures concurrentes sérialisées),
 * les règles sont tenues par {@link ReferentialVersion} ; une étape retirée est supprimée explicitement.
 */
@Service
@Transactional
public class ReferentialEtapesService implements GererEtapesReferentielUseCase {

    private final ReferentialVersionRepository versions;
    private final EtapeReferentielRepository etapes;

    public ReferentialEtapesService(ReferentialVersionRepository versions, EtapeReferentielRepository etapes) {
        this.versions = versions;
        this.etapes = etapes;
    }

    @Override
    public EtapeReferentiel ajouter(UUID versionId, String code, String libelle, CouleurEtape couleur, EtapeCode canonique, Integer ordre) {
        return verrouiller(versionId).ajouterEtape(code, libelle, couleur, canonique, ordre);
    }

    @Override
    public EtapeReferentiel modifier(UUID versionId, String code, String libelle, CouleurEtape couleur, EtapeCode canonique) {
        return verrouiller(versionId).modifierEtape(code, libelle, couleur, canonique);
    }

    @Override
    public void supprimer(UUID versionId, String code) {
        EtapeReferentiel e = verrouiller(versionId).retirerEtape(code);
        if (e.getId() != null) {
            etapes.delete(e);
        }
    }

    @Override
    public ReferentialVersion ordonner(UUID versionId, List<String> codes) {
        ReferentialVersion v = verrouiller(versionId);
        v.ordonnerEtapes(codes);
        return v;
    }

    private ReferentialVersion verrouiller(UUID versionId) {
        return versions.findByIdForUpdate(versionId).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", versionId));
    }
}
