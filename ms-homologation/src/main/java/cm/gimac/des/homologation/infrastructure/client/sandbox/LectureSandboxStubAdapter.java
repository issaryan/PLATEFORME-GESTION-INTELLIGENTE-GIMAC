package cm.gimac.des.homologation.infrastructure.client.sandbox;

import cm.gimac.des.homologation.application.port.out.LectureSandboxPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * SBX-LECTURE de substitution (développement, tests), au contrat exact de l'annexe D : réponses JSON conformes aux
 * schémas publiés, relues par le même traducteur que l'adaptateur réel. Positions strictement croissantes par campagne,
 * jeton opaque lié à sa campagne (400 sinon), page close si et seulement si pleine, création PENDING puis validation,
 * pièces dont l'empreinte est celle de leurs octets, indisponibilité (503) et surcharge (429) avec délai, campagne en
 * collision remplacée par un signalement. Alimenté par les tests et la recette ({@link #alimenter}).
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.sbx-lecture", havingValue = "stub", matchIfMissing = true)
public class LectureSandboxStubAdapter implements LectureSandboxPort {

    static final int TAILLE_MAX = 100;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Campagne servie : en-tête (forme du détail), enregistrements dans l'ordre de leur position, pièces. */
    public static final class CampagneServie {

        final String id;
        final UUID liaison;
        final ObjectNode entete;
        final List<ObjectNode> enregistrements = new ArrayList<>();
        final Map<String, byte[]> pieces = new LinkedHashMap<>();
        String signalement;
        String empreinteFalsifiee;

        CampagneServie(String id, UUID liaison, ObjectNode entete) {
            this.id = id;
            this.liaison = liaison;
            this.entete = entete;
        }

        /** Ajoute un enregistrement à la position suivante (création, validation ou rectification). */
        public CampagneServie enregistrer(String execution, String scenario, String cas, String verdictNatif, String verdictNormalise, String nature, String date) {
            ObjectNode e = JSON.createObjectNode();
            e.put("identifiantCampagne", id).put("identifiantLiaison", liaison.toString()).put("codeParticipant", entete.get("codeParticipant").asString());
            e.put("identifiantCas", cas).put("identifiantScenario", scenario).put("versionScenario", "3").put("identifiantExecution", execution);
            e.put("dateExecution", date).put("verdictNatif", verdictNatif).put("verdictNormalise", verdictNormalise);
            e.putNull("motif");
            e.put("modeExecution", "hybride").put("referenceTrace", "TRC-" + execution).put("nature", nature).put("versionTable", "1.0").put("origine", "PLATEFORME");
            e.put("position", "");
            enregistrements.add(e);
            return this;
        }

        /** Validation d'une exécution : création PENDING puis validation au verdict donné. */
        public CampagneServie executer(String execution, String scenario, String verdictNatif, String verdictNormalise, String date) {
            enregistrer(execution, scenario, "CAS-" + scenario, "PENDING", null, "CREATION", date);
            return enregistrer(execution, scenario, "CAS-" + scenario, verdictNatif, verdictNormalise, "VALIDATION", date);
        }

        public CampagneServie taux(String service, String nature, int numerateur, int denominateur, String valeur, String horodatage) {
            ObjectNode t = ((ArrayNode) entete.get("taux")).addObject();
            if (service == null) {
                t.putNull("service");
            } else {
                t.put("service", service);
            }
            t.put("nature", nature).put("numerateur", numerateur).put("denominateur", denominateur).put("valeur", valeur).put("versionFormule", "1.0").put("horodatage", horodatage);
            return this;
        }

        public CampagneServie etat(String etat, Integer versionBilan) {
            entete.put("etat", etat);
            if (versionBilan != null) {
                entete.put("versionBilan", versionBilan);
            }
            return this;
        }

        public CampagneServie piece(String nature, int version, String contenu) {
            pieces.put(nature + "/" + version, contenu.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public CampagneServie signaler(String motif) {
            signalement = motif;
            return this;
        }

        /** HEAD annonce une autre empreinte que celle des octets (contrôle de l'empreinte). */
        public CampagneServie falsifierEmpreinte(String hex) {
            empreinteFalsifiee = hex;
            return this;
        }
    }

    private final Map<String, CampagneServie> campagnes = new ConcurrentHashMap<>();
    private volatile int indisponibles;
    private volatile int avantPanne;
    private volatile int statut = 503;
    private volatile Duration delai = Duration.ofSeconds(30);

    /** Nouvelle campagne d'une liaison (état EN_COURS, aucun taux). */
    public CampagneServie alimenter(UUID liaison, String campagneId, String codeParticipant, String interfaceTest, List<String> services) {
        ObjectNode c = JSON.createObjectNode();
        c.put("identifiantCampagne", campagneId).put("code", "CMP-" + codeParticipant + "/" + interfaceTest).put("identifiantLiaison", liaison.toString());
        c.put("codeParticipant", codeParticipant).put("interfaceTest", interfaceTest).put("etat", "EN_COURS").put("versionTable", "1.0");
        ArrayNode s = c.putArray("services");
        services.forEach(s::add);
        c.putArray("taux");
        CampagneServie cs = new CampagneServie(campagneId, liaison, c);
        campagnes.put(campagneId, cs);
        return cs;
    }

    /** Les {@code fois} appels suivants répondent {@code statut} (503 ou 429) avec le délai indicatif donné. */
    public void indisponible(int fois, int statutHttp, Duration retryAfter) {
        indisponibleApres(0, fois, statutHttp, retryAfter);
    }

    /** Comme {@link #indisponible}, après {@code appels} appels servis normalement (panne au milieu d'une relève). */
    public void indisponibleApres(int appels, int fois, int statutHttp, Duration retryAfter) {
        this.avantPanne = appels;
        this.indisponibles = fois;
        this.statut = statutHttp;
        this.delai = retryAfter;
    }

    public void vider() {
        campagnes.clear();
        indisponibles = 0;
        avantPanne = 0;
    }

    @Override
    public List<CampagneListee> campagnes(UUID liaisonId) {
        return TraducteurLectureSandbox.campagnes(jsonCampagnes(liaisonId));
    }

    /** Réponse JSON de GET /v1/liaisons/{id}/campagnes (contrat lisible par machine, tests de contrat). */
    public byte[] jsonCampagnes(UUID liaisonId) {
        disponible();
        ObjectNode r = JSON.createObjectNode();
        ArrayNode a = r.putArray("campagnes");
        campagnes.values().stream().filter(c -> c.liaison.equals(liaisonId)).sorted((x, y) -> x.id.compareTo(y.id)).forEach(c -> {
            ObjectNode o = a.addObject().put("identifiantCampagne", c.id).put("code", c.entete.get("code").asString());
            if (c.signalement != null) {
                o.put("etat", "SIGNALEE").put("signalement", c.signalement);
            } else {
                o.put("etat", c.entete.get("etat").asString()).putNull("signalement");
            }
        });
        return octets(r);
    }

    @Override
    public Campagne campagne(String campagneId) {
        return TraducteurLectureSandbox.campagne(jsonCampagne(campagneId));
    }

    /** Réponse JSON de GET /v1/campagnes/{c}. */
    public byte[] jsonCampagne(String campagneId) {
        return octets(servie(campagneId).entete);
    }

    @Override
    public PageExecutions executions(String campagneId, String depuis, int taille) {
        return TraducteurLectureSandbox.page(jsonExecutions(campagneId, depuis, taille));
    }

    /** Réponse JSON de GET /v1/campagnes/{c}/executions?depuis=&taille=. */
    public byte[] jsonExecutions(String campagneId, String depuis, int taille) {
        CampagneServie c = servie(campagneId);
        long p0 = depuis == null ? 0 : position(campagneId, depuis);
        int k = Math.clamp(taille, 1, TAILLE_MAX);
        ObjectNode r = JSON.createObjectNode();
        ArrayNode a = r.putArray("enregistrements");
        long dernier = p0;
        for (int i = (int) p0; i < c.enregistrements.size() && a.size() < k; i++) {
            dernier = i + 1L;
            a.add(c.enregistrements.get(i).deepCopy().put("position", jeton(campagneId, dernier)));
        }
        r.put("positionSuivante", jeton(campagneId, dernier)).put("close", a.size() == k).put("etiquette", hex(sha256(octets(a))));
        return octets(r);
    }

    @Override
    public List<Piece> pieces(String campagneId) {
        return TraducteurLectureSandbox.pieces(jsonPieces(campagneId));
    }

    /** Réponse JSON de GET /v1/campagnes/{c}/pieces. */
    public byte[] jsonPieces(String campagneId) {
        CampagneServie c = servie(campagneId);
        ObjectNode r = JSON.createObjectNode();
        ArrayNode a = r.putArray("pieces");
        c.pieces.forEach((cle, contenu) -> {
            String[] nv = cle.split("/");
            a.addObject().put("nature", nv[0]).put("version", Integer.parseInt(nv[1])).put("empreinte", hex(sha256(contenu))).put("taille", contenu.length).put("nom", nv[0].toLowerCase(java.util.Locale.ROOT) + "-v" + nv[1] + ".json");
        });
        return octets(r);
    }

    @Override
    public String empreinte(String campagneId, String nature, int version) {
        CampagneServie c = servie(campagneId);
        String hex = c.empreinteFalsifiee != null ? c.empreinteFalsifiee : hex(sha256(contenu(c, nature, version)));
        return TraducteurLectureSandbox.empreinteRepr(TraducteurLectureSandbox.reprDigest(hex));
    }

    @Override
    public byte[] telecharger(String campagneId, String nature, int version) {
        return contenu(servie(campagneId), nature, version).clone();
    }

    private static byte[] contenu(CampagneServie c, String nature, int version) {
        byte[] b = c.pieces.get(nature + "/" + version);
        if (b == null) {
            throw new HorsContrat("SBX-LECTURE répond 404");
        }
        return b;
    }

    /** Campagne du périmètre ; absente ou en collision : 404 ou 409, hors contrat pour le lecteur. */
    private CampagneServie servie(String campagneId) {
        disponible();
        CampagneServie c = campagnes.get(campagneId);
        if (c == null) {
            throw new HorsContrat("SBX-LECTURE répond 404");
        }
        if (c.signalement != null) {
            throw new HorsContrat("SBX-LECTURE répond 409");
        }
        return c;
    }

    private void disponible() {
        if (indisponibles > 0) {
            if (avantPanne > 0) {
                avantPanne--;
                return;
            }
            indisponibles--;
            throw new Indisponible("SBX-LECTURE répond " + statut, delai);
        }
    }

    /** Jeton opaque : base64url de (v1, campagne, position) ; présenté pour une autre campagne, ou altéré : 400. */
    static String jeton(String campagneId, long position) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(("v1|" + campagneId + "|" + position).getBytes(StandardCharsets.UTF_8));
    }

    static long position(String campagneId, String jeton) {
        try {
            String[] p = new String(Base64.getUrlDecoder().decode(jeton), StandardCharsets.UTF_8).split("\\|");
            if (p.length == 3 && "v1".equals(p[0]) && campagneId.equals(p[1])) {
                return Long.parseLong(p[2]);
            }
        } catch (IllegalArgumentException e) {
            // Jeton altéré : même réponse qu'un jeton d'une autre campagne.
        }
        throw new HorsContrat("SBX-LECTURE répond 400 : position invalide");
    }

    private static byte[] octets(Object n) {
        return JSON.writeValueAsBytes(n);
    }

    static byte[] sha256(byte[] b) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(b);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hex(byte[] b) {
        return HexFormat.of().formatHex(b);
    }
}
