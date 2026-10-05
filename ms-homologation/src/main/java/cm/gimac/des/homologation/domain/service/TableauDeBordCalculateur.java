package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.parcours.Acteur;
import cm.gimac.des.homologation.domain.parcours.CatalogueServices;
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
 * déclencheur, particularité, fonction responsable), jamais par leur rang. Service de domaine pur.
 */
public class TableauDeBordCalculateur {

    private final ParcoursProjection projection;

    public TableauDeBordCalculateur(ParcoursProjection projection) {
        this.projection = projection;
    }

    /**
     * Tableau de bord d'un acteur pour ses fonctions. {@code institutionPortail} cloisonne la vue du
     * participant (RG-59) ; {@code conducteurs} donne le nom affiché du conducteur de chaque liaison.
     */
    public Vues.TableauActeur tableau(List<Parcours> tous, List<Fonction> fonctions, Acteur acteur, String institutionPortail, Map<UUID, String> conducteurs, LocalDate aujourdhui) {
        Set<Fonction> f = fonctions.isEmpty() ? Set.of() : Set.copyOf(fonctions);
        boolean participant = f.contains(Fonction.PARTICIPANT);
        List<Parcours> actives = tous.stream().filter(p -> p.etat().getStatut() != StatutParcours.CLOSE_SANS_SUITE).toList();
        List<Parcours> visibles = participant ? actives.stream().filter(p -> p.liaison().getInstitutionId().toString().equalsIgnoreCase(institutionPortail)).toList() : actives;
        List<Vues.Tache> taches = taches(visibles, f, participant, aujourdhui);
        List<Vues.Compteur> c = new ArrayList<>();
        Predicate<Parcours> miens = p -> acteur.id() != null && acteur.id().equals(p.liaison().getChefDeProjetId()) && !p.etat().isConducteurADesigner();

        if (f.contains(Fonction.CHEF_DDRM) || f.contains(Fonction.CHEF_DES)) {
            c.add(compteur("conducteursADesigner", actives.stream().filter(p -> p.etat().isConducteurADesigner()).count(), "warn", "/des/parcours?sansConducteur=1"));
        }
        if (f.contains(Fonction.CHEF_DDRM)) {
            c.add(compteur("liaisonsAmont", aEtape(actives, EtapeCode.CADRAGE, EtapeCode.LANCEMENT, EtapeCode.ATELIERS).size(), "info", "/des/parcours?etape=CADRAGE,LANCEMENT,ATELIERS"));
        }
        if (f.contains(Fonction.DDRM) || f.contains(Fonction.CHEF_DDRM)) {
            // Ateliers à tenir : première opération de l'étape des ateliers ouverte ; périmètres : opérations du cadrage ouvertes.
            var ateliers = ops(actives, (p, o) -> p.premiereDeLEtape(EtapeCode.ATELIERS).map(x -> x == o).orElse(false) && o.getEtat() == EtatOperationParcours.A_FAIRE);
            var perimetres = ops(actives, o -> o.getEtape() == EtapeCode.CADRAGE && o.getEtat() == EtatOperationParcours.A_FAIRE);
            c.add(compteur("ateliersATenir", ateliers.size(), "warn", versOps(ateliers, "ATELIERS")));
            c.add(compteur("perimetresAArreter", perimetres.size(), "warn", versOps(perimetres, "CADRAGE")));
        }
        if (f.contains(Fonction.CHEF_DES)) {
            var ressources = actives.stream().filter(p -> !p.operations(o -> o.porte(Geste.AFFECTER_RESSOURCES) && o.getEtat() == EtatOperationParcours.A_FAIRE).isEmpty()).toList();
            var pv = actives.stream().filter(p -> p.etat().getEtatPv() == EtatProcesVerbal.ETABLI || p.etat().getEtatPv() == EtatProcesVerbal.SIGNE_PARTICIPANT).toList();
            c.add(compteur("ressourcesAAffecter", ressources.size(), "warn", versLiaisons(ressources, "etapes", "CADRAGE,LANCEMENT")));
            c.add(compteur("enIntegrationTests", aEtape(actives, EtapeCode.INTEGRATION, EtapeCode.TESTS).size(), "info", "/des/parcours?etape=INTEGRATION,TESTS"));
            c.add(compteur("pvASigner", pv.size(), "warn", versLiaisons(pv, "decision", "CERTIFICATION")));
        }
        if (f.contains(Fonction.CONDUCTEUR)) {
            var aEtablir = actives.stream().filter(p -> p.parDeclencheur(Declencheurs.PV_ETABLI).stream().anyMatch(o -> o.getEtat() == EtatOperationParcours.A_FAIRE)).toList();
            c.add(compteur("mesProjets", actives.stream().filter(miens).count(), "info", "/des/parcours?mesProjets=1"));
            c.add(compteur("pvAEtablir", aEtablir.size(), "warn", versLiaisons(aEtablir, "decision", "CERTIFICATION")));
        }
        if (f.contains(Fonction.SNA)) {
            Predicate<OperationParcours> engagee = o -> !o.faite() && o.getEtat() != EtatOperationParcours.VERROUILLEE;
            var o15 = ops(actives, o -> o.getParticularite() == Particularite.ENTREE_ESSAIS && engagee.test(o));
            var o16 = ops(actives, o -> o.getParticularite() == Particularite.FIN_ESSAIS && engagee.test(o));
            var o17 = ops(actives, o -> o.porte(Geste.CONSTATER_COUVERTURE) && o.getEtat() == EtatOperationParcours.A_FAIRE);
            var constats = actives.stream().filter(p -> p.etat().getFicheEtat() == EtatFicheSuivi.RENSEIGNEE).toList();
            c.add(compteur("essaisEnCours", aEtape(actives, EtapeCode.TESTS).size(), "info", versOps(o16, "TESTS")));
            c.add(compteur("documentsParametrage", o15.size(), "warn", versOps(o15, "INTEGRATION")));
            c.add(compteur("couverturesAConstater", o17.size(), "warn", versOps(o17, "CERTIFICATION")));
            c.add(compteur("constatsMiseEnService", constats.size(), "warn", constats.size() == 1 ? "/des/parcours/" + constats.get(0).id() + "?onglet=mise-en-service" : "/des/parcours?etape=SUIVI"));
        }
        if (f.contains(Fonction.PRODUCTION_MONETIQUE)) {
            var dossiers = actives.stream().filter(p -> p.etat().getEtatApprobation() == EtatApprobation.EN_ATTENTE_DOSSIER && p.etat().getEtatPv() == EtatProcesVerbal.SIGNE).toList();
            var branche = ops(actives, o -> o.getSegment() == SegmentParcours.BRANCHE && o.fonctionR() == Fonction.PRODUCTION_MONETIQUE && o.getEtat() == EtatOperationParcours.A_FAIRE);
            var o31 = ops(actives, o -> o.getJalon() == Jalon.EN_PRODUCTION && o.getEtat() == EtatOperationParcours.A_FAIRE);
            c.add(compteur("dossiersChangement", dossiers.size(), "warn", dossiers.size() == 1 ? "/des/parcours/" + dossiers.get(0).id() + "?onglet=decision" : "/des/parcours?etape=APPROBATION_SECURITE"));
            c.add(compteur("operationsBranche", branche.size(), "info", versOps(branche, "MISE_EN_PRODUCTION")));
            c.add(compteur("rapportsAConsolider", o31.size(), "warn", versOps(o31, "MISE_EN_PRODUCTION")));
        }
        if (f.contains(Fonction.POLE_RESEAU)) {
            // Liens d'essai : opérations de l'intégration tenues par le pôle réseau, engagées.
            var liens = ops(actives, o -> o.getEtape() == EtapeCode.INTEGRATION && o.getResponsable() == Fonction.POLE_RESEAU && !o.faite() && o.getEtat() != EtatOperationParcours.VERROUILLEE);
            c.add(compteur("liensEssai", liens.size(), "warn", versOps(liens, "INTEGRATION")));
            c.add(compteur("operationsReseau", taches.size(), "info", taches.size() == 1 ? taches.get(0).lien() : null));
        }
        if (f.contains(Fonction.POLE_SYSTEME)) {
            // Scripts de dépôt : opérations du tronc aval dont l'oubli est sans symptôme, engagées.
            var o28 = ops(actives, o -> o.getSegment() == SegmentParcours.AVAL && o.isOubliSansSymptome() && !o.faite() && o.getEtat() != EtatOperationParcours.VERROUILLEE);
            c.add(compteur("scriptsDepot", o28.size(), "crit", versOps(o28, "MISE_EN_PRODUCTION")));
            c.add(compteur("operationsSysteme", taches.size(), "info", taches.size() == 1 ? taches.get(0).lien() : null));
        }
        if (f.contains(Fonction.RSSI)) {
            c.add(compteur("attestationsAVerifier", actives.stream().mapToLong(p -> p.etat().getAttestationsDeposees().size()).sum(), "warn", null));
            c.add(compteur("approbationsEnAttente", actives.stream().filter(p -> Set.of(EtatApprobation.DOSSIER_CONSTITUE, EtatApprobation.EN_VERIFICATION, EtatApprobation.VERIFIE_ARCHIVE).contains(p.etat().getEtatApprobation())).count(), "warn", null));
            // Cérémonies de clés : opérations sur une clé tenues par la sécurité, ouvertes.
            c.add(compteur("ceremoniesCles", ops(actives, o -> o.isCle() && o.getResponsable() == Fonction.RSSI && o.getEtat() == EtatOperationParcours.A_FAIRE).size(), "warn", null));
        }
        if (f.contains(Fonction.CONFORMITE)) {
            c.add(compteur("visasAttendus", actives.stream().filter(p -> p.etat().getVisasAttendus().contains("CONFORMITE")).count(), "warn", null));
            c.add(compteur("fintechsCobac", actives.stream().filter(p -> p.etat().isFintech()).count(), "info", null));
        }
        if (f.contains(Fonction.DG)) {
            c.add(compteur("visasAttendus", actives.stream().filter(p -> p.etat().getVisasAttendus().contains("DIRECTION_GENERALE")).count(), "warn", null));
        }
        if (participant) {
            c.add(compteur("tachesARealiser", taches.size(), "warn", null));
            c.add(compteur("pvASigner", visibles.stream().filter(p -> p.etat().getEtatPv() == EtatProcesVerbal.ETABLI || p.etat().getEtatPv() == EtatProcesVerbal.SIGNE_GIMAC).count(), "warn", null));
            c.add(compteur("attestationsAFournir", visibles.stream().mapToLong(p -> p.attestationsAFournir().size()).sum(), "warn", null));
            c.add(compteur("fichesSuivi", visibles.stream().filter(p -> p.etat().getFicheEtat() == EtatFicheSuivi.REMISE).count(), "warn", null));
        }
        c.add(compteur("tachesEnRetard", taches.stream().filter(t -> t.sla() == Sla.DEPASSE).count(), "crit", null));
        c.add(compteur("echeancesProches", taches.stream().filter(t -> t.sla() == Sla.ECHEANCE_PROCHE).count(), "warn", null));

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

    private Vues.LiaisonResume resumeVisible(Parcours p, boolean participant, Map<UUID, String> conducteurs, LocalDate aujourdhui) {
        String nom = participant ? "Conducteur de projet GIMAC" : conducteurs.get(p.liaison().getChefDeProjetId());
        return projection.resume(p, nom, aujourdhui);
    }

    /** Tâches : opérations en cours dont une fonction de l'acteur est responsable (R), validatrice (V) ou, pour le participant, externe (E). */
    List<Vues.Tache> taches(List<Parcours> visibles, Set<Fonction> f, boolean participant, LocalDate aujourdhui) {
        List<Vues.Tache> l = new ArrayList<>();
        for (Parcours p : visibles) {
            for (OperationParcours o : p.operations()) {
                if (!o.getEtat().enCours()) {
                    continue;
                }
                var raci = o.getRaci();
                Fonction r = o.fonctionR();
                boolean estR = r != null && (f.contains(r) || (r == Fonction.CHEF_DES && f.contains(Fonction.CHEFS_DEPARTEMENT)));
                boolean estE = participant && (r == Fonction.PARTICIPANT || raci.e().stream().anyMatch(x -> x.fonction() == Fonction.PARTICIPANT));
                boolean estV = o.getEtat() == EtatOperationParcours.EN_VALIDATION && raci.v() != null && f.contains(raci.v().fonction());
                if (!estR && !estE && !estV) {
                    continue;
                }
                Qualite q = estV ? Qualite.V : estE && !estR ? Qualite.E : Qualite.R;
                String lien;
                List<String> actions = null;
                if (participant) {
                    var g = projection.gesteParticipant(p, o);
                    lien = g.lien();
                    actions = g.actionsPermises();
                } else {
                    lien = ParcoursProjection.lienOperation(p, o);
                }
                l.add(new Vues.Tache(o.getId().toString(), p.id().toString(), p.etat().getReference(), p.liaison().getInstitutionNom(), o.getRang(), o.getEtape(), q, o.getEcheance(), Parcours.sla(o, aujourdhui), o.isOubliSansSymptome(), lien, actions));
            }
        }
        l.sort(Comparator.comparing(t -> t.echeance() == null ? "9" : t.echeance().toString()));
        return l;
    }

    private static Vues.Compteur compteur(String cle, long valeur, String ton, String lien) {
        return new Vues.Compteur(cle, valeur, valeur == 0 ? "ok" : ton, lien);
    }

    private static List<Parcours> aEtape(List<Parcours> actives, EtapeCode... etapes) {
        Set<EtapeCode> e = Set.of(etapes);
        return actives.stream().filter(p -> e.contains(p.etat().getEtapeCourante())).toList();
    }

    private record OpDe(Parcours p, OperationParcours o) {}

    private static List<OpDe> ops(List<Parcours> actives, Predicate<OperationParcours> filtre) {
        return ops(actives, (p, o) -> filtre.test(o));
    }

    private static List<OpDe> ops(List<Parcours> actives, BiPredicate<Parcours, OperationParcours> filtre) {
        return actives.stream().flatMap(p -> p.operations().stream().filter(o -> filtre.test(p, o)).map(o -> new OpDe(p, o))).toList();
    }

    /** Lien d'un compteur : l'opération même s'il n'en compte qu'une, sinon la liste des liaisons de l'étape. */
    private static String versOps(List<OpDe> ops, String etape) {
        return ops.size() == 1 ? ParcoursProjection.lienOperation(ops.get(0).p(), ops.get(0).o()) : "/des/parcours?etape=" + etape;
    }

    /** Lien d'un compteur de liaisons : l'onglet du dossier s'il n'y en a qu'une, sinon la liste filtrée par étape. */
    private static String versLiaisons(List<Parcours> l, String onglet, String etapes) {
        return l.size() == 1 ? "/des/parcours/" + l.get(0).id() + "?onglet=" + onglet : "/des/parcours?etape=" + etapes;
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
