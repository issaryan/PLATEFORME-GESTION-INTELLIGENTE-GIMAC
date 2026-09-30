import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import PolitiqueSecuritePage from './PolitiqueSecuritePage'
import MessageriePage from './MessageriePage'
import ProfilsPage from './ProfilsPage'
import ConfigurationPlateformePage from './ConfigurationPlateformePage'
import JournalisationPage from './JournalisationPage'
import { RSSI, SUPER_ADMIN, avec, rendrePage, signer, simules } from '../test/rendu'
import type { ConfigurationMessagerie, PolitiqueSecurite } from '../modele/configuration'

afterEach(cleanup)
type O = Partial<Parameters<typeof rendrePage>[1]>
const politique = (o: O = {}) => rendrePage(<PolitiqueSecuritePage />, { chemin: '/p', motif: '/p', permissions: SUPER_ADMIN, ...o })
const messagerie = (o: O = {}) => rendrePage(<MessageriePage />, { chemin: '/m', motif: '/m', permissions: SUPER_ADMIN, ...o })
const profils = (o: O = {}) => rendrePage(<ProfilsPage />, { chemin: '/pr', motif: '/pr', permissions: SUPER_ADMIN, ...o })
const champ = (c: HTMLElement, nom: string, valeur: string) => fireEvent.change(c.querySelector(`[name="${nom}"]`)!, { target: { value: valeur } })

describe('Politique de sécurité', () => {
  it('16 champs, planchers rappelés, application signée avec motif', async () => {
    let recue: PolitiqueSecurite | undefined
    const base = simules()
    const { container } = politique({ services: avec('configuration', { appliquerPolitique: async (p, m, a) => { recue = p; return base.configuration.appliquerPolitique(p, m, a) } }, base) })
    expect(await screen.findByText('Mots de passe')).toBeTruthy()
    expect(container.querySelectorAll('input, select')).toHaveLength(16)
    expect(screen.getByText('De 12 à 128.')).toBeTruthy()
    expect(screen.getByText('De 1 à 1440.')).toBeTruthy()
    expect(screen.getByText('Obligatoire : ne peut pas être désactivée.')).toBeTruthy()
    champ(container, 'longueurMinimale', '16')
    champ(container, 'otpAlgorithme', 'HmacSHA512')
    champ(container, 'otpChiffres', '8')
    fireEvent.click(screen.getByLabelText('Interdire l’identifiant dans le mot de passe'))
    fireEvent.click(screen.getByRole('button', { name: 'Appliquer la politique' }))
    const d = await signer('Alignement sur la PSSI révisée en 2026')
    expect(await within(d).findByText('Signature confirmée')).toBeTruthy()
    expect(recue).toMatchObject({ longueurMinimale: 16, otpAlgorithme: 'HmacSHA512', otpChiffres: 8, interditIdentifiant: false })
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('La politique de sécurité a été appliquée.')).toBeTruthy()
  })

  it('IAM_POLICY_INVALID : erreur de chaque champ (details.champs) ; saisie non entière refusée localement', async () => {
    const { container } = politique()
    await screen.findByText('Mots de passe')
    champ(container, 'historique', 'abc')
    fireEvent.click(screen.getByRole('button', { name: 'Appliquer la politique' }))
    expect(await screen.findByText('Nombre entier attendu.')).toBeTruthy()
    expect(screen.queryByRole('dialog')).toBeNull()
    champ(container, 'historique', '5')
    champ(container, 'longueurMinimale', '8')
    champ(container, 'majusculesMin', '4')
    champ(container, 'minusculesMin', '4')
    champ(container, 'otpPeriodeSecondes', '10')
    fireEvent.click(screen.getByLabelText('Protection contre la force brute'))
    fireEvent.click(screen.getByRole('button', { name: 'Appliquer la politique' }))
    await signer()
    expect(await screen.findByText('Politique de sécurité refusée : corrigez les champs signalés.')).toBeTruthy()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.getByText('Valeur refusée par MS-IAM : 12 à 128.')).toBeTruthy()
    expect(screen.getByText('Valeur refusée par MS-IAM : 30 à 60.')).toBeTruthy()
    expect(screen.getByText('La somme des classes de caractères dépasse la longueur minimale.')).toBeTruthy()
    expect(screen.getByText('Valeur refusée par MS-IAM : obligatoire.')).toBeTruthy()
    expect(container.querySelector('[name="longueurMinimale"]')!.getAttribute('aria-invalid')).toBe('true')
  })

  it('RSSI sans droit de modification : consultation seule ; autre refus affiché dans le dialogue', async () => {
    const { container } = politique({ permissions: RSSI.filter((p) => p !== 'PLATFORM_SECURITY_POLICY_EDIT') })
    expect(await screen.findByText(/Consultation seule/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Appliquer la politique' })).toBeNull()
    expect(container.querySelector('[name="longueurMinimale"]')!.hasAttribute('readonly')).toBe(true)
    cleanup()
    politique({ services: avec('configuration', { appliquerPolitique: async () => { throw new ApiError(403, 'IAM_REAUTH_INVALID', 'x', 'RQ-P') } }) })
    fireEvent.click(await screen.findByRole('button', { name: 'Appliquer la politique' }))
    const d = await signer()
    expect(await within(d).findByText(/a été refusée : recommencez/)).toBeTruthy()
  })
})

