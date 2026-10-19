import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, DataTable, DateField, InlineAlert, KeyValueList, Select, StatusBadge, TextArea, TextField, Note, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { usePersonnes } from '../../../../hooks/parcours'
import { useFonctionsCatalogue } from '../../../../hooks/referentiels'
import type { Fonction, LivrableAttendu, OperationParcours, Qualite, SignatureOperation } from '../../../../modele/parcours'
import { useExecuteurActions } from '../../../../ressources/actions'
import { PERM } from '../../../../permissions'
import { ChampFichier } from '../../../commun/ChampFichier'
import { useActionsOperation } from './actions'
import { lireCodeControle } from './regles'
import { preuveAttendue } from './Detail'
import { libelleOperation } from '../../../../adaptateurs/modelisateur'
import { useFormatDate } from '../communs'
import { BadgeLivrable } from './Preuves'

// Formulaires d'exécution d'une opération, dans le tiroir : dépôt de preuve (valeur de référence, code
// de contrôle, cérémonie de clés), dépôt de livrable de phase, validation par le validateur, attribution
// des qualités. Un formulaire n'apparaît que si le service le permet ET si le jeton porte la permission.

const SUJET = 'parcours'
const vide = (s: string) => !s.trim()

function ChampCodeControle({ label, valeur, onChange, erreur, help }: { label: string; valeur: string; onChange: (v: string) => void; erreur?: string; help?: string }) {
  const { t } = useTranslation()
  const [refus, setRefus] = useState(false)
  return (
    <TextField
      label={label}
      value={valeur}
      mono
      required
      autoComplete="off"
      help={help}
      error={refus ? t('parcoursOps.kcv.tropLong') : erreur}
      onChange={(v) => {
        const l = lireCodeControle(v)
        setRefus(l.erreur === 'tropLong')
        onChange(l.erreur === 'tropLong' ? '' : l.valeur)
      }}
    />
  )
}


/** Livrables attendus que la personne peut déposer (D-93 §8.4) ; plusieurs : le dépôt choisit son code. */
const deposables = (op: OperationParcours) => (op.livrablesAttendus ?? []).filter((l) => l.peutDeposer)
function ChoixLivrable({ op, valeur, onChange }: { op: OperationParcours; valeur: string; onChange: (v: string) => void }) {
  const { t } = useTranslation()
  const l = deposables(op)
  if (l.length < 2) return null
  return <Select name="livrable" label={t('parcoursOps.livrable.choisir')} required value={valeur} onChange={onChange}
    options={l.map((x) => ({ value: x.code, label: `${x.code} · ${x.libelle}${x.obligatoire ? '' : ` (${t('parcoursOps.livrable.facultatif')})`}` }))} />
}

/** D-109 : livrable visé par le formulaire (code choisi, sinon le premier livrable déposable). */
const cible = (op: OperationParcours, code: string) => deposables(op).find((x) => x.code === code) ?? deposables(op)[0]
/** D-109 : services encore attendus d'un livrable au grain du service. */
export const servicesAttendus = (l?: LivrableAttendu) =>
  l?.grain === 'SERVICE' ? (l.services ?? []).filter((s) => s.etat === 'ATTENDUE' || s.etat === 'REFUSEE').map((s) => s.serviceCode) : []
/** D-109 : choix du service pour un livrable au grain du service ; consigne de la pièce attendue. */
function ChoixService({ livrable, valeur, onChange, tente }: { livrable?: LivrableAttendu; valeur: string; onChange: (v: string) => void; tente: boolean }) {
  const { t } = useTranslation()
  if (livrable?.grain !== 'SERVICE') return null
  const l = servicesAttendus(livrable)
  return <Select name="service" label={t('preuves.service')} required value={valeur} onChange={onChange} help={t('preuves.service.aide')}
    error={tente && !valeur ? t('preuves.service.requis') : undefined}
    options={[{ value: '', label: t('preuves.service.choisir') }, ...l.map((s) => ({ value: s, label: s }))]} />
}
function Consigne({ livrable }: { livrable?: LivrableAttendu }) {
  const { t } = useTranslation()
  return livrable?.consigne ? <Note>{t('preuves.consigne', { consigne: livrable.consigne })}</Note> : null
}

