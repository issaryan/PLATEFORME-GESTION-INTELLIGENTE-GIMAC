package cm.gimac.des.gateway.domain.supervision;

/** Informations de construction publiées par /management/info. */
public record InfoService(String version, String artefact, String dateConstruction, String commit, String branche) {
    public static final InfoService VIDE = new InfoService(null, null, null, null, null);
}
