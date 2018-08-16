package com.baasbox.databean;

public class GeoLocation {
	Double latitude;
	Double longitude;
	Long timestamp;
	

	public GeoLocation(Double latitude, Double longitude, Long timestamp) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.timestamp = timestamp;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "GeoLocation [latitude=" + latitude + ", longitude=" + longitude + ", timestamp=" + timestamp + "]";
	}
	
	
}
