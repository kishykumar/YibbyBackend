/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
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
 */

package com.baasbox.commands;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.controllers.Document;
import com.baasbox.controllers.actions.exceptions.RidNotFoundException;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UpdateOldVersionException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.AclNotValidException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.util.BBJson;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {resource: 'documents',
 *  name: <'get'|'list'|'post'|'put'|'delete',
 *  params; {
 *      collection: <collection>,
 *
 *      query: <query>,*
 *
 *      id: <id>*,
 *
 *      grants: {}
 *  }}
 *
 *
 *  {resource: 'documents',
 *   name: 'get',
 *   params: {
 *       collection: <collection>,
 *       id: <uuid>,
 *
 *   }}
 *
 *
 *  {resource: 'documents',
 *   name: 'grant',
 *   params: {
 *       collection: <collection>,
 *       id: <uuid>,
 *       users: {read: [,,,],
 *               update: [,,,,],
 *               delete: [....],
 *               all: [,,,,],
 *               },
 *       roles: {read: [,,,,],
 *               update: [,,,,],
 *               delete: [,,,,,],
 *               all: [,,,,,]
 *   }}
 *
 *
 *
 * Created by Andrea Tortorella on 30/06/14.
 */
class DocumentsResource extends BaseRestResource {
	public static final Resource INSTANCE = new DocumentsResource();

	private static final String RESOURCE_NAME = "documents";

	private static final String COLLECTION = "collection";
	private static final String QUERY = "query";
	private static final String DATA = "data";
	private static final String AUTHOR = "author";

	@Override
	protected ImmutableMap.Builder<String, ScriptCommand> baseCommands() {
		return super.baseCommands().put("grant", new ScriptCommand() {
			@Override
			public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
				return grant(command,true);
			}
		}).put("revoke", new ScriptCommand() {
			@Override
			public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
				return grant(command,false);
			}
		});
	}

	private JsonNode grant(JsonNode command, boolean grant) throws CommandException {
		validateHasParams(command);
		String coll = getCollectionName(command);
		String id = getDocumentId(command);

		try {
			try {
				String rid = DocumentService.getRidByString(id, true);
				alterGrants(command, coll, rid, true, grant);
				alterGrants(command, coll, rid, false, grant);
			} catch (Exception e){
				BaasBoxLogger.error("error",e);
				throw  e;
			}
		} catch (UserNotFoundException e) {

			throw new CommandExecutionException(command,"user not found exception");
		} catch (DocumentNotFoundException e) {
			throw new CommandExecutionException(command,"document not found exception");
		} catch (InvalidCollectionException e) {
			throw new CommandExecutionException(command,"invalid colleciton exception");
		} catch (InvalidModelException e) {
			throw new CommandExecutionException(command,"invalid model exception");
		} catch (RoleNotFoundException e) {
			throw new CommandExecutionException(command,"role not found exception");
		} catch (RidNotFoundException e) {
			throw new CommandExecutionException(command,"document "+id+" not found");
		}
		return BooleanNode.getTrue();
	}

	@Override
	protected JsonNode delete(JsonNode command) throws CommandException {
		validateHasParams(command);
		String coll = getCollectionName(command);
		String id = getDocumentId(command);
		String rid;
		try {
			rid = DocumentService.getRidByString(id,true);
		} catch (RidNotFoundException e) {
			return null;
		}
		try {
			DocumentService.delete(coll,rid);
		} catch (OSecurityException e) {
			throw new CommandExecutionException(command, "you don't have permissions to delete: "+id);
		} catch (ODatabaseException e){
			return null;
		} catch (Throwable e){
			throw new CommandExecutionException(command,"error executing delete command on "+id+ " message: "+ExceptionUtils.getMessage(e));
		}
		return null;
	}


	private String getCollectionName(JsonNode command) throws CommandParsingException {
		JsonNode params = command.get(ScriptCommand.PARAMS);
		JsonNode collNode = params.get(COLLECTION);
		if (collNode == null|| !collNode.isTextual()){
			throw new CommandParsingException(command,"invalid collection param: "+(collNode==null?"null":collNode.toString()));
		}
		return collNode.asText();
	}


	@Override
	protected JsonNode put(JsonNode command) throws CommandException{
		validateHasParams(command);
		String coll = getCollectionName(command);
		ObjectNode data = (ObjectNode) getData(command);
		String id = getDocumentId(command);
		try {
			String rid=null;
			try{
				rid = DocumentService.getRidByString(id, true);
			} catch (RidNotFoundException e) {
				//swallow
			}	
			ODocument doc=null;
			if (rid!=null) //update
				doc = DocumentService.update(coll, rid, data);
			else {// save
				data.put("id", id);
				doc = DocumentService.create(coll, data);
			}
			String json = JSONFormats.prepareDocToJson(doc, JSONFormats.Formats.DOCUMENT_PUBLIC);
			ObjectNode node = (ObjectNode) BBJson.mapper().readTree(json);
			node.remove(TO_REMOVE);
			//node.remove("@rid");
			return node;
		} catch (RidNotFoundException e) {
			throw new CommandExecutionException(command,"document: "+id+" does not exists");
		} catch (UpdateOldVersionException e) {
			throw new CommandExecutionException(command,"document: "+id+" has a more recent version");
		} catch (DocumentNotFoundException e) {
			throw new CommandExecutionException(command,"document: "+id+" does not exists");
		} catch (InvalidCollectionException e) {
			throw new CommandExecutionException(command,"invalid collection: "+coll);
		} catch (InvalidModelException e) {
			throw new CommandExecutionException(command,"error updating document (is the provided ID belonging to the provided collection?): "+id+" message: "+ExceptionUtils.getMessage(e));
		} catch (JsonProcessingException e) {
			throw new CommandExecutionException(command,"data do not represents a valid document, message: "+ExceptionUtils.getMessage(e));
		} catch (IOException e) {
			throw new CommandExecutionException(command,"error updating document: "+id+" message:"+ExceptionUtils.getMessage(e));
		} catch (AclNotValidException e) {
			throw new CommandExecutionException(command,"error updating document (check the ACL fields): "+id+" message:"+ExceptionUtils.getMessage(e));
		} catch (Throwable e){
			BaasBoxLogger.error("error updating document: "+id,e);
			throw new CommandExecutionException(command," error updating document: "+id+" message:"+ExceptionUtils.getMessage(e));
		}
	}

	@Override
	protected JsonNode post(JsonNode command) throws CommandException{
		validateHasParams(command);
		String collection = getCollectionName(command);
		JsonNode data = getData(command);
		String authorOverride = getAuthorOverride(command);

		try {
			ODocument doc =
					DocumentService.create(collection, (ObjectNode)data);
			//                    DocumentService.createOnBehalf(collection,authorOverride,data);
			if (doc == null){
				return null;
			}
			String fmt = JSONFormats.prepareDocToJson(doc, JSONFormats.Formats.DOCUMENT_PUBLIC);
			JsonNode node = BBJson.mapper().readTree(fmt);
			ObjectNode n =(ObjectNode)node;
			n.remove(TO_REMOVE);
			//            n.remove("@rid");
			return n;
		} catch (InvalidCollectionException throwable) {
			throw new CommandExecutionException(command,"invalid collection: "+collection);
		} catch (InvalidModelException e) {
			throw new CommandExecutionException(command,"error creating document: "+ExceptionUtils.getMessage(e));
		} catch (Throwable e) {
			throw new CommandExecutionException(command,"error creating document: "+ExceptionUtils.getMessage(e));
		}
	}

	private String getAuthorOverride(JsonNode command) throws CommandParsingException{
		JsonNode node = command.get(ScriptCommand.PARAMS).get(AUTHOR);
		if (node != null && !node.isTextual()){
			throw new CommandParsingException(command,"author must be a string");

		} else if (node != null){
			return node.asText();
		}
		return null;
	}

	private JsonNode getData(JsonNode command) throws CommandParsingException {
		JsonNode node = command.get(ScriptCommand.PARAMS).get(DATA);
		if (node == null||(!node.isObject())){
			throw new CommandParsingException(command,"missing required data parameter");
		}
		return node;
	}

	@Override
	protected JsonNode list(JsonNode command) throws CommandException {
		String collection= getCollectionName(command);
		QueryParams params = QueryParams.getParamsFromJson(command.get(ScriptCommand.PARAMS).get(QUERY));
		String fetchPlan = super.getFetchPlan(command);
		try {
			List<ODocument> docs = DocumentService.getDocuments(collection, params);

			String usableFetchPlan = (fetchPlan==null) ? JSONFormats.Formats.DOCUMENT_PUBLIC.toString() : fetchPlan;
			BaasBoxLogger.debug("FetchPlan: " + usableFetchPlan);
			
			DbHelper.filterOUserPasswords(true);
			String s = JSONFormats.prepareDocToJson(docs, usableFetchPlan);
			ArrayNode lst = (ArrayNode) BBJson.mapper().readTree(s);
			if (fetchPlan==null) lst.forEach((j)->((ObjectNode)j).remove(TO_REMOVE));
			return lst;
		} catch (SqlInjectionException | IOException e) {
			throw new CommandExecutionException(command,"error executing command: "+ExceptionUtils.getMessage(e),e);
		} catch (InvalidCollectionException e) {
			throw new CommandExecutionException(command,"invalid collection: "+collection,e);
		} finally {
			DbHelper.filterOUserPasswords(false);
		}
	}




	@Override
	protected JsonNode get(JsonNode command) throws CommandException{
		String collection = getCollectionName(command);
		String id = getDocumentId(command);
		LinkQuery lq = getLinkQuery(command);
		String fetchPlan = super.getFetchPlan(command);
		String usableFetchPlan = (fetchPlan==null) ? JSONFormats.Formats.DOCUMENT_PUBLIC.toString() : fetchPlan;
		try {
			String rid = DocumentService.getRidByString(id, true);
			if(lq!=null){
				QueryParams qp = QueryParams.getParamsFromJson(command.get(ScriptCommand.PARAMS).get(ScriptCommand.LINKS));

				List<ODocument> documents =  DocumentService.queryLink(collection, id, lq.linkName, Document.LinkDirection.map(lq.linkDir),qp);
				JsonNode node = (JsonNode)BBJson.mapper().createArrayNode();
				
				DbHelper.filterOUserPasswords(true);
				if(documents.size()>0){
					String s = JSONFormats.prepareDocToJson(documents, usableFetchPlan);
					node = BBJson.mapper().readTree(s);
					if (fetchPlan==null) node.forEach(n->{
						((ObjectNode)n).remove(TO_REMOVE);
					});

				}
				return node;
			}else{
				ODocument document = DocumentService.get(collection, rid);
				if (document == null){
					return null;
				} else {
					String s = JSONFormats.prepareDocToJson(document, usableFetchPlan);
					ObjectNode node = (ObjectNode) BBJson.mapper().readTree(s);
					if (fetchPlan==null) node.remove(TO_REMOVE);
					return node;
				}
			}
		} catch (RidNotFoundException e) {
			return null;
		} catch (DocumentNotFoundException e) {
			return null;
		} catch (InvalidCollectionException e) {
			throw new CommandExecutionException(command,"invalid collection: "+collection);
		} catch (InvalidModelException e) {
			throw new CommandExecutionException(command,"error executing command: "+ExceptionUtils.getMessage(e));
		} catch (JsonProcessingException e) {
			throw new CommandExecutionException(command,"error executing command: "+ExceptionUtils.getMessage(e));
		} catch (IOException e) {
			throw new CommandExecutionException(command,"error executing command: "+ExceptionUtils.getMessage(e));
		} finally {
			DbHelper.filterOUserPasswords(false);
		}
	}

	private LinkQuery getLinkQuery(JsonNode command) throws CommandParsingException {
		LinkQuery lq = null;
		JsonNode params = command.get(ScriptCommand.PARAMS);
		if(params.has(ScriptCommand.LINKS)){
			try{
				lq = BBJson.mapper().treeToValue(params.get(ScriptCommand.LINKS), LinkQuery.class);
				if (!lq.linkDir.matches(Document.LinkDirection.regexp())) {
					throw new CommandParsingException(params,"linkDir param must contain one of the following values: to(default),from or both");
				}
			}catch(IOException ioe){
				throw new CommandParsingException(command,"Unable to parse links from provided command:"+ExceptionUtils.getMessage(ioe));
			}
		}
		return lq;
	}

	private void alterGrants(JsonNode command,String collection,String docId,boolean users,boolean grant ) throws CommandParsingException, UserNotFoundException, DocumentNotFoundException, InvalidCollectionException, InvalidModelException, RoleNotFoundException {
		JsonNode params = command.get(ScriptCommand.PARAMS);
		JsonNode node = users?params.get("users"):params.get("roles");
		if (node != null){
			JsonNode read = node.get("read");
			if (read!=null){
				alterGrantsTo(command, read, collection, docId, grant, users, Permissions.ALLOW_READ);
			}

			//DEPRECATED
			JsonNode write = node.get("write");
			if (write!=null){
				alterGrantsTo(command,write,collection,docId,grant,users,Permissions.ALLOW_UPDATE);
			}

			//issue 682 - field to grant/revoke update permission is "write" instead of "update" into the plugin engine
			//we now have to maintain both due retro-compatibility 
			JsonNode update = node.get("update");
			if (update!=null){
				alterGrantsTo(command,update,collection,docId,grant,users,Permissions.ALLOW_UPDATE);
			}
			//------

			JsonNode delete = node.get("delete");
			if (delete!=null){
				alterGrantsTo(command,delete,collection,docId,grant,users,Permissions.ALLOW_DELETE);
			}
			JsonNode all= node.get("all");
			if (all!=null){
				alterGrantsTo(command,all,collection,docId,grant,users,Permissions.ALLOW);
			}

		}
	}

	private void alterGrantsTo(JsonNode command, JsonNode to, String collection, String docId, boolean isGrant,boolean users, Permissions permission) throws CommandParsingException, UserNotFoundException, DocumentNotFoundException, InvalidCollectionException, InvalidModelException, RoleNotFoundException {
		if (!to.isArray()) throw new CommandParsingException(command,"targets of permissions must be an array");
		if (isGrant){
			if (users){
				for (JsonNode u: to){
					if (!u.isTextual()) throw new CommandParsingException(command,"invalid user name specified: "+u);
					DocumentService.grantPermissionToUser(collection,docId,permission,u.asText());
				}
			} else {
				for (JsonNode r:to){
					if (!r.isTextual()) throw new CommandParsingException(command,"invalid role name specified: "+r);
					DocumentService.grantPermissionToRole(collection,docId,permission,r.asText());
				}
			}
		} else {
			if (users){
				for (JsonNode u: to){
					if (!u.isTextual()) throw new CommandParsingException(command,"invalid user name specified: "+u);
					DocumentService.revokePermissionToUser(collection, docId, permission, u.asText());
				}
			} else {
				for (JsonNode r:to){
					if (!r.isTextual()) throw new CommandParsingException(command,"invalid role name specified: "+r);
					DocumentService.revokePermissionToRole(collection, docId, permission, r.asText());
				}
			}
		}
	}

	private String getDocumentId(JsonNode command) throws CommandException{
		JsonNode params = command.get(ScriptCommand.PARAMS);
		JsonNode id = params.get("id");
		if (id==null||!id.isTextual()){
			throw new CommandParsingException(command,"missing document id");
		}
		String idString = id.asText();
		return idString;
	}
	
	@Override
	public String name() {
		return RESOURCE_NAME;
	}

	@JsonIgnoreProperties(ignoreUnknown=true)
	static class LinkQuery {
		@JsonProperty("linkName")
		String linkName;
		@JsonProperty("linkDir")
		String linkDir;

		//For json serialization
		public LinkQuery(){}
	}
}
