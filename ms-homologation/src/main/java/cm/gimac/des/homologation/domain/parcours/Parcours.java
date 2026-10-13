package cm.gimac.des.homologation.domain.parcours;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Agrégat du parcours d'une liaison, étapes 3 à 10 (docs/lot1/parcours-backend.md) : opérations
 * instanciées de la version publiée du référentiel de la liaison (moteur unique, D-77), graphe des
 * prédécesseurs, gestes (livrable, preuve,
 * validation, qualités, ressources, blocages, constat de la couverture, coordonnées d'essai, fiche de
 * suivi), projection des verrous tenus par MS-CERTIFICATION, et grandeurs calculées (étape courante,
 * statut du CR, SLA, couvertures). Les gestes, jalons, déclencheurs, particularités et livrables sont lus
 * sur chaque opération : aucun rang n'est codé en dur. Sans Spring.
 */
public final class Parcours {

    /** Une échéance est proche à cinq jours ou moins (gimac.homologation.deadlines.warning-days). */
    public static final int JOURS_ECHEANCE_PROCHE = 5;

    static final Pattern CODE_CONTROLE = Pattern.compile("^[0-9A-F]{4,8}$");
    static final Pattern HOTE = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?$");
    static final Pattern PORT = Pattern.compile("^\\d{2,5}$");

    /** Désigne la personne qui tient une fonction lors de l'instanciation (annuaire, conducteur, correspondant). */
    @FunctionalInterface
    public interface Designation {
        Personne pour(Fonction f);
    }

    /** Ligne de la fiche de suivi renseignée par le participant. */
    public record LigneFiche(String serviceCode, Boolean ouvert, String commentaire) {}

    private final Liaison liaison;
    private final ParcoursLiaison etat;
    private final List<OperationParcours> operations;
    private final List<ServiceParcours> services;
    private final List<BlocageParcours> blocages;
    /** Taux de couverture repris du simulateur par service (table coverage, HOM-12) ; vide si aucune reprise. */
    private Map<String, Integer> mesures = Map.of();

    public Parcours(Liaison liaison, ParcoursLiaison etat, List<OperationParcours> operations, List<ServiceParcours> services, List<BlocageParcours> blocages) {
        this.liaison = Objects.requireNonNull(liaison);
        this.etat = Objects.requireNonNull(etat);
        this.operations = new ArrayList<>(operations);
        this.operations.sort(Comparator.comparingInt(OperationParcours::getOrdre));
        this.services = new ArrayList<>(services);
        this.services.sort(Comparator.comparing(ServiceParcours::getServiceCode));
        this.blocages = new ArrayList<>(blocages);
        this.blocages.sort(Comparator.comparing(BlocageParcours::getDepuis));
    }

    /**
     * Instancie le parcours à l'ouverture de la liaison depuis les opérations de la version publiée de son
     * référentiel (les étapes 1 et 2, tenues par MS-PARTICIPANT, ne sont pas instanciées) : échéances au plus tôt
     * sur le graphe depuis le démarrage, qualités initiales par fonction (R, V, C, I, E de l'opération),
     * opérations sans prédécesseur en attente ouvertes.
     */
    public static Parcours instancier(
        Liaison liaison,
        ParcoursLiaison etat,
        List<ReferenceOperation> referentiel,
        List<ServiceParcours> services,
        Designation designation
    ) {
        List<ReferenceOperation> suite = referentiel
            .stream()
            .filter(o -> !o.etapeEffective().horsParcours())
            .sorted(Comparator.comparingInt(ReferenceOperation::ordreEffectif).thenComparing(ReferenceOperation::getCode))
            .toList();
        List<OperationParcours> ops = new ArrayList<>();
        for (int i = 0; i < suite.size(); i++) {
            ReferenceOperation o = suite.get(i);
            OperationParcours op = new OperationParcours(liaison.getId(), o, i);
            op.setRaci(raciInitial(o, designation));
            ops.add(op);
        }
        Parcours p = new Parcours(liaison, etat, ops, services, List.of());
        p.planifier();
        p.ouvrirSuivantes(etat.getDateDemarrage());
        p.rafraichir();
        return p;
    }

    /** Qualités initiales de l'opération : chaque fonction (R, V, C, I, E) tenue par la personne désignée. */
    static Raci raciInitial(ReferenceOperation o, Designation d) {
        return new Raci(
            d.pour(o.responsableEffectif()),
            o.getConsultes().stream().map(d::pour).toList(),
            d.pour(o.validateurEffectif()),
            o.informesEffectifs().stream().map(d::pour).toList(),
            o.getExternes().stream().map(d::pour).toList()
        );
    }

    /**
     * Échéances au plus tôt sur le graphe (docs/lot1/modelisateur-processus.md §3) : maximum des échéances des
     * prédécesseurs, plus la durée de l'opération, depuis la date de démarrage ; jours calendaires.
     */
    public void planifier() {
        Map<String, CalendrierDag.Tache> cible = new LinkedHashMap<>();
        Map<String, CalendrierDag.Tache> max = new LinkedHashMap<>();
        operations.forEach(o -> {
            cible.put(o.getRang(), new CalendrierDag.Tache(o.getPredecesseurs(), o.getDelaiJours()));
            max.put(o.getRang(), new CalendrierDag.Tache(o.getPredecesseurs(), o.getDelaiMaxJours()));
        });
        CalendrierDag.Resultat rc = CalendrierDag.calculer(cible);
        CalendrierDag.Resultat rm = CalendrierDag.calculer(max);
        for (OperationParcours o : operations) {
            Integer finCible = rc.fins().get(o.getRang());
            Integer finMax = rm.fins().get(o.getRang());
            if (finMax != null && !o.faite()) {
                o.setEcheance(etat.getDateDemarrage().plusDays(finMax));
                o.setEcheanceCible(etat.getDateDemarrage().plusDays(finCible == null ? finMax : finCible));
            }
        }
    }

    /** Différentiel appliqué par une migration : opérations ajoutées, supprimées, mises sans objet, modifiées. */
    public record Differentiel(List<OperationParcours> ajoutees, List<OperationParcours> supprimees, int sansObjet, int modifiees) {}

    /**
     * Migration vers une nouvelle version publiée du référentiel (docs/lot1/modelisateur-processus.md §3) :
     * opérations ajoutées verrouillées puis éligibilité recalculée ; retirées supprimées si elles ne sont pas
     * commencées, sinon conservées sans objet (une opération faite garde sa preuve et son état) ; modifiées et
     * non faites : modèle (libellé, geste, jalon, déclencheur, livrables, délai, prédécesseurs…) et qualités mis à
     * jour, les personnes déjà désignées restant en place quand les fonctions ne changent pas. Les échéances au
     * plus tôt des opérations non faites sont recalculées.
     */
    public Differentiel migrer(UUID versionId, Integer numero, List<ReferenceOperation> cible, Designation designation, LocalDate aujourdhui) {
        List<ReferenceOperation> refs = cible
            .stream()
            .filter(o -> !o.etapeEffective().horsParcours())
            .sorted(Comparator.comparingInt(ReferenceOperation::ordreEffectif).thenComparing(ReferenceOperation::getCode))
            .toList();
        Map<String, ReferenceOperation> parCode = new LinkedHashMap<>();
        refs.forEach(r -> parCode.put(r.getCode(), r));
        List<OperationParcours> supprimees = new ArrayList<>();
        int sansObjet = 0;
        int modifiees = 0;
        for (OperationParcours o : List.copyOf(operations)) {
            ReferenceOperation ref = parCode.get(o.getRang());
            if (ref == null) {
                if (!o.faite() && o.getPreuve() == null && Set.of(EtatOperationParcours.VERROUILLEE, EtatOperationParcours.A_FAIRE, EtatOperationParcours.BLOQUEE).contains(o.getEtat())) {
                    supprimees.add(o);
                    operations.remove(o);
                    continue;
                }
                if (!o.faite() && o.getEtat() != EtatOperationParcours.SANS_OBJET) {
                    o.setEtat(EtatOperationParcours.SANS_OBJET);
                    sansObjet++;
                }
                o.setVersionId(versionId);
            } else if (o.faite()) {
                o.setVersionId(versionId);
            } else {
                List<Object> avant = o.signature();
                o.appliquerModele(ref);
                Raci nouvelle = raciInitial(ref, designation);
                if (!fonctions(nouvelle).equals(fonctions(o.getRaci()))) {
                    o.setRaci(nouvelle);
                }
                if (!avant.equals(o.signature())) {
                    modifiees++;
                }
                if (o.getEtat() == EtatOperationParcours.EN_SIGNATURE && o.signaturesAttendues().isEmpty()) {
                    // Plus aucune signature attendue dans la nouvelle version (D-91) : l'opération se clôt.
                    clore(o, o.getPreuve(), aujourdhui);
                }
            }
        }
        List<OperationParcours> ajoutees = new ArrayList<>();
        for (ReferenceOperation ref : refs) {
            if (operation(ref.getCode()).isEmpty()) {
                OperationParcours n = new OperationParcours(liaison.getId(), ref, 0);
                n.setRaci(raciInitial(ref, designation));
                operations.add(n);
                ajoutees.add(n);
            }
        }
        List<String> ordre = new ArrayList<>(parCode.keySet());
        for (OperationParcours o : operations) {
            int i = ordre.indexOf(o.getRang());
            o.setOrdre(i >= 0 ? i : 10_000 + o.getOrdre());
        }
        operations.sort(Comparator.comparingInt(OperationParcours::getOrdre));
        for (OperationParcours o : operations) {
            if (o.getEtat() == EtatOperationParcours.A_FAIRE && !predecesseursNonFaits(o).isEmpty()) {
                o.setEtat(EtatOperationParcours.VERROUILLEE);
            }
        }
        etat.rattacher(versionId, numero);
        planifier();
        ouvrirSuivantes(aujourdhui);
        rafraichir();
        return new Differentiel(List.copyOf(ajoutees), List.copyOf(supprimees), sansObjet, modifiees);
    }

