import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { useLocation } from 'react-router'
import { sha256Hex } from '@gimac/socle'
import { OngletChronologie, OngletPreuves, OngletSignatures, lignesPreuves } from './OngletPreuves'
import { Chronologie, LivrablesEtPreuves, SignaturesOperation, verifierPiece } from '../operations/Preuves'
import { rendrePage, sessionTest } from '../../../../test/rendu'
import { servicesSimules } from '../../../../services/simule'
import type { Services } from '../../../../services/ports'
import type { DossierLiaison, EvenementPreuve, OperationParcours } from '../../../../modele/parcours'
import { PROFILS } from '../../../../permissions'

vi.mock('../../../../ressources/fichier', () => ({ enregistrerFichier: vi.fn() }))

afterEach(cleanup)

// Onglets D-109 du dossier : livrables et preuves par étape, signatures, chronologie ; bloc des preuves d'une opération.

const op = (o: Partial<OperationParcours>): OperationParcours => ({
  id: 'op-1', liaisonId: 'L-1', rang: '13', etape: 'INTEGRATION', segment: 'COMMUN', plateforme: null, grain: 'OPERATION', etat: 'EN_COURS',
  raci: { r: null, a: null, c: [], i: [], e: [] }, echeance: null, sla: null, faiteLe: null, preuve: null, cle: false, secret: false,
  valeurReference: false, oubliSansSymptome: false, pivot: false, predecesseursNonFaits: [], actionsPermises: [], libelle: 'Contrôle d’atteignabilité du port', ...o,
} as unknown as OperationParcours)

const ping = op({
  livrablesAttendus: [
    { code: 'PING', libelle: 'Capture du ping', nature: 'PREUVE', deposant: 'SNA', typesPieces: ['PNG'], obligatoire: true, peutDeposer: true, grain: 'OPERATION', consigne: 'Ping de l’adresse d’essai', etat: 'ATTENDUE' },
    { code: 'EP', libelle: 'Points de terminaison PayWay', nature: 'LIVRABLE', deposant: null, typesPieces: [], obligatoire: false, peutDeposer: false, grain: 'SERVICE', services: [
      { serviceCode: 'P2P', etat: 'DEPOSEE', depot: { documentRef: 'DOC-1', nom: 'ep.png', empreinte: 'ab'.repeat(32), deposeLe: '2026-10-10', deposePar: 'SNA', origine: 'auto', repris: true } },
      { serviceCode: 'CASHIN', etat: 'SANS_OBJET', depot: { sansObjet: true, motif: 'Hors périmètre', deposeLe: '2026-10-11', deposePar: 'Conducteur' } },
    ] },
  ],
  signatures: [
    { code: 'S2', libelle: 'Visa SNA', ordre: 2, signataire: 'SNA', libelleSignataire: 'Ingénieur SNA', porte: [], etat: 'SIGNEE', suivante: false, signeParNom: 'Guilene', signeLe: '2026-10-12' },
    { code: 'S1', libelle: 'Visa conducteur', ordre: 1, signataire: 'CONDUCTEUR', porte: [], etat: 'ATTENDUE', suivante: true, attendu: { id: 'u', nom: 'Berni', fonction: 'CONDUCTEUR' } },
    { code: 'S3', libelle: 'Visa RSSI', ordre: 3, signataire: 'RSSI', porte: [], etat: 'ATTENDUE', suivante: false },
  ],
  actionsPermises: ['SIGNER'],
})
const reprise = op({ id: 'op-2', rang: '9', etape: 'ATELIERS', etat: 'FAITE', reprise: true, livrablesAttendus: [
  { code: 'CR', libelle: 'Compte rendu d’atelier', nature: 'LIVRABLE', deposant: 'CONDUCTEUR', typesPieces: [], obligatoire: true, peutDeposer: false, etat: 'REPRISE_SANS_PIECE' },
], signatures: [{ code: 'R1', libelle: 'Visa repris', ordre: 1, signataire: 'CONDUCTEUR', porte: [], etat: 'REPRISE', suivante: false }] })

