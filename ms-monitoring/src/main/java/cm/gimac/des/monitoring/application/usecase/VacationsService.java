package cm.gimac.des.monitoring.application.usecase;

import cm.gimac.des.monitoring.application.port.in.Appelant;
import cm.gimac.des.monitoring.application.port.in.Page;
import cm.gimac.des.monitoring.application.port.in.VacationsUseCase;
import cm.gimac.des.monitoring.application.port.in.Vues.EntreeVue;
import cm.gimac.des.monitoring.application.port.in.Vues.RapportCourt;
import cm.gimac.des.monitoring.application.port.in.Vues.VacationCourte;
import cm.gimac.des.monitoring.application.port.in.Vues.VacationEnCours;
import cm.gimac.des.monitoring.application.port.in.Vues.VacationVue;
import cm.gimac.des.monitoring.application.port.in.Vues.VerificationVue;
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.domain.enumeration.ShiftLogNature;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.infrastructure.persistance.MainCouranteRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.RapportsQuartRepository;
import cm.gimac.des.monitoring.infrastructure.persistance.VacationsRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vacations et main courante chaînée (docs/lot1/astreintes-monitoring.md §5, MON-13, MON-14). */
@Service
@Transactional
public class VacationsService implements VacationsUseCase {

    /** Natures saisies par un opérateur ; les autres sont réservées aux entrées automatiques. */
    static final Set<ShiftLogNature> NATURES_SAISIES = EnumSet.of(
        ShiftLogNature.EVENEMENT,
        ShiftLogNature.INTERVENTION,
        ShiftLogNature.RETABLISSEMENT,
        ShiftLogNature.MAINTENANCE,
        ShiftLogNature.CONSIGNE,
        ShiftLogNature.CORRECTION
    );

    private final VacationsRepository vacations;
    private final MainCouranteRepository entrees;
    private final RapportsQuartRepository rapports;
    private final MainCourante mainCourante;
    private final Quarts quarts;

    public VacationsService(
        VacationsRepository vacations,
        MainCouranteRepository entrees,
        RapportsQuartRepository rapports,
        MainCourante mainCourante,
        Quarts quarts
    ) {
        this.vacations = vacations;
        this.entrees = entrees;
        this.rapports = rapports;
        this.mainCourante = mainCourante;
        this.quarts = quarts;
    }

    Shift lire(UUID id) {
        return vacations.findById(id).orElseThrow(() -> MonitoringException.introuvable("MON_VACATION_INTROUVABLE", id));
    }

