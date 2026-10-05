package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.BlocageParcours;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.EtatFicheSuivi;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.EtatProcesVerbal;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.Permissions;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.Sla;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.parcours.Vues;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Projection du parcours en représentations de l'interface (dossier, opérations, étapes à six éléments,
 * périmètre, résumé, fiche de suivi, contexte de certification), pour le DES et pour le portail. Les
 * actions permises sont décidées ici, selon l'état et la matrice des rôles du jeton (D-58, D-61).
 * Service de domaine pur.
 */
public class ParcoursProjection {

    /** Données tenues hors de l'agrégat : conducteur (annuaire), banque partenaire (registre), version du référentiel. */
    public record Contexte(Personne conducteur, String banquePartenaireNom, Integer versionReferentiel) {}

    private static final String CONDUCTEUR_GIMAC = "Conducteur de projet GIMAC";

    // ------------------------------------------------------------------ opérations

    public Vues.Operation operation(Parcours p, OperationParcours o, Acteur a, LocalDate aujourdhui) {
        return new Vues.Operation(
            o.getId().toString(),
            p.id().toString(),
            o.getRang(),
            o.getLibelle(),
            o.getEtape(),
            o.getSegment(),
            o.getPlateforme(),
            o.getGrain(),
            o.getEtat(),
            o.getRaci(),
            o.getEcheance(),
            Parcours.sla(o, aujourdhui),
            o.getFaiteLe(),
            o.getPreuve(),
            o.isCle(),
            o.isSecret(),
            o.isValeurReference(),
            o.isOubliSansSymptome(),
            o.isPivot(),
            p.predecesseursNonFaits(o),
            List.of(),
            actions(o, a),
            o.getGeste(),
            o.getJalon(),
            o.getDeclencheur(),
            o.getLivrables()
        );
    }

    /** Actions permises sur une opération : son geste quand elle est ouverte, la validation (jamais par R), l'attribution des qualités. */
    public List<String> actions(OperationParcours o, Acteur a) {
        List<String> l = new ArrayList<>();
        boolean attribuer = a.peut(Permissions.OPERATION_ASSIGN, Permissions.STEP_MANAGE);
        switch (o.getEtat()) {
            case FAITE, SANS_OBJET -> l.add("CONSULTER");
            case A_FAIRE, EN_COURS -> {
                if (Parcours.gesteDedie(o)) {
                    // Geste réalisé ailleurs (affectation, onglet de la décision, fiche de suivi) : aucune pièce libre.
                    break;
                }
                String geste = Parcours.geste(o);
                boolean permis = switch (geste) {
                    case "CONSTATER_COUVERTURE" -> a.peut(Permissions.DOSSIER_VERIFY);
                    case "DEPOSER_LIVRABLE" -> a.peut(Permissions.STEP_MANAGE, Permissions.EVIDENCE_SUBMIT);
                    default -> a.peut(Permissions.EVIDENCE_SUBMIT);
                };
                if (permis) {
                    l.add(geste);
                }
            }
            case EN_VALIDATION -> {
                if (a.peut(Permissions.OPERATION_VALIDATE) && !a.est(o.getRaci().r())) {
                    l.add("VALIDER");
                }
            }
            default -> {
                // Verrouillée ou bloquée : seule l'attribution des qualités reste possible.
            }
        }
        if (attribuer && !o.faite()) {
            l.add("ATTRIBUER");
        }
        return l;
    }

    /**
     * Onglet du dossier où se réalise le geste d'une opération : décision pour une opération close par un
     * événement de MS-CERTIFICATION (verrous), mise en service pour la fiche de suivi et le service
     * supplémentaire, opérations sinon.
     */
    public static String onglet(OperationParcours o) {
        if (o.getDeclencheur() != null) {
            return "decision";
        }
        return o.porte(Geste.FICHE_SUIVI) || o.getParticularite() == Particularite.SERVICE_SUPPLEMENTAIRE ? "mise-en-service" : "operations";
    }

    public static String lienOperation(Parcours p, OperationParcours o) {
        return "/des/parcours/" + p.id() + "?onglet=" + onglet(o) + "&operation=" + o.getId();
    }

    // ------------------------------------------------------------------ étapes, périmètre, blocages

