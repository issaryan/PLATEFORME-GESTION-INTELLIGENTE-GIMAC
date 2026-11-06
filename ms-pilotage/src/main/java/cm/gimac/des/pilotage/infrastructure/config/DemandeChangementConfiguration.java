package cm.gimac.des.pilotage.infrastructure.config;

import cm.gimac.des.pilotage.domain.processus.ActionProcessus;
import cm.gimac.des.pilotage.domain.processus.Champ;
import cm.gimac.des.pilotage.domain.processus.ChampFormulaire;
import cm.gimac.des.pilotage.domain.processus.Etape;
import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
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
    public static final String REFERENCE = "CHG-{AAAA}-{N}";
    /** Gabarit daté du modèle système publié du 28/10 au 04/11/2026 (recette), remplacé au démarrage. */
    static final String REFERENCE_DATEE = "CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}";
    static final String PROFIL_SSI = "ROLE_RSSI";
    /** Profil de remplacement de la mise en œuvre avant D-142, qui restreignait la désignation à la Production monétique. */
    static final String PROFIL_MISE_EN_OEUVRE_AVANT = "ROLE_PRODUCTION_MONETIQUE";
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
                processus.findFirstByCodeAndEtat(CODE, EtatProcessus.PUBLIEE).filter(DemandeChangementConfiguration::aRemplacer).ifPresent(this::remplacer);
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

    /**
     * Version publiée encore au gabarit daté du modèle système : une nouvelle version, copie de celle-ci, prend le format
     * de la fiche (CHG-{AAAA}-{N}, D-127) et le RSSI à l'étape SSI (D-130), puis remplace la précédente, retirée ; une
     * version publiée n'est jamais modifiée. Un brouillon en cours ou un gabarit personnalisé sont laissés tels quels.
     */
    private void remplacer(Processus publiee) {
        if (processus.findFirstByCodeAndEtat(CODE, EtatProcessus.BROUILLON).isPresent()) {
            LOG.warn("Processus {} : brouillon en cours, gabarit daté de la version {} conservé", CODE, publiee.getVersion());
            return;
        }
        Instant le = horloge.instant();
        Processus p = publiee.nouvelleVersion(processus.derniereVersion(CODE) + 1, "systeme", le);
        p.decrire(publiee.getLibelle(), publiee.getDescription(), publiee.getCategorie(), publiee.getDelaiCibleJours(), publiee.getDelaiMaxJours(), aJour(publiee.getEtapes()), "systeme", le);
        p.definirReference(REFERENCE);
        String motif = "Format de la fiche de changement CHG-AAAA-N (D-127), RSSI signataire de la décision SSI (D-130), mise en œuvre confiée à toute personne interne (D-142)";
        publiee.retirer("systeme", "Remplacée par la version " + p.getVersion(), le);
        processus.saveAndFlush(publiee);
        historique.save(new HistoriqueProcessus(publiee.getId(), null, null, le, "systeme", ActionProcessus.RETIRE, "Remplacée par la version " + p.getVersion()));
        p.publier("systeme", motif, false, le);
        processus.save(p);
        historique.save(new HistoriqueProcessus(p.getId(), null, null, le, "systeme", ActionProcessus.PUBLIE, motif));
        LOG.info("Processus {} : version {} publiée avec la référence {}, version {} retirée", CODE, p.getVersion(), REFERENCE, publiee.getVersion());
    }

    /**
     * Version publiée du modèle système à remplacer : gabarit daté (D-127), ou étapes du modèle système d'avant D-130 et
     * D-142 (agent SSI, mise en œuvre réservée à la Production monétique). Un modèle personnalisé n'est jamais touché.
     */
    static boolean aRemplacer(Processus publiee) {
        if (REFERENCE_DATEE.equals(publiee.getReferenceModele())) {
            return true;
        }
        return REFERENCE.equals(publiee.getReferenceModele()) && !aJour(publiee.getEtapes()).equals(publiee.getEtapes());
    }

    /** Étapes du modèle système mises à jour : RSSI à l'étape SSI (D-130), mise en œuvre ouverte à tout le GIMAC (D-142). */
    static List<Etape> aJour(List<Etape> etapes) {
        return ouvrirMiseEnOeuvre(rssi(etapes));
    }

    /** D-142 : le responsable de la mise en œuvre, désigné au lancement, peut être toute personne interne active. */
    static List<Etape> ouvrirMiseEnOeuvre(List<Etape> etapes) {
        return etapes.stream().map(e -> {
            Intervenant i = e.intervenant();
            if (!"IMPLEMENTATION".equals(e.code()) || i == null || i.mode() != ModeIntervenant.AU_LANCEMENT || !PROFIL_MISE_EN_OEUVRE_AVANT.equals(i.remplacement())) {
                return e;
            }
            return new Etape(e.code(), e.libelle(), e.action(), new Intervenant(i.mode(), i.profil(), i.personneId(), i.unite(), null), e.validateur(), e.predecesseurs(), e.delaiJours(), e.livrables(), e.signatures(), e.champs(), e.position(), e.delaiMaxJours());
        }).toList();
    }

    /** Étape SSI : intervenant et signataires de profil ramenés au RSSI ; les autres étapes restent identiques. */
    static List<Etape> rssi(List<Etape> etapes) {
        return etapes.stream().map(e -> !"SSI".equals(e.code()) ? e : new Etape(
            e.code(), e.libelle(), e.action(), versRssi(e.intervenant()), e.validateur(), e.predecesseurs(), e.delaiJours(), e.livrables(),
            e.signatures().stream().map(s -> new SignatureRequise(s.code(), s.libelle(), versRssi(s.signataire()), s.porte(), s.ordre())).toList(),
            e.champs(), e.position(), e.delaiMaxJours()
        )).toList();
    }

    private static Intervenant versRssi(Intervenant i) {
        return i == null || i.mode() != ModeIntervenant.PROFIL ? i : new Intervenant(i.mode(), PROFIL_SSI, i.personneId(), i.unite(), i.remplacement());
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
        // D-142 : n'importe quelle personne interne active du GIMAC peut être désignée pour mettre en œuvre le changement.
        Intervenant responsable = new Intervenant(ModeIntervenant.AU_LANCEMENT, null, null, UNITE, null);
        // Verrou 2, approbation de la sécurité : le RSSI seul décide et signe (décision du 04/11/2026, D-130) ; l'agent SSI
        // n'y intervient pas.
        Intervenant ssi = new Intervenant(ModeIntervenant.PROFIL, PROFIL_SSI, null, UNITE, null);
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
