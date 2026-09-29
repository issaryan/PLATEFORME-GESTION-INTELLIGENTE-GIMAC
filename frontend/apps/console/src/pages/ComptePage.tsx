import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router'
import {
  Button, Checkbox, CheckboxGroup, ConfirmDialog, DataTable, Dialog, InlineAlert, KeyValueList, OneTimeSecret, Select, StatusBadge, Tabs, Tag, TextArea, TextField,
  type Column,
} from '@gimac/design-system'
import { aPermission, usePermissions, useSession } from '@gimac/socle'
import { ACTIONS_REQUISES, LANGUES_COMPTE, dateHeure, useLibelleProfil, useMessageErreur, type ActionRequise, type Compte, type MessageErreur, type SessionCompte } from '@gimac/compte'
import { useCompte, useEffet, useJournal, useProfils, useSessionsCompte } from '../hooks'
import type { EntreeJournal, FicheCompte } from '../modele/comptes'
import { Annonce } from '../composants/Annonce'
import { useActes } from '../composants/actes'
import { Donnees } from '../composants/communs'
import { etatListe } from '../composants/formats'

// Fiche d'un compte (D-62) : identité, profils, sécurité (actions requises, second facteur), sessions,
// journal. Chaque acte n'apparaît qu'avec sa permission ; profils et suppression exigent la
// réauthentification FIDO2 (ADMIN_SENSITIVE) et un motif ; les autres actes un motif. Un administrateur
// n'agit pas sur son propre compte (séparation des pouvoirs, refaite par MS-IAM).

const CHAMP_FICHE: Record<string, string> = { IAM_PHONE_INVALID: 'telephone', IAM_ACCOUNT_CONFLICT: 'email', IAM_LANGUAGE_INVALID: 'langue' }

function DialogueFiche({ compte, onFermer, onEnregistre }: { compte: Compte; onFermer: () => void; onEnregistre: () => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const modifier = useEffet((s, f: FicheCompte) => s.comptes.modifier(compte.id, f))
  const [v, setV] = useState({
    prenom: compte.prenom ?? '', nom: compte.nom ?? '', email: compte.email ?? '', telephone: compte.telephone ?? '', fonction: compte.fonction ?? '',
    langue: compte.langue ?? 'fr', institutionId: compte.institutionId ?? '',
  })
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  const champ = (k: keyof typeof v) => (x: string) => setV((o) => ({ ...o, [k]: x }))
  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    setErreurs({})
    setErreur(null)
    try {
      await modifier.mutateAsync({
        prenom: v.prenom.trim(), nom: v.nom.trim(), email: v.email.trim(), telephone: v.telephone.trim() || null, fonction: v.fonction.trim() || null,
        langue: v.langue, institutionId: v.institutionId.trim() || null,
      })
      onEnregistre()
    } catch (x) {
      const m = message(x)
      const c = m.code ? CHAMP_FICHE[m.code] : undefined
      if (c) setErreurs({ [c]: m.texte })
      else setErreur(m)
    }
  }
  return (
    <Dialog open size="lg" title={t('console.compte.modifierFiche')} onClose={onFermer} id="gm-dlg-fiche">
      <form className="gm-col" onSubmit={(e) => void soumettre(e)} aria-label={t('console.compte.modifierFiche')}>
        {erreur ? <InlineAlert level="crit" title={erreur.texte} correlationId={erreur.correlationId} /> : null}
        <div className="gm-form-grid">
          <div className="gm-form-field"><TextField name="prenom" label={t('compte.prenom')} value={v.prenom} onChange={champ('prenom')} /></div>
          <div className="gm-form-field"><TextField name="nom" label={t('compte.nom')} value={v.nom} onChange={champ('nom')} /></div>
          <div className="gm-form-field"><TextField name="email" type="email" label={t('compte.email')} value={v.email} onChange={champ('email')} error={erreurs.email} /></div>
          <div className="gm-form-field"><TextField name="telephone" type="tel" label={t('compte.telephone')} value={v.telephone} onChange={champ('telephone')} error={erreurs.telephone} /></div>
          <div className="gm-form-field"><TextField name="fonction" label={t('compte.fonction')} value={v.fonction} onChange={champ('fonction')} /></div>
          <div className="gm-form-field">
            <Select name="langue" label={t('compte.langue')} value={v.langue} onChange={champ('langue')} error={erreurs.langue} options={LANGUES_COMPTE.map((l) => ({ value: l, label: t(`compte.langue.${l}`) }))} />
          </div>
          <div className="gm-form-field"><TextField name="institutionId" label={t('console.creation.institution')} mono value={v.institutionId} onChange={champ('institutionId')} /></div>
        </div>
        <div className="gm-row" style={{ justifyContent: 'flex-end' }}>
          <Button variant="ghost" onClick={onFermer}>{t('commun.annuler')}</Button>
          <Button variant="primary" type="submit" loading={modifier.isPending}>{t('console.compte.enregistrerFiche')}</Button>
        </div>
      </form>
    </Dialog>
  )
}