    private static List<Object> fonctions(Raci r) {
        java.util.function.Function<Personne, Object> f = x -> x == null ? null : x.fonction();
        return java.util.Arrays.asList(f.apply(r.r()), f.apply(r.v()), r.c().stream().map(f).toList(), r.i().stream().map(f).toList(), r.e().stream().map(f).toList());
    }

    // ------------------------------------------------------------------ accès

    public Liaison liaison() {
        return liaison;
    }

    public ParcoursLiaison etat() {
        return etat;
    }

    public UUID id() {
        return liaison.getId();
    }

    public List<OperationParcours> operations() {
        return List.copyOf(operations);
    }

    public List<ServiceParcours> services() {
        return List.copyOf(services);
    }

    public List<BlocageParcours> blocages() {
        return List.copyOf(blocages);
    }

    public Map<String, Integer> mesures() {
        return mesures;
    }

    /** Taux repris du simulateur (HOM-12), par service ; la certification d'un service mesuré exige 100 %. */
    public Parcours avecMesures(Map<String, Integer> m) {
        this.mesures = Map.copyOf(m);
        return this;
    }

    public Optional<OperationParcours> operation(String rang) {
        return operations.stream().filter(o -> o.getRang().equals(rang)).findFirst();
    }

    /** Opérations qui satisfont un critère du modèle, dans l'ordre du référentiel. */
    public List<OperationParcours> operations(Predicate<OperationParcours> critere) {
        return operations.stream().filter(critere).toList();
    }

    /** Première opération portant un geste. */
    public Optional<OperationParcours> parGeste(Geste g) {
        return operations.stream().filter(o -> o.porte(g)).findFirst();
    }

    /** Opération qui porte un jalon. */
    public Optional<OperationParcours> parJalon(Jalon j) {
        return operations.stream().filter(o -> o.getJalon() == j).findFirst();
    }

    /** Le jalon est atteint : l'opération qui le porte est faite. */
    public boolean jalonAtteint(Jalon j) {
        return parJalon(j).map(OperationParcours::faite).orElse(false);
    }

    /** Opérations closes par un sujet d'événement. */
    public List<OperationParcours> parDeclencheur(String sujet) {
        return operations(o -> sujet.equals(o.getDeclencheur()));
    }

    /** Première opération d'une étape, dans l'ordre du référentiel. */
    public Optional<OperationParcours> premiereDeLEtape(EtapeCode e) {
        return operations.stream().filter(o -> o.getEtape() == e).findFirst();
    }

    /** Première opération (dans l'ordre du référentiel) dont la fonction responsable de référence est donnée. */
    public Optional<OperationParcours> premiereDuResponsable(Fonction f) {
        return operations.stream().filter(o -> f.equals(o.getResponsable())).findFirst();
    }

    public OperationParcours operation(UUID id) {
        return operations
            .stream()
            .filter(o -> o.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> HomologationException.notFound("HOM_OPERATION_NOT_FOUND", id));
    }

    public Optional<BlocageParcours> blocage(UUID id) {
        return blocages.stream().filter(b -> b.getId().equals(id)).findFirst();
    }

    public List<ServiceParcours> servicesInclus() {
        return services.stream().filter(ServiceParcours::isInclus).toList();
    }

    public boolean operationDansEtat(String rang, EtatOperationParcours e) {
        return operation(rang).map(o -> o.getEtat() == e).orElse(false);
    }

    // ------------------------------------------------------------------ grandeurs calculées

    /** Prédécesseurs instanciés et non faits (les rangs 1 et 2, tenus par MS-PARTICIPANT, sont réputés faits). */
    public List<String> predecesseursNonFaits(OperationParcours op) {
        if (op.faite()) {
            return List.of();
        }
        return op.getPredecesseurs().stream().filter(r -> operation(r).map(x -> !x.faite()).orElse(false)).toList();
    }

    /** Étape de la première opération non faite, dans l'ordre du référentiel ; SUIVI si tout est fait. */
    public EtapeCode etapeCourante() {
        return operations.stream().filter(o -> !o.faite() && o.getEtat() != EtatOperationParcours.SANS_OBJET).findFirst().map(OperationParcours::getEtape).orElse(EtapeCode.SUIVI);
    }

    /** Statut du CR (D-57), déduit des opérations closes. */
    public StatutParcours statutCalcule() {
        if (etat.getStatut() == StatutParcours.SUSPENDUE || etat.getStatut() == StatutParcours.CLOSE_SANS_SUITE) {
            return etat.getStatut();
        }
        if (jalonAtteint(Jalon.EN_PRODUCTION)) {
            return StatutParcours.EN_PRODUCTION;
        }
        if (jalonAtteint(Jalon.APPROUVEE_SECURITE) || etat.getEtatApprobation() == EtatApprobation.APPROUVEE) {
            return StatutParcours.APPROUVEE_SECURITE;
        }
        if (jalonAtteint(Jalon.CERTIFIEE)) {
            return StatutParcours.CERTIFIEE;
        }
        EtapeCode e = etapeCourante();
        if (e == EtapeCode.TESTS || e == EtapeCode.CERTIFICATION) {
            return StatutParcours.EN_TEST;
        }
        return e == EtapeCode.INTEGRATION ? StatutParcours.EN_INTEGRATION : StatutParcours.CADREE;
    }

    public boolean bloquee() {
        return blocages.stream().anyMatch(BlocageParcours::actif);
    }

    /**
     * SLA d'une opération en cours (D-106) : dépassé après l'échéance maximale ; proche à cinq jours ou moins de
     * l'échéance cible, ou au-delà de la cible sans dépasser le maximum ; sinon dans les délais. Sans délai maximal
     * distinct (cible = maximum), la règle antérieure est inchangée.
     */
    public static Sla sla(OperationParcours o, LocalDate aujourdhui) {
        if (!o.getEtat().enCours() || o.getEcheance() == null) {
            return null;
        }
        if (o.getEcheance().isBefore(aujourdhui)) {
            return Sla.DEPASSE;
        }
        return ChronoUnit.DAYS.between(aujourdhui, o.getEcheanceCible()) <= JOURS_ECHEANCE_PROCHE ? Sla.ECHEANCE_PROCHE : Sla.DANS_LES_DELAIS;
    }

