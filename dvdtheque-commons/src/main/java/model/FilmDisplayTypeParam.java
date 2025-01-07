package model;


import enums.FilmDisplayType;
import enums.FilmOrigine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilmDisplayTypeParam {
	protected Logger logger = LoggerFactory.getLogger(FilmDisplayTypeParam.class);
	private final FilmDisplayType filmDisplayType;
	private final int limitFilmSize;
	private final FilmOrigine filmOrigine;
	public FilmDisplayTypeParam(FilmDisplayType filmDisplayType, int limitFilmSize, FilmOrigine filmOrigine) {
		super();
		this.filmDisplayType = filmDisplayType;
		this.limitFilmSize = limitFilmSize;
		this.filmOrigine = filmOrigine;
	}
	public FilmDisplayType getFilmDisplayType() {
		return filmDisplayType;
	}
	public int getLimitFilmSize() {
		return limitFilmSize;
	}
	public FilmOrigine getFilmOrigine() {
		return filmOrigine;
	}
	@Override
	public String toString() {
		return "FilmDisplayTypeParam [filmDisplayType=" + filmDisplayType + ", limitFilmSize=" + limitFilmSize
				+ ", filmOrigine=" + filmOrigine + "]";
	}
}
