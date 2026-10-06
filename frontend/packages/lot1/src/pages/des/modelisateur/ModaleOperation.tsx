import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Button, Checkbox, ConfirmDialog, Dialog, InlineAlert, KeyValueList, MultiSelect, NumberField, Select, Tabs, Tag, TextArea, TextField, type Option,
} from '@gimac/design-system'
import type { ApiError } from '@gimac/socle'
import type { CatalogueModelisateur, Livrable, NoeudGraphe, ReferenceOperation } from '../../../modele/homologation'
import { erreursSaisie, libelleEntree, livrableVierge, ongletDuChamp } from '../../../adaptateurs/modelisateur'

// Modale de l'opération du modélisateur (D-77, §6) : cinq onglets (Général ; Rôles R, V, C, I, E ; Livrables et
// preuves ; Exécution ; Chronologie). Contrôles de forme avant envoi (V ≠ R, déclencheur d'un événement externe,
// secret sans valeur de référence) ; le service refait tout et ses refus HOM_* sont restitués lisiblement.

type Onglet = 'general' | 'roles' | 'livrables' | 'execution' | 'chronologie'
const ONGLETS: Onglet[] = ['general', 'roles', 'livrables', 'execution', 'chronologie']

export type ModaleOperationProps = {
  /** Opération à modifier, ou opération vierge (création). */
  operation: ReferenceOperation
  creation: boolean
  lectureSeule: boolean
  catalogue: CatalogueModelisateur
  /** Nœuds du graphe : prédécesseurs possibles, successeurs calculés, dates au plus tôt. */
  noeuds: NoeudGraphe[]
  enCours?: boolean
  erreurServeur?: ApiError | null
  onEnregistrer: (o: ReferenceOperation) => void
  onSupprimer?: () => void
  onFermer: () => void
}

/** Choix multiple par jetons, avec suggestions filtrées par la saisie. */
function ChoixMultiple(p: { label: string; options: Option[]; value: string[]; onChange: (v: string[]) => void; lectureSeule: boolean; help?: string; error?: string; name: string }) {
  const [q, setQ] = useState('')
  if (p.lectureSeule) {
    return (
      <div className="gm-field">
        <span className="gm-field-label">{p.label}</span>
        <span className="gm-row" style={{ flexWrap: 'wrap', gap: 'var(--gm-space-1)' }}>
          {p.value.length ? p.value.map((v) => <Tag key={v}>{p.options.find((o) => o.value === v)?.label ?? v}</Tag>) : <span className="gm-field-help">{'∅'}</span>}
        </span>
      </div>
    )
  }
  const filtre = q.trim().toLowerCase()
  return (
    <MultiSelect
      name={p.name}
      label={p.label}
      help={p.help}
      error={p.error}
      value={p.value}
      onChange={p.onChange}
      options={filtre ? p.options.filter((o) => o.label.toLowerCase().includes(filtre) || o.value.toLowerCase().includes(filtre)) : p.options}
      query={q}
      onQuery={setQ}
      suggestions
    />
  )
}