function DialogueProfils({ compte, privilegie, onFermer, onContinuer }: { compte: Compte; privilegie: boolean; onFermer: () => void; onContinuer: (profils: string[]) => void }) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const profils = useProfils()
  const [choix, setChoix] = useState<string[]>(compte.profils)
  return (
    <Dialog open size="lg" title={t('console.compte.modifierProfils')} description={t('console.compte.profils.texte')} onClose={onFermer} id="gm-dlg-profils"
      footer={[
        <Button key="a" variant="secondary" onClick={onFermer}>{t('commun.annuler')}</Button>,
        <Button key="b" variant="primary" icon="pen-line" onClick={() => {
          // Sans droit d'octroi privilégié, les profils privilégiés détenus sont conservés tels quels.
          const privilegies = (profils.data ?? []).filter((p) => p.privilegie).map((p) => p.code)
          const conserves = privilegie ? [] : compte.profils.filter((p) => privilegies.includes(p))
          onContinuer([...new Set([...choix.filter((p) => privilegie || !privilegies.includes(p)), ...conserves])].sort())
        }}>{t('console.compte.profils.continuer')}</Button>,
      ]}>
      <Donnees requete={profils}>
        {(l) => (
          <CheckboxGroup label={t('compte.profils')} value={choix} onChange={setChoix}
            options={l.filter((p) => privilegie || !p.privilegie).map((p) => ({ value: p.code, label: libelleProfil(p.code) }))} />
        )}
      </Donnees>
    </Dialog>
  )
}

function DialogueActions({ onFermer, onValider }: { onFermer: () => void; onValider: (a: ActionRequise[], parCourriel: boolean, motif: string) => void }) {
  const { t } = useTranslation()
  const [actions, setActions] = useState<string[]>([])
  const [parCourriel, setParCourriel] = useState(true)
  const [motif, setMotif] = useState('')
  const ok = actions.length > 0 && motif.trim().length >= 10
  return (
    <Dialog open title={t('console.compte.exigerActions')} description={t('console.compte.exigerActions.texte')} onClose={onFermer} id="gm-dlg-actions"
      footer={[
        <Button key="a" variant="secondary" onClick={onFermer}>{t('commun.annuler')}</Button>,
        <Button key="b" variant="primary" disabled={!ok} disabledReason={t('console.compte.exigerActions.incomplet')}
          onClick={() => onValider(actions as ActionRequise[], parCourriel, motif.trim())}>{t('console.compte.exigerActions')}</Button>,
      ]}>
      <div className="gm-col">
        <CheckboxGroup label={t('console.compte.actionsRequises')} value={actions} onChange={setActions} options={ACTIONS_REQUISES.map((a) => ({ value: a, label: t(`console.action.${a}`) }))} />
        <Checkbox name="parCourriel" label={t('console.compte.parCourriel')} checked={parCourriel} onChange={setParCourriel} />
        <TextArea name="motifActions" label={t('console.motif')} required value={motif} onChange={setMotif} help={t('console.motif.aide')} />
      </div>
    </Dialog>
  )
}

function MotDePasse({ valeur, onFermer }: { valeur: string; onFermer: () => void }) {
  const { t } = useTranslation()
  return (
    <Dialog open title={t('console.creation.mdp.titre')} onClose={onFermer} id="gm-dlg-mdp" footer={<Button variant="primary" onClick={onFermer}>{t('commun.fermer')}</Button>}>
      <OneTimeSecret label={t('console.creation.mdp.titre')} value={valeur} warning={t('console.creation.mdp.avertissement')} />
    </Dialog>
  )
}

