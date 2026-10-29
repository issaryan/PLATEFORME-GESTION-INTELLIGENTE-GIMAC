import { useState } from 'react'
import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, DataTable, EmptyState, InlineAlert, KeyValueList, RadioGroup, StatusBadge, TextArea, type Column } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { DossierLiaison, FicheSuiviServices } from '../../../modele/parcours'
import { useFicheSuiviPortail, useRenseignerFicheSuivi } from '../../../hooks/parcours'
import { CadreAction } from './cadre'
import { PERM, useDate, useGeste, usePermissionPortail } from './commun'

// Fiche de suivi des services (étape 10, rang 32, CR §5.6 et §6.5) : après la bascule, le participant porte
// service par service ce qui est effectivement ouvert, avec un commentaire ; tous les champs sont
// obligatoires. La fiche est un objet de la plateforme : elle fonde la couverture de mise en service.

type Ligne = FicheSuiviServices['lignes'][number]
type Saisie = Record<string, { ouvert: 'oui' | 'non' | ''; commentaire: string }>
type Envoi = { liaisonId: string; lignes: { serviceCode: string; ouvert: boolean; commentaire: string | null }[] }

export const manquantsFiche = (lignes: Ligne[], s: Saisie) => lignes.filter((l) => !s[l.serviceCode]?.ouvert || !s[l.serviceCode]?.commentaire.trim())

function Formulaire({ fiche }: { fiche: FicheSuiviServices }) {
  const { t } = useTranslation()
  const renseigner = useRenseignerFicheSuivi()
  const geste = useGeste()
  const [saisie, setSaisie] = useState<Saisie>(() =>
    Object.fromEntries(fiche.lignes.map((l) => [l.serviceCode, { ouvert: l.ouvert === null ? '' : l.ouvert ? 'oui' : 'non', commentaire: l.commentaire ?? '' }])),
  )
  const [controle, setControle] = useState(false)
  const manquants = manquantsFiche(fiche.lignes, saisie)
  const modifier = (code: string, v: Partial<Saisie[string]>) => setSaisie((s) => ({ ...s, [code]: { ...s[code], ...v } }))
  const envoyer = () => {
    setControle(true)
    if (manquants.length) return
    const envoi: Envoi = { liaisonId: fiche.liaisonId, lignes: fiche.lignes.map((l) => ({ serviceCode: l.serviceCode, ouvert: saisie[l.serviceCode].ouvert === 'oui', commentaire: saisie[l.serviceCode].commentaire.trim() })) }
    const ouverts = envoi.lignes.filter((l) => l.ouvert).length
    geste<Envoi>(
      {
        key: 'renseignerFiche', label: 'pp.fiche.envoyer', icon: 'send', variant: 'primary', permission: PERM.deposer, confirm: 'confirm',
        consequences: () => (
          <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
            <KeyValueList items={fiche.lignes.map((l) => ({ key: l.libelle, value: t(saisie[l.serviceCode].ouvert === 'oui' ? 'pp.fiche.ouvert' : 'pp.fiche.nonOuvert') }))} />
            <span>{t('pp.fiche.consequence', { n: ouverts, total: envoi.lignes.length })}</span>
          </div>
        ),
        effet: (x) => renseigner.mutateAsync(x),
      },
      envoi,
      t('parcours.op.32'),
    )
  }
  return (
    <form className="gm-col" noValidate onSubmit={(e) => { e.preventDefault(); envoyer() }}>
      {controle && manquants.length ? (
        <InlineAlert level="crit" title={t('pp.fiche.incomplete', { count: manquants.length })}>
          {manquants.map((l) => l.libelle).join(', ')}
        </InlineAlert>
      ) : null}
      {fiche.lignes.map((l) => {
        const v = saisie[l.serviceCode]
        return (
          <Card key={l.serviceCode} title={l.libelle}>
            <div className="gm-col">
              <RadioGroup name={`ouvert-${l.serviceCode}`} label={t('pp.fiche.question', { service: l.libelle })} value={v.ouvert}
                options={[{ value: 'oui', label: t('pp.fiche.oui') }, { value: 'non', label: t('pp.fiche.non') }]} onChange={(o) => modifier(l.serviceCode, { ouvert: o as 'oui' | 'non' })} />
              {controle && !v.ouvert ? <span className="gm-field-error" role="alert">{t('pp.fiche.choixRequis')}</span> : null}
              <TextArea name={`commentaire-${l.serviceCode}`} label={t('pp.fiche.commentaire')} help={t(v.ouvert === 'non' ? 'pp.fiche.commentaireNon' : 'pp.fiche.commentaireOui')}
                required rows={2} maxLength={500} value={v.commentaire} onChange={(c) => modifier(l.serviceCode, { commentaire: c })}
                error={controle && !v.commentaire.trim() ? t('pp.fiche.commentaireRequis') : undefined} />
            </div>
          </Card>
        )
      })}
      <div className="gm-row">
        <Button type="submit" variant="primary" size="lg" icon="send">{t('pp.fiche.envoyer')}</Button>
      </div>
    </form>
  )
}

