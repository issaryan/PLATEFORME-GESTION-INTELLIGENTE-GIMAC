import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@gimac/socle'
import { activiteHttp } from './http'
import { activiteSimulee, debutPeriode, CAMPAGNES_DEMO, EXECUTIONS_DEMO, LIAISONS_DEMO } from './simule'
import { jours, lireVue, pas, pourcent, serie } from './filtres'
import { etapes } from './Tableaux'
import { PERMISSION_ACTIVITE, PERMISSION_LIAISON, type FiltreActivite } from './modele'

const json = (corps: unknown) => new Response(JSON.stringify(corps), { status: 200, headers: { 'Content-Type': 'application/json' } })
const F: FiltreActivite = { du: '2026-10-01T00:00:00Z', au: '2026-11-01T00:00:00Z' }

afterEach(() => vi.restoreAllMocks())

describe('SBX-LECTURE par la passerelle', () => {
  it('cinq lectures exactes sous /bff/des/sbx-lecture/statistiques et la liaison sous MS-HOMOLOGATION', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({}))
    const g = { ...F, participant: '99001', interface: 'CARTE' as const }
    await activiteHttp.synthese(F)
    await activiteHttp.activite(g, 'SEMAINE')
    await activiteHttp.participants(F, { page: 1, taille: 10 })
    await activiteHttp.campagnes(F, 'TAUX_REUSSITE', { page: 0, taille: 8 })
    await activiteHttp.cas(F, { page: 0, taille: 8 })
    await activiteHttp.liaison('l 1')
    expect(await activiteHttp.codesRetour(F, { page: 0, taille: 8 })).toBeNull()
    const urls = spy.mock.calls.map(([u]) => { const x = new URL(String(u)); return `${x.pathname}${x.search}` })
    const p = 'du=2026-10-01T00%3A00%3A00Z&au=2026-11-01T00%3A00%3A00Z'
    expect(urls).toEqual([
      `/bff/des/sbx-lecture/statistiques/synthese?${p}`,
      `/bff/des/sbx-lecture/statistiques/activite?${p}&participant=99001&interface=CARTE&granularite=SEMAINE`,
      `/bff/des/sbx-lecture/statistiques/participants?${p}&page=1&taille=10`,
      `/bff/des/sbx-lecture/statistiques/campagnes?${p}&tri=TAUX_REUSSITE&page=0&taille=8`,
      `/bff/des/sbx-lecture/statistiques/cas?${p}&page=0&taille=8`,
      '/bff/des/homologation/liaisons/l%201',
    ])
  })
})

describe('statistiques simulées au contrat de SBX-LECTURE', () => {
  const s = activiteSimulee({ permissions: [PERMISSION_ACTIVITE, PERMISSION_LIAISON], maintenant: () => new Date('2026-11-04T10:00:00Z') })

  it('jeu déterministe : cinq participants, sept campagnes, trois interfaces', async () => {
    expect(CAMPAGNES_DEMO).toHaveLength(7)
    expect(new Set(CAMPAGNES_DEMO.map((c) => c.interfaceTest))).toEqual(new Set(['ISO20022', 'CARTE', 'MOBILE']))
    expect(EXECUTIONS_DEMO.length).toBeGreaterThan(300)
    const synthese = await s.synthese({ du: '2026-08-01T00:00:00Z', au: '2026-11-05T00:00:00Z' })
    expect(synthese.participantsActifs).toBe(5)
    expect(synthese.executions).toBe(EXECUTIONS_DEMO.length)
    expect(synthese.calculeLe).toBe('2026-11-04T10:00:00.000Z')
    const v = synthese.verdicts
    expect(v.favorables + v.defavorables + v.nonExecutes + v.enAttente).toBe(synthese.executions)
  })

  it('pages bornées, tri des campagnes et filtres', async () => {
    const p = await s.participants(F, { page: 0, taille: 2 })
    expect(p.suivant).toBe('1')
    expect((await s.participants(F, { page: 9, taille: 2 })).suivant).toBeNull()
    const faibles = await s.campagnes(F, 'TAUX_REUSSITE', { page: 0, taille: 100 })
    const taux = faibles.elements.map((x) => Number(x.tauxReussite.valeur))
    expect(taux).toEqual([...taux].sort((a, b) => a - b))
    const recentes = await s.campagnes({ ...F, interface: 'MOBILE' }, 'DERNIER_ESSAI', { page: 0, taille: 100 })
    expect(recentes.elements.every((x) => x.interfaceTest === 'MOBILE')).toBe(true)
    expect((await s.cas({ ...F, participant: '99002' }, { page: 0, taille: 100 })).elements.every((x) => x.interfaceTest === 'MOBILE')).toBe(true)
    expect((await s.activite(F, 'MOIS')).points.every((x) => x.periode.endsWith('-01'))).toBe(true)
    expect(await s.codesRetour(F, { page: 0, taille: 8 })).toBeNull()
  })

  it('campagne sans exécution jugée et campagne sans dernier essai daté au tri', async () => {
    const t = activiteSimulee({
      permissions: [PERMISSION_ACTIVITE],
      executions: [
        { campagne: CAMPAGNES_DEMO[0].id, cas: 'A', service: 'A', le: '2026-10-02T09:00:00Z', verdict: null },
        { campagne: CAMPAGNES_DEMO[1].id, cas: 'B', service: 'B', le: '2026-10-03T09:00:00Z', verdict: 'favorable' },
        { campagne: CAMPAGNES_DEMO[2].id, cas: 'C', service: 'C', le: '2026-10-04T09:00:00Z', verdict: 'non_execute' },
      ],
    })
    const l = await t.campagnes(F, 'TAUX_REUSSITE', { page: 0, taille: 10 })
    expect(l.elements.map((x) => x.identifiantCampagne)).toEqual([CAMPAGNES_DEMO[1].id, CAMPAGNES_DEMO[0].id, CAMPAGNES_DEMO[2].id])
    expect(l.elements[1].tauxReussite.valeur).toBe('0.0000')
  })

  it('refus au contrat : permission, critères, granularité et pages', async () => {
    const sans = activiteSimulee({ permissions: [] })
    await expect(sans.synthese(F)).rejects.toMatchObject({ status: 404, code: 'SBX_LEC_INTROUVABLE' })
    await expect(sans.liaison(LIAISONS_DEMO[0].id)).rejects.toBeInstanceOf(ApiError)
    await expect(s.liaison('inconnue')).rejects.toMatchObject({ status: 404 })
    expect((await s.liaison(LIAISONS_DEMO[1].id)).etat).toBe('GELEE')
    for (const appel of [
      () => s.synthese({ du: F.au, au: F.du }),
      () => s.synthese({ du: '2025-01-01T00:00:00Z', au: F.au }),
      () => s.synthese({ ...F, participant: '12' }),
      () => s.activite({ du: '2026-06-01T00:00:00Z', au: F.au }, 'JOUR'),
      () => s.cas(F, { page: -1, taille: 10 }),
      () => s.cas(F, { page: 0, taille: 101 }),
    ]) await expect(appel()).rejects.toMatchObject({ status: 400, code: 'SBX_LEC_CRITERE_INVALIDE' })
  })
})

