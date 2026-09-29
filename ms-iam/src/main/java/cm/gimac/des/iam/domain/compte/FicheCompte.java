package cm.gimac.des.iam.domain.compte;

/** Données descriptives modifiables d'un compte (par l'administration ou par son titulaire). */
public record FicheCompte(String prenom, String nom, String email, String telephone, String fonction, String langue, String institutionId) {}
