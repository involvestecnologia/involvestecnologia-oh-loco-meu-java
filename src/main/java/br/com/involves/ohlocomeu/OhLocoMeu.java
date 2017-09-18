package br.com.involves.ohlocomeu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(defaultPhase = LifecyclePhase.PROCESS_SOURCES, name = "i18n")
public class OhLocoMeu extends AbstractMojo {
	private static final String FILENAME_MASK = "%s%s.%s";
	// https://localise.biz/api/export/locale/<LOCALE>.<TYPE>?key=<APPLICATION_KEY>
	private static final String URL_PREFIX = "https://localise.biz/api/export/locale/";
	private static final String LOCO_ENV_NAME = "LOCO_WEB_KEY";
	private static final int SUCCESS_CODE = 200;
	private static final String FAILED_HTTP_ERROR_CODE_WAS_MESSAGE = "Failed! HTTP error code was %s";
	private static final String FILE_SIZE_DIFFERS_FROM_INPUT_MESSAGE = "File size (%s) differs from Input size (%s).";

	@Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = true)
	private File outputDirectory;

	@Parameter(defaultValue = "en,es,pt", property = "locales", required = true)
	private String locales;

	@Parameter(defaultValue = "properties", property = "types", required = true)
	private String types;

	@Parameter(defaultValue = "messages_", property = "namePrefix", required = true)
	private String namePrefix;

	private URL buildUrl(String locale, String type) throws MalformedURLException {
		StringBuilder builder = new StringBuilder(URL_PREFIX);

		return new URL(
				builder.append(locale).append(".").append(type).append("?key=").append(getLocoWebKey()).toString());
	}

	private String getLocoWebKey() {
		return System.getenv(LOCO_ENV_NAME);
	}

	private HttpURLConnection get(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", "application/json");

		if (conn.getResponseCode() != SUCCESS_CODE) {
			throw new RuntimeException(String.format(FAILED_HTTP_ERROR_CODE_WAS_MESSAGE, conn.getResponseCode()));

		}

		return conn;
	}

	private void downloadAndSave(String locale, String type)
			throws IOException, MalformedURLException, FileNotFoundException {
		HttpURLConnection conn = get(buildUrl(locale, type));

		File target = getFile(locale, type);

		long contentLengthLong = conn.getContentLengthLong();
		OutputStream outStream = new FileOutputStream(target);

		byte[] buffer = new byte[8 * 1024];
		long size = 0;
		int bytesRead = 0;

		while ((bytesRead = conn.getInputStream().read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
			size += bytesRead;
		}

		if (size != contentLengthLong) {
			outStream.close();

			throw new IOException(String.format(FILE_SIZE_DIFFERS_FROM_INPUT_MESSAGE, size, contentLengthLong));
		}

		conn.disconnect();
		outStream.flush();
		outStream.close();
	}

	private File getFile(String locale, String type) throws IOException {
		File outputDir = outputDirectory;

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		String fileName = String.format(FILENAME_MASK, namePrefix, locale, type);
		File target = Paths.get(outputDir.getCanonicalPath(), fileName).toFile();

		return target;
	}

	private byte[] normalizeText(byte[] buffer, Charset charset) throws UnsupportedEncodingException {
		Pattern pattern = Pattern.compile("\\\\[\\s]+;");
		Matcher matcher = pattern.matcher(new String(buffer, charset));
		String result = matcher.replaceAll(";");

		return result.getBytes();
	}

	private void normalizeFile(String locale, String type) throws UnsupportedEncodingException, IOException {
		Path target = getFile(locale, type).toPath();

		Files.write(target, normalizeText(Files.readAllBytes(target), Charset.forName("UTF-8")),
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	public void execute() throws MojoExecutionException {
		try {
			for (String locale : locales.split(",")) {
				for (String type : types.split(",")) {
					downloadAndSave(locale, type);
					normalizeFile(locale, type);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();

		}
	}
}
