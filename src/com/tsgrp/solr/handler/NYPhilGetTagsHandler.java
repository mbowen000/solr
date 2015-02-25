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

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class NYPhilGetTagsHandler extends SearchHandler 
{
	private static final Logger logger = LoggerFactory.getLogger( NYPhilGetTagsHandler.class );
	
	private static final String PARAM_ASSET_ID = "assetId";
	private static final String PARAM_ALL_TAGS = "allTags";
	private static final String PARAM_CALLBACK = "callback";

	/*
	 * @see org.apache.solr.handler.component.SearchHandler#handleRequestBody(org.apache.solr.request.SolrQueryRequest, org.apache.solr.request.SolrQueryResponse)
	 */
	@Override
	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse res)
			throws Exception, ParseException, InstantiationException,
			IllegalAccessException 
	{
		NamedList<Object> params = req.getParams().toNamedList();
		
		String assetId = (String) params.get( PARAM_ASSET_ID );
		if( assetId == null || assetId.trim().length() < 0 ) {
			throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "Asset Id required to retrieve tags." );
		}
		
		boolean allTags = ( params.get( PARAM_ALL_TAGS ) != null && 
				Boolean.parseBoolean( (String) params.get( PARAM_ALL_TAGS ) ) );
		
		StringBuffer q = new StringBuffer( "+" ).append(NYPhilSolrConstants.NPT_ASSET_ID_ESC )
				.append( ":" ).append( QueryParser.escape( assetId ) );
		
		if( !allTags ) {
			q.append( " +" ).append( NYPhilSolrConstants.NPT_STATUS_ESC ).append( ":" ).append( NYPhilSolrConstants.STATUS_APPROVED );
		}
		
		String cb = (String) params.get( PARAM_CALLBACK );
		if( cb != null && cb.length() > 0 ) {
			params.add( "json.wrf", cb );
		}
		
		params.add( CommonParams.HEADER_ECHO_PARAMS, "explicit" );
		params.add( CommonParams.WT, "json" );
		params.add( "json.nl", "map" );
		
		params.add( CommonParams.Q, q.toString() );
		params.add( CommonParams.ROWS, 1000 );
		
        req.setParams( SolrParams.toSolrParams( params ) );
        
        super.handleRequestBody( req, res );
	}

}
