package cm.gimac.des.gateway.domain.supervision;

/** Service supervisé : nom (clé de gimac.services) et adresse de base. */
public record ServiceCible(String nom, String url) {}
