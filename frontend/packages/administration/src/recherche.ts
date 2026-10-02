import { useMemo } from 'react'
import type { SourceRecherche } from '@gimac/socle'
import { useChemins, useServicesConsole } from './services/useServicesConsole'

/**
 * Source « comptes » de la recherche globale (Ctrl+K) : réservée aux porteurs de IAM_ACCOUNT_READ, elle
 * cherche par la liste paginée de MS-IAM (5 premiers comptes) et ouvre la fiche dans l'application courante.
 */
export function useSourceComptes(permissions: string[] | undefined): SourceRecherche[] {
  const services = useServicesConsole()
  const { base } = useChemins()
  const actif = (permissions ?? []).includes('IAM_ACCOUNT_READ')
  return useMemo(
    () =>
      actif
        ? [{
            cle: 'comptes',
            chercher: async (q: string) => (await services.comptes.lister({ page: 1, taille: 5, q })).contenu.map((c) => ({
              cle: c.id, type: 'compte' as const, code: c.identifiant, libelle: c.nomAffiche, detail: c.fonction ?? c.email ?? undefined,
              href: `${base}/comptes/${encodeURIComponent(c.id)}`,
            })),
          }]
        : [],
    [actif, services, base],
  )
}