function Sessions({ id }: { id: string }) {
  const { t, i18n } = useTranslation()
  const sessions = useSessionsCompte(id)
  const colonnes: Column<SessionCompte>[] = [
    { key: 'adresseIp', header: t('compte.sessions.adresse'), render: (s) => <span className="gm-mono">{s.adresseIp ?? '—'}</span> },
    { key: 'debut', header: t('compte.sessions.debut'), render: (s) => dateHeure(s.debut, i18n.language) },
    { key: 'dernierAcces', header: t('compte.sessions.dernierAcces'), render: (s) => dateHeure(s.dernierAcces, i18n.language) },
    { key: 'applications', header: t('compte.sessions.applications'), render: (s) => s.applications.join(', ') },
  ]
  return (
    <Donnees requete={sessions} estVide={(l) => l.length === 0} vide={t('compte.sessions.aucune')}>
      {(l) => <DataTable columns={colonnes} rows={l} rowKey={(s) => s.id} density="compact" />}
    </Donnees>
  )
}

function Journal({ id }: { id: string }) {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const journal = useJournal({ page, taille: 10, compte: id })
  const lignes = journal.data?.contenu ?? []
  const colonnes: Column<EntreeJournal>[] = [
    { key: 'horodatage', header: t('console.journal.horodatage'), render: (j) => dateHeure(j.horodatage, i18n.language) },
    { key: 'acteurNom', header: t('console.journal.acteur'), render: (j) => j.acteurNom ?? j.acteurId },
    { key: 'action', header: t('console.journal.action'), render: (j) => t(`console.journal.action.${j.action}`, { defaultValue: j.action }) },
    { key: 'motif', header: t('console.journal.motif'), render: (j) => j.motif ?? '—' },
  ]
  return (
    <DataTable columns={colonnes} rows={lignes} rowKey={(j) => j.id} total={journal.data?.total} page={page} pageSize={10} onPage={setPage} density="compact"
      emptyText={t('console.journal.vide')} {...etatListe(journal, lignes)} />
  )
}

function Fiche({ compte }: { compte: Compte }) {
  const { t, i18n } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const oui = (b: boolean) => t(b ? 'console.oui' : 'console.non')
  return (
    <KeyValueList
      items={[
        { key: t('compte.identifiant'), value: compte.identifiant, mono: true },
        { key: t('compte.email'), value: `${compte.email ?? '—'} (${t(compte.emailVerifie ? 'compte.email.verifie' : 'compte.email.nonVerifie')})` },
        { key: t('compte.telephone'), value: compte.telephone ?? '—' },
        { key: t('compte.fonction'), value: compte.fonction ?? '—' },
        { key: t('compte.langue'), value: compte.langue ? t(`compte.langue.${compte.langue}`, { defaultValue: compte.langue }) : '—' },
        { key: t('console.creation.institution'), value: compte.institutionId ?? '—', mono: true },
        { key: t('console.compte.creeLe'), value: dateHeure(compte.creeLe, i18n.language) },
        { key: t('console.compte.ficheComplete'), value: oui(compte.profilComplet) },
        { key: t('compte.profils'), value: <span className="gm-row">{compte.profils.map((p) => <Tag key={p}>{libelleProfil(p)}</Tag>)}</span> },
      ]}
    />
  )
}

