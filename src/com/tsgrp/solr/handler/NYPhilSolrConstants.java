/*
 * Copyright (C) Technology Services Group, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.tsgrp.com/legal/license
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package com.tsgrp.solr.handler;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.solr.handler.component.SearchHandler;

/**
 * @author Phil MacCart, Technology Services Group
 * @version 1.0
 *
 * <h2>Modification History</h2>
 * <ul>
 *      <li>Feb 7, 2012 (Phil MacCart) Created.</li>
 * </ul>
 * <p>Copyright &copy; 2012 Technology Services Group, Inc.</p>
 */
public interface NYPhilSolrConstants
{
	public static final String NPT_CONTENT_FACET = "npt:content_facet";
	public static final String NPT_CONTENT_FACET_ESC = QueryParser.escape( NPT_CONTENT_FACET );
	
	public static final String NPT_CONTENT = "npt:content";
	public static final String NPT_CONTENT_ESC = QueryParser.escape( NPT_CONTENT );
	
	public static final String NPV_PERSONAL_NAMES_FACET = "npv:PersonalNames_facet";
	public static final String NPV_PERSONAL_NAMES_FACET_ESC = QueryParser.escape( NPV_PERSONAL_NAMES_FACET );
	
	public static final String NPV_PERSONAL_NAMES = "npv:PersonalNames";
	public static final String NPV_PERSONAL_NAMES_ESC = QueryParser.escape( NPV_PERSONAL_NAMES );
	
	public static final String NPT_ASSET_ID = "npt:assetId";
	public static final String NPT_ASSET_ID_ESC = QueryParser.escape( NPT_ASSET_ID );
	
	public static final String NPT_STATUS = "npt:status";
	public static final String NPT_STATUS_ESC = QueryParser.escape( NPT_STATUS );
	public static final String STATUS_APPROVED = "Approved";


}
