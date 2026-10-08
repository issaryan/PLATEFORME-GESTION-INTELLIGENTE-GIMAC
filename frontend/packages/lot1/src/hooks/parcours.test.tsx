import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider, type QueryKey } from '@tanstack/react-query'
import { ServicesProvider } from '../services/ServicesProvider'
import type { Services } from '../services/ports'
import * as H from './parcours'
import { cles } from './cles'

// Hooks du parcours (D-56 à D-59) : chaque lecture appelle la bonne méthode du port injecté sous la bonne clé ;
// chaque action appelle la bonne méthode avec les bons arguments puis invalide les sujets « parcours » et « homologation ».
type Espion = ReturnType<typeof vi.fn>
const port = (noms: string[]) => Object.fromEntries(noms.map((n) => [n, vi.fn(async () => ({ resultat: n }))])) as Record<string, Espion>
function monter() {
  const services = {
    parcours: port(['listerLiaisons', 'conducteurs', 'ouvrirLiaison', 'designerConducteur', 'dossier', 'operations', 'affecterRessources', 'deposerLivrable', 'attribuer', 'deposerPreuve', 'validerOperation', 'declarerBlocage', 'leverBlocage', 'constaterCouverture', 'ouvrirServiceSupplementaire', 'tableauActeur', 'pilotage', 'personnes']),
    decision: port(['procesVerbal', 'etablirProcesVerbal', 'signerProcesVerbalGimac', 'approbation', 'constituerDossierChangement', 'referencerFicheChangement', 'relireFicheChangement', 'verifierAttestation', 'archiverAttestations', 'signerVisa', 'ficheSuivi', 'remettreFicheSuivi', 'constaterMiseEnService']),
    portailParcours: port(['mesLiaisons', 'dossier', 'mesTaches', 'communiquerCoordonnees', 'deposerLivrable', 'procesVerbal', 'signerProcesVerbal', 'approbation', 'deposerAttestation', 'ficheSuivi', 'renseignerFicheSuivi']),
  }
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const invalider = vi.spyOn(client, 'invalidateQueries')
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}><ServicesProvider services={services as unknown as Services}>{children}</ServicesProvider></QueryClientProvider>
  )
  return { services, client, invalider, wrapper }
}
const fichier = new File(['x'], 'cr.pdf')

