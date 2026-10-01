// Photo de profil (docs/lot1/mon-compte.md §1, §4) : Avatar avec photo et repli sur les initiales, recadrage
// carré (AvatarCropper), menu utilisateur de la barre supérieure, avatars de la messagerie et des destinataires.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { useState } from 'react'
import {
  Avatar, AvatarCropper, ConversationList, LangueProvider, MessageThread, MultiSelect, RECADRAGE_INITIAL, Tag, TopBar, initiales, recadrerImage, zoneRecadree,
  type Recadrage,
} from '../src'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('Avatar', () => {
  it('initiales sans photo, nom accessible, tailles', () => {
    const { container } = render(<><Avatar name="Aïssatou Ngo Mbarga" /><Avatar name="Paul" size="xl" /><Avatar name="" size="xs" /></>)
    expect(screen.getByRole('img', { name: 'Aïssatou Ngo Mbarga' }).textContent).toBe('AN')
    expect(container.querySelector('.gm-avatar-xl')!.textContent).toBe('P')
    expect(container.querySelector('.gm-avatar-xs')!.textContent).toBe('')
    expect(initiales('  jean   baptiste  ella ')).toBe('JB')
  })
  it('photo affichée ; en cas d’échec, retour aux initiales ; nouvelle adresse retentée ; décoratif masqué', () => {
    const { container, rerender } = render(<Avatar name="Hervé Kamga" src="/bff/des/iam/avatars/u-1?v=a" />)
    const img = container.querySelector('img')!
    expect(img.getAttribute('src')).toBe('/bff/des/iam/avatars/u-1?v=a')
    expect(img.getAttribute('alt')).toBe('')
    expect(container.querySelector('.gm-avatar-photo')).toBeTruthy()
    fireEvent.error(img)
    expect(container.querySelector('img')).toBeNull()
    expect(container.textContent).toBe('HK')
    rerender(<Avatar name="Hervé Kamga" src="/bff/des/iam/avatars/u-1?v=b" />)
    expect(container.querySelector('img')!.getAttribute('src')).toContain('v=b')
    rerender(<Avatar name="Hervé Kamga" decorative />)
    expect(container.querySelector('.gm-avatar')!.getAttribute('aria-hidden')).toBe('true')
    expect(screen.queryByRole('img')).toBeNull()
  })
})

