package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.CatalogueServices;
import cm.gimac.des.homologation.domain.parcours.DefinitionCompteur;
import cm.gimac.des.homologation.domain.parcours.Personne;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.EtatApprobation;
import cm.gimac.des.homologation.domain.parcours.EtatFicheSuivi;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.EtatProcesVerbal;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.Qualite;
import cm.gimac.des.homologation.domain.parcours.SegmentParcours;
import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import cm.gimac.des.homologation.domain.parcours.Sla;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.parcours.Vues;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Tableaux de bord par fonction (D-58, CR §3.6, vision diapositive 14) et pilotage (CR §7.3, annexe B) :
 * compteurs, tâches, liaisons et blocages visibles, calculés par le service ; le participant ne voit que
 * les liaisons de son institution. Les compteurs désignent les opérations par leur modèle (étape, geste, jalon,
 * déclencheur, particularité, fonction responsable), jamais par leur rang ; ils sont décrits en données (D-94 §3,
 * {@link DefinitionCompteur}). Service de domaine pur.
 */
public class TableauDeBordCalculateur {

    private final ParcoursProjection projection;
    private final List<DefinitionCompteur> compteurs;

    /** Compteurs livrés avec le service ({@link DefinitionCompteur#parDefaut()}). */
    public TableauDeBordCalculateur(ParcoursProjection projection) {
        this(projection, DefinitionCompteur.parDefaut());
    }

    /** Compteurs décrits en données (D-94 §3), lus par la configuration. */
    public TableauDeBordCalculateur(ParcoursProjection projection, List<DefinitionCompteur> compteurs) {
        this.projection = projection;
        this.compteurs = List.copyOf(compteurs);
    }

    public List<DefinitionCompteur> compteurs() {
        return compteurs;
    }

    /**
     * Tableau de bord d'un acteur pour ses fonctions. {@code institutionPortail} cloisonne la vue du
     * participant (RG-59) ; {@code conducteurs} donne le nom affiché du conducteur de chaque liaison. Les compteurs sont
     * ceux des définitions dont une fonction est retenue (ou sans fonction) ; les tâches sont celles de la personne
     * (D-93 §5), pas de sa fonction.
     */
    public Vues.TableauActeur tableau(List<Parcours> tous, List<Fonction> fonctions, Acteur acteur, String institutionPortail, Map<UUID, String> conducteurs, LocalDate aujourdhui) {
        Set<Fonction> f = fonctions.isEmpty() ? Set.of() : Set.copyOf(fonctions);
        boolean participant = f.contains(Fonction.PARTICIPANT);
        List<Parcours> actives = tous.stream().filter(p -> p.etat().getStatut() != StatutParcours.CLOSE_SANS_SUITE).toList();
        List<Parcours> visibles = participant ? actives.stream().filter(p -> p.liaison().getInstitutionId().toString().equalsIgnoreCase(institutionPortail)).toList() : actives;
        List<Vues.Tache> taches = participant ? tachesParticipant(visibles, aujourdhui) : taches(visibles, acteur, aujourdhui);
        Predicate<Parcours> miens = p -> acteur.id() != null && acteur.id().equals(p.liaison().getChefDeProjetId()) && !p.etat().isConducteurADesigner();
        List<Vues.Compteur> c = new ArrayList<>();
        Set<String> vus = new java.util.HashSet<>();
        for (DefinitionCompteur d : compteurs) {
            boolean retenu = d.fonctions().isEmpty() || d.fonctions().stream().anyMatch(x -> f.contains(Fonction.valueOf(x)));
            if (retenu && vus.add(d.cle() + "|" + String.join(",", d.fonctions()))) {
                c.add(evaluer(d, visibles, taches, miens, acteur));
            }
        }
        List<Parcours> liaisons = participant ? visibles : f.equals(Set.of(Fonction.CONDUCTEUR)) ? actives.stream().filter(miens).toList() : actives;
        List<Vues.Blocage> blocages = visibles
            .stream()
            .flatMap(p -> p.blocages().stream().filter(b -> b.actif()))
            .map(b -> projection.blocage(b, participant ? new Acteur(null, null, Set.of(), null) : acteur))
            .map(b -> participant ? new Vues.Blocage(b.id(), b.liaisonId(), b.motif(), b.depuis(), b.prochaineAction(), b.responsable().parFonction(), b.echeanceLevee(), b.leveLe(), b.etape(), List.of(), List.of()) : b)
            .toList();
        return new Vues.TableauActeur(
            fonctions.isEmpty() ? Fonction.DDRM : fonctions.get(0),
            c,
            taches,
            liaisons.stream().map(p -> resumeVisible(p, participant, conducteurs, aujourdhui)).toList(),
            blocages
        );
    }

