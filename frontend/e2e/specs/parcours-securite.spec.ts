import { ADRESSES, demo, expect, test } from '../fixtures'
import { carte, cheminDossier, confirmerAction, joindre, monTravail, signer, tiroir } from './outils'

// Fonctionnalité : verrou 2, approbation de la sécurité (D-56, rangs 20 à 26), et visas optionnels :
// la production monétique lit le dossier de changement et référence la fiche de changement ; la sécurité
// des SI vérifie, rejette ou archive les attestations du participant ; la conformité et la direction
// générale signent leur visa, motivé, avec leur clé de sécurité. Scénario LIA-2026-019 : attestations
// fournies, visas activés. Services simulés.

/**
 * CER-02 : un visa se signe sur l'opération ouverte de l'approbation (rang 23), ouverte par l'archivage des attestations.
 * La sécurité des SI vérifie et archive celles de LIA-2026-019, puis l'acteur du visa prend la main (démonstration continue).
 */
async function attestationsArchivees(page: import('@playwright/test').Page, profil: string) {
  await page.goto(`${ADRESSES.departement}/favicon.svg`)
  await page.evaluate(() => { localStorage.setItem('gm.profilDemo', 'ROLE_RSSI'); localStorage.setItem('gm.demoContinue', '1') })
  await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-019', 'decision')}`)
  await page.getByRole('button', { name: 'Déclarer conforme' }).first().click()
  await confirmerAction(page, 'Déclarer conforme')
  await page.getByRole('button', { name: 'Archiver les attestations' }).click()
  await confirmerAction(page, 'Archiver les attestations')
  await expect(page.getByText('Vérifiée et archivée').first()).toBeVisible()
  await page.evaluate((r) => localStorage.setItem('gm.profilDemo', r), profil)
}

