import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, CrudDialog, DataTable, InlineAlert, KeyValueList, Select, StatusBadge, Tag, TextArea, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { DossierLiaison, EtapeCode, IntervenantLiaison, OperationParcours, Personne, Qualite } from '../../../../modele/parcours'
import { useAffecterFonction, useOperationsParcours, usePersonnes } from '../../../../hooks/parcours'
import { useFonctionsCatalogue } from '../../../../hooks/referentiels'
import { PERM } from '../../../../permissions'
import { Donnees, useErreurService } from '../../liaisons/communs'
import { useRetour, Valeur } from '../communs'
import { NON_AFFECTABLES } from '../DialogueAffectation'

// Onglet « Gouvernance » (CR §3.6) : intervenants de chaque étape selon leur qualité (R responsable,
// V validateur, C consulté, I informé, E externe), tels que les services les attribuent aux opérations ;
// interlocuteurs du participant et tiers (banque partenaire, personnalisateur).

type LigneRaci = { etape: EtapeCode } & Record<Qualite, Personne[]>
const QUALITES: Qualite[] = ['R', 'V', 'C', 'I', 'E']

/** Regroupe, par étape, les intervenants distincts que servent les opérations (simple regroupement d'affichage). */
function parEtape(ops: OperationParcours[]): LigneRaci[] {
  const lignes = new Map<EtapeCode, LigneRaci>()
  const ajouter = (l: Personne[], p: Personne | null) => {
    if (p && !l.some((x) => x.id === p.id)) l.push(p)
  }
  ops.forEach((o) => {
    const l = lignes.get(o.etape) ?? { etape: o.etape, R: [], V: [], C: [], I: [], E: [] }
    ajouter(l.R, o.raci.R)
    ajouter(l.V, o.raci.V)
    o.raci.C.forEach((p) => ajouter(l.C, p))
    o.raci.I.forEach((p) => ajouter(l.I, p))
    o.raci.E.forEach((p) => ajouter(l.E, p))
    lignes.set(o.etape, l)
  })
  return [...lignes.values()]
}

function Personnes({ l }: { l: Personne[] }) {
  const { t } = useTranslation()
  const f = useFonctionsCatalogue()
  if (!l.length) return <span className="gm-field-help">{t('parcoursGouvernance.aucun')}</span>
  return (
    <ul className="gm-col" style={{ gap: 0, listStyle: 'none', margin: 0, padding: 0 }}>
      {l.map((p) => (
        <li key={p.id} title={f.libelle(p.fonction)}>{p.aAffecter ? t('parcoursOps.aAffecter', { fonction: f.libelle(p.fonction) }) : p.nom}</li>
      ))}
    </ul>
  )
}

/** Affectation d'une fonction « à affecter » (D-93 §8.1) : une personne qui porte un profil de la fonction, motif facultatif. */
function DialogueAffecterFonction({ dossier, intervenant, onFermer, onAffecte }: { dossier: DossierLiaison; intervenant: IntervenantLiaison; onFermer: () => void; onAffecte: (nom: string) => void }) {
  const { t } = useTranslation()
  const personnes = usePersonnes(intervenant.fonction)
  const affecter = useAffecterFonction()
  const erreur = useErreurService()(affecter.error)
  const [personneId, setPersonneId] = useState(intervenant.aAffecter ? '' : intervenant.personne?.id ?? '')
  const [motif, setMotif] = useState('')
  const [tente, setTente] = useState(false)
  const choix = (personnes.data ?? []).find((p) => p.id === personneId)
  return (
    <CrudDialog context="edit" size="md" id="gm-dlg-affecter-fonction" title={t('parcoursGouvernance.affecter.titre', { fonction: intervenant.libelle })}
      subject={`${dossier.reference} · ${dossier.institutionNom}`} description={t('parcoursGouvernance.affecter.texte')} onClose={onFermer}
      submitLabel={t('parcoursGouvernance.affecter.valider')} submitIcon="users" submitting={affecter.isPending} dirty={!!personneId || !!motif}
      error={erreur ? { title: erreur.message, correlationId: erreur.correlationId } : undefined}
      onSubmit={() => {
        setTente(true)
        if (!choix) return
        affecter.mutate({ liaisonId: dossier.id, fonction: intervenant.fonction, personneId: choix.id, motif: motif.trim() || undefined }, { onSuccess: () => onAffecte(choix.nom) })
      }}>
      <Select name="personne" label={t('parcoursGouvernance.affecter.personne')} required value={personneId} onChange={setPersonneId} disabled={personnes.isPending}
        options={[{ value: '', label: t('parcoursOps.attribution.choisir') }, ...(personnes.data ?? []).map((p) => ({ value: p.id, label: p.nom }))]}
        error={tente && !choix ? t('parcoursOps.attribution.personneRequise') : undefined} />
      <TextArea name="motif" label={t('parcoursGouvernance.affecter.motif')} rows={3} maxLength={500} value={motif} onChange={setMotif} />
    </CrudDialog>
  )
}