    /** Valeur et lien d'un compteur décrit en données. */
    Vues.Compteur evaluer(DefinitionCompteur d, List<Parcours> visibles, List<Vues.Tache> taches, Predicate<Parcours> miens, Acteur acteur) {
        String fonction = d.fonctions().isEmpty() ? null : String.join(",", d.fonctions());
        String portee = d.portee() == null ? "LIAISON" : d.portee();
        long valeur;
        String lien = d.lien();
        switch (portee) {
            case "TACHE" -> {
                List<Vues.Tache> l = taches.stream().filter(t -> d.sla() == null || t.sla() == d.sla()).toList();
                valeur = l.size();
                if ("TACHE".equals(d.lienUnique()) && l.size() == 1) {
                    lien = l.get(0).lien();
                }
            }
            case "OPERATION" -> {
                List<OpDe> l = ops(visibles.stream().filter(p -> liaisonRetenue(d.liaison(), p, miens)).toList(), (p, o) -> operationRetenue(d.operation(), p, o));
                valeur = l.size();
                if ("OPERATION".equals(d.lienUnique()) && l.size() == 1) {
                    lien = ParcoursProjection.lienOperation(l.get(0).p(), l.get(0).o());
                }
            }
            default -> {
                List<Parcours> l = visibles
                    .stream()
                    .filter(p -> liaisonRetenue(d.liaison(), p, miens))
                    .filter(p -> d.operation() == null || p.operations().stream().anyMatch(o -> operationRetenue(d.operation(), p, o)))
                    .toList();
                valeur = switch (d.mesure() == null ? "NOMBRE" : d.mesure()) {
                    case "ATTESTATIONS_DEPOSEES" -> l.stream().mapToLong(p -> p.etat().getAttestationsDeposees().size()).sum();
                    case "ATTESTATIONS_A_FOURNIR" -> l.stream().mapToLong(p -> p.attestationsAFournir().size()).sum();
                    default -> l.size();
                };
                if (d.lienUnique() != null && d.lienUnique().startsWith("DOSSIER:") && l.size() == 1) {
                    lien = "/des/parcours/" + l.get(0).id() + "?onglet=" + d.lienUnique().substring("DOSSIER:".length());
                }
            }
        }
        return new Vues.Compteur(d.cle(), valeur, valeur == 0 ? "ok" : d.ton(), lien, d.libelle(), fonction);
    }

    static boolean liaisonRetenue(DefinitionCompteur.FiltreLiaison f, Parcours p, Predicate<Parcours> miens) {
        if (f == null) {
            return true;
        }
        var e = p.etat();
        return (
            dans(f.etapesCourantes(), e.getEtapeCourante()) &&
            (f.conducteurADesigner() == null || f.conducteurADesigner() == e.isConducteurADesigner()) &&
            (f.mesProjets() == null || f.mesProjets() == miens.test(p)) &&
            dans(f.etatsPv(), e.getEtatPv()) &&
            dans(f.etatsApprobation(), e.getEtatApprobation()) &&
            dans(f.etatsFiche(), e.getFicheEtat()) &&
            (f.fintech() == null || f.fintech() == e.isFintech()) &&
            (f.visaAttendu() == null || e.getVisasAttendus().contains(f.visaAttendu()))
        );
    }