    public long tachesEnRetard(LocalDate aujourdhui) {
        return operations.stream().filter(o -> sla(o, aujourdhui) == Sla.DEPASSE).count();
    }

    public LocalDate prochaineEcheance() {
        return operations.stream().filter(o -> !o.faite() && o.getEcheance() != null).map(OperationParcours::getEcheance).min(Comparator.naturalOrder()).orElse(null);
    }

    /** Date cible de mise en production : échéance cible planifiée (D-106) de l'opération qui porte le jalon EN_PRODUCTION. */
    public LocalDate dateCibleProduction() {
        return parJalon(Jalon.EN_PRODUCTION).map(OperationParcours::getEcheanceCible).orElse(null);
    }

    /** Couverture de certification : services inclus certifiés (ouverts ou non). */
    public Couverture couvertureCertification() {
        List<ServiceParcours> inclus = servicesInclus();
        return Couverture.de((int) inclus.stream().filter(s -> s.certifie() || s.getOuvert() != null).count(), inclus.size());
    }

    /** Couverture de mise en service : services inclus déclarés ouverts sur la fiche de suivi. */
    public Couverture couvertureMiseEnService() {
        List<ServiceParcours> inclus = servicesInclus();
        return Couverture.de((int) inclus.stream().filter(s -> Boolean.TRUE.equals(s.getOuvert())).count(), inclus.size());
    }

    public List<String> servicesCouverts() {
        return servicesInclus().stream().filter(s -> s.certifie() || s.getOuvert() != null).map(ServiceParcours::getServiceCode).toList();
    }

    /** Attestations exigées du participant et non encore fournies (tâche du portail, après le procès-verbal signé). */
    public List<TypeAttestation> attestationsAFournir() {
        if (etat.getEtatPv() != EtatProcesVerbal.SIGNE || etat.getEtatApprobation() == EtatApprobation.VERIFIE_ARCHIVE || etat.getEtatApprobation() == EtatApprobation.APPROUVEE) {
            return List.of();
        }
        List<TypeAttestation> deposees = etat.getAttestationsDeposees();
        return liaison.modeleTypeProjet().attestationsExigees(etat.isFintech()).stream().filter(t -> !deposees.contains(t)).toList();
    }

    /** Recalcule les champs dénormalisés (statut, étape courante, blocage) et projette l'automate technique (D-57). */
    public void rafraichir() {
        etat.setEtapeCourante(etapeCourante());
        etat.setStatut(statutCalcule());
        etat.setBloquee(bloquee());
        if (liaison.getEtat() != EtatLiaison.GELEE) {
            liaison.setEtat(etatTechnique(etat.getStatut(), jalonAtteint(Jalon.APTE_CERTIFICATION)));
        }
    }

    /** Correspondance du statut du CR vers l'automate technique du plan (HOM-19), conservé et projeté (D-57). */
    static EtatLiaison etatTechnique(StatutParcours s, boolean couvertureConstatee) {
        return switch (s) {
            case CADREE -> EtatLiaison.OUVERTE;
            case EN_INTEGRATION -> EtatLiaison.EN_HOMOLOGATION;
            case EN_TEST -> couvertureConstatee ? EtatLiaison.APTE_CERTIFICATION : EtatLiaison.EN_HOMOLOGATION;
            case CERTIFIEE -> EtatLiaison.CERTIFIEE;
            case APPROUVEE_SECURITE -> EtatLiaison.MISE_EN_SERVICE_AUTORISEE;
            case EN_PRODUCTION, CLOSE_SANS_SUITE -> EtatLiaison.CLOSE;
            case SUSPENDUE -> EtatLiaison.GELEE;
        };
    }

    // ------------------------------------------------------------------ gestes

    /**
     * Clôt une opération, applique sa particularité (entrée en essai, fin des essais), puis ouvre les suivantes
     * dont tous les prédécesseurs sont faits.
     */
    public void terminer(OperationParcours o, Preuve preuve, LocalDate aujourdhui) {
        if (o.getEtat() == EtatOperationParcours.EN_SIGNATURE) {
            return;
        }
        if (!o.signaturesAttendues().isEmpty()) {
            // Signatures exigées (D-91) : l'opération ne se clôt qu'après elles, dans leur ordre.
            o.setPreuve(preuve);
            o.setEtat(EtatOperationParcours.EN_SIGNATURE);
            rafraichir();
            return;
        }
        clore(o, preuve, aujourdhui);
    }

    /** Clôture effective : opération faite, particularité appliquée, suivantes ouvertes. */
    private void clore(OperationParcours o, Preuve preuve, LocalDate aujourdhui) {
        o.setEtat(EtatOperationParcours.FAITE);
        o.setFaiteLe(aujourdhui);
        o.setPreuve(preuve);
        if (o.getParticularite() == Particularite.ENTREE_ESSAIS) {
            servicesInclus().stream().filter(s -> s.getEntreeEssai() == null).forEach(s -> s.setEntreeEssai(aujourdhui));
        }
        if (o.getParticularite() == Particularite.FIN_ESSAIS) {
            certifierServices(aujourdhui);
        }
        ouvrirSuivantes(aujourdhui);
        rafraichir();
    }

    // ------------------------------------------------------------------ signatures (D-91, docs/lot1/types-projet.md §3)

    /** Empreinte signée et documents des livrables portés déposés. */
    public record Empreinte(String empreinte, List<SignatureRecueillie.DocumentSigne> documents) {}

    /** Signature exigée de l'opération (404 HOM_SIGNATURE_INCONNUE). */
    public static SignatureOperation signature(OperationParcours o, String code) {
        return o
            .getSignatures()
            .stream()
            .filter(s -> s.code().equals(code))
            .findFirst()
            .orElseThrow(() -> HomologationException.notFound("HOM_SIGNATURE_INCONNUE", "Signature inconnue de cette opération.", code));
    }

    /**
     * Personne attendue pour une fonction sur une opération : celle qui tient la fonction dans les qualités de
     * l'opération (R, V, C, I, E), sinon la ressource affectée à la liaison, sinon le conducteur ; vide si
     * personne n'est désigné (tout porteur de la fonction peut alors signer).
     */
    public Optional<Personne> titulaire(OperationParcours o, Fonction f) {
        Raci r = o.getRaci();
        List<Personne> qualites = new ArrayList<>();
        qualites.add(r.r());
        qualites.add(r.v());
        qualites.addAll(r.c());
        qualites.addAll(r.i());
        qualites.addAll(r.e());
        Optional<Personne> p = qualites.stream().filter(x -> x != null && x.affectee() && f.equals(x.fonction())).findFirst();
        if (p.isPresent()) {
            return p;
        }
        return tenant(f);
    }

    /**
     * Personne qui tient une fonction sur la liaison, hors qualités d'une opération : ressource affectée, institution
     * liée (fonction externe), conducteur ; vide si la fonction est à affecter (D-93 §1).
     */
    public Optional<Personne> tenant(Fonction f) {
        Personne ressource = etat.getRessources().get(f);
        if (ressource != null && ressource.affectee()) {
            return Optional.of(ressource);
        }
        InstitutionLiee i = etat.getInstitutionsLiees().get(f);
        if (i != null) {
            return Optional.of(Personne.institution(f, i.id(), i.nom()));
        }
        if (Fonction.CONDUCTEUR.equals(f)) {
            return Optional.ofNullable(conducteur());
        }
        return Optional.empty();
    }

    /** Conducteur de la liaison, s'il est désigné. */
    public Personne conducteur() {
        if (liaison.getChefDeProjetId() == null || etat.isConducteurADesigner()) {
            return null;
        }
        return new Personne(liaison.getChefDeProjetId().toString(), "Conducteur de projet", Fonction.CONDUCTEUR);
    }

    /** L'acteur conduit cette liaison (conducteur désigné). */
    public boolean conduitePar(Acteur a) {
        Personne c = conducteur();
        return c != null && a.est(c);
    }

