import assert from 'node:assert/strict'
import test from 'node:test'
import { createServer } from 'vite'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'

test('renders the POS artwork as the login hero background', async () => {
  const vite = await createServer({ server: { middlewareMode: true } })

  try {
    const { default: LoginScreen } = await vite.ssrLoadModule(
      '/src/features/auth/LoginScreen.jsx',
    )
    const markup = renderToStaticMarkup(
      createElement(LoginScreen, { onLogin: () => {} }),
    )

    assert.match(
      markup,
      /class=\x22login-hero-wrap\x22 style=\x22--login-hero-image:url\(&quot;\/src\/assets\/login-pos-terminal-cutout\.png&quot;\)\x22/,
    )
    assert.doesNotMatch(markup, /<img/)
  } finally {
    await vite.close()
  }
})
