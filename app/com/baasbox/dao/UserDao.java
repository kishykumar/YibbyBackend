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

import java.security.InvalidParameterException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.OpenTransactionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.sociallogin.UserInfo;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.metadata.security.OUser.STATUSES;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;






public class UserDao extends NodeDao  {

	public final static String MODEL_NAME="_BB_User";
	public final static String USER_LINK = "user";
	private final static String USER_NAME_INDEX = "ouser.name";

	public final static String USER_PUSH_TOKEN="pushToken";
	public final static String USER_DEVICE_OS="os";
	public final static String USER_LOGIN_INFO="login_info";
	public final static String SOCIAL_LOGIN_INFO="sso_tokens";

	public final static String USER_ATTRIBUTES_CLASS = "_BB_UserAttributes";
	public final static String ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER="visibleByAnonymousUsers";
	public final static String ATTRIBUTES_VISIBLE_BY_REGISTERED_USER="visibleByRegisteredUsers";
	public final static String ATTRIBUTES_VISIBLE_BY_FRIENDS_USER="visibleByFriends";
	public final static String ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER="visibleByTheUser";
	public final static String USER_SIGNUP_DATE="signUpDate";
	public final static String ATTRIBUTES_SYSTEM="system";
	public static final String GENERATED_USERNAME = "generated_username";
  private static final String USERNAME_PLACEHOLDER = "<USERNAME_PLACEHOLDER>";
  protected static final String GET_FOLLOWING_FROM_FORMAT = "(select from _bb_user where user.name in (select name.substring(11) from (select expand(roles) from ouser where name = '"
    + USERNAME_PLACEHOLDER + "') where name like 'friends_of_%'))";

	public static UserDao getInstance(){
		return new UserDao();
	}

	protected UserDao() {
		super(MODEL_NAME);
	}

	@Override
	@Deprecated
	public ODocument create(){
		throw new IllegalAccessError("To create a new user call create(String username, String password) or create(String username, String password, String role)");
	}

	public ODocument create(String username, String password) throws UserAlreadyExistsException {
		return create(username, password, null);
	};


	public ODocument create(String username, String password, String role) throws UserAlreadyExistsException {
		OrientGraph db = DbHelper.getOrientGraphConnection();
		if (existsUserName(username)) throw new UserAlreadyExistsException("Error signing up");
		OUser user=null;
		if (role==null) user=db.getRawGraph().getMetadata().getSecurity().createUser(username,password,new String[]{DefaultRoles.REGISTERED_USER.toString()});
		else {
			ORole orole = RoleDao.getRole(role);
			if (orole==null) throw new InvalidParameterException("Role " + role + " does not exists");
			user=db.getRawGraph().getMetadata().getSecurity().createUser(username,password,new String[]{role}); 
		}
		
    ODocument doc = new ODocument(UserDao.MODEL_NAME);
		ODocument vertex = db.addVertex("class:"+CLASS_VERTEX_NAME,FIELD_TO_DOCUMENT_FIELD,doc).getRecord();
		doc.field(FIELD_LINK_TO_VERTEX,vertex);
		doc.field(FIELD_CREATION_DATE,new Date());

		doc.field(USER_LINK,user.getDocument().getIdentity());
		//since 0.9.4 each username has also an id
		doc.field(BaasBoxPrivateFields.ID.toString(),UUID.randomUUID().toString());
		
		doc.save();
		return doc;
	}

  public void delete(OUser user) throws Throwable {
	  try{
		  delete(getByUserName(user.getName()).getIdentity());
		  db.getMetadata().getSecurity().dropUser(user.getName());
	  }catch(Throwable e){
		 throw e;
	  }
  }

	public boolean existsUserName(String username){
		OIndex idx = db.getMetadata().getIndexManager().getIndex(USER_NAME_INDEX);
		OIdentifiable record = (OIdentifiable) idx.get( username );
		return (record!=null);
	}

	public ODocument getByUserName(String username) throws SqlInjectionException{
		ODocument result=null;
		QueryParams criteria = QueryParams.getInstance().where("user.name=?").params(new String [] {username});
		List<ODocument> resultList= super.get(criteria);
		if (resultList!=null && resultList.size()>0) result = resultList.get(0);
		return result;
	}
	
