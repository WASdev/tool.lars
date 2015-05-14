/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ibm.ws.lars.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.apache.wink.common.model.multipart.InPart;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.ws.lars.rest.Condition.Operation;
import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetList;
import com.ibm.ws.lars.rest.model.Attachment;
import com.ibm.ws.lars.rest.model.AttachmentContentResponse;
import com.ibm.ws.lars.rest.model.AttachmentList;
import com.ibm.ws.lars.rest.model.RepositoryResourceLifecycleException;

/**
 * Simple REST handler for the on-premise repository solution.
 * <p>
 * A number of methods in this class throw JsonProcessingException. These are produce when
 * manipulating retrieved database objects. As the back-end is currently Mongo, there should never
 * be any invalid JSON, so a JsonProcessingException indicates something bad, and a 500 response is
 * appropriate.
 * <p>
 * Although this class is annotated with {@link PermitAll}, permissions are further restricted by a
 * security constraint in the web.xml which ensures that the user at least has the User or
 * Administrator role.
 */
@Path("/ma/v1")
@PermitAll
public class RepositoryRESTResource {

    private static final String USER_ROLE = "User";
    private static final String ADMIN_ROLE = "Administrator";

    private static final Logger logger = Logger.getLogger(RepositoryRESTResource.class.getCanonicalName());

    @Inject
    private AssetServiceLayer assetService;

    public RepositoryRESTResource() {
        // constructor left intentionally blank
    }

    enum ArtefactType {
        ASSET("asset"), ATTACHMENT("attachment");
        String value;

        ArtefactType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    };

    @GET
    @Path("/assets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssets(@Context UriInfo info) throws JsonProcessingException {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("getAssets called with query parameters: " + info.getRequestUri().getRawQuery());
        }

        // Specifiying 'true' results in decoded % sequences, but not '+' for spaces
        // which is a bit odd. Hence the step below to decode manually
        MultivaluedMap<String, String> params = info.getQueryParameters(false);
        decodeParams(params);

        // Massive appears to use any query parameters as filters, apart from a
        // few exceptions. For now, remove the exceptions and ignore them,
        // so the relevant features can be introduced in the future if required.
        // 'limit' and 'offset' - used for pagination
        // 'apiKey' - used to allow different market places on the same server
        // TODO: Removing these should probably cause a log warning message
        params.remove("limit");
        params.remove("offset");
        params.remove("fields");
        params.remove("apiKey");

        // A parameter of 'q' is a search term
        String searchTerm = null;
        List<String> searches = params.remove("q");
        if (searches != null && searches.size() != 0) {
            searchTerm = searches.get(searches.size() - 1);
            if (searchTerm.isEmpty()) {
                // massive seems to treat this is as no search at all
                searchTerm = null;
            }
        }

        // process any remaining parameters as filters
        // Filters have the following syntax
        // field=value[|value]...

        // Multiple filters with the same name are meaningless as the value is treated as an exact match
        // use the last one in the list.
        Map<String, List<Condition>> filterMap = new HashMap<>();
        for (Entry<String, List<String>> entry : params.entrySet()) {
            List<String> values = entry.getValue();
            int size = values.size();
            String lastValue = values.get(size - 1);

            // Pipe (|) separates values which should be ORed together
            // split(<regex>, -1) is used to retain any trailing empty strings, ensuring that we always have a non-empty list
            List<String> orParts = new ArrayList<String>(Arrays.asList(lastValue.split("\\|", -1)));

            List<Condition> conditions = new ArrayList<>();

            // The first value can begin with ! to indicate that a filter for NOT that value
            if (orParts.get(0).startsWith("!")) {
                conditions.add(new Condition(Operation.NOT_EQUALS, orParts.get(0).substring(1)));
                orParts.remove(0);
            }

            // Any later values beginning with ! are ignored
            for (Iterator<String> iterator = orParts.iterator(); iterator.hasNext();) {
                if (iterator.next().startsWith("!")) {
                    iterator.remove();
                }
            }

            // Finally all remaining values represent an equals condition
            if (!orParts.isEmpty()) {
                for (String part : orParts) {
                    conditions.add(new Condition(Operation.EQUALS, part));
                }
            }

            filterMap.put(entry.getKey(), conditions);
        }

