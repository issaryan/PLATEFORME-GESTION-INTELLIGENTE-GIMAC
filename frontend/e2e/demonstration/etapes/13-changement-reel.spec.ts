import { createHash } from 'node:crypto'
import { writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { test, type Page } from '@playwright/test'
import { ADRESSES, Journal, appelerPasserelle, expect, seConnecter } from '../outils'

const INSTANCE = (id: string) => `/bff/des/pilotage/processus/instances/${encodeURIComponent(id)}`

async function lireInstance(page: Page, id: string) {
  const lecture = await appelerPasserelle(page, 'GET', INSTANCE(id))
  expect(lecture.statut, JSON.stringify(lecture.corps)).toBe(200)
  return lecture.corps
}

async function telechargerPdf(page: Page, id: string): Promise<Buffer> {
  const pdf = await page.evaluate(async (chemin) => {
    const reponse = await fetch(chemin, { credentials: 'include', headers: { Accept: 'application/pdf' } })
    const octets = new Uint8Array(await reponse.arrayBuffer())
    let binaire = ''
    for (let i = 0; i < octets.length; i += 0x8000) binaire += String.fromCharCode(...octets.subarray(i, i + 0x8000))
    return { statut: reponse.status, type: reponse.headers.get('content-type'), base64: btoa(binaire) }
  }, `${INSTANCE(id)}/pdf`)
  expect(pdf.statut).toBe(200)
  expect(pdf.type).toContain('application/pdf')
  const contenu = Buffer.from(pdf.base64, 'base64')
  expect(contenu.subarray(0, 5).toString('ascii')).toBe('%PDF-')
  expect(contenu.length).toBeGreaterThan(1000)
  return contenu
}

/** Authentificateur CTAP2 natif de Chromium : les appels navigator.credentials restent ceux de l'application. */
async function activerAuthentificateur(page: Page) {
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('WebAuthn.enable', { enableUI: false })
  await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: {
      protocol: 'ctap2',
      transport: 'internal',
      hasResidentKey: false,
      hasUserVerification: true,
      isUserVerified: true,
      automaticPresenceSimulation: true,
    },
  })
}

async function enrolerCle(page: Page, journal: Journal, nom: string, capture: string) {
  await page.goto(`${ADRESSES.departement}/des/mon-compte`)
  await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
  expect(await page.evaluate(() => window.isSecureContext && typeof PublicKeyCredential !== 'undefined')).toBe(true)
  await activerAuthentificateur(page)
  await page.getByRole('button', { name: 'Enrôler une clé' }).click()
  const dialogue = page.getByRole('dialog')
  await expect(dialogue).toBeVisible()
  await dialogue.locator('input[name="nomNouvelleCle"]').fill(nom)
  const enregistrement = page.waitForResponse((r) => r.url().includes('/bff/des/iam/moi/cles-securite') && !r.url().endsWith('/defi') && r.request().method() === 'POST')
  await dialogue.getByRole('button', { name: 'Enrôler une clé' }).click()
  const reponse = await enregistrement
  expect(reponse.status(), await reponse.text()).toBe(200)
  await expect(page.getByText(`La clé « ${nom} » est enrôlée.`, { exact: true })).toBeVisible()
  await expect(page.getByText(nom, { exact: true }).first()).toBeVisible()
  await journal.capturer(page, capture)
}

async function terminerEtape(page: Page, id: string, code: string) {
  await page.getByRole('button', { name: 'Terminer l’étape' }).click()
  const dialogue = page.getByRole('alertdialog')
  await expect(dialogue).toBeVisible()
  const reponse = page.waitForResponse((r) => r.url().includes(`${INSTANCE(id)}/etapes/${code}/fin`) && r.request().method() === 'POST')
  await dialogue.getByRole('button', { name: 'Terminer l’étape' }).click()
  expect((await reponse).status()).toBe(200)
}

