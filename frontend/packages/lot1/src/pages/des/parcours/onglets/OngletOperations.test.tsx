import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { useParams } from 'react-router'
import axe from 'axe-core'
import { ApiError } from '@gimac/socle'
import { rendrePage, sessionTest } from '../../../../test/rendu'
import { servicesSimules } from '../../../../services/simule'
import type { Services } from '../../../../services/ports'
import { useDossierLiaison } from '../../../../hooks/parcours'
import { PROFILS } from '../../../../permissions'
import { OngletOperations } from './OngletOperations'

// Onglet « Opérations » du dossier de liaison, monté dans un banc d'essai (/des/parcours/:id) avec la
// session d'un profil : les actions n'apparaissent que si le service les permet et si le jeton les autorise.
afterEach(cleanup)

function Banc() {
  const { id = '' } = useParams()
  const d = useDossierLiaison(id)
  return d.data ? <OngletOperations dossier={d.data} /> : null
}
type O = { role?: string; services?: Services; langue?: 'fr' | 'en' | 'es'; theme?: 'light' | 'dark' }
const ouvrir = (chemin: string, o: O = {}) =>
  rendrePage(<Banc />, { chemin, motif: '/des/parcours/:id', langue: o.langue, theme: o.theme, services: o.services ?? servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-TEST', nom: 'Agent de test', profils: [o.role ?? 'ROLE_SNA'] } }), session: sessionTest(PROFILS[o.role ?? 'ROLE_SNA'], [o.role ?? 'ROLE_SNA']) })
const url = (liaison: string, rang?: string, extra = '') => `/des/parcours/${liaison}?onglet=operations${rang ? `&operation=${encodeURIComponent(`${liaison}:${rang}`)}` : ''}${extra}`
const tiroir = () => screen.findByRole('dialog', { name: /^(Opération|Operation|Operación) / })
const signatureRapide = (s: Services): Services => ({ ...s, reauth: { assertion: () => Promise.resolve('YXNzZXJ0aW9u') } })

describe('Onglet Opérations : vue de la liaison', () => {
  it('groupes par étape et par segment, compteurs, plateforme, marques ; alerte permanente de l\'opération 28', async () => {
    ouvrir(url('LIA-2026-005'), { role: 'ROLE_POLE_SYSTEME' })
    expect(await screen.findByRole('heading', { name: 'Mise en production' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Intégration' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Branche du type de projet · Switch interbancaire' })).toBeTruthy()
    expect(screen.getAllByRole('heading', { name: 'Tronc commun aval' })).toHaveLength(2)
    const banniere = screen.getByText('Mise à jour du script de dépôt : à ne pas omettre').closest('[role]')!
    expect(banniere.textContent).toContain('le participant ne reçoit pas ses fichiers')
    expect(screen.getAllByText('Oubli sans symptôme').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Code de contrôle').length).toBeGreaterThan(0)
    // La bannière reste visible quels que soient les filtres.
    fireEvent.change(screen.getByLabelText('Étape'), { target: { value: 'INTEGRATION' } })
    expect(screen.getByText('Mise à jour du script de dépôt : à ne pas omettre')).toBeTruthy()
    expect(screen.queryByRole('heading', { name: 'Mise en production' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir l\'opération 28' }))
    const d = await tiroir()
    expect(within(d).getByText('Mise à jour du script de dépôt des fichiers')).toBeTruthy()
    expect(within(d).getAllByText(/le participant ne reçoit pas ses fichiers/).length).toBeGreaterThan(0)
  })

  it('filtres dans l\'adresse : à faire, en retard, ma fonction, recherche ; effacement', async () => {
    ouvrir(url('LIA-2026-005', undefined, '&vue=a-faire'), { role: 'ROLE_POLE_SYSTEME' })
    await screen.findByText(/Opérations affichées : 2 sur/)
    fireEvent.change(screen.getByLabelText('Afficher'), { target: { value: 'retard' } })
    await screen.findByText(/Opérations affichées : \d+ sur/)
    fireEvent.change(screen.getByLabelText('Afficher'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('switch', { name: 'Ma fonction seulement' }))
    expect(await screen.findByText(/Opérations affichées : 3 sur/)).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Rechercher (rang ou libellé)'), { target: { value: 'zzz' } })
    expect(await screen.findByText('Aucune opération ne répond à ces filtres.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Effacer les filtres' }))
    expect(screen.queryByText('Aucune opération ne répond à ces filtres.')).toBeNull()
  })

  it('lien profond vers une opération inconnue : alerte et fermeture ; verrou avec ses prédécesseurs', async () => {
    ouvrir(`/des/parcours/LIA-2026-005?onglet=operations&operation=LIA-2026-005%3AX9`)
    expect(await screen.findByText('Opération introuvable')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Fermer' }))
    await waitFor(() => expect(screen.queryByText('Opération introuvable')).toBeNull())
    cleanup()
    ouvrir(url('LIA-2026-005', '30'))
    const d = await tiroir()
    expect(within(d).getAllByText('En attente d\'un prédécesseur').length).toBeGreaterThan(0)
    fireEvent.click(within(d).getByRole('button', { name: /^28 · / }))
    await waitFor(() => expect(screen.getByRole('dialog', { name: /^Opération / }).textContent).toContain('Mise à jour du script de dépôt des fichiers'))
    fireEvent.click(within(screen.getByRole('dialog', { name: /^Opération / })).getByRole('button', { name: 'Fermer' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('erreur de chargement : identifiant de corrélation et réessai', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    s.parcours.operations = vi.fn().mockRejectedValue(new ApiError(503, 'HOM_UNAVAILABLE', 'Service indisponible.', 'cid-503'))
    ouvrir(url('LIA-2026-005'), { services: s })
    expect((await screen.findByRole('alert')).textContent).toContain('cid-503')
  })
})

describe('Onglet Opérations : exécution', () => {
  it('opération 28 par le pôle système : preuve de trace déposée puis attente de validation', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const depot = vi.spyOn(s.parcours, 'deposerPreuve')
    ouvrir(url('LIA-2026-005', '28'), { role: 'ROLE_POLE_SYSTEME', services: s })
    const d = await tiroir()
    fireEvent.click(within(d).getByRole('button', { name: 'Déposer la preuve' }))
    expect(await within(d).findByText('Choisissez le fichier de preuve.')).toBeTruthy()
    fireEvent.change(d.querySelector('input[type="file"]')!, { target: { files: [new File(['x'], 'trace-script.png', { type: 'image/png' })] } })
    fireEvent.click(within(d).getByRole('button', { name: 'Déposer la preuve' }))
    const conf = await screen.findByRole('alertdialog', { name: 'Déposer la preuve' })
    expect(conf.textContent).toContain('trace-script.png')
    fireEvent.click(within(conf).getByRole('button', { name: 'Déposer la preuve' }))
    // D-109 : le rang 28 attend deux pièces (script mis à jour, premier fichier facultatif) : le livrable visé est transmis.
    await waitFor(() => expect(depot).toHaveBeenCalledWith('LIA-2026-005:28', expect.any(File), { valeurReference: undefined, codeControle: undefined, ceremonie: undefined, livrable: 'L1' }))
    expect(await screen.findByText(/Preuve déposée, en attente de la validation de Brice Ondo Mba/)).toBeTruthy()
  })

  it('opération 29 (secret) : trace seule, avertissement, aucun code de contrôle demandé', async () => {
    ouvrir(url('LIA-2026-005', '29'), { role: 'ROLE_POLE_SYSTEME' })
    const d = await tiroir()
    expect(within(d).getByText('Aucun secret dans le dossier')).toBeTruthy()
    expect(within(d).queryByLabelText(/Code de contrôle/)).toBeNull()
  })

  it('cérémonie de clés C1 : date, trois porteurs et trois codes ; une saisie trop longue est effacée', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const depot = vi.spyOn(s.parcours, 'deposerPreuve')
    ouvrir(url('LIA-2026-012', 'C1'), { role: 'ROLE_RSSI', services: s })
    const d = await tiroir()
    expect(within(d).getByText(/jamais la valeur d'une clé ou d'une composante/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Déposer la preuve' }))
    expect(await within(d).findByText('La date de tenue est exigée.')).toBeTruthy()
    expect(within(d).getAllByText('Chaque composante a un porteur.')).toHaveLength(3)
    const kcv1 = within(d).getByLabelText(/Code de contrôle de la composante 1/) as HTMLInputElement
    fireEvent.change(kcv1, { target: { value: '0123456789ABCDEF0123456789ABCDEF' } })
    expect(kcv1.value).toBe('')
    expect(within(d).getByText(/Saisie refusée et effacée/)).toBeTruthy()
    fireEvent.change(d.querySelector('input[type="file"]')!, { target: { files: [new File(['x'], 'pv-ceremonie.pdf', { type: 'application/pdf' })] } })
    fireEvent.change(within(d).getByLabelText(/Date de tenue/), { target: { value: '2026-09-28' } })
    ;[1, 2, 3].forEach((n) => {
      fireEvent.change(within(d).getByLabelText(new RegExp(`Porteur de la composante ${n}`)), { target: { value: `Porteur ${n}` } })
      fireEvent.change(within(d).getByLabelText(new RegExp(`Code de contrôle de la composante ${n}`)), { target: { value: `a1b2c${n}` } })
    })
    fireEvent.click(within(d).getByRole('button', { name: 'Déposer la preuve' }))
    const conf = await screen.findByRole('alertdialog', { name: 'Déposer la preuve' })
    fireEvent.click(within(conf).getByRole('button', { name: 'Déposer la preuve' }))
    await waitFor(() =>
      expect(depot).toHaveBeenCalledWith('LIA-2026-012:C1', expect.any(File), expect.objectContaining({ ceremonie: { tenueLe: '2026-09-28', porteurs: ['Porteur 1', 'Porteur 2', 'Porteur 3'], codesControle: ['A1B2C1', 'A1B2C2', 'A1B2C3'] } })),
    )
  })

  it('valeur de référence C2 exigée ; refus du service affiché dans le dialogue avec sa corrélation', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    s.parcours.deposerPreuve = vi.fn().mockRejectedValue(new ApiError(422, 'HOM_REFERENCE_VALUE_REQUIRED', 'x', 'cid-422'))
    ouvrir(url('LIA-2026-012', 'C2'), { role: 'ROLE_PRODUCTION_MONETIQUE', services: s })
    const d = await tiroir()
    fireEvent.change(d.querySelector('input[type="file"]')!, { target: { files: [new File(['x'], 'membre.png', { type: 'image/png' })] } })
    fireEvent.click(within(d).getByRole('button', { name: 'Déposer la preuve' }))
    expect(await within(d).findByText('Cette preuve doit porter la valeur de référence relevée.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/Code du membre/), { target: { value: 'M-0061' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Déposer la preuve' }))
    const conf = await screen.findByRole('alertdialog', { name: 'Déposer la preuve' })
    expect(conf.textContent).toContain('M-0061')
    fireEvent.click(within(conf).getByRole('button', { name: 'Déposer la preuve' }))
    await waitFor(() => expect(conf.textContent).toContain('cid-422'))
    expect(conf.textContent).toContain('Cette preuve doit porter la valeur de référence relevée.')
  })

  it('validation par le validateur (signature FIDO2), puis consultation de la preuve et de sa valeur de référence', async () => {
    const s = signatureRapide(servicesSimules({ latenceMs: 0 }))
    const o = (await s.parcours.operations('LIA-2026-012')).find((x) => x.rang === 'C2')!
    await s.parcours.deposerPreuve(o.id, new File(['x'], 'membre.png'), { valeurReference: 'M-0061' })
    const valider = vi.spyOn(s.parcours, 'validerOperation')
    ouvrir(url('LIA-2026-012', 'C2'), { role: 'ROLE_CONDUCTEUR_PROJET', services: s })
    const d = await tiroir()
    expect(within(d).getByText('M-0061')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Valider l\'opération' }))
    const sign = await waitFor(() => { const x = screen.getAllByRole('dialog').find((y) => within(y).queryByRole('button', { name: 'Utiliser ma clé de sécurité' })); if (!x) throw new Error('signature absente'); return x })
    fireEvent.click(within(sign).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    await waitFor(() => expect(valider).toHaveBeenCalledWith(o.id, 'YXNzZXJ0aW9u'))
    await waitFor(() => expect(within(screen.getByRole('dialog', { name: /^Opération / })).getByText(/Opération close le/)).toBeTruthy())
    fireEvent.click(within(screen.getByRole('dialog', { name: /^Opération / })).getByRole('tab', { name: 'Livrables et preuves' }))
    const p = screen.getByRole('dialog', { name: /^Opération / })
    expect(within(p).getByText('Code du membre')).toBeTruthy()
    expect(within(p).getByText('Empreinte SHA-256')).toBeTruthy()
  })

  it('sans la permission, aucun geste : le responsable est rappelé', async () => {
    ouvrir(url('LIA-2026-005', '28'), { role: 'ROLE_CONFORMITE' })
    const d = await tiroir()
    expect(within(d).queryByRole('button', { name: 'Déposer la preuve' })).toBeNull()
    expect(within(d).getByText(/Aucune action ne vous revient sur cette opération : elle incombe à la fonction Pôle système/)).toBeTruthy()
  })

  it('rang 16 mobile : taux du simulateur repris sans recalcul ; rang 17 : couverture constatée par le SNA', async () => {
    ouvrir(url('LIA-2026-038', '16'))
    const d = await tiroir()
    expect(within(d).getByText('Taux repris du simulateur, sans recalcul.')).toBeTruthy()
    expect(within(d).getAllByText('60 %').length).toBeGreaterThan(0)
    cleanup()
    const s = servicesSimules({ latenceMs: 0 })
    const constat = vi.spyOn(s.parcours, 'constaterCouverture')
    ouvrir(url('LIA-2026-022', '17'), { services: s })
    const d2 = await tiroir()
    expect(within(d2).getByText('Couverture de certification')).toBeTruthy()
    expect(within(d2).getByText(/le procès-verbal ne pourra pas être établi/)).toBeTruthy()
    fireEvent.click(within(d2).getByRole('button', { name: 'Constater la couverture' }))
    const conf = await screen.findByRole('alertdialog', { name: 'Constater la couverture' })
    fireEvent.click(within(conf).getByRole('button', { name: 'Constater la couverture' }))
    await waitFor(() => expect(constat).toHaveBeenCalledWith('LIA-2026-022'))
  })

  it('livrable de phase (rang 7) avec commentaire', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const livrable = vi.spyOn(s.parcours, 'deposerLivrable')
    ouvrir(url('LIA-2026-041', '7'), { services: s })
    const d = await tiroir()
    fireEvent.click(within(d).getByRole('button', { name: 'Déposer le livrable' }))
    expect(await within(d).findByText('Choisissez le fichier de preuve.')).toBeTruthy()
    fireEvent.change(d.querySelector('input[type="file"]')!, { target: { files: [new File(['x'], 'cr-lancement.pdf')] } })
    fireEvent.change(within(d).getByLabelText(/Commentaire/), { target: { value: ' relu ' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Déposer le livrable' }))
    const conf = await screen.findByRole('alertdialog', { name: 'Déposer le livrable' })
    fireEvent.click(within(conf).getByRole('button', { name: 'Déposer le livrable' }))
    // D-109 : compte rendu (L1) et feuille de présence facultative (L2) : le livrable visé est transmis.
    await waitFor(() => expect(livrable).toHaveBeenCalledWith('LIA-2026-041:7', expect.any(File), 'relu', 'L1', undefined))
  })

  it('attribution d\'une qualité, motivée', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const attribuer = vi.spyOn(s.parcours, 'attribuer')
    ouvrir(url('LIA-2026-005', '29'), { role: 'ROLE_CONDUCTEUR_PROJET', services: s })
    const d = await tiroir()
    fireEvent.click(within(d).getByRole('button', { name: 'Attribuer' }))
    expect(await within(d).findByText('Choisissez l\'intervenant.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Qualité/), { target: { value: 'V' } })
    fireEvent.change(within(d).getByLabelText(/^Fonction/), { target: { value: 'SNA' } })
    await waitFor(() => expect(within(d).getByRole('option', { name: 'Rachel Biyong Mengue' })).toBeTruthy())
    fireEvent.change(within(d).getByLabelText(/^Intervenant/), { target: { value: 'U-SNA-1' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Attribuer' }))
    const conf = await screen.findByRole('alertdialog', { name: 'Attribuer' })
    fireEvent.change(within(conf).getByRole('textbox'), { target: { value: 'Relecture croisée des essais' } })
    fireEvent.click(within(conf).getByRole('button', { name: 'Attribuer' }))
    await waitFor(() => expect(attribuer).toHaveBeenCalledWith('LIA-2026-005:29', 'V', 'U-SNA-1', 'Relecture croisée des essais'))
  })

  it('opérations d\'un autre écran ou d\'un externe : lecture seule et lien ; opération bloquée', async () => {
    ouvrir(url('LIA-2026-019', '20'), { role: 'ROLE_PRODUCTION_MONETIQUE' })
    const d = await tiroir()
    expect(within(d).getByRole('link', { name: 'Ouvrir dans l\'onglet Décision' }).getAttribute('href')).toBe('/des/parcours/LIA-2026-019?onglet=decision&operation=LIA-2026-019%3A20')
    cleanup()
    ouvrir(url('LIA-2026-040', '12'))
    const e = await tiroir()
    expect(within(e).getByText('Adresse et port d\'essai')).toBeTruthy()
    expect(within(e).getByText('participant-040.essai:8443')).toBeTruthy()
    cleanup()
    ouvrir(url('LIA-2026-030', '14'), { role: 'ROLE_POLE_RESEAU' })
    const b = await tiroir()
    expect(within(b).getByText('Opération suspendue par un blocage')).toBeTruthy()
    expect(within(b).queryByRole('button', { name: 'Déposer la preuve' })).toBeNull()
  })
})

describe('Onglet Opérations : lecture', () => {
  it('étapes closes repliées puis dépliées ; ouverture par la ligne ; détail, intervenants, cérémonie consultée', async () => {
    ouvrir(url('LIA-2026-005'), { role: 'ROLE_RSSI' })
    const deplier = (await screen.findAllByRole('button', { name: 'Afficher les 3 opérations' }))[0]
    expect(screen.queryByText('Arrêt du périmètre des services')).toBeNull()
    fireEvent.click(deplier)
    expect(screen.getByText('Arrêt du périmètre des services')).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: 'Replier' })[0])
    expect(screen.queryByText('Arrêt du périmètre des services')).toBeNull()
    fireEvent.click(screen.getByText('Cérémonie de clés : génération de la clé maîtresse de zone en trois composantes et remise au participant'))
    const d = await tiroir()
    fireEvent.click(within(d).getByRole('tab', { name: 'Livrables et preuves' }))
    expect(within(d).getByText('Cérémonie de clés')).toBeTruthy()
    expect(within(d).getByText('Composante 3')).toBeTruthy()
    expect(within(d).getByText(/elle ne porte aucune composante/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('tab', { name: 'Intervenants' }))
    expect(within(d).getByText(/validateur \(V\) est toujours distinct du responsable/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('tab', { name: 'Détail' }))
    expect(within(d).getByText('Plateforme d\'exécution')).toBeTruthy()
    expect(within(d).getByText('Switch interbancaire')).toBeTruthy()
  })

  it('opération 28 encore verrouillée : alerte d\'avertissement ; détail d\'une opération verrouillée ; essais sans simulateur', async () => {
    ouvrir(url('LIA-2026-012', '31'), { role: 'ROLE_PRODUCTION_MONETIQUE' })
    const d = await tiroir()
    fireEvent.click(within(d).getByRole('tab', { name: 'Détail' }))
    expect(within(d).getByText('Cette opération s\'ouvrira quand les opérations suivantes seront faites :')).toBeTruthy()
    expect(document.body.textContent).toContain('Mise à jour du script de dépôt : à ne pas omettre')
    cleanup()
    ouvrir(url('LIA-2026-022', '16'))
    const e = await tiroir()
    fireEvent.click(within(e).getByRole('tab', { name: 'Détail' }))
    expect(within(e).queryByText('Taux repris du simulateur, sans recalcul.')).toBeNull()
    expect(within(e).getAllByText('Service').length).toBeGreaterThan(0)
  })

  it('couverture non calculable : motif affiché, jamais 0 %', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const lire = s.parcours.dossier
    s.parcours.dossier = async (id) => ({ ...(await lire(id)), couvertureCertification: { taux: null, numerateur: 0, denominateur: 0, calculable: false, motifNonCalculable: 'aucun service inclus' } })
    ouvrir(url('LIA-2026-022', '17'), { services: s })
    const d = await tiroir()
    expect(within(d).getByText('Couverture non calculable')).toBeTruthy()
    expect(within(d).getByText('aucun service inclus')).toBeTruthy()
    expect(within(d).queryByText('0 %')).toBeNull()
  })
})

describe('Onglet Opérations : langues et accessibilité', () => {
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    ouvrir(url('LIA-2026-012', 'C1'), { role: 'ROLE_RSSI', langue })
    await tiroir()
    await waitFor(() => expect(document.body.textContent).not.toMatch(/\bparcours(Ops)?\.[a-zA-Z]/))
    expect(document.body.textContent).not.toContain('Déposer la preuve')
  })

  it('aucune violation axe sur la liste et le tiroir (sombre)', async () => {
    const { container } = ouvrir(url('LIA-2026-005', '28'), { role: 'ROLE_POLE_SYSTEME', theme: 'dark' })
    await tiroir()
    // Contraste non mesurable sous jsdom ; « region » relève de la page hôte (le banc n'a pas de <main>) ;
    // aria-allowed-role vise <aside role="dialog"> du Drawer du catalogue (écart signalé).
    const r = await axe.run(document.body, { rules: { 'color-contrast': { enabled: false }, 'aria-allowed-role': { enabled: false }, region: { enabled: false } } })
    void container
    expect(r.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(', ')}`)).toEqual([])
  })
})
