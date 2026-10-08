import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import { PERMISSIONS_REALM } from '../services/simule/catalogue'
import { FONCTIONNALITES_DEMO, catalogueDroits, permissionsCatalogue } from '@gimac/lot1'
import ProfilsPage from './ProfilsPage'
import MatricePouvoirsPage from './MatricePouvoirsPage'
import OrganigrammePage from './OrganigrammePage'
import { LECTEUR, RSSI, SUPER_ADMIN, avec, confirmer, rendrePage, signer, simules } from '../test/rendu'
import { configurationHttp } from '../services/http/configuration'

// D-86 (profils-organisation.md) : libellé et description réels, modification signée, matrice avec les actions des
// processus d'unité, organigramme (glisser-déposer, gestes au clavier, pouvoirs d'un compte). D-90
// (roles-fonctionnalites.md) : fiche d'un profil à onglets, arbre des fonctionnalités, comparaison de deux profils.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const profils = (o: Partial<Parameters<typeof rendrePage>[1]> = {}) => rendrePage(<ProfilsPage />, { chemin: '/console/profils', motif: '/console/profils', ...o })
const organigramme = (o: Partial<Parameters<typeof rendrePage>[1]> = {}) =>
  rendrePage(<OrganigrammePage />, { chemin: '/console/organigramme', motif: '/console/organigramme', permissions: RSSI, ...o })

describe('D-90 : catalogue des droits et permissions du domaine', () => {
  it('chaque permission du catalogue des permissions (hors INTERNAL_*) appartient à au moins un droit', () => {
    const couvertes = permissionsCatalogue(catalogueDroits(FONCTIONNALITES_DEMO))
    // Écart signalé : HOM_PROJECT_CONDUCT (D-94 §6.5) n'appartient encore à aucun droit des valeurs de départ de MS-IAM.
    expect(PERMISSIONS_REALM.filter((p) => !p.startsWith('INTERNAL_') && !couvertes.has(p))).toEqual(['HOM_PROJECT_CONDUCT'])
  })
})

