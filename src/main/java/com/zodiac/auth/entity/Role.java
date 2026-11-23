package com.zodiac.auth.entity;

/** What kind of person an account belongs to. Must match ck_user_account_role in the database. */
public enum Role {
	CUSTOMER,
	STAFF,
	ADMIN
}
