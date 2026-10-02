import { useTranslation } from 'react-i18next'
import { LinkList, PageSection, type LinkListItem } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { ADMINISTRATION, versConsole } from '../../../navigation'
import { PERM } from '../../../permissions'

// Administration et sécurité des comptes (D-62, D-69), sur le tableau de bord des porteurs de IAM_ACCOUNT_READ
// (RSSI, super administrateur) : chaque lien ouvre l'écran voulu du département (liste filtrée, profils,
// journal, politique) ; la console de la plateforme reste un lien externe. Aucune donnée n'est lue ici.
export function SectionAdministration() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const peut = (p: string) => aPermission(permissions, p)
  const lien = (key: string, chemin: string, libelle: string): LinkListItem => ({ key, href: `${ADMINISTRATION}${chemin}`, label: t(libelle) })
  const items: LinkListItem[] = [
    ...(peut(PERM.compteCreer) ? [lien('creer', '/comptes/nouveau', 'admin.creer')] : []),
    lien('comptes', '/comptes', 'admin.comptes'),
    lien('sans2fa', '/comptes?etat=SANS_2FA', 'admin.sans2fa'),
    lien('premierAcces', '/comptes?etat=PREMIER_ACCES', 'admin.premierAcces'),
    lien('inactifs', '/comptes?etat=INACTIF', 'admin.inactifs'),
    lien('profils', '/profils', 'admin.profils'),
    ...(peut(PERM.journalAdministrationLire) ? [lien('journal', '/journal', 'admin.journal')] : []),
    ...(peut(PERM.politiqueLire) ? [lien('politique', '/politique-securite', 'admin.politique')] : []),
    ...(peut(PERM.configurationLire) ? [{ key: 'console', href: versConsole('/console/tableau-de-bord'), label: t('admin.console'), meta: t('admin.meta.console') }] : []),
  ]
  return (
    <PageSection title={t('admin.titre')} description={t('admin.texte')}>
      <LinkList label={t('admin.liens')} items={items} pageSize={10} />
    </PageSection>
  )
}
