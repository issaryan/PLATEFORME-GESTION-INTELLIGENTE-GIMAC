import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError, MOT_DE_PASSE_DEMO, avatarsSimules } from '@gimac/socle'
import MonComptePage from '../MonComptePage'
import { monCompteSimule } from '../../services/simule'
import { avec, rendre } from '../../test/rendu'
import { typeReel } from '../../composants/photo'

// docs/lot1/mon-compte.md §1 à §4 : photo de profil (choix, contrôles, recadrage, envoi, retrait), case
// « Fermer mes autres sessions », historique des connexions paginé ; tout geste en modale CRUD standard.

const PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0]
const fichier = (octets: number[], nom: string, type: string, taille = 2048) => {
  const corps = new Uint8Array(Math.max(taille, octets.length))
  corps.set(octets)
  return new File([corps], nom, { type })
}
// jsdom ne décode pas les images : dimensions et chargement simulés ; canevas simulé pour le recadrage.
let dims = { l: 800, h: 600 }
let lisible = true
beforeEach(() => {
  dims = { l: 800, h: 600 }
  lisible = true
  const src = Object.getOwnPropertyDescriptor(HTMLImageElement.prototype, 'src')!
  vi.spyOn(HTMLImageElement.prototype, 'src', 'set').mockImplementation(function (this: HTMLImageElement, v: string) {
    src.set!.call(this, v)
    setTimeout(() => (lisible ? this.onload : this.onerror)?.call(this, new Event('load')))
  })
  vi.spyOn(HTMLImageElement.prototype, 'naturalWidth', 'get').mockImplementation(() => dims.l)
  vi.spyOn(HTMLImageElement.prototype, 'naturalHeight', 'get').mockImplementation(() => dims.h)
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({ drawImage: () => undefined } as unknown as CanvasRenderingContext2D)
  vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation((cb, type) => cb(new Blob([new Uint8Array(PNG)], { type: type ?? 'image/png' })))
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  avatarsSimules.reinitialiser()
})
const fenetre = () => screen.getByRole('dialog')
const deposer = (f: File) => fireEvent.change(fenetre().querySelector('input[type="file"]')!, { target: { files: [f] } })

