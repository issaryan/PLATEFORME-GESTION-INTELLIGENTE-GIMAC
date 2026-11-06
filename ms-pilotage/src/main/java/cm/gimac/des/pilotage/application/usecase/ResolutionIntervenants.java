package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.Candidat;
import cm.gimac.des.pilotage.application.port.in.ProcessusUseCase.Candidats;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.MembreProfil;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.UniteMembres;
import cm.gimac.des.pilotage.domain.processus.Affectation;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.service.ControleProcessus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Résolution d'un intervenant à l'activation d'une étape (§2), avec l'organisation de MS-IAM : PERSONNE,
 * AU_LANCEMENT (désignation), CHEF_UNITE (responsable de l'unité), LANCEUR, N_PLUS_1_LANCEUR → cette personne si
 * elle est un compte interne actif ; sinon (personne partie, désactivée, aucun chef, aucun n+1) et pour PROFIL → par
 * profil (profil de l'intervenant ou de remplacement ; sans profil : tout membre de l'unité). Désignation hors de
 * l'unité : les porteurs actifs du profil sont cherchés dans l'unité (sous-unités comprises), à défaut dans tout le
 * GIMAC (unités actives) ; un seul porteur reçoit l'étape, plusieurs se la voient offrir (à prendre, dans l'unité ou,
 * hors de l'unité, sans unité visée) ; aucun nulle part : le chef de l'unité, à défaut le lanceur, reçoit l'étape pour
 * désigner une personne. L'unité est celle de l'intervenant, à défaut l'unité propriétaire.
 *
 * <p>Séparation des fonctions (D-99) : sauf si la définition l'autorise ({@code initiateurs.initiateurIntervenant}) ou
 * si l'intervenant est le lanceur lui-même (mode LANCEUR, choix explicite de la définition), l'initiateur de l'instance
 * ({@code exclu}) n'est jamais retenu : personne résolue égale à l'initiateur, elle est remplacée par le profil de
 * remplacement ; porteurs du profil comptés sans lui ; aucun autre porteur : chef de l'unité, à défaut (chef =
 * initiateur) le responsable du chef, à défaut tâche à prendre en attente d'une désignation par le pilote.
 */
@Component
class ResolutionIntervenants {

    /**
     * Affectation résolue ; {@code parti} : personne prévue mais partie ou inactive (null sinon) ; {@code note} :
     * explication pour l'historique (null si la résolution est nominale).
     */
    record Resolution(Affectation affectation, String parti, String note) {
        boolean nominative() {
            return affectation.personneId() != null;
        }
    }

    private final Equipes equipes;

    ResolutionIntervenants(Equipes equipes) {
        this.equipes = equipes;
    }

    Resolution resoudre(Intervenant i, String uniteProcessus, String lanceurId, String nPlus1, Map<String, String> designations, String cle) {
        return resoudre(i, uniteProcessus, lanceurId, nPlus1, designations, cle, null);
    }

    /**
     * Résolution avec séparation des fonctions (D-99) : {@code exclu} = initiateur à écarter (null : aucun, la
     * définition l'autorise). L'intervenant LANCEUR désigne explicitement l'initiateur : jamais écarté.
     */
    Resolution resoudre(Intervenant i, String uniteProcessus, String lanceurId, String nPlus1, Map<String, String> designations, String cle, String exclu) {
        String unite = i.uniteOu(uniteProcessus);
        String sauf = i.mode() == ModeIntervenant.LANCEUR ? null : exclu;
        return switch (i.mode()) {
            case PROFIL -> pourRole(i.profil(), unite, lanceurId, sauf);
            case PERSONNE -> personne(i, unite, i.personneId(), "Personne nommée", lanceurId, sauf);
            case AU_LANCEMENT -> personne(i, unite, designations.get(cle), "Personne désignée au lancement", lanceurId, sauf);
            case CHEF_UNITE -> personne(i, unite, chef(unite), "Chef de l'unité " + unite, lanceurId, sauf);
            case LANCEUR -> personne(i, unite, lanceurId, "Lanceur", lanceurId, null);
            case N_PLUS_1_LANCEUR -> personne(i, unite, nPlus1, "N+1 du lanceur", lanceurId, sauf);
        };
    }

    private Resolution personne(Intervenant i, String unite, String id, String qui, String lanceurId, String exclu) {
        if (id != null && id.equals(exclu)) {
            // D-99 : la personne prévue est l'initiateur de la demande, elle n'intervient pas sur sa propre demande.
            return parProfil(i.remplacement(), unite, lanceurId, null, qui + " initiateur de la demande (séparation des fonctions) : étape ", exclu);
        }
        if (id != null && equipes.interneActif(id)) {
            return new Resolution(Affectation.personne(id, unite), null, null);
        }
        String cause = id == null ? qui + " absent : étape " : qui + " partie ou inactive : remplacement, étape ";
        return parProfil(i.remplacement(), unite, lanceurId, id, cause, exclu);
    }

