package com.fedstack.spending.auth.token;

public enum TokenPurpose {
	ACCESS("access"),
	REFRESH("refresh");

	private final String claimValue;

	TokenPurpose(String claimValue) {
		this.claimValue = claimValue;
	}

	public String claimValue() {
		return claimValue;
	}
}
