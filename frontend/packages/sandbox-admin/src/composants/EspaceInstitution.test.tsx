import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import { EspaceInstitution, actesPossibles } from './EspaceInstitution'
import { LECTEUR, rendre, simules } from '../test/rendu'
import { CORRESPONDANT_99004, ESPACES_DEMO } from '../services/simule'
import type { ServicesSandbox } from '../services/ports'

// Espace d'une institution (UC-INS-09), servi aussi par l'onglet « Sandbox » du dossier de liaison (D-118 point 2).
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-10-22T09:00:00Z'))
})
afterEach(() => vi.useRealTimers())

const page = (code: string, ouvrable = false) => <EspaceInstitution codeParticipant={code} ouvrable={ouvrable} />

describe('EspaceInstitution', () => {
  it('actes admis par l’état de l’espace', () => {
    const [ouvert, parRegistre, retire] = ESPACES_DEMO
    expect(actesPossibles(ouvert)).toEqual(['suspension'])
    expect(actesPossibles(parRegistre)).toEqual(['suspension'])
    expect(actesPossibles(retire)).toEqual(['fermeture'])
    expect(actesPossibles({ ...ouvert, etat: 'SUSPENDU', motifsSuspension: ['DECISION_GIMAC'] })).toEqual(['retablissement'])
    expect(actesPossibles({ ...ouvert, etat: 'FERME' })).toEqual([])
    expect(actesPossibles({ ...ouvert, etat: 'EN_OUVERTURE' })).toEqual([])
  })

  it('suspension puis rétablissement : confirmation, état et motif mis à jour', async () => {
    rendre(page('99001'), { chemin: '/x', route: '/x' })
    expect(await screen.findByText('ESP-99001')).toBeTruthy()
    expect(screen.getByText('01/10/2026 08:30 UTC')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Suspendre l’espace' }))
    const d = await screen.findByRole('alertdialog')
    expect(within(d).getByText(/perdent l’accès à la sandbox/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Suspendre' }))
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull())
    expect(await screen.findByText('Suspendu par décision de l’administrateur GIMAC')).toBeTruthy()
    expect(screen.getByText('22/10/2026 09:00 UTC')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Rétablir l’espace' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Rétablir' }))
    await waitFor(() => expect(screen.queryByText('Suspendu par décision de l’administrateur GIMAC')).toBeNull())
  })

  it('fermeture d’un espace suspendu d’une institution retirée : acte irréversible', async () => {
    rendre(page('99003'), { chemin: '/x', route: '/x' })
    fireEvent.click(await screen.findByRole('button', { name: 'Fermer l’espace' }))
    const d = await screen.findByRole('alertdialog')
    expect(within(d).getByText(/il ne pourra pas être rouvert/)).toBeTruthy()
    const confirmer = within(d).queryByRole('checkbox')
    if (confirmer) fireEvent.click(confirmer)
    fireEvent.click(within(d).getByRole('button', { name: 'Fermer définitivement' }))
    expect(await screen.findByText('Fermé le')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Fermer l’espace' })).toBeNull()
  })

  it('acte refusé par le service : problème traduit, dialogue ouvert', async () => {
    const s = simules()
    const services: ServicesSandbox = { espaces: { ...s.espaces, agir: async () => { throw new ApiError(409, 'SBX_ESP_REGISTRE_OPPOSE', '', 'c-1') } } }
    rendre(page('99001'), { chemin: '/x', route: '/x', services })
    fireEvent.click(await screen.findByRole('button', { name: 'Suspendre l’espace' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Suspendre' }))
    expect(await within(d).findByText('Le statut de l’institution au registre s’oppose au rétablissement')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull())
  })

  it('lecteur : fiche et motifs, aucun acte', async () => {
    rendre(page('99002'), { chemin: '/x', route: '/x', permissions: LECTEUR })
    expect(await screen.findByText(/Suspendu par le statut de l’institution au registre/)).toBeTruthy()
    expect(screen.queryByRole('button')).toBeNull()
  })

  it('espace absent dans le dossier de liaison : ouverture proposée au code de l’institution', async () => {
    const services = simules()
    rendre(page('99004', true), { chemin: '/x', route: '/x', services })
    expect(await screen.findByText('Aucun espace dans la sandbox')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir un espace' }))
    const d = await screen.findByRole('dialog')
    expect((within(d).getByLabelText(/^Code participant/) as HTMLInputElement).value).toBe('99004')
    fireEvent.change(within(d).getByLabelText(/^Correspondant/), { target: { value: CORRESPONDANT_99004 } })
    fireEvent.click(within(d).getByRole('button', { name: 'Ouvrir l’espace' }))
    expect(await screen.findByText('L’espace de l’institution 99004 est ouvert.')).toBeTruthy()
    expect(await screen.findByText('ESP-99004')).toBeTruthy()
  })

  it('espace en ouverture : statut et date inconnus ; ouverture abandonnée', async () => {
    const r = rendre(page('99001'), { chemin: '/x', route: '/x', services: simules({ espaces: [{ ...ESPACES_DEMO[0], etat: 'EN_OUVERTURE', statutRegistre: null, dateOuverture: null }] }) })
    expect(await screen.findByText('Inconnu')).toBeTruthy()
    expect(screen.getByText('Non ouvert')).toBeTruthy()
    r.unmount()
    rendre(page('99004', true), { chemin: '/x', route: '/x' })
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir un espace' }))
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Annuler' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('espace absent hors dossier ou sans droit d’administration : rien à ouvrir', async () => {
    rendre(page('99004'), { chemin: '/x', route: '/x' })
    expect(await screen.findByText('Aucun espace dans la sandbox')).toBeTruthy()
    expect(screen.queryByRole('button')).toBeNull()
  })

  it('lecture en échec (autre que 404) et refus non traduit', async () => {
    const s = simules()
    const services: ServicesSandbox = { espaces: { ...s.espaces, consulter: async () => { throw new ApiError(503, 'HTTP_503', 'indisponible', '') } } }
    rendre(page('99001'), { chemin: '/x', route: '/x', services })
    expect(await screen.findByText('L’espace n’a pas pu être lu. Réessayez dans un instant.')).toBeTruthy()
  })

  it('ouverture en attente de reprise et refus inattendu', async () => {
    const s = simules()
    let appel = 0
    const services: ServicesSandbox = {
      espaces: {
        ...s.espaces,
        ouvrir: async () => {
          appel++
          if (appel === 1) throw new Error('réseau')
          if (appel === 2) throw new ApiError(500, 'HTTP_500', 'panne interne', 'c-2')
          return { idSaga: 's1', etatSaga: 'EN_REPRISE', codeParticipant: '99004', etatEspace: 'EN_OUVERTURE', motifEchec: 'MS-IAM injoignable' }
        },
      },
    }
    rendre(page('99004', true), { chemin: '/x', route: '/x', services })
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir un espace' }))
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByLabelText(/^Correspondant/), { target: { value: CORRESPONDANT_99004 } })
    fireEvent.click(within(d).getByRole('button', { name: 'Ouvrir l’espace' }))
    expect(await within(d).findByText('La sandbox a refusé l’opération.')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Ouvrir l’espace' }))
    expect(await within(d).findByText('panne interne')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Ouvrir l’espace' }))
    expect(await screen.findByText('L’ouverture de l’espace de l’institution 99004 attend sa reprise.')).toBeTruthy()
    expect(screen.getByText('MS-IAM injoignable')).toBeTruthy()
  })
})
