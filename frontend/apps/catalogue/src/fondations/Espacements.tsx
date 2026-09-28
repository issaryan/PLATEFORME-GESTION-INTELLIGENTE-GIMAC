import jetons from '@gimac/design-system/tokens.json'

export default function Espacements() {
  return (
    <div className="cat-planche">
      <div>
        <h2 className="gm-type-h2">Espacement : pas de 4 px, grille de 8 px</h2>
        {jetons.spacing.tokens.map((t) => (
          <div key={t.name} className="cat-reglette">
            <span className="gm-mono">{t.name} · {t.value}</span>
            <span className="cat-reglette-barre" style={{ width: `var(--${t.name})` }} />
          </div>
        ))}
      </div>
      {(['radius', 'border', 'duration', 'size', 'breakpoint'] as const).map((f) => (
        <div key={f}>
          <h2 className="gm-type-h2">{jetons[f].note}</h2>
          <dl className="gm-kv">
            {jetons[f].tokens.map((t) => (
              <div key={t.name} style={{ display: 'contents' }}>
                <dt className="gm-mono">{t.name}</dt>
                <dd>
                  <span className="gm-num">{t.value}</span> · {t.usage}
                </dd>
              </div>
            ))}
          </dl>
        </div>
      ))}
    </div>
  )
}
