package cm.gimac.des.gateway.domain.supervision;

/** Niveau d'un logger : configuré (null s'il est hérité) et effectif. */
public record NiveauLogger(String nom, String niveauConfigure, String niveauEffectif) {}
