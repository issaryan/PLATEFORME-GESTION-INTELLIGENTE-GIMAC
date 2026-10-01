import { useState } from 'react'
import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, InlineAlert, KeyValueList, TextArea, TextField, Note } from '@gimac/design-system'
import type { DossierLiaison } from '../../../modele/parcours'
import { useCommuniquerCoordonnees } from '../../../hooks/parcours'
import { CadreAction, useTacheLiaison } from './cadre'
import { Echeance, PERM, useDate, useGeste, usePermissionPortail } from './commun'

// Communication de l'adresse et du port d'essai (rang 12, fiche des coordonnées techniques, CR §3.4) :
// contrôle du format avant l'envoi, confirmation, puis valeur relisible (CR §6.4.3). Le service reste
// seul juge : un refus est affiché dans le dialogue avec son identifiant de corrélation.

/** Nom d'hôte ou adresse IPv4 : étiquettes alphanumériques séparées par des points (jamais d'espace ni de schéma). */
const HOTE = /^(?=.{1,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$/i
const PORT = /^\d{2,5}$/

export const controlerCoordonnees = (adresse: string, port: string) => ({
  adresse: !adresse.trim() ? 'pp.coordonnees.adresseRequise' : HOTE.test(adresse.trim()) ? undefined : 'pp.coordonnees.adresseInvalide',
  port: !port.trim() ? 'pp.coordonnees.portRequis' : PORT.test(port.trim()) && Number(port) >= 10 && Number(port) <= 65535 ? undefined : 'pp.coordonnees.portInvalide',
})

type Saisie = { liaisonId: string; adresse: string; port: string; pointsTerminaison?: string }

function Formulaire({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const communiquer = useCommuniquerCoordonnees()
  const geste = useGeste()
  const [adresse, setAdresse] = useState('')
  const [port, setPort] = useState('')
  const [points, setPoints] = useState('')
  const [controle, setControle] = useState(false)
  const erreurs = controlerCoordonnees(adresse, port)
  const mobile = d.typeProjet === 'PAIEMENT_MOBILE'
  const envoyer = (form: HTMLFormElement) => {
    setControle(true)
    if (erreurs.adresse || erreurs.port) {
      // Le premier champ en erreur reçoit le focus : l'erreur est lue et corrigée sans chercher.
      form.querySelector<HTMLInputElement>(`[name="${erreurs.adresse ? 'adresse' : 'port'}"]`)?.focus()
      return
    }
    const saisie: Saisie = { liaisonId: d.id, adresse: adresse.trim(), port: port.trim(), pointsTerminaison: points.trim() || undefined }
    geste<Saisie>(
      {
        key: 'communiquerCoordonnees', label: 'pp.coordonnees.envoyer', icon: 'send', variant: 'primary', permission: PERM.deposer, confirm: 'confirm',
        consequences: (s) => (
          <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
            <KeyValueList items={[
              { key: t('pp.coordonnees.adresse'), value: s.adresse, mono: true },
              { key: t('pp.coordonnees.port'), value: s.port, mono: true },
              ...(s.pointsTerminaison ? [{ key: t('pp.coordonnees.points'), value: s.pointsTerminaison, mono: true }] : []),
            ]} />
            <span>{t('pp.coordonnees.consequence')}</span>
          </div>
        ),
        effet: (s) => communiquer.mutateAsync(s),
      },
      saisie,
      t('parcours.op.12'),
    )
  }
  return (
    <form className="gm-col" noValidate onSubmit={(e) => { e.preventDefault(); envoyer(e.currentTarget) }}>
      <TextField name="adresse" label={t('pp.coordonnees.adresse')} help={t('pp.coordonnees.adresseAide')} required mono autoComplete="off" maxLength={253}
        value={adresse} onChange={setAdresse} error={controle && erreurs.adresse ? t(erreurs.adresse) : undefined} />
      <TextField name="port" label={t('pp.coordonnees.port')} help={t('pp.coordonnees.portAide')} required mono inputMode="numeric" maxLength={5} autoComplete="off"
        value={port} onChange={(v) => setPort(v.replace(/\D/g, ''))} error={controle && erreurs.port ? t(erreurs.port) : undefined} />
      {mobile ? (
        <TextArea name="pointsTerminaison" label={t('pp.coordonnees.points')} help={t('pp.coordonnees.pointsAide')} rows={3} mono value={points} onChange={setPoints} maxLength={2000} />
      ) : null}
      <div className="gm-row">
        <Button type="submit" variant="primary" size="lg" icon="send">{t('pp.coordonnees.envoyer')}</Button>
      </div>
    </form>
  )
}

function Coordonnees({ d }: { d: DossierLiaison }) {
  const { t } = useTranslation()
  const ecrire = useDate()
  const peut = usePermissionPortail(PERM.deposer)
  const { requete, tache } = useTacheLiaison(d.id, (x) => x.rang === '12')
  const c = d.coordonneesTechniques
  if (c) {
    return (
      <Card title={t('pp.coordonnees.communiquees')}>
        <div className="gm-col">
          <KeyValueList items={[
            { key: t('pp.coordonnees.adresse'), value: c.adresse, mono: true },
            { key: t('pp.coordonnees.port'), value: c.port, mono: true },
            ...(c.pointsTerminaison ? [{ key: t('pp.coordonnees.points'), value: c.pointsTerminaison, mono: true }] : []),
            { key: t('pp.coordonnees.le'), value: ecrire(c.communiqueesLe) },
          ]} />
          <InlineAlert level="info">{t('pp.coordonnees.relisible')}</InlineAlert>
        </div>
      </Card>
    )
  }
  if (requete.isPending) return null
  const ouverte = tache?.actionsPermises?.includes('COMMUNIQUER_COORDONNEES')
  if (!ouverte) return <EmptyState icon="clock" title={t('pp.coordonnees.pasAttendues')} text={t('pp.coordonnees.pasAttenduesAide')} />
  return (
    <Card title={t('pp.coordonnees.formulaire')}>
      <div className="gm-col">
        <Note>{t('pp.coordonnees.intro')}</Note>
        {tache ? <KeyValueList items={[{ key: t('pp.echeance'), value: <Echeance date={tache.echeance} sla={tache.sla} /> }, { key: t('pp.preuveAttendueLibelle'), value: t('parcours.preuveAttendue.12') }]} /> : null}
        {peut ? <Formulaire d={d} /> : <InlineAlert level="info">{t('pp.droitManquant')}</InlineAlert>}
      </div>
    </Card>
  )
}

export default function CoordonneesPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  return <CadreAction id={id} titre={t('pp.coordonnees.titre')}>{(d) => <Coordonnees d={d} />}</CadreAction>
}
