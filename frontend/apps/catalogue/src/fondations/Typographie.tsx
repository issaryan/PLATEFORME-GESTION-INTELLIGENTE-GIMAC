import jetons from '@gimac/design-system/tokens.json'

export default function Typographie() {
  return (
    <div className="cat-planche">
      {jetons.type.groups.map((g) => (
        <div key={g.name}>
          <h2 className="gm-type-h2">{g.name}</h2>
          {g.styles.map((s) => (
            <div key={s.name} className="cat-type-ligne">
              <div>
                <div className="gm-mono">{s.name}</div>
                <div className="gm-field-help">
                  {s.fontSize} / {s.lineHeight} · {s.fontWeight}
                </div>
              </div>
              <div>
                <div className={s.name}>{s.sample}</div>
                <div className="gm-field-help">{s.usage}</div>
              </div>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}
