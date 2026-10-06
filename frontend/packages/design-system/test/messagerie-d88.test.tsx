// Accusés de lecture (D-88) : coches sous mes messages, texte accessible, détail « Lu par » d'un message de
// groupe (survol, clic, Échap), coches devant l'extrait de la liste ; libellés fr, en, es.
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { ConversationList, LangueProvider, MessageThread } from '../src'

afterEach(cleanup)

describe('MessageThread : accusés de lecture (D-88)', () => {
  it('une coche grise, deux coches grises, deux coches d’accent ; jamais sous les messages des autres', () => {
    const { container } = render(
      <MessageThread
        titre="Échange"
        messages={[
          { id: 'a', auteur: 'Aïcha', quand: '09:00', contenu: 'Bonjour', lecture: { etat: 'LU', lus: 1, destinataires: 1 } },
          { id: 'b', auteur: 'Paul', quand: '09:01', contenu: 'Envoyé', mine: true, lecture: { etat: 'ENVOYE', lus: 0, destinataires: 1 } },
          { id: 'c', auteur: 'Paul', quand: '09:02', contenu: 'Lu', mine: true, lecture: { etat: 'LU', lus: 1, destinataires: 1 } },
          { id: 'd', auteur: 'Paul', quand: '09:03', contenu: 'Sans accusé', mine: true },
        ]}
      />,
    )
    expect(screen.getByRole('img', { name: 'Envoyé' }).classList.contains('gm-coches-lu')).toBe(false)
    expect(screen.getByRole('img', { name: 'Lu' }).classList.contains('gm-coches-lu')).toBe(true)
    expect(container.querySelectorAll('.gm-msg-lecture')).toHaveLength(2)
  })

  it('groupe : « Lu par 2 sur 3 », détail au clic, au survol, fermé par Échap', () => {
    render(
      <MessageThread
        titre="Groupe"
        messages={[{ id: 'g', auteur: 'Paul', quand: '09:00', contenu: 'Merci', mine: true, lecture: { etat: 'LU_PAR_CERTAINS', lus: 2, destinataires: 3, detail: <p>Rachel, 09:20</p> } }]}
      />,
    )
    const bouton = screen.getByRole('button', { name: 'Lu par 2 sur 3, voir qui l’a lu' })
    expect(bouton.textContent).toContain('2/3')
    expect(screen.queryByText('Rachel, 09:20')).toBeNull()
    fireEvent.click(bouton)
    const detail = screen.getByRole('dialog', { name: 'Lu par' })
    expect(within(detail).getByText('Rachel, 09:20')).toBeTruthy()
    expect(bouton.getAttribute('aria-controls')).toBe(detail.id)
    fireEvent.keyDown(bouton, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).toBeNull()
    fireEvent.keyDown(bouton, { key: 'Escape' })
    fireEvent.mouseEnter(bouton.parentElement!)
    expect(screen.getByRole('dialog', { name: 'Lu par' })).toBeTruthy()
    fireEvent.mouseLeave(bouton.parentElement!)
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('liste : coches devant l’extrait ; libellés anglais et espagnols', () => {
    const { unmount } = render(
      <LangueProvider langue="en">
        <ConversationList items={[{ id: '1', titre: 'Aïcha', extrait: 'Vous : Merci', lecture: { etat: 'LU_PAR_CERTAINS', lus: 1, destinataires: 2 } }]} />
      </LangueProvider>,
    )
    expect(screen.getByRole('img', { name: 'Read by 1 of 2' })).toBeTruthy()
    unmount()
    render(
      <LangueProvider langue="es">
        <MessageThread titre="G" messages={[{ id: 'x', auteur: 'P', quand: '1', contenu: 'x', mine: true, lecture: { etat: 'LU', lus: 2, destinataires: 2, detail: <span /> } }]} />
      </LangueProvider>,
    )
    expect(screen.getByRole('button', { name: 'Leído, ver quién lo leyó' })).toBeTruthy()
  })
})
