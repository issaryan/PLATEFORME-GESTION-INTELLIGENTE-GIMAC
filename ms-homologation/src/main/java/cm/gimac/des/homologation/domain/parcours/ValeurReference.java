package cm.gimac.des.homologation.domain.parcours;

/** Valeur de référence relisible et comparable portée par une preuve (CR §6.4.3) : point de terminaison, code membre, BIN. */
public record ValeurReference(String libelle, String valeur) {}
