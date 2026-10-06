import type { ApiError } from '@gimac/socle'
import { BORNES_INFRASTRUCTURE, type Charge, type EtatCharge, type EvenementCharge, type Geste, type LigneJournal, type NoeudCluster, type Operation, type Orchestrateur, type PodCharge, type TacheCluster } from '../../modele/infrastructure'
import type { EtatFlux } from '../../modele/plateforme'
import type { ServiceInfrastructure } from '../ports'

// Exploitation simulée de l'infrastructure (D-78) : charges d'une recette Kubernetes (ou Docker), gestes permis
// selon les règles du contrat (§5), opérations poussées sur le flux, mêmes codes de refus que la passerelle.
// Données fictives : aucune charge, image ni journal réels.

export type OptionsInfrastructureSimulee = {
  attendre: <T>(v: T | (() => T)) => Promise<T>
  refus: (status: number, code: string, message: string, details?: Record<string, unknown>) => ApiError
  acteur: { id: string; nom: string; permissions: string[] }
  /** Vérifie l'assertion FIDO2 simulée ; faux : 401 REAUTH_REQUIRED. */
  assertionValide: (assertion: string | undefined) => boolean
  orchestrateur?: Orchestrateur
  /** Durée d'un geste avant son issue, en millisecondes. */
  dureeGesteMs?: number
  latenceMs?: number
}

const il = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString()

type Graine = [nom: string, etat: EtatCharge, sante: Charge['sante'], voulues: number, pretes: number, redemarrages: number, protegee: boolean]
const GRAINES: Graine[] = [
  ['gateway', 'EN_COURS', 'SAINE', 2, 2, 0, true],
  ['frontend-console', 'EN_COURS', 'SAINE', 1, 1, 0, true],
  ['frontend-departement', 'EN_COURS', 'SAINE', 1, 1, 0, false],
  ['frontend-portail', 'EN_COURS', 'SAINE', 1, 1, 0, false],
  ['iam', 'EN_COURS', 'SAINE', 1, 1, 1, false],
  ['homologation', 'EN_COURS', 'SAINE', 2, 2, 0, false],
  ['certification', 'PARTIELLE', 'DEGRADEE', 2, 1, 3, false],
  ['pilotage', 'EN_ERREUR', 'DEGRADEE', 1, 0, 7, false],
  ['collaboration', 'ARRETEE', 'INCONNUE', 0, 0, 0, false],
  ['keycloak', 'EN_COURS', 'SAINE', 1, 1, 0, true],
  ['iam-postgresql', 'EN_COURS', 'SAINE', 1, 1, 0, true],
  ['kafka', 'EN_COURS', 'SAINE', 1, 1, 0, true],
]

/** Charges simulées qui ont une version précédente d'image différente (geste RETABLIR, Kubernetes). */
const VERSIONNEES = ['gateway', 'homologation', 'pilotage']

/** Noms des charges simulées (séries CHARGE_* des graphiques). */
export const NOMS_CHARGES_SIMULEES = (): string[] => GRAINES.map((g) => g[0])

/** Gestes permis à cet instant (§5) ; aucun pendant une opération. */
export function gestesPermis(c: Omit<Charge, 'gestes'>, enCours: boolean): Geste[] {
  if (enCours) return []
  const g: Geste[] = []
  if (c.orchestrateur === 'KUBERNETES') {
    if (c.repliques.voulues > 0) g.push('REDEMARRER', ...(c.protegee ? [] : (['ARRETER'] as Geste[])))
    else g.push('DEMARRER')
    g.push('ECHELLE')
    if (c.repliques.voulues > 0 && c.imagePrecedente) g.push('RETABLIR')
    return g
  }
  if (c.etat !== 'ARRETEE') g.push('REDEMARRER', ...(c.protegee ? [] : (['ARRETER'] as Geste[])))
  if (['ARRETEE', 'PARTIELLE', 'EN_ERREUR'].includes(c.etat)) g.push('DEMARRER')
  return g
}

const LIGNES: [LigneJournal['flux'], string][] = [
  ['stdout', 'Démarrage du service : profil prod, port 8080'],
  ['stdout', 'Connexion au courtier Kafka établie'],
  ['stdout', 'GET /management/health 200 (4 ms)'],
  ['stderr', 'Jeton de service renouvelé : Bearer ****'],
  ['stdout', 'Consommateur abonné au sujet platform.events.v1'],
  ['stderr', 'Délai de réponse dépassé vers MS-IAM, nouvelle tentative'],
  ['stdout', 'Carte 412345******1234 masquée dans le journal applicatif'],
  ['stdout', 'Tâche planifiée terminée en 120 ms'],
]
const DUREE = /^PT([1-9][0-9]*)([SM])$/

