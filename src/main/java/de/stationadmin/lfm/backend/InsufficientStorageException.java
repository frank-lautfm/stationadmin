package de.stationadmin.lfm.backend;

public class InsufficientStorageException extends AdminServiceException {
	private static final long serialVersionUID = 7738302454779978970L;
	
	public InsufficientStorageException(String message) {
		super(message);
	}


}