    /** Workflow d'étape à six éléments (CR §7.1) : l'étape n'est close que s'ils sont tous réunis. */
    public List<Vues.Etape> etapes(Parcours p, LocalDate aujourdhui) {
        List<Vues.Etape> l = new ArrayList<>();
        for (EtapeCode code : EtapeCode.values()) {
            if (code == EtapeCode.PRISE_CONTACT || code == EtapeCode.ADHESION) {
                // Étapes 1 et 2 tenues par MS-PARTICIPANT : closes à l'ouverture de la liaison (D-57).
                Map<String, Boolean> tout = elementsReunis();
                String livrable = code == EtapeCode.ADHESION ? "parcours.livrable.dossierAdhesion" : "parcours.livrable.enregistrementCandidat";
                l.add(new Vues.Etape(code, code.rang(), "CLOSE", List.of(Fonction.DDRM), null, tout, 1, 1, List.of(new Vues.Livrable(livrable, true)), null, null, null, null, null, null));
                continue;
            }
            List<OperationParcours> ops = p.operations().stream().filter(o -> o.getEtape() == code).toList();
            long faites = ops.stream().filter(OperationParcours::faite).count();
            boolean close = !ops.isEmpty() && faites == ops.size();
            boolean enCours = !close && (faites > 0 || ops.stream().anyMatch(o -> o.getEtat() == EtatOperationParcours.A_FAIRE || o.getEtat() == EtatOperationParcours.BLOQUEE || o.getEtat() == EtatOperationParcours.EN_VALIDATION));
            boolean bloquee = ops.stream().anyMatch(o -> o.getEtat() == EtatOperationParcours.BLOQUEE);
            OperationParcours premier = ops.isEmpty() ? null : ops.get(0);
            OperationParcours dernier = ops.isEmpty() ? null : ops.get(ops.size() - 1);
            String etat = ops.isEmpty() ? "SANS_OBJET" : close ? "CLOSE" : bloquee ? "BLOQUEE" : enCours ? "EN_COURS" : "A_VENIR";
            Set<Fonction> porteurs = new LinkedHashSet<>();
            ops.forEach(o -> {
                if (o.fonctionR() != null) {
                    porteurs.add(o.fonctionR());
                }
            });
            Personne responsable = premier == null ? null : premier.getRaci().r();
            boolean preuve = faites > 0 && ops.stream().allMatch(o -> !o.faite() || o.getPreuve() != null);
            Map<String, Boolean> el = new LinkedHashMap<>();
            el.put("RESPONSABLE", responsable != null);
            el.put("ACTIONS", close);
            el.put("LIVRABLES", close);
            el.put("VALIDATION", close);
            el.put("PREUVE", preuve);
            el.put("DATE", close);
            Sla sla = close
                ? null
                : ops.stream().anyMatch(o -> Parcours.sla(o, aujourdhui) == Sla.DEPASSE)
                    ? Sla.DEPASSE
                    : ops.stream().anyMatch(o -> Parcours.sla(o, aujourdhui) == Sla.ECHEANCE_PROCHE) ? Sla.ECHEANCE_PROCHE : enCours ? Sla.DANS_LES_DELAIS : null;
            LocalDate closeLe = close ? dernier.getFaiteLe() : null;
            LocalDate debut = premier == null ? null : premier.getFaiteLe() != null ? premier.getFaiteLe() : enCours ? premier.getEcheance() : null;
            l.add(
                new Vues.Etape(
                    code,
                    code.rang(),
                    etat,
                    List.copyOf(porteurs),
                    responsable,
                    el,
                    (int) faites,
                    ops.size(),
                    ops.stream().filter(o -> !o.getLivrables().isEmpty()).map(o -> new Vues.Livrable("parcours.op." + o.getRang(), o.faite())).toList(),
                    premier == null ? null : premier.getRaci().v(),
                    closeLe,
                    debut,
                    closeLe,
                    dernier == null ? null : dernier.getEcheance(),
                    sla
                )
            );
        }
        return l;
    }

    private static Map<String, Boolean> elementsReunis() {
        Map<String, Boolean> el = new LinkedHashMap<>();
        for (String e : List.of("RESPONSABLE", "ACTIONS", "LIVRABLES", "VALIDATION", "PREUVE", "DATE")) {
            el.put(e, true);
        }
        return el;
    }