    static boolean operationRetenue(DefinitionCompteur.FiltreOperation f, Parcours p, OperationParcours o) {
        if (f == null) {
            return true;
        }
        boolean engagee = !o.faite() && o.getEtat() != EtatOperationParcours.VERROUILLEE;
        return (
            dans(f.etats(), o.getEtat()) &&
            (f.engagee() == null || f.engagee() == engagee) &&
            dans(f.etapes(), o.getEtape()) &&
            dans(f.etapesVersion(), o.getEtapeCode()) &&
            dans(f.gestes(), o.getGeste()) &&
            dans(f.jalons(), o.getJalon()) &&
            dans(f.declencheurs(), o.getDeclencheur()) &&
            dans(f.particularites(), o.getParticularite()) &&
            dans(f.segments(), o.getSegment()) &&
            dans(f.responsables(), o.getResponsable()) &&
            dans(f.fonctionsR(), o.fonctionR()) &&
            (f.cle() == null || f.cle() == o.isCle()) &&
            (f.oubliSansSymptome() == null || f.oubliSansSymptome() == o.isOubliSansSymptome()) &&
            (f.premiereDeLEtape() == null || f.premiereDeLEtape() == p.premiereDeLEtape(o.getEtape()).map(x -> x == o).orElse(false)) &&
            dans(f.vues(), o.getVue())
        );
    }

    private static <T> boolean dans(List<T> admis, T valeur) {
        return admis == null || admis.isEmpty() || admis.contains(valeur);
    }

    private Vues.LiaisonResume resumeVisible(Parcours p, boolean participant, Map<UUID, String> conducteurs, LocalDate aujourdhui) {
        String nom = participant ? "Conducteur de projet GIMAC" : conducteurs.get(p.liaison().getChefDeProjetId());
        return projection.resume(p, nom, aujourdhui);
    }

    /**
     * « Mes tâches » de la personne (D-93 §5) : opérations en cours dont elle est le R désigné (ou, R à affecter, porteuse
     * de sa fonction : tâche à prendre), le V désigné en validation (même règle), un consulté désigné quand l'opération
     * sollicite les consultés, un signataire attendu ; conducteur : les opérations d'une fonction externe sans
     * institution liée.
     */
    List<Vues.Tache> taches(List<Parcours> visibles, Acteur acteur, LocalDate aujourdhui) {
        List<Vues.Tache> l = new ArrayList<>();
        if (acteur.id() == null) {
            return l;
        }
        Set<Fonction> f = VisibiliteReferentiel.fonctions(acteur.permissions());
        for (Parcours p : visibles) {
            for (OperationParcours o : p.operations()) {
                Part x = part(p, o, acteur, f);
                if (x == null) {
                    continue;
                }
                l.add(
                    new Vues.Tache(
                        o.getId().toString(),
                        p.id().toString(),
                        p.etat().getReference(),
                        p.liaison().getInstitutionNom(),
                        o.getRang(),
                        o.getEtape(),
                        x.qualite(),
                        o.getEcheance(),
                        Parcours.sla(o, aujourdhui),
                        o.isOubliSansSymptome(),
                        ParcoursProjection.lienOperation(p, o),
                        projection.actionsAvecSignature(p, o, acteur),
                        o.getLibelle(),
                        x.aPrendre(),
                        x.signature()
                    )
                );
            }
        }
        l.sort(Comparator.comparing(t -> t.echeance() == null ? "9" : t.echeance().toString()));
        return l;
    }

    /** Part de la personne sur une opération en cours : qualité (R, V, C), tâche à prendre, signature attendue. */
    record Part(Qualite qualite, Boolean aPrendre, String signature) {}

