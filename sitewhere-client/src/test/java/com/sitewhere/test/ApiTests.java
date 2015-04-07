/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.sitewhere.rest.client.SiteWhereClient;
import com.sitewhere.rest.model.common.Location;
import com.sitewhere.rest.model.device.Device;
import com.sitewhere.rest.model.device.DeviceAssignment;
import com.sitewhere.rest.model.device.Zone;
import com.sitewhere.rest.model.device.batch.BatchOperation;
import com.sitewhere.rest.model.device.request.DeviceAssignmentCreateRequest;
import com.sitewhere.rest.model.device.request.DeviceCreateRequest;
import com.sitewhere.rest.model.device.request.ZoneCreateRequest;
import com.sitewhere.rest.model.search.SearchResults;
import com.sitewhere.rest.model.system.Version;
import com.sitewhere.spi.ISiteWhereClient;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.SiteWhereSystemException;
import com.sitewhere.spi.device.DeviceAssignmentType;
import com.sitewhere.spi.error.ErrorCode;

/**
 * Test cases for client API calls.
 * 
 * @author dadams
 */
@SuppressWarnings("deprecation")
public class ApiTests {

	/** Device specification id used in tests */
	public static final String TEST_SPECIFICATION_TOKEN = "293749827342243827349";

	/** Hardware id used for test cases */
	public static final String TEST_HARDWARE_ID = "12356789-TEST-123";

	/** Asset id for testing */
	public static final String TEST_ASSET_ID = "174";

	/** Site token used in tests */
	public static final String TEST_SITE_TOKEN = "22223793-3028-4114-86ba-aefc7d05369f";

	/** Android device specification token */
	public static final String ANDROID_SPEC_TOKEN = "d2604433-e4eb-419b-97c7-88efe9b2cd41";

	/** SiteWhere client */
	private ISiteWhereClient client;

	@Before
	public void setup() {
		this.client = new SiteWhereClient();
	}

	@Test
	public void testConnectivity() throws SiteWhereException {
		SiteWhereClient client = new SiteWhereClient();
		Version version = client.getSiteWhereVersion();
		System.out.println("SiteWhere version is " + version.getVersionIdentifier() + ".");
	}

	@Test
	public void testDeviceCRUD() throws SiteWhereException {
		// Delete device if it already exists.
		try {
			client.getDeviceByHardwareId(TEST_HARDWARE_ID);
			client.deleteDevice(TEST_HARDWARE_ID, true);
		} catch (SiteWhereException e) {
			// Ignore missing device since we wanted it deleted.
		}

		// Test initial create.
		DeviceCreateRequest request = new DeviceCreateRequest();
		request.setHardwareId(TEST_HARDWARE_ID);
		request.setSpecificationToken(TEST_SPECIFICATION_TOKEN);
		request.setComments("This is a test device.");
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("name1", "value1");
		metadata.put("name2", "value2");
		request.setMetadata(metadata);
		Device device = client.createDevice(request);
		Assert.assertNotNull("Device create returned null.", device);
		Assert.assertEquals("Metadata not stored properly.", 2, device.getMetadata().size());

		// Test get by hardware id.
		try {
			device = client.getDeviceByHardwareId(TEST_HARDWARE_ID);
		} catch (SiteWhereException e) {
			Assert.fail("Device should exist, but not found by handware id.");
		}

		// Test update.
		DeviceCreateRequest update = new DeviceCreateRequest();
		update.setComments("Updated.");
		metadata = new HashMap<String, String>();
		metadata.put("name1", "value1");
		update.setMetadata(metadata);
		device = client.updateDevice(TEST_HARDWARE_ID, update);
		Assert.assertEquals("Updated.", device.getComments());
		Assert.assertEquals("Metadata not updated properly.", 1, device.getMetadata().size());
		Assert.assertNotNull("Updated date not set.", device.getUpdatedDate());

		// Should not allow hardware id to be updated.
		try {
			update = new DeviceCreateRequest();
			update.setHardwareId("xxx");
			client.updateDevice(TEST_HARDWARE_ID, update);
			Assert.fail("Device update allowed update of hardware id.");
		} catch (SiteWhereSystemException e) {
			verifyErrorCode(e, ErrorCode.DeviceHardwareIdCanNotBeChanged);
		}

		// Test duplicate.
		try {
			device = client.createDevice(request);
			Assert.fail("Create device allowed duplicate.");
		} catch (SiteWhereException e) {
			verifyErrorCode(e, ErrorCode.DuplicateHardwareId);
		}

		// Create a device assignment.
		DeviceAssignmentCreateRequest assnRequest = new DeviceAssignmentCreateRequest();
		assnRequest.setAssignmentType(DeviceAssignmentType.Associated);
		assnRequest.setAssetModuleId("testAssetModuleId");
		assnRequest.setAssetId(TEST_ASSET_ID);
		assnRequest.setDeviceHardwareId(device.getHardwareId());
		metadata = new HashMap<String, String>();
		metadata.put("name1", "value1");
		metadata.put("name2", "value2");
		assnRequest.setMetadata(metadata);
		DeviceAssignment assignment = client.createDeviceAssignment(assnRequest);
		Assert.assertNotNull("Assignment token was null.", assignment.getToken());
		Assert.assertEquals("Assignment metadata count incorrect.", 2, assignment.getMetadata().size());

		// Test get assignment by token.
		assignment = client.getDeviceAssignmentByToken(assignment.getToken());
		Assert.assertNotNull("Assignment by token returned null.", assignment);

		// Test getting current assignment for a device.
		DeviceAssignment currAssignment = client.getCurrentAssignmentForDevice(TEST_HARDWARE_ID);
		Assert.assertEquals("Current device assignment is incorrect.", assignment.getToken(),
				currAssignment.getToken());

		// Verify that an assignment can not be created for a device if one is already
		// assigned.
		try {
			assignment = client.createDeviceAssignment(assnRequest);
		} catch (SiteWhereException e) {
			verifyErrorCode(e, ErrorCode.DeviceAlreadyAssigned);
		}

		// Delete device.
		device = client.deleteDevice(TEST_HARDWARE_ID, true);
		Assert.assertNotNull(device);
	}

