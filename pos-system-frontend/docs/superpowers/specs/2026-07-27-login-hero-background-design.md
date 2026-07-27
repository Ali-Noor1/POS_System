# Login Hero Background Design

## Goal

Make the POS terminal artwork feel integrated into the login page by using
`login-pos-terminal.png` as the background of `.login-hero-wrap` instead of
rendering it as a separate `<img>` element.

## Design

- Keep the existing `.login-hero-wrap` location in the login showcase.
- Remove the hero `<img>` from `LoginScreen.jsx`.
- Apply the imported image to `.login-hero-wrap` through an inline CSS custom
  property so Vite continues to resolve the asset URL.
- Render the artwork as a centered, contained, non-repeating background.
- Preserve the current desktop and mobile hero heights.
- Remove the image drop shadow that creates the raised, pasted-on appearance.

## Accessibility

The artwork is decorative and will not carry an accessible name after becoming
a CSS background. The surrounding login content already communicates the page
purpose.

## Verification

- Build the application successfully.
- Confirm the hero artwork appears within the login showcase on desktop.
- Confirm it remains centered and contained on narrow layouts.
- Confirm no separate image element or drop shadow remains.
