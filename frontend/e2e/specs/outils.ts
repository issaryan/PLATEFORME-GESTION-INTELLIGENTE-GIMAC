import { expect, type Page } from '@playwright/test'

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