describe('D-86 : présentation des profils', () => {
  it('libellé et description du serveur, modification signée (PUT …/profils/{code}), dernière modification affichée', async () => {
    const s = simules()
    const modifier = vi.spyOn(s.configuration, 'modifierProfil')
    profils({ services: s })
    const ligne = (await screen.findByText('ROLE_POLE_RESEAU')).closest('tr')!
    expect(within(ligne).getByText('Pôle réseau')).toBeTruthy()
    expect(within(ligne).getByText(/Réalise les opérations réseau/)).toBeTruthy()
    expect(within(ligne).getByText('non modifié')).toBeTruthy()
    // Un profil livré n'a jamais de bouton « Supprimer ».
    expect(within(ligne).queryByRole('button', { name: /Supprimer/ })).toBeNull()
    fireEvent.click(within(ligne).getByRole('button', { name: 'Modifier le libellé et la description du profil Pôle réseau' }))
    const d = await screen.findByRole('dialog', { name: 'Modifier le profil' })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: '  ' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    expect(within(d).getByText('Libellé obligatoire, 80 caractères au plus.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Pôle réseau et télécoms' } })
    fireEvent.change(within(d).getByLabelText(/^Description/), { target: { value: 'x'.repeat(501) } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    expect(within(d).getByText('Description de 500 caractères au plus.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Description/), { target: { value: 'Liens, routage et télécoms du GIMAC.' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    const sig = await signer('Évolution du périmètre du pôle')
    expect(await within(sig).findByText('Signature confirmée')).toBeTruthy()
    expect(modifier).toHaveBeenCalledWith('ROLE_POLE_RESEAU', { libelle: 'Pôle réseau et télécoms', description: 'Liens, routage et télécoms du GIMAC.' }, 'Évolution du périmètre du pôle', expect.any(String))
    fireEvent.click(within(sig).getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('Profil « Pôle réseau et télécoms » enregistré.')).toBeTruthy()
    const apres = (await screen.findByText('Pôle réseau et télécoms')).closest('tr')!
    expect(within(apres).getByText('Serge Mouelle Kombi')).toBeTruthy()
  })

  it('profil privilégié en lecture pour le RSSI ; refus du service restitué ; libellé égal au code : catalogue de l’interface', async () => {
    profils({ permissions: RSSI, services: simules({ acteur: { id: 'u-rssi', nom: 'RSSI', permissions: RSSI } }) })
    const ligne = (await screen.findByText('ROLE_SUPER_ADMIN')).closest('tr')!
    expect(within(ligne).queryByRole('button', { name: /libellé et la description/ })).toBeNull()
    cleanup()
    const s = simules()
    const base = s.configuration.profils
    s.configuration.profils = async () => (await base()).map((p) => (p.code === 'ROLE_SNA' ? { ...p, libelle: 'ROLE_SNA' } : p))
    vi.spyOn(s.configuration, 'modifierProfil').mockRejectedValueOnce(new ApiError(400, 'IAM_PROFIL_INVALIDE', 'x', 'cid-p', { champ: 'libelle' }))
    profils({ services: s })
    const sna = (await screen.findByText('ROLE_SNA')).closest('tr')!
    expect(within(sna).getByText('Normes et Agréments')).toBeTruthy()
    fireEvent.click(within(sna).getByRole('button', { name: /libellé et la description du profil Normes et Agréments/ }))
    const d = await screen.findByRole('dialog', { name: 'Modifier le profil' })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Normes' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    const sig = await signer('Libellé raccourci')
    expect(await within(sig).findByText(/Libellé obligatoire \(80 caractères au plus\)/)).toBeTruthy()
  })

  it('modification réservée : simulé refuse un profil privilégié sans octroi et un libellé vide', async () => {
    const s = simules({ acteur: { id: 'u-rssi', nom: 'RSSI', permissions: ['IAM_PROFILE_EDIT'] } })
    const a = await s.reauth.assertion('ADMIN_SENSITIVE')
    await expect(s.configuration.modifierProfil('ROLE_SUPER_ADMIN', { libelle: 'X', description: '' }, 'motif', a)).rejects.toMatchObject({ code: 'IAM_PRIVILEGED_PROFILE' })
    await expect(s.configuration.modifierProfil('ROLE_SNA', { libelle: ' ', description: '' }, 'motif', a)).rejects.toMatchObject({ code: 'IAM_PROFIL_INVALIDE' })
    await expect(s.configuration.modifierProfil('ROLE_INCONNU', { libelle: 'X', description: '' }, 'motif', a)).rejects.toMatchObject({ code: 'IAM_PROFILE_NOT_FOUND' })
    await expect(s.configuration.modifierProfil('ROLE_SNA', { libelle: 'X', description: '' }, 'motif', '')).rejects.toMatchObject({ code: 'IAM_REAUTH_REQUIRED' })
    await expect(s.configuration.modifierProfil('ROLE_SNA', { libelle: 'X', description: '' }, '', a)).rejects.toMatchObject({ code: 'IAM_REASON_REQUIRED' })
    const lecteur = simules({ acteur: { id: 'u-l', nom: 'L', permissions: LECTEUR } })
    await expect(lecteur.configuration.modifierProfil('ROLE_SNA', { libelle: 'X', description: '' }, 'motif', a)).rejects.toMatchObject({ code: 'IAM_PERMISSION_REQUISE' })
    const p = await s.configuration.modifierProfil('ROLE_SNA', { libelle: '  Normes  ', description: ' Mission ' }, 'motif', a)
    expect(p).toMatchObject({ libelle: 'Normes', description: 'Mission', modifiePar: { id: 'u-rssi' } })
    expect((await s.pouvoirs.matrice()).profils.find((l) => l.profil === 'ROLE_SNA')?.libelle).toBe('Normes')
  })

  it('HTTP : PUT /configuration/profils/{code} avec libellé, description, motif et assertion', async () => {
    const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))
    await configurationHttp('console').modifierProfil('ROLE_SNA', { libelle: 'Normes', description: 'Mission' }, 'Revue', 'A9')
    await configurationHttp('des').creerProfil({ code: 'ROLE_X', libelle: 'X', description: 'D', permissions: [] }, 'Création', 'A8')
    const [u, i] = spy.mock.calls[0] as [string, RequestInit]
    expect(`${i.method} ${new URL(u).pathname}`).toBe('PUT /bff/console/iam/configuration/profils/ROLE_SNA')
    expect(JSON.parse(String(i.body))).toEqual({ libelle: 'Normes', description: 'Mission', motif: 'Revue' })
    expect(new Headers(i.headers).get('X-Reauth-Assertion')).toBe('A9')
    expect(JSON.parse(String((spy.mock.calls[1][1] as RequestInit).body))).toEqual({ code: 'ROLE_X', libelle: 'X', description: 'D', permissions: [], motif: 'Création' })
  })
})

describe('D-90 : fiche d’un profil, fonctionnalités, comparaison', () => {
  it('onglets Fonctionnalités (arbre, aperçu de la barre latérale), Pouvoirs et Organisation ; enregistrement signé', async () => {
    const s = simules()
    const definir = vi.spyOn(s.configuration, 'definirPermissions')
    profils({ services: s })
    const ligne = (await screen.findByText('ROLE_SNA')).closest('tr')!
    fireEvent.click(within(ligne).getByRole('button', { name: 'Modifier la composition' }))
    const d = await screen.findByRole('dialog', { name: 'Profil Normes et Agréments' })
    const apercu = within(d).getByRole('complementary', { name: 'Aperçu de la barre latérale' })
    expect(within(apercu).queryByText('Mon équipe')).toBeNull()
    fireEvent.click(await within(d).findByRole('checkbox', { name: 'Mon équipe' }))
    expect(within(apercu).getByText('Mon équipe')).toBeTruthy()
    expect(await within(apercu).findByText('(ajoutée)')).toBeTruthy()
    fireEvent.click(within(d).getByRole('checkbox', { name: 'Mon équipe' }))
    expect((within(d).getByRole('button', { name: 'Continuer vers la signature' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(within(d).getByRole('button', { name: /^Droits de Référentiels d.opérations$/ }))
    fireEvent.click(within(d).getByRole('checkbox', { name: /HOM_REFERENTIEL_PUBLISH/ }))
    expect((within(d).getByRole('checkbox', { name: /HOM_REFERENTIEL_PUBLISH/ }) as HTMLInputElement).checked).toBe(true)
    // Décocher la consultation retire aussi les droits d'écriture.
    fireEvent.click(within(d).getByRole('checkbox', { name: /\(HOM_REFERENTIEL_READ\)/ }))
    expect((within(d).getByRole('checkbox', { name: /HOM_REFERENTIEL_PUBLISH/ }) as HTMLInputElement).checked).toBe(false)
    fireEvent.click(within(d).getByRole('checkbox', { name: /HOM_REFERENTIEL_EDIT/ }))
    fireEvent.click(within(d).getByRole('tab', { name: 'Pouvoirs' }))
    expect(await within(d).findByText('Lancer un processus')).toBeTruthy()
    fireEvent.click(within(d).getByRole('tab', { name: 'Organisation' }))
    expect(await within(d).findByText('Rachel Biyong Mengue')).toBeTruthy()
    expect(within(d).getByText('Christian Ndong Obiang')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    const sig = await signer('Lecture des référentiels pour le SNA')
    expect(await within(sig).findByText('Signature confirmée')).toBeTruthy()
    expect(definir.mock.calls[0][1]).toEqual(expect.arrayContaining(['HOM_REFERENTIEL_READ', 'HOM_REFERENTIEL_EDIT']))
    expect(definir.mock.calls[0][1]).not.toContain('HOM_REFERENTIEL_PUBLISH')
  })

  it('liens de la fiche vers la matrice (ligne en évidence) et vers l’organigramme', async () => {
    profils()
    fireEvent.click(within((await screen.findByText('ROLE_CHEF_DES')).closest('tr')!).getByRole('button', { name: 'Modifier la composition' }))
    const d = await screen.findByRole('dialog')
    fireEvent.click(within(d).getByRole('tab', { name: 'Pouvoirs' }))
    fireEvent.click(await within(d).findByRole('button', { name: 'Ouvrir dans la matrice des pouvoirs' }))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/matrice-pouvoirs?profil=ROLE_CHEF_DES')
    cleanup()
    profils()
    fireEvent.click(within((await screen.findByText('ROLE_CHEF_DES')).closest('tr')!).getByRole('button', { name: 'Modifier la composition' }))
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('tab', { name: 'Organisation' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir l’organigramme' }))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/organigramme')
  })

  it('comparaison de deux profils : différences seulement, aperçus des barres latérales', async () => {
    profils({ permissions: LECTEUR })
    await screen.findByText('ROLE_SNA')
    fireEvent.click(screen.getByRole('button', { name: 'Comparer deux profils' }))
    const d = await screen.findByRole('dialog', { name: 'Comparer deux profils' })
    fireEvent.change(within(d).getByLabelText('Premier profil'), { target: { value: 'ROLE_SNA' } })
    fireEvent.change(within(d).getByLabelText('Second profil'), { target: { value: 'ROLE_CHEF_DES' } })
    expect(within(d).getByText(/droits diffèrent/)).toBeTruthy()
    const lignes = within(d).getAllByRole('row').length
    fireEvent.click(within(d).getByLabelText('Seulement les différences'))
    expect(within(d).getAllByRole('row').length).toBeLessThan(lignes)
    expect(within(d).getAllByRole('complementary')).toHaveLength(2)
    fireEvent.change(within(d).getByLabelText('Second profil'), { target: { value: 'ROLE_SNA' } })
    expect(within(d).getByText('0 droit diffère')).toBeTruthy()
    fireEvent.click(within(d).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('création par fonctionnalité avec libellé ; erreurs de libellé et de description', async () => {
    const s = simules()
    const creer = vi.spyOn(s.configuration, 'creerProfil')
    profils({ services: s })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau profil' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau profil' })
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'ROLE_VEILLE' } })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'x'.repeat(81) } })
    fireEvent.change(within(d).getByLabelText(/^Description/), { target: { value: 'y'.repeat(501) } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    expect(within(d).getByText('Libellé obligatoire, 80 caractères au plus.')).toBeTruthy()
    expect(within(d).getByText('Description de 500 caractères au plus.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Veille' } })
    fireEvent.change(within(d).getByLabelText(/^Description/), { target: { value: 'Veille des liaisons' } })
    fireEvent.click(within(d).getByRole('checkbox', { name: 'Supervision' }))
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    const sig = await signer('Création de la veille')
    expect(await within(sig).findByText('Signature confirmée')).toBeTruthy()
    expect(creer.mock.calls[0][0]).toEqual({ code: 'ROLE_VEILLE', libelle: 'Veille', description: 'Veille des liaisons', permissions: ['PLATFORM_SUPERVISION_READ'] })
  })
})

describe('D-86 §4 : matrice avec les actions des processus d’unité', () => {
  it('colonnes « Processus : définir » et « Processus : lancer », libellé de la ligne, ligne en évidence', async () => {
    rendrePage(<MatricePouvoirsPage />, { chemin: '/console/matrice-pouvoirs?profil=ROLE_CHEF_DES', motif: '/console/matrice-pouvoirs' })
    expect(await screen.findByRole('columnheader', { name: 'Processus : définir' })).toBeTruthy()
    expect(screen.getByRole('columnheader', { name: 'Processus : lancer' })).toBeTruthy()
    const ligne = (await screen.findByText('ROLE_CHEF_DES')).closest('tr')!
    expect(ligne.getAttribute('aria-selected')).toBe('true')
    fireEvent.click(within(ligne).getByRole('button', { name: 'Pouvoirs du profil Chef du DES' }))
    const d = await screen.findByRole('dialog', { name: 'Pouvoirs du profil Chef du DES' })
    expect((within(d).getByLabelText(/^Définir les processus de l’unité/) as HTMLSelectElement).value).toBe('UNITES')
  })

  it('ligne sans les actions des processus (représentation antérieure) : AUCUNE', async () => {
    const s = simules()
    s.pouvoirs.matrice = async () => ({ profils: [{ profil: 'ROLE_SNA', actions: { TACHE_CONFIER: 'DIRECTS' } as never, profilsDemandables: [] }] })
    const modifier = vi.spyOn(s.pouvoirs, 'modifierMatrice').mockResolvedValue({ profil: 'ROLE_SNA', actions: {} as never, profilsDemandables: [] })
    rendrePage(<MatricePouvoirsPage />, { chemin: '/console/matrice-pouvoirs', motif: '/console/matrice-pouvoirs', services: s })
    fireEvent.click(await screen.findByRole('button', { name: 'Pouvoirs du profil Normes et Agréments' }))
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByLabelText(/^Lancer un processus/), { target: { value: 'DIRECTS' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    await signer('Lancement par le SNA')
    await waitFor(() => expect(modifier).toHaveBeenCalled())
    expect(modifier.mock.calls[0][1].actions).toMatchObject({ TACHE_CONFIER: 'DIRECTS', PROCESSUS_LANCER: 'DIRECTS', PROCESSUS_DEFINIR: 'AUCUNE', STATUT_DEMANDER: 'AUCUNE' })
  })
})

/** Glisse un élément sur une cible (glisser-déposer HTML natif). */
function glisser(source: Element, cible: Element) {
  const donnees = new Map<string, string>()
  const dataTransfer = { setData: (k: string, v: string) => donnees.set(k, v), getData: (k: string) => donnees.get(k) ?? '', effectAllowed: '', dropEffect: '' }
  fireEvent.dragStart(source, { dataTransfer })
  fireEvent.dragOver(cible, { dataTransfer })
  fireEvent.drop(cible, { dataTransfer })
  fireEvent.dragEnd(source, { dataTransfer })
}
const unite = (code: string) => document.querySelector(`[data-unite="${code}"]`)!
const compte = (id: string) => document.querySelector(`[data-compte="${id}"]`)!

describe('D-86 §3 : organigramme', () => {
  it('arbre des unités (chef, effectif), membres en arbre des n+1, panneau « Pouvoirs » et lien vers la matrice', async () => {
    organigramme()
    const arbre = await screen.findByRole('list', { name: 'Unités' })
    expect(within(arbre).getByText('Département Exploitation et Supervision')).toBeTruthy()
    await waitFor(() => expect(unite('DES').textContent).toMatch(/Chef : Christian Ndong Obiang · 4 personnes/))
    fireEvent.click(unite('SNA'))
    const membres = await screen.findByRole('group', { name: 'Membres de Service Normes et Agréments' })
    expect(within(membres).getByText('Patrick Mbida Essomba')).toBeTruthy()
    expect(within(membres).getAllByText(/n\+1 : Christian Ndong Obiang/).length).toBe(2)
    expect(screen.getByRole('heading', { name: 'Comptes sans unité' })).toBeTruthy()
    fireEvent.click(unite('DES'))
    fireEvent.click(await screen.findByRole('button', { name: 'Pouvoirs de Christian Ndong Obiang' }))
    const panneau = await screen.findByRole('region', { name: 'Pouvoirs de Christian Ndong Obiang' })
    expect(within(panneau).getByText('Confier une tâche')).toBeTruthy()
    expect(within(panneau).getAllByText(/personnes couvertes/).length).toBeGreaterThan(0)
    fireEvent.click(within(panneau).getByRole('button', { name: 'Ligne de la matrice des pouvoirs : Chef du DES' }))
    expect(screen.getByTestId('emplacement').textContent).toBe('/console/matrice-pouvoirs?profil=ROLE_CHEF_DES')
  })

  it('glisser une unité sur une autre la déplace, un compte sur une unité l’y rattache, sur un compte en fait son n+1 (confirmation motivée)', async () => {
    const s = simules()
    const modifier = vi.spyOn(s.organisation, 'modifierUnite')
    const rattacher = vi.spyOn(s.organisation, 'rattacher')
    organigramme({ services: s })
    await screen.findByRole('list', { name: 'Unités' })
    await waitFor(() => expect(document.querySelector('[data-compte="u-sans-2fa"]')).toBeTruthy())
    // Une unité ne se dépose ni sur elle-même ni sur l'une de ses sous-unités.
    glisser(unite('PROD_INFO'), unite('POLE_RESEAU'))
    expect(screen.queryByRole('alertdialog')).toBeNull()
    glisser(unite('POLE_SYSTEME'), unite('SNA'))
    let c = await screen.findByRole('alertdialog', { name: 'Déplacer l’unité' })
    expect(within(c).getByText('Service Production informatique → Service Normes et Agréments')).toBeTruthy()
    await confirmer('Déplacer l’unité', 'Réorganisation du pôle système')
    await waitFor(() => expect(modifier).toHaveBeenCalledWith('POLE_SYSTEME', expect.objectContaining({ parent: 'SNA' })))
    expect(await screen.findByText('Unité déplacée.')).toBeTruthy()
    glisser(compte('u-sans-2fa'), unite('POLE_SYSTEME'))
    c = await screen.findByRole('alertdialog', { name: 'Rattacher à l’unité' })
    await confirmer('Rattacher à l’unité', 'Affectation au pôle système')
    await waitFor(() => expect(rattacher).toHaveBeenCalledWith('u-sans-2fa', { unite: 'POLE_SYSTEME', responsableId: null }))
    fireEvent.click(unite('SNA'))
    await screen.findByRole('group', { name: 'Membres de Service Normes et Agréments' })
    glisser(compte('u-sna'), compte('u-ingenieur'))
    await screen.findByRole('alertdialog', { name: 'Changer le n+1' })
    await confirmer('Changer le n+1', 'Encadrement par l’ingénieur')
    await waitFor(() => expect(rattacher).toHaveBeenCalledWith('u-sna', { unite: 'SNA', responsableId: 'u-ingenieur' }))
    // Désigner le chef en déposant un compte sur la case « Chef d'unité ».
    glisser(compte('u-ingenieur'), screen.getByText('Déposez ici un compte pour le désigner chef de l’unité.').closest('.gm-org-zone')!)
    await screen.findByRole('alertdialog', { name: 'Désigner le chef d’unité' })
    await confirmer('Désigner le chef d’unité', 'Chef du service SNA')
    await waitFor(() => expect(modifier).toHaveBeenCalledWith('SNA', expect.objectContaining({ responsableId: 'u-ingenieur' })))
  })

  it('vue liste au clavier : déplacer, désigner le chef, changer le n+1 et l’unité ; refus restitué ; lecture seule', async () => {
    const s = simules()
    vi.spyOn(s.organisation, 'rattacher').mockRejectedValueOnce(new ApiError(409, 'ORG_CYCLE', 'x', 'cid-c'))
    organigramme({ services: s })
    fireEvent.click(await screen.findByRole('button', { name: 'Liste' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Déplacer l’unité Pôle réseau' }))
    let d = await screen.findByRole('dialog', { name: 'Déplacer l’unité' })
    fireEvent.change(within(d).getByLabelText(/^Nouvelle unité parente/), { target: { value: 'DES' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    await confirmer('Déplacer l’unité', 'Rattachement direct au département')
    expect(await screen.findByText('Unité déplacée.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Désigner le chef de Pôle réseau' }))
    d = await screen.findByRole('dialog', { name: 'Désigner le chef d’unité' })
    fireEvent.change(within(d).getByLabelText(/^Chef d’unité/), { target: { value: 'u-sna' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    await confirmer('Désigner le chef d’unité', 'Cheffe du pôle réseau')
    expect(await screen.findByText('Chef d’unité désigné.')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Changer le n+1 de Rachel Biyong Mengue' }))
    d = await screen.findByRole('dialog', { name: 'Changer le n+1' })
    fireEvent.change(within(d).getByLabelText(/^Nouveau n\+1/), { target: { value: 'u-ingenieur' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    await confirmer('Changer le n+1', 'Encadrement par l’ingénieur')
    expect(await screen.findByText(/Boucle hiérarchique/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Changer l’unité de Rachel Biyong Mengue' }))
    d = await screen.findByRole('dialog', { name: 'Rattacher à l’unité' })
    fireEvent.change(within(d).getByLabelText(/^Unité/), { target: { value: 'POLE_RESEAU' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    await confirmer('Rattacher à l’unité', 'Renfort du pôle réseau')
    expect(await screen.findByText('Compte rattaché à l’unité.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Pouvoirs de Rachel Biyong Mengue' }))
    expect(await screen.findByRole('region', { name: 'Pouvoirs de Rachel Biyong Mengue' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir l’unité Pôle réseau' }))
    expect(await screen.findByRole('group', { name: 'Membres de Pôle réseau' })).toBeTruthy()
    cleanup()
    organigramme({ permissions: LECTEUR })
    await screen.findByRole('list', { name: 'Unités' })
    expect(screen.queryByRole('button', { name: 'Nouvelle unité' })).toBeNull()
    expect(unite('DES').getAttribute('draggable')).toBe('false')
  })

  it('unités : nouvelle sous-unité, renommer, désactiver et réactiver depuis le panneau', async () => {
    organigramme({ permissions: SUPER_ADMIN })
    await screen.findByText('Pôle système')
    fireEvent.click(unite('POLE_SYSTEME'))
    fireEvent.click(await screen.findByRole('button', { name: 'Sous-unité' }))
    let d = await screen.findByRole('dialog', { name: 'Nouvelle unité' })
    expect((within(d).getByLabelText(/^Unité parente/) as HTMLSelectElement).value).toBe('POLE_SYSTEME')
    fireEvent.change(within(d).getByLabelText(/^Code/), { target: { value: 'CELLULE_STOCKAGE' } })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Cellule stockage' } })
    fireEvent.change(within(d).getByLabelText('Type'), { target: { value: 'CELLULE' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer l’unité' }))
    expect(await screen.findByText('Unité « Cellule stockage » créée.')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Renommer' }))
    d = await screen.findByRole('dialog', { name: 'Modifier l’unité' })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Cellule stockage et sauvegarde' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer l’unité' }))
    expect(await screen.findByText('Unité « Cellule stockage et sauvegarde » modifiée.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Désactiver l’unité' }))
    await confirmer('Désactiver l’unité', 'Cellule fusionnée avec le pôle')
    expect(await screen.findByText('Unité « Cellule stockage et sauvegarde » désactivée.')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Réactiver l’unité' }))
    await confirmer('Réactiver l’unité', 'Cellule rétablie par la direction')
    expect(await screen.findByText(/réactivée/)).toBeTruthy()
  })
})

describe('Comptes en grand nombre', () => {
  it('tous les comptes sont lus par pages de 100', async () => {
    const s = simules()
    const base = s.comptes.lister
    const lister = vi.fn(async (f: Parameters<typeof base>[0]) => ({ ...(await base(f)), total: 150 }))
    organigramme({ services: avec('comptes', { lister }, s) })
    await screen.findByRole('list', { name: 'Unités' })
    await waitFor(() => expect(lister).toHaveBeenCalledWith({ page: 2, taille: 100 }))
  })
})
