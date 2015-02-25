/*
 * Copyright (C) Technology Services Group, Inc.
 * 
 * Licensed under the Mozilla Public License version 1.1 with a permitted attribution clause. You may obtain a copy of
 * the License at
 * 
 * http://www.tsgrp.com/legal/license
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.tsgrp.solr.handler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.lucene.queryParser.ParseException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.schema.DateField;
import org.apache.solr.search.ExtendedDismaxQParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * By including the {!tag=test} at the start of a FILTER QUERY, the filter query will not affect facets that are prefixed with the {!ex=test} attribute.
 * The {!tag=test} is PURPOSELY left off the filter query that is applied with the date ranges (document type used when no date query exists), and specific type restrictions so the counts are properly updated.
 * 
 * @author gamin, Technology Services Group
 * @version 1.0
 * 
 *          <h2>Modification History</h2>
 *          <ul>
 *          <li>Nov 15, 2010 (gamin) Created.</li>
 *          </ul>
 *          <p>
 *          Copyright &copy; 2010 Technology Services Group, Inc.
 *          </p>
 */
public class NYPhilSearchHandler extends SearchHandler
{

    protected static Logger logger = LoggerFactory.getLogger( NYPhilSearchHandler.class );

    private static final String PARAM_KEYWORDS = "keywords";

    private static final String PARAM_DOCTYPE = "doctype";

    private static final String PARAM_GENERATE_FACETS = "generateFacets";

    private static final String PARAM_PAGE_INDEX = "index";

    private static final String PARAM_RESULTS_PER_PAGE = "resultsPerPage";
    
    private static final String PARAM_FACET_QUERY = "facetQuery";
    
    private static final String PARAM_SUGGESTED_QUERY = "suggestedQuery";
    
    private static final String PARAM_SORT_COLUMN = "sortColumn";
    
    private static final String PARAM_SORT_ORDER = "sortOrder";
    
    private static final String PARAM_DATE_FROM = "dateFrom";
    
    private static final String PARAM_DATE_TO = "dateTo";
    
    
    private SimpleDateFormat DATE_FORMAT_PARAM = new SimpleDateFormat( "yyyyMMdd" );
    
    private DateField solrField = new DateField();

    /**
     * @see org.apache.solr.handler.component.SearchHandler#handleRequestBody(org.apache.solr.request.SolrQueryRequest,
     *      org.apache.solr.request.SolrQueryResponse)
     */
    @Override
    public void handleRequestBody( SolrQueryRequest req, SolrQueryResponse rsp ) throws Exception, ParseException, InstantiationException, IllegalAccessException
    {
        SolrParams requestParams = req.getParams();
        
        NamedList<Object> params = requestParams.toNamedList();
        
        //for now lets echo the handler and all the params for debugging purposes
        //params.add( CommonParams.HEADER_ECHO_HANDLER, Boolean.TRUE );
        //params.add( CommonParams.HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.ALL );
        
        String rows = (String)params.get( CommonParams.ROWS );
        if (rows == null || rows.trim().length() < 1)
        {
            //setup items per page, default to 10 items
            params.add( CommonParams.ROWS, 10 );
            rows = "10";
        }
        
        //enable faceting and only return facets with at least 1 item
        params.add( FacetParams.FACET, Boolean.TRUE );
        params.add( FacetParams.FACET_MINCOUNT, 1 );
        
        //defult ordering to index order (alphabetical)
        params.add( FacetParams.FACET_SORT, FacetParams.FACET_SORT_INDEX);
        
        //only return a maximum of 10 facet values
        params.add( FacetParams.FACET_LIMIT, 10);
        
        //always add facets unless they are explicitly not requested
        String addFacets = (String)params.get( PARAM_GENERATE_FACETS );
        boolean generateFacets = (addFacets == null ) ? true : addFacets.equalsIgnoreCase("true"); 
                
        //enable highlighting
        params.add( HighlightParams.HIGHLIGHT, Boolean.TRUE );

        //remove the query if provided, always want to use our translated query
        String originalQuery = (String)params.remove( CommonParams.Q );
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Original query: " + originalQuery);            
        }
        
        //setup sorting params
        String sortColumn = (String)params.get( PARAM_SORT_COLUMN );
        String sortOrder = (String)params.get( PARAM_SORT_ORDER );
        if (sortColumn != null && sortOrder != null)
        {
            params.add( CommonParams.SORT, sortColumn + " " + sortOrder );
        }
        
