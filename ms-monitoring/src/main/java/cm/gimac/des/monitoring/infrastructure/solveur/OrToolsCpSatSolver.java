package cm.gimac.des.monitoring.infrastructure.solveur;

import cm.gimac.des.monitoring.application.port.out.IntegerProgramSolver;
import cm.gimac.des.monitoring.domain.exploitation.ModeleLineaire;
import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adaptateur du port {@link IntegerProgramSolver} (D-15) : Google OR-Tools (Apache-2.0), moteur CP-SAT, bibliothèques
 * natives embarquées et chargées hors ligne. Les bornes infinies du modèle sont ramenées aux bornes de CP-SAT.
 */
@Component
public class OrToolsCpSatSolver implements IntegerProgramSolver {

    private static final long BORNE = 1L << 40;
    private static volatile boolean charge;

    private final int fils;
    private final double ecartRelatif;
    private final long stagnationMs;

    /**
     * @param fils fils de recherche de CP-SAT
     * @param ecartRelatif arrêt anticipé dès que l'écart d'optimalité relatif est atteint (0 : preuve d'optimalité
     *     exigée, jusqu'à la limite de temps)
     * @param stagnationSecondes arrêt anticipé quand aucune meilleure solution n'est trouvée pendant cette durée
     *     (la limite de 300 s de D-15 reste le plafond ; l'écart d'optimalité est alors rendu)
     */
    public OrToolsCpSatSolver(
        @Value("${gimac.monitoring.grille.fils:8}") int fils,
        @Value("${gimac.monitoring.grille.ecart-relatif:0.02}") double ecartRelatif,
        @Value("${gimac.monitoring.grille.stagnation-secondes:15}") double stagnationSecondes
    ) {
        this.fils = Math.max(1, fils);
        this.ecartRelatif = Math.max(0, ecartRelatif);
        this.stagnationMs = Math.max(100L, Math.round(stagnationSecondes * 1000));
    }

    private static synchronized void charger() {
        if (!charge) {
            Loader.loadNativeLibraries();
            charge = true;
        }
    }

    @Override
    public Resultat resoudre(ModeleLineaire modele, Duration limite) {
        charger();
        long debut = System.nanoTime();
        CpModel m = new CpModel();
        List<ModeleLineaire.Variable> vars = modele.variables();
        IntVar[] x = new IntVar[vars.size()];
        for (int i = 0; i < x.length; i++) {
            ModeleLineaire.Variable v = vars.get(i);
            x[i] = m.newIntVar(borne(v.inf()), borne(v.sup()), v.nom());
        }
        for (ModeleLineaire.Contrainte c : modele.contraintes()) {
            m.addLinearConstraint(expression(x, c.termes()), borne(c.inf()), borne(c.sup()));
        }
        m.minimize(expression(x, modele.objectif()));
        CpSolver s = new CpSolver();
        s.getParameters().setMaxTimeInSeconds(Math.max(0.1, limite.toMillis() / 1000.0));
        s.getParameters().setNumWorkers(fils);
        s.getParameters().setRelativeGapLimit(ecartRelatif);
        AtomicLong derniere = new AtomicLong(0);
        CpSolverSolutionCallback suivi = new CpSolverSolutionCallback() {
            @Override
            public void onSolutionCallback() {
                derniere.set(System.nanoTime());
            }
        };
        ScheduledExecutorService veille = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grille-stagnation");
            t.setDaemon(true);
            return t;
        });
        veille.scheduleAtFixedRate(
            () -> {
                long d = derniere.get();
                if (d != 0 && (System.nanoTime() - d) / 1_000_000L > stagnationMs) {
                    suivi.stopSearch();
                }
            },
            100,
            100,
            TimeUnit.MILLISECONDS
        );
        CpSolverStatus st;
        try {
            st = s.solve(m, suivi);
        } finally {
            veille.shutdownNow();
        }
        long duree = (System.nanoTime() - debut) / 1_000_000L;
        Statut statut = switch (st) {
            case OPTIMAL -> Statut.OPTIMALE;
            case FEASIBLE -> Statut.REALISABLE;
            case INFEASIBLE, MODEL_INVALID -> Statut.IRREALISABLE;
            default -> Statut.SANS_SOLUTION_DANS_LE_DELAI;
        };
        if (statut != Statut.OPTIMALE && statut != Statut.REALISABLE) {
            return new Resultat(statut, new long[0], 0, 0, duree);
        }
        long[] valeurs = new long[x.length];
        for (int i = 0; i < x.length; i++) {
            valeurs[i] = s.value(x[i]);
        }
        return new Resultat(statut, valeurs, Math.round(s.objectiveValue()), Math.round(s.bestObjectiveBound()), duree);
    }

    private static long borne(long v) {
        return Math.max(-BORNE, Math.min(BORNE, v));
    }

    private static LinearExpr expression(IntVar[] x, List<ModeleLineaire.Terme> termes) {
        IntVar[] v = new IntVar[termes.size()];
        long[] c = new long[termes.size()];
        for (int k = 0; k < v.length; k++) {
            v[k] = x[termes.get(k).index()];
            c[k] = termes.get(k).coef();
        }
        return LinearExpr.weightedSum(v, c);
    }
}
