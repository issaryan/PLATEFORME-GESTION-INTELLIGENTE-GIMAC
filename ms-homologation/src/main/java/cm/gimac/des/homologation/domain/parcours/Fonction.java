package cm.gimac.des.homologation.domain.parcours;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fonctions du parcours (CR chapitre 3, D-58) et profils Keycloak qui les portent
 * (frontend/packages/lot1/src/permissions.ts, FONCTION_DU_ROLE).
 */
public enum Fonction {
    DDRM("ROLE_CHARGE_DDRM"),
    CHEF_DDRM("ROLE_CHEF_DDRM"),
    CHEF_DES("ROLE_CHEF_DES"),
    CHEFS_DEPARTEMENT("ROLE_CHEF_DES"),
    CONDUCTEUR("ROLE_CONDUCTEUR_PROJET"),
    SNA("ROLE_SNA", "ROLE_INGENIEUR_HOMOLOGATION"),
    PRODUCTION_MONETIQUE("ROLE_PRODUCTION_MONETIQUE"),
    POLE_RESEAU("ROLE_POLE_RESEAU"),
    POLE_SYSTEME("ROLE_POLE_SYSTEME"),
    RSSI("ROLE_RSSI"),
    CONFORMITE("ROLE_CONFORMITE"),
    DG("ROLE_DG"),
    // Exploitation et supervision (CR §3) : tenue par la production monétique, aucun profil propre (D-58).
    EXPLOITATION("ROLE_PRODUCTION_MONETIQUE"),
    PARTICIPANT("ROLE_CORRESPONDANT"),
    BANQUE_PARTENAIRE,
    PERSONNALISATEUR;

    private final List<String> profils;

    Fonction(String... profils) {
        this.profils = List.of(profils);
    }

    /** Profils Keycloak qui confèrent la fonction ; le premier sert à chercher les intervenants dans l'annuaire. */
    public List<String> profils() {
        return profils;
    }

    /** Libellé français de la fonction (catalogue du modélisateur, tâches). */
    public String libelle() {
        return switch (this) {
            case DDRM -> "Développement du Réseau et du Marché";
            case CHEF_DDRM -> "Chef du département DDRM";
            case CHEF_DES -> "Chef du département DES";
            case CHEFS_DEPARTEMENT -> "Chefs des deux départements";
            case CONDUCTEUR -> "Conducteur de projet";
            case SNA -> "Normes et Agréments";
            case PRODUCTION_MONETIQUE -> "Production monétique";
            case POLE_RESEAU -> "Pôle réseau";
            case POLE_SYSTEME -> "Pôle système";
            case RSSI -> "Sécurité des systèmes d'information";
            case CONFORMITE -> "Conformité";
            case DG -> "Direction générale";
            case EXPLOITATION -> "Exploitation et Supervision";
            case PARTICIPANT -> "Participant";
            case BANQUE_PARTENAIRE -> "Banque partenaire";
            case PERSONNALISATEUR -> "Personnalisateur";
        };
    }

    /** Fonction tenue par un tiers : le participant, sa banque partenaire, le personnalisateur. */
    public boolean externe() {
        return this == PARTICIPANT || this == BANQUE_PARTENAIRE || this == PERSONNALISATEUR;
    }

    /** Fonction d'un profil (ROLE_*), le cas échéant. CHEFS_DEPARTEMENT n'est porté par aucun profil propre. */
    public static Optional<Fonction> duProfil(String profil) {
        for (Fonction f : values()) {
            if (f != CHEFS_DEPARTEMENT && f.profils.contains(profil)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    /** Fonctions portées par une session (un utilisateur peut détenir plusieurs profils). */
    public static Set<Fonction> desProfils(Collection<String> autorites) {
        Set<Fonction> fonctions = EnumSet.noneOf(Fonction.class);
        // Un profil peut conférer plusieurs fonctions (production monétique : aussi l'exploitation).
        for (Fonction f : values()) {
            if (f != CHEFS_DEPARTEMENT && f.profils.stream().anyMatch(autorites::contains)) {
                fonctions.add(f);
            }
        }
        return fonctions;
    }
}
