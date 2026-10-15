package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import cm.gimac.des.homologation.domain.parcours.DepotLivrable;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.Renvoi;
import cm.gimac.des.homologation.domain.parcours.SignatureRecueillie;
import cm.gimac.des.homologation.domain.parcours.Vues;
import cm.gimac.des.homologation.domain.parcours.Vues.EtatLivrable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * États des preuves et chronologie réelle d'une liaison (D-109, docs/lot1/referentiel-preuves-signatures.md) :
 * service de domaine pur, sans état. Une opération reprise de l'existant (D-107) n'est jamais montrée en attente :
 * ses livrables sans pièce sont « reprise, pièce non numérisée » ; une opération close avant que le livrable ne soit
 * exigé le montre « non exigé ».
 */
public final class EtatsPreuves {

    private EtatsPreuves() {}

    /** État d'un livrable de l'opération, tous services confondus (grain du service : le moins avancé). */
    public static EtatLivrable etat(OperationParcours o, Livrable l, List<String> services) {
        if (l.parService() && !services.isEmpty() && o.depot(l.code(), null).isEmpty()) {
            List<EtatLivrable> e = services.stream().map(s -> etat(o, l, o.depot(l.code(), s))).toList();
            for (EtatLivrable x : List.of(EtatLivrable.REFUSEE, EtatLivrable.ATTENDUE, EtatLivrable.REPRISE_SANS_PIECE, EtatLivrable.NON_EXIGEE, EtatLivrable.DEPOSEE)) {
                if (e.contains(x)) {
                    return x;
                }
            }
            return e.stream().allMatch(x -> x == EtatLivrable.SANS_OBJET) ? EtatLivrable.SANS_OBJET : EtatLivrable.VALIDEE;
        }
        return etat(o, l, o.depot(l.code(), null).or(() -> l.parService() ? o.depot(l.code()) : Optional.empty()));
    }

    /** État d'un livrable pour un dépôt donné (ou son absence). */
    static EtatLivrable etat(OperationParcours o, Livrable l, Optional<DepotLivrable> depot) {
        EtatOperationParcours e = o.getEtat();
        boolean close = e == EtatOperationParcours.FAITE || e == EtatOperationParcours.SANS_OBJET;
        if (depot.isEmpty()) {
            if (o.reprise()) {
                return EtatLivrable.REPRISE_SANS_PIECE;
            }
            return close || e == EtatOperationParcours.EN_SIGNATURE ? EtatLivrable.NON_EXIGEE : EtatLivrable.ATTENDUE;
        }
        DepotLivrable d = depot.orElseThrow();
        if (d.estSansObjet()) {
            return EtatLivrable.SANS_OBJET;
        }
        if (close || e == EtatOperationParcours.EN_SIGNATURE) {
            return EtatLivrable.VALIDEE;
        }
        if (e.ouverte() && renvoyeApres(o, d.deposeLe())) {
            return EtatLivrable.REFUSEE;
        }
        return EtatLivrable.DEPOSEE;
    }

    /** Un renvoi du validateur est intervenu le jour du dépôt ou après : la pièce est à reprendre. */
    private static boolean renvoyeApres(OperationParcours o, LocalDate depose) {
        return o.getRenvois().stream().anyMatch(r -> depose == null || (r.le() != null && !r.le().isBefore(depose)));
    }

    /** Une ligne par service inclus pour un livrable au grain du service (vide sinon). */
    public static List<Vues.ServiceLivrable> services(OperationParcours o, Livrable l, List<String> services) {
        if (!l.parService()) {
            return List.of();
        }
        Optional<DepotLivrable> commun = o.depot(l.code(), null);
        return services
            .stream()
            .map(s -> {
                Optional<DepotLivrable> d = o.depot(l.code(), s).or(() -> commun);
                return new Vues.ServiceLivrable(s, etat(o, l, d), d.orElse(null));
            })
            .toList();
    }

