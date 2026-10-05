import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, screen } from '@testing-library/react'
import MonComptePage from '../MonComptePage'
import { monCompteSimule } from '../../services/simule'
import { monCompteHttp } from '../../services/http'
import { rendre } from '../../test/rendu'

// Rattachement organisationnel en lecture dans « Mon compte » (D-76) : unité et responsable fixés par
// l'administration, libellés lus sur GET /bff/des/iam/organisation/moi, codes à défaut.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const rattachement = {
  id: 'u-demo', nomAffiche: 'Aïssatou Ngo Mbarga', unite: { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE', parent: 'PROD_INFO', active: true },
  chemin: [{ code: 'PROD_INFO', libelle: 'Service Production informatique', type: 'SERVICE', parent: 'DES', active: true }, { code: 'POLE_RESEAU', libelle: 'Pôle réseau', type: 'POLE', parent: 'PROD_INFO', active: true }],
  responsable: { id: 'u-chef', nomAffiche: 'Gaston Ndzana Ebode', fonction: 'Chef du service Production informatique' }, chaine: ['u-chef'], equipeDirecte: 2,
}

describe('Mon compte : rattachement', () => {
  it('unité (chemin) et responsable avec sa fonction, équipe directe', async () => {
    rendre(<MonComptePage />, { service: monCompteSimule({ latenceMs: 0, compte: { unite: 'POLE_RESEAU', responsableId: 'u-chef' }, rattachement }) })
    expect(await screen.findByRole('heading', { name: 'Mon rattachement' })).toBeTruthy()
    expect(await screen.findByText('Service Production informatique › Pôle réseau')).toBeTruthy()
    expect(screen.getByText('Gaston Ndzana Ebode, Chef du service Production informatique')).toBeTruthy()
    expect(screen.getByText('2 collaborateurs')).toBeTruthy()
  })
  it('sans service d’organisation (portail, console) : codes bruts ; sans rattachement : aucun bloc', async () => {
    rendre(<MonComptePage />, { service: monCompteSimule({ latenceMs: 0, compte: { unite: null, responsableId: 'u-chef' } }) })
    expect(await screen.findByRole('heading', { name: 'Mon rattachement' })).toBeTruthy()
    expect(screen.getByText('aucune unité')).toBeTruthy()
    expect(screen.getByText('u-chef')).toBeTruthy()
    cleanup()
    rendre(<MonComptePage />, { service: monCompteSimule({ latenceMs: 0 }) })
    expect(await screen.findByRole('heading', { level: 1, name: 'Mon compte' })).toBeTruthy()
    await screen.findByText('demo')
    expect(screen.queryByRole('heading', { name: 'Mon rattachement' })).toBeNull()
  })
  it('HTTP : organisation/moi pour la couche interne seulement', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
    await monCompteHttp('des').organisation!()
    expect(new URL(String(spy.mock.calls[0][0])).pathname).toBe('/bff/des/iam/organisation/moi')
    expect(monCompteHttp('portail').organisation).toBeUndefined()
    expect(monCompteHttp('console').organisation).toBeUndefined()
  })
})
