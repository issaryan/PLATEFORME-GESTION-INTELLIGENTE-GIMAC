import { ApiError } from '@gimac/socle'
import { PERMISSION, type ActeEspace, type DemandeOuverture, type Espace, type Ouverture, type PageEspaces } from '../modele'
import type { ServiceEspaces, ServicesSandbox } from './ports'
import { activiteSimulee, type OptionsActiviteSimulee } from '../activite/simule'
import { supervisionSimulee, type OptionsSupervisionSimulee } from './supervisionSimulee'

// SBX-ESPACE simulé au contrat exact des routes de la console (EspaceConsoleController, ProblemesEspace, Appelant et
// EspaceInstitution de la branche sbx-espace-hexagone) : jamais servi par une construction de production.
// - lecture : SBX_HAB_CONSULTATION d'un compte du GIMAC, sinon 404 SBX_ESP_INTROUVABLE (indiscernable d'une absence) ;
// - écriture : SBX_HAB_ADMINISTRATION et second facteur (422 SBX_ESP_SECOND_FACTEUR_EXIGE), clé d'idempotence
//   obligatoire (400 SBX_ESP_CLE_IDEMPOTENCE_ABSENTE, 422 …_INVALIDE) ; une clé rejouée rend le résultat d'origine ;
// - tout refus « introuvable » rend 404 SBX_ESP_INTROUVABLE, sans détail.

/** Institution du registre de MS-PARTICIPANT vue par SBX-ESPACE : statut et correspondants actifs. */
export type InstitutionRegistre = { codeParticipant: string; statut: string; versionStatut: number; correspondants: string[] }

export type OptionsEspacesSimules = {
  permissions: string[]
  /** Compte d'une institution : toute route de la console lui est introuvable. */
  institution?: string | null
  deuxFacteurs?: boolean
  registre?: InstitutionRegistre[]
  espaces?: Espace[]
  maintenant?: () => Date
  latenceMs?: number
}

export const STATUTS_ADMIS = ['CANDIDAT', 'EN_HOMOLOGATION', 'ACTIF', 'A_REGULARISER']
export const CORRESPONDANT_99004 = '7b0c4d1e-2f3a-4b5c-8d6e-9f0a1b2c3d44'

/** Registre et espaces de démonstration (institutions fictives 99001 à 99005). */
export const REGISTRE_DEMO: InstitutionRegistre[] = [
  { codeParticipant: '99001', statut: 'EN_HOMOLOGATION', versionStatut: 3, correspondants: ['7b0c4d1e-2f3a-4b5c-8d6e-9f0a1b2c3d41'] },
  { codeParticipant: '99002', statut: 'SUSPENDU', versionStatut: 5, correspondants: [] },
  { codeParticipant: '99003', statut: 'RETIRE', versionStatut: 7, correspondants: [] },
  { codeParticipant: '99004', statut: 'EN_HOMOLOGATION', versionStatut: 1, correspondants: [CORRESPONDANT_99004] },
  { codeParticipant: '99005', statut: 'ACTIF', versionStatut: 2, correspondants: ['7b0c4d1e-2f3a-4b5c-8d6e-9f0a1b2c3d45'] },
]

const espace = (codeParticipant: string, o: Partial<Espace>): Espace => ({
  codeEspace: `ESP-${codeParticipant}`, codeParticipant, etat: 'OUVERT', motifsSuspension: [], statutRegistre: null, versionStatutRegistre: 0,
  administrateursActifs: 1, dateOuverture: null, dateSuspension: null, dateFermeture: null, ...o,
})

export const ESPACES_DEMO: Espace[] = [
  espace('99001', { statutRegistre: 'EN_HOMOLOGATION', versionStatutRegistre: 3, administrateursActifs: 2, dateOuverture: '2026-10-01T08:30:00Z' }),
  espace('99002', { etat: 'SUSPENDU', motifsSuspension: ['STATUT_REGISTRE'], statutRegistre: 'SUSPENDU', versionStatutRegistre: 5, dateOuverture: '2026-09-15T09:00:00Z', dateSuspension: '2026-10-12T14:05:00Z' }),
  espace('99003', { etat: 'SUSPENDU', motifsSuspension: ['DECISION_GIMAC', 'STATUT_REGISTRE'], statutRegistre: 'RETIRE', versionStatutRegistre: 7, dateOuverture: '2026-08-20T10:00:00Z', dateSuspension: '2026-10-02T07:45:00Z' }),
]

