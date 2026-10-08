package cm.gimac.des.homologation.domain.parcours;

import java.time.LocalDate;

/** Renvoi d'une opération par son validateur (D-93 §2) : motif, auteur et date ; l'opération revient à faire. */
public record Renvoi(String motif, String parId, String parNom, LocalDate le) {}
