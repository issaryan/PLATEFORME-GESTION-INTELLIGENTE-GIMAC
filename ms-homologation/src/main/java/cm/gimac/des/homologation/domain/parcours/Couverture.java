package cm.gimac.des.homologation.domain.parcours;

/** Valeur d'une couverture (certification ou mise en service) : taux entier, non calculable sans service inclus. */
public record Couverture(Integer taux, int numerateur, int denominateur, boolean calculable, String motifNonCalculable) {
    public static Couverture de(int numerateur, int denominateur) {
        if (denominateur == 0) {
            return new Couverture(null, 0, 0, false, "aucun service inclus");
        }
        return new Couverture((int) Math.round(numerateur * 100.0 / denominateur), numerateur, denominateur, true, null);
    }

    public boolean complete() {
        return taux != null && taux == 100;
    }
}
