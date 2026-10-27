import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { PageHeader, Tabs } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { OngletMandats, OngletPerimetres, OngletPrestataires, OngletProtections, OngletRegistre, PERMISSION_REGISTRE } from '../composants/sandboxOnglets'
import { OngletReexamens } from '../composants/reexamens'

// Sandbox GIMAC dans la console (D-119, docs/lot1/sandbox-existant-adapte.md §8), IAM_ACCOUNT_READ : ce que MS-IAM
// applique pour la sandbox. « Périmètres » : institutions dont l'espace est suspendu (introspection refusée, sessions
// révoquées, état des comptes inchangé) ; « Comptes protégés » : derniers administrateurs actifs ; « Mandats » :
// délégations temporaires des partenaires techniques ; « Accès de prestataire » (US-EXI-24) : accès de maintenance
// bornés aux composants techniques, demandés puis approuvés par le RSSI (IAM_ACCOUNT_SECURITY) ; « Réexamen des accès »
// (US-EXI-23) : campagnes trimestrielles et scellement ; « Registre d'audit » (US-EXI-10, SBX_TRA_CONSULTATION) :
// événements à effet de la sandbox au registre de MS-AUDIT. Onglet tenu dans
// l'adresse (?onglet=).
export const ONGLETS_SANDBOX = ['perimetres', 'protections', 'mandats', 'prestataires', 'reexamens', 'registre'] as const
export type OngletSandbox = (typeof ONGLETS_SANDBOX)[number]

const CONTENUS: Record<OngletSandbox, () => React.JSX.Element> = {
  perimetres: OngletPerimetres,
  protections: OngletProtections,
  mandats: OngletMandats,
  prestataires: OngletPrestataires,
  reexamens: OngletReexamens,
  registre: OngletRegistre,
}

export default function SandboxIamPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const permissions = usePermissions()
  const visibles = ONGLETS_SANDBOX.filter((k) => k !== 'registre' || aPermission(permissions, PERMISSION_REGISTRE))
  const demande = params.get('onglet')
  const onglet: OngletSandbox = visibles.includes(demande as OngletSandbox) ? (demande as OngletSandbox) : 'perimetres'
  const Contenu = CONTENUS[onglet]
  return (
    <div className="gm-col">
      <PageHeader title={t('sbx.iam.titre')} subtitle={t('sbx.iam.sousTitre')} />
      <Tabs items={visibles.map((k) => ({ key: k, label: t(`sbx.iam.onglet.${k}`) }))} value={onglet} onChange={(k) => setParams({ onglet: k })} />
      <Contenu />
    </div>
  )
}
