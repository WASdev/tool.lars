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

package com.ibm.ws.massive.resources;

public class ImageDetails {

    private final com.ibm.ws.massive.sa.client.model.ImageDetails _ImageDetails;

    public ImageDetails() {
        _ImageDetails = new com.ibm.ws.massive.sa.client.model.ImageDetails();
    }

    public ImageDetails(com.ibm.ws.massive.sa.client.model.ImageDetails id) {
        _ImageDetails = id;
    }

    public int getWidth() {
        return _ImageDetails.getWidth();
    }

    public void setWidth(int width) {
        _ImageDetails.setWidth(width);
    }

    public int getHeight() {
        return _ImageDetails.getHeight();
    }

    public void setHeight(int height) {
        _ImageDetails.setHeight(height);
    }

    com.ibm.ws.massive.sa.client.model.ImageDetails getImageDetails() {
        return _ImageDetails;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((_ImageDetails == null) ? 0 : _ImageDetails.hashCode());
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
        ImageDetails other = (ImageDetails) obj;
        if (_ImageDetails == null) {
            if (other._ImageDetails != null)
                return false;
        } else if (!_ImageDetails.equals(other._ImageDetails))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return _ImageDetails.toString();
    }

}
