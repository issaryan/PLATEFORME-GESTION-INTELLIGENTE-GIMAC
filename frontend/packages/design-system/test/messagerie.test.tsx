// Messagerie de travail (D-64) : liste des conversations, fil et zone de saisie.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { ConversationList, LangueProvider, MessageComposer, MessageThread } from '../src'

afterEach(cleanup)

describe('ConversationList', () => {
  it('conversations, non-lus en pastille, conversation ouverte marquée, sélection', () => {
    const choisir = vi.fn()
    render(
      <ConversationList
        selected="c2"
        onSelect={choisir}
        items={[
          { id: 'c1', titre: 'Aïcha Mahamat Nour', extrait: 'Le procès-verbal est établi', quand: '15:12', nonLus: 2 },
          { id: 'c2', titre: 'Essais de LIA-2026-030', sousTitre: '4 membres', extrait: 'Rapport joint', groupe: true },
        ]}
      />,
    )
    const liste = screen.getByRole('list', { name: 'Conversations' })
    expect(within(liste).getAllByRole('button')).toHaveLength(2)
    expect(screen.getByLabelText('2 non lus').textContent).toBe('2')
    expect(screen.getByRole('button', { name: /Essais de LIA-2026-030/ }).getAttribute('aria-current')).toBe('true')
    expect(screen.getByRole('button', { name: /Aïcha/ }).className).toContain('gm-conv-nonlu')
    fireEvent.click(screen.getByRole('button', { name: /Aïcha/ }))
    expect(choisir).toHaveBeenCalledWith('c1')
    // Groupe : pictogramme, pas d'initiales.
    expect(screen.getByRole('button', { name: /Essais/ }).querySelector('.gm-avatar svg')).toBeTruthy()
  })
  it('aucune conversation : état vide, libellé fourni ou par défaut', () => {
    const { rerender } = render(<ConversationList items={[]} />)
    expect(screen.getByText('Aucune conversation')).toBeTruthy()
    rerender(<ConversationList items={[]} empty="Rien ici" />)
    expect(screen.getByText('Rien ici')).toBeTruthy()
  })
})

describe('MessageThread', () => {
  it('messages, « Vous » pour les siens, étiquettes, pièces téléchargeables, plus anciens', () => {
    const piece = vi.fn()
    const plus = vi.fn()
    render(
      <MessageThread
        titre="Essais de LIA-2026-030"
        sousTitre="4 membres"
        actions={<button type="button">Ouvrir la liaison</button>}
        plusAnciens
        onPlusAnciens={plus}
        onPiece={piece}
        saisie={<div>saisie</div>}
        messages={[
          { id: 'm1', auteur: 'Rachel Biyong Mengue', quand: '09:10', contenu: 'Rapport joint', etiquettes: <a href="/des/parcours/LIA-2026-030">LIA-2026-030</a>, pieces: [{ id: 'p1', nom: 'rapport.pdf', taille: '12 Ko' }] },
          { id: 'm2', auteur: 'Paul', quand: '09:12', contenu: 'Merci', mine: true },
        ]}
      />,
    )
    const fil = screen.getByRole('list', { name: 'Messages' })
    expect(within(fil).getByText('Vous')).toBeTruthy()
    expect(within(fil).getByRole('link', { name: 'LIA-2026-030' }).getAttribute('href')).toBe('/des/parcours/LIA-2026-030')
    fireEvent.click(screen.getByRole('button', { name: 'Télécharger rapport.pdf' }))
    expect(piece).toHaveBeenCalledWith('p1')
    fireEvent.click(screen.getByRole('button', { name: 'Afficher les messages plus anciens' }))
    expect(plus).toHaveBeenCalled()
    expect(screen.getByRole('region', { name: 'Essais de LIA-2026-030' })).toBeTruthy()
    expect(screen.getByText('saisie')).toBeTruthy()
    expect(screen.getByText('12 Ko', { exact: false })).toBeTruthy()
  })
  it('fil vide', () => {
    render(<MessageThread titre="Nouvelle conversation" messages={[]} />)
    expect(screen.getByText('Aucun message pour le moment')).toBeTruthy()
  })
})

describe('MessageComposer', () => {
  it('Entrée envoie, Maj + Entrée non ; envoi désactivé sans contenu ni pièce', () => {
    const envoyer = vi.fn()
    const changer = vi.fn()
    const { rerender } = render(<MessageComposer value="" onChange={changer} onSend={envoyer} />)
    expect((screen.getByRole('button', { name: 'Envoyer' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.keyDown(screen.getByLabelText('Votre message'), { key: 'Enter' })
    expect(envoyer).not.toHaveBeenCalled()
    fireEvent.change(screen.getByLabelText('Votre message'), { target: { value: 'Bonjour' } })
    expect(changer).toHaveBeenCalledWith('Bonjour')
    rerender(<MessageComposer value="Bonjour" onChange={changer} onSend={envoyer} />)
    fireEvent.keyDown(screen.getByLabelText('Votre message'), { key: 'Enter', shiftKey: true })
    expect(envoyer).not.toHaveBeenCalled()
    fireEvent.keyDown(screen.getByLabelText('Votre message'), { key: 'Enter' })
    expect(envoyer).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByRole('button', { name: 'Envoyer' }))
    expect(envoyer).toHaveBeenCalledTimes(2)
  })
  it('pièces jointes : ajout par le sélecteur, plafond, retrait, erreur et compléments', () => {
    const fichiers = vi.fn()
    const a = new File(['a'], 'a.pdf', { type: 'application/pdf' })
    const b = new File(['b'], 'b.png', { type: 'image/png' })
    const { container, rerender } = render(<MessageComposer value="" onChange={() => undefined} onSend={() => undefined} fichiers={[a]} onFichiers={fichiers} maxFichiers={2} error="Type non admis" extras={<span>étiquettes</span>} />)
    const entree = container.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(entree, { target: { files: [b, b] } })
    expect(fichiers).toHaveBeenLastCalledWith([a, b])
    fireEvent.click(screen.getByRole('button', { name: /Retirer/ }))
    expect(fichiers).toHaveBeenLastCalledWith([])
    expect(screen.getByText('Type non admis')).toBeTruthy()
    expect(screen.getByText('étiquettes')).toBeTruthy()
    // Avec une pièce seule, l'envoi est possible ; au plafond, plus de pièce à joindre.
    expect((screen.getByRole('button', { name: 'Envoyer' }) as HTMLButtonElement).disabled).toBe(false)
    rerender(<MessageComposer value="" onChange={() => undefined} onSend={() => undefined} fichiers={[a, b]} maxFichiers={2} />)
    expect((screen.getAllByRole('button', { name: 'Joindre des fichiers' })[0] as HTMLButtonElement).disabled).toBe(true)
  })
  it('se traduit', () => {
    render(<LangueProvider langue="en"><MessageComposer value="" onChange={() => undefined} onSend={() => undefined} /></LangueProvider>)
    expect(screen.getByRole('button', { name: 'Send' })).toBeTruthy()
    expect(screen.getByLabelText('Your message')).toBeTruthy()
  })
})
