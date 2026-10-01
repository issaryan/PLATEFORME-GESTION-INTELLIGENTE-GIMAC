package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.JsonTexte;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialMigration;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.EtatMigration;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.repository.ReferentialMigrationRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migration des liaisons vers la version en vigueur du référentiel (HOM-03) : simulation par liaison
 * (opérations conservées, ajoutées, retirées, modifiées ; liaison close exclue), puis rattachement des
 * liaisons retenues à la version cible, tracé (migration APPROUVEE, événement par l'outbox). Le parcours
 * des étapes 3 à 10 (D-57) n'est pas réinstancié : il suit le référentiel de l'annexe A.
 */
@Service
@Transactional
public class ReferentialMigrationService {

    public static final String EVENEMENT = "homologation.referential-migrated.v1";

    public record Impact(UUID liaisonId, String institutionNom, int conservees, int ajoutees, int retirees, int modifiees, boolean exclue, String motifExclusion) {}

    private final ReferentialVersionRepository versions;
    private final LiaisonRepository liaisons;
    private final ReferentialMigrationRepository migrations;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public ReferentialMigrationService(
        ReferentialVersionRepository versions,
        LiaisonRepository liaisons,
        ReferentialMigrationRepository migrations,
        OutboxPublisher outbox,
        Clock clock
    ) {
        this.versions = versions;
        this.liaisons = liaisons;
        this.migrations = migrations;
        this.outbox = outbox;
        this.clock = clock;
    }

    /** Simulation sans effet sur les liaisons : liaisons du même type rattachées à une autre version. */
    @Transactional(readOnly = true)
    public List<Impact> simuler(UUID cibleId) {
        ReferentialVersion cible = cible(cibleId);
        return candidates(cible).stream().map(l -> impact(l, cible)).toList();
    }

    /** Rattache les liaisons retenues à la version cible ; toute liaison exclue ou étrangère est refusée. */
    public ReferentialMigration migrer(UUID cibleId, List<UUID> retenues, UUID decideurId) {
        ReferentialVersion cible = cible(cibleId);
        if (retenues == null || retenues.isEmpty()) {
            throw HomologationException.unprocessable("HOM_MIGRATION_EMPTY", "Aucune liaison retenue");
        }
        Map<UUID, Liaison> parId = candidates(cible).stream().collect(Collectors.toMap(Liaison::getId, Function.identity()));
        Set<UUID> ids = new LinkedHashSet<>(retenues);
        List<Impact> rapport = new ArrayList<>();
        for (UUID id : ids) {
            Liaison l = parId.get(id);
            if (l == null) {
                throw HomologationException.conflict("HOM_MIGRATION_LIAISON_INELIGIBLE", "Liaison non concernée par cette version", Map.of("liaisonId", String.valueOf(id)));
            }
            Impact i = impact(l, cible);
            if (i.exclue()) {
                throw HomologationException.conflict("HOM_MIGRATION_LIAISON_EXCLUDED", i.motifExclusion(), Map.of("liaisonId", String.valueOf(id)));
            }
            rapport.add(i);
        }
        UUID source = parId.get(ids.iterator().next()).getVersion().getId();
        ids.forEach(id -> parId.get(id).setVersion(cible));
        ReferentialMigration m = new ReferentialMigration()
            .versionSourceId(source)
            .versionCibleId(cible.getId())
            .etat(EtatMigration.APPROUVEE)
            .rapport(JsonTexte.ecrire(rapport))
            .dateDemande(clock.instant())
            .decideurId(decideurId)
            .liaisonsRetenues(JsonTexte.ecrire(ids.stream().map(UUID::toString).toList()));
        m = migrations.save(m);
        outbox.publish(
            "referential",
            cible.getId().toString(),
            EVENEMENT,
            Map.of("versionCibleId", cible.getId().toString(), "liaisons", ids.stream().map(UUID::toString).toList(), "migrationId", m.getId().toString())
        );
        return m;
    }

    private ReferentialVersion cible(UUID id) {
        ReferentialVersion v = versions.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", id));
        if (v.getEtat() != EtatVersionReferentiel.PUBLIEE) {
            throw HomologationException.conflict("HOM_MIGRATION_TARGET_NOT_PUBLISHED", "Seule la version en vigueur peut recevoir des liaisons", Map.of("versionId", String.valueOf(id)));
        }
        return v;
    }

    private List<Liaison> candidates(ReferentialVersion cible) {
        return liaisons
            .findAll()
            .stream()
            .filter(l -> l.getTypeProjet() == cible.getTypeProjet() && l.getVersion() != null && !cible.getId().equals(l.getVersion().getId()))
            .sorted(Comparator.comparing(Liaison::getDateOuverture, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    static Impact impact(Liaison l, ReferentialVersion cible) {
        Map<String, ReferenceOperation> avant = parCode(l.getVersion().getOperations());
        Map<String, ReferenceOperation> apres = parCode(cible.getOperations());
        int conservees = 0;
        int modifiees = 0;
        for (var e : apres.entrySet()) {
            ReferenceOperation a = avant.get(e.getKey());
            if (a == null) continue;
            if (identiques(a, e.getValue())) conservees++;
            else modifiees++;
        }
        int ajoutees = (int) apres.keySet().stream().filter(c -> !avant.containsKey(c)).count();
        int retirees = (int) avant.keySet().stream().filter(c -> !apres.containsKey(c)).count();
        boolean close = l.getEtat() == EtatLiaison.CLOSE;
        return new Impact(l.getId(), l.getInstitutionNom(), conservees, ajoutees, retirees, modifiees, close, close ? "Liaison close" : null);
    }

    private static Map<String, ReferenceOperation> parCode(List<ReferenceOperation> ops) {
        return ops.stream().collect(Collectors.toMap(ReferenceOperation::getCode, Function.identity(), (a, b) -> a));
    }

    private static boolean identiques(ReferenceOperation a, ReferenceOperation b) {
        return (
            Objects.equals(a.getLibelle(), b.getLibelle()) &&
            Objects.equals(a.getPhase(), b.getPhase()) &&
            a.getSegment() == b.getSegment() &&
            a.getPartie() == b.getPartie() &&
            Objects.equals(a.getServiceCode(), b.getServiceCode()) &&
            a.getNatureCouverture() == b.getNatureCouverture() &&
            Objects.equals(a.getFonctionResponsable(), b.getFonctionResponsable()) &&
            Objects.equals(a.predecesseursListe(), b.predecesseursListe())
        );
    }
}