        //get date fields
        String sDateFrom = (String)params.get( PARAM_DATE_FROM );
        String sDateTo = (String)params.get( PARAM_DATE_TO );
        Date dateFrom = null;
        Date dateTo = null;
        if (sDateFrom != null && sDateTo != null)
        {
            dateFrom = DATE_FORMAT_PARAM.parse( sDateFrom );
            dateTo = DATE_FORMAT_PARAM.parse(sDateTo);    
        }
        
        String keywords = (String)params.get( PARAM_KEYWORDS );
        
        if (keywords == null || keywords.trim().length() < 1)
        {
            //query everything since nothing was provided
            keywords = "*";
        }
        
        //always use the extended dismax parser
        params.add( "defType", ExtendedDismaxQParserPlugin.NAME );
        
        //the keywords sent in are the query since we're in DISMAX mode
        params.add( CommonParams.Q, keywords );
        
        String pageIndex = (String)params.get( PARAM_PAGE_INDEX );
        String resultsPerPage = (String)params.get( PARAM_RESULTS_PER_PAGE );
        if (resultsPerPage == null || resultsPerPage.trim().length() < 0)
        {
            resultsPerPage = "10";
        }
        
        if (pageIndex == null || pageIndex.trim().length() < 1)
        {
            pageIndex = "1";
        }
        
        //figure out the skip count, use the (pageIndex - 1) * resultsPerPage
        //ie pageIndex = 3, 10 results per page, we'll set start to (3-1)*10 = 20
        int start = (Integer.parseInt( pageIndex ) - 1 ) * Integer.parseInt(resultsPerPage);
        params.add( CommonParams.START, start );
        
        
        String doctype = (String)params.get( PARAM_DOCTYPE );
        if (doctype == null || doctype.trim().length() < 1)
        {
            doctype = "";
        }

        String facetQuery = (String)params.get( PARAM_FACET_QUERY );
        if (facetQuery != null && facetQuery.trim().length() > 0)
        {
            //facetQuery is pre formatted and correct, just add it as a filter query
            params.add( CommonParams.FQ, facetQuery );
        }
        
        String suggestedQuery = (String)params.get( PARAM_SUGGESTED_QUERY );
        if (suggestedQuery != null && suggestedQuery.trim().length() > 0)
        {
            //suggestedQuery is pre formatted and correct, just add it as a filter query
            params.add( CommonParams.FQ, suggestedQuery );
        }
                        
        //base fields
        params.add( DisMaxParams.QF, "nyp:DocumentType" );
        params.add( DisMaxParams.QF, "nyp:Notes");
        
        //program fields
        params.add( DisMaxParams.QF, "npp:ProgramID" );
        params.add( DisMaxParams.QF, "npp:Season" );
        params.add( DisMaxParams.QF, "npp:OrchestraCode" );
        params.add( DisMaxParams.QF, "npp:OrchestraName" );
        params.add( DisMaxParams.QF, "npp:LocationName" );
        params.add( DisMaxParams.QF, "npp:VenueName" );
        params.add( DisMaxParams.QF, "npp:EventTypeName" );
        params.add( DisMaxParams.QF, "npp:SubEventName" );
        params.add( DisMaxParams.QF, "npp:ConductorName" );
        params.add( DisMaxParams.QF, "npp:SoloistsNames" );
        params.add( DisMaxParams.QF, "npp:SoloistsInstrumentName" );
        params.add( DisMaxParams.QF, "npp:WorksComposerNames" );
        params.add( DisMaxParams.QF, "npp:WorksTitle" );
        params.add( DisMaxParams.QF, "npp:WorksShortTitle" );
        params.add( DisMaxParams.QF, "npp:WorksConductorNames" );
        
        //printedMusic fields
        params.add( DisMaxParams.QF, "npm:LibraryID" );
        params.add( DisMaxParams.QF, "npm:ShortTitle" );
        params.add( DisMaxParams.QF, "npm:ComposerName" );
        params.add( DisMaxParams.QF, "npm:PublisherName" );
        params.add( DisMaxParams.QF, "npm:ComposerNameTitle" );
        params.add( DisMaxParams.QF, "npm:ScoreMarkingArtist" );
        params.add( DisMaxParams.QF, "npm:ScoreEditionTypeDesc" );
        params.add( DisMaxParams.QF, "npm:ScoreNotes" );

