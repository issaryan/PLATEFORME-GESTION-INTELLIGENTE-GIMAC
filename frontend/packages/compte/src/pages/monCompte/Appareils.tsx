import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, DataTable, Icon, Note, StatusBadge, type Column, type IconName } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { Appareil, Connexion, DescriptionAppareil, TypeAppareil } from '../../modele'
import { useAppareils, useFermerAppareil, useFermerAutresAppareils, useHistoriqueConnexions } from '../../hooks'
import { dateHeure, useMessageErreur } from '../../outils'
import { ModaleCompte } from '../../composants/ModaleSaisie'
import { Annonce, type Retour } from '../../composants/Annonce'

// Appareils connectés et historique des connexions (docs/lot1/mon-compte.md §3, §4) : sessions ouvertes
// portées par la passerelle (appareil, navigateur, système, IP masquée hors session courante, couche, ouverture,
// dernière activité), session courante marquée « Cet appareil » ; fermeture d'une autre session ou de toutes
// les autres, toujours confirmée dans la modale CRUD standard ; historique des 90 derniers jours, paginé.

const ICONE: Record<TypeAppareil, IconName> = { ORDINATEUR: 'monitor', MOBILE: 'smartphone', TABLETTE: 'tablet', INCONNU: 'globe' }

/** Appareil lisible : type, navigateur et système. */
function useLibelleAppareil() {
  const { t } = useTranslation()
  return {
    type: (a: DescriptionAppareil | string | null) => (a && typeof a === 'object' ? t(`appareils.type.${a.type}`, { defaultValue: a.type }) : (a ?? t('appareils.inconnu'))),
    complet: (a: DescriptionAppareil | string | null) =>
      a && typeof a === 'object'
        ? [t(`appareils.type.${a.type}`, { defaultValue: a.type }), a.navigateur, a.systeme].filter(Boolean).join(' · ')
        : (a ?? t('appareils.inconnu')),
  }
}

export function Appareils() {
  const { t, i18n } = useTranslation()
  const message = useMessageErreur()
  const libelle = useLibelleAppareil()
  const appareils = useAppareils()
  const fermer = useFermerAppareil()
  const fermerAutres = useFermerAutresAppareils()
  const [aFermer, setAFermer] = useState<Appareil | 'AUTRES' | null>(null)
  const [retour, setRetour] = useState<Retour>(null)
  const autres = (appareils.data ?? []).filter((a) => !a.courante).length
  const agir = async (f: () => Promise<unknown>, succes: string) => {
    setRetour(null)
    try {
      await f()
      setAFermer(null)
      setRetour({ niveau: 'info', message: { texte: succes } })
    } catch (x) {
      setAFermer(null)
      setRetour({ niveau: 'crit', message: message(x) })
    }
  }
  const couche = (c: string | null) => (c ? t(`appareils.couche.${c}`, { defaultValue: c }) : '—')
  const colonnes: Column<Appareil>[] = [
    {
      key: 'appareil', header: t('appareils.appareil'),
      render: (a) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
          <Icon name={ICONE[a.appareil.type] ?? 'globe'} size={16} />
          <span>{libelle.type(a.appareil)}</span>
          {a.courante ? <StatusBadge status="info" icon="check" label={t('appareils.courant')} /> : null}
        </span>
      ),
    },
    { key: 'navigateur', header: t('appareils.navigateur'), render: (a) => a.appareil.navigateur ?? '—' },
    { key: 'systeme', header: t('appareils.systeme'), render: (a) => a.appareil.systeme ?? '—' },
    { key: 'adresseIp', header: t('appareils.adresse'), render: (a) => <span className="gm-mono">{a.adresseIp ?? '—'}</span> },
    { key: 'couche', header: t('appareils.couche'), render: (a) => couche(a.couche) },
    { key: 'ouverteLe', header: t('appareils.ouverteLe'), render: (a) => dateHeure(a.ouverteLe, i18n.language) },
    { key: 'derniereActivite', header: t('appareils.derniereActivite'), render: (a) => dateHeure(a.derniereActivite, i18n.language) },
    {
      key: 'actions', header: t('appareils.actions'), type: 'actions',
      render: (a) => (a.courante ? <span className="gm-field-help">{t('appareils.courant.aide')}</span> : <Button size="sm" onClick={() => { setRetour(null); setAFermer(a) }}>{t('appareils.fermer')}</Button>),
    },
  ]
  return (
    <Card title={t('appareils.titre')}
      actions={<Button size="sm" variant="danger" icon="log-out" disabled={autres === 0} disabledReason={t('appareils.aucuneAutre')} onClick={() => { setRetour(null); setAFermer('AUTRES') }}>{t('appareils.fermerAutres')}</Button>}>
      <div className="gm-col">
        <Note>{t('appareils.texte')}</Note>
        <EtatsRequete etat={appareils} isEmpty={(l) => l.length === 0} emptyTitle={t('appareils.aucun')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
          {(l) => <DataTable columns={colonnes} rows={l} rowKey={(a) => a.id} density="compact" />}
        </EtatsRequete>
        <Annonce retour={retour} />
      </div>
      {aFermer === 'AUTRES' ? (
        <ModaleCompte id="gm-dialogue-appareils" context="delete" irreversible={false} title={t('appareils.fermerAutres')} description={t('appareils.fermerAutres.texte', { count: autres })}
          submitLabel={t('appareils.fermerAutres')} submitting={fermerAutres.isPending}
          onSubmit={() => void agir(() => fermerAutres.mutateAsync(undefined), t('appareils.autresFermees'))} onClose={() => setAFermer(null)} />
      ) : aFermer ? (
        <ModaleCompte id="gm-dialogue-appareil" context="confirm" title={t('appareils.fermer.titre')} description={t('appareils.fermer.texte')}
          submitLabel={t('appareils.fermer.titre')} submitIcon="log-out" submitting={fermer.isPending}
          object={[
            { key: t('appareils.appareil'), value: libelle.complet(aFermer.appareil) },
            { key: t('appareils.adresse'), value: aFermer.adresseIp ?? '—', mono: true },
            { key: t('appareils.couche'), value: couche(aFermer.couche) },
            { key: t('appareils.derniereActivite'), value: dateHeure(aFermer.derniereActivite, i18n.language) },
          ]}
          onSubmit={() => void agir(() => fermer.mutateAsync(aFermer.id), t('appareils.fermee'))} onClose={() => setAFermer(null)} />
      ) : null}
    </Card>
  )
}

