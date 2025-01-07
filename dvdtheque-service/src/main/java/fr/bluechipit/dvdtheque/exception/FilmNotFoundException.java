package fr.bluechipit.dvdtheque.exception;

public class FilmNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4807041272415619275L;

	public FilmNotFoundException(String msg) {
		super(msg);
	}
	public FilmNotFoundException(String msg,Throwable e) {
		super(msg,e);
	}
}
