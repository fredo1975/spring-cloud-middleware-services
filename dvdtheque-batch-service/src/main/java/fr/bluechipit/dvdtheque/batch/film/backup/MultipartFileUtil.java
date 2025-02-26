package fr.bluechipit.dvdtheque.batch.film.backup;

import exceptions.DvdthequeCommonsException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

@Component
public class MultipartFileUtil {
	protected Logger logger = LoggerFactory.getLogger(MultipartFileUtil.class);
	@Autowired
	private ExcelFilmHandler excelFilmHandler;
	/**
	 *
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public File createFileToImport(MultipartFile file) throws Exception {
		File resFile = null;
		Calendar cal = Calendar.getInstance();
		File tempFile = new File(System.getProperty("java.io.tmpdir")+"/"+ cal.getTimeInMillis() + "_"+file.getOriginalFilename());
		File convFile = new File(System.getProperty("java.io.tmpdir")+"/"+file.getOriginalFilename());
		try {
			file.transferTo(convFile);
		} catch (IllegalStateException | IOException e) {
			logger.error(e.getMessage());
			throw e;
		}
		FileSystemResource resource = new FileSystemResource(convFile);
		if (!resource.exists()) {
			throw new IllegalStateException("file does not exists " + resource.getPath());
		}
		if(StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(file.getOriginalFilename()),"csv")) {
			resFile = convFile;
		} else if(StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(file.getOriginalFilename()),"xls")
				|| StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(file.getOriginalFilename()),"xlsx")) {
			Workbook workBook;
			try {
				logger.info("convFile.getAbsolutePath()="+convFile.getAbsolutePath());
				workBook = this.excelFilmHandler.createSheetFromFile(convFile);

				String csv = this.excelFilmHandler.createCsvFromExcel(workBook);
				FileOutputStream outputStream = new FileOutputStream(tempFile);
				byte[] strToBytes = csv.getBytes();
				outputStream.write(strToBytes);
				outputStream.close();
				resFile = tempFile;
			} catch (EncryptedDocumentException | IOException e) {
				logger.error(e.getMessage());
				throw e;
			}
		}else {
			String msg = "File not recognized";
			logger.error(msg);
			throw new DvdthequeCommonsException(msg);
		}
		return resFile;
	}
}