        //part fields
        params.add( DisMaxParams.QF, "npm:PartTypeDesc" );
        params.add( DisMaxParams.QF, "npm:PartMarkingArtist" );
        params.add( DisMaxParams.QF, "npm:UsedByArtistName" );

        //businessRecord fields
        params.add( DisMaxParams.QF, "npb:BoxNumber" );
        params.add( DisMaxParams.QF, "npb:RecordGroup" );
        params.add( DisMaxParams.QF, "npb:Series" );
        params.add( DisMaxParams.QF, "npb:SubSeries" );
        params.add( DisMaxParams.QF, "npb:Folder" );
        params.add( DisMaxParams.QF, "npb:Names" );
        params.add( DisMaxParams.QF, "npb:Subject" );
        params.add( DisMaxParams.QF, "npb:Abstract" );
        
        //visual fields
        params.add( DisMaxParams.QF, "npv:ID" );
        params.add( DisMaxParams.QF, "npv:BoxNumber" );
        params.add( DisMaxParams.QF, "npv:PhilharmonicSource" );
        params.add( DisMaxParams.QF, "npv:OutsideSource" );
        params.add( DisMaxParams.QF, "npv:Photographer" );
        params.add( DisMaxParams.QF, "npv:CopyrightHolder" );
        params.add( DisMaxParams.QF, "npv:PlaceOfImage" );
        params.add( DisMaxParams.QF, "npv:PersonalNames" );
        params.add( DisMaxParams.QF, "npv:CorporateNames" );
        params.add( DisMaxParams.QF, "npv:Event" );
        params.add( DisMaxParams.QF, "npv:ImageType" );
        params.add( DisMaxParams.QF, "npv:LocationName" );
        params.add( DisMaxParams.QF, "npv:VenueName" );        
        
        //audio fields
        params.add( DisMaxParams.QF, "npa:ProgramID" );
        params.add( DisMaxParams.QF, "npa:Location" );
        params.add( DisMaxParams.QF, "npa:EventTypeName" );
        params.add( DisMaxParams.QF, "npa:ConductorName" );
        params.add( DisMaxParams.QF, "npa:SoloistsAndInstruments" );
        params.add( DisMaxParams.QF, "npa:ComposerWork" );
        params.add( DisMaxParams.QF, "npa:OrchestraName" );
        params.add( DisMaxParams.QF, "npa:IntermissionFeature" );
        params.add( DisMaxParams.QF, "npa:LocationName" );
        params.add( DisMaxParams.QF, "npa:VenueName" );
        params.add( DisMaxParams.QF, "npa:SubEventName" );
        params.add( DisMaxParams.QF, "npa:URLLocation" );
        params.add( DisMaxParams.QF, "npa:IntermissionGuests" );
        params.add( DisMaxParams.QF, "npa:Announcer" );
        
        //video fields
        params.add( DisMaxParams.QF, "npx:ProgramID" );
        params.add( DisMaxParams.QF, "npx:Location" );
        params.add( DisMaxParams.QF, "npx:EventTypeName" );
        params.add( DisMaxParams.QF, "npx:ConductorName" );
        params.add( DisMaxParams.QF, "npx:SoloistsAndInstruments" );
        params.add( DisMaxParams.QF, "npx:ComposerNameWork" );
        params.add( DisMaxParams.QF, "npx:OrchestraName" );
        params.add( DisMaxParams.QF, "npx:IntermissionFeature" );
        params.add( DisMaxParams.QF, "npx:LocationName" );
        params.add( DisMaxParams.QF, "npx:VenueName" );
        params.add( DisMaxParams.QF, "npx:SubEventName" );
        params.add( DisMaxParams.QF, "npx:IntermissionGuests" );
        params.add( DisMaxParams.QF, "npx:Announcer" );
        
        params.add( DisMaxParams.QF, "npt:tagged" );
        
        //add in all the restrictions that can be applied to a document type count in the same filter query
        
        //this includes all DATE range queries
        //if a date query does not exist, the TYPE is queried so all results are shown
        //restriction is applied to business records so only the web publishable items are shown
        StringBuffer filterTypesCountsQuery = new StringBuffer();
        
