import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Locator, Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { joindre } from './outils'

// Fonctionnalités D-82, D-83, D-84 (services simulés) : répondre à un message précis et mentionner avec « @ »
// (citation dans la bulle, liste des membres, mention mise en évidence, notifications MENTION et REPONSE, lien
// vers le message), aperçu des documents bureautiques convertis en PDF (docx, pptx, refus expliqué), largeur du
// fil (séparateur, liste repliée, barre latérale réduite). Captures clair et sombre dans
// e2e/captures/messagerie-d82 (ou $GIMAC_CAPTURES/messagerie-d82), et l'étiquette « Prédécesseurs » corrigée (D-77).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'messagerie-d82') : resolve(ICI, '..', 'captures', 'messagerie-d82')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().filter((a) => a.effect?.getComputedTiming().iterations !== Infinity).map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}

const conversations = (page: Page) => page.getByRole('list', { name: 'Conversations' })
const cloche = (page: Page) => page.getByRole('banner').getByRole('button', { name: /^Notifications/ })
const panneau = (page: Page) => page.getByRole('dialog', { name: 'Centre de notifications' })
/** Bulle désignée par son propre texte (une citation reprend celui du message cité). */
const bulle = (fil: Locator, texte: string | RegExp) => fil.locator('li.gm-msg').filter({ has: fil.page().locator('.gm-msg-texte', { hasText: texte }) })
const recevoir = (page: Page, o: Record<string, unknown>) =>
  page.evaluate((x) => (window as unknown as { gmDemoNotifications: { recevoirMessage(o: unknown): string } }).gmDemoNotifications.recevoirMessage(x), o)

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'

  test.describe(`Fonctionnalité : répondre et mentionner (D-82, ${suffixe})`, () => {
    test('Scénario : citation dans la bulle, réponse en cours, liste @, mention envoyée ; notifications et lien vers le message', async ({ page, context }) => {
      await context.grantPermissions(['notifications'])
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/messagerie`)
      await conversations(page).getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })

      await test.step('Étant donné la réponse du RSSI au rapport des essais, qui mentionne le chef du DES', async () => {
        const reponse = bulle(fil, /je prends contact/)
        await expect(reponse.locator('.gm-mention-moi')).toHaveText('@Paul Mbarga Atangana')
        await expect(reponse).toHaveClass(/gm-msg-mentionne/)
        await expect(reponse.getByRole('img', { name: 'Vous concerne' })).toBeVisible()
        await capturer(page, `message-mention-${suffixe}`)
      })

      await test.step('Quand il ouvre la citation, le message d’origine est amené et surligné', async () => {
        await bulle(fil, /je prends contact/).getByRole('button', { name: /En réponse à :\s*Rachel Biyong Mengue/ }).click()
        await expect(bulle(fil, 'Les essais sont suspendus')).toHaveClass(/gm-msg-evidence/)
        await capturer(page, `bulle-citation-${suffixe}`)
      })

      await test.step('Quand il répond au dernier message de Rachel en mentionnant le RSSI', async () => {
        const derniere = bulle(fil, 'Le blocage est déclaré')
        await derniere.hover()
        await derniere.getByRole('button', { name: 'Répondre à Rachel Biyong Mengue' }).click()
        await expect(fil.getByRole('group', { name: 'Réponse à Rachel Biyong Mengue' })).toContainText('Le blocage est déclaré')
        const champ = fil.getByLabel('Votre message')
        await expect(champ).toBeFocused()
        await champ.pressSequentially('Bien noté. @')
        const liste = fil.getByRole('listbox', { name: 'Membres à mentionner' })
        await expect(liste.getByRole('option')).toHaveCount(3)
        await champ.pressSequentially('her')
        await expect(liste.getByRole('option')).toHaveCount(1)
        await capturer(page, `liste-mentions-${suffixe}`)
        await champ.press('Enter')
        await expect(champ).toHaveValue('Bien noté. @Hervé Kamga Tchoupo ')
        await champ.pressSequentially('pouvez-vous confirmer la date ?')
        await capturer(page, `reponse-saisie-${suffixe}`)
        // Échap ferme d'abord la liste, puis annule la réponse ; on la reprend.
        await champ.press('Escape')
        await expect(fil.getByRole('group', { name: /Réponse à/ })).toHaveCount(0)
        await derniere.getByRole('button', { name: 'Répondre à Rachel Biyong Mengue' }).click()
        await fil.getByRole('button', { name: 'Envoyer' }).click()
      })

      await test.step('Alors la bulle envoyée porte la citation et la mention', async () => {
        const envoye = bulle(fil, 'pouvez-vous confirmer la date ?')
        await expect(envoye.getByRole('button', { name: /En réponse à :\s*Rachel Biyong Mengue/ })).toBeVisible()
        await expect(envoye.locator('.gm-mention')).toHaveText('@Hervé Kamga Tchoupo')
        await expect(fil.getByRole('group', { name: /Réponse à/ })).toHaveCount(0)
      })

      let conversation = ''
      await test.step('Quand Aïcha le mentionne puis répond à son message : deux notifications, sans regroupement', async () => {
        await cloche(page).click()
        await panneau(page).getByRole('button', { name: 'Tout marquer comme lu' }).click()
        await expect(page.locator('.gm-topbar-pastille')).toHaveCount(0)
        await page.keyboard.press('Escape')
        conversation = await recevoir(page, { contenu: 'pouvez-vous relire le procès-verbal ?', mention: true })
        await recevoir(page, { conversationId: conversation, contenu: 'Je confirme la signature.', reponse: true })
        // Message regroupé dans la notification de la conversation, mention et réponse jamais regroupées.
        await expect(page.locator('.gm-topbar-pastille')).toHaveText('3')
        await cloche(page).click()
        await expect(panneau(page).getByRole('button', { name: 'Mention : Aïcha Mahamat Nour vous a mentionné, non lue' })).toBeVisible()
        await expect(panneau(page).getByRole('button', { name: 'Réponse : Aïcha Mahamat Nour a répondu à votre message, non lue' })).toContainText('Je confirme la signature.')
        await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
        await capturer(page, `cloche-mention-reponse-${suffixe}`)
      })

      await test.step('Alors la notification de mention ouvre la conversation sur le message, surligné', async () => {
        await panneau(page).getByRole('button', { name: /^Mention : Aïcha Mahamat Nour/ }).click()
        await expect(page).toHaveURL(new RegExp(`/des/messagerie\\?conversation=${conversation}&message=MSG-`))
        const direct = page.getByRole('region', { name: 'Aïcha Mahamat Nour' })
        await expect(bulle(direct, 'pouvez-vous relire le procès-verbal ?')).toHaveClass(/gm-msg-evidence/)
        await expect(page.locator('.gm-topbar-pastille')).toHaveCount(0)
      })

      await test.step('Et un message qui le mentionne dans la conversation ouverte ne surgit pas', async () => {
        await recevoir(page, { conversationId: conversation, contenu: 'merci pour la relecture.', mention: true })
        await expect(page.getByRole('region', { name: 'Aïcha Mahamat Nour' }).getByText(/merci pour la relecture/)).toBeVisible({ timeout: 15_000 })
        await page.waitForTimeout(800)
        await expect(page.getByRole('region', { name: 'Notifications reçues' })).toHaveCount(0)
      })

      await test.step('Et les préférences de Mon compte portent les interrupteurs Mentions et Réponses', async () => {
        await page.goto(`${ADRESSES.departement}/des/mon-compte#notifications`)
        const section = page.getByRole('region', { name: 'Notifications', exact: true })
        await expect(section.getByRole('switch', { name: 'Réponses : quand on répond à l’un de mes messages' })).toBeChecked()
        await section.getByRole('switch', { name: 'Mentions : quand on me mentionne avec @' }).click()
        await expect(section.getByRole('switch', { name: 'Mentions : quand on me mentionne avec @' })).not.toBeChecked()
        await expect(section.getByText('Préférences de notification enregistrées.')).toBeVisible()
      })
    })

    test('Scénario : aperçu des documents Word et PowerPoint, refus expliqué (D-83)', async ({ page }) => {
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/messagerie`)
      await conversations(page).getByRole('button', { name: /Hervé Kamga Tchoupo/ }).click()
      const fil = page.getByRole('region', { name: 'Hervé Kamga Tchoupo' })
      await expect(fil.getByText('Message supprimé')).toBeVisible()

      await test.step('Le document Word est converti puis affiché page par page, son pictogramme devant le titre', async () => {
        await fil.getByRole('button', { name: 'Ouvrir note-attestations-PCI-DSS.docx' }).click()
        const d = page.getByRole('dialog', { name: /note-attestations-PCI-DSS\.docx/ })
        await expect(d.getByRole('img', { name: 'Page 1 sur 1' })).toBeVisible()
        await expect(d.locator('.gm-dialog-icone')).toBeVisible()
        await expect(d.getByRole('button', { name: 'Télécharger' })).toBeVisible()
        await capturer(page, `apercu-docx-${suffixe}`)
        await d.getByRole('button', { name: 'Fermer' }).last().click()
      })

      await test.step('La présentation PowerPoint aussi', async () => {
        await fil.getByRole('button', { name: 'Ouvrir revue-securite-LIA-2026-019.pptx' }).click()
        const d = page.getByRole('dialog', { name: /revue-securite-LIA-2026-019\.pptx/ })
        await expect(d.getByRole('img', { name: 'Page 1 sur 1' })).toBeVisible()
        await capturer(page, `apercu-pptx-${suffixe}`)
        const telechargement = page.waitForEvent('download')
        await d.getByRole('button', { name: 'Télécharger' }).click()
        expect((await telechargement).suggestedFilename()).toBe('revue-securite-LIA-2026-019.pptx')
        await page.keyboard.press('Escape')
        await expect(d).toHaveCount(0)
      })

      await test.step('Un document protégé par mot de passe : cause affichée, téléchargement toujours possible', async () => {
        await joindre(fil, 'contrat-protege.docx', 'PK', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')
        await expect(fil.getByRole('list', { name: 'Pièces en cours d’envoi' }).getByText('Prêt')).toBeVisible()
        await fil.getByRole('button', { name: 'Envoyer' }).click()
        await fil.getByRole('button', { name: 'Ouvrir contrat-protege.docx' }).click()
        const d = page.getByRole('dialog', { name: /contrat-protege\.docx/ })
        await expect(d.getByText('Document illisible : fichier endommagé ou protégé par mot de passe.')).toBeVisible()
        await expect(d.getByRole('button', { name: 'Télécharger' })).toBeEnabled()
      })
    })

    test('Scénario : élargir le fil, séparateur, liste repliée, barre latérale réduite (D-84)', async ({ page }) => {
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/messagerie`)
      await conversations(page).getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
      const sep = page.getByRole('separator', { name: 'Largeur de la liste des conversations' })
      const liste = page.getByRole('complementary', { name: 'Messagerie' })

      await test.step('Le séparateur se déplace à la souris et au clavier, dans les bornes de 240 à 480 px', async () => {
        await expect(sep).toHaveAttribute('aria-valuenow', '320')
        const boite = (await sep.boundingBox())!
        await page.mouse.move(boite.x + boite.width / 2, boite.y + boite.height / 2)
        await page.mouse.down()
        await page.mouse.move(boite.x + 120, boite.y + boite.height / 2, { steps: 5 })
        await page.mouse.up()
        await expect(sep).toHaveAttribute('aria-valuenow', /^4[0-9]{2}$/)
        await sep.focus()
        await page.keyboard.press('End')
        await expect(sep).toHaveAttribute('aria-valuenow', '480')
        expect(Math.round((await liste.boundingBox())!.width)).toBe(480)
        await capturer(page, `liste-elargie-${suffixe}`)
        await page.keyboard.press('ArrowLeft')
        await expect(sep).toHaveAttribute('aria-valuenow', '464')
        await sep.dblclick()
        await expect(sep).toHaveAttribute('aria-valuenow', '320')
      })

      await test.step('La liste se replie depuis l’en-tête du fil : le fil prend toute la largeur', async () => {
        await fil.getByRole('button', { name: 'Masquer la liste des conversations' }).click()
        await expect(liste).toBeHidden()
        await capturer(page, `liste-repliee-${suffixe}`)
        await fil.getByRole('button', { name: 'Afficher la liste des conversations' }).click()
        await expect(liste).toBeVisible()
      })

      const nav = page.locator('.gm-shell-nav .gm-sidenav')
      await test.step('« Élargir » réduit la barre latérale aux pictogrammes, libellés toujours lus', async () => {
        await page.getByRole('button', { name: 'Élargir' }).click()
        await expect(nav).toHaveClass(/gm-sidenav-reduite/)
        await expect(page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Tableau de bord', exact: true })).toHaveAttribute('title', 'Tableau de bord')
        await capturer(page, `barre-reduite-${suffixe}`)
      })

      await test.step('Elle revient en quittant la messagerie, et se réduit de nouveau au retour (préférence gardée)', async () => {
        await page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Tableau de bord', exact: true }).click()
        await expect(nav).not.toHaveClass(/gm-sidenav-reduite/)
        await page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: /^Messagerie/ }).click()
        await expect(nav).toHaveClass(/gm-sidenav-reduite/)
        await page.getByRole('button', { name: 'Rétablir la barre latérale' }).click()
        await expect(nav).not.toHaveClass(/gm-sidenav-reduite/)
      })

      await test.step('Sous 900 px, comportement d’origine : ni séparateur ni repli', async () => {
        await page.setViewportSize({ width: 820, height: 900 })
        await expect(sep).toHaveCount(0)
        await expect(page.getByRole('button', { name: 'Élargir' })).toHaveCount(0)
      })
    })

    test('Scénario : étiquette « Prédécesseurs » du modélisateur, libellé long tronqué (D-77)', async ({ page }) => {
      await demo(page, { profil: 'ROLE_INGENIEUR_HOMOLOGATION', theme })
      await page.goto(`${ADRESSES.departement}/des/referentiels/operations?version=REF-CI-4`)
      await page.locator('.react-flow__node[data-id="C11"]').getByRole('button', { name: 'Modifier l\'opération C11' }).click()
      const d = page.getByRole('dialog', { name: 'Opération C11' })
      await d.getByRole('tab', { name: 'Chronologie' }).click()
      const combo = d.getByRole('combobox', { name: /^Prédécesseurs/ })
      const champ = combo.locator('xpath=..')
      await combo.click()
      await combo.fill('C')
      const options = page.getByRole('option')
      // Libellé le plus long parmi les propositions : il ne déborde pas de son étiquette.
      const textes = await options.allTextContents()
      const long = textes.reduce((a, b) => (b.length > a.length ? b : a), '')
      await options.filter({ hasText: long }).first().click({ force: true })
      const etiquette = champ.locator('.gm-tag-texte', { hasText: long.slice(0, 12) }).last()
      await expect(etiquette).toHaveAttribute('title', long)
      const [b, c] = await Promise.all([etiquette.locator('xpath=..').boundingBox(), champ.boundingBox()])
      expect(b!.x + b!.width).toBeLessThanOrEqual(c!.x + c!.width + 1)
      await d.getByRole('heading', { name: 'Opération C11' }).click()
      await capturer(page, `predecesseurs-${suffixe}`)
    })
  })
}
