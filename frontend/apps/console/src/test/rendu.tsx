import { expect } from 'vitest'
import type { ReactElement } from 'react'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import i18next from 'i18next'
import { SessionProvider, initI18n, type Langue, type Session } from '@gimac/socle'
import { catalogueCompte, monCompteSimule, type ServiceMonCompte } from '@gimac/compte'
import { catalogueMessagerie, servicesSimules } from '@gimac/lot1'
import { catalogues } from '../i18n'
import { Fournisseurs } from '../Fournisseurs'
import { creerClientRequetes } from '../services/clientRequetes'
import { servicesConsoleSimules, type OptionsConsoleSimulee } from '../services/simule'
import type { ServicesConsole } from '../services/ports'
import { PROFILS_DEMO } from '../navigation'

// Monte un écran de la console comme dans l'application : routeur, services simulés sans latence,
// requêtes sans réessai, session du profil demandé. Aucun appel réseau.

export const SUPER_ADMIN = PROFILS_DEMO.ROLE_SUPER_ADMIN.permissions
export const RSSI = PROFILS_DEMO.ROLE_RSSI.permissions
export const LECTEUR = ['IAM_ACCOUNT_READ']

export const session = (permissions: string[], o: Partial<Session> = {}): Session => ({
  sujet: 'u-test', nom: 'Agent de test', identifiant: 'test', roles: [], permissions, institutionId: '', acr: '2', doubleFacteur: true, ...o,
})

export const simules = (o: OptionsConsoleSimulee = {}) => servicesConsoleSimules({ latenceMs: 0, periodeFluxMs: 60_000, ...o })

/** Remplace des opérations d'un port des services simulés. */
export function avec<K extends keyof ServicesConsole>(cle: K, surcharge: Partial<ServicesConsole[K]>, base = simules()): ServicesConsole {
  return { ...base, [cle]: { ...base[cle], ...surcharge } }
}

function Emplacement() {
  const l = useLocation()
  return <span data-testid="emplacement">{l.pathname + l.search}</span>
}

export function rendrePage(page: ReactElement, o: { chemin: string; motif: string; permissions?: string[]; session?: Session; services?: ServicesConsole; compte?: ServiceMonCompte; langue?: Langue }) {
  if (!i18next.isInitialized) initI18n([catalogueCompte, catalogueMessagerie, ...catalogues], o.langue ?? 'fr')
  else void i18next.changeLanguage(o.langue ?? 'fr')
  const client = creerClientRequetes()
  client.setDefaultOptions({ queries: { retry: false, staleTime: 0 }, mutations: { retry: false } })
  const s = o.session ?? session(o.permissions ?? SUPER_ADMIN)
  return render(
    <MemoryRouter initialEntries={[o.chemin]}>
      <Fournisseurs services={o.services ?? simules()} compte={o.compte ?? monCompteSimule({ latenceMs: 0 })}
        messagerie={servicesSimules({ latenceMs: 0, utilisateur: { id: s.sujet, nom: s.nom } })} clientRequetes={client}>
        <SessionProvider session={s}>
          <Routes>
            <Route path={o.motif} element={page} />
            <Route path="*" element={<span>ailleurs</span>} />
          </Routes>
          <Emplacement />
        </SessionProvider>
      </Fournisseurs>
    </MemoryRouter>,
  )
}

/** Signe un acte sensible : motif puis clé de sécurité simulée. */
export async function signer(motif = 'Motif de test suffisamment long') {
  const d = await screen.findByRole('dialog')
  fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: motif } })
  await waitFor(() => expect((within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }) as HTMLButtonElement).disabled).toBe(false))
  fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
  return d
}

/** Confirme un acte motivé (ConfirmDialog). */
export async function confirmer(libelle: string, motif = 'Motif de test suffisamment long') {
  const d = await screen.findByRole('alertdialog')
  const champ = within(d).queryByLabelText(/Motif/)
  if (champ) fireEvent.change(champ, { target: { value: motif } })
  fireEvent.click(within(d).getByRole('button', { name: libelle }))
}
