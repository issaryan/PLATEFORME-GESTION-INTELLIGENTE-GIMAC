import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, DateField, InlineAlert, KeyValueList, Select, TextArea, TextField, Note } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { usePersonnes } from '../../../../hooks/parcours'
import type { Fonction, OperationParcours, Qualite } from '../../../../modele/parcours'
import { useExecuteurActions } from '../../../../ressources/actions'
import { PERM } from '../../../../permissions'
import { ChampFichier } from '../../../commun/ChampFichier'
import { useActionsOperation } from './actions'
import { lireCodeControle } from './regles'

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


/** Date saisie (JJ/MM/AAAA, format du champ) envoyée au service en ISO (AAAA-MM-JJ) ; une date déjà ISO passe telle quelle. */
export const dateIso = (saisie: string) => {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(saisie.trim())
  return m ? `${m[3]}-${m[2]}-${m[1]}` : saisie.trim()
}
export function FormulairePreuve({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const executeur = useExecuteurActions()
  const { preuve } = useActionsOperation()
  const ceremonie = op.rang === 'C1'
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
    }
    executeur.lancer(preuve, { ...op, saisie }, `${op.rang} · ${t(`parcours.op.${op.rang}`)}`, SUJET)
  }
  return (
    <div className="gm-col">
      <KeyValueList items={[{ key: t('parcoursOps.preuveAttendue'), value: t(`parcours.preuveAttendue.${op.rang}`) }]} />
      {op.cle || ceremonie ? <InlineAlert level="warn" title={t('parcoursOps.secret.titre')}>{t('parcours.alerte.cle')}</InlineAlert> : null}
      {op.secret ? <InlineAlert level="warn" title={t('parcoursOps.secret.titre')}>{t('parcoursOps.secret.texte')}</InlineAlert> : null}
      <ChampFichier label={t('parcoursOps.preuve.fichier')} required fichier={fichier} onFichier={setFichier} error={tente && !fichier ? t('parcoursOps.fichier.requis') : undefined} />
      {op.valeurReference ? (
        <TextField
          label={t(`parcours.valeur.${op.rang}`)}
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
  const [tente, setTente] = useState(false)
  return (
    <div className="gm-col">
      <KeyValueList items={[{ key: t('parcoursOps.preuveAttendue'), value: t(`parcours.preuveAttendue.${op.rang}`) }]} />
      {op.grain === 'PHASE' ? <InlineAlert level="info" title={t('parcours.grain.PHASE')}>{t('parcoursOps.livrable.unique')}</InlineAlert> : null}
      <ChampFichier label={t('parcoursOps.livrable.fichier')} required fichier={fichier} onFichier={setFichier} error={tente && !fichier ? t('parcoursOps.fichier.requis') : undefined} />
      <TextArea label={t('parcoursOps.livrable.commentaire')} value={commentaire} onChange={setCommentaire} rows={3} />
      <div className="gm-row">
        <Button
          variant="primary"
          icon="upload"
          onClick={() => {
            setTente(true)
            if (fichier) executeur.lancer(livrable, { ...op, saisie: { fichier, commentaire } }, `${op.rang} · ${t(`parcours.op.${op.rang}`)}`, SUJET)
          }}
        >
          {t(livrable.label)}
        </Button>
      </div>
    </div>
  )
}

export function BlocValidation({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const executeur = useExecuteurActions()
  const { validation } = useActionsOperation()
  return (
    <div className="gm-col">
      <InlineAlert level="info" title={t('parcoursOps.valider.titre')}>{t('parcoursOps.valider.regle')}</InlineAlert>
      <div className="gm-row">
        <Button variant="primary" icon="pen-line" onClick={() => executeur.lancer(validation, op, `${op.rang} · ${t(`parcours.op.${op.rang}`)}`, SUJET)}>{t(validation.label)}</Button>
      </div>
    </div>
  )
}

const QUALITES: Qualite[] = ['R', 'C', 'V', 'I']
/** Fonctions qui interviennent sur les opérations d'exécution (liste fermée de moins de dix choix, DS §7.2). */
const FONCTIONS_INTERNES: Fonction[] = ['CONDUCTEUR', 'SNA', 'PRODUCTION_MONETIQUE', 'POLE_RESEAU', 'POLE_SYSTEME', 'RSSI', 'DDRM', 'CHEF_DES', 'EXPLOITATION']

export function FormulaireAttribution({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const executeur = useExecuteurActions()
  const { attribution } = useActionsOperation()
  const [qualite, setQualite] = useState<Qualite>('R')
  const [fonction, setFonction] = useState<Fonction>(op.raci.R?.fonction && FONCTIONS_INTERNES.includes(op.raci.R.fonction) ? op.raci.R.fonction : 'SNA')
  const [personneId, setPersonneId] = useState('')
  const personnes = usePersonnes(fonction)
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
            options={FONCTIONS_INTERNES.map((f) => ({ value: f, label: t(`parcours.fonction.${f}`) }))}
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
            if (choix) executeur.lancer(attribution, { ...op, saisie: { qualite, personneId: choix.id, personneNom: choix.nom } }, `${op.rang} · ${t(`parcours.op.${op.rang}`)}`, SUJET)
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
    preuve: a('DEPOSER_PREUVE', PERM.preuveDeposer),
    // Livrable d'étape (compte rendu de lancement, d'atelier…) : conduite des étapes ou dépôt de preuve.
    livrable: a('DEPOSER_LIVRABLE', PERM.etapeConduire) || a('DEPOSER_LIVRABLE', PERM.preuveDeposer),
    valider: a('VALIDER', PERM.operationValider),
    attribuer: a('ATTRIBUER', PERM.operationAttribuer),
    constater: a('CONSTATER_COUVERTURE', PERM.dossierVerifier),
  }
}