function Lecture({ fiche }: { fiche: FicheSuiviServices }) {
  const { t } = useTranslation()
  const ecrire = useDate()
  const cols: Column<Ligne>[] = [
    { key: 'libelle', header: t('pp.fiche.service') },
    { key: 'ouvert', header: t('pp.fiche.etatService'), render: (l) => (l.ouvert === null ? '-' : <StatusBadge status={l.ouvert ? 'ok' : 'warn'} label={t(l.ouvert ? 'pp.fiche.ouvert' : 'pp.fiche.nonOuvert')} />) },
    { key: 'commentaire', header: t('pp.fiche.commentaire'), render: (l) => l.commentaire ?? '-' },
  ]
  return (
    <Card title={t('pp.fiche.renseignee')}>
      <div className="gm-col">
        <KeyValueList items={[
          { key: t('pp.fiche.renseigneeLe'), value: ecrire(fiche.renseigneeLe) || '-' },
          { key: t('pp.fiche.constat'), value: fiche.constateeLe ? t('pp.fiche.constateeLe', { date: ecrire(fiche.constateeLe) }) : t('pp.fiche.constatAttendu') },
        ]} />
        <DataTable columns={cols} rows={fiche.lignes} rowKey={(l) => l.serviceCode} density="compact" />
      </div>
    </Card>
  )
}

function Fiche({ fiche }: { fiche: FicheSuiviServices }) {
  const { t } = useTranslation()
  const peut = usePermissionPortail(PERM.deposer)
  if (fiche.etat === 'A_REMETTRE') return <EmptyState icon="hourglass" title={t('pp.fiche.pasRemise')} text={t('pp.fiche.pasRemiseAide')} />
  if (fiche.etat !== 'REMISE') return <Lecture fiche={fiche} />
  if (!fiche.actionsPermises.includes('RENSEIGNER')) return <InlineAlert level="info">{t('pp.fiche.fermee')}</InlineAlert>
  return (
    <div className="gm-col">
      <InlineAlert level="info" title={t('pp.fiche.consigneTitre')}>{t('pp.fiche.consigne')}</InlineAlert>
      {peut ? <Formulaire fiche={fiche} /> : <InlineAlert level="info">{t('pp.droitManquant')}</InlineAlert>}
    </div>
  )
}

function Chargement({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useFicheSuiviPortail(d.id)
  return (
    <EtatsRequete etat={q} emptyTitle={t('pp.introuvable')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {(f) => <Fiche fiche={f} />}
    </EtatsRequete>
  )
}

export default function FicheSuiviPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  return <CadreAction id={id} titre={t('pp.fiche.titre')}>{(d) => <Chargement d={d} />}</CadreAction>
}
