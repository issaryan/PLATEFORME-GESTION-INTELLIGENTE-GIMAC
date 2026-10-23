import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { PageHeader, Tabs } from '@gimac/design-system'
import { OngletMandats, OngletPerimetres, OngletProtections } from '../composants/sandboxOnglets'

// Sandbox GIMAC dans la console (D-119, docs/lot1/sandbox-existant-adapte.md §8), IAM_ACCOUNT_READ : ce que MS-IAM
// applique pour la sandbox. « Périmètres » : institutions dont l'espace est suspendu (introspection refusée, sessions
// révoquées, état des comptes inchangé) ; « Comptes protégés » : derniers administrateurs actifs ; « Mandats » :
// délégations temporaires des partenaires techniques. Onglet tenu dans l'adresse (?onglet=).
export const ONGLETS_SANDBOX = ['perimetres', 'protections', 'mandats'] as const
export type OngletSandbox = (typeof ONGLETS_SANDBOX)[number]

const CONTENUS: Record<OngletSandbox, () => React.JSX.Element> = {
  perimetres: OngletPerimetres,
  protections: OngletProtections,
  mandats: OngletMandats,
}

export default function SandboxIamPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const demande = params.get('onglet')
  const onglet: OngletSandbox = ONGLETS_SANDBOX.includes(demande as OngletSandbox) ? (demande as OngletSandbox) : 'perimetres'
  const Contenu = CONTENUS[onglet]
  return (
    <div className="gm-col">
      <PageHeader title={t('sbx.iam.titre')} subtitle={t('sbx.iam.sousTitre')} />
      <Tabs items={ONGLETS_SANDBOX.map((k) => ({ key: k, label: t(`sbx.iam.onglet.${k}`) }))} value={onglet} onChange={(k) => setParams({ onglet: k })} />
      <Contenu />
    </div>
  )
}
