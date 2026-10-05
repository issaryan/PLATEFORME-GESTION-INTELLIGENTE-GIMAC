import { expect, type Locator, type Page } from '@playwright/test'

// Gestes communs des scénarios : signature d'un acte sensible (motif puis clé de sécurité),
// confirmation motivée, navigation latérale.

export async function signer(page: Page, motif = 'Motif de test suffisamment détaillé') {
  const d = page.getByRole('dialog')
  await d.getByLabel(/Motif/).fill(motif)
  await d.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
  return d
}

export async function signerEtTerminer(page: Page, motif?: string) {
  const d = await signer(page, motif)
  await expect(d.getByText('Signature confirmée')).toBeVisible()
  await d.getByRole('button', { name: 'Continuer' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

export async function confirmer(page: Page, libelle: string, motif?: string) {
  const d = page.getByRole('alertdialog')
  if (motif !== undefined) await d.getByLabel(/Motif/).fill(motif)
  await d.getByRole('button', { name: libelle }).click()
}

export async function naviguer(page: Page, entree: string) {
  await page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: entree, exact: true }).click()
}

/** Carte du système de conception (section.gm-card) désignée par son titre. */
export function carte(page: Page, titre: string | RegExp) {
  return page.locator('section.gm-card').filter({ has: page.getByRole('heading', { name: titre }) })
}

/** Section « Mon travail » du tableau de bord (D-58) : compteurs, tâches, liaisons et blocages de l'acteur. */
export function monTravail(page: Page) {
  return page.getByRole('region', { name: 'Mon travail' })
}

/** Tiroir d'exécution d'une opération du dossier de liaison (Drawer « Opération <rang> »). */
export function tiroir(page: Page, rang: string) {
  return page.getByRole('dialog', { name: `Opération ${rang}` })
}

/** Dossier numérique d'une liaison, sur un onglet et éventuellement une opération ouverte dans son tiroir. */
export function cheminDossier(liaison: string, onglet?: string, rang?: string) {
  const p = new URLSearchParams()
  if (onglet) p.set('onglet', onglet)
  if (rang) p.set('operation', `${liaison}:${rang}`)
  const q = p.toString()
  return `/des/parcours/${liaison}${q ? `?${q}` : ''}`
}

/** Fichier joint à un champ de dépôt (input[type=file] du ChampFichier, du FileDropzone ou du MessageComposer). */
export async function joindre(portee: Page | Locator, nom: string, contenu = 'contenu de démonstration', type = 'application/pdf') {
  await portee.locator('input[type="file"]').first().setInputFiles({ name: nom, mimeType: type, buffer: Buffer.from(contenu) })
}

/** Confirmation d'un ExecuteurActions (ConfirmDialog) par son bouton, motif facultatif. */
export async function confirmerAction(page: Page, libelle: string, motif?: string) {
  const d = page.getByRole('alertdialog')
  if (motif !== undefined) await d.getByRole('textbox').fill(motif)
  await d.getByRole('button', { name: libelle }).click()
  await expect(page.getByRole('alertdialog')).toHaveCount(0)
}

/** Onglet choisi, en réessayant tant qu'un rendu concurrent (retour d'une action) n'a pas pris le clic. */
export async function ongletSelectionne(portee: Page | Locator, nom: string) {
  const onglet = portee.getByRole('tab', { name: nom, exact: true })
  await expect(async () => {
    await onglet.click()
    await expect(onglet).toHaveAttribute('aria-selected', 'true', { timeout: 1000 })
  }).toPass({ timeout: 15_000 })
}

/** Signature sans motif (SignDialog) : clé de sécurité, confirmation, fermeture du dialogue. */
export async function signerCle(page: Page) {
  const d = page.getByRole('dialog').filter({ hasText: 'scellée dans le registre d\'audit' })
  await d.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
  await expect(d.getByText('Signature confirmée')).toBeVisible()
  await d.getByRole('button', { name: 'Continuer' }).click()
  await expect(d).toHaveCount(0)
}

/**
 * Capture plein écran d'une page dont le contenu défile dans l'enveloppe (et non dans le document) : la fenêtre
 * est agrandie à la hauteur du plus haut conteneur défilant le temps de la capture, puis rétablie.
 */
export async function capturePleinePage(page: Page, chemin: string) {
  const taille = page.viewportSize() ?? { width: 1440, height: 900 }
  const hauteur = await page.evaluate(() => Math.max(...Array.from(document.querySelectorAll('body, body *')).map((e) => e.scrollHeight)))
  await page.setViewportSize({ width: taille.width, height: Math.min(Math.max(hauteur, taille.height), 8000) })
  await page.waitForTimeout(300)
  await page.screenshot({ path: chemin, fullPage: true })
  await page.setViewportSize(taille)
}