	@Test
	public void testCreateZone() throws SiteWhereException {
		SiteWhereClient client = new SiteWhereClient();
		ZoneCreateRequest request = new ZoneCreateRequest();
		request.setName("My Test Zone");
		List<Location> coords = new ArrayList<Location>();
		coords.add(new Location(30.0, -85.0));
		coords.add(new Location(30.0, -90.0));
		coords.add(new Location(35.0, -90.0));
		coords.add(new Location(35.0, -85.0));
		request.setCoordinates(coords);
		Zone results = client.createZone(TEST_SITE_TOKEN, request);
		System.out.println("Created zone: " + results.getName());
		SearchResults<Zone> search = client.listZonesForSite(TEST_SITE_TOKEN);
		System.out.println("Found " + search.getNumResults() + " results.");
	}

	@Test
	public void testListDevices() throws SiteWhereException {
		SiteWhereClient client = new SiteWhereClient();
		SearchResults<Device> devices = client.listDevices(false, true, true, true, 1, 100, null, null);
		System.out.println("Found " + devices.getNumResults() + " devices.");
	}

	@Test
	public void sendBatchCommandInvocation() throws SiteWhereException {
		SiteWhereClient client = new SiteWhereClient();
		List<Device> androids = getDevicesForSpecification(ANDROID_SPEC_TOKEN);
		List<String> hwids = new ArrayList<String>();
		for (Device device : androids) {
			hwids.add(device.getHardwareId());
		}
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("color", "#ff0000");
		BatchOperation op =
				client.createBatchCommandInvocation(null, "17340bb1-8673-4fc9-8ed0-4f818acedaa5", parameters,
						hwids);
		System.out.println("Created operation: " + op.getToken());
	}

	/**
	 * Get all devices for a given specification. NOTE: Logic only looks at the first 100
	 * devices.
	 * 
	 * @param token
	 * @return
	 * @throws SiteWhereException
	 */
	protected List<Device> getDevicesForSpecification(String token) throws SiteWhereException {
		SiteWhereClient client = new SiteWhereClient();
		SearchResults<Device> devices = client.listDevices(false, true, true, true, 1, 100, null, null);
		List<Device> results = new ArrayList<Device>();
		for (Device device : devices.getResults()) {
			if (device.getSpecificationToken().equals(token)) {
				results.add(device);
			}
		}
		return results;
	}

	/**
	 * Verifies that
	 * 
	 * @param e
	 */
	protected void verifyErrorCode(SiteWhereException e, ErrorCode code) {
		if (e instanceof SiteWhereSystemException) {
			SiteWhereSystemException sw = (SiteWhereSystemException) e;
			if (code != sw.getCode()) {
				Assert.fail("Unexpected error code returned. Expected " + code.getCode() + " but got: "
						+ sw.getCode());
			}
		} else {
			Assert.fail("Unexpected exception: " + e.getMessage());
		}
	}
}