    /**
     * Ce que la personne doit faire sur l'opération (D-93 §5) : R désigné (ou, R à affecter, porteuse de sa fonction :
     * à prendre), V désigné en validation (même règle), consulté désigné quand l'opération sollicite les consultés,
     * signataire attendu ; conducteur : une opération d'une fonction externe sans institution liée. Rien sinon.
     */
    Part part(Parcours p, OperationParcours o, Acteur acteur, Set<Fonction> f) {
        if (acteur.id() == null || !o.getEtat().enCours()) {
            return null;
        }
        var raci = o.getRaci();
        Personne r = raci.r();
        Qualite q = null;
        Boolean aPrendre = null;
        String signature = null;
        boolean actif = o.getEtat() != EtatOperationParcours.EN_VALIDATION && o.getEtat() != EtatOperationParcours.EN_SIGNATURE;
        if (actif && r != null) {
            if (acteur.est(r)) {
                q = Qualite.R;
            } else if (!r.affectee() && !r.fonction().externe() && f.contains(r.fonction())) {
                q = Qualite.R;
                aPrendre = Boolean.TRUE;
            } else if (!r.affectee() && r.fonction().externe() && p.conduitePar(acteur)) {
                q = Qualite.R;
            }
        }
        if (q == null && o.getEtat() == EtatOperationParcours.EN_VALIDATION && p.peutValider(o, acteur, f)) {
            q = Qualite.V;
            aPrendre = raci.v() != null && !raci.v().affectee() ? Boolean.TRUE : null;
        }
        if (q == null && o.isConsultesRecoiventTache() && o.getEtat() != EtatOperationParcours.EN_SIGNATURE && raci.c().stream().anyMatch(acteur::est)) {
            q = Qualite.C;
        }
        if (q == null && o.getEtat() == EtatOperationParcours.EN_SIGNATURE) {
            signature = o
                .signaturesSuivantes()
                .stream()
                .filter(s -> p.titulaire(o, s.signataire()).map(acteur::est).orElse(f.contains(s.signataire())))
                .map(cm.gimac.des.homologation.domain.parcours.SignatureOperation::code)
                .findFirst()
                .orElse(null);
        }
        return q == null && signature == null ? null : new Part(q, aPrendre, signature);
    }

    /**
     * « Mes actions de certification » (D-98 §5) : sur les liaisons actives, les opérations en cours que la personne
     * doit faire (même règle que « mes tâches », sans exiger qu'elle porte une fonction), celles qu'elle suit sur les
     * liaisons qu'elle conduit ou sur lesquelles elle intervient, et l'état de ces liaisons et de leurs jalons. Une
     * opération figure une seule fois, à faire de préférence.
     */
    public Vues.MesOperations mesOperations(List<Parcours> tous, Acteur acteur, LocalDate aujourdhui) {
        List<Vues.MonOperation> aAgir = new ArrayList<>();
        List<Vues.MonOperation> aSuivre = new ArrayList<>();
        List<Vues.LiaisonSuivie> liaisons = new ArrayList<>();
        if (acteur.id() == null) {
            return new Vues.MesOperations(aAgir, aSuivre, liaisons);
        }
        Set<Fonction> f = VisibiliteReferentiel.fonctions(acteur.permissions());
        for (Parcours p : tous) {
            if (p.etat().getStatut() == StatutParcours.CLOSE_SANS_SUITE) {
                continue;
            }
            boolean conduit = p.conduitePar(acteur);
            boolean implique = conduit || p.implique(acteur.id().toString());
            boolean agit = false;
            for (OperationParcours o : p.operations()) {
                Part x = part(p, o, acteur, f);
                if (x != null) {
                    aAgir.add(monOperation(p, o, x, acteur, aujourdhui));
                    agit = true;
                } else if (implique && o.getEtat().enCours()) {
                    aSuivre.add(monOperation(p, o, null, acteur, aujourdhui));
                }
            }
            if (implique || agit) {
                liaisons.add(suivie(p, conduit));
            }
        }
        aAgir.sort(Comparator.comparing((Vues.MonOperation m) -> m.echeance() == null ? "9" : m.echeance().toString()).thenComparing(Vues.MonOperation::liaisonReference));
        aSuivre.sort(Comparator.comparing(Vues.MonOperation::liaisonReference).thenComparing(m -> m.echeance() == null ? "9" : m.echeance().toString()));
        liaisons.sort(Comparator.comparing(Vues.LiaisonSuivie::reference));
        return new Vues.MesOperations(aAgir, aSuivre, liaisons);
    }

    private Vues.MonOperation monOperation(Parcours p, OperationParcours o, Part x, Acteur acteur, LocalDate aujourdhui) {
        var raci = o.getRaci();
        return new Vues.MonOperation(
            o.getId().toString(),
            p.id().toString(),
            p.etat().getReference(),
            p.liaison().getInstitutionNom(),
            p.etat().getStatut(),
            o.getRang(),
            o.getLibelle(),
            o.getEtape(),
            o.getEtapeCode(),
            o.getEtat(),
            x == null ? null : x.qualite(),
            x == null ? null : x.aPrendre(),
            x == null ? null : x.signature(),
            o.getGeste(),
            o.getJalon(),
            o.getDeclencheur(),
            o.getVue(),
            raci.r(),
            raci.v(),
            o.getEcheance(),
            Parcours.sla(o, aujourdhui),
            ParcoursProjection.lienOperation(p, o),
            projection.actionsAvecSignature(p, o, acteur)
        );
    }

