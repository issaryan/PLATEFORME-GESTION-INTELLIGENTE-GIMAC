package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.domain.event.DestinataireOperation;
import cm.gimac.des.homologation.domain.event.LiaisonMilestoneReachedPayload;
import cm.gimac.des.homologation.domain.event.OperationClosedPayload;
import cm.gimac.des.homologation.domain.event.OperationParcoursPayload;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Raci;
import cm.gimac.des.homologation.domain.parcours.Renvoi;
import cm.gimac.des.homologation.domain.service.ParcoursProjection;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Événements du parcours pour les intervenants qui ne reçoivent pas de tâche (D-93 §1), écrits dans l'outbox de la
 * transaction du geste, à partir de la photographie prise au chargement (SuiviTachesParcours) :
 * <ul>
 * <li>{@code homologation.operation-activated.v1} : opération devenue à faire (ouverture, renvoi, reprise) ;</li>
 * <li>{@code homologation.operation-validation-requested.v1} : opération passée en validation ;</li>
 * <li>{@code homologation.operation-closed.v1} : opération devenue faite ou sans objet (clôtures forcées comprises) ;</li>
 * <li>{@code homologation.liaison-milestone-reached.v1} : jalon du référentiel atteint.</li>
 * </ul>
 */
@Component
public class EvenementsParcours {

    static final String AGREGAT = "liaison";
    static final Set<EtatOperationParcours> ACTIVES = EnumSet.of(EtatOperationParcours.A_FAIRE, EtatOperationParcours.EN_COURS, EtatOperationParcours.BLOQUEE);
    static final Set<EtatOperationParcours> DEPASSEES = EnumSet.of(EtatOperationParcours.EN_SIGNATURE, EtatOperationParcours.FAITE, EtatOperationParcours.SANS_OBJET);
    static final Set<EtatOperationParcours> CLOSES = EnumSet.of(EtatOperationParcours.FAITE, EtatOperationParcours.SANS_OBJET);

    private final OutboxPublisher outbox;
    private final Clock clock;