    /**
     * Empreinte SHA-256 (hexadécimal) des lignes {@code REFERENCE:documentId:sha256\n} des livrables portés déposés,
     * dans l'ordre de {@code porte} : un livrable de l'opération est sa pièce déposée, un livrable {@code OP/L}
     * celle de l'opération OP.
     */
    public Empreinte empreinte(OperationParcours o, SignatureOperation s) {
        List<SignatureRecueillie.DocumentSigne> docs = new ArrayList<>();
        StringBuilder lignes = new StringBuilder();
        for (String ref : s.porte()) {
            String code = SignatureOperation.operationDe(ref);
            Optional<OperationParcours> porteur = code == null ? Optional.of(o) : operation(code);
            Preuve piece = porteur.map(OperationParcours::getPreuve).orElse(null);
            if (piece != null && piece.documentRef() != null && !piece.documentRef().isBlank()) {
                docs.add(new SignatureRecueillie.DocumentSigne(ref, piece.documentRef(), piece.empreinte()));
                lignes.append(ref).append(':').append(piece.documentRef()).append(':').append(piece.empreinte()).append('\n');
            }
        }
        return new Empreinte(empreinte(lignes.toString()), List.copyOf(docs));
    }

    /**
     * Signature d'une opération en attente de signatures (D-91) : dans l'ordre croissant (même ordre : indifférent),
     * une fois chacune ; la dernière clôt l'opération. Le droit du signataire et l'assertion FIDO2 sont vérifiés par
     * l'appelant. Rend la signature recueillie.
     */
    public SignatureRecueillie signer(OperationParcours o, String code, Personne signataire, java.time.Instant instant, LocalDate aujourdhui) {
        SignatureOperation s = verifierSignable(o, code);
        Empreinte e = empreinte(o, s);
        SignatureRecueillie r = new SignatureRecueillie(code, signataire.id(), signataire.nom(), s.signataire(), instant, e.empreinte(), e.documents());
        o.ajouterSignature(r);
        if (o.signaturesAttendues().isEmpty()) {
            clore(o, o.getPreuve(), aujourdhui);
        }
        return r;
    }

    /**
     * La signature peut être donnée maintenant : opération EN_SIGNATURE (409 HOM_SIGNATURE_ETAT), signature pas encore
     * donnée (409 HOM_SIGNATURE_DEJA_SIGNEE) et du plus petit ordre restant (409 HOM_SIGNATURE_ORDRE).
     */
    public SignatureOperation verifierSignable(OperationParcours o, String code) {
        SignatureOperation s = signature(o, code);
        if (o.getEtat() != EtatOperationParcours.EN_SIGNATURE) {
            throw HomologationException.conflict(
                "HOM_SIGNATURE_ETAT",
                "L'opération n'attend pas de signature.",
                Map.of("etat", o.getEtat().name(), "attendu", EtatOperationParcours.EN_SIGNATURE.name())
            );
        }
        if (o.recueillie(code).isPresent()) {
            throw HomologationException.conflict("HOM_SIGNATURE_DEJA_SIGNEE", "Cette signature est déjà donnée.", Map.of("signature", code));
        }
        if (o.signaturesSuivantes().stream().noneMatch(x -> x.code().equals(code))) {
            throw HomologationException.conflict(
                "HOM_SIGNATURE_ORDRE",
                "Les signatures se donnent dans leur ordre : d'autres sont attendues avant celle-ci.",
                Map.of("signature", code, "attendues", o.signaturesSuivantes().stream().map(SignatureOperation::code).toList())
            );
        }
        return s;
    }

    /** Clôture imposée par un événement ou un constat : sans effet si l'opération est faite. */
    public void forcerCloture(OperationParcours o, Preuve preuve, LocalDate aujourdhui) {
        if (!o.faite()) {
            terminer(o, preuve, aujourdhui);
        }
    }

    /** Clôture imposée de toutes les opérations qu'un sujet d'événement déclenche, dans l'ordre du référentiel. */
    public void forcerCloture(String sujet, Preuve preuve, LocalDate aujourdhui) {
        parDeclencheur(sujet).forEach(o -> forcerCloture(o, preuve, aujourdhui));
    }

    /**
     * Ouvre les opérations verrouillées dont tous les prédécesseurs sont faits ; un point de passage (geste AUCUN)
     * devenu éligible est clos aussitôt, ce qui peut en ouvrir d'autres.
     */
    void ouvrirSuivantes(LocalDate aujourdhui) {
        boolean bloque = bloquee();
        boolean encore = true;
        while (encore) {
            encore = false;
            for (OperationParcours x : operations) {
                if (x.getEtat() == EtatOperationParcours.VERROUILLEE && predecesseursNonFaits(x).isEmpty()) {
                    if (x.porte(Geste.AUCUN)) {
                        x.setEtat(EtatOperationParcours.FAITE);
                        x.setFaiteLe(aujourdhui);
                        encore = true;
                    } else {
                        x.setEtat(bloque ? EtatOperationParcours.BLOQUEE : EtatOperationParcours.A_FAIRE);
                    }
                }
            }
        }
    }

    /**
     * Certification des services à la fin des essais (particularité FIN_ESSAIS) et au constat de la couverture : un service mesuré
     * par le simulateur est certifié à 100 % ; sans aucune mesure reprise, les essais validés certifient
     * le périmètre inclus.
     */
    void certifierServices(LocalDate aujourdhui) {
        for (ServiceParcours s : servicesInclus()) {
            Integer m = mesures.get(s.getServiceCode());
            boolean certifie = mesures.isEmpty() || (m != null && m >= 100);
            if (certifie && s.getCertifieLe() == null) {
                s.setCertifieLe(aujourdhui);
                if (s.getSortieEssai() == null) {
                    s.setSortieEssai(aujourdhui);
                }
            } else if (!certifie) {
                s.setCertifieLe(null);
            }
        }
    }