const ETAT: Record<Connexion['resultat'], 'ok' | 'warn' | 'crit'> = { REUSSIE: 'ok', ECHEC_MOT_DE_PASSE: 'warn', ECHEC_CODE: 'warn', BLOQUEE: 'crit' }

export function Historique() {
  const { t, i18n } = useTranslation()
  const message = useMessageErreur()
  const libelle = useLibelleAppareil()
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(20)
  const h = useHistoriqueConnexions(page, taille)
  const lignes = h.data?.contenu ?? []
  const colonnes: Column<Connexion & { id: string }>[] = [
    { key: 'le', header: t('historique.le'), render: (c) => <span className="gm-num">{dateHeure(c.le, i18n.language)}</span> },
    { key: 'resultat', header: t('historique.resultat'), render: (c) => <StatusBadge status={ETAT[c.resultat] ?? 'neutral'} label={t(`historique.resultat.${c.resultat}`, { defaultValue: c.resultat })} /> },
    { key: 'appareil', header: t('appareils.appareil'), render: (c) => libelle.complet(c.appareil) },
    { key: 'adresseIp', header: t('appareils.adresse'), render: (c) => <span className="gm-mono">{c.adresseIp ?? '—'}</span> },
    { key: 'couche', header: t('appareils.couche'), render: (c) => (c.couche ? t(`appareils.couche.${c.couche}`, { defaultValue: c.couche }) : '—') },
  ]
  const rows = lignes.map((c, i) => ({ ...c, id: `${c.le}-${i}` }))
  return (
    <Card title={t('historique.titre')}>
      <div className="gm-col">
        <Note>{t('historique.texte')}</Note>
        <DataTable
          columns={colonnes}
          rows={rows}
          rowKey={(c) => c.id}
          density="compact"
          total={h.data?.total ?? 0}
          page={page}
          pageSize={taille}
          onPage={setPage}
          onPageSize={(n) => { setTaille(n); setPage(1) }}
          state={h.isPending ? 'loading' : h.isError ? 'error' : rows.length ? 'ok' : 'empty'}
          error={h.error ? { code: h.error.code, message: message(h.error).texte, correlationId: h.error.correlationId } : undefined}
          onRetry={() => void h.refetch()}
          emptyText={t('historique.aucune')}
        />
      </div>
    </Card>
  )
}