    /** Périmètre service par service : essais, certification, ouverture ; taux du simulateur repris pour le mobile. */
    public List<Vues.ServicePerimetre> perimetre(Parcours p) {
        boolean enTest = p.etat().getStatut() == StatutParcours.EN_TEST;
        boolean mobile = p.liaison().getTypeProjet() == TypeProjet.PAIEMENT_MOBILE;
        return p
            .services()
            .stream()
            .map(s -> {
                String etat;
                if (!s.isInclus()) {
                    etat = "NON_DEMARRE";
                } else if (Boolean.TRUE.equals(s.getOuvert())) {
                    etat = "OUVERT";
                } else if (Boolean.FALSE.equals(s.getOuvert())) {
                    etat = "NON_OUVERT";
                } else if (s.certifie()) {
                    etat = "CERTIFIE";
                } else {
                    etat = enTest && s.getEntreeEssai() != null ? "EN_TEST" : "NON_DEMARRE";
                }
                Integer avancement = null;
                if (mobile && s.isInclus()) {
                    avancement = s.certifie() ? Integer.valueOf(100) : "EN_TEST".equals(etat) ? p.mesures().get(s.getServiceCode()) : null;
                }
                return new Vues.ServicePerimetre(s.getServiceCode(), s.getLibelle(), s.isInclus(), etat, s.getEntreeEssai(), s.getSortieEssai(), s.getCertifieLe(), avancement);
            })
            .toList();
    }

    public Vues.Blocage blocage(BlocageParcours b, Acteur a) {
        List<String> actions = b.actif() && a.peut(Permissions.STEP_MANAGE) ? List.of("LEVER") : List.of();
        return new Vues.Blocage(b.getId().toString(), b.getLiaisonId().toString(), b.getMotif(), b.getDepuis(), b.getProchaineAction(), b.getResponsable(), b.getEcheanceLevee(), b.getLeveLe(), b.getEtape(), List.of(), actions);
    }

    // ------------------------------------------------------------------ dossier et résumé

    /** Dossier numérique (CR Fig. 7.3) vu du DES, avec les actions permises à l'acteur. */
    public Vues.Dossier dossier(Parcours p, Contexte ctx, Acteur a, LocalDate aujourdhui) {
        List<String> actions = new ArrayList<>();
        boolean conducteur = !p.etat().isConducteurADesigner();
        if (conducteur && a.peut(Permissions.STEP_MANAGE)) {
            actions.add("DECLARER_BLOCAGE");
        }
        if (a.peut(Permissions.PROJECT_DESIGNATE)) {
            actions.add("DESIGNER_CONDUCTEUR");
        }
        if (p.etat().getStatut() == StatutParcours.EN_PRODUCTION && a.peut(Permissions.LIAISON_OPEN, Permissions.STEP_MANAGE)) {
            actions.add("OUVRIR_SERVICE_SUPPLEMENTAIRE");
        }
        if (!p.operations(o -> o.porte(Geste.AFFECTER_RESSOURCES) && o.getEtat() == EtatOperationParcours.A_FAIRE).isEmpty() && a.peut(Permissions.STEP_MANAGE)) {
            actions.add("AFFECTER_RESSOURCES");
        }
        if (!p.operations(o -> o.porte(Geste.CONSTATER_COUVERTURE) && o.getEtat().ouverte()).isEmpty() && a.peut(Permissions.DOSSIER_VERIFY)) {
            actions.add("CONSTATER_COUVERTURE");
        }
        var l = p.liaison();
        return new Vues.Dossier(
            l.getId().toString(),
            p.etat().getReference(),
            l.getInstitutionId().toString(),
            l.getInstitutionNom(),
            l.getTypeProjet(),
            ctx.banquePartenaireNom(),
            null,
            p.etat().getStatut(),
            p.etat().getEtapeCourante(),
            conducteur ? ctx.conducteur() : null,
            p.premiereDuResponsable(Fonction.DDRM).map(o -> o.getRaci().r()).orElse(null),
            p.premiereDuResponsable(Fonction.SNA).map(o -> o.getRaci().r()).orElse(null),
            p.etat().getDateDemarrage(),
            p.dateCibleProduction(),
            ctx.versionReferentiel(),
            perimetre(p),
            p.couvertureCertification(),
            p.couvertureMiseEnService(),
            etapes(p, aujourdhui),
            p.blocages().stream().map(b -> blocage(b, a)).toList(),
            List.of(new Vues.Interlocuteur(Personne.correspondant(l.getInstitutionId().toString(), l.getInstitutionNom()).nom(), "Correspondant du participant")),
            p.etat().getLiaisonOrigineId() == null ? null : p.etat().getLiaisonOrigineId().toString(),
            coordonnees(p),
            List.of(),
            actions
        );
    }

