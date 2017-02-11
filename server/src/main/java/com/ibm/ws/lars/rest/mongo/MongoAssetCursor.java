/*******************************************************************************
 * Copyright (c) 2017 IBM Corp.
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
package com.ibm.ws.lars.rest.mongo;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.lars.rest.model.Asset;
import com.ibm.ws.lars.rest.model.AssetCursor;
import com.ibm.ws.lars.rest.model.AssetOperation;
import com.mongodb.DBCursor;

/**
 * An {@link AssetCursor} implementation which streams Assets from a Mongo {@link DBCursor}.
 */
public class MongoAssetCursor implements AssetCursor {

    private final DBCursor cursor;
    private final List<AssetOperation> operations = new ArrayList<>();

    public MongoAssetCursor(DBCursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public boolean hasNext() {
        return cursor.hasNext();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Asset next() {
        Asset next = Asset.createAssetFromMap(cursor.next().toMap());
        for (AssetOperation op : operations) {
            op.perform(next);
        }
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return cursor.size();
    }

    @Override
    public void addOperation(AssetOperation op) {
        operations.add(op);
    }

}
