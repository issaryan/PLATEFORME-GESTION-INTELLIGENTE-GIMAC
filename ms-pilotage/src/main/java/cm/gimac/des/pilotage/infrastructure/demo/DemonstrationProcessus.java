package cm.gimac.des.pilotage.infrastructure.demo;

import cm.gimac.des.pilotage.domain.processus.ActionProcessus;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import cm.gimac.des.pilotage.domain.processus.Intervenant;
import cm.gimac.des.pilotage.domain.processus.Livrable;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.Position;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.SignatureRequise;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
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

/**
 * Jeu de démonstration des processus d'unité (D-87), chargé au démarrage avec gimac.demonstration.processus=true
 * (profil dev, avec le pôle réseau de démonstration de l'adaptateur de substitution de l'organisation) : le
 * processus publié « Ouverture d'un flux réseau » du pôle réseau (OUVERTURE_FLUX_RESEAU v1) : demande par le
 * lanceur, analyse par un ingénieur réseau (profil ROLE_POLE_RESEAU) validée par le chef d'unité, mise en œuvre
 * par une personne désignée au lancement (remplacement : ROLE_POLE_RESEAU), recette dont le procès-verbal est
 * signé par le chef d'unité. Rien n'est fait si le code existe déjà.
 */
@Component
@ConditionalOnProperty(name = "gimac.demonstration.processus", havingValue = "true")
public class DemonstrationProcessus implements ApplicationRunner {

    public static final String CODE = "OUVERTURE_FLUX_RESEAU";
    public static final String UNITE = "POLE_RESEAU";
    public static final String PROFIL = "ROLE_POLE_RESEAU";
    static final String ACTEUR = "demonstration";
    private static final Logger LOG = LoggerFactory.getLogger(DemonstrationProcessus.class);

    private final ProcessusRepository processus;
    private final HistoriqueProcessusRepository historique;
    private final TransactionTemplate transactions;
    private final Clock horloge;

    public DemonstrationProcessus(ProcessusRepository processus, HistoriqueProcessusRepository historique, TransactionTemplate transactions, Clock horloge) {
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
            Processus p = Processus.brouillon(CODE, UNITE, ACTEUR, le);
            p.decrire(
                "Ouverture d'un flux réseau",
                "Demande, analyse de sécurité, mise en œuvre et recette signée de l'ouverture d'un flux entre deux systèmes.",
                "Réseau",
                10,
                etapes(),
                ACTEUR,
                le
            );
            p.publier(ACTEUR, "Jeu de démonstration", false, le);
            processus.save(p);
            historique.save(new HistoriqueProcessus(p.getId(), null, null, le, ACTEUR, ActionProcessus.PUBLIE, "Jeu de démonstration"));
            LOG.info("Processus de démonstration {} publié (unité {})", CODE, UNITE);
        });
    }

    /** Étapes du processus « Ouverture d'un flux réseau ». */
    public static List<Etape> etapes() {
        Intervenant lanceur = new Intervenant(ModeIntervenant.LANCEUR, null, null, null, PROFIL);
        Intervenant reseau = new Intervenant(ModeIntervenant.PROFIL, PROFIL, null, null, null);
        Intervenant chef = new Intervenant(ModeIntervenant.CHEF_UNITE, null, null, null, "ROLE_RESPONSABLE_SERVICE");
        Intervenant designe = new Intervenant(ModeIntervenant.AU_LANCEMENT, null, null, null, PROFIL);
        List<String> pdf = List.of("application/pdf");
        return List.of(
            new Etape(
                "DEMANDE",
                "Demande d'ouverture",
                "Décrire le flux : source, destination, port, protocole et justification.",
                lanceur,
                null,
                List.of(),
                1,
                List.of(new Livrable("FICHE_DEMANDE", "Fiche de demande", pdf, false, null)),
                List.of(),
                List.of(
                    new Champ("SOURCE", "Adresse source", TypeChamp.TEXTE, List.of(), true),
                    new Champ("DESTINATION", "Adresse destination", TypeChamp.TEXTE, List.of(), true),
                    new Champ("PORT", "Port", TypeChamp.NOMBRE, List.of(), true),
                    new Champ("PROTOCOLE", "Protocole", TypeChamp.CHOIX, List.of("TCP", "UDP"), true),
                    new Champ("DATE_SOUHAITEE", "Date souhaitée", TypeChamp.DATE, List.of(), false)
                ),
                new Position(0, 0)
            ),
            new Etape(
                "ANALYSE",
                "Analyse de sécurité",
                "Évaluer le risque du flux et rédiger la note d'analyse.",
                reseau,
                chef,
                List.of("DEMANDE"),
                3,
                List.of(new Livrable("NOTE_ANALYSE", "Note d'analyse", pdf, true, null)),
                List.of(),
                List.of(new Champ("AVIS", "Avis", TypeChamp.CHOIX, List.of("FAVORABLE", "RESERVE", "DEFAVORABLE"), true)),
                new Position(260, 0)
            ),
            new Etape(
                "MISE_EN_OEUVRE",
                "Mise en œuvre",
                "Configurer les équipements et joindre la configuration appliquée.",
                designe,
                null,
                List.of("ANALYSE"),
                3,
                List.of(new Livrable("CONFIGURATION", "Configuration appliquée", List.of("application/pdf", "text/plain"), true, null)),
                List.of(),
                List.of(),
                new Position(520, 0)
            ),
            new Etape(
                "RECETTE",
                "Recette",
                "Vérifier le flux de bout en bout et établir le procès-verbal de recette.",
                reseau,
                null,
                List.of("MISE_EN_OEUVRE"),
                2,
                List.of(new Livrable("PV_RECETTE", "Procès-verbal de recette", pdf, true, null)),
                List.of(new SignatureRequise("VISA_CHEF", "Visa du chef d'unité", chef, List.of("PV_RECETTE", "NOTE_ANALYSE"), 1)),
                List.of(),
                new Position(780, 0)
            )
        );
    }
}