    /** Coordonnées techniques d'essai communiquées (geste COORDONNEES_TECHNIQUES), relisibles. */
    public Vues.Coordonnees coordonnees(Parcours p) {
        return p
            .parGeste(Geste.COORDONNEES_TECHNIQUES)
            .filter(OperationParcours::faite)
            .map(o -> {
                Preuve pr = o.getPreuve();
                String v = pr == null || pr.valeurReference() == null ? null : pr.valeurReference().valeur();
                if (v == null || v.lastIndexOf(':') < 0) {
                    return null;
                }
                return new Vues.Coordonnees(v.substring(0, v.lastIndexOf(':')), v.substring(v.lastIndexOf(':') + 1), p.etat().getPointsTerminaison(), o.getFaiteLe());
            })
            .orElse(null);
    }

    /**
     * Projection du portail : aucune donnée interne ; les intervenants du GIMAC n'apparaissent que par
     * leur fonction, le conducteur comme « Conducteur de projet GIMAC ». Aucune action du DES.
     */
    public Vues.Dossier dossierPortail(Parcours p, Contexte ctx, LocalDate aujourdhui) {
        Vues.Dossier d = dossier(p, ctx, new Acteur(null, null, Set.of(), null), aujourdhui);
        Personne conducteur = d.conducteur() == null ? null : new Personne("CONDUCTEUR", CONDUCTEUR_GIMAC, Fonction.CONDUCTEUR);
        List<Vues.Etape> etapes = d
            .etapes()
            .stream()
            .map(e ->
                new Vues.Etape(e.code(), e.rang(), e.etat(), e.porteurs(), parFonction(e.responsable()), e.elements(), e.actionsFaites(), e.actionsTotal(), e.livrables(), parFonction(e.validateur()), e.valideeLe(), e.debut(), e.closeLe(), e.echeance(), e.sla())
            )
            .toList();
        List<Vues.Blocage> blocages = d
            .blocages()
            .stream()
            .map(b -> new Vues.Blocage(b.id(), b.liaisonId(), b.motif(), b.depuis(), b.prochaineAction(), parFonction(b.responsable()), b.echeanceLevee(), b.leveLe(), b.etape(), List.of(), List.of()))
            .toList();
        return new Vues.Dossier(
            d.id(),
            d.reference(),
            d.institutionId(),
            d.institutionNom(),
            d.typeProjet(),
            d.banquePartenaireNom(),
            d.personnalisateurNom(),
            d.statut(),
            d.etapeCourante(),
            conducteur,
            parFonction(d.responsableFonctionnel()),
            parFonction(d.responsableTechnique()),
            d.dateDemarrage(),
            d.dateCibleProduction(),
            d.versionReferentiel(),
            d.perimetre(),
            d.couvertureCertification(),
            d.couvertureMiseEnService(),
            etapes,
            blocages,
            d.interlocuteursParticipant(),
            d.liaisonOrigineId(),
            d.coordonneesTechniques(),
            List.of(),
            List.of()
        );
    }

    private static Personne parFonction(Personne p) {
        return p == null ? null : p.parFonction();
    }

    public Vues.LiaisonResume resume(Parcours p, String conducteurNom, LocalDate aujourdhui) {
        var l = p.liaison();
        return new Vues.LiaisonResume(
            l.getId().toString(),
            p.etat().getReference(),
            l.getInstitutionNom(),
            l.getTypeProjet(),
            p.etat().getStatut(),
            p.etat().getEtapeCourante(),
            p.etat().isConducteurADesigner() ? null : conducteurNom,
            p.couvertureCertification(),
            p.couvertureMiseEnService(),
            p.bloquee(),
            p.tachesEnRetard(aujourdhui),
            p.prochaineEcheance(),
            p.dateCibleProduction()
        );
    }

    // ------------------------------------------------------------------ fiche de suivi et contexte de certification

