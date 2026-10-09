package cm.gimac.des.pilotage.infrastructure.demo;

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
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter;
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
 * signé par le chef d'unité. D-98 : deux demandes lancées de tout le GIMAC (initiateurs TOUT_LE_GIMAC) avec leur
 * formulaire de lancement, « Demande de congé » (DEMANDE_CONGE, unité RH) et « Demande de changement d'adresse IP »
 * (CHANGEMENT_ADRESSE_IP, pôle réseau). D-100 : deux processus au public restreint par leur créateur, pour montrer
 * qu'un administrateur ne voit plus tout dans « Lancer une demande » : « Création d'une machine virtuelle »
 * (CREATION_MACHINE_VIRTUELLE, pôle système), réservé à deux personnes désignées (l'ingénieure réseau et la chargée
 * d'études du SNA de l'organisation simulée), et « Mise à jour de la plateforme » (MISE_A_JOUR_PLATEFORME, conformité),
 * réservé aux porteurs des profils Conformité et Sécurité des SI. Rien n'est fait pour un code qui existe déjà.
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
            demandes();
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

    // ------------------------------------------------------------------ demandes lancées de tout le GIMAC (D-98)

    public static final String CODE_CONGE = "DEMANDE_CONGE";
    public static final String CODE_IP = "CHANGEMENT_ADRESSE_IP";
    public static final String UNITE_RH = "RH";
    /** D-100 : processus au public restreint (personnes désignées, profils). */
    public static final String CODE_VM = "CREATION_MACHINE_VIRTUELLE";
    public static final String CODE_MISE_A_JOUR = "MISE_A_JOUR_PLATEFORME";
    public static final String UNITE_SYSTEME = "POLE_SYSTEME";
    public static final String UNITE_CONFORMITE = "CONFORMITE";

    /** Public de la création d'une machine virtuelle : deux personnes désignées nommément (D-100). */
    public static Initiateurs publicMachineVirtuelle() {
        return new Initiateurs(
            List.of(RegleInitiateur.PERSONNES),
            List.of(),
            List.of(),
            List.of(),
            List.of(OrganisationStubAdapter.INGENIEUR_RESEAU, OrganisationStubAdapter.AGENT_BIS),
            false
        );
    }

    /** Public de la mise à jour de la plateforme : porteurs des profils Conformité et Sécurité des SI (D-100). */
    public static Initiateurs publicMiseAJour() {
        return new Initiateurs(List.of(RegleInitiateur.PROFILS), List.of(), List.of("ROLE_CONFORMITE", "ROLE_RSSI"), List.of(), List.of(), false);
    }

    private static Initiateurs toutLeGimac() {
        return new Initiateurs(List.of(RegleInitiateur.TOUT_LE_GIMAC), List.of(), List.of(), List.of(), List.of(), false);
    }

    /**
     * Publie « Demande de congé » (RH) et « Demande de changement d'adresse IP » (pôle réseau), ouverts à tout le GIMAC, puis
     * « Création d'une machine virtuelle » et « Mise à jour de la plateforme » au public restreint (D-100), s'ils sont absents.
     */
    private void demandes() {
        Instant le = horloge.instant();
        publierDemande(
            CODE_CONGE,
            UNITE_RH,
            "Demande de congé",
            "Demande de congé adressée au service des ressources humaines : vérification des droits, accord du supérieur, enregistrement et clôture.",
            "Ressources humaines",
            etapesConge(),
            formulaireConge(),
            toutLeGimac(),
            le
        );
        publierDemande(
            CODE_IP,
            UNITE,
            "Demande de changement d'adresse IP",
            "Demande adressée au pôle réseau pour changer l'adresse IP d'un poste ou d'un équipement : analyse, validation, mise en œuvre et clôture.",
            "Réseau",
            etapesIp(),
            formulaireIp(),
            toutLeGimac(),
            le
        );
        publierDemande(
            CODE_VM,
            UNITE_SYSTEME,
            "Création d'une machine virtuelle",
            "Demande de création d'une machine virtuelle sur l'infrastructure de virtualisation : dimensionnement, création et remise.",
            "Système",
            etapesInterne("ROLE_POLE_SYSTEME", UNITE_SYSTEME, "Dimensionnement et création", "Remise de la machine"),
            formulaireVm(),
            publicMachineVirtuelle(),
            le
        );
        publierDemande(
            CODE_MISE_A_JOUR,
            UNITE_CONFORMITE,
            "Mise à jour de la plateforme",
            "Demande de mise à jour de la plateforme soumise à la revue de conformité : analyse d'impact, revue et clôture.",
            "Conformité",
            etapesInterne("ROLE_CONFORMITE", UNITE_CONFORMITE, "Analyse d'impact et revue", "Clôture de la revue"),
            Formulaire.vide(),
            publicMiseAJour(),
            le
        );
    }

    /** Formulaire de la création d'une machine virtuelle : système, processeurs, mémoire. */
    public static Formulaire formulaireVm() {
        return new Formulaire(
            List.of(
                new ChampFormulaire("SYSTEME", "Système d'exploitation", TypeChampFormulaire.LISTE, List.of("Linux", "Windows Server"), true, null, List.of()),
                new ChampFormulaire("PROCESSEURS", "Nombre de processeurs", TypeChampFormulaire.NOMBRE, List.of(), true, null, List.of()),
                new ChampFormulaire("MEMOIRE", "Mémoire (Go)", TypeChampFormulaire.NOMBRE, List.of(), true, null, List.of())
            )
        );
    }

    /** Deux étapes tenues par les porteurs d'un profil de l'unité, la seconde validée par le chef de l'unité. */
    public static List<Etape> etapesInterne(String profil, String unite, String premiere, String seconde) {
        Intervenant porteur = new Intervenant(ModeIntervenant.PROFIL, profil, null, unite, null);
        Intervenant chef = new Intervenant(ModeIntervenant.CHEF_UNITE, null, null, null, "ROLE_RESPONSABLE_SERVICE");
        return List.of(
            new Etape("REALISATION", premiere, "Réaliser la demande et consigner le résultat.", porteur, null, List.of(), 3, List.of(), List.of(), List.of(), new Position(0, 0)),
            new Etape("CLOTURE", seconde, "Informer le demandeur et clôturer la demande.", porteur, chef, List.of("REALISATION"), 1, List.of(), List.of(), List.of(), new Position(260, 0))
        );
    }

    private void publierDemande(
        String code,
        String unite,
        String libelle,
        String description,
        String categorie,
        List<Etape> etapes,
        Formulaire formulaire,
        Initiateurs initiateurs,
        Instant le
    ) {
        if (processus.existsByCode(code)) {
            return;
        }
        Processus p = Processus.brouillon(code, unite, ACTEUR, le);
        p.decrire(libelle, description, categorie, 5, etapes, ACTEUR, le);
        p.definirLancement(initiateurs, formulaire, ACTEUR, le);
        p.publier(ACTEUR, "Jeu de démonstration", false, le);
        processus.save(p);
        historique.save(new HistoriqueProcessus(p.getId(), null, null, le, ACTEUR, ActionProcessus.PUBLIE, "Jeu de démonstration"));
        LOG.info("Processus de démonstration {} publié (unité {}, public : {})", code, unite, initiateurs.regles());
    }

    /** Formulaire de la demande de congé : période, motif, pièce jointe facultative. */
    public static Formulaire formulaireConge() {
        return new Formulaire(
            List.of(
                new ChampFormulaire("PERIODE", "Période de congé", TypeChampFormulaire.PERIODE, List.of(), true, "Premier et dernier jour d'absence.", List.of()),
                new ChampFormulaire("MOTIF", "Motif", TypeChampFormulaire.TEXTE_LONG, List.of(), true, "Congé annuel, événement familial, récupération…", List.of()),
                new ChampFormulaire(
                    "JUSTIFICATIF",
                    "Pièce justificative",
                    TypeChampFormulaire.PIECE_JOINTE,
                    List.of(),
                    false,
                    "Facultative : PDF ou image.",
                    List.of("application/pdf", "image/*")
                )
            )
        );
    }

    /** Étapes de la demande de congé (unité RH) : analyse, validation, exécution, clôture. */
    public static List<Etape> etapesConge() {
        Intervenant gestionnaire = new Intervenant(ModeIntervenant.PROFIL, "ROLE_COLLABORATEUR", null, UNITE_RH, null);
        Intervenant superieur = new Intervenant(ModeIntervenant.N_PLUS_1_LANCEUR, null, null, null, "ROLE_RESPONSABLE_SERVICE");
        return List.of(
            new Etape(
                "ANALYSE",
                "Vérification des droits à congé",
                "Vérifier le solde de congés du demandeur et la conformité de la période demandée.",
                gestionnaire,
                null,
                List.of(),
                2,
                List.of(),
                List.of(),
                List.of(new Champ("SOLDE_JOURS", "Solde de jours disponible", TypeChamp.NOMBRE, List.of(), true)),
                new Position(0, 0)
            ),
            new Etape(
                "VALIDATION",
                "Accord du supérieur hiérarchique",
                "Donner l'accord du service sur la période demandée.",
                superieur,
                null,
                List.of("ANALYSE"),
                2,
                List.of(),
                List.of(),
                List.of(new Champ("DECISION", "Décision", TypeChamp.CHOIX, List.of("Accordé", "Accordé avec dates modifiées"), true)),
                new Position(260, 0)
            ),
            new Etape(
                "EXECUTION",
                "Enregistrement du congé",
                "Enregistrer le congé dans le dossier du demandeur et établir la décision.",
                gestionnaire,
                null,
                List.of("VALIDATION"),
                2,
                List.of(new Livrable("DECISION_CONGE", "Décision de congé", List.of("application/pdf"), false, null)),
                List.of(),
                List.of(),
                new Position(520, 0)
            ),
            new Etape(
                "CLOTURE",
                "Clôture du dossier",
                "Informer le demandeur et archiver la demande.",
                gestionnaire,
                null,
                List.of("EXECUTION"),
                1,
                List.of(),
                List.of(),
                List.of(),
                new Position(780, 0)
            )
        );
    }

    /** Formulaire de la demande de changement d'adresse IP : équipement, adresse actuelle, adresse demandée, justification. */
    public static Formulaire formulaireIp() {
        return new Formulaire(
            List.of(
                new ChampFormulaire("EQUIPEMENT", "Équipement", TypeChampFormulaire.TEXTE, List.of(), true, "Nom ou numéro d'inventaire du poste ou de l'équipement.", List.of()),
                new ChampFormulaire("ADRESSE_ACTUELLE", "Adresse IP actuelle", TypeChampFormulaire.TEXTE, List.of(), true, "Par exemple 10.20.4.15.", List.of()),
                new ChampFormulaire("ADRESSE_DEMANDEE", "Adresse IP demandée", TypeChampFormulaire.TEXTE, List.of(), true, "Adresse souhaitée, ou plage du réseau visé.", List.of()),
                new ChampFormulaire("JUSTIFICATION", "Justification", TypeChampFormulaire.TEXTE_LONG, List.of(), true, "Pourquoi le changement est nécessaire.", List.of())
            )
        );
    }

    /** Étapes de la demande de changement d'adresse IP (pôle réseau) : analyse, validation, exécution, clôture. */
    public static List<Etape> etapesIp() {
        Intervenant reseau = new Intervenant(ModeIntervenant.PROFIL, PROFIL, null, null, null);
        Intervenant chef = new Intervenant(ModeIntervenant.CHEF_UNITE, null, null, null, "ROLE_RESPONSABLE_SERVICE");
        return List.of(
            new Etape(
                "ANALYSE",
                "Analyse technique",
                "Vérifier le plan d'adressage, la disponibilité de l'adresse demandée et l'impact sur les règles de filtrage.",
                reseau,
                null,
                List.of(),
                2,
                List.of(),
                List.of(),
                List.of(new Champ("ADRESSE_DISPONIBLE", "Adresse disponible", TypeChamp.CHOIX, List.of("Oui", "Non, autre adresse proposée"), true)),
                new Position(0, 0)
            ),
            new Etape(
                "VALIDATION",
                "Validation du chef de pôle",
                "Autoriser le changement d'adresse au vu de l'analyse.",
                chef,
                null,
                List.of("ANALYSE"),
                1,
                List.of(),
                List.of(),
                List.of(new Champ("DECISION", "Décision", TypeChamp.CHOIX, List.of("Accordée", "Accordée avec réserves"), true)),
                new Position(260, 0)
            ),
            new Etape(
                "EXECUTION",
                "Mise en œuvre du changement",
                "Appliquer la nouvelle adresse, mettre à jour le DHCP, le DNS et les règles de filtrage.",
                reseau,
                null,
                List.of("VALIDATION"),
                2,
                List.of(),
                List.of(),
                List.of(new Champ("ADRESSE_ATTRIBUEE", "Adresse IP attribuée", TypeChamp.TEXTE, List.of(), true)),
                new Position(520, 0)
            ),
            new Etape(
                "CLOTURE",
                "Vérification et clôture",
                "Vérifier la connectivité avec le demandeur et clôturer la demande.",
                reseau,
                null,
                List.of("EXECUTION"),
                1,
                List.of(),
                List.of(),
                List.of(),
                new Position(780, 0)
            )
        );
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