    private static String empreinte(String texte) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(texte.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Opération dont le geste a sa route propre et ne se clôt jamais par un livrable ni une preuve libres :
     * affectation des ressources, verrous de la décision tenus par MS-CERTIFICATION (événement externe, D-56),
     * fiche de suivi et son constat.
     */
    public static boolean gesteDedie(OperationParcours o) {
        return o.getGeste().dedie();
    }

    private static void refuserGesteLibre(OperationParcours o) {
        if (gesteDedie(o)) {
            throw HomologationException.conflict(
                "HOM_OPERATION_DEDICATED_GESTURE",
                "Cette opération se réalise par son geste propre (affectation, verrous de la décision ou fiche de suivi), jamais par une pièce libre.",
                Map.of("rang", o.getRang())
            );
        }
    }

    /** Action attendue sur une opération ouverte, selon son geste : constat, livrable, sinon preuve. */
    public static String geste(OperationParcours o) {
        return o.getGeste().action();
    }

    /** Livrable (compte rendu, document de paramétrage…) : clôt l'opération ouverte ; jamais sur une clé ni une valeur de référence. */
    public void deposerLivrable(OperationParcours o, Preuve preuve, LocalDate aujourdhui) {
        deposerLivrable(o, livrableUnique(o), preuve, null, null, false, aujourdhui);
    }

    /** Livrable unique de l'opération, sinon null (dépôt sans code, antérieur à D-93). */
    static Livrable livrableUnique(OperationParcours o) {
        List<Livrable> l = o.getLivrables();
        return l.size() == 1 ? l.get(0) : null;
    }

    /**
     * Dépôt d'un livrable attendu (D-93 §3) : {@code livrable} est l'entrée du référentiel (null pour une opération sans
     * livrable déclaré, comportement antérieur : la pièce clôt l'opération). Le dépôt est inscrit ; l'opération se clôt
     * quand plus aucun livrable obligatoire ne manque, sinon elle passe EN_COURS.
     */
    public void deposerLivrable(OperationParcours o, Livrable livrable, Preuve preuve, String typeMime, String deposantId, boolean supervision, LocalDate aujourdhui) {
        refuserGesteLibre(o);
        if (!o.getEtat().ouverte() || o.porte(Geste.CONSTATER_COUVERTURE) || o.isCle() || o.isValeurReference()) {
            throw HomologationException.conflict("HOM_OPERATION_NOT_OPEN", "Cette opération n'attend pas de livrable.", Map.of("etat", o.getEtat().name(), "rang", o.getRang()));
        }
        if (livrable == null) {
            terminer(o, preuve, aujourdhui);
            return;
        }
        inscrire(o, livrable, preuve, typeMime, deposantId, supervision);
        if (o.livrablesManquants().isEmpty()) {
            terminer(o, preuve, aujourdhui);
        } else {
            o.setPreuve(preuve);
            o.setEtat(EtatOperationParcours.EN_COURS);
        }
    }

    private static void inscrire(OperationParcours o, Livrable l, Preuve p, String typeMime, String deposantId, boolean supervision) {
        o.deposer(new DepotLivrable(l.code(), p.documentRef(), p.nom(), p.empreinte(), typeMime, p.deposeLe(), p.deposePar(), deposantId, supervision ? Boolean.TRUE : null, null));
    }

    /**
     * Livrable visé par un dépôt (D-93 §3) : le code donné ; sans code, l'unique livrable de l'opération ; aucun livrable
     * déclaré : null (pièce libre). 422 HOM_LIVRABLE_REQUIS (plusieurs livrables, aucun code), HOM_LIVRABLE_INCONNU.
     */
    public static Livrable livrableVise(OperationParcours o, String code) {
        List<Livrable> l = o.getLivrables();
        if (code == null || code.isBlank()) {
            if (l.size() > 1) {
                throw HomologationException.unprocessableDetails(
                    "HOM_LIVRABLE_REQUIS",
                    "Cette opération attend plusieurs livrables : préciser le livrable déposé.",
                    Map.of("attendus", l.stream().map(Livrable::code).toList())
                );
            }
            return l.isEmpty() ? null : l.get(0);
        }
        return l
            .stream()
            .filter(x -> x.code().equals(code.trim()))
            .findFirst()
            .orElseThrow(() ->
                HomologationException.unprocessableDetails(
                    "HOM_LIVRABLE_INCONNU",
                    "Ce livrable n'est pas attendu sur cette opération.",
                    Map.of("livrable", code, "attendus", l.stream().map(Livrable::code).toList())
                )
            );
    }

    /** Type de pièce admis par le livrable (types déclarés au référentiel ; aucun : tout type), 422 HOM_LIVRABLE_TYPE_REFUSE. */
    public static void verifierType(Livrable l, String typeMime) {
        if (l == null || l.typesPieces().isEmpty()) {
            return;
        }
        String t = typeMime == null ? "" : typeMime.split(";")[0].trim().toLowerCase(java.util.Locale.ROOT);
        if (l.typesPieces().stream().noneMatch(x -> x.equalsIgnoreCase(t))) {
            throw HomologationException.unprocessableDetails(
                "HOM_LIVRABLE_TYPE_REFUSE",
                "Le type de la pièce n'est pas admis pour ce livrable.",
                Map.of("livrable", l.code(), "type", t, "admis", l.typesPieces())
            );
        }
    }

    /** Clôture refusée tant qu'un livrable obligatoire manque (D-93 §3), 409 HOM_LIVRABLE_MANQUANT. */
    public static void exigerLivrables(OperationParcours o) {
        List<Livrable> manquants = o.livrablesManquants();
        if (!manquants.isEmpty()) {
            throw HomologationException.conflict(
                "HOM_LIVRABLE_MANQUANT",
                "Un livrable obligatoire manque : l'opération ne peut pas être close.",
                Map.of("manquants", manquants.stream().map(Livrable::code).toList())
            );
        }
    }

    /**
     * Preuve au grain de l'opération (CR §6.4.3, D-59) : une clé ne porte que son code de contrôle (4 à 8
     * hexadécimaux), une cérémonie de clés (particularité CEREMONIE_CLES) sa date, ses trois porteurs et leurs
     * trois codes ; un secret ne porte
     * qu'une trace ; une valeur de référence est exigée quand l'opération en attend une. L'opération passe
     * en validation.
     */
    public void deposerPreuve(OperationParcours o, Preuve piece, String valeurReference, String codeControle, Ceremonie ceremonie) {
        deposerPreuve(o, livrableUnique(o), piece, valeurReference, codeControle, ceremonie);
    }

    /**
     * Preuve d'un livrable attendu (D-93 §3) : le dépôt est inscrit ; l'opération ne passe en validation qu'une fois
     * tous ses livrables obligatoires déposés (sinon EN_COURS). {@code livrable} null : preuve unique (antérieur).
     */
    public void deposerPreuve(OperationParcours o, Livrable livrable, Preuve piece, String valeurReference, String codeControle, Ceremonie ceremonie) {
        refuserGesteLibre(o);
        if (!o.getEtat().ouverte() || o.porte(Geste.CONSTATER_COUVERTURE)) {
            throw HomologationException.conflict("HOM_OPERATION_NOT_OPEN", "Cette opération n'est pas ouverte.", Map.of("etat", o.getEtat().name(), "rang", o.getRang()));
        }
        String code = codeControle == null || codeControle.isBlank() ? null : codeControle.trim().toUpperCase(java.util.Locale.ROOT);
        Ceremonie c = null;
        if (o.getParticularite() == Particularite.CEREMONIE_CLES) {
            c = ceremonieControlee(ceremonie);
        } else if (o.isCle() && code == null) {
            throw HomologationException.unprocessable("HOM_KEY_CHECK_VALUE_REQUIRED", "Le code de contrôle de la clé est exigé ; aucune valeur de clé n'est admise.");
        }
        if (o.isCle() && code != null && !CODE_CONTROLE.matcher(code).matches()) {
            throw invalide();
        }
        if (o.isValeurReference() && (valeurReference == null || valeurReference.isBlank())) {
            throw HomologationException.unprocessable("HOM_REFERENCE_VALUE_REQUIRED", "Cette preuve doit porter la valeur de référence relevée.");
        }
        Preuve p = piece;
        if (o.isValeurReference()) {
            p = p.avecValeurReference(new ValeurReference("parcours.valeur." + o.getRang(), valeurReference.trim()));
        }
        if (o.isCle()) {
            p = p.avecCle(code, c);
        }
        o.setPreuve(p);
        if (livrable != null) {
            inscrire(o, livrable, p, null, null, false);
        }
        o.setEtat(o.livrablesManquants().isEmpty() ? EtatOperationParcours.EN_VALIDATION : EtatOperationParcours.EN_COURS);
    }

    /** Réinscrit le dépôt de la preuve d'un livrable attendu avec la pièce revérifiée et son déposant (D-93 §3). */
    public void inscrirePreuve(OperationParcours o, Livrable livrable, String typeMime, String deposantId, boolean supervision) {
        Preuve p = o.getPreuve();
        if (livrable != null && p != null) {
            inscrire(o, livrable, p, typeMime, deposantId, supervision);
        }
    }

    private static Ceremonie ceremonieControlee(Ceremonie c) {
        boolean dateValide = c != null && c.tenueLe() != null && dateValide(c.tenueLe());
        if (!dateValide || c.porteurs().stream().filter(x -> x != null && !x.isBlank()).count() != 3 || c.codesControle().size() != 3) {
            throw HomologationException.unprocessable(
                "HOM_KEY_CEREMONY_INCOMPLETE",
                "La cérémonie exige sa date de tenue, les trois porteurs de composantes et leurs trois codes de contrôle."
            );
        }
        List<String> codes = c.codesControle().stream().map(x -> x == null ? "" : x.trim().toUpperCase(java.util.Locale.ROOT)).toList();
        if (!codes.stream().allMatch(x -> CODE_CONTROLE.matcher(x).matches())) {
            throw invalide();
        }
        return new Ceremonie(c.tenueLe(), c.porteurs().stream().map(String::trim).toList(), codes);
    }

    private static boolean dateValide(String s) {
        try {
            LocalDate.parse(s);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    private static HomologationException invalide() {
        return HomologationException.unprocessable("HOM_KEY_CHECK_VALUE_INVALID", "Un code de contrôle compte de 4 à 8 caractères hexadécimaux.");
    }

    /** Validation par le validateur (V), jamais le responsable (R) de la même opération. */
    public void valider(OperationParcours o, Acteur acteur, LocalDate aujourdhui) {
        if (o.getEtat() != EtatOperationParcours.EN_VALIDATION) {
            throw HomologationException.conflict("HOM_OPERATION_NOT_IN_VALIDATION", "L'opération n'attend pas de validation.", Map.of("etat", o.getEtat().name()));
        }
        if (acteur.est(o.getRaci().r())) {
            throw validateurResponsable();
        }
        exigerLivrables(o);
        terminer(o, o.getPreuve(), aujourdhui);
    }

    /**
     * Renvoi par le validateur (D-93 §2) : l'opération en validation revient à faire (bloquée si un blocage est actif),
     * sa preuve et ses dépôts restent consultables, le motif est inscrit ; la tâche de R est rouverte.
     */
    public Renvoi renvoyer(OperationParcours o, Acteur acteur, String motif, LocalDate aujourdhui) {
        if (o.getEtat() != EtatOperationParcours.EN_VALIDATION) {
            throw HomologationException.conflict("HOM_OPERATION_NOT_IN_VALIDATION", "L'opération n'attend pas de validation.", Map.of("etat", o.getEtat().name()));
        }
        String m = motif == null ? "" : motif.trim();
        if (m.isEmpty() || m.length() > 500) {
            throw HomologationException.unprocessable("HOM_MOTIF_REQUIS", "Le motif du renvoi est exigé (500 caractères au plus).");
        }
        if (acteur.est(o.getRaci().r())) {
            throw validateurResponsable();
        }
        Renvoi r = new Renvoi(m, acteur.id() == null ? null : acteur.id().toString(), acteur.nom(), aujourdhui);
        o.ajouterRenvoi(r);
        o.setEtat(bloquee() ? EtatOperationParcours.BLOQUEE : EtatOperationParcours.A_FAIRE);
        rafraichir();
        return r;
    }

    // ------------------------------------------------------------------ RACI à l'exécution (D-93 §2)

    /** Titre auquel un acteur dépose un livrable ou une preuve. */
    public enum DroitDepot {
        /** Responsable (R) désigné. */
        RESPONSABLE,
        /** R à affecter : porteur de sa fonction. */
        PORTEUR,
        /** Déposant du livrable (désigné, ou porteur de la fonction déposante sans désignation). */
        DEPOSANT,
        /** Conducteur de la liaison. */
        CONDUCTEUR,
        /** HOM_STEP_MANAGE, hors RACI : supervision journalisée. */
        SUPERVISION,
    }

    /**
     * Droit de dépôt sur une opération (D-93 §2) : R désigné (ou porteur de sa fonction s'il est à affecter), déposant
     * du livrable visé (ou de l'un d'eux sans code), conducteur de la liaison, sinon supervision par HOM_STEP_MANAGE ;
     * vide : refus.
     */
    public Optional<DroitDepot> droitDepot(OperationParcours o, Livrable livrable, Acteur a, Set<Fonction> fonctions) {
        if (a.id() == null) {
            return Optional.empty();
        }
        Personne r = o.getRaci().r();
        if (a.est(r)) {
            return Optional.of(DroitDepot.RESPONSABLE);
        }
        if (r != null && !r.affectee() && fonctions.contains(r.fonction())) {
            return Optional.of(DroitDepot.PORTEUR);
        }
        List<Livrable> vises = livrable != null ? List.of(livrable) : o.getLivrables();
        for (Livrable l : vises) {
            if (l.deposant() != null && titulaire(o, l.deposant()).map(a::est).orElse(fonctions.contains(l.deposant()))) {
                return Optional.of(DroitDepot.DEPOSANT);
            }
        }
        if (conduitePar(a)) {
            return Optional.of(DroitDepot.CONDUCTEUR);
        }
        return a.peut(Permissions.STEP_MANAGE) ? Optional.of(DroitDepot.SUPERVISION) : Optional.empty();
    }

    /** Dépôt admis, sinon 403 HOM_DEPOT_NON_AUTORISE (D-93 §2). */
    public DroitDepot exigerDroitDepot(OperationParcours o, Livrable livrable, Acteur a, Set<Fonction> fonctions) {
        return droitDepot(o, livrable, a, fonctions).orElseThrow(() ->
            HomologationException.forbidden(
                "HOM_DEPOT_NON_AUTORISE",
                "Ce dépôt revient au responsable désigné (" + nomOuFonction(o.getRaci().r()) + "), au déposant du livrable ou au conducteur de la liaison."
            )
        );
    }

    /** Validateur admis : le V désigné ; V à affecter : un porteur de sa fonction (D-93 §2). */
    public boolean peutValider(OperationParcours o, Acteur a, Set<Fonction> fonctions) {
        Personne v = o.getRaci().v();
        if (a.id() == null || v == null || a.est(o.getRaci().r())) {
            return false;
        }
        return v.affectee() ? a.est(v) : fonctions.contains(v.fonction());
    }

    /** Validation ou renvoi admis, sinon 403 HOM_VALIDATION_NON_VALIDATEUR. */
    public void exigerValidateur(OperationParcours o, Acteur a, Set<Fonction> fonctions) {
        if (!a.est(o.getRaci().r()) && !peutValider(o, a, fonctions)) {
            throw HomologationException.forbidden("HOM_VALIDATION_NON_VALIDATEUR", "Cette validation revient au validateur désigné (" + nomOuFonction(o.getRaci().v()) + ").");
        }
    }

    private static String nomOuFonction(Personne p) {
        if (p == null) {
            return "non désigné";
        }
        return p.affectee() ? p.nom() : "un porteur de la fonction " + p.fonction().libelle();
    }

    // ------------------------------------------------------------------ affectation et départ (D-93 §1)

    /**
     * Affecte une personne (fonction interne) ou une institution liée (fonction externe) à une fonction sur la liaison :
     * elle remplace, sur les opérations non faites, la fonction à affecter et la personne qui la tenait jusque-là.
     */
    public void affecterFonction(Fonction f, Personne p) {
        if (p.institutionnelle()) {
            etat.lierInstitution(f, new InstitutionLiee(p.institutionId(), p.nom()));
        } else {
            Map<Fonction, Personne> ressources = new LinkedHashMap<>(etat.getRessources());
            ressources.put(f, p);
            etat.setRessources(ressources);
        }
        operations
            .stream()
            .filter(o -> !o.faite() && o.getEtat() != EtatOperationParcours.SANS_OBJET)
            .forEach(o -> o.setRaci(o.getRaci().remplacerSi(x -> f.equals(x.fonction()), x -> p)));
    }

    /**
     * Départ d'un compte (désactivation, suppression) : ses qualités sur les opérations non faites repassent « à
     * affecter », ses ressources sont retirées ; conducteur parti : conducteur à désigner. Rend les fonctions libérées.
     */
    public Set<Fonction> retirerCompte(String compteId) {
        Set<Fonction> liberees = new java.util.LinkedHashSet<>();
        Map<Fonction, Personne> ressources = new LinkedHashMap<>(etat.getRessources());
        ressources.entrySet().removeIf(e -> {
            boolean lui = compteId.equals(e.getValue().id());
            if (lui) {
                liberees.add(e.getKey());
            }
            return lui;
        });
        etat.setRessources(ressources);
        for (OperationParcours o : operations) {
            if (o.faite() || o.getEtat() == EtatOperationParcours.SANS_OBJET) {
                continue;
            }
            o.getRaci().toutes().stream().filter(x -> compteId.equals(x.id())).forEach(x -> liberees.add(x.fonction()));
            o.setRaci(o.getRaci().remplacerSi(x -> compteId.equals(x.id()), x -> Personne.aAffecter(x.fonction())));
        }
        if (liaison.getChefDeProjetId() != null && compteId.equals(liaison.getChefDeProjetId().toString()) && !etat.isConducteurADesigner()) {
            etat.setConducteurADesigner(true);
            liberees.add(Fonction.CONDUCTEUR);
        }
        return liberees;
    }

    /** Le compte tient une qualité sur une opération non faite, une ressource ou la conduite de la liaison. */
    public boolean implique(String compteId) {
        if (etat.getRessources().values().stream().anyMatch(p -> compteId.equals(p.id()))) {
            return true;
        }
        if (liaison.getChefDeProjetId() != null && compteId.equals(liaison.getChefDeProjetId().toString()) && !etat.isConducteurADesigner()) {
            return true;
        }
        return operations.stream().filter(o -> !o.faite()).anyMatch(o -> o.getRaci().toutes().stream().anyMatch(x -> compteId.equals(x.id())));
    }

    private static HomologationException validateurResponsable() {
        return HomologationException.conflict("HOM_VALIDATOR_IS_RESPONSIBLE", "Le validateur doit être distinct du responsable.", Map.of());
    }

    /** Attribution d'une qualité (R, C, V, I, E) : R et V restent distincts. */
    public void attribuer(OperationParcours o, Qualite q, Personne p) {
        Raci r = o.getRaci();
        if ((q == Qualite.V && r.r() != null && p.id().equals(r.r().id())) || (q == Qualite.R && r.v() != null && p.id().equals(r.v().id()))) {
            throw validateurResponsable();
        }
        o.setRaci(r.ajouter(q, p));
    }

    /** Affectation des ressources (geste AFFECTER_RESSOURCES) : chaque personne remplace la fonction qu'elle tient sur les opérations non faites. */
    public void affecterRessources(List<Personne> affectations, Preuve preuve, LocalDate aujourdhui) {
        OperationParcours o6 = operations(o -> o.porte(Geste.AFFECTER_RESSOURCES) && o.getEtat() == EtatOperationParcours.A_FAIRE).stream().findFirst().orElseThrow(() ->
            HomologationException.conflict("HOM_STEP_NOT_OPEN", "L'affectation des ressources n'est pas ouverte.", Map.of())
        );
        if (affectations.isEmpty()) {
            throw HomologationException.unprocessable("HOM_RESOURCES_REQUIRED", "Au moins une ressource doit être affectée.");
        }
        Map<Fonction, Personne> ressources = new LinkedHashMap<>(etat.getRessources());
        affectations.forEach(p -> ressources.put(p.fonction(), p));
        etat.setRessources(ressources);
        for (OperationParcours o : operations) {
            if (!o.faite()) {
                Raci r = o.getRaci();
                for (Personne p : affectations) {
                    r = r.remplacer(p.fonction(), p);
                }
                o.setRaci(r);
            }
        }
        terminer(o6, preuve, aujourdhui);
    }

    /** Conducteur désigné ou redésigné : il tient désormais sa fonction sur les opérations non faites. */
    public void designerConducteur(Personne conducteur) {
        etat.setConducteurADesigner(false);
        operations.stream().filter(o -> !o.faite()).forEach(o -> o.setRaci(o.getRaci().remplacer(Fonction.CONDUCTEUR, conducteur)));
    }

    /** Blocage daté (CR annexe B) : les opérations ouvertes sont bloquées jusqu'à la levée. */
    public BlocageParcours declarerBlocage(String motif, String prochaineAction, Personne responsable, LocalDate echeanceLevee, UUID declarePar, LocalDate aujourdhui) {
        BlocageParcours b = new BlocageParcours(id(), motif.trim(), aujourdhui, prochaineAction.trim(), responsable, echeanceLevee, etapeCourante(), declarePar);
        blocages.add(b);
        operations.stream().filter(o -> o.getEtat().ouverte()).forEach(o -> o.setEtat(EtatOperationParcours.BLOQUEE));
        rafraichir();
        return b;
    }

    /** Levée d'un blocage : les opérations bloquées sont rouvertes quand plus aucun blocage n'est actif. */
    public void leverBlocage(BlocageParcours b, String commentaire, LocalDate aujourdhui) {
        if (!b.actif()) {
            throw HomologationException.conflict("HOM_BLOCKAGE_ALREADY_LIFTED", "Ce blocage est déjà levé.", Map.of("leveLe", b.getLeveLe().toString()));
        }
        b.lever(aujourdhui, commentaire);
        if (!bloquee()) {
            operations.stream().filter(o -> o.getEtat() == EtatOperationParcours.BLOQUEE).forEach(o -> o.setEtat(EtatOperationParcours.A_FAIRE));
        }
        rafraichir();
    }

    /** Constat de la couverture (geste CONSTATER_COUVERTURE) : ouvert à la fin des essais ; la couverture constatée est rendue. */
    public Couverture constaterCouverture(Preuve preuve, LocalDate aujourdhui) {
        OperationParcours o17 = operations(o -> o.porte(Geste.CONSTATER_COUVERTURE) && o.getEtat().ouverte()).stream().findFirst().orElseThrow(() ->
            HomologationException.conflict("HOM_COVERAGE_NOT_READY", "Les essais ne sont pas terminés : la couverture ne peut pas être constatée.", Map.of())
        );
        certifierServices(aujourdhui);
        etat.constaterCouverture(aujourdhui);
        terminer(o17, preuve, aujourdhui);
        return couvertureCertification();
    }

    /** Coordonnées techniques d'essai communiquées par le participant (geste COORDONNEES_TECHNIQUES) : hôte et port contrôlés, relisibles. */
    public void communiquerCoordonnees(String adresse, String port, String pointsTerminaison, Preuve piece, LocalDate aujourdhui) {
        String a = adresse == null ? "" : adresse.trim();
        String p = port == null ? "" : port.trim();
        if (!HOTE.matcher(a).matches() || !PORT.matcher(p).matches() || Integer.parseInt(p) > 65535 || (pointsTerminaison != null && pointsTerminaison.length() > 500)) {
            throw HomologationException.unprocessable("HOM_TECHNICAL_DATA_INVALID", "Adresse ou port invalide.");
        }
        OperationParcours o12 = operations(o -> o.porte(Geste.COORDONNEES_TECHNIQUES) && o.getEtat() == EtatOperationParcours.A_FAIRE).stream().findFirst().orElseThrow(() ->
            HomologationException.conflict("HOM_OPERATION_NOT_OPEN", "Les coordonnées techniques ne sont pas attendues à cette étape.", Map.of())
        );
        etat.setPointsTerminaison(pointsTerminaison == null || pointsTerminaison.isBlank() ? null : pointsTerminaison.trim());
        terminer(o12, piece.avecValeurReference(new ValeurReference("parcours.valeur." + o12.getRang(), a + ":" + p)), aujourdhui);
    }

    /** Remise de la fiche de suivi des services au participant (étape 10). */
    public void remettreFiche() {
        if (etat.getFicheEtat() != EtatFicheSuivi.A_REMETTRE) {
            throw HomologationException.conflict("HOM_SERVICE_SHEET_ALREADY_SENT", "La fiche de suivi a déjà été remise.", Map.of("etat", etat.getFicheEtat().name()));
        }
        etat.setFicheEtat(EtatFicheSuivi.REMISE);
    }

    /** Fiche renseignée par le participant (geste FICHE_SUIVI, responsable externe) : chaque service du périmètre, ouvert ou non. */
    public void renseignerFiche(List<LigneFiche> lignes, Preuve preuve, LocalDate aujourdhui) {
        if (etat.getFicheEtat() != EtatFicheSuivi.REMISE) {
            throw HomologationException.conflict("HOM_SERVICE_SHEET_NOT_OPEN", "La fiche de suivi n'est pas ouverte à la saisie.", Map.of("etat", etat.getFicheEtat().name()));
        }
        List<ServiceParcours> inclus = servicesInclus();
        Map<String, LigneFiche> parCode = new HashMap<>();
        lignes.forEach(l -> parCode.put(l.serviceCode(), l));
        if (lignes.size() != inclus.size() || inclus.stream().anyMatch(s -> parCode.get(s.getServiceCode()) == null || parCode.get(s.getServiceCode()).ouvert() == null)) {
            throw HomologationException.unprocessable("HOM_SERVICE_SHEET_INCOMPLETE", "Chaque service du périmètre doit être renseigné.");
        }
        for (ServiceParcours s : inclus) {
            LigneFiche l = parCode.get(s.getServiceCode());
            s.setOuvert(l.ouvert());
            s.setCommentaire(l.commentaire() == null || l.commentaire().isBlank() ? null : l.commentaire().trim());
        }
        etat.setFicheEtat(EtatFicheSuivi.RENSEIGNEE);
        etat.setFicheRenseigneeLe(aujourdhui);
        operations(o -> o.porte(Geste.FICHE_SUIVI) && ficheDuParticipant(o)).forEach(o -> forcerCloture(o, preuve, aujourdhui));
    }

    /** Opération de la fiche de suivi qui revient au participant (responsable de référence externe), sinon son constat. */
    public static boolean ficheDuParticipant(OperationParcours o) {
        return o.getResponsable() != null && o.getResponsable().externe();
    }

    /** Constat de mise en service (geste FICHE_SUIVI, responsable interne), sur la fiche renseignée. */
    public void constaterMiseEnService(Preuve preuve, LocalDate aujourdhui) {
        if (etat.getFicheEtat() != EtatFicheSuivi.RENSEIGNEE) {
            throw HomologationException.conflict("HOM_SERVICE_SHEET_INCOMPLETE", "La fiche de suivi n'est pas encore renseignée par le participant.", Map.of("etat", etat.getFicheEtat().name()));
        }
        etat.setFicheEtat(EtatFicheSuivi.CONSTATEE);
        etat.setFicheConstateeLe(aujourdhui);
        operations(o -> o.porte(Geste.FICHE_SUIVI) && !ficheDuParticipant(o)).forEach(o -> forcerCloture(o, preuve, aujourdhui));
    }

    // ------------------------------------------------------------------ projection des verrous (événements de MS-CERTIFICATION)

    /** certification.pv-established.v1 : opérations de ce déclencheur closes avec le procès-verbal. */
    public void pvEtabli(Preuve preuve, LocalDate aujourdhui) {
        if (etat.getEtatPv() == EtatProcesVerbal.NON_ETABLI) {
            etat.setEtatPv(EtatProcesVerbal.ETABLI);
        }
        forcerCloture(Declencheurs.PV_ETABLI, preuve, aujourdhui);
    }

    /**
     * certification.pv-signed.v1 : signé des deux parties, les opérations du procès-verbal établi puis signé sont
     * closes (jalon CERTIFIEE) ; une seule signature est projetée.
     */
    public void pvSigne(boolean gimac, boolean participant, Preuve preuve, LocalDate aujourdhui) {
        EtatProcesVerbal e = etat.getEtatPv();
        boolean g = gimac || e == EtatProcesVerbal.SIGNE_GIMAC || e == EtatProcesVerbal.SIGNE;
        boolean pa = participant || e == EtatProcesVerbal.SIGNE_PARTICIPANT || e == EtatProcesVerbal.SIGNE;
        if (g && pa) {
            etat.setEtatPv(EtatProcesVerbal.SIGNE);
            forcerCloture(Declencheurs.PV_ETABLI, preuve, aujourdhui);
            forcerCloture(Declencheurs.PV_SIGNE, preuve, aujourdhui);
        } else if (g || pa) {
            etat.setEtatPv(g ? EtatProcesVerbal.SIGNE_GIMAC : EtatProcesVerbal.SIGNE_PARTICIPANT);
        }
        rafraichir();
    }

    /**
     * certification.pv-signed.v1 (D-91, D-94) : les signatures configurées des opérations de ce déclencheur que le
     * circuit de MS-CERTIFICATION a recueillies (même code de signature) sont tenues pour recueillies, avec leur
     * signataire, leur date et l'empreinte du procès-verbal, origine {@code certification.pv-signed.v1} : elles ne sont
     * jamais redemandées (pas de double signature). Une opération en attente de signatures qui n'en attend plus est
     * close. Rend les signatures reprises ({@code <code opération>/<code signature>}).
     */
    public List<String> recueillirSignaturesProcesVerbal(List<SignatureRecueillie> signees, LocalDate aujourdhui) {
        List<String> reprises = new ArrayList<>();
        for (OperationParcours o : parDeclencheur(Declencheurs.PV_SIGNE)) {
            if (o.faite() || o.getEtat() == EtatOperationParcours.SANS_OBJET) {
                continue;
            }
            for (SignatureOperation s : o.signaturesAttendues()) {
                signees
                    .stream()
                    .filter(x -> s.code().equals(x.code()))
                    .findFirst()
                    .ifPresent(x -> {
                        o.ajouterSignature(new SignatureRecueillie(s.code(), x.signataireId(), x.signataireNom(), s.signataire(), x.signeLe(), x.empreinte(), List.of(), Declencheurs.PV_SIGNE));
                        reprises.add(o.getRang() + "/" + s.code());
                    });
            }
            if (o.getEtat() == EtatOperationParcours.EN_SIGNATURE && o.signaturesAttendues().isEmpty()) {
                clore(o, o.getPreuve(), aujourdhui);
            }
        }
        return reprises;
    }

    /** certification.change-record-constituted.v1 : opérations de ce déclencheur closes. */
    public void dossierChangementConstitue(Preuve preuve, LocalDate aujourdhui) {
        if (etat.getEtatApprobation() == EtatApprobation.EN_ATTENTE_DOSSIER) {
            etat.setEtatApprobation(EtatApprobation.DOSSIER_CONSTITUE);
        }
        forcerCloture(Declencheurs.DOSSIER_CHANGEMENT, preuve, aujourdhui);
    }

    /** certification.change-record-referenced.v1 : opérations de ce déclencheur closes si elles sont ouvertes. */
    public void ficheChangementReferencee(String reference, Preuve preuve, LocalDate aujourdhui) {
        etat.setFicheChangementReference(reference);
        parDeclencheur(Declencheurs.FICHE_CHANGEMENT).stream().filter(o -> o.getEtat().ouverte()).forEach(o -> terminer(o, preuve, aujourdhui));
    }

    /** certification.attestations-archived.v1 : opérations de ce déclencheur closes. */
    public void attestationsArchivees(Preuve preuve, LocalDate aujourdhui) {
        etat.setEtatApprobation(EtatApprobation.VERIFIE_ARCHIVE);
        etat.setAttestationsDeposees(List.of());
        forcerCloture(Declencheurs.ATTESTATIONS_ARCHIVEES, preuve, aujourdhui);
    }

    /** certification.security-approved.v1 : opérations de ce déclencheur closes, liaison APPROUVEE_SECURITE. */
    public void securiteApprouvee(Preuve preuve, LocalDate aujourdhui) {
        etat.setEtatApprobation(EtatApprobation.APPROUVEE);
        etat.setVisasAttendus(List.of());
        forcerCloture(Declencheurs.SECURITE_APPROUVEE, preuve, aujourdhui);
        // Ouverture de la fiche de changement (déclencheur change-record-referenced) : elle ne s'ouvre qu'après
        // l'approbation, alors que la fiche est déjà référencée et lue (elle fonde l'approbation, D-56) ; sa preuve
        // est cette référence, jamais redemandée.
        String reference = etat.getFicheChangementReference();
        if (reference != null) {
            parDeclencheur(Declencheurs.FICHE_CHANGEMENT)
                .stream()
                .filter(o -> o.getEtat().ouverte())
                .forEach(o -> terminer(o, Preuve.piece(reference, reference, empreinte(reference), aujourdhui, "PRODUCTION_MONETIQUE"), aujourdhui));
        }
        rafraichir();
    }

    /** certification.attestation-deposited.v1 (facultatif) : attestation à vérifier par la sécurité. */
    public void attestationDeposee(TypeAttestation t) {
        List<TypeAttestation> l = new ArrayList<>(etat.getAttestationsDeposees());
        if (!l.contains(t)) {
            l.add(t);
        }
        etat.setAttestationsDeposees(l);
    }

    /** certification.attestation-verified.v1 (facultatif) : l'attestation vérifiée ou rejetée n'est plus à vérifier. */
    public void attestationVerifiee(TypeAttestation t) {
        etat.setAttestationsDeposees(etat.getAttestationsDeposees().stream().filter(x -> x != t).toList());
        if (etat.getEtatApprobation() == EtatApprobation.DOSSIER_CONSTITUE) {
            etat.setEtatApprobation(EtatApprobation.EN_VERIFICATION);
        }
    }

    /** certification.visas-required.v1 et certification.visa-signed.v1 (facultatifs) : visas optionnels attendus. */
    public void visas(List<String> attendus) {
        etat.setVisasAttendus(attendus.stream().distinct().toList());
    }

    public void visaSigne(String nature) {
        etat.setVisasAttendus(etat.getVisasAttendus().stream().filter(v -> !v.equals(nature)).toList());
    }
}
