package cm.gimac.des.homologation.domain.parcours;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fonction du parcours (CR chapitre 3, D-58), tenue en qualité R, V, C, I, E ou déposant : code d'une entrée du
 * catalogue des fonctions (D-91, {@link CatalogueFonctions}), qui porte son libellé, les profils Keycloak qui la
 * confèrent et son caractère externe. Les seize fonctions reprises de l'ancienne énumération gardent leurs
 * constantes et leurs codes (mêmes valeurs JSON et en base).
 */
public final class Fonction implements Comparable<Fonction>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Code d'une fonction : majuscule, puis 1 à 39 majuscules, chiffres ou soulignés. */
    public static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,39}$");

    public static final Fonction DDRM = new Fonction("DDRM");
    public static final Fonction CHEF_DDRM = new Fonction("CHEF_DDRM");
    public static final Fonction CHEF_DES = new Fonction("CHEF_DES");
    public static final Fonction CHEFS_DEPARTEMENT = new Fonction("CHEFS_DEPARTEMENT");
    public static final Fonction CONDUCTEUR = new Fonction("CONDUCTEUR");
    public static final Fonction SNA = new Fonction("SNA");
    public static final Fonction PRODUCTION_MONETIQUE = new Fonction("PRODUCTION_MONETIQUE");
    public static final Fonction POLE_RESEAU = new Fonction("POLE_RESEAU");
    public static final Fonction POLE_SYSTEME = new Fonction("POLE_SYSTEME");
    public static final Fonction RSSI = new Fonction("RSSI");
    public static final Fonction CONFORMITE = new Fonction("CONFORMITE");
    public static final Fonction DG = new Fonction("DG");
    public static final Fonction EXPLOITATION = new Fonction("EXPLOITATION");
    public static final Fonction PARTICIPANT = new Fonction("PARTICIPANT");
    public static final Fonction BANQUE_PARTENAIRE = new Fonction("BANQUE_PARTENAIRE");
    public static final Fonction PERSONNALISATEUR = new Fonction("PERSONNALISATEUR");

    /** Fonctions de l'ancienne énumération, dans leur ordre. */
    static final List<Fonction> REPRISES = List.of(
        DDRM,
        CHEF_DDRM,
        CHEF_DES,
        CHEFS_DEPARTEMENT,
        CONDUCTEUR,
        SNA,
        PRODUCTION_MONETIQUE,
        POLE_RESEAU,
        POLE_SYSTEME,
        RSSI,
        CONFORMITE,
        DG,
        EXPLOITATION,
        PARTICIPANT,
        BANQUE_PARTENAIRE,
        PERSONNALISATEUR
    );

    private final String code;

    private Fonction(String code) {
        this.code = code;
    }

    /** Fonction de ce code (présente ou non au catalogue) ; un code mal formé est refusé. */
    @JsonCreator
    public static Fonction valueOf(String code) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Code de fonction invalide : " + code);
        }
        for (Fonction f : REPRISES) {
            if (f.code.equals(code)) {
                return f;
            }
        }
        return new Fonction(code);
    }

    /** Fonction de ce code si elle figure au catalogue (active ou non), sinon vide ; un code mal formé est vide aussi. */
    public static Optional<Fonction> duCatalogue(String code) {
        if (code == null || !CODE.matcher(code).matches()) {
            return Optional.empty();
        }
        return CatalogueFonctions.trouver(code).map(d -> valueOf(d.code()));
    }

    /** La fonction figure au catalogue (active ou non). */
    public boolean connue() {
        return CatalogueFonctions.trouver(code).isPresent();
    }

    /** Fonctions du catalogue, actives ou non : les reprises dans leur ordre, puis les autres par code. */
    public static Fonction[] values() {
        return CatalogueFonctions.toutes().stream().map(d -> valueOf(d.code())).toArray(Fonction[]::new);
    }

    @JsonValue
    public String name() {
        return code;
    }

    public CatalogueFonctions.Definition definition() {
        return CatalogueFonctions.definition(this);
    }

    /** Profils Keycloak qui confèrent la fonction ; le premier sert à chercher les intervenants dans l'annuaire. */
    public List<String> profils() {
        return definition().profils();
    }

    /**
     * Permissions qui caractérisent la fonction (D-94) : ses porteurs, lus par permission dans MS-IAM, sont les
     * candidats d'une fonction à affecter (tâche à prendre de MS-PILOTAGE) ; vide : aucune.
     */
    public List<String> permissions() {
        return definition().permissions();
    }

    /** Libellé français de la fonction (catalogue du modélisateur, tâches). */
    public String libelle() {
        return definition().libelle();
    }

    /** Fonction tenue par un tiers : le participant, sa banque partenaire, le personnalisateur, ou une fonction externe du catalogue. */
    public boolean externe() {
        return definition().externe();
    }

    /** Fonction reprise de l'ancienne énumération. */
    public boolean reprise() {
        return REPRISES.contains(this);
    }

    /** Fonction d'un profil (ROLE_*), le cas échéant. CHEFS_DEPARTEMENT n'est porté par aucun profil propre. */
    public static Optional<Fonction> duProfil(String profil) {
        return CatalogueFonctions.toutes()
            .stream()
            .filter(d -> !CHEFS_DEPARTEMENT.code.equals(d.code()) && d.profils().contains(profil))
            .map(d -> valueOf(d.code()))
            .findFirst();
    }

    /** Fonctions portées par une session (un utilisateur peut détenir plusieurs profils), lues au catalogue. */
    public static Set<Fonction> desProfils(Collection<String> autorites) {
        Set<Fonction> fonctions = new LinkedHashSet<>();
        // Un profil peut conférer plusieurs fonctions (production monétique : aussi l'exploitation).
        for (CatalogueFonctions.Definition d : CatalogueFonctions.toutes()) {
            if (!CHEFS_DEPARTEMENT.code.equals(d.code()) && d.profils().stream().anyMatch(autorites::contains)) {
                fonctions.add(valueOf(d.code()));
            }
        }
        return fonctions;
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Fonction f && f.code.equals(code));
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    /** Les fonctions reprises d'abord, dans leur ordre historique, puis les autres par code. */
    @Override
    public int compareTo(Fonction o) {
        int a = REPRISES.indexOf(this);
        int b = REPRISES.indexOf(o);
        if (a >= 0 || b >= 0) {
            return Integer.compare(a < 0 ? Integer.MAX_VALUE : a, b < 0 ? Integer.MAX_VALUE : b);
        }
        return code.compareTo(o.code);
    }
}
