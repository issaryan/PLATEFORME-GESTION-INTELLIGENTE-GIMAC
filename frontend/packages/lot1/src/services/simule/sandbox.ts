import type { TypeProjet } from '../../modele/commun'
import { MENTION_REPRIS, type CampagneSandbox, type ExecutionSandbox, type PageExecutionsSandbox, type ResultatsSandbox } from '../../modele/sandbox'
import { empreinte } from './donnees'

// Résultats de la sandbox simulés au contrat exact de MS-HOMOLOGATION (ResultatsSandbox, US-EXI-14, US-EXI-17) : une
// campagne close reprise pour les liaisons en essais ou au-delà, deux exécutions d'un scénario inconnu, une relève en
// échec pour un projet délégataire en essais (critère 3 de US-EXI-17). Aucune règle propre : rien n'est recalculé.

export type LiaisonDemo = { id: string; typeProjet: TypeProjet; statut: string; services: { code: string; inclus: boolean }[] }

const EN_ESSAIS_OU_AU_DELA = ['EN_TEST', 'APPROUVEE_SECURITE', 'CERTIFIEE', 'EN_PRODUCTION']
const RELEVE = '2026-10-22T08:05:00Z'
const EXECUTIONS = 45
const interfaceDe = (t: TypeProjet) => (t === 'PAIEMENT_MOBILE' ? 'MOB' : 'CAR')
const campagneDe = (l: LiaisonDemo) => `0192f0aa-0000-7000-8000-${l.id.replace(/\D/g, '').padStart(12, '0')}`
const scenarioDe = (i: number, l: LiaisonDemo) => (i <= 2 ? `SCN-INCONNU-${i}` : `SCN-${interfaceDe(l.typeProjet)}-00${(i % 3) + 1}`)
const echoue = (i: number) => i % 9 === 0

function executionDemo(l: LiaisonDemo, i: number): ExecutionSandbox {
  const ko = echoue(i)
  return {
    identifiantExecution: `EXE-${String(i).padStart(6, '0')}`,
    identifiantCas: `CAS-${String(i).padStart(3, '0')}`,
    identifiantScenario: scenarioDe(i, l),
    versionScenario: '3',
    dateExecution: `2026-10-13T16:${String(i % 60).padStart(2, '0')}:07Z`,
    verdictNatif: ko ? 'FAILED' : 'PASSED',
    verdictNormalise: ko ? 'defavorable' : 'favorable',
    motif: ko ? 'Code réponse 05 au lieu de 00' : null,
    modeExecution: 'hybride',
    referenceTrace: `TRC-${55100 + i}`,
    nature: 'VALIDATION',
    versionTable: '1.0',
    origine: 'PLATEFORME',
    operationCode: i <= 2 ? null : `OP-E${String((i % 6) + 1).padStart(2, '0')}`,
  }
}

function campagneDemo(l: LiaisonDemo): CampagneSandbox {
  const services = l.services.filter((s) => s.inclus).map((s) => s.code)
  const id = campagneDe(l)
  const parService = Math.max(1, Math.floor(EXECUTIONS / Math.max(1, services.length)))
  return {
    campagneId: id,
    code: `CMP-${l.id}/${interfaceDe(l.typeProjet)}`,
    etat: 'TERMINEE',
    interfaceTest: interfaceDe(l.typeProjet),
    services,
    versionBilan: 1,
    versionTable: '1.0',
    signalement: null,
    taux: [
      { service: null, nature: 'AVANCEMENT', numerateur: EXECUTIONS, denominateur: EXECUTIONS, valeur: '100.00', versionFormule: '1.0', horodatage: '2026-10-13T17:00:00Z' },
      { service: null, nature: 'REUSSITE', numerateur: EXECUTIONS - 1, denominateur: EXECUTIONS, valeur: ((Math.floor(((EXECUTIONS - 1) / EXECUTIONS) * 10000)) / 100).toFixed(2), versionFormule: '1.0', horodatage: '2026-10-13T17:00:00Z' },
      ...services.map((s, i) => {
        const reussies = i === 1 ? parService - 1 : parService
        return { service: s, nature: 'REUSSITE', numerateur: reussies, denominateur: parService, valeur: ((Math.floor((reussies / parService) * 10000)) / 100).toFixed(2), versionFormule: '1.0', horodatage: '2026-10-13T17:00:00Z' }
      }),
    ],
    executions: EXECUTIONS,
    nonRapprochees: 2,
    pieces: [
      { nature: 'BILAN', version: 1, empreinte: empreinte(`${id}:bilan`), taille: 18_432, nom: 'bilan-certification-v1.pdf', documentId: `doc-${l.id}-bilan`, statut: 'DEJA_CONSERVEE' },
      { nature: 'RESULTATS_DETAILLES', version: 1, empreinte: empreinte(`${id}:resultats`), taille: 96_210, nom: 'resultats-detailles-v1.json', documentId: `doc-${l.id}-resultats`, statut: 'DEPOSEE' },
    ],
    releveLe: RELEVE,
  }
}

/** Résultats d'une liaison : aucune campagne avant les essais ; relève en échec pour un délégataire en essais. */
export function resultatsSandboxDemo(l: LiaisonDemo): ResultatsSandbox {
  const reprise = EN_ESSAIS_OU_AU_DELA.includes(l.statut)
  const enEchec = reprise && l.typeProjet === 'DELEGATAIRE' && l.statut === 'EN_TEST'
  const campagnes = reprise ? [campagneDemo(l)] : []
  return {
    liaisonId: l.id,
    mention: MENTION_REPRIS,
    derniereReleveReussie: reprise ? RELEVE : null,
    dernierEchec: enEchec ? '2026-10-22T09:10:00Z' : null,
    motifEchec: enEchec ? 'SBX-LECTURE répond 503' : null,
    enEchec,
    campagnes,
    scenariosNonRapproches: reprise
      ? [1, 2].map((i) => ({ id: `${l.id}-U${i}`, idScenario: `SCN-INCONNU-${i}`, campagneId: campagnes[0].campagneId, qualification: 'A_QUALIFIER', dateDetection: RELEVE }))
      : [],
  }
}

/** Exécutions retenues de la campagne, triées par date puis identifiant, page à partir de 0 (1 à 100 par page). */
export function executionsSandboxDemo(l: LiaisonDemo, campagneId: string, page: number, taille: number): PageExecutionsSandbox {
  const t = Math.min(100, Math.max(1, taille))
  const toutes = EN_ESSAIS_OU_AU_DELA.includes(l.statut) && campagneId === campagneDe(l)
    ? Array.from({ length: EXECUTIONS }, (_, i) => executionDemo(l, i + 1)).sort((a, b) => a.dateExecution.localeCompare(b.dateExecution) || a.identifiantExecution.localeCompare(b.identifiantExecution))
    : []
  const p = Math.max(0, page)
  return { contenu: toutes.slice(p * t, (p + 1) * t), total: toutes.length, page: p, taille: t }
}