describe('Messagerie', () => {
  it('mot de passe en écriture seule : vide conservé, saisi transmis ; motif obligatoire ; test', async () => {
    const recues: ConfigurationMessagerie[] = []
    const base = simules()
    const { container } = messagerie({ services: avec('configuration', { appliquerMessagerie: async (m, motif) => { recues.push(m); return base.configuration.appliquerMessagerie(m, motif) } }, base) })
    expect(await screen.findByText('mot de passe défini')).toBeTruthy()
    const secret = container.querySelector('input[name="motDePasse"]') as HTMLInputElement
    expect(secret.type).toBe('password')
    expect(secret.value).toBe('')
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer la messagerie' }))
    expect(await screen.findByText('Obligatoire, 10 caractères au moins ; il est inscrit au journal d’administration.', { selector: '.gm-field-error' })).toBeTruthy()
    champ(container, 'motifMessagerie', 'Changement de relais SMTP du groupement')
    champ(container, 'port', '465')
    fireEvent.click(screen.getByLabelText('SSL/TLS implicite'))
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer la messagerie' }))
    expect(await screen.findByText('La messagerie a été enregistrée.')).toBeTruthy()
    expect(recues[0]).toMatchObject({ port: 465, ssl: true, motDePasse: null, utilisateur: 'plateforme-des' })
    champ(container, 'motDePasse', 'n0uveau-Secret')
    champ(container, 'motifMessagerie', 'Rotation du secret SMTP trimestrielle')
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer la messagerie' }))
    await waitFor(() => expect(recues[1]).toMatchObject({ motDePasse: 'n0uveau-Secret' }))
    champ(container, 'destinataire', 'rssi@gimac-afr.org')
    fireEvent.click(screen.getByRole('button', { name: 'Envoyer un courriel de test' }))
    expect(await screen.findByText('Courriel de test envoyé à rssi@gimac-afr.org.')).toBeTruthy()
    champ(container, 'destinataire', 'invalide')
    fireEvent.click(screen.getByRole('button', { name: 'Envoyer un courriel de test' }))
    expect(await screen.findByText(/Configuration de messagerie refusée/)).toBeTruthy()
  })

  it('validation locale et refus du service', async () => {
    const services = avec('configuration', { appliquerMessagerie: async () => { throw new ApiError(400, 'IAM_SMTP_INVALID', 'x', 'RQ-SMTP') } })
    const { container } = messagerie({ services })
    await screen.findByText('mot de passe défini')
    champ(container, 'hote', ' ')
    champ(container, 'port', '70000')
    champ(container, 'expediteur', '')
    champ(container, 'utilisateur', '')
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer la messagerie' }))
    expect(await screen.findByText('Port de 1 à 65535.')).toBeTruthy()
    expect(screen.getAllByText('Champ obligatoire.')).toHaveLength(3)
    champ(container, 'hote', 'smtp.gimac-afr.org')
    champ(container, 'port', '587')
    champ(container, 'expediteur', 'des@gimac-afr.org')
    fireEvent.click(screen.getByLabelText('Le serveur exige une authentification'))
    expect((container.querySelector('input[name="utilisateur"]') as HTMLInputElement).disabled).toBe(true)
    champ(container, 'motifMessagerie', 'Motif de test suffisamment long')
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer la messagerie' }))
    expect(await screen.findByText('RQ-SMTP')).toBeTruthy()
  })
})