let compteurCorrelation = 0
const refus = (status: number, code: string, detail: string | null) => new ApiError(status, code, detail ?? '', `sim-sbx-${++compteurCorrelation}`)
const introuvable = () => refus(404, 'SBX_ESP_INTROUVABLE', null)
const CLE = /^[\x21-\x7E]{8,100}$/
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export function espacesSimules(o: OptionsEspacesSimules): ServiceEspaces {
  const registre = new Map((o.registre ?? REGISTRE_DEMO).map((i) => [i.codeParticipant, i]))
  const espaces = new Map((o.espaces ?? ESPACES_DEMO).map((x) => [x.codeParticipant, { ...x, motifsSuspension: [...x.motifsSuspension] }]))
  const rejouees = new Map<string, unknown>()
  const maintenant = () => (o.maintenant ? o.maintenant() : new Date()).toISOString()
  const attendre = <T,>(f: () => T): Promise<T> =>
    new Promise((ok, ko) => setTimeout(() => {
      try {
        ok(f())
      } catch (err) {
        ko(err)
      }
    }, o.latenceMs ?? 0))
  const lecteur = () => {
    if (o.institution || !o.permissions.includes(PERMISSION.consultation)) throw introuvable()
  }
  // Ordre du contrôleur : en-tête puis corps (résolution des arguments, 400), appelant (404, 422), format de la clé (422).
  const administrateur = (cle: string, corpsValide = true) => {
    if (!cle) throw refus(400, 'SBX_ESP_CLE_IDEMPOTENCE_ABSENTE', 'Idempotency-Key')
    if (!corpsValide) throw refus(400, 'HTTP_400', 'Saisie refusée')
    if (o.institution || !o.permissions.includes(PERMISSION.administration)) throw introuvable()
    if (o.deuxFacteurs === false) throw refus(422, 'SBX_ESP_SECOND_FACTEUR_EXIGE', 'second facteur exigé')
    if (!CLE.test(cle)) throw refus(422, 'SBX_ESP_CLE_IDEMPOTENCE_INVALIDE', "clé d'idempotence de 8 à 100 caractères")
  }
  /** Acte idempotent : la clé rejouée rend le résultat d'origine, sans rien refaire. */
  const une = <T,>(cle: string, f: () => T): T => {
    if (rejouees.has(cle)) return structuredClone(rejouees.get(cle)) as T
    const r = f()
    rejouees.set(cle, structuredClone(r))
    return r
  }
  const exiger = (condition: boolean, code: string) => {
    if (!condition) throw refus(409, code, "transition refusée par l'état de l'espace")
  }
  const copie = (x: Espace): Espace => ({ ...x, motifsSuspension: [...x.motifsSuspension].sort() })
  const trouver = (code: string) => {
    const x = espaces.get(code)
    if (!x) throw introuvable()
    return x
  }

  return {
    lister: (q) => attendre(() => {
      lecteur()
      const taille = Math.min(Math.max(q.taille ?? 20, 1), 100)
      const tous = [...espaces.values()].sort((a, b) => a.codeParticipant.localeCompare(b.codeParticipant)).filter((x) => !q.apres || x.codeParticipant > q.apres)
      const page = tous.slice(0, taille)
      return { elements: page.map(copie), suivant: page.length === taille ? page[page.length - 1].codeParticipant : null } satisfies PageEspaces
    }),
    consulter: (code) => attendre(() => {
      lecteur()
      return copie(trouver(code))
    }),
    ouvrir: (d: DemandeOuverture, cle) => attendre(() => {
      administrateur(cle, /^[0-9]{5}$/.test(String(d.codeParticipant)) && UUID.test(String(d.correspondantId)))
      return une(cle, (): Ouverture => {
        const inst = registre.get(d.codeParticipant)
        if (!inst) throw introuvable()
        if (!STATUTS_ADMIS.includes(inst.statut)) throw refus(409, 'SBX_ESP_STATUT_NON_ADMIS', `statut ${inst.statut}`)
        if (espaces.has(d.codeParticipant)) throw refus(409, 'SBX_ESP_ESPACE_EXISTANT', d.codeParticipant)
        if (!inst.correspondants.includes(d.correspondantId)) throw refus(422, 'SBX_ESP_CORRESPONDANT_INCONNU', 'correspondant inconnu')
        espaces.set(d.codeParticipant, espace(d.codeParticipant, { statutRegistre: inst.statut, versionStatutRegistre: inst.versionStatut, dateOuverture: maintenant() }))
        return { idSaga: `saga-${d.codeParticipant}-${rejouees.size + 1}`, etatSaga: 'TERMINEE', codeParticipant: d.codeParticipant, etatEspace: 'OUVERT', motifEchec: null }
      })
    }),
    agir: (code, acte: ActeEspace, cle) => attendre(() => {
      administrateur(cle)
      return une(cle, () => {
        const x = trouver(code)
        if (acte === 'suspension') {
          exiger(x.etat === 'OUVERT' || x.etat === 'SUSPENDU', 'SBX_ESP_ETAT_INCOMPATIBLE')
          if (!x.motifsSuspension.includes('DECISION_GIMAC')) x.motifsSuspension.push('DECISION_GIMAC')
          if (x.etat !== 'SUSPENDU') {
            x.etat = 'SUSPENDU'
            x.dateSuspension = maintenant()
          }
        } else if (acte === 'retablissement') {
          exiger(x.etat === 'SUSPENDU', 'SBX_ESP_ETAT_INCOMPATIBLE')
          exiger(!x.motifsSuspension.includes('STATUT_REGISTRE'), 'SBX_ESP_REGISTRE_OPPOSE')
          x.motifsSuspension = []
          x.etat = 'OUVERT'
          x.dateSuspension = null
        } else {
          exiger(x.etat === 'SUSPENDU', 'SBX_ESP_ETAT_INCOMPATIBLE')
          exiger(x.statutRegistre === 'RETIRE', 'SBX_ESP_NON_RETIRE')
          x.etat = 'FERME'
          x.dateFermeture = maintenant()
        }
        return copie(x)
      })
    }),
  }
}

export const servicesSandboxSimules = (o: OptionsEspacesSimules & OptionsSupervisionSimulee & Partial<OptionsActiviteSimulee>): ServicesSandbox => ({
  espaces: espacesSimules(o),
  supervision: supervisionSimulee(o),
  activite: activiteSimulee(o),
})
