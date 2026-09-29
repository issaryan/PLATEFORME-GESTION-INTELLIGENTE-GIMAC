import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import type { ApprobationSecurite } from '../../../modele/parcours'
import AttestationsPage from './AttestationsPage'
import { auditer, ouvrir, sansCleNiNomInterne, services } from './outilsTest'

afterEach(cleanup)

const MOTIF = '/portail/liaisons/:id/attestations'
const monter = (o: Parameters<typeof ouvrir>[3] = {}) => ouvrir(<AttestationsPage />, '/portail/liaisons/LIA-2026-030/attestations', MOTIF, o)

/** Scénario : procès-verbal signé, ISO 27001 vérifiée, GSMA rejetée par la sécurité, COBAC (fintech) à fournir. */
const APPROBATION: ApprobationSecurite = {
  liaisonId: 'LIA-2026-030', etat: 'EN_VERIFICATION', dossierChangement: null, ficheChangement: null, documentParametrageRecu: null, fintech: true, visas: [], approuveeLe: null,
  attestations: [
    { type: 'ISO_27001', requise: true, preuve: { documentRef: 'ATT-1', nom: 'iso-27001.pdf', empreinte: 'ab'.repeat(32), deposeLe: '2026-09-20', deposePar: 'Correspondant' }, verifieeLe: '2026-09-22', verifieePar: 'RSSI', archiveeLe: null, motifRejet: null },
    { type: 'GSMA', requise: true, preuve: null, verifieeLe: null, verifieePar: null, archiveeLe: null, motifRejet: 'Attestation expirée depuis le 30 juin 2026' },
    { type: 'COBAC', requise: true, preuve: null, verifieeLe: null, verifieePar: null, archiveeLe: null, motifRejet: null },
  ],
  transitionsPermises: [], actionsPermises: ['DEPOSER_ATTESTATION'],
}
const scenario = (deposer = vi.fn(() => Promise.resolve())) => services(() => ({ approbation: () => Promise.resolve(APPROBATION), deposerAttestation: deposer }))
const fichier = (i: number, nom: string) => fireEvent.change(document.querySelectorAll('input[type="file"]')[i], { target: { files: [new File(['x'], nom)] } })

describe('Portail : attestations de sécurité (rangs 21 et 22)', () => {
  it('exigées selon l’activité, vérifiée, rejetée avec le motif du RSSI, à fournir', async () => {
    const { container } = monter({ services: scenario() })
    expect(await screen.findByText('Exigées pour votre activité : ISO 27001, GSMA, Conformité COBAC')).toBeTruthy()
    expect(screen.getByText(/En tant que fintech/)).toBeTruthy()
    expect(screen.getByText('vérifiée')).toBeTruthy()
    expect(screen.getByText(/le 22 septembre 2026 par la Sécurité des systèmes d'information/)).toBeTruthy()
    expect(screen.getByText('Attestation rejetée par la Sécurité des systèmes d\'information')).toBeTruthy()
    expect(screen.getByText('Attestation expirée depuis le 30 juin 2026')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Déposer une nouvelle attestation' })).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Déposer l\'attestation' })).toBeTruthy()
    sansCleNiNomInterne()
    await auditer(container)
  })

  it('PDF seulement, contrôlé avant l’envoi ; dépôt confirmé par le hook du portail', async () => {
    const deposer = vi.fn(() => Promise.resolve())
    monter({ services: scenario(deposer) })
    await screen.findByText('Attestation expirée depuis le 30 juin 2026')
    fireEvent.click(screen.getByRole('button', { name: 'Déposer une nouvelle attestation' }))
    expect(await screen.findByText('Choisissez l\'attestation à déposer.')).toBeTruthy()
    fichier(0, 'gsma.docx')
    expect(await screen.findByText('L\'attestation doit être un fichier PDF.')).toBeTruthy()
    fichier(0, 'gsma-2026.pdf')
    fireEvent.click(screen.getByRole('button', { name: 'Déposer une nouvelle attestation' }))
    const dialogue = await screen.findByRole('alertdialog')
    expect(within(dialogue).getByText('gsma-2026.pdf')).toBeTruthy()
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Déposer l\'attestation' }))
    await vi.waitFor(() => expect(deposer).toHaveBeenCalledWith('LIA-2026-030', 'GSMA', expect.any(File)))
  })

  it('avant la signature du procès-verbal des deux parties : attendues plus tard, sans dépôt', async () => {
    monter()
    expect(await screen.findByText('Les attestations sont attendues une fois le procès-verbal de certification signé des deux parties.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Déposer l\'attestation' })).toBeNull()
  })

  it('sans HOM_PORTAL_SUBMIT : état lisible, dépôt réservé', async () => {
    monter({ services: scenario(), sans: ['HOM_PORTAL_SUBMIT'] })
    expect((await screen.findAllByText(/pas de l'accomplir/)).length).toBe(2)
    expect(document.querySelector('input[type="file"]')).toBeNull()
  })

  it('sans CER_PORTAL_READ : aucune pièce de certification', async () => {
    monter({ sans: ['CER_PORTAL_READ'] })
    expect(await screen.findByText('Votre profil ne donne pas accès aux pièces de certification.')).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    monter({ services: scenario(), langue })
    await screen.findByText(langue === 'en' ? 'Upload a new certificate' : 'Depositar un nuevo certificado')
    sansCleNiNomInterne()
  })
})
