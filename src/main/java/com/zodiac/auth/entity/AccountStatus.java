package com.zodiac.auth.entity;

/**
 * Whether an account may log in. Must match ck_user_account_status in the database.
 * (The temporary lock after too many wrong passwords uses locked_until instead, so it ends by itself.)
 */
public enum AccountStatus {
	ACTIVE,
	LOCKED,
	DISABLED
}