	public OUser getOUserByUsername(String username){
		ODocument user = null;
		try {
			user = getByUserName(username);
			if (user==null) return null;
		} catch (SqlInjectionException e) {
			BaasBoxLogger.debug(ExceptionUtils.getMessage(e));
			throw new RuntimeException(e);
		}
		return new OUser((ODocument)user.field("user"));	
	}

    public List<ODocument> getByUsernames(List<String> usernames,QueryParams query) throws SqlInjectionException {
        if (query == null){
            query = QueryParams.getInstance().where("user.name in ?").params(new Object[]{usernames});
        } else {
            String where = query.getWhere();
            if (where==null|| where.isEmpty()){
                query = QueryParams.getInstance().where("user.name in ?").params(new Object[]{usernames});
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(where).append(" AND ( user.name in ? )");
                Object[] newParams;
                Object[] params = query.getParams();
                if (params == null){
                    newParams = new Object[]{usernames};
                } else {
                    newParams = new Object[params.length+1];
                    System.arraycopy(params,0,newParams,0,params.length);
                    newParams[newParams.length-1]=new Object[]{usernames};
                }
                query.where(sb.toString());
                query.params(newParams);
            }
        }
        List<ODocument> resultList = super.get(query);
        return resultList;
    }

	public List<ODocument> getByUsernames(List<String> usernames) throws SqlInjectionException{
		QueryParams criteria = QueryParams.getInstance().where("user.name in ?").params(new Object [] {usernames});
		List<ODocument> resultList= super.get(criteria);
		
		return resultList;
	}

	public ODocument getBySocialUserId(UserInfo ui) throws SqlInjectionException{
		ODocument result=null;
		StringBuffer where = new StringBuffer(UserDao.ATTRIBUTES_SYSTEM).append(".");
		where.append(UserDao.SOCIAL_LOGIN_INFO).append("[").append(ui.getFrom()).append("]").append(".id").append(" = ?");
		QueryParams criteria = QueryParams.getInstance().where(where.toString()).params(new String [] {ui.getId()});
		List<ODocument> resultList= super.get(criteria);
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Found "+resultList.size() +" elements for given tokens");
		if (resultList!=null && resultList.size()>0) result = resultList.get(0);

		return result;
	}

	public void disableUser(String username) throws UserNotFoundException, OpenTransactionException{
		db = DbHelper.reconnectAsAdmin();
		OUser user = getOUserByUsername(username);
		if (user==null) throw new UserNotFoundException("The user " + username + " does not exist.");
		user.setAccountStatus(STATUSES.SUSPENDED);
		user.save();
		//cannot resume the old connection because now the user is disabled
	}
	
	public void enableUser(String username) throws UserNotFoundException, OpenTransactionException{
		db = DbHelper.reconnectAsAdmin();
		OUser user = getOUserByUsername(username);
		if (user==null) throw new UserNotFoundException("The user " + username + " does not exist.");
		user.setAccountStatus(STATUSES.ACTIVE);
		user.save();
	}

  public static String getFollowingSelectQuery(String username, QueryParams criteria) {
    String queryString = StringUtils.replace(GET_FOLLOWING_FROM_FORMAT, USERNAME_PLACEHOLDER, username);
    return DbHelper.selectQueryBuilder(queryString, criteria.justCountTheRecords(), criteria);

  }

  public List<ODocument> getFollowing(String username, QueryParams criteria) {
    return (List<ODocument>) DbHelper.genericSQLStatementExecute(getFollowingSelectQuery(username, criteria), criteria.getParams());

  }

	public void changeUsername(String currentUsername, String newUsername) throws UserNotFoundException {
		if (existsUserName(currentUsername)){
			Object command = DbHelper.genericSQLStatementExecute("update ouser set name=? where name=?", new String[]{newUsername,currentUsername});
		}else throw new UserNotFoundException(currentUsername + " does not exists");
	}

	public boolean emailIsAlreadyUsed(String email) {
		String selectStatement="select from _bb_user where visibleByTheUser.email= ?";
		List<ODocument> ret=(List<ODocument>)DbHelper.genericSQLStatementExecute(selectStatement, new String[]{email});
		if (ret.size()!=0) return true;
		return false;
	}

}
