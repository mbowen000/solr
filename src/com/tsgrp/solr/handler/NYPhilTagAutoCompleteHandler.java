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

import java.util.regex.Pattern;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
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
public class NYPhilTagAutoCompleteHandler extends SearchHandler 
{
	private static final Logger logger = LoggerFactory.getLogger( NYPhilTagAutoCompleteHandler.class );
	
	private static final String PARAM_VALUE = "value";
	private static final String PARAM_CALLBACK = "callback";
	
	// regex used to remove all non-alphanumeric chars from the query input so we can 
	// still use wildcard searches without need to use an edge n-gram filter factory
	private static final Pattern QUERY_TERM_REGEX =	Pattern.compile( "[\\W]" );
	
	
	/*
	 * @see org.apache.solr.handler.component.SearchHandler#handleRequestBody(org.apache.solr.request.SolrQueryRequest, org.apache.solr.request.SolrQueryResponse)
	 */
	@Override
	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse res)
			throws Exception, ParseException, InstantiationException,
			IllegalAccessException 
	{
		NamedList<Object> params = req.getParams().toNamedList();
		
		params.add( CommonParams.ROWS, 0 );
		params.add( FacetParams.FACET, true );
		params.add( FacetParams.FACET_FIELD, NYPhilSolrConstants.NPT_CONTENT_FACET );
		params.add( FacetParams.FACET_MINCOUNT, 1 );
		params.add( FacetParams.FACET_SORT, FacetParams.FACET_SORT_INDEX );
		params.add( CommonParams.HEADER_ECHO_PARAMS, "explicit" );
		params.add( CommonParams.WT, "json" );
		params.add( "json.nl", "map" );
		
		String query = (String) params.get( PARAM_VALUE );
		if( query == null || query.length() == 0 ) {
			query = "*";
		}
		else
		{
			query = QueryParser.escape( query.toLowerCase() );
		}
		
		String [] queryTerms = query.split( " " );
		
		// wrap our query term in parentheses to allow searching on terms separated by whitespace
		StringBuffer q = new StringBuffer();
		
		// for each query term, require the term with a trailing wildcard match
		for( String queryTerm : queryTerms ) {
			
			// remove all non-alphanumeric chars from the query term
			queryTerm = QUERY_TERM_REGEX.matcher( queryTerm.toLowerCase() ).replaceAll( "" );
			q.append("+").append( NYPhilSolrConstants.NPT_CONTENT_ESC )
					.append( ":" ).append( QueryParser.escape( queryTerm) ).append( "* ");
		}
		q.append("+" ).append( NYPhilSolrConstants.NPT_STATUS_ESC )
				.append( ":" ).append( NYPhilSolrConstants.STATUS_APPROVED );
		
		params.add( CommonParams.Q, q.toString() );
		
		if( logger.isDebugEnabled() ) {
			logger.debug( "Autocomplete Query: " + q.toString() );
		}
		
		String cb = (String) params.get( PARAM_CALLBACK );
		if( cb != null && cb.length() > 0 ) {
			params.add( "json.wrf", cb );
		}
		
        req.setParams( SolrParams.toSolrParams( params ) );
        
        super.handleRequestBody( req, res );
	}
	

}