/** Date saisie (JJ/MM/AAAA, format du champ) envoyée au service en ISO (AAAA-MM-JJ) ; une date déjà ISO passe telle quelle. */
export const dateIso = (saisie: string) => {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(saisie.trim())
  return m ? `${m[3]}-${m[2]}-${m[1]}` : saisie.trim()
}
export function FormulairePreuve({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const executeur = useExecuteurActions()
  const { preuve } = useActionsOperation()
  // Cérémonie de clés : vue de l'opération (D-93), jamais son rang.
  const ceremonie = op.vue === 'CEREMONIE'
  const [livrable, setLivrable] = useState(deposables(op)[0]?.code ?? '')
  const [service, setService] = useState('')
  const vise = cible(op, livrable)
  const [fichier, setFichier] = useState<File | null>(null)
  const [valeur, setValeur] = useState('')
  const [kcv, setKcv] = useState('')
  const [tenueLe, setTenueLe] = useState('')
  const [porteurs, setPorteurs] = useState(['', '', ''])
  const [kcvs, setKcvs] = useState(['', '', ''])
  const [tente, setTente] = useState(false)
  const kcvErr = (v: string) => (tente && lireCodeControle(v).erreur ? t(`parcoursOps.kcv.${lireCodeControle(v).erreur}`) : undefined)
  const complet =
    fichier !== null &&
    (vise?.grain !== 'SERVICE' || Boolean(service)) &&
    (!op.valeurReference || !vide(valeur)) &&
    (!op.cle || ceremonie || !lireCodeControle(kcv).erreur) &&
    (!ceremonie || (!vide(tenueLe) && porteurs.every((p) => !vide(p)) && kcvs.every((k) => !lireCodeControle(k).erreur)))
  const deposer = () => {
    setTente(true)
    if (!complet || !fichier) return
    const saisie = {
      fichier,
      valeurReference: op.valeurReference ? valeur.trim() : undefined,
      codeControle: op.cle && !ceremonie ? kcv : undefined,
      ceremonie: ceremonie ? { tenueLe: dateIso(tenueLe), porteurs: porteurs.map((p) => p.trim()), codesControle: kcvs } : undefined,
      livrable: deposables(op).length > 1 || vise?.grain === 'SERVICE' ? vise?.code : undefined,
      service: vise?.grain === 'SERVICE' ? service : undefined,
    }
    executeur.lancer(preuve, { ...op, saisie }, `${op.rang} · ${libelleOperation(t, op)}`, SUJET)
  }
  return (
    <div className="gm-col">
      <KeyValueList items={[{ key: t('parcoursOps.preuveAttendue'), value: preuveAttendue(op, t) }]} />
      <ChoixLivrable op={op} valeur={livrable} onChange={(v) => { setLivrable(v); setService('') }} />
      <Consigne livrable={vise} />
      <ChoixService livrable={vise} valeur={service} onChange={setService} tente={tente} />
      {op.cle || ceremonie ? <InlineAlert level="warn" title={t('parcoursOps.secret.titre')}>{t('parcours.alerte.cle')}</InlineAlert> : null}
      {op.secret ? <InlineAlert level="warn" title={t('parcoursOps.secret.titre')}>{t('parcoursOps.secret.texte')}</InlineAlert> : null}
      <ChampFichier label={t('parcoursOps.preuve.fichier')} required fichier={fichier} onFichier={setFichier} error={tente && !fichier ? t('parcoursOps.fichier.requis') : undefined} />
      {op.valeurReference ? (
        <TextField
          label={t(`parcours.valeur.${op.rang}`, { defaultValue: t('parcoursOps.valeur.libelle') })}
          value={valeur}
          onChange={setValeur}
          required
          mono
          autoComplete="off"
          help={t('parcoursOps.valeur.aide')}
          error={tente && vide(valeur) ? t('parcoursOps.valeur.requise') : undefined}
        />
      ) : null}
      {op.cle && !ceremonie ? (
        <ChampCodeControle label={t('parcoursOps.preuve.codeControle')} valeur={kcv} onChange={setKcv} erreur={kcvErr(kcv)} help={t('parcoursOps.kcv.aide')} />
      ) : null}
      {ceremonie ? (
        <fieldset className="gm-col" style={{ border: 0, padding: 0, margin: 0 }}>
          <legend className="gm-type-h3" style={{ marginBottom: 'var(--gm-space-2)' }}>{t('parcoursOps.ceremonie.titre')}</legend>
          <DateField label={t('parcoursOps.ceremonie.tenueLe')} value={tenueLe} onChange={setTenueLe} required error={tente && vide(tenueLe) ? t('parcoursOps.ceremonie.dateRequise') : undefined} />
          {[0, 1, 2].map((i) => (
            <div key={i} className="gm-row" style={{ alignItems: 'flex-start' }}>
              <div style={{ flex: '1 1 200px' }}>
                <TextField
                  label={t('parcoursOps.ceremonie.porteur', { n: i + 1 })}
                  value={porteurs[i]}
                  required
                  autoComplete="off"
                  onChange={(v) => setPorteurs((l) => l.map((x, j) => (j === i ? v : x)))}
                  error={tente && vide(porteurs[i]) ? t('parcoursOps.ceremonie.porteurRequis') : undefined}
                />
              </div>
              <div style={{ flex: '1 1 160px' }}>
                <ChampCodeControle label={t('parcoursOps.ceremonie.kcv', { n: i + 1 })} valeur={kcvs[i]} onChange={(v) => setKcvs((l) => l.map((x, j) => (j === i ? v : x)))} erreur={kcvErr(kcvs[i])} />
              </div>
            </div>
          ))}
        </fieldset>
      ) : null}
      <div className="gm-row">
        <Button variant="primary" icon="upload" onClick={deposer}>{t(preuve.label)}</Button>
      </div>
    </div>
  )
}

export function FormulaireLivrable({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const executeur = useExecuteurActions()
  const { livrable } = useActionsOperation()
  const [fichier, setFichier] = useState<File | null>(null)
  const [commentaire, setCommentaire] = useState('')
  const [livrableVise, setLivrableVise] = useState(deposables(op)[0]?.code ?? '')
  const [service, setService] = useState('')
  const vise = cible(op, livrableVise)
  const [tente, setTente] = useState(false)
  return (
    <div className="gm-col">
      <KeyValueList items={[{ key: t('parcoursOps.preuveAttendue'), value: preuveAttendue(op, t) }]} />
      <ChoixLivrable op={op} valeur={livrableVise} onChange={(v) => { setLivrableVise(v); setService('') }} />
      <Consigne livrable={vise} />
      <ChoixService livrable={vise} valeur={service} onChange={setService} tente={tente} />
      {op.grain === 'PHASE' ? <InlineAlert level="info" title={t('parcours.grain.PHASE')}>{t('parcoursOps.livrable.unique')}</InlineAlert> : null}
      <ChampFichier label={t('parcoursOps.livrable.fichier')} required fichier={fichier} onFichier={setFichier} error={tente && !fichier ? t('parcoursOps.fichier.requis') : undefined} />
      <TextArea label={t('parcoursOps.livrable.commentaire')} value={commentaire} onChange={setCommentaire} rows={3} />
      <div className="gm-row">
        <Button
          variant="primary"
          icon="upload"
          onClick={() => {
            setTente(true)
            if (fichier && (vise?.grain !== 'SERVICE' || service)) {
              executeur.lancer(livrable, { ...op, saisie: { fichier, commentaire, livrable: deposables(op).length > 1 || vise?.grain === 'SERVICE' ? vise?.code : undefined, service: vise?.grain === 'SERVICE' ? service : undefined } }, `${op.rang} · ${libelleOperation(t, op)}`, SUJET)
            }
          }}
        >
          {t(livrable.label)}
        </Button>
      </div>
    </div>
  )
}

export function BlocValidation({ op, renvoyer = false }: { op: OperationParcours; renvoyer?: boolean }) {
  const { t } = useTranslation()
  const executeur = useExecuteurActions()
  const { validation, renvoi } = useActionsOperation()
  return (
    <div className="gm-col">
      <InlineAlert level="info" title={t('parcoursOps.valider.titre')}>{t('parcoursOps.valider.regle')}</InlineAlert>
      <div className="gm-row">
        <Button variant="primary" icon="pen-line" onClick={() => executeur.lancer(validation, op, `${op.rang} · ${libelleOperation(t, op)}`, SUJET)}>{t(validation.label)}</Button>
        {/* D-93 : renvoi motivé, l'opération revient à faire et la tâche de R est rouverte. */}
        {renvoyer ? <Button variant="secondary" icon="rotate-ccw" onClick={() => executeur.lancer(renvoi, op, `${op.rang} · ${libelleOperation(t, op)}`, SUJET)}>{t(renvoi.label)}</Button> : null}
      </div>
    </div>
  )
}

/** Renvois de l'opération par son validateur (D-93) : motif, auteur, date. */
export function Renvois({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  if (!op.renvois?.length) return null
  return (
    <InlineAlert level="warn" title={t('parcoursOps.renvoi.titre', { count: op.renvois.length })}>
      <ul className="gm-col" style={{ gap: 'var(--gm-space-1)', margin: 0, paddingLeft: 'var(--gm-space-4)' }}>
        {op.renvois.map((r, i) => <li key={i}>{t('parcoursOps.renvoi.ligne', { motif: r.motif, par: r.parNom, le: date(r.le) })}</li>)}
      </ul>
    </InlineAlert>
  )
}

/** Livrables attendus de l'opération (D-93 §8.4) : déposant, caractère obligatoire, dépôt. */
export function LivrablesAttendus({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const l = op.livrablesAttendus ?? []
  // D-109 : chaque opération affiche ses preuves attendues et leur état, même un livrable unique.
  if (!l.length) return null
  const colonnes: Column<LivrableAttendu>[] = [
    { key: 'code', header: t('parcoursOps.livrable.code'), type: 'mono', width: 80 },
    { key: 'libelle', header: t('parcoursOps.livrable.libelle') },
    { key: 'etat', header: t('parcoursOps.col.etat'), width: 150, render: (x) => <BadgeLivrable etat={x.etat} /> },
    { key: 'deposant', header: t('parcoursOps.livrable.deposant'), render: (x) => x.libelleDeposant ?? x.deposant ?? '' },
    { key: 'depot', header: t('parcoursOps.livrable.depot'), render: (x) => (x.depot ? (x.depot.sansObjet ? t('preuves.etat.SANS_OBJET') : `${x.depot.nom ?? ''} · ${date(x.depot.deposeLe)}`) : <StatusBadge status={x.obligatoire ? 'warn' : 'neutral'} label={t(x.obligatoire ? 'parcoursOps.livrable.manquant' : 'parcoursOps.livrable.facultatif')} />) },
  ]
  return (
    <section className="gm-col" aria-labelledby={`liv-${op.id}`} style={{ gap: 'var(--gm-space-2)' }}>
      <h3 id={`liv-${op.id}`} className="gm-type-h3" style={{ margin: 0 }}>{t('parcoursOps.livrable.attendus')}</h3>
      <DataTable columns={colonnes} rows={l} rowKey={(x) => x.code} density="compact" state="ok" />
    </section>
  )
}

/** Signatures attendues de l'opération (D-91) et bouton « Signer » pour la signature suivante que la personne peut donner. */
export function Signatures({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const executeur = useExecuteurActions()
  const { signature } = useActionsOperation()
  const f = useFonctionsCatalogue()
  const l = [...(op.signatures ?? [])].sort((a, b) => a.ordre - b.ordre)
  if (!l.length) return null
  const peutSigner = op.actionsPermises.includes('SIGNER')
  const colonnes: Column<SignatureOperation>[] = [
    { key: 'ordre', header: t('parcoursOps.signature.ordre'), type: 'number', width: 72 },
    { key: 'libelle', header: t('parcoursOps.signature.libelle'), render: (x) => <span className="gm-col" style={{ gap: 0 }}><span>{x.libelle}</span><span className="gm-mono gm-type-small">{x.code}</span></span> },
    { key: 'signataire', header: t('parcoursOps.signature.signataire'), render: (x) => x.libelleSignataire ?? f.libelle(x.signataire) },
    {
      key: 'etat', header: t('parcoursOps.col.etat'), render: (x) => (x.etat === 'SIGNEE'
        ? <span className="gm-col" style={{ gap: 0, alignItems: 'flex-start' }}><StatusBadge status="ok" label={t('parcoursOps.signature.signee')} /><span className="gm-field-help">{t('parcoursOps.signature.par', { nom: x.signeParNom ?? '', le: x.signeLe ? date(x.signeLe) : '' })}</span></span>
        : <span className="gm-col" style={{ gap: 0, alignItems: 'flex-start' }}><StatusBadge status={x.suivante ? 'warn' : 'neutral'} label={t(x.suivante ? 'parcoursOps.signature.attendue' : 'parcoursOps.signature.aVenir')} />{x.attendu ? <span className="gm-field-help">{x.attendu.nom}</span> : null}</span>),
    },
    {
      key: 'action', header: '', type: 'actions', render: (x) => (peutSigner && x.suivante && x.etat === 'ATTENDUE'
        ? <Button size="sm" variant="primary" icon="pen-line" aria-label={t('parcoursOps.signature.signerQui', { libelle: x.libelle })}
            onClick={() => executeur.lancer(signature, { ...op, saisie: x }, `${op.rang} · ${x.libelle}`, SUJET)}>{t('parcoursOps.action.signer')}</Button>
        : null),
    },
  ]
  return (
    <section className="gm-col" aria-labelledby={`sig-${op.id}`} style={{ gap: 'var(--gm-space-2)' }}>
      <h3 id={`sig-${op.id}`} className="gm-type-h3" style={{ margin: 0 }}>{t('parcoursOps.signature.titre')}</h3>
      {op.etat === 'EN_SIGNATURE' ? <InlineAlert level="info">{t('parcoursOps.signature.regle')}</InlineAlert> : null}
      <DataTable columns={colonnes} rows={l} rowKey={(x) => x.code} density="compact" state="ok" />
    </section>
  )
}

const QUALITES: Qualite[] = ['R', 'C', 'V', 'I']

export function FormulaireAttribution({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const executeur = useExecuteurActions()
  const { attribution } = useActionsOperation()
  // Fonctions internes du catalogue (D-91) : plus de liste figée.
  const catalogue = useFonctionsCatalogue()
  const internes = catalogue.internes.map((f) => f.code).filter((c) => c !== 'CHEFS_DEPARTEMENT')
  const [qualite, setQualite] = useState<Qualite>('R')
  const [choixFonction, setFonction] = useState<Fonction>('')
  const fonction = choixFonction || (op.raci.R?.fonction && internes.includes(op.raci.R.fonction) ? op.raci.R.fonction : internes[0] ?? '')
  const [personneId, setPersonneId] = useState('')
  const personnes = usePersonnes(fonction, Boolean(fonction))
  const choix = (personnes.data ?? []).find((p) => p.id === personneId)
  const [tente, setTente] = useState(false)
  return (
    <div className="gm-col">
      <Note>{t('parcoursOps.attribution.regle')}</Note>
      <div className="gm-row" style={{ alignItems: 'flex-start' }}>
        <div style={{ flex: '1 1 140px' }}>
          <Select label={t('parcoursOps.attribution.qualite')} value={qualite} onChange={(v) => setQualite(v as Qualite)} options={QUALITES.map((q) => ({ value: q, label: t(`parcours.qualite.${q}`) }))} />
        </div>
        <div style={{ flex: '1 1 200px' }}>
          <Select
            label={t('parcoursOps.attribution.fonction')}
            value={fonction}
            onChange={(v) => {
              setFonction(v as Fonction)
              setPersonneId('')
            }}
            options={internes.map((f) => ({ value: f, label: catalogue.libelle(f) }))}
          />
        </div>
      </div>
      <Select
        label={t('parcoursOps.attribution.personne')}
        value={personneId}
        onChange={setPersonneId}
        required
        disabled={personnes.isPending}
        options={[{ value: '', label: t('parcoursOps.attribution.choisir') }, ...(personnes.data ?? []).map((p) => ({ value: p.id, label: p.nom }))]}
        error={tente && !choix ? t('parcoursOps.attribution.personneRequise') : personnes.isError ? personnes.error?.message : undefined}
      />
      <div className="gm-row">
        <Button
          variant="secondary"
          icon="users"
          onClick={() => {
            setTente(true)
            if (choix) executeur.lancer(attribution, { ...op, saisie: { qualite, personneId: choix.id, personneNom: choix.nom } }, `${op.rang} · ${libelleOperation(t, op)}`, SUJET)
          }}
        >
          {t(attribution.label)}
        </Button>
      </div>
    </div>
  )
}

/** Actions permises sur l'opération, croisées avec les permissions du jeton. */
export function usePermis(op: OperationParcours) {
  const permissions = usePermissions()
  const a = (action: string, permission: string) => op.actionsPermises.includes(action) && aPermission(permissions, permission)
  return {
    // D-98 : comme le livrable, la preuve est ouverte à la conduite des étapes (chargé de la DDRM, responsable des preuves du
    // cadrage) ou au dépôt de preuve ; le service décide du droit de dépôt (RACI).
    preuve: a('DEPOSER_PREUVE', PERM.preuveDeposer) || a('DEPOSER_PREUVE', PERM.etapeConduire),
    // Livrable d'étape (compte rendu de lancement, d'atelier…) : conduite des étapes ou dépôt de preuve.
    livrable: a('DEPOSER_LIVRABLE', PERM.etapeConduire) || a('DEPOSER_LIVRABLE', PERM.preuveDeposer),
    valider: a('VALIDER', PERM.operationValider),
    // D-93 : renvoi motivé par le validateur admis.
    renvoyer: a('RENVOYER', PERM.operationValider),
    attribuer: a('ATTRIBUER', PERM.operationAttribuer),
    constater: a('CONSTATER_COUVERTURE', PERM.dossierVerifier),
  }
}
