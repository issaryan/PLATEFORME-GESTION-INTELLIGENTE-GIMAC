import { avatarsSimules } from '@gimac/socle'
import type { Compte } from '@gimac/compte'
import type { EntreeJournal } from '../../modele/comptes'
import type { ConfigurationMessagerie, PolitiqueSecurite } from '../../modele/configuration'
import type { EtatAlertes, EtatServeurs, EtatService, InstantanePlateforme, NiveauxJournalisation, ConfigurationPlateforme } from '../../modele/plateforme'

// Données fictives de la console (développement) : comptes de démonstration super.admin et rssi, agents
// des directions, supervision d'une plateforme de recette. Aucune donnée réelle.

const il = (heures: number) => new Date(Date.now() - heures * 3_600_000).toISOString()

type Graine = [id: string, identifiant: string, prenom: string, nom: string, fonction: string, profils: string[], o?: Partial<Compte>]
const GRAINES: Graine[] = [
  ['u-super-admin', 'super.admin', 'Serge', 'Mouelle Kombi', 'Super administrateur de la plateforme', ['ROLE_SUPER_ADMIN']],
  ['u-rssi', 'rssi', 'Carine', 'Ebongue Ndjock', 'Responsable de la sécurité des SI', ['ROLE_RSSI']],
  ['u-chef-des', 'c.ndong', 'Christian', 'Ndong Obiang', 'Chef du DES', ['ROLE_CHEF_DES']],
  ['u-ingenieur', 'p.mbida', 'Patrick', 'Mbida Essomba', 'Ingénieur d’homologation', ['ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_CONDUCTEUR_PROJET']],
  ['u-chef-ddrm', 'a.ngombarga', 'Aïssatou', 'Ngo Mbarga', 'Cheffe de la DDRM', ['ROLE_CHEF_DDRM']],
  ['u-charge-ddrm', 'j.essomba', 'Joël', 'Essomba Atangana', 'Chargé d’études DDRM', ['ROLE_CHARGE_DDRM']],
  ['u-conformite', 'm.oumarou', 'Mariam', 'Oumarou Bello', 'Responsable conformité', ['ROLE_CONFORMITE']],
  ['u-dg', 'f.tchoumi', 'François', 'Tchoumi Kamga', 'Directeur général adjoint', ['ROLE_DG']],
  ['u-correspondant', 'correspondant.epa', 'Brice', 'Nzeng Mba', 'Correspondant GIMAC', ['ROLE_CORRESPONDANT'], { institutionId: 'INST-0042' }],
  ['u-sna', 'r.biyong', 'Rachel', 'Biyong Mengue', 'Analyste Normes et Agréments', ['ROLE_SNA']],
  ['u-inactif', 'k.ateba', 'Kevin', 'Ateba Owona', 'Ancien opérateur réseau', ['ROLE_POLE_RESEAU'], { actif: false }],
  ['u-premier-acces', 'l.nguema', 'Laetitia', 'Nguema Ondo', 'Opératrice production monétique', ['ROLE_PRODUCTION_MONETIQUE'],
    { profilComplet: false, telephone: null, actionsRequises: ['MOT_DE_PASSE', 'SECOND_FACTEUR', 'VERIFIER_COURRIEL'], secondFacteur: false, emailVerifie: false, creeLe: il(30) }],
  ['u-sans-2fa', 'b.hamadou', 'Bachir', 'Hamadou Ali', 'Ingénieur système', ['ROLE_POLE_SYSTEME'], { secondFacteur: false }],
]

/** Comptes qui ont déposé une photo de profil (démonstration, docs/lot1/mon-compte.md) ; les autres gardent leurs initiales. */
export const PHOTOS_DEMO = ['u-rssi', 'u-chef-des', 'u-chef-ddrm', 'u-conformite', 'u-correspondant', 'u-sna', 'u-dg']

