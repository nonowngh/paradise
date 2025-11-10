package mb.fw.paradise.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.server.ServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mb.fw.paradise.dto.APIRequestMessage;
import mb.fw.paradise.dto.DataItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DataItemUtil {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static int tableDataCount(DataItem resultItem) {
		if (resultItem == null || resultItem.getTable() == null) {
			return 0;
		}
		LinkedHashMap<String, List<Map<String, Object>>> table = resultItem.getTable();
		return table.values().stream().filter(Objects::nonNull).mapToInt(List::size).sum();
	}

	public static void main(String[] args) {
		LinkedHashMap<String, List<Map<String, Object>>> table = new LinkedHashMap<>();
		System.out.println(tableDataCount(DataItem.builder().table(table).build()));
	}

	public static Flux<APIRequestMessage> chunkLargeData(APIRequestMessage request, int chunkSize) {
		List<APIRequestMessage> messageChunks = new ArrayList<>();
		Map<String, List<Map<String, Object>>> tables = request.getDataItem().getTable();
		// 테이블별 chunking
		for (Map.Entry<String, List<Map<String, Object>>> entry : tables.entrySet()) {
			String tableName = entry.getKey();
			List<Map<String, Object>> rows = entry.getValue();
			if (rows == null || rows.isEmpty())
				continue;
			for (int i = 0; i < rows.size(); i += chunkSize) {
				List<Map<String, Object>> subList = rows.subList(i, Math.min(i + chunkSize, rows.size()));
				DataItem subData = new DataItem();
				LinkedHashMap<String, List<Map<String, Object>>> tableItem = new LinkedHashMap<>();
				tableItem.put(tableName, subList);
				subData.setTable(tableItem);
				APIRequestMessage chunkMessage = new APIRequestMessage();
				chunkMessage.setInterfaceId(request.getInterfaceId());
				chunkMessage.setTransactionId(request.getTransactionId());
				chunkMessage.setDataItem(subData);
				chunkMessage.setDataCount(subList.size());
				messageChunks.add(chunkMessage);
			}
		}
		return Flux.fromIterable(messageChunks);
	}

	public static Mono<byte[]> gzipNDJSON(Flux<APIRequestMessage> messages) {
		return messages.map(msg -> {
			try {
				return objectMapper.writeValueAsString(msg) + "\n";
			} catch (JsonProcessingException e) {
				throw new RuntimeException("JSON 직렬화 실패", e);
			}
		}).collectList() // Flux → List<String>
				.map(lines -> String.join("", lines)) // NDJSON 문자열 합치기
				.map(GzipUtil::gzip); // 압축 (byte[])
	}

	// NDJSON + GZIP 파싱
	public static Flux<APIRequestMessage> parseNdjsonGzip(ServerRequest serverRequest) {
		return serverRequest.bodyToFlux(byte[].class).collectList() // 모든 chunk를 합침
				.flatMapMany(new Function<List<byte[]>, Publisher<APIRequestMessage>>() {
					@Override
					public Publisher<APIRequestMessage> apply(List<byte[]> chunks) {
						try {
							// 여러 byte[]를 하나로 합치기
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							for (byte[] b : chunks) {
								outputStream.write(b);
							}
							byte[] fullBytes = outputStream.toByteArray();

							// GZIP 해제
							String ndjson = GzipUtil.ungzip(fullBytes);

							// NDJSON → Flux<APIRequestMessage>
							List<APIRequestMessage> list = new ArrayList<>();
							BufferedReader reader = new BufferedReader(new StringReader(ndjson));
							String line;
							ObjectMapper mapper = new ObjectMapper();
							while ((line = reader.readLine()) != null) {
								line = line.trim();
								if (!line.isEmpty()) {
									list.add(mapper.readValue(line, APIRequestMessage.class));
								}
							}
							return Flux.fromIterable(list);
						} catch (Exception e) {
							return Flux.error(new RuntimeException("GZIP 해제 또는 NDJSON 변환 실패", e));
						}
					}
				});
	}
}
