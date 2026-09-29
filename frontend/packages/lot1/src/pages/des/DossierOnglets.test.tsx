import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'
import type { Dossier } from '../../modele/certification'
import DossierMiseEnServicePage from './DossierMiseEnServicePage'

// DES-11 : onglets du dossier de mise en service (fiche de changement, procès-verbal, horodatages,
// attestation et vérification de son empreinte, registre et export contrôlé) et actions de la fiche.
beforeEach(() => {
  Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() })
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const ouvrir = (onglet: string, services?: Services) =>
  rendrePage(<DossierMiseEnServicePage />, { chemin: `/des/liaisons/LIA-2025-102/mise-en-service?onglet=${onglet}`, motif: '/des/liaisons/:id/mise-en-service', services })
/** Services simulés dont le dossier et la certification sont ajustés pour le scénario. */
function avec(dossier: Partial<Dossier>, certification: Partial<Services['certification']> = {}, reauth?: Services['reauth']): Services {
  const s = servicesSimules({ latenceMs: 0 })
  const lire = async () => ({ ...(await s.certification.dossierParLiaison('LIA-2025-102'))!, ...dossier })
  return { ...s, reauth: reauth ?? s.reauth, certification: { ...s.certification, dossierParLiaison: lire, ...certification } }
}

describe('DES-11 : onglets du dossier', () => {
  it('fiche de changement lue, à relire, en lecture seule', async () => {
    ouvrir('fiche')
    expect(await screen.findByText('CHG-2026-0412', { selector: 'dd' })).toBeTruthy()
    expect(screen.getByText(/sera relue avant tout usage/)).toBeTruthy()
    expect(screen.getByText(/n'écrit jamais dans le dispositif/)).toBeTruthy()
  })
  it('aucune fiche : liaison par sa référence, refus traduit', async () => {
    const lier = vi.fn(async () => { throw new ApiError(409, 'CER_FICHE_INCONNUE', 'Fiche inconnue.', 'cid-fiche') })
    ouvrir('fiche', avec({ actionsPermises: ['LIER_FICHE'] }, { ficheDeChangement: async () => null, lierFicheDeChangement: lier }))
    expect(await screen.findByText('Aucune fiche de changement liée')).toBeTruthy()
    fireEvent.change(screen.getByLabelText(/Référence de la fiche/), { target: { value: ' CHG-2026-0500 ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lier la fiche de changement' }))
    await waitFor(() => expect(lier).toHaveBeenCalledWith('DMS-2026-004', 'CHG-2026-0500'))
    expect(await screen.findByText('Fiche inconnue.')).toBeTruthy()
  })
  it('procès-verbal : attente sans droit de dépôt, puis dépôt avec l’heure de bascule', async () => {
    ouvrir('procesVerbal')
    expect(await screen.findByText('Le procès-verbal sera déposé après la bascule en production.')).toBeTruthy()
    cleanup()
    const deposer = vi.fn(async () => undefined)
    const { container } = ouvrir('procesVerbal', avec({ actionsPermises: ['DEPOSER_PROCES_VERBAL'] }, { deposerProcesVerbal: deposer }))
    expect(await screen.findByText('Déposez le procès-verbal de bascule avec son heure réelle.')).toBeTruthy()
    fireEvent.change(container.querySelector('input[type="file"]')!, { target: { files: [new File(['pv'], 'pv.pdf', { type: 'application/pdf' })] } })
    fireEvent.change(screen.getByLabelText(/Heure de bascule/), { target: { value: '05/10/2026' } })
    fireEvent.change(screen.getByLabelText('Heure'), { target: { value: '23:10' } })
    fireEvent.click(screen.getByRole('button', { name: 'Déposer le procès-verbal' }))
    await waitFor(() => expect(deposer).toHaveBeenCalledWith('DMS-2026-004', expect.any(File), '05/10/2026 23:10'))
  })
  it('procès-verbal déposé : empreinte et fenêtre', async () => {
    ouvrir('procesVerbal', avec({}, { procesVerbal: async () => ({ empreinte: 'a'.repeat(64), heureBascule: '05/10/2026 23:10', horsFenetre: true }) }))
    expect(await screen.findByText('hors fenêtre')).toBeTruthy()
    expect(screen.getByText('05/10/2026 23:10')).toBeTruthy()
  })
  it('horodatages vérifiés, ou aucun', async () => {
    ouvrir('horodatages')
    expect((await screen.findAllByText('vérifié')).length).toBe(2)
    cleanup()
    ouvrir('horodatages', avec({}, { horodatages: async () => [] }))
    expect(await screen.findByText('Aucun jeton d\'horodatage')).toBeTruthy()
  })
  it('attestation : aucune tant que le dossier n’est pas complet', async () => {
    ouvrir('attestation')
    expect(await screen.findByText('Aucune attestation émise')).toBeTruthy()
  })
  it('attestation émise : téléchargement et empreinte vérifiée, divergente ou indisponible', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const empreinte = (await s.certification.lireAttestation('ATT-2026-0007')).empreinteDocument
    const reponses = [empreinte, 'f'.repeat(64), null]
    const services = avec({ numeroAttestation: 'ATT-2026-0007' }, { telechargerAttestation: async () => ({ blob: new Blob(['%PDF']), empreinte: reponses.shift() ?? null }) })
    ouvrir('attestation', services)
    const bouton = await screen.findByRole('button', { name: 'Télécharger l\'attestation' })
    fireEvent.click(bouton)
    await waitFor(() => expect(URL.createObjectURL).toHaveBeenCalled())
    fireEvent.click(bouton)
    expect(await screen.findByText('Empreinte divergente')).toBeTruthy()
    fireEvent.click(bouton)
    expect(await screen.findByText(/vérification impossible/)).toBeTruthy()
  })
  it('attestation : échec du téléchargement signalé', async () => {
    ouvrir('attestation', avec({ numeroAttestation: 'ATT-2026-0007' }, { telechargerAttestation: async () => { throw new ApiError(503, 'X', 'Service des attestations indisponible.', 'c') } }))
    fireEvent.click(await screen.findByRole('button', { name: 'Télécharger l\'attestation' }))
    expect(await screen.findByText('Service des attestations indisponible.')).toBeTruthy()
  })
  it('demande de statut : approbation signée par un autre utilisateur ; quatre yeux pour le demandeur', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const a = await s.certification.lireAttestation('ATT-2026-0007')
    const approuver = vi.fn(async () => undefined)
    const demande = { id: 'DS-1', typeDemande: 'SUSPENSION' as const, motif: 'Risque', empreinte: 'e'.repeat(64), demandeeParMoi: false }
    const services = avec({ numeroAttestation: 'ATT-2026-0007' }, { approuverStatut: approuver, lireAttestation: async () => ({ ...a, demandeEnAttente: demande, actionsPermises: ['APPROUVER_STATUT'] }) },
      { assertion: async () => 'assertion' })
    ouvrir('attestation', services)
    fireEvent.click(await screen.findByRole('button', { name: 'Approuver la demande de statut' }))
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByRole('textbox'), { target: { value: 'Suspension justifiée par le rapport de risque.' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    await waitFor(() => expect(approuver).toHaveBeenCalledWith('DS-1', 'Suspension justifiée par le rapport de risque.', 'assertion'))
    cleanup()
    ouvrir('attestation', avec({ numeroAttestation: 'ATT-2026-0007' }, { lireAttestation: async () => ({ ...a, demandeEnAttente: { ...demande, demandeeParMoi: true } }) }))
    expect(await screen.findByText(/son approbation revient à un autre utilisateur/)).toBeTruthy()
  })
  it('registre : attestations et export contrôlé', async () => {
    ouvrir('registre')
    expect(await screen.findByText('ATT-2025-0031')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Exporter le registre (contrôlé)' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Exporter le registre (contrôlé)' }))
    await waitFor(() => expect(URL.createObjectURL).toHaveBeenCalled())
  })
  it('actions de la fiche : vérifier, relire la fiche, émettre (refus du service expliqué)', async () => {
    const verifier = vi.fn(async () => undefined)
    const lier = vi.fn(async () => undefined)
    ouvrir('detail', avec({ actionsPermises: ['VERIFIER', 'RELIRE_FICHE', 'EMETTRE_ATTESTATION'] }, { verifierDossier: verifier, lierFicheDeChangement: lier }))
    fireEvent.click(await screen.findByRole('button', { name: 'Vérifier la complétude' }))
    await waitFor(() => expect(verifier).toHaveBeenCalledWith('DMS-2026-004'))
    fireEvent.click(screen.getByRole('button', { name: 'Relire la fiche' }))
    await waitFor(() => expect(lier).toHaveBeenCalledWith('DMS-2026-004', 'CHG-2026-0412'))
    fireEvent.click(screen.getByRole('button', { name: 'Émettre l\'attestation de certification' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Émettre l\'attestation de certification' }))
    await waitFor(() => expect(d.textContent).toContain("l'attestation ne peut pas encore être émise"))
  })
})
