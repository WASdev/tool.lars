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

package com.ibm.ws.lars.rest.model;

import java.io.InputStream;
import java.util.Map;

import com.ibm.ws.lars.rest.exceptions.InvalidJsonAssetException;

/**
 *
 */
public class Attachment extends RepositoryObject {

    public Attachment() {
        super();
    }

    /**
     * Wrap an existing map of properties in an attachment object.
     * <p>
     * Changes to the attachment will be reflected in the original map.
     *
     * @param state the map of properties
     */
    public Attachment(Map<String, Object> state) {
        super(state);
    }

    /**
     * Copy another attachment, creating a new copy of the internal set of properties.
     *
     * @param toCopy the attachment to clone
     */
    public Attachment(Attachment toCopy) {
        super(toCopy);
    }

    public enum LinkType {

        DIRECT("direct"),
        WEB_PAGE("web_page");

        String linkType;

        private LinkType(String linkType) {
            this.linkType = linkType;
        }

        public String getValue() {
            return linkType;
        }

        public static LinkType forValue(String value) {
            for (LinkType type : LinkType.values()) {
                if (type.getValue().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return null;
        }

    }

    public static final String ASSET_ID = "assetId";
    public static final String CONTENT_TYPE = "contentType";
    public static final String GRIDFS_ID = "gridFSId";
    public static final String LOCALE = "locale";
    public static final String NAME = "name";
    public static final String SIZE = "size";
    public static final String TYPE = "type";
    public static final String UPLOAD_ON = "uploadOn";
    public static final String URL = "url";
    public static final String LINK_TYPE = "linkType";

    public static Attachment jsonToAttachment(String json) throws InvalidJsonAssetException {
        return new Attachment(readJsonState(json));
    }

    public static Attachment jsonToAttachment(byte[] json) throws InvalidJsonAssetException {
        return new Attachment(readJsonState(json));
    }

    public static Attachment jsonToAttachment(InputStream json) throws InvalidJsonAssetException {
        return new Attachment(readJsonState(json));
    }

    public static Attachment createAttachmentFromMap(Map<String, Object> state) {
        return new Attachment(state);
    }

    public void setAssetId(String assetId) {
        put(ASSET_ID, assetId);
    }

    public String getAssetId() {
        return get(ASSET_ID);
    }

    public void setContentType(String contentType) {
        put(CONTENT_TYPE, contentType);
    }

    public String getContentType() {
        return get(CONTENT_TYPE);
    }

    public void setGridFSId(String gridFSId) {
        put(GRIDFS_ID, gridFSId);
    }

    public String getGridFSId() {
        return get(GRIDFS_ID);
    }

    public void setName(String name) {
        put(NAME, name);
    }

    public String getName() {
        return get(NAME);
    }

    public void setSize(long size) {
        put(SIZE, size);
    }

    public long getSize() {
        Object size = get(SIZE);
        if (size instanceof Integer) {
            int sizeInt = (Integer) size;
            return sizeInt; // automatically cast to long
        } else {
            return get(SIZE);
        }
    }

    public void setType(String type) {
        put(TYPE, type);
    }

    public String getType() {
        return get(TYPE);
    }

    // perhaps this should end up being a Date or a Calendar
    public void setUploadOn(String uploadOn) {
        put(UPLOAD_ON, uploadOn);
    }

    // ditto
    public String getUploadOn() {
        return get(UPLOAD_ON);
    }

    public void setUrl(String url) {
        put(URL, url);
    }

    public String getUrl() {
        return get(URL);
    }

    public String getLinkType() {
        return get(LINK_TYPE);
    }

    public void setLinkType(String type) {
        put(LINK_TYPE, type);
    }
}
