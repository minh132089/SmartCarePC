# Maintenance guidance and reminder implementation

Implemented from `maintenance-spec.md` on 2026-10-29. UI copy is English.

## Entry and use

- Home: Menu → 3D assembly → Maintenance guide. The 3D sidebar also has a Maintenance guide button.
- Choose the closest desktop case, cooler and whether a dedicated GPU is present. A selected advice build supplies its model directly. Laptop users cannot start a desktop guide or reminder setup.
- Existing available models are five case families plus Gaming. Slim Console remains excluded, following the earlier product decision. Illustrations are case references, not exact service manuals: instructions use the chosen cooler and disclose a reference-model mismatch.
- Dust, paste and quick-check tasks reuse GuidePanel, GuideStep, machineOf, part highlighting, guide camera and separate loadDone/saveDone keys. Maintenance displays the cooler fan / pump leads as unplugged before lifting the cooler and reconnects them at the refit step. It leaves all other parts assembled except the CPU cooler base / AIO pump during cleaning and paste application. AIO radiator and backplate stay installed; hoses remain connected in the illustration.
- Quick checks keep the case closed. Physical tasks include shutdown, unplugging and explicit PSU / AIO safety limits. Maintenance chat receives the canonical current step.
- Every step records Done it myself or I needed help. Completion asks who did the work. Leaving an unfinished guide records abandonment only for study participants.

## Reminders

The pure schedule rules live in `src/data/maintenanceSchedule.ts`. Dust intervals are three or six months. Unknown / never-cleaned history, old cleaning dates, and symptoms can make cleaning due now. An exact cleaning date overrides a range estimate. Paste is due for a PC over two years old with never-replaced paste, or persistent heat / shutdowns after dust cleaning. Unknown paste history is not treated as confirmed failure. Routine paste review uses 30 months.

Due-now work asks for a user-chosen date in the next 1–14 days, defaulting to the next weekend. Future work retains the calculated interval. The four core answers must be supplied before saving; paste history is an additional clarification needed by the rule. No contact details are collected.

`pcsc-maintenance-v1` stores PC choices, answers, appointments, completion dates and the current run. Progress uses task + model + cooler + GPU-specific guide keys. Storage access is guarded; when unavailable, the UI continues in memory and warns if study events cannot be persisted.

Calendar exports use the current app origin, so the same source works on localhost and the Cloudflare Pages alias. Links use `?baotri=dust|paste|check`, plus the anonymous study code / phase when applicable, and the app removes these parameters after reading them. ICS uses RFC 5545 CRLF, UTF-8 line folding, monthly recurrence, an all-day start/end date and a one-day VALARM. Google opens a calendar draft; users must save it and check calendar notifications. `calendar_added` denotes a draft opened or a file downloaded, not proof of notification delivery.

In-app snoozes and completion dates do not modify events already saved in an external calendar. Users need to update those events in their calendar.

## Research data

Only an active study code causes `/api/maintenance` POST requests. Events contain anonymous code, phase, model, task, event UUID, occurrence time and bounded event-specific details. A run ID links steps, completion and abandonment. The first reminder baseline includes the original answers and `never_serviced`; later answers do not overwrite historical events.

Events are queued before sending, retry on start/focus/online, and keep their UUID on retry. Backend uniqueness on `maintenance_event.event_id` handles duplicate requests, including concurrent submissions. POST validation rejects missing study metadata and missing step independence / completion outcome.

- `POST /api/maintenance/events`
- `GET /api/maintenance/list`
- `GET /api/maintenance/export.csv`

Read/export use the existing feedback key solely in `X-Export-Key`; query keys are not accepted. `/feedback` has a maintenance summary, event list and separate CSV download. Counts include non-completers and group by phase and baseline. Technical background joins by participant code + phase using the future `technical_background` feedback answer. Calendar export/click counts are labelled accordingly.

## Follow-up for form v2

As specified, this implementation does not add the four-week question to form v2. Its author still needs to add:

> Since your first visit, have you cleaned or serviced your PC? (yes, with the app / yes, someone else did it / no)

Form v2 also needs to supply technical background for Q4 grouping. Until then the dashboard reports that background as Not recorded.

## Checks

- `node tools/check_maintenance.mjs`: dates, never-cleaned history, pet environment, recent cleaning, old PC / paste history, persistent heat, Google dates and ICS recurrence / alarm / folding.
- `node tools/check_maintenance_flow.mjs`: all available models and tasks, separate GPU omission, study-only traffic, queue replay, original event IDs, per-step independence, abandonment, completion, grouping and seven-day snooze.
- `npx tsc -b`, `npx oxlint src`, `npm run build`.
- `cd backend && sh mvnw test`: original backend suite plus maintenance validation, header authorization, deduplication and a transactional MySQL integration check. Integration fixtures roll back.
- `npm run check:paths -- --maintenance`: CPU cooler movement with all other parts installed. Reuses the existing ray-based collision checker; a bounding-box broad phase excludes physically unreachable blockers before raycasting.

Browser interaction checks cover case/cooler selection, deep links, dust and paste references, quick-check completion, work outcome, reminder setup, due cards and snooze. Native calendar imports and actual notification delivery require the user's calendar app and notification settings; exporting a file alone does not verify delivery.

This change has not been deployed to Cloudflare or pushed to Git.

Final verification: 315 backend tests passed. The MySQL API test inserted one transactional fixture, posted it twice, confirmed only one row, checked list/CSV header authorization, and rolled back. All six maintenance model paths were checked; Standard, Compact and Mini were rechecked successfully after correcting disconnected cooler leads. Frontend type checking, lint command and production build passed (the lint command retains existing component warnings). The release JAR at `backend/target/smartcarepc-english-release.jar` has been rebuilt for the next backend start.