export function comptesInitiaux(): Compte[] {
  PHOTOS_DEMO.forEach((id) => avatarsSimules.demo(id))
  return GRAINES.map(([id, identifiant, prenom, nom, fonction, profils, o], i) => {
    const c: Compte = {
      id, identifiant, prenom, nom, nomAffiche: `${prenom} ${nom}`, email: `${identifiant}@gimac-afr.org`,
      emailVerifie: true, actif: true, creeLe: il(24 * (200 - i * 12)), telephone: `+237 6${(90 + i).toString()} ${(10 + i).toString()} ${(20 + i).toString()} ${(30 + i).toString()}`,
      fonction, langue: 'fr', institutionId: null, profilComplet: true, premierAccesEnAttente: false, profils, actionsRequises: [], secondFacteur: true, avatarVersion: avatarsSimules.version(id), ...o,
    }
    c.premierAccesEnAttente = !c.profilComplet || c.actionsRequises.length > 0
    return c
  })
}

export function journalInitial(): EntreeJournal[] {
  const e = (h: number, action: string, cibleId: string | null, cibleLibelle: string, motif: string | null, details = '', acteur: [string, string] = ['u-super-admin', 'Serge Mouelle Kombi']): EntreeJournal => ({
    id: `j-${action}-${h}`, horodatage: il(h), acteurId: acteur[0], acteurNom: acteur[1], action, cibleId, cibleLibelle, motif, details, correlationId: `c0f${Math.round(h * 1000).toString(16)}-4d2a`,
  })
  return [
    e(2, 'CREATION', 'u-premier-acces', 'l.nguema', null, 'ROLE_PRODUCTION_MONETIQUE'),
    e(5, 'ACTIONS_REQUISES', 'u-sans-2fa', 'b.hamadou', 'Second facteur absent depuis la migration du poste', 'SECOND_FACTEUR', ['u-rssi', 'Carine Ebongue Ndjock']),
    e(20, 'CONFIGURATION_POLITIQUE_SECURITE', null, 'POLITIQUE_SECURITE', 'Alignement sur la PSSI 2026 : 14 caractères', 'longueurMinimale', ['u-rssi', 'Carine Ebongue Ndjock']),
    e(30, 'DESACTIVATION', 'u-inactif', 'k.ateba', 'Départ du collaborateur le 15 septembre', ''),
    e(52, 'PROFILS', 'u-ingenieur', 'p.mbida', 'Désignation comme conducteur de projet LIA-2026-017', 'ROLE_CONDUCTEUR_PROJET, ROLE_INGENIEUR_HOMOLOGATION'),
  ]
}

export const politiqueInitiale = (): PolitiqueSecurite => ({
  longueurMinimale: 14, majusculesMin: 1, minusculesMin: 1, chiffresMin: 1, speciauxMin: 1, historique: 5, expirationJours: 90, interditIdentifiant: true,
  otpChiffres: 6, otpPeriodeSecondes: 30, otpAlgorithme: 'HmacSHA256', sessionInactiviteMinutes: 15, sessionDureeMaxHeures: 8, forceBruteActive: true,
  forceBruteEchecsMax: 5, forceBruteAttenteMinutes: 15,
})

export const messagerieInitiale = (): ConfigurationMessagerie => ({
  hote: 'smtp.gimac-afr.org', port: 587, expediteur: 'moabi@gimac-afr.org', nomExpediteur: 'Plateforme Moabi du GIMAC', starttls: true, ssl: false,
  authentification: true, utilisateur: 'plateforme-des', motDePasse: null, motDePasseDefini: true,
})

// ------------------------------------------------------------------ supervision

const SERVICES: [string, number, string, number, number][] = [
  ['gateway', 8080, '1.4.0', 0.08, 41], ['iam', 8081, '1.2.3', 0.05, 38], ['homologation', 8082, '1.3.0', 0.12, 55], ['certification', 8083, '1.1.2', 0.04, 33],
  ['participant', 8084, '1.0.4', 0.03, 29], ['document', 8085, '1.0.1', 0.06, 47], ['notification', 8086, '0.9.0', 0.02, 21], ['audit', 8087, '1.0.0', 0.07, 62],
]