describe('AvatarCropper', () => {
  it('zone retenue : carré centré, zoom et positions bornés', () => {
    expect(zoneRecadree(400, 200, RECADRAGE_INITIAL)).toEqual({ sx: 100, sy: 0, cote: 200 })
    expect(zoneRecadree(400, 200, { zoom: 2, x: -1, y: 1 })).toEqual({ sx: 0, sy: 100, cote: 100 })
    expect(zoneRecadree(200, 400, { zoom: 9, x: 5, y: -5 })).toEqual({ sx: 150, sy: 0, cote: 50 })
  })

  function Banc({ initial = RECADRAGE_INITIAL, suivi }: { initial?: Recadrage; suivi: (r: Recadrage) => void }) {
    const [r, setR] = useState(initial)
    return <AvatarCropper src="data:image/png;base64,AAAA" label="Recadrer la photo" value={r} onChange={(v) => { setR(v); suivi(v) }} name="Paul Mbarga" />
  }
  const charger = (container: HTMLElement, l = 400, h = 200) => {
    const img = container.querySelector('.gm-crop-cadre img') as HTMLImageElement
    Object.defineProperty(img, 'naturalWidth', { value: l })
    Object.defineProperty(img, 'naturalHeight', { value: h })
    fireEvent.load(img)
    return img
  }

  it('clavier : flèches, Maj, zoom + et −, 0 recentre ; touches sans effet ignorées', () => {
    const suivi = vi.fn()
    const { container } = render(<Banc suivi={suivi} />)
    charger(container)
    const cadre = screen.getByRole('group', { name: 'Recadrer la photo' })
    expect(cadre.getAttribute('aria-describedby')).toBeTruthy()
    fireEvent.keyDown(cadre, { key: 'ArrowRight' })
    expect(suivi).toHaveBeenLastCalledWith({ zoom: 1, x: 0.05, y: 0 })
    fireEvent.keyDown(cadre, { key: 'ArrowLeft', shiftKey: true })
    expect(suivi).toHaveBeenLastCalledWith({ zoom: 1, x: -0.15, y: 0 })
    fireEvent.keyDown(cadre, { key: 'ArrowDown' })
    fireEvent.keyDown(cadre, { key: 'ArrowUp' })
    fireEvent.keyDown(cadre, { key: '+' })
    fireEvent.keyDown(cadre, { key: '=' })
    expect(suivi).toHaveBeenLastCalledWith({ zoom: 1.2, x: -0.15, y: 0 })
    fireEvent.keyDown(cadre, { key: '-' })
    expect(suivi).toHaveBeenLastCalledWith({ zoom: 1.1, x: -0.15, y: 0 })
    const n = suivi.mock.calls.length
    fireEvent.keyDown(cadre, { key: 'a' })
    expect(suivi).toHaveBeenCalledTimes(n)
    fireEvent.keyDown(cadre, { key: '0' })
    expect(suivi).toHaveBeenLastCalledWith(RECADRAGE_INITIAL)
    // Image placée : 400 × 200 dans un cadre de 240 px, carré centré.
    expect((container.querySelector('.gm-crop-cadre img') as HTMLImageElement).style.left).toBe('-120px')
    expect(container.querySelectorAll('.gm-crop-mini')).toHaveLength(3)
  })

  it('glissé au pointeur : l’image suit le pointeur ; curseur de zoom ; recentrer', () => {
    const suivi = vi.fn()
    const { container } = render(<Banc suivi={suivi} />)
    const cadre = screen.getByTestId('gm-crop-cadre')
    // Avant chargement : aucun débattement, position inchangée.
    fireEvent.pointerDown(cadre, { clientX: 100, clientY: 100, pointerId: 1 })
    fireEvent.pointerMove(cadre, { clientX: 140, clientY: 100, pointerId: 1 })
    expect(suivi).toHaveBeenLastCalledWith(RECADRAGE_INITIAL)
    fireEvent.pointerUp(cadre)
    charger(container)
    fireEvent.pointerMove(cadre, { clientX: 10, clientY: 10 })
    fireEvent.pointerDown(cadre, { clientX: 100, clientY: 100, pointerId: 1 })
    fireEvent.pointerMove(cadre, { clientX: 160, clientY: 100, pointerId: 1 })
    expect(suivi).toHaveBeenLastCalledWith({ zoom: 1, x: -0.5, y: 0 })
    fireEvent.pointerCancel(cadre)
    const zoom = screen.getByLabelText('Zoom')
    fireEvent.change(zoom, { target: { value: '2' } })
    expect(suivi).toHaveBeenLastCalledWith({ zoom: 2, x: -0.5, y: 0 })
    expect(zoom.getAttribute('aria-valuetext')).toBe('200 %')
    fireEvent.pointerDown(cadre, { clientX: 100, clientY: 100, pointerId: 2 })
    fireEvent.pointerMove(cadre, { clientX: 100, clientY: 160, pointerId: 2 })
    expect(suivi.mock.calls.at(-1)![0].y).toBeLessThan(0)
    fireEvent.pointerUp(cadre)
    fireEvent.click(screen.getByRole('button', { name: 'Recentrer' }))
    expect(suivi).toHaveBeenLastCalledWith(RECADRAGE_INITIAL)
  })

  it('libellés en anglais', () => {
    render(<LangueProvider langue="en"><AvatarCropper src="data:," label="Crop" value={RECADRAGE_INITIAL} onChange={() => undefined} size={160} /></LangueProvider>)
    expect(screen.getByText('Preview', { selector: 'span' })).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Recentre' })).toBeTruthy()
  })

  it('recadrerImage : zone dessinée sur un canevas carré, WebP sinon PNG ; canevas indisponible : erreur', async () => {
    // jsdom ne charge pas les images : le chargement est simulé à l'affectation de l'adresse.
    let lisible = true
    const src = Object.getOwnPropertyDescriptor(HTMLImageElement.prototype, 'src')!
    vi.spyOn(HTMLImageElement.prototype, 'src', 'set').mockImplementation(function (this: HTMLImageElement, v: string) {
      src.set!.call(this, v)
      setTimeout(() => (lisible ? this.onload : this.onerror)?.call(this, new Event('load')))
    })
    vi.spyOn(HTMLImageElement.prototype, 'naturalWidth', 'get').mockReturnValue(800)
    vi.spyOn(HTMLImageElement.prototype, 'naturalHeight', 'get').mockReturnValue(600)
    const drawImage = vi.fn()
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({ drawImage, imageSmoothingQuality: 'low' } as unknown as CanvasRenderingContext2D)
    let webp = true
    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation(function (this: HTMLCanvasElement, cb, type) {
      cb(type === 'image/webp' && !webp ? new Blob(['x'], { type: 'image/png' }) : new Blob(['x'], { type: type ?? 'image/png' }))
    })
    const b = await recadrerImage('data:image/png;base64,AAAA', { zoom: 2, x: 0, y: 0 }, 128)
    expect(b.type).toBe('image/webp')
    expect(drawImage).toHaveBeenCalledWith(expect.any(HTMLImageElement), 250, 150, 300, 300, 0, 0, 128, 128)
    webp = false
    expect((await recadrerImage('data:,', RECADRAGE_INITIAL)).type).toBe('image/png')
    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation((cb) => cb(null))
    await expect(recadrerImage('data:,', RECADRAGE_INITIAL)).rejects.toThrow('Encodage impossible')
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null)
    await expect(recadrerImage('data:,', RECADRAGE_INITIAL)).rejects.toThrow('Canevas indisponible')
    lisible = false
    await expect(recadrerImage('data:,', RECADRAGE_INITIAL)).rejects.toThrow('Image illisible')
    // Fichier sans createImageBitmap : lu en adresse data: (blob: refusé par la politique de sécurité du contenu).
    lisible = true
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({ drawImage, imageSmoothingQuality: 'low' } as unknown as CanvasRenderingContext2D)
    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation((cb, type) => cb(new Blob(['x'], { type: type ?? 'image/png' })))
    expect((await recadrerImage(new Blob(['x'], { type: 'image/jpeg' }), RECADRAGE_INITIAL)).type).toBe('image/webp')
  })

  it('recadrerImage : orientation EXIF appliquée par createImageBitmap (photo de téléphone, Orientation = 6)', async () => {
    // Fichier 400 × 300 tourné de 90° : le bitmap orienté mesure 300 × 400 ; le carré se prend dans ce sens.
    const close = vi.fn()
    const bitmap = vi.fn(async () => ({ width: 300, height: 400, close }))
    vi.stubGlobal('createImageBitmap', bitmap)
    const drawImage = vi.fn()
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({ drawImage, imageSmoothingQuality: 'low' } as unknown as CanvasRenderingContext2D)
    vi.spyOn(HTMLCanvasElement.prototype, 'toBlob').mockImplementation((cb, type) => cb(new Blob(['x'], { type: type ?? 'image/png' })))
    const jpeg = new Blob([new Uint8Array([0xff, 0xd8, 0xff, 0xe1])], { type: 'image/jpeg' })
    await recadrerImage(jpeg, RECADRAGE_INITIAL, 100)
    expect(bitmap).toHaveBeenCalledWith(jpeg, { imageOrientation: 'from-image' })
    expect(drawImage).toHaveBeenCalledWith(expect.anything(), 0, 50, 300, 300, 0, 0, 100, 100)
    expect(close).toHaveBeenCalled()
    bitmap.mockRejectedValueOnce(new Error('x'))
    await expect(recadrerImage(jpeg, RECADRAGE_INITIAL)).rejects.toThrow('Image illisible')
    vi.unstubAllGlobals()
  })
})

