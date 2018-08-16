/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
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

package com.baasbox.service.business;

import java.util.List;

import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.business.VehicleDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.VehicleBean;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.VehicleNotFoundException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.user.CaberService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class VehicleService {

		public static ODocument createVehicle(VehicleBean vehicle) throws Throwable {
			VehicleDao dao = VehicleDao.getInstance();
			
			// Create the database object for a vehicle
			ODocument doc = dao.create(vehicle);

			dao.save(doc);
			return doc;
		}

		public static VehicleBean getVehicleBeanFromODocument(ODocument vehicleDoc) {
		    VehicleBean vehicleBean = new VehicleBean();
		    vehicleBean.setCapacity(vehicleDoc.field(VehicleDao.CAPACITY_FIELD_NAME));
            vehicleBean.setExteriorColor(vehicleDoc.field(VehicleDao.EXTERIOR_COLOR_FIELD_NAME));
            
            ODocument vehicleInspectionPicture = vehicleDoc.field(VehicleDao.INSPECTION_PICTURE_FIELD_NAME);
            if (vehicleInspectionPicture != null) {
                vehicleBean.setInspectionFormPicture((String)vehicleInspectionPicture.field(BaasBoxPrivateFields.ID.toString()));
            }
            
            vehicleBean.setLicensePlate(vehicleDoc.field(VehicleDao.LICENSE_PLATE_FIELD_NAME));
            vehicleBean.setMake(vehicleDoc.field(VehicleDao.MAKE_FIELD_NAME));
            vehicleBean.setModel(vehicleDoc.field(VehicleDao.MODEL_FIELD_NAME));
            vehicleBean.setYear(vehicleDoc.field(VehicleDao.YEAR_FIELD_NAME));
		    return vehicleBean; 
		}
		
		public static ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
			VehicleDao dao = VehicleDao.getInstance();
			return dao.getById(id);
		}

		public static void deleteById(String id) throws Throwable{
			VehicleDao dao = VehicleDao.getInstance();
			ODocument vehicle=getById(id);
			if (vehicle==null) throw new VehicleNotFoundException();
			dao.delete(vehicle.getIdentity());
		}

		public static List<ODocument> getVehicles(QueryParams criteria) throws SqlInjectionException {
			VehicleDao dao = VehicleDao.getInstance();
			return dao.get(criteria); 
		}
		
		public static ODocument grantPermissionToCaber(String id, Permissions permission, String username) throws UserNotFoundException, RoleNotFoundException, SqlInjectionException, 
		IllegalArgumentException, InvalidModelException, VehicleNotFoundException {
			OUser user = CaberService.getOUserByUsername(username);
			
			if (user == null) {
				throw new UserNotFoundException(username);
			}
			
			ODocument doc = getById(id);
			if (doc == null) { 
				throw new VehicleNotFoundException(id);
			}
			
			return PermissionsHelper.grant(doc, permission, user);
		}
}
