package cm.gimac.des.iam.domain.compte;

import java.util.Set;

/** Données d'un compte créé par le super administrateur ou le RSSI (aucune inscription libre). */
public record NouveauCompte(
    String identifiant,
    String prenom,
    String nom,
    String email,
    String telephone,
    String fonction,
    String institutionId,
    Set<String> profils
) {
    public NouveauCompte {
        profils = profils == null ? Set.of() : Set.copyOf(profils);
    }
}