describe('TopBar : menu utilisateur', () => {
  it('avatar avec photo, menu ancré (nom, fonction, entrées), entrée choisie puis fermeture ; Échap et clic extérieur', () => {
    const monCompte = vi.fn()
    const { container } = render(
      <TopBar user="Paul Mbarga Atangana" userDetail="Chef du département DES" avatar="data:image/png;base64,AAAA"
        preferences={<button type="button">Thème</button>}
        userMenu={[{ key: 'compte', label: 'Mon compte', icon: 'user', onClick: monCompte }, { key: 'sortie', label: 'Se déconnecter', icon: 'log-out' }]} />,
    )
    const bouton = screen.getByRole('button', { name: 'Menu utilisateur : Paul Mbarga Atangana' })
    expect(bouton.getAttribute('aria-haspopup')).toBe('menu')
    expect(bouton.querySelector('img')!.getAttribute('src')).toBe('data:image/png;base64,AAAA')
    fireEvent.click(bouton)
    expect(bouton.getAttribute('aria-expanded')).toBe('true')
    const menu = screen.getByRole('menu', { name: 'Menu utilisateur : Paul Mbarga Atangana' })
    expect(container.querySelector('.gm-topbar-menu-nom')!.textContent).toBe('Paul Mbarga Atangana')
    expect(screen.getByText('Chef du département DES')).toBeTruthy()
    expect(container.querySelector('.gm-topbar-menu .gm-avatar-lg img')).toBeTruthy()
    expect(document.activeElement).toBe(within(menu).getAllByRole('menuitem')[0])
    fireEvent.click(within(menu).getByRole('menuitem', { name: 'Mon compte' }))
    expect(monCompte).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('menu')).toBeNull()
    fireEvent.click(bouton)
    fireEvent.click(screen.getByRole('menuitem', { name: 'Se déconnecter' }))
    expect(screen.queryByRole('menu')).toBeNull()
    fireEvent.click(bouton)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByRole('menu')).toBeNull()
    expect(document.activeElement).toBe(bouton)
    fireEvent.click(bouton)
    fireEvent.mouseDown(document.body)
    expect(screen.queryByRole('menu')).toBeNull()
    // Les deux menus surgissants s'excluent.
    fireEvent.click(bouton)
    fireEvent.click(screen.getByRole('button', { name: /Langue/ }))
    expect(screen.queryByRole('menu')).toBeNull()
    expect(screen.getByRole('dialog')).toBeTruthy()
    fireEvent.click(bouton)
    expect(screen.queryByRole('dialog')).toBeNull()
  })
  it('portail : institution dans l’en-tête du menu ; sans entrée, avatar seul', () => {
    const { rerender } = render(<TopBar layer="portail" institution="Établissement A" user="Correspondant" userMenu={[{ label: 'Mon compte' }]} />)
    fireEvent.click(screen.getByRole('button', { name: /Menu utilisateur/ }))
    expect(screen.getAllByText('Établissement A')).toHaveLength(2)
    rerender(<TopBar user="Correspondant" avatar={null} />)
    expect(screen.getByRole('img', { name: 'Correspondant' }).textContent).toBe('C')
    act(() => undefined)
  })
})

