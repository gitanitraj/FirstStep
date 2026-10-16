import type { ContentType } from '../types/api';

/**
 * ContentType → i18n key for its badge label.
 *
 * Extracted in Slice F6 once a second component needed it (ContentCard, beside
 * CategoryUpdates) — the same "an abstraction earns its name on the second use"
 * rule the backend services follow.
 *
 * Typed as an exhaustive `Record<ContentType, …>`, so adding a content type
 * without giving it a label FAILS THE BUILD rather than rendering `undefined`.
 *
 * This is where Decision 036's exit criterion lands on the frontend: the backend
 * says what a thing IS (`contentType`), and the display string is chosen here,
 * per locale. No presentation label ever crosses the wire.
 */
export const CONTENT_TYPE_LABEL: Record<ContentType, string> = {
  RESOURCE: 'contentType.resource',
  NEWS: 'contentType.news',
  LAW: 'contentType.law',
  FLYER: 'contentType.flyer',
  EXPERT: 'contentType.expert',
};
