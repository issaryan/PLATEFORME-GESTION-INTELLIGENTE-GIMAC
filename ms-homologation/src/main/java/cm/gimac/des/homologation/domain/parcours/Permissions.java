package cm.gimac.des.homologation.domain.parcours;

/** Permissions du parcours (frontend/packages/lot1/src/permissions.ts, docs/lot1/correspondance.md). */
public final class Permissions {

    public static final String LIAISON_LIST = "HOM_LIAISON_LIST";
    public static final String LIAISON_READ = "HOM_LIAISON_READ";
    public static final String LIAISON_OPEN = "HOM_LIAISON_OPEN";
    public static final String PROJECT_DESIGNATE = "HOM_PROJECT_DESIGNATE";
    public static final String DASHBOARD_READ = "HOM_DASHBOARD_READ";
    public static final String OPERATION_ASSIGN = "HOM_OPERATION_ASSIGN";
    public static final String EVIDENCE_SUBMIT = "HOM_EVIDENCE_SUBMIT";
    public static final String OPERATION_VALIDATE = "HOM_OPERATION_VALIDATE";
    public static final String STEP_MANAGE = "HOM_STEP_MANAGE";
    public static final String DOSSIER_VERIFY = "CER_DOSSIER_VERIFY";
    public static final String PORTAL_READ = "HOM_PORTAL_READ";
    public static final String PORTAL_SUBMIT = "HOM_PORTAL_SUBMIT";
    public static final String INTERNAL_READ = "INTERNAL_HOMOLOGATION_READ";

    private Permissions() {}
}
