package mb.fw.paradise.common.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipUtil {
	public static String ungzip(byte[] compressed) {
		if (compressed == null || compressed.length == 0) {
			return "";
		}

		try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
				GZIPInputStream gzip = new GZIPInputStream(bis);
				InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8);
				BufferedReader buffered = new BufferedReader(reader)) {

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = buffered.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException("GZIP 해제 실패", e);
		}
	}

	/**
	 * 문자열을 GZIP 압축 후 byte[] 로 반환
	 */
	public static byte[] gzip(String data) {
		if (data == null || data.isEmpty()) {
			return new byte[0];
		}

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
			gzip.write(data.getBytes(StandardCharsets.UTF_8));
			gzip.finish(); // 압축 마무리
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("GZIP 압축 실패", e);
		}
	}

	/**
	 * byte[] 데이터를 GZIP 압축 후 반환
	 */
	public static byte[] gzip(byte[] input) {
		if (input == null || input.length == 0) {
			return new byte[0];
		}

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
			gzip.write(input);
			gzip.finish();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("GZIP 압축 실패", e);
		}
	}

	/**
	 * 문자열을 GZIP Base64 문자열로 반환 (옵션)
	 */
	public static String gzipToBase64(String data) {
		return java.util.Base64.getEncoder().encodeToString(gzip(data));
	}
}
