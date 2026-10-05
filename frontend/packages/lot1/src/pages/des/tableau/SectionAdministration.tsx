import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ActionBar, Button, PageSection } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { ADMINISTRATION, versConsole } from '../../../fonctionnalites'
import { PERM } from '../../../permissions'

// Administration et sécurité des comptes (D-62, D-69), sur le tableau de bord des porteurs de IAM_ACCOUNT_READ
// (RSSI, super administrateur), quand l'application ne fournit pas son tableau d'administration (extensions.tsx) :
// barre d'actions vers les écrans du département (création, listes filtrées, profils, journal, politique) et
// la console de la plateforme. Aucune donnée d'administration n'est lue ici.
export function SectionAdministration() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  const peut = (p: string) => aPermission(permissions, p)
  const vers = (chemin: string) => () => void navigate(`${ADMINISTRATION}${chemin}`)
  return (
    <PageSection title={t('admin.titre')} description={t('admin.texte')}>
      <ActionBar align="start" label={t('admin.liens')}>
        {peut(PERM.compteCreer) ? <Button variant="primary" icon="plus" onClick={vers('/comptes/nouveau')}>{t('admin.creer')}</Button> : null}
        <Button onClick={vers('/comptes')}>{t('admin.comptes')}</Button>
        <Button variant="ghost" onClick={vers('/comptes?etat=SANS_2FA')}>{t('admin.sans2fa')}</Button>
        <Button variant="ghost" onClick={vers('/comptes?etat=PREMIER_ACCES')}>{t('admin.premierAcces')}</Button>
        <Button variant="ghost" onClick={vers('/comptes?etat=INACTIF')}>{t('admin.inactifs')}</Button>
        <Button variant="ghost" onClick={vers('/profils')}>{t('admin.profils')}</Button>
        {peut(PERM.journalAdministrationLire) ? <Button variant="ghost" onClick={vers('/journal')}>{t('admin.journal')}</Button> : null}
        {peut(PERM.politiqueLire) ? <Button variant="ghost" onClick={vers('/politique-securite')}>{t('admin.politique')}</Button> : null}
        {peut(PERM.configurationLire) ? (
          <Button variant="ghost" icon="monitor" onClick={() => window.location.assign(versConsole('/console/tableau-de-bord'))}>{t('admin.console')}</Button>
        ) : null}
      </ActionBar>
    </PageSection>
  )
}