/** Intervenants de la liaison lus du RACI (D-93 §8.5) : fonction, qualités, personne désignée ou fonction à affecter. */
function Intervenants({ dossier, fonction }: { dossier: DossierLiaison; fonction?: string | null }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const [choisi, setChoisi] = useState<IntervenantLiaison | null>(null)
  const [retour, annoncer] = useRetour()
  const peutAffecter = dossier.actionsPermises.includes('AFFECTER_FONCTION') && aPermission(permissions, PERM.etapeConduire, PERM.operationAttribuer)
  const l = dossier.intervenants ?? []
  // D-98 : fonction demandée par la tâche qui mène ici (?fonction=) : signalée en tête, avec son affectation.
  const demandee = fonction ? l.find((i) => i.fonction === fonction) : undefined
  const colonnes: Column<IntervenantLiaison>[] = [
    { key: 'libelle', header: t('parcoursGouvernance.fonction'), render: (i) => <span className="gm-col" style={{ gap: 0 }}><span>{i.libelle}</span><span className="gm-mono gm-type-small">{i.fonction}</span></span> },
    { key: 'qualites', header: t('parcoursGouvernance.qualites'), render: (i) => <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>{i.qualites.map((q) => <Tag key={q}>{q}</Tag>)}</span> },
    {
      key: 'personne', header: t('parcoursGouvernance.personne'),
      render: (i) => (i.aAffecter ? <StatusBadge status="warn" label={t('parcoursOps.aAffecterCourt')} /> : <Valeur>{i.personne?.nom}</Valeur>),
    },
    {
      key: 'action', header: '', type: 'actions',
      render: (i) => (peutAffecter && !i.externe && !NON_AFFECTABLES.includes(i.fonction)
        ? <Button size="sm" variant={i.aAffecter ? 'primary' : 'secondary'} icon="user" aria-label={t('parcoursGouvernance.affecter.qui', { fonction: i.libelle })} onClick={() => setChoisi(i)}>
            {t(i.aAffecter ? 'parcoursGouvernance.affecter.bouton' : 'parcoursGouvernance.affecter.changer')}
          </Button>
        : null),
    },
  ]
  return (
    <Card title={t('parcoursGouvernance.intervenants', { count: l.length })}>
      <div className="gm-col">
        {demandee ? (
          demandee.aAffecter ? (
            <InlineAlert level="warn" title={t('certif.fonction.demandee.titre', { fonction: demandee.libelle })}
              action={peutAffecter && !demandee.externe && !NON_AFFECTABLES.includes(demandee.fonction) ? <Button size="sm" variant="primary" icon="user" onClick={() => setChoisi(demandee)}>{t('parcoursGouvernance.affecter.bouton')}</Button> : undefined}>
              {t('certif.fonction.demandee.texte')}
            </InlineAlert>
          ) : <InlineAlert level="info">{t('certif.fonction.demandee.affectee', { fonction: demandee.libelle, nom: demandee.personne?.nom ?? '' })}</InlineAlert>
        ) : null}
        {l.some((i) => i.aAffecter) ? <InlineAlert level="warn" title={t('parcoursGouvernance.aAffecter.titre')}>{t('parcoursGouvernance.aAffecter.texte')}</InlineAlert> : null}
        <DataTable columns={colonnes} rows={l} rowKey={(i) => i.fonction} density="compact" state={l.length ? 'ok' : 'empty'} emptyText={t('parcoursGouvernance.aucun')} />
        {retour}
      </div>
      {choisi ? (
        <DialogueAffecterFonction dossier={dossier} intervenant={choisi} onFermer={() => setChoisi(null)}
          onAffecte={(nom) => { annoncer(t('parcoursGouvernance.affecter.fait', { fonction: choisi.libelle, nom })); setChoisi(null) }} />
      ) : null}
    </Card>
  )
}

