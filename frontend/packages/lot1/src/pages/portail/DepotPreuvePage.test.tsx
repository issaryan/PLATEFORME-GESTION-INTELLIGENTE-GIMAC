import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { rendrePage } from '../../test/rendu'
import DepotPreuvePage from './DepotPreuvePage'

afterEach(cleanup)

const ouvrir = () =>
  rendrePage(<DepotPreuvePage />, { chemin: '/portail/liaisons/LIA-2026-017/operations/OPI-017-4/preuve', motif: '/portail/liaisons/:id/operations/:op/preuve' })

const choisir = (container: HTMLElement, nom: string) => {
  const entree = container.querySelector('input[type="file"]') as HTMLInputElement
  fireEvent.change(entree, { target: { files: [new File(['%PDF'], nom, { type: 'application/pdf' })] } })
}

describe('POR-03 Dépôt de preuve', () => {
  it('POR-03 : le dépôt n\'est possible qu\'avec un fichier, puis l\'accusé porte l\'empreinte', async () => {
    const { container } = ouvrir()
    const bouton = await screen.findByRole('button', { name: 'Déposer la preuve' })
    expect((bouton as HTMLButtonElement).disabled).toBe(true)
    choisir(container, 'rapport-tests.pdf')
    expect(screen.getByText('rapport-tests.pdf')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Déposer la preuve' }))
    const dialogue = await screen.findByRole('alertdialog')
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Déposer la preuve' }))
    expect(await screen.findByText('Preuve déposée')).toBeTruthy()
    expect(screen.getByText('Empreinte du fichier déposé')).toBeTruthy()
  })

  it('POR-03 : un refus de la chaîne de contrôle est expliqué avec son identifiant de corrélation', async () => {
    const { container } = ouvrir()
    await screen.findByRole('button', { name: 'Déposer la preuve' })
    choisir(container, 'carte-client.pdf')
    fireEvent.click(screen.getByRole('button', { name: 'Déposer la preuve' }))
    const dialogue = await screen.findByRole('alertdialog')
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Déposer la preuve' }))
    await waitFor(() => expect(dialogue.textContent).toContain('numéro de carte non masqué'))
    expect(dialogue.textContent).toMatch(/sim-/)
  })
})