export function ModaleOperation(p: ModaleOperationProps) {
  const { t } = useTranslation()
  const [o, setO] = useState<ReferenceOperation>(p.operation)
  const [onglet, setOnglet] = useState<Onglet>('general')
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [confirmerSuppression, setConfirmerSuppression] = useState(false)
  const lecture = p.lectureSeule
  const maj = (champs: Partial<ReferenceOperation>) => setO((x) => ({ ...x, ...champs }))
  const err = (champ: string) => (erreurs[champ] ? t(erreurs[champ]) : undefined)
  const c = p.catalogue
  const options = useMemo(() => {
    const aucun = { value: '', label: t('modelisateur.aucun') }
    const de = (l: typeof c.fonctions, prefixe: string | null) => l.map((e) => ({ value: e.code, label: libelleEntree(t, prefixe, e) }))
    return {
      aucun,
      fonctions: de(c.fonctions, 'parcours.fonction'),
      // Étapes de la version (catalogue ?version=, D-89), dans leur ordre ; liste du catalogue à défaut.
      etapes: c.etapesVersion?.length
        ? [...c.etapesVersion].sort((a, b) => a.ordre - b.ordre).map((e) => ({ value: e.code, label: e.libelle }))
        : de(c.etapes, 'parcours.etape'),
      segments: de(c.segments, 'parcours.segment'),
      gestes: de(c.gestes, 'modelisateur.geste'),
      jalons: de(c.jalons, 'modelisateur.jalon'),
      declencheurs: c.declencheurs.map((e) => ({ value: e.code, label: libelleEntree(t, 'modelisateur.declencheur', { ...e, code: e.code.replace(/\./g, '_') }) })),
      particularites: de(c.particularites, 'modelisateur.particularite'),
      plateformes: de(c.plateformes, 'parcours.plateforme'),
      grains: de(c.grains, 'parcours.grain'),
      natures: de(c.naturesLivrable, 'modelisateur.nature'),
      pieces: c.typesPieces.map((m) => ({ value: m, label: t(`modelisateur.piece.${m.replace(/[./+-]/g, '_')}`, { defaultValue: m }) })),
      predecesseurs: p.noeuds.filter((n) => !n.fantome && n.code !== o.code).map((n) => ({ value: n.code, label: `${n.code} · ${n.libelle}` })),
    }
  }, [c, p.noeuds, o.code, t])
  const noeud = p.noeuds.find((n) => n.code === p.operation.code && !p.creation)
  const successeurs = p.creation ? [] : (noeud?.successeurs ?? p.noeuds.filter((n) => (n.predecesseurs ?? []).includes(o.code)).map((n) => n.code))

  const enregistrer = () => {
    const e = erreursSaisie(o, p.creation, p.noeuds.map((n) => n.code))
    setErreurs(e)
    const premier = Object.keys(e)[0]
    if (premier) {
      setOnglet(ongletDuChamp(premier))
      return
    }
    p.onEnregistrer(o)
  }
  const majLivrable = (i: number, champs: Partial<Livrable>) => maj({ livrables: o.livrables.map((l, j) => (j === i ? { ...l, ...champs } : l)) })
  const fermer = p.onFermer
  const titre = p.creation ? t('modelisateur.modale.creer') : t('modelisateur.modale.titre', { code: p.operation.code })
  const serveur = p.erreurServeur
  const nbErreurs = (k: Onglet) => Object.keys(erreurs).filter((x) => ongletDuChamp(x) === k).length

  return (
    <Dialog
      open
      id="gm-modale-operation"
      size="lg"
      title={titre}
      subtitle={p.creation ? undefined : p.operation.libelle}
      onClose={fermer}
      footer={
        <>
          {!lecture && !p.creation && p.onSupprimer ? <Button variant="danger" icon="trash" onClick={() => setConfirmerSuppression(true)}>{t('modelisateur.action.supprimer')}</Button> : null}
          <Button variant="ghost" onClick={fermer}>{lecture ? t('commun.fermer') : t('commun.annuler')}</Button>
          {!lecture ? <Button variant="primary" icon="check" loading={p.enCours} onClick={enregistrer}>{p.creation ? t('modelisateur.action.creer') : t('modelisateur.action.enregistrer')}</Button> : null}
        </>
      }
    >
      <div className="gm-col gm-mod-modale">
        {lecture ? <InlineAlert level="info">{t('modelisateur.lectureSeule')}</InlineAlert> : null}
        {serveur ? (
          <InlineAlert level="crit" title={t(`erreur.${serveur.code}`, { defaultValue: serveur.message })} correlationId={serveur.correlationId || undefined} />
        ) : null}
        <Tabs
          items={ONGLETS.map((k) => ({ key: k, label: nbErreurs(k) ? `${t(`modelisateur.onglet.${k}`)} (${nbErreurs(k)})` : t(`modelisateur.onglet.${k}`) }))}
          value={onglet}
          onChange={(k) => setOnglet(k as Onglet)}
        />
        <div role="tabpanel" aria-label={t(`modelisateur.onglet.${onglet}`)} className="gm-col">
          {onglet === 'general' ? (
            <div className="gm-form-grid gm-mod-grille">
              {p.creation ? (
                <TextField name="code" label={t('modelisateur.champ.code')} help={t('modelisateur.aide.code')} value={o.code} required mono maxLength={40} error={err('code')}
                  onChange={(v) => maj({ code: v.toUpperCase() })} />
              ) : (
                <TextField name="code" label={t('modelisateur.champ.code')} value={o.code} readOnly mono />
              )}
              <TextField name="libelle" label={t('modelisateur.champ.libelle')} value={o.libelle} required maxLength={300} readOnly={lecture} error={err('libelle')} onChange={(v) => maj({ libelle: v })} />
              <TextArea name="description" label={t('modelisateur.champ.description')} help={t('modelisateur.aide.description')} value={o.description ?? ''} rows={3} readOnly={lecture}
                error={err('description')} onChange={(v) => maj({ description: v || null })} className="gm-mod-large" />
              <Select name="etape" label={t('modelisateur.champ.etape')} value={o.etape ?? ''} required disabled={lecture} error={err('etape')} options={[options.aucun, ...options.etapes]}
                onChange={(v) => maj({ etape: v || null })} />
              <Select name="segment" label={t('modelisateur.champ.segment')} value={o.segment ?? ''} required disabled={lecture} error={err('segment')} options={[options.aucun, ...options.segments]}
                onChange={(v) => maj({ segment: (v || null) as ReferenceOperation['segment'] })} />
              <NumberField name="ordre" label={t('modelisateur.champ.ordre')} help={t('modelisateur.aide.ordre')} value={o.ordre === null ? '' : String(o.ordre)} min={0} readOnly={lecture}
                onChange={(v) => maj({ ordre: v === '' ? null : Number(v) })} />
            </div>
          ) : null}
          {onglet === 'roles' ? (
            <div className="gm-form-grid gm-mod-grille">
              <Select name="responsable" label={t('modelisateur.champ.responsable')} help={t('modelisateur.aide.responsable')} value={o.responsable ?? ''} required disabled={lecture}
                error={err('responsable')} options={[options.aucun, ...options.fonctions]} onChange={(v) => maj({ responsable: (v || null) as ReferenceOperation['responsable'] })} />
              <Select name="validateur" label={t('modelisateur.champ.validateur')} help={t('modelisateur.aide.validateur')} value={o.validateur ?? ''} required disabled={lecture}
                error={err('validateur')} options={[options.aucun, ...options.fonctions]} onChange={(v) => maj({ validateur: (v || null) as ReferenceOperation['validateur'] })} />
              <ChoixMultiple name="consultes" label={t('modelisateur.champ.consultes')} options={options.fonctions} value={o.consultes} lectureSeule={lecture} onChange={(v) => maj({ consultes: v as ReferenceOperation['consultes'] })} />
              <ChoixMultiple name="informes" label={t('modelisateur.champ.informes')} options={options.fonctions} value={o.informes} lectureSeule={lecture} onChange={(v) => maj({ informes: v as ReferenceOperation['informes'] })} />
              <ChoixMultiple name="externes" label={t('modelisateur.champ.externes')} help={t('modelisateur.aide.externes')} options={options.fonctions.filter((f) => c.fonctions.find((x) => x.code === f.value)?.externe)}
                value={o.externes} lectureSeule={lecture} onChange={(v) => maj({ externes: v as ReferenceOperation['externes'] })} />
            </div>
          ) : null}
          {onglet === 'livrables' ? (
            <div className="gm-col">
              {o.livrables.length ? null : <p className="gm-field-help" style={{ margin: 0 }}>{t('modelisateur.livrable.aucun')}</p>}
              {o.livrables.map((l, i) => (
                <fieldset key={i} className="gm-mod-livrable" aria-label={t('modelisateur.livrable.n', { n: i + 1 })}>
                  <legend className="gm-type-body-strong">{t('modelisateur.livrable.n', { n: i + 1 })}</legend>
                  <div className="gm-form-grid gm-mod-grille">
                    <TextField name={`livrables.${i}.code`} label={t('modelisateur.livrable.code')} value={l.code} mono required readOnly={lecture} error={err(`livrables.${i}.code`)}
                      onChange={(v) => majLivrable(i, { code: v.toUpperCase() })} />
                    <TextField name={`livrables.${i}.libelle`} label={t('modelisateur.livrable.libelle')} value={l.libelle} required readOnly={lecture} error={err(`livrables.${i}.libelle`)}
                      onChange={(v) => majLivrable(i, { libelle: v })} />
                    <Select name={`livrables.${i}.nature`} label={t('modelisateur.livrable.nature')} value={l.nature} disabled={lecture} options={options.natures}
                      onChange={(v) => majLivrable(i, { nature: v as Livrable['nature'] })} />
                    <Select name={`livrables.${i}.deposant`} label={t('modelisateur.livrable.deposant')} help={t('modelisateur.aide.deposant')} value={l.deposant ?? ''} required disabled={lecture}
                      error={err(`livrables.${i}.deposant`)} options={[options.aucun, ...options.fonctions]} onChange={(v) => majLivrable(i, { deposant: (v || null) as Livrable['deposant'] })} />
                    <ChoixMultiple name={`livrables.${i}.typesPieces`} label={t('modelisateur.livrable.types')} options={options.pieces} value={l.typesPieces} lectureSeule={lecture}
                      onChange={(v) => majLivrable(i, { typesPieces: v })} />
                    <Checkbox name={`livrables.${i}.obligatoire`} label={t('modelisateur.livrable.obligatoire')} checked={l.obligatoire} disabled={lecture} onChange={(v) => majLivrable(i, { obligatoire: v })} />
                  </div>
                  {!lecture ? (
                    <div className="gm-row" style={{ justifyContent: 'flex-end' }}>
                      <Button size="sm" variant="ghost" icon="trash" onClick={() => maj({ livrables: o.livrables.filter((_, j) => j !== i) })}>
                        {t('modelisateur.livrable.retirer', { n: i + 1 })}
                      </Button>
                    </div>
                  ) : null}
                </fieldset>
              ))}
              {!lecture ? (
                <div>
                  <Button size="sm" icon="plus" onClick={() => maj({ livrables: [...o.livrables, livrableVierge(o.livrables.length + 1, o.responsable)] })}>{t('modelisateur.livrable.ajouter')}</Button>
                </div>
              ) : null}
            </div>
          ) : null}
          {onglet === 'execution' ? (
            <div className="gm-form-grid gm-mod-grille">
              <Select name="geste" label={t('modelisateur.champ.geste')} value={o.geste ?? ''} disabled={lecture} options={[options.aucun, ...options.gestes]}
                onChange={(v) => maj({ geste: (v || null) as ReferenceOperation['geste'], declencheur: v === 'EVENEMENT_EXTERNE' ? o.declencheur : null })} />
              <Select name="declencheur" label={t('modelisateur.champ.declencheur')} help={t('modelisateur.aide.declencheur')} value={o.declencheur ?? ''} required={o.geste === 'EVENEMENT_EXTERNE'}
                disabled={lecture || o.geste !== 'EVENEMENT_EXTERNE'} error={err('declencheur')} options={[options.aucun, ...options.declencheurs]} onChange={(v) => maj({ declencheur: v || null })} />
              <Select name="jalon" label={t('modelisateur.champ.jalon')} help={t('modelisateur.aide.jalon')} value={o.jalon ?? ''} disabled={lecture} options={[options.aucun, ...options.jalons]}
                onChange={(v) => maj({ jalon: (v || null) as ReferenceOperation['jalon'] })} />
              <Select name="particularite" label={t('modelisateur.champ.particularite')} value={o.particularite ?? ''} disabled={lecture} options={[options.aucun, ...options.particularites]}
                onChange={(v) => maj({ particularite: (v || null) as ReferenceOperation['particularite'] })} />
              <Select name="plateforme" label={t('modelisateur.champ.plateforme')} value={o.plateforme ?? ''} disabled={lecture} options={[options.aucun, ...options.plateformes]}
                onChange={(v) => maj({ plateforme: (v || null) as ReferenceOperation['plateforme'] })} />
              <Select name="grain" label={t('modelisateur.champ.grain')} value={o.grain ?? ''} disabled={lecture} options={[options.aucun, ...options.grains]}
                onChange={(v) => maj({ grain: (v || null) as ReferenceOperation['grain'] })} />
              <TextField name="valeurReference" label={t('modelisateur.champ.valeurReference')} help={o.secret ? t('modelisateur.aide.secret') : t('modelisateur.aide.valeurReference')}
                value={o.valeurReference ?? ''} disabled={lecture || o.secret} maxLength={120} error={err('valeurReference')} onChange={(v) => maj({ valeurReference: v || null })} />
              <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
                <Checkbox name="cle" label={t('modelisateur.champ.cle')} checked={o.cle} disabled={lecture} onChange={(v) => maj({ cle: v })} />
                <Checkbox name="secret" label={t('modelisateur.champ.secret')} checked={o.secret} disabled={lecture} onChange={(v) => maj({ secret: v, valeurReference: v ? null : o.valeurReference })} />
                <Checkbox name="oubliSansSymptome" label={t('modelisateur.champ.oubliSansSymptome')} checked={o.oubliSansSymptome} disabled={lecture} onChange={(v) => maj({ oubliSansSymptome: v })} />
                <Checkbox name="pivot" label={t('modelisateur.champ.pivot')} checked={o.pivot} disabled={lecture} onChange={(v) => maj({ pivot: v })} />
              </div>
            </div>
          ) : null}
          {onglet === 'chronologie' ? (
            <div className="gm-form-grid gm-mod-grille">
              <ChoixMultiple name="predecesseurs" label={t('modelisateur.champ.predecesseurs')} help={t('modelisateur.aide.predecesseurs')} options={options.predecesseurs} value={o.predecesseurs}
                lectureSeule={lecture} error={err('predecesseurs')} onChange={(v) => maj({ predecesseurs: v })} />
              <NumberField name="delaiJours" label={t('modelisateur.champ.delai')} unit={t('modelisateur.jours')} value={o.delaiJours === null ? '' : String(o.delaiJours)} min={0} max={365} required
                readOnly={lecture} error={err('delaiJours')} onChange={(v) => maj({ delaiJours: v === '' ? null : Number(v) })} />
              <div className="gm-col gm-mod-large" style={{ gap: 'var(--gm-space-2)' }}>
                <KeyValueList
                  items={[
                    { key: t('modelisateur.champ.successeurs'), value: successeurs.length ? successeurs.join(', ') : '∅', mono: true },
                    { key: t('modelisateur.champ.debut'), value: noeud?.debutAuPlusTot === null || noeud?.debutAuPlusTot === undefined ? t('modelisateur.calculAEnregistrer') : t('modelisateur.jourN', { n: noeud.debutAuPlusTot }) },
                    { key: t('modelisateur.champ.fin'), value: noeud?.finAuPlusTot === null || noeud?.finAuPlusTot === undefined ? t('modelisateur.calculAEnregistrer') : t('modelisateur.jourN', { n: noeud.finAuPlusTot }) },
                  ]}
                />
              </div>
            </div>
          ) : null}
        </div>
      </div>
      {confirmerSuppression ? (
        <ConfirmDialog
          open
          danger
          title={t('modelisateur.action.supprimer')}
          confirmLabel={t('modelisateur.action.supprimer')}
          object={[{ key: t('modelisateur.champ.code'), value: p.operation.code, mono: true }, { key: t('modelisateur.champ.libelle'), value: p.operation.libelle }]}
          consequences={[t('modelisateur.suppression.consequence')]}
          onConfirm={() => {
            setConfirmerSuppression(false)
            p.onSupprimer?.()
          }}
          onClose={() => setConfirmerSuppression(false)}
        />
      ) : null}
    </Dialog>
  )
}
