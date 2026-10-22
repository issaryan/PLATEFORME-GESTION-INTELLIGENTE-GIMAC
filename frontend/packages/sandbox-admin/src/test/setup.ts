import { afterEach } from 'vitest'
import { cleanup, configure } from '@testing-library/react'

// Services simulés, chargements et dialogues : délai de Testing Library allongé sous la charge ; écran démonté après chaque test.
configure({ asyncUtilTimeout: 5000 })
afterEach(() => cleanup())