describe('filtres et pas de l’axe du temps', () => {
  const maintenant = new Date('2026-11-04T10:00:00Z')

  it('lit la période, le participant et l’interface de l’adresse', () => {
    expect(lireVue(new URLSearchParams(''), maintenant)).toEqual({ periode: 30, granularite: 'JOUR', filtre: { du: '2026-10-06T00:00:00Z', au: '2026-11-05T00:00:00Z', participant: undefined, interface: undefined } })
    const v = lireVue(new URLSearchParams('periode=90&participant=99001&interface=CARTE'), maintenant)
    expect([v.granularite, v.filtre.participant, v.filtre.interface]).toEqual(['SEMAINE', '99001', 'CARTE'])
    expect(lireVue(new URLSearchParams('periode=365&participant=abc&interface=SWIFT'), maintenant)).toMatchObject({ granularite: 'MOIS', filtre: { participant: undefined, interface: undefined } })
    expect(lireVue(new URLSearchParams('periode=12'), maintenant).periode).toBe(30)
  })

  it('pas du jour, de la semaine (lundi) et du mois, zéro aux pas sans exécution', () => {
    const f = { du: '2026-10-01T00:00:00Z', au: '2026-11-01T00:00:00Z' }
    expect(pas(f, 'JOUR')).toHaveLength(31)
    expect(new Date(pas(f, 'SEMAINE')[0]).toISOString()).toBe('2026-09-28T00:00:00.000Z')
    expect(pas({ du: '2026-08-15T00:00:00Z', au: '2026-11-01T00:00:00Z' }, 'MOIS').map((t) => new Date(t).toISOString().slice(0, 10))).toEqual(['2026-08-01', '2026-09-01', '2026-10-01'])
    const v = { favorables: 1, defavorables: 0, nonExecutes: 0, enAttente: 0 }
    const s = serie([{ periode: '2026-10-02', interfaceTest: 'CARTE', executions: 3, verdicts: v }, { periode: '2026-10-02', interfaceTest: 'MOBILE', executions: 9, verdicts: v }], 'CARTE', pas(f, 'JOUR'))
    expect(s.map((p) => p.v).slice(0, 3)).toEqual([0, 3, 0])
    expect(debutPeriode('2026-10-07T10:00:00Z', 'SEMAINE')).toBe('2026-10-05')
    expect(debutPeriode('2026-10-07T10:00:00Z', 'MOIS')).toBe('2026-10-01')
    expect(pourcent('0.6666')).toBe(66.7)
    expect(jours('2026-10-01T00:00:00Z', '2026-10-01T02:00:00Z')).toBe(1)
  })

  it('chaîne de certification : sandbox, états parcourus, état courant et certification complète', () => {
    expect(etapes(null, null)).toEqual({ courant: 'SANDBOX', franchis: ['SANDBOX'] })
    expect(etapes('APTE_CERTIFICATION', ['OUVERTE', 'EN_HOMOLOGATION'])).toEqual({ courant: 'APTE_CERTIFICATION', franchis: ['SANDBOX', 'EN_HOMOLOGATION', 'APTE_CERTIFICATION'] })
    expect(etapes('GELEE', ['OUVERTE', 'EN_HOMOLOGATION']).courant).toBe('EN_HOMOLOGATION')
    expect(etapes('CERTIFIEE', null).franchis).toHaveLength(5)
  })
})

describe('formes rares servies par le service', () => {
  it('nombre entier sur l’axe, rien pour une graduation fractionnaire', async () => {
    const { entier } = await import('./Graphiques')
    expect([entier(3), entier(1.5)]).toEqual(['3', ''])
  })

  it('horloge du navigateur à défaut d’horloge injectée', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-04T10:00:00Z'))
    const s = activiteSimulee({ permissions: [PERMISSION_ACTIVITE] })
    expect((await s.synthese(F)).calculeLe).toBe('2026-11-04T10:00:00.000Z')
    vi.useRealTimers()
  })
})
