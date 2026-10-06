import { useTranslation } from 'react-i18next'
import { EmptyState, Link, PageHeader, PageSection } from '@gimac/design-system'
import { aPermission, usePermissions, useSession, useLibelleProfil } from '@gimac/socle'
import { SectionActes, SectionGestionUtilisateurs } from '../composants/sectionsTableau'
import { SectionSupervision } from '../composants/supervision'
import { WidgetChargesEnErreur } from '../composants/widgetsPlateforme'
import { useChemins } from '../services/useServicesConsole'
import { useLibellesProfils } from '../hooks'

// Tableau de bord de la console (D-61, D-62, D-79), composé selon les permissions de la session, jamais selon le
// nom du rôle ; la gestion des utilisateurs (actions directes et indicateurs cliquables) vient en tête :
// - super administrateur (PLATFORM_SUPERVISION_READ et IAM_ROLE_PRIVILEGED_GRANT) : état de la plateforme
//   en temps réel (flux SSE /api/plateforme/flux), services, serveurs, alertes, indicateurs des comptes ;
// - RSSI (IAM_ACCOUNT_READ sans IAM_ROLE_PRIVILEGED_GRANT) : posture de sécurité des comptes, derniers
//   actes d'administration, politique de sécurité, et la supervision si la session l'ouvre ;
// - exploitation (PLATFORM_OPS_READ, D-78) : charges de travail en erreur (administrateur système, RSSI, super
//   administrateur), puis la supervision.

export default function TableauDeBordPage() {
  const { t } = useTranslation()
  const session = useSession()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil(useLibellesProfils())
  const { base } = useChemins()
  const peut = (p: string) => aPermission(permissions, p)
  const privilegie = peut('IAM_ROLE_PRIVILEGED_GRANT')
  const supervision = peut('PLATFORM_SUPERVISION_READ')
  const lecteurComptes = peut('IAM_ACCOUNT_READ')
  const exploitation = peut('PLATFORM_OPS_READ')
  // Titre selon les permissions : octroi privilégié et supervision (super administrateur), comptes sans octroi
  // privilégié (sécurité des SI), exploitation sans comptes (administrateur système).
  const titre = supervision && privilegie ? 'superAdmin' : lecteurComptes && !privilegie ? 'rssi' : exploitation ? 'exploitation' : null
  const securite = lecteurComptes && !privilegie
  const profils = (session?.roles ?? []).filter((r) => r !== 'ROLE_USER')
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <PageHeader
        title={t(titre ? `console.tableau.${titre}` : 'console.tableau.titre')}
        subtitle={t(titre ? `console.tableau.sousTitre.${titre}` : 'console.tableau.vide.texte')}
        user={session ? { name: session.nom, roles: profils.map((r) => libelleProfil(r)) } : undefined}
      />
      {lecteurComptes ? <SectionGestionUtilisateurs posture={securite} /> : null}
      {securite && peut('IAM_JOURNAL_READ') ? <SectionActes /> : null}
      {securite && peut('PLATFORM_SECURITY_POLICY_READ') ? (
        <PageSection title={t('console.nav.politique')} description={t('console.tableau.politique.texte')}
          actions={<Link href={`${base}/politique-securite`}>{t('console.tableau.politique.lien')}</Link>} />
      ) : null}
      {/* Exploitation de l'infrastructure (D-78) : charges en erreur pour PLATFORM_OPS_READ. */}
      {exploitation ? <WidgetChargesEnErreur /> : null}
      {supervision ? <SectionSupervision /> : null}
      {!lecteurComptes && !supervision && !exploitation ? <EmptyState icon="gauge" title={t('console.tableau.vide.titre')} text={t('console.tableau.vide.texte')} /> : null}
    </div>
  )
}