describe('Mon compte : photo de profil', () => {
  it('type réel par les octets : PNG, JPEG, WebP, autre', () => {
    expect(typeReel(new Uint8Array(PNG))).toBe('image/png')
    expect(typeReel(new Uint8Array([0xff, 0xd8, 0xff, 0xe0]))).toBe('image/jpeg')
    expect(typeReel(new TextEncoder().encode('RIFF\0\0\0\0WEBPVP8 '))).toBe('image/webp')
    expect(typeReel(new TextEncoder().encode('%PDF-1.7'))).toBeNull()
  })

  it('choix, refus expliqués avant l’envoi, recadrage, envoi, avatar affiché, puis retrait confirmé', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    const changerPhoto = vi.fn(base.changerPhoto)
    const { container } = rendre(<MonComptePage />, { service: avec({ changerPhoto }, base) })
    const carte = (await screen.findByRole('heading', { name: 'Photo de profil' })).closest('section')!
    expect(within(carte).getByRole('img', { name: 'Aïssatou Ngo Mbarga' }).textContent).toBe('AN')
    expect(within(carte).queryByRole('button', { name: 'Retirer ma photo' })).toBeNull()
    fireEvent.click(within(carte).getByRole('button', { name: 'Ajouter une photo' }))
    expect((within(fenetre()).getByRole('button', { name: 'Enregistrer la photo' }) as HTMLButtonElement).disabled).toBe(true)
    // Type réel refusé (un PDF renommé en .png), poids, dimensions, image illisible.
    deposer(fichier([...new TextEncoder().encode('%PDF-1.7')], 'photo.png', 'image/png'))
    expect(await within(fenetre()).findByText(/Format refusé/)).toBeTruthy()
    deposer(fichier(PNG, 'lourde.png', 'image/png', 2 * 1024 * 1024 + 1))
    expect(await within(fenetre()).findByText(/Image trop lourde/)).toBeTruthy()
    dims = { l: 40, h: 40 }
    deposer(fichier(PNG, 'petite.png', 'image/png'))
    expect(await within(fenetre()).findByText(/Dimensions refusées/)).toBeTruthy()
    lisible = false
    deposer(fichier(PNG, 'abimee.png', 'image/png'))
    expect(await within(fenetre()).findByText(/Image illisible/)).toBeTruthy()
    lisible = true
    dims = { l: 800, h: 600 }
    deposer(fichier(PNG, 'portrait.png', 'image/png'))
    const cadre = await within(fenetre()).findByRole('group', { name: 'Cadrer la photo de profil' })
    expect(within(fenetre()).getByText('portrait.png')).toBeTruthy()
    expect(within(fenetre()).getByText(/800 × 600 px · 2 Ko/)).toBeTruthy()
    fireEvent.keyDown(cadre, { key: '+' })
    fireEvent.keyDown(cadre, { key: 'ArrowLeft' })
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer la photo' }))
    expect(await screen.findByText('Votre photo de profil est enregistrée.')).toBeTruthy()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(changerPhoto).toHaveBeenCalledWith(expect.any(Blob))
    await waitFor(() => expect(container.querySelector('.gm-avatar-xl img')).toBeTruthy())
    expect(container.querySelector('.gm-avatar-xl img')!.getAttribute('src')).toMatch(/^data:image\/(webp|png);base64,/)
    // Retrait confirmé en modale.
    fireEvent.click(within(carte).getByRole('button', { name: 'Retirer ma photo' }))
    fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Retirer ma photo' }))
    expect(await screen.findByText('Votre photo a été retirée : vos initiales s’affichent à sa place.')).toBeTruthy()
    await waitFor(() => expect(container.querySelector('.gm-avatar-xl img')).toBeNull())
  })

  it('autre image, retrait du choix, abandon confirmé ; refus du service avec corrélation ; erreur locale', async () => {
    let refus: unknown = new ApiError(400, 'IAM_AVATAR_DIMENSIONS', 'x', 'RQ-21')
    rendre(<MonComptePage />, { service: avec({ changerPhoto: async () => { throw refus }, retirerPhoto: async () => { throw new ApiError(503, 'X', 'indisponible', 'RQ-22') } }, monCompteSimule({ latenceMs: 0, photo: true })) })
    const carte = (await screen.findByRole('heading', { name: 'Photo de profil' })).closest('section')!
    expect(within(carte).getByText(/Votre photo apparaît/)).toBeTruthy()
    fireEvent.click(within(carte).getByRole('button', { name: 'Changer ma photo' }))
    deposer(fichier(PNG, 'a.png', 'image/png'))
    await within(fenetre()).findByRole('group', { name: 'Cadrer la photo de profil' })
    deposer(fichier([0xff, 0xd8, 0xff, 0xe0], 'b.jpg', 'image/jpeg'))
    expect(await within(fenetre()).findByText('b.jpg')).toBeTruthy()
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer la photo' }))
    expect(await within(fenetre()).findByText('RQ-21')).toBeTruthy()
    expect(within(fenetre()).getByText(/Dimensions refusées/)).toBeTruthy()
    refus = new Error('canevas')
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Enregistrer la photo' }))
    expect(await within(fenetre()).findByText(/Image illisible/)).toBeTruthy()
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Retirer le fichier' }))
    expect(within(fenetre()).queryByRole('group', { name: 'Cadrer la photo de profil' })).toBeNull()
    deposer(fichier(PNG, 'c.png', 'image/png'))
    await within(fenetre()).findByRole('group', { name: 'Cadrer la photo de profil' })
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Annuler' }))
    expect(within(fenetre()).getByText('Abandonner la saisie ?')).toBeTruthy()
    fireEvent.click(within(fenetre()).getByRole('button', { name: 'Abandonner les modifications' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    fireEvent.click(within(carte).getByRole('button', { name: 'Retirer ma photo' }))
    fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Retirer ma photo' }))
    expect(await screen.findByText('RQ-22')).toBeTruthy()
  })
})

describe('Mon compte : mot de passe et historique', () => {
  it('case « Fermer mes autres sessions » transmise, annonce adaptée, appareils relus', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    const changer = vi.fn(base.changerMotDePasse)
    rendre(<MonComptePage />, { service: avec({ changerMotDePasse: changer }, base) })
    const appareils = (await screen.findByRole('heading', { name: 'Appareils connectés' })).closest('section')!
    expect(await within(appareils).findByText('Firefox 131')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Changer mon mot de passe' }))
    const d = fenetre()
    fireEvent.change(d.querySelector('[name="motDePasseActuel"]')!, { target: { value: MOT_DE_PASSE_DEMO } })
    fireEvent.change(d.querySelector('[name="nouveauMotDePasse"]')!, { target: { value: 'Nouveau-Mdp-2026!' } })
    fireEvent.change(d.querySelector('[name="confirmationMotDePasse"]')!, { target: { value: 'Nouveau-Mdp-2026!' } })
    fireEvent.click(within(d).getByLabelText('Fermer mes autres sessions (tous les appareils sauf celui-ci)'))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le mot de passe' }))
    expect(await screen.findByText('Votre mot de passe a été changé et vos autres sessions ont été fermées.')).toBeTruthy()
    expect(changer).toHaveBeenCalledWith({ actuel: MOT_DE_PASSE_DEMO, nouveau: 'Nouveau-Mdp-2026!', fermerAutresSessions: true })
    await waitFor(() => expect(within(appareils).queryByText('Firefox 131')).toBeNull())
  })

  it('historique paginé : résultats en libellés, page suivante, taille de page, vide, erreur', async () => {
    const base = monCompteSimule({ latenceMs: 0 })
    const historique = vi.fn(base.historique)
    rendre(<MonComptePage />, { service: avec({ historique }, base) })
    const carte = (await screen.findByRole('heading', { name: 'Historique des connexions' })).closest('section')!
    expect(await within(carte).findAllByText('Réussie')).not.toHaveLength(0)
    expect(within(carte).getAllByText('Mot de passe incorrect').length).toBeGreaterThan(0)
    expect(within(carte).getByText(/64/)).toBeTruthy()
    expect(historique).toHaveBeenLastCalledWith(0, 20)
    fireEvent.click(within(carte).getByRole('button', { name: /Suivante/ }))
    await waitFor(() => expect(historique).toHaveBeenLastCalledWith(1, 20))
    expect(await within(carte).findByText('Bloquée (trop de tentatives)')).toBeTruthy()
    fireEvent.change(within(carte).getByRole('combobox'), { target: { value: '50' } })
    await waitFor(() => expect(historique).toHaveBeenLastCalledWith(0, 50))
    cleanup()
    rendre(<MonComptePage />, { service: avec({ historique: async () => ({ contenu: [], total: 0 }) }) })
    expect(await screen.findByText('Aucune connexion enregistrée')).toBeTruthy()
    cleanup()
    rendre(<MonComptePage />, {
      service: avec({
        historique: async () => { throw new ApiError(503, 'CNX_HISTORIQUE_INDISPONIBLE', 'x', 'RQ-30') },
        appareils: async () => [{ id: 'a', courante: true, appareil: { navigateur: null, systeme: null, type: 'INCONNU' }, adresseIp: null, couche: 'console', ouverteLe: '2026-09-30T08:00:00Z', derniereActivite: null, expireA: null }],
      }),
    })
    expect(await screen.findByText('RQ-30')).toBeTruthy()
    expect(screen.getByText(/historique des connexions est momentanément indisponible/)).toBeTruthy()
    expect(screen.getAllByText('Appareil inconnu').length).toBeGreaterThan(0)
    expect(screen.getByText('Console d’administration')).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('photo, appareils et historique en %s, sans clé brute', async (langue) => {
    const { container } = rendre(<MonComptePage />, { langue })
    await screen.findAllByText(langue === 'en' ? 'This device' : 'Este dispositivo')
    expect(container.textContent).not.toMatch(/\b(photo|appareils|historique)\.[a-zA-Z]/)
  })
})
