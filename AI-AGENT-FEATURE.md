# AI assistant (DeepSeek, role-scoped tools, voice input)

An assistant integrated into both portals that can answer questions about the system
and perform real actions on the user's behalf - registering inventory, submitting or
approving requests, allocating stock, etc. - strictly limited to what that user's role
is already allowed to do everywhere else in the app.

## How it's built

Everything lives in a new, self-contained package (`backend/.../com/inventory/agent/`)
and three new frontend files. **Nothing that already existed was modified beyond three
small, additive touches**, listed at the bottom of this file.

### Role-scoped tools, not free-form access

The assistant never talks to the database directly and never has standing permissions
of its own. It has a fixed set of **tools** (`AgentToolRegistry`), each one a thin
wrapper around an existing service method you already had - `InventoryService.create`,
`AllocationService.allocate`, `InventoryRequestService.updateRequestStatus`, and so on -
and each tool declares exactly which role(s) may use it:

- **ADMIN**: everything DEPARTMENT can do, plus registering/adding inventory, creating
  categories, allocating stock, and approving/rejecting requests, and reading data across
  the whole system (all departments' allocations, all requests).
- **DEPARTMENT**: read the item catalog and categories, scan barcodes, and read/submit
  requests and view allocations - always scoped to the caller's own department. Tools
  scoped to "my department" hard-code the caller's actual department from their
  authenticated session; there is no parameter a department user (or the model, even if
  misled) could supply to read or act on another department's data.
- **Deliberately excluded for every role**: creating, deleting, or changing a user
  account, or changing anyone's role. That stays exclusively in the dedicated Users page,
  regardless of who's asking the assistant.

On top of the role check, every tool that bypasses the normal `@Valid` controller layer
(by calling a service method directly) re-implements whatever validation would
otherwise be lost - e.g. the category-code format check, and the "needed-by date can't
be in the past" check - so calling a service through the assistant is exactly as safe as
calling it through the existing UI, never less.

If the model asks for a tool outside the caller's role (or a tool that doesn't exist),
the request fails gracefully with a message the assistant relays back in plain language
("that requires ADMIN access") - it never causes an error page or a silent bypass.

### The conversation loop

`AgentService` sends the user's message, a role-appropriate system prompt, and the
role-appropriate tool list to DeepSeek. If DeepSeek's response asks to call a tool,
`AgentService` executes it and feeds the result back, repeating (capped at 5 rounds)
until DeepSeek produces a final plain-language answer. A failed or invalid tool call
becomes an `{"error": "..."}` result fed back to the model - the conversation continues
gracefully instead of the whole request crashing.

Conversation history is kept client-side only (`sessionStorage`, capped to the last 20
turns sent per request) - there's no new chat-log table in the database. This was a
deliberate scope choice to avoid a new persistence layer; if you want conversations to
survive a browser restart or be reviewable later, that's a contained addition (a new
`AgentMessage` table plus a couple of endpoints) - flag it and I'll build it.

### Voice input

The microphone button uses the browser's built-in Web Speech API
(`SpeechRecognition`/`webkitSpeechRecognition`) - genuinely free, no extra API or cost,
entirely client-side. Support varies by browser (best in Chrome/Edge; not available
everywhere) and generally requires HTTPS (or localhost) for microphone permission, same
as any site using the microphone. The button disables itself with an explanatory tooltip
if the browser doesn't support it, rather than failing silently.

## On "free"

The Web Speech API voice input is genuinely free. **DeepSeek's API itself is not
literally free** - it's low-cost, usage-based pricing (pay per token), not a flat $0.
You'll need a DeepSeek account and API key. If `DEEPSEEK_API_KEY` is left unset, the
assistant shows a clear "not configured yet" banner instead of failing oddly - the rest
of the app is completely unaffected either way.

## Setup

1. Get a key at https://platform.deepseek.com
2. Set `DEEPSEEK_API_KEY` (see `_env.example`) - optionally `DEEPSEEK_MODEL` if you want
   a different DeepSeek model than the default `deepseek-chat`
3. Redeploy the backend - no database migration is needed for this feature, it adds no
   new tables

## What actually touched existing files (the only three)

- `application.properties` / `_env.example`: added the new `DEEPSEEK_API_KEY` /
  `deepseek.*` config lines - purely additive, nothing existing changed
- `frontend/js/api.js`: added one small function (`injectAssistantNavLink`) and one call
  to it inside the existing `requireAuth()`, so every admin/department page picks up an
  "Assistant" sidebar link automatically instead of hand-editing 15 separate HTML files'
  navigation markup
- `frontend/css/admin.css`: added new, additive CSS rules for the chat UI (message
  bubbles, input row, mic button states) - no existing rule was changed

Every other file in this feature is new.

## Try asking it things like

- "What's low on stock right now?"
- "Show me pending requests" (ADMIN)
- "Request 10 reams of A4 paper for my department, needed by the 30th" (DEPARTMENT)
- "Add 25 units of HP EliteBook 840, category IT Equipment" (ADMIN)
- "Allocate 5 of those laptops to the Registrar's office" (ADMIN)
- "What is INV-ELEC-0007-0003?" (any role, if you have a barcode to look up)
