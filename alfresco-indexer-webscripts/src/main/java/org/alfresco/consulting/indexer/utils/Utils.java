/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.manifoldcf.integration.alfresco.indexer.utils;

import java.util.Iterator;

import org.alfresco.service.cmr.repository.Path;

public class Utils
{
    
    public static String getSiteName(Path path) {
        //Fetching Path and preparing for rendering
        Iterator<Path.Element> pathIter = path.iterator();

        //Scan the Path to find the Alfresco Site name
        boolean siteFound = false;
        while(pathIter.hasNext()) {
          String pathElement = pathIter.next().getElementString();
          //Stripping out namespace from PathElement
          int firstChar = pathElement.lastIndexOf('}');
          if (firstChar > 0) {
            pathElement = pathElement.substring(firstChar+1);
          }
          if (pathElement.equals("sites")) {
            siteFound = true;
          } else if (siteFound) {
            return pathElement;
          }
        }
        return null;
      }

}