export function OngletGouvernance({ dossier, fonction }: { dossier: DossierLiaison; fonction?: string | null }) {
  const { t } = useTranslation()
  const ops = useOperationsParcours(dossier.id)
  const colonnes: Column<LigneRaci>[] = [
    { key: 'etape', header: t('parcoursEtapes.etape'), render: (l) => dossier.etapesVersion?.find((e) => e.code === l.etape)?.libelle ?? t(`parcours.etape.${l.etape}`) },
    ...QUALITES.map((q) => ({ key: q, header: `${q} · ${t(`parcours.qualite.${q}`)}`, render: (l: LigneRaci) => <Personnes l={l[q]} /> })),
  ]
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ alignItems: 'stretch', gap: 'var(--gm-space-4)' }}>
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <Card title={t('parcoursGouvernance.gimac')}>
            <KeyValueList
              items={[
                { key: t('parcoursDossier.conducteur'), value: <Valeur>{dossier.conducteur?.nom}</Valeur> },
                ...(dossier.responsableFonctionnel ? [{ key: t('parcoursDossier.responsableFonctionnel'), value: <Valeur>{dossier.responsableFonctionnel.nom}</Valeur> }] : []),
                ...(dossier.responsableTechnique ? [{ key: t('parcoursDossier.responsableTechnique'), value: <Valeur>{dossier.responsableTechnique.nom}</Valeur> }] : []),
              ]}
            />
          </Card>
        </div>
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <Card title={t('parcoursGouvernance.participant')}>
            {dossier.interlocuteursParticipant.length ? (
              <KeyValueList items={dossier.interlocuteursParticipant.map((i) => ({ key: i.fonction, value: i.nom }))} />
            ) : (
              <span className="gm-field-help">{t('parcoursGouvernance.aucunInterlocuteur')}</span>
            )}
          </Card>
        </div>
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <Card title={t('parcoursGouvernance.tiers')}>
            <KeyValueList
              items={[
                { key: t('parcours.fonction.BANQUE_PARTENAIRE'), value: dossier.banquePartenaireNom ?? <span className="gm-field-help">{t('parcoursDossier.sansBanque')}</span> },
                { key: t('parcours.fonction.PERSONNALISATEUR'), value: dossier.personnalisateurNom ?? <span className="gm-field-help">{t('parcoursGouvernance.sansPersonnalisateur')}</span> },
              ]}
            />
          </Card>
        </div>
      </div>
      <Intervenants dossier={dossier} fonction={fonction} />
      <Card title={t('parcoursGouvernance.raci')}>
        <div className="gm-col">
          <span className="gm-field-help">{t('parcoursGouvernance.raci.aide')}</span>
          <Donnees requete={ops} estVide={(l) => l.length === 0} vide={t('parcoursEtapes.aucuneOperation')}>
            {(l) => <DataTable columns={colonnes} rows={parEtape(l)} rowKey={(x) => x.etape} density="compact" />}
          </Donnees>
        </div>
      </Card>
    </div>
  )
}