describe('hooks du parcours : lectures', () => {
  const lectures: [string, () => { data?: unknown }, 'parcours' | 'decision' | 'portailParcours', string, unknown[], QueryKey][] = [
    ['useLiaisonsParcours', () => H.useLiaisonsParcours({ page: 1, taille: 25, mesProjets: true }), 'parcours', 'listerLiaisons', [{ page: 1, taille: 25, mesProjets: true }], cles.parcoursLiaisons({ page: 1, taille: 25, mesProjets: true })],
    ['useConducteurs', () => H.useConducteurs(), 'parcours', 'conducteurs', [], ['parcours', 'personnes', 'CONDUCTEUR']],
    ['usePersonnes', () => H.usePersonnes('RSSI'), 'parcours', 'personnes', ['RSSI'], ['parcours', 'personnes', 'RSSI']],
    ['useDossierLiaison', () => H.useDossierLiaison('L1'), 'parcours', 'dossier', ['L1'], ['parcours', 'dossier', 'L1']],
    ['useOperationsParcours', () => H.useOperationsParcours('L1', { etape: 'TESTS', fonction: 'SNA' }), 'parcours', 'operations', ['L1', { etape: 'TESTS', fonction: 'SNA' }], ['parcours', 'dossier', 'L1', 'operations:TESTS:SNA']],
    ['useOperationsParcours (sans filtre)', () => H.useOperationsParcours('L1'), 'parcours', 'operations', ['L1', undefined], ['parcours', 'dossier', 'L1', 'operations::']],
    ['useTableauActeur', () => H.useTableauActeur(['SNA', 'RSSI']), 'parcours', 'tableauActeur', [['SNA', 'RSSI']], ['parcours', 'acteur', 'SNA,RSSI']],
    ['usePilotage', () => H.usePilotage(), 'parcours', 'pilotage', [], ['parcours', 'pilotage']],
    ['useProcesVerbal', () => H.useProcesVerbal('L1'), 'decision', 'procesVerbal', ['L1'], ['parcours', 'dossier', 'L1', 'pv']],
    ['useApprobation', () => H.useApprobation('L1'), 'decision', 'approbation', ['L1'], ['parcours', 'dossier', 'L1', 'approbation']],
    ['useFicheSuivi', () => H.useFicheSuivi('L1'), 'decision', 'ficheSuivi', ['L1'], ['parcours', 'dossier', 'L1', 'fiche-suivi']],
    ['useMesLiaisonsPortail', () => H.useMesLiaisonsPortail(), 'portailParcours', 'mesLiaisons', [], ['parcours', 'portail', 'liaisons']],
    ['useDossierPortail', () => H.useDossierPortail('L1'), 'portailParcours', 'dossier', ['L1'], ['parcours', 'portail', 'dossier', 'L1']],
    ['useMesTachesPortail', () => H.useMesTachesPortail(), 'portailParcours', 'mesTaches', [], ['parcours', 'portail', 'taches']],
    ['useProcesVerbalPortail', () => H.useProcesVerbalPortail('L1'), 'portailParcours', 'procesVerbal', ['L1'], ['parcours', 'portail', 'pv', 'L1']],
    ['useApprobationPortail', () => H.useApprobationPortail('L1'), 'portailParcours', 'approbation', ['L1'], ['parcours', 'portail', 'approbation', 'L1']],
    ['useFicheSuiviPortail', () => H.useFicheSuiviPortail('L1'), 'portailParcours', 'ficheSuivi', ['L1'], ['parcours', 'portail', 'fiche-suivi', 'L1']],
  ]
  it.each(lectures)('%s : méthode du port, arguments et clé de requête', async (_nom, hook, sujet, methode, args, cle) => {
    const { services, client, wrapper } = monter()
    const { result } = renderHook(hook, { wrapper })
    await waitFor(() => expect(result.current.data).toEqual({ resultat: methode }))
    expect(services[sujet][methode]).toHaveBeenCalledTimes(1)
    expect(services[sujet][methode]).toHaveBeenCalledWith(...args)
    expect(client.getQueryData(cle)).toEqual({ resultat: methode })
  })

  it('lectures désactivées : identifiant vide, fonctions vides ou drapeau inactif, aucun appel', async () => {
    const { services, wrapper } = monter()
    renderHook(() => {
      H.useDossierLiaison('')
      H.useOperationsParcours('')
      H.useProcesVerbal('')
      H.useApprobation('')
      H.useFicheSuivi('')
      H.useDossierPortail('')
      H.useProcesVerbalPortail('')
      H.useApprobationPortail('')
      H.useFicheSuiviPortail('')
      H.useTableauActeur(['SNA'], false)
      H.useTableauActeur(['SNA'], false)
      H.useConducteurs(false)
      H.usePersonnes('SNA', false)
      H.usePilotage(false)
    }, { wrapper })
    await new Promise((r) => setTimeout(r, 20))
    Object.values(services).forEach((p) => Object.values(p).forEach((f) => expect(f).not.toHaveBeenCalled()))
  })
})

