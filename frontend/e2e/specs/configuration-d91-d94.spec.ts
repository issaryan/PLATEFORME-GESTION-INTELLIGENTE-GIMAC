import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { cheminDossier, confirmerAction, joindre, naviguer, signerCle, signerEtTerminer, tiroir } from './outils'

// Fonctionnalités D-91 à D-94 en services simulés : catalogue des fonctionnalités modifié par le super administrateur
// (barre latérale et gardes qui suivent), profil externe, types de projet et fonctions (permissions comprises), onglet
// « Signatures » du modélisateur, opération signée dans l'ordre, renvoi motivé, affectation d'une fonction, tâche
// offerte par permission, administration de tous les domaines des processus, portée « Toutes les unités » de la
// matrice, notifications d'homologation (application interne et portail). Captures en thème clair et sombre dans
// e2e/captures/d91-d94 (ou $GIMAC_CAPTURES/d91-d94).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd91-d94') : resolve(ICI, '..', 'captures', 'd91-d94')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
/** Démonstration continue (D-98) : l'acteur change par la session de démonstration, sur le même état. */
async function acteur(page: Page, role: string, adresse: string) {
  await page.evaluate((r) => sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: '', role: r })), role)
  await page.goto(adresse)
}
const nav = (page: Page) => page.getByRole('navigation', { name: 'Navigation principale' })
/** Navigation interne sans rechargement (l'état des services simulés vit dans la page). */
async function allerSansRecharger(page: Page, chemin: string) {
  await page.evaluate((c) => {
    window.history.pushState({}, '', c)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }, chemin)
}

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'

  test.describe(`Fonctionnalité : écrans pilotés par la configuration (D-94, ${suffixe})`, () => {
    test('Scénario : le super administrateur renomme et masque une fonctionnalité ; la barre latérale et les gardes suivent', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)

      await test.step('Étant donné le catalogue des fonctionnalités de la console', async () => {
        await naviguer(page, 'Profils et permissions')
        await page.getByRole('tab', { name: 'Fonctionnalités' }).click()
        await expect(page).toHaveURL(/vue=fonctionnalites/)
        await page.getByLabel('Application').selectOption('console')
        await expect(page.getByRole('row').filter({ hasText: 'consoleOrganigramme' })).toBeVisible()
        await capturer(page, `fonctionnalites-catalogue-${suffixe}`)
      })

      await test.step('Quand il renomme l’organigramme (motif et clé de sécurité)', async () => {
        await page.getByRole('button', { name: 'Modifier la fonctionnalité consoleOrganigramme', exact: true }).click()
        const d = page.getByRole('dialog', { name: 'Modifier la fonctionnalité' })
        await d.getByLabel(/^Libellé/).fill('Organigramme du GIMAC')
        await capturer(page, `fonctionnalites-modale-${suffixe}`)
        await d.getByRole('button', { name: 'Enregistrer la fonctionnalité' }).click()
        await signerEtTerminer(page, 'Libellé demandé par la direction')
      })

      await test.step('Alors la barre latérale porte le nouveau libellé, sans nouvelle construction', async () => {
        await expect(nav(page).getByRole('link', { name: 'Organigramme du GIMAC', exact: true })).toBeVisible()
      })

      await test.step('Quand il masque le journal d’administration', async () => {
        await page.getByRole('button', { name: 'Modifier la fonctionnalité consoleJournal', exact: true }).click()
        const d = page.getByRole('dialog', { name: 'Modifier la fonctionnalité' })
        await d.getByRole('switch', { name: 'Visible' }).click()
        await expect(d.getByText(/Masquée : la fonctionnalité disparaît/)).toBeVisible()
        await d.getByRole('button', { name: 'Enregistrer la fonctionnalité' }).click()
        await signerEtTerminer(page, 'Écran retiré de la console')
      })

      await test.step('Alors l’entrée disparaît et l’écran est refusé', async () => {
        await expect(nav(page).getByRole('link', { name: 'Journal d’administration', exact: true })).toHaveCount(0)
        await expect(page.getByRole('row').filter({ hasText: 'consoleJournal' }).getByText('Masquée')).toBeVisible()
        await allerSansRecharger(page, '/console/journal')
        await expect(page.getByText('Accès refusé')).toBeVisible()
        await capturer(page, `fonctionnalites-refus-${suffixe}`)
      })
    })

    test('Scénario : profil externe signalé ; son caractère ne change pas tant qu’il a des titulaires', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.console}/console/profils`)
      await test.step('Étant donné la liste des profils : le correspondant d’institution est externe', async () => {
        await expect(page.getByRole('row').filter({ hasText: 'ROLE_CORRESPONDANT' }).getByText('Profil externe')).toBeVisible()
      })
      await test.step('Quand il rend externe le profil Normes et Agréments, qui a des titulaires', async () => {
        await page.getByRole('button', { name: 'Modifier le libellé et la description du profil Normes et Agréments' }).click()
        const d = page.getByRole('dialog', { name: 'Modifier le profil' })
        await d.getByRole('checkbox', { name: /Profil externe/ }).check()
        await expect(d.getByText(/son caractère externe ne pourra pas changer/)).toBeVisible()
        await capturer(page, `profil-externe-modale-${suffixe}`)
        await d.getByRole('button', { name: 'Continuer vers la signature' }).click()
        const s = page.getByRole('dialog')
        await s.getByLabel(/Motif/).fill('Ouverture du profil aux institutions')
        await s.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
      })
      await test.step('Alors MS-IAM refuse (409 IAM_PROFIL_EXTERNE_TITULAIRES), message explicite', async () => {
        await expect(page.getByText(/Le caractère externe d'un profil ne change pas tant qu'il a des titulaires/)).toBeVisible()
      })
    })

    test('Scénario : l’ingénieur d’homologation crée un type de projet et une fonction tenue par permission', async ({ page }) => {
      await demo(page, { profil: 'ROLE_INGENIEUR_HOMOLOGATION', theme })
      await page.goto(`${ADRESSES.departement}/des/referentiels/types-projet?vue=fonctions`)

      await test.step('Étant donné le catalogue des fonctions : profils et permissions de chacune', async () => {
        await expect(page.getByRole('heading', { level: 1, name: 'Types de projet et fonctions' })).toBeVisible()
        await expect(page.getByRole('columnheader', { name: 'Permissions' })).toBeVisible()
      })

      await test.step('Quand il crée une fonction avec une permission invalide, puis la corrige', async () => {
        await page.getByRole('button', { name: 'Nouvelle fonction' }).click()
        const d = page.getByRole('dialog', { name: 'Nouvelle fonction' })
        await d.getByLabel(/^Code/).fill('AUDITEUR_RESEAU')
        await d.getByLabel(/^Libellé/).fill('Auditeur réseau')
        await d.getByLabel(/^Profils/).fill('ROLE_POLE_RESEAU')
        await d.getByLabel(/^Permissions/).fill('CER-DOSSIER')
        await d.getByRole('button', { name: 'Créer la fonction' }).click()
        await expect(d.getByText('Chaque permission est un code en majuscules (20 au plus).')).toBeVisible()
        await d.getByLabel(/^Permissions/).fill('CER_DOSSIER_VERIFY, HOM_EVIDENCE_SUBMIT')
        await capturer(page, `fonction-modale-${suffixe}`)
        await d.getByRole('button', { name: 'Créer la fonction' }).click()
        await expect(page.getByText('Fonction « Auditeur réseau » enregistrée.')).toBeVisible()
        await expect(page.getByRole('row').filter({ hasText: 'AUDITEUR_RESEAU' }).getByTitle('CER_DOSSIER_VERIFY')).toBeVisible()
        await capturer(page, `fonctions-liste-${suffixe}`)
      })

      await test.step('Et il crée un type de projet vierge : son brouillon s’ouvre dans le modélisateur', async () => {
        await page.getByRole('tab', { name: 'Types de projet' }).click()
        await page.getByRole('button', { name: 'Nouveau type de projet' }).click()
        const d = page.getByRole('dialog', { name: 'Nouveau type de projet' })
        await d.getByLabel(/^Code/).fill('MONNAIE_ELECTRONIQUE')
        await d.getByLabel(/^Libellé/).fill('Monnaie électronique')
        await capturer(page, `type-projet-modale-${suffixe}`)
        await d.getByRole('button', { name: 'Créer le type' }).click()
        await expect(page).toHaveURL(/\/des\/referentiels\/operations\?version=/)
        await expect(page.getByText(/Monnaie électronique/).first()).toBeVisible()
      })
    })

    test('Scénario : onglet « Signatures » et vue spécialisée d’une opération du modélisateur', async ({ page }) => {
      await demo(page, { profil: 'ROLE_INGENIEUR_HOMOLOGATION', theme })
      await page.goto(`${ADRESSES.departement}/des/referentiels/operations?version=REF-CI-4&vue=liste`)
      await page.getByRole('button', { name: 'Modifier l\'opération 3', exact: true }).click()
      const d = page.getByRole('dialog', { name: /^Opération 3/ })
      await test.step('Quand il ajoute une signature et choisit la vue spécialisée', async () => {
        await d.getByRole('tab', { name: 'Signatures' }).click()
        await expect(d.getByText('Aucune signature : l\'opération se clôt dès son geste abouti.')).toBeVisible()
        await d.getByRole('button', { name: 'Ajouter une signature' }).click()
        const s = d.getByRole('group', { name: 'Signature 1' })
        await s.getByLabel(/^Code/).fill('VISA_CONDUITE')
        await s.getByLabel(/^Libellé/).fill('Visa du conducteur')
        await s.getByLabel(/^Signataire/).selectOption('CONDUCTEUR')
        await d.getByRole('button', { name: 'Enregistrer' }).click()
        await expect(s.getByText('Une signature porte au moins un livrable.')).toBeVisible()
        await capturer(page, `modelisateur-signatures-${suffixe}`)
        await d.getByRole('tab', { name: 'Exécution' }).click()
        await d.getByLabel(/^Vue spécialisée/).selectOption('ESSAIS')
      })
    })
  })

  test.describe(`Fonctionnalité : écrans pilotés par l’opération (D-93, ${suffixe})`, () => {
    test('Scénario : la fonction Normes et Agréments signe l’opération en premier ; la suivante attend le conducteur', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SNA', theme })
      await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-036', 'operations', '11')}`)
      const t = tiroir(page, '11')
      await test.step('Étant donné l’opération 11 en signature, deux signatures dans l’ordre', async () => {
        await expect(t.getByText('Signatures attendues')).toBeVisible()
        await capturer(page, `operation-signatures-${suffixe}`)
      })
      await test.step('Quand il signe avec sa clé de sécurité (finalité SIGNATURE_OPERATION)', async () => {
        await t.getByRole('button', { name: 'Signer : Visa des normes sur le compte rendu' }).click()
        await signerCle(page)
      })
      await test.step('Alors sa signature est donnée ; celle du conducteur reste attendue, sans geste pour lui', async () => {
        await expect(t.getByRole('button', { name: 'Signer : Visa des normes sur le compte rendu' })).toHaveCount(0)
        await expect(t.getByRole('button', { name: 'Signer : Visa du conducteur de projet' })).toHaveCount(0)
        await expect(t.getByText('Signée').first()).toBeVisible()
      })
    })

    test('Scénario : le conducteur renvoie une preuve avec son motif, puis affecte une fonction à affecter', async ({ page }) => {
      // DDR-05 : la trace se dépose par le responsable (R, pôle système) ; le conducteur désigné de la liaison (V) la renvoie.
      // LIA-2026-005 est d'abord confiée, motif à l'appui, au conducteur de la démonstration (démonstration continue, D-98).
      await demo(page, { theme })
      await page.addInitScript(() => localStorage.setItem('gm.demoContinue', '1'))
      const operation = `${ADRESSES.departement}${cheminDossier('LIA-2026-005', 'operations', '28')}`
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await acteur(page, 'ROLE_CHEF_DDRM', `${ADRESSES.departement}${cheminDossier('LIA-2026-005')}`)
      await page.getByRole('button', { name: 'Redésigner le conducteur' }).click()
      const redesignation = page.getByRole('dialog')
      await redesignation.getByLabel(/Conducteur désigné/).selectOption('U-COND-1')
      await redesignation.getByLabel(/Motif de la redésignation/).fill('Reprise de la liaison dans le portefeuille du conducteur')
      await redesignation.getByRole('button', { name: 'Redésigner le conducteur' }).click()
      await expect(page.getByRole('dialog')).toHaveCount(0)
      await test.step('Quand le pôle système dépose la trace de l’opération 28 et que le conducteur la renvoie avec un motif', async () => {
        await acteur(page, 'ROLE_POLE_SYSTEME', operation)
        let t = tiroir(page, '28')
        await t.getByRole('button', { name: 'Déposer la preuve' }).click()
        await joindre(t, 'trace-script-depot.png', 'trace', 'image/png')
        await t.getByRole('button', { name: 'Déposer la preuve' }).click()
        await confirmerAction(page, 'Déposer la preuve')
        await expect(t.getByText('trace-script-depot.png')).toBeVisible()
        await acteur(page, 'ROLE_CONDUCTEUR_PROJET', operation)
        t = tiroir(page, '28')
        await t.getByRole('button', { name: 'Renvoyer' }).click()
        await capturer(page, `operation-renvoi-modale-${suffixe}`)
        await confirmerAction(page, 'Renvoyer', 'Trace illisible : joindre la capture complète')
      })
      await test.step('Alors l’opération revient à faire et garde la trace du renvoi', async () => {
        const t = tiroir(page, '28')
        await expect(t.getByText('Opération renvoyée 1 fois')).toBeVisible()
        await expect(t.getByText(/Trace illisible : joindre la capture complète/)).toBeVisible()
        await t.getByRole('button', { name: 'Fermer' }).click()
      })
      await test.step('Quand il affecte la fonction Pôle réseau, à affecter sur LIA-2026-041', async () => {
        await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-041', 'gouvernance')}`)
        await expect(page.getByText('Fonctions à affecter').first()).toBeVisible()
        await capturer(page, `intervenants-${suffixe}`)
        await page.getByRole('button', { name: 'Affecter la fonction Pôle réseau' }).click()
        const d = page.getByRole('dialog', { name: 'Affecter la fonction Pôle réseau' })
        await d.getByLabel(/^Personne/).selectOption({ index: 1 })
        await d.getByRole('button', { name: 'Affecter', exact: true }).click()
        await expect(page.getByText(/Fonction Pôle réseau affectée à/)).toBeVisible()
      })
    })

    test('Scénario : une tâche « fonction à prendre » est offerte aux porteurs de la permission requise', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SNA', theme })
      await page.goto(`${ADRESSES.departement}/des/taches?vue=unite`)
      const ligne = page.getByRole('row').filter({ hasText: 'Prendre la fonction Normes et Agréments (LIA-2026-041)' })
      await expect(ligne.getByText(/Offerte aux porteurs de :/)).toBeVisible()
      // DDR-16 : la colonne Unité d'une tâche offerte sans unité porte « Toutes unités ».
      await expect(ligne.getByText('Toutes unités')).toBeVisible()
      await capturer(page, `taches-a-prendre-${suffixe}`)
      await ligne.getByRole('button', { name: 'Prendre la fonction Normes et Agréments (LIA-2026-041)' }).click()
      const d = page.getByRole('dialog', { name: 'Prendre la fonction Normes et Agréments (LIA-2026-041)' })
      await expect(d.getByText('Permissions requises')).toBeVisible()
      await d.getByRole('button', { name: 'Prendre la tâche' }).click()
      await expect(page.getByText('Tâche prise : vous en êtes le responsable.')).toBeVisible()
    })
  })

  test.describe(`Fonctionnalité : administration de tous les domaines et notifications d’homologation (${suffixe})`, () => {
    test('Scénario : le super administrateur administre les processus de toutes les unités', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.departement}/des/processus`)
      await expect(page.getByText('Administration de tous les domaines')).toBeVisible()
      await capturer(page, `processus-tous-domaines-${suffixe}`)
    })

    test('Scénario : la portée « Toutes les unités » n’est proposée que pour les processus', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.console}/console/matrice-pouvoirs`)
      await page.getByRole('button', { name: 'Pouvoirs du profil Responsable de service' }).click()
      const d = page.getByRole('dialog', { name: 'Pouvoirs du profil Responsable de service' })
      await expect(d.getByLabel(/^Modifier le rattachement/).locator('option', { hasText: 'Toutes les unités' })).toHaveCount(0)
      const definir = d.getByLabel('Définir les processus de l’unité')
      await expect(definir.locator('option', { hasText: 'Toutes les unités' })).toHaveCount(1)
      await definir.selectOption({ label: 'Toutes les unités' })
      await capturer(page, `matrice-toutes-${suffixe}`)
    })

    test('Scénario : notification d’homologation, du portail au livrable attendu', async ({ page }) => {
      await demo(page, { theme })
      await page.goto(`${ADRESSES.portail}/portail/tableau-de-bord`)
      const cloche = page.getByRole('banner').getByRole('button', { name: /^Notifications/ })
      // CER-12 : une action attendue par tâche de l'institution (fiche de suivi, procès-verbal, coordonnées d'essai), avec le
      // lien du geste au portail.
      await expect(cloche).toHaveAccessibleName('Notifications, 3 non lues')
      await cloche.click()
      const panneau = page.getByRole('dialog', { name: 'Centre de notifications' })
      const action = panneau.getByText(/^Action attendue : Communication de l.adresse et du port d.essai$/)
      await expect(action).toBeVisible()
      await capturer(page, `portail-cloche-${suffixe}`)
      await action.click()
      await expect(page).toHaveURL(/\/portail\/liaisons\/LIA-2026-046\/coordonnees$/)
    })
  })
}

