package fr.raksrinana.twitchminer.api.passport.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Error while login in, 2FA code is missing.
 */
public class MissingAuthy2FA extends LoginException{
	public MissingAuthy2FA(@Nullable Integer errorCode, @Nullable String description){
		super(errorCode, description);
	}
}
