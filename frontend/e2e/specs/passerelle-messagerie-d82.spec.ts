import { ADRESSES, expect, test } from '../fixtures'
import { Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'

// Fonctionnalités D-82 et D-83 sur la passerelle (console construite avec VITE_SERVICES=http) : la réponse et
// la mention partent dans la partie JSON « message » (reponseA, mentions) ; la notification MENTION de
// MS-NOTIFICATION, poussée par le canal, mène au message ; l'aperçu bureautique envoie les octets du document à
// POST /bff/console/document/apercus (X-Nom-Fichier, Accept) et affiche le PDF, ou la cause d'un refus.

test.describe('Fonctionnalité : réponses, mentions et aperçu bureautique par la passerelle', () => {
  test('Scénario : répondre en mentionnant, être mentionné, prévisualiser un docx, refus d’un tableur protégé', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/messagerie?conversation=c-dg`)
    const fil = page.getByRole('region', { name: 'François Tchoumi Kamga' })
    // Message de la direction générale, désigné par son propre texte (une citation le reprend).
    const message = fil.locator('li.gm-msg').filter({ has: page.locator('.gm-msg-texte', { hasText: 'Pouvez-vous me confirmer la revue des habilitations' }) })

    await test.step('Réponse au message de la direction générale avec une mention : corps reponseA et mentions', async () => {
      await message.hover()
      await message.getByRole('button', { name: 'Répondre à François Tchoumi Kamga' }).click()
      const champ = fil.getByLabel('Votre message')
      await champ.pressSequentially('Confirmé, @fr')
      await expect(fil.getByRole('option', { name: /François Tchoumi Kamga/ })).toBeVisible()
      await champ.press('Tab')
      await champ.pressSequentially('la revue est planifiée jeudi.')
      await fil.getByRole('button', { name: 'Envoyer' }).click()
      const envoye = fil.locator('li.gm-msg').filter({ hasText: 'la revue est planifiée jeudi.' })
      await expect(envoye.getByRole('button', { name: /En réponse à :\s*François Tchoumi Kamga/ })).toBeVisible()
      await expect(envoye.locator('.gm-mention')).toHaveText('@François Tchoumi Kamga')
      expect(gw.messagesRecus).toEqual([{ contenu: 'Confirmé, @François Tchoumi Kamga la revue est planifiée jeudi.', etiquettes: [], reponseA: 'm-1', mentions: ['u-dg'] }])
    })

    await test.step('Mention reçue hors de la messagerie : fenêtre surgissante, puis le message cité est surligné', async () => {
      await page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Tableau de bord', exact: true }).click()
      const id = gw.recevoirMention('merci, je valide.')
      const toast = page.getByRole('region', { name: 'Notifications reçues' }).locator('.gm-toast')
      await expect(toast.getByText('François Tchoumi Kamga vous a mentionné')).toBeVisible({ timeout: 15_000 })
      await expect(toast.getByText('@Serge Mouelle Kombi merci, je valide.')).toBeVisible()
      await toast.getByRole('button', { name: 'Ouvrir' }).click()
      await expect(page).toHaveURL(new RegExp(`/console/messagerie\\?conversation=c-dg&message=${id}$`))
      const recu = fil.locator('li.gm-msg').filter({ has: page.locator('.gm-msg-texte', { hasText: 'merci, je valide.' }) })
      await expect(recu).toHaveClass(/gm-msg-evidence/)
      await expect(recu.locator('.gm-mention-moi')).toHaveText('@Serge Mouelle Kombi')
      await expect(recu.getByRole('button', { name: /En réponse à :\s*Vous/ })).toBeVisible()
      await expect.poll(() => gw.appels.filter((a) => a.chemin === '/bff/console/notification/notifications/lecture').map((a) => a.corps))
        .toContainEqual({ reference: { service: 'collaboration', type: 'CONVERSATION', id: 'c-dg' } })
      expect(gw.notifications.find((n) => n.type === 'MENTION')!.lueLe).not.toBeNull()
    })

    await test.step('Aperçu d’un document Word : octets envoyés à MS-DOCUMENT, PDF affiché', async () => {
      await message.getByRole('button', { name: 'Ouvrir note-habilitations.docx' }).click()
      const d = page.getByRole('dialog', { name: /note-habilitations\.docx/ })
      await expect(d.getByRole('img', { name: 'Page 1 sur 1' })).toBeVisible()
      expect(gw.apercus[0]).toEqual({ nom: 'note-habilitations.docx', type: 'application/octet-stream', octets: 6, accept: 'application/pdf, application/problem+json' })
      expect(gw.appels.find((a) => a.chemin === '/bff/console/document/apercus')!.entetes['x-nom-fichier']).toBe('note-habilitations.docx')
      await d.getByRole('button', { name: 'Fermer' }).last().click()
    })

    await test.step('Tableau protégé par mot de passe : 422 expliqué, Télécharger toujours présent', async () => {
      await message.getByRole('button', { name: 'Ouvrir grille-protegee.xlsx' }).click()
      const d = page.getByRole('dialog', { name: /grille-protegee\.xlsx/ })
      await expect(d.getByText('Document illisible : fichier endommagé ou protégé par mot de passe.')).toBeVisible()
      const telechargement = page.waitForEvent('download')
      await d.getByRole('button', { name: 'Télécharger' }).click()
      expect((await telechargement).suggestedFilename()).toBe('grille-protegee.xlsx')
      expect(gw.appels.filter((a) => a.chemin.startsWith('/bff/des/'))).toEqual([])
    })
  })
})