// Preuve intégrale sur MS-PILOTAGE, MS-DOCUMENT et MS-IAM réels. Chromium fournit les authentificateurs CTAP2 ; aucune
// attestation ou assertion n'est fabriquée par le test, et le PDF intermédiaire comme final vient de Gotenberg.
test('13, changement complet, signature FIDO2 et PDF final réel', async ({ page, browser }) => {
  test.setTimeout(300_000)
  const journal = new Journal('13-changement')
  const responsableContext = await browser.newContext({ locale: 'fr-FR', timezoneId: 'Africa/Douala', viewport: { width: 1440, height: 900 } })
  const ssiContext = await browser.newContext({ locale: 'fr-FR', timezoneId: 'Africa/Douala', viewport: { width: 1440, height: 900 } })
  const responsablePage = await responsableContext.newPage()
  const ssiPage = await ssiContext.newPage()
  // Les contextes additionnels sont fermés par Playwright avec le navigateur.
  {
    await seConnecter(page, ADRESSES.departement, 'demo.admin.gimac')

    const lancables = await appelerPasserelle(page, 'GET', '/bff/des/pilotage/processus/lancables')
    expect(lancables.statut, JSON.stringify(lancables.corps)).toBe(200)
    const definitions = lancables.corps.groupes.flatMap((g: { processus: { code: string }[] }) => g.processus)
    const definition = definitions.find((p: { code: string }) => p.code === 'DEMANDE_CHANGEMENT')
    expect(definition, 'le modèle DEMANDE_CHANGEMENT est publié et lancable').toBeTruthy()

    const candidats = await appelerPasserelle(page, 'GET', '/bff/des/pilotage/processus/candidats?unite=SSI&profil=ROLE_PRODUCTION_MONETIQUE')
    expect(candidats.statut).toBe(200)
    const personnes = [...candidats.corps.dansUnite, ...candidats.corps.ailleurs]
    const responsable = personnes.find((p: { nomAffiche: string }) => p.nomAffiche === 'Admin 2 GIMAC démonstration')
    expect(responsable, JSON.stringify(candidats.corps)).toBeTruthy()
    const champs = {
      TYPE_DEMANDEUR: 'SUPPORT_MONETIQUE',
      ELEMENT_CONFIGURATION: 'AMA',
      IDENTIFIANT_EC: 'EC-AMA-RECETTE',
      PRIORITE: 'NORMAL',
      DECLENCHEUR: 'PROJET',
      DEMANDEUR_CONTACT: 'demo.admin.gimac@gimac.test',
      RAISON: 'Vérifier le cycle complet et le PDF final signé de la fiche de changement.',
      ELEMENT_IMPACTE: 'Aucun impact sur les autres éléments de configuration.',
      DESCRIPTION_IMPACT: 'Aucun impact fonctionnel attendu.',
      RESPONSABLE_CONTACT: 'demo.admin2.gimac@gimac.test',
      PLAN_TEST: 'Déposer le dossier, renseigner la décision SSI et vérifier la signature et le PDF final.',
      PLAN_RETOUR: 'Annuler la demande de recette et conserver son historique.',
    }
    const lancement = await appelerPasserelle(page, 'POST', `/bff/des/pilotage/processus/${definition.id}/instances`, {
      intitule: `Recette changement signé du ${new Date().toISOString().slice(0, 10)}`,
      formulaire: champs,
      designations: { IMPLEMENTATION: responsable.id },
    })
    expect(lancement.statut, JSON.stringify(lancement.corps)).toBe(201)
    const instance = lancement.corps
    // Format de la fiche de changement (CHG-AAAA-N, D-127 point 5), admis par ApprobationPolicy au verrou 2.
    expect(instance.reference).toMatch(/^CHG-\d{4}-\d{1,6}$/)

    for (const etape of ['IDENTIFICATION', 'RAISON', 'IMPACT']) {
      const fin = await appelerPasserelle(page, 'POST', `${INSTANCE(instance.id)}/etapes/${etape}/fin`, {})
      expect(fin.statut, `${etape}: ${JSON.stringify(fin.corps)}`).toBe(200)
    }

    await page.goto(`${ADRESSES.departement}/des/processus/instances/${instance.id}?etape=IMPLEMENTATION`)
    await expect(page.getByRole('heading', { level: 1, name: instance.intitule })).toBeVisible()
    await expect(page.getByText(instance.reference, { exact: true }).first()).toBeVisible()
    await journal.capturer(page, 'demande-changement-reelle')
    const dossier = await telechargerPdf(page, instance.id)
    const empreinteDossier = createHash('sha256').update(dossier).digest('hex')

    // Étape 4 : le responsable désigné enrôle sa propre clé dans Mon compte, puis dépose le vrai PDF par l'interface.
    await seConnecter(responsablePage, ADRESSES.departement, 'demo.admin2.gimac')
    await enrolerCle(responsablePage, journal, 'Clé CDP responsable changement', 'cle-responsable-enrolee')
    await responsablePage.goto(`${ADRESSES.departement}/des/processus/instances/${instance.id}?etape=IMPLEMENTATION`)
    await expect(responsablePage.getByRole('heading', { level: 1, name: instance.intitule })).toBeVisible()
    await responsablePage.locator('section[aria-label="Livrables"] input[type="file"]').setInputFiles({
      name: `${instance.reference}-dossier.pdf`, mimeType: 'application/pdf', buffer: dossier,
    })
    const rattachement = responsablePage.waitForResponse((r) => r.url().includes(`${INSTANCE(instance.id)}/etapes/IMPLEMENTATION/livrables`) && r.request().method() === 'POST')
    await responsablePage.getByRole('button', { name: 'Envoyer DOSSIER_CHANGEMENT' }).click()
    expect((await rattachement).status()).toBe(200)
    await expect(responsablePage.getByText(`SHA-256 ${empreinteDossier}`, { exact: true })).toBeVisible({ timeout: 60_000 })
    await journal.capturer(responsablePage, 'livrable-pdf-depose')
    const apresDepot = await lireInstance(responsablePage, instance.id)
    const depot = apresDepot.etapes.find((e: { code: string }) => e.code === 'IMPLEMENTATION').livrables
      .find((l: { code: string }) => l.code === 'DOSSIER_CHANGEMENT').depot
    expect(depot.typeMime).toBe('application/pdf')
    expect(depot.sha256).toBe(empreinteDossier)
    await terminerEtape(responsablePage, instance.id, 'IMPLEMENTATION')
    const apresImplementation = await lireInstance(responsablePage, instance.id)
    expect(apresImplementation.etapes.find((e: { code: string }) => e.code === 'IMPLEMENTATION').etat).toBe('TERMINEE')
    expect(apresImplementation.etapes.find((e: { code: string }) => e.code === 'SSI').etat).toBe('ACTIVE')

    // Étape 5 : le RSSI (D-130, seul signataire du verrou 2) enrôle une autre clé, renseigne les champs, termine puis
    // signe avec la clé et MS-IAM. Le compte demo.certificateur des preuves du 30/10 ne porte que ROLE_AGENT_SSI.
    const rssi = process.env.GIMAC_DEMO_COMPTE_RSSI
    if (!rssi) throw new Error('D-130 : compte de démonstration portant ROLE_RSSI attendu dans GIMAC_DEMO_COMPTE_RSSI')
    await seConnecter(ssiPage, ADRESSES.departement, rssi)
    await enrolerCle(ssiPage, journal, 'Clé CDP RSSI', 'cle-ssi-enrolee')
    await ssiPage.goto(`${ADRESSES.departement}/des/processus/instances/${instance.id}?etape=SSI`)
    await expect(ssiPage.getByRole('heading', { level: 1, name: instance.intitule })).toBeVisible()
    const prendre = ssiPage.getByRole('button', { name: 'Prendre l’étape' })
    if (await prendre.isVisible()) {
      await prendre.click()
      await expect(prendre).toBeHidden()
    }
    const valeurs = {
      DATE_DEPLOIEMENT: '2026-11-02',
      DATE_REVUE: '2026-11-03',
      EVALUATION: 'Risque maîtrisé par le plan de test et le plan de retour.',
      DECISION: 'ACCEPTEE',
      DESCRIPTION: 'Décision favorable après revue du dossier de changement déposé.',
    }
    await ssiPage.locator('[name="valeurs.DATE_DEPLOIEMENT"]').fill('02/11/2026')
    await ssiPage.locator('[name="valeurs.DATE_REVUE"]').fill('03/11/2026')
    await ssiPage.locator('[name="valeurs.EVALUATION"]').fill(valeurs.EVALUATION)
    await ssiPage.locator('[name="valeurs.DECISION"]').selectOption(valeurs.DECISION)
    await ssiPage.locator('[name="valeurs.DESCRIPTION"]').fill(valeurs.DESCRIPTION)
    const enregistrement = ssiPage.waitForResponse((r) => r.url().includes(`${INSTANCE(instance.id)}/etapes/SSI/champs`) && r.request().method() === 'PUT')
    await ssiPage.getByRole('button', { name: 'Enregistrer les champs' }).click()
    expect((await enregistrement).status()).toBe(200)
    await journal.capturer(ssiPage, 'champs-ssi-renseignes')
    const apresChamps = await lireInstance(ssiPage, instance.id)
    const champsSsi = apresChamps.etapes.find((e: { code: string }) => e.code === 'SSI').champs
    for (const [code, valeur] of Object.entries(valeurs)) expect(champsSsi.find((c: { code: string }) => c.code === code).valeur).toBe(valeur)
    await terminerEtape(ssiPage, instance.id, 'SSI')
    const aSigner = await lireInstance(ssiPage, instance.id)
    expect(aSigner.etapes.find((e: { code: string }) => e.code === 'SSI').etat).toBe('A_SIGNER')
    await expect(ssiPage.getByRole('button', { name: 'Signer', exact: true })).toBeVisible()
    await journal.capturer(ssiPage, 'decision-ssi-a-signer')

    const empreinteSignature = createHash('sha256').update(`DOSSIER_CHANGEMENT:${depot.documentId}:${depot.sha256}\n`).digest('hex')
    await ssiPage.getByRole('button', { name: 'Signer', exact: true }).click()
    const dialogueSignature = ssiPage.getByRole('dialog')
    await expect(dialogueSignature).toBeVisible()
    await expect(dialogueSignature.locator(`[title="${empreinteSignature}"]`).first()).toBeVisible()
    await journal.capturer(ssiPage, 'signature-empreinte-controlee')
    const signature = ssiPage.waitForResponse((r) => r.url().includes(`${INSTANCE(instance.id)}/etapes/SSI/signature`) && r.request().method() === 'POST')
    await dialogueSignature.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
    const reponseSignature = await signature
    expect(reponseSignature.status(), await reponseSignature.text()).toBe(200)
    await expect(dialogueSignature.getByText('Signature confirmée', { exact: true })).toBeVisible()
    await journal.capturer(ssiPage, 'signature-fido2-confirmee')
    await dialogueSignature.getByRole('button', { name: 'Continuer' }).click()

    const instanceFinale = await lireInstance(ssiPage, instance.id)
    expect(instanceFinale.etat).toBe('CLOSE')
    expect(instanceFinale.avancement).toEqual({ total: 5, terminees: 5 })
    const decision = instanceFinale.etapes.find((e: { code: string }) => e.code === 'SSI').signatures
      .find((s: { code: string }) => s.code === 'DECISION_SSI')
    expect(decision.etat).toBe('SIGNEE')
    expect(decision.empreinte).toBe(empreinteSignature)
    expect(decision.documents).toEqual([{ livrable: 'DOSSIER_CHANGEMENT', documentId: depot.documentId, sha256: depot.sha256 }])
    expect(instanceFinale.historique.some((h: { action: string; detail: string | null }) => h.action === 'SIGNEE' && h.detail?.includes(empreinteSignature))).toBe(true)
    await expect(ssiPage.getByText('Close', { exact: true }).first()).toBeVisible()
    await expect(ssiPage.getByText(`SHA-256 ${empreinteSignature}`, { exact: true })).toBeVisible()
    expect(await ssiPage.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    await ssiPage.evaluate(() => window.scrollTo(0, 0))
    await ssiPage.locator('[data-testid="gm-shell-contenu"]').evaluate((zone) => zone.scrollTo({ top: 0, left: 0 }))
    await journal.capturer(ssiPage, 'instance-close-signee')

    const pdfFinal = await telechargerPdf(ssiPage, instance.id)
    const destination = join(process.cwd(), '..', 'docs', 'recette', 'demonstration', '13-changement', `${instance.reference}.pdf`)
    writeFileSync(destination, pdfFinal)
  }
})