        if (dateFrom != null && dateTo != null)
        {
            //program date query
            filterTypesCountsQuery.append("(npp\\:Date:[" + solrField.toExternal( dateFrom ) + " TO " + solrField.toExternal( dateTo ) + "])");
            
            //printedMusic (scores) and parts have no date range, so just or in the type so those results come back
            filterTypesCountsQuery.append(" OR (nyp\\:DocumentType:Printed Music)");
            filterTypesCountsQuery.append(" OR (nyp\\:DocumentType:Part)");

            //business record range AND web publishable restriction
            filterTypesCountsQuery.append(" OR (nyp\\:WebPublishable:true AND ((npb\\:DateFrom:[" + solrField.toExternal( dateFrom ) + " TO *] AND npb\\:DateTo:[* TO " + solrField.toExternal( dateTo ) + "])");
            filterTypesCountsQuery.append(" OR (npb\\:DateFrom:[* TO " + solrField.toExternal( dateFrom ) + "] AND npb\\:DateTo:[* TO " + solrField.toExternal( dateTo ) + "] AND npb\\:DateTo:[" + solrField.toExternal( dateFrom ) + " TO *])");
            filterTypesCountsQuery.append(" OR (npb\\:DateFrom:[" + solrField.toExternal( dateFrom ) + " TO *] AND npb\\:DateTo:[" + solrField.toExternal( dateTo ) + " TO *] AND npb\\:DateFrom:[* TO " + solrField.toExternal( dateFrom ) + "])))"); 
            
            //visual
            filterTypesCountsQuery.append(" OR ((npv\\:DateFrom:[" + solrField.toExternal( dateFrom ) + " TO *] AND npv\\:DateTo:[* TO " + solrField.toExternal( dateTo ) + "])");
            filterTypesCountsQuery.append(" OR (npv\\:DateFrom:[* TO " + solrField.toExternal( dateFrom ) + "] AND npv\\:DateTo:[* TO " + solrField.toExternal( dateTo ) + "] AND npv\\:DateTo:[" + solrField.toExternal( dateFrom ) + " TO *])");
            filterTypesCountsQuery.append(" OR (npv\\:DateFrom:[" + solrField.toExternal( dateFrom ) + " TO *] AND npv\\:DateTo:[" + solrField.toExternal( dateTo ) + " TO *] AND npv\\:DateFrom:[* TO " + solrField.toExternal( dateFrom ) + "]))");
            
            //audio
            filterTypesCountsQuery.append(" OR (npa\\:Date:[" + solrField.toExternal( dateFrom ) + " TO " + solrField.toExternal( dateTo ) + "])");
            
            //video
            filterTypesCountsQuery.append(" OR (npx\\:Date:[" + solrField.toExternal( dateFrom ) + " TO " + solrField.toExternal( dateTo ) + "])");
        }
        else
        {
            //no date queries, just perform query based on type restrictions            
            filterTypesCountsQuery.append("(nyp\\:DocumentType:Program) OR (nyp\\:DocumentType:Printed Music)  OR (nyp\\:DocumentType:Part) OR (nyp\\:DocumentType:Business Record AND nyp\\:WebPublishable:true)");
            filterTypesCountsQuery.append(" OR (nyp\\:DocumentType:Visual) OR (nyp\\:DocumentType:Audio) OR (nyp\\:DocumentType:Video)");
        }
        
        params.add( CommonParams.FQ, filterTypesCountsQuery );
        
        
        //default facet for document type that will always return the counts regardless of filter queries applied
        params.add( FacetParams.FACET_FIELD, "{!ex=test}nyp:DocumentType_facet" );
        //allow this facet to always display all values, even when there are 0 results for the type
        params.add( "f.nyp:DocumentType_facet.facet.mincount", 0);

