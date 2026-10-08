import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, Card, EmptyState, PageHeader, Select, StatusBadge, TextField } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import { ETATS_INSTANCE, etapesEnCours, type DemandeProcessus, type EtapeDemande, type EtatInstance } from '../../../modele/processus'
import { useDemandes } from '../../../hooks/processus'
import { useLibelleProfilEquipe } from '../activite/dialoguesEquipe'
import { useDates, useLibelleUnite } from '../activite/communs'
import { BadgeEtape, BadgeInstance } from './communs'

// « Mes demandes » (D-98 §3, /des/demandes) : instances que j'ai lancées, servies par MS-PILOTAGE (GET /processus/demandes) et
// relues périodiquement ; pour chacune, l'étape en cours et la personne qui la tient (ou le service qui doit la prendre),
// l'état de chaque étape, la dernière évolution ; un clic ouvre le suivi de l'instance (graphe, historique, valeurs).

const PAR_PAGE = 10

/** « Tenue par Ingrid Mballa », « À prendre : Pôle réseau (Pôle réseau) ». */
export function useQuiTient() {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfilEquipe()
  const libelleUnite = useLibelleUnite()
  return (e: Pick<EtapeDemande, 'executant' | 'aPrendre'>) => {
    if (e.executant) return t('dem.tenuePar', { nom: e.executant.nomAffiche })
    if (e.aPrendre) {
      return t('dem.aPrendre', {
        profil: e.aPrendre.profil ? libelleProfil(e.aPrendre.profil) : t('proc.suivi.toutMembre'),
        unite: e.aPrendre.unite ? libelleUnite(e.aPrendre.unite) : t('proc.suivi.toutLeGimac'),
      })
    }
    return ''
  }
}

function Demande({ d }: { d: DemandeProcessus }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const dates = useDates()
  const libelleUnite = useLibelleUnite()
  const quiTient = useQuiTient()
  const enCours = etapesEnCours(d.etapes)
  const ouvrir = (etape?: string) => navigate(`/des/demandes/${encodeURIComponent(d.id)}${etape ? `?etape=${encodeURIComponent(etape)}` : ''}`)
  return (
    <Card
      title={(
        <span className="gm-col" style={{ gap: 0 }}>
          <button type="button" className="gm-link gm-dem-titre" onClick={() => ouvrir()}>{d.intitule}</button>
          <span className="gm-type-small">{t('dem.vers', { processus: d.processus.libelle, unite: libelleUnite(d.unite) })}</span>
        </span>
      )}
      actions={<BadgeInstance etat={d.etat} />}>
      <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }} data-testid={`demande-${d.id}`}>
        {enCours.length ? enCours.map((e) => (
          <p key={e.code} className="gm-dem-encours" style={{ margin: 0 }}>
            <strong>{t('dem.etapeEnCours')} : </strong>{e.libelle} <BadgeEtape etat={e.etat} />
            {quiTient(e) ? <span> · {quiTient(e)}</span> : null}
            {e.echeance ? <span className="gm-field-help"> · {t('proc.suivi.echeance', { date: dates.jour(e.echeance) })}</span> : null}
            {e.enRetard ? <> <StatusBadge status="crit" icon="clock" label={t('proc.suivi.enRetard')} /></> : null}
          </p>
        )) : d.etat === 'CLOSE' ? <p style={{ margin: 0 }}>{t('dem.close', { date: d.closeLe ? dates.instant(d.closeLe) : '' })}</p>
          : d.etat === 'ANNULEE' ? <p style={{ margin: 0 }}>{t('dem.annulee', { motif: d.motif ?? '' })}</p> : null}
        <ol className="gm-dem-etapes" aria-label={t('dem.etapes', { n: d.avancement.terminees, total: d.avancement.total })}>
          {d.etapes.map((e) => (
            <li key={e.code} className={`gm-dem-etape gm-dem-etape-${e.etat.toLowerCase()}`}>
              <button type="button" className="gm-dem-etape-bouton" aria-label={t('dem.ouvrirEtape', { libelle: e.libelle, etat: t(`proc.etape.etat.${e.etat}`) })} onClick={() => ouvrir(e.code)}>
                <span className="gm-dem-etape-libelle">{e.libelle}</span>
                <BadgeEtape etat={e.etat} />
                {e.etat === 'TERMINEE' && e.executant ? <span className="gm-field-help">{e.executant.nomAffiche}</span> : null}
              </button>
            </li>
          ))}
        </ol>
        <span className="gm-field-help">
          {t('dem.lanceeLe', { date: dates.instant(d.lanceeLe) })}
          {d.derniereEvolution ? ` · ${t('dem.derniere', {
            action: t(`proc.historique.${d.derniereEvolution.action}`, { defaultValue: d.derniereEvolution.action }),
            date: dates.instant(d.derniereEvolution.quand),
            qui: d.derniereEvolution.acteur?.nomAffiche ?? '',
          })}` : ''}
        </span>
      </div>
    </Card>
  )
}