    public EvenementsParcours(OutboxPublisher outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    /** Compare l'état d'avant (par opération, jalons atteints) à l'état présent et écrit les événements. */
    public void emettre(Parcours p, Map<UUID, EtatOperationParcours> avant, Set<Jalon> jalonsAvant) {
        for (OperationParcours o : p.operations()) {
            EtatOperationParcours av = avant.getOrDefault(o.getId(), EtatOperationParcours.VERROUILLEE);
            EtatOperationParcours ap = o.getEtat();
            if (ACTIVES.contains(ap) && !ACTIVES.contains(av)) {
                String motif = av == EtatOperationParcours.EN_VALIDATION ? "RENVOI" : DEPASSEES.contains(av) ? "REPRISE" : "OUVERTURE";
                publier(p, OperationParcoursPayload.TYPE_ACTIVEE, operation(p, o, motif));
            }
            if (ap == EtatOperationParcours.EN_VALIDATION && av != EtatOperationParcours.EN_VALIDATION) {
                publier(p, OperationParcoursPayload.TYPE_VALIDATION, operation(p, o, null));
            }
            if (CLOSES.contains(ap) && ap != av) {
                publier(p, OperationClosedPayload.TYPE, cloture(p, o));
            }
        }
        for (Jalon j : Jalon.values()) {
            if (p.jalonAtteint(j) && !jalonsAvant.contains(j)) {
                p.parJalon(j).ifPresent(o -> publierJalon(p, j, o));
            }
        }
    }

    /** Jalons atteints d'un parcours (photographie). */
    public static Set<Jalon> jalons(Parcours p) {
        Set<Jalon> s = EnumSet.noneOf(Jalon.class);
        for (Jalon j : Jalon.values()) {
            if (p.jalonAtteint(j)) {
                s.add(j);
            }
        }
        return s;
    }

    private void publier(Parcours p, String type, Object charge) {
        outbox.publish(AGREGAT, p.id(), type, charge, p.liaison().getInstitutionId().toString());
    }

    OperationParcoursPayload operation(Parcours p, OperationParcours o, String motif) {
        Raci r = o.getRaci();
        List<DestinataireOperation> dest = new ArrayList<>();
        r.c().stream().filter(x -> x != null && !x.fonction().externe()).forEach(x -> dest.add(destinataire("C", x)));
        r.i().stream().filter(x -> x != null && !x.fonction().externe()).forEach(x -> dest.add(destinataire("I", x)));
        Set<String> institutions = new LinkedHashSet<>();
        if (r.r() != null && r.r().institutionnelle()) {
            institutions.add(r.r().institutionId());
        }
        r.e().stream().filter(x -> x != null && x.institutionnelle()).forEach(x -> institutions.add(x.institutionId()));
        boolean externe = (r.r() != null && r.r().fonction().externe()) || !r.e().isEmpty();
        List<Renvoi> renvois = o.getRenvois();
        var l = p.liaison();
        return new OperationParcoursPayload(
            l.getId(),
            p.etat().getReference(),
            l.getInstitutionId(),
            l.getInstitutionNom(),
            l.getTypeProjet().name(),
            l.getEtat() == null ? null : l.getEtat().name(),
            p.etat().getStatut().name(),
            o.getId(),
            o.getRang(),
            o.getLibelle(),
            o.getEtapeCode(),
            o.getVue().name(),
            o.getGeste().name(),
            o.getJalon() == null ? null : o.getJalon().name(),
            o.getEcheance(),
            motif,
            r.r() == null ? null : destinataire("R", r.r()),
            r.v() == null ? null : destinataire("V", r.v()),
            dest,
            externe,
            institutions.isEmpty() ? null : institutions.iterator().next(),
            institutions.isEmpty() ? null : List.copyOf(institutions),
            "RENVOI".equals(motif) && !renvois.isEmpty() ? renvois.get(renvois.size() - 1).motif() : null,
            ParcoursProjection.lienOperation(p, o),
            clock.instant(),
            o.getDeclencheur(),
            o
                .getSignatures()
                .stream()
                .map(s -> new OperationParcoursPayload.SignatureConfiguree(s.code(), s.libelle(), s.signataire() == null ? null : s.signataire().name(), s.ordreEffectif()))
                .toList()
        );
    }

    OperationClosedPayload cloture(Parcours p, OperationParcours o) {
        var l = p.liaison();
        return new OperationClosedPayload(
            l.getId(),
            p.etat().getReference(),
            l.getInstitutionId(),
            o.getId(),
            o.getRang(),
            o.getLibelle(),
            o.getEtat().name(),
            o.getDeclencheur(),
            o.getJalon() == null ? null : o.getJalon().name(),
            ParcoursProjection.lienOperation(p, o),
            clock.instant()
        );
    }

    static DestinataireOperation destinataire(String qualite, Personne x) {
        boolean aAffecter = !x.affectee();
        String compte = x.affectee() && !x.institutionnelle() ? x.id() : null;
        return new DestinataireOperation(
            qualite,
            x.fonction().name(),
            x.fonction().libelle(),
            compte,
            aAffecter,
            aAffecter ? x.fonction().profils() : null,
            x.institutionId()
        );
    }

    private void publierJalon(Parcours p, Jalon j, OperationParcours o) {
        var l = p.liaison();
        Raci r = o.getRaci();
        List<DestinataireOperation> dest = new ArrayList<>();
        r.c().stream().filter(x -> x != null && !x.fonction().externe()).forEach(x -> dest.add(destinataire("C", x)));
        r.i().stream().filter(x -> x != null && !x.fonction().externe()).forEach(x -> dest.add(destinataire("I", x)));
        publier(
            p,
            LiaisonMilestoneReachedPayload.TYPE,
            new LiaisonMilestoneReachedPayload(
                l.getId(),
                p.etat().getReference(),
                l.getInstitutionId(),
                l.getInstitutionNom(),
                l.getTypeProjet().name(),
                l.modeleTypeProjet().name(),
                j.name(),
                j.libelle(),
                p.etat().getStatut().name(),
                l.getEtat() == null ? null : l.getEtat().name(),
                o.getId(),
                o.getRang(),
                o.getLibelle(),
                p.etat().getVersionId(),
                p.etat().getVersionNumero(),
                p.conducteur() == null ? null : p.conducteur().id(),
                dest,
                "/des/parcours/" + p.id(),
                clock.instant()
            )
        );
    }
}
