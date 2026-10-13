package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.EtapeReferentiel;
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
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import cm.gimac.des.homologation.domain.parcours.SignatureRecueillie;
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

    /**
     * Données tenues hors de l'agrégat : conducteur (annuaire), banque partenaire (registre), version du référentiel et
     * ses étapes réelles (D-89, D-93 §5 ; absentes : les étapes canoniques).
     */
    public record Contexte(Personne conducteur, String banquePartenaireNom, Integer versionReferentiel, List<EtapeReferentiel> etapesVersion) {
        public Contexte(Personne conducteur, String banquePartenaireNom, Integer versionReferentiel) {
            this(conducteur, banquePartenaireNom, versionReferentiel, null);
        }
    }

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
            actionsAvecSignature(p, o, a),
            o.getGeste(),
            o.getJalon(),
            o.getDeclencheur(),
            o.getLivrables(),
            signatures(p, o),
            o.getEtapeCode(),
            o.getVue(),
            livrablesAttendus(p, o, a),
            o.getRenvois(),
            o.getEcheanceCible(),
            o.getDelaiJours(),
            o.getDelaiMaxJours()
        );
    }

    /** Livrables attendus de l'opération, leur dépôt et le droit de l'acteur à les déposer maintenant (D-93 §3). */
    public List<Vues.LivrableAttendu> livrablesAttendus(Parcours p, OperationParcours o, Acteur a) {
        Set<Fonction> f = VisibiliteReferentiel.fonctions(a.permissions());
        boolean ouverte = o.getEtat().ouverte() && !Parcours.gesteDedie(o);
        return o
            .getLivrables()
            .stream()
            .map(l ->
                new Vues.LivrableAttendu(
                    l.code(),
                    l.libelle(),
                    l.nature(),
                    l.deposant(),
                    l.deposant() == null ? null : l.deposant().libelle(),
                    l.typesPieces(),
                    l.obligatoire(),
                    o.depot(l.code()).orElse(null),
                    ouverte && offert(p.droitDepot(o, l, a, f))
                )
            )
            .toList();
    }

    /** Signatures exigées de l'opération (D-91), attendues ou signées, dans leur ordre. */
    public List<Vues.Signature> signatures(Parcours p, OperationParcours o) {
        List<String> suivantes = o.signaturesSuivantes().stream().map(SignatureOperation::code).toList();
        boolean enSignature = o.getEtat() == EtatOperationParcours.EN_SIGNATURE;
        return o
            .getSignatures()
            .stream()
            .map(s -> {
                var r = o.recueillie(s.code());
                return new Vues.Signature(
                    s.code(),
                    s.libelle(),
                    s.ordreEffectif(),
                    s.signataire(),
                    s.signataire().libelle(),
                    s.porte(),
                    r.isPresent() ? "SIGNEE" : "ATTENDUE",
                    enSignature && r.isEmpty() && suivantes.contains(s.code()),
                    r.isPresent() ? null : p.titulaire(o, s.signataire()).orElse(null),
                    r.map(SignatureRecueillie::signataireId).orElse(null),
                    r.map(SignatureRecueillie::signataireNom).orElse(null),
                    r.map(SignatureRecueillie::signeLe).orElse(null),
                    r.map(SignatureRecueillie::empreinte).orElse(null)
                );
            })
            .toList();
    }

    /**
     * Actions de l'opération, plus SIGNER quand elle attend une signature que l'acteur peut donner maintenant : il
     * est la personne désignée pour la fonction, ou, sans désignation, il porte la fonction (D-91).
     */
    List<String> actionsAvecSignature(Parcours p, OperationParcours o, Acteur a) {
        List<String> l = new ArrayList<>(actions(p, o, a));
        if (o.getEtat() == EtatOperationParcours.EN_SIGNATURE && a.id() != null) {
            Set<Fonction> fonctions = VisibiliteReferentiel.fonctions(a.permissions());
            boolean peutSigner = o
                .signaturesSuivantes()
                .stream()
                .anyMatch(s -> p.titulaire(o, s.signataire()).map(a::est).orElse(fonctions.contains(s.signataire())));
            if (peutSigner) {
                l.add(0, "SIGNER");
            }
        }
        return l;
    }

    /**
     * Actions de l'opération pour la personne (D-93 §2) : celles que sa matrice de rôles permet, restreintes au RACI
     * imposé : dépôt par le R désigné (porteur de sa fonction s'il est à affecter), un déposant ou le conducteur (jamais la
     * supervision, DDR-05) ; validation et renvoi par le V désigné (porteur de sa fonction s'il est à affecter).
     */
    public List<String> actions(Parcours p, OperationParcours o, Acteur a) {
        Set<Fonction> f = VisibiliteReferentiel.fonctions(a.permissions());
        List<String> l = new ArrayList<>();
        for (String x : actions(o, a)) {
            switch (x) {
                case "DEPOSER_PREUVE", "DEPOSER_LIVRABLE" -> {
                    if (offert(p.droitDepot(o, null, a, f))) {
                        l.add(x);
                    }
                }
                case "VALIDER" -> {
                    if (p.peutValider(o, a, f)) {
                        l.add("VALIDER");
                        l.add("RENVOYER");
                    }
                }
                default -> l.add(x);
            }
        }
        return l;
    }

    /**
     * Dépôt offert à l'acteur (DDR-05) : seulement au titre du RACI (R désigné ou porteur de sa fonction à affecter,
     * déposant, conducteur). La supervision par HOM_STEP_MANAGE reste admise par le service et journalisée, mais n'est
     * jamais offerte comme une action de l'acteur : le geste revient au responsable.
     */
    static boolean offert(java.util.Optional<Parcours.DroitDepot> droit) {
        return droit.isPresent() && droit.orElseThrow() != Parcours.DroitDepot.SUPERVISION;
    }

    /** Actions permises par la matrice des rôles : son geste quand elle est ouverte, la validation (jamais par R), l'attribution des qualités. */
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
                    // D-98 : livrable et preuve ouverts aux mêmes permissions ; le RACI décide ensuite (droitDepot).
                    default -> a.peut(Permissions.STEP_MANAGE, Permissions.EVIDENCE_SUBMIT);
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
                    livrablesEtape(ops),
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

    /** Livrables des opérations d'une étape (D-93 §5) : libellé, opération, code, obligatoire, fourni. */
    static List<Vues.Livrable> livrablesEtape(List<OperationParcours> ops) {
        List<Vues.Livrable> l = new ArrayList<>();
        for (OperationParcours o : ops) {
            for (cm.gimac.des.homologation.domain.parcours.Livrable x : o.getLivrables()) {
                l.add(new Vues.Livrable(null, o.faite() || o.depot(x.code()).isPresent(), x.libelle(), o.getRang(), x.code(), x.obligatoire()));
            }
        }
        return l;
    }

    /**
     * Étapes réelles de la version (D-89, D-93 §5), dans leur ordre, avec l'état et les grandeurs de leurs opérations ;
     * sans étapes connues, les étapes canoniques. Une étape hors parcours (tenue par MS-PARTICIPANT) est close.
     */
    public List<Vues.EtapeVersion> etapesVersion(Parcours p, List<EtapeReferentiel> etapes, LocalDate aujourdhui) {
        List<EtapeReferentiel> l = etapes;
        List<Vues.EtapeVersion> r = new ArrayList<>();
        if (l == null || l.isEmpty()) {
            for (EtapeCode e : EtapeCode.values()) {
                r.add(etapeVersion(p, e.name(), e.libelle(), e.ordinal() + 1, e.couleur(), e, aujourdhui));
            }
            return r;
        }
        for (EtapeReferentiel e : l) {
            r.add(etapeVersion(p, e.getCode(), e.getLibelle(), e.getOrdre(), e.getCouleur(), e.getCanonique(), aujourdhui));
        }
        return r;
    }

    private Vues.EtapeVersion etapeVersion(Parcours p, String code, String libelle, int ordre, cm.gimac.des.homologation.domain.parcours.CouleurEtape couleur, EtapeCode canonique, LocalDate aujourdhui) {
        List<OperationParcours> ops = p.operations().stream().filter(o -> code.equals(o.getEtapeCode())).toList();
        long faites = ops.stream().filter(OperationParcours::faite).count();
        boolean close = !ops.isEmpty() && faites == ops.size();
        boolean enCours = !close && (faites > 0 || ops.stream().anyMatch(o -> o.getEtat().enCours()));
        boolean bloquee = ops.stream().anyMatch(o -> o.getEtat() == EtatOperationParcours.BLOQUEE);
        String etat;
        if (ops.isEmpty()) {
            etat = canonique != null && canonique.horsParcours() ? "CLOSE" : "SANS_OBJET";
        } else {
            etat = close ? "CLOSE" : bloquee ? "BLOQUEE" : enCours ? "EN_COURS" : "A_VENIR";
        }
        OperationParcours premier = ops.isEmpty() ? null : ops.get(0);
        OperationParcours dernier = ops.isEmpty() ? null : ops.get(ops.size() - 1);
        Sla sla = close
            ? null
            : ops.stream().anyMatch(o -> Parcours.sla(o, aujourdhui) == Sla.DEPASSE)
                ? Sla.DEPASSE
                : ops.stream().anyMatch(o -> Parcours.sla(o, aujourdhui) == Sla.ECHEANCE_PROCHE) ? Sla.ECHEANCE_PROCHE : enCours ? Sla.DANS_LES_DELAIS : null;
        return new Vues.EtapeVersion(
            code,
            libelle,
            ordre,
            couleur,
            couleur == null ? null : couleur.jeton(),
            canonique,
            etat,
            (int) faites,
            ops.size(),
            premier == null ? null : premier.getRaci().r(),
            premier == null ? null : premier.getRaci().v(),
            premier == null ? null : premier.getFaiteLe() != null ? premier.getFaiteLe() : enCours ? premier.getEcheance() : null,
            close ? dernier.getFaiteLe() : null,
            dernier == null ? null : dernier.getEcheance(),
            sla,
            livrablesEtape(ops)
        );
    }

    /**
     * Intervenants du dossier lus dans le RACI (D-93 §5) : chaque fonction tenue en R, V, C, I ou E sur une opération,
     * dans l'ordre du référentiel, avec la personne désignée (première désignée sur une opération non faite, sinon
     * celle qui tient la fonction sur la liaison) ou « à affecter ».
     */
    public List<Vues.IntervenantDossier> intervenants(Parcours p) {
        Map<Fonction, Set<cm.gimac.des.homologation.domain.parcours.Qualite>> qualites = new LinkedHashMap<>();
        Map<Fonction, Personne> personnes = new LinkedHashMap<>();
        for (OperationParcours o : p.operations()) {
            var raci = o.getRaci();
            noter(qualites, personnes, cm.gimac.des.homologation.domain.parcours.Qualite.R, raci.r(), o);
            noter(qualites, personnes, cm.gimac.des.homologation.domain.parcours.Qualite.V, raci.v(), o);
            raci.c().forEach(x -> noter(qualites, personnes, cm.gimac.des.homologation.domain.parcours.Qualite.C, x, o));
            raci.i().forEach(x -> noter(qualites, personnes, cm.gimac.des.homologation.domain.parcours.Qualite.I, x, o));
            raci.e().forEach(x -> noter(qualites, personnes, cm.gimac.des.homologation.domain.parcours.Qualite.E, x, o));
        }
        List<Vues.IntervenantDossier> l = new ArrayList<>();
        qualites.forEach((f, q) -> {
            Personne x = personnes.get(f);
            if (x == null) {
                x = p.tenant(f).orElse(null);
            }
            l.add(new Vues.IntervenantDossier(f, f.libelle(), List.copyOf(q), x, x == null));
        });
        return l;
    }

    private static void noter(
        Map<Fonction, Set<cm.gimac.des.homologation.domain.parcours.Qualite>> qualites,
        Map<Fonction, Personne> personnes,
        cm.gimac.des.homologation.domain.parcours.Qualite q,
        Personne x,
        OperationParcours o
    ) {
        if (x == null || x.fonction() == null) {
            return;
        }
        qualites.computeIfAbsent(x.fonction(), k -> new java.util.TreeSet<>()).add(q);
        if (x.affectee() && !o.faite()) {
            personnes.putIfAbsent(x.fonction(), x);
        }
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
        boolean mobile = p.liaison().modeleTypeProjet().avancementSimulateur();
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
            l.libelleTypeProjet(),
            ctx.banquePartenaireNom(),
            null,
            p.etat().getStatut(),
            p.etat().getEtapeCourante(),
            conducteur ? ctx.conducteur() : null,
            // Responsables fonctionnel et technique figés (DDRM, SNA) supprimés (D-93 §5) : lire « intervenants ».
            null,
            null,
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
            actions,
            etapesVersion(p, ctx.etapesVersion(), aujourdhui),
            intervenants(p),
            l.getIntituleProjet(),
            l.getReferenceOrigine()
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
            d.typeProjetLibelle(),
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
            List.of(),
            d.etapesVersion()
                .stream()
                .map(e ->
                    new Vues.EtapeVersion(e.code(), e.libelle(), e.ordre(), e.couleur(), e.jeton(), e.canonique(), e.etat(), e.actionsFaites(), e.actionsTotal(), parFonction(e.responsable()), parFonction(e.validateur()), e.debut(), e.closeLe(), e.echeance(), e.sla(), e.livrables())
                )
                .toList(),
            d.intervenants().stream().map(i -> new Vues.IntervenantDossier(i.fonction(), i.libelle(), i.qualites(), parFonction(i.personne()), i.aAffecter())).toList(),
            // D-104 : l'intitulé distingue les projets du participant ; la référence d'origine (reprise) reste interne.
            d.intituleProjet(),
            null
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
            l.libelleTypeProjet(),
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
            conducteurNom,
            l.modeleTypeProjet(),
            l.modeleTypeProjet().attestationsExigees(p.etat().isFintech())
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
