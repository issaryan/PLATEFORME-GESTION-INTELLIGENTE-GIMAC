import { ICONS, Icon, type IconName } from '@gimac/design-system'

export default function Pictogrammes() {
  return (
    <div className="cat-pictos">
      {(Object.keys(ICONS) as IconName[]).map((n) => (
        <div key={n} className="cat-picto">
          <Icon name={n} size={24} />
          <span className="gm-mono" style={{ fontSize: 11 }}>{n}</span>
        </div>
      ))}
    </div>
  )
}
