import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { joindre } from './outils'

// Fonctionnalité : messagerie de travail entre acteurs du GIMAC (D-64, D-68, /des/messagerie) : pastille des
// non-lus dans la navigation, conversations et fil, pièce jointe ouverte dans une fenêtre d'aperçu puis
// téléchargée, nouveau message à une personne cherchée par son nom dans l'annuaire, liaison citée et pièce
// jointe, groupe de discussion nommé (création, renommage, membres ajoutés et retirés, départ), liaisons
// citées en liens vers leur dossier, « Discuter de cette liaison » depuis le dossier. Services simulés.

const entree = (page: Page) => page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: /^Messagerie/ })
const conversations = (page: Page) => page.getByRole('list', { name: 'Conversations' })

test.describe('Fonctionnalité : messagerie de travail', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DES', theme: 'light' })
  })

  test('Scénario : lire ses conversations, les non-lus diminuent, répondre en citant une liaison', async ({ page }) => {
    await test.step('Étant donné trois messages non lus signalés dans la navigation', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(entree(page).getByLabel('3 non lus')).toBeVisible()
      await entree(page).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Messagerie')
      await expect(conversations(page).getByRole('button')).toHaveCount(3)
      await expect(page.getByText('Choisissez une conversation')).toBeVisible()
      await expect(page.getByText(/^Page 1 sur 1 · 3 éléments$/)).toBeVisible()
    })
    await test.step('Quand il ouvre la conversation directe avec la conductrice de projet', async () => {
      await conversations(page).getByRole('button', { name: /Aïcha Mahamat Nour/ }).click()
      const fil = page.getByRole('region', { name: 'Aïcha Mahamat Nour' })
      await expect(fil.getByText('Merci. Le participant signera ensuite depuis son portail.')).toBeVisible()
      await expect(fil.getByText('Bien reçu, je le signe cet après-midi.')).toBeVisible()
    })
    await test.step('Alors ses non-lus diminuent', async () => {
      await expect(entree(page).getByLabel('2 non lus')).toBeVisible()
    })
    await test.step('Quand il ouvre la conversation de groupe rattachée à LIA-2026-030 et consulte le rapport joint', async () => {
      await conversations(page).getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
      await expect(fil.getByRole('link', { name: 'Ouvrir la liaison LIA-2026-030' })).toHaveAttribute('href', '/des/parcours/LIA-2026-030')
      await expect(entree(page).getByLabel(/non lus/)).toHaveCount(0)
      await expect(fil.getByText('Paul Mbarga Atangana a créé le groupe « Essais de LIA-2026-030 »').or(fil.getByText('Vous avez créé le groupe « Essais de LIA-2026-030 »'))).toBeVisible()
      await fil.getByRole('button', { name: 'Ouvrir rapport-essais-LIA-2026-030.pdf' }).click()
    })
    await test.step('Alors le rapport s’affiche dans une fenêtre d’aperçu, d’où il se télécharge', async () => {
      const apercu = page.getByRole('dialog', { name: /rapport-essais-LIA-2026-030\.pdf/ })
      await expect(apercu.locator('iframe.gm-apercu-cadre')).toBeVisible()
      const telechargement = page.waitForEvent('download')
      await apercu.getByRole('button', { name: 'Télécharger' }).click()
      expect((await telechargement).suggestedFilename()).toBe('rapport-essais-LIA-2026-030.pdf')
      await apercu.getByRole('button', { name: 'Fermer' }).last().click()
      await expect(apercu).toHaveCount(0)
    })
    await test.step('Et le journal des essais (CSV) s’y lit en texte', async () => {
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
      await fil.getByRole('button', { name: 'Ouvrir journal-essais.csv' }).click()
      const apercu = page.getByRole('dialog', { name: /journal-essais\.csv/ })
      await expect(apercu.getByText(/retrait;échec;05/)).toBeVisible()
      await page.keyboard.press('Escape')
      await expect(apercu).toHaveCount(0)
    })
    await test.step('Et qu’il répond en citant la liaison LIA-2026-027', async () => {
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
      await fil.getByRole('button', { name: 'Citer une liaison' }).click()
      await fil.getByRole('combobox', { name: 'Citer une liaison' }).fill('LIA-2026-027')
      await fil.getByRole('option', { name: /LIA-2026-027/ }).click()
      await expect(fil.getByText('# LIA-2026-027')).toBeVisible()
      await fil.getByLabel('Votre message').fill('Point sur LIA-2026-027 demain matin.')
      await fil.getByRole('button', { name: 'Envoyer' }).click()
    })
    await test.step('Alors son message figure au fil, la liaison citée devenue un lien vers son dossier', async () => {
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
      await expect(fil.getByLabel('Votre message')).toHaveValue('')
      const message = fil.getByRole('listitem').filter({ hasText: 'Point sur' })
      await expect(message.getByText('Vous')).toBeVisible()
      await message.getByRole('link', { name: 'LIA-2026-027' }).first().click()
      await expect(page).toHaveURL(/\/des\/parcours\/LIA-2026-027$/)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Établissement de paiement A')
    })
  })

  test('Scénario : écrire à une personne cherchée par son nom dans l’annuaire, pièce jointe et liaison citée', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/messagerie`)
    const panneau = page.getByRole('region', { name: 'Nouveau message' })
    await test.step('Quand il ouvre un nouveau message, l’annuaire de la plateforme est proposé', async () => {
      await page.getByRole('button', { name: 'Nouveau message' }).click()
      await expect(panneau.getByRole('list', { name: 'Annuaire de la plateforme' }).getByRole('button').first()).toBeVisible()
      await expect(panneau.getByLabel('Votre message')).toHaveCount(0)
    })
    await test.step('Quand il cherche « rachel » et choisit la personne', async () => {
      await panneau.getByLabel('Rechercher une personne').fill('rachel')
      const annuaire = panneau.getByRole('list', { name: 'Annuaire de la plateforme' })
      await expect(annuaire.getByRole('button')).toHaveCount(1)
      await annuaire.getByRole('button', { name: /Rachel Biyong Mengue/ }).click()
      await expect(panneau.getByText('Destinataire', { exact: true })).toBeVisible()
      await expect(panneau.getByText('Normes et Agréments', { exact: false })).toBeVisible()
    })
    await test.step('Et qu’il cite une liaison, joint une pièce et envoie', async () => {
      await panneau.getByRole('button', { name: 'Citer une liaison' }).click()
      await panneau.getByRole('combobox', { name: 'Citer une liaison' }).fill('Banque D')
      await panneau.getByRole('option', { name: /LIA-2026-012/ }).click()
      await expect(panneau.getByText('# LIA-2026-012')).toBeVisible()
      await joindre(panneau, 'plan-bascule.pdf', 'plan de bascule')
      await expect(panneau.getByText('plan-bascule.pdf')).toBeVisible()
      await panneau.getByLabel('Votre message').fill('Plan de bascule joint pour relecture.')
      await panneau.getByRole('button', { name: 'Envoyer' }).click()
    })
    await test.step('Alors la conversation directe s’ouvre sur le message et la pièce', async () => {
      const fil = page.getByRole('region', { name: 'Rachel Biyong Mengue' })
      await expect(fil.getByText('Plan de bascule joint pour relecture.')).toBeVisible()
      await expect(fil.getByRole('button', { name: 'Ouvrir plan-bascule.pdf' })).toBeVisible()
      await expect(fil.getByRole('link', { name: '# LIA-2026-012' })).toHaveAttribute('href', '/des/parcours/LIA-2026-012')
      await expect(conversations(page).getByRole('button')).toHaveCount(4)
    })
    await test.step('Et la recherche retrouve une conversation par son contenu', async () => {
      await page.getByLabel('Rechercher une conversation').fill('pci')
      await expect(conversations(page).getByRole('button')).toHaveCount(1)
      await expect(conversations(page).getByRole('button', { name: /Hervé Kamga Tchoupo/ })).toBeVisible()
    })
  })

  test('Scénario : créer un groupe de discussion, le renommer, ajouter et retirer des membres, puis le quitter', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/messagerie`)
    const panneau = page.getByRole('region', { name: 'Nouveau groupe de discussion' })
    await test.step('Quand il crée un groupe sans nom ni membre, la création est refusée', async () => {
      await page.getByRole('button', { name: 'Nouveau groupe' }).click()
      await panneau.getByRole('button', { name: 'Créer le groupe' }).click()
      await expect(panneau.getByText('Donnez un nom au groupe.')).toBeVisible()
      await expect(panneau.getByText('Choisissez au moins un destinataire.')).toBeVisible()
    })
    await test.step('Quand il le nomme et choisit deux personnes dans l’annuaire', async () => {
      await panneau.getByLabel(/Nom du groupe/).fill('Cellule de bascule')
      const annuaire = panneau.getByRole('list', { name: 'Annuaire de la plateforme' })
      await panneau.getByLabel('Rechercher une personne').fill('rachel')
      await annuaire.getByRole('button', { name: /Rachel Biyong Mengue/ }).click()
      await panneau.getByLabel('Rechercher une personne').fill('conductrice')
      await annuaire.getByRole('button', { name: /Aïcha Mahamat Nour/ }).click()
      await expect(panneau.getByText('2 personnes choisies')).toBeVisible()
      await panneau.getByRole('button', { name: 'Créer le groupe' }).click()
    })
    const fil = page.getByRole('region', { name: /Cellule de bascule/ })
    await test.step('Alors le groupe s’ouvre sur l’événement de sa création', async () => {
      await expect(fil.getByText('Vous avez créé le groupe « Cellule de bascule »')).toBeVisible()
      await expect(conversations(page).getByRole('button', { name: /Cellule de bascule/ })).toBeVisible()
    })
    await test.step('Quand, administrateur, il le renomme et ajoute un membre', async () => {
      await fil.getByRole('button', { name: 'Membres et réglages' }).click()
      const reglages = page.getByRole('dialog', { name: /Cellule de bascule/ })
      await expect(reglages.getByText('Administrateur').first()).toBeVisible()
      await reglages.getByLabel(/Nom du groupe/).fill('Cellule de bascule LIA-2026-012')
      await reglages.getByRole('button', { name: 'Renommer' }).click()
      await reglages.getByLabel('Rechercher une personne').fill('Hervé')
      await reglages.getByRole('list', { name: 'Annuaire de la plateforme' }).getByRole('button', { name: /Hervé Kamga Tchoupo/ }).click()
      await reglages.getByRole('button', { name: 'Ajouter 1 membre' }).click()
      await expect(reglages.getByRole('button', { name: 'Retirer Hervé Kamga Tchoupo du groupe' })).toBeVisible()
    })
    await test.step('Et qu’il retire une personne après confirmation', async () => {
      const reglages = page.getByRole('dialog', { name: /Cellule de bascule LIA-2026-012/ })
      await reglages.getByRole('button', { name: 'Retirer Rachel Biyong Mengue du groupe' }).click()
      await page.getByRole('alertdialog').getByRole('button', { name: 'Retirer Rachel Biyong Mengue du groupe' }).click()
      await expect(reglages.getByRole('button', { name: 'Retirer Rachel Biyong Mengue du groupe' })).toHaveCount(0)
      await reglages.getByRole('button', { name: 'Fermer' }).last().click()
    })
    await test.step('Alors le fil retrace chaque changement', async () => {
      const groupe = page.getByRole('region', { name: 'Cellule de bascule LIA-2026-012' })
      await expect(groupe.getByText('Vous avez renommé le groupe en « Cellule de bascule LIA-2026-012 »')).toBeVisible()
      await expect(groupe.getByText(/Vous avez ajouté Hervé Kamga Tchoupo/)).toBeVisible()
      await expect(groupe.getByText(/Vous avez retiré Rachel Biyong Mengue/)).toBeVisible()
    })
    await test.step('Quand il quitte le groupe, celui-ci disparaît de ses conversations', async () => {
      const groupe = page.getByRole('region', { name: 'Cellule de bascule LIA-2026-012' })
      await groupe.getByRole('button', { name: 'Membres et réglages' }).click()
      await page.getByRole('dialog', { name: /Cellule de bascule/ }).getByRole('button', { name: 'Quitter le groupe' }).click()
      await page.getByRole('alertdialog').getByRole('button', { name: 'Quitter le groupe' }).click()
      await expect(page.getByText('Choisissez une conversation')).toBeVisible()
      await expect(conversations(page).getByRole('button', { name: /Cellule de bascule/ })).toHaveCount(0)
    })
  })

  test('Scénario : discuter d’une liaison depuis son dossier', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/parcours/LIA-2026-030`)
    await test.step('Quand il choisit « Discuter de cette liaison » dans le dossier', async () => {
      await page.getByRole('link', { name: 'Discuter de cette liaison' }).click()
      await expect(page).toHaveURL(/\/des\/messagerie\?nouveau=1&liaison=LIA-2026-030/)
    })
    await test.step('Alors le nouveau message est rattaché à la liaison, citée d’emblée une fois la conductrice choisie', async () => {
      const panneau = page.getByRole('region', { name: 'Nouveau message' })
      await expect(panneau.getByText(/Liaison LIA-2026-030 · Rattache la conversation au dossier de la liaison/)).toBeVisible()
      await panneau.getByLabel('Rechercher une personne').fill('conductrice')
      await panneau.getByRole('list', { name: 'Annuaire de la plateforme' }).getByRole('button', { name: /Aïcha Mahamat Nour/ }).click()
      await expect(panneau.getByText('# LIA-2026-030')).toBeVisible()
    })
    await test.step('Et, une fois envoyé, le fil mène à la liaison', async () => {
      const panneau = page.getByRole('region', { name: 'Nouveau message' })
      await panneau.getByLabel('Votre message').fill('Où en est la levée du blocage de LIA-2026-030 ?')
      await panneau.getByRole('button', { name: 'Envoyer' }).click()
      const fil = page.getByRole('region', { name: /Aïcha Mahamat Nour/ })
      await fil.getByRole('link', { name: 'Ouvrir la liaison LIA-2026-030' }).click()
      await expect(page).toHaveURL(/\/des\/parcours\/LIA-2026-030$/)
    })
  })
})
