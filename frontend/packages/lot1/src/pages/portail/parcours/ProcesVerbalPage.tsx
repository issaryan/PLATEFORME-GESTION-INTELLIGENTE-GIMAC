import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, Hash, InlineAlert, KeyValueList, StatusBadge, Tag } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { DossierLiaison, ProcesVerbalCertification, Signature } from '../../../modele/parcours'
import { useProcesVerbalPortail, useSignerProcesVerbalPortail } from '../../../hooks/parcours'
import { CadreAction } from './cadre'
import { PERM, useDate, useFonction, useGeste, usePermissionPortail } from './commun'

// Signature du procès-verbal de certification par le participant (rang 19, verrou 1, D-56) : couverture,
// services couverts, empreinte de ce qui est signé, signature du GIMAC déjà apposée ou non. La signature
// passe par le SignDialog avec réauthentification FIDO2 (finalité QUITUS_SIGN). Pièce pivot : signé des
// deux parties, il déclenche le raccordement de production.

const tonPv = (e: ProcesVerbalCertification['etat']) => (e === 'SIGNE' ? 'ok' : e === 'NON_ETABLI' ? 'neutral' : 'warn')

function Pv({ d, pv }: { d: DossierLiaison; pv: ProcesVerbalCertification }) {
  const { t } = useTranslation()
  const ecrire = useDate()
  const fonction = useFonction()
  const geste = useGeste()
  const signer = useSignerProcesVerbalPortail()
  const peutSigner = usePermissionPortail(PERM.signer)
  if (pv.etat === 'NON_ETABLI') return <EmptyState icon="scroll-text" title={t('pp.pv.nonEtabli')} text={t('pp.pv.nonEtabliAide')} />
  const libelleService = (code: string) => d.perimetre.find((s) => s.serviceCode === code)?.libelle ?? code
  const signature = (s: Signature | null, gimac: boolean) =>
    s ? (
      <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
        <StatusBadge status="ok" label={t('pp.pv.apposee')} />
        <span>{t('pp.pv.signeeLe', { date: ecrire(s.signeLe), par: gimac ? fonction(s.fonction) : s.signataire })}</span>
      </span>
    ) : (
      <StatusBadge status="neutral" label={t(gimac ? 'pp.pv.gimacAttendue' : 'pp.pv.vousAttendue')} />
    )
  const couv = pv.couverture
  const attendue = pv.actionsPermises.includes('SIGNER_PARTICIPANT')
  const lancer = () =>
    geste<ProcesVerbalCertification>(
      {
        key: 'signerPv', label: 'pp.pv.signer', icon: 'pen-line', variant: 'primary', permission: PERM.signer, confirm: 'sign',
        sign: { purpose: 'QUITUS_SIGN', empreinte: (p) => p.empreinte ?? '' },
        consequences: () => (
          <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
            <span>{t('pp.pv.consequence', { n: couv.numerateur, total: couv.denominateur })}</span>
            <span>{t(pv.signatureGimac ? 'pp.pv.consequenceDerniere' : 'pp.pv.consequencePremiere')}</span>
          </div>
        ),
        effet: (p, ctx) => signer.mutateAsync({ liaisonId: p.liaisonId, assertion: ctx.assertion ?? '' }),
      },
      pv,
      t('pp.pv.objet', { reference: d.reference }),
    )
  return (
    <div className="gm-col">
      <Card title={t('pp.pv.contenu')}>
        <KeyValueList items={[
          { key: t('pp.etat'), value: <StatusBadge status={tonPv(pv.etat)} label={t(`parcours.pv.etat.${pv.etat}`)} /> },
          {
            key: t('pp.couvertureCertification'),
            value: (
              <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
                <span className="gm-num gm-type-body-strong">{couv.calculable ? `${couv.taux} %` : t('pp.nonCalculable')}</span>
                <span className="gm-num">{t('pp.pv.servicesSur', { n: couv.numerateur, total: couv.denominateur })}</span>
                {couv.taux === 100 ? <StatusBadge status="ok" label={t('pp.complete')} /> : <StatusBadge status="crit" label={t('pp.incomplete')} />}
              </span>
            ),
          },
          { key: t('pp.pv.services'), value: <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>{pv.servicesCouverts.map((s) => <Tag key={s}>{libelleService(s)}</Tag>)}</span> },
          { key: t('pp.pv.etabli'), value: t('pp.pv.etabliLe', { date: ecrire(pv.etabliLe), par: fonction('CONDUCTEUR') }) },
          { key: t('pp.pv.empreinte'), value: pv.empreinte ? <Hash value={pv.empreinte} /> : '—' },
          { key: t('pp.pv.signatureGimac'), value: signature(pv.signatureGimac, true) },
          { key: t('pp.pv.signatureParticipant'), value: signature(pv.signatureParticipant, false) },
        ]} />
      </Card>
      <InlineAlert level="info">{t('parcours.alerte.pivot')}</InlineAlert>
      {attendue && couv.taux !== 100 ? <InlineAlert level="crit">{t('pp.pv.couvertureIncomplete')}</InlineAlert> : null}
      {attendue ? (
        peutSigner ? (
          <div className="gm-row">
            <Button variant="primary" size="lg" icon="pen-line" onClick={lancer}>{t('pp.pv.signer')}</Button>
            <span className="gm-type-small" style={{ color: 'var(--gm-color-text-secondary)' }}>{t('pp.pv.cleSecurite')}</span>
          </div>
        ) : (
          <InlineAlert level="info">{t('pp.pv.droitManquant')}</InlineAlert>
        )
      ) : pv.signatureParticipant && !pv.signatureGimac ? (
        <InlineAlert level="info">{t('pp.pv.attenteGimac')}</InlineAlert>
      ) : null}
    </div>
  )
}

function Chargement({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useProcesVerbalPortail(d.id)
  return (
    <EtatsRequete etat={q} emptyTitle={t('pp.pv.nonEtabli')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {(pv) => <Pv d={d} pv={pv} />}
    </EtatsRequete>
  )
}

export default function ProcesVerbalPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const lire = usePermissionPortail(PERM.lireCertification)
  return (
    <CadreAction id={id} titre={t('pp.pv.titre')}>
      {(d) => (lire ? <Chargement d={d} /> : <InlineAlert level="info">{t('pp.droitLecture')}</InlineAlert>)}
    </CadreAction>
  )
}
