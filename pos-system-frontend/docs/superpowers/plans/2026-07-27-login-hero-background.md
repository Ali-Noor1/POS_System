# Login Hero Background Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render the existing POS terminal artwork as the login hero container's background so it no longer behaves like a separate picture layered over the page.

**Architecture:** `LoginScreen.jsx` will keep Vite's asset import and pass the resolved URL to `.login-hero-wrap` through a CSS custom property. `App.css` will consume that property as a centered, contained, non-repeating background and retain responsive heights without styling a child image.

**Tech Stack:** React 19, Vite 8, CSS, Node.js built-in test runner, React DOM server rendering

## Global Constraints

- Keep `login-pos-terminal.png` in the existing login hero location.
- Preserve the current desktop and mobile visual sizing.
- Do not include the existing unrelated user changes in task commits.

---

### Task 1: Integrate the artwork into the hero background

**Files:**
- Create: `tests/LoginScreen.test.js`
- Modify: `src/features/auth/LoginScreen.jsx:3,65-67`
- Modify: `src/App.css:215-226,3113-3115`

**Interfaces:**
- Consumes: Vite-resolved `heroImage` URL imported by `LoginScreen.jsx`
- Produces: `.login-hero-wrap` with `--login-hero-image` set to `url("<resolved URL>")`

- [ ] **Step 1: Write the failing render test**

```js
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
      /class="login-hero-wrap" style="--login-hero-image:url\(&quot;\/src\/assets\/login-pos-terminal\.png&quot;\)"/,
    )
    assert.doesNotMatch(markup, /<img/)
  } finally {
    await vite.close()
  }
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `node --test tests/LoginScreen.test.js`

Expected: FAIL because the hero still renders a child `<img>` and does not set `--login-hero-image`.

- [ ] **Step 3: Move the image URL onto the hero container**

Replace the hero markup in `src/features/auth/LoginScreen.jsx` with:

```jsx
<div
  className="login-hero-wrap"
  style={{ '--login-hero-image': `url("${heroImage}")` }}
  role="img"
  aria-label="Retail POS terminal with barcode scanner and receipt printer"
/>
```

- [ ] **Step 4: Convert the hero CSS to a contained background**

Replace the hero rules in `src/App.css` with:

```css
.login-hero-wrap {
  width: min(500px, 100%);
  min-height: 245px;
  background-image: var(--login-hero-image);
  background-position: center;
  background-repeat: no-repeat;
  background-size: contain;
}
```

Replace the narrow-layout child-image rule with:

```css
.login-hero-wrap {
  min-height: 210px;
}
```

- [ ] **Step 5: Run the focused test**

Run: `node --test tests/LoginScreen.test.js`

Expected: PASS with 1 test and 0 failures.

- [ ] **Step 6: Run project verification**

Run: `npm run lint`

Expected: Exit code 0.

Run: `npm run build`

Expected: Exit code 0 and a generated Vite production bundle.

- [ ] **Step 7: Review the scoped diff**

Run: `git diff --check -- src/features/auth/LoginScreen.jsx src/App.css tests/LoginScreen.test.js`

Expected: Exit code 0 with no whitespace errors.

Do not commit `src/features/auth/LoginScreen.jsx` or `src/App.css` because both contain pre-existing uncommitted user work that cannot be cleanly separated from this visual change.
