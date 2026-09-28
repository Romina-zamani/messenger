---
product: Java Messenger Learning Project
mode: seed
status: approved
---

# Overview

The interface is a focused shared-room messenger for learning WebSocket behavior. It uses a light, quiet workspace with a compact instructional rail on wide screens and the conversation as the primary surface. On small screens the rail becomes a short introduction above the chat. The design should feel like a dependable developer tool without looking like a terminal.

# Colors

- Canvas: `#F3F5F1`, a soft neutral that reduces glare.
- Surface: `#FBFCF9`, used for the conversation area and inputs.
- Primary text: `#17201F`.
- Secondary text: `#66716E`.
- Accent: `#176B61`, used for primary actions, focus, and connected state.
- Accent soft: `#DDEDE8`, used for the current user's messages and status backgrounds.
- Border: `#D5DDD9`.
- Error: `#A63838` with a pale error background.

Use the accent sparingly. Do not use gradients, glass effects, or pure black and white.

# Typography

Use the system UI stack for fast loading and a familiar application feel. Headings use a slightly tighter letter spacing and medium-to-bold weight. Body text remains at least 16px on inputs and important reading surfaces. Technical labels such as `WS`, event names, and timestamps may use the system monospace stack.

# Elevation

Structure the interface primarily with spacing, borders, and tonal surface changes. Use one subtle shadow only for the main application frame on wide screens. Buttons and message bubbles should not float with independent shadows.

# Components

- Connection indicator: icon, text, and color; never color alone.
- Join form: nickname field with supporting validation text and a clear primary action.
- Message stream: server-authoritative messages, sender, and local-format timestamp.
- Composer: message field, character count, and send button.
- Error notice: concise explanation plus a reconnect action when useful.
- Learning rail: a short live explanation of browser-to-server-to-database-to-clients flow.

# Do's and Don'ts

Do keep the connection state visible, use semantic controls, preserve a clear reading order, support keyboard navigation, and keep motion between 150–250ms for direct state changes.

Don't append optimistic messages, hide errors in developer tools, use excessive cards, animate decorative elements, or make the learning notes compete with the conversation.
