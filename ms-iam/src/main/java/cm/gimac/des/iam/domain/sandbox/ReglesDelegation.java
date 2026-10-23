package cm.gimac.des.iam.domain.sandbox;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatEffectif;
import cm.gimac.des.iam.domain.service.ReglesSandbox;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Délégations temporaires de la sandbox (D-119, US-EXI-04, US-EXI-24, algorithme 6.3 du CC de la sandbox), service de
 * domaine pur : bornes de date et de durée, périmètre d'un mandat, composants d'un accès de prestataire, état effectif à
 * un instant (l'accès cesse à la date de fin, quel que soit le planificateur).
 */
public final class ReglesDelegation {

    /** Composants techniques de la sandbox (onze services et le cœur bancaire simulé), cibles d'un accès de prestataire. */
    public static final List<String> COMPOSANTS = List.of(
        "SBX-ESPACE",
        "SBX-RACCORDEMENT",
        "SBX-REFERENTIEL",
        "SBX-DONNEES",
        "SBX-SIMULATION",
        "SBX-MOBILE",
        "SBX-ISO20022",
        "SBX-CARTE",
        "SBX-CAMPAGNE",
        "SBX-TRACE",
        "SBX-LECTURE",
        "MIFOS"
    );

    private ReglesDelegation() {}

    /** État effectif d'une délégation à l'instant {@code t}. */
    public static EtatEffectif etatEffectif(EtatDelegation etat, Instant debut, Instant fin, Instant t) {
        return switch (etat) {
            case REVOQUEE -> EtatEffectif.REVOQUEE;
            case REFUSEE -> EtatEffectif.REFUSEE;
            case DEMANDEE -> fin.isAfter(t) ? EtatEffectif.DEMANDEE : EtatEffectif.ECHUE;
            case ACCORDEE -> !fin.isAfter(t) ? EtatEffectif.ECHUE : t.isBefore(debut) ? EtatEffectif.PROGRAMMEE : EtatEffectif.ACTIVE;
        };
    }

    /**
     * Bornes d'une délégation : début avant la fin (422 IAM_SBX_PERIODE_INVALIDE), fin à venir (422 IAM_SBX_PERIODE_ECHUE),
     * durée au plus égale au maximum paramétré (422 IAM_SBX_DUREE_EXCESSIVE, avec la durée maximale).
     */
    public static void verifierBornes(Instant debut, Instant fin, Instant maintenant, Duration dureeMax) {
        if (debut == null || fin == null || !debut.isBefore(fin)) {
            throw IamException.nonTraitable("IAM_SBX_PERIODE_INVALIDE", "La date de début précède la date de fin", Map.of());
        }
        if (!fin.isAfter(maintenant)) {
            throw IamException.nonTraitable("IAM_SBX_PERIODE_ECHUE", "La date de fin est déjà passée", Map.of());
        }
        if (Duration.between(debut, fin).compareTo(dureeMax) > 0) {
            throw IamException.nonTraitable("IAM_SBX_DUREE_EXCESSIVE", "Durée supérieure au maximum paramétré", Map.of("dureeMax", dureeMax.toString()));
        }
    }

    /**
     * Droits accordables par un mandat : permissions de la sandbox portées par un rôle de portée institution, hors
     * validation de verdict (réservée au GIMAC).
     */
    public static Set<String> accordables(Set<String> rolesInstitution, Map<String, Set<String>> compositions) {
        Set<String> s = new TreeSet<>();
        rolesInstitution.forEach(r -> compositions.getOrDefault(r, Set.of()).stream().filter(p -> ReglesSandbox.conforme(p) && !ReglesSandbox.validation(p)).forEach(s::add));
        return s;
    }

    /**
     * Périmètre d'un mandat : au moins un droit (400 IAM_SBX_PERIMETRE_REQUIS), jamais de validation (422
     * IAM_SBX_VALIDATION_RESERVEE), tous accordables par l'institution (403 IAM_SBX_PERIMETRE_HORS_PORTEE).
     */
    public static Set<String> verifierPerimetre(Collection<String> demandees, Set<String> accordables) {
        if (demandees == null || demandees.isEmpty()) {
            throw IamException.invalide("IAM_SBX_PERIMETRE_REQUIS", "Au moins un droit délégué", Map.of());
        }
        ReglesSandbox.verifierValidationReservee(true, demandees);
        TreeSet<String> hors = new TreeSet<>(demandees);
        hors.removeAll(accordables);
        if (!hors.isEmpty()) {
            throw IamException.interdit("IAM_SBX_PERIMETRE_HORS_PORTEE", "Droit hors de la portée de l'institution", Map.of("permissions", List.copyOf(hors)));
        }
        return new TreeSet<>(demandees);
    }

    /** Composants d'un accès de prestataire : au moins un, tous connus (400 IAM_SBX_COMPOSANT_INCONNU). */
    public static Set<String> verifierComposants(Collection<String> composants) {
        if (composants == null || composants.isEmpty()) {
            throw IamException.invalide("IAM_SBX_COMPOSANT_REQUIS", "Au moins un composant technique", Map.of());
        }
        TreeSet<String> inconnus = new TreeSet<>(composants);
        inconnus.removeAll(COMPOSANTS);
        if (!inconnus.isEmpty()) {
            throw IamException.invalide("IAM_SBX_COMPOSANT_INCONNU", "Composant technique inconnu", Map.of("composants", List.copyOf(inconnus)));
        }
        return new TreeSet<>(composants);
    }

    /**
     * Prolongation : délégation accordée et non échue (409 IAM_SBX_DELEGATION_CLOSE), nouvelle fin postérieure à
     * l'actuelle (422 IAM_SBX_PROLONGATION_INVALIDE), durée totale sous le maximum (422 IAM_SBX_DUREE_EXCESSIVE).
     */
    public static void verifierProlongation(Delegation d, Instant nouvelleFin, Instant maintenant, Duration dureeMax) {
        EtatEffectif e = d.etatA(maintenant);
        if (e != EtatEffectif.ACTIVE && e != EtatEffectif.PROGRAMMEE) {
            throw IamException.conflit("IAM_SBX_DELEGATION_CLOSE", "Délégation close, échue ou non accordée", Map.of("etat", e.name()));
        }
        if (nouvelleFin == null || !nouvelleFin.isAfter(d.fin())) {
            throw IamException.nonTraitable("IAM_SBX_PROLONGATION_INVALIDE", "La nouvelle fin suit la fin actuelle", Map.of());
        }
        verifierBornes(d.debut(), nouvelleFin, maintenant, dureeMax);
    }

    /** Décision sur une demande d'accès : demande encore ouverte (409 IAM_SBX_DELEGATION_CLOSE). */
    public static void verifierDemandeOuverte(Delegation d, Instant maintenant) {
        if (d.etatA(maintenant) != EtatEffectif.DEMANDEE) {
            throw IamException.conflit("IAM_SBX_DELEGATION_CLOSE", "Demande déjà décidée ou échue", Map.of("etat", d.etatA(maintenant).name()));
        }
    }
}
