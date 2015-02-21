/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

import org.osgi.framework.Bundle;

/**
 * Provides the event information for a Wiring Admin event.
 * 
 * @Immutable
 */
public class WiringAdminEvent {
	/**
	 * Add an import registration. 
	 */
	public static final int			IMPORT_REGISTRATION		= 1;

	/**
	 * Add an export registration.
	 */
	public static final int			EXPORT_REGISTRATION		= 2;

	/**
	 * Remove an export registration. 
	 */
	public static final int			EXPORT_UNREGISTRATION	= 3;

	/**
	 * Remove an import registration. 
	 */
	public static final int			IMPORT_UNREGISTRATION	= 4;

	/**
	 * A fatal importing error occurred. The Import Registration has been
	 * closed.
	 */
	public static final int			IMPORT_ERROR			= 5;

	/**
	 * A fatal exporting error occurred. The Export Registration has been
	 * closed.
	 */
	public static final int			EXPORT_ERROR			= 6;

	/**
	 * A problematic situation occurred, the export is still active.
	 */
	public static final int			EXPORT_WARNING			= 7;
	/**
	 * A problematic situation occurred, the import is still active.
	 */
	public static final int			IMPORT_WARNING			= 8;

	private final WiringEndpointDescription endpointDesciption;
	private final Throwable			exception;
	private final int				type;
	private final Bundle			source;

	/**
	 * Private constructor.
	 * 
	 * @param type The event type
	 * @param source The source bundle, must not be {@code null}.
	 * @param endpointDescription The endpointDescription, can be {@code null}.
	 * @param exception Any exceptions encountered, can be {@code null}
	 */
	private WiringAdminEvent(int type, Bundle source, WiringEndpointDescription endpointDescription, Throwable exception) {
		if (source == null) {
			throw new NullPointerException("source must not be null");
		}
		this.type = type;
		this.source = source;
		this.endpointDesciption = endpointDescription;
		this.exception = exception;
	}

	/**
	 * Return the Endpoint Description for this event.
	 * 
	 * @return The EndpointDescription or {@code null}.
	 */
	public WiringEndpointDescription getEndpointDescription() {
		return endpointDesciption;
	}

	/**
	 * Return the exception for this event.
	 * 
	 * @return The exception or {@code null}.
	 */
	public Throwable getException() {
		return exception;
	}

	/**
	 * Return the type of this event.
	 * 
	 * @return The type of this event.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Return the bundle source of this event.
	 * 
	 * @return The bundle source of this event.
	 */
	public Bundle getSource() {
		return source;
	}
}
