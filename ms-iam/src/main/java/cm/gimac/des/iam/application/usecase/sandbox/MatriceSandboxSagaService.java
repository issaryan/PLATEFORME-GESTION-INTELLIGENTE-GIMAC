package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.EtapesIdempotentes;
import cm.gimac.des.iam.application.usecase.CatalogueProfils;
import cm.gimac.des.iam.application.usecase.ConfigurationAuthentificationService;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.domain.service.ReglesSandbox;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publication globale et idempotente de la matrice SBX, sur la recomposition ordinaire et auditée des profils. */
@Service
public class MatriceSandboxSagaService {

    public record Publication(long version, boolean rejouee) {}

    static final String PUBLICATION = "SBX_MATRICE_GLOBALE";
    static final Set<String> PROFILS_MATRICE = Set.of(
        "ROLE_SUPER_ADMIN",
        "ROLE_CHEF_DES",
        "ROLE_SNA",
        "ROLE_RSSI",
        "ROLE_PRODUCTION_MONETIQUE",
        "ROLE_ADMIN_SYSTEME",
        // D-133 : chargé de l'implémentation des projets de la SNA, SBX_CER_CONSULTATION au générateur du domaine.
        "ROLE_CHARGE_IMPLEMENTATION_SNA",
        "ROLE_SBX_ADMIN_INSTITUTION",
        "ROLE_SBX_CHEF_PROJET",
        "ROLE_SBX_DEVELOPPEUR",
        "ROLE_SBX_TESTEUR",
        "ROLE_SBX_LECTEUR",
        "ROLE_SBX_PARTENAIRE"
    );

    private final ConfigurationAuthentificationService profils;
    private final CatalogueProfils catalogue;
    private final EtapesIdempotentes etapes;

    public MatriceSandboxSagaService(ConfigurationAuthentificationService profils, CatalogueProfils catalogue, EtapesIdempotentes etapes) {
        this.profils = profils;
        this.catalogue = catalogue;
        this.etapes = etapes;
    }

    /**
     * Remplace les seules permissions SBX des profils fournis. Leurs autres permissions et les profils absents de la
     * matrice restent inchangés. Toutes les entrées sont validées avant la première écriture ; une clé rejouée ne
     * recompose ni ne journalise une seconde fois les profils.
     */
    @Transactional
    public Publication publier(Acteur acteur, long version, Map<String, Set<String>> matrice, String cle) {
        if (version < 1 || matrice == null || matrice.isEmpty()) {
            throw IamException.invalide("IAM_SBX_MATRICE_INVALIDE", "Une version positive et au moins un profil sont requis", Map.of());
        }
        Map<String, Set<String>> compositions = catalogue.compositions();
        Set<String> attribuables = catalogue.attribuables();
        Set<String> externes = catalogue.externes();
        Map<String, Set<String>> fusion = new TreeMap<>();
        new TreeMap<>(matrice).forEach((profil, demandees) -> {
            Set<String> actuelles = compositions.getOrDefault(profil, Set.of());
            if (!attribuables.contains(profil) || !PROFILS_MATRICE.contains(profil)) {
                throw IamException.nonTraitable("IAM_SBX_PROFIL_INVALIDE", "Seuls les profils de la matrice sandbox sont recomposables", Map.of("profil", profil));
            }
            Set<String> permissions = demandees == null ? Set.<String>of() : demandees;
            Set<String> invalides = new TreeSet<>();
            permissions.stream().filter(p -> !ReglesSandbox.conforme(p)).forEach(invalides::add);
            if (!invalides.isEmpty()) {
                throw IamException.nonTraitable("IAM_SBX_PERMISSION_INVALIDE", "Permissions SBX_<MODULE>_<FONCTION> attendues", Map.of("permissions", List.copyOf(invalides)));
            }
            ReglesSandbox.verifierValidationReservee(externes.contains(profil), permissions);
            Set<String> composees = new TreeSet<>(permissions);
            actuelles.stream().filter(p -> !ReglesSandbox.sandbox(p)).forEach(composees::add);
            // Une publication globale ne doit pas recomposer les profils inchangés : chaque écriture appelle Keycloak.
            if (!composees.equals(actuelles)) {
                fusion.put(profil, Set.copyOf(composees));
            }
        });
        Optional<String> deja = cle == null || cle.isBlank()
            ? Optional.empty()
            : etapes.reserver(cle.strip(), PUBLICATION, Long.toString(version), acteur.id());
        if (deja.isPresent()) {
            return new Publication(Long.parseLong(deja.orElseThrow()), true);
        }
        String motif = "Publication de la matrice sandbox version " + version;
        Acteur service = new Acteur(acteur.id(), acteur.nom(), Set.of(ReglesAdministration.OCTROI_PRIVILEGIE));
        fusion.forEach((profil, permissions) -> profils.definirPermissions(service, profil, permissions, motif));
        return new Publication(version, false);
    }
}