    public Vues.FicheSuivi fiche(Parcours p, Acteur a, boolean portail) {
        EtatFicheSuivi e = p.etat().getFicheEtat();
        List<String> actions = new ArrayList<>();
        if (portail) {
            if (e == EtatFicheSuivi.REMISE && a.peut(Permissions.PORTAL_SUBMIT)) {
                actions.add("RENSEIGNER");
            }
        } else {
            if (e == EtatFicheSuivi.A_REMETTRE && p.etat().getStatut() == StatutParcours.EN_PRODUCTION && a.peut(Permissions.STEP_MANAGE, Permissions.DOSSIER_VERIFY)) {
                actions.add("REMETTRE");
            }
            if (e == EtatFicheSuivi.RENSEIGNEE && a.peut(Permissions.DOSSIER_VERIFY)) {
                actions.add("CONSTATER");
            }
        }
        List<Vues.LigneFiche> lignes = p.servicesInclus().stream().map(s -> new Vues.LigneFiche(s.getServiceCode(), s.getLibelle(), s.getOuvert(), s.getCommentaire())).toList();
        return new Vues.FicheSuivi(p.id().toString(), e, lignes, p.etat().getFicheRenseigneeLe(), p.etat().getFicheConstateeLe(), List.of(), actions);
    }

    public Vues.ContexteCertification contexte(Parcours p, String conducteurNom) {
        var c = p.couvertureCertification();
        var l = p.liaison();
        return new Vues.ContexteCertification(
            l.getId(),
            p.etat().getReference(),
            l.getInstitutionId(),
            l.getInstitutionNom(),
            l.getTypeProjet(),
            p.etat().isFintech(),
            p.etat().getStatut(),
            new Vues.CouvertureSimple(c.taux(), c.numerateur(), c.denominateur()),
            p.etat().isCouvertureConstatee(),
            p.servicesCouverts(),
            l.getChefDeProjetId(),
            conducteurNom
        );
    }

    // ------------------------------------------------------------------ portail : geste du participant

    /** Geste du participant sur une opération ouverte et écran du portail où il l'accomplit (qualité Externe, CR §3.4). */
    public GesteParticipant gesteParticipant(Parcours p, OperationParcours o) {
        String base = "/portail/liaisons/" + p.id();
        boolean ouverte = o.getEtat().ouverte();
        EtatProcesVerbal pv = p.etat().getEtatPv();
        if (o.porte(Geste.COORDONNEES_TECHNIQUES)) {
            return new GesteParticipant(base + "/coordonnees", ouverte ? List.of("COMMUNIQUER_COORDONNEES") : List.of());
        }
        if (Declencheurs.PV_SIGNE.equals(o.getDeclencheur())) {
            return new GesteParticipant(base + "/proces-verbal", pv == EtatProcesVerbal.ETABLI || pv == EtatProcesVerbal.SIGNE_GIMAC ? List.of("SIGNER_PROCES_VERBAL") : List.of());
        }
        if (Declencheurs.ATTESTATIONS_ARCHIVEES.equals(o.getDeclencheur())) {
            return new GesteParticipant(base + "/attestations", ouverte && !p.attestationsAFournir().isEmpty() ? List.of("DEPOSER_ATTESTATION") : List.of());
        }
        if (o.porte(Geste.FICHE_SUIVI) && Parcours.ficheDuParticipant(o)) {
            return new GesteParticipant(base + "/fiche-suivi", p.etat().getFicheEtat() == EtatFicheSuivi.REMISE ? List.of("RENSEIGNER_FICHE_SUIVI") : List.of());
        }
        if (livrableParticipant(o) && ouverte) {
            return new GesteParticipant(base + "/livrables/" + o.getId(), List.of("DEPOSER_LIVRABLE"));
        }
        return new GesteParticipant(base, List.of());
    }

    /**
     * Livrables qui reviennent au participant, déposés depuis le portail : pièce libre (livrable ou preuve, hors
     * clé, valeur de référence et opération close par un événement) dont il est responsable, dont un livrable
     * attendu est déposé par une fonction externe, ou contribution à un livrable de phase (atelier, paramétrage
     * d'essai) dont il est partie externe.
     */
    public static boolean livrableParticipant(OperationParcours o) {
        if ((!o.porte(Geste.DEPOSER_LIVRABLE) && !o.porte(Geste.DEPOSER_PREUVE)) || o.getDeclencheur() != null || o.isCle() || o.isValeurReference()) {
            return false;
        }
        Personne r = o.getRaci().r();
        if ((r != null && r.fonction() == Fonction.PARTICIPANT) || o.getLivrables().stream().anyMatch(l -> l.deposant() != null && l.deposant().externe())) {
            return true;
        }
        return o.porte(Geste.DEPOSER_LIVRABLE) && o.getRaci().e().stream().anyMatch(x -> x.fonction() == Fonction.PARTICIPANT);
    }

    public record GesteParticipant(String lien, List<String> actionsPermises) {}
}
