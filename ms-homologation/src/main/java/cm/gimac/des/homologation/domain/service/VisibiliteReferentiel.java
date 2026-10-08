package cm.gimac.des.homologation.domain.service;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visibilité du référentiel des opérations (D-89, docs/lot1/referentiel-droits-etapes.md §1). Les éditeurs et
 * les porteurs de {@code HOM_REFERENTIEL_READ_ALL} voient tout ({@link Filtre#COMPLET}) ; tout autre lecteur ne
 * reçoit que les opérations dont une de ses fonctions (déduites des profils du jeton, catalogue de D-77) est R,
 * V, C, I, E ou déposant d'un livrable, plus leurs prédécesseurs et successeurs directs en fantômes (code et
 * libellé seulement) ({@link Filtre#CONCERNE}).
 */
public final class VisibiliteReferentiel {

    public static final String LECTURE_COMPLETE = "HOM_REFERENTIEL_READ_ALL";

    /** Permissions qui ouvrent la lecture complète : éditer, publier, migrer (impact sur toutes les opérations), tout lire. */
    public static final Set<String> PERMISSIONS_COMPLETES = Set.of(
        LECTURE_COMPLETE,
        "HOM_REFERENTIEL_EDIT",
        "HOM_REFERENTIEL_PUBLISH",
        "HOM_REFERENTIEL_MIGRATE",
        // Administration des référentiels métier, tous domaines (D-92).
        "REF_METIER_ADMIN"
    );

    public enum Filtre {
        COMPLET,
        CONCERNE,
    }

    /** Opérations retenues : visibles en entier, ou fantômes (voisines directes d'une opération visible). */
    public record Vue(Filtre filtre, Set<String> visibles, Set<String> fantomes) {
        public static Vue complete(Collection<ReferenceOperation> ops) {
            return new Vue(Filtre.COMPLET, ops.stream().map(ReferenceOperation::getCode).collect(Collectors.toSet()), Set.of());
        }

        public boolean visible(String code) {
            return visibles.contains(code);
        }

        public boolean fantome(String code) {
            return fantomes.contains(code);
        }

        public boolean retenue(String code) {
            return visible(code) || fantome(code);
        }

        public boolean complete() {
            return filtre == Filtre.COMPLET;
        }
    }

    private VisibiliteReferentiel() {}

    public static boolean lectureComplete(Collection<String> autorites) {
        return autorites.stream().anyMatch(PERMISSIONS_COMPLETES::contains);
    }

    /** Fonctions du lecteur ; un chef de département tient aussi la fonction collective des chefs de département. */
    public static Set<Fonction> fonctions(Collection<String> autorites) {
        Set<Fonction> f = new java.util.LinkedHashSet<>();
        f.addAll(Fonction.desProfils(autorites));
        if (f.contains(Fonction.CHEF_DES) || f.contains(Fonction.CHEF_DDRM)) {
            f.add(Fonction.CHEFS_DEPARTEMENT);
        }
        return f;
    }

    /** Une des fonctions est R, V, C, I, E, déposant d'un livrable ou signataire de l'opération (valeurs effectives). */
    public static boolean concerne(ReferenceOperation o, Set<Fonction> fonctions) {
        if (fonctions.isEmpty()) {
            return false;
        }
        Set<Fonction> qualites = new java.util.LinkedHashSet<>();
        qualites.add(o.responsableEffectif());
        qualites.add(o.validateurEffectif());
        qualites.addAll(o.getConsultes());
        qualites.addAll(o.informesEffectifs());
        qualites.addAll(o.getExternes());
        o.getLivrables().stream().map(Livrable::deposant).filter(d -> d != null).forEach(qualites::add);
        // Signataires (D-91) : la personne qui signe voit l'opération.
        o.getSignatures().stream().map(cm.gimac.des.homologation.domain.parcours.SignatureOperation::signataire).filter(d -> d != null).forEach(qualites::add);
        return qualites.stream().anyMatch(fonctions::contains);
    }

    public static Vue pour(Collection<String> autorites, List<ReferenceOperation> ops) {
        if (lectureComplete(autorites)) {
            return Vue.complete(ops);
        }
        Set<Fonction> fonctions = fonctions(autorites);
        Set<String> visibles = ops.stream().filter(o -> concerne(o, fonctions)).map(ReferenceOperation::getCode).collect(Collectors.toSet());
        Set<String> fantomes = new HashSet<>();
        for (ReferenceOperation o : ops) {
            if (visibles.contains(o.getCode())) {
                o.predecesseursListe().stream().filter(p -> !visibles.contains(p)).forEach(fantomes::add);
            } else if (o.predecesseursListe().stream().anyMatch(visibles::contains)) {
                fantomes.add(o.getCode());
            }
        }
        Set<String> codes = ops.stream().map(ReferenceOperation::getCode).collect(Collectors.toSet());
        fantomes.retainAll(codes);
        return new Vue(Filtre.CONCERNE, Set.copyOf(visibles), Set.copyOf(fantomes));
    }
}