        if (doctype.equalsIgnoreCase( "program" ))
        {             
            //set the document type restriction
            params.add( CommonParams.FQ, "{!tag=test}nyp\\:DocumentType:Program" );
            
            if (generateFacets)
            {
                params.add( FacetParams.FACET_FIELD, "npp:ConductorName_facet" );
                params.add( FacetParams.FACET_FIELD, "npp:SoloistsNames_facet" );
                params.add( FacetParams.FACET_FIELD, "npp:WorksComposerNames_facet" );
                params.add( FacetParams.FACET_FIELD, "npp:LocationName_facet" );
                params.add( FacetParams.FACET_FIELD, "npp:VenueName_facet" );
                params.add( FacetParams.FACET_FIELD, "npp:EventTypeName_facet" );
                params.add( FacetParams.FACET_FIELD, "npp:Season_facet" );
            }
        }
        else if (doctype.equalsIgnoreCase( "printedMusic" ))
        {
            //set the document type restriction
            params.add( CommonParams.FQ, "{!tag=test}nyp\\:DocumentType:Printed Music" );
            
            if (generateFacets)
            {
                params.add( FacetParams.FACET_FIELD, "npm:ScoreMarkingArtist_facet" );
                params.add( FacetParams.FACET_FIELD, "npm:ComposerName_facet" );
            }

        }
        else if (doctype.equalsIgnoreCase( "part" ))
        {
            //set the document type restriction
            params.add( CommonParams.FQ, "{!tag=test}nyp\\:DocumentType:Part" );

            if (generateFacets)
            {
                params.add( FacetParams.FACET_FIELD, "npm:ComposerName_facet");
                params.add( FacetParams.FACET_FIELD, "npm:UsedByArtistName_facet");
                params.add( FacetParams.FACET_FIELD, "npm:PartMarkingArtist_facet");
                params.add( FacetParams.FACET_FIELD, "npm:PartTypeDesc_facet");
            }

            // if sort parameter has been supplied (e.g. non-facet search) - apply additional part sorting
            if (sortColumn != null && sortOrder != null) {
                logger.debug("Addition additional sort parameter for PART type...");
                String prevParams = (String)params.get(CommonParams.SORT);
                String newParams = prevParams + ", npm:PartID ASC";
                params.add(CommonParams.SORT, newParams);
            }

        }
        else if (doctype.equalsIgnoreCase("businessRecord"))
        {
            //set the document type restriction
            params.add( CommonParams.FQ, "{!tag=test}nyp\\:DocumentType:Business Record" );
            
            if (generateFacets)
            {
                params.add( FacetParams.FACET_FIELD, "npb:Names_facet" );
                params.add( FacetParams.FACET_FIELD, "npb:Subject_facet" );
                params.add( FacetParams.FACET_FIELD, "npb:RecordGroup_facet" );
                params.add( FacetParams.FACET_FIELD, "npb:Series_facet" );
                params.add( FacetParams.FACET_FIELD, "npb:SubSeries_facet" );
            }
            
        }
        else if (doctype.equalsIgnoreCase("visual"))
        {
            //set the document type restriction
            params.add( CommonParams.FQ, "{!tag=test}nyp\\:DocumentType:Visual" );
            
            if (generateFacets)
            {
                params.add( FacetParams.FACET_FIELD, "npv:Photographer_facet" );
                params.add( FacetParams.FACET_FIELD, "npv:CopyrightHolder_facet" );
                params.add( FacetParams.FACET_FIELD, "npv:ImageType_facet" );
                params.add( FacetParams.FACET_FIELD, "npv:PlaceOfImage_facet" );
                params.add( FacetParams.FACET_FIELD, "npv:Event_facet" );
                params.add( FacetParams.FACET_FIELD, "npv:PersonalNames_facet" );
                params.add( FacetParams.FACET_FIELD, "npv:LocationName_facet" );
                params.add( FacetParams.FACET_FIELD, "npv:VenueName_facet" );
            }
                        
        }
        else if (doctype.equalsIgnoreCase("audio"))
        {
            //set the document type restriction
            params.add( CommonParams.FQ, "{!tag=test}nyp\\:DocumentType:Audio" );
            
            //no facets being generated
        }
        else if (doctype.equalsIgnoreCase("video"))
        {
            //set the document type restriction
            params.add( CommonParams.FQ, "{!tag=test}nyp\\:DocumentType:Video" );
            
            //no facets being generated
        }
        else
        {
            logger.error("Invalid document type: " + doctype);
            throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "Invalid document type: " + doctype );
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Params: " + params);            
        }

        //set the new request parameters, then call the default handler behavior
        req.setParams( SolrParams.toSolrParams( params ) );
        
        super.handleRequestBody( req, rsp );
    }

}