function service(nom: string, port: number, version: string, cpu: number, tas: number, variation: number, etat: EtatService['etat'] = 'UP'): EtatService {
  const up = etat === 'UP'
  return {
    nom, url: `http://${nom}:${port}`, etat, latenceMs: up ? Math.round(12 + port % 40 + variation * 30) : null,
    composants: up ? { db: 'UP', kafka: 'UP', diskSpace: 'UP' } : {},
    info: { version, artefact: nom, dateConstruction: '2026-09-28T18:04:11Z', commit: 'a17f3c9', branche: 'main' },
    metriques: up
      ? {
          dureeFonctionnementSecondes: 86_400 * 3 + port, cpuProcessus: Math.min(0.95, cpu + variation * 0.05), cpuSysteme: 0.31 + variation * 0.05, nombreCpu: 4,
          memoireTasUtiliseeOctets: tas * 10_485_760, memoireTasMaxOctets: 1_048_576_000, memoireTasPourcent: tas + variation * 3,
          disqueLibreOctets: 61_000_000_000, disqueTotalOctets: 107_374_182_400, chargeMoyenne1m: 1.2 + variation, threadsActifs: 48 + port % 20,
        }
      : null,
    erreur: up ? null : 'Délai de réponse dépassé (2 s)', controleLe: new Date().toISOString(),
  }
}

/** Instantané de la plateforme ; « variation » (0 à 1) anime le flux simulé. */
export function instantane(variation = 0, degradee = true): InstantanePlateforme {
  const services = SERVICES.map(([n, p, v, c, t]) => service(n, p, v, c, t, variation, degradee && n === 'notification' ? 'INJOIGNABLE' : 'UP'))
  const up = services.filter((s) => s.etat === 'UP').length
  return {
    horodatage: new Date().toISOString(),
    global: { etat: up === services.length ? 'OPERATIONNELLE' : up === 0 ? 'HORS_SERVICE' : 'DEGRADEE', total: services.length, up, down: services.length - up, injoignables: services.length - up },
    services,
    keycloak: service('keycloak', 9990, '26.4.0', 0.09, 58, variation),
  }
}

export const serveurs = (): EtatServeurs => ({
  horodatage: new Date().toISOString(), prometheusJoignable: true, erreur: null, ciblesUp: 11, ciblesDown: 1,
  cibles: [
    { job: 'node', instance: 'gimac-app-01:9100', etat: 'up', derniereCollecte: il(0.01), dureeDerniereCollecteSecondes: 0.021, erreur: null },
    { job: 'notification', instance: 'notification:8086', etat: 'down', derniereCollecte: il(0.2), dureeDerniereCollecteSecondes: null, erreur: 'connection refused' },
  ],
  nodeExporter: true,
  hotes: [
    { instance: 'gimac-app-01', cpuPourcent: 42.5, memoirePourcent: 68.1, memoireTotaleOctets: 34_359_738_368, disquePourcent: 57.3, chargeMoyenne1m: 2.4 },
    { instance: 'gimac-app-02', cpuPourcent: 37.9, memoirePourcent: 61.4, memoireTotaleOctets: 34_359_738_368, disquePourcent: 49.8, chargeMoyenne1m: 1.9 },
    { instance: 'gimac-donnees-01', cpuPourcent: 71.2, memoirePourcent: 83.6, memoireTotaleOctets: 68_719_476_736, disquePourcent: 88.4, chargeMoyenne1m: 5.1 },
  ],
  conteneurs: [
    { nom: 'gimac-gateway', cpuCoeurs: 0.31, memoireOctets: 612_368_384 },
    { nom: 'gimac-iam', cpuCoeurs: 0.18, memoireOctets: 498_073_600 },
    { nom: 'gimac-homologation', cpuCoeurs: 0.44, memoireOctets: 731_906_048 },
    { nom: 'keycloak', cpuCoeurs: 0.22, memoireOctets: 804_257_792 },
    { nom: 'postgresql', cpuCoeurs: 0.63, memoireOctets: 1_610_612_736 },
    { nom: 'kafka', cpuCoeurs: 0.38, memoireOctets: 1_207_959_552 },
  ],
})

