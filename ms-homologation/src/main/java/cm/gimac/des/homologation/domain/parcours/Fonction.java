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
    EXPLOITATION("ROLE_EXPLOITATION"),
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
        autorites.forEach(a -> duProfil(a).ifPresent(fonctions::add));
        return fonctions;
    }
}