const avec = (ops: OperationParcours[], surcharge: Partial<Services['parcours']> = {}): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, parcours: { ...s.parcours, operations: async () => ops, ...surcharge } }
}
const dossier = { id: 'L-1', etapesVersion: [{ code: 'ATELIERS', libelle: 'Ateliers', ordre: 5 }, { code: 'INTEGRATION', libelle: 'Intégration', ordre: 6 }] } as unknown as DossierLiaison
function Lieu() { const l = useLocation(); return <span data-testid="lieu">{l.search}</span> }
const page = (el: React.ReactElement, services: Services) =>
  rendrePage(<>{el}<Lieu /></>, { chemin: '/des/parcours/L-1', motif: '/des/parcours/:id', services, session: sessionTest(PROFILS.ROLE_CHEF_DDRM, ['ROLE_CHEF_DDRM']) })

describe('D-109 : onglet « Livrables et preuves »', () => {
  it('une ligne par livrable et par service, compteurs, étapes de la version, ouverture de l’opération', async () => {
    page(<OngletPreuves dossier={dossier} />, avec([ping, reprise]))
    expect(await screen.findByRole('heading', { name: 'Intégration' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Ateliers' })).toBeTruthy()
    expect(screen.getByText('Capture du ping')).toBeTruthy()
    expect(screen.getByText('Ping de l’adresse d’essai')).toBeTruthy()
    expect(screen.getByText('EP · P2P')).toBeTruthy()
    expect(screen.getByText('Hors périmètre')).toBeTruthy()
    expect(screen.getByText('Reprise, pièce non numérisée')).toBeTruthy()
    expect(screen.getAllByText('Sans objet').length).toBeGreaterThan(0)
    const integration = screen.getByRole('heading', { name: 'Intégration' }).closest('section') as HTMLElement
    expect(within(integration).getByText('1 preuve attendue')).toBeTruthy()
    expect(screen.getByText('Preuves réunies')).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: /13 ·/ })[0])
    await waitFor(() => expect(screen.getByTestId('lieu').textContent).toContain('operation=op-1'))
  })
  it('lignes : grain du service sans services → une seule ligne attendue', () => {
    const o = op({ livrablesAttendus: [{ code: 'X', libelle: 'X', nature: 'PREUVE', deposant: null, typesPieces: [], obligatoire: true, peutDeposer: false, grain: 'SERVICE', services: [] }] })
    expect(lignesPreuves(o)).toEqual([expect.objectContaining({ cle: 'op-1/X', etat: 'ATTENDUE' })])
    expect(lignesPreuves(op({}))).toEqual([])
  })
  it('aucune opération : état vide', async () => {
    page(<OngletPreuves dossier={dossier} />, avec([]))
    expect(await screen.findByText(/Aucune opération/i)).toBeTruthy()
  })
})

describe('D-109 : onglet « Signatures »', () => {
  it('ordre, signataire, état, signée par, à venir ; reprise', async () => {
    page(<OngletSignatures dossier={dossier} />, avec([ping, reprise]))
    expect(await screen.findByText('Visa conducteur')).toBeTruthy()
    expect(screen.getByText(/^Guilene · .*2026/)).toBeTruthy()
    expect(screen.getByText('Berni')).toBeTruthy()
    expect(screen.getByText('Reprise, non recueillie sur la plateforme')).toBeTruthy()
    expect(screen.getByText('Signatures attendues')).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: /9 ·/ })[0])
    await waitFor(() => expect(screen.getByTestId('lieu').textContent).toContain('operation=op-2'))
  })
  it('aucune signature', async () => {
    page(<OngletSignatures dossier={dossier} />, avec([op({})]))
    expect(await screen.findByText('Aucune signature attendue.')).toBeTruthy()
  })
})

describe('D-109 : onglet « Chronologie »', () => {
  const evenements: EvenementPreuve[] = [
    { date: '2026-10-12', type: 'DEPOT', operationId: 'op-1', rang: '13', operation: 'Ping', acteur: 'SNA', livrable: 'EP', service: 'P2P', detail: 'ep.png' },
    { date: null, type: 'BLOCAGE', etapeCode: 'TESTS', detail: 'Port filtré' },
    { date: '2026-10-01', type: 'REPRISE', rang: '9', operation: 'Atelier réseau' },
  ]
  it('événements, ouverture de l’opération, étape à défaut d’opération', async () => {
    page(<OngletChronologie dossier={dossier} />, avec([], { chronologie: async () => evenements }))
    expect(await screen.findByText('Pièce déposée')).toBeTruthy()
    expect(screen.getByText('EP · P2P : ep.png')).toBeTruthy()
    expect(screen.getByText('9 · Atelier réseau')).toBeTruthy()
    expect(screen.getByText('Port filtré')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: '13 · Ping' }))
    await waitFor(() => expect(screen.getByTestId('lieu').textContent).toContain('operation=op-1'))
  })
  it('composant seul : sans colonne d’opération, liste vide', () => {
    page(<Chronologie evenements={[]} avecOperation={false} />, avec([]))
    expect(screen.getByText('Aucun événement.')).toBeTruthy()
  })
})