describe('avatars de la messagerie et des destinataires', () => {
  it('conversation directe et auteur d’un message avec photo, initiales à défaut', () => {
    const { container } = render(
      <>
        <ConversationList items={[{ id: 'c1', titre: 'Rachel Biyong', avatar: 'data:image/png;base64,R' }, { id: 'c2', titre: 'Serge Ondoua' }]} />
        <MessageThread titre="Fil" messages={[
          { id: 'm1', auteur: 'Rachel Biyong', quand: '10:00', contenu: 'Bonjour', avatar: 'data:image/png;base64,R' },
          { id: 'm2', auteur: 'Paul Mbarga', quand: '10:01', contenu: 'Bonjour', mine: true },
        ]} />
      </>,
    )
    expect(screen.getByRole('img', { name: 'Rachel Biyong' }).querySelector('img')).toBeTruthy()
    expect(screen.getByRole('img', { name: 'Serge Ondoua' }).textContent).toBe('SO')
    // Fond texturé du fil (D-66) : le motif enveloppe la liste des messages seulement.
    expect(container.querySelector('.gm-fil-fond.gm-trame-fil > ol.gm-chat-list')).toBeTruthy()
    expect(container.querySelector('.gm-chat-head')!.closest('.gm-trame-fil')).toBeNull()
    const metas = container.querySelectorAll('.gm-msg-meta .gm-avatar')
    expect(metas).toHaveLength(2)
    expect(metas[0].querySelector('img')).toBeTruthy()
    expect(metas[1].textContent).toBe('PM')
  })
  it('destinataires : avatar dans les suggestions et les jetons choisis, y compris hors résultats', () => {
    const changer = vi.fn()
    const { container } = render(
      <MultiSelect label="Destinataires" suggestions value={['u-1', 'u-9']} onChange={changer} labels={{ 'u-9': 'Mireille Obame' }}
        avatars={{ 'u-9': { name: 'Mireille Obame', src: 'data:image/png;base64,M' } }}
        options={[{ value: 'u-1', label: 'Rachel Biyong', avatar: { name: 'Rachel Biyong', src: 'data:image/png;base64,R' } }, { value: 'u-2', label: 'Serge Ondoua', avatar: { name: 'Serge Ondoua' } }, { value: 'u-3', label: 'Sans avatar' }]} />,
    )
    expect(container.querySelectorAll('.gm-tag-personne img')).toHaveLength(2)
    fireEvent.focus(screen.getByRole('combobox'))
    const options = screen.getAllByRole('option')
    expect(options[0].querySelector('.gm-avatar')!.textContent).toBe('SO')
    expect(options[1].querySelector('.gm-avatar')).toBeNull()
    render(<Tag avatar={{ name: 'Aïcha Mahamat' }}>Aïcha Mahamat</Tag>)
    expect(screen.getByText('Aïcha Mahamat').querySelector('.gm-avatar-xs')!.textContent).toBe('AM')
  })
})
