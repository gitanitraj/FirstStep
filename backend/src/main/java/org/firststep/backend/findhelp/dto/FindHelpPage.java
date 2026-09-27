package org.firststep.backend.findhelp.dto;

import java.util.List;

import org.firststep.backend.shared.dto.ContentItem;

/**
 * The Find Help page: where a resident works out where to begin.
 *
 * <p>Three levels of engagement, in the order a resident needs them:
 *
 * <ol>
 *   <li><b>I know what I need</b> → {@code gateways}, which hand off to category
 *       pages.</li>
 *   <li><b>I need a starting point</b> → {@code openToEveryone}.</li>
 *   <li><b>I know who I am looking for</b> → {@code directory}.</li>
 * </ol>
 *
 * <p><b>The directory is deliberately last.</b> Find Help is a gateway, not a
 * database; asking a resident in difficulty to scan 63 organizations before
 * offering them a way in would be the alphabetical directory this page was
 * scoped against.
 *
 * <p><b>Why {@code openToEveryone} and not "Popular" or "Recently added".</b>
 * Both were considered and neither is supportable: First Step collects no usage
 * data, so popularity would be an invented signal, and resources carry no date
 * field at all. This set has a rule that can be stated in its own heading —
 * services with no age or gender restriction — so the label IS the disclosure and
 * nothing is implied about quality or endorsement.
 */
public record FindHelpPage(
        List<CategoryGateway> gateways,
        /** Services with no eligibility restriction. The heading states the rule. */
        List<ContentItem> openToEveryone,
        List<DirectoryEntry> directory
) {
}