describe('D-109 : preuves et signatures d’une opération', () => {
  it('livrables : nature, déposant, consigne, sans objet proposé sur une preuve attendue, pièce téléchargée vérifiée', async () => {
    const contenu = new Blob(['capture'])
    const empreinte = await sha256Hex(contenu)
    const o = op({ livrablesAttendus: [ping.livrablesAttendus![0], { ...ping.livrablesAttendus![1], services: [{ serviceCode: 'P2P', etat: 'DEPOSEE', depot: { documentRef: 'DOC-1', nom: 'ep.png', empreinte, deposeLe: '2026-10-10', deposePar: 'SNA', origine: 'auto', repris: true } }] }] })
    const telechargerPiece = vi.fn(async () => ({ blob: contenu, empreinte }))
    page(<LivrablesEtPreuves op={o} />, avec([], { telechargerPiece }))
    expect(screen.getByText('Preuves et livrables attendus')).toBeTruthy()
    expect(screen.getByText(/Preuve · obligatoire/)).toBeTruthy()
    expect(screen.getAllByText(/une pièce par service/).length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: 'Déclarer PING sans objet' })).toBeTruthy()
    expect(screen.getByText(/pièce produite par la plateforme/)).toBeTruthy()
    expect(screen.getByText(/dépôt repris/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Télécharger ep.png' }))
    expect(await screen.findByText(/Empreinte vérifiée/)).toBeTruthy()
    expect(telechargerPiece).toHaveBeenCalledWith('op-1', 'DOC-1')
  })
  it('pièce divergente, erreur de téléchargement', async () => {
    const o = op({ livrablesAttendus: [{ code: 'A', libelle: 'A', nature: 'LIVRABLE', deposant: null, typesPieces: [], obligatoire: true, peutDeposer: false, etat: 'DEPOSEE', depot: { documentRef: 'D', nom: 'a.pdf', empreinte: 'cd'.repeat(32), deposeLe: '2026-10-10', deposePar: 'X' } }] })
    let n = 0
    page(<LivrablesEtPreuves op={o} />, avec([], { telechargerPiece: async () => { if (n++) throw new Error('Pièce introuvable'); return { blob: new Blob(['autre']), empreinte: null } } }))
    fireEvent.click(screen.getByRole('button', { name: 'Télécharger a.pdf' }))
    expect(await screen.findByText(/Empreinte divergente/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Télécharger a.pdf' }))
    expect(await screen.findByText('Pièce introuvable')).toBeTruthy()
  })
  it('opération reprise : bandeau, aucune pièce exigée ; sans livrable', () => {
    page(<><LivrablesEtPreuves op={op({ ...reprise, livrablesAttendus: [{ ...reprise.livrablesAttendus![0], depot: null }] })} /><LivrablesEtPreuves op={op({ id: 'op-3' })} /></>, avec([]))
    expect(screen.getByText('Opération reprise de l’existant'.replace('’', "'"))).toBeTruthy()
    expect(screen.getByText("Reprise de l'existant, pièce non numérisée")).toBeTruthy()
    expect(screen.getByText('Aucune preuve attendue pour cette opération.')).toBeTruthy()
  })
  it('signatures de l’opération : bouton sur la suivante, aucune signature', () => {
    page(<><SignaturesOperation op={ping} /><SignaturesOperation op={op({ id: 'op-4' })} /></>, avec([]))
    expect(screen.getByRole('button', { name: /Visa conducteur/ })).toBeTruthy()
    expect(screen.getByText(/^Guilene · .*2026/)).toBeTruthy()
    expect(screen.getByText('Aucune signature attendue.')).toBeTruthy()
  })
  it('vérification d’empreinte : sans empreinte inscrite, casse, empreinte reçue divergente', async () => {
    const b = new Blob(['x'])
    const h = await sha256Hex(b)
    expect(await verifierPiece(b, null, null)).toBeNull()
    expect(await verifierPiece(b, h.toUpperCase(), null)).toBe(true)
    expect(await verifierPiece(b, h, 'ff'.repeat(32))).toBe(false)
  })
})
