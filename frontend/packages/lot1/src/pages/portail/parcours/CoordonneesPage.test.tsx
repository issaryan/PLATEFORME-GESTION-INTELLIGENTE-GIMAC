import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import CoordonneesPage, { controlerCoordonnees } from './CoordonneesPage'
import { auditer, ouvrir, sansCleNiNomInterne, services } from './outilsTest'

afterEach(cleanup)

const MOTIF = '/portail/liaisons/:id/coordonnees'
const monter = (id: string, o: Parameters<typeof ouvrir>[3] = {}) => ouvrir(<CoordonneesPage />, `/portail/liaisons/${id}/coordonnees`, MOTIF, o)
const saisir = (nom: string, valeur: string) => fireEvent.change(document.querySelector(`[name="${nom}"]`)!, { target: { value: valeur } })

describe('Portail : communication de l’adresse et du port d’essai (rang 12)', () => {
  it('contrôle du format : hôte sans schéma ni espace, port de 10 à 65535', () => {
    expect(controlerCoordonnees('', '')).toEqual({ adresse: 'pp.coordonnees.adresseRequise', port: 'pp.coordonnees.portRequis' })
    expect(controlerCoordonnees('http://hote', '8443').adresse).toBe('pp.coordonnees.adresseInvalide')
    expect(controlerCoordonnees('-hote.exemple', '8443').adresse).toBe('pp.coordonnees.adresseInvalide')
    expect(controlerCoordonnees('10.20.30.40', '9').port).toBe('pp.coordonnees.portInvalide')
    expect(controlerCoordonnees('essai.exemple', '70000').port).toBe('pp.coordonnees.portInvalide')
    expect(controlerCoordonnees('essai.participant-a.exemple', '8443')).toEqual({ adresse: undefined, port: undefined })
  })

  it('erreurs affichées avant l’envoi, focus sur le premier champ, puis confirmation et valeur relisible', async () => {
    const { container } = monter('LIA-2026-046')
    expect(await screen.findByText("Vos coordonnées d'essai")).toBeTruthy()
    expect(screen.getByLabelText(/Points de terminaison/)).toBeTruthy()
    await auditer(container)
    saisir('adresse', 'http://essai participant')
    saisir('port', '8')
    fireEvent.click(screen.getByRole('button', { name: 'Communiquer les coordonnées' }))
    expect(await screen.findByText(/Adresse invalide/)).toBeTruthy()
    expect(screen.getByText(/Port invalide/)).toBeTruthy()
    expect(document.activeElement?.getAttribute('name')).toBe('adresse')
    saisir('port', '84a43')
    expect((document.querySelector('[name="port"]') as HTMLInputElement).value).toBe('8443')
    saisir('adresse', 'essai.participant-a.exemple')
    saisir('pointsTerminaison', 'https://essai.participant-a.exemple/transactions')
    fireEvent.click(screen.getByRole('button', { name: 'Communiquer les coordonnées' }))
    const dialogue = await screen.findByRole('alertdialog')
    expect(within(dialogue).getByText('essai.participant-a.exemple')).toBeTruthy()
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Communiquer les coordonnées' }))
    expect(await screen.findByText('Coordonnées communiquées')).toBeTruthy()
    expect(screen.getByText('essai.participant-a.exemple')).toBeTruthy()
    expect(screen.getByText('8443')).toBeTruthy()
    expect(screen.getByText('https://essai.participant-a.exemple/transactions')).toBeTruthy()
    expect(await screen.findByText('Communiquer les coordonnées : effectué.')).toBeTruthy()
  })

  it('refus du service affiché dans le dialogue, avec son identifiant de corrélation', async () => {
    const communiquer = vi.fn(() => Promise.reject(new ApiError(422, 'HOM_TECHNICAL_DATA_INVALID', 'Adresse ou port invalide.', 'cid-coord')))
    monter('LIA-2026-046', { services: services(() => ({ communiquerCoordonnees: communiquer })) })
    await screen.findByText("Vos coordonnées d'essai")
    saisir('adresse', 'essai.exemple')
    saisir('port', '8443')
    fireEvent.click(screen.getByRole('button', { name: 'Communiquer les coordonnées' }))
    const dialogue = await screen.findByRole('alertdialog')
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Communiquer les coordonnées' }))
    expect(await within(dialogue).findByText(/Adresse ou port refusé par le GIMAC.*cid-coord/)).toBeTruthy()
    expect(communiquer).toHaveBeenCalledWith('LIA-2026-046', { adresse: 'essai.exemple', port: '8443', pointsTerminaison: undefined })
  })

  it('déjà communiquées : valeur relisible, sans formulaire', async () => {
    monter('LIA-2026-030')
    expect(await screen.findByText('participant-030.essai')).toBeTruthy()
    expect(screen.getByText('8443')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Communiquer les coordonnées' })).toBeNull()
    sansCleNiNomInterne()
  })

  it('pas encore attendues : état explicite', async () => {
    monter('LIA-2025-088', { services: services((b) => ({ dossier: async (id) => ({ ...(await b.portailParcours.dossier(id)), coordonneesTechniques: null }) })) })
    expect(await screen.findByText('Vos coordonnées ne sont pas encore attendues')).toBeTruthy()
  })

  it('sans HOM_PORTAL_SUBMIT : la tâche est lisible, le formulaire absent', async () => {
    monter('LIA-2026-046', { sans: ['HOM_PORTAL_SUBMIT'] })
    expect(await screen.findByText(/pas de l'accomplir/)).toBeTruthy()
    expect(document.querySelector('[name="adresse"]')).toBeNull()
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    monter('LIA-2026-046', { langue })
    await screen.findByText(langue === 'en' ? 'Your test details' : 'Sus datos de prueba')
    sansCleNiNomInterne()
  })
})
