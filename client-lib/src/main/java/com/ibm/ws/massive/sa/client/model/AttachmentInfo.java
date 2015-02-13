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

package com.ibm.ws.massive.sa.client.model;

public class AttachmentInfo extends AbstractJSON {
    private long _CRC;
    private ImageDetails imageDetails;

    public void setCRC(long crc) {
        this._CRC = crc;
    }

    public long getCRC() {
        return this._CRC;
    }

    public void setImageDetails(ImageDetails details) {
        this.imageDetails = details;
    }

    public ImageDetails getImageDetails() {
        return imageDetails;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (_CRC ^ (_CRC >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        // We know it's an AttachmentInfo or equivalent would have failed and we wont reach this point
        AttachmentInfo other = (AttachmentInfo) obj;
        if (_CRC != other._CRC)
            return false;

        if (imageDetails == null) {
            if (other.imageDetails != null)
                return false;
        } else if (!imageDetails.equals(other.imageDetails))
            return false;

        return true;
    }

}