export const alertes = (): EtatAlertes => ({
  horodatage: new Date().toISOString(), joignable: true, erreur: null, nombre: 3,
  alertes: [
    { nom: 'ServiceInjoignable', severite: 'critical', resume: 'MS-NOTIFICATION ne répond plus à la sonde de santé', description: 'Aucune réponse depuis 12 minutes', depuis: il(0.2), cible: 'notification:8086', job: 'notification', etat: 'firing' },
    { nom: 'DisqueSature', severite: 'warning', resume: 'Disque de gimac-donnees-01 occupé à 88 %', description: 'Seuil d’alerte : 85 %', depuis: il(3), cible: 'gimac-donnees-01:9100', job: 'node', etat: 'firing' },
    { nom: 'CertificatBientotExpire', severite: 'info', resume: 'Le certificat TLS de la passerelle expire dans 21 jours', description: null, depuis: il(26), cible: 'gateway:8080', job: 'gateway', etat: 'firing' },
  ],
})

export const configurationPlateforme = (): ConfigurationPlateforme => ({
  horodatage: new Date().toISOString(),
  routes: [
    { id: 'bff-des-participant', uri: 'http://participant:8084', predicats: 'Paths: [/bff/des/participant/**]', filtres: ['RewritePath /bff/des/participant/(?<s>.*) → /api/v1/${s}', 'OpaAuthorize'], ordre: 0 },
    { id: 'bff-console-iam', uri: 'http://iam:8081', predicats: 'Paths: [/bff/console/iam/**]', filtres: ['RewritePath /bff/console/iam/(?<s>.*) → /api/v1/${s}', 'OpaAuthorize'], ordre: 0 },
    { id: 'bff-des-homologation', uri: 'http://homologation:8082', predicats: 'Paths: [/bff/des/homologation/**]', filtres: ['RewritePath', 'OpaAuthorize'], ordre: 0 },
  ],
  services: [
    { nom: 'iam', url: 'http://iam:8081', lue: true, erreur: null, profilsActifs: ['prod', 'api-docs'],
      proprietes: { 'gimac.webauthn.rp-id': 'des.gimac.local', 'gimac.premiere-connexion.validite-invitation': 'PT72H', 'spring.datasource.url': 'jdbc:postgresql://postgresql:5432/iam', 'spring.datasource.password': '******', 'spring.kafka.bootstrap-servers': 'kafka:9092' } },
    { nom: 'participant', url: 'http://participant:8084', lue: true, erreur: null, profilsActifs: ['prod'],
      proprietes: { 'gimac.document.url': 'http://document:8085', 'spring.datasource.url': 'jdbc:postgresql://postgresql:5432/participant', 'spring.datasource.username': 'participant' } },
    { nom: 'notification', url: 'http://notification:8086', lue: false, erreur: 'Service injoignable', profilsActifs: [], proprietes: {} },
  ],
})

const LOGGERS: [string, string | null, string][] = [
  ['ROOT', 'INFO', 'INFO'], ['cm.gimac.des', 'INFO', 'INFO'], ['cm.gimac.des.iam.web', null, 'INFO'], ['org.hibernate.SQL', 'WARN', 'WARN'],
  ['org.springframework.security', 'INFO', 'INFO'], ['org.apache.kafka', 'WARN', 'WARN'],
]
export const journalisation = (service: string): NiveauxJournalisation => ({
  service, niveauxDisponibles: ['OFF', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'],
  loggers: LOGGERS.map(([nom, niveauConfigure, niveauEffectif]) => ({ nom: nom.replace('.iam.', `.${service}.`), niveauConfigure, niveauEffectif })),
})
