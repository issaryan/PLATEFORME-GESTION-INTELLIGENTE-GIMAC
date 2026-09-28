import './couverture.css'

const losanges = (x: number) =>
  Array.from({ length: 9 }, (_, i) => `M${x} ${i * 32} L${x - 16} ${i * 32 + 16} L${x} ${i * 32 + 32} L${x + 16} ${i * 32 + 16} Z`).join(' ')

export default function Couverture() {
  return (
    <div className="cat-cover">
      <div className="art" aria-hidden="true">
        <svg viewBox="0 0 480 288" width="480" height="288">
          <rect className="action" x="0" y="0" width="192" height="288" />
          <rect className="logo" x="208" y="0" width="96" height="128" />
          <rect className="ok" x="208" y="144" width="96" height="64" />
          <rect className="sunken" x="208" y="224" width="96" height="64" />
          <rect className="earth" x="320" y="0" width="64" height="288" />
          <rect className="sunken" x="400" y="0" width="80" height="288" />
          <g className="trame">
            <path d={losanges(352)} />
            <path d={losanges(440)} />
            <path d="M256 240 L240 256 L256 272 L272 256 Z" />
          </g>
        </svg>
      </div>
      <div className="words">
        <h2 className="name">
          GIMAC<span>DES</span>
        </h2>
        <p className="tag">Un instrument de mesure fiable pour l'exploitation et la supervision monétique.</p>
      </div>
    </div>
  )
}
