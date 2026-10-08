package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.service.ReglesProfils;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Comptes internes actifs dont un profil porte une permission (D-94 §3, docs/lot1/ecrans-par-configuration.md §6.6) :
 * la composition est lue dans le fournisseur d'identité à chaque appel, profils créés (D-81) et recompositions faites
 * dans la console compris. Aucun nom de profil n'y intervient.
 *
 * <p>Trois lectures de l'annuaire, quel que soit le nombre de comptes : compositions des profils, profils de chaque
 * compte, comptes actifs (allégés, complétés par leurs profils).
 */
public final class PorteursPermission {

    private PorteursPermission() {}

    /** Porteurs triés par nom affiché puis identifiant ; vide si aucun profil ne compose la permission. */
    public static List<Compte> lire(Annuaire annuaire, String permission) {
        Set<String> profils = ReglesProfils.profilsPortant(permission, annuaire.compositions());
        if (profils.isEmpty()) {
            return List.of();
        }
        Map<String, Set<String>> parCompte = annuaire.profilsParCompte();
        return annuaire
            .actifs()
            .stream()
            .filter(c -> c.actif() && c.interne())
            .filter(c -> parCompte.getOrDefault(c.id(), Set.of()).stream().anyMatch(profils::contains))
            .map(c -> c.avecProfils(parCompte.get(c.id())))
            .sorted(Comparator.comparing((Compte c) -> c.nomAffiche().toLowerCase(Locale.ROOT)).thenComparing(Compte::id))
            .toList();
    }
}