export default function MesDemandesPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [etat, setEtat] = useState<EtatInstance | ''>('')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const demandes = useDemandes({ etat: etat || undefined, q: q || undefined, page, taille: PAR_PAGE })
  const total = demandes.data?.total ?? 0
  return (
    <div className="gm-col" data-testid="mes-demandes">
      <PageHeader title={t('dem.mesDemandes.titre')} subtitle={t('dem.mesDemandes.sousTitre')}
        actions={<Button variant="primary" icon="send" onClick={() => navigate('/des/demandes/lancer')}>{t('dem.lancer.titre')}</Button>} />
      <div className="gm-row" style={{ gap: 'var(--gm-space-3)', flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div style={{ minWidth: 220 }}>
          <Select name="etat" label={t('proc.filtre.etat')} value={etat} onChange={(v) => { setEtat(v as EtatInstance | ''); setPage(0) }}
            options={[{ value: '', label: t('proc.filtre.tous') }, ...ETATS_INSTANCE.map((e) => ({ value: e, label: t(`proc.instance.etat.${e}`) }))]} />
        </div>
        <div style={{ minWidth: 260, flex: 1, maxWidth: 520 }}>
          <TextField id="dem-filtre-q" name="q" label={t('proc.filtre.recherche')} help={t('dem.mesDemandes.rechercheAide')} value={q} onChange={(v) => { setQ(v); setPage(0) }} />
        </div>
      </div>
      <EtatsRequete etat={demandes} emptyTitle={t('dem.mesDemandes.vide')} errorTitle={t('dem.erreur.lecture')} retryLabel={t('commun.reessayer')}>
        {(p) => (p.contenu.length ? (
          <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
            {p.contenu.map((d) => <Demande key={d.id} d={d} />)}
            {total > PAR_PAGE ? (
              <div className="gm-row" style={{ gap: 'var(--gm-space-2)', alignItems: 'center' }}>
                <Button size="sm" variant="secondary" icon="arrow-left" disabled={page === 0} disabledReason={t('dem.pagePremiere')} onClick={() => setPage((x) => x - 1)}>{t('dem.pagePrecedente')}</Button>
                <span className="gm-field-help">{t('dem.page', { n: page + 1, total: Math.ceil(total / PAR_PAGE) })}</span>
                <Button size="sm" variant="secondary" icon="arrow-right" disabled={(page + 1) * PAR_PAGE >= total} disabledReason={t('dem.pageDerniere')} onClick={() => setPage((x) => x + 1)}>{t('dem.pageSuivante')}</Button>
              </div>
            ) : null}
          </div>
        ) : (
          <EmptyState icon="inbox" title={t('dem.mesDemandes.vide')} text={t(etat || q ? 'dem.mesDemandes.videFiltre' : 'dem.mesDemandes.videTexte')}
            action={<Button variant="primary" icon="send" onClick={() => navigate('/des/demandes/lancer')}>{t('dem.lancer.titre')}</Button>} />
        ))}
      </EtatsRequete>
    </div>
  )
}