    /**
     * Résolution par profil (désignation hors de l'unité) : porteurs actifs dans l'unité, à défaut dans tout le GIMAC ;
     * un seul : à lui ; plusieurs : à prendre ; aucun : chef de l'unité, à défaut lanceur (à prendre par personne s'il
     * n'y a ni l'un ni l'autre). Sans profil : à prendre par tout membre de l'unité. {@code cause} : début de la note
     * (personne absente ou partie), null pour un intervenant PROFIL.
     */
    /**
     * D-138 : un intervenant PROFIL est adressé au rôle, jamais à une personne, dès qu'un porteur actif est dans
     * l'unité, même seul : la tâche reste visible et faisable par tous les porteurs, y compris ceux qui reçoivent le
     * rôle après l'activation (résolution à la lecture). Sans porteur dans l'unité : règles de {@link #parProfil}.
     */
    Resolution pourRole(String profil, String unite, String lanceurId, String exclu) {
        if (profil != null && !sans(actifsDans(unite, profil), exclu).isEmpty()) {
            return new Resolution(Affectation.profil(profil, unite), null, null);
        }
        return parProfil(profil, unite, lanceurId, null, null, exclu);
    }

    Resolution parProfil(String profil, String unite, String lanceurId, String parti, String cause) {
        return parProfil(profil, unite, lanceurId, parti, cause, null);
    }

    /** Résolution par profil sans {@code exclu} (initiateur écarté par la séparation des fonctions, D-99 ; null : aucun). */
    Resolution parProfil(String profil, String unite, String lanceurId, String parti, String cause, String exclu) {
        String debut = cause == null ? "Étape " : cause;
        if (profil == null) {
            return new Resolution(Affectation.profil(null, unite), parti, cause == null ? null : debut + "à prendre par tout membre de l'unité " + unite);
        }
        List<MembreProfil> dans = sans(actifsDans(unite, profil), exclu);
        if (dans.size() > 1) {
            return new Resolution(Affectation.profil(profil, unite), parti, cause == null ? null : debut + "à prendre par le profil " + profil + " (unité " + unite + ")");
        }
        if (dans.size() == 1) {
            MembreProfil m = dans.get(0);
            return new Resolution(Affectation.personne(m.id(), unite), parti, debut + "confiée à " + nom(m) + ", seul porteur actif du profil " + profil + " dans l'unité " + unite);
        }
        List<MembreProfil> ailleurs = sans(equipes.porteursActifs(profil), exclu);
        if (ailleurs.size() == 1) {
            MembreProfil m = ailleurs.get(0);
            return new Resolution(
                Affectation.personne(m.id(), unite),
                parti,
                debut + "confiée à " + nom(m) + ", seul porteur actif du profil " + profil + ", hors de l'unité " + unite + " (" + uniteDe(m) + ")"
            );
        }
        if (ailleurs.size() > 1) {
            return new Resolution(
                Affectation.profil(profil, null),
                parti,
                debut + "à prendre par les " + ailleurs.size() + " porteurs du profil " + profil + ", hors de l'unité " + unite + " (" + repartition(ailleurs) + ")"
            );
        }
        String chef = chef(unite);
        String superieur = null;
        String designateur;
        if (chef != null && !chef.equals(exclu) && equipes.interneActif(chef)) {
            designateur = chef;
        } else if (chef != null && chef.equals(exclu)) {
            // D-99 : le chef de l'unité est l'initiateur ; son propre responsable désigne.
            superieur = equipes.rattachement(chef).map(r -> r.responsable() == null ? null : r.responsable().id()).filter(r -> !r.equals(exclu)).filter(equipes::interneActif).orElse(null);
            designateur = superieur;
        } else {
            designateur = exclu == null ? lanceurId : null;
        }
        String aucun = exclu == null
            ? "Aucune personne active ne porte le profil " + profil + " dans le GIMAC : " + (cause == null ? "étape " : debut)
            : "Aucun porteur actif du profil " + profil + " hors de l'initiateur de la demande (séparation des fonctions) : " + (cause == null ? "étape " : debut);
        if (designateur == null) {
            return new Resolution(Affectation.profil(profil, unite), parti, aucun + "à prendre, en attente d'une désignation");
        }
        String qui = designateur.equals(chef) ? "au chef de l'unité " + unite : designateur.equals(superieur) ? "au responsable du chef de l'unité " + unite : "au lanceur";
        return new Resolution(Affectation.personne(designateur, unite), parti, aucun + "confiée " + qui + " pour désigner une personne");
    }

    private static List<MembreProfil> sans(List<MembreProfil> l, String exclu) {
        return exclu == null ? l : l.stream().filter(m -> !exclu.equals(m.id())).toList();
    }

    /** Membres actifs de l'unité (sous-unités comprises) portant le profil (null : tous). */
    private List<MembreProfil> actifsDans(String unite, String profil) {
        return unite == null ? List.of() : equipes.membresUnite(unite, profil).map(UniteMembres::actifs).orElse(List.of());
    }

    /** Responsable (chef) de l'unité, ou null. */
    String chef(String unite) {
        return unite == null ? null : equipes.membresUnite(unite, null).map(UniteMembres::responsableId).orElse(null);
    }

