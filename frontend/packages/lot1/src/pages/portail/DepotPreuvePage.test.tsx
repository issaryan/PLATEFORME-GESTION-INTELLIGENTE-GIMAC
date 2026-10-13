import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import type { TacheActeur } from '../../modele/parcours'
import DepotPreuvePage from './DepotPreuvePage'
import { auditer, ouvrir, sansCleNiNomInterne, services } from './parcours/outilsTest'

afterEach(cleanup)

const MOTIF = '/portail/liaisons/:id/livrables/:op'
const OP = 'LIA-2026-030:15'
const TACHE: TacheActeur = {
  operationId: OP, liaisonId: 'LIA-2026-030', liaisonReference: 'LIA-2026-030', institutionNom: 'Établissement de paiement A', rang: '15', etape: 'INTEGRATION',
  qualite: 'E', echeance: '2026-10-02', sla: 'ECHEANCE_PROCHE', oubliSansSymptome: true, lien: `/portail/liaisons/LIA-2026-030/livrables/${encodeURIComponent(OP)}`, actionsPermises: ['DEPOSER_LIVRABLE'],
}
/** Scénario : le paramétrage d'essai (rang 15, pièce de phase) attend un livrable du participant. */
const avecTache = (deposer = vi.fn(() => Promise.resolve())) =>
  services((b) => ({ mesTaches: async () => ({ ...(await b.portailParcours.mesTaches()), taches: [TACHE] }), deposerLivrable: deposer }))
const monter = (o: Parameters<typeof ouvrir>[3] = {}) => ouvrir(<DepotPreuvePage />, `/portail/liaisons/LIA-2026-030/livrables/${encodeURIComponent(OP)}`, MOTIF, o)
const fichier = (nom: string) => fireEvent.change(document.querySelector('input[type="file"]')!, { target: { files: [new File(['contenu'], nom, { type: 'application/pdf' })] } })

describe('POR-03 Dépôt d’un livrable du participant', () => {
  it('pièce attendue, échéance, alerte d’oubli ; fichier exigé, confirmation, dépôt par le hook du portail', async () => {
    const deposer = vi.fn(() => Promise.resolve())
    const { container } = monter({ services: avecTache(deposer) })
    expect(await screen.findByText('Paramétrage d\'essai, sur les mêmes objets que la branche du type de projet')).toBeTruthy()
    expect(screen.getByText('Document de paramétrage, livrable unique et imposé de la phase')).toBeTruthy()
    expect(screen.getByText('Échéance proche')).toBeTruthy()
    expect(screen.getByText(/Opération facile à omettre/)).toBeTruthy()
    await auditer(container)
    fireEvent.click(screen.getByRole('button', { name: 'Déposer le livrable' }))
    expect(await screen.findByText('Choisissez le fichier à déposer.')).toBeTruthy()
    fichier('parametrage-essai.pdf')
    fireEvent.change(document.querySelector('[name="commentaire"]')!, { target: { value: 'Version validée en atelier' } })
    fireEvent.click(screen.getByRole('button', { name: 'Déposer le livrable' }))
    const dialogue = await screen.findByRole('alertdialog')
    expect(within(dialogue).getByText('parametrage-essai.pdf')).toBeTruthy()
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Déposer le livrable' }))
    await vi.waitFor(() => expect(deposer).toHaveBeenCalledWith(OP, expect.any(File), 'Version validée en atelier'))
  })

  it('tâche close ou étrangère : état explicite et retour au suivi', async () => {
    monter()
    expect(await screen.findByText('Ce livrable ne vous est pas demandé')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Suivi de LIA-2026-030' })).toBeTruthy()
    sansCleNiNomInterne()
  })

  it('sans HOM_PORTAL_SUBMIT : pas de formulaire', async () => {
    monter({ services: avecTache(), sans: ['HOM_PORTAL_SUBMIT'] })
    expect(await screen.findByText(/pas de l'accomplir/)).toBeTruthy()
    expect(document.querySelector('input[type="file"]')).toBeNull()
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    monter({ services: avecTache(), langue })
    await screen.findByText(langue === 'en' ? 'Upload the deliverable' : 'Depositar el entregable')
    sansCleNiNomInterne()
  })
})
