# OmniAgent Canva Visual Spec

This document is the Canva-ready source of truth for the enterprise AI console refresh. Recreate the four key boards in Canva at 1920 x 1080, then use the same tokens in the Vue implementation.

## Direction

- Product tone: enterprise AI command center, calm, precise, data-heavy.
- Layout: dark navigation rail, cool white workspace, strict 12-column grid, clear operational hierarchy.
- Avoid: marketing hero layouts, purple gradients, decorative blobs, nested cards, oversized rounded pills.

## Tokens

| Token | Value | Use |
| --- | --- | --- |
| Ink 950 | `#07111f` | Sidebar and high-emphasis text |
| Ink 900 | `#0b1628` | Navigation surface |
| Ink 800 | `#132238` | Dark panels and chart backplates |
| Slate 700 | `#344256` | Secondary text |
| Slate 500 | `#64748b` | Muted text |
| Slate 200 | `#d9e2ef` | Borders |
| Canvas | `#f5f8fc` | App background |
| Surface | `#ffffff` | Primary panels |
| Surface Alt | `#eef4fb` | Strips and empty states |
| Cyan | `#11b5c8` | AI/agent active states |
| Blue | `#2f6fed` | Primary action and graph accents |
| Green | `#16a36f` | Success |
| Amber | `#f59e0b` | Warning and key attention |
| Red | `#dc2626` | Failure |

## Typography

- Display: `Plus Jakarta Sans`, Semibold/Bold.
- Chinese fallback: `Microsoft YaHei`, `PingFang SC`.
- H1: 32-40px, line-height 1.08.
- H2: 18-22px, line-height 1.2.
- Body: 14px, line-height 1.6.
- Labels: 11-12px uppercase English or concise Chinese.

## Canva Boards

1. Login
   - Split composition: left product command surface, right authentication panel.
   - Use a dark AI topology preview on the left, not a generic illustration.
   - Right panel width: 420-480px, rectangular radius 8-12px.

2. Dashboard
   - Top command header across the workspace.
   - First row: 3 metric blocks.
   - Main row: recent conversations 8 columns, capability/risk summary 4 columns.

3. Agent Workbench
   - Three-zone structure: conversation rail, task composer, trace/output stack.
   - Graph panel gets a dark technical backplate so agent nodes read as operational telemetry.
   - Context files are shown as compact rows, not large decorative cards.

4. Configuration/Data Template
   - Left 5 columns: form panel.
   - Right 7 columns: inventory/table panel.
   - Reused for Knowledge, Tools, Models, Stats, and Feedback screens.

## Component Rules

- Panels: radius 10px, 1px slate border, white surface, no card inside card.
- Buttons: 8-10px radius, icon + text for commands, icon-only only for compact tools.
- Badges: small rectangular chips, status color only; never use large pill clusters for core layout.
- Forms: labels top-aligned, two-column only above 1100px.
- Mobile: sidebar becomes a horizontal navigation strip; all content stacks to one column.
