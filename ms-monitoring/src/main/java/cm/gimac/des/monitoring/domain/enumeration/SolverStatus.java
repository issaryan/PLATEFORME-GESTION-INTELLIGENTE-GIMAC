package cm.gimac.des.monitoring.domain.enumeration;

/**
 * Issue de la résolution par le solveur en nombres entiers (D-15).
 */
public enum SolverStatus {
    OPTIMALE,
    REALISABLE,
    IRREALISABLE,
    SANS_SOLUTION_DANS_LE_DELAI,
}