export function infrastructureSimulee(o: OptionsInfrastructureSimulee): ServiceInfrastructure {
  const { attendre, refus } = o
  const orchestrateur: Orchestrateur = o.orchestrateur ?? 'KUBERNETES'
  const duree = o.dureeGesteMs ?? 1500
  const enCours = new Set<string>()
  const ecouteurs = new Set<(op: Operation) => void>()
  let n = 0
  const charges: Omit<Charge, 'gestes'>[] = orchestrateur === 'AUCUN' ? [] : GRAINES.map(([nom, etat, sante, voulues, pretes, redemarrages, protegee], i) => ({
    nom, orchestrateur, etat, sante, repliques: { voulues, pretes }, redemarrages, image: `registre.gimac.local/gimac/${nom}:1.4.${i}`,
    demarreeLe: etat === 'ARRETEE' ? null : il(60 * (i + 1)), protegee,
    // Kubernetes : version précédente d'image différente pour les charges déjà livrées plusieurs fois.
    imagePrecedente: orchestrateur === 'KUBERNETES' && VERSIONNEES.includes(nom) ? `registre.gimac.local/gimac/${nom}:1.3.${i}` : null,
  }))
  const acteurOp = { id: o.acteur.id, nomAffiche: o.acteur.nom }
  const historique: Operation[] = [
    ['iam', 'REDEMARRER', 'Prise en compte de la nouvelle configuration SMTP', 'REUSSIE', 30],
    ['certification', 'ECHELLE', 'Montée en charge pour la campagne de certification', 'REUSSIE', 180],
    ['pilotage', 'REDEMARRER', 'Erreurs de connexion à la base après maintenance', 'ECHOUEE', 240],
    ['collaboration', 'ARRETER', 'Maintenance programmée de la messagerie', 'REUSSIE', 600],
  ].map(([charge, geste, motif, resultat, minutes], i) => ({
    id: `op-sim-${i + 1}`, charge: charge as string, geste: geste as Geste, motif: motif as string, acteur: { id: 'u-admin-systeme', nomAffiche: 'Thierry Abessolo Nkoulou' },
    avant: 'EN_COURS', apres: resultat === 'ECHOUEE' ? 'EN_ERREUR' : geste === 'ARRETER' ? 'ARRETEE' : 'EN_COURS', repliquesAvant: 1, repliquesApres: geste === 'ECHELLE' ? 2 : geste === 'ARRETER' ? 0 : 1,
    resultat: resultat as Operation['resultat'], erreur: resultat === 'ECHOUEE' ? 'Délai de démarrage dépassé (90 s)' : null, demandeeLe: il(minutes as number), termineeLe: il((minutes as number) - 1),
  }))

  const vue = (c: Omit<Charge, 'gestes'>): Charge => ({ ...c, gestes: gestesPermis(c, enCours.has(c.nom)) })
  const configuree = () => {
    if (orchestrateur === 'AUCUN') throw refus(422, 'OPS_NON_CONFIGURE', "Exploitation de l'infrastructure non configurée.")
  }
  const trouver = (nom: string) => {
    const c = charges.find((x) => x.nom === nom)
    if (!c) throw refus(404, 'OPS_CHARGE_INCONNUE', 'Charge de travail inconnue.')
    return c
  }
  const pousser = (op: Operation) => ecouteurs.forEach((e) => e(structuredClone(op)))

  const terminer = (op: Operation, c: Omit<Charge, 'gestes'>, repliques: number | undefined) => {
    enCours.delete(c.nom)
    // Échec simulé : redémarrer une charge en erreur (base de données encore indisponible).
    const echec = op.geste === 'REDEMARRER' && op.avant === 'EN_ERREUR'
    if (echec) {
      c.etat = 'EN_ERREUR'
      c.redemarrages += 1
    } else if (op.geste === 'ARRETER' || (op.geste === 'ECHELLE' && repliques === 0)) {
      Object.assign(c, { etat: 'ARRETEE', sante: 'INCONNUE', repliques: { voulues: 0, pretes: 0 }, demarreeLe: null })
    } else {
      const voulues = op.geste === 'ECHELLE' ? repliques! : Math.max(1, c.repliques.voulues)
      Object.assign(c, { etat: 'EN_COURS', sante: 'SAINE', repliques: { voulues, pretes: voulues }, demarreeLe: new Date().toISOString() })
      if (op.geste === 'REDEMARRER') c.redemarrages += 1
      // Comme Kubernetes : la version rétablie devient la plus récente, la version quittée devient la précédente.
      if (op.geste === 'RETABLIR') Object.assign(c, { image: c.imagePrecedente ?? c.image, imagePrecedente: c.image })
    }
    Object.assign(op, {
      resultat: echec ? 'ECHOUEE' : 'REUSSIE', erreur: echec ? 'Délai de démarrage dépassé (90 s)' : null, apres: c.etat,
      repliquesApres: c.repliques.voulues, termineeLe: new Date().toISOString(),
    })
    pousser(op)
  }

  // Lectures Kubernetes : jeu de démonstration ; sous Docker, 422 comme la passerelle.
  const kubernetes = () => {
    configuree()
    if (orchestrateur !== 'KUBERNETES') throw refus(422, 'OPS_NON_PRIS_EN_CHARGE', 'Kubernetes seulement')
  }
  const podsDe = (c: Omit<Charge, 'gestes'>): PodCharge[] =>
    Array.from({ length: Math.max(c.repliques.voulues, c.repliques.pretes) }, (_, k) => {
      const pret = k < c.repliques.pretes
      return {
        nom: `${c.nom}-7d9f${k}c-${'abcde'[k % 5]}${k}x`, phase: pret ? 'Running' : 'Pending', pret, redemarrages: k === 0 ? c.redemarrages : 0,
        noeud: `gimac-noeud-0${(k % 3) + 1}`, demarreLe: il(30 + k * 7), attente: pret ? null : c.etat === 'EN_ERREUR' ? 'CrashLoopBackOff' : 'ContainerCreating',
      }
    })
  const evenementsDe = (c: Omit<Charge, 'gestes'>): EvenementCharge[] => {
    const l: EvenementCharge[] = [
      { instant: il(42), type: 'Normal', raison: 'ScalingReplicaSet', message: `Scaled up replica set ${c.nom}-7d9f to ${c.repliques.voulues}`, objet: `Deployment/${c.nom}`, nombre: 1 },
      { instant: il(41), type: 'Normal', raison: 'Scheduled', message: `Successfully assigned gimac-des/${c.nom}-7d9f0c-a0x to gimac-noeud-01`, objet: `Pod/${c.nom}-7d9f0c-a0x`, nombre: 1 },
      { instant: il(40), type: 'Normal', raison: 'Started', message: 'Started container', objet: `Pod/${c.nom}-7d9f0c-a0x`, nombre: 1 },
    ]
    if (c.etat === 'EN_ERREUR' || c.etat === 'PARTIELLE') {
      l.push({ instant: il(2), type: 'Warning', raison: 'BackOff', message: 'Back-off restarting failed container', objet: `Pod/${c.nom}-7d9f1c-b1x`, nombre: c.redemarrages || 3 })
    }
    return l.sort((a, b) => (b.instant ?? '').localeCompare(a.instant ?? ''))
  }
  const NOEUDS: NoeudCluster[] = [
    { nom: 'gimac-noeud-01', pret: true, roles: ['control-plane'], versionKubelet: 'v1.31.2', cpuAllouable: '7800m', memoireAllouableOctets: 31 * 2 ** 30, nonPlanifiable: false },
    { nom: 'gimac-noeud-02', pret: true, roles: ['worker'], versionKubelet: 'v1.31.2', cpuAllouable: '7800m', memoireAllouableOctets: 31 * 2 ** 30, nonPlanifiable: false },
    { nom: 'gimac-noeud-03', pret: false, roles: ['worker'], versionKubelet: 'v1.31.1', cpuAllouable: '3800m', memoireAllouableOctets: 15 * 2 ** 30, nonPlanifiable: true },
  ]
  const TACHES: TacheCluster[] = [
    { nom: 'jhipster-grafana-dashboard', nature: 'JOB', planification: null, suspendue: null, etat: 'REUSSIE', debut: il(600), fin: il(599), origine: null },
  ]
  return {
    pods: (nom) => attendre(() => { kubernetes(); return podsDe(trouver(nom)) }),
    evenements: (nom) => attendre(() => { kubernetes(); return evenementsDe(trouver(nom)) }),
    noeuds: () => attendre(() => { kubernetes(); return NOEUDS }),
    taches: () => attendre(() => { kubernetes(); return TACHES }),
    charges: () => attendre(() => ({ orchestrateur, charges: charges.map(vue) })),
    journaux: (nom, lignes, depuis) =>
      attendre(() => {
        configuree()
        const c = trouver(nom)
        const d = DUREE.exec(depuis)
        const secondes = d ? Number(d[1]) * (d[2] === 'M' ? 60 : 1) : 0
        if (!Number.isInteger(lignes) || lignes < 1 || lignes > BORNES_INFRASTRUCTURE.lignesMax || secondes < 1 || secondes > 1800) {
          throw refus(400, 'OPS_PARAMETRE_INVALIDE', 'Paramètre de lecture des journaux invalide.')
        }
        const disponibles = c.etat === 'ARRETEE' ? 0 : 24
        const nombre = Math.min(lignes, disponibles)
        const flux = orchestrateur === 'KUBERNETES'
        return {
          charge: nom,
          lignes: Array.from({ length: nombre }, (_, i) => {
            const [f, texte] = LIGNES[i % LIGNES.length]
            return { quand: new Date(Date.now() - (nombre - i) * 7_000).toISOString(), flux: flux ? 'stdout' as const : f, texte: `[${nom}] ${texte}` }
          }),
          tronque: lignes < disponibles,
        }
      }),
    operations: (page, taille) =>
      attendre(() => {
        if (!Number.isInteger(page) || page < 0 || !Number.isInteger(taille) || taille < 1 || taille > 100) throw refus(400, 'OPS_PARAMETRE_INVALIDE', 'Pagination invalide.')
        return { contenu: historique.slice(page * taille, (page + 1) * taille), total: historique.length, page, taille }
      }),
    executer: (nom, geste, d, assertion) =>
      attendre(() => {
        if (!o.acteur.permissions.includes('PLATFORM_OPS_EXECUTE')) throw refus(403, 'HTTP_403', 'Accès refusé.')
        if (!o.assertionValide(assertion)) throw refus(401, 'REAUTH_REQUIRED', 'Confirmation par la clé de sécurité exigée.')
        configuree()
        const c = trouver(nom)
        const motif = d.motif?.trim() ?? ''
        if (motif.length < BORNES_INFRASTRUCTURE.motifMin || motif.length > BORNES_INFRASTRUCTURE.motifMax) throw refus(400, 'OPS_MOTIF_INVALIDE', 'Motif obligatoire (5 à 500 caractères).')
        if (geste === 'ECHELLE') {
          if (orchestrateur !== 'KUBERNETES') throw refus(422, 'OPS_NON_PRIS_EN_CHARGE', 'Geste non pris en charge par cet orchestrateur.')
          const r = d.repliques
          if (r === undefined || !Number.isInteger(r) || r < BORNES_INFRASTRUCTURE.repliquesMin || r > BORNES_INFRASTRUCTURE.repliquesMax) {
            throw refus(400, 'OPS_REPLIQUES_INVALIDES', 'Nombre de répliques attendu entre 0 et 5.')
          }
          if (c.protegee && r === 0) throw refus(409, 'OPS_CHARGE_PROTEGEE', 'Charge protégée : elle ne peut être arrêtée ni ramenée à zéro réplique.')
        }
        if (geste === 'RETABLIR' && orchestrateur !== 'KUBERNETES') throw refus(422, 'OPS_NON_PRIS_EN_CHARGE', 'Retour à la version précédente : Kubernetes seulement')
        if (geste === 'ARRETER' && c.protegee) throw refus(409, 'OPS_CHARGE_PROTEGEE', 'Charge protégée : elle ne peut être arrêtée ni ramenée à zéro réplique.')
        if (enCours.has(nom)) throw refus(409, 'OPS_OPERATION_EN_COURS', 'Une opération est déjà en cours sur cette charge.')
        if (!gestesPermis(c, false).includes(geste)) throw refus(409, 'OPS_GESTE_NON_PERMIS', "Geste sans objet dans l'état actuel de la charge.")
        const op: Operation = {
          id: `op-sim-${historique.length + ++n}`, charge: nom, geste, motif, acteur: acteurOp, avant: c.etat, apres: null, repliquesAvant: c.repliques.voulues,
          repliquesApres: null, resultat: 'EN_COURS', erreur: null, demandeeLe: new Date().toISOString(), termineeLe: null,
        }
        enCours.add(nom)
        if (geste === 'REDEMARRER' || geste === 'DEMARRER' || geste === 'RETABLIR') c.etat = 'REDEMARRAGE'
        historique.unshift(op)
        pousser(op)
        window.setTimeout(() => terminer(op, c, d.repliques), duree)
        return op
      }),
    flux: (e) => {
      const ecouteur = (op: Operation) => e.onOperation(op)
      ecouteurs.add(ecouteur)
      e.onEtat('connexion')
      const id = window.setTimeout(() => e.onEtat('connecte' as EtatFlux), o.latenceMs ?? 300)
      return () => {
        window.clearTimeout(id)
        ecouteurs.delete(ecouteur)
      }
    },
  }
}
