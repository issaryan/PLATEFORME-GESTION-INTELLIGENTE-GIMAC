import { afterEach, describe, expect, it, vi } from 'vitest'
import { organisationHttp } from './organisation'

// Organisation (D-76, D-102) : unités, rattachement motivé, affectations, retrait et organigramme en PDF, par la passerelle.
const json = (corps: unknown) => new Response(JSON.stringify(corps), { status: 200, headers: { 'Content-Type': 'application/json' } })
afterEach(() => vi.restoreAllMocks())

describe('organisation (MS-IAM)', () => {
  it('motif des gestes d’unité, affectations, retrait et PDF sur les routes du contrat', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (u) =>
      String(u).includes('.pdf') ? new Response('%PDF-1.7', { status: 200, headers: { 'Content-Type': 'application/pdf' } }) : json([]))
    const o = organisationHttp('console')
    const unite = { code: 'SNA', libelle: 'Service', type: 'SERVICE' as const, parent: 'DES', active: true }
    await o.creerUnite(unite, 'Création')
    await o.creerUnite(unite)
    await o.modifierUnite('SNA', unite, 'Déplacement')
    await o.modifierUnite('SNA', unite)
    await o.rattacher('u 1', { unite: 'SNA', responsableId: null, motif: 'Renfort' })
    await o.affecter('SNA', { comptes: ['u-1', 'u-2'], motif: 'Renfort du service' })
    await o.retirer('SNA', 'u-1', 'Départ du service')
    const pdf = await o.organigrammePdf({ racine: 'DES', format: 'A4', orientation: 'portrait', membres: true })
    await organisationHttp('des').organigrammePdf({ format: 'A3', orientation: 'paysage', membres: false })
    expect(await pdf.text()).toBe('%PDF-1.7')
    const vus = spy.mock.calls.map(([u, i]) => `${(i as RequestInit).method} ${new URL(String(u)).pathname}${new URL(String(u)).search}`)
    expect(vus).toEqual([
      'POST /bff/console/iam/organisation/unites',
      'POST /bff/console/iam/organisation/unites',
      'PUT /bff/console/iam/organisation/unites/SNA',
      'PUT /bff/console/iam/organisation/unites/SNA',
      'PUT /bff/console/iam/comptes/u%201/rattachement',
      'POST /bff/console/iam/organisation/unites/SNA/affectations',
      'DELETE /bff/console/iam/organisation/unites/SNA/affectations/u-1?motif=D%C3%A9part%20du%20service',
      'GET /bff/console/iam/organisation/organigramme.pdf?format=A4&orientation=portrait&membres=true&racine=DES',
      'GET /bff/des/iam/organisation/organigramme.pdf?format=A3&orientation=paysage&membres=false',
    ])
    const corps = (i: number) => JSON.parse(String((spy.mock.calls[i][1] as RequestInit).body))
    expect(corps(0).motif).toBe('Création')
    expect(corps(1).motif).toBeUndefined()
    expect(corps(2).motif).toBe('Déplacement')
    expect(corps(4)).toEqual({ unite: 'SNA', responsableId: null, motif: 'Renfort' })
    expect(corps(5)).toEqual({ comptes: ['u-1', 'u-2'], motif: 'Renfort du service' })
    expect(new Headers((spy.mock.calls[7][1] as RequestInit).headers).get('Accept')).toBe('application/pdf')
  })
})
