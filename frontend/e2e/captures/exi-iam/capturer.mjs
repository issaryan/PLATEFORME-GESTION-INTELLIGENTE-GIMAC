// Captures du lot IAM de l'épopée EXI (D-119) : console servie par Vite sur les services simulés
// (VITE_SERVICES=simule npx vite --port 5431 dans frontend/apps/console), Chromium sans tête, thèmes clair et sombre.
//   node frontend/e2e/captures/exi-iam/capturer.mjs <dossier de sortie> [récit…]
// Chaque récit est une suite d'étapes numérotées clic par clic ; aucune donnée réelle.
import { chromium } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'

const BASE = process.env.GIMAC_CAPTURES_URL ?? 'http://localhost:5431'
const sortie = process.argv[2] ?? 'captures-exi-iam'
const demandes = process.argv.slice(3)

/** Récits : profil de démonstration et étapes (nom de fichier, action avant la capture). */
export const RECITS = {
  'exi-06': {
    profil: 'ROLE_SUPER_ADMIN',
    etapes: [
      ['01-perimetres-suspendus', async (p) => { await p.goto(`${BASE}/console/sandbox`); await p.getByRole('cell', { name: '99001' }).waitFor() }],
      ['02-comptes-proteges', async (p) => { await p.getByRole('tab', { name: 'Comptes protégés' }).click(); await p.getByText('Awa Ndiaye', { exact: true }).waitFor() }],
    ],
  },
  'exi-04': {
    profil: 'ROLE_SUPER_ADMIN',
    etapes: [
      ['01-sandbox', async (p) => { await p.goto(`${BASE}/console/sandbox`); await p.getByRole('cell', { name: '99001' }).waitFor() }],
      ['02-mandats', async (p) => { await p.getByRole('tab', { name: 'Mandats de partenaire' }).click(); await p.getByText('Révoquée', { exact: true }).waitFor() }],
    ],
  },
  // US-EXI-24 : accès de prestataire, demandé puis approuvé par le RSSI (le super administrateur porte IAM_ACCOUNT_SECURITY).
  'exi-24': {
    profil: 'ROLE_RSSI',
    etapes: [
      ['01-acces-prestataire', async (p) => { await p.goto(`${BASE}/console/sandbox?onglet=prestataires`); await p.getByText('Montée de version de SBX-TRACE').waitFor() }],
      ['02-demande-saisie', async (p) => {
        await p.getByRole('button', { name: 'Demander un accès de prestataire' }).click()
        const d = p.getByRole('dialog')
        await d.getByLabel('Compte du prestataire').fill('presta.maintenance')
        await d.getByLabel('SBX-TRACE').check()
        const demain = new Date(Date.now() + 86_400_000)
        const jj = String(demain.getDate()).padStart(2, '0'); const mm = String(demain.getMonth() + 1).padStart(2, '0')
        await d.getByLabel(/^Début/).fill(`${jj}/${mm}/${demain.getFullYear()}`)
        await d.getByLabel('Heure', { exact: true }).fill('09:00')
        await d.getByLabel(/Motif de l’intervention/).fill('Correctif du relais de SBX-TRACE')
      }],
      ['03-demande-enregistree', async (p) => {
        await p.getByRole('dialog').getByRole('button', { name: 'Envoyer la demande' }).click()
        await p.getByText('Demande d’accès enregistrée : elle attend l’approbation du RSSI.').waitFor()
      }],
      ['04-approbation-dialogue', async (p) => {
        await p.getByRole('row').filter({ hasText: 'Montée de version de SBX-TRACE' }).getByRole('button', { name: 'Approuver l’accès' }).click()
        await p.getByRole('alertdialog').waitFor()
      }],
      ['05-approuve', async (p) => {
        await p.getByRole('alertdialog').getByRole('button', { name: 'Approuver l’accès' }).click()
        await p.getByText('Accès de prestataire approuvé.').waitFor()
      }],
      ['06-quatre-yeux', async (p) => {
        await p.getByRole('row').filter({ hasText: 'Correctif du relais de SBX-TRACE' }).getByRole('button', { name: 'Approuver l’accès' }).click()
        await p.getByRole('alertdialog').getByRole('button', { name: 'Approuver l’accès' }).click()
        await p.getByText(/L’auteur de la demande ne l’approuve pas/).waitFor()
      }],
    ],
  },
  // US-EXI-23 : compte verrouillé après cinq échecs, débloqué depuis la fiche du compte par le super administrateur.
  'exi-23': {
    profil: 'ROLE_SUPER_ADMIN',
    etapes: [
      ['01-compte-verrouille', async (p) => { await p.goto(`${BASE}/console/comptes/u-correspondant?onglet=sessions`); await p.getByText('Compte verrouillé après 5 échecs de connexion').waitFor() }],
      ['02-deblocage-motif', async (p) => {
        await p.getByRole('button', { name: 'Débloquer le compte' }).click()
        const d = p.getByRole('alertdialog')
        await d.getByLabel(/Motif/).fill('Appel du titulaire au support')
      }],
      ['03-compte-debloque', async (p) => {
        await p.getByRole('alertdialog').getByRole('button', { name: 'Débloquer le compte' }).click()
        await p.getByText('Compte débloqué.').waitFor()
      }],
    ],
  },
  // US-EXI-10 : registre d'audit de la sandbox (MS-AUDIT), filtre par institution, export et rebut, profil RSSI.
  'exi-10': {
    profil: 'ROLE_RSSI',
    etapes: [
      ['01-registre', async (p) => { await p.goto(`${BASE}/console/sandbox?onglet=registre`); await p.getByText('sbx-lecture.access-recorded.v1').first().waitFor() }],
      ['02-filtre-99001', async (p) => {
        await p.getByLabel(/^Institution/).fill('99001')
        await p.getByRole('button', { name: 'Filtrer' }).click()
        await p.getByText('99002', { exact: true }).first().waitFor({ state: 'detached' })
      }],
      ['03-export', async (p) => {
        await p.getByRole('button', { name: 'Exporter (CSV)' }).click()
        await p.getByText('Export du registre téléchargé (CSV, UTF-8).').waitFor()
      }],
      ['04-rebut', async (p) => { await p.getByText('Horodatage invalide').scrollIntoViewIfNeeded() }],
    ],
  },
}

const navigateur = await chromium.launch({ headless: true, executablePath: process.env.GIMAC_CHROMIUM })
try {
  for (const [nom, recit] of Object.entries(RECITS)) {
    if (demandes.length && !demandes.includes(nom)) continue
    const dossier = join(sortie, nom)
    mkdirSync(dossier, { recursive: true })
    for (const theme of ['clair', 'sombre']) {
      const contexte = await navigateur.newContext({ locale: 'fr-FR', timezoneId: 'Africa/Douala', viewport: { width: 1440, height: 900 } })
      await contexte.addInitScript((x) => {
        localStorage.setItem('gm.profilDemo', x.profil)
        localStorage.setItem('gm.theme', x.theme)
        if (!sessionStorage.getItem('gm.sessionDemo')) sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: '' }))
      }, { profil: recit.profil, theme: theme === 'clair' ? 'light' : 'dark' })
      const page = await contexte.newPage()
      page.setDefaultTimeout(60_000)
      for (const [fichier, action] of recit.etapes) {
        await action(page)
        await page.waitForTimeout(400)
        await page.screenshot({ path: join(dossier, `${fichier}-${theme}.png`), fullPage: true })
      }
      await contexte.close()
    }
    console.log(`${nom} : ${recit.etapes.length * 2} captures dans ${dossier}`)
  }
} finally {
  await navigateur.close()
}
