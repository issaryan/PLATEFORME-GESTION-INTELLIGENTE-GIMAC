import { useTranslation } from 'react-i18next'
import { LinkList, PageSection, type LinkListItem } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { versConsole } from '../../../navigation'
import { PERM } from '../../../permissions'

// Administration et sécurité des comptes (D-62), sur le tableau de bord des porteurs de IAM_ACCOUNT_READ
// (le RSSI) : la gestion des comptes est dans la console ; chaque lien ouvre l'écran voulu de la console
// (liste filtrée, profils, journal, politique). Aucune donnée d'administration n'est lue ici.
export function SectionAdministration() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const peut = (p: string) => aPermission(permissions, p)
  const console_ = t('admin.meta.console')
  const lien = (key: string, chemin: string, libelle: string): LinkListItem => ({ key, href: versConsole(chemin), label: t(libelle), meta: console_ })
  const items: LinkListItem[] = [
    ...(peut(PERM.compteCreer) ? [lien('creer', '/console/comptes/nouveau', 'admin.creer')] : []),
    lien('comptes', '/console/comptes', 'admin.comptes'),
    lien('sans2fa', '/console/comptes?etat=SANS_2FA', 'admin.sans2fa'),
    lien('premierAcces', '/console/comptes?etat=PREMIER_ACCES', 'admin.premierAcces'),
    lien('inactifs', '/console/comptes?etat=INACTIF', 'admin.inactifs'),
    lien('profils', '/console/profils', 'admin.profils'),
    ...(peut(PERM.journalAdministrationLire) ? [lien('journal', '/console/journal', 'admin.journal')] : []),
    ...(peut(PERM.politiqueLire) ? [lien('politique', '/console/politique-securite', 'admin.politique')] : []),
    lien('console', '/console/tableau-de-bord', 'admin.console'),
  ]
  return (
    <PageSection title={t('admin.titre')} description={t('admin.texte')}>
      <LinkList label={t('admin.liens')} items={items} pageSize={10} />
    </PageSection>
  )
}
