import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, DataTable, InlineAlert, PageHeader, PageSection, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useEspaces } from '../hooks'
import { PERMISSION, type Espace, type Ouverture } from '../modele'
import { BadgeEspace, etatListe, useFormat } from '../composants/communs'
import { OuvertureEspace } from '../composants/OuvertureEspace'

// « Espaces des institutions » (D-118 point 3, /des/sandbox/espaces) : espaces de la Sandbox GIMAC servis par
// SBX-ESPACE, par pages de 20 parcourues par curseur ; ouverture d'un espace pour SBX_HAB_ADMINISTRATION.

export const TAILLE = 20

export default function EspacesPage() {
  const { t } = useTranslation()
  const f = useFormat()
  const naviguer = useNavigate()
  const permissions = usePermissions()
  // Pile des curseurs : la page n est lue après le curseur curseurs[n] (vide pour la première).
  const [curseurs, setCurseurs] = useState<string[]>([''])
  const [ouverture, setOuverture] = useState(false)
  const [ouverte, setOuverte] = useState<Ouverture | null>(null)
  const page = curseurs.length - 1
  const q = useEspaces(curseurs[page] || undefined, TAILLE)
  const lignes = q.data?.elements ?? []
  const suivant = q.data?.suivant ?? null
  const colonnes: Column<Espace>[] = [
    { key: 'codeEspace', header: t('sbx.espace.code'), type: 'mono' },
    { key: 'codeParticipant', header: t('sbx.espace.codeParticipant'), type: 'mono' },
    { key: 'etat', header: t('sbx.espace.etat'), render: (x) => <BadgeEspace etat={x.etat} /> },
    { key: 'statutRegistre', header: t('sbx.espace.statutRegistre'), render: (x) => (x.statutRegistre ? t(`sbx.registre.${x.statutRegistre}`, { defaultValue: x.statutRegistre }) : t('sbx.espace.inconnu')) },
    { key: 'administrateursActifs', header: t('sbx.espace.administrateurs'), render: (x) => String(x.administrateursActifs) },
    { key: 'dateOuverture', header: t('sbx.espace.dateOuverture'), render: (x) => f.instant(x.dateOuverture) || t('sbx.espace.sansDate') },
  ]
  return (
    <div className="gm-col">
      <PageHeader title={t('sbx.espaces.titre')} subtitle={t('sbx.espaces.sousTitre')}
        actions={aPermission(permissions, PERMISSION.administration) ? <Button variant="primary" icon="plus" onClick={() => setOuverture(true)}>{t('sbx.ouverture.titre')}</Button> : undefined} />
      {ouverte ? <InlineAlert level="info" title={t(`sbx.ouverture.resultat.${ouverte.etatSaga}`, { code: ouverte.codeParticipant })}>{ouverte.motifEchec ?? undefined}</InlineAlert> : null}
      <PageSection title={t('sbx.espaces.liste')} description={t('sbx.espaces.page', { page: page + 1 })}
        actions={
          <span className="gm-row">
            <Button variant="ghost" icon="arrow-left" disabled={page === 0} onClick={() => setCurseurs((c) => c.slice(0, -1))}>{t('sbx.espaces.precedente')}</Button>
            <Button variant="ghost" icon="arrow-right" disabled={!suivant} onClick={() => setCurseurs((c) => [...c, suivant as string])}>{t('sbx.espaces.suivante')}</Button>
          </span>
        }>
        <DataTable columns={colonnes} rows={lignes} rowKey={(x) => x.codeParticipant} pagination={false} onRowOpen={(x) => naviguer(encodeURIComponent(x.codeParticipant))}
          emptyText={t('sbx.espaces.aucun')} emptyHelp={t('sbx.espaces.aucunAide')} {...etatListe(q, lignes)} onRetry={() => void q.refetch()} />
      </PageSection>
      {ouverture ? <OuvertureEspace onFermer={() => setOuverture(false)} onOuvert={(o) => { setOuverture(false); setOuverte(o) }} /> : null}
    </div>
  )
}
