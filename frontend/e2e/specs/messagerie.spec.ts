import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { joindre } from './outils'

// Fonctionnalité : messagerie de travail entre acteurs du GIMAC (D-64, /des/messagerie) : pastille des
// non-lus dans la navigation, conversations et fil, pièce jointe téléchargée, nouveau message à une
// personne de l'annuaire avec objet, liaison citée et pièce jointe, refus du service expliqué, liaisons
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
    await test.step('Quand il ouvre la conversation de groupe rattachée à LIA-2026-030 et télécharge le rapport joint', async () => {
      await conversations(page).getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
      await expect(fil.getByRole('link', { name: 'Ouvrir la liaison LIA-2026-030' })).toHaveAttribute('href', '/des/parcours/LIA-2026-030')
      await expect(entree(page).getByLabel(/non lus/)).toHaveCount(0)
      const telechargement = page.waitForEvent('download')
      await fil.getByRole('button', { name: 'Télécharger rapport-essais-LIA-2026-030.pdf' }).click()
      expect((await telechargement).suggestedFilename()).toBe('rapport-essais-LIA-2026-030.pdf')
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

  test('Scénario : écrire un nouveau message à une personne de l’annuaire, pièce jointe et liaison citée', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/messagerie`)
    const panneau = page.getByRole('region', { name: 'Nouveau message' })
    await test.step('Quand il envoie un message sans destinataire, le service le refuse', async () => {
      await page.getByRole('button', { name: 'Nouveau message' }).click()
      await panneau.getByLabel('Votre message').fill('Sans destinataire')
      await panneau.getByRole('button', { name: 'Envoyer' }).click()
      await expect(panneau.getByText('Choisissez au moins un destinataire.')).toBeVisible()
      await panneau.getByRole('button', { name: 'Annuler' }).click()
      await expect(page.getByText('Choisissez une conversation')).toBeVisible()
    })
    await test.step('Quand il choisit une destinataire de l’annuaire, un objet, une liaison et une pièce jointe', async () => {
      await page.getByRole('button', { name: 'Nouveau message' }).click()
      await panneau.getByRole('combobox', { name: 'Destinataires' }).fill('rachel')
      await panneau.getByRole('option', { name: /Rachel Biyong Mengue/ }).click()
      await expect(panneau.getByText(/Rachel Biyong Mengue · Normes et Agréments/)).toBeVisible()
      await panneau.getByLabel('Objet').fill('Bascule de LIA-2026-012')
      await panneau.getByRole('button', { name: 'Citer une liaison' }).click()
      await panneau.getByRole('combobox', { name: 'Citer une liaison' }).fill('Banque D')
      await panneau.getByRole('option', { name: /LIA-2026-012/ }).click()
      await expect(panneau.getByText('# LIA-2026-012')).toBeVisible()
      await joindre(panneau, 'plan-bascule.pdf', 'plan de bascule')
      await expect(panneau.getByText('plan-bascule.pdf')).toBeVisible()
      await panneau.getByLabel('Votre message').fill('Plan de bascule joint pour relecture.')
      await panneau.getByRole('button', { name: 'Envoyer' }).click()
    })
    await test.step('Alors la conversation est créée et son fil montre le message et la pièce', async () => {
      const fil = page.getByRole('region', { name: 'Bascule de LIA-2026-012' })
      await expect(fil.getByText('Plan de bascule joint pour relecture.')).toBeVisible()
      await expect(fil.getByRole('button', { name: 'Télécharger plan-bascule.pdf' })).toBeVisible()
      await expect(fil.getByRole('link', { name: '# LIA-2026-012' })).toHaveAttribute('href', '/des/parcours/LIA-2026-012')
      await expect(conversations(page).getByRole('button')).toHaveCount(4)
    })
    await test.step('Et la recherche retrouve une conversation par son contenu', async () => {
      await page.getByLabel('Rechercher une conversation').fill('pci')
      await expect(conversations(page).getByRole('button')).toHaveCount(1)
      await expect(conversations(page).getByRole('button', { name: /Hervé Kamga Tchoupo/ })).toBeVisible()
    })
  })

  test('Scénario : discuter d’une liaison depuis son dossier', async ({ page }) => {
    await page.goto(`${ADRESSES.departement}/des/parcours/LIA-2026-030`)
    await test.step('Quand il choisit « Discuter de cette liaison » dans le dossier', async () => {
      await page.getByRole('link', { name: 'Discuter de cette liaison' }).click()
      await expect(page).toHaveURL(/\/des\/messagerie\?nouveau=1&liaison=LIA-2026-030/)
    })
    await test.step('Alors le nouveau message est rattaché à la liaison, citée d’emblée', async () => {
      const panneau = page.getByRole('region', { name: 'Nouveau message' })
      await expect(panneau.getByText(/Liaison LIA-2026-030 · Rattache la conversation au dossier de la liaison/)).toBeVisible()
      await expect(panneau.getByText('# LIA-2026-030')).toBeVisible()
    })
    await test.step('Et, une fois envoyé à la conductrice de projet, le fil mène à la liaison', async () => {
      const panneau = page.getByRole('region', { name: 'Nouveau message' })
      await panneau.getByRole('combobox', { name: 'Destinataires' }).fill('conductrice')
      await panneau.getByRole('option', { name: /Aïcha Mahamat Nour/ }).click()
      await panneau.getByLabel('Votre message').fill('Où en est la levée du blocage de LIA-2026-030 ?')
      await panneau.getByRole('button', { name: 'Envoyer' }).click()
      const fil = page.getByRole('region', { name: /Aïcha Mahamat Nour/ })
      await fil.getByRole('link', { name: 'Ouvrir la liaison LIA-2026-030' }).click()
      await expect(page).toHaveURL(/\/des\/parcours\/LIA-2026-030$/)
    })
  })
})