describe('hooks du parcours : actions', () => {
  const actions: [string, () => { mutateAsync: (v: never) => Promise<unknown> }, unknown, 'parcours' | 'decision' | 'portailParcours', string, unknown[]][] = [
    ['useDesignerConducteur', H.useDesignerConducteur, { liaisonId: 'L1', conducteurId: 'U2', motif: 'm' }, 'parcours', 'designerConducteur', ['L1', 'U2', 'm']],
    ['useAffecterRessources', H.useAffecterRessources, { liaisonId: 'L1', affectations: [{ fonction: 'SNA', personneId: 'U' }] }, 'parcours', 'affecterRessources', ['L1', [{ fonction: 'SNA', personneId: 'U' }]]],
    ['useDeposerLivrable', H.useDeposerLivrable, { operationId: 'L1:7', fichier, commentaire: 'c' }, 'parcours', 'deposerLivrable', ['L1:7', fichier, 'c', undefined]],
    ['useAttribuer', H.useAttribuer, { operationId: 'L1:7', qualite: 'C', personneId: 'U', motif: 'm' }, 'parcours', 'attribuer', ['L1:7', 'C', 'U', 'm']],
    ['useDeposerPreuveParcours', H.useDeposerPreuveParcours, { operationId: 'L1:C1', fichier, codeControle: 'ABCD' }, 'parcours', 'deposerPreuve', ['L1:C1', fichier, { valeurReference: undefined, codeControle: 'ABCD' }]],
    ['useValiderOperationParcours', H.useValiderOperationParcours, { operationId: 'L1:C1', assertion: 'a' }, 'parcours', 'validerOperation', ['L1:C1', 'a']],
    ['useDeclarerBlocage', H.useDeclarerBlocage, { liaisonId: 'L1', motif: 'm', prochaineAction: 'p', responsableId: 'U', echeanceLevee: '2026-10-10' }, 'parcours', 'declarerBlocage', ['L1', { motif: 'm', prochaineAction: 'p', responsableId: 'U', echeanceLevee: '2026-10-10' }]],
    ['useLeverBlocage', H.useLeverBlocage, { blocageId: 'B1', commentaire: 'c' }, 'parcours', 'leverBlocage', ['B1', 'c']],
    ['useConstaterCouverture', H.useConstaterCouverture, 'L1', 'parcours', 'constaterCouverture', ['L1']],
    ['useOuvrirServiceSupplementaire', H.useOuvrirServiceSupplementaire, { liaisonId: 'L1', services: ['RETRAIT'] }, 'parcours', 'ouvrirServiceSupplementaire', ['L1', ['RETRAIT']]],
    ['useEtablirProcesVerbal', H.useEtablirProcesVerbal, 'L1', 'decision', 'etablirProcesVerbal', ['L1']],
    ['useSignerProcesVerbalGimac', H.useSignerProcesVerbalGimac, { liaisonId: 'L1', assertion: 'a' }, 'decision', 'signerProcesVerbalGimac', ['L1', 'a']],
    ['useConstituerDossierChangement', H.useConstituerDossierChangement, { liaisonId: 'L1', fichier }, 'decision', 'constituerDossierChangement', ['L1', fichier]],
    ['useReferencerFicheChangement', H.useReferencerFicheChangement, { liaisonId: 'L1', reference: 'CHG-2026-1' }, 'decision', 'referencerFicheChangement', ['L1', 'CHG-2026-1']],
    ['useRelireFicheChangement', H.useRelireFicheChangement, 'L1', 'decision', 'relireFicheChangement', ['L1']],
    ['useVerifierAttestation', H.useVerifierAttestation, { liaisonId: 'L1', type: 'GSMA', conforme: false, motif: 'm' }, 'decision', 'verifierAttestation', ['L1', 'GSMA', false, 'm']],
    ['useArchiverAttestations', H.useArchiverAttestations, 'L1', 'decision', 'archiverAttestations', ['L1']],
    ['useSignerVisa', H.useSignerVisa, { liaisonId: 'L1', nature: 'CONFORMITE', favorable: true, motif: 'm', assertion: 'a' }, 'decision', 'signerVisa', ['L1', 'CONFORMITE', true, 'm', 'a']],
    ['useRemettreFicheSuivi', H.useRemettreFicheSuivi, 'L1', 'decision', 'remettreFicheSuivi', ['L1']],
    ['useConstaterMiseEnService', H.useConstaterMiseEnService, 'L1', 'decision', 'constaterMiseEnService', ['L1']],
    ['useCommuniquerCoordonnees', H.useCommuniquerCoordonnees, { liaisonId: 'L1', adresse: 'h', port: '8443' }, 'portailParcours', 'communiquerCoordonnees', ['L1', { adresse: 'h', port: '8443', pointsTerminaison: undefined }]],
    ['useDeposerLivrablePortail', H.useDeposerLivrablePortail, { operationId: 'L1:21', fichier, commentaire: 'c' }, 'portailParcours', 'deposerLivrable', ['L1:21', fichier, 'c']],
    ['useSignerProcesVerbalPortail', H.useSignerProcesVerbalPortail, { liaisonId: 'L1', assertion: 'a' }, 'portailParcours', 'signerProcesVerbal', ['L1', 'a']],
    ['useDeposerAttestation', H.useDeposerAttestation, { liaisonId: 'L1', type: 'COBAC', fichier }, 'portailParcours', 'deposerAttestation', ['L1', 'COBAC', fichier]],
    ['useRenseignerFicheSuivi', H.useRenseignerFicheSuivi, { liaisonId: 'L1', lignes: [{ serviceCode: 'RETRAIT', ouvert: true, commentaire: null }] }, 'portailParcours', 'renseignerFicheSuivi', ['L1', [{ serviceCode: 'RETRAIT', ouvert: true, commentaire: null }]]],
  ]
  it.each(actions)('%s : méthode du port, arguments, puis invalidation de « parcours » et « homologation »', async (_nom, hook, variables, sujet, methode, args) => {
    const { services, invalider, wrapper } = monter()
    const { result } = renderHook(hook, { wrapper })
    await expect(result.current.mutateAsync(variables as never)).resolves.toEqual({ resultat: methode })
    expect(services[sujet][methode]).toHaveBeenCalledWith(...args)
    expect(invalider.mock.calls.map(([f]) => f?.queryKey)).toEqual([['parcours'], ['homologation']])
  })

  // D-98 : l'ouverture fait passer l'institution adhérente en homologation (MS-PARTICIPANT) : le registre est relu aussi.
  it('useOuvrirLiaison : méthode du port, arguments, puis invalidation de « parcours », « homologation » et « participant »', async () => {
    const { services, invalider, wrapper } = monter()
    const { result } = renderHook(H.useOuvrirLiaison, { wrapper })
    const o = { institutionId: 'I', typeProjet: 'DELEGATAIRE', chefDeProjetId: 'U' }
    await expect(result.current.mutateAsync(o as never)).resolves.toEqual({ resultat: 'ouvrirLiaison' })
    expect(services.parcours.ouvrirLiaison).toHaveBeenCalledWith(o)
    expect(invalider.mock.calls.map(([f]) => f?.queryKey)).toEqual([['parcours'], ['homologation'], ['participant']])
  })

  it('un refus du service n’invalide rien et remonte l’erreur', async () => {
    const { services, invalider, wrapper } = monter()
    services.decision.signerProcesVerbalGimac.mockRejectedValueOnce(Object.assign(new Error('refus'), { code: 'CER_PV_NOT_SIGNABLE' }))
    const { result } = renderHook(() => H.useSignerProcesVerbalGimac(), { wrapper })
    await expect(result.current.mutateAsync({ liaisonId: 'L1', assertion: 'a' })).rejects.toMatchObject({ code: 'CER_PV_NOT_SIGNABLE' })
    expect(invalider).not.toHaveBeenCalled()
  })

  it('une action rafraîchit les lectures du parcours déjà chargées (dossier et tableau de bord)', async () => {
    const { services, wrapper } = monter()
    const { result } = renderHook(() => ({ dossier: H.useDossierLiaison('L1'), tableau: H.useTableauActeur(['SNA']), lever: H.useLeverBlocage() }), { wrapper })
    await waitFor(() => expect(result.current.tableau.isSuccess && result.current.dossier.isSuccess).toBe(true))
    await result.current.lever.mutateAsync({ blocageId: 'B1', commentaire: 'levé' })
    await waitFor(() => expect(services.parcours.dossier).toHaveBeenCalledTimes(2))
    expect(services.parcours.tableauActeur).toHaveBeenCalledTimes(2)
  })
})
