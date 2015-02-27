/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.InvalidValueException;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ValueResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.request.ResourceAccessException;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mResponseBuilder<T extends LwM2mResponse> implements DownlinkRequestVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mResponseBuilder.class);

    private LwM2mResponse lwM2mresponse;
    private final Request coapRequest;
    private final Response coapResponse;
    private final ObservationRegistry observationRegistry;
    private final Client client;

    public static ResponseCode fromCoapCode(final int code) {
        Validate.notNull(code);

        if (code == CoAP.ResponseCode.CREATED.value) {
            return ResponseCode.CREATED;
        } else if (code == CoAP.ResponseCode.DELETED.value) {
            return ResponseCode.DELETED;
        } else if (code == CoAP.ResponseCode.CHANGED.value) {
            return ResponseCode.CHANGED;
        } else if (code == CoAP.ResponseCode.CONTENT.value) {
            return ResponseCode.CONTENT;
        } else if (code == CoAP.ResponseCode.BAD_REQUEST.value) {
            return ResponseCode.BAD_REQUEST;
        } else if (code == CoAP.ResponseCode.UNAUTHORIZED.value) {
            return ResponseCode.UNAUTHORIZED;
        } else if (code == CoAP.ResponseCode.NOT_FOUND.value) {
            return ResponseCode.NOT_FOUND;
        } else if (code == CoAP.ResponseCode.METHOD_NOT_ALLOWED.value) {
            return ResponseCode.METHOD_NOT_ALLOWED;
        } else if (code == CoAP.ResponseCode.FORBIDDEN.value) {
            return ResponseCode.FORBIDDEN;
        } else {
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);
        }
    }

    public LwM2mResponseBuilder(final Request coapRequest, final Response coapResponse, final Client client,
            final ObservationRegistry observationRegistry) {
        super();
        this.coapRequest = coapRequest;
        this.coapResponse = coapResponse;
        this.observationRegistry = observationRegistry;
        this.client = client;
    }

    @Override
    public void visit(final ReadRequest request) {
        switch (coapResponse.getCode()) {
        case CONTENT:
            lwM2mresponse = buildContentResponse(request.getPath(), coapResponse);
            break;
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new ValueResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        default:
            handleUnexpectedResponseCode(client.getEndpoint(), coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final DiscoverRequest request) {
        switch (coapResponse.getCode()) {
        case CONTENT:
            LinkObject[] links = null;
            if (MediaTypeRegistry.APPLICATION_LINK_FORMAT != coapResponse.getOptions().getContentFormat()) {
                LOG.debug("Expected LWM2M Client [{}] to return application/link-format [{}] content but got [{}]",
                        client.getEndpoint(), MediaTypeRegistry.APPLICATION_LINK_FORMAT, coapResponse.getOptions()
                                .getContentFormat());
                links = new LinkObject[] {}; // empty list
            } else {
                links = LinkObject.parse(coapResponse.getPayload());
            }
            lwM2mresponse = new DiscoverResponse(fromCoapCode(coapResponse.getCode().value), links);
            break;
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new DiscoverResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        default:
            handleUnexpectedResponseCode(client.getEndpoint(), coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final WriteRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = new LwM2mResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new LwM2mResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        default:
            handleUnexpectedResponseCode(client.getEndpoint(), coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final WriteAttributesRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = new LwM2mResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        case BAD_REQUEST:
        case NOT_FOUND:
        case UNAUTHORIZED:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new LwM2mResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        default:
            handleUnexpectedResponseCode(client.getEndpoint(), coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final ExecuteRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            lwM2mresponse = new LwM2mResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        case BAD_REQUEST:
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new LwM2mResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        default:
            handleUnexpectedResponseCode(client.getEndpoint(), coapRequest, coapResponse);
        }

    }

    @Override
    public void visit(final CreateRequest request) {
        switch (coapResponse.getCode()) {
        case CREATED:
            lwM2mresponse = new CreateResponse(fromCoapCode(coapResponse.getCode().value), coapResponse.getOptions()
                    .getLocationPathString());
            break;
        case BAD_REQUEST:
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new CreateResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        default:
            handleUnexpectedResponseCode(client.getEndpoint(), coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final DeleteRequest request) {
        switch (coapResponse.getCode()) {
        case DELETED:
            lwM2mresponse = new LwM2mResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        case UNAUTHORIZED:
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new LwM2mResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        default:
            handleUnexpectedResponseCode(client.getEndpoint(), coapRequest, coapResponse);
        }
    }

    @Override
    public void visit(final ObserveRequest request) {
        switch (coapResponse.getCode()) {
        case CHANGED:
            // ignore changed response (this is probably a NOTIFY)
            lwM2mresponse = null;
            break;
        case CONTENT:
            lwM2mresponse = buildContentResponse(request.getPath(), coapResponse);
            if (coapResponse.getOptions().hasObserve()) {
                // observe request succeed so we can add and observation to registry
                final CaliforniumObservation observation = new CaliforniumObservation(coapRequest, client,
                        request.getPath());
                coapRequest.addMessageObserver(observation);
                observationRegistry.addObservation(observation);
            }
            break;
        case NOT_FOUND:
        case METHOD_NOT_ALLOWED:
            lwM2mresponse = new ValueResponse(fromCoapCode(coapResponse.getCode().value));
            break;
        default:
            handleUnexpectedResponseCode(client.getEndpoint(), coapRequest, coapResponse);
        }
    }

    private ValueResponse buildContentResponse(final LwM2mPath path, final Response coapResponse) {
        final ResponseCode code = ResponseCode.CONTENT;
        LwM2mNode content;
        try {
            content = LwM2mNodeDecoder.decode(coapResponse.getPayload(),
                    ContentFormat.fromCode(coapResponse.getOptions().getContentFormat()), path);
        } catch (final InvalidValueException e) {
            final String msg = String.format("[%s] ([%s])", e.getMessage(), e.getPath().toString());
            throw new ResourceAccessException(code, path.toString(), msg, e);
        }
        return new ValueResponse(code, content);
    }

    @SuppressWarnings("unchecked")
    public T getResponse() {
        return (T) lwM2mresponse;
    }

    /**
     * Throws a generic {@link ResourceAccessException} indicating that the client returned an unexpected response code.
     *
     * @param request
     * @param coapRequest
     * @param coapResponse
     */
    private void handleUnexpectedResponseCode(final String clientEndpoint, final Request coapRequest,
            final Response coapResponse) {
        final String msg = String.format("Client [%s] returned unexpected response code [%s]", clientEndpoint,
                coapResponse.getCode());
        throw new ResourceAccessException(fromCoapCode(coapResponse.getCode().value), coapRequest.getURI(), msg);
    }
}
