/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.qos.logback.classic.db.DBHelper;

import com.baasbox.BBCache;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UpdateOldVersionException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.command.OCommandManager;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.OSQLHelper;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;


public abstract class NodeDao  {

	public final String MODEL_NAME;
	public static final String CLASS_NODE_NAME = "_BB_Node";
	public static final String CLASS_VERTEX_NAME = "_BB_NodeVertex";
	
	public static final String FIELD_LINK_TO_VERTEX = "_links";

	public static final String FIELD_TO_DOCUMENT_FIELD = "_node";

  public static final String LINKS_QUERY_FORMAT = "(select expand(_links.%s(\"%s\")._node) from %s where id = '%s')";

	public static final String FIELD_CREATION_DATE = BaasBoxPrivateFields.CREATION_DATE.toString();
	
	public static final String EDGE_CLASS_CREATED = "Created";
	

	protected ODatabaseRecordTx db;

	public static void updateAuthor(String oldAuthor,String newAuthor){
		Object command = DbHelper.genericSQLStatementExecute(
				"update _bb_node set _author=? where _author=?", new String[]{newAuthor,oldAuthor});
	}
	 
  protected NodeDao(String modelName) {
		super();
		this.MODEL_NAME=modelName;
		this.db=DbHelper.getConnection();
	}

	
	public static ODocument removeClassAndRid(ODocument document){
		document.removeField("@class");
		document.removeField("@rid");
		return document;
	}
	public ObjectNode removeClassAndRid(ObjectNode document) {
		document.remove("@class");
		document.remove("@rid");
		return document;
	}
	
	
	protected static HashMap<String,Object> backupBaasBoxFields(ODocument document){
		HashMap<String,Object> map = new HashMap<String,Object>();
		for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
			map.put(r.toString(), document.field(r.toString()));
		}
		return map;
	}
	
	
	protected static ODocument restoreBaasBoxFields(ODocument document, HashMap<String,Object> map){
		for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
			document.fields(r.toString(),map.get(r.toString()));
		}
		return document;
	}
	

	public void checkModelDocument(ODocument doc) throws InvalidModelException {
		if (doc!=null && !doc.getClassName().equalsIgnoreCase(this.MODEL_NAME))
			throw new InvalidModelException();
	}

	public Integer updateByQuery(String query) throws InvalidCriteriaException{
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Update query: " + query);
		OCommandRequest command = db.command(new OCommandSQL(
				query
				));
		Integer records=null;
		try{
			records=DbHelper.sqlCommandExecute(command, null);
		}catch (OQueryParsingException e ){
			throw new InvalidCriteriaException("Invalid criteria. Please check if your querystring is encoded in a corrected way. Double check the single-quote and the quote characters",e);
		}catch (OCommandSQLParsingException e){
			throw new InvalidCriteriaException(e);
		}
		return records;
	}
	
	public List<ODocument> selectByQuery(String query) throws InvalidCriteriaException{
		List<ODocument> list=null;
		try{
			list = DbHelper.commandExecute(new OSQLSynchQuery<ODocument>(
				query
				), null);
		}catch (OQueryParsingException e ){
			throw new InvalidCriteriaException("Invalid criteria. Please check if your querystring is encoded in a corrected way. Double check the single-quote and the quote characters",e);
		}catch (OCommandSQLParsingException e){
			throw new InvalidCriteriaException(e);
		}	
		return list;
	}

	public ODocument create() throws Throwable {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		OrientGraph db = DbHelper.getOrientGraphConnection();
		try{
				DbHelper.requestTransaction();
				ODocument doc = new ODocument(this.MODEL_NAME);
				ODocument vertex = db.addVertex("class:" + CLASS_VERTEX_NAME,FIELD_TO_DOCUMENT_FIELD,doc).getRecord();
				doc.field(FIELD_LINK_TO_VERTEX,vertex);
				doc.field(FIELD_CREATION_DATE,new Date());
				UUID token = UUID.randomUUID();
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CreateUUID.onRecordBeforeCreate: " + doc.getIdentity() + " -->> " + token.toString());
				doc.field(BaasBoxPrivateFields.ID.toString(),token.toString());
				doc.field(BaasBoxPrivateFields.AUTHOR.toString(),db.getRawGraph().getUser().getName());
				DbHelper.commitTransaction();
				return doc;
		}catch (Throwable e){
			DbHelper.rollbackTransaction();
			throw e;
		}finally{
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		}
	}
	


	protected  void save(ODocument document) throws InvalidModelException {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		checkModelDocument(document);
		document.save();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	}
	

	public void update(ODocument originalDocument, ODocument documentToMerge) throws UpdateOldVersionException  {
		if (documentToMerge.getVersion()!=0 && documentToMerge.getVersion()!=originalDocument.getVersion()) throw new UpdateOldVersionException("The document to merge is older than the stored one v" +documentToMerge.getVersion() + " vs v"+documentToMerge.getVersion(),documentToMerge.getVersion(), originalDocument.getVersion());
		//backup the baasbox's fields 
		HashMap<String,Object> map = backupBaasBoxFields(originalDocument);
		//remove backupBaasBoxFields from data that will be merged
		documentToMerge=removeClassAndRid(documentToMerge);
		//update the document
		originalDocument.merge(documentToMerge, false, false);
		//restore the baasbox's fields
		restoreBaasBoxFields(originalDocument, map);
		originalDocument.save();
	}
	

	public Object explainQuery(QueryParams criteria) throws SqlInjectionException{
		try{
			OCommandRequest command = DbHelper.genericSQLStatementCommandBuilder("explain " + DbHelper.selectQueryBuilder(MODEL_NAME, false, criteria));
			Object toRet = DbHelper.genericSQLCommandExecute(command, criteria.getParams());
			return toRet;
		}catch (OCommandExecutionException e ){
			throw new InvalidCriteriaException("Invalid criteria. Please check if your querystring is encoded in a corrected way. Double check the single-quote and the quote characters",e);
		}catch (OQueryParsingException e ){
			throw new InvalidCriteriaException("Invalid criteria. Please check if your querystring is encoded in a corrected way. Double check the single-quote and the quote characters",e);
		}catch (OCommandSQLParsingException e){
			throw new InvalidCriteriaException(e);
		}catch (StringIndexOutOfBoundsException e){
			throw new InvalidCriteriaException("Invalid criteria. Please check your query, the syntax and the parameters",e);
		}catch (IndexOutOfBoundsException e){
			throw new InvalidCriteriaException("Invalid criteria. Please check your query, the syntax and the parameters",e);
		}
	}
	
	public List<ODocument> get(QueryParams criteria) throws SqlInjectionException, InvalidCriteriaException {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		List<ODocument> result = null;
		OCommandRequest command = DbHelper.selectCommandBuilder(MODEL_NAME, false, criteria);
		try{
			result = DbHelper.selectCommandExecute(command, criteria.getParams());
		}catch (OCommandExecutionException e ){
			throw new InvalidCriteriaException("Invalid criteria. Please check if your querystring is encoded in a corrected way. Double check the single-quote and the quote characters",e);
			
		}catch (OQueryParsingException e ){
			throw new InvalidCriteriaException("Invalid criteria. Please check if your querystring is encoded in a corrected way. Double check the single-quote and the quote characters",e);
		}catch (OCommandSQLParsingException e){
			throw new InvalidCriteriaException(e);
		}catch (StringIndexOutOfBoundsException e){
			throw new InvalidCriteriaException("Invalid criteria. Please check your query, the syntax and the parameters",e);
		}catch (IndexOutOfBoundsException e){
			throw new InvalidCriteriaException("Invalid criteria. Please check your query, the syntax and the parameters",e);
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return result;
	}


	public ODocument get(ORID rid) throws InvalidModelException, DocumentNotFoundException {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Object doc=db.load(rid);
		if (doc==null) throw new DocumentNotFoundException();
		if (!(doc instanceof ODocument)) throw new IllegalArgumentException(rid +" is a rid not referencing a valid Document");
		try{
			checkModelDocument((ODocument)doc);
		}catch(InvalidModelException e){
			//the rid may reference a ORecordBytes which is not a ODocument
			throw new InvalidModelException("the rid " + rid + " is not valid belong to the collection " + this.MODEL_NAME);
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return (ODocument)doc;
	}


	public ODocument get(String rid) throws InvalidModelException, ODatabaseException, DocumentNotFoundException {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Object orid=OSQLHelper.parseValue(rid, null);
		if ((orid==null) || !(orid instanceof ORecordId) || (orid.toString().equals(OSQLHelper.VALUE_NOT_PARSED))) throw new IllegalArgumentException(rid +" is not a valid rid");
		Object odoc=get((ORecordId)orid);
		
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return (ODocument)odoc;
	}
 

	public boolean exists(ODocument document) throws InvalidModelException, DocumentNotFoundException {
		return exists(document.getRecord().getIdentity());
	}


	public boolean exists(ORID rid) throws InvalidModelException, DocumentNotFoundException {
		ODocument doc = get(rid);
		return (doc!=null);
	}


	public boolean exists(String rid) throws InvalidModelException, ODatabaseException, DocumentNotFoundException {
		ODocument doc = get(rid);
		return (doc!=null);
	}

	public ODocument revokePermission(ODocument document, Permissions permission, OUser user) {
		return PermissionsHelper.revoke(document, permission, user); 
	}

	public ODocument revokePermission(ODocument document, Permissions permission, ORole role) {
		return PermissionsHelper.revoke(document, permission, role);
	}
	
	public ODocument grantPermission(ODocument document, Permissions permission, OUser user) {
		return PermissionsHelper.grant(document, permission, user);
		 
	}

	public ODocument grantPermission(ODocument document, Permissions permission, ORole role) {
		return PermissionsHelper.grant(document, permission, role);
	}
	
	public ODocument grantPermission(ODocument document, Permissions permission, ORole[] roles) {
		for (ORole role : roles){
			grantPermission(document, permission, role);
		}
		return document;
	}

	
	public long getCount(){
		return DbHelper.getODatabaseDocumentTxConnection().countClass(MODEL_NAME);
	}
	
	public long getCount(QueryParams criteria) throws SqlInjectionException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		List<ODocument> result = null;
		OCommandRequest command = DbHelper.selectCommandBuilder(MODEL_NAME, true, criteria);
		try{
			result = DbHelper.selectCommandExecute(command, criteria.getParams());
		}catch (OCommandExecutionException e ){
			throw new InvalidCriteriaException("Invalid criteria. Please check if your querystring is encoded in a corrected way. Double check the single-quote and the quote characters",e);
			
		}catch (OQueryParsingException e ){
			throw new InvalidCriteriaException("Invalid criteria. Please check if your querystring is encoded in a corrected way. Double check the single-quote and the quote characters",e);
		}catch (OCommandSQLParsingException e){
			throw new InvalidCriteriaException(e);
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ((Long)result.get(0).field("count")).longValue();
	}
	
	
	public void delete(String rid) throws Throwable{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		ODocument doc = get(rid);
		delete(doc.getIdentity());
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	}
	
	public void delete(ORID rid) throws Throwable{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		OrientGraph db = DbHelper.getOrientGraphConnection();
		//retrieve the vertex associated to this node
		try{
			DbHelper.requestTransaction();
			OrientVertex vertex = db.getVertex(((ODocument)db.getRawGraph().load(rid))
														.field(FIELD_LINK_TO_VERTEX));
			db.removeVertex(vertex);
			db.getRawGraph().delete(rid);
			DbHelper.commitTransaction();
		}catch (Throwable e){
			DbHelper.rollbackTransaction();
			throw e;
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	}

  public static void deleteVerticesByAuthor(OUser user) throws Throwable {
    deleteByQuery("delete vertex " + CLASS_VERTEX_NAME + " where node._author=?", new Object[] {user.getName()});
  }

  public static void deleteDocumentsByAuthor(OUser user) throws Throwable {
    deleteByQuery("delete from " + CLASS_NODE_NAME + " where _author = ?", new Object[] {user.getName()});
  }

  private static void deleteByQuery(String query, Object[] params) throws Throwable {
    if (BaasBoxLogger.isTraceEnabled())
      BaasBoxLogger.trace("Method Start");
    try {
      DbHelper.requestTransaction();
      DbHelper.sqlCommandExecute(new OCommandSQL(
        query), params);
      DbHelper.commitTransaction();
      if (BaasBoxLogger.isTraceEnabled())
        BaasBoxLogger.trace("Method End");
    } catch (Throwable e) {
      DbHelper.rollbackTransaction();
      throw e;
    }
  }

  public void deleteNodesByAuthor(OUser user) throws Throwable {

  }
	
	
}