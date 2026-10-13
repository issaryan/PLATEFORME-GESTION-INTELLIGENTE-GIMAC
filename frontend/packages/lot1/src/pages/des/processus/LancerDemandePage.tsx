import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, Card, EmptyState, InlineAlert, PageHeader, StatusBadge, TextField } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { ResumeProcessus } from '../../../modele/processus'
import { useLancables, useProcessus } from '../../../hooks/processus'
import { useOrganisation } from '../../../hooks/pilotage'
import { useLibelleUnite } from '../activite/communs'
import { DialogueLancement } from './DialogueLancement'
import { useErreurProcessus } from './communs'

// « Lancer une demande » (D-98 §3, /des/demandes/lancer) : processus publiés que je peux lancer (public défini par le
// créateur de chaque définition, seul juge depuis D-100 : un administrateur n'y voit pas les processus réservés, servis
// par MS-PILOTAGE), groupés par service destinataire (unité propriétaire), mes unités d'abord, avec
// recherche ; le formulaire de lancement s'ouvre en fenêtre modale, puis l'instance créée (suivi).

/** Lancement : la définition complète (formulaire, désignations) est lue avant d'ouvrir la fenêtre. */
function Lancement({ id, onFermer }: { id: string; onFermer: () => void }) {
  const navigate = useNavigate()
  const { t } = useTranslation()
  const erreur = useErreurProcessus()
  const p = useProcessus(id)
  if (p.isError) return <InlineAlert level="crit" title={erreur(p.error)?.titre ?? t('proc.erreur.lecture')} />
  return p.data ? <DialogueLancement demande processus={p.data} onFermer={onFermer} onLance={(i) => navigate(`/des/demandes/${encodeURIComponent(i.id)}`)} /> : null
}

function CarteProcessus({ p, onLancer }: { p: ResumeProcessus; onLancer: () => void }) {
  const { t } = useTranslation()
  const ouvert = p.initiateurs?.regles.includes('TOUT_LE_GIMAC')
  // D-100 : processus réservé par son créateur à des personnes désignées (dont moi, puisque le service me le propose).
  const designe = !ouvert && p.initiateurs?.regles.includes('PERSONNES')
  return (
    <Card title={p.libelle} actions={<Button size="sm" variant="primary" icon="send" aria-label={t('dem.lancer.qui', { libelle: p.libelle })} onClick={onLancer}>{t('dem.lancer.action')}</Button>}>
      <div className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
        {p.description ? <p style={{ margin: 0 }}>{p.description}</p> : null}
        <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
          {p.categorie ? <StatusBadge status="neutral" icon={null} label={p.categorie} /> : null}
          {ouvert ? <StatusBadge status="info" icon={null} label={t('dem.lancer.ouvertATous')} /> : null}
          {designe ? <StatusBadge status="info" icon={null} label={t('dem.lancer.designe')} /> : null}
          <span className="gm-field-help">
            {t('dem.lancer.etapes', { count: p.nombreEtapes })}
            {p.nombreChampsFormulaire ? ` · ${t('dem.lancer.champs', { count: p.nombreChampsFormulaire })}` : ''}
            {p.delaiCibleJours !== null ? ` · ${t('dem.lancer.delai', { n: p.delaiCibleJours })}` : ''}
            {p.delaiMaxJours !== null && p.delaiMaxJours !== undefined && p.delaiMaxJours !== p.delaiCibleJours ? ` · ${t('dem.lancer.delaiMax', { n: p.delaiMaxJours })}` : ''}
          </span>
        </span>
      </div>
    </Card>
  )
}

export default function LancerDemandePage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const libelleUnite = useLibelleUnite()
  const organisation = useOrganisation()
  const [q, setQ] = useState('')
  const [lancer, setLancer] = useState<string | null>(null)
  const lancables = useLancables({ q: q || undefined })
  // Mes unités d'abord (processus internes de mon service), puis les autres services par libellé.
  const miennes = new Set([...(organisation.data?.chemin ?? []).map((u) => u.code), ...(organisation.data?.unitesDirigees ?? [])])
  return (
    <div className="gm-col" data-testid="lancer-demande">
      <PageHeader title={t('dem.lancer.titre')} subtitle={t('dem.lancer.sousTitre')}
        actions={<Button size="sm" variant="secondary" icon="inbox" onClick={() => navigate('/des/demandes')}>{t('dem.mesDemandes.titre')}</Button>} />
      <div style={{ maxWidth: 520 }}>
        <TextField id="dem-recherche" name="q" label={t('dem.lancer.recherche')} help={t('dem.lancer.rechercheAide')} value={q} onChange={setQ} />
      </div>
      <EtatsRequete etat={lancables} emptyTitle={t('dem.lancer.vide')} errorTitle={t('dem.erreur.lecture')} retryLabel={t('commun.reessayer')}>
        {(l) => (l.groupes.length ? (
          <div className="gm-col" style={{ gap: 'var(--gm-space-4)' }}>
            {[...l.groupes].sort((a, b) => Number(!miennes.has(a.unite)) - Number(!miennes.has(b.unite)) || libelleUnite(a.unite).localeCompare(libelleUnite(b.unite), 'fr')).map((g) => (
              <section key={g.unite} className="gm-col" aria-labelledby={`dem-unite-${g.unite}`} style={{ gap: 'var(--gm-space-2)' }}>
                <h2 id={`dem-unite-${g.unite}`} className="gm-type-h3" style={{ margin: 0 }}>
                  {libelleUnite(g.unite)}{miennes.has(g.unite) ? <span className="gm-field-help"> · {t('proc.monUnite')}</span> : null}
                </h2>
                <div className="gm-dem-cartes">
                  {g.processus.map((p) => <CarteProcessus key={p.id} p={p} onLancer={() => setLancer(p.id)} />)}
                </div>
              </section>
            ))}
          </div>
        ) : <EmptyState icon="send" title={t('dem.lancer.vide')} text={t(q ? 'dem.lancer.videRecherche' : 'dem.lancer.videTexte')} />)}
      </EtatsRequete>
      {lancer ? <Lancement id={lancer} onFermer={() => setLancer(null)} /> : null}
    </div>
  )
}