    /** Signature d'une opération close sans avoir été recueillie sur la plateforme : reprise, sinon non recueillie. */
    public static String etatSignature(OperationParcours o, boolean recueillie) {
        if (recueillie) {
            return "SIGNEE";
        }
        if (o.reprise()) {
            return "REPRISE";
        }
        return o.faite() || o.getEtat() == EtatOperationParcours.SANS_OBJET ? "NON_RECUEILLIE" : "ATTENDUE";
    }

    // ------------------------------------------------------------------ chronologie

    /** Chronologie réelle de la liaison, de l'événement le plus récent au plus ancien. */
    public static List<Vues.EvenementChronologie> chronologie(Parcours p) {
        List<Vues.EvenementChronologie> l = new ArrayList<>();
        for (OperationParcours o : p.operations()) {
            String id = o.getId().toString();
            List<DepotLivrable> depots = o.getDepots();
            for (DepotLivrable d : depots) {
                String type = d.estSansObjet() ? "SANS_OBJET" : d.estRepris() ? "DEPOT_REPRIS" : d.origine() != null ? "PIECE_AUTOMATIQUE" : "DEPOT";
                l.add(ev(d.deposeLe(), null, type, o, d.livrable(), d.service(), d.deposePar(), d.estSansObjet() ? d.motif() : d.nom(), d.documentRef(), d.empreinte()));
            }
            Preuve pr = o.getPreuve();
            if (pr != null && depots.isEmpty() && !pr.estReprise() && pr.documentRef() != null && !pr.documentRef().isBlank()) {
                l.add(ev(pr.deposeLe(), null, "DEPOT", o, null, null, pr.deposePar(), pr.nom(), pr.documentRef(), pr.empreinte()));
            }
            for (Renvoi r : o.getRenvois()) {
                l.add(ev(r.le(), null, "RENVOI", o, null, null, r.parNom(), r.motif(), null, null));
            }
            for (SignatureRecueillie s : o.getSignaturesRecueillies()) {
                LocalDate jour = s.signeLe() == null ? null : s.signeLe().atZone(ZoneOffset.UTC).toLocalDate();
                l.add(ev(jour, s.signeLe(), "SIGNATURE", o, null, null, s.signataireNom(), s.code() + (s.fonction() == null ? "" : " · " + s.fonction().name()), null, s.empreinte()));
            }
            if (o.faite() && o.getFaiteLe() != null) {
                boolean reprise = o.reprise();
                l.add(ev(o.getFaiteLe(), null, reprise ? "REPRISE" : "FAITE", o, null, null, reprise ? null : o.getRaci().r() == null ? null : o.getRaci().r().nom(), reprise ? pr.commentaire() : null, null, null));
            }
        }
        for (BlocageParcours b : p.blocages()) {
            l.add(new Vues.EvenementChronologie(b.getDepuis(), null, "BLOCAGE", null, null, null, b.getEtape() == null ? null : b.getEtape().name(), null, null, null, b.getMotif(), null, null));
            if (b.getLeveLe() != null) {
                l.add(new Vues.EvenementChronologie(b.getLeveLe(), null, "LEVEE", null, null, null, b.getEtape() == null ? null : b.getEtape().name(), null, null, null, b.getMotif(), null, null));
            }
        }
        l.sort(
            Comparator.comparing(Vues.EvenementChronologie::date, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(
                Vues.EvenementChronologie::instant,
                Comparator.nullsLast(Comparator.reverseOrder())
            )
        );
        return l;
    }

    private static Vues.EvenementChronologie ev(
        LocalDate date,
        java.time.Instant instant,
        String type,
        OperationParcours o,
        String livrable,
        String service,
        String acteur,
        String detail,
        String documentRef,
        String empreinte
    ) {
        return new Vues.EvenementChronologie(date, instant, type, o.getId().toString(), o.getRang(), o.getLibelle(), o.getEtapeCode(), livrable, service, acteur, detail, documentRef, empreinte);
    }
}
