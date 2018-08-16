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

package com.baasbox.dao.business;

import java.util.List;

import com.baasbox.dao.NodeDao;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.databean.VehicleBean;
import com.baasbox.service.storage.FileService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class VehicleDao extends NodeDao  {
	public final static String MODEL_NAME="_BB_Vehicle";
	public final static String YEAR_FIELD_NAME="year";
	public final static String MAKE_FIELD_NAME="make";
	public static final String MODEL_FIELD_NAME="model";
	public static final String EXTERIOR_COLOR_FIELD_NAME="exteriorColor";
	public static final String LICENSE_PLATE_FIELD_NAME="licensePlate";
	public static final String CAPACITY_FIELD_NAME = "capacity";
	public static final String VEHICLE_PICTURE_FILE_ID_FIELD_NAME = "vehiclePictureFileId";
	public static final String INSPECTION_PICTURE_FIELD_NAME = "inspectionFormPicture";
	
	protected VehicleDao(String modelName) {
		super(modelName);
	}
	
	public static VehicleDao getInstance(){
		return new VehicleDao(MODEL_NAME);
	}
	
	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name, String fileName, String contentType, byte[] content) instead");
	}
	
	public ODocument create(VehicleBean v) throws Throwable {
		
		 ODocument inspectionFormFile = FileService.getById(v.getInspectionFormPicture());

		if (inspectionFormFile == null)
			throw new FileNotFoundException("File id pointed to by vehicle.inspectionFormPicture doesn't exist: " + v.getInspectionFormPicture());
		
		ODocument vehicle = super.create();
		vehicle.field(VehicleDao.EXTERIOR_COLOR_FIELD_NAME, v.getExteriorColor());
		vehicle.field(VehicleDao.LICENSE_PLATE_FIELD_NAME, v.getLicensePlate());
		vehicle.field(VehicleDao.CAPACITY_FIELD_NAME, v.getCapacity());
		vehicle.field(VehicleDao.MODEL_FIELD_NAME, v.getModel());
		vehicle.field(VehicleDao.MAKE_FIELD_NAME, v.getMake());
		vehicle.field(VehicleDao.YEAR_FIELD_NAME, v.getYear());
		vehicle.field(VehicleDao.INSPECTION_PICTURE_FIELD_NAME, inspectionFormFile);
		return vehicle;
	}
	
	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}

	public ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
		QueryParams criteria=QueryParams.getInstance().where("id=?").params(new String[]{id});
		List<ODocument> listOfVehicles = this.get(criteria);
		if (listOfVehicles==null || listOfVehicles.size()==0) return null;
		ODocument doc=listOfVehicles.get(0);
		try{
			checkModelDocument((ODocument)doc);
		}catch(InvalidModelException e){
			//the id may reference a ORecordBytes which is not a ODocument
			throw new InvalidModelException("the id " + id + " is not a vehicle " + VehicleDao.MODEL_NAME);
		}
		return doc;
	}	
}