describe('Profils et permissions', () => {
  it('super administrateur : composition par domaine, INTERNAL exclu, enregistrement signé', async () => {
    const base = simules()
    const definir = vi.fn(base.configuration.definirPermissions)
    profils({ services: avec('configuration', { definirPermissions: definir }, base) })
    expect(await screen.findByText('ROLE_SUPER_ADMIN')).toBeTruthy()
    expect(screen.getAllByText('privilégié')).toHaveLength(2)
    const ligne = screen.getByText('ROLE_SNA').closest('tr')!
    fireEvent.click(within(ligne).getByRole('button', { name: 'Modifier la composition' }))
    const d = await screen.findByRole('dialog')
    expect(within(d).getByText('Homologation (HOM)')).toBeTruthy()
    expect(within(d).queryByText(/INTERNAL_/)).toBeNull()
    fireEvent.click(await within(d).findByLabelText('HOM_REPLAY_ADMIN'))
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer vers la signature' }))
    const s = await signer('Rejeu confié au service Normes et Agréments')
    expect(await within(s).findByText('Signature confirmée')).toBeTruthy()
    expect(definir.mock.calls[0][0]).toBe('ROLE_SNA')
    expect(definir.mock.calls[0][1]).toContain('HOM_REPLAY_ADMIN')
    fireEvent.click(within(s).getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText(/La composition du profil Normes et Agréments a été enregistrée/)).toBeTruthy()
  })

  it('RSSI : profils privilégiés en consultation seule ; lecteur : aucune modification', async () => {
    profils({ permissions: RSSI })
    await screen.findByText('ROLE_SUPER_ADMIN')
    const ligne = screen.getByText('ROLE_SUPER_ADMIN').closest('tr')!
    fireEvent.click(within(ligne).getByRole('button', { name: 'Voir la composition' }))
    const d = await screen.findByRole('dialog')
    expect(within(d).getByText('Consultation seule.')).toBeTruthy()
    expect((await within(d).findByLabelText('IAM_ACCOUNT_READ') as HTMLInputElement).disabled).toBe(true)
    expect(within(d).queryByRole('button', { name: 'Continuer vers la signature' })).toBeNull()
    fireEvent.click(within(d).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    expect(within(screen.getByText('ROLE_SNA').closest('tr')!).getByRole('button', { name: 'Modifier la composition' })).toBeTruthy()
    cleanup()
    profils({ permissions: ['IAM_ACCOUNT_READ'] })
    await screen.findByText('ROLE_SNA')
    expect(screen.queryByRole('button', { name: 'Modifier la composition' })).toBeNull()
  })
})

describe('Configuration et journalisation de la plateforme', () => {
  it('routes, propriétés masquées, service injoignable, lien vers la journalisation', async () => {
    rendrePage(<ConfigurationPlateformePage />, { chemin: '/c', motif: '/c', permissions: RSSI })
    expect(await screen.findByText('bff-console-iam')).toBeTruthy()
    expect(screen.getByText('******')).toBeTruthy()
    expect(screen.getByText('Service injoignable')).toBeTruthy()
    expect(screen.getAllByRole('link', { name: 'Niveaux de journalisation' })[0].getAttribute('href')).toBe('/console/plateforme/journalisation?service=iam')
  })

  it('super administrateur : niveau changé à chaud avec motif ; filtre par préfixe', async () => {
    const base = simules()
    const modifier = vi.fn(base.plateforme.modifierJournalisation)
    const lecture = vi.fn(base.plateforme.journalisation)
    rendrePage(<JournalisationPage />, { chemin: '/j', motif: '/j', permissions: SUPER_ADMIN, services: avec('plateforme', { modifierJournalisation: modifier, journalisation: lecture }, base) })
    expect(await screen.findByText('Choisissez un service', { selector: 'h3' })).toBeTruthy()
    const service = screen.getByRole('combobox', { name: 'Service' })
    await within(service).findByRole('option', { name: 'iam' })
    fireEvent.change(service, { target: { value: 'iam' } })
    expect(await screen.findByText('cm.gimac.des.iam.web')).toBeTruthy()
    fireEvent.change(screen.getByRole('searchbox', { name: 'Filtrer par préfixe de logger' }), { target: { value: 'org.' } })
    await waitFor(() => expect(lecture).toHaveBeenLastCalledWith('iam', 'org.'))
    await waitFor(() => expect(screen.queryByText('cm.gimac.des.iam.web')).toBeNull())
    const ligne = (await screen.findByText('org.hibernate.SQL')).closest('tr')!
    fireEvent.click(within(ligne).getByRole('button', { name: 'Modifier le niveau' }))
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByRole('combobox', { name: 'Nouveau niveau' }), { target: { value: 'DEBUG' } })
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Analyse de lenteurs SQL' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Appliquer le niveau' }))
    expect(await screen.findByText('Niveau DEBUG appliqué à org.hibernate.SQL.')).toBeTruthy()
    expect(modifier).toHaveBeenCalledWith('iam', { logger: 'org.hibernate.SQL', niveau: 'DEBUG', motif: 'Analyse de lenteurs SQL' })
    fireEvent.click(within((await screen.findByText('org.apache.kafka')).closest('tr')!).getByRole('button', { name: 'Modifier le niveau' }))
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Annuler' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('RSSI : jamais de modification du niveau ; refus du service affiché', async () => {
    rendrePage(<JournalisationPage />, { chemin: '/j?service=iam', motif: '/j', permissions: RSSI })
    expect(await screen.findByText('cm.gimac.des.iam.web')).toBeTruthy()
    expect(screen.getByText(/leur changement est réservé au super administrateur/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Modifier le niveau' })).toBeNull()
    cleanup()
    rendrePage(<JournalisationPage />, {
      chemin: '/j?service=iam', motif: '/j', permissions: SUPER_ADMIN,
      services: avec('plateforme', { modifierJournalisation: async () => { throw new ApiError(502, 'HTTP_502', 'Service injoignable ou en erreur', 'RQ-LOG') } }),
    })
    fireEvent.click((await screen.findAllByRole('button', { name: 'Modifier le niveau' }))[0])
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Analyse incident' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Appliquer le niveau' }))
    expect(await screen.findByText('RQ-LOG')).toBeTruthy()
    fireEvent.change(screen.getByRole('combobox', { name: 'Service' }), { target: { value: '' } })
    await waitFor(() => expect(screen.queryByText('RQ-LOG')).toBeNull())
  })
})