test.describe('Fonctionnalité : approbation de la sécurité et visas', () => {
  test('Scénario : la production monétique référence puis relit la fiche de changement', async ({ page }) => {
    await demo(page, { profil: 'ROLE_PRODUCTION_MONETIQUE', theme: 'light' })
    await test.step('Étant donné la production monétique et ses compteurs', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-nom', { hasText: 'Serge Ondoua Essomba' })).toBeVisible()
      const compteurs = monTravail(page).locator('[aria-label="Mes compteurs"]')
      for (const c of ['Dossiers de changement à constituer', 'Opérations de branche à faire', 'Rapports de mise en production']) await expect(compteurs.getByText(c, { exact: true })).toBeVisible()
    })
    await test.step('Et le dossier de changement déjà constitué de LIA-2026-019', async () => {
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-019', 'decision')}`)
      const verrou2 = carte(page, 'Verrou 2 : approbation de la sécurité')
      await expect(verrou2.getByText('Dossier de changement', { exact: true })).toBeVisible()
      await expect(verrou2.getByText('dossier-changement-LIA-2026-019.pdf')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Déclarer conforme' })).toHaveCount(0)
      await page.getByRole('button', { name: 'Référencer la fiche de changement' }).click()
      await expect(page.getByRole('dialog').getByRole('button', { name: 'Référencer la fiche de changement' })).toBeDisabled()
    })
    await test.step('Quand il saisit une fiche au mauvais format, le refus s’affiche sous le champ (CER-10)', async () => {
      const f = page.getByRole('dialog')
      await f.getByRole('textbox', { name: /Référence de la fiche/ }).fill('chg412')
      await expect(f.getByText(/Référence de fiche de changement invalide/)).toBeVisible()
      await expect(f.getByRole('button', { name: 'Référencer la fiche de changement' })).toBeDisabled()
      await expect(page.getByRole('alertdialog')).toHaveCount(0)
    })
    await test.step('Et quand il la référence au format CHG-AAAA-N', async () => {
      await page.getByRole('dialog').getByRole('textbox', { name: /Référence de la fiche/ }).fill('CHG-2026-412')
      await page.getByRole('dialog').getByRole('button', { name: 'Référencer la fiche de changement' }).click()
      await confirmerAction(page, 'Référencer la fiche de changement')
    })
    await test.step('Alors la fiche est lue, en examen, et l’approbation attend l’archivage des attestations', async () => {
      await expect(page.getByText('CHG-2026-412')).toBeVisible()
      await expect(page.getByText('En examen')).toBeVisible()
      await page.getByRole('button', { name: 'Relire la fiche de changement' }).click()
      await expect(page.getByText(/une fois les attestations vérifiées et archivées/)).toBeVisible()
    })
  })

  test('Scénario : la production monétique crée le membre sur le switch, valeur de référence exigée', async ({ page }) => {
    await demo(page, { profil: 'ROLE_PRODUCTION_MONETIQUE', theme: 'light' })
    await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-012', 'operations', 'C2')}`)
    const t = tiroir(page, 'C2')
    await test.step('Quand il dépose la preuve sans la valeur de référence relevée', async () => {
      await joindre(t, 'membre-switch.png', 'capture', 'image/png')
      await t.getByRole('button', { name: 'Déposer la preuve' }).click()
      await expect(t.getByText('Cette preuve doit porter la valeur de référence relevée.')).toBeVisible()
    })
    await test.step('Et qu’il la complète du code du membre', async () => {
      await t.getByLabel(/Code du membre/).fill('M-0061')
      await t.getByRole('button', { name: 'Déposer la preuve' }).click()
      await expect(page.getByRole('alertdialog').getByText('M-0061')).toBeVisible()
      await confirmerAction(page, 'Déposer la preuve')
    })
    await test.step('Alors la preuve attend la validation du conducteur de projet', async () => {
      await expect(t.getByText(/Preuve déposée, en attente de la validation de Brice Ondo Mba/)).toBeVisible()
      await expect(t.getByText('M-0061')).toBeVisible()
    })
  })

  test('Scénario : la sécurité des SI déclare l’attestation conforme puis archive', async ({ page }) => {
    await demo(page, { profil: 'ROLE_RSSI', theme: 'light' })
    await test.step('Étant donné l’attestation PCI DSS fournie par le participant de LIA-2026-019', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      const compteurs = monTravail(page).locator('[aria-label="Mes compteurs"]')
      for (const c of ['Attestations à vérifier', 'Approbations en attente', 'Cérémonies de clés']) await expect(compteurs.getByText(c, { exact: true })).toBeVisible()
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-019', 'decision')}`)
      await expect(page.getByText('1 attestation fournie attend votre vérification.')).toBeVisible()
      await expect(page.getByText('Activité carte : attestation PCI DSS exigée.')).toBeVisible()
    })
    await test.step('Quand il la déclare conforme', async () => {
      await page.getByRole('button', { name: 'Déclarer conforme' }).click()
      await confirmerAction(page, 'Déclarer conforme')
      await expect(page.getByText('Conforme', { exact: true })).toBeVisible()
    })
    await test.step('Et qu’il archive les attestations vérifiées', async () => {
      await page.getByRole('button', { name: 'Archiver les attestations' }).click()
      await confirmerAction(page, 'Archiver les attestations')
    })
    await test.step('Alors les attestations sont vérifiées et archivées au dossier', async () => {
      await expect(page.getByText('Vérifiée et archivée')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Archiver les attestations' })).toHaveCount(0)
    })
  })

  test('Scénario : la sécurité des SI rejette une attestation, motif à l’appui', async ({ page }) => {
    await demo(page, { profil: 'ROLE_RSSI', theme: 'dark' })
    await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-019', 'decision')}`)
    await test.step('Quand il rejette l’attestation en motivant', async () => {
      await page.getByRole('button', { name: 'Rejeter' }).click()
      await confirmerAction(page, 'Rejeter', 'Attestation expirée depuis le trimestre dernier.')
    })
    await test.step('Alors elle est à fournir de nouveau, avec le motif communiqué', async () => {
      await expect(page.getByText('Rejetée, à fournir de nouveau')).toBeVisible()
      await expect(page.getByText('Motif : Attestation expirée depuis le trimestre dernier.')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Déclarer conforme' })).toHaveCount(0)
    })
  })

  test('Scénario : la conformité vise favorablement, motif et clé de sécurité à l’appui', async ({ page }) => {
    await demo(page, { theme: 'light' })
    await attestationsArchivees(page, 'ROLE_CONFORMITE')
    await test.step('Étant donné la conformité et le visa attendu de LIA-2026-019', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      const compteurs = monTravail(page).locator('[aria-label="Mes compteurs"]')
      await expect(compteurs.getByText('Visas attendus', { exact: true })).toBeVisible()
      // CER-02 : la tâche du visa figure dans « Mes tâches » et mène à l'onglet Décision.
      await expect(monTravail(page).getByRole('link', { name: 'Signer : Visa de la conformité' })).toHaveAttribute('href', /LIA-2026-019\?onglet=decision/)
      await expect(compteurs.getByText('Fintechs sous contrôle COBAC', { exact: true })).toBeVisible()
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-019', 'decision')}`)
      await expect(page.getByRole('region', { name: 'Visa de la direction générale' }).getByRole('button')).toHaveCount(0)
    })
    await test.step('Quand elle vise favorablement, motif et clé de sécurité à l’appui', async () => {
      await page.getByRole('region', { name: 'Visa de la conformité' }).getByRole('button', { name: 'Viser favorablement' }).click()
      await signer(page, 'Dossier conforme aux exigences réglementaires.')
      await expect(page.getByRole('dialog').getByText('Signature confirmée')).toBeVisible()
      await page.getByRole('dialog').getByRole('button', { name: 'Continuer' }).click()
    })
    await test.step('Alors son visa est favorable, signé et motivé', async () => {
      const visa = page.getByRole('region', { name: 'Visa de la conformité' })
      await expect(visa.getByText('Favorable')).toBeVisible()
      await expect(visa.getByText('Dossier conforme aux exigences réglementaires.')).toBeVisible()
      await expect(visa.getByRole('button')).toHaveCount(0)
    })
  })

  test('Scénario : la direction générale vise défavorablement, motif obligatoire', async ({ page }) => {
    await demo(page, { theme: 'light' })
    await attestationsArchivees(page, 'ROLE_DG')
    await test.step('Étant donné la direction générale, son cockpit et son visa attendu', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-nom', { hasText: 'Jean-Baptiste Ella Mintsa' })).toBeVisible()
      await expect(page.getByRole('region', { name: 'Pilotage des projets de certification' })).toBeVisible()
      await expect(monTravail(page).locator('[aria-label="Mes compteurs"]').getByText('Visas attendus')).toBeVisible()
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-019', 'decision')}`)
    })
    await test.step('Quand elle vise défavorablement, la clé n’est proposée qu’une fois le motif saisi', async () => {
      await page.getByRole('region', { name: 'Visa de la direction générale' }).getByRole('button', { name: 'Viser défavorablement' }).click()
      const d = page.getByRole('dialog')
      await expect(d.getByRole('button', { name: 'Utiliser ma clé de sécurité' })).toBeDisabled()
      await signer(page, 'Risque opérationnel non couvert à ce jour.')
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
    })
    await test.step('Alors le visa est défavorable et l’approbation ne pourra pas être prononcée', async () => {
      await expect(page.getByRole('region', { name: 'Visa de la direction générale' }).getByText('Défavorable')).toBeVisible()
    })
  })
})
