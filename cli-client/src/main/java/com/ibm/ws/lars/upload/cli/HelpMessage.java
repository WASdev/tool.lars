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

package com.ibm.ws.lars.upload.cli;

public interface HelpMessage {

    /**
     * @return a one-line help message summary for this command.
     */
    String getHelpSummary();

    /**
     * @return a usage message. Note that this should not include the String "Usage:" or the command
     *         name
     */
    String getUsage();

    /**
     * @return a detailed help message or null if no detailed help message is appropriate
     */
    String getHelpDetail();

}
