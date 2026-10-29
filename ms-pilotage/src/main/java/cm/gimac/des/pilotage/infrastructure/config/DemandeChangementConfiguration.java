package cm.gimac.des.pilotage.infrastructure.config;

import cm.gimac.des.pilotage.domain.processus.ActionProcessus;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.ChampFormulaire;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.Formulaire;
import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import cm.gimac.des.pilotage.domain.processus.Initiateurs;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.Livrable;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.Position;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import cm.gimac.des.pilotage.domain.processus.SignatureRequise;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
import cm.gimac.des.pilotage.domain.processus.TypeChampFormulaire;
import cm.gimac.des.pilotage.repository.HistoriqueProcessusRepository;
import cm.gimac.des.pilotage.repository.ProcessusRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Modèle publié de la fiche RFC : demandeur, responsable de mise en œuvre, puis décision SSI signée. */
@Component
@ConditionalOnProperty(name = "gimac.processus.demande-changement", havingValue = "true", matchIfMissing = true)
public class DemandeChangementConfiguration implements ApplicationRunner {

    public static final String CODE = "DEMANDE_CHANGEMENT";
    public static final String UNITE = "SSI";
    public static final String REFERENCE = "CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}";
    private static final Logger LOG = LoggerFactory.getLogger(DemandeChangementConfiguration.class);

    private final ProcessusRepository processus;
    private final HistoriqueProcessusRepository historique;
    private final TransactionTemplate transactions;
    private final Clock horloge;

    public DemandeChangementConfiguration(ProcessusRepository processus, HistoriqueProcessusRepository historique, TransactionTemplate transactions, Clock horloge) {
        this.processus = processus;
        this.historique = historique;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactions.executeWithoutResult(s -> {
            if (processus.existsByCode(CODE)) {
                return;
            }
            Instant le = horloge.instant();
            Processus p = Processus.brouillon(CODE, UNITE, "systeme", le);
            p.decrire("Demande de changement", "Fiche de demande de changement GIMAC : identification, raison, impact, mise en œuvre et décision SSI.", "Exploitation", 10, etapes(), "systeme", le);
            p.definirLancement(new Initiateurs(List.of(RegleInitiateur.TOUT_LE_GIMAC), List.of(), List.of(), List.of(), List.of(), true), formulaire(), "systeme", le);
            p.definirReference(REFERENCE);
            p.publier("systeme", "Modèle initial de la fiche RFC", false, le);
            processus.save(p);
            historique.save(new HistoriqueProcessus(p.getId(), null, null, le, "systeme", ActionProcessus.PUBLIE, "Modèle initial de la fiche RFC"));
            LOG.info("Processus {} publié avec la référence dynamique {}", CODE, REFERENCE);
        });
    }

    public static Formulaire formulaire() {
        return new Formulaire(List.of(
            texte("TYPE_DEMANDEUR", "Unité du demandeur", TypeChampFormulaire.LISTE, List.of("SUPPORT_MONETIQUE", "AUTRE_DES")),
            texte("ELEMENT_CONFIGURATION", "Élément de configuration (AMA, BD…)", TypeChampFormulaire.TEXTE, List.of()),
            texte("IDENTIFIANT_EC", "Identifiant EC", TypeChampFormulaire.TEXTE, List.of()),
            texte("PRIORITE", "Priorité", TypeChampFormulaire.LISTE, List.of("NORMAL", "URGENT")),
            texte("DECLENCHEUR", "Déclencheur", TypeChampFormulaire.LISTE, List.of("CLIENT", "ERREUR_CONNUE", "PROBLEME", "PROJET")),
            texte("DEMANDEUR_CONTACT", "Contact du demandeur (téléphone, courriel)", TypeChampFormulaire.TEXTE, List.of()),
            texte("RAISON", "Raison détaillée du changement", TypeChampFormulaire.TEXTE_LONG, List.of()),
            texte("ELEMENT_IMPACTE", "EC impactés", TypeChampFormulaire.TEXTE_LONG, List.of()),
            texte("DESCRIPTION_IMPACT", "Description de l'impact", TypeChampFormulaire.TEXTE_LONG, List.of()),
            texte("RESPONSABLE_CONTACT", "Contact du responsable de mise en œuvre", TypeChampFormulaire.TEXTE, List.of()),
            texte("PLAN_TEST", "Description du plan de test", TypeChampFormulaire.TEXTE_LONG, List.of()),
            texte("PLAN_RETOUR", "Description du plan de retour", TypeChampFormulaire.TEXTE_LONG, List.of())
        ));
    }

    private static ChampFormulaire texte(String code, String libelle, TypeChampFormulaire type, List<String> choix) {
        return new ChampFormulaire(code, libelle, type, choix, true, null, List.of());
    }

    public static List<Etape> etapes() {
        Intervenant demandeur = new Intervenant(ModeIntervenant.LANCEUR, null, null, null, null);
        Intervenant responsable = new Intervenant(ModeIntervenant.AU_LANCEMENT, null, null, UNITE, "ROLE_PRODUCTION_MONETIQUE");
        Intervenant ssi = new Intervenant(ModeIntervenant.PROFIL, "ROLE_AGENT_SSI", null, UNITE, null);
        return List.of(
            new Etape("IDENTIFICATION", "1. Identification du changement", "Vérifier l'élément de configuration, le demandeur, la priorité et le déclencheur.", demandeur, null, List.of(), 1, List.of(), List.of(), List.of(), new Position(0, 0)),
            new Etape("RAISON", "2. Raison du changement", "Vérifier la raison détaillée.", demandeur, null, List.of("IDENTIFICATION"), 1, List.of(), List.of(), List.of(), new Position(220, 0)),
            new Etape("IMPACT", "3. Impact du changement", "Vérifier les EC impactés et la description de l'impact.", demandeur, null, List.of("RAISON"), 1, List.of(), List.of(), List.of(), new Position(440, 0)),
            new Etape("IMPLEMENTATION", "4. Détail d'implémentation", "Confirmer le responsable, le plan de test et le plan de retour, puis déposer le dossier PDF.", responsable, null, List.of("IMPACT"), 2,
                List.of(new Livrable("DOSSIER_CHANGEMENT", "Dossier de changement PDF", List.of("application/pdf"), true, null)), List.of(), List.of(), new Position(660, 0)),
            new Etape("SSI", "5. Décision de la Sécurité du Système d'Information", "Planifier, évaluer et signer la décision unique.", ssi, null, List.of("IMPLEMENTATION"), 2, List.of(),
                List.of(new SignatureRequise("DECISION_SSI", "Décision SSI", ssi, List.of("DOSSIER_CHANGEMENT"), 1)),
                List.of(
                    new Champ("DATE_DEPLOIEMENT", "Date de déploiement", TypeChamp.DATE, List.of(), true),
                    new Champ("DATE_REVUE", "Date de revue", TypeChamp.DATE, List.of(), true),
                    new Champ("EVALUATION", "Évaluation du changement", TypeChamp.TEXTE, List.of(), true),
                    new Champ("DECISION", "Décision", TypeChamp.CHOIX, List.of("ACCEPTEE", "REFUSEE"), true),
                    new Champ("DESCRIPTION", "Description de la décision", TypeChamp.TEXTE, List.of(), true)
                ), new Position(880, 0))
        );
    }
}
