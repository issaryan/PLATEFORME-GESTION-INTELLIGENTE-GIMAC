import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@gimac/socle'
import { servicesSandboxHttp, supervisionHttp } from './http'
import { DISPONIBILITES_DEMO, ELEMENTS_DEMO, SANTE_DEMO, supervisionSimulee } from './supervisionSimulee'
import { PERMISSION_SUPERVISION } from '../modele'

const json = (corps: unknown) => new Response(JSON.stringify(corps), { status: 200, headers: { 'Content-Type': 'application/json' } })
afterEach(() => vi.restoreAllMocks())

describe('supervision SBX-TRACE par la passerelle', () => {
  it('appelle uniquement les quatre lectures DES avec leur pagination', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => json({}))
    await supervisionHttp.etatSante()
    await supervisionHttp.elements({ page: 1, taille: 10 })
    await supervisionHttp.alertes({ page: 2, taille: 20 })
    await supervisionHttp.disponibilites({ page: 0, taille: 10 })
    expect(spy.mock.calls.map(([u]) => {
      const url = new URL(String(u))
      return `${url.pathname}${url.search}`
    })).toEqual([
      '/bff/des/sbx-trace/etat-sante/detail',
      '/bff/des/sbx-trace/elements-supervises?page=1&taille=10',
      '/bff/des/sbx-trace/alertes?page=2&taille=20',
      '/bff/des/sbx-trace/disponibilites?page=0&taille=10',
    ])
    expect(servicesSandboxHttp().supervision).toBe(supervisionHttp)
  })
})

describe('supervision SBX-TRACE simulée', () => {
  const service = (o: Partial<Parameters<typeof supervisionSimulee>[0]> = {}) => supervisionSimulee({ permissions: [PERMISSION_SUPERVISION], ...o })

  it('restitue la santé et trois pages numérotées, avec copies isolées', async () => {
    const s = service({ latenceMs: 1 })
    expect(await s.etatSante()).toEqual(SANTE_DEMO)
    expect((await s.etatSante())).not.toBe(SANTE_DEMO)
    const page = await s.elements({ page: 2, taille: 10 })
    expect(page).toMatchObject({ total: 23, page: 2, taille: 10, suivant: null })
    expect(page.elements).toHaveLength(3)
    expect((await s.alertes({ page: 0, taille: 1 })).total).toBe(2)
    expect((await s.disponibilites({ page: 1, taille: 1 })).elements[0]).toEqual(DISPONIBILITES_DEMO[1])
    page.elements[0].nature = 'MODIFIE'
    expect(ELEMENTS_DEMO[20].nature).not.toBe('MODIFIE')
  })

  it('exige la permission dédiée, un compte GIMAC et une pagination valide', async () => {
    const sansDroit = service({ permissions: [] })
    await expect(sansDroit.etatSante()).rejects.toMatchObject({ status: 404, code: 'SBX_TRA_PERMISSION_ABSENTE' })
    await expect(service({ institution: '99001' }).elements({ page: 0, taille: 10 })).rejects.toBeInstanceOf(ApiError)
    await expect(service().alertes({ page: -1, taille: 10 })).rejects.toMatchObject({ status: 422, code: 'SBX_TRA_CRITERE_INVALIDE' })
    await expect(service().disponibilites({ page: 0, taille: 101 })).rejects.toMatchObject({ status: 422 })
  })
})