        AssetList assets = assetService.retrieveAllAssets(filterMap, searchTerm);
        String json = assets.toJson();
        return Response.ok(json).build();
    }

    @POST
    @Path("/assets")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN_ROLE)
    public Response postAssets(String assetJSON) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("postAssets called with json content:\n" + assetJSON);
        }

        Asset asset = null;
        try {
            asset = assetService.createAsset(Asset.deserializeAssetFromJson(assetJSON));
        } catch (InvalidJsonAssetException e) {
            String body = getErrorJson(Response.Status.BAD_REQUEST, "Invalid asset definition");
            return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
        }

        return Response.ok(asset.toJson()).build();
    }

    @GET
    @Path("/assets/{assetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAsset(@PathParam("assetId") String assetId) throws InvalidIdException, NonExistentArtefactException {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("getAsset called with id of '" + assetId + "'");
        }

        sanitiseId(assetId, ArtefactType.ASSET);

        Asset asset = assetService.retrieveAsset(assetId);

        return Response.ok(asset.toJson()).build();
    }

    @DELETE
    @Path("/assets/{assetId}")
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteAsset(@PathParam("assetId") String assetId) throws InvalidIdException, NonExistentArtefactException {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("deleteAsset called with id of " + assetId);
        }

        sanitiseId(assetId, ArtefactType.ASSET);

        assetService.deleteAsset(assetId);
        // TODO This could produce a 202 (rather than a 204 no content), to
        // reflect that there is no guarantee that mongo's delete is complete
        return Response.noContent().build();
    }

    @POST
    @Path("/assets/{assetId}/attachments")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN_ROLE)
    public Response createAttachmentWithContent(@QueryParam("name") String name,
                                                @PathParam("assetId") String assetId,
                                                @Context HttpServletRequest request,
                                                BufferedInMultiPart inMultiPart
            ) throws InvalidJsonAssetException, InvalidIdException, AssetPersistenceException {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("createAttachmentWithContent called, name: " + name + " assetId: " + assetId);
        }

        sanitiseId(assetId, ArtefactType.ASSET);

        List<InPart> parts = inMultiPart.getParts();

        Attachment attachmentMetadata = null;
        InputStream contentStream = null;
        String contentType = null;

        for (InPart part : parts) {
            String partName = part.getPartName();
            if ("attachmentInfo".equals(partName)) {

                attachmentMetadata = Attachment.jsonToAttachment(part.getInputStream());
            } else if (partName != null && partName.equals(name)) {
                contentType = part.getContentType();

                contentStream = part.getInputStream();
            }
        }

        Attachment result = assetService.createAttachmentWithContent(assetId, name, attachmentMetadata, contentType, contentStream);

        return Response.ok(result.toJson()).build();
    }

    @POST
    @Path("/assets/{assetId}/attachments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN_ROLE)
    public Response createAttachmentNoContent(@QueryParam("name") String name,
                                              @PathParam("assetId") String assetId,
                                              @Context HttpServletRequest request,
                                              String bodyJSON) throws InvalidJsonAssetException, InvalidIdException, AssetPersistenceException {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("createAttachmentNoContent called, name: " + name
                        + " assetId: " + assetId + " json content:\n" + bodyJSON);
        }

        sanitiseId(assetId, ArtefactType.ASSET);

        Attachment attachmentMetadata = Attachment.jsonToAttachment(bodyJSON);

        Attachment result = assetService.createAttachmentNoContent(assetId, name, attachmentMetadata);

        return Response.ok(result.toJson()).build();
    }

    @GET
    @Path("/assets/{assetId}/attachments")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ ADMIN_ROLE, USER_ROLE })
    public Response getAttachments(@PathParam("assetId") String assetId) throws IOException, ServletException, InvalidJsonAssetException, InvalidIdException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("getAttachments called for assetId: " + assetId);
        }

        sanitiseId(assetId, ArtefactType.ASSET);

        AttachmentList attachments = assetService.retrieveAttachmentsForAsset(assetId);

        return Response.ok(attachments.toJson()).build();
    }

    @DELETE
    @Path("/assets/{assetId}/attachments/{attachmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN_ROLE)
    public Response deleteAttachment(@PathParam("assetId") String assetId,
                                     @PathParam("attachmentId") String attachmentId) throws InvalidIdException {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("deleteAttachment called for assetId: " + assetId + " and attachmentId: " + attachmentId);
        }

        sanitiseId(assetId, ArtefactType.ASSET);
        sanitiseId(attachmentId, ArtefactType.ATTACHMENT);

        assetService.deleteAttachment(attachmentId);

        return Response.noContent().build();
    }

    @GET
    @Path("/assets/{assetId}/attachments/{attachmentId}/{name}")
    public Response getAttachmentContent(@PathParam("assetId") String assetId,
                                         @PathParam("attachmentId") String attachmentId,
                                         @PathParam("name") String name) throws InvalidIdException, NonExistentArtefactException {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("getAttachmentContent called for assetId: " + assetId
                        + " attachmentId: " + attachmentId + " name: " + name);
        }

        sanitiseId(assetId, ArtefactType.ASSET);
        sanitiseId(attachmentId, ArtefactType.ATTACHMENT);

        AttachmentContentResponse contentResponse = assetService.retrieveAttachmentContent(assetId, attachmentId, name);
        if (contentResponse != null) {
            final InputStream contentInputStream = contentResponse.getContentStream();
            StreamingOutput stream = new InputStreamStreamingOutput(contentInputStream);

            return Response.ok(stream)
                    .header("Content-Type", contentResponse.getContentType())
                    .build();
        } else {
            String body = getErrorJson(Response.Status.NOT_FOUND, "Could not find attachment for id " + attachmentId);
            return Response.status(Response.Status.NOT_FOUND).entity(body).build();
        }
    }

    @PUT
    @Path("/assets/{assetId}/state")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN_ROLE)
    public Response updateAssetState(@PathParam("assetId") String assetId, String actionJSON)
            throws NonExistentArtefactException, RepositoryResourceLifecycleException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("updateAssetState called for assetId: " + assetId + " action: " + actionJSON);
        }

        Asset.StateAction action = getStateAction(actionJSON);
        if (action == null) {
            String error = "Either the supplied JSON was badly formed, or it did not contain a valid 'action' field: " + actionJSON;
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(getErrorJson(Response.Status.BAD_REQUEST, error))
                    .build();
        }

        assetService.updateAssetState(action, assetId);
        return Response.ok().build();
    }

    /**
     * Returns a dummy installation manager repository.config file
     * <p>
     * This is needed so that a liberty repository can be added to installation manager through the
     * repository config panel.
     *
     * @return a dummy IM repository.config file
     */
    @GET
    @Path("/repository.config")
    @Produces(MediaType.TEXT_PLAIN)
    public String getFakeImConfig() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("getFakeImConfig called");
        }

        return "LayoutPolicy=Composite\n"
               + "LayoutPolicyVersion=0.0.0.1\n"
               + "# repository.type=liberty.lars\n";
    }

    /**
     * Check that id represents a valid asset id. Currently checks that the id conforms to what a
     * MongoDB ObjectId should look like.
     */
    static boolean validId(String id) {
        if (id.matches("[a-zA-Z0-9]{24}")) {
            return true;
        }
        return false;
    }

    // Used for the error json below
    // TODO hopefully we get rid of this and replace with exception mapper
    private static ObjectMapper errorMapper = new ObjectMapper();

    /**
     * Produce a JSON string with an error message, hopefully matching the same standard as what
     * comes out of Massive. Except without the stack trace for the moment.
     */
    static String getErrorJson(Response.Status status, String message) {
        Map<String, Object> errorMap = new HashMap<String, Object>();
        errorMap.put("statusCode", status.getStatusCode());
        errorMap.put("message", message);
        String error;
        try {
            error = errorMapper.writeValueAsString(errorMap);
        } catch (JsonProcessingException e) {
            // Ooer missus, this really shouldn't happen
            throw new WebApplicationException(e);
        }
        return error;
    }

    private static void sanitiseId(String id, ArtefactType typeOfId) throws InvalidIdException {
        if (!validId(id)) {
            throw new InvalidIdException("Invalid " + typeOfId.getValue() + " id");
        }
    }

    /**
     * Retrieve a state action from some json input. The expected json should look like:<br>
     *
     * <pre>
     * {"action":"publish"}
     * </pre>
     *
     * Returns null if the JSON isn't in the required form, or if it contains an invalid action.
     */
    static Asset.StateAction getStateAction(String input) {
        Map<String, String> inputMap = null;
        try {
            inputMap = errorMapper.readValue(input, new TypeReference<Map<String, String>>() {});
        } catch (JsonParseException e) {
            return null;
        } catch (JsonMappingException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        String actionString = inputMap.get("action");
        return Asset.StateAction.forValue(actionString);
    }

    /**
     * Remove URL encoding from both the keys and values of the supplied map. This will deal with
     * both % encoding and + for spaces. Uses URLDecoder ( {@link URLDecoder#decode(String, String)}
     * )
     *
     * @param params
     */
    private static void decodeParams(MultivaluedMap<String, String> params) {
        Map<String, List<String>> copy = new HashMap<String, List<String>>();
        copy.putAll(params);
        params.clear();
        try {
            for (Map.Entry<String, List<String>> entry : copy.entrySet()) {
                List<String> decodedValues = new ArrayList<String>();
                for (String value : entry.getValue()) {
                    decodedValues.add(URLDecoder.decode(value, StandardCharsets.UTF_8.name()));
                }
                params.put(URLDecoder.decode(entry.getKey(), StandardCharsets.UTF_8.name()), decodedValues);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("UTF-8 is unexpectedly missing.", e);
        }
    }

    /**
     * Implementation of {@link StreamingOutput} to put the input stream onto the output stream.
     */
    private static class InputStreamStreamingOutput implements StreamingOutput {
        /**  */
        private final InputStream contentInputStream;

        /**
         * @param contentInputStream
         */
        private InputStreamStreamingOutput(InputStream contentInputStream) {
            this.contentInputStream = contentInputStream;
        }

        @Override
        public void write(OutputStream os) throws IOException {
            try {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = contentInputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            } finally {
                contentInputStream.close();
            }
        }
    }

}
