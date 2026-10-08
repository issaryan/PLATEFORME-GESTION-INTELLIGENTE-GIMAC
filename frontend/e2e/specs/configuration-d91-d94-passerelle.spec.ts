import { ADRESSES, expect, test } from '../fixtures'
import { signerEtTerminer } from './outils'
import { Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'
import { catalogueE2E } from './passerelle-catalogue'
import { PasserelleDes } from './passerelle-des'
import { PasserellePortail } from './passerelle-portail'

// Fonctionnalités D-94 et MS-CERTIFICATION §10.5 sur la passerelle (VITE_SERVICES=http) : catalogue des fonctionnalités
// servi par MS-IAM (GET /bff/{des,console}/iam/configuration/fonctionnalites) et modifié par PUT …/{cle} avec motif et
// assertion FIDO2 ; repli prudent quand il n'est pas servi ; procès-verbal signé par le participant avec la finalité
// SIGNATURE_OPERATION, circuit de signature affiché et refus CER_* traduits.

test.describe('Fonctionnalité : catalogue des fonctionnalités par la passerelle (D-94)', () => {
  test('Scénario : catalogue non servi, repli prudent sur l’accueil avec un message clair', async ({ page }) => {
    const gw = new PasserelleDes()
    await gw.brancher(page)
    catalogueE2E(page).servi = false
    await page.goto(`${ADRESSES.departementHttp}/des/tableau-de-bord`)
    await expect(page.getByText('Catalogue des fonctionnalités indisponible').first()).toBeVisible()
    await page.goto(`${ADRESSES.departementHttp}/des/institutions`)
    await expect(page.getByText('Catalogue des fonctionnalités indisponible').first()).toBeVisible()
    const nav = page.getByRole('navigation', { name: 'Navigation principale' })
    await expect(nav.getByRole('link', { name: 'Tableau de bord', exact: true })).toBeVisible()
    await expect(nav.getByRole('link', { name: 'Liaisons et parcours', exact: true })).toHaveCount(0)
  })

  test('Scénario : console sans catalogue servi, l’accueil seul ; écran gardé indisponible', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    catalogueE2E(page).servi = false
    await page.goto(`${ADRESSES.consoleHttp}/console/profils`)
    await expect(page.getByText('Catalogue des fonctionnalités indisponible').first()).toBeVisible()
    const nav = page.getByRole('navigation', { name: 'Navigation principale' })
    await expect(nav.getByRole('link', { name: 'Tableau de bord', exact: true })).toBeVisible()
    await expect(nav.getByRole('link', { name: 'Profils et permissions', exact: true })).toHaveCount(0)
  })

  test('Scénario : le super administrateur renomme une fonctionnalité ; PUT signé avec le motif', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/profils?vue=fonctionnalites`)
    await page.getByLabel('Application').selectOption('console')
    await page.getByRole('button', { name: 'Modifier la fonctionnalité consoleComptes' }).click()
    const d = page.getByRole('dialog', { name: 'Modifier la fonctionnalité' })
    await d.getByLabel(/^Libellé/).fill('Comptes des agents')
    await d.getByLabel(/^Ordre/).fill('205')
    await d.getByRole('button', { name: 'Enregistrer la fonctionnalité' }).click()
    await signerEtTerminer(page, 'Libellé harmonisé')
    const [m] = catalogueE2E(page).modifications
    expect(m.cle).toBe('consoleComptes')
    expect(m.motif).toBe('Libellé harmonisé')
    expect(m.corps).toMatchObject({ libelle: 'Comptes des agents', ordre: 205, application: 'console', visible: true })
    await expect(page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Comptes des agents', exact: true })).toBeVisible()
  })
})

test.describe('Fonctionnalité : signature du procès-verbal par le participant (MS-CERTIFICATION §10.5)', () => {
  test('Scénario : circuit affiché, refus de la certification traduits, puis signature SIGNATURE_OPERATION', async ({ page }) => {
    const gw = new PasserellePortail({ pvASigner: { refus: ['CER_SIGNATAIRE_NON_DESIGNE', 'CER_OPERATION_NON_OUVERTE'] } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.portailHttp}/portail/liaisons/LIA-2026-061/proces-verbal`)

    await test.step('Étant donné le circuit : GIMAC signé (ordre 1), participant attendu (ordre 2)', async () => {
      const circuit = page.locator('section.gm-card').filter({ has: page.getByRole('heading', { name: 'Circuit de signature' }) })
      await expect(circuit.getByText('Signature du GIMAC')).toBeVisible()
      await expect(circuit.getByText('Portail du participant · ordre 2')).toBeVisible()
      await expect(circuit.getByText('Attendue')).toBeVisible()
    })

    const d = page.getByRole('dialog', { name: 'Signer le procès-verbal' })
    await test.step('Quand la certification refuse : signataire non désigné, puis opération non ouverte', async () => {
      await page.getByRole('button', { name: 'Signer le procès-verbal' }).click()
      await d.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
      await expect(d.getByText(/Vous ne tenez pas la fonction signataire attendue à cette étape du circuit/)).toBeVisible()
      await d.getByRole('button', { name: 'Réessayer' }).click()
      await expect(d.getByText(/L'opération qui porte ce geste n'est pas ouverte/)).toBeVisible()
    })
    await test.step('Alors la signature aboutit, demandée avec la finalité SIGNATURE_OPERATION', async () => {
      await d.getByRole('button', { name: 'Réessayer' }).click()
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
      await expect(page.getByText('Signé des deux parties')).toBeVisible()
      expect(new Set(gw.finalites)).toEqual(new Set(['SIGNATURE_OPERATION']))
    })
  })
})
