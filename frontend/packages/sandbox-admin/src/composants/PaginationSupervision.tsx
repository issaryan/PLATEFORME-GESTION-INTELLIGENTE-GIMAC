import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, PageSection } from '@gimac/design-system'
import type { PageSupervision } from '../modele'

export type RequetePage<T> = { data?: PageSupervision<T>; isPending: boolean; isError: boolean; refetch: () => unknown }

/** Section paginée d'une lecture de supervision : page courante, total, boutons précédent et suivant. */
export function PaginationSupervision<T>({ titre, page, taille, changerPage, requete, children }: { titre: string; page: number; taille: number; changerPage: (page: number) => void; requete: RequetePage<T>; children: ReactNode }) {
  const { t } = useTranslation()
  const total = requete.data?.total ?? 0
  const pages = Math.max(1, Math.ceil(total / taille))
  return <PageSection title={titre} description={t('sbx.supervision.page', { page: page + 1, pages, total })} actions={<span className="gm-row">
    <Button variant="ghost" icon="arrow-left" disabled={page === 0} onClick={() => changerPage(page - 1)}>{t('sbx.supervision.precedente')}</Button>
    <Button variant="ghost" icon="arrow-right" disabled={(page + 1) * taille >= total} onClick={() => changerPage(page + 1)}>{t('sbx.supervision.suivante')}</Button>
  </span>}>{children}</PageSection>
}
