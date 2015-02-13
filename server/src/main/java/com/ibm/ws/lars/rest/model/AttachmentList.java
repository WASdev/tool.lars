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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.ws.lars.rest.InvalidJsonAssetException;

/**
 *
 */
public class AttachmentList extends RepositoryObjectList<Attachment> implements Iterable<Attachment> {

    public static AttachmentList createAttachmentListFromMaps(List<Map<String, Object>> state) {
        return new AttachmentList(state);
    }

    private AttachmentList(List<Map<String, Object>> state) {
        super(state);
    }

    public static AttachmentList jsonToAttachmentList(byte[] json) throws InvalidJsonAssetException {
        return new AttachmentList(readJsonState(json));
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<Attachment> iterator() {
        return new AttachmentIterator(state);
    }

    private class AttachmentIterator implements Iterator<Attachment> {
        private final Iterator<Map<String, Object>> stateIterator;

        public AttachmentIterator(List<Map<String, Object>> state) {
            stateIterator = state.iterator();
        }

        @Override
        public boolean hasNext() {
            return stateIterator.hasNext();
        }

        @Override
        public Attachment next() {
            return Attachment.createAttachmentFromMap(stateIterator.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() not supported");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Attachment wrapState(Map<String, Object> props) {
        return Attachment.createAttachmentFromMap(props);
    }
}
