import { Button, Checkbox, DateTime, IconButton, Popover, Tooltip } from '@gimac/design-system'

const rien = () => {}

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-7)' }}>
      <Popover label="Colonnes affichées" title="Colonnes affichées" width={280}>
        <div className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
          <Checkbox label="Code participant" checked onChange={rien} />
          <Checkbox label="Pays" checked onChange={rien} />
          <Checkbox label="Banque de règlement" checked={false} onChange={rien} />
          <Checkbox label="Date d'adhésion" checked={false} onChange={rien} />
          <div className="gm-row" style={{ justifyContent: 'flex-end', marginTop: 'var(--gm-space-2)' }}>
            <Button size="sm" variant="ghost">Réinitialiser</Button>
            <Button size="sm" variant="primary">Appliquer</Button>
          </div>
        </div>
      </Popover>
      <div className="gm-col" style={{ gap: 'var(--gm-space-6)', paddingTop: 'var(--gm-space-6)' }}>
        <Tooltip text="Copier l'identifiant de corrélation" open>
          <IconButton variant="ghost" icon="copy" label="Copier l'identifiant de corrélation" />
        </Tooltip>
        <Tooltip text="28/09/2026 13:05 UTC" open placement="bottom">
          <DateTime local="28/09/2026 14:05" utc="28/09/2026 13:05" />
        </Tooltip>
      </div>
    </div>
  )
}