test('Scénario : notification d’homologation de l’application interne, lien profond vers l’opération', async ({ page }) => {
  await demo(page, { profil: 'ROLE_SNA', theme: 'light' })
  await page.goto(`${ADRESSES.departement}/des/notifications`)
  await page.getByRole('tab', { name: 'Toutes' }).click()
  const n = page.getByText('Action attendue : Atelier comptabilisation')
  await expect(n).toBeVisible()
  await n.click()
  await expect(page).toHaveURL(/\/des\/parcours\/LIA-2026-036\?onglet=operations&operation=LIA-2026-036%3A11/)
  await expect(tiroir(page, '11')).toBeVisible()
})

test('Scénario : saisie d’une fonctionnalité contrôlée avant tout appel ; catalogue lu par application', async ({ page }) => {
  await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme: 'light' })
  await page.goto(`${ADRESSES.console}/console/profils?vue=fonctionnalites`)
  await expect(page.getByRole('row').filter({ hasText: 'liaisons' }).first()).toBeVisible()
  await page.getByLabel('Application').selectOption('portail')
  await page.getByLabel('Application').selectOption('console')
  await page.getByRole('button', { name: 'Modifier la fonctionnalité consoleOrganigramme', exact: true }).click()
  const d = page.getByRole('dialog', { name: 'Modifier la fonctionnalité' })
  await d.getByLabel(/^Libellé/).fill('')
  await d.getByLabel(/^Ordre/).fill('')
  await d.getByLabel(/^Icône/).fill('Mauvaise icone')
  await d.getByLabel(/^Groupe de la barre latérale/).selectOption('')
  await d.getByRole('button', { name: 'Enregistrer la fonctionnalité' }).click()
  await expect(d.getByText('Saisissez un libellé de 1 à 200 caractères.')).toBeVisible()
  await expect(d.getByText('Saisissez un nombre entier positif.')).toBeVisible()
  await expect(d.getByText(/Saisissez un nom d'icône en minuscules/)).toBeVisible()
  // Saisie corrigée, puis abandon ; nouveau profil contrôlé avant la signature.
  await d.getByLabel(/^Libellé/).fill('Organigramme')
  await d.getByLabel(/^Ordre/).fill('260')
  await d.getByLabel(/^Icône/).fill('network')
  // Permissions toutes exigées : liste filtrée par la saisie.
  const tous = d.getByRole('combobox', { name: /Permissions toutes exigées/ })
  await tous.click()
  await tous.fill('IAM_JOURNAL')
  await page.getByRole('listbox').getByRole('option', { name: 'IAM_JOURNAL_READ', exact: true }).click()
  await page.goto(`${ADRESSES.console}/console/profils`)
  await page.getByRole('button', { name: 'Nouveau profil' }).click()
  const n = page.getByRole('dialog', { name: 'Nouveau profil' })
  await n.getByRole('button', { name: 'Continuer vers la signature' }).click()
  await expect(n.getByText(/Code invalide/)).toBeVisible()
  await expect(n.getByText('La description est obligatoire.')).toBeVisible()
})

