import type { Page, Route } from '@playwright/test'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

// Catalogue des fonctionnalités servi par MS-IAM (D-94, docs/lot1/ecrans-par-configuration.md §6.2) pour les
// constructions sur la passerelle : GET /bff/{des,console}/iam/configuration/fonctionnalites rend les valeurs de
// départ (copie de ms-iam/src/main/resources/config/fonctionnalites-initiales.json), PUT …/{cle} les modifie (motif
// et clé de sécurité exigés, IAM_PROFILE_EDIT vérifié par la passerelle simulée de chaque scénario). La route est
// posée par la fixture `page` et reste prioritaire sur les passerelles des scénarios (toujours la dernière posée).

type Fonctionnalite = { cle: string; application: string; permissions: { tous: string[]; unDe: string[] }; [k: string]: unknown }

const SOURCE = resolve(dirname(fileURLToPath(import.meta.url)), '../../packages/lot1/src/services/simule/fonctionnalites-initiales.json')
const initiales = (): Fonctionnalite[] => (JSON.parse(readFileSync(SOURCE, 'utf8')) as { fonctionnalites: Fonctionnalite[] }).fonctionnalites

const RE = /\/bff\/(des|console)\/iam\/configuration\/fonctionnalites(\/[^/?]+)?(\?.*)?$/

/** État du catalogue d'une page : servi ou indisponible (503), modifications reçues. */
export class CatalogueE2E {
  fonctionnalites = initiales()
  servi = true
  readonly modifications: { cle: string; motif: string; corps: Record<string, unknown> }[] = []

  async servir(route: Route) {
    const req = route.request()
    const json = (status: number, corps: unknown) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(corps) })
    if (!this.servi) return json(503, { code: 'IAM_INDISPONIBLE', message: 'Service indisponible', correlationId: 'RQ-CAT-E2E' })
    const [, , suite] = new URL(req.url()).pathname.match(RE) ?? []
    if (req.method() === 'GET' && !suite) {
      const application = new URL(req.url()).searchParams.get('application')
      return json(200, this.fonctionnalites.filter((f) => !application || f.application === application))
    }
    if (req.method() === 'PUT' && suite) {
      const cle = decodeURIComponent(suite.slice(1))
      const i = this.fonctionnalites.findIndex((f) => f.cle === cle)
      if (i < 0) return json(404, { code: 'IAM_FONCTIONNALITE_INCONNUE', message: 'Fonctionnalité inconnue', correlationId: 'RQ-CAT-404' })
      if (!req.headers()['x-reauth-assertion']) return json(428, { code: 'IAM_REAUTH_REQUIRED', message: 'Réauthentification forte exigée', correlationId: 'RQ-CAT-428' })
      const corps = req.postDataJSON() as Record<string, unknown> & { motif?: string }
      if (!corps.motif || String(corps.motif).trim().length < 3) return json(422, { code: 'IAM_MOTIF_REQUIS', message: 'Motif requis', correlationId: 'RQ-CAT-422' })
      const { motif, ...f } = corps
      this.fonctionnalites[i] = { ...this.fonctionnalites[i], ...f, cle, modifieLe: new Date().toISOString(), modifiePar: 'e2e' } as Fonctionnalite
      this.modifications.push({ cle, motif: String(motif), corps: f })
      return json(200, this.fonctionnalites[i])
    }
    return json(405, { code: 'HTTP_405', message: 'Méthode non simulée', correlationId: 'RQ-CAT-405' })
  }
}

const parPage = new WeakMap<Page, CatalogueE2E>()

/** Catalogue de la page (posé par la fixture) : pour le rendre indisponible ou lire les modifications. */
export const catalogueE2E = (page: Page): CatalogueE2E => {
  const c = parPage.get(page)
  if (!c) throw new Error('Catalogue E2E absent : la fixture `page` de fixtures.ts le pose.')
  return c
}

/** Pose la route du catalogue et la garde prioritaire : reposée après chaque page.route des scénarios. */
export async function poserCatalogue(page: Page) {
  const c = new CatalogueE2E()
  parPage.set(page, c)
  const servir = (r: Route) => c.servir(r)
  const route = page.route.bind(page)
  await route(RE, servir)
  page.route = (async (...args: Parameters<Page['route']>) => {
    await route(...args)
    await page.unroute(RE, servir)
    await route(RE, servir)
  }) as Page['route']
}
