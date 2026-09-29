package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.event.CatalogPublishedPayload;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publication d'une version du référentiel (HOM-01 A4) : verrou, complétude, contrôle du graphe
 * (HOM-02), retrait de la version publiée précédente, numérotation, événement par l'outbox dans
 * la même transaction.
 */
@Service
@Transactional
public class ReferentialLifecycleService implements ManageReferentialUseCase {

    static final String AGREGAT = "catalog";

    private final ReferentialVersionRepository versions;
    private final ReferentialCompletenessChecker completeness;
    private final ReferentialGraphValidator graphValidator;
    private final OutboxPublisher outboxPublisher;
    private final Clock clock;

    public ReferentialLifecycleService(
        ReferentialVersionRepository versions,
        ReferentialCompletenessChecker completeness,
        ReferentialGraphValidator graphValidator,
        OutboxPublisher outboxPublisher,
        Clock clock
    ) {
        this.versions = versions;
        this.completeness = completeness;
        this.graphValidator = graphValidator;
        this.outboxPublisher = outboxPublisher;
        this.clock = clock;
    }

    @Override
    public ReferentialVersion creerBrouillon(TypeProjet type, String libelle, UUID versionSourceId) {
        Instant now = clock.instant();
        ReferentialVersion brouillon = versionSourceId == null
            ? ReferentialVersion.nouveauBrouillon(type, libelle, now)
            : ReferentialVersion.deriverDe(charger(versionSourceId), libelle, now);
        return versions.save(brouillon);
    }

    @Override
    public ReferenceOperation enregistrerOperation(UUID versionId, String code, ReferenceOperationData data) {
        return versions
            .findByIdForUpdate(versionId)
            .orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", versionId))
            .upsertOperation(code, data);
    }

    @Override
    public ReferentialVersion publier(UUID versionId) {
        ReferentialVersion v = versions
            .findByIdForUpdate(versionId)
            .orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", versionId));
        v.assertModifiable();
        var manquants = completeness.check(v.getOperations());
        if (!manquants.isEmpty()) {
            throw HomologationException.conflict("HOM_REFERENTIAL_INCOMPLETE", "Publication refusée : opérations incomplètes", Map.of("operations", manquants));
        }
        List<String> ordre = graphValidator.validate(v.noeuds()); // lève HOM_REFERENTIAL_GRAPH_INVALID
        versions.findFirstByTypeProjetAndEtat(v.getTypeProjet(), EtatVersionReferentiel.PUBLIEE).ifPresent(ReferentialVersion::retirer);
        int numero = versions.maxNumero(v.getTypeProjet()).orElse(0) + 1;
        v.publier(numero, clock.instant(), ordre);
        outboxPublisher.publish(
            AGREGAT,
            v.getId().toString(),
            CatalogPublishedPayload.TYPE,
            new CatalogPublishedPayload(v.getId(), v.getTypeProjet(), numero, v.getDatePublication(), v.getOperations().size(), v.getVersionSourceId())
        );
        return v;
    }

    private ReferentialVersion charger(UUID id) {
        return versions.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", id));
    }
}