    /**
     * Porteurs actifs du profil (null : tous les membres), sauf {@code sauf} : dans l'unité, ou, unité nulle (étape
     * offerte hors de l'unité), dans tout le GIMAC.
     */
    List<String> porteurs(String unite, String profil, String sauf) {
        List<MembreProfil> l = unite == null ? equipes.porteursActifs(profil) : actifsDans(unite, profil);
        return l.stream().map(MembreProfil::id).filter(id -> !id.equals(sauf)).toList();
    }

    /** Le compte, actif, porte le profil quelque part dans le GIMAC (unité active ou sans rattachement). */
    boolean portePartout(String compteId, String profil) {
        return compteId != null && profil != null && equipes.porteursActifs(profil).stream().anyMatch(m -> compteId.equals(m.id()));
    }

    /** Destinataires d'une affectation : la personne, ou les porteurs du profil, à défaut le chef de l'unité. */
    List<String> destinataires(Affectation a, String sauf) {
        return destinataires(a, sauf, null);
    }

    /** Destinataires sans {@code sauf} (l'acteur) ni {@code exclu} (initiateur écarté par la séparation des fonctions, D-99). */
    List<String> destinataires(Affectation a, String sauf, String exclu) {
        if (a == null) {
            return List.of();
        }
        if (a.personneId() != null) {
            return Objects.equals(a.personneId(), sauf) ? List.of() : List.of(a.personneId());
        }
        List<String> p = porteurs(a.unite(), a.profil(), sauf).stream().filter(x -> !x.equals(exclu)).toList();
        if (!p.isEmpty()) {
            return p;
        }
        String chef = chef(a.unite());
        return chef == null || chef.equals(sauf) || chef.equals(exclu) ? List.of() : List.of(chef);
    }

    /**
     * Le compte répond à l'affectation : personne nommée, membre actif de l'unité portant le profil, ou, sans unité
     * (étape offerte hors de l'unité), porteur actif du profil dans le GIMAC.
     */
    boolean affecte(String compteId, Affectation a) {
        if (a == null || compteId == null) {
            return false;
        }
        if (a.personneId() != null) {
            return a.personneId().equals(compteId);
        }
        if (a.unite() == null) {
            return portePartout(compteId, a.profil());
        }
        return equipes.porteProfilDans(compteId, a.unite(), a.profil());
    }

    /**
     * Candidats d'un intervenant : porteurs actifs du profil dans l'unité, puis ailleurs dans le GIMAC (triés par
     * nom) ; sans profil : membres actifs de l'unité seulement.
     */
    Candidats candidats(String unite, String profil) {
        return candidats(unite, profil, null);
    }

    /**
     * Candidats sans {@code exclu} (D-99 : initiateur de l'instance, jamais proposé sauf si la définition l'autorise) ;
     * {@code exclus} de la réponse le nomme pour que l'interface l'écarte aussi de l'annuaire.
     */
    Candidats candidats(String unite, String profil, String exclu) {
        List<MembreProfil> dans = sans(actifsDans(unite, profil), exclu);
        Set<String> ids = dans.stream().map(MembreProfil::id).collect(Collectors.toSet());
        List<MembreProfil> ailleurs = profil == null
            ? List.of()
            : sans(equipes.porteursActifs(profil), exclu).stream().filter(m -> !ids.contains(m.id())).toList();
        return new Candidats(unite, profil, candidats(dans), candidats(ailleurs), exclu == null ? List.of() : List.of(exclu));
    }

    private static List<Candidat> candidats(List<MembreProfil> l) {
        return l
            .stream()
            .sorted(Comparator.comparing((MembreProfil m) -> m.nomAffiche() == null ? m.id() : m.nomAffiche()).thenComparing(MembreProfil::id))
            .map(m -> new Candidat(m.id(), m.nomAffiche(), m.fonction(), m.unite()))
            .toList();
    }

    private static String nom(MembreProfil m) {
        return m.nomAffiche() == null || m.nomAffiche().isBlank() ? m.id() : m.nomAffiche();
    }

    private static String uniteDe(MembreProfil m) {
        return m.unite() == null ? "sans unité" : "unité " + m.unite();
    }

    /** Répartition des porteurs par unité : « SNA : 2, DDRM : 1 ». */
    static String repartition(List<MembreProfil> l) {
        Map<String, Long> n = l.stream().collect(Collectors.groupingBy(m -> m.unite() == null ? "sans unité" : m.unite(), TreeMap::new, Collectors.counting()));
        return n.entrySet().stream().map(e -> e.getKey() + " : " + e.getValue()).collect(Collectors.joining(", "));
    }

    /**
     * Clé d'une désignation au lancement : CODE_ETAPE (exécutant), CODE_ETAPE.validateur,
     * CODE_ETAPE.signature.CODE (signataire).
     */
    static String cle(String etape, String element) {
        return ControleProcessus.INTERVENANT.equals(element) ? etape : etape + "." + element;
    }
}
