import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import ProcesVerbalPage from './ProcesVerbalPage'
import { auditer, ouvrir, sansCleNiNomInterne, services } from './outilsTest'

afterEach(cleanup)

const MOTIF = '/portail/liaisons/:id/proces-verbal'
const monter = (id: string, o: Parameters<typeof ouvrir>[3] = {}) => ouvrir(<ProcesVerbalPage />, `/portail/liaisons/${id}/proces-verbal`, MOTIF, o)

describe('Portail : signature du procès-verbal de certification (rang 19)', () => {
  it('ce qui est signé : couverture 100 %, services couverts, empreinte, signature du GIMAC non encore apposée', async () => {
    const { container } = monter('LIA-2026-027')
    expect(await screen.findByText('Établi, à signer')).toBeTruthy()
    expect(screen.getByText('100 %')).toBeTruthy()
    expect(screen.getByText('complète')).toBeTruthy()
    for (const s of ['Retrait', 'Paiement marchand', 'Transfert']) expect(screen.getByText(s)).toBeTruthy()
    expect(screen.getByText('pas encore apposée')).toBeTruthy()
    expect(screen.getByText(/par le Conducteur de projet/)).toBeTruthy()
    sansCleNiNomInterne()
    await auditer(container)
  })

  it('SignDialog avec réauthentification FIDO2, puis état signé par le participant', async () => {
    monter('LIA-2026-027')
    fireEvent.click(await screen.findByRole('button', { name: 'Signer le procès-verbal' }))
    const dialogue = await screen.findByRole('dialog')
    expect(within(dialogue).getByText('Procès-verbal de certification de LIA-2026-027')).toBeTruthy()
    expect(within(dialogue).getByText(/couvrent 3 services sur 3/)).toBeTruthy()
    expect(within(dialogue).getByText('Le procès-verbal attendra ensuite la signature du GIMAC.')).toBeTruthy()
    fireEvent.click(within(dialogue).getByRole('button', { name: /Utiliser ma clé de sécurité/ }))
    expect(await screen.findByText('Signé par le participant', undefined, { timeout: 4000 })).toBeTruthy()
    expect(await screen.findByText('Vous avez signé. Le procès-verbal attend la signature du GIMAC.')).toBeTruthy()
  })

  it('le GIMAC a déjà signé : la signature du participant achève la certification', async () => {
    const s = services((b) => ({ procesVerbal: async (id) => ({ ...(await b.portailParcours.procesVerbal(id)), etat: 'SIGNE_GIMAC', signatureGimac: { signataire: 'CHEF_DES', fonction: 'CHEF_DES', signeLe: '2026-09-20', horodatage: '2026-09-20T10:15:00Z' } }) }))
    monter('LIA-2026-027', { services: s })
    expect(await screen.findByText(/le 20 septembre 2026, Chef du département DES/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Signer le procès-verbal' }))
    expect(await screen.findByText(/votre signature achève la certification/)).toBeTruthy()
  })

  it('refus du service affiché dans le SignDialog', async () => {
    const signer = vi.fn(() => Promise.reject(new ApiError(409, 'CER_PV_NOT_SIGNABLE', 'Non signable.', 'cid-pv')))
    monter('LIA-2026-027', { services: services(() => ({ signerProcesVerbal: signer })) })
    fireEvent.click(await screen.findByRole('button', { name: 'Signer le procès-verbal' }))
    const dialogue = await screen.findByRole('dialog')
    fireEvent.click(within(dialogue).getByRole('button', { name: /Utiliser ma clé de sécurité/ }))
    expect(await within(dialogue).findByText(/n'attend pas votre signature.*cid-pv/, undefined, { timeout: 4000 })).toBeTruthy()
    expect(signer).toHaveBeenCalledWith('LIA-2026-027', expect.any(String))
  })

  it('sans CER_PORTAL_SIGN : le procès-verbal est lisible, la signature réservée au représentant habilité', async () => {
    monter('LIA-2026-027', { sans: ['CER_PORTAL_SIGN'] })
    expect(await screen.findByText(/Seul le représentant habilité/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Signer le procès-verbal' })).toBeNull()
  })

  it('sans CER_PORTAL_READ : aucune pièce de certification', async () => {
    monter('LIA-2026-027', { sans: ['CER_PORTAL_READ'] })
    expect(await screen.findByText('Votre profil ne donne pas accès aux pièces de certification.')).toBeTruthy()
  })

  it('non établi : état explicite', async () => {
    monter('LIA-2026-030')
    expect(await screen.findByText('Le procès-verbal n\'est pas encore établi')).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    monter('LIA-2026-027', { langue })
    await screen.findByText(langue === 'en' ? 'Sign the report' : 'Firmar el acta')
    sansCleNiNomInterne()
  })
})