    private static Vues.LiaisonSuivie suivie(Parcours p, boolean conduit) {
        List<Vues.JalonSuivi> jalons = p
            .operations()
            .stream()
            .filter(o -> o.getJalon() != null && o.getEtat() != EtatOperationParcours.SANS_OBJET)
            .map(o -> new Vues.JalonSuivi(o.getJalon(), o.faite(), o.faite() ? o.getFaiteLe() : null, o.getId().toString()))
            .toList();
        long faites = p.operations().stream().filter(OperationParcours::faite).count();
        return new Vues.LiaisonSuivie(
            p.id().toString(),
            p.etat().getReference(),
            p.liaison().getInstitutionNom(),
            p.liaison().getTypeProjet(),
            p.etat().getStatut(),
            p.etat().getEtapeCourante(),
            conduit,
            faites,
            p.operations().size(),
            jalons,
            "/des/parcours/" + p.id()
        );
    }

    /** Tâches du participant (portail) : opérations en cours dont il est responsable (R) ou partie externe (E). */
    List<Vues.Tache> tachesParticipant(List<Parcours> visibles, LocalDate aujourdhui) {
        List<Vues.Tache> l = new ArrayList<>();
        for (Parcours p : visibles) {
            for (OperationParcours o : p.operations()) {
                if (!o.getEtat().enCours()) {
                    continue;
                }
                var raci = o.getRaci();
                Fonction r = o.fonctionR();
                boolean estR = r == Fonction.PARTICIPANT;
                boolean estE = estR || raci.e().stream().anyMatch(x -> x.fonction() == Fonction.PARTICIPANT);
                if (!estE) {
                    continue;
                }
                var g = projection.gesteParticipant(p, o);
                l.add(
                    new Vues.Tache(
                        o.getId().toString(),
                        p.id().toString(),
                        p.etat().getReference(),
                        p.liaison().getInstitutionNom(),
                        o.getRang(),
                        o.getEtape(),
                        estR ? Qualite.R : Qualite.E,
                        o.getEcheance(),
                        Parcours.sla(o, aujourdhui),
                        o.isOubliSansSymptome(),
                        g.lien(),
                        g.actionsPermises(),
                        o.getLibelle(),
                        null,
                        null
                    )
                );
            }
        }
        l.sort(Comparator.comparing(t -> t.echeance() == null ? "9" : t.echeance().toString()));
        return l;
    }

    private record OpDe(Parcours p, OperationParcours o) {}

    private static List<OpDe> ops(List<Parcours> actives, BiPredicate<Parcours, OperationParcours> filtre) {
        return actives.stream().flatMap(p -> p.operations().stream().filter(o -> filtre.test(p, o)).map(o -> new OpDe(p, o))).toList();
    }

    // ------------------------------------------------------------------ pilotage