export default function ComptePage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const navigate = useNavigate()
  const session = useSession()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil()
  const message = useMessageErreur()
  const compte = useCompte(id)
  const actes = useActes()
  const s = {
    profils: useEffet((x, v: { profils: string[]; motif: string; assertion: string }) => x.comptes.definirProfils(id, v.profils, v.motif, v.assertion)),
    activer: useEffet((x, motif: string) => x.comptes.activer(id, motif)),
    desactiver: useEffet((x, motif: string) => x.comptes.desactiver(id, motif)),
    supprimer: useEffet((x, v: { motif: string; assertion: string }) => x.comptes.supprimer(id, v.motif, v.assertion)),
    actions: useEffet((x, v: { actions: ActionRequise[]; parCourriel: boolean; motif: string }) => x.comptes.exigerActions(id, v.actions, v.parCourriel, v.motif)),
    secondFacteur: useEffet((x, motif: string) => x.comptes.reinitialiserSecondFacteur(id, motif)),
    motDePasse: useEffet((x, motif: string) => x.comptes.motDePasseProvisoire(id, motif)),
    sessions: useEffet((x, motif: string) => x.comptes.fermerSessions(id, motif)),
  }
  const [onglet, setOnglet] = useState('fiche')
  const [dialogue, setDialogue] = useState<'fiche' | 'profils' | 'actions' | 'suppression' | null>(null)
  const [mdp, setMdp] = useState<string | null>(null)
  const peut = (p: string) => aPermission(permissions, p)
  const privilegie = peut('IAM_ROLE_PRIVILEGED_GRANT')

  return (
    <Donnees requete={compte}>
      {(c) => {
        const soiMeme = session?.sujet === c.id
        const objet = [
          { key: t('compte.identifiant'), value: c.identifiant, mono: true },
          { key: t('console.comptes.nom'), value: c.nomAffiche },
        ]
        const onglets = [
          { key: 'fiche', label: t('console.compte.onglet.fiche') },
          { key: 'securite', label: t('console.compte.onglet.securite') },
          { key: 'sessions', label: t('console.compte.onglet.sessions') },
          ...(peut('IAM_JOURNAL_READ') ? [{ key: 'journal', label: t('console.compte.onglet.journal') }] : []),
        ]
        return (
          <div className="gm-resource">
            <header className="gm-objhead">
              <div className="gm-objhead-main">
                <div className="gm-row">
                  <span className="gm-type-small">{t('console.compte.type')}</span>
                  <span className="gm-mono">{c.identifiant}</span>
                </div>
                <h1 className="gm-heading-display">{c.nomAffiche}</h1>
                <div className="gm-row">
                  <StatusBadge status={c.actif ? 'ok' : 'neutral'} label={t(c.actif ? 'console.comptes.actif' : 'console.comptes.inactif')} />
                  {c.premierAccesEnAttente ? <StatusBadge status="info" label={t('console.comptes.premierAcces')} /> : null}
                  <StatusBadge status={c.secondFacteur ? 'ok' : 'warn'} label={t(c.secondFacteur ? 'console.compte.2fa.oui' : 'console.comptes.sans2fa')} />
                  {c.profils.map((p) => <Tag key={p}>{libelleProfil(p)}</Tag>)}
                </div>
              </div>
            </header>
            {soiMeme ? <InlineAlert level="info" title={t('console.compte.soiMeme')} /> : null}
            <div className="gm-row" role="toolbar" aria-label={t('console.compte.actes')} style={{ flexWrap: 'wrap', margin: 'var(--gm-space-4) 0' }}>
              {peut('IAM_ACCOUNT_EDIT') ? <Button icon="pen-line" onClick={() => setDialogue('fiche')}>{t('console.compte.modifierFiche')}</Button> : null}
              {peut('IAM_ACCOUNT_ROLES') && !soiMeme ? <Button icon="shield-check" onClick={() => setDialogue('profils')}>{t('console.compte.modifierProfils')}</Button> : null}
              {peut('IAM_ACCOUNT_STATUS') && !soiMeme ? (
                <Button
                  onClick={() =>
                    c.actif
                      ? actes.motive({ titre: t('console.compte.desactiver'), libelle: t('console.compte.desactiver'), danger: true, objet, description: t('console.compte.desactiver.texte'),
                          succes: t('console.compte.desactive'), executer: (m) => s.desactiver.mutateAsync(m) })
                      : actes.motive({ titre: t('console.compte.activer'), libelle: t('console.compte.activer'), objet, succes: t('console.compte.active'), executer: (m) => s.activer.mutateAsync(m) })
                  }
                >
                  {t(c.actif ? 'console.compte.desactiver' : 'console.compte.activer')}
                </Button>
              ) : null}
              {peut('IAM_ACCOUNT_SECURITY') ? (
                <>
                  <Button icon="flag" onClick={() => setDialogue('actions')}>{t('console.compte.exigerActions')}</Button>
                  {!soiMeme ? (
                    <>
                      <Button icon="smartphone" onClick={() => actes.motive({ titre: t('console.compte.reinitialiser2fa'), libelle: t('console.compte.reinitialiser2fa'), objet, danger: true,
                        description: t('console.compte.reinitialiser2fa.texte'), succes: t('console.compte.2faReinitialise'), executer: (m) => s.secondFacteur.mutateAsync(m) })}>
                        {t('console.compte.reinitialiser2fa')}
                      </Button>
                      <Button icon="key-round" onClick={() => actes.motive({ titre: t('console.compte.motDePasseProvisoire'), libelle: t('console.compte.motDePasseProvisoire'), objet,
                        description: t('console.compte.motDePasseProvisoire.texte'), succes: t('console.compte.motDePasseDefini'),
                        executer: async (m) => setMdp((await s.motDePasse.mutateAsync(m)).motDePasseProvisoire) })}>
                        {t('console.compte.motDePasseProvisoire')}
                      </Button>
                    </>
                  ) : null}
                  <Button icon="lock" onClick={() => actes.motive({ titre: t('console.compte.fermerSessions'), libelle: t('console.compte.fermerSessions'), objet,
                    description: t('console.compte.fermerSessions.texte'), succes: t('console.compte.sessionsFermees'), executer: (m) => s.sessions.mutateAsync(m) })}>
                    {t('console.compte.fermerSessions')}
                  </Button>
                </>
              ) : null}
              {peut('IAM_ACCOUNT_DELETE') && !soiMeme ? <Button variant="danger" icon="x" onClick={() => setDialogue('suppression')}>{t('console.compte.supprimer')}</Button> : null}
            </div>
            <Annonce retour={actes.retour} />
            <Tabs items={onglets} value={onglet} onChange={setOnglet} />
            <div style={{ paddingTop: 'var(--gm-space-4)' }} role="tabpanel" aria-label={onglets.find((o) => o.key === onglet)?.label}>
              {onglet === 'fiche' ? <Fiche compte={c} /> : null}
              {onglet === 'securite' ? (
                <KeyValueList
                  items={[
                    { key: t('compte.secondFacteur'), value: <StatusBadge status={c.secondFacteur ? 'ok' : 'warn'} label={t(c.secondFacteur ? 'compte.secondFacteur.actif' : 'compte.secondFacteur.absent')} /> },
                    { key: t('console.compte.actionsRequises'), value: c.actionsRequises.length ? <span className="gm-row">{c.actionsRequises.map((a) => <Tag key={a}>{t(`console.action.${a}`, { defaultValue: a })}</Tag>)}</span> : t('console.compte.aucuneAction') },
                    { key: t('console.compte.premierAcces'), value: t(c.premierAccesEnAttente ? 'console.compte.premierAcces.enAttente' : 'console.compte.premierAcces.termine') },
                  ]}
                />
              ) : null}
              {onglet === 'sessions' ? <Sessions id={c.id} /> : null}
              {onglet === 'journal' ? <Journal id={c.id} /> : null}
            </div>
            {dialogue === 'fiche' ? (
              <DialogueFiche compte={c} onFermer={() => setDialogue(null)} onEnregistre={() => { setDialogue(null); actes.setRetour({ niveau: 'info', message: { texte: t('console.compte.ficheEnregistree') } }) }} />
            ) : null}
            {dialogue === 'profils' ? (
              <DialogueProfils compte={c} privilegie={privilegie} onFermer={() => setDialogue(null)} onContinuer={(profils) => {
                setDialogue(null)
                actes.sensible({
                  titre: t('console.compte.modifierProfils'), objet: [...objet, { key: t('compte.profils'), value: profils.map(libelleProfil).join(', ') }],
                  contenu: { compte: c.id, profils }, succes: t('console.compte.profilsModifies'),
                  executer: (motif, assertion) => s.profils.mutateAsync({ profils, motif, assertion }),
                })
              }} />
            ) : null}
            {dialogue === 'actions' ? (
              <DialogueActions onFermer={() => setDialogue(null)} onValider={(a, parCourriel, motif) => {
                setDialogue(null)
                actes.setRetour(null)
                s.actions.mutateAsync({ actions: a, parCourriel, motif }).then(
                  () => actes.setRetour({ niveau: 'info', message: { texte: t('console.compte.actionsExigees') } }),
                  (e: unknown) => actes.setRetour({ niveau: 'crit', message: message(e) }),
                )
              }} />
            ) : null}
            {dialogue === 'suppression' ? (
              <ConfirmDialog open danger irreversible title={t('console.compte.supprimer')} description={t('console.compte.supprimer.texte')} confirmLabel={t('console.compte.supprimer.continuer')}
                object={objet} onClose={() => setDialogue(null)}
                onConfirm={() => {
                  setDialogue(null)
                  actes.sensible({
                    titre: t('console.compte.supprimer'), objet, contenu: { compte: c.id, acte: 'SUPPRESSION' }, succes: t('console.compte.supprime'),
                    executer: async (motif, assertion) => {
                      await s.supprimer.mutateAsync({ motif, assertion })
                      void navigate('/console/comptes')
                    },
                  })
                }} />
            ) : null}
            {mdp ? <MotDePasse valeur={mdp} onFermer={() => setMdp(null)} /> : null}
            {actes.dialogue}
          </div>
        )
      }}
    </Donnees>
  )
}
