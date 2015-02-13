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

import java.util.List;

public class AssetInformation extends AbstractJSON {

    private List<Language> languages;
    private List<PlatformRequirement> platformrequirements;
    private List<Platform> platforms;
    private String size;

    public enum Language {
        JS
    }

    public enum PlatformRequirement {
        WINDOWS, ECLIPSE
    }

    public enum Platform {
        ANDROID, IOS
    }

    public List<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }

    public List<PlatformRequirement> getPlatformrequirements() {
        return platformrequirements;
    }

    public void setPlatformrequirements(
                                        List<PlatformRequirement> platformrequirements) {
        this.platformrequirements = platformrequirements;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((languages == null) ? 0 : languages.hashCode());
        result = prime
                 * result
                 + ((platformrequirements == null) ? 0 : platformrequirements
                                 .hashCode());
        result = prime * result
                 + ((platforms == null) ? 0 : platforms.hashCode());
        result = prime * result + ((size == null) ? 0 : size.hashCode());
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
        AssetInformation other = (AssetInformation) obj;
        if (languages == null) {
            if (other.languages != null)
                return false;
        } else if (!languages.equals(other.languages))
            return false;
        if (platformrequirements == null) {
            if (other.platformrequirements != null)
                return false;
        } else if (!platformrequirements.equals(other.platformrequirements))
            return false;
        if (platforms == null) {
            if (other.platforms != null)
                return false;
        } else if (!platforms.equals(other.platforms))
            return false;
        if (size == null) {
            if (other.size != null)
                return false;
        } else if (!size.equals(other.size))
            return false;
        return true;
    }

}