    /** Pilotage (CR §7.3, D-59) : quatre compteurs, répartition par étape, délais et les six questions de l'annexe B. */
    public Vues.Pilotage pilotage(List<Parcours> tous, LocalDate aujourdhui) {
        List<Parcours> actives = tous.stream().filter(p -> p.etat().getStatut() != StatutParcours.CLOSE_SANS_SUITE).toList();
        Map<String, Long> demandes = new LinkedHashMap<>();
        actives.forEach(p -> p.servicesInclus().forEach(s -> demandes.merge(s.getServiceCode(), 1L, Long::sum)));
        List<Map.Entry<String, Long>> tri = demandes.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).toList();
        Map<String, Long> lancements = new LinkedHashMap<>();
        actives
            .stream()
            .filter(p -> !p.etat().getDateDemarrage().isBefore(aujourdhui.minusMonths(12)))
            .forEach(p -> p.servicesInclus().forEach(s -> lancements.merge(s.getServiceCode(), 1L, Long::sum)));
        List<Vues.RepartitionEtape> repartition = new ArrayList<>();
        for (EtapeCode e : EtapeCode.values()) {
            if (e != EtapeCode.PRISE_CONTACT && e != EtapeCode.ADHESION) {
                repartition.add(new Vues.RepartitionEtape(e, actives.stream().filter(p -> p.etat().getEtapeCourante() == e).count()));
            }
        }
        List<Vues.ProjetBloque> bloques = actives
            .stream()
            .flatMap(p ->
                p.blocages().stream().filter(b -> b.actif()).map(b -> new Vues.ProjetBloque(p.id().toString(), p.etat().getReference(), p.liaison().getInstitutionNom(), b.getMotif(), b.getDepuis(), ChronoUnit.DAYS.between(b.getDepuis(), aujourdhui)))
            )
            .toList();
        return new Vues.Pilotage(
            actives.stream().filter(p -> p.etat().getStatut() != StatutParcours.EN_PRODUCTION).count(),
            actives.stream().filter(Parcours::bloquee).count(),
            actives.stream().mapToLong(p -> p.tachesEnRetard(aujourdhui)).sum(),
            actives.stream().flatMap(p -> p.operations().stream()).filter(o -> Parcours.sla(o, aujourdhui) == Sla.ECHEANCE_PROCHE).count(),
            repartition,
            moyenne(actives.stream().map(p -> delai(p.etat().getDateDemarrage(), p.parJalon(Jalon.CERTIFIEE).map(OperationParcours::getFaiteLe).orElse(null))).toList()),
            tauxRespect(actives),
            new Vues.Questions(
                actives.stream().filter(p -> Set.of(EtapeCode.INTEGRATION, EtapeCode.TESTS, EtapeCode.CERTIFICATION).contains(p.etat().getEtapeCourante())).count(),
                tri.stream().limit(4).map(e -> new Vues.ServiceNombre(e.getKey(), CatalogueServices.libelle(e.getKey()), e.getValue())).toList(),
                servicesPlusLongs(actives),
                moyenne(actives.stream().map(p -> delai(p.etat().getDateDemarrage(), p.parJalon(Jalon.EN_PRODUCTION).map(OperationParcours::getFaiteLe).orElse(null))).toList()),
                bloques,
                lancements.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).map(e -> new Vues.ServiceNombre(e.getKey(), CatalogueServices.libelle(e.getKey()), e.getValue())).toList()
            )
        );
    }

    private static Long delai(LocalDate debut, LocalDate fin) {
        return debut == null || fin == null ? null : ChronoUnit.DAYS.between(debut, fin);
    }

    private static Long moyenne(Collection<Long> valeurs) {
        OptionalDouble m = valeurs.stream().filter(Objects::nonNull).mapToLong(Long::longValue).average();
        return m.isPresent() ? Math.round(m.orElseThrow()) : null;
    }

    /** Part des opérations faites à leur échéance ou avant. */
    private static Integer tauxRespect(List<Parcours> actives) {
        List<OperationParcours> faites = actives.stream().flatMap(p -> p.operations().stream()).filter(o -> o.faite() && o.getFaiteLe() != null && o.getEcheance() != null).toList();
        if (faites.isEmpty()) {
            return null;
        }
        long aTemps = faites.stream().filter(o -> !o.getFaiteLe().isAfter(o.getEcheance())).count();
        return (int) Math.round(aTemps * 100.0 / faites.size());
    }

    /** Services dont les essais durent le plus : délai moyen de l'entrée en essai à la certification. */
    private static List<Vues.ServiceDelai> servicesPlusLongs(List<Parcours> actives) {
        Map<String, List<Long>> delais = new LinkedHashMap<>();
        for (Parcours p : actives) {
            for (ServiceParcours s : p.servicesInclus()) {
                Long d = delai(s.getEntreeEssai(), s.getCertifieLe());
                if (d != null) {
                    delais.computeIfAbsent(s.getServiceCode(), k -> new ArrayList<>()).add(d);
                }
            }
        }
        return delais
            .entrySet()
            .stream()
            .map(e -> new Vues.ServiceDelai(e.getKey(), CatalogueServices.libelle(e.getKey()), moyenne(e.getValue())))
            .sorted(Comparator.comparingLong(Vues.ServiceDelai::delaiMoyenJours).reversed())
            .limit(3)
            .toList();
    }
}