test('Scénario : le conducteur lit les fonctions à affecter et une signature qui ne lui revient pas encore', async ({ page }) => {
  await demo(page, { profil: 'ROLE_CONDUCTEUR_PROJET', theme: 'light' })
  await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-041', 'operations')}`)
  await expect(page.getByText(/À affecter/).first()).toBeVisible()
  // Opération dont le responsable est une fonction à affecter : la fonction s'affiche à la place d'une personne.
  const op = page.locator('li, tr').filter({ hasText: /À affecter/ }).getByRole('button', { name: /^Ouvrir l'opération / }).first()
  if (await op.count()) {
    await op.click()
    await expect(page.getByRole('dialog', { name: /^Opération / }).getByText(/À affecter/).first()).toBeVisible()
    await page.getByRole('dialog', { name: /^Opération / }).getByRole('button', { name: 'Fermer' }).click()
  }
  // Affectation : une personne exigée ; une fonction déjà tenue se change, titulaire proposé d'emblée.
  await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-041', 'gouvernance')}`)
  await page.getByRole('button', { name: 'Affecter la fonction Pôle réseau' }).click()
  const a = page.getByRole('dialog', { name: 'Affecter la fonction Pôle réseau' })
  await a.getByRole('button', { name: 'Affecter', exact: true }).click()
  await expect(a.locator('.gm-field-error, [role="alert"]').first()).toBeVisible()
  await a.getByRole('button', { name: 'Annuler' }).click()
  const changer = page.getByRole('button', { name: /^Affecter la fonction / }).filter({ hasText: 'Changer' }).first()
  if (await changer.count()) {
    await changer.click()
    await expect(page.getByRole('dialog', { name: /^Affecter la fonction / }).getByLabel(/^Personne/)).not.toHaveValue('')
    await page.getByRole('dialog', { name: /^Affecter la fonction / }).getByRole('button', { name: 'Annuler' }).click()
  }
  await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-036', 'operations', '11')}`)
  const t = tiroir(page, '11')
  await expect(t.getByText('Signatures attendues')).toBeVisible()
  await expect(t.getByRole('button', { name: /^Signer : / })).toHaveCount(0)
  await t.getByRole('tab', { name: 'Intervenants' }).click()
  // Opérations à geste dédié (vue servie) : coordonnées faites avec leur valeur, affectation dans un autre onglet.
  await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-030', 'operations', '12')}`)
  await expect(tiroir(page, '12')).toBeVisible()
  await page.goto(`${ADRESSES.departement}${cheminDossier('LIA-2026-044', 'operations', '6')}`)
  await expect(tiroir(page, '6')).toBeVisible()
})

test('Scénario : « Mon travail » de Normes et Agréments : tâche de signature et fonction à prendre signalées', async ({ page }) => {
  await demo(page, { profil: 'ROLE_SNA', theme: 'dark' })
  await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
  const travail = page.getByRole('region', { name: 'Mon travail' })
  await expect(travail.getByText('Signature', { exact: true }).first()).toBeVisible()
  await capturer(page, 'mon-travail-signature-sombre')
})
