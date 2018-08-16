package com.baasbox.service.business;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilService {
	private static final String EMAIL_PATTERN = 
			"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	private static final String PHONE_PATTERN =
			"^\\(?([0-9]{3})\\)?[-.\\s]?([0-9]{3})[-.\\s]?([0-9]{4})$";
	
	private static Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
	private static Pattern phonePattern = Pattern.compile(PHONE_PATTERN);
	
	private static Matcher matcher;
	
	/**
	 * Eamil string validation.
	 * @param emailStr
	 * @return
	 */
	public boolean isValidEmailString(String email) {
		matcher = emailPattern.matcher(email);
		return matcher.matches();
	}
	
	/**
	 * Phone number string validation.
	 * @param phoneNumber
	 * @return
	 */
	public boolean isValidPhoneNumber(String phoneNumber) {
		matcher = phonePattern.matcher(phoneNumber);
		return matcher.matches();		
	}
}