    @Override
    @Transactional(readOnly = true)
    public VacationEnCours enCours(Appelant appelant) {
        return new VacationEnCours(quarts.enCours().map(this::vue).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public VacationVue vacation(Appelant appelant, UUID id) {
        return vue(lire(id));
    }

    VacationVue vue(Shift v) {
        Optional<Shift> prec = quarts.precedente(v);
        Optional<Shift> suiv = quarts.suivante(v);
        Set<String> ids = new HashSet<>(Quarts.membres(v));
        Map<String, String> noms = quarts.noms(ids);
        RapportCourt rapport = rapports.findByShiftId(v.getId()).map(VacationsService::court).orElse(null);
        RapportCourt rapportPrec = prec.flatMap(p -> rapports.findByShiftId(p.getId())).map(VacationsService::court).orElse(null);
        Instant maintenant = quarts.maintenant();
        boolean enCours = !maintenant.isBefore(v.getStartsAt()) && maintenant.isBefore(v.getEndsAt());
        boolean precedenteResponsable = prec.isPresent() && (rapportPrec == null || "BROUILLON".equals(rapportPrec.etat()) || "SIGNE_SORTANT".equals(rapportPrec.etat()));
        return new VacationVue(
            v.getId(),
            v.getReference(),
            v.getShiftDate(),
            v.getSlot().name(),
            v.getStartsAt(),
            v.getEndsAt(),
            Quarts.operateurs(v).stream().map(o -> Quarts.personne(o, noms)).toList(),
            Quarts.personne(v.getShiftLeadId(), noms),
            enCours,
            rapport,
            prec.map(p -> new VacationCourte(p.getId(), p.getReference(), rapportPrec)).orElse(null),
            suiv.map(s -> new VacationCourte(s.getId(), s.getReference(), rapports.findByShiftId(s.getId()).map(VacationsService::court).orElse(null))).orElse(null),
            precedenteResponsable ? "VACATION_PRECEDENTE" : "CETTE_VACATION"
        );
    }

    static RapportCourt court(ShiftReport r) {
        return new RapportCourt(r.getId(), r.getState().name());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntreeVue> mainCourante(Appelant appelant, UUID vacation, Integer page, Integer taille) {
        lire(vacation);
        var res = entrees.findByShiftIdOrderByChainIndexDesc(vacation, SupervisionService.pagination(page, taille));
        Set<String> ids = new HashSet<>();
        res.getContent().forEach(e -> ids.add(e.getAuthorId().toString()));
        Map<String, String> noms = quarts.noms(ids);
        return new Page<>(res.getContent().stream().map(e -> vue(e, noms)).toList(), res.getTotalElements(), res.getNumber(), res.getSize());
    }

    static EntreeVue vue(ShiftLogEntry e, Map<String, String> noms) {
        return new EntreeVue(
            e.getId(),
            e.getChainIndex(),
            e.getOccurredAt(),
            MainCourante.SYSTEME.equals(e.getAuthorId()) ? null : Quarts.personne(e.getAuthorId(), noms),
            e.getNature().name(),
            e.getText(),
            e.getReference(),
            e.getCorrects() == null ? null : e.getCorrects().getId(),
            e.getHash(),
            e.getPreviousHash()
        );
    }

    @Override
    public EntreeVue consigner(Appelant appelant, UUID vacation, String nature, String texte, String reference, UUID rectifie) {
        ShiftLogNature n = SupervisionService.enumeration(ShiftLogNature.class, nature, "nature");
        if (n == null || !NATURES_SAISIES.contains(n)) {
            throw MonitoringException.invalide("nature", "Nature invalide");
        }
        String t = texte == null ? "" : texte.trim();
        if (t.isEmpty() || t.length() > 2000) {
            throw MonitoringException.invalide("texte", "Texte de 1 à 2000 caractères");
        }
        String ref = reference == null || reference.isBlank() ? null : reference.trim();
        if (ref != null && ref.length() > 100) {
            throw MonitoringException.invalide("reference", "Référence de 100 caractères au plus");
        }
        UUID auteur = appelant.uuid().orElseThrow(() -> MonitoringException.interdit("MON_JETON_UTILISATEUR_EXIGE", "Jeton utilisateur exigé"));
        Shift v = lire(vacation);
        Instant maintenant = quarts.maintenant();
        if (!maintenant.isBefore(v.getEndsAt())) {
            throw MonitoringException.conflit("MON_VACATION_CLOSE", "Vacation close");
        }
        if (maintenant.isBefore(v.getStartsAt()) || !Quarts.membre(v, appelant.id())) {
            throw MonitoringException.interdit("MON_HORS_VACATION", "Réservé aux membres de la vacation en cours");
        }
        ShiftLogEntry corrigee = null;
        if (n == ShiftLogNature.CORRECTION) {
            corrigee = rectifie == null ? null : entrees.findById(rectifie).filter(e -> e.getShift().getId().equals(v.getId())).orElse(null);
            if (corrigee == null) {
                throw MonitoringException.invalide("rectifie", "Une correction cite une entrée de la même vacation");
            }
        }
        ShiftLogEntry e = mainCourante.ajouter(v, auteur, n, t, ref, corrigee, maintenant);
        return vue(e, quarts.noms(List.of(auteur.toString())));
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationVue verifier(Appelant appelant, UUID vacation) {
        lire(vacation);
        MainCourante.Verification v = MainCourante.verifier(new ArrayList<>(entrees.findByShiftIdOrderByChainIndexAsc(vacation)));
        return new VerificationVue(v.intacte(), v.entrees(), v.tete());
    }
}